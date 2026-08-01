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

    private suspend fun roundTrip() {
        Backup.export(context, archive, exportedAt = 1_754_000_000_000L)
        val staging = File(context.cacheDir, "restore-${System.nanoTime()}")
        val opened = ExportContainer.open(archive, staging).getOrThrow()
        Backup.restore(context, opened).getOrThrow()
        staging.deleteRecursively()
    }

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
        val manifest = Backup.export(context, archive, exportedAt = 1_754_000_000_000L)
        val staging = File(context.cacheDir, "manifest-${System.nanoTime()}")
        val opened = ExportContainer.open(archive, staging).getOrThrow()

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
