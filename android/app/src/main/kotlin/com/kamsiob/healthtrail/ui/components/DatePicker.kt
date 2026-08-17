package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object DatePickerTags {
    const val SHEET = "date_picker_sheet"
    const val GRID = "date_picker_grid"
    const val CONFIRM = "date_picker_confirm"
    const val CLEAR = "date_picker_clear"
    const val TIME = "date_picker_time"
    fun day(day: Int) = "date_picker_day_$day"
    fun month(month: Int) = "date_picker_month_$month"

    /** The heading, which is the way up from days to months to years. #132. */
    const val ZOOM = "date_picker_zoom"
    const val MONTHS = "date_picker_months"
    const val YEARS = "date_picker_years"
    fun monthCell(month: Int) = "date_picker_month_cell_$month"
    fun yearCell(year: Int) = "date_picker_year_cell_$year"
}

/**
 * Choosing a date, per `DESIGN.md` section 5.16.
 *
 * **One sheet with three levels of precision, not three controls.** A day, a
 * month, or a year, each optional, each mapping to exactly one EDTF form. The
 * person picks how much they know and the app records that and no more.
 *
 * Putting them in one place is what makes "I only know the month" as easy as
 * "I know the day". **The coarse answer must not feel like the failure case**,
 * because for a record written from memory it is usually the true one.
 *
 * **Nothing is preselected and it opens on whatever the entry already says.**
 * A picker that preselects today turns every mistap into a claim, in a record
 * somebody may rely on years later.
 *
 * It never shows EDTF, a precision name, or a format. What it shows is a
 * calendar, twelve month names, and a year.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: Edtf.Date?,
    onPick: (Edtf.Date) -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
    /**
     * The heading, which defaults to asking when something happened.
     *
     * **Overridable because the same picker asks about the future.** An
     * appointment is usually ahead, and "When was this?" over a date somebody
     * is scheduling reads as the app not knowing what it is asking about. The
     * default is the past tense because five of the six things that open this
     * are records of something that already happened.
     */
    titleKey: String = "date.pick.title",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()

    val opening = remember(initial) { Opening.of(initial, today) }
    var month by remember { mutableStateOf(opening.month) }
    var day by remember { mutableStateOf(opening.day) }
    var wholeMonth by remember { mutableStateOf(opening.wholeMonth) }
    var time by remember { mutableStateOf(opening.time) }

    /**
     * How far out the picker is zoomed. #132.
     *
     * **Days is where it opens and where it returns to**, because the common
     * answer is this month or last and that case must stay exactly as fast as
     * it was. Picking a month drops back to days, and picking a year drops back
     * to months, so a person walking back four years lands on a day grid
     * without a third tap to get there.
     */
    var zoom by remember { mutableStateOf(Zoom.DAYS) }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(DatePickerTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings[titleKey],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                // Says the coarse answer is a real one, before the person has
                // to decide whether it is.
                text = strings["date.pick.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            // **The heading is the way out of a swipe count.** It used to be a
            // label between two month arrows, so moving back a year was twelve
            // taps and moving back to a birth year was hundreds. Tapping it
            // zooms out; the arrows still step by whatever is on screen. #132.
            PickerHeader(
                zoom = zoom,
                month = month,
                onPrevious = {
                    month = when (zoom) {
                        Zoom.DAYS -> month.minusMonths(1)
                        Zoom.MONTHS -> month.minusYears(1)
                        Zoom.YEARS -> month.minusYears(YEARS_PER_PAGE.toLong())
                    }
                },
                onNext = {
                    month = when (zoom) {
                        Zoom.DAYS -> month.plusMonths(1)
                        Zoom.MONTHS -> month.plusYears(1)
                        Zoom.YEARS -> month.plusYears(YEARS_PER_PAGE.toLong())
                    }
                },
                onZoomOut = {
                    zoom = when (zoom) {
                        Zoom.DAYS -> Zoom.MONTHS
                        Zoom.MONTHS -> Zoom.YEARS
                        Zoom.YEARS -> Zoom.YEARS
                    }
                },
            )

            Spacer(Modifier.height(Space.sm))

            when (zoom) {
                Zoom.DAYS -> MonthGrid(
                    month = month,
                    selected = if (wholeMonth) null else day,
                    today = today,
                    dimmed = wholeMonth,
                    onPick = { picked ->
                        day = picked
                        wholeMonth = false
                    },
                )

                // **Picking a month drops straight back to days.** The person
                // came here to get somewhere, not to look at months.
                Zoom.MONTHS -> MonthsGrid(
                    year = month.year,
                    selected = month.monthValue,
                    today = today,
                    onPick = { picked ->
                        month = YearMonth.of(month.year, picked)
                        zoom = Zoom.DAYS
                    },
                )

                // **And a year drops back to months rather than to days**, so
                // the next tap is the one they were already going to make.
                Zoom.YEARS -> YearsGrid(
                    around = month.year,
                    selected = month.year,
                    today = today,
                    onPick = { picked ->
                        month = YearMonth.of(picked, month.monthValue)
                        zoom = Zoom.MONTHS
                    },
                )
            }

            // **Both of these belong to a chosen day and are hidden while the
            // picker is zoomed out.** A chip offering "the whole of November"
            // under a grid of years is an answer to a question nobody is
            // looking at, and it would change the month out from under the
            // grid on screen. #132.
            if (zoom == Zoom.DAYS) {
                Spacer(Modifier.height(Space.m))

                // The whole month, as an answer in its own right rather than as
                // a fallback. Selecting it clears the day, because "November"
                // and "the eighteenth" are different claims and the app may
                // hold only the one the person made.
                ChoiceChip(
                    label = strings("date.month", "date" to monthName(month)),
                    selected = wholeMonth,
                    onClick = {
                        wholeMonth = !wholeMonth
                        if (wholeMonth) day = null
                    },
                    modifier = Modifier.testTag(DatePickerTags.month(month.monthValue)),
                )

                if (day != null && !wholeMonth) {
                    Spacer(Modifier.height(Space.m))
                    TimeRow(
                        time = time,
                        onToggle = { time = if (time == null) DEFAULT_TIME else null },
                        onChange = { time = it },
                    )
                }
            }

            Spacer(Modifier.height(Space.l))

            Action(
                label = strings["date.pick.confirm"],
                onClick = {
                    onPick(
                        answer(
                            month = month,
                            day = day,
                            wholeMonth = wholeMonth,
                            time = time,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(DatePickerTags.CONFIRM), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.s))

            // **Every level has a way back to less precision.** Someone who
            // taps a day and then realizes they are not sure has to be able to
            // say so without leaving and starting again.
            Action(
                label = strings["date.pick.clear"],
                onClick = { onPick(Edtf.unknown()) },
                modifier = Modifier.fillMaxWidth().testTag(DatePickerTags.CLEAR),
            )
        }
    }
}

/**
 * What the sheet opens on.
 *
 * **Opening on the entry's own date, at its own precision, is what makes
 * confirming without touching anything a no-op.** An editor that resets to
 * today is an editor that quietly rewrites what it was opened to change.
 */
private data class Opening(
    val month: YearMonth,
    val day: Int?,
    val wholeMonth: Boolean,
    val time: LocalDateTime?,
) {
    companion object {
        fun of(date: Edtf.Date?, today: LocalDate): Opening {
            val body = date?.canonical?.let {
                if (date.qualifier == Edtf.Qualifier.NONE) it else it.dropLast(1)
            }
            return when (date?.precision) {
                Edtf.Precision.MOMENT -> {
                    val at = LocalDateTime.parse(body)
                    Opening(YearMonth.from(at), at.dayOfMonth, false, at)
                }
                Edtf.Precision.DAY -> {
                    val on = LocalDate.parse(body)
                    Opening(YearMonth.from(on), on.dayOfMonth, false, null)
                }
                Edtf.Precision.MONTH -> Opening(
                    YearMonth.of(body!!.take(4).toInt(), body.drop(5).toInt()),
                    null,
                    true,
                    null,
                )
                // Anything coarser, unreadable, or absent opens on this month
                // with nothing chosen, which claims nothing.
                else -> Opening(YearMonth.from(today), null, false, null)
            }
        }
    }
}

/** The answer, at exactly the precision the person expressed and no finer. */
private fun answer(
    month: YearMonth,
    day: Int?,
    wholeMonth: Boolean,
    time: LocalDateTime?,
): Edtf.Date = when {
    wholeMonth -> Edtf.month(month.year, month.monthValue)
    day == null -> Edtf.unknown()
    time != null -> Edtf.moment(
        LocalDateTime.of(month.year, month.monthValue, day, time.hour, time.minute)
    )
    else -> Edtf.day(LocalDate.of(month.year, month.monthValue, day))
}

/**
 * How far out the picker is zoomed. #132.
 *
 * **Three steps and no more.** Days, months, years. A fourth for decades would
 * be a level nobody asks for by name, and the year grid already holds enough
 * years that stepping it by a page covers a lifetime in a few taps.
 */
private enum class Zoom { DAYS, MONTHS, YEARS }

/** How many years one page of the year grid holds, which is four rows of four. */
private const val YEARS_PER_PAGE = 16

/**
 * The oldest year the picker will offer.
 *
 * **A hundred and twenty years back, because this app records a birth date.**
 * The subject of a care notebook is frequently in their nineties, and a picker
 * that cannot reach the year somebody was born is a picker that cannot answer
 * the question the setup screen asks. Bounded rather than infinite, per the
 * acceptance criteria, and bounded by the person rather than by a round number.
 */
private const val YEARS_BACK = 120

/**
 * And the newest. Appointments are scheduled ahead, rarely by more than a year.
 */
private const val YEARS_FORWARD = 5

/**
 * The heading, which is both a label and the way out of a swipe count.
 *
 * **It used to be a label between two arrows.** Moving back a year cost twelve
 * taps and reaching a birth year cost hundreds, on a screen the app actively
 * encourages people to come back to, because rule 17 makes every date editable
 * forever and rule 13 makes a partial answer a finished one. `DESIGN.md` 10.10:
 * taps are the currency. #132.
 *
 * **The arrows step whatever is on screen**, so they keep working the way they
 * read: a month at a time on the day grid, a year at a time on the month grid,
 * a page at a time on the year grid.
 *
 * **The year view's heading does not zoom further**, because there is nothing
 * above it. It stays a label there rather than becoming a button that does
 * nothing, which is rule 11.
 */
@Composable
private fun PickerHeader(
    zoom: Zoom,
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onZoomOut: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    val label = when (zoom) {
        Zoom.DAYS -> monthName(month)
        Zoom.MONTHS -> month.year.toString()
        Zoom.YEARS -> {
            val page = yearsPage(month.year)
            strings(
                "date.pick.years.range",
                "from" to page.first().toString(),
                "to" to page.last().toString(),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // **The arrows say what they actually do at this zoom.** They stepped
        // by a month, a year or sixteen years while announcing "previous
        // month" in all three, which is a control naming an action it does not
        // perform. Rule 11, and the same shape as #231.
        Stepper(
            label = strings[
                when (zoom) {
                    Zoom.DAYS -> "date.pick.previous"
                    Zoom.MONTHS -> "date.pick.previous.year"
                    Zoom.YEARS -> "date.pick.previous.years"
                }
            ],
            pointsForward = false,
            onClick = onPrevious,
        )

        if (zoom == Zoom.YEARS) {
            Text(
                text = label,
                style = HealthTrail.type.displayS,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        } else {
            val interaction = remember { MutableInteractionSource() }
            val surface by pressedSurface(interaction, Color.Transparent)
            val ring by focusRingAlpha(interaction)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radius.tile)
                    .background(surface)
                    .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.tile)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onZoomOut,
                    )
                    .semantics {
                        contentDescription = strings(
                            if (zoom == Zoom.DAYS) {
                                "date.pick.zoom.months"
                            } else {
                                "date.pick.zoom.years"
                            },
                            "date" to label,
                        )
                    }
                    .testTag(DatePickerTags.ZOOM),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = Space.xs),
                )
            }
        }

        Stepper(
            label = strings[
                when (zoom) {
                    Zoom.DAYS -> "date.pick.next"
                    Zoom.MONTHS -> "date.pick.next.year"
                    Zoom.YEARS -> "date.pick.next.years"
                }
            ],
            pointsForward = true,
            onClick = onNext,
        )
    }
}

