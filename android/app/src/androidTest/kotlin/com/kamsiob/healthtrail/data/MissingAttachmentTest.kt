package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An attachment row whose bytes are gone, and what the export does about it.
 * #332.
 *
 * **The defect this class exists for was silent and landed at recovery time.**
 * `Attachments.all` lists files on disk rather than rows, so a live `attachment`
 * row whose file had gone shipped as a row with no file, and
 * [ExportContainer.open] then refused the whole archive by name. Nothing noticed
 * at export and the person was told it had succeeded. They would find out on the
 * new phone, with the old one gone, which is the shape
 * `contract/DATA-CONTRACT.md` section 8 opens by calling worse than an honest
 * failure.
 *
 * **What this proves is that the export now looks, and that it looks for exactly
 * what the import refuses on.** The first test asserts both halves against one
 * archive: [Backup.export] names the file, and opening that same archive fails
 * with [ExportContainer.Problem.AttachmentMissing] naming the same hash. Those
 * two agreeing is the whole point, because a report that did not match the
 * refusal would be a second opinion rather than a warning.
 *
 * **What this does not prove**, said plainly so a green run is not read as more
 * than it is: **the archive is still one this app cannot open.** Closing that
 * needs the missing list in `MANIFEST.json`, which is a change to a published
 * format and belongs with the owner, and then `open` accepting a row the
 * manifest says was already missing. Until then the person is told at the moment
 * they can still do something about it, and that is all.
 *
 * **The rows are rows the app itself writes.** Every attachment here comes from
 * `Repository.createDocument`, which is the app's own path, so the fixture rule
 * holds. What the test does that the app does not is delete the bytes from under
 * a row, which is precisely the state the defect is about and which no screen
 * can produce on purpose.
 */
