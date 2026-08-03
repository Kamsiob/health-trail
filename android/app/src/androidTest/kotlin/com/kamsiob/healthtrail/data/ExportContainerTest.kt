package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The export container, and the files that must fail cleanly.
 *
 * `contract/export-format.md` section 7 lists eight files that each have to
 * change nothing and name what was wrong. **A generic failure is not enough
 * here.** The person is holding what may be the only copy of a year of their
 * mother's care, and "could not import" tells them nothing about whether to
 * try again, find another copy, or update the app.
 *
 * The round trip these support is section 6, and its rule is the one that
 * matters: **no feature is finished until it survives it.**
 */
@RunWith(AndroidJUnit4::class)
class ExportContainerTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var work: File

    @Before
    fun setUp() {
        work = File(context.cacheDir, "export-test-${System.nanoTime()}")
        work.mkdirs()
    }

    @After
    fun tearDown() {
        work.deleteRecursively()
    }

    private fun source(
        database: File,
        attachments: List<File> = emptyList(),
        rowCounts: Map<String, Int> = mapOf("entry" to 3, "person" to 0),
    ) = ExportContainer.Source(
        database = database,
        attachments = attachments,
        appVersion = "0.1.0",
        originDevice = "01J8Z9K2QF7X3M5N",
        rowCounts = rowCounts,
        subjectCount = 1,
        exportedAt = 1_753_977_600_000L,
    )

    /**
     * A fresh array per use, because both [ExportContainer.write] and
     * [ExportContainer.open] wipe what they are handed.
     *
     * **Every container these tests build is encrypted**, because since format
     * version 2 that is the only kind the app writes and the only kind the
     * importer will open. The failure cases below are about what happens after
     * the encryption gate, so they have to get past it. The refusal of a plain
     * file is its own test. D67.
     */
    private val secret: CharArray get() = "a passphrase for the tests".toCharArray()

    private fun fakeDatabase(contents: String = "SQLite format 3\u0000 and then some rows"): File =
        File(work, "source.sqlite").apply { writeBytes(contents.toByteArray()) }

    // -- the round trip -----------------------------------------------------

    @Test
    fun whatGoesInComesOutByteForByte() = runBlocking {
        val database = fakeDatabase()
        val attachment = File(work, "photo").apply { writeBytes(ByteArray(5000) { it.toByte() }) }
        val hashed = File(work, Attachments.sha256(attachment.readBytes()))
        attachment.renameTo(hashed)

        val target = File(work, "export.htx")
        val written = ExportContainer.write(target, source(database, listOf(hashed)), passphrase = secret)

        val opened = ExportContainer.open(target, File(work, "staging"), passphrase = secret).getOrThrow()

        assertTrue(
            "the database came back changed",
            database.readBytes().contentEquals(opened.database.readBytes()),
        )
        assertEquals(1, opened.attachments.size)
        assertTrue(
            "the attachment came back changed",
            hashed.readBytes().contentEquals(opened.attachments.first().readBytes()),
        )
        assertEquals(written.databaseSha256, opened.manifest.databaseSha256)
    }

    @Test
    fun theManifestSurvivesTheRoundTripIntact() = runBlocking {
        val target = File(work, "export.htx")
        val counts = mapOf("entry" to 1843, "person" to 62, "chapter" to 0)
        val written = ExportContainer.write(
            target,
            source(fakeDatabase(), rowCounts = counts),
            passphrase = secret,
        )

        val read = ExportContainer.open(target, File(work, "staging"), passphrase = secret).getOrThrow().manifest

        assertEquals(written.formatVersion, read.formatVersion)
        assertEquals(written.appVersion, read.appVersion)
        assertEquals(written.originDevice, read.originDevice)
        // To the millisecond, per section 6.
        assertEquals(written.exportedAt, read.exportedAt)
        assertEquals(written.subjectCount, read.subjectCount)
        // Including the tables with zero rows, which is what lets an import say
        // what it is about to do before doing it.
        assertEquals(counts, read.rowCounts)
        assertEquals(0, read.rowCounts["chapter"])
    }

    @Test
    fun theManifestIsTheFirstEntryInTheArchive() = runBlocking {
        // So a reader can say what a file is before it can ask for a
        // passphrase, which is the whole reason the format fixes the order.
        val target = File(work, "export.htx")
        ExportContainer.write(target, source(fakeDatabase()), passphrase = secret)

        java.util.zip.ZipInputStream(target.inputStream()).use { zip ->
            assertEquals(ExportContainer.MANIFEST, zip.nextEntry?.name)
        }
    }

    // -- the files that must fail cleanly ------------------------------------

    @Test
    fun aTruncatedFileNamesWhatWasWrong() = runBlocking {
        val target = File(work, "export.htx")
        ExportContainer.write(target, source(fakeDatabase()), passphrase = secret)
        val whole = target.readBytes()
        target.writeBytes(whole.copyOfRange(0, whole.size / 3))

        val problem = problemFrom(ExportContainer.open(target, File(work, "staging"), passphrase = secret))
        assertTrue(
            "a truncated file did not say so: ${problem.message}",
            problem is ExportContainer.Problem.NotAContainer ||
                problem is ExportContainer.Problem.DatabaseCorrupt ||
                problem is ExportContainer.Problem.NoManifest,
        )
        assertSaysNothingChanged(problem)
    }

    @Test
    fun aZipWithNoManifestNamesWhatWasWrong() = runBlocking {
        val target = File(work, "no-manifest.htx")
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(ExportContainer.DATABASE))
            zip.write("rows".toByteArray())
            zip.closeEntry()
        }

        val problem = problemFrom(ExportContainer.open(target, File(work, "staging"), passphrase = secret))
        assertTrue(problem is ExportContainer.Problem.NoManifest)
        assertTrue(problem.message.contains("manifest"))
    }

    @Test
    fun aManifestFromTheFutureIsRefusedAndNamesBothVersions() = runBlocking {
        // It never guesses at an unknown format. This costs nothing now and is
        // unfixable later.
        val target = File(work, "future.htx")
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(ExportContainer.MANIFEST))
            zip.write("""{"format_version": 99}""".toByteArray())
            zip.closeEntry()
        }

        val problem = problemFrom(ExportContainer.open(target, File(work, "staging"), passphrase = secret))
        assertTrue(problem is ExportContainer.Problem.FromTheFuture)
        assertEquals(99, (problem as ExportContainer.Problem.FromTheFuture).found)
        assertTrue("it does not name the version it found", problem.message.contains("99"))
        assertTrue(
            "it does not name the version it supports",
            problem.message.contains(ExportContainer.FORMAT_VERSION.toString()),
        )
    }

    @Test
    fun aDamagedDatabaseIsCaughtBeforeAnythingIsTouched() = runBlocking {
        val target = File(work, "export.htx")
        val manifest = ExportContainer.write(target, source(fakeDatabase()), passphrase = secret)

        // Rewrite the archive with the same manifest and different rows, which
        // is what a damaged transfer produces.
        val rebuilt = File(work, "damaged.htx")
        ZipOutputStream(rebuilt.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(ExportContainer.MANIFEST))
            zip.write(
                """{"format_version":1,"database":{"sha256":"${manifest.databaseSha256}"}}"""
                    .toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ExportContainer.DATABASE))
            zip.write("not the rows the manifest describes".toByteArray())
            zip.closeEntry()
        }

        val problem = problemFrom(ExportContainer.open(rebuilt, File(work, "staging"), passphrase = secret))
        assertTrue(problem is ExportContainer.Problem.DatabaseCorrupt)
        assertSaysNothingChanged(problem)
    }

    @Test
    fun anAttachmentThatDoesNotHashToItsNameIsCaught() = runBlocking {
        // Detectable only because the name is a claim about the bytes, which is
        // the reason attachments are content addressed at all.
        //
        // **Built through `write` rather than by hand.** This used to assemble
        // the zip itself, which meant it also assembled its own manifest, and
        // that manifest said the file was unencrypted. Since format version 2
        // that is refused before any attachment is looked at, so the test was
        // passing through a door that no longer exists. Going through `write`
        // means the archive is the shape the app actually produces, which is
        // the rule in `TESTING-PERSONAS.md` section 7. D67.
        val database = fakeDatabase()

        // A file whose name is a hash and whose contents are not that hash.
        val liar = File(work, "0".repeat(64))
        liar.writeBytes("bytes that hash to something else entirely".toByteArray())

        val target = File(work, "bad-attachment.htx")
        ExportContainer.write(target, source(database, listOf(liar)), passphrase = secret)

        val problem = problemFrom(
            ExportContainer.open(target, File(work, "staging"), passphrase = secret)
        )
        assertTrue(
            "a lying attachment name was not caught: ${problem.message}",
            problem is ExportContainer.Problem.AttachmentCorrupt,
        )
        assertSaysNothingChanged(problem)
    }

    fun aManifestWithNoDatabaseIsCaught() = runBlocking {
        val target = File(work, "empty.htx")
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(ExportContainer.MANIFEST))
            zip.write("""{"format_version":1}""".toByteArray())
            zip.closeEntry()
        }

        val problem = problemFrom(ExportContainer.open(target, File(work, "staging"), passphrase = secret))
        assertTrue(problem is ExportContainer.Problem.DatabaseMissing)
    }

    @Test
    fun everyFailureMessageIsPlainAndNamesTheProblem() = runBlocking {
        // No placeholder error copy. "Something went wrong, please try again"
        // strips the human voice out at the exact moment the person needs it,
        // which is on the banned list in DESIGN.md section 1.
        val target = File(work, "no-manifest.htx")
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("something-else"))
            zip.write("x".toByteArray())
            zip.closeEntry()
        }

        val problem = problemFrom(ExportContainer.open(target, File(work, "staging"), passphrase = secret))
        val message = problem.message
        assertTrue("the message is too short to say anything: $message", message.length > 30)
        listOf("went wrong", "try again", "error", "failed", "invalid").forEach { banned ->
            assertTrue(
                "the failure message reads as placeholder copy: $message",
                banned !in message.lowercase(),
            )
        }
    }

    // -- the last two of the eight, which need to read the database ---------

    /**
     * A real SQLite database, so the checks that open one have something to
     * open. The container's other tests use a stub payload on purpose, since
     * they are testing the envelope rather than the records.
     */
    private fun realDatabase(build: (android.database.sqlite.SQLiteDatabase) -> Unit): File {
        val file = File(work, "real-${System.nanoTime()}.sqlite")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use(build)
        return file
    }

    private val knownSchema: ExportContainer.Schema = mapOf(
        "entry" to setOf("id", "body"),
        "attachment" to setOf("id", "sha256", "deleted_at"),
    )

    @Test
    fun aDatabaseWithAnUnknownTableIsRefusedAndNamesIt() = runBlocking {
        val database = realDatabase { db ->
            db.execSQL("CREATE TABLE entry (id TEXT, body TEXT)")
            db.execSQL("CREATE TABLE secret_notes (id TEXT)")
        }
        val target = File(work, "unknown-table.htx")
        ExportContainer.write(target, source(database), passphrase = secret)

        val problem = problemFrom(
            ExportContainer.open(target, File(work, "staging"), passphrase = secret, expected = knownSchema),
        )

        assertTrue(
            "an unknown table was not named: ${problem.message}",
            problem is ExportContainer.Problem.UnknownSchema,
        )
        assertEquals("secret_notes", (problem as ExportContainer.Problem.UnknownSchema).what)
        assertTrue(problem.message.contains("secret_notes"))
        assertSaysNothingChanged(problem)
    }

    @Test
    fun aDatabaseWithAnUnknownColumnIsRefusedAndNamesIt() = runBlocking {
        // The column half of the same section 7 line, which a table level check
        // alone would pass straight through.
        val database = realDatabase { db ->
            db.execSQL("CREATE TABLE entry (id TEXT, body TEXT, smuggled TEXT)")
        }
        val target = File(work, "unknown-column.htx")
        ExportContainer.write(target, source(database), passphrase = secret)

        val problem = problemFrom(
            ExportContainer.open(target, File(work, "staging"), passphrase = secret, expected = knownSchema),
        )

        assertTrue(problem is ExportContainer.Problem.UnknownSchema)
        assertEquals("entry.smuggled", (problem as ExportContainer.Problem.UnknownSchema).what)
    }

    @Test
    fun anUnknownShapeIsNotReportedAsAFileFromTheFuture() = runBlocking {
        // The two failures call for opposite advice. A later format means
        // update the app; a file claiming this format while carrying a shape
        // this format does not have has been altered, and sending somebody to
        // update the app would send them somewhere that cannot help.
        val database = realDatabase { db ->
            db.execSQL("CREATE TABLE entry (id TEXT, body TEXT)")
            db.execSQL("CREATE TABLE secret_notes (id TEXT)")
        }
        val target = File(work, "not-the-future.htx")
        ExportContainer.write(target, source(database), passphrase = secret)

        val problem = problemFrom(
            ExportContainer.open(target, File(work, "staging"), passphrase = secret, expected = knownSchema),
        )

        assertTrue(problem !is ExportContainer.Problem.FromTheFuture)
        assertTrue(
            "it tells the person to update the app, which cannot help here",
            "update the app" !in problem.message.lowercase(),
        )
    }

    @Test
    fun anAttachmentTheDatabaseNamesAndTheArchiveLacksIsRefused() = runBlocking {
        val absent = "a".repeat(64)
        val database = realDatabase { db ->
            db.execSQL("CREATE TABLE entry (id TEXT, body TEXT)")
            db.execSQL("CREATE TABLE attachment (id TEXT, sha256 TEXT, deleted_at INTEGER)")
            db.execSQL("INSERT INTO attachment VALUES ('a1', '$absent', NULL)")
        }
        val target = File(work, "missing-attachment.htx")
        ExportContainer.write(target, source(database), passphrase = secret)

        val problem = problemFrom(
            ExportContainer.open(target, File(work, "staging"), passphrase = secret, expected = knownSchema),
        )

        assertTrue(
            "a referenced attachment that is not in the file was not caught",
            problem is ExportContainer.Problem.AttachmentMissing,
        )
        assertEquals(absent, (problem as ExportContainer.Problem.AttachmentMissing).hash)
        assertSaysNothingChanged(problem)
    }

    @Test
    fun aDeletedAttachmentIsAllowedToHaveNoFile() = runBlocking {
        // Its bytes are legitimately gone while the row travels, because the
        // row is how a restore learns the deletion happened. Requiring the file
        // would reject every export taken after somebody deleted a photograph.
        val database = realDatabase { db ->
            db.execSQL("CREATE TABLE entry (id TEXT, body TEXT)")
            db.execSQL("CREATE TABLE attachment (id TEXT, sha256 TEXT, deleted_at INTEGER)")
            db.execSQL("INSERT INTO attachment VALUES ('a1', '${"b".repeat(64)}', 1700)")
        }
        val target = File(work, "deleted-attachment.htx")
        ExportContainer.write(target, source(database), passphrase = secret)

        val opened = ExportContainer.open(
            target,
            File(work, "staging"),
            passphrase = secret,
            expected = knownSchema,
        )

        assertTrue(
            "a tombstoned attachment with no file was refused: ${opened.exceptionOrNull()}",
            opened.isSuccess,
        )
    }

    // -- the two refusals format version 2 added -----------------------------

    @Test
    fun anUnencryptedExportIsRefusedAndSaysWhatTheFileIs() = runBlocking {
        // **Written deliberately plain**, which is the one place in the project
        // that passes null, and it exists so this refusal can be proven rather
        // than assumed. D67.
        val target = File(work, "plain.htx")
        ExportContainer.write(target, source(fakeDatabase()), passphrase = null)

        val problem = problemFrom(
            ExportContainer.open(target, File(work, "staging"), passphrase = secret)
        )

        assertTrue(
            "an unencrypted export was not refused: ${problem.message}",
            problem is ExportContainer.Problem.NotEncrypted,
        )
        // The message has to say what the file is, not that something failed.
        // Somebody holding this is holding their whole record in the clear and
        // the useful thing to tell them is that.
        assertTrue(
            "the refusal does not say the file is readable: ${problem.message}",
            problem.message.contains("readable"),
        )
        assertTrue(
            "the refusal does not say what to do instead: ${problem.message}",
            problem.message.contains("passphrase"),
        )
        assertSaysNothingChanged(problem)
    }

    @Test
    fun anExportThatDecryptsToSomethingOtherThanADatabaseSaysSoRatherThanDamaged() =
        runBlocking {
            // The pre-portability export, which is a real file somebody may be
            // holding: it opens, it authenticates, and what comes out is a
            // SQLCipher database keyed to a phone that may no longer exist.
            // Without this it fails later as "damaged" and sends somebody
            // hunting a corruption that is not there. D61 and D67.
            val target = File(work, "old.htx")
            val notADatabase = fakeDatabase(contents = "not a database at all, just bytes")
            ExportContainer.write(target, source(notADatabase), passphrase = secret)

            val problem = problemFrom(
                ExportContainer.open(target, File(work, "staging"), passphrase = secret)
            )

            assertTrue(
                "a non portable payload was not named as such: ${problem.message}",
                problem is ExportContainer.Problem.NotPortable,
            )
            // It must not blame the passphrase or the file's integrity, because
            // both are fine and saying otherwise sends the person to fix the
            // wrong thing.
            assertTrue(
                "the message does not clear the passphrase: ${problem.message}",
                problem.message.contains("passphrase is right") ||
                    problem.message.contains("passphrase is") &&
                    problem.message.contains("not damaged"),
            )
            assertSaysNothingChanged(problem)
        }

    private fun problemFrom(result: Result<ExportContainer.Opened>): ExportContainer.Problem {
        val error = result.exceptionOrNull()
        assertNotNull("the file opened when it should not have", error)
        assertTrue(error is ExportContainer.ExportProblem)
        return (error as ExportContainer.ExportProblem).problem
    }

    private fun assertSaysNothingChanged(problem: ExportContainer.Problem) {
        assertTrue(
            "a failure that changes nothing should say so: ${problem.message}",
            "nothing was changed" in problem.message.lowercase() ||
                "may be" in problem.message.lowercase(),
        )
    }
}
