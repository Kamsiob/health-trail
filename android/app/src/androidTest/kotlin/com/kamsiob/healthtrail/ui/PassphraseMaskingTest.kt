package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ExportScreen
import com.kamsiob.healthtrail.ui.screens.ExportState
import com.kamsiob.healthtrail.ui.screens.ExportTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The export passphrase is hidden as it is typed.
 *
 * **This was a defect found on the device, not a theory.** The field asked for
 * a password keyboard and nothing else, which selects a keyboard layout and
 * conceals nothing, so the app's most consequential secret rendered in full on
 * the one screen where somebody is most likely to be sitting in a waiting room
 * with people behind them.
 *
 * The check is on what the field actually renders rather than on which
 * parameters are passed, because a parameter was already being passed and was
 * already not doing this.
 */
class PassphraseMaskingTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val secret = "correcthorsebattery"

    private fun ComposeContentTestRule.show(content: @Composable () -> Unit) {
        val strings = Strings.load(context)
        setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) { content() }
            }
        }
    }

    @Test
    fun theExportPassphraseIsConcealedUntilThePersonAsksToSeeIt() {
        compose.show {
            ExportScreen(state = ExportState.READY, onExport = {}, onBack = {})
        }

        compose.onNodeWithTag(ExportTags.PASSPHRASE).performTextInput(secret)
        compose.waitForIdle()

        assertFalse(
            "the passphrase is rendered in the clear",
            editableTextOf(ExportTags.PASSPHRASE).contains(secret),
        )

        // And it can be revealed, because typing a passphrase blind, twice,
        // with no way to look is its own trap.
        compose.onNodeWithTag(ExportTags.REVEAL).performClick()
        compose.waitForIdle()

        assertTrue(
            "asking to see the passphrase did not show it",
            editableTextOf(ExportTags.PASSPHRASE).contains(secret),
        )
    }

    @Test
    fun finishingAnExportTakesTheFormAndThePassphraseOffTheScreen() {
        compose.show {
            ExportScreen(state = ExportState.DONE, onExport = {}, onBack = {})
        }
        compose.waitForIdle()

        assertTrue(
            "the passphrase field is still on screen after the export finished",
            compose.onAllNodesWithTag(ExportTags.PASSPHRASE).fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            "the export screen still offers to write another file over the result",
            compose.onAllNodesWithTag(ExportTags.SAVE).fetchSemanticsNodes().isEmpty(),
        )
    }

    /**
     * What the field shows, as the accessibility tree sees it.
     *
     * A masked field keeps the real characters in its edit buffer and publishes
     * the transformed ones, which is exactly the distinction being tested, so
     * this reads the text that is rendered rather than the value held in state.
     */
    private fun SemanticsNodeInteractionsProvider.editableTextOf(tag: String): String {
        val node = onNodeWithTag(tag).fetchSemanticsNode()
        val text = node.config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
        val displayed = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            .orEmpty()
        return "$text $displayed"
    }

    private fun editableTextOf(tag: String): String = compose.editableTextOf(tag)
}
