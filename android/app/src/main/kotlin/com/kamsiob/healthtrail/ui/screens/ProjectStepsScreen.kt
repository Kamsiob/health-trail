package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
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
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider

object ProjectStepsTags {
    const val NAME = "project-steps"
    const val ADD_FIELD = "project-steps-add-field"
    const val DONE_FOLD = "project-steps-done"
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
    /**
     * What the way back is called, because there are two ways in.
     *
     * **D164 gave the steps a door on the project itself** and the label stayed
     * "Back to setup", so a screen reached from the project's own file promised
     * to return somebody to a settings screen they had never opened. Back
     * always went to the right place; the words did not, and for a reader the
     * words are the whole of it, rule 19.
     */
    backKey: String = "section.back.project",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    var pending by rememberSaveable { mutableStateOf("") }

    Page(
        title = strings["project.steps.title"],
        onBack = onBack,
        backLabel = strings[backKey],
        modifier = modifier.testTag(SectionTags.root(ProjectStepsTags.NAME)),
        eyebrow = strings["notebook.section.projects"],
        subtitle = Bidi.isolate(projectName),
    ) {
        // **No paragraph about where the steps came from.** "What the template
        // started this with, and whatever you have added since" sat above the
        // steps themselves, said nothing about this project, and is the app
        // explaining its own organizing scheme in the space the content should
        // have, which is rule 20. It is the same sentence D206 took off the
        // project's own screen, and it goes for the same reason.

        // **Clustered here too where the steps carry areas**, so the list a
        // person edits is the list they read on the project. Two orders for
        // one set of steps is two mental models for one thing.
        //
        // **What is done sits under its own quiet label**, which is the grammar
        // the medications list and the projects list already use for what is
        // finished, and it means the steps somebody still has in front of them
        // are the ones at the top. **It is not a count and never a meter**,
        // rule 13: nothing here says how many of somebody's steps are ticked.
        val live = steps.filterNot { it.isDone }
        val done = steps.filter { it.isDone }
        val clustered = live.groupBy { it.cluster?.takeIf { c -> c.isNotBlank() } }
        val named = clustered.filterKeys { it != null }
        val loose = clustered[null].orEmpty()

        for ((position, entry) in named.entries.withIndex()) {
            val (area, inArea) = entry
            item {
                if (position > 0) Spacer(Modifier.height(Space.m))
                Eyebrow(
                    text = Bidi.join(area.orEmpty(), inArea.size.toString()),
                    fixed = false,
                    description = strings("projects.step_count", "count" to inArea.size),
                )
                Spacer(Modifier.height(Space.xs))
                Block(padding = Space.none) {
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
                Block(padding = Space.none) {
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

        if (done.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.m))
                Eyebrow(
                    text = strings["projects.done_fold"],
                    modifier = Modifier.testTag(ProjectStepsTags.DONE_FOLD),
                )
                Spacer(Modifier.height(Space.xs))
                Block(padding = Space.none) {
                    done.forEachIndexed { index, step ->
                        StepEditRow(
                            step = step,
                            onOpen = { onOpen(step) },
                            divider = index != done.lastIndex,
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
                support = strings["project.steps.add.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = ProjectStepsTags.ADD_FIELD,
            )
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["projects.add_step"],
                enabled = pending.isNotBlank(),
                onClick = {
                    onAdd(pending.trim())
                    pending = ""
                },
                modifier = Modifier.testTag(ProjectStepsTags.ADD),
            )
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
    Column {
        ListRow(
            // **The mark every other list in the notebook carries.** D198 item
            // 4: this screen was the one list drawn with no color in it at all,
            // which is what rule 15's "no page is overwhelmingly one color"
            // rules out from the other direction. The card size, not the row
            // size, because every row here is the same kind. #388 finding 8.
            mark = Symbols.standingInstructions,
            markSize = Space.markCard,
            markHue = hueFor(Repository.Section.PROJECTS),
            title = Bidi.isolate(step.text),
            support = step.note?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
                ?: step.handlerLabel?.let { strings("project.step.handled_by", "who" to it) },
            // **A step that has been taken says so, and one that has not says
            // nothing.** Rule 13: an unfilled slot reads as "not yet" rather
            // than as an error, so there is no empty box down the list and
            // nothing counts how many of them are ticked. The state is set in
            // the sheet the row opens, and `project_step.is_done` had been in
            // the schema with nothing in the app able to write it. #390.
            //
            // bidi-ok: the app's own word for the state, from the catalog.
            value = strings["project.step.done"].takeIf { step.isDone },
            isDoor = true,
            onClick = onOpen,
            modifier = Modifier.testTag(ProjectStepsTags.step(step.id)),
        )
        if (divider) RowDivider(inset = false)
    }
}
