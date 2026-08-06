package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.PaperEditSheet
import com.kamsiob.healthtrail.ui.screens.PaperEditTags
import com.kamsiob.healthtrail.ui.screens.ProjectPapersScreen
import com.kamsiob.healthtrail.ui.screens.ProjectPapersTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The named places a project keeps its paper. `DESIGN.md` 20.5 screen 18.
 *
 * **A placeholder is a place, not a paper**, and an empty one is waiting rather
 * than missing. What this holds is that nothing on the screen counts the empty
 * ones or chases them, per rule 13, and that no control here can cost somebody
 * a document.
 */
@RunWith(AndroidJUnit4::class)
class ProjectPapersScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val waiting = Repository.ProjectPaper("p1", "The award letter", 0, null, null)
    private val filed = Repository.ProjectPaper("p2", "Proof of income", 1, "sent", "doc-1")

    private var added: String? = null
    private var opened: Repository.ProjectPaper? = null

    private fun showList(list: List<Repository.ProjectPaper> = listOf(waiting, filed)) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectPapersScreen(
                        projectName = "Medicaid application",
                        papers = list,
                        onAdd = { added = it },
                        onOpen = { opened = it },
                        onBack = {},
                    )
                }
            }
        }
    }

    /**
     * An empty place says it is waiting, and the screen never says how many are
     * not filled.
     *
     * Rule 13 rules out a progress meter on the person's own work, and six
     * placeholders with a count of how many are still empty is exactly that
     * pointed at somebody who is waiting on other people's post.
     */
    @Test
    fun anEmptyPlaceIsWaitingRatherThanMissing() {
        showList()
        compose.onNodeWithText(strings["project.papers.waiting"]).assertIsDisplayed()
        compose.onNodeWithText(strings["project.papers.filed"]).assertIsDisplayed()
    }

    @Test
    fun aPlaceCanBeAddedAndTheFieldIsTheOnlyThingThatStopsIt() {
        showList()
        compose.onNodeWithTag(ProjectPapersTags.ADD).assertIsNotEnabled()
        compose.onNodeWithTag(ProjectPapersTags.ADD_FIELD).performTextInput("The denial letter")
        compose.onNodeWithTag(ProjectPapersTags.ADD).performClick()
        assertEquals("The denial letter", added)
    }

    @Test
    fun tappingAPlaceOpensThatPlace() {
        showList()
        compose.onNodeWithTag(ProjectPapersTags.paper("p2")).performClick()
        assertEquals("p2", opened?.id)
    }

    @Test
    fun noPlacesReadsAsNotYet() {
        showList(emptyList())
        compose.onNodeWithText(strings["project.papers.none"]).assertIsDisplayed()
    }

    private var saved: String? = null
    private var emptied = 0
    private var removed = 0

    private fun showSheet(paper: Repository.ProjectPaper) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    PaperEditSheet(
                        paper = paper,
                        onSave = { saved = it },
                        onEmpty = { emptied++ },
                        onRemove = { removed++ },
                        onDismiss = {},
                    )
                }
            }
        }
    }

    /**
     * A place with nothing in it is not offered a way to take something out.
     *
     * Section 5.14: everything the person touches responds, and the honest way
     * to keep that true is to not draw the control.
     */
    @Test
    fun anEmptyPlaceIsNotOfferedAWayToEmptyIt() {
        showSheet(waiting)
        compose.onNodeWithTag(PaperEditTags.EMPTY).assertDoesNotExist()
        compose.onNodeWithTag(PaperEditTags.REMOVE).assertIsDisplayed()
    }

    @Test
    fun aFilledPlaceCanHaveThePaperTakenOut() {
        showSheet(filed)
        compose.onNodeWithTag(PaperEditTags.EMPTY).performClick()
        assertEquals(1, emptied)
        // Emptying is not removing, and the two are separate controls because
        // taking the wrong document out of the right place is the common
        // mistake and must not require destroying the place.
        assertEquals(0, removed)
    }

    /**
     * The sheet says that none of this touches the person's own documents.
     *
     * Leaving somebody to find that out by trying it is the app asking them to
     * understand how it stores things, which is rule 20, and the thing at risk
     * here is a photograph of a letter they may not be able to get again.
     */
    @Test
    fun theSheetSaysNothingHereTouchesTheDocuments() {
        showSheet(filed)
        compose.onNodeWithText(strings["project.papers.keeps_document"]).assertIsDisplayed()
    }

    @Test
    fun theSheetSavesTheNewName() {
        showSheet(waiting)
        compose.onNodeWithTag(PaperEditTags.NAME).performTextClearance()
        compose.onNodeWithTag(PaperEditTags.NAME).performTextInput("The decision letter")
        compose.onNodeWithTag(PaperEditTags.SAVE).performClick()
        assertEquals("The decision letter", saved)
    }

    @Test
    fun anEmptyNameCannotBeSaved() {
        showSheet(waiting)
        compose.onNodeWithTag(PaperEditTags.NAME).performTextClearance()
        compose.onNodeWithTag(PaperEditTags.SAVE).assertIsNotEnabled()
    }
}
