package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
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
/**
 * Where a large system font stops being a font size and starts being a layout.
 *
 * At and above this the Today field reflows to one column and a card's height
 * becomes a floor rather than a fixed value. **One number, read by the grid and
 * by the card**, because two copies of it is a wide card holding a fixed height
 * in a field that has already reflowed.
 */
const val WIDE_TYPE_SCALE = 1.5f

enum class CardSize {
    /**
     * Square, half the width. One answer and one line of context.
     *
     * **The height is the same as [WIDE]'s and comes from this one**: the
     * square's side is the field's only card height. Owner, 2026-08-17.
     */
    SMALL,

    /**
     * Full width, and the card's whole rendering: the answer, its detail, and
     * the chart or mini spine where the card has one.
     *
     * **Exactly as tall as [SMALL]**, owner 2026-08-17: two widths, one height,
     * and nothing in between. A wide card that grew with its content was
     * inventing a third size, and a field of five different heights has no
     * rhythm for the eye to follow.
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
    // **Shorter than square**, D172. A square small card is a little over two
    // hundred dp tall on a phone, and the honest answer in one is three words:
    // "Nothing scheduled" sat in the middle of an empty white field with a
    // hand's width of nothing above and below it. That is not calm, it is
    // unfinished, and four of them at once was most of what the screen was.
    //
    // **The proportion is the approved mockup's**, whose count tiles are 120dp
    // against a 170dp width. This keeps the tile a tile rather than letting it
    // collapse to a row, and lets a card with a real answer in it grow past the
    // minimum as it always could.
    val bounded = constraints.hasBoundedWidth
    val tile = if (bounded) {
        (constraints.maxWidth * TILE_HEIGHT_RATIO).toInt().coerceAtMost(constraints.maxHeight)
    } else {
        0
    }
    val placeable = measurable.measure(
        constraints.copy(minHeight = tile.coerceAtLeast(constraints.minHeight)),
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** The approved mockup's tile: 120dp tall against a 170dp column. */
private const val TILE_HEIGHT_RATIO = 0.70f

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
    /**
     * The white surface with an edge, for a card holding the person's own
     * record rather than the app's summary of one. **D190, decided and until
     * now unbuilt.**
     *
     * `m3v4-0` puts a tracked measurement on white: a column of numbers
     * somebody wrote down themselves is theirs in the same sense a photograph
     * of a discharge letter is. **It carries a hairline**, because `paper` is
     * `#FBFAF8` and `card` is `#FFFFFF`, four values apart, so a white card on
     * the canvas would have no boundary at all. That is an edge and not a
     * raise: the only shadow in this app is still under the person's paper.
     */
    onOwnPaper: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    // **One height, two widths, and nothing in between.** Owner ruling,
    // 2026-08-17: "all widgets are either small (square) which takes up half
    // width or a large rectangle which fills the width, height is the same
    // regardless. don't invent new sizes."
    //
    // So the height is the square's side, worked out from the grid the field
    // actually uses: the screen less its two margins less the gap between two
    // columns, halved. A wide card is that height and the full width. Cards
    // that grew with their content, the medication list and the measure with a
    // chart, were inventing sizes between the two, and a field of five
    // different heights is a field with no rhythm.
    // **The window's own width, not the configuration's.** Lint is right that
    // `Configuration.screenWidthDp` is the wrong question in a windowed world:
    // it answers about the display rather than about the space this composable
    // was actually given.
    val width = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    val cardHeight: Dp = (width - Space.screenHorizontal * 2 - Space.cardGap) / 2

