package com.kamsiob.healthtrail.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kamsiob.healthtrail.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The type scale from DESIGN.md section 4.3.
 *
 * **The faces are bundled**, per issue #12. Bricolage Grotesque for display,
 * Atkinson Hyperlegible for body, JetBrains Mono for metadata, each falling
 * through to Noto Sans Arabic for glyphs it does not have. Every license was
 * verified against `google/fonts` rather than assumed. Simplified Chinese is
 * still on the system face, which is a size decision recorded below.
 *
 * The 13sp floor and its two exemptions are in DESIGN.md section 4.3. The nav
 * label and the mono metadata style are the only two, both because they never
 * carry information on their own and both scale with dynamic type.
 */
@Immutable
data class HealthTrailTypography(
    /** Screen titles. */
    /**
     * The one thing on a screen, per law 1. `DESIGN.md` 5.1.
     *
     * **23sp against supporting content at 13sp, and the jump is the point.**
     * Hierarchy fails when sizes are close, and closing this gap to make a
     * screen look balanced is the most common way to lose it. It is meant to be
     * felt at arm's length by somebody standing in a hallway.
     *
     * **At most one per screen, and no hero at all is a valid screen.**
     */
    val hero: TextStyle,
    val displayL: TextStyle,
    /** Detail page titles, month headers. */
    val displayM: TextStyle,
    /** Card titles, section names in the table of contents. */
    val displayS: TextStyle,
    /**
     * The title line of a group row. `DESIGN.md` 5.1, and it was specified in
     * the ladder from the beginning and never implemented.
     *
     * **This is why a list in this app reads flatter than a phone's own
     * Contacts.** With no token for it, `DenseRow` reached for `bodyL` and its
     * subtitle for `bodyM`: two sizes of the same unbolded face, twelve percent
     * apart. Both grids draw the row title bold over a detail line a quarter
     * smaller, which is a weight jump and a size drop. The app had the size
     * drop and no weight at all, on every list in it, which is most of it.
     *
     * **The display face, because a row title is a name rather than prose.**
     * Bricolage at 700 is the hand-lettered tab in a binder, and the body face
     * underneath it is the note written in it. #361.
     */
    val rowTitle: TextStyle,
    /** Note bodies, anything read at length. */
    val bodyL: TextStyle,
    /** Subtitles, supporting text, list rows. */
    val bodyM: TextStyle,
    /** Tertiary detail. The floor. */
    val bodyS: TextStyle,
    /** Buttons, chips, emphasis inside body text. */
    val label: TextStyle,
    /** Bottom navigation only. Exempt from the 13sp floor. */
    val navLabel: TextStyle,
    /** Eyebrow labels, timestamps, counts, metadata. Exempt from the 13sp floor. */
    val mono: TextStyle,
    /**
     * A number at display size, in the big-number component and nowhere else.
     *
     * **It carries no tracking, and that is deliberate rather than an
     * oversight.** The 0.12em on [mono] exists to raise letter distinction at
     * 11sp, where the label is small and short. At 22sp and above the same
     * tracking pulls the digits of one number apart until it reads as several
     * numbers, which is the opposite of what a big number is for. Tabular
     * figures do the alignment work instead. `DESIGN.md` 5.1.
     */
    val monoL: TextStyle,
)

// The faces, per DESIGN.md section 4.3, bundled rather than requested at
// runtime: this app works offline and a typeface that needs the network is a
// typeface that is sometimes absent.
//
// **Atkinson Hyperlegible is a deliberate choice rather than an aesthetic
// one.** It was designed by the Braille Institute for maximum character
// distinction for low vision readers, and the audience for this app is
// stressed, frequently older, and often reading in bad light. It is the reason
// the body face is not whatever the display face is.
//
// **Each family lists its Arabic face after its Latin one.** Android matches a
// glyph against the family in order and falls through to the next entry when a
// face does not have it, so Arabic text picks up Noto without any locale check
// in this code. That matters because a screen can hold both at once, a person's
// name in one script inside a sentence in another, and a locale switch would
// get that wrong in exactly the case nobody tests.
//
// **Licenses, verified against google/fonts METADATA.pb on 2026-08-01**, all
// SIL Open Font License 1.1: Atkinson Hyperlegible, Copyright 2020 Braille
// Institute of America. Bricolage Grotesque, Copyright 2022 The Bricolage
// Grotesque Project Authors. JetBrains Mono, Copyright 2020 The JetBrains Mono
// Project Authors. Noto Sans Arabic, Copyright 2022 The Noto Project Authors.
//
// **Simplified Chinese is not bundled yet**, and that is a size decision rather
// than an oversight: Noto Sans SC is around ten megabytes per weight against
// 680 kilobytes for everything here put together. Chinese falls back to the
// system face until #12 settles how to carry it. The app is honest about this
// on the issue rather than pretending the coverage is complete.
private val DisplayFamily = FontFamily(
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(R.font.noto_sans_arabic_bold, FontWeight.Bold),
)

