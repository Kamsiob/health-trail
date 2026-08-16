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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object MedicationTags {
    const val NAME = "medication"
    const val EDIT = "medication_edit"
    const val RECORD = "medication_record"
    const val REMOVE = "medication_remove"
    fun question(id: String) = "medication_question_$id"
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
    /**
     * Questions waiting to be asked about this one.
     *
     * `MASTER_SPEC.md` section 3 promises exactly this and it had no data
     * behind it: `question.medication_id` sat in the schema with no writer and
     * no reader, so a question about a dose change lived only in the questions
     * section as a sentence with a drug name in it.
     */
    questions: List<Repository.Question>,
    onOpenQuestion: (Repository.Question) -> Unit,
    onEdit: () -> Unit,
    /**
     * Taking the medication out of the notebook, per #218. Opens the
     * confirmation, which is the only thing that removes anything.
     */
    onRemove: () -> Unit,
    onRecordChange: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.medications",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        onEdit = onEdit,
        editTag = MedicationTags.EDIT,
        editLabel = strings["medication.edit"],
        name = MedicationTags.NAME,
        // **The chip says where you are, the heading says what you came for.**
        // This passed the record's own words as the title, which put them in an
        // 11sp mono chip and again underneath at display weight: the same label
        // in two slots, which section 1 bans. #189 gave the scaffold a heading
        // for exactly this, and every detail screen inherits it.
        title = strings["notebook.section.medications"],
        heading = Bidi.isolate(medication.name),
        section = Repository.Section.MEDICATIONS,
        subtitle = medication.doseText?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
            ?: strings["medication.nodose"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        item {
            medication.purposeText?.takeIf { it.isNotBlank() }?.let {
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
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
                // bidi-ok: a catalog label, in the app's own words rather than the person's.
                Text(text = it, style = HealthTrail.type.mono, color = colors.ink2)
                Spacer(Modifier.height(Space.xs))
            }

            medication.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Space.s))
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyM, color = colors.ink2)
            }

            Spacer(Modifier.height(Space.sectionGap))
            // **Writing down a change comes before correcting the record**,
            // because a dose changing is the ordinary event and a typo is the
            // rare one.
            QuietButton(
                label = strings["medication.record"],
                onClick = onRecordChange,
                modifier = Modifier.testTag(MedicationTags.RECORD),
            )
            Spacer(Modifier.height(Space.cardGap))
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **Before the history, because it is the thing to act on.** The
        // history is what happened; these are what to do about it, and somebody
        // opening this screen on the way into a room needs them first.
        if (questions.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "medication.questions")
                Spacer(Modifier.height(Space.headerGap))
            }
            questions.forEachIndexed { index, question ->
                item(key = "q_${question.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < questions.lastIndex,
                        node = colors.blue,
                        // Hollow: nobody has asked it yet. Same shape the prep
                        // sheet uses for the same state, per DESIGN.md 5.2.1.
                        state = Waypoint.UPCOMING,
                        routeColor = colors.blue,
                    ) {
                        Column {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) { }
                                    .clip(Radius.cardLarge)
                                    // #231: the tap opens the question's own
                                    // entry, and now says so.
                                    .openableByTap(
                                        label = strings["prep.change.open"],
                                        onTap = { onOpenQuestion(question) },
                                        resting = colors.card,
                                    )
                                    .testTag(MedicationTags.question(question.id))
                                    .padding(Space.cardPadding),
                            ) {
                                question.roleLabel?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = Bidi.isolate(it),
                                        style = HealthTrail.type.mono,
                                        color = colors.ink2,
                                    )
                                    Spacer(Modifier.height(Space.xs))
                                }
                                Text(
                                    text = Bidi.isolate(question.text),
                                    style = HealthTrail.type.bodyL,
                                    color = colors.ink,
                                )
                            }
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Space.sectionGap)) }
        }

        item {
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
                                .clip(Radius.cardLarge)
                                .background(colors.card)
                                .testTag(MedicationTags.event(event.id))
                                .padding(Space.cardPadding),
                        ) {
                            // Same as the incident's, and for the same
                            // reason: a date and a chapter name are two runs.
                            val eyebrow = Bidi.join(
                                listOfNotNull(
                                    event.occurredEdtf?.takeIf { it.isNotBlank() }
                                        ?.let { EventDateText.render(strings, it) },
                                    event.chapterName?.takeIf { it.isNotBlank() },
                                ),
                            )
                            if (eyebrow.isNotEmpty()) {
                                Text(
                                    text = eyebrow,
                                    style = HealthTrail.type.bodyS,
                                    color = colors.ink2,
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
                                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyM, color = colors.ink2)
                            }
                            event.note?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyM, color = colors.ink2)
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }

        // **Taking it out of the notebook, from the medication's own screen**,
        // per #218. Last, because it is the rarest errand here and because the
        // history above it is what somebody came to read.
        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(MedicationTags.REMOVE),
            )
            Spacer(Modifier.height(Space.l))
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
