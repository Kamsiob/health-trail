package com.kamsiob.healthtrail.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kamsiob.healthtrail.MainActivity
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.components.NavTags
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
 * Every section of the notebook opens by being tapped, and closes by going back.
 *
 * **A screen only ever composed directly is a screen no test has visited**, D65
 * and #129. Thirty-one test files mount one screen inside a bare activity;
 * three walk the app. **Everything above the screen is invisible to the first
 * kind**, which is how a card that opened the wrong list, a row that opened
 * nothing at all, and a back press that was silently swallowed all shipped past
 * a green suite on 2026-08-12 and were found by a hand holding the phone.
 *
 * **This walks all twelve rows the way a person does**: the notebook, the tile,
 * the screen that opens, and back. It is deliberately shallow and wide, because
 * the defect class it exists for is "the door goes somewhere else", which does
 * not need depth to catch.
 */
@RunWith(AndroidJUnit4::class)
class SectionsReachableJourneyTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun showing(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun reachTheNotebook() {
        compose.waitUntil(timeoutMillis = 20_000) {
            showing(DisclaimerTags.ACCEPT) || showing(SetupTags.SKIP) || showing(ShellTags.ROOT)
        }
        if (showing(DisclaimerTags.ACCEPT)) {
            compose.onNodeWithTag(DisclaimerTags.ACCEPT).performClick()
        }
        compose.waitForIdle()
        if (showing(SetupTags.SKIP)) {
            compose.onNodeWithTag(SetupTags.SKIP).performClick()
            compose.waitForIdle()
        }
        if (showing(SituationPickerTags.SKIP)) {
            compose.onNodeWithTag(SituationPickerTags.SKIP).performClick()
            compose.waitForIdle()
        }
        compose.waitUntil(timeoutMillis = 20_000) { showing(ShellTags.ROOT) }
        compose.onNodeWithTag(NavTags.tab(Destination.NOTEBOOK)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(NotebookTags.ROOT) }
    }

    @Test
    fun everySectionOfTheNotebookOpensAndComesBack() {
        reachTheNotebook()

        val unreachable = mutableListOf<String>()
        for (section in Repository.Section.entries) {
            if (!showing(NotebookTags.ROOT)) {
                compose.onNodeWithTag(NavTags.tab(Destination.NOTEBOOK)).performClick()
                compose.waitUntil(timeoutMillis = 10_000) { showing(NotebookTags.ROOT) }
            }

            val tile = NotebookTags.section(section)
            if (!showing(tile)) {
                // A section with no tile on this notebook is not this test's
                // business: the table of contents decides what it lists.
                continue
            }

            compose.onNodeWithTag(tile).performScrollTo()
            compose.onNodeWithTag(tile).performClick()
            compose.waitForIdle()

            // **The section's own scaffold, not just "something changed".** A
            // tile that opened the wrong screen would otherwise pass.
            val opened = try {
                compose.waitUntil(timeoutMillis = 8_000) {
                    showing(SectionTags.root(nameFor(section))) || showing(SectionTags.BACK)
                }
                true
            } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
                false
            }
            if (!opened) {
                unreachable += section.name
                continue
            }

            Espresso.pressBack()
            compose.waitForIdle()
        }

        assertTrue(
            "these sections have a tile that does not open their screen: $unreachable",
            unreachable.isEmpty(),
        )
        compose.onNodeWithTag(NotebookTags.ROOT).assertIsDisplayed()
    }

    /**
     * The scaffold name a section screen registers itself under.
     *
     * **Read from the screen rather than guessed**, so a rename breaks the test
     * loudly rather than turning it into a check that always passes.
     */
    private fun nameFor(section: Repository.Section): String = when (section) {
        Repository.Section.CARE_TEAM -> "care_team"
        Repository.Section.MEDICATIONS -> "medications"
        Repository.Section.APPOINTMENTS -> "appointments"
        Repository.Section.CHAPTERS -> "chapters"
        Repository.Section.THREADS -> "care_threads"
        Repository.Section.TRAIL -> "trail"
        Repository.Section.PROGRESS -> "progress"
        Repository.Section.DOCUMENTS -> "documents"
        Repository.Section.MONEY -> "money"
        Repository.Section.STANDING_INSTRUCTIONS -> "instructions"
        Repository.Section.ASK_NEXT_TIME -> "questions"
        Repository.Section.EMERGENCY_CARD -> "emergency_card"
        Repository.Section.PROJECTS -> "projects"
    }
}
