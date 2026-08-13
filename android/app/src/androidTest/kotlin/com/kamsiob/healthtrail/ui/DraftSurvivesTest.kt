package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AddDocTags
import com.kamsiob.healthtrail.ui.screens.AddDocumentScreen
import com.kamsiob.healthtrail.ui.screens.SetupScreen
import com.kamsiob.healthtrail.ui.screens.SetupTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A half filled form is still there after the system kills the app. #371 item 7.
 *
 * **The audit found that only the capture form's draft survived process
 * death.** Ten other forms held everything in a plain `remember`, which the
 * phone discards when it reclaims the process, and Android reclaims a
 * backgrounded app routinely rather than exceptionally. Somebody photographing
 * a discharge letter in a corridor, answering a call, and coming back is the
 * ordinary case, not the edge.
 *
 * **`AddDocumentScreen` was the worst of them**, because its stage number was
 * saved and its answers were not: the form came back insisting it was on
 * question three of a form with nothing in it, which is worse than coming back
 * empty. It looks like the app kept the work and lost it silently.
 *
 * **Verified by emulating the restore rather than by reading the code**, which
 * is what [StateRestorationTester] is for: it saves the whole state tree,
 * throws the composition away, and rebuilds it the way the system does.
 *
 * **The export and restore passphrases are deliberately not covered here and
 * are deliberately not saved.** A secret that opens an archive does not go into
 * a bundle. `DraftSavers.kt` says so where somebody would look for it.
 */
@RunWith(AndroidJUnit4::class)
class DraftSurvivesTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun restorer(content: @androidx.compose.runtime.Composable () -> Unit) =
        StateRestorationTester(compose).apply {
            setContent {
                CompositionLocalProvider(LocalStrings provides strings) {
                    HealthTrailTheme { content() }
                }
            }
        }

    /**
     * The document form comes back with its answers and its place in the form
     * agreeing with each other.
     */
    @Test
    fun adocumentHalfDescribedIsStillThereAfterTheProcessDies() {
        val restoration = restorer {
            AddDocumentScreen(
                onSave = {},
                onCancel = {},
                existing = null,
                error = null,
                folders = listOf("Insurance"),
            )
        }

        compose.onNodeWithText(strings["capture.skip"]).performClick()
        compose.onNodeWithTag(AddDocTags.field("title")).performTextInput("Discharge summary")
        compose.onNodeWithText("Discharge summary").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Discharge summary").assertIsDisplayed()
    }

    /** And setup, which is the first thing anybody types into this app. */
    @Test
    fun thenameTypedIntoSetupIsStillThereAfterTheProcessDies() {
        val restoration = restorer { SetupScreen(onContinue = {}, onSkip = {}) }

        compose.onNodeWithTag(SetupTags.NAME).performTextInput("Margaret")
        compose.onNodeWithText("Margaret").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Margaret").assertIsDisplayed()
    }
}
