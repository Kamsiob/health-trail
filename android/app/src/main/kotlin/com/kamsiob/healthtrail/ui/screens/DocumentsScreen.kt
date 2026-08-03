package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.CARD_SIZE
import com.kamsiob.healthtrail.ui.components.FILL
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.Thumbnail
import com.kamsiob.healthtrail.ui.components.tileColumns
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object DocTags {
    const val NAME = "documents"
    const val ADD = "documents_add"
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
    onRemove: (Repository.Document) -> Unit,
    onEdit: (Repository.Document) -> Unit,
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

    SectionScaffold(
        name = DocTags.NAME,
        title = strings["notebook.section.documents"],
        subtitle = strings["docs.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (documents.isEmpty()) {
            item {
                SectionEmpty(name = DocTags.NAME, text = strings["docs.empty"], section = Repository.Section.DOCUMENTS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        // **A gallery, per 11.12, and it was a column of cards each carrying a
        // 180dp photograph.** This app stores photographs of the person's own
        // paper, and one per screenful is a list of documents with pictures
        // attached rather than a place to find a letter by recognizing it.
        //
        // **Grouped by year**, which is the coarsest thing every date in this
        // app is guaranteed to have. A month heading would need the date
        // rendered at month precision, and plenty of these are known only to
        // the year, so grouping by month would either invent precision or
        // scatter half the documents into their own headings.
        documents.groupBy { it.receivedEdtf?.takeIf { edtf -> edtf.isNotBlank() }?.take(4) }
            .forEach { (year, inYear) ->
                item(key = "year_${year ?: "none"}") {
                    Spacer(Modifier.height(Space.s))
                    GroupHeaderText(label = year ?: strings["date.unknown"])
                    Spacer(Modifier.height(Space.headerGap))
                }

                inYear.chunked(columns).forEachIndexed { rowIndex, row ->
                    item(key = "row_${year ?: "none"}_$rowIndex") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            row.forEach { document ->
                                DocumentCell(
                                    document = document,
                                    attachments = attachments,
                                    stacked = columns > 1,
                                    onRemove = { onRemove(document) },
                                    onEdit = { onEdit(document) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // A short last row keeps its columns here, unlike a
                            // tile grid: a photograph stretched to three times
                            // its neighbors would read as the important one,
                            // and this app has no view about which document
                            // matters most.
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(Space.sm))
                    }
                }
            }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["docs.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(DocTags.ADD),
            )
            Spacer(Modifier.height(Space.l))
        }
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
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    val caption: @Composable ColumnScope.() -> Unit = {
        Text(
            text = document.title,
            style = HealthTrail.type.bodyM,
            color = colors.ink,
        )
        document.receivedEdtf?.takeIf { it.isNotBlank() }?.let { received ->
            Text(
                text = EventDateText.render(strings, received),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
        }
        document.originalLocation?.takeIf { it.isNotBlank() }?.let { where ->
            Text(
                text = where,
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }

    val body = modifier
        .semantics(mergeDescendants = true) { }
        .clip(Radius.tile)
        .removableByLongPress(strings["edit.hint"], onRemove, onEdit)
        .testTag(DocTags.row(document.id))

    if (stacked) {
        Column(modifier = body) {
            Thumbnail(
                sha256 = document.sha256,
                attachments = attachments,
                section = Repository.Section.DOCUMENTS,
                size = FILL,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xs))
            caption()
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

