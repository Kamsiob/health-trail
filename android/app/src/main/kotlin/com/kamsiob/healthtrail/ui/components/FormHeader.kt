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
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Lead
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
    /**
     * The sentence under the title, or null where the screen says it in an
     * [Aside] instead.
     *
     * **Null is not "no lead", it is "the lead is a real object".** The
     * approved v4 mockup puts a form's opening sentence on the section's wash
     * with its own icon, because it is the line that sets the terms for
     * everything below it, and the caption ink under a heading is where a
     * sentence goes to be skipped.
     */
    lead: String?,
    section: Repository.Section,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(modifier = modifier) {
        Spacer(Modifier.height(Space.sm))
        // **The tab chip is gone**, `docs/V4.md` 4: it named the section
        // directly above a heading that names the form, which is two names for
        // one place, and it was the last piece of the old binder language left
        // on the forms. The section is where the person came from and the app
        // has said it once already. #386.
        //
        // **The eyebrow says which section this belongs to**, in that section's
        // own ink, which is what the drawings put above a display title.
        Eyebrow(text = strings[labelKey(section)], color = hueFor(section).ink)
        Lead(text = title, modifier = Modifier.semantics { heading() })
        if (lead != null) {
            // bidi-ok: a form's opening sentence is the app's own words about
            // what the form is for, never anything somebody typed.
            Body(text = lead, style = HealthTrail.type.bodyL)
        }
    }
}
