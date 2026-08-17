package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider

object ProjectSetupTags {
    const val NAME = "project-setup"
    const val LEAD = "project-setup-lead"
    const val STATUS = "project-setup-status"
    const val STEPS = "project-setup-steps"
    const val STAGES = "project-setup-stages"
    const val KINDS = "project-setup-kinds"
    const val PAPERS = "project-setup-papers"
    const val WAITING = "project-setup-waiting"
    const val SAVE_WAITING = "project-setup-waiting-save"
    const val SAVE_TEMPLATE = "project-setup-save-template"
    const val SAVED_TEMPLATE = "project-setup-saved-template"
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
    onSetWaitingOn: (String) -> Unit,
    onSaveAsTemplate: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenRoad: () -> Unit,
    onOpenKinds: () -> Unit,
    onOpenPapers: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the template it came from is called, where this build still has it. */
    templateName: String? = null,
    /** Whether this session has already kept this project as a template. */
    savedAsTemplate: Boolean = false,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    // **Held in the screen and written on an explicit save.** Writing on every
    // keystroke would bump the revision and append to the change log once per
    // letter, which the data contract would carry and nobody should have to
    // read. Keyed on the stored value so a save, or a change made anywhere
    // else, lands in the field rather than being overwritten by a stale draft.
    var waitingOn by remember(project.id, project.waitingOn) {
        mutableStateOf(project.waitingOn.orEmpty())
    }

    Page(
        title = strings["project.setup"],
        onBack = onBack,
        backLabel = strings["section.back.project"],
        modifier = modifier.testTag(SectionTags.root(ProjectSetupTags.NAME)),
        eyebrow = strings["notebook.section.projects"],
        subtitle = Bidi.join(
            project.name,
            templateName?.let { strings("project.setup.from_template", "name" to it) }
                ?: strings["project.setup.from_nothing"].takeIf { project.templateId == null },
        ),
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
        }

        // **What the template decided, each with what it has become.** The
        // counts are counts and never scores: "6 steps" is what is there, and
        // rule 13 rules out saying how many of them somebody has got through.
        item {
            Block(padding = Space.none) {
                ListRow(
                    title = strings["project.setup.stages"],
                    // **Joined by Bidi rather than by a dot**, because these
                    // are names a family typed and a run of them in one script
                    // inside a layout in another reorders without an isolate
                    // around each. `Bidi.join` also drops the blanks, so a
                    // stage with no name cannot produce a stray separator.
                    support = Bidi.join(stages.map { it.name })
                        .ifBlank { strings["project.setup.nothing_yet"] },
                    isDoor = true,
                    onClick = onOpenRoad,
                    modifier = Modifier.testTag(ProjectSetupTags.STAGES),
                )
                RowDivider(inset = false)
                // **A door, not a line.** 20.5 screen 18 draws this with a
                // chevron, and adding, editing, reordering and removing a step
                // all went with the superseded detail screen: the repository
                // has kept every one of those calls with nothing reachable
                // making them. A screen that says everything is changeable and
                // offers no way to change it is the promise without the thing.
                ListRow(
                    title = strings["project.setup.steps"],
                    support = strings("projects.step_count", "count" to steps.size),
                    isDoor = true,
                    onClick = onOpenSteps,
                    modifier = Modifier.testTag(ProjectSetupTags.STEPS),
                )
                RowDivider(inset = false)
                ListRow(
                    title = strings["project.setup.papers"],
                    // **How many places there are, and not how many are
                    // filled.** `ProjectPaperworkScreen` says why in as many
                    // words: a project with six places and two filled is
                    // somebody waiting on other people's post, and a
                    // completion count pointed at that is what rule 13 rules
                    // out. This row was counting it anyway, which is one
                    // decision written down twice and disagreeing with itself.
                    // Seen on a brand new notebook, where it read "0 filled"
                    // about somebody who had had the app for a minute.
                    //
                    // **The papers screen's own key**, rather than a second one
                    // saying nearly the same thing in four languages. D133.
                    support = strings(
                        "project.paperwork.count",
                        "count" to papers.size,
                    ),
                    isDoor = true,
                    onClick = onOpenPapers,
                    modifier = Modifier.testTag(ProjectSetupTags.PAPERS),
                )
                RowDivider(inset = false)
                ListRow(
                    title = strings["project.setup.kinds"],
                    support = Bidi.join(dateKinds)
                        .ifBlank { strings["project.setup.nothing_yet"] },
                    isDoor = true,
                    onClick = onOpenKinds,
                    modifier = Modifier.testTag(ProjectSetupTags.KINDS),
                )
            }
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.setup.never_changes"],
                style = type.bodyS,
                color = colors.ink2,
            )
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

