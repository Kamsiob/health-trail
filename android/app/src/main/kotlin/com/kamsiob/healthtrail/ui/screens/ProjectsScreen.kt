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
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.HeaderActions
import com.kamsiob.healthtrail.ui.v4.TipsSheet
import com.kamsiob.healthtrail.ui.v4.tipForDestination
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.kamsiob.healthtrail.ui.v4.fabSafeActionBar
import com.kamsiob.healthtrail.ui.v4.fabScrollClearance
import com.kamsiob.healthtrail.ui.v4.RoadSize
import com.kamsiob.healthtrail.ui.v4.RoadStage
import com.kamsiob.healthtrail.ui.v4.RoadStrip
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.hueFor
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.ListRow
import androidx.compose.ui.semantics.contentDescription

object ProjectTags {
    const val ROOT = "projects_root"
    const val START = "projects_start"
    /** The screen's own name, which is also what its empty block is tagged by. */
    const val NAME = "projects"
    const val EMPTY = "section_empty_projects"
    const val EMPTY_START = "section_empty_action_projects"
    const val COUNTS = "projects_counts"
    /** The one project this screen leads with. D205. */
    const val LEAD = "projects_lead"
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
        // **The lead is chosen here rather than passed in**, because it is a
        // fact about the list and not about any one project: whichever live
        // project has the nearest date somebody else set. D205.
        //
        // **A date is a fact from outside, never a judgment.** Rule 2 forbids
        // the app concluding anything and rule 13 forbids it scoring the
        // person's diligence; "a decision is expected on October 17" is neither.
        // It is the one thing on this screen with a clock attached to it, which
        // is why it earns the lead.
        val live = projects.filterNot { it.isFinished }
        val finished = projects.filter { it.isFinished }
        val dated = live
            .mapNotNull { project ->
                cards[project.id]?.nextDate?.dueStart?.let { due -> project to due }
            }
            .minByOrNull { it.second }
            ?.first
        // **With no date anywhere, the lead is the one started most recently**,
        // which is what the repository's own order already puts first. It leads
        // with where it stands instead of a countdown, and the eyebrow says
        // which of the two this is rather than leaving somebody to work it out.
        val lead = dated ?: live.firstOrNull()
        val rest = live.filter { it.id != lead?.id }

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
                // of what a project is.** Rule 20: the app explaining its own
                // organizing scheme on the screen. A count of things is not a
                // score on the person, per rule 13, and the finished half is
                // left out entirely when there is none. D142.
                if (projects.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = strings(
                            "projects.subtitle.counts",
                            "live" to live.size,
                            "finished" to finished.size,
                        ),
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        modifier = Modifier.testTag(ProjectTags.COUNTS),
                    )
                }
                Spacer(Modifier.height(Space.betweenGroups))
            }

            if (projects.isEmpty()) {
                item {
                    // **A warm invitation and never a blank**, 20.5 screen 01.
                    // **No section drawing, because projects are not one of the
                    // twelve.**
                    SectionEmpty(
                        name = ProjectTags.NAME,
                        lead = strings["projects.empty.lead"],
                        text = strings["projects.empty"],
                        actionLabel = strings["projects.start.long"],
                        onAction = onStart,
                    )
                }
            }

            // == The lead ======================================================
            //
            // **One thing leads and it is unmistakable**, `docs/V4.md` 6.1 item
            // 1. Before this the screen was four cards of identical
            // construction and the eye landed nowhere.
            //
            // **It is drawn the way Today draws its hero**: one tonal block, the
            // date as an eyebrow above it, the name at the size, and the block
            // is the door. Rule 16 wants one answer to one question, and Today
            // already answered this one.
            lead?.let { project ->
                item(key = "lead-${project.id}") {
                    ProjectLead(
                        project = project,
                        card = cards[project.id],
                        countdown = countdown(project),
                        dated = project.id == dated?.id,
                        onOpen = { onOpen(project) },
                    )
                    Spacer(Modifier.height(Space.betweenGroups))
                }
            }

            // == Everything else, as rows =====================================
            //
            // **A row, not a card.** Rule 22: a card is for three or more lines
            // actually read, a dense row is for a long scanned list. A project
            // here is a name and one line of fact.
            //
            // **No road at row width.** Three of the four roads on the old
            // screen had their stage labels overlapping or cut, which the owner
            // named before any research was read. The road is a map on the lead
            // and on the project's own screen, and its information travels into
            // the row as words: "In review · 2 of 3". D205.
            if (rest.isNotEmpty()) {
                item {
                    Eyebrow(text = strings["projects.group.live"])
                    Spacer(Modifier.height(Space.withinGroup))
                }
                for (project in rest) {
                    item(key = project.id) {
                        ProjectRow(
                            project = project,
                            card = cards[project.id],
                            onOpen = { onOpen(project) },
                        )
                    }
                }
                item { Spacer(Modifier.height(Space.betweenGroups)) }
            }

            // **Finished is not hidden and never reads as an achievement
            // count.** The group names them, and the count is a fact about the
            // list rather than a score on the person, per rule 13.
            if (finished.isNotEmpty()) {
                item {
                    Eyebrow(
                        text = strings["projects.finished"],
                        modifier = Modifier.testTag(ProjectTags.FINISHED_FOLD),
                    )
                    Spacer(Modifier.height(Space.withinGroup))
                }
                for (project in finished) {
                    item(key = project.id) {
                        ProjectRow(
                            project = project,
                            card = cards[project.id],
                            onOpen = { onOpen(project) },
                        )
                    }
                }
                item { Spacer(Modifier.height(Space.betweenGroups)) }
            }

            // **Not while the screen is empty.** The empty state already
            // carries this action, at the place the eye lands. D200.
            if (projects.isNotEmpty()) item {
                // **It stops before the FAB's corner.** `fabSafeActionBar` is
                // the modifier D81 exists for: full width, the button ran
                // underneath the gold FAB, and at font scale 2.0 the FAB sat on
                // top of its trailing end.
                Action(
                    label = strings["projects.start"],
                    onClick = onStart,
                    modifier = Modifier
                        .fabSafeActionBar()
                        .testTag(ProjectTags.START),
                )
                Spacer(Modifier.height(fabScrollClearance))
            }
        }
    }
}

