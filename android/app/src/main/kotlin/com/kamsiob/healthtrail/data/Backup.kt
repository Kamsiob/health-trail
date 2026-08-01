package com.kamsiob.healthtrail.data

import android.content.Context
import com.kamsiob.healthtrail.time.Edtf
import java.io.File
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Export the whole notebook, and put it back.
 *
 * **This is the piece B4 assumed existed.** Dropping the emulator from this
 * project was justified on the grounds that data survival is not proven by a
 * long lived installation on one phone, which is a sample of one nobody can
 * reproduce, but by the export and import round trip against shared vectors in
 * continuous integration. That argument was correct and the test it rested on
 * did not exist, so until now nothing proved a person's records survive an
 * update at all.
 *
 * [ExportContainer] is the envelope: it writes the zip, reads it back, and
 * refuses a file that fails any of the format's checks. This is the part that
 * knows what goes in it and what to do with it on the way back.
 */
object Backup {

    /**
     * Everything the container needs, gathered from the live database.
     *
     * **Row counts include tables with zero rows**, because the manifest's job
     * is to let an importer state plainly what is about to be imported, and a
     * table silently missing from that list is indistinguishable from a table
     * that was never in the format.
     *
     * **Counted from the base tables rather than the live views.** An export
     * that drops tombstones cannot restore a deletion, so restoring a backup
     * would resurrect everything the person deleted. The counts have to
     * describe what is actually in the file.
     */
    suspend fun export(
        context: Context,
        target: File,
        exportedAt: Long,
        /** Null writes an unencrypted file. Cleared by the writer either way. */
        passphrase: CharArray? = null,
    ): ExportContainer.Manifest = withContext(Dispatchers.IO) {
        val database = HealthTrailDatabase.open(context)
        val file = context.getDatabasePath(HealthTrailDatabase.FILE_NAME)

        // The database is written to as this runs in the app. A checkpoint is
        // not enough on its own, so the file is copied to a staging path and
        // the copy is what goes in the archive, hashed as stored.
        val staged = File(context.cacheDir, "export-staging-$exportedAt.sqlite")
        file.copyTo(staged, overwrite = true)

        val store = Attachments.open(context)
        val attachments = store.all().map { store.fileFor(it) }

        try {
            ExportContainer.write(
                target = target,
                source = ExportContainer.Source(
                    database = staged,
                    attachments = attachments,
                    appVersion = com.kamsiob.healthtrail.BuildConfig.VERSION_NAME,
                    originDevice = database.deviceId,
                    rowCounts = rowCounts(database.database),
                    subjectCount = countOf(database.database, "subject"),
                    exportedAt = exportedAt,
                ),
                passphrase = passphrase,
            )
        } finally {
            staged.delete()
        }
    }

    /**
     * Every user data table and how many rows it holds, tombstones included.
     *
     * **Read from the database rather than from a list in Kotlin.** A hard
     * coded table list is a second declaration of the schema, which is the one
     * thing D16 exists to prevent, and it would go stale the first time a table
     * was added without anybody noticing that the export had quietly stopped
     * describing it.
     */
    internal fun rowCounts(database: SQLiteDatabase): Map<String, Int> =
        userTables(database).associateWith { countOf(database, it) }

