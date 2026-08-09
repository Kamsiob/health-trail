package com.kamsiob.healthtrail.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Event dates, per `contract/DATA-CONTRACT.md` section 3.1.
 *
 * A care record spans years and is written from memory. "The fall was sometime
 * in November 2024." "She was moved in the fall." "I called them, I think it
 * was a Tuesday." Storing only a precise timestamp turns every one of those
 * into a claim the person never made, and after an export and reimport there is
 * nothing left to tell a guess from a known time.
 *
 * **The format is EDTF**, ISO 8601-2:2019, developed by the Library of Congress
 * for exactly this. Nothing here is invented. Precision is expressed by
 * truncation, so the shorter the string the less the person claimed, and
 * uncertainty is a separate axis from precision, because knowing the month and
 * being unsure of it is not the same as knowing only the year.
 *
 * **The string is the truth and the range is an index.** [resolve] is how the
 * database sorts and answers range queries, and it is recomputed from the
 * string rather than stored alongside it as a second opinion. If the two ever
 * disagree, the string wins.
 *
 * **The string carries a local wall-clock reading with no offset.** A visit
 * logged at 2:40 pm happened at 2:40 pm where the person was and still reads as
 * 2:40 pm after they travel. The zone is carried beside it so the range can be
 * computed, and recomputed identically later.
 */
object Edtf {

    /** How much the person actually said. */
    enum class Precision {
        /** Down to the minute. */
        MOMENT,
        DAY,

        /** EDTF has no week token, so a week is an interval, which is what a week is. */
        WEEK,
        MONTH,

        /** EDTF sub-year groupings 21 through 24. */
        SEASON,
        YEAR,

        /** Any other interval the person gave: "between the move and the fall". */
        RANGE,

        /** Genuinely unknown, and a real answer rather than a missing one. */
        UNKNOWN,
    }

    /**
     * How sure the person was, which EDTF keeps separate from how precise they
     * were, and so does this.
     */
    enum class Qualifier(val suffix: String) {
        NONE(""),
        UNCERTAIN("?"),
        APPROXIMATE("~"),
        BOTH("%"),
        ;

        companion object {
            fun of(suffix: Char): Qualifier? = entries.firstOrNull {
                it != NONE && it.suffix.first() == suffix
            }
        }
    }

    /**
     * One parsed event date.
     *
     * [canonical] is what goes in the column and what survives export and
     * import unchanged. Everything else on this class is read from it.
     */
    data class Date(
        val canonical: String,
        val precision: Precision,
        val qualifier: Qualifier,
    )

    /**
     * The earliest and latest instant a date could refer to, UTC milliseconds.
     *
     * Both null for [Precision.UNKNOWN], which is the one date that cannot be
     * placed on a line. It sorts by when it was written down instead, and it is
     * never hidden for it.
     */
    data class Range(val start: Long?, val end: Long?) {
        val isUnbounded: Boolean get() = start == null && end == null
    }

    /** The unspecified-digits form. EDTF's own way of saying nothing is known. */
    const val UNKNOWN = "XXXX-XX-XX"

    // -- building ----------------------------------------------------------

    fun unknown(): Date = Date(UNKNOWN, Precision.UNKNOWN, Qualifier.NONE)

    fun moment(at: LocalDateTime, qualifier: Qualifier = Qualifier.NONE): Date = Date(
        "%04d-%02d-%02dT%02d:%02d".format(
            at.year, at.monthValue, at.dayOfMonth, at.hour, at.minute,
        ) + qualifier.suffix,
        Precision.MOMENT,
        qualifier,
    )

    fun day(on: LocalDate, qualifier: Qualifier = Qualifier.NONE): Date = Date(
        "%04d-%02d-%02d".format(on.year, on.monthValue, on.dayOfMonth) + qualifier.suffix,
        Precision.DAY,
        qualifier,
    )

    fun month(year: Int, month: Int, qualifier: Qualifier = Qualifier.NONE): Date = Date(
        "%04d-%02d".format(year, month) + qualifier.suffix,
        Precision.MONTH,
        qualifier,
    )

    fun year(year: Int, qualifier: Qualifier = Qualifier.NONE): Date = Date(
        "%04d".format(year) + qualifier.suffix,
        Precision.YEAR,
        qualifier,
    )

