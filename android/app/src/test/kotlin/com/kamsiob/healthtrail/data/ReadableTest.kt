package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a person who has never seen this app actually receives.
 *
 * `MASTER_SPEC.md` 4.9's governing sentence: **every export must be legible
 * standalone to a reader who has never seen the app.** That reader is usually a
 * sibling in another state, sometimes a case manager, occasionally a lawyer.
 * They will not be told what a thread is, they do not know the app has
 * sections, and they are reading on a phone in a hurry.
 *
 * These check the two things a document like that can get wrong: leaking the
 * app's own vocabulary, and implying it is something it is not.
 *
 * The file naming is checked here too, because a name is the only part of a
 * shared document somebody sees before deciding whether to open it.
 */
class ReadableTest {

    // -- what a file gets called ---------------------------------------------

    @Test
    fun `a file is named with the words the person already knows`() {
        assertEquals(
            "A fall in the bathroom 2026-08-02.txt",
            Readable.fileName("A fall in the bathroom", "2026-08-02", "Note"),
        )
    }

    @Test
    fun `characters a file system dislikes are removed rather than escaped`() {
        // **An escaped file name is a file name nobody recognizes.** The point
        // of the name is that somebody finds it in a downloads folder six
        // months later.
        val name = Readable.fileName("Wound care: 50% / week?", "2026-08-02", "Note")

        assertFalse("a slash survived into a file name", name.contains("/"))
        assertFalse("a colon survived into a file name", name.contains(":"))
        assertFalse("a question mark survived into a file name", name.contains("?"))
        assertTrue("the words the person wrote were lost", name.contains("Wound care"))
        assertTrue("the percent is part of what they wrote", name.contains("50%"))
    }

    @Test
    fun `a title with nothing in it still produces a findable name`() {
        assertEquals(
            "Health Trail note 2026-08-02.txt",
            Readable.fileName("   ", "2026-08-02", "Health Trail note"),
        )
    }

    @Test
    fun `a very long title is cut rather than producing an unusable name`() {
        val long = "x".repeat(300)
        val name = Readable.fileName(long, "2026-08-02", "Note")

        assertTrue("a 300 character title produced a 300 character file name", name.length < 90)
        assertTrue(name.endsWith("2026-08-02.txt"))
    }

    @Test
    fun `runs of whitespace collapse so the name is not full of gaps`() {
        assertEquals(
            "A fall 2026-08-02.txt",
            Readable.fileName("A     fall", "2026-08-02", "Note"),
        )
    }

    @Test
    fun `a name never begins or ends with stray space`() {
        val name = Readable.fileName("  A fall  ", "2026-08-02", "Note")
        assertEquals("A fall 2026-08-02.txt", name)
    }
}
