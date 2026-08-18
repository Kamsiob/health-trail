package com.kamsiob.healthtrail.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.IconTile
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The six things a person can write down, blooming from the button itself.
 *
 * **This is what grid screen 04 draws** and what the app did not have: "a
 * labeled menu blooms from the button itself, in reach of the same thumb. Six
 * choices, icons and words, over a dimmed screen. **Not a wall, a fan.**" The
 * app answered with a modal sheet of tiles, which is a wall: it comes from the
 * bottom edge rather than from the control that was pressed, and it puts the
 * choices where the thumb is not.
 *
 * **Each choice is words then its own mark**, right where the button is, so the
 * hand that opened the menu does not travel. The order is the order of the
 * enum, which is the order the grid draws and the order the person learns.
 *
 * **They arrive in sequence, quickly.** The stagger is what makes it read as
 * one thing unfolding rather than six things appearing, and it is short enough
 * that somebody in a corridor never waits for it: the last choice is in place
 * before a thumb can reach it. **With reduced motion on, the tokens flatten the
 * spring and the whole thing simply is there**, which is the correct behavior
 * rather than a special case.
 *
 * **The scrim is the same one the sheet used**, and tapping it closes the menu
 * without choosing, which is the only thing a scrim should ever do.
 */
@Composable
fun CaptureBloom(
    onChoose: (CaptureKind) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val motion = LocalMotion.current

    // **Each choice is released in turn**, nearest the thumb first, which is
    // what makes it read as one thing unfolding rather than six appearing. The
    // delay is here rather than inside the animation so that a person with
    // reduced motion on gets them all at once, which is the correct behavior
    // rather than a special case.
    // **The keyboard goes down when the menu comes up.** The modal sheet this
    // replaced was its own window and took the keyboard with it; a menu drawn
    // in the same tree does not, so a form left with the keyboard up handed the
    // next back press to the keyboard instead of to the menu. A walk cannot see
    // that and a test reads it as a screen that never arrived.
    val keyboard = LocalSoftwareKeyboardController.current

    var released by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        keyboard?.hide()
        repeat(CaptureKind.entries.size) {
            released += 1
            kotlinx.coroutines.delay(BLOOM_STEP_MILLIS.toLong())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = BLOOM_SCRIM_ALPHA))
            // The scrim closes and chooses nothing. It carries no label of its
            // own, because a reader announcing "dimmed background, button" over
            // six real choices is noise.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .testTag(CaptureTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                // **One width for every choice**, so the six read as one menu
                // rather than six ragged pills. `IntrinsicSize.Max` asks the
                // widest label and gives that width to all of them, which is
                // what makes the left edge a line and the marks a column.
                .width(IntrinsicSize.Max)
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                // **Clear of the button itself**, which stays where it was and
                // turns rather than being covered by its own menu. The button
                // sits `sm` above the bar and is `fabSize` tall, and the gap
                // above it is what stops the nearest choice from sitting on
                // the thing that opened it: on the phone, at `fabSize + m`,
                // the last two choices were over the button.
                .padding(end = Space.sm, bottom = Space.fabSize + Space.sm + Space.xl + Space.m),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            CaptureKind.entries.forEachIndexed { index, kind ->
                BloomChoice(
                    label = strings[bloomLabelKey(kind)],
                    kind = kind,
                    shown = released > CaptureKind.entries.size - 1 - index,
                    onClick = { onChoose(kind) },
                )
            }
        }
    }
}

@Composable
private fun BloomChoice(
    label: String,
    kind: CaptureKind,
    shown: Boolean,
    onClick: () -> Unit,
) {
    val colors = HealthTrail.colors
    val motion = LocalMotion.current

    // **Movement only, never opacity.** A pill fading in is present in the
    // tree and not yet visible, which is a target somebody can tap and not see
    // and a node a test can find and not click: the first version staggered
    // alpha and the top choice was still invisible when a walk reached for it.
    // Sliding a short distance reads as the same bloom and every choice is
    // touchable from the first frame.
    val rise by animateDpAsState(
        targetValue = if (shown) Space.none else Space.m,
        // **The expressive spring, and this is one of the only three places
        // allowed to overshoot.** `DESIGN.md` 10 names them: the capture menu
        // blooming, a milestone landing on the arc, and an incident marked
        // resolved, "because each is a small piece of relief in an app used
        // during hard times". **It had zero call sites**, so the one moment the
        // design reserved a spring for was rising on the same curve as
        // everything else. #371 item 5.
        //
        // Still the app's own token, so reduced motion flattens it to a snap
        // with everything else rather than needing a special case here.
        animationSpec = motion.expressive(),
        label = "bloom",
    )

    // **Material owns the pill and the press.** #392. The bloom's own
    // rise stays: it is the one motion `DESIGN.md` 10 lets overshoot, and it is
    // placement rather than a state layer.
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            // The lambda overload, so the slide happens in layout rather than
            // recomposing the whole pill on every frame. Lint asks for this and
            // it is right: six pills recomposing together is six times the work
            // for a movement that could be a placement.
            .offset { IntOffset(0, rise.roundToPx()) }
            .semantics(mergeDescendants = true) {
                contentDescription = label
                onClick { onClick(); true }
            }
            .testTag(CaptureTags.option(kind)),
        shape = BloomShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
    Row(
        modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Space.s))
        // The section's own mark, in its own hue, so a choice looks like the
        // place it writes to rather than like a generic menu item.
        IconTile(
            kind = kind,
            tint = colors.ink,
            background = colors.sand,
            tileSize = BLOOM_MARK,
            iconSize = BLOOM_DRAWING,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
    }
}

private fun bloomLabelKey(kind: CaptureKind): String = when (kind) {
    CaptureKind.CALL -> "capture.call"
    CaptureKind.VISIT -> "capture.visit"
    CaptureKind.INCIDENT -> "capture.incident"
    CaptureKind.MEASUREMENT -> "capture.measurement"
    CaptureKind.QUESTION -> "capture.question"
    CaptureKind.DOCUMENT -> "capture.document"
}

/** Fully rounded, because a pill beside a round button is the same family. */
private val BloomShape = RoundedCornerShape(percent = 50)

/** The same dimming the sheet used, so the app has one scrim rather than two. */
private const val BLOOM_SCRIM_ALPHA = 0.62f

/**
 * How far apart the choices arrive.
 *
 * Short enough that the last one is in place before a thumb reaches it, long
 * enough that the eye reads one thing unfolding rather than six appearing.
 */
private const val BLOOM_STEP_MILLIS = 26

private val BLOOM_MARK = Space.bloomMark
private val BLOOM_DRAWING = Space.bloomDrawing
