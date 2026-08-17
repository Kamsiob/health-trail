package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Eyebrow

/**
 * A group of fields under the quiet label that names it. #386.
 *
 * **The label was a gray sentence and it got lost.** The owner, on the setup
 * screen, 2026-08-17: "the text just kind of gets lost and it seems like
 * clutter." It was body text in the secondary ink, indented, sitting directly
 * above a block of fields whose own floating labels are body text in the
 * secondary ink: three sizes of the same thing stacked, so nothing said which
 * was the heading.
 *
 * **So it is the eyebrow**, which is what `docs/V4.md` 2.1 gives a label that
 * names what follows: small, tracked, capitals where the words are the app's
 * own and short. Different in size, weight, spacing and case from everything
 * inside the block, which is what makes it read as a heading rather than as
 * another line of the form. Rule 15: hierarchy before decoration.
 *
 * **The fields sit on the canvas, not in a block**, which is what `m3v4-4`
 * draws: a field is already a container, with its own notched outline and its
 * own floating label, so putting it inside a second container is two edges
 * around one thing. That is the clutter the owner named. The eyebrow and the
 * air between groups are what say where one group ends. D183.
 */
@Composable
fun FieldBlock(
    label: String,
    modifier: Modifier = Modifier,
    /** The ink the label takes, where a form belongs to a section. */
    labelColor: Color = HealthTrail.colors.ink2,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        Eyebrow(text = label, color = labelColor)
        content()
    }
}
