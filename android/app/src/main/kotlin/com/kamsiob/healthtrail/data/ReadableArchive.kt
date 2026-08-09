package com.kamsiob.healthtrail.data

/**
 * The human copy inside the archive. `contract/DATA-CONTRACT.md` section 8.2.
 *
 * **This is the point of the format.** A machine payload that only this app can
 * read is a record that dies with the app, and a caregiver's archive outlives
 * the phone, outlives Android, and very likely outlives this project. Everything
 * else in the container exists so that this folder can be trusted.
 *
 * **Pure, and that is not an aesthetic choice.** Rows in, a map of paths to HTML
 * out. No Android, no file system, no clock, no locale lookup that depends on
 * the device. Two things follow from it that nothing else would give:
 *
 * The renderer can be checked exhaustively without a phone, which matters
 * because the failure this guards against shows up in year three and nobody is
 * going to walk a five year archive by hand every release.
 *
 * And it is **deterministic**, which the regeneration test in 8.5 rests on
 * entirely: export, import onto a clean install, regenerate, assert byte
 * identical. If the same rows can produce two different files, that test can
 * never pass and the strongest guarantee in the format is unavailable. So there
 * is no timestamp in the output, no generated id, and no iteration over a map
 * whose order is not fixed.
 *
 * **Completeness is structural rather than remembered.** Every column's fate is
 * decided in `contract/readable-fields.json` and `check_readable_coverage.py`
 * fails the build when a new column has no decision. This class renders what
 * that map says to render.
 *
 * **Visible beyond this file because [Words] is part of what an export is given**
 * and `ExportContainer.Source` carries it, alongside the schema and the rows.
 * Nothing outside the export path calls [render].
 */
object ReadableArchive {

    /** One row, as column name to value. Null is a null column, not an empty one. */
    typealias Row = Map<String, String?>

    /**
     * What the renderer needs, which is deliberately less than a database.
     *
     * @param tables every table's live rows, already tombstone-filtered by the
     *   caller. A deleted row is absent from the readable copy by definition:
     *   printing that something was deleted would put back what the person
     *   removed.
     * @param words every word the pages say that the person did not write,
     *   already resolved in their language. Handed in rather than looked up,
     *   for the reason in the class comment above.
     * @param subjectName who the record is about, for the front door.
     */
    data class Source(
        val tables: Map<String, List<Row>>,
        val words: Words,
        val subjectName: String?,
    )

    /**
     * The archive's own vocabulary, in the person's language.
     *
     * **Handed in, never looked up, and that is what makes the localization
     * safe rather than a second failure.** This object is pure by contract, and
     * a catalog lookup inside it would tie the strongest guarantee in the format
     * to whatever the device happened to be set to at render time. So the caller
     * resolves every word once and passes the result, and the renderer stays a
     * function of its arguments.
     *
     * **The pages were English in every language until 2026-08-09**, which was
     * found by exporting in Arabic and reading the result: `<html lang="ar"
     * dir="rtl">` on a correctly mirrored page whose every heading was English.
     * The direction half of section 8.2 was built and the language half had
     * never been started. #327.
     *
     * **The three templates are functions rather than strings** because they
     * carry a count or a year, and Arabic needs six plural forms for the count.
     * Formatting belongs to the catalog, where a translator can see it, and ICU
     * belongs to the caller, which has a locale. Determinism is unaffected: the
     * same words and the same rows still produce the same bytes.
     *
     * **Dates are the one thing not in here**, and that is deliberate rather
     * than missed. `ReadableDate` says why in its own comment: the month name is
     * spelled in English because the stranger a date has to survive is a records
     * office or a lawyer who may not read the person's language.
     */
    data class Words(
        /** The BCP 47 tag the person used the app in. */
        val lang: String,
        /** "rtl" or "ltr". */
        val dir: String,
        /** Table name to the section heading a reader sees. */
        val tables: Map<String, String>,
        /** Column name to its field label. */
        val columns: Map<String, String>,
        /** What the front page is titled when the record names nobody. */
        val subjectFallback: String,
        /** The front page's opening paragraph, which is also its disclaimer. */
        val about: String,
        val datedHeading: String,
        val wholeHeading: String,
        val howToHeading: String,
        /**
         * The front page's closing paragraph.
         *
         * Carries `{database}` and `{attachments}` where the two paths go. They
         * are substituted after escaping rather than before, because what
         * replaces them is markup and escaping it would print the tags.
         */
        val howToBody: String,
        /** The link every page carries back to the front page. */
        val back: String,
        /** What the year bucket for rows with no date is called. */
        val undated: String,
        /** A field the person never filled. Never blank and never zero. */
        val notRecorded: String,
        val yes: String,
        val no: String,
        /** The years covered, given the first and the last. Equal when one. */
        val covers: (String, String) -> String,
        /** One dated page's title, given its section name and its year. */
        val yearTitle: (String, String) -> String,
        /** How many records are on this page. Plural, in the locale's own rules. */
        val records: (Int) -> String,
    )

