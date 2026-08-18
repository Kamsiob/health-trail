package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.CARD_SIZE
import com.kamsiob.healthtrail.ui.components.FILL
import com.kamsiob.healthtrail.ui.components.ROW_SIZE
import com.kamsiob.healthtrail.ui.components.ViewOption
import com.kamsiob.healthtrail.ui.components.ViewToggle
import com.kamsiob.healthtrail.ui.components.rememberViewChoice
import com.kamsiob.healthtrail.ui.components.Thumbnail
import com.kamsiob.healthtrail.ui.components.tileColumns
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.raisedCard
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider

object DocTags {
    const val NAME = "documents"
    const val ADD = "documents_add"
    const val TOGGLE = "documents_view"
    fun row(id: String) = "document_$id"
}

/**
 * Documents: photographs of the paperwork, and where each original is.
 *
 * **Where the paper physically is matters more than the photograph does.** The
 * schema says so in its own comment and it is the reason this section earns its
 * place: the digital copy is rarely the one a clerk will accept, so "signed and
 * handed back to the ward clerk, copy in the blue folder" is the line that
 * saves a phone call six weeks later.
 *
 * **A document with no photograph is a real document.** Knowing the discharge
 * summary exists and lives in the blue folder is worth writing down before
 * there is a picture of it.
 *
 * The thumbnail is decoded from the content addressed file on disk, at a size
 * that fits the card rather than at full resolution, because a list of full
 * size photographs is how a scrolling list runs out of memory.
 */
