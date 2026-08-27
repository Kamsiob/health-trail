package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A permanent delete takes the row and everything it left behind. #465.
 *
 * **The one write in this app that is not a tombstone**, so it is the one write
 * whose test has to prove absence rather than presence. Everything else in the
 * data layer is checked by looking for a row; here the assertion is that
 * nothing is left anywhere, and "anywhere" is the part that is easy to get
 * wrong. The row is the obvious half. The other four are its dependent rows,
 * its change log entries, its attachment bytes, and the archive, and each of
 * them was a real hole in the first version of this feature:
 *
 * - **The dependents.** Foreign keys here are `NO ACTION` and enforced on the
 *   connection, and a tombstoned child is still a row, so deleting a parent
 *   without them does not leave a mess, it throws.
 * - **The change log.** It carries no content and it is still a durable record
 *   that a row with that id existed, was written n times and was deleted at a
 *   particular moment, and `sqlcipher_export` copies it into every archive.
 * - **The bytes.** The attachment store is content addressed, so a file has to
 *   go only when no row is left naming its hash, tombstones included.
 *
 * `contract/DATA-CONTRACT.md` 3 is the rule these assert, and it was amended by
 * the owner for this feature rather than worked around.
 */
@RunWith(AndroidJUnit4::class)
class PurgeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var archive: File

    @Before
    fun setUp() {
        Repository.closeForTest()
        archive = File(context.cacheDir, "purge-${System.nanoTime()}.htz")
    }

    @After
    fun tearDown() {
        archive.delete()
        Repository.closeForTest()
    }

    private fun countLog(table: String, rowId: String): Int = runBlocking {
        HealthTrailDatabase.open(context).database.rawQuery(
            "SELECT COUNT(*) FROM change_log WHERE table_name = ? AND row_id = ?",
            arrayOf(table, rowId),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    @Test
    fun aPurgedRowIsGoneFromTheTableAndFromTheChangeLog() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")
        val kept = repository.createEntry(subjectId = subjectId, kind = "call", title = "Kept")
        val gone = repository.createEntry(subjectId = subjectId, kind = "call", title = "Gone")

        repository.delete(Repository.Section.TRAIL, gone)
        assertTrue("the change log did not record the row", countLog("entry", gone) > 0)

        repository.purge("entry", gone)

        // allow-base-table: absence in the base table is the whole assertion,
        // and the live view hid this row the moment it was deleted.
        assertFalse(
            "the row is still in the table",
            repository.rowExistsForTest("entry", gone),
        )
        assertEquals("the change log still names it", 0, countLog("entry", gone))
        assertTrue(
            "purging one thing took another with it",
            repository.rowExistsForTest("entry", kept),
        )
        assertTrue("the change log for the kept row went too", countLog("entry", kept) > 0)
    }

    @Test
    fun everythingThatOnlyExistedBecauseOfItGoesWithIt() = runBlocking {
        // A call entry owns a `call_detail` row whose `entry_id` is NOT NULL.
        // Without the cascade the delete does not leave a mess, it throws on
        // the foreign key, which is the failure this feature would have had on
        // the first row anybody tried it on.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "About the discharge",
        )
        repository.addCallDetail(entryId, reached = true, outcome = "They will call back")

        assertEquals(
            "the call detail was not written",
            1,
            childCount("call_detail", "entry_id", entryId),
        )

        repository.delete(Repository.Section.TRAIL, entryId)
        val purged = repository.purge("entry", entryId)

        assertTrue("nothing was taken", purged.rows >= 2)
        assertFalse(
            "the entry survived",
            repository.rowExistsForTest("entry", entryId),
        )
        assertEquals(
            "a child row was left pointing at nothing",
            0,
            childCount("call_detail", "entry_id", entryId),
        )
    }

    @Test
    fun aReferenceThatMayBeAbsentIsClearedRatherThanFollowed() = runBlocking {
        // An entry may belong to a chapter, and `entry.chapter_id` is nullable.
        // Purging the chapter must not take the entries filed against it: they
        // are the person's record and the chapter was a label on them.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")
        val chapterId = repository.moveToChapter(subjectId, "Maplewood")
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Called the ward",
            chapterId = chapterId,
        )

        repository.delete(Repository.Section.CHAPTERS, chapterId)
        repository.purge("chapter", chapterId)

        assertFalse(
            "the chapter survived",
            repository.rowExistsForTest("chapter", chapterId),
        )
        assertTrue(
            "an entry was deleted along with the chapter it was filed under",
            repository.rowExistsForTest("entry", entryId),
        )
        assertEquals(
            "the entry still points at a chapter that does not exist",
            null,
            repository.columnForTest("entry", entryId, "chapter_id"),
        )
    }

    @Test
    fun theBytesGoOnlyWhenNothingElseNamesThem() = runBlocking {
        val repository = Repository.open(context)
        val store = Attachments.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")

        val bytes = "a photograph of a letter".toByteArray()
        val hash = store.put(bytes)

        val first = repository.createDocument(
            subjectId = subjectId,
            title = "The letter",
            received = Edtf.day(java.time.LocalDate.of(2026, 8, 27)),
            sha256 = hash,
            byteSize = bytes.size.toLong(),
        )
        val second = repository.createDocument(
            subjectId = subjectId,
            title = "The same letter",
            received = Edtf.day(java.time.LocalDate.of(2026, 8, 27)),
            sha256 = hash,
            byteSize = bytes.size.toLong(),
        )

        repository.delete(Repository.Section.DOCUMENTS, first)
        val firstPurge = repository.purge("document", first)

        assertEquals("the file went while another row still named it", 0, firstPurge.files)
        assertTrue("the bytes were deleted too early", store.exists(hash))

        repository.delete(Repository.Section.DOCUMENTS, second)
        val secondPurge = repository.purge("document", second)

        assertEquals("the last row went and the file stayed", 1, secondPurge.files)
        assertFalse("the bytes are still on disk", store.exists(hash))
    }

    @Test
    fun aPurgedRowIsAbsentFromAFreshArchive() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")
        val gone = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "A call nobody should find again",
        )

        repository.delete(Repository.Section.TRAIL, gone)
        repository.purge("entry", gone)

        val passphrase = "a passphrase for the purge round trip".toCharArray()
        Backup.export(
            context,
            archive,
            exportedAt = 1_754_000_000_000L,
            passphrase = passphrase.copyOf(),
        )
        val staging = File(context.cacheDir, "purge-open-${System.nanoTime()}")
        try {
            val opened = ExportContainer
                .open(archive, staging, passphrase.copyOf())
                .getOrThrow()
            assertTrue("the archive has no payload to read", opened.database.isFile)
            val raw = opened.database.readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(
                "the purged row's id is still somewhere in the archive",
                gone in raw,
            )
        } finally {
            staging.deleteRecursively()
        }
    }

    @Test
    fun somethingElseCanStillBePutBack() = runBlocking {
        // The other half of the screen has to keep working, and a restore after
        // a purge is the case where a wrong id would show up.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")
        val gone = repository.createEntry(subjectId = subjectId, kind = "call", title = "Gone")
        val back = repository.createEntry(subjectId = subjectId, kind = "call", title = "Back")

        repository.delete(Repository.Section.TRAIL, gone)
        repository.delete(Repository.Section.TRAIL, back)
        repository.purge("entry", gone)
        repository.restore("entry", back)

        val listed = repository.discarded(subjectId).map { it.id }
        assertFalse("a purged row is still listed", gone in listed)
        assertFalse("a restored row is still listed", back in listed)
        assertTrue("the restored row did not come back", repository.rowExistsForTest("entry", back))
    }

    @Test
    fun aDeletedMemoIsListedOnceRatherThanTwice() = runBlocking {
        // `Section.TRAIL` and `Section.NOTES` are both the `entry` table, so a
        // screen iterating sections showed every deleted memo twice, both rows
        // pointing at the same thing. Permanently deleting one of them would
        // have left the other naming a row that no longer exists.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Purge subject")
        val memoId = repository.createEntry(
            subjectId = subjectId,
            kind = "note",
            title = "Bring her glasses",
        )

        repository.delete(Repository.Section.NOTES, memoId)

        val listed = repository.discarded(subjectId).filter { it.id == memoId }
        assertEquals("a deleted memo is listed more than once: $listed", 1, listed.size)
        assertEquals(
            "a memo is listed as the trail rather than as memos",
            Repository.Section.NOTES,
            listed.first().section,
        )
    }

    private fun childCount(table: String, column: String, parentId: String): Int = runBlocking {
        HealthTrailDatabase.open(context).database.rawQuery(
            // allow-base-table: counting rows that should not exist at all.
            "SELECT COUNT(*) FROM \"$table\" WHERE \"$column\" = ?",
            arrayOf(parentId),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }
}
