package com.kamsiob.healthtrail.time

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every `date.format.*` pattern in every catalog, compiled and used.
 *
 * **A date pattern is a translated string that is also code**, which is the
 * unusual thing about it and the reason this exists. `EventDateText` builds a
 * `DateTimeFormatter` out of whatever the catalog holds, because "November 18,
 * 2024" and "2024年11月18日" are not one shape with different words in it. That
 * means a translator can write something that throws, and it throws **only in
 * that language**, on a screen nobody in this project reads by default.
 *
 * **Nothing else could catch it.** `check_i18n.py` holds the four catalogs to
 * each other, so four patterns that are equally wrong all pass. The Kotlin
 * compiler never sees the string. Lint never sees it. The instrumented suite
 * runs in English unless a test asks otherwise, and the Arabic and Chinese
 * sweeps are done by eye on a device. This runs on the JVM in seconds and needs
 * no phone, which is what makes it a check rather than a hope.
 *
 * **It reads the canonical catalogs from `/contract` directly**, the same way
 * `SchemaStatementSplitTest` reads the canonical schema. There is one copy of
 * each in this repository and this is held to it, not to a duplicate.
 */
class DateFormatPatternTest {

    private val locales = listOf("en", "es", "zh", "ar")

    /**
     * A moment with every field a pattern here could ask for.
     *
     * **The 9th and not the 1st**, so a pattern that silently drops the day is
     * visible: a formatted string with no `9` in it did not render the day. And
     * a time, because `date.format.time` needs one and a `LocalDate` would
     * throw for a reason that has nothing to do with the pattern being wrong.
     */
    private val moment: LocalDateTime = LocalDateTime.of(2026, 4, 9, 15, 7)

