package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue

/**
 * Where a project stands: whose hands it is in, and since when. #387, D196.
 *
 * **Written fresh on Material's `Card` and the old file deleted.** It was a
 * `Column` with a clip and a background, which is what a card is made of rather
 * than what a card is.
 *
 * **This is the first of the three answers**, and on the long road it is the one
 * thing the screen leads with, because it is what somebody opens the project to
 * find out.
 *
 * **The sentence is bare and the card does not open anything**: there is nowhere
 * more detailed to go than the sentence it is already showing. Its actions,
 * where there are any, are pills, and each one is a verb or a dialable number.
 *
 * **The elapsed fact is a fact and never a judgment.** "23 days so far" is
 * counted from a date somebody recorded. It is not late, it is not overdue, and
 * it carries no color: an urgency color here would be the app telling a person
 * how to feel about a wait they can do nothing about.
 *
 * **Nobody is an adversary.** The holder is named by role, as the person wrote
 * it down: "The county", "The insurer", "My brother".
 */
@Composable
fun StandingCard(
    eyebrow: String,
    holder: String,
    modifier: Modifier = Modifier,
    /**
     * Null when nobody has said anything about how long or what: a real state,
     * and the card simply does not draw the line rather than filling it.
     */
    since: String? = null,
    /**
     * Pills, each a verb or a dialable number.
     *
     * **Null rather than an empty lambda when there are none**, because an
     * empty row still costs its own top padding and leaves a band of dead space
     * under the sentence that reads as something failing to load.
     */
    actions: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = scheme.surfaceContainer,
            contentColor = scheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.sm),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = goldHue().ink,
            )
            Text(
                text = holder,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(top = Space.xs)
                    // The answer, so it is the heading a reader jumps to.
                    .semantics { heading() },
            )
            if (!since.isNullOrBlank()) {
                Text(
                    text = since,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }
            if (actions != null) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.s),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    actions()
                }
            }
        }
    }
}
