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
 * **The countdown is a number and never an alarm**, 20.7. It carries the
 * ordinary ink color at every value, and a date that has passed says so in plain
 * words: "passed 6 days ago". **There is no urgency color and no red at any
 * distance from the date**,
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
    /**
     * Whether this date is the thing the screen leads with.
     *
     * **This is law 1 made a parameter.** On the closing window the countdown
     * is the lead and is drawn at `monoL`, which the grid draws large on
     * purpose. Everywhere else it sits under a lead and takes `bodyL`.
     *
     * Seen on the phone: at `monoL` under a standing card, "72 days" at 22sp
     * beat "The county" at 18sp, so the screen led with the date on a project
     * whose whole shape is that it leads with where it stands. **Two things
     * were competing and the wrong one was winning**, which is the defect law 1
     * exists to prevent, and it is invisible until it is on a phone.
     */
    prominent: Boolean = false,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .openableByTap(label = openLabel, onTap = onOpen)
            .padding(horizontal = Space.sm, vertical = Space.s)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        // **Top, not center.** The countdown is one line and the fact beside it
        // is two or three, so centering floated "63 days" in the middle of
        // "Renewal · October 17, 2026 / the letter of March 5" with nothing
        // level with it. The two belong to each other and the way to say so is
        // to start them on the same line. Seen on a project's own screen after
        // the type ladder was lifted, which is what made the fact wrap.
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            // Isolated, so a Latin number inside an Arabic layout keeps its own
            // direction rather than being pulled into the sentence beside it.
            text = Bidi.isolate(countdown),
            style = if (prominent) type.monoL else type.bodyL,
            color = colors.ink,
            // **No cap.** "In 3 days" is short in English and is a phrase in
            // every other language this ships in, and a countdown that ends
            // mid-word tells somebody a date they do not have.
            //
            // **Weighted, so it cannot take the whole row.** Uncapped and
            // unweighted it measured first: "passed 6 days ago" at `monoL` and
            // font scale 2.0 left the column beside it, which is what the row
            // is about, at zero width along with the chevron. #361.
            modifier = Modifier.weight(1f, fill = false),
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
