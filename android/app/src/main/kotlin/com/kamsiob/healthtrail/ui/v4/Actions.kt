package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The actions of the rebuilt interface, written from scratch. #386.
 *
 * **Two weights and no third.** `docs/V4.md` 2.1: one filled action per group
 * at most and the rest tonal, both at [Radius.button] rather than a pill, and
 * **no outlines**, because a container says tappable more clearly than an edge
 * does and a screen made of edges is what the old set produced.
 *
 * **Blue and only blue.** Blue is what this app spends on actions, so an action
 * never wears gold, and gold never wears an action's shape.
 */

/** How much of the screen an action is allowed to take. */
enum class ActionEmphasis {
    /**
     * The one thing this screen is for: saving, starting, confirming.
     *
     * Filled, and **at most one per group**. A screen with three blue bars has
     * told the person nothing about which one matters, which is rule 15 read
     * backwards.
     */
    Main,

    /**
     * A real action that is not the point of the screen it sits on.
     *
     * Tonal: it keeps its presence and gives up the shout. Most actions in this
     * app are this one.
     */
    Quiet,
}

/**
 * An action, in one of the two weights.
 *
 * The [mark] sits before the label rather than above it, which is the shape
 * `m3v4-5` draws on the paper card. The stacked arrangement, a mark over its
 * word, is `m3v4-2`'s row of three and arrives with that screen.
 *
 * **The mark is never announced.** It draws the same word the label already
 * says, and a reader that hears it twice has been given noise, `docs/V4.md`
 * 2.1.
 */
@Composable
fun Action(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: ActionEmphasis = ActionEmphasis.Quiet,
    @DrawableRes mark: Int? = null,
    enabled: Boolean = true,
    /**
     * Whether this action writes something down, which is what the tick is for.
     *
     * **Weight and commitment are two different things.** D168 gave the filled
     * button a haptic because a tick at the moment something is saved is the
     * difference between an interface that responded and one that felt like it
     * did. An action can still be the one thing a group offers without writing
     * anything: the paper card's way to full size is filled because it is the
     * card's only action, and it opens rather than saves.
     */
    confirms: Boolean = emphasis == ActionEmphasis.Main,
) {
    val colors = HealthTrail.colors
    val haptics = LocalHapticFeedback.current
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            mark?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(Space.markInline),
                )
            }
            Text(
                text = label,
                style = HealthTrail.type.label,
                textAlign = TextAlign.Center,
                // The button's own minimum would clip a label at font scale
                // 2.0; the 48dp target around it is what keeps the finger honest.
                modifier = Modifier.defaultMinSize(minHeight = Space.none),
            )
        }
    }
    when (emphasis) {
        ActionEmphasis.Main -> Button(
            onClick = {
                // **A tick at the moment something is written down.** The quiet
                // action stays silent either way: a secondary action that
                // buzzes is noise. D168.
                if (confirms) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick()
            },
            modifier = modifier.sizeIn(minHeight = Space.touchTarget),
            enabled = enabled,
            shape = Radius.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.blue,
                contentColor = colors.onBlue,
                disabledContainerColor = colors.sand,
                disabledContentColor = colors.ink2,
            ),
            contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.sm),
        ) { content() }

        ActionEmphasis.Quiet -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier.sizeIn(minHeight = Space.touchTarget),
            enabled = enabled,
            shape = Radius.button,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.sand,
                contentColor = colors.blue,
                disabledContainerColor = colors.sand,
                disabledContentColor = colors.ink2,
            ),
            contentPadding = PaddingValues(horizontal = Space.m, vertical = Space.sm),
        ) { content() }
    }
}

/**
 * One action as a mark alone, which is what the top corner of a page holds.
 *
 * **The label is required rather than optional**, because the mark is the only
 * thing naming this control and a reader gets nothing without it. `m3v4-5`
 * draws two of them opposite the back arrow.
 */
@Composable
fun IconAction(
    @DrawableRes symbol: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = HealthTrail.colors.ink,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Space.touchTarget),
    ) {
        Icon(
            painter = painterResource(symbol),
            contentDescription = label,
            tint = tint,
        )
    }
}
