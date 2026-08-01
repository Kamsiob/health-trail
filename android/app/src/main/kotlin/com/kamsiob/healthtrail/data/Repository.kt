package com.kamsiob.healthtrail.data

import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import java.time.ZoneId
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
     * answer to "where are they right now". It gets a start of today and no
     * end, because someone standing in a corridor knows they are there now and
     * does not know when this ends. A day rather than an instant: the instant
     * would be a claim about when the stay began, and nobody made it.
     */
    suspend fun createChapter(subjectId: String, name: String): String = insert(
        "chapter",
        mapOf(
            "subject_id" to subjectId,
            "name" to name,
            // The chapter began the day the person set the notebook up, which
            // is as much as anyone in a corridor can say. Not the minute: the
            // minute would be a claim about when the stay started, and nobody
            // said that.
        ) + dateColumns("started", Edtf.day(LocalDate.now())),
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
                    "sort_index" to index,
                    // A thread the person just turned on started today, and a
                    // day is exactly as much as anyone knows about it.
                ) + dateColumns("started", Edtf.day(LocalDate.now())),
            )
        }
    }

    /**
     * The four columns an event date occupies, per contract section 3.1.
     *
     * **The string is the truth and the range is an index.** The range is
     * computed here, on the way in, from the string and the zone, and it is
     * recomputed the same way on import rather than being trusted from the
     * file. That is what keeps it an index rather than a second opinion that
     * can drift.
     *
     * Replaced the older pair of a millisecond value and a precision enum,
     * which could not tell "she was moved in the fall" from a specific
     * afternoon and could not survive an export without losing which it was.
     */
    private fun dateColumns(base: String, date: Edtf.Date): Map<String, Any?> {
        val zone = ZoneId.systemDefault()
        val range = Edtf.resolve(date, zone)
        return mapOf(
            "${base}_edtf" to date.canonical,
            "${base}_zone" to Edtf.zoneFor(date, zone),
            "${base}_start" to range.start,
            "${base}_end" to range.end,
        )
    }

    /**
     * Writes one entry to the trail.
     *
     * **Every argument except the kind is optional.** A half remembered note is
     * a valid note. Nothing here validates, nothing rejects, and nothing is
     * required, because the alternative is a person in a corridor losing what
     * they were trying to write down.
     *
     * `isUnfiled` marks an entry the person could not place. It sits in the
     * Unfiled tray until they confirm a home. The app suggests by plain word
     * matching and never files anything on its own.
     */
    suspend fun createEntry(
        subjectId: String,
        kind: String,
        title: String? = null,
        body: String? = null,
        occurred: Edtf.Date = Edtf.day(LocalDate.now()),
        chapterId: String? = null,
        isUnfiled: Boolean = false,
    ): String = insert(
        "entry",
        mapOf(
            "subject_id" to subjectId,
            "kind" to kind,
            "title" to title?.ifBlank { null },
            "body" to body?.ifBlank { null },
            "chapter_id" to chapterId,
            "is_unfiled" to if (isUnfiled) 1 else 0,
        ) + dateColumns("occurred", occurred),
    )

    /**
     * The date one entry carries, read back through the live view.
     *
     * Returns null for a string this version cannot read, which is what an
     * export from a later version of the app would carry. The caller shows it
     * as a date it does not understand rather than dropping the entry, because
     * the entry is still the person's and is still the record.
     */
    suspend fun entryOccurred(entryId: String): Edtf.Date? = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT occurred_edtf FROM live_entry WHERE id = ?",
            arrayOf(entryId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            Edtf.parse(cursor.getString(0))
        }
    }

    // -- measures and measurements -----------------------------------------

    /** A thing this notebook tracks over time. */
    data class Measure(
        val id: String,
        val name: String,
        val presetId: String?,
        val unit: String?,
        val isText: Boolean,
    )

    /** What this notebook already tracks, in the order it was set up. */
    suspend fun measures(subjectId: String): List<Measure> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, name, preset_id, unit, style FROM live_measure " +
                "WHERE subject_id = ? ORDER BY sort_index, created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Measure(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            presetId = cursor.getString(2),
                            unit = cursor.getString(3),
                            isText = cursor.getString(4) in TEXT_STYLES,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Starts tracking something, from a preset.
     *
     * **Created the first time the person records one, not at setup.** A
     * notebook that arrives with sixteen empty charts is a list of things
     * somebody has not done, which is the scorecard this app does not keep.
     *
     * `advice_risk` is carried from the preset onto the row so the rendering
     * layer can hold the content rules without looking the preset up again. It
     * is never shown to the person and never becomes a warning.
     */
    suspend fun createMeasure(
        subjectId: String,
        preset: TemplateCatalog.Preset,
        unit: String?,
        sortIndex: Int = 0,
    ): String = insert(
        "measure",
        mapOf(
            "subject_id" to subjectId,
            "name" to preset.name,
            "preset_id" to preset.id,
            "unit" to unit,
            "style" to preset.style,
            "gap_tolerance" to preset.gapTolerance,
            "advice_risk" to preset.adviceRisk,
            "show_medication_markers" to if (preset.medicationMarkers) 1 else 0,
            "sort_index" to sortIndex,
        ),
    )

    /**
     * Records one measurement.
     *
     * **A number and words are different columns, not one column that holds
     * either.** "Ate about half her lunch" is not a number and storing it as
     * one would either lose it or invent a figure nobody gave. The preset says
     * which the thing being tracked is.
     *
     * `source` records who provided it, because a value the family measured
     * and a value a clinician stated are different things and the record must
     * not blur them. Nothing here is judged, ranged, or compared.
     */
    suspend fun recordMeasurement(
        measureId: String,
        number: Double? = null,
        text: String? = null,
        unit: String? = null,
        occurred: Edtf.Date = Edtf.day(LocalDate.now()),
        note: String? = null,
        entryId: String? = null,
        source: String = "family",
    ): String = insert(
        "measurement",
        mapOf(
            "measure_id" to measureId,
            "entry_id" to entryId,
            "value_number" to number,
            "value_text" to text?.ifBlank { null },
            "unit" to unit,
            "note" to note?.ifBlank { null },
            "source" to source,
        ) + dateColumns("occurred", occurred),
    )

    /** Styles whose value is words rather than a number, per the preset catalog. */
    private val TEXT_STYLES = setOf("categorical", "observational", "event_log", "photo_log")

    // -- deleting, which is always a tombstone ------------------------------

    /**
     * Marks one row deleted.
     *
     * **Never a DELETE.** The data contract's second named impossibility is a
     * schema that removes rows: with the row gone there is nothing left to tell
     * another device it was deleted, so the peer resurrects it on the next sync
     * and the deletion appears to undo itself, forever. Sets `deleted_at`,
     * bumps `rev`, and lets the change log trigger record it in the same
     * transaction.
     *
     * Takes a [Section] rather than a table name for the same reason [count]
     * does: a caller cannot reach a table by passing a string.
     *
     * **The row stays readable to the two operations the contract allows to see
     * it**, the full data wipe and the tombstone purge, and to nothing else.
     * Every read in this class goes through a `live_` view, so from the moment
     * this returns the row is gone from the app's point of view.
     */
    suspend fun delete(section: Section, rowId: String) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE ${section.table} SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ? AND deleted_at IS NULL",
            arrayOf<Any?>(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                rowId,
            ),
        )
    }

    /**
     * Whether a row is still physically present, tombstone and all.
     *
     * **For tests only, and named so nobody reaches for it by accident.** The
     * app has no business asking this: every screen reads a live view and a
     * deleted row is gone from its point of view. What a test needs to prove is
     * the opposite, that the row survived the delete, because a row that was
     * removed leaves nothing to tell another device it was deleted.
     */
    suspend fun rowExistsForTest(table: String, rowId: String): Boolean =
        withContext(Dispatchers.IO) {
            // allow-base-table: proving a tombstoned row still exists. Reading
            // the live view here would assert the opposite of the point.
            db().database.rawQuery(
                "SELECT COUNT(*) FROM $table WHERE id = ?",
                arrayOf(rowId),
            ).use { it.moveToFirst() && it.getInt(0) == 1 }
        }

    /** The revision of one row, for tests that assert a write did or did not happen. */
    suspend fun revisionForTest(table: String, rowId: String): Int =
        withContext(Dispatchers.IO) {
            // allow-base-table: a deleted row has no live view to read from,
            // and its revision is exactly what these tests are about.
            db().database.rawQuery(
                "SELECT rev FROM $table WHERE id = ?",
                arrayOf(rowId),
            ).use { if (it.moveToFirst()) it.getInt(0) else -1 }
        }

    // -- the Unfiled tray -------------------------------------------------

    /**
     * One entry the person could not place, with everything the tray needs to
     * show it and nothing more.
     */
    data class UnfiledEntry(
        val id: String,
        val kind: String,
        val title: String?,
        val body: String?,
        /** The EDTF string, rendered by `EventDateText` rather than here. */
        val occurredEdtf: String?,
        /** For ordering when the date is unknown, which is a common case here. */
        val createdAt: Long,
    )

    /**
     * Everything waiting to be filed, oldest first.
     *
     * **Ordered by when it was written down, not by when it happened.** An
     * entry reaches this tray precisely because the person could not place it,
     * and a good share of them have an unknown date and therefore no position
     * on a timeline at all. When it was captured is the one ordering every row
     * here definitely has.
     */
    suspend fun unfiled(subjectId: String): List<UnfiledEntry> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, kind, title, body, occurred_edtf, created_at FROM live_entry " +
                "WHERE subject_id = ? AND is_unfiled = 1 ORDER BY created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        UnfiledEntry(
                            id = cursor.getString(0),
                            kind = cursor.getString(1),
                            title = cursor.getString(2),
                            body = cursor.getString(3),
                            occurredEdtf = cursor.getString(4),
                            createdAt = cursor.getLong(5),
                        ),
                    )
                }
            }
        }
    }

    /** How many are waiting. Read on its own so a caller can ask without loading them. */
    suspend fun unfiledCount(subjectId: String): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT COUNT(*) FROM live_entry WHERE subject_id = ? AND is_unfiled = 1",
            arrayOf(subjectId),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Files one entry into a thread, or clears it from the tray with no thread.
     *
     * **Both halves in one transaction.** Linking without clearing leaves the
     * entry filed and still in the tray. Clearing without linking loses the
     * answer the person just gave. Either alone is worse than neither, and the
     * change log triggers fire inside the same transaction on both writes, per
     * the data contract.
     *
     * A null [threadId] is a real answer: it means the person decided this
     * belongs to nothing in particular. It leaves the tray all the same,
     * because the tray is for things nobody has looked at yet rather than for
     * things without a thread.
     */
    suspend fun fileEntry(entryId: String, threadId: String?) = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            if (threadId != null) {
                val now = System.currentTimeMillis()
                database.execSQL(
                    "INSERT INTO entry_thread " +
                        "(id, created_at, updated_at, origin_device, rev, entry_id, thread_id) " +
                        "VALUES (?, ?, ?, ?, 1, ?, ?)",
                    arrayOf<Any?>(Ids.new(), now, now, db().deviceId, entryId, threadId),
                )
            }
            database.execSQL(
                "UPDATE entry SET is_unfiled = 0, updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(System.currentTimeMillis(), entryId),
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    /**
     * The threads one entry is linked to.
     *
     * Read through the live views on both sides, so a thread the person deleted
     * does not resurface here attached to an entry.
     */
    suspend fun threadsForEntry(entryId: String): List<String> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT t.id FROM live_entry_thread et " +
                "JOIN live_care_thread t ON t.id = et.thread_id " +
                "WHERE et.entry_id = ?",
            arrayOf(entryId),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
    }

    /**
     * A care thread, which is one parallel stream of care running through the
     * whole notebook.
     *
     * `colorIndex` is an index into the theme's route colors rather than a
     * stored color, so the dark theme substitution happens in the theme and a
     * stored color can never fail contrast.
     */
    data class CareThread(val id: String, val label: String, val colorIndex: Int)

    /**
     * The threads this notebook carries, in the order the person sees them.
     *
     * Returns an empty list for a notebook with no situation template applied,
     * which is a real state rather than an error: "Not sure yet" is a valid
     * answer to the situation picker and it produces a working notebook.
     */
    suspend fun threads(subjectId: String): List<CareThread> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, label, color_index FROM live_care_thread " +
                "WHERE subject_id = ? ORDER BY sort_index, created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CareThread(
                            id = cursor.getString(0),
                            label = cursor.getString(1),
                            colorIndex = cursor.getInt(2),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Files an entry under a thread.
     *
     * A join row rather than a column on the entry, because an entry can belong
     * to several: a call about a wound during a physical therapy week belongs to
     * both. The schema says so and this follows it.
     */
    suspend fun linkEntryToThread(entryId: String, threadId: String) {
        insert("entry_thread", mapOf("entry_id" to entryId, "thread_id" to threadId))
    }

    /** The extra fields a call carries, written alongside its entry. */
    suspend fun addCallDetail(
        entryId: String,
        reached: Boolean? = null,
        outcome: String? = null,
    ) {
        insert(
            "call_detail",
            mapOf(
                "entry_id" to entryId,
                "reached" to reached?.let { if (it) 1 else 0 },
                "outcome" to outcome?.ifBlank { null },
            ),
        )
    }

    // -- counting, for the table of contents ------------------------------

    /**
     * How many live rows a section holds, for one subject.
     *
     * Takes a [Section] rather than a table name, so a caller cannot reach a
     * base table by passing a string. That is the difference between a
     * repository that filters tombstones and one that merely offers to.
     *
     * **The subject is required, and that is the whole point of this signature.**
     * It counted across every subject until a test caught it returning two
     * after one measure was created. With one notebook that is invisible. With
     * two it makes every number on the table of contents quietly wrong, and
     * quietly wrong counts in a care record are worse than missing ones,
     * because nobody thinks to check them. Issue #58.
     */
    suspend fun count(section: Section, subjectId: String): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT COUNT(*) FROM ${section.view} WHERE subject_id = ?",
            arrayOf(subjectId),
        ).use {
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
        ;

        /**
         * The base table behind the view, derived rather than listed a second
         * time so the two cannot drift.
         *
         * Only [delete] uses it, because a write has to name the table: there
         * is nothing to update through a view. Everything that reads uses
         * [view] and therefore cannot see a tombstone.
         */
        internal val table: String get() = view.removePrefix("live_")
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
