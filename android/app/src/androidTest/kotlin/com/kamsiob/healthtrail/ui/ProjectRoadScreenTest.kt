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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ProjectRoadScreen
import com.kamsiob.healthtrail.ui.screens.ProjectRoadTags
import com.kamsiob.healthtrail.ui.screens.StageEditSheet
import com.kamsiob.healthtrail.ui.screens.StageEditTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Changing the road. `DESIGN.md` 20.5 screen 18, and law 5.
 *
 * The data half is `RoadEditTest`. This holds the part a person touches: that
 * the road drawn is the road being edited, and that a control which would do
 * nothing is not offered.
 */
@RunWith(AndroidJUnit4::class)
class ProjectRoadScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun stage(id: String, name: String, reached: Boolean, index: Int) =
        Repository.ProjectStage(id, name, index, if (reached) "2026-03-04" else null, null)

    private val stages = listOf(
        stage("g1", "Applied", true, 0),
        stage("g2", "In review", true, 1),
        stage("g3", "Decision", false, 2),
    )

    private var added: String? = null
    private var opened: Repository.ProjectStage? = null

    private fun showList(list: List<Repository.ProjectStage> = stages) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectRoadScreen(
                        projectName = "Medicaid application",
                        stages = list,
                        onAdd = { added = it },
                        onOpen = { opened = it },
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun theRoadItEditsIsTheRoadItDraws() {
        showList()
        compose.onNodeWithTag(ProjectRoadTags.STRIP).assertIsDisplayed()
        compose.onNodeWithTag(ProjectRoadTags.stage("g2")).assertIsDisplayed()
    }

    /**
     * One waypoint on a dashed line says nothing, which is what `RoadStrip`
     * itself requires. A project down to a single stage draws the list alone
     * rather than crashing or drawing half a road.
     */
    @Test
    fun oneStageIsAListAndNotARoad() {
        showList(listOf(stage("only", "Applied", true, 0)))
        compose.onNodeWithTag(ProjectRoadTags.STRIP).assertDoesNotExist()
        compose.onNodeWithTag(ProjectRoadTags.stage("only")).assertIsDisplayed()
    }

    @Test
    fun aStageCanBeAddedAndTheFieldIsTheOnlyThingThatStopsIt() {
        showList()
        compose.onNodeWithTag(ProjectRoadTags.ADD).assertIsNotEnabled()
        compose.onNodeWithTag(ProjectRoadTags.ADD_FIELD).performTextInput("Renewal due")
        // **The keyboard is put away before the button is tapped**, and that
        // is the screen being right rather than the test being helped. Since
        // #371 item 6 a section screen makes room for the keyboard, so with it
        // up the list is short and a control below the field is genuinely off
        // screen. Tapping where it used to be is a tap on the keyboard.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag(ProjectRoadTags.ADD).performScrollTo().performClick()
        assertEquals("Renewal due", added)
    }

    @Test
    fun tappingAStageOpensThatStage() {
        showList()
        compose.onNodeWithTag(ProjectRoadTags.stage("g3")).performClick()
        assertEquals("g3", opened?.id)
    }

    private var saved: String? = null
    private var moved: Boolean? = null
    private var removed = 0

    private fun showSheet(
        target: Repository.ProjectStage,
        canMoveEarlier: Boolean = true,
        canMoveLater: Boolean = true,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    StageEditSheet(
                        stage = target,
                        canMoveEarlier = canMoveEarlier,
                        canMoveLater = canMoveLater,
                        onSave = { saved = it },
                        onMove = { moved = it },
                        onRemove = { removed++ },
                        onDismiss = {},
                    )
                }
            }
        }
    }

    /**
     * A reached stage says what renaming will and will not do.
     *
     * Leaving somebody to guess whether renaming a stage they have already
     * passed loses the date is the app asking them to understand how it stores
     * things, which is rule 20.
     */
    @Test
    fun aReachedStageSaysThatRenamingKeepsTheArrival() {
        showSheet(stages[1])
        compose.onNodeWithText(strings["project.road.keeps_arrival"]).assertIsDisplayed()
    }

    @Test
    fun anUnreachedStageDoesNotClaimAnArrival() {
        showSheet(stages[2])
        compose.onNodeWithText(strings["project.road.keeps_arrival"]).assertDoesNotExist()
    }

    @Test
    fun theSheetSavesTheNewName() {
        showSheet(stages[1])
        compose.onNodeWithTag(StageEditTags.NAME).performTextClearance()
        compose.onNodeWithTag(StageEditTags.NAME).performTextInput("With the reviewer")
        compose.onNodeWithTag(StageEditTags.SAVE).performClick()
        assertEquals("With the reviewer", saved)
    }

    @Test
    fun anEmptyNameCannotBeSaved() {
        showSheet(stages[0])
        compose.onNodeWithTag(StageEditTags.NAME).performTextClearance()
        compose.onNodeWithTag(StageEditTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun theLastStageIsNotOfferedAWayToMoveLater() {
        showSheet(stages[2], canMoveEarlier = true, canMoveLater = false)
        compose.onNodeWithTag(StageEditTags.LATER).assertDoesNotExist()
        compose.onNodeWithTag(StageEditTags.EARLIER).performClick()
        assertEquals(true, moved)
    }

    @Test
    fun removingIsOfferedPlainlyAndReportsOnce() {
        showSheet(stages[0])
        compose.onNodeWithTag(StageEditTags.REMOVE).performClick()
        assertEquals(1, removed)
    }
}
