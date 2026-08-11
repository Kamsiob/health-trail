package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A document's date is the person's, at their precision, and correctable.
 *
 * **The app used to invent it.** `NotebookShell` passed
 * `Edtf.day(LocalDate.now())` into every `createDocument` and `updateDocument`
 * did not touch the date at all, so a letter from three weeks ago was recorded
 * as arriving on the day somebody photographed it, and nothing anywhere could
 * change that. #339.
 *
 * **Nothing looked wrong from any direction**, which is why this is held here
 * rather than by a walk. The documents list, the document's own screen, a
 * project's papers and the readable archive all render `received_edtf`, and all
 * four rendered something plausible. The fixture sets real dates, so a seeded
 * notebook shows a sensible date on every row. **The only way to see it was to
 * save a document by hand and read back what the app decided.**
 *
 * Rule 17: dates are flexible, always editable, and never falsely precise.
 * `DESIGN.md` 9.2: whatever the person expresses is recorded at exactly that
 * precision and no finer, and display never invents precision.
 */
@RunWith(AndroidJUnit4::class)
class DocumentDateTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private suspend fun oneDocument(repository: Repository, subjectId: String) =
        repository.documents(subjectId).single()

    /**
     * **A month stays a month.** The commonest real case is a letter somebody
     * can date only to when it turned up, and a day recorded for it would be a
     * precision nobody gave.
     */
    @Test
    fun theStoredDateIsExactlyThePrecisionGiven() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Precision")

        repository.createDocument(
            subjectId = subjectId,
            title = "The letter about the room change",
            received = Edtf.parse("2026-03")!!,
        )

        assertEquals("2026-03", oneDocument(repository, subjectId).receivedEdtf)
    }

    /**
     * **Unknown saves, and it is a value rather than an absence.**
     *
     * Rule 17 makes unknown first class: an entry with an unknown date saves,
     * is valid, and appears. What it must never be is quietly filled in with
     * today, which is what this replaced.
     */
    @Test
    fun unknownSavesAsUnknownAndNotAsToday() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Unknown")

        repository.createDocument(
            subjectId = subjectId,
            title = "A page with no date on it anywhere",
            received = Edtf.unknown(),
        )

        val stored = oneDocument(repository, subjectId).receivedEdtf
        assertEquals(Edtf.unknown().canonical, stored)
    }

    /**
     * **Correctable forever, which is the half that did not exist at all.**
     *
     * `updateDocument` took a title, a location, notes and a folder, and no
     * date, so a document dated wrongly stayed dated wrongly for as long as the
     * notebook existed.
     */
    @Test
    fun theDateCanBeCorrectedAfterwards() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Correction")

        val id = repository.createDocument(
            subjectId = subjectId,
            title = "Admission agreement",
            received = Edtf.unknown(),
        )

        repository.updateDocument(
            documentId = id,
            title = "Admission agreement",
            originalLocation = null,
            notes = null,
            received = Edtf.parse("2026-02-14"),
        )

        assertEquals("2026-02-14", oneDocument(repository, subjectId).receivedEdtf)
    }

    /**
     * **A correction that says nothing about the date leaves it alone.**
     *
     * Fixing a typo in a title must not reach the date, which is the reason
     * `received` is null by default rather than a value the caller has to
     * remember to pass back.
     */
    @Test
    fun correctingTheWordsDoesNotTouchTheDate() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Left alone")

        val id = repository.createDocument(
            subjectId = subjectId,
            title = "Resident rights notice",
            received = Edtf.parse("2026-01-09")!!,
        )

        repository.updateDocument(
            documentId = id,
            title = "Resident rights notice, signed",
            originalLocation = "The blue folder",
            notes = null,
        )

        val document = oneDocument(repository, subjectId)
        assertEquals("Resident rights notice, signed", document.title)
        assertEquals("2026-01-09", document.receivedEdtf)
    }
}
