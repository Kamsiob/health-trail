package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A setting applies its first days list and its papers, not only its threads.
 *
 * **`templates/data/situations.json` carried ten checklist items and six
 * document slots per setting and nothing in the app read either.** #135. P2's
 * requirement is one sentence, that a situation template applies its roles,
 * threads, checklist and document slots, and two of the four did not exist.
 *
 * **The checklist is the highest value content in the whole catalog.** For a
 * nursing home it is writing down the direct number for the unit rather than
 * the main line, asking for the current medication list, asking who calls when
 * something changes, and asking for the written grievance procedure. That is
 * what somebody needs in the week they know least, and it was invisible.
 *
 * **It becomes a project rather than a thirteenth section**, D136, because a
 * project already is a named process holding steps somebody edits, marks done
 * and removes, and papers that are named slots waiting for a document.
 */
@RunWith(AndroidJUnit4::class)
class FirstDaysTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private val checklist = listOf(
        "Write down the direct phone number for the unit, not just the main line",
        "Ask for the current medication list and keep a copy",
    )

    private val papers = listOf("Admission agreement", "Resident rights notice")

    private suspend fun apply(
        repository: Repository,
        subjectId: String,
        templateId: String = "nursing_home",
        name: String? = "The first days",
    ) = repository.applySituation(
        subjectId = subjectId,
        templateId = templateId,
        threads = listOf("nursing" to "Nursing"),
        startingHand = listOf("digest" to "wide", "next_up" to "small"),
        checklist = checklist,
        documents = papers,
        firstDaysName = name,
    )

    @Test
    fun theChecklistBecomesStepsAndTheSlotsBecomePapers() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "First days subject")

        apply(repository, subjectId)

        val project = repository.projects(subjectId).single()
        assertEquals("The first days", project.name)
        assertEquals("nursing_home", project.templateId)
        // **Steps lead**, 20.3: this shape is many small arrangements rather
        // than one slow process being waited on.
        assertEquals("steps", project.lead)

        assertEquals(checklist, repository.projectSteps(project.id).map { it.text })
        assertEquals(papers, repository.projectPapers(project.id).map { it.name })
    }

    /**
     * **Every step arrives not done, and that is a finished state.**
     *
     * Rule 13: an unfilled thing reads as "not yet" and never as a deficiency,
     * and nothing here counts how much of it somebody has got through.
     */
    @Test
    fun everyStepArrivesNotDoneAndNothingIsScored() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Not done yet")

        apply(repository, subjectId)
        val project = repository.projects(subjectId).single()

        assertTrue(repository.projectSteps(project.id).none { it.isDone })
        assertEquals(0, project.doneCount)

        // And a step can be taken, which is the other half of "editable".
        val first = repository.projectSteps(project.id).first()
        repository.setProjectStepDone(first.id, true)
        assertTrue(repository.projectSteps(project.id).first().isDone)
    }

    /**
     * **A paper slot arrives empty, which is a named thing waiting.**
     *
     * "How a document slot reads before there is a document in it" is one of the
     * two questions #135 said to settle first. It reads as the name of the paper
     * with nothing filed against it yet, which is what `project_paper` already
     * does everywhere else in the app.
     */
    @Test
    fun aPaperSlotArrivesEmptyAndNamed() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Empty slots")

        apply(repository, subjectId)
        val project = repository.projects(subjectId).single()

        val slots = repository.projectPapers(project.id)
        assertEquals(papers.size, slots.size)
        assertTrue(slots.all { it.documentId == null })
    }

    /**
     * **Today points at it**, because invisible was the defect being fixed.
     *
     * Left in the Projects tab alone the list would be one tab away with
     * nothing pointing at it, which is the same absence in a nicer place. The
     * card is appended rather than promoted: 21.1 allows exactly one lead and
     * the setting's own hand already has one.
     */
    @Test
    fun todayCarriesACardPointedAtIt() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Today points at it")

        apply(repository, subjectId)
        val project = repository.projects(subjectId).single()

        val layout = repository.todayLayout(subjectId)
        assertNotNull(layout)
        val card = layout!!.all.last()
        assertEquals("project_steps", card.type)
        assertEquals("project", card.sourceTable)
        assertEquals(project.id, card.sourceId)
        assertTrue("the appended card must not take the lead", !card.isLead)
    }

    /**
     * **The same setting applied twice makes one list, not two.**
     *
     * Somebody who goes back through setup, or a restore that replays it, must
     * not end up with the same ten things twice.
     */
    @Test
    fun applyingTheSameSettingTwiceMakesOneList() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Twice")

        apply(repository, subjectId)
        apply(repository, subjectId)

        assertEquals(1, repository.projects(subjectId).size)
    }

    /**
     * **A different setting is a different set of first days**, and the old one
     * is untouched.
     *
     * `MASTER_SPEC.md` 4.6 asks that changing settings carries both forward
     * rather than discarding them, and here that is true by construction rather
     * than by a merge: the first list is a project and nothing about applying a
     * second setting reaches it.
     */
    @Test
    fun adifferentSettingAddsItsOwnAndKeepsTheFirst() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Moved")

        apply(repository, subjectId, templateId = "hospital_stay")
        val first = repository.projects(subjectId).single()

        apply(repository, subjectId, templateId = "nursing_home")

        val all = repository.projects(subjectId)
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == first.id })
        assertEquals(
            setOf("hospital_stay", "nursing_home"),
            all.mapNotNull { it.templateId }.toSet(),
        )
    }

    /**
     * **A setting with nothing to ship makes nothing**, which matters because
     * two of the fourteen settings could reasonably ship an empty checklist and
     * a project called "The first days" with no steps in it would be a screen
     * saying nothing.
     */
    @Test
    fun aSettingWithNoChecklistAndNoPapersMakesNoProject() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Nothing to ship")

        repository.applySituation(
            subjectId = subjectId,
            templateId = "home_family",
            threads = listOf("nursing" to "Nursing"),
            startingHand = listOf("digest" to "wide"),
            checklist = emptyList(),
            documents = emptyList(),
            firstDaysName = "The first days",
        )

        assertTrue(repository.projects(subjectId).isEmpty())
        assertNull(repository.todayLayout(subjectId)!!.all.firstOrNull { it.sourceId != null })
    }
}
