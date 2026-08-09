package com.kamsiob.healthtrail.data

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.kamsiob.healthtrail.time.Edtf

/**
 * Merging a real archive into a real notebook. `contract/DATA-CONTRACT.md` 8.3,
 * issue #211.
 *
 * **The rules live in `MergeTest`, which needs no device.** This is the half
 * that cannot: a live SQLCipher database, the schema's own change log triggers,
 * the conflict log, and the attachment store. What it proves is that the plan
 * `Merge` produces actually lands, atomically, and that the notebook afterward
 * is the union of the two rather than one of them.
 *
 * **Every case here is one somebody would hit.** A second phone with entries
 * this one has never seen. The same entry edited on both. A deletion made on
 * one phone that must not come back. And a file that cannot be placed, which
 * must change nothing at all.
 */
class MergeApplyTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var work: File
    private val secret get() = "a passphrase for the merge test".toCharArray()

    @Before
    fun setUp() {
        work = File(context.cacheDir, "merge").apply { deleteRecursively(); mkdirs() }
    }

    @After
    fun tearDown() {
        work.deleteRecursively()
    }

    /** A notebook with one subject and one entry, exported to a file. */
    private suspend fun archiveOf(build: suspend (Repository, String) -> Unit): ExportContainer.Opened {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret", relationship = "Mom")
        build(repository, subject)

        val archive = File(work, "out-${System.nanoTime()}.zip")
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val staging = File(work, "staging-${System.nanoTime()}")
        return ExportContainer.open(archive, staging, passphrase = secret).getOrThrow()
    }

    private suspend fun countOf(sql: String): Int =
        HealthTrailDatabase.open(context).database.rawQuery(sql, null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    @Test
    fun anEntryTheOtherPhoneHasArrivesAndNothingElseChanges() = runBlocking {
        // The ordinary case: two phones, each with something the other has not.
        val opened = archiveOf { repository, subject ->
            repository.createEntry(
                subjectId = subject, kind = "call",
                occurred = Edtf.parse("2026-08-01T14:30")!!,
                body = "Spoke to the night nurse",
            )
        }
        val before = countOf("SELECT count(*) FROM entry")

        val report = MergeApply.merge(context, opened, mergedAt = 1_785_000_100_000L).getOrThrow()

        assertEquals("nothing new arrives when the file came from this notebook",
            0, report.inserted)
        assertEquals(before, countOf("SELECT count(*) FROM entry"))
        assertTrue("a merge of a notebook with itself has no conflicts", report.conflicts == 0)
        assertTrue("and everything in it is unchanged", report.unchanged > 0)
    }

    @Test
    fun theLaterEditWinsAndTheOtherVersionIsKeptWhole() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret", relationship = "Mom")
        val entry = repository.createEntry(
            subjectId = subject, kind = "note",
            occurred = Edtf.parse("2026-08-01")!!, body = "one version of a note",
        )

        // An archive taken now, then the row edited here, so this phone holds
        // the later version and the file holds the earlier one.
        val archive = File(work, "earlier.zip")
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val opened = ExportContainer.open(archive, File(work, "s1"), passphrase = secret).getOrThrow()

        // An ordinary edit through the app's own path, which bumps updated_at
        // and rev exactly as any edit does.
        repository.setEntryPinned(entry, pinned = true)

        val conflictsBefore = countOf("SELECT count(*) FROM conflict_log")
        val report = MergeApply.merge(context, opened, mergedAt = 1_785_000_100_000L).getOrThrow()

        assertEquals("this phone's later version is kept", 0, report.updated)
        assertTrue("and the person is told a second version existed", report.conflicts >= 1)
        assertTrue(countOf("SELECT count(*) FROM conflict_log") > conflictsBefore)

        val pinned = HealthTrailDatabase.open(context).database.rawQuery(
            // allow-base-table: asserting on the row itself, tombstone or not.
            "SELECT pinned_at FROM entry WHERE id = ?", arrayOf(entry),
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
        assertNotNull("this phone's later edit survived the merge", pinned)

        val kept = HealthTrailDatabase.open(context).database.rawQuery(
            "SELECT winner, reason, local_json, incoming_json FROM conflict_log " +
                "ORDER BY seq DESC LIMIT 1",
            null,
        ).use {
            it.moveToFirst()
            listOf(it.getString(0), it.getString(1), it.getString(2), it.getString(3))
        }
        assertEquals("local", kept[0])
        assertEquals(Merge.Reason.NEWER, kept[1])
        assertTrue("both sides are kept whole", kept[2].contains("one version of a note"))
        assertTrue(kept[3].contains("one version of a note"))
        assertTrue("and they differ, which is why there was a conflict", kept[2] != kept[3])
    }

    @Test
    fun aDeletionDoesNotComeBack() = runBlocking {
        // A resolved incident that reopens is named in 8.3 as a failure. The
        // same argument applies to anything removed: a merge that resurrects it
        // has undone something the person did on purpose.
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret", relationship = "Mom")
        val entry = repository.createEntry(
            subjectId = subject, kind = "note",
            occurred = Edtf.parse("2026-08-01")!!, body = "written then removed",
        )

        val archive = File(work, "before-delete.zip")
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val opened = ExportContainer.open(archive, File(work, "s2"), passphrase = secret).getOrThrow()

        repository.delete(Repository.Section.TRAIL, entry)
        assertNotNull("the tombstone is here", tombstone(entry))

        MergeApply.merge(context, opened, mergedAt = 1_785_000_100_000L).getOrThrow()

        assertNotNull("the file held the row alive and must not have put it back", tombstone(entry))
    }

    private suspend fun tombstone(id: String): Long? =
        HealthTrailDatabase.open(context).database.rawQuery(
            // allow-base-table: the point of the assertion is the tombstone,
            // which the live view exists to hide.
            "SELECT deleted_at FROM entry WHERE id = ?", arrayOf(id),
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }

    @Test
    fun aMergeThatCannotPlaceARowChangesNothing() = runBlocking {
        // 8.3: references resolve or the import stops, naming the row and the
        // missing parent. Never a quietly dropped child, never an invented
        // placeholder parent.
        val opened = archiveOf { repository, subject ->
            repository.createEntry(
                subjectId = subject, kind = "note",
                occurred = Edtf.parse("2026-08-01")!!, body = "an entry",
            )
        }
        // Break one reference inside the staged file, which is exactly the
        // shape of an archive that has been altered or truncated.
        net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            opened.database.path, null,
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
        ).use { it.execSQL("UPDATE entry SET chapter_id = 'no-such-chapter'") }

        val entriesBefore = countOf("SELECT count(*) FROM entry")
        val conflictsBefore = countOf("SELECT count(*) FROM conflict_log")

        val result = MergeApply.merge(context, opened, mergedAt = 1_785_000_100_000L)

        assertTrue("the merge must refuse", result.isFailure)
        val refused = result.exceptionOrNull() as MergeApply.Refused
        assertTrue("and name what it could not place",
            refused.dangling.any { it.parentTable == "chapter" })
        assertEquals("nothing was written", entriesBefore, countOf("SELECT count(*) FROM entry"))
        assertEquals(conflictsBefore, countOf("SELECT count(*) FROM conflict_log"))
    }

    @Test
    fun theTablesThatAreNotMergedAreNamedInTheReport() = runBlocking {
        val opened = archiveOf { repository, subject ->
            repository.createEntry(
                subjectId = subject, kind = "note",
                occurred = Edtf.parse("2026-08-01")!!, body = "an entry",
            )
        }
        val report = MergeApply.merge(context, opened, mergedAt = 1_785_000_100_000L).getOrThrow()
        assertTrue("a reader of the report is never left wondering",
            report.skipped.containsKey("change_log"))
        assertNull("and nothing in it is unexplained",
            report.skipped.values.firstOrNull { it.isBlank() })
    }
}
