package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Which rows a month gathers, and which it must not.
 *
 * **The boundary is the part that can be quietly wrong.** A review that takes
 * one row too many or one too few does not look broken: it looks like a shorter
 * month, and somebody tells a sibling that nothing happened in June. So this is
 * a table of edges rather than one happy case, the same shape `PrepTest` uses
 * for its window.
 *
 * **The precision rule is the one worth the most tests.** Rule 17 and
 * `DESIGN.md` 9.2: an entry dated to a year overlaps twelve months, and putting
 * it in any one of them would be the app inventing a precision the person never
 * gave. Testing only the start of the range would put every year-precise row
 * into January, which is exactly the mistake that reads as correct in a demo.
 */
@RunWith(AndroidJUnit4::class)
class MonthReviewTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    private val zone: ZoneId = ZoneId.systemDefault()
    private val june = YearMonth.of(2026, 6)

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(displayName = "Review fixture", relationship = "mother")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private fun day(text: String) = Edtf.day(LocalDate.parse(text))

    private fun entry(title: String, occurred: Edtf.Date) = runBlocking {
        repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = title,
            occurred = occurred,
        )
    }

    private fun reviewOf(month: YearMonth = june) =
        runBlocking { repository.monthReview(subjectId, month, zone) }

    @Test
    fun aDayInsideTheMonthIsInIt() = runBlocking {
        entry("The fifteenth", day("2026-06-15"))
        assertEquals(listOf("The fifteenth"), reviewOf().entries.map { it.title })
    }

    @Test
    fun theFirstAndLastDaysAreInsideTheMonth() = runBlocking {
        // Off by one at either edge silently drops a day of somebody's record,
        // and the day it drops is the one they are most likely to be looking
        // for, because a month boundary is where people go to check.
        entry("The first", day("2026-06-01"))
        entry("The last", day("2026-06-30"))

        val titles = reviewOf().entries.mapNotNull { it.title }.toSet()
        assertTrue("the first of the month was dropped: $titles", "The first" in titles)
        assertTrue("the last of the month was dropped: $titles", "The last" in titles)
    }

    @Test
    fun theDaysEitherSideAreNotInTheMonth() = runBlocking {
        entry("May's last", day("2026-05-31"))
        entry("July's first", day("2026-07-01"))

        assertTrue("a neighboring month leaked in", reviewOf().entries.isEmpty())
    }

    @Test
    fun aMonthPreciseDateBelongsToItsOwnMonth() = runBlocking {
        // "Sometime in June" is a real answer and it happened in June. It sits
        // among the days without being given one, which is rule 17 working
        // rather than an exception to it.
        entry("Sometime in June", Edtf.month(2026, 6))
        assertEquals(listOf("Sometime in June"), reviewOf().entries.map { it.title })
    }

    @Test
    fun aYearPreciseDateBelongsToNoMonthAtAll() = runBlocking {
        entry("Sometime in 2026", Edtf.year(2026))

        // Not January, which is where a test of the range's start alone would
        // put it, and not June either. The record never said which month.
        assertTrue(
            "a year-precise entry was claimed by January",
            reviewOf(YearMonth.of(2026, 1)).entries.isEmpty(),
        )
        assertTrue("a year-precise entry was claimed by June", reviewOf().entries.isEmpty())
    }

    @Test
    fun anUnknownDateBelongsToNoMonth() = runBlocking {
        // It is still a valid entry and it is still on the trail, under its own
        // heading at the end. It is simply not in any month, because nobody
        // said when it was.
        entry("Nobody remembers when", Edtf.unknown())
        assertTrue(reviewOf().entries.isEmpty())
    }

    @Test
    fun aMilestoneInTheMonthIsGathered() = runBlocking {
        repository.createMilestone(
            subjectId = subjectId,
            label = "First day without oxygen",
            occurred = day("2026-06-19"),
        )
        repository.createMilestone(
            subjectId = subjectId,
            label = "Long before",
            occurred = day("2026-01-23"),
        )

        assertEquals(
            listOf("First day without oxygen"),
            reviewOf().milestones.map { it.label },
        )
    }

    @Test
    fun anAppointmentAndADocumentInTheMonthAreGathered() = runBlocking {
        repository.createAppointment(
            subjectId = subjectId,
            title = "Care plan meeting",
            scheduled = day("2026-06-11"),
            locationNote = null,
            notes = null,
        )
        repository.createDocument(
            subjectId = subjectId,
            title = "Care plan, signed",
            received = day("2026-06-12"),
        )

        val review = reviewOf()
        assertEquals(listOf("Care plan meeting"), review.appointments.map { it.title })
        assertEquals(listOf("Care plan, signed"), review.documents.map { it.title })
    }

    @Test
    fun oneReportedAndAnsweredInsideTheMonthIsListedOnlyOnce() = runBlocking {
        // It listed under both headings on the phone, which reads as a
        // rendering fault rather than as two facts. What the second row taught
        // is that it was answered, and the first row says that in a word.
        val (incidentId, _) = repository.reportIncident(
            subjectId = subjectId,
            title = "Wrong medication brought to the room",
            description = null,
            occurred = day("2026-06-12"),
            threadId = null,
            isUnfiled = false,
        )
        repository.resolveIncident(
            incidentId = incidentId,
            resolvedAt = LocalDate.parse("2026-06-20").atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val review = reviewOf()
        assertEquals(1, review.reported.size)
        assertTrue(
            "an incident reported and answered in one month appeared under both headings",
            review.answered.isEmpty(),
        )
        assertFalse("the row should carry that it was answered", review.reported.first().isOpen)
    }

    @Test
    fun oneReportedEarlierAndAnsweredInTheMonthIsTheAnsweredGroup() = runBlocking {
        // This is the whole reason the second group exists. A person reading
        // June learns here that March's complaint was finally answered, and
        // nowhere else would tell them.
        val (incidentId, _) = repository.reportIncident(
            subjectId = subjectId,
            title = "Nobody called back",
            description = null,
            occurred = day("2026-03-04"),
            threadId = null,
            isUnfiled = false,
        )
        repository.resolveIncident(
            incidentId = incidentId,
            resolvedAt = LocalDate.parse("2026-06-20").atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val review = reviewOf()
        assertTrue("it was reported in March, not June", review.reported.isEmpty())
        assertEquals(listOf("Nobody called back"), review.answered.map { it.title })
    }

    @Test
    fun anIncidentStillOpenIsNotInAnyAnsweredGroup() = runBlocking {
        repository.reportIncident(
            subjectId = subjectId,
            title = "Still waiting",
            description = null,
            occurred = day("2026-06-05"),
            threadId = null,
            isUnfiled = false,
        )

        val review = reviewOf()
        assertEquals(1, review.reported.size)
        assertTrue(review.answered.isEmpty())
        assertTrue("an open incident must read as open", review.reported.first().isOpen)
    }

    @Test
    fun theKindCountsFollowTheNotebooksOrderRatherThanTheirSize() = runBlocking {
        // Ranking by count would move the kinds around from month to month and
        // put whichever part of a month happened most at the top, which is the
        // opinion `Digest` refuses to form about sections.
        repeat(3) { index ->
            repository.createEntry(
                subjectId = subjectId,
                kind = "visit",
                title = "Visit $index",
                occurred = day("2026-06-0${index + 1}"),
            )
        }
        entry("One call", day("2026-06-10"))

        val kinds = reviewOf().kinds
        assertEquals(listOf("call", "visit"), kinds.map { it.kind })
        assertEquals(listOf(1, 3), kinds.map { it.count })
    }

    @Test
    fun aMonthWithNothingInItIsEmptyRatherThanAbsent() = runBlocking {
        // Rule 11 and 13.3: the empty state has to exist and has to be
        // reachable. A month whose every entry is dated to the year is exactly
        // this, and it is reachable from the trail, because those entries do
        // group under a month heading there.
        entry("Sometime in 2026", Edtf.year(2026))

        val review = reviewOf()
        assertTrue("a month holding nothing must say so rather than throw", review.isEmpty)
        assertTrue(review.entries.isEmpty())
        assertTrue(review.kinds.isEmpty())
    }

    @Test
    fun amonthThatHoldsAnythingAtAllIsNotEmpty() = runBlocking {
        repository.createDocument(
            subjectId = subjectId,
            title = "The only thing in June",
            received = day("2026-06-12"),
        )
        assertFalse(reviewOf().isEmpty)
    }
}
