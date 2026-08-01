package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion

/**
 * The press state, per `DESIGN.md` section 5.14. One treatment, used by
 * everything the person can touch.
 *
 * **A control that does nothing when it is pressed reads as broken**, and this
 * app is used by someone standing in a hallway who cannot tell a slow app from
 * a dead one. Every button, row, chip, and tappable card acknowledges the
 * finger before anything else happens.
 *
 * **It is a tonal step, never a bounce.** A bounce on every press is on the
 * banned list in section 1, and a scale animation on a card the size of a
 * notebook row reads as a toy. The surface moves one step along the same
 * paper to card to sand ladder that carries elevation, over the 120ms
 * `quick` duration, which means the reduced motion setting reaches it: with
 * animations off it becomes a 100ms fade rather than nothing at all, so the
 * acknowledgment survives even when the movement does not.
 *
 * @param resting the surface the control sits on when nothing is happening
 */
@Composable
fun pressedSurface(interaction: InteractionSource, resting: Color): State<Color> {
    val pressed by interaction.collectIsPressedAsState()
    val colors = HealthTrail.colors

    // Toward `sand`, which is the next step along the paper to card to sand
    // ladder in both themes, so one rule covers light and dark rather than two.
    // Part of the way rather than all of it, because a surface that changed
    // completely would read as selected rather than pressed.
    val target = if (pressed) lerp(resting, colors.sand, PRESS_MIX) else resting

    return animateColorAsState(
        targetValue = target,
        animationSpec = LocalMotion.current.quick(),
        label = "press",
    )
}

/**
 * The focus ring's alpha, so a focused control fades its ring in rather than
 * snapping it on. The ring itself is 2dp of `blue` at 2dp offset, which is the
 * same focus treatment section 5.9 and section 5.11 already name for fields and
 * chips: one focus treatment for the whole app, per section 9.
 */
@Composable
fun focusRingAlpha(interaction: InteractionSource): State<Float> {
    val focused by interaction.collectIsFocusedAsState()
    return animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = LocalMotion.current.quick(),
        label = "focus",
    )
}

/** How far toward the next surface a press travels. Enough to see, not enough to read as a state. */
private const val PRESS_MIX = 0.55f
