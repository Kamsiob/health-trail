package com.kamsiob.healthtrail.ui

import com.kamsiob.healthtrail.ui.v4.RichText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three marks, and everything that is not one of them. D207.
 *
 * **The claim these tests exist to hold is that storage has no converter.**
 * `contract/DATA-CONTRACT.md` 8.8.1 says what the person typed is what is in
 * the column, so nothing here may rewrite a string: this file checks how a
 * string is *drawn* and that reading it aloud drops the marks.
 */
class RichTextTest {

    @Test
    fun boldIsTwoAsterisksAndTheMarksAreNotSpoken() {
        assertEquals("call the office", RichText.plain("**call the office**"))
        assertTrue(RichText.hasMarks("**call the office**"))
    }

    @Test
    fun italicIsOneUnderscore() {
        assertEquals("she said soon", RichText.plain("_she said soon_"))
        assertTrue(RichText.hasMarks("_she said soon_"))
    }

    @Test
    fun aBulletIsALineThatStartsWithDashSpace() {
        assertEquals("her glasses\nthe blue cardigan", RichText.plain("- her glasses\n- the blue cardigan"))
        assertTrue(RichText.hasMarks("- her glasses"))
    }

    /** **A dash inside a line is a dash.** Only the start of a line is a bullet. */
    @Test
    fun aDashInTheMiddleIsNotABullet() {
        assertEquals("call 555 - 0121", RichText.plain("call 555 - 0121"))
        assertFalse(RichText.hasMarks("call 555 - 0121"))
    }

    /**
     * **Nothing else is a mark**, which is the whole point of naming a subset:
     * it cannot grow by accident.
     */
    @Test
    fun everythingElseIsJustText() {
        for (notAMark in listOf(
            "# not a heading",
            "## still not a heading",
            "[link](https://example.com)",
            "`code`",
            "~~struck~~",
            "1. not a numbered list",
            "> not a quote",
        )) {
            assertEquals("'$notAMark' was changed", notAMark, RichText.plain(notAMark))
            assertFalse("'$notAMark' was drawn as a mark", RichText.hasMarks(notAMark))
        }
    }

    /**
     * **A half typed note is a valid note**, rule 13. An opener with no closer
     * is the characters somebody typed, and it never swallows the rest.
     */
    @Test
    fun anUnclosedMarkIsTheCharactersItIs() {
        assertEquals("**she wants", RichText.plain("**she wants"))
        assertEquals("_and then", RichText.plain("_and then"))
        assertFalse(RichText.hasMarks("**she wants"))
    }

    /** An empty pair is the characters, never an empty style that eats them. */
    @Test
    fun anEmptyPairStays() {
        assertEquals("****", RichText.plain("****"))
        assertEquals("__", RichText.plain("__"))
    }

    /** **Marks do not span lines**, so writing one does not restyle what follows. */
    @Test
    fun aMarkDoesNotReachTheNextLine() {
        assertEquals("**first\nsecond**", RichText.plain("**first\nsecond**"))
    }

    @Test
    fun boldAndItalicCanSitInOneLine() {
        assertEquals(
            "bring her glasses and the cardigan",
            RichText.plain("bring **her glasses** and _the cardigan_"),
        )
    }

    @Test
    fun plainTextIsUntouchedAndHasNoMarks() {
        val note = "She wants me to bring her glasses from the house."
        assertEquals(note, RichText.plain(note))
        assertFalse(RichText.hasMarks(note))
    }

    /**
     * **The marks are drawn, not stored differently.** Anything this file does
     * to a string for the eye must leave the words a reader hears intact.
     */
    @Test
    fun whatIsDrawnAndWhatIsSpokenCarryTheSameWords() {
        val source = "- bring **her glasses**\n- and _the blue cardigan_"
        val spoken = RichText.plain(source)
        val drawn = RichText.annotated(source).text
        assertEquals(
            "the drawn text lost or gained words",
            spoken.replace("\n", "").replace(" ", ""),
            drawn.replace("•", "").replace("\n", "").replace(" ", ""),
        )
    }
}
