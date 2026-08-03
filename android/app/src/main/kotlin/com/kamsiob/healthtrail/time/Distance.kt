package com.kamsiob.healthtrail.time

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * How far apart two things on the trail are, said in words.
 *
 * **A trail map tells you how far apart things are and a list does not.**
 * `DESIGN.md` section 5.2.4. Two calls a week apart read as a week of calls;
 * the same two rows with four months between them read as somebody who was left
 * alone until something happened. The list shows the same rows either way, and
 * the story is only in the dates, where nobody does the arithmetic.
 *
 * **It is never a judgment and never a warning.** This is subtraction on two
 * dates the person recorded. Nothing here says a gap was too long, nothing is
 * colored, and there is no threshold above which the app has an opinion, per
 * `CLAUDE.md` rule 2.
 *
 * **Pure, and tested as a table.** No Android, no composition, no clock: the
 * two instants and the zone come in and a key and a count come out. That is the
 * shape `DigestTest` uses and the reason both can be checked exhaustively
 * without a device.
 */
object Distance {

    /**
     * Below this, no marker at all.
     *
     * **Fourteen days**, because under it the line appears between almost every
     * pair of rows and stops being information. A marker on every row is
     * wallpaper, and wallpaper is what section 1 calls uniform weight: it
     * pushes the whole job of sorting back onto the person.
     */
    const val THRESHOLD_DAYS = 14L

    /** What to say, as a catalog key and the number to put in it. */
    data class Gap(val key: String, val count: Long)

    /**
     * The gap between two adjacent trail rows, or null when there is nothing
     * worth saying.
     *
     * Null when either date is missing, when they are the same day, or when the
     * gap is under [THRESHOLD_DAYS].
     *
     * **A date the person gave coarsely never produces a marker**, because the
     * distance genuinely is not known and rule 17 forbids inventing precision.
     * That is the caller's decision rather than this function's, since only the
     * caller knows the EDTF precision, and it is documented on the parameters
     * rather than left implicit.
     *
     * @param olderMillis the earlier of the two, at day resolution
     * @param newerMillis the later of the two
     */
    fun between(olderMillis: Long?, newerMillis: Long?, zone: ZoneId): Gap? {
        if (olderMillis == null || newerMillis == null) return null

        val older = Instant.ofEpochMilli(olderMillis).atZone(zone).toLocalDate()
        val newer = Instant.ofEpochMilli(newerMillis).atZone(zone).toLocalDate()
        if (!older.isBefore(newer)) return null

        val days = ChronoUnit.DAYS.between(older, newer)
        if (days < THRESHOLD_DAYS) return null

        // **Calendar units, not divided days.** A month is not 30 days and a
        // year is not 365, and a person reading "twelve months later" about
        // something a year ago would be right to think the app was counting
        // rather than reading a calendar.
        val years = ChronoUnit.YEARS.between(older, newer)
        if (years >= 1) return Gap("trail.gap.years", years)

        val months = ChronoUnit.MONTHS.between(older, newer)
        if (months >= 1) return Gap("trail.gap.months", months)

        val weeks = ChronoUnit.WEEKS.between(older, newer)
        if (weeks >= 1) return Gap("trail.gap.weeks", weeks)

        return Gap("trail.gap.days", days)
    }
}