    /** Sub-year groupings 21 spring, 22 summer, 23 autumn, 24 winter. */
    fun season(year: Int, season: Int, qualifier: Qualifier = Qualifier.NONE): Date {
        require(season in SEASON_FIRST..SEASON_LAST) { "not an EDTF season: $season" }
        return Date("%04d-%02d".format(year, season) + qualifier.suffix, Precision.SEASON, qualifier)
    }

    /** The week containing [anyDayInIt], Monday through Sunday, as the interval it is. */
    fun week(anyDayInIt: LocalDate, qualifier: Qualifier = Qualifier.NONE): Date {
        val monday = anyDayInIt.minusDays((anyDayInIt.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        return Date(
            "${isoDay(monday)}/${isoDay(sunday)}" + qualifier.suffix,
            Precision.WEEK,
            qualifier,
        )
    }

    fun range(from: Date, to: Date, qualifier: Qualifier = Qualifier.NONE): Date = Date(
        "${from.canonical.trimQualifier()}/${to.canonical.trimQualifier()}" + qualifier.suffix,
        Precision.RANGE,
        qualifier,
    )

    // -- reading -----------------------------------------------------------

    /**
     * Reads a stored string.
     *
     * Returns null for anything this version does not understand, which is a
     * real case rather than a defensive one: an export written by a later
     * version of the app will carry forms this one has never seen, and it has
     * to open rather than fail. The caller keeps the string and renders it as
     * a date it cannot read, which is honest, rather than discarding it.
     */
    fun parse(text: String): Date? {
        if (text.isEmpty()) return null

        val qualifier = Qualifier.of(text.last()) ?: Qualifier.NONE
        val body = if (qualifier == Qualifier.NONE) text else text.dropLast(1)
        if (body.isEmpty()) return null

        if (body == UNKNOWN) {
            // Uncertainty about a date nobody knows is not a distinction worth
            // carrying, and allowing it would give two spellings of one thing.
            return if (qualifier == Qualifier.NONE) unknown() else null
        }

        if (body.contains('/')) {
            val halves = body.split('/')
            if (halves.size != 2) return null
            val from = parse(halves[0]) ?: return null
            val to = parse(halves[1]) ?: return null
            if (from.precision == Precision.UNKNOWN || to.precision == Precision.UNKNOWN) return null
            // A Monday through Sunday pair of days is what `week` writes, and
            // reading it back as a week rather than as a plain interval is what
            // lets the screen say "sometime that week" instead of naming two
            // dates the person never said.
            val isWeek = from.precision == Precision.DAY && to.precision == Precision.DAY &&
                runCatching {
                    val a = LocalDate.parse(halves[0])
                    val b = LocalDate.parse(halves[1])
                    a.dayOfWeek.value == 1 && b == a.plusDays(6)
                }.getOrDefault(false)
            return Date(text, if (isWeek) Precision.WEEK else Precision.RANGE, qualifier)
        }

        val precision = when {
            MOMENT.matches(body) -> Precision.MOMENT
            DAY.matches(body) -> Precision.DAY
            YEAR_MONTH.matches(body) -> {
                val month = body.substring(5).toInt()
                when {
                    month in SEASON_FIRST..SEASON_LAST -> Precision.SEASON
                    month in 1..12 -> Precision.MONTH
                    else -> return null
                }
            }
            YEAR.matches(body) -> Precision.YEAR
            else -> return null
        }

        // A shape that parses but names no real day, 2024-02-31 for instance,
        // is not a date and must not become one silently.
        if (precision == Precision.DAY || precision == Precision.MOMENT) {
            val valid = runCatching {
                if (precision == Precision.DAY) LocalDate.parse(body) else LocalDateTime.parse(body)
            }.isSuccess
            if (!valid) return null
        }

        return Date(text, precision, qualifier)
    }

    /**
     * The earliest and latest instant this date could mean, in [zone].
     *
     * **Uncertainty never widens the range.** Being unsure about November is
     * still a claim about November, and widening it would quietly turn the
     * person's hedge into a different answer than the one they gave.
     */
    /**
     * Whether a stored date names a single day, or a moment within one.
     *
     * **The question every distance has to ask first.** A bare year or a year
     * and a month is a real answer, rule 17, and the distance between two of
     * them is not a number anybody gave: subtracting them would turn somebody's
     * "sometime in April" into a confident "three weeks earlier". So a gap
     * marker, a countdown, and anything else measured between two dates asks
     * this before it computes.
     *
     * **Through the parser rather than by inspecting the string.** There were
     * two private copies of this, one on the trail and one on a project's
     * trail, and the second was a regular expression matching four digits, a
     * dash and two more. That is the parser rewritten badly in one line, and it
     * would have called an interval starting on a day day-precise.
     */
    fun isDayPrecise(text: String?): Boolean {
        val parsed = text?.takeIf { it.isNotBlank() }?.let { parse(it) } ?: return false
        return parsed.precision == Precision.DAY || parsed.precision == Precision.MOMENT
    }

    fun resolve(date: Date, zone: ZoneId): Range {
        val body = date.canonical.trimQualifier()
        if (date.precision == Precision.UNKNOWN) return Range(null, null)

        if (body.contains('/')) {
            val halves = body.split('/')
            val from = parse(halves[0])?.let { resolve(it, zone) } ?: return Range(null, null)
            val to = parse(halves[1])?.let { resolve(it, zone) } ?: return Range(null, null)
            return Range(from.start, to.end)
        }

        return when (date.precision) {
            Precision.MOMENT -> {
                val at = LocalDateTime.parse(body)
                val ms = at.atZone(zone).toInstant().toEpochMilli()
                Range(ms, ms)
            }
            Precision.DAY -> {
                val on = LocalDate.parse(body)
                Range(startOf(on, zone), startOf(on.plusDays(1), zone) - 1)
            }
            Precision.MONTH -> {
                val first = LocalDate.of(body.substring(0, 4).toInt(), body.substring(5).toInt(), 1)
                Range(startOf(first, zone), startOf(first.plusMonths(1), zone) - 1)
            }
            Precision.SEASON -> {
                val year = body.substring(0, 4).toInt()
                val (firstMonth, months) = seasonMonths(body.substring(5).toInt())
                val first = LocalDate.of(year, firstMonth, 1)
                Range(startOf(first, zone), startOf(first.plusMonths(months.toLong()), zone) - 1)
            }
            Precision.YEAR -> {
                val first = LocalDate.of(body.toInt(), 1, 1)
                Range(startOf(first, zone), startOf(first.plusYears(1), zone) - 1)
            }
            // Both are intervals and were handled above. Reaching here means a
            // stored string disagreed with its own parsed precision.
            Precision.WEEK, Precision.RANGE, Precision.UNKNOWN -> Range(null, null)
        }
    }

    /**
     * The zone to store beside the string, or null where the precision is
     * coarser than a day, since a month has no clock and recording a zone for
     * one would be recording something the person never said.
     */
    fun zoneFor(date: Date, zone: ZoneId): String? = when (date.precision) {
        Precision.MOMENT, Precision.DAY, Precision.WEEK -> zone.id
        else -> null
    }

    /** Falls back to UTC for a zone this device does not know, rather than throwing. */
    fun zoneOrUtc(id: String?): ZoneId =
        id?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneOffset.UTC

    private fun startOf(day: LocalDate, zone: ZoneId): Long =
        day.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun isoDay(day: LocalDate) =
        "%04d-%02d-%02d".format(day.year, day.monthValue, day.dayOfMonth)

    /**
     * The meteorological seasons EDTF's 21 through 24 name.
     *
     * **Winter crosses the year boundary**, so `2024-24` runs from December
     * 2024 into February 2025. That is what winter is, and resolving it inside
     * the calendar year instead would place a January event outside the season
     * the person named it by.
     */
    private fun seasonMonths(season: Int): Pair<Int, Int> = when (season) {
        21 -> 3 to 3
        22 -> 6 to 3
        23 -> 9 to 3
        else -> 12 to 3
    }

    private fun String.trimQualifier(): String =
        if (isNotEmpty() && Qualifier.of(last()) != null) dropLast(1) else this

    private const val SEASON_FIRST = 21
    private const val SEASON_LAST = 24

    private val MOMENT = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$""")
    private val DAY = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val YEAR_MONTH = Regex("""^\d{4}-\d{2}$""")
    private val YEAR = Regex("""^\d{4}$""")
}
