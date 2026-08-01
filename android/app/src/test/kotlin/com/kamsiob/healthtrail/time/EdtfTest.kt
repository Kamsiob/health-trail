package com.kamsiob.healthtrail.time

import com.kamsiob.healthtrail.time.Edtf.Precision
import com.kamsiob.healthtrail.time.Edtf.Qualifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Event dates, against `contract/DATA-CONTRACT.md` section 3.1.
 *
 * The thing worth testing here is not that the parser works. It is that the app
 * can never claim more than the person said. Every test below is a way that
 * could go wrong: a month quietly becoming its first day, a hedge widening into
 * a different answer, an unknown date being dropped, or a string coming back
 * from an export as something else.
 *
 * A JVM test rather than an instrumented one. `java.time` is available from
 * API 26, which is this app's floor, and none of this touches a device.
 */
class EdtfTest {

    private val newYork: ZoneId = ZoneId.of("America/New_York")
    private val tokyo: ZoneId = ZoneId.of("Asia/Tokyo")

    // -- what the person said, and nothing more ----------------------------

    @Test
    fun precisionIsExpressedByTruncation() {
        assertEquals("2024", Edtf.year(2024).canonical)
        assertEquals("2024-11", Edtf.month(2024, 11).canonical)
        assertEquals("2024-11-18", Edtf.day(LocalDate.of(2024, 11, 18)).canonical)
        assertEquals(
            "2024-11-18T14:40",
            Edtf.moment(LocalDateTime.of(2024, 11, 18, 14, 40)).canonical,
        )
    }

    @Test
    fun aMonthResolvesToTheWholeMonthAndNotToItsFirstDay() {
        // The defect this whole model exists to prevent. A range collapsed to
        // its start is how "sometime in November" becomes "November 1".
        val range = Edtf.resolve(Edtf.month(2024, 11), newYork)
        val start = requireNotNull(range.start)
        val end = requireNotNull(range.end)

        assertEquals(
            LocalDate.of(2024, 11, 1).atStartOfDay(newYork).toInstant().toEpochMilli(),
            start,
        )
        assertEquals(
            LocalDate.of(2024, 12, 1).atStartOfDay(newYork).toInstant().toEpochMilli() - 1,
            end,
        )
        assertTrue("a month resolved to a single instant", end - start > 25L * 24 * 3600 * 1000)
    }

    @Test
    fun aKnownMomentHasAnIdenticalStartAndEnd() {
        val range = Edtf.resolve(Edtf.moment(LocalDateTime.of(2024, 11, 18, 14, 40)), newYork)
        assertEquals(range.start, range.end)
    }

    @Test
    fun aDayIsMidnightToMidnightLocal() {
        val range = Edtf.resolve(Edtf.day(LocalDate.of(2024, 11, 18)), newYork)
        assertEquals(
            LocalDate.of(2024, 11, 18).atStartOfDay(newYork).toInstant().toEpochMilli(),
            range.start,
        )
        assertEquals(
            LocalDate.of(2024, 11, 19).atStartOfDay(newYork).toInstant().toEpochMilli() - 1,
            range.end,
        )
    }

    @Test
    fun aWeekIsMondayThroughSundayAsTheIntervalItIs() {
        // EDTF has no week token, so a week is an interval, per the contract.
        val week = Edtf.week(LocalDate.of(2024, 11, 20))
        assertEquals("2024-11-18/2024-11-24", week.canonical)
        assertEquals(Precision.WEEK, week.precision)

        // And it reads back as a week rather than as two dates the person never
        // said, which is what lets the screen say "sometime that week".
        assertEquals(Precision.WEEK, Edtf.parse(week.canonical)?.precision)
    }

    @Test
    fun aSeasonResolvesToItsMonths() {
        val autumn = Edtf.season(2024, 23)
        assertEquals("2024-23", autumn.canonical)
        val range = Edtf.resolve(autumn, newYork)
        assertEquals(
            LocalDate.of(2024, 9, 1).atStartOfDay(newYork).toInstant().toEpochMilli(),
            range.start,
        )
        assertEquals(
            LocalDate.of(2024, 12, 1).atStartOfDay(newYork).toInstant().toEpochMilli() - 1,
            range.end,
        )
    }

