package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
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
 * A notebook exported and put back is the same notebook.
 *
 * **This is the test the whole project's testing argument rests on, and it did
 * not exist until now.** B4 dropped the emulator from this project on explicit
 * reasoning: data survival is not proven by a long lived installation on one
 * phone, which is a sample of one nobody can reproduce, but by the export and
 * import round trip against shared vectors in continuous integration.
 *
 * That reasoning was right and the test it named was never written, so for the
 * whole life of the project **nothing proved that a person's records survive an
 * update at all.** Every other test proves a part in isolation.
 *
 * Unencrypted, deliberately. The work order puts this before encryption
 * precisely so the central claim stops being unproven while a dependency
 * decision is settled.
 */
@RunWith(AndroidJUnit4::class)
class RoundTripTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var archive: File

    @Before
    fun setUp() {
        Repository.closeForTest()
        archive = File(context.cacheDir, "round-trip-${System.nanoTime()}.htz")
    }

    @After
    fun tearDown() {
        archive.delete()
        Repository.closeForTest()
    }

    /**
     * A notebook with something of every awkward shape in it.
     *
     * **Deliberately not tidy data.** A round trip over one clean row proves
     * almost nothing. What matters is the rows that are easy to lose: a date
     * whose precision is coarser than a day, an unknown date, a tombstone, and
     * an entry with no thread.
     */
    private suspend fun seed(): Seeded {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret", relationship = "Mom")

        val precise = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2026-08-01T14:30")!!,
            body = "Spoke to the night nurse about the dressing",
        )
        // A month, which must never collapse to its first day.
        val month = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2024-11")!!,
            body = "The fall was sometime in November",
        )
        // Unknown, which is a first class value and must survive as one.
        val unknown = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse(Edtf.UNKNOWN)!!,
            body = "It happened and nobody knows when",
        )
        // Uncertain, where the qualifier is part of the string.
        val uncertain = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2025-03?")!!,
            body = "I think it was March",
        )
        val deleted = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2026-07-14")!!,
            body = "This one gets deleted",
        )
        repository.delete(Repository.Section.TRAIL, deleted)

        return Seeded(subject, listOf(precise, month, unknown, uncertain), month, unknown, uncertain, deleted)
    }

    /**
     * The ids, because the tests key off them rather than off values.
     *
     * **The database persists across tests in this class**, since it is the
     * app's real one, so each `seed` adds another set of rows. Looking a row up
     * by its EDTF string finds every previous run's copy too, which fails as
     * "expected one, found three" and reads like a round trip defect rather
     * than a test isolation one. An id is unambiguous.
     */
    private data class Seeded(
        val subject: String,
        val entries: List<String>,
        val month: String,
        val unknown: String,
        val uncertain: String,
        val deleted: String,
    )

    /** Every user table and row, read straight out of the database. */
    private suspend fun snapshot(): Map<String, List<Map<String, String?>>> {
        val database = HealthTrailDatabase.open(context).database
        return Backup.userTables(database).associateWith { table ->
            // allow-base-table: a round trip has to compare tombstones too. A
            // live view would hide exactly the rows most likely to be lost.
            database.rawQuery("SELECT * FROM $table ORDER BY id", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            (0 until cursor.columnCount).associate {
                                cursor.getColumnName(it) to
                                    if (cursor.isNull(it)) null else cursor.getString(it)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Export and restore, optionally through encryption.
     *
     * **Every export is encrypted, because since format version 2 that is the
     * only kind there is**, D67. This class used to run every assertion twice,
     * once plain and once encrypted, on the reasoning that a suite exercising
     * only the unencrypted path would prove the round trip for a file nobody
     * ships. That reasoning now points the other way: the unencrypted path is
     * the one nobody ships, so the plain half proved nothing and was removed
     * rather than kept for symmetry.
     */
    /**
     * The passphrase the ordinary round trips use.
     *
     * It is a constant rather than a parameter with a null default because
     * there is no unencrypted export to default to. D67.
     */
    private val DEFAULT_PASSPHRASE = "a passphrase for the round trip"

    private suspend fun roundTrip(passphrase: String = DEFAULT_PASSPHRASE) {
        Backup.export(
            context,
            archive,
            exportedAt = 1_754_000_000_000L,
            passphrase = passphrase.toCharArray(),
        )
        val staging = File(context.cacheDir, "restore-${System.nanoTime()}")
        val opened = ExportContainer
            .open(archive, staging, passphrase.toCharArray())
            .getOrThrow()
        Backup.restore(context, opened).getOrThrow()
        staging.deleteRecursively()
    }

    private val secret = "correct horse battery staple"

    @Test
    fun everyRowAndEveryColumnComesBackIdentical() = runBlocking {
        seed()
        val before = snapshot()
        roundTrip()
        val after = snapshot()

        assertEquals("the set of tables changed", before.keys, after.keys)
        for (table in before.keys) {
            assertEquals(
                "$table lost or gained rows across the round trip",
                before.getValue(table).size,
                after.getValue(table).size,
            )
            before.getValue(table).zip(after.getValue(table)).forEachIndexed { row, (a, b) ->
                assertEquals("$table row $row differs", a, b)
            }
        }
    }

    @Test
    fun theEdtfColumnSurvivesByteForByte() = runBlocking {
        // The one the format names explicitly. Every other date behavior in the
        // app is derived from this string, so a round trip that reformats it,
        // normalizes it, or resolves it has changed what the person said.
        val seeded = seed()
        val expected = seeded.entries.associateWith { entryEdtf(it) }
        assertTrue("the fixture did not produce any dates", expected.isNotEmpty())

        roundTrip()

        expected.forEach { (id, edtf) ->
            assertEquals("the EDTF string changed across the round trip", edtf, entryEdtf(id))
        }
    }

    @Test
    fun aMonthNeverCollapsesToItsFirstDayAcrossTheRoundTrip() = runBlocking {
        val seeded = seed()
        roundTrip()
        val month = entryRow(seeded.month)!!

        assertEquals("2024-11", month["occurred_edtf"])
        // Its range is the whole month, not one day. A collapse would show as a
        // start and end inside the same day.
        val start = month["occurred_start"]!!.toLong()
        val end = month["occurred_end"]!!.toLong()
        assertTrue("the month collapsed to a single day", end - start > 27L * 86_400_000L)
    }

    @Test
    fun unknownSurvivesAsUnknownRatherThanAsNullOrToday() = runBlocking {
        val seeded = seed()
        roundTrip()
        val unknown = entryRow(seeded.unknown)!!

        assertEquals(Edtf.UNKNOWN, unknown["occurred_edtf"])
        assertEquals("unknown gained a start", null, unknown["occurred_start"])
        assertEquals("unknown gained an end", null, unknown["occurred_end"])
    }

    @Test
    fun theUncertaintyQualifierIsNotStrippedOnTheWayThrough() = runBlocking {
        val seeded = seed()
        roundTrip()
        assertEquals("2025-03?", entryRow(seeded.uncertain)!!["occurred_edtf"])
    }

    @Test
    fun theDerivedRangeIsRecomputedOnImportRatherThanTrusted() = runBlocking {
        // The format's actual requirement, and the only way to test it is to
        // put a wrong range in the file and watch the import correct it.
        //
        // This is not hypothetical: a file written by a build whose resolution
        // had a bug would otherwise carry the wrong answer forever, and nothing
        // would flag it, because a range and an EDTF string each look plausible
        // on their own.
        val seeded = seed()
        val database = HealthTrailDatabase.open(context).database
        val target = seeded.month
        database.execSQL(
            "UPDATE entry SET occurred_start = 1, occurred_end = 2 WHERE id = ?",
            arrayOf<Any?>(target),
        )
        assertEquals("1", entryColumn(target, "occurred_start"))

        roundTrip()

        assertNotEquals(
            "the import trusted the range from the file instead of recomputing it",
            "1",
            entryColumn(target, "occurred_start"),
        )
        val start = entryColumn(target, "occurred_start")!!.toLong()
        val end = entryColumn(target, "occurred_end")!!.toLong()
        assertTrue("the recomputed range is not a month", end - start > 27L * 86_400_000L)
    }

    @Test
    fun tombstonesTravelSoADeletionIsNotResurrected() = runBlocking {
        // The last unmet criterion on #8. An export that drops tombstones
        // cannot restore a deletion, which means restoring a backup brings back
        // everything the person deleted, silently.
        val seeded = seed()
        roundTrip()

        val repository = Repository.open(context)
        assertTrue(
            "the deleted row did not survive as a tombstone, so a peer can never learn of it",
            repository.rowExistsForTest("entry", seeded.deleted),
        )
        assertEquals(
            "the deleted entry came back into a live read",
            null,
            entryColumn(seeded.deleted, "id", live = true),
        )
    }

    @Test
    fun theManifestDescribesWhatIsActuallyInTheFile() = runBlocking<Unit> {
        seed()
        val manifest = Backup.export(
            context,
            archive,
            exportedAt = 1_754_000_000_000L,
            passphrase = DEFAULT_PASSPHRASE.toCharArray(),
        ).manifest
        val staging = File(context.cacheDir, "manifest-${System.nanoTime()}")
        val opened = ExportContainer
            .open(archive, staging, DEFAULT_PASSPHRASE.toCharArray())
            .getOrThrow()

        assertEquals(ExportContainer.FORMAT_VERSION, opened.manifest.formatVersion)
        assertEquals(manifest.databaseSha256, opened.manifest.databaseSha256)
        // Tables with zero rows are present, which is what lets an import state
        // plainly what it is about to do.
        assertTrue(
            "the manifest omits empty tables, so a missing table is undetectable",
            opened.manifest.rowCounts.values.any { it == 0 },
        )
        assertTrue(
            "the manifest does not count the entries",
            opened.manifest.rowCounts.getValue("entry") >= 5,
        )
        staging.deleteRecursively()
    }

    @Test
    fun everyEdtfGroupInTheSchemaIsFoundRatherThanListed() = runBlocking {
        // A hard coded list of date columns would be a second declaration of
        // the schema, which D16 exists to prevent, and it would go stale the
        // first time a table was added.
        val database = HealthTrailDatabase.open(context).database
        val groups = Backup.edtfGroups(database)

        assertTrue("far fewer date groups than the schema defines: ${groups.size}", groups.size >= 25)
        assertTrue(
            "entry.occurred is not among the groups found",
            groups.any { it.first == "entry" && it.second == "occurred" },
        )
    }

    // -- the same notebook, through encryption ----------------------------

    @Test
    fun everyRowComesBackIdenticalThroughAnEncryptedFile() = runBlocking {
        // The assertion that matters once encryption exists. Encryption that
        // preserves the bytes is the only kind worth having.
        seed()
        val before = snapshot()
        roundTrip(passphrase = secret)
        val after = snapshot()

        assertEquals(before.keys, after.keys)
        for (table in before.keys) {
            assertEquals(
                "$table differs across the encrypted round trip",
                before.getValue(table),
                after.getValue(table),
            )
        }
    }

    @Test
    fun theEdtfColumnSurvivesEncryptionByteForByte() = runBlocking {
        val seeded = seed()
        val expected = seeded.entries.associateWith { entryEdtf(it) }
        roundTrip(passphrase = secret)
        expected.forEach { (id, edtf) ->
            assertEquals("the EDTF string changed through encryption", edtf, entryEdtf(id))
        }
    }

    @Test
    fun anEncryptedFileSaysSoAndRecordsHowItWasMade() = runBlocking {
        seed()
        val manifest = Backup.export(
            context, archive, exportedAt = 1_754_000_000_000L, passphrase = secret.toCharArray(),
        ).manifest

        assertTrue("the manifest does not say it is encrypted", manifest.encrypted)
        val parameters = manifest.encryption!!
        assertEquals("AES-256-GCM", parameters.algorithm)
        assertEquals("Argon2id", parameters.kdf)
        // Recorded so a file written today opens years from now against
        // whatever the shipped cost has become by then.
        assertEquals(ExportCrypto.ITERATIONS, parameters.iterations)
        assertEquals(ExportCrypto.MEMORY_KIB, parameters.memoryKib)
        assertTrue("no salt recorded", parameters.salt.isNotEmpty())
        assertTrue("no nonce prefix recorded", parameters.noncePrefix.isNotEmpty())
        // Recorded so a reader can size its buffers without guessing, and so a
        // later build can change the frame size without stranding this file.
        assertEquals(ExportCrypto.CHUNK_BYTES, parameters.chunkBytes)
    }

    @Test
    fun theManifestStaysReadableWithoutThePassphrase() = runBlocking<Unit> {
        // The format's rule: a reader has to be able to say what a file is
        // before it can ask for a passphrase.
        seed()
        Backup.export(
            context, archive, exportedAt = 1_754_000_000_000L, passphrase = secret.toCharArray(),
        )
        val staging = File(context.cacheDir, "peek-${System.nanoTime()}")

        val problem = ExportContainer.open(archive, staging).exceptionOrNull()
        val reason = (problem as ExportContainer.ExportProblem).problem

        assertTrue(
            "an encrypted file with no passphrase should ask rather than fail: $reason",
            reason is ExportContainer.Problem.PassphraseNeeded,
        )
        assertTrue("it does not say it is encrypted", "encrypted" in reason.message)
        staging.deleteRecursively()
    }

    @Test
    fun aWrongPassphraseIsRefusedWithoutClaimingToKnowWhy() = runBlocking<Unit> {
        // GCM cannot distinguish a wrong passphrase from a tampered file, so
        // the message must say both and claim neither.
        seed()
        Backup.export(
            context, archive, exportedAt = 1_754_000_000_000L, passphrase = secret.toCharArray(),
        )
        val staging = File(context.cacheDir, "wrong-${System.nanoTime()}")

        val problem = ExportContainer
            .open(archive, staging, "not the passphrase".toCharArray())
            .exceptionOrNull()
        val reason = (problem as ExportContainer.ExportProblem).problem

        assertTrue(reason is ExportContainer.Problem.CouldNotDecrypt)
        assertTrue("it does not say nothing changed", "Nothing was changed" in reason.message)
        assertTrue(
            "it claims to know which of the two went wrong",
            "no way to tell which" in reason.message,
        )
        staging.deleteRecursively()
    }

    @Test
    fun anEncryptedPayloadIsNotReadableAsPlainText() = runBlocking {
        // The whole point. If the notebook's words are findable in the bytes
        // of the file, nothing above matters.
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret")
        repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2026-08-01")!!,
            body = "the wound dressing was changed late again",
        )
        Backup.export(
            context, archive, exportedAt = 1_754_000_000_000L, passphrase = secret.toCharArray(),
        )

        val raw = archive.readBytes().toString(Charsets.ISO_8859_1)
        assertTrue(
            "the notebook's content is readable in the encrypted file",
            "wound dressing was changed late" !in raw,
        )
        // The manifest is deliberately readable without the passphrase, which
        // is how a reader tells what the file is before asking for anything.
        //
        // Read through the zip rather than searched for in the raw bytes: the
        // entry is deflated, so its text is not literally present, and an
        // earlier version of this assertion failed for that reason and looked
        // like the manifest had been encrypted along with the payload.
        val manifestText = java.util.zip.ZipInputStream(archive.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { it.name == ExportContainer.OUTER_MANIFEST }
                ?.let { zip.readBytes().decodeToString() }
        }
        assertTrue(
            "the manifest is not readable without the passphrase",
            manifestText != null && "format_version" in manifestText,
        )
        assertTrue(
            "the manifest does not declare the file encrypted",
            "\"encrypted\": true" in manifestText!!,
        )
    }

    // -- reading helpers --------------------------------------------------

    private suspend fun entryEdtf(id: String): String? = entryColumn(id, "occurred_edtf")

    private suspend fun entryColumn(
        id: String,
        column: String,
        live: Boolean = false,
    ): String? {
        val database = HealthTrailDatabase.open(context).database
        // allow-base-table: this test compares what is stored, tombstones
        // included, which is the whole subject of a round trip.
        val table = if (live) "live_entry" else "entry"
        return database.rawQuery("SELECT $column FROM $table WHERE id = ?", arrayOf(id)).use {
            if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
        }
    }

    private suspend fun entryRow(id: String): Map<String, String?>? {
        val database = HealthTrailDatabase.open(context).database
        return database.rawQuery(
            // allow-base-table: a round trip compares what is stored, including
            // the tombstoned row, which is exactly what a live view hides.
            "SELECT * FROM entry WHERE id = ?", arrayOf(id),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            (0 until cursor.columnCount).associate {
                cursor.getColumnName(it) to
                    if (cursor.isNull(it)) null else cursor.getString(it)
            }
        }
    }
}
