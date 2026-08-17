package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.v4.Eyebrow

/**
 * One tracked measurement: its name, its latest value, how much is behind it,
 * and the line the readings make. #386.
 *
 * **This is the second half of `m3v4-0`**, measured: a white card at
 * `Radius.cardLarge`, the measure's own name at the leading edge with a counted
 * chip opposite it, the value at display size under them, the line under that,
 * and the month the line starts in at the foot.
 *
 * **White is the third reserved surface, and this is why.** `docs/V4.md` 2.1
 * kept `card` for the person's own paper and for the actions band at the foot of
 * a form. The drawing puts a tracked measure on it too, and the reason holds:
 * the readings are the person's own record of something they measured, which is
 * the same kind of thing as the paper they photographed. D190.
 *
 * **It carries a hairline, and it is the only place in the app that does.**
 * `paper` and `card` are one point apart, so a white card on the canvas has no
 * edge at all without one, and the drawing gives it a soft separation rather
 * than the shadow rule 2.1 reserves for paper. An edge here is not the outline
 * the Controls section bans: that rule is about a control saying it is tappable,
 * and this is two near-identical surfaces saying where one ends.
 *
 * **The name keeps the case somebody typed.** A measure is named by the person,
 * so it drops the eyebrow's capitals and its tracking. Capitals cost about
 * fifteen percent of the width, which is how "Blood pressure, sitting" became
 * their own words cut off. D183.
 *
 * **It never says how the number is doing.** No arrow, no range, no color that
 * changes with a value, no comparison against anything. Rule 2 and law 4: this
 * shows the latest reading, counts the rest, and stops. The hue it wears is the
 * Progress section's, which is identity and says only which part of the notebook
 * this belongs to.
 *
 * **The whole card is one stop for a reader**, because a name, a value, a count
 * and a line are one fact about one measure, and four stops per measure is four
 * swipes to learn what one glance gives.
 */
@Composable
fun StatBlock(
    /** The measure's own name, in the case somebody wrote it. */
    name: String,
    /** The latest reading, already formatted. Null where nothing has been written down. */
    value: String?,
    /**
     * What a reader is told, as one sentence.
     *
     * Passed rather than built here, because the sentence is the catalog's and
     * its plural is the reader's own language's.
     */
    description: String,
    /** The section's colors. Progress, wherever this card is drawn. */
    hue: TabHue,
    modifier: Modifier = Modifier,
    /** The unit, quiet beside the figure. Null on a measure that has none. */
    unit: String? = null,
    /** How many readings there are, as a counted phrase. Null shows no chip. */
    count: String? = null,
    /** The readings themselves, for the line. Empty draws no line at all. */
    readings: List<Repository.Reading> = emptyList(),
    /**
     * What the line starts in, at the foot of the card.
     *
     * A month rather than a scale: it says when, which is a date, and it says
     * nothing about the values, which would be a judgment.
     */
    footnote: String? = null,
    /**
     * What has not been written down yet, said as an invitation.
     *
     * **A measure with no readings is a finished card, not a broken one**, rule
     * 13. It keeps its name, says plainly that nothing is recorded, and never
     * frames that as something the person failed to do.
     */
    empty: String? = null,
    /** Where the card goes. Rule 18: a number that leads nowhere is a dead end. */
    onOpen: (() -> Unit)? = null,
) {
    val colors = HealthTrail.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(colors.card)
            .border(HAIRLINE, colors.hairline, Radius.cardLarge)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onOpen)
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(Space.ml),
        // Everything in here answers one question about one measure, so it is
        // the within-group gap throughout. D188.
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Eyebrow(
                // bidi-ok: the caller isolates, because a measure's name is
                // always somebody's own words.
                text = name,
                fixed = false,
                color = hue.ink,
                modifier = Modifier.weight(1f),
            )
            count?.let {
                Chip(label = it, container = hue.wash, content = hue.ink)
            }
        }

        if (value != null) {
            BigNumber(value = value, unit = unit)
        } else if (empty != null) {
            // bidi-ok: the app's own sentence about a measure with nothing in
            // it yet, never the person's words.
            Body(text = empty, style = HealthTrail.type.bodyL)
        }

        if (readings.isNotEmpty()) {
            // **A shape, so the line takes the hue's base.** `docs/V4.md` 2.1,
            // and the same color every other chart in the app already draws in,
            // so one measure looks like itself on both screens that hold it.
            Trace(readings = readings, line = hue.base)
        }

        footnote?.let {
            Text(
                // bidi-ok: a month name the app formatted from a stored date.
                text = it,
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }
    }
}

/**
 * The edge that separates white from the canvas.
 *
 * `Space.hairlineWidth`, which is what `m3v4-0` draws: `paper` and `card` differ
 * by four values out of 255, so without it the card has no boundary at all.
 */
private val HAIRLINE = Space.hairlineWidth
