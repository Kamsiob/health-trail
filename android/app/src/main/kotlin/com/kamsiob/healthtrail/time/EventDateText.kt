package com.kamsiob.healthtrail.time

import com.kamsiob.healthtrail.i18n.Strings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * How an event date reads, per `DESIGN.md` section 10.9.
 *
 * **The one rule: never invent precision.** "Sometime in November 2024" is
 * honest. "November 1, 2024" for that same input is a fabrication, and it is
 * the fabrication this whole model exists to prevent. Everywhere a date
 * renders goes through here: the trail, month reviews, exports, PDFs, and the
 * engine's composed sentences.
 *
 * **Composed from message templates, never by gluing a formatted date into a
 * sentence.** The catalog owns the framing, so a locale that says the month
 * before the qualifier, or puts no space before a comma, can say so. Even the
 * date patterns are catalog strings, because "November 18, 2024" and
 * "2024年11月18日" are not the same shape with different words in it.
 *
 * **Uncertainty renders as its own clause and never as a change to the date.**
 * A hedge the person offered stays a hedge rather than becoming a vaguer claim.
 */
object EventDateText {

    /**
     * One date, as the person said it.
     *
     * Returns the raw string for anything this version cannot parse, which is
     * what an export from a later version of the app would carry. Showing the
     * stored string is honest and keeps the entry readable. Dropping the entry,
     * or rendering a guess, would not be.
     */
    fun render(strings: Strings, canonical: String?): String {
        if (canonical.isNullOrEmpty()) return strings["date.unknown"]
        val date = Edtf.parse(canonical) ?: return canonical
        return render(strings, date)
    }

    fun render(strings: Strings, date: Edtf.Date): String {
        val base = base(strings, date)
        return when (date.qualifier) {
            Edtf.Qualifier.NONE -> base
            Edtf.Qualifier.UNCERTAIN -> strings("date.uncertain", "date" to base)
            Edtf.Qualifier.APPROXIMATE -> strings("date.approximate", "date" to base)
            Edtf.Qualifier.BOTH -> strings("date.both", "date" to base)
        }
    }

    /**
     * The date with its framing: "Sometime in November 2024".
     *
     * The framing is what stops a coarse date reading as a precise one. A month
     * with no hedge in front of it looks like a claim about the month rather
     * than about a day inside it, which is close enough to the fabrication this
     * model exists to prevent.
     */
    private fun base(strings: Strings, date: Edtf.Date): String = when (date.precision) {
        Edtf.Precision.UNKNOWN -> strings["date.unknown"]

        // A day and a moment need no framing. The formatted date already says
        // exactly what the person said, and "Sometime on November 18" would add
        // a hedge they did not offer.
        Edtf.Precision.MOMENT, Edtf.Precision.DAY -> bare(strings, date)

        Edtf.Precision.WEEK -> strings("date.week", "date" to bare(strings, date))
        Edtf.Precision.MONTH -> strings("date.month", "date" to bare(strings, date))
        Edtf.Precision.YEAR -> strings("date.year", "date" to bare(strings, date))

        // The season templates carry their own framing, since "Fall 2024" is
        // already as vague as it claims to be.
        Edtf.Precision.SEASON -> bare(strings, date)

        // **A range composes bare halves, never framed ones.** Composing the
        // framed forms produced "Between Sometime in November 2024 and Sometime
        // in December 2024", which is what gluing sentences together always
        // gives you and is why the framing lives one layer down.
        Edtf.Precision.RANGE -> {
            val halves = body(date).split('/')
            strings(
                "date.range",
                "from" to (Edtf.parse(halves[0])?.let { bare(strings, it) } ?: halves[0]),
                "to" to (Edtf.parse(halves[1])?.let { bare(strings, it) } ?: halves[1]),
            )
        }
    }

