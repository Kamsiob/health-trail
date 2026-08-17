package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Radius
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
 *
 * **It never hides something already written down**, which is what [startOpen]
 * is for. A form correcting a saved record passes true: folding away a note
 * somebody typed last week would be the app hiding their own words behind a
 * control that says "Add more", which is the opposite of what it means.
 */
@Composable
fun Disclosure(
    modifier: Modifier = Modifier,
    /** Overridable so a screen can say what it is offering in its own words. */
    labelKey: String = "capture.more",
    asideKey: String? = "capture.more.aside",
    testTag: String? = null,
    /** True when what is inside is already filled in and must not be hidden. */
    startOpen: Boolean = false,
    content: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val motion = LocalMotion.current
    // Saveable, so a rotation or a theme change does not fold the form back up
    // under somebody part way through filling it in.
    var open by rememberSaveable(startOpen) { mutableStateOf(startOpen) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!open) {
            // **A container, not a link.** The approved mockup draws this as a
            // full width surface carrying a plus, the words, and a chevron,
            // `m3v4-4`, and it was a small outlined pill with a sentence
            // underneath it. On a form made of full width fields, a pill is
            // the one thing that does not line up with anything, and the
            // aside under it read as a footnote about a control rather than
            // as part of it.
            //
            // **The chevron points down, which is the app's own fold
            // vocabulary.** `FoldRow` rotates the same mark ninety degrees to
            // say a thing opens downward, so this reads as the gesture it
            // already is rather than as a new one. The mockup also carries a
            // plus; there is no plus glyph in the library and the chevron is
            // not ambiguous without it, so none was invented for this.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.cardLarge)
                    .openableByTap(
                        label = strings[labelKey],
                        onTap = { open = true },
                        resting = colors.sand,
                        shape = Radius.cardLarge,
                    )
                    .defaultMinSize(minHeight = Space.touchTarget)
                    .padding(Space.cardPadding)
                    .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings[labelKey],
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                    // **Inside the container now**, because it is what the
                    // control promises rather than a remark about it. Rule 13:
                    // it says nothing here is needed, which is the whole point
                    // of putting it behind a control nobody has to touch.
                    if (asideKey != null) {
                        Text(
                            text = strings[asideKey],
                            style = HealthTrail.type.bodyS,
                            color = colors.ink2,
                        )
                    }
                }
                Chevron(modifier = Modifier.rotate(DOWN))
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

/** A chevron turned to point down, the way `FoldRow` says a thing opens. */
private const val DOWN = 90f
