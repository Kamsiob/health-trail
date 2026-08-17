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
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.FieldBlock
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.SwitchRow

object AddMedTags {
    const val ROOT = "add_med_root"
    const val SAVE = "add_med_save"
    const val ON_CARD = "add_med_on_card"
    const val MORE = "add_med_more"
    fun field(key: String) = "add_med_$key"
}

/** What the person typed about a medication. */
data class MedicationDraft(
    val name: String = "",
    val dose: String = "",
    /** How often, in the words it was given in. #379. */
    val frequency: String = "",
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
 *
 * **The name, the dose and the card question lead, and the rest is behind "Add
 * more".** #361, 2026-08-12. It was four multi line fields down one scroll, each
 * with its label above it, which is the shape the owner called a data entry app.
 * What it is for and any other note open by themselves when either already says
 * something, so a correction never folds away what is written.
 */
@Composable
fun AddMedicationScreen(
    onSave: (MedicationDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** The medication being corrected, or null when this one is new. */
    existing: Repository.Medication? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by remember(existing?.id) {
        mutableStateOf(
            MedicationDraft(
                name = existing?.name.orEmpty(),
                dose = existing?.doseText.orEmpty(),
                frequency = existing?.frequencyText.orEmpty(),
                purpose = existing?.purposeText.orEmpty(),
                notes = existing?.notes.orEmpty(),
                onEmergencyCard = existing?.onEmergencyCard == true,
            ),
        )
    }

    Page(
        title = if (existing == null) {
                        strings["meds.add.title"]
                    } else {
                        strings["meds.edit.title"]
                    },
        onBack = onCancel,
        backLabel = strings["common.cancel"],
        modifier = modifier.testTag(AddMedTags.ROOT),
        eyebrow = strings[labelKey(Repository.Section.MEDICATIONS)],
        section = Repository.Section.MEDICATIONS,
        // **The form's own gaps, not the page's.** A form is one
        // question after another rather than a column of groups, and
        // it spaces itself inside its single item.
        itemSpacing = Space.none,
        band = {
        Action(
            label = strings["capture.save"],
            onClick = { onSave(draft) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screenHorizontal)
                .testTag(AddMedTags.SAVE), emphasis = ActionEmphasis.Main,
        )
        Spacer(Modifier.height(Space.s))
        },
    ) {
        item {
            Column {
            Spacer(Modifier.height(Space.m))

            FactBlock(
                label = null,
                text = strings["meds.add.lead"],
                tone = BlockTone.Section,
                mark = Symbols.of(Repository.Section.MEDICATIONS),
                hue = hueFor(Repository.Section.MEDICATIONS),
            )
            Spacer(Modifier.height(Space.l))

            Field(
                label = strings["meds.name"],
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                fieldTestTag = AddMedTags.field("name"),
                support = strings["meds.name.hint"],
            )
            Spacer(Modifier.height(Space.m))

            DictatableField(
                label = strings["meds.dose"],
                value = draft.dose,
                onValueChange = { draft = draft.copy(dose = it) },
                support = strings["meds.dose.hint"],
                singleLine = false,
                fieldTestTag = AddMedTags.field("dose"),
            )
            Spacer(Modifier.height(Space.m))

            // **How often, up front with the name and the dose**, the
            // owner's direction on #379. It is the third thing anybody is
            // told at a bedside and the form asked for two of the three,
            // which sent the frequency into a note or nowhere.
            //
            // **Text, and never parsed**, for the same reason the dose is:
            // "every other morning" and "with meals, but not on dialysis
            // days" are real answers, and a schedule picker that could not
            // hold the second would make somebody write something less
            // true.
            DictatableField(
                label = strings["meds.frequency"],
                value = draft.frequency,
                onValueChange = { draft = draft.copy(frequency = it) },
                support = strings["meds.frequency.hint"],
                singleLine = false,
                fieldTestTag = AddMedTags.field("frequency"),
            )
            Spacer(Modifier.height(Space.l))

            // **A switch, because the question has two answers and one of
            // them is what happens if nobody touches it.** The approved
            // mockup asks it this way and the form asked it with a single
            // chip, which reads as a filter everywhere else in this app.
            // The note that sat underneath in the caption ink is the
            // switch's own subtitle now, so the question and the reason
            // for it are one object.
            //
            // **It stays above the disclosure**, unlike the two fields
            // below it: the moment somebody writes a medication down is the
            // moment they know whether it matters in an emergency, and a
            // question folded away is a question nobody answers.
            SwitchRow(
                title = strings["meds.on_card.badge"],
                checked = draft.onEmergencyCard,
                onCheckedChange = { draft = draft.copy(onEmergencyCard = it) },
                modifier = Modifier.testTag(AddMedTags.ON_CARD),
                support = strings["meds.on_card.note"],
                mark = Symbols.of(Repository.Section.EMERGENCY_CARD),
                hue = hueFor(Repository.Section.EMERGENCY_CARD),
            )

            Spacer(Modifier.height(Space.sectionGap))

            // **What it is for and anything else, behind "Add more"**, per
            // law 3 and 10.8. #361: this form put four multi line fields
            // down one scroll, each with a label above it, and the two that
            // matter are the name and what she actually takes. Neither of
            // these is ever required, and the disclosure opens by itself
            // when one of them already says something.
            // **The rest of the form is a group with a label, not a fold.**
            // D185: nothing sits behind a fold that a label and a scroll can
            // carry, and the sentence that used to explain the fold is the
            // group's own line now. Nothing here was ever required.
            FieldBlock(
                label = strings["capture.more"],
                aside = strings["capture.more.aside"],
                modifier = Modifier.testTag(AddMedTags.MORE),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DictatableField(
                        label = strings["meds.purpose"],
                        value = draft.purpose,
                        onValueChange = { draft = draft.copy(purpose = it) },
                        support = strings["meds.purpose.hint"],
                        singleLine = false,
                        fieldTestTag = AddMedTags.field("purpose"),
                    )
                    Spacer(Modifier.height(Space.m))

                    DictatableField(
                        label = strings["meds.notes"],
                        value = draft.notes,
                        onValueChange = { draft = draft.copy(notes = it) },
                        support = strings["meds.notes.hint"],
                        singleLine = false,
                        imeAction = ImeAction.Done,
                        fieldTestTag = AddMedTags.field("notes"),
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))
            }
        }
    }
}
