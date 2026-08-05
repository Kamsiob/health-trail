package com.kamsiob.healthtrail.ui.components

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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A date this project has, with the countdown and where the date came from.
 *
 * `DESIGN.md` 20.1 and 20.6. **This is the second of the three answers**, and it
 * is the one the closing window leads with.
 *
 * **The source is half the component and not a footnote.** "Apr 12, from the
 * letter of Mar 5" is usable a year later; a bare Apr 12 is not, because nobody
 * can tell whether it was written down off a letter, said on the phone, or
 * guessed. A date with no source still renders, and it simply says less.
 *
 * **The countdown is a number and never an alarm**, 20.7. It is mono and
 * tabular so a column of them lines up, it carries the ordinary ink color at
 * every value, and a date that has passed says so in plain words: "passed 6 days
 * ago". **There is no urgency color and no red at any distance from the date**,
 * because coloring by nearness would be the app performing urgency at somebody
 * who already knows, which is exactly what section 22 bans.
 *
 * **Costume: a row ending in a chevron**, opening the date's own detail, where
 * it can be edited forever, per rule 17. The whole row is the target.
 *
 * **When to use it.** Under the lead on a project, or as the lead itself on the
 * closing window.
 *
 * **When not to use it.** For an appointment, which is a different thing with a
 * place and a person and its own screen. This is a date a process has, taken off
 * a paper.
 */
@Composable
fun DateRow(
    /**
     * The countdown, already composed as a short mono string by the caller:
     * "12d", "6d ago". The deterministic engine writes it, in the person's
     * language, so this component never does arithmetic on a date.
     */
    countdown: String,
    /** What the date is: its kind, and the date itself, read as one line. */
    what: String,
    /** What a reader says instead of the row, composed by the caller. */
    description: String,
    onOpen: () -> Unit,
    /** The verb a reader announces for the tap. */
    openLabel: String,
    modifier: Modifier = Modifier,
    /**
     * Where the date was taken from, as the person would say it out loud.
     *
     * Null is a real state: plenty of dates are said on the phone and never
     * written on anything. The line is not drawn rather than being filled with
     * a placeholder.
     */
    source: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .openableByTap(label = openLabel, onTap = onOpen)
            .padding(horizontal = Space.sm, vertical = Space.s)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // Tabular and mono so a column of these lines up, and isolated so a
            // Latin number inside an Arabic layout keeps its own direction.
            text = Bidi.isolate(countdown),
            style = type.monoL,
            color = colors.ink,
            maxLines = 1,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = what,
                style = type.bodyM,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!source.isNullOrBlank()) {
                Text(
                    text = source,
                    style = type.bodyS,
                    color = colors.ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Chevron()
    }
}