    /**
     * Sections that grow over time, so they are split by year.
     *
     * **One page per section per year**, section 8.2, so no single page becomes
     * unopenable at year five. A five year trail is 1,630 entries and a browser
     * asked to lay that out at once on an old phone will simply stop.
     *
     * The pair is the table and the column its year comes from.
     *
     * **Two of these named a column that does not exist**, `issued_edtf` on
     * `bill` and `dated_edtf` on `document`, so every bill and every document
     * ever exported was grouped under a null date and landed on one `undated`
     * page. Nothing failed: a missing column reads as null and null is a real
     * bucket, so the pages were produced, linked and counted, and the year they
     * belonged to was simply gone. Both tables carry `received_edtf`, which is
     * the date the person actually recorded. Found on 2026-08-09 by holding
     * this list to `contract/readable-fields.json` while doing #327, and the
     * reason it is now held there rather than here.
     */
    private val DATED = listOf(
        "entry" to "occurred_edtf",
        "incident" to "reported_edtf",
        "appointment" to "scheduled_edtf",
        "measurement" to "occurred_edtf",
        "medication_event" to "occurred_edtf",
        "bill" to "received_edtf",
        "document" to "received_edtf",
        "question" to "created_at",
        "milestone" to "occurred_edtf",
        "cost_entry" to "occurred_edtf",
        "instruction_violation" to "occurred_edtf",
    )

    /**
     * Reference tables, rendered whole on one page each.
     *
     * These are rosters rather than streams. Somebody on seven medications is
     * on seven medications; the list does not grow with the years the way the
     * trail does, so splitting it by year would scatter one short list across
     * five pages for no gain.
     */
    private val WHOLE = listOf(
        "subject", "person", "organization", "chapter", "care_thread",
        "medication", "medication_flag", "measure", "project", "project_step",
        "standing_instruction", "emergency_card", "emergency_contact",
        "cost_sheet", "custom_template", "attachment", "link",
        "person_chapter", "entry_thread", "entry_person", "project_person",
        "call_detail", "visit_detail",
        // The project's own shape and the arranged Today, per 8.7. These are
        // rosters, not streams: a project has a handful of stages and a person
        // has a screenful of cards, so splitting either by year would scatter a
        // short list across five pages and lose the order, which is the part of
        // it that carries meaning.
        "project_stage", "project_standing", "project_date", "project_date_kind",
        "project_paper", "today_card",
    )

