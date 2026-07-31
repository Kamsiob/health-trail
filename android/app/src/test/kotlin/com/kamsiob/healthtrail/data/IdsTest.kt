package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ids are the one thing in this schema that cannot be fixed later.
 *
 * Two devices both creating row 47 has no correct merge, and repairing it after
 * real data exists means reassigning every id and every foreign key on
 * someone's records. So these are not incidental properties: each test here
 * corresponds to a way a future sync would break.
 */
class IdsTest {

    @Test
    fun `ids are unique across a large tight batch`() {
        val seen = HashSet<String>()
        repeat(200_000) {
            assertTrue("duplicate id generated", seen.add(Ids.new()))
        }
        assertEquals(200_000, seen.size)
    }

    @Test
    fun `ids generated in order sort in that order`() {
        // The trail is chronological, so ids sorting out of generation order
        // would show two entries written in the same second in an order that
        // changes between queries.
        val generated = List(50_000) { Ids.new() }
        assertEquals(
            "ids do not sort in the order they were generated",
            generated,
            generated.sorted(),
        )
    }

    @Test
    fun `ids stay ordered within a single millisecond`() {
        // Every id here is stamped with the same millisecond, so ordering can
        // only come from the sequence bits.
        val fixed = 1_753_977_600_000L
        val generated = List(3_000) { Ids.new(fixed) }
        assertEquals(generated, generated.sorted())
        assertEquals("ids collided inside one millisecond", 3_000, generated.toSet().size)
    }

    @Test
    fun `ids never go backward when the clock does`() {
        // A timezone change or a manual clock correction mid session moves the
        // clock backward. If ids followed it, yesterday's call would file after
        // today's, and the person would see their own trail reorder itself.
        val later = Ids.new(1_753_977_600_000L)
        val earlier = Ids.new(1_753_977_500_000L)
        assertTrue(
            "an id generated after a backward clock jump sorted before its predecessor",
            earlier > later,
        )
    }

    @Test
    fun `ids carry the time so they sort by creation across days`() {
        val old = Ids.new(1_700_000_000_000L)
        val new = Ids.new(1_753_977_600_000L)
        assertTrue("an older id did not sort before a newer one", old < new)
    }

    @Test
    fun `ids are well formed version 7 uuids`() {
        val pattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        repeat(1_000) {
            val id = Ids.new()
            assertTrue("not a well formed version 7 uuid: $id", pattern.matches(id))
        }
    }

    @Test
    fun `device ids differ between generations`() {
        // The device id is the deterministic tiebreaker when two versions of a
        // row carry identical timestamps, so two installs sharing one would
        // make that tiebreak meaningless.
        assertNotEquals(Ids.newDeviceId(), Ids.newDeviceId())
    }
}
