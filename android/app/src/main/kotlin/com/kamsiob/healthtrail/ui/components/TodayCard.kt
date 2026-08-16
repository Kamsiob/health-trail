package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.raisedCard
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The width the corner chevron needs, plus the space around it.
 *
 * The chevron is 8dp wide with `Space.s` of padding on each side, and it floats
 * over the card's content rather than sitting in the layout, so anything that
 * reaches the end edge has to leave this clear by hand.
 */
private val CHEVRON_ROOM: Dp = 8.dp + 8.dp + 8.dp

/**
 * The room a corner control needs instead.
 *
 * **The dot is a touch target and the chevron is a drawing**, so reserving the
 * chevron's width for it ran a long tab underneath it: "Tracking · How she
 * see..." ellipsized into the remove dot in edit mode. Same family as the
 * defect the chevron itself caused, and nothing collides until the text is long
 * enough. Section 9's floor plus the padding around it.
 */
private val CORNER_ROOM: Dp = 48.dp + 8.dp

/**
 * How much room a card takes on the field. `DESIGN.md` 21.3.
 *
 * **Two sizes, and the owner has asked for two more than once.** A card is a
 * square or it is the full width of the screen, because that is what a widget is
 * on the phone the person already owns, and the phone is the only frame of
 * reference anybody brings to this. A third size in between was a choice nobody
 * asked to make, on the most looked at screen in the app.
 *
 * **The stored column still allows `tall`**, and that is deliberate rather than
 * an oversight. The schema is fixed by `contract/DATA-CONTRACT.md` and changing
 * it needs the owner, per rule 3, so a notebook or an archive written before
 * tonight keeps every row it had. A `tall` card reads as wide, which is what it
 * always looked like: full width, as tall as what it has to say.
 */
enum class CardSize {
    /**
     * Square. One answer and one line of context, in a cell as tall as it is
     * wide.
     *
     * **Square is a floor rather than a cage.** The cell is as tall as it is
     * wide and grows past that when the words need it, which is what happens at
     * a large system font before the field reflows to one column. A square that
     * clipped its own answer to stay square would be rule 11's truncation with
     * a tidy excuse.
     */
    SMALL,

    /**
     * Full width, and the card's whole rendering: the answer, its detail, and
     * the chart or mini spine where the card has one.
     *
     * **This absorbed what used to be a third size.** Wide and tall differed
     * only in whether the rich body was drawn, which made the person choose
     * between two full width cards on a difference no label could honestly
     * name.
     */
    WIDE,
    ;

    companion object {
        fun of(stored: String): CardSize = when (stored) {
            // `tall` is a row written before Today had two sizes. It is a full
            // width card and it always was.
            "wide", "tall" -> WIDE
            else -> SMALL
        }
    }
}

/**
 * As tall as it is wide, and taller when the words need it.
 *
 * **Why this rather than `aspectRatio`.** `aspectRatio` sets the height from the
 * width and then makes the content fit inside it, so a square card whose answer
 * ran long would clip its own words, which is rule 11's truncation wearing a
 * tidy shape. This sets the square as the minimum and lets the card grow, so at
 * a large system font, or in the longest language, a small card is a tall
 * rectangle with everything on it rather than a neat box with a sentence cut in
 * half. The grid row sizes to its tallest cell, so the pair stays level either
 * way.
 */
