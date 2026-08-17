package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * What is coming, as the one saturated block on the screen. #386.
 *
 * **This is the lead of `m3v4-0`**, measured: a filled blue block at
 * `Radius.cardLarge`, a quiet pill saying when, the faces of whoever it is with
 * opposite it, the thing itself at display size, where it is under that, and an
 * inset white card carrying the one thing worth doing before it.
 *
 * **Blue, and it is still only ever an action.** `docs/V4.md` 2.1 gives blue to
 * actions alone, and the whole of this block is a door: it is the next
 * appointment and pressing it opens the appointment. A saturated block that did
 * nothing on press would be the broken control rule 16 names.
 *
 * **One of these per screen at most**, 2.1. It is the thing the person opened
 * the app to find, so everything under it recedes and nothing else on the screen
 * is filled.
 *
 * **The ink comes from the palette, never from the call site.** `onBlue` flips
 * with the theme, so the words stay readable when blue goes pale in dark and the
 * ink under it goes near-black. D172: the palette flips and call sites do not.
 *
 * **Two stops for a reader, not one.** The appointment is one thing to hear and
 * the door inside it is another, and merging them would announce a paragraph and
 * then offer no way to press only the part that is a separate destination.
 */
@Composable
fun NextBlock(
    /**
     * The thing itself, in the person's own words, or the app's line when there
     * is nothing ahead.
     *
     * **A calendar with nothing on it is a finished state**, rule 13, so the
     * empty case keeps the block and says what is true in the same register.
     * The block never reads as something the person failed to fill in.
     */
    title: String,
    /** What a reader hears for the block itself, as one sentence. */
    description: String,
    modifier: Modifier = Modifier,
    /**
     * When it is, already said the way somebody standing in a kitchen needs it.
     *
     * Null when there is nothing ahead, and the pill is then absent rather than
     * saying so: a label over an empty value is the blank area rule 11 bans.
     */
    whenLabel: String? = null,
    /** Where it is, when the person wrote it down. Absent is ordinary, rule 13. */
    where: String? = null,
    /** Whoever it is with, as their marks. Empty draws none. */
    faces: List<Face> = emptyList(),
    /**
     * Opens the thing itself. Null where there is nothing to open.
     *
     * **The words stop being a target when there is nothing behind them**, rule
     * 16 read the other way: a press that does nothing reads as broken, so the
     * empty block offers its actions and not itself.
     */
    onOpen: (() -> Unit)? = null,
    /**
     * The foot of the block: one wide thing and up to two marks, on one row.
     *
     * **One row, owner ruling 2026-08-17**: the actions "should be streamlined
     * and on the same row". Stacked full width pills made the block tall and
     * read as a menu rather than as one answer with a couple of ways to act on
     * it. [InsetDoor] is the wide one and [BlockIconAction] the marks.
     *
     * **White is what a person can press inside this block**, which is the rule
     * the inset card set: the block is one saturated surface, the pill saying
     * when is a piece of it, and everything drawn on `card` is a door. One
     * vocabulary, learned once.
     */
    footer: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = HealthTrail.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(colors.blue)
            .padding(Space.ml),
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onOpen == null) {
                        Modifier
                    } else {
                        Modifier.clickable(role = Role.Button, onClick = onOpen)
                    },
                )
                .semantics(mergeDescendants = true) { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
        ) {
            if (whenLabel != null || faces.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // **A pill of the block's own color lightened, not a second
                    // hue.** Measured off the drawing at white over blue at
                    // twenty-two percent, which reads as a quieter piece of the
                    // same surface rather than as another thing with its own
                    // meaning. **And never white**, because white inside this
                    // block means a door, and this pill is not one.
                    whenLabel?.let { label ->
                        Text(
                            text = label.uppercase(
                                androidx.compose.ui.platform.LocalConfiguration
                                    .current.locales[0],
                            ),
                            style = HealthTrail.type.eyebrow,
                            color = colors.onBlue,
                            modifier = Modifier
                                .clip(Radius.pill)
                                .background(colors.onBlue.copy(alpha = PILL_ALPHA))
                                .padding(horizontal = Space.sm, vertical = Space.s)
                                // Capitals are for the eye. A reader gets the
                                // words as they were written, D183.
                                .semantics { contentDescription = label },
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    if (faces.isNotEmpty()) {
                        // **Overlapped, because they are one group**, measured
                        // off the drawing at two marks across 52dp. They are
                        // decorative here, and the names are in the sentence a
                        // reader hears.
                        //
                        // **Each one carries a ring in the block's own color,
                        // and the overlap eats the ring rather than the
                        // letters.** Without it the second circle sat directly
                        // on the first and cut its initials in half: the phone
                        // showed "MI" clipped to "M" beside "WO", which is two
                        // people rendered as one smudge. The ring is what makes
                        // a stack countable, and the overlap is measured from
                        // the outside of it so the text is never touched. Rule
                        // 21, and invisible in the source.
                        Row(horizontalArrangement = Arrangement.spacedBy(-FACE_OVERLAP)) {
                            faces.forEach { face ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.blue)
                                        .padding(FACE_RING),
                                ) {
                                    Avatar(name = face.name, hue = face.hue, size = FACE)
                                }
                            }
                        }
                    }
                }
            }

            Text(
                // bidi-ok: the caller isolates. An appointment's name is always
                // somebody's own words, and the empty line is always the app's.
                text = title,
                style = HealthTrail.type.displayM,
                color = colors.onBlue,
            )

            where?.let {
                // **The same ink as the title, one step down the ladder.**
                // `docs/V4.md` 2.1: the third level comes from size and weight
                // and never from a paler ink, which on a saturated block is also
                // the difference between readable and nearly gone.
                Text(
                    // bidi-ok: the caller isolates. A place is what somebody
                    // typed off an appointment letter.
                    text = it,
                    style = HealthTrail.type.bodyL,
                    color = colors.onBlue,
                )
            }
        }

        footer?.let { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalAlignment = Alignment.CenterVertically,
                content = row,
            )
        }
    }
}

