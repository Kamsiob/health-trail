package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.SearchScreen
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How many stops a screen reader makes, and what each one says.
 *
 * ## Why this exists, and what it corrects
 *
 * `ScreenReaderTest` proves every control is labeled and says plainly that it
 * cannot see traversal order. #44 asks for the rest, and the obvious way to get
 * it, turning TalkBack on and dumping the view hierarchy over adb, **does not
 * work and quietly looks like it does.**
 *
 * `uiautomator dump`, with or without `--compressed`, prints the view tree. For
 * a Compose app that is the raw node list, not the merged semantics tree the
 * reader actually consumes. On 2026-08-02 it reported the notebook's twelve
 * rows as twenty four separate stops, "Care team" then "Nothing yet", which
 * read as a regression against D54 and was a measurement artifact. Adding
 * explicit merging changed the app and did not change the dump, which is what
 * gave it away.
 *
 * **The merged semantics tree is the authority**, and Compose hands it over
 * directly: `useUnmergedTree = false` is the tree a reader walks. So this test
 * asks Compose rather than asking the window manager.
 *
 * ## What it still cannot do
 *
 * **It does not hear anything.** How a label sounds, whether a pause lands in
 * the right place, and whether the reader's own verbosity settings make a row
 * unbearable are questions for ears, and #44 stays open for them. What this
 * closes is the countable half: how many stops there are, in what order, and
 * what text each carries.
 */
@RunWith(AndroidJUnit4::class)
class ReaderStopsTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) { content() }
            }
        }
    }

    /**
     * Every stop a reader would make, in order, with what it would announce.
     *
     * A stop is a node that is either touchable or carries text, taken from the
     * **merged** tree, which is the one a reader walks.
     */
    private fun stops(): List<String> {
        val root = compose.onAllNodes(isRoot(), useUnmergedTree = false).fetchSemanticsNodes()
        val found = mutableListOf<String>()

        fun walk(node: SemanticsNode) {
            val config = node.config
            val text = buildList {
                config.getOrNull(SemanticsProperties.ContentDescription)?.let { addAll(it) }
                config.getOrNull(SemanticsProperties.Text)?.let { texts ->
                    addAll(texts.map { it.text })
                }
            }.joinToString(", ").trim()

            val touchable = config.contains(SemanticsActions.OnClick) ||
                config.contains(SemanticsActions.OnLongClick)

            if (text.isNotEmpty() && (touchable || node.children.isEmpty())) {
                found += text
            }
            node.children.forEach(::walk)
        }

        root.forEach(::walk)
        return found
    }

    /** What the notebook's own subtitle promises, and what a reader counts. */
    private val NOTEBOOK_ROWS = 12

    private val sections = Repository.Section.entries.map {
        SectionCount(section = it, count = 0, emphasis = Emphasis.STANDING)
    }

    @Test
    fun aNotebookRowIsOneStopRatherThanTwoFragments() {
        show {
            NotebookScreen(sections = sections, onOpen = {})
        }

        val all = stops()

        // **The row's name and its count are one announcement.** "Care team,
        // nothing yet" is one stop; "Care team" followed by "Nothing yet" is
        // two, and twelve sections at two stops each is twenty four swipes
        // across a table of contents whose whole promise is that the places
        // never move.
        val careTeam = all.filter { it.contains("Care team") }
        assertEquals(
            "the care team row should be exactly one stop, got: $careTeam",
            1,
            careTeam.size,
        )
        assertTrue(
            "the row's count is not in the same announcement as its name: $careTeam",
            careTeam.first().contains("Nothing yet"),
        )
    }

    @Test
    fun theNotebookIsTwelveRowsRatherThanTwentyFourFragments() {
        show {
            NotebookScreen(sections = sections, onOpen = {})
        }

        val rowStops = stops().count { it.contains("Nothing yet") }

        // **Twelve, not thirteen, and the difference is deliberate.**
        // `Repository.Section` has thirteen values because Projects is a
        // section of the record, and the notebook shows twelve because Projects
        // is its own destination in the bottom navigation. "Twelve places that
        // never move" is the promise the screen makes in its own subtitle, and
        // this is the number a reader counts.
        assertEquals(
            "every section row should carry its count in one stop",
            NOTEBOOK_ROWS,
            rowStops,
        )
    }

    @Test
    fun readerStopsFollowVisualOrderOnTheNotebook() {
        show {
            NotebookScreen(sections = sections, onOpen = {})
        }

        val all = stops()
        // The order `MASTER_SPEC.md` 4.4 fixes, which is the order the table of
        // contents promises never changes. A reader meeting them in a different
        // order is meeting a different notebook.
        val order = listOf("Care team", "Medications", "Appointments", "Chapters")
        val positions = order.map { name -> all.indexOfFirst { it.contains(name) } }

        assertTrue("a named section never appeared to the reader: $positions", positions.none { it < 0 })
        assertEquals(
            "the reader meets the sections in a different order than the screen shows",
            positions.sorted(),
            positions,
        )
    }

    @Test
    fun aSearchResultIsOneStopCarryingItsDateAndItsChapter() {
        show {
            SearchScreen(
                query = "ward",
                onQueryChange = {},
                results = listOf(
                    Repository.SearchHit(
                        id = "h1",
                        section = Repository.Section.TRAIL,
                        title = "Nurse Okonkwo, ward 4",
                        kind = "call",
                        detail = "Said the dressing looks better",
                        chapterName = "Riverbend Rehab",
                        occurredEdtf = "2026-08-02",
                        occurredStart = 1_785_000_000_000L,
                    ),
                ),
                onOpen = {},
                onBack = {},
            )
        }

        val result = stops().filter { it.contains("Okonkwo") }
        assertEquals("a search result should be one stop, got: $result", 1, result.size)

        // **Where in the journey it happened travels with it**, per
        // `MASTER_SPEC.md` 4.8, and a reader who has to swipe to a separate
        // node for the chapter is being handed the row in pieces.
        assertTrue(
            "the result's chapter is not in the same announcement: ${result.first()}",
            result.first().contains("Riverbend Rehab"),
        )
    }
}
