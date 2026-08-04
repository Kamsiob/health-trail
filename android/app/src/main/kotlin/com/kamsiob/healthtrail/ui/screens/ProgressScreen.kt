package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ProgressTags {
    const val NAME = "progress"
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
 * arrow, no trend, and no chart line.** Each reading is shown as it was
 * recorded, next to the date it was given, in the order it happened. Anyone who
 * wants to know what the numbers mean has to ask somebody qualified, which is
 * the correct outcome and the one the app must not quietly substitute for.
 *
 * **Gaps are gaps.** Readings are listed, not interpolated. A month with
 * nothing in it shows nothing rather than a line drawn across it, which is the
 * one thing `MASTER_SPEC.md` section 4.3 names explicitly.
 *
 * **Who said it is kept.** A value the family measured and a value a clinician
 * stated are different things and the record must not blur them, so a reading
 * from a clinician says so and one from the family carries no label at all,
 * because that is the ordinary case and labeling it would be noise.
 *
 * The chart, the medication start markers, and the milestone arc from section
 * 4.3 are not built. This is the readable record underneath them, and it is
 * complete on its own terms rather than a placeholder for a chart.
 */
@Composable
fun ProgressScreen(
    measures: List<Repository.Measure>,
    readings: List<Repository.Reading>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val byMeasure = readings.groupBy { it.measureId }

    SectionScaffold(
        name = ProgressTags.NAME,
        title = strings["notebook.section.progress"],
        subtitle = strings["progress.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.PROGRESS,
        headingKey = "progress.heading",
    ) {
        if (measures.isEmpty()) {
            item { SectionEmpty(name = ProgressTags.NAME, text = strings["progress.empty"], section = Repository.Section.PROGRESS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION)) }
        }

        for (measure in measures) {
            val forMeasure = byMeasure[measure.id].orEmpty()

            item(key = "measure_${measure.id}") {
                Spacer(Modifier.height(Space.s))
                GroupHeaderText(
                    label = measure.name,
                    modifier = Modifier.testTag(ProgressTags.measure(measure.id)),
                )
                Spacer(Modifier.height(Space.headerGap))

                // A measure exists as soon as somebody records against it, so
                // this is rare rather than impossible. It reads as "nothing
                // recorded" rather than as an error, per rule 13.
                if (forMeasure.isEmpty()) {
                    Text(
                        text = strings("progress.readings", "count" to 0),
                        style = HealthTrail.type.bodyM,
                        color = HealthTrail.colors.ink2,
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            for (reading in forMeasure) {
                item(key = reading.id) {
                    ReadingRow(reading = reading, measure = measure)
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }
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

    // Formatted with no rounding, no padding, and no thousands grouping beyond
    // what the value has: a recorded number is shown as the person gave it.
    // A whole number loses its trailing zero because "128.0" is a claim of
    // precision nobody made.
    val value = when {
        reading.text?.isNotBlank() == true -> reading.text
        reading.number != null -> {
            val number = reading.number
            if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
        }
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .testTag(ProgressTags.reading(reading.id))
            .padding(Space.cardPadding),
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
