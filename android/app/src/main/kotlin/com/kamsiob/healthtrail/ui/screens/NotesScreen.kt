package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RichText
import com.kamsiob.healthtrail.ui.v4.Segments
import com.kamsiob.healthtrail.ui.v4.rememberViewChoice

object NotesTags {
    const val NAME = "notes"
    const val ADD = "notes_add"
    const val TOGGLE = "notes_view"
    const val PINNED = "notes_pinned"
    fun note(id: String) = "notes_note_$id"
    fun pin(id: String) = "notes_pin_$id"
    fun remove(id: String) = "notes_remove_$id"
}

/**
 * The grid, which is this screen's other way of looking.
 *
 * **`VIEW_LIST` is the app's own and is not declared twice.** The documents
 * screen already named it, and a second constant with the same value is how two
 * screens come to disagree about what a stored preference means.
 */
private const val VIEW_GRID = "grid"

/** How much of a note a card shows before the rest waits inside it. */
private const val CARD_LINES = 6
private const val ROW_LINES = 2

/**
 * Every note, as a grid or as a list. #397.
 *
 * **The owner, 2026-08-18: "for viewing notes, they should be able to see the
 * notes as a grid or as a list with options to pin or delete both from the
 * notes list and within a specific note."**
 *
 * **The toggle is the one this app already has**, `rememberViewChoice`, which
 * the documents screen uses for exactly the same question and which remembers
 * the answer per section, because a toggle that forgets is a tax for having a
 * preference at all.
 *
 * **A note is an entry**, D207, so this screen is a lens on the trail rather
 * than a second record. Nothing here is stored that is not stored there.
 *
 * **The marks are drawn, never shown.** `RichText` turns the three marks into
 * styles for the eye and strips them for anybody listening, so a card never
 * reads "star star bring her glasses star star" aloud.
 *
 * **Kept in view is `entry.pinned_at`**, which already means exactly that
 * everywhere else in the app, and it is the person's own decision rather than
 * anything the app worked out. Rule 13: it is not a score and nothing counts
 * how many are pinned.
 */
@Composable
fun NotesScreen(
    notes: List<Repository.TrailEntry>,
    onOpen: (Repository.TrailEntry) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Keeps one in view, or stops. The caller writes it; this only asks. */
    onPin: (Repository.TrailEntry, Boolean) -> Unit = { _, _ -> },
    /** Opens the confirmation that takes one out. Never removes anything itself. */
    onRemove: (Repository.TrailEntry) -> Unit = {},
) {
    val strings = LocalStrings.current
    val view = rememberViewChoice(section = NotesTags.NAME, fallback = VIEW_GRID)

    // **Kept in view first, then by when they happened.** The pin is the
    // person's own answer to "which of these do I need in front of me", so it
    // outranks the date, and everything else stays in the order the trail uses.
    val pinned = notes.filter { it.pinnedAt != null }
    val rest = notes.filter { it.pinnedAt == null }

    Page(
        title = strings["notes.heading"],
        onBack = onBack,
        backLabel = strings[LocalSectionBackKey.current],
        modifier = modifier.testTag(SectionTags.root(NotesTags.NAME)),
        eyebrow = strings["notebook.section.notes"],
        subtitle = strings["notes.subtitle"],
        section = Repository.Section.NOTES,
        // **Null while the screen is empty**, D200, because the empty block
        // carries the verb itself and two ways to do one thing on a screen with
        // nothing on it is the same control twice.
        fab = if (notes.isEmpty()) {
            null
        } else {
            {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = {
                        Icon(painter = painterResource(Symbols.add), contentDescription = null)
                    },
                    text = { Text(text = strings["note.title"]) },
                    modifier = Modifier
                        .testTag(NotesTags.ADD)
                        .semantics { contentDescription = strings["note.title"] },
                )
            }
        },
    ) {
        if (notes.isEmpty()) {
            item {
                SectionEmpty(
                    name = NotesTags.NAME,
                    lead = strings["notes.empty.lead"],
                    text = strings["notes.empty"],
                    section = Repository.Section.NOTES,
                    actionLabel = strings["note.title"],
                    onAction = onAdd,
                )
            }
            return@Page
        }

        item {
            val views = listOf(VIEW_GRID, VIEW_LIST)
            Segments(
                options = listOf(strings["notes.view.grid"], strings["notes.view.list"]),
                selected = views.indexOf(view.value).coerceAtLeast(0),
                onSelect = { view.onSelect(views[it]) },
                modifier = Modifier.testTag(NotesTags.TOGGLE),
            )
            Spacer(Modifier.height(Space.betweenGroups))
        }

        if (pinned.isNotEmpty()) {
            item {
                Eyebrow(
                    text = strings["notes.pinned"],
                    modifier = Modifier.testTag(NotesTags.PINNED),
                )
                Spacer(Modifier.height(Space.withinGroup))
            }
            notesIn(view.value, pinned, onOpen, onPin, onRemove)
            item { Spacer(Modifier.height(Space.betweenGroups)) }
        }

        notesIn(view.value, rest, onOpen, onPin, onRemove)
    }
}

