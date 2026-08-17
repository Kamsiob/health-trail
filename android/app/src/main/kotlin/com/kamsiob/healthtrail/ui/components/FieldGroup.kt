package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A handful of related fields, in a container of their own. D174.
 *
 * **The owner, looking at the setup form:** "pages like that are just entry box.
 * text And box text And box text with lines in between. there needs to be some
 * design elements ... maybe they're broken into containers or cart or something
 * but that's just messy."
 *
 * **The defect is uniform weight, wearing a form.** Every field on the page was
 * a label, a box and a gap, at the same weight, for as far as the screen
 * scrolled; the only structure was a hairline with a word sitting on it, which
 * separates without organizing. Fifteen equal things means the person does the
 * sorting, which is rule 15, and rule 15 does not stop applying because the
 * screen happens to be an input form.
 *
 * **So a group is a real object.** The fields inside it sit on one raised
 * surface with a generous radius, which is what the expressive baseline does
 * with grouped list items: the page becomes three or four chunks a person can
 * take one at a time rather than one undifferentiated column. Scrolling past a
 * container is scrolling past a thing you have finished.
 *
 * **The heading sits above the container rather than on a rule.** It names what
 * the group is for and then gets out of the way, in the quiet ink, at body
 * size. The container's own edge does the separating that the hairline was
 * doing badly.
 *
 * **Nothing here asks for completion.** Rule 13: a group with one field filled
 * and three empty is a finished state, and this component has no counter, no
 * progress and no way to mark itself incomplete.
 */
@Composable
fun FieldGroup(
    /** The catalog key naming what these fields are about. */
    titleKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings[titleKey],
            style = HealthTrail.type.bodyS,
            color = colors.ink2,
            modifier = Modifier.padding(start = Space.cardPadding),
        )
        Spacer(Modifier.height(Space.s))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.cardLarge)
                .background(colors.sand)
                .padding(horizontal = Space.cardPadding, vertical = Space.cardPadding),
        ) {
            content()
        }
    }
}

/** The gap between one group and the next. Bigger than the gap inside one. */
@Composable
fun FieldGroupGap() {
    Spacer(Modifier.height(Space.sectionGap))
}
