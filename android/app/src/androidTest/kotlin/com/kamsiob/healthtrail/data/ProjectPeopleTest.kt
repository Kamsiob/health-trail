package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The people a project has involved. `DESIGN.md` 20.5 screen 14, issue #287.
 *
 * **Derived rather than stored**, so what can go wrong is the derivation: who
 * counts as being on a project, whether the care team leaks in, and whether the
 * cross-project door points at the right other project.
 */
@RunWith(AndroidJUnit4::class)
class ProjectPeopleTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repository: Repository
    private lateinit var subjectId: String
    private lateinit var medicaid: String
    private lateinit var appeal: String

    @Before
    fun setUp() = runBlocking {
        Repository.closeForTest()
        repository = Repository.open(context)
        subjectId = repository.createSubject("Margaret")
        medicaid = repository.startProject(
            subjectId = subjectId,
            templateId = "medicaid_ltc",
            name = "Medicaid application",
            steps = emptyList(),
            lead = "standing",
            stages = listOf("Applied", "In review"),
            dateKinds = emptyList(),
            papers = emptyList(),
        )
        appeal = repository.startProject(
            subjectId = subjectId,
            templateId = "medicaid_ltc",
            name = "Wheelchair claim appeal",
            steps = emptyList(),
            lead = "standing",
            stages = listOf("Filed", "Answered"),
            dateKinds = emptyList(),
            papers = emptyList(),
        )
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private suspend fun callOn(projectId: String, personId: String, on: LocalDate) {
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            body = "A call",
            occurred = Edtf.day(on),
        )
        repository.linkEntryToProject(entryId, projectId)
        repository.linkEntryToPerson(entryId, personId)
    }

    /**
     * Somebody on the care team who has not turned up on this project is not
     * on it.
     *
     * **The two lists overlap and are not the same one.** Showing the whole
     * care team under a Medicaid application would bury the caseworker who
     * matters in a list of nurses.
     */
    @Test
    fun theCareTeamIsNotTheProjectsPeople() = runBlocking {
        val denise = repository.createPerson(subjectId, "Denise", roleLabel = "Intake caseworker")
        repository.createPerson(subjectId, "A nurse", roleLabel = "Ward nurse")
        callOn(medicaid, denise, LocalDate.parse("2026-03-28"))

        val people = repository.projectPeople(medicaid)
        assertEquals(listOf("Denise"), people.map { it.person.displayName })
    }

    /** How many of the project's entries name them, and when they last did. */
    @Test
    fun aPersonCarriesHowOftenTheyTurnedUpAndWhen() = runBlocking {
        val denise = repository.createPerson(subjectId, "Denise", roleLabel = "Intake caseworker")
        callOn(medicaid, denise, LocalDate.parse("2026-03-01"))
        callOn(medicaid, denise, LocalDate.parse("2026-03-28"))

        val person = repository.projectPeople(medicaid).single()
        assertEquals(2, person.mentions)
        assertEquals("2026-03-28", person.lastEdtf)
    }

    /**
     * The cross-project door, which is the one new navigation idea on this
     * surface.
     */
    @Test
    fun somebodyOnTwoProjectsNamesTheOtherOne() = runBlocking {
        val okafor = repository.createPerson(subjectId, "Dr. Okafor", roleLabel = "Primary care")
        callOn(medicaid, okafor, LocalDate.parse("2026-03-10"))
        callOn(appeal, okafor, LocalDate.parse("2026-04-02"))

        val onMedicaid = repository.projectPeople(medicaid).single()
        assertEquals(listOf("Wheelchair claim appeal"), onMedicaid.alsoIn.map { it.name })
        // And the same in reverse, because it is one relationship read twice.
        val onAppeal = repository.projectPeople(appeal).single()
        assertEquals(listOf("Medicaid application"), onAppeal.alsoIn.map { it.name })
    }

    /** Somebody on one project only says so, rather than naming itself. */
    @Test
    fun somebodyOnOneProjectNamesNoOther() = runBlocking {
        val denise = repository.createPerson(subjectId, "Denise", roleLabel = "Intake caseworker")
        callOn(medicaid, denise, LocalDate.parse("2026-03-28"))
        assertEquals(emptyList<Repository.EntryProject>(), repository.projectPeople(medicaid).single().alsoIn)
    }

    /** Most recently seen first, because that is who you are dealing with now. */
    @Test
    fun theMostRecentlySeenComesFirst() = runBlocking {
        val old = repository.createPerson(subjectId, "R. Boyd", roleLabel = "Intake supervisor")
        val recent = repository.createPerson(subjectId, "Denise", roleLabel = "Intake caseworker")
        callOn(medicaid, old, LocalDate.parse("2026-01-05"))
        callOn(medicaid, recent, LocalDate.parse("2026-03-28"))

        assertEquals(
            listOf("Denise", "R. Boyd"),
            repository.projectPeople(medicaid).map { it.person.displayName },
        )
    }

    /** A project nobody has been named on has no people, rather than the team. */
    @Test
    fun aProjectWithNoCallsHasNobodyOnIt() = runBlocking {
        repository.createPerson(subjectId, "Denise", roleLabel = "Intake caseworker")
        assertTrue(repository.projectPeople(medicaid).isEmpty())
    }
}
