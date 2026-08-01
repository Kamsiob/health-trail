package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Content addressed attachment storage.
 *
 * The data contract picks this shape for three consequences, and each one is a
 * test here: an attachment cannot conflict during sync because identical bytes
 * are the same file, transferring one twice is free, and a corrupt transfer is
 * detectable by rehashing.
 *
 * The last is the one the export depends on. `contract/export-format.md`
 * section 7 lists "an attachment whose hash does not match its filename" as a
 * file that must fail cleanly, and it can only be detected because the name is
 * a claim about the bytes.
 */
@RunWith(AndroidJUnit4::class)
class AttachmentsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var root: File
    private lateinit var attachments: Attachments

    @Before
    fun setUp() {
        root = File(context.cacheDir, "attachments-test-${System.nanoTime()}")
        root.mkdirs()
        attachments = Attachments.openAt(root)
    }

    @After
    fun tearDown() {
        root.listFiles()?.forEach { it.delete() }
        root.delete()
    }

    @Test
    fun theSameBytesAreTheSameFile() = runBlocking {
        // Why this matters: two devices that photographed the same page agree
        // without a merge, and a person who attaches one discharge summary to
        // two entries has one file.
        val bytes = "a photographed bill".toByteArray()

        val first = attachments.put(bytes)
        val second = attachments.put(bytes)

        assertEquals(first, second)
        assertEquals("storing twice wrote two files", 1, attachments.all().size)
    }

    @Test
    fun differentBytesAreDifferentFiles() = runBlocking {
        val one = attachments.put("page one".toByteArray())
        val two = attachments.put("page two".toByteArray())
        assertNotEquals(one, two)
        assertEquals(2, attachments.all().size)
    }

    @Test
    fun theNameIsTheHashOfTheBytes() = runBlocking {
        val bytes = "the discharge summary".toByteArray()
        val hash = attachments.put(bytes)

        assertEquals(Attachments.sha256(bytes), hash)
        assertEquals(
            "the file on disk is not named by its hash",
            hash,
            attachments.fileFor(hash).name,
        )
    }

    @Test
    fun whatComesBackIsWhatWentIn() = runBlocking {
        // Bytes rather than text, and with a zero in them, because a
        // photograph is not a string and an off by one in a buffer loop shows
        // up here first.
        val bytes = ByteArray(200_000) { (it % 251).toByte() }
        val hash = attachments.put(bytes)

        val read = attachments.read(hash)
        assertTrue("the bytes came back changed", bytes.contentEquals(read))
    }

    @Test
    fun aStreamAndAByteArrayAgree() = runBlocking {
        // The two entry points must produce the same hash for the same bytes,
        // or a photograph stored by streaming would be a different file from
        // the same photograph stored whole.
        val bytes = ByteArray(300_000) { (it % 97).toByte() }

        val streamed = attachments.put(bytes.inputStream())
        val whole = attachments.put(bytes)

        assertEquals(streamed, whole)
        assertEquals("the two paths wrote two files", 1, attachments.all().size)
    }

    @Test
    fun verifyCatchesAFileThatNoLongerMatchesItsName() = runBlocking {
        // The export's failure case, and the reason content addressing earns
        // its keep. A file whose bytes changed is corrupt by definition,
        // because its name is a claim about its contents.
        val hash = attachments.put("intact".toByteArray())
        assertTrue(attachments.verify(hash))

        attachments.fileFor(hash).writeBytes("tampered".toByteArray())

        assertFalse("a changed file still verified", attachments.verify(hash))
    }

    @Test
    fun aMissingFileVerifiesAsFalseRatherThanThrowing() = runBlocking {
        val hash = Attachments.sha256("never stored".toByteArray())
        assertFalse(attachments.verify(hash))
        assertFalse(attachments.exists(hash))
        assertNull(attachments.read(hash))
    }

    @Test
    fun aHalfWrittenFileIsNeverVisibleUnderItsHash() = runBlocking {
        // Written to a temporary name and moved, so the presence of a file
        // named by a hash means the whole file is there. A partial file under
        // the right name is the one thing content addressing must not produce.
        attachments.put("real".toByteArray())
        File(root, "deadbeef.part").writeBytes("half a photograph".toByteArray())

        assertEquals("a .part file was listed as an attachment", 1, attachments.all().size)
        assertEquals(1, attachments.sweepIncomplete())
        assertEquals(1, attachments.all().size)
    }

    @Test
    fun totalBytesCountsWhatIsActuallyThere() = runBlocking {
        // What the manifest reports and what the 4 GB warning from D13 reads.
        attachments.put(ByteArray(1000))
        attachments.put(ByteArray(2000) { 1 })

        assertEquals(3000L, attachments.totalBytes())
    }

    @Test
    fun theLimitsFromTheDecisionAreCarriedInTheCode() {
        // D13, so the numbers live next to the thing they limit rather than
        // only in a decisions file nobody greps.
        assertEquals(25L * 1024 * 1024, Attachments.MAX_ATTACHMENT_BYTES)
        assertEquals(4L * 1024 * 1024 * 1024, Attachments.WARN_TOTAL_BYTES)
    }
}
