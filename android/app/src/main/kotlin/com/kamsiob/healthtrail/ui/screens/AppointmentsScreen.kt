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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ApptTags {
    const val NAME = "appointments"
    const val ADD = "appointments_add"
    fun row(id: String) = "appointment_$id"
}

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
 * The prep sheet of `MASTER_SPEC.md` section 4.3, which gathers the questions
 * waiting for that person and a change summary, is Phase 2 and is not built.
 * This is the list underneath it.
 */
@Composable
fun AppointmentsScreen(
    appointments: List<Repository.Appointment>,
    todayMillis: Long,
    onRemove: (Repository.Appointment) -> Unit,
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
    val upcoming = appointments.filter {
        it.scheduledStart == null || it.scheduledStart >= todayMillis
    }
    val past = appointments.filter {
        it.scheduledStart != null && it.scheduledStart < todayMillis
    }

    SectionScaffold(
        name = ApptTags.NAME,
        title = strings["notebook.section.appointments"],
        subtitle = strings["appts.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (appointments.isEmpty()) {
            item {
                SectionEmpty(name = ApptTags.NAME, text = strings["appts.empty"], section = Repository.Section.APPOINTMENTS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "appts.group.upcoming")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (appointment in upcoming) {
                item(key = appointment.id) {
                    AppointmentRow(
                        appointment = appointment,
                        onRemove = { onRemove(appointment) },
                        onOpen = { onOpen(appointment) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        if (past.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "appts.group.past")
                Spacer(Modifier.height(Space.headerGap))
            }
            // Most recent first, which is the reverse of upcoming and is what
            // somebody looking back actually wants.
            for (appointment in past.reversed()) {
                item(key = appointment.id) {
                    AppointmentRow(
                        appointment = appointment,
                        onRemove = { onRemove(appointment) },
                        onOpen = { onOpen(appointment) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.s))
            QuietButton(
                label = strings["appts.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(ApptTags.ADD),
            )
        }
    }
}

@Composable
private fun AppointmentRow(
    appointment: Repository.Appointment,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            .removableByLongPress(strings["edit.hint"], onRemove, onOpen)
            .testTag(ApptTags.row(appointment.id))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = EventDateText.render(strings, appointment.scheduledEdtf),
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
        Spacer(Modifier.height(Space.xs))

        Text(
            text = appointment.title,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        appointment.locationNote?.takeIf { it.isNotBlank() }?.let { where ->
            Spacer(Modifier.height(Space.xs))
            Text(text = where, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        appointment.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.xs))
            Text(text = notes, style = HealthTrail.type.bodyS, color = colors.ink3Text)
        }
    }
}