    /**
     * The catalog's `date.format.*` entries, read as text.
     *
     * **Not through `org.json`**, which on the unit test classpath is the
     * Android stub and throws "Method keys in org.json.JSONObject not mocked"
     * for every call. Robolectric would fix that and would cost an Android
     * runtime to check four dozen strings. These catalogs are flat, one key per
     * line, so a line reader is the whole parser needed and it has the same
     * shape as the one `check_i18n.py` uses for the duplicate key check.
     */
    private fun patterns(code: String): Map<String, String> {
        val file = File("../../contract/i18n/$code.json")
        assertTrue(
            "contract/i18n/$code.json was not found at ${file.absolutePath}. " +
                "This test reads the canonical catalogs rather than a copy.",
            file.isFile,
        )
        val line = Regex("""^\s{2}"(date\.format\.[^"]+)"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val found = file.readLines().mapNotNull { line.find(it) }.associate {
            it.groupValues[1] to it.groupValues[2].replace("\\\"", "\"")
        }
        // **A reader that finds nothing makes every assertion below pass.**
        // That is the failure mode of any check written against a file format,
        // and it is silent: reformat the catalog and three green tests keep
        // saying yes about nothing at all.
        assertTrue(
            "no date patterns were read out of $code.json. The catalog format " +
                "changed and this reader did not, so the checks below are " +
                "asserting things about an empty map.",
            found.isNotEmpty(),
        )
        return found
    }

    @Test
    fun `every date pattern compiles and formats in its own language`() {
        val failures = mutableListOf<String>()

        for (code in locales) {
            val locale = Locale.forLanguageTag(code)
            for ((key, pattern) in patterns(code)) {
                val rendered = runCatching {
                    DateTimeFormatter.ofPattern(pattern, locale).format(moment)
                }.getOrElse { error ->
                    failures += "$code.json $key is not a usable pattern: " +
                        "${pattern.let { "\"$it\"" }} threw ${error::class.simpleName}, " +
                        "${error.message}. This would throw on every screen showing " +
                        "a date in $code and nowhere else."
                    continue
                }
                if (rendered.isBlank()) {
                    failures += "$code.json $key formatted to nothing"
                }
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every catalog carries the same date patterns`() {
        // A pattern present in three catalogs and missing from the fourth is a
        // crash in the fourth, because `EventDateText` asks for the key by name
        // and `Strings.resolve` throws in debug rather than falling back
        // silently. `check_i18n.py` holds the whole catalogs to each other, and
        // this states the same thing about the subset that is executable, so a
        // failure here says what it means.
        val expected = patterns("en").keys
        for (code in locales) {
            val got = patterns(code).keys
            assertTrue(
                "$code.json is missing date patterns ${expected - got}",
                (expected - got).isEmpty(),
            )
            assertTrue(
                "$code.json has date patterns English does not: ${got - expected}",
                (got - expected).isEmpty(),
            )
        }
    }

    @Test
    fun `every pattern reads back the date it was given`() {
        // **The trap here is a pattern that compiles and is still wrong**, which
        // is the likelier defect by far. Spanish needs the word "de" between
        // the day and the month and it has to be quoted: unquoted, `d de MMMM
        // de yyyy` is day, then localized day-of-week, then month, then
        // day-of-week again, then year. It throws nothing. It renders a string
        // with the right digits in it. It is not a date anybody wrote.
        //
        // **A round trip does not catch it, which was worth finding out.** The
        // broken pattern renders `9 95 abril 95 2026` and parses straight back
        // to 2026-04-09, because the day-of-week digits it invented are
        // consistent with the date they came from. The output is nonsense and
        // every field agrees with itself.
        //
        // **What gives it away is the 95.** So the round trip stays, because a
        // pattern that cannot read its own output is broken a different way,
        // and the check that does the work is the one below it: every run of
        // digits in the rendered string must be a value this date actually has.
        val fields = listOf(
            ChronoField.YEAR to moment.year,
            ChronoField.MONTH_OF_YEAR to moment.monthValue,
            ChronoField.DAY_OF_MONTH to moment.dayOfMonth,
            ChronoField.HOUR_OF_DAY to moment.hour,
            ChronoField.MINUTE_OF_HOUR to moment.minute,
        )
        val failures = mutableListOf<String>()

        for (code in locales) {
            val locale = Locale.forLanguageTag(code)
            for ((key, pattern) in patterns(code)) {
                val formatter = DateTimeFormatter.ofPattern(pattern, locale)
                val rendered = formatter.format(moment)
                val parsed = runCatching { formatter.parse(rendered) }.getOrElse { error ->
                    failures += "$code.json $key rendered \"$rendered\" and cannot " +
                        "read it back: ${error::class.simpleName}. A pattern that " +
                        "cannot parse its own output is rendering something other " +
                        "than the date. Pattern: \"$pattern\""
                    continue
                }
                for ((field, expected) in fields) {
                    if (!parsed.isSupported(field)) continue
                    val got = parsed.getLong(field).toInt()
                    if (got != expected) {
                        failures += "$code.json $key rendered \"$rendered\", which " +
                            "reads back with $field = $got rather than $expected. " +
                            "Pattern: \"$pattern\""
                    }
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `no pattern renders a number this date does not have`() {
        // **The check that catches an unquoted word.** Spanish "de" left
        // unquoted is `d` day and `e` localized day-of-week, so the pattern
        // renders `9 95 abril 95 2026`: it compiles, it round trips, and it
        // shows somebody a 95 in the middle of their date. Every digit in a
        // rendered date must be a value this moment actually has, and nothing
        // in these patterns has any business printing a weekday as a number.
        //
        // **Proved by unquoting the Spanish "de" in `es.json` and watching this
        // fail**, then restoring the catalog by copy from the scratchpad rather
        // than with git, per the note in `HANDOFF.md` section 7.
        val allowed = buildSet {
            for (value in listOf(
                moment.year, moment.year % 100, moment.monthValue,
                moment.dayOfMonth, moment.hour, moment.hour % 12,
                moment.minute, moment.second,
            )) {
                add(value.toString())
                add(value.toString().padStart(2, '0'))
            }
        }
        val digits = Regex("""\d+""")
        val failures = mutableListOf<String>()

        for (code in locales) {
            val locale = Locale.forLanguageTag(code)
            for ((key, pattern) in patterns(code)) {
                val rendered = DateTimeFormatter.ofPattern(pattern, locale).format(moment)
                val stray = digits.findAll(rendered).map { it.value }.filterNot { it in allowed }
                for (run in stray) {
                    failures += "$code.json $key rendered \"$rendered\", which " +
                        "contains $run. This moment has no such number, so a " +
                        "pattern letter is being read where a literal was meant. " +
                        "Words inside a pattern have to be quoted. Pattern: \"$pattern\""
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
