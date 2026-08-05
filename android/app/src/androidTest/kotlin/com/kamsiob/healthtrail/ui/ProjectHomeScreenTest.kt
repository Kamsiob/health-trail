package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ProjectHomeScreen
import com.kamsiob.healthtrail.ui.screens.ProjectHomeTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One grammar, three arrangements. `DESIGN.md` 20.3.
 *
 * **The shape of a project is only the order of the same components**, and that
 * claim is the reason somebody who has learned one project can read the next
 * one. It is also the easiest thing on this surface to break by accident, since
 * every shape renders the same four things and a wrong order still looks like a
 * finished screen.
 *
 * **What is asserted is vertical position, not presence.** Every shape shows
 * all three answers; what differs is which one is at the top, and a test that
 * only checked they were all on screen would pass on all three orders at once.
 */
@RunWith(AndroidJUnit4::class)
class ProjectHomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun project(lead: String) = Repository.Project(
        id = "p1",
        name = "The waiver application",
        templateId = "medicaid_ltc",
        status = "active",
        waitingOn = null,
        notes = null,
        stepCount = 2,
        doneCount = 1,
        nextStep = "Gather the statements",
        lead = lead,
    )

    private val stages = listOf(
        Repository.ProjectStage("s1", "Applied", 0, "2026-03-05", 1L),
        Repository.ProjectStage("s2", "In review", 1, null, null),
    )

    private val standing = Repository.ProjectStanding(
        id = "st1",
        holderLabel = "The county",
        personId = null,
        organizationId = null,
        activity = "reviewing it",
        sinceEdtf = "2026-03",
        sinceStart = 1L,
        entryId = null,
        note = null,
    )

    private val nextDate = Repository.ProjectDate(
        id = "d1",
        kind = "Decision expected",
        dueEdtf = "2026-09-12",
        dueStart = 2L,
        sourceNote = "the letter of March 5",
        sourceDocumentId = null,
        sourceEntryId = null,
    )

    private val steps = listOf(
        Repository.ProjectStep("x1", "Get the form", "2026-03-06", null, "The paperwork", "Me"),
        Repository.ProjectStep("x2", "Gather the statements", null, null, "The paperwork", null),
    )

    private fun show(lead: String, steps: List<Repository.ProjectStep> = this.steps) {
        compose.setContent {
            CompositionLocalProvider(
                LocalStrings provides Strings.load(context, Locale.ENGLISH),
            ) {
                HealthTrailTheme {
                    ProjectHomeScreen(
                        project = project(lead),
                        stages = stages,
                        standing = standing,
                        nextDate = nextDate,
                        latestWord = null,
                        countdown = "12 days",
                        dateKind = nextDate.kind,
                        dateWhen = "September 12, 2026",
                        standingSince = "reviewing it",
                        steps = steps,
                        papers = emptyList(),
                        onBack = {},
                    )
                }
            }
        }
    }

    private fun topOf(tag: String): Float =
        compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    /** The same, for a heading the catalog cannot name: it is composed from data. */
    private fun topOfText(text: String): Float =
        compose.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y

    /**
     * Every shape shows all three answers.
     *
     * **The order changes and the content does not**, 20.3. A shape that
     * dropped an answer would be a different screen rather than a different
     * arrangement, so each of the three tests below asserts this as well as the
     * order. It is not a fourth test, because `setContent` may be called once
     * per rule and a loop over the three shapes silently tests only the first.
     */
    private fun assertAllThreeAnswersAreThere() {
        compose.onNodeWithTag(ProjectHomeTags.STANDING).assertIsDisplayed()
        compose.onNodeWithTag(ProjectHomeTags.DATE).assertIsDisplayed()
        compose.onNodeWithTag(ProjectHomeTags.LATEST).assertIsDisplayed()
    }

    @Test
    fun theLongRoadOpensWithWhereItStands() {
        show("standing")
        assertAllThreeAnswersAreThere()
        assertTrue(
            "the date came before where it stands on the long road",
            topOf(ProjectHomeTags.STANDING) < topOf(ProjectHomeTags.DATE),
        )
    }

    @Test
    fun theClosingWindowOpensWithTheNextDate() {
        show("date")
        assertAllThreeAnswersAreThere()
        assertTrue(
            "where it stands came before the date on the closing window",
            topOf(ProjectHomeTags.DATE) < topOf(ProjectHomeTags.STANDING),
        )
    }

    @Test
    fun theBusyStretchOpensWithTheSteps() {
        show("steps")
        assertAllThreeAnswersAreThere()
        // **The cluster is open from the start here**, because somebody in the
        // middle of two intense weeks opens this to see what is left rather
        // than to read a sentence about an office.
        //
        // Asserted by position rather than by the step's own words: `StepRow`
        // replaces its text with one composed description, which is what a
        // reader should hear, so there is no text node saying "Get the form".
        compose.onNodeWithTag(ProjectHomeTags.STEPS).assertIsDisplayed()
        assertTrue(
            "where it stands came before the steps on the busy stretch",
            topOf(ProjectHomeTags.STEPS) < topOf(ProjectHomeTags.STANDING),
        )
    }

    /**
     * The busy stretch groups its steps by area, with a count of what is there.
     *
     * `DESIGN.md` 20.3. **The count is how many steps are in the area and never
     * how many are done**, rule 13: this screen does not measure the person's
     * own work, and the grid's "1 OF 3" is the one thing in the drawing that
     * this does not draw. `DECISIONS.md` D116.
     *
     * **A step nobody has filed keeps its place.** It runs after the named
     * areas without a heading of its own, because hiding it until it was tidy
     * would be the app asking to be organized before it would help.
     */
    @Test
    fun theBusyStretchGroupsItsStepsByArea() {
        show(
            "steps",
            listOf(
                Repository.ProjectStep("g1", "Call them", null, null, "The phone calls", null),
                Repository.ProjectStep("g2", "Get the form", null, null, "The paperwork", null),
                Repository.ProjectStep("g3", "Send it certified", null, null, "The paperwork", null),
                Repository.ProjectStep("g4", "Ask in writing", null, null, null, null),
            ),
        )

        // Composed the way the header composes it, isolate marks and all. A
        // test that compared the bare words passed for months while the screen
        // rendered something else, which is how SituationPickerTest broke.
        val calls = Bidi.join("THE PHONE CALLS", "1")
        val paperwork = Bidi.join("THE PAPERWORK", "2")
        compose.onNodeWithText(calls).assertIsDisplayed()
        compose.onNodeWithText(paperwork).assertIsDisplayed()

        assertTrue(
            "the areas ran in the order the steps came in",
            topOfText(calls) < topOfText(paperwork),
        )
    }

    /**
     * A project whose steps have no areas gets no headings.
     *
     * A heading over the whole list says nothing that the list does not already
     * say, and inventing one would be decoration standing in for hierarchy,
     * rule 15.
     */
    @Test
    fun stepsWithNoAreaGetNoHeadings() {
        show(
            "steps",
            listOf(
                Repository.ProjectStep("n1", "Call them", null, null, null, null),
                Repository.ProjectStep("n2", "Get the form", null, null, "", null),
            ),
        )

        compose.onNodeWithTag(ProjectHomeTags.STEPS).assertIsDisplayed()
        // A blank area is not an area. Were the guard dropped, the empty string
        // would become a group of its own and draw a heading with no words in
        // it, which is the placeholder rule 11 rules out.
        compose.onNodeWithText(Bidi.join("", "1")).assertDoesNotExist()
        compose.onNodeWithText(Bidi.join("", "2")).assertDoesNotExist()
    }
}
