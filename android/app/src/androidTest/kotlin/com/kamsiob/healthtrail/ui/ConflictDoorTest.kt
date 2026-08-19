package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.MoreScreen
import com.kamsiob.healthtrail.ui.screens.MoreTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import org.junit.Rule
import org.junit.Test

/**
 * The door to what a merge decided appears when there is something behind it.
 *
 * **#402, and this test exists because the diagnosis had nowhere to stand.**
 * Six resolutions were recorded on a real phone and the door never appeared.
 * `MergeApplyTest` proves the repository writes them and can count them, so the
 * fault is on this side of the line, and this is the assertion that says which
 * half of *this* side: does the screen draw the door when it is handed a count.
 *
 * **It is also the test that should have existed.** `contract/DATA-CONTRACT.md`
 * 8.3 requires every resolution to reach "a conflict log the person can
 * actually open and read", and nothing held the reading half.
 */
class ConflictDoorTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun ComposeContentTestRule.show(content: @Composable () -> Unit) {
        val strings = Strings.load(context)
        setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) { content() }
            }
        }
    }

    private fun show(conflicts: Int) {
        compose.show {
            MoreScreen(
                choice = ThemeChoice.FOLLOW_SYSTEM,
                onChoose = {},
                onAbout = {},
                onSearch = {},
                onLibrary = {},
                onSituation = {},
                onSubject = {},
                onPeople = {},
                onExport = {},
                onRestore = {},
                onConflicts = {},
                conflicts = conflicts,
            )
        }
    }

    /** **Nothing behind it, no row.** A permanent "nothing to look at" teaches
     * somebody to ignore a row that will one day matter. */
    @Test
    fun theDoorIsAbsentWhenNothingHasBeenResolved() {
        show(conflicts = 0)
        compose.onNodeWithTag(MoreTags.CONFLICTS).assertDoesNotExist()
    }

    /** And present the moment there is. */
    @Test
    fun theDoorAppearsWhenAMergeHasDecidedSomething() {
        show(conflicts = 1)
        compose.onNodeWithTag(MoreTags.CONFLICTS).assertExists()
    }
}
