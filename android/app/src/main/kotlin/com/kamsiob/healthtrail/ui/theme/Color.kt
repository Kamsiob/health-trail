package com.kamsiob.healthtrail.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The color tokens from DESIGN.md sections 4.1 through 4.6, at their exact
 * values, under design direction v4.
 *
 * Four rules the type system here is shaped to enforce rather than leaving them
 * to be remembered:
 *
 * There is one accent, [blue]. Every action, every button.
 *
 * [gold] is the trail and capture. It is not a second accent. It never fills a
 * button that is not the capture button and never colors ordinary text. Gold
 * text is [goldInk], a different value chosen to pass contrast.
 *
 * [alert] belongs to the emergency card, the open incident dot and pill, and the
 * disputed bill pill. It never appears as a warning about a measurement, ever,
 * because the app does not judge measurements.
 *
 * A tab hue is identity, never state. It says which section you are in and
 * nothing more. See [TabHue].
 *
 * Color is never the only carrier of meaning. Every state that has a color also
 * has a word, a shape, or an icon. An incident is not the red one, it is the one
 * whose pill says OPEN.
 */
@Immutable
data class HealthTrailColors(
    /** App background. Warm, never white. */
    val paper: Color,
    /** Recessed: inputs, folds, insets, the search bar. */
    val sand: Color,
    /** Raised groups and sheets. */
    val card: Color,

    /** Primary text. */
    val ink: Color,
    /**
     * Secondary text, and the only other text level there is.
     *
     * DESIGN.md 4.6: at the 4.5:1 floor against warm `sand` there is no room for
     * a third distinct text level. Anything light enough to read as tertiary
     * fails the floor, and anything that clears the floor is this value. So the
     * app has two text colors and gets its third level from size and weight,
     * which is what law 1's scale jump is made of anyway.
     */
    val ink2: Color,
    /**
     * Non-text only: hairlines, dividers, inactive icon strokes, the scrubber's
     * inactive marks. These need 3:1 as interface components rather than 4.5:1
     * as text, and this value is 2.37:1 on paper, so it never renders a word.
     */
    val ink3: Color,

    /**
     * The fold row's surface, which is not simply `sand`.
     *
     * **A fold means "quieter, and more lives here."** In light theme `sand` is
     * darker than `card`, so a fold recedes and the meaning is carried for free.
     * In dark theme `sand` is *lighter* than `card`, because a recessed input on
     * a dark screen reads as lighter, and that inverts the fold: it becomes the
     * brightest surface on the screen and reads as raised.
     *
     * **Seen on the phone rather than reasoned about.** The converted notebook
     * in dark had its fold row shouting over the four sections above it.
     *
     * So the fold keeps its meaning rather than its token: `sand` in light,
     * and a value between `paper` and `card` in dark. What is preserved across
     * themes is "quieter than the group beside it", which is the thing the
     * costume actually promises.
     */
    val foldSurface: Color,

    /** Row separators inside a group. */
    val hairline: Color,
    /** Chip borders, the view toggle's container. */
    val hairlineHeavy: Color,

    /** The single accent. Every action and only actions. */
    val blue: Color,
    /** Text on a blue wash, and the pressed state. */
    val blueDeep: Color,
    /** Text and icons on a [blue] fill. */
    val onBlue: Color,
    val blueWash: Color,

    /** The trail and capture only. Shapes, never text. */
    val gold: Color,
    /**
     * The glyph on the capture button.
     *
     * Not white. White on [gold] measures 2.38:1, well under the 3:1 a control
     * needs. The capture button is the single way data enters this app and it
     * has to be findable without thought by someone tired, in bad light, and
     * often older. So the fill stays gold in both themes, which is what carries
     * the meaning, and the glyph darkens. This value measures 6.88:1.
     */
    val onGold: Color,
    /** Gold text. [gold] itself never renders text. */
    val goldInk: Color,
    val goldWash: Color,

    /** Resolved and done only. */
    val leaf: Color,
    val leafInk: Color,
    val leafWash: Color,

    /** Emergency, open incidents, disputed. Never a measurement. */
    val alert: Color,
    val alertInk: Color,
    /** The emergency card header fill. */
    val alertFill: Color,
    val onAlertFill: Color,
    val alertWash: Color,

    /** Section identity, DESIGN.md 4.3. The mapping is an owner decision. */
    val rose: TabHue,
    val teal: TabHue,
    val slate: TabHue,
    val moss: TabHue,
    val manila: TabHue,
    val stone: TabHue,

    /** Care thread route colors, in order. */
    val threadRoutes: List<Color>,

    val isDark: Boolean,
) {
    /** Every tab hue in the order DESIGN.md 4.3 lists them. */
    val tabHues: List<TabHue> get() = listOf(rose, teal, slate, moss, manila, stone)
}

