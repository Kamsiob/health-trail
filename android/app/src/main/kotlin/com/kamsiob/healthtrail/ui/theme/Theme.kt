package com.kamsiob.healthtrail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.staticCompositionLocalOf

val LocalHealthTrailColors = staticCompositionLocalOf { LightColors }
val LocalHealthTrailType = staticCompositionLocalOf { HealthTrailType }

/**
 * The app theme, on Material 3 Expressive. #385, `docs/V4.md` 3, D178.
 *
 * **`MaterialExpressiveTheme` rather than `MaterialTheme`, and it is the first
 * step of the rebuild because everything below reads its defaults from here**:
 * shape, color, type and motion. A component converted before the theme is a
 * component converted twice.
 *
 * Dynamic color is deliberately not used: the palette carries meaning here,
 * gold means the trail and red means the emergency card, and letting the
 * wallpaper reassign those would break the one rule that keeps the app from
 * looking clinical.
 */
@Composable
fun HealthTrailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val motion = rememberSystemMotion()
    // Remembered because both are forty eight and fifteen argument objects
    // built at the root of the tree, and neither changes unless the theme or
    // the locale does.
    val material = remember(colors) { materialSchemeFor(colors) }

    // **The type scale depends on the locale**, because the display face's
    // tight tracking is a Latin device that breaks a connected script. Read
    // from the configuration rather than from LocalStrings, since the theme
    // sits above the catalog and a per-app locale is applied to the
    // configuration either way. See healthTrailTypeFor.
    val locale = LocalConfiguration.current.locales[0]
    val type = healthTrailTypeFor(locale)
    val materialType = remember(type) { materialTypographyFor(type) }

    CompositionLocalProvider(
        LocalHealthTrailColors provides colors,
        LocalHealthTrailType provides type,
        LocalMotion provides motion,
    ) {
        // **Two call sites, and the difference between them is the whole
        // accessibility promise.** `MotionScheme.expressive()` is internal to
        // material3, so the only way to ask for Google's expressive motion is
        // to let the theme default to it, and the only way to turn it off is to
        // pass a scheme that does nothing. Reduced motion has to reach
        // Material's own components, not just this app's: see
        // [StillMotionScheme].
        if (motion.isReduced) {
            MaterialExpressiveTheme(
                colorScheme = material,
                motionScheme = StillMotionScheme,
                shapes = HealthTrailShapes,
                typography = materialType,
                content = content,
            )
        } else {
            MaterialExpressiveTheme(
                colorScheme = material,
                shapes = HealthTrailShapes,
                typography = materialType,
                content = content,
            )
        }
    }
}

/**
 * Every Material color role, named from this app's tokens.
 *
 * **All forty eight of them, in both themes, and the count is the point.** A
 * role left undefined falls back to Material's baseline, which is lavender, and
 * that is how a mockup of this app first came out purple: D167. The old scheme
 * named twenty one roles in light and sixteen in dark, so the container ladder
 * a converted screen sits on existed in one theme and not the other, and every
 * expressive component reads that ladder.
 *
 * **One function for both themes rather than two blocks.** Two blocks is how
 * the dark scheme came to be missing five roles the light one had: nothing
 * compares them. Where a role genuinely differs by theme it says so here, in
 * one place, and [HealthTrailColors.isDark] is what it asks.
 */
