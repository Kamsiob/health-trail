package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.Thumbnail
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectPaperworkTags {
    const val NAME = "project-paperwork"
    const val FILTERS = "project-paperwork-filters"
    const val VIEW = "project-paperwork-view"
    fun filter(key: String) = "project-paperwork-filter-$key"
    fun paper(id: String) = "project-paperwork-$id"
}

private const val ALL = "all"

/** A place whose direction nobody has said, which is an ordinary state. */
private const val UNSAID = "unsaid"
private const val TILE = 104

/**
 * The papers of one project. `DESIGN.md` 20.5, screen 13.
 *
 * **The one distinction that matters in any process: what they sent and what
 * you sent.** It is the question a person is actually asking when they open
 * this, because the two have completely different consequences, and the schema
 * has carried `project_paper.direction` for it the whole time.
 *
 * **A placeholder is a place, not a paper**, 20.4 and rule 13. An empty one says
 * "Waiting" and **nothing here counts the empty ones or chases them**: a project
 * with six placeholders and two filled is somebody waiting on other people's
 * post, and a completion count pointed at that is exactly what rule 13 rules
 * out. The count in the header is how many places there are.
 *
 * **Every filled place is a door to the document itself**, and the document
 * already knows where it came from, so rule 18 holds both ways.
 *
 * **Nothing on this screen removes or empties anything.** Those live on the
 * setup screen's papers editor, which is the screen that owns the places. This
 * one is for looking at the paper.
 */
