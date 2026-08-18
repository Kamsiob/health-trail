package com.kamsiob.healthtrail.ui

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kamsiob.healthtrail.MainActivity
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.v4.Destination
import com.kamsiob.healthtrail.ui.v4.NavTags
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CaptureFormTags
import com.kamsiob.healthtrail.ui.screens.CaptureTags
import com.kamsiob.healthtrail.ui.screens.DisclaimerTags
import com.kamsiob.healthtrail.ui.screens.NotebookTags
import com.kamsiob.healthtrail.ui.screens.SectionTags
import com.kamsiob.healthtrail.ui.screens.SetupTags
import com.kamsiob.healthtrail.ui.screens.SituationPickerTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The system back button, walked through the real activity.
 *
 * **This is the test that did not exist, and its absence cost the back button
 * on every screen above the notebook.** Opening a section, a project, About,
 * the export screen, the capture sheet, or any of the eighteen overlays and
 * pressing back **left the app entirely**, dropping someone standing in a
 * hospital corridor onto their home screen with a half read section behind
 * them. It shipped, and it was found by a hand holding the phone.
 *
 * **Why 130 of the 137 interface tests could not have caught it.** They use
 * `createComposeRule`, which composes one screen inside a bare test activity.
 * That is a fine way to prove a screen renders, and it is structurally unable
 * to prove anything about back, because there is no navigation state to return
 * to and no activity to leave. Every `BackHandler` in this app lives in
 * `NotebookShell`, above the individual screens, so composing a screen on its
 * own composes the one part of the app that has no opinion about back.
 *
 * **The shortcut those tests take is the shape D61 names**: a test that reaches
 * past the person's path proves something the person does not get. There the
 * shortcut was restoring an export onto the device that wrote it. Here it is
 * mounting a screen without the shell that owns its back behavior.
 *
 * So this one launches `MainActivity`, walks in from wherever the install
 * happens to be, and presses the real system back button through Espresso.
 * `pressBack` throws `NoActivityResumedException` when the press leaves the
 * app, which is the failure being tested for, so it is an assertion rather
 * than a hazard.
 *
 * `TESTING-PERSONAS.md` section 9 carries the rule this test is the first
 * instance of.
 */
