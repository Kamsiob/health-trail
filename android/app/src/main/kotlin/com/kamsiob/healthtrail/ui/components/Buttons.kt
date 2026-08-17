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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.annotation.DrawableRes
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.graphicsLayer
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
// The give a button has under a finger is [pressScale] in `Press.kt`, beside
// the resting surface and the focus ring. It was private here, so everything
// that was not a button copied it. D167.

@Composable
fun FilledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HealthTrail.colors
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            // **The filled button is the commit**, D168: saving, starting,
            // confirming. A tick under the thumb at the moment something is
            // written down is the difference between an interface that
            // responded and one that felt like it did. Quiet buttons stay
            // silent, because a secondary action that buzzes is noise.
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        modifier = modifier.sizeIn(minHeight = Space.touchTarget),
        enabled = enabled,
        // **A corner of its own, not a pill.** `m3v4-2` draws its three
        // actions as rounded rectangles and `m3v4-4` draws Save the same way.
        // D167: shape variety is the loudest expressive signal, and an action
        // that wears the card's corner is one more thing that looks like
        // everything else.
        shape = Radius.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.blue,
            contentColor = colors.onBlue,
            disabledContainerColor = colors.sand,
            disabledContentColor = colors.ink2,
        ),
        contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.sm),
    ) {
        Text(
            text = label,
            style = HealthTrail.type.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 0.dp),
        )
    }
}

/**
 * The support button, which is now an ordinary outlined action in blue.
 *
 * **D59's gold-outline exception ended on 2026-08-03, by owner decision.**
 * `DECISIONS.md` D93. Gold is the trail and capture only, and an outlined gold
 * button was **a seventh costume existing on exactly one screen**, which is
 * precisely the class of thing law 2 was written to eliminate. A person who has
 * learned that gold means "the way in" met one gold thing that was not.
 *
 * **The copy is unchanged.** The visible label is still the caller's, and on
 * both screens that is "Support this work."
 *
 * **It must never read as a request.** It sits after the sentence saying the app
 * is free and asks for nothing, and both screens are fully passable without
 * noticing it. A support button that reads as a nag undoes the sentence above
 * it, which is worth more than the button.
 *
 * It delegates rather than drawing its own pill, so there is one outlined action
 * in this app and not two that drift.
 */
@Composable
fun SupportButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuietButton(label = label, onClick = onClick, modifier = modifier)
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
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = Space.touchTarget),
        enabled = enabled,
        shape = Radius.button,
        // **Tonal rather than outlined, which is the v4 change.** `docs/V4.md`
        // 4 lists "the outlined pill button" as the thing being removed, and
        // `m3v4-2` draws the two secondary actions beside the filled one as
        // quiet filled blocks with no border at all. A container is a stronger
        // "this is tappable" than an outline and it does not add a second edge
        // to a screen already made of edges.
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.sand,
            // **Blue, because this is an action**, and blue is what this app
            // spends on actions and only on actions.
            contentColor = colors.blue,
            disabledContainerColor = colors.sand,
            disabledContentColor = colors.ink2,
        ),
        contentPadding = PaddingValues(horizontal = Space.m, vertical = Space.s),
    ) {
        Text(
            text = label,
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

/**
 * One action as a mark in a filled circle. `m3v4-3`, #386.
 *
 * **The drawing puts these at the end of a row**: a phone on every person in
 * the unit list, gold on the section's own wash. A word in blue at the end of
 * fifteen rows reads as a column of links, and law 2 bans a bare text link
 * precisely because it is the costume a tired person cannot tell from ordinary
 * emphasis.
 *
 * **The description is required rather than optional.** The mark is the only
 * thing naming this control, so a reader gets nothing without it, and "Call"
 * fifteen times is the ambiguity `DESIGN.md` 5.12 exists to prevent: the caller
 * passes whose number it is.
 */
@Composable
fun CircleAction(
    @DrawableRes symbol: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = HealthTrail.colors.goldWash,
    tint: Color = HealthTrail.colors.goldInk,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(Space.touchTarget),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = tint,
        ),
    ) {
        Symbol(symbol = symbol, contentDescription = null)
    }
}
