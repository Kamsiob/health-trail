package com.kamsiob.healthtrail.data

import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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
        db().database.write(
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
    /**
     * Every write goes through here, so every string reaches SQLite in NFC.
     *
     * `contract/DATA-CONTRACT.md` 8.4: **a name typed with a combining accent
     * on one device and a precomposed character on another is the same person,
     * not two.** `José` from a keyboard that emits `e` plus U+0301 and `José`
     * from one that emits U+00E9 are different byte sequences that look
     * identical in every screenshot and every log, so search misses one of
     * them, an import makes a second row, and nobody can see why. **Nothing in
     * the app normalized anything until #227.**
     *
     * **At the write and not at the read.** Normalizing on the way out would
     * leave the database holding both forms and every future query having to
     * remember; normalizing on the way in means the stored bytes are canonical
     * once and the problem stops existing.
     *
     * **NFC and not NFD**, which is what the contract names: it is the form the
     * web and most keyboards already produce, so it is the one that changes the
     * fewest bytes.
     */
    private fun net.zetetic.database.sqlcipher.SQLiteDatabase.write(sql: String) =
        execSQL(sql)

    /** The same, for the writes that carry arguments. Those are the text ones. */
    private fun net.zetetic.database.sqlcipher.SQLiteDatabase.write(
        sql: String,
        args: Array<out Any?>,
    ) =
        execSQL(
            sql,
            Array(args.size) { index ->
                val value = args[index]
                // Only strings need it, and an already-normal string is
                // returned unchanged rather than copied.
                if (value is String) Text.nfc(value) else value
            },
        )

    private suspend fun insert(table: String, values: Map<String, Any?>): String =
        withContext(Dispatchers.IO) { insertRow(table, values) }

    /**
     * The insert itself, with no dispatcher of its own.
     *
     * Split out so a caller that has already opened a transaction can add rows
     * to it. Switching dispatchers inside a transaction is how a write ends up
     * on a different thread than the transaction it believes it is part of.
     */
    /**
     * The same insert, with nothing suspending in it. #425.
     *
     * **A `suspend` call between `beginTransaction` and `endTransaction` is a
     * place a write can change threads**, and an Android transaction is bound
     * to the thread that opened it. [insertRow] is only `suspend` because it
     * reaches for `db()`, and inside a transaction the database is by
     * definition already open, so handing the handle in removes the reason
     * rather than working around it.
     *
     * This was safe today for a reason that is not a guarantee:
     * `withContext(Dispatchers.IO)` called from an IO thread does not actually
     * dispatch. Nothing would have failed if that changed. The write would just
     * have landed outside the transaction it believed it was in, and the
     * atomicity would have quietly stopped being true.
     */
    private fun insertRow(
        database: net.zetetic.database.sqlcipher.SQLiteDatabase,
        deviceId: String,
        table: String,
        values: Map<String, Any?>,
    ): String {
        val id = Ids.new()
        val now = System.currentTimeMillis()
        val all = LinkedHashMap<String, Any?>()
        all["id"] = id
        all["created_at"] = now
        all["updated_at"] = now
        all["origin_device"] = deviceId
        all["rev"] = 1
        all.putAll(values)

        val columns = all.keys.joinToString(", ")
        val placeholders = all.keys.joinToString(", ") { "?" }
        database.write(
            "INSERT INTO $table ($columns) VALUES ($placeholders)",
            all.values.toTypedArray(),
        )
        return id
    }

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
        db().database.write(
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

    /**
     * Everyone this notebook is keeping, oldest first. #379.
     *
     * **The `subject` table has carried `is_active` since Phase 0** and
     * nothing in the app ever reached it, so a family caring for two parents
     * had one notebook and no way to say which parent a row was about. The
     * data layer anticipated this; only the surface was missing.
     */
    suspend fun subjects(): List<Subject> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, display_name, relationship, situation_template_id " +
                "FROM live_subject ORDER BY created_at",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Subject(
                            id = cursor.getString(0),
                            displayName = cursor.getString(1),
                            relationship = cursor.getString(2),
                            situationTemplateId = cursor.getString(3),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Switches which person the notebook is showing.
     *
     * **Exactly one is active, always.** Every query in the app reads the
     * active subject, so two actives would silently mix two people's records
     * on one screen, which is the worst thing this app could do. The clear
     * and the set are one transaction for that reason.
     */
    suspend fun makeSubjectActive(subjectId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // **Now actually one transaction.** #423. The comment above said the
        // clear and the set were one and they were two bare writes, which is
        // the shape TRAPS section 8 opens with: read the code under the
        // comment, not the comment.
        //
        // **The window this closes is the worst one in the app.** Every subject
        // scoped screen reads the active subject. Dying between the clear and
        // the set leaves zero active rows, so the notebook opens empty and the
        // person believes their record is gone. Dying the other way round
        // leaves two, and `activeSubject()` resolves that silently with
        // `ORDER BY created_at LIMIT 1`, so the visible symptom is somebody
        // else's notebook rather than an error. `schema.sql` has no unique
        // index on `is_active`, so nothing underneath catches either.
        val database = db().database
        database.beginTransaction()
        try {
            database.write(
                "UPDATE subject SET is_active = 0, updated_at = ?, rev = rev + 1 " +
                    "WHERE is_active = 1 AND deleted_at IS NULL",
                arrayOf<Any?>(now),
            )
            database.write(
                "UPDATE subject SET is_active = 1, updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(now, subjectId),
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Starts a second person, and makes them the one being shown.
     *
     * **Their records are separate from the first person's by construction**,
     * because every table already carries `subject_id` and every query already
     * filters on it. Nothing is shared and nothing is copied.
     */
    suspend fun addSubject(
        displayName: String,
        relationship: String? = null,
    ): String {
        val id = createSubject(displayName = displayName, relationship = relationship)
        makeSubjectActive(id)
        return id
    }

    /**
     * Corrects who this notebook is about. #371.
     *
     * **The name was typed once at setup and could never be changed.** There
     * was no updater at all, and the name appears on no screen inside the app:
     * every read of it is inside a share block, so it reaches the emergency
     * card, the incident summary, the prep sheet and the month review. **A typo
     * made at two in the morning on install day was invisible until a clinician
     * was holding it.**
     *
     * **The relationship travels with it**, because the two were asked in one
     * breath at setup and correcting one without the other would send somebody
     * back through the same screen twice.
     */
    suspend fun updateSubject(
        subjectId: String,
        displayName: String,
        relationship: String?,
    ) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE subject SET display_name = ?, relationship = ?, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                displayName,
                relationship?.ifBlank { null },
                System.currentTimeMillis(),
                subjectId,
            ),
        )
    }

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
    /**
     * Renames a chapter. #371.
     *
     * **A chapter is the app's unit of "where", and its name could never be
     * fixed.** It titles its own screen, heads a run in the month review, and
     * is printed on the documents this app hands to other people. Setup asks
     * for it in one line at two in the morning, which is exactly when it gets
     * typed wrong.
     */
    suspend fun renameChapter(chapterId: String, name: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE chapter SET name = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(name.trim(), System.currentTimeMillis(), chapterId),
        )
    }

    /**
     * Corrects when a chapter started, or when it ended. #432, rule 17.
     *
     * **`moveToChapter` stamps today and `renameChapter` was the only other
     * mutator**, so a chapter's dates could never be changed by anything. Rule
     * 17 says dates are flexible, always editable, never falsely precise, and
     * editable forever from the entry itself. That held for entries and not for
     * the axis the whole record is organized by: somebody recording a move
     * three days after it happened had a permanently wrong admission date.
     *
     * **Either date can be null, and null means unknown rather than absent.**
     * A chapter with no end date is the one the person is in now, which is what
     * `currentChapterId` reads, so clearing the end reopens a place and setting
     * it closes one. That is the same meaning the rest of the app already gives
     * those columns, not a new one invented here.
     *
     * **Unknown is a first class value that saves**, rule 17 again. Passing an
     * `Edtf.unknown()` stores the unknown form rather than refusing.
     */
    suspend fun updateChapterDates(
        chapterId: String,
        started: Edtf.Date?,
        ended: Edtf.Date?,
    ) = withContext(Dispatchers.IO) {
        val columns = linkedMapOf<String, Any?>()
        columns.putAll(
            started?.let { dateColumns("started", it) }
                ?: mapOf(
                    "started_edtf" to null,
                    "started_zone" to null,
                    "started_start" to null,
                    "started_end" to null,
                ),
        )
        columns.putAll(
            ended?.let { dateColumns("ended", it) }
                ?: mapOf(
                    "ended_edtf" to null,
                    "ended_zone" to null,
                    "ended_start" to null,
                    "ended_end" to null,
                ),
        )
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
            "UPDATE chapter SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), chapterId)).toTypedArray(),
        )
    }

    /**
     * Corrects a question's own words. #374.
     *
     * **A question is typed in a corridor and read out in an appointment**, so
     * the words matter and the moment they are written is the worst moment to
     * get them right. It could not be corrected at all until now.
     *
     * **The change log is the schema's job**, not this function's:
     * `trg_question_update` writes the row, which is what keeps rule 3's "every
     * write appends to the change log in the same transaction" true whether or
     * not a writer remembers.
     */
    suspend fun updateQuestionText(questionId: String, text: String) =
        withContext(Dispatchers.IO) {
            db().database.write(
                "UPDATE question SET text = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(text.trim(), System.currentTimeMillis(), questionId),
            )
        }

    /**
     * Corrects a standing instruction's own words. #374.
     *
     * **The wording is the instruction**, and it is quoted back to staff and
     * printed on what this app hands to other people. Its name is what the list
     * calls it, so both are correctable and both are written together: two
     * round trips would let one land and the other fail.
     */
    suspend fun updateInstructionWords(instructionId: String, name: String, wording: String) =
        withContext(Dispatchers.IO) {
            db().database.write(
                "UPDATE standing_instruction SET name = ?, wording = ?, " +
                    "updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(
                    name.trim(),
                    wording.trim(),
                    System.currentTimeMillis(),
                    instructionId,
                ),
            )
        }

    /**
     * Corrects a reading: its value, its unit, when it happened and its note.
     * #374.
     *
     * **A reading is typed on a phone one handed while holding something
     * else**, which is how 138.8 becomes 1388, and it could not be corrected at
     * all. Rule 17 says a date is editable forever from the entry itself, and
     * this is the writer that makes that true for a measurement.
     *
     * **Number and text are both passed and both may be null**, because a
     * measure is continuous or observational and a reading carries whichever
     * its measure is for. Passing both as null is not an error here: the caller
     * decides what a reading with nothing in it means, and refusing it in the
     * writer would be this layer making a screen's decision.
     *
     * **Every date column moves together.** `occurred_edtf`, its zone and the
     * resolved range are one fact, and writing the text without the range is
     * how a corrected date sorts to where the old one was.
     */
    suspend fun updateReading(
        readingId: String,
        number: Double?,
        text: String?,
        unit: String?,
        occurred: Edtf.Date,
        note: String?,
    ) = withContext(Dispatchers.IO) {
        val dates = dateColumns("occurred", occurred)
        db().database.write(
            "UPDATE measurement SET value_number = ?, value_text = ?, unit = ?, " +
                "note = ?, occurred_edtf = ?, occurred_zone = ?, occurred_start = ?, " +
                "occurred_end = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf(
                number,
                text?.trim()?.takeIf { it.isNotEmpty() },
                unit?.trim()?.takeIf { it.isNotEmpty() },
                note?.trim()?.takeIf { it.isNotEmpty() },
                dates["occurred_edtf"],
                dates["occurred_zone"],
                dates["occurred_start"],
                dates["occurred_end"],
                System.currentTimeMillis(),
                readingId,
            ),
        )

        // **And the entry that carries it into the trail, search and the
        // readable copy.** #428, and it is the same shape as #429's incident
        // mirror: correcting a reading updated the measurement and left the
        // entry saying the old number, so a shared month printed a value the
        // person had already fixed.
        db().database.rawQuery(
            "SELECT measure_id, entry_id FROM live_measurement WHERE id = ?",
            arrayOf(readingId),
        ).use { cursor ->
            if (cursor.moveToNext() && !cursor.isNull(1)) {
                val heading = measurementHeading(
                    cursor.getString(0),
                    number,
                    text,
                    unit,
                )
                db().database.write(
                    "UPDATE entry SET title = ?, body = ?, updated_at = ?, rev = rev + 1 " +
                        "WHERE id = ?",
                    arrayOf<Any?>(
                        heading,
                        note?.trim()?.takeIf { it.isNotEmpty() },
                        System.currentTimeMillis(),
                        cursor.getString(1),
                    ),
                )
            }
        }
    }

    /**
     * Corrects what a measure is called and the unit it is kept in. #374.
     *
     * **The unit is nullable on purpose**, because a measure can be a text one:
     * "how she seemed" has no unit and never will, and writing an empty string
     * there would make the column say something the record does not.
     *
     * **`preset_id` and `style` are untouched.** A measure taken from a preset
     * stays taken from it; renaming is not adopting.
     */
    suspend fun updateMeasure(measureId: String, name: String, unit: String?) =
        withContext(Dispatchers.IO) {
            db().database.write(
                "UPDATE measure SET name = ?, unit = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(
                    name.trim(),
                    unit?.trim()?.takeIf { it.isNotEmpty() },
                    System.currentTimeMillis(),
                    measureId,
                ),
            )
        }

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
        /**
         * Anything else about them, which the column has held since Phase 0 and
         * nothing ever wrote. #361: the owner asked for it by name, and where
         * somebody parks or which extension actually reaches them is the
         * knowledge that makes a care team worth keeping.
         */
        notes: String? = null,
        /** Where they work, and it is never required. Rule 13, #353. */
        organizationId: String? = null,
        /** Their email. The column shipped in Phase 0 unwritten. #379. */
        email: String? = null,
    ): String {
        val personId = insert(
            "person",
            mapOf(
                "subject_id" to subjectId,
                "display_name" to displayName,
                "phone" to phone?.ifBlank { null },
                "role_label" to roleLabel,
                "notes" to notes?.ifBlank { null },
                "email" to email?.ifBlank { null },
                "organization_id" to organizationId,
            ),
        )
        // **Which chapter of her life this person belongs to**, stamped at write
        // time from the chapter with no end date, exactly as an entry, a
        // document and an incident already are.
        //
        // **#143 and #371 together.** `person_chapter` is read by the chapters
        // screen's count of who was involved, and until now the only thing that
        // had ever written a row was the fixture generator, so that count could
        // be non-zero in a screenshot and never in a real notebook. The
        // generator gained a writer for it earlier today, which made the
        // asymmetry worse rather than better: a fixture that fills a column the
        // app cannot write is precisely how a screen comes to look joined up
        // and be empty.
        //
        // **No role is recorded here.** `role_label` on this table would be
        // what somebody did during that chapter specifically, which nothing
        // asks and nobody has said, and a copy of the person's own role would
        // be the app inventing a fact about a period of time.
        currentChapterId(subjectId)?.let { chapterId ->
            insert(
                "person_chapter",
                mapOf("person_id" to personId, "chapter_id" to chapterId),
            )
        }
        return personId
    }

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
        notes: String? = null,
        organizationId: String? = null,
        /** Their email. Editable like everything else. #379. */
        email: String? = null,
    ) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE person SET display_name = ?, phone = ?, role_label = ?, notes = ?, " +
                "email = ?, organization_id = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                displayName,
                phone?.ifBlank { null },
                roleLabel?.ifBlank { null },
                notes?.ifBlank { null },
                email?.ifBlank { null },
                organizationId,
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
        /** How often, in the words it was given in. #379. */
        frequencyText: String? = null,
        purposeText: String?,
        notes: String?,
        onEmergencyCard: Boolean,
    ) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE medication SET name = ?, dose_text = ?, frequency_text = ?, " +
                "purpose_text = ?, notes = ?, " +
                "on_emergency_card = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                name,
                doseText?.ifBlank { null },
                frequencyText?.ifBlank { null },
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
    /**
     * The Today for somebody who did not pick a setting.
     *
     * **Skipping is a real answer**, per rule 13 and the situation picker's own
     * copy, so it cannot produce the blank dashboard 21.5 rules out. This is the
     * smallest hand that is useful in every setting: what the record says today,
     * the next dated thing, what is on the list, what is saved to ask, and the
     * card that exists to be handed to a paramedic.
     *
     * **It is a starting hand like any other**, editable from the first minute,
     * and nothing here is inferred from watching anybody.
     */
    val defaultStartingHand: List<Pair<String, String>> = listOf(
        "digest" to "wide",
        "next_up" to "small",
        "medications" to "small",
        "ask_next_time" to "small",
        "emergency_card" to "small",
    )

    /** Gives a subject the default hand, for a person who picked no setting. */
    suspend fun applyDefaultStartingHand(subjectId: String) {
        if (todayLayout(subjectId) == null) {
            setTodayLayout(subjectId, defaultStartingHand)
        }
    }

    suspend fun applySituation(
        subjectId: String,
        templateId: String,
        threads: List<Pair<String, String>>,
        /**
         * The Today this situation ships, as pairs of card type and size.
         *
         * **Applied only when the person has no layout yet.** A situation
         * changing later must not throw away the desk somebody spent months
         * arranging, and 21.8's promise that the app never rearranges Today is
         * not suspended because the care setting changed.
         *
         * Empty for a caller with nothing to apply, which then leaves whatever
         * is there alone.
         */
        startingHand: List<Pair<String, String>> = emptyList(),
        /**
         * The first days list this setting ships, and the papers it produces.
         *
         * **Both sat in the template data with nothing reading them**, #135, so
         * a person who picked "Nursing home" got its threads and none of the
         * ten things worth doing in the first week. That is the highest value
         * content in the whole catalog and it was invisible.
         *
         * **They become a project rather than a thirteenth section.** A project
         * already is a named process holding steps that can be edited, marked
         * done and removed, and papers that are named slots waiting for a
         * document, and `DESIGN.md` 20.2 says the checklist "remains, as steps,
         * and leads only in the one shape where the work truly is many small
         * arrangements". The first days is exactly that shape. D136.
         */
        checklist: List<String> = emptyList(),
        documents: List<String> = emptyList(),
        /**
         * What that project is called, in the person's own language.
         *
         * **The caller composes it**, the same way it composes a thread's
         * label, because a repository that reached into the catalog would be
         * writing display text out of the data layer.
         *
         * Null from a caller that does not want the project at all, which is
         * how a test asks for the old behavior.
         */
        firstDaysName: String? = null,
    ) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE subject SET situation_template_id = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any>(templateId, System.currentTimeMillis(), subjectId),
        )

        // **One per setting, and a second setting gets its own.** Moving from a
        // hospital stay to a nursing home is genuinely a new set of first days
        // with different people to call, so the new list is real work rather
        // than a duplicate. Applying the same setting twice is not, and this is
        // what stops it: the guard is the template the project came from.
        val firstDays = if (
            firstDaysName != null &&
            (checklist.isNotEmpty() || documents.isNotEmpty()) &&
            // **The guard that stops a setting applied twice making two
            // lists**, and it reads the same query the template library
            // already reads rather than a second copy of it, per D133. A
            // tombstoned project does not count, because somebody who removed
            // their first days list and set the setting up again is asking
            // for it back.
            projectsFromTemplate(subjectId, templateId).isEmpty()
        ) {
            startProject(
                subjectId = subjectId,
                templateId = templateId,
                name = firstDaysName,
                steps = checklist,
                // **Steps lead**, 20.3, because this shape is many small
                // arrangements rather than one slow process being waited on.
                lead = "steps",
                // **No stages and no date kinds.** The first days is not a
                // process with named stages somebody agreed to, and a road
                // drawn over it would say there is a sequence when there is
                // not. 20.6 and the road strip's own rule.
                papers = documents,
            )
        } else {
            null
        }

        if (startingHand.isNotEmpty() && todayLayout(subjectId) == null) {
            // **The list goes on Today, because invisible was the defect.**
            // Left in the Projects tab alone it would be one tab away with
            // nothing pointing at it, which is the same absence in a nicer
            // place.
            //
            // **First in the field, and never the lead.** 21.1 allows exactly
            // one lead and the setting's own hand already has one. Appended
            // instead, it sat under seven cards each saying "nothing yet",
            // which is what a brand new notebook is: on day one this is the
            // only card with anything in it and the only thing anybody can
            // act on, and rule 15 puts that where the eye lands first.
            // Seen on a first run rather than reasoned about.
            //
            // **This is the one moment the app decides an order**, which is
            // what a starting hand is. 21.8 holds from the first minute after:
            // the person can move it or take it off and nothing puts it back.
            val hand = if (firstDays == null) {
                startingHand
            } else {
                listOf(startingHand.first()) +
                    ("project_steps" to "wide") +
                    startingHand.drop(1)
            }
            val sources = if (firstDays == null) {
                emptyMap()
            } else {
                mapOf(1 to ("project" to firstDays))
            }
            setTodayLayout(subjectId, hand, sources)
        }
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
            db().database.write(
                "UPDATE entry SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
                (columns.values + listOf(System.currentTimeMillis(), entryId)).toTypedArray(),
            )
        }

    /**
     * Corrects what an entry says. #368.
     *
     * **The most created thing in the app was the only one that could not be
     * corrected.** Person, medication, bill, appointment, document, milestone,
     * incident and project step all had an update; an entry had
     * `updateEntryOccurred` and nothing else, so a note typed one handed in a
     * corridor kept its typo forever and the only remedy was removing the
     * record of the call and retyping it from memory, losing its threads, its
     * chapter, its incident link and the moment it was written.
     *
     * **Four independent walkthroughs found this**, and the owner's own words
     * were that things should be editable after the fact. `Repository`'s own
     * comment on an incident's follow-ups already claimed it: "Those are
     * entries, and they are corrected where entries are corrected."
     *
     * **Only the words.** The date has its own path, per rule 17, and the
     * links this entry has are changed where they were made. Blank is a real
     * answer for both columns: a title somebody wants gone should go.
     */
    suspend fun updateEntry(entryId: String, title: String?, body: String?) =
        withContext(Dispatchers.IO) {
            db().database.write(
                "UPDATE entry SET title = ?, body = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(
                    title?.ifBlank { null },
                    body?.ifBlank { null },
                    System.currentTimeMillis(),
                    entryId,
                ),
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
     * Starts tracking something the catalog never heard of.
     *
     * **Sixteen presets is a good starting set and it is not the world**, and
     * until now they were the only way in, which made the sixteen read as the
     * only sixteen things that count. A family weighing a wound, counting good
     * days, or writing down how far she walked to the door had no way to say
     * so. #203, and it is the same argument `createProject` already makes about
     * sixteen catalog processes.
     *
     * **`preset_id` stays null**, which is what the schema already means by
     * something the person set up themselves, and it is what the measurement
     * screen reads to know there is no preset behind it.
     *
     * **`advice_risk` is low and `style` is the default**, because those two
     * exist to let the rendering layer hold the content rules, and a thing the
     * person named is not one the catalog has made any claim about. Nothing
     * here is ever shown, ranged, or turned into a warning: rule 2.
     */
    suspend fun createOwnMeasure(
        subjectId: String,
        name: String,
        unit: String?,
        /** Words rather than a number, which is a different column, not a flag. */
        isText: Boolean,
        sortIndex: Int = 0,
    ): String = insert(
        "measure",
        mapOf(
            "subject_id" to subjectId,
            "name" to name.trim(),
            "preset_id" to null,
            "unit" to unit?.trim()?.ifBlank { null },
            "style" to if (isText) "observational" else "continuous",
            "gap_tolerance" to "moderate",
            "advice_risk" to "low",
            "show_medication_markers" to 0,
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
    /**
     * Writes a reading, **and its trail entry, in the same transaction.** #371.
     *
     * **A reading was not on the trail.** `entryId` has been a parameter since
     * this was written and the only caller passed null, so a measurement was
     * not in `trail()`, not in a month review, not in the prep sheet's changes,
     * and not in the digest. `MASTER_SPEC.md` 4.10b says anything created is
     * written to the trail, and this was one of the writers that was not.
     *
     * **The entry carries what the person wrote, and nothing the app inferred.**
     * The measure's name is the title and their note is the body. The value
     * itself stays on the measurement row, where the chart reads it: rendering
     * a number into the trail's own words would be the app deciding the reading
     * was the interesting part, and rule 2 keeps this to recording.
     *
     * **`entryId` is still honored when a caller has one**, which is how a
     * reading captured as part of something else stays attached to it rather
     * than growing a second entry.
     */
    /**
     * What a reading's entry is called: the measure, the value, the unit. #428.
     *
     * **A whole number loses its decimal point.** SQLite hands back a REAL, so
     * 72 arrives as 72.0 and "Weight 72.0 kg" is a number nobody wrote. A value
     * with a fraction keeps it exactly as it came.
     *
     * **`value_text` is used when there is no number**, because a measure can
     * use either column and a categorical reading is the whole value rather
     * than a label on one.
     */
    private suspend fun measurementHeading(
        measureId: String,
        number: Double?,
        text: String?,
        unit: String?,
    ): String? {
        val name = measureName(measureId)
        val value = when {
            number != null -> {
                val whole = number.toLong()
                if (number == whole.toDouble()) whole.toString() else number.toString()
            }
            else -> text?.ifBlank { null }
        } ?: return name
        return listOfNotNull(name, value, unit?.ifBlank { null }).joinToString(" ")
    }

    suspend fun recordMeasurement(
        measureId: String,
        number: Double? = null,
        text: String? = null,
        unit: String? = null,
        occurred: Edtf.Date = Edtf.day(LocalDate.now()),
        note: String? = null,
        entryId: String? = null,
        source: String = "family",
        /**
         * The notebook this belongs to, so the entry can be written.
         *
         * **Null keeps the old behavior**, which is a measurement with no trail
         * entry, and is what the fixture generator wants: it writes its own
         * entries and would otherwise get two.
         */
        subjectId: String? = null,
        chapterId: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val ownEntry = entryId ?: subjectId?.let { subject ->
                insertRow(
                    "entry",
                    mapOf(
                        "subject_id" to subject,
                        "kind" to "measurement",
                        // **The number goes in the entry, and it never did.**
                        // #428. The title was the measure's name alone and the
                        // value lived only in `measurement.value_number`, while
                        // `Readable.monthReview` renders an entry's title and
                        // body. So a shared month printed "Weight" with no
                        // number, for every reading: the one artifact people
                        // hand to a clinician carried none of the data it
                        // appeared to carry. The trail and search read the same
                        // columns, so all three were blank in the same way.
                        //
                        // **Rule 2 holds exactly here.** This states the number
                        // that was recorded, next to what it is a reading of.
                        // It says nothing about a change, a direction, a range,
                        // or whether the number is good.
                        "title" to measurementHeading(measureId, number, text, unit),
                        "body" to note?.ifBlank { null },
                        "chapter_id" to chapterId,
                    ) + dateColumns("occurred", occurred),
                )
            }
            val id = insertRow(
                "measurement",
                mapOf(
                    "measure_id" to measureId,
                    "entry_id" to ownEntry,
                    "value_number" to number,
                    "value_text" to text?.ifBlank { null },
                    "unit" to unit,
                    "note" to note?.ifBlank { null },
                    "source" to source,
                ) + dateColumns("occurred", occurred),
            )
            database.setTransactionSuccessful()
            id
        } finally {
            database.endTransaction()
        }
    }

    /** What this measure is called, for the trail entry that names it. */
    private suspend fun measureName(measureId: String): String? =
        db().database.rawQuery(
            "SELECT name FROM live_measure WHERE id = ?",
            arrayOf(measureId),
        ).use { cursor -> if (cursor.moveToNext()) cursor.getString(0) else null }

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
    /**
     * The three record kinds that live outside the twelve sections and can
     * still be taken out. **Anything that can be added can be removed**, the
     * owner's rule, 2026-08-16: a milestone marked by mistake, a reading typed
     * twice, a measure started and regretted. The same tombstone as every
     * other removal, so another device learns it was deleted.
     */
    suspend fun deleteMilestone(id: String) = deleteRow("milestone", id)

    /**
     * A reading, which lives in `measurement`. #471.
     *
     * **It named a table that does not exist**, so removing a reading typed
     * twice threw "no such table: reading" from `execSQL` rather than removing
     * it. `Digest.kt` records the same word being got wrong once before: the
     * screens and the model call these readings and the schema calls them
     * measurements, and nothing in the language stops a writer from reaching
     * for the wrong one. A table name in a string is a name nothing checks.
     */
    suspend fun deleteReading(id: String) = deleteRow("measurement", id)

    /**
     * Its readings stay in the table and vanish from every screen with it,
     * because a reading of a removed measure has nowhere to be shown. Undoing
     * the mistake is a restore, like every other removal.
     */
    suspend fun deleteMeasure(id: String) = deleteRow("measure", id)

    private suspend fun deleteRow(table: String, rowId: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE $table SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ? AND deleted_at IS NULL",
            arrayOf<Any?>(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                rowId,
            ),
        )
    }

    suspend fun delete(section: Section, rowId: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE ${section.table} SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ? AND deleted_at IS NULL",
            arrayOf<Any?>(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                rowId,
            ),
        )
    }

    /** One thing deleted, as the person would recognize it. #405, #465. */
    data class Discarded(
        /**
         * The section whose mark and hue it wears.
         *
         * **Display only.** It used to be the identity too, and that was the
         * defect: `Section` and table are not one to one, so a deleted memo
         * arrived twice, once as the trail and once as memos, both pointing at
         * the same row. Restoring either restored the same thing and the second
         * row stayed on screen naming something that was already back. #465.
         */
        val section: Section,
        /** The table the row is actually in, which is what a write needs. */
        val table: String,
        val id: String,
        /** What it was, in their words rather than a table name or a row id. */
        val label: String,
        /** When it was deleted. */
        val deletedAt: Long,
    )

    /**
     * What a permanent delete took, so the screen can say it plainly.
     *
     * Rows includes everything that had to go with it: a row nothing points at
     * is rare in this schema.
     */
    data class Purged(
        val rows: Int,
        val logEntries: Int,
        val files: Int,
    )

    /**
     * The tables Deleted Items lists, the column that names a row in each, and
     * the mark it wears.
     *
     * **Driven by table rather than by `Section`**, which is what fixes two
     * defects at once. `Section` is a screen's idea of the notebook and there
     * are fourteen of them over eleven tables: `TRAIL` and `NOTES` are both
     * `entry`, so iterating sections listed every deleted memo twice. And five
     * things a person can delete are in tables no `Section` names as its own:
     * a milestone, a reading, a chapter, a care thread and a tracked measure
     * were all deleted into a screen that could not show them, which means they
     * could not be put back. #465.
     *
     * **The order is the order they are listed in**, before the sort by date.
     */
    private data class BinTable(
        val table: String,
        /** SQL that names one row in the person's own words. */
        val label: String,
        /** SQL that limits the table to one notebook, with one `?` for its id. */
        val mine: String,
        val section: Section,
    )

    private val BIN_TABLES = listOf(
        BinTable("entry", "title", "subject_id = ?", Section.TRAIL),
        BinTable("person", "display_name", "subject_id = ?", Section.CARE_TEAM),
        BinTable("medication", "name", "subject_id = ?", Section.MEDICATIONS),
        BinTable("appointment", "title", "subject_id = ?", Section.APPOINTMENTS),
        BinTable("question", "text", "subject_id = ?", Section.ASK_NEXT_TIME),
        BinTable("document", "title", "subject_id = ?", Section.DOCUMENTS),
        BinTable("bill", "description", "subject_id = ?", Section.MONEY),
        BinTable("project", "name", "subject_id = ?", Section.PROJECTS),
        BinTable("chapter", "name", "subject_id = ?", Section.CHAPTERS),
        BinTable("care_thread", "label", "subject_id = ?", Section.THREADS),
        BinTable(
            "standing_instruction",
            "name",
            "subject_id = ?",
            Section.STANDING_INSTRUCTIONS,
        ),
        BinTable("measure", "name", "subject_id = ?", Section.PROGRESS),
        BinTable("milestone", "label", "subject_id = ?", Section.CHAPTERS),
        // **A reading is named by the entry it wrote**, which is where its
        // number lives, #428. Its own columns are a value and an optional note,
        // and "168" alone says nothing about what was weighed.
        BinTable(
            "measurement",
            // allow-base-table: a reading's own entry may itself have been
            // deleted, and a row in Deleted Items with no name at all is worse
            // than one named by a title the live view is hiding.
            "(SELECT e.title FROM entry e WHERE e.id = measurement.entry_id)",
            // allow-base-table: the measure a reading belongs to may be
            // deleted too, and its readings still have to be findable here.
            "measure_id IN (SELECT id FROM measure WHERE subject_id = ?)",
            Section.PROGRESS,
        ),
    )

    /**
     * Everything deleted from the notebook and not yet put back. #405, #465.
     *
     * **This is a reader over rows the app already keeps.** Rule 3 and 3.1:
     * deletion is always a tombstone, and every screen reads a `live_*` view
     * that filters `deleted_at IS NULL`. So nothing had to be stored for this
     * to be possible; the record was already there and nothing could read it.
     *
     * **Named in the person's own words**, because rule 20 says a table name
     * and a row id never reach the screen.
     *
     * **Most recently deleted first**, which is the order somebody looking for
     * what they just did needs.
     */
    suspend fun discarded(subjectId: String): List<Discarded> = withContext(Dispatchers.IO) {
        val database = db().database
        val out = mutableListOf<Discarded>()
        for (bin in BIN_TABLES) {
            // A memo is a memo and not the trail, which is the one place a
            // table maps to two sections, so the kind comes back with the row.
            val kind = if (bin.table == "entry") "kind" else "NULL"
            database.rawQuery(
                // allow-base-table: the whole point is the rows the live views
                // hide, so this cannot read a live view.
                "SELECT id, ${bin.label}, deleted_at, $kind FROM \"${bin.table}\" " +
                    "WHERE ${bin.mine} AND deleted_at IS NOT NULL " +
                    "ORDER BY deleted_at DESC, id",
                arrayOf(subjectId),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out += Discarded(
                        section = if (cursor.getString(3) == "note") Section.NOTES else bin.section,
                        table = bin.table,
                        id = cursor.getString(0),
                        label = cursor.getString(1).orEmpty(),
                        deletedAt = cursor.getLong(2),
                    )
                }
            }
        }
        out.sortedByDescending { it.deletedAt }
    }

    /**
     * Puts one back exactly where it was. #405.
     *
     * **Clearing one column is the whole of it**, and that is the point of
     * tombstones: the row kept its own `occurred_*` dates, so a thing deleted
     * in March goes back in March rather than at the top of the trail. Nothing
     * had to be remembered separately.
     *
     * **`updated_at` and `rev` move, because this is a write.** Another phone
     * has to learn that the row came back, and 8.3's merge decides by
     * `updated_at`: a restore that left the stamp alone would be undone by the
     * next merge with a device that still thinks it is deleted.
     */
    suspend fun restore(table: String, rowId: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE \"$table\" SET deleted_at = NULL, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ? AND deleted_at IS NOT NULL",
            arrayOf<Any?>(System.currentTimeMillis(), rowId),
        )
    }

    /**
     * Takes one thing out of the record for good, and everything that only
     * existed because of it. #465, and it is an owner decision.
     *
     * **This is the one write in the app that is not a tombstone**, and the
     * data contract had to be amended for it: `contract/DATA-CONTRACT.md` 3
     * says deletion is always a tombstone and names its exceptions, and this is
     * now the third one. It is reachable from exactly one place, Deleted Items,
     * on a row the person has already deleted once, behind a confirmation that
     * says it cannot be undone.
     *
     * **What "removed from the database entirely" has to mean, exactly**, or
     * the promise is not kept:
     *
     * 1. **The row.**
     * 2. **Every row that only existed because of it.** Foreign keys here are
     *    `NO ACTION` and enforced on the connection, so deleting a parent with
     *    a surviving child throws. A tombstoned child is still a row, so this
     *    is not a rare case: an entry has twelve child tables and a chapter has
     *    thirteen. Children are found from the schema itself rather than from a
     *    list here, which cannot go stale.
     * 3. **A reference that is allowed to be absent is cleared, not followed.**
     *    A nullable foreign key means "this may point at nothing", so a live
     *    project date that happened to cite a deleted entry loses the citation
     *    and keeps the date. Following it would delete things the person never
     *    asked about.
     * 4. **The change log rows.** They carry no content, and they are still a
     *    durable record that a row with that id existed, was written n times
     *    and was deleted at a particular moment, and `sqlcipher_export` copies
     *    them into every archive. "Absent from any export" is not true while
     *    they are there.
     * 5. **The attachment bytes**, when nothing else names the same file. The
     *    store is content addressed and two rows can name one file, so the
     *    hash is swept only if no `attachment` row is left holding it,
     *    tombstones included.
     *
     * **No trigger fires and nothing is logged.** There is no `AFTER DELETE`
     * trigger in the schema, which is what makes this possible at all, and it
     * is also the right answer: a log entry saying a row was purged is the
     * residue this exists to remove.
     *
     * **All of it in one transaction.** A half applied purge is a row with its
     * children gone or a change log pointing at nothing.
     */
    suspend fun purge(table: String, rowId: String): Purged = withContext(Dispatchers.IO) {
        val database = db().database
        val children = childrenOf(database)

        // Discovered parents first, so deleting in reverse takes children
        // first, which is what `NO ACTION` requires.
        val order = mutableListOf<Pair<String, String>>()
        val seen = mutableSetOf<Pair<String, String>>()
        val clear = mutableListOf<Triple<String, String, String>>()

        fun walk(atTable: String, atId: String) {
            if (!seen.add(atTable to atId)) return
            order += atTable to atId
            for (child in children[atTable].orEmpty()) {
                database.rawQuery(
                    // allow-base-table: a purge is about rows the live views
                    // hide, and a tombstoned child still blocks the delete.
                    // unordered-query: a set of ids to delete, and delete order
                    // among siblings cannot matter. Nothing renders from here.
                    "SELECT id FROM \"${child.table}\" WHERE \"${child.column}\" = ?",
                    arrayOf(atId),
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val childId = cursor.getString(0)
                        if (child.required) {
                            walk(child.table, childId)
                        } else {
                            clear += Triple(child.table, child.column, childId)
                        }
                    }
                }
            }
        }
        walk(table, rowId)

        val hashes = mutableSetOf<String>()
        for ((atTable, atId) in order) {
            if (atTable != "attachment") continue
            database.rawQuery(
                // allow-base-table: reading a row that is about to stop
                // existing, so there is no view that can answer this.
                "SELECT sha256 FROM attachment WHERE id = ?",
                arrayOf(atId),
            ).use { if (it.moveToFirst()) hashes += it.getString(0) }
        }

        var logEntries = 0
        database.beginTransaction()
        try {
            for ((childTable, childColumn, childId) in clear) {
                database.write(
                    "UPDATE \"$childTable\" SET \"$childColumn\" = NULL, " +
                        "updated_at = ?, rev = rev + 1 WHERE id = ?",
                    arrayOf<Any?>(System.currentTimeMillis(), childId),
                )
            }
            for ((atTable, atId) in order.asReversed()) {
                logEntries += database.rawQuery(
                    "SELECT COUNT(*) FROM change_log WHERE table_name = ? AND row_id = ?",
                    arrayOf(atTable, atId),
                ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
                database.write(
                    "DELETE FROM change_log WHERE table_name = ? AND row_id = ?",
                    arrayOf<Any?>(atTable, atId),
                )
                database.write("DELETE FROM \"$atTable\" WHERE id = ?", arrayOf<Any?>(atId))
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        // **Files last, and only once the rows are gone.** A file swept before
        // the transaction committed would be a photograph deleted for a purge
        // that then rolled back.
        var files = 0
        if (hashes.isNotEmpty()) {
            val store = Attachments.open(app)
            for (hash in hashes) {
                val stillNamed = database.rawQuery(
                    // allow-base-table: a tombstoned attachment still names the
                    // file, and its bytes have to survive for a restore.
                    // unordered-query: whether any row is left, which is one
                    // fact rather than a list.
                    "SELECT 1 FROM attachment WHERE sha256 = ? LIMIT 1",
                    arrayOf(hash),
                ).use { it.moveToFirst() }
                if (!stillNamed && store.remove(hash)) files += 1
            }
        }

        Purged(rows = order.size, logEntries = logEntries, files = files)
    }

    /** One table that points at another, and whether it may point at nothing. */
    private data class ChildRef(val table: String, val column: String, val required: Boolean)

    /**
     * Which tables point at each table, read out of the schema itself.
     *
     * **Never a list written here.** A hand written map of thirteen children
     * per parent is a map that goes stale the first time a column is added, and
     * the failure it produces is a purge that throws on a foreign key or, worse,
     * leaves a child row pointing at nothing.
     */
    private fun childrenOf(
        database: net.zetetic.database.sqlcipher.SQLiteDatabase,
    ): Map<String, List<ChildRef>> {
        val out = mutableMapOf<String, MutableList<ChildRef>>()
        for (table in Backup.userTables(database)) {
            val required = mutableSetOf<String>()
            // unordered-query: the shape of a table, not a list anybody reads.
            database.rawQuery("PRAGMA table_info(\"$table\")", null).use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getInt(3) == 1) required += cursor.getString(1)
                }
            }
            // unordered-query: the same, for its references.
            database.rawQuery("PRAGMA foreign_key_list(\"$table\")", null).use { cursor ->
                val parentAt = cursor.getColumnIndexOrThrow("table")
                val fromAt = cursor.getColumnIndexOrThrow("from")
                while (cursor.moveToNext()) {
                    val column = cursor.getString(fromAt)
                    out.getOrPut(cursor.getString(parentAt)) { mutableListOf() } +=
                        ChildRef(table, column, column in required)
                }
            }
        }
        return out
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
     * One column of one row, read past the live view.
     *
     * **For tests only.** It exists for assertions the app itself must never be
     * able to make: that a removed card is a tombstone rather than a deleted
     * row, and that a column holds what was written rather than what a data
     * class chose to expose.
     */
    suspend fun columnForTest(table: String, rowId: String, column: String): String? =
        withContext(Dispatchers.IO) {
            // allow-base-table: several callers assert on a tombstoned row,
            // which a live view hides by definition.
            db().database.rawQuery(
                "SELECT $column FROM $table WHERE id = ?",
                arrayOf(rowId),
            ).use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }
        }

    /**
     * Leaves a Today layout with no lead at all.
     *
     * **For tests only, and it produces a state the app cannot.** Every write
     * path keeps exactly one lead and the database refuses a second, so the only
     * way to prove that the reader reports a broken layout rather than quietly
     * promoting the first card is to break one on purpose.
     *
     * That distinction matters: a reader that repairs silently is a reader that
     * hides the defect, and the repair itself would be the app deciding what
     * belongs at the top of somebody's screen.
     */
    suspend fun clearEveryLeadForTest(subjectId: String) = withContext(Dispatchers.IO) {
        // **`updated_at` and `rev` move, and they did not.** #424. This ships in
        // the main source set and wrote neither, so the rows it touched carried
        // a stale revision and an old timestamp while their contents had
        // changed. A peer merging on last-write-wins would then keep whichever
        // copy happened to have the newer stamp, which is not the newer edit,
        // and the Today lead is exactly the kind of single-row-wins column that
        // resolves wrongly and silently.
        //
        // **Left in `main` rather than moved to `androidTest`, deliberately.**
        // The KDoc above says why it exists: it produces a state the app itself
        // cannot, so the reader can be proved to report a broken layout rather
        // than quietly promoting the first card. Moving it would mean the test
        // builds that state through a different door than the app's own writer,
        // which is the thing `applySchema` being `internal` already argues
        // against. What was wrong was the write, not the address.
        db().database.write(
            "UPDATE today_card SET is_lead = 0, updated_at = ?, rev = rev + 1 " +
                "WHERE subject_id = ?",
            arrayOf<Any?>(System.currentTimeMillis(), subjectId),
        )
    }

    /**
     * Tombstones a row without going through the screen that owns it.
     *
     * **For tests only.** It is how the source-closed rung of the states ladder
     * is produced: a project goes away underneath a card that points at it, and
     * the card has to be kept rather than dropped. 8.7.
     */
    suspend fun tombstoneForTest(table: String, rowId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE $table SET deleted_at = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(now, now, rowId),
        )
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
                database.write(
                    "INSERT INTO entry_thread " +
                        "(id, created_at, updated_at, origin_device, rev, entry_id, thread_id) " +
                        "VALUES (?, ?, ?, ?, 1, ?, ?)",
                    arrayOf<Any?>(Ids.new(), now, now, db().deviceId, entryId, threadId),
                )
            }
            database.write(
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
                "WHERE et.entry_id = ? " +
                // **The same order threads appear in everywhere else**, so one
                // entry's threads read the same here as on the threads screen.
                // Unordered, this returned whatever SQLite chose, which is
                // stable until a vacuum or an index changes it, at which point
                // two exports of one unchanged database stop matching and 8.5's
                // regeneration test begins failing intermittently. 8.4.
                "ORDER BY t.sort_index, t.created_at",
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
    data class CareThread(
        val id: String,
        val label: String,
        val colorIndex: Int,
        /**
         * When the person said this thread ended, or null while it runs. #433.
         *
         * **Carried on the thread itself** so every screen that has a thread
         * knows whether it is still running, rather than each one asking a
         * second time or, as before, no screen being able to ask at all.
         */
        val endedEdtf: String? = null,
    )

    /**
     * The threads this notebook carries, in the order the person sees them.
     *
     * Returns an empty list for a notebook with no situation template applied,
     * which is a real state rather than an error: "Not sure yet" is a valid
     * answer to the situation picker and it produces a working notebook.
     */
    suspend fun threads(subjectId: String): List<CareThread> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT id, label, color_index, ended_edtf FROM live_care_thread " +
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
                            endedEdtf = if (cursor.isNull(3)) null else cursor.getString(3),
                        ),
                    )
                }
            }
        }
    }

    /**
     * A care thread with no situation template behind it. #349, D145.
     *
     * **Applying a situation was the only way a thread had ever been created**,
     * so a person dealing with a landlord, a school, or an employer's leave
     * department had a real recurring thread that none of the fourteen
     * situations ever heard of, and the answer was that it lived in the trail
     * with no spine of its own. **The same argument [createProject] and
     * [createOwnMeasure] both already make**: a starting set is not the world.
     *
     * `template_id` stays null, which is what the schema's own comment already
     * means by a thread that did not come from a template.
     *
     * **The label is the only thing asked for.** Everything else takes the
     * default the schema carries, and `sortIndex` is the count of what is
     * already there so a new thread joins the end of the list and takes the
     * next route color rather than colliding with the first.
     */
    /**
     * Renames a care thread. #371.
     *
     * **A thread could be started and never corrected.** Its label is the title
     * of its own screen, a chip on the capture form, a filing target in the
     * unfiled tray and a line in the trail, so a thread named wrong was named
     * wrong in five places forever and the only escape was removing it and
     * losing everything filed under it.
     */
    suspend fun renameThread(threadId: String, label: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE care_thread SET label = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(label.trim(), System.currentTimeMillis(), threadId),
        )
    }

    /**
     * Ends a care thread, on the day the person says it ended. #433.
     *
     * **`ended_edtf` and `end_note` have existed since the schema was written,
     * are read in two places, and `CareThreadsScreen` already splits running
     * threads from ended ones. Nothing ever wrote either column.** So every
     * thread a person opened stayed open forever: the running list only grew,
     * the ended group was permanently empty, and a situation that finished and
     * later started again could not be expressed at all.
     *
     * **The note is optional and stays optional**, rule 13. "The appeal is
     * over" is a complete answer and so is silence.
     *
     * **Nothing is deleted and nothing is hidden.** An ended thread keeps every
     * entry filed against it and keeps showing them; what changes is which of
     * the two groups it sits in.
     */
    suspend fun endThread(
        threadId: String,
        ended: Edtf.Date,
        note: String? = null,
    ) = withContext(Dispatchers.IO) {
        val columns = dateColumns("ended", ended) +
            (note?.ifBlank { null }?.let { mapOf("end_note" to it) } ?: emptyMap())
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
            "UPDATE care_thread SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), threadId)).toTypedArray(),
        )
    }

    /**
     * Starts an ended thread running again. #433.
     *
     * **The same shape incidents and projects already use**, and the reason is
     * the same: a thing that was finished and turns out not to be is ordinary,
     * and making somebody create a second thread for it would split one
     * situation across two places forever.
     *
     * **The end note goes with it.** It described an ending that is no longer
     * true, and leaving it would put a closing sentence on a running thread.
     */
    suspend fun reopenThread(threadId: String) = withContext(Dispatchers.IO) {
        val cleared = dateColumns("ended", Edtf.unknown()).keys
            .joinToString(", ") { "$it = NULL" }
        db().database.write(
            "UPDATE care_thread SET $cleared, end_note = NULL, " +
                "updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(System.currentTimeMillis(), threadId),
        )
    }

    suspend fun createThread(subjectId: String, label: String, sortIndex: Int = 0): String =
        insert(
            "care_thread",
            mapOf(
                "subject_id" to subjectId,
                "label" to label.trim(),
                "template_id" to null,
                "color_index" to sortIndex,
                "sort_index" to sortIndex,
                // A thread somebody starts today started today, and a day is
                // exactly as much as anyone knows about it. The same line
                // `applySituation` writes.
            ) + dateColumns("started", Edtf.day(LocalDate.now())),
        )

    /**
     * The care threads, most recently filed into first.
     *
     * **What this answers is "which three would you like offered", not "what
     * are the threads".** The Unfiled tray leads with the matcher's suggestion
     * and offers two alternates beside it, and the capture form caps its chips
     * at five: in both places the ones the person actually uses beat the ones
     * the situation template happened to list first. On a notebook where the
     * matcher finds nothing, which is common because it matches whole words
     * and never stems, the offered set is otherwise the same three on all
     * eighty six cards.
     *
     * **The care threads screen keeps `sort_index`**, which is the template's
     * own order and what somebody scanning a roster expects. Two orders,
     * because they answer two different questions, exactly as [people] and
     * [peopleByRecentUse] do.
     *
     * A thread nothing has ever been filed under sorts last and keeps its own
     * order among its peers, so a fresh notebook behaves exactly as [threads]
     * does.
     */
    suspend fun threadsByRecentUse(subjectId: String): List<CareThread> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT t.id, t.label, t.color_index, " +
                    "(SELECT MAX(coalesce(e.occurred_start, e.created_at)) " +
                    "   FROM live_entry_thread et " +
                    "   JOIN live_entry e ON e.id = et.entry_id " +
                    "  WHERE et.thread_id = t.id) AS last_used " +
                    "FROM live_care_thread t WHERE t.subject_id = ? " +
                    "ORDER BY last_used IS NULL, last_used DESC, " +
                    "t.sort_index, t.created_at",
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
        /**
         * When the person pinned this, or null if they have not.
         *
         * **Pinned entries float above time itself**, which is law 4's fourth
         * tool. In a five year notebook the thing somebody needs at the desk is
         * almost never the most recent thing, and scrolling to it every time is
         * the tax that makes a person stop opening the app.
         *
         * The timestamp rather than a flag, because the pinned run is ordered
         * most recently pinned first and because "you pinned this in June" is
         * what the row says.
         *
         * **It has no default, deliberately.** It had one for an hour, and in
         * that hour the entry screen's own query kept selecting the columns it
         * always had: the pin wrote to the database and the button that wrote it
         * still offered to pin, because nothing had gone looking for the value.
         * A default is what let a second reader of this table compile while
         * quietly dropping a column. Every construction site now has to say what
         * it knows, and the two that did not know were the two that were wrong.
         */
        val pinnedAt: Long?,
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
            "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, is_unfiled, " +
                "pinned_at FROM live_entry WHERE subject_id = ? " +
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
                            pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Pin an entry to the top of the trail, or take the pin out.
     *
     * **Pinning is not filing and it is not a favorite.** It is the person
     * saying "I keep needing this one", usually about the letter they have to
     * quote or the incident nobody has answered. It changes where the entry
     * appears and nothing else about it: no state, no category, no meaning the
     * app added.
     *
     * `pinned_at` has existed in the schema since the contract was written and
     * nothing wrote to it until the trail earned law 4's fourth tool.
     */
    /**
     * Puts somebody at the top of the care team, or takes them back out. #361.
     *
     * **The same shape as `setEntryPinned`**, deliberately: one nullable
     * timestamp, set to now or cleared, and no second table. The owner ruled
     * for this over a sort order because pinning a few is what people actually
     * do, and because a manual order over forty people is work nobody finishes.
     *
     * **It travels in the archive and survives a restore**, which is the whole
     * argument for it being a column rather than a device preference.
     */
    /**
     * Says somebody is no longer involved, without erasing them. #371.
     *
     * **`archived_at` was read in five places and written by nothing but the
     * fixture.** So the only thing the app offered was Remove, which tombstones:
     * `live_person` stops returning them, every entry where they were the person
     * spoken to loses its person, and the care team of the place she left goes
     * blank. Somebody who moved facilities had eleven people to retire and the
     * only available action destroyed six months of their own record.
     *
     * **Archiving and removing are different sentences.** Archiving is the
     * person saying somebody is no longer involved; removing is them saying the
     * row should not exist. The queries already tell them apart, which is what
     * made this a missing writer rather than a missing feature.
     */
    suspend fun setPersonArchived(personId: String, archived: Boolean) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE person SET archived_at = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(if (archived) now else null, now, personId),
            )
        }

    suspend fun setPersonPinned(personId: String, pinned: Boolean) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE person SET pinned_at = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(if (pinned) now else null, now, personId),
            )
        }

    suspend fun setEntryPinned(entryId: String, pinned: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE entry SET pinned_at = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(if (pinned) now else null, now, entryId),
        )
    }

    /**
     * One resolution a merge made, in the terms a person can read.
     *
     * **Not the JSON.** The schema keeps both versions whole so that nothing is
     * lost and either can be recovered by hand, and that is a storage promise
     * rather than a screen. Rule 20: any time the interface asks somebody to
     * understand how the app stores something, that is the code failing to
     * absorb its own complexity. So this carries the fields that actually
     * differed, already paired up.
     *
     * @param kept which side was kept, "local" or "incoming". Not shown as
     *   those words: the screen says "the one written on this phone" or "the
     *   one in the file", because local and incoming are the database's words.
     * @param reason one of `Merge.Reason`, turned into a sentence on screen.
     * @param differences the columns whose values were not the same, with the
     *   kept value and the other one. **Only the ones that differ**, because a
     *   row has forty columns and thirty nine of them agreeing is not news.
     */
    data class Resolution(
        val seq: Long,
        val table: String,
        val rowId: String,
        val resolvedAt: Long,
        val kept: String,
        val reason: String,
        val differences: List<Difference>,
        val seen: Boolean,
        /**
         * What the record is called, in the person's own words.
         *
         * **Which record, not only which column.** The screen said "Their role,
         * kept: Senior intake caseworker" and never said whose role, so a
         * notebook with fifteen people on its care team gave the person no way
         * to tell which of them the merge had decided about. #389.
         *
         * Null where the row has no name of its own, which is honest: some
         * rows genuinely have nothing a person would call them.
         */
        val what: String? = null,
        /**
         * Whose notebook the record belongs to.
         *
         * **A notebook can hold more than one person**, and a resolution about
         * one of Margaret's medications and one of Harold's read identically
         * without this. It is null for a row that belongs to the notebook
         * rather than to anybody in it.
         */
        val whose: String? = null,
    )

    /**
     * One column where the two versions disagreed.
     *
     * @param render what the column is, from `contract/readable-fields.json`,
     *   so the screen shows a value rather than what the database holds. It
     *   printed `Kept: 1786315875877` for a pinned entry before this existed,
     *   which is #328's defect appearing in a screen on the day it was closed
     *   in the archive.
     * @param vocabulary for an `enum`, which set of words its value is in.
     */
    data class Difference(
        val column: String,
        val render: String,
        val vocabulary: String?,
        val keptValue: String?,
        val otherValue: String?,
    )

    /**
     * Every resolution a merge has made, newest first.
     *
     * **Newest first, unlike most lists here.** The trail reads oldest first
     * because it is a history somebody follows; this is a notice, and the one
     * that matters is the one that just happened.
     */
    suspend fun conflicts(): List<Resolution> = withContext(Dispatchers.IO) {
        // Read once, rather than per resolution: a merge between two long
        // notebooks can resolve hundreds of rows and every one of them would
        // otherwise open the subject table again.
        val subjectNames = buildMap {
            // **The live view, so a removed person is not named.** A resolution
            // about somebody who has since been taken out of the notebook still
            // shows which record it was about; it just does not put their name
            // back on a screen after the person removed it.
            //
            // Ordered even though this becomes a lookup map, because 8.4 is
            // about the query rather than about what the caller does next, and
            // an exception here would cost a reader the same reasoning twice.
            db().database.rawQuery(
                "SELECT id, display_name FROM live_subject ORDER BY id",
                null,
            ).use { c ->
                while (c.moveToNext()) put(c.getString(0), c.getString(1))
            }
        }
        db().database.rawQuery(
            "SELECT seq, table_name, row_id, resolved_at, winner, reason, local_json, " +
                "incoming_json, seen_at FROM conflict_log ORDER BY seq DESC",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val winner = cursor.getString(4)
                    val local = RowJson.read(cursor.getString(6))
                    val incoming = RowJson.read(cursor.getString(7))
                    val kept = if (winner == "local") local else incoming
                    val other = if (winner == "local") incoming else local
                    add(
                        Resolution(
                            seq = cursor.getLong(0),
                            table = cursor.getString(1),
                            rowId = cursor.getString(2),
                            resolvedAt = cursor.getLong(3),
                            kept = winner,
                            reason = cursor.getString(5),
                            differences = (kept.keys + other.keys).sorted()
                                .filter { kept[it] != other[it] }
                                // **Only columns a person has a word for**, and
                                // the field map is what decides that: a column
                                // it renders has a label in all four catalogs,
                                // held by check_readable_labels.py, and one it
                                // does not is bookkeeping or a derived index.
                                //
                                // This is not thrift, it is the dynamic key
                                // trap in docs/TRAPS.md section 3. Asking the
                                // catalog for a key built from a column name
                                // throws in debug, so a conflict on a derived
                                // column would have crashed the notice screen
                                // the first time somebody opened it.
                                .filter { nameable(cursor.getString(1), it) }
                                .map { column ->
                                    val field = field(cursor.getString(1), column)
                                    Difference(
                                        column = column,
                                        render = field?.render ?: "timestamp",
                                        vocabulary = field?.vocabulary,
                                        keptValue = kept[column],
                                        otherValue = other[column],
                                    )
                                },
                            seen = !cursor.isNull(8),
                            // **The kept version names it**, because that is
                            // the one the notebook now holds. The other one is
                            // shown field by field below.
                            what = HEADING_COLUMNS.firstNotNullOfOrNull { kept[it] }
                                ?.takeIf { it.isNotBlank() },
                            whose = kept["subject_id"]?.let { subjectNames[it] },
                        ),
                    )
                }
            }
        }
    }

    /**
     * Where a row's own name lives, in the order the archive's own pages ask.
     *
     * The same list `ReadableArchive` uses for a record's heading, because a
     * conflict notice and the readable copy must call the same record the same
     * thing.
     */
    /**
     * What each table calls the thing a person would recognize it by. #397.
     *
     * **Named per table rather than derived**, because the columns genuinely
     * differ and a wrong guess would put a row id on somebody's screen, which
     * is rule 20 exactly.
     */
    private val ABOUT_HEADINGS = mapOf(
        "person" to "display_name",
        "appointment" to "title",
        "incident" to "title",
        "document" to "title",
        "bill" to "description",
        "project" to "name",
        "question" to "text",
        "medication" to "name",
        "entry" to "title",
    )

    private val HEADING_COLUMNS = listOf(
        "display_name", "title", "name", "question", "label", "text",
    )

    /** How many resolutions the person has not looked at yet. */
    suspend fun unseenConflicts(): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT count(*) FROM conflict_log WHERE seen_at IS NULL", null,
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Mark every resolution as seen.
     *
     * **Not a tombstone and not a deletion.** The row stays, because the
     * person may want to read it again, and because the schema is explicit
     * that nothing here is ever discarded. Seen means seen.
     */
    suspend fun markConflictsSeen(at: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            db().database.write(
                "UPDATE conflict_log SET seen_at = ? WHERE seen_at IS NULL",
                arrayOf<Any?>(at),
            )
        }

    /** Columns that change on every write and say nothing about the record. */
    private val BOOKKEEPING = setOf("rev", "updated_at", "created_at", "origin_device")

    /**
     * Whether this screen can say what a column is.
     *
     * **A deletion is always sayable**, and it is the one difference that
     * matters most: a row removed on one phone and edited on the other is
     * exactly the case somebody needs to see. It has no archive label because
     * the archive does not print tombstones, so it carries its own word.
     */
    private fun nameable(table: String, column: String): Boolean {
        if (column in BOOKKEEPING) return false
        if (column == "deleted_at") return true
        val field = field(table, column) ?: return false
        // **An identifier is not something a person can act on.** A conflict
        // that differs only in what a row points at would show a UUID, which
        // rule 20 calls the interface asking somebody to understand how the app
        // stores things. Both versions stay whole in the database either way.
        return field.render !in setOf("dateZone", "moneyCurrency", "id", "link", "tableName")
    }

    /** What the contract says a column is, or null when it renders nothing. */
    private fun field(table: String, column: String): ReadableArchive.Field? =
        ReadableFieldMap.tables[table]?.rendered?.firstOrNull { it.column == column }

    /** One person on the care team, with everything a row about them shows. */
    data class Person(
        val id: String,
        val displayName: String,
        val roleLabel: String?,
        val phone: String?,
        val email: String?,
        val notes: String?,
        /**
         * When the person put them at the top of the care team, or null.
         *
         * **The same column and the same meaning as an entry's pin**, because
         * it is the same decision: "I keep needing this one." Until this
         * existed the top of the care team was decided by `peopleByRecentUse`,
         * which is a good default and was the only answer, so somebody who
         * wanted the night charge nurse up there could not say so. Owner
         * ruling, #361.
         */
        val pinnedAt: Long? = null,
        /**
         * When the person said they are no longer involved, or null. #371.
         *
         * **Not the same as removed.** An archived person stays on everything
         * already written and stops being offered when something new is being
         * written. Removing tombstones the row and takes them off six months of
         * entries with it.
         */
        val archivedAt: Long? = null,
        /**
         * Where they work, when the person said so. #353.
         *
         * **The column and its index shipped in Phase 0 and nothing wrote
         * either.** Grid screen 11 folds the care team by where people work,
         * "At Maplewood, 4 more" and "Outside, billing, ombudsman, 2", and
         * that half could not be built: grouping by a column nothing writes
         * produces one fold holding everybody, labeled with nothing.
         */
        val organizationId: String? = null,
        /** Its name, carried so a row can say where somebody works. */
        val organizationName: String? = null,
    )

    /**
     * Finds a place by the name somebody typed, or writes it down.
     *
     * **Matched on the name the person gave, ignoring case and surrounding
     * space**, because "Maplewood" typed twice is one place and asking somebody
     * to notice they have two would be the app making its own storage their
     * problem, which is rule 20.
     */
    suspend fun organizationNamed(name: String): String? {
        val wanted = name.trim()
        if (wanted.isEmpty()) return null
        val existing = withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id FROM live_organization WHERE lower(name) = lower(?) " +
                    "ORDER BY created_at LIMIT 1",
                arrayOf(wanted),
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }
        return existing ?: insert("organization", mapOf("name" to wanted))
    }

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
            "SELECT p.id, p.display_name, p.role_label, p.phone, p.email, p.notes, " +
                "p.pinned_at, p.archived_at, p.organization_id, o.name " +
                "FROM live_person p " +
                "LEFT JOIN live_organization o ON o.id = p.organization_id " +
                "WHERE p.subject_id = ? AND p.archived_at IS NULL " +
                // **Pinned first, most recently pinned at the top**, which is
                // the ordering the trail's pinned run already uses, and the
                // order somebody added people in for the rest.
                "ORDER BY p.pinned_at IS NULL, p.pinned_at DESC, p.created_at",
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
                            pinnedAt = if (cursor.isNull(6)) null else cursor.getLong(6),
                            archivedAt = if (cursor.isNull(7)) null else cursor.getLong(7),
                            organizationId = cursor.getString(8),
                            organizationName = cursor.getString(9),
                        ),
                    )
                }
            }
        }
    }

    /**
     * The care team, most recently involved first.
     *
     * **This is what the capture form's five chips are picked from**, per
     * `DESIGN.md` 5.11.1. A year five notebook has ten people on it and showing
     * all ten as chips is the wall the owner named; showing the first five by
     * creation date would have been a cap with no reasoning behind it, and it
     * would have put whoever was added on day one in front of the nurse from
     * this morning.
     *
     * **Recency is measured by the entries they are actually linked to**, using
     * `entry_person`, which capture has been writing since the chips existed.
     * Somebody never linked to an entry sorts after everybody who has been,
     * and among those the creation order is kept, so a fresh notebook where
     * nothing has been linked yet behaves exactly as [people] does.
     *
     * The date used is the entry's own `occurred_start` where it has one and
     * its `created_at` otherwise, so backdating a call from three months ago
     * does not push that person to the front of a list meant to answer "who
     * have you been dealing with lately".
     */
    suspend fun peopleByRecentUse(subjectId: String): List<Person> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT p.id, p.display_name, p.role_label, p.phone, p.email, p.notes, " +
                    "(SELECT MAX(coalesce(e.occurred_start, e.created_at)) " +
                    "   FROM live_entry_person ep " +
                    "   JOIN live_entry e ON e.id = ep.entry_id " +
                    "  WHERE ep.person_id = p.id) AS last_used " +
                    "FROM live_person p " +
                    "WHERE p.subject_id = ? AND p.archived_at IS NULL " +
                    "ORDER BY last_used IS NULL, last_used DESC, p.created_at",
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
            db().database.write(
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
            db().database.write(
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
        /**
         * How often, in the words they were told it in. #379.
         *
         * **Last in the list and defaulted**, so every positional caller that
         * predates it still compiles and means the same thing.
         */
        val frequencyText: String? = null,
    ) {
        /**
         * A medication that has been stopped is kept, not removed. Its whole
         * history is the point, and "she was on this until March" is the answer
         * to a question somebody will eventually be asked.
         */
        val isStopped: Boolean get() = !stoppedEdtf.isNullOrBlank()

        /**
         * Whether it is actually on the card a paramedic would be handed.
         *
         * **The flag alone is not the answer, and this exists because three
         * places were each deciding that for themselves.** The card assembles
         * itself from medications that say they belong on it and drops the
         * stopped ones, which is right: a card listing something she stopped
         * taking in March is worse than a card that omits it. But the
         * medications list and the medication screen rendered the stored flag,
         * so a stopped medication sat there in alert orange claiming to be on a
         * card it had already fallen off.
         *
         * One rule in one place. A safety claim copied into three screens is a
         * safety claim that will disagree with itself the first time one of
         * them is edited, and this one had already started to.
         */
        val showsOnEmergencyCard: Boolean get() = onEmergencyCard && !isStopped
    }

    /** Everything recorded for one subject, still being taken first. */
    suspend fun medications(subjectId: String): List<Medication> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, name, dose_text, purpose_text, notes, on_emergency_card, " +
                    "stopped_edtf, frequency_text FROM live_medication WHERE subject_id = ? " +
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
                                frequencyText = cursor.getString(7),
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
        /** How often, in the words it was given in. #379. */
        frequencyText: String? = null,
    ): String = insert(
        "medication",
        mapOf(
            "subject_id" to subjectId,
            "name" to name,
            "dose_text" to doseText?.ifBlank { null },
            "frequency_text" to frequencyText?.ifBlank { null },
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
            db().database.write(
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
        /**
         * The first step nobody has marked done, or null when none is left.
         *
         * **This is what the list shows instead of "2 of 5 steps done".** A
         * completion count on the person's own work is what rule 13 rules out,
         * and it was also the least useful thing the row could say: somebody
         * scanning five long processes wants to know what to do next, not how
         * far behind they are.
         */
        val nextStep: String?,
        /**
         * Which of the three answers this project opens with. `DESIGN.md` 20.3.
         *
         * **It is the shape**, and the shape is only the order of the same
         * components: the long road leads with where it stands, the closing
         * window with the next date, the busy stretch with the steps. One
         * grammar, three arrangements, so somebody who has learned one project
         * can read the next one.
         */
        val lead: String = "standing",
        /**
         * When this began and when it ended, as the person gave them.
         *
         * **Both are EDTF and neither is display text**, per rule 17: a process
         * somebody remembers starting "sometime in March" started in March, and
         * the screen renders exactly that precision. **`finishedEdtf` is null
         * while a project is open**, which is what it means for it to be open.
         *
         * They are here rather than queried separately because screen 17 asks
         * the closed project how long it took, and a span is two dates.
         */
        val startedEdtf: String? = null,
        val finishedEdtf: String? = null,
        /** The same two, resolved, for the arithmetic a span needs. */
        val startedStart: Long? = null,
        val finishedStart: Long? = null,
    ) {
        val isFinished: Boolean get() = status in FINISHED_STATUSES
    }

    /** One step of a project, in the order the template gave. */
    data class ProjectStep(
        val id: String,
        val text: String,
        val completedEdtf: String?,
        val note: String?,
        /**
         * The area this step belongs to on the busy stretch, or null.
         *
         * `DESIGN.md` 20.3: steps clustered by area with an arranged count. Null
         * is the normal state on the other two shapes.
         */
        val cluster: String? = null,
        /**
         * Who said they would handle it. **A label and never an identity**, D108.
         */
        val handlerLabel: String? = null,
    ) {
        /**
         * Done, which the busy stretch calls arranged.
         *
         * One truth with two names on two screens, rather than two columns that
         * disagree by year two.
         */
        val isDone: Boolean get() = !completedEdtf.isNullOrBlank()
    }

    /** Every project, unfinished first, with how many steps each has. */
    suspend fun projects(subjectId: String): List<Project> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT p.id, p.name, p.template_id, p.status, p.waiting_on, p.notes, " +
                "COUNT(s.id), COUNT(s.completed_edtf), " +
                "(SELECT n.text FROM live_project_step n " +
                "  WHERE n.project_id = p.id AND n.completed_edtf IS NULL " +
                "  ORDER BY n.sort_index, n.created_at LIMIT 1), p.lead, " +
                "p.started_edtf, p.finished_edtf, p.started_start, p.finished_start " +
                "FROM live_project p " +
                "LEFT JOIN live_project_step s ON s.project_id = p.id " +
                "WHERE p.subject_id = ? " +
                "GROUP BY p.id, p.name, p.template_id, p.status, p.waiting_on, " +
                "p.notes, p.created_at, p.lead, p.started_edtf, p.finished_edtf, " +
                "p.started_start, p.finished_start " +
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
                            nextStep = cursor.getString(8),
                            lead = cursor.getString(9),
                            startedEdtf = cursor.getString(10),
                            finishedEdtf = cursor.getString(11),
                            startedStart = if (cursor.isNull(12)) null else cursor.getLong(12),
                            finishedStart = if (cursor.isNull(13)) null else cursor.getLong(13),
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
                "SELECT id, text, completed_edtf, note, cluster, handler_label " +
                    "FROM live_project_step " +
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
                                cluster = cursor.getString(4),
                                handlerLabel = cursor.getString(5),
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
        lead: String = "standing",
        stages: List<String> = emptyList(),
        dateKinds: List<String> = emptyList(),
        papers: List<String> = emptyList(),
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
                    "lead" to lead,
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
            // **The other four defaults, copied in the same transaction.**
            // DESIGN.md 20.4: a project template is a bundle of five defaults,
            // nothing more and nothing less, and every one is visible, editable
            // and removable after setup.
            //
            // **Copied, with no live link back.** Editing this project never
            // touches the template, and updating the template never touches a
            // project already underway. That is the whole reason these are rows
            // here rather than a template id somebody reads through later.
            //
            // **No stage is entered.** A project that has just been created has
            // not reached anything yet, and `current_stage_id` stays null, which
            // is a real state and reads as "not yet" rather than as an error.
            stages.forEachIndexed { index, stage ->
                insertRow(
                    "project_stage",
                    mapOf(
                        "project_id" to projectId,
                        "name" to stage,
                        "sort_index" to index,
                    ),
                )
            }
            dateKinds.forEachIndexed { index, label ->
                insertRow(
                    "project_date_kind",
                    mapOf(
                        "project_id" to projectId,
                        "label" to label,
                        "sort_index" to index,
                    ),
                )
            }
            papers.forEachIndexed { index, paper ->
                insertRow(
                    "project_paper",
                    mapOf(
                        "project_id" to projectId,
                        "name" to paper,
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
     * A project with no template behind it.
     *
     * **`MASTER_SPEC.md` 4.10 has always required this and nothing offered it.**
     * Sixteen catalog processes is a good starting set and it is not the world:
     * a family fighting something the catalog never heard of had no way in at
     * all, which made the sixteen read as the only sixteen things that count.
     *
     * `template_id` stays null, which is what the schema's own comment on
     * `custom_template.derived_from_id` already means by built from scratch,
     * and it is what the detail screen reads to say "you started this one from
     * nothing" rather than naming a template that does not exist.
     */
    suspend fun createProject(subjectId: String, name: String): String =
        insert(
            "project",
            mapOf(
                "subject_id" to subjectId,
                "name" to name,
                "template_id" to null,
                "status" to "active",
            ) + dateColumns("started", Edtf.day(LocalDate.now())),
        )

    /** Changes what a project is called. Every name in this app is a correction away. */
    suspend fun renameProject(projectId: String, name: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE project SET name = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(name, System.currentTimeMillis(), projectId),
        )
    }

    /**
     * Adds a step to the end of a project.
     *
     * **The end, not the beginning**, because the person adding one has just
     * learned about something further along. Moving it earlier is one tap from
     * the step itself, and guessing wrong in the other direction costs two.
     */
    suspend fun addProjectStep(projectId: String, text: String): String =
        withContext(Dispatchers.IO) {
            val next = db().database.rawQuery(
                "SELECT coalesce(MAX(sort_index), -1) + 1 FROM live_project_step " +
                    "WHERE project_id = ?",
                arrayOf(projectId),
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

            insert(
                "project_step",
                mapOf(
                    "project_id" to projectId,
                    "text" to text,
                    "sort_index" to next,
                ),
            )
        }

    /**
     * Changes what a step says and what is written under it.
     *
     * **The note is a first class part of a step rather than an afterthought.**
     * `project_step.note` has been in the schema since Phase 0 with no writer,
     * and it is where "the woman on the phone said to call back after the 15th"
     * goes. That sentence is the whole reason these processes are survivable.
     */
    suspend fun updateProjectStep(stepId: String, text: String, note: String?) =
        withContext(Dispatchers.IO) {
            db().database.write(
                "UPDATE project_step SET text = ?, note = ?, updated_at = ?, " +
                    "rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(
                    text,
                    note?.ifBlank { null },
                    System.currentTimeMillis(),
                    stepId,
                ),
            )
        }

    /**
     * Moves a step one place earlier or later.
     *
     * **A swap with its neighbor rather than a renumbering**, because a
     * renumbering rewrites every row of the project for one move, and every one
     * of those writes appends to the change log in the same transaction. One
     * move should cost two entries in the record, not nine.
     *
     * **Both writes and nothing between them.** A swap that half applied would
     * put two steps on the same index, which sorts by `created_at` and reads as
     * a move that did nothing.
     *
     * The step at either end does not move, and the caller is not told off for
     * asking: a control that is there and does nothing is what D42 removed
     * elsewhere, so the screen hides it instead.
     */
    suspend fun moveProjectStep(stepId: String, earlier: Boolean) =
        withContext(Dispatchers.IO) {
            val database = db().database
            database.beginTransaction()
            try {
                val here = database.rawQuery(
                    "SELECT project_id, sort_index FROM live_project_step WHERE id = ?",
                    arrayOf(stepId),
                ).use {
                    if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
                } ?: return@withContext

                val (projectId, index) = here
                val comparison = if (earlier) "<" else ">"
                val direction = if (earlier) "DESC" else "ASC"
                val neighbor = database.rawQuery(
                    "SELECT id, sort_index FROM live_project_step " +
                        "WHERE project_id = ? AND sort_index $comparison ? " +
                        "ORDER BY sort_index $direction LIMIT 1",
                    arrayOf(projectId, index.toString()),
                ).use {
                    if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
                } ?: return@withContext

                val now = System.currentTimeMillis()
                database.write(
                    "UPDATE project_step SET sort_index = ?, updated_at = ?, " +
                        "rev = rev + 1 WHERE id = ?",
                    arrayOf<Any?>(neighbor.second, now, stepId),
                )
                database.write(
                    "UPDATE project_step SET sort_index = ?, updated_at = ?, " +
                        "rev = rev + 1 WHERE id = ?",
                    arrayOf<Any?>(index, now, neighbor.first),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }

    /**
     * Removes a step.
     *
     * A tombstone like every other deletion, per rule 3, so a peer is told the
     * step went rather than resurrecting it on the next sync.
     */
    suspend fun deleteProjectStep(stepId: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE project_step SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any?>(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                stepId,
            ),
        )
    }

    /**
     * One of the person's own templates.
     *
     * **The body is the same shape as the bundled JSON**, which is what the
     * schema's own comment on `custom_template.body_json` already asks for, so
     * one loader reads both and the start screen does not have to know which
     * kind of template it is offering.
     */
    data class OwnTemplate(
        val id: String,
        val name: String,
        /**
         * The shipped template this was derived from, or null for one built
         * from nothing.
         *
         * **This is the lineage, and it is the whole reason the column
         * exists.** Editing a shipped template makes the person's own copy
         * rather than changing the shipped one, so a catalog update in a later
         * version cannot overwrite what they wrote, and the copy still knows
         * what it grew out of.
         */
        val derivedFromId: String?,
        val steps: List<String>,
        /**
         * The other four defaults, carried so a project started from somebody's
         * own template has a shape.
         *
         * **A template is five defaults, DESIGN.md 20.4**, and an own template
         * that carried only the steps produced a project with no road to draw,
         * no chips when a date was recorded, and no papers. A person who shaped
         * a project over months and saved it would have got the checklist back
         * and nothing else.
         *
         * **A body written by an older build has none of these**, so each one
         * falls back to empty and `lead` to where it stands. That is not a
         * defect: it is what those templates actually held.
         */
        val lead: String = "standing",
        val stages: List<String> = emptyList(),
        val dateKinds: List<String> = emptyList(),
        val papers: List<String> = emptyList(),
        val createdAt: Long,
    )

    /**
     * Saves a project's current steps as the person's own template.
     *
     * **The steps are copied at the moment of saving**, exactly as
     * [startProject] copies them in the other direction. A template that
     * referred back to a live project would change under somebody every time
     * they ticked a step off, which is the opposite of what a template is.
     *
     * **Lineage travels.** A project started from a shipped template keeps that
     * template's id in `derived_from_id`, so the library can say what this grew
     * out of and a later catalog update can never overwrite it.
     */
    suspend fun saveProjectAsTemplate(projectId: String, name: String): String =
        withContext(Dispatchers.IO) {
            val derivedFrom = db().database.rawQuery(
                "SELECT template_id FROM live_project WHERE id = ?",
                arrayOf(projectId),
            ).use { if (it.moveToFirst()) it.getString(0) else null }

            val steps = projectSteps(projectId).map { it.text }
            // The whole shape, not only the checklist. What somebody spent
            // months arranging is the road, the lead and the papers as much as
            // the steps, and a template that dropped them handed back the least
            // interesting part of the work.
            //
            // **The stages are copied without their dates.** A stage carries
            // when this project reached it, which belongs to this project and
            // says nothing about the next one.
            val lead = db().database.rawQuery(
                "SELECT lead FROM live_project WHERE id = ?",
                arrayOf(projectId),
            ).use { if (it.moveToFirst()) it.getString(0) else "standing" }
            val body = JSONObject()
                .put("name", name)
                .put("steps", JSONArray(steps))
                .put("lead", lead)
                .put("stages", JSONArray(projectStages(projectId).map { it.name }))
                .put("date_kinds", JSONArray(projectDateKinds(projectId)))
                .put("papers", JSONArray(projectPapers(projectId).map { it.name }))

            insert(
                "custom_template",
                mapOf(
                    "kind" to "project",
                    "derived_from_id" to derivedFrom,
                    "name" to name,
                    "body_json" to body.toString(),
                ),
            )
        }

    /**
     * The person's own templates of one kind, newest first.
     *
     * **A body that will not parse is skipped rather than crashing the screen.**
     * This column holds JSON written by whatever version of the app the person
     * was running, and an export from a later one can carry a shape this build
     * has never seen. The library says how many it holds by counting what it
     * could read, which is honest, rather than by counting rows it cannot show.
     */
    suspend fun ownTemplates(kind: String): List<OwnTemplate> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, name, derived_from_id, body_json, created_at " +
                    "FROM live_custom_template WHERE kind = ? ORDER BY created_at DESC",
                arrayOf(kind),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val body = runCatching {
                            JSONObject(cursor.getString(3))
                        }.getOrNull() ?: continue

                        fun list(key: String): List<String> {
                            val array = body.optJSONArray(key) ?: return emptyList()
                            return (0 until array.length()).map { array.getString(it) }
                        }

                        add(
                            OwnTemplate(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                derivedFromId = cursor.getString(2),
                                steps = list("steps"),
                                lead = body.optString("lead", "standing"),
                                stages = list("stages"),
                                dateKinds = list("date_kinds"),
                                papers = list("papers"),
                                createdAt = cursor.getLong(4),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Every project a template has produced, newest first.
     *
     * **This is what makes the library show state rather than a menu.** A list
     * of sixteen processes says nothing about a person's notebook; "you started
     * this in March and it is still waiting on the caseworker" is the thing
     * they came to the library to find out.
     *
     * Matches on the shipped template's id and on any of the person's own
     * templates derived from it, so a process the person adjusted once still
     * counts as the same process.
     */
    suspend fun projectsFromTemplate(subjectId: String, templateId: String): List<Project> =
        projects(subjectId).filter { it.templateId == templateId }

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
            db().database.write(
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
        // **Closing a project writes down when it closed, and reopening takes
        // that back.** The columns were in the schema from Phase 0 and nothing
        // ever set them, so a finished project could not say when it finished
        // and screen 17's "closed November 8, started March 3 the year before"
        // had no dates to render. Same shape as a control that kept its
        // repository call through a supersession: read, carried, never written.
        //
        // **Through `dateColumns`, so all four move together** and the EDTF
        // agrees with the instants derived from it, which is what the archive
        // and every date check depend on.
        val finished = if (status in FINISHED_STATUSES) {
            dateColumns("finished", Edtf.day(LocalDate.now()))
        } else {
            // **Cleared rather than left**, because a project that is open
            // again did not finish. Leaving the old date would make the record
            // say something that is no longer true.
            listOf("edtf", "zone", "start", "end").associate { "finished_$it" to null }
        }
        val sets = finished.keys.joinToString("") { ", $it = ?" }
        db().database.write(
            "UPDATE project SET status = ?, waiting_on = ?, waiting_since = ?" +
                sets + ", updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(
                status,
                waitingOn?.ifBlank { null },
                if (status == "waiting") System.currentTimeMillis() else null,
                *finished.values.toTypedArray(),
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
        /** The place this came out of, so a document can be opened from there. */
        val chapterId: String? = null,
        val chapterName: String? = null,
    )

    /** Every document, most recently received first. */
    suspend fun documents(subjectId: String): List<Document> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT d.id, d.title, d.category, d.original_location, d.notes, " +
                "d.received_edtf, a.sha256, a.byte_size, d.chapter_id, c.name " +
                "FROM live_document d " +
                "LEFT JOIN live_attachment a ON a.document_id = d.id " +
                "LEFT JOIN live_chapter c ON c.id = d.chapter_id " +
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
                            chapterId = cursor.getString(8),
                            chapterName = cursor.getString(9),
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
        /** Where she was when this arrived, stamped by the caller. #371. */
        chapterId: String? = null,
        /**
         * The person's own word for the pile this belongs in, or null.
         *
         * **Null is the ordinary answer and it is not a gap.** The documents
         * screen folds by this and calls the unfoldered pile "Everything else",
         * which is a real place rather than a fallback. Nothing in the app
         * wrote it until 2026-08-10, so every document a person saved landed
         * there and folders were visible only because the fixture invented
         * them. #221.
         */
        category: String? = null,
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
                    "category" to category?.trim()?.ifBlank { null },
                    // **Where she was when this paper arrived.** #371: the
                    // chapter axis had readers everywhere and no writer, so a
                    // chapter's documents group could never be non-empty.
                    "chapter_id" to chapterId,
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
        /** When it is due, which is the thing that makes a bill urgent or not. */
        val dueEdtf: String? = null,
        /** The place this bill came out of, so it can be opened from here. */
        val chapterId: String? = null,
        val chapterName: String? = null,
        /** Who sent it, carried so a bill can say so without a second query. */
        val organizationName: String? = null,
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
            "SELECT b.id, b.description, b.amount_minor, b.currency, b.state, " +
                "b.state_note, b.received_edtf, b.notes, b.due_edtf, b.chapter_id, " +
                "c.name, o.name FROM live_bill b " +
                "LEFT JOIN live_chapter c ON c.id = b.chapter_id " +
                "LEFT JOIN live_organization o ON o.id = b.organization_id " +
                "WHERE b.subject_id = ? " +
                "ORDER BY b.received_start IS NULL, b.received_start DESC, b.created_at DESC",
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
                            dueEdtf = cursor.getString(8),
                            chapterId = cursor.getString(9),
                            chapterName = cursor.getString(10),
                            organizationName = cursor.getString(11),
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
        db().database.write(
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
        personId: String? = null,
    ) = withContext(Dispatchers.IO) {
        val columns = dateColumns("scheduled", scheduled) + mapOf(
            "title" to title,
            "location_note" to locationNote?.ifBlank { null },
            "notes" to notes?.ifBlank { null },
            "person_id" to personId,
        )
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
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
        /**
         * The folder, which a correction may set **or clear**.
         *
         * Blank becomes null rather than an empty string, so "no folder" is one
         * value rather than two that look the same on a screen and different in
         * an archive. 8.4's absence rule.
         */
        category: String? = null,
        /**
         * When the paper is from, corrected.
         *
         * **This did not exist and the date could not be changed at all**,
         * while `createDocument` stamped today into every row, so a letter
         * from three weeks ago was dated the day it was photographed forever.
         * #339, and rule 17 requires every date be editable from the entry
         * itself.
         *
         * Null leaves the four columns alone, which is what a caller with
         * nothing to say about the date means.
         */
        received: Edtf.Date? = null,
    ) = withContext(Dispatchers.IO) {
        val date = received?.let { dateColumns("received", it) }.orEmpty()
        val sets = listOf("title = ?", "original_location = ?", "notes = ?", "category = ?") +
            date.keys.map { "$it = ?" } +
            listOf("updated_at = ?", "rev = rev + 1")
        db().database.write(
            "UPDATE document SET " + sets.joinToString(", ") + " WHERE id = ?",
            (
                listOf<Any?>(
                    title,
                    originalLocation?.ifBlank { null },
                    notes?.ifBlank { null },
                    category?.trim()?.ifBlank { null },
                ) + date.values + listOf<Any?>(
                    System.currentTimeMillis(),
                    documentId,
                )
                ).toTypedArray(),
        )
    }

    /**
     * The folders this notebook already has, most used first.
     *
     * **Offered rather than imposed.** A folder is the person's own word for a
     * pile of paper, so the form suggests what they have already said and lets
     * them type anything. Ordered by how much is in each, because the pile they
     * file into most is the one they are most likely to want again.
     */
    suspend fun documentFolders(subjectId: String): List<String> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT category, COUNT(*) AS n FROM live_document " +
                    "WHERE subject_id = ? AND category IS NOT NULL AND TRIM(category) <> '' " +
                    "GROUP BY category ORDER BY n DESC, category ASC",
                arrayOf(subjectId),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        }

    /** Moves a bill to another state. Nothing else about it changes. */
    suspend fun setBillState(billId: String, state: String) = withContext(Dispatchers.IO) {
        db().database.write(
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
        db().database.write(
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
        /**
         * Who it is with, when the person said so.
         *
         * **The column has been in the schema since Phase 0 and nothing wrote
         * it**, which is why the prep sheet could not filter: `question` has a
         * `person_id` whose own schema comment says "a question waiting for the
         * wound nurse should not appear on the prep sheet for a billing
         * meeting", and there was nothing on the other side to compare against.
         * #371 item 2.
         */
        val personId: String? = null,
        /** Their name, carried so the screen says who without a second query. */
        val personName: String? = null,
        /**
         * When the person said this actually happened, or null. #430.
         *
         * **Three columns and an outcome note have been in the schema since
         * Phase 0, are read by search, and were written by nothing.** So an
         * appointment slid into the past unchanged and "she came Thursday and
         * said this" had to be re-entered as a separate call or memo,
         * disconnected from the appointment it belongs to. The line the next
         * prep sheet most needs was the one that could not be written.
         *
         * **Null reads as not yet, never as did not happen.** `appointment` has
         * attended dates and no negative state, so the two cannot be
         * distinguished, and rule 13 says an unfilled slot reads as "not yet".
         * A missed state needs schema and the issue says to raise it separately.
         */
        val attendedEdtf: String? = null,
        /** What came of it, in the person's own words, or null. #430. */
        val outcomeNote: String? = null,
    )

    /** Every appointment, soonest first. */
    suspend fun appointments(subjectId: String): List<Appointment> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT a.id, a.title, a.scheduled_edtf, a.scheduled_start, " +
                    "a.location_note, a.notes, a.person_id, p.display_name, " +
                    "a.attended_edtf, a.outcome_note " +
                    "FROM live_appointment a " +
                    "LEFT JOIN live_person p ON p.id = a.person_id " +
                    "WHERE a.subject_id = ? " +
                    "ORDER BY a.scheduled_start IS NULL, a.scheduled_start",
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
                                personId = cursor.getString(6),
                                personName = cursor.getString(7),
                                attendedEdtf = cursor.getString(8),
                                outcomeNote = cursor.getString(9),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Says an appointment happened, and what came of it. #430.
     *
     * **The columns existed and nothing wrote them.** `attended_edtf`,
     * `attended_start`, `attended_end` and `outcome_note` have been in the
     * schema since Phase 0 and are read by search, so the words were findable
     * and unwritable at the same time.
     *
     * **Either half alone is a complete answer**, rule 13. Saying it happened
     * with nothing to add is ordinary, and so is writing down what came of it
     * on something whose date the person never confirmed.
     *
     * **A null [attended] does not mean it was missed.** There is no negative
     * state in the schema, so absent has to read as "not yet". The issue says
     * a missed state is a separate question for the owner.
     */
    suspend fun recordAttendance(
        appointmentId: String,
        attended: Edtf.Date?,
        outcomeNote: String? = null,
    ) = withContext(Dispatchers.IO) {
        val columns = linkedMapOf<String, Any?>()
        columns.putAll(
            attended?.let { dateColumns("attended", it) }
                ?: mapOf(
                    "attended_edtf" to null,
                    "attended_zone" to null,
                    "attended_start" to null,
                    "attended_end" to null,
                ),
        )
        // Only when given, so recording that it happened does not wipe what was
        // written about it last week. The shape #420 and #421 both needed.
        outcomeNote?.ifBlank { null }?.let { columns["outcome_note"] = it }
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
            "UPDATE appointment SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), appointmentId)).toTypedArray(),
        )
    }

    /**
     * Writes what came of an appointment, leaving the attendance alone. #430.
     *
     * **Separate from [recordAttendance] because passing a null date there
     * clears it**, and writing down what came of something is not a statement
     * that it did not happen. The two halves are independent answers, rule 13.
     */
    suspend fun recordAttendanceNote(
        appointmentId: String,
        outcomeNote: String,
    ) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE appointment SET outcome_note = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any?>(
                outcomeNote.ifBlank { null },
                System.currentTimeMillis(),
                appointmentId,
            ),
        )
    }

    /** Records an appointment. Only the title is required, and the date may be coarse. */
    suspend fun createAppointment(
        subjectId: String,
        title: String,
        scheduled: Edtf.Date,
        locationNote: String? = null,
        notes: String? = null,
        /**
         * Who it is with, and it is never required. Rule 13: somebody who was
         * told "come in Tuesday" and nothing else has an appointment.
         */
        personId: String? = null,
    ): String = insert(
        "appointment",
        mapOf(
            "subject_id" to subjectId,
            "title" to title,
            "location_note" to locationNote?.ifBlank { null },
            "notes" to notes?.ifBlank { null },
            "person_id" to personId,
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

    /**
     * What one chapter holds, counted. #356.
     *
     * **Grid screen 19 makes the current place a card for this line**: "23
     * entries · 7 documents · 6 people · 1 open incident". The card had the
     * name and the date and said nothing about what happened there, which is
     * the thing a caregiver opens it for.
     *
     * **Four counts, from four places that already carry `chapter_id`**, and
     * the people come through `person_chapter`, which is the table that exists
     * because a nurse at the rehab is a different row from the same human at
     * the nursing home.
     */
    data class ChapterContents(
        val entries: Int,
        val documents: Int,
        val people: Int,
        val openIncidents: Int,
    ) {
        val isEmpty: Boolean get() = entries == 0 && documents == 0 &&
            people == 0 && openIncidents == 0
    }

    /**
     * What every chapter holds, counted, by chapter id.
     *
     * **Four queries for the whole notebook rather than four per chapter.** The
     * first version asked per chapter and the shell called it in a loop, so a
     * five year record with eleven chapters ran forty four queries on every
     * reload for one line of text. Grouped, it is four, whatever the record
     * holds.
     *
     * **A chapter with nothing in it is absent from the map** rather than
     * present with four zeroes, so a caller cannot render "no documents" on the
     * place somebody is living now.
     */
    suspend fun chapterContents(subjectId: String): Map<String, ChapterContents> =
        withContext(Dispatchers.IO) {
            val database = db().database
            val entries = mutableMapOf<String, Int>()
            val documents = mutableMapOf<String, Int>()
            val people = mutableMapOf<String, Int>()
            val incidents = mutableMapOf<String, Int>()

            fun fill(into: MutableMap<String, Int>, sql: String) {
                database.rawQuery(sql, arrayOf(subjectId)).use { cursor ->
                    while (cursor.moveToNext()) {
                        if (!cursor.isNull(0)) into[cursor.getString(0)] = cursor.getInt(1)
                    }
                }
            }

            fill(
                entries,
                "SELECT chapter_id, COUNT(*) FROM live_entry " +
                    "WHERE subject_id = ? AND chapter_id IS NOT NULL GROUP BY chapter_id",
            )
            fill(
                documents,
                "SELECT chapter_id, COUNT(*) FROM live_document " +
                    "WHERE subject_id = ? AND chapter_id IS NOT NULL GROUP BY chapter_id",
            )
            fill(
                people,
                "SELECT pc.chapter_id, COUNT(DISTINCT pc.person_id) " +
                    "FROM live_person_chapter pc " +
                    "JOIN live_person p ON p.id = pc.person_id " +
                    "WHERE p.subject_id = ? AND p.archived_at IS NULL " +
                    "GROUP BY pc.chapter_id",
            )
            fill(
                incidents,
                "SELECT chapter_id, COUNT(*) FROM live_incident " +
                    "WHERE subject_id = ? AND chapter_id IS NOT NULL " +
                    "AND resolved_at IS NULL GROUP BY chapter_id",
            )

            (entries.keys + documents.keys + people.keys + incidents.keys)
                .associateWith { id ->
                    ChapterContents(
                        entries = entries[id] ?: 0,
                        documents = documents[id] ?: 0,
                        people = people[id] ?: 0,
                        openIncidents = incidents[id] ?: 0,
                    )
                }
                .filterValues { !it.isEmpty }
        }

    /**
     * They moved: the place they were ends today and a new one begins today.
     *
     * **This is what a chapter boundary is, and stating one without making one
     * is the screen telling the person something the record does not say.** A
     * chapter is current because it has no end date, so starting a second one
     * without ending the first leaves two places somebody is in at once, and
     * the chapters screen shows both under "where they are now".
     *
     * **Ending a chapter destroys nothing.** It writes a date on it, and every
     * entry, incident, document and milestone filed against it stays exactly
     * where it was. That is what #202 promises in its own words, and it is why
     * this is a date rather than a deletion.
     *
     * **Today, at day precision, because that is what anybody knows.** Not the
     * minute: a minute would be a claim about when the move happened, and
     * nobody said that. Rule 17.
     *
     * **Only the chapters with no end date are touched**, so a stay somebody
     * already closed by hand keeps the date they gave it.
     */
    suspend fun moveToChapter(subjectId: String, name: String): String =
        withContext(Dispatchers.IO) {
            val today = Edtf.day(LocalDate.now())
            val ending = dateColumns("ended", today)
            val assignments = ending.keys.joinToString(", ") { "$it = ?" }
            // **One transaction, and it was two unwrapped writes.** #423.
            // Failing between them leaves no open chapter, so new entries file
            // nowhere and the spine has a gap that no screen can repair.
            val handle = db()
            val database = handle.database
            database.beginTransaction()
            try {
                database.write(
                    "UPDATE chapter SET $assignments, updated_at = ?, rev = rev + 1 " +
                        "WHERE subject_id = ? AND deleted_at IS NULL " +
                        "AND (ended_edtf IS NULL OR ended_edtf = '')",
                    (
                        ending.values + listOf(System.currentTimeMillis(), subjectId)
                        ).toTypedArray(),
                )
                val id = insertRow(
                    database,
                    handle.deviceId,
                    "chapter",
                    mapOf("subject_id" to subjectId, "name" to name) +
                        dateColumns("started", today),
                )
                database.setTransactionSuccessful()
                id
            } finally {
                database.endTransaction()
            }
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

    // -- milestones, the arc ---------------------------------------------------

    /**
     * One thing somebody decided was worth marking.
     *
     * **The app never decides one.** A milestone is not derived from anything:
     * not from a count of entries, not from a measurement crossing a number,
     * not from a chapter starting. Rule 2, and the reason this is a table of
     * its own rather than a flag on an entry. "First day without oxygen" is a
     * sentence only the person watching can write.
     *
     * **The table shipped in `contract/schema.sql` and nothing read it or wrote
     * it until 2026-08-04**, which meant an export carried a section of
     * milestones the person had no way to have written. #234.
     *
     * `chapterName` comes from the join rather than being stored, so a chapter
     * renamed later renames itself here too.
     */
    data class Milestone(
        val id: String,
        val label: String,
        val occurredEdtf: String?,
        val occurredStart: Long?,
        val chapterId: String?,
        val chapterName: String?,
        val note: String?,
    )

    /**
     * Every milestone for one subject, oldest first.
     *
     * **Oldest first, unlike the trail.** The trail answers "what happened
     * lately" and this answers "how did we get here", which is read forward.
     *
     * **An unknown date sorts last rather than first.** A milestone somebody
     * marked without a date still happened, and putting it at the head of the
     * arc would claim it happened before everything else. It goes at the end,
     * where it reads as "and also this", which is what it is.
     */
    suspend fun milestones(subjectId: String): List<Milestone> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT m.id, m.label, m.occurred_edtf, m.occurred_start, " +
                    "m.chapter_id, c.name, m.note " +
                    "FROM live_milestone m " +
                    "LEFT JOIN live_chapter c ON c.id = m.chapter_id " +
                    "WHERE m.subject_id = ? " +
                    "ORDER BY m.occurred_start IS NULL, m.occurred_start, m.created_at",
                arrayOf(subjectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            Milestone(
                                id = cursor.getString(0),
                                label = cursor.getString(1),
                                occurredEdtf = cursor.getString(2),
                                occurredStart =
                                    if (cursor.isNull(3)) null else cursor.getLong(3),
                                chapterId = cursor.getString(4),
                                chapterName = cursor.getString(5),
                                note = cursor.getString(6),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Marks one.
     *
     * Only the label is required, and the date may be as coarse as the person
     * gave it or unknown, which is a real answer and saves like any other.
     */
    suspend fun createMilestone(
        subjectId: String,
        label: String,
        occurred: Edtf.Date,
        chapterId: String? = null,
        note: String? = null,
    ): String = insert(
        "milestone",
        mapOf(
            "subject_id" to subjectId,
            "label" to label,
            "chapter_id" to chapterId,
            "note" to note?.ifBlank { null },
        ) + dateColumns("occurred", occurred),
    )

    /** Changes what was marked, or when. Every date stays editable forever. */
    suspend fun updateMilestone(
        milestoneId: String,
        label: String,
        occurred: Edtf.Date,
        /**
         * Where they were, and **null is a value here rather than "leave it
         * alone"**. The form offers the chapter chips when correcting as well
         * as when marking, and tapping the chosen one again clears it, so a
         * writer that ignored null would show a control that does nothing.
         */
        chapterId: String? = null,
        note: String? = null,
    ) = withContext(Dispatchers.IO) {
        val columns = dateColumns("occurred", occurred) + mapOf(
            "label" to label,
            "chapter_id" to chapterId,
            "note" to note?.ifBlank { null },
        )
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
            "UPDATE milestone SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), milestoneId)).toTypedArray(),
        )
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
            db().database.write(
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
    data class ThreadWithCount(
        val thread: CareThread,
        val entryCount: Int,
        /**
         * When something last happened on this thread, or null if nothing has.
         *
         * **What makes a thread the one that is moving.** A care notebook runs
         * several of these at once and they are almost never equally live: one
         * has a grievance going through it this week and four have not been
         * touched since spring. Ordering by when the thread was created says
         * nothing about which is which.
         */
        val lastActivity: Long? = null,
        /** The EDTF the person gave when the thread ended, or null if it runs. */
        val endedEdtf: String? = null,
    )

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
                "SELECT t.id, t.label, t.color_index, COUNT(et.id), " +
                    "MAX(e.occurred_start), t.ended_edtf " +
                    "FROM live_care_thread t " +
                    "LEFT JOIN live_entry_thread et ON et.thread_id = t.id " +
                    "LEFT JOIN live_entry e ON e.id = et.entry_id " +
                    "WHERE t.subject_id = ? " +
                    "GROUP BY t.id, t.label, t.color_index, t.ended_edtf, " +
                    "t.sort_index, t.created_at " +
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
                                    // **Carried on the thread too.** #433. The
                                    // threads screen reads the end date off the
                                    // row beside it, but the thread object is
                                    // what opens, so without this the screen for
                                    // an ended thread offered to end it again.
                                    endedEdtf = cursor.getString(5),
                                ),
                                entryCount = cursor.getInt(3),
                                lastActivity = if (cursor.isNull(4)) null else cursor.getLong(4),
                                endedEdtf = cursor.getString(5),
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
        /** The medication it is about, when it is about one. */
        val medicationId: String? = null,
        /**
         * That medication's name, carried so a question can say what it is
         * about without the screen fetching it again. Null when the medication
         * has since been removed, which leaves the question standing on its
         * own rather than disappearing with it.
         */
        val medicationName: String? = null,
        /**
         * Who it is waiting on, which is what the prep sheet filters by.
         *
         * **Null is a real answer and it is the commonest one**: a question
         * nobody in particular owns is a question for whoever is in the room,
         * so it comes to every appointment rather than to none.
         */
        val personId: String? = null,
        /** Their name, so a question can say who it is for without a second query. */
        val personName: String? = null,
        /**
         * The appointment it was asked at, when it was asked at one.
         *
         * **`asked_at_appointment_id` shipped in Phase 0, is rendered by the
         * archive, is named in all four catalogs, and the only thing that had
         * ever written it was the fixture generator.** So half of the link
         * shipped: a question claiming an appointment it does not appear on.
         * #371 item 2, and the same shape as #330.
         */
        val askedAtAppointmentId: String? = null,
        val askedAtAppointmentTitle: String? = null,
    ) {
        val isOpen: Boolean get() = askedEdtf.isNullOrBlank()
    }

    private val questionColumns =
        "SELECT q.id, q.text, q.role_label, q.entry_id, q.asked_edtf, q.answer_text, " +
            "q.medication_id, m.name, q.person_id, p.display_name, " +
            "q.asked_at_appointment_id, a.title FROM live_question q " +
            "LEFT JOIN live_medication m ON m.id = q.medication_id " +
            "LEFT JOIN live_person p ON p.id = q.person_id " +
            "LEFT JOIN live_appointment a ON a.id = q.asked_at_appointment_id "

    private fun android.database.Cursor.toQuestion() = Question(
        id = getString(0),
        text = getString(1),
        roleLabel = getString(2),
        entryId = getString(3),
        askedEdtf = getString(4),
        answerText = getString(5),
        medicationId = getString(6),
        medicationName = getString(7),
        personId = getString(8),
        personName = getString(9),
        askedAtAppointmentId = getString(10),
        askedAtAppointmentTitle = getString(11),
    )

    /** Everything to ask, still waiting first, oldest first within each group. */
    suspend fun questions(subjectId: String): List<Question> = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            questionColumns + "WHERE q.subject_id = ? " +
                "ORDER BY q.asked_edtf IS NOT NULL, q.created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toQuestion()) }
        }
    }

    /**
     * The questions waiting to be asked about one medication.
     *
     * `MASTER_SPEC.md` section 3 promises a medication knows its pending
     * questions, and until now nothing wrote `question.medication_id` and
     * nothing read it. **Open ones only**, because this is the list somebody
     * carries into the room, and one already asked has its answer on the
     * question itself rather than here.
     */
    suspend fun openQuestionsAbout(medicationId: String): List<Question> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                questionColumns +
                    "WHERE q.medication_id = ? AND (q.asked_edtf IS NULL OR q.asked_edtf = '') " +
                    "ORDER BY q.created_at",
                arrayOf(medicationId),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.toQuestion()) }
            }
        }

    /**
     * How many questions are still waiting on each medication, by medication id.
     *
     * **The list row was the one place this link did not go both ways**, #352.
     * The capture form can attach a question to a medication and the
     * medication's own screen lists them, so the only way to find out that
     * something is waiting was to open each medication in turn. Grid screen 12
     * draws it on the row, and the caption says why: a question already
     * attached shows as a door rather than a mystery.
     *
     * **Open ones only**, the same rule [openQuestionsAbout] uses, because a
     * question that has been asked has its answer on the question itself.
     *
     * **A medication with none is absent from the map** rather than present
     * with a zero, so a caller cannot render "0 questions" by accident.
     */
    suspend fun openQuestionCountsByMedication(subjectId: String): Map<String, Int> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT medication_id, COUNT(*) FROM live_question " +
                    "WHERE subject_id = ? AND medication_id IS NOT NULL " +
                    "AND (asked_edtf IS NULL OR asked_edtf = '') " +
                    "GROUP BY medication_id",
                arrayOf(subjectId),
            ).use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(0), cursor.getInt(1))
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
        /**
         * The medication this is about, when it is about one.
         *
         * `MASTER_SPEC.md` section 3: "a medication knows its own incidents,
         * its pending questions, its dose history, and its place on the
         * Emergency Card." The column has been in the schema since Phase 0 and
         * **nothing wrote to it and nothing read it**, so that clause of the
         * promise had no data behind it at all. Half a question about a
         * medication is somebody typing the drug's name into the text and
         * hoping to find it again by searching.
         */
        medicationId: String? = null,
        /**
         * Who it is waiting on, as a link rather than as words.
         *
         * **The capture form has always collected this and this writer never
         * received it.** Choosing the charge nurse from the chips wrote her
         * name into `role_label` and linked the entry to her, and the question
         * itself pointed at nobody, so `question.person_id` sat in the schema
         * with the comment "a question waiting for the wound nurse should not
         * appear on the prep sheet for a billing meeting" and no writer at all.
         * That is the whole of #371's root cause in one argument.
         */
        personId: String? = null,
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
                    "medication_id" to medicationId,
                    "person_id" to personId,
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
        /**
         * Which appointment it was asked at, when it was ticked off a prep
         * sheet rather than off the list.
         *
         * **Null is right when it was not.** A question asked on the phone on a
         * Tuesday belongs to no appointment, and stamping the nearest one would
         * be the app inventing a fact about where somebody was.
         */
        appointmentId: String? = null,
    ) = withContext(Dispatchers.IO) {
        // **answer_text is only written when it was given.** #420.
        //
        // This used to write `answer_text = ?` unconditionally, and the only
        // caller in the app passes no answer. `NotebookShell` sets `savingAnswer`
        // and `markingAsked` in the same handler, two independent
        // `LaunchedEffect` blocks then run, and if this one landed second it
        // wrote NULL over the answer that had just been typed and saved. The
        // person types an answer to a question, it saves, and it is gone.
        // Silent.
        //
        // **Leaving the column alone rather than reordering the two writes**,
        // because ordering two effects makes the race less likely and this
        // makes it impossible. Marking a question asked genuinely has nothing
        // to say about its answer, so the honest shape is not to write it.
        val columns = dateColumns("asked", asked) +
            mapOf("asked_at_appointment_id" to appointmentId) +
            (answerText?.ifBlank { null }?.let { mapOf("answer_text" to it) } ?: emptyMap())
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
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
     *
     * **A section's count is the number of rows its own screen shows**, which
     * is why [Section.hiddenWhen] exists. The care team said ten and opened
     * onto nine: `people` excludes archived rows and this counted them, so a
     * person somebody had marked as no longer involved went on being counted
     * by the table of contents forever. Found on 2026-08-03 by counting the
     * rows on the screen the number opens, which is the only way it can be
     * found. **A count that disagrees with its own screen is the app being
     * wrong about itself**, which is worse than not counting.
     */
    /**
     * What is not settled, in minor units, and the currency it is in.
     *
     * **The notebook's Money row says an amount rather than a count**, which is
     * what the grid draws: "$15,072.98 not settled". "6 bills" is true and is
     * not the number somebody opens Money to find. #347.
     *
     * **Open means the same thing here as on the Money screen**, `isOpen`:
     * everything except paid and closed. Two places deciding separately what
     * counts as settled is how a total on one screen stops matching the total
     * on another.
     *
     * **A bill with no amount is left out rather than added as zero**, for the
     * reason `MoneyScreen` gives: null is not zero, and a bill that arrived
     * saying "this is not a bill" is still a real bill.
     *
     * Returns null when there is nothing unsettled to total, so the row falls
     * back to counting. **A zero on a settled notebook is a number with nothing
     * behind it**, which is the rule the Money screen already applies to its
     * own band.
     */
    suspend fun unsettledTotal(subjectId: String): Pair<Long, String>? =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT SUM(amount_minor), MIN(currency) FROM live_bill " +
                    "WHERE subject_id = ? AND state NOT IN ('paid', 'closed') " +
                    "AND amount_minor IS NOT NULL",
                arrayOf(subjectId),
            ).use {
                if (!it.moveToFirst() || it.isNull(0)) return@use null
                val total = it.getLong(0)
                if (total <= 0L) null else total to (it.getString(1) ?: "USD")
            }
        }

    suspend fun count(section: Section, subjectId: String): Int = withContext(Dispatchers.IO) {
        db().database.rawQuery(
            "SELECT COUNT(*) FROM ${section.view} WHERE subject_id = ?" +
                section.hiddenWhen.orEmpty(),
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
    enum class Section(
        internal val view: String,
        /**
         * A predicate the section's own screen applies and the count has to
         * apply with it, appended to the `WHERE` clause.
         *
         * **Only for rows the screen genuinely does not show.** A stopped
         * medication is still on the medications screen and is still counted; an
         * archived person is on neither. The test is what the screen lists, not
         * what the row means.
         */
        internal val hiddenWhen: String? = null,
    ) {
        CARE_TEAM("live_person", hiddenWhen = " AND archived_at IS NULL"),
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

        /**
         * Memos, which are entries and are counted as the ones that are memos.
         *
         * **The person reads "Memo"; the record says `note`.** The owner,
         * 2026-08-18: "the name is problematic since we have Notebook. let's
         * call it Memo." **Only the words changed.** `entry.kind` is `'note'`
         * in the schema's `CHECK` constraint and stays there, because rule 3
         * fixes the schema and because renaming a stored value would make every
         * archive written before today unreadable by the app that wrote it. The
         * catalogs carry the word somebody sees, which is what catalogs are
         * for.
         *
         * **The view is the entry view and the predicate is the kind**, which is
         * exactly what `hiddenWhen` is for: a note is not a table of its own,
         * D207, and giving it one would have been a second answer to a question
         * the schema settled in Phase 0.
         */
        NOTES("live_entry", hiddenWhen = " AND kind = 'note'"),
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
                    val kind = cursor.getString(6)
                    hits += SearchHit(
                        id = cursor.getString(0),
                        // **A note says Notes, not The trail.** #397: notes are
                        // entries and are searched with them, so without this a
                        // hit landed under the section the row's table belongs
                        // to rather than under the one the person opened it
                        // from. The kind is the only thing that separates them
                        // and it is already read here.
                        section = if (section == Section.TRAIL && kind == "note") {
                            Section.NOTES
                        } else {
                            section
                        },
                        title = cursor.getString(1) ?: "",
                        detail = cursor.getString(2),
                        chapterName = cursor.getString(3),
                        occurredEdtf = cursor.getString(4),
                        occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                        kind = kind,
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
        // **Care threads were not searched at all.** #429. A thread's label is
        // how somebody refers to a whole running situation, "the appeal", "the
        // wound", and the empty state promises this looks through every section
        // at once. The end note goes in with it, since a finished thread's last
        // word is exactly the kind of thing somebody comes back for.
        run(Section.THREADS, "live_care_thread", "label", "end_note", "started",
            listOf("label", "end_note"),
            hasChapter = false)

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
        /**
         * Where she was when it happened. #371.
         *
         * **The chapter axis had readers everywhere and no writer.** A chapter
         * could never hold an incident, so `ChapterScreen`'s incidents group
         * could never be non-empty and "has this happened at this place
         * before" was unanswerable. Stamped on the incident and on its own
         * entry, because both are read by chapter.
         */
        chapterId: String? = null,
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
                    "chapter_id" to chapterId,
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
                    "chapter_id" to chapterId,
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
    /**
     * Everybody named on an incident, gathered from its own thread.
     *
     * `MASTER_SPEC.md` section 3: "an incident knows its project, its
     * documents, and its people." **The people half needed no new column and no
     * new writer**, because every call chasing an incident is an ordinary entry
     * and entries learned to name who they involved. It is a join nobody had
     * written rather than data nobody had.
     *
     * **Distinct, and in the order they first appear on the thread**, which is
     * the order somebody would recount it: who I reported it to, then who I was
     * escalated to. A name repeated across four calls is one person.
     */
    /**
     * Everything hanging off one incident, in one object.
     *
     * **Four readers behind one call, the way `EntryDetail` already does it.**
     * Not for the round trips: for the shell. `NotebookShell` is a single
     * composable sitting at the JVM's 64KB method limit, B6, and every list
     * passed to a screen is a parameter at a call site inside it. Adding the
     * violations as a fifth list failed the build outright, in a message naming
     * the shell rather than the change. Four names collapsing into one is what
     * made room for the fifth.
     */
    data class IncidentDetail(
        val entries: List<TrailEntry> = emptyList(),
        val people: List<Person> = emptyList(),
        val documents: List<Document> = emptyList(),
        /**
         * Every time a request was not followed that names this incident.
         *
         * **The other half of rule 18**, which four of the five panels called a
         * dead end wearing a disguise: a violation could name an incident from
         * the moment the form began to ask, and the incident said nothing back.
         */
        val violations: List<Violation> = emptyList(),
    )

    suspend fun incidentDetail(incidentId: String) = IncidentDetail(
        entries = incidentTrail(incidentId),
        people = peopleOnIncident(incidentId),
        documents = documentsOnIncident(incidentId),
        violations = violationsLinkedTo(incidentId),
    )

    suspend fun peopleOnIncident(incidentId: String): List<Person> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT DISTINCT p.id, p.display_name, p.role_label, p.phone, " +
                    "p.email, p.notes " +
                    "FROM live_entry e " +
                    "JOIN live_entry_person ep ON ep.entry_id = e.id " +
                    "JOIN live_person p ON p.id = ep.person_id " +
                    "WHERE e.incident_id = ? " +
                    "ORDER BY e.occurred_start IS NULL, e.occurred_start, e.created_at",
                arrayOf(incidentId),
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

    /**
     * The paperwork that came out of an incident.
     *
     * The other half that needed nothing new: a document already points at the
     * entry it was saved against, and an entry already knows its incident. The
     * grievance somebody filed and the letter they were sent are the documents
     * that matter most six months later, and they were reachable only by
     * scrolling the documents section.
     */
    suspend fun documentsOnIncident(incidentId: String): List<Document> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT d.id, d.title, d.category, d.original_location, d.notes, " +
                    "d.received_edtf, a.sha256, a.byte_size " +
                    "FROM live_document d " +
                    "JOIN live_entry e ON e.id = d.entry_id " +
                    "LEFT JOIN live_attachment a ON a.document_id = d.id " +
                    "WHERE e.incident_id = ? " +
                    "ORDER BY d.received_start IS NULL, d.received_start, d.created_at",
                arrayOf(incidentId),
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

    suspend fun incidentTrail(incidentId: String): List<TrailEntry> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, " +
                    "is_unfiled, pinned_at FROM live_entry WHERE incident_id = ? " +
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
                                pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
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
    /**
     * Corrects what an incident says and when it was reported. #358.
     *
     * **An incident is typed in a hurry, at the worst moment, one handed**, and
     * it was the one thing in this app that could not be fixed afterward: the
     * only `UPDATE incident` set `resolved_at`. `renameProject` carries the
     * sentence the rest of the app already lives by, "every name in this app is
     * a correction away", and this is the row where it matters most.
     *
     * **The date keeps whatever precision was given**, per rule 17 and the date
     * columns the schema carries for it, so correcting a title does not quietly
     * sharpen "sometime in March" into a day.
     *
     * **What happened next is untouched.** Those are entries, and they are
     * corrected where entries are corrected.
     */
    suspend fun updateIncident(
        incidentId: String,
        title: String,
        description: String?,
        reported: Edtf.Date?,
    ) = withContext(Dispatchers.IO) {
        val dates = reported?.let { dateColumns("reported", it) }
            ?: mapOf(
                "reported_edtf" to null,
                "reported_zone" to null,
                "reported_start" to null,
                "reported_end" to null,
            )
        val columns = linkedMapOf<String, Any?>(
            "title" to title.trim(),
            "description" to description?.ifBlank { null },
        )
        columns.putAll(dates)
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        val now = System.currentTimeMillis()
        val database = db().database
        database.beginTransaction()
        try {
            database.write(
                "UPDATE incident SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
                (columns.values + listOf(now, incidentId)).toTypedArray(),
            )

            // **The mirror entry is re-stamped, and it never was.** #429.
            //
            // `reportIncident` writes an `entry` beside the incident carrying
            // the same title and description, and **search reads `live_entry`,
            // not `incident`**. Correcting an incident updated only the
            // incident, so the trail went on showing the old title and the
            // corrected words matched nothing: **a corrected incident was
            // findable only by the words the person had just replaced.**
            //
            // Written in the same transaction as the correction, because a
            // record and the copy search reads are one fact, and half of that
            // is what produced the defect.
            database.write(
                "UPDATE entry SET title = ?, body = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE incident_id = ? AND deleted_at IS NULL",
                arrayOf<Any?>(
                    title.trim().ifBlank { null },
                    description?.ifBlank { null },
                    now,
                    incidentId,
                ),
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Removes an incident, as a tombstone. #358, rule 3.
     *
     * **Not through [delete] and its `Section`**, because incidents have no
     * section of their own in the notebook's table of contents and adding one
     * to that enum to reach this would put a thirteenth row on a screen the
     * grid draws with twelve.
     *
     * **The entries written under it are not removed with it.** They are the
     * record of what happened and they survive the grouping being taken away,
     * which is the same call [delete] makes for a thread: the links stay and
     * the chips stop offering it.
     */
    suspend fun removeIncident(incidentId: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE incident SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ? AND deleted_at IS NULL",
            arrayOf<Any?>(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                incidentId,
            ),
        )
    }

    suspend fun resolveIncident(
        incidentId: String,
        resolvedAt: Long?,
        resolutionNote: String? = null,
    ) = withContext(Dispatchers.IO) {
        // The same shape `markQuestionAsked` uses, so the change log trigger
        // sees an ordinary update and `rev` moves the way every other write
        // moves it.
        //
        // **The note is written only when it is given.** #421, and it is the
        // same defect #420 had: this wrote `resolution_note = ?`
        // unconditionally, so **reopening an incident wrote NULL over the note
        // saying what was done about it last time.** That is exactly the thing
        // a recurring problem is looked up for.
        //
        // Reopening passes no note because reopening has nothing to say about
        // one, so leaving the column alone is both the fix and the honest
        // shape.
        val columns = linkedMapOf<String, Any?>("resolved_at" to resolvedAt)
        resolutionNote?.ifBlank { null }?.let { columns["resolution_note"] = it }
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
            "UPDATE incident SET $assignments, updated_at = ?, rev = rev + 1 WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), incidentId)).toTypedArray(),
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
            db().database.write(
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
        /**
         * The medication a question is about, when it is about one.
         *
         * **The other half of a link that was one way for an hour.** A
         * medication learned to show its pending questions and the question's
         * own entry still said nothing about the medication, which rule 18
         * calls a dead end wearing a disguise. Reached through the question
         * row, because the link belongs to the question rather than to the
         * entry: the entry is what appears on the trail, and it is the question
         * that is about a drug.
         */
        val medicationId: String?,
        val medicationName: String?,
        /**
         * The long processes this entry is about. Rule 18, and #283.
         *
         * **The other half of a link that was one way.** A project shows every
         * entry connected to it, `entriesAbout`, and logging a call from inside
         * a project writes that connection. The entry said nothing back, so the
         * call somebody logged from a Medicaid application opened onto a screen
         * with no way to get to the Medicaid application: a dead end wearing a
         * disguise, and the last one on this screen.
         *
         * **A list rather than one.** 8.1's `link` table does not stop an entry
         * being connected to two projects, and a person who rings one office
         * about two applications has done exactly that. The screen draws each.
         */
        val projects: List<EntryProject> = emptyList(),
        /** Where this entry sits in each thread it belongs to, keyed by thread id. #348. */
        val threadPositions: Map<String, ThreadPosition> = emptyMap(),
    )

    /**
     * A project as an entry names it: enough to draw a door and no more.
     *
     * **Not [Project], which costs three more queries per row** for step counts
     * and a next step that nothing on the entry screen shows.
     */
    data class EntryProject(val id: String, val name: String, val status: String)

    /**
     * Where one entry sits inside one of its threads.
     *
     * **Per link rather than per entry**, because an entry can belong to more
     * than one thread and is a different step of each.
     */
    data class ThreadPosition(val step: Int, val total: Int)

    /** One entry read on its own, or null when it is gone. */
    suspend fun entry(entryId: String): EntryDetail? = withContext(Dispatchers.IO) {
        val database = db().database
        val threads = mutableListOf<CareThread>()
        val positions = mutableMapOf<String, ThreadPosition>()
        database.rawQuery(
            // **Where this entry sits in each of its threads.** A thread is a
            // sequence somebody is trying to follow, so the entry's own screen
            // says which step it is rather than repeating the word "thread",
            // per the grid's screen 09 and #348.
            //
            // **Counted from the oldest**, which is the opposite of how the
            // thread screen lists them. That screen shows the newest first
            // because that is what somebody scanning wants; a step number only
            // means anything counting forward from the start.
            //
            // **Ties keep the same number rather than being broken by id.** Two
            // entries written for the same moment are the same step of the
            // story, and inventing an order between them would be the app
            // deciding something the record does not say.
            "SELECT t.id, t.label, t.color_index, " +
                "(SELECT COUNT(*) FROM live_entry_thread p " +
                " JOIN live_entry pe ON pe.id = p.entry_id " +
                " WHERE p.thread_id = t.id AND coalesce(pe.occurred_start, pe.created_at) <= " +
                "   coalesce(e.occurred_start, e.created_at)) AS step, " +
                "(SELECT COUNT(*) FROM live_entry_thread a WHERE a.thread_id = t.id) AS total " +
                "FROM live_entry_thread et " +
                "JOIN live_care_thread t ON t.id = et.thread_id " +
                "JOIN live_entry e ON e.id = et.entry_id " +
                "WHERE et.entry_id = ? ORDER BY t.sort_index, t.created_at",
            arrayOf(entryId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                threads += CareThread(id, cursor.getString(1), cursor.getInt(2))
                positions[id] = ThreadPosition(cursor.getInt(3), cursor.getInt(4))
            }
        }

        database.rawQuery(
            "SELECT e.id, e.kind, e.title, e.body, e.occurred_edtf, e.occurred_start, " +
                "e.created_at, e.is_unfiled, e.chapter_id, c.name, " +
                "e.incident_id, i.title, i.resolved_at, q.medication_id, m.name, " +
                "e.pinned_at " +
                "FROM live_entry e " +
                "LEFT JOIN live_chapter c ON c.id = e.chapter_id " +
                "LEFT JOIN live_incident i ON i.id = e.incident_id " +
                "LEFT JOIN live_question q ON q.entry_id = e.id " +
                "LEFT JOIN live_medication m ON m.id = q.medication_id " +
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
                    pinnedAt = if (cursor.isNull(15)) null else cursor.getLong(15),
                ),
                chapterId = cursor.getString(8),
                chapterName = cursor.getString(9),
                incidentId = cursor.getString(10),
                incidentTitle = cursor.getString(11),
                incidentIsOpen = cursor.isNull(12),
                medicationId = cursor.getString(13),
                medicationName = cursor.getString(14),
                projects = projectsOnEntryBlocking(database, entryId),
                threadPositions = positions,
            )
        }
    }

    /**
     * The projects one entry is connected to, in the order they were linked.
     *
     * **Both directions of the link are accepted**, exactly as [entriesAbout]
     * and [latestWordFor] do. A link written from the entry and one written
     * from the project mean the same thing to a person, and a screen that
     * showed the connection only when the row happened to be written one way
     * round is the dead end rule 18 is about.
     *
     * **Tombstoned projects are excluded by `live_project`**, so an entry about
     * a project somebody removed says nothing rather than offering a door onto
     * a screen that is gone.
     */
    private fun projectsOnEntryBlocking(
        database: net.zetetic.database.sqlcipher.SQLiteDatabase,
        entryId: String,
    ): List<EntryProject> = database.rawQuery(
        "SELECT p.id, p.name, p.status FROM live_link l " +
            "JOIN live_project p ON p.id = " +
            "  CASE WHEN l.source_table = 'project' THEN l.source_id ELSE l.target_id END " +
            "WHERE (l.source_table = 'entry' AND l.source_id = ? AND l.target_table = 'project') " +
            "   OR (l.target_table = 'entry' AND l.target_id = ? AND l.source_table = 'project') " +
            "ORDER BY l.created_at",
        arrayOf(entryId, entryId),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    EntryProject(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        status = cursor.getString(2),
                    ),
                )
            }
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
                    "e.created_at, e.is_unfiled, e.pinned_at FROM live_entry e " +
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
                                pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
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
         * What was asked at this appointment, so it does not simply vanish.
         *
         * **The other half of the link, per rule 18.** A question stamped with
         * this appointment said so on its own face and the appointment said
         * nothing back, and a ticked question disappearing off the sheet is
         * also the shape somebody reads as data loss.
         *
         * **Carried inside `Prep` rather than as a second parameter**, which is
         * the B6 lesson: every parameter crossing into a screen is bytecode
         * inside `NotebookShell`, and this object was already going there.
         */
        val asked: List<Question> = emptyList(),
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

            // **The questions for the person you are about to see, plus the
            // ones waiting on nobody in particular.**
            //
            // **It used to be every open question in the notebook**, which is
            // what #371 item 2 named: a question written for the wound nurse
            // came to the billing meeting, and on a year five notebook the
            // sheet was a wall of things nobody in that room could answer. The
            // schema's own comment on `question.person_id` has said so since
            // Phase 0; what was missing was anything writing either side.
            //
            // **A question with nobody on it comes to every appointment.**
            // That is a real answer rather than a gap: most questions are for
            // whoever is in the room, and filtering them out would hide the
            // commonest kind. Rule 13, and it is why this is a filter on a link
            // rather than a requirement to choose one.
            //
            // **An appointment with nobody on it shows everything**, because
            // there is nothing to compare against and a sheet that hid
            // questions on the strength of a link nobody made would be the app
            // deciding for them.
            val everything = questions(subjectId)
            val open = everything
                .filter { it.isOpen }
                .filter { question ->
                    appointment.personId == null ||
                        question.personId == null ||
                        question.personId == appointment.personId
                }

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
                asked = everything.filter { it.askedAtAppointmentId == appointmentId },
            )
        }

    // ---- month review ---------------------------------------------------

    /**
     * How many of one kind of thing were written down in the month.
     *
     * **A count, and the app stops there.** `MASTER_SPEC.md` section 5 allows
     * counts and forbids what they mean: nothing here says a month was busy, or
     * quiet, or worse than the one before it, because all three would be the app
     * forming an opinion about somebody's care. Rule 2.
     */
    data class KindCount(val kind: String, val count: Int)

    /**
     * One month of the record, gathered.
     *
     * `MASTER_SPEC.md` section 4.5 and `DESIGN.md` section 14: a period of the
     * trail, composed, **every line tapping through to its source entry.**
     *
     * **It is a view, not a document.** Nothing here is stored, summarized, or
     * derived into a new fact. Every row in every list is the row that already
     * exists somewhere else in the notebook, gathered by the one thing they have
     * in common, which is when they happened. That is what makes each line able
     * to open its own source, and it is why section 5 can promise the output is
     * verifiable: there is nothing to verify beyond a date comparison.
     *
     * **A row belongs to the month only if its whole date fits inside it.** An
     * entry dated "2026" overlaps June and eleven other months, and putting it
     * in June's review would be the app claiming a precision the person never
     * gave. Rule 17, and `DESIGN.md` 9.2: display never invents precision. So a
     * year-precise row appears in no month's review, which is correct rather
     * than a gap, because the record never said it happened in one. A month
     * precise row does belong, and sits among the days without being given one.
     */
    data class MonthReview(
        /**
         * The first instant of the month, so the screen names the month in the
         * locale's own words rather than being handed an English string.
         */
        val monthStart: Long,
        /** What was written down, newest first, exactly as the trail holds it. */
        val entries: List<TrailEntry>,
        /** The same entries counted by kind, in the order the trail draws them. */
        val kinds: List<KindCount>,
        /** What somebody decided was worth marking, oldest first, as on the arc. */
        val milestones: List<Milestone>,
        val appointments: List<Appointment>,
        /** Incidents first reported in the month, whether or not they were answered. */
        val reported: List<Incident>,
        /**
         * Incidents answered in the month **and reported before it.**
         *
         * **Answered is a moment rather than a date somebody gave**, because it
         * is stamped when the person marks it, so it needs no precision test.
         * An incident reported in March and answered in June belongs to June's
         * review as an answer and to March's as a report, which is what
         * happened.
         *
         * **One reported and answered inside the same month is in [reported]
         * only.** It listed twice on the screen, under two headings, and read
         * as a defect rather than as two facts: what somebody learns from the
         * second row is that it was answered, which the first row can say in a
         * word. The rule is the honest one either way, since neither list is a
         * count of incidents and both are answers to "what happened in June".
         */
        val answered: List<Incident>,
        val documents: List<Document>,
        /**
         * Places that began in the month, and places that ended in it.
         *
         * **A place can be in both**, which is what an overnight stay is, and
         * the screen says so in one row rather than listing the name twice.
         */
        val began: List<Chapter>,
        val ended: List<Chapter>,
    ) {
        /**
         * True when the month holds nothing at all.
         *
         * **Not a total, and there is deliberately no total on this class.** A
         * single number over a month invites the comparison with last month
         * that rule 2 forbids the app to make, and law 1 would put it in
         * competition with the one thing the screen leads with. Each group
         * carries its own count where a count answers something.
         */
        val isEmpty: Boolean get() = entries.isEmpty() && milestones.isEmpty() &&
            appointments.isEmpty() && documents.isEmpty() && began.isEmpty() &&
            ended.isEmpty() && reported.isEmpty() && answered.isEmpty()
    }

    /**
     * Gathers one month.
     *
     * Composed over the readers that already exist rather than through a query
     * of its own, which is what `MASTER_SPEC.md` section 5 means by the engine
     * doing its arithmetic in code: the rows come from the same place the
     * screens get them, so a review can never disagree with the section it
     * points at.
     */
    suspend fun monthReview(
        subjectId: String,
        month: java.time.YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthReview = withContext(Dispatchers.IO) {
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        fun inMonth(edtf: String?): Boolean = fitsInside(edtf, from, to, zone)

        val entries = trail(subjectId).filter { inMonth(it.occurredEdtf) }
        val allIncidents = incidents(subjectId)
        val reported = allIncidents.filter { inMonth(it.reportedEdtf) }
        val reportedIds = reported.map { it.id }.toSet()

        MonthReview(
            monthStart = from,
            entries = entries,
            // The trail's own order, so a kind never moves between months.
            kinds = KIND_ORDER.mapNotNull { kind ->
                entries.count { it.kind == kind }.takeIf { it > 0 }
                    ?.let { KindCount(kind, it) }
            },
            milestones = milestones(subjectId).filter { inMonth(it.occurredEdtf) },
            appointments = appointments(subjectId).filter { inMonth(it.scheduledEdtf) },
            reported = reported,
            answered = allIncidents.filter {
                it.resolvedAt?.let { at -> at in from..to } == true && it.id !in reportedIds
            },
            documents = documents(subjectId).filter { inMonth(it.receivedEdtf) },
            began = chapters(subjectId).filter { inMonth(it.startedEdtf) },
            ended = chapters(subjectId).filter { inMonth(it.endedEdtf) },
        )
    }

    /**
     * True when everything the date could mean happens inside the window.
     *
     * **Both ends, not just the start.** Testing the start alone would put a
     * year-precise "2026" into January's review, since January is where that
     * year begins, which is the app inventing a month nobody named. An unknown
     * date and a date this version cannot read both answer false and belong to
     * no month, which is honest: neither says when it happened.
     */
    private fun fitsInside(edtf: String?, from: Long, to: Long, zone: ZoneId): Boolean {
        val parsed = edtf?.takeIf { it.isNotBlank() }?.let { Edtf.parse(it) } ?: return false
        val range = Edtf.resolve(parsed, zone)
        val start = range.start ?: return false
        val end = range.end ?: return false
        return start >= from && end <= to
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
                    "e.created_at, e.is_unfiled, e.pinned_at FROM live_entry e " +
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
                                pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * What happened while they were in one place.
     *
     * `MASTER_SPEC.md` 4.6: inside a chapter, its dates, why the stay began,
     * its incidents, its documents, and any project that began there.
     *
     * **A chapter is the app's unit of "where", and it could not be opened.**
     * The chapters screen drew the journey and every stop on it was a dead end,
     * which is the shape #46 exists to remove.
     */
    data class ChapterDetail(
        val chapter: Chapter,
        val entries: List<TrailEntry>,
        val incidents: List<Incident>,
        val documents: List<Document>,
        /**
         * What was worth marking while they were here.
         *
         * **Rule 18's other end.** A milestone names its chapter on the arc,
         * and a chapter could not name its milestones, which is a link with one
         * side. Oldest first, like the arc itself.
         */
        val milestones: List<Milestone>,
    )

    /**
     * The documents filed against one chapter.
     *
     * A query rather than a filter over every document, because `Document`
     * carries no chapter of its own and adding one to the model to make a
     * filter possible would be shaping the read model around one screen.
     */
    private fun documentsInChapter(
        database: net.zetetic.database.sqlcipher.SQLiteDatabase,
        chapterId: String,
    ): List<Document> = database.rawQuery(
        "SELECT id, title, category, original_location, notes, received_edtf " +
            "FROM live_document WHERE chapter_id = ? ORDER BY created_at DESC",
        arrayOf(chapterId),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Document(
                        id = cursor.getString(0),
                        title = cursor.getString(1) ?: "",
                        category = cursor.getString(2),
                        originalLocation = cursor.getString(3),
                        notes = cursor.getString(4),
                        receivedEdtf = cursor.getString(5),
                        sha256 = null,
                        byteSize = null,
                    ),
                )
            }
        }
    }

    suspend fun chapterDetail(subjectId: String, chapterId: String): ChapterDetail? =
        withContext(Dispatchers.IO) {
            val chapter = chapters(subjectId).firstOrNull { it.id == chapterId }
                ?: return@withContext null
            val database = db().database

            val entries = mutableListOf<TrailEntry>()
            database.rawQuery(
                "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, " +
                    "is_unfiled, pinned_at FROM live_entry WHERE chapter_id = ? " +
                    "ORDER BY coalesce(occurred_start, created_at) DESC",
                arrayOf(chapterId),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    entries += TrailEntry(
                        id = cursor.getString(0),
                        kind = cursor.getString(1),
                        title = cursor.getString(2),
                        body = cursor.getString(3),
                        occurredEdtf = cursor.getString(4),
                        occurredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                        createdAt = cursor.getLong(6),
                        isUnfiled = cursor.getInt(7) == 1,
                        threads = emptyList(),
                        pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
                    )
                }
            }

            ChapterDetail(
                chapter = chapter,
                entries = entries,
                incidents = incidents(subjectId).filter { it.chapterName == chapter.name },
                documents = documentsInChapter(database, chapterId),
                milestones = milestones(subjectId).filter { it.chapterId == chapterId },
            )
        }

    /**
     * A medication's own history: what changed, when, and where.
     *
     * `MASTER_SPEC.md` 4.6: a medication's journey crosses chapters and keeps
     * its concern flags attached forever. **That journey is what makes this a
     * record rather than a list**, and "she was on this until March, and it was
     * changed at the rehab" is the answer to a question somebody will
     * eventually be asked in a room where nobody has the notes.
     */
    data class MedicationEvent(
        val id: String,
        /** started, stopped, dose changed, held, restarted. The schema's own words. */
        val kind: String,
        val occurredEdtf: String?,
        val doseText: String?,
        val note: String?,
        val chapterName: String?,
    )

    suspend fun medicationHistory(medicationId: String): List<MedicationEvent> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT e.id, e.kind, e.occurred_edtf, e.dose_text, e.note, c.name " +
                    "FROM live_medication_event e " +
                    "LEFT JOIN live_chapter c ON c.id = e.chapter_id " +
                    "WHERE e.medication_id = ? " +
                    // Oldest first, because a medication's history is a story
                    // about how it changed, and a story told backward is not
                    // the same story. Same reasoning as an incident thread.
                    "ORDER BY coalesce(e.occurred_start, e.created_at) ASC",
                arrayOf(medicationId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            MedicationEvent(
                                id = cursor.getString(0),
                                kind = cursor.getString(1),
                                occurredEdtf = cursor.getString(2),
                                doseText = cursor.getString(3),
                                note = cursor.getString(4),
                                chapterName = cursor.getString(5),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Records something that happened to a medication.
     *
     * **The writer `medication_event` never had.** The table has been in the
     * schema since Phase 0 and the app could read a history it had no way to
     * write, so every medication's history was empty forever.
     *
     * The chapter is stamped at the moment of writing, from wherever the person
     * currently is, because that is what makes a medication's journey cross
     * chapters at all. `MASTER_SPEC.md` 4.6.
     */
    /**
     * Writes a change to a medication, **and changes the medication.** #371.
     *
     * **Recording that something was stopped used to leave it running.** The
     * event row was written and `medication.stopped_edtf` was written by
     * nothing anywhere, so `isStopped` was always false. The consequences ran
     * downhill from there: the stopped fold on the medications list stayed
     * empty, the Today count was wrong, and **the emergency card kept listing a
     * medication the person had recorded as stopped in March** on the one
     * screen whose own filter comment calls this "the one that is dangerous to
     * get wrong". Three panels found it independently.
     *
     * **A dose change used to leave the dose.** Same shape, quieter harm: the
     * list showed the old dose until somebody separately opened the correction
     * form, and nothing told them so, so most people did it once and the list
     * was wrong from then on.
     *
     * **The event and its consequence are one transaction**, which is the rule
     * this establishes: an event writer updates its parent's state columns in
     * the same breath, so no screen has to derive a state a second way and no
     * two screens can disagree about it.
     *
     * **Resuming and starting clear the stop**, because a medication somebody
     * has gone back on is not stopped, and leaving the date behind would be the
     * record contradicting itself.
     */
    suspend fun recordMedicationEvent(
        medicationId: String,
        kind: String,
        occurred: Edtf.Date,
        doseText: String?,
        note: String?,
        chapterId: String?,
    ): String = withContext(Dispatchers.IO) {
        // **One transaction, and its comment claimed one before it had one.**
        // #423. This was an insert plus up to two UPDATE medication, each in
        // its own withContext, so a stop event could be logged while the
        // medication was never marked stopped. The record then says the drug
        // was stopped and the medication list says it is still being taken,
        // and both come from the same tap.
        val handle = db()
        val database = handle.database
        database.beginTransaction()
        try {
            val id = insertRow(
                database,
                handle.deviceId,
                "medication_event",
                mapOf(
                    "medication_id" to medicationId,
                    "kind" to kind,
                    "dose_text" to doseText?.ifBlank { null },
                    "note" to note?.ifBlank { null },
                    "chapter_id" to chapterId,
                ) + dateColumns("occurred", occurred),
            )

            when (kind) {
                "stopped" -> {
                    val columns = dateColumns("stopped", occurred)
                    val assignments = columns.keys.joinToString(", ") { "$it = ?" }
                    database.write(
                        "UPDATE medication SET $assignments, updated_at = ?, rev = rev + 1 " +
                            "WHERE id = ?",
                        (columns.values + listOf(System.currentTimeMillis(), medicationId))
                            .toTypedArray(),
                    )
                }

                "started", "resumed" -> {
                    val columns = dateColumns("stopped", Edtf.unknown()).keys
                        .joinToString(", ") { "$it = NULL" }
                    database.write(
                        "UPDATE medication SET $columns, updated_at = ?, rev = rev + 1 " +
                            "WHERE id = ?",
                        arrayOf<Any?>(System.currentTimeMillis(), medicationId),
                    )
                }

                else -> Unit
            }

            // **The dose follows the change, whatever the kind.** A dose written
            // on a hold or a resume is still the dose from that day forward.
            doseText?.takeIf { it.isNotBlank() }?.let { dose ->
                database.write(
                    "UPDATE medication SET dose_text = ?, updated_at = ?, rev = rev + 1 " +
                        "WHERE id = ?",
                    arrayOf<Any?>(dose, System.currentTimeMillis(), medicationId),
                )
            }

            database.setTransactionSuccessful()
            id
        } finally {
            database.endTransaction()
        }
    }

    /**
     * The chapter the person is in now, which is the one with no end date.
     *
     * Null for a notebook where nobody has said where they are, which is a real
     * state: setup lets that question be skipped.
     */
    suspend fun currentChapterId(subjectId: String): String? =
        withContext(Dispatchers.IO) {
            chapters(subjectId).firstOrNull { it.isCurrent }?.id
        }

    // ---- standing instructions that were not followed ---------------------

    /**
     * A time an instruction was not followed.
     *
     * **This is the part of the record a family actually needs in a room.**
     * "We asked in writing in March, and it happened again in May and again in
     * June" is a different conversation from "we asked in March", and
     * `instruction_violation` has been in the schema since Phase 0 with no
     * reader and no writer.
     *
     * **A count and never a judgment.** `MASTER_SPEC.md` 4.11 and rule 2: the
     * app counts and says plainly that what the count means is the person's to
     * judge. Nothing here says a facility is bad, nothing is colored by how
     * many, and no threshold turns a number into an opinion.
     */
    data class Violation(
        val id: String,
        val occurredEdtf: String?,
        val note: String?,
        /** What it broke against, when the person linked one. */
        val incidentId: String?,
        val incidentTitle: String?,
        val billId: String?,
        val billDescription: String?,
        /**
         * Which request this was, for the screens reading from the other end.
         *
         * **Rule 18 is why this is here.** An incident that a violation names
         * has to be able to name it back, and on that screen the instruction is
         * the one thing the row cannot be read without.
         */
        val instructionId: String = "",
        val instructionName: String? = null,
    )

    /**
     * Every time an instruction was not followed, by instruction id.
     *
     * **This replaced a count and a reader nobody called**, which is the shape
     * #371 named: `violations(instructionId)` returned the person's own words
     * with both joins already written and had no call site anywhere, while the
     * screen was handed a separate `COUNT(*)`. So somebody typed "the night
     * nurse gave it at 9 instead of 6" and the app showed them a 3. **One
     * query, and the count is the list's own size**, because two readers of the
     * same rows is how a screen comes to know a number and not the sentence
     * behind it.
     *
     * **An instruction with none is absent from this map** rather than present
     * holding an empty list, the same way `openQuestionCountsByMedication`
     * decides it. Absent means nothing was written down; a zero would be a
     * finding.
     */
    suspend fun violationsBySubject(subjectId: String): Map<String, List<Violation>> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                VIOLATION_SELECT + "WHERE s.subject_id = ? " + VIOLATION_ORDER,
                arrayOf(subjectId),
            ).use { cursor ->
                buildMap<String, MutableList<Violation>> {
                    while (cursor.moveToNext()) {
                        getOrPut(cursor.getString(1)) { mutableListOf() }.add(cursor.toViolation())
                    }
                }
            }
        }

    /**
     * Every time an instruction was not followed that names this row.
     *
     * **The other half of rule 18**, and the half that did not exist: an
     * incident and a bill could be named by a violation and said nothing about
     * it, which four of the five panels called a dead end wearing a disguise.
     *
     * **One reader for both**, matching either column against the same id,
     * because every id in this notebook is generated locally and unique across
     * every table: a bill's id is never an incident's. Two functions differing
     * only in which column they compare would drift the moment either changed.
     */
    suspend fun violationsLinkedTo(rowId: String): List<Violation> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                VIOLATION_SELECT + "WHERE v.incident_id = ? OR v.bill_id = ? " + VIOLATION_ORDER,
                arrayOf(rowId, rowId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toViolation())
                }
            }
        }

    private fun android.database.Cursor.toViolation() = Violation(
        id = getString(0),
        instructionId = getString(1),
        occurredEdtf = getString(2),
        note = getString(3),
        incidentId = getString(4),
        incidentTitle = getString(5),
        billId = getString(6),
        billDescription = getString(7),
        instructionName = getString(8),
    )

    /**
     * Records that an instruction was not followed.
     *
     * The link to what it broke against is optional and stays that way.
     * Somebody writing this down in a corridor knows it happened; working out
     * which bill or which incident it belongs to is a later, calmer job, and
     * requiring it now would mean the thing never gets written down at all.
     *
     * **The form asks for it from behind the disclosure**, which is how both
     * things stay true at once: one field for the corridor, and the link for
     * the person who already knows. It went unasked for the life of the screen,
     * which is why `Violation.incidentTitle` and `billDescription` were read by
     * a screen that could never receive one. #371.
     */
    suspend fun recordViolation(
        instructionId: String,
        occurred: Edtf.Date,
        note: String?,
        incidentId: String? = null,
        billId: String? = null,
    ): String = insert(
        "instruction_violation",
        mapOf(
            "instruction_id" to instructionId,
            "note" to note?.ifBlank { null },
            "incident_id" to incidentId,
            "bill_id" to billId,
        ) + dateColumns("occurred", occurred),
    )

    /**
     * Corrects a time that was written down.
     *
     * **The record a family reads aloud in a room could not be corrected**, and
     * three of the five panels named that as the thing to fix first. Somebody
     * writing this down in a corridor is interrupted mid sentence and taps
     * Save; without this the half typed word is permanent and it permanently
     * increments a count. Rule 17 for the date, and #371 section 4 for the rest.
     *
     * **The instruction it belongs to is not a parameter**, deliberately. A time
     * written against the wrong request is a different record, not a correction
     * of this one, and moving it would silently change two counts at once.
     */
    suspend fun updateViolation(
        violationId: String,
        occurred: Edtf.Date,
        note: String?,
        incidentId: String? = null,
        billId: String? = null,
    ) = withContext(Dispatchers.IO) {
        val columns = linkedMapOf<String, Any?>(
            "note" to note?.ifBlank { null },
            "incident_id" to incidentId,
            "bill_id" to billId,
        )
        columns.putAll(dateColumns("occurred", occurred))
        val assignments = columns.keys.joinToString(", ") { "$it = ?" }
        db().database.write(
            "UPDATE instruction_violation SET $assignments, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            (columns.values + listOf(System.currentTimeMillis(), violationId)).toTypedArray(),
        )
    }

    /**
     * Takes a time back off the record, as a tombstone. Rule 3.
     *
     * **Not through [delete] and its `Section`**, for the reason
     * [removeIncident] records: this is not a section of the notebook and
     * adding one to that enum to reach a row would put a thirteenth row on a
     * screen the grid draws with twelve.
     */
    suspend fun removeViolation(violationId: String) = withContext(Dispatchers.IO) {
        db().database.write(
            "UPDATE instruction_violation SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ? AND deleted_at IS NULL",
            arrayOf<Any?>(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                violationId,
            ),
        )
    }

    // -- the shape a person gave a project ----------------------------------
    //
    // contract/DATA-CONTRACT.md 8.7 and DESIGN.md 20. **Every project screen
    // answers one of exactly three questions**, 20.1: where it stands, the next
    // date, and the latest word. The first two are read from here. The third is
    // the trail, which this layer already holds.
    //
    // **Nothing here advises, scores, or colors by urgency**, 20.7. A date is a
    // number and where it came from. An elapsed count is a fact drawn from what
    // was recorded, never a judgment about it.

    /** One named stretch of a project's road. */
    data class ProjectStage(
        val id: String,
        val name: String,
        val sortIndex: Int,
        /** When the project reached this stage, or null for one never reached. */
        val enteredEdtf: String?,
        val enteredStart: Long?,
    ) {
        val isReached: Boolean get() = enteredEdtf != null
    }

    /**
     * Whose hands a project is in, since when, and what is happening there.
     *
     * **The elapsed count is not here**, deliberately. It is a rendering of
     * `sinceStart` against today, and today changes while the screen is open. A
     * number stored on a row is a number that goes stale in the database.
     */
    data class ProjectStanding(
        val id: String,
        val holderLabel: String,
        val personId: String?,
        val organizationId: String?,
        val activity: String?,
        val sinceEdtf: String?,
        val sinceStart: Long?,
        val entryId: String?,
        val note: String?,
    )

    /** A date taken off a real paper or out of a real call, with its source. */
    data class ProjectDate(
        val id: String,
        val kind: String,
        val dueEdtf: String?,
        val dueStart: Long?,
        val sourceNote: String?,
        val sourceDocumentId: String?,
        val sourceEntryId: String?,
    )

    /** A paper the project needs. Empty means not yet, never missing. */
    data class ProjectPaper(
        val id: String,
        val name: String,
        val sortIndex: Int,
        val direction: String?,
        val documentId: String?,
    ) {
        val isFilled: Boolean get() = documentId != null
    }

    /**
     * What a project's card on the list needs, for every project at once.
     *
     * `DESIGN.md` 20.5 screen 2: each card carries its mini road and answers
     * where it stands and the next date at a glance.
     *
     * **Three queries rather than three per project.** A person with fifteen
     * projects would otherwise cost forty-five round trips to draw one screen,
     * and this is the screen the Projects tab opens on.
     */
    data class ProjectCard(
        val stages: List<ProjectStage>,
        val holder: String?,
        val nextDate: ProjectDate?,
    )

    suspend fun projectCards(
        subjectId: String,
        now: Long = System.currentTimeMillis(),
    ): Map<String, ProjectCard> = withContext(Dispatchers.IO) {
        val database = db().database

        val stages = mutableMapOf<String, MutableList<ProjectStage>>()
        database.rawQuery(
            "SELECT s.project_id, s.id, s.name, s.sort_index, s.entered_edtf, s.entered_start " +
                "FROM live_project_stage s JOIN live_project p ON p.id = s.project_id " +
                "WHERE p.subject_id = ? ORDER BY s.project_id, s.sort_index, s.created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                stages.getOrPut(cursor.getString(0)) { mutableListOf() }.add(
                    ProjectStage(
                        id = cursor.getString(1),
                        name = cursor.getString(2),
                        sortIndex = cursor.getInt(3),
                        enteredEdtf = cursor.getString(4),
                        enteredStart = if (cursor.isNull(5)) null else cursor.getLong(5),
                    ),
                )
            }
        }

        // The most recent standing per project. Ordered so the first row seen
        // for a project is its current one, and the rest are skipped.
        val holders = mutableMapOf<String, String>()
        database.rawQuery(
            "SELECT t.project_id, t.holder_label FROM live_project_standing t " +
                "JOIN live_project p ON p.id = t.project_id WHERE p.subject_id = ? " +
                "ORDER BY t.project_id, t.since_start DESC, t.created_at DESC",
            arrayOf(subjectId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                holders.putIfAbsent(cursor.getString(0), cursor.getString(1))
            }
        }

        val dates = mutableMapOf<String, MutableList<ProjectDate>>()
        database.rawQuery(
            "SELECT d.project_id, d.id, d.kind, d.due_edtf, d.due_start, d.source_note, " +
                "d.source_document_id, d.source_entry_id FROM live_project_date d " +
                "JOIN live_project p ON p.id = d.project_id WHERE p.subject_id = ? " +
                "ORDER BY d.project_id, d.due_start, d.created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                dates.getOrPut(cursor.getString(0)) { mutableListOf() }.add(
                    ProjectDate(
                        id = cursor.getString(1),
                        kind = cursor.getString(2),
                        dueEdtf = cursor.getString(3),
                        dueStart = if (cursor.isNull(4)) null else cursor.getLong(4),
                        sourceNote = cursor.getString(5),
                        sourceDocumentId = cursor.getString(6),
                        sourceEntryId = cursor.getString(7),
                    ),
                )
            }
        }

        (stages.keys + holders.keys + dates.keys).associateWith { projectId ->
            val theirs = dates[projectId].orEmpty().filter { it.dueStart != null }
            ProjectCard(
                stages = stages[projectId].orEmpty(),
                holder = holders[projectId],
                // The same rule the project's own screen uses, D113, so the
                // card and the screen it opens can never disagree.
                nextDate = theirs.firstOrNull { it.dueStart!! >= now } ?: theirs.lastOrNull(),
            )
        }
    }

    /** The stages of one project, in road order. */
    suspend fun projectStages(projectId: String): List<ProjectStage> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, name, sort_index, entered_edtf, entered_start " +
                    "FROM live_project_stage WHERE project_id = ? " +
                    "ORDER BY sort_index, created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ProjectStage(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                sortIndex = cursor.getInt(2),
                                enteredEdtf = cursor.getString(3),
                                enteredStart = if (cursor.isNull(4)) null else cursor.getLong(4),
                            ),
                        )
                    }
                }
            }
        }

    /** Adds a stage to the end of the road. */
    suspend fun addProjectStage(projectId: String, name: String): String =
        withContext(Dispatchers.IO) {
            val next = db().database.rawQuery(
                "SELECT coalesce(MAX(sort_index), -1) + 1 FROM live_project_stage " +
                    "WHERE project_id = ?",
                arrayOf(projectId),
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            insertRow(
                "project_stage",
                mapOf("project_id" to projectId, "name" to name, "sort_index" to next),
            )
        }

    /**
     * Renames a stage without touching when it was reached.
     *
     * **The arrival is a fact and the name is a label.** Somebody who calls a
     * stage "In review" and later decides it is really "With the reviewer" has
     * not changed when the project got there, and a rename that cleared the
     * date would quietly rewrite the road's own history.
     */
    suspend fun renameProjectStage(stageId: String, name: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE project_stage SET name = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(name, now, stageId),
            )
        }

    /**
     * Reorders a stage against its neighbor, the way a step reorders.
     *
     * **The road is the person's, law 5.** A template that guessed the order of
     * a process wrong is a template, not a verdict, and these processes vary by
     * state and by office.
     */
    suspend fun moveProjectStage(stageId: String, earlier: Boolean) =
        withContext(Dispatchers.IO) {
            val database = db().database
            database.beginTransaction()
            try {
                val here = database.rawQuery(
                    "SELECT project_id, sort_index FROM live_project_stage WHERE id = ?",
                    arrayOf(stageId),
                ).use {
                    if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
                } ?: return@withContext

                val (projectId, index) = here
                val comparison = if (earlier) "<" else ">"
                val direction = if (earlier) "DESC" else "ASC"
                val neighbor = database.rawQuery(
                    "SELECT id, sort_index FROM live_project_stage " +
                        "WHERE project_id = ? AND sort_index $comparison ? " +
                        "ORDER BY sort_index $direction LIMIT 1",
                    arrayOf(projectId, index.toString()),
                ).use {
                    if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
                } ?: return@withContext

                val now = System.currentTimeMillis()
                database.write(
                    "UPDATE project_stage SET sort_index = ?, updated_at = ?, " +
                        "rev = rev + 1 WHERE id = ?",
                    arrayOf<Any?>(neighbor.second, now, stageId),
                )
                database.write(
                    "UPDATE project_stage SET sort_index = ?, updated_at = ?, " +
                        "rev = rev + 1 WHERE id = ?",
                    arrayOf<Any?>(index, now, neighbor.first),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }

    /**
     * Removes a stage, and moves the project off it if that is where it stood.
     *
     * **Both or neither.** A project pointing at a stage that is no longer on
     * its road is a road that cannot be drawn: `RoadStrip` computes where the
     * project is from the stages themselves, and a dangling pointer would show
     * a project as having reached nothing. The project falls back to the last
     * stage before the removed one that had been reached, which is where it
     * actually got to.
     *
     * A tombstone like every other deletion, per rule 3.
     */
    suspend fun removeProjectStage(stageId: String) = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val here = database.rawQuery(
                "SELECT project_id, sort_index FROM live_project_stage WHERE id = ?",
                arrayOf(stageId),
            ).use {
                if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
            } ?: return@withContext
            val (projectId, index) = here

            val now = System.currentTimeMillis()
            database.write(
                "UPDATE project_stage SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(now, now, stageId),
            )

            val standingHere = database.rawQuery(
                "SELECT current_stage_id FROM live_project WHERE id = ?",
                arrayOf(projectId),
            ).use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }

            if (standingHere == stageId) {
                val fallback = database.rawQuery(
                    "SELECT id FROM live_project_stage " +
                        "WHERE project_id = ? AND sort_index < ? AND entered_edtf IS NOT NULL " +
                        "ORDER BY sort_index DESC LIMIT 1",
                    arrayOf(projectId, index.toString()),
                ).use { if (it.moveToFirst()) it.getString(0) else null }

                database.write(
                    "UPDATE project SET current_stage_id = ?, updated_at = ?, rev = rev + 1 " +
                        "WHERE id = ?",
                    arrayOf<Any?>(fallback, now, projectId),
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Moves a project onto a stage, and records when it got there.
     *
     * **Both writes or neither.** The project's pointer and the stage's own
     * arrival date are one fact stated twice, and a half-applied move is a road
     * whose current stage was never reached.
     *
     * **A stage already reached keeps its original date.** Roads turn back:
     * an application returns to In review after more was asked for. Overwriting
     * the first arrival would erase that it had ever been there, and the trail
     * carries the sequence.
     */
    suspend fun moveProjectToStage(projectId: String, stageId: String, reached: Edtf.Date) =
        withContext(Dispatchers.IO) {
            val database = db().database
            database.beginTransaction()
            try {
                val now = System.currentTimeMillis()
                val alreadyReached = database.rawQuery(
                    "SELECT entered_edtf FROM live_project_stage WHERE id = ?",
                    arrayOf(stageId),
                ).use { it.moveToFirst() && !it.isNull(0) }

                if (!alreadyReached) {
                    val dates = dateColumns("entered", reached)
                    database.write(
                        "UPDATE project_stage SET entered_edtf = ?, entered_zone = ?, " +
                            "entered_start = ?, entered_end = ?, updated_at = ?, " +
                            "rev = rev + 1 WHERE id = ?",
                        arrayOf(
                            dates["entered_edtf"], dates["entered_zone"],
                            dates["entered_start"], dates["entered_end"], now, stageId,
                        ),
                    )
                }
                database.write(
                    "UPDATE project SET current_stage_id = ?, updated_at = ?, " +
                        "rev = rev + 1 WHERE id = ?",
                    arrayOf<Any?>(stageId, now, projectId),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }

    /**
     * Where the project stands now, or null when nobody has said yet.
     *
     * The most recent by the date it began, and by when it was written down
     * where two share a date. **Null is a real answer** and the screen says so
     * plainly rather than filling the space.
     */
    suspend fun projectStanding(projectId: String): ProjectStanding? =
        withContext(Dispatchers.IO) {
            projectStandingHistory(projectId).firstOrNull()
        }

    /** Every time the project changed hands, most recent first. */
    suspend fun projectStandingHistory(projectId: String): List<ProjectStanding> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, holder_label, person_id, organization_id, activity, " +
                    "since_edtf, since_start, entry_id, note " +
                    "FROM live_project_standing WHERE project_id = ? " +
                    "ORDER BY since_start DESC, created_at DESC",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ProjectStanding(
                                id = cursor.getString(0),
                                holderLabel = cursor.getString(1),
                                personId = cursor.getString(2),
                                organizationId = cursor.getString(3),
                                activity = cursor.getString(4),
                                sinceEdtf = cursor.getString(5),
                                sinceStart = if (cursor.isNull(6)) null else cursor.getLong(6),
                                entryId = cursor.getString(7),
                                note = cursor.getString(8),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Records that the project changed hands.
     *
     * A new row rather than an edit of the last one, because where it stood
     * three months ago is part of the record. `DESIGN.md` 20.5 screen 8: logging
     * a call offers this automatically when things changed hands, which is why
     * [entryId] is here.
     */
    suspend fun addProjectStanding(
        projectId: String,
        holderLabel: String,
        since: Edtf.Date,
        activity: String? = null,
        personId: String? = null,
        organizationId: String? = null,
        entryId: String? = null,
        note: String? = null,
    ): String = insert(
        "project_standing",
        mapOf(
            "project_id" to projectId,
            "holder_label" to holderLabel,
            "person_id" to personId,
            "organization_id" to organizationId,
            "activity" to activity?.ifBlank { null },
            "entry_id" to entryId,
            "note" to note?.ifBlank { null },
        ) + dateColumns("since", since),
    )

    /**
     * The most recent thing anybody said about this project.
     *
     * `DESIGN.md` 20.1, the third of the three answers: what the office, the
     * insurer or the facility last said, which is the sentence a person repeats
     * at the start of every call.
     *
     * **Read through the `link` table, which is what it is for.** An entry has
     * no `project_id` column and does not need one: 8.1's generic connection
     * table carries exactly this kind of relationship, and adding a column for
     * it would be a schema change nobody has approved.
     *
     * **Both directions are accepted.** A link written from the entry and a
     * link written from the project mean the same thing to a person, and a
     * screen that showed the latest word only when the row happened to be
     * written one way round would be a dead end wearing a disguise, rule 18.
     *
     * Null is a real answer and stays null: **nothing here invents a latest
     * word from the trail at large.** An entry that was never connected to this
     * project is not something anybody said about this project.
     */
    suspend fun latestWordFor(projectId: String): TrailEntry? = withContext(Dispatchers.IO) {
        val entryId = db().database.rawQuery(
            "SELECT CASE WHEN source_table = 'entry' THEN source_id ELSE target_id END " +
                "FROM live_link " +
                "WHERE (source_table = 'entry' AND target_table = 'project' AND target_id = ?) " +
                "   OR (source_table = 'project' AND target_table = 'entry' AND source_id = ?) " +
                "ORDER BY created_at DESC",
            arrayOf(projectId, projectId),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        if (entryId.isEmpty()) return@withContext null

        // Ordered by when it happened rather than by when the link was written,
        // because somebody catching up records three calls in one evening and
        // the latest word is the last one that happened, not the last one typed.
        val marks = entryId.joinToString(", ") { "?" }
        db().database.rawQuery(
            "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, " +
                "is_unfiled, pinned_at FROM live_entry WHERE id IN ($marks) " +
                "ORDER BY occurred_start DESC, created_at DESC LIMIT 1",
            entryId.toTypedArray(),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
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
                pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
            )
        }
    }

    /**
     * Everything written down about this project, most recent first.
     *
     * **Rule 18 in the other direction.** The entry knows the project because
     * something wrote the link; this is the project showing the entry back, and
     * without it a person who logged six calls can see one of them.
     *
     * Ordered by when it happened rather than when it was written, for the same
     * reason [latestWordFor] is: somebody catching up records three calls in one
     * evening and the order that matters is the order they happened in.
     */
    suspend fun entriesAbout(projectId: String): List<TrailEntry> =
        withContext(Dispatchers.IO) {
            val ids = db().database.rawQuery(
                "SELECT CASE WHEN source_table = 'entry' THEN source_id ELSE target_id END " +
                    "FROM live_link " +
                    "WHERE (source_table = 'entry' AND target_table = 'project' AND target_id = ?) " +
                    "   OR (source_table = 'project' AND target_table = 'entry' AND source_id = ?) " +
                    "ORDER BY created_at DESC",
                arrayOf(projectId, projectId),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            if (ids.isEmpty()) return@withContext emptyList()

            val marks = ids.joinToString(", ") { "?" }
            db().database.rawQuery(
                "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, " +
                    "is_unfiled, pinned_at FROM live_entry WHERE id IN ($marks) " +
                    "ORDER BY occurred_start DESC, created_at DESC",
                ids.toTypedArray(),
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
                                pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Writes a note, and attaches it to the thing it is about. D207, #397.
     *
     * **A note is an entry**, `kind = 'note'`, which the schema has allowed
     * since Phase 0 and which nothing could write until now. That keeps it on
     * the trail, in search, in the archive, in the merge and in the change log
     * with no new plumbing, and it is why #397 changed no table.
     *
     * **The body is stored exactly as typed**, marks and all, per
     * `contract/DATA-CONTRACT.md` 8.8.1. There is no encode step here and no
     * decode step on the way out, which is what makes "survives the archive
     * byte for byte" true by construction.
     *
     * **The attachment is one `link` row and it reads from both sides**, rule
     * 18. A note about Tuesday's visit is
     * `('entry', <note>, 'appointment', <appointment>)`, and both the note and
     * the appointment find it with the same query.
     *
     * **Both writes are one transaction**, so a note never exists attached to
     * nothing because the second insert failed.
     */
    suspend fun addNote(
        subjectId: String,
        title: String?,
        body: String?,
        /**
         * When it happened, at exactly the precision the person gave, rule 17.
         *
         * **Today is the honest default, not unknown.** Looked at on the phone:
         * a note written during a visit went in with no date, and an undated
         * entry sorts last, so it landed at the foot of a 670 entry trail the
         * moment it was written. The person is writing it because it is
         * happening now, which is the same reasoning the questions screen uses
         * for marking one asked. **It stays editable forever from the entry
         * itself**, rule 17, because a note is an entry.
         */
        occurred: Edtf.Date = Edtf.day(LocalDate.now()),
        /** What this note is about, as a table and a row id, or null for a general note. */
        aboutTable: String? = null,
        aboutId: String? = null,
        chapterId: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val id = insertRow(
                "entry",
                mapOf(
                    "subject_id" to subjectId,
                    "kind" to "note",
                    "title" to title?.ifBlank { null },
                    // **Exactly what was typed.** Rule 3 and 8.8.1: the column
                    // is the record, and nothing here rewrites it.
                    "body" to body?.ifBlank { null },
                    "chapter_id" to chapterId,
                ) + dateColumns("occurred", occurred),
            )
            if (aboutTable != null && aboutId != null) {
                insertRow(
                    "link",
                    mapOf(
                        "source_table" to "entry",
                        "source_id" to id,
                        "target_table" to aboutTable,
                        "target_id" to aboutId,
                        "relation" to "about",
                    ),
                )
            }
            database.setTransactionSuccessful()
            id
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Every note about one thing, newest first. Rule 18, the other direction.
     *
     * **Read from both sides of `link`**, exactly as `entriesAbout` does, so a
     * row written from either end is found. A link is a fact about two things
     * and not a property of one of them.
     */
    suspend fun notesAbout(table: String, rowId: String): List<TrailEntry> =
        withContext(Dispatchers.IO) {
            val ids = db().database.rawQuery(
                "SELECT CASE WHEN source_table = 'entry' THEN source_id ELSE target_id END " +
                    "FROM live_link " +
                    "WHERE (source_table = 'entry' AND target_table = ? AND target_id = ?) " +
                    "   OR (source_table = ? AND source_id = ? AND target_table = 'entry') " +
                    "ORDER BY created_at DESC",
                arrayOf(table, rowId, table, rowId),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            if (ids.isEmpty()) return@withContext emptyList()
            val marks = ids.joinToString(", ") { "?" }
            db().database.rawQuery(
                "SELECT id, kind, title, body, occurred_edtf, occurred_start, created_at, " +
                    "is_unfiled, pinned_at FROM live_entry " +
                    "WHERE id IN ($marks) AND kind = 'note' " +
                    "ORDER BY occurred_start DESC, created_at DESC",
                ids.toTypedArray(),
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
                                pinnedAt = if (cursor.isNull(8)) null else cursor.getLong(8),
                            ),
                        )
                    }
                }
            }
        }

    /** What one entry is attached to, as the person would name it. #397. */
    data class About(val table: String, val id: String, val label: String)

    /**
     * What a memo is about, read from `link` and named in the person's words.
     *
     * **Rule 18's other direction.** `notesAbout` answers "what memos are on
     * this thing"; this answers "what thing is this memo on", and both read the
     * same row from either end, because a link is a fact about two things.
     *
     * **The heading column is looked up per table rather than guessed**, which
     * is the same problem the conflict screen already solved: a person is a
     * `display_name`, an appointment a `title`, a project a `name`. Rule 20
     * says the interface never shows somebody a table name or a row id.
     */
    suspend fun aboutFor(entryId: String): About? = withContext(Dispatchers.IO) {
        val database = db().database
        // **Ordered, because `LIMIT 1` without one picks whichever row the
        // storage engine happens to hand back first**, and that changes after a
        // vacuum, an index or a version upgrade. 8.4, and the check that holds
        // it. A memo has one target today; the order is what keeps the answer
        // the same on two devices if it ever has two.
        val target = database.rawQuery(
            "SELECT target_table, target_id, created_at FROM live_link " +
                "WHERE source_table = 'entry' AND source_id = ? " +
                "UNION ALL " +
                "SELECT source_table, source_id, created_at FROM live_link " +
                "WHERE target_table = 'entry' AND target_id = ? " +
                "ORDER BY created_at, 1, 2 LIMIT 1",
            arrayOf(entryId, entryId),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) to cursor.getString(1) else null
        } ?: return@withContext null

        val (table, id) = target
        val column = ABOUT_HEADINGS[table] ?: return@withContext null
        val label = database.rawQuery(
            "SELECT \"$column\" FROM \"live_$table\" WHERE id = ?",
            arrayOf(id),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: return@withContext null
        About(table = table, id = id, label = label)
    }

    /** Connects an entry to a project, so each can show the other. Rule 18. */
    suspend fun linkEntryToProject(entryId: String, projectId: String): String = insert(
        "link",
        mapOf(
            "source_table" to "entry",
            "source_id" to entryId,
            "target_table" to "project",
            "target_id" to projectId,
            "relation" to "about",
        ),
    )

    /**
     * Writes a raw link with the sides given, for a test and nothing else.
     *
     * **It exists to produce a state the app cannot**, which is the only way to
     * prove the readers accept a link written from either side. Every writer in
     * this app puts the entry on the source side; an imported notebook is not
     * obliged to, and 8.1 does not make one direction canonical. Without this
     * the both-ways readers in [entriesAbout], [latestWordFor] and
     * [projectsOnEntryBlocking] are half untested and would keep passing.
     *
     * **Named so nobody reaches for it by accident**, like `columnForTest`,
     * `clearEveryLeadForTest` and `tombstoneForTest`.
     */
    suspend fun insertLinkForTest(
        sourceTable: String,
        sourceId: String,
        targetTable: String,
        targetId: String,
        relation: String = "about",
    ): String = insert(
        "link",
        mapOf(
            "source_table" to sourceTable,
            "source_id" to sourceId,
            "target_table" to targetTable,
            "target_id" to targetId,
            "relation" to relation,
        ),
    )

    /**
     * What kind of thing one row of a project's trail is. `DESIGN.md` 20.5
     * screen 11.
     *
     * **Three sources, one line.** A long process is not only the calls: the
     * road turning and a deadline arriving are as much a part of what happened
     * as anything somebody wrote down, and the grid draws them on the same
     * spine for exactly that reason.
     */
    enum class ProjectTrailKind { ENTRY, STAGE, DATE }

    /**
     * One thing that happened to a project, whatever kind of thing it was.
     *
     * **The screen does no arithmetic and no lookups on this.** Everything it
     * needs to draw a row is here, because the alternative is a screen holding
     * three lists and working out which one each row came from, which is the
     * complexity rule 20 says belongs in the code.
     */
    data class ProjectTrailItem(
        val kind: ProjectTrailKind,
        val id: String,
        val whenEdtf: String?,
        val whenStart: Long?,
        /** What the row says, already the person's own words where there are any. */
        val title: String,
        /** The second line, or null where the row is one line. */
        val note: String?,
        /** The entry itself, for the rows that are one and null for the rest. */
        val entry: TrailEntry? = null,
    )

    /**
     * One project's own trail: what was said, where the road turned, and the
     * dates it is running against. `DESIGN.md` 20.5 screen 11.
     *
     * **Oldest first, unlike the main trail**, which is newest first. A process
     * is read forward: somebody wants to see how it got here and what is coming,
     * and the grid draws it that way. The main trail answers "what happened
     * lately", which is a different question.
     *
     * **Dates that have not arrived are included and are not marked as late or
     * missed.** A response window closing is a fact about the calendar; the app
     * records and counts and never concludes, per rule 2 and 20.7.
     *
     * **A stage nobody has reached contributes nothing.** It has no date, so it
     * has no place on a line ordered by date, and putting it at the end would
     * assert it happened last rather than not at all.
     */
    suspend fun projectTrail(projectId: String): List<ProjectTrailItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<ProjectTrailItem>()

            entriesAbout(projectId).forEach { entry ->
                items += ProjectTrailItem(
                    kind = ProjectTrailKind.ENTRY,
                    id = entry.id,
                    whenEdtf = entry.occurredEdtf,
                    whenStart = entry.occurredStart,
                    // The row renders the entry itself, so the screen keeps the
                    // one heading rule that every other trail row already uses.
                    title = entry.title.orEmpty(),
                    note = entry.body,
                    entry = entry,
                )
            }

            projectStages(projectId).filter { it.isReached }.forEach { stage ->
                items += ProjectTrailItem(
                    kind = ProjectTrailKind.STAGE,
                    id = stage.id,
                    whenEdtf = stage.enteredEdtf,
                    whenStart = stage.enteredStart,
                    title = stage.name,
                    note = null,
                )
            }

            projectDates(projectId).forEach { date ->
                items += ProjectTrailItem(
                    kind = ProjectTrailKind.DATE,
                    id = date.id,
                    whenEdtf = date.dueEdtf,
                    whenStart = date.dueStart,
                    title = date.kind,
                    note = date.sourceNote,
                )
            }

            // **Undated rows sort last rather than being dropped.** "Not sure
            // when" is a first class value per rule 17, and an entry somebody
            // wrote without a date is still something that happened to this
            // project. It goes at the end because that is the only place that
            // does not assert a position it does not have.
            items.sortedWith(
                compareBy(nullsLast()) { it.whenStart },
            )
        }

    /** Every date this project holds, soonest first. */
    suspend fun projectDates(projectId: String): List<ProjectDate> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, kind, due_edtf, due_start, source_note, " +
                    "source_document_id, source_entry_id " +
                    "FROM live_project_date WHERE project_id = ? " +
                    "ORDER BY due_start, created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ProjectDate(
                                id = cursor.getString(0),
                                kind = cursor.getString(1),
                                dueEdtf = cursor.getString(2),
                                dueStart = if (cursor.isNull(3)) null else cursor.getLong(3),
                                sourceNote = cursor.getString(4),
                                sourceDocumentId = cursor.getString(5),
                                sourceEntryId = cursor.getString(6),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * The date a project screen leads with, or null when it has none.
     *
     * **The soonest that has not passed, and the most recent when they all
     * have.** D113: no column marks one date as the important one, because a
     * person recording a hearing date should not also have to tell the app that
     * a hearing matters, and a marked date stays marked after it passes.
     *
     * [now] is a parameter rather than read here, so a test can state what today
     * is instead of arranging for it.
     */
    suspend fun leadingProjectDate(
        projectId: String,
        now: Long = System.currentTimeMillis(),
    ): ProjectDate? {
        val dates = projectDates(projectId).filter { it.dueStart != null }
        return dates.firstOrNull { it.dueStart!! >= now } ?: dates.lastOrNull()
    }

    /** Records a date, with where it was taken from. */
    suspend fun addProjectDate(
        projectId: String,
        kind: String,
        due: Edtf.Date,
        sourceNote: String? = null,
        sourceDocumentId: String? = null,
        sourceEntryId: String? = null,
    ): String = insert(
        "project_date",
        mapOf(
            "project_id" to projectId,
            "kind" to kind,
            "source_note" to sourceNote?.ifBlank { null },
            "source_document_id" to sourceDocumentId,
            "source_entry_id" to sourceEntryId,
        ) + dateColumns("due", due),
    )

    /** The kinds of date this project offers as chips. Never a closed set. */
    suspend fun projectDateKinds(projectId: String): List<String> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT label FROM live_project_date_kind WHERE project_id = ? " +
                    "ORDER BY sort_index, created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        }

    /**
     * One of a project's date kinds, with the id needed to change it.
     *
     * **`projectDateKinds` returns labels alone** because that is all the chips
     * on the date sheet need, and it stays that way: a caller that only offers
     * them should not have to know they have identities.
     */
    data class ProjectDateKind(val id: String, val label: String)

    /** The same list, for the screen that edits it. */
    suspend fun projectDateKindRows(projectId: String): List<ProjectDateKind> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, label FROM live_project_date_kind WHERE project_id = ? " +
                    "ORDER BY sort_index, created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(ProjectDateKind(cursor.getString(0), cursor.getString(1)))
                    }
                }
            }
        }

    /**
     * Renames a date kind.
     *
     * **Dates already recorded under the old name keep it.** `project_date.kind`
     * is the words the person used at the time, copied at the moment they wrote
     * the date down, and rewriting history to match a label they changed later
     * would be the app editing what somebody recorded. The kind list is what is
     * offered next time, not a key into what was.
     */
    suspend fun renameProjectDateKind(kindId: String, label: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE project_date_kind SET label = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(label, now, kindId),
            )
        }

    /**
     * Removes a date kind, and leaves every date recorded under it alone.
     *
     * A tombstone like every other deletion, per rule 3. Taking a kind off the
     * list stops it being offered; it does not reach back into the record.
     */
    suspend fun removeProjectDateKind(kindId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE project_date_kind SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any?>(now, now, kindId),
        )
    }

    suspend fun addProjectDateKind(projectId: String, label: String): String =
        withContext(Dispatchers.IO) {
            val next = db().database.rawQuery(
                "SELECT coalesce(MAX(sort_index), -1) + 1 FROM live_project_date_kind " +
                    "WHERE project_id = ?",
                arrayOf(projectId),
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            insertRow(
                "project_date_kind",
                mapOf("project_id" to projectId, "label" to label, "sort_index" to next),
            )
        }

    /** The papers this project needs, filled and not yet, in order. */
    /**
     * One named place for paper, with whatever is actually in it. Screen 13.
     *
     * **The place and the paper are different things and this carries both.**
     * [ProjectPaper] is the place; a person looking at the papers of a project
     * wants the photograph, its title and when it arrived, and getting those
     * one row at a time would be a query per placeholder on a screen drawing
     * twelve of them.
     */
    data class ProjectPaperCard(
        val paper: ProjectPaper,
        /** The document filed here, or null while the place is still waiting. */
        val documentId: String?,
        val title: String?,
        /** The photograph's content hash, null where the document has none. */
        val sha256: String?,
        val receivedEdtf: String?,
        /** Where the original physically is, as the person wrote it. */
        val originalLocation: String?,
    ) {
        val isFilled: Boolean get() = documentId != null
    }

    /**
     * The project's papers with what is filed in each, in one query.
     *
     * **A left join, so an empty place is still a row.** 20.4: an unfilled
     * placeholder is "not yet" and never missing, and dropping it here would
     * make the screen unable to say so.
     */
    /**
     * Which project has this document filed as one of its papers, if any.
     *
     * **The other half of screen 13's link**, rule 18. A project's papers open
     * the document; without this the document was a dead end that said nothing
     * about the process it belongs to, which is the thing somebody opening it
     * from Documents six months later most wants to know.
     *
     * **A list, because one photograph can be filed in two places.** Nothing in
     * the app does that yet and the schema does not stop it.
     */
    data class DocumentFiling(
        val projectId: String,
        val projectName: String,
        /** The name of the place it is filed in, as the person named it. */
        val paperName: String,
    )

    /** Where this document is filed among the projects' papers. Rule 18. */
    suspend fun filingsForDocument(documentId: String): List<DocumentFiling> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT pr.id, pr.name, p.name FROM live_project_paper p " +
                    "JOIN live_project pr ON pr.id = p.project_id " +
                    "WHERE p.document_id = ? ORDER BY p.created_at",
                arrayOf(documentId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DocumentFiling(
                                projectId = cursor.getString(0),
                                projectName = cursor.getString(1),
                                paperName = cursor.getString(2),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Somebody this project has actually involved. `DESIGN.md` 20.5 screen 14.
     *
     * **The project's own contacts, and not the care team.** They overlap and
     * they are not the same list: the care team is everybody looking after the
     * person, and this is whoever has turned up in this process. A screen
     * showing the whole care team under a Medicaid application would bury the
     * two caseworkers who matter in a list of nurses.
     *
     * **Derived rather than stored.** There is no project-to-person table and
     * this does not add one: somebody is on a project because they are named on
     * an entry that is linked to it, which is a fact the record already holds.
     * Nothing here writes anything.
     */
    data class ProjectPerson(
        val person: Person,
        /** How many of this project's entries name them. Never a score. */
        val mentions: Int,
        /** When they last turned up on it, at whatever precision was given. */
        val lastEdtf: String?,
        val lastStart: Long?,
        /**
         * The other projects that also name them, for the cross-project door.
         *
         * **The one new navigation idea on this surface**, per screen 14: a
         * caseworker handling two of somebody's processes is the case where a
         * person is holding two threads and the app can say so.
         */
        val alsoIn: List<EntryProject>,
    )

    /**
     * The people this project has involved, most recently seen first.
     *
     * **One query for the people and one for where else they turn up**, rather
     * than one per person: a project with nine contacts would otherwise cost
     * ten round trips to draw one screen.
     */
    suspend fun projectPeople(projectId: String): List<ProjectPerson> =
        withContext(Dispatchers.IO) {
            val database = db().database
            val rows = database.rawQuery(
                "SELECT pe.id, pe.display_name, pe.role_label, pe.phone, pe.email, " +
                    "pe.notes, COUNT(*), MAX(e.occurred_edtf), MAX(e.occurred_start) " +
                    "FROM live_link l " +
                    "JOIN live_entry e ON e.id = " +
                    "  CASE WHEN l.source_table = 'entry' THEN l.source_id ELSE l.target_id END " +
                    "JOIN live_entry_person ep ON ep.entry_id = e.id " +
                    "JOIN live_person pe ON pe.id = ep.person_id " +
                    "WHERE (l.source_table = 'entry' AND l.target_table = 'project' " +
                    "       AND l.target_id = ?) " +
                    "   OR (l.source_table = 'project' AND l.target_table = 'entry' " +
                    "       AND l.source_id = ?) " +
                    "GROUP BY pe.id " +
                    "ORDER BY MAX(e.occurred_start) DESC, pe.display_name",
                arrayOf(projectId, projectId),
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
                            ) to Triple(
                                cursor.getInt(6),
                                cursor.getString(7),
                                if (cursor.isNull(8)) null else cursor.getLong(8),
                            ),
                        )
                    }
                }
            }
            if (rows.isEmpty()) return@withContext emptyList()

            // Where else each of them turns up, in one pass over the same join.
            val elsewhere = mutableMapOf<String, MutableList<EntryProject>>()
            val marks = rows.joinToString(", ") { "?" }
            database.rawQuery(
                "SELECT DISTINCT ep.person_id, pr.id, pr.name, pr.status " +
                    "FROM live_link l " +
                    "JOIN live_project pr ON pr.id = " +
                    "  CASE WHEN l.source_table = 'project' THEN l.source_id ELSE l.target_id END " +
                    "JOIN live_entry e ON e.id = " +
                    "  CASE WHEN l.source_table = 'entry' THEN l.source_id ELSE l.target_id END " +
                    "JOIN live_entry_person ep ON ep.entry_id = e.id " +
                    "WHERE pr.id <> ? AND ep.person_id IN ($marks) " +
                    "ORDER BY pr.name",
                (listOf(projectId) + rows.map { it.first.id }).toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    elsewhere.getOrPut(cursor.getString(0)) { mutableListOf() }
                        .add(
                            EntryProject(
                                id = cursor.getString(1),
                                name = cursor.getString(2),
                                status = cursor.getString(3),
                            ),
                        )
                }
            }

            rows.map { (person, seen) ->
                ProjectPerson(
                    person = person,
                    mentions = seen.first,
                    lastEdtf = seen.second,
                    lastStart = seen.third,
                    alsoIn = elsewhere[person.id].orEmpty(),
                )
            }
        }

    suspend fun projectPaperCards(projectId: String): List<ProjectPaperCard> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT p.id, p.name, p.sort_index, p.direction, p.document_id, " +
                    "d.title, a.sha256, d.received_edtf, d.original_location " +
                    "FROM live_project_paper p " +
                    "LEFT JOIN live_document d ON d.id = p.document_id " +
                    "LEFT JOIN live_attachment a ON a.document_id = d.id " +
                    "WHERE p.project_id = ? " +
                    "ORDER BY p.sort_index, p.created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ProjectPaperCard(
                                paper = ProjectPaper(
                                    id = cursor.getString(0),
                                    name = cursor.getString(1),
                                    sortIndex = cursor.getInt(2),
                                    direction = cursor.getString(3),
                                    // **The document as the place records it**,
                                    // not as the join found it: a place pointing
                                    // at a tombstoned document is still filled
                                    // as far as the person arranged it, and the
                                    // screen says the paper is gone rather than
                                    // silently emptying the place.
                                    documentId = cursor.getString(4),
                                ),
                                documentId = if (cursor.isNull(5)) null else cursor.getString(4),
                                title = cursor.getString(5),
                                sha256 = cursor.getString(6),
                                receivedEdtf = cursor.getString(7),
                                originalLocation = cursor.getString(8),
                            ),
                        )
                    }
                }
            }
        }

    suspend fun projectPapers(projectId: String): List<ProjectPaper> =
        withContext(Dispatchers.IO) {
            db().database.rawQuery(
                "SELECT id, name, sort_index, direction, document_id " +
                    "FROM live_project_paper WHERE project_id = ? " +
                    "ORDER BY sort_index, created_at",
                arrayOf(projectId),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ProjectPaper(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                sortIndex = cursor.getInt(2),
                                direction = cursor.getString(3),
                                documentId = cursor.getString(4),
                            ),
                        )
                    }
                }
            }
        }

    suspend fun addProjectPaper(
        projectId: String,
        name: String,
        direction: String? = null,
        documentId: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val next = db().database.rawQuery(
            "SELECT coalesce(MAX(sort_index), -1) + 1 FROM live_project_paper " +
                "WHERE project_id = ?",
            arrayOf(projectId),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        insertRow(
            "project_paper",
            mapOf(
                "project_id" to projectId,
                "name" to name,
                "sort_index" to next,
                "direction" to direction,
                "document_id" to documentId,
            ),
        )
    }

    /** Fills a paper placeholder with the document that arrived for it. */
    /**
     * Renames a paper placeholder, and leaves whatever is filed in it alone.
     *
     * **The placeholder is a named place, not the paper.** Somebody who decides
     * "The application copy" is really "The renewal packet" has not changed
     * which document is filed there, and clearing it on a rename would throw
     * away the one thing the placeholder was for.
     */
    suspend fun renameProjectPaper(paperId: String, name: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE project_paper SET name = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE id = ?",
                arrayOf<Any?>(name, now, paperId),
            )
        }

    /**
     * Takes a document back out of its placeholder, leaving the place named.
     *
     * **The document is not touched.** It stays in the notebook where it was
     * filed; this only says it is not the thing this placeholder was waiting
     * for. Somebody who photographed the wrong letter needs this and should not
     * have to delete the photograph to get it.
     */
    suspend fun emptyProjectPaper(paperId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE project_paper SET document_id = NULL, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any?>(now, paperId),
        )
    }

    /**
     * Removes a paper placeholder.
     *
     * A tombstone, per rule 3. **Whatever was filed in it stays in the
     * notebook**: a placeholder is a named place, and taking the place away is
     * not a reason to lose the person's own paper.
     */
    suspend fun removeProjectPaper(paperId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE project_paper SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE id = ?",
            arrayOf<Any?>(now, now, paperId),
        )
    }

    suspend fun fillProjectPaper(paperId: String, documentId: String, direction: String?) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE project_paper SET document_id = ?, direction = coalesce(?, direction), " +
                    "updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(documentId, direction, now, paperId),
            )
        }

    /**
     * Which of the three answers this project opens with.
     *
     * **One control, changed with no penalty**, 20.3: the shape is a default and
     * never a cage.
     */
    suspend fun setProjectLead(projectId: String, lead: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE project SET lead = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(lead, now, projectId),
        )
    }

    /** Writes who said they would handle a step, and which area it belongs to. */
    suspend fun setProjectStepHandling(stepId: String, cluster: String?, handlerLabel: String?) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db().database.write(
                "UPDATE project_step SET cluster = ?, handler_label = ?, updated_at = ?, " +
                    "rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(
                    cluster?.ifBlank { null },
                    handlerLabel?.ifBlank { null },
                    now,
                    stepId,
                ),
            )
        }

    /**
     * The answer one card shows, already counted but not yet worded.
     *
     * **The words are the screen's**, from the catalog, in the person's own
     * language. What comes out of here is what the record says: a number, a
     * name, a date. `DESIGN.md` 21.2: a card is a named deterministic question
     * asked of the record, and nothing here interprets the answer.
     */
    data class TodayAnswer(
        /** The count, where the question is "how many". Null when it is not. */
        val count: Int? = null,
        /** The thing itself, where the question is "which one". */
        val title: String? = null,
        /** When it is, or where it came from. Already an EDTF string. */
        val whenEdtf: String? = null,
        /** A second line the card shows at wide and tall. */
        val detail: String? = null,
        /**
         * Whether what this card points at is closed or gone.
         *
         * **The source-closed rung**, `DESIGN.md` 21.4. A card pointing at a
         * finished project says so and keeps working as a door, and it is
         * removed only by the person's hand. **Never by the app**, per 8.7:
         * dropping it would be the file quietly editing somebody's desk.
         *
         * A boolean rather than a sentence, because the sentence belongs in the
         * catalog in the person's own language.
         */
        val sourceClosed: Boolean = false,
        /**
         * The same answer in more detail, for the wide and tall forms.
         *
         * **More of the same answer and never a new kind of content**, per 21.3,
         * which uses this card as its example: the medications card at small is
         * a count and at wide it is the list. Both are the one question "what is
         * on the list right now", asked once.
         *
         * **Short by construction.** The query takes a handful, and [count] is
         * still the true total, so the card can say how many it is not showing
         * rather than quietly cropping. A card is never a dense feed, 21.3.
         */
        val items: List<TodayItem> = emptyList(),
        /**
         * What this card points at, where it points at one thing.
         *
         * **It qualifies the tab, not the answer.** A card for one measure is
         * "Progress · Weight" and a card for one project is "Waiver · the next
         * date", which is what the grid draws and what lets four project cards
         * on one screen be told apart. The tab is identity, so the name belongs
         * there rather than taking the place of the answer.
         *
         * **This is why the measure card was upside down.** It put the measure's
         * name in [title] and the reading in [detail], so the card asking "what
         * is the latest value" showed the word Weight at display size and the
         * number in the quiet line under it.
         */
        val sourceName: String? = null,
        /**
         * The recent readings, for a measure card drawn at tall.
         *
         * **The same rows the Progress screen plots**, so the two charts of one
         * measure cannot disagree about the same silence. `ChartCard.chartPoints`
         * turns them into points and owns the gap rule.
         *
         * **Newest first, as the query returns them**, and a handful of them:
         * 21.3 says tall carries a chart and never a dense feed.
         */
        val series: List<Reading> = emptyList(),
        /**
         * A second, counted line, as a catalog key and its number.
         *
         * **A key rather than a sentence**, because the sentence is the
         * screen's and its plural is the reader's language's. The key is a
         * literal here, so `check_string_keys.py` can still see it.
         *
         * For the standing instructions card this is how many times something
         * was not followed, which is the other half of the question 21.7 asks.
         * **A count and never a judgment**: nothing says a facility is bad and
         * no threshold turns the number into an opinion.
         */
        val detailKey: String? = null,
        val detailCount: Int? = null,
        /**
         * Whether [items] is a sample of the thing [count] counted.
         *
         * **True for almost every card**, where the count is how many there are
         * and the items are the first few of them, so the card can honestly say
         * "and 8 more". **False for the steps card**, whose count is steps and
         * whose items are clusters: subtracting one from the other gave a
         * project with six steps in one cluster the line "and 5 more", which is
         * a sentence about nothing. Seen on the phone.
         */
        val itemsSampleTheCount: Boolean = true,
        /**
         * A number the card may offer to dial, for the care team card.
         *
         * **Only the card pointing at one person has one**, 21.7: the question
         * is "how do I reach them, now", and the answer at wide is that
         * person's number as an outlined pill. 21.3 allows an inline action at
         * wide and tall, outlined, a verb or a dialable number, and this is
         * the number half of that sentence.
         *
         * **Stored exactly as the person typed it and never reformatted.** It
         * goes to `ACTION_DIAL` as it is, and a number the app tidied is a
         * number that may no longer connect.
         *
         * **Null is an ordinary state**, not a gap: plenty of people in a care
         * team are somebody you only ever see in person, and the card says so
         * rather than showing an empty pill.
         */
        val phone: String? = null,
    ) {
        /** Whether the record has anything to say. The none-yet rung, 21.4. */
        val isEmpty: Boolean get() = (count ?: 0) == 0 && title.isNullOrBlank()
    }

    /**
     * One line of a card's list.
     *
     * **Two fields rather than one joined string**, because joining is wording
     * and wording is the screen's. A separator chosen here would also be a
     * separator with no bidi isolation around either half, which is the exact
     * family of defect `RoadStrip`'s fallback line carried: text the person
     * typed, concatenated by hand, running the wrong way in Arabic.
     */
    data class TodayItem(
        /** The thing itself. A medication's name, a person's name. */
        val label: String,
        /** The one thing about it worth seeing beside it, where there is one. */
        val note: String? = null,
        /**
         * When it was, where the line carries a date.
         *
         * **Separate from [note] because a stored date is not display text.**
         * `DESIGN.md` 9.2 and rule 17: the person never sees EDTF, ever, and
         * putting `2026-04` in [note] would put exactly that on the front
         * screen of the app. It renders through `EventDateText` like every
         * other date, so a month stays a month.
         */
        val noteEdtf: String? = null,
        /**
         * An amount, where the line is money, in minor units.
         *
         * **Not a formatted string, for the same reason [note] is not a joined
         * one.** Money's shape belongs to the reader's locale and its currency
         * belongs to the bill, and `formatMoney` already knows that an Arabic
         * reader in the United States is still looking at dollars. A number
         * formatted here would be formatted in whatever locale the query
         * happened to run in.
         */
        val amountMinor: Long? = null,
        /** The amount's own currency, never the locale's. */
        val currency: String? = null,
        /**
         * The content hash of the picture of this thing, where there is one.
         *
         * **Only the documents card uses it**, and only at wide. 21.7: that
         * card never renders private content larger than a thumbnail, so what
         * travels is a hash and the screen decides how big the picture is
         * allowed to be. Null for a document nobody photographed, which is a
         * real and ordinary state.
         */
        val imageSha: String? = null,
        /**
         * What kind of thing this was, for a row drawn on a spine.
         *
         * **The node's color, which `DESIGN.md` 5.2 makes a vocabulary rather
         * than a decoration**: a call is gold, a visit is blue, an incident is
         * alert, and everything else takes the quiet ink rather than borrowing
         * one of the three. The stored value travels and the screen decides the
         * color, because a color is a rendering and this is a record.
         */
        val kind: String? = null,
        /**
         * When it happened, in milliseconds, for measuring the gap to the row
         * above it.
         *
         * **Separate from [noteEdtf] because they answer different questions.**
         * The EDTF says what precision the person gave and is what gets
         * rendered; this is the resolved instant the distance is arithmetic on.
         * A coarse date has one and must not produce a marker from it, which is
         * the caller's decision because only the caller knows the precision.
         */
        val noteStart: Long? = null,
    )


    /**
     * Every card's answer, in one pass. `DESIGN.md` 21.2.
     *
     * **Queries run when Today gains focus and after any save**, which is what
     * makes the surface a pull model rather than something that watches the
     * person. This is that pass.
     *
     * **A type with no answer here is a card that would render blank**, so it
     * is better to have none: every one of the seventeen in 21.7 is answered,
     * and a card whose source is gone answers its source-closed rung rather
     * than disappearing.
     */
    suspend fun todayAnswers(subjectId: String): Map<String, TodayAnswer> =
        withContext(Dispatchers.IO) {
            val database = db().database

            // **One card's failure is one card's failure.** These queries ran
            // in a single block under the shell's own catch, so one column name
            // that did not exist made every card on Today say "Nothing waiting"
            // at once, which is the app asserting something false about
            // somebody's record rather than failing visibly. A card with no
            // answer is absent from this map and says so; it does not claim the
            // record is empty.
            val answers = mutableMapOf<String, TodayAnswer>()
            fun put(key: String, compute: () -> TodayAnswer) {
                runCatching(compute).getOrNull()?.let { answers[key] = it }
            }

            fun one(sql: String, vararg args: String): Pair<String?, String?>? =
                database.rawQuery(sql, arrayOf(*args)).use {
                    if (it.moveToFirst()) it.getString(0) to it.getString(1) else null
                }

            fun countOf(sql: String, vararg args: String): Int =
                database.rawQuery(sql, arrayOf(*args)).use {
                    if (it.moveToFirst()) it.getInt(0) else 0
                }

            // A short list for a card's wide form. Two columns, the second
            // optional, and the caller's query does the limiting so the count
            // beside it stays the true total.
            fun manyOf(
                sql: String,
                vararg args: String,
                /**
                 * Whether the second column is a stored date rather than text.
                 *
                 * **It has to be said rather than guessed**, because an EDTF
                 * string put in `note` renders as `2026-04` on the front screen
                 * of the app, which rule 17 and 9.2 forbid outright. It nearly
                 * shipped that way for the trail card.
                 */
                dates: Boolean = false,
            ): List<TodayItem> =
                database.rawQuery(sql, arrayOf(*args)).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            val second = cursor.getString(1)?.takeIf { it.isNotBlank() }
                            add(
                                TodayItem(
                                    label = cursor.getString(0),
                                    note = if (dates) null else second,
                                    noteEdtf = if (dates) second else null,
                                ),
                            )
                        }
                    }
                }

            val now = System.currentTimeMillis()

            // The next dated thing. **Only what has not happened**, because a
            // card called next up that shows last week is answering a different
            // question than the one it names.
            put("next_up") {
                val next = one(
                    "SELECT title, scheduled_edtf FROM live_appointment " +
                        "WHERE subject_id = ? AND scheduled_start >= ? " +
                        "ORDER BY scheduled_start LIMIT 1",
                    subjectId, now.toString(),
                )
                TodayAnswer(
                    title = next?.first,
                    whenEdtf = next?.second,
                    // **The rest of what is ahead, for the wide form.** 21.7:
                    // two on one day become two lines. Which of these belong to
                    // the same day is decided in Kotlin rather than in SQL,
                    // because a day is a timezone question and `scheduled_start`
                    // is an instant: comparing epoch milliseconds here would put
                    // an early appointment on the wrong side of midnight for
                    // somebody in the wrong offset.
                    items = manyOf(
                        "SELECT title, scheduled_edtf FROM live_appointment " +
                            "WHERE subject_id = ? AND scheduled_start >= ? " +
                            "ORDER BY scheduled_start LIMIT ? OFFSET 1",
                        subjectId, now.toString(), TODAY_CARD_ITEMS.toString(),
                        dates = true,
                    ),
                    // The card says only what shares the day with the answer, so
                    // the count cannot be a sample of it.
                    itemsSampleTheCount = false,
                )
            }

            // **The list is the same answer at a larger size**, 21.3, and this
            // card is the example that rule is written around: at small a
            // count, at wide the list itself, ready to show a nurse.
            //
            // **Record keeping only.** D111: nothing here reminds, alarms, or
            // tracks a dose. It says what is on the list, in the words the
            // person was told, and the dose is never parsed into a number and a
            // unit because misparsing a dose is worse than not parsing one.
            put("medications") { TodayAnswer(
                count = countOf(
                    "SELECT COUNT(*) FROM live_medication WHERE subject_id = ? " +
                        "AND stopped_edtf IS NULL",
                    subjectId,
                ),
                items = manyOf(
                    "SELECT name, dose_text FROM live_medication WHERE subject_id = ? " +
                        "AND stopped_edtf IS NULL ORDER BY name LIMIT ?",
                    subjectId, TODAY_CARD_ITEMS.toString(),
                ),
            ) }
            // Counted here rather than through the section's own suspend
            // helpers, so every answer in this pass runs on one connection
            // inside one guard. The predicates are the same ones those helpers
            // use, and `check_query_ordering.py` holds both to the live views.
            put("incidents") {
                TodayAnswer(
                    count = countOf(
                        "SELECT COUNT(*) FROM live_incident WHERE subject_id = ? " +
                            "AND resolved_at IS NULL",
                        subjectId,
                    ),
                )
            }
            put("unfiled") {
                TodayAnswer(
                    count = countOf(
                        "SELECT COUNT(*) FROM live_entry WHERE subject_id = ? " +
                            "AND is_unfiled = 1",
                        subjectId,
                    ),
                )
            }
            put("money") {
                // **Amounts only at wide**, 21.7, and the amount travels as a
                // number in minor units with its own currency rather than as
                // text. Formatting is the screen's, because the shape belongs
                // to the reader's locale and the currency belongs to the bill.
                val open = database.rawQuery(
                    "SELECT description, state, amount_minor, currency FROM live_bill " +
                        "WHERE subject_id = ? AND state NOT IN ('paid', 'closed') " +
                        "ORDER BY coalesce(due_start, received_start, created_at) LIMIT ?",
                    arrayOf(subjectId, TODAY_CARD_ITEMS.toString()),
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                TodayItem(
                                    label = cursor.getString(0),
                                    amountMinor =
                                        if (cursor.isNull(2)) null else cursor.getLong(2),
                                    currency = cursor.getString(3),
                                ),
                            )
                        }
                    }
                }
                TodayAnswer(
                    count = countOf(
                        "SELECT COUNT(*) FROM live_bill WHERE subject_id = ? " +
                            "AND state NOT IN ('paid', 'closed')",
                        subjectId,
                    ),
                    items = open,
                )
            }
            put("care_team") { TodayAnswer(
                count = countOf(
                    "SELECT COUNT(*) FROM live_person WHERE subject_id = ? " +
                        "AND archived_at IS NULL",
                    subjectId,
                ),
                // **Who they are, not how to dial them.** 21.7 asks "how do I
                // reach them, now", and the answer to that at wide is an
                // outlined dialable number, which is an inline action and
                // belongs to #258. Growing a card reveals more of the same
                // answer, so what wide adds here is which people, by name.
                items = manyOf(
                    "SELECT display_name, role_label FROM live_person " +
                        "WHERE subject_id = ? AND archived_at IS NULL " +
                        "ORDER BY display_name LIMIT ?",
                    subjectId, TODAY_CARD_ITEMS.toString(),
                ),
            ) }
            put("recent_documents") { TodayAnswer(
                count = countOf(
                    "SELECT COUNT(*) FROM live_document WHERE subject_id = ?", subjectId,
                ),
                // **The title and its category, and never the paper itself.**
                // 21.7: this card never renders private content larger than a
                // thumbnail, and a title is what the person named it.
                items = database.rawQuery(
                    "SELECT d.title, d.category, a.sha256 FROM live_document d " +
                        "LEFT JOIN live_attachment a ON a.document_id = d.id " +
                        "WHERE d.subject_id = ? " +
                        "ORDER BY coalesce(d.received_start, d.created_at) DESC LIMIT ?",
                    arrayOf(subjectId, TODAY_CARD_ITEMS.toString()),
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                TodayItem(
                                    label = cursor.getString(0),
                                    note = cursor.getString(1)?.takeIf { it.isNotBlank() },
                                    imageSha = cursor.getString(2),
                                ),
                            )
                        }
                    }
                },
            ) }
            put("standing_instructions") { TodayAnswer(
                count = countOf(
                    "SELECT COUNT(*) FROM live_standing_instruction WHERE subject_id = ? " +
                        "AND ended_edtf IS NULL",
                    subjectId,
                ),
                // **In force means not ended.** The count was every instruction
                // ever written, including the ones the person had ended, so a
                // card asking how many are active answered with how many have
                // existed. Its own screen has always filtered these.
                items = manyOf(
                    "SELECT name, notes FROM live_standing_instruction " +
                        "WHERE subject_id = ? AND ended_edtf IS NULL " +
                        "ORDER BY name LIMIT ?",
                    subjectId, TODAY_CARD_ITEMS.toString(),
                ),
                // **And are any issues noted**, which is the other half of the
                // question 21.7 asks and which the card never answered. This is
                // the part of the record a family actually needs in a room:
                // "we asked in writing in March, and it happened again in May
                // and again in June" is a different conversation.
                //
                // **Zero is not a line.** A card saying "not followed 0 times"
                // introduces a worry to somebody who does not have one, and
                // 21.4 says quiet is allowed to be good news without being
                // announced.
                detailKey = "instruction.violations.count",
                detailCount = countOf(
                    "SELECT COUNT(*) FROM live_instruction_violation v " +
                        "JOIN live_standing_instruction s ON s.id = v.instruction_id " +
                        "WHERE s.subject_id = ?",
                    subjectId,
                ).takeIf { it > 0 },
            ) }
            // Open means unanswered. `question` has `answer_text` and no
            // `resolved_at`, and asking for one that is not there throws.
            put("ask_next_time") { TodayAnswer(
                count = countOf(
                    "SELECT COUNT(*) FROM live_question WHERE subject_id = ? " +
                        "AND answer_text IS NULL",
                    subjectId,
                ),
                // **And for whom**, which is half the question 21.7 asks. The
                // named person first and the role when there is no name, so a
                // question saved for "the ward nurse" still says who it is for.
                items = manyOf(
                    "SELECT q.text, coalesce(p.display_name, q.role_label) " +
                        "FROM live_question q " +
                        "LEFT JOIN live_person p ON p.id = q.person_id " +
                        "WHERE q.subject_id = ? AND q.answer_text IS NULL " +
                        "ORDER BY q.created_at LIMIT ?",
                    subjectId, TODAY_CARD_ITEMS.toString(),
                ),
            ) }
            put("emergency_card") { TodayAnswer(
                // **No question, on purpose**, 21.7. This card is pure access:
                // one tap to the hand-over screen. Its count is whether there
                // is one to hand over at all.
                count = countOf(
                    "SELECT COUNT(*) FROM live_emergency_card WHERE subject_id = ?", subjectId,
                ),
            ) }

            put("milestones") {
                val latest = one(
                    // `label`, not `title`. A milestone is labeled rather than
                    // titled, and the wrong name throws rather than returning
                    // nothing, which is worse: the shell's catch turned it into
                    // every card on Today saying "Nothing waiting" at once.
                    "SELECT label, occurred_edtf FROM live_milestone WHERE subject_id = ? " +
                        "ORDER BY occurred_start DESC LIMIT 1",
                    subjectId,
                )
                TodayAnswer(title = latest?.first, whenEdtf = latest?.second)
            }

            put("trail_lately") {
                val latest = one(
                    "SELECT coalesce(title, body), occurred_edtf FROM live_entry " +
                        "WHERE subject_id = ? ORDER BY occurred_start DESC LIMIT 1",
                    subjectId,
                )
                TodayAnswer(
                    // **No count on this card.** 21.7 asks it "what were the
                    // last few entries", and the answer is the entries. A total
                    // of 182 is a fact about the trail rather than an answer to
                    // that question, and it was being announced to a reader
                    // while the screen did not show it at all, because the big
                    // number only renders when there is no title.
                    title = latest?.first,
                    // The date the latest entry carries, which the card showed
                    // nowhere: "what were the last few entries" with no when.
                    whenEdtf = latest?.second,
                    // **The last few, which is what the card is called**, and
                    // the newest one is in here rather than skipped: at tall
                    // these are the three waypoints of the mini spine 21.7 asks
                    // for, and a spine that starts at the second entry is a
                    // spine missing its head. #259.
                    //
                    // **The screen drops the first one at wide**, where the
                    // newest entry is already the answer at display size and
                    // listing it again made the card say "Care plan meeting"
                    // twice. That is a rendering rule and it lives with the
                    // renderer that has the size in front of it.
                    //
                    // **The kind and the resolved instant travel too**, because
                    // a spine node's color comes from the kind and its gap
                    // marker is arithmetic on two instants. Neither is display
                    // text and neither is decided here.
                    items = database.rawQuery(
                        "SELECT coalesce(title, body), occurred_edtf, kind, occurred_start " +
                            "FROM live_entry WHERE subject_id = ? " +
                            "ORDER BY occurred_start DESC LIMIT ?",
                        arrayOf(subjectId, TODAY_CARD_ITEMS.toString()),
                    ).use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                add(
                                    TodayItem(
                                        label = cursor.getString(0),
                                        noteEdtf = cursor.getString(1)
                                            ?.takeIf { it.isNotBlank() },
                                        kind = cursor.getString(2),
                                        noteStart = if (cursor.isNull(3)) {
                                            null
                                        } else {
                                            cursor.getLong(3)
                                        },
                                    ),
                                )
                            }
                        }
                    },
                )
            }

            answers
        }

    /**
     * The answer for a card that points at something, which the map cannot hold.
     *
     * A measure card, a project card and a care team card pointed at one person
     * are one per source, so their answers belong to the instance rather than
     * to the type.
     *
     * **Null means this card is not sourced**, and the caller falls back to the
     * answer for its type. That is how the care team card carries two variants
     * without being two card types: with a source it is one person and their
     * number, without one it is everybody. 21.7.
     */
    suspend fun todayAnswerForSource(
        cardType: String,
        sourceTable: String?,
        sourceId: String?,
    ): TodayAnswer? = withContext(Dispatchers.IO) {
        if (sourceId == null) return@withContext null
        val database = db().database
        when (cardType) {
            // **The value is the answer and the name is the tab.** 21.7 asks
            // "what is the latest value", and this card answered with the word
            // Weight at display size and the reading in the quiet line under
            // it, which is the question and the answer the wrong way round.
            //
            // **A measure with no reading yet is the none-yet rung**, not a
            // missing card: the person chose to track it and has not written
            // anything down. It keeps its name and says nothing is there.
            // **`coalesce`, because a value lives in one of two columns.** This
            // read only `value_text`, which is null for every measure recorded
            // as a number, so a weight card with a hundred readings behind it
            // had no answer at all and rendered "No readings yet" directly above
            // its own chart. Seen on the phone; nothing in the code says which
            // column a measure uses, because a measure can use either.
            "measure" -> database.rawQuery(
                "SELECT m.name, " +
                    "trim(coalesce(v.value_text, v.value_number || '') || ' ' || " +
                    "coalesce(v.unit, '')), " +
                    "v.occurred_edtf FROM live_measure m " +
                    "LEFT JOIN live_measurement v ON v.measure_id = m.id " +
                    "WHERE m.id = ? ORDER BY v.occurred_start DESC LIMIT 1",
                arrayOf(sourceId),
            ).use {
                if (!it.moveToFirst()) null
                else TodayAnswer(
                    sourceName = it.getString(0),
                    title = it.getString(1),
                    whenEdtf = it.getString(2),
                )
            }?.let { answer ->
                // **The recent shape, which is the other half of the question.**
                // 21.7 asks "what is the latest value, and its recent shape",
                // and the card had no shape at any size. Read oldest first,
                // because a chart is drawn along time and `chartPoints` positions
                // by it.
                answer.copy(
                    // **A measure recorded in words has no shape to draw**, and
                    // plenty are: "Brighter than yesterday. Ate most of her
                    // lunch." is a real measure and the app never parses it.
                    // At tall that left a card of empty space, which rule 11
                    // rules out. The recent readings are the same answer
                    // revealed further, which is what 21.3 asks a larger size
                    // to do, so every measure has something to grow into.
                    items = database.rawQuery(
                        // **Offset by one, because the newest reading is
                        // already the answer above.** Without it the card said
                        // the same sentence twice, once at display size and
                        // once in the list under it, which is the shape of a
                        // card arguing with itself.
                        "SELECT coalesce(value_text, value_number), occurred_edtf " +
                            "FROM live_measurement WHERE measure_id = ? " +
                            "ORDER BY occurred_start DESC LIMIT ? OFFSET 1",
                        arrayOf(sourceId, TODAY_CARD_ITEMS.toString()),
                    ).use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                add(
                                    TodayItem(
                                        label = cursor.getString(0),
                                        noteEdtf = cursor.getString(1),
                                    ),
                                )
                            }
                        }
                    },
                    series = database.rawQuery(
                        "SELECT id, measure_id, value_number, value_text, unit, " +
                            "occurred_edtf, occurred_start, note, source " +
                            "FROM live_measurement WHERE measure_id = ? " +
                            "AND value_number IS NOT NULL AND occurred_start IS NOT NULL " +
                            "ORDER BY occurred_start DESC LIMIT ?",
                        arrayOf(sourceId, TODAY_CARD_POINTS.toString()),
                    ).use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                add(
                                    Reading(
                                        id = cursor.getString(0),
                                        measureId = cursor.getString(1),
                                        number = cursor.getDouble(2),
                                        text = cursor.getString(3),
                                        unit = cursor.getString(4),
                                        occurredEdtf = cursor.getString(5),
                                        occurredStart = cursor.getLong(6),
                                        note = cursor.getString(7),
                                        source = cursor.getString(8),
                                    ),
                                )
                            }
                        }.reversed()
                    },
                )
            }

            // **One chosen person, which is the care team card's other half.**
            // 21.7 asks "how do I reach them, now" and the grid draws two
            // answers to it: one person with their number as an outlined pill,
            // or the row of everyone. A card with a source is the first. A card
            // without one falls through to the count in [todayAnswers], which
            // is the second, and that is the whole difference between them.
            //
            // **The base table, deliberately**, exactly as the project cards
            // read theirs. Somebody archived is the person saying they are no
            // longer involved, and a card pointing at them renders the
            // source-closed rung, 21.4, which it can only do if it can still
            // read the name. It stays until the person's own hand removes it.
            "care_team" -> database.rawQuery(
                "SELECT display_name, role_label, phone, archived_at, deleted_at " +
                    // allow-base-table: the source-closed rung is the whole point.
                    "FROM person WHERE id = ?",
                arrayOf(sourceId),
            ).use {
                // **Gone entirely**, which an import from a notebook that had
                // them can produce. The card says it cannot reach what it
                // points at rather than quietly becoming a different card.
                if (!it.moveToFirst()) TodayAnswer(sourceClosed = true)
                else {
                    val closed = !it.isNull(3) || !it.isNull(4)
                    TodayAnswer(
                        // **The name is the answer and the tab stays the tab.**
                        // Unlike a measure, whose name qualifies "Progress ·
                        // Weight", the question here is who and how, so the
                        // person's name belongs at display size rather than
                        // beside the section word.
                        title = it.getString(0),
                        detail = it.getString(1)?.takeIf { role -> role.isNotBlank() },
                        // **No number to offer for somebody no longer
                        // involved.** The card keeps working as a door to them,
                        // because the record of who they were is still worth
                        // reaching, but offering to dial them would be the app
                        // suggesting a call the person has already decided
                        // against making.
                        phone = if (closed) {
                            null
                        } else {
                            it.getString(2)?.takeIf { number -> number.isNotBlank() }
                        },
                        sourceClosed = closed,
                    )
                }
            }

            "project_standing", "project_date", "project_steps" -> {
                val project = database.rawQuery(
                    // **The base table, deliberately.** A card pointing at a
                    // tombstoned project renders its source-closed rung, 21.4,
                    // and it can only do that if it can still read the name.
                    "SELECT name, deleted_at, status, waiting_on, waiting_since " +
                        // allow-base-table: the source-closed rung is the whole point.
                        "FROM project WHERE id = ?",
                    arrayOf(sourceId),
                ).use {
                    if (!it.moveToFirst()) null
                    else ProjectFacts(
                        name = it.getString(0),
                        // Closed means finished or removed. Both are states the
                        // person put the project in, and neither is a reason
                        // for the app to take the card off their screen.
                        closed = !it.isNull(1) ||
                            it.getString(2) in setOf("done", "abandoned"),
                        status = it.getString(2),
                        waitingOn = it.getString(3),
                        waitingSince = if (it.isNull(4)) null else it.getLong(4),
                    )
                }
                if (project == null) {
                    // **The project is gone entirely**, which an import from a
                    // notebook that had it can produce. The card is kept and
                    // says it cannot reach what it points at.
                    TodayAnswer(sourceClosed = true)
                } else {
                    projectCardAnswer(database, cardType, sourceId, project)
                }
            }

            else -> null
        }
    }

    /** What the three project cards all need to read once. */
    private data class ProjectFacts(
        val name: String,
        val closed: Boolean,
        val status: String,
        val waitingOn: String?,
        val waitingSince: Long?,
    )

    /**
     * One project card's answer. `DESIGN.md` 21.7.
     *
     * **Three cards, three questions, one project.** They differ in what they
     * ask, not in what they point at, and all three carry the project's name on
     * the tab so four of them on one screen can be told apart.
     *
     * **A closed project answers that it is closed and nothing else.** 21.4's
     * source-closed rung: the card says so plainly, keeps working as a door, and
     * stays until the person's own hand removes it. Reporting a countdown to a
     * date on a project somebody finished would be the app talking about
     * something that is over.
     */
    private fun projectCardAnswer(
        database: net.zetetic.database.sqlcipher.SQLiteDatabase,
        cardType: String,
        projectId: String,
        project: ProjectFacts,
    ): TodayAnswer {
        if (project.closed) {
            return TodayAnswer(sourceName = project.name, sourceClosed = true)
        }
        return when (cardType) {
            // **Whose hands, since when.** The named person is the answer where
            // there is one, because "waiting on the county" is what the person
            // actually wants off this card, and the status word alone is what
            // the card said before.
            "project_standing" -> TodayAnswer(
                sourceName = project.name,
                title = project.waitingOn,
                // The status travels as its stored value and the screen words
                // it, the same as everywhere else.
                detail = project.status,
                whenEdtf = project.waitingSince?.let {
                    Edtf.day(
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate(),
                    ).canonical
                },
            )

            // **How many days to the date that matters**, and the counting is
            // the screen's: a number of days is a fact about now, and now is
            // not something a query result should be frozen at. What comes out
            // of here is the date itself.
            "project_date" -> database.rawQuery(
                "SELECT kind, due_edtf FROM live_project_date " +
                    "WHERE project_id = ? AND due_start IS NOT NULL " +
                    "ORDER BY due_start LIMIT 1",
                arrayOf(projectId),
            ).use {
                if (!it.moveToFirst()) TodayAnswer(sourceName = project.name)
                else TodayAnswer(
                    sourceName = project.name,
                    title = it.getString(0),
                    whenEdtf = it.getString(1),
                )
            }

            // **Counts only**, 21.7: the steps themselves live in the project,
            // and a card is never a dense feed. One line per cluster saying how
            // many of its steps are done, which is a count of the work rather
            // than a score on the person: rule 13 rules out a completion meter
            // and this is a tally of a plan, not of anybody's diligence.
            "project_steps" -> database.rawQuery(
                "SELECT coalesce(cluster, ''), COUNT(*), " +
                    "SUM(CASE WHEN completed_edtf IS NULL THEN 0 ELSE 1 END) " +
                    "FROM live_project_step WHERE project_id = ? " +
                    "GROUP BY coalesce(cluster, '') ORDER BY MIN(sort_index)",
                arrayOf(projectId),
            ).use { cursor ->
                val clusters = buildList {
                    while (cursor.moveToNext()) {
                        add(
                            TodayItem(
                                label = cursor.getString(0),
                                note = "${cursor.getInt(2)}/${cursor.getInt(1)}",
                            ),
                        )
                    }
                }
                TodayAnswer(
                    sourceName = project.name,
                    // **How many steps the plan has, not how many are done.** A
                    // completed count at display size is a progress meter on
                    // somebody's own diligence, which rule 13 rules out. This
                    // sums each cluster's total, and the per cluster tally in
                    // the items is a count of the plan, which is what 21.7 asks.
                    count = clusters.sumOf {
                        it.note?.substringAfter('/')?.toIntOrNull() ?: 0
                    },
                    // **A step with no cluster is still in a group**, so the
                    // blank label is not filtered away: a project whose steps
                    // are clustered except for a few would otherwise lose the
                    // few, and the group keeps its place and the screen names
                    // it.
                    //
                    // **Except when it is the only group, where it is a score
                    // and nothing else.** A plan with no clusters at all rendered
                    // one line reading "Everything else, 0 of 10", which is a
                    // completion tally over the whole list under a bin name
                    // meaning "the ones that did not fit anywhere". Rule 13
                    // rules out a meter on the person's own diligence, and 20.2
                    // says in as many words that a wall of unchecked boxes makes
                    // the waiting feel like their failure. Seen on a brand new
                    // notebook where it was the first thing the app said.
                    //
                    // **The count above it stays**, because how many steps the
                    // plan has is a fact about the plan.
                    items = if (clusters.size == 1 && clusters.single().label.isBlank()) {
                        emptyList()
                    } else {
                        clusters
                    },
                    itemsSampleTheCount = false,
                )
            }

            else -> TodayAnswer(sourceName = project.name)
        }
    }

    // -- what a person arranged on Today ------------------------------------
    //
    // contract/DATA-CONTRACT.md 8.7 and DESIGN.md 21. **The trust model of this
    // surface lives in this section**, so it is worth saying what it is before
    // any of the functions: the app never rearranges Today, promotes a card,
    // injects one, or removes one. **Nothing below is called by data changing.**
    // Every function here is reached from the person's own hand in edit mode,
    // or from the situation template at onboarding, and there is deliberately
    // no function that reorders by relevance, recency, or urgency, because a
    // surface that quietly reorders itself is one nobody can trust to still be
    // where they left it.

    /** One card on Today, as the person placed it. */
    data class TodayCard(
        val id: String,
        val type: String,
        val size: String,
        val sortIndex: Int,
        val isLead: Boolean,
        /**
         * What this card points at, where it points at something.
         *
         * A pair rather than a typed id, because a card can point at a measure,
         * a project, or a person. **It is kept even when it no longer resolves**,
         * per 8.7: the card renders its source-closed rung and stays until the
         * person's own hand removes it.
         */
        val sourceTable: String?,
        val sourceId: String?,
    )

    /**
     * The whole surface: exactly one lead, and the field under it in order.
     *
     * **The lead is not nullable and that is the point.** `DESIGN.md` 21.1:
     * there is never zero and never two. Two is refused by the database, by the
     * partial unique index `ux_today_card_lead`. Zero is refused here, by this
     * type having nowhere to put the absence.
     */
    data class TodayLayout(
        val lead: TodayCard,
        val field: List<TodayCard>,
    ) {
        // `this.field` and not `field`, which inside a property accessor means
        // the backing field of `all` rather than the property below it. The
        // design calls this the card field, 21.1, so the name is kept and the
        // reference is qualified.
        val all: List<TodayCard> get() = listOf(lead) + this.field
    }

    private fun readTodayCard(cursor: android.database.Cursor) = TodayCard(
        id = cursor.getString(0),
        type = cursor.getString(1),
        size = cursor.getString(2),
        sortIndex = cursor.getInt(3),
        isLead = cursor.getInt(4) == 1,
        sourceTable = cursor.getString(5),
        sourceId = cursor.getString(6),
    )

    /**
     * Today as the person left it, or null when nothing has been arranged yet.
     *
     * **Null means the layout has not been created**, which happens only before
     * onboarding applies a situation template. It does not mean an empty
     * dashboard, because nobody ever sees one: 21.5, defaults beat blank
     * canvases.
     *
     * A layout with cards but no lead is a broken layout rather than an empty
     * one, and this returns null for it rather than inventing a lead by picking
     * the first card. **Inventing one would be the app arranging Today**, which
     * is the one thing this surface must never do, and it would hide the defect
     * that produced it.
     */
    suspend fun todayLayout(subjectId: String): TodayLayout? = withContext(Dispatchers.IO) {
        val cards = db().database.rawQuery(
            "SELECT id, card_type, size, sort_index, is_lead, source_table, source_id " +
                "FROM live_today_card WHERE subject_id = ? " +
                "ORDER BY is_lead DESC, sort_index, created_at",
            arrayOf(subjectId),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(readTodayCard(cursor)) }
        }
        val lead = cards.firstOrNull { it.isLead } ?: return@withContext null
        TodayLayout(lead = lead, field = cards.filterNot { it.isLead })
    }

    /**
     * Writes a whole starting hand, replacing anything already there.
     *
     * **This is the only function that writes more than one card**, and it is
     * called at onboarding, when the situation template's starting hand is
     * applied. `DESIGN.md` 21.5: the template default is a starting hand and is
     * editable from the first minute without penalty.
     *
     * The first card in [cards] takes the lead. **All of it in one transaction**,
     * because a half-applied layout is a Today with two leads or none, and the
     * database would refuse the first of those halfway through.
     */
    suspend fun setTodayLayout(
        subjectId: String,
        cards: List<Pair<String, String>>,
        sources: Map<Int, Pair<String, String>> = emptyMap(),
    ): Int = withContext(Dispatchers.IO) {
        require(cards.isNotEmpty()) {
            "A starting hand with no cards would be a blank Today, which 21.5 rules out."
        }
        val database = db().database
        database.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            // Tombstoned, never deleted, per rule 3. The old arrangement stays
            // in the record and in the change log.
            database.write(
                "UPDATE today_card SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                    "WHERE subject_id = ? AND deleted_at IS NULL",
                arrayOf<Any?>(now, now, subjectId),
            )
            cards.forEachIndexed { index, (type, size) ->
                val source = sources[index]
                insertRow(
                    "today_card",
                    mapOf(
                        "subject_id" to subjectId,
                        "card_type" to type,
                        "size" to size,
                        "sort_index" to index,
                        "is_lead" to if (index == 0) 1 else 0,
                        "source_table" to source?.first,
                        "source_id" to source?.second,
                    ),
                )
            }
            database.setTransactionSuccessful()
            cards.size
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Adds one card to the end of the field.
     *
     * **The end, and never the lead.** Adding a card is reached from the gallery
     * in 21.6 screen 6, where the person chose a card to have, not a card to put
     * at the top. Promoting is its own deliberate act.
     */
    suspend fun addTodayCard(
        subjectId: String,
        type: String,
        size: String = "small",
        sourceTable: String? = null,
        sourceId: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val next = db().database.rawQuery(
            "SELECT coalesce(MAX(sort_index), -1) + 1 FROM live_today_card WHERE subject_id = ?",
            arrayOf(subjectId),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        insertRow(
            "today_card",
            mapOf(
                "subject_id" to subjectId,
                "card_type" to type,
                "size" to size,
                "sort_index" to next,
                "is_lead" to 0,
                "source_table" to sourceTable,
                "source_id" to sourceId,
            ),
        )
    }

    /**
     * Removes a card from the field.
     *
     * **The lead cannot be removed and this refuses rather than improvises.**
     * Removing it would leave zero, and the alternative, quietly promoting
     * whatever was next, is the app deciding what belongs at the top of
     * somebody's screen. Edit mode shows no remove dot on the lead, so this path
     * is not reachable by hand; it returns false rather than throwing, because a
     * caller that gets here has a bug and the person should not lose the screen
     * over it.
     */
    suspend fun removeTodayCard(cardId: String): Boolean = withContext(Dispatchers.IO) {
        val isLead = db().database.rawQuery(
            "SELECT is_lead FROM live_today_card WHERE id = ?",
            arrayOf(cardId),
        ).use { if (it.moveToFirst()) it.getInt(0) == 1 else return@withContext false }
        if (isLead) return@withContext false

        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE today_card SET deleted_at = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(now, now, cardId),
        )
        true
    }

    /** Changes one card's size. 21.3: growing reveals more of the same answer. */
    suspend fun setTodayCardSize(cardId: String, size: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db().database.write(
            "UPDATE today_card SET size = ?, updated_at = ?, rev = rev + 1 WHERE id = ?",
            arrayOf<Any?>(size, now, cardId),
        )
    }

    /**
     * Puts a card in the lead, and the old lead back into the field.
     *
     * **The demotion is half of the operation, not a consequence of it.** 21.1:
     * promoting demotes the previous lead into the field, and there is never
     * zero and never two. The database refuses two, so the order matters: the
     * old lead is cleared first, in the same transaction, or the insert of the
     * second lead fails against `ux_today_card_lead`.
     *
     * **The demoted card goes to the top of the field rather than the end**,
     * because it was at the top a second ago and the person is watching it move.
     * A card that vanished from the top and reappeared at the bottom of a long
     * scroll reads as lost.
     */
    suspend fun promoteTodayCardToLead(cardId: String): Boolean = withContext(Dispatchers.IO) {
        val database = db().database
        database.beginTransaction()
        try {
            val subjectId = database.rawQuery(
                "SELECT subject_id FROM live_today_card WHERE id = ? AND is_lead = 0",
                arrayOf(cardId),
            ).use {
                if (it.moveToFirst()) it.getString(0) else return@withContext false
            }

            val now = System.currentTimeMillis()
            val lowest = database.rawQuery(
                "SELECT coalesce(MIN(sort_index), 0) - 1 FROM live_today_card " +
                    "WHERE subject_id = ? AND is_lead = 0",
                arrayOf(subjectId),
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

            database.write(
                "UPDATE today_card SET is_lead = 0, sort_index = ?, updated_at = ?, " +
                    "rev = rev + 1 WHERE subject_id = ? AND is_lead = 1 AND deleted_at IS NULL",
                arrayOf<Any?>(lowest, now, subjectId),
            )
            database.write(
                "UPDATE today_card SET is_lead = 1, updated_at = ?, rev = rev + 1 WHERE id = ?",
                arrayOf<Any?>(now, cardId),
            )
            database.setTransactionSuccessful()
            true
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Moves a field card one place earlier or later.
     *
     * **This is the accessible reorder path**, 21.6 screen 7: Move up and Move
     * down exist so reordering works one-handed, with the reader on, and with
     * switch access. Drag is the shortcut, never the only way.
     *
     * A swap with the neighbor rather than a renumbering, for the reason
     * `moveProjectStep` gives: one move should cost two rows in the change log
     * rather than every card on the screen.
     *
     * **The lead does not take part.** It is not in the field, so moving the top
     * field card up does nothing rather than displacing the lead.
     */
    suspend fun moveTodayCard(cardId: String, earlier: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val database = db().database
            database.beginTransaction()
            try {
                val here = database.rawQuery(
                    "SELECT subject_id, sort_index FROM live_today_card " +
                        "WHERE id = ? AND is_lead = 0",
                    arrayOf(cardId),
                ).use {
                    if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
                } ?: return@withContext false

                val (subjectId, index) = here
                val comparison = if (earlier) "<" else ">"
                val direction = if (earlier) "DESC" else "ASC"
                val neighbor = database.rawQuery(
                    "SELECT id, sort_index FROM live_today_card " +
                        "WHERE subject_id = ? AND is_lead = 0 AND sort_index $comparison ? " +
                        "ORDER BY sort_index $direction LIMIT 1",
                    arrayOf(subjectId, index.toString()),
                ).use {
                    if (it.moveToFirst()) it.getString(0) to it.getInt(1) else null
                } ?: return@withContext false

                val now = System.currentTimeMillis()
                database.write(
                    "UPDATE today_card SET sort_index = ?, updated_at = ?, rev = rev + 1 " +
                        "WHERE id = ?",
                    arrayOf<Any?>(neighbor.second, now, cardId),
                )
                database.write(
                    "UPDATE today_card SET sort_index = ?, updated_at = ?, rev = rev + 1 " +
                        "WHERE id = ?",
                    arrayOf<Any?>(index, now, neighbor.first),
                )
                database.setTransactionSuccessful()
                true
            } finally {
                database.endTransaction()
            }
        }

    companion object {
        /**
         * The kinds of entry, in the order a month review counts them.
         *
         * **The notebook's own order, never by how many.** Ranking by count
         * would move the kinds around from month to month and put whichever
         * part of a month happened most at the top, which is the same mistake
         * [Digest] refuses to make with sections and for the same reason: the
         * places never move.
         *
         * A kind the schema gains later is left out rather than counted into
         * something, exactly as `Digest.sectionOf` leaves an unmapped table
         * out. It shows up in the entries themselves either way.
         */
        internal val KIND_ORDER = listOf(
            "call", "visit", "incident", "measurement", "question", "document", "note",
        )

        /** The person accepted the disclaimer at this time. Never cleared. */
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted_at"

        /** When the person last opened the app. What the digest reads from. */
        const val KEY_LAST_OPENED = "last_opened_at"

        /**
         * When an export last finished and was read back. #413.
         *
         * **A fact about the file, never a judgment on the person.** Rule 13
         * forbids the nag, not the date: no score, no percentage, no progress
         * meter, no prompt to do better, and no color that reads as a warning.
         * "Last saved 14 March" is the whole of it. A person who has not
         * exported in a year is not being told off; they are being told when.
         *
         * **Written only after the readback in #412 succeeds**, so the date
         * means an archive that opens rather than an attempt that was made.
         */
        const val KEY_LAST_EXPORT = "last_export_at"

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

/**
 * How many of a list a wide Today card shows before it says how many are left.
 *
 * Three, because a wide card carries the answer plus two or three lines, per
 * `DESIGN.md` 21.3, and the line after them is the one that says what is not
 * here. **The count on the answer stays the true total**, so a card that is
 * showing three of eleven says so rather than quietly cropping to three.
 */
private const val TODAY_CARD_ITEMS = 3

/**
 * How many readings a measure card's chart draws.
 *
 * **A card is never a dense feed**, 21.3, and the tall size carries a chart
 * rather than a history. Twelve is enough for a shape and few enough that the
 * line stays a line at a card's width.
 */
private const val TODAY_CARD_POINTS = 12

/**
 * The two statuses that mean a project is over. `DESIGN.md` 20.5 screen 17.
 *
 * **Named once**, because the same question was asked in three hand written
 * forms and a fourth status would have been added to two of them. Done and
 * abandoned are both endings, and the record keeps both exactly as it kept
 * everything else: screen 17 is called "closed, and kept".
 */
internal val FINISHED_STATUSES = setOf("done", "abandoned")

/**
 * The columns every reader of a violation needs, and the joins behind them.
 *
 * **Written once because there are two readers now**, one keyed by the person
 * and one by whatever the violation was linked to. Two copies of a nine column
 * select with three joins is two things to keep in step, and the second reader
 * exists precisely because the first one's columns were being read by nothing.
 */
private const val VIOLATION_SELECT =
    "SELECT v.id, v.instruction_id, v.occurred_edtf, v.note, " +
        "v.incident_id, i.title, v.bill_id, b.description, s.name " +
        "FROM live_instruction_violation v " +
        "JOIN live_standing_instruction s ON s.id = v.instruction_id " +
        "LEFT JOIN live_incident i ON i.id = v.incident_id " +
        "LEFT JOIN live_bill b ON b.id = v.bill_id "

/** Most recent first, by when it happened rather than by when it was typed. */
private const val VIOLATION_ORDER =
    "ORDER BY coalesce(v.occurred_start, v.created_at) DESC"
