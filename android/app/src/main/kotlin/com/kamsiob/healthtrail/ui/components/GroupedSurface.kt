package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius

/**
 * A raised group holding rows separated by hairlines. `DESIGN.md` section 7.
 *
 * **This is the shape most of the app is made of**, and naming it is what stops
 * every screen assembling its own card-with-rows slightly differently. Before
 * this existed each screen clipped its own `card`, chose its own radius, and
 * decided for itself whether the last row got a divider, which is how a design
 * language quietly acquires four versions of one thing.
 *
 * **A tonal block now, not a raised white card.** #386. The approved mockups
 * draw one quiet warm container per group on a near-white canvas, flat, with
 * the rows inside it: `m3v4-1`'s two notebook groups and `m3v4-3`'s unit list.
 * `docs/V4.md` 4 lists "the hairline-divided white monolith" as the thing being
 * removed, and the white card with a shadow under it was most of what made the
 * app read that way.
 *
 * **The shadow is gone rather than softened.** Elevation on every one of these
 * was doing the job the container's own color does in the new language, and two
 * separations at once is what made the old screens feel busy. Depth is now
 * `paper` against `sand`, which is the same device `DESIGN.md` 4.7 already used
 * for a fold.
 *
 * **No hairline after the last row.** The group's own edge ends it, and a rule
 * under the final row draws a line to nowhere. The caller passes `divider =
 * false` on its last [DenseRow], or uses [GroupedRows] which does it for them.
 *
 * **A group is not a card in the law 2 sense and has no costume of its own.** It
 * is a container. What is tappable inside it is tappable because the row wears a
 * chevron and carries a handler, never because the group looks raised.
 *
 * **When not to use it.** Around a single row, where the group is a shadow drawn
 * around one thing. Around content the person reads at length, which is a card.
 * Around a list long enough to scroll, where the rows should be full bleed with
 * hairlines and no surface at all, so the scroll is not a slab moving under a
 * window.
 */
@Composable
fun GroupedSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(HealthTrail.colors.sand),
    ) {
        content()
    }
}

/**
 * A group built from a list, which handles the last-row divider itself.
 *
 * The common case, and the one where forgetting the final `divider = false` is
 * easiest. Passing the index and the last-ness to the row builder means the
 * caller cannot get it wrong rather than merely being told not to.
 */
@Composable
fun <T> GroupedRows(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, isLast: Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    GroupedSurface(modifier = modifier) {
        items.forEachIndexed { index, item ->
            // **The rows arrive in order rather than all at once**, D168.
            // Every grouped list in the app goes through here, so one line
            // gives the notebook, the care team, the medications and the rest
            // the same rhythm. Reduced motion draws them instantly.
            Box(modifier = Modifier.arrivesInOrder(index)) {
                row(item, index == items.lastIndex)
            }
        }
    }
}
