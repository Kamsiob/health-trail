package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.components.Symbols
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

/**
 * The icon tile, per `DESIGN.md` section 5.12.
 *
 * A rounded tile carrying one line drawing, which is how the reference file
 * draws every row of the table of contents. The radius comes from section 4.2,
 * which already named the icon tile before anything was built against it.
 *
 * **The drawings are paths rather than an icon font.** The same reason the
 * chevron is a path: a font can fall back to a box glyph in a language nobody
 * tested, and this app ships in four scripts. A path draws the same everywhere.
 *
 * **Icons never mirror here.** Section 4.4 mirrors directional icons, and none
 * of these is directional: a clipboard points nowhere. The trail's own icon
 * describes a branching route rather than a direction of travel, so it stays as
 * drawn in every locale, and the chevron beside it is the thing that flips.
 *
 * The stroke is held to a 3:1 non-text ratio rather than a text ratio, which is
 * what section 2.3 sets `ink3` aside for, and no icon here is ever the
 * only thing naming its row: every row carries the section's name in words.
 */
@Composable
fun IconTile(
    section: Repository.Section,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    tileSize: Dp = TILE,
    iconSize: Dp = DRAWING,
) = SymbolTile(Symbols.of(section), tint, background, modifier, tileSize, iconSize)

/**
 * The same tile, carrying one of the six capture drawings.
 *
 * The capture sheet is a two by three grid of tiles per `DESIGN.md` 11.2, and a
 * capture kind is a destination in exactly the sense 11.1 means: a fixed set the
 * person chooses from by position and shape rather than by reading.
 */
@Composable
fun IconTile(
    kind: CaptureKind,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    tileSize: Dp = TILE,
    iconSize: Dp = DRAWING,
) = SymbolTile(Symbols.of(kind), tint, background, modifier, tileSize, iconSize)

/**
 * The tile itself, given its symbol.
 *
 * Private, so a caller reaches it through one of the typed overloads above and
 * a screen never picks the drawing for a section by hand.
 */
@Composable
private fun SymbolTile(
    @DrawableRes symbol: Int,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    tileSize: Dp = TILE,
    iconSize: Dp = DRAWING,
) {
    // **Material's surface owns the shape and the two colors.** It was a `Box`
    // with a clip and a background, which is what a surface is made of rather
    // than what a surface is.
    Surface(
        modifier = modifier.size(tileSize),
        shape = MaterialTheme.shapes.medium,
        color = background,
        contentColor = tint,
    ) {
        Box(contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(symbol),
            // The row's own words name it. A reader that says the section twice
            // is worse than one that says it once.
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
        }
    }
}

/**
 * The tile and its mark, at the size the mockups draw them.
 *
 * **The tile was 36dp at a 16dp corner, which is within two points of a
 * circle**, so every section row in the app read as a bubble. `m3v4-1` draws a
 * rounded square that is clearly a square: larger, with a corner well under
 * half its size.
 *
 * Here rather than on a screen, because a measurement typed into a screen is
 * invisible to every check in this repository. D142.
 */
private val TILE: Dp = 44.dp
private val DRAWING: Dp = 24.dp

private const val VIEWPORT = 24f
private const val STROKE = 1.7f