    /**
     * One date, under a heading that has already named its month and year.
     *
     * **The trail said "June 2026" once and then said it again on all forty
     * three rows underneath it.** Every row inside a month band carried the
     * month and the year the band above it had just given, which is most of the
     * ink on the most read screen in the app and is what #361 means by a long
     * list with no shape: when every line starts with the same eleven
     * characters, nothing on the screen distinguishes anything.
     *
     * **The weekday replaces them rather than nothing taking their place.** A
     * bare "29" is a number in a column, and the question somebody actually
     * asks of a record is "was that the Monday". `date.format.weekday_day` is
     * the same shape the day heading already uses, minus the month.
     *
     * **Only a day and a moment shorten.** A coarse date keeps [render] whole,
     * hedge and all, because "sometime in June" is the claim the person made
     * and turning it into a weekday would be inventing a precision they did not
     * give. Rule 17.
     *
     * **The hedge still wraps whatever comes back.** An uncertain Monday reads
     * as an uncertain Monday.
     */
    fun withinMonth(strings: Strings, canonical: String?): String {
        if (canonical.isNullOrEmpty()) return strings["date.unknown"]
        val date = Edtf.parse(canonical) ?: return canonical
        val short = shortened(strings, date) ?: return render(strings, date)
        return when (date.qualifier) {
            Edtf.Qualifier.NONE -> short
            Edtf.Qualifier.UNCERTAIN -> strings("date.uncertain", "date" to short)
            Edtf.Qualifier.APPROXIMATE -> strings("date.approximate", "date" to short)
            Edtf.Qualifier.BOTH -> strings("date.both", "date" to short)
        }
    }

    /** The weekday and the day of the month, or null for anything coarser. */
    private fun shortened(strings: Strings, date: Edtf.Date): String? {
        val body = body(date)
        val pattern = pattern(strings, "date.format.weekday_day_only")
        return when (date.precision) {
            Edtf.Precision.DAY ->
                runCatching { LocalDate.parse(body).format(pattern) }.getOrNull()

            Edtf.Precision.MOMENT -> runCatching {
                val at = LocalDateTime.parse(body)
                strings(
                    "date.moment",
                    "date" to at.toLocalDate().format(pattern),
                    "time" to at.format(pattern(strings, "date.format.time")),
                )
            }.getOrNull()

            else -> null
        }
    }

