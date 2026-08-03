package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object MedicationTags {
    const val NAME = "medication"
    const val EDIT = "medication_edit"
    const val RECORD = "medication_record"
    fun event(id: String) = "medication_event_$id"
}

/**
 * One medication, and how it changed.
 *
 * `MASTER_SPEC.md` 4.6: a medication's journey crosses chapters and keeps its
 * concern flags attached forever. **That journey is what makes this a record
 * rather than a list.** "She was on this until March, and it was changed at the
 * rehab" is the answer to a question somebody is eventually asked in a room
 * where nobody has the notes.
 *
 * **This app records medications and does not track them.** No reminders, no
 * doses taken, no adherence, and nothing here says whether any of it was right.
 * Rule 2, and the medications screen says so in its own subtitle.
 *
 * **The spine is continuous**, because a medication's history is the thing
 * itself rather than a filter over the record, which is the same reason an
 * incident thread and a chapter both get one.
 *
 * **Oldest first**, because it is a story about how something changed.
 */
@Composable
fun MedicationScreen(
    medication: Repository.Medication,
    history: List<Repository.MedicationEvent>,
    onEdit: () -> Unit,
    onRecordChange: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.medications",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = MedicationTags.NAME,
        title = medication.name,
        subtitle = medication.doseText?.takeIf { it.isNotBlank() }
            ?: strings["medication.nodose"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        item {
            medication.purposeText?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.m))
            }

            // **Both facts stated in words**, per 2.2, so neither depends on
            // noticing a color or a mark.
            val facts = listOfNotNull(
                if (medication.isStopped) {
                    medication.stoppedEdtf?.takeIf { it.isNotBlank() }?.let {
                        strings("medication.stopped.on", "date" to EventDateText.render(strings, it))
                    } ?: strings["medication.stopped"]
                } else {
                    null
                },
                // Same as the list: the flag is stored, but a stopped
                // medication is not on the card, so saying it is would be the
                // record lying about itself.
                if (medication.showsOnEmergencyCard) {
                    strings["medication.on.card"]
                } else {
                    null
                },
            )
            facts.forEach {
                Text(text = it, style = HealthTrail.type.mono, color = colors.ink3Text)
                Spacer(Modifier.height(Space.xs))
            }

            medication.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Space.s))
                Text(text = it, style = HealthTrail.type.bodyM, color = colors.ink2)
            }

            Spacer(Modifier.height(Space.sectionGap))
            // **Writing down a change comes before correcting the record**,
            // because a dose changing is the ordinary event and a typo is the
            // rare one.
            QuietButton(
                label = strings["medication.record"],
                onClick = onRecordChange,
                modifier = Modifier.fillMaxWidth().testTag(MedicationTags.RECORD),
            )
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["medication.edit"],
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().testTag(MedicationTags.EDIT),
            )
            Spacer(Modifier.height(Space.sectionGap))

            GroupHeader(labelKey = "medication.history")
            Spacer(Modifier.height(Space.headerGap))
        }

        if (history.isEmpty()) {
            item {
                Text(
                    text = strings["medication.history.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))
            }
            return@SectionScaffold
        }

        history.forEachIndexed { index, event ->
            item(key = event.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < history.lastIndex,
                    node = colors.blue,
                    // The first thing that happened to it is where it started.
                    state = if (index == 0) Waypoint.MILESTONE else Waypoint.HAPPENED,
                    routeColor = colors.blue,
                    dash = null,
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .clip(Radius.card)
                                .background(colors.card)
                                .testTag(MedicationTags.event(event.id))
                                .padding(Space.cardPadding),
                        ) {
                            val eyebrow = listOfNotNull(
                                event.occurredEdtf?.takeIf { it.isNotBlank() }
                                    ?.let { EventDateText.render(strings, it) },
                                event.chapterName?.takeIf { it.isNotBlank() },
                            ).joinToString("  ")
                            if (eyebrow.isNotEmpty()) {
                                Text(
                                    text = eyebrow,
                                    style = HealthTrail.type.mono,
                                    color = colors.ink3Text,
                                )
                                Spacer(Modifier.height(Space.xs))
                            }
                            Text(
                                text = strings[medicationEventKey(event.kind)],
                                style = HealthTrail.type.displayS,
                                color = colors.ink,
                            )
                            event.doseText?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(text = it, style = HealthTrail.type.bodyM, color = colors.ink2)
                            }
                            event.note?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(text = it, style = HealthTrail.type.bodyM, color = colors.ink2)
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }
    }
}

/**
 * What a change to a medication is called, in the person's words.
 *
 * The schema's own vocabulary, translated rather than shown raw, because
 * "dose_changed" is a column name and not something anybody says.
 */
/**
 * The catalog key for a kind of change.
 *
 * **Built by interpolation, which meant the database could crash the screen.**
 * `Strings.resolve` throws on a key no catalog defines, deliberately, so a
 * translation gap is loud rather than silent. That is right for a key the code
 * writes and wrong for one assembled out of a column: `medication.event.noted`
 * was a real value in the schema's CHECK constraint that no catalog had, and
 * opening any medication whose history contained one took the whole app down.
 *
 * A `when` over the six kinds the schema names, so what reaches the catalog is
 * chosen here rather than by whatever is in the row. Anything else falls back
 * to the neutral one instead of throwing, which is what an imported notebook
 * from a later version of the app will eventually need.
 */
internal fun medicationEventKey(kind: String): String = when (kind) {
    "started" -> "medication.event.started"
    "stopped" -> "medication.event.stopped"
    "dose_changed" -> "medication.event.dose_changed"
    "held" -> "medication.event.held"
    "resumed" -> "medication.event.resumed"
    else -> "medication.event.noted"
}
