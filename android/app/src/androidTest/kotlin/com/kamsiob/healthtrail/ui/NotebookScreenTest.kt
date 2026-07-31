package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.NotebookTags
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The table of contents, and the two promises it makes.
 *
 * That the sections never move, so a person who learned where something was
 * finds it there next month. And that a count of zero reads as words rather
 * than as a digit, because a column of zeros is a scorecard of what someone has
 * failed to fill in and this app never keeps score of anyone's diligence.
 */
@RunWith(AndroidJUnit4::class)
class NotebookScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val order = listOf(
        Repository.Section.CARE_TEAM,
        Repository.Section.MEDICATIONS,
        Repository.Section.APPOINTMENTS,
        Repository.Section.CHAPTERS,
        Repository.Section.THREADS,
        Repository.Section.TRAIL,
        Repository.Section.PROGRESS,
        Repository.Section.DOCUMENTS,
        Repository.Section.MONEY,
        Repository.Section.STANDING_INSTRUCTIONS,
        Repository.Section.ASK_NEXT_TIME,
        Repository.Section.EMERGENCY_CARD,
    )

    private fun show(counts: Map<Repository.Section, Int> = emptyMap()) {
        val strings = Strings.load(context)
        val rows = order.map { SectionCount(it, counts[it] ?: 0) }
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    NotebookScreen(sections = rows, onOpen = {})
                }
            }
        }
    }

    @Test
    fun everySectionIsPresentAndReachable() {
        show()
        order.forEach { section ->
            compose.onNodeWithTag(NotebookTags.ROOT)
                .performScrollToNode(hasTestTag(NotebookTags.section(section)))
            compose.onNodeWithTag(NotebookTags.section(section)).assertIsDisplayed()
        }
    }

    @Test
    fun anEmptySectionSaysNothingYetRatherThanShowingAZero() {
        show()
        val strings = Strings.load(context)
        val empty = strings("notebook.count", "count" to 0)

        assertTrue(
            "the empty count renders a digit, which reads as a scorecard: $empty",
            !empty.contains("0"),
        )

        // Every section is empty in this fixture, so all twelve say it. Asserted
        // as a count rather than as a single node, because onNodeWithText
        // expects exactly one match and finding twelve is the correct outcome
        // here rather than an error.
        assertEquals(
            "not every empty section said so",
            order.size,
            compose.onAllNodesWithText(empty).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aSectionWithItemsShowsHowMany() {
        show(mapOf(Repository.Section.THREADS to 5))
        val strings = Strings.load(context)

        compose.onNodeWithTag(NotebookTags.ROOT)
            .performScrollToNode(hasTestTag(NotebookTags.section(Repository.Section.THREADS)))
        compose.onNodeWithText(strings("notebook.count", "count" to 5)).assertIsDisplayed()
    }

    @Test
    fun oneItemIsNotPluralized() {
        // The plural boundary, which is where a hand rolled count string breaks
        // first and where composing from a message template earns its keep.
        val strings = Strings.load(context)
        val one = strings("notebook.count", "count" to 1)
        val two = strings("notebook.count", "count" to 2)
        assertTrue("one and two render identically: $one", one != two)
    }
}
