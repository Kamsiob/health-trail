package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChartCard
import com.kamsiob.healthtrail.ui.components.ChartHeight
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.Reading as Point

object ProgressTags {
    const val NAME = "progress"
    const val ADD = "progress_add"
    fun measure(id: String) = "progress_measure_$id"
    fun reading(id: String) = "progress_reading_$id"
}

/**
 * Progress: what has been written down over time.
 *
 * **This is the screen in the app where rule 2 is easiest to break and worst to
 * break.** It holds numbers about a person's body, and every convention for
 * showing numbers over time carries a judgment: a range says what is normal, a
 * color says whether today is good, an arrow says which way things are going, a
 * line between two points says what happened in between.
 *
 * So it has none of them. **No range, no threshold, no color coded value, no
 * arrow, no trend.** There is a line now, and the line is drawn under exactly
 * two rules that keep it from becoming a claim: **it has no scale**, so it says
 * shape and never amount, and **a gap is drawn as a gap** rather than joined
 * across, so a silence is visible as silence. Anyone who wants to know what the
 * numbers mean has to ask somebody qualified, which is the correct outcome and
 * the one the app must not quietly substitute for.
 *
 * **The one thing, per law 1: the measure the person touched most recently.** It
 * gets the chart at full height. Everything else is a row carrying its own
 * latest value, and **tapping one makes it the hero** rather than opening
 * anything.
 *
 * **The rows do not wear chevrons, and the grid draws them with chevrons.** A
 * chevron means a screen opens, per law 2, and the per-measure history screen is
 * #199 and does not exist yet. A chevron pointing at nothing is the dead end
 * rule 18 forbids, so until that screen exists these are a choice: the row for
 * what you are looking at is marked, and the others offer to take its place.
 * `DESIGN.md` 15.1 records the departure.
 *
 * **A measure recorded in words has no chart and says so**, rather than showing
 * an empty frame. "Meals finished, two of three" is a real reading and not a
 * number, and plotting it would mean inventing a scale for the person's words.
 *
 * **Who said it is kept.** A value the family measured and a value a clinician
 * stated are different things and the record must not blur them, so a reading
 * from a clinician says so and one from the family carries no label at all,
 * because that is the ordinary case and labeling it would be noise.
 */
