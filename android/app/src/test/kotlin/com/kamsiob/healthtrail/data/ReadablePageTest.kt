package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The guarantees the archive's readable copy rests on.
 *
 * `contract/DATA-CONTRACT.md` section 8.2. Each of these is a property that
 * looks obviously true when the code is written and quietly stops being true
 * later, which is why each is pinned rather than trusted.
 */
class ReadablePageTest {

    private fun page(body: String = "<p>x</p>", dir: String = "ltr", lang: String = "en") =
        ReadablePage.render(title = "T", lang = lang, dir = dir, body = body)

    @Test
    fun `nothing reaches the network`() {
        val html = page()
        // The whole argument of the format: this file must open on a machine
        // with no network, in ten years. Anything that fetches is a thing that
        // renders differently, or not at all, on the machine where it matters.
        for (forbidden in listOf("http://", "https://", "//fonts.", "@import", "<link", "<script")) {
            assertFalse("readable page reaches out: $forbidden", html.contains(forbidden))
        }
    }

    @Test
    fun `the stylesheet is inlined into every page`() {
        // Not shared, so a page mailed on its own or printed still stands up.
        assertTrue(page().contains("<style>"))
    }

    @Test
    fun `it carries a print stylesheet with page breaks between entries`() {
        val html = page()
        assertTrue(html.contains("@media print"))
        // Section 8.2: page breaks land between entries, never inside one.
        assertTrue(html.contains("break-inside: avoid"))
    }

    @Test
    fun `direction and language are on the html element`() {
        val arabic = page(dir = "rtl", lang = "ar")
        assertTrue(arabic.contains("""<html lang="ar" dir="rtl">"""))
        assertTrue(page().contains("""<html lang="en" dir="ltr">"""))
    }

    @Test
    fun `the same input produces byte identical output`() {
        // The regeneration test in 8.5 rests entirely on this. If rendering is
        // not deterministic, that test can never pass and the strongest
        // guarantee in the format is unavailable.
        assertEquals(page(), page())
    }

    @Test
    fun `a note containing markup is escaped rather than rendered`() {
        // A care record contains quoted speech from real people. A note reading
        // "she said <no> to the aide" must come back whole, not as a swallowed
        // tag, because an archive that loses half a note has failed at its job.
        val html = ReadablePage.render(
            title = "T", lang = "en", dir = "ltr",
            body = ReadablePage.field("Note", "she said <no> & meant it", "not recorded"),
        )
        assertTrue(html.contains("she said &lt;no&gt; &amp; meant it"))
        assertFalse(html.contains("<no>"))
    }

    @Test
    fun `a title containing markup is escaped too`() {
        val html = ReadablePage.render(title = "a <b> title", lang = "en", dir = "ltr", body = "")
        assertTrue(html.contains("<title>a &lt;b&gt; title</title>"))
    }

    @Test
    fun `an unfilled field reads as not recorded, never blank and never zero`() {
        // Section 8.2. A blank cell is ambiguous between "nothing was written"
        // and "the archive lost it". A zero is a measurement nobody took being
        // reported as a measurement of zero.
        for (empty in listOf(null, "", "   ")) {
            val html = ReadablePage.field("Blood type", empty, "not recorded")
            assertTrue(html.contains("not recorded"))
            assertFalse(html.contains(">0<"))
        }
    }

    @Test
    fun `the phrase for an unfilled field comes from the caller, not from this file`() {
        // #327. This file has no locale and the page is written in the language
        // the person used the app in, so the words arrive as arguments. It said
        // "not recorded" in Arabic archives until the words were passed in.
        val html = ReadablePage.field("فصيلة الدم", null, "غير مُدوَّن")
        assertTrue(html.contains("غير مُدوَّن"))
        assertFalse(html.contains("not recorded"))
    }

    @Test
    fun `a filled field renders its value`() {
        assertTrue(ReadablePage.field("Blood type", "O positive", "not recorded").contains("O positive"))
        assertFalse(ReadablePage.field("Blood type", "O positive", "not recorded").contains("not recorded"))
    }

    @Test
    fun `attachments are relative paths, never embedded`() {
        // Section 8.2: never base64, because a three-year archive inlined into
        // one file will not open. It also means a person can open the folder and
        // find their own photographs without going through the pages at all.
        val html = ReadablePage.attachment("a1b2.jpg", "Insurance card")
        assertTrue(html.contains("""href="../attachments/a1b2.jpg""""))
        assertFalse(html.contains("base64"))
    }

    @Test
    fun `an attachment filename containing markup is escaped`() {
        assertTrue(ReadablePage.attachment("x\".jpg", "c").contains("x&quot;.jpg"))
    }
}