    // **Except at a large system font, where it is a floor.** There the field
    // has already reflowed to one column and the words need the room: a fixed
    // height would clip somebody's own medication name, which is rule 11's
    // truncation wearing a tidy shape. The two rules do not conflict, because
    // nobody sees a two column grid and a clipped card at the same time.
    val fixed = LocalDensity.current.fontScale < WIDE_TYPE_SCALE

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
            .then(
                if (fixed) Modifier.height(cardHeight) else Modifier.heightIn(min = cardHeight),
            )
            // #324. The card is a thing on a desk, and the desk is the
            // whole metaphor of the surface.
            // **The card token, not a number typed here.** This drew its own
            // 15dp while the token was 17 and both grids draw 18, so the most
            // looked at surface in the app was three off the drawing and
            // nothing pointed at it. D142.
            // **Flat, because depth here is `paper` against the block and never
            // elevation.** `docs/V4.md` 2.1, and the only shadow in the app is
            // under the person's own paper, which casts one. A field of raised
            // white rectangles is what made this surface read as unfinished
            // beside the hero above it. D193.
            .clip(Radius.cardLarge)
            // **The card is quiet and the screen has one colored thing.**
            // D171. Tinting every card turned Today into a rainbow: six
            // washes competing, none of them meaning anything, and the eye
            // with nowhere to land. Restraint is what elegance is made of
            // here: the lead carries the color, the cards carry the content,
            // and the section's hue appears as one small mark rather than as
            // a field.
            // **`sand`, the language's ordinary group.** White is reserved for
            // the person's own paper, a form's action band and a tracked
            // measure, D190, and a card counting what is on a list is none of
            // those. On `paper` a white card has no edge at all, which is why
            // the field needed a shadow to be visible: the tonal block needs
            // neither. **Two places, because the resting surface is the one
            // that paints.**
            .background(if (onOwnPaper) colors.card else colors.sand)
            .then(
                if (onOwnPaper) {
                    Modifier.border(Space.hairlineWidth, colors.hairline, Radius.cardLarge)
                } else {
                    Modifier
                },
            )
            .openableByTap(
                label = openLabel,
                onTap = onOpen,
                // **The resting surface is the one that actually paints**, and
                // the background above sits under it. A card given the white
                // surface here and the sand one below is drawn in sand.
                resting = if (onOwnPaper) colors.card else colors.sand,
                shape = Radius.cardLarge,
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
            modifier = Modifier.padding(Space.ml),
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
            // **One quiet line, in the app's own body face.** D171. This was
            // a filled chip, then it was uppercase mono in the section's ink,
            // and both were louder than the answer underneath them. Uppercase
            // letterspaced mono is a third typographic voice, and three
            // voices on a card the size of a stamp is why the grid read as
            // noise. The hue survives as a dot: enough to tell the sections
            // apart, not enough to shout.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = silence) {
            Text(
                // **The eyebrow's metrics and the section's ink, in sentence
                // case.** The mockups set these labels in capitals, and every
                // label they draw is short: "WEIGHT", "PART OF", "DECISION
                // EXPECTED". This one is written by the person, "Project,
                // Medicaid application", and capitals cost about fifteen
                // percent of the width: on a half width card it ellipsized
                // their own project name, which rule 11 bans by name. Seen on
                // the phone, twice, since the first attempt broke it mid word
                // as well.
                text = tab,
                // **The eyebrow without its tracking**, which is the third
                // attempt at this line and the one that fits. 0.14em is what
                // makes a short label read as deliberate, and it adds about
                // fifty points of width to a thirty character one: "Project,
                // Medicaid application" ellipsized on a half width card in
                // capitals and again in sentence case. The size, the weight
                // and the section's ink are what carry the treatment; the
                // tracking is what a label the person wrote cannot afford.
                style = type.eyebrow.copy(letterSpacing = TextUnit.Unspecified),
                color = hue.ink,
                // **Two lines rather than an ellipsis.** D173, and rule 11
                // bans truncation by name. A tracked measure's tab is
                // "Tracking, " plus whatever the person called the thing they
                // are tracking, and their own words are exactly what must not
                // be cut: "Tracking, How she ..." on a card half the screen
                // wide tells them nothing and looks broken. Two lines holds
                // every label the fixture has at the largest font scale, and a
                // card that grows a line is a card that grew a line.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // **One weight in this row, not two.** The first build gave
                // the label `weight(1f, fill = false)` and then put a weighted
                // spacer after it, so the two split the row evenly and a label
                // that had half a card broke mid word: "PROJEC" over "T, ME".
                // Seen on the phone one screenshot later.
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (corner == null) Space.s else CORNER_ROOM),
            )
            if (corner == null) {
                Symbol(
                    symbol = Symbols.forward,
                    contentDescription = null,
                    modifier = Modifier.size(Space.ml),
                    tint = colors.ink3,
                )
            }
            }
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
        // **Only the corner slot floats now.** The chevron used to hang here
        // and a long tab ran its own words underneath it: "Project, Appeal the
        // level of care assessment" ellipsized behind a drawing. It sits at the
        // end of the eyebrow's row instead, where nothing can collide with it.
        // The corner keeps the remove dot, which is what edit mode draws here.
        if (corner != null) {
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                corner.invoke()
            }
        }
    }
}
