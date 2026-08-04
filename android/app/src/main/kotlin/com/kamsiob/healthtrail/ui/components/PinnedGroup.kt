package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The person's own hierarchy, marked and floated to the top. `DESIGN.md`
 * section 7 and law 4.
 *
 * **Pinned things outrank everything on their screen, including the Today
 * hero.** That is the strongest statement law 4 makes and it is the point of
 * pinning: the app proposes an order and **the person overrules it, permanently,
 * per thing.** A pin that can be outranked by something the app decided is not
 * a pin, it is a suggestion with a marker on it.
 *
 * **Anything can be pinned:** a measure, an entry, a question set, a document, a
 * section. So this component takes arbitrary content rather than a list of one
 * kind of row.
 *
 * **The marker is gold**, because pinning is the person acting on their own
 * record, which is the same thing capture is, and gold is what the app spends
 * on that. It is not a section hue, because a pinned document and a pinned
 * person belong to the same group and that group is not a section.
 *
 * **The eyebrow says why these are here**, not just that they are. "Pinned" is
 * enough on the trail; a question set pinned for Thursday says so, because
 * "for Thursday, Dr. Prasad" is the reason it floated and the reason it will
 * stop floating.
 */
@Composable
fun PinnedGroup(
    labelKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PinnedGroupText(label = LocalStrings.current[labelKey], modifier = modifier, content = content)
}

/**
 * The same group over a label composed from the person's own data, which is the
 * common case: a pin's reason is usually theirs rather than the catalog's.
 */
@Composable
fun PinnedGroupText(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            modifier = Modifier.padding(horizontal = Space.xs),
        ) {
            PinMark(tint = colors.goldInk)
            Text(
                text = label,
                style = HealthTrail.type.mono,
                color = colors.goldInk,
            )
        }
        content()
    }
}

/**
 * The pin itself, drawn rather than taken from an icon font.
 *
 * `DESIGN.md` section 15: icons are one coherent set at one stroke weight, drawn
 * for this app, **never assembled from a default library**. A material pin glyph
 * beside this app's own hand-drawn section marks is the exact seam that makes an
 * interface read as assembled rather than made.
 *
 * **Decorative for a screen reader.** The label beside it says the group is
 * pinned, and a reader announcing "pin" before the word "pinned" is noise.
 */
@Composable
fun PinMark(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 11.dp,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.16f)

        // A pushpin seen from the side: a flat head, a tapered body, and a point
        // going into the page. Two strokes, no fill, one weight, which is the
        // set's rule.
        val head = Path().apply {
            moveTo(w * 0.28f, h * 0.10f)
            lineTo(w * 0.72f, h * 0.10f)
            lineTo(w * 0.62f, h * 0.52f)
            lineTo(w * 0.38f, h * 0.52f)
            close()
        }
        drawPath(head, color = tint, style = stroke)
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.52f),
            end = Offset(w * 0.50f, h * 0.94f),
            strokeWidth = stroke.width,
        )
    }
}
