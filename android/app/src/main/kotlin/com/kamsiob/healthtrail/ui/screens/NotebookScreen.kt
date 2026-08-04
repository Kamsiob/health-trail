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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.Hero
import com.kamsiob.healthtrail.ui.components.HeroLine
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.Tile
import com.kamsiob.healthtrail.ui.components.tileColumns
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object NotebookTags {
    const val ROOT = "notebook_root"
    fun section(section: Repository.Section) = "notebook_section_${section.name.lowercase()}"
    fun count(section: Repository.Section) = "notebook_count_${section.name.lowercase()}"
    const val WAITING = "notebook_waiting"
    const val OPEN_INCIDENTS = "notebook_open_incidents"
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
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val bySection = sections.associateBy { it.section }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.paper,
    ) {
        Column(
            // **A plain scrolling column rather than a lazy list**, which is
            // not a regression. Twelve tiles and a hero is a fixed, small
            // screen: laziness buys nothing here and costs the one thing that
            // matters, which is that every tile exists in the tree so a test
            // and a screen reader both reach it without a scroll dance. The
            // trail, which is sixteen hundred rows, is still lazy.
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(NotebookTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
        ) {
            Spacer(Modifier.height(Space.l))
            Text(
                text = strings["notebook.title"],
                style = HealthTrail.type.displayL,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["notebook.subtitle"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            // **The hero, and it is absent more often than it is present**, per
            // 11.5. A notebook with nothing waiting and no open incident has
            // nothing that needs the person right now, and saying so at display
            // size would be an announcement about the absence of a problem.
            // The grid simply starts higher.
            //
            // **Two lines at most and never three.** Two things needing
            // attention is a fact about a notebook; three at display size is a
            // screen shouting, and the rest are in their own sections where
            // they already live.
            if (waiting > 0 || openIncidents > 0) {
                Hero(eyebrowKey = "notebook.attention") {
                    if (waiting > 0) {
                        HeroLine(
                            text = strings("unfiled.waiting", "count" to waiting),
                            onClick = onOpenUnfiled,
                            modifier = Modifier.testTag(NotebookTags.WAITING),
                        )
                    }
                    if (openIncidents > 0) {
                        HeroLine(
                            text = strings("today.open.incidents", "count" to openIncidents),
                            onClick = onOpenIncidents,
                            modifier = Modifier.testTag(NotebookTags.OPEN_INCIDENTS),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(Space.l))
            }

            // **One grid, and the four group headers are gone.**
            //
            // **Nothing moved.** The order is still `NotebookGroup`'s, which is
            // still `MASTER_SPEC.md` section 4.4's, so a person who learned
            // where documents were finds them in the same place. That is what
            // 10.8 actually protects and it is untouched.
            //
            // **What went is the headers, and they were a fix for a list.**
            // Twelve rows at uniform weight read as a list of everything, so
            // #36 grouped them, correctly. A grid is told apart by shape and
            // position instead, which is the job the headers were doing, and
            // four headers chopping twelve tiles into groups of three, four,
            // three and two left a half empty row in three of the four and cost
            // most of what the grid was worth: seven rows and four headers
            // instead of six rows, which is barely shorter than the list it
            // replaced. Measured on the phone against the year five fixture
            // rather than reasoned about.
            //
            // **Two grids, because the template's three weights are two tile
            // sizes plus one fill rule**, per 11.2. Forward and standing are
            // the same size and differ by the fill on their drawing; folded is
            // compact and sits after them, still in its own place in the order,
            // still one tap away. Folded means quieter, never gone.
            val ordered = NotebookGroup.entries.flatMap { group ->
                group.sections.mapNotNull { bySection[it] }
            }
            val full = ordered.filter { it.emphasis != Emphasis.FOLDED }
            val folded = ordered.filter { it.emphasis == Emphasis.FOLDED }

            TileGrid(rows = full, compact = false, onOpen = onOpen)
            if (full.isNotEmpty() && folded.isNotEmpty()) {
                Spacer(Modifier.height(Space.s))
            }
            TileGrid(rows = folded, compact = true, onOpen = onOpen)

            // Clearance for the capture button, which overlaps the navigation
            // bar's top edge by 16dp and would otherwise sit on top of the last
            // tile. 56dp button, 16dp overhang, plus a gap so the last row is
            // readable rather than merely uncovered.
            Spacer(Modifier.height(Space.xxl + Space.l))
        }
    }
}

/**
 * A run of sections, as a grid.
 *
 * The row is measured to its tallest tile, so a two line name never leaves its
 * neighbor short, and a row that is not full keeps its columns rather than
 * letting the last tile stretch to the width of two, which would read as one
 * section being the important one.
 */
@Composable
private fun TileGrid(
    rows: List<SectionCount>,
    compact: Boolean,
    onOpen: (Repository.Section) -> Unit,
) {
    if (rows.isEmpty()) return
    val columns = tileColumns(compact)

    rows.chunked(columns).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            row.forEach { entry ->
                SectionTile(
                    row = entry,
                    compact = compact,
                    onClick = { onOpen(entry.section) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(NotebookTags.section(entry.section)),
                )
            }
            // **A short last row shares the full width rather than leaving a
            // hole.** Holding the column left a half tile of empty paper beside
            // the last section of a group, which reads as a missing tile rather
            // than as the end of a row. A wider tile is the same tile: same
            // height, same drawing, same fill, and the width is the grid's to
            // set. This is why there is no spacer here.
        }
        Spacer(Modifier.height(Space.cardGap))
    }
}

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

    // The emergency card keeps the alert tone at every weight. Section 2.2
    // gives `alert` to this one section, and the reference file draws its row
    // that way in screen 04.
    val emergency = row.section == Repository.Section.EMERGENCY_CARD
    val tint = when {
        emergency -> colors.alertInk
        row.emphasis == Emphasis.FORWARD -> colors.ink
        row.emphasis == Emphasis.FOLDED -> colors.ink3
        else -> colors.ink2
    }
    val fill = when {
        row.emphasis == Emphasis.FOLDED -> Color.Transparent
        emergency -> colors.alertWash
        row.emphasis == Emphasis.FORWARD -> colors.sand
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
    val countKey = if (emergency) "notebook.count.emergency_card" else "notebook.count"

    Tile(
        label = strings[labelKey(row.section)],
        count = strings(countKey, "count" to row.count),
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
