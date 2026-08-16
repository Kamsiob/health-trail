package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * One step of a project, with who said they would handle it.
 *
 * `DESIGN.md` 20.6. **The box is the interactive element and the handler tag is
 * data**: mono, bare, and it does nothing on touch, because tapping a name that
 * did nothing would suggest the app can reach that person, and it cannot.
 *
 * **A handler tag is a label and never an identity**, D108. No account, no
 * address, no notification, no second user, and nothing about it leaves the
 * device. It is the person writing down who said they would do a thing, which is
 * how these weeks actually get survived.
 *
 * **Done is not a score.** A step that is done is a fact about the step, and the
 * cluster above counts them because a person arranging a discharge genuinely
 * needs to know what is left. **What never appears is a percentage of the
 * person's own diligence**, per rule 13, and nothing here ever says a step is
 * late or overdue.
 *
 * **The whole row is the target**, not the fifteen point box: a small square is
 * unusable one handed on a phone somebody is holding in a corridor.
 *
 * **When to use it.** In a cluster on the busy stretch, and in the steps fold on
 * the other two shapes.
 *
 * **When not to use it.** For a question, which is a sentence somebody asks and
 * therefore a dense row, per rule 22 and the lesson the prep sheet taught. And
 * never for anything the app is telling the person to do: these are the steps
 * they wrote or accepted, and the app never adds one.
 */
@Composable
fun StepRow(
    text: String,
    done: Boolean,
    onToggle: (Boolean) -> Unit,
    /** What a reader says instead of the row, composed by the caller. */
    description: String,
    modifier: Modifier = Modifier,
    /** Who said they would handle it. A label the person wrote down. */
    handler: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), RoundedCornerShape(0.dp))
            .toggleable(
                value = done,
                interactionSource = interaction,
                indication = null,
                role = Role.Checkbox,
                onValueChange = onToggle,
            )
            .defaultMinSize(minHeight = Space.touchTarget)
            .padding(horizontal = Space.sm, vertical = Space.s)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // **The box and its mark are drawn, not set in type.**
        //
        // The mark was a "✓" in a Text inside a fixed 16dp box. Text scales with
        // the font setting and the box does not, so at font scale 2.0 the glyph
        // was cut in half: a checkbox that reads as damaged rather than as
        // ticked, on the setting the people this app is for are most likely to
        // be using. Found on the phone at 2.0, invisible at 1.0.
        //
        // **A mark rather than only a fill**, section 9, so it survives
        // grayscale and every color vision difference.
        Box(
            modifier = Modifier
                .size(18.dp)
                .drawBehind {
                    val corner = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                    if (done) {
                        drawRoundRect(color = colors.leaf, cornerRadius = corner)
                        val stroke = 2.dp.toPx()
                        val w = size.width
                        val h = size.height
                        drawLine(
                            color = colors.paper,
                            start = Offset(w * 0.24f, h * 0.52f),
                            end = Offset(w * 0.44f, h * 0.72f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = colors.paper,
                            start = Offset(w * 0.44f, h * 0.72f),
                            end = Offset(w * 0.76f, h * 0.30f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    } else {
                        drawRoundRect(
                            color = colors.hairlineHeavy,
                            cornerRadius = corner,
                            style = Stroke(width = 1.8.dp.toPx()),
                        )
                    }
                },
        )

        Text(
            text = Bidi.isolate(text),
            style = type.bodyM,
            color = if (done) colors.ink2 else colors.ink,
            modifier = Modifier.weight(1f),
        )

        if (!handler.isNullOrBlank()) {
            Text(
                // Isolated, because a name is text the person typed.
                text = Bidi.isolate(handler),
                style = type.bodyS,
                color = colors.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
