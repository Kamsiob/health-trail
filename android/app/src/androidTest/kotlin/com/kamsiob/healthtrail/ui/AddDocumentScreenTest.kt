package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AddDocumentScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Saving a document, which asks three questions rather than stacking five
 * fields. #361, and law 3 applied to a form that predates the law.
 *
 * **What is worth a test here is the shape rather than the words.** A stage
 * that cannot be left, a save that only works at the end, or a correction that
 * hides half of what is written would each be invisible in a screenshot of one
 * screen and obvious to somebody using it.
 *
 * **The picked photograph is not walked from here and cannot be.** Choosing one
 * opens the system picker, which on this phone is the owner's real photo
 * library, per `docs/TRAPS.md` section 1. What that path renders is the empty
 * sheet's other half, and it is checked by eye when a document is saved by hand.
 */
@RunWith(AndroidJUnit4::class)
class AddDocumentScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val saved = Repository.Document(
        id = "d1",
        title = "Discharge summary",
        category = "Hospital and doctors",
        originalLocation = "The blue folder",
        notes = "Ask about the follow up",
        receivedEdtf = null,
        sha256 = null,
        byteSize = null,
    )

    private fun show(
        existing: Repository.Document? = null,
        error: String? = null,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        compose.setContent {
            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalLayoutDirection provides direction,
            ) {
                HealthTrailTheme {
                    AddDocumentScreen(
                        onSave = {},
                        onCancel = {},
                        existing = existing,
                        error = error,
                        folders = listOf("Insurance"),
                    )
                }
            }
        }
    }

    @Test
    fun itOpensOnTheSheetAndNothingElse() {
        show()
        compose.onNodeWithContentDescription(strings["docs.pick"]).assertIsDisplayed()
        compose.onNodeWithContentDescription("Step 1 of 3").assertIsDisplayed()
        // The questions behind it are questions, not a wall of fields.
        compose.onNodeWithText(strings["docs.title"]).assertDoesNotExist()
        compose.onNodeWithText(strings["docs.original"]).assertDoesNotExist()
    }

    /**
     * **Save is live from the first question**, which is the fifteen second path
     * law 3 is written around: photograph a letter, save, walk on.
     */
    @Test
    fun theSaveActionIsThereBeforeAnyQuestionIsAnswered() {
        show()
        compose.onNodeWithText(strings["capture.save"]).assertIsDisplayed()
    }

    @Test
    fun theWayOnLeadsThroughAllThreeQuestions() {
        show()
        compose.onNodeWithText(strings["capture.skip"]).performClick()
        compose.onNodeWithText(strings["docs.title"]).assertIsDisplayed()
        compose.onNodeWithContentDescription("Step 2 of 3").assertIsDisplayed()

        compose.onNodeWithText(strings["capture.skip"]).performClick()
        compose.onNodeWithText(strings["docs.original"]).assertIsDisplayed()
        compose.onNodeWithContentDescription("Step 3 of 3").assertIsDisplayed()

        // The last question has no way on, only a way back.
        compose.onNodeWithText(strings["capture.skip"]).assertDoesNotExist()
        compose.onNodeWithText(strings["capture.back"]).assertIsDisplayed()
    }

    /**
     * **The folder and the note are behind "Add more"**, and nothing there is
     * ever required. `Disclosure` says so once in the aside.
     */
    @Test
    fun theFolderAndTheNoteWaitBehindAddMore() {
        show()
        compose.onNodeWithText(strings["capture.skip"]).performClick()
        compose.onNodeWithText(strings["capture.skip"]).performClick()

        compose.onNodeWithText(strings["docs.folder"]).assertDoesNotExist()
        compose.onNodeWithText(strings["capture.more"]).performClick()
        compose.onNodeWithText(strings["docs.folder"]).assertIsDisplayed()
    }

    /**
     * **A refusal is readable from wherever somebody was standing.** Save works
     * from any question, so a message pinned inside one of them would be
     * invisible to two thirds of the people who could see the refusal.
     */
    @Test
    fun aRefusedFileIsSaidOnEveryQuestion() {
        show(error = strings["docs.too_large"])
        compose.onNodeWithText(strings["docs.too_large"]).assertIsDisplayed()

        compose.onNodeWithText(strings["capture.skip"]).performClick()
        compose.onNodeWithText(strings["docs.too_large"]).assertIsDisplayed()
    }

    /**
     * **Correcting is not a conversation.** Somebody who opens a saved document
     * knows which line is wrong, and three questions between them and it would
     * be the form starting something they came to end. D146.
     */
    @Test
    fun correctingASavedDocumentShowsEverythingAtOnce() {
        show(existing = saved)

        // **Scrolled to rather than asserted where they land.** A correction is
        // the whole record at once, which is longer than a phone, and a node
        // below the fold is present and reachable rather than absent.
        compose.onNodeWithText(strings["docs.title"]).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(strings["docs.original"]).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(strings["docs.received"]).performScrollTo().assertIsDisplayed()
        // No stages, so no way on and no way back.
        compose.onNodeWithText(strings["capture.skip"]).assertDoesNotExist()
        compose.onNodeWithText(strings["capture.back"]).assertDoesNotExist()
    }

    /**
     * **A disclosure never hides what is already written.** Folding a note
     * somebody typed last week behind a control that offers to add more would
     * be the app hiding their own words from them.
     */
    @Test
    fun aCorrectionOpensTheDisclosureOverWhatIsAlreadyThere() {
        show(existing = saved)
        compose.onNodeWithText(strings["docs.folder"]).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ask about the follow up").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(strings["capture.more"]).assertDoesNotExist()
    }

    /**
     * **Right to left is verified against a forced layout direction**, per D141
     * and rule 24, rather than against Arabic content. What is at risk in a
     * mirrored layout is the structure: the sheet, the dots and the way on all
     * still compose and are all still reachable.
     */
    @Test
    fun itComposesRightToLeft() {
        show(direction = LayoutDirection.Rtl)
        compose.onNodeWithContentDescription(strings["docs.pick"]).assertIsDisplayed()
        compose.onNodeWithContentDescription("Step 1 of 3").assertIsDisplayed()

        compose.onNodeWithText(strings["capture.skip"]).performClick()
        compose.onNodeWithText(strings["docs.title"]).assertIsDisplayed()
    }
}