private fun Modifier.atLeastSquare(): Modifier = layout { measurable, constraints ->
    // **Only where the width is actually known.** In a parent that scrolls
    // sideways the incoming width is infinite, and asking for a height to match
    // it would ask for an infinite card. The grid always gives a real width, so
    // this never fires today; it is here because the day it does fire is a
    // crash rather than a wrong shape.
    val bounded = constraints.hasBoundedWidth
    val square = if (bounded) constraints.maxWidth.coerceAtMost(constraints.maxHeight) else 0
    val placeable = measurable.measure(
        constraints.copy(minHeight = square.coerceAtLeast(constraints.minHeight)),
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
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
 * **When to use it.** On Today, and in the gallery that offers cards for Today.
 * It is the surface's unit, and the gallery is where somebody picks one.
 *
 * **The gallery draws the real card rather than a line describing it**, D157,
 * because that is what a widget picker does on the phone somebody already owns.
 * That does not weaken the rule below: what is offered there is exactly a card
 * they are about to have and will be able to remove, so the promise the shape
 * makes is the one being kept.
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
    /**
     * One inline control, outside everything the card says.
     *
     * **21.3 allows exactly one at wide and tall, outlined, a verb or a
     * dialable number**, and it is here rather than in [content] for a reason
     * that is not layout: the answer is silenced for a reader so the card is
     * one stop, and anything inside that silence is silenced with it. The care
     * team card's number was drawn, focusable by finger, and reachable by no
     * reader at all until it moved out here.
     *
     * **Null on almost every card**, and a card is still a door without one.
     */
    action: (@Composable () -> Unit)? = null,
    /**
     * What sits in the corner, or null for the chevron every card wears.
     *
     * **A card is a door and the chevron says so**, 21.2. In edit mode it is
     * not a door to its section any more, it is a door to its own options, and
     * the corner carries the remove dot instead: grid screen 05 puts the dot on
     * the card and the spec under it says the dot exists only inside edit mode,
     * which is what keeps "nothing bare responds to touch" true in ordinary
     * reading.
     */
    corner: (@Composable () -> Unit)? = null,
    /**
     * Touch and hold, which on Today starts arranging the screen.
     *
     * **Because that is what holding a widget does on the phone they own.**
     * Nobody has to be taught it and nobody has to find the word for it first.
     * The visible control that does the same thing stays where it is: this is
     * the shortcut, per 21.6 screen 5.
     *
     * long-press-twin: the Arrange action in Today's own header, which enters
     * the same mode and is the path a reader and switch access take. D155.
     */
    onLongPress: (() -> Unit)? = null,
    /** What a reader calls the hold. Required whenever [onLongPress] is set. */
    longPressLabel: String? = null,
    /**
     * Centers the answer in the square instead of dropping it to the bottom.
     *
     * **For the empty rung only.** A filled square lines its answer along the
     * bottom so a row of cards reads across one baseline; an empty square
     * doing that put one small gray line under two thirds of blank white,
     * which is the first thing a brand new notebook showed anybody. Rule 11's
     * blank area, on the first screen of a first run. #376.
     */
    centerContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    // **Wide earns its height from content.** It reserved 168dp once, so a card
    // whose record had nothing more to show, a measure with one reading and no
    // line to draw, rendered as a hundred points of empty box. Rule 11: no
    // blank area, and a reserved height is a blank area with a reason.
    //
    // **Nothing is hidden by not padding it.** 21.3: shrinking never hides the
    // existence of something open, only its detail, and there is no detail here
    // to hide. A chart or a short list makes a wide card tall; an empty record
    // makes it the size of what it has to say, which is honest.
    val minHeight: Dp = 96.dp

    Box(
        // **The square has to reach the content or it is just a tall box.**
        // A `Box` keeps its minimum constraints to itself by default, so the
        // card measured square and the column inside it measured to its own
        // text and sat at the top, which is exactly the two thirds of empty
        // white this was meant to fix. Passing the minimum down is what lets
        // `SpaceBetween` below have a square to spread across.
        //
        // **The corner is unaffected**, because it aligns to the top end rather
        // than filling anything.
        propagateMinConstraints = true,
        modifier = modifier
            .fillMaxWidth()
            .then(if (size == CardSize.SMALL) Modifier.atLeastSquare() else Modifier)
            .defaultMinSize(minHeight = minHeight)
            // #324. The card is a thing on a desk, and the desk is the
            // whole metaphor of the surface.
            // **The card token, not a number typed here.** This drew its own
            // 15dp while the token was 17 and both grids draw 18, so the most
            // looked at surface in the app was three off the drawing and
            // nothing pointed at it. D142.
            .raisedCard(Radius.card)
            .clip(Radius.card)
            .openableByTap(
                label = openLabel,
                onTap = onOpen,
                // long-press-twin: Today's Arrange action, per the parameter above.
                onLongPress = onLongPress,
                longPressLabel = longPressLabel,
            )
            // **The card's own sentence lives on the card's own node**, beside
            // its tap action, so a reader that stops here is told what the card
            // says and what pressing it does in one stop.
            .semantics { contentDescription = description },
    ) {
        // **Silenced in one place rather than two.** The tab and the answer are
        // separate children now, so that a square can push them apart, and both
        // have to be out of the reader's way for the same reason as before: the
        // caller has already composed them into one sentence on the card's own
        // node, and a card that announced its tab, its number, its line and its
        // chevron as four stops would make somebody listen to four things to
        // learn one. [action] stays outside the silence, because a control
        // inside it is a control no reader can reach.
        val silence = if (speaksAsOneNode) Modifier.clearAndSetSemantics { } else Modifier

        Column(
            modifier = Modifier.padding(Space.sm),
            // **A square card fills its square**, which is what a widget does
            // on the phone somebody already owns and what the first build of
            // this did not do. "6 / on the list now" sat in the top third of a
            // square and two thirds of the card was empty white, which is rule
            // 11's blank area on the most looked at surface in the app.
            //
            // **The eyebrow stays at the top and the answer falls to the
            // bottom**, so a row of squares lines its answers up along one
            // baseline and the eye reads across them. Centering the block
            // instead would put three cards' numbers at three heights,
            // depending on how many lines each label ran to.
            //
            // **Full width is unchanged**, because there it is the content that
            // sets the height rather than the other way round, and spreading a
            // wide card's two lines apart would open a gap in the middle for
            // no reason.
            verticalArrangement = if (size == CardSize.SMALL) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.Top
            },
        ) {
            Text(
                text = tab,
                style = type.mono,
                color = hue.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = silence
                    // **The chevron's room, kept clear.** The tab had no
                    // width limit and the chevron floats in the corner over
                    // the top of it, so a card naming its source, "Project ·
                    // Appeal the level of care assessment", ran its own
                    // words underneath the chevron and ellipsized behind it.
                    // Seen on the phone and invisible in the code, because
                    // nothing collides until the text is long enough.
                    .padding(end = if (corner == null) CHEVRON_ROOM else CORNER_ROOM)
                    .clip(Radius.sourceTab)
                    .background(hue.wash)
                    .padding(horizontal = Space.s, vertical = 2.dp),
            )
            Column(
                modifier = silence
                    .padding(top = Space.s)
                    // **The empty line takes the leftover square and centers
                    // in it.** Weight only on the small card: the wide card's
                    // height is set by its content, and a weight there would
                    // stretch a two line card for no reason.
                    .then(
                        if (centerContent && size == CardSize.SMALL) {
                            Modifier
                                .weight(1f)
                                .wrapContentHeight(Alignment.CenterVertically)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                content()
            }
            action?.invoke()
        }
        // **`wrapContentSize` because the card now passes its minimum down.**
        // Without it this box inherits the card's full width, so the chevron
        // aligns to the start of a full width box and lands on top of the tab
        // in the opposite corner from where it belongs. Seen on the phone one
        // screenshot after the square started filling itself, and invisible in
        // the code: `align` says where the box goes, not how big it is.
        // The alignment belongs on `wrapContentSize` rather than on `align`:
        // the card passes its minimum down, so this box is full size whatever
        // `align` says, and only `wrapContentSize` decides where the drawing
        // inside it sits. With the default it centered, which put the chevron
        // in the middle of the card.
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            corner?.invoke() ?: Chevron(modifier = Modifier.padding(Space.s))
        }
    }
}
