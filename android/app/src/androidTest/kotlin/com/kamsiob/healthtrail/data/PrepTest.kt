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

    // ---- whose questions come with you --------------------------------------

    private fun person(name: String, role: String?) = runBlocking {
        repository.createPerson(subjectId = subjectId, displayName = name, roleLabel = role)
    }

    private fun question(text: String, forPerson: String?) = runBlocking {
        repository.createQuestionWithEntry(
            subjectId = subjectId,
            text = text,
            roleLabel = null,
            occurred = day("2026-02-01"),
            threadId = null,
            isUnfiled = false,
            personId = forPerson,
        ).second
    }

    private fun appointmentWith(title: String, on: String, personId: String?) = runBlocking {
        repository.createAppointment(
            subjectId = subjectId,
            title = title,
            scheduled = day(on),
            locationNote = null,
            notes = null,
            personId = personId,
        )
    }

    /**
     * **The whole of #371 item 2.** The sheet used to carry every open question
     * in the notebook, so a question written for the wound nurse arrived at the
     * billing meeting, and on a year five notebook that is a wall of things
     * nobody in the room can answer.
     */
    @Test
    fun onlyTheQuestionsForThePersonYouAreSeeingComeWithYou() = runBlocking {
        val nurse = person("Angela Reyes", "Charge nurse")
        val billing = person("Wesley Obi", "Billing office")
        question("Why was the dressing not changed", nurse)
        question("What is this charge for", billing)

        val sheet = prepFor(appointmentWith("Care plan meeting", "2026-03-01", nurse))
        val texts = sheet.questions.map { it.text }
        assertTrue("the nurse's question comes", texts.any { it.startsWith("Why was the dressing") })
        assertTrue("the billing question stays behind", texts.none { it.startsWith("What is this charge") })
    }

    /**
     * **A question waiting on nobody comes to everything.** It is the commonest
     * kind, it is what somebody asks whoever is in the room, and filtering it
     * out would hide most of the sheet. Rule 13.
     */
    @Test
    fun aquestionWaitingOnNobodyComesToEveryAppointment() = runBlocking {
        val nurse = person("Angela Reyes", "Charge nurse")
        question("Does she need new shoes", null)

        val sheet = prepFor(appointmentWith("Care plan meeting", "2026-03-01", nurse))
        assertEquals(listOf("Does she need new shoes"), sheet.questions.map { it.text })
    }

    /**
     * **An appointment with nobody on it shows everything.** There is nothing
     * to compare against, and hiding questions on the strength of a link
     * nobody made would be the app deciding for them.
     */
    @Test
    fun anAppointmentWithNobodyOnItShowsEveryOpenQuestion() = runBlocking {
        val nurse = person("Angela Reyes", "Charge nurse")
        question("Why was the dressing not changed", nurse)
        question("Does she need new shoes", null)

        val sheet = prepFor(appointmentWith("Somebody will call", "2026-03-01", null))
        assertEquals(2, sheet.questions.size)
    }

    /**
     * **Both ends of `asked_at_appointment_id`.** It shipped in Phase 0, the
     * archive renders it, all four catalogs name it, and the only thing that
     * had ever written it was the fixture generator, so half the link shipped:
     * a question claiming an appointment that said nothing back. Rule 18.
     */
    @Test
    fun aquestionAskedAtAnAppointmentIsCarriedByBothOfThem() = runBlocking {
        val nurse = person("Angela Reyes", "Charge nurse")
        val id = appointmentWith("Care plan meeting", "2026-03-01", nurse)
        val questionId = question("Why was the dressing not changed", nurse)

        repository.markQuestionAsked(questionId, day("2026-03-01"), appointmentId = id)

        val sheet = prepFor(id)
        assertTrue("it is no longer waiting", sheet.questions.isEmpty())
        assertEquals(
            "and the appointment says it was asked here",
            listOf("Why was the dressing not changed"),
            sheet.asked.map { it.text },
        )
        val asked = repository.questions(subjectId).first { it.id == questionId }
        assertEquals("Care plan meeting", asked.askedAtAppointmentTitle)
    }

    /** A question ticked off the list rather than off a sheet claims no appointment. */
    @Test
    fun aquestionAskedAwayFromAnAppointmentClaimsNone() = runBlocking {
        val questionId = question("Does she need new shoes", null)
        repository.markQuestionAsked(questionId, day("2026-03-02"))

        val asked = repository.questions(subjectId).first { it.id == questionId }
        assertNull(asked.askedAtAppointmentId)
        assertNull(asked.askedAtAppointmentTitle)
    }
}