    /**
     * Render the whole readable folder.
     *
     * @return path inside the archive to file contents, for every page.
     */
    fun render(source: Source, fieldMap: Map<String, TableFields>): Map<String, String> {
        val pages = LinkedHashMap<String, String>()
        val words = source.words
        val labels = Labels(source)

        val datedPages = mutableListOf<PageRef>()
        for ((table, dateColumn) in DATED) {
            val rows = source.tables[table].orEmpty()
            if (rows.isEmpty()) continue
            val fields = fieldMap[table] ?: continue
            val byYear = rows.groupBy { yearOf(it[dateColumn]) }
            // Sorted so the output does not depend on map iteration order, which
            // is what determinism means in practice.
            for (year in byYear.keys.sortedDescending()) {
                val inYear = byYear.getValue(year).sortedBy { it["id"].orEmpty() }
                // **The path stays in the schema's own words while the title is
                // translated.** A file name is an address: it is what the index
                // links to, what a person types, and what somebody who extracted
                // this folder three years ago already has bookmarked. Translating
                // it would mean the same notebook exported in two languages
                // produces two folders that cannot be diffed, and a path with
                // Arabic in it that some file systems will mangle.
                val path = "$table-$year.html"
                val title = words.yearTitle(
                    labels.table(table),
                    if (year == UNDATED) words.undated else year,
                )
                pages[path] = ReadablePage.render(
                    title = title,
                    lang = words.lang,
                    dir = words.dir,
                    upLink = "index.html",
                    upLabel = words.back,
                    body = section(title, inYear, fields, labels),
                )
                datedPages += PageRef(path, title, inYear.size)
            }
        }

        val wholePages = mutableListOf<PageRef>()
        for (table in WHOLE) {
            val rows = source.tables[table].orEmpty()
            if (rows.isEmpty()) continue
            val fields = fieldMap[table] ?: continue
            val sorted = rows.sortedBy { it["id"].orEmpty() }
            val path = "$table.html"
            val title = labels.table(table)
            pages[path] = ReadablePage.render(
                title = title,
                lang = words.lang,
                dir = words.dir,
                upLink = "index.html",
                upLabel = words.back,
                body = section(title, sorted, fields, labels),
            )
            wholePages += PageRef(path, title, sorted.size)
        }

        pages["index.html"] = index(source, datedPages, wholePages)
        // The index is written last and moved to the front, so a reader opening
        // the folder sees the front door first in an alphabetical listing too.
        return linkedMapOf("index.html" to pages.getValue("index.html")) +
            pages.filterKeys { it != "index.html" }
    }

    private data class PageRef(val path: String, val title: String, val count: Int)

    /**
     * The front door. Section 8.2: who this record is about, the date range it
     * covers, the counts by section, and a table of contents linking to every
     * other page.
     */
    private fun index(
        source: Source,
        dated: List<PageRef>,
        whole: List<PageRef>,
    ): String {
        val words = source.words
        val body = buildString {
            append("<h1>")
            append(ReadablePage.escape(source.subjectName ?: words.subjectFallback))
            append("</h1>\n")
            append("<p class=\"sub\">").append(ReadablePage.escape(words.about)).append("</p>\n")

            val years = dated.mapNotNull { it.path.substringAfterLast('-').removeSuffix(".html").toIntOrNull() }
            if (years.isNotEmpty()) {
                append("<p><strong>")
                append(ReadablePage.escape(words.covers(years.min().toString(), years.max().toString())))
                append("</strong></p>\n")
            }

            if (dated.isNotEmpty()) {
                append("<h2>").append(ReadablePage.escape(words.datedHeading))
                append("</h2>\n<ul class=\"toc\">\n")
                for (ref in dated) append(tocItem(ref))
                append("</ul>\n")
            }
            if (whole.isNotEmpty()) {
                append("<h2>").append(ReadablePage.escape(words.wholeHeading))
                append("</h2>\n<ul class=\"toc\">\n")
                for (ref in whole) append(tocItem(ref))
                append("</ul>\n")
            }

            append("<h2>").append(ReadablePage.escape(words.howToHeading)).append("</h2>\n")
            append("<p>").append(howTo(words.howToBody)).append("</p>\n")
        }
        return ReadablePage.render(
            title = source.subjectName ?: words.subjectFallback,
            lang = words.lang,
            dir = words.dir,
            body = body,
        )
    }

