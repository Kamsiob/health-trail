package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Both versions of a conflicted row survive being written down and read back.
 *
 * **The schema's promise is that nothing is lost**: `conflict_log` keeps both
 * sides whole "so that nothing is lost and either can be restored by hand if
 * the person disagrees with the resolution". That promise is only as good as
 * this round trip, and the writer and the reader used to live in two different
 * files with nothing holding them together.
 *
 * **The values here are the ones that actually break a hand written encoder**,
 * and every one of them is a thing a care record really contains: a quote
 * around something somebody said, a Windows path with backslashes, a note typed
 * across several lines, an accented name, Arabic, and an empty string, which is
 * not the same as a column nobody filled.
 */
class RowJsonTest {

    private fun roundTrip(row: Map<String, String?>) {
        assertEquals(row, RowJson.read(RowJson.write(row)))
    }

    @Test
    fun `an ordinary row comes back exactly`() {
        roundTrip(
            mapOf(
                "id" to "01J8Z9K2QF",
                "body" to "They said someone would call back.",
                "chapter_id" to null,
            ),
        )
    }

    @Test
    fun `quoted speech survives, which is most of what a care record holds`() {
        roundTrip(mapOf("body" to """She said "nobody told me" and I wrote it down."""))
    }

    @Test
    fun `a backslash is not an escape when it is somebody's text`() {
        roundTrip(mapOf("original_location" to """C:\Users\Margaret\scan.pdf"""))
    }

    @Test
    fun `a note typed across several lines keeps its shape`() {
        roundTrip(mapOf("notes" to "First the call.\nThen the visit.\r\nThen nothing.\tAt all."))
    }

    @Test
    fun `an accent and a right to left script come back unchanged`() {
        roundTrip(mapOf("display_name" to "José Álvarez", "role_label" to "الممرضة المناوبة"))
    }

    @Test
    fun `null and the empty string stay two different things`() {
        // Section 8.2 keeps null, empty and zero as three different answers. A
        // field nobody filled is not a field somebody cleared.
        val row = mapOf("a" to null, "b" to "", "c" to "0")
        val back = RowJson.read(RowJson.write(row))
        assertEquals(row, back)
        assertEquals(null, back["a"])
        assertEquals("", back["b"])
    }

    @Test
    fun `a control character is escaped rather than written raw`() {
        // A raw control byte would make the stored JSON unparseable by anything
        // else, including a person looking at the database by hand.
        val written = RowJson.write(mapOf("body" to "before\u0001after"))
        assertTrue(written, written.contains("\\u0001"))
        assertEquals("before\u0001after", RowJson.read(written)["body"])
    }

    @Test
    fun `the same row always writes the same bytes`() {
        // Written in a different column order, so the sort is what makes this
        // true rather than luck.
        val one = linkedMapOf<String, String?>("b" to "2", "a" to "1", "c" to null)
        val two = linkedMapOf<String, String?>("c" to null, "a" to "1", "b" to "2")
        assertEquals(RowJson.write(one), RowJson.write(two))
    }

    @Test
    fun `something this did not write comes back empty rather than throwing`() {
        // This feeds a notice screen, and a notice screen that crashes is worse
        // than one that says less: the person is already looking at it because
        // something unexpected happened to their record.
        assertEquals(emptyMap<String, String?>(), RowJson.read(""))
        assertEquals(emptyMap<String, String?>(), RowJson.read("not json at all"))
    }
}
