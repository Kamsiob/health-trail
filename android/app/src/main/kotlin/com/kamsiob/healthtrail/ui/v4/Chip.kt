package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A short counted fact, as a tonal pill. #386.
 *
 * **`m3v4-0` draws one and it is the reason this exists**: "6 readings" sits at
 * the trailing edge of the tracked measure's card, opposite the measure's own
 * name, saying how much is behind the number without competing with it. Measured
 * off the drawing at 74.7dp wide and 24.7dp tall, which is a 14sp line with
 * 12dp of air either side and 4dp above and below.
 *
 * **A fact, never a control.** It carries no press state and no minimum touch
 * target, because nothing happens when it is pressed and rule 16's promise runs
 * the other way: everything touchable responds, so anything that responds is
 * read as touchable. A chip that offered a press and did nothing would be the
 * broken control rule 16 exists to prevent.
 *
 * **It never says how something is going.** Rule 2 and law 4: this counts and
 * stops. The color it wears comes from the section it belongs to, which is
 * identity, so a chip on a measure's card is the same green as that section's
 * mark in the notebook and means nothing beyond "this is Progress".
 *
 * **Not merged for a reader here.** The caller decides: a chip inside a card
 * that already describes itself as one stop is part of that description, and a
 * chip standing on its own is its own words.
 */
@Composable
fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    /** The pill itself. A section's wash, or `sand` where the fact belongs to no section. */
    container: Color = HealthTrail.colors.sand,
    /** The words on it. A section's ink against its own wash. */
    content: Color = HealthTrail.colors.ink2,
) {
    Text(
        // bidi-ok: a chip is a counted fact the app composed from a catalog
        // template. A caller putting somebody's own words on one isolates them,
        // the way every other slot in this package does.
        text = label,
        style = HealthTrail.type.bodyM,
        color = content,
        modifier = modifier
            .clip(Radius.pill)
            .background(container)
            .padding(horizontal = Space.sm, vertical = Space.xs),
    )
}
