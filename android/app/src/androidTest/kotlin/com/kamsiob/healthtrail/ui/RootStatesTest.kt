package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two screens above the notebook, which nothing could reach until now.
 *
 * **A state nobody can reach is a state nobody reviews.** The unrecoverable
 * screen shipped saying "That did not work. Nothing was changed.", which is the
 * copy for an action that failed, on the one screen in the app that appears
 * when the notebook cannot be opened at all. It survived because
 * `RootState` and both screens were private: no test composed them, and seeing
 * either on a phone means a factory reset or real damage.
 *
 * **So the seam came first and the fix second**, which is the order #343 now
 * carries. Making them internal costs nothing at runtime and is the difference
 * between a screen that gets looked at and one that does not.
 *
 * These assert the words rather than the layout, because the defect was the
 * words: a screen that renders beautifully and says the wrong sentence passes
 * every check this repository has.
 */
@RunWith(AndroidJUnit4::class)
class RootStatesTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme { content() }
            }
        }
    }

    /**
     * **It says what happened, what it means, and what to do**, in that order.
     *
     * D24 decided all three sentences and none of them were on the screen.
     */
    @Test
    fun theUnopenableNotebookSaysWhatHappenedAndWhatIsLeft() {
        show { UnrecoverableScreen() }

        compose.onNodeWithTag(AppRootTags.UNRECOVERABLE).assertIsDisplayed()
        compose.onNodeWithText(strings["unrecoverable.title"]).assertIsDisplayed()
        compose.onNodeWithText(strings["unrecoverable.body"]).assertIsDisplayed()
        compose.onNodeWithText(strings["unrecoverable.next"]).assertIsDisplayed()
    }

    /**
     * **And it never says the sentence it used to.**
     *
     * Asserting the new words would pass on a screen that showed both, and the
     * old one is the whole defect: it tells somebody there is something else to
     * try when there is not.
     */
    @Test
    fun theUnopenableNotebookNoLongerOffersTheCopyForAFailedAction() {
        show { UnrecoverableScreen() }

        assertFalse(
            "the unrecoverable screen still carries the generic failure copy",
            compose.onAllNodesWithText(strings["common.error.generic"])
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    /**
     * **One word, and no spinner.** The loading screen's own KDoc described a
     * spinner that was never in the code, which is how a component gets built
     * to match a comment. This asserts the word and nothing else, because the
     * word is the whole of it.
     */
    @Test
    fun theOpeningScreenSaysOneQuietWord() {
        show { OpeningScreen() }

        compose.onNodeWithTag(AppRootTags.LOADING).assertIsDisplayed()
        compose.onNodeWithText(strings["common.loading"]).assertIsDisplayed()
    }

    /**
     * **The screen offers the answer rather than an instruction.** #343: it
     * ended by telling somebody to install the app again and restore from
     * their file, which is work the app can do for them. Rule 20.
     *
     * **Reaching this state for real means a factory reset**, which is why the
     * screen is walked here rather than on the phone.
     */
    @Test
    fun theUnrecoverableScreenOffersToRestore() {
        var asked = false
        show { UnrecoverableScreen(onRestore = { asked = true }) }

        compose.onNodeWithTag(AppRootTags.UNRECOVERABLE_RESTORE)
            .assertIsDisplayed()
            .performClick()
        assertTrue("the one action on this screen did nothing", asked)
    }
}
