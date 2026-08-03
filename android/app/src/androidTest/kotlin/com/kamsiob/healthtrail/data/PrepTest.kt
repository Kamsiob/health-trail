package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a prep sheet is counting, and from when.
 *
 * **The window is the part that can be quietly wrong.** A sheet that shows the
 * wrong span does not look broken: it looks like a shorter list, and the person
 * walks into a care plan meeting without the thing they meant to raise. That is
 * the failure mode worth a table of boundaries rather than one happy case.
 *
 * The composition itself is deliberately dull. Nothing is summarized,
 * inferred, or ranked, per `MASTER_SPEC.md` 4.11, so what these check is which
 * rows are in and which are out.
 */
@RunWith(AndroidJUnit4::class)
class PrepTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(displayName = "Prep fixture", relationship = "mother")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private fun day(text: String) = Edtf.day(LocalDate.parse(text))

    private fun appointment(title: String, on: String) = runBlocking {
        repository.createAppointment(
            subjectId = subjectId,
            title = title,
            scheduled = day(on),
            locationNote = null,
            notes = null,
        )
    }

    private fun entry(title: String, on: String) = runBlocking {
        repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = title,
            occurred = day(on),
        )
    }

    private fun prepFor(id: String) = runBlocking { repository.prep(subjectId, id)!! }

    @Test
    fun theFirstAppointmentCountsEverythingWrittenSoFar() = runBlocking {
        entry("Long before", "2026-01-05")
        entry("Also before", "2026-02-05")
        val id = appointment("First meeting", "2026-03-01")

        val sheet = prepFor(id)
        assertNull("with no earlier appointment there is no window to state", sheet.sinceEdtf)
        assertEquals(2, sheet.changes.size)
    }

    @Test
    fun alaterAppointmentCountsOnlySinceTheLastOne() = runBlocking {
        entry("Before the first meeting", "2026-01-05")
        appointment("First meeting", "2026-02-01")
        entry("In between", "2026-02-10")
        entry("Also in between", "2026-02-20")
        val second = appointment("Second meeting", "2026-03-01")

        val sheet = prepFor(second)

        // **The window is stated, not implied.** A person who cannot see where
        // the list starts cannot tell whether something is missing.
        assertEquals("2026-02-01", sheet.sinceEdtf)

        val titles = sheet.changes.map { it.title }
        assertTrue("something from before the last meeting was carried forward: $titles",
            titles.none { it == "Before the first meeting" })
        assertEquals(2, sheet.changes.size)
    }

    @Test
    fun nothingAfterTheAppointmentIsCountedAsPreparationForIt() = runBlocking {
        appointment("First meeting", "2026-02-01")
        val second = appointment("Second meeting", "2026-03-01")
        entry("After the second meeting", "2026-03-15")

        val titles = prepFor(second).changes.map { it.title }
        assertTrue(
            "an entry written after the appointment appeared on its prep sheet: $titles",
            titles.none { it == "After the second meeting" },
        )
    }

    @Test
    fun theWindowIsTheAppointmentBeforeThisOneRatherThanTheMostRecentOfAll() = runBlocking {
        // Three meetings, and the sheet for the middle one must count from the
        // first, not from the third. A naive "latest appointment" would take
        // the third and produce an empty sheet.
        appointment("First", "2026-01-01")
        val middle = appointment("Middle", "2026-02-01")
        appointment("Last", "2026-03-01")
        entry("Between first and middle", "2026-01-15")
        entry("Between middle and last", "2026-02-15")

        val sheet = prepFor(middle)
        assertEquals("2026-01-01", sheet.sinceEdtf)
        assertEquals(listOf("Between first and middle"), sheet.changes.map { it.title })
    }

    @Test
    fun onlyQuestionsNobodyHasAskedYetAreCarried() = runBlocking {
        val id = appointment("The meeting", "2026-03-01")
        val (_, openQuestion) = repository.createQuestionWithEntry(
            subjectId = subjectId,
            text = "Why was the dressing schedule changed?",
            roleLabel = "Charge nurse",
            occurred = day("2026-02-10"),
            threadId = null,
            isUnfiled = false,
        )
        val (_, asked) = repository.createQuestionWithEntry(
            subjectId = subjectId,
            text = "Already asked this one",
            roleLabel = null,
            occurred = day("2026-02-11"),
            threadId = null,
            isUnfiled = false,
        )
        repository.markQuestionAsked(asked, asked = day("2026-02-12"))

        val sheet = prepFor(id)
        val texts = sheet.questions.map { it.text }
        assertTrue("an answered question came back onto a prep sheet: $texts",
            texts.none { it == "Already asked this one" })
        assertTrue(texts.any { it.startsWith("Why was the dressing") })
        assertTrue(openQuestion.isNotBlank())
    }

    @Test
    fun anAppointmentWithNoDateStillProducesASheet() = runBlocking {
        // Rule 17: unknown is a first class value. Somebody who knows a meeting
        // is coming and not when still needs the questions they saved.
        val id = runBlocking {
            repository.createAppointment(
                subjectId = subjectId,
                title = "Sometime soon",
                scheduled = Edtf.unknown(),
                locationNote = null,
                notes = null,
            )
        }
        entry("Something written down", "2026-02-10")

        val sheet = prepFor(id)
        assertEquals("Sometime soon", sheet.appointment.title)
        assertEquals(1, sheet.changes.size)
    }

    @Test
    fun anAppointmentThatIsGoneProducesNothingRatherThanThrowing() = runBlocking {
        assertNull(repository.prep(subjectId, "not-an-appointment-id"))
    }
}
