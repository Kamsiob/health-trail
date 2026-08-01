package com.kamsiob.healthtrail.time

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.Strings
import java.time.ZoneId
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The golden date vectors, `contract/test-vectors/dates.json`.
 *
 * **This runs the shared file rather than a copy.** The build syncs
 * `contract/test-vectors` into assets, so what is asserted here is the same
 * bytes the web app will assert against. A vector kept in this source tree
 * would be a second opinion about correct, which is exactly what a shared
 * vector exists to prevent.
 *
 * The failure it is here to catch is one line long: a renderer that turns
 * `2024-11` into "November 1, 2024". That is the app claiming a day the person
 * never gave, in a record that may be read years later by someone deciding
 * something.
 *
 * Instrumented rather than a JVM test only because `Strings` loads its catalogs
 * out of assets. Nothing else here needs a device.
 */
@RunWith(AndroidJUnit4::class)
class DateVectorTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val vector: JSONObject by lazy {
        JSONObject(
            context.assets.open("contract/test-vectors/dates.json")
                .bufferedReader().use { it.readText() }
        )
    }

    private val zone: ZoneId by lazy {
        ZoneId.of(vector.getJSONObject("_meta").getString("zone"))
    }

    private fun cases(): List<JSONObject> {
        val array = vector.getJSONArray("cases")
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    @Test
    fun theVectorItselfIsNotEmpty() {
        // A vector file that stopped being copied into assets would make every
        // other test here pass by having nothing to check.
        assertTrue("no cases in dates.json", cases().size >= 11)
    }

    @Test
    fun everyCaseParsesToTheStatedPrecision() {
        cases().forEach { case ->
            val edtf = case.getString("edtf")
            val parsed = Edtf.parse(edtf)
            assertNotNull("${case.getString("id")}: $edtf did not parse", parsed)
            assertEquals(
                "${case.getString("id")}: wrong precision for $edtf",
                case.getString("precision"),
                parsed!!.precision.name,
            )
        }
    }

    @Test
    fun everyCaseResolvesToTheStatedRange() {
        cases().forEach { case ->
            val id = case.getString("id")
            val parsed = requireNotNull(Edtf.parse(case.getString("edtf")))
            val range = Edtf.resolve(parsed, zone)

            val start = if (case.isNull("start")) null else case.getLong("start")
            val end = if (case.isNull("end")) null else case.getLong("end")

            assertEquals("$id: wrong range start", start, range.start)
            assertEquals("$id: wrong range end", end, range.end)
        }
    }

    @Test
    fun everyCaseRendersAsTheVectorSaysInAllFourLocales() {
        val failures = mutableListOf<String>()

        cases().forEach { case ->
            val id = case.getString("id")
            val parsed = requireNotNull(Edtf.parse(case.getString("edtf")))
            val expected = case.getJSONObject("render")

            expected.keys().forEach { locale ->
                val strings = Strings.load(context, Locale.forLanguageTag(locale))
                val actual = EventDateText.render(strings, parsed)
                if (actual != expected.getString(locale)) {
                    failures += "$id [$locale]: expected \"${expected.getString(locale)}\", got \"$actual\""
                }
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun nothingCoarserThanADayEverRendersADayNumber() {
        // The specific fabrication this model exists to prevent, asserted
        // directly rather than only through the expected strings. A month, a
        // season, and a year must not name a day, in any locale.
        val coarse = cases().filter {
            it.getString("precision") in setOf("MONTH", "SEASON", "YEAR")
        }
        assertTrue("the vector covers no coarse precisions", coarse.isNotEmpty())

        val failures = mutableListOf<String>()
        coarse.forEach { case ->
            val id = case.getString("id")
            val parsed = requireNotNull(Edtf.parse(case.getString("edtf")))
            listOf("en", "es", "ar", "zh").forEach { locale ->
                val strings = Strings.load(context, Locale.forLanguageTag(locale))
                val rendered = EventDateText.render(strings, parsed)
                val dayOfMonth = requireNotNull(Edtf.resolve(parsed, zone).start)
                val firstDay = java.time.Instant.ofEpochMilli(dayOfMonth)
                    .atZone(zone).toLocalDate()
                // "1" appearing in "2024" is not a day number, so the check is
                // for the day rendered the way a date would render it.
                val asDate = EventDateText.render(strings, Edtf.day(firstDay))
                if (rendered == asDate) {
                    failures += "$id [$locale]: rendered as its first day, $rendered"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun anUnreadableStringRendersAsItselfRatherThanAsAGuess() {
        // What an export from a later version of the app carries. The entry is
        // still the person's record and must stay readable.
        val strings = Strings.load(context, Locale.ENGLISH)
        assertEquals("Y170000002", EventDateText.render(strings, "Y170000002"))
    }
}
