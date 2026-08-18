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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.RowDivider
import com.kamsiob.healthtrail.ui.v4.StatBlock

object ProgressTags {
    const val NAME = "progress"
    const val ADD = "progress_add"
    fun measure(id: String) = "progress_measure_$id"
    fun reading(id: String) = "progress_reading_$id"
    const val CORRECT_MEASURE = "progress_correct_measure"
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
    /** Opens the form that corrects one reading. #374. */
    onCorrectReading: (Repository.Reading) -> Unit = {},
    /** Opens the form that corrects the measure on screen. #374. */
    onCorrectMeasure: (Repository.Measure) -> Unit = {},
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

    Page(
        title = strings["progress.heading"],
        onBack = onBack,
        backLabel = strings[LocalSectionBackKey.current],
        modifier = modifier.testTag(SectionTags.root(ProgressTags.NAME)),
        eyebrow = strings["notebook.section.progress"],
        subtitle = strings["progress.subtitle"],
        section = Repository.Section.PROGRESS,
        // **The corner, where D173 puts a correction on every other screen.**
        // It was a pill in the body of the page, under the measure it
        // corrects, which meant a control that moved as the page grew. The
        // pencil is in the same place on every screen, and what it changes is
        // said in its own words for a reader.
        onEdit = hero?.let { { onCorrectMeasure(it) } },
        editLabel = hero?.let {
            strings[if (it.isText) "progress.correct.name" else "progress.correct.measure"]
        },
        editTag = ProgressTags.CORRECT_MEASURE,
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
                Action(
                    label = strings["progress.add"],
                    onClick = onAddReading,
                    modifier = Modifier.fillMaxWidth().testTag(ProgressTags.ADD), emphasis = ActionEmphasis.Main,
                )
            }
            return@Page
        }

        // Oldest first for the plot, since a chart reads left to right in time
        // whichever way the layout runs.
        val heroReadings = byMeasure[hero.id].orEmpty().sortedBy { it.occurredStart }

        item(key = "hero_${hero.id}") {
            if (hero.isText) {
                // **A measure written in words is still a measure.** It was
                // the one thing on this page drawn as bare text on the canvas
                // while everything else was a card, so the screen showed its
                // own subject two ways. The words are the value; there is no
                // line, because there is nothing to plot, and the card says so
                // in the footnote rather than in a paragraph of its own.
                val latest = latestParts(strings, hero, heroReadings)
                StatBlock(
                    name = hero.name,
                    value = latest?.figure,
                    description = strings(
                        "progress.chart.description",
                        "name" to hero.name,
                        "count" to heroReadings.size,
                        "first" to EventDateText.render(strings, heroReadings.firstOrNull()?.occurredEdtf),
                        "last" to EventDateText.render(strings, heroReadings.lastOrNull()?.occurredEdtf),
                    ),
                    hue = hue,
                    modifier = Modifier.testTag(ProgressTags.measure(hero.id)),
                    count = strings("progress.readings", "count" to heroReadings.size),
                    footnote = listOfNotNull(latest?.date, strings["progress.nochart"])
                        .let { Bidi.join(it) }
                        .takeIf { it.isNotBlank() },
                    empty = strings["progress.empty"],
                )
            } else {
                // **The v4 stat block, which is this card in the new
                // language**: the measure's own name as its eyebrow, the
                // latest reading at display size, the count as a chip, and
                // the trace under it drawing one path with the silences
                // dashed rather than the line stopping dead. D193.
                val latest = latestParts(strings, hero, heroReadings)
                StatBlock(
                    name = hero.name,
                    value = latest?.figure,
                    description = strings(
                        "progress.chart.description",
                        "name" to hero.name,
                        "count" to heroReadings.size,
                        "first" to EventDateText.render(strings, heroReadings.firstOrNull()?.occurredEdtf),
                        "last" to EventDateText.render(strings, heroReadings.lastOrNull()?.occurredEdtf),
                    ),
                    hue = hue,
                    modifier = Modifier.testTag(ProgressTags.measure(hero.id)),
                    unit = latest?.unit,
                    count = strings("progress.readings", "count" to heroReadings.size),
                    readings = heroReadings,
                    // When the newest reading was taken, and the spread beside
                    // it where there is one. A date, never a judgment.
                    footnote = listOfNotNull(
                        latest?.date,
                        spread(hero, heroReadings)?.let {
                            strings("progress.range", "low" to it.first, "high" to it.second)
                        },
                    ).let { Bidi.join(it) }.takeIf { it.isNotBlank() },
                    empty = strings["progress.empty"],
                )
            }
        }
        // The others, as a choice rather than as a wall of charts. One chart is
        // the hero; four charts at once is four things competing, which law 1
        // says is a screen that is wrong.
        // **A measure is a measure wherever it is drawn**, which is what
        // `m3v4-0` settles: the tracked thing is a card with its name as the
        // eyebrow, the latest reading at display size, the count as a chip and
        // the line under it. A row with the reading typed at the end was this
        // screen showing its own subject in a shape the drawing never uses,
        // and it is the reason the page read as a list of strings. #387.
        //
        // **The hero is still the one with the room.** These are the same card
        // without the footnote, so one screen has one leading thing and the
        // others are still measures rather than links to measures.
        val others = ordered.filter { it.id != hero.id }
        others.forEach { measure ->
            item(key = "measure_${measure.id}") {
                val forMeasure = byMeasure[measure.id].orEmpty()
                    .sortedBy { it.occurredStart }
                val latest = latestParts(strings, measure, forMeasure)
                StatBlock(
                    name = measure.name,
                    value = if (measure.isText) latest?.date else latest?.figure,
                    description = strings(
                        "progress.chart.description",
                        "name" to measure.name,
                        "count" to forMeasure.size,
                        "first" to EventDateText.render(strings, forMeasure.firstOrNull()?.occurredEdtf),
                        "last" to EventDateText.render(strings, forMeasure.lastOrNull()?.occurredEdtf),
                    ),
                    hue = hue,
                    modifier = Modifier.testTag(ProgressTags.measure(measure.id)),
                    unit = if (measure.isText) null else latest?.unit,
                    count = strings("progress.readings", "count" to forMeasure.size),
                    readings = if (measure.isText) emptyList() else forMeasure,
                    footnote = latest?.date?.takeIf { !measure.isText },
                    empty = strings["progress.empty"],
                    onOpen = { chosen = measure.id },
                )
            }
        }

        // Every reading for what is on screen, folded and counted, because the
        // chart says shape and the list says what was actually written down.
        item(key = "every") {
            Eyebrow(text = strings["progress.every"])
        }

        // Newest first here, unlike the plot. A list of what happened reads
        // most recent first everywhere else in this app, and a chart reads
        // left to right in time. They are two different jobs.
        for (reading in heroReadings.reversed()) {
            item(key = reading.id) {
                ReadingRow(
                    reading = reading,
                    measure = hero,
                    onCorrect = { onCorrectReading(reading) },
                )
            }
        }

        item(key = "add") {
            Spacer(Modifier.height(Space.m))
            Action(
                label = strings["progress.add"],
                onClick = onAddReading,
                modifier = Modifier.fillMaxWidth().testTag(ProgressTags.ADD), emphasis = ActionEmphasis.Main,
            )
        }
    }
}


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
/**
 * The latest reading as a figure and, separately, when it was taken.
 *
 * **`m3v4-0` puts the number at display size with its unit quiet beside it and
 * nothing else on that line.** A date joined onto the figure makes the card's
 * one loud thing a sentence, which is what the Progress page was doing: "131.2
 * lb · May 8, 2026" set as a headline. The date belongs in the footnote, where
 * the drawing puts the month the line starts in.
 */
