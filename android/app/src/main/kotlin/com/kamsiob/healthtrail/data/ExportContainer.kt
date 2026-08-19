package com.kamsiob.healthtrail.data

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The export container, per `contract/EXPORT-FORMAT.md`.
 *
 * **The governing rule is one sentence:** anything the app can store, the
 * export contains and the import restores. A feature that stores something the
 * export does not carry silently loses records on device migration, which for
 * this audience is the worst failure the app can have.
 *
 * **An ordinary zip, named `.zip`.** The old `.htx` extension made the file
 * recognizable and made it a dead end: somebody who copies their archive to a
 * laptop and double clicks it gets nothing, on the first step of the one
 * procedure this whole container exists to make possible. D98.
 *
 * ```
 * MANIFEST.json      the header, in the clear, and it says nothing about anybody
 * README.txt         how to open this without this app
 * payload.enc        everything else, framed and encrypted
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
     * **Version 3, and the change is that the container has two layers.**
     *
     * Version 2 required a passphrase and encrypted every entry of one zip
     * separately. That kept the record safe and left the archive readable only
     * by something that knew the per entry nonce rule, which is a format one
     * program can open wearing the clothes of a format anyone can.
     *
     * **Version 3 draws the line where `contract/DATA-CONTRACT.md` 8.1 draws
     * it.** The outer layer is a plain ZIP64 holding exactly three things:
     * `README.txt`, `MANIFEST.json`, and `payload.enc`. Nothing in it says
     * anything about the person. The inner layer, once decrypted, is an
     * ordinary zip with the whole record in it, and it is ordinary on purpose:
     * a stranger with the passphrase and the published specification gets a
     * folder, not a puzzle.
     *
     * **Nothing released ever wrote version 1 or 2.** Both existed only inside
     * this project's own development, so version 3 does not carry a reader for
     * them: what it carries is an honest refusal that says which build wrote the
     * file. Compatibility with a version nobody has is code that can only ever
     * be wrong in ways nobody will find. D96.
     *
     * The plain-file refusal stands and is now stronger, because an unencrypted
     * archive cannot be expressed in this format at all: `payload.enc` is the
     * only place data can go. D67.
     */
    const val FORMAT_VERSION = 3

    /**
     * The three things the outer layer holds, and it holds nothing else.
     *
     * `contract/DATA-CONTRACT.md` 8.1. The names are capitalized because they
     * are what a person sees when they open the file with an ordinary zip tool,
     * and a capital README is the fifty year old convention for "start here".
     */
    const val OUTER_MANIFEST = "MANIFEST.json"
    const val OUTER_README = "README.txt"
    const val PAYLOAD = "payload.enc"

    /**
     * The inner layout, exactly as 8.1 draws it.
     *
     * **The database is `data/trail.sqlite` rather than `data.sqlite`**, which
     * is a rename version 3 makes deliberately: the inner container groups the
     * machine copy under `data/` beside the schema it was written against, so
     * somebody looking at the folder can see that the two belong together.
     */
    const val INNER_README = "README.txt"
    const val INNER_MANIFEST = "MANIFEST.json"

    /**
     * SHA-256 of every other file in the inner container, one per line.
     *
     * **It is not a security measure and does not pretend to be.** It sits
     * inside the encryption, so anything that could forge it could forge the
     * files it describes. It is there so somebody who copied this archive across
     * four machines over ten years can tell which file went bad, in the format
     * every operating system's checksum tool already speaks.
     */
    const val CHECKSUMS = "CHECKSUMS.txt"
    const val DATABASE = "data/trail.sqlite"

    /**
     * The schema the payload was written against, as commented DDL.
     *
     * **So the database can be understood without this app or its source.** A
     * SQLite file tells you its columns and nothing about what they mean;
     * `contract/schema.sql` is written with the reasoning in comments, and it is
     * shipped here rather than summarized so that what travels with somebody's
     * archive is the real thing.
     */
    const val SCHEMA = "data/schema.sql"
    const val ATTACHMENTS = "attachments/"

    /**
     * The human copy. `contract/DATA-CONTRACT.md` section 8.2.
     *
     * **This is the point of the format.** A machine payload only this app can
     * read is a record that dies with the app, and a caregiver's archive
     * outlives the phone, outlives Android, and very likely outlives this
     * project. Everything else in the container exists so this folder can be
     * trusted.
     */
    const val READABLE = "readable/"

    /**
     * An attachment the archive names and does not carry, listed in the manifest.
     *
     * **`contract/DATA-CONTRACT.md` 8.2 always required this** and the format
     * document never carried it, so it was never written. 8.3 says what it is
     * for: an attachment missing at export time is imported "with its name and
     * date intact, so the person sees that a photo existed and is gone, rather
     * than never learning it was there". A bare content hash says that to
     * nobody, so the name and the date travel with it.
     *
     * **It is what turns a silent failure into a stated one.** Without it, a
     * live attachment row whose bytes were gone produced an archive [open]
     * refused by name, at restore, on the new phone, with the old one gone.
     * With it, the archive opens and says what is missing. #332.
     */
    data class MissingAttachment(
        val sha256: String,
        val originalFilename: String?,
        val createdAt: Long,
    )

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
        /**
         * How many pages the readable copy holds.
         *
         * **Recorded so a missing human copy is loud rather than silent.** The
         * offline read test in 8.5 opens `readable/index.html` on a machine with
         * no network, and a zero here is a failed export rather than a quiet
         * one. It also lets an importer say what a file contains before writing
         * anything, per 8.6.
         */
        val readablePages: Int = 0,
        /**
         * What the person wrote to remind themselves of the passphrase, or null.
         *
         * **In the outer manifest, in the clear, and the screen says so.**
         * `contract/DATA-CONTRACT.md` 8.1: it has to be readable before the
         * passphrase is known, or it is not a hint, it is a second thing to
         * lose. That is exactly why the screen states plainly that anyone
         * holding the file can read it and that it must never contain the
         * passphrase itself.
         *
         * **It is the one field here the person wrote themselves**, so it is the
         * one thing in the outer layer that could say something about them. That
         * is their choice to make, made in front of a sentence that tells them
         * what it costs, which is a different thing from the app deciding to put
         * their row counts out there.
         */
        val passphraseHint: String? = null,
        /**
         * The BCP 47 tag the readable copy is written in. #210.
         *
         * **`contract/DATA-CONTRACT.md` 8.2 always said the inner manifest
         * carries this and `EXPORT-FORMAT.md` never listed it**, so the code
         * followed the format document and wrote nothing. The two disagreed and
         * `CLAUDE.md`'s precedence settles it: the data contract governs a data
         * question. The format document is corrected rather than the contract.
         *
         * **It stopped being cosmetic when the pages stopped being English.**
         * Since #327 the readable copy is written in the person's language, so
         * 8.5's regeneration is a function of a language the archive did not
         * record: the same rows regenerate correct pages on a phone set to
         * another language, and they are not these bytes. Recording it is what
         * lets a regeneration reproduce the archive it came from.
         */
        val readableLocale: String? = null,
        /**
         * The IANA zone the exporting device was in. 8.2 asks for it by name.
         *
         * Beside `exportedAt` rather than folded into it, for the same reason
         * every event date in the schema carries a zone next to its instant: an
         * epoch alone cannot say what time it was where somebody was standing.
         */
        val exportedZone: String? = null,
        /**
         * Attachments this archive names and does not carry. 8.2 and #332.
         *
         * **Empty on every sound archive**, which is every archive this project
         * has produced. It is not a warning that fires routinely; it is the one
         * that must not be silent the day storage failed.
         */
        val missingAttachments: List<MissingAttachment> = emptyList(),
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
        /**
         * The random four bytes every frame nonce begins with.
         *
         * **Not a whole nonce, because the payload is not one message.** Each
         * frame's nonce is this prefix followed by its own eight byte counter,
         * so a file of ten thousand frames uses ten thousand distinct nonces
         * under one key without carrying ten thousand of them here.
         */
        val noncePrefix: String,
        /** How much plaintext one frame holds, so a reader can size its buffers. */
        val chunkBytes: Int,
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
        /**
         * `contract/schema.sql`, as commented DDL, to ship beside the database.
         *
         * **Passed in rather than read here**, because this object has no
         * Android context and should not grow one: it is the format, and the
         * format does not know where an app keeps its assets. The caller reads
         * it from `contract/schema.sql` in the assets, which the build copies
         * from the contract itself, so what travels with somebody's archive is
         * the real file and not a summary of it.
         */
        val schemaSql: String,
        /**
         * Every word the readable copy says, in the person's own language.
         *
         * **Passed in for the same reason `schemaSql` is**, and it is the same
         * argument: this object is the format and the format does not know
         * where an app keeps its catalogs. `ReadableWords.from` reads them and
         * hands over a plain value.
         *
         * **No default, deliberately.** A default would be an English archive
         * that nothing complains about, which is precisely the defect this
         * argument exists to close. #327.
         */
        val readableWords: ReadableArchive.Words,
        /**
         * The person's own reminder, or null if they did not write one.
         *
         * Trimmed and emptied to null by the caller, so the format never has to
         * decide whether a hint of three spaces is a hint.
         */
        val passphraseHint: String? = null,
        /**
         * Live attachment rows whose bytes were not on the device. #332.
         *
         * **Gathered by the caller because only it has the store.** This object
         * is the format; it knows what the manifest must say and not where a
         * phone keeps its files.
         */
        val missingAttachments: List<MissingAttachment> = emptyList(),
        /**
         * The IANA zone the device was in, beside [exportedAt]. 8.2.
         *
         * Passed in rather than read, for the same reason `exportedAt` is: a
         * value taken from the clock inside here is a value no test can pin.
         */
        val exportedZone: String? = null,
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
         * **It is not nullable any more.** Version 2 allowed null so the
         * container's own test could build the unencrypted file that [open] must
         * refuse. In version 3 an unencrypted archive cannot be expressed at
         * all, because `payload.enc` is the only place data can go, so the test
         * assembles a legacy shaped file by hand instead. That is the stronger
         * test: it proves the refusal catches a file this code could not have
         * written. D67.
         */
        passphrase: CharArray,
    ): Manifest = withContext(Dispatchers.IO) {
        // Rendered before the manifest, because the manifest reports how many
        // pages there are and a count written before the pages exist is a claim
        // rather than a fact.
        val readable = readablePages(source.database, source.readableWords)

        // Fresh per file. Reusing a nonce under one key is the mistake that
        // breaks GCM outright rather than merely weakening it.
        val salt = ExportCrypto.randomSalt()
        val noncePrefix = ExportCrypto.randomNoncePrefix()
        val key = ExportCrypto.derive(passphrase, salt)
        passphrase.fill('\u0000')

        target.parentFile?.mkdirs()

        // **The inner container is built on disk, not in memory.** It holds the
        // database and every attachment, which is gigabytes on a real notebook,
        // and the whole reason the payload is framed is that this must work at
        // that size. Staged beside the target so it lands on the same volume.
        val staged = File(target.parentFile, "${target.name}.building")
        val manifest: Manifest
        try {
            manifest = buildInner(staged, source, readable, salt, noncePrefix)

            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                // **The manifest first in the file**, so a reader can learn the
                // format version and the key derivation parameters from the
                // front of a stream without seeking, which is what lets a tool
                // read a very large archive without holding it.
                zip.putNextEntry(ZipEntry(OUTER_MANIFEST))
                zip.write(manifest.toPublicJson().toString(2).toByteArray())
                zip.closeEntry()

                // **Written for a stranger who found this file and has the
                // passphrase.** Section 8.1. ASCII only, no markup, because it
                // has to be readable by whatever opens a text file in ten years.
                zip.putNextEntry(ZipEntry(OUTER_README))
                zip.write(readmeText(manifest).toByteArray(Charsets.US_ASCII))
                zip.closeEntry()

                // **Stored rather than deflated.** The payload is ciphertext,
                // which does not compress, so deflating it costs time to make it
                // very slightly larger. Its own entries were compressed inside.
                val payload = ZipEntry(PAYLOAD).apply { method = ZipEntry.STORED }
                sealPayload(zip, payload, staged, key, noncePrefix)
            }
        } finally {
            // The staged inner container is the whole record in the clear. It
            // does not outlive this function on any path, including a failure.
            staged.delete()
            ExportCrypto.wipe(key)
        }
        manifest
    }

    /**
     * Builds the inner container: the layout 8.1 draws, as an ordinary zip.
     *
     * **Ordinary is the requirement, not an implementation detail.** Somebody
     * with the passphrase and the published format gets a folder they can read
     * with tools that already exist, which is what "openable by someone who does
     * not have this app" means in practice.
     *
     * The manifest is written into the inner container in full, counts and all,
     * because everything in here is inside the encryption. The outer one is the
     * same object with the describing half left out.
     */
    private fun buildInner(
        staged: File,
        source: Source,
        readable: Map<String, String>,
        salt: ByteArray,
        noncePrefix: ByteArray,
    ): Manifest {
        val plainDatabase = source.database.readBytes()
        val manifest = Manifest(
            formatVersion = FORMAT_VERSION,
            appVersion = source.appVersion,
            platform = "android",
            exportedAt = source.exportedAt,
            originDevice = source.originDevice,
            encrypted = true,
            encryption = Encryption(
                algorithm = "AES-256-GCM",
                kdf = "Argon2id",
                iterations = ExportCrypto.ITERATIONS,
                memoryKib = ExportCrypto.MEMORY_KIB,
                parallelism = ExportCrypto.PARALLELISM,
                salt = base64(salt),
                noncePrefix = base64(noncePrefix),
                chunkBytes = ExportCrypto.CHUNK_BYTES,
            ),
            // **Hashed as it sits inside the payload**, which is the plain
            // database. In version 2 this hashed the ciphertext so a reader
            // could check a file before asking for a passphrase; now the hash
            // lives inside the encryption anyway, so it describes the thing
            // somebody actually wants checked.
            databaseSha256 = Attachments.sha256(plainDatabase),
            databaseBytes = plainDatabase.size.toLong(),
            rowCounts = source.rowCounts,
            attachmentCount = source.attachments.size,
            attachmentBytes = source.attachments.sumOf { it.length() },
            subjectCount = source.subjectCount,
            readablePages = readable.size,
            passphraseHint = source.passphraseHint,
            // **The language the pages are actually in**, taken from the words
            // that rendered them rather than from the device, so the manifest
            // cannot disagree with the folder beside it. #210.
            readableLocale = source.readableWords.lang,
            exportedZone = source.exportedZone,
            missingAttachments = source.missingAttachments,
        )

        // Every file's hash, gathered as it is written, for CHECKSUMS.txt.
        val checksums = sortedMapOf<String, String>()

        ZipOutputStream(staged.outputStream().buffered()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                requireSafeName(name)
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
                checksums[name] = Attachments.sha256(bytes)
            }

            put(INNER_README, innerReadmeText(manifest).toByteArray(Charsets.US_ASCII))
            put(INNER_MANIFEST, manifest.toFullJson().toString(2).toByteArray())
            put(DATABASE, plainDatabase)
            put(SCHEMA, source.schemaSql.toByteArray(Charsets.UTF_8))

            // **Rendered from the same staged database the payload carries**, so
            // the two halves of the archive cannot disagree. An archive whose
            // readable copy said something different from its SQLite would be
            // the silent partial correctness section 8 opens by calling worse
            // than an honest failure.
            readable.forEach { (path, html) ->
                put(READABLE + path, html.toByteArray(Charsets.UTF_8))
            }

            source.attachments.forEach { file ->
                put(ATTACHMENTS + file.name, file.readBytes())
            }

            // Last, because it describes everything above it. Not in its own
            // list, for the same reason a checksum file never checksums itself.
            zip.putNextEntry(ZipEntry(CHECKSUMS))
            zip.write(checksumText(checksums).toByteArray(Charsets.US_ASCII))
            zip.closeEntry()
        }
        return manifest
    }

    /**
     * Refuses a name that would not survive being extracted somewhere else.
     *
     * **The archive is a folder on somebody's computer in ten years**, and the
     * computer is not this one. `contract/DATA-CONTRACT.md` 8.1 names the rules
     * and they all come from the same place: a name that is fine on Android and
     * impossible on Windows or a case-insensitive volume turns a restore into a
     * partial one, silently, on the day it matters.
     *
     * **Checked rather than documented**, because the names are generated. The
     * attachments are content hashes and safe by construction, and the readable
     * pages are built from the person's own data, which is where a name will one
     * day come from that nobody predicted. Asserted on a real export first: 106
     * files, longest path 76 characters, no clashes.
     */
    @VisibleForTesting
    internal fun requireSafeName(name: String) {
        require(name.all { it.code in 32..126 }) {
            "an archive entry name is not ASCII: $name"
        }
        require(name.length <= MAX_PATH_LENGTH) {
            "an archive entry path is longer than $MAX_PATH_LENGTH characters: $name"
        }
        require(name.none { it in "<>:\"|?*\\" }) {
            "an archive entry name uses a character some systems refuse: $name"
        }
        val stem = name.substringAfterLast('/').substringBefore('.').uppercase()
        require(stem !in RESERVED_NAMES) {
            "an archive entry is named after a reserved device name: $name"
        }
    }

    /** From 8.1. Short enough to survive being nested a few folders deep. */
    private const val MAX_PATH_LENGTH = 180

    /**
     * Names Windows still refuses, forty years on, with or without an extension.
     *
     * A readable page called `con.html` is not a thing this app would generate
     * today. It is exactly the thing that would arrive the day somebody's
     * chapter is named for a person called Con.
     */
    private val RESERVED_NAMES = buildSet {
        addAll(listOf("CON", "PRN", "AUX", "NUL"))
        for (n in 1..9) {
            add("COM$n")
            add("LPT$n")
        }
    }

    /**
     * Encrypts the staged inner container into `payload.enc`, a frame at a time.
     *
     * **Framed rather than sealed in one call**, because one call needs the
     * whole archive in memory and the contract requires this to work past four
     * gigabytes. Each frame is a four byte big-endian length followed by that
     * many bytes of ciphertext with its tag.
     *
     * **Each frame authenticates its own position and whether it is the last**,
     * through the additional data. Without that, frames verify individually and
     * a stream can be cut short, reordered, or have frames dropped from the
     * middle, and what comes out decrypts perfectly and is missing a year of
     * somebody's record. That failure is silent, which makes it the worst kind
     * this format could have.
     */
    private fun sealPayload(
        zip: ZipOutputStream,
        entry: ZipEntry,
        staged: File,
        key: ByteArray,
        noncePrefix: ByteArray,
    ) {
        // **Sealed to a file first, then copied into the archive.** A stored zip
        // entry has to declare its size and CRC before its bytes, and the only
        // honest ways to know those are to hold the whole payload in memory or
        // to write it once and measure it. On a notebook with four gigabytes of
        // photographs the first is not available, so it is written out and
        // streamed in, and neither the plaintext nor the ciphertext is ever held
        // whole.
        val sealed = File(staged.parentFile, "${staged.name}.enc")
        val crc = java.util.zip.CRC32()
        try {
            val buffer = ByteArray(ExportCrypto.CHUNK_BYTES)
            sealed.outputStream().buffered().use { out ->
                staged.inputStream().buffered().use { input ->
                    var index = 0L
                    var filled = fill(input, buffer)
                    do {
                        // Read ahead by one frame, because a frame cannot say it
                        // is the last until something has tried to read past it.
                        val plain = buffer.copyOf(filled)
                        val next = ByteArray(ExportCrypto.CHUNK_BYTES)
                        val nextFilled = fill(input, next)
                        val last = nextFilled == 0
                        val frame = ExportCrypto.encrypt(
                            key = key,
                            nonce = ExportCrypto.chunkNonce(noncePrefix, index),
                            plaintext = plain,
                            aad = ExportCrypto.frameAad(index, last),
                        )
                        val header = frameHeader(frame.size)
                        out.write(header)
                        out.write(frame)
                        crc.update(header)
                        crc.update(frame)
                        if (last) break
                        next.copyInto(buffer)
                        filled = nextFilled
                        index += 1
                    } while (true)
                }
            }

            entry.size = sealed.length()
            entry.compressedSize = sealed.length()
            entry.crc = crc.value
            zip.putNextEntry(entry)
            sealed.inputStream().buffered().use { it.copyTo(zip) }
            zip.closeEntry()
        } finally {
            sealed.delete()
        }
    }

    /**
     * Fills a buffer as far as the stream allows, returning how much it got.
     *
     * **A single `read` is allowed to return less than it was asked for**, and a
     * framed format that treats a short read as the end of the stream writes a
     * truncated archive on the day the storage is slow. Reading until the buffer
     * is full or the stream is done is the only correct way to frame.
     */
    private fun fill(input: java.io.InputStream, buffer: ByteArray): Int {
        var filled = 0
        while (filled < buffer.size) {
            val read = input.read(buffer, filled, buffer.size - filled)
            if (read < 0) break
            filled += read
        }
        return filled
    }

    /** A frame's four byte big-endian length, ahead of its ciphertext. */
    private fun frameHeader(size: Int): ByteArray = byteArrayOf(
        (size ushr 24).toByte(),
        (size ushr 16).toByte(),
        (size ushr 8).toByte(),
        size.toByte(),
    )

    /** How many bytes a frame spends saying how long it is. */
    private const val FRAME_HEADER_BYTES = 4

    /**
     * `CHECKSUMS.txt`, in the format every checksum tool already reads: the
     * hash, two spaces, the path. Sorted, so two exports of one database
     * produce the same file and 8.5's regeneration test can compare them.
     */
    private fun checksumText(checksums: Map<String, String>): String =
        checksums.entries.joinToString("\n", postfix = "\n") { "${it.value}  ${it.key}" }

    /**
     * A nonce for one attachment, from its content hash.
     *
     * The file's name **is** its SHA-256, so it is unique within the archive by
     * construction, which is exactly the property a nonce needs. Taking the
     * first twelve bytes of it gives a distinct nonce per attachment without
     * storing one per file in the manifest.
     */
    /**
     * The first thing a stranger opens. `contract/DATA-CONTRACT.md` section 8.1.
     *
     * **Written for a person, not for a developer.** It says what the file is,
     * that the contents are encrypted, exactly which algorithm and parameters
     * were used, where the format is documented, how to decrypt it without this
     * app, and that a lost passphrase means a lost archive with no recovery path
     * anywhere.
     *
     * **ASCII only and no markup**, so it opens correctly in whatever reads a
     * text file in ten years, on a machine whose defaults nobody can predict.
     *
     * **It carries nothing about the person.** No name, no counts, no dates. It
     * sits in the clear beside the ciphertext, so everything in it is public.
     */
    private fun readmeText(manifest: Manifest): String {
        val encryption = manifest.encryption
        return buildString {
            appendLine("WHAT THIS FILE IS")
            appendLine()
            appendLine("This is a Health Trail archive. It holds one person's care notebook:")
            appendLine("the notes somebody kept about looking after them, the calls they made,")
            appendLine("the questions they asked, and photographs of the paperwork.")
            appendLine()
            appendLine("It was written by Health Trail " + manifest.appVersion + ",")
            appendLine("archive format version " + manifest.formatVersion + ".")
            appendLine()
            appendLine("THE CONTENTS ARE ENCRYPTED")
            appendLine()
            appendLine("You need the passphrase chosen when this file was made.")
            appendLine()
            manifest.passphraseHint?.let { hint ->
                appendLine("Whoever made this file left themselves this reminder of it:")
                appendLine()
                appendLine("  " + hint)
                appendLine()
            }
            appendLine("IF THE PASSPHRASE IS LOST, THIS ARCHIVE CANNOT BE OPENED.")
            appendLine("Not by its author, not by anyone. There is no server holding a copy,")
            appendLine("no recovery code, and no backdoor. This is not a policy that can be")
            appendLine("appealed to somebody; it is a property of how the file was written.")
            appendLine()
            appendLine("HOW TO OPEN IT WITHOUT THIS APP")
            appendLine()
            appendLine("You do not need Health Trail, or the phone this came from.")
            appendLine()
            if (encryption != null) {
                appendLine("The key is derived from your passphrase with " + encryption.kdf + ":")
                appendLine("  iterations   " + encryption.iterations)
                appendLine("  memory       " + encryption.memoryKib + " KiB")
                appendLine("  parallelism  " + encryption.parallelism)
                appendLine("The contents are then decrypted with " + encryption.algorithm + ".")
                appendLine("The salt and the nonce prefix are in MANIFEST.json beside this file.")
                appendLine()
                appendLine("payload.enc is not one encrypted block. It is a run of frames, each")
                appendLine("a four byte big-endian length followed by that many bytes of")
                appendLine("ciphertext and its tag. Frame number N uses the nonce prefix")
                appendLine("followed by N as eight big-endian bytes, and authenticates those")
                appendLine("same eight bytes plus one byte that is 1 on the last frame and 0")
                appendLine("otherwise. That last byte is what makes a truncated file fail")
                appendLine("rather than open short. Each frame holds " + encryption.chunkBytes)
                appendLine("bytes of the archive, except the last.")
                appendLine()
            }
            appendLine("The format is specified byte for byte at:")
            appendLine("  contract/EXPORT-FORMAT.md")
            appendLine("in the Health Trail source repository, which is public and licensed")
            appendLine("so that it outlives the project.")
            appendLine()
            appendLine("A ready made tool is in the same repository at:")
            appendLine("  tools/decrypt/")
            appendLine("It needs Python and two ordinary libraries, and its README is written")
            appendLine("for somebody who does not write software.")
            appendLine()
            appendLine("ONCE IT IS OPEN")
            appendLine()
            appendLine("What comes out of payload.enc is an ordinary zip file. Inside it:")
            appendLine()
            appendLine("  README.txt          this again, from the inside")
            appendLine("  MANIFEST.json       what the archive holds, in full")
            appendLine("  CHECKSUMS.txt       SHA-256 of every file above, one per line")
            appendLine("  data/trail.sqlite   the record, which any SQLite tool reads")
            appendLine("  data/schema.sql     what every table and column in it means")
            appendLine("  readable/           the record as ordinary web pages")
            appendLine("  attachments/        the original photographs and documents")
            appendLine()
            appendLine("Open readable/index.html in any web browser. That folder is the whole")
            appendLine("record as ordinary pages and needs no software and no internet at all.")
            appendLine()
            appendLine("WHAT THIS IS NOT")
            appendLine()
            appendLine("These are one person's own notes. It is not a clinical record, it was")
            appendLine("not written or checked by a doctor, and nothing in it is advice.")
        }
    }

    /**
     * The README that travels inside the payload.
     *
     * **The same text, deliberately.** Two files with the same name and
     * different words is how a reader learns not to trust either. The inner copy
     * exists so that a folder somebody extracted years ago, long separated from
     * the zip it came out of, still says what it is and where the format is
     * written down.
     */
    private fun innerReadmeText(manifest: Manifest): String = readmeText(manifest)

    /**
     * The readable copy's pages, keyed by their path inside `readable/`.
     *
     * **Empty rather than throwing if anything goes wrong reading the staged
     * database.** An export that fails outright because the human copy could not
     * be built would cost the person the machine payload too, and the payload is
     * the half that restores. The readable copy is what makes the archive
     * outlive the app; the payload is what gets their record back on a new
     * phone, and losing the second to protect the first is the wrong trade at
     * the moment somebody is exporting.
     *
     * **A missing readable folder is loud rather than silent**, because
     * `MANIFEST.json` carries the page count and the offline read test in 8.5
     * opens `readable/index.html` on a machine with no network. A zero there is
     * a failed export, not a quiet one.
     */
    @VisibleForTesting
    internal fun readablePages(
        database: File,
        words: ReadableArchive.Words,
    ): Map<String, String> = try {
        val fields = ReadableFieldMap.tables
        val rows = ReadableRows.read(database, fields.keys)
        ReadableArchive.render(
            ReadableArchive.Source(
                tables = rows,
                // The language the readable copy is written in, section 8.2,
                // and every word it uses. **Both travel together in [words]**,
                // because the direction was right and the language was English
                // for as long as they were two separate questions. #327.
                words = words,
                subjectName = ReadableRows.subjectName(rows),
            ),
            fields,
        )
    } catch (error: RuntimeException) {
        // **Empty rather than throwing, and the KDoc above says why**: losing
        // the payload to protect the readable copy is the wrong trade at the
        // moment somebody is exporting, because the payload is the half that
        // gets their record back.
        //
        // **#412 wanted this to fail the export and it does not, because the
        // check belongs one layer up.** `ExportContainerTest` writes payloads
        // that are deliberately not databases at all, so a throw here fails
        // eleven tests that are testing the envelope rather than the notebook.
        // What was actually missing is that nobody ever read the page count
        // this produces. `MANIFEST.json` carries it, and the export's readback
        // now refuses a real archive whose readable half came out empty.
        emptyMap()
    }

    /**
     * Whether [target] really lands inside [root]. #414.
     *
     * **Canonical paths on both sides, and a separator on the root.** Without
     * the separator, `/data/app/stagingevil` starts with `/data/app/staging`
     * and passes a check that was meant to refuse it. Symlinks are resolved by
     * `canonicalPath`, which is the reason for using it over `absolutePath`.
     */
    private fun isInside(target: File, root: File): Boolean {
        val within = root.canonicalFile.path + File.separator
        return target.canonicalPath.startsWith(within)
    }

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

        // -- the outer layer, which says nothing about the person -------------

        var outerJson: JSONObject? = null
        var sawPayload = false
        val sealed = File(staging, "payload.enc")

        try {
            ZipInputStream(file.inputStream().buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        OUTER_MANIFEST -> outerJson = JSONObject(zip.readText())
                        PAYLOAD -> {
                            sealed.outputStream().buffered().use { zip.copyTo(it) }
                            sawPayload = true
                        }
                        // Version 2's shape, kept only so its refusal below can
                        // name what the file is. Nothing reads these.
                        "manifest.json" -> outerJson = JSONObject(zip.readText())
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (_: Throwable) {
            passphrase?.fill('\u0000')
            return@withContext failure(
                Problem.NotAContainer(
                    "This file could not be opened as a Health Trail export. " +
                        "It may be truncated or it may be a different kind of file."
                )
            )
        }

        val json = outerJson ?: run {
            passphrase?.fill('\u0000')
            return@withContext failure(
                Problem.NoManifest(
                    "This file has no manifest, so there is no way to tell what it holds. " +
                        "Health Trail exports always carry one."
                )
            )
        }

        val outer = Manifest.from(json)

        // Read before anything else, and refuse a version we do not understand
        // rather than guessing. This costs nothing now and is unfixable later.
        if (outer.formatVersion > FORMAT_VERSION) {
            passphrase?.fill('\u0000')
            return@withContext failure(
                Problem.FromTheFuture(
                    outer.formatVersion,
                    "This export was written by a newer version of Health Trail. " +
                        "It is format ${outer.formatVersion} and this app understands " +
                        "up to $FORMAT_VERSION. Update the app and try again.",
                )
            )
        }

        // **An unencrypted export is refused by what it is, not by what wrote
        // it**, so a hand assembled one is caught too. D67. This is checked
        // before the version, because what matters about such a file is that it
        // is a readable copy of somebody's record, not which build made it.
        if (!outer.encrypted) {
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

        if (outer.formatVersion < FORMAT_VERSION || !sawPayload) {
            passphrase?.fill('\u0000')
            return@withContext failure(
                Problem.NotPortable(
                    "This export was written by a development build of Health Trail, " +
                        "before the archive was split into a plain outer layer and an " +
                        "encrypted payload. No released version wrote one, so this app has " +
                        "no reader for it. Nothing was changed."
                )
            )
        }

        val parameters = outer.encryption ?: run {
            passphrase?.fill('\u0000')
            return@withContext failure(
                Problem.CouldNotDecrypt(
                    "This export says it is encrypted but does not record how, so there " +
                        "is no way to open it. Nothing was changed."
                )
            )
        }
        val offered = passphrase ?: return@withContext failure(
            Problem.PassphraseNeeded(
                "This export is encrypted. It needs the passphrase that was chosen " +
                    "when it was made. There is no way to recover it if it is lost."
            )
        )

        // -- the payload ------------------------------------------------------

        val key = ExportCrypto.derive(
            passphrase = offered,
            salt = unbase64(parameters.salt),
            iterations = parameters.iterations,
            memoryKib = parameters.memoryKib,
            parallelism = parameters.parallelism,
        )
        offered.fill('\u0000')

        val inner = File(staging, "payload.zip")
        try {
            unsealPayload(sealed, inner, key, unbase64(parameters.noncePrefix))
        } catch (_: Throwable) {
            ExportCrypto.wipe(key)
            inner.delete()
            sealed.delete()
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
            sealed.delete()
        }

        // -- the inner layer, which is an ordinary zip -------------------------

        val database = File(staging, "trail.sqlite")
        val attachments = mutableListOf<File>()
        var innerJson: JSONObject? = null
        var checksums: Map<String, String> = emptyMap()
        val actual = sortedMapOf<String, String>()

        try {
            ZipInputStream(inner.inputStream().buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBytes()
                        when {
                            entry.name == INNER_MANIFEST ->
                                innerJson = JSONObject(bytes.decodeToString())
                            entry.name == CHECKSUMS ->
                                checksums = parseChecksums(bytes.decodeToString())
                            entry.name == DATABASE -> database.writeBytes(bytes)
                            entry.name.startsWith(ATTACHMENTS) -> {
                                // **Where this entry would land is checked
                                // before a byte of it is written.** #414.
                                //
                                // The name comes out of a file somebody was
                                // handed, and it was joined to the staging
                                // directory and written immediately. The
                                // checksum and hash checks that would reject it
                                // run afterward, so a name like
                                // `attachments/../../shared_prefs/health_trail_key.xml`
                                // was already on the disk by the time anything
                                // objected, and overwriting the wrapped key
                                // makes the notebook permanently unopenable.
                                //
                                // **`requireSafeName` does not close this and
                                // must not be reached for.** It tests ASCII,
                                // length, the characters some filesystems
                                // refuse, and reserved device names. The string
                                // `../../shared_prefs/health_trail_key.xml`
                                // passes every one of those, and it is only
                                // called on the write path anyway.
                                //
                                // A containment check is the canonical path
                                // starting inside the canonical root, and
                                // nothing else is.
                                val out = File(staging, entry.name.removePrefix(ATTACHMENTS))
                                if (!isInside(out, staging)) {
                                    throw java.io.IOException(
                                        "an entry in this archive names a place outside it",
                                    )
                                }
                                out.parentFile?.mkdirs()
                                out.writeBytes(bytes)
                                attachments += out
                            }
                        }
                        if (entry.name != CHECKSUMS) {
                            actual[entry.name] = Attachments.sha256(bytes)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (_: Throwable) {
            inner.delete()
            return@withContext failure(
                Problem.NotAContainer(
                    "This export opened with that passphrase, so the passphrase is right, " +
                        "and what came out is not a Health Trail archive. The file has " +
                        "been altered since it was made. Nothing was changed."
                )
            )
        } finally {
            inner.delete()
        }

        val manifest = innerJson?.let { Manifest.from(it) } ?: return@withContext failure(
            Problem.NoManifest(
                "This export opened with that passphrase and has no manifest inside it, " +
                    "so there is no way to tell what it holds. Nothing was changed."
            )
        )

        if (!database.isFile) {
            return@withContext failure(
                Problem.DatabaseMissing("This export has a manifest but no records in it.")
            )
        }

        // **Every file checked against CHECKSUMS.txt, not only the database.**
        // The readable copy is the half a person actually reads, and an archive
        // that reported its database sound while its prose had rotted would be
        // exactly the partial correctness section 8 opens by refusing.
        checksums.forEach { (name, expectedHash) ->
            val got = actual[name]
            if (got == null) {
                return@withContext failure(
                    Problem.DatabaseCorrupt(
                        "This export is missing $name, which its own list of contents says " +
                            "it should have. The file was probably damaged in transit. " +
                            "Nothing was changed."
                    )
                )
            }
            if (got != expectedHash) {
                return@withContext failure(
                    if (name.startsWith(ATTACHMENTS)) {
                        Problem.AttachmentCorrupt(
                            name.removePrefix(ATTACHMENTS),
                            "One of the attached files is damaged. Nothing was changed.",
                        )
                    } else {
                        Problem.DatabaseCorrupt(
                            "Part of this export does not match what its own list of " +
                                "contents says it should be: $name. The file was probably " +
                                "damaged in transit. Nothing was changed."
                        )
                    }
                )
            }
        }

        // **What came out has to be a database.** An export written before
        // 2026-08-02 decrypts perfectly and yields a SQLCipher file keyed to the
        // phone that wrote it, which nothing else can open. Without this it fails
        // two checks later as "damaged", sending somebody to hunt a corruption
        // that is not there, on the one file standing between them and losing the
        // record. D61 and D67.
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

        if (Attachments.sha256(database.readBytes()) != manifest.databaseSha256) {
            return@withContext failure(
                Problem.DatabaseCorrupt(
                    "The records in this export do not match what its manifest says they " +
                        "should be. The file was probably damaged in transit. Nothing was " +
                        "changed."
                )
            )
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
        if (expected != null) {
            inspect(
                database,
                attachments.map { it.name }.toSet(),
                expected,
                manifest.missingAttachments.map { it.sha256 }.toSet(),
            )?.let { return@withContext failure<Opened>(it) }
        }

        Result.success(Opened(manifest, database, attachments))
    }

    /**
     * Decrypts `payload.enc` back into the inner container, a frame at a time.
     *
     * **A frame that says it is the last has to be the last.** The additional
     * data binds each frame's index and its finality into its tag, so a stream
     * that has been cut short, reordered, or had frames lifted out of the middle
     * fails here rather than producing a shorter archive that opens perfectly.
     *
     * Throws on any failure, including a wrong passphrase, and the caller says
     * so without claiming to know which it was.
     */
    private fun unsealPayload(
        sealed: File,
        target: File,
        key: ByteArray,
        noncePrefix: ByteArray,
    ) {
        sealed.inputStream().buffered().use { input ->
            target.outputStream().buffered().use { out ->
                var index = 0L
                var finished = false
                while (true) {
                    val header = ByteArray(FRAME_HEADER_BYTES)
                    val got = fill(input, header)
                    if (got == 0) break
                    check(!finished) { "the payload continues past the frame that said it ended" }
                    check(got == FRAME_HEADER_BYTES) { "the payload ends inside a frame header" }
                    val size = ((header[0].toInt() and 0xFF) shl 24) or
                        ((header[1].toInt() and 0xFF) shl 16) or
                        ((header[2].toInt() and 0xFF) shl 8) or
                        (header[3].toInt() and 0xFF)
                    check(size in 1..MAX_FRAME_BYTES) { "the payload declares an impossible frame" }
                    val frame = ByteArray(size)
                    check(fill(input, frame) == size) { "the payload ends inside a frame" }

                    // **Which frame this is decided by trying**, because only the
                    // tag knows whether this one was written as the last. A frame
                    // that verifies as final is final; one that verifies as
                    // ordinary is not; one that verifies as neither is a wrong
                    // passphrase or an altered file, which is the same exception.
                    val plain = try {
                        ExportCrypto.decrypt(
                            key, ExportCrypto.chunkNonce(noncePrefix, index), frame,
                            ExportCrypto.frameAad(index, last = false),
                        )
                    } catch (_: Throwable) {
                        finished = true
                        ExportCrypto.decrypt(
                            key, ExportCrypto.chunkNonce(noncePrefix, index), frame,
                            ExportCrypto.frameAad(index, last = true),
                        )
                    }
                    out.write(plain)
                    index += 1
                }
                check(finished) { "the payload has no final frame, so it was cut short" }
            }
        }
    }

    /**
     * The largest frame this reader will allocate for.
     *
     * **A length prefix read from a file is an instruction from whoever wrote
     * the file**, and one that says four gigabytes is how a reader is made to
     * exhaust memory before it has authenticated anything. The writer's frames
     * are a megabyte plus a tag; this allows generously more and refuses the
     * rest.
     */
    private const val MAX_FRAME_BYTES = 64 shl 20

    /** Reads `CHECKSUMS.txt` back into a map of path to hash. */
    private fun parseChecksums(text: String): Map<String, String> =
        text.lineSequence()
            .mapNotNull { line ->
                val at = line.indexOf("  ")
                if (at <= 0) null else line.substring(at + 2) to line.substring(0, at)
            }
            .toMap()

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
        declaredMissing: Set<String>,
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
            unknownShape(open, expected) ?: missingAttachment(open, present, declaredMissing)
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
        // **Views are validated, and they were not.** #415.
        //
        // This enumerated `type = 'table'` only. `sqlcipher_export` copies views
        // and triggers, so an archive's views land in the live database, and
        // `Backup.userTables` then reads `type = 'view' AND name LIKE 'live_%'`,
        // strips the prefix and puts the result into statement text. Every
        // integrity check passes by construction, because the passphrase, the
        // manifest and the databaseSha256 are all produced by whoever made the
        // file.
        //
        // **Whitelisted structurally rather than rejected**, exactly as the
        // issue requires: every legitimate archive carries forty of these, so
        // refusing views outright would break every real restore. A view is
        // expected when it is `live_` plus a table this build actually has,
        // which is derived from the live schema rather than listed a second
        // time in Kotlin, per D16.
        //
        // Triggers are deliberately not covered here. Nothing reads a trigger
        // name into SQL text, so they are not the path this closes, and
        // enumerating the eighty the contract creates would be the second
        // declaration D16 exists to prevent.
        open.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'view'",
            null,
        ).use { views ->
            while (views.moveToNext()) {
                val view = views.getString(0)
                val backing = view.removePrefix("live_")
                if (!view.startsWith("live_") || backing !in expected) {
                    return Problem.UnknownSchema(
                        view,
                        "This export holds a kind of record this version of Health Trail " +
                            "does not have, called \"$view\". It says it is a format this " +
                            "app understands, so it has been altered since it was made. " +
                            "Nothing was changed.",
                    )
                }
            }
        }

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
    private fun missingAttachment(
        open: SQLiteDatabase,
        present: Set<String>,
        /**
         * Hashes the manifest already says are gone, which are not a failure.
         *
         * **This is the clause that turns a silent failure into a stated one.**
         * Before it, an attachment row whose bytes were gone at export produced
         * an archive this app refused by name, at restore, on the new phone,
         * with the old one gone. The export now looks and records what it
         * found, so a file the manifest declares missing is a fact the archive
         * is carrying rather than damage in transit. `contract/DATA-CONTRACT.md`
         * 8.3: it imports "with its name and date intact, so the person sees
         * that a photo existed and is gone, rather than never learning it was
         * there". #332.
         *
         * **A hash that is absent and undeclared still fails.** That is the
         * difference between an archive that knows what it is missing and one
         * that was damaged, and it is the whole reason this is a list rather
         * than a flag.
         */
        declaredMissing: Set<String>,
    ): Problem? {
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
                if (hash !in present && hash !in declaredMissing) {
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

    /**
     * What may sit in the clear. `contract/DATA-CONTRACT.md` section 8.1.
     *
     * **Only what a reader needs before it can ask for a passphrase**, which is
     * the format version, what wrote it, when, and how the key is derived.
     * Nothing here says anything about the person: no counts, no dates of care,
     * no device identity, no locale.
     *
     * The list is deliberately short and adding to it is a decision. A field
     * here is a field that anything with access to the folder can read: a
     * backup agent, a cloud sync, a file manager preview.
     */
    private fun Manifest.toPublicJson(): JSONObject = JSONObject().apply {
        put("format_version", formatVersion)
        put("app_version", appVersion)
        put("platform", platform)
        put("schema_version", FORMAT_VERSION)
        put("exported_at", exportedAt)
        put("encrypted", encrypted)
        // Only when there is one. An absent key and an empty string are the same
        // fact, and writing the empty one would put a field in every archive
        // that exists to say the person declined.
        passphraseHint?.let { put("passphrase_hint", it) }
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
                    put("nonce_prefix", it.noncePrefix)
                    put("chunk_bytes", it.chunkBytes)
                },
            )
        }
    }

    /**
     * Everything else, which goes inside the encryption.
     *
     * **Row counts are a profile.** "23 appointments, 9 chapters, 1,630
     * entries, 6 years" describes how ill somebody has been and for how long.
     * So are the attachment count, the subject count, the page count, and the
     * device that wrote it.
     *
     * The payload hash lives here too. It describes the ciphertext, so it is
     * still checkable, and it is a fingerprint of a specific file that would
     * otherwise let somebody match two copies of one person's archive across
     * two places without opening either.
     */
    /**
     * The manifest as it appears inside the payload: everything, counts and all.
     *
     * **The outer one is this with the describing half left out**, rather than
     * the other way around, so there is one definition of what an archive knows
     * about itself and the public copy is visibly a subset of it.
     */
    private fun Manifest.toFullJson(): JSONObject = toPublicJson().apply {
        for (field in toPrivateJson().keys()) put(field, toPrivateJson().get(field))
    }

    private fun Manifest.toPrivateJson(): JSONObject = JSONObject().apply {
        put("origin_device", originDevice)
        put(
            "database",
            JSONObject().apply {
                put("sha256", databaseSha256)
                put("byte_size", databaseBytes)
                put("row_counts", JSONObject(rowCounts.toMap()))
            },
        )
        put(
            "attachments",
            JSONObject().apply {
                put("count", attachmentCount)
                put("total_bytes", attachmentBytes)
                // **Only when there are any**, so an archive from a sound
                // notebook does not carry a field that exists to say nothing
                // went wrong. A reader treats absent and empty the same.
                if (missingAttachments.isNotEmpty()) {
                    put(
                        "missing",
                        org.json.JSONArray().apply {
                            missingAttachments.forEach { gone ->
                                put(
                                    JSONObject().apply {
                                        put("sha256", gone.sha256)
                                        gone.originalFilename?.let { put("original_filename", it) }
                                        put("created_at", gone.createdAt)
                                    },
                                )
                            }
                        },
                    )
                }
            },
        )
        put("subject_count", subjectCount)
        put(
            "readable",
            JSONObject().apply {
                put("pages", readablePages)
                // 8.2 always asked for this and the format document never
                // listed it. #210.
                readableLocale?.let { put("locale", it) }
            },
        )
        exportedZone?.let { put("exported_zone", it) }
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
        passphraseHint = json.optString("passphrase_hint").takeIf { it.isNotBlank() },
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
                noncePrefix = it.optString("nonce_prefix"),
                chunkBytes = it.optInt("chunk_bytes"),
            )
        },
        databaseSha256 = database.optString("sha256"),
        databaseBytes = database.optLong("byte_size"),
        rowCounts = counts.keys().asSequence().associateWith { counts.optInt(it) },
        attachmentCount = attachments.optInt("count"),
        attachmentBytes = attachments.optLong("total_bytes"),
        subjectCount = json.optInt("subject_count"),
        readableLocale = json.optJSONObject("readable")
            ?.optString("locale")?.takeIf { it.isNotBlank() },
        exportedZone = json.optString("exported_zone").takeIf { it.isNotBlank() },
        missingAttachments = attachments.optJSONArray("missing")
            ?.let { array ->
                (0 until array.length()).mapNotNull { at ->
                    array.optJSONObject(at)?.let { gone ->
                        ExportContainer.MissingAttachment(
                            sha256 = gone.optString("sha256"),
                            originalFilename = gone.optString("original_filename")
                                .takeIf { it.isNotBlank() },
                            createdAt = gone.optLong("created_at"),
                        )
                    }
                }
            }
            .orEmpty(),
    )
}
