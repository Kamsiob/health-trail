package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.raisedCard

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
 * The give a surface has under a finger. D167.
 *
 * **One spring for the whole app, in the same file as the other two press
 * primitives.** It lived private inside `Buttons.kt`, so anything that was not
 * a button hand-rolled its own copy: the same six lines, the same label string,
 * and one more place for the reduced-motion path to be forgotten. Three of the
 * fourteen v4 components needed exactly this and could not reach it.
 *
 * **The scale comes from the motion tokens**, which is what lets the reduced
 * motion setting turn it off. With motion reduced the scale is 1 and the press
 * still answers in color, through [pressedSurface].
 *
 * Callers that own a whole surface want [Modifier.openableByTap] instead, which
 * carries this, the resting color and the focus ring together.
 */
@Composable
fun pressScale(interaction: InteractionSource): State<Float> {
    val motion = LocalMotion.current
    val pressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.springy(),
        label = "pressScale",
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
 * A card that opens something, and does nothing else.
 *
 * **The plain case, which had no component and so kept borrowing the wrong
 * one.** `removableByLongPress` was reached for whenever a card needed to be
 * tappable, with an empty removal lambda passed in to switch the removal off.
 * The gesture went quiet; the words did not. A reader still announced the tap
 * action with whatever label was handed over, which on the prep sheet was
 * "remove", and still offered a long press labeled "remove" that ran an empty
 * function. Rule 11: a control that says it does something and does nothing is
 * not finished.
 *
 * **It carries the surface as well as the gesture**, because 5.14 says every
 * tappable thing answers a finger and the only way to make that automatic is to
 * put the resting color and the press in the same place. The caller clips
 * first and does not set its own background.
 *
 * @param label what a reader says the tap does, in the person's language and as
 *   a verb: "Open this entry", never the name of the thing being opened.
 */
@Composable
fun Modifier.openableByTap(
    label: String,
    onTap: () -> Unit,
    resting: Color = HealthTrail.colors.card,
    /**
     * The focus ring's corners, which must be the caller's own clip.
     *
     * **A ring drawn at 17dp around a shape clipped at 13dp is visibly wrong**,
     * and it is the kind of thing that only shows up with a keyboard attached.
     * Defaults to the card radius because that is what most callers clip to.
     */
    shape: Shape = Radius.cardLarge,
    /**
     * What touching and holding does, or null where holding does nothing.
     *
     * **A shortcut and never the only way in.** Every caller that sets this has
     * a visible control doing the same thing, because a gesture nobody is told
     * about is a feature that does not exist for most people, and it is a
     * feature that does not exist at all for somebody using a screen reader or
     * switch access. [longPressLabel] is what a reader offers instead, so the
     * hold is a real action in the accessibility tree rather than a rumor.
     *
     * long-press-twin: whatever visible control the caller already has. The one
     * caller today is Today's card, whose twin is the Arrange action in the
     * screen header. D155, and the check that reads this marker is
     * `tools/checks/check_dead_gestures.py`.
     */
    onLongPress: (() -> Unit)? = null,
    /** What a reader calls the hold, as a verb. Required whenever [onLongPress] is set. */
    longPressLabel: String? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, resting)
    val ring by focusRingAlpha(interaction)
    val haptics = LocalHapticFeedback.current

    // **Every tappable surface in the app now answers with physics.** D167.
    // A color change alone is what a web page does; a surface that gives
    // under a finger and springs back is what a phone does, and it is the
    // single cheapest thing that makes an interface feel built rather than
    // assembled. One spring here reaches every card, row, tile and thumbnail
    // that goes through this modifier, which is most of them.
    //
    // **Reduced motion sets the scale to 1 and the spring to a snap**, so the
    // press still answers, in color, exactly as it did before.
    val motion = LocalMotion.current
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.springy(),
        label = "pressScale",
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        // **Raised, like every other surface in the app.** #324 lifted the
        // cards and reached `GroupedSurface`, `Tile`, `TodayCard` and the rest,
        // and missed this one, so every card built through the tappable path
        // stayed flat: a person moved from the notebook, where surfaces sit
        // above the paper, to projects and care threads, where they dissolve
        // into it. Twenty two call sites, one line. Dark theme is flat by
        // `Raise.kt` either way, which is correct there. #361.
        .raisedCard(shape)
        .background(surface)
        .border(Space.focusRing, HealthTrail.colors.blue.copy(alpha = ring), shape)
        // long-press-twin: the caller's own visible control, named on the
        // onLongPress parameter above. D155.
        .then(
            if (onLongPress == null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = label,
                    role = Role.Button,
                    onClick = onTap,
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = label,
                    role = Role.Button,
                    onLongClickLabel = longPressLabel,
                    // **The phone's own answer to a hold.** Compose does not
                    // buzz for a long press by itself, and without it the
                    // gesture succeeds in silence: the screen changes a moment
                    // later and nothing connects the two. On the home screen
                    // every person already has, the buzz is the confirmation.
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                    onClick = onTap,
                )
            },
        )
}

/**
 * How far toward `ink` a press travels. Enough to see without reading as a
 * state change: a surface that moved all the way would say selected rather
 * than pressed.
 */
private const val PRESS_MIX = 0.08f
