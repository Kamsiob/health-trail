package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A reference number, in the dress it wears everywhere in the app.
 *
 * `DESIGN.md` 20.6: mono in a sand pill, and **the standard dress for reference
 * numbers everywhere, not only on projects.** It arrived with the Projects grid
 * because that is where reference numbers matter most, and it is general
 * because a claim number on a bill is the same kind of thing as a case number
 * on a waiver application.
 *
 * **Why a reference number gets its own component at all.** It is the string a
 * person reads down a phone line to somebody who will not help them without it,
 * often while holding something else. It has to be findable at a glance and
 * unambiguous character by character, which is what mono is for, and it must
 * never be reflowed, hyphenated, or reordered.
 *
 * **It is bare.** Information, not a control. There is no copy button, because
 * a control that silently changes the clipboard is a control that has to be
 * explained, and the number is short enough to read aloud, which is what people
 * actually do with it.
 *
 * **The number is isolated**, per section 15. A Latin case number inside an
 * Arabic sentence reorders without it, and a reference number that renders in
 * the wrong order is worse than one that is missing: it is wrong and it looks
 * right.
 *
 * **When to use it.** Any identifier issued by somebody else that a person will
 * have to quote back: a case number, a claim number, a confirmation number, a
 * reference on a letter.
 *
 * **When not to use it.** For anything the app generated. A person never reads
 * an internal id to anybody, and dressing one like this says it matters.
 */
@Composable
fun ReferenceLine(
    /** What kind of number it is, in the person's language: "Case number". */
    label: String,
    /** The number itself, exactly as it was issued. */
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    // **Held in a local first**, which is not a style preference. Referencing
    // the token inline inside this modifier chain crashes Compose lint's
    // `ModifierParameterDetector`, reproducibly, on this file alone. The token
    // is `.refline` at 7px in the Projects grid either way.
    val shape = Radius.referenceLine

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.sand)
            .padding(horizontal = Space.s, vertical = Space.xs)
            // One node and one sentence: a reader saying the label and then
            // the number as two stops makes the person hold the first while
            // waiting for the second.
            .clearAndSetSemantics {
                contentDescription = Bidi.join(label, value, separator = " ")
            },
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = type.mono,
            color = colors.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            // Isolated, never reordered, never wrapped mid number.
            text = Bidi.isolate(value),
            style = type.mono,
            color = colors.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
