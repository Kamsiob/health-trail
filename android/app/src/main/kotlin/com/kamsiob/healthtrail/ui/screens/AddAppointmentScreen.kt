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
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChipPickerSheet
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.Disclosure
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.MoreChip
import com.kamsiob.healthtrail.ui.components.PickerOption
import com.kamsiob.healthtrail.ui.components.cappedChips
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

object AddApptTags {
    const val ROOT = "add_appt_root"
    const val SAVE = "add_appt_save"
    const val CANCEL = "add_appt_cancel"
    const val PICK_DATE = "add_appt_pick_date"
    const val MORE = "add_appt_more"
    const val MORE_PEOPLE = "add_appt_more_people"
    fun field(key: String) = "add_appt_$key"
    fun person(id: String) = "add_appt_person_$id"
}

/** What the person typed about an appointment. */
data class AppointmentDraft(
    val title: String = "",
    val where: String = "",
    val notes: String = "",
    val scheduled: Edtf.Date? = null,
    /**
     * Who it is with.
     *
     * **`appointment.person_id` has been in the schema since Phase 0 with
     * nothing writing it**, and it is the half that makes the prep sheet
     * possible: `question.person_id` says who a question is waiting for, and
     * without this there was nothing to compare it against, so the sheet
     * showed every open question in the notebook. #371 item 2.
     */
    val personId: String? = null,
)

/**
 * Recording an appointment.
 *
 * **The date is as rough as the person was told.** Somebody is told "next
 * Tuesday", "sometime in March", or "they will call you", and every one of
 * those is a real answer. It uses the same picker as every other date in the
 * app and the same rough chips, so "not sure" saves and appears rather than
 * being refused.
 *
 * **Only the title is needed.** A date with no name is not an appointment, but
 * a name with no date is: "the care plan meeting, they have not scheduled it
 * yet" is exactly the kind of thing worth writing down before it slips.
 *
 * **What it is and roughly when lead; where and the note are behind "Add
 * more".** #361, 2026-08-12. Four fields down one scroll asked for an address
 * in the same breath as the appointment, and the address usually arrives on a
 * letter days later.
 *
 * **Who it is with leads too, and it is a row of taps rather than a field.**
 * It is what makes the prep sheet answer: without it every open question in the
 * notebook came to every appointment, including the ones waiting on somebody
 * who will not be in the room. **The aside says why**, because a control that
 * asks for something without saying what it buys is one people skip.
 */
