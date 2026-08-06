package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An entry and the project it is about, both ways. Rule 18, and #283.
 *
 * **This was one way for as long as the connection existed.** `entriesAbout`
 * let a project list every entry connected to it, and logging a call from
 * inside a project wrote that connection, but `EntryDetail` had no project on
 * it at all. So a call somebody logged from a Medicaid application opened onto
 * a screen with no way back to the Medicaid application: rule 18's dead end
 * wearing a disguise, and the last one left on the entry screen.
 *
 * **Both link directions are covered on purpose.** 8.1's `link` table records a
 * source and a target, and a link written from the entry and one written from
 * the project mean the same thing to a person. A read that only accepted one
 * of them would work everywhere the app writes links today and break the first
 * time an import wrote the other, which is the failure nobody would find.
 */
@RunWith(AndroidJUnit4::class)
class EntryProjectLinkTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repository: Repository
    private lateinit var subjectId: String
    private lateinit var projectId: String

    @Before
    fun setUp() = runBlocking {
        Repository.closeForTest()
        repository = Repository.open(context)
        subjectId = repository.createSubject("Margaret")
        projectId = repository.startProject(
            subjectId = subjectId,
            templateId = "medicaid_ltc",
            name = "Medicaid application",
            steps = emptyList(),
            lead = "standing",
            stages = listOf("Applied", "In review"),
            dateKinds = emptyList(),
            papers = emptyList(),
        )
    }

    // The repository is a singleton over the one real database, and a test that
    // does not close it breaks a class four files away with nothing to connect
    // them. Every other data test here closes it.
    @After
    fun tearDown() = Repository.closeForTest()

    private suspend fun newCall(body: String) = repository.createEntry(
        subjectId = subjectId,
        kind = "call",
        body = body,
    )

    @Test
    fun theEntryNamesTheProjectItWasLoggedAgainst() = runBlocking {
        val entryId = newCall("It is with the nurse reviewer.")
        repository.linkEntryToProject(entryId, projectId)

        val detail = repository.entry(entryId)
        assertEquals(1, detail?.projects?.size)
        assertEquals(projectId, detail?.projects?.first()?.id)
        assertEquals("Medicaid application", detail?.projects?.first()?.name)
        assertEquals("active", detail?.projects?.first()?.status)
    }

    /**
     * The same connection written the other way round reads the same.
     *
     * Nothing in the app writes it this way today. An imported notebook can,
     * and `entriesAbout` and `latestWordFor` both already accept both.
     */
    @Test
    fun aLinkWrittenFromTheProjectSideReadsTheSame() = runBlocking {
        val entryId = newCall("They said to call back after the 15th.")
        repository.insertLinkForTest(
            sourceTable = "project",
            sourceId = projectId,
            targetTable = "entry",
            targetId = entryId,
        )

        val detail = repository.entry(entryId)
        assertEquals(
            "a link written from the project is invisible from the entry",
            listOf(projectId),
            detail?.projects?.map { it.id },
        )
    }

    /** Both halves agree: the project lists the entry and the entry the project. */
    @Test
    fun theLinkGoesBothWays() = runBlocking {
        val entryId = newCall("Still with the reviewer.")
        repository.linkEntryToProject(entryId, projectId)

        val fromProject = repository.entriesAbout(projectId).map { it.id }
        val fromEntry = repository.entry(entryId)?.projects?.map { it.id }
        assertTrue("the project does not list the entry", entryId in fromProject)
        assertEquals("the entry does not list the project", listOf(projectId), fromEntry)
    }

    /** An entry connected to nothing says nothing, rather than an empty door. */
    @Test
    fun anUnconnectedEntryNamesNoProject() = runBlocking {
        val entryId = newCall("A call about nothing in particular.")
        assertEquals(emptyList<Repository.EntryProject>(), repository.entry(entryId)?.projects)
    }

    /**
     * A removed project is not offered as a door.
     *
     * Deletion is a tombstone and never a row removal, per rule 3, so the row
     * is still there to be joined against. `live_project` is what keeps it off
     * the screen, and a door onto a project the person removed would open on
     * nothing.
     */
    @Test
    fun aRemovedProjectStopsBeingADoor() = runBlocking {
        val entryId = newCall("About the application.")
        repository.linkEntryToProject(entryId, projectId)
        assertEquals(1, repository.entry(entryId)?.projects?.size)

        repository.tombstoneForTest("project", projectId)

        assertEquals(emptyList<Repository.EntryProject>(), repository.entry(entryId)?.projects)
    }
}
