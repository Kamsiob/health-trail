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
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.UniversalSearchDoor
import com.kamsiob.healthtrail.ui.components.Hero
import com.kamsiob.healthtrail.ui.components.HeroLine
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.GroupedRows
import com.kamsiob.healthtrail.ui.components.HeaderActions
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.components.tipForDestination
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import com.kamsiob.healthtrail.ui.components.arrivesInOrder
import com.kamsiob.healthtrail.ui.components.DenseRow
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
 * The table of contents, with live counts.
 *
 * **The sections never move.** Their order is fixed, none is ever hidden, and
 * none changes group, because the whole value of a table of contents is that a
 * person who learned where something was finds it in the same place next month.
 * What the situation template decides is weight: a section it puts forward gets
 * the fullest row, a section it folds collapses to a single line, and a folded
 * section is still right there in its own place, one tap away.
 *
 * **A count of zero is shown as words, not as a zero.** "Nothing yet" invites,
 * where a column of zeros reads as a scorecard of what the person has failed to
 * fill in. That is the same reason there are no progress rings here and no
 * percentage anywhere: the app never keeps score of someone's diligence. The
 * count is one style in one color at every weight, so the row's emphasis is
 * never mistaken for a judgment about how full the section is.
 *
 * **The empty state is the resting state.** A new notebook is twelve rows each
 * saying "Nothing yet", which is a complete screen rather than a blank one, so
 * there is no separate empty layout to fall into and no way to reach one.
 *
 * Composed from the icon tile 5.12, the group header 5.13, cards 5.3, Display L,
 * Display S, Label, Body M, and the Mono count style. Nothing new was invented.
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
    // **Open where the screen has room for it, closed where it does not.**
    // The fold exists so twelve rows do not overwhelm a short screen, and on a
    // tall one it was buying nothing and costing a tap: the notebook ended
    // above the halfway mark with the rest of the screen empty. **The fold is
    // still a fold**, so somebody who closes it on a tall phone keeps it
    // closed, and the order never changes either way. Rule 23.

    // **Measured against the closed notebook rather than the open one.**
    // Twelve rows do not fit on any phone, so asking whether everything fits
    // would keep the fold shut forever. The question worth asking is whether
    // closing it leaves the screen half empty, which it does on anything taller
    // than the closed content.
    //
    // **Asked of the window rather than of the layout.** A
    // `BoxWithConstraints` answers the same question and does it during
    // measurement, which made a journey test die with "performMeasureAndLayout
    // called during measure layout" when three classes ran together. The
    // window's size is known before anything is measured, and it is what lint
    // asks for in place of the configuration's own height.
    // **Compared in pixels, because that is what the window reports.** The
    // first version converted the window's height to dp and the fold stayed
    // shut on a screen with room: the conversion was going the wrong way, and
    // the only way to tell was to look at the phone rather than at the code.
    val density = LocalDensity.current

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(NotebookTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            Spacer(Modifier.height(Space.sm))

            // **No eyebrow chip on a tab root.** The bottom bar already says
            // where you are, and this screen wore a second name, "The binder",
            // over its first. Two names for one place is what the stranger
            // test failed on: the chip taught vocabulary nobody needed. The
            // chip stays on interior screens, where it is the way back. #376.
            // **The tab root gets its own page onboarding**, #379, beside the
            // title rather than under it, so the heading still leads.
            var showTips by remember { mutableStateOf(false) }
            if (showTips) {
                TipsSheet(
                    tip = tipForDestination("notebook"),
                    onDismiss = { showTips = false },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = strings["notebook.title"],
                    style = HealthTrail.type.displayM,
                    color = colors.ink,
                )
                HeaderActions(onTips = { showTips = true })
            }

            // **The door to everything, on the screen that is about
            // everything.** The approved v4 mockup puts it directly under the
            // title, `m3v4-1`, and the built screen had no way into search at
            // all: it was reachable from Today, Today's field and More, and
            // not from the table of contents.
            //
            // **This is the surface [UniversalSearchDoor] is for.** Its own
            // note rules it out inside a section, where a universal field
            // brings eleven other sections to answer a question about one.
            // The notebook is not a section, it is the way to all of them.
            UniversalSearchDoor(
                onOpen = onSearch,
                modifier = Modifier.testTag(NotebookTags.SEARCH),
            )

            // **What needs the person, as one row rather than a hero.**
            //
            // The grid draws it as a single grouped row with an open marker, and
            // that is right for this screen: the notebook's job is to be a table
            // of contents, so the one thing at the top is a door to whatever is
            // wrong rather than a display-size statement of it. Today is the
            // screen that says it large, and saying it twice at two sizes would
            // be the app raising its voice about the same fact.
            //
            // **Absent when there is nothing**, per 11.5. Announcing the absence
            // of a problem is not information.
            if (waiting > 0 || openIncidents > 0) {
                GroupedSurface {
                    if (openIncidents > 0) {
                        DenseRow(
                            title = strings("today.open.incidents", "count" to openIncidents),
                            leading = {
                                WaypointDot(color = colors.alert, state = Waypoint.OPEN)
                            },
                            chevron = true,
                            divider = waiting > 0,
                            onClick = onOpenIncidents,
                            modifier = Modifier.testTag(NotebookTags.OPEN_INCIDENTS),
                        )
                    }
                    if (waiting > 0) {
                        DenseRow(
                            title = strings("unfiled.waiting", "count" to waiting),
                            chevron = true,
                            divider = false,
                            onClick = onOpenUnfiled,
                            modifier = Modifier.testTag(NotebookTags.WAITING),
                        )
                    }
                }
            }

            // **Four named clusters, not a wall and not a fold.** The owner
            // watched a stranger face this screen and then faced it himself:
            // "a gigantic wall of options. there's no categories. nothing
            // makes sense. I don't know where to look." The group model below
            // had existed for months and the render only ever used it to
            // compute order: the headers were designed and never drawn. Now
            // they are drawn, which is rule 15 doing its actual job, and the
            // "More sections" fold is gone with the wall it managed: eight
            // rows behind an unnamed count was hiding, not hierarchy.
            //
            // **Grid screen 03 draws four lead sections and a fold.** This
            // departs from it, on the owner's live direction, 2026-08-16, and
            // D163 records the departure so D142 stays honest.
            //
            // **The order inside each group never changes**, so a person who
            // learned where documents live finds them in the same place, only
            // now under a name that says why they live there.
            for ((groupIndex, group) in NotebookGroup.entries.withIndex()) {
                val rows = group.sections.mapNotNull { bySection[it] }
                if (rows.isEmpty()) continue
                // **The headers arrive with their groups**, D168, so the four
                // clusters land in reading order rather than the whole screen
                // appearing at once.
                Box(modifier = Modifier.arrivesInOrder(groupIndex * 2)) {
                    GroupHeader(labelKey = group.labelKey)
                }
                // **No gap here, because the column already spaces its
                // children.** With `headerGap` on top of that the eyebrow sat
                // 30dp from the group it names and 40dp from the one above,
                // which is almost the same distance: on the phone it read as
                // floating between the two rather than belonging to either.
                // Rule 15 asks that what belongs together be grouped, and a
                // heading that is not attached to its own rows is the one
                // thing a heading has to get right.
                GroupedRows(items = rows) { row, isLast ->
                    SectionRow(row = row, isLast = isLast, onOpen = onOpen)
                }
                Spacer(Modifier.height(Space.sectionGap))
            }

            Spacer(Modifier.height(fabScrollClearance))
        }
    }
}



