package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue

/**
 * The last thing somebody actually said about a process. #387, D196.
 *
 * **Written fresh on Material's `Card` and the old file deleted.** It was a
 * `Row` with a clip and `openableByTap`, which bundled a background, a hand
 * animated pressed surface, a focus ring border and an `indication = null`
 * `clickable`. `Card` with an `onClick` is all of that, and the state layer is
 * Material's rather than a color this app steps toward `ink` by eight percent.
 *
 * **The wash is the card's own container**, so the press darkens the band
 * itself rather than a card behind it, which is what the old resting color was
 * arranging by hand.
 *
 * **The whole card is one stop and one sentence.** The caller composes what a
 * reader hears, because only the caller knows whose words these are.
 *
 * **When to use it.** Under the lead on a project, on any of the three shapes.
 *
 * **When not to use it.** As a general quote block. This means specifically the
 * latest word on one process, and using it for anything else makes the
 * surface's one recurring landmark stop meaning that.
 */
@Composable
fun LatestWordCard(
    /** The quiet eyebrow naming what this is, in the person's language. */
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
    val hue = goldHue()
    val scheme = MaterialTheme.colorScheme

    Card(
        onClick = onOpen,
        // **The tap, the sentence and the tag on one node**, `docs/TRAPS.md`.
        // The caller has already composed the whole card into one sentence, so
        // everything inside is silenced rather than read out twice.
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = hue.wash,
            contentColor = scheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.s),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = hue.ink,
                )
                Text(
                    // Isolated: this is text the person typed, and in an Arabic
                    // layout an English sentence reorders against it otherwise.
                    text = Bidi.isolate(words),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Space.xs),
                )
                Text(
                    text = attribution,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Space.xs),
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
                tint = scheme.outline,
            )
        }
    }
}