@Composable
fun DocumentsScreen(
    documents: List<Repository.Document>,
    /**
     * Opens the document itself.
     *
     * **The cell used to open the form that edits it**, which is the app
     * answering "show me this" with "change this" on the one section whose
     * whole point is holding the person's own paper.
     */
    onOpen: (Repository.Document) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val attachments = remember(context) { Attachments.open(context) }
    // **Three across, two above font scale 1.3, one above 1.8**, which is the
    // same table 11.2 gives the tile grid and for the same reason. At 2.0 a
    // third of the width holds about eight characters, and every caption on
    // this screen was silently clipped: "Insurance card, both" lost "sides"
    // and "June 25, 2026" lost its year. Found at 2.0 and nowhere else.
    val columns = tileColumns(compact = true)

    val view = rememberViewChoice(section = DocTags.NAME, fallback = VIEW_PICTURES)

    // **The two most recently received, as the one thing.** The grid calls them
    // the two the person reaches for most, and the app has no way to know that:
    // nothing here records what somebody opened, deliberately, and there is no
    // pin on a document. Rather than invent a signal, this shows what arrived
    // most recently, which is what the query already orders by and is honestly
    // what paperwork usually means. #221 asks the owner whether documents
    // should carry a pin like an entry does.
    val recent = documents.take(HERO_COUNT)
    val rest = documents.drop(HERO_COUNT)

    Page(
        title = strings["documents.heading"],
        onBack = onBack,
        backLabel = strings[LocalSectionBackKey.current],
        modifier = modifier.testTag(SectionTags.root(DocTags.NAME)),
        eyebrow = strings["notebook.section.documents"],
        subtitle = strings["docs.subtitle"],
        section = Repository.Section.DOCUMENTS,
    ) {
        if (documents.isEmpty()) {
            item {
                SectionEmpty(
                    name = DocTags.NAME,
                    text = strings["docs.empty"],
                    section = Repository.Section.DOCUMENTS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                )
                Spacer(Modifier.height(Space.l))
                Action(
                    label = strings["docs.photograph"],
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().testTag(DocTags.ADD), emphasis = ActionEmphasis.Main,
                )
            }
            return@Page
        }

        item(key = "toggle") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ViewToggle(
                    options = listOf(
                        ViewOption(VIEW_PICTURES, "docs.view.pictures"),
                        ViewOption(VIEW_LIST, "docs.view.list"),
                    ),
                    selected = view.value,
                    onSelect = view.onSelect,
                    modifier = Modifier.testTag(DocTags.TOGGLE),
                )
            }
        }

        // **Her actual papers, at a size somebody recognizes them at.** A
        // filename is a thing to read; a photograph of the insurance card is a
        // thing to spot. Two across, which is the largest a picture can be while
        // two of them still fit above the fold.
        item(key = "recent") {
            // **The view choice reaches these too.** They used to be cells
            // whatever the toggle said, and the toggle sits directly above
            // them: with every folder closed, which is how this screen opens,
            // choosing List changed the pill and nothing else on the screen.
            // **A control that does nothing visible reads as broken**, rule 16,
            // and it was worse than nothing here because the two documents it
            // was ignoring are the two somebody looked at most recently.
            if (view.value == VIEW_LIST) {
                Block(padding = Space.none) {
                    recent.forEachIndexed { index, document ->
                        DocumentRow(
                            document = document,
                            attachments = attachments,
                            divider = index < recent.size - 1,
                            onOpen = { onOpen(document) },
                        )
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
                return@item
            }
            // **One per row once the grid is down to one column.** At font scale
            // 2.0 the folded grids give way to a single column, per 11.2, and
            // the pair was still sitting two across: half a screen of picture
            // beside a caption wrapping to three lines. A person who needs large
            // text is not asking for a large picture, which is the reasoning
            // `DocumentCell` already carries, and it applies here too.
            if (columns == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
                    recent.forEach { document ->
                        DocumentCell(
                            document = document,
                            attachments = attachments,
                            stacked = false,
                            onOpen = { onOpen(document) },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    recent.forEach { document ->
                        DocumentCell(
                            document = document,
                            attachments = attachments,
                            stacked = true,
                            onOpen = { onOpen(document) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Keeps a single document at half width rather than letting
                    // it swell to fill the row, so the pair reads as a pair
                    // either way.
                    if (recent.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // **Folders as folds, named and counted.** The category is the person's
        // own word for a pile of paper, and anything they have not filed sits
        // under one heading at the end rather than being scattered.
        val folders = rest.groupBy { it.category?.takeIf { name -> name.isNotBlank() } }
            .toList()
            .sortedWith(compareBy({ it.first == null }, { it.first ?: "" }))

        for ((category, inFolder) in folders) {
            val label = category ?: strings["docs.other"]

            item(key = "folder_$label") {
                // **The folder is a label, not a fold.** D185: a name and a
                // count over the papers in it, and the scroll does the rest.
                // A folder that hides its contents behind a tap is a filing
                // cabinet somebody has to open twice.
                Eyebrow(
                    // The folder's name is usually the person's own word for
                    // it, so it keeps its case. No count: nothing is hidden,
                    // and no group label in any approved drawing carries one.
                    text = label,
                    fixed = false,
                )
            }

            if (view.value == VIEW_LIST) {
                item(key = "list_$label") {
                    Block(padding = Space.none) {
                        inFolder.forEachIndexed { index, document ->
                            DocumentRow(
                                document = document,
                                attachments = attachments,
                                divider = index < inFolder.size - 1,
                                onOpen = { onOpen(document) },
                            )
                        }
                    }
                }
            } else {
                inFolder.chunked(columns).forEachIndexed { rowIndex, row ->
                    item(key = "grid_${label}_$rowIndex") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            row.forEach { document ->
                                DocumentCell(
                                    document = document,
                                    attachments = attachments,
                                    stacked = columns > 1,
                                    onOpen = { onOpen(document) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        item(key = "add") {
            Spacer(Modifier.height(Space.m))
            // **The camera is the filled action**, because photographing is how
            // paper gets in here at all. Everything else on this screen is
            // something the person already has.
            Action(
                label = strings["docs.photograph"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(DocTags.ADD), emphasis = ActionEmphasis.Main,
            )
        }
    }
}

/** How many documents lead the screen. Two, which is what the grid draws. */
private const val HERO_COUNT = 2

const val VIEW_PICTURES = "pictures"
const val VIEW_LIST = "list"

/**
 * One document as a dense row, for the compact view.
 *
 * **The thumbnail stays.** The compact view is about fitting more on a screen,
 * not about turning the person's paper back into filenames, which is the thing
 * this section exists to stop being.
 */
@Composable
private fun DocumentRow(
    document: Repository.Document,
    attachments: Attachments,
    divider: Boolean,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    Column {
        ListRow(
            title = Bidi.isolate(document.title),
            support = document.originalLocation?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) },
            leading = {
                Thumbnail(
                    sha256 = document.sha256,
                    attachments = attachments,
                    section = Repository.Section.DOCUMENTS,
                    size = ROW_SIZE,
                )
            },
            value = document.receivedEdtf?.takeIf { it.isNotBlank() }
                ?.let { EventDateText.render(strings, it) },
            onClick = onOpen,
            clickLabel = strings["open.action"],
            modifier = Modifier.testTag(DocTags.row(document.id)),
        )
        if (divider) RowDivider()
    }
}

/**
 * One document in the gallery: its own paper, and enough words to find it by.
 *
 * **The caption is not optional**, per 11.7. A thumbnail is never the only
 * thing naming its item, both because a photograph of a letter is hard to tell
 * from another photograph of a letter at 112dp, and because a reader user has
 * nothing else.
 *
 * **Where the paper physically is stays on the card**, because the schema's own
 * comment says that matters more than the photograph does: the digital copy is
 * rarely the one a clerk will accept.
 */
@Composable
private fun DocumentCell(
    document: Repository.Document,
    attachments: Attachments,
    /**
     * True in a grid, where the picture sits above its caption. False at one
     * column, where it sits beside it.
     *
     * **A single column is not a very wide gallery.** At font scale 2.0 the
     * grid gives way to one column, per 11.2's table, and a square thumbnail
     * across the full width is one document per screenful: a person who needs
     * large text is not asking for a large picture. At one column the cell
     * becomes the dense row shape 11.3 and 11.7 already describe, a 64dp
     * thumbnail leading its words. Found at 2.0 by looking at it.
     */
    stacked: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    val caption: @Composable ColumnScope.() -> Unit = {
        // **Isolated, because these are the person's own words inside a layout
        // that has a direction.** A Latin title in an Arabic layout is a run
        // going the other way, and without an isolate it is laid out against
        // the surrounding direction rather than within its own box. Seen in
        // Arabic, where the last character of a caption sat on the cell's edge.
        Text(
            text = Bidi.isolate(document.title),
            style = HealthTrail.type.bodyM,
            color = colors.ink,
        )
        document.receivedEdtf?.takeIf { it.isNotBlank() }?.let { received ->
            Text(
                text = EventDateText.render(strings, received),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
        document.originalLocation?.takeIf { it.isNotBlank() }?.let { where ->
            Text(
                text = Bidi.isolate(where),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }

    // **The cell opens the document and does nothing else**, per #218. It used
    // to carry removal on a long press, which was the only way to remove a
    // document and could not be found by anybody who did not already know it
    // was there. Removal is on the document's own screen now.
    //
    // **The focus ring follows this clip**, which is the tile radius here
    // rather than the card's.
    val body = modifier
        .semantics(mergeDescendants = true) { }
        .clip(Radius.tile)
        .openableByTap(
            label = strings["open.action"],
            onTap = onOpen,
            resting = Color.Transparent,
            shape = Radius.tile,
        )
        .testTag(DocTags.row(document.id))

    if (stacked) {
        Column(modifier = body) {
            // **The picture sits on the person's own paper**, which is what
            // `m3v4-5` draws and what D190 reserves white for: a photograph of
            // their letter is theirs, and it is the one thing in this app that
            // casts a shadow. The pictures view drew it bare on the canvas, so
            // a wall of gray placeholders read as a page that had not loaded.
            // Seen on the phone, rule 21.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedCard(Radius.thumbnail)
                    .clip(Radius.thumbnail)
                    .background(colors.card),
            ) {
                Thumbnail(
                    sha256 = document.sha256,
                    attachments = attachments,
                    section = Repository.Section.DOCUMENTS,
                    size = FILL,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Space.s))
            // **An optical inset against the cell's own clip.** A caption whose
            // line lands exactly on the cell width had its last letter shaved
            // off by the rounded clip above: "the county" became "the countv"
            // and "the original is" lost its s. Text is allowed to paint a
            // fraction outside its layout box, and an ancestor clip takes that
            // fraction with it.
            //
            // Found in Arabic and then found to have been true in English all
            // along, which is the second time that has happened on this screen.
            // A shaved letter is a truncation, and rule 11 rules those out.
            Column(modifier = Modifier.padding(horizontal = Space.xs), content = caption)
        }
    } else {
        Row(modifier = body, verticalAlignment = Alignment.Top) {
            Thumbnail(
                sha256 = document.sha256,
                attachments = attachments,
                section = Repository.Section.DOCUMENTS,
                size = CARD_SIZE,
            )
            Spacer(Modifier.width(Space.sm))
            Column(modifier = Modifier.weight(1f), content = caption)
        }
    }
}

// **No `maxLines` anywhere in the caption**, which the first build had.
// `maxLines` without an overflow treatment clips silently, and a clipped
// caption is a truncation, which rule 11 rules out. A long title makes its own
// cell taller and the row grows with it, which is ragged and complete rather
// than tidy and wrong.

