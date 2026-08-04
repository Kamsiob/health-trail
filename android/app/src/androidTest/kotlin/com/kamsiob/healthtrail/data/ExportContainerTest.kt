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
 * `contract/EXPORT-FORMAT.md` section 7 lists eight files that each have to
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
        schemaSql = "-- the schema this payload was written against\nCREATE TABLE entry (id TEXT);\n",
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

    /** An attachment, named by its content hash the way the store names them. */
    private fun attachment(contents: String): File {
        val bytes = contents.toByteArray()
        return File(work, Attachments.sha256(bytes)).apply { writeBytes(bytes) }
    }

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
            assertEquals(ExportContainer.OUTER_MANIFEST, zip.nextEntry?.name)
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
            zip.putNextEntry(ZipEntry(ExportContainer.OUTER_MANIFEST))
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
        // **Damaged inside the payload, which is where the record now lives.**
        // Version 2 could corrupt the archive from outside, because the database
        // was an entry of the outer zip. In version 3 the only thing out there
        // is ciphertext, so a test that damages the outer layer is testing the
        // cipher rather than the corruption check. This rebuilds the inner
        // container with a manifest that describes rows the payload does not
        // have, which is what a damaged transfer and a bad disk both look like
        // by the time the importer sees them.
        val target = File(work, "export.htx")
        ExportContainer.write(target, source(fakeDatabase()), passphrase = secret)

        val damaged = File(work, "damaged.htx")
        rebuildPayload(target, damaged) { name, bytes ->
            if (name == ExportContainer.DATABASE) {
                "SQLite format 3\u0000 but not the rows the manifest describes".toByteArray()
            } else {
                bytes
            }
        }

        val problem = problemFrom(
            ExportContainer.open(damaged, File(work, "staging"), passphrase = secret)
        )
        assertTrue(
            "a damaged payload was not caught: $problem",
            problem is ExportContainer.Problem.DatabaseCorrupt,
        )
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
            zip.putNextEntry(ZipEntry(ExportContainer.OUTER_MANIFEST))
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
        // **Assembled by hand, because this code cannot write one.** Format
        // version 3 has nowhere to put an unencrypted record: `payload.enc` is
        // the only entry data can go in. Version 2 kept a null passphrase path
        // alive purely so this test could exist, which meant the one thing in
        // the project that could write a plain copy of somebody's whole record
        // was kept alive to prove that plain copies are refused.
        //
        // Building it here is the stronger test anyway: it proves the refusal
        // catches a file this app could not have produced, which is the case
        // that actually matters. D67.
        val target = File(work, "plain.htx")
        java.util.zip.ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry(ExportContainer.OUTER_MANIFEST))
            zip.write(
                (
                    "{\"format_version\": 2, \"app_version\": \"0.1.0\", " +
                        "\"platform\": \"android\", \"exported_at\": 1753977600000, " +
                        "\"encrypted\": false}"
                    ).toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("data.sqlite"))
            zip.write(fakeDatabase().readBytes())
            zip.closeEntry()
        }

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

    // -- the two layers format version 3 introduced --------------------------

    @Test
    fun theOuterLayerHoldsExactlyThreeThings() = runBlocking {
        // `contract/DATA-CONTRACT.md` 8.1 names them, and "exactly" is the word
        // that matters: anything else out here is something a person did not
        // choose to publish sitting beside their encrypted record.
        val target = File(work, "export.htx")
        ExportContainer.write(
            target,
            source(fakeDatabase(), attachments = listOf(attachment("a photograph"))),
            passphrase = secret,
        )

        val names = java.util.zip.ZipInputStream(target.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.toList()
        }
        assertEquals(
            listOf(
                ExportContainer.OUTER_MANIFEST,
                ExportContainer.OUTER_README,
                ExportContainer.PAYLOAD,
            ),
            names,
        )
    }

    @Test
    fun theOuterLayerSaysNothingAboutThePerson() = runBlocking {
        // Row counts alone are a profile. "1,630 entries over six years" says
        // how ill somebody has been and for how long, to anything that can read
        // the file without the passphrase.
        val target = File(work, "export.htx")
        ExportContainer.write(
            target,
            source(fakeDatabase(), rowCounts = mapOf("entry" to 1630, "appointment" to 23)),
            passphrase = secret,
        )

        val outside = java.util.zip.ZipInputStream(target.inputStream()).use { zip ->
            buildString {
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name != ExportContainer.PAYLOAD) {
                        append(zip.readBytes().decodeToString())
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        for (secretish in listOf("row_counts", "1630", "entry\"", "origin_device", "subject")) {
            assertTrue(
                "the outer layer leaks $secretish",
                secretish !in outside,
            )
        }
    }

    @Test
    fun aPayloadWithItsLastFramesRemovedIsRefusedRatherThanOpenedShort() = runBlocking {
        // **The failure this format is most afraid of.** Every frame
        // authenticates on its own, so without the final-frame flag a stream cut
        // short decrypts perfectly and hands back an archive missing a year of
        // somebody's record, with nothing anywhere saying so.
        val target = File(work, "export.htx")
        // Large enough to be several frames, so there is a middle to cut.
        // **Bytes that do not compress.** The inner container is deflated, and
        // the first version of this test filled the database with a repeating
        // pattern: three megabytes of it became fifteen kilobytes, the payload
        // was a single frame, and the test failed trying to cut a tail that did
        // not exist. A hash chain is deterministic and incompressible, which is
        // both properties this needs.
        val big = File(work, "big.sqlite").apply {
            val out = java.io.ByteArrayOutputStream()
            out.write("SQLite format 3\u0000".toByteArray())
            var block = "seed".toByteArray()
            while (out.size() < 3 * ExportCrypto.CHUNK_BYTES) {
                block = java.security.MessageDigest.getInstance("SHA-256").digest(block)
                out.write(block)
            }
            writeBytes(out.toByteArray())
        }
        ExportContainer.write(target, source(big), passphrase = secret)

        // Rebuilt with the payload's tail cut off, so the outer zip stays valid
        // and only the frames are short. Truncating the file itself would be
        // caught by the zip layer and would prove nothing about the frames.
        val entries = java.util.zip.ZipInputStream(target.inputStream()).use { zip ->
            buildMap<String, ByteArray> {
                var entry = zip.nextEntry
                while (entry != null) {
                    put(entry.name, zip.readBytes())
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val cut = File(work, "cut.htx")
        java.util.zip.ZipOutputStream(cut.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(
                    if (name == ExportContainer.PAYLOAD) {
                        bytes.copyOfRange(0, ExportCrypto.CHUNK_BYTES + 100)
                    } else {
                        bytes
                    },
                )
                zip.closeEntry()
            }
        }

        val problem = problemFrom(
            ExportContainer.open(cut, File(work, "staging"), passphrase = secret)
        )
        assertTrue(
            "a payload cut short was not refused: ${problem.message}",
            problem is ExportContainer.Problem.CouldNotDecrypt,
        )
    }

    @Test
    fun theInnerLayerIsAnOrdinaryZipWithTheLayoutTheContractDraws() = runBlocking {
        val target = File(work, "export.htx")
        ExportContainer.write(
            target,
            source(fakeDatabase(), attachments = listOf(attachment("a photograph"))),
            passphrase = secret,
        )

        val opened = ExportContainer.open(target, File(work, "staging"), passphrase = secret)
        assertTrue("the archive did not open: ${opened.exceptionOrNull()}", opened.isSuccess)

        val inner = innerNames(target)
        assertTrue("no inner README", ExportContainer.INNER_README in inner)
        assertTrue("no inner manifest", ExportContainer.INNER_MANIFEST in inner)
        assertTrue("no checksums", ExportContainer.CHECKSUMS in inner)
        assertTrue("the database is not at data/trail.sqlite", ExportContainer.DATABASE in inner)
        assertTrue("the schema does not travel with it", ExportContainer.SCHEMA in inner)
        assertTrue(
            "the attachment is not in the inner container",
            inner.any { it.startsWith(ExportContainer.ATTACHMENTS) },
        )
    }

    /**
     * Rebuilds an archive with the inner container's files passed through a
     * transform, resealed under the same passphrase.
     *
     * **This is what makes a corruption test possible at all now.** Everything
     * that describes the record is inside the encryption, so damaging it means
     * opening the payload, changing something, and sealing it again, exactly the
     * way somebody with the passphrase and the specification could.
     */
    private fun rebuildPayload(source: File, target: File, change: (String, ByteArray) -> ByteArray) {
        val outer = java.util.zip.ZipInputStream(source.inputStream()).use { zip ->
            buildMap<String, ByteArray> {
                var entry = zip.nextEntry
                while (entry != null) {
                    put(entry.name, zip.readBytes())
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val encryption = org.json.JSONObject(
            outer.getValue(ExportContainer.OUTER_MANIFEST).decodeToString(),
        ).getJSONObject("encryption")
        val key = ExportCrypto.derive(
            passphrase = secret,
            salt = android.util.Base64.decode(encryption.getString("salt"), android.util.Base64.NO_WRAP),
            iterations = encryption.getInt("kdf_iterations"),
            memoryKib = encryption.getInt("kdf_memory_kib"),
            parallelism = encryption.getInt("kdf_parallelism"),
        )
        val prefix = android.util.Base64.decode(
            encryption.getString("nonce_prefix"), android.util.Base64.NO_WRAP,
        )

        val plain = unsealByHand(outer.getValue(ExportContainer.PAYLOAD), key, prefix)
        val rebuiltInner = java.io.ByteArrayOutputStream()
        ZipOutputStream(rebuiltInner).use { out ->
            java.util.zip.ZipInputStream(plain.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val bytes = zip.readBytes()
                    out.putNextEntry(ZipEntry(entry.name))
                    out.write(change(entry.name, bytes))
                    out.closeEntry()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val sealed = java.io.ByteArrayOutputStream()
        val body = rebuiltInner.toByteArray()
        val frames = maxOf(1, (body.size + ExportCrypto.CHUNK_BYTES - 1) / ExportCrypto.CHUNK_BYTES)
        for (index in 0 until frames) {
            val from = index * ExportCrypto.CHUNK_BYTES
            val piece = body.copyOfRange(from, minOf(body.size, from + ExportCrypto.CHUNK_BYTES))
            val frame = ExportCrypto.encrypt(
                key, ExportCrypto.chunkNonce(prefix, index.toLong()), piece,
                ExportCrypto.frameAad(index.toLong(), index == frames - 1),
            )
            sealed.write(
                byteArrayOf(
                    (frame.size ushr 24).toByte(), (frame.size ushr 16).toByte(),
                    (frame.size ushr 8).toByte(), frame.size.toByte(),
                ),
            )
            sealed.write(frame)
        }
        ExportCrypto.wipe(key)

        ZipOutputStream(target.outputStream()).use { zip ->
            outer.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(if (name == ExportContainer.PAYLOAD) sealed.toByteArray() else bytes)
                zip.closeEntry()
            }
        }
    }

    /** The frames, back into the inner container. The reader's half of the format. */
    private fun unsealByHand(sealed: ByteArray, key: ByteArray, prefix: ByteArray): ByteArray {
        val plain = java.io.ByteArrayOutputStream()
        var at = 0
        var index = 0L
        while (at < sealed.size) {
            val size = ((sealed[at].toInt() and 0xFF) shl 24) or
                ((sealed[at + 1].toInt() and 0xFF) shl 16) or
                ((sealed[at + 2].toInt() and 0xFF) shl 8) or
                (sealed[at + 3].toInt() and 0xFF)
            at += 4
            val frame = sealed.copyOfRange(at, at + size)
            at += size
            val last = at >= sealed.size
            plain.write(
                ExportCrypto.decrypt(
                    key, ExportCrypto.chunkNonce(prefix, index), frame,
                    ExportCrypto.frameAad(index, last),
                ),
            )
            index += 1
        }
        return plain.toByteArray()
    }

    /**
     * The inner container's entry names, by unsealing the payload by hand.
     *
     * **Deliberately not through `open`**, which reads the inner zip and deletes
     * it. This walks the published format the way `tools/decrypt/` does: read
     * the outer manifest, derive the key from what the file says, then read
     * frames. If this ever stops working, the standalone tool has stopped
     * working too, and that is the promise the whole two-layer shape is for.
     */
    private fun innerNames(target: File): List<String> {
        val outer = java.util.zip.ZipInputStream(target.inputStream()).use { zip ->
            buildMap<String, ByteArray> {
                var entry = zip.nextEntry
                while (entry != null) {
                    put(entry.name, zip.readBytes())
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val encryption = org.json.JSONObject(
            outer.getValue(ExportContainer.OUTER_MANIFEST).decodeToString(),
        ).getJSONObject("encryption")
        val key = ExportCrypto.derive(
            passphrase = secret,
            salt = android.util.Base64.decode(encryption.getString("salt"), android.util.Base64.NO_WRAP),
            iterations = encryption.getInt("kdf_iterations"),
            memoryKib = encryption.getInt("kdf_memory_kib"),
            parallelism = encryption.getInt("kdf_parallelism"),
        )
        val prefix = android.util.Base64.decode(
            encryption.getString("nonce_prefix"), android.util.Base64.NO_WRAP,
        )
        val plain = unsealByHand(outer.getValue(ExportContainer.PAYLOAD), key, prefix)
        ExportCrypto.wipe(key)
        return java.util.zip.ZipInputStream(plain.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.toList()
        }
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
