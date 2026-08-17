package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail

/**
 * The chevron, drawn with a Canvas so it needs no icon font and cannot fall
 * back to a box in any language.
 *
 * **It mirrors.** Directional icons flip in a right to left layout, per
 * `DESIGN.md` section 4.4, and a chevron still pointing right in Arabic is the
 * single most likely direction defect in an app like this. The layout direction
 * is read at draw time rather than assumed, which is why this is a Canvas and
 * not a rotated asset.
 *
 * **One chevron for the whole app.** It was written once for the notebook rows
 * and was about to be written a second time for the date picker's month steps,
 * which is the defect section 10.2 names. Extracted before the second one
 * shipped.
 *
 * Non-text, so it is held to the 3:1 boundary rather than a text ratio, and it
 * carries no content description of its own: the thing it sits inside is the
 * button, and that is what says where it goes.
 *
 * @param pointsForward toward the end edge, which is right in English and left
 *   in Arabic. False points back the other way, for a previous step.
 */
@Composable
fun Chevron(
    modifier: Modifier = Modifier,
    pointsForward: Boolean = true,
    width: Dp = SIZE,
    height: Dp = SIZE,
) {
    // **Material's mark, not two lines this app draws.** #386, `docs/V4.md` 4
    // lists "the app's hand-drawn switch, chevron and controls" among the
    // things being replaced, and this one sat at the end of rows on most
    // screens in the app: two `drawLine` calls with their own stroke, their
    // own cap and their own direction flip.
    //
    // **The direction still flips**, because `chevron_right` is a directional
    // mark and `DESIGN.md` 4.4 mirrors those. The vector drawable is
    // autoMirrored through the layout direction rather than by arithmetic
    // here, and `pointsForward = false` picks the other symbol outright.
    //
    // **Still non-text and still unannounced.** The thing it sits inside is
    // the button, and that is what says where it goes.
    Symbol(
        symbol = if (pointsForward) Symbols.forward else Symbols.back,
        contentDescription = null,
        modifier = modifier.size(width = width, height = height),
        tint = HealthTrail.colors.ink3,
    )
}

/**
 * The mark's box, which is square now rather than 8 by 14.
 *
 * A Material Symbol is drawn on a square grid: given the old rectangle it
 * squeezed the glyph. The visual weight is the same because the symbol's own
 * strokes sit well inside its box.
 */
private val SIZE: Dp = 20.dp
