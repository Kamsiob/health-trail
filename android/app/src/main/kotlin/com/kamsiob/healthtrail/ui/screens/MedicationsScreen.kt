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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object MedsTags {
    const val NAME = "medications"
    const val ADD = "medications_add"
    fun row(id: String) = "medication_$id"
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
    onEdit: (Repository.Medication) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = MedsTags.NAME,
        title = strings["notebook.section.medications"],
        subtitle = strings["meds.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (medications.isEmpty()) {
            item {
                SectionEmpty(name = MedsTags.NAME, text = strings["meds.empty"], section = Repository.Section.MEDICATIONS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        for (medication in medications) {
            item(key = medication.id) {
                MedicationRow(
                    medication = medication,
                    onRemove = { onRemove(medication) },
                    onEdit = { onEdit(medication) },
                )
                Spacer(Modifier.height(Space.cardGap))
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
    onEdit: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .removableByLongPress(strings["edit.hint"], onRemove, onEdit)
            .testTag(MedsTags.row(medication.id))
            .padding(Space.cardPadding),
    ) {
        // A stopped medication says so before its name, so the state is read
        // first rather than discovered after.
        if (medication.isStopped) {
            Text(
                text = strings["meds.stopped"],
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = medication.name,
            style = HealthTrail.type.displayS,
            // A stopped medication is quieter but never struck through: it is
            // still true, it is simply no longer current.
            color = if (medication.isStopped) colors.ink2 else colors.ink,
        )

        medication.doseText?.takeIf { it.isNotBlank() }?.let { dose ->
            Spacer(Modifier.height(Space.xs))
            Text(text = dose, style = HealthTrail.type.bodyL, color = colors.ink2)
        }

        medication.purposeText?.takeIf { it.isNotBlank() }?.let { purpose ->
            Spacer(Modifier.height(Space.xs))
            Text(text = purpose, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        medication.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.xs))
            Text(text = notes, style = HealthTrail.type.bodyS, color = colors.ink3Text)
        }

        if (medication.onEmergencyCard) {
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = strings["meds.on_card.badge"],
                    style = HealthTrail.type.mono,
                    color = colors.alertText,
                )
            }
        }
    }
}