/** The sixteen years one page of the year grid shows, containing [year]. */
private fun yearsPage(year: Int): List<Int> {
    val newest = LocalDate.now().year + YEARS_FORWARD
    val oldest = LocalDate.now().year - YEARS_BACK
    // Pages are anchored to the newest year so the current decade never lands
    // split across two pages, which is where almost every answer is.
    val offset = ((newest - year) / YEARS_PER_PAGE) * YEARS_PER_PAGE
    val top = newest - offset
    return (top downTo maxOf(oldest, top - YEARS_PER_PAGE + 1)).toList().sorted()
}

/**
 * The twelve months of one year, as a grid.
 *
 * **Named in full rather than abbreviated.** Three letter month names are a
 * different word in every language and a worse word in most, and there is room
 * for the real one at three across.
 */
@Composable
private fun MonthsGrid(
    year: Int,
    selected: Int,
    today: LocalDate,
    onPick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(DatePickerTags.MONTHS),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        (1..12).chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                row.forEach { number ->
                    CalendarCell(
                        label = monthLabel(year, number),
                        selected = number == selected,
                        isNow = year == today.year && number == today.monthValue,
                        onClick = { onPick(number) },
                        modifier = Modifier.weight(1f).testTag(DatePickerTags.monthCell(number)),
                    )
                }
            }
        }
    }
}

