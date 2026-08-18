package com.kamsiob.healthtrail.ui

import android.content.Intent
import android.provider.CalendarContract
import com.kamsiob.healthtrail.ui.v4.CalendarHandoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the calendar hand-off offers, and what it refuses to offer.
 *
 * **The day this covers was wrong on the phone.** An appointment recorded for
 * November 27 opened the calendar app on Thursday the 26th, because an all day
 * event's end is exclusive and none was sent, so the calendar received a day of
 * zero length and drew the day before. Nothing in the app looked wrong: the
 * screen said November 27 the whole time, and the only place the defect existed
 * was inside somebody else's app after they had tapped.
 *
 * **It is instrumented rather than a unit test** because the intent extras are
 * Android's own and `CalendarContract` is not on the host classpath.
 */
class CalendarHandoffTest {

    @Test
    fun aWholeDayCoversThatDayAndEndsAtTheNextMidnight() {
        val intent = CalendarHandoff.intent(
            title = "Doctor, follow up",
            scheduledEdtf = "2026-11-27",
            scheduledStart = null,
            locationNote = "Suite 210, the medical building",
        )
        assertNotNull(intent)
        checkNotNull(intent)

        val zone = ZoneId.systemDefault()
        val begin = LocalDate.of(2026, 11, 27).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 11, 28).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.data)
        assertTrue(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        assertEquals(begin, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        // **The one that was missing.** Without it the event lands a day early.
        assertEquals(end, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0))
    }

    @Test
    fun aTimeKeepsItsInstantAndGetsNoEnd() {
        val instant = 1_795_791_600_000L
        val intent = CalendarHandoff.intent(
            title = "Doctor, follow up",
            scheduledEdtf = "2026-11-27T10:00",
            scheduledStart = instant,
            locationNote = null,
        )
        assertNotNull(intent)
        checkNotNull(intent)

        assertEquals(instant, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        assertFalse(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        // How long it runs was never recorded, so the calendar app's own
        // default applies rather than one this app made up.
        assertFalse(intent.hasExtra(CalendarContract.EXTRA_EVENT_END_TIME))
    }

    @Test
    fun theNotesNeverTravel() {
        // The one thing this must not do. A calendar syncs to an account on
        // most phones, and the notes on an appointment are the care record.
        val intent = CalendarHandoff.intent(
            title = "Doctor, follow up",
            scheduledEdtf = "2026-11-27",
            scheduledStart = null,
            locationNote = "Suite 210",
        )
        checkNotNull(intent)

        assertFalse(intent.hasExtra(CalendarContract.Events.DESCRIPTION))
        assertEquals("Suite 210", intent.getStringExtra(CalendarContract.Events.EVENT_LOCATION))
        assertEquals("Doctor, follow up", intent.getStringExtra(CalendarContract.Events.TITLE))
    }

    @Test
    fun aDateCoarserThanADayIsNotOffered() {
        // "Sometime in March" is a real thing to be told and is not an event.
        // Handing it over as March 1st would invent a precision nobody gave.
        listOf("2026-03", "2026", "2026-21", "XXXX-XX-XX", "2026-03/2026-05").forEach { edtf ->
            assertNull(
                edtf,
                CalendarHandoff.intent(
                    title = "Doctor, follow up",
                    scheduledEdtf = edtf,
                    scheduledStart = 1_795_791_600_000L,
                    locationNote = null,
                ),
            )
        }
    }

    @Test
    fun noDateAtAllIsNotOffered() {
        listOf(null, "", "   ", "not a date").forEach { edtf ->
            assertNull(
                CalendarHandoff.intent(
                    title = "Doctor, follow up",
                    scheduledEdtf = edtf,
                    scheduledStart = null,
                    locationNote = null,
                ),
            )
        }
    }

    @Test
    fun aQualifiedDayIsStillADay() {
        // EDTF keeps how sure somebody was separate from how precise they were,
        // and so does this. "Probably the 27th" is still the 27th.
        val intent = CalendarHandoff.intent(
            title = "Doctor, follow up",
            scheduledEdtf = "2026-11-27?",
            scheduledStart = null,
            locationNote = null,
        )
        assertNotNull(intent)
        checkNotNull(intent)

        val begin = LocalDate.of(2026, 11, 27)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(begin, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
    }
}
