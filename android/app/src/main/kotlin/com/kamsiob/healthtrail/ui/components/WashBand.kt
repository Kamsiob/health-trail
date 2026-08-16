package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * A section-colored strip carrying one summary figure. `DESIGN.md` section 7.
 *
 * The grid uses it once, on Money, for the total not settled. That is the shape
 * of the thing: **a label and a number, in the section's own wash, sitting above
 * the detail rather than inside it.**
 *
 * **It is bare.** No container, no chevron, no handler. It states a figure the
 * app counted and stops. If tapping it would do something, it is a row and
 * belongs in a group.
 *
 * **It never interprets the number it carries.** No arrow, no comparison to last
 * month, no color that changes with the value. `$15,072.98 not settled` is a
 * fact the app added up. Whether that is a lot is the person's to judge, and
 * `CLAUDE.md` rule 2 is not negotiable here just because a number looks like it
 * wants a verdict.
 *
 * **At most one per screen.** Two summary strips is two things competing for the
 * top, which law 1 says means the screen is wrong.
 *
 * **The amount is Mono and tabular and the label is not.** A total is data; what
 * it is a total of is something a person reads.
 *
 * @param description what a screen reader says instead of reading the label and
 *   the number as two unrelated fragments. A band saying "Not settled yet" and
 *   "$15,072.98" separately is two announcements that do not obviously belong
 *   to each other.
 */
@Composable
fun WashBand(
    label: String,
    value: String,
    hue: TabHue,
    description: String,
    modifier: Modifier = Modifier,
) {
    val type = HealthTrail.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(hue.wash)
            .padding(horizontal = Space.cardPadding, vertical = Space.sm)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = type.bodyS,
            color = hue.ink,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = value,
            style = type.monoL,
            color = hue.ink,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
