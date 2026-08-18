package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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

    /**
     * Something started and not finished: an open incident, an unanswered
     * question, a request nobody has come back on.
     *
     * **A heavier ring than [UPCOMING], and that difference is load bearing.**
     * Open and upcoming are both "not done", and a person scanning a thread has
     * to tell "I am waiting on somebody" from "this has not come round yet",
     * because only the first one is theirs to chase. The grid draws open at a
     * 3px ring against upcoming's 2.5px, filled with the page rather than the
     * ink, so it reads as a hole in the line rather than as a lighter dot.
     */
    OPEN,

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
    surface: Color = MaterialTheme.colorScheme.background,
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
            drawWaypoint(
                center = Offset(this.size.width / 2, this.size.height / 2),
                color = color,
                state = state,
                surface = surface,
                alpha = alpha,
            )
        }
    }
}

/**
 * One waypoint, drawn at a point.
 *
 * **Shared so the two places that draw a node cannot drift.** [WaypointDot]
 * places one as a composable, wherever a row wants to carry the mark; a
 * [SpineRow] draws its own into the gutter canvas, because it needs the row's
 * measured height to place it and asking a composable for that inside a Row
 * measured with `IntrinsicSize.Min` is not something Compose will do:
 * `BoxWithConstraints` is a `SubcomposeLayout` and throws outright.
 */
private fun DrawScope.drawWaypoint(
    center: Offset,
    color: Color,
    state: Waypoint,
    surface: Color,
    alpha: Float,
) {
    val radius = Trail.nodeSize.toPx() / 2

    // The ring in the surface color, drawn first, so whatever the node sits on
    // does not touch it.
    drawCircle(
        color = surface.copy(alpha = alpha),
        radius = radius + Trail.nodeRing.toPx(),
        center = center,
    )

    when (state) {
        Waypoint.HAPPENED -> drawCircle(color.copy(alpha = alpha), radius, center)

        // A heavier ring than upcoming, on the page's own surface, so it reads
        // as a hole in the line. Same 12dp, so an open thing is not a larger
        // thing, which would say it matters more than the ones around it.
        Waypoint.OPEN -> {
            drawCircle(surface.copy(alpha = alpha), radius, center)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius - Trail.openStroke.toPx() / 2,
                center = center,
                style = Stroke(width = Trail.openStroke.toPx()),
            )
        }

        // Hollow, and the stroke sits inside the same 12dp so an upcoming thing
        // does not read as a larger thing.
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

/**
 * A short piece of a thread's route, as the thread's own identity.
 *
 * **The color and the dash together**, per section 5.2.2, never the color
 * alone. A plain dot is what the care threads screen, the trail, and the entry
 * screen each carried independently, and it is the reason two threads that
 * landed on similar colors were indistinguishable in grayscale, to a colorblind
 * reader, and on a phone in sunlight.
 *
 * Decorative: the thread's name always sits beside it, so this carries no
 * content description.
 */
@Composable
fun RouteSwatch(
    color: Color,
    /** The thread's creation order, which is what picks its dash. */
    index: Int,
    modifier: Modifier = Modifier,
) {
    val dash = RouteDash.forIndex(index)
    Canvas(
        modifier = modifier
            // **Wide enough to read as a pattern rather than as a mark.** At
            // the node's own width only two dashes fit, and two dashes is a
            // dash pair, not a route: the whole point is that the rhythm is
            // recognizable at a glance beside a name.
            .width(Trail.swatchWidth)
            .height(Trail.nodeSize)
            .clearAndSetSemantics { },
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = Trail.strokeWidth.toPx(),
            cap = dash.cap,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.on.toPx(), dash.off.toPx())),
        )
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
    routeColor: Color = HealthTrail.colors.gold,
    routeAlpha: Float = Trail.ROUTE_ALPHA,
    dash: RouteDash? = RouteDash.TRAIL,
    /** 0f to 1f, for the draw-in on first appearance. 1f is fully drawn. */
    progress: Float = 1f,
    marker: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(modifier = Modifier.width(Trail.gutterWidth).fillMaxHeight()) {
            // **Mirrored by hand, because a canvas is not.** `Modifier.offset`
            // is layout direction aware and the node used to be placed with
            // one; moving the drawing in here to get the row's height traded
            // that away without saying so, and in Arabic the whole spine sat
            // nineteen dp from the start edge instead of nine. It is the exact
            // trap `absoluteOffset` sets, arrived at from the other side.
            //
            // Found by measuring the same screen in both directions, not by
            // looking: the layout mirrors, the gutter moves to the right, the
            // line is inside it, and it reads as correct until the two
            // screenshots are put side by side.
            val x = when (layoutDirection) {
                LayoutDirection.Rtl -> size.width - Trail.lineCenter.toPx()
                else -> Trail.lineCenter.toPx()
            }

            // **Never below the row's own middle.** `nodeCenterY` was measured
            // against a trail card, which carries a date line above its title.
            // A card with one line and no eyebrow is shorter than that, so a
            // fixed offset put the waypoint below the text it marks, near the
            // card's bottom edge. Rows of both shapes sit next to each other on
            // the prep sheet, where the questions carrying a role label and the
            // ones without it visibly drifted apart, and a spine whose nodes do
            // not line up with their rows reads as a rendering fault rather
            // than as a trail.
            //
            // Clamping keeps a tall row anchored at its first line, which is
            // the whole reason this is an offset and not simply a center.
            val nodeY = minOf(Trail.nodeCenterY.toPx(), size.height / 2)

            val top = if (continuesAbove) 0f else nodeY
            val bottom = if (continuesBelow) size.height else nodeY
            // Strokes in from the top, which is the direction the person reads
            // and the direction the trail travels.
            val end = top + (bottom - top) * progress
            if (end > top) {
                drawLine(
                    color = routeColor.copy(alpha = routeAlpha),
                    start = Offset(x, top),
                    end = Offset(x, end),
                    strokeWidth = Trail.strokeWidth.toPx(),
                    cap = dash?.cap ?: StrokeCap.Round,
                    pathEffect = dash?.let {
                        PathEffect.dashPathEffect(floatArrayOf(it.on.toPx(), it.off.toPx()))
                    },
                )
            }

            // **Drawn here rather than placed as a composable**, so it can use
            // the row's measured height. The gutter is decorative and the row
            // beside it always names the thing in words, so nothing is lost to
            // the reader by this not being a node in the semantics tree.
            if (node != null) {
                drawWaypoint(
                    center = Offset(x, nodeY),
                    color = node,
                    state = state,
                    surface = colors.paper,
                    alpha = progress,
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
    // **No leading gutter of its own.** This is placed inside a [SpineRow]'s
    // content, which is already inset past the line, so adding the gutter again
    // indented it further than every other thing on the screen: the month
    // heading, the cards, and the dates all start at one edge and the marker
    // started 28dp inside them. Seen on the phone, where a misaligned line is
    // obvious and in the code it is arithmetic nobody questions.
    Text(
        text = text,
        style = HealthTrail.type.mono,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}
