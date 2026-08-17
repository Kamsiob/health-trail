package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.HeaderActions
import com.kamsiob.healthtrail.ui.components.TipsSheet
import com.kamsiob.healthtrail.ui.components.tipForDestination
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.kamsiob.healthtrail.ui.components.fabSafeActionBar
import com.kamsiob.healthtrail.ui.components.fabScrollClearance
import com.kamsiob.healthtrail.ui.components.RoadSize
import com.kamsiob.healthtrail.ui.components.RoadStage
import com.kamsiob.healthtrail.ui.components.RoadStrip
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Eyebrow

object ProjectTags {
    const val ROOT = "projects_root"
    const val START = "projects_start"
    /** The screen's own name, which is also what its empty block is tagged by. */
    const val NAME = "projects"
    const val EMPTY = "section_empty_projects"
    const val EMPTY_START = "section_empty_action_projects"
    const val COUNTS = "projects_counts"
    const val FINISHED_FOLD = "projects_finished_fold"
    fun row(id: String) = "project_$id"
}

/**
 * Projects: the long processes a family has to run.
 *
 * **This is the app at the sharp end.** A Medicaid application, an appeal
 * against a discharge, a records request. Each takes months and a dozen phone
 * calls, and the value of the template is the ordered steps, which somebody
 * would otherwise discover one call at a time, usually after missing one.
 *
 * **What it is waiting on is shown before anything else about it.** The schema
 * makes that a first class field rather than a note, and its comment says why:
 * these processes stall on other people constantly. "Waiting on the caseworker"
 * is the single most useful thing the app can hold about a project.
 *
 * **The step count is a count, not a progress meter.** Rule 13 forbids progress
 * meters on the person's own diligence, and the distinction that keeps this on
 * the right side of it is whose behavior is being measured. These steps are a
 * bureaucracy's requirements, not the person's conscientiousness, and the count
 * is there so somebody can see how much of a process is left rather than how
 * diligent they have been. There is no bar, no percentage, and nothing that
 * changes tone as it fills.
 *
 * **A finished project stays.** An application that was approved in March is
 * the answer to a question somebody gets asked later.
 */
