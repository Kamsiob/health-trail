package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The digest, against fixed vectors.
 *
 * These run on the JVM with no database and no composition, because the thing
 * that has to be right is the counting rule rather than the pixels. The same
 * vectors are what a second platform would have to reproduce, which is the
 * point of #15.
 */
class DigestTest {

    private fun change(
        table: String,
        rowId: String,
        op: String,
        at: Long,
    ) = Digest.Change(table = table, rowId = rowId, op = op, changedAt = at)

    private val visit = 1_000L

    @Test
    fun aFirstVisitHasNothingToReport() {
        assertTrue(Digest.since(emptyList(), since = 0).isEmpty)
    }

    @Test
    fun nothingWrittenSinceTheLastVisitReportsNothing() {
        val before = listOf(change("entry", "a", "insert", at = 900))
        assertTrue(Digest.since(before, since = visit).isEmpty)
    }

    @Test
    fun theBoundaryIsStrictSoAVisitNeverCountsItsOwnLastRowTwice() {
        val exactly = listOf(change("entry", "a", "insert", at = visit))
        assertTrue(
            "a change logged at the moment of the last visit was already on screen then",
            Digest.since(exactly, since = visit).isEmpty,
        )
    }

    @Test
    fun newThingsAreCountedWhereTheyLive() {
        val changes = listOf(
            change("entry", "e1", "insert", at = 1100),
            change("entry", "e2", "insert", at = 1200),
            change("person", "p1", "insert", at = 1300),
        )

        val summary = Digest.since(changes, since = visit)

        assertEquals(
            listOf(
                Digest.Added(Repository.Section.CARE_TEAM, 1),
                Digest.Added(Repository.Section.TRAIL, 2),
            ),
            summary.added,
        )
        assertEquals(3, summary.newThings)
    }

    @Test
    fun sectionsComeBackInTheNotebooksOrderRatherThanByHowBusyTheyWere() {
        // Care team is second in the notebook and the trail is sixth, so a week
        // with far more trail entries must not push the trail to the top.
        val changes = buildList {
            repeat(9) { add(change("entry", "e$it", "insert", at = 1100L + it)) }
            add(change("person", "p1", "insert", at = 2000))
        }

        val order = Digest.since(changes, since = visit).added.map { it.section }

        assertEquals(
            listOf(Repository.Section.CARE_TEAM, Repository.Section.TRAIL),
            order,
        )
    }

    @Test
    fun oneRowWrittenManyTimesIsOneCorrection() {
        val fussing = (1..4).map { change("entry", "e1", "update", at = 1000L + it * 10) }

        val summary = Digest.since(fussing, since = visit)

        assertEquals(1, summary.corrected)
        assertEquals(0, summary.newThings)
    }

    @Test
    fun aRowCreatedAndThenRemovedInTheSameSpanIsOnlyRemoved() {
        val changes = listOf(
            change("entry", "e1", "insert", at = 1100),
            change("entry", "e1", "update", at = 1200),
            change("entry", "e1", "delete", at = 1300),
        )

        val summary = Digest.since(changes, since = visit)

        assertEquals("it should not also be announced as new", 0, summary.newThings)
        assertEquals(0, summary.corrected)
        assertEquals(1, summary.removed)
    }

    @Test
    fun aRowThatWasCreatedBeforeAndCorrectedSinceIsACorrection() {
        val changes = listOf(
            change("entry", "e1", "insert", at = 500),
            change("entry", "e1", "update", at = 1500),
        )

        val summary = Digest.since(changes, since = visit)

        assertEquals(0, summary.newThings)
        assertEquals(1, summary.corrected)
    }

    @Test
    fun bookkeepingTablesAreLeftOutRatherThanCountedIntoSomething() {
        // A table with no section is either the app's own storage or something a
        // later schema added. Neither is a thing the person put anywhere.
        val changes = listOf(
            change("app_meta", "device_id", "insert", at = 1100),
            change("sync_peer", "x", "insert", at = 1200),
            change("entry", "e1", "insert", at = 1300),
        )

        val summary = Digest.since(changes, since = visit)

        assertEquals(listOf(Digest.Added(Repository.Section.TRAIL, 1)), summary.added)
    }

    @Test
    fun everySectionATableFeedsIsOneTheNotebookActuallyHas() {
        // Guards against a table being mapped to a section that was renamed,
        // which would compile and then quietly stop reporting.
        val tables = listOf(
            "entry", "person", "medication", "appointment", "chapter", "care_thread",
            "reading", "document", "bill", "standing_instruction", "question",
            "emergency_card", "emergency_contact", "project",
        )
        tables.forEach { table ->
            val section = Digest.sectionOf(table)
            assertTrue("$table maps to no section", section != null)
            assertTrue(
                "$table maps to a section the notebook does not have",
                section in Repository.Section.entries,
            )
        }
    }
}
