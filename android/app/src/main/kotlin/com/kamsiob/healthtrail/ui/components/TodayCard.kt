package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The width the corner chevron needs, plus the space around it.
 *
 * The chevron is 8dp wide with `Space.s` of padding on each side, and it floats
 * over the card's content rather than sitting in the layout, so anything that
 * reaches the end edge has to leave this clear by hand.
 */
private val CHEVRON_ROOM: Dp = 8.dp + 8.dp + 8.dp

/** How much room a card takes on the field. `DESIGN.md` 21.3. */
enum class CardSize {
    /** Half width. One answer, one line of context. One touch target. */
    SMALL,

    /** Full width. The answer plus two or three lines. Two targets. */
    WIDE,

    /** Full width, taller. A chart, a mini spine, or a short list. Three. */
    TALL,
    ;

    companion object {
        fun of(stored: String): CardSize = when (stored) {
            "wide" -> WIDE
            "tall" -> TALL
            else -> SMALL
        }
    }
}

/**
 * One card on Today. `DESIGN.md` 21.2.
 *
 * **A card is a named, deterministic question asked of the record**, and this
 * is the shape all seventeen of them share: an index tab in the card's own
 * section hue naming where the answer lives, the answer itself, a quiet line
 * under it, and a corner chevron, because every card is a door.
 *
 * **Identity comes from the tab pack, never from decoration.** An appointments
 * card is slate because appointments are slate everywhere in the binder, and a
 * card for a whole-app surface wears gold. Nothing here colors by value, by
 * urgency, or by how the answer is going: 21.8, and the semantic colors keep
 * their locked meanings.
 *
 * **Growing a card reveals more of the same answer**, 21.3, so size is passed
 * to the content rather than switching it: the medications card at small is a
 * count and at wide it is the list, and both are the same question.
 *
 * **It is one node to a reader.** A card that announced its tab, its number,
 * its line and its chevron as four stops would make somebody listen to four
 * things to learn one. The caller composes the sentence.
 *
 * **When to use it.** On Today, and only there. It is the surface's unit.
 *
 * **When not to use it.** Anywhere else in the app. A card here is something
 * the person chose to have and can remove, and using the shape on a screen
 * where they cannot would make that promise somewhere it is not true.
 */
@Composable
fun TodayCard(
    /** The section this card's answer lives in, in mono. */
    tab: String,
    /** The tab's hue, from the tab pack. Gold for a whole-app surface. */
    hue: TabHue,
    /** What a reader says instead of the card, composed by the caller. */
    description: String,
    onOpen: () -> Unit,
    /** The verb a reader announces for the tap. */
    openLabel: String,
    size: CardSize,
    modifier: Modifier = Modifier,
    /**
     * Whether the card speaks as one node.
     *
     * **True while it is only a door**, which is almost always: a card that
     * announced its tab, its number, its line and its chevron as four stops
     * would make somebody listen to four things to learn one.
     *
     * **False in edit mode**, and that is not a preference. `clearAndSetSemantics`
     * clears every descendant, so the Move up, Move down and Remove controls
     * inside the card were unreachable by a screen reader and by switch access:
     * the accessible reorder path 21.6 exists to provide did not exist. Found by
     * trying to drive the controls from a semantics dump and finding nothing
     * there.
     */
    speaksAsOneNode: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    // **One floor for all three sizes, and tall earns its height from content.**
    // Tall reserved 168dp, so a card the person made tall whose record has
    // nothing more to show, a measure with one reading and no line to draw,
    // rendered as a hundred points of empty box. Rule 11: no blank area, and a
    // reserved height is a blank area with a reason.
    //
    // **Nothing is hidden by not padding it.** 21.3: shrinking never hides the
    // existence of something open, only its detail, and there is no detail
    // here to hide. A chart or a short list makes a tall card tall; an empty
    // record makes it the size of what it has to say, which is honest.
    val minHeight: Dp = 96.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(15.dp))
            .openableByTap(label = openLabel, onTap = onOpen)
            .then(
                if (speaksAsOneNode) {
                    Modifier.clearAndSetSemantics { contentDescription = description }
                } else {
                    Modifier.semantics { contentDescription = description }
                },
            ),
    ) {
        Column(modifier = Modifier.padding(Space.sm)) {
            Text(
                text = tab,
                style = type.mono,
                color = hue.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    // **The chevron's room, kept clear.** The tab had no width
                    // limit and the chevron floats in the corner over the top
                    // of it, so a card naming its source, "Project · Appeal the
                    // level of care assessment", ran its own words underneath
                    // the chevron and ellipsized behind it. Seen on the phone
                    // and invisible in the code, because nothing collides until
                    // the text is long enough.
                    .padding(end = CHEVRON_ROOM)
                    .clip(RoundedCornerShape(5.dp))
                    .background(hue.wash)
                    .padding(horizontal = Space.s, vertical = 2.dp),
            )
            Column(modifier = Modifier.padding(top = Space.s)) {
                content()
            }
        }
        Chevron(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Space.s),
        )
    }
}
