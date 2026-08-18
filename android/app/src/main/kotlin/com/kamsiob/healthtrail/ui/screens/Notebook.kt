package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.TipsSheet
import com.kamsiob.healthtrail.ui.v4.fabScrollClearance
import com.kamsiob.healthtrail.ui.v4.tipForDestination
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.SearchDoor
import com.kamsiob.healthtrail.ui.theme.alertHue
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider

object NotebookTags {
    const val ROOT = "notebook_root"
    const val SEARCH = "notebook_search"
    fun section(section: Repository.Section) = "notebook_section_${section.name.lowercase()}"
    fun count(section: Repository.Section) = "notebook_count_${section.name.lowercase()}"
    const val WAITING = "notebook_waiting"
    const val OPEN_INCIDENTS = "notebook_open_incidents"

    /** One section's row. Kept as `section(...)` so existing tests still find it. */
    fun row(section: Repository.Section) = section(section)
}

/**
 * How much weight a section carries in this notebook, which the active
 * situation template decides.
 *
 * **This is emphasis, never availability.** All three are present, all three
 * are one tap from the table of contents, and none of them changes where a
 * section sits. A notebook with no situation template renders every section
 * [STANDING], because "Not sure yet" is a real answer to the situation picker
 * and it must not cost the person a working notebook.
 */
enum class Emphasis {
    /** Named in the template's `forward` array. The fullest row. */
    FORWARD,

    /** Named in neither array, or no template chosen. */
    STANDING,

    /** Named in the template's `folded` array. Collapsed to one line, never hidden. */
    FOLDED,
}

/** One row of the table of contents: a section, how much is in it, and its weight. */
data class SectionCount(
    val section: Repository.Section,
    val count: Int,
    val emphasis: Emphasis = Emphasis.STANDING,
    /**
     * What Money says instead of counting, when there is something unsettled.
     *
     * **The grid draws "$15,072.98 not settled"**, which is the number somebody
     * opens Money to find; "6 bills" is true and is not it. #347. Null when
     * nothing is unsettled, and the row counts bills as usual, because a zero
     * on a settled notebook is a number with nothing behind it.
     */
    val amount: String? = null,
)

/**
 * The weights a situation template asks for, turned into sections.
 *
 * Pure, and separate from the screen and from the catalog, because this is the
 * rule that has to be right rather than the pixels: an app that quietly buried
 * a section a person needed would be a worse failure than one that drew the row
 * badly.
 *
 * The template's ids are the section names in lower case, which is what
 * `templates/data/situations.json` already carries. **An id that names no
 * section is skipped rather than failing**, because an export written by a
 * later version of this app will carry sections this one has never heard of and
 * it must still open.
 *
 * Folded is applied first and forward second, so a hand edited template naming
 * the same section in both arrays resolves to the louder of the two. Nothing in
 * the bundled data does that. This is here so that it could never quietly bury
 * a section that was also meant to be put forward.
 */
fun emphasisFrom(
    forward: List<String>,
    folded: List<String>,
): Map<Repository.Section, Emphasis> {
    fun section(id: String) =
        Repository.Section.entries.firstOrNull { it.name.lowercase() == id }

    return buildMap {
        folded.forEach { id -> section(id)?.let { put(it, Emphasis.FOLDED) } }
        forward.forEach { id -> section(id)?.let { put(it, Emphasis.FORWARD) } }
    }
}

/**
 * The four groups of the table of contents, and which sections sit in each.
 *
 * **The grouping is fixed and the order inside it is the order in
 * `MASTER_SPEC.md` section 4.4, unchanged.** Grouping was added to give the
 * screen a hierarchy, and it would have cost more than it gave if it had also
 * reordered the sections: a person who learned where documents were would have
 * had to learn it again. So the twelve sections are read in their existing
 * order and a header is placed at each of the three points where the subject
 * changes. Nothing moved.
 */
