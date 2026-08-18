package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Surface
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
 * `Row` with a clip and the old `openableByTap`, which bundled a background, a hand
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
    /** Whether this draws its own tonal card, or sits inside somebody else's. */
    flat: Boolean = false,
) {
    val hue = goldHue()
    val scheme = MaterialTheme.colorScheme

    // **Flat draws no container of its own**, because its caller already is
    // one. D206 puts where it stands and the next date inside a single block:
    // they were two tonal cards a gap apart, and the second was drawn louder
    // than the first, so the screen had two possible leads and `docs/V4.md`
    // 6.1 item 1 says that is no lead at all. A wash card nested in a wash
    // card is also the defect item 4 names, a container drawn in the color of
    // what is under it.
    //
    // **The press still answers either way.** Rule 16: a control that does
    // nothing under a finger reads as broken, so flat is a transparent
    // `Surface` with Material's own state layer rather than a bare `Column`.
    val shell: @Composable (@Composable () -> Unit) -> Unit = { body ->
        if (flat) {
            Surface(
                onClick = onOpen,
                color = Color.Transparent,
                contentColor = scheme.onSurface,
                // **Square, because a rounded clip eats the words.** Looked at
                // on the phone: with the block's own padding placing this row's
                // text hard against the surface's left edge, a large corner
                // radius clipped the first letter of the top line and the last,
                // so "RENEWAL" rendered as "ENEWAL". The container around it
                // owns the corners; this one only needs to take the press.
                shape = RectangleShape,
                modifier = modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = description },
            ) { body() }
        } else {
            Card(
                onClick = onOpen,
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = hue.wash,
                    contentColor = scheme.onSurface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
                // One node, one sentence: the caller composed it, and everything
                // inside is silenced rather than read out twice. `docs/TRAPS.md`.
                modifier = modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = description },
            ) { body() }
        }
    }

    shell {
        Row(
            modifier = Modifier.padding(if (flat) Space.none else Space.ml),
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
