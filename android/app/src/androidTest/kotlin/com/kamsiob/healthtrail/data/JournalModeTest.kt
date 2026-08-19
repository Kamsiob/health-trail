package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Which journal mode is actually live on the phone. #408.
 *
 * **This asserts the read back value, not the requested one.** The defect it
 * covers is a pragma that was declared in the contract, set through a path that
 * could not apply it, and never once checked: `applySchema` runs its pragmas
 * inside a transaction, SQLite refuses a journal mode change inside one and
 * answers with the mode it is already in, and that answer was discarded. So a
 * test that asserted "we asked for WAL" would have passed for the whole time
 * the bug existed.
 *
 * **Why it matters beyond tidiness.** Rollback journal at `synchronous =
 * NORMAL` does not fsync the journal before overwriting pages, so a power loss
 * mid commit can tear the file. `DatabaseKey` and `Backup` both already delete
 * `-wal` and `-shm`, which is to say the rest of the code believed this was on.
 */
@RunWith(AndroidJUnit4::class)
class JournalModeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    @Test
    fun writeAheadLoggingIsLiveOnThisPhone() {
        runBlocking { HealthTrailDatabase.open(context) }
        val mode = HealthTrailDatabase.journalMode
        assertNotNull("opening the database must record what the mode became", mode)
        assertEquals(
            "the contract declares WAL and this is the value SQLite reports, " +
                "synchronous = ${mode?.synchronous}",
            "wal",
            mode?.journal?.lowercase(),
        )
        assertTrue(mode!!.isWal)
    }

    /**
     * More than one connection can actually be opened. #408, #457.
     *
     * **This is the test that caught the real defect.** Write ahead logging
     * raises the connection pool's cap from one to several, and the driver's
     * configuration holds the passphrase array by reference rather than copying
     * it, so wiping that array after the first open left every later connection
     * opening against a passphrase of all zeroes. The failure it produced was
     * `SQLiteNotADatabaseException: file is not a database`, which is to say
     * turning on write ahead logging naively made a working notebook report
     * itself corrupt.
     *
     * Concurrent readers are what force the pool to grow, so that is what this
     * does. On one connection it passes whether or not the bug is present.
     */
    @Test
    fun thePoolCanOpenMoreThanOneConnection() {
        val database = runBlocking { HealthTrailDatabase.open(context) }
        runBlocking {
            val readers = (0 until 8).map {
                async(kotlinx.coroutines.Dispatchers.IO) {
                    database.database.rawQuery(
                        "SELECT COUNT(*) FROM sqlite_master", null,
                    ).use { cursor ->
                        cursor.moveToFirst()
                        cursor.getInt(0)
                    }
                }
            }
            val counts = readers.map { it.await() }
            assertTrue("every pooled connection must open", counts.all { it > 0 })
        }
    }

    /**
     * Foreign keys are enforced on a connection other than the one that was
     * configured. #457.
     *
     * A pragma is per connection, and the pool is now larger than one, so
     * setting it on the handle that came back from the open says nothing about
     * the connection a write lands on.
     */
    @Test
    fun foreignKeysAreOnAcrossThePool() {
        val database = runBlocking { HealthTrailDatabase.open(context) }
        repeat(6) {
            database.database.rawQuery("PRAGMA foreign_keys", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("foreign keys must be enforced on every connection", 1, cursor.getInt(0))
            }
        }
    }

    /**
     * The mode survives the pool, which is the half a single `execSQL` misses.
     *
     * A journal mode set with one statement lands on one pooled connection.
     * Reading it back through a fresh query after real work has been done is
     * the cheapest available proof that the setting belongs to the database
     * rather than to whichever connection happened to answer first.
     */
    @Test
    fun theModeHoldsAfterWorkRatherThanOnlyAtOpen() {
        val database = runBlocking { HealthTrailDatabase.open(context) }
        repeat(8) { database.changeLogSize() }
        database.database.rawQuery("PRAGMA journal_mode", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("wal", cursor.getString(0).lowercase())
        }
    }
}
