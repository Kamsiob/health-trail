package com.kamsiob.healthtrail.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The type scale from DESIGN.md section 4.3.
 *
 * The faces are not bundled yet. That is issue #12, which has to verify the
 * current release name and license of each one and prove a Noto fallback chain
 * covers Arabic and Chinese by rendering real strings on a device. Until then
 * these styles use the platform default family, which means the sizes, weights,
 * line heights, and tracking are already correct and only the face is pending.
 * That is deliberate: getting the scale right first means the layouts are built
 * against final measurements.
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

// Replaced in issue #12 by Bricolage Grotesque, Atkinson Hyperlegible, and
// JetBrains Mono, each with a bundled Noto fallback for Arabic and Simplified
// Chinese. Atkinson Hyperlegible is a deliberate choice rather than an
// aesthetic one: it was designed by the Braille Institute for maximum character
// distinction for low vision readers, and the audience for this app is
// stressed, frequently older, and often reading in bad light.
private val DisplayFamily = FontFamily.Default
private val BodyFamily = FontFamily.Default
private val MonoFamily = FontFamily.Monospace

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
    ),
)
