package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * A month, as a grid of days, for appointments. `DESIGN.md` section 7, drawn as
 * grid screen 22 behind the view toggle.
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
 * a person's month. The dot says "something is written here" and the count says
 * how much, and neither says whether that is a lot.
 *
 * **The agenda is the default and this is the toggle**, per screen 22, because
 * the question a person actually opens this screen with is "what is next",
 * which a list answers and a grid makes them hunt for.
 */
@Composable
fun MonthGrid(
    weekdayLabels: List<String>,
    days: List<MonthDay>,
    hue: TabHue,
    onOpenDay: (MonthDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .padding(Space.s),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = type.mono,
                    color = colors.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = Space.xs)
                        // The column headers are read once by a reader user at
                        // the top and would otherwise be announced again inside
                        // every one of the thirty-five cells below them.
                        .clearAndSetSemantics { },
                )
            }
        }

        days.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day -> DayCell(day, hue, onOpenDay, Modifier.weight(1f)) }
                // A short final week keeps its columns rather than centering,
                // so the grid stays a grid and the last row lines up under the
                // one above it.
                repeat(DAYS_IN_WEEK - week.size) { Box(Modifier.weight(1f)) }
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
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(Radius.tile)
            // **Today is a ring, not a fill**, so it does not compete with the
            // marks that say something is written down. The app's own sense of
            // where it is should never outweigh the person's record.
            .background(if (day.isToday) colors.blueWash else Color.Transparent)
            .clickable(
                enabled = day.count > 0,
                role = Role.Button,
                onClickLabel = day.description,
                onClick = { onOpenDay(day) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                // bidi-ok: the app formats this itself, so it is never somebody's own words.
                text = day.label,
                style = type.mono,
                color = when {
                    !day.inMonth -> colors.ink2.copy(alpha = OUT_OF_MONTH_ALPHA)
                    day.isToday -> colors.blueDeep
                    else -> colors.ink
                },
                modifier = Modifier.clearAndSetSemantics { },
            )
            // One dot for anything, rather than one dot per appointment. A
            // person scanning a month wants to know which days have something
            // on them; how many is what the day itself says when they open it.
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(MarkSize)
                    .clip(CircleShape)
                    .background(if (day.count > 0) hue.base else Color.Transparent),
            )
        }
    }
}

private const val DAYS_IN_WEEK = 7
private const val OUT_OF_MONTH_ALPHA = 0.45f
private val MarkSize = 4.dp
