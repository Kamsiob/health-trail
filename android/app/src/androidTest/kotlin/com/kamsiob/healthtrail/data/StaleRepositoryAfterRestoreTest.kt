package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A `Repository` held across a restore, and what happens when it is written to.
 *
 * **It was written to test a hypothesis and the hypothesis was wrong**, which
 * is why it passes. It stays, because what it rules out is worth keeping:
 * a future session reading #346 should not spend an afternoon on the same
 * idea. #346 and #307.
 *
 * **Why it passes.** A `Repository` holds no connection. Every operation calls
 * `db()`, which is `HealthTrailDatabase.open(app)`, so a reference captured
 * before a restore re-resolves the singleton on its next use and picks up the
 * new database. **The object is a handle to the app, not to a connection**,
 * and that design makes this whole class of staleness bug impossible.
 *
 * **The hypothesis.** `Backup.restore` closes the `Repository` singleton and
 * then replaces the database file underneath it. Closing the singleton nulls a
 * field; **it cannot reach a reference somebody already holds**. So a
 * repository captured before a restore points at a closed connection and a
 * replaced file, and its next write should raise
 * `SQLiteReadOnlyDatabaseException` with code 1032, `SQLITE_READONLY_DBMOVED`,
 * which is what the intermittent failure in `RoundTripTest` reports.
 *
 * **Why it matters beyond a flaky test.** `AppRoot` records the disclaimer
 * acceptance through exactly this shape: a `Repository` captured when the state
 * was built, written to from a `LaunchedEffect` afterward. **The one moment the
 * gate reappears is right after a restore**, per the comment in that file, so
 * that write happens exactly when the reference is most likely to be stale. If
 * it fails there, the acceptance is never recorded and the person meets the
 * gate again, which is #307's symptom.
 */
@RunWith(AndroidJUnit4::class)
class StaleRepositoryAfterRestoreTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var archive: File

    @Before
    fun setUp() {
        Repository.closeForTest()
        archive = File(context.cacheDir, "stale-${System.nanoTime()}.htz")
    }

    @After
    fun tearDown() {
        archive.delete()
        Repository.closeForTest()
    }

    /**
     * **The write that `AppRoot` performs after a restore**, through a
     * reference taken before it.
     *
     * It passes, so #346 is something else. Kept as the record of that.
     */
    @Test
    fun aRepositoryHeldAcrossARestoreCanStillBeWrittenTo() = runBlocking {
        val before = Repository.open(context)
        before.createSubject(displayName = "Held across the restore")

        val passphrase = "a passphrase for the stale reference test"
        Backup.export(
            context,
            archive,
            exportedAt = 1_754_000_000_000L,
            passphrase = passphrase.toCharArray(),
        )
        val staging = File(context.cacheDir, "stale-restore-${System.nanoTime()}")
        val opened = ExportContainer
            .open(archive, staging, passphrase.toCharArray())
            .getOrThrow()
        Backup.restore(context, opened).getOrThrow()
        staging.deleteRecursively()

        // Exactly what AppRoot does from RootState.Accepting, through the
        // reference it captured earlier.
        before.putSettingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED, 1_754_000_000_001L)

        assertEquals(
            "the acceptance written through a repository held across a restore did not land",
            1_754_000_000_001L,
            Repository.open(context).settingTimestamp(Repository.KEY_DISCLAIMER_ACCEPTED),
        )
    }
}
