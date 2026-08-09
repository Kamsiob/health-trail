package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `contract/DATA-CONTRACT.md` section 8.2, the readable copy.
 *
 * These run at the shapes 8.5's shape test names, empty and one and a few,
 * because an archive of an almost-empty notebook and an archive of a five year
 * notebook fail in different ways.
 */
class ReadableArchiveTest {

    private val fields = mapOf(
        "entry" to ReadableArchive.TableFields(
            listOf(
                "id" to "id",
                "title" to "value",
                "body" to "value",
                "occurred_edtf" to "date",
                "occurred_zone" to "dateZone",
                "chapter_id" to "link",
            ),
        ),
        "person" to ReadableArchive.TableFields(
            listOf(
                "id" to "id",
                "display_name" to "value",
                "role_label" to "value",
                "phone" to "value",
            ),
        ),
        "chapter" to ReadableArchive.TableFields(
            listOf("id" to "id", "name" to "value"),
        ),
    )

    /**
     * The vocabulary the renderer is handed, standing in for the catalogs.
     *
     * **English here, and that is the fixture rather than the product.** What
     * the app passes comes from `ReadableWords.from`, which reads the four
     * catalogs. These tests are about what the renderer does with a vocabulary,
     * not about which one; the test lower down that renders in Arabic is what
     * proves it uses the one it is given.
     */
    private fun words(
        lang: String = "en",
        dir: String = "ltr",
        tables: Map<String, String> = mapOf(
            "entry" to "The trail", "person" to "The care team", "chapter" to "Chapters",
        ),
        columns: Map<String, String> = mapOf(
            "id" to "Reference", "title" to "What it was", "body" to "What was written",
            "occurred_edtf" to "When", "chapter_id" to "Chapter",
            "display_name" to "Name", "role_label" to "Their role", "phone" to "Phone",
            "name" to "Name", "is_unfiled" to "Still waiting to be filed",
        ),
        notRecorded: String = "not recorded",
        yes: String = "yes",
        no: String = "no",
    ) = ReadableArchive.Words(
        lang = lang,
        dir = dir,
        tables = tables,
        columns = columns,
        subjectFallback = "This record",
        about = "A copy of a care notebook, written by the person who kept it. " +
            "It is not a clinical record and nothing in it is advice.",
        datedHeading = "What happened, by year",
        wholeHeading = "The people, places and things it refers to",
        howToHeading = "How to read this",
        howToBody = "Every entry shows the id it has in {database}, and the files are " +
            "in the {attachments} folder beside this one.",
        back = "Back to the front page",
        undated = "undated",
        notRecorded = notRecorded,
        yes = yes,
        no = no,
        covers = { from, to -> if (from == to) "Covers $from" else "Covers $from to $to" },
        yearTitle = { section, year -> "$section, $year" },
        records = { count -> if (count == 1) "1 record." else "$count records." },
    )

    private fun source(
        entries: List<Map<String, String?>> = emptyList(),
        people: List<Map<String, String?>> = emptyList(),
        chapters: List<Map<String, String?>> = emptyList(),
        words: ReadableArchive.Words = words(),
    ) = ReadableArchive.Source(
        tables = mapOf("entry" to entries, "person" to people, "chapter" to chapters),
        words = words,
        subjectName = "Ruth Baxter",
    )

    private fun entry(
        id: String,
        title: String? = "Called the unit",
        body: String? = "They said they would call back.",
        edtf: String? = "2026-07-06T14:30",
        zone: String? = "America/New_York",
        chapter: String? = null,
    ) = mapOf(
        "id" to id, "title" to title, "body" to body,
        "occurred_edtf" to edtf, "occurred_zone" to zone, "chapter_id" to chapter,
    )

    @Test
    fun `an empty notebook still produces a front door`() {
        // The shape test in 8.5: empty is one of the four, and a person who
        // exports on day one should get a file that opens rather than nothing.
        val pages = ReadableArchive.render(source(), fields)
        assertEquals(setOf("index.html"), pages.keys)
        assertTrue(pages.getValue("index.html").contains("Ruth Baxter"))
    }

    @Test
    fun `the index is first, so an alphabetical folder listing shows it first`() {
        val pages = ReadableArchive.render(source(entries = listOf(entry("e1"))), fields)
        assertEquals("index.html", pages.keys.first())
    }

