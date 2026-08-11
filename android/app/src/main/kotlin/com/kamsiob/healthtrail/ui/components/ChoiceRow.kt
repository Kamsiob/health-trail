package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * One answer to a question with a small fixed set of them, each needing a line
 * of explanation. `DESIGN.md` section 11.
 *
 * **A radio group, so it is one grouped surface rather than several cards.**
 * Separate cards for one question is a second pattern for a job an existing
 * shape already does, and a card is for something with three or more lines the
 * person actually reads. **It is not chips either**, which belong to short
 * answers: an option that needs a sentence under it is a row.
 *
 * **Selection is state rather than decoration.** The dot says "this one" the
 * same way the bottom navigation does, and the `selected` semantic says it to a
 * reader, because without it a screen reader meets identical rows.
 *
 * **Lifted out of `AppearanceScreen` when the restore screen needed the same
 * question shape**, which is merge or replace. Two screens with the same
 * question drawn two ways is how an app grows a second standard, and rule 14
 * says the bar is retroactive rather than applying only to new work.
 *
 * @param isLast whether to draw the group's hairline beneath. The row owns it
 *   so that several of them read as one surface rather than as a stack.
 */
@Composable
fun ChoiceRow(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = Space.touchTarget)
                .clip(Radius.tile)
                .background(surface)
                .border(2.dp, colors.blue.copy(alpha = ring), Radius.tile)
                .clickable(
                    interactionSource = interaction,
                    // The row's own surface is the answer to the touch, per
                    // section 5.14. A ripple over it would be a second, louder
                    // one.
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .semantics { this.selected = selected }
                .let { if (testTag != null) it.testTag(testTag) else it }
                .padding(horizontal = Space.cardPadding, vertical = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // bidi-ok: every caller isolates before handing it here.
                Text(text = label, style = HealthTrail.type.label, color = colors.ink)
                Spacer(Modifier.height(Space.xs))
                // bidi-ok: every caller isolates before handing it here.
                Text(text = detail, style = HealthTrail.type.bodyM, color = colors.ink2)
            }
            if (selected) {
                Spacer(Modifier.width(Space.sm))
                Box(
                    modifier = Modifier
                        .size(Space.s)
                        .clip(CircleShape)
                        .background(colors.blueDeep),
                )
            }
        }
        if (!isLast) Hairline(inset = Space.cardPadding, end = Space.cardPadding)
    }
}