private val BodyFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
    Font(R.font.noto_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.noto_sans_arabic_bold, FontWeight.Bold),
)

private val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    // Arabic has no monospace design here and does not need one: the mono style
    // carries eyebrows, counts, and timestamps, and in Arabic those render in
    // Noto at the same size and tracking rather than in a Latin face that has
    // no glyphs for them.
    Font(R.font.noto_sans_arabic_regular, FontWeight.Normal),
)

val HealthTrailType = HealthTrailTypography(
    hero = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.015).em,
    ),
    displayL = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 31.sp,
        lineHeight = 37.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
    ),
    displayM = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.015).em,
    ),
    displayS = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
    ),
    rowTitle = TextStyle(
        fontFamily = DisplayFamily,
        // **16sp rather than the ladder's 13**, and this is the one departure
        // worth writing down. The ladder's figure is the grid's `.row .t` in
        // CSS pixels against a 10.5px detail line; read as sp it would shrink
        // every row title in the app by three points at the same moment it
        // gained weight, which is two changes at once and the wrong one is
        // invisible. The ratio the grid actually draws, a title a quarter
        // larger than its detail and bold, is what is kept: 16 over 13.
        fontSize = 17.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.01).em,
    ),
    bodyL = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyM = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyS = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    label = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    navLabel = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    ),
    mono = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.12.em,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    monoL = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.em,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
)

/**
 * Tabular figures, so every digit occupies the same width.
 *
 * **This app is full of numbers sitting in lists**: counts beside twelve
 * section names, dates down the trail, doses, amounts, and durations. With
 * proportional figures a column of them jitters, and a column that jitters has
 * to be read rather than scanned. `DESIGN.md` section 5.19.
 *
 * It rides on the Mono style, which 4.3 already assigns to counts, timestamps,
 * and metadata, so nothing has to be restyled to get it.
 *
 * **`lnum` is here as well as `tnum`** because a face that defaults to old
 * style figures would otherwise drop the 3 and the 9 below the baseline in a
 * column of dates. JetBrains Mono does not, and asking costs nothing and
 * survives a change of face.
 *
 * A face that has neither feature ignores both, which is why this is safe on
 * the system CJK face that serves the mono style in Chinese, per 4.3.
 */
private const val TABULAR_FIGURES = "tnum 1, lnum 1"

/**
 * Languages whose script joins its letters, where tracking is not a stylistic
 * choice but a defect.
 *
 * Currently Arabic, which is the only connected script the app ships. Persian
 * and Urdu are here in advance because they use the same script and would
 * arrive with the same problem, and a one line addition later is cheaper than
 * rediscovering this.
 */
private val CONNECTED_SCRIPTS = setOf("ar", "fa", "ur")

/**
 * The type scale for one locale.
 *
 * **The display face is Bricolage Grotesque, a Latin face, and its tight
 * tracking is a Latin typographic device.** `displayL` and `displayM` carry
 * negative letter spacing to hold large headings together. On a connected
 * script that is wrong twice over: it crushes the joins that make the script
 * legible, and on the device it broke line layout outright.
 *
 * **The symptom was a title split in the middle of a word.** In Arabic the
 * notebook's own title rendered as two lines, the last letter alone on the
 * second, on a screen with most of its width empty. Every Display L heading in
 * the app did the same, which is every screen title. `displayS` carries no
 * tracking and was always correct, which is what identified the cause.
 *
 * It was found by looking at the device in Arabic, not by any check and not in
 * the code, and it had been shipping since the type scale was written. The
 * Arabic pass that came before it confirmed real glyphs and a mirrored layout
 * and did not look at a heading.
 */
fun healthTrailTypeFor(locale: java.util.Locale): HealthTrailTypography =
    if (locale.language in CONNECTED_SCRIPTS) {
        HealthTrailType.copy(
            hero = HealthTrailType.hero.copy(letterSpacing = TextUnit.Unspecified),
            displayL = HealthTrailType.displayL.copy(letterSpacing = TextUnit.Unspecified),
            displayM = HealthTrailType.displayM.copy(letterSpacing = TextUnit.Unspecified),
        )
    } else {
        HealthTrailType
    }