/**
 * One section's identity color, in three parts.
 *
 * The split exists because every base hue fails the small-text floor. Measured
 * on adoption, the six bases landed between 3.23:1 and 4.56:1 against the
 * surfaces they sit on, and the tab chip is roughly 11sp and is the first
 * element on every section screen. DECISIONS.md D80.
 *
 * [base] is for shapes: the tab chip's fill, the avatar circle, the waypoint,
 * the icon in its wash. [ink] is for text. [wash] is its own background.
 *
 * This is not a new idea in this codebase. It is the split already used for
 * gold, leaf, and alert, applied to six more hues.
 */
@Immutable
data class TabHue(
    val base: Color,
    val ink: Color,
    val wash: Color,
)

/**
 * Light theme, DESIGN.md 4.1 through 4.3.
 *
 * Three values differ from the grid file, each because it failed the floor when
 * measured against the actual warm surfaces rather than against white. D80.
 */
val LightColors = HealthTrailColors(
    // **The canvas is quiet and the color lives in the containers.** It was
    // a warm beige, and the owner called it off-putting; the research agreed,
    // because every showcase Material 3 Expressive surface puts vibrant tonal
    // blocks on near-white ground so the color reads as deliberate objects
    // rather than as atmosphere. The warmth did not leave the app, it moved:
    // into the washes, the gold, and the paper of a document itself. D167.
    paper = Color(0xFFFBFAF8),
    sand = Color(0xFFF1EEE7),
    card = Color(0xFFFFFFFF),

    ink = Color(0xFF233240),
    // The grid draws #5A6B77, which measures 4.36:1 on sand, under the floor.
    ink2 = Color(0xFF576873),
    ink3 = Color(0xFF94A0A9),

    foldSurface = Color(0xFFF1EEE7),
    hairline = Color(0x1A233240),
    hairlineHeavy = Color(0x33233240),

    // The grid draws #2F6F8F, which measures 4.37:1 on sand, under the floor.
    blue = Color(0xFF2E6D8C),
    blueDeep = Color(0xFF245C77),
    onBlue = Color(0xFFFFFFFF),
    blueWash = Color(0xFFE2EDF2),

    gold = Color(0xFFD99D2B),
    onGold = Color(0xFF2B1D06),
    goldInk = Color(0xFF895D10),
    goldWash = Color(0xFFF5E9CD),

    leaf = Color(0xFF4E8A5C),
    leafInk = Color(0xFF3B6C48),
    leafWash = Color(0xFFE2EDE1),

    alert = Color(0xFFB5492E),
    alertInk = Color(0xFF9A3C25),
    alertFill = Color(0xFFB5492E),
    onAlertFill = Color(0xFFFFFFFF),
    alertWash = Color(0xFFF6E2DA),

    // **Spread across a lightness and saturation range rather than sitting at
    // the contrast floor**, D89. Every hue keeps its angle, which is the owner's
    // mapping; only lightness and saturation move, and they move to separate the
    // six from each other rather than to clear a minimum.
    //
    // **The ink variants are spread too, and that was the binding half.** They
    // were first derived for contrast alone, and the notebook draws each
    // section's icon in `ink` rather than `base`, so on the one screen that
    // shows all six the separation being measured was not the separation being
    // seen. The inks hold at 12.5 across the three vision models.
    //
    // As the grid drew them, the six collapsed under red-green color vision
    // deficiency: rose against moss measured 2.4 CIEDE2000 under simulated
    // deuteranopia, which is the same color. **They now hold at 11.1 across
    // normal vision, protanopia, and deuteranopia**, and no pair collapses, so
    // the second distinguisher D89 holds in reserve is not needed.
    //
    // People and chapters.
    rose = TabHue(Color(0xFFBC6949), Color(0xFFA14C2B), Color(0xFFF2E1D8)),
    // Medications, tests, questions.
    teal = TabHue(Color(0xFF4D8980), Color(0xFF2D7166), Color(0xFFDEEBE6)),
    // Appointments.
    slate = TabHue(Color(0xFF4A5E73), Color(0xFF324E6C), Color(0xFFE3E9F0)),
    // Progress, care threads.
    moss = TabHue(Color(0xFF484D38), Color(0xFF23241E), Color(0xFFEAECD8)),
    // Documents, money.
    manila = TabHue(Color(0xFF825A17), Color(0xFF4A3107), Color(0xFFF1E6CC)),
    // Standing instructions. Added in v4 because the grid draws no such screen
    // and the section needs an identity. D79.
    stone = TabHue(Color(0xFF706A5C), Color(0xFF4E483C), Color(0xFFEAE7E0)),

    threadRoutes = listOf(
        Color(0xFF2E6D8C), // physical therapy
        Color(0xFF4E8A5C), // occupational therapy
        Color(0xFFBC6949), // speech
        Color(0xFF484D38), // nursing
    ),

    isDark = false,
)

