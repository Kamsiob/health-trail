package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An incident is a thread, not an event.
 *
 * `MASTER_SPEC.md` 4.7: from first report to resolution, with each call and
 * escalation as a node on it. **Until 2026-08-02 it was an entry with a scary
 * kind**, reported once and never followed, which meant the one thing a
 * caregiver actually needs from an incident six months later, the sequence, did
 * not exist anywhere.
 */
@RunWith(AndroidJUnit4::class)
class IncidentTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(displayName = "Incident fixture", relationship = "mother")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private fun report(title: String) = runBlocking {
        repository.reportIncident(
            subjectId = subjectId,
            title = title,
            description = "Nobody wrote it in the notes",
            occurred = Edtf.day(LocalDate.of(2026, 8, 2)),
            threadId = null,
            isUnfiled = false,
        )
    }

    @Test
    fun reportingWritesTheIncidentAndItsFirstEntryTogether() = runBlocking {
        val (incidentId, entryId) = report("A fall in the bathroom")

        // **Both rows, or the incident can never be followed.** Writing only the
        // entry is what left this feature unbuildable, the same way writing only
        // an entry left the questions section counting zero forever.
        assertTrue(incidentId.isNotBlank())
        assertTrue(entryId.isNotBlank())

        val incidents = repository.incidents(subjectId)
        assertEquals(1, incidents.size)
        assertEquals("A fall in the bathroom", incidents.first().title)
        assertEquals(1, incidents.first().entryCount)

        // And it is on the trail in its own right, because it is a thing that
        // happened as well as a thing being chased.
        assertTrue(repository.trail(subjectId).any { it.id == entryId })
    }

    @Test
    fun anIncidentStartsOpenAndCounts() = runBlocking {
        report("A fall in the bathroom")

        assertTrue(repository.incidents(subjectId).first().isOpen)
        assertEquals(1, repository.openIncidentCount(subjectId))
    }

    @Test
    fun eachEscalationIsANodeOnTheThreadInTheOrderItHappened() = runBlocking {
        val (incidentId, _) = report("A fall in the bathroom")

        repository.addToIncident(
            subjectId = subjectId,
            incidentId = incidentId,
            kind = "call",
            title = "Escalated to the director of nursing",
            body = null,
            occurred = Edtf.day(LocalDate.of(2026, 8, 5)),
        )
        repository.addToIncident(
            subjectId = subjectId,
            incidentId = incidentId,
            kind = "call",
            title = "They said they would review the schedule",
            body = null,
            occurred = Edtf.day(LocalDate.of(2026, 8, 9)),
        )

        val thread = repository.incidentTrail(incidentId)
        assertEquals(3, thread.size)

        // **Oldest first, which is the opposite of the trail.** The trail
        // answers "what has been happening lately"; a thread answers "how did
        // this go", and a story told backward is not the same story.
        assertEquals("A fall in the bathroom", thread[0].title)
        assertEquals("Escalated to the director of nursing", thread[1].title)
        assertEquals("They said they would review the schedule", thread[2].title)

        assertEquals(3, repository.incidents(subjectId).first().entryCount)
    }

    @Test
    fun resolvingRecordsWhenAndIsNeverARemoval() = runBlocking {
        val (incidentId, _) = report("A fall in the bathroom")

        repository.resolveIncident(
            incidentId = incidentId,
            resolvedAt = 1_785_000_000_000L,
            resolutionNote = "They changed the checks to hourly",
        )

        val settled = repository.incidents(subjectId).first()
        assertFalse(settled.isOpen)
        assertNotNull(settled.resolvedAt)
        assertEquals("They changed the checks to hourly", settled.resolutionNote)
        assertEquals(0, repository.openIncidentCount(subjectId))

        // **It is still there, and so is its thread.** A resolved incident is
        // the half of the record that shows something was chased and answered,
        // which is exactly what somebody needs at the next care plan meeting.
        assertEquals(1, repository.incidents(subjectId).size)
        assertEquals(1, repository.incidentTrail(incidentId).size)
    }

    @Test
    fun anIncidentCanBeReopenedBecauseAnAnswerCanTurnOutNotToHold() = runBlocking {
        val (incidentId, _) = report("A fall in the bathroom")
        repository.resolveIncident(incidentId, resolvedAt = 1_785_000_000_000L)
        assertFalse(repository.incidents(subjectId).first().isOpen)

        repository.resolveIncident(incidentId, resolvedAt = null)

        val reopened = repository.incidents(subjectId).first()
        assertTrue("an incident could not be reopened", reopened.isOpen)
        assertNull(reopened.resolvedAt)
        assertEquals(1, repository.openIncidentCount(subjectId))
    }

    @Test
    fun openIncidentsComeBeforeAnsweredOnes() = runBlocking {
        // **The one nobody has answered is what the person is carrying around.**
        // A list that buries it under answered ones by date has forgotten what
        // it is for. This orders by state and never judges how long.
        val (older, _) = report("Reported first, and answered")
        repository.resolveIncident(older, resolvedAt = 1_785_000_000_000L)
        report("Reported later, and still open")

        val incidents = repository.incidents(subjectId)
        assertEquals(2, incidents.size)
        assertTrue("an answered incident sorted above an open one", incidents.first().isOpen)
        assertEquals("Reported later, and still open", incidents.first().title)
    }

    @Test
    fun anEntryCanBeAttachedToAnIncidentAfterItWasWritten() = runBlocking {
        // The path a capture opened from a thread takes: the entry is written
        // the ordinary way, so it lands on the trail, then it is told which
        // thread it belongs to. Rule 18, links go both ways.
        val (incidentId, _) = report("A fall in the bathroom")
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Called the ward back",
        )
        assertEquals(1, repository.incidentTrail(incidentId).size)

        repository.attachEntryToIncident(entryId, incidentId)

        val thread = repository.incidentTrail(incidentId)
        assertEquals(2, thread.size)
        assertTrue(thread.any { it.id == entryId })
    }
}
