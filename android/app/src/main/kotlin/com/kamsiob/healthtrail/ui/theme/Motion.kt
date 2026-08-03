package com.kamsiob.healthtrail.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Motion, from DESIGN.md section 6. Two spring personalities and three
 * durations, and nothing else.
 *
 * Reduced motion is not a variant of this. When the system setting is on, every
 * spring becomes an instant state change, the trail draw becomes an immediate
 * render, and the only remaining transition is a 100ms opacity fade. Use
 * [Motion] through [LocalMotion] rather than building specs inline, because a
 * spec built inline is one the reduced motion setting cannot reach.
 */
interface Motion {
    // **Every spec here is finite, and the type says so.** `AnimatedVisibility`
    // and the other enter and exit transitions ask for `FiniteAnimationSpec`,
    // and an interface promising only `AnimationSpec` sends the next screen
    // that needs one off to build a spec inline, which is the one thing section
    // 6 forbids because an inline spec is one reduced motion cannot reach.
    // Every implementation below already returned a spring, a tween, or a snap,
    // all of which are finite.
    /** Everything by default: screen transitions, sheets, list entry, expansion. */
    fun <T> standard(): FiniteAnimationSpec<T>

    /**
     * Slight overshoot. Reserved for exactly three moments and nothing else:
     * the capture sheet opening, a milestone being added to the arc, and an
     * incident being marked resolved. Three, because each one is a small piece
     * of relief in an app used during hard times.
     */
    fun <T> expressive(): FiniteAnimationSpec<T>

    /** Press feedback and chip selection. */
    fun <T> quick(): FiniteAnimationSpec<T>

    /** Sheets and navigation. */
    fun <T> deliberateStandard(): FiniteAnimationSpec<T>

    /** The trail drawing itself in on first view of a timeline. */
    fun <T> trailDraw(): FiniteAnimationSpec<T>

    /** Stagger between trail nodes fading in. Zero when motion is reduced. */
    val trailNodeStaggerMillis: Int

    val isReduced: Boolean
}

private const val QUICK_MILLIS = 120
private const val STANDARD_MILLIS = 240
private const val DELIBERATE_MILLIS = 400
private const val REDUCED_FADE_MILLIS = 100

object FullMotion : Motion {
    override fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 380f)

    override fun <T> expressive(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.68f, stiffness = 300f)

    override fun <T> quick(): FiniteAnimationSpec<T> = tween(QUICK_MILLIS)

    override fun <T> deliberateStandard(): FiniteAnimationSpec<T> = tween(STANDARD_MILLIS)

    override fun <T> trailDraw(): FiniteAnimationSpec<T> = tween(DELIBERATE_MILLIS)

    override val trailNodeStaggerMillis: Int = 30

    override val isReduced: Boolean = false
}

/**
 * Every spring becomes an instant state change. The only motion left anywhere
 * is a 100ms opacity fade, which is what [quick] returns so that a press still
 * acknowledges itself.
 */
object ReducedMotion : Motion {
    override fun <T> standard(): FiniteAnimationSpec<T> = snap()
    override fun <T> expressive(): FiniteAnimationSpec<T> = snap()
    override fun <T> quick(): FiniteAnimationSpec<T> = tween(REDUCED_FADE_MILLIS)
    override fun <T> deliberateStandard(): FiniteAnimationSpec<T> = tween(REDUCED_FADE_MILLIS)
    override fun <T> trailDraw(): FiniteAnimationSpec<T> = snap()
    override val trailNodeStaggerMillis: Int = 0
    override val isReduced: Boolean = true
}

val LocalMotion = staticCompositionLocalOf<Motion> { FullMotion }

/**
 * Reads the system animator duration scale. A scale of zero means the person
 * has turned animations off, either through the accessibility setting or
 * through developer options, and both mean the same thing to us.
 *
 * DESIGN.md says to verify this by actually enabling the setting rather than by
 * reading the code, and that verification belongs in the accessibility pass.
 */
@Composable
@ReadOnlyComposable
fun rememberSystemMotion(): Motion {
    val context = LocalContext.current
    val scale = android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return if (scale == 0f) ReducedMotion else FullMotion
}

@Suppress("unused")
private val unusedSpringReference = Spring.StiffnessMedium
