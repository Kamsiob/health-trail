package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
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

    private fun show(lead: String) {
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
}