enum class NotebookGroup(val labelKey: String, val sections: List<Repository.Section>) {
    PEOPLE_AND_CARE(
        "notebook.group.people_and_care",
        listOf(
            Repository.Section.CARE_TEAM,
            Repository.Section.MEDICATIONS,
            Repository.Section.APPOINTMENTS,
        ),
    ),
    THE_RECORD(
        "notebook.group.the_record",
        listOf(
            Repository.Section.CHAPTERS,
            Repository.Section.THREADS,
            Repository.Section.TRAIL,
            // **Notes are not here, and that is deliberate.** They were a row
            // in this group until the owner moved them into the bottom bar on
            // 2026-08-18, #397. **A destination is not also a notebook row**,
            // which is the shape Projects has had all along: two doors to one
            // place is rule 16's two answers to one question, and the one a
            // person learns is whichever they found first.
            Repository.Section.PROGRESS,
        ),
    ),
    PAPERWORK(
        "notebook.group.paperwork",
        listOf(
            Repository.Section.DOCUMENTS,
            Repository.Section.MONEY,
            Repository.Section.STANDING_INSTRUCTIONS,
        ),
    ),
    KEEP_AT_HAND(
        "notebook.group.keep_at_hand",
        listOf(
            Repository.Section.ASK_NEXT_TIME,
            Repository.Section.EMERGENCY_CARD,
        ),
    ),
}


