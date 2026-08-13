package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
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
 *
 * **It may open the period it names, and then it wears the chevron.** Law 2:
 * a row that opens another screen ends in a chevron, and one that does not must
 * not carry one. The trail's month headers are the only caller that opts in, and
 * they open that month's review. Every other list leaves [onOpen] null and gets
 * a header that is exactly what it looks like: furniture that does nothing.
 *
 * **Opening the thing whose name is already on screen costs no furniture**,
 * which is why the door lives here rather than as a row somewhere below fourteen
 * entries. The header is where the eye already is when somebody wonders what a
 * month held.
 */
@Composable
fun StickySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    /** What the tap does, in the person's words, for the reader. Null does not open. */
    openLabel: String? = null,
    onOpen: (() -> Unit)? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    // The header is opaque against paper by construction, so the 5.14 press
    // treatment tints from paper rather than from transparent. Without a
    // resting color the rows behind it would show through on press.
    val surface by pressedSurface(interaction, colors.paper)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (onOpen == null) colors.paper else surface)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier
                        .semantics(mergeDescendants = true) {
                            // The month and its count are one stop, and the tap
                            // says what it does rather than leaving a reader
                            // user to guess that a heading is also a door.
                            openLabel?.let {
                                onClick(label = it) { onOpen(); true }
                            }
                        }
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = onOpen,
                        )
                }
            )
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
                    strokeWidth = Space.hairlineWidth.toPx(),
                )
            }
            .padding(start = Space.xs, end = Space.xs, top = Space.xs, bottom = Space.s)
            .semantics { heading() },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // **Weighted, so the count and the chevron survive.** Unweighted, a
        // month name at font scale 2.0 took the whole row and measured both to
        // zero, and the chevron is the only thing saying the header is a door
        // to that month's review. Every sticky header in the app. #371.
        Text(
            text = title,
            style = type.displayS,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.Bottom) {
            if (count != null) {
                Text(text = count, style = type.mono, color = colors.ink2)
            }
            if (onOpen != null) {
                Spacer(Modifier.width(Space.xs))
                Chevron()
            }
        }
    }
}
