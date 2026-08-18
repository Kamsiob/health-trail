package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RouteMark
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton

object ThreadTags {
    const val NAME = "care_threads"
    const val ADD = "care_threads_add"
    fun row(id: String) = "care_thread_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_care_threads"
}

/**
 * Care threads: the parallel streams running through one notebook. Rewritten
 * onto `ui/v4`, #386.
 *
 * **No spine, and D187 is why.** A thread is a filter over entries rather than
 * anybody's actual path, which is exactly the distinction `DESIGN.md` 5.2 draws
 * and the reason the old screen drew its line dashed. A dashed line down the
 * side of a list is a costume carrying one word of meaning; the road stays where
 * a road is real, and the thread's own route sits beside its name where it
 * identifies the thread rather than sequencing it.
 *
 * **The route color identifies a thread and never carries the meaning alone.**
 * Every row has its name in words beside the mark, per `DESIGN.md` 9, and the
 * dash pattern travels with the thread everywhere it appears.
 *
 * **A thread with nothing on it is shown, not hidden.** Applying a situation
 * template creates several at once and most are empty on day one. They are
 * places the record will go rather than places it has been, and hiding the empty
 * ones would mean the set the person chose quietly disagrees with the set they
 * see.
 *
 * **The one that is moving leads.** A care notebook runs several threads at once
 * and they are almost never equally live: one has a grievance going through it
 * this week and four have not been touched since spring.
 *
 * **An ended thread is kept and says when it ended.** It is the record of
 * something that happened, not a mistake to clear away, and it keeps its color
 * at a lower opacity so it reads as finished rather than deleted. It used to sit
 * behind a fold. D185.
 */
@Composable
fun CareThreadsScreen(
    threads: List<Repository.ThreadWithCount>,
    /** Opens the thread itself, which nothing could do until 2026-08-03. */
    onOpen: (Repository.CareThread) -> Unit,
    /**
     * Starts a thread from nothing, which nothing could do until 2026-08-12.
     *
     * **A situation template was the only way one had ever been created**, so a
     * person whose situation the fourteen do not cover was locked out of a
     * section of their own notebook. #349, D145.
     */
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.THREADS)
    val zone = remember { java.time.ZoneId.systemDefault() }

    val running = threads.filter { it.endedEdtf.isNullOrBlank() }
        .sortedByDescending { it.lastActivity ?: Long.MIN_VALUE }
    val ended = threads.filter { !it.endedEdtf.isNullOrBlank() }

    Page(
        eyebrow = strings["notebook.section.threads"],
        eyebrowColor = hue.ink,
        title = strings["threads.heading"],
        subtitle = strings["threads.subtitle"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(ThreadTags.ROOT),
        // **The way in floats over the list rather than sitting under it.**
        // D200: it was the last `item` in the `LazyColumn`, and a section
        // screen has no capture button in that corner to compete with.
        // **While the section is empty the way in is in the words, not in
        // the corner.** D200 put the add control on the scaffold so nobody
        // scrolls a list to reach it, and that is right for a list. An empty
        // screen has no list: the empty state carries the action itself, and a
        // floating copy of it would be the same verb twice on one screen and
        // two nodes under one test tag, which is the first entry in
        // `docs/TRAPS.md`. #388.
        fab = if (threads.isEmpty()) {
            null
        } else {
            {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = {
                        Icon(painter = painterResource(Symbols.add), contentDescription = null)
                    },
                    text = { Text(text = strings["threads.add"]) },
                    // The sentence sits on the button's own node, `docs/TRAPS.md`.
                    modifier = Modifier
                        .testTag(ThreadTags.ADD)
                        .semantics { contentDescription = strings["threads.add"] },
                )
            }
        },
    ) {
        if (threads.isEmpty()) {
            item {
                // **One empty state, app-wide.** #388 finding 1: this screen
                // drew its own quiet block with one gray sentence in it while
                // fourteen others centered a faded drawing in a void, and two
                // answers to one question is the same as none. `SectionEmpty`
                // is the answer now, and it carries the section's own mark.
                // bidi-ok: the app's own sentence about an empty list.
                SectionEmpty(
                    name = ThreadTags.NAME,
                    text = strings["threads.empty"],
                    section = Repository.Section.THREADS,
                    actionLabel = strings["threads.add"],
                    onAction = onAdd,
                )
            }
        }

        running.forEachIndexed { index, row ->
            item(key = row.thread.id) {
                ThreadBlock(
                    row = row,
                    hue = hue,
                    // **The leading thread is the one thing on the screen**, in
                    // the section's wash at the hero size. Everything under it
                    // is the same block quiet and one step down the ladder,
                    // which is the jump rule 15 asks for rather than four
                    // things at one size and nothing to look at first.
                    lead = index == 0,
                    zone = zone,
                    onOpen = { onOpen(row.thread) },
                )
            }
        }

        if (ended.isNotEmpty()) {
            item { Eyebrow(text = strings["threads.ended"]) }
        }

        ended.forEach { row ->
            item(key = "ended_${row.thread.id}") {
                ThreadBlock(
                    row = row,
                    hue = hue,
                    lead = false,
                    zone = zone,
                    onOpen = { onOpen(row.thread) },
                )
            }
        }

    }
}

/**
 * One thread: its route, its name, how much is on it and whether it is moving.
 */
@Composable
private fun ThreadBlock(
    row: Repository.ThreadWithCount,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    /** True for the thread that moved most recently, which leads the screen. */
    lead: Boolean,
    zone: java.time.ZoneId,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val base = colors.threadRoutes[row.thread.colorIndex % colors.threadRoutes.size]
    val ended = !row.endedEdtf.isNullOrBlank()
    // **An ended thread keeps its color and drops in opacity**, `DESIGN.md` 5.2,
    // so it reads as finished rather than deleted.
    val route = if (ended) base.copy(alpha = ENDED_THREAD_ALPHA) else base

    Block(
        tone = if (lead) BlockTone.Section else BlockTone.Quiet,
        hue = hue,
        modifier = Modifier
            .semantics(mergeDescendants = true) { }
            .clickable(
                role = Role.Button,
                onClickLabel = strings["threads.open"],
                onClick = onOpen,
            )
            .testTag(ThreadTags.row(row.thread.id)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            RouteMark(color = route, index = row.thread.colorIndex)
            Column(modifier = Modifier.weight(1f)) {
                Body(
                    text = Bidi.isolate(row.thread.label),
                    color = colors.ink,
                    style = if (lead) HealthTrail.type.hero else HealthTrail.type.displayS,
                )
                // The same plural template the table of contents uses, so zero
                // reads as words rather than as a digit and says the same thing
                // in both places.
                Body(
                    text = Bidi.join(
                        strings("notebook.count", "count" to row.entryCount),
                        when {
                            ended -> EventDateText.render(strings, row.endedEdtf)
                            row.lastActivity != null -> strings(
                                "threads.moving",
                                "date" to EventDateText.monthHeading(
                                    strings,
                                    row.lastActivity,
                                    zone,
                                ),
                            )
                            else -> strings["threads.quiet"]
                        },
                    ),
                    style = HealthTrail.type.bodyS,
                )
            }
        }
    }
}

/**
 * How much of its color an ended thread keeps.
 *
 * Far enough to feel past and near enough that the route is still recognizably
 * the same thread it was. `DESIGN.md` 5.2.
 */
private const val ENDED_THREAD_ALPHA = 0.6f
