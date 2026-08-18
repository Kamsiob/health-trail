package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.screens.HueMark
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue

/**
 * The date a project turns on, and how long there is. #387, D196.
 *
 * **Written fresh on Material's `Card` and the old file deleted.** It was a
 * `Row` with a clip and `openableByTap`, which bundles a background, a hand
 * animated pressed surface, a focus ring border and an `indication = null`
 * `clickable`. `Card` with an `onClick` is all four, and the wash is the card's
 * own container, so the press darkens the block itself rather than something
 * behind it. That parameter existed because the old modifier painted its own
 * surface underneath and the gold came out white on the phone; there is nothing
 * left for it to fight.
 *
 * **A gold tonal block.** It was a bare row once, a countdown and a fact on the
 * page's own paper, so the one date a project turns on looked like every other
 * line on the screen.
 *
 * **The number of days is the answer** and the date it resolves to is the
 * supporting line. [prominent] decides whether it competes with the screen's own
 * lead.
 *
 * **The mark is the app's own disc**, D198, so the block wears the same shape
 * every list row wears. It is decoration beside words that already say
 * everything, so the whole card carries one sentence and the mark is silent.
 */
@Composable
fun DateRow(
    countdown: String,
    what: String,
    description: String,
    onOpen: () -> Unit,
    openLabel: String,
    modifier: Modifier = Modifier,
    source: String? = null,
    prominent: Boolean = false,
) {
    val hue = goldHue()
    val scheme = MaterialTheme.colorScheme

    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = hue.wash,
            contentColor = scheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
        // One node, one sentence: the caller composed it, and everything inside
        // is silenced rather than read out twice. `docs/TRAPS.md`.
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(Space.ml),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // **No cap and no ellipsis.** This line carries the kind and
                    // the day, and a date that ends mid word is a date somebody
                    // does not have. Rule 11.
                    text = what.uppercase(LocalConfiguration.current.locales[0]),
                    style = MaterialTheme.typography.labelMedium,
                    color = hue.ink,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    // Isolated, so a Latin number inside an Arabic layout keeps
                    // its own direction rather than being pulled into the
                    // sentence beside it.
                    text = Bidi.isolate(countdown),
                    style = if (prominent) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                )
                if (!source.isNullOrBlank()) {
                    Text(
                        text = source,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            HueMark(hue = hue, mark = Symbols.appointments, size = Space.touchTarget)
        }
    }
}
