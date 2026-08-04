package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
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
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.fabScrollClearance
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.components.TabChip
import com.kamsiob.healthtrail.ui.components.FoldRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectTags {
    const val ROOT = "projects_root"
    const val START = "projects_start"
    const val EMPTY = "projects_empty"
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
    onRemove: (Repository.Project) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    var finishedOpen by rememberSaveable { mutableStateOf(false) }

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
                // Projects belong to no section, so gold and the base ladder
                // rather than a section hue, per `DESIGN.md` 4.3.
                TabChip(hue = wholeAppHue(), labelKey = "projects.tab")
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["nav.projects"],
                    style = HealthTrail.type.displayM,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = strings["projects.subtitle"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.cardGap))
            }

            if (projects.isEmpty()) {
                item {
                    Text(
                        text = strings["projects.empty"],
                        style = HealthTrail.type.bodyL,
                        color = colors.ink2,
                        modifier = Modifier.testTag(ProjectTags.EMPTY),
                    )
                    Spacer(Modifier.height(Space.l))
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
                        onOpen = { onOpen(project) },
                        onRemove = { onRemove(project) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            if (finished.isNotEmpty()) {
                item {
                    FoldRow(
                        labelKey = "projects.finished",
                        expanded = finishedOpen,
                        onToggle = { finishedOpen = !finishedOpen },
                        count = finished.size.toString(),
                        modifier = Modifier.testTag(ProjectTags.FINISHED_FOLD),
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
                if (finishedOpen) {
                    for (project in finished) {
                        item(key = project.id) {
                            ProjectRow(
                                project = project,
                                onOpen = { onOpen(project) },
                                onRemove = { onRemove(project) },
                            )
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(Space.s))
                QuietButton(
                    label = strings["projects.start"],
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().testTag(ProjectTags.START),
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
    onRemove: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .removableByLongPress(strings["edit.hint"], onRemove, onOpen)
            .testTag(ProjectTags.row(project.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = strings["projects.status.${project.status}"],
            style = HealthTrail.type.mono,
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

        project.waitingOn?.takeIf { it.isNotBlank() }?.let { who ->
            Spacer(Modifier.height(Space.xs))
            Text(
                text = strings("projects.waiting_on", "who" to who),
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
        }

        // **What is next, rather than how far behind you are.**
        //
        // This row said "2 of 5 steps done" until 2026-08-03, which is a
        // completion count on the person's own work and is what rule 13 rules
        // out. It was also the least useful thing the row could say: somebody
        // scanning five long processes wants to know what to do next.
        //
        // **A project with nothing left says so and does not say it twice.** A
        // finished one already carries "Done" in its own eyebrow, so the line
        // is only worth printing where the steps ran out before the status did.
        if (project.stepCount > 0) {
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
