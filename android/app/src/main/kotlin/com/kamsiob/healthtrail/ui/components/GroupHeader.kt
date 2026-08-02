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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
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
fun GroupHeaderText(label: String, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(strings.locale),
            style = HealthTrail.type.mono,
            color = colors.ink3Text,
        )
        Spacer(Modifier.width(Space.sm))
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(
                color = colors.ink3NonText.copy(alpha = HAIRLINE_ALPHA),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
            )
        }
    }
}

private const val HAIRLINE_ALPHA = 0.4f