/** One page of years, four across. */
@Composable
private fun YearsGrid(
    around: Int,
    selected: Int,
    today: LocalDate,
    onPick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(DatePickerTags.YEARS),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        yearsPage(around).chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                row.forEach { year ->
                    CalendarCell(
                        label = year.toString(),
                        selected = year == selected,
                        isNow = year == today.year,
                        onClick = { onPick(year) },
                        modifier = Modifier.weight(1f).testTag(DatePickerTags.yearCell(year)),
                    )
                }
                // A short last row keeps its cells the same width as the rows
                // above rather than stretching to fill, which would make the
                // oldest years look like a different kind of control.
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * One month or one year, as a cell.
 *
 * **The same treatment `DayCell` uses**, deliberately: selected is a filled
 * wash with a ring, today carries the same quiet dot, and the press step and
 * focus ring come from the same helpers. Two calendars in one sheet that looked
 * like two different controls would be the defect section 11 exists to prevent.
 */
@Composable
private fun CalendarCell(
    label: String,
    selected: Boolean,
    isNow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val resting = if (selected) colors.blueWash else Color.Transparent
    val surface by pressedSurface(interaction, resting)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(
                2.dp,
                colors.blue.copy(alpha = maxOf(if (selected) 1f else 0f, ring)),
                Radius.tile,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = HealthTrail.type.bodyM,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) colors.blueDeep else colors.ink,
                textAlign = TextAlign.Center,
            )
            if (isNow) {
                Spacer(Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.ink3),
                )
            }
        }
    }
}