/**
 * One section, as a row in a grouped surface.
 *
 * **Its icon sits in its own section's wash**, per `DESIGN.md` 4.3, which is
 * what lets a person find documents by color before reading a word. The mapping
 * is the owner's and lives in `hueFor` alone.
 *
 * **The count is Mono and a count of zero reads as words.** "Nothing yet"
 * invites, where a column of zeros reads as a scorecard of what the person has
 * failed to fill in. One style at one weight for every section, so a row's
 * emphasis is never mistaken for a judgment about how full it is.
 */
@Composable
private fun SectionRow(
    row: SectionCount,
    isLast: Boolean,
    onOpen: (Repository.Section) -> Unit,
) {
    val strings = LocalStrings.current
    val hue = hueFor(row.section)
    // **Each section counts in its own units**, which is what the grid
    // draws: "9 people", "3 current", "1,630 entries". It said "N items"
    // for all twelve, and **"Money: 6 items" tells somebody nothing they
    // came for.** #347.
    //
    // **The key is derived from the section rather than listed here**, so a
    // section added later fails loudly at its missing key instead of
    // quietly falling back to a generic word. D133.
    val countKey = "notebook.count.${row.section.name.lowercase()}"

    DenseRow(
        title = strings[labelKey(row.section)],
        // **The count is the row's second line, not a trailing value.** The grid
        // draws it that way and it is right for a table of contents: "Care team,
        // 9 people" reads as one fact about one section, where a number pushed to
        // the far edge reads as a column in a table and invites comparing
        // sections against each other, which says nothing.
        subtitle = row.amount?.let { strings("notebook.count.money.unsettled", "amount" to it) }
            ?: strings(countKey, "count" to row.count),
        subtitleTestTag = NotebookTags.count(row.section),
        leading = {
            IconTile(
                section = row.section,
                tint = hue.ink,
                background = hue.wash,
                tileSize = SectionIconSize,
                iconSize = SectionDrawingSize,
            )
        },
        chevron = true,
        divider = !isLast,
        onClick = { onOpen(row.section) },
        modifier = Modifier.testTag(NotebookTags.row(row.section)),
    )
}