    /**
     * The closing paragraph, with its two paths set in `<code>`.
     *
     * **Escaped first, substituted second, and the order is the whole of it.**
     * What goes in is a sentence a translator wrote, which may contain anything;
     * what comes out of the placeholders is markup this file controls. Escaping
     * after substitution would print the tags, and substituting without escaping
     * would let a catalog string carry HTML into the page.
     *
     * The braces survive escaping untouched, which is what makes this safe: the
     * escaper rewrites five characters and none of them is a brace.
     */
    private fun howTo(template: String): String =
        ReadablePage.escape(template)
            .replace("{database}", "<code>data/trail.sqlite</code>")
            .replace("{attachments}", "<code>attachments</code>")

    private fun tocItem(ref: PageRef): String =
        "<li><a href=\"${ReadablePage.escape(ref.path)}\">${ReadablePage.escape(ref.title)}</a>" +
            " <span class=\"count\">${ref.count}</span></li>\n"

    /** One page's worth of items. */
    private fun section(
        title: String,
        rows: List<Row>,
        fields: TableFields,
        labels: Labels,
    ): String = buildString {
        append("<h1>").append(ReadablePage.escape(title)).append("</h1>\n")
        append("<p class=\"sub\">")
        append(ReadablePage.escape(labels.words.records(rows.size)))
        append("</p>\n")
        for (row in rows) append(item(row, fields, labels))
    }

    /**
     * One record, with **every rendered field on it**.
     *
     * The heading is whatever the record calls itself, so a reader sees the
     * person's own words rather than a table name and a row number. Everything
     * else follows as labeled fields, including the ones the app's own screens
     * would fold away: this is the copy that has to be complete, not the calm
     * one.
     */
    private fun item(row: Row, fields: TableFields, labels: Labels): String = buildString {
        append("<div class=\"item\">\n")
        val heading = row["title"] ?: row["display_name"] ?: row["name"] ?: row["question"]
        if (!heading.isNullOrBlank()) {
            append("<h3>").append(ReadablePage.escape(heading)).append("</h3>\n")
        }
        append("<dl>\n")
        for ((column, decision) in fields.rendered) {
            if (column == "title" && !heading.isNullOrBlank()) continue
            if (column == "display_name" && !heading.isNullOrBlank()) continue
            if (column == "name" && !heading.isNullOrBlank()) continue
            append(renderField(column, decision, row, labels))
        }
        append("</dl>\n")
        append("</div>\n")
    }

