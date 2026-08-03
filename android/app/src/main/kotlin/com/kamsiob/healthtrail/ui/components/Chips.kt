package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * One short answer the person can tap, per `DESIGN.md` section 5.11.
 *
 * **Not a pill.** Section 5.6 pills report a state and cannot be touched. This
 * is the opposite: a control for choosing among a few short answers, which is
 * what screen 26 of the reference file needs for the rough date and the care
 * thread and which nothing existing could carry.
 *
 * **Selection is never carried by color alone.** A selected chip changes its
 * surface, changes its label to bold, and gains a 2dp ring. That is the section
 * 2.2 rule applied to a control: it stays distinguishable with color vision
 * differences and in a grayscale screenshot.
 *
 * The visual height is 40dp and the touch target is 48dp, reached by
 * `minimumInteractiveComponentSize` rather than by padding the chip out to a
 * shape nobody wanted. The accessibility floor is about where a finger lands,
 * not about how big the paint is.
 *
 * Announced as a radio button with its selected state, because that is what it
 * is: one answer out of a set.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A care thread's route color, shown as a leading dot. Never the only difference between two chips. */
    dotColor: Color? = null,
    enabled: Boolean = true,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }

    val resting = when {
        !enabled -> colors.sand.copy(alpha = 0.5f)
        selected -> colors.blueSoft
        else -> colors.sand
    }
    val labelColor = when {
        !enabled -> colors.ink3Text
        selected -> colors.blueDeep
        else -> colors.ink
    }

    val surface by pressedSurface(interaction, resting)
    val ring by focusRingAlpha(interaction)
    // A selected chip always carries its ring and a focused one gains it, so
    // focusing a chip that is already selected changes nothing rather than
    // flickering. The ring is one treatment doing two jobs, which is why it is
    // the same 2dp of `blue` in both cases.
    val ringAlpha = maxOf(if (selected) 1f else 0f, ring)

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = ChipHeight)
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ringAlpha), Radius.pill)
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interaction,
                // The chip's own surface is the press feedback, per 5.14. A
                // ripple on top would be a second, louder answer to one touch.
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(ChipDotSize)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(Space.s))
        }
        Text(
            text = label,
            style = if (selected) type.label else type.bodyM,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
        )
    }
}

/**
 * The way to the rest of the set, per `DESIGN.md` 5.11.1.
 *
 * **Chip shaped and deliberately not a chip.** It is announced as a button
 * rather than a radio button, because it is not one of the answers: choosing it
 * opens the full set rather than saying anything about what happened. Its label
 * is `blue`, which is what every action in this app is, so a person can tell at
 * a glance which of the six things in front of them is a way in rather than an
 * answer. Nothing else about it differs, because it lives in the same wrapping
 * row and a second shape there would read as a second kind of question.
 */
@Composable
fun MoreChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)
    val ring by focusRingAlpha(interaction)

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = ChipHeight)
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.pill)
            .clickable(
                interactionSource = interaction,
                // The chip's own surface is the press feedback, per 5.14.
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = type.label,
            color = colors.blue,
        )
    }
}

/**
 * Which of a long set of answers to put in front of the person.
 *
 * **The cap is five and the selected answer is always among them**, even when it
 * would otherwise fall outside, because a chip set that hides the answer the
 * person already chose is lying about the state of the form.
 *
 * Pure, and separate from the screen, because this is the rule that has to be
 * right rather than the pixels: a cap that quietly dropped the chosen answer
 * would be a form that forgets what it was told.
 *
 * [all] arrives in whatever order its query set, which for people is most
 * recently involved first and for medications is the ones she is still on
 * first. **The cap takes the head of that order and does not reorder it**, so
 * the reasoning about what is likely lives in the query where the data is,
 * rather than here where it would be a guess.
 */
fun <T> cappedChips(all: List<T>, selected: T?, limit: Int = CHIP_CAP): List<T> {
    if (all.size <= limit) return all
    val head = all.take(limit)
    if (selected == null || selected in head) return head
    // The chosen one displaces the last of the head rather than being appended,
    // so the row never grows past the cap and never moves the other four.
    return head.dropLast(1) + selected
}

/** Five, per `DESIGN.md` 5.11.1. */
const val CHIP_CAP = 5

private val ChipHeight = 40.dp
private val ChipDotSize = 8.dp

/**
 * A question with a set of chip answers under it.
 *
 * **The label uses the same treatment as a text field's label,** Body M in
 * `ink2` with 8dp beneath it, per section 5.9. Two different label treatments on
 * one screen is exactly the kind of thing that makes a screen read as assembled
 * rather than designed, and a chip group and a text field are the same thing
 * from the reader's side: a question, then a way to answer it.
 *
 * **The aside is part of the pattern rather than decoration.** "Roughly is fine"
 * and "Skip if you don't know" are what make a chip set an invitation instead of
 * a required field, and screen 26 draws them on every step. Optional.
 *
 * Chips wrap rather than scrolling sideways, because a horizontal scroller hides
 * answers and the whole point of the set is that every answer is visible at once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChipGroup(
    label: String,
    modifier: Modifier = Modifier,
    aside: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = type.bodyM, color = colors.ink2)
        if (aside != null) {
            Spacer(Modifier.height(Space.xs))
            Text(text = aside, style = type.bodyS, color = colors.ink3Text)
        }
        Spacer(Modifier.height(Space.s))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            // A chip's touch target is 48dp while its paint is 40dp, so the
            // rows sit further apart than the 8dp here suggests. That is the
            // right trade: the gap is generous, and a finger never misses.
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
            content = { content() },
        )
    }
}
