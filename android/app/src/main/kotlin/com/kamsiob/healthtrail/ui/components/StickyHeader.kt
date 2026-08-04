package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The context header that sticks while a long list scrolls under it.
 * `DESIGN.md` section 7, and the first of law 4's four tools.
 *
 * **So the person always knows where they are.** On a notebook holding 1,630
 * entries, scrolling without this means arriving somewhere in 2024 with no way
 * to tell without scrolling back. The count beside the month is what makes it
 * a header rather than a label: "June 2026, 14 entries" says how much of this
 * month there is before the person decides whether to read it.
 *
 * **It sits on `paper`, not on a card**, because it is the page's own furniture
 * rather than an item in the list. It is opaque so the rows genuinely disappear
 * behind it rather than showing through, which is what makes it read as sticking
 * rather than as floating.
 *
 * **The month is Bricolage and the count is Mono**, which is the same split as
 * everywhere else: a month is something a person reads, a count is data.
 *
 * **It is a heading for a screen reader**, so a reader user can jump between
 * months by heading rather than swiping through every entry in a five year
 * record. That is the single most useful thing this component does for somebody
 * not using their eyes, and it is one line.
 *
 * **When not to use it.** On a list that cannot grow past a screenful, where it
 * is furniture with nothing to do. It is one of the four tools a list earns
 * **the moment it can grow**, not before.
 */
@Composable
fun StickySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .drawBehind {
                // A hairline under it, so the boundary is visible the moment a
                // row slides beneath. Decorative in the 4.6 sense: remove it and
                // nothing becomes unreadable, because the type carries the
                // heading on its own.
                val y = size.height
                drawLine(
                    color = colors.hairline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(start = Space.xs, end = Space.xs, top = Space.xs, bottom = Space.s)
            .semantics { heading() },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = type.displayS, color = colors.ink)
        if (count != null) {
            Text(text = count, style = type.mono, color = colors.ink2)
        }
    }
}
