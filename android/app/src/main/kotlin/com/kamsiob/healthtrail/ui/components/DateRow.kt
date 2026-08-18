package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
            // **A gold tonal block, which is what `m3v4-2` draws here.** It was
            // a bare row: a countdown, a fact and a chevron on the page's own
            // paper, so the one date a project turns on looked like every other
            // line on the screen. Every color in the drawing's block is already
            // a token here, `goldWash` behind `goldInk` with a `gold` circle,
            // measured off the PNG rather than matched by eye. D183.
            // **The resting color goes to `openableByTap`, not to a
            // `background` before it.** That modifier draws its own surface so
            // a press can darken it, and it defaults to `card`: a gold block
            // painted underneath it came out white on the phone, which is the
            // whole reason this line is a parameter rather than a fill.
            .openableByTap(label = openLabel, onTap = onOpen, resting = colors.goldWash)
            .padding(Space.ml)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = what.uppercase(LocalConfiguration.current.locales[0]),
                style = type.eyebrow,
                color = colors.goldInk,
                // **No cap and no ellipsis.** This line carries the kind and
                // the day, and a date that ends mid word is a date somebody
                // does not have. Rule 11.
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                // Isolated, so a Latin number inside an Arabic layout keeps its
                // own direction rather than being pulled into the sentence
                // beside it.
                text = Bidi.isolate(countdown),
                // **The big thing in the block**, which is what the drawing
                // leads with: the number of days is the answer, and the date
                // it resolves to is the supporting line. `prominent` still
                // decides whether this competes with the screen's own lead.
                style = if (prominent) type.displayM else type.displayS,
                color = colors.ink,
            )
            if (!source.isNullOrBlank()) {
                Text(
                    text = source,
                    style = type.bodyS,
                    color = colors.ink2,
                )
            }
        }
        // **The mark, in the accent the block is made of.** `m3v4-2` puts a
        // filled gold circle here carrying a calendar. It is decoration beside
        // words that already say everything, so it is not announced.
        Box(
            modifier = Modifier
                .size(Space.touchTarget)
                .clip(CircleShape)
                .background(colors.gold),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Symbols.appointments),
                contentDescription = null,
                tint = colors.onGold,
            )
        }
    }
}
