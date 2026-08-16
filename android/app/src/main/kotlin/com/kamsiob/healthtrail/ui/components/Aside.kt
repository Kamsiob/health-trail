package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor

/**
 * A short note from the app, on a tinted surface, with the section's own icon.
 *
 * **It is in two of the six approved v4 mockups and existed in neither the
 * component library nor the code.** The medication form opens with one in the
 * medication hue saying "Whatever you were told, in the words you were told it
 * in"; the document screen carries one in manila under "WHERE THE PAPER IS".
 * Both were being drawn as a bare `Text` in the secondary ink, which is this
 * app's caption treatment, so the one line on the screen that sets the terms
 * for everything under it read as the smallest thing on it.
 *
 * **The tint is the section's wash and never the base.** V4 section 1: one
 * saturated block per screen, and on a form that block is not the aside. A wash
 * with the section's icon says which part of the notebook this is without
 * competing with the lead or with the one filled button.
 *
 * **It never gives advice.** Rule 2, and this component is exactly where that
 * rule would be broken first: a tinted card at the top of a form with an icon
 * beside it is the shape every health app uses to tell somebody what a number
 * means. What goes here is what the app is asking for and how to answer it,
 * never what the answer should be.
 *
 * **It speaks as one node.** The icon carries no description of its own, so a
 * reader says the sentence once rather than announcing a decorative drawing
 * first.
 *
 * @param eyebrow the quiet line naming what the note is about, or null where
 *   the sentence names itself. Uppercased by the caller in its own locale,
 *   never here, because Turkish and Azeri do not uppercase `i` the way the
 *   default rules do.
 */
@Composable
fun Aside(
    text: String,
    section: Repository.Section,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val hue = hueFor(section)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(hue.wash)
            .padding(Space.cardPadding)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        IconTile(
            section = section,
            tint = hue.ink,
            // Transparent, because the surface behind it is already the wash.
            // A tile on a tile is two rounded rectangles saying one thing.
            background = androidx.compose.ui.graphics.Color.Transparent,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            if (eyebrow != null) {
                Text(text = eyebrow, style = type.eyebrow, color = hue.ink)
                Spacer(Modifier.height(Space.xs))
            }
            // **`ink` rather than `ink2`.** This is the app talking, at the top
            // of a form, and the secondary ink is what made it read as a
            // footnote in the first place.
            Text(text = text, style = type.bodyM, color = colors.ink)
        }
    }
}
