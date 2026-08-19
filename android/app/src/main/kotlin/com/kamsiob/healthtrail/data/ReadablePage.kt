package com.kamsiob.healthtrail.data

/**
 * The page shell every readable page is written into.
 * `contract/DATA-CONTRACT.md` section 8.2.
 *
 * **The standard is HL7 CDA's**, which has required for twenty years that a
 * clinical document be renderable by any recipient using general-market tools
 * with no special stylesheet shipped alongside it. Applied here that means the
 * page has to open, correctly, on a machine that has never heard of this app,
 * with no network, in a browser that does not exist yet.
 *
 * So, and each of these is a rule rather than a preference:
 *
 * **No JavaScript.** Nothing to execute, nothing to be blocked, nothing to rot.
 *
 * **No web fonts, no external stylesheet, no CDN, no network request of any
 * kind.** A page that fetches anything is a page that renders differently, or
 * not at all, on the machine where it matters.
 *
 * **The CSS is inlined into every page rather than shared**, so a page that gets
 * separated from its siblings, mailed on its own, or printed still stands up.
 * That costs a few hundred bytes per page and buys the thing the format exists
 * for.
 *
 * **System font stack only**, so it uses whatever the reader's machine has.
 *
 * **A print stylesheet**, because section 8.2 requires that a person can print a
 * year, or one incident thread, and hand it to a doctor, a lawyer, or a sibling.
 * Page breaks land between entries and never inside one.
 *
 * **Deterministic.** No timestamp, no random id, no ordering that depends on a
 * hash map. The same database produces byte-identical HTML every time, which is
 * what the regeneration test in 8.5 rests on.
 */
internal object ReadablePage {

    /**
     * One page, complete.
     *
     * @param dir "rtl" for Arabic, "ltr" otherwise. Section 8.2 requires the
     *   correct `dir` attribute and correct RTL rendering, verified in a browser
     *   rather than assumed.
     * @param lang the BCP 47 tag the person used the app in, so a screen reader
     *   opening this file pronounces it correctly.
     * @param upLink a relative path back to the index, or null on the index
     *   itself. Every page is reachable from every other page without the
     *   reader knowing the folder structure.
     */
    fun render(
        title: String,
        lang: String,
        dir: String,
        body: String,
        upLink: String? = null,
        upLabel: String = "",
    ): String = buildString {
        append("<!DOCTYPE html>\n")
        append("<html lang=\"").append(escape(lang)).append("\" dir=\"").append(escape(dir))
            .append("\">\n<head>\n<meta charset=\"utf-8\">\n")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        append("<title>").append(escape(title)).append("</title>\n")
        append("<style>\n").append(CSS).append("</style>\n</head>\n<body>\n")
        if (upLink != null) {
            append("<nav class=\"up\"><a href=\"").append(escape(upLink)).append("\">")
                .append(escape(upLabel)).append("</a></nav>\n")
        }
        append(body)
        append("</body>\n</html>\n")
    }

