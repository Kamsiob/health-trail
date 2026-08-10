package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.raisedCard
import com.kamsiob.healthtrail.ui.theme.Radius
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Where a project stands: whose hands it is in, and since when.
 *
 * `DESIGN.md` 20.1 and 20.6. **This is the first of the three answers**, and on
 * the long road it is the one thing the screen leads with, at display scale,
 * because it is what somebody opens the project to find out.
 *
 * **The sentence is bare and the card is a grouped surface.** The card itself
 * does not open anything: there is nowhere more detailed to go than the sentence
 * it is already showing. Its actions, where there are any, are outlined pills,
 * and each one is a verb or a dialable number.
 *
 * **The elapsed fact is a fact and never a judgment**, 20.7. "23 days so far" is
 * counted from a date somebody recorded. It is not late, it is not overdue, and
 * it carries no color: an urgency color here would be the app telling a person
 * how to feel about a wait they can do nothing about. The caller composes the
 * sentence with the deterministic engine, the same one that writes the digest.
 *
 * **Nobody is an adversary**, section 22. The holder is named by role, as the
 * person wrote it down: "The county", "The insurer", "My brother".
 *
 * **When to use it.** At the top of a project that leads with where it stands,
 * and under the lead on the other two shapes.
 *
 * **When not to use it.** For anything with a next action attached. This says
 * what is happening elsewhere, and a card that says "the county is reviewing it"
 * with a filled action underneath it is the app suggesting the person should be
 * doing something about a wait, which 20.7 rules out.
 */
@Composable
fun StandingCard(
    /** The mono eyebrow naming what this is, in the person's language. */
    eyebrow: String,
    /** Whose hands it is in, as the person wrote it down. */
    holder: String,
    modifier: Modifier = Modifier,
    /**
     * The elapsed fact and what is happening, already composed as one sentence.
     *
     * Null when nobody has said anything about how long or what: a real state,
     * and the card simply does not draw the line rather than filling it.
     */
    since: String? = null,
    /**
     * Outlined pills, each a verb or a dialable number.
     *
     * **Null rather than an empty lambda when there are none**, because an
     * empty row still costs its own top padding and leaves a band of dead
     * space under the sentence that reads as something failing to load.
     */
    actions: (@Composable () -> Unit)? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .raisedCard(Radius.card)
            .clip(Radius.card)
            .background(colors.card)
            .padding(horizontal = Space.sm, vertical = Space.sm),
    ) {
        Text(
            text = eyebrow,
            style = type.mono,
            color = colors.goldInk,
        )
        Text(
            text = holder,
            style = type.displayS,
            color = colors.ink,
            modifier = Modifier
                .padding(top = Space.xs)
                // The answer, so it is the heading a reader jumps to.
                .semantics { heading() },
        )
        if (!since.isNullOrBlank()) {
            Text(
                text = since,
                style = type.bodyS,
                color = colors.ink2,
                modifier = Modifier.padding(top = 2.dp),
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
