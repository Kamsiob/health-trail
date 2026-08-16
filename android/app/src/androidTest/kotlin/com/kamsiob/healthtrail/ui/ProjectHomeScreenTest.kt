package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ProjectHomeScreen
import com.kamsiob.healthtrail.ui.screens.ProjectHomeTags
import com.kamsiob.healthtrail.ui.screens.SectionTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One order, every project, no exceptions. D164.
 *
 * **The old contract was "one grammar, three arrangements": the same blocks in
 * a lead-dependent order.** The owner's verdict on the result, live, was
 * "menus and sub menus and tabs and accordions. it's just a gigantic mess",
 * and the root of it was exactly the reordering: a person could learn one
 * project and be lost on the next, because the screen itself moved.
 *
 * **The new contract is the opposite claim**: the answer, the date, the three
 * verbs, the road, the latest word, the file. In that order on the long road,
 * on the closing window, and on the busy stretch alike. These tests hold the
 * order constant across every lead value the old design used to reorder by,
 * which is the strongest possible statement that the reordering is gone.
 */
@RunWith(AndroidJUnit4::class)
class ProjectHomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

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

    private fun show(
        lead: String,
        onLogCall: () -> Unit = {},
        onAddDate: () -> Unit = {},
        onUpdateStanding: () -> Unit = {},
        onOpenSteps: () -> Unit = {},
        onOpenTrail: () -> Unit = {},
        onOpenPaperwork: () -> Unit = {},
        onOpenPeople: () -> Unit = {},
        onOpenSetup: () -> Unit = {},
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
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
                        onLogCall = onLogCall,
                        onAddDate = onAddDate,
                        onUpdateStanding = onUpdateStanding,
                        onOpenSteps = onOpenSteps,
                        onOpenTrail = onOpenTrail,
                        onOpenPaperwork = onOpenPaperwork,
                        onOpenPeople = onOpenPeople,
                        onOpenSetup = onOpenSetup,
                    )
                }
            }
        }
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag(SectionTags.root(ProjectHomeTags.NAME))
            .performScrollToNode(hasTestTag(tag))
    }

    private fun topOf(tag: String): Float =
        compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    /**
     * The reordering is gone: the answer leads and the date follows on every
     * shape the old design shuffled. Run for each historical lead value, so a
     * regression that brings the shuffling back fails three ways at once.
     */
    @Test
    fun everyShapeReadsInTheSameOrder() {
        // **One composition, three shapes.** setContent may run once per
        // test, so the lead is state and the same tree re-reads for each
        // value the old design used to reorder by.
        val lead = androidx.compose.runtime.mutableStateOf("standing")
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectHomeScreen(
                        project = project(lead.value),
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

        for (shape in listOf("standing", "date", "steps")) {
            compose.runOnUiThread { lead.value = shape }
            compose.waitForIdle()
            compose.onNodeWithTag(ProjectHomeTags.STANDING).assertIsDisplayed()
            assertTrue(
                "on lead=$shape the answer no longer leads",
                topOf(ProjectHomeTags.STANDING) < topOf(ProjectHomeTags.DATE),
            )
            assertTrue(
                "on lead=$shape the verbs are not under the date",
                topOf(ProjectHomeTags.DATE) < topOf(ProjectHomeTags.LOG_CALL),
            )
        }
    }

    /** The three verbs are one row: same height, all present, all firing. */
    @Test
    fun theThreeVerbsSitTogetherAndFire() {
        var calls = 0
        var dates = 0
        var standings = 0
        show("standing", onLogCall = { calls++ }, onAddDate = { dates++ }, onUpdateStanding = { standings++ })

        val callTop = topOf(ProjectHomeTags.LOG_CALL)
        assertEquals(
            "the verbs are not one row",
            callTop,
            topOf(ProjectHomeTags.ADD_DATE),
            1f,
        )
        assertEquals(
            "the third verb left the row",
            callTop,
            topOf(ProjectHomeTags.UPDATE_STANDING),
            1f,
        )

        compose.onNodeWithTag(ProjectHomeTags.LOG_CALL).performClick()
        compose.onNodeWithTag(ProjectHomeTags.ADD_DATE).performClick()
        compose.onNodeWithTag(ProjectHomeTags.UPDATE_STANDING).performClick()
        assertEquals(1, calls)
        assertEquals(1, dates)
        assertEquals(1, standings)
    }

    /**
     * The road is vertical and the move control lives on the current stage.
     * Rule 18: the control sits where the state it changes sits.
     */
    @Test
    fun theRoadCarriesItsMoveControlOnTheCurrentStage() {
        show("standing")
        // The move control is the bottom of the road block, so landing it on
        // screen lands the whole road. The names are asserted as present in
        // the tree rather than as visible, because a tall road can honestly
        // run past either screen edge on a small window.
        scrollTo(ProjectHomeTags.MOVE_STAGE)
        // docs/TRAPS.md: the names are bidi isolated, so the finder matches
        // the isolated string rather than the raw one.
        compose.onNodeWithText(Bidi.isolate("Applied")).assertExists()
        compose.onNodeWithText(Bidi.isolate("In review")).assertExists()
        compose.onNodeWithTag(ProjectHomeTags.MOVE_STAGE).assertIsDisplayed()
    }

    /**
     * The file is five doors that look like doors: every row navigates, and
     * none of them unfolds anything in place. The accordion contract, dead.
     */
    @Test
    fun theFileRowsAllNavigate() {
        var steps = 0
        var trail = 0
        var papers = 0
        var people = 0
        var setup = 0
        show(
            "standing",
            onOpenSteps = { steps++ },
            onOpenTrail = { trail++ },
            onOpenPaperwork = { papers++ },
            onOpenPeople = { people++ },
            onOpenSetup = { setup++ },
        )

        for ((tag, _) in listOf(
            ProjectHomeTags.STEPS to "steps",
            ProjectHomeTags.TRAIL to "trail",
            ProjectHomeTags.PAPERS to "papers",
            ProjectHomeTags.PEOPLE to "people",
            ProjectHomeTags.SETUP to "setup",
        )) {
            scrollTo(tag)
            compose.onNodeWithTag(tag).performClick()
        }

        assertEquals("the steps row did not navigate", 1, steps)
        assertEquals("the trail row did not navigate", 1, trail)
        assertEquals("the papers row did not navigate", 1, papers)
        assertEquals("the people row did not navigate", 1, people)
        assertEquals("the setup row did not navigate", 1, setup)
    }

    /** The empty latest word still names its own question. Unchanged from the old contract. */
    @Test
    fun theEmptyLatestWordStillSaysWhatItIs() {
        show("standing")
        scrollTo(ProjectHomeTags.LATEST)
        compose.onNodeWithText(strings["project.latest_word"]).assertIsDisplayed()
        compose.onNodeWithText(strings["project.word.none"]).assertIsDisplayed()
    }
}
