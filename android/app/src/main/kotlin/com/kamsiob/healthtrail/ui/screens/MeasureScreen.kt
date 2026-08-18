package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.Symbols
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueForMeasure
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.StatBlock

object MeasureTags {
    const val NAME = "measure"
    const val PLOT = "measure_plot"
    const val READINGS = "measure_readings"
    const val ADD = "measure_add"
    const val EMPTY = "section_empty_measure"
}

/**
 * One tracked thing, on its own screen. #398.
 *
 * **The owner, 2026-08-18: "we need to make sure that all of the different
 * things that can be tracked for progress have corresponding screens. we've
 * only done it with weight so far from what I can tell."** He was right and it
 * was not a fixture artifact: the Progress page gave a chart to whichever
 * measure was the hero and every other measure a number with no way into its
 * own history. A second measure was a card with a figure in it and a dead end.
 *
 * **Read `docs/TRACKED-THINGS.md` before changing anything here.** It is the
 * research, one entry per preset, and it is mostly about what rule 2 rules out.
 * Its section 7 is the whole system in a paragraph: five shapes cover sixteen
 * things, every screen leads with what the person last wrote down in that
 * measure's own hue, D204, and everything under it is the record in date order.
 *
 * **Rule 2 is the entire risk on this screen and it is easiest to break here.**
 * No range, no threshold, no color that changes with a value, no arrow, no
 * trend, no judgment, no interpretation. The commercial category is built on
 * exactly those: blood pressure apps sell hypertension staging, growth apps
 * sell centile curves, and both are conclusions about somebody's health that
 * this app does not draw. `docs/V4.md` 6.1 item 11 says a screen that could
 * belong to any app has failed; here the stronger claim holds, that a screen
 * that looks like those apps has broken rule 2.
 *
 * **The only numbers here that the person did not write down are counts**, and
 * a count is arithmetic rather than a conclusion.
 *
 * **A measure written in words is still a measure**, and it has no plot. Its
 * screen is its readings, and the card says there is nothing to plot rather
 * than drawing an empty frame.
 *
 * **Rule 17 on every date.** A reading given as "sometime in April" is shown at
 * exactly that precision, here and in the plot, through `EventDateText`.
 *
 * `MeasurementScreen` is the form that writes one reading and is a different
 * thing; this was not bolted onto it, per #398.
 */
@Composable
fun MeasureScreen(
    measure: Repository.Measure,
    /** Every reading for this measure and no other. */
    readings: List<Repository.Reading>,
    onAddReading: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the form that corrects one reading, from the reading itself. #374, rule 17. */
    onCorrectReading: (Repository.Reading) -> Unit = {},
    /** Opens the form that corrects this measure's own name. #374. */
    onCorrectMeasure: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val hue = hueForMeasure(measure.id)

    // **Oldest first for the plot**, because a chart reads left to right in
    // time whichever way the layout runs. The list below reads newest first,
    // which is what every other record in this app does. Two different jobs.
    val inTime = readings.sortedBy { it.occurredStart }
    val latest = latestParts(strings, measure, inTime)

    Page(
        title = Bidi.isolate(measure.name),
        onBack = onBack,
        backLabel = strings["section.back.progress"],
        modifier = modifier.testTag(SectionTags.root(MeasureTags.NAME)),
        eyebrow = strings["notebook.section.progress"],
        section = Repository.Section.PROGRESS,
        // **The corner, where D173 puts a correction on every screen.**
        onEdit = onCorrectMeasure,
        editLabel = strings[
            if (measure.isText) "progress.correct.name" else "progress.correct.measure",
        ],
        editTag = ProgressTags.CORRECT_MEASURE,
        // **The way in floats**, D200, and it is null while there is nothing
        // here, because the empty block carries the verb itself.
        fab = if (readings.isEmpty()) {
            null
        } else {
            {
                ExtendedFloatingActionButton(
                    onClick = onAddReading,
                    icon = {
                        Icon(painter = painterResource(Symbols.add), contentDescription = null)
                    },
                    text = { Text(text = strings["progress.add"]) },
                    // The sentence sits on the button's own node, `docs/TRAPS.md`.
                    modifier = Modifier
                        .testTag(MeasureTags.ADD)
                        .semantics { contentDescription = strings["progress.add"] },
                )
            }
        },
    ) {
        if (readings.isEmpty()) {
            item {
                // **A measure with nothing in it is a finished screen, not a
                // broken one**, rule 13. It says plainly that nothing is
                // written down yet and never frames that as something the
                // person failed to do. `SectionEmpty` is the one designed
                // empty state in this app and this screen does not invent a
                // second, rule 16.
                SectionEmpty(
                    name = MeasureTags.NAME,
                    lead = strings["measure.empty.lead"],
                    text = strings["measure.empty"],
                    section = Repository.Section.PROGRESS,
                    actionLabel = strings["progress.add"],
                    onAction = onAddReading,
                )
            }
            return@Page
        }

        item(key = "plot") {
            // **The card this measure already had, at the size it deserves.**
            // It is the same `StatBlock` the Progress page draws, so a person
            // who has learned one tracked thing can read the next: the name as
            // its eyebrow, the latest reading at display size with its unit
            // quiet beside it, the count as a chip, and the line under it with
            // the silences drawn as gaps rather than the line stopping dead.
            //
            // **The footnote is a date and a spread, and neither is a
            // judgment.** The spread is the lowest and highest the person
            // themselves wrote down; it is not a reference range, it names no
            // normal value, and nothing on the card changes color because of
            // it.
            StatBlock(
                // **The page's heading already says it.** Null here, so the
                // card's eyebrow does not repeat the title an inch below it.
                name = null,
                value = latest?.figure,
                description = strings(
                    "progress.chart.description",
                    "name" to measure.name,
                    "count" to inTime.size,
                    "first" to EventDateText.render(strings, inTime.firstOrNull()?.occurredEdtf),
                    "last" to EventDateText.render(strings, inTime.lastOrNull()?.occurredEdtf),
                ),
                hue = hue,
                modifier = Modifier.testTag(MeasureTags.PLOT),
                unit = if (measure.isText) null else latest?.unit,
                count = strings("progress.readings", "count" to inTime.size),
                // **Words have no line**, and the card says so rather than
                // drawing an empty frame.
                readings = if (measure.isText) emptyList() else inTime,
                footnote = if (measure.isText) {
                    Bidi.join(latest?.date, strings["progress.nochart"])
                        .takeIf { it.isNotBlank() }
                } else {
                    listOfNotNull(
                        latest?.date,
                        spread(measure, inTime)?.let {
                            strings("progress.range", "low" to it.first, "high" to it.second)
                        },
                    ).let { Bidi.join(it) }.takeIf { it.isNotBlank() }
                },
                empty = strings["progress.empty"],
            )
            Spacer(Modifier.height(Space.betweenZones))
        }

        item(key = "readings-label") {
            Eyebrow(
                text = strings["measure.readings"],
                modifier = Modifier.testTag(MeasureTags.READINGS),
            )
            Spacer(Modifier.height(Space.withinGroup))
        }

        // **Newest first**, which is how a record of what happened reads
        // everywhere else in this app. Every one of them opens the form that
        // corrects it, because rule 17 wants a date editable forever from the
        // entry itself and because a reading is typed one handed while holding
        // something else, which is how 138.8 becomes 1388.
        for (reading in inTime.reversed()) {
            item(key = reading.id) {
                ReadingRow(
                    reading = reading,
                    measure = measure,
                    onCorrect = { onCorrectReading(reading) },
                )
            }
        }
    }
}
