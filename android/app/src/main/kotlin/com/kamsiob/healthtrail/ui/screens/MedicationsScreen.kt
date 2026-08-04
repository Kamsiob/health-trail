package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.DenseRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object MedsTags {
    const val NAME = "medications"
    const val ADD = "medications_add"
    fun row(id: String) = "medication_$id"
    const val STOPPED_FOLD = "medications_stopped_fold"
}

/**
 * Medications: what they take, written down.
 *
 * **Record only, and the screen says so out loud.** `MASTER_SPEC.md` section
 * 4.3 requires it to state plainly that the app does not remind or alert, and
 * that sentence is in the subtitle rather than buried. Somebody arriving at a
 * screen called Medications reasonably expects reminders, and the honest place
 * to correct that expectation is the moment they arrive, not after they have
 * relied on it.
 *
 * **A dose is never parsed into a number and a unit.** The schema says so and
 * this screen shows the sentence the person was told, in the words they were
 * told it in. Misparsing a dose is worse than not parsing one, and there is
 * nothing the app could do with a number that it cannot do with the sentence.
 *
 * **A stopped medication stays.** It moves below the current ones and says it
 * stopped, because "she was on this until March" is the answer to a question
 * somebody will eventually be asked, and a record that quietly drops what
 * ended is not a record.
 */
@Composable
fun MedicationsScreen(
    medications: List<Repository.Medication>,
    onRemove: (Repository.Medication) -> Unit,
    onOpen: (Repository.Medication) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var stoppedOpen by rememberSaveable { mutableStateOf(false) }

    SectionScaffold(
        name = MedsTags.NAME,
        title = strings["notebook.section.medications"],
        subtitle = strings["meds.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.MEDICATIONS,
        headingKey = "meds.heading",
    ) {
        if (medications.isEmpty()) {
            item {
                SectionEmpty(name = MedsTags.NAME, text = strings["meds.empty"], section = Repository.Section.MEDICATIONS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        // **Current medications lead, stopped ones fold**, per grid screen 12
        // and law 4: a finished group collapses rather than sitting among the
        // ones that are still true.
        //
        // **Stopped is kept forever and never hidden**, which is the whole
        // point of a record. The fold names them and counts them.
        val current = medications.filterNot { it.isStopped }
        val stopped = medications.filter { it.isStopped }

        if (current.isNotEmpty()) {
            item {
                GroupedSurface {
                    current.forEachIndexed { index, medication ->
                        MedicationRow(
                            medication = medication,
                            onRemove = { onRemove(medication) },
                            onOpen = { onOpen(medication) },
                            isLast = index == current.lastIndex,
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        if (stopped.isNotEmpty()) {
            item {
                FoldRow(
                    labelKey = "meds.stopped.fold",
                    expanded = stoppedOpen,
                    onToggle = { stoppedOpen = !stoppedOpen },
                    count = stopped.size.toString(),
                    modifier = Modifier.testTag(MedsTags.STOPPED_FOLD),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            if (stoppedOpen) {
                item {
                    GroupedSurface {
                        stopped.forEachIndexed { index, medication ->
                            MedicationRow(
                                medication = medication,
                                onRemove = { onRemove(medication) },
                                onOpen = { onOpen(medication) },
                                isLast = index == stopped.lastIndex,
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["meds.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(MedsTags.ADD),
            )
        }
    }
}

/**
 * One medication.
 *
 * The name carries the weight and the dose sits with it, because those are the
 * two things somebody reads together. What it is for and anything else recede.
 *
 * **The emergency card marker is words, never a color alone**, per section 9,
 * and it is stated rather than styled as a badge competing with the name.
 */
@Composable
private fun MedicationRow(
    medication: Repository.Medication,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
    isLast: Boolean,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **The dose is the trailing value, in Mono and tabular**, per grid screen
    // 12 and `DESIGN.md` 5: a dose is data, so it aligns down the column and a
    // person can compare two of them without reading a sentence twice.
    //
    // **Everything else that was on the card becomes the second line.** A
    // medication is a name, a dose and one line of why: that is a row somebody
    // scans, not a card somebody reads.
    val detail = listOfNotNull(
        strings["meds.stopped"].takeIf { medication.isStopped },
        medication.purposeText?.takeIf { it.isNotBlank() },
        medication.notes?.takeIf { it.isNotBlank() },
        // **Only while it is true.** The row used to render the stored flag
        // rather than the effect of it, so a stopped medication claimed to be
        // on a card it is not on. The list contradicted the card, and the card
        // is the one somebody hands to a paramedic.
        strings["meds.on_card.badge"].takeIf { medication.showsOnEmergencyCard },
    ).let { Bidi.join(it) }.takeIf { it.isNotBlank() }

    DenseRow(
        title = medication.name,
        subtitle = detail,
        trailing = medication.doseText?.takeIf { it.isNotBlank() },
        chevron = true,
        divider = !isLast,
        onClick = onOpen,
        modifier = Modifier
            .removableByLongPress(strings["edit.hint"], onRemove, onOpen)
            .testTag(MedsTags.row(medication.id)),
    )
}
