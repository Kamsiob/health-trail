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
        withContext(Dispatchers.IO) { insertRow(table, values) }

    /**
     * The insert itself, with no dispatcher of its own.
     *
     * Split out so a caller that has already opened a transaction can add rows
     * to it. Switching dispatchers inside a transaction is how a write ends up
     * on a different thread than the transaction it believes it is part of.
     */
    private suspend fun insertRow(table: String, values: Map<String, Any?>): String {
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
        return id
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

    /**
     * Everything written after [since], for the digest.
     *
     * **Read from the change log rather than by scanning the tables.** Every
     * write appends to it in the same transaction, per rule 3, so it is the one
     * place that knows a row was corrected or removed rather than merely what
     * the row says now. Reconstructing that from the tables would mean
     * inferring history from state, which is exactly what the log exists to
     * make unnecessary.
     *
     * **Tombstones included**, because a removal is a change the person made
     * and a summary that silently omitted it would be describing a notebook
     * that never existed.
     */
    suspend fun changesSince(since: Long): List<Digest.Change> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            // allow-base-table: the change log is the log, not user data, and
            // has no live_ view over it by design. It carries no tombstone.
            "SELECT table_name, row_id, op, changed_at FROM change_log " +
                "WHERE changed_at > ? ORDER BY seq",
            arrayOf(since.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Digest.Change(
                            table = cursor.getString(0),
                            rowId = cursor.getString(1),
                            op = cursor.getString(2),
                            changedAt = cursor.getLong(3),
                        ),
                    )
                }
            }
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
     * Changes what is recorded about somebody.
     *
     * **Every field can be corrected, including to blank.** A phone number
     * written down wrong is worse than none, so clearing one has to be
     * possible. Blank is stored as null, so "never given" and "taken back out"
     * read the same way downstream, which is what the card wants: both mean the
     * app knows nothing.
     */
    suspend fun updatePerson(
        personId: String,
        displayName: String,
        phone: String?,
        roleLabel: String?,
    ) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE person SET display_name = ?, phone = ?, role_label = ?, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                displayName,
                phone?.ifBlank { null },
                roleLabel?.ifBlank { null },
                System.currentTimeMillis(),
                personId,
            ),
        )
    }

    /** Changes what is recorded about a medication. The dose stays text. */
    suspend fun updateMedication(
        medicationId: String,
        name: String,
        doseText: String?,
        purposeText: String?,
        notes: String?,
        onEmergencyCard: Boolean,
    ) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE medication SET name = ?, dose_text = ?, purpose_text = ?, notes = ?, " +
                "on_emergency_card = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                name,
                doseText?.ifBlank { null },
                purposeText?.ifBlank { null },
                notes?.ifBlank { null },
                if (onEmergencyCard) 1 else 0,
                System.currentTimeMillis(),
                medicationId,
            ),
        )
    }

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

    /**
     * Changes when an entry happened.
     *
     * **This exists because rule 17 requires it**: every date is editable
     * forever, from the entry itself. A date captured in a hallway is the one
     * most likely to be wrong, and a record that will not let somebody correct
     * it is a record that quietly accumulates errors nobody can reach.
     *
     * All four columns of the group move together, through [dateColumns], so
     * the derived range can never disagree with the EDTF string it came from.
     * Writing the string alone would leave the trail ordering by the old date
     * while displaying the new one.
     */
    suspend fun updateEntryOccurred(entryId: String, occurred: Edtf.Date) =
        withContext(Dispatchers.IO) {
            val columns = dateColumns("occurred", occurred)
            val assignments = columns.keys.joinToString(", ") { "$it = ?" }
            db().database.execSQL(
                "UPDATE entry SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
                (columns.values + listOf(System.currentTimeMillis(), entryId)).toTypedArray(),
            )
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

    /**
     * How many entries the change log holds.
     *
     * For tests. The app never counts the log: what it will read is "everything
     * after sequence N", which is a different question and the only one a
     * digest or a peer ever asks.
     */
    suspend fun changeLogSizeForTest(): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery("SELECT COUNT(*) FROM change_log", null)
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /** The most recent change log entry, for tests that assert what was recorded. */
    suspend fun latestChangeForTest(): Map<String, String>? = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT table_name, row_id, op, device_id FROM change_log ORDER BY seq DESC LIMIT 1",
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            mapOf(
                "table_name" to cursor.getString(0),
                "row_id" to cursor.getString(1),
                "op" to cursor.getString(2),
                "device_id" to cursor.getString(3),
            )
        }
    }

    /**
     * Runs a write inside a transaction that is then abandoned.
     *
     * **For one test, and it is the test the schema check cannot do.** A
     * repository write nested inside a caller's transaction has to take its
     * change log entry with it when that transaction rolls back, or the log
     * records an event with no row behind it and a peer is handed a change
     * that never happened.
     *
     * Deliberately never marks the transaction successful.
     */
    suspend fun <T> inAbandonedTransactionForTest(block: suspend Repository.() -> T): T =
        withContext(Dispatchers.IO) {
            val database = db().database
            database.beginTransaction()
            try {
                block()
            } finally {
                // No setTransactionSuccessful, so ending it rolls everything
                // back, the data write and its log entry together.
                database.endTransaction()
            }
        }

    /** The schema version this database is at, for the migration test. */
    suspend fun schemaVersionForTest(): Int = withContext(Dispatchers.IO) {
        Migrations.versionOf(db().database)
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

    // -- reading a section back -------------------------------------------

    /**
     * One entry as the trail shows it, with the threads it belongs to already
     * attached.
     *
     * The threads come along because a trail row shows them, and fetching them
     * per row would be one query per entry on a screen whose whole job is to
     * hold years of them.
     */
    data class TrailEntry(
        val id: String,
        val kind: String,
        val title: String?,
        val body: String?,
        /** The EDTF string. Rendered by `EventDateText`, never parsed here. */
        val occurredEdtf: String?,
        /** Null exactly when the date is unknown, which is a real answer. */
        val occurredStart: Long?,
        val createdAt: Long,
        val isUnfiled: Boolean,
        val threads: List<CareThread>,
    )

    /**
     * The whole trail for one subject, most recent first.
     *
     * **Ordered by when things happened, not by when they were written down.**
     * That is the distinction the schema draws with `occurred_start` against
     * `created_at`, and it is the one that matters to somebody looking back:
     * a call logged on Tuesday about something that happened on Sunday belongs
     * on Sunday.
     *
     * **An unknown date sorts last rather than sorting as zero or as today.**
     * Both of those would be the app inventing a position on the timeline that
     * the person never gave it, which rule 17 forbids and which would be a
     * quiet lie in a care record. Entries with no date sit together at the end,
     * ordered by when they were captured, and each says the date is not known.
     * `occurred_start IS NULL` sorts before `DESC` rather than relying on
     * NULLS LAST, whose availability varies by SQLite build.
     */
    suspend fun trail(subjectId: String): List<TrailEntry> = withContext(Dispatchers.IO) {
        val threadsByEntry = mutableMapOf<String, MutableList<CareThread>>()
        db().database.rawQuery(
            "SELECT et.entry_id, t.id, t.label, t.color_index " +
                "FROM live_entry_thread et " +
                "JOIN live_care_thread t ON t.id = et.thread_id " +
                "JOIN live_entry e ON e.id = et.entry_id " +
                "WHERE e.subject_id = ? ORDER BY t.sort_index, t.created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                threadsByEntry
                    .getOrPut(cursor.getString(0)) { mutableListOf() }
                    .add(CareThread(cursor.getString(1), cursor.getString(2), cursor.getInt(3)))
            }
        }

        db().database.rawQuery(
            "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, is_unfiled " +
                "FROM live_entry WHERE subject_id = ? " +
                "ORDER BY occurred_start IS NULL, occurred_start DESC, created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    add(
                        TrailEntry(
                            id = id,
                            kind = cursor.getString(1),
                            title = cursor.getString(2),
                            body = cursor.getString(3),
                            occurredEdtf = cursor.getString(4),
                            occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                            createdAt = cursor.getLong(6),
                            isUnfiled = cursor.getInt(7) == 1,
                            threads = threadsByEntry[id].orEmpty(),
                        ),
                    )
                }
            }
        }
    }

    /** One person on the care team, with everything a row about them shows. */
    data class Person(
        val id: String,
        val displayName: String,
        val roleLabel: String?,
        val phone: String?,
        val email: String?,
        val notes: String?,
    )

    /**
     * The care team, in the order they were added.
     *
     * **Archived people are excluded and deleted people never reach here**,
     * the first by the `archived_at` test and the second by the live view. They
     * are different states: archiving is the person saying somebody is no
     * longer involved, deleting is them saying the row should not exist.
     */
    suspend fun people(subjectId: String): List<Person> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, display_name, role_label, phone, email, notes " +
                "FROM live_person WHERE subject_id = ? AND archived_at IS NULL " +
                "ORDER BY created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Person(
                            id = cursor.getString(0),
                            displayName = cursor.getString(1),
                            roleLabel = cursor.getString(2),
                            phone = cursor.getString(3),
                            email = cursor.getString(4),
                            notes = cursor.getString(5),
                        ),
                    )
                }
            }
        }
    }

    // -- the emergency card ------------------------------------------------

    /**
     * What the card holds, as `MASTER_SPEC.md` section 4.6 lists it.
     *
     * **Every field is nullable and that is the point.** The card is useful
     * with one line filled in and it is meant to be filled in over months, in
     * whatever order the person learns things. A card with only an allergy on
     * it is worth handing to a paramedic.
     *
     * The resuscitation status is stored as **what the signed paperwork says**,
     * alongside where the original is kept, because the card is useless if the
     * paper cannot be produced. The app records that sentence and never
     * interprets it, per rule 2.
     */
    data class EmergencyCard(
        val id: String,
        val allergies: String?,
        val bloodType: String?,
        val conditions: String?,
        val resuscitationStatus: String?,
        val resuscitationDocumentLocation: String?,
        val decisionMakerDocumentLocation: String?,
        val insuranceNote: String?,
        val otherNotes: String?,
    ) {
        /**
         * True when nothing has been written yet. A card row can exist and hold
         * nothing, because the row is created the first time somebody opens the
         * editor rather than the first time they finish it.
         */
        val isEmpty: Boolean
            get() = listOf(
                allergies, bloodType, conditions, resuscitationStatus,
                resuscitationDocumentLocation, decisionMakerDocumentLocation,
                insuranceNote, otherNotes,
            ).all { it.isNullOrBlank() }
    }

    /** The card for one subject, or null when none has ever been started. */
    suspend fun emergencyCard(subjectId: String): EmergencyCard? =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, allergies, blood_type, conditions, resuscitation_status, " +
                    "resuscitation_document_location, decision_maker_document_location, " +
                    "insurance_note, other_notes FROM live_emergency_card " +
                    "WHERE subject_id = ? ORDER BY created_at LIMIT 1",
                arrayOf(subjectId),
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                EmergencyCard(
                    id = cursor.getString(0),
                    allergies = cursor.getString(1),
                    bloodType = cursor.getString(2),
                    conditions = cursor.getString(3),
                    resuscitationStatus = cursor.getString(4),
                    resuscitationDocumentLocation = cursor.getString(5),
                    decisionMakerDocumentLocation = cursor.getString(6),
                    insuranceNote = cursor.getString(7),
                    otherNotes = cursor.getString(8),
                )
            }
        }

    /**
     * Writes the card, creating it if this is the first time.
     *
     * **One card per subject**, which the read enforces by taking the earliest
     * and this enforces by updating the one that exists. The schema does not
     * constrain it to one, because a second arriving from a peer during a sync
     * is a conflict to resolve rather than a write to reject.
     *
     * Blank is a real answer and clears the field, so somebody who wrote down
     * an allergy that turned out to be wrong can take it back out. Blanks are
     * stored as null rather than as empty strings, so "never filled in" and
     * "emptied on purpose" read the same way to everything downstream, which is
     * what the card wants: both mean the app knows nothing.
     */
    suspend fun saveEmergencyCard(
        subjectId: String,
        allergies: String? = null,
        bloodType: String? = null,
        conditions: String? = null,
        resuscitationStatus: String? = null,
        resuscitationDocumentLocation: String? = null,
        decisionMakerDocumentLocation: String? = null,
        insuranceNote: String? = null,
        otherNotes: String? = null,
    ): String {
        // Typed as Any? rather than String?, so the argument array below is
        // Array<Any?> and not an inferred intersection of the value types.
        val values: Map<String, Any?> = mapOf(
            "allergies" to allergies?.ifBlank { null },
            "blood_type" to bloodType?.ifBlank { null },
            "conditions" to conditions?.ifBlank { null },
            "resuscitation_status" to resuscitationStatus?.ifBlank { null },
            "resuscitation_document_location" to
                resuscitationDocumentLocation?.ifBlank { null },
            "decision_maker_document_location" to
                decisionMakerDocumentLocation?.ifBlank { null },
            "insurance_note" to insuranceNote?.ifBlank { null },
            "other_notes" to otherNotes?.ifBlank { null },
        )

        val existing = emergencyCard(subjectId)
            ?: return insert("emergency_card", values + mapOf("subject_id" to subjectId))

        withContext(Dispatchers.IO) {
            val assignments = values.keys.joinToString(", ") { "$it = ?" }
            db().database.execSQL(
                "UPDATE emergency_card SET $assignments, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                (values.values + listOf(System.currentTimeMillis(), existing.id)).toTypedArray(),
            )
        }
        return existing.id
    }

    /**
     * One person to call first, as the card carries them.
     *
     * **The name and number are copied onto the card rather than read through
     * the link every time.** `personId` records who this came from so the two
     * can be kept together, but the card holds its own copy, because a card
     * that goes blank when somebody archives a care team row is a card that
     * fails at the worst possible moment.
     */
    data class EmergencyContact(
        val id: String,
        val personId: String?,
        val displayName: String,
        val phone: String?,
        val relationship: String?,
    )

    /** Who to call first, in the order they were added to the card. */
    suspend fun emergencyContacts(cardId: String): List<EmergencyContact> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, person_id, display_name, phone, relationship " +
                    "FROM live_emergency_contact WHERE emergency_card_id = ? " +
                    "ORDER BY sort_index, created_at",
                arrayOf(cardId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            EmergencyContact(
                                id = cursor.getString(0),
                                personId = cursor.getString(1),
                                displayName = cursor.getString(2),
                                phone = cursor.getString(3),
                                relationship = cursor.getString(4),
                            ),
                        )
                    }
                }
            }
        }

    /** Puts somebody on the card, copying what is known about them onto it. */
    suspend fun addEmergencyContact(
        cardId: String,
        personId: String?,
        displayName: String,
        phone: String?,
        relationship: String?,
        sortIndex: Int,
    ): String = insert(
        "emergency_contact",
        mapOf(
            "emergency_card_id" to cardId,
            "person_id" to personId,
            "display_name" to displayName,
            "phone" to phone?.ifBlank { null },
            "relationship" to relationship?.ifBlank { null },
            "sort_index" to sortIndex,
        ),
    )

    /**
     * Takes somebody off the card.
     *
     * A tombstone like every other deletion, per the data contract, so removing
     * somebody from the card never removes them from the care team and the
     * removal itself travels to a peer.
     */
    suspend fun removeEmergencyContact(contactId: String) =
        withContext(Dispatchers.IO) {
            db().database.execSQL(
                "UPDATE emergency_contact SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    contactId,
                ),
            )
        }

    // -- medications --------------------------------------------------------

    /**
     * One medication, as the record holds it.
     *
     * **The dose is text and is never parsed.** The schema says so and this
     * follows it: what the person was told, in the words they were told it in.
     * Misparsing a dose is worse than not parsing one, and there is nothing the
     * app could do with a number that it cannot do with the sentence.
     */
    data class Medication(
        val id: String,
        val name: String,
        val doseText: String?,
        val purposeText: String?,
        val notes: String?,
        val onEmergencyCard: Boolean,
        val stoppedEdtf: String?,
    ) {
        /**
         * A medication that has been stopped is kept, not removed. Its whole
         * history is the point, and "she was on this until March" is the answer
         * to a question somebody will eventually be asked.
         */
        val isStopped: Boolean get() = !stoppedEdtf.isNullOrBlank()
    }

    /** Everything recorded for one subject, still being taken first. */
    suspend fun medications(subjectId: String): List<Medication> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, name, dose_text, purpose_text, notes, on_emergency_card, " +
                    "stopped_edtf FROM live_medication WHERE subject_id = ? " +
                    "ORDER BY stopped_edtf IS NOT NULL, created_at",
                arrayOf(subjectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            Medication(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                doseText = cursor.getString(2),
                                purposeText = cursor.getString(3),
                                notes = cursor.getString(4),
                                onEmergencyCard = cursor.getInt(5) == 1,
                                stoppedEdtf = cursor.getString(6),
                            ),
                        )
                    }
                }
            }
        }

    /** Records a medication. Only the name is needed, and even it may be thin. */
    suspend fun createMedication(
        subjectId: String,
        name: String,
        doseText: String? = null,
        purposeText: String? = null,
        notes: String? = null,
        onEmergencyCard: Boolean = false,
    ): String = insert(
        "medication",
        mapOf(
            "subject_id" to subjectId,
            "name" to name,
            "dose_text" to doseText?.ifBlank { null },
            "purpose_text" to purposeText?.ifBlank { null },
            "notes" to notes?.ifBlank { null },
            "on_emergency_card" to if (onEmergencyCard) 1 else 0,
        ),
    )

    /**
     * Puts a medication on the emergency card, or takes it off.
     *
     * **The flag lives on the medication rather than on the card**, which is
     * what the schema chose and what makes the link work both ways: the
     * medication knows it is on the card, and the card is assembled from the
     * medications that say so. Neither has to be kept in step with the other.
     */
    suspend fun setMedicationOnEmergencyCard(medicationId: String, onCard: Boolean) =
        withContext(Dispatchers.IO) {
            db().database.execSQL(
                "UPDATE medication SET on_emergency_card = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(
                    if (onCard) 1 else 0,
                    System.currentTimeMillis(),
                    medicationId,
                ),
            )
        }

    // -- projects --------------------------------------------------------------

    /**
     * One long process the family is running.
     *
     * **What it is waiting on is a first class field, not a note.** The schema
     * says so in its own comment: these processes stall on other people
     * constantly, and "waiting on the caseworker since the 3rd" is the single
     * most useful thing the app can hold about one.
     */
    data class Project(
        val id: String,
        val name: String,
        val templateId: String?,
        val status: String,
        val waitingOn: String?,
        val notes: String?,
        val stepCount: Int,
        val doneCount: Int,
    ) {
        val isFinished: Boolean get() = status == "done" || status == "abandoned"
    }

    /** One step of a project, in the order the template gave. */
    data class ProjectStep(
        val id: String,
        val text: String,
        val completedEdtf: String?,
        val note: String?,
    ) {
        val isDone: Boolean get() = !completedEdtf.isNullOrBlank()
    }

    /** Every project, unfinished first, with how many steps each has. */
    suspend fun projects(subjectId: String): List<Project> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT p.id, p.name, p.template_id, p.status, p.waiting_on, p.notes, " +
                "COUNT(s.id), COUNT(s.completed_edtf) " +
                "FROM live_project p " +
                "LEFT JOIN live_project_step s ON s.project_id = p.id " +
                "WHERE p.subject_id = ? " +
                "GROUP BY p.id, p.name, p.template_id, p.status, p.waiting_on, " +
                "p.notes, p.created_at " +
                "ORDER BY p.status IN ('done', 'abandoned'), p.created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Project(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            templateId = cursor.getString(2),
                            status = cursor.getString(3),
                            waitingOn = cursor.getString(4),
                            notes = cursor.getString(5),
                            stepCount = cursor.getInt(6),
                            doneCount = cursor.getInt(7),
                        ),
                    )
                }
            }
        }
    }

    /** The steps of one project, in order. */
    suspend fun projectSteps(projectId: String): List<ProjectStep> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, text, completed_edtf, note FROM live_project_step " +
                    "WHERE project_id = ? ORDER BY sort_index, created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ProjectStep(
                                id = cursor.getString(0),
                                text = cursor.getString(1),
                                completedEdtf = cursor.getString(2),
                                note = cursor.getString(3),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Starts a project from a template, with all of its steps, in one
     * transaction.
     *
     * **Both or neither.** A project with no steps is an empty shell of the one
     * thing that made the template worth having, and steps with no project
     * belong to nothing.
     *
     * The step text is copied rather than referenced, so editing the catalog
     * later never rewrites a process somebody is halfway through.
     */
    suspend fun startProject(
        subjectId: String,
        templateId: String,
        name: String,
        steps: List<String>,
    ): String = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val projectId = insertRow(
                "project",
                mapOf(
                    "subject_id" to subjectId,
                    "name" to name,
                    "template_id" to templateId,
                    "status" to "active",
                ) + dateColumns("started", Edtf.day(LocalDate.now())),
            )
            steps.forEachIndexed { index, text ->
                insertRow(
                    "project_step",
                    mapOf(
                        "project_id" to projectId,
                        "text" to text,
                        "sort_index" to index,
                    ),
                )
            }
            database.setTransactionSuccessful()
            projectId
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Marks a step done, or puts it back.
     *
     * **Putting it back is as ordinary as marking it done.** These processes go
     * backward constantly: a form is returned, a document is rejected, a step
     * turns out to have been done wrong. A checklist that only moves forward
     * would make the person lie to it.
     */
    suspend fun setProjectStepDone(stepId: String, done: Boolean) =
        withContext(Dispatchers.IO) {
            val columns = if (done) {
                dateColumns("completed", Edtf.day(LocalDate.now()))
            } else {
                mapOf<String, Any?>(
                    "completed_edtf" to null,
                    "completed_zone" to null,
                    "completed_start" to null,
                    "completed_end" to null,
                )
            }
            val assignments = columns.keys.joinToString(", ") { "$it = ?" }
            db().database.execSQL(
                "UPDATE project_step SET $assignments, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                (columns.values + listOf(System.currentTimeMillis(), stepId)).toTypedArray(),
            )
        }

    /** Changes a project's status, and what it is waiting on. */
    suspend fun setProjectStatus(
        projectId: String,
        status: String,
        waitingOn: String?,
    ) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE project SET status = ?, waiting_on = ?, waiting_since = ?, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                status,
                waitingOn?.ifBlank { null },
                if (status == "waiting") System.currentTimeMillis() else null,
                System.currentTimeMillis(),
                projectId,
            ),
        )
    }

    // -- documents -------------------------------------------------------------

    /**
     * One document, with the attachment that carries its image.
     *
     * **`originalLocation` is the field that makes this section worth having.**
     * The schema says so in its own comment: the digital copy is rarely the one
     * a clerk will accept, so where the paper physically is matters more than
     * the photograph does.
     */
    data class Document(
        val id: String,
        val title: String,
        val category: String?,
        val originalLocation: String?,
        val notes: String?,
        val receivedEdtf: String?,
        /** Null when the document was recorded without a photograph. */
        val sha256: String?,
        val byteSize: Long?,
    )

    /** Every document, most recently received first. */
    suspend fun documents(subjectId: String): List<Document> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT d.id, d.title, d.category, d.original_location, d.notes, " +
                "d.received_edtf, a.sha256, a.byte_size " +
                "FROM live_document d " +
                "LEFT JOIN live_attachment a ON a.document_id = d.id " +
                "WHERE d.subject_id = ? " +
                "ORDER BY d.received_start IS NULL, d.received_start DESC, d.created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Document(
                            id = cursor.getString(0),
                            title = cursor.getString(1),
                            category = cursor.getString(2),
                            originalLocation = cursor.getString(3),
                            notes = cursor.getString(4),
                            receivedEdtf = cursor.getString(5),
                            sha256 = cursor.getString(6),
                            byteSize = if (cursor.isNull(7)) null else cursor.getLong(7),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Records a document and the file that carries it, in one transaction.
     *
     * **Both or neither.** A document row whose attachment is missing points at
     * a file that is not there, and an attachment with no document is a file
     * nothing can reach. The bytes are already on disk by this point, written
     * content addressed by [Attachments], so this is only the rows.
     *
     * `sha256` null records a document nobody photographed, which is a real
     * case: knowing the discharge summary exists and lives in the blue folder
     * is worth writing down before there is a picture of it.
     */
    suspend fun createDocument(
        subjectId: String,
        title: String,
        received: Edtf.Date,
        originalLocation: String? = null,
        notes: String? = null,
        sha256: String? = null,
        byteSize: Long = 0,
        mimeType: String? = null,
        originalFilename: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val documentId = insertRow(
                "document",
                mapOf(
                    "subject_id" to subjectId,
                    "title" to title,
                    "original_location" to originalLocation?.ifBlank { null },
                    "notes" to notes?.ifBlank { null },
                ) + dateColumns("received", received),
            )
            if (sha256 != null) {
                insertRow(
                    "attachment",
                    mapOf(
                        "sha256" to sha256,
                        "original_filename" to originalFilename,
                        "mime_type" to mimeType,
                        "byte_size" to byteSize,
                        "document_id" to documentId,
                    ),
                )
            }
            database.setTransactionSuccessful()
            documentId
        } finally {
            database.endTransaction()
        }
    }

    // -- money ----------------------------------------------------------------

    /**
     * One bill.
     *
     * **The amount is minor units as an integer and never a floating point
     * number.** The schema says so and the reason is in its comment: floating
     * point money is a defect waiting for a rounding boundary, and this record
     * may be read out in a dispute. Null means the bill has not said an amount
     * yet, which happens constantly and is not the same as zero.
     */
    data class Bill(
        val id: String,
        val description: String,
        val amountMinor: Long?,
        val currency: String,
        val state: String,
        val stateNote: String?,
        val receivedEdtf: String?,
        val notes: String?,
    ) {
        /**
         * Whether this is still hanging over somebody.
         *
         * Paid and closed are settled. Everything else is not, and that is the
         * set the total above the list adds up.
         */
        val isOpen: Boolean get() = state != "paid" && state != "closed"
    }

    /** Every bill, most recently received first. */
    suspend fun bills(subjectId: String): List<Bill> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, description, amount_minor, currency, state, state_note, " +
                "received_edtf, notes FROM live_bill WHERE subject_id = ? " +
                "ORDER BY received_start IS NULL, received_start DESC, created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Bill(
                            id = cursor.getString(0),
                            description = cursor.getString(1),
                            amountMinor = if (cursor.isNull(2)) null else cursor.getLong(2),
                            currency = cursor.getString(3),
                            state = cursor.getString(4),
                            stateNote = cursor.getString(5),
                            receivedEdtf = cursor.getString(6),
                            notes = cursor.getString(7),
                        ),
                    )
                }
            }
        }
    }

    /** Records a bill. Only the description is required. */
    suspend fun createBill(
        subjectId: String,
        description: String,
        amountMinor: Long?,
        state: String,
        received: Edtf.Date,
        notes: String? = null,
    ): String = insert(
        "bill",
        mapOf(
            "subject_id" to subjectId,
            "description" to description,
            "amount_minor" to amountMinor,
            "state" to state,
            "notes" to notes?.ifBlank { null },
        ) + dateColumns("received", received),
    )

    /** Changes what is recorded about a bill, including its state. */
    suspend fun updateBill(
        billId: String,
        description: String,
        amountMinor: Long?,
        state: String,
        notes: String?,
    ) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE bill SET description = ?, amount_minor = ?, state = ?, notes = ?, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                description,
                amountMinor,
                state,
                notes?.ifBlank { null },
                System.currentTimeMillis(),
                billId,
            ),
        )
    }

    /** Changes what is recorded about an appointment, including when it is. */
    suspend fun updateAppointment(
        appointmentId: String,
        title: String,
        scheduled: Edtf.Date,
        locationNote: String?,
        notes: String?,
    ) = withContext(Dispatchers.IO) {
        val columns = dateColumns("scheduled", scheduled) + mapOf(
            "title" to title,
            "location_note" to locationNote?.ifBlank { null },
            "notes" to notes?.ifBlank { null },
        )
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.execSQL(
            "UPDATE appointment SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), appointmentId)).toTypedArray(),
        )
    }

    /**
     * Changes what is recorded about a document.
     *
     * **The photograph is not touched.** Replacing it is a different action
     * with different consequences, since the old file may be shared with
     * another row by its hash, and conflating the two here would make a text
     * correction quietly capable of losing an image.
     */
    suspend fun updateDocument(
        documentId: String,
        title: String,
        originalLocation: String?,
        notes: String?,
    ) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE document SET title = ?, original_location = ?, notes = ?, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                title,
                originalLocation?.ifBlank { null },
                notes?.ifBlank { null },
                System.currentTimeMillis(),
                documentId,
            ),
        )
    }

    /** Moves a bill to another state. Nothing else about it changes. */
    suspend fun setBillState(billId: String, state: String) = withContext(Dispatchers.IO) {
        db().database.execSQL(
            "UPDATE bill SET state = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(state, System.currentTimeMillis(), billId),
        )
    }

    // -- standing instructions ------------------------------------------------

    /**
     * One thing this family has asked for, as recorded.
     *
     * **`tag` is never null and never empty**, because the schema constrains it
     * to `federal` or `request` and because an instruction rendered without it
     * would let the app imply a right that may not exist.
     */
    data class StandingInstruction(
        val id: String,
        val name: String,
        val wording: String,
        val tag: String,
        val givenEdtf: String?,
        val acknowledgedEdtf: String?,
        val acknowledgedHow: String?,
        val notes: String?,
    ) {
        val isAcknowledged: Boolean get() = !acknowledgedEdtf.isNullOrBlank()
    }

    /**
     * Records how a facility answered a standing instruction.
     *
     * **This is the half that gets pointed back to.** `MASTER_SPEC.md` section
     * 4.3 asks for what was asked, of whom, when, and how it was acknowledged,
     * and only the first three could be held. "I asked, and the charge nurse
     * put it in the care plan on the 4th" is the sentence that settles an
     * argument months later.
     *
     * The date is stamped when the person records it rather than asked for, on
     * the same reasoning as marking a question asked: they are writing it down
     * because it just happened. It stays editable like every other date.
     */
    suspend fun setInstructionAcknowledged(
        instructionId: String,
        how: String?,
        acknowledged: Edtf.Date,
    ) = withContext(Dispatchers.IO) {
        val columns = dateColumns("acknowledged", acknowledged) +
            mapOf("acknowledged_how" to how?.ifBlank { null })
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.execSQL(
            "UPDATE standing_instruction SET $assignments, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), instructionId)).toTypedArray(),
        )
    }

    /** Everything asked for, most recently recorded first. */
    suspend fun standingInstructions(subjectId: String): List<StandingInstruction> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, name, wording, tag, given_edtf, acknowledged_edtf, " +
                    "acknowledged_how, notes FROM live_standing_instruction " +
                    "WHERE subject_id = ? ORDER BY created_at DESC",
                arrayOf(subjectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            StandingInstruction(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                wording = cursor.getString(2),
                                tag = cursor.getString(3),
                                givenEdtf = cursor.getString(4),
                                acknowledgedEdtf = cursor.getString(5),
                                acknowledgedHow = cursor.getString(6),
                                notes = cursor.getString(7),
                            ),
                        )
                    }
                }
            }
        }

    /** Records that this was asked for, on the day it was asked. */
    suspend fun createStandingInstruction(
        subjectId: String,
        templateId: String?,
        name: String,
        wording: String,
        tag: String,
        given: Edtf.Date,
        notes: String? = null,
    ): String = insert(
        "standing_instruction",
        mapOf(
            "subject_id" to subjectId,
            "template_id" to templateId,
            "name" to name,
            "wording" to wording,
            "tag" to tag,
            "notes" to notes?.ifBlank { null },
        ) + dateColumns("given", given),
    )

    // -- appointments ---------------------------------------------------------

    /**
     * One appointment.
     *
     * **Upcoming and past are decided by the date rather than by a flag.** The
     * schema keeps `attended_*` for what actually happened, which is a
     * different question from whether the date has passed, and conflating the
     * two would mean an appointment nobody attended quietly stays "coming up"
     * forever.
     */
    data class Appointment(
        val id: String,
        val title: String,
        val scheduledEdtf: String?,
        val scheduledStart: Long?,
        val locationNote: String?,
        val notes: String?,
    )

    /** Every appointment, soonest first. */
    suspend fun appointments(subjectId: String): List<Appointment> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, title, scheduled_edtf, scheduled_start, location_note, notes " +
                    "FROM live_appointment WHERE subject_id = ? " +
                    "ORDER BY scheduled_start IS NULL, scheduled_start",
                arrayOf(subjectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            Appointment(
                                id = cursor.getString(0),
                                title = cursor.getString(1),
                                scheduledEdtf = cursor.getString(2),
                                scheduledStart =
                                    if (cursor.isNull(3)) null else cursor.getLong(3),
                                locationNote = cursor.getString(4),
                                notes = cursor.getString(5),
                            ),
                        )
                    }
                }
            }
        }

    /** Records an appointment. Only the title is required, and the date may be coarse. */
    suspend fun createAppointment(
        subjectId: String,
        title: String,
        scheduled: Edtf.Date,
        locationNote: String? = null,
        notes: String? = null,
    ): String = insert(
        "appointment",
        mapOf(
            "subject_id" to subjectId,
            "title" to title,
            "location_note" to locationNote?.ifBlank { null },
            "notes" to notes?.ifBlank { null },
        ) + dateColumns("scheduled", scheduled),
    )

    // -- chapters, the places -------------------------------------------------

    /**
     * One place, and when they were there.
     *
     * `endedEdtf` null means this is where they are now, which is what makes
     * the current chapter identifiable without a separate flag that could
     * disagree with the dates.
     */
    data class Chapter(
        val id: String,
        val name: String,
        val reason: String?,
        val notes: String?,
        val startedEdtf: String?,
        val endedEdtf: String?,
    ) {
        val isCurrent: Boolean get() = endedEdtf.isNullOrBlank()
    }

    /** Every place, most recent first. */
    suspend fun chapters(subjectId: String): List<Chapter> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, name, reason, notes, started_edtf, ended_edtf " +
                "FROM live_chapter WHERE subject_id = ? " +
                "ORDER BY started_start IS NULL, started_start DESC, created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Chapter(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            reason = cursor.getString(2),
                            notes = cursor.getString(3),
                            startedEdtf = cursor.getString(4),
                            endedEdtf = cursor.getString(5),
                        ),
                    )
                }
            }
        }
    }

    // -- readings, for the progress section ----------------------------------

    /**
     * One reading, exactly as it was recorded.
     *
     * **The number is carried as it was entered and never interpreted.** No
     * range, no threshold, no comparison to a previous value. `MASTER_SPEC.md`
     * section 4.3 and rule 2: the app records and counts, it never concludes.
     *
     * `source` is kept because a value the family measured and a value a
     * clinician stated are different things and the record must not blur them.
     * The schema says so in a comment and this is the read that honors it.
     */
    data class Reading(
        val id: String,
        val measureId: String,
        val number: Double?,
        val text: String?,
        val unit: String?,
        val occurredEdtf: String?,
        val occurredStart: Long?,
        val note: String?,
        val source: String?,
    )

    /**
     * Every reading for one subject, newest first within each measure.
     *
     * Read in one query across all measures rather than per measure, and
     * grouped by the caller, for the same reason the thread counts are: a
     * screen whose content is a series should not issue a query per series.
     *
     * **An unknown date sorts last**, exactly as the trail does, because
     * placing a reading with no date at zero or at today would put it somewhere
     * on the series the person never put it.
     */
    suspend fun readings(subjectId: String): List<Reading> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT m.id, m.measure_id, m.value_number, m.value_text, m.unit, " +
                "m.occurred_edtf, m.occurred_start, m.note, m.source " +
                "FROM live_measurement m " +
                "JOIN live_measure me ON me.id = m.measure_id " +
                "WHERE me.subject_id = ? " +
                "ORDER BY m.occurred_start IS NULL, m.occurred_start DESC, m.created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Reading(
                            id = cursor.getString(0),
                            measureId = cursor.getString(1),
                            number = if (cursor.isNull(2)) null else cursor.getDouble(2),
                            text = cursor.getString(3),
                            unit = cursor.getString(4),
                            occurredEdtf = cursor.getString(5),
                            occurredStart = if (cursor.isNull(6)) null else cursor.getLong(6),
                            note = cursor.getString(7),
                            source = cursor.getString(8),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Records what came back.
     *
     * **The answer is the other half of the record.** "We asked in March" is
     * worth something; "we asked in March and were told it would be reviewed at
     * the next care plan meeting" is the thing somebody actually needs six
     * months later, and until now the app could hold only the first half.
     *
     * It never touches whether the question was asked or when. Somebody
     * correcting what they were told is not un-asking the question.
     */
    suspend fun setQuestionAnswer(questionId: String, answerText: String?) =
        withContext(Dispatchers.IO) {
            db().database.execSQL(
                "UPDATE question SET answer_text = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(
                    answerText?.ifBlank { null },
                    System.currentTimeMillis(),
                    questionId,
                ),
            )
        }

    // -- care threads --------------------------------------------------------

    /** A thread, with how much of the record runs through it. */
    data class ThreadWithCount(val thread: CareThread, val entryCount: Int)

    /**
     * The threads and what is on each, in the order the person sees them.
     *
     * **Counted with a join rather than a query per thread**, because this is a
     * screen whose whole content is counts and one query per row is how a list
     * gets slow on the notebooks that have been kept longest.
     *
     * A thread with nothing on it is still returned. Applying a situation
     * template creates several at once, and most of them are empty on day one:
     * they are places the record will go, not places it has been.
     */
    suspend fun threadsWithCounts(subjectId: String): List<ThreadWithCount> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT t.id, t.label, t.color_index, COUNT(et.id) " +
                    "FROM live_care_thread t " +
                    "LEFT JOIN live_entry_thread et ON et.thread_id = t.id " +
                    "LEFT JOIN live_entry e ON e.id = et.entry_id " +
                    "WHERE t.subject_id = ? " +
                    "GROUP BY t.id, t.label, t.color_index, t.sort_index, t.created_at " +
                    "ORDER BY t.sort_index, t.created_at",
                arrayOf(subjectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ThreadWithCount(
                                thread = CareThread(
                                    id = cursor.getString(0),
                                    label = cursor.getString(1),
                                    colorIndex = cursor.getInt(2),
                                ),
                                entryCount = cursor.getInt(3),
                            ),
                        )
                    }
                }
            }
        }

    // -- questions to ask next time ------------------------------------------

    /**
     * One thing to ask, and whether it has been asked yet.
     *
     * `askedEdtf` null means still waiting, which is what the schema's partial
     * index keys off and what "open" means everywhere in the app.
     */
    data class Question(
        val id: String,
        val text: String,
        val roleLabel: String?,
        val entryId: String?,
        val askedEdtf: String?,
        val answerText: String?,
    ) {
        val isOpen: Boolean get() = askedEdtf.isNullOrBlank()
    }

    /** Everything to ask, still waiting first, oldest first within each group. */
    suspend fun questions(subjectId: String): List<Question> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, text, role_label, entry_id, asked_edtf, answer_text " +
                "FROM live_question WHERE subject_id = ? " +
                "ORDER BY asked_edtf IS NOT NULL, created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Question(
                            id = cursor.getString(0),
                            text = cursor.getString(1),
                            roleLabel = cursor.getString(2),
                            entryId = cursor.getString(3),
                            askedEdtf = cursor.getString(4),
                            answerText = cursor.getString(5),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Records a question and the trail entry that carries it, in one transaction.
     *
     * **Both or neither.** Capturing "ask next time" was writing only the entry,
     * so the question appeared in the trail and the Ask next time section
     * counted zero forever. A section that counts nothing while the thing it
     * counts is being captured is the app being wrong about itself, which is
     * worse than the section not existing.
     *
     * The question keeps `entry_id` so it holds onto what prompted it months
     * later, which is what the schema comment asks for and what makes the link
     * work in both directions.
     */
    suspend fun createQuestionWithEntry(
        subjectId: String,
        text: String,
        /**
         * Who the question is for, kept in its own column rather than folded
         * into the text. The schema wants it that way so a question waiting for
         * the wound nurse does not turn up on the prep sheet for a billing
         * meeting, and the screen reads it as the eyebrow above the question.
         */
        roleLabel: String?,
        occurred: Edtf.Date,
        threadId: String?,
        isUnfiled: Boolean,
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val entryId = insertRow(
                "entry",
                mapOf(
                    "subject_id" to subjectId,
                    "kind" to "question",
                    // The trail shows who it is for as the entry's subject and
                    // the question itself as the body, which is how the other
                    // five kinds already read.
                    "title" to roleLabel?.ifBlank { null },
                    "body" to text.ifBlank { null },
                    "is_unfiled" to if (isUnfiled) 1 else 0,
                ) + dateColumns("occurred", occurred),
            )
            if (threadId != null) {
                insertRow(
                    "entry_thread",
                    mapOf("entry_id" to entryId, "thread_id" to threadId),
                )
            }
            val questionId = insertRow(
                "question",
                mapOf(
                    "subject_id" to subjectId,
                    // The schema requires text, and an empty question is not a
                    // question. The caller checks first; this is the backstop.
                    "text" to text.ifBlank { "" },
                    "role_label" to roleLabel?.ifBlank { null },
                    "entry_id" to entryId,
                ),
            )
            database.setTransactionSuccessful()
            entryId to questionId
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Marks a question as asked, on the day the person says it was asked.
     *
     * It is never removed. What was asked and what came back is the record, and
     * "we asked in March and were told it would be reviewed" is exactly the
     * kind of thing somebody needs six months later.
     */
    suspend fun markQuestionAsked(
        questionId: String,
        asked: Edtf.Date,
        answerText: String? = null,
    ) = withContext(Dispatchers.IO) {
        val columns = dateColumns("asked", asked) +
            mapOf("answer_text" to answerText?.ifBlank { null })
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.execSQL(
            "UPDATE question SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), questionId)).toTypedArray(),
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

    // ---- search --------------------------------------------------------

    /**
     * One thing the search found.
     *
     * **Every result carries its chapter**, per `MASTER_SPEC.md` 4.8, so the
     * person always knows where in the journey it happened. A result with no
     * chapter says so by leaving it null rather than by inventing one: plenty
     * of things are recorded before anybody has said where they belong, and
     * "Unfiled" would be a claim the record does not make.
     */
    data class SearchHit(
        val id: String,
        /** Which notebook section this lives in, so results group where the person expects. */
        val section: Section,
        val title: String,
        /**
         * What kind of entry this is, for the trail's results only.
         *
         * A trail row with no title falls back to its kind, "A call" or "A
         * visit", because the kind is what the app knows for certain and it
         * tells the person something. A search result was falling back to "No
         * title", which tells them nothing and disagreed with the same row on
         * the trail.
         */
        val kind: String?,
        /** The line under the title, which is usually where the match was found. */
        val detail: String?,
        val chapterName: String?,
        /** The EDTF string of when it happened, for the ones that happened. */
        val occurredEdtf: String?,
        /** Sorts most recent first within a section. Null sorts last. */
        val occurredStart: Long?,
    )

    /**
     * Everything matching what the person typed, grouped by section.
     *
     * **The search a years long notebook is unusable without.** By year two
     * there are hundreds of entries, and "the call where they said the wound
     * was healing" is somewhere in them. `MASTER_SPEC.md` 4.8.
     *
     * **Plain substring matching, case folded, and nothing cleverer.** No
     * stemming, no fuzzy distance, no ranking by relevance. A caregiver
     * searching for "Dr Okonkwo" wants the rows containing those letters, and a
     * clever matcher that helpfully returns something else has hidden the row
     * they were looking for. The one accommodation is case, because nobody
     * types a facility's capitalization from memory.
     *
     * **Read through the live views**, so anything deleted is already gone
     * rather than filtered here, which is what `check_live_views.py` enforces.
     *
     * Returns at most [limit] per section. A person with four hundred matching
     * entries is not helped by four hundred rows, and the sections that
     * overflow say so on screen rather than truncating quietly.
     */
    suspend fun search(
        subjectId: String,
        query: String,
        limit: Int = 50,
    ): List<SearchHit> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        // LIKE with the pattern's own characters escaped, so a person searching
        // for a literal percent sign or underscore, which appear in doses and
        // in file names, gets what they asked for rather than a wildcard.
        val pattern = "%" + trimmed.lowercase()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_") + "%"

        val hits = mutableListOf<SearchHit>()
        // Resolved once, outside the local function, because opening the
        // database is a suspending call and this runs ten queries.
        val database = db().database

        fun run(
            section: Section,
            table: String,
            titleColumn: String,
            detailColumn: String?,
            dateColumn: String?,
            searched: List<String>,
            /**
             * Whether this table has a `chapter_id` of its own.
             *
             * Stated per table rather than assumed, because three of the ten do
             * not: a person belongs to chapters through `person_chapter`, a
             * medication deliberately crosses them, and a question hangs off an
             * entry rather than a place.
             */
            hasChapter: Boolean = true,
            /**
             * The column naming what kind of thing it is, where there is one.
             *
             * Last, and named at every call site, because the parameters before
             * it are positional and inserting one in the middle silently
             * reassigns every argument after it.
             */
            kindColumn: String? = null,
        ) {
            val where = searched.joinToString(" OR ") {
                "lower(coalesce(x.$it, '')) LIKE ? ESCAPE '\\'"
            }
            val date = dateColumn?.let { "x.${it}_edtf, x.${it}_start" } ?: "NULL, NULL"
            val kind = kindColumn?.let { "x.$it" } ?: "NULL"
            val detail = detailColumn?.let { "x.$it" } ?: "NULL"
            // **Not every table carries a chapter, and the ones that do not
            // are not an oversight.** A person belongs to chapters through
            // `person_chapter`, because somebody can be on the care team across
            // several stays, and a medication crosses chapters by design, which
            // `MASTER_SPEC.md` 4.8 calls a medication's journey. Joining
            // `x.chapter_id` blindly threw "no such column" for those three and
            // took the whole search down with them.
            val chapter = when {
                table == "live_chapter" -> "x.name"
                hasChapter -> "c.name"
                else -> "NULL"
            }
            val join = if (hasChapter) "LEFT JOIN live_chapter c ON c.id = x.chapter_id " else ""
            database.rawQuery(
                "SELECT x.id, x.$titleColumn, $detail, $chapter, $date, $kind " +
                    "FROM $table x $join" +
                    "WHERE x.subject_id = ? AND ($where) " +
                    "ORDER BY x.created_at DESC LIMIT ?",
                arrayOf(subjectId) + Array(searched.size) { pattern } + arrayOf(limit.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    hits += SearchHit(
                        id = cursor.getString(0),
                        section = section,
                        title = cursor.getString(1) ?: "",
                        detail = cursor.getString(2),
                        chapterName = cursor.getString(3),
                        occurredEdtf = cursor.getString(4),
                        occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                        kind = cursor.getString(6),
                    )
                }
            }
        }

        // **In notebook order**, which is the order the person already learned,
        // rather than by how many matches each section produced. A list whose
        // order changes with the query is one nobody can build a habit against.
        run(Section.CARE_TEAM, "live_person", "display_name", "role_label", null,
            listOf("display_name", "role_label", "notes", "shift_note", "phone", "email"),
            hasChapter = false)
        run(Section.MEDICATIONS, "live_medication", "name", "dose_text", "started",
            listOf("name", "dose_text", "purpose_text", "notes", "stop_reason"),
            hasChapter = false)
        run(Section.APPOINTMENTS, "live_appointment", "title", "location_note", "scheduled",
            listOf("title", "location_note", "notes", "outcome_note"))
        run(Section.CHAPTERS, "live_chapter", "name", "reason", "started",
            listOf("name", "reason", "notes", "transfer_note"),
            hasChapter = false)
        run(Section.TRAIL, "live_entry", "title", "body", "occurred",
            listOf("title", "body", "suggested_home"), kindColumn = "kind")
        run(Section.DOCUMENTS, "live_document", "title", "original_location", "received",
            listOf("title", "original_location", "notes", "category"))
        run(Section.MONEY, "live_bill", "description", "state_note", "received",
            listOf("description", "state_note", "notes"))
        run(Section.STANDING_INSTRUCTIONS, "live_standing_instruction", "name", "wording", "given",
            listOf("name", "wording", "notes", "acknowledged_how"))
        run(Section.ASK_NEXT_TIME, "live_question", "text", "answer_text", "asked",
            listOf("text", "answer_text", "role_label"),
            hasChapter = false)
        run(Section.PROJECTS, "live_project", "name", "waiting_on", "started",
            listOf("name", "waiting_on", "notes"))

        hits
    }

    // ---- incidents -----------------------------------------------------

    /**
     * An incident, which is a thread rather than an event.
     *
     * `MASTER_SPEC.md` 4.7: from first report to resolution, with each call and
     * escalation as a node on it. **That is the whole point and it is why an
     * incident is not just an entry with a scary kind.** A fall, a missed
     * medication, or a wound that was not dressed is followed by calls, by
     * somebody promising to look into it, and eventually by an answer, and the
     * thing a caregiver needs six months later is the sequence rather than the
     * first line of it.
     */
    data class Incident(
        val id: String,
        val title: String,
        val description: String?,
        val reportedEdtf: String?,
        val reportedStart: Long?,
        /** Null while it is still open, which is the state that matters most. */
        val resolvedAt: Long?,
        val resolutionNote: String?,
        val chapterName: String?,
        /** How many entries hang off it, the first report included. */
        val entryCount: Int,
    ) {
        val isOpen: Boolean get() = resolvedAt == null
    }

    /**
     * Reports an incident, writing the incident and its first entry together.
     *
     * **Two rows in one transaction**, the same shape `createQuestionWithEntry`
     * uses and for the same reason: writing only the entry put a question in
     * the trail and left its own section counting zero forever, which is the
     * app being wrong about itself. An incident that exists only as an entry
     * can never be escalated, resolved, or exported as a thread.
     *
     * Returns the incident id and the entry id.
     */
    suspend fun reportIncident(
        subjectId: String,
        title: String,
        description: String?,
        occurred: Edtf.Date,
        threadId: String?,
        isUnfiled: Boolean,
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val incidentId = insertRow(
                "incident",
                mapOf(
                    "subject_id" to subjectId,
                    // The schema requires a title. An incident with no words is
                    // still an incident somebody wanted recorded, so it gets
                    // the description or a placeholder rather than being
                    // refused at the moment they are least able to argue.
                    "title" to title.ifBlank { description?.take(80) ?: "" },
                    "description" to description?.ifBlank { null },
                ) + dateColumns("reported", occurred),
            )
            val entryId = insertRow(
                "entry",
                mapOf(
                    "subject_id" to subjectId,
                    "kind" to "incident",
                    "title" to title.ifBlank { null },
                    "body" to description?.ifBlank { null },
                    "incident_id" to incidentId,
                    "is_unfiled" to if (isUnfiled) 1 else 0,
                ) + dateColumns("occurred", occurred),
            )
            if (threadId != null) {
                insertRow("entry_thread", mapOf("entry_id" to entryId, "thread_id" to threadId))
            }
            database.setTransactionSuccessful()
            incidentId to entryId
        } finally {
            database.endTransaction()
        }
    }

    /** Every incident for this subject, open ones first, then most recent. */
    suspend fun incidents(subjectId: String): List<Incident> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT i.id, i.title, i.description, i.reported_edtf, i.reported_start, " +
                "i.resolved_at, i.resolution_note, c.name, " +
                "(SELECT COUNT(*) FROM live_entry e WHERE e.incident_id = i.id) " +
                "FROM live_incident i " +
                "LEFT JOIN live_chapter c ON c.id = i.chapter_id " +
                "WHERE i.subject_id = ? " +
                // **Open first.** An incident nobody has answered is the thing
                // a caregiver is carrying around, and a list that buries it
                // under resolved ones by date is a list that forgot what it is
                // for. Never a judgment about how long it has been open.
                "ORDER BY (i.resolved_at IS NOT NULL), " +
                "coalesce(i.reported_start, i.created_at) DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Incident(
                            id = cursor.getString(0),
                            title = cursor.getString(1) ?: "",
                            description = cursor.getString(2),
                            reportedEdtf = cursor.getString(3),
                            reportedStart = if (cursor.isNull(4)) null else cursor.getLong(4),
                            resolvedAt = if (cursor.isNull(5)) null else cursor.getLong(5),
                            resolutionNote = cursor.getString(6),
                            chapterName = cursor.getString(7),
                            entryCount = cursor.getInt(8),
                        ),
                    )
                }
            }
        }
    }

    /** The entries on one incident, oldest first, which is the order it happened. */
    suspend fun incidentTrail(incidentId: String): List<TrailEntry> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, " +
                    "is_unfiled FROM live_entry WHERE incident_id = ? " +
                    // **Oldest first, which is the opposite of the trail.** The
                    // trail answers "what has been happening lately" and reads
                    // newest first. A thread answers "how did this go", and a
                    // story told backward is not the same story.
                    "ORDER BY coalesce(occurred_start, created_at) ASC",
                arrayOf(incidentId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            TrailEntry(
                                id = cursor.getString(0),
                                kind = cursor.getString(1),
                                title = cursor.getString(2),
                                body = cursor.getString(3),
                                occurredEdtf = cursor.getString(4),
                                occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                                createdAt = cursor.getLong(6),
                                isUnfiled = cursor.getInt(7) == 1,
                                threads = emptyList(),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Adds something that happened on an incident: a call, an escalation, a note.
     *
     * The node on the thread that `MASTER_SPEC.md` 4.7 asks for. It is an
     * ordinary entry, so it appears on the trail in its own right, and it
     * carries the incident so the thread can be read as one thing.
     */
    suspend fun addToIncident(
        subjectId: String,
        incidentId: String,
        kind: String,
        title: String?,
        body: String?,
        occurred: Edtf.Date,
    ): String = insert(
        "entry",
        mapOf(
            "subject_id" to subjectId,
            "kind" to kind,
            "title" to title?.ifBlank { null },
            "body" to body?.ifBlank { null },
            "incident_id" to incidentId,
        ) + dateColumns("occurred", occurred),
    )

    /**
     * Marks an incident resolved, with what the answer actually was.
     *
     * **Never removed and never hidden.** A resolved incident is the half of
     * the record that shows something was chased and answered, and "we raised
     * it in March and they changed the dressing schedule" is exactly what
     * somebody needs at the next care plan meeting.
     *
     * Passing null reopens it, because somebody who resolved the wrong one, or
     * whose answer turned out not to hold, must be able to say so.
     */
    suspend fun resolveIncident(
        incidentId: String,
        resolvedAt: Long?,
        resolutionNote: String? = null,
    ) = withContext(Dispatchers.IO) {
        // The same shape `markQuestionAsked` uses, so the change log trigger
        // sees an ordinary update and `rev` moves the way every other write
        // moves it.
        db().database.execSQL(
            "UPDATE incident SET resolved_at = ?, resolution_note = ?, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                resolvedAt,
                resolutionNote?.ifBlank { null },
                System.currentTimeMillis(),
                incidentId,
            ),
        )
    }

    /**
     * Hangs an entry that already exists onto an incident.
     *
     * Used when a capture was opened from a thread: the entry is written the
     * ordinary way, so it appears on the trail in its own right, and then it is
     * told which thread it belongs to. Rule 18, links go both ways.
     */
    suspend fun attachEntryToIncident(entryId: String, incidentId: String) =
        withContext(Dispatchers.IO) {
            db().database.execSQL(
                "UPDATE entry SET incident_id = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(incidentId, System.currentTimeMillis(), entryId),
            )
        }

    /** How many incidents are still open. Today shows this, and only counts it. */
    suspend fun openIncidentCount(subjectId: String): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT COUNT(*) FROM live_incident WHERE subject_id = ? AND resolved_at IS NULL",
            arrayOf(subjectId),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * One entry, with everything hanging off it.
     *
     * **Rule 18: links go both ways.** An entry knew its threads only because
     * the trail query gathered them in bulk, and it knew nothing at all about
     * its chapter or its incident, so there was no way to get from a thing that
     * happened to the thread it belonged to. That is a dead end wearing a
     * disguise, which is what #46 exists to remove.
     */
    data class EntryDetail(
        val entry: TrailEntry,
        /** Who this involved, when they are on the care team. Rule 18. */
        val people: List<Person>,
        val chapterId: String?,
        val chapterName: String?,
        val incidentId: String?,
        val incidentTitle: String?,
        val incidentIsOpen: Boolean,
    )

    /** One entry read on its own, or null when it is gone. */
    suspend fun entry(entryId: String): EntryDetail? = withContext(Dispatchers.IO) {
        val database = db().database
        val threads = mutableListOf<CareThread>()
        database.rawQuery(
            "SELECT t.id, t.label, t.color_index FROM live_entry_thread et " +
                "JOIN live_care_thread t ON t.id = et.thread_id " +
                "WHERE et.entry_id = ? ORDER BY t.sort_index, t.created_at",
            arrayOf(entryId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                threads += CareThread(cursor.getString(0), cursor.getString(1), cursor.getInt(2))
            }
        }

        database.rawQuery(
            "SELECT e.id, e.kind, e.title, e.body, e.occurred_edtf, e.occurred_start, " +
                "e.created_at, e.is_unfiled, e.chapter_id, c.name, " +
                "e.incident_id, i.title, i.resolved_at " +
                "FROM live_entry e " +
                "LEFT JOIN live_chapter c ON c.id = e.chapter_id " +
                "LEFT JOIN live_incident i ON i.id = e.incident_id " +
                "WHERE e.id = ?",
            arrayOf(entryId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            EntryDetail(
                people = peopleOnEntryBlocking(database, entryId),
                entry = TrailEntry(
                    id = cursor.getString(0),
                    kind = cursor.getString(1),
                    title = cursor.getString(2),
                    body = cursor.getString(3),
                    occurredEdtf = cursor.getString(4),
                    occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                    createdAt = cursor.getLong(6),
                    isUnfiled = cursor.getInt(7) == 1,
                    threads = threads,
                ),
                chapterId = cursor.getString(8),
                chapterName = cursor.getString(9),
                incidentId = cursor.getString(10),
                incidentTitle = cursor.getString(11),
                incidentIsOpen = cursor.isNull(12),
            )
        }
    }

    /**
     * Links an entry to the person it involved.
     *
     * **`entry_person` has been in the schema since Phase 0 and nothing wrote
     * to it.** `MASTER_SPEC.md` section 3 promises that a person knows every
     * call and visit involving them, and that promise had no data behind it:
     * capture kept who was spoken to as the entry's title, which is a string,
     * so a person's page could never list their own calls.
     */
    suspend fun linkEntryToPerson(entryId: String, personId: String) =
        withContext(Dispatchers.IO) {
            insert("entry_person", mapOf("entry_id" to entryId, "person_id" to personId))
        }

    /**
     * Everything written down that involved this person, most recent first.
     *
     * The other half of the link, per rule 18. An entry names the people on it
     * and each of them opens; a person names their entries and each of those
     * opens.
     */
    suspend fun entriesForPerson(personId: String): List<TrailEntry> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT e.id, e.kind, e.title, e.body, e.occurred_edtf, e.occurred_start, " +
                    "e.created_at, e.is_unfiled FROM live_entry e " +
                    "JOIN live_entry_person ep ON ep.entry_id = e.id " +
                    "WHERE ep.person_id = ? " +
                    "ORDER BY coalesce(e.occurred_start, e.created_at) DESC",
                arrayOf(personId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            TrailEntry(
                                id = cursor.getString(0),
                                kind = cursor.getString(1),
                                title = cursor.getString(2),
                                body = cursor.getString(3),
                                occurredEdtf = cursor.getString(4),
                                occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                                createdAt = cursor.getLong(6),
                                isUnfiled = cursor.getInt(7) == 1,
                                threads = emptyList(),
                            ),
                        )
                    }
                }
            }
        }

    /** The people named on one entry, so the entry can offer them back. */
    suspend fun peopleOnEntry(entryId: String): List<Person> = withContext(Dispatchers.IO) {
        peopleOnEntryBlocking(db().database, entryId)
    }

    /** The same read, for a caller that already has the database open. */
    private fun peopleOnEntryBlocking(
        database: net.zetetic.database.sqlcipher.SQLiteDatabase,
        entryId: String,
    ): List<Person> {
        return database.rawQuery(
            "SELECT p.id, p.display_name, p.role_label, p.phone, p.email, p.notes " +
                "FROM live_entry_person ep JOIN live_person p ON p.id = ep.person_id " +
                "WHERE ep.entry_id = ? ORDER BY p.created_at",
            arrayOf(entryId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Person(
                            id = cursor.getString(0),
                            displayName = cursor.getString(1),
                            roleLabel = cursor.getString(2),
                            phone = cursor.getString(3),
                            email = cursor.getString(4),
                            notes = cursor.getString(5),
                        ),
                    )
                }
            }
        }
    }

    // ---- the prep sheet -------------------------------------------------

    /**
     * What to walk into an appointment carrying.
     *
     * `MASTER_SPEC.md` 4.5: the questions waiting for that person plus a change
     * summary composed from real entries, every line tapping through to its
     * source.
     *
     * **This is the app's most useful two minutes and it is entirely
     * composition.** Nothing here is generated, inferred, or summarized: the
     * questions are the ones somebody wrote down, and the changes are the
     * entries themselves. Rule 2 and `MASTER_SPEC.md` 4.11, which says every
     * prep sheet comes from a deterministic composition engine and never from a
     * model.
     */
    data class Prep(
        val appointment: Appointment,
        /** Questions nobody has asked yet, most recently written first. */
        val questions: List<Question>,
        /**
         * Everything written down since the last appointment.
         *
         * **Since the last one, not since some window.** A person walking into
         * a review wants what has happened since the last time they sat in that
         * room, and a fixed thirty days would either repeat what was already
         * covered or drop what was not.
         */
        val changes: List<TrailEntry>,
        /**
         * When the window starts, as the previous appointment's own date
         * string, so the screen says it exactly as that appointment says it.
         *
         * The EDTF rather than a timestamp, because a date rendered from an
         * instant would be a precision claim the record may not make, which is
         * rule 17 and `DESIGN.md` 10.9.
         */
        val sinceEdtf: String?,
    )

    suspend fun prep(subjectId: String, appointmentId: String): Prep? =
        withContext(Dispatchers.IO) {
            val all = appointments(subjectId)
            val appointment = all.firstOrNull { it.id == appointmentId }
                ?: return@withContext null

            // The most recent appointment strictly before this one, which is
            // what "since the last time" means. Null before the first.
            val previous = all
                .filter { it.id != appointmentId && it.scheduledStart != null }
                .filter { other ->
                    appointment.scheduledStart?.let { other.scheduledStart!! < it } ?: true
                }
                .maxByOrNull { it.scheduledStart!! }
            val since = previous?.scheduledStart

            val open = questions(subjectId).filter { it.isOpen }

            val changes = trail(subjectId).filter { entry ->
                val at = entry.occurredStart ?: entry.createdAt
                val notAfter = appointment.scheduledStart?.let { at <= it } ?: true
                val after = since?.let { at > it } ?: true
                notAfter && after
            }

            Prep(
                appointment = appointment,
                questions = open,
                changes = changes,
                sinceEdtf = previous?.scheduledEdtf,
            )
        }

    /**
     * Everything on one care thread, most recent first.
     *
     * **A thread is the app's own metaphor and could not be opened.** Routes
     * identify one on the trail, on an entry, and on the threads screen, and
     * tapping the thread itself did nothing, which is the dead end #46 exists
     * to remove. `MASTER_SPEC.md` 4.6: threads with per-thread colors,
     * filtering, and preserved history when ended.
     */
    suspend fun entriesForThread(threadId: String): List<TrailEntry> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT e.id, e.kind, e.title, e.body, e.occurred_edtf, e.occurred_start, " +
                    "e.created_at, e.is_unfiled FROM live_entry e " +
                    "JOIN live_entry_thread et ON et.entry_id = e.id " +
                    "WHERE et.thread_id = ? " +
                    "ORDER BY coalesce(e.occurred_start, e.created_at) DESC",
                arrayOf(threadId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            TrailEntry(
                                id = cursor.getString(0),
                                kind = cursor.getString(1),
                                title = cursor.getString(2),
                                body = cursor.getString(3),
                                occurredEdtf = cursor.getString(4),
                                occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                                createdAt = cursor.getLong(6),
                                isUnfiled = cursor.getInt(7) == 1,
                                threads = emptyList(),
                            ),
                        )
                    }
                }
            }
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
