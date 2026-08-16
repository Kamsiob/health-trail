package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space

object HeaderActionTags {
    const val EDIT = "header_edit"
    const val ROW = "header_actions"
}

/**
 * The controls in the top corner of a screen, in one fixed order. D173.
 *
 * **The owner's rule, and it is about learnability rather than looks:** "if
 * there's anything on one page then the user should know where to find it on a
 * different page." A control that moves between screens has to be found again
 * every time, and finding it again is the cost paid by somebody who is tired
 * and holding a phone in a waiting room.
 *
 * **So the corner has one grammar everywhere.** Reading inward from the edge:
 * the edit mark sits furthest right, because it is the one the thumb reaches
 * for most and the corner is the easiest target on the screen; the lamp sits
 * beside it. A screen that has only one of them still puts that one in its own
 * place rather than sliding it over to the corner, so the lamp is always the
 * lamp's position and never sometimes the edit position.
 *
 * **Both are circular and the same size**, which is what makes them read as one
 * family of controls rather than two unrelated buttons that happen to be near
 * each other.
 */
@Composable
fun HeaderActions(
    modifier: Modifier = Modifier,
    /** Opens what this page is for. Null on a screen with nothing to explain. */
    onTips: (() -> Unit)? = null,
    /** Goes back and changes what is already written here. Null where nothing is editable. */
    onEdit: (() -> Unit)? = null,
    /** What a reader calls the edit action, in this screen's own words. */
    editLabel: String? = null,
    /**
     * The screen's own tag for its edit control.
     *
     * **The tag follows the control rather than the position.** Five screens
     * had their own tag on a button in the body; moving the button to the
     * corner without bringing the tag would have left five instrumented tests
     * looking for a node that no longer exists, and a green suite that had
     * stopped checking the thing it names.
     */
    editTag: String? = null,
) {
    if (onTips == null && onEdit == null) return
    Row(
        modifier = modifier.testTag(HeaderActionTags.ROW),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // **Both slots are held open whichever control is missing**, which is
        // the whole point. Seen on the phone: Today had a lamp and a pencil, so
        // the lamp sat one place in from the corner; More had a lamp alone, so
        // the lamp sat in the corner. Same control, two positions, and the
        // person has to look for it again on every screen. Reserving the empty
        // slot costs one invisible box and makes the corner learnable once.
        if (onTips != null) TipsButton(onOpen = onTips) else HeldSlot()
        if (onEdit != null) {
            EditAction(onClick = onEdit, label = editLabel, tag = editTag)
        } else {
            HeldSlot()
        }
    }
}

/** One action's width, held open so the other never slides into its place. */
@Composable
private fun HeldSlot() {
    Box(Modifier.size(Space.touchTarget))
}

/**
 * Change what is already written on this screen.
 *
 * **A mark rather than the word.** The owner, looking at Today: the corner
 * should carry an edit icon instead of the word "Arrange". A verb in the corner
 * is a different word on every screen, which means the corner has to be read
 * before it can be used; a pencil is the same mark everywhere and is read once.
 *
 * **Quieter than the lamp on purpose.** Gold is the app's accent and it is
 * spent on the capture button and on the lamp. Two gold circles side by side
 * would be two accents in one corner and neither would lead. The pencil takes
 * the neutral ground, which is the right weight for a control that is used
 * often and is never the reason somebody opened the screen.
 *
 * **The spoken label is the screen's own.** A reader should hear "arrange the
 * cards" or "edit these notes" rather than "edit", because the mark is general
 * and what it edits is not.
 */
@Composable
fun EditAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    /**
     * The screen's own tag, replacing the shared one.
     *
     * **A parameter rather than a `testTag` in the caller's modifier.** Two
     * `testTag` calls in one chain do not compose: the later one silently wins,
     * so the component's own tag would have overridden every screen's and the
     * five tests moved here would have gone looking for nodes that never
     * appeared. One tag, chosen once.
     */
    tag: String? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.springy(),
        label = "editPress",
    )
    val spoken = label ?: strings["action.edit"]

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(Space.touchTarget)
            .clip(CircleShape)
            .background(colors.sand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = spoken,
                onClick = onClick,
            )
            .semantics { contentDescription = spoken }
            .testTag(tag ?: HeaderActionTags.EDIT),
        contentAlignment = Alignment.Center,
    ) {
        PencilMark(tint = colors.ink)
    }
}

/**
 * A drawn pencil, because the app has no icon set and never wanted one.
 *
 * Three strokes: the barrel on the diagonal, the ferrule across it, and the
 * point. Drawn in fractions of the given size so it stays itself when the
 * person's font scale grows the touch target around it.
 */
@Composable
private fun PencilMark(tint: Color) {
    Canvas(modifier = Modifier.size(Space.editMark)) {
        val s = size.minDimension
        val barrel = s * 0.155f
        // Barrel, from the point at lower left to the eraser at upper right.
        drawLine(
            color = tint,
            start = Offset(s * 0.30f, s * 0.70f),
            end = Offset(s * 0.74f, s * 0.26f),
            strokeWidth = barrel,
            cap = StrokeCap.Round,
        )
        // Ferrule: the band across the barrel that makes it a pencil rather
        // than a stick. Without it the mark reads as a slash at small sizes.
        drawLine(
            color = tint,
            start = Offset(s * 0.55f, s * 0.33f),
            end = Offset(s * 0.67f, s * 0.45f),
            strokeWidth = s * 0.07f,
            cap = StrokeCap.Round,
        )
        // The point, and the short line it has just drawn.
        drawLine(
            color = tint,
            start = Offset(s * 0.16f, s * 0.84f),
            end = Offset(s * 0.30f, s * 0.78f),
            strokeWidth = s * 0.075f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = tint,
            radius = s * 0.038f,
            center = Offset(s * 0.185f, s * 0.815f),
            style = Stroke(width = s * 0.05f),
        )
    }
}
