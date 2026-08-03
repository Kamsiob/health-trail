package com.kamsiob.healthtrail.data

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The export container, per `contract/export-format.md`.
 *
 * **The governing rule is one sentence:** anything the app can store, the
 * export contains and the import restores. A feature that stores something the
 * export does not carry silently loses records on device migration, which for
 * this audience is the worst failure the app can have.
 *
 * An ordinary zip with a `.htx` extension. The extension exists so the file is
 * recognizable and so nobody opens it expecting a document. **It stays an
 * ordinary zip on purpose:** in ten years somebody should be able to open it
 * with whatever they have.
 *
 * ```
 * manifest.json      always first, always unencrypted
 * data.sqlite        the whole database, tombstones included
 * attachments/       one file per attachment, named by its content hash
 * ```
 *
 * **The manifest is first and never encrypted** because an importer has to be
 * able to say what a file is before it can ask for a passphrase.
 *
 * **Tombstones travel.** An export that drops them cannot restore a deletion,
 * so restoring a backup would resurrect everything the person deleted. This is
 * the one place a tombstone is deliberately carried rather than filtered, and
 * it is why the whole database file is copied rather than the live views read.
 *
 * **Every export is encrypted. There is no unencrypted export**, as of format
 * version 2. See [FORMAT_VERSION] for why that changed.
 */
object ExportContainer {

    /**
     * The container format this build writes.
     *
     * **Version 2, and the change is that a passphrase is required.**
     *
     * Version 1 allowed an unencrypted export and offered it plainly, on the
     * reasoning that it is the person's data and wanting to read it is
     * reasonable. That reasoning was sound for what version 1 wrote: the
     * payload was the SQLCipher file as it sat on disk, so an unencrypted
     * container still held bytes nothing could read without this phone's
     * Keystore.
     *
     * **Making the export portable changed what an unencrypted one is.** The
     * payload is now a plain SQLite database, which is exactly what makes it
     * openable on another machine and is the whole point of D61. An unencrypted
     * export is therefore a fully readable copy of somebody's entire care
     * record: every call, every diagnosis in a note, every medication, every
     * bill, sitting in a folder that a file manager, a backup agent, or a cloud
     * sync may pick up and copy somewhere the person never chose.
     *
     * The same property that fixed the recovery path is what makes the plain
     * file dangerous, so the decision that was right at version 1 is wrong at
     * version 2. **A passphrase is required and there is no way to ask for a
     * file without one.** D67.
     *
     * The importer still reads a version 1 file, because refusing one would
     * destroy a real backup to make a point. What it refuses is an unencrypted
     * export of any version, and it says exactly what the file is rather than
     * failing generically.
     */
    const val FORMAT_VERSION = 2
    const val MANIFEST = "manifest.json"
    const val DATABASE = "data.sqlite"
    const val ATTACHMENTS = "attachments/"

    /** What a reader learned before deciding whether it can import. */
    data class Manifest(
        val formatVersion: Int,
        val appVersion: String,
        val platform: String,
        val exportedAt: Long,
        val originDevice: String,
        val encrypted: Boolean,
        /**
         * How the key was derived, present only when [encrypted].
         *
         * **Read from the file on import, never assumed from this build's
         * constants.** That is what lets the cost rise as hardware improves
         * without stranding a file written years earlier. An importer using
         * today's values against an older file fails to derive the key from a
         * correct passphrase and reports a wrong passphrase, which tells
         * somebody their memory is wrong when their file is fine.
         */
        val encryption: Encryption? = null,
        val databaseSha256: String,
        val databaseBytes: Long,
        val rowCounts: Map<String, Int>,
        val attachmentCount: Int,
        val attachmentBytes: Long,
        val subjectCount: Int,
    ) {
        companion object
    }

    /**
     * The key derivation parameters, recorded so an older file still opens.
     *
     * The salt and nonce are per file. Reusing a nonce under one key is the
     * mistake that breaks GCM outright rather than merely weakening it, so they
     * are generated fresh for every export and carried here rather than
     * derived from anything.
     */
    data class Encryption(
        val algorithm: String,
        val kdf: String,
        val iterations: Int,
        val memoryKib: Int,
        val parallelism: Int,
        val salt: String,
        val nonce: String,
    )

    /**
     * The shape this build expects, as table name to column names.
     *
     * Supplied by the caller from the database it actually created, rather than
     * written out a second time here. A hard coded copy of the schema is the
     * duplication D16 exists to prevent, and it would go stale the first time a
     * table was added.
     */
    typealias Schema = Map<String, Set<String>>

