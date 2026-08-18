package com.kamsiob.healthtrail.ui.v4

import android.content.Intent
import android.provider.CalendarContract
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import java.time.ZoneId

/**
 * Handing one appointment to the phone's own calendar app.
 *
 * `DESIGN.md` 9.1: **one event, user initiated, one way, with nothing read
 * back.** No account, no sync, and no calendar permission is ever requested.
 * `ACTION_INSERT` needs none, because it does not write anything: it opens the
 * calendar app's own new event screen already filled in, and nothing exists
 * until the person taps save there. They see exactly what is going across
 * before it goes, which is a stronger consent than any dialog this app could
 * put in front of them.
 *
 * **The notes do not go.** A calendar is, on most phones, the one thing on the
 * device that syncs to an account by default, and the notes on an appointment
 * are the care record. What travels is the three things that make an event:
 * what it is called, when it is, and where. Rule 23 filters on safe, private,
 * and compatible before it asks what is easiest, and putting somebody's care
 * notes into a synced calendar fails the second one.
 *
 * **Only a date somebody could actually keep.** "Sometime in March" is a real
 * thing to be told and a real thing to record, and it is not an event. Handing
 * it over as March 1st would invent a precision the person never gave, which is
 * rule 17, so the offer is not made and the screen shows no action at all.
 */
object CalendarHandoff {

    /**
     * The intent, or null when this appointment has no date precise enough to
     * be an event.
     *
     * The caller shows nothing rather than an action that would open a calendar
     * on a day the person never named.
     */
    fun intent(
        title: String,
        scheduledEdtf: String?,
        /** The stored instant, already resolved in the zone it was recorded in. */
        scheduledStart: Long?,
        locationNote: String?,
    ): Intent? {
        val date = scheduledEdtf?.takeIf { it.isNotBlank() }?.let { Edtf.parse(it) } ?: return null

        // **An all day event starts at midnight where the person is standing,
        // and a timed one at the instant that was stored.**
        //
        // **The current zone rather than the stored one, for a whole day.** The
        // stored instant is resolved in whichever zone the person was in when
        // they wrote it down, and somebody who has since traveled would have
        // that rendered in the zone they are in now, a day out either way. A
        // date with no clock on it is a calendar date, and it means that date
        // wherever they happen to be.
        //
        // A moment is different and keeps its instant. Half past two is half
        // past two in the room it was said in, and that is what was recorded.
        val day = runCatching {
            LocalDate.parse(date.canonical.take(DAY_LENGTH))
        }.getOrNull()

        val begin = when (date.precision) {
            Edtf.Precision.DAY -> day?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
            Edtf.Precision.MOMENT -> scheduledStart
            else -> null
        } ?: return null

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)

            // **A timed appointment gets no end.** How long it runs is not
            // something this app was ever told, and a tidy one hour default
            // would be this app inventing something inside somebody else's
            // calendar. Left absent, the calendar app applies the default its
            // own person already chose, which is theirs to decide and not ours.
            if (date.precision == Edtf.Precision.DAY) {
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)

                // **An all day event ends at the next midnight, and saying so
                // is not inventing a duration.** The end of a whole day event
                // is exclusive: it is the start of the following day, which the
                // date itself already decides. Without it the calendar app
                // received a zero length day and drew the event on **November
                // 26 for an appointment on the 27th**, found on the phone on
                // 2026-08-04 and blamed twice on time zones before the end was
                // the thing missing.
                day?.plusDays(1)?.atStartOfDay(ZoneId.systemDefault())?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.toInstant().toEpochMilli())
                }
            }

            locationNote?.takeIf { it.isNotBlank() }?.let {
                putExtra(CalendarContract.Events.EVENT_LOCATION, it)
            }
        }
    }

    /** `YYYY-MM-DD`, the leading part of both a day and a moment. */
    private const val DAY_LENGTH = 10
}
