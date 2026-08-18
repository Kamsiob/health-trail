package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.ConfirmRemoveSheet
import com.kamsiob.healthtrail.ui.components.EmptyDrawing
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object ProjectDetailTags {
    const val NAME = "project_detail"
    fun step(id: String) = "project_step_$id"
    const val WAITING = "project_waiting"
    const val SAVE_WAITING = "project_save_waiting"
    fun status(key: String) = "project_status_$key"
    const val ADD_STEP = "project_add_step"
    const val STEP_SHEET = "project_step_sheet"
    const val STEP_TEXT = "project_step_text"
    const val STEP_NOTE = "project_step_note"
    const val STEP_UP = "project_step_up"
    const val STEP_DOWN = "project_step_down"
    const val STEP_SAVE = "project_step_save"
    const val STEP_REMOVE = "project_step_remove"
    const val NO_STEPS = "project_no_steps"
    const val DONE_FOLD = "project_done_fold"
    const val NEXT = "project_next"
    const val SAVE_TEMPLATE = "project_save_template"
    const val SAVED_TEMPLATE = "project_saved_template"
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
    onSetWaitingOn: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the template it came from is called, where it came from one. */
    templateName: String? = null,
    onAddStep: (String) -> Unit = {},
    onEditStep: (stepId: String, text: String, note: String?) -> Unit = { _, _, _ -> },
    onMoveStep: (stepId: String, earlier: Boolean) -> Unit = { _, _ -> },
    onRemoveStep: (Repository.ProjectStep) -> Unit = {},
    onSaveAsTemplate: () -> Unit = {},
    /** True once this project's steps have been saved as a template. */
    savedAsTemplate: Boolean = false,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var waitingOn by remember(project.id, project.waitingOn) {
        mutableStateOf(project.waitingOn.orEmpty())
    }
    // Which step's sheet is open, and null for the one that adds a new step.
    var editing by remember(project.id) { mutableStateOf<Editing?>(null) }
    var removing by remember(project.id) { mutableStateOf<Repository.ProjectStep?>(null) }
    var doneOpen by rememberSaveable(project.id) { mutableStateOf(false) }

    // **What is left, and what is behind.** The next step is the first thing
    // nobody has marked done, which is law 1 for this screen: somebody opening
    // a project is asking what to do next, not how far along they are.
    val done = steps.filter { it.isDone }
    val remaining = steps.filterNot { it.isDone }

    Page(
        title = Bidi.isolate(project.name),
        onBack = onBack,
        backLabel = strings["section.back.projects"],
        modifier = modifier.testTag(SectionTags.root(ProjectDetailTags.NAME)),
        eyebrow = strings["notebook.section.projects"],
        subtitle = when {
            // **Not when the name would be said twice.** A project keeps its
            // template's name until somebody renames it, so naming the
            // template again produced "Started from the Medicaid application
            // for long term care template", read directly under the same
            // words at display size. The unnamed line says the same thing and
            // does not stutter.
            templateName != null && templateName != project.name ->
                strings("projects.from_template", "name" to templateName)
            project.templateId != null -> strings["projects.from_a_template"]
            else -> strings["projects.own"]
        },
    ) {
        // **A spine, because a project is a sequence.** 11.11 and 11.12: a
        // chapter list, an incident thread, a milestone arc and a project's
        // steps are one shape seen four times, and this was the fourth one
        // still drawn as a list of checkboxes.
        //
        // **The line is continuous rather than dashed**, per 5.2, because these
        // steps are the person's actual path through a process rather than a
        // filter over entries.
        // **What is already done folds, with its count.** It is the record and
        // it is not the job: somebody opening a project needs the next thing,
        // and eleven finished steps above it push that off the fold. Nothing is
        // hidden, because what has already been sent is exactly what somebody
        // is asked about on the phone, and one tap brings it all back.
        //
        // **It is a count of steps, not a score.** "Already done, 2" says what
        // is in the fold, which is what every other fold in this app says. Rule
        // 13 rules out "2 of 5", and that is a different sentence.
        if (done.isNotEmpty()) {
            item {
                Eyebrow(text = strings["projects.done_fold"], modifier = Modifier.testTag(ProjectDetailTags.DONE_FOLD))
            }

            itemsIndexed(done, key = { _, step -> "done_${step.id}" }) { index, step ->
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < done.lastIndex,
                    node = colors.blue,
                    // Filled, per 5.2.1: the shape carries the state, and a
                    // step that happened is a waypoint that happened.
                    state = Waypoint.HAPPENED,
                    dash = null,
                ) {
                    Column(modifier = Modifier.padding(bottom = Space.s)) {
                        StepRow(
                            step = step,
                            onToggle = { onToggleStep(step) },
                            onEdit = { editing = Editing.Existing(step) },
                            onRemove = { removing = step },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **The next step is the one thing, and it is the first one left.** It
        // wears an eyebrow saying so and its words are at hero weight, which is
        // the whole difference between this screen and the four identical cards
        // it was: the fourth card down was the answer and nothing said so.
        //
        // **It stays on the spine.** Lifting it out and repeating it above
        // would have been the same sentence in two places, and the spine is
        // what shows where in the process the person actually is.
        // **The eyebrow sits at the margin, not in the spine's content column.**
        // Inside the row it started where the card starts, so its rule began 48dp
        // in while every other heading on the screen began at the edge, and the
        // screen read as though one section had slipped.
        if (remaining.isNotEmpty()) {
            item {
                Eyebrow(text = strings["projects.next.label"])
            }
        }

        itemsIndexed(remaining, key = { _, step -> step.id }) { index, step ->
            val isNext = index == 0
            SpineRow(
                continuesAbove = index > 0 || (done.isNotEmpty() && doneOpen),
                continuesBelow = index < remaining.lastIndex,
                node = colors.blue,
                // **Hollow for not yet**, per 5.2.1: a step nobody has started
                // reads as "not yet" rather than as a failure. No strikethrough
                // anywhere, which is the checklist idiom this screen is getting
                // out of.
                state = Waypoint.UPCOMING,
                dash = null,
            ) {
                // **The gap between steps lives inside the row, not between
                // rows.** A spacer between two `SpineRow`s has no gutter in it,
                // so the line stopped at the bottom of each card and started
                // again at the top of the next, and a continuous route rendered
                // as a dashed one. It looked deliberate, which is why it needed
                // looking at rather than reading.
                Column(modifier = Modifier.padding(bottom = if (isNext) Space.m else Space.s)) {
                    StepRow(
                        step = step,
                        onToggle = { onToggleStep(step) },
                        onEdit = { editing = Editing.Existing(step) },
                        onRemove = { removing = step },
                        lead = isNext,
                    )
                }
            }
        }

        // **Nothing left to do is a state and it is said out loud.** A project
        // whose steps are all done otherwise showed a fold and then nothing,
        // which is the blank area rule 11 rules out.
        if (remaining.isEmpty() && steps.isNotEmpty()) {
            item {
                // Air above it, because with the fold open it otherwise sat
                // directly under the last step and read as a caption on it
                // rather than as a statement about the whole list.
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["projects.nothing_left"],
                    style = HealthTrail.type.bodyL,
                    color = colors.ink2,
                    modifier = Modifier.testTag(ProjectDetailTags.NEXT),
                )
            }
        }

        // **A project with no steps has an empty state, per 5.10.** A blank
        // project made from nothing starts here, and "Add a step" alone above
        // two thirds of an empty screen is the blank area rule 11 rules out.
        // The drawing is the projects icon on the shared trail ground, per
        // 5.17, so the empty screen is already teaching the shape the person
        // will navigate by.
        if (steps.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag(ProjectDetailTags.NO_STEPS),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(Space.l))
                    EmptyDrawing(section = Repository.Section.PROJECTS)
                    Spacer(Modifier.height(Space.m))
                    Text(
                        text = strings["projects.no_steps"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // **Where it stands sits under the steps, not over them.** Five status
        // chips and an empty field were the first thing on the screen and took
        // a third of the fold, so the loudest thing on a screen about what to
        // do next was a control for describing it. They are the same controls,
        // moved: the steps answer the question, and this records the answer.
        item {
            Spacer(Modifier.height(Space.sectionGap))
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
            // **Who you are waiting on, which is the most useful field a
            // project has.** The schema makes it first class for that reason.
            // It is always offered rather than only when the status is
            // "waiting", because somebody is usually waiting on somebody long
            // before they think to change a status.
            Spacer(Modifier.height(Space.m))
            Field(
                label = strings["projects.waiting_field"],
                value = waitingOn,
                onValueChange = { waitingOn = it },
                fieldTestTag = ProjectDetailTags.WAITING,
                support = strings["projects.waiting_field.hint"],
            )

            // **Saving is explicit and the control only exists when there is
            // something to save.** Writing on every keystroke would bump the
            // revision and append to the change log once per letter, which the
            // data contract would carry but nobody should have to read.
            if (waitingOn.trim() != project.waitingOn.orEmpty().trim()) {
                Spacer(Modifier.height(Space.s))
                Action(
                    label = strings["projects.waiting_save"],
                    onClick = { onSetWaitingOn(waitingOn.trim()) },
                    modifier = Modifier.fillMaxWidth().testTag(ProjectDetailTags.SAVE_WAITING),
                )
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            // **Adding a step is the control this screen never had**, and
            // `MASTER_SPEC.md` 4.10 has required it since Phase 0. It sits
            // after the steps because that is where a new one goes.
            // **Adding a step outranks saving a template, and read as the
            // smaller of the two.** They were two pills of different widths,
            // ragged against the left edge, with the rarer action wider than
            // the common one. Adding a step is what somebody does while they
            // are still learning the process, which is most of the time.
            Action(
                label = strings["projects.add_step"],
                onClick = { editing = Editing.New },
                modifier = Modifier.fillMaxWidth().testTag(ProjectDetailTags.ADD_STEP),
            )

            // **Saving these steps as the person's own template**, which is
            // what makes editing a shipped one their copy rather than a change
            // to the catalog: `custom_template.derived_from_id` keeps the
            // lineage, so a catalog update in a later version can never
            // overwrite what they wrote.
            //
            // **Only when there is something to save.** A template of no steps
            // is a name with nothing in it, and the control would be one that
            // does nothing, which D42 rules out.
            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(Space.sectionGap))
                if (savedAsTemplate) {
                    Text(
                        text = strings["projects.saved_as_template"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        modifier = Modifier.testTag(ProjectDetailTags.SAVED_TEMPLATE),
                    )
                } else {
                    Action(
                        label = strings["projects.save_as_template"],
                        onClick = onSaveAsTemplate,
                        modifier = Modifier.testTag(ProjectDetailTags.SAVE_TEMPLATE),
                    )
                    Text(
                        text = strings["projects.save_as_template.aside"],
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                    )
                }
            }

        }
    }

    editing?.let { target ->
        StepSheet(
            step = (target as? Editing.Existing)?.step,
            canMoveEarlier = (target as? Editing.Existing)
                ?.let { steps.indexOf(it.step) > 0 } ?: false,
            canMoveLater = (target as? Editing.Existing)
                ?.let { steps.indexOf(it.step) < steps.lastIndex } ?: false,
            onSave = { text, note ->
                when (target) {
                    Editing.New -> onAddStep(text)
                    is Editing.Existing -> onEditStep(target.step.id, text, note)
                }
                editing = null
            },
            onMove = { earlier ->
                (target as? Editing.Existing)?.let { onMoveStep(it.step.id, earlier) }
                editing = null
            },
            onRemove = {
                // The sheet closes first, so the confirmation is not a sheet
                // over a sheet, which Material stacks and a person reads as one
                // screen that has stopped responding.
                (target as? Editing.Existing)?.let { removing = it.step }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    removing?.let { step ->
        ConfirmRemoveSheet(
            what = step.text,
            onConfirm = {
                onRemoveStep(step)
                removing = null
            },
            onDismiss = { removing = null },
        )
    }
}

/** Whether the step sheet is changing one that exists or making a new one. */
private sealed interface Editing {
    data object New : Editing
    data class Existing(val step: Repository.ProjectStep) : Editing
}

/**
 * One step, on the spine.
 *
 * **A tap marks it done and a long press opens it**, which is the same gesture
 * pair the rest of the app uses for a row: 5.4 puts removal and "more to do
 * with this" behind a long press everywhere, and a step is a row like any
 * other. The explicit long click action is declared rather than left as a
 * gesture, so a reader user finds it in their list.
 *
 * **A done step is quieted, not struck through.** The waypoint beside it is
 * already filled, which is the state, and crossing out the words is the
 * checklist idiom this screen is getting out of. What has already been sent is
 * exactly what somebody is asked about on the phone.
 *
 * **The note is shown under the step and it is the reason the note exists.**
 * "The woman on the phone said to call back after the 15th" is the sentence
 * that makes one of these processes survivable, and `project_step.note` had no
 * writer and no reader until 2026-08-03.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepRow(
    step: Repository.ProjectStep,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    /**
     * The next step, which is the one thing on this screen.
     *
     * **The difference is size and space, not color.** Section 9: color alone
     * carries no meaning, and a step tinted to mean "do this one" would say
     * nothing in grayscale and nothing to anybody who cannot separate the hue.
     */
    lead: Boolean = false,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                // A reader is told the state rather than left to infer it from
                // a shape, which is the same reason chips report as checked.
                //
                // **It said "done" and "not done" in English to everybody.**
                // The two words were written straight into the semantics tree,
                // where no check looks: `check_text_sources.py` reads what is
                // drawn, and a string that is only ever spoken is drawn
                // nowhere. A reader user in Spanish heard one English word per
                // step, on the screen that lists every step of a project.
                stateDescription = if (step.isDone) {
                    strings["step.done"]
                } else {
                    strings["step.not_done"]
                }
                customActions = listOf(
                    CustomAccessibilityAction(strings["projects.step.title"]) {
                        onEdit(); true
                    },
                    CustomAccessibilityAction(strings["projects.step.remove"]) {
                        onRemove(); true
                    },
                )
            }
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.cardLarge)
            .background(surface)
            .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.cardLarge)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Checkbox,
                // **A reader is told what the tap does, in words.** Without
                // this it announced "double tap to activate" on a row whose
                // activation marks a step done or puts it back, which are
                // opposite things and neither of them was said.
                onClickLabel = strings[
                    if (step.isDone) "projects.step.undone" else "projects.step.done"
                ],
                onClick = onToggle,
                onLongClick = onEdit,
            )
            .testTag(ProjectDetailTags.step(step.id))
            .padding(if (lead) Space.l else Space.cardPadding),
    ) {
        Text(
            text = Bidi.isolate(step.text),
            style = if (lead) HealthTrail.type.hero else HealthTrail.type.bodyL,
            color = if (step.isDone) colors.ink2 else colors.ink,
        )
        step.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Bidi.isolate(note),
                style = if (lead) HealthTrail.type.bodyL else HealthTrail.type.bodyM,
                color = colors.ink2,
            )
        }
    }
}