    /** Everything the writer needs, gathered by the caller so this stays pure plumbing. */
    data class Source(
        val database: File,
        val attachments: List<File>,
        val appVersion: String,
        val originDevice: String,
        val rowCounts: Map<String, Int>,
        val subjectCount: Int,
        val exportedAt: Long,
    )

    /**
     * Writes the container.
     *
     * **The manifest is written last and stored first.** Its hash and row
     * counts describe the payload, so the payload has to exist before they can
     * be true. Zip lets an entry be written in any order, and the format says
     * the manifest comes first in the archive, so the payload is staged and the
     * archive assembled with the manifest at the front.
     *
     * **No compression on the attachments.** They are already compressed
     * photographs, and deflating a JPEG costs time to make it very slightly
     * larger.
     */
    suspend fun write(
        target: File,
        source: Source,
        /**
         * Cleared by this function before it returns, on every path.
         *
         * **A `CharArray` rather than a `String` because a String cannot be
         * wiped.** A passphrase that sits in the heap until garbage collection
         * is a passphrase in a heap dump, and this one opens the only copy of
         * somebody's records.
         *
         * **There is no default, and null is not for production.** Passing null
         * writes a plain readable copy of the whole notebook, which no path a
         * person can reach is allowed to do since format version 2. It stays
         * possible only so the container's own tests can build the unencrypted
         * file that [open] must refuse, and a test that builds one has to say
         * so at the call site. A defaulted null would make the dangerous case
         * the one you get by not thinking about it. D67.
         */
        passphrase: CharArray?,
    ): Manifest = withContext(Dispatchers.IO) {
        val plainDatabase = source.database.readBytes()

        // Fresh per file. Reusing a nonce under one key is the mistake that
        // breaks GCM outright rather than merely weakening it.
        val salt = passphrase?.let { ExportCrypto.randomSalt() }
        val nonce = passphrase?.let { ExportCrypto.randomNonce() }
        val key = passphrase?.let { ExportCrypto.derive(it, salt!!) }
        passphrase?.fill('\u0000')

        val databaseBytes = if (key != null) {
            ExportCrypto.encrypt(key, nonce!!, plainDatabase)
        } else {
            plainDatabase
        }
        val manifest = Manifest(
            formatVersion = FORMAT_VERSION,
            appVersion = source.appVersion,
            platform = "android",
            exportedAt = source.exportedAt,
            originDevice = source.originDevice,
            encrypted = key != null,
            encryption = key?.let {
                Encryption(
                    algorithm = "AES-256-GCM",
                    kdf = "Argon2id",
                    iterations = ExportCrypto.ITERATIONS,
                    memoryKib = ExportCrypto.MEMORY_KIB,
                    parallelism = ExportCrypto.PARALLELISM,
                    salt = base64(salt!!),
                    nonce = base64(nonce!!),
                )
            },
            // **Hashed as stored**, meaning the ciphertext when encrypted. The
            // hash exists so a reader can tell a truncated file from a whole
            // one before asking for a passphrase, which it could not do if the
            // hash described bytes it cannot yet see.
            databaseSha256 = Attachments.sha256(databaseBytes),
            databaseBytes = databaseBytes.size.toLong(),
            rowCounts = source.rowCounts,
            attachmentCount = source.attachments.size,
            attachmentBytes = source.attachments.sumOf { it.length() },
            subjectCount = source.subjectCount,
        )

        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toJson().toString(2).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DATABASE))
            zip.write(databaseBytes)
            zip.closeEntry()

            source.attachments.forEach { file ->
                zip.putNextEntry(ZipEntry(ATTACHMENTS + file.name))
                if (key != null) {
                    // The same key, a nonce derived per attachment from the
                    // file's own name, which is its content hash and therefore
                    // unique within the archive. Deriving rather than storing
                    // keeps the manifest a fixed size regardless of how many
                    // attachments there are.
                    zip.write(ExportCrypto.encrypt(key, nonceFor(file.name), file.readBytes()))
                } else {
                    file.inputStream().buffered().use { it.copyTo(zip) }
                }
                zip.closeEntry()
            }
        }
        key?.let { ExportCrypto.wipe(it) }
        manifest
    }

    /**
     * A nonce for one attachment, from its content hash.
     *
     * The file's name **is** its SHA-256, so it is unique within the archive by
     * construction, which is exactly the property a nonce needs. Taking the
     * first twelve bytes of it gives a distinct nonce per attachment without
     * storing one per file in the manifest.
     */
    private fun nonceFor(name: String): ByteArray =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(name.toByteArray())
            .copyOf(ExportCrypto.NONCE_BYTES)

    private fun base64(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun unbase64(text: String): ByteArray =
        android.util.Base64.decode(text, android.util.Base64.NO_WRAP)

    /** Why a container could not be read, in terms a person could be told. */
    sealed interface Problem {
        val message: String

        data class NotAContainer(override val message: String) : Problem
        data class NoManifest(override val message: String) : Problem
        data class FromTheFuture(val found: Int, override val message: String) : Problem
        data class DatabaseMissing(override val message: String) : Problem
        data class DatabaseCorrupt(override val message: String) : Problem
        data class AttachmentCorrupt(val hash: String, override val message: String) : Problem
        data class RowCountsDisagree(override val message: String) : Problem

        /**
         * The database holds a table or column this build does not know.
         *
         * **Distinct from [FromTheFuture] on purpose.** A file whose manifest
         * claims a later format is a file this app has simply not caught up
         * with, and the answer is to update the app. A file whose manifest
         * claims *this* format while carrying a shape this format does not have
         * is a file that has been altered, and telling somebody to update the
         * app would send them somewhere that cannot help.
         */
        data class UnknownSchema(
            val what: String,
            override val message: String,
        ) : Problem

        /**
         * The database points at an attached file the archive does not carry.
         *
         * Caught here rather than at use, because finding it later means
         * finding it after the import said it worked, and by then the notebook
         * has a photograph of a discharge letter that opens onto nothing.
         */
        data class AttachmentMissing(
            val hash: String,
            override val message: String,
        ) : Problem

        /** Encrypted, and no passphrase was offered. Not a failure, a question. */
        data class PassphraseNeeded(override val message: String) : Problem

        /**
         * An unencrypted export, which this app no longer writes or accepts.
         *
         * **The message says what the file is rather than that it failed.**
         * Somebody holding one is holding a complete, readable copy of their
         * own care record, and the useful thing to tell them is that, plus what
         * to do instead. "Unsupported format" would be true and useless. D67.
         */
        data class NotEncrypted(override val message: String) : Problem

        /**
         * Decrypted cleanly, and what came out is not a database.
         *
         * **This is the pre-portability export, and it is a real file somebody
         * may be holding.** Until 2026-08-02 the archive carried the SQLCipher
         * file exactly as it sat on disk, keyed by 32 random bytes wrapped by
         * the writing phone's Keystore, which cannot be exported. Such a file
         * opens correctly, authenticates correctly, and yields a payload no
         * other device can read, so without this check it would fail later as
         * "damaged" and send somebody looking for a corruption that is not
         * there. D61 and D67.
         */
        data class NotPortable(override val message: String) : Problem

        /**
         * The payload did not authenticate.
         *
         * **This cannot distinguish a wrong passphrase from a tampered file**,
         * because GCM cannot: both mean the tag did not verify. The message
         * therefore says both and claims neither. Telling somebody their file
         * is corrupt when they mistyped is as bad as the reverse.
         */
        data class CouldNotDecrypt(override val message: String) : Problem
    }

    data class Opened(
        val manifest: Manifest,
        val database: File,
        val attachments: List<File>,
    )

    /**
     * Reads a container into a staging directory, or says why it cannot.
     *
     * **Nothing is applied here.** Reading and importing are separate because
     * the import has to be atomic: it fully succeeds or it changes nothing, and
     * a partially restored state that looks complete is worse than a clean
     * failure, because the person stops worrying.
     *
     * Every check the format's section 7 names happens here, before a single
     * row is touched, and each one names what was wrong rather than reporting a
     * generic failure.
     */
    suspend fun open(
        file: File,
        staging: File,
        /** Cleared before this function returns, on every path. */
        passphrase: CharArray? = null,
        /**
         * The shape to check the payload against, or null to skip that check.
         *
         * Null is for callers holding a file rather than an app, such as the
         * container's own tests, which build payloads that are not databases at
         * all. A real import always passes one.
         */
        expected: Schema? = null,
    ): Result<Opened> = withContext(Dispatchers.IO) {
        staging.mkdirs()

        var manifestJson: JSONObject? = null
        val attachments = mutableListOf<File>()
        val database = File(staging, DATABASE)

        try {
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == MANIFEST -> manifestJson = JSONObject(zip.readText())
                        entry.name == DATABASE -> database.outputStream().use { zip.copyTo(it) }
                        entry.name.startsWith(ATTACHMENTS) && !entry.isDirectory -> {
                            val out = File(staging, entry.name.removePrefix(ATTACHMENTS))
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                            attachments += out
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (t: Throwable) {
            return@withContext failure(
                Problem.NotAContainer(
                    "This file could not be opened as a Health Trail export. " +
                        "It may be truncated or it may be a different kind of file."
                )
            )
        }

        val json = manifestJson ?: return@withContext failure(
            Problem.NoManifest(
                "This file has no manifest, so there is no way to tell what it holds. " +
                    "Health Trail exports always carry one."
            )
        )

        val manifest = Manifest.from(json)

        // Read before anything else, and refuse a version we do not understand
        // rather than guessing. This costs nothing now and is unfixable later.
        if (manifest.formatVersion > FORMAT_VERSION) {
            return@withContext failure(
                Problem.FromTheFuture(
                    manifest.formatVersion,
                    "This export was written by a newer version of Health Trail. " +
                        "It is format ${manifest.formatVersion} and this app understands " +
                        "up to $FORMAT_VERSION. Update the app and try again.",
                )
            )
        }

        if (!database.isFile) {
            return@withContext failure(
                Problem.DatabaseMissing("This export has a manifest but no records in it.")
            )
        }

        val actual = Attachments.sha256(database.readBytes())
        if (actual != manifest.databaseSha256) {
            return@withContext failure(
                Problem.DatabaseCorrupt(
                    "The records in this export do not match what its manifest says they " +
                        "should be. The file was probably damaged in transit. Nothing was " +
                        "changed."
                )
            )
        }

        // **An unencrypted export is refused, whatever version wrote it.**
        //
        // A version 1 file that carries a passphrase is still a real backup and
        // is still read, because refusing one would destroy somebody's only
        // recovery path in order to make a point about a format number. What is
        // refused is the plain file, and it is refused by what it is rather
        // than by what wrote it, so a hand assembled one is caught too. D67.
        if (!manifest.encrypted) {
            passphrase?.fill('\u0000')
            return@withContext failure(
                Problem.NotEncrypted(
                    "This file is an unencrypted Health Trail export, which means it is a " +
                        "complete and readable copy of the notebook: every call, every note, " +
                        "every medication, and every bill. Older versions of the app could " +
                        "write one. This version will not open one, because a file like this " +
                        "in a folder that syncs somewhere is the whole record leaving the " +
                        "phone. Open it on the version that wrote it and save a new export " +
                        "with a passphrase. Nothing was changed."
                )
            )
        }

        // Decrypted after the hash check and before the attachment checks,
        // because the hash describes the bytes as stored and the attachment
        // hashes describe them as written. Doing it in the other order would
        // fail every attachment on a perfectly good encrypted file.
        run {
            val parameters = manifest.encryption ?: return@withContext failure(
                Problem.CouldNotDecrypt(
                    "This export says it is encrypted but does not record how, so there " +
                        "is no way to open it. Nothing was changed."
                )
            )
            val offered = passphrase ?: return@withContext failure(
                Problem.PassphraseNeeded(
                    "This export is encrypted. It needs the passphrase that was chosen " +
                        "when it was made. There is no way to recover it if it is lost."
                )
            )

            val key = ExportCrypto.derive(
                passphrase = offered,
                salt = unbase64(parameters.salt),
                iterations = parameters.iterations,
                memoryKib = parameters.memoryKib,
                parallelism = parameters.parallelism,
            )
            offered.fill('\u0000')

            try {
                val nonce = unbase64(parameters.nonce)
                database.writeBytes(ExportCrypto.decrypt(key, nonce, database.readBytes()))
                attachments.forEach {
                    it.writeBytes(ExportCrypto.decrypt(key, nonceFor(it.name), it.readBytes()))
                }
            } catch (_: Throwable) {
                return@withContext failure(
                    Problem.CouldNotDecrypt(
                        "This export could not be opened with that passphrase. Either the " +
                            "passphrase is wrong or the file has been altered since it was " +
                            "made, and there is no way to tell which from here. Nothing " +
                            "was changed."
                    )
                )
            } finally {
                ExportCrypto.wipe(key)
            }

            // **What came out has to be a database.** An export written before
            // 2026-08-02 decrypts perfectly and yields a SQLCipher file keyed
            // to the phone that wrote it, which nothing else can open. Without
            // this it fails two checks later as "damaged", sending somebody to
            // hunt a corruption that is not there, on the one file standing
            // between them and losing the record. D61 and D67.
            if (!isSqlite(database.readBytes())) {
                return@withContext failure(
                    Problem.NotPortable(
                        "This export opened with that passphrase, so the passphrase is " +
                            "right and the file is not damaged. It was written by a " +
                            "version of Health Trail whose exports could only be opened " +
                            "by the phone that made them. If that phone still works, open " +
                            "it there and save a new export, which will be readable " +
                            "anywhere. Nothing was changed."
                    )
                )
            }
        }

        // An attachment is named by its content hash, so a name that does not
        // match its bytes is corruption by definition. Checked here rather than
        // at use, because finding it later means finding it after the import
        // said it worked.
        attachments.forEach { attachment ->
            if (Attachments.sha256(attachment.readBytes()) != attachment.name) {
                return@withContext failure(
                    Problem.AttachmentCorrupt(
                        attachment.name,
                        "One of the attached files is damaged. Nothing was changed.",
                    )
                )
            }
        }

        // The payload is a plain SQLite file, which is what makes it portable,
        // and that is also what makes these last two checks possible at all.
        // While the archive carried a device keyed SQLCipher file there was no
        // way to look inside one without the key that could not travel.
        if (expected != null) {
            inspect(database, attachments.map { it.name }.toSet(), expected)
                ?.let { return@withContext failure<Opened>(it) }
        }

        Result.success(Opened(manifest, database, attachments))
    }

    /**
     * The sixteen byte header every plain SQLite file begins with.
     *
     * `PortabilityTest` asserts an export decrypts to one of these and that the
     * live database does not, so the check cannot pass by going vacuous.
     */
    private const val SQLITE_MAGIC = "SQLite format 3\u0000"

    /**
     * True when these bytes are an ordinary SQLite database.
     *
     * **This is the whole portability question in one line.** A SQLCipher file
     * is encrypted from its first byte and has no recognizable header, so the
     * absence of this magic is exactly what an export written before
     * 2026-08-02 looks like from the outside. D61.
     */
    private fun isSqlite(bytes: ByteArray): Boolean =
        bytes.size >= 16 &&
            String(bytes, 0, 16, Charsets.ISO_8859_1) == SQLITE_MAGIC

    /**
     * The last two checks the format's section 7 asks for, both of which have
     * to read the database rather than the envelope.
     *
     * Returns the problem, or null when the file is sound.
     */
    private fun inspect(
        database: File,
        present: Set<String>,
        expected: Schema,
    ): Problem? {
        val db = try {
            SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (_: Throwable) {
            return Problem.DatabaseCorrupt(
                "The records in this export could not be read at all. The file was " +
                    "probably damaged in transit. Nothing was changed."
            )
        }

        return db.use { open ->
            unknownShape(open, expected) ?: missingAttachment(open, present)
        }
    }

    /**
     * A table or column this build does not have, per section 7.
     *
     * **Checked against the schema this build actually created**, passed in by
     * the caller, rather than a list of tables written out a second time in
     * Kotlin. That duplication is what D16 exists to prevent, and it would go
     * stale the first time a table was added.
     */
    private fun unknownShape(open: SQLiteDatabase, expected: Schema): Problem? {
        open.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
            null,
        ).use { tables ->
            while (tables.moveToNext()) {
                val table = tables.getString(0)
                if (table in PLATFORM_TABLES) continue
                val columns = expected[table]
                    ?: return Problem.UnknownSchema(
                        table,
                        "This export holds a kind of record this version of Health Trail " +
                            "does not have, called \"$table\". It says it is a format this " +
                            "app understands, so it has been altered since it was made. " +
                            "Nothing was changed.",
                    )

                open.rawQuery("PRAGMA table_info(\"$table\")", null).use { info ->
                    val nameColumn = info.getColumnIndexOrThrow("name")
                    while (info.moveToNext()) {
                        val column = info.getString(nameColumn)
                        if (column !in columns) {
                            return Problem.UnknownSchema(
                                "$table.$column",
                                "This export holds a detail this version of Health Trail " +
                                    "does not have, called \"$column\" on \"$table\". It " +
                                    "says it is a format this app understands, so it has " +
                                    "been altered since it was made. Nothing was changed.",
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * An attachment the database points at that the archive does not carry.
     *
     * **Only rows that are not tombstoned.** A deleted attachment's bytes are
     * legitimately gone while its row still travels, because the row is how a
     * restore learns the deletion happened at all. Requiring the file for a row
     * that records its own removal would reject every export taken after
     * somebody deleted a photograph.
     */
    private fun missingAttachment(open: SQLiteDatabase, present: Set<String>): Problem? {
        val hasAttachments = open.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'attachment'",
            null,
        ).use { it.moveToFirst() }
        if (!hasAttachments) return null

        open.rawQuery(
            // allow-base-table: an export carries tombstones, and the live view
            // would hide exactly the rows whose files are allowed to be absent.
            "SELECT sha256 FROM attachment WHERE deleted_at IS NULL",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val hash = cursor.getString(0)
                if (hash !in present) {
                    return Problem.AttachmentMissing(
                        hash,
                        "This export refers to an attached file that is not in it. The " +
                            "file was probably not copied completely. Nothing was changed.",
                    )
                }
            }
        }
        return null
    }

    /**
     * Tables the platform creates for itself, which are not records.
     *
     * Android's SQLite writes `android_metadata` into every database it makes,
     * to hold the locale. It is nobody's record and it is not in the contract,
     * so it is skipped rather than treated as an alteration. Leaving it to the
     * expectation to happen to contain it would work today and break the first
     * time an expectation is built any other way.
     */
    private val PLATFORM_TABLES = setOf("android_metadata")

    private fun <T> failure(problem: Problem): Result<T> =
        Result.failure(ExportProblem(problem))

    /** Carries a [Problem] through a `Result` without losing which one it was. */
    class ExportProblem(val problem: Problem) : Exception(problem.message)

    private fun Manifest.toJson(): JSONObject = JSONObject().apply {
        put("format_version", formatVersion)
        put("app_version", appVersion)
        put("platform", platform)
        put("exported_at", exportedAt)
        put("origin_device", originDevice)
        put("encrypted", encrypted)
        encryption?.let {
            put(
                "encryption",
                JSONObject().apply {
                    put("algorithm", it.algorithm)
                    put("kdf", it.kdf)
                    put("kdf_iterations", it.iterations)
                    put("kdf_memory_kib", it.memoryKib)
                    put("kdf_parallelism", it.parallelism)
                    put("salt", it.salt)
                    put("nonce", it.nonce)
                },
            )
        }
        put(
            "database",
            JSONObject().apply {
                put("sha256", databaseSha256)
                put("byte_size", databaseBytes)
                put("schema_version", FORMAT_VERSION)
                put("row_counts", JSONObject(rowCounts.toMap()))
            },
        )
        put(
            "attachments",
            JSONObject().apply {
                put("count", attachmentCount)
                put("total_bytes", attachmentBytes)
            },
        )
        put("subject_count", subjectCount)
    }

    private fun ZipInputStream.readText(): String = readBytes().decodeToString()

    private fun InputStream.copyTo(out: java.io.OutputStream) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
    }
}

/** Reads a manifest back out of its JSON. */
internal fun ExportContainer.Manifest.Companion.from(json: JSONObject): ExportContainer.Manifest {
    val database = json.optJSONObject("database") ?: JSONObject()
    val attachments = json.optJSONObject("attachments") ?: JSONObject()
    val counts = database.optJSONObject("row_counts") ?: JSONObject()
    return ExportContainer.Manifest(
        formatVersion = json.optInt("format_version", 0),
        appVersion = json.optString("app_version"),
        platform = json.optString("platform"),
        exportedAt = json.optLong("exported_at"),
        originDevice = json.optString("origin_device"),
        encrypted = json.optBoolean("encrypted", false),
        encryption = json.optJSONObject("encryption")?.let {
            ExportContainer.Encryption(
                algorithm = it.optString("algorithm"),
                kdf = it.optString("kdf"),
                iterations = it.optInt("kdf_iterations"),
                memoryKib = it.optInt("kdf_memory_kib"),
                parallelism = it.optInt("kdf_parallelism"),
                salt = it.optString("salt"),
                nonce = it.optString("nonce"),
            )
        },
        databaseSha256 = database.optString("sha256"),
        databaseBytes = database.optLong("byte_size"),
        rowCounts = counts.keys().asSequence().associateWith { counts.optInt(it) },
        attachmentCount = attachments.optInt("count"),
        attachmentBytes = attachments.optLong("total_bytes"),
        subjectCount = json.optInt("subject_count"),
    )
}
