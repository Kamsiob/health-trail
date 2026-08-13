package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.screens.labelKey
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import androidx.compose.material3.Text

/**
 * The head of a screen where somebody writes something down.
 *
 * **#371 item 5: the app had two header systems and they split along exactly
 * the wrong line.** Fifty five screens wore the scaffold's tab chip over a
 * 22sp title in the section's own hue. Nineteen built a bare 28sp title with no
 * chip and no hue, and those nineteen were every screen where you make
 * something. So the app changed identity at the moment somebody wrote in it,
 * which is the moment it should feel most like the same place.
 *
 * **The chip says you are still where you were.** Adding a medication is a
 * thing you do inside Medications, not a departure from it, and the hue is what
 * carries that without a word. `DESIGN.md` law 2 and 4.3.
 *
 * **The title comes down to `displayM` to match**, rather than the chip being
 * added under a heavier one. Two headers at two sizes is the same split wearing
 * a smaller difference, and law 1 gives the screen one dominant thing: on a
 * form that is the first question, not the name of the form.
 *
 * **Four screens deliberately do not use this and keep a bare title.** The
 * disclaimer and setup come before a notebook exists, so there is no section
 * they are inside and a chip would name a place the person has not reached.
 * The situation picker is choosing what the notebook is about, and correcting
 * the cared-for person's name belongs to the whole app rather than to one
 * section. A chip on any of the four would be decoration rather than location.
 */
@Composable
fun FormHeader(
    title: String,
    lead: String,
    section: Repository.Section,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = modifier) {
        Spacer(Modifier.height(Space.sm))
        TabChipText(
            hue = hueFor(section),
            label = strings[labelKey(section)],
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = title,
            style = HealthTrail.type.displayM,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = lead,
            style = HealthTrail.type.bodyM,
            color = colors.ink2,
        )
    }
}