/**
 * Dark theme, DESIGN.md 4.5, re-derived against the v4 ladder. D87.
 *
 * A trail map at dusk, not an inverted document. Surfaces get lighter as they
 * come forward and elevation is carried by surface lightness rather than by
 * shadow. Never black, which smears on OLED during scroll and is harsh in a dark
 * room, which is exactly when this theme gets used.
 *
 * `sand` is lighter than `card` here, the opposite of light theme. A recessed
 * surface reads as lighter on dark, which is why an input field on a dark screen
 * is drawn lighter than the card it sits in rather than darker.
 *
 * The six tab hues are separated along lightness rather than only along hue,
 * and that is deliberate. A first derivation optimized each hue against its own
 * wash alone and produced six colors that collapsed under red-green color vision
 * deficiency: rose against stone measured 2.8 CIEDE2000 under simulated
 * deuteranopia, which is the same color. Lightness is what survives red-green
 * CVD, so the hues keep their angles exactly, which is the owner's mapping, and
 * spread across a 48 to 78 percent lightness band. Minimum pairwise separation
 * is 10.8 across normal vision, protanopia, and deuteranopia.
 */
val DarkColors = HealthTrailColors(
    paper = Color(0xFF141C23),
    sand = Color(0xFF25313A),
    card = Color(0xFF1C262E),

    ink = Color(0xFFE8EDF1),
    ink2 = Color(0xFFAFBCC5),
    ink3 = Color(0xFF6E7C85),

    // Between paper and card, so a fold recedes in dark exactly as it does in
    // light. ink2 measures 8.39:1 on it.
    foldSurface = Color(0xFF18212A),
    hairline = Color(0x1AFFFFFF),
    hairlineHeavy = Color(0x33FFFFFF),

    blue = Color(0xFF7FB6D4),
    blueDeep = Color(0xFF9BCBE4),
    onBlue = Color(0xFF0B171E),
    blueWash = Color(0xFF1E323D),

    gold = Color(0xFFE3B155),
    onGold = Color(0xFF2B1D06),
    goldInk = Color(0xFFE9BE6E),
    goldWash = Color(0xFF33290F),

    leaf = Color(0xFF74B383),
    leafInk = Color(0xFF8CC79A),
    leafWash = Color(0xFF16291C),

    alert = Color(0xFFE58163),
    alertInk = Color(0xFFE58163),
    alertFill = Color(0xFFA8412A),
    onAlertFill = Color(0xFFFFFFFF),
    alertWash = Color(0xFF3B1E14),

    rose = TabHue(Color(0xFFC79B8A), Color(0xFFB98E7E), Color(0xFF2F1D16)),
    teal = TabHue(Color(0xFFA0CFC8), Color(0xFF6CADA2), Color(0xFF172E2A)),
    slate = TabHue(Color(0xFF6789AD), Color(0xFF829BB5), Color(0xFF1B222A)),
    moss = TabHue(Color(0xFFCFD8B6), Color(0xFF9BAA6E), Color(0xFF282D18)),
    manila = TabHue(Color(0xFFD1A761), Color(0xFFC39A55), Color(0xFF312614)),
    stone = TabHue(Color(0xFF9F8856), Color(0xFFAA976E), Color(0xFF2A251B)),

    threadRoutes = listOf(
        Color(0xFF7FB6D4), // physical therapy
        Color(0xFF74B383), // occupational therapy
        Color(0xFFC79B8A), // speech
        Color(0xFFCFD8B6), // nursing
    ),

    isDark = true,
)

/**
 * The capture button is gold in both themes. It is the one element whose color
 * does not shift between themes, because it is the single way data enters the
 * app and it has to be findable without thought.
 */
val CaptureButtonLight: Color = LightColors.gold
val CaptureButtonDark: Color = DarkColors.gold
