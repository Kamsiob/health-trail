package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DateRow
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.FoldRowText
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.LatestWordCard
import com.kamsiob.healthtrail.ui.components.RoadSize
import com.kamsiob.healthtrail.ui.components.RoadStage
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RoadStrip
import com.kamsiob.healthtrail.ui.components.StandingCard
import com.kamsiob.healthtrail.ui.components.StepRow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectHomeTags {
    const val NAME = "project-home"
    const val ROAD = "project-home-road"
    const val STANDING = "project-home-standing"
    const val DATE = "project-home-date"
    const val LATEST = "project-home-latest"
    const val STEPS = "project-home-steps"
    const val UPDATE_STANDING = "project-home-update-standing"
    const val ADD_DATE = "project-home-add-date"
    const val LOG_CALL = "project-home-log-call"
    const val SETUP = "project-home-setup"
    const val MOVE_STAGE = "project-home-move-stage"
    const val TRAIL = "project-home-trail"
    const val PAPERS = "project-home-papers"
    const val PEOPLE = "project-home-people"
    const val ENDED = "project-home-ended"
    const val STORY = "project-home-story"
    const val REOPEN = "project-home-reopen"
}

/**
 * Everything one project is, on one screen. `DESIGN.md` 20.5, screen 5.
 *
 * **The shape is which of the three answers it leads with**, 20.3, and that is
 * the project's own `lead`. The long road opens with where it stands, the
 * closing window with the next date, the busy stretch with the steps. **The
 * components do not change and neither does their meaning: only the order
 * does.** One grammar, three arrangements, which is what keeps a person who has
 * learned one project able to read the next one.
 *
 * **The road strip under the title is what makes it a project at a glance.**
 * It is information and does nothing on touch.
 *
 * **Every answer this screen cannot give is a real state and says so plainly.**
 * Nobody has said where it stands, no date has been written down, nothing has
 * been recorded from them: each of those is a sentence rather than a blank, and
 * none of them is framed as something the person failed to do. Rule 13.
 *
 * **Nothing here advises, scores, or colors by urgency**, 20.7.
 */
