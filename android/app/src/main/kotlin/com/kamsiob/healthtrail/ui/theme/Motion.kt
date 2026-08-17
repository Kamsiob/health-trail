package com.kamsiob.healthtrail.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MotionScheme
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

    /**
     * The tilt a card holds while Today is being arranged, in degrees.
     *
     * **This is the phone's own idiom and it is here on purpose.** Somebody
     * rearranging their home screen has seen exactly one thing say "these can be
     * picked up now", and it is this. The only frame of reference anybody brings
     * to a grid of cards is the grid of icons they already own, so borrowing it
     * costs nothing to learn.
     *
     * **Small, because this app is used during hard times.** A caregiver at two
     * in the morning does not need the screen shouting. Enough tilt to read as
     * unlatched at a glance and not enough to be hard to look at.
     *
     * **Zero when motion is reduced**, and the mode still works: the cards sit
     * still, the remove marks are still there, and the words on every card still
     * say what will happen. The wobble is the shortcut, never the information.
     */
    val arrangeTiltDegrees: Float

    /** How long one half of that tilt takes. Cards are offset so they do not march in step. */
    val arrangeTiltMillis: Int

    /**
     * The spring a thing settles on when it is picked up or lands. D167.
     *
     * **Bouncier than [expressive] on purpose.** Material 3 Expressive's whole
     * motion argument is that a spring with visible overshoot reads as
     * physical, and physical reads as responsive. This is for the moments
     * worth a small flourish: a press releasing, a card arriving, a control
     * changing shape under a finger.
     */
    fun <T> springy(): FiniteAnimationSpec<T>

    /**
     * A slower, heavier spring for something large moving: a sheet, a hero
     * expanding, a list settling after a pull.
     */
    fun <T> settle(): FiniteAnimationSpec<T>

    /** How far a pressed surface shrinks. 1f when motion is reduced. */
    val pressScale: Float

    val isReduced: Boolean
}

private const val QUICK_MILLIS = 120
private const val STANDARD_MILLIS = 240
private const val DELIBERATE_MILLIS = 400
private const val REDUCED_FADE_MILLIS = 100

/**
 * How far a card tilts while Today is being arranged.
 *
 * **Under a degree.** The phone's own home screen uses roughly this, and it is
 * the difference between "you can pick these up" and a screen that is unpleasant
 * to look at for the minute somebody spends arranging it.
 */
private const val ARRANGE_TILT_DEGREES = 0.7f

/** One half of the tilt. Slow enough to read as breathing rather than shaking. */
private const val ARRANGE_TILT_MILLIS = 130

object FullMotion : Motion {
    override fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 380f)

    override fun <T> expressive(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.68f, stiffness = 300f)

    override fun <T> quick(): FiniteAnimationSpec<T> = tween(QUICK_MILLIS)

    override fun <T> deliberateStandard(): FiniteAnimationSpec<T> = tween(STANDARD_MILLIS)

    override fun <T> trailDraw(): FiniteAnimationSpec<T> = tween(DELIBERATE_MILLIS)

    override val trailNodeStaggerMillis: Int = 30

    override val arrangeTiltDegrees: Float = ARRANGE_TILT_DEGREES

    override val arrangeTiltMillis: Int = ARRANGE_TILT_MILLIS

    override fun <T> springy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 520f)

    override fun <T> settle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = 220f)

    // **A whole percent and a half, which is felt rather than seen.** Larger
    // and a row looks like it is collapsing; smaller and a press on a big
    // card reads as nothing happening at all.
    override val pressScale: Float = 0.985f

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

    // **Still, and the mode is not diminished by it.** Every card still carries
    // its remove mark and its worded move actions; the tilt was the shortcut.
    override val arrangeTiltDegrees: Float = 0f
    override val arrangeTiltMillis: Int = ARRANGE_TILT_MILLIS

    override fun <T> springy(): FiniteAnimationSpec<T> = snap()
    override fun <T> settle(): FiniteAnimationSpec<T> = snap()

    // **No scale at all.** A shrink is motion, and this mode has none of it;
    // the press still answers through color, which `pressedSurface` carries.
    override val pressScale: Float = 1f

    override val isReduced: Boolean = true
}

/**
 * Material's own motion, turned off. #385.
 *
 * **Every Material component below the theme reads its motion from a
 * [MotionScheme] rather than from [Motion]**, so [ReducedMotion] alone leaves
 * the platform's springs running: a sheet still slides, a switch still travels,
 * a button still bounces under a finger. Reduced motion is a promise about the
 * whole screen, and half of one is worse than none, because the person who
 * turned the setting on is the one who finds out.
 *
 * The two families split the same way [ReducedMotion] does. Spatial specs move
 * a thing across the screen and become instant. Effects specs are color and
 * opacity, and keep the 100ms fade, so a press still acknowledges itself.
 *
 * **Material 3 Expressive's own scheme is what runs when motion is not
 * reduced**, and it is not named here: `MotionScheme.expressive()` is internal
 * to material3, so the way to ask for it is to let
 * `MaterialExpressiveTheme` default to it. `HealthTrailTheme` therefore passes
 * this scheme or passes nothing.
 */
object StillMotionScheme : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = tween(REDUCED_FADE_MILLIS)
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(REDUCED_FADE_MILLIS)
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = tween(REDUCED_FADE_MILLIS)
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
