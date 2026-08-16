package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.raisedCard
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Roughly square, in a grid: a drawing above a name above a count.
 * `DESIGN.md` section 11.2.
 *
 * **This is the single largest change available to this app.** Twelve notebook
 * sections as a grid is a third of the scroll length, it is scannable by shape
 * and position rather than by reading, and it immediately stops looking like a
 * settings screen. It only works because the drawings are told apart by
 * silhouette, per 5.12.1: without that a grid of tiles is only a shorter list.
 *
 * **Two sizes and only two.** Standard sits in a two column grid, compact in a
 * three column grid. Weight is carried by the drawing's fill, per 5.12 and D33,
 * never by the tile's own surface or a hue.
 *
 * **Content is start aligned, not centered.** Centered icons over centered
 * labels is the launcher grid every phone already has, and it reads as an app
 * drawer rather than as a table of contents. Start alignment also survives a
 * three line name without the block drifting and mirrors correctly in Arabic.
 *
 * **Do not use a tile** for an action, which is a button; for a list that grows,
 * because a grid of unknown length is a wall of squares; or where the person is
 * looking for one specific named item, which is a dense row.
 */
@Composable
fun Tile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Under the name, in Mono. Absent where there is nothing to count. */
    count: String? = null,
    /**
     * A tag on the count itself, not on the tile.
     *
     * The tile merges its children into one reader stop, which is right, and it
     * also means the count has no node of its own to assert against. A caller
     * that needs to check what the number says supplies this and reads it
     * through the unmerged tree.
     */
    countTestTag: String? = null,
    compact: Boolean = false,
    /**
     * The tile's own surface. `card` by default, which is what 11.2 sets.
     *
     * A sheet is already `card`, so a tile on one uses `sand` instead: a card
     * tile on a card sheet is a shape with no edges.
     */
    container: Color? = null,
    /**
     * The drawing, given the tile and drawing sizes for the size in use, so a
     * caller never restates them and the two sizes cannot drift apart.
     */
    icon: @Composable (tileSize: Dp, drawingSize: Dp) -> Unit,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, container ?: colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = modifier
            // One stop for the reader, asked for rather than relied on, per
            // D54. A tile is a drawing, a name and a count, and it is one
            // thing: "Care team, 9 items, button".
            .semantics(mergeDescendants = true) { }
            .sizeIn(minHeight = if (compact) COMPACT_MIN_HEIGHT else STANDARD_MIN_HEIGHT)
            // #324. The border below is the focus ring and is transparent
            // until something focuses this, so it is not a second definition
            // competing with the shadow. 4.7 bans a border as a card's *only*
            // definition, which is what this used to be one of.
            .raisedCard(Radius.cardLarge)
            .clip(Radius.cardLarge)
            .background(surface)
            .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.cardLarge)
            .clickable(
                interactionSource = interaction,
                // The tile's own surface is the press feedback, per 5.14.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(if (compact) Space.sm else Space.cardPadding),
    ) {
        icon(
            if (compact) COMPACT_ICON_TILE else STANDARD_ICON_TILE,
            if (compact) COMPACT_DRAWING else STANDARD_DRAWING,
        )
        Spacer(Modifier.height(if (compact) Space.s else Space.sm))
        Text(
            text = label,
            style = if (compact) HealthTrail.type.label else HealthTrail.type.displayS,
            color = colors.ink,
        )
        if (count != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = count,
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
                modifier = if (countTestTag == null) {
                    Modifier
                } else {
                    Modifier.testTag(countTestTag)
                },
            )
        }
    }
}

/**
 * How many columns a tile grid gets at the current font size, per 11.2's table.
 *
 * **The grid gives way, never the type.** A tile whose name wraps to five lines
 * is a card wearing a tile's clothes, and shrinking the text to avoid that
 * would break the 13sp floor in section 3 item 4. A one column grid of short
 * wide tiles is a legitimate layout rather than a fallback.
 */
@Composable
fun tileColumns(compact: Boolean): Int {
    val scale = LocalDensity.current.fontScale
    val base = if (compact) 3 else 2
    return when {
        scale > 1.8f -> 1
        scale > 1.3f -> minOf(base, 2)
        else -> base
    }
}

private val STANDARD_MIN_HEIGHT = 132.dp
private val COMPACT_MIN_HEIGHT = 108.dp
private val STANDARD_ICON_TILE = 40.dp
private val STANDARD_DRAWING = 24.dp
private val COMPACT_ICON_TILE = 32.dp
private val COMPACT_DRAWING = 18.dp
