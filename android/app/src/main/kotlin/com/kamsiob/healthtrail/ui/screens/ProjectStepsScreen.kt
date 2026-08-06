package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectStepsTags {
    const val NAME = "project-steps"
    const val ADD_FIELD = "project-steps-add-field"
    const val ADD = "project-steps-add"
    fun step(id: String) = "project-steps-$id"
}

/**
 * The starting steps, changed without penalty. `DESIGN.md` 20.5, screen 18.
 *
 * **The one thing law 5 promised and the app stopped doing.** Adding, editing,
 * reordering and removing a step all existed on the superseded detail screen
 * and every one of them went with it: `addProjectStep`, `updateProjectStep`,
 * `moveProjectStep` and `deleteProjectStep` have been in the repository the
 * whole time with nothing reachable calling them. A template that cannot be
 * changed is not a starting point, it is a form.
 *
 * **Nothing here is framed as falling behind.** The steps are listed in their
 * own order with what somebody wrote under them, and rule 13 rules out any
 * count of how many are done: this screen is about what the list says, not
 * about how far through it anybody is. Marking one done belongs on the project
 * itself, which is where somebody actually does it.
 *
 * **One row, one sheet**, law 3 and section 9. Three small controls per row
 * would be three 24dp targets at font scale 1.0 and a wreck at 2.0, so the row
 * is the whole target and everything that can be done to a step is on the sheet
 * it opens.
 */
@Composable
fun ProjectStepsScreen(
    projectName: String,
    steps: List<Repository.ProjectStep>,
    onAdd: (String) -> Unit,
    onOpen: (Repository.ProjectStep) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    var pending by rememberSaveable { mutableStateOf("") }

    SectionScaffold(
        name = ProjectStepsTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.steps.title",
        subtitle = Bidi.isolate(projectName),
        onBack = onBack,
        // **Where it actually goes.** This is reached from setup and returns
        // there, and a way back naming the project is the small lie the
        // scaffold's own comment warns about.
        backLabelKey = "section.back.setup",
        modifier = modifier,
    ) {
        item {
            Text(
                text = strings["project.steps.lead"],
                style = type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **Clustered here too where the steps carry areas**, so the list a
        // person edits is the list they read on the project. Two orders for
        // one set of steps is two mental models for one thing.
        val clustered = steps.groupBy { it.cluster?.takeIf { c -> c.isNotBlank() } }
        val named = clustered.filterKeys { it != null }
        val loose = clustered[null].orEmpty()

        for ((position, entry) in named.entries.withIndex()) {
            val (area, inArea) = entry
            item {
                if (position > 0) Spacer(Modifier.height(Space.m))
                GroupHeaderText(
                    label = area.orEmpty(),
                    count = inArea.size.toString(),
                    countDescription = strings("projects.step_count", "count" to inArea.size),
                )
                Spacer(Modifier.height(Space.xs))
                GroupedSurface {
                    inArea.forEachIndexed { index, step ->
                        StepEditRow(
                            step = step,
                            onOpen = { onOpen(step) },
                            divider = index != inArea.lastIndex,
                        )
                    }
                }
            }
        }

        if (loose.isNotEmpty()) {
            item {
                if (named.isNotEmpty()) Spacer(Modifier.height(Space.m))
                GroupedSurface {
                    loose.forEachIndexed { index, step ->
                        StepEditRow(
                            step = step,
                            onOpen = { onOpen(step) },
                            divider = index != loose.lastIndex,
                        )
                    }
                }
            }
        }

        if (steps.isEmpty()) {
            item {
                Text(
                    // **"Not yet", never a deficiency**, rule 13. A project
                    // whose steps somebody removed is a project they have
                    // decided how to run, not an incomplete one.
                    text = strings["projects.no_steps"],
                    style = type.bodyL,
                    color = colors.ink2,
                )
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            // **The field and the button do not say the same words.** Both
            // read "Add a step", so a reader heard the label twice and had no
            // way to tell the field from the control that acts on it.
            DictatableField(
                label = strings["project.steps.new"],
                value = pending,
                onValueChange = { pending = it },
                hint = strings["project.steps.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = ProjectStepsTags.ADD_FIELD,
            )
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["projects.add_step"],
                enabled = pending.isNotBlank(),
                onClick = {
                    onAdd(pending.trim())
                    pending = ""
                },
                modifier = Modifier.fillMaxWidth().testTag(ProjectStepsTags.ADD),
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/**
 * One step, as a row that opens everything that can be done to it.
 *
 * **The note is shown, not hidden behind the edit sheet.** "The woman on the
 * phone said to call back after the 15th" is the whole reason these processes
 * are survivable, and a note nobody can see without tapping is a note nobody
 * reads.
 */
@Composable
private fun StepEditRow(
    step: Repository.ProjectStep,
    onOpen: () -> Unit,
    divider: Boolean,
) {
    val strings = LocalStrings.current
    DenseRow(
        title = Bidi.isolate(step.text),
        subtitle = step.note?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
            ?: step.handlerLabel?.let { strings("project.step.handled_by", "who" to it) },
        subtitleMaxLines = 3,
        chevron = true,
        onClick = onOpen,
        divider = divider,
        modifier = Modifier.testTag(ProjectStepsTags.step(step.id)),
    )
}
