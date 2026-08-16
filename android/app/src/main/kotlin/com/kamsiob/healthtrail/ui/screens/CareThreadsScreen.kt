package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RouteDash
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.RouteSwatch
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ThreadTags {
    const val NAME = "care_threads"
    const val ADD = "care_threads_add"
    fun row(id: String) = "care_thread_$id"
}

/**
 * Care threads: the parallel streams running through one notebook.
 *
 * **The notebook counted four of these and opened none of them**, which is the
 * shape of dead end rule 18 names: a number that says something exists and no
 * way to reach it.
 *
 * **A thread with nothing on it is shown, not hidden.** Applying a situation
 * template creates several at once and most are empty on day one. They are
 * places the record will go rather than places it has been, and hiding the
 * empty ones would mean the set the person chose quietly disagrees with the
 * set they see.
 *
 * **The route color identifies a thread and never carries the meaning alone.**
 * Every row has its name in words beside the dot, per section 9, and the color
 * is an index into the theme's routes so the dark substitution happens in the
 * theme and a stored color can never fail contrast.
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
) {
    val strings = LocalStrings.current
    val zone = remember { java.time.ZoneId.systemDefault() }

    // **The one that is moving leads.** A care notebook runs several threads at
    // once and they are almost never equally live: one has a grievance going
    // through it this week and four have not been touched since spring.
    // Ordering by when a thread was created says nothing about which is which.
    val running = threads.filter { it.endedEdtf.isNullOrBlank() }
        .sortedByDescending { it.lastActivity ?: Long.MIN_VALUE }
    val ended = threads.filter { !it.endedEdtf.isNullOrBlank() }

    var endedOpen by rememberSaveable { mutableStateOf(false) }

    SectionScaffold(
        name = ThreadTags.NAME,
        title = strings["notebook.section.threads"],
        subtitle = strings["threads.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.THREADS,
        headingKey = "threads.heading",
    ) {
        if (threads.isEmpty()) {
            item {
                SectionEmpty(
                    name = ThreadTags.NAME,
                    text = strings["threads.empty"],
                    section = Repository.Section.THREADS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                    // **The empty state is where this is most needed**, because
                    // a notebook with no situation applied has no threads at
                    // all and the sentence above now says one can be started.
                    // A screen that says something is possible and offers no
                    // way to it is the dead end rule 18 names.
                    actionLabel = strings["threads.add"],
                    onAction = onAdd,
                )
            }
            return@SectionScaffold
        }

        // **A thread with nothing on it is shown, not hidden.** Applying a
        // situation template creates several at once and most are empty on day
        // one. They are places the record will go rather than places it has
        // been, and hiding them would mean the set the person chose quietly
        // disagrees with the set they see.
        running.forEachIndexed { index, row ->
            item(key = row.thread.id) {
                ThreadSpineRow(
                    row = row,
                    continuesAbove = index > 0,
                    continuesBelow = index < running.lastIndex,
                    lead = index == 0,
                    zone = zone,
                    onOpen = { onOpen(row.thread) },
                )
            }
        }

        // **Ended threads fold, and they are kept.** A thread that has finished
        // is the record of something that happened, not a mistake to clear
        // away, and it stays reachable with its own count.
        if (ended.isNotEmpty()) {
            item(key = "ended") {
                Spacer(Modifier.height(Space.s))
                FoldRow(
                    labelKey = "threads.ended",
                    expanded = endedOpen,
                    onToggle = { endedOpen = !endedOpen },
                    count = ended.size.toString(),
                )
                Spacer(Modifier.height(Space.cardGap))
            }

            if (endedOpen) {
                ended.forEachIndexed { index, row ->
                    item(key = "ended_${row.thread.id}") {
                        ThreadSpineRow(
                            row = row,
                            continuesAbove = index > 0,
                            continuesBelow = index < ended.lastIndex,
                            lead = false,
                            zone = zone,
                            onOpen = { onOpen(row.thread) },
                        )
                    }
                }
            }
        }

        // **Under the list rather than in the header**, the same place naming
        // your own measure sits, because the common errand here is opening one
        // of the threads already running and this is the rarer one. It is not
        // hidden: 13.5 calls a capability only its author can find unfinished,
        // and fourteen situations with no way past them was exactly that.
        item(key = "add") {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["threads.add"],
                onClick = onAdd,
                modifier = Modifier.testTag(ThreadTags.ADD),
            )
        }
    }
}

/**
 * A thread on its own route.
 *
 * **A route is a color and a dash pattern together, never a color alone**, per
 * `DESIGN.md` section 5.2.2. This screen carried the color as a plain dot, so
 * two threads that landed on similar colors were indistinguishable in
 * grayscale, to a colorblind reader, and on a phone in sunlight. The dash is
 * assigned by creation order and travels with the thread everywhere it appears.
 *
 * **Dashed rather than continuous**, because a thread is a filter over entries
 * rather than the person's actual path. A chapter journey gets the continuous
 * line. That distinction is the last bullet of 5.2.
 */