@RunWith(AndroidJUnit4::class)
class MissingAttachmentTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var archive: File

    /**
     * Where [ExportContainer.open] unpacks, cleaned up here rather than in the
     * tests.
     *
     * **Because a test that ends on its own cleanup returns whatever the
     * cleanup returned**, and a `runBlocking` expression body then makes the
     * method non-void, which JUnit refuses for the whole class with a message
     * about the method rather than about the expression. Both tests that open
     * an archive hit it at once.
     */
    private lateinit var staging: File

    private val secret get() = "a passphrase for the missing attachment tests".toCharArray()

    /**
     * Every hash this test made a row for, so [tearDown] can put the database
     * back.
     *
     * **This class leaves the app's real database in the one state every other
     * export test must not meet.** The database persists across classes in a
     * connected run, so a live attachment row with no file left behind here
     * would make `RoundTripTest`, `RegenerationTest` and `MergeApplyTest` all
     * fail on an archive that will not open, naming a hash from a fixture they
     * have never heard of. That is a defect this class would be inserting into
     * the suite rather than finding.
     */
    private val made = mutableListOf<String>()

    @Before
    fun setUp() {
        Repository.closeForTest()
        made.clear()
        archive = File(context.cacheDir, "missing-${System.nanoTime()}.htz")
        staging = File(context.cacheDir, "missing-staging-${System.nanoTime()}")
    }

    @After
    fun tearDown() {
        // Tombstoned rather than removed, because a tombstone is what the app
        // itself would leave and the container allows a deleted attachment to
        // have no bytes. Removing the rows would be the one thing rule 3 says
        // never happens to a row.
        made.forEach { runBlocking { tombstoneAttachment(it) } }
        archive.delete()
        staging.deleteRecursively()
        Repository.closeForTest()
    }

    /**
     * Writes bytes, records a document that points at them, and returns the hash.
     *
     * The bytes differ per call because a file is named by their hash: two
     * documents attached to identical bytes are one file by design, which is a
     * property [oneFileTwoRowsCountsOnce] leans on deliberately and every other
     * test here has to avoid by accident.
     */
    private suspend fun document(title: String, bytes: ByteArray): String {
        val store = Attachments.open(context)
        val hash = store.put(bytes)
        Repository.open(context).createDocument(
            subjectId = subject(),
            title = title,
            received = Edtf.parse("2026-08-10")!!,
            sha256 = hash,
            byteSize = bytes.size.toLong(),
            mimeType = "image/jpeg",
            originalFilename = "$title.jpg",
        )
        made += hash
        return hash
    }

    /** One per test, rather than one per document, which would be five Margarets. */
    private var subject: String? = null

    private suspend fun subject(): String = subject ?: Repository.open(context)
        .createSubject(displayName = "Margaret", relationship = "Mom")
        .also { subject = it }

    /** Takes the bytes out from under a live row, which is the whole fixture. */
    private suspend fun loseTheFile(hash: String) {
        val file = Attachments.open(context).fileFor(hash)
        assertTrue("the fixture never wrote the file it means to remove", file.isFile)
        // Not `rm`: this is the app's own storage directory and one file in it,
        // which is what a failed write or an interrupted copy leaves behind.
        assertTrue("the fixture could not remove the file", file.delete())
    }

    private suspend fun export(): Backup.Written = Backup.export(
        context = context,
        target = archive,
        exportedAt = 1_785_000_000_000L,
        passphrase = secret,
    )

    /**
     * The whole chain, on one archive: export names it, and open refuses on it.
     *
     * **Both halves against the same file, deliberately.** Asserting them
     * separately would let the report and the refusal drift apart, and a warning
     * that named a different file from the one that blocks the restore would be
     * worse than none.
     */
    @Test
    fun exportNamesTheFileItCouldNotFind() = runBlocking {
        val hash = document("discharge summary", "the bytes of a discharge summary".toByteArray())
        loseTheFile(hash)

        val written = export()

        val missing = written.missingAttachments.singleOrNull {
            it.sha256 == hash
        }
        assertTrue(
            "the export did not notice a live attachment row with no file: " +
                written.missingAttachments,
            missing != null,
        )
        // The name and the date, which 8.3 requires to survive so that somebody
        // can see a photograph existed and is gone rather than never learning it
        // was there. A bare content hash says that to nobody.
        assertEquals("discharge summary.jpg", missing!!.originalFilename)
        assertTrue("the row carries no created_at", missing.createdAt > 0)

        // And the same archive is the one the app refuses, on the same hash.
        val problem = ExportContainer.open(
            archive,
            staging,
            passphrase = secret,
            expected = Backup.schema(context),
        ).exceptionOrNull()
        val reason = (problem as ExportContainer.ExportProblem).problem
        assertTrue(
            "opening the archive failed for some other reason: ${reason.message}",
            reason is ExportContainer.Problem.AttachmentMissing,
        )
        // **That the refusal names a file the export warned about, rather than
        // this exact one.** `open` stops at the first row it finds with no
        // file, and which row that is depends on the whole database, which in a
        // connected run holds whatever the classes before this one left. The
        // property worth asserting is not which file it picked; it is that
        // export cannot be silent about anything import will refuse on.
        val refused = (reason as ExportContainer.Problem.AttachmentMissing).hash
        assertTrue(
            "the import refused on $refused, which the export never warned about: " +
                written.missingAttachments,
            written.missingAttachments.any { it.sha256 == refused },
        )
    }

    /**
     * An attachment whose file is where it should be is not reported.
     *
     * **The half that stops this being a warning nobody can act on.** A check
     * that fires on a sound notebook trains somebody to ignore it, and the state
     * it describes has never once occurred in this project.
     */
    @Test
    fun anIntactAttachmentIsNotReported() = runBlocking {
        val hash = document("blood results", "the bytes of a results page".toByteArray())

        val written = export()

        assertTrue(
            "an attachment whose file is present was reported missing: " +
                written.missingAttachments,
            written.missingAttachments.none { it.sha256 == hash },
        )
    }

    /**
     * A tombstoned attachment whose bytes are gone is not reported.
     *
     * **This is the clause that keeps the warning honest.** A deleted
     * attachment's bytes are legitimately absent while its row still travels,
     * because the row is how a restore learns the deletion happened at all.
     * Reporting it would fire on every notebook where somebody has ever removed
     * a photograph, which is most of them, and [ExportContainer.open] does not
     * refuse on it either. The two have to agree.
     */
    @Test
    fun aDeletedAttachmentWithNoFileIsNotReported() = runBlocking {
        val hash = document("an old letter", "the bytes of an old letter".toByteArray())
        tombstoneAttachment(hash)
        loseTheFile(hash)

        val written = export()

        assertTrue(
            "a tombstoned attachment was reported as missing: " +
                written.missingAttachments,
            written.missingAttachments.none { it.sha256 == hash },
        )

        // And the archive still opens, which is the assertion that proves the
        // clause above matters rather than merely being consistent.
        val opened = ExportContainer.open(
            archive,
            staging,
            passphrase = secret,
            expected = Backup.schema(context),
        )
        assertTrue(
            "a notebook with a deleted photograph produced an archive that will not open: " +
                opened.exceptionOrNull()?.message,
            opened.isSuccess,
        )
    }

    /**
     * Two rows naming one absent file are one missing file, not two.
     *
     * **A file is named by the hash of its bytes**, so a person who attached the
     * same discharge summary to two entries has one file. Counting rows would
     * tell them two photographs are gone when one is, which is a number they
     * cannot reconcile with anything they can see, and the screen prints this
     * count.
     */
    @Test
    fun oneFileTwoRowsCountsOnce() = runBlocking {
        val bytes = "the bytes attached to two documents".toByteArray()
        val first = document("the letter", bytes)
        val second = document("the letter again", bytes)
        assertEquals("the fixture did not produce one file under two rows", first, second)

        loseTheFile(first)

        val written = export()

        assertEquals(
            "one absent file under two rows was counted twice: " +
                written.missingAttachments,
            1,
            written.missingAttachments.count { it.sha256 == first },
        )
    }

    /**
     * Tombstones an attachment row, with the statement the app itself runs.
     *
     * There is no `Repository.Section` for attachments, because no screen lists
     * them on their own: a photograph is removed by removing what it is attached
     * to. This is `Repository.delete`'s own `UPDATE`, against the one table it
     * cannot be pointed at, so the row that results is the row the app produces.
     */
    private suspend fun tombstoneAttachment(hash: String) {
        val database = HealthTrailDatabase.open(context).database
        database.execSQL(
            "UPDATE attachment SET deleted_at = ?, updated_at = ?, rev = rev + 1 " +
                "WHERE sha256 = ? AND deleted_at IS NULL",
            arrayOf<Any?>(1_785_000_000_000L, 1_785_000_000_000L, hash),
        )
    }
}
