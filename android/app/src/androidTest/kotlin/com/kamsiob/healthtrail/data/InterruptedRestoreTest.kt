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
import java.io.File
import java.io.RandomAccessFile

/**
 * What survives a restore that was interrupted part way. #409.
 *
 * **Three files were being written and none of them was ever read.** A restore
 * took a safety copy of the notebook it was about to replace, built the new one
 * beside it, and then replaced the live file with a delete and a stream copy.
 * Process death inside that copy left a truncated `health-trail.db` next to a
 * complete `.replacing` and a complete `.arriving`, and the next launch opened
 * the truncated one. **The record was lost at the exact moment the person was
 * recovering it**, and the two complete copies of it sat on the disk unread.
 *
 * The swap is a rename now, so it either happened or it did not. These cover
 * the other half: what startup does with what it finds.
 */
@RunWith(AndroidJUnit4::class)
class InterruptedRestoreTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val live: File get() = context.getDatabasePath(HealthTrailDatabase.FILE_NAME)
    private val replacing: File get() = File(live.path + HealthTrailDatabase.REPLACING_SUFFIX)
    private val arriving: File get() = File(live.path + HealthTrailDatabase.ARRIVING_SUFFIX)

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    /**
     * A torn live file and a good safety copy comes back as the notebook.
     *
     * **The identity is what proves it is the same notebook** rather than a
     * fresh one. `ensureDeviceId` generates and stores an id on a database that
     * has none, so a rebuilt database would answer with a different one, and
     * "the app opened and did not crash" would otherwise pass on an empty
     * notebook. That is the failure this whole milestone is about.
     */
    @Test
    fun aTornNotebookWithASafetyCopyComesBack() {
        val before = runBlocking { HealthTrailDatabase.open(context) }.deviceId
        Repository.closeForTest()
        val good = live.readBytes()

        try {
            replacing.writeBytes(good)
            RandomAccessFile(live, "rw").use { handle ->
                handle.seek(2048)
                handle.write(ByteArray(4096) { 0x7A })
            }

            val after = runBlocking { HealthTrailDatabase.open(context) }.deviceId
            assertEquals("the notebook that comes back must be the same one", before, after)
            assertFalse(
                "and the safety copy is spent once the notebook opens",
                replacing.exists(),
            )
        } finally {
            Repository.closeForTest()
            replacing.delete()
            live.writeBytes(good)
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()
            Repository.closeForTest()
        }
    }

    /**
     * No live file at all, and the safety copy is adopted rather than a first
     * run being offered.
     *
     * This is the shape that opens the app at "Before you start" with the whole
     * record still on the disk.
     */
    @Test
    fun aMissingNotebookWithASafetyCopyIsAdopted() {
        val before = runBlocking { HealthTrailDatabase.open(context) }.deviceId
        Repository.closeForTest()
        val good = live.readBytes()

        try {
            replacing.writeBytes(good)
            live.delete()
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()

            val after = runBlocking { HealthTrailDatabase.open(context) }.deviceId
            assertEquals("a missing notebook must be recovered, not replaced", before, after)
            assertTrue(live.isFile)
        } finally {
            Repository.closeForTest()
            replacing.delete()
            live.writeBytes(good)
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()
            Repository.closeForTest()
        }
    }

    /**
     * A half built arriving notebook is scrap, and startup says so.
     *
     * Its presence means the rename never ran, so the live file is still the
     * notebook. Nothing is adopted from it and it does not accumulate.
     */
    @Test
    fun aHalfBuiltArrivingNotebookIsSweptAndTheLiveOneIsKept() {
        val before = runBlocking { HealthTrailDatabase.open(context) }.deviceId
        Repository.closeForTest()

        try {
            arriving.writeBytes(ByteArray(4096) { 0x5B })
            val after = runBlocking { HealthTrailDatabase.open(context) }.deviceId
            assertEquals("the live notebook is untouched", before, after)
            assertFalse("and the scrap is gone", arriving.exists())
        } finally {
            Repository.closeForTest()
            arriving.delete()
            Repository.closeForTest()
        }
    }

    /**
     * A finished restore is not rolled back by its own safety copy.
     *
     * Dying between the rename and the cleanup leaves a live file that is the
     * newly restored notebook and a `.replacing` that is the old one. Adopting
     * whenever `.replacing` exists would throw away exactly what the person
     * asked for, so adoption is gated on the live file failing to open.
     */
    @Test
    fun aFinishedRestoreIsNotUndoneByTheCopyItLeft() {
        val before = runBlocking { HealthTrailDatabase.open(context) }.deviceId
        Repository.closeForTest()

        try {
            // Not a database. If this were ever adopted, the open would fail.
            replacing.writeBytes(ByteArray(8192) { 0x3C })
            val after = runBlocking { HealthTrailDatabase.open(context) }.deviceId
            assertEquals("a notebook that opens is never replaced", before, after)
            assertFalse(replacing.exists())
        } finally {
            Repository.closeForTest()
            replacing.delete()
            Repository.closeForTest()
        }
    }
}
