package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.text.Normalizer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Text reaches the database in NFC. `contract/DATA-CONTRACT.md` 8.4, issue #227.
 *
 * **"A name typed with a combining accent on one device and a precomposed
 * character on another is the same person, not two."** Nothing in the app
 * normalized anything, so the two forms were different byte sequences that look
 * identical in every screenshot and every log: search found one and missed the
 * other, and an import made a second row nobody could tell apart.
 *
 * **The contract names the three scripts to test with** and all three are here.
 */
@RunWith(AndroidJUnit4::class)
class NormalizationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repository: Repository
    private lateinit var subjectId: String

    /** "José" with a combining acute, which is what some keyboards emit. */
    private val decomposed = "José"

    /** The same name precomposed, which is what most emit. */
    private val composed = "José"

    @Before
    fun setUp() = runBlocking {
        Repository.closeForTest()
        repository = Repository.open(context)
        subjectId = repository.createSubject("Margaret")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    /** The premise: these are two different strings that look the same. */
    @Test
    fun theTwoFormsAreGenuinelyDifferentBytes() {
        assertNotEquals(decomposed, composed)
        assertEquals(5, decomposed.length)
        assertEquals(4, composed.length)
    }

    @Test
    fun aNameWrittenDecomposedComesBackComposed() = runBlocking {
        val id = repository.createPerson(subjectId, decomposed, roleLabel = "Social worker")
        val stored = repository.people(subjectId).first { it.id == id }
        assertEquals(composed, stored.displayName)
        assertTrue(Normalizer.isNormalized(stored.displayName, Normalizer.Form.NFC))
    }

    /**
     * The two forms are one row, which is the whole point.
     *
     * Written one way and searched the other, a person typing the name they
     * always type finds what they wrote.
     */
    @Test
    fun theTwoFormsAreOnePersonRatherThanTwo() = runBlocking {
        repository.createPerson(subjectId, decomposed, roleLabel = "Social worker")
        repository.createPerson(subjectId, composed, roleLabel = "Social worker")
        val names = repository.people(subjectId).map { it.displayName }.filter { it == composed }
        assertEquals(
            "the two spellings stored as two different strings",
            listOf(composed, composed),
            names,
        )
    }

    /** Arabic, which the contract names. Combining marks are ordinary there. */
    @Test
    fun arabicIsNormalizedToo() = runBlocking {
        // Alef with hamza below, written as the base plus the combining mark.
        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "note",
            body = "إ مرحبا",
        )
        val body = repository.entry(id)?.entry?.body.orEmpty()
        assertTrue(
            "Arabic reached the database unnormalized",
            Normalizer.isNormalized(body, Normalizer.Form.NFC),
        )
    }

    /** Chinese, which the contract names. It should pass through untouched. */
    @Test
    fun chineseIsUnchanged() = runBlocking {
        val words = "她坐起来了"
        val id = repository.createEntry(subjectId = subjectId, kind = "note", body = words)
        assertEquals(words, repository.entry(id)?.entry?.body)
    }

    /**
     * Normalizing changes the encoding of a character and nothing else.
     *
     * No trimming, no casing, no collapsing of spaces: the person's own words
     * are theirs, and a write that quietly tidied them would be the app editing
     * somebody's record.
     */
    @Test
    fun nothingElseAboutTheTextIsChanged() = runBlocking {
        val fussy = "  two  spaces, TRAILING space and a tab\t "
        val id = repository.createEntry(subjectId = subjectId, kind = "note", body = fussy)
        assertEquals(fussy, repository.entry(id)?.entry?.body)
    }

    /** An update normalizes as well as an insert. There are fifty-two of them. */
    @Test
    fun anUpdateNormalizesAsWellAsAnInsert() = runBlocking {
        val id = repository.createPerson(subjectId, "Placeholder", roleLabel = null)
        repository.updatePerson(id, decomposed, null, "Social worker")
        assertEquals(composed, repository.people(subjectId).first { it.id == id }.displayName)
    }
}