@RunWith(AndroidJUnit4::class)
class BackJourneyTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /**
     * Walks in from whatever state the install is in.
     *
     * The suite uninstalls the app when it finishes, so a run may start at the
     * disclaimer gate, and a second test in the same run starts past it. Both
     * are legitimate and neither is the thing under test, so each step is taken
     * only if it is actually on screen.
     */
    private fun reachTheNotebook() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag(AppRootTags.LOADING).fetchSemanticsNodes().isEmpty()
        }

        if (showing(DisclaimerTags.ROOT)) {
            compose.onNodeWithTag(DisclaimerTags.ACCEPT).performClick()
            settle()
        }
        if (showing(SetupTags.ROOT)) {
            // Skipping is a real answer, and it is the fastest way to a
            // notebook. P1 walks the same path.
            compose.onNodeWithTag(SetupTags.SKIP).performClick()
            settle()
        }
        if (showing(SituationPickerTags.ROOT)) {
            compose.onNodeWithTag(SituationPickerTags.SKIP).performClick()
            settle()
        }

        compose.waitUntil(timeoutMillis = 20_000) { showing(ShellTags.ROOT) }
    }

    private fun showing(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /** Waits for whatever the last action started, including database work. */
    private fun settle() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag(AppRootTags.LOADING).fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
    }

    private fun openTheNotebook() {
        compose.onNodeWithTag(NavTags.tab(Destination.NOTEBOOK)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(NotebookTags.ROOT) }
    }

    /**
     * Presses the system back button and fails with a readable message if that
     * left the app.
     *
     * Espresso raises `NoActivityResumedException` when a back press finishes
     * the last activity. That exception is precisely the defect, so it is
     * caught and rewritten into a sentence naming what a person would have
     * experienced.
     */
    private fun backWithoutLeavingTheApp(from: String) {
        try {
            Espresso.pressBack()
        } catch (_: androidx.test.espresso.NoActivityResumedException) {
            throw AssertionError(
                "pressing back from $from left the app entirely. " +
                    "Back should return to where the person came from. " +
                    "Add a BackHandler in NotebookShell for this state.",
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun backFromAnOpenSectionReturnsToTheNotebookRatherThanLeavingTheApp() {
        reachTheNotebook()
        openTheNotebook()

        // The trail, because it is the section every notebook has and it is the
        // app's namesake. Scrolled to by key, never by viewport, per the trap
        // recorded in HANDOFF section 8.
        val section = Repository.Section.TRAIL
        // **Scrolls to the tile rather than asking a list for a key.** The
        // notebook became a plain scrolling column of tiles on 2026-08-03, so
        // there is no lazy list to ask, and every tile is in the tree.
        compose.onNodeWithTag(NotebookTags.section(section)).performScrollTo()
        compose.onNodeWithTag(NotebookTags.section(section)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(SectionTags.BACK) }

        backWithoutLeavingTheApp("an open section")

        compose.onNodeWithTag(NotebookTags.ROOT).assertIsDisplayed()
        assertTrue(
            "back from a section left the section open",
            compose.onAllNodesWithTag(SectionTags.BACK).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun backFromTheCaptureSheetClosesTheSheetRatherThanLeavingTheApp() {
        reachTheNotebook()

        compose.onNodeWithTag(NavTags.CAPTURE).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureTags.SHEET) }

        backWithoutLeavingTheApp("the capture sheet")

        assertTrue(
            "back left the capture sheet open",
            compose.onAllNodesWithTag(CaptureTags.SHEET).fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithTag(ShellTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun backFromACaptureFormReturnsToTheSheetRatherThanLeavingTheApp() {
        reachTheNotebook()

        compose.onNodeWithTag(NavTags.CAPTURE).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureTags.SHEET) }
        compose.onNodeWithTag(CaptureTags.option(CaptureKind.CALL)).performClick()
        compose.waitForIdle()

        // Two presses, because a form opened from the sheet is two states deep.
        // The person's expectation is that each press undoes one thing, which
        // is the whole reason the shell holds them as separate handlers.
        backWithoutLeavingTheApp("a capture form")
        backWithoutLeavingTheApp("the capture sheet under a form")

        compose.onNodeWithTag(ShellTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun aHalfWrittenNoteSurvivesLeavingTheFormAndIsDiscardedOnlyByCancel() {
        // **The worst thing this app could do short of losing the notebook.**
        // Somebody in a corridor writing down what the nurse just said, who
        // presses back for any reason, must not lose it. Until the draft was
        // hoisted out of the form it was gone: a back press, a rotation, or the
        // system reclaiming memory took the lot.
        reachTheNotebook()

        val words = "Nurse Ana, ward 4"

        compose.onNodeWithTag(NavTags.CAPTURE).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureTags.SHEET) }
        compose.onNodeWithTag(CaptureTags.option(CaptureKind.CALL)).performClick()
        compose.waitForIdle()
        // **The note rather than the who field**, which is what this test is
        // named for and is now stage one of the staged capture. Using the who
        // field meant walking two stages to prove something about leaving the
        // form, which is two taps of noise around the assertion.
        // **A call opens on who it was with, since #379**, so the note is one
        // stage in rather than the first thing shown. The owner's words: "it
        // should start off with who did you speak to and then notes about the
        // call". One tap of Next is the whole difference, and the assertion
        // this test exists for is unchanged.
        compose.onNodeWithTag(CaptureFormTags.NEXT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(CaptureFormTags.NOTE).performTextInput(words)
        compose.waitForIdle()

        // **Out of the form and straight back in through the chooser.** Back
        // from a form reopens the chooser, D65, so the way back in is the
        // choice itself rather than closing the chooser and pressing the
        // button again. **That middle step is what this test used to do and it
        // is not what it is about**: the assertion is that the words survive
        // leaving the form, and going out through the chooser and in again
        // proves exactly that with nothing else in the way.
        // **The keyboard eats the first back press.** Typing into the note
        // leaves the IME up, and a back press with the IME up dismisses the
        // keyboard rather than the form, so the wait below was watching for a
        // chooser while the form was still on screen. The modal sheet this
        // replaced was its own window and took the keyboard down with it.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()

        backWithoutLeavingTheApp("a capture form")
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureTags.SHEET) }
        compose.onNodeWithTag(CaptureTags.option(CaptureKind.CALL)).performClick()
        compose.waitForIdle()

        // Back on stage one, which is where a reopened form starts: the stage
        // is not remembered, deliberately, because reopening somebody on stage
        // three would be the app deciding where they are in a sentence. **For
        // a call stage one is who it was with**, #379, so the note is one tap
        // along, and that it survived the trip is what this test is about.
        compose.onNodeWithTag(CaptureFormTags.NEXT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(CaptureFormTags.NOTE).assertTextContains(words, substring = true)

        // **Cancel is the one thing that does discard it**, because cancel
        // means abandoning the entry rather than stepping back a level.
        compose.onNodeWithTag(CaptureFormTags.CANCEL).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(NavTags.CAPTURE).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureTags.SHEET) }
        compose.onNodeWithTag(CaptureTags.option(CaptureKind.CALL)).performClick()
        compose.waitForIdle()

        assertTrue(
            "cancel left the note behind, so abandoning an entry does not abandon it",
            compose.onAllNodesWithText(words, substring = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun backFromAnotherDestinationReturnsToTheNotebookBeforeLeaving() {
        // **Found by walking search on the phone.** Back from More left the app
        // outright, from four taps deep, with no chance to glance at anything
        // else first. Android walks up to the start destination before it
        // exits, and every other app on this person's phone behaves that way,
        // so leaving from any tab reads as being dropped.
        reachTheNotebook()

        compose.onNodeWithTag(NavTags.tab(Destination.MORE)).performClick()
        compose.waitForIdle()

        backWithoutLeavingTheApp("the More destination")

        compose.onNodeWithTag(NotebookTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun backFromTheNotebookItselfDoesLeaveTheApp() {
        // The other half of the contract, and the reason this is not fixed by
        // swallowing every back press. At the notebook there is nowhere further
        // back to go, so back does what it does everywhere else on Android.
        reachTheNotebook()
        openTheNotebook()

        // **Everything above the destination is closed first, deliberately.**
        // This test reads whatever state the classes before it left in the one
        // shared database, and `reachTheNotebook` clicks through the gate,
        // setup and the picker only if each is showing, so the number of
        // screens on the stack differed with what ran first. **That is why it
        // failed twice in a full suite and never alone**: it was asserting
        // about a back stack it had not established. #302.
        //
        // Pressing back until the notebook is the only thing left makes the
        // premise true rather than hoping it is, and the loop is bounded so a
        // screen that refuses to close fails here rather than hanging.
        repeat(6) {
            if (!showing(SectionTags.BACK)) return@repeat
            Espresso.pressBack()
            compose.waitForIdle()
        }
        compose.onNodeWithTag(NotebookTags.ROOT).assertIsDisplayed()

        // Unconditionally, because the ordinary `pressBack` raises when the app
        // exits and here the exit is the expected result.
        Espresso.pressBackUnconditionally()

        // **Waited for rather than read once, and that is the rest of #302.**
        // `pressBackUnconditionally` returns as soon as the event is sent, and
        // the activity reaches `DESTROYED` on the main thread some frames
        // later. Reading the state on the very next line is a race, and it is
        // one a busy phone loses: the assertion then says the app trapped the
        // person, which is the most alarming sentence this suite can print and
        // was not true either time it printed it.
        //
        // Bounded, so an app that genuinely does not leave fails here with the
        // same message rather than hanging.
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (compose.activityRule.scenario.state != Lifecycle.State.DESTROYED &&
            SystemClock.uptimeMillis() < deadline
        ) {
            SystemClock.sleep(50)
        }

        // Asked of the scenario rather than of the activity. `compose.activity`
        // reaches into a destroyed activity and throws a null pointer, which
        // reads as a broken test rather than as the pass it is.
        assertTrue(
            "back at the notebook did not leave the app, so it is trapping the person",
            compose.activityRule.scenario.state == Lifecycle.State.DESTROYED,
        )
    }
}
