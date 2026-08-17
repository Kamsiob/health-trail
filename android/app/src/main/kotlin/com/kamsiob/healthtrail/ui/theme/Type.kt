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
 * **The faces are bundled**, per issue #12. Two of them, D181: Roboto for
 * everything a person reads, at Bold for display, and JetBrains Mono for
 * figures that line up in a column. Each falls through to Noto Sans Arabic for
 * glyphs it does not have. Every license was verified against `google/fonts`
 * rather than assumed. Simplified Chinese is still on the system face, which is
 * a size decision recorded below.
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
     * **Bold, because a row title is a name rather than prose.** The weight is
     * the hand-lettered tab in a binder, and the regular face underneath it is
     * the note written in it. #361. Since D174 that is one family at two
     * weights rather than two families, and the split reads the same.
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
    /**
     * The quiet line that names a group. Rule 15. Exempt from the 13sp floor.
     *
     * **This is [mono]'s metrics in the reading face, and that is the whole
     * point of it.** The eyebrow was set in JetBrains Mono, which put a
     * typewriter line directly above a display heading and body text on most
     * screens in the app. D172 is the owner saying the fonts do not mix, and
     * D173 settles where the boundary is: mono keeps the figures that line up
     * in a column, and everything a person reads as words leaves it.
     *
     * **The role survives the face change.** Rule 15 asks for a quiet eyebrow
     * to group what belongs together, and small size, weight and wide tracking
     * are what make a short line read as deliberate rather than as leftover.
     * None of that needed a second typeface to do.
     */
    val eyebrow: TextStyle,
    /**
     * Figures that line up in a column, and nothing else. Exempt from the floor.
     *
     * Chart axes, day grids, reference values, the date picker, the scrubber, a
     * step number, an amount of money in a list of amounts. **A count, a label,
     * an eyebrow and a person's name are words**, and they take [eyebrow] or
     * [bodyS]. V4.md section 3, D173.
     */
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
// **Roboto, because the approved mockups are set in it.** D181. It is also
// what Material 3 Expressive is drawn against, so a Material component and a
// heading this app writes itself are the same letterforms rather than two that
// nearly agree.
//
// **Each family lists its Arabic face after its Latin one.** Android matches a
// glyph against the family in order and falls through to the next entry when a
// face does not have it, so Arabic text picks up Noto without any locale check
// in this code. That matters because a screen can hold both at once, a person's
// name in one script inside a sentence in another, and a locale switch would
// get that wrong in exactly the case nobody tests.
//
// **Licenses, verified against google/fonts METADATA.pb**, all SIL Open Font
// License 1.1: Roboto, Copyright 2011 The Roboto Project Authors, checked
// 2026-08-16. JetBrains Mono, Copyright 2020 The JetBrains Mono Project
// Authors. Noto Sans Arabic, Copyright 2022 The Noto Project Authors, both
// checked 2026-08-01. Bricolage Grotesque went in D174 and Atkinson
// Hyperlegible in D181, so neither license is one this app still carries.
//
// **Simplified Chinese is not bundled yet**, and that is a size decision rather
// than an oversight: Noto Sans SC is around ten megabytes per weight against
// 800 kilobytes for everything here put together. Chinese falls back to the
// system face until #12 settles how to carry it. The app is honest about this
// on the issue rather than pretending the coverage is complete.
/**
 * The display face, which is the body face. **Roboto, D181**, superseding the
 * Atkinson Hyperlegible of [D174] and the Bricolage Grotesque before it.
 *
 * **The approved mockups are set in Roboto and the app was not.** The owner,
 * looking at the first captures off the rebuilt theme: "the style and the font
 * and the icons are different". Measured rather than eyeballed: the headline in
 * `m3v4-0-light.png` is a tight neo-grotesque, and Atkinson at the same size is
 * visibly wider, rounder and more openly spaced. The mockups are the spec, so
 * the face follows them.
 *
 * **What this gives up, stated plainly.** Atkinson was drawn by the Braille
 * Institute for maximum character distinction for low vision readers, and the
 * audience here is stressed, frequently older, and often reading in bad light.
 * That was a real argument and it lost to the design the owner approved. The
 * legibility work that survives is the part that was never about the face: the
 * 13sp floor, the two text colors at their measured contrast, and the weight
 * and size jumps of rule 15.
 *
 * **Bundled rather than taken from the system**, like every other face here.
 * `FontFamily.Default` is Roboto on a Pixel and something else on a Samsung,
 * and a design that changes shape by manufacturer is not a design. 315 KB for
 * the two weights, instanced from Google's official variable font.
 *
 * **Mono stays**, for figures that line up in a column. Two faces total with
 * different jobs, which is what "they do not mix" was asking for.
 */
private val DisplayFamily = FontFamily(
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.noto_sans_arabic_bold, FontWeight.Bold),
)

private val BodyFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_bold, FontWeight.Bold),
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
    // **The display end carries the weight contrast**, D167, at Bold rather
    // than ExtraBold since D173. The approved mockup sets its headline Bold,
    // and the owner's brief for the type is "clean fonts that are easy to read
    // with a little bit of Elegance but not something weird or something that
    // is artsy". ExtraBold at display size is the weight that starts reading as
    // a poster: the letterforms thicken until the counters close up and the
    // word becomes a shape. Bold keeps the gap against body text, which is what
    // the contrast was ever for, and stays a word.
    //
    // **Tracked tight, but less tight than it was.** Negative tracking pulls a
    // headline into one object; past about two and a half percent it starts
    // pulling the letters into each other instead, which is most of what
    // "the fonts are unnatural" was pointing at.
    hero = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.022).em,
    ),
    displayL = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.026).em,
    ),
    displayM = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 32.sp,
        lineHeight = 37.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.022).em,
    ),
    displayS = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
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
    eyebrow = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        // **Weight and wider tracking are what make a small line read as
        // deliberate rather than as leftover.** D167 wrote that about the mono
        // eyebrow and it was never about the face. Bold rather than Medium,
        // because Roboto at 12sp is a quieter letter than JetBrains Mono at
        // 12sp and the line has to hold its own under a display heading.
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.14.em,
    ),
    mono = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.16.em,
        // **The reason mono exists.** Every digit on one width, so a column of
        // figures lines up and a changing number does not shift the ones
        // beside it.
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
 * **The display face is a Latin face and its tight tracking is a Latin
 * typographic device.** `displayL` and `displayM` carry negative letter
 * spacing to hold large headings together. On a connected
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
            // **The eyebrow carries positive tracking, and that is worse on a
            // connected script than negative tracking is.** Letting the joins
            // out is what the script uses to say two letters are one word.
            // It is here from the day the token exists rather than after
            // somebody finds a broken group label in Arabic, which is how the
            // three above it were found.
            eyebrow = HealthTrailType.eyebrow.copy(letterSpacing = TextUnit.Unspecified),
        )
    } else {
        HealthTrailType
    }
