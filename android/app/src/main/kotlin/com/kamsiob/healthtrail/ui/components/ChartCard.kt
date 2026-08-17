package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * One measure over time: its name, its latest value, and a plain line.
 * `DESIGN.md` section 7.
 *
 * **This component is where the app is most tempted to break its own rules, so
 * the rules are in the type rather than only in this comment.** There is no
 * parameter for a target, a normal range, a threshold, a zone, or a color
 * ramp, and there is no way to pass one. `CLAUDE.md` rule 2 and `DESIGN.md`
 * section 17 both ban them, and a chart is the one place where somebody
 * reasonable will eventually think a reference line would help. **It would be
 * medical interpretation, which this app does not do**, and the absence is
 * enforced by there being nothing to call.
 *
 * **The line is one color, the measure's section hue, and it never changes with
 * the value.** A line that turns red when a number is high is the app telling
 * somebody their mother is unwell, which is a doctor's job.
 *
 * **Gaps are drawn as gaps.** This is the load-bearing rule. A measurement
 * nobody took is not a value, and joining the dots across three missing months
 * invents a line the person never recorded. The chart breaks into segments and
 * says in words how long the gap was. **Interpolation here would be the app
 * fabricating data in a record whose entire purpose is being true.**
 *
 * **A screen reader gets the whole thing in one description**, because a chart
 * announced as a list of coordinates is useless: the measure, how many
 * readings, the range of dates, the latest value, and whether there are gaps.
 */
@Composable
fun ChartCard(
    name: String,
    latest: String,
    readings: List<Reading>,
    hue: TabHue,
    description: String,
    modifier: Modifier = Modifier,
    footerStart: String? = null,
    footerEnd: String? = null,
    height: Dp = ChartHeight.standard,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(colors.card)
            .padding(horizontal = Space.cardPadding, vertical = Space.sm)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            // **Both weighted**, because a measure is named by the person and
            // "Blood pressure, sitting, left arm" measured first against the
            // whole row and left the reading beside it a column one character
            // wide. `DenseRow` documents finding this exact shape on this exact
            // screen and was fixed; the chart above it was not. #361.
            Text(
                text = name,
                style = type.displayS,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = latest,
                style = type.mono,
                color = colors.ink2,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        Plot(
            readings = readings,
            // **A mark cannot be drawn in the color of the thing it is drawn
            // on.** The line is the section's own hue, which is identity on a
            // white card and invisible inside a hero, because a hero *is* that
            // hue at full strength. On Today's arranged lead the chart was a
            // scatter of faint rings with no line at all: the stroke was there,
            // painted in exactly the background color.
            //
            // **`onHue` flips the ink ladder and cannot reach this**, because
            // this color comes from the `TabHue` rather than from the palette.
            // D172's rule still holds, that the palette flips and call sites do
            // not, so the test is on the palette: if the paper under this chart
            // is already the hue, the mark takes the ink that was chosen to
            // read against it.
            line = if (colors.paper == hue.base) colors.ink else hue.base,
            height = height,
        )

        if (footerStart != null || footerEnd != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = footerStart.orEmpty(),
                    style = type.mono,
                    color = colors.ink2,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = footerEnd.orEmpty(),
                    style = type.mono,
                    color = colors.ink2,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

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

object ChartHeight {
    /** In a list of measures, where the chart is the hero of its own row. */
    val standard: Dp = 52.dp
    /** On a screen showing one measure, where the chart gets the space. */
    val full: Dp = 82.dp
}

@Composable
internal fun Plot(readings: List<Reading>, line: Color, height: Dp) {
    val colors = HealthTrail.colors

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (readings.isEmpty()) return@Canvas

        val lo = readings.minOf { it.value }
        val hi = readings.maxOf { it.value }
        // A flat series has no range to scale against. Drawing it through the
        // middle is honest: the values did not move.
        val span = if (hi - lo < FLAT_EPSILON) 1f else hi - lo
        val inset = DOT_RADIUS.toPx() * 2f
        val usable = size.height - inset * 2f

        fun point(r: Reading) = Offset(
            x = r.position.coerceIn(0f, 1f) * size.width,
            y = inset + (1f - (r.value - lo) / span) * usable,
        )

        // **Segments, not one path.** A new segment starts wherever the caller
        // marked a gap, and the two are never joined. This is the whole rule.
        var path = Path()
        var open = false
        readings.forEachIndexed { index, reading ->
            val p = point(reading)
            if (index == 0 || reading.startsSegment) {
                if (open) drawPath(path, color = line, style = Stroke(width = STROKE.toPx()))
                path = Path().apply { moveTo(p.x, p.y) }
                open = true
            } else {
                path.lineTo(p.x, p.y)
            }
        }
        if (open) drawPath(path, color = line, style = Stroke(width = STROKE.toPx()))

        // Dots last, so a reading is never hidden under the line that connects
        // it. Every recorded value gets one, including a lone reading with no
        // line at all, which is a legitimate chart rather than an empty one.
        readings.forEach { reading ->
            val p = point(reading)
            drawCircle(color = line, radius = DOT_RADIUS.toPx(), center = p)
            // A hairline ring in the card's own color, so two dots close
            // together stay countable rather than merging into a blob.
            drawCircle(
                color = colors.card,
                radius = DOT_RADIUS.toPx(),
                center = p,
                style = Stroke(width = RING.toPx()),
            )
        }
    }
}

private val STROKE = 2.2.dp
private val DOT_RADIUS = 3.dp
private val RING = 0.75.dp

/** Below this, a series is flat rather than having a range worth scaling to. */
private const val FLAT_EPSILON = 1e-6f

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
 * **The value is unscaled and stays that way.** `ChartCard` normalizes to its
 * own extremes and draws no axis, which is what keeps the line a shape rather
 * than a measurement somebody could read a number off.
 *
 * **It lives here rather than on the screen that first needed it**, because the
 * Today surface draws the same series at the same measure's tall size, and a
 * second copy of the gap rule is two charts that disagree about the same
 * silence the first time either is touched. The rule is part of the chart, not
 * part of the Progress screen.
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
 * Three, which is loose enough that an ordinary irregular week does not shatter
 * a chart into fragments and tight enough that a season with nothing in it is
 * visible as one. It is a drawing rule and not a judgment: the app is deciding
 * where to lift the pen, never what the silence meant.
 */
private const val GAP_MULTIPLE = 3
