package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Field
import java.time.LocalDate

object MedEventTags {
    const val NAME = "medication_event"
    const val DOSE = "medication_event_dose"
    const val NOTE = "medication_event_note"
    const val SAVE = "medication_event_save"
    fun kind(value: String) = "medication_event_kind_$value"
}

/**
 * What a change to a medication is, in the five words the schema allows.
 *
 * Kept as an enum here rather than as free text, because these are the states
 * the record understands and a sixth invented one would be a row nothing could
 * read back. The words the person sees are the catalog's, per `MedicationScreen`.
 */
/**
 * The kinds of change, in the schema's own words.
 *
 * **Every one of these is checked by the database, and one of them was not a
 * word the database knows.** `medication_event.kind` carries a CHECK constraint
 * naming exactly six values, and this enum was written from memory rather than
 * from the constraint: it offered `restarted`, which the schema calls `resumed`,
 * so choosing "Started again" wrote a value the table rejects. That path had
 * never been exercised, because nothing had ever produced a medication with a
 * history until the generator learned to tonight.
 *
 * `noted` was missing from both this list and all four catalogs, so an event of
 * that kind crashed the medication screen outright rather than degrading:
 * `Strings.resolve` throws on a key no catalog defines, and this asked for one.
 *
 * **The order is the order somebody would look for them in**, not the order the
 * constraint happens to list them.
 */
enum class MedicationChange(val stored: String) {
    STARTED("started"),
    DOSE_CHANGED("dose_changed"),
    HELD("held"),
    RESUMED("resumed"),
    STOPPED("stopped"),
    NOTED("noted"),
}

/** What the person filled in, ready to be written. */
data class MedicationEventDraft(
    val medicationId: String,
    val kind: String,
    val occurred: Edtf.Date,
    val doseText: String?,
    val note: String?,
)

/**
 * Recording that a medication changed.
 *
 * **The writer `medication_event` never had.** The table has been in the schema
 * since Phase 0, `MedicationScreen` could read a history, and nothing could
 * write one, so every medication's history was empty forever.
 *
 * **Chips rather than a text field**, because the set of possible answers is
 * knowable and short, which is Part Two's first rule. Five words, and the
 * record understands all five.
 *
 * **The dose is words, not a number.** "Half of one, twice a day" is what
 * somebody is told, and the app never reads it as a quantity. That is already
 * true of the medication itself and it stays true here.
 *
 * **Today by default, changeable.** A dose change is usually recorded the day
 * it happens or the day after, and rule 13 makes a default a starting point
 * rather than a decision.
 */
@Composable
fun MedicationEventScreen(
    medicationId: String,
    medicationName: String,
    onSave: (MedicationEventDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    var kind by rememberSaveable { mutableStateOf(MedicationChange.DOSE_CHANGED.name) }
    var dose by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = MedEventTags.NAME,
        // **The chip says which section, the heading says what you came for.**
        // This put "What changed with Donepezil" in the 11sp mono chip and
        // again at display weight underneath. The sentence is already a
        // heading; what it needed was a chip that is not the same sentence.
        // #341.
        title = strings["notebook.section.medications"],
        heading = strings("medevent.title", "name" to medicationName),
        subtitle = strings["medevent.lead"],
        onBack = onCancel,
        backLabelKey = "section.back.medications",
        modifier = modifier,
    ) {
        item {
            ChoiceChipGroup(label = strings["medevent.what"]) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    MedicationChange.entries.forEach { option ->
                        ChoiceChip(
                            label = strings["medication.event.${option.stored}"],
                            selected = kind == option.name,
                            onClick = { kind = option.name },
                            modifier = Modifier.testTag(MedEventTags.kind(option.stored)),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.sectionGap))

            Field(
                label = strings["medevent.dose"],
                value = dose,
                onValueChange = { dose = it },
                // **The example is the line, and it is the one that carries the
                // promise.** "Half of one, twice a day" is not a number, which
                // says however it was said is fine more convincingly than a
                // second sentence saying so. One supporting line, `docs/V4.md`
                // 2.1.
                support = strings["medevent.dose.hint"],
                fieldTestTag = MedEventTags.DOSE,
            )

            Spacer(Modifier.height(Space.sectionGap))

            DictatableField(
                label = strings["medevent.note"],
                value = note,
                onValueChange = { note = it },
                support = strings["medevent.note.hint"],
                fieldTestTag = MedEventTags.NOTE,
            )

            Spacer(Modifier.height(Space.sectionGap))
            Action(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        MedicationEventDraft(
                            medicationId = medicationId,
                            kind = MedicationChange.valueOf(kind).stored,
                            occurred = Edtf.day(LocalDate.now()),
                            doseText = dose.trim(),
                            note = note.trim(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(MedEventTags.SAVE), emphasis = ActionEmphasis.Main,
            )
            Spacer(Modifier.height(Space.cardGap))
            // **Sized to its label, not the width of the screen.** D137: a
            // full width outlined bar is the way back and nothing else, and
            // under a full width filled action it is a second bar of which
            // only one leaves. #371 item 5, and it is retroactive per rule 14.
            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
