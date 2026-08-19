package com.kamsiob.healthtrail.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Distance
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.MemosAbout
import com.kamsiob.healthtrail.ui.v4.DateRow
import com.kamsiob.healthtrail.ui.v4.LatestWordCard
import com.kamsiob.healthtrail.ui.v4.SpineRow
import com.kamsiob.healthtrail.ui.v4.StandingCard
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.v4.Waypoint
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider
import java.time.ZoneId
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.MaterialTheme
import com.kamsiob.healthtrail.ui.theme.hueFor

/**
 * How present the rule between the answer and its date is.
 *
 * **A hairline, not a border.** It separates two questions inside one block;
 * drawn at full strength it would read as two blocks again, which is the thing
 * D206 merged them to stop.
 */
private const val DATE_RULE_ALPHA = 0.35f

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
    /** The corner that holds setup, the name and removing it. D206. */
    const val MORE = "project-home-more"
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
    /** The memos written about this. Rule 18, #397. */
    memos: List<Repository.TrailEntry> = emptyList(),
    onOpenMemo: (Repository.TrailEntry) -> Unit = {},
    /** Writes one already knowing what it is about. */
    onWriteMemo: () -> Unit = {},
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
    /**
     * Opens one entry by id, which on this screen is the latest word.
     *
     * **The card said it was a door and there was nothing behind it.** It took
     * a separate `onOpenEntry` that no caller ever passed, so its tap ran an
     * empty default while the shell was passing this one and nothing read it.
     * #390, and it is exactly the shape `docs/TRAPS.md` warns about: a row that
     * promises a door may not have one.
     */
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

    Page(
        title = Bidi.isolate(project.name),
        onBack = onBack,
        backLabel = strings["section.back.projects"],
        modifier = modifier.testTag(SectionTags.root(ProjectHomeTags.NAME)),
        eyebrow = strings["notebook.section.projects"],
        // **No subtitle, and that is the point.** "Started from a template. The
        // steps are a starting point, and this one is yours to change" sat
        // above everything on the screen, said nothing about this project, and
        // is the app explaining its own organizing scheme in the most valuable
        // space it has, which is rule 20. It lives in setup now, where somebody
        // asking where the steps came from is already looking. D206.
        badge = stages.lastOrNull { it.isReached }?.name?.takeIf { !project.isFinished },
        // **Housekeeping is in the corner, not in the record.** Setup, the name
        // and removing it were a menu row and two loose tonal pills at the foot
        // of the scroll, which is three things on the page that are not what
        // the page is about. Material's own overflow is where a person looks
        // for them. D206.
        actions = {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag(ProjectHomeTags.MORE),
                ) {
                    Icon(
                        painter = painterResource(Symbols.more),
                        contentDescription = strings["project.more"],
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings["project.setup.open"]) },
                        onClick = { menuOpen = false; onOpenSetup() },
                        modifier = Modifier.testTag(ProjectHomeTags.SETUP),
                    )
                    DropdownMenuItem(
                        text = { Text(strings["projects.rename"]) },
                        onClick = { menuOpen = false; onRename() },
                        modifier = Modifier.testTag(ProjectHomeTags.RENAME),
                    )
                    DropdownMenuItem(
                        text = { Text(strings["remove.action"]) },
                        onClick = { menuOpen = false; onRemove() },
                        modifier = Modifier.testTag(ProjectHomeTags.REMOVE),
                    )
                }
            }
        },
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

        // -- 0. The date, which now sits under the answer rather than over it.
        //
        // **D164 fixed this screen's order and D206 fixes what comes first.**
        // The date led for a while, on the reasoning that a process taking a
        // year has one time critical fact. What that missed is that most of
        // these projects have no date at all, so the screen opened on "No date
        // written down yet" above everything else it knew. Where it stands is
        // the answer this screen exists to give, and it is always there.
        //
        // The date row itself is unchanged and so is everything below it.
        val dateBlock: @Composable () -> Unit = {
            // **Tappable when there is one, per rule 17**: the date is editable
            // forever from the thing itself.
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
                    // The answer's own block is the container now. D206.
                    flat = true,
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
        }

        val away = if (project.isFinished) null else monthsAway(entries)

        // -- 1. The answer. A closed project answers with how it ended. -------
        if (project.isFinished) {
            item {
                StandingCard(
                    eyebrow = strings["project.ended"],
                    holder = strings[endedKey(project.status)],
                    since = endedSpan(project, strings),
                    modifier = Modifier.testTag(ProjectHomeTags.ENDED),
                    actions = {
                        Action(
                            label = strings["project.reopen"],
                            onClick = onReopen,
                            modifier = Modifier.testTag(ProjectHomeTags.REOPEN),
                        )
                    },
                )
            }
            item {
                Eyebrow(text = strings["project.story"])
                Column {
                    ListRow(
                        title = strings["project.story.line"],
                        support = storyLine(project, entries, papers, peopleCount, strings),
                        modifier = Modifier.testTag(ProjectHomeTags.STORY),
                    )
                    RowDivider(inset = false)
                }
                Text(
                    text = strings["project.story.kept"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }
        } else {
            item {
                // **One block, and "while you were away" is inside it now.**
                // D206. Those were two blocks one gap apart saying the same
                // thing: one said "it has been a month, last written down June
                // 29", the other said "the county, waiting on the bank
                // statements, March 21". A person reading down met the same
                // fact twice before reaching anything they could act on.
                //
                // **The eyebrow names the question, the holder answers it at
                // display weight, and how long it has stood there sits under
                // it.** Where the file has gone quiet, the block says so in its
                // own voice and offers the one thing to do about it.
                //
                // **Rule 13 is untouched.** The sentence is about the file, not
                // about the person: nothing here says they should have written
                // something down, and there is no meter on how often they do.
                // **One block, one lead.** Looked at on the phone: the answer
                // sat plain on the canvas and the date sat under it in a gold
                // card with its number drawn larger, so the loudest thing on
                // the screen was the second most important. Two things that
                // could each be the lead is `docs/V4.md` 6.1 item 1 failing.
                //
                // **One tonal block per page, never full height.** D198 item 3,
                // and this is that block: the surfaces above and below it stay
                // neutral.
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = goldHue().wash,
                        contentColor = colors.ink,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                Column(
                    modifier = Modifier
                        .testTag(ProjectHomeTags.STANDING)
                        .padding(Space.ml),
                ) {
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
                    // **The sentence, and no button under it.** The owner,
                    // 2026-08-18: "I don't understand why there are three
                    // places to update ... it's confusing, it's redundant,
                    // it's not natural wording." Two of the three opened the
                    // same sheet: this one and the verb in the row below, an
                    // inch apart. Rule 16 calls that two answers to one
                    // question, and the verb row is the answer, because it is
                    // in the same place on every project forever.
                    //
                    // **The sentence stays, because lapse tolerance is law.**
                    // Rule 13: coming back after a month meets a file that
                    // kept everything, never a prompt to catch up.
                    away?.let { lapse ->
                        Spacer(Modifier.height(Space.sm))
                        Text(
                            text = strings(returnKey(lapse), "count" to lapse.count),
                            style = type.bodyM,
                            color = colors.ink2,
                            modifier = Modifier.testTag(ProjectHomeTags.RETURN),
                        )
                    }
                    // **The date is the second line of the answer, not a rival
                    // to it.** A hairline separates the two questions inside
                    // one block, which is the rhythm token `betweenGroups`
                    // exists for.
                    Spacer(Modifier.height(Space.betweenGroups))
                    HorizontalDivider(color = goldHue().base.copy(alpha = DATE_RULE_ALPHA))
                    Spacer(Modifier.height(Space.sm))
                    dateBlock()
                }
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
                // **One height for the three, and that is the whole defect.**
                // A `Row` aligns its children to the top and lets each one be
                // as tall as its own content, so "Log a call" fits on one line
                // and stood shorter than the two that wrap. Three verbs of
                // equal rank drawn at three different heights is the eye being
                // told they are not equal. `IntrinsicSize.Min` measures the
                // tallest and the members fill it.
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    ProjectAction(
                        label = strings["project.log_call"],
                        symbol = Symbols.call,
                        filled = true,
                        onClick = onLogCall,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(ProjectHomeTags.LOG_CALL),
                    )
                    ProjectAction(
                        label = strings["project.date.add"],
                        symbol = Symbols.appointments,
                        filled = false,
                        onClick = onAddDate,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(ProjectHomeTags.ADD_DATE),
                    )
                    ProjectAction(
                        label = strings["project.standing.update"],
                        symbol = Symbols.edit,
                        filled = false,
                        onClick = onUpdateStanding,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(ProjectHomeTags.UPDATE_STANDING),
                    )
                }
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
                                    Action(
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
                    onOpen = { onOpenEntryById(latestWord.id) },
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
        }

        // **Rule 18's other half**, #397: a memo written about this appears
        // here, and this appears on the memo.
        item {
            MemosAbout(
                memos = memos,
                onOpen = onOpenMemo,
                onWrite = onWriteMemo,
                modifier = Modifier.padding(top = Space.betweenGroups, bottom = Space.betweenGroups),
            )
        }

        // -- 5. The file: four tiles, because they are a fixed set of doors. --
        //
        // **Rule 22 names the component for this shape**: a tile is for a fixed
        // set of destinations, and a dense row is for a long scanned list. This
        // was five dense rows, one of which was settings, which is what the
        // owner meant by "menus and sub menus". Steps, the trail, papers and
        // people are four destinations that never change and never reorder.
        // Setup went to the overflow with the rest of the housekeeping. D206.
        //
        // **Every tile wears the mark and the color of what it opens.** D198,
        // and the hues are `hueFor`'s, the owner's mapping: the papers take the
        // documents' manila, the people take the care team's rose, and what
        // belongs to the project itself takes the gold every whole-app surface
        // takes.
        item {
            val projectHue = hueFor(Repository.Section.PROJECTS)
            Eyebrow(text = strings["project.file"])
            Spacer(Modifier.height(Space.headerGap))
            // **One height across a row, measured rather than assumed.** A
            // `Row` lets each child be as tall as its own content, so a tile
            // whose label wraps stands taller than its neighbor and two doors
            // of equal rank are drawn at two heights. `IntrinsicSize.Min`
            // measures the tallest and the members fill it, which is the same
            // fix the three verbs above already needed.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                FileTile(
                    label = strings["project.fold.steps"],
                    count = strings("projects.step_count", "count" to steps.size),
                    symbol = Symbols.standingInstructions,
                    hue = projectHue,
                    onClick = onOpenSteps,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(ProjectHomeTags.STEPS),
                )
                FileTile(
                    label = strings["project.fold.trail"],
                    count = strings("project.count.trail", "count" to trailCount),
                    symbol = Symbols.trail,
                    hue = projectHue,
                    onClick = onOpenTrail,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(ProjectHomeTags.TRAIL),
                )
            }
            Spacer(Modifier.height(Space.s))
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                FileTile(
                    label = strings["project.fold.papers"],
                    count = strings("project.count.papers", "count" to papers.size),
                    symbol = Symbols.documents,
                    hue = hueFor(Repository.Section.DOCUMENTS),
                    onClick = onOpenPaperwork,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(ProjectHomeTags.PAPERS),
                )
                FileTile(
                    label = strings["project.fold.people"],
                    count = strings("project.count.people", "count" to peopleCount),
                    symbol = Symbols.careTeam,
                    hue = hueFor(Repository.Section.CARE_TEAM),
                    onClick = onOpenPeople,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag(ProjectHomeTags.PEOPLE),
                )
            }
        }
    }
}


