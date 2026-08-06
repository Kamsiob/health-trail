package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectSetupTags {
    const val NAME = "project-setup"
    const val LEAD = "project-setup-lead"
    const val STATUS = "project-setup-status"
    const val STEPS = "project-setup-steps"
    const val STAGES = "project-setup-stages"
    fun lead(value: String) = "project-setup-lead-$value"
    fun status(value: String) = "project-setup-status-$value"
}

/**
 * Everything the template decided, changeable without penalty.
 *
 * `DESIGN.md` 20.5, screen 18. **This is law 5 made concrete.** A template is a
 * starting hand and not a contract, and this screen is where that stops being a
 * claim in a document: the shape, the road, the steps, the papers and the date
 * kinds are all the person's from the moment the project exists.
 *
 * **Changing this project never changes the template**, and the screen says so
 * in words rather than leaving somebody to wonder whether editing here will
 * quietly rewrite what the next project starts from. The reverse is equally
 * true and equally invisible, so it is stated once, plainly.
 *
 * **Nothing here is framed as a penalty.** Putting a project on hold and
 * closing it are ordinary things that happen to long processes, and neither is
 * described as giving up: rule 13, and 20.7's ban on treating a lapse as
 * failure.
 */
@Composable
fun ProjectSetupScreen(
    project: Repository.Project,
    stages: List<Repository.ProjectStage>,
    steps: List<Repository.ProjectStep>,
    papers: List<Repository.ProjectPaper>,
    dateKinds: List<String>,
    onSetLead: (String) -> Unit,
    onSetStatus: (String) -> Unit,
    onOpenSteps: () -> Unit,
    onOpenRoad: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the template it came from is called, where this build still has it. */
    templateName: String? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    SectionScaffold(
        name = ProjectSetupTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.setup",
        subtitle = Bidi.join(
            project.name,
            templateName?.let { strings("project.setup.from_template", "name" to it) }
                ?: strings["project.setup.from_nothing"].takeIf { project.templateId == null },
        ),
        onBack = onBack,
        // **The project, not the projects list.** Setup sits on top of one
        // project and back returns to it. A way back that names the wrong
        // destination is a small lie somebody only notices by being surprised.
        backLabelKey = "section.back.project",
        modifier = modifier,
    ) {
        // **The shape, as three chips.** 20.3: the shape is a default and never
        // a cage, and this is the one control that changes it.
        item {
            Text(
                text = strings["project.setup.lead"],
                style = type.displayS,
                color = colors.ink,
            )
            Text(
                text = strings["project.setup.lead.note"],
                style = type.bodyS,
                color = colors.ink2,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(Space.s))
            FlowRow(
                modifier = Modifier.fillMaxWidth().testTag(ProjectSetupTags.LEAD),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                for (option in listOf("standing", "date", "steps")) {
                    ChoiceChip(
                        label = strings["today.card.project_$option.long"],
                        selected = project.lead == option,
                        // **No confirmation and no warning.** Changing the
                        // shape reorders one screen and loses nothing, so
                        // asking somebody whether they are sure would invent a
                        // consequence that does not exist.
                        onClick = { onSetLead(option) },
                        modifier = Modifier.testTag(ProjectSetupTags.lead(option)),
                    )
                }
            }
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **What the template decided, each with what it has become.** The
        // counts are counts and never scores: "6 steps" is what is there, and
        // rule 13 rules out saying how many of them somebody has got through.
        item {
            GroupedSurface {
                DenseRow(
                    title = strings["project.setup.stages"],
                    subtitle = stages.joinToString(" · ") { it.name }
                        .ifBlank { strings["project.setup.nothing_yet"] },
                    subtitleMaxLines = 2,
                    chevron = true,
                    onClick = onOpenRoad,
                    modifier = Modifier.testTag(ProjectSetupTags.STAGES),
                )
                // **A door, not a line.** 20.5 screen 18 draws this with a
                // chevron, and adding, editing, reordering and removing a step
                // all went with the superseded detail screen: the repository
                // has kept every one of those calls with nothing reachable
                // making them. A screen that says everything is changeable and
                // offers no way to change it is the promise without the thing.
                DenseRow(
                    title = strings["project.setup.steps"],
                    subtitle = strings("projects.step_count", "count" to steps.size),
                    chevron = true,
                    onClick = onOpenSteps,
                    modifier = Modifier.testTag(ProjectSetupTags.STEPS),
                )
                DenseRow(
                    title = strings["project.setup.papers"],
                    subtitle = strings(
                        "project.setup.papers.count",
                        "count" to papers.size,
                        "filled" to papers.count { it.isFilled },
                    ),
                )
                DenseRow(
                    title = strings["project.setup.kinds"],
                    subtitle = dateKinds.joinToString(" · ")
                        .ifBlank { strings["project.setup.nothing_yet"] },
                    subtitleMaxLines = 2,
                    divider = false,
                )
            }
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.setup.never_changes"],
                style = type.bodyS,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **Where the project is, and it is not a verdict.** A long process
        // stalls, waits and gets put down for months, and none of that is the
        // person failing at it.
        item {
            Text(
                text = strings["project.setup.status"],
                style = type.displayS,
                color = colors.ink,
            )
            Text(
                text = strings["project.setup.status.note"],
                style = type.bodyS,
                color = colors.ink2,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(Space.s))
            FlowRow(
                modifier = Modifier.fillMaxWidth().testTag(ProjectSetupTags.STATUS),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                for (option in listOf("active", "waiting", "stalled", "done", "abandoned")) {
                    ChoiceChip(
                        label = strings["projects.status.$option"],
                        selected = project.status == option,
                        onClick = { onSetStatus(option) },
                        modifier = Modifier.testTag(ProjectSetupTags.status(option)),
                    )
                }
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