/**
 * One step, opened.
 *
 * **The same sheet adds a step and changes one**, because they are the same
 * question with a different heading, which is the rule 5.4 already sets for a
 * card's tap opening the form that made it. Somebody fixing a typo is never
 * told they are adding a step.
 *
 * **Moving it earlier or later is here rather than a drag.** A drag is a
 * gesture a reader user cannot perform and a tired thumb performs badly, and
 * two named controls are one tap each. **A step at either end does not show the
 * control that would do nothing**, per D42.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepSheet(
    step: Repository.ProjectStep?,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onSave: (text: String, note: String?) -> Unit,
    onMove: (earlier: Boolean) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()
    var text by remember(step?.id) { mutableStateOf(step?.text.orEmpty()) }
    var note by remember(step?.id) { mutableStateOf(step?.note.orEmpty()) }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(ProjectDetailTags.STEP_SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(top = Space.l, bottom = Space.l),
        ) {
            Text(
                text = strings[if (step == null) "projects.step.new" else "projects.step.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(Modifier.height(Space.l))

            Field(
                label = strings["projects.step.text"],
                value = text,
                onValueChange = { text = it },
                fieldTestTag = ProjectDetailTags.STEP_TEXT,
                support = strings["projects.step.text.hint"],
            )

            // **Only on a step that exists.** A note about a step nobody has
            // written yet is a field with nothing to be about, and the sheet
            // that adds one should ask for the one thing it needs.
            if (step != null) {
                Spacer(Modifier.height(Space.m))
                Field(
                    label = strings["projects.step.note"],
                    value = note,
                    onValueChange = { note = it },
                    fieldTestTag = ProjectDetailTags.STEP_NOTE,
                    singleLine = false,
                    support = strings["projects.step.note.hint"],
                )

                Spacer(Modifier.height(Space.m))
                if (canMoveEarlier) {
                    Action(
                        label = strings["projects.step.up"],
                        onClick = { onMove(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ProjectDetailTags.STEP_UP),
                    )
                }
                if (canMoveLater) {
                    Action(
                        label = strings["projects.step.down"],
                        onClick = { onMove(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ProjectDetailTags.STEP_DOWN),
                    )
                }

                // **Removing is here, and it was reachable only by a screen
                // reader for an hour.** The long press opens this sheet, and
                // the row's custom accessibility actions offered removal to a
                // reader user while a sighted person had no way to do it at
                // all. Found by trying to remove a step on the phone.
                //
                // It is a text action opening the confirmation rather than a
                // destructive button resting on the sheet, per 5.4: the alert
                // fill exists only inside the confirmation it belongs to.
                Action(
                    label = strings["projects.step.remove"],
                    onClick = onRemove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ProjectDetailTags.STEP_REMOVE),
                )
            }

            Spacer(Modifier.height(Space.l))

            Action(
                label = strings["common.save"],
                onClick = { onSave(text.trim(), note.trim().ifBlank { null }) },
                modifier = Modifier.fillMaxWidth().testTag(ProjectDetailTags.STEP_SAVE), emphasis = ActionEmphasis.Main,
            )
        }
    }
}

