package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A heading over a run of rows, per `DESIGN.md` section 5.13: a mono eyebrow
 * with a hairline running out to the end edge, which is how the reference file
 * heads a month in the trail.
 *
 * **One component, because a long list is grouped one way in this app.** The
 * notebook and the situation picker both needed it in the same session, and
 * building it twice would have been the defect section 10.2 names: a pattern
 * appearing in two forms, where the fix is always to correct the earlier one
 * rather than leave both standing.
 *
 * The label is uppercased against the catalog's own locale rather than the
 * device's, so a Turkish phone showing the English catalog cannot turn an "i"
 * into a dotted capital. In Arabic and Chinese it is a no-op, which is correct:
 * neither script has case, and the eyebrow reads as an eyebrow there through
 * its size and tracking.
 *
 * The label carries no layout weight and the rule carries all of it, so the
 * label takes exactly the width it needs. A label long enough to fill the row,
 * which is what the longest language does, wraps inside the row and the rule
 * shrinks to nothing rather than pushing the words off the end edge.
 *
 * The rule is decorative in the sense section 2.3 defines: remove it and
 * nothing becomes unreadable, because the words carry the heading alone.
 */
@Composable
fun GroupHeader(labelKey: String, modifier: Modifier = Modifier) {
    GroupHeaderText(label = LocalStrings.current[labelKey], modifier = modifier)
}

/**
 * The same header over a label the catalog cannot hold, which in practice means
 * a month in the trail.
 *
 * **A month heading is data, not copy.** "November 2024" is composed from an
 * entry's own date through the catalog's own pattern, so it cannot be a key.
 * It shares this component rather than growing a second one, because section
 * 5.13 describes one way of heading a run of rows and two implementations of it
 * would drift the moment either was touched.
 */
@Composable
fun GroupHeaderText(
    label: String,
    modifier: Modifier = Modifier,
    /**
     * How many rows are under this heading.
     *
     * **A count of what is in the group and never a completion count.** Rule 13
     * rules out measuring the person's own work, so this says how many things
     * are here in the same words every other count in the app uses.
     */
    count: String? = null,
    /**
     * The count as a sentence, for the reader. "3" beside a word announces as
     * "Equipment 3", which is not what it means. Section 9: what is read aloud
     * says the same thing the screen says.
     */
    countDescription: String? = null,
    /**
     * The eyebrow's ink, for the one heading in the app that marks something
     * asking for an answer rather than naming a group.
     *
     * **Null is the quiet register and is what almost every caller wants.**
     * Grid screen 14 draws "Needs attention" over the bill that wants a
     * decision in alert ink, and section 9 is satisfied because the words say
     * it and the color only agrees with them.
     */
    tint: Color? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // **The count joins the label rather than sitting beside it.** The label
    // carries no layout weight so a long one wraps, and a separate count Text
    // after it would be the thing squeezed off the end edge in the longest
    // language. Joined, it wraps with the words it belongs to.
    val shown = Bidi.join(label.uppercase(strings.locale), count)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (countDescription != null) {
                    contentDescription = Bidi.join(label, countDescription)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = shown,
            style = HealthTrail.type.mono,
            color = tint ?: colors.ink2,
        )
        Spacer(Modifier.width(Space.sm))
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(
                color = colors.ink3.copy(alpha = HAIRLINE_ALPHA),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
            )
        }
    }
}

private const val HAIRLINE_ALPHA = 0.4f