    @Test
    fun winterCrossesTheYearBoundary() {
        // Resolving winter inside the calendar year would put a January event
        // outside the season the person named it by.
        val range = Edtf.resolve(Edtf.season(2024, 24), newYork)
        assertEquals(
            LocalDate.of(2024, 12, 1).atStartOfDay(newYork).toInstant().toEpochMilli(),
            range.start,
        )
        assertEquals(
            LocalDate.of(2025, 3, 1).atStartOfDay(newYork).toInstant().toEpochMilli() - 1,
            range.end,
        )
    }

    @Test
    fun aRangeRunsFromTheStartOfTheFirstToTheEndOfTheLast() {
        val range = Edtf.range(Edtf.month(2024, 11), Edtf.month(2024, 12))
        assertEquals("2024-11/2024-12", range.canonical)
        val resolved = Edtf.resolve(range, newYork)
        assertEquals(Edtf.resolve(Edtf.month(2024, 11), newYork).start, resolved.start)
        assertEquals(Edtf.resolve(Edtf.month(2024, 12), newYork).end, resolved.end)
    }

    // -- uncertainty, which is a separate axis from precision ---------------

    @Test
    fun uncertaintyIsCarriedSeparatelyFromPrecision() {
        val unsureMonth = Edtf.month(2024, 11, Qualifier.UNCERTAIN)
        assertEquals("2024-11?", unsureMonth.canonical)

        val parsed = requireNotNull(Edtf.parse("2024-11?"))
        assertEquals(Precision.MONTH, parsed.precision)
        assertEquals(Qualifier.UNCERTAIN, parsed.qualifier)

        // Knowing the month and being unsure of it is not the same as knowing
        // only the year, and the model must not be able to confuse them.
        assertEquals(Precision.YEAR, requireNotNull(Edtf.parse("2024")).precision)
    }

    @Test
    fun uncertaintyNeverWidensTheRange() {
        // Being unsure about November is still a claim about November. Widening
        // it would turn the person's hedge into a different answer.
        val sure = Edtf.resolve(Edtf.month(2024, 11), newYork)
        listOf(Qualifier.UNCERTAIN, Qualifier.APPROXIMATE, Qualifier.BOTH).forEach { q ->
            val unsure = Edtf.resolve(Edtf.month(2024, 11, q), newYork)
            assertEquals("$q widened the range", sure, unsure)
        }
    }

    @Test
    fun allThreeQualifiersParse() {
        assertEquals(Qualifier.UNCERTAIN, Edtf.parse("2024-11-18?")?.qualifier)
        assertEquals(Qualifier.APPROXIMATE, Edtf.parse("2024-11-18~")?.qualifier)
        assertEquals(Qualifier.BOTH, Edtf.parse("2024-11-18%")?.qualifier)
        assertEquals(Qualifier.NONE, Edtf.parse("2024-11-18")?.qualifier)
    }

    // -- unknown, which is a real answer ------------------------------------

    @Test
    fun unknownIsAFirstClassValueRatherThanAnAbsence() {
        val unknown = Edtf.unknown()
        assertEquals("XXXX-XX-XX", unknown.canonical)
        assertEquals(Precision.UNKNOWN, unknown.precision)

        // It parses back, which is what makes it survive an export.
        assertEquals(Precision.UNKNOWN, Edtf.parse("XXXX-XX-XX")?.precision)
    }

    @Test
    fun unknownHasNoRangeAndSaysSo() {
        val range = Edtf.resolve(Edtf.unknown(), newYork)
        assertNull(range.start)
        assertNull(range.end)
        assertTrue(range.isUnbounded)
    }

    @Test
    fun thereIsOnlyOneSpellingOfUnknown() {
        // Two spellings of one thing is a second source of truth, and being
        // unsure about a date nobody knows is not a distinction worth carrying.
        assertNull(Edtf.parse("XXXX-XX-XX?"))
    }

