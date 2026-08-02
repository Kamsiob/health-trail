package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Buttons, from DESIGN.md section 5.4.
 *
 * Filled uses the single accent, a pill shape, and a 48dp minimum height. Quiet
 * is a card surface with the same geometry. Both carry a visible focus state,
 * a 2dp `blue` outline, per the accessibility floor in section 9, and both
 * carry the one press state from section 5.14.
 *
 * **Both of these shipped with no press state at all**, passing
 * `indication = null` and answering only to focus. That is the "press states
 * that do nothing" tell by name, and it is why 5.14 exists.
 *
 * An action keeps the same word through its whole flow: the button that says
 * Export produces a result that says Exported.
 *
 * There is deliberately no destructive variant here yet. Destructive buttons
 * exist only inside a confirmation flow, never as a resting state on a screen,
 * and the shared confirmation component is where that belongs.
 */
@Composable
fun FilledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, if (enabled) colors.blue else colors.sand)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = modifier
            // The touch target is 48dp regardless of how tall the button looks,
            // which is the floor for everything in this app.
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.blueDeep.copy(alpha = ring), Radius.pill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.l, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.label,
            color = if (enabled) colors.onBlue else colors.ink3Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 0.dp),
        )
    }
}

/**
 * A quiet button, per DESIGN.md section 5.4: `card` surface, `ink` label, the
 * same pill geometry and the same 48dp floor as the filled one.
 *
 * **It exists because the filled button was winning screens it should not.**
 * On the care team, "Add someone" as a full width blue bar was the loudest
 * thing on a screen whose whole subject is the people above it, which inverts
 * rule 15: the accent belongs on reaching somebody, not on the way to add one.
 * A quiet button keeps the presence and gives up the shout.
 *
 * It is the right choice for a real, common, structural action that is not the
 * point of the screen it sits on. Where an action is the point, the filled
 * button is still correct.
 */
@Composable
fun QuietButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = modifier
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.pill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.l, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.label,
            color = if (enabled) colors.ink else colors.ink3Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 0.dp),
        )
    }
}

/**
 * A text action, per DESIGN.md section 5.4: no container, `blue` label, and
 * still 48dp of touch area regardless of how small the words are.
 *
 * Used where an action is genuinely secondary but must not feel discouraged.
 * Skipping setup is the clearest case: it is a real path, not a failure, so it
 * gets full reach and legible weight rather than being tucked away small.
 */
@Composable
fun TextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    // No container at rest, per 5.4, and a faint one under a finger. The 5.14
    // rule gives that for free: 8% toward `ink` from transparent is a tint
    // rather than a surface, which is exactly the right weight for an action
    // that is secondary but must not feel discouraged.
    val surface by pressedSurface(interaction, Color.Transparent)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = modifier
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.pill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.m, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.label,
            color = if (enabled) colors.blue else colors.ink3Text,
            textAlign = TextAlign.Center,
        )
    }
}
