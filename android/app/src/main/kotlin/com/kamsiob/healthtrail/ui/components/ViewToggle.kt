package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Two or three views of the same content, and the person's choice is remembered
 * per section. `DESIGN.md` section 7 and law 4.
 *
 * **It switches a view. It never navigates and it never changes the subject.**
 * If tapping it would take the person somewhere else, it is a destination and
 * belongs in a list or a grid. That distinction is what keeps this from becoming
 * a second, quieter navigation bar, which is what every segmented control in
 * every app eventually turns into if nobody writes the rule down.
 *
 * **Density is the person's, not ours.** The default is set per section, and
 * whatever they pick is remembered for that section. A person who wants
 * documents as a grid and the trail as a compact list gets both, permanently,
 * without a settings screen.
 *
 * **Selection is surface plus weight, never color alone**, which is the same
 * language the choice chip uses rather than a second one, and which is what
 * `DESIGN.md` 4.4 requires of every state that has a color.
 *
 * **It never scrolls and never wraps.** At a font scale where the labels no
 * longer fit it becomes a stacked column of full width choices rather than
 * shrinking its text below the 13sp floor. **That is the same control laid out
 * for the space it has, not a fallback**, and it is why [ViewToggle] takes its
 * options as a list rather than as two fixed slots.
 *
 * **When not to use it.** More than three options, which is chips. Anything
 * that navigates. A single on or off, which is a setting.
 */
@Composable
fun ViewToggle(
    options: List<ViewOption>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(options.size in 2..3) {
        // Four segments is the point at which a person stops reading it as a
        // pair of views and starts reading it as navigation, which is exactly
        // what this control must never become.
        "A view toggle carries two or three options, not ${options.size}. DESIGN.md section 7."
    }

    val colors = HealthTrail.colors
    val strings = LocalStrings.current

    Row(
        modifier = modifier
            .clip(ToggleShape)
            .border(1.5.dp, colors.hairlineHeavy, ToggleShape)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option.key == selected
            val interaction = remember(option.key) { MutableInteractionSource() }
            val resting = if (isSelected) colors.ink else Color.Transparent
            val surface by pressedSurface(interaction, resting)

            Text(
                text = strings[option.labelKey],
                style = HealthTrail.type.label,
                color = if (isSelected) colors.paper else colors.ink2,
                modifier = Modifier
                    .selectable(
                        selected = isSelected,
                        interactionSource = interaction,
                        // No ripple. D175: the app answers a touch by
                        // springing and by shifting the surface, and this
                        // segment was the last place a spreading circle
                        // appeared. The selected surface already says which
                        // one is chosen, so the press only has to be felt.
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(option.key) },
                    )
                    .background(surface)
                    .defaultMinSize(minHeight = Space.touchTarget)
                    .padding(horizontal = Space.sm, vertical = Space.s),
            )
        }
    }
}

/**
 * One view of a section's content.
 *
 * @param key what gets remembered per section. It is stored, so it is stable
 *   rather than an index: reordering the options must not silently change what
 *   somebody chose a year ago.
 */
data class ViewOption(val key: String, val labelKey: String)

private val ToggleShape = RoundedCornerShape(percent = 50)
