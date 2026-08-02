package com.kamsiob.healthtrail.ui.screens

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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object AddMedTags {
    const val ROOT = "add_med_root"
    const val SAVE = "add_med_save"
    const val CANCEL = "add_med_cancel"
    const val ON_CARD = "add_med_on_card"
    fun field(key: String) = "add_med_$key"
}

/** What the person typed about a medication. */
data class MedicationDraft(
    val name: String = "",
    val dose: String = "",
    val purpose: String = "",
    val notes: String = "",
    val onEmergencyCard: Boolean = false,
)

/**
 * Recording a medication.
 *
 * **The dose field says out loud that the app never reads it as a number.**
 * That is not a disclaimer, it is permission: it tells the person that "half a
 * white one, twice a day, with food" is a perfectly good answer, which is what
 * people are actually told and what they would otherwise try to convert into
 * something that looks official.
 *
 * **Nothing here is checked against anything**, and the lead says so. There is
 * no drug database, no interaction check, and no spelling correction, because
 * every one of those would be the app knowing something about medicine, which
 * rule 2 forbids outright.
 *
 * **The emergency card choice is on this screen** rather than somewhere else,
 * because the moment somebody writes down a medication is the moment they know
 * whether it matters in an emergency. It carries a line saying why the card
 * holds what matters most rather than everything, so leaving it off does not
 * read as an omission.
 */
@Composable
fun AddMedicationScreen(
    onSave: (MedicationDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by remember { mutableStateOf(MedicationDraft()) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(AddMedTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["meds.add.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["meds.add.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["meds.name"],
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    hint = strings["meds.name.hint"],
                    fieldTestTag = AddMedTags.field("name"),
                )
                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["meds.dose"],
                    value = draft.dose,
                    onValueChange = { draft = draft.copy(dose = it) },
                    hint = strings["meds.dose.hint"],
                    singleLine = false,
                    fieldTestTag = AddMedTags.field("dose"),
                )
                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["meds.purpose"],
                    value = draft.purpose,
                    onValueChange = { draft = draft.copy(purpose = it) },
                    hint = strings["meds.purpose.hint"],
                    singleLine = false,
                    fieldTestTag = AddMedTags.field("purpose"),
                )
                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["meds.notes"],
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    hint = strings["meds.notes.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = AddMedTags.field("notes"),
                )

                Spacer(Modifier.height(Space.l))

                // The group asks the question and the chip is the answer. They
                // carried the same words at first, which read as the label
                // repeating itself and made the control look like a heading.
                ChoiceChipGroup(label = strings["meds.on_card"]) {
                    ChoiceChip(
                        label = strings["meds.on_card.chip"],
                        selected = draft.onEmergencyCard,
                        onClick = {
                            draft = draft.copy(onEmergencyCard = !draft.onEmergencyCard)
                        },
                        modifier = Modifier.testTag(AddMedTags.ON_CARD),
                    )
                }

                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["meds.on_card.note"],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink3Text,
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddMedTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddMedTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}
