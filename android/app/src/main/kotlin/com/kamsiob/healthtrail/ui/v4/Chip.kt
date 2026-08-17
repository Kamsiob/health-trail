package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A short counted fact, as a tonal pill. #386.
 *
 * **`m3v4-0` draws one and it is the reason this exists**: "6 readings" sits at
 * the trailing edge of the tracked measure's card, opposite the measure's own
 * name, saying how much is behind the number without competing with it. Measured
 * off the drawing at 74.7dp wide and 24.7dp tall, which is a 14sp line with
 * 12dp of air either side and 4dp above and below.
 *
 * **A fact, never a control.** It carries no press state and no minimum touch
 * target, because nothing happens when it is pressed and rule 16's promise runs
 * the other way: everything touchable responds, so anything that responds is
 * read as touchable. A chip that offered a press and did nothing would be the
 * broken control rule 16 exists to prevent.
 *
 * **It never says how something is going.** Rule 2 and law 4: this counts and
 * stops. The color it wears comes from the section it belongs to, which is
 * identity, so a chip on a measure's card is the same green as that section's
 * mark in the notebook and means nothing beyond "this is Progress".
 *
 * **Not merged for a reader here.** The caller decides: a chip inside a card
 * that already describes itself as one stop is part of that description, and a
 * chip standing on its own is its own words.
 */
@Composable
fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    /** The pill itself. A section's wash, or `sand` where the fact belongs to no section. */
    container: Color = HealthTrail.colors.sand,
    /** The words on it. A section's ink against its own wash. */
    content: Color = HealthTrail.colors.ink2,
) {
    Text(
        // bidi-ok: a chip is a counted fact the app composed from a catalog
        // template. A caller putting somebody's own words on one isolates them,
        // the way every other slot in this package does.
        text = label,
        style = HealthTrail.type.bodyM,
        color = content,
        modifier = modifier
            .clip(Radius.pill)
            .background(container)
            .padding(horizontal = Space.sm, vertical = Space.xs),
    )
}

/**
 * One short answer the person taps, and the set of them is the question.
 *
 * **Not [Chip], and the difference is the whole reason both exist.** [Chip] is a
 * counted fact that reports and cannot be touched. This is a control: it answers
 * a question, and rule 16 says everything touchable responds.
 *
 * **Tonal when open, filled when chosen.** `docs/V4.md` 2.1: no outlines, so the
 * difference between an answered and an unanswered chip is how much ink is on
 * the screen rather than whether it has an edge. The label inverts and goes to
 * the label weight with it, so selection survives a grayscale screen and any
 * color vision difference. Never hue alone.
 *
 * **It is never shaped like an action.** A chip sits in a wrapping row of its
 * siblings and answers a question; an action sits alone at `Radius.button` and
 * does something. That is why chips are never laid out one to a row.
 *
 * Announced as a radio button carrying its selected state, because that is what
 * it is: one answer out of a set. `DESIGN.md` 12.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * A care thread's route color, as a leading dot.
     *
     * **Never the only difference between two chips**, because a person who
     * cannot tell the colors apart would be choosing between two identical
     * words. The name carries the answer and this agrees with it.
     */
    dotColor: Color? = null,
    enabled: Boolean = true,
) {
    val colors = HealthTrail.colors
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = CHIP_HEIGHT)
            .clip(Radius.pill)
            .background(
                when {
                    !enabled -> colors.sand
                    selected -> colors.blue
                    else -> colors.sand
                },
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(CHIP_DOT)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(Space.s))
        }
        Text(
            // bidi-ok: a chip's label is a person's name, a thread they named or
            // the app's own short word, and the caller isolates what is theirs.
            text = label,
            style = if (selected) HealthTrail.type.label else HealthTrail.type.bodyM,
            // **`ink2` when disabled as well.** `ink3` is not a text ink in
            // this app, D92: it sits at 2.37:1 on paper, so an unavailable
            // answer would be one nobody could read rather than one nobody can
            // choose. The container says unavailable; the words stay legible.
            color = if (selected) colors.onBlue else colors.ink2,
        )
    }
}