@Composable
fun ProjectsScreen(
    projects: List<Repository.Project>,
    onOpen: (Repository.Project) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The mini road and the two answers, per project. `DESIGN.md` 20.5 screen 2.
     *
     * **Empty for a project the caller has nothing for**, and the card then
     * draws what it has. A card with no road is a project whose stages were all
     * removed, which law 5 allows, and it is not an error.
     */
    cards: Map<String, Repository.ProjectCard> = emptyMap(),
    /** The countdown for a project, already composed by the caller. */
    countdown: (Repository.Project) -> String? = { null },
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .testTag(ProjectTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
        ) {
            item {
                Spacer(Modifier.height(Space.sm))
                // **No eyebrow chip on a tab root**: it repeated the title
                // word for word, and the bottom bar already says where you
                // are. #376.
                var showTips by remember { mutableStateOf(false) }
                if (showTips) {
                    TipsSheet(
                        tip = tipForDestination("projects"),
                        onDismiss = { showTips = false },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = strings["nav.projects"],
                        style = HealthTrail.type.displayM,
                        color = colors.ink,
                    )
                    HeaderActions(onTips = { showTips = true })
                }
                // **The state of the person's own projects, not a definition
                // of what a project is.** It read "The long processes:
                // applications, appeals, requests. Each one says where it
                // stands, the next date, and the latest word", which is three
                // lines telling somebody who already has three projects what a
                // project is. Rule 20: the app explaining its own organizing
                // scheme on the screen.
                //
                // **Grid screen 02 draws a count here** and nothing recorded a
                // departure, so D142 settles it. A count of things is not a
                // score on the person, per rule 13, and it is the same shape
                // the notebook's section counts already use: the finished half
                // is left out entirely when there is none, so a first project
                // does not arrive beside a zero.
                //
                // **Still nothing before the first one.** The empty state
                // carries its own invitation and a count of none is furniture.
                if (projects.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = strings(
                            "projects.subtitle.counts",
                            "live" to projects.count { !it.isFinished },
                            "finished" to projects.count { it.isFinished },
                        ),
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        modifier = Modifier.testTag(ProjectTags.COUNTS),
                    )
                }
                Spacer(Modifier.height(Space.cardGap))
            }

            if (projects.isEmpty()) {
                item {
                    // **A warm invitation and never a blank**, 20.5 screen 01.
                    // This was one gray paragraph under the subtitle with the
                    // rest of the screen empty below it, which is the shape
                    // 5.17 already solved everywhere else in the app. It uses
                    // that solution rather than a second one, with the line
                    // that says what this place is for taking the size.
                    //
                    // **No section drawing, because projects are not one of the
                    // twelve.** The trail map ground alone is what 5.17
                    // prescribes for a place outside the sections.
                    SectionEmpty(
                        name = ProjectTags.NAME,
                        lead = strings["projects.empty.lead"],
                        text = strings["projects.empty"],
                        actionLabel = strings["projects.start.long"],
                        onAction = onStart,
                        // **More of the height than a section's empty state
                        // gets**, because this one dropped its subtitle and
                        // carries three things rather than one. At 0.62 the
                        // block sat in the upper half with the bottom third of
                        // the screen blank under it, which reads as a screen
                        // that failed to load. Measured by looking at it.
                        modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_TALL),
                    )
                }
            }

            // **Live projects lead, finished ones fold**, per law 1 and law 4:
            // a finished group collapses into a fold rather than sitting among
            // the ones that still need something.
            //
            // **Finished is not hidden and never reads as an achievement
            // count.** The fold names them and counts them, and the count is a
            // fact about the list rather than a score on the person, per rule
            // 13. Nothing here says "3 of 7 done".
            val live = projects.filterNot { it.isFinished }
            val finished = projects.filter { it.isFinished }

            for (project in live) {
                item(key = project.id) {
                    ProjectRow(
                        project = project,
                        card = cards[project.id],
                        countdown = countdown(project),
                        onOpen = { onOpen(project) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            if (finished.isNotEmpty()) {
                item {
                    Eyebrow(text = Bidi.join(strings["projects.finished"], finished.size.toString()), modifier = Modifier.testTag(ProjectTags.FINISHED_FOLD))
                    Spacer(Modifier.height(Space.cardGap))
                }
                for (project in finished) {
                    item(key = project.id) {
                        ProjectRow(
                            project = project,
                            card = cards[project.id],
                            countdown = countdown(project),
                            onOpen = { onOpen(project) },
                        )
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }

            // **Not while the screen is empty.** The empty state already
            // carries this action, at the place the eye lands, and repeating it
            // under a screen with nothing on it is the same control twice.
            if (projects.isNotEmpty()) item {
                Spacer(Modifier.height(Space.s))
                // **It stops before the FAB's corner.** `fabSafeActionBar` is
                // the modifier D81 exists for and this screen was not using it:
                // full width, the button ran underneath the gold FAB, and at
                // font scale 2.0 the FAB sat on top of its trailing end. **A
                // control somebody cannot reach because another control is on
                // top of it is the most basic possible defect**, and it is
                // invisible in a mockup because nothing overlaps until the FAB
                // is real. Seen on the phone at 2.0.
                // **Sized to its label since #371 item 5**, D137. The FAB
                // clearance stays: the button no longer spans the width, and
                // the token is what keeps that true at font scale 2.0 rather
                // than an assumption about how wide the label happens to be.
                Action(
                    label = strings["projects.start"],
                    onClick = onStart,
                    modifier = Modifier
                        .fabSafeActionBar()
                        .testTag(ProjectTags.START),
                )
                // The FAB sits over the bottom of this destination, so the last
                // thing in the list needs room to scroll fully clear of it,
                // from the token rather than from arithmetic here. D81.
                Spacer(Modifier.height(fabScrollClearance))
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: Repository.Project,
    onOpen: () -> Unit,
    card: Repository.ProjectCard? = null,
    countdown: String? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **The card opens the project and nothing else**, per #218. Removing it
    // is on the project's own screen, where the person can see the road, the
    // papers and the people they are removing along with it.
    //
    // **`openableByTap` paints the surface as well as taking the tap**, which
    // is why the background is no longer set here: one place decides what a
    // tappable card looks like at rest and under a finger, per 5.14.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .openableByTap(label = strings["open.action"], onTap = onOpen)
            .testTag(ProjectTags.row(project.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = strings["projects.status.${project.status}"],
            style = HealthTrail.type.eyebrow,
            // Waiting and stalled are stated, never colored as a problem. The
            // app does not have a view about how a bureaucracy is going.
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.xs))

        Text(
            text = Bidi.isolate(project.name),
            style = HealthTrail.type.displayS,
            color = if (project.isFinished) colors.ink2 else colors.ink,
        )

        // **The mini road, which is what makes a card read as a project.**
        // DESIGN.md 20.5 screen 2 and 20.6. Bare, no labels at this size: three
        // mono words under a card in a list is noise, and the road's shape
        // already says where the thing is. The reader gets the sentence.
        val stages = card?.stages.orEmpty()
        if (stages.size >= 2) {
            Spacer(Modifier.height(Space.s))
            // **Named, because an unnamed road is a progress bar.** Four dots
            // on a dashed line say only how far along something is, which is
            // the one thing rule 13 rules out and the one thing this component
            // is not for: a road says *where it stands*, and "In review" is the
            // word somebody repeats on the phone. Grid screen 02 draws the
            // stage names on the card and nothing recorded a departure from it,
            // so D142 settles it.
            //
            // **`LabelsOrList` measures rather than guessing**, laying the
            // longest name out in the label's own style and asking how wide it
            // actually came out, so a road whose names do not fit under their
            // waypoints becomes one line instead of four crushed columns.
            RoadStrip(
                stages = stages.map { RoadStage(name = it.name, reached = it.isReached) },
                description = roadSentence(project.name, stages, strings),
                size = RoadSize.MINI,
                showLabels = true,
            )
        }

        // **Where it stands and the next date, at a glance**, which is what the
        // grid asks a card to answer. One line, because a card in a list is
        // scanned rather than read, and the project's own screen is one tap
        // away for the rest.
        val glance = Bidi.join(
            card?.holder,
            countdown,
            project.waitingOn?.takeIf { it.isNotBlank() && card?.holder == null },
        )
        if (glance.isNotBlank()) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = glance,
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
        }

        // **What is next, and only when the card would otherwise say nothing.**
        //
        // This row said "2 of 5 steps done" until 2026-08-03, which is a
        // completion count on the person's own work and is what rule 13 rules
        // out. It became the next step instead, which was right while a project
        // was a checklist.
        //
        // **Under the Projects grid a card answers where it stands and the next
        // date**, 20.5 screen 2, and printing the next step under those two
        // makes three lines competing where the grid draws one. So it prints
        // only when there is neither: a project nobody has said anything about
        // and that has no date is a real state, and the card should still say
        // something rather than being a title and a road.
        if (project.stepCount > 0 && glance.isBlank()) {
            val next = project.nextStep
            val line = when {
                next != null -> strings("projects.next", "step" to next)
                project.isFinished -> null
                else -> strings["projects.nothing_left"]
            }
            line?.let {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = it,
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
            }
        }
    }
}

/**
 * The road as one sentence, for a reader.
 *
 * The same shape the project's own screen uses, so the card and the screen it
 * opens say the same thing about where the project is.
 */
private fun roadSentence(
    name: String,
    stages: List<Repository.ProjectStage>,
    strings: com.kamsiob.healthtrail.i18n.Strings,
): String {
    val current = stages.indexOfLast { it.isReached }
    return if (current < 0) {
        strings("project.road.not_started", "name" to name, "total" to stages.size)
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
