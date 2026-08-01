package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
    width: Dp = 8.dp,
    height: Dp = 14.dp,
) {
    val colors = HealthTrail.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val strokeWidth = 2.dp.toPx()
        val midY = size.height / 2f
        // Forward means the end edge, whichever edge that is in this direction.
        val forward = pointsForward != rtl
        val tipX = if (forward) size.width else 0f
        val baseX = if (forward) 0f else size.width

        drawLine(
            color = colors.ink3NonText,
            start = Offset(baseX, 0f),
            end = Offset(tipX, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.ink3NonText,
            start = Offset(tipX, midY),
            end = Offset(baseX, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