@Composable
fun ProgressScreen(
    measures: List<Repository.Measure>,
    readings: List<Repository.Reading>,
    /** Opens the measurement form, which is a real destination rather than a stub. */
    onAddReading: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.PROGRESS)

    val byMeasure = readings.groupBy { it.measureId }

    // **Ordered by what was touched most recently**, which is the order the
    // person is thinking in. A measure with nothing recorded sorts last rather
    // than first, because an empty one is not what they came for.
    val ordered = measures.sortedByDescending { measure ->
        byMeasure[measure.id].orEmpty().mapNotNull { it.occurredStart }.maxOrNull() ?: Long.MIN_VALUE
    }

    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val hero = ordered.firstOrNull { it.id == chosen } ?: ordered.firstOrNull()
    var everyReading by rememberSaveable { mutableStateOf(false) }

    SectionScaffold(
        name = ProgressTags.NAME,
        title = strings["notebook.section.progress"],
        subtitle = strings["progress.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.PROGRESS,
        headingKey = "progress.heading",
    ) {
        if (hero == null) {
            item {
                SectionEmpty(
                    name = ProgressTags.NAME,
                    text = strings["progress.empty"],
                    section = Repository.Section.PROGRESS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                )
            }
            item {
                FilledButton(
                    label = strings["progress.add"],
                    onClick = onAddReading,
                    modifier = Modifier.fillMaxWidth().testTag(ProgressTags.ADD),
                )
            }
            return@SectionScaffold
        }

        // Oldest first for the plot, since a chart reads left to right in time
        // whichever way the layout runs.
        val heroReadings = byMeasure[hero.id].orEmpty().sortedBy { it.occurredStart }

        item(key = "hero_${hero.id}") {
            if (hero.isText) {
                TextMeasureHero(measure = hero, readings = heroReadings)
            } else {
                val points = plot(heroReadings)
                ChartCard(
                    name = hero.name,
                    latest = latestOf(strings, hero, heroReadings) ?: "",
                    readings = points,
                    hue = hue,
                    description = strings(
                        "progress.chart.description",
                        "name" to hero.name,
                        "count" to heroReadings.size,
                        "first" to EventDateText.render(strings, heroReadings.firstOrNull()?.occurredEdtf),
                        "last" to EventDateText.render(strings, heroReadings.lastOrNull()?.occurredEdtf),
                    ),
                    footerStart = strings("progress.readings", "count" to heroReadings.size),
                    footerEnd = spread(hero, heroReadings)?.let {
                        strings("progress.range", "low" to it.first, "high" to it.second)
                    },
                    height = ChartHeight.full,
                    modifier = Modifier.testTag(ProgressTags.measure(hero.id)),
                )
            }
            Spacer(Modifier.height(Space.cardGap))
        }

        // The others, as a choice rather than as a wall of charts. One chart is
        // the hero; four charts at once is four things competing, which law 1
        // says is a screen that is wrong.
        val others = ordered.filter { it.id != hero.id }
        if (others.isNotEmpty()) {
            item(key = "others") {
                GroupedSurface {
                    others.forEachIndexed { index, measure ->
                        val forMeasure = byMeasure[measure.id].orEmpty()
                        DenseRow(
                            title = measure.name,
                            trailing = latestOf(strings, measure, forMeasure, brief = true)
                                ?: strings("progress.readings", "count" to 0),
                            divider = index < others.size - 1,
                            onClick = {
                                chosen = measure.id
                                everyReading = false
                            },
                            modifier = Modifier.testTag(ProgressTags.measure(measure.id)),
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // Every reading for what is on screen, folded and counted, because the
        // chart says shape and the list says what was actually written down.
        item(key = "every") {
            FoldRow(
                labelKey = "progress.every",
                expanded = everyReading,
                onToggle = { everyReading = !everyReading },
                count = heroReadings.size.toString(),
            )
            Spacer(Modifier.height(Space.cardGap))
        }

        if (everyReading) {
            // Newest first here, unlike the plot. A list of what happened reads
            // most recent first everywhere else in this app, and a chart reads
            // left to right in time. They are two different jobs.
            for (reading in heroReadings.reversed()) {
                item(key = reading.id) {
                    ReadingRow(reading = reading, measure = hero)
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item(key = "add") {
            Spacer(Modifier.height(Space.m))
            FilledButton(
                label = strings["progress.add"],
                onClick = onAddReading,
                modifier = Modifier.fillMaxWidth().testTag(ProgressTags.ADD),
            )
        }
    }
}

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
 */
private fun plot(readings: List<Repository.Reading>): List<Point> {
    val usable = readings.filter { it.number != null && it.occurredStart != null }
    if (usable.size < 2) {
        return usable.map { Point(position = 0.5f, value = it.number!!.toFloat()) }
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
        Point(
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

/**
 * The latest value with its date, which is what a row of a measure is for.
 *
 * **A measure recorded in words gives only its date.** "Brighter than
 * yesterday. Ate most of her lunch." is a sentence, and a sentence in the slot
 * a date belongs in is not a trailing value, it is the row's whole width. The
 * words are not shortened to fit, which would be the truncation rule 11
 * forbids; they are one tap away, where the screen has room to say them.
 *
 * @param brief true for a row, false for the hero, which has the whole screen.
 */
private fun latestOf(
    strings: Strings,
    measure: Repository.Measure,
    readings: List<Repository.Reading>,
    brief: Boolean = false,
): String? {
    val latest = readings.maxByOrNull { it.occurredStart ?: Long.MIN_VALUE } ?: return null
    val date = EventDateText.render(strings, latest.occurredEdtf)
    if (brief && measure.isText) return date
    val value = valueOf(latest) ?: return date
    val unit = latest.unit?.takeIf { it.isNotBlank() } ?: measure.unit?.takeIf { it.isNotBlank() }
    return buildString {
        append(value)
        if (unit != null) append(" ").append(unit)
        append(" · ").append(date)
    }
}

/**
 * The lowest and highest numbers recorded, as written.
 *
 * **Stated, never interpreted.** It says what the range of the person's own
 * readings is, which is a fact about what they wrote down, and says nothing
 * about whether that range is anything.
 */
private fun spread(
    measure: Repository.Measure,
    readings: List<Repository.Reading>,
): Pair<String, String>? {
    if (measure.isText) return null
    val numbers = readings.mapNotNull { it.number }
    if (numbers.size < 2) return null
    return format(numbers.min()) to format(numbers.max())
}

/**
 * A recorded number, shown as the person gave it.
 *
 * No rounding, no padding, no thousands grouping. A whole number loses its
 * trailing zero because "128.0" is a claim of precision nobody made.
 */
private fun format(number: Double): String =
    if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()

private fun valueOf(reading: Repository.Reading): String? = when {
    reading.text?.isNotBlank() == true -> reading.text
    reading.number != null -> format(reading.number)
    else -> null
}

/**
 * A measure recorded in words, at the top of the screen where the chart would be.
 *
 * **It says why there is no chart rather than leaving a hole.** An empty plot
 * frame reads as a chart that failed to draw, and rule 11 is explicit that
 * nothing unfinished reaches the person. There is nothing unfinished here: some
 * things are written in words and words do not have a shape.
 */
@Composable
private fun TextMeasureHero(
    measure: Repository.Measure,
    readings: List<Repository.Reading>,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val latest = readings.maxByOrNull { it.occurredStart ?: Long.MIN_VALUE }

    Column(
        modifier = Modifier.fillMaxWidth().testTag(ProgressTags.measure(measure.id)),
    ) {
        Text(text = measure.name, style = HealthTrail.type.bodyM, color = colors.ink2)
        Spacer(Modifier.height(Space.xs))
        Text(
            text = latest?.let { valueOf(it) } ?: strings("progress.readings", "count" to 0),
            style = HealthTrail.type.hero,
            color = colors.ink,
        )
        latest?.let {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = EventDateText.render(strings, it.occurredEdtf),
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
        }
        Spacer(Modifier.height(Space.s))
        Text(
            text = strings["progress.nochart"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}

/**
 * One reading.
 *
 * The value carries the weight and the date is the quiet eyebrow, the same
 * arrangement the trail uses, because the value is what somebody came to read.
 * The unit sits with the value rather than under it, since "128" and "mmHg"
 * are one thing being said.
 */
@Composable
private fun ReadingRow(reading: Repository.Reading, measure: Repository.Measure) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val value = valueOf(reading)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ProgressTags.reading(reading.id))
            .padding(horizontal = Space.sm),
    ) {
        Text(
            text = EventDateText.render(strings, reading.occurredEdtf),
            style = HealthTrail.type.mono,
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.xs))

        Row(verticalAlignment = Alignment.Bottom) {
            if (value != null) {
                Text(
                    text = value,
                    style = HealthTrail.type.displayS,
                    // **One ink, always.** The value is never colored by what
                    // it is, which would be the app judging it.
                    color = colors.ink,
                )
            }
            val unit = reading.unit?.takeIf { it.isNotBlank() }
                ?: measure.unit?.takeIf { it.isNotBlank() }
            if (unit != null) {
                Spacer(Modifier.width(Space.xs))
                Text(text = unit, style = HealthTrail.type.bodyM, color = colors.ink2)
            }
        }

        reading.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(Space.xs))
            Text(text = note, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        // Only said when it is not the ordinary case. Labeling every family
        // reading "recorded by you" would be noise on every row.
        if (reading.source == "clinician") {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["progress.by_clinician"],
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
        }
    }
}