/**
 * A month step.
 *
 * Carries a content description rather than a visible label, and it is one of
 * the few controls in the app that does, because a chevron beside a month name
 * says what it does to everyone who can see it and nothing to anyone who
 * cannot. `ScreenReaderTest` is what keeps that description present.
 */
@Composable
private fun Stepper(label: String, pointsForward: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, Color.Transparent)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(Space.touchTarget)
            .clip(CircleShape)
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Chevron(pointsForward = pointsForward)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: Int?,
    today: LocalDate,
    dimmed: Boolean,
    onPick: (Int) -> Unit,
) {
    val colors = HealthTrail.colors
    val first = month.atDay(1)
    // Monday first, which is what `Edtf.week` already assumes, so the two
    // cannot disagree about which week a day belongs to.
    val leading = (first.dayOfWeek.value - 1)
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth().testTag(DatePickerTags.GRID)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayInitials().forEach { initial ->
                Text(
                    text = initial,
                    style = HealthTrail.type.mono,
                    color = colors.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Space.xs))

        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    val dayOfMonth = index - leading + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        // **A day outside this month is absent, not grayed.** A
                        // grayed cell that cannot be tapped is a control that
                        // does nothing, which D42 removed elsewhere for the
                        // same reason.
                        if (dayOfMonth in 1..month.lengthOfMonth()) {
                            DayCell(
                                day = dayOfMonth,
                                selected = selected == dayOfMonth,
                                isToday = month.atDay(dayOfMonth) == today,
                                dimmed = dimmed,
                                onClick = { onPick(dayOfMonth) },
                            )
                        } else {
                            Spacer(Modifier.size(Space.touchTarget))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One day.
 *
 * Selected uses the choice chip treatment from 5.11 in a round shape rather
 * than a second selection language: `blue_soft`, a 2dp `blue` ring, and the
 * label at weight 700, so selection survives a grayscale screenshot and does
 * not rely on color.
 *
 * **Today carries a dot and is never preselected.** Marking where today is
 * helps someone orient. Choosing it for them would be the app making a claim.
 */
@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    isToday: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val resting = if (selected) colors.blueWash else Color.Transparent
    val surface by pressedSurface(interaction, resting)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(surface)
            .border(
                2.dp,
                colors.blue.copy(alpha = maxOf(if (selected) 1f else 0f, ring)),
                CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(DatePickerTags.day(day)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = HealthTrail.type.bodyM,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    dimmed -> colors.ink2
                    selected -> colors.blueDeep
                    else -> colors.ink
                },
            )
            if (isToday) {
                Spacer(Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.ink3),
                )
            }
        }
    }
}

