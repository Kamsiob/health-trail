package com.kamsiob.healthtrail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.staticCompositionLocalOf

val LocalHealthTrailColors = staticCompositionLocalOf { LightColors }
val LocalHealthTrailType = staticCompositionLocalOf { HealthTrailType }

/**
 * The app theme.
 *
 * Material 3 is used for its components and its accessibility plumbing, with a
 * fully custom color scheme. Dynamic color is deliberately not used: the palette
 * carries meaning here, gold means the trail and red means the emergency card,
 * and letting the wallpaper reassign those would break the one rule that keeps
 * the app from looking clinical.
 */
@Composable
fun HealthTrailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val motion = rememberSystemMotion()

    // Material's own scheme is filled from our tokens so that any Material
    // component picked up along the way lands in the right place rather than
    // rendering in its defaults.
    val material = if (darkTheme) {
        darkColorScheme(
            primary = colors.blue,
            onPrimary = colors.onBlue,
            primaryContainer = colors.blueWash,
            onPrimaryContainer = colors.blueDeep,
            secondary = colors.leaf,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.sand,
            onSurfaceVariant = colors.ink2,
            error = colors.alert,
            onError = colors.onAlertFill,
            errorContainer = colors.alertWash,
            onErrorContainer = colors.alertInk,
            outline = colors.ink3,
        )
    } else {
        lightColorScheme(
            primary = colors.blue,
            onPrimary = colors.onBlue,
            primaryContainer = colors.blueWash,
            onPrimaryContainer = colors.blueDeep,
            secondary = colors.leaf,
            onSecondary = colors.card,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.sand,
            onSurfaceVariant = colors.ink2,
            error = colors.alert,
            onError = colors.onAlertFill,
            errorContainer = colors.alertWash,
            onErrorContainer = colors.alertInk,
            outline = colors.ink3,
        )
    }

    // **The type scale depends on the locale**, because the display face's
    // tight tracking is a Latin device that breaks a connected script. Read
    // from the configuration rather than from LocalStrings, since the theme
    // sits above the catalog and a per-app locale is applied to the
    // configuration either way. See healthTrailTypeFor.
    val locale = LocalConfiguration.current.locales[0]
    val type = healthTrailTypeFor(locale)

    val materialType = Typography(
        headlineLarge = type.displayL,
        headlineMedium = type.displayM,
        titleLarge = type.displayS,
        bodyLarge = type.bodyL,
        bodyMedium = type.bodyM,
        bodySmall = type.bodyS,
        labelLarge = type.label,
        labelSmall = type.navLabel,
    )

    CompositionLocalProvider(
        LocalHealthTrailColors provides colors,
        LocalHealthTrailType provides type,
        LocalMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = material,
            typography = materialType,
            content = content,
        )
    }
}

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
