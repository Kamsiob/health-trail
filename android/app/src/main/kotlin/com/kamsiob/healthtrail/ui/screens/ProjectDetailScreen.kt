package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectDetailTags {
    const val NAME = "project_detail"
    fun step(id: String) = "project_step_$id"
    fun status(key: String) = "project_status_$key"
}

private val STATUSES = listOf("active", "waiting", "stalled", "done", "abandoned")

/**
 * One project: where it stands, and what is left to do.
 *
 * **The steps are the point.** They came from the catalog because somebody
 * otherwise learns this process one missed requirement at a time. They are
 * ordered, and every one can be marked done or put back.
 *
 * **Putting a step back is as ordinary as marking it done.** These processes go
 * backward constantly: a form is returned, a document rejected, a step turns
 * out to have been done wrong. A checklist that only moves forward makes the
 * person lie to it, and then it is worth nothing.
 *
 * **A done step is struck through and quieted, not hidden.** What has already
 * been sent is exactly what somebody is asked about on the phone, and removing
 * it from view to make the list shorter would take away the record.
 *
 * **The status is stated, never colored as a problem.** Waiting and stalled are
 * the normal condition of these processes rather than failures, and the app has
 * no view about how a bureaucracy is going.
 */
@Composable
fun ProjectDetailScreen(
    project: Repository.Project,
    steps: List<Repository.ProjectStep>,
    onToggleStep: (Repository.ProjectStep) -> Unit,
    onSetStatus: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = ProjectDetailTags.NAME,
        title = project.name,
        subtitle = if (steps.isEmpty()) {
            strings["projects.subtitle"]
        } else {
            strings(
                "projects.steps_done",
                "done" to steps.count { it.isDone },
                "total" to steps.size,
            )
        },
        onBack = onBack,
        backLabelKey = "section.back.projects",
        modifier = modifier,
    ) {
        item {
            ChoiceChipGroup(label = strings["money.state"]) {
                STATUSES.forEach { status ->
                    ChoiceChip(
                        label = strings["projects.status.$status"],
                        selected = project.status == status,
                        onClick = { onSetStatus(status) },
                        modifier = Modifier.testTag(ProjectDetailTags.status(status)),
                    )
                }
            }
            Spacer(Modifier.height(Space.l))
        }

        for (step in steps) {
            item(key = step.id) {
                StepRow(step = step, onToggle = { onToggleStep(step) })
                Spacer(Modifier.height(Space.s))
            }
        }

        item { Spacer(Modifier.height(Space.l)) }
    }
}

/**
 * One step, with a box that says whether it is done.
 *
 * The whole row is the target rather than the box alone, because a checkbox
 * sized to a design is smaller than a tired thumb, and the row already carries
 * the words that say what is being ticked.
 */
@Composable
private fun StepRow(step: Repository.ProjectStep, onToggle: () -> Unit) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.card)
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Checkbox,
                onClick = onToggle,
            )
            .semantics {
                // A reader is told the state rather than left to infer it from
                // a shape, which is the same reason chips report as checked.
                stateDescription = if (step.isDone) "done" else "not done"
            }
            .testTag(ProjectDetailTags.step(step.id))
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(Radius.thumbnail)
                .background(if (step.isDone) colors.leaf else Color_Transparent)
                .border(
                    2.dp,
                    if (step.isDone) colors.leaf else colors.ink3NonText,
                    Radius.thumbnail,
                ),
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            text = step.text,
            style = HealthTrail.type.bodyL,
            color = if (step.isDone) colors.ink3Text else colors.ink,
            textDecoration = if (step.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
    }
}

private val Color_Transparent = androidx.compose.ui.graphics.Color.Transparent