    private fun renderField(
        column: String,
        decision: String,
        row: Row,
        labels: Labels,
    ): String {
        val label = labels.column(column)
        val words = labels.words
        return when (decision) {
            "id" -> "<div class=\"f\"><dt>${ReadablePage.escape(label)}</dt>" +
                "<dd class=\"id\">${ReadablePage.escape(row[column].orEmpty())}</dd></div>"

            "date" -> {
                val zone = row[column.removeSuffix("_edtf") + "_zone"]
                ReadablePage.field(label, ReadableDate.render(row[column], zone), words.notRecorded)
            }

            // The zone is rendered as part of its own date rather than on its
            // own, so it is not printed twice.
            "dateZone" -> ""

            // **A flag reads as a word.** SQLite stores it as 0 or 1, and a
            // reader handed a column of noughts learns nothing: "Still waiting
            // to be filed: 0" is a sentence with no meaning outside a database.
            //
            // A missing flag is not the same as a false one and does not become
            // "no". Section 8.2 keeps null, empty and zero as three different
            // things, and a flag nobody set is not a flag somebody cleared.
            "boolean" -> when (row[column]) {
                null, "" -> ReadablePage.notRecorded(label, words.notRecorded)
                "0" -> ReadablePage.field(label, words.no, words.notRecorded)
                else -> ReadablePage.field(label, words.yes, words.notRecorded)
            }

            // **The one link that leaves the page, and it goes nowhere near a
            // network.** `contract/DATA-CONTRACT.md` 8.2 requires every
            // attachment referenced by a relative path into `../attachments/`
            // and never as base64. The file's name **is** its SHA-256, so this
            // column is the checksum and the path at once.
            //
            // The index page has promised since it was written that "the links
            // here point straight at them". It was not true: a real export
            // carried forty photographs and zero references to them, so a person
            // reading the prose had no way to know a picture of that letter
            // existed. Found by unsealing an archive and grepping it.
            "attachment" -> {
                val digest = row[column]
                if (digest.isNullOrBlank()) {
                    ReadablePage.notRecorded(label, words.notRecorded)
                } else {
                    // **`ReadablePage.attachment` existed and nothing called
                    // it.** It was written when the page shell was, for exactly
                    // this, and the pages have been shipping without a single
                    // link since. A helper nobody calls is not a feature.
                    "<div class=\"f\"><dt>${ReadablePage.escape(label)}</dt>" +
                        "<dd>${ReadablePage.attachment(digest, digest)}</dd></div>"
                }
            }

            "link" -> {
                val target = row[column]
                if (target.isNullOrBlank()) {
                    ReadablePage.notRecorded(label, words.notRecorded)
                } else {
                    ReadablePage.field(label, labels.nameFor(column, target), words.notRecorded)
                }
            }

            else -> ReadablePage.field(label, row[column], words.notRecorded)
        }
    }

    /**
     * The year bucket for rows with no date, as it appears in a file name.
     *
     * **The path is never translated**, so this is the English word in every
     * language and it is a path segment rather than prose. What a reader sees is
     * `Words.undated`.
     */
    private const val UNDATED = "undated"

    private fun yearOf(value: String?): String {
        if (value.isNullOrBlank()) return UNDATED
        // An epoch millisecond column, for the few dated tables that carry one.
        value.toLongOrNull()?.let { millis ->
            return java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneOffset.UTC).year.toString()
        }
        val year = value.take(4)
        return if (year.length == 4 && year.all { it.isDigit() }) year else UNDATED
    }

    /** A table's rendering decisions, in a fixed order. */
    data class TableFields(val rendered: List<Pair<String, String>>)

    /**
     * Human words for table and column names.
     *
     * **A reader never sees a column name.** `role_label` is "Their role" and
     * `occurred_edtf` is "When". A document that shows its own schema is a data
     * dump wearing a stylesheet, and section 8.2's whole standard is that a
     * stranger can read it.
     *
     * **The words come from [Words] and this class only chooses between them.**
     * They were two hard-coded English maps until 2026-08-09, which is why an
     * Arabic archive was a correctly mirrored page of English. #327.
     *
     * Anything without a specific word falls back to the column name with its
     * underscores opened out and its suffixes dropped, which is not elegant and
     * is honest: it is visibly a field rather than pretending to be prose.
     * **In the shipped app that fallback is unreachable**, because
     * `check_readable_labels.py` fails the build when a rendered table or column
     * has no label in all four catalogs. It is here for a caller holding a
     * partial vocabulary, which in practice means a test.
     */
    private class Labels(val source: Source) {

        val words: Words get() = source.words

        private val names: Map<String, String> = buildMap {
            for ((table, rows) in source.tables) {
                for (row in rows) {
                    val id = row["id"] ?: continue
                    val name = row["display_name"] ?: row["title"] ?: row["name"] ?: continue
                    put(id, name)
                }
            }
        }

        fun nameFor(column: String, id: String): String = names[id] ?: id

        fun table(table: String): String = words.tables[table] ?: open(table)

        fun column(column: String): String = words.columns[column] ?: open(
            column.removeSuffix("_edtf").removeSuffix("_id").removeSuffix("_at"),
        )

        private fun open(raw: String): String =
            raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
