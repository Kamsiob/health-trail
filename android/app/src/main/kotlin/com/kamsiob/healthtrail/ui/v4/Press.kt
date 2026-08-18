package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * A surface the person can open, on Material's own state layer. #392, D196.
 *
 * **Use [androidx.compose.material3.Surface] wherever the thing being made
 * tappable is a container**, which is most places: it owns the shape, the
 * color, the state layer and the button role together, and nothing here is
 * needed. This exists for the sites where the tap belongs to a modifier chain
 * the caller already owns, a row inside a lazy item or a cell that is a
 * `Column` in one branch and a `Row` in the other, and wrapping those in a
 * surface would mean restructuring the screen around the gesture.
 *
 * **What it replaces is the point.** `ui/components/Press.kt` animated the
 * resting color toward `ink` by hand, animated a focus border's alpha
 * separately, ran a spring on the scale, and passed `indication = null` so none
 * of Material's own feedback could reach the control. Four moving parts, in one
 * file, that thirty screens depended on and that no Material component knew
 * about: a chip and a card sitting next to each other pressed differently
 * because one was Material's and one was ours.
 *
 * **[ripple] is Material's state layer**, so press, focus and hover all answer
 * with the treatment the rest of the app already uses, and the reduced motion
 * setting reaches it through Material rather than through a token this file
 * would have to remember to read.
 *
 * @param label what a reader says the tap does, in the person's language and as
 *   a verb: "Open this entry", never the name of the thing being opened.
 */
@Composable
fun Modifier.opensOnTap(
    label: String,
    onTap: () -> Unit,
    /**
     * The corner the state layer is clipped to.
     *
     * **A ripple drawn square inside a shape rounded at 26dp shows at the
     * corners**, which is the same defect the old focus ring had when its
     * radius and its caller's clip disagreed.
     */
    shape: Shape,
    /**
     * The resting color, where the caller is not already painting one.
     *
     * **Transparent is the ordinary answer**, and it is the default because
     * this is a gesture rather than a container: a caller that wants a filled
     * card wants `Surface`. The old modifier defaulted to a filled surface,
     * which is how tappable cards stayed white while everything around them
     * went tonal. `docs/V4.md` 2.1.
     */
    container: Color = Color.Transparent,
    /**
     * What touching and holding does, or null where holding does nothing.
     *
     * **A shortcut and never the only way in.** Every caller that sets this has
     * a visible control doing the same thing, because a gesture nobody is told
     * about is a feature that does not exist for most people, and does not
     * exist at all for somebody using a screen reader or switch access.
     * [longPressLabel] is what a reader offers instead, so the hold is a real
     * action in the accessibility tree rather than a rumor.
     *
     * long-press-twin: whatever visible control the caller already has. The one
     * caller today is Today's card, whose twin is the Arrange action in the
     * screen header. D155, and the check that reads this marker is
     * `tools/checks/check_dead_gestures.py`.
     */
    onLongPress: (() -> Unit)? = null,
    /** What a reader calls the hold, as a verb. Required whenever [onLongPress] is set. */
    longPressLabel: String? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val indication = ripple()

    return this
        .clip(shape)
        .background(container, shape)
        // long-press-twin: the caller's own visible control, named on the
        // onLongPress parameter above. D155.
        .then(
            if (onLongPress == null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = indication,
                    onClickLabel = label,
                    role = Role.Button,
                    onClick = onTap,
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = indication,
                    onClickLabel = label,
                    role = Role.Button,
                    onLongClickLabel = longPressLabel,
                    // **The phone's own answer to a hold.** Compose does not
                    // buzz for a long press by itself, and without it the
                    // gesture succeeds in silence: the screen changes a moment
                    // later and nothing connects the two. On the home screen
                    // every person already has, the buzz is the confirmation.
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                    onClick = onTap,
                )
            },
        )
}
