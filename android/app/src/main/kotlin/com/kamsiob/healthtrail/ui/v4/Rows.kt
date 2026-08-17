package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The row this interface scans, written from scratch. #386.
 *
 * **One shape, measured off `m3v4-1`**: a 44dp squircle carrying a Material
 * Symbol in the section's wash, a bold title, a quiet line under it, and a mark
 * at the end when the row is a door. The tiles sit on a 64dp pitch, which is
 * 13dp of air above and below the content, and that pitch is the difference the
 * owner named as the drawing breathing. D183.
 *
 * **The whole row is one stop for a reader.** A row is a name, a state and a
 * date, and it is one thing: fifty rows at three stops each is a hundred and
 * fifty swipes down one list.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    support: String? = null,
    @DrawableRes mark: Int? = null,
    markTint: Color = HealthTrail.colors.ink2,
    markWash: Color = HealthTrail.colors.card,
    trailing: (@Composable () -> Unit)? = null,
    isDoor: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .sizeIn(minHeight = Space.touchTarget)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
                },
            )
            .padding(horizontal = Space.ml, vertical = Space.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (mark != null) {
            Box(
                modifier = Modifier
                    .size(MARK_TILE)
                    .clip(Radius.iconTile)
                    .background(markWash),
                contentAlignment = Alignment.Center,
            ) {
                Symbol(symbol = mark, contentDescription = null, tint = markTint)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HealthTrail.type.rowTitle,
                color = HealthTrail.colors.ink,
            )
            if (!support.isNullOrBlank()) {
                Text(
                    text = support,
                    style = HealthTrail.type.bodyM,
                    color = HealthTrail.colors.ink2,
                )
            }
        }
        trailing?.invoke()
        if (isDoor && trailing == null) {
            Symbol(
                symbol = com.kamsiob.healthtrail.ui.components.Symbols.forward,
                contentDescription = null,
                tint = HealthTrail.colors.ink3,
            )
        }
    }
}

/**
 * The line between two rows inside a block.
 *
 * **Inset past the mark**, so it separates the words rather than cutting across
 * the tile, which is how `m3v4-1` draws it. Where a block holds one row there is
 * no line at all: the block's own edge ends it.
 */
@Composable
fun RowDivider(modifier: Modifier = Modifier, inset: Boolean = true) {
    HorizontalDivider(
        modifier = modifier.padding(start = if (inset) MARK_TILE + Space.sm else Space.none),
        color = HealthTrail.colors.hairline,
    )
}

/** The squircle a row's mark sits in, measured off `m3v4-1`. */
private val MARK_TILE = Space.markTile
