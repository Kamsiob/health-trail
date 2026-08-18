package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * A month, as a grid of days, for appointments. #387, D196.
 *
 * **It is drawn only from appointments recorded in this app.** No account, no
 * sync, and **no calendar read permission is ever requested**, per `DESIGN.md`
 * 9.1 and `MASTER_SPEC.md`. A month view is exactly the feature that tempts an
 * app to ask for the person's calendar, and this one does not: what it shows is
 * what they wrote down here, and nothing else.
 *
 * **This component cannot read a calendar even if somebody wanted it to**, per
 * D90, because it takes a list of days and marks and has no way to reach a
 * provider. That is deliberate rather than incidental.
 *
 * **A day with something on it is marked with a dot, not colored.** A colored
 * cell would make a busy week look like a warning, and this app does not grade
 * a person's month, rule 2. The dot says "something is written here" and the
 * day itself says how much when it opens; neither says whether that is a lot.
 * The dot is the section's own [TabHue] base, D198, so the grid is the same
 * color as the list behind the toggle.
 *
 * **The agenda is the default and this is the toggle**, per screen 22, because
 * the question a person actually opens this screen with is "what is next",
 * which a list answers and a grid makes them hunt for.
 *
 * **Written fresh on Material's own, the old file deleted.** The frame was a
 * `Column` with a `clip` and a `background`; every cell was a `Box` with a
 * `clip`, a `background`, an `indication = null` `clickable` and a hand rolled
 * press scale and pressed surface. `Surface` is all of that in one component,
 * and its state layer is the app-wide press treatment rule 16 asks for rather
 * than a second one written here.
 */
@Composable
fun MonthGrid(
    weekdayLabels: List<String>,
    days: List<MonthDay>,
    hue: TabHue,
    onOpenDay: (MonthDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(Space.s),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = HealthTrail.type.mono,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = Space.xs)
                            // The column headers are read once by a reader user
                            // at the top and would otherwise be announced again
                            // inside every one of the thirty-five cells below.
                            .clearAndSetSemantics { },
                    )
                }
            }

            days.chunked(DAYS_IN_WEEK).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day -> DayCell(day, hue, onOpenDay, Modifier.weight(1f)) }
                    // A short final week keeps its columns rather than
                    // centering, so the grid stays a grid and the last row
                    // lines up under the one above it.
                    repeat(DAYS_IN_WEEK - week.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * One day in a month view.
 *
 * @param label the day number as the person's locale writes it, composed by the
 *   caller rather than formatted here, so an Arabic month shows Arabic numerals
 *   without this component knowing anything about locales.
 * @param count how many appointments are recorded on it. **Zero means the day
 *   is bare**, not that it renders a nought: an empty day in a care record is
 *   the normal case and printing zeros across a month would make the ordinary
 *   look like an absence.
 * @param description what a screen reader says for the whole cell, so it
 *   announces "14 March, two appointments" rather than "14" and then a dot.
 * @param inMonth false for the leading and trailing days that fill the first
 *   and last weeks. They are shown rather than blank, because a person orients
 *   by where the month sits in its weeks, and they recede rather than vanish.
 */
data class MonthDay(
    val label: String,
    val count: Int,
    val description: String,
    val inMonth: Boolean = true,
    val isToday: Boolean = false,
)

@Composable
private fun DayCell(
    day: MonthDay,
    hue: TabHue,
    onOpenDay: (MonthDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    // **Today is a wash, and it never outweighs the marks.** The app's own
    // sense of where it is should not compete with the person's record, so it
    // takes the quiet accent container and the dots keep the section's hue.
    val resting = if (day.isToday) scheme.primaryContainer else Color.Transparent
    val ink = when {
        !day.inMonth -> scheme.onSurfaceVariant.copy(alpha = OUT_OF_MONTH_ALPHA)
        day.isToday -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }

    val face: @Composable () -> Unit = {
        Box(
            modifier = Modifier.aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    // bidi-ok: the app formats this itself, so it is never
                    // somebody's own words.
                    text = day.label,
                    style = HealthTrail.type.mono,
                    color = ink,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                // One dot for anything, rather than one dot per appointment. A
                // person scanning a month wants to know which days have
                // something on them; how many is what the day itself says when
                // they open it.
                Box(
                    modifier = Modifier
                        .padding(top = MARK_GAP)
                        .size(MARK_SIZE),
                ) {
                    if (day.count > 0) {
                        Surface(
                            modifier = Modifier.size(MARK_SIZE),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = hue.base,
                        ) {}
                    }
                }
            }
        }
    }

    // **A day with nothing on it is not a control**, so it gets no press
    // treatment and no click. Springing an empty square would promise something
    // behind it, and there is nothing behind it.
    if (day.count > 0) {
        Surface(
            onClick = { onOpenDay(day) },
            // **One node carries the tap and the sentence.** A reader hears
            // "14 March, two appointments, button" rather than "14" and then a
            // dot it cannot name. `docs/TRAPS.md`.
            modifier = modifier.semantics(mergeDescendants = true) {
                contentDescription = day.description
            },
            shape = MaterialTheme.shapes.small,
            color = resting,
            content = face,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = resting,
            content = face,
        )
    }
}

private const val DAYS_IN_WEEK = 7
private const val OUT_OF_MONTH_ALPHA = 0.45f

/** The dot that says a day has something written on it, and the air above it. */
private val MARK_SIZE = 4.dp
private val MARK_GAP = 2.dp

/** The hairline of canvas between one week and the next. */
private val ROW_GAP = 2.dp
