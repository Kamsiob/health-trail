package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.NotebookGroup
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.NotebookTags
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.emphasisFrom
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The table of contents, and the three promises it makes.
 *
 * That the sections never move, so a person who learned where something was
 * finds it there next month. That a count of zero reads as words rather than as
 * a digit, because a column of zeros is a scorecard of what someone has failed
 * to fill in and this app never keeps score of anyone's diligence. And that a
 * section the situation template folds is quieter but never gone, which is the
 * promise it would be easiest to break by accident and the most damaging to
 * break, because a person who cannot find money the one month it matters has
 * been failed by the app rather than by the template.
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

    private fun show(
        counts: Map<Repository.Section, Int> = emptyMap(),
        emphasis: Map<Repository.Section, Emphasis> = emptyMap(),
        locale: Locale = Locale.ENGLISH,
        direction: LayoutDirection = LayoutDirection.Ltr,
        onOpen: (Repository.Section) -> Unit = {},
    ) {
        val strings = Strings.load(context, locale)
        val rows = order.map {
            SectionCount(it, counts[it] ?: 0, emphasis[it] ?: Emphasis.STANDING)
        }
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalLayoutDirection provides direction,
                ) {
                    NotebookScreen(sections = rows, onOpen = onOpen)
                }
            }
        }
    }

    /**
     * Scrolls by the lazy list's own item key rather than by hunting for a test
     * tag.
     *
     * `performScrollToNode` walks the list a viewport at a time and gives up
     * when it has scrolled as far as it thinks it can, which it got wrong for
     * the Arabic catalog: it stopped two rows short of the end and reported the
     * rows as absent when they were only further down. Scrolling by key asks
     * the list where the item is and goes there, so a test failure after this
     * means the row is genuinely missing rather than merely out of view.
     */
    /**
     * **Scrolls to the node itself rather than asking a list for a key.**
     *
     * The screen became a plain scrolling column of tiles on 2026-08-03. Twelve
     * tiles and a hero is a fixed, small screen where laziness buys nothing and
     * costs the thing that matters: every tile exists in the tree, so a test and
     * a screen reader both reach it without a scroll dance. `performScrollToKey`
     * needs a lazy list to ask, and there is no longer one here.
     */
    private fun scrollTo(section: Repository.Section) {
        compose.onNodeWithTag(NotebookTags.section(section)).performScrollTo()
    }

    @Test
    fun everySectionIsPresentAndReachable() {
        show()
        order.forEach { section ->
            scrollTo(section)
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

        // Every section is empty in this fixture, so every one of them has to
        // say it. Walked row by row rather than counted in one pass, because
        // the list is longer than the screen and a lazy list only composes what
        // it has scrolled to: counting matches would quietly assert less than
        // it looks like it does.
        // **The emergency card says something else, and it is supposed to.**
        // Every other section holds a list somebody adds to, so "9 items" is a
        // fact about their notebook. A card is a single thing, and "1 item" was
        // the shape of the table showing through onto the front door. It says
        // whether there is anything on it instead. This test asserted the
        // generic string for all twelve and went red the moment that landed.
        order.forEach { section ->
            scrollTo(section)
            val expected = if (section == Repository.Section.EMERGENCY_CARD) {
                strings("notebook.count.emergency_card", "count" to 0)
            } else {
                empty
            }
            compose.onNodeWithTag(NotebookTags.count(section), useUnmergedTree = true)
                .assertTextEquals(expected)
        }
    }

    @Test
    fun aSectionWithItemsShowsHowMany() {
        show(mapOf(Repository.Section.THREADS to 5))
        val strings = Strings.load(context)

        scrollTo(Repository.Section.THREADS)
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

    // -- what the situation template is allowed to change -------------------

    @Test
    fun aFoldedSectionIsStillThereAndStillOpens() {
        // The promise most worth a test. A hospital stay folds money, and a
        // person who cannot find money the one month a bill arrives has been
        // failed by the app.
        var opened: Repository.Section? = null
        show(
            emphasis = mapOf(Repository.Section.MONEY to Emphasis.FOLDED),
            onOpen = { opened = it },
        )

        scrollTo(Repository.Section.MONEY)
        compose.onNodeWithTag(NotebookTags.section(Repository.Section.MONEY))
            .assertIsDisplayed()
            .performClick()

        assertEquals("a folded section did not open on one tap", Repository.Section.MONEY, opened)
    }

    @Test
    fun aFoldedSectionStillSaysHowMuchIsInIt() {
        // Folding changes where the count sits, never whether it is shown. A
        // row that hid its count would be telling the person that this section
        // does not matter, which is the app making a judgment.
        show(
            counts = mapOf(Repository.Section.MONEY to 3),
            emphasis = mapOf(Repository.Section.MONEY to Emphasis.FOLDED),
        )
        val strings = Strings.load(context)

        scrollTo(Repository.Section.MONEY)
        compose.onNodeWithTag(NotebookTags.count(Repository.Section.MONEY), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText(strings("notebook.count", "count" to 3)).assertIsDisplayed()
    }

    @Test
    fun theTemplateChangesWeightAndNeverOrder() {
        // Rendered twice, once with no template and once with a template that
        // folds two sections and puts four forward, and asserted to hold the
        // same sections in the same groups both times.
        val groupOf = NotebookGroup.entries.flatMap { g -> g.sections.map { it to g } }.toMap()
        val emphasis = emphasisFrom(
            forward = listOf("trail", "appointments", "documents", "standing_instructions"),
            folded = listOf("money", "progress"),
        )

        assertEquals(Emphasis.FORWARD, emphasis[Repository.Section.TRAIL])
        assertEquals(Emphasis.FOLDED, emphasis[Repository.Section.MONEY])
        assertNull(
            "a section named in neither array must be left standing",
            emphasis[Repository.Section.CARE_TEAM],
        )

        // Every section still sits in the group it always sat in, since the
        // groups are a property of the app and not of the template.
        order.forEach { section ->
            assertTrue("$section belongs to no group", groupOf.containsKey(section))
        }
        assertEquals(
            "the groups do not cover the twelve sections exactly once",
            order.toSet(),
            NotebookGroup.entries.flatMap { it.sections }.toSet(),
        )
        assertEquals(
            "a section appears in more than one group",
            order.size,
            NotebookGroup.entries.sumOf { it.sections.size },
        )

        show(emphasis = emphasis)
        order.forEach { section ->
            scrollTo(section)
            compose.onNodeWithTag(NotebookTags.section(section)).assertIsDisplayed()
        }
    }

    @Test
    fun aTemplateIdThatNamesNoSectionIsSkippedRatherThanFailing() {
        // What an export written by a later version of this app looks like.
        val emphasis = emphasisFrom(forward = listOf("trail", "wellness_score"), folded = listOf())
        assertEquals(Emphasis.FORWARD, emphasis[Repository.Section.TRAIL])
        assertEquals("an unknown id became a section", 1, emphasis.size)
    }

    @Test
    fun aSectionInBothArraysIsPutForwardRatherThanBuried() {
        val emphasis = emphasisFrom(forward = listOf("money"), folded = listOf("money"))
        assertEquals(Emphasis.FORWARD, emphasis[Repository.Section.MONEY])
    }

    @Test
    fun aNotebookWithNoSituationTemplateRendersEverySection() {
        // "Not sure yet" is a real answer to the situation picker and it must
        // not cost the person a working notebook.
        show(emphasis = emptyMap())
        order.forEach { section ->
            scrollTo(section)
            compose.onNodeWithTag(NotebookTags.section(section)).assertIsDisplayed()
        }
    }

    // -- the states the screen has to hold in ------------------------------

    @Test
    fun theSectionsKeepTheirOrderWithoutTheGroupHeaders() {
        // **The four group headers went on 2026-08-03 and the order did not.**
        // They were a fix for twelve rows at uniform weight, which a grid is
        // not: a tile is told apart by shape and position, and four headers
        // chopping twelve tiles into groups of three, four, three and two left
        // a half empty row in three of the four and cost most of what the grid
        // was worth. What 10.8 protects is that nothing moved, and this is the
        // assertion for that: the sections still read in `NotebookGroup` order,
        // which is `MASTER_SPEC.md` section 4.4's order, unchanged.
        show()
        NotebookGroup.entries.flatMap { it.sections }.forEach { section ->
            scrollTo(section)
            compose.onNodeWithTag(NotebookTags.section(section)).assertIsDisplayed()
        }
        assertEquals(
            "the order the grid reads in is no longer the order the app promises",
            order,
            NotebookGroup.entries.flatMap { it.sections },
        )
    }

    @Test
    fun itHoldsUpInTheLongestLanguage() {
        // Spanish is where this app's wrapping breaks first. Every tile still
        // renders, which is the assertion that catches a name running off the
        // end edge of its tile or a row collapsing to nothing.
        show(locale = Locale("es"))
        order.forEach { section ->
            scrollTo(section)
            compose.onNodeWithTag(NotebookTags.section(section)).assertIsDisplayed()
        }
    }

    @Test
    fun itHoldsUpRightToLeft() {
        show(
            counts = mapOf(Repository.Section.TRAIL to 214),
            emphasis = mapOf(
                Repository.Section.TRAIL to Emphasis.FORWARD,
                Repository.Section.MONEY to Emphasis.FOLDED,
            ),
            locale = Locale("ar"),
            direction = LayoutDirection.Rtl,
        )
        order.forEach { section ->
            scrollTo(section)
            compose.onNodeWithTag(NotebookTags.section(section)).assertIsDisplayed()
        }
    }

    @Test
    fun aFullNotebookRendersAsWellAsAnEmptyOne() {
        // The many-item state, including a count with four digits, which is
        // where a row that assumed a short count would break.
        show(
            counts = order.associateWith { 1247 },
            emphasis = mapOf(Repository.Section.MONEY to Emphasis.FOLDED),
        )
        val strings = Strings.load(context)
        val many = strings("notebook.count", "count" to 1247)

        order.forEach { section ->
            scrollTo(section)
            val expected = if (section == Repository.Section.EMERGENCY_CARD) {
                strings("notebook.count.emergency_card", "count" to 1247)
            } else {
                many
            }
            compose.onNodeWithTag(NotebookTags.count(section), useUnmergedTree = true)
                .assertTextEquals(expected)
        }
    }
}
