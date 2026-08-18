package com.kamsiob.healthtrail.ui.v4

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * The only rich text this app stores, and there are exactly three marks. D207.
 *
 * **`contract/DATA-CONTRACT.md` 8.8.1 is the contract and this file obeys it.**
 * `**bold**`, `_italic_`, and a line beginning `- `. Nothing else is a mark: no
 * headings, no links, no tables, no code, no nesting, no numbered lists, no
 * strikethrough. A `#` at the start of a line is a `#`.
 *
 * **There is no encode and no decode in storage.** What the person typed is
 * what is in `entry.body`, and export copies the column, so "survives the
 * archive byte for byte" is true by construction rather than by a converter
 * that has to be right twice. This file only decides how those bytes are
 * *drawn*, and it never rewrites them.
 *
 * **A reader hears the words and never the marks**, which is [plain]. The
 * asterisks are emphasis for the eye; for somebody listening the sentence is
 * the sentence, and "star star call the office star star" is not.
 *
 * **Unclosed marks are text.** Somebody typing `**` and stopping has typed two
 * asterisks, and the app shows two asterisks rather than swallowing the rest of
 * the note into bold. Rule 13: a half written note is a valid note.
 */
object RichText {

    /** Bold, and it takes two asterisks on each side. */
    const val BOLD = "**"

    /** Italic, and it takes one underscore on each side. */
    const val ITALIC = "_"

    /** A bullet is a line that starts with this and nothing else counts. */
    const val BULLET = "- "

    /**
     * The three marks removed, leaving what a person would read aloud.
     *
     * **Bullets become their own lines with the dash kept**, because a reader
     * announcing a list wants to hear the items separated, and a bare `- ` at
     * the start of a spoken line is how every other list in this app is read.
     */
    fun plain(source: String): String = buildString {
        for ((index, line) in source.lines().withIndex()) {
            if (index > 0) append('\n')
            append(spans(stripBullet(line).text).joinToString("") { it.text })
        }
    }

    /**
     * The same string, with the marks turned into styles.
     *
     * **The bullet's dash is kept as a character** rather than drawn as a shape,
     * so a line wraps under its own text the way the trail's rows do and so
     * copying a note out of the app copies what was written.
     */
    fun annotated(source: String): AnnotatedString = buildAnnotatedString {
        for ((index, line) in source.lines().withIndex()) {
            if (index > 0) append('\n')
            val bulleted = stripBullet(line)
            if (bulleted.wasBullet) append("•  ")
            for (span in spans(bulleted.text)) {
                if (span.bold || span.italic) {
                    pushStyle(
                        SpanStyle(
                            fontWeight = if (span.bold) FontWeight.SemiBold else null,
                            fontStyle = if (span.italic) FontStyle.Italic else null,
                        ),
                    )
                    append(span.text)
                    pop()
                } else {
                    append(span.text)
                }
            }
        }
    }

    /** Whether anything in this string would be drawn differently from plain text. */
    fun hasMarks(source: String): Boolean =
        source.lines().any { line ->
            val bulleted = stripBullet(line)
            bulleted.wasBullet || spans(bulleted.text).any { it.bold || it.italic }
        }

    /** One run of characters and how it is drawn. */
    internal data class Span(val text: String, val bold: Boolean = false, val italic: Boolean = false)

    private data class Bulleted(val text: String, val wasBullet: Boolean)

    private fun stripBullet(line: String): Bulleted =
        if (line.startsWith(BULLET)) {
            Bulleted(line.removePrefix(BULLET), true)
        } else {
            Bulleted(line, false)
        }

    /**
     * One line split into runs.
     *
     * **Bold is looked for before italic**, because `**` starts with a character
     * that is not `_` and the two never fight; and an opener with no closer on
     * the same line is left as the characters it is. Marks do not span lines,
     * which is what keeps a half typed note from turning the rest of the note
     * bold while somebody is still writing it.
     */
    internal fun spans(line: String): List<Span> {
        val out = mutableListOf<Span>()
        val run = StringBuilder()
        var i = 0

        fun flush() {
            if (run.isNotEmpty()) {
                out += Span(run.toString())
                run.clear()
            }
        }

        while (i < line.length) {
            val bold = line.startsWith(BOLD, i)
            val italic = !bold && line.startsWith(ITALIC, i)
            if (!bold && !italic) {
                run.append(line[i])
                i += 1
                continue
            }
            val mark = if (bold) BOLD else ITALIC
            val close = line.indexOf(mark, startIndex = i + mark.length)
            // **An empty pair is two characters, not an empty style.** `**bold**`
            // has something between the marks; `****` does not, and drawing it
            // as nothing would delete what somebody typed.
            if (close < 0 || close == i + mark.length) {
                run.append(mark)
                i += mark.length
                continue
            }
            flush()
            out += Span(
                text = line.substring(i + mark.length, close),
                bold = bold,
                italic = italic,
            )
            i = close + mark.length
        }
        flush()
        return out
    }
}
