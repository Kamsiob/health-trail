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
        /**
         * Required, and there is no way to ask for a file without one.
         *
         * **The export has no unencrypted form since format version 2.** The
         * payload is now a plain SQLite database, which is what makes the file
         * portable and is the whole point of D61, and it also means a plain
         * container is a fully readable copy of somebody's entire care record
         * sitting in a folder that something else may sync. The property that
         * fixed the recovery path is the one that makes the plain file
         * dangerous. D67.
         *
         * Cleared by the writer on every path.
         */
        passphrase: CharArray,
    ): ExportContainer.Manifest = withContext(Dispatchers.IO) {
        val database = HealthTrailDatabase.open(context)

        // **The archive carries a plain SQLite file, never the encrypted one as
        // it sits on disk.** The database at rest is SQLCipher, keyed by 32
        // random bytes wrapped by this device's Keystore, and that wrapping key
        // cannot be exported and does not travel. Copying the file verbatim
        // therefore produced an archive that only the phone that wrote it could
        // ever open, which is not a backup, it is a copy. It passed every round
        // trip test for exactly as long as those tests restored onto the same
        // device. D24 makes this file the only recovery path from key loss, so
        // a payload that needs the lost key to read is the one shape it must
        // not have. See `PortabilityTest`.
        //
        // What protects the contents is the container's own passphrase, chosen
        // by the person, which is what `contract/export-format.md` always said.
        // **Since format version 2 there is no export that declines it**, D67,
        // because a portable payload and an optional passphrase together mean
        // the whole record in the clear.
        val staged = decryptedCopy(context, database, exportedAt)

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
     * The shape this build creates, read from the database rather than listed.
     *
     * **This is the single source of truth for what an import may contain.**
     * `contract/schema.sql` created this database, so asking it what tables and
     * columns exist is asking the contract, with no second declaration to drift
     * out of step the first time a table is added. That is D16.
     */
    suspend fun schema(context: Context): ExportContainer.Schema =
        withContext(Dispatchers.IO) {
            val handle = HealthTrailDatabase.open(context).database
            handle.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE " +
                    "'sqlite_%'",
                null,
            ).use { tables ->
                buildMap {
                    while (tables.moveToNext()) {
                        val table = tables.getString(0)
                        handle.rawQuery("PRAGMA table_info(\"$table\")", null).use { info ->
                            val nameColumn = info.getColumnIndexOrThrow("name")
                            val columns = buildSet {
                                while (info.moveToNext()) add(info.getString(nameColumn))
                            }
                            put(table, columns)
                        }
                    }
                }
            }
        }

    /**
     * A portable, unencrypted copy of the whole database.
     *
     * `sqlcipher_export` is SQLCipher's own mechanism for this and it copies
     * the entire schema rather than the rows alone: tables, indexes, views, and
     * the change log triggers, which is what the contract requires to survive.
     * Rebuilding the schema from `contract/schema.sql` on the way out would work
     * too and would be a second declaration of it, which is what D16 exists to
     * prevent.
     *
     * **Inside a transaction**, because the app writes to this database while
     * the export runs and a copy taken across a half applied write would be a
     * file that fails its own hash check on the way back in, at the moment it is
     * needed most.
     *
     * The caller deletes the file. It is the one place in the app where records
     * exist unencrypted, so it lives in the cache under a name unique to the
     * export and does not outlive it.
     */
    private fun decryptedCopy(
        context: Context,
        database: HealthTrailDatabase,
        exportedAt: Long,
    ): File {
        val staged = File(context.cacheDir, "export-staging-$exportedAt.sqlite")
        // sqlcipher_export appends into whatever it finds, so a leftover file
        // from an interrupted export would be exported into rather than
        // replaced, and the archive would carry both notebooks.
        staged.delete()

        val handle = database.database
        handle.execSQL("ATTACH DATABASE ? AS portable KEY ''", arrayOf(staged.path))
        try {
            handle.beginTransaction()
            try {
                handle.rawQuery("SELECT sqlcipher_export('portable')", null).use { it.moveToNext() }
                handle.setTransactionSuccessful()
            } finally {
                handle.endTransaction()
            }
        } finally {
            handle.execSQL("DETACH DATABASE portable")
        }
        return staged
    }

    /**
     * The reverse: a portable file turned back into this device's encrypted one.
     *
     * The restored database is keyed with **this** device's passphrase rather
     * than the one that wrote the file, which is the entire point. A notebook
     * moved to a new phone is encrypted at rest there by that phone's Keystore,
     * and the old device's key is neither needed nor recoverable.
     *
     * Built at a temporary path and moved into place by the caller, so a failure
     * partway through never leaves a half written database where the live one
     * belongs.
     */
    private fun encryptedCopy(context: Context, portable: File, target: File) {
        target.delete()
        val key = DatabaseKey(context)
        val passphrase = key.passphrase()
        val encrypted = try {
            SQLiteDatabase.openOrCreateDatabase(target, passphrase, null, null)
        } finally {
            passphrase.fill(0)
        }

        try {
            encrypted.execSQL("ATTACH DATABASE ? AS portable KEY ''", arrayOf(portable.path))
            try {
                encrypted.rawQuery("SELECT sqlcipher_export('main', 'portable')", null)
                    .use { it.moveToNext() }
            } finally {
                encrypted.execSQL("DETACH DATABASE portable")
            }
        } finally {
            encrypted.close()
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
            // **Re-encrypted with this device's key, never copied in.** The
            // file in the archive is a plain SQLite database, which is what
            // makes it portable at all, and putting it at the live path
            // unchanged would leave the notebook sitting unencrypted on disk.
            // The one built here is keyed by this phone's Keystore, so a
            // notebook that arrives from another device is protected here the
            // same way it was there.
            val rebuilt = File(live.parentFile, "${HealthTrailDatabase.FILE_NAME}.arriving")
            try {
                encryptedCopy(context, opened.database, rebuilt)
                rebuilt.copyTo(live, overwrite = true)
                // The write ahead log and shared memory file belong to the
                // database that was just replaced. Left behind, they describe
                // pages that no longer exist in the file beside them, which is
                // how a restore that reported success opens corrupt afterward.
                File(live.path + "-wal").delete()
                File(live.path + "-shm").delete()
            } finally {
                rebuilt.delete()
            }

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
