package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Distance
import com.kamsiob.healthtrail.time.Edtf
import java.time.ZoneId
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DateRow
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.components.Symbol
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.ColumnScope
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.LatestWordCard
import com.kamsiob.healthtrail.ui.components.RoadSize
import com.kamsiob.healthtrail.ui.components.RoadStage
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RoadStrip
import com.kamsiob.healthtrail.ui.components.StandingCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Tile
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.IconTile
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.components.Waypoint
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
    const val RENAME = "project-home-rename"
    const val SETUP = "project-home-setup"
    const val MOVE_STAGE = "project-home-move-stage"
    const val TRAIL = "project-home-trail"
    const val PAPERS = "project-home-papers"
    const val PEOPLE = "project-home-people"
    const val ENDED = "project-home-ended"
    const val STORY = "project-home-story"
    const val REOPEN = "project-home-reopen"
    const val RETURN = "project-home-return"
    const val REMOVE = "project-home-remove"
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
    /** Opens the screen that corrects this project's name. #374. */
    onRename: () -> Unit = {},
    /**
     * Taking the whole project out of the notebook, per #218. The projects list
     * used to do this on a long press, which is the one path a sighted person
     * cannot find. It opens the confirmation and removes nothing itself.
     */
    onRemove: () -> Unit = {},
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
    /**
     * Opens the steps as their own screen. D164: the steps used to unfold in
     * place on one project shape and fold away on the others, the exact
     * accordion grammar the owner named; they have had a screen of their own
     * all along and the file row is now the one way to it.
     */
    onOpenSteps: () -> Unit = {},
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
        // == The redesign, 2026-08-16, D164. ==================================
        //
        // **The owner, live: "there's just menus and sub menus and tabs and
        // accordions. it's just a gigantic mess."** The old screen stacked a
        // road strip, a move control, three answer cards whose ORDER CHANGED
        // per project, an inline steps cluster that appeared on one shape and
        // folded on the others, three fold-shaped doors, and four pills. A
        // person could learn one project and be lost on the next, because the
        // screen itself moved.
        //
        // **The new contract: one fixed order on every project, always.**
        //   1. The answer: where it stands, composed as one block.
        //   2. The three verbs, in one row, in one place, forever.
        //   3. The road, vertical, with the move control on the current stage.
        //   4. The latest word.
        //   5. The file: five plain rows, no folds, no accordions.
        //   6. Housekeeping.
        // Nothing reorders itself, nothing unfolds in place, and every door
        // looks like a door.

        // -- 0. The greeting, kept: lapse tolerance is law. -------------------
        val away = if (project.isFinished) null else monthsAway(entries)
        if (away != null) {
            item {
                StandingCard(
                    eyebrow = strings["project.return"],
                    holder = strings(returnKey(away), "count" to away.count),
                    since = returnLine(entries, standing, strings),
                    modifier = Modifier.testTag(ProjectHomeTags.RETURN),
                    actions = {
                        QuietButton(
                            label = strings["project.return.confirm"],
                            onClick = onUpdateStanding,
                        )
                    },
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // -- 1. The answer. A closed project answers with how it ended. -------
        if (project.isFinished) {
            item {
                StandingCard(
                    eyebrow = strings["project.ended"],
                    holder = strings[endedKey(project.status)],
                    since = endedSpan(project, strings),
                    modifier = Modifier.testTag(ProjectHomeTags.ENDED),
                    actions = {
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
                    modifier = Modifier.testTag(ProjectHomeTags.STORY),
                )
                Text(
                    text = strings["project.story.kept"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = Space.xs),
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        } else {
            item {
                // **One block, not three cards fighting.** The eyebrow names
                // the question, the holder answers it at display weight, the
                // date sits under it as the one line with a clock in it. The
                // old screen made "where it stands", "the next date" and "the
                // latest word" three siblings and then shuffled them; a person
                // opening any project now reads down: standing, date, verbs,
                // road, word. Law 1: one thing leads.
                Column(modifier = Modifier.testTag(ProjectHomeTags.STANDING)) {
                    Text(
                        text = strings["project.where_it_stands"],
                        style = type.eyebrow,
                        color = colors.goldInk,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = standing?.holderLabel?.let { Bidi.isolate(it) }
                            ?: strings["project.stands.none"],
                        style = type.displayS,
                        color = if (standing != null) colors.ink else colors.ink2,
                    )
                    (if (standing != null) standingSince else strings["project.stands.none.note"])
                        ?.let {
                            Spacer(Modifier.height(Space.xs))
                            Text(text = it, style = type.bodyM, color = colors.ink2)
                        }
                }
                Spacer(Modifier.height(Space.cardGap))
            }

            item {
                // **The date is a row under the answer, at the same place on
                // every project.** Tappable when there is one, per rule 17: the
                // date is editable forever from the thing itself.
                if (nextDate != null && countdown != null && dateKind != null) {
                    DateRow(
                        countdown = countdown,
                        what = Bidi.join(dateKind, dateWhen),
                        source = nextDate.sourceNote,
                        description = Bidi.join(
                            countdown, dateKind, dateWhen, nextDate.sourceNote,
                        ),
                        onOpen = onOpenDate,
                        openLabel = strings["project.open_date"],
                        prominent = false,
                        modifier = Modifier.testTag(ProjectHomeTags.DATE),
                    )
                } else {
                    Text(
                        text = strings["project.date.none"],
                        style = type.bodyM,
                        color = colors.ink2,
                        modifier = Modifier
                            .testTag(ProjectHomeTags.DATE)
                            .padding(vertical = Space.xs),
                    )
                }
                Spacer(Modifier.height(Space.sectionGap))
            }

            // -- 2. The three verbs of a long process, one row, forever. ------
            item {
                // **Log a call, write down a date, say where it stands.**
                // Everything a bureaucracy does to a family lands in one of
                // these three, and the old screen scattered them: one under
                // the latest word, one under the date, one on the standing
                // card, each appearing and disappearing with its block. Three
                // tiles in one row never move, which is what builds the only
                // kind of speed this person gets to keep: muscle memory.
                // Rule 22: a tile for a fixed set of destinations.
                // **One filled action beside two tonal ones**, which is what
                // `m3v4-2` draws: logging the call is the verb this screen
                // exists for and the other two are the things you might also
                // do. Three white tiles at one weight made the person sort
                // them, which is exactly what rule 15 says uniform weight
                // costs. The icons sit above the labels, as the drawing has
                // them, so three verbs of different lengths still line up.
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    ProjectAction(
                        label = strings["project.log_call"],
                        symbol = Symbols.call,
                        filled = true,
                        onClick = onLogCall,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ProjectHomeTags.LOG_CALL),
                    )
                    ProjectAction(
                        label = strings["project.date.add"],
                        symbol = Symbols.appointments,
                        filled = false,
                        onClick = onAddDate,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ProjectHomeTags.ADD_DATE),
                    )
                    ProjectAction(
                        label = strings["project.standing.update"],
                        symbol = Symbols.edit,
                        filled = false,
                        onClick = onUpdateStanding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ProjectHomeTags.UPDATE_STANDING),
                    )
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // -- 3. The road, vertical, with the record's own grammar. ------------
        // **A project is a journey through stages the way the person's care is
        // a journey through places, and the app already speaks that language
        // everywhere else**: the trail, the chapters, the milestones all run
        // on one vertical spine. The horizontal strip was the only road in the
        // app lying on its side, and it needed a separate control floating
        // beside it. Vertical, each stage is a waypoint: passed ones solid,
        // the current one ringed as a milestone, the ones ahead hollow, and
        // "Move it along" sits ON the current stage, which is rule 18: the
        // control lives where the state it changes lives.
        if (stages.size >= 2) {
            item {
                Column {
                    // **The road is named**, which `m3v4-2` draws and this
                    // screen did not: the spine began under the actions with
                    // nothing saying what it was. The string has been in the
                    // catalog since the road was built and nothing rendered it.
                    Text(
                        text = strings["project.road.title"],
                        style = type.displayS,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(Space.sm))
                Column(
                    modifier = Modifier
                        .testTag(ProjectHomeTags.ROAD)
                        .semantics { contentDescription = roadDescription(project.name, stages, strings) },
                ) {
                    // **A file that exists is standing at its first stretch.**
                    // A fresh project has reached nothing, and indexOfLast
                    // returns -1: drawn literally, the road had no ring and no
                    // move control, a path with nobody on it. Seen on the
                    // phone. The first stage is where a just-started process
                    // stands, which is also where the horizontal preview said
                    // it would begin.
                    val current = stages.indexOfLast { it.isReached }
                        .let { if (it < 0) 0 else it }
                    stages.forEachIndexed { index, stage ->
                        SpineRow(
                            continuesAbove = index > 0,
                            continuesBelow = index < stages.lastIndex,
                            // **The node draws only when given a color**: null
                            // is "no waypoint at this row", which left the
                            // road a bare line. Seen on the phone.
                            node = colors.gold,
                            state = when {
                                index == current && !project.isFinished -> Waypoint.MILESTONE
                                stage.isReached -> Waypoint.HAPPENED
                                else -> Waypoint.UPCOMING
                            },
                            dash = null,
                        ) {
                            Column(modifier = Modifier.padding(bottom = Space.s)) {
                                Text(
                                    text = Bidi.isolate(stage.name),
                                    style = if (index == current && !project.isFinished) {
                                        type.bodyL
                                    } else {
                                        type.bodyM
                                    },
                                    color = when {
                                        index == current && !project.isFinished -> colors.ink
                                        stage.isReached -> colors.ink2
                                        else -> colors.ink2
                                    },
                                )
                                if (index == current && !project.isFinished) {
                                    Spacer(Modifier.height(Space.xs))
                                    QuietButton(
                                        label = strings["project.stage.move.short"],
                                        onClick = onMoveStage,
                                        modifier = Modifier.testTag(ProjectHomeTags.MOVE_STAGE),
                                    )
                                }
                            }
                        }
                    }
                }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // -- 4. The latest word, in the same place on every project. ----------
        item {
            if (latestWord != null && !latestWord.body.isNullOrBlank()) {
                LatestWordCard(
                    eyebrow = strings["project.latest_word"],
                    words = latestWord.body,
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
                // **The eyebrow stays when the card goes**, so the empty rung
                // still names its own question.
                Column(
                    modifier = Modifier
                        .testTag(ProjectHomeTags.LATEST)
                        .padding(vertical = Space.s),
                ) {
                    Text(
                        text = strings["project.latest_word"],
                        style = type.eyebrow,
                        color = colors.goldInk,
                    )
                    Text(
                        text = strings["project.word.none"],
                        style = type.bodyS,
                        color = colors.ink2,
                    )
                }
            }
            Spacer(Modifier.height(Space.sectionGap))
        }

        // -- 5. The file: everything else, as five doors that look like doors.
        // **No folds, no accordions, no counts that vanish.** The steps, the
        // trail, the papers, the people and the setup each already have a
        // screen of their own; the old home reached them through fold-shaped
        // rows that sometimes opened in place and sometimes navigated, which
        // is exactly the grammar the owner called a mess. Five dense rows
        // under one header: every one navigates, every one says what it
        // holds, none of them moves.
        item {
            GroupHeader(labelKey = "project.file")
            Spacer(Modifier.height(Space.headerGap))
            GroupedSurface {
                DenseRow(
                    title = strings["project.fold.steps"],
                    trailing = strings("projects.step_count", "count" to steps.size),
                    chevron = true,
                    divider = true,
                    onClick = onOpenSteps,
                    modifier = Modifier.testTag(ProjectHomeTags.STEPS),
                )
                DenseRow(
                    title = strings["project.fold.trail"],
                    trailing = trailCount.toString(),
                    chevron = true,
                    divider = true,
                    onClick = onOpenTrail,
                    modifier = Modifier.testTag(ProjectHomeTags.TRAIL),
                )
                DenseRow(
                    title = strings["project.fold.papers"],
                    // bidi-ok: a bare count with no direction of its own, the
                    // same shape every fold in the app has always carried.
                    trailing = papers.size.toString(),
                    chevron = true,
                    divider = true,
                    onClick = onOpenPaperwork,
                    modifier = Modifier.testTag(ProjectHomeTags.PAPERS),
                )
                DenseRow(
                    title = strings["project.fold.people"],
                    trailing = peopleCount.toString(),
                    chevron = true,
                    divider = true,
                    onClick = onOpenPeople,
                    modifier = Modifier.testTag(ProjectHomeTags.PEOPLE),
                )
                DenseRow(
                    title = strings["project.setup.open"],
                    chevron = true,
                    divider = false,
                    onClick = onOpenSetup,
                    modifier = Modifier.testTag(ProjectHomeTags.SETUP),
                )
            }
            Spacer(Modifier.height(Space.sectionGap))
        }

        // -- 6. Housekeeping, last, unchanged. --------------------------------
        item {
            QuietButton(
                label = strings["projects.rename"],
                onClick = onRename,
                modifier = Modifier.testTag(ProjectHomeTags.RENAME),
            )
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(ProjectHomeTags.REMOVE),
            )
            Spacer(Modifier.height(Space.sectionGap))
        }
    }
}


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

/**
 * How long this project has been sitting, or null when it has not been.
 *
 * **Months, plural, which is what screen 16 is about.** A fortnight without a
 * call on a process that moves at the speed of a county office is not somebody
 * coming back, it is Tuesday, and a greeting on every project every time would
 * be exactly the wallpaper `Distance` already refuses to be.
 *
 * **Measured from the last thing written down about it**, which is a fact about
 * the file rather than about the person. Nothing here counts visits, and this
 * app does not watch its user.
 *
 * **A coarse date produces nothing**, per rule 17: the distance between
 * "sometime in the spring" and today is not a number anybody gave.
 */
private fun monthsAway(entries: List<Repository.TrailEntry>): Distance.Gap? {
    val last = entries.firstOrNull() ?: return null
    if (!Edtf.isDayPrecise(last.occurredEdtf)) return null
    val gap = Distance.between(
        olderMillis = last.occurredStart,
        newerMillis = System.currentTimeMillis(),
        zone = ZoneId.systemDefault(),
    ) ?: return null
    // `Distance` reports days, weeks, months and years. Only the last two mean
    // somebody has been away rather than busy.
    return gap.takeIf { it.key == "trail.gap.months" || it.key == "trail.gap.years" }
}

/**
 * How long it has been, in this screen's own voice.
 *
 * **Not the trail's gap wording**, which is written for a marker between two
 * rows and says "1 month earlier". Here the sentence is addressed to somebody
 * who has just come back, so it says how long the file has been holding this
 * and that it kept everything. That reassurance is the whole point of the
 * screen: `DESIGN.md` 20.5 screen 16, no shame, no streaks, nothing owed.
 */
private fun returnKey(gap: Distance.Gap): String =
    if (gap.key == "trail.gap.years") "project.return.years" else "project.return.months"

/**
 * What the file was holding while nobody was looking.
 *
 * **The last thing written down and where it stood**, which together are the
 * answer to "what did I leave this in the middle of". Both are already on the
 * screen further down; saying them here is what saves somebody scrolling to
 * remember before they can decide anything.
 */
@Composable
private fun returnLine(
    entries: List<Repository.TrailEntry>,
    standing: Repository.ProjectStanding?,
    strings: Strings,
): String {
    val last = entries.firstOrNull()?.occurredEdtf?.takeIf { it.isNotBlank() }
        ?.let { strings("project.return.last", "date" to EventDateText.render(strings, it)) }
    return Bidi.join(last, standing?.holderLabel)
}

/**
 * One of a project's three verbs. `m3v4-2`, #386.
 *
 * **A Material button with its mark above its word**, rather than the tile this
 * screen used to draw. A tile is for a fixed set of destinations, rule 22, and
 * these are actions: they do something here rather than taking you somewhere.
 *
 * **Only one of the three is filled.** Logging the call is the verb a long
 * process is made of; the other two are things you might also do, and three
 * actions at one weight is the flatness rule 15 names.
 */
@Composable
private fun ProjectAction(
    label: String,
    @DrawableRes symbol: Int,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val content: @Composable ColumnScope.() -> Unit = {
        Symbol(symbol = symbol, contentDescription = null)
        Spacer(Modifier.height(Space.s))
        Text(
            text = label,
            style = HealthTrail.type.label,
            textAlign = TextAlign.Center,
        )
    }
    val shape = Radius.button
    val padding = PaddingValues(horizontal = Space.sm, vertical = Space.m)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.blue,
                contentColor = colors.onBlue,
            ),
            contentPadding = padding,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.sand,
                contentColor = colors.ink,
            ),
            contentPadding = padding,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}