/**
 * The notebook: the table of contents for everything written down.
 *
 * **Written fresh on Material 3 Expressive, D196**, and the file it replaces is
 * deleted. `Scaffold` with `LargeFlexibleTopAppBar`, which collapses the title
 * as the list comes up; `LazyColumn`, so a notebook with three hundred things in
 * it composes the rows it is showing; `Card` for each group and `ListItem`
 * through [ListRow] for each section.
 *
 * **And it is not one color any more.** The owner, 2026-08-17: "the Notebook
 * page is dreary and ugly, the colors are overwhelmingly depressing... there
 * could never be a page that is overwhelmingly one color." He was right and the
 * measurement said so before he did: every mark on this page sat in its hue's
 * palest wash, so twelve sections drew twelve near-white discs on beige, and the
 * page was two beiges with hints in it. D198 puts the mark in the hue's base
 * with the computed ink on top, so the twelve identities are actually visible at
 * the size a mark is, and the surfaces stay neutral because a page is read on
 * its surfaces and found by its marks.
 *
 * **What needs the person leads, in its own color.** The open incidents and the
 * unfiled tray are the one thing on this screen that is about right now rather
 * than about where something lives, so they sit above the table of contents in a
 * tonal block, and they are absent when there is nothing: rule 11.5, announcing
 * the absence of a problem is not information.
 *
 * **The fold is gone and so is the wall it managed.** Every section is on the
 * screen under the name of the group it belongs to, D163, after the owner
 * watched a stranger face this screen: "a gigantic wall of options, there's no
 * categories, nothing makes sense".
 *
 * **Each section counts in its own units**, which is what `m3v4-1` draws: "9
 * people", "6 on the list", "231 entries". #347.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    sections: List<SectionCount>,
    onOpen: (Repository.Section) -> Unit,
    modifier: Modifier = Modifier,
    /** How many entries are waiting in the Unfiled tray. Zero shows nothing. */
    waiting: Int = 0,
    /** How many incidents are still open. Zero shows nothing. */
    openIncidents: Int = 0,
    onOpenUnfiled: () -> Unit = {},
    onOpenIncidents: () -> Unit = {},
    /** Opens universal search, which this screen is the front door to. */
    onSearch: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val bySection = sections.associateBy { it.section }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showTips by remember { mutableStateOf(false) }
    if (showTips) {
        TipsSheet(tip = tipForDestination("notebook"), onDismiss = { showTips = false })
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.background,
        // **The shell owns the insets.** It pads for the status bar and draws
        // the navigation bar below this.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                // **No eyebrow on a tab root.** The bottom bar already says
                // where you are, and this screen used to wear a second name over
                // its first, which is the vocabulary the stranger test failed
                // on. #376.
                title = { Text(text = strings["notebook.title"]) },
                // **Search left the top bar for the door under the title**,
                // which is where `m3v4-1` draws it. #388 finding 3: this screen
                // opened with a title and then rows of equal weight and nothing
                // on it led, and the one thing every person does on a notebook
                // this size was a 24dp glyph in a corner. Two ways into search
                // on one screen would be the same defect the add controls had,
                // so the icon is gone rather than duplicated.
                actions = {
                    com.kamsiob.healthtrail.ui.v4.TipsButton(
                        onOpen = { showTips = true },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    scrolledContainerColor = scheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inset ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(NotebookTags.ROOT),
            contentPadding = PaddingValues(
                start = Space.screenHorizontal,
                end = Space.screenHorizontal,
                top = inset.calculateTopPadding(),
                bottom = inset.calculateBottomPadding() + fabScrollClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            item(key = "notebook-search") {
                SearchDoor(
                    label = strings["today.search.everything"],
                    onOpen = onSearch,
                    modifier = Modifier.testTag(NotebookTags.SEARCH),
                )
            }

            if (waiting > 0 || openIncidents > 0) {
                item(key = "notebook-attention") {
                    // **The one tonal block on the page**, `docs/V4.md` 2.1. It
                    // is what is happening rather than where something lives, so
                    // it wears a color and the table of contents under it does
                    // not. Alert where an incident is open, because an open
                    // incident is the loudest true thing this screen knows;
                    // gold otherwise, which is the app's own.
                    val lead = if (openIncidents > 0) alertHue() else goldHue()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = lead.wash),
                        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
                    ) {
                        if (openIncidents > 0) {
                            ListRow(
                                title = strings("today.open.incidents", "count" to openIncidents),
                                mark = Symbols.incidents,
                                markHue = alertHue(),
                                isDoor = true,
                                onClick = onOpenIncidents,
                                modifier = Modifier.testTag(NotebookTags.OPEN_INCIDENTS),
                            )
                            if (waiting > 0) RowDivider()
                        }
                        if (waiting > 0) {
                            ListRow(
                                title = strings("unfiled.waiting", "count" to waiting),
                                mark = Symbols.noteStack,
                                markHue = goldHue(),
                                isDoor = true,
                                onClick = onOpenUnfiled,
                                modifier = Modifier.testTag(NotebookTags.WAITING),
                            )
                        }
                    }
                }
            }

            // **Every section, under the name of the group it belongs to**, in
            // an order that never changes, so somebody who learned where the
            // documents live finds them there next month. D163.
            for (group in NotebookGroup.entries) {
                val rows = group.sections.mapNotNull { bySection[it] }
                if (rows.isEmpty()) continue
                item(key = "notebook-label-${group.name}") {
                    Eyebrow(
                        text = strings[group.labelKey],
                        modifier = Modifier.padding(top = Space.m, bottom = Space.xs),
                    )
                }
                item(key = "notebook-group-${group.name}") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = scheme.surfaceContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
                    ) {
                        rows.forEachIndexed { index, row ->
                            SectionRow(row = row, onOpen = onOpen)
                            if (index != rows.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * One section, as a row in a group.
 *
 * **Its mark sits in its own section's color**, `DESIGN.md` 4.3 and D198, which
 * is what lets a person find the documents by color before reading a word. The
 * mapping is the owner's and lives in `hueFor` alone.
 *
 * **Each section counts in its own units.** The key is derived from the section,
 * so a section added later fails loudly at a missing key rather than falling
 * back to a generic word. D133.
 */
@Composable
private fun SectionRow(
    row: SectionCount,
    onOpen: (Repository.Section) -> Unit,
) {
    val strings = LocalStrings.current
    val countKey = "notebook.count.${row.section.name.lowercase()}"
    ListRow(
        title = strings[labelKey(row.section)],
        support = row.amount?.let { strings("notebook.count.money.unsettled", "amount" to it) }
            ?: strings(countKey, "count" to row.count),
        supportTestTag = NotebookTags.count(row.section),
        mark = Symbols.of(row.section),
        markHue = hueFor(row.section),
        isDoor = true,
        onClick = { onOpen(row.section) },
        modifier = Modifier.testTag(NotebookTags.row(row.section)),
    )
}

internal fun labelKey(section: Repository.Section): String = when (section) {
    Repository.Section.NOTES -> "notebook.section.notes"
    Repository.Section.CARE_TEAM -> "notebook.section.care_team"
    Repository.Section.MEDICATIONS -> "notebook.section.medications"
    Repository.Section.APPOINTMENTS -> "notebook.section.appointments"
    Repository.Section.CHAPTERS -> "notebook.section.chapters"
    Repository.Section.THREADS -> "notebook.section.threads"
    Repository.Section.TRAIL -> "notebook.section.trail"
    Repository.Section.PROGRESS -> "notebook.section.progress"
    Repository.Section.DOCUMENTS -> "notebook.section.documents"
    Repository.Section.MONEY -> "notebook.section.money"
    Repository.Section.STANDING_INSTRUCTIONS -> "notebook.section.standing_instructions"
    Repository.Section.ASK_NEXT_TIME -> "notebook.section.ask_next_time"
    Repository.Section.EMERGENCY_CARD -> "notebook.section.emergency_card"
    Repository.Section.PROJECTS -> "notebook.section.projects"
}