@Composable
fun ProjectHomeScreen(
    project: Repository.Project,
    stages: List<Repository.ProjectStage>,
    standing: Repository.ProjectStanding?,
    /** The date the screen leads with, chosen by the rule in D113. */
    nextDate: Repository.ProjectDate?,
    latestWord: Repository.TrailEntry?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The countdown, already composed by the caller: "18 days", "passed 6 days ago". */
    countdown: String? = null,
    /** What kind of date it is, as the person labeled it. Raw, not isolated. */
    dateKind: String? = null,
    /** The date itself, already rendered at its own precision. Raw. */
    dateWhen: String? = null,
    /** How long it has stood where it stands, composed by the caller. */
    standingSince: String? = null,
    /** Who said the latest word, raw. Null where nobody was named. */
    attributionWho: String? = null,
    /** When it was said, already rendered at its own precision. Raw. */
    attributionWhen: String? = null,
    onOpenDate: () -> Unit = {},
    onOpenEntry: () -> Unit = {},
    /** Opens the sheet that records where it stands. 20.5 screen 8. */
    onUpdateStanding: () -> Unit = {},
    /** Opens the sheet that writes down a date, with where it came from. */
    onAddDate: () -> Unit = {},
    /** Opens the sheet that logs a call, already knowing which project. */
    onLogCall: () -> Unit = {},
    /** Opens the project's setup, where everything the template decided lives. */
    onOpenSetup: () -> Unit = {},
    /** Opens the sheet that moves the project along its road. 20.5 screen 12. */
    onMoveStage: () -> Unit = {},
    steps: List<Repository.ProjectStep> = emptyList(),
    papers: List<Repository.ProjectPaper> = emptyList(),
    /**
     * Everything written down about this project, most recent first.
     *
     * **Rule 18 in the other direction.** Without this a person who logged six
     * calls can see one of them, which makes the record look like a setting
     * rather than a record.
     */
    entries: List<Repository.TrailEntry> = emptyList(),
    onOpenEntryById: (String) -> Unit = {},
    /** How many things are on this project's trail, entries and stages and dates. */
    trailCount: Int = 0,
    /** Opens the project's own trail. 20.5 screen 11. */
    onOpenTrail: () -> Unit = {},
    /** Opens the project's papers as paper. 20.5 screen 13. */
    onOpenPaperwork: () -> Unit = {},
    /** Opens the people this process has involved. 20.5 screen 14. */
    onOpenPeople: () -> Unit = {},
    /** How many people this process has involved. Never a score. */
    peopleCount: Int = 0,
    onToggleStep: (Repository.ProjectStep) -> Unit = {},
    /**
     * Puts a closed project back to work. 20.5 screen 17.
     *
     * **It exists because these processes come back.** An appeal is refused and
     * refiled, a waiver lapses and is applied for again, and a file that could
     * only be closed once would make somebody start a second one and lose the
     * history that made the first worth keeping.
     */
    onReopen: () -> Unit = {},
) {
    // **Open from the start on the busy stretch**, where the steps are the
    // lead, and closed everywhere else, where they are volume behind the
    // answer. The person's own toggle wins from the first tap.
    var stepsOpen by rememberSaveable(project.id) { mutableStateOf(project.lead == "steps") }
    var papersOpen by rememberSaveable(project.id) { mutableStateOf(false) }
    var trailOpen by rememberSaveable(project.id) { mutableStateOf(false) }
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    SectionScaffold(
        name = ProjectHomeTags.NAME,
        title = strings["notebook.section.projects"],
        heading = Bidi.isolate(project.name),
        // The provenance line the old screen already got right, kept: what this
        // came from, never how far through it somebody is.
        subtitle = when {
            project.templateId != null -> strings["projects.from_a_template"]
            else -> strings["projects.own"]
        },
        onBack = onBack,
        backLabelKey = "section.back.projects",
        modifier = modifier,
    ) {
        // **The road, first and bare.** It is the one element that says "this is
        // a project" before a single word is read.
        if (stages.size >= 2) {
            item {
                RoadStrip(
                    stages = stages.map { RoadStage(name = it.name, reached = it.isReached) },
                    description = roadDescription(project.name, stages, strings),
                    size = RoadSize.FULL,
                    modifier = Modifier.testTag(ProjectHomeTags.ROAD),
                )
                // **The road is bare and its control is beside it**, 20.6. The
                // strip itself does nothing on touch, so the way to move along
                // it is an outlined action rather than a tappable waypoint,
                // which would make an information graphic look like a picker.
                //
                // **Not on a closed project.** The road stays, because it is
                // the shape of what happened and worth reading afterwards, but
                // an action that says move it along sits above a block that
                // says how it ended and contradicts it. Reopening is the way
                // back to moving, and it is right there.
                if (!project.isFinished) {
                    Spacer(Modifier.height(Space.s))
                    QuietButton(
                        label = strings["project.stage.move.short"],
                        onClick = onMoveStage,
                        modifier = Modifier.testTag(ProjectHomeTags.MOVE_STAGE),
                    )
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **A closed project leads with how it ended.** 20.5 screen 17. The
        // three answers below still render, because the record is kept whole
        // and a finished process is still a process somebody may need to read,
        // but the question this screen answers first has changed: not "where
        // does it stand" but "what happened, and what did it take".
        //
        // **Nothing here scores the person.** The numbers are counts of the
        // record: how long it ran, how many calls, how many papers, how many
        // people. Rule 13 and 20.7 both rule out anything that reads as a grade
        // on how well somebody handled it.
        if (project.isFinished) {
            item {
                StandingCard(
                    eyebrow = strings["project.ended"],
                    holder = strings[endedKey(project.status)],
                    since = endedSpan(project, strings),
                    modifier = Modifier.testTag(ProjectHomeTags.ENDED),
                    actions = {
                        // **Outlined, and the only action here.** Reopening is
                        // a real thing somebody does and it is not the point of
                        // the screen, which is the record.
                        QuietButton(
                            label = strings["project.reopen"],
                            onClick = onReopen,
                            modifier = Modifier.testTag(ProjectHomeTags.REOPEN),
                        )
                    },
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
            item {
                GroupHeaderText(label = strings["project.story"])
                DenseRow(
                    title = strings["project.story.line"],
                    subtitle = storyLine(project, entries, papers, peopleCount, strings),
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier.testTag(ProjectHomeTags.STORY),
                )
                // **What closing did not do**, said plainly, because that is
                // the whole promise of the screen's name. Nothing is deleted,
                // nothing is archived away, and the export still holds it.
                Text(
                    text = strings["project.story.kept"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = Space.xs),
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **The three answers, in the order this project's shape puts them.**
        // DESIGN.md 20.3. The components and their meanings do not change:
        // only the order does, which is what keeps somebody who has learned one
        // project able to read the next one.
        //
        // The lead is drawn at its own weight; the other two follow as rows.
        // **The date is the only one that changes size**, because a countdown
        // that leads is the thing the closing window is for and a countdown
        // under a lead must not out-shout it.
        val standingBlock: @Composable () -> Unit = {
            if (standing != null) {
                StandingCard(
                    eyebrow = strings["project.where_it_stands"],
                    holder = Bidi.isolate(standing.holderLabel),
                    since = standingSince,
                    modifier = Modifier.testTag(ProjectHomeTags.STANDING),
                    // **An outlined pill, and a verb**, 20.6. It records what
                    // somebody was told; it does not suggest chasing anybody.
                    actions = {
                        QuietButton(
                            label = strings["project.standing.update"],
                            onClick = onUpdateStanding,
                            modifier = Modifier.testTag(ProjectHomeTags.UPDATE_STANDING),
                        )
                    },
                )
            } else {
                // **A calm state, never a scold.** It says what would go here
                // and does not imply anybody should have filled it in.
                StandingCard(
                    eyebrow = strings["project.where_it_stands"],
                    holder = strings["project.stands.none"],
                    since = strings["project.stands.none.note"],
                    modifier = Modifier.testTag(ProjectHomeTags.STANDING),
                    // **The none-yet rung names its one action**, 21.4's rule
                    // applied here: a calm state that says what would go in it.
                    actions = {
                        QuietButton(
                            label = strings["project.standing.update"],
                            onClick = onUpdateStanding,
                            modifier = Modifier.testTag(ProjectHomeTags.UPDATE_STANDING),
                        )
                    },
                )
            }
        }

        val dateBlock: @Composable () -> Unit = {
            Column {
            if (nextDate != null && countdown != null && dateKind != null) {
                DateRow(
                    countdown = countdown,
                    // **Both built from the same raw parts**, so neither is a
                    // joined string handed back into a join. Bidi.join isolates
                    // every part it is given, and isolating an isolate nests the
                    // marks.
                    what = Bidi.join(dateKind, dateWhen),
                    source = nextDate.sourceNote,
                    description = Bidi.join(
                        countdown, dateKind, dateWhen, nextDate.sourceNote,
                    ),
                    onOpen = onOpenDate,
                    openLabel = strings["project.open_date"],
                    prominent = project.lead == "date",
                    modifier = Modifier.testTag(ProjectHomeTags.DATE),
                )
            } else {
                // **The none-yet rung names its one action**, 21.4. A project
                // with no date written down is an ordinary state, and the card
                // says what would go here rather than only that nothing does.
                Text(
                    text = strings["project.date.none"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier
                        .testTag(ProjectHomeTags.DATE)
                        .padding(horizontal = Space.xs, vertical = Space.s),
                )
            }
            // **Always offered, not only when there are none.** A project with
            // a filing deadline still gets a hearing date, and an action that
            // appears only on an empty screen is an action nobody finds twice.
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["project.date.add"],
                onClick = onAddDate,
                modifier = Modifier.testTag(ProjectHomeTags.ADD_DATE),
            )
            }
        }

        val latestBlock: @Composable () -> Unit = {
            Column {
            if (latestWord != null && !latestWord.body.isNullOrBlank()) {
                LatestWordCard(
                    eyebrow = strings["project.latest_word"],
                    words = latestWord.body,
                    // Both built from the same raw parts, so neither is a
                    // joined string handed back into a join.
                    attribution = Bidi.join(attributionWho, attributionWhen),
                    description = Bidi.join(
                        strings["project.latest_word"],
                        latestWord.body,
                        attributionWho,
                        attributionWhen,
                    ),
                    onOpen = onOpenEntry,
                    openLabel = strings["project.open_entry"],
                    modifier = Modifier.testTag(ProjectHomeTags.LATEST),
                )
            } else {
                // **The eyebrow stays when the card goes.** Without it this
                // rung is the sentence "Nothing written down from them yet"
                // with nothing on the screen that says who "them" is: the
                // filled card carries "The latest word" and the empty one
                // dropped it, so on a brand new project two of the three
                // answers were loose gray lines with no label at all. 20.1
                // says the screen answers three questions, and a question is
                // not answered by a sentence that does not name it.
                //
                // The date rung needs none of this: "No date written down yet"
                // says what it is about, and the filled DateRow has no eyebrow
                // either, so adding one there would invent a label the drawn
                // state does not have.
                Column(
                    modifier = Modifier
                        .testTag(ProjectHomeTags.LATEST)
                        .padding(horizontal = Space.xs, vertical = Space.s),
                ) {
                    Text(
                        text = strings["project.latest_word"],
                        style = type.mono,
                        color = colors.goldInk,
                    )
                    Text(
                        text = strings["project.word.none"],
                        style = type.bodyS,
                        color = colors.ink2,
                    )
                }
            }
            // **Always offered.** The latest word is the thing that changes
            // most often on a long process, and an action that appears only
            // when there is nothing recorded would be gone the moment it
            // started being useful.
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["project.log_call"],
                onClick = onLogCall,
                modifier = Modifier.testTag(ProjectHomeTags.LOG_CALL),
            )
            }
        }

        // **The steps lead on the busy stretch.** Two intense weeks of small
        // parallel arrangements is a screen somebody opens to see what is left,
        // not to read a sentence about an office, so the cluster sits above the
        // other two answers and starts open. On the other two shapes the steps
        // are volume behind the answer and fold away below.
        val stepsBlock: @Composable () -> Unit = {
            Column(modifier = Modifier.testTag(ProjectHomeTags.STEPS)) {
                FoldRow(
                    labelKey = "project.fold.steps",
                    expanded = stepsOpen,
                    onToggle = { stepsOpen = !stepsOpen },
                    count = steps.size.toString(),
                )
                if (stepsOpen) {
                    Spacer(Modifier.height(Space.cardGap))
                    // **Clustered by area on the busy stretch**, 20.3. Two
                    // intense weeks of small parallel arrangements is not one
                    // list: the house, the ride and the equipment are different
                    // errands with different people, and a flat list of
                    // nineteen makes somebody re-sort them in their head every
                    // time they open it.
                    //
                    // **Steps with no area keep their own group at the end**,
                    // because a step nobody has filed is still a step and
                    // hiding it until it is tidy would be the app asking to be
                    // organized before it will help.
                    val clustered = steps.groupBy { it.cluster?.takeIf { c -> c.isNotBlank() } }
                    val named = clustered.filterKeys { it != null }
                    val loose = clustered[null].orEmpty()

                    for ((position, entry) in named.entries.withIndex()) {
                        val (area, inArea) = entry
                        // **The room goes above the label, not below it.** A
                        // heading belongs to the rows under it, so the gap that
                        // separates one area from the next sits before the
                        // heading. Trailing it instead left every label tight to
                        // the block it named and loose from its own rows.
                        if (position > 0) Spacer(Modifier.height(Space.m))
                        GroupHeaderText(
                            // **Raw, because the header isolates it itself.**
                            // Isolating here too nests the marks, which is the
                            // same defect three other screens had.
                            label = area.orEmpty(),
                            // **A count of what is in the group and nothing
                            // else.** The grid draws "1 OF 3" here; rule 13
                            // rules out a completion count on the person's own
                            // work, and that conflict is the owner's to settle.
                            // Until then this says what every other count in
                            // the app says. DECISIONS.md D116.
                            count = inArea.size.toString(),
                            countDescription = strings(
                                "projects.step_count",
                                "count" to inArea.size,
                            ),
                        )
                        Spacer(Modifier.height(Space.xs))
                        inArea.forEach { step ->
                            StepRow(
                                text = step.text,
                                done = step.isDone,
                                handler = step.handlerLabel,
                                onToggle = { onToggleStep(step) },
                                description = stepDescription(step, strings),
                            )
                        }
                    }
                    if (named.isNotEmpty() && loose.isNotEmpty()) {
                        Spacer(Modifier.height(Space.m))
                    }
                    loose.forEach { step ->
                        StepRow(
                            text = step.text,
                            done = step.isDone,
                            handler = step.handlerLabel,
                            onToggle = { onToggleStep(step) },
                            description = stepDescription(step, strings),
                        )
                    }
                }
            }
        }

        val order: List<@Composable () -> Unit> = when (project.lead) {
            "date" -> listOf(dateBlock, standingBlock, latestBlock)
            "steps" -> listOfNotNull(
                if (steps.isNotEmpty()) stepsBlock else null,
                standingBlock,
                dateBlock,
                latestBlock,
            )
            else -> listOf(standingBlock, dateBlock, latestBlock)
        }

        order.forEachIndexed { index, block ->
            item {
                block()
                Spacer(
                    Modifier.height(
                        if (index == order.lastIndex) Space.sectionGap else Space.cardGap,
                    ),
                )
            }
        }

        // **Everything else, folded and counted.** DESIGN.md 20.5 screen 5: the
        // three answers first, then the volume behind them. A project with two
        // hundred entries and forty papers is not a screen anybody can read,
        // and the fold is what makes the answers stay first. Section 7.
        //
        // **The counts are counts, never scores.** "Steps 6" says what is in the
        // fold, which is what every other fold in this app says, and rule 13
        // rules out "2 of 6".
        // **Everything said about this project, folded and counted.** The
        // latest word is above; this is the rest of it, and 20.5 screen 11 puts
        // the project's own trail here. It folds because a project two years
        // old has two hundred of these and the three answers stay first.
        // **A door rather than a fold, since 20.5 screen 11 is a screen.** The
        // fold listed the linked entries and nothing else; the project's trail
        // is those *and* the road turning *and* the dates it is running
        // against, on one spine, read forwards, with its own filters. None of
        // that fits under a disclosure on a screen whose job is the three
        // answers. It keeps the fold row's own shape, the way Setup does.
        // **Always here, even at nothing.** A door that appears only once there
        // is something behind it is one nobody learns about, which is the same
        // arithmetic that put "Write down a date" on a project that already has
        // one. "Nothing on this project's trail yet" is a real answer and the
        // trail screen gives it; a missing row gives nothing.
        item {
            FoldRow(
                labelKey = "project.fold.trail",
                expanded = false,
                onToggle = onOpenTrail,
                count = trailCount.toString(),
                modifier = Modifier.testTag(ProjectHomeTags.TRAIL),
            )
            Spacer(Modifier.height(Space.cardGap))
        }

        // Only when the steps are not the lead, or the busy stretch would
        // draw the same cluster twice.
        if (steps.isNotEmpty() && project.lead != "steps") {
            item {
                FoldRow(
                    labelKey = "project.fold.steps",
                    expanded = stepsOpen,
                    onToggle = { stepsOpen = !stepsOpen },
                    count = steps.size.toString(),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            if (stepsOpen) {
                items(steps, key = { it.id }) { step ->
                    StepRow(
                        text = step.text,
                        done = step.isDone,
                        handler = step.handlerLabel,
                        onToggle = { onToggleStep(step) },
                        description = stepDescription(step, strings),
                    )
                }
                item { Spacer(Modifier.height(Space.cardGap)) }
            }
        }

        // **A door, like the trail.** The fold listed the places and their two
        // states and nothing else; 20.5 screen 13 shows the paper itself, split
        // by what they sent and what you sent, which is the question somebody
        // is actually asking. Drawn even at zero, for the reason the trail is.
        // **The people this process has involved**, 20.5 screen 14, above the
        // papers because a long process is mostly other people.
        item {
            FoldRow(
                labelKey = "project.fold.people",
                expanded = false,
                onToggle = onOpenPeople,
                count = peopleCount.toString(),
                modifier = Modifier.testTag(ProjectHomeTags.PEOPLE),
            )
            Spacer(Modifier.height(Space.cardGap))
        }

        item {
            FoldRow(
                labelKey = "project.fold.papers",
                expanded = false,
                onToggle = onOpenPaperwork,
                count = papers.size.toString(),
                modifier = Modifier.testTag(ProjectHomeTags.PAPERS),
            )
            Spacer(Modifier.height(Space.cardGap))
        }

        // **The way to everything the template decided.** 20.5 screen 18 and
        // law 5: it sits last because it is the least often needed and the
        // most reassuring to know is there.
        item {
            FoldRowText(
                label = strings["project.setup.open"],
                expanded = false,
                onToggle = onOpenSetup,
                modifier = Modifier.testTag(ProjectHomeTags.SETUP),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }
    }
}

/** What a reader says for one step, as one sentence rather than three stops. */
private fun stepDescription(
    step: Repository.ProjectStep,
    strings: com.kamsiob.healthtrail.i18n.Strings,
): String = Bidi.join(
    step.text,
    strings[if (step.isDone) "project.step.done" else "project.step.not_done"],
    step.handlerLabel?.let { strings("project.step.handled_by", "who" to it) },
)

/**
 * What a reader says instead of the road.
 *
 * **One sentence naming where the project is**, not a list of five waypoints.
 * A road with nothing reached says so rather than claiming stage zero.
 */
// **Numbers go to ICU as numbers.** A count handed over as text crashes on a
// plural and, on the ones that do not crash, renders Western digits inside an
// Arabic sentence, which is the quieter half of the same defect.
private fun roadDescription(
    name: String,
    stages: List<Repository.ProjectStage>,
    strings: com.kamsiob.healthtrail.i18n.Strings,
): String {
    val current = stages.indexOfLast { it.isReached }
    return if (current < 0) {
        strings(
            "project.road.not_started",
            "name" to name,
            "total" to stages.size,
        )
    } else {
        strings(
            "project.road.at",
            "name" to name,
            "position" to current + 1,
            "total" to stages.size,
            "stage" to stages[current].name,
        )
    }
}

/**
 * How a finished project ended, in the person's own language.
 *
 * **Two endings and the record keeps both.** Done is a process that reached an
 * answer; abandoned is one somebody stopped carrying, which is a decision and
 * not a failure. 20.7: nothing here says whether either was the right call.
 */
private fun endedKey(status: String): String = when (status) {
    "abandoned" -> "project.ended.abandoned"
    else -> "project.ended.done"
}

/**
 * When it closed and when it began, on one quiet line.
 *
 * **Exactly the precision each date was given**, through `EventDateText`, per
 * rule 17. A project somebody remembers starting sometime in March started in
 * March, and this line says March.
 *
 * **A missing close date is a real state.** Every project closed before the app
 * wrote that column has one, and the line says when it started instead of
 * inventing a day it ended.
 */
@Composable
private fun endedSpan(project: Repository.Project, strings: Strings): String {
    val closed = project.finishedEdtf?.takeIf { it.isNotBlank() }
        ?.let { strings("project.ended.on", "date" to EventDateText.render(strings, it)) }
    val began = project.startedEdtf?.takeIf { it.isNotBlank() }
        ?.let { strings("project.ended.from", "date" to EventDateText.render(strings, it)) }
    return Bidi.join(closed, began)
}

/**
 * The whole story in honest numbers. 20.5 screen 17.
 *
 * **Counts of the record and never a grade.** How long it ran, how many calls
 * were logged, how many papers were kept, how many people it involved. Rule 13
 * rules out anything that reads as a score on somebody's own diligence, and
 * 20.7 rules out the app having a view about how a process went.
 *
 * **A part that cannot be counted is left out rather than shown as zero.** A
 * project with no papers does not need to be told it has no papers.
 */
@Composable
private fun storyLine(
    project: Repository.Project,
    entries: List<Repository.TrailEntry>,
    papers: List<Repository.ProjectPaper>,
    peopleCount: Int,
    strings: Strings,
): String {
    val days = project.startedStart?.let { from ->
        project.finishedStart?.let { to ->
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.Instant.ofEpochMilli(from).atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate(),
                java.time.Instant.ofEpochMilli(to).atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate(),
            )
        }
    }
    val calls = entries.count { it.kind == "call" }
    return Bidi.join(
        days?.takeIf { it > 0 }?.let { strings("project.story.days", "count" to it) },
        calls.takeIf { it > 0 }?.let { strings("project.story.calls", "count" to it) },
        papers.size.takeIf { it > 0 }?.let { strings("project.story.papers", "count" to it) },
        peopleCount.takeIf { it > 0 }?.let { strings("project.story.people", "count" to it) },
    )
}
