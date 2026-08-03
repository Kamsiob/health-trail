package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Finding one thing in a notebook that has years in it.
 *
 * **Written through the repository's own create methods**, not through SQL, per
 * `TESTING-PERSONAS.md` section 7. A test that inserts rows by hand proves the
 * query against a shape the app might not actually write, and this project has
 * paid for that reading three times.
 *
 * **The first run found a real defect immediately**, which is the argument for
 * covering all ten sections rather than the one being demonstrated: `person`,
 * `medication`, and `question` carry no `chapter_id`, because somebody on the
 * care team spans several stays and a medication crosses chapters by design.
 * Joining `x.chapter_id` blindly threw "no such column" and took the entire
 * search down, every section of it, on any query at all.
 */
@RunWith(AndroidJUnit4::class)
class SearchTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(
            displayName = "Search fixture",
            relationship = "mother",
        )
    }

    @After
    fun tearDown() {
        Repository.closeForTest()
    }

    private fun find(query: String) = runBlocking { repository.search(subjectId, query) }

    @Test
    fun everySectionIsSearchedRatherThanTheOneBeingDemonstrated() = runBlocking {
        // **The defect this catches took the whole search down**, not one
        // section, because a broken query in any one of the ten threw and the
        // caller reported it as finding nothing. A search that says "no match"
        // when it never ran tells somebody their record does not contain what
        // they are certain they wrote down.
        //
        // A word that appears nowhere, so every section runs and every one
        // returns empty rather than throwing.
        val hits = find("zzzzzzunlikelyzzzzz")
        assertEquals("a search for nothing should find nothing, not throw", 0, hits.size)
    }

    @Test
    fun aWordFromAnEntryFindsThatEntryUnderTheTrail() = runBlocking {
        repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Nurse Okonkwo, ward 4",
            body = "Said the dressing looks better",
        )

        val hits = find("okonkwo")
        assertEquals(1, hits.size)
        assertEquals(Repository.Section.TRAIL, hits.first().section)
        assertEquals("Nurse Okonkwo, ward 4", hits.first().title)
    }

    @Test
    fun theWordIsFoundInTheBodyAsWellAsTheTitle() = runBlocking {
        // The note is where somebody writes what was actually said, and it is
        // the thing they will search for months later.
        repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "The ward",
            body = "They mentioned a swallowing assessment on Thursday",
        )

        val hits = find("swallowing")
        assertEquals(1, hits.size)
        assertEquals("The ward", hits.first().title)
    }

    @Test
    fun caseIsIgnoredBecauseNobodyTypesCapitalsFromMemory() = runBlocking {
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Dr Aurelio Sandoval")

        assertEquals(1, find("aurelio").size)
        assertEquals(1, find("AURELIO").size)
        assertEquals(1, find("AuReLiO").size)
    }

    @Test
    fun aPersonIsFoundByNameAndByWhatTheyDo() = runBlocking {
        repository.createPerson(
            subjectId = subjectId,
            displayName = "Marguerite Boateng",
            phone = "555 0134",
        )

        val byName = find("boateng")
        assertEquals(1, byName.size)
        assertEquals(Repository.Section.CARE_TEAM, byName.first().section)

        // The number counts too. Somebody who remembers only the last digits of
        // a line they called is still looking for that person.
        assertEquals(1, find("0134").size)
    }

    @Test
    fun aDeletedThingIsNotFound() = runBlocking {
        // Read through the live views, which is what `check_live_views.py`
        // enforces and what makes a deletion actually a deletion.
        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Should not be findable",
        )
        assertEquals(1, find("findable").size)

        repository.delete(Repository.Section.TRAIL, id)
        assertEquals("a deleted entry came back in a search", 0, find("findable").size)
    }

    @Test
    fun anEmptyQueryFindsNothingRatherThanEverything() = runBlocking {
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Anything at all")

        assertEquals(0, find("").size)
        assertEquals("whitespace is not a query", 0, find("   ").size)
    }

    @Test
    fun aWildcardIsSearchedForRatherThanActedOn() = runBlocking {
        // **`%` and `_` are literal characters to the person typing them.** They
        // turn up in doses and in file names, and a search for one that quietly
        // matched everything would be the app doing something the person did
        // not ask for on the screen where they are looking for one thing.
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Down to 50% strength")
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Nothing special here")

        val hits = find("50%")
        assertEquals(1, hits.size)
        assertEquals("Down to 50% strength", hits.first().title)

        // A bare percent must not become "match everything".
        assertEquals("a bare wildcard matched rows that do not contain it", 1, find("%").size)
    }

    @Test
    fun resultsComeBackInNotebookOrderRatherThanByCount() = runBlocking {
        // The order the person learned from the table of contents. A list whose
        // order changes with the query is one nobody can build a habit against.
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Ashford note")
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Ashford again")
        repository.createPerson(subjectId = subjectId, displayName = "Ashford", phone = "")

        val sections = find("ashford").map { it.section }.distinct()
        assertEquals(
            "care team comes before the trail in the notebook, and so should its results",
            listOf(Repository.Section.CARE_TEAM, Repository.Section.TRAIL),
            sections,
        )
    }

    @Test
    fun aResultCarriesTheChapterItHappenedIn() = runBlocking {
        // `MASTER_SPEC.md` 4.8: every result carries its chapter, so the person
        // always knows where in the journey it happened.
        val chapterId = repository.createChapter(subjectId, "Riverbend Rehab")
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Care plan meeting set",
            chapterId = chapterId,
        )
        assertTrue(entryId.isNotBlank())

        val hit = find("care plan").first()
        assertEquals("Riverbend Rehab", hit.chapterName)
    }

    @Test
    fun somethingWithNoChapterSaysNothingRatherThanInventingOne() = runBlocking {
        repository.createEntry(subjectId = subjectId, kind = "call", title = "Logged in a hallway")

        val hit = find("hallway").first()
        assertEquals(
            "a result with no chapter should carry null, never a placeholder",
            null,
            hit.chapterName,
        )
    }
}
