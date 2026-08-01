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
 * **The rule is one line: the resting surface moves 8% of the way toward
 * `ink`.** That is deliberately the same rule in both themes and on every
 * surface, and it works because `ink` is dark on light paper and light on dark
 * paper. A white card darkens, a dark card lightens, the blue button deepens
 * toward `blue_deep`, and a control with no container at all picks up a faint
 * tint. One rule, no table of exceptions, nothing to get wrong on the next
 * component.
 *
 * An earlier version stepped toward `sand` instead. It was wrong on anything
 * that was not already a card: on the blue filled button it pulled the surface
 * toward a warm neutral, which is a different color rather than a pressed one.
 *
 * **It is a tonal step, never a bounce.** A bounce on every press is on the
 * banned list in section 1, and a scale animation on a card the size of a
 * notebook row reads as a toy in an app that is deliberately unexcited about
 * itself.
 *
 * **Reduced motion reaches it**, because the spec comes from [LocalMotion]
 * rather than being built inline. With animations off the step becomes a 100ms
 * fade rather than nothing at all, so the acknowledgment survives even when the
 * movement does not.
 *
 * @param resting the surface the control sits on when nothing is happening.
 *   `Color.Transparent` is a legitimate value and gives a faint tint.
 */
@Composable
fun pressedSurface(interaction: InteractionSource, resting: Color): State<Color> {
    val pressed by interaction.collectIsPressedAsState()
    val target = if (pressed) lerp(resting, HealthTrail.colors.ink, PRESS_MIX) else resting

    return animateColorAsState(
        targetValue = target,
        animationSpec = LocalMotion.current.quick(),
        label = "press",
    )
}

/**
 * The focus ring's alpha, so a focused control fades its ring in rather than
 * snapping it on. The ring itself is 2dp of `blue` at the control's own radius,
 * which is the focus treatment 5.9 and 5.11 already name for fields and chips:
 * one focus treatment for the whole app, per section 9.
 *
 * **Press and focus are separate states and a control shows both.** A keyboard
 * user pressing space on a focused button needs to see the press as much as a
 * finger does.
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

/**
 * How far toward `ink` a press travels. Enough to see without reading as a
 * state change: a surface that moved all the way would say selected rather
 * than pressed.
 */
private const val PRESS_MIX = 0.08f
