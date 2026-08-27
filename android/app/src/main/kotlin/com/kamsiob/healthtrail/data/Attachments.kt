package com.kamsiob.healthtrail.data

import android.content.Context
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Attachment storage, content addressed, per `contract/DATA-CONTRACT.md`
 * section 3.
 *
 * **A file is named by the hash of its bytes.** Every consequence of that is
 * one the app wants:
 *
 * - An attachment can never conflict during sync, because identical bytes are
 *   the same file and two devices that photographed the same page agree.
 * - Storing or transferring one twice is free.
 * - A corrupt transfer is detectable by rehashing, which is what the export's
 *   failure case "an attachment whose hash does not match its filename" is.
 *
 * **The bytes are the truth and the row is the bookkeeping.** The database
 * holds the hash, the original filename, the mime type, and the size. It never
 * holds the bytes, because a photographed bill in a database row is a database
 * nobody can back up incrementally and a query that pages megabytes to count
 * rows.
 *
 * **Deleting a row never deletes the file.** Two rows can name one file, and a
 * tombstone is not a deletion. Sweeping unreferenced files is a separate,
 * explicit operation for the same reason the tombstone purge is: something
 * that removes bytes runs when it is asked to and not as a side effect.
 */
class Attachments private constructor(private val root: File) {

    /**
     * Writes bytes and returns their hash, which is also the filename.
     *
     * **Writing the same bytes twice is a no-op that returns the same hash.**
     * That is not an optimization, it is the point: a person who attaches the
     * same discharge summary to two entries has one file, and the second
     * attach costs nothing and cannot disagree with the first.
     *
     * Written to a temporary name and then moved, so a file that exists under
     * its hash is always complete. A half written file named by a hash it does
     * not match is the one thing content addressing must never produce.
     */
    suspend fun put(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val hash = sha256(bytes)
        val target = fileFor(hash)
        if (target.exists() && target.length() == bytes.size.toLong()) return@withContext hash

        root.mkdirs()
        val staging = File(root, "$hash.part")
        staging.writeBytes(bytes)
        // renameTo on the same filesystem is atomic, which is what makes the
        // presence of the file mean the whole file is there.
        if (!staging.renameTo(target)) {
            staging.delete()
            error("could not store attachment $hash")
        }
        hash
    }

    /** Streams bytes in, for a photograph too large to hold twice in memory. */
    suspend fun put(stream: InputStream): String = withContext(Dispatchers.IO) {
        root.mkdirs()
        val staging = File.createTempFile("incoming", ".part", root)
        val digest = MessageDigest.getInstance("SHA-256")
        staging.outputStream().use { out ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
                out.write(buffer, 0, read)
            }
        }
        val hash = digest.digest().toHex()
        val target = fileFor(hash)
        if (target.exists()) {
            staging.delete()
            return@withContext hash
        }
        if (!staging.renameTo(target)) {
            staging.delete()
            error("could not store attachment $hash")
        }
        hash
    }

    /** The file for a hash, whether or not it exists. */
    fun fileFor(hash: String): File = File(root, hash)

    suspend fun exists(hash: String): Boolean = withContext(Dispatchers.IO) {
        fileFor(hash).isFile
    }

    suspend fun read(hash: String): ByteArray? = withContext(Dispatchers.IO) {
        fileFor(hash).takeIf { it.isFile }?.readBytes()
    }

    /**
     * Whether the bytes on disk still hash to their own name.
     *
     * **The check the export's failure cases need**, and cheap insurance
     * against a truncated copy, a bad transfer, or a disk that lied. A file
     * that fails this is corrupt by definition, since its name is a claim
     * about its contents.
     */
    suspend fun verify(hash: String): Boolean = withContext(Dispatchers.IO) {
        val file = fileFor(hash)
        file.isFile && sha256(file.readBytes()) == hash
    }

    /** Every hash currently stored, for the export to walk. */
    suspend fun all(): List<String> = withContext(Dispatchers.IO) {
        root.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".part") }
            ?.map { it.name }
            .orEmpty()
            .sorted()
    }

    /** Total bytes on disk, which the manifest reports and the warning at 4 GB reads. */
    suspend fun totalBytes(): Long = withContext(Dispatchers.IO) {
        root.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    /** Removes any half written file left behind by a crash mid-write. */
    /**
     * Deletes one file, and only when a permanent delete has asked for it.
     *
     * **Nothing else in this class removes anything, and that is the rule.**
     * The store is content addressed and two rows can name one file, so a row
     * disappearing never means the bytes should. #465 is the one caller: it
     * checks that no `attachment` row still names the hash, tombstones
     * included, before asking.
     *
     * Returns whether a file was actually there to delete, so a purge can say
     * plainly what it took.
     */
    suspend fun remove(hash: String): Boolean = withContext(Dispatchers.IO) {
        val file = fileFor(hash)
        file.isFile && file.delete()
    }

    suspend fun sweepIncomplete(): Int = withContext(Dispatchers.IO) {
        root.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".part") || it.name.startsWith("incoming")) }
            ?.count { it.delete() }
            ?: 0
    }

    companion object {
        /** 25 MB each, decided in D13. Enforced by the caller, stated here so it is findable. */
        const val MAX_ATTACHMENT_BYTES = 25L * 1024 * 1024

        /** A warning at 4 GB total, and no hard ceiling. D13. */
        const val WARN_TOTAL_BYTES = 4L * 1024 * 1024 * 1024

        private const val BUFFER = 64 * 1024

        fun open(context: Context): Attachments =
            Attachments(File(context.filesDir, "attachments"))

        /** For tests, which need a directory they can fill and throw away. */
        fun openAt(root: File): Attachments = Attachments(root)

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

        private fun ByteArray.toHex(): String =
            joinToString("") { "%02x".format(it) }
    }
}