    @Test
    fun `dated sections split by year`() {
        // Section 8.2: one page per section per year, so no single page becomes
        // unopenable at year five.
        val pages = ReadableArchive.render(
            source(
                entries = listOf(
                    entry("e1", edtf = "2024-03-02"),
                    entry("e2", edtf = "2026-07-06"),
                    entry("e3", edtf = "2026-08-01"),
                ),
            ),
            fields,
        )
        assertTrue(pages.containsKey("entry-2024.html"))
        assertTrue(pages.containsKey("entry-2026.html"))
        assertTrue(pages.getValue("entry-2026.html").contains("2 records"))
    }

    @Test
    fun `an entry with no date lands on an undated page rather than being dropped`() {
        // Unknown is a first-class value, CLAUDE.md rule 17. An entry whose date
        // nobody knows is still part of the record and must not vanish from the
        // archive because it could not be filed under a year.
        val pages = ReadableArchive.render(
            source(entries = listOf(entry("e1", edtf = null))), fields,
        )
        assertTrue(pages.containsKey("entry-undated.html"))
        assertTrue(pages.getValue("entry-undated.html").contains("Called the unit"))
    }

    @Test
    fun `every entry prints its id, so the two halves of the archive can be matched by hand`() {
        // Section 8.2, and the reason is that a person or a future tool can move
        // between the readable copy and data/trail.sqlite without this app.
        val pages = ReadableArchive.render(source(entries = listOf(entry("01J8Z9K2QF"))), fields)
        assertTrue(pages.getValue("entry-2026.html").contains("01J8Z9K2QF"))
    }

    @Test
    fun `a reference renders as the name of the thing rather than a uuid`() {
        val pages = ReadableArchive.render(
            source(
                entries = listOf(entry("e1", chapter = "c1")),
                chapters = listOf(mapOf("id" to "c1", "name" to "Maplewood Care Center")),
            ),
            fields,
        )
        val page = pages.getValue("entry-2026.html")
        assertTrue(page.contains("Maplewood Care Center"))
    }

    @Test
    fun `a reference to something not in the file still shows something`() {
        // Import halts on a dangling reference, 8.3, so this should not happen.
        // If it ever does, the archive shows the id rather than an empty field,
        // because a visible id is a lead and a blank is a dead end.
        val pages = ReadableArchive.render(
            source(entries = listOf(entry("e1", chapter = "missing-id"))), fields,
        )
        assertTrue(pages.getValue("entry-2026.html").contains("missing-id"))
    }

    @Test
    fun `an unfilled field reads as not recorded`() {
        val pages = ReadableArchive.render(
            source(entries = listOf(entry("e1", body = null))), fields,
        )
        assertTrue(pages.getValue("entry-2026.html").contains("not recorded"))
    }

    @Test
    fun `a reader never sees a column name`() {
        // Section 8.2's standard is that a stranger can read it. A document that
        // shows its own schema is a data dump wearing a stylesheet.
        val pages = ReadableArchive.render(
            source(
                entries = listOf(entry("e1", chapter = "c1")),
                chapters = listOf(mapOf("id" to "c1", "name" to "Home")),
            ),
            fields,
        )
        val page = pages.getValue("entry-2026.html")
        for (raw in listOf("occurred_edtf", "chapter_id", "display_name", "role_label")) {
            assertFalse("column name leaked: $raw", page.contains(raw))
        }
    }

    @Test
    fun `the zone is not printed as its own field`() {
        val pages = ReadableArchive.render(source(entries = listOf(entry("e1"))), fields)
        val page = pages.getValue("entry-2026.html")
        // It appears inside the rendered date as an offset, not as a row saying
        // "America/New_York".
        assertFalse(page.contains("America/New_York"))
        assertTrue(page.contains("UTC-04:00"))
    }

    @Test
    fun `reference tables are rendered whole rather than split by year`() {
        val pages = ReadableArchive.render(
            source(
                people = listOf(
                    mapOf("id" to "p1", "display_name" to "Angela Reyes",
                          "role_label" to "Charge nurse", "phone" to "555 0142"),
                ),
            ),
            fields,
        )
        assertTrue(pages.containsKey("person.html"))
        assertTrue(pages.getValue("person.html").contains("Angela Reyes"))
    }

