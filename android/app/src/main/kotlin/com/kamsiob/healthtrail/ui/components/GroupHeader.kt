package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail

/**
 * A heading over a run of rows: an eyebrow, and nothing else.
 *
 * **The hairline is gone and the words are uppercase**, which is what the
 * approved v4 mockups draw. `m3v4-1` heads two groups with "PEOPLE AND CARE"
 * and "THE RECORD", tracked and quiet, with clear space to the end edge;
 * `m3v4-3` heads a list with "EVERYONE ELSE ON THE UNIT" the same way. D173:
 * where the mockup and `DESIGN.md` 5.13 disagree, the mockup wins.
 *
 * **The rule was decorative by this file's own account**, "remove it and
 * nothing becomes unreadable, because the words carry the heading alone", and
 * on the phone it was the loudest thing about a heading meant to recede: a
 * line running the width of the screen out of every group name in the app.
 *
 * **This is the app's eyebrow and it was set in `bodyS`**, which is the same
 * style as the row subtitles underneath it. D176 gave the eyebrow its own
 * token and this component was invisible to that pass, because the pass swept
 * for the mono face and this had never been mono. Uniform weight, rule 15,
 * found by looking at the screen rather than by any check.
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
 * The label carries no layout weight, so it takes exactly the width it needs
 * and a label long enough to fill the row, which is what the longest language
 * does, wraps inside the row rather than running off the end edge.
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
    val shown = Bidi.join(label, count)

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
            // **Uppercased here, which this file has claimed since it was
            // written and never did.** The KDoc above described the locale
            // rule in detail and the code passed the label through untouched,
            // so every group heading in the app read as a sentence in the
            // caption ink. Against `strings.locale` rather than the device's,
            // so a Turkish phone showing the English catalog cannot turn an
            // "i" into a dotted capital.
            text = shown.uppercase(strings.locale),
            style = HealthTrail.type.eyebrow,
            color = tint ?: colors.ink2,
        )
    }
}
