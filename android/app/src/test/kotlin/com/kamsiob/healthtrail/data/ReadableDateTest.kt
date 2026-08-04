package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `contract/DATA-CONTRACT.md` section 8.2's date rule, which is three
 * prohibitions and one requirement, each pinned here.
 */
class ReadableDateTest {

    @Test
    fun `never a locale-ambiguous numeric date`() {
        // 03/04/2027 is March in the United States and April nearly everywhere
        // else. A care record read by a sibling abroad, or by a lawyer years
        // later, cannot carry a date that means two things.
        val rendered = ReadableDate.render("2027-03-04", "America/New_York")
        assertTrue(rendered.contains("March"))
        assertFalse(rendered.contains("03/04"))
        assertFalse(rendered.contains("3/4"))
    }

    @Test
    fun `never a bare epoch number`() {
        val rendered = ReadableDate.timestamp(1_753_977_600_000L, "America/New_York")
        assertFalse(rendered.contains("1753977600000"))
        assertTrue(rendered.contains("2025"))
    }

    @Test
    fun `a moment carries the offset the person was standing in`() {
        // The offset is the evidence that the reading is the one the person saw
        // rather than a recalculation into wherever the file is opened.
        val newYork = ReadableDate.render("2026-07-06T21:00", "America/New_York")
        assertTrue(newYork, newYork.contains("21:00"))
        assertTrue(newYork, newYork.contains("UTC-04:00"))
    }

    @Test
    fun `the same wall clock reading in another zone keeps its own offset`() {
        val berlin = ReadableDate.render("2026-07-06T21:00", "Europe/Berlin")
        assertTrue(berlin, berlin.contains("21:00"))
        assertTrue(berlin, berlin.contains("UTC+02:00"))
    }

    @Test
    fun `a note written on July 6 still reads July 6`() {
        // The named failure mode in 8.4, stated as a sentence: "A note written
        // on July 6 must still read July 6 after the person moves to another
        // country."
        val written = ReadableDate.render("2026-07-06T21:00", "America/New_York")
        assertTrue(written.contains("July 6, 2026"))
    }

    @Test
    fun `a month stays a month and is never given a day`() {
        // DESIGN.md 9.2 and CLAUDE.md rule 17. "November 1, 2024" for an input
        // of "sometime in November" is a fabrication, and one that survives into
        // a document somebody may rely on.
        val rendered = ReadableDate.render("2024-11", "America/New_York")
        assertTrue(rendered, rendered.contains("November"))
        assertTrue(rendered, rendered.contains("2024"))
        assertFalse(rendered, rendered.contains("November 1,"))
    }

    @Test
    fun `a year stays a year`() {
        assertEquals("2024", ReadableDate.render("2024", "UTC"))
    }

    @Test
    fun `unknown is a real answer and says so`() {
        for (nothing in listOf(null, "", "   ")) {
            assertEquals(ReadableDate.UNKNOWN, ReadableDate.render(nothing, "UTC"))
        }
    }

    @Test
    fun `uncertainty is said in words rather than punctuation`() {
        // EDTF keeps uncertainty separate from precision and so does the app. A
        // stranger reading the archive does not know what a trailing question
        // mark means, so it is spelled out.
        val uncertain = ReadableDate.render("2024-11-18?", "UTC")
        assertTrue(uncertain, uncertain.contains("not sure"))
        assertFalse(uncertain, uncertain.endsWith("?"))
    }

    @Test
    fun `approximate is said in words too`() {
        val approximate = ReadableDate.render("2024-11~", "UTC")
        assertTrue(approximate, approximate.contains("approximately"))
    }

    @Test
    fun `rendering is deterministic`() {
        // 8.5's regeneration test rests on this.
        assertEquals(
            ReadableDate.render("2026-07-06T21:00", "America/New_York"),
            ReadableDate.render("2026-07-06T21:00", "America/New_York"),
        )
    }

    @Test
    fun `a missing zone does not lose the date`() {
        // An older row, or one imported from a file that predates the zone
        // column. It falls back to UTC rather than refusing to render, because
        // a date with an uncertain offset is worth more than no date.
        val rendered = ReadableDate.render("2026-07-06T21:00", null)
        assertTrue(rendered, rendered.contains("July 6, 2026"))
    }
}
