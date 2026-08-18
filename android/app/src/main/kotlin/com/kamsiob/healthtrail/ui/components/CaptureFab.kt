package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius

/**
 * The gold capture button, in the trailing corner. `DESIGN.md` sections 7 and 8.
 *
 * **Material's own `FloatingActionButton`, D196.** This was a `Box` with a clip,
 * a background, a hand rolled press and a plus built out of two more boxes: the
 * shape of a floating action button rather than one. Material owns the size, the
 * shadow, the state layer, the ripple and the semantics of a button now, and
 * what stays hand written is the part that is this app's and not Material's.
 *
 * **What stays, and why each one is ours rather than the scheme's:**
 *
 * - **Gold, fixed in both themes.** It is the single way data enters the app, it
 *   sits on every screen, it never moves and never hides on scroll, so it has to
 *   be findable without thought by somebody tired, in bad light, and often
 *   older. `primary` is the app's blue and every other action wears it; this one
 *   is deliberately not one of those.
 * - **A dark glyph, and that is measured rather than chosen.** White on gold is
 *   2.38:1, well under the 3:1 a control needs. The dark glyph measures 6.88:1.
 * - **Open, it turns 45 degrees into a close.** The same control in two
 *   positions rather than a swap to a different mark, so it is visibly the thing
 *   that opened the menu and visibly the thing that will close it.
 * - **The corner morphs as it opens and as it is pressed.** D167: a control
 *   whose silhouette answers the state it is in is Material 3 Expressive's
 *   signature move, and it is animated rather than swapped so it reads as one
 *   object moving. Reduced motion snaps all of it, because the specs come from
 *   `LocalMotion`.
 *
 * **The mark is Google's `add` rather than two drawn bars.** The app authors no
 * glyphs, and a symbol from the catalog cannot fall back to a box in any
 * language any more than a drawing can: these are vector drawables.
 *
 * **Nothing tappable ever sits underneath it**, section 8 and D81, enforced by
 * the callers using [fabSafeActionBar] and [fabScrollClearance].
 *
 * The caller places it, so a screen without a bottom bar can sit it lower. In
 * Arabic it belongs in the start corner, and the caller's alignment does that.
 */
@Composable
fun CaptureFab(
    open: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val motion = HealthTrail.motion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val turn by animateFloatAsState(
        targetValue = if (open) 45f else 0f,
        animationSpec = motion.springy(),
        label = "capture",
    )
    val corner by animateDpAsState(
        targetValue = when {
            open -> Radius.fabCornerOpen
            pressed -> Radius.fabCornerPressed
            else -> Radius.fabCorner
        },
        animationSpec = motion.springy(),
        label = "captureShape",
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = description }
            .testTag(NavTags.CAPTURE),
        shape = RoundedCornerShape(corner),
        containerColor = colors.gold,
        contentColor = colors.onGold,
        // **Material's own resting elevation.** The app draws no other shadow,
        // and this is the one control that is genuinely floating over the
        // content rather than sitting in it.
        elevation = FloatingActionButtonDefaults.elevation(),
        interactionSource = interaction,
    ) {
        Icon(
            painter = painterResource(Symbols.add),
            contentDescription = null,
            modifier = Modifier.rotate(turn),
        )
    }
}
