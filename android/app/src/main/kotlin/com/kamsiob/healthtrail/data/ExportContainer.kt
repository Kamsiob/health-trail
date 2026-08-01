package com.kamsiob.healthtrail.data

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
 */
object ExportContainer {

    const val FORMAT_VERSION = 1
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
        val databaseSha256: String,
        val databaseBytes: Long,
        val rowCounts: Map<String, Int>,
        val attachmentCount: Int,
        val attachmentBytes: Long,
        val subjectCount: Int,
    ) {
        companion object
    }

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
    suspend fun write(target: File, source: Source): Manifest = withContext(Dispatchers.IO) {
        val databaseBytes = source.database.readBytes()
        val manifest = Manifest(
            formatVersion = FORMAT_VERSION,
            appVersion = source.appVersion,
            platform = "android",
            exportedAt = source.exportedAt,
            originDevice = source.originDevice,
            encrypted = false,
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
                file.inputStream().buffered().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        manifest
    }

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
    suspend fun open(file: File, staging: File): Result<Opened> = withContext(Dispatchers.IO) {
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

        Result.success(Opened(manifest, database, attachments))
    }

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
        databaseSha256 = database.optString("sha256"),
        databaseBytes = database.optLong("byte_size"),
        rowCounts = counts.keys().asSequence().associateWith { counts.optInt(it) },
        attachmentCount = attachments.optInt("count"),
        attachmentBytes = attachments.optLong("total_bytes"),
        subjectCount = json.optInt("subject_count"),
    )
}
