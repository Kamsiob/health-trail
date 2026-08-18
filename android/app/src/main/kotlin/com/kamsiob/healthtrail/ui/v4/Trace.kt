package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.v4.chartPoints

/**
 * The line a series makes, written from scratch. #386.
 *
 * **This is what `m3v4-0` draws under the tracked measure**, and it is a
 * different drawing from the old plot rather than a recolored one: one line, a
 * soft wash falling away underneath it, and a single ring on the newest reading.
 * The old one put a dot on every point, which at six readings across a card this
 * wide read as a row of beads with a thread through them.
 *
 * **The gap rule is borrowed, not rewritten.** [chartPoints] decides where the
 * pen lifts, from the measure's own rhythm, and it is a unit tested function
 * about time rather than a piece of the old design language. Two copies of that
 * rule is how the Progress screen and this card come to disagree about the same
 * silence, which is the reason its own documentation gives for living where it
 * does. `initialsOf` is borrowed the same way and for the same reason.
 *
 * **It says nothing about the values.** No axis, no scale, no ticks, no baseline
 * at zero, nothing that changes color with a number. Rule 2 and law 4: the line
 * is a shape the readings make, and the numbers themselves are read from the
 * value above it and from the Progress screen. The wash underneath is the line's
 * own color at a fraction of its strength, so it reads as the same mark rather
 * than as a second thing with its own meaning.
 *
 * **One reading is a chart.** A single dot on its own is the honest drawing of a
 * measure taken once, and rule 13 says an unfilled slot reads as "not yet": a
 * card that refused to draw until there were three readings would be the app
 * grading how diligently somebody has been weighing themselves.
 */
@Composable
fun Trace(
    /** The readings themselves, newest first as the repository returns them. */
    readings: List<Repository.Reading>,
    /**
     * The line's color.
     *
     * **The section's `base`, because a line is a shape**, `docs/V4.md` 2.1, and
     * that is what every other chart in the app already takes. The caller passes
     * it rather than choosing here, for the same reason [Block] takes a tone: a
     * component that guessed its own section would be guessing identity.
     */
    line: Color,
    modifier: Modifier = Modifier,
    height: Dp = TraceHeight.card,
) {
    val points = chartPoints(readings)
    val surface = HealthTrail.colors.card

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (points.isEmpty()) return@Canvas

        val lo = points.minOf { it.value }
        val hi = points.maxOf { it.value }
        // A series that did not move has no range to scale against, so it is
        // drawn through the middle. That is the truth about it, and stretching a
        // flat week to fill the card would be the drawing inventing a shape.
        val span = if (hi - lo < FLAT_EPSILON) 1f else hi - lo
        // Room above and below for the ring, so the newest reading is never a
        // half circle clipped by the card's own edge.
        val inset = RING_RADIUS.toPx() + RING_STROKE.toPx()
        val usable = size.height - inset * 2f

        fun at(point: Reading) = Offset(
            x = point.position.coerceIn(0f, 1f) * size.width,
            y = inset + (1f - (point.value - lo) / span) * usable,
        )

        val spots = points.map { at(it) }

        // **One wash under the whole series**, so the card has a single shape
        // rather than a row of islands. It fades to nothing at the foot, which
        // is what stops it reading as a filled area with a baseline: a baseline
        // is a scale, and this app draws none.
        if (spots.size > 1) {
            val under = Path().apply {
                moveTo(spots.first().x, size.height)
                spots.forEach { lineTo(it.x, it.y) }
                lineTo(spots.last().x, size.height)
                close()
            }
            drawPath(
                path = under,
                brush = Brush.verticalGradient(
                    colors = listOf(line.copy(alpha = WASH_ALPHA), Color.Transparent),
                    startY = 0f,
                    endY = size.height,
                ),
            )
        }

        // **One path the eye can follow, and a silence drawn as a silence.**
        //
        // The line used to stop dead at a gap and start again on the far side,
        // which on a real notebook, where readings are irregular by nature,
        // produced three disconnected fragments with a dot marooned in the
        // corner. The owner: "it shouldn't be a broken graph... that just looks
        // broken and weird." He is right, and a chart that reads as a rendering
        // failure is worse than useless: it is the app looking unreliable about
        // the person's own numbers.
        //
        // **The promise it has to keep is that no chart interpolates across a
        // gap**, `TESTING-PERSONAS.md` 5.3 and rule 2. Interpolation is claiming
        // a value that was never recorded. A dash claims nothing: it is the
        // standard way a time series says "nothing was measured here", it is
        // visibly not a measurement, and every actual reading still carries its
        // own dot. So the path is continuous for the eye and honest about the
        // record, which the fragments were not: they said the same thing by
        // saying nothing, and nobody read it that way. D193.
        points.zipWithNext().forEachIndexed { index, (_, next) ->
            val from = spots[index]
            val to = spots[index + 1]
            drawLine(
                color = line,
                start = from,
                end = to,
                strokeWidth = STROKE.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (next.startsSegment) {
                    PathEffect.dashPathEffect(
                        floatArrayOf(DASH.toPx(), DASH_GAP.toPx()),
                        0f,
                    )
                } else {
                    null
                },
            )
        }

        // **A dot on every reading, because the dots are the data.** The line
        // between them is connective tissue; these are the values somebody
        // actually wrote down, and a chart that draws only the last one hides
        // how much is behind it. Each carries a hairline ring in the card's own
        // color so two close together stay countable.
        spots.forEach { spot ->
            drawCircle(color = line, radius = DOT_RADIUS.toPx(), center = spot)
            drawCircle(
                color = surface,
                radius = DOT_RADIUS.toPx(),
                center = spot,
                style = Stroke(width = RING_STROKE.toPx()),
            )
        }

        // **The newest reading is raised**, because it is the figure printed
        // above the card and the ring is the drawing saying which point that is.
        spots.lastOrNull()?.let { spot ->
            drawCircle(color = line, radius = RING_RADIUS.toPx(), center = spot)
            drawCircle(
                color = surface,
                radius = RING_RADIUS.toPx(),
                center = spot,
                style = Stroke(width = RING_STROKE.toPx()),
            )
        }
    }
}

