package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

object ViolationTags {
    const val NAME = "violation"
    const val NOTE = "violation_note"
    const val SAVE = "violation_save"
}

/** What the person wrote down about a time an instruction was not followed. */
data class ViolationDraft(
    val instructionId: String,
    val occurred: Edtf.Date,
    val note: String?,
)

/**
 * Writing down a time a standing instruction was not followed.
 *
 * **This is the part of the record a family actually needs in a room.** "We
 * asked in writing in March, and it happened again in May and again in June" is
 * a different conversation from "we asked in March", and
 * `instruction_violation` sat in the schema from Phase 0 with no reader and no
 * writer.
 *
 * **One field, and the link is optional.** Somebody writing this down in a
 * corridor knows it happened. Working out which bill or which incident it
 * belongs to is a later, calmer job, and asking for it now is how the thing
 * never gets written down at all. Rule 13: partial is a finished state.
 *
 * **Nothing here is a complaint form.** The screen records what happened and
 * says nothing about what should be done, because the app never concludes.
 */
@Composable
fun ViolationScreen(
    instruction: Repository.StandingInstruction,
    onSave: (ViolationDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var note by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = ViolationTags.NAME,
        // **The chip says which section, the heading says what you came for.**
        // "A time it was not followed" was in both slots, once at 11sp in mono
        // and once at display weight. The instruction's own name is the
        // subtitle, so the three lines are now where you are, what you are
        // doing, and which request it is about. #341.
        title = strings["notebook.section.standing_instructions"],
        headingKey = "violation.title",
        subtitle = Bidi.isolate(instruction.name),
        // **The way back is the cancel**, which is why this screen draws no
        // second one. It used to carry a full width outlined "Cancel" directly
        // above a full width outlined "Back to what you have asked for", two
        // identical bars doing the identical thing under two different words.
        // #340, and the same reasoning 15.1 records for the emergency card's
        // four Change pills.
        onBack = onCancel,
        backLabelKey = "section.back.instructions",
        modifier = modifier,
    ) {
        item {
            // The instruction in its own words, so somebody writing this down
            // is looking at what they actually asked for rather than at a
            // paraphrase of it.
            instruction.wording.takeIf { it.isNotBlank() }?.let {
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.sectionGap))
            }

            DictatableField(
                label = strings["violation.what"],
                value = note,
                onValueChange = { note = it },
                hint = strings["violation.what.hint"],
                fieldTestTag = ViolationTags.NOTE,
            )

            Spacer(Modifier.height(Space.sectionGap))
            FilledButton(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        ViolationDraft(
                            instructionId = instruction.id,
                            occurred = Edtf.day(LocalDate.now()),
                            note = note.trim(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(ViolationTags.SAVE),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
