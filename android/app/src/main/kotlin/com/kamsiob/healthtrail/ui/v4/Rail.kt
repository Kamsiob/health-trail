package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The index down the trailing margin, for jumping a long trail by year. #387.
 *
 * **Written fresh on Material's own components and the old file deleted**, D196.
 * What it replaces was a `Box` with a `clip`, a `background`, an `indication =
 * null` `clickable` and two hand written press animations, which is what a
 * `Surface` already is. Material owns the shape, the tonal color, the state
 * layer and the button role now, and this file owns only the part Material has
 * no opinion about: what the rail is for and how a drag maps onto it.
 *
 * **There is no Material component for an index, and that is not a license to
 * rebuild one.** The four exceptions in `gh issue view 321` are drawings; this
 * is an arrangement. Every piece of it is Material's and only the arrangement
 * is ours.
 *
 * **A five year notebook is 1,630 entries.** Scrolling to a year is not a
 * gesture, it is a commitment, and the person doing it is usually looking for
 * one thing they half remember. The rail turns that into a drag.
 *
 * **A drag is not the only way in.** Every label is its own `Surface(onClick)`,
 * so somebody who cannot hold a precise drag, and anybody using the reader,
 * reaches every year by tapping. `DESIGN.md` 12: anything gesture-only also has
 * a visible non-gesture path, and here the gesture and the path are one control
 * rather than two.
 *
 * **The current label is filled, never colored alone.** It survives grayscale
 * and any color vision difference, which a tint by itself does not.
 *
 * **A haptic tick on each new label**, which is what makes a drag feel like it
 * is catching on something rather than sliding over glass. On change, never per
 * frame: a tick every frame is a buzz, and a buzz reads as the phone objecting.
 */
@Composable
fun EdgeRail(
    labels: List<String>,
    currentIndex: Int,
    onScrub: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    val scheme = MaterialTheme.colorScheme
    // Read outside the draw scope, which is not a composable.
    val line = scheme.outlineVariant
    val haptics = LocalHapticFeedback.current
    var height by remember { mutableIntStateOf(0) }
    var lastReported by remember { mutableIntStateOf(currentIndex) }

    fun indexAt(y: Float): Int {
        if (height <= 0) return currentIndex
        val fraction = (y / height).coerceIn(0f, 1f)
        return (fraction * labels.size).toInt().coerceIn(0, labels.size - 1)
    }

    fun report(index: Int) {
        if (index == lastReported) return
        lastReported = index
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onScrub(index)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth())
            // **The line is what makes it an index rather than stray digits.**
            // #361: on a notebook a few months old the rail carries two labels,
            // and `SpaceBetween` puts one near the top and one in the middle of
            // an otherwise empty margin, where they read as numbers that have
            // come loose from something. The hairline they sit on says they are
            // one control and says how far it runs, which is the half a person
            // needs before they will touch it.
            .drawBehind {
                val x = size.width / 2
                drawLine(
                    color = line,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = Space.hairlineWidth.toPx(),
                )
            }
            .onSizeChanged { height = it.height }
            .pointerInput(labels.size) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> report(indexAt(offset.y)) },
                ) { change, _ -> report(indexAt(change.position.y)) }
            }
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        labels.forEachIndexed { index, label ->
            val current = index == currentIndex
            // **Material's own surface, which is the whole point of the
            // rewrite.** It carries the shape, the container color, the content
            // color that goes with it, the state layer on press and hover, and
            // the button role a reader announces. All five were hand painted
            // here, and the press animation was two `animateAsState` calls that
            // did what Material's indication does.
            Surface(
                onClick = {
                    lastReported = index
                    onScrub(index)
                },
                shape = MaterialTheme.shapes.extraSmall,
                color = if (current) scheme.secondaryContainer else scheme.surface,
                contentColor = if (current) {
                    scheme.onSecondaryContainer
                } else {
                    scheme.onSurfaceVariant
                },
                // The words are the label, so the node that takes the tap says
                // them and a reader hears the year rather than "button".
                modifier = Modifier.semantics { contentDescription = label },
            ) {
                Text(
                    text = label,
                    style = railLabelStyle(),
                    // **Never wrapped, and now never clipped either.** The rail
                    // was a fixed 22dp and the label is set with tracking, so
                    // "26" overflowed its own strip and the screen edge cut the
                    // 6 off. Seen on the phone, rule 21. The width is measured
                    // from the type now rather than guessed, so it is right at
                    // every font scale instead of at one of them.
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(
                        horizontal = RailLabelPadding,
                        vertical = Space.xs,
                    ),
                )
            }
        }
    }
}

/**
 * How a year is set on the rail.
 *
 * **Material's own ladder with tabular figures**, rather than the app's mono
 * style. D173 keeps mono for figures that line up in a column, and that is
 * exactly what this is, but the reason mono was reached for is a metric rather
 * than a face: every digit on one width. `tnum` asks the reading face for the
 * same thing and costs the rail nothing, so the rail stays on
 * `MaterialTheme.typography` and the second ladder does not follow it here.
 */
@Composable
private fun railLabelStyle(): TextStyle =
    MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")

/** The air on either side of two digits, inside the pill. */
private val RailLabelPadding: Dp = 6.dp

/**
 * How wide the rail is, and therefore how much margin a page reserves for it.
 *
 * **Measured off the type rather than declared.** It was 22dp scaled by the
 * font scale, and 22dp is not two digits of `labelMedium` with tracking at any
 * font scale: the labels overflowed the strip and the screen edge cut them.
 * Measuring the widest label the rail can hold, at the size it will actually be
 * set, is the only version of this number that is right without somebody
 * checking it again after the next type change.
 *
 * **Two digits is the widest label**, because a year on the rail is its last
 * two: `WIDEST` is the pair of digits that sets widest in the reading face.
 * Clamped at the bottom to Material's own minimum touch target, because a
 * target narrower than that is a control a person misses.
 */
@Composable
fun railWidth(): Dp {
    val measurer = rememberTextMeasurer()
    val style = railLabelStyle()
    val density = LocalDensity.current
    val text = measurer.measure(text = WIDEST, style = style, softWrap = false)
    val measured = with(density) { text.size.width.toDp() } + RailLabelPadding * 2
    // **Material's own minimum, and it is not optional here.** `Surface` with an
    // `onClick` applies `minimumInteractiveComponentSize` to itself, so a strip
    // narrower than the target does not make the pill smaller: it makes the
    // pill overflow the strip, which is the defect this rewrite is fixing.
    // The page has to reserve what the control will actually take.
    return maxOf(measured, Space.touchTarget)
}

/**
 * The pair of digits that sets widest, so the measurement is the worst case
 * rather than whichever year happens to be on the rail today.
 */
private const val WIDEST = "00"