/** How tall a trace is, by what it sits in. */
object TraceHeight {
    /**
     * Inside a card that leads with a value, which is what `m3v4-0` draws.
     *
     * Measured off the drawing: the line occupies 66.7dp between the number
     * above it and the month under it.
     */
    val card: Dp = 68.dp
}

/** Measured off `m3v4-0`: the stroke is a little over two points. */
private val STROKE = 2.2.dp

/** A dot on a reading. Smaller than the ring, because the newest one leads. */
private val DOT_RADIUS = 3.dp

/** The dash that crosses a silence, and the space between dashes. */
private val DASH = 4.dp
private val DASH_GAP = 4.dp

/** The ring on the newest reading, measured off the drawing at 7dp across. */
private val RING_RADIUS = 7.dp

/** Thick enough that a dot sitting on the line still reads as a dot. */
private val RING_STROKE = 2.dp

/**
 * How strong the wash under the line is.
 *
 * Measured off `m3v4-0` at the top of the fill, where the drawing puts the
 * line's own color at about seven percent and lets it fall to nothing.
 */
private const val WASH_ALPHA = 0.07f

/** Below this, a series is flat rather than having a range worth scaling to. */
private const val FLAT_EPSILON = 1e-6f

/**
 * One recorded value.
 *
 * @param position where it sits along the chart's time axis, 0 at the left edge
 *   of the window and 1 at the right. **The caller computes this from the real
 *   dates**, so an irregular series is drawn irregularly rather than being
 *   spaced evenly, which would be its own quiet lie about when things happened.
 * @param value the number, in the measure's own units. Only its relative height
 *   is drawn; the chart never prints an axis scale, because a scale invites the
 *   comparison this app does not make.
 * @param startsSegment true where this reading follows a gap long enough that
 *   the line must break before it. **The caller decides what counts as a gap**,
 *   because that depends on how often the measure is taken: a daily weight and
 *   a quarterly lab round have very different silences.
 */
data class Reading(
    val position: Float,
    val value: Float,
    val startsSegment: Boolean = false,
)

/**
 * The plot points for one measure, positioned by when each reading happened.
 *
 * **Positioned by time, not by index.** Six readings in a week and one four
 * months later are not evenly spaced, and drawing them evenly would be the chart
 * saying something about the rhythm that the record does not.
 *
 * **A gap breaks the line.** The rule is a silence longer than three times the
 * median interval for this measure, which is derived from the measure's own
 * rhythm rather than from a fixed number of days: a daily weight and a quarterly
 * lab round have very different silences, and one threshold for both would draw
 * a break in every lab chart and none in any weight chart.
 *
 * **The value is unscaled and stays that way.** The trace normalizes to its own
 * extremes and draws no axis, which is what keeps the line a shape rather than
 * a measurement somebody could read a number off.
 *
 * **It lives with the trace rather than on the screen that first needed it**,
 * because Today draws the same series at the same measure's tall size, and a
 * second copy of the gap rule is two charts that disagree about the same
 * silence the first time either is touched. The rule is part of the drawing,
 * not part of the Progress screen. It moved here when `ChartCard` was retired.
 */
fun chartPoints(readings: List<Repository.Reading>): List<Reading> {
    val usable = readings.filter { it.number != null && it.occurredStart != null }
    if (usable.size < 2) {
        return usable.map { Reading(position = 0.5f, value = it.number!!.toFloat()) }
    }

    val first = usable.first().occurredStart!!
    val last = usable.last().occurredStart!!
    val span = (last - first).coerceAtLeast(1L).toFloat()

    val intervals = usable.zipWithNext { older, newer ->
        newer.occurredStart!! - older.occurredStart!!
    }.sorted()
    val median = intervals[intervals.size / 2].coerceAtLeast(1L)
    val breakAfter = median * GAP_MULTIPLE

    return usable.mapIndexed { index, reading ->
        val previous = usable.getOrNull(index - 1)
        Reading(
            position = (reading.occurredStart!! - first) / span,
            value = reading.number!!.toFloat(),
            startsSegment = previous != null &&
                reading.occurredStart - previous.occurredStart!! > breakAfter,
        )
    }
}


/**
 * How much longer than usual a silence has to be before the line breaks.
 *
 * Six. It was three, which on a real notebook marked most of the line: readings
 * are irregular by nature, so half the intervals beat three times the median and
 * the drawing said "nothing was measured here" between almost every pair. A rule
 * that fires constantly says nothing at all. Six is a silence somebody would
 * actually notice, and it is what the owner was looking at when he said the
 * chart looked broken. D193. It is a drawing rule and not a judgment: the app is deciding
 * where to lift the pen, never what the silence meant.
 */
private const val GAP_MULTIPLE = 6
