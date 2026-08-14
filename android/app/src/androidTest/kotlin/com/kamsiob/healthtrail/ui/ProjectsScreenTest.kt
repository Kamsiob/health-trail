package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ProjectTags
import com.kamsiob.healthtrail.ui.screens.ProjectsScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The projects tab before the first project, and after it. `DESIGN.md` 20.5.
 *
 * **The empty state is the screen a person sees first and the one most likely
 * to ship thin**, rule 11 and rule 14. It carried one gray paragraph under the
 * subtitle for months while every other empty screen in the app had a drawing,
 * a line at size, and something to do.
 */
@RunWith(AndroidJUnit4::class)
class ProjectsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val aProject = Repository.Project(
        id = "p1",
        name = "Medicaid application",
        templateId = null,
        status = "active",
        waitingOn = null,
        notes = null,
        stepCount = 3,
        doneCount = 1,
        nextStep = "Call and ask what form it is",
        lead = "standing",
    )

    private var started = 0

    private fun show(projects: List<Repository.Project>) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectsScreen(
                        projects = projects,
                        onOpen = {},
                        onStart = { started++ },
                    )
                }
            }
        }
    }

    /**
     * Before the first one: a line that says what this place is for, the
     * paragraph, and the one thing to do.
     *
     * **Not a blank and not one gray sentence.** Rule 11 rules out a screen
     * that ships with an empty area, and rule 15 rules out three things at one
     * weight: the lead takes the size and the paragraph recedes under it.
     */
    @Test
    fun beforeTheFirstProjectTheScreenSaysWhatThisPlaceIsForAndOffersTheOneAction() {
        show(emptyList())

        compose.onNodeWithTag(ProjectTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText(strings["projects.empty.lead"]).assertIsDisplayed()
        compose.onNodeWithText(strings["projects.empty"]).assertIsDisplayed()

        compose.onNodeWithTag(ProjectTags.EMPTY_START).assertIsDisplayed().performClick()
        assertTrue("the empty state's action did not start a project", started == 1)
    }

    /**
     * The subtitle describes the rows, so an empty screen does without it.
     *
     * It opened with the same four words as the empty state's own lead, so the
     * screen said "the long processes" twice, one line above the other, at the
     * one moment it has the reader's whole attention.
     */
    @Test
    fun theCountIsGoneWhileThereAreNoProjectsAndBackWhenThereAre() {
        show(emptyList())
        compose.onNodeWithTag(ProjectTags.COUNTS).assertDoesNotExist()
    }

    @Test
    fun theCountSaysTheStateOfTheProjectsRatherThanWhatAProjectIs() {
        // **A fact about this person's own list**, per grid screen 02, which
        // draws a count here. It used to be three lines saying what a project
        // is, to somebody who already has one. Rule 20, and D142 settles which
        // wins because nothing recorded a departure.
        //
        // **The finished half is absent when there is none**, so a first
        // project does not arrive beside a zero.
        show(listOf(aProject))
        compose.onNodeWithTag(ProjectTags.COUNTS)
            .assertTextEquals(strings("projects.subtitle.counts", "live" to 1, "finished" to 0))
        compose.onNodeWithTag(ProjectTags.START).assertIsDisplayed()
        // **One start action at a time.** The empty state carries its own, and
        // the two would otherwise both be on screen saying the same thing.
        compose.onNodeWithTag(ProjectTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun theEmptyStateDoesNotRepeatTheStartButtonBelowIt() {
        show(emptyList())
        compose.onNodeWithTag(ProjectTags.START).assertDoesNotExist()
    }
}
