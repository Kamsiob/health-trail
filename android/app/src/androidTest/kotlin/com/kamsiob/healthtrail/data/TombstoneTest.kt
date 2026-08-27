package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A deleted row is gone from every way the app can look at it.
 *
 * This is the data contract's first named failure and the one with the worst
 * shape: a forgotten `deleted_at IS NULL` does not crash, does not fail a
 * build, and does not show up until a thing somebody deleted reappears in a
 * search result, a chart, or an export, possibly months later, possibly in
 * front of someone else.
 *
 * `check_live_views.py` makes writing that query hard. This proves the views
 * behind it actually work, through SQLCipher and the real triggers rather than
 * against an in-memory copy of the schema.
 *
 * **Deletion is a tombstone, never a DELETE.** The second named impossibility
 * is a schema that removes rows: with the row gone there is nothing left to
 * tell a peer it was deleted, so the peer resurrects it on the next sync and
 * the deletion undoes itself forever.
 */
@RunWith(AndroidJUnit4::class)
class TombstoneTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    @Test
    fun aDeletedEntryLeavesEveryReadTheAppHas() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tombstone subject")

        val kept = repository.createEntry(
            subjectId = subjectId, kind = "call", title = "Kept",
        )
        val deleted = repository.createEntry(
            subjectId = subjectId, kind = "call", title = "Deleted", isUnfiled = true,
        )

        assertEquals(2, repository.count(Repository.Section.TRAIL, subjectId))
        assertEquals(1, repository.unfiled(subjectId).size)
        assertNotNull(repository.entryOccurred(deleted))

        repository.delete(Repository.Section.TRAIL, deleted)

        // Every read this class offers, one at a time. A view that filters in
        // one place and not another is the defect, so listing them is the test.
        assertEquals(
            "the count still sees a deleted entry",
            1,
            repository.count(Repository.Section.TRAIL, subjectId),
        )
        assertTrue(
            "the Unfiled tray still holds a deleted entry",
            repository.unfiled(subjectId).none { it.id == deleted },
        )
        // The kept entry was never unfiled, so its absence from the tray is
        // correct rather than a loss. Asserted so the tray is not quietly
        // sweeping in everything after a delete.
        assertEquals(
            "the tray picked up an entry that was never unfiled",
            0,
            repository.unfiled(subjectId).count { it.id == kept },
        )
        assertNull(
            "a deleted entry still reads its date",
            repository.entryOccurred(deleted),
        )
        assertNotNull("the kept entry disappeared too", repository.entryOccurred(kept))
    }

    @Test
    fun aDeletedThreadLeavesTheChipsAndTheLinks() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tombstone subject")
        val situation = TemplateCatalog.situations(context).all.first { it.threads.isNotEmpty() }
        repository.applySituation(
            subjectId = subjectId,
            templateId = situation.id,
            threads = situation.threads.map { it.id to it.label },
        )
        val thread = repository.threads(subjectId).first()

        val entryId = repository.createEntry(subjectId = subjectId, kind = "call")
        repository.linkEntryToThread(entryId, thread.id)
        assertTrue(repository.threadsForEntry(entryId).contains(thread.id))

        repository.delete(Repository.Section.THREADS, thread.id)

        assertTrue(
            "a deleted thread is still offered",
            repository.threads(subjectId).none { it.id == thread.id },
        )
        // The link row itself was not deleted. The join reads the thread through
        // its live view, so a deleted thread cannot resurface attached to an
        // entry, which is the case a link table makes easy to get wrong.
        assertFalse(
            "a deleted thread still reaches an entry through the link table",
            repository.threadsForEntry(entryId).contains(thread.id),
        )
    }

    @Test
    fun removingAReadingTombstonesTheMeasurementItActuallyIs() = runBlocking {
        // #471. `deleteReading` named a table called `reading`, and there is no
        // such table: readings are `measurement`. `execSQL` throws on that, so
        // the one control that takes back a reading typed twice failed rather
        // than working, and nothing exercised it.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Reading subject")
        val weight = TemplateCatalog.presets(context).first { it.id == "weight" }
        val measureId = repository.createMeasure(subjectId, weight, unit = "lb")
        val readingId = repository.recordMeasurement(
            measureId = measureId,
            number = 168.0,
            unit = "lb",
        )

        assertTrue(
            "the reading was not recorded",
            repository.readings(subjectId).any { it.id == readingId },
        )

        repository.deleteReading(readingId)

        assertTrue(
            "a removed reading is still offered",
            repository.readings(subjectId).none { it.id == readingId },
        )
        // allow-base-table: a tombstone is what this asserts, so the view
        // cannot answer it.
        assertTrue(
            "the reading was removed rather than tombstoned",
            repository.rowExistsForTest("measurement", readingId),
        )
    }

    @Test
    fun deletingIsATombstoneRatherThanARemoval() = runBlocking {
        // The row has to still be there, or there is nothing to tell another
        // device it was deleted and the deletion undoes itself on the next sync.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tombstone subject")
        val entryId = repository.createEntry(subjectId = subjectId, kind = "call")

        repository.delete(Repository.Section.TRAIL, entryId)

        // allow-base-table: the whole point of this assertion is that the row
        // survives in the base table after the view stops seeing it. Reading
        // the view here would prove nothing.
        val stillThere = repository.rowExistsForTest("entry", entryId)
        assertTrue("the row was removed rather than tombstoned", stillThere)
    }

    @Test
    fun deletingTwiceDoesNotBumpTheRevisionAgain() = runBlocking {
        // A second delete of an already deleted row is a no-op rather than a
        // new event. Without the guard it would append another change log entry
        // and hand a peer a deletion it has already applied.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tombstone subject")
        val entryId = repository.createEntry(subjectId = subjectId, kind = "call")

        repository.delete(Repository.Section.TRAIL, entryId)
        val first = repository.revisionForTest("entry", entryId)
        repository.delete(Repository.Section.TRAIL, entryId)
        val second = repository.revisionForTest("entry", entryId)

        assertEquals("deleting an already deleted row wrote again", first, second)
    }
}
