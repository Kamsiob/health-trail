package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
 * a 2dp `blue` outline offset 2dp, per the accessibility floor in section 9.
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
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            // The touch target is 48dp regardless of how tall the button looks,
            // which is the floor for everything in this app.
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(if (enabled) colors.blue else colors.sand)
            .then(
                if (focused) {
                    Modifier.border(2.dp, colors.blueDeep, Radius.pill)
                } else {
                    Modifier
                }
            )
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
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .then(
                if (focused) Modifier.border(2.dp, colors.blue, Radius.pill) else Modifier
            )
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
