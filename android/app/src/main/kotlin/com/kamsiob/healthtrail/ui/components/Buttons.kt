package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
            color = if (enabled) colors.onBlue else colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 0.dp),
        )
    }
}

/**
 * The support button, and the only outlined button in the app.
 *
 * **It is a deliberate, recorded exception to section 2.2**, which reserves
 * `gold` for the trail metaphor and the capture button and says plainly that
 * it never fills a button that is not the capture button. The owner asked for
 * a gold outline here. Two things keep the exception narrow rather than making
 * the rule meaningless:
 *
 * **It is an outline, not a fill.** The rule 2.2 is actually protecting is that
 * gold means "the way in" and must not be spent twice. An outlined box reads as
 * an offer rather than as an action of that weight.
 *
 * **The label is `ink`, not gold**, because 2.2 also says `gold` never colors
 * text, and that half of the rule is kept exactly.
 *
 * They are never on screen together: this appears on the disclaimer gate, which
 * is the one screen with no capture button, and in More, which is a list.
 *
 * **It must never read as a request.** It sits after the sentence saying the
 * app is free and asks for nothing, and the screen it lives on is fully
 * passable without noticing it. A support button that reads as a nag undoes the
 * sentence above it, which is worth more than the button.
 */
@Composable
fun SupportButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, Color.Transparent)
    val ring by focusRingAlpha(interaction)

    Box(
        modifier = modifier
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(
                width = 2.dp,
                // The focus ring takes the border over rather than drawing a
                // second one outside it, which would be two rectangles around
                // one control.
                color = if (ring > 0f) colors.blue.copy(alpha = ring) else colors.gold,
                shape = Radius.tile,
            )
            .clickable(
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
            color = colors.ink,
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
            // **A visible border at rest, which is the v4 change.** Before, this
            // drew its border only when focused, so at rest it was a pale
            // surface with no edge: readable as a button on `paper`, invisible
            // as one on `card`. Law 2 requires a costume be recognizable without
            // context, so the outlined pill is outlined all the time. The focus
            // ring thickens the same border rather than adding a second one, so
            // a focused control is not two rings.
            .border(
                width = if (ring > 0f) 2.dp else 1.5.dp,
                color = if (ring > 0f) colors.blue else colors.blue.copy(alpha = OUTLINE_ALPHA),
                shape = Radius.pill,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.m, vertical = Space.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            // **Blue, not ink.** The outlined pill is an action, and blue is
            // what this app spends on actions and only on actions.
            color = if (enabled) colors.blue else colors.ink2,
            style = HealthTrail.type.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 0.dp),
        )
    }
}

/** How present the outline is at rest. Enough to read as an edge, not a frame. */
private const val OUTLINE_ALPHA = 0.55f

/**
 * The same outlined pill, under the name most screens still call it by.
 *
 * **It used to be a bare text link, and v4 bans those outright.** Law 2: "No
 * bare text links anywhere." A word in blue with no container is the one
 * costume a person cannot tell from ordinary emphasis, and this app is used by
 * somebody tired who should never have to guess whether a thing is tappable.
 *
 * **So it is not deprecated, it is corrected.** Changing what it renders fixes
 * every one of its call sites at once rather than leaving sixty-five screens
 * carrying a banned costume until each is converted. There is one outlined
 * action in this app and this is a second name for it, which is a naming
 * problem rather than the "same pattern in two forms" defect `DESIGN.md` 13.2
 * warns about: both names produce the same pixels because one calls the other.
 *
 * **The name goes as each screen converts**, at which point call sites read
 * [QuietButton] directly and this is deleted.
 *
 * The leading slot is accepted and ignored. An outlined pill is a verb or a
 * dialable number, and the one thing it never needs is a decorative glyph in
 * front of the verb. It stays in the signature only so that sixty-five call
 * sites keep compiling until each is converted.
 */
@Composable
fun TextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") leading: (@Composable () -> Unit)? = null,
) {
    QuietButton(label = label, onClick = onClick, modifier = modifier, enabled = enabled)
}
