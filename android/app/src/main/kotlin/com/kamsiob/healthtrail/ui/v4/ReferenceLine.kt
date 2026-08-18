package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A number somebody else issued, kept exactly as it was issued. #387, D196.
 *
 * **Written fresh on Material's `Surface` and the old file deleted.** It was a
 * `Row` with a clip and a background, which is what a `Surface` is.
 *
 * **Tabular figures rather than the mono face.** D173 keeps mono for figures
 * that line up in a column; a case number is read across rather than down, and
 * what it actually needs is that no digit is narrower than another. `tnum` asks
 * the reading face for that and keeps the app on `MaterialTheme.typography`.
 *
 * **One node and one sentence.** A reader saying the label and then the number
 * as two stops makes the person hold the first while waiting for the second.
 */
@Composable
fun ReferenceLine(
    /** What kind of number it is, in the person's language: "Case number". */
    label: String,
    /** The number itself, exactly as it was issued. */
    value: String,
    modifier: Modifier = Modifier,
) {
    val figures = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")

    Surface(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = Bidi.join(label, value, separator = " ")
        },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = figures, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                // Isolated, never reordered, never wrapped mid number.
                text = Bidi.isolate(value),
                style = figures,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