    /**
     * The month a run of trail entries belongs to, as its group heading.
     *
     * **A heading, so it carries no hedge.** Everywhere else a coarse date is
     * framed, because "November 2024" standing alone reads as a claim that
     * something happened across the whole month. A group header is not making
     * that claim: it is naming the run of entries beneath it, and each of those
     * still says its own date at its own precision.
     *
     * Formatted through the catalog's own pattern, so the shape of the heading
     * is part of the translation rather than something applied to it.
     */
    fun monthHeading(strings: Strings, epochMillis: Long, zone: ZoneId): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), zone)
            .format(pattern(strings, "date.format.month_year"))

    /**
     * A day with its weekday, for a heading that says what day it is.
     *
     * **This is a heading and not an event date**, which is why it carries no
     * hedge and takes a [LocalDate] rather than a stored value: nothing here is
     * reporting when something happened, so there is no precision to preserve
     * and nothing to be uncertain about. The lead slot on Today uses it, per
     * `DESIGN.md` 21.1.
     *
     * **The pattern is a catalog string like every other**, because "Tuesday,
     * April 4" and "4月4日星期二" are not one shape with different words in it.
     */
    fun dayHeading(strings: Strings, date: LocalDate): String =
        date.format(pattern(strings, "date.format.weekday_day"))

    /**
     * "Today" or "Tomorrow", with the time where there is one.
     *
     * **Null for anything else**, which is most dates, and the caller falls back
     * to [render]. This is not a general way of saying dates: it is for the one
     * card whose question is what happens next, where "Tomorrow 10:15" is what
     * somebody standing in a kitchen actually needs and "April 9, 2026" makes
     * them count.
     *
     * **Null for a coarse date too, and that is the important part.** A month is
     * not a day, so it can be neither today nor tomorrow, and answering
     * "sometime in April" with "Today" would be the fabrication rule 17 exists
     * to prevent. Only a day or a moment gets this treatment, and a moment keeps
     * its time.
     *
     * **Never "Yesterday" or any other backwards word.** Nothing that calls this
     * is looking behind it, and a card called next up showing last week is
     * answering a different question from the one it names.
     */
    fun nearby(strings: Strings, canonical: String?, today: LocalDate): String? {
        val date = canonical?.takeIf { it.isNotBlank() }?.let { Edtf.parse(it) } ?: return null
        if (date.qualifier != Edtf.Qualifier.NONE) return null
        val body = date.canonical
        val (day, time) = when (date.precision) {
            Edtf.Precision.DAY -> runCatching { LocalDate.parse(body) }.getOrNull() to null
            Edtf.Precision.MOMENT -> runCatching { LocalDateTime.parse(body) }.getOrNull()
                ?.let { it.toLocalDate() to it.format(pattern(strings, "date.format.time")) }
                ?: (null to null)
            else -> null to null
        }
        val word = when (day) {
            today -> strings["date.today"]
            today.plusDays(1) -> strings["date.tomorrow"]
            else -> return null
        }
        return com.kamsiob.healthtrail.i18n.Bidi.join(word, time)
    }

    /** The date itself, formatted, with no hedge around it. */
    private fun bare(strings: Strings, date: Edtf.Date): String {
        val body = body(date)
        return when (date.precision) {
            Edtf.Precision.UNKNOWN -> strings["date.unknown"]

            Edtf.Precision.MOMENT -> {
                val at = LocalDateTime.parse(body)
                strings(
                    "date.moment",
                    "date" to at.toLocalDate().format(pattern(strings, "date.format.day")),
                    "time" to at.format(pattern(strings, "date.format.time")),
                )
            }

            Edtf.Precision.DAY ->
                LocalDate.parse(body).format(pattern(strings, "date.format.day"))

            // The week reads by the day it starts on, which is how a person
            // says it, rather than by naming both ends.
            Edtf.Precision.WEEK -> LocalDate.parse(body.substringBefore('/'))
                .format(pattern(strings, "date.format.month_day"))

            Edtf.Precision.MONTH -> LocalDate.of(body.take(4).toInt(), body.drop(5).toInt(), 1)
                .format(pattern(strings, "date.format.month_year"))

            Edtf.Precision.SEASON -> strings(
                seasonKey(body.drop(5).toInt()),
                // A string rather than a number, so no locale groups the year
                // into "2,024".
                "year" to body.take(4),
            )

            Edtf.Precision.YEAR -> LocalDate.of(body.toInt(), 1, 1)
                .format(pattern(strings, "date.format.year"))

            // A range inside a range is not a thing the model can produce, and
            // rendering the stored string is more honest than guessing.
            Edtf.Precision.RANGE -> body
        }
    }

    /** The canonical string with its qualifier suffix removed. */
    private fun body(date: Edtf.Date): String = date.canonical.let {
        if (date.qualifier == Edtf.Qualifier.NONE) it else it.dropLast(1)
    }

    private fun seasonKey(season: Int): String = when (season) {
        21 -> "date.season.spring"
        22 -> "date.season.summer"
        23 -> "date.season.autumn"
        else -> "date.season.winter"
    }

    /**
     * The date pattern comes from the catalog because the shape of a date is
     * part of the translation, not a formatting detail applied to it.
     */
    private fun pattern(strings: Strings, key: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(strings[key], strings.locale)

    /**
     * The date as a masthead overline: "SUNDAY, 16 AUGUST". D170.
     *
     * **Localized through the platform rather than assembled here**, so the
     * order of weekday, day and month is whatever the reader's locale puts
     * them in, and Arabic gets Arabic numerals and month names for free. The
     * uppercase is the style's, applied by the caller's own type token rather
     * than baked into the string, because several scripts have no case at all
     * and forcing it there produces nothing.
     */
    fun masthead(strings: Strings, day: LocalDate): String {
        val locale = strings.locale
        val pattern = DateTimeFormatter.ofPattern(
            android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"),
            locale,
        )
        return day.format(pattern)
    }
}
