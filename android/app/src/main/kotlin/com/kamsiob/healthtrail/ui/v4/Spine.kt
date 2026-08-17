package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Trail

/**
 * The road: what happened, where it is now, and what is still ahead. #386.
 *
 * **`m3v4-2` is the one drawing with a spine on it** and this is that spine,
 * measured rather than judged: a 24dp filled node carrying a check for a stop
 * that happened, a 20dp disc inside a 28dp halo for where it is now, an 18dp
 * outline for what is ahead, a 4dp line, and a 56dp gutter. D183.
 *
 * **Solid above where you are and pale below it**, which is the whole point:
 * the road behind is a fact and the road ahead is an expectation. The old spine
 * dashed everything in one color and used size to say state, at half these
 * measurements; this one says it in weight and color, which survives grayscale
 * and a person who cannot tell gold from gray. `DESIGN.md` 12.
 *
 * **The node sits on the row's first line**, clamped to half the row's height,
 * so a long stop does not push its own node down to the bottom of the words it
 * marks and a short one still centers.
 *
 * **A spine is for a path, never for a filter.** D187: a list of things that
 * merely share a subject is a list, and drawing a road under it says a sequence
 * that is not there.
 */
@Composable
fun Road(
    stop: Stop,
    modifier: Modifier = Modifier,
    /** Whether the road continues above this stop, which the first one ends. */
    continuesAbove: Boolean = true,
    /** Whether it continues below, which the last one ends. */
    continuesBelow: Boolean = true,
    /**
     * Whether the half above this stop has been traveled.
     *
     * **The road does not know which way time runs on a screen.** `m3v4-2`'s
     * project reads forward, so what is above a stop is behind you; the
     * chapters read backward, current place first, so what is *below* a stop is
     * behind you. Defaulting from the stop alone drew the road already walked
     * in the pale color of a road ahead, on the one screen that reads the other
     * way. Seen on the phone, rule 21.
     */
    traveledAbove: Boolean = stop != Stop.Ahead,
    /** Whether the half below has been traveled. */
    traveledBelow: Boolean = stop == Stop.Done,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val done = colors.goldInk
    val ahead = colors.hairlineHeavy
    // **Material's own mark, drawn into the canvas as a painter.** D182: this
    // app does not author glyphs, and a check drawn with two `drawLine` calls
    // would be exactly that. The painter is the same vector every symbol uses.
    val check = painterResource(Symbols.check)
    val checkTint = ColorFilter.tint(colors.paper)

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(modifier = Modifier.width(Trail.roadGutter).fillMaxHeight()) {
            // **Mirrored by hand, because a canvas is not.** A layout direction
            // aware offset is not available inside a draw scope, and the trap
            // the old spine records is arriving here from the other side: in
            // Arabic the whole road would sit at the wrong edge and read as
            // correct until two captures are put side by side.
            val x = when (layoutDirection) {
                LayoutDirection.Rtl -> size.width - Trail.roadLineCenter.toPx()
                else -> Trail.roadLineCenter.toPx()
            }
            val centerY = minOf(Trail.roadNodeCenterY.toPx(), size.height / 2f)
            val stroke = Trail.roadLine.toPx()

            // **The half above a stop is solid once you have reached it**, and
            // the half below is solid only once you have passed it. Two halves
            // per stop means the joint between two rows always agrees.
            if (continuesAbove) {
                drawLine(
                    color = if (traveledAbove) done else ahead,
                    start = Offset(x, 0f),
                    end = Offset(x, centerY),
                    strokeWidth = stroke,
                )
            }
            if (continuesBelow) {
                drawLine(
                    color = if (traveledBelow) done else ahead,
                    start = Offset(x, centerY),
                    end = Offset(x, size.height),
                    strokeWidth = stroke,
                )
            }

            when (stop) {
                Stop.Done -> {
                    drawCircle(
                        color = done,
                        radius = Trail.roadNodeDone.toPx() / 2f,
                        center = Offset(x, centerY),
                    )
                    val mark = Trail.roadNodeDone.toPx() * DONE_MARK
                    translate(left = x - mark / 2f, top = centerY - mark / 2f) {
                        with(check) { draw(size = Size(mark, mark), colorFilter = checkTint) }
                    }
                }

                Stop.Now -> {
                    drawCircle(
                        color = colors.goldWash,
                        radius = Trail.roadNodeNowHalo.toPx() / 2f,
                        center = Offset(x, centerY),
                    )
                    drawCircle(
                        color = colors.gold,
                        radius = Trail.roadNodeNow.toPx() / 2f,
                        center = Offset(x, centerY),
                    )
                }

                Stop.Ahead -> drawCircle(
                    color = colors.ink3,
                    radius = (Trail.roadNodeAhead.toPx() - Trail.roadNodeAheadStroke.toPx()) / 2f,
                    center = Offset(x, centerY),
                    style = Stroke(width = Trail.roadNodeAheadStroke.toPx()),
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/**
 * Where a stop stands, which is the only thing the road says about it.
 *
 * **Three states and no fourth.** Rule 2: this is a position on a path the
 * person wrote down, never a judgment about whether it is going well.
 */
enum class Stop {
    /** Behind you. Filled, and it carries the check the drawing puts in it. */
    Done,

    /** Where it is now. The one thing on the road the eye should find first. */
    Now,

    /** Still ahead. An outline, because nothing has happened yet. */
    Ahead,
}

/**
 * How much of a finished node the check fills, measured off `m3v4-2`: a little
 * over half, so the disc still reads as a disc.
 */
private const val DONE_MARK = 0.58f

/**
 * A thread's own route, drawn as a swatch beside its name. #386.
 *
 * **A route is a color and a dash pattern together, never a color alone**,
 * `DESIGN.md` 5.2.2: two threads that land on similar colors are otherwise
 * indistinguishable in grayscale, to a colorblind reader, and on a phone in
 * sunlight. The dash is assigned by creation order and travels with the thread
 * everywhere it appears.
 *
 * **Wide enough to read as a pattern rather than as a mark.** At a node's width
 * only two dashes fit, and two dashes is a dash pair, not a route.
 *
 * **Decorative**, because the name is always beside it.
 */
@Composable
fun RouteMark(
    color: Color,
    /** The thread's creation order, which is what picks its dash. */
    index: Int,
    modifier: Modifier = Modifier,
) {
    val dash = RouteDash.forIndex(index)
    Canvas(
        modifier = modifier
            .width(Trail.swatchWidth)
            .height(Trail.nodeSize)
            .clearAndSetSemantics { },
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = Trail.strokeWidth.toPx(),
            cap = dash.cap,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.on.toPx(), dash.off.toPx())),
        )
    }
}