    @Test
    fun `the same rows produce byte identical output`() {
        // 8.5's regeneration test rests entirely on this: export, import onto a
        // clean install, regenerate, assert byte identical. If the same rows can
        // produce two different files, that test can never pass.
        val rows = source(
            entries = listOf(entry("e2", edtf = "2026-07-06"), entry("e1", edtf = "2026-02-01")),
            people = listOf(mapOf("id" to "p1", "display_name" to "A", "role_label" to null, "phone" to null)),
            chapters = listOf(mapOf("id" to "c1", "name" to "Home")),
        )
        assertEquals(ReadableArchive.render(rows, fields), ReadableArchive.render(rows, fields))
    }

    @Test
    fun `row order does not change the output`() {
        // Rows arrive in whatever order the query gave them. The archive sorts,
        // so an export run twice against the same database cannot differ because
        // SQLite returned rows differently.
        val forward = ReadableArchive.render(
            source(entries = listOf(entry("e1", edtf = "2026-01-01"), entry("e2", edtf = "2026-02-01"))),
            fields,
        )
        val backward = ReadableArchive.render(
            source(entries = listOf(entry("e2", edtf = "2026-02-01"), entry("e1", edtf = "2026-01-01"))),
            fields,
        )
        assertEquals(forward, backward)
    }

    @Test
    fun `every page carries the direction and language the person used`() {
        val pages = ReadableArchive.render(
            source(entries = listOf(entry("e1")), words = words(lang = "ar", dir = "rtl")), fields,
        )
        for ((path, html) in pages) {
            assertTrue(path, html.contains("""<html lang="ar" dir="rtl">"""))
        }
    }

    @Test
    fun `every word on the page is the one it was handed, in the person's language`() {
        // #327. The pages carried `lang="ar" dir="rtl"` and not one Arabic word
        // of their own, because the table and column names were hard-coded
        // English maps in the renderer. A correctly mirrored page of English is
        // the failure that looks most like success, so this asserts the words
        // rather than the direction.
        val arabic = words(
            lang = "ar",
            dir = "rtl",
            tables = mapOf("entry" to "الأثر", "chapter" to "الفصول"),
            columns = mapOf(
                "id" to "المرجع", "title" to "ما كان", "body" to "ما كُتب",
                "occurred_edtf" to "متى", "chapter_id" to "الفصل",
            ),
            notRecorded = "غير مُدوَّن",
        )
        val page = ReadableArchive.render(
            source(entries = listOf(entry("e1", body = null)), words = arabic), fields,
        ).getValue("entry-2026.html")

        // The section heading, a field label, and the phrase for a field the
        // person never filled. Not the title's own label: a record that names
        // itself uses that as its heading, so the label is not drawn.
        assertTrue(page.contains("الأثر"))
        assertTrue(page.contains("متى"))
        assertTrue(page.contains("المرجع"))
        assertTrue(page.contains("غير مُدوَّن"))
        // The English the renderer used to hold, gone rather than merely joined.
        for (english in listOf("The trail", "When", "Reference", "not recorded")) {
            assertFalse("English survived into an Arabic page: $english", page.contains(english))
        }
    }

    @Test
    fun `a page path stays in the schema's own words even when the page does not`() {
        // A file name is an address: the index links to it, a person may have
        // bookmarked it, and some file systems mangle non-ASCII. So the title
        // translates and the path does not, and the same notebook exported in
        // two languages still produces two folders that can be compared.
        val pages = ReadableArchive.render(
            source(
                entries = listOf(entry("e1"), entry("e2", edtf = null)),
                words = words(lang = "ar", dir = "rtl", tables = mapOf("entry" to "الأثر")),
            ),
            fields,
        )
        assertTrue(pages.containsKey("entry-2026.html"))
        assertTrue(pages.containsKey("entry-undated.html"))
    }

    @Test
    fun `the two paths in the closing paragraph survive as markup rather than as tags`() {
        // The paragraph is escaped and then substituted, in that order. Escaping
        // afterward would print the tags; substituting into unescaped text would
        // let a catalog string carry HTML onto the page.
        val index = ReadableArchive.render(source(), fields).getValue("index.html")
        assertTrue(index.contains("<code>data/trail.sqlite</code>"))
        assertTrue(index.contains("<code>attachments</code>"))
        assertFalse(index.contains("{database}"))
        assertFalse(index.contains("{attachments}"))
    }

