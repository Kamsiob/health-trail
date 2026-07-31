package com.kamsiob.healthtrail.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The color tokens from DESIGN.md sections 2.1, 2.3, and 2.4, at their exact
 * values.
 *
 * Three rules that the type system here is shaped to enforce, rather than
 * leaving them to be remembered:
 *
 * There is one accent, [blue]. Every action, every button, every link.
 *
 * [blaze] is reserved for the trail metaphor and the capture button. It is not
 * a second accent. It never fills a button that is not the capture button,
 * never highlights a row, and never colors text. Gold text is [blazeText],
 * which is a different value chosen to pass contrast.
 *
 * [alert] belongs to the emergency card, the open incident dot and pill, and
 * the disputed bill pill. It never appears as a warning about a measurement,
 * ever, because the app does not judge measurements.
 *
 * Color is never the only carrier of meaning. Every state that has a color also
 * has a word, a shape, or an icon. An incident is not the red one, it is the
 * one whose pill says OPEN.
 */
@Immutable
data class HealthTrailColors(
    /** App background. Warm, never white. */
    val paper: Color,
    /** Card and sheet surfaces. */
    val card: Color,
    /** Recessed surfaces: icon tiles, avatars, inset rows, disabled chips. */
    val sand: Color,

    /** Primary text. */
    val ink: Color,
    /** Secondary text. */
    val ink2: Color,
    /**
     * Tertiary text, at the corrected value from DESIGN.md section 2.3. The
     * mockups used #96A4AE here, which is about 2.3:1 on paper and fails AA at
     * real text sizes.
     */
    val ink3Text: Color,
    /**
     * Non-text only: hairline rules, dividers, inactive icon strokes. These
     * need 3:1 as UI components rather than 4.5:1 as text.
     */
    val ink3NonText: Color,

    /** The single accent. */
    val blue: Color,
    val blueDeep: Color,
    /** Text and icons on a [blue] fill. */
    val onBlue: Color,
    val blueSoft: Color,

    /** Shapes only: the mark, the trail line, timeline nodes, the capture button. */
    val blaze: Color,
    /**
     * The glyph on the capture button.
     *
     * Not white. DESIGN.md section 5.5 originally said white, and measurement
     * put white on [blaze] at 2.38:1 in light and 1.97:1 in dark, well under
     * the 3:1 a control needs. The capture button is the single way data enters
     * this app and it has to be findable without thought by someone tired, in
     * bad light, and often older. So the fill stays gold in both themes, which
     * is what carries the meaning, and the glyph darkens.
     */
    val onBlaze: Color,
    /** Gold text. [blaze] itself never renders text. */
    val blazeText: Color,
    val blazeSoft: Color,

    /** Shapes only. Resolved and progress indicators. */
    val leaf: Color,
    /** Green text. */
    val leafText: Color,
    val leafSoft: Color,

    /** Emergency card, open incident dots, the disputed state. Never a measurement. */
    val alert: Color,
    val alertText: Color,
    /** The emergency card header fill. */
    val alertFill: Color,
    val onAlertFill: Color,
    val alertSoft: Color,

    /** Care thread route colors, in order. */
    val threadRoutes: List<Color>,

    val isDark: Boolean,
)

/**
 * Light theme, exactly the values in the mockups apart from the three text
 * contrast corrections in DESIGN.md section 2.3.
 */
val LightColors = HealthTrailColors(
    paper = Color(0xFFFAF6EE),
    card = Color(0xFFFFFFFF),
    sand = Color(0xFFF1EBDC),

    ink = Color(0xFF22384A),
    // 5C6F7E measured 4.38:1 on sand, just under the floor. Darkened until it
    // clears on all three surfaces it lands on.
    ink2 = Color(0xFF5A6D7C),
    // 5E6E79, the correction DESIGN.md section 2.3 proposed, measured 4.43:1 on
    // sand. The document says its own numbers are calculated and the
    // measurement is what counts, and this is the measurement.
    ink3Text = Color(0xFF5C6C77),
    ink3NonText = Color(0xFF96A4AE),

    blue = Color(0xFF2F6F8F),
    blueDeep = Color(0xFF245A75),
    onBlue = Color(0xFFF3FAFD),
    blueSoft = Color(0xFFE3EEF3),

    blaze = Color(0xFFD99D2B),
    onBlaze = Color(0xFF22384A),
    // 9A6E14 measured 4.22:1 on paper and 3.87:1 inside a gold tonal card,
    // against the roughly 4.9:1 DESIGN.md calculated. That calculation was
    // against white rather than against warm paper.
    blazeText = Color(0xFF8F6309),
    blazeSoft = Color(0xFFF7ECD1),

    leaf = Color(0xFF4E8A5C),
    leafText = Color(0xFF3D7049),
    leafSoft = Color(0xFFE4EFE5),

    alert = Color(0xFFB84A2E),
    // The shape color measured 4.22:1 as text inside a red tonal pill, so text
    // gets its own slightly darker value, the same split already used for gold
    // and green.
    alertText = Color(0xFFB34529),
    alertFill = Color(0xFFB84A2E),
    onAlertFill = Color(0xFFFFFFFF),
    alertSoft = Color(0xFFF8E4DB),

    threadRoutes = listOf(
        Color(0xFF2F6F8F), // physical therapy
        Color(0xFF4E8A5C), // occupational therapy
        Color(0xFFB36A3C), // speech
        Color(0xFF6E7F5A), // nursing
    ),

    isDark = false,
)

/**
 * Dark theme, from DESIGN.md section 2.4.
 *
 * A trail map at dusk, not an inverted document. Surfaces get lighter as they
 * come forward and elevation is carried by surface lightness rather than by
 * shadow. Never black, which smears on OLED during scroll and is harsh in a
 * dark room, which is exactly when this theme gets used.
 */
val DarkColors = HealthTrailColors(
    paper = Color(0xFF121A20),
    card = Color(0xFF1A242B),
    sand = Color(0xFF223038),

    ink = Color(0xFFE9EEF1),
    ink2 = Color(0xFFA6B4BD),
    // 7F9099 measured 4.10:1 on sand, which is the lightest dark surface and
    // therefore the tightest pairing in this theme.
    ink3Text = Color(0xFF8798A1),
    ink3NonText = Color(0xFF66757E),

    blue = Color(0xFF7FB6D4),
    blueDeep = Color(0xFF9BCBE4),
    onBlue = Color(0xFF0B171E),
    blueSoft = Color(0xFF1E323D),

    blaze = Color(0xFFE3B155),
    onBlaze = Color(0xFF0B171E),
    blazeText = Color(0xFFE9BE6E),
    blazeSoft = Color(0xFF33290F),

    leaf = Color(0xFF74B383),
    leafText = Color(0xFF8CC79A),
    leafSoft = Color(0xFF16291C),

    alert = Color(0xFFE58163),
    alertText = Color(0xFFE58163),
    alertFill = Color(0xFFA8412A),
    onAlertFill = Color(0xFFFFFFFF),
    alertSoft = Color(0xFF3B1E14),

    threadRoutes = listOf(
        Color(0xFF7FB6D4), // physical therapy
        Color(0xFF74B383), // occupational therapy
        Color(0xFFD0946A), // speech
        Color(0xFF9CAE85), // nursing
    ),

    isDark = true,
)

/**
 * The capture button is gold in both themes. It is the one element whose color
 * does not shift between themes, because it is the single way data enters the
 * app and it has to be findable without thought.
 */
val CaptureButtonLight: Color = LightColors.blaze
val CaptureButtonDark: Color = DarkColors.blaze
