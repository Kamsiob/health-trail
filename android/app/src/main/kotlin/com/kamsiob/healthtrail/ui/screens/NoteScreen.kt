package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.Field
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import androidx.compose.ui.Alignment
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RichText

/**
 * How tall the note's own field stands before anything is typed.
 *
 * Enough to read as a page rather than a name field, and not so tall that the
 * three marks below it fall under the fold on a small phone.
 */
private const val NOTE_LINES = 5

object NoteTags {
    const val NAME = "note"
    const val TITLE = "note_title"
    const val BODY = "note_body"
    const val SAVE = "note_save"
    const val CANCEL = "note_cancel"
    const val ABOUT = "note_about"
    const val BOLD = "note_mark_bold"
    const val ITALIC = "note_mark_italic"
    const val BULLET = "note_mark_bullet"
}

/**
 * Writing a note. #397, D207.
 *
 * **The owner: "it may be that I visit my mom and I need to take a note that
 * she wants me to bring her something from the house or things like that and I
 * want to have basic rich editing. nothing too crazy but it needs to have a
 * very clean layout."**
 *
 * **It is not "ask next time"** and it is not a second trail. A note is an
 * entry, so it is already on the trail, already in search and already in the
 * archive; this screen only writes one.
 *
 * **Nothing here is required**, rule 13. A note with a body and no name is a
 * note. A note with no date is a note, and the date is unknown rather than
 * today's, which is rule 17's first class value rather than a guess.
 *
 * **The context is carried forward rather than asked for**, rule 18: opening
 * this from Tuesday's visit already knows which visit it is about, and the chip
 * says so rather than making somebody choose from a list they just came out of.
 *
 * **Three marks and no more**, `contract/DATA-CONTRACT.md` 8.8.1. The controls
 * wrap the selection in the characters themselves, because the characters are
 * what is stored: there is no editor state, no document model, and nothing to
 * convert on the way to the archive.
 */
