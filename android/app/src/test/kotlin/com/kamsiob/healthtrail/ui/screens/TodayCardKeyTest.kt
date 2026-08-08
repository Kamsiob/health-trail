package com.kamsiob.healthtrail.ui.screens

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every Today card type the database allows has the catalog strings the screen
 * asks for by name.
 *
 * **This exists because the screen builds those keys at runtime.**
 * `TodayFieldScreen` asks for `"today.card.${card.type}"`, and
 * `check_string_keys.py` **deliberately skips a key built from a variable**: it
 * cannot be resolved by reading the source, so guessing would be worse than
 * declining. Its stated safety net is the instrumented suite rendering the
 * screen.
 *
 * **That net has a hole in it, and the hole was open on 2026-08-08.** The phone
 * was locked, the instrumented suite could not run at all, and a card type
 * added to the schema without its catalog entry would have shipped. It would
 * not fail quietly either: `Strings.resolve` throws in debug, so the first
 * person to be given that card gets a crash on the screen the app opens on.
 * That is exactly what `more.title` did to `ChangeSituationScreen`, which
 * passed seventeen checks, the compiler and lint before taking the app down on
 * the first tap.
 *
 * **So this holds the schema to the catalog directly**, both canonical files,
 * neither of them a copy, and it runs on the JVM in a second.
 */
class TodayCardKeyTest {

    private fun read(path: String): String {
        val file = File("../../$path")
        assertTrue(
            "$path was not found at ${file.absolutePath}. This test reads the " +
                "canonical files rather than copies of them.",
            file.isFile,
        )
        return file.readText()
    }

    /**
     * The card types the database will accept, read out of the `card_type`
     * check constraint.
     *
     * **From the schema rather than from a list written here**, because a list
     * written here is a second copy of the seventeen and the first thing to go
     * stale when an eighteenth arrives. The point of this test is to notice
     * that arrival.
     */
    private fun cardTypes(): List<String> {
        val schema = read("contract/schema.sql")
        val constraint = Regex("""card_type\s+TEXT\s+NOT NULL CHECK \(card_type IN \(([^)]*)\)""")
            .find(schema)
        assertTrue(
            "the card_type check constraint was not found in contract/schema.sql. " +
                "The schema changed shape and this reader did not, so everything " +
                "below is asserting things about an empty list.",
            constraint != null,
        )
        val types = Regex("""'([a-z_]+)'""")
            .findAll(constraint!!.groupValues[1])
            .map { it.groupValues[1] }
            .toList()
        assertEquals(
            "DESIGN.md 21.7 says seventeen card types and the schema lists " +
                "${types.size}. If the catalog genuinely changed, this number " +
                "changes with it and 21.7 changes too.",
            17,
            types.size,
        )
        return types
    }

    /** Every top level key in the English catalog. */
    private fun catalogKeys(): Set<String> {
        val line = Regex("""^\s{2}"((?:[^"\\]|\\.)*)"\s*:""")
        val keys = read("contract/i18n/en.json").lines()
            .mapNotNull { line.find(it)?.groupValues?.get(1) }
            .toSet()
        assertTrue("no keys were read out of en.json", keys.size > 100)
        return keys
    }

    @Test
    fun `every card type has the tab name the card renders`() {
        val keys = catalogKeys()
        val missing = cardTypes().filterNot { "today.card.$it" in keys }
        assertTrue(
            "these card types are accepted by the schema and have no " +
                "today.card.<type> in en.json, so a card of that type crashes " +
                "the app the first time Today renders it: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `every card type has the label the gallery offers it under`() {
        // The add-a-card gallery lists the catalog, so a type with no name
        // there is a row the person cannot be offered.
        val keys = catalogKeys()
        val missing = cardTypes().filterNot { "today.card.$it" in keys }
        assertTrue("card types with no name at all: $missing", missing.isEmpty())
    }

    @Test
    fun `the catalog has no today card key for a type the schema refuses`() {
        // The other direction. A `today.card.something` left behind after a type
        // was renamed is copy nobody can reach, and it reads to the next person
        // as a card that exists.
        val types = cardTypes().toSet()
        val suffixes = setOf(
            "open", "nothing", "unread", "source_closed", "more",
            "digest.new", "digest.quiet",
        )
        val stray = catalogKeys()
            .filter { it.startsWith("today.card.") }
            .map { it.removePrefix("today.card.") }
            .filterNot { it in suffixes }
            // A qualifier on a real type, such as `measure.long` or
            // `medications.count`, belongs to that type.
            .filterNot { key -> types.any { key == it || key.startsWith("$it.") } }
        assertTrue(
            "these catalog keys name a Today card type the schema does not " +
                "accept, so nothing can ever render them: $stray",
            stray.isEmpty(),
        )
    }
}
