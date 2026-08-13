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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

object CorrectIncidentTags {
    const val ROOT = "correct_incident"
    const val TITLE = "correct_incident_title"
    const val DESCRIPTION = "correct_incident_description"
    const val PICK_DATE = "correct_incident_date"
    const val UNKNOWN_DATE = "correct_incident_date_unknown"
    const val SAVE = "correct_incident_save"
    const val CANCEL = "correct_incident_cancel"
}

/** What a correction carries back, so the screen holds no repository of its own. */
data class IncidentCorrection(
    val title: String,
    val description: String,
    val reported: Edtf.Date?,
)

/**
 * Correcting an incident. #358.
 *
 * **This screen only ever corrects and never creates**, which is why it is not
 * an `AddIncidentScreen` with an `existing` parameter like the others. An
 * incident is born in the capture form, with an entry beside it in the same
 * transaction, and a second way to create one would be a second answer to a
 * question the capture form already asks.
 *
 * **It was the one thing in this app that could not be fixed afterward.** The
 * words are typed one handed in a corridor at the worst moment of somebody's
 * week, and until now they stayed exactly as typed. `renameProject` carries the
 * sentence the rest of the app lives by: every name in this app is a correction
 * away.
 *
 * **The date keeps whatever precision was given**, rule 17, and it uses the
 * same picker and the same "not sure" every other date in the app uses, so
 * correcting one behaves the way entering one did.
 *
 * **What happened next is not here.** Those are entries and they are corrected
 * where entries are corrected, which is the entry itself.
 */
@Composable
fun CorrectIncidentScreen(
    incident: Repository.Incident,
    onSave: (IncidentCorrection) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // bidi-ok: the values inside fields being edited. Isolate marks here would
    // become characters the person has to delete.
    var title by remember(incident.id) { mutableStateOf(incident.title) }
    var description by remember(incident.id) { mutableStateOf(incident.description.orEmpty()) }
    var reported by remember(incident.id) {
        mutableStateOf(incident.reportedEdtf?.let { Edtf.parse(it) })
    }
    var picking by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(CorrectIncidentTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                FormHeader(
                    title = strings["incident.correct.title"],
                    lead = strings["incident.correct.lead"],
                    section = Repository.Section.THREADS,
                )
                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["incident.correct.what"],
                    value = title,
                    onValueChange = { title = it },
                    hint = strings["incident.correct.what.hint"],
                    fieldTestTag = CorrectIncidentTags.TITLE,
                )
                Spacer(Modifier.height(Space.m))

                // **The same two answers the rest of the app gives a date**: a
                // day from the picker, or "not sure", which saves and appears
                // rather than being refused. Rule 17.
                ChoiceChipGroup(label = strings["incident.correct.when"]) {
                    ChoiceChip(
                        label = strings["capture.when.exact"],
                        selected = reported != null &&
                            reported?.precision != Edtf.Precision.UNKNOWN,
                        onClick = { picking = true },
                        modifier = Modifier.testTag(CorrectIncidentTags.PICK_DATE),
                    )
                    ChoiceChip(
                        label = strings["date.pick.clear"],
                        selected = reported?.precision == Edtf.Precision.UNKNOWN,
                        onClick = { reported = Edtf.unknown() },
                        modifier = Modifier.testTag(CorrectIncidentTags.UNKNOWN_DATE),
                    )
                }

                reported?.let { chosen ->
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = EventDateText.render(strings, chosen),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                }

                Spacer(Modifier.height(Space.m))

                DictatableField(
                    label = strings["incident.correct.note"],
                    value = description,
                    onValueChange = { description = it },
                    hint = strings["incident.correct.note.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = CorrectIncidentTags.DESCRIPTION,
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        IncidentCorrection(
                            title = title.trim(),
                            description = description,
                            reported = reported,
                        ),
                    )
                },
                // **The words are the one thing it cannot do without**, since
                // the schema requires a title and an incident with none is not
                // a record of anything. Everything else can be emptied.
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(CorrectIncidentTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(CorrectIncidentTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }

    if (picking) {
        DatePickerSheet(
            initial = reported,
            onPick = {
                reported = it
                picking = false
            },
            onDismiss = { picking = false },
            today = today,
        )
    }
}
