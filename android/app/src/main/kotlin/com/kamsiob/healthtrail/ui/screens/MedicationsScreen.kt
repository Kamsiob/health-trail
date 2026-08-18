package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object MedsTags {
    const val NAME = "medications"
    const val ADD = "medications_add"
    fun row(id: String) = "medication_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_medications"
}

/**
 * Medications: what they take, written down. Rewritten onto `ui/v4`, #386.
 *
 * **Record only, and the screen says so out loud.** `MASTER_SPEC.md` 4.3
 * requires it to state plainly that the app does not remind or alert, and that
 * sentence is the page's own line rather than something buried. Somebody
 * arriving at a screen called Medications reasonably expects reminders, and the
 * honest place to correct that expectation is the moment they arrive.
 *
 * **A dose is never parsed into a number and a unit.** The schema says so and
 * this screen shows the sentence the person was told, in the words they were
 * told it in. Misparsing a dose is worse than not parsing one.
 *
 * **A stopped medication stays, and it is no longer behind a fold.** "She was
 * on this until March" is the answer to a question somebody will eventually be
 * asked. It sits under its own label below the current ones, which says
 * finished without hiding it and without asking for a tap. D185.
 */
@Composable
fun MedicationsScreen(
    medications: List<Repository.Medication>,
    onOpen: (Repository.Medication) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * How many questions are still waiting on each medication, by id. #352.
     *
     * **A medication with none is absent from the map** rather than present with
     * a zero, so a row cannot say "0 questions" by accident.
     */
    openQuestions: Map<String, Int> = emptyMap(),
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val hue = hueFor(Repository.Section.MEDICATIONS)

    val current = medications.filterNot { it.isStopped }
    val stopped = medications.filter { it.isStopped }

    Page(
        eyebrow = strings["notebook.section.medications"],
        section = Repository.Section.MEDICATIONS,
        eyebrowColor = hue.ink,
        title = strings["meds.heading"],
        subtitle = strings["meds.subtitle"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(MedsTags.ROOT),
    ) {
        // **An empty section is a sentence on a quiet block**, which is the
        // shape every v4 screen uses for "nothing here yet": the words are the
        // content and rule 13 forbids reading an unfilled thing as a failure.
        if (medications.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about an empty list.
                    Body(
                        text = strings["meds.empty"],
                        color = HealthTrail.colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        labeledBlock(
            leading = true,
            label = null,
            rows = current.map { medication ->
                { MedicationRow(medication, openQuestions[medication.id] ?: 0, hue, onOpen) }
            },
        )

        labeledBlock(
            label = strings["meds.stopped"].takeIf { stopped.isNotEmpty() },
            rows = stopped.map { medication ->
                { MedicationRow(medication, openQuestions[medication.id] ?: 0, hue, onOpen) }
            },
        )

        item {
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["meds.add"],
                onClick = onAdd,
                mark = Symbols.add,
                modifier = Modifier.testTag(MedsTags.ADD),
            )
        }
    }
}

/**
 * One medication: the name, the dose beside it, and one line of why.
 *
 * **The dose is the trailing value, in the mono face and tabular**, per
 * `DESIGN.md` 5: a dose is data, so it lines up down the column and two of them
 * can be compared without reading a sentence twice.
 *
 * **The emergency card marker is words, never a color alone**, per section 9,
 * and it is stated rather than styled as a badge competing with the name.
 */
@Composable
private fun MedicationRow(
    medication: Repository.Medication,
    /** Questions attached to it and not asked yet. Zero says nothing at all. */
    waiting: Int,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onOpen: (Repository.Medication) -> Unit,
) {
    val strings = LocalStrings.current

    val detail = listOfNotNull(
        strings["meds.stopped"].takeIf { medication.isStopped },
        // **A question waiting leads the second line**, after the stopped
        // marker, which is state. Rule 15: the thing somebody can act on gets
        // the position.
        strings("meds.questions.waiting", "count" to waiting).takeIf { waiting > 0 },
        medication.purposeText?.takeIf { it.isNotBlank() },
        medication.notes?.takeIf { it.isNotBlank() },
        // **Only while it is true.** The row used to render the stored flag
        // rather than the effect of it, so a stopped medication claimed to be
        // on a card it is not on. The list contradicted the card, and the card
        // is the one somebody hands to a paramedic.
        strings["meds.on_card.badge"].takeIf { medication.showsOnEmergencyCard },
    ).let { Bidi.join(it) }.takeIf { it.isNotBlank() }

    ListRow(
        title = Bidi.isolate(medication.name),
        support = detail,
        mark = Symbols.medications,
        markTint = hue.ink,
        markWash = hue.wash,
        // **Isolated, like the name beside it.** The list showed a dose raw
        // while the medication's own screen isolated the same string, which is
        // one record reading two ways. Seen in Arabic on the phone.
        // One placement for the whole column, whatever each dose happens
        // to be long enough for on its own.
        valueBelow = true,
        value = medication.doseText?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) },
        onClick = { onOpen(medication) },
        clickLabel = strings["open.action"],
        modifier = Modifier.testTag(MedsTags.row(medication.id)),
    )
}
