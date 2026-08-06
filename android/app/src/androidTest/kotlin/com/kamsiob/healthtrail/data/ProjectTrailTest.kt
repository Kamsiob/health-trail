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
 * One project's own trail. `DESIGN.md` 20.5 screen 11, issue #284.
 *
 * **Three sources on one line**, and the part that can go wrong is the merge:
 * the order, what is left out, and what is quietly invented. A screenshot shows
 * a plausible list either way.
 */
@RunWith(AndroidJUnit4::class)
class ProjectTrailTest {

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
            stages = listOf("Applied", "In review", "Decision"),
            dateKinds = emptyList(),
            papers = emptyList(),
        )
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private suspend fun call(body: String, on: LocalDate): String {
        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            body = body,
            occurred = Edtf.day(on),
        )
        repository.linkEntryToProject(id, projectId)
        return id
    }

    /**
     * Oldest first, which is the opposite of the main trail.
     *
     * A process is read forward: how it got here and what is coming. Getting
     * this backwards produces a screen that looks entirely finished and tells
     * the story in reverse.
     */
    @Test
    fun theTrailRunsForwards() = runBlocking {
        call("Third", LocalDate.parse("2026-05-01"))
        call("First", LocalDate.parse("2026-03-01"))
        call("Second", LocalDate.parse("2026-04-01"))

        val bodies = repository.projectTrail(projectId).map { it.entry?.body }
        assertEquals(listOf("First", "Second", "Third"), bodies)
    }

    /** All three sources land on the one line, in date order among each other. */
    @Test
    fun whatWasSaidTheRoadAndTheDatesAreAllOnIt() = runBlocking {
        call("They have it", LocalDate.parse("2026-03-10"))
        val stages = repository.projectStages(projectId)
        repository.moveProjectToStage(
            projectId,
            stages[1].id,
            Edtf.day(LocalDate.parse("2026-03-20")),
        )
        repository.addProjectDate(
            projectId = projectId,
            kind = "Decision expected",
            due = Edtf.day(LocalDate.parse("2026-06-01")),
        )

        val trail = repository.projectTrail(projectId)
        assertEquals(
            listOf(
                Repository.ProjectTrailKind.ENTRY,
                Repository.ProjectTrailKind.STAGE,
                Repository.ProjectTrailKind.DATE,
            ),
            trail.map { it.kind },
        )
        // **Starting a project reaches no stage.** The road opens with every
        // waypoint hollow and `roadDescription` has a "not started" sentence
        // for exactly that, so the only stage here is the one moved to.
        assertEquals("In review", trail[1].title)
        assertEquals("Decision expected", trail.last().title)
    }

    /**
     * A stage nobody has reached is not on the line at all.
     *
     * It has no date, so it has no place on something ordered by date, and
     * putting it at the end would say it happened last rather than not at all.
     */
    @Test
    fun aStageNotYetReachedIsNotOnTheTrail() = runBlocking {
        // Three stages exist and none has been reached, so none is on the line.
        assertEquals(3, repository.projectStages(projectId).size)
        assertTrue(
            "a stage nobody has reached is on the trail",
            repository.projectTrail(projectId).none {
                it.kind == Repository.ProjectTrailKind.STAGE
            },
        )

        val stages = repository.projectStages(projectId)
        repository.moveProjectToStage(
            projectId,
            stages[0].id,
            Edtf.day(LocalDate.parse("2026-02-01")),
        )

        val names = repository.projectTrail(projectId)
            .filter { it.kind == Repository.ProjectTrailKind.STAGE }
            .map { it.title }
        assertEquals(listOf("Applied"), names)
        assertTrue("In review has not been reached", "In review" !in names)
    }

    /**
     * A date in the future is on the trail and is not marked as anything.
     *
     * 20.5 screen 11 draws the response window closing as an ordinary row. The
     * app records and counts and never concludes, rule 2.
     */
    @Test
    fun aDateThatHasNotArrivedIsStillOnTheTrail() = runBlocking {
        val ahead = LocalDate.now().plusDays(90)
        repository.addProjectDate(
            projectId = projectId,
            kind = "Response window closes",
            due = Edtf.day(ahead),
        )
        val trail = repository.projectTrail(projectId)
        assertEquals("Response window closes", trail.last().title)
        assertEquals(Repository.ProjectTrailKind.DATE, trail.last().kind)
    }

    /** An entry with no date is kept and sorts last, rather than being dropped. */
    @Test
    fun anUndatedEntryIsKeptAtTheEnd() = runBlocking {
        call("Dated", LocalDate.parse("2026-03-01"))
        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            body = "No idea when",
            occurred = Edtf.unknown(),
        )
        repository.linkEntryToProject(id, projectId)

        val trail = repository.projectTrail(projectId)
        assertTrue("the undated entry was dropped", trail.any { it.id == id })
        assertEquals("No idea when", trail.last().entry?.body)
    }

    /**
     * A project nobody has touched has nothing on its trail.
     *
     * **Starting one reaches no stage, writes no date and says nothing**, so
     * the honest answer is empty and the screen has an empty state for it. A
     * first row invented at creation would be the app putting something in
     * somebody's record that they did not.
     */
    @Test
    fun aFreshProjectHasNothingOnItsTrail() = runBlocking {
        assertEquals(emptyList<Repository.ProjectTrailItem>(), repository.projectTrail(projectId))
    }
}
