package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.theme.HealthTrail

/**
 * Two or three named views of one list, one of them showing. `m3v4-3`, #386.
 *
 * **This is what replaces the folds on the care team.** The drawing has no
 * accordion on it: it puts "At Maplewood" beside "Her own doctors" at the top
 * and shows one of them. A toggle says the other group exists and is one tap
 * away, which is the whole thing a fold's count was for, and it does it without
 * the person opening anything. D185.
 *
 * **Material's own segmented button**, themed. It draws the outline, the check
 * on the chosen one, and the shapes at the ends of the row, and it carries the
 * radio semantics a reader needs: a segment announces itself as selected or not
 * rather than as a button that may or may not have done something.
 *
 * **Gold marks the chosen segment**, which is the accent this app spends on a
 * chip. `docs/V4.md` 2.1: gold is a chip, an icon, the capture button, never a
 * field.
 *
 * **Two or three, never more.** Past that it is a list of filters, and a list
 * of filters is a screen the person has to learn rather than read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Segments(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** A tag per segment, for a caller whose test presses one of them. */
    tagFor: ((Int) -> String)? = null,
) {
    if (options.size < 2) return
    val colors = HealthTrail.colors
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.goldWash,
                    activeContentColor = colors.goldInk,
                    activeBorderColor = colors.hairlineHeavy,
                    inactiveContainerColor = colors.card,
                    inactiveContentColor = colors.ink,
                    inactiveBorderColor = colors.hairlineHeavy,
                ),
                modifier = tagFor?.let { Modifier.testTag(it(index)) } ?: Modifier,
            ) {
                Text(
                    // A segment is usually a place somebody named, so it is
                    // isolated like every other one of their words.
                    text = Bidi.isolate(label),
                    style = HealthTrail.type.label,
                )
            }
        }
    }
}
