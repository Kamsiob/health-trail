package com.kamsiob.healthtrail.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Putting an opened archive into the notebook that is already here.
 * `contract/DATA-CONTRACT.md` section 8.3, issue #211.
 *
 * **The other half of `Backup.restore`, and a different promise.** Restore
 * replaces: the file describes a complete notebook and the intent is to end up
 * with that notebook. This merges: two phones each hold part of the record and
 * the intent is to end up with both. **8.3 says the choice between them is
 * explicit and described in plain words, never a guess**, which is why they are
 * two functions with two names rather than a flag.
 *
 * **Everything is decided before anything is written.** [Merge] is pure and
 * produces the whole plan first, so a merge that cannot place a row never opens
 * a transaction at all. That is what makes "fully succeeds or changes nothing"
 * true rather than hoped for.
 *
 * **The change log tells the truth about what happened.** Inserts go in as
 * inserts and updates as updates, so the schema's own triggers record the right
 * operation. `INSERT OR REPLACE` would have been one statement instead of two
 * and would have written every update to the change log as an insert, which is
 * a history that did not happen.
 */
internal object MergeApply {

    /**
     * What a merge did, in the terms the person is told it in.
     *
     * @param conflicts how many rows had two versions. **Reported even when
     *   this phone's copy won**, because the person needs to know a second
     *   version existed.
     */
    data class Report(
        val inserted: Int,
        val updated: Int,
        val unchanged: Int,
        val conflicts: Int,
        val attachments: Int,
        val skipped: Map<String, String>,
    )

    /** Why a merge did not run. Nothing was written when this comes back. */
    data class Refused(val dangling: List<Merge.Dangling>) : Exception(
        "This file has ${dangling.size} record(s) that point at something neither " +
            "notebook has, so nothing was merged. " +
            dangling.take(3).joinToString("; ") {
                "a ${it.table} refers to a ${it.parentTable} that is not here"
            },
    )

    /**
     * Merge an opened container into the live notebook.
     *
     * @param mergedAt the moment to stamp on the conflict log, passed in rather
     *   than read from a clock so that a test can assert on it. Nothing else
     *   here takes a timestamp: 8.3's rule is that nothing is invented, and a
     *   merged row keeps the `updated_at` it arrived with.
     */
    suspend fun merge(
        context: Context,
        opened: ExportContainer.Opened,
        mergedAt: Long,
    ): Result<Report> = withContext(Dispatchers.IO) {
        val handle = HealthTrailDatabase.open(context).database
        val schema = Backup.schema(context)
        // **Asked of the schema rather than listed.** A merge matches rows by
        // `id` and resolves them by `updated_at`, so a table without both
        // cannot be merged whatever it is called. Naming them by hand is how
        // this broke the first time: the list said `migration` and the table is
        // `schema_migration`, so the read hit a table with no `id` and threw.
        val mergeable = schema.entries
            .filter { (table, columns) ->
                table !in Merge.NOT_MERGED && "id" in columns && "updated_at" in columns
            }
            .map { it.key }
            .sorted()

        val local = readAll(handle, mergeable)
        val present = SQLiteDatabase.openDatabase(
            opened.database.path, null, SQLiteDatabase.OPEN_READONLY,
        ).use { tablesIn(it) }
        val incoming = SQLiteDatabase.openDatabase(
            opened.database.path, null, SQLiteDatabase.OPEN_READONLY,
        ).use { readAll(it, mergeable.filter { table -> table in present }) }

        // **What the file holds and this did not merge, with the reason.**
        // Computed from the file's own tables rather than from the rows that
        // were read, because the rows that were read are by definition the ones
        // that were not skipped. Reporting from them said nothing, which the
        // instrumented test caught: 8.3's rule is that content is never quietly
        // left out, and a report that silently omits what it omitted is the
        // same failure one level up.
        val skipped = present
            .filter { it !in mergeable && it != "sqlite_sequence" }
            .sorted()
            .associateWith { table ->
                Merge.NOT_MERGED[table]
                    ?: "no id and updated_at, so a row in it cannot be matched or dated"
            }

        val plan = Merge.plan(local, incoming, references(handle, mergeable))
        if (!plan.canApply) return@withContext Result.failure(Refused(plan.dangling))

        // **Before the transaction, and deliberately.** An attachment is
        // content addressed, so writing one twice is writing the same bytes to
        // the same name and putting them in early cannot corrupt anything. What
        // it can leave behind, if the rows then fail, is a file nothing points
        // at, which is invisible and harmless. The reverse order could leave a
        // row pointing at bytes that are not there, which is not.
        val store = Attachments.open(context)
        var attachments = 0
        for (file in opened.attachments) {
            file.inputStream().use { store.put(it) }
            attachments++
        }

        handle.beginTransaction()
        try {
            for ((table, rows) in plan.inserts) {
                for (row in rows) insert(handle, table, row)
            }
            for ((table, rows) in plan.updates) {
                for (row in rows) update(handle, table, row)
            }
            for (conflict in plan.conflicts) writeConflict(handle, conflict, mergedAt)
            // The derived start and end columns are recomputed rather
            // than trusted from the file, per 3.1, and the EDTF string is what
            // round trips. A merged row brings its own EDTF and this puts the
            // index back in step with it.
            Backup.recomputeRanges(handle)
            handle.setTransactionSuccessful()
        } finally {
            handle.endTransaction()
        }

        Result.success(
            Report(
                inserted = plan.insertCount,
                updated = plan.updateCount,
                unchanged = plan.unchanged,
                conflicts = plan.conflicts.size,
                attachments = attachments,
                skipped = skipped,
            ),
        )
    }

