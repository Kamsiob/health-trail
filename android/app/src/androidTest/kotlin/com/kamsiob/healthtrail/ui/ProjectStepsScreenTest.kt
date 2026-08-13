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
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ProjectStepsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectStepsTags
import com.kamsiob.healthtrail.ui.screens.StepEditSheet
import com.kamsiob.healthtrail.ui.screens.StepEditTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Changing the starting steps. `DESIGN.md` 20.5, screen 18, and law 5.
 *
 * **What this holds is that the promise is reachable.** Adding, editing,
 * reordering and removing a step have been in the repository the whole time
 * with nothing calling them since the detail screen was superseded, and a
 * screen that says everything is changeable while offering no way to change it
 * is the promise without the thing.
 */
@RunWith(AndroidJUnit4::class)
class ProjectStepsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun step(id: String, text: String, note: String? = null, cluster: String? = null) =
        Repository.ProjectStep(id, text, null, note, cluster, null)

    private val steps = listOf(
        step("s1", "Call and ask what form it is"),
        step("s2", "Get the form", note = "They will mail it"),
        step("s3", "Send it certified"),
    )

    private var added: String? = null
    private var opened: Repository.ProjectStep? = null

    private fun showList(list: List<Repository.ProjectStep> = steps) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectStepsScreen(
                        projectName = "Medicaid application",
                        steps = list,
                        onAdd = { added = it },
                        onOpen = { opened = it },
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun aStepCanBeAddedAndTheFieldIsTheOnlyThingThatStopsIt() {
        showList()

        compose.onNodeWithTag(ProjectStepsTags.ADD).assertIsNotEnabled()
        compose.onNodeWithTag(ProjectStepsTags.ADD_FIELD).performTextInput("Ask for it in writing")
        // **The keyboard is put away before the button is tapped**, and that
        // is the screen being right rather than the test being helped. Since
        // #371 item 6 a section screen makes room for the keyboard, so with it
        // up the list is short and a control below the field is genuinely off
        // screen. Tapping where it used to be is a tap on the keyboard.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag(ProjectStepsTags.ADD).performScrollTo().performClick()

        assertEquals("Ask for it in writing", added)
    }

    /**
     * The note is on the row rather than behind the sheet.
     *
     * "The woman on the phone said to call back after the 15th" is the whole
     * reason these processes are survivable, and a note nobody can see without
     * tapping is a note nobody reads.
     */
    @Test
    fun aStepShowsWhatIsWrittenUnderIt() {
        showList()
        compose.onNodeWithText(Bidi.isolate("They will mail it")).assertIsDisplayed()
    }

    @Test
    fun tappingAStepOpensThatStepAndNotAnother() {
        showList()
        compose.onNodeWithTag(ProjectStepsTags.step("s3")).performClick()
        assertEquals("s3", opened?.id)
    }

    /**
     * Steps carrying an area are grouped here the same way the project groups
     * them, because two orders for one set of steps is two mental models for
     * one thing.
     */
    @Test
    fun theListGroupsByAreaTheSameWayTheProjectDoes() {
        showList(
            listOf(
                step("c1", "Call them", cluster = "The phone calls"),
                step("c2", "Get the form", cluster = "The paperwork"),
                step("c3", "Send it certified", cluster = "The paperwork"),
            ),
        )
        compose.onNodeWithText(Bidi.join("THE PAPERWORK", "2")).assertIsDisplayed()
        compose.onNodeWithText(Bidi.join("THE PHONE CALLS", "1")).assertIsDisplayed()
    }

    // The sheet, which is where everything that can be done to a step lives.

    private var saved: Pair<String, String?>? = null
    private var moved: Boolean? = null
    private var removed = 0

    private fun showSheet(
        target: Repository.ProjectStep,
        canMoveEarlier: Boolean = true,
        canMoveLater: Boolean = true,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    StepEditSheet(
                        step = target,
                        canMoveEarlier = canMoveEarlier,
                        canMoveLater = canMoveLater,
                        onSave = { text, note -> saved = text to note },
                        onMove = { moved = it },
                        onRemove = { removed++ },
                        onDismiss = {},
                    )
                }
            }
        }
    }

    @Test
    fun theSheetSavesTheTextAndTheNoteSeparately() {
        showSheet(steps[1])

        compose.onNodeWithTag(StepEditTags.NOTE).performTextClearance()
        compose.onNodeWithTag(StepEditTags.NOTE).performTextInput("Called Mar 3")
        compose.onNodeWithTag(StepEditTags.SAVE).performClick()

        // **The text is untouched by writing a note.** Typing into the wrong
        // field on the phone concatenated the two, which is what this holds
        // against: they are separate fields and they stay separate.
        assertEquals("Get the form" to "Called Mar 3", saved)
    }

    @Test
    fun anEmptyStepCannotBeSaved() {
        showSheet(steps[0])
        compose.onNodeWithTag(StepEditTags.TEXT).performTextClearance()
        compose.onNodeWithTag(StepEditTags.SAVE).assertIsNotEnabled()
    }

    /**
     * A control that would do nothing is not offered.
     *
     * Section 5.14: everything the person touches responds, and the honest way
     * to keep that true for the first step in a list is to not draw a control
     * that moves it earlier.
     */
    @Test
    fun theFirstStepIsNotOfferedAWayToMoveEarlier() {
        showSheet(steps[0], canMoveEarlier = false, canMoveLater = true)
        compose.onNodeWithTag(StepEditTags.EARLIER).assertDoesNotExist()
        compose.onNodeWithTag(StepEditTags.LATER).assertIsDisplayed().performClick()
        assertTrue("moving later did not report later", moved == false)
    }

    @Test
    fun removingIsOfferedPlainlyAndReportsOnce() {
        showSheet(steps[1])
        compose.onNodeWithTag(StepEditTags.REMOVE).performClick()
        assertEquals(1, removed)
    }
}
