package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Suggesting a home for an unfiled entry.
 *
 * The thing under test is not really the matching. It is the restraint: this
 * app never files anything on its own, so what matters most here is that the
 * matcher declines when it does not know, rather than producing a confident
 * guess for a person who is already tired and inclined to accept whatever is
 * offered.
 */
class SuggestTest {

    private fun thread(id: String, label: String) =
        Repository.CareThread(id = id, label = label, colorIndex = 0)

    private val threads = listOf(
        thread("t1", "Nursing"),
        thread("t2", "Daily personal care"),
        thread("t3", "Meals and dietary"),
        thread("t4", "Social services"),
    )

    @Test
    fun aSharedWordIsASuggestion() {
        assertEquals(
            "t1",
            Suggest.threadFor("Called the nursing station about her fall", threads)?.id,
        )
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertEquals("t1", Suggest.threadFor("NURSING", threads)?.id)
    }

    @Test
    fun theBestMatchWinsWhenItIsClear() {
        // Two words of "meals and dietary" against one of "nursing".
        assertEquals(
            "t3",
            Suggest.threadFor("Asked about her meals and the dietary notes", threads)?.id,
        )
    }

    @Test
    fun aTieSuggestsNothing() {
        // One word each. Two equally good guesses mean the app does not know,
        // and picking whichever sorted first would be the app deciding.
        assertNull(Suggest.threadFor("nursing and dietary", threads))
    }

    @Test
    fun noSharedWordSuggestsNothing() {
        // The common case, and the right outcome. An entry reached the tray
        // because it was hard to place, and a wrong guess presented confidently
        // is worse than an honest blank.
        assertNull(Suggest.threadFor("She seemed brighter today", threads))
    }

    @Test
    fun emptyTextSuggestsNothing() {
        assertNull(Suggest.threadFor("", threads))
        assertNull(Suggest.threadFor("   ", threads))
    }

    @Test
    fun noThreadsSuggestsNothing() {
        assertNull(Suggest.threadFor("nursing", emptyList()))
    }

    @Test
    fun shortConnectingWordsAreNotAMatch() {
        // "and" appears in two of the four labels. If it counted, half the
        // catalog would match every sentence in English and the suggestion
        // would be noise wearing the shape of help.
        assertNull(Suggest.threadFor("I rang and asked", threads))
    }

    @Test
    fun punctuationDoesNotHideAWord() {
        assertEquals("t1", Suggest.threadFor("Rang the ward. Nursing, again.", threads)?.id)
    }

    @Test
    fun aPartialWordIsNotAMatch() {
        // "nurse" is not "nursing". Stemming is exactly the kind of cleverness
        // that turns matching into interpreting, and the person is one tap from
        // the right answer either way.
        assertNull(Suggest.threadFor("Spoke to a nurse", threads))
    }
}
