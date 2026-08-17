package com.kamsiob.healthtrail.ui.v4

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * `DESIGN.md` section 7: initials are taken **by grapheme cluster, never by
 * character index**.
 *
 * This is a record-keeping app and the thing most likely to be cut wrong is a
 * person's name, which is exactly the content that must never render as a box.
 * Cutting by code unit splits a surrogate pair or separates a combining mark
 * from its base, and both render as a replacement character rather than a
 * letter.
 */
class AvatarInitialsTest {

    @Test
    fun `takes the first letter of the first and last words`() {
        assertEquals("AR", initialsOf("Angela Reyes"))
        assertEquals("WO", initialsOf("Wesley Obi"))
    }

    @Test
    fun `a leading honorific is not an initial`() {
        // Found by this test rather than by looking. The reference file lists
        // "Dr. Priya Raman" beside an avatar reading PR, and the obvious
        // implementation gives DR, so every doctor on the roster would share
        // initials with every other doctor.
        assertEquals("PR", initialsOf("Dr. Priya Raman"))
        assertEquals("MG", initialsOf("Mrs. Marta Gomez"))
        assertEquals("SL", initialsOf("Prof. Sandra Lin"))
    }

    @Test
    fun `a person recorded only as a title keeps it`() {
        // An empty avatar helps nobody, and that is the name the person wrote.
        assertEquals("D", initialsOf("Dr."))
    }

    @Test
    fun `a long word ending in a period is a name rather than a title`() {
        assertEquals("AR", initialsOf("Anderson. Reyes"))
    }

    @Test
    fun `a single word gives a single initial`() {
        assertEquals("T", initialsOf("Tonya"))
    }

    @Test
    fun `a middle name is skipped rather than making three initials`() {
        // Two characters, per the component. A third would not fit the circle
        // and would shrink the type below the floor to make it.
        assertEquals("MW", initialsOf("Mary Anne Wilson"))
    }

    @Test
    fun `a combining accent stays attached to its base letter`() {
        // "José" written with a combining acute. Cutting by index would take
        // the bare "e" or split the pair, depending which end it cut from.
        val decomposed = "José Martínez"
        assertEquals("JM", initialsOf(decomposed))

        // And the precomposed spelling of the same name gives the same answer,
        // which is what the data contract's NFC rule is protecting.
        assertEquals(initialsOf("José Martínez"), initialsOf(decomposed))
    }

    @Test
    fun `a name outside the basic plane is not split in half`() {
        // A surrogate pair. Cutting by code unit yields half a character, which
        // renders as a box rather than a letter.
        val name = "𐰀rmenian Something"
        val initials = initialsOf(name)
        assertEquals(2, initials.codePointCount(0, initials.length))
        assertEquals('\uD803', initials[0])
    }

    @Test
    fun `Arabic gives Arabic initials rather than boxes`() {
        val initials = initialsOf("فاطمة الزهراء")
        assertEquals("فا", initials)
    }

    @Test
    fun `Chinese gives its own characters`() {
        assertEquals("王李", initialsOf("王 李"))
    }

    @Test
    fun `uppercasing follows the catalog locale rather than the device`() {
        // A Turkish device uppercases "i" to a dotted capital. The catalog's
        // locale decides, not the phone's, so an English catalog on a Turkish
        // phone still reads "I".
        assertEquals("II", initialsOf("iris irving", Locale.ENGLISH))
    }

    @Test
    fun `a name that was never recorded gives nothing to draw`() {
        // The caller shows the care team drawing instead. A question mark would
        // read as the app asking the person something.
        assertEquals("", initialsOf(""))
        assertEquals("", initialsOf("   "))
    }

    @Test
    fun `extra whitespace does not become an initial`() {
        assertEquals("AR", initialsOf("  Angela   Reyes  "))
    }
}
