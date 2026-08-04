package com.kamsiob.healthtrail.data

import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * How a date reads in the archive. `contract/DATA-CONTRACT.md` section 8.2.
 *
 * The contract is short and every clause of it is load bearing:
 *
 * > Every date renders in a form a stranger reads without ambiguity, showing the
 * > local date and time as the person experienced it plus the UTC offset. Never
 * > a bare epoch number. Never a locale-ambiguous numeric date such as
 * > 03/04/2027.
 *
 * **The month is spelled out**, which is the whole of the ambiguity fix.
 * `03/04/2027` is March in the United States and April nearly everywhere else,
 * and a care record read by a sibling abroad or a lawyer years later cannot
 * carry a date that means two things.
 *
 * **The offset travels with the time**, so a note written at 9pm in New York
 * still reads as 9pm after the person moves to Berlin. The offset is the
 * evidence that the reading is the one the person saw, rather than a
 * recalculation into wherever the file is being opened.
 *
 * **Precision is never invented.** `DESIGN.md` 9.2 and `CLAUDE.md` rule 17: a
 * month stays a month. "Sometime in November 2024" is honest and "November 1,
 * 2024" for the same input is a fabrication, and it is a fabrication that
 * survives into a document somebody may rely on.
 *
 * **Unknown is a real answer**, not a missing one, and it says so.
 *
 * **English, deliberately, and this is the one place the archive is not in the
 * person's language.** Section 8.2 requires the readable copy to be written in
 * the locale the person used the app in, and that governs the prose. A date is
 * different: it is the thing a stranger must read without ambiguity, and the
 * stranger in the failure case this section exists for is a records office, a
 * lawyer, or a doctor who does not read Arabic. So the month name is spelled in
 * English alongside the person's own prose. **This is a deliberate exception and
 * it is recorded here rather than left as an accident**, because it is the kind
 * of thing a later session would "fix" by localizing it.
 */
internal object ReadableDate {

    /** What a date with no value at all reads as. */
    const val UNKNOWN = "date not recorded"

    /**
     * One event date, at exactly the precision it was given.
     *
     * @param edtf the stored EDTF string, which is the source of truth and what
     *   round trips unchanged.
     * @param zoneId the IANA zone in effect where the entry was made, so the
     *   offset shown is the one the person was standing in.
     */
    fun render(edtf: String?, zoneId: String?): String {
        if (edtf.isNullOrBlank()) return UNKNOWN
        val date = Edtf.parse(edtf) ?: return UNKNOWN
        val zone = Edtf.zoneOrUtc(zoneId)

        val qualified = when (date.qualifier) {
            Edtf.Qualifier.NONE -> ""
            Edtf.Qualifier.UNCERTAIN -> ", the person was not sure"
            Edtf.Qualifier.APPROXIMATE -> ", approximately"
            Edtf.Qualifier.BOTH -> ", approximate and not certain"
        }

        val body = when (date.precision) {
            Edtf.Precision.UNKNOWN -> return UNKNOWN
            Edtf.Precision.MOMENT -> moment(date.canonical, zone)
            Edtf.Precision.DAY -> day(date.canonical)
            Edtf.Precision.MONTH -> month(date.canonical)
            Edtf.Precision.YEAR -> date.canonical.take(4)
            Edtf.Precision.SEASON -> season(date.canonical)
            Edtf.Precision.WEEK, Edtf.Precision.RANGE -> range(date.canonical, zone)
        }
        return body + qualified
    }

    /**
     * A row timestamp, for the few places the archive prints one.
     *
     * **Never a bare epoch number**, which is the other half of the contract's
     * date rule and the easier half to get wrong, because an epoch is what the
     * column actually holds and printing it takes no work at all.
     */
    fun timestamp(millis: Long?, zoneId: String?): String {
        if (millis == null) return UNKNOWN
        val zone = Edtf.zoneOrUtc(zoneId)
        val at = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone)
        return "${monthName(at.monthValue)} ${at.dayOfMonth}, ${at.year} at " +
            "${twoDigit(at.hour)}:${twoDigit(at.minute)} ${offset(at)}"
    }

    private fun moment(canonical: String, zone: ZoneId): String {
        val at = runCatching { LocalDateTime.parse(canonical.take(16)) }.getOrNull()
            ?: return day(canonical)
        val zoned = at.atZone(zone)
        return "${monthName(at.monthValue)} ${at.dayOfMonth}, ${at.year} at " +
            "${twoDigit(at.hour)}:${twoDigit(at.minute)} ${offset(zoned)}"
    }

    private fun day(canonical: String): String {
        val date = runCatching { LocalDate.parse(canonical.take(10)) }.getOrNull()
            ?: return canonical
        return "${monthName(date.monthValue)} ${date.dayOfMonth}, ${date.year}"
    }

    private fun month(canonical: String): String {
        val year = canonical.take(4)
        val m = canonical.drop(5).take(2).toIntOrNull() ?: return year
        // "Sometime in" rather than a bare month, because a bare month beside a
        // precise date in the next row reads as a shorthand for its first day
        // rather than as the person having only known the month.
        return "sometime in ${monthName(m)} $year"
    }

    private fun season(canonical: String): String {
        val year = canonical.take(4)
        return when (canonical.drop(5).take(2)) {
            "21" -> "spring $year"
            "22" -> "summer $year"
            "23" -> "autumn $year"
            "24" -> "winter $year"
            else -> year
        }
    }

    private fun range(canonical: String, zone: ZoneId): String {
        val parts = canonical.split("/")
        if (parts.size != 2) return canonical
        val from = render(parts[0], zone.id)
        val to = render(parts[1], zone.id)
        return "between $from and $to"
    }

    /**
     * `UTC+02:00`, or `UTC` on the dot.
     *
     * Spelled with the letters rather than as a bare `+02:00`, because the bare
     * form is only unambiguous to somebody who already knows what it is.
     */
    private fun offset(at: ZonedDateTime): String {
        val id = at.offset.id
        return if (id == "Z") "UTC" else "UTC$id"
    }

    private fun monthName(month: Int): String =
        java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    private fun twoDigit(value: Int): String = if (value < 10) "0$value" else "$value"
}
