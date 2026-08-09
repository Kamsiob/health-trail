package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The merge rules. `contract/DATA-CONTRACT.md` section 8.3.
 *
 * **These are the assertions that decide whose version of somebody's note
 * survives**, which is why they are unit tests rather than something a person
 * checks on a phone: the failure is silent by construction. A merge that keeps
 * the older copy produces a notebook that opens, looks complete, and is wrong,
 * and nobody finds out until they go looking for something they wrote.
 */
class MergeTest {

    private fun row(
        id: String,
        updatedAt: Long = 1_000L,
        device: String = "AAAA",
        body: String? = "what was written",
        deletedAt: Long? = null,
        chapter: String? = null,
    ): Map<String, String?> = mapOf(
        "id" to id,
        "updated_at" to updatedAt.toString(),
        "origin_device" to device,
        "body" to body,
        "deleted_at" to deletedAt?.toString(),
        "chapter_id" to chapter,
    )

    private val noReferences = emptyMap<String, Map<String, String>>()

    private fun plan(
        local: Map<String, List<Map<String, String?>>> = emptyMap(),
        incoming: Map<String, List<Map<String, String?>>> = emptyMap(),
        references: Map<String, Map<String, String>> = noReferences,
    ) = Merge.plan(local, incoming, references)

    @Test
    fun `a row this phone has never seen is inserted`() {
        val result = plan(incoming = mapOf("entry" to listOf(row("e1"))))
        assertEquals(1, result.insertCount)
        assertEquals(0, result.updateCount)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `an identical row is neither written nor reported as a conflict`() {
        // The overwhelming majority of a merge. Reporting these would bury the
        // handful that matter under a thousand that do not.
        val same = row("e1")
        val result = plan(
            local = mapOf("entry" to listOf(same)),
            incoming = mapOf("entry" to listOf(same)),
        )
        assertEquals(0, result.insertCount)
        assertEquals(0, result.updateCount)
        assertTrue(result.conflicts.isEmpty())
        assertEquals(1, result.unchanged)
    }

    @Test
    fun `the later version wins, whichever side it is on`() {
        val incomingNewer = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 100, body = "old"))),
            incoming = mapOf("entry" to listOf(row("e1", updatedAt = 200, body = "new"))),
        )
        assertEquals(1, incomingNewer.updateCount)
        assertEquals("incoming", incomingNewer.conflicts.single().winner)
        assertEquals("new", incomingNewer.updates.getValue("entry").single()["body"])

        val localNewer = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 300, body = "mine"))),
            incoming = mapOf("entry" to listOf(row("e1", updatedAt = 200, body = "theirs"))),
        )
        assertEquals("nothing is written when this phone already has the later version",
            0, localNewer.updateCount)
        assertEquals("local", localNewer.conflicts.single().winner)
    }

    @Test
    fun `a version that lost is still reported, and both sides are kept whole`() {
        // The whole argument for the conflict log. A record keeping app that
        // quietly eats an entry has failed at its one job, so the person is
        // told a second version existed even when this phone's copy won.
        val result = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 300, body = "mine"))),
            incoming = mapOf("entry" to listOf(row("e1", updatedAt = 200, body = "theirs"))),
        )
        val conflict = result.conflicts.single()
        assertEquals("mine", conflict.local["body"])
        assertEquals("theirs", conflict.incoming["body"])
        assertEquals(Merge.Reason.NEWER, conflict.reason)
    }

    @Test
    fun `two versions written in the same millisecond are resolved the same way on both phones`() {
        // Not a coin toss. If the two devices disagreed here the notebooks
        // would diverge permanently and each would think it was right.
        val forward = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 100, device = "AAAA"))),
            incoming = mapOf("entry" to listOf(row("e1", updatedAt = 100, device = "BBBB"))),
        )
        val backward = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 100, device = "BBBB"))),
            incoming = mapOf("entry" to listOf(row("e1", updatedAt = 100, device = "AAAA"))),
        )
        assertEquals("BBBB", forward.updates.getValue("entry").single()["origin_device"])
        assertEquals("the same pair resolved the other way round must keep the same row",
            0, backward.updateCount)
        assertEquals(Merge.Reason.SAME_TIME, forward.conflicts.single().reason)
    }

    @Test
    fun `a deletion travels as a tombstone and is applied like any other row`() {
        // The schema has no hard deletes precisely so this needs no special
        // case. A merge that ignored tombstones would put back what somebody
        // removed, on the phone they removed it from.
        val result = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 100, deletedAt = null))),
            incoming = mapOf("entry" to listOf(row("e1", updatedAt = 200, deletedAt = 250))),
        )
        assertEquals("250", result.updates.getValue("entry").single()["deleted_at"])
    }

    @Test
    fun `a row only this phone has is left alone, because merge never deletes`() {
        // The other phone never saw it. That is not the same as somebody
        // removing it, and only one of those is a thing the file can say.
        val result = plan(
            local = mapOf("entry" to listOf(row("mine"))),
            incoming = mapOf("entry" to emptyList()),
        )
        assertEquals(0, result.insertCount)
        assertEquals(0, result.updateCount)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `a row pointing at a parent nobody has stops the merge and names it`() {
        val result = plan(
            incoming = mapOf("entry" to listOf(row("e1", chapter = "missing-chapter"))),
            references = mapOf("entry" to mapOf("chapter_id" to "chapter")),
        )
        assertFalse("a merge that cannot place a row must not run", result.canApply)
        val dangling = result.dangling.single()
        assertEquals("entry", dangling.table)
        assertEquals("chapter_id", dangling.column)
        assertEquals("chapter", dangling.parentTable)
        assertEquals("missing-chapter", dangling.missingId)
    }

    @Test
    fun `a parent arriving in the same file resolves, and so does one already here`() {
        val fromTheFile = plan(
            incoming = mapOf(
                "chapter" to listOf(row("c1")),
                "entry" to listOf(row("e1", chapter = "c1")),
            ),
            references = mapOf("entry" to mapOf("chapter_id" to "chapter")),
        )
        assertTrue(fromTheFile.dangling.toString(), fromTheFile.canApply)

        val alreadyHere = plan(
            local = mapOf("chapter" to listOf(row("c1"))),
            incoming = mapOf("entry" to listOf(row("e1", chapter = "c1"))),
            references = mapOf("entry" to mapOf("chapter_id" to "chapter")),
        )
        assertTrue(alreadyHere.dangling.toString(), alreadyHere.canApply)
    }

    @Test
    fun `an empty reference is not a dangling one`() {
        // Most of these columns are optional and "not filed under a chapter" is
        // a state the app offers deliberately, per rule 13.
        val result = plan(
            incoming = mapOf("entry" to listOf(row("e1", chapter = null))),
            references = mapOf("entry" to mapOf("chapter_id" to "chapter")),
        )
        assertTrue(result.canApply)
    }

    @Test
    fun `the tables that are not merged are named rather than quietly skipped`() {
        val result = plan(
            incoming = mapOf(
                "entry" to listOf(row("e1")),
                "change_log" to listOf(mapOf("id" to "x")),
                "device" to listOf(mapOf("id" to "y")),
            ),
        )
        assertEquals(setOf("change_log", "device"), result.skipped.keys)
        assertTrue(result.skipped.values.all { it.isNotBlank() })
        assertEquals("only the entry is merged", 1, result.insertCount)
    }

    @Test
    fun `the same two notebooks always produce the same plan`() {
        // A plan that depends on map iteration order is a merge nobody can
        // reason about after it has run.
        val local = mapOf("entry" to listOf(row("b", updatedAt = 100), row("a", updatedAt = 100)))
        val incoming = mapOf(
            "person" to listOf(row("p1")),
            "entry" to listOf(row("a", updatedAt = 200), row("b", updatedAt = 50)),
        )
        assertEquals(
            Merge.plan(local, incoming, noReferences).toString(),
            Merge.plan(local, incoming, noReferences).toString(),
        )
        val ids = Merge.plan(local, incoming, noReferences).conflicts.map { it.rowId }
        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun `nothing is invented, so a winning row is written exactly as it arrived`() {
        // No new ids, no refreshed timestamps, no defaults filling a null.
        val arriving = row("e1", updatedAt = 900, body = null, chapter = null)
        val result = plan(
            local = mapOf("entry" to listOf(row("e1", updatedAt = 100))),
            incoming = mapOf("entry" to listOf(arriving)),
        )
        assertEquals(arriving, result.updates.getValue("entry").single())
    }
}
