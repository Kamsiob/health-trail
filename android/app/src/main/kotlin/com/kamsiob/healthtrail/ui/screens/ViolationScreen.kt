package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChipPickerSheet
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.Disclosure
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.MoreChip
import com.kamsiob.healthtrail.ui.components.PickerOption
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.cappedChips
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

object ViolationTags {
    const val NAME = "violation"
    const val NOTE = "violation_note"
    const val SAVE = "violation_save"
    const val ABOUT = "violation_about"
    const val PICK_DATE = "violation_date"
    const val UNKNOWN_DATE = "violation_date_unknown"
    const val REMOVE = "violation_remove"
    const val MORE_INCIDENTS = "violation_more_incidents"
    const val MORE_BILLS = "violation_more_bills"
    fun incident(id: String) = "violation_incident_$id"
    fun bill(id: String) = "violation_bill_$id"
}

/** What the person wrote down about a time an instruction was not followed. */
data class ViolationDraft(
    /** The time being corrected, or null when one is being written down. */
    val violationId: String? = null,
    val instructionId: String,
    val occurred: Edtf.Date,
    val note: String?,
    /**
     * What it broke against, when the person already knows.
     *
     * **Both were parameters of `recordViolation` with a null default and the
     * only caller passed neither**, so `Violation.incidentTitle` and
     * `billDescription` were read by a screen that could never be handed one.
     * #371, and it is the shape three panels named: the schema was built for
     * this app and the interface called the writers without the arguments.
     */
    val incidentId: String? = null,
    val billId: String? = null,
)

