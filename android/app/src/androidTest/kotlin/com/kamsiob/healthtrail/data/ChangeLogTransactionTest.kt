package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The change log append happens inside the caller's transaction, not beside it.
 *
 * `check_schema.py` already proves the triggers fire and that a failing log
 * write rolls the data write back. It proves it against a plain SQLite database
 * built from `schema.sql`. **This proves the same thing through the path the app
 * actually uses**: SQLCipher, the Kotlin repository, and a real encrypted file
 * on the device.
 *
 * The difference matters because the two can disagree. A trigger that fires in
 * plain SQLite can be defeated by a wrapper that opens its own connection, by a
 * library that autocommits, or by a repository that starts a transaction the
 * trigger never sees. None of that is visible in the schema.
 *
 * **The failure this guards against is silent.** A write that succeeds while its
 * log entry does not leaves a record that looks complete and a digest that will
 * never mention it. Nobody notices until a peer syncs, or until somebody asks
 * what changed last week and the honest answer is missing.
 */
@RunWith(AndroidJUnit4::class)
class ChangeLogTransactionTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private suspend fun logCount(repository: Repository): Int =
        repository.changeLogSizeForTest()

    @Test
    fun oneInsertAppendsExactlyOneEntry() = runBlocking {
        val repository = Repository.open(context)
        val before = logCount(repository)

        repository.createSubject(displayName = "Change log subject")

        assertEquals("an insert did not append exactly one entry", before + 1, logCount(repository))
    }

    @Test
    fun anUpdateAndATombstoneEachAppendOne() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Change log subject")
        val entryId = repository.createEntry(subjectId = subjectId, kind = "call")

        val beforeUpdate = logCount(repository)
        repository.fileEntry(entryId, null)
        assertEquals(
            "an update did not append exactly one entry",
            beforeUpdate + 1,
            logCount(repository),
        )

        val beforeDelete = logCount(repository)
        repository.delete(Repository.Section.TRAIL, entryId)
        assertEquals(
            "a tombstone did not append exactly one entry",
            beforeDelete + 1,
            logCount(repository),
        )
    }

    @Test
    fun aWriteInsideAnOuterTransactionThatRollsBackLeavesNoOrphanEntry() = runBlocking {
        // The case the schema check cannot reach. A repository write nested
        // inside a transaction the caller later abandons must take its log
        // entry with it, or the log records something that never happened and
        // a peer is handed an event with no row behind it.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Change log subject")
        val before = logCount(repository)

        val doomed = repository.inAbandonedTransactionForTest {
            createEntry(subjectId = subjectId, kind = "call", title = "Never happened")
        }

        assertEquals(
            "the abandoned write left an orphan change log entry",
            before,
            logCount(repository),
        )
        assertFalse(
            "the abandoned write survived the rollback",
            repository.rowExistsForTest("entry", doomed),
        )
    }

    @Test
    fun theLogRecordsWhichRowAndWhichOperation() = runBlocking {
        // Not just that something happened. A peer asks "what changed after
        // sequence N" and needs the table, the row, and the operation to do
        // anything with the answer.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Change log subject")
        val entryId = repository.createEntry(subjectId = subjectId, kind = "call")

        val latest = repository.latestChangeForTest()
        assertEquals("entry", latest?.get("table_name"))
        assertEquals(entryId, latest?.get("row_id"))
        assertEquals("insert", latest?.get("op"))

        repository.delete(Repository.Section.TRAIL, entryId)
        val afterDelete = repository.latestChangeForTest()
        assertEquals(
            "setting deleted_at was not logged as a delete",
            "delete",
            afterDelete?.get("op"),
        )
    }

    @Test
    fun theLogIsLocalAndCarriesTheDeviceThatWrote() = runBlocking {
        // `seq` is meaningful only on the device that wrote it, which is why a
        // peer tracks the last sequence it received from each device rather
        // than a single global number. D12 records that the log is exported
        // and that the importer renumbers it.
        val repository = Repository.open(context)
        repository.createSubject(displayName = "Change log subject")

        val latest = repository.latestChangeForTest()
        assertTrue("the log entry has no device", (latest?.get("device_id") ?: "").isNotBlank())
        assertEquals(repository.deviceId(), latest?.get("device_id"))
    }
}