private fun materialSchemeFor(colors: HealthTrailColors): ColorScheme = with(colors) {
    // The container ladder runs from the surface furthest from the eye to the
    // one nearest it, and the direction of "further" inverts between themes: a
    // recessed surface is darker on light and lighter on dark. Color.kt says
    // this about `sand` and the fold row, and it is the same fact.
    val containerLowest = if (isDark) paper else card
    val containerLow = if (isDark) foldSurface else paper
    val container = if (isDark) card else sand
    val containerHigh = sand
    val containerHighest = sand

    // **The constructor rather than `lightColorScheme` or `darkColorScheme`.**
    // Those two exist to fill in what a caller leaves out, and every role here
    // is named, so all they would add is a word claiming a theme this object
    // does not carry either way.
    ColorScheme(
        // The single accent. Every action, and only actions.
        primary = blue,
        onPrimary = onBlue,
        primaryContainer = blueWash,
        onPrimaryContainer = blueDeep,
        // **The wash is the accent seen against the inverted ground**, and it
        // is already the right value in both themes without a branch: pale on
        // a dark inverse surface, deep on a light one.
        inversePrimary = blueWash,
        // **Gold is not a second accent.** `secondary` is a fill role, and the
        // loud gold belongs to the trail and the capture button, which draw it
        // themselves. So the quiet gold takes the role and the loud one stays
        // where the app puts it by hand. Color.kt says why gold text is
        // `goldInk` rather than `gold`.
        secondary = goldInk,
        onSecondary = paper,
        // The navigation bar's selected indicator is this pair, and the
        // approved mockups draw exactly that: a gold wash behind the icon of
        // the destination you are on. `m3v4-0`.
        secondaryContainer = goldWash,
        onSecondaryContainer = goldInk,
        // Resolved and done.
        tertiary = leafInk,
        onTertiary = paper,
        tertiaryContainer = leafWash,
        onTertiaryContainer = leafInk,
        background = paper,
        onBackground = ink,
        // **Dark lifts its surfaces off the canvas and light does not.** On
        // dark, elevation is carried by surface lightness, so a Material
        // surface sits one step above the paper it is on; on light the canvas
        // is already the lightest quiet value and a step would be invented.
        surface = if (isDark) card else paper,
        onSurface = ink,
        surfaceVariant = sand,
        onSurfaceVariant = ink2,
        surfaceTint = blue,
        inverseSurface = ink,
        inverseOnSurface = paper,
        // The emergency card, an open incident, a disputed bill. **Never a
        // measurement**, rule 2, and nothing in this scheme makes it one.
        error = alert,
        // `alert` inverts with the theme, dark on light and light on dark, so
        // the paper does too and the pair holds without a branch.
        onError = paper,
        errorContainer = alertWash,
        onErrorContainer = alertInk,
        outline = ink3,
        outlineVariant = hairline,
        // Black at full strength. Every component that draws a scrim applies
        // its own alpha over this.
        scrim = Color(0xFF000000),
        surfaceBright = if (isDark) sand else card,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        surfaceContainerLow = containerLow,
        surfaceContainerLowest = containerLowest,
        surfaceDim = if (isDark) paper else sand,
        // **The fixed roles come from the light palette in both themes, which
        // is what fixed means**: Material's contract is that a fixed container
        // keeps its value when the theme flips, so a thing carried between
        // surfaces does not change color underneath somebody. They are named
        // because an unnamed role is lavender, not because the app reaches for
        // them; nothing here draws one today.
        //
        // **There is no dim step in this palette and one is not invented.** A
        // fixed pair and its dim sibling are the same wash, which is honest:
        // a made up value would be a color nobody chose, sitting in the theme
        // waiting for a component to find it.
        primaryFixed = LightColors.blueWash,
        primaryFixedDim = LightColors.blueWash,
        onPrimaryFixed = LightColors.blueDeep,
        onPrimaryFixedVariant = LightColors.blue,
        secondaryFixed = LightColors.goldWash,
        secondaryFixedDim = LightColors.goldWash,
        onSecondaryFixed = LightColors.goldInk,
        onSecondaryFixedVariant = LightColors.goldInk,
        tertiaryFixed = LightColors.leafWash,
        tertiaryFixedDim = LightColors.leafWash,
        onTertiaryFixed = LightColors.leafInk,
        onTertiaryFixedVariant = LightColors.leafInk,
    )
}

/**
 * The shape scale every Material component falls back to.
 *
 * **Shape variety is the loudest signal of Material 3 Expressive**, `docs/V4.md`
 * 2, and one radius on every surface is the thing that makes an app read as
 * undesigned. So the five roles are five different corners from [Radius] rather
 * than one value repeated, and a component with an opinion about its own shape,
 * the hero and the capture button most of all, still passes it.
 *
 * The largest symmetric corner in the app is the container corner, so sheets
 * and dialogs carry it. The hero's asymmetric corner is drawn by the hero and
 * is deliberately not on this scale: it is the one shape nothing else wears.
 */
private val HealthTrailShapes = Shapes(
    extraSmall = Radius.groupMiddle,
    small = Radius.tile,
    medium = Radius.button,
    large = Radius.cardLarge,
    extraLarge = Radius.cardLarge,
)

/**
 * Every Material type role, from this app's ladder.
 *
 * **All fifteen, for the same reason all forty eight colors are named**: a role
 * left undefined is Material's default face, which is not a face this app
 * bundles, so a single unnamed role puts one line of Roboto on a screen set in
 * Atkinson Hyperlegible. The old mapping named eight.
 *
 * The ladder itself is `Type.kt` and `DESIGN.md` 5.1, which
 * `check_type_ladder.py` holds together. Nothing new is invented here; this is
 * only which rung answers to which Material name.
 */
private fun materialTypographyFor(type: HealthTrailTypography) = Typography(
    displayLarge = type.displayL,
    displayMedium = type.hero,
    displaySmall = type.displayM,
    headlineLarge = type.displayL,
    headlineMedium = type.displayM,
    headlineSmall = type.displayS,
    titleLarge = type.displayS,
    titleMedium = type.rowTitle,
    titleSmall = type.label,
    bodyLarge = type.bodyL,
    bodyMedium = type.bodyM,
    bodySmall = type.bodyS,
    labelLarge = type.label,
    labelMedium = type.navLabel,
    labelSmall = type.navLabel,
)

/**
 * Whether [choice] means dark right now.
 *
 * **[ThemeChoice.FOLLOW_SYSTEM] reads `isSystemInDarkTheme` inside composition
 * rather than resolving once**, so a phone that flips to dark while the app is
 * open takes the app with it. Resolving it at the call site would freeze the
 * app on whatever the system was at launch, which looks like a bug precisely
 * when somebody is watching for the change.
 */
@Composable
fun ThemeChoice.isDark(): Boolean = when (this) {
    ThemeChoice.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    ThemeChoice.LIGHT -> false
    ThemeChoice.DARK -> true
}

/**
 * Shorthand accessors, so a screen reads `HealthTrail.colors.blue` rather than
 * reaching into a composition local by hand.
 */
object HealthTrail {
    val colors: HealthTrailColors
        @Composable get() = LocalHealthTrailColors.current

    val type: HealthTrailTypography
        @Composable get() = LocalHealthTrailType.current

    val motion: Motion
        @Composable get() = LocalMotion.current
}
