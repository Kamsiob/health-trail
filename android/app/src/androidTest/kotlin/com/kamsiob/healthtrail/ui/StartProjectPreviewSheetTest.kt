package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.StartPreviewTags
import com.kamsiob.healthtrail.ui.screens.StartProjectPreviewSheet
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The template system made visible, `DESIGN.md` 20.5 screen 04.
 *
 * **The claim under test is that looking is not agreeing.** The flow this
 * replaced created the project, its road, its steps, its papers and its date
 * chips on one tap of a row, so the first time anybody saw what a template
 * meant was on a project that already existed.
 */
@RunWith(AndroidJUnit4::class)
class StartProjectPreviewSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val template = TemplateCatalog.ProjectTemplate(
        id = "medicaid_ltc",
        name = "Medicaid application for long term care",
        subtitle = "Applying for coverage of nursing home care",
        category = "paying",
        stateVariance = true,
        roles = emptyList(),
        steps = listOf("a", "b", "c"),
        lead = "standing",
        stages = listOf("Gathering", "Applied", "In review", "Decision"),
        dateKinds = listOf("Response window"),
        papers = listOf("The application copy", "Their letters"),
    )

    private var created: String? = null
    private var dismissed = 0

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    StartProjectPreviewSheet(
                        template = template,
                        onCreate = { created = it },
                        onDismiss = { dismissed++ },
                    )
                }
            }
        }
    }

    /**
     * Every one of the five defaults is on the sheet before anything exists.
     *
     * The road is drawn rather than counted, so it is asserted by its tag; the
     * other three are counts of what will be created.
     */
    @Test
    fun theSheetShowsWhatTheTemplateWillSetUp() {
        show()

        compose.onNodeWithTag(StartPreviewTags.ROAD).assertIsDisplayed()
        compose.onNodeWithText(strings("project.preview.steps", "count" to 3))
            .assertIsDisplayed()
        compose.onNodeWithText(strings("project.preview.papers", "count" to 2))
            .assertIsDisplayed()
        compose.onNodeWithTag(StartPreviewTags.KINDS).assertIsDisplayed()
        // The shape is the one default nothing else here shows, and it decides
        // what the person sees first every time they open the project.
        compose.onNodeWithText(
            strings(
                "project.preview.lead",
                "answer" to strings["today.card.project_standing.long"],
            ),
        ).assertIsDisplayed()
    }

    /**
     * The name arrives filled in from the template and what the person types
     * instead is what gets created.
     *
     * **Rule 20.** A project called something other than what they typed is the
     * app deciding it knows better, and it is the kind of thing nobody notices
     * until they are looking for it in a list of five months later.
     */
    @Test
    fun theNameIsPreFilledAndWhatTheyTypeIsWhatIsCreated() {
        show()

        compose.onNodeWithText(template.name).assertIsDisplayed()

        compose.onNodeWithTag(StartPreviewTags.NAME).performTextClearance()
        compose.onNodeWithTag(StartPreviewTags.NAME).performTextInput("Mom's Medicaid")
        // The sheet scrolls, and on a shorter phone its action starts below the
        // fold. A click at a node's centre outside the viewport does nothing.
        compose.onNodeWithTag(StartPreviewTags.CREATE).performScrollTo()
        compose.onNodeWithTag(StartPreviewTags.CREATE).performClick()

        assertEquals("Mom's Medicaid", created)
    }

    /**
     * A project has to be called something.
     *
     * **This is the one thing on the sheet that can stop it**, and it is only
     * reachable by somebody deliberately emptying a field the template already
     * filled. Rule 13 keeps everything else optional.
     */
    @Test
    fun anEmptyNameIsTheOneThingThatStopsIt() {
        show()

        compose.onNodeWithTag(StartPreviewTags.NAME).performTextClearance()
        compose.onNodeWithTag(StartPreviewTags.CREATE).assertIsNotEnabled()
        assertEquals(null, created)
    }
}
