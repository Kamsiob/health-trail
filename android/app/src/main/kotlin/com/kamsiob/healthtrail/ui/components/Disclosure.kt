package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The rest of a form, behind one control nobody is required to touch.
 *
 * **Progressive disclosure is part of hierarchy rather than a separate
 * feature**, per `DESIGN.md` 10.8, and everything visible at once is the most
 * common structural tell on the ban list in section 1. On a year five notebook
 * the capture form put roughly twenty three chips in front of somebody standing
 * in a corridor, which is the opposite of forgiving.
 *
 * **It opens and stays open.** There is no close control, because a person who
 * opened it wanted what is inside and taking it away again would be the form
 * arguing with them. Leaving the screen resets it, which is right: the next
 * capture starts from the short form.
 *
 * **It never carries a count and never says how much is left.** "Add more" is an
 * offer. "3 more fields" would be a measure of how incomplete the entry is,
 * which rule 13 rules out: an unfilled slot reads as "not yet", never as a
 * deficiency.
 *
 * **Nothing inside it is ever required**, and the aside says so once. A
 * disclosure that hides something the person has to fill in is not disclosure,
 * it is a trap.
 */
@Composable
fun Disclosure(
    modifier: Modifier = Modifier,
    /** Overridable so a screen can say what it is offering in its own words. */
    labelKey: String = "capture.more",
    asideKey: String? = "capture.more.aside",
    testTag: String? = null,
    content: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val motion = LocalMotion.current
    // Saveable, so a rotation or a theme change does not fold the form back up
    // under somebody part way through filling it in.
    var open by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!open) {
            TextAction(
                label = strings[labelKey],
                onClick = { open = true },
                modifier = if (testTag == null) {
                    Modifier
                } else {
                    Modifier.testTag(testTag)
                },
            )
            if (asideKey != null) {
                Text(
                    text = strings[asideKey],
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
            }
        }

        // **The standard spring from section 6, taken through `LocalMotion`**,
        // never built inline, because a spec built inline is one the reduced
        // motion setting cannot reach. With motion reduced, `standard()` is a
        // snap and the expansion becomes an immediate state change, which is
        // what section 6 asks for.
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(animationSpec = motion.standard<IntSize>()) +
                fadeIn(animationSpec = motion.quick<Float>()),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(Space.s))
                content()
            }
        }
    }
}
