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
    val displayL: TextStyle,
    /** Detail page titles, month headers. */
    val displayM: TextStyle,
    /** Card titles, section names in the table of contents. */
    val displayS: TextStyle,
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
    displayL = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
    ),
    displayM = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.015).em,
    ),
    displayS = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyL = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyM = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyS = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
    ),
    label = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    ),
    navLabel = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    ),
    mono = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.12.em,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
)

/**
 * Tabular figures, so every digit occupies the same width.
 *
 * **This app is full of numbers sitting in lists**: counts beside twelve
 * section names, dates down the trail, doses, amounts, and durations. With
 * proportional figures a column of them jitters, and a column that jitters has
 * to be read rather than scanned. `DESIGN.md` section 5.18.
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
            displayL = HealthTrailType.displayL.copy(letterSpacing = TextUnit.Unspecified),
            displayM = HealthTrailType.displayM.copy(letterSpacing = TextUnit.Unspecified),
        )
    } else {
        HealthTrailType
    }