    @Test
    fun `a bill and a document are filed under the year the person recorded`() {
        // #327 found this: DATED named `issued_edtf` on bill and `dated_edtf` on
        // document and neither column exists, so every bill and every document
        // ever exported landed on one undated page. A missing column reads as
        // null, null is a real bucket, and nothing failed.
        val paperFields = mapOf(
            "bill" to ReadableArchive.TableFields(listOf("id" to "id", "received_edtf" to "date")),
            "document" to ReadableArchive.TableFields(listOf("id" to "id", "received_edtf" to "date")),
        )
        val pages = ReadableArchive.render(
            ReadableArchive.Source(
                tables = mapOf(
                    "bill" to listOf(mapOf("id" to "b1", "received_edtf" to "2025-04-02")),
                    "document" to listOf(mapOf("id" to "d1", "received_edtf" to "2026-01-09")),
                ),
                words = words(
                    tables = mapOf("bill" to "Bills", "document" to "Documents"),
                    columns = mapOf("id" to "Reference", "received_edtf" to "Received"),
                ),
                subjectName = "Ruth Baxter",
            ),
            paperFields,
        )
        assertTrue(pages.containsKey("bill-2025.html"))
        assertTrue(pages.containsKey("document-2026.html"))
        assertFalse(pages.containsKey("bill-undated.html"))
        assertFalse(pages.containsKey("document-undated.html"))
    }

    @Test
    fun `every page links back to the front door except the front door`() {
        val pages = ReadableArchive.render(source(entries = listOf(entry("e1"))), fields)
        for ((path, html) in pages) {
            if (path == "index.html") continue
            assertTrue(path, html.contains("""href="index.html""""))
        }
    }

    @Test
    fun `the index links every page it names`() {
        val pages = ReadableArchive.render(
            source(
                entries = listOf(entry("e1", edtf = "2024-01-01"), entry("e2", edtf = "2026-01-01")),
                people = listOf(mapOf("id" to "p1", "display_name" to "A", "role_label" to null, "phone" to null)),
            ),
            fields,
        )
        val index = pages.getValue("index.html")
        for (path in pages.keys - "index.html") {
            assertTrue("index does not link $path", index.contains("""href="$path""""))
        }
    }

    @Test
    fun `the index says the range the record covers`() {
        val pages = ReadableArchive.render(
            source(entries = listOf(entry("e1", edtf = "2022-01-01"), entry("e2", edtf = "2026-01-01"))),
            fields,
        )
        val index = pages.getValue("index.html")
        assertTrue(index.contains("2022"))
        assertTrue(index.contains("2026"))
    }

    @Test
    fun `a flag reads as a word rather than as a digit`() {
        // Found on a real five year export: "Still waiting to be filed: 0" is a
        // sentence with no meaning outside a database. SQLite stores a flag as
        // 0 or 1 and a reader handed a column of noughts learns nothing.
        val flagged = mapOf(
            "entry" to ReadableArchive.TableFields(
                listOf("id" to "id", "is_unfiled" to "boolean"),
            ),
        )
        fun render(value: String?) = ReadableArchive.render(
            ReadableArchive.Source(
                tables = mapOf("entry" to listOf(mapOf(
                    "id" to "e1", "is_unfiled" to value, "occurred_edtf" to "2026-01-01",
                ))),
                words = words(), subjectName = "R",
            ),
            flagged,
        ).getValue("entry-2026.html")

        assertTrue(render("1").contains(">yes<"))
        assertTrue(render("0").contains(">no<"))
        assertFalse(render("0").contains(">0<"))
    }

    @Test
    fun `a flag nobody set is not a flag somebody cleared`() {
        // Section 8.2 keeps null, empty and zero as three different things.
        val flagged = mapOf(
            "entry" to ReadableArchive.TableFields(
                listOf("id" to "id", "is_unfiled" to "boolean"),
            ),
        )
        val html = ReadableArchive.render(
            ReadableArchive.Source(
                tables = mapOf("entry" to listOf(mapOf(
                    "id" to "e1", "is_unfiled" to null, "occurred_edtf" to "2026-01-01",
                ))),
                words = words(), subjectName = "R",
            ),
            flagged,
        ).getValue("entry-2026.html")
        assertTrue(html.contains("not recorded"))
        assertFalse(html.contains(">no<"))
    }

    @Test
    fun `the front page says plainly that this is not a clinical record`() {
        // Rule 2 and section 8.2: the readable copy carries the app's own
        // content rules. A document that looks official and is not is worse
        // than one that says what it is.
        val index = ReadableArchive.render(source(), fields).getValue("index.html")
        assertTrue(index.contains("not a clinical record"))
        assertTrue(index.contains("not advice") || index.contains("nothing in it is advice"))
    }
}
