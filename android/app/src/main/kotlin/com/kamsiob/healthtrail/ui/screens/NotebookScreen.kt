package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Lead
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider
import com.kamsiob.healthtrail.ui.v4.SearchDoor
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Hero
import com.kamsiob.healthtrail.ui.components.HeroLine
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.HeaderActions
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.components.tipForDestination
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import com.kamsiob.healthtrail.ui.components.arrivesInOrder
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.components.fabScrollClearance
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.Tile
import com.kamsiob.healthtrail.ui.components.tileColumns
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object NotebookTags {
    const val ROOT = "notebook_root"
    const val SEARCH = "notebook_search"
    fun section(section: Repository.Section) = "notebook_section_${section.name.lowercase()}"
    fun count(section: Repository.Section) = "notebook_count_${section.name.lowercase()}"
    const val WAITING = "notebook_waiting"
    const val OPEN_INCIDENTS = "notebook_open_incidents"
    const val FOLD = "notebook_fold"

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
 * **Rewritten from scratch onto `ui/v4`**, #386, not converted. The owner: "no
 * old design language at all. get rid of it so it doesn't influence." What this
 * screen draws is `m3v4-1`, measured: a lead at display size, the door into
 * search directly under it, and quiet tonal blocks of rows with a small colored
 * label naming each group.
 *
 * **The fold is gone and so is the wall it managed.** Every section is on the
 * screen under the name of the group it belongs to, which is what D163 decided
 * after the owner watched a stranger face this screen: "a gigantic wall of
 * options, there's no categories, nothing makes sense".
 *
 * **What needs the person sits in its own block at the top**, and is absent
 * when there is nothing, per rule 11.5: announcing the absence of a problem is
 * not information.
 */
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
    val colors = HealthTrail.colors
    val bySection = sections.associateBy { it.section }
    var showTips by remember { mutableStateOf(false) }
    if (showTips) {
        TipsSheet(tip = tipForDestination("notebook"), onDismiss = { showTips = false })
    }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(NotebookTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Spacer(Modifier.height(Space.sm))

            // **No eyebrow on a tab root.** The bottom bar already says where
            // you are, and this screen used to wear a second name over its
            // first, which is the vocabulary the stranger test failed on. #376.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lead(text = strings["notebook.title"], modifier = Modifier.weight(1f))
                HeaderActions(onTips = { showTips = true })
            }

            SearchDoor(
                label = strings["today.search.everything"],
                onOpen = onSearch,
                modifier = Modifier.testTag(NotebookTags.SEARCH),
            )

            if (waiting > 0 || openIncidents > 0) {
                Block(padding = Space.none) {
                    if (openIncidents > 0) {
                        ListRow(
                            title = strings("today.open.incidents", "count" to openIncidents),
                            mark = Symbols.incidents,
                            markTint = colors.alertInk,
                            markWash = colors.alertWash,
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
                            markTint = colors.goldInk,
                            markWash = colors.goldWash,
                            isDoor = true,
                            onClick = onOpenUnfiled,
                            modifier = Modifier.testTag(NotebookTags.WAITING),
                        )
                    }
                }
            }

            // **Every section, under the name of the group it belongs to**, in
            // an order that never changes, so somebody who learned where the
            // documents live finds them there next month. D163.
            for ((groupIndex, group) in NotebookGroup.entries.withIndex()) {
                val rows = group.sections.mapNotNull { bySection[it] }
                if (rows.isEmpty()) continue
                Spacer(Modifier.height(Space.s))
                // The label belongs to the block under it, so it arrives with
                // it rather than floating between two groups. D168.
                Box(modifier = Modifier.arrivesInOrder(groupIndex * 2)) {
                    Eyebrow(text = strings[group.labelKey])
                }
                Block(padding = Space.none) {
                    rows.forEachIndexed { index, row ->
                        SectionRow(row = row, onOpen = onOpen)
                        if (index != rows.lastIndex) RowDivider()
                    }
                }
            }

            Spacer(Modifier.height(fabScrollClearance))
        }
    }
}


/**
 * One section, as a row in a block.
 *
 * **Its mark sits in its own section's wash**, `DESIGN.md` 4.3, which is what
 * lets a person find the documents by color before reading a word. The mapping
 * is the owner's and lives in `hueFor` alone.
 *
 * **Each section counts in its own units**, which is what `m3v4-1` draws: "9
 * people", "6 on the list", "231 entries". It said "N items" for all twelve
 * once, and "Money, 6 items" tells somebody nothing they came for. #347. The
 * key is derived from the section, so a section added later fails loudly at a
 * missing key rather than falling back to a generic word. D133.
 */
@Composable
private fun SectionRow(
    row: SectionCount,
    onOpen: (Repository.Section) -> Unit,
) {
    val strings = LocalStrings.current
    val hue = hueFor(row.section)
    val countKey = "notebook.count.${row.section.name.lowercase()}"
    ListRow(
        title = strings[labelKey(row.section)],
        support = row.amount?.let { strings("notebook.count.money.unsettled", "amount" to it) }
            ?: strings(countKey, "count" to row.count),
        supportTestTag = NotebookTags.count(row.section),
        mark = Symbols.of(row.section),
        markTint = hue.ink,
        markWash = hue.wash,
        isDoor = true,
        onClick = { onOpen(row.section) },
        modifier = Modifier.testTag(NotebookTags.row(row.section)),
    )
}


internal fun labelKey(section: Repository.Section): String = when (section) {
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
