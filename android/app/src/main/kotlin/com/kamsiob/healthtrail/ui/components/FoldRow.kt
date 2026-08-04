package com.kamsiob.healthtrail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * Content waiting behind one tap, named and counted. `DESIGN.md` section 7, and
 * one of the six costumes in law 2.
 *
 * **This is the workhorse of law 1.** Every screen opens with one dominant
 * element and up to three supporting items, and **everything else on that
 * screen becomes one of these**. It is what lets a screen be calm for the
 * person standing in a hallway without hiding anything from the person with ten
 * minutes.
 *
 * **A quieter surface means "more lives here."** That is the whole of its
 * costume, and it is why the fold is never drawn on `card`: a fold on a raised
 * surface reads as an item in a list rather than as a door to more of them.
 *
 * **It uses `foldSurface`, not `sand`, and the difference is not pedantry.** In
 * light the two are the same value. In dark, `sand` is lighter than `card`,
 * because a recessed input on a dark screen reads as lighter, and that would
 * make the fold the brightest thing on the screen. `foldSurface` sits between
 * `paper` and `card` in dark, so the fold recedes in both themes. **What is kept
 * across themes is the meaning rather than the token.**
 *
 * **It always says what it holds and how much.** A fold with no count is a
 * mystery box, and the count is the entire reason folding is not hiding: a
 * person can see there are eighty-four more things waiting without being made
 * to look at eighty-four things. **Nothing disappears, it waits.**
 *
 * **It opens in place**, per `DESIGN.md` section 9, rather than navigating,
 * unless what it holds exceeds a screenful. A fold that opens a new screen is a
 * row with a chevron and should be built as one.
 *
 * **When not to use it.** Never where the content is a single item, which is a
 * row. Never to defer something the person needs right now, which is the one
 * thing the screen should have led with. Never more than about four on one
 * screen: past that the screen is a list of folds, which is the same failure as
 * a list of cards wearing a different costume.
 */
@Composable
fun FoldRow(
    labelKey: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    count: String? = null,
) {
    FoldRowText(
        label = LocalStrings.current[labelKey],
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        count = count,
    )
}

/**
 * The same fold over a label composed from the person's own data, such as a
 * month in the trail or the name of a facility a group belongs to.
 */
@Composable
fun FoldRowText(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    count: String? = null,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }

    // Open, the fold stops being sand and becomes the surface its contents sit
    // on, which is what tells the person the door is already open rather than
    // leaving two identical rows one of which happens to be showing something.
    val resting = if (expanded) colors.card else colors.foldSurface
    val surface by pressedSurface(interaction, resting)

    // The chevron turns rather than being swapped for a different glyph, so the
    // open and closed states are visibly the same control in two positions.
    val turn by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = LocalMotion.current.standard(),
        label = "fold",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FoldShape)
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onToggle,
            )
            // The row is 46dp visually and the touch target is 48dp, per
            // DESIGN.md section 6. Both, rather than either.
            .defaultMinSize(minHeight = Space.touchTarget)
            .padding(horizontal = Space.cardPadding, vertical = Space.rowPaddingTight)
            // **The platform announces the state, not this app.** `expand` and
            // `collapse` are standard semantics actions, so TalkBack says
            // "expand" or "collapse" in the reader's own language, already
            // translated, already the wording that reader user expects from
            // every other app on the phone. Writing our own would mean four
            // catalog entries saying something the system already says better.
            .semantics {
                if (expanded) {
                    collapse { onToggle(); true }
                } else {
                    expand { onToggle(); true }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Text(
            text = label,
            style = type.bodyS,
            color = colors.ink2,
            modifier = Modifier.weight(1f, fill = false),
        )

        // The count sits in its own pill on the raised surface, so it reads as a
        // quantity rather than as part of the sentence. Mono and tabular, per
        // DESIGN.md section 5: a count is data.
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (expanded) colors.foldSurface else colors.card)
                    .padding(horizontal = Space.s, vertical = 2.dp),
            ) {
                Text(text = count, style = type.mono, color = colors.ink2)
            }
        }

        Box(modifier = Modifier.weight(1f))

        // The chevron mirrors in Arabic on its own, per Chevron, and the
        // rotation composes with that: closed it points along the reading
        // direction, open it points down in both directions, because "open"
        // is not a direction the language has an opinion about.
        Chevron(modifier = Modifier.rotate(turn))
    }
}

/**
 * A fold with nothing behind it but an action, drawn bare.
 *
 * The grid uses this shape twice, for "Skip this question" and "Track something
 * new", where the control belongs at the bottom of the screen in the fold's
 * position but is not opening anything. **It carries no sand and no count**,
 * because it is not a fold: it is a quiet action in the fold's place, and
 * dressing it as a fold would promise content that is not there.
 *
 * It is still a costumed, handled control, so law 2 holds. It is centered
 * because it is a single action rather than a labeled group.
 */
@Composable
fun QuietAction(
    labelKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, Color.Transparent)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FoldShape)
            .background(surface)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            )
            .defaultMinSize(minHeight = Space.touchTarget)
            .padding(horizontal = Space.cardPadding, vertical = Space.rowPaddingTight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = LocalStrings.current[labelKey],
            style = HealthTrail.type.bodyS,
            color = colors.ink2,
        )
    }
}

/**
 * Softer than a card, because a fold is a row rather than a surface that holds
 * things. The grid draws it at 14dp against the card's 17dp.
 */
private val FoldShape = RoundedCornerShape(14.dp)