    // -- the local wall-clock reading ---------------------------------------

    @Test
    fun theStringKeepsTheLocalReadingAfterTravel() {
        // A visit logged at 2:40 pm happened at 2:40 pm where the person was,
        // and it still reads as 2:40 pm from anywhere else, because what is
        // stored is the reading rather than an instant.
        val logged = Edtf.moment(LocalDateTime.of(2024, 11, 18, 14, 40))
        assertEquals("2024-11-18T14:40", logged.canonical)

        // The instant it resolves to does depend on the zone, which is exactly
        // why the zone is stored beside it rather than assumed later.
        val here = Edtf.resolve(logged, newYork).start
        val there = Edtf.resolve(logged, tokyo).start
        assertTrue("the same reading resolved identically in two zones", here != there)
    }

    @Test
    fun theZoneIsStoredOnlyWherePrecisionIsFineEnoughToNeedOne() {
        // A month has no clock, and recording a zone for one would be recording
        // something the person never said.
        assertEquals("America/New_York", Edtf.zoneFor(Edtf.day(LocalDate.of(2024, 11, 18)), newYork))
        assertEquals(
            "America/New_York",
            Edtf.zoneFor(Edtf.moment(LocalDateTime.of(2024, 11, 18, 14, 40)), newYork),
        )
        assertNull(Edtf.zoneFor(Edtf.month(2024, 11), newYork))
        assertNull(Edtf.zoneFor(Edtf.year(2024), newYork))
        assertNull(Edtf.zoneFor(Edtf.unknown(), newYork))
    }

    @Test
    fun anUnknownZoneFallsBackRatherThanThrowing() {
        // What an import from a device in a zone this one has never heard of
        // looks like. It must open.
        assertNotNull(Edtf.zoneOrUtc("Mars/Olympus"))
        assertNotNull(Edtf.zoneOrUtc(null))
        assertEquals(newYork, Edtf.zoneOrUtc("America/New_York"))
    }

    // -- round tripping, which is the promise the contract makes ------------

    @Test
    fun everySupportedPrecisionRoundTrips() {
        val all = listOf(
            Edtf.moment(LocalDateTime.of(2024, 11, 18, 14, 40)),
            Edtf.day(LocalDate.of(2024, 11, 18)),
            Edtf.week(LocalDate.of(2024, 11, 20)),
            Edtf.month(2024, 11),
            Edtf.season(2024, 23),
            Edtf.year(2024),
            Edtf.range(Edtf.month(2024, 11), Edtf.month(2024, 12)),
            Edtf.unknown(),
            Edtf.month(2024, 11, Qualifier.UNCERTAIN),
            Edtf.day(LocalDate.of(2024, 11, 18), Qualifier.APPROXIMATE),
            Edtf.year(2024, Qualifier.BOTH),
        )

        all.forEach { original ->
            val reread = requireNotNull(Edtf.parse(original.canonical)) {
                "${original.canonical} did not survive a round trip"
            }
            assertEquals(original.canonical, reread.canonical)
            assertEquals(original.precision, reread.precision)
            assertEquals(original.qualifier, reread.qualifier)
            assertEquals(
                "the derived range changed across a round trip",
                Edtf.resolve(original, newYork),
                Edtf.resolve(reread, newYork),
            )
        }
    }

    // -- what must not parse -------------------------------------------------

    @Test
    fun aShapeThatNamesNoRealDayIsNotADate() {
        assertNull(Edtf.parse("2024-02-31"))
        assertNull(Edtf.parse("2024-13"))
        assertNull(Edtf.parse("2024-11-18T25:00"))
    }

    @Test
    fun aFormThisVersionDoesNotKnowIsSkippedRatherThanFailing() {
        // An export written by a later version of the app will carry forms this
        // one has never seen. It has to open. The caller keeps the string.
        assertNull(Edtf.parse("Y170000002"))
        assertNull(Edtf.parse("2024-11-18/2024-11-24/2024-12-01"))
        assertNull(Edtf.parse(""))
        assertNull(Edtf.parse("sometime last fall"))
    }
}
