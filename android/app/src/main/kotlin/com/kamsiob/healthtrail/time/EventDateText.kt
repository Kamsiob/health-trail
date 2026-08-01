package com.kamsiob.healthtrail.time

import com.kamsiob.healthtrail.i18n.Strings
import java.time.LocalDate
import java.time.LocalDateTime
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
}
