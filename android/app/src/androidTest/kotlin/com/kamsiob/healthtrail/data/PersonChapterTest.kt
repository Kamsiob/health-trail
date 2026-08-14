package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Somebody added to the care team belongs to the chapter she is in. #143, #371.
 *
 * **`person_chapter` was read and never written**, which is the audit's root
 * cause in its purest form: the chapters screen counts who was involved in each
 * chapter, the table exists because a nurse at the rehab is a different row from
 * the same human at the nursing home, and **the only thing that had ever written
 * a row was the fixture generator**. So the count could be six in a screenshot
 * and zero in every notebook anybody actually kept.
 *
 * **The generator gained a writer for it earlier the same day, which made it
 * worse rather than better.** A fixture that fills a column the app cannot write
 * is precisely how a screen comes to look joined up and be empty, and that is
 * worth saying because the fixture change was mine.
 *
 * **The link is stamped at creation, from the chapter with no end date**, the
 * same way an entry, a document and an incident already are. Nobody is asked,
 * because asking "which chapter of her life is this nurse part of" is the
 * interface making the person understand how the app stores things, rule 20.
 */
@RunWith(AndroidJUnit4::class)
class PersonChapterTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(displayName = "Margaret", relationship = "Mom")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    @Test
    fun somebodyAddedDuringAchapterIsCountedInIt() = runBlocking {
        val chapterId = repository.createChapter(subjectId = subjectId, name = "Maplewood")

        repository.createPerson(subjectId = subjectId, displayName = "Angela Reyes")
        repository.createPerson(subjectId = subjectId, displayName = "Marcus Bell")

        assertEquals(
            "the chapter says nobody was involved, on a notebook where two people were " +
                "added while it was the current one",
            2,
            repository.chapterContents(subjectId)[chapterId]?.people,
        )
    }

    /**
     * **A notebook with no chapter still takes people**, and that is the case
     * worth guarding: rule 13 says partial is a finished state, so a care team
     * must not depend on somebody having named where they are.
     */
    @Test
    fun anotebookWithNoChapterStillTakesSomebody() = runBlocking {
        val personId = repository.createPerson(subjectId = subjectId, displayName = "Angela Reyes")

        assertEquals(1, repository.people(subjectId).size)
        assertEquals(personId, repository.people(subjectId).first().id)
        assertEquals(
            "there is no chapter, so there is nothing to count",
            0,
            repository.chapterContents(subjectId).size,
        )
    }

    /**
     * **An archived person leaves the count**, which the reader already does by
     * joining on `live_person` and testing `archived_at`. Kept here because that
     * join is the only thing standing between this count and the defect #145
     * was filed for, where a number on a card disagreed with the screen.
     */
    @Test
    fun anArchivedPersonLeavesTheChaptersCount() = runBlocking {
        val chapterId = repository.createChapter(subjectId = subjectId, name = "Maplewood")
        val personId = repository.createPerson(subjectId = subjectId, displayName = "Angela Reyes")

        assertEquals(1, repository.chapterContents(subjectId)[chapterId]?.people)

        repository.setPersonArchived(personId, true)

        assertEquals(
            "somebody retired from the care team is still counted in the chapter",
            null,
            repository.chapterContents(subjectId)[chapterId]?.people,
        )
    }
}
