package com.kamsiob.healthtrail.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only way the rest of the app reaches the database.
 *
 * **The rule this exists to enforce, and why it is structural rather than a
 * convention.** Every read of user data filters tombstones. A single forgotten
 * `deleted_at IS NULL` is a leak of something the person believed they had
 * deleted, and the symptom is an entry reappearing in a search result or an
 * export months later, long after anyone could connect it to the cause.
 *
 * So reads go through the `live_*` views, which filter by construction, and
 * nothing outside this package holds a database handle at all. Reading
 * tombstones requires calling something whose name says so.
 *
 * Writes set the six columns the data contract requires and bump `rev`. The
 * change log is not written here: it is appended by triggers in the schema, in
 * the same transaction as the write, because a rule the application has to
 * remember is a rule that gets forgotten exactly once, silently.
 *
 * Every method suspends onto `Dispatchers.IO`. There is no way to reach the
 * database from the main thread through this class.
 */
class Repository private constructor(
    /**
     * Typed as [Application] rather than `Context` on purpose.
     *
     * A `Context` held in a static field is a memory leak when it is an
     * Activity, and lint says so. The value here has always been the
     * application context, but the field type is what carries that guarantee to
     * anyone reading it, and to the tooling. Narrowing the type answers the
     * warning instead of suppressing it.
     */
    private val app: android.app.Application,
) {

    /**
     * The database, resolved per call rather than held.
     *
     * **This is deliberate and it fixed a real bug.** An earlier version cached
     * the [HealthTrailDatabase] in the constructor. Closing the database, which
     * the full data wipe does and which tests do between cases, left this
     * object holding a closed handle, and the next call failed with "attempt to
     * re-open an already-closed object". Two singletons with independent
     * lifecycles will always drift like that.
     *
     * Resolving each time removes the possibility rather than patching it.
     * [HealthTrailDatabase.open] caches internally, so this costs a map lookup,
     * not a reopen.
     */
    private suspend fun db(): HealthTrailDatabase = HealthTrailDatabase.open(app)

    suspend fun deviceId(): String = db().deviceId

    // -- settings ---------------------------------------------------------

    /**
     * `app_meta` holds settings for this installation rather than the person's
     * records: the device id, the disclaimer acceptance, the last opened
     * timestamp the digest reads. It carries no tombstone because it is not
     * user authored content, which is why it is reached separately from
     * everything below.
     */
    suspend fun setting(key: String): String? = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT value FROM app_meta WHERE key = ?", arrayOf(key),
        ).use { if (it.moveToFirst()) it.getString(0) else null }
    }

    suspend fun putSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "INSERT OR REPLACE INTO app_meta (key, value, updated_at) VALUES (?, ?, ?)",
            arrayOf<Any>(key, value, System.currentTimeMillis()),
        )
    }

    suspend fun settingTimestamp(key: String): Long? = setting(key)?.toLongOrNull()

    suspend fun putSettingTimestamp(key: String, millis: Long) =
        putSetting(key, millis.toString())

    // -- writing ----------------------------------------------------------

    /**
     * Inserts a row, setting the six columns the data contract requires.
     *
     * Every entity write goes through here rather than building its own SQL, so
     * none of them can forget a column. The change log is not touched: triggers
     * in the schema append it in the same transaction, which is a guarantee the
     * database makes rather than one this layer keeps.
     *
     * Returns the generated id.
     */
    private suspend fun insert(table: String, values: Map<String, Any?>): String =
        withContext(Dispatchers.IO) {
            val id = Ids.new()
            val now = System.currentTimeMillis()
            val all = LinkedHashMap<String, Any?>()
            all["id"] = id
            all["created_at"] = now
            all["updated_at"] = now
            all["origin_device"] = db().deviceId
            all["rev"] = 1
            all.putAll(values)

            val columns = all.keys.joinToString(", ")
            val placeholders = all.keys.joinToString(", ") { "?" }
            db().database.execSQL(
                "INSERT INTO $table ($columns) VALUES ($placeholders)",
                all.values.toTypedArray(),
            )
            id
        }

    /**
     * The person being looked after.
     *
     * Every field except the name is optional, and the name itself is whatever
     * the person calls them rather than a legal name. Setup asks for three
     * things and lets everything else wait, so this has to be creatable from
     * almost nothing.
     */
    data class Subject(
        val id: String,
        val displayName: String,
        val relationship: String?,
        val situationTemplateId: String?,
    )

    suspend fun createSubject(
        displayName: String,
        relationship: String? = null,
        situationTemplateId: String? = null,
    ): String = insert(
        "subject",
        mapOf(
            "display_name" to displayName,
            "relationship" to relationship?.ifBlank { null },
            "situation_template_id" to situationTemplateId,
            "is_active" to 1,
        ),
    )

    /** One subject by id. Used where a caller knows which one it means. */
    suspend fun subject(id: String): Subject? = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, display_name, relationship, situation_template_id " +
                "FROM live_subject WHERE id = ?",
            arrayOf(id),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            Subject(
                id = cursor.getString(0),
                displayName = cursor.getString(1),
                relationship = cursor.getString(2),
                situationTemplateId = cursor.getString(3),
            )
        }
    }

    /** The active subject, or null when setup has not happened yet. */
    suspend fun activeSubject(): Subject? = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, display_name, relationship, situation_template_id " +
                "FROM live_subject WHERE is_active = 1 ORDER BY created_at LIMIT 1",
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            Subject(
                id = cursor.getString(0),
                displayName = cursor.getString(1),
                relationship = cursor.getString(2),
                situationTemplateId = cursor.getString(3),
            )
        }
    }

    /**
     * A chapter is a place and a period. Setup creates the first one from the
     * answer to "where are they right now", with no dates, because someone
     * standing in a corridor does not know when this started or when it ends.
     */
    suspend fun createChapter(subjectId: String, name: String): String = insert(
        "chapter",
        mapOf(
            "subject_id" to subjectId,
            "name" to name,
            "started_at" to System.currentTimeMillis(),
        ),
    )

    /**
     * One phone number worth having in a hurry. Stored as a care team person
     * rather than as a loose string, so it is already on the emergency card and
     * already links to every call involving them.
     */
    suspend fun createPerson(
        subjectId: String,
        displayName: String,
        phone: String? = null,
        roleLabel: String? = null,
    ): String = insert(
        "person",
        mapOf(
            "subject_id" to subjectId,
            "display_name" to displayName,
            "phone" to phone?.ifBlank { null },
            "role_label" to roleLabel,
        ),
    )

    /**
     * Applies a situation template to a notebook.
     *
     * Records which template was used and creates a care thread per thread the
     * template offers. Everything created is ordinary editable data: the person
     * can rename, reorder, or delete any of it, and reapplying a different
     * template later never destroys what is already here.
     */
    suspend fun applySituation(
        subjectId: String,
        templateId: String,
        threads: List<Pair<String, String>>,
    ) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE subject SET situation_template_id = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any>(templateId, System.currentTimeMillis(), subjectId),
        )
        threads.forEachIndexed { index, (threadTemplateId, label) ->
            insert(
                "care_thread",
                mapOf(
                    "subject_id" to subjectId,
                    "label" to label,
                    "template_id" to threadTemplateId,
                    "color_index" to index,
                    "started_at" to System.currentTimeMillis(),
                    "sort_index" to index,
                ),
            )
        }
    }

    // -- counting, for the table of contents ------------------------------

    /**
     * How many live rows a section holds.
     *
     * Takes a [Section] rather than a table name, so a caller cannot reach a
     * base table by passing a string. That is the difference between a
     * repository that filters tombstones and one that merely offers to.
     */
    suspend fun count(section: Section): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery("SELECT COUNT(*) FROM ${section.view}", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * The sections a notebook can hold, each bound to its live view.
     *
     * An enum rather than free text on purpose: it is the boundary that stops a
     * table name reaching SQL from anywhere else in the app.
     */
    enum class Section(internal val view: String) {
        CARE_TEAM("live_person"),
        MEDICATIONS("live_medication"),
        APPOINTMENTS("live_appointment"),
        CHAPTERS("live_chapter"),
        THREADS("live_care_thread"),
        TRAIL("live_entry"),
        PROGRESS("live_measure"),
        DOCUMENTS("live_document"),
        MONEY("live_bill"),
        STANDING_INSTRUCTIONS("live_standing_instruction"),
        ASK_NEXT_TIME("live_question"),
        EMERGENCY_CARD("live_emergency_card"),
        PROJECTS("live_project"),
    }

    companion object {
        /** The person accepted the disclaimer at this time. Never cleared. */
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted_at"

        /** When the person last opened the app. What the digest reads from. */
        const val KEY_LAST_OPENED = "last_opened_at"

        @Volatile
        private var instance: Repository? = null

        suspend fun open(context: android.content.Context): Repository {
            val existing = instance
            if (existing != null) return existing
            // Opening the database here rather than lazily, so a caller that
            // gets a Repository back knows the database is ready and any
            // DatabaseKeyLost has already surfaced.
            HealthTrailDatabase.open(context)
            return synchronized(this@Companion) {
                instance ?: Repository(
                    context.applicationContext as android.app.Application,
                ).also { instance = it }
            }
        }

        /** For tests, and for the full data wipe. */
        fun closeForTest() = synchronized(this) {
            instance = null
            HealthTrailDatabase.closeForTest()
        }
    }
}
