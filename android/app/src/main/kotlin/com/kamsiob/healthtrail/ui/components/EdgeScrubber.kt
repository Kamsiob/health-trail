package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The index scrubber on the trailing edge, for jumping by year and month.
 * `DESIGN.md` section 7, and the second of law 4's four tools.
 *
 * **A five year notebook is 1,630 entries.** Scrolling to a year is not a
 * gesture, it is a commitment, and the person doing it is usually looking for
 * one thing they half remember. The scrubber turns that into a drag.
 *
 * **It moves to the start edge in Arabic**, along with the FAB and the trail
 * itself, because it is an index down the outer margin of a page and the outer
 * margin changes sides. The caller aligns it, so nothing here has to know.
 *
 * **A drag is not the only way to use it.** Every label is also a tap target in
 * its own right, so a person who cannot hold a precise drag, or who is using a
 * screen reader, still reaches every year. **`DESIGN.md` section 12: anything
 * gesture-only also has a visible, non-gesture path**, and here the gesture and
 * the path are the same control rather than two.
 *
 * **The current label is the only one that is emphasized**, and it is emphasized
 * with a filled pill rather than with color alone, so it survives grayscale and
 * any color vision difference.
 *
 * **A haptic tick on each new label**, which is what makes a drag feel like it
 * is catching on something rather than sliding over glass. It fires on change,
 * never per frame.
 *
 * **When not to use it.** On a list that fits in a screenful or two. It is one
 * of the four tools a list earns the moment it can grow, not furniture every
 * list gets.
 */
@Composable
fun EdgeScrubber(
    labels: List<String>,
    currentIndex: Int,
    onScrub: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    val colors = HealthTrail.colors
    val type = HealthTrail.type
    // Read outside the draw scope, which is not a composable.
    val line = colors.hairline
    val haptics = LocalHapticFeedback.current
    var height by remember { mutableIntStateOf(0) }
    var lastReported by remember { mutableIntStateOf(currentIndex) }

    fun indexAt(y: Float): Int {
        if (height <= 0) return currentIndex
        val fraction = (y / height).coerceIn(0f, 1f)
        return (fraction * labels.size).toInt().coerceIn(0, labels.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth())
            // **The line is what makes it an index rather than stray digits.**
            // #361: on a notebook a few months old the rail carries two labels,
            // and `SpaceBetween` puts one near the top and one in the middle of
            // an otherwise empty margin, where they read as numbers that have
            // come loose from something. The hairline they sit on says they
            // are one control and says how far it runs, which is the half a
            // person needs before they will touch it.
            //
            // **Drawn behind, at the width of a hairline**, so it is the same
            // mark the rest of the app uses to say "these belong together" and
            // costs the content nothing.
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
                    onDragStart = { offset ->
                        val i = indexAt(offset.y)
                        if (i != lastReported) {
                            lastReported = i
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onScrub(i)
                        }
                    },
                ) { change, _ ->
                    val i = indexAt(change.position.y)
                    // **On change, never per frame.** A tick every frame is a
                    // buzz, which reads as the phone objecting rather than as
                    // the list catching on a year.
                    if (i != lastReported) {
                        lastReported = i
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onScrub(i)
                    }
                }
            }
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        labels.forEachIndexed { index, label ->
            val current = index == currentIndex
            Box(
                modifier = Modifier
                    .clip(Radius.tile)
                    // **Opaque, so the rail's line runs between the labels
                    // rather than through them.** `paper` because the scrubber
                    // rides a reserved margin and never sits over content.
                    .background(if (current) colors.blueWash else colors.paper)
                    // **Each label is its own tap target**, which is what makes
                    // the non-gesture path real rather than claimed. A person
                    // who cannot hold a precise drag, and anybody using a screen
                    // reader, reaches every year by tapping.
                    .clickable(
                        role = Role.Button,
                        onClickLabel = label,
                        onClick = {
                            lastReported = index
                            onScrub(index)
                        },
                    )
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = type.mono,
                    // **Never wrapped.** At font scale 2.0 a fixed 22dp strip
                    // broke "26" into a 2 above a 6, and the whole index became
                    // a column of single digits that read as nothing at all.
                    // Found on the phone at maximum font scale, which is the
                    // only place it is visible.
                    maxLines = 1,
                    softWrap = false,
                    // **`ink-2`, not `ink-3`.** D92: `ink-3` is non-text only,
                    // at 2.37:1 on paper, and these are words.
                    color = if (current) colors.blueDeep else colors.ink2,
                )
            }
        }
    }
}

/**
 * Narrow on purpose. It rides the margin beside the content rather than taking
 * a column from it, and the drag target is the whole height of the strip rather
 * than the width of the digits.
 */
private val ScrubberWidth = 22.dp

/**
 * How wide the rail is, and therefore how much margin a screen has to reserve
 * for it, at the font scale the person is actually using.
 *
 * **One definition, used by both the strip and the margin.** They were two
 * numbers for an hour: a 22dp strip inside a 30dp margin, both fixed, both
 * correct at font scale 1.0 and both wrong at 2.0, where two mono digits do not
 * fit in 22dp and the labels wrapped into single digits down the edge.
 *
 * Clamped at the top, because past 2.0 an index wide enough for its own text
 * would be taking a column from the content rather than riding beside it, and
 * at that point the folds are the way back through the record.
 */
@Composable
fun railWidth(): Dp = ScrubberWidth * LocalDensity.current.fontScale.coerceIn(1f, 2f)
