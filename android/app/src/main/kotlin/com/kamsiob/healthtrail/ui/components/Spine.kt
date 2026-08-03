package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.Trail

/**
 * The trail vocabulary, as a system rather than one screen's drawing.
 *
 * `DESIGN.md` section 5.2 and its five parts. **This existed only on the
 * timeline for a week**, which is the whole diagnosis: section 1 bans every
 * cheap way to make a screen interesting, nothing replaced them, and every
 * screen converged on the one pattern that survives the bans, a card with text
 * in it. The answer is not to relax the bans. It is to use the vocabulary this
 * app already owns, which is native here rather than borrowed.
 *
 * **The rule that makes this a system: a shape means the same thing everywhere
 * it appears.** Somebody who learns it on the trail can read a chapter list, a
 * thread, or a search result without being taught twice.
 */

/**
 * What a waypoint's shape says, independent of what its color says.
 *
 * **Color carries the kind, shape carries the state, and the two are read
 * separately.** A hollow `alert` node is an incident that has not happened yet;
 * a filled one is an incident that has. One drawing rule, eight meanings, which
 * is what makes it learnable rather than memorized.
 *
 * **Every state survives grayscale**, because the shape does the work. Section
 * 2.2's rule that every color carries a word alongside it is unchanged.
 */
enum class Waypoint {
    /** Something that happened. The default, and most of what a notebook holds. */
    HAPPENED,

    /** Something upcoming, or expected and not yet here. */
    UPCOMING,

    /** A milestone. **Rare by design:** if everything is ringed, nothing is. */
    MILESTONE,
}

/**
 * A route's dash pattern, which is half of a thread's identity.
 *
 * **A route is a color and a dash together, never a color alone.** Two threads
 * that land on similar colors are still told apart in grayscale, by a
 * colorblind reader, and on a phone in sunlight. Section 5.2.2.
 */
data class RouteDash(val on: Dp, val off: Dp, val cap: StrokeCap = StrokeCap.Butt) {
    companion object {
        /**
         * The four patterns, assigned in the order threads are created.
         *
         * Four rather than more because a fifth would have to be close enough
         * to one of these to be mistaken for it at 2dp, and a distinction the
         * eye cannot make is not a distinction.
         */
        private val ALL = listOf(
            RouteDash(6.dp, 6.dp),
            RouteDash(2.dp, 5.dp),
            RouteDash(10.dp, 4.dp),
            RouteDash(1.dp, 4.dp, StrokeCap.Round),
        )

        /** The trail's own route, which is the first pattern. Section 5.2. */
        val TRAIL = ALL[0]

        /**
         * The pattern for the nth thread, wrapping.
         *
         * Wrapping rather than failing, because a person with five care threads
         * has five care threads and the app does not get to object. The fifth
         * repeats the first, and its color differs.
         */
        fun forIndex(index: Int): RouteDash = ALL[index.mod(ALL.size)]
    }
}

/**
 * One waypoint, drawable anywhere.
 *
 * Standalone so a search result, an entry that belongs to a thread, or a row on
 * Today can carry the same mark the trail uses, which is the point of section
 * 5.2.1. It is decorative: the row it sits beside always names the thing in
 * words, so this carries no content description and is skipped by the reader.
 *
 * @param color what kind of thing this is
 * @param surface the color behind it, which the ring is drawn in so the node
 *   reads as sitting on the line rather than beside it
 */
@Composable
fun WaypointDot(
    color: Color,
    modifier: Modifier = Modifier,
    state: Waypoint = Waypoint.HAPPENED,
    surface: Color = HealthTrail.colors.paper,
    alpha: Float = 1f,
) {
    val ring = Trail.nodeRing
    val size = Trail.nodeSize
    // A milestone needs room for its outer ring, so the box is larger and the
    // drawing inside it is not, which keeps every state optically the same
    // weight on the line.
    val box = if (state == Waypoint.MILESTONE) size + ring * 2 + Trail.milestoneGap * 2 else size + ring * 2

    Box(
        modifier = modifier.size(box).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(box)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = size.toPx() / 2

            // The ring in the surface color, drawn first, so whatever the node
            // sits on does not touch it.
            drawCircle(
                color = surface.copy(alpha = alpha),
                radius = radius + ring.toPx(),
                center = center,
            )

            when (state) {
                Waypoint.HAPPENED -> drawCircle(color.copy(alpha = alpha), radius, center)

                // Hollow, and the stroke sits inside the same 12dp so an
                // upcoming thing does not read as a larger thing.
                Waypoint.UPCOMING -> drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius - Trail.hollowStroke.toPx() / 2,
                    center = center,
                    style = Stroke(width = Trail.hollowStroke.toPx()),
                )

                Waypoint.MILESTONE -> {
                    drawCircle(color.copy(alpha = alpha), radius, center)
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = radius + Trail.milestoneGap.toPx(),
                        center = center,
                        style = Stroke(width = Trail.milestoneRing.toPx()),
                    )
                }
            }
        }
    }
}

