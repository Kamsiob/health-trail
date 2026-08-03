package com.kamsiob.healthtrail.ui.screens

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * That the app, the schema, and all four catalogs agree on the six words.
 *
 * **Three layers each held their own list and one of them was written from
 * memory.** `medication_event.kind` carries a CHECK constraint naming exactly
 * six values. The picker offered `restarted`, which the schema calls `resumed`,
 * so that choice wrote a value the table rejects. The catalogs matched the
 * picker rather than the schema, and none of the four defined `noted` at all,
 * so an event of that kind crashed the medication screen outright.
 *
 * **None of it was reachable until the fixture generator produced a medication
 * with a history**, which it learned to do the same night this was found. Three
 * layers agreeing with each other and disagreeing with the database is exactly
 * the shape of defect a test that composes its own data cannot see, per
 * `TESTING-PERSONAS.md` section 7.
 *
 * So this reads the constraint out of `contract/schema.sql` rather than
 * restating it. A test that repeats the list it is checking is a fourth copy of
 * the same mistake.
 */
class MedicationEventKindTest {

    private val root = File("../..").canonicalFile

    private fun kindsFromSchema(): List<String> {
        val sql = File(root, "contract/schema.sql").readText()
        val table = sql.substringAfter("CREATE TABLE IF NOT EXISTS medication_event")
            .substringBefore("\n);")
        val check = Regex("""kind\s+TEXT\s+NOT NULL CHECK \(kind IN \(([^)]*)\)""")
            .find(table)
        requireNotNull(check) { "the kind CHECK constraint moved or changed shape" }
        return Regex("'([a-z_]+)'").findAll(check.groupValues[1])
            .map { it.groupValues[1] }
            .toList()
    }

    @Test
    fun theSchemaStillNamesSixKinds() {
        val kinds = kindsFromSchema()
        assertEquals("the schema changed; every list below has to follow it", 6, kinds.size)
        assertTrue(kinds.contains("resumed"))
        assertTrue(kinds.contains("noted"))
    }

    @Test
    fun thepickerOffersOnlyWordsTheDatabaseAccepts() {
        val kinds = kindsFromSchema().toSet()
        MedicationChange.entries.forEach { option ->
            assertTrue(
                "the picker writes '${option.stored}', which the CHECK constraint rejects",
                kinds.contains(option.stored),
            )
        }
    }

    @Test
    fun everyKindTheSchemaAllowsCanBeChosen() {
        // The other direction. A kind the database accepts but the picker never
        // offers is a state a person can arrive at and cannot record.
        val offered = MedicationChange.entries.map { it.stored }.toSet()
        kindsFromSchema().forEach {
            assertTrue("the schema allows '$it' and nothing offers it", offered.contains(it))
        }
    }

    /**
     * The catalogs read as text rather than as JSON.
     *
     * `org.json` on the unit test classpath is Android's stub and throws on
     * every call, and pulling in a parser to check that a key exists would be a
     * dependency carried for one assertion. The catalogs are generated with one
     * key per line, and `check_i18n.py` is what actually validates their shape.
     */
    private fun defines(locale: String, key: String): String? {
        val text = File(root, "contract/i18n/$locale.json").readText()
        val match = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(text)
        return match?.groupValues?.get(1)
    }

    @Test
    fun allFourCatalogsNameEveryKind() {
        val kinds = kindsFromSchema()
        listOf("en", "es", "zh", "ar").forEach { locale ->
            kinds.forEach { kind ->
                val key = medicationEventKey(kind)
                val value = defines(locale, key)
                assertNotNull(
                    "$locale has no $key, so a '$kind' event would throw on that screen",
                    value,
                )
                assertTrue("$locale defines $key as blank", value!!.isNotBlank())
            }
        }
    }

    @Test
    fun akindFromAlaterVersionFallsBackRatherThanThrowing() {
        // An imported notebook written by a newer build. The screen must render
        // something rather than take the app down, which is what interpolating
        // the key straight from the column did.
        assertNotNull(defines("en", medicationEventKey("something_invented_later")))
    }
}
