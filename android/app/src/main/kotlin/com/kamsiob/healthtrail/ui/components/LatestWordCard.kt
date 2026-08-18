package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon

/**
 * The most recent thing the office, the insurer or the facility actually said.
 *
 * `DESIGN.md` 20.1 and 20.6. **This is the third of the three answers**, and it
 * is the sentence a person repeats at the start of every call: who said it,
 * when, and the reference number they will be asked for.
 *
 * **Gold, because it is a trail entry surfaced rather than a new kind of thing.**
 * A gold wash band, the same gold the trail wears everywhere, and tapping it
 * opens the entry it quotes. That is rule 18 in both directions: the entry knows
 * the project, and the project shows the entry.
 *
 * **The words are the person's own record of what was said and are never
 * summarized.** The app quotes; it does not paraphrase, and it never adds what
 * the words might mean. Long text wraps rather than being cut to fit, because a
 * half sentence from a phone call is worse than a card one line taller.
 *
 * **The reference number wears [ReferenceLine]**, the same dress it wears
 * everywhere else in the app.
 *
 * **When to use it.** Under the lead on a project, on any of the three shapes.
 *
 * **When not to use it.** As a general quote block. This means specifically the
 * latest word on one process, and using it for anything else makes the surface's
 * one recurring landmark stop meaning that.
 */
@Composable
fun LatestWordCard(
    /** The mono eyebrow naming what this is, in the person's language. */
    eyebrow: String,
    /** What was said, in the person's own words as they recorded them. */
    words: String,
    /** Who said it and when, composed as one line by the caller. */
    attribution: String,
    /** What a reader says instead of the card, composed by the caller. */
    description: String,
    onOpen: () -> Unit,
    /** The verb a reader announces for the tap. */
    openLabel: String,
    modifier: Modifier = Modifier,
    /** What kind of reference number it is, where there is one. */
    referenceLabel: String? = null,
    /** The reference number itself, where there is one. */
    reference: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            // The wash is the resting color, so the press darkens the band
            // itself rather than a card behind it.
            .openableByTap(label = openLabel, onTap = onOpen, resting = colors.goldWash)
            .padding(horizontal = Space.sm, vertical = Space.s)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                style = type.bodyS,
                color = colors.goldInk,
            )
            Text(
                // Isolated: this is text the person typed, and in an Arabic
                // layout an English sentence reorders against it otherwise.
                text = Bidi.isolate(words),
                style = type.bodyM,
                color = colors.ink,
                modifier = Modifier.padding(top = Space.xs),
            )
            Text(
                text = attribution,
                style = type.bodyS,
                color = colors.ink2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!reference.isNullOrBlank() && !referenceLabel.isNullOrBlank()) {
                ReferenceLine(
                    label = referenceLabel,
                    value = reference,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }
        }
        Icon(
            painter = painterResource(Symbols.forward),
            contentDescription = null,
            modifier = Modifier.size(Space.markInline),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