/** Which full set the person asked for, when five chips are not all of them. */
private enum class AboutPicker { INCIDENTS, BILLS }

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
 * **Which is why the link lives behind the disclosure**, per 10.8, rather than
 * on the short form or nowhere at all. It was nowhere at all until #371: the
 * two columns existed, the reader joined them, and the form never asked, so a
 * violation could never say what it broke against. The corridor path is still
 * one field and a Save.
 *
 * **When it happened is asked rather than stamped.** It used to be
 * `LocalDate.now()` with no control on the form at all, so somebody writing
 * down last month's nine o'clock dose got today's date at full day precision
 * and could never change it: false precision on a record read aloud in a room,
 * which is exactly what rule 17 forbids. Today is still the default, so the
 * corridor path is unchanged, and "I am not sure" saves and appears.
 *
 * **The same screen corrects one.** D147: a correction is never staged, it
 * opens the disclosure, and it carries the way to take the record off. Somebody
 * interrupted mid sentence who tapped Save had a half typed word on their
 * record permanently, and it permanently counted.
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
    /**
     * What this could have broken against, offered and never required.
     *
     * Empty on a notebook with none of either, and the disclosure is absent
     * rather than empty: an offer to link to nothing is a control that does
     * nothing, which is rule 11.
     */
    incidents: List<Repository.Incident> = emptyList(),
    bills: List<Repository.Bill> = emptyList(),
    /** The time being corrected, or null when one is being written down. */
    existing: Repository.Violation? = null,
    onRemove: (() -> Unit)? = null,
    today: LocalDate = LocalDate.now(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    // Saveable, because a rotation part way through writing this down must not
    // quietly drop the link the person already chose.
    var incidentId by rememberSaveable(existing?.id) { mutableStateOf(existing?.incidentId) }
    var billId by rememberSaveable(existing?.id) { mutableStateOf(existing?.billId) }
    var openPicker by remember { mutableStateOf<AboutPicker?>(null) }
    var picking by remember { mutableStateOf(false) }
    // **The canonical string rather than the parsed date**, because that is
    // what a bundle can carry and what the row actually holds.
    var occurredEdtf by rememberSaveable(existing?.id) {
        mutableStateOf(
            existing?.occurredEdtf ?: Edtf.day(today).canonical,
        )
    }
    val occurred = Edtf.parse(occurredEdtf) ?: Edtf.day(today)

    SectionScaffold(
        name = ViolationTags.NAME,
        // **The chip says which section, the heading says what you came for.**
        // "A time it was not followed" was in both slots, once at 11sp in mono
        // and once at display weight. The instruction's own name is the
        // subtitle, so the three lines are now where you are, what you are
        // doing, and which request it is about. #341.
        title = strings["notebook.section.standing_instructions"],
        headingKey = if (existing == null) "violation.title" else "violation.correct.title",
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

            // **Asked, with today already the answer.** The same two the rest
            // of the app gives a date: a day from the picker, or "I am not
            // sure", which saves and appears rather than being refused. Rule 17,
            // and `contract/DATA-CONTRACT.md` names this row's `occurred_edtf`
            // as one that may be entirely unknown.
            Spacer(Modifier.height(Space.sectionGap))
            ChoiceChipGroup(label = strings["violation.when"]) {
                ChoiceChip(
                    label = strings["capture.when.exact"],
                    selected = occurred.precision != Edtf.Precision.UNKNOWN,
                    onClick = { picking = true },
                    modifier = Modifier.testTag(ViolationTags.PICK_DATE),
                )
                ChoiceChip(
                    label = strings["date.pick.clear"],
                    selected = occurred.precision == Edtf.Precision.UNKNOWN,
                    onClick = { occurredEdtf = Edtf.unknown().canonical },
                    modifier = Modifier.testTag(ViolationTags.UNKNOWN_DATE),
                )
            }
            Spacer(Modifier.height(Space.s))
            Text(
                text = EventDateText.render(strings, occurred),
                style = HealthTrail.type.bodyL,
                color = colors.ink,
            )

            // **Behind one control nobody has to touch**, and absent entirely
            // where there is nothing to point at. The five chip cap and its way
            // to the rest are 5.11.1, the same set the capture form offers.
            if (incidents.isNotEmpty() || bills.isNotEmpty()) {
                Spacer(Modifier.height(Space.sectionGap))
                Disclosure(
                    labelKey = "violation.about",
                    asideKey = "violation.about.aside",
                    testTag = ViolationTags.ABOUT,
                    // **A correction never hides what is already written down**,
                    // D147 and the disclosure's own rule: folding away a link
                    // somebody chose last week behind a control that says "Say
                    // what this was about" is the app hiding their own answer.
                    startOpen = existing?.incidentId != null || existing?.billId != null,
                ) {
                    if (incidents.isNotEmpty()) {
                        val chosenIncident = incidents.firstOrNull { it.id == incidentId }
                        ChoiceChipGroup(label = strings["violation.about.incident"]) {
                            cappedChips(incidents, chosenIncident).forEach { incident ->
                                ChoiceChip(
                                    // The person's own words for what happened.
                                    label = Bidi.isolate(incident.title),
                                    selected = incidentId == incident.id,
                                    // **Tapping the chosen one clears it**, the
                                    // same toggle the capture form's person and
                                    // medication chips use. A link a person
                                    // cannot take off again is a link they
                                    // hesitate to put on.
                                    onClick = {
                                        incidentId =
                                            if (incidentId == incident.id) null else incident.id
                                    },
                                    modifier = Modifier
                                        .testTag(ViolationTags.incident(incident.id)),
                                )
                            }
                            if (incidents.size > cappedChips(incidents, chosenIncident).size) {
                                MoreChip(
                                    label = strings("chips.all", "count" to incidents.size),
                                    onClick = { openPicker = AboutPicker.INCIDENTS },
                                    modifier = Modifier.testTag(ViolationTags.MORE_INCIDENTS),
                                )
                            }
                        }
                    }

                    if (incidents.isNotEmpty() && bills.isNotEmpty()) {
                        Spacer(Modifier.height(Space.sectionGap))
                    }

                    if (bills.isNotEmpty()) {
                        val chosenBill = bills.firstOrNull { it.id == billId }
                        ChoiceChipGroup(label = strings["violation.about.bill"]) {
                            cappedChips(bills, chosenBill).forEach { bill ->
                                ChoiceChip(
                                    label = Bidi.isolate(bill.description),
                                    selected = billId == bill.id,
                                    onClick = {
                                        billId = if (billId == bill.id) null else bill.id
                                    },
                                    modifier = Modifier.testTag(ViolationTags.bill(bill.id)),
                                )
                            }
                            if (bills.size > cappedChips(bills, chosenBill).size) {
                                MoreChip(
                                    label = strings("chips.all", "count" to bills.size),
                                    onClick = { openPicker = AboutPicker.BILLS },
                                    modifier = Modifier.testTag(ViolationTags.MORE_BILLS),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.sectionGap))
            FilledButton(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        ViolationDraft(
                            violationId = existing?.id,
                            instructionId = instruction.id,
                            occurred = occurred,
                            note = note.trim(),
                            incidentId = incidentId,
                            billId = billId,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(ViolationTags.SAVE),
            )

            // **Last, and set apart**, per D135 and every other thing in the
            // app that can be taken off: it is the rarest thing anybody comes
            // here to do, and it opens the confirmation rather than doing
            // anything itself.
            if (onRemove != null) {
                Spacer(Modifier.height(Space.sectionGap))
                QuietButton(
                    label = strings["remove.action"],
                    onClick = onRemove,
                    modifier = Modifier.testTag(ViolationTags.REMOVE),
                )
            }
            Spacer(Modifier.height(Space.l))
        }
    }

    if (picking) {
        DatePickerSheet(
            // Opens on whatever is already chosen, so confirming without
            // touching anything changes nothing.
            initial = occurred,
            onPick = {
                occurredEdtf = it.canonical
                picking = false
            },
            onDismiss = { picking = false },
            today = today,
        )
    }

    // **One sheet for whichever set was asked for**, per 5.11.1, because it is
    // the same question both times: which one of these, out of more than fit in
    // a row. The date is the detail, since two bills from the same place carry
    // the same description and differ by when they arrived.
    when (openPicker) {
        AboutPicker.INCIDENTS -> ChipPickerSheet(
            title = strings["violation.about.incident"],
            options = incidents.map { incident ->
                PickerOption(
                    id = incident.id,
                    label = Bidi.isolate(incident.title),
                    detail = incident.reportedEdtf?.takeIf { it.isNotBlank() }
                        ?.let { EventDateText.render(strings, it) },
                )
            },
            selectedId = incidentId,
            onPick = { option ->
                openPicker = null
                incidentId = if (incidentId == option.id) null else option.id
            },
            onDismiss = { openPicker = null },
        )
        AboutPicker.BILLS -> ChipPickerSheet(
            title = strings["violation.about.bill"],
            options = bills.map { bill ->
                PickerOption(
                    id = bill.id,
                    label = Bidi.isolate(bill.description),
                    detail = bill.receivedEdtf?.takeIf { it.isNotBlank() }
                        ?.let { EventDateText.render(strings, it) },
                )
            },
            selectedId = billId,
            onPick = { option ->
                openPicker = null
                billId = if (billId == option.id) null else option.id
            },
            onDismiss = { openPicker = null },
        )
        null -> Unit
    }
}
