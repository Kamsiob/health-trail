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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ThreadTags {
    const val NAME = "care_threads"
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = ThreadTags.NAME,
        title = strings["notebook.section.threads"],
        subtitle = strings["threads.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (threads.isEmpty()) {
            item { SectionEmpty(name = ThreadTags.NAME, text = strings["threads.empty"], section = Repository.Section.THREADS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION)) }
        }

        threads.forEachIndexed { index, row ->
            item(key = row.thread.id) {
                ThreadSpineRow(
                    row = row,
                    continuesAbove = index > 0,
                    continuesBelow = index < threads.lastIndex,
                )
            }
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
@Composable
private fun ThreadSpineRow(
    row: Repository.ThreadWithCount,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
) {
    val colors = HealthTrail.colors
    val route = colors.threadRoutes[row.thread.colorIndex % colors.threadRoutes.size]

    // **An ended thread should keep its color and drop to ENDED_THREAD_ALPHA**,
    // per section 5.2, so it reads as finished rather than deleted. That is not
    // wired here because `CareThread` does not carry an ended timestamp yet, and
    // inventing one in the view would be the interface guessing at data the app
    // does not have. The token is defined and waiting.
    SpineRow(
        continuesAbove = continuesAbove,
        continuesBelow = continuesBelow,
        node = route,
        routeColor = route,
        dash = RouteDash.forIndex(row.thread.colorIndex),
    ) {
        Column {
            ThreadRow(row)
            Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun ThreadRow(row: Repository.ThreadWithCount) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val route = colors.threadRoutes[row.thread.colorIndex % colors.threadRoutes.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(ThreadTags.row(row.thread.id))
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Space.sm)
                .clip(CircleShape)
                .background(route),
        )
        Spacer(Modifier.width(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.thread.label,
                style = HealthTrail.type.displayS,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.xs))
            // The same plural template the table of contents uses, so zero
            // reads as words rather than as a digit and says the same thing in
            // both places.
            Text(
                text = strings("notebook.count", "count" to row.entryCount),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
        }
    }
}