@Composable
fun ProjectPaperworkScreen(
    projectName: String,
    papers: List<Repository.ProjectPaperCard>,
    onOpenDocument: (String) -> Unit,
    /**
     * Fills an empty place with a paper. #379, the owner: "I can't add a
     * document to one of the existing spots that a project template
     * recommended."
     *
     * **`fillProjectPaper` has been in the repository with no caller**, so the
     * template could suggest a place and nothing could ever be put in it. An
     * empty slot that answers no tap is the dead end rule 18 names.
     */
    onFillPaper: (Repository.ProjectPaper) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    // Opened here rather than threaded through the shell, exactly as the
    // documents gallery does it: the store is a directory and a hash, not
    // state, and two screens opening it is not two of anything.
    val context = LocalContext.current
    val attachments = remember(context) { Attachments.open(context) }

    // **Only the directions this project actually uses.** A template that never
    // sends anything would otherwise draw a chip with nothing behind it, D42.
    val counts = remember(papers) {
        papers.groupingBy { it.paper.direction ?: UNSAID }.eachCount()
    }
    var filter by rememberSaveable(projectName) { mutableStateOf(ALL) }
    val active = if (filter == ALL || counts.containsKey(filter)) filter else ALL
    val shown = remember(papers, active) {
        if (active == ALL) papers else papers.filter { (it.paper.direction ?: UNSAID) == active }
    }

    // **Remembered for as long as this build can remember anything.** The grid
    // asks for the toggle to persist; view preferences have no table and that
    // is #222, blocked on an owner decision, so this survives rotation and the
    // back stack and not a restart. Storing it anywhere else would be inventing
    // the table that issue is about.
    var grid by rememberSaveable(projectName) { mutableStateOf(true) }

    SectionScaffold(
        name = ProjectPaperworkTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.paperwork",
        subtitle = Bidi.isolate(projectName),
        onBack = onBack,
        backLabelKey = "section.back.project",
        modifier = modifier,
    ) {
        if (papers.isEmpty()) {
            item {
                SectionEmpty(
                    name = ProjectPaperworkTags.NAME,
                    lead = strings["project.paperwork.empty.lead"],
                    text = strings["project.paperwork.none"],
                    section = Repository.Section.DOCUMENTS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_TALL),
                )
            }
            return@SectionScaffold
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // **How many places there are**, never how many are filled.
                    text = strings("project.paperwork.count", "count" to papers.size),
                    style = type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.weight(1f),
                )
                ChoiceChip(
                    label = strings[if (grid) "project.paperwork.view.list" else "project.paperwork.view.grid"],
                    selected = false,
                    onClick = { grid = !grid },
                    modifier = Modifier.testTag(ProjectPaperworkTags.VIEW),
                )
            }
            Spacer(Modifier.height(Space.m))
        }

        if (counts.keys.count { it != UNSAID } > 1) {
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag(ProjectPaperworkTags.FILTERS),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    ChoiceChip(
                        label = strings["project.paperwork.filter.all"],
                        selected = active == ALL,
                        onClick = { filter = ALL },
                        modifier = Modifier.testTag(ProjectPaperworkTags.filter(ALL)),
                    )
                    // **Only the two the grid draws.** A place nobody has said
                    // a direction about is still here under All; giving it a
                    // chip of its own would put a third filter on screen to
                    // select the papers the person has said the least about.
                    for (key in counts.keys.filter { it != UNSAID }
                        .sortedBy { directionOrder(it) }) {
                        ChoiceChip(
                            label = strings[directionLabelKey(key)],
                            selected = active == key,
                            onClick = { filter = key },
                            modifier = Modifier.testTag(ProjectPaperworkTags.filter(key)),
                        )
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        if (grid) {
            // **Three across**, the same as the documents gallery, so the
            // person's own paper looks the same wherever they meet it.
            val rows = shown.chunked(3)
            items(rows.size) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    for (card in rows[rowIndex]) {
                        PaperTile(
                            card = card,
                            attachments = attachments,
                            onOpen = card.documentId
                                ?.let { id -> { onOpenDocument(id) } }
                                ?: { onFillPaper(card.paper) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Keeps the last row's tiles the width of every other
                    // row's rather than stretching two across the screen.
                    repeat(3 - rows[rowIndex].size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(Space.m))
            }
        } else {
            item {
                GroupedSurface {
                    shown.forEachIndexed { index, card ->
                        DenseRow(
                            title = Bidi.isolate(card.paper.name),
                            subtitle = subtitleFor(card),
                            subtitleMaxLines = 2,
                            chevron = true,
                            // **An empty place offers to be filled**, rather
                            // than sitting inert. #379.
                            onClick = card.documentId
                                ?.let { id -> { onOpenDocument(id) } }
                                ?: { onFillPaper(card.paper) },
                            divider = index < shown.lastIndex,
                            leading = {
                                Thumbnail(
                                    sha256 = card.sha256,
                                    attachments = attachments,
                                    section = Repository.Section.DOCUMENTS,
                                )
                            },
                            modifier = Modifier.testTag(ProjectPaperworkTags.paper(card.paper.id)),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One place, drawn as the paper in it.
 *
 * **An empty place is drawn at the same size as a full one.** It is a place
 * that is waiting, and shrinking it or graying it out would make the screen say
 * something about the person's progress that rule 13 rules out.
 */
@Composable
private fun PaperTile(
    card: Repository.ProjectPaperCard,
    attachments: Attachments?,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Column(
        modifier = modifier
            .clip(Radius.card)
            .then(
                if (onOpen != null) {
                    Modifier.openableByTap(
                        label = strings["project.paperwork.open_document"],
                        onTap = onOpen,
                        // **Transparent at rest, so a filled place and an empty
                        // one are the same object in two states.** The default
                        // is the card surface, which gave every filled tile a
                        // raised white panel the empty ones did not have: on
                        // the phone that read as two different components, and
                        // it made the filled state look like the designed one
                        // and "Waiting" like an afterthought, which is the
                        // opposite of what 20.4 says a placeholder is.
                        resting = Color.Transparent,
                    )
                } else {
                    Modifier
                },
            )
            .testTag(ProjectPaperworkTags.paper(card.paper.id))
            .padding(Space.xs),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Thumbnail(
                sha256 = card.sha256,
                attachments = attachments,
                section = Repository.Section.DOCUMENTS,
                size = TILE.dp,
            )
        }
        Spacer(Modifier.height(Space.xs))
        // **Neither line is capped.** At font scale 2.0 a third of the width
        // holds about eight characters of mono, so two lines cut "May 12, 2026"
        // to "May 12," and rule 11 rules truncation out. An uneven tile is the
        // honest shape: nothing is dropped and nothing is abbreviated, which is
        // the same call the road strip makes when a stage name will not fit.
        Text(
            text = Bidi.isolate(card.paper.name),
            style = type.bodyS,
            color = colors.ink,
        )
        Text(
            text = subtitleFor(card, compact = true),
            style = type.mono,
            color = colors.ink2,
        )
    }
}

/**
 * What the second line says: which way the paper went, and when.
 *
 * **"Waiting" and never "missing"**, 20.4. An empty place is a place, and the
 * post it is waiting for is somebody else's to send.
 */
@Composable
private fun subtitleFor(
    card: Repository.ProjectPaperCard,
    /** A tile is a third of the width, so it says less and shows the paper. */
    compact: Boolean = false,
): String {
    val strings = LocalStrings.current
    val direction = card.paper.direction?.let { strings[directionLabelKey(it)] }
    return if (card.documentId == null) {
        Bidi.join(direction, strings["project.papers.waiting"])
    } else {
        Bidi.join(
            direction,
            // **What actually got filed here, when it is not called the same
            // thing as the place.** The title used to be the document's, which
            // hid the name the person gave the slot: somebody looking for
            // "Proof of income" found a tile called "Discharge summary" and had
            // no way to tell it was the same place. The place leads, and what
            // is in it says so underneath.
            //
            // **Not on a tile.** Three parts on a third of the width ran to
            // three lines and the year came off the end, which rule 11 rules
            // out. The photograph is already the answer to what is in it there,
            // which is what the grid draws.
            card.title?.takeIf { !compact && it.isNotBlank() && it != card.paper.name },
            card.receivedEdtf?.let { EventDateText.render(strings, it) },
        )
    }
}

/** The two directions the schema allows, plus the places nobody has said about. */
private fun directionLabelKey(key: String): String = when (key) {
    "received" -> "project.paperwork.filter.received"
    "sent" -> "project.paperwork.filter.sent"
    else -> "project.paperwork.filter.unsaid"
}

private fun directionOrder(key: String): Int = when (key) {
    "received" -> 0
    "sent" -> 1
    else -> 2
}