/**
 * How much of its color an ended thread keeps.
 *
 * `DESIGN.md` section 5.2: an ended thread keeps its color and drops in
 * opacity, so it reads as finished rather than deleted. Sixty percent is far
 * enough to feel past and near enough that the route is still recognizably the
 * same thread it was.
 */
private const val ENDED_THREAD_ALPHA = 0.6f

@Composable
private fun ThreadSpineRow(
    row: Repository.ThreadWithCount,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
    /** True for the thread that moved most recently, which leads the screen. */
    lead: Boolean,
    zone: java.time.ZoneId,
    onOpen: () -> Unit,
) {
    val colors = HealthTrail.colors
    val base = colors.threadRoutes[row.thread.colorIndex % colors.threadRoutes.size]

    // **An ended thread keeps its color and drops to `ENDED_THREAD_ALPHA`**, per
    // section 5.2, so it reads as finished rather than deleted. It could not be
    // wired until the query carried `ended_edtf`, which it does now: the token
    // had been defined and waiting since the screen was first built.
    val ended = !row.endedEdtf.isNullOrBlank()
    val route = if (ended) base.copy(alpha = ENDED_THREAD_ALPHA) else base


    SpineRow(
        continuesAbove = continuesAbove,
        continuesBelow = continuesBelow,
        node = route,
        routeColor = route,
        dash = RouteDash.forIndex(row.thread.colorIndex),
    ) {
        Column {
            ThreadRow(row = row, lead = lead, route = route, zone = zone, onOpen = onOpen)
            Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun ThreadRow(
    row: Repository.ThreadWithCount,
    lead: Boolean,
    route: androidx.compose.ui.graphics.Color,
    zone: java.time.ZoneId,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            // A tap opens the thread, which is the rule every list in this
            // app learned on 2026-08-03. **#231: it says so now**, instead of
            // announcing "remove" and offering a long press that did nothing.
            // `openableByTap` carries its own interaction source, its own
            // press step and the focus ring, so the hand rolled pair above is
            // gone with it.
            .openableByTap(
                label = strings["threads.open"],
                onTap = onOpen,
                resting = colors.card,
            )
            .testTag(ThreadTags.row(row.thread.id))
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The thread's route rather than a dot, per 5.2.2, so the same thread
        // looks the same here, on the trail, and on an entry.
        RouteSwatch(color = route, index = row.thread.colorIndex)
        Spacer(Modifier.width(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Bidi.isolate(row.thread.label),
                // **The leading thread is the one thing on the screen**, at the
                // hero size. Everything under it is the same row at display
                // weight, which is law 1's scale jump rather than four things
                // at one size and nothing to look at first.
                style = if (lead) HealthTrail.type.hero else HealthTrail.type.displayS,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.xs))
            // The same plural template the table of contents uses, so zero
            // reads as words rather than as a digit and says the same thing in
            // both places.
            Text(
                text = Bidi.join(
                    strings("notebook.count", "count" to row.entryCount),
                    when {
                        !row.endedEdtf.isNullOrBlank() ->
                            EventDateText.render(strings, row.endedEdtf)
                        row.lastActivity != null -> strings(
                            "threads.moving",
                            "date" to EventDateText.monthHeading(strings, row.lastActivity, zone),
                        )
                        else -> strings["threads.quiet"]
                    },
                ),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }
}
