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
}
