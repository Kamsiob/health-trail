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

    // -- ranked, which is where the tray's two alternates come from ---------

    @Test
    fun rankedPutsTheBestMatchFirstAndKeepsEverybody() {
        val order = Suggest.ranked("Called the nursing station", threads).map { it.id }

        assertEquals("t1", order.first())
        assertEquals(
            "a thread matching nothing was dropped rather than sorted last",
            threads.size,
            order.size,
        )
    }

    @Test
    fun rankedAgreesWithTheSuggestionItSitsUnder() {
        // The tray leads with `threadFor` and offers the next two from
        // `ranked`. If they disagreed, the second option would sometimes be a
        // better match than the one presented as the suggestion.
        val text = "Asked about her meals and the dietary notes"
        assertEquals(
            Suggest.threadFor(text, threads)?.id,
            Suggest.ranked(text, threads).first().id,
        )
    }

    @Test
    fun rankedKeepsTheGivenOrderWhenNothingMatches() {
        // Which is the ordinary case on this tray, because the matcher works on
        // whole words and never stems. The caller passes threads in the order
        // worth offering, most recently filed into first, and `ranked` must not
        // shuffle that into something arbitrary.
        assertEquals(
            threads.map { it.id },
            Suggest.ranked("She was sitting up and knew who I was", threads).map { it.id },
        )
    }

    @Test
    fun rankedKeepsTheGivenOrderAmongTies() {
        // `threadFor` returns nothing on a tie, because a confident wrong guess
        // is worse than a blank. Here the tie only means the app has nothing to
        // say about which comes first, and the caller's order is a better
        // answer than a coin toss.
        val text = "nursing and social matters"
        assertNull("a tie produced a suggestion", Suggest.threadFor(text, threads))
        val order = Suggest.ranked(text, threads).map { it.id }
        assertEquals(
            "the tie was broken by something other than the given order",
            listOf("t1", "t4"),
            order.take(2),
        )
    }
}
