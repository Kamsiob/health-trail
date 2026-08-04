package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * One or two lines, 52dp to 56dp, no card, separated by a hairline rather than
 * by a shadow and a gap. `DESIGN.md` section 11.3.
 *
 * **For a long list the person is scanning for something they already have in
 * mind.** Not everything deserves to be a card, and making everything a card is
 * the reason nothing in this app stood out: a card row plus its 12dp gap costs
 * 88dp, and this costs 52dp for the same two lines. On a five year notebook
 * that is the difference between a list you scroll and a list you scan.
 *
 * **The shape is fixed everywhere in the app, and that is the whole point.** The
 * leading element, then what it is, then when, then state, in the same slots on
 * every screen. A person who learns to read one dense row can read every list in
 * the app.
 *
 * **Do not use it** where the person will read rather than scan, where the
 * content genuinely needs three or more lines, or **where there are fewer than
 * five rows**, because four dense rows on an otherwise empty screen read as the
 * top of a longer list that failed to load, where four cards read as four
 * things.
 *
 * Selection, where a list carries it, is `blue_soft` plus the title at weight
 * 700, which is the chip's own language from 5.11 rather than a second one.
 */
@Composable
fun DenseRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /**
     * An avatar, a thumbnail, a waypoint, or a thread swatch. Never an icon
     * tile, which belongs to tiles and to the table of contents.
     */
    leading: (@Composable () -> Unit)? = null,
    /** A date or a count, in Mono. At most one of this and [trailingContent]. */
    trailing: String? = null,
    /** A pill, per 5.6. At most one of this and [trailing]. */
    trailingContent: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    selected: Boolean = false,
    /**
     * How far in the hairline starts, so it runs under the words rather than
     * cutting across the leading element. Defaults to clearing a 32dp leading
     * element and its gap when there is one.
     */
    dividerInset: Dp = if (leading != null) LEADING_SIZE + Space.sm else 0.dp,
    divider: Boolean = true,
    /**
     * Horizontal inset for the row's own content. Defaults to the group row
     * padding in `DESIGN.md` section 6. Pass `0.dp` for a full bleed list whose
     * screen already provides the inset.
     */
    contentPadding: Dp = Space.cardPadding,
    onClick: (() -> Unit)? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val resting = if (selected) colors.blueWash else Color.Transparent
    val surface by pressedSurface(interaction, resting)
    val ring by focusRingAlpha(interaction)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // One stop for the reader. A row is a name, a date, and a state
                // and it is one thing, per D54. Fifty rows at three stops each
                // is a hundred and fifty swipes down one list.
                .semantics(mergeDescendants = true) { }
                .defaultMinSize(minHeight = if (subtitle == null) ONE_LINE else TWO_LINE)
                // The focus ring is drawn at the tile radius rather than across
                // the full bleed, because a ring against the screen edge reads
                // as a border on the window rather than as a focused row.
                .clip(Radius.tile)
                .background(surface)
                .border(2.dp, colors.blue.copy(alpha = ring), Radius.tile)
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = interaction,
                            // The row's own surface is the press feedback, 5.14.
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    },
                )
                // **A row's content never touches its container's edge.**
                // `DESIGN.md` section 6 puts group row padding at 11 to 13dp,
                // and this had none: inside a grouped surface the leading icon
                // sat flush against the card's rounded corner, which clipped it.
                // Seen in Arabic first, where it reads as a layout error rather
                // than as a tight margin, and then in English, where it had
                // been wrong the whole time and simply looked deliberate.
                //
                // Full bleed callers pass zero, which is the trail's case: there
                // the screen's own padding provides the inset and doubling it
                // would push a 1,630 row list needlessly inward.
                .padding(horizontal = contentPadding, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(Space.sm))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = type.bodyL,
                    color = colors.ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = type.bodyM,
                        color = colors.ink2,
                        maxLines = 1,
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(Space.sm))
                Text(text = trailing, style = type.mono, color = colors.ink2)
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(Space.sm))
                trailingContent()
            }
            if (chevron) {
                Spacer(Modifier.width(Space.s))
                Chevron()
            }
        }

        if (divider) {
            Hairline(inset = contentPadding + dividerInset, end = contentPadding)
        }
    }
}

/**
 * The rule between two dense rows.
 *
 * Decorative in the sense 2.3 defines: remove it and nothing becomes
 * unreadable, because the rows carry themselves. So it is measured and reported
 * rather than held to the 3:1 ratio, exactly as the group header's rule is.
 */
@Composable
fun Hairline(modifier: Modifier = Modifier, inset: Dp = 0.dp, end: Dp = 0.dp) {
    val colors = HealthTrail.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Start and end rather than left and right, so in Arabic the rule
            // still runs under the words and still stops short of the far edge.
            .padding(start = inset, end = end)
            .height(1.dp)
            .background(colors.ink3.copy(alpha = HAIRLINE_ALPHA)),
    )
}

/** 52dp for one line and 56dp for two, per 11.3. Both clear the 48dp floor. */
private val ONE_LINE = 52.dp
private val TWO_LINE = 56.dp

/** An avatar or a thumbnail in a dense row, per 11.7 and 11.8. */
internal val LEADING_SIZE = 32.dp

/** The same 40% the group header's rule carries, per 5.13. */
private const val HAIRLINE_ALPHA = 0.4f