private val SectionIconSize = 32.dp
private val SectionDrawingSize = 18.dp

/**
 * One section, as a tile at one of three weights.
 *
 * **The filled drawing is what carries the hierarchy**, per D33 and 5.12, and
 * it was tried first as a difference in ink alone: at a glance twelve rows
 * still read as twelve identical rows, which is the defect this screen keeps
 * being rebuilt to fix. A filled tile against an unfilled one is visible
 * without reading anything, it costs the app no second accent, and it survives
 * a grayscale screenshot.
 */
@Composable
private fun SectionTile(
    row: SectionCount,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **Each section carries its own hue's icon in its own wash**, per
    // `DESIGN.md` 4.3: "its notebook row carries its icon in its wash". The
    // mapping is the owner's and lives in one place, [hueFor], so no screen
    // decides a section's identity for itself.
    //
    // This replaces the single `sand` fill the previous direction used. That
    // fill made twelve sections one shape in one color, which is the diagnosis
    // D71 recorded: the notebook was organization rather than hierarchy, and
    // nothing on it could be found by anything except reading.
    //
    // **The hue is identity, and emphasis is still carried by the fill**, which
    // is the rule D33 set and v4 does not change. A section the situation
    // template puts forward gets its wash filled. A standing section shows the
    // drawing on the bare surface. A folded one is quieter still. So a person
    // learns "documents are the manila ones" and the template can still say
    // which ones matter this week, without the two meanings colliding.
    val hue = hueFor(row.section)
    val tint = when (row.emphasis) {
        Emphasis.FOLDED -> colors.ink3
        else -> hue.ink
    }
    val fill = when (row.emphasis) {
        Emphasis.FORWARD -> hue.wash
        else -> Color.Transparent
    }

    // Composed from a message template so the plural is the catalog's problem
    // rather than a branch in this code, and so zero reads as words rather than
    // as a digit. One style, one color, at all three weights, so a tile's
    // emphasis is never mistaken for a judgment about how full it is.
    //
    // **The emergency card is one row and counting it says nothing.** Every
    // other section holds a list somebody adds to, so "9 items" is a fact about
    // their notebook. A card is a single thing, and "1 item" was the shape of
    // the table showing through onto the app's front door, which is rule 20
    // exactly. It says whether there is anything on it instead, and it does not
    // grade how much, per rule 13.
    // Same derivation as the row above, for the same reason. #347.
    val countKey = "notebook.count.${row.section.name.lowercase()}"

    Tile(
        label = strings[labelKey(row.section)],
        count = row.amount?.let { strings("notebook.count.money.unsettled", "amount" to it) }
            ?: strings(countKey, "count" to row.count),
        countTestTag = NotebookTags.count(row.section),
        compact = compact,
        onClick = onClick,
        modifier = modifier,
        icon = { tileSize, drawingSize ->
            IconTile(
                section = row.section,
                tint = tint,
                background = fill,
                tileSize = tileSize,
                iconSize = drawingSize,
            )
        },
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
