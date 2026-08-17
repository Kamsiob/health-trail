package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The way into search, in the one place it never moves from. `DESIGN.md` 21.1.
 *
 * **It is a door and not a field.** Search itself is a whole screen with its own
 * field, its own results and its own empty state, and putting a second live
 * field on Today would mean two places to type the same query and one of them
 * throwing the words away when the person navigates. This looks like the field
 * it opens, which is what makes it obvious, and it is a button.
 *
 * **It keeps its place regardless of layout**, which is the whole point of the
 * component. Finding and recording are the two acts that must never move, so
 * this and the gold capture button are fixed while everything between them is
 * the person's to arrange. A search affordance that moved with the cards would
 * be a search affordance somebody has to find first.
 *
 * **Sand, not card.** It is furniture rather than content, and a white surface
 * here would make it the fifth card on a screen whose cards are the point.
 *
 * **When to use it.** On Today, once, under the lead. That is the only place
 * universal search belongs, per `MASTER_SPEC.md` 4.8.
 *
 * **When not to use it.** Inside a section. A section searches itself with
 * [ScopedSearch], and a universal field sitting on one list is the app
 * answering a question nobody asked, with eleven other sections arriving to
 * help.
 */
@Composable
fun UniversalSearchDoor(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val strings = LocalStrings.current
    val label = strings["today.search.everything"]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .openableByTap(label = label, onTap = onOpen, resting = colors.sand)
            // One node, saying the one thing it does. The magnifier beside the
            // words is the same mark the field it opens wears, and a reader
            // announcing it too would say search twice.
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = Space.cardPadding, vertical = Space.sm),
        // **The mark sits with the first line, not with the middle of the
        // block.** Centered, it was fine at font scale 1.0 where the label is
        // one line, and at 2.0 the label wraps and the magnifier floated
        // between the two lines beside nothing. Found on the phone at maximum
        // font scale, which is the only place it exists. Rule 19's pass.
        verticalAlignment = Alignment.Top,
    ) {
        // Nudged down to the first line's optical center, because a 15dp mark
        // hung from the same top edge as a 16sp line sits visibly high.
        MagnifierMark(modifier = Modifier.padding(top = Space.xs))
        Spacer(Modifier.width(Space.s))
        Text(
            text = label,
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}