/**
 * The way to the rest of the set, where a chip row is capped at [CHIP_CAP].
 *
 * **Chip shaped and deliberately not a chip.** It is announced as a button
 * rather than a radio button, because it is not one of the answers: choosing it
 * opens the full set rather than saying anything about what happened. Its label
 * is blue, which is what every action in this app is, so a person can tell at a
 * glance which of the six things in front of them is a way in rather than an
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
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = CHIP_HEIGHT)
            .clip(Radius.pill)
            .background(colors.sand)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // bidi-ok: the app's own words for the way to the rest of the set,
        // composed from a catalog template with a count in it.
        Text(text = label, style = HealthTrail.type.label, color = colors.blue)
    }
}

/**
 * A question with a set of chip answers under it.
 *
 * **The label is the eyebrow**, the same quiet line every group in this
 * interface is named by, so a chip set and a field group read as the same kind
 * of thing: a question, then a way to answer it.
 *
 * **The aside is part of the pattern rather than decoration.** "Roughly is fine"
 * and "Skip if you don't know" are what make a chip set an invitation instead of
 * a required field. Optional.
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
    /**
     * Whether the label is drawn.
     *
     * **False where an eyebrow directly above already says the same words.** The
     * emergency card had "WHO TO CALL FIRST" and "Who to call first" one under
     * the other with an explaining sentence between them. Seen on the phone.
     *
     * **The label is still required and still passed**, because a reader needs
     * the set named even when the eye does not: the heading above carries it for
     * somebody moving by headings, and this keeps the words at the call site so
     * turning it back on is a one word change rather than a rewrite.
     */
    showLabel: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        // bidi-ok: the app's own label for the question.
        if (showLabel) Eyebrow(text = label)
        // bidi-ok: the app's own sentence about how to answer it.
        aside?.let { Body(text = it, style = HealthTrail.type.bodyS) }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            // A chip's touch target is 48dp while its paint is 40dp, so the rows
            // sit further apart than the 8dp here suggests. That is the right
            // trade: the gap is generous, and a finger never misses.
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
            content = { content() },
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
 * Pure, and separate from the drawing, because this is the rule that has to be
 * right rather than the pixels: a cap that quietly dropped the chosen answer
 * would be a form that forgets what it was told.
 *
 * [all] arrives in whatever order its query set, which for people is most
 * recently involved first and for medications is the ones she is still on first.
 * **The cap takes the head of that order and does not reorder it**, so the
 * reasoning about what is likely lives in the query where the data is, rather
 * than here where it would be a guess.
 */
fun <T> cappedChips(all: List<T>, selected: T?, limit: Int = CHIP_CAP): List<T> {
    if (all.size <= limit) return all
    val head = all.take(limit)
    if (selected == null || selected in head) return head
    // The chosen one displaces the last of the head rather than being appended,
    // so the row never grows past the cap and never moves the other four.
    return head.dropLast(1) + selected
}

/**
 * The same cap where more than one chip can be chosen at once.
 *
 * **The single selection overload cannot serve a set.** The emergency card asks
 * which of the care team to put on it, and several people belong on it, so its
 * chip set was drawing the whole team: fifteen names filling the first screen,
 * with the fields the card exists for below the fold.
 *
 * **Everything chosen stays visible**, however many that is, because a chip that
 * disappears when the list is capped would hide a choice somebody made. The cap
 * fills out from the front with whoever is not chosen, so the row is at least
 * five where there are five to show and grows only with the choosing.
 */
fun <T> cappedChips(all: List<T>, selected: Set<T>, limit: Int = CHIP_CAP): List<T> {
    if (all.size <= limit) return all
    val chosen = all.filter { it in selected }
    val rest = all.filterNot { it in selected }
    return chosen + rest.take((limit - chosen.size).coerceAtLeast(0))
}

/** Five answers in front of the person, and a way to the rest. */
const val CHIP_CAP = 5

private val CHIP_HEIGHT = Space.xxl
private val CHIP_DOT = Space.s
