package com.kamsiob.healthtrail.time

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The distance between two trail rows, as a table.
 *
 * Pure: two instants and a zone in, a key and a count out. No Android, no
 * composition, no clock, which is the shape `DigestTest` uses and the reason
 * every boundary can be checked rather than sampled.
 *
 * **What is being protected here is a piece of copy that could quietly lie.**
 * "Two months earlier" on a gap of five weeks is the app telling somebody
 * something about their own record that is not true, on a screen whose whole
 * job is to be trustworthy about dates.
 */
class DistanceTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")

    private fun at(text: String): Long =
        LocalDate.parse(text).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun gap(older: String, newer: String) =
        Distance.between(at(older), at(newer), zone)

    // -- the threshold -------------------------------------------------------

    @Test
    fun `nothing is said below the threshold`() {
        // Thirteen days. Under it the marker appears between almost every pair
        // of rows and stops being information.
        assertNull(gap("2026-08-01", "2026-08-14"))
    }

    @Test
    fun `the threshold itself is said`() {
        val result = gap("2026-08-01", "2026-08-15")
        assertEquals("trail.gap.weeks", result?.key)
        assertEquals(2L, result?.count)
    }

    @Test
    fun `the same day is nothing`() {
        assertNull(gap("2026-08-01", "2026-08-01"))
    }

    @Test
    fun `a date going the wrong way is nothing rather than a negative number`() {
        // The caller passes the older first. If it ever passes them the other
        // way round, the answer is silence rather than "minus three weeks".
        assertNull(gap("2026-08-15", "2026-08-01"))
    }

    @Test
    fun `a missing date on either side is nothing`() {
        assertNull(Distance.between(null, at("2026-08-01"), zone))
        assertNull(Distance.between(at("2026-08-01"), null, zone))
        assertNull(Distance.between(null, null, zone))
    }

    // -- the units -----------------------------------------------------------

    @Test
    fun `weeks are weeks until a month has passed`() {
        assertEquals("trail.gap.weeks", gap("2026-08-01", "2026-08-20")?.key)
        assertEquals(2L, gap("2026-08-01", "2026-08-20")?.count)

        val fourWeeks = gap("2026-08-01", "2026-08-29")
        assertEquals("trail.gap.weeks", fourWeeks?.key)
        assertEquals(4L, fourWeeks?.count)
    }

    @Test
    fun `a month is a calendar month rather than thirty days`() {
        // **The reason this is not division.** February to March is a month
        // whether it is 28 days or 29, and a person reading their own record
        // would be right to expect the calendar to be doing the work.
        val february = gap("2026-02-01", "2026-03-01")
        assertEquals("trail.gap.months", february?.key)
        assertEquals(1L, february?.count)
    }

    @Test
    fun `eleven months is months and twelve is a year`() {
        val eleven = gap("2026-01-01", "2026-12-01")
        assertEquals("trail.gap.months", eleven?.key)
        assertEquals(11L, eleven?.count)

        // Never "twelve months earlier", which reads as an app counting rather
        // than reading a calendar.
        val twelve = gap("2026-01-01", "2027-01-01")
        assertEquals("trail.gap.years", twelve?.key)
        assertEquals(1L, twelve?.count)
    }

    @Test
    fun `a long absence is years`() {
        val result = gap("2020-03-14", "2026-08-02")
        assertEquals("trail.gap.years", result?.key)
        assertEquals(6L, result?.count)
    }

    @Test
    fun `a day short of a month is still weeks`() {
        // The boundary in the other direction, so a rounding change cannot slip
        // a month in a day early.
        val result = gap("2026-08-01", "2026-08-31")
        assertEquals("trail.gap.weeks", result?.key)
        assertEquals(4L, result?.count)
    }

    // -- the zone ------------------------------------------------------------

    @Test
    fun `the gap is counted in days rather than in elapsed hours`() {
        // Two timestamps 14 days and one hour apart, and two 14 days minus one
        // hour apart, are both fourteen calendar days. A person who logged one
        // call in the morning and one in the evening did not create a gap of
        // 14.5 days, and nothing on this screen should imply they did.
        val morning = LocalDate.parse("2026-08-01").atStartOfDay(zone)
            .plusHours(9).toInstant().toEpochMilli()
        val evening = LocalDate.parse("2026-08-15").atStartOfDay(zone)
            .plusHours(21).toInstant().toEpochMilli()

        val result = Distance.between(morning, evening, zone)
        assertEquals("trail.gap.weeks", result?.key)
        assertEquals(2L, result?.count)
    }

    @Test
    fun `a gap spanning a daylight saving change is still whole days`() {
        // The United States moves its clocks on 2026-03-08. An hour appearing
        // or vanishing must not turn a four week gap into three weeks and six
        // days, which is what counting elapsed milliseconds would do.
        val result = gap("2026-02-22", "2026-03-22")
        assertEquals("trail.gap.months", result?.key)
        assertEquals(1L, result?.count)
    }
}
