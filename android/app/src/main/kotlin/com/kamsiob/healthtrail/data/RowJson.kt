package com.kamsiob.healthtrail.data

/**
 * A row as JSON, and back. The two halves of `conflict_log`'s stored versions.
 *
 * **One file because they are one promise.** The schema keeps both sides of a
 * conflict "as JSON, complete, so nothing is lost and either can be restored by
 * hand if the person disagrees with the resolution". A writer in one file and a
 * reader in another is a pair nothing holds together, and the day they drift
 * the failure is a screen showing an empty resolution for a row that has two
 * perfectly good versions in the database.
 *
 * **No library, and that is not thrift.** Every value here is a string or null,
 * because that is how a row is read out of SQLite, so the whole grammar is an
 * object of quoted strings. Pulling in a parser to handle a shape this narrow
 * would add a dependency to the one code path that has to keep working when
 * somebody's only copy is at stake.
 *
 * **Pure, so the round trip is a unit test** rather than something that needs a
 * phone. `RowJsonTest` writes and reads back the values that actually break
 * this: a quote, a backslash, a newline, a tab, an accent, Arabic, and null,
 * which is not the empty string.
 */
internal object RowJson {

    /**
     * Keys sorted, so two runs of the same merge write the same bytes.
     *
     * A conflict written twice with the columns in a different order would look
     * like two different resolutions to anything comparing them.
     */
    fun write(row: Map<String, String?>): String = buildString {
        append('{')
        row.keys.sorted().forEachIndexed { index, key ->
            if (index > 0) append(',')
            append(quote(key)).append(':')
            val value = row[key]
            if (value == null) append("null") else append(quote(value))
        }
        append('}')
    }

    /**
     * Read back what [write] wrote.
     *
     * **Anything it cannot read comes back empty rather than throwing.** This
     * feeds a notice screen, and a notice screen that crashes is worse than one
     * that says less: the person is already looking at it because something
     * unexpected happened to their record.
     */
    fun read(text: String): Map<String, String?> = runCatching {
        val out = LinkedHashMap<String, String?>()
        var at = text.indexOf('{') + 1
        while (at < text.length) {
            while (at < text.length && text[at] != '"' && text[at] != '}') at++
            if (at >= text.length || text[at] == '}') break
            val (key, afterKey) = readString(text, at)
            var cursor = afterKey
            while (cursor < text.length && text[cursor] != ':') cursor++
            cursor++
            while (cursor < text.length && text[cursor] == ' ') cursor++
            if (text.startsWith("null", cursor)) {
                out[key] = null
                at = cursor + 4
            } else {
                val (value, afterValue) = readString(text, cursor)
                out[key] = value
                at = afterValue
            }
        }
        out
    }.getOrElse { emptyMap() }

    private fun quote(value: String): String = buildString(value.length + 2) {
        append('"')
        for (char in value) {
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char < ' ' -> append("\\u%04x".format(char.code))
                else -> append(char)
            }
        }
        append('"')
    }

    private fun readString(text: String, start: Int): Pair<String, Int> {
        var at = start
        while (at < text.length && text[at] != '"') at++
        at++
        val builder = StringBuilder()
        while (at < text.length && text[at] != '"') {
            if (text[at] == '\\') {
                at++
                when (text[at]) {
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    'u' -> {
                        builder.append(text.substring(at + 1, at + 5).toInt(16).toChar())
                        at += 4
                    }
                    else -> builder.append(text[at])
                }
            } else {
                builder.append(text[at])
            }
            at++
        }
        return builder.toString() to at + 1
    }
}
