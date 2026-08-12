package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An incident can be corrected and removed. #358.
 *
 * **It was the one thing in this app that could not be fixed afterward.** The
 * only `UPDATE incident` set `resolved_at`, so a title typed one handed in a
 * corridor stayed exactly as typed, and a date guessed wrong stayed wrong.
 * `renameProject` carries the sentence the rest of the app lives by: every name
 * in this app is a correction away.
 *
 * **The date keeps whatever precision was given**, rule 17, so correcting the
 * words does not quietly sharpen "sometime in March" into a day.
 *
 * **What happened next is untouched by either**, because those are entries and
 * they are the record of what happened.
 */
@RunWith(AndroidJUnit4::class)
class IncidentCorrectionTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private suspend fun anIncident(repository: Repository, subject: String): String =
        repository.reportIncident(
            subjectId = subject,
            title = "Bruse on her arm nobody could explain",
            description = "Reported to the charge nurse.",
            occurred = Edtf.parse("2026-03")!!,
            threadId = null,
            isUnfiled = false,
        ).first

    @Test
    fun theWordsCanBeCorrectedAndTheDateKeepsItsPrecision() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Correcting")
        val id = anIncident(repository, subject)

        repository.updateIncident(
            incidentId = id,
            title = "  Bruise on her arm nobody could explain  ",
            description = "Reported to the charge nurse. Asked for it in writing.",
            reported = Edtf.parse("2026-03")!!,
        )

        val incident = repository.incidents(subject).single { it.id == id }
        assertEquals("Bruise on her arm nobody could explain", incident.title)
        // **A month stays a month.** Rule 17: the app never sharpens a date the
        // person did not sharpen.
        assertEquals("2026-03", repository.columnForTest("incident", id, "reported_edtf"))
    }

    /**
     * **A correction is an ordinary write**, so the change log sees it and the
     * revision moves the way every other write moves it. Rule 3.
     */
    @Test
    fun aCorrectionMovesTheRevisionAndIsLogged() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Logged")
        val id = anIncident(repository, subject)
        val before = repository.revisionForTest("incident", id)
        val logBefore = repository.changeLogSizeForTest()

        repository.updateIncident(
            incidentId = id,
            title = "Bruise on her arm",
            description = null,
            reported = null,
        )

        assertEquals(before + 1, repository.revisionForTest("incident", id))
        assertTrue(repository.changeLogSizeForTest() > logBefore)
        // Cleared rather than left behind, which is what "unknown" has to mean
        // when somebody takes a date back off.
        assertEquals(null, repository.columnForTest("incident", id, "reported_edtf"))
    }

    /**
     * **Removal is a tombstone and the entries survive it.** They are the record
     * of what happened, and the grouping being taken away does not unmake them.
     */
    @Test
    fun removalIsATombstoneAndTheEntriesSurvive() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Removing")
        val id = anIncident(repository, subject)
        val entries = repository.incidentTrail(id).size

        repository.removeIncident(id)

        assertTrue(repository.incidents(subject).none { it.id == id })
        assertNotNull(repository.columnForTest("incident", id, "deleted_at"))
        // The entry written when it was reported is still in the trail.
        assertTrue(entries > 0)
        assertTrue(repository.trail(subject).isNotEmpty())
    }
}
