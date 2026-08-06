package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ProjectSetupScreen
import com.kamsiob.healthtrail.ui.screens.ProjectSetupTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The project's setup screen, and the two controls it spent a week without.
 *
 * **#314.** Saving the project as a template and saying who the project is
 * waiting on both went with the superseded `ProjectDetailScreen`. Their
 * repository calls and their shell state survived the supersession and nothing
 * anywhere set them, which is a shape a screenshot cannot show and a compiler
 * will never complain about: the state is read, the effect is written, and the
 * control that would fire it does not exist.
 *
 * **So these assert the callback fires**, not that the screen draws something.
 */
@RunWith(AndroidJUnit4::class)
class ProjectSetupScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun project(waitingOn: String? = null) = Repository.Project(
        id = "p1",
        name = "The waiver application",
        templateId = "medicaid_ltc",
        status = "active",
        waitingOn = waitingOn,
        notes = null,
        stepCount = 2,
        doneCount = 1,
        nextStep = "Gather the statements",
        lead = "standing",
    )

    private val stages = listOf(
        Repository.ProjectStage("s1", "Applied", 0, "2026-03-05", 1L),
        Repository.ProjectStage("s2", "In review", 1, null, null),
    )
    private val steps = listOf(
        Repository.ProjectStep("t1", "Gather the statements", null, null),
    )
    private val papers = listOf(
        Repository.ProjectPaper("d1", "The award letter", 0, null, null),
    )

    private var waitingSaved: String? = null
    private var templateSaves = 0

    private fun show(
        project: Repository.Project = project(),
        stages: List<Repository.ProjectStage> = this.stages,
        steps: List<Repository.ProjectStep> = this.steps,
        papers: List<Repository.ProjectPaper> = this.papers,
        dateKinds: List<String> = listOf("Deadline"),
        savedAsTemplate: Boolean = false,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectSetupScreen(
                        project = project,
                        stages = stages,
                        steps = steps,
                        papers = papers,
                        dateKinds = dateKinds,
                        onSetLead = {},
                        onSetStatus = {},
                        onSetWaitingOn = { waitingSaved = it },
                        onSaveAsTemplate = { templateSaves++ },
                        onOpenSteps = {},
                        onOpenRoad = {},
                        onOpenKinds = {},
                        onOpenPapers = {},
                        onBack = {},
                        savedAsTemplate = savedAsTemplate,
                    )
                }
            }
        }
    }

    /**
     * Naming who the project is waiting on reaches the repository.
     *
     * The field is always offered rather than only when the status says
     * "waiting", because somebody is usually waiting on somebody long before
     * they think to change a status.
     */
    @Test
    fun whoTheProjectIsWaitingOnCanBeWrittenDown() {
        show()
        compose.onNodeWithTag(ProjectSetupTags.WAITING).performScrollTo()
        compose.onNodeWithTag(ProjectSetupTags.WAITING).performTextInput("Denise at intake")
        compose.onNodeWithTag(ProjectSetupTags.SAVE_WAITING).performScrollTo().performClick()
        assertEquals("Denise at intake", waitingSaved)
    }

    /**
     * Clearing the field is a save and not a no-op.
     *
     * Somebody who is no longer waiting on the county has to be able to say so,
     * and a save control that only appeared when the field had text in it would
     * leave a stale name on the projects list forever.
     */
    @Test
    fun theNameCanBeTakenBackOffAgain() {
        show(project(waitingOn = "Denise at intake"))
        compose.onNodeWithTag(ProjectSetupTags.WAITING).performScrollTo()
        compose.onNodeWithTag(ProjectSetupTags.WAITING).performTextClearance()
        compose.onNodeWithTag(ProjectSetupTags.SAVE_WAITING).performScrollTo().performClick()
        assertEquals("", waitingSaved)
    }

    /**
     * Nothing has changed, so there is nothing to save and no control.
     *
     * Rule 16: a control that does nothing on press reads as broken, and the
     * honest way to keep that true is to not draw it.
     */
    @Test
    fun anUnchangedFieldIsOfferedNoSave() {
        show(project(waitingOn = "Denise at intake"))
        compose.onNodeWithTag(ProjectSetupTags.SAVE_WAITING).assertDoesNotExist()
    }

    @Test
    fun theProjectCanBeKeptAsATemplate() {
        show()
        compose.onNodeWithTag(ProjectSetupTags.SAVE_TEMPLATE).performScrollTo().performClick()
        assertEquals(1, templateSaves)
    }

    /**
     * A project with no road, no steps, no papers and no date kinds has no
     * shape to keep, and the control that would make a template of nothing is
     * not drawn. The lead alone does not count: every project has one whether
     * or not anybody chose it.
     */
    @Test
    fun aProjectWithNoShapeIsNotOfferedATemplate() {
        show(
            stages = emptyList(),
            steps = emptyList(),
            papers = emptyList(),
            dateKinds = emptyList(),
        )
        compose.onNodeWithTag(ProjectSetupTags.SAVE_TEMPLATE).assertDoesNotExist()
    }

    /** Once it is kept, the screen says where it went rather than offering again. */
    @Test
    fun onceKeptTheScreenSaysWhereItWent() {
        show(savedAsTemplate = true)
        compose.onNodeWithTag(ProjectSetupTags.SAVED_TEMPLATE).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag(ProjectSetupTags.SAVE_TEMPLATE).assertDoesNotExist()
    }

    /**
     * The template block is headed in both of its states.
     *
     * Without a heading it was a button floating directly under the waiting-on
     * field, where it read as that field's save control, and its saved state
     * was one gray sentence alone at the foot of the screen. Rule 15: the two
     * blocks above it are named, and so is this one.
     */
    @Test
    fun theTemplateBlockIsHeadedWhetherOrNotItHasBeenSaved() {
        show()
        compose.onNodeWithText(strings["project.setup.template"]).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun theHeadingSurvivesTheSavedState() {
        show(savedAsTemplate = true)
        compose.onNodeWithText(strings["project.setup.template"]).performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * The label describes what is actually saved.
     *
     * It said "these steps" from the day it was written, and a template has
     * carried the lead, the stages, the steps, the papers and the date kinds
     * since #262. A label naming one of the five is the app describing itself
     * wrongly on the one screen whose whole job is saying what a template is.
     */
    @Test
    fun theLabelDoesNotPromiseOnlyTheSteps() {
        show()
        compose.onNodeWithText(strings["projects.save_as_template"]).performScrollTo()
            .assertIsDisplayed()
        assertEquals("Save this setup as your own template", strings["projects.save_as_template"])
    }
}