@Composable
fun AddAppointmentScreen(
    onSave: (AppointmentDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** The record being corrected, or null when this one is new. */
    existing: Repository.Appointment? = null,
    /** The care team, most recently used first. Empty on a new notebook. */
    people: List<Repository.Person> = emptyList(),
    today: LocalDate = LocalDate.now(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by remember(existing?.id) {
        mutableStateOf(
            AppointmentDraft(
                // bidi-ok: the value inside a field being edited. Isolate marks here would become characters the person has to delete.
                title = existing?.title.orEmpty(),
                where = existing?.locationNote.orEmpty(),
                notes = existing?.notes.orEmpty(),
                scheduled = existing?.scheduledEdtf?.let { Edtf.parse(it) },
                personId = existing?.personId,
            ),
        )
    }
    var picking by remember { mutableStateOf(false) }
    var pickingPerson by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(AddApptTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = if (existing == null) strings["appts.add"] else strings["appts.edit.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["appts.add.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["appts.title"],
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    hint = strings["appts.title.hint"],
                    fieldTestTag = AddApptTags.field("title"),
                )
                Spacer(Modifier.height(Space.m))

                // **The capture form's rough chips are not reused here, and
                // that is deliberate.** They are today, yesterday, this week,
                // and not sure, which are the answers to "when did this
                // happen". An appointment is usually ahead, so every one of
                // them except the last would be wrong. The picker and "not
                // sure" carry it, in the words the rest of the app already
                // uses for both.
                ChoiceChipGroup(label = strings["appts.when"]) {
                    ChoiceChip(
                        label = strings["capture.when.exact"],
                        selected = draft.scheduled != null &&
                            draft.scheduled?.precision != Edtf.Precision.UNKNOWN,
                        onClick = { picking = true },
                        modifier = Modifier.testTag(AddApptTags.PICK_DATE),
                    )
                    ChoiceChip(
                        label = strings["date.pick.clear"],
                        selected = draft.scheduled?.precision == Edtf.Precision.UNKNOWN,
                        onClick = { draft = draft.copy(scheduled = Edtf.unknown()) },
                    )
                }

                draft.scheduled?.let { chosen ->
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = EventDateText.render(strings, chosen),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                }

                // **Who it is with**, five chips and a way to the rest, per
                // 5.11.1 and the same set the capture form offers. Tapping the
                // chosen one clears it, because a link somebody cannot take off
                // again is one they hesitate to put on.
                if (people.isNotEmpty()) {
                    val chosen = people.firstOrNull { it.id == draft.personId }
                    val shown = cappedChips(people, chosen)
                    Spacer(Modifier.height(Space.sectionGap))
                    ChoiceChipGroup(
                        label = strings["appts.who"],
                        aside = strings["appts.who.aside"],
                    ) {
                        shown.forEach { person ->
                            ChoiceChip(
                                // A name is the person's own words. #226.
                                label = Bidi.isolate(person.displayName),
                                selected = draft.personId == person.id,
                                onClick = {
                                    draft = draft.copy(
                                        personId = if (draft.personId == person.id) {
                                            null
                                        } else {
                                            person.id
                                        },
                                    )
                                },
                                modifier = Modifier.testTag(AddApptTags.person(person.id)),
                            )
                        }
                        if (people.size > shown.size) {
                            MoreChip(
                                label = strings("chips.all", "count" to people.size),
                                onClick = { pickingPerson = true },
                                modifier = Modifier.testTag(AddApptTags.MORE_PEOPLE),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.sectionGap))

                // **Where it is and anything else, behind "Add more"**, per law
                // 3 and 10.8. #361. What somebody knows when they hang up is
                // what the appointment is and roughly when; the address usually
                // arrives on a letter later, and asking for it in the same
                // breath is what made this read as a data entry form.
                //
                // **Open already when either says something**, so correcting an
                // appointment never hides the note somebody wrote last week.
                Disclosure(
                    testTag = AddApptTags.MORE,
                    startOpen = draft.where.isNotBlank() || draft.notes.isNotBlank(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HealthTrailTextField(
                            label = strings["appts.where"],
                            value = draft.where,
                            onValueChange = { draft = draft.copy(where = it) },
                            hint = strings["appts.where.hint"],
                            fieldTestTag = AddApptTags.field("where"),
                        )
                        Spacer(Modifier.height(Space.m))

                        DictatableField(
                            label = strings["appts.notes"],
                            value = draft.notes,
                            onValueChange = { draft = draft.copy(notes = it) },
                            hint = strings["appts.notes.hint"],
                            singleLine = false,
                            imeAction = ImeAction.Done,
                            fieldTestTag = AddApptTags.field("notes"),
                        )
                    }
                }

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddApptTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddApptTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }

    if (picking) {
        DatePickerSheet(
            initial = draft.scheduled,
            onPick = { picked ->
                draft = draft.copy(scheduled = picked)
                picking = false
            },
            onDismiss = { picking = false },
            today = today,
            // An appointment is ahead, so the picker asks in the future tense.
            titleKey = "date.pick.title.future",
        )
    }

    if (pickingPerson) {
        ChipPickerSheet(
            title = strings["appts.who"],
            options = people.map {
                PickerOption(
                    id = it.id,
                    label = Bidi.isolate(it.displayName),
                    detail = it.roleLabel?.takeIf { role -> role.isNotBlank() }
                        ?.let { role -> Bidi.isolate(role) },
                )
            },
            selectedId = draft.personId,
            onPick = { option ->
                pickingPerson = false
                draft = draft.copy(
                    personId = if (draft.personId == option.id) null else option.id,
                )
            },
            onDismiss = { pickingPerson = false },
        )
    }
}
