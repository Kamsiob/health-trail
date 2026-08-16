package com.kamsiob.healthtrail.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handing a generated document to whatever the person already uses.
 *
 * `MASTER_SPEC.md` 4.9: generated locally, handed to the system share sheet.
 * **No account, no server, no link.** The document goes to the sheet, the
 * person picks the app, and this project never learns where it went.
 *
 * **Written to the app's own cache, shared through a content URI.** A file in
 * Downloads would outlive the share and sit in a folder something else may
 * sync, which is the whole argument D67 makes about unencrypted exports. The
 * cache directory is cleaned by the system, the URI grants read to exactly the
 * app the person chose, and the grant dies with the activity.
 */
object Share {

    /**
     * The subdirectory the file provider is scoped to.
     *
     * Named rather than the whole cache, so a future accident somewhere else in
     * the cache cannot become shareable by default.
     */
    private const val DIRECTORY = "shared"

    /**
     * Writes the text and returns the intent that offers it.
     *
     * Returns null when the file cannot be written, so the caller can say so
     * rather than launching a sheet with nothing behind it, per rule 11.
     */
    fun documentIntent(
        context: Context,
        fileName: String,
        text: String,
        chooserTitle: String,
    ): Intent? {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }

        // **Old shares are removed before a new one is written.** These are
        // somebody's care notes sitting in plain text, and the cache is not a
        // place to accumulate them. The system would clear it eventually, and
        // eventually is not a policy.
        directory.listFiles()?.forEach { it.delete() }

        val file = File(directory, fileName)
        return try {
            file.writeText(text)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.shared",
                file,
            )
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    // **The text as well as the file.** Messaging apps take the
                    // text and ignore the attachment, mail apps take both, and
                    // a sibling reading it on a phone should not have to open a
                    // file to see what they were sent.
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                chooserTitle,
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * The intent that offers the person's own paper, as the image it is.
     *
     * **#379: "anywhere there's a document I need to be able to download it to
     * my phone or save it."** The share sheet is the Android way to do both:
     * every destination the phone offers, including Files and Downloads, comes
     * from the one control, and it needs no storage permission.
     *
     * **Copied into the shared cache rather than exposed from the store.** The
     * attachment directory is the app's private notebook and a content URI
     * into it would outlive the share. Old shares are cleared first, for the
     * same reason the text one clears them: these are somebody's records.
     */
    fun paperIntent(
        context: Context,
        sourceFile: File,
        fileName: String,
        chooserTitle: String,
    ): Intent? {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }

        val file = File(directory, fileName)
        return try {
            sourceFile.copyTo(file, overwrite = true)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.shared",
                file,
            )
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    // **Declared as an image**, so the sheet offers the gallery
                    // and the photo apps rather than only a file manager.
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                chooserTitle,
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * A file name a person recognizes in their Downloads folder.
     *
     * The document's own title, stripped to what a file system accepts, so
     * "Discharge summary, March" arrives as `Discharge summary, March.jpg`
     * rather than a hash. Falls back to a plain name when the title is all
     * punctuation or another script the file system may refuse.
     */
    fun paperFileName(title: String): String {
        val cleaned = title.trim().replace(Regex("""[\\/:*?"<>|]"""), " ").take(60).trim()
        return if (cleaned.isBlank()) "document.jpg" else "$cleaned.jpg"
    }
}