/**
 * One door out of a project, as a tile. D206, rule 22.
 *
 * **Material's card owns the surface and the press**, D196, so there is no hand
 * drawn background, no `indication = null`, and no second answer to what a
 * tappable surface looks like under a finger.
 *
 * **The count is a count and never a meter.** Rule 13: nothing here is a
 * fraction of anything the person was supposed to finish.
 */
@Composable
private fun FileTile(
    label: String,
    count: String,
    @DrawableRes symbol: Int,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
        // **An edge the eye can find**, `docs/V4.md` 6.1 item 4, and the same
        // hairline `Block` puts on every other group in the app. With no
        // shadow and no elevation a container is only as visible as the step
        // between its color and the canvas, and in light this palette keeps
        // that step deliberately small.
        border = BorderStroke(Space.hairlineWidth, MaterialTheme.colorScheme.outlineVariant),
        // **One stop for a reader, on the node that takes the tap.**
        modifier = modifier.semantics(mergeDescendants = true) { },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Space.cardPadding)) {
            HueMark(hue = hue, mark = symbol, size = Space.markCard)
            Spacer(Modifier.height(Space.s))
            Text(text = label, style = type.rowTitle, color = colors.ink)
            Spacer(Modifier.height(Space.xs))
            Text(text = count, style = type.bodyS, color = colors.ink2)
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
        Icon(
            painter = painterResource(symbol),
            contentDescription = null,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
    // **Material's own shape, at the Expressive corner.** `Radius.button` was
    // the app's second ladder; `shapes.large` is what this scheme sets for a
    // container this size, and the rounder corner is the Expressive one.
    val shape = MaterialTheme.shapes.large
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
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
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
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}
