package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.ui.components.MonthDay
import com.kamsiob.healthtrail.ui.components.MonthGrid
import com.kamsiob.healthtrail.ui.components.ViewOption
import com.kamsiob.healthtrail.ui.components.ViewToggle
import com.kamsiob.healthtrail.ui.components.rememberViewChoice
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import com.kamsiob.healthtrail.ui.components.FoldRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Block

object ApptTags {
    const val NAME = "appointments"
    const val ADD = "appointments_add"
    const val PAST_FOLD = "appointments_past_fold"
    const val TOGGLE = "appointments_view"
    const val MONTH = "appointments_month"
    const val MONTH_BACK = "appointments_month_back"
    const val MONTH_NEXT = "appointments_month_next"
    fun row(id: String) = "appointment_$id"
}

/** The list, which is what somebody opens this screen for. Grid screen 22. */
const val VIEW_AGENDA = "agenda"

/** The same appointments laid out on their month, which is the other question. */
const val VIEW_MONTH = "month"

/**
 * Appointments: what is coming up and what already happened.
 *
 * **The subtitle says nothing here reminds you**, for the same reason the
 * medications screen says the app does not alert. A screen called Appointments
 * is exactly where somebody would assume a reminder lives, and correcting that
 * on arrival is honest where correcting it after they missed something is not.
 *
 * **Upcoming and past are decided by the date, never by a flag.** The schema
 * keeps whether somebody attended as a separate question, and conflating the
 * two would leave an appointment nobody attended sitting in "coming up"
 * forever.
 *
 * **An appointment with no date lands in "coming up"**, which is deliberate:
 * "they said sometime next week" is a real thing somebody is told, and the
 * useful place for it is with the things still ahead rather than filed away
 * with what is finished.
 *
 * **Tapping one opens its prep sheet**, which is `PrepScreen` and which is also
 * the appointment's own detail screen. `MASTER_SPEC.md` 4.3: the questions
 * waiting for that person and a change summary composed from real entries. This
 * is the list underneath it.
 */
