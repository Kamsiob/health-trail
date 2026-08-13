package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * After a restore, this phone writes as itself. #320.
 *
 * **`app_meta` travels in the archive and `device_id` lives in it**, so a
 * restored notebook arrives carrying the source phone's identity. Nothing looked
 * wrong: the notebook was complete, the screens were right, and every row this
 * phone wrote afterward was stamped with a device that was not it.
 *
 * **`contract/DATA-CONTRACT.md` defines `origin_device` as "the id of the device
 * that created the row"**, and it is there so a future merge can tell two
 * devices' writes apart. Restore is exactly the moment a notebook moves between
 * devices, so this corrupted provenance at the one point provenance is for.
 *
 * **The old identity is kept as a peer rather than erased**, because the
 * notebook genuinely did come from another device and the `device` table exists
 * to say so. Only which row is `is_self` changes.
 */
@RunWith(AndroidJUnit4::class)
class RestoreIdentityTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var work: File
    private val secret get() = "a passphrase for the identity test".toCharArray()

    @Before
    fun setUp() {
        work = File(context.cacheDir, "identity").apply { deleteRecursively(); mkdirs() }
    }

    @After
    fun tearDown() {
        work.deleteRecursively()
        Repository.closeForTest()
    }

    /** The archive, opened the way the restore screen opens it. */
    private suspend fun opened(archive: File, name: String) =
        ExportContainer
            .open(archive, File(work, "staging-$name"), secret)
            .getOrThrow()

    private fun deviceId(): String = runBlocking {
        HealthTrailDatabase.open(context).deviceId
    }

    private fun selfRows(): List<String> = runBlocking {
        HealthTrailDatabase.open(context).database
            .rawQuery("SELECT id FROM device WHERE is_self = 1", null)
            .use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
    }

    private fun originOfNewestEntry(): String = runBlocking {
        HealthTrailDatabase.open(context).database
            .rawQuery(
                "SELECT origin_device FROM live_entry " +
                    "ORDER BY created_at DESC, id DESC LIMIT 1",
                null,
            )
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getString(0)
            }
    }

    /**
     * **A restore of this phone's own archive still re-identifies it**, and that
     * is deliberate rather than a limitation of the test. Nothing in the file
     * says which phone wrote it, so the alternative is to compare identities and
     * keep the old one when they match, which would leave the interesting case,
     * a file from another phone, resting on a comparison that cannot be made.
     */
    @Test
    fun thephoneWritesAsItselfAfterARestore() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret", relationship = "Mom")
        repository.createEntry(
            subjectId = subject,
            kind = "call",
            title = "Before the archive was written",
            occurred = com.kamsiob.healthtrail.time.Edtf.unknown(),
        )

        val archive = File(work, "out.zip")
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val was = deviceId()

        Backup.restore(context, opened(archive, "one")).getOrThrow()

        val now = deviceId()
        assertNotEquals(
            "the phone kept the identity that arrived in the file, so every row " +
                "it writes from now on claims to have been written somewhere else",
            was,
            now,
        )

        // And the rows it writes afterward carry it.
        val after = Repository.open(context)
        val subjectAfter = after.activeSubject()!!.id
        after.createEntry(
            subjectId = subjectAfter,
            kind = "call",
            title = "Written after the restore",
            occurred = com.kamsiob.healthtrail.time.Edtf.unknown(),
        )
        assertEquals(
            "a row written after the restore is stamped with the phone that wrote it",
            now,
            originOfNewestEntry(),
        )
    }

    /** Exactly one row says it is this device, and it is the new one. */
    @Test
    fun theArrivingDeviceBecomesApeerRatherThanDisappearing() = runBlocking {
        val repository = Repository.open(context)
        repository.createSubject(displayName = "Margaret", relationship = "Mom")
        val archive = File(work, "peer.zip")
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val was = deviceId()

        Backup.restore(context, opened(archive, "two")).getOrThrow()

        val selves = selfRows()
        assertEquals("exactly one device is this one", 1, selves.size)
        assertEquals(deviceId(), selves.first())

        val known = HealthTrailDatabase.open(context).database
            .rawQuery("SELECT id FROM device", null)
            .use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        assertTrue(
            "the device the notebook came from is kept as a peer, because it did",
            was in known,
        )
    }
}
