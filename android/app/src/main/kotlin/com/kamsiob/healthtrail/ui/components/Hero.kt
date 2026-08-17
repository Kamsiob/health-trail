package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.Eyebrow

/**
 * The single thing the person came for, at the top of the screen.
 * `DESIGN.md` section 11.5.
 *
 * **Exactly one per screen at most, and no hero at all is a valid screen.** It
 * is 10.8 step one rendered: decide what matters most, name it out loud, and
 * this is that thing. **If you cannot name it, the screen does not have one**,
 * and inventing a hero to fill the slot is the decorative banner section 1 bans.
 *
 * **It is not a card and must not be one.** It sits directly on `paper` with no
 * surface, no shadow and no border, which is what makes it read as the top of
 * the page rather than as the first item in a list. That is hierarchy without
 * decoration, which is what rule 15 asks for.
 *
 * **The space is the component.** 24dp above and 32dp below, and the 32dp is
 * the first thing sacrificed when a screen is built to be merely correct.
 * Without it the hero is just a larger row.
 *
 * **Never a count of the person's own diligence**, per rule 13, which rules out
 * "4 of 7 steps done" as a hero forever.
 */
@Composable
fun Hero(
    /** A mono eyebrow saying what this is, per 5.13's label without its rule. */
    eyebrowKey: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(Space.l))
        Eyebrow(text = LocalStrings.current[eyebrowKey])
        Spacer(Modifier.height(Space.sm))
        content()
        Spacer(Modifier.height(Space.xl))
    }
}

/**
 * One line of a hero, at real size, opening what it names.
 *
 * **A hero block may carry two of these and never three.** Two things needing
 * attention is a fact about a notebook; three is a list, and a list at display
 * size is a screen shouting. Where there would be three, the block carries the
 * two and the rest stay in their own sections where they already live.
 *
 * Display M rather than Display L, because two of them at 28sp on a 360dp
 * screen is most of the fold. The size is what makes it a hero; the eyebrow and
 * the space around the block are what make it one thing.
 */
@Composable
fun HeroLine(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    // Transparent at rest, per 11.5: the hero has no surface of its own. The
    // 5.14 rule still reaches it, because 8% toward `ink` from transparent is a
    // tint rather than a surface, which is the right weight for something that
    // is not a button.
    val surface by pressedSurface(interaction, Color.Transparent)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.tile)
            .background(surface)
            .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.tile)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = HealthTrail.type.displayM,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Space.sm))
        Chevron()
    }
}
