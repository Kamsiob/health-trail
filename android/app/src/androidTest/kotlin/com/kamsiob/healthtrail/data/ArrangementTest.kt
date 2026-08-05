package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a person arranged is kept exactly as they left it.
 *
 * `contract/DATA-CONTRACT.md` 8.7 and `DESIGN.md` 21.8. **This is the trust
 * model of the Today surface expressed as tests**, because the promise is not
 * that the layout is stored, it is that nothing moves it. A dashboard that
 * quietly reorders itself is one nobody can trust to still be where they left
 * it, and the difference between screens 2 and 3 of the grid is data changing
 * inside a layout the person owns.
 *
 * The states ladder and the look of any of this are device work and are not
 * here. **What is here is the part a screenshot cannot show**: that the lead is
 * singular, that a card pointing at something gone is kept, and that no read
 * path reorders anything.
 */
@RunWith(AndroidJUnit4::class)
class ArrangementTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        Repository.closeForTest()
        repository = Repository.open(context)
        subjectId = repository.createSubject("Margaret")
    }

    // **Closed after every test, like every other data test here.** The
    // repository is a singleton over the one real database, so a test that
    // leaves it open hands the next class a repository it did not open. Leaving
    // it open made BackJourneyTest fail with "back at the notebook did not
    // leave the app", four classes later and with nothing to connect them.
    @After
    fun tearDown() = Repository.closeForTest()

    private suspend fun startingHand() = repository.setTodayLayout(
        subjectId,
        listOf(
            "digest" to "wide",
            "next_up" to "small",
            "medications" to "small",
            "incidents" to "small",
        ),
    )

    // -- the lead is singular ----------------------------------------------

    @Test
    fun aStartingHandLeadsWithItsFirstCardAndTheRestAreTheField() = runBlocking {
        assertEquals(4, startingHand())

        val layout = repository.todayLayout(subjectId)
        assertNotNull("the layout was not written", layout)
        assertEquals("digest", layout!!.lead.type)
        assertEquals("wide", layout.lead.size)
        assertEquals(
            listOf("next_up", "medications", "incidents"),
            layout.field.map { it.type },
        )
    }

    @Test
    fun promotingACardDemotesTheOldLeadRatherThanLeavingTwo() = runBlocking {
        startingHand()
        val medications = repository.todayLayout(subjectId)!!.field.first { it.type == "medications" }

        assertTrue(repository.promoteTodayCardToLead(medications.id))

        val layout = repository.todayLayout(subjectId)!!
        assertEquals("medications", layout.lead.type)
        assertEquals("there is more than one lead", 1, layout.all.count { it.isLead })
        // The demoted card goes to the top of the field, where the eye already
        // is, rather than to the bottom of a scroll.
        assertEquals("digest", layout.field.first().type)
    }

    // **The database refusing a second lead is proved in check_schema.py**,
    // against the same contract/schema.sql this build runs, and it runs on every
    // push rather than only when a phone is attached. What is proved here is the
    // layer above it: that promoting demotes, so the refusal is never reached.

    @Test
    fun theLeadCannotBeRemoved() = runBlocking {
        startingHand()
        val lead = repository.todayLayout(subjectId)!!.lead

        assertFalse("the lead was removed, leaving zero", repository.removeTodayCard(lead.id))

        val layout = repository.todayLayout(subjectId)!!
        assertEquals("digest", layout.lead.type)
        assertEquals(4, layout.all.size)
    }

    @Test
    fun aFieldCardIsRemovedByTombstoneAndNothingElseMoves() = runBlocking {
        startingHand()
        val before = repository.todayLayout(subjectId)!!
        val medications = before.field.first { it.type == "medications" }

        assertTrue(repository.removeTodayCard(medications.id))

        val after = repository.todayLayout(subjectId)!!
        assertEquals(listOf("next_up", "incidents"), after.field.map { it.type })
        assertEquals("the lead moved when a field card was removed", "digest", after.lead.type)
        // A tombstone, never a delete. Rule 3.
        assertTrue(
            "the row was deleted rather than tombstoned",
            repository.rowExistsForTest("today_card", medications.id),
        )
        assertNotNull(
            "the row survived but carries no tombstone",
            repository.columnForTest("today_card", medications.id, "deleted_at"),
        )
    }

    // -- nothing moves on its own ------------------------------------------

    @Test
    fun readingTheLayoutManyTimesReturnsTheSameOrderEveryTime() = runBlocking {
        startingHand()
        val first = repository.todayLayout(subjectId)!!.all.map { it.type }

        // Writing unrelated records is the whole point: this is screen 2 versus
        // screen 3 of the grid, where the data changes and the layout does not.
        // Under the same subject rather than a second one, because a second
        // subject changes what the app itself considers current, and this test
        // is about the layout rather than about whose layout it is.
        repository.startProject(
            subjectId, "medicaid_ltc", "Something new to look at", listOf("A step"),
        )
        repeat(5) {
            val seen = repository.todayLayout(subjectId)!!.all.map { it.type }
            assertEquals("the layout reordered itself between reads", first, seen)
        }
    }

    @Test
    fun aLayoutWithNoLeadIsReportedAsBrokenRatherThanRepairedQuietly() = runBlocking {
        startingHand()
        repository.clearEveryLeadForTest(subjectId)

        // Null, not a layout with the first card silently promoted. Inventing a
        // lead would be the app arranging Today, and it would hide the defect.
        assertNull(repository.todayLayout(subjectId))
    }

    @Test
    fun moveUpAndMoveDownReorderTheFieldAndTheLeadDoesNotTakePart() = runBlocking {
        startingHand()
        val incidents = repository.todayLayout(subjectId)!!.field.first { it.type == "incidents" }

        assertTrue(repository.moveTodayCard(incidents.id, earlier = true))
        assertEquals(
            listOf("next_up", "incidents", "medications"),
            repository.todayLayout(subjectId)!!.field.map { it.type },
        )

        // The top field card cannot displace the lead: there is no neighbor
        // above it inside the field.
        val top = repository.todayLayout(subjectId)!!.field.first()
        assertFalse(repository.moveTodayCard(top.id, earlier = true))
        assertEquals("digest", repository.todayLayout(subjectId)!!.lead.type)
    }

    // -- a source that no longer resolves is kept --------------------------

    @Test
    fun aCardPointingAtSomethingGoneIsKeptRatherThanDropped() = runBlocking {
        startingHand()
        val projectId = repository.startProject(
            subjectId = subjectId,
            templateId = "medicaid_ltc",
            name = "The waiver application",
            steps = listOf("Get the form"),
        )
        val cardId = repository.addTodayCard(
            subjectId = subjectId,
            type = "project_standing",
            size = "wide",
            sourceTable = "project",
            sourceId = projectId,
        )

        // The project goes away underneath the card, which is the source-closed
        // rung of the states ladder, 21.4.
        repository.tombstoneForTest("project", projectId)

        val card = repository.todayLayout(subjectId)!!.all.firstOrNull { it.id == cardId }
        assertNotNull("the card was dropped when its source went away", card)
        assertEquals("project", card!!.sourceTable)
        assertEquals(
            "the reference was cleared, so the card can never point back",
            projectId,
            card.sourceId,
        )
    }

    // -- the shape a person gave a project ---------------------------------

    @Test
    fun aProjectRemembersItsRoadAndWhereItStandsOnIt() = runBlocking {
        val projectId = repository.startProject(
            subjectId = subjectId,
            templateId = "medicaid_ltc",
            name = "The waiver application",
            steps = listOf("Get the form"),
        )
        val applied = repository.addProjectStage(projectId, "Applied")
        val review = repository.addProjectStage(projectId, "In review")
        repository.addProjectStage(projectId, "Decision")

        repository.moveProjectToStage(projectId, applied, Edtf.parse("2026-03-05")!!)
        repository.moveProjectToStage(projectId, review, Edtf.parse("2026-04-12")!!)

        val stages = repository.projectStages(projectId)
        assertEquals(listOf("Applied", "In review", "Decision"), stages.map { it.name })
        assertEquals(listOf(true, true, false), stages.map { it.isReached })

        // A road that turns back keeps the first arrival rather than overwriting
        // it, because the trail carries the sequence.
        repository.moveProjectToStage(projectId, applied, Edtf.parse("2026-05-01")!!)
        assertEquals(
            "returning to a stage rewrote when it was first reached",
            "2026-03-05",
            repository.projectStages(projectId).first { it.id == applied }.enteredEdtf,
        )
    }

    @Test
    fun whereItStandsIsTheMostRecentAndTheHistoryIsKept() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "The waiver application", listOf("Get the form"),
        )
        repository.addProjectStanding(
            projectId, holderLabel = "The county", since = Edtf.parse("2026-03-05")!!,
            activity = "reviewing it",
        )
        repository.addProjectStanding(
            projectId, holderLabel = "The bank", since = Edtf.parse("2026-04-20")!!,
            activity = "sending the statements",
        )

        assertEquals("The bank", repository.projectStanding(projectId)?.holderLabel)
        assertEquals(
            "where it stood before was lost",
            listOf("The bank", "The county"),
            repository.projectStandingHistory(projectId).map { it.holderLabel },
        )
    }

    @Test
    fun whereItStandsIsNullBeforeAnybodyHasSaid() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "The waiver application", listOf("Get the form"),
        )
        // Null is a real answer and the screen says so plainly. It is not an
        // empty string and not an invented sentence.
        assertNull(repository.projectStanding(projectId))
    }

    @Test
    fun theLeadingDateIsTheSoonestThatHasNotPassed() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "The waiver application", listOf("Get the form"),
        )
        val march = Edtf.parse("2026-03-01")!!
        val may = Edtf.parse("2026-05-01")!!
        val july = Edtf.parse("2026-07-01")!!
        repository.addProjectDate(projectId, "Hearing", july, sourceNote = "the letter of Mar 5")
        repository.addProjectDate(projectId, "Filing deadline", may)
        repository.addProjectDate(projectId, "Applied", march)

        val april = Edtf.resolve(Edtf.parse("2026-04-01")!!, java.time.ZoneId.systemDefault()).start!!

        // D113. The soonest that has not passed, with no column marking it and
        // nothing asked of the person.
        assertEquals(
            "Filing deadline",
            repository.leadingProjectDate(projectId, now = april)?.kind,
        )

        // And when they have all passed, the most recent, rather than nothing.
        val nextYear = Edtf.resolve(Edtf.parse("2027-01-01")!!, java.time.ZoneId.systemDefault()).start!!
        assertEquals("Hearing", repository.leadingProjectDate(projectId, now = nextYear)?.kind)
    }

    @Test
    fun aDateCarriesWhereItCameFrom() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "The waiver application", listOf("Get the form"),
        )
        repository.addProjectDate(
            projectId, "Filing deadline", Edtf.parse("2026-04-12")!!,
            sourceNote = "the letter of Mar 5",
        )
        // The source is the half that makes the date usable a year later. A bare
        // Apr 12 is not.
        assertEquals(
            "the letter of Mar 5",
            repository.projectDates(projectId).single().sourceNote,
        )
    }

    @Test
    fun anEmptyPaperPlaceholderIsNotYetRatherThanMissing() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "The waiver application", listOf("Get the form"),
        )
        val paperId = repository.addProjectPaper(projectId, "Proof of income")
        repository.addProjectPaper(projectId, "Bank statements")

        val papers = repository.projectPapers(projectId)
        assertEquals(listOf("Proof of income", "Bank statements"), papers.map { it.name })
        assertEquals(
            "an empty placeholder should be unfilled, not absent",
            listOf(false, false),
            papers.map { it.isFilled },
        )

        val documentId = repository.createDocument(
            subjectId = subjectId,
            title = "The award letter",
            received = Edtf.parse("2026-04-20")!!,
        )
        repository.fillProjectPaper(paperId, documentId, direction = "received")

        val filled = repository.projectPapers(projectId).first { it.id == paperId }
        assertTrue(filled.isFilled)
        assertEquals("received", filled.direction)
    }

    @Test
    fun aHandlerTagIsALabelAndNothingMore() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "Coming home", listOf("Order the bed"),
        )
        val step = repository.projectSteps(projectId).single()
        repository.setProjectStepHandling(step.id, cluster = "The house", handlerLabel = "My brother")

        val after = repository.projectSteps(projectId).single()
        assertEquals("The house", after.cluster)
        assertEquals("My brother", after.handlerLabel)
        // No account, no address, no link to person. D108: it is the person
        // writing down who said they would do a thing.
        assertFalse(
            "a handler tag became an identity",
            after.handlerLabel!!.contains("@"),
        )
    }

    // -- a template is five defaults, and all five are applied ---------------

    @Test
    fun everyShippedTemplateCarriesTheFiveDefaults() = runBlocking {
        // The catalog on the device, not the file in the repository.
        // check_templates.py holds the data to this; this holds the build to it,
        // because a field that never reaches the assets is a field the app does
        // not have.
        val templates = TemplateCatalog.projects(context)
        assertEquals(16, templates.size)
        templates.forEach { template ->
            assertTrue(
                "${template.id} leads with ${template.lead}",
                template.lead in setOf("standing", "date", "steps"),
            )
            assertTrue("${template.id} has no stages", template.stages.size >= 2)
            assertTrue("${template.id} has no date kinds", template.dateKinds.isNotEmpty())
            assertTrue("${template.id} has no papers", template.papers.isNotEmpty())
        }
        // All three shapes exist in the catalog, or two of the three project
        // home screens can never be reached from a shipped template.
        assertEquals(
            setOf("standing", "date", "steps"),
            templates.map { it.lead }.toSet(),
        )
    }

    @Test
    fun startingFromATemplateAppliesAllFiveDefaults() = runBlocking {
        val template = TemplateCatalog.projects(context).first { it.id == "discharge_planning" }

        val projectId = repository.startProject(
            subjectId = subjectId,
            templateId = template.id,
            name = template.name,
            steps = template.steps,
            lead = template.lead,
            stages = template.stages,
            dateKinds = template.dateKinds,
            papers = template.papers,
        )

        assertEquals(template.lead, repository.columnForTest("project", projectId, "lead"))
        assertEquals(template.stages, repository.projectStages(projectId).map { it.name })
        assertEquals(template.dateKinds, repository.projectDateKinds(projectId))
        assertEquals(template.papers, repository.projectPapers(projectId).map { it.name })
        assertEquals(template.steps, repository.projectSteps(projectId).map { it.text })

        // **Nothing is entered yet.** A project created a second ago has not
        // reached a stage, and that is a real state rather than an error.
        assertNull(repository.columnForTest("project", projectId, "current_stage_id"))
        assertTrue(repository.projectStages(projectId).none { it.isReached })
        // And no paper is filled, which reads as not yet.
        assertTrue(repository.projectPapers(projectId).none { it.isFilled })
    }

    @Test
    fun aTemplateIsCopiedSoEditingTheProjectNeverTouchesIt() = runBlocking {
        val template = TemplateCatalog.projects(context).first { it.id == "discharge_planning" }
        val projectId = repository.startProject(
            subjectId, template.id, template.name, template.steps,
            template.lead, template.stages, template.dateKinds, template.papers,
        )

        // Reshape the project completely. 20.4: applied once, at setup, and
        // after that there is no live link in either direction.
        repository.setProjectLead(projectId, "standing")
        repository.addProjectStage(projectId, "A stage the template never had")

        val again = TemplateCatalog.projects(context).first { it.id == "discharge_planning" }
        assertEquals("editing a project changed the template", template.lead, again.lead)
        assertEquals(template.stages, again.stages)
    }

    @Test
    fun savingAProjectAsATemplateKeepsItsWholeShape() = runBlocking {
        val template = TemplateCatalog.projects(context).first { it.id == "discharge_planning" }
        val projectId = repository.startProject(
            subjectId, template.id, template.name, template.steps,
            template.lead, template.stages, template.dateKinds, template.papers,
        )
        repository.saveProjectAsTemplate(projectId, "How we did it last time")

        val own = repository.ownTemplates("project").first { it.name == "How we did it last time" }
        // **The whole shape, not only the checklist.** What somebody spent
        // months arranging is the road and the lead as much as the steps, and a
        // template that kept only the steps hands back the least useful part.
        assertEquals(template.lead, own.lead)
        assertEquals(template.stages, own.stages)
        assertEquals(template.dateKinds, own.dateKinds)
        assertEquals(template.papers, own.papers)
        assertEquals(template.steps, own.steps)
        // The lineage travels, so the library can say what this grew out of.
        assertEquals(template.id, own.derivedFromId)
    }

    @Test
    fun theShapeIsADefaultAndNotACage() = runBlocking {
        val projectId = repository.startProject(
            subjectId, "medicaid_ltc", "The waiver application", listOf("Get the form"),
        )
        assertEquals("standing", repository.columnForTest("project", projectId, "lead"))
        repository.setProjectLead(projectId, "date")
        assertEquals("date", repository.columnForTest("project", projectId, "lead"))
        repository.setProjectLead(projectId, "steps")
        assertEquals("steps", repository.columnForTest("project", projectId, "lead"))
    }
}
