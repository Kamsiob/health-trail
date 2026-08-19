package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The three marks in the file a stranger opens. D207, `contract/DATA-CONTRACT.md`
 * 8.8.1.
 *
 * **The first test is the one that matters.** The readable copy is HTML written
 * from somebody's own words, so the question is not only "does bold come out
 * bold" but "can anything they typed become an element". Escaping happens before
 * any tag is introduced, and this holds that order in place.
 */
class ReadableMarkedTest {

    @Test
    fun nothingSomebodyTypesCanOpenAnElement() {
        assertEquals(
            "&lt;script&gt;alert(1)&lt;/script&gt;",
            ReadablePage.marked("<script>alert(1)</script>"),
        )
        assertEquals(
            "<strong>&lt;b&gt;</strong>",
            ReadablePage.marked("**<b>**"),
        )
    }

    @Test
    fun boldAndItalicBecomeTags() {
        assertEquals("<strong>call them</strong>", ReadablePage.marked("**call them**"))
        assertEquals("<em>soon</em>", ReadablePage.marked("_soon_"))
        assertEquals(
            "bring <strong>her glasses</strong> and <em>the cardigan</em>",
            ReadablePage.marked("bring **her glasses** and _the cardigan_"),
        )
    }

    @Test
    fun aRunOfBulletsBecomesOneList() {
        assertEquals(
            "<ul><li>her glasses</li><li>the blue cardigan</li></ul>",
            ReadablePage.marked("- her glasses\n- the blue cardigan"),
        )
    }

    @Test
    fun aListEndsWhenTheBulletsDo() {
        assertEquals(
            "before<ul><li>one</li></ul>after",
            ReadablePage.marked("before\n- one\nafter"),
        )
    }

    /**
     * **Nothing else is a mark**, which is what naming a subset is for.
     *
     * **Escaped, not unchanged.** `>` becomes `&gt;` because this is HTML and
     * escaping is the safety; the claim here is that none of these opens a tag
     * or changes the words, not that the bytes are identical.
     */
    @Test
    fun everythingElseIsWordsRatherThanAMark() {
        assertEquals("# heading", ReadablePage.marked("# heading"))
        assertEquals("1. one", ReadablePage.marked("1. one"))
        assertEquals("`code`", ReadablePage.marked("`code`"))
        assertEquals("~~struck~~", ReadablePage.marked("~~struck~~"))
        assertEquals("&gt; quote", ReadablePage.marked("> quote"))
    }

    /** A half written memo reads as what somebody wrote. */
    @Test
    fun anUnclosedMarkStaysTheCharactersItIs() {
        assertEquals("**she wants", ReadablePage.marked("**she wants"))
        assertEquals("_and then", ReadablePage.marked("_and then"))
    }

    @Test
    fun plainLinesAreSeparatedAndNotJoined() {
        assertEquals("first<br>second", ReadablePage.marked("first\nsecond"))
    }
}