@Composable
fun AppointmentsScreen(
    appointments: List<Repository.Appointment>,
    todayMillis: Long,
    /**
     * Opens the appointment's prep sheet.
     *
     * **A tap opens the thing itself**, which is the rule the trail row and the
     * care team row both learned on 2026-08-03. Correcting the details is on
     * the prep sheet, where the rest of the appointment is.
     */
    onOpen: (Repository.Appointment) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    // **Agenda by default and the month on the toggle**, per grid screen 22.
    // `MonthGrid` was built for this and nothing composed it for eight days,
    // which is the state nobody reviews. #357.
    val view = rememberViewChoice(section = ApptTags.NAME, fallback = VIEW_AGENDA)
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(todayMillis, zone) {
        Instant.ofEpochMilli(todayMillis).atZone(zone).toLocalDate()
    }
    // Saved as a count of months so the state survives a rotation, which a
    // `YearMonth` cannot do on its own.
    var monthOffset by rememberSaveable { mutableStateOf(0) }
    val month = remember(today, monthOffset) { YearMonth.from(today).plusMonths(monthOffset.toLong()) }

    val upcoming = appointments.filter {
        it.scheduledStart == null || it.scheduledStart >= todayMillis
    }
    val past = appointments.filter {
        it.scheduledStart != null && it.scheduledStart < todayMillis
    }

    var pastOpen by rememberSaveable { mutableStateOf(false) }

    SectionScaffold(
        name = ApptTags.NAME,
        title = strings["notebook.section.appointments"],
        subtitle = strings["appts.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.APPOINTMENTS,
        headingKey = "appointments.heading",
    ) {
        if (appointments.isEmpty()) {
            item {
                SectionEmpty(name = ApptTags.NAME, text = strings["appts.empty"], section = Repository.Section.APPOINTMENTS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        // **The toggle appears only where there is something to look at**, the
        // same call the documents screen makes: a choice of two views over an
        // empty notebook is furniture.
        if (appointments.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ViewToggle(
                        options = listOf(
                            ViewOption(VIEW_AGENDA, "appts.view.agenda"),
                            ViewOption(VIEW_MONTH, "appts.view.month"),
                        ),
                        selected = view.value,
                        onSelect = view.onSelect,
                        modifier = Modifier.testTag(ApptTags.TOGGLE),
                    )
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        if (view.value == VIEW_MONTH && appointments.isNotEmpty()) {
            item {
                MonthOfAppointments(
                    month = month,
                    today = today,
                    zone = zone,
                    appointments = appointments,
                    onStep = { monthOffset += it },
                    onOpen = onOpen,
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            item {
                Spacer(Modifier.height(Space.s))
                QuietButton(
                    label = strings["appts.add"],
                    onClick = onAdd,
                    modifier = Modifier.testTag(ApptTags.ADD),
                )
            }
            return@SectionScaffold
        }

        // **The next one leads and what has happened folds.** Grid screen 22
        // and law 1: the question a person opens this screen with is "what is
        // next", so that is the one thing, and thirty one past appointments are
        // the record rather than the job.
        if (upcoming.isNotEmpty()) {
            item {
                Block(padding = Space.none) {
                    upcoming.forEachIndexed { index, appointment ->
                        AppointmentRow(
                            appointment = appointment,
                            onOpen = { onOpen(appointment) },
                            isLast = index == upcoming.lastIndex,
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        if (past.isNotEmpty()) {
            item {
                FoldRow(
                    labelKey = "appts.group.past",
                    expanded = pastOpen,
                    onToggle = { pastOpen = !pastOpen },
                    count = past.size.toString(),
                    modifier = Modifier.testTag(ApptTags.PAST_FOLD),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            if (pastOpen) {
                item {
                    Block(padding = Space.none) {
                        // Most recent first, which is the reverse of upcoming
                        // and is what somebody looking back actually wants.
                        val recent = past.reversed()
                        recent.forEachIndexed { index, appointment ->
                            AppointmentRow(
                                appointment = appointment,
                                onOpen = { onOpen(appointment) },
                                isLast = index == recent.lastIndex,
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["appts.add"],
                onClick = onAdd,
                modifier = Modifier.testTag(ApptTags.ADD),
            )
        }
    }
}

@Composable
private fun AppointmentRow(
    appointment: Repository.Appointment,
    onOpen: () -> Unit,
    isLast: Boolean,
) {
    val strings = LocalStrings.current

    // **The date is the trailing value**, because it is what somebody scans a
    // list of appointments by, and it is data so it is Mono and tabular. Where
    // it is and any note become the second line.
    Column {
        ListRow(
            title = Bidi.isolate(appointment.title),
            support = listOfNotNull(
                appointment.locationNote?.takeIf { it.isNotBlank() },
                appointment.notes?.takeIf { it.isNotBlank() },
            ).let { Bidi.join(it) }.takeIf { it.isNotBlank() },
            value = EventDateText.render(strings, appointment.scheduledEdtf),
            isDoor = true,
            onClick = onOpen,
            clickLabel = strings["open.action"],
            modifier = Modifier.testTag(ApptTags.row(appointment.id)),
        )
        if (!isLast) RowDivider(inset = false)
    }
}

/**
 * One month of appointments, drawn from what is recorded here and nothing else.
 *
 * **No calendar is read, ever**, per D90 and `DESIGN.md` 9.1: the component
 * takes days and marks and has no way to reach a provider, and the manifest
 * asks for no calendar permission. A month view is exactly the feature that
 * tempts an app to ask.
 *
 * **A day with nothing on it does nothing when tapped.** Opening an empty day
 * onto an empty screen is the dead end rule 18 names, and a care month is
 * mostly empty days by nature.
 */
@Composable
private fun MonthOfAppointments(
    month: YearMonth,
    today: LocalDate,
    zone: ZoneId,
    appointments: List<Repository.Appointment>,
    onStep: (Int) -> Unit,
    onOpen: (Repository.Appointment) -> Unit,
) {
    val strings = LocalStrings.current
    val locale = strings.locale
    val numbers = remember(locale) { java.text.NumberFormat.getIntegerInstance(locale) }

    val byDay = remember(appointments, zone) {
        appointments.mapNotNull { appointment ->
            appointment.scheduledStart?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate() to appointment
            }
        }.groupBy({ it.first }, { it.second })
    }

    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdayLabels = remember(locale, firstDayOfWeek) {
        (0..6).map {
            firstDayOfWeek.plus(it.toLong())
                .getDisplayName(java.time.format.TextStyle.NARROW, locale)
        }
    }

    val days = remember(month, today, byDay, firstDayOfWeek, locale) {
        val first = month.atDay(1)
        // How many days of the previous month fill the first week, so the grid
        // starts on the person's own first day of the week rather than Monday.
        val lead = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
        val start = first.minusDays(lead.toLong())
        val cells = ((lead + month.lengthOfMonth() + 6) / 7) * 7
        (0 until cells).map { index ->
            val date = start.plusDays(index.toLong())
            val on = byDay[date].orEmpty()
            MonthDay(
                label = numbers.format(date.dayOfMonth),
                count = on.size,
                description = Bidi.join(
                    EventDateText.dayHeading(strings, date),
                    strings("notebook.count.appointments", "count" to on.size),
                ),
                inMonth = YearMonth.from(date) == month,
                isToday = date == today,
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth().testTag(ApptTags.MONTH)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietButton(
                label = strings["appts.month.back"],
                onClick = { onStep(-1) },
                modifier = Modifier.testTag(ApptTags.MONTH_BACK),
            )
            Text(
                // bidi-ok: a month name and a year, composed by the catalog's
                // own pattern rather than by anything the person typed.
                text = EventDateText.monthHeading(
                    strings,
                    month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                    zone,
                ),
                style = HealthTrail.type.displayS,
                color = HealthTrail.colors.ink,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            QuietButton(
                label = strings["appts.month.next"],
                onClick = { onStep(1) },
                modifier = Modifier.testTag(ApptTags.MONTH_NEXT),
            )
        }
        Spacer(Modifier.height(Space.cardGap))
        MonthGrid(
            weekdayLabels = weekdayLabels,
            days = days,
            hue = hueFor(Repository.Section.APPOINTMENTS),
            onOpenDay = { day ->
                // The label is the person's own numerals, so the day is found
                // by position rather than by parsing it back.
                val index = days.indexOf(day)
                if (index >= 0) {
                    val first = month.atDay(1)
                    val lead = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
                    val date = first.minusDays(lead.toLong()).plusDays(index.toLong())
                    byDay[date]?.firstOrNull()?.let(onOpen)
                }
            },
        )
    }
}
