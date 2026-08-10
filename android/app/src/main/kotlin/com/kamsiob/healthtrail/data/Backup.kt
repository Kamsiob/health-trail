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
     * An attachment row that is live and whose bytes are not on this phone.
     *
     * **Moved into [ExportContainer] on 2026-08-10, and the move is the point.
     *
     * It began as an export-time finding this layer returned to its caller,
     * because the manifest had nowhere to put it. `contract/DATA-CONTRACT.md`
     * 8.2 always said the manifest carries the list, so once the format
     * document was corrected to match the contract it stopped being a finding
     * and became part of what an archive says about itself. #332, #210.
     */
    typealias MissingAttachment = ExportContainer.MissingAttachment

    /**
     * What an export produced, and what it noticed while producing it.
     *
     * **[export] used to return the manifest alone**, which had no room to say
     * that anything was wrong, so a notebook with an attachment row whose file
     * was gone produced an archive this app then refuses to open and told the
     * person it had succeeded. #332. The failure landed at restore, on the new
     * phone, with the old one gone, which is the shape `DATA-CONTRACT.md`
     * section 8 opens by calling worse than an honest failure.
     *
     * **[missingAttachments] is empty on every archive this project has made**,
     * and on every export where nothing went wrong. It is not a warning that
     * fires routinely; it is the one that has never fired and must not be
     * silent the day it does.
     */
    data class Written(
        val manifest: ExportContainer.Manifest,
        val missingAttachments: List<MissingAttachment>,
    )

    /**
     * Every live attachment row whose bytes the archive does not carry.
     *
     * **Read from the staged copy rather than the live database**, because the
     * staged copy is what actually ships. A row written between staging and now
     * is not in the archive, so reporting it would name a file the archive
     * never claimed, and a row deleted in that window is still in the archive
     * and still has to be checked.
     *
     * **The query is deliberately the same one [ExportContainer.open] refuses
     * on**, down to the tombstone clause, so what export looks for and what
     * import rejects cannot drift apart. A deleted attachment's bytes are
     * legitimately gone while its row still travels, because the row is how a
     * restore learns the deletion happened at all.
     *
     * **One entry per file, not per row.** Two entries can name one photograph,
     * since a file is named by the hash of its bytes, and telling somebody two
     * files are missing when one is would be a count they cannot reconcile with
     * anything they can see.
     */
    internal fun missingAttachments(
        staged: File,
        present: Set<String>,
    ): List<MissingAttachment> = android.database.sqlite.SQLiteDatabase.openDatabase(
        staged.path,
        null,
        android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
    ).use { open ->
        open.rawQuery(
            // allow-base-table: an export carries tombstones, and the live view
            // would hide exactly the rows whose files are allowed to be absent.
            // Ordered explicitly, per 8.4, so two exports of one notebook report
            // the same list in the same order.
            "SELECT sha256, original_filename, created_at FROM attachment " +
                "WHERE deleted_at IS NULL ORDER BY created_at, sha256",
            null,
        ).use { cursor ->
            val seen = mutableSetOf<String>()
            buildList {
                while (cursor.moveToNext()) {
                    val hash = cursor.getString(0)
                    if (hash in present || !seen.add(hash)) continue
                    add(
                        ExportContainer.MissingAttachment(
                            sha256 = hash,
                            originalFilename = if (cursor.isNull(1)) null else cursor.getString(1),
                            createdAt = cursor.getLong(2),
                        )
                    )
                }
            }
        }
    }

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
        /**
         * What the person wrote to remind themselves, or null.
         *
         * **A String rather than a CharArray, unlike the passphrase, and that is
         * the point.** The passphrase is wiped from memory because it opens the
         * record; the hint is written into the archive in the clear on purpose,
         * so treating it as a secret here would be a gesture rather than a
         * protection, and a misleading one for whoever reads this next.
         */
        passphraseHint: String? = null,
    ): Written = withContext(Dispatchers.IO) {
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
        // by the person, which is what `contract/EXPORT-FORMAT.md` always said.
        // **Since format version 2 there is no export that declines it**, D67,
        // because a portable payload and an optional passphrase together mean
        // the whole record in the clear.
        val staged = decryptedCopy(context, database, exportedAt)

        val store = Attachments.open(context)
        val onDisk = store.all()
        val attachments = onDisk.map { store.fileFor(it) }

        // **The export looks before it writes.** `Attachments.all` lists files
        // on disk rather than rows, so a live attachment row whose bytes are
        // gone ships as a row with no file, and [ExportContainer.open] then
        // refuses the whole archive by name. Nothing noticed, and the person was
        // told the export succeeded. #332.
        //
        // **What is not here is the other half**, and it is deliberately not
        // here: `contract/EXPORT-FORMAT.md` is published byte for byte and
        // `tools/decrypt/` was written from it, so putting this list into
        // `MANIFEST.json` is a format decision rather than a session's. Until
        // that is settled the finding travels to the caller instead, so the
        // person is told at the moment they can still do something about it.
        val missing = missingAttachments(staged, onDisk.toSet())

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
                    // **The contract's own schema file, shipped whole.** The
                    // build copies `contract/schema.sql` into the assets, so
                    // what travels inside somebody's archive is the real DDL
                    // with its reasoning in comments rather than a summary of
                    // it that could drift.
                    schemaSql = context.assets
                        .open(com.kamsiob.healthtrail.contract.ContractAssets.SCHEMA_PATH)
                        .use { it.readBytes().decodeToString() },
                    // **The readable copy's own words, in the person's
                    // language.** Read here rather than in the container for
                    // the same reason the schema is: this is the layer that has
                    // a context, and `ExportContainer` is the format. Asked of
                    // `Strings.load` rather than of `Locale.getDefault`,
                    // because the default is not reliably the language the
                    // person set this app to. #327.
                    readableWords = ReadableWords.from(
                        com.kamsiob.healthtrail.i18n.Strings.load(context),
                        // **The shipped catalogs' own names**, so a page says
                        // what a template is called rather than printing the
                        // identifier it is filed under. Read here for the same
                        // reason the schema and the words are: this is the layer
                        // that has a context. #329.
                        catalogNames = ReadableWords.catalogNames(context),
                    ),
                    passphraseHint = passphraseHint?.trim()?.takeIf { it.isNotEmpty() },
                    // **What the export looked for and did not find**, so the
                    // archive states it rather than failing to open on the new
                    // phone with the old one gone. #332.
                    missingAttachments = missing,
                    // 8.2 asks for the zone beside the instant, by name. An
                    // epoch alone cannot say what time it was where somebody
                    // was standing.
                    exportedZone = ZoneId.systemDefault().id,
                ),
                passphrase = passphrase,
            ).let { Written(manifest = it, missingAttachments = missing) }
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
     * precaution.** `contract/EXPORT-FORMAT.md` says the `_edtf` column round
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
