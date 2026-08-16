package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The gold capture button, in the trailing corner. `DESIGN.md` section 7 and
 * section 8.
 *
 * **It moved out of the bottom navigation in v4.** It used to sit in a reserved
 * column in the middle of the bar, which cost the bar a quarter of its width and
 * put a notch between two tabs. In the corner it is closer to where a thumb
 * already rests on a large phone, and the bar gets to be four equal tabs.
 *
 * **It is the one element whose color does not shift between themes.** It is the
 * single way data enters the app, it sits on every screen, it never moves, never
 * hides on scroll, and never changes color, because it has to be findable
 * without thought by somebody tired, in bad light, and often older.
 *
 * **Its glyph is dark rather than white**, and that is measured rather than
 * chosen: white on gold is 2.38:1, well under the 3:1 a control needs, and this
 * is the last control in the app that can afford to be missed. The dark glyph
 * measures 6.88:1.
 *
 * **The plus is drawn from two bars rather than a glyph**, so it needs no icon
 * font and cannot fall back to a box in any language.
 *
 * **A press is not a change of color, it is the control saying it heard you.**
 * The rule that it never changes color is about its resting state.
 *
 * **Open, it turns 45 degrees into a close.** The same control in two positions,
 * rather than swapping to a different glyph, so it is visibly the thing that
 * opened the menu and visibly the thing that will close it.
 *
 * **Nothing tappable ever sits underneath it**, which is section 8 and D81, and
 * which is enforced by the callers using [fabSafeActionBar] and
 * [fabScrollClearance] rather than by anybody remembering.
 *
 * The caller places it, so that a screen without a bottom bar can sit it lower.
 * In Arabic it belongs in the start corner, and the caller's alignment does
 * that on its own.
 */
@Composable
fun CaptureFab(
    open: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.gold)
    val motion = HealthTrail.motion
    val turn by animateFloatAsState(
        targetValue = if (open) 45f else 0f,
        animationSpec = motion.springy(),
        label = "capture",
    )

    // **The button changes shape as it opens, and it is the app's one piece of
    // true shape morphing.** D167. Material 3 Expressive's signature move is a
    // control whose silhouette answers the state it is in: a squircle at rest,
    // rounder as it becomes the thing you are canceling. The corner is
    // animated rather than swapped, so it reads as the same object moving.
    //
    // **A press squashes it a little**, which is the physics every other
    // surface in the app now has. Reduced motion snaps all of it.
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = when {
            open -> Space.fabSize / 2
            pressed -> Radius.fabCornerPressed
            else -> Radius.fabCorner
        },
        animationSpec = motion.springy(),
        label = "captureShape",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = motion.springy(),
        label = "captureScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(Space.fabSize)
            .clip(RoundedCornerShape(corner))
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .testTag(NavTags.CAPTURE),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.rotate(turn), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 2.5.dp)
                    .clip(Radius.pill)
                    .background(colors.onGold),
            )
            Box(
                modifier = Modifier
                    .size(width = 2.5.dp, height = 20.dp)
                    .clip(Radius.pill)
                    .background(colors.onGold),
            )
        }
    }
}

/**
 * A rounded square rather than a circle, at 17dp on 48dp.
 *
 * The grid draws it this way, and it is the same radius as a card, which is what
 * keeps it reading as part of this app's own shape vocabulary rather than as a
 * platform control dropped on top of it. A circle here would be the one fully
 * round object on a screen made of soft rectangles.
 */
private val FabShape = Radius.fab