    /**
     * Every row of every mergeable table, tombstones included.
     *
     * allow-base-table: a merge is about rows rather than about what a screen
     * shows, and the live views hide exactly the tombstones a merge has to see.
     * Reading through them would make a deletion look like a row the other
     * phone had and this one never did, and the merge would put it back.
     */
    private fun readAll(
        db: SQLiteDatabase,
        tables: List<String>,
    ): Map<String, List<Merge.Row>> = buildMap {
        for (table in tables) {
            val rows = mutableListOf<Merge.Row>()
            // Ordered by id, which is stable and locally generated, so a plan
            // never depends on the order SQLite happened to return.
            db.rawQuery("SELECT * FROM \"$table\" ORDER BY id", null).use { cursor ->
                val columns = cursor.columnNames
                while (cursor.moveToNext()) {
                    val row = LinkedHashMap<String, String?>(columns.size)
                    for (index in columns.indices) {
                        row[columns[index]] =
                            if (cursor.isNull(index)) null else cursor.getString(index)
                    }
                    rows += row
                }
            }
            put(table, rows)
        }
    }

    private fun tablesIn(db: SQLiteDatabase): Set<String> = buildSet {
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
            null,
        ).use { while (it.moveToNext()) add(it.getString(0)) }
    }

    /**
     * Which columns point at which table, asked of the database rather than
     * declared here.
     *
     * `contract/schema.sql` created this database, so `PRAGMA foreign_key_list`
     * is asking the contract. A list written out in Kotlin would be a second
     * declaration and would go stale the first time a column was added, which
     * is D16.
     */
    private fun references(
        db: SQLiteDatabase,
        tables: List<String>,
    ): Map<String, Map<String, String>> = buildMap {
        for (table in tables) {
            val columns = LinkedHashMap<String, String>()
            db.rawQuery("PRAGMA foreign_key_list(\"$table\")", null).use { cursor ->
                val parent = cursor.getColumnIndex("table")
                val from = cursor.getColumnIndex("from")
                if (parent >= 0 && from >= 0) {
                    while (cursor.moveToNext()) {
                        columns[cursor.getString(from)] = cursor.getString(parent)
                    }
                }
            }
            if (columns.isNotEmpty()) put(table, columns)
        }
    }

    private fun insert(db: SQLiteDatabase, table: String, row: Merge.Row) {
        val columns = row.keys.toList()
        db.execSQL(
            "INSERT INTO \"$table\" (${columns.joinToString(", ") { "\"$it\"" }}) " +
                "VALUES (${columns.joinToString(", ") { "?" }})",
            columns.map { row[it] }.toTypedArray(),
        )
    }

    /**
     * **By id and by nothing else**, which is 8.3's single most important rule.
     * Never by name, by row order, by index, or by any value the person can
     * edit: that is the common way a round trip silently produces a wrong
     * record, and the schema already prevents it.
     */
    private fun update(db: SQLiteDatabase, table: String, row: Merge.Row) {
        val columns = row.keys.filter { it != "id" }
        db.execSQL(
            "UPDATE \"$table\" SET ${columns.joinToString(", ") { "\"$it\" = ?" }} WHERE id = ?",
            (columns.map { row[it] } + row["id"]).toTypedArray(),
        )
    }

    /**
     * The version that lost, written down whole.
     *
     * **Both sides, as JSON, complete**, which is what the schema asks for so
     * that either can be restored by hand if the person disagrees with the
     * resolution. A record keeping app that quietly eats an entry has failed at
     * its one job.
     *
     * **The reason is a token rather than a sentence**, `Merge.Reason`, and it
     * is turned into words on the screen that shows it. A stored value is not
     * display text, and this row travels between phones that may be set to
     * different languages.
     */
    private fun writeConflict(db: SQLiteDatabase, conflict: Merge.Conflict, at: Long) {
        db.execSQL(
            "INSERT INTO conflict_log (table_name, row_id, resolved_at, winner, reason, " +
                "local_json, incoming_json) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                conflict.table,
                conflict.rowId,
                at,
                conflict.winner,
                conflict.reason,
                RowJson.write(conflict.local),
                RowJson.write(conflict.incoming),
            ),
        )
    }

}