/**
 * The one project this screen leads with, drawn as the app's own hero. D205.
 *
 * **One tonal block, never full height**, per D198 item 3, and the surface
 * under it stays neutral. The block is the gold wash rather than the gold base
 * for two reasons that are not taste: the saturated gold is what the capture
 * button means app-wide, and the road draws itself in gold and would vanish on
 * a gold ground, which is `docs/V4.md` 6.1 item 4.
 *
 * **The road is here at full width with its labels on**, which is the one place
 * on this screen it can be read. On a card in a list it was a decoration with
 * three of four label rows overlapping or cut.
 *
 * **Nothing here is colored by how the process is going.** Rule 2. The hue is
 * the section's identity and says only that this is a project.
 */
@Composable
private fun ProjectLead(
    project: Repository.Project,
    onOpen: () -> Unit,
    card: Repository.ProjectCard?,
    countdown: String?,
    /** Whether this one leads because of a date, or because it is the newest. */
    dated: Boolean,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.PROJECTS)
    val stages = card?.stages.orEmpty()

    // **The date, the kind, and the countdown, as one eyebrow line.** Raw parts
    // into `Bidi.join`, never pre-isolated ones: join isolates each part
    // itself, so a joined string passed into another join nests the marks.
    val dateLine = card?.nextDate?.let { date ->
        Bidi.join(countdown, date.kind, EventDateText.render(strings, date.dueEdtf))
    }
    // **The support line is what the row would say**, so the lead and the rows
    // answer the same question in the same words.
    val support = Bidi.join(
        card?.holder,
        stageLine(stages, strings),
        project.waitingOn?.takeIf { it.isNotBlank() && card?.holder == null },
    )

    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = hue.wash,
            contentColor = colors.ink,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ProjectTags.LEAD)
            // **One stop for a reader, on the node that takes the tap**, which
            // is `docs/TRAPS.md`'s first entry.
            .semantics(mergeDescendants = true) {
                contentDescription = Bidi.join(
                    strings[if (dated) "projects.lead.eyebrow" else "projects.lead.no_date"],
                    project.name,
                    dateLine,
                    support,
                    stages.takeIf { it.size >= 2 }
                        ?.let { roadSentence(project.name, it, strings) },
                )
                onClick(label = strings["projects.open"]) { onOpen(); true }
            },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Space.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueMark(
                    hue = hue,
                    mark = Symbols.projects,
                    size = Space.markCard,
                )
                Spacer(Modifier.width(Space.s))
                Eyebrow(
                    text = strings[
                        if (dated) "projects.lead.eyebrow" else "projects.lead.no_date",
                    ],
                    color = hue.ink,
                )
            }

            if (dateLine != null) {
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = dateLine,
                    style = HealthTrail.type.bodyL,
                    color = hue.ink,
                )
            }

            Spacer(Modifier.height(Space.s))
            // **The name takes the size, not the countdown.** Somebody scanning
            // this screen is asking which of their processes this is; the date
            // is why it leads, and the name is what it is. Today's hero makes
            // the same call, and rule 16 wants one answer to one question.
            Text(
                text = Bidi.isolate(project.name),
                style = HealthTrail.type.hero,
                color = colors.ink,
            )

            if (support.isNotBlank()) {
                Spacer(Modifier.height(Space.s))
                Text(
                    text = support,
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
            }

            if (stages.size >= 2) {
                Spacer(Modifier.height(Space.l))
                RoadStrip(
                    stages = stages.map { RoadStage(name = it.name, reached = it.isReached) },
                    description = roadSentence(project.name, stages, strings),
                    size = RoadSize.FULL,
                    showLabels = true,
                )
            }
        }
    }
}