    /**
     * The user data tables, meaning the ones a `live_` view is defined over.
     *
     * That is the definition the schema itself already uses to mean "carries
     * the six contract columns and a tombstone", so it needs no second list.
     */
    internal fun userTables(database: SQLiteDatabase): List<String> =
        database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'view' AND name LIKE 'live_%' " +
                "ORDER BY name",
            null,
        ).use {
            buildList { while (it.moveToNext()) add(it.getString(0).removePrefix("live_")) }
        }

    private fun countOf(database: SQLiteDatabase, table: String): Int =
        // allow-base-table: the manifest describes what is in the file, and the
        // file carries tombstones. Counting the view would understate it and an
        // importer checking counts against the payload would reject the export.
        database.rawQuery("SELECT COUNT(*) FROM $table", null)
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * Every EDTF column group in the schema, found rather than declared.
     *
     * A group is `<name>_edtf`, `<name>_zone`, `<name>_start`, `<name>_end`.
     * There are thirty of them and listing them here would be the second
     * declaration D16 forbids, so they are read out of the table definitions.
     */
    internal fun edtfGroups(database: SQLiteDatabase): List<Pair<String, String>> =
        buildList {
            for (table in userTables(database)) {
                database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                    val nameColumn = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        val column = cursor.getString(nameColumn)
                        if (column.endsWith("_edtf")) {
                            add(table to column.removeSuffix("_edtf"))
                        }
                    }
                }
            }
        }

    /**
     * Recomputes every derived date range from the EDTF string beside it.
     *
     * **Run on import, and this is a requirement of the format rather than a
     * precaution.** `contract/export-format.md` says the `_edtf` column round
     * trips byte for byte and the `_start` and `_end` columns are recomputed on
     * the other side rather than trusted from the file.
     *
     * **The reason is that they are an index, not a second source of truth.** A
     * range that arrives already disagreeing with its own EDTF string would
     * make an entry sort and search as a date the person never wrote, and
     * nothing would ever flag it, because both values look plausible on their
     * own. Recomputing makes the EDTF authoritative by construction.
     *
     * It also absorbs a real case: a file written by a build whose resolution
     * had a bug, or whose rules changed, is corrected on the way in rather than
     * carrying the old answer forever.
     *
     * **The EDTF string itself is never touched.** It is what the person
     * expressed and nothing here is entitled to reinterpret it.
     */
    internal fun recomputeRanges(database: SQLiteDatabase): Int {
        var updated = 0
        for ((table, group) in edtfGroups(database)) {
            val rows = database.rawQuery(
                "SELECT id, ${group}_edtf, ${group}_zone FROM $table " +
                    "WHERE ${group}_edtf IS NOT NULL",
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            Triple(
                                cursor.getString(0),
                                cursor.getString(1),
                                if (cursor.isNull(2)) null else cursor.getString(2),
                            )
                        )
                    }
                }
            }

            for ((id, edtf, zoneName) in rows) {
                val parsed = Edtf.parse(edtf) ?: continue
                val zone = runCatching { ZoneId.of(zoneName ?: "") }
                    .getOrElse { ZoneId.systemDefault() }
                val range = Edtf.resolve(parsed, zone)
                database.execSQL(
                    "UPDATE $table SET ${group}_start = ?, ${group}_end = ? WHERE id = ?",
                    arrayOf<Any?>(range.start, range.end, id),
                )
                updated++
            }
        }
        return updated
    }

    /**
     * Puts an opened container back, replacing whatever is here.
     *
     * **Whole file replacement rather than a row by row merge.** This is a
     * restore, not a sync: the file describes a complete notebook and the
     * intent is to end up with that notebook. Merging would need conflict
     * resolution, which is what `SyncTransport` is for and is a different
     * problem with a different issue.
     *
     * **Atomic in the way that matters.** The staged database is validated by
     * [ExportContainer.open] before this is called, the live database is closed
     * and swapped in one move, and the ranges are recomputed inside a
     * transaction. A failure leaves the original file in place.
     */
    suspend fun restore(
        context: Context,
        opened: ExportContainer.Opened,
    ): Result<Int> = withContext(Dispatchers.IO) {
        val live = context.getDatabasePath(HealthTrailDatabase.FILE_NAME)
        val backup = File(live.parentFile, "${HealthTrailDatabase.FILE_NAME}.replacing")

        HealthTrailDatabase.closeForTest()
        Repository.closeForTest()

        if (live.exists()) live.copyTo(backup, overwrite = true)

        try {
            opened.database.copyTo(live, overwrite = true)

            val attachments = Attachments.open(context)
            for (file in opened.attachments) {
                file.inputStream().use { attachments.put(it) }
            }

            // Reopened through the app's own path, so the restored file goes
            // through migrations and key handling exactly as any other open.
            val database = HealthTrailDatabase.open(context)
            val recomputed = database.database.let { handle ->
                handle.beginTransaction()
                try {
                    val n = recomputeRanges(handle)
                    handle.setTransactionSuccessful()
                    n
                } finally {
                    handle.endTransaction()
                }
            }

            backup.delete()
            Result.success(recomputed)
        } catch (problem: Throwable) {
            // Put back what was here. A half restored notebook that looks
            // complete is worse than a clean failure, because the person stops
            // worrying about it.
            HealthTrailDatabase.closeForTest()
            Repository.closeForTest()
            if (backup.exists()) {
                backup.copyTo(live, overwrite = true)
                backup.delete()
            }
            Result.failure(problem)
        }
    }
}
