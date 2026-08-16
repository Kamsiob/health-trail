package com.kamsiob.healthtrail.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The type scale's one locale dependency, which exists because of a defect that
 * shipped and was invisible to every check in the project.
 *
 * **In Arabic, every Display L heading in the app broke in the middle of a
 * word.** The notebook's own title rendered across two lines with its last
 * letter alone on the second, on a screen with most of its width empty. The
 * cause was the negative letter spacing the display styles carry: tracking is a
 * Latin typographic device, and on a connected script it crushes the joins and
 * broke line layout outright.
 *
 * It was found by opening the app in Arabic and looking at it. No check covered
 * it, the code looked correct, and an earlier Arabic pass on the device had
 * confirmed real glyphs and a mirrored layout without ever looking at a
 * heading. These tests are what keep it fixed; they are not what found it.
 */
class TypeTest {

    @Test
    fun `Latin keeps the display tracking the design asks for`() {
        val type = healthTrailTypeFor(Locale.ENGLISH)

        // The v4 values, D173. These were the pre-overhaul numbers and the test
        // was asserting a design that no longer exists: what it guards is that
        // a Latin locale keeps *some* negative tracking on the display end,
        // because the point of the test is the scripts that must not have any.
        assertEquals((-0.026).em, type.displayL.letterSpacing)
        assertEquals((-0.022).em, type.displayM.letterSpacing)
    }

    @Test
    fun `Spanish is Latin script and is left alone`() {
        val type = healthTrailTypeFor(Locale.forLanguageTag("es"))

        assertEquals(HealthTrailType.displayL.letterSpacing, type.displayL.letterSpacing)
    }

    @Test
    fun `Arabic carries no tracking on either display size`() {
        val type = healthTrailTypeFor(Locale.forLanguageTag("ar"))

        assertEquals(TextUnit.Unspecified, type.displayL.letterSpacing)
        assertEquals(TextUnit.Unspecified, type.displayM.letterSpacing)
    }

    /**
     * Persian and Urdu use the same connected script and would arrive with the
     * same defect. They are covered in advance because rediscovering this from
     * a screenshot costs more than one line in a set.
     */
    @Test
    fun `the other languages in the same script are covered in advance`() {
        for (tag in listOf("fa", "ur")) {
            val type = healthTrailTypeFor(Locale.forLanguageTag(tag))
            assertEquals(
                "$tag should carry no display tracking",
                TextUnit.Unspecified,
                type.displayL.letterSpacing,
            )
        }
    }

    /**
     * **Only the tracking changes.** A locale must not quietly get a different
     * size, weight, or family, which would be a second type scale rather than
     * one scale adapted to a script.
     */
    @Test
    fun `Arabic changes nothing else about the scale`() {
        val latin = healthTrailTypeFor(Locale.ENGLISH)
        val arabic = healthTrailTypeFor(Locale.forLanguageTag("ar"))

        assertEquals(latin.displayL.fontSize, arabic.displayL.fontSize)
        assertEquals(latin.displayL.fontWeight, arabic.displayL.fontWeight)
        assertEquals(latin.displayL.fontFamily, arabic.displayL.fontFamily)
        assertEquals(latin.displayL.lineHeight, arabic.displayL.lineHeight)

        // Everything that never carried tracking is untouched, which is what
        // makes displayS the control: it was always correct in Arabic and that
        // is what identified the cause.
        assertEquals(latin.displayS, arabic.displayS)
        assertEquals(latin.bodyL, arabic.bodyL)
        assertEquals(latin.bodyM, arabic.bodyM)
        assertEquals(latin.mono, arabic.mono)
    }

    /**
     * The mono style's positive tracking is deliberate and is a different
     * thing: it is a metadata style that never carries a heading, and it is
     * held at 0.12em in both scripts because widening a connected script does
     * not break its joins the way tightening it does.
     */
    @Test
    fun `the mono metadata style keeps its tracking everywhere`() {
        assertTrue(HealthTrailType.mono.letterSpacing.value > 0f)
        assertEquals(
            HealthTrailType.mono.letterSpacing,
            healthTrailTypeFor(Locale.forLanguageTag("ar")).mono.letterSpacing,
        )
    }
}