            // **Who you are waiting on, beside the status and not behind it.**
            // The schema makes it first class because it is the most useful
            // field a project has, and it is always offered rather than only
            // when the status says "waiting": somebody is usually waiting on
            // somebody long before they think to change a status.
            //
            // It went with the superseded detail screen and its repository call
            // and shell state have been sitting here with nothing setting them
            // ever since, which is #314.
            Spacer(Modifier.height(Space.m))
            Field(
                label = strings["projects.waiting_field"],
                value = waitingOn,
                onValueChange = { waitingOn = it },
                fieldTestTag = ProjectSetupTags.WAITING,
                support = strings["projects.waiting_field.hint"],
            )

            // **Only drawn when it would do something.** An empty field that
            // has not been touched has nothing to save, and a control that does
            // nothing on press reads as broken, per rule 16 and D42.
            //
            // **Sized to its label rather than to the screen.** A full width
            // outlined button is what this app puts at the foot of a screen to
            // mean "the way back", and drawing one here made a save for one
            // field look like the screen's own footer. In content actions are
            // pills, the way the project's own screen draws "Log a call".
            if (waitingOn.trim() != project.waitingOn.orEmpty().trim()) {
                Spacer(Modifier.height(Space.s))
                Action(
                    label = strings["projects.waiting_save"],
                    onClick = { onSetWaitingOn(waitingOn.trim()) },
                    modifier = Modifier.testTag(ProjectSetupTags.SAVE_WAITING),
                )
            }
        }

        // **Keeping what this project turned into, for the next one like it.**
        // Somebody who has spent four months learning what a Medicaid
        // application actually needs should not have to learn it twice, and
        // this is what makes editing a shipped template their own copy rather
        // than a change to the catalog: `custom_template.derived_from_id` keeps
        // the lineage, so a later version's catalog can never overwrite it.
        //
        // **It sits here rather than on the project's own screen** because it
        // is a decision about the template library rather than about this
        // project, and it is last because it is the rarest thing on the screen.
        item {
            // **Only when there is a shape to keep.** A template made from a
            // project with no road, no steps, no papers and no date kinds is a
            // name with nothing under it. The lead alone is not enough, because
            // every project has one whether or not anybody chose it.
            val hasShape = stages.isNotEmpty() || steps.isNotEmpty() ||
                papers.isNotEmpty() || dateKinds.isNotEmpty()
            if (hasShape) {
                // **A section of its own, headed like its two peers.** Without
                // a heading this was a button floating under the waiting-on
                // field, which read as that field's save control, and its saved
                // state was one gray sentence alone at the foot of the screen.
                // Rule 15: the block is named, then the rest recedes.
                Text(
                    text = strings["project.setup.template"],
                    style = type.displayS,
                    color = colors.ink,
                )
                Text(
                    text = strings["projects.save_as_template.aside"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(Space.s))
                if (savedAsTemplate) {
                    Text(
                        text = strings["projects.saved_as_template"],
                        style = type.bodyM,
                        color = colors.ink2,
                        modifier = Modifier.testTag(ProjectSetupTags.SAVED_TEMPLATE),
                    )
                } else {
                    Action(
                        label = strings["projects.save_as_template"],
                        onClick = onSaveAsTemplate,
                        modifier = Modifier.testTag(ProjectSetupTags.SAVE_TEMPLATE),
                    )
                }
            }
        }
    }
}