private data class Latest(val figure: String?, val unit: String?, val date: String)

private fun latestParts(
    strings: Strings,
    measure: Repository.Measure,
    readings: List<Repository.Reading>,
): Latest? {
    val latest = readings.maxByOrNull { it.occurredStart ?: Long.MIN_VALUE } ?: return null
    return Latest(
        figure = readingValue(latest),
        unit = latest.unit?.takeIf { it.isNotBlank() }
            ?: measure.unit?.takeIf { it.isNotBlank() },
        date = EventDateText.render(strings, latest.occurredEdtf),
    )
}

private fun latestOf(
    strings: Strings,
    measure: Repository.Measure,
    readings: List<Repository.Reading>,
    brief: Boolean = false,
): String? {
    val latest = readings.maxByOrNull { it.occurredStart ?: Long.MIN_VALUE } ?: return null
    val date = EventDateText.render(strings, latest.occurredEdtf)
    if (brief && measure.isText) return date
    val value = readingValue(latest) ?: return date
    val unit = latest.unit?.takeIf { it.isNotBlank() } ?: measure.unit?.takeIf { it.isNotBlank() }
    // **Isolated, because this line mixes directions.** A number, a Latin unit
    // and an Arabic month in one string are three bidirectional runs, and
    // without isolation the algorithm reorders them against each other: this
    // read "2026 يونيو to 10 · 26 0 1.4" on the phone.
    return Bidi.join(listOfNotNull(value, unit).joinToString(" "), date)
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

/**
 * One reading, as the person wrote it: their words where they used words, and
 * the number otherwise.
 *
 * **Internal rather than private, and borrowed rather than copied.** Today's
 * tracked card shows the latest reading of the same measure this screen plots,
 * and a second copy of [format] is how the two come to disagree about whether a
 * whole number keeps its trailing zero. This is a rule about numbers somebody
 * gave, not a piece of the old design language, so the v4 screen calls it the
 * way `ui/v4` borrows `initialsOf` and `chartPoints`. It moves with the Progress
 * screen when that screen is rewritten. #386.
 */
internal fun readingValue(reading: Repository.Reading): String? = when {
    reading.text?.isNotBlank() == true -> reading.text
    reading.number != null -> format(reading.number)
    else -> null
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
private fun ReadingRow(
    reading: Repository.Reading,
    measure: Repository.Measure,
    onCorrect: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val value = readingValue(reading)

    // **The row is the door to correcting it.** #374, and it is the shape rule
    // 17 asks for: a date editable forever *from the entry itself* rather than
    // from a menu somewhere else. A reading is typed one handed while holding
    // something else, which is how 138.8 becomes 1388.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .openableByTap(
                label = strings("measurement.correct.open", "value" to (value ?: "")),
                onTap = onCorrect,
                resting = Color.Transparent,
            )
            .testTag(ProgressTags.reading(reading.id))
            .padding(horizontal = Space.sm),
    ) {
        Text(
            text = EventDateText.render(strings, reading.occurredEdtf),
            style = HealthTrail.type.bodyS,
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
                Text(text = Bidi.isolate(unit), style = HealthTrail.type.bodyM, color = colors.ink2)
            }
        }

        reading.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(Space.xs))
            Text(text = Bidi.isolate(note), style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        // Only said when it is not the ordinary case. Labeling every family
        // reading "recorded by you" would be noise on every row.
        if (reading.source == "clinician") {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["progress.by_clinician"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }
}
