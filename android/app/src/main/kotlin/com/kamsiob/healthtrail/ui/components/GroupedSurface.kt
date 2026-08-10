package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.raisedCard
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
 * **No border, and the shadow is the definition.** `DESIGN.md` 4.7 and the ban
 * list in 17: a card outlined with a hairline as its only definition is a
 * current design tell, and this app's surfaces separate by elevation and warmth
 * instead.
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
            // **The shadow before the clip**, so it is drawn outside the
            // surface rather than inside it. This component's own comment has
            // always said "no border, and the shadow is the definition", and
            // until 2026-08-10 it drew a fill and nothing else. #324.
            .raisedCard(Radius.card)
            .clip(Radius.card)
            .background(HealthTrail.colors.card),
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
            row(item, index == items.lastIndex)
        }
    }
}
