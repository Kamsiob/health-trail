package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.DictateAction
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.Page

object CorrectEntryTags {
    const val ROOT = "correct_entry"
    const val TITLE = "correct_entry_title"
    const val BODY = "correct_entry_body"
    const val SAVE = "correct_entry_save"
}

/** What a correction carries back, so the screen holds no repository of its own. */
data class EntryCorrection(val title: String, val body: String)

/**
 * Changing what an entry says. #368.
 *
 * **The most created thing in the app was the only one that could not be
 * corrected.** A person, a medication, a bill, an appointment, a document, a
 * milestone, an incident and a project step all had a way to fix them. A trail
 * entry had its date and nothing else, so a note typed one handed in a corridor
 * kept its typo forever, and the only remedy was removing the record of the
 * call and typing it again from memory, which loses its threads, its chapter,
 * its incident link and the moment it was written.
 *
 * **Four independent walkthroughs found this on 2026-08-12**, walking the app
 * as a person four days in, six months in, and looking for one fact. The one
 * four days in had dictated a note, watched the recognizer hear a drug name
 * wrong, and said she would keep a wrong note about her mother's care rather
 * than delete the record of the call. **The owner's words the same day were
 * that things should be movable and editable after the fact.**
 *
 * **It corrects and never creates**, like `CorrectIncidentScreen`, which is why
 * it takes an existing entry rather than an optional one. An entry is born in
 * the capture form, and a second way to make one would be a second answer to a
 * question that form already asks.
 *
 * **The words only.** The date has its own control on the entry screen, per
 * rule 17, and the links this entry carries are changed where they were made.
 * Nothing here is required: a title somebody wants gone should go, which is why
 * saving with either field emptied is allowed.
 */
@Composable
fun CorrectEntryScreen(
    entry: Repository.TrailEntry,
    onSave: (EntryCorrection) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // bidi-ok: the values inside fields being edited. Isolate marks here would
    // become characters the person has to delete.
    var title by remember(entry.id) { mutableStateOf(entry.title.orEmpty()) }
    var body by remember(entry.id) { mutableStateOf(entry.body.orEmpty()) }

    Page(
        title = strings["entry.correct"],
        onBack = onCancel,
        backLabel = strings["common.cancel"],
        modifier = modifier.testTag(CorrectEntryTags.ROOT),
        eyebrow = strings[labelKey(Repository.Section.TRAIL)],
        section = Repository.Section.TRAIL,
        // **The form's own gaps, not the page's.** A form is one
        // question after another rather than a column of groups, and
        // it spaces itself inside its single item.
        itemSpacing = Space.none,
        band = {
        // **It never disables.** Emptying a title is a correction like any
        // other, and a greyed out Save would be the app deciding what the
        // person meant. Rule 13.
        Action(
            label = strings["capture.save"],
            onClick = { onSave(EntryCorrection(title = title, body = body)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screenHorizontal)
                .testTag(CorrectEntryTags.SAVE), emphasis = ActionEmphasis.Main,
        )
        Spacer(Modifier.height(Space.s))
        },
    ) {
        item {
            Column {
                Spacer(Modifier.height(Space.m))
                FactBlock(
                    label = null,
                    text = strings["entry.correct.lead"],
                    tone = BlockTone.Section,
                    mark = Symbols.of(Repository.Section.TRAIL),
                    hue = hueFor(Repository.Section.TRAIL),
                )

            Spacer(Modifier.height(Space.l))

            // **Fields sit on the canvas, never in a block.** A field is
            // already a container with its own outline and its own label, so a
            // second one around it is two edges on one thing, which is the
            // clutter D183 took out of the forms. `docs/V4.md` 2.1, `m3v4-4`.
            Column(verticalArrangement = Arrangement.spacedBy(Space.withinGroup)) {
                DictatableField(
                    label = strings[kindNameKey(entry.kind)],
                    value = title,
                    onValueChange = { title = it },
                    support = strings["entry.untitled"],
                    imeAction = ImeAction.Next,
                    fieldTestTag = CorrectEntryTags.TITLE,
                )

                Field(
                    label = strings["capture.call.note"],
                    value = body,
                    onValueChange = { body = it },
                    support = strings["capture.call.note.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = CorrectEntryTags.BODY,
                    // The microphone in the field, per #361. This is the
                    // screen somebody reaches because dictation heard a name
                    // wrong, so it has to offer dictation again.
                    trailing = {
                        DictateAction(
                            inField = true,
                            onText = { spoken ->
                                body = if (body.isBlank()) {
                                    spoken
                                } else {
                                    "${body.trimEnd()} $spoken"
                                }
                            },
                        )
                    },
                )
            }

            Spacer(Modifier.height(Space.xl))
            }
        }
    }
}