/**
 * The notes themselves, in whichever shape was chosen.
 *
 * **Two per row in the grid, one in the list**, and the card is the same card
 * either way: a note is a note, and drawing it two different ways would make
 * the toggle a change of subject rather than a change of density. Rule 22's
 * point exactly.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.notesIn(
    view: String,
    notes: List<Repository.TrailEntry>,
    onOpen: (Repository.TrailEntry) -> Unit,
    onPin: (Repository.TrailEntry, Boolean) -> Unit,
    onRemove: (Repository.TrailEntry) -> Unit,
) {
    if (view == VIEW_GRID) {
        // **Rows of two, laid out here rather than in a nested lazy grid.** A
        // scrolling grid inside a scrolling column is the layout that measures
        // to zero, `docs/TRAPS.md`, and this list is tens of notes rather than
        // thousands.
        for (pair in notes.chunked(2)) {
            item(key = pair.first().id) {
                // **One height across the row, measured rather than assumed.**
                // A `Row` lets each child be as tall as its own content, so two
                // notes of different lengths ended at different depths and the
                // grid read as ragged. `IntrinsicSize.Min` measures the taller
                // and both fill it, which is the same fix the project's verbs
                // and its file tiles already needed.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    for (note in pair) {
                        NoteCard(
                            note = note,
                            lines = CARD_LINES,
                            onOpen = { onOpen(note) },
                            onPin = { onPin(note, note.pinnedAt == null) },
                            onRemove = { onRemove(note) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    // **A last odd note keeps the card's width.** Stretching it
                    // across the row would make one note look like a different
                    // kind of thing from the note above it.
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(Space.s))
            }
        }
    } else {
        for (note in notes) {
            item(key = note.id) {
                NoteCard(
                    note = note,
                    lines = ROW_LINES,
                    onOpen = { onOpen(note) },
                    onPin = { onPin(note, note.pinnedAt == null) },
                    onRemove = { onRemove(note) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Space.s))
            }
        }
    }
}

/**
 * One note, with the two things somebody can do to it from here.
 *
 * **The whole card opens it and the two marks do their own jobs**, which needs
 * the card's own tap and the buttons' taps to be different nodes. `docs/TRAPS.md`
 * warns about the other half of this: the sentence a reader hears belongs on the
 * node that takes the tap.
 *
 * **Truncated on purpose and never mid-word-of-a-mark.** `RichText.annotated`
 * has already turned the marks into styles, so what is cut is words rather than
 * asterisks, and the rest is one tap away where the screen has room. Rule 11
 * forbids truncation that loses something with no way back; this has one.
 */
@Composable
private fun NoteCard(
    note: Repository.TrailEntry,
    lines: Int,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = goldHue()
    // bidi-ok: the stored string itself, which `RichText` reads for marks. An
    // isolation mark inside it would be a character the parser has to know
    // about, and 8.8.1 says the column is the record.
    val body = note.body.orEmpty()
    val heading = note.title?.takeIf { it.isNotBlank() } ?: strings["notes.untitled"]
    val kept = note.pinnedAt != null

    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
        modifier = modifier
            .testTag(NotesTags.note(note.id))
            .semantics(mergeDescendants = true) {
                // **The words, never the marks.** A reader hears the note.
                contentDescription = Bidi.join(
                    heading,
                    RichText.plain(body),
                    EventDateText.render(strings, note.occurredEdtf),
                )
            },
    ) {
        Column(modifier = Modifier.padding(Space.cardPadding)) {
            // **The name gets the whole width.** Looked at on the phone: with
            // the two marks beside it in a two column grid, "The long one" came
            // out as "The long o..." and "Care plan meeting" as "Care plan
            // m...". A name cut after eight characters is not a name. The marks
            // moved to the foot of the card, where they have room and where the
            // eye is not looking for the heading.
            Text(
                text = Bidi.isolate(heading),
                style = HealthTrail.type.rowTitle,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (body.isNotBlank()) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    // **Drawn, never shown.** `annotated` turns the three marks
                    // into styles, so what is cut short is words rather than
                    // asterisks, and the rest is one tap away.
                    text = RichText.annotated(body),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    maxLines = lines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            // **The date and the two marks share the foot**, which also gives
            // every card one row of controls at a fixed height, so two cards in
            // a grid row end level with each other.
            // **The date on its own line, above the marks.** Looked at on the
            // phone: sharing a row with two buttons in a two column grid cut it
            // to "August 1..." and "March 6,...", and a date somebody cannot
            // read is not a date. It costs one line and never truncates.
            Spacer(Modifier.height(Space.s))
            Text(
                // bidi-ok: a date the app formatted from a stored value.
                text = EventDateText.render(strings, note.occurredEdtf),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPin,
                    modifier = Modifier
                        .testTag(NotesTags.pin(note.id))
                        .semantics {
                            contentDescription =
                                strings[if (kept) "notes.unpin" else "notes.pin"]
                        },
                ) {
                    Icon(
                        painter = painterResource(Symbols.pin),
                        contentDescription = null,
                        // **The one place a color says state on this screen**,
                        // and it says the person's own decision rather than
                        // anything the app worked out. The word beside it in
                        // the reader's sentence carries the same fact, so the
                        // color is never alone. D198 item 6.
                        tint = if (kept) hue.base else colors.ink2,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .testTag(NotesTags.remove(note.id))
                        .semantics { contentDescription = strings["remove.action"] },
                ) {
                    Icon(
                        painter = painterResource(Symbols.bin),
                        contentDescription = null,
                        tint = colors.ink2,
                    )
                }
            }
        }
    }
}
