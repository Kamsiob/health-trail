package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.IconAction
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.Road
import com.kamsiob.healthtrail.ui.v4.Stop
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object MedicationTags {
    const val NAME = "medication"
    const val EDIT = "medication_edit"
    const val RECORD = "medication_record"
    const val REMOVE = "medication_remove"
    fun question(id: String) = "medication_question_$id"
    fun event(id: String) = "medication_event_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_medication"
}

/**
 * One medication, and how it changed. Rewritten onto `ui/v4`, #386.
 *
 * `MASTER_SPEC.md` 4.6: a medication's journey crosses chapters and keeps its
 * concern flags attached forever. **That journey is what makes this a record
 * rather than a list.** "She was on this until March, and it was changed at the
 * rehab" is the answer to a question somebody is eventually asked in a room
 * where nobody has the notes.
 *
 * **This app records medications and does not track them.** No reminders, no
 * doses taken, no adherence, and nothing here says whether any of it was right.
 * Rule 2.
 *
 * **The history is a road and the questions are a list.** D187: what happened to
 * this medication is a path, oldest first, because it is a story about how
 * something changed; what is still to ask about it is not a sequence and does
 * not get a line drawn down it.
 */
@Composable
fun MedicationScreen(
    medication: Repository.Medication,
    history: List<Repository.MedicationEvent>,
    /**
     * Questions waiting to be asked about this one.
     *
     * `MASTER_SPEC.md` 3 promises exactly this and it had no data behind it:
     * `question.medication_id` sat in the schema with no writer and no reader,
     * so a question about a dose change lived only in the questions section as a
     * sentence with a drug name in it.
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
    val hue = hueFor(Repository.Section.MEDICATIONS)

    Page(
        eyebrow = strings["notebook.section.medications"],
        eyebrowColor = hue.ink,
        title = Bidi.isolate(medication.name),
        subtitle = medication.doseText?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
            ?: strings["medication.nodose"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        actions = {
            IconAction(
                symbol = Symbols.edit,
                label = strings["medication.edit"],
                onClick = onEdit,
                modifier = Modifier.testTag(MedicationTags.EDIT),
            )
        },
        modifier = modifier.testTag(MedicationTags.ROOT),
    ) {
        item {
            Block(tone = BlockTone.Section, hue = hue) {
                medication.purposeText?.takeIf { it.isNotBlank() }?.let {
                    Body(text = Bidi.isolate(it), color = colors.ink, style = HealthTrail.type.bodyL)
                }

                // **Both facts stated in words**, so neither depends on
                // noticing a color or a mark. `DESIGN.md` 9.
                listOfNotNull(
                    if (medication.isStopped) {
                        medication.stoppedEdtf?.takeIf { it.isNotBlank() }?.let {
                            strings(
                                "medication.stopped.on",
                                "date" to EventDateText.render(strings, it),
                            )
                        } ?: strings["medication.stopped"]
                    } else {
                        null
                    },
                    // Same as the list: the flag is stored, but a stopped
                    // medication is not on the card, so saying it is would be
                    // the record lying about itself.
                    strings["medication.on.card"].takeIf { medication.showsOnEmergencyCard },
                ).forEach {
                    // bidi-ok: a catalog label, in the app's own words rather
                    // than the person's.
                    Body(text = it, color = hue.ink)
                }

                medication.notes?.takeIf { it.isNotBlank() }?.let {
                    Body(text = Bidi.isolate(it))
                }

                // **Writing down a change comes before correcting the record**,
                // because a dose changing is the ordinary event and a typo is
                // the rare one.
                Action(
                    label = strings["medication.record"],
                    onClick = onRecordChange,
                    mark = Symbols.add,
                    modifier = Modifier.testTag(MedicationTags.RECORD),
                )
            }
        }

        // **Before the history, because it is the thing to act on.** The history
        // is what happened; these are what to do about it, and somebody opening
        // this screen on the way into a room needs them first.
        labeledBlock(
            label = strings["medication.questions"],
            rows = questions.map { question ->
                {
                    ListRow(
                        title = Bidi.isolate(question.text),
                        support = question.roleLabel?.takeIf { it.isNotBlank() }
                            ?.let { Bidi.isolate(it) },
                        mark = Symbols.askNextTime,
                        markTint = colors.blueDeep,
                        markWash = colors.blueWash,
                        isDoor = true,
                        onClick = { onOpenQuestion(question) },
                        clickLabel = strings["prep.change.open"],
                        modifier = Modifier.testTag(MedicationTags.question(question.id)),
                    )
                }
            },
        )

        item { Eyebrow(text = strings["medication.history"]) }

        if (history.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about a medication with
                    // nothing written about it yet.
                    Body(text = strings["medication.history.empty"])
                }
            }
        }

        // **One item for the whole road**, because a page puts air between its
        // items and air between two stops is a road with gaps in it.
        if (history.isNotEmpty()) {
            item {
                Column {
                    history.forEachIndexed { index, event ->
                        Road(
                            // Oldest first, so the road reads forward: the first
                            // thing that happened to it is where it started, and
                            // everything since has happened too.
                            stop = Stop.Done,
                            continuesAbove = index > 0,
                            continuesBelow = index < history.lastIndex,
                        ) {
                            Column(modifier = Modifier.padding(bottom = Space.sm)) {
                                Block(modifier = Modifier
                                    .semantics(mergeDescendants = true) { }
                                    .testTag(MedicationTags.event(event.id))) {
                                    // Same as the incident's, and for the same
                                    // reason: a date and a chapter name are two
                                    // runs.
                                    val eyebrow = Bidi.join(
                                        listOfNotNull(
                                            event.occurredEdtf?.takeIf { it.isNotBlank() }
                                                ?.let { EventDateText.render(strings, it) },
                                            event.chapterName?.takeIf { it.isNotBlank() },
                                        ),
                                    )
                                    if (eyebrow.isNotEmpty()) {
                                        Eyebrow(text = eyebrow, fixed = false)
                                    }
                                    Body(
                                        text = strings[medicationEventKey(event.kind)],
                                        color = colors.ink,
                                        style = HealthTrail.type.displayS,
                                    )
                                    event.doseText?.takeIf { it.isNotBlank() }?.let {
                                        Body(text = Bidi.isolate(it))
                                    }
                                    event.note?.takeIf { it.isNotBlank() }?.let {
                                        Body(text = Bidi.isolate(it))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // **Taking it out of the notebook, from the medication's own screen**,
        // per #218. Last, because it is the rarest errand here and because the
        // history above it is what somebody came to read.
        item {
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(MedicationTags.REMOVE),
            )
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
