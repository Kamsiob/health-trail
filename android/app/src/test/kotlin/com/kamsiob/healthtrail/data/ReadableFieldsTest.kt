package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The field map, as the build generated it from `contract/readable-fields.json`.
 *
 * **Generated rather than parsed at runtime**, so the archive's field order is
 * fixed at build time rather than at the mercy of whichever JSON implementation
 * is present. JSON does not guarantee object key order, and 8.5's regeneration
 * test rests on the output being byte identical across runs.
 *
 * That also means these assertions run against the real contract file rather
 * than a fixture. A change to it that broke the renderer shows up here, not on
 * somebody's phone in a year.
 */
class ReadableFieldsTest {

    private val parsed = ReadableFieldMap.tables

    @Test
    fun `every table in the contract is present`() {
        assertTrue(parsed.containsKey("entry"))
        assertTrue(parsed.containsKey("person"))
        assertTrue(parsed.containsKey("incident"))
        assertTrue(parsed.containsKey("attachment"))
    }

    @Test
    fun `a column the contract says not to render is absent, not merely flagged`() {
        // The renderer walks what this returns, so a not-rendered column being
        // absent is what makes it impossible to print by accident. D90: the rule
        // is enforced by there being nothing to call.
        val entry = parsed.getValue("entry").rendered.map { it.first }
        for (bookkeeping in listOf("created_at", "updated_at", "deleted_at", "origin_device", "rev")) {
            assertFalse("$bookkeeping should not be renderable", entry.contains(bookkeeping))
        }
    }

    @Test
    fun `the id renders, because both halves of the archive have to be matchable by hand`() {
        // Section 8.2. Every entry prints its id, and it is the same id as in
        // data/trail.sqlite.
        val entry = parsed.getValue("entry").rendered
        assertTrue(entry.any { it.first == "id" && it.second == "id" })
    }

    @Test
    fun `an event date renders as a date and its zone as part of that date`() {
        val entry = parsed.getValue("entry").rendered.toMap()
        assertEquals("date", entry["occurred_edtf"])
        assertEquals("dateZone", entry["occurred_zone"])
    }

    @Test
    fun `derived index columns do not render`() {
        // contract/DATA-CONTRACT.md 3.1: occurred_start and occurred_end are an
        // index recomputed from the EDTF string on import, not a second source
        // of truth, and printing them would show a reader two dates for one
        // event.
        val entry = parsed.getValue("entry").rendered.map { it.first }
        assertFalse(entry.contains("occurred_start"))
        assertFalse(entry.contains("occurred_end"))
    }

    @Test
    fun `a foreign key renders as a link rather than a raw value`() {
        val entry = parsed.getValue("entry").rendered.toMap()
        assertEquals("link", entry["chapter_id"])
    }

    @Test
    fun `internal tables render nothing at all`() {
        for (internal in listOf("app_meta", "device", "schema_migration", "change_log")) {
            val rendered: List<Pair<String, String>> = parsed[internal]?.rendered.orEmpty()
            assertEquals("$internal should render nothing", 0, rendered.size)
        }
    }

    @Test
    fun `the render order is explicit rather than incidental`() {
        // The renderer walks this order and the archive has to be byte identical
        // across runs. It comes from an "order" array in the contract file, so
        // it cannot change because a parser iterated an object differently.
        val entry = parsed.getValue("entry").rendered.map { it.first }
        assertEquals("id", entry.first())
        assertTrue(entry.indexOf("occurred_edtf") < entry.indexOf("body"))
    }

    @Test
    fun `the person's own words are rendered`() {
        val entry = parsed.getValue("entry").rendered.map { it.first }
        assertTrue(entry.contains("title"))
        assertTrue(entry.contains("body"))
    }
}
