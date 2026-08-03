package com.kamsiob.healthtrail.ui.screens

import com.kamsiob.healthtrail.data.Repository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the largest words on an entry screen are.
 *
 * **This is a hierarchy rule, not a formatting one.** Rule 15 says the thing
 * that matters most gets the most weight. On an entry the thing that matters
 * most is what the person wrote, and most entries have no title, so the naive
 * reading put a stock phrase at display size above the actual sentence.
 *
 * Pure in and out, so the boundaries can be checked exhaustively without a
 * device, which is the point of `TESTING-PERSONAS.md` section 7: nothing here
 * is handed to the code that the screen could not produce.
 */
class EntryHeadingTest {

    private val untitled = "Something you wrote down"

    private fun entry(title: String? = null, body: String? = null) = Repository.TrailEntry(
        id = "e1",
        kind = "visit",
        title = title,
        body = body,
        occurredEdtf = "2026-06-28",
        occurredStart = 0L,
        createdAt = 0L,
        isUnfiled = false,
        threads = emptyList(),
    )

    @Test
    fun atitleIsTheHeadingAndTheBodyStillFollows() {
        val heading = headingFor(entry(title = "Care plan meeting", body = "It went badly."), untitled)
        assertEquals("Care plan meeting", heading.text)
        assertTrue("a titled entry lost its body", heading.repeatBody)
    }

    @Test
    fun ashortBodyBecomesTheHeadingAndIsNotSaidTwice() {
        val sentence = "Asked again about the shower schedule. Third time."
        val heading = headingFor(entry(body = sentence), untitled)
        assertEquals(sentence, heading.text)
        assertFalse("the same sentence was rendered twice", heading.repeatBody)
    }

    @Test
    fun alongBodyIsCutAtAwordAndThenShownInFull() {
        val long = "She was left in the chair by the window from just after lunch " +
            "until the evening staff came on, and nobody wrote any of it down anywhere."
        val heading = headingFor(entry(body = long), untitled)

        assertTrue("the heading did not end with an ellipsis", heading.text.endsWith("…"))
        assertTrue("the heading is longer than the cap", heading.text.length <= 91)
        assertFalse("a word was cut in half", heading.text.dropLast(1).endsWith(" "))
        assertTrue("the heading is not the start of what was written", long.startsWith(heading.text.dropLast(1)))
        assertTrue("the full text was dropped instead of repeated", heading.repeatBody)
    }

    @Test
    fun onlyTheFirstLineIsPromotedAndTheRestIsKept() {
        val heading = headingFor(entry(body = "Fell in the bathroom.\nNobody called me."), untitled)
        assertEquals("Fell in the bathroom.", heading.text)
        assertTrue("everything after the first line vanished", heading.repeatBody)
    }

    @Test
    fun anentryWithNoWordsKeepsTheStockPhrase() {
        // A photograph or a recording. This is the case the phrase was written
        // for, and the only one where it is honest.
        assertEquals(untitled, headingFor(entry(), untitled).text)
        assertEquals(untitled, headingFor(entry(title = "   ", body = "  "), untitled).text)
        assertFalse(headingFor(entry(), untitled).repeatBody)
    }

    @Test
    fun aoneWordBodyLongerThanTheCapStillProducesAheading() {
        // No space to cut at. It must not come back blank.
        val heading = headingFor(entry(body = "x".repeat(200)), untitled)
        assertTrue("a heading with no word boundary came back empty", heading.text.length > 1)
        assertTrue(heading.repeatBody)
    }
}
