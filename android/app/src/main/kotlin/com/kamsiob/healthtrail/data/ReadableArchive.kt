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
 */
internal object ReadableArchive {

    /** One row, as column name to value. Null is a null column, not an empty one. */
    typealias Row = Map<String, String?>

    /**
     * What the renderer needs, which is deliberately less than a database.
     *
     * @param tables every table's live rows, already tombstone-filtered by the
     *   caller. A deleted row is absent from the readable copy by definition:
     *   printing that something was deleted would put back what the person
     *   removed.
     * @param lang the BCP 47 tag the person used the app in.
     * @param dir "rtl" or "ltr".
     * @param subjectName who the record is about, for the front door.
     */
    data class Source(
        val tables: Map<String, List<Row>>,
        val lang: String,
        val dir: String,
        val subjectName: String?,
    )

    /**
     * Sections that grow over time, so they are split by year.
     *
     * **One page per section per year**, section 8.2, so no single page becomes
     * unopenable at year five. A five year trail is 1,630 entries and a browser
     * asked to lay that out at once on an old phone will simply stop.
     *
     * The pair is the table and the column its year comes from.
     */
    private val DATED = listOf(
        "entry" to "occurred_edtf",
        "incident" to "reported_edtf",
        "appointment" to "scheduled_edtf",
        "measurement" to "occurred_edtf",
        "medication_event" to "occurred_edtf",
        "bill" to "issued_edtf",
        "document" to "dated_edtf",
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
    )

    /**
     * Render the whole readable folder.
     *
     * @return path inside the archive to file contents, for every page.
     */
    fun render(source: Source, fieldMap: Map<String, TableFields>): Map<String, String> {
        val pages = LinkedHashMap<String, String>()
        val labels = Labels(source, fieldMap)

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
                val path = "$table-$year.html"
                val title = "${labels.table(table)}, $year"
                pages[path] = ReadablePage.render(
                    title = title,
                    lang = source.lang,
                    dir = source.dir,
                    upLink = "index.html",
                    upLabel = "Back to the front page",
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
                lang = source.lang,
                dir = source.dir,
                upLink = "index.html",
                upLabel = "Back to the front page",
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
        val body = buildString {
            append("<h1>")
            append(ReadablePage.escape(source.subjectName ?: "This record"))
            append("</h1>\n")
            append("<p class=\"sub\">")
            append(
                "A copy of a care notebook, written by the person who kept it. " +
                    "Everything here is their own notes. It is not a clinical record " +
                    "and nothing in it is advice.",
            )
            append("</p>\n")

            val years = dated.mapNotNull { it.path.substringAfterLast('-').removeSuffix(".html").toIntOrNull() }
            if (years.isNotEmpty()) {
                append("<p><strong>Covers ")
                append(years.min())
                if (years.min() != years.max()) {
                    append(" to ").append(years.max())
                }
                append("</strong></p>\n")
            }

            if (dated.isNotEmpty()) {
                append("<h2>What happened, by year</h2>\n<ul class=\"toc\">\n")
                for (ref in dated) append(tocItem(ref))
                append("</ul>\n")
            }
            if (whole.isNotEmpty()) {
                append("<h2>The people, places and things it refers to</h2>\n<ul class=\"toc\">\n")
                for (ref in whole) append(tocItem(ref))
                append("</ul>\n")
            }

            append("<h2>How to read this</h2>\n")
            append(
                "<p>Every entry shows the id it has in <code>data/trail.sqlite</code>, " +
                    "so anything here can be matched to the machine copy by hand. " +
                    "Photographs and documents are in the <code>attachments</code> " +
                    "folder beside this one, and the links here point straight at them. " +
                    "Nothing on these pages needs an internet connection.</p>\n",
            )
        }
        return ReadablePage.render(
            title = source.subjectName ?: "This record",
            lang = source.lang,
            dir = source.dir,
            body = body,
        )
    }

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
        append("<p class=\"sub\">").append(rows.size)
        append(if (rows.size == 1) " record." else " records.")
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
        return when (decision) {
            "id" -> "<div class=\"f\"><dt>${ReadablePage.escape(label)}</dt>" +
                "<dd class=\"id\">${ReadablePage.escape(row[column].orEmpty())}</dd></div>"

            "date" -> {
                val zone = row[column.removeSuffix("_edtf") + "_zone"]
                ReadablePage.field(label, ReadableDate.render(row[column], zone))
            }

            // The zone is rendered as part of its own date rather than on its
            // own, so it is not printed twice.
            "dateZone" -> ""

            "link" -> {
                val target = row[column]
                if (target.isNullOrBlank()) {
                    ReadablePage.notRecorded(label)
                } else {
                    ReadablePage.field(label, labels.nameFor(column, target))
                }
            }

            else -> ReadablePage.field(label, row[column])
        }
    }

