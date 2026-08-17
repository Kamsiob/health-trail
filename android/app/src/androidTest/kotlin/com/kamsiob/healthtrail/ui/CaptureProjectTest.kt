package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureFormState
import com.kamsiob.healthtrail.ui.screens.CaptureFormTags
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An entry captured anywhere can say which project it belongs to. #303.
 *
 * **The read path shipped in Phase 1 and nothing outside a project wrote it.**
 * `latestWordFor` reads the link and `LatestWordCard` renders it, and the only
 * writer was the sheet inside a project, so a call taken in a corridor about
 * the appeal reached the trail and never reached the appeal. The card rendered
 * its none-yet rung forever on any real notebook.
 *
 * **A screen test, because what was missing is a chip and a field on a draft.**
 * That the shell writes `link` when the draft carries a project is one line in
 * two save paths, and `ProjectTrailTest` already covers the reader.
 */
@RunWith(AndroidJUnit4::class)
class CaptureProjectTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val appeal = Repository.Project(
        id = "p1",
        name = "The coverage appeal",
        templateId = null,
        status = "active",
        waitingOn = null,
        notes = null,
        stepCount = 0,
        doneCount = 0,
        nextStep = null,
    )

    private fun show(
        kind: CaptureKind = CaptureKind.CALL,
        projects: List<Repository.Project> = listOf(appeal),
        onSave: (CaptureDraft) -> Unit = {},
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    var state by remember { mutableStateOf(CaptureFormState()) }
                    CaptureFormScreen(
                        kind = kind,
                        threads = emptyList(),
                        projects = projects,
                        state = state,
                        onStateChange = { state = it },
                        onSave = onSave,
                        onCancel = {},
                    )
                }
            }
        }
    }

    private fun showing(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Walks to the stage that holds the rest of the form rather than counting
     * taps, which is the lesson #308 cost: a hard coded number of stages fails
     * after a redesign with a message about the wrong thing.
     *
     * **There is nothing to open**, D185: the group carries a label and the
     * chips are on the screen. Arriving at the stage is the whole job.
     */
    private fun walkToTheRestOfTheForm() {
        repeat(3) {
            if (showing(CaptureFormTags.MORE)) {
                return
            }
            // **Not scrolled to.** The way on and the save are pinned outside
            // the scrolling region on this form, deliberately, so asking them
            // to scroll raises rather than doing nothing.
            compose.onNodeWithTag(CaptureFormTags.NEXT).performClick()
            compose.waitForIdle()
        }
    }

    @Test
    fun anEntryCanBeMarkedAsPartOfAProject() {
        var saved: CaptureDraft? = null
        show(onSave = { saved = it })

        walkToTheRestOfTheForm()
        compose.onNodeWithTag(CaptureFormTags.project(appeal.id)).performScrollTo().performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(appeal.id, saved?.projectId)
    }

    /** Tapping the chosen one clears it, the same as every other chip here. */
    @Test
    fun theProjectCanBeTakenOffAgain() {
        var saved: CaptureDraft? = null
        show(onSave = { saved = it })

        walkToTheRestOfTheForm()
        compose.onNodeWithTag(CaptureFormTags.project(appeal.id)).performScrollTo().performClick()
        compose.onNodeWithTag(CaptureFormTags.project(appeal.id)).performScrollTo().performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertNull(saved?.projectId)
    }

    /**
     * **Something that went wrong belongs to a project as much as a call
     * does**, and its save path writes the link through the entry the incident
     * is born with, which is the half a chip on a form cannot show.
     */
    @Test
    fun somethingThatWentWrongCanBelongToAproject() {
        var saved: CaptureDraft? = null
        show(kind = CaptureKind.INCIDENT, onSave = { saved = it })

        walkToTheRestOfTheForm()
        compose.onNodeWithTag(CaptureFormTags.project(appeal.id)).performScrollTo().performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(appeal.id, saved?.projectId)
    }

    /** A notebook with no projects is offered nothing, rather than an empty group. */
    @Test
    fun anotebookWithNoProjectsIsOfferedNothing() {
        show(projects = emptyList())
        compose.onNodeWithTag(CaptureFormTags.project(appeal.id)).assertDoesNotExist()
    }
}