/**
 * A quick add inside the block, as a mark alone.
 *
 * **The compact half of the foot's grammar.** The row has one wide thing that
 * says what it does in words and up to two of these beside it, which is what
 * keeps the block one answer rather than a stack of buttons. Owner, 2026-08-17.
 *
 * **The label is required and is the only thing naming it**, so a reader gets a
 * verb where the eye gets a symbol. Circular and 48dp, which is the target rule
 * 19 gates on, drawn on `card` because white inside this block means a door.
 */
@Composable
fun BlockIconAction(
    @DrawableRes mark: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    Box(
        modifier = modifier
            .size(Space.touchTarget)
            .clip(CircleShape)
            .background(colors.card)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Symbol(
            symbol = mark,
            contentDescription = null,
            tint = colors.blue,
            modifier = Modifier.size(Space.markInline),
        )
    }
}

/**
 * One quick way to add what is missing, inside a saturated block, in words.
 *
 * **The wide half of the foot's grammar**, used where there is no count to
 * offer as a door: it says what it does rather than leaving a bare mark to
 * carry the whole meaning.
 *
 * **White, because white inside the block is what a person can press.** A tonal
 * pill for an action would look exactly like the pill that does nothing, which
 * is the confusion rule 16 exists to prevent.
 */
@Composable
fun BlockAction(
    @DrawableRes mark: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors
    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            // The same floor the inset card keeps, and the same reason.
            .sizeIn(minHeight = Space.touchTarget)
            .padding(horizontal = Space.sm, vertical = Space.xs)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Symbol(
            symbol = mark,
            contentDescription = null,
            tint = colors.blue,
            modifier = Modifier.size(Space.markInline),
        )
        Text(
            // bidi-ok: the app's own name for what pressing this does.
            text = label,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
        )
    }
}

/** One person's mark on the block: who they are, and the circle they wear. */
data class Face(val name: String, val hue: TabHue)

/**
 * The one thing worth doing before what is next, as a white card inside the
 * block.
 *
 * **`m3v4-0` draws it as a count, a phrase and a way in**, and the count sits in
 * its own disc rather than inside the sentence, so the number reads at a glance
 * on a screen somebody is looking at while putting a coat on.
 *
 * **A door, and it says so twice.** The whole card is the target and the arrow
 * at its end draws what the press already does. Rule 16, and law 3: state in
 * more than one channel.
 *
 * **The count is not read out on its own.** A reader hears the whole card as one
 * sentence, because "two" and "questions ready to ask" are one fact split in two
 * for the eye.
 */
@Composable
fun InsetDoor(
    /** The number, in its own disc. */
    count: String,
    /** What the number is of. */
    label: String,
    /** What a reader hears, as one sentence. */
    description: String,
    /** The section the thing counted belongs to, which colors the disc. */
    hue: TabHue,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HealthTrail.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(colors.card)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = description }
            // **48dp and not a point less.** The owner, 2026-08-17: the cards
            // inside the hero "are too big. they don't look elegant". What was
            // making them big was the furniture, a 32dp disc and a 44dp circle
            // inside 8dp of padding, so the card measured about sixty. The
            // furniture came down and the target did not: 48 is Material's
            // minimum and rule 19's gate, and a beautiful control nobody with
            // shaky hands can hit is not a control. D192.
            .sizeIn(minHeight = Space.touchTarget)
            .padding(horizontal = Space.s, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(
            modifier = Modifier
                .size(DISC)
                .clip(CircleShape)
                .background(hue.wash),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // bidi-ok: a figure the app counted.
                text = count,
                style = HealthTrail.type.bodyL,
                color = hue.ink,
            )
        }
        Text(
            // bidi-ok: the app's own words for what it counted.
            text = label,
            style = HealthTrail.type.bodyL,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Symbol(
            symbol = Symbols.forward,
            contentDescription = null,
            tint = colors.blue,
            modifier = Modifier.size(Space.markInline),
        )
    }
}

/** White over the block, measured off `m3v4-0` at twenty-two percent. */
private const val PILL_ALPHA = 0.22f

/** A face on the block, measured off the drawing: two circles across 52dp. */
private val FACE = Space.xl

/**
 * How much of the circle behind is covered, so the pair reads as one group.
 *
 * **Measured from the outside of the ring**, so what the overlap covers is the
 * ring and not the letters under it. The drawing's two marks span 52dp; at 32dp
 * each plus two rings of two that is eight of overlap.
 */
private val FACE_OVERLAP = Space.s

/** The block's own color around each mark, so a stack of them stays countable. */
private val FACE_RING = Space.xs

/**
 * The count's disc.
 *
 * Measured off `m3v4-0` at 29.7dp and brought in to 24 when the owner said the
 * cards inside the hero were too big. The figure on it is `bodyL` rather than a
 * row title, so it still reads at a glance without setting the card's height.
 */
private val DISC = Space.l