    /**
     * HTML escaping, applied to **everything** that came from the person.
     *
     * A care record contains names, notes, and quoted speech from real people,
     * and any of them can contain a `<` or an `&`. An archive that renders a
     * note wrong, or swallows half of it, has failed at the one job the format
     * has. Quotes are escaped too, because the same helper writes attributes.
     */
    fun escape(value: String): String = buildString(value.length) {
        for (char in value) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    /**
     * A value the person never filled in.
     *
     * **"Not recorded", never blank and never zero.** Section 8.2: a field the
     * person never filled reads as not recorded. A blank cell is ambiguous
     * between "nothing was written" and "the archive lost it", and a zero is a
     * measurement nobody took being reported as a measurement of zero.
     *
     * @param word the phrase itself, in the person's language, from
     *   `ReadableArchive.Words`. **Passed in rather than written here**, because
     *   this file has no locale and the page is written in the language the
     *   person used the app in. It said "not recorded" in Arabic archives until
     *   2026-08-09. #327.
     */
    fun notRecorded(label: String, word: String): String =
        "<div class=\"f\"><dt>${escape(label)}</dt><dd class=\"none\">${escape(word)}</dd></div>"

    /**
     * The three marks a memo can carry, turned into tags. D207, 8.8.1.
     *
     * **Escaped first and marked second, and that order is the whole of the
     * safety.** The person's own text becomes harmless characters before any
     * tag is introduced, so nothing anybody typed can open an element. The only
     * tags this ever produces are `strong`, `em` and a `ul` of `li`.
     *
     * **Only the three**, `contract/DATA-CONTRACT.md` 8.8.1: `**bold**`,
     * `_italic_`, and a line beginning `- `. Anything else stays exactly as it
     * was typed, which is what makes the file legible without this app.
     *
     * **An unclosed mark stays the characters it is**, the same rule the app
     * itself follows, so a half written memo reads as what somebody wrote
     * rather than swallowing the rest of the page.
     */
    fun marked(value: String): String {
        val out = StringBuilder()
        var inList = false
        for ((index, raw) in value.lines().withIndex()) {
            val bullet = raw.startsWith("- ")
            if (bullet && !inList) {
                out.append("<ul>")
                inList = true
            } else if (!bullet && inList) {
                out.append("</ul>")
                inList = false
            } else if (!bullet && index > 0) {
                out.append("<br>")
            }
            val line = inline(escape(if (bullet) raw.removePrefix("- ") else raw))
            if (bullet) out.append("<li>").append(line).append("</li>") else out.append(line)
        }
        if (inList) out.append("</ul>")
        return out.toString()
    }

    /** Bold and italic within one already escaped line. */
    private fun inline(line: String): String {
        var out = line
        for ((mark, tag) in listOf("**" to "strong", "_" to "em")) {
            val parts = out.split(mark)
            // An odd number of pieces means every opener found its closer.
            if (parts.size >= 3 && parts.size % 2 == 1) {
                out = parts.mapIndexed { i, part ->
                    if (i % 2 == 1 && part.isNotEmpty()) "<$tag>$part</$tag>" else part
                }.joinToString("")
            }
        }
        return out
    }

    fun field(label: String, value: String?, notRecordedWord: String): String =
        if (value.isNullOrBlank()) {
            notRecorded(label, notRecordedWord)
        } else {
            // **Marked rather than escaped alone**, so the three a memo can
            // carry read as emphasis in the file a stranger opens. `marked`
            // escapes first, so this is not a hole. 8.8.1.
            "<div class=\"f\"><dt>${escape(label)}</dt><dd>${marked(value)}</dd></div>"
        }

    /**
     * A relative link into `../attachments/`.
     *
     * **Never base64.** Section 8.2: a three-year archive inlined into one file
     * will not open. The bytes stay in `attachments/` and the page points at
     * them, which also means a person can open the folder and find their own
     * photographs without going through the pages at all.
     */
    fun attachment(path: String, caption: String): String =
        "<a class=\"att\" href=\"../attachments/${escape(path)}\">${escape(caption)}</a>"

    /**
     * The stylesheet, inlined into every page.
     *
     * Deliberately small and deliberately dull. It uses nothing clever, no
     * custom properties, no grid areas, no `:has`, because it has to render on a
     * browser that does not exist yet and every clever thing is a thing that can
     * stop being supported. Two columns come from a float-free definition list
     * rather than from a layout engine.
     *
     * **Colors are near-black on white**, not the app's palette. This is a
     * document rather than the app: it gets printed, photocopied, and read on
     * whatever screen is to hand, and the app's warm paper would come out of a
     * printer as a gray wash.
     */
    private val CSS = """
        :root { color-scheme: light; }
        * { box-sizing: border-box; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
            "Helvetica Neue", Arial, "Noto Sans", sans-serif;
          line-height: 1.5; color: #111; background: #fff;
          margin: 0 auto; padding: 24px 20px 64px; max-width: 46em;
        }
        h1 { font-size: 1.6em; line-height: 1.2; margin: 0 0 4px; }
        h2 { font-size: 1.2em; margin: 32px 0 8px; }
        h3 { font-size: 1em; margin: 0 0 4px; }
        p { margin: 0 0 12px; }
        a { color: #14507a; }
        .up { margin-bottom: 16px; font-size: .9em; }
        .sub { color: #444; margin: 0 0 24px; }
        .item { border-top: 1px solid #ccc; padding: 16px 0; }
        .item:first-of-type { border-top: 0; }
        .when { font-size: .85em; color: #444; margin: 0 0 2px; }
        .note { white-space: pre-wrap; margin: 8px 0; }
        dl { margin: 8px 0 0; }
        .f { display: flex; gap: 8px; align-items: baseline; margin: 0 0 2px; }
        dt { flex: 0 0 11em; font-size: .85em; color: #444; margin: 0; }
        dd { margin: 0; }
        .none { color: #666; font-style: italic; }
        .id { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
              font-size: .78em; color: #555; word-break: break-all; }
        .att { display: inline-block; margin: 2px 8px 2px 0; }
        .gap { color: #444; font-style: italic; margin: 12px 0; }
        ul.toc { list-style: none; padding: 0; margin: 0; }
        ul.toc li { border-top: 1px solid #ccc; padding: 8px 0; }
        ul.toc li:first-child { border-top: 0; }
        .count { color: #444; font-size: .9em; }
        @media print {
          body { max-width: none; padding: 0; font-size: 11pt; }
          .up { display: none; }
          .item { break-inside: avoid; page-break-inside: avoid; }
          h2 { break-after: avoid; page-break-after: avoid; }
          a { color: #000; text-decoration: none; }
        }
    """.trimIndent()
}
