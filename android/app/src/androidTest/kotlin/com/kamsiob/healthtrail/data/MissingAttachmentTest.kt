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
 * **Both halves are built now**, as of 2026-08-10. The export looks and records
 * what it found, `MANIFEST.json` carries the list that
 * `contract/DATA-CONTRACT.md` 8.2 always asked for, and [ExportContainer.open]
 * accepts an archive that declares what it is missing. So the archive opens, the
 * row arrives with its name and its date, and the person learns a photograph
 * existed and is gone rather than never learning it was there, which is 8.3.
 *
 * **The distinction the list exists to make is the important assertion here.**
 * An archive that declares a missing attachment is a record with a gap in it. An
 * archive whose attachment is simply absent is damaged in transit. Collapsing
 * the two would mean a truncated copy opens quietly and restores short, which is
 * the silent partial correctness section 8 opens by refusing, so
 * [anUndeclaredMissingAttachmentIsStillRefused] builds that case by hand and
 * asserts the refusal survives.
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

        // **And the archive opens**, which is the half that changed on
        // 2026-08-10. Until then this same file was refused by name, at
        // restore, on the new phone with the old one gone, and that was the
        // whole defect. The manifest declares what is gone now, so the archive
        // is one that knows what it is missing rather than one that is damaged.
        // #332, and `contract/DATA-CONTRACT.md` 8.2 always asked for the list.
        val opened = ExportContainer.open(
            archive,
            staging,
            passphrase = secret,
            expected = Backup.schema(context),
        )
        assertTrue(
            "an archive that declares its missing attachment was still refused: " +
                opened.exceptionOrNull()?.message,
            opened.isSuccess,
        )

        // **And it says so in the manifest**, with the name and the date 8.3
        // requires, so a reader learns a photograph existed and is gone rather
        // than never learning it was there.
        val declared = opened.getOrThrow().manifest.missingAttachments
            .singleOrNull { it.sha256 == hash }
        assertTrue("the manifest does not name the missing file: $declared", declared != null)
        assertEquals("discharge summary.jpg", declared!!.originalFilename)
        assertEquals(missing.createdAt, declared.createdAt)
    }

    /**
     * An attachment that is absent and **not** declared is still refused.
     *
     * **This is the difference the list exists to make**, and without it the fix
     * for #332 would have turned a loud failure into a silent one. An archive
     * that declares a missing attachment is a record with a gap in it. An
     * archive whose attachment is simply absent is damaged in transit, and it
     * has to fail, or a truncated copy opens quietly and restores short.
     *
     * **Built by hand, because the app cannot produce one any more.** That is
     * the point: the export always declares what it could not find now, so the
     * only way to get an undeclared gap is to write a container whose manifest
     * says nothing while its `attachments/` folder is empty. The database is the
     * app's own, taken out of a real archive, so the rows are rows the app
     * wrote.
     */
    @Test
    fun anUndeclaredMissingAttachmentIsStillRefused() = runBlocking {
        val hash = document("a letter", "the bytes of a letter that will be dropped".toByteArray())
        export()
        val sound = ExportContainer.open(archive, staging, passphrase = secret).getOrThrow()
        assertTrue(
            "the fixture's own archive is missing the file it should carry",
            sound.attachments.any { it.name == hash },
        )

        // The same database, and no attachments at all, with a manifest that
        // declares nothing missing. This is what a damaged transfer looks like
        // from the reader's side.
        val undeclared = File(context.cacheDir, "undeclared-${System.nanoTime()}.htz")
        val elsewhere = File(context.cacheDir, "undeclared-staging-${System.nanoTime()}")
        try {
            ExportContainer.write(
                target = undeclared,
                source = ExportContainer.Source(
                    database = sound.database,
                    attachments = emptyList(),
                    appVersion = sound.manifest.appVersion,
                    originDevice = sound.manifest.originDevice,
                    rowCounts = sound.manifest.rowCounts,
                    subjectCount = sound.manifest.subjectCount,
                    exportedAt = sound.manifest.exportedAt,
                    schemaSql = "-- not read by this test\n",
                    readableWords = ReadableWords.from(
                        com.kamsiob.healthtrail.i18n.Strings.load(context),
                        catalogNames = ReadableWords.catalogNames(context),
                    ),
                    // The whole fixture: nothing declared, and nothing carried.
                    missingAttachments = emptyList(),
                ),
                passphrase = secret,
            )

            val opened = ExportContainer.open(
                undeclared,
                elsewhere,
                passphrase = secret,
                expected = Backup.schema(context),
            )
            val reason = (opened.exceptionOrNull() as? ExportContainer.ExportProblem)?.problem
            assertTrue(
                "an archive missing a file it never declared was accepted, so a damaged " +
                    "copy now restores short and says nothing",
                reason is ExportContainer.Problem.AttachmentMissing,
            )
        } finally {
            undeclared.delete()
            elsewhere.deleteRecursively()
        }
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