/**
 * The time, which is a second act rather than part of choosing a day.
 *
 * **A day with no time is a day.** This is the one place the model's precision
 * is visible as a choice, and it is visible as "do you know the time" rather
 * than as a precision selector, per 10.9.
 */
@Composable
private fun TimeRow(
    time: LocalDateTime?,
    onToggle: () -> Unit,
    onChange: (LocalDateTime) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = Modifier.fillMaxWidth().testTag(DatePickerTags.TIME)) {
        Text(
            text = strings["date.pick.time"],
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
        Spacer(Modifier.height(Space.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChoiceChip(
                label = strings[
                    if (time == null) "date.pick.time.add" else "date.pick.time.clear"
                ],
                selected = time != null,
                onClick = onToggle,
            )
            if (time != null) {
                Spacer(Modifier.width(Space.s))
                Stepper(
                    label = strings["date.pick.year.down"],
                    pointsForward = false,
                    onClick = { onChange(time.minusMinutes(STEP_MINUTES)) },
                )
                Text(
                    text = "%02d:%02d".format(time.hour, time.minute),
                    style = HealthTrail.type.mono,
                    color = colors.ink,
                )
                Stepper(
                    label = strings["date.pick.year.up"],
                    pointsForward = true,
                    onClick = { onChange(time.plusMinutes(STEP_MINUTES)) },
                )
            }
        }
    }
}

/**
 * One month's name on its own, without its year.
 *
 * **Outside the composable deliberately.** Lint's `NonObservableLocale` refuses
 * a locale read inside one, and it is right: a composable that reads the
 * default locale does not recompose when the locale changes. `monthName` has
 * always been a plain function for the same reason and this sits beside it.
 */
private fun monthLabel(year: Int, month: Int): String =
    YearMonth.of(year, month).month
        .getDisplayName(JavaTextStyle.FULL_STANDALONE, java.util.Locale.getDefault())

private fun monthName(month: YearMonth): String =
    "${month.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, java.util.Locale.getDefault())} " +
        month.year

private fun weekdayInitials(): List<String> =
    (1..7).map {
        java.time.DayOfWeek.of(it)
            .getDisplayName(JavaTextStyle.NARROW, java.util.Locale.getDefault())
    }

/** Two in the afternoon, which is when most of what this app records happens. */
private val DEFAULT_TIME: LocalDateTime = LocalDateTime.of(2000, 1, 1, 14, 0)

private const val STEP_MINUTES = 15L