/**
 * A row on a spine: the line, a waypoint on it, and content to the inline end.
 *
 * **A chapter list, an incident thread, a milestone arc, and a medication's
 * history across chapters are the same shape**, a line with events on it, and
 * they looked like four unrelated lists. Section 5.2.3.
 *
 * The whole geometry mirrors in right to left, because the row is laid out with
 * ordinary composables rather than absolute positions, and the canvas reads its
 * own width.
 *
 * @param dash null draws a continuous line, which means this is the person's
 *   actual path, a chapter journey or a milestone arc. A dash means this is a
 *   filter over entries, a thread or a search result. That distinction is the
 *   last bullet of 5.2 and it now applies everywhere.
 * @param marker the distance to the previous waypoint, already worded, or null.
 *   Section 5.2.4.
 */
@Composable
fun SpineRow(
    continuesAbove: Boolean,
    continuesBelow: Boolean,
    modifier: Modifier = Modifier,
    node: Color? = null,
    state: Waypoint = Waypoint.HAPPENED,
    routeColor: Color = HealthTrail.colors.blaze,
    routeAlpha: Float = Trail.ROUTE_ALPHA,
    dash: RouteDash? = RouteDash.TRAIL,
    /** 0f to 1f, for the draw-in on first appearance. 1f is fully drawn. */
    progress: Float = 1f,
    marker: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(Trail.gutterWidth).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxHeight().width(Trail.gutterWidth)) {
                val x = Trail.lineCenter.toPx()
                val nodeY = Trail.nodeCenterY.toPx()
                val top = if (continuesAbove) 0f else nodeY
                val bottom = if (continuesBelow) size.height else nodeY
                // Strokes in from the top, which is the direction the person
                // reads and the direction the trail travels.
                val end = top + (bottom - top) * progress
                if (end > top) {
                    drawLine(
                        color = routeColor.copy(alpha = routeAlpha),
                        start = Offset(x, top),
                        end = Offset(x, end),
                        strokeWidth = Trail.strokeWidth.toPx(),
                        cap = dash?.cap ?: StrokeCap.Round,
                        pathEffect = dash?.let {
                            PathEffect.dashPathEffect(
                                floatArrayOf(it.on.toPx(), it.off.toPx()),
                            )
                        },
                    )
                }
            }

            if (node != null) {
                WaypointDot(
                    color = node,
                    state = state,
                    surface = colors.paper,
                    alpha = progress,
                    // **Offset rather than padding.** A milestone's box is
                    // wider than the others, and the line sits 9dp from the
                    // start edge, so centering one on the line is a negative
                    // start of 4dp. Padding refuses a negative value at runtime
                    // with "Padding must be non-negative", which is a crash
                    // rather than a layout artifact, and `ScreenReaderTest`
                    // caught it on the chapters screen within a minute of the
                    // milestone existing.
                    //
                    // `offset` is layout direction aware, so a positive x still
                    // travels toward the end edge in Arabic and the whole spine
                    // mirrors, which `absoluteOffset` would break.
                    modifier = Modifier.offset(
                        x = Trail.lineCenter - Trail.nodeSize / 2 - Trail.nodeRing -
                            if (state == Waypoint.MILESTONE) Trail.milestoneGap else 0.dp,
                        y = Trail.nodeCenterY - Trail.nodeSize / 2 - Trail.nodeRing -
                            if (state == Waypoint.MILESTONE) Trail.milestoneGap else 0.dp,
                    ),
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/**
 * The distance between two waypoints, said in words at the spine.
 *
 * **A trail map tells you how far apart things are and a list does not.**
 * Section 5.2.4. Two calls a week apart read as a week of calls; the same two
 * rows four months apart read as somebody left alone until something happened,
 * and the list shows the same rows either way.
 *
 * **It is never a judgment.** It is arithmetic on two dates the person recorded.
 * Nothing says a gap was too long and no gap is colored, per rule 2.
 */
@Composable
fun DistanceMarker(text: String, modifier: Modifier = Modifier) {
    val colors = HealthTrail.colors
    Row(modifier = modifier.fillMaxWidth()) {
        // Aligned to the spine, so it reads as a note on the line rather than
        // as a row of its own.
        Box(modifier = Modifier.width(Trail.gutterWidth + Space.s))
        Text(
            text = text,
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
    }
}