    private fun yearOf(value: String?): String {
        if (value.isNullOrBlank()) return "undated"
        // An epoch millisecond column, for the few dated tables that carry one.
        value.toLongOrNull()?.let { millis ->
            return java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneOffset.UTC).year.toString()
        }
        val year = value.take(4)
        return if (year.length == 4 && year.all { it.isDigit() }) year else "undated"
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
     * Anything without a specific word falls back to the column name with its
     * underscores opened out and its suffixes dropped, which is not elegant and
     * is honest: it is visibly a field rather than pretending to be prose.
     */
    private class Labels(val source: Source, val fieldMap: Map<String, TableFields>) {
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

        fun table(table: String): String = TABLES[table] ?: open(table)

        fun column(column: String): String = COLUMNS[column] ?: open(
            column.removeSuffix("_edtf").removeSuffix("_id").removeSuffix("_at"),
        )

        private fun open(raw: String): String =
            raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private val TABLES = mapOf(
        "entry" to "The trail",
        "incident" to "Incidents",
        "appointment" to "Appointments",
        "measurement" to "Measurements",
        "medication_event" to "Medication changes",
        "bill" to "Bills",
        "document" to "Documents",
        "question" to "Questions",
        "milestone" to "Milestones",
        "cost_entry" to "Costs",
        "instruction_violation" to "Times an instruction was not followed",
        "subject" to "Who this is about",
        "person" to "The care team",
        "organization" to "Places and organizations",
        "chapter" to "Chapters",
        "care_thread" to "Care threads",
        "medication" to "Medications",
        "medication_flag" to "Concerns raised about a medication",
        "measure" to "What was tracked",
        "project" to "Projects",
        "project_step" to "Project steps",
        "standing_instruction" to "Standing instructions",
        "emergency_card" to "Emergency card",
        "emergency_contact" to "Emergency contacts",
        "cost_sheet" to "Cost sheets",
        "custom_template" to "Templates the person made",
        "attachment" to "Files",
        "link" to "Connections between records",
        "person_chapter" to "Who was involved in each chapter",
        "entry_thread" to "Which thread each entry belongs to",
        "entry_person" to "Who each entry involved",
        "project_person" to "Who each project involved",
        "call_detail" to "Details of calls",
        "visit_detail" to "Details of visits",
    )

    private val COLUMNS = mapOf(
        "id" to "Reference",
        "title" to "What it was",
        "body" to "What was written",
        "display_name" to "Name",
        "role_label" to "Their role",
        "occurred_edtf" to "When",
        "reported_edtf" to "Reported",
        "scheduled_edtf" to "Scheduled for",
        "issued_edtf" to "Dated",
        "dated_edtf" to "Dated",
        "started_edtf" to "Started",
        "ended_edtf" to "Ended",
        "resolved_at" to "Answered",
        "resolution_note" to "How it was answered",
        "shift_note" to "Which shift",
        "phone" to "Phone",
        "email" to "Email",
        "notes" to "Notes",
        "note" to "Note",
        "description" to "Description",
        "kind" to "Kind",
        "unit" to "Unit",
        "value_number" to "Value",
        "value_text" to "Value as written",
        "archived_at" to "Archived",
        "pinned_at" to "Pinned",
        "is_unfiled" to "Still waiting to be filed",
        "suggested_home" to "Where the app suggested it went",
        "subject_id" to "Who this is about",
        "chapter_id" to "Chapter",
        "incident_id" to "Incident",
        "organization_id" to "Organization",
        "measure_id" to "What was tracked",
        "entry_id" to "Entry",
        "reported_by_person_id" to "Reported by",
        "source" to "Where it came from",
    )
}
