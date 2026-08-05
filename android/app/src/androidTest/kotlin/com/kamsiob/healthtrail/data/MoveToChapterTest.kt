package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A change of situation is a chapter boundary, and a boundary has two sides.
 *
 * **Starting a place without ending the one before it is the defect this
 * exists for.** A chapter is current because it has no end date, so a second
 * one left the chapters screen showing two places somebody was in at once,
 * under a heading that says "where they are now". Seen on the phone.
 *
 * **And the other half: nothing is destroyed.** #202 promises it in those
 * words, so it is asserted rather than trusted, because the promise is the
 * reason somebody is willing to press the button at all.
 */
@RunWith(AndroidJUnit4::class)
class MoveToChapterTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(displayName = "Move fixture", relationship = "mother")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private fun chapters() = runBlocking { repository.chapters(subjectId) }

    @Test
    fun theOldPlaceEndsAndTheNewOneBegins() = runBlocking {
        repository.createChapter(subjectId, "Maplewood Care Center")
        repository.moveToChapter(subjectId, "Birchwood Assisted Living")

        val all = chapters()
        assertEquals(2, all.size)
        val current = all.filter { it.isCurrent }
        assertEquals(
            "exactly one place is current after a move: ${current.map { it.name }}",
            listOf("Birchwood Assisted Living"),
            current.map { it.name },
        )
    }

    @Test
    fun theOldPlaceKeepsItsNameAndItsStartDate() = runBlocking {
        repository.createChapter(subjectId, "Maplewood Care Center")
        val startedBefore = chapters().single().startedEdtf
        repository.moveToChapter(subjectId, "Birchwood Assisted Living")

        val old = chapters().single { it.name == "Maplewood Care Center" }
        assertEquals("the stay was rewritten rather than closed", startedBefore, old.startedEdtf)
        assertEquals(LocalDate.now().toString(), old.endedEdtf)
    }

    @Test
    fun nothingFiledAgainstTheOldPlaceIsTouched() = runBlocking {
        // The promise #202 makes in its own words, and the reason somebody is
        // willing to press the button. Asserted rather than trusted.
        val oldId = repository.createChapter(subjectId, "Maplewood Care Center")
        val before = repository.chapterDetail(subjectId, oldId)

        repository.moveToChapter(subjectId, "Birchwood Assisted Living")

        val after = repository.chapterDetail(subjectId, oldId)
        assertTrue("the old chapter itself was removed", after != null)
        assertEquals(before?.entries?.size, after?.entries?.size)
        assertEquals(before?.incidents?.size, after?.incidents?.size)
        assertEquals(before?.documents?.size, after?.documents?.size)
        assertEquals(before?.milestones?.size, after?.milestones?.size)
    }

    @Test
    fun aPlaceSomebodyAlreadyClosedKeepsTheDateTheyGaveIt() = runBlocking {
        // Only the open ones are ended. A stay closed by hand at some earlier
        // date must not be silently restamped with today's.
        repository.createChapter(subjectId, "First place")
        repository.moveToChapter(subjectId, "Second place")
        val firstEnded = chapters().single { it.name == "First place" }.endedEdtf

        repository.moveToChapter(subjectId, "Third place")

        assertEquals(
            "an already ended stay was restamped",
            firstEnded,
            chapters().single { it.name == "First place" }.endedEdtf,
        )
        assertEquals(
            listOf("Third place"),
            chapters().filter { it.isCurrent }.map { it.name },
        )
    }

    @Test
    fun movingWithNoPlaceRecordedYetJustStartsOne() = runBlocking {
        // Setup can be skipped, so a notebook may have no chapter at all. That
        // is not an error and the move is not blocked by it.
        repository.moveToChapter(subjectId, "The first place anybody wrote down")
        assertEquals(
            listOf("The first place anybody wrote down"),
            chapters().filter { it.isCurrent }.map { it.name },
        )
    }
}
