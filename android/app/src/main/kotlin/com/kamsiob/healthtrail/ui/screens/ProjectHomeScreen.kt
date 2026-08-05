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
import com.kamsiob.healthtrail.ui.components.DateRow
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.FoldRowText
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
    steps: List<Repository.ProjectStep> = emptyList(),
    papers: List<Repository.ProjectPaper> = emptyList(),
    onToggleStep: (Repository.ProjectStep) -> Unit = {},
) {
    // **Open from the start on the busy stretch**, where the steps are the
    // lead, and closed everywhere else, where they are volume behind the
    // answer. The person's own toggle wins from the first tap.
    var stepsOpen by rememberSaveable(project.id) { mutableStateOf(project.lead == "steps") }
    var papersOpen by rememberSaveable(project.id) { mutableStateOf(false) }
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
                Text(
                    text = strings["project.word.none"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier
                        .testTag(ProjectHomeTags.LATEST)
                        .padding(horizontal = Space.xs, vertical = Space.s),
                )
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
                    steps.forEach { step ->
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

        if (papers.isNotEmpty()) {
            item {
                FoldRow(
                    labelKey = "project.fold.papers",
                    expanded = papersOpen,
                    onToggle = { papersOpen = !papersOpen },
                    count = papers.size.toString(),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            if (papersOpen) {
                items(papers, key = { it.id }) { paper ->
                    // **An empty placeholder reads "not yet", never as an
                    // error**, 20.4. It is a paper that has not arrived, which
                    // is the ordinary state of most of them for most of the time.
                    DenseRow(
                        title = Bidi.isolate(paper.name),
                        subtitle = strings[
                            if (paper.isFilled) "project.paper.held"
                            else "project.paper.not_yet"
                        ],
                    )
                }
                item { Spacer(Modifier.height(Space.cardGap)) }
            }
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