/**
 * One project in the list, as a dense row. D205.
 *
 * **Sentence case, and the status is not a tag above the name.** GOV.UK spent
 * two years and three services iterating exactly this and came out at plain
 * sentence case: uppercase was found hard to read, colored status boxes had no
 * findings behind them, and users tried to click a tag that looked like a
 * button. Sources on #399. This screen put an uppercase status above every
 * project's name, so the loudest word on each card was the state rather than
 * the thing.
 *
 * **The status is left off when it would repeat its own group.** A project
 * under "Under way" whose status is "Under way" says it twice, an inch apart.
 */
@Composable
private fun ProjectRow(
    project: Repository.Project,
    onOpen: () -> Unit,
    card: Repository.ProjectCard?,
) {
    val strings = LocalStrings.current
    val stages = card?.stages.orEmpty()

    // **Two facts and never four.** Looked at on the phone: "The bank's legal
    // team · Signed · 2 of 4 · Waiting on somebody" wrapped to three lines and
    // read as a run-on, which is `docs/V4.md` 6.1 item 7, density chosen per
    // surface. A row that is scanned carries who it is with and where it is,
    // and the person's own word for how it is going is one tap away on the
    // project's own screen.
    //
    // **A finished one trades the holder for the ending**, because "done" and
    // "set aside" are the difference between two closed files and nobody
    // remembers which was which a year later.
    val support = if (project.isFinished) {
        Bidi.join(
            strings["projects.status.${project.status}"],
            stageLine(stages, strings),
        )
    } else {
        Bidi.join(
            card?.holder ?: project.waitingOn?.takeIf { it.isNotBlank() },
            stageLine(stages, strings),
        )
    }

    ListRow(
        title = Bidi.isolate(project.name),
        support = support.takeIf { it.isNotBlank() },
        mark = Symbols.projects,
        // **The card size, not the row size**, because every row here is the
        // same kind. #388 finding 8, D198 item 4.
        markSize = Space.markCard,
        markHue = hueFor(Repository.Section.PROJECTS),
        isDoor = true,
        onClick = onOpen,
        clickLabel = strings["projects.open"],
        modifier = Modifier.testTag(ProjectTags.row(project.id)),
    )
}

/**
 * Where a project stands and how far along, as words rather than a drawing.
 *
 * **Never a percentage and never a bar.** Rule 13, and `docs/V4.md`: a
 * months-long process has no percentage, and a bar that races then stalls is
 * the shape Nielsen Norman warns damages trust. These stages are a
 * bureaucracy's requirements, not the person's conscientiousness.
 */
@Composable
private fun stageLine(
    stages: List<Repository.ProjectStage>,
    strings: com.kamsiob.healthtrail.i18n.Strings,
): String? {
    if (stages.size < 2) return null
    val current = stages.indexOfLast { it.isReached }
    if (current < 0) return null
    return strings(
        "projects.row.stage",
        "stage" to stages[current].name,
        "position" to current + 1,
        "total" to stages.size,
    )
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