@Composable
fun NoteScreen(
    onSave: (title: String, body: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** What this note is about, already in the app's own words. Null for a general note. */
    aboutLabel: String? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }

    Page(
        title = strings["note.title"],
        onBack = onBack,
        backLabel = strings["common.cancel"],
        modifier = modifier.testTag(SectionTags.root(NoteTags.NAME)),
        subtitle = strings["note.lead"],
        // **The way out sits in the band**, where every other form's Save does,
        // so the keyboard never covers it. #129.
        // **A save and a cancel, in that order of weight.** The owner,
        // 2026-08-18: "make that the save/cancel material 3 expressive style."
        // It was one quiet action sitting on the left of the band, which reads
        // as a link rather than as the thing the screen is for.
        //
        // **One filled action and one quiet one**, `ActionEmphasis` law: at most
        // one filled per group, because a band with two shouts has told the
        // person nothing about which one matters. Save takes the width it
        // deserves and cancel keeps its presence without competing.
        band = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Action(
                    label = strings["common.cancel"],
                    emphasis = ActionEmphasis.Quiet,
                    onClick = onBack,
                    modifier = Modifier.testTag(NoteTags.CANCEL),
                )
                Action(
                    label = strings["note.save"],
                    emphasis = ActionEmphasis.Main,
                    // **A note with nothing in it is not a note.** This is the
                    // one thing on the screen that is required, and it is
                    // required because saving nothing writes an empty row
                    // rather than helping anybody. Rule 13 is about not
                    // blocking on *missing* fields, not about writing blank
                    // records.
                    enabled = body.isNotBlank() || title.isNotBlank(),
                    onClick = { onSave(title.trim(), body) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(NoteTags.SAVE),
                )
            }
        },
    ) {
        aboutLabel?.let { about ->
            item {
                // **What it is about, carried rather than asked**, rule 18. It
                // is a label and not a control: this note is about that thing
                // because that is where it was opened from, and offering to
                // change it here would be a picker for a question already
                // answered.
                AssistChip(
                    // This is a label and not a control: the note is about that
                    // thing because that is where it was opened from, and it is
                    // disabled, so nothing announces it as something to press.
                    // allow-empty-handler: a disabled chip stating a fact.
                    onClick = {},
                    enabled = false,
                    label = { Text(strings("note.about", "what" to Bidi.isolate(about))) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = colors.ink,
                    ),
                    modifier = Modifier.testTag(NoteTags.ABOUT),
                )
                Spacer(Modifier.height(Space.betweenGroups))
            }
        }

        item {
            // **No microphone on this screen, either style.** The owner,
            // 2026-08-18: "not have two different styles for the microphone.
            // get rid of the mic completely." It had both at once, the inline
            // mark inside this field and the prominent Speak it button under
            // the body, which is rule 16's two answers to one question on one
            // screen. `Field` rather than `DictatableField`.
            Field(
                label = strings["note.name"],
                value = title,
                onValueChange = { title = it },
                support = strings["note.name.hint"],
                singleLine = true,
                imeAction = ImeAction.Next,
                fieldTestTag = NoteTags.TITLE,
            )
            Spacer(Modifier.height(Space.betweenGroups))
        }

        item {
            // **The marks belong to the field they act on.** The owner: "the
            // rich formatting needs to be done proper in body with buttons.
            // not just randomly floating." They were an eyebrow and three
            // chips in a block of their own under the field, which reads as a
            // separate section of the page rather than as the field's own
            // controls.
            //
            // **One container holds the field and its toolbar**, so the two are
            // visibly one thing, and the toolbar sits under the writing surface
            // where a thumb already is rather than above it where the label is.
            // **One box, not a box in a box.** Looked at on the phone: the
            // field kept its own outline inside a bordered container, so the
            // writing surface was drawn twice. The field is the box; the
            // toolbar is bound to it by sitting against it, at the within-group
            // gap, with nothing between them.
            Column(modifier = Modifier.fillMaxWidth()) {
                Field(
                    label = strings["note.body"],
                    value = body,
                    onValueChange = { body = it },
                    support = null,
                    singleLine = false,
                    fieldTestTag = NoteTags.BODY,
                        // **The main writing surface looks like one.** Looked at
                        // on the phone: it drew at the height of a field asking
                        // for a name, on a screen whose whole purpose is the
                        // paragraph that goes in it.
                    minLines = NOTE_LINES,
                )
                Spacer(Modifier.height(Space.withinGroup))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    MarkButton(
                        label = strings["note.mark.bold"],
                        symbol = Symbols.bold,
                        tag = NoteTags.BOLD,
                        onClick = { body = wrap(body, RichText.BOLD) },
                    )
                    MarkButton(
                        label = strings["note.mark.italic"],
                        symbol = Symbols.italic,
                        tag = NoteTags.ITALIC,
                        onClick = { body = wrap(body, RichText.ITALIC) },
                    )
                    MarkButton(
                        label = strings["note.mark.bullet"],
                        symbol = Symbols.bullet,
                        tag = NoteTags.BULLET,
                        onClick = { body = bullet(body) },
                    )
                }
            }
            // **One line under the marks, not two paragraphs.** The example
            // sentence and the rule about the subset were stacked in the same
            // gray, which reads as a wall rather than as help.
            Spacer(Modifier.height(Space.withinGroup))
            Text(
                text = strings["note.marks.help"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }
}

/**
 * One mark, as a button on the field's own toolbar.
 *
 * **The glyph and the word together**, D198 item 6: a control that says only
 * "B" is a control somebody has to already know, and this app is used by
 * people who are tired.
 */
@Composable
private fun MarkButton(
    label: String,
    @DrawableRes symbol: Int,
    tag: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Space.sm, vertical = Space.xs),
        modifier = Modifier
            .testTag(tag)
            .semantics { contentDescription = label },
    ) {
        Icon(
            painter = painterResource(symbol),
            contentDescription = null,
            modifier = Modifier.size(Space.markInline),
        )
        Spacer(Modifier.width(Space.xs))
        Text(text = label)
    }
}

/**
 * The mark added at the end, ready to be typed into.
 *
 * **It appends rather than wrapping a selection**, because a plain string field
 * has no selection to read. Tapping bold gives somebody `**` `**` with the
 * caret where the words go, which is what the marks are: characters.
 */
internal fun wrap(body: String, mark: String): String =
    if (body.isEmpty() || body.endsWith("\n")) {
        "$body$mark$mark"
    } else {
        "$body $mark$mark"
    }

/** A new line beginning with the bullet, or this line turned into one. */
internal fun bullet(body: String): String = when {
    body.isEmpty() -> RichText.BULLET
    body.endsWith("\n") -> body + RichText.BULLET
    else -> "$body\n${RichText.BULLET}"
}
