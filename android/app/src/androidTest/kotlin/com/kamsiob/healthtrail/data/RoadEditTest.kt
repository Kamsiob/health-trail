package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The road is the person's, not the template's. `DESIGN.md` 20.4 and law 5.
 *
 * **A template that guessed the order of a process wrong is a template, not a
 * verdict.** These processes vary by state and by office, and until now a
 * project's stages could only be added to: never renamed, never reordered,
 * never removed. This is the data half of that, and it is here rather than in a
 * screen test because the part that can go wrong is what happens to the project
 * standing on a stage that is taken away.
 */
@RunWith(AndroidJUnit4::class)
class RoadEditTest {

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

    private suspend fun stages() = repository.projectStages(projectId)

    @Test
    fun aStageCanBeRenamedWithoutLosingWhenItWasReached() = runBlocking {
        val inReview = stages()[1]
        repository.moveProjectToStage(projectId, inReview.id, Edtf.day(java.time.LocalDate.parse("2026-03-04")))

        val reachedBefore = stages()[1].enteredEdtf
        assertNotNull("the stage was never marked reached", reachedBefore)

        repository.renameProjectStage(inReview.id, "With the reviewer")

        val after = stages()[1]
        assertEquals("With the reviewer", after.name)
        // **The arrival is a fact and the name is a label.** Somebody deciding
        // a stage is really called something else has not changed when the
        // project got there.
        assertEquals(reachedBefore, after.enteredEdtf)
    }

    @Test
    fun aStageMovesAgainstItsNeighborAndTheEndsStayPut() = runBlocking {
        repository.moveProjectStage(stages()[2].id, earlier = true)
        assertEquals(listOf("Applied", "Decision", "In review"), stages().map { it.name })

        // Moving the first one earlier does nothing rather than wrapping it to
        // the end, which would be the road reordering itself behind somebody.
        repository.moveProjectStage(stages()[0].id, earlier = true)
        assertEquals(listOf("Applied", "Decision", "In review"), stages().map { it.name })

        repository.moveProjectStage(stages()[2].id, earlier = false)
        assertEquals(listOf("Applied", "Decision", "In review"), stages().map { it.name })
    }

    @Test
    fun removingAStageTakesItOffTheRoad() = runBlocking {
        repository.removeProjectStage(stages()[1].id)
        assertEquals(listOf("Applied", "Decision"), stages().map { it.name })
    }

    /**
     * The project cannot be left standing on a stage that is gone.
     *
     * `RoadStrip` works out where a project is from the stages themselves, so a
     * project pointing at a removed stage draws as having reached nothing at
     * all: the road would say the application had not been filed. It falls back
     * to the last stage before it that was actually reached, which is where the
     * project really got to.
     */
    @Test
    fun aProjectStandingOnARemovedStageFallsBackToTheLastOneItReached() = runBlocking {
        val all = stages()
        repository.moveProjectToStage(projectId, all[0].id, Edtf.day(java.time.LocalDate.parse("2026-02-01")))
        repository.moveProjectToStage(projectId, all[1].id, Edtf.day(java.time.LocalDate.parse("2026-03-04")))

        repository.removeProjectStage(all[1].id)

        val road = stages()
        assertEquals(listOf("Applied", "Decision"), road.map { it.name })
        // The last stage it had actually reached, not the first on the list and
        // not nothing.
        assertEquals("Applied", road.firstOrNull { it.isReached }?.name)
        assertEquals(
            "the project was left pointing at a stage that is gone",
            "Applied",
            road.firstOrNull { it.id == currentStage() }?.name,
        )
    }

    /**
     * Removing the only stage the project had ever reached leaves it at the
     * start of its road rather than pointing at nothing.
     */
    @Test
    fun removingTheOnlyReachedStageLeavesTheProjectAtNoStage() = runBlocking {
        val all = stages()
        repository.moveProjectToStage(projectId, all[0].id, Edtf.day(java.time.LocalDate.parse("2026-02-01")))

        repository.removeProjectStage(all[0].id)

        assertNull("the project still points at the stage that was removed", currentStage())
    }

    private suspend fun currentStage(): String? =
        repository.columnForTest("project", projectId, "current_stage_id")
}
