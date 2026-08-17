package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue

/**
 * The surfaces of the rebuilt interface. Written from scratch, #386.
 *
 * **Nothing in this package descends from the old components.** The owner,
 * after a pass that recolored them in place: "no old design language at all.
 * get rid of it so it doesn't influence. there's no reason to change it or
 * update it. you're building from scratch." [D178] said the same thing before
 * the work started and the previous pass drifted back into converting, which
 * is what `docs/V4.md` 3 calls half converted.
 *
 * **Every rule these obey is in `docs/V4.md` 2.1**, and every number in it was
 * measured off the approved drawings rather than judged.
 */

/**
 * A block: the container this interface is made of.
 *
 * **Flat and tonal on a quiet canvas.** Depth is `paper` against the block's
 * own color, never elevation, because the drawings carry no shadow anywhere and
 * two separations at once is what made the old screens feel busy.
 *
 * [tone] is the block's job rather than its color: [BlockTone.Quiet] for the
 * ordinary group, [BlockTone.Raised] for the one thing on a screen that has to
 * be found first, and the section tones for a block that belongs to a section.
 */
@Composable
fun Block(
    modifier: Modifier = Modifier,
    tone: BlockTone = BlockTone.Quiet,
    shape: Shape = Radius.cardLarge,
    padding: androidx.compose.ui.unit.Dp = Space.ml,
    /**
     * Which section's colors [BlockTone.Section] means, and nothing otherwise.
     *
     * **A section hue is identity, never state**, `docs/V4.md` 2.1, so it is
     * passed rather than chosen here: the block does not know which screen it
     * is on and must not guess.
     */
    hue: TabHue? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.container(hue))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(Space.s),
        content = content,
    )
}

/**
 * What a block is for, which decides its color.
 *
 * **A tone rather than a color at the call site.** A screen says what the block
 * does and the language decides how that looks, which is the whole reason the
 * old set drifted: sixty call sites each chose `card` or `sand` for themselves.
 */
enum class BlockTone {
    /** The ordinary group. Most blocks. */
    Quiet,

    /** A single fact worth raising: the decision date, the count that matters. */
    Gold,

    /** Resolved, done, arrived. */
    Leaf,

    /** The emergency card, an open incident. Never a measurement, rule 2. */
    Alert,

    /**
     * The one thing on a section's screen that belongs to that section.
     *
     * `m3v4-3` raises the person you call most into the care team's own wash.
     * **Identity, never state**: it says which part of the notebook this is,
     * and it never says anything about how the thing inside it is going.
     */
    Section,
    ;

    @Composable
    fun container(hue: TabHue? = null): Color = when (this) {
        Quiet -> HealthTrail.colors.sand
        Gold -> HealthTrail.colors.goldWash
        Leaf -> HealthTrail.colors.leafWash
        Alert -> HealthTrail.colors.alertWash
        // A section block with no section is the ordinary quiet one rather than
        // a crash: the tone is a request, and the hue is what answers it.
        Section -> hue?.wash ?: HealthTrail.colors.sand
    }

    /** The ink a label takes inside this block. */
    @Composable
    fun label(hue: TabHue? = null): Color = when (this) {
        Quiet -> HealthTrail.colors.ink2
        Gold -> HealthTrail.colors.goldInk
        Leaf -> HealthTrail.colors.leafInk
        Alert -> HealthTrail.colors.alertInk
        Section -> hue?.ink ?: HealthTrail.colors.ink2
    }
}

/**
 * The quiet line that names what follows.
 *
 * **Capitals only when the words are fixed and short**, which is what every
 * eyebrow in the drawings is: WEIGHT, PART OF, DECISION EXPECTED. A label the
 * person wrote keeps its own case and loses the tracking, because capitals cost
 * about fifteen percent of the width and 0.14em costs about fifty points on a
 * thirty character string, and the result was their own project name cut off.
 * D183.
 *
 * **Capitals are for the eye.** Compose has no text transform, so the node
 * carries the natural words for a reader.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HealthTrail.colors.ink2,
    fixed: Boolean = true,
    /**
     * What a reader hears instead of the words on screen.
     *
     * **For a label carrying a count.** "Equipment 3" is not what the screen
     * means, and `DESIGN.md` 12 asks that what is read aloud say the same thing
     * the screen says: the label passes the sentence, "Equipment, 3 things",
     * and the eye keeps the digit.
     */
    description: String? = null,
) {
    val shown = if (fixed) text.uppercase(LocalConfiguration.current.locales[0]) else text
    Text(
        text = shown,
        style = if (fixed) {
            HealthTrail.type.eyebrow
        } else {
            HealthTrail.type.eyebrow.copy(letterSpacing = TextUnit.Unspecified)
        },
        color = color,
        modifier = modifier.semantics { contentDescription = description ?: text },
    )
}

/**
 * The one thing a screen leads with, at display size.
 *
 * Rule 15 and law 1: a screen leads with something, and the jump from this to
 * the rows under it is the drawing's 2.3, not the 1.5 the app used to carry.
 */
@Composable
fun Lead(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HealthTrail.colors.ink,
) {
    // **Isolated, because a heading is usually a name.** A project the person
    // named, a person, a place: a Latin name inside an Arabic layout is pulled
    // into the sentence around it without this. D180 stopped verifying RTL for
    // version one and explicitly did not delete the marks, because they are
    // ordinary correct layout and rewriting them later would be expensive.
    Text(
        text = Bidi.isolate(text),
        style = HealthTrail.type.displayM,
        color = color,
        modifier = modifier,
    )
}

/**
 * A big number with its unit, as the drawings set one.
 *
 * `m3v4-0` puts the weight this way and `m3v4-2` the days remaining: the figure
 * at display size in the reading face, the unit beside it quiet. **Not the mono
 * face**, which D173 left to figures that line up in a column; a number a screen
 * leads with is a headline rather than a column.
 */
@Composable
fun BigNumber(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    color: Color = HealthTrail.colors.ink,
) {
    Box(modifier = modifier) {
        Text(
            // Isolated for the same reason a figure always is: a Latin number
            // keeps its own direction rather than joining the words beside it.
            text = Bidi.isolate(if (unit == null) value else "$value $unit"),
            style = HealthTrail.type.displayM,
            color = color,
        )
    }
}

/**
 * One fact, raised: a mark, the words naming it, and the sentence itself.
 *
 * **This is how `m3v4-5` draws where the paper is**, and it is the shape for
 * any single fact a screen exists to carry: a gold block, the mark at the
 * leading edge, a fixed capital label over the person's own words at reading
 * size. The words are the content, so they are set at [HealthTrail.type.bodyL]
 * rather than at a row's size: a row would put "signed and handed back to the
 * ward clerk" at the same weight as a date, and that sentence is what saves the
 * phone call six weeks later.
 *
 * **One of these per screen at most**, `docs/V4.md` 2.1. A second raised block
 * is a rainbow and gives the eye nowhere to land.
 */
@Composable
fun FactBlock(
    label: String?,
    text: String,
    modifier: Modifier = Modifier,
    tone: BlockTone = BlockTone.Gold,
    @DrawableRes mark: Int? = null,
    /**
     * Which section this block belongs to, where [tone] is
     * [BlockTone.Section].
     *
     * **The note a form opens with is this shape**: the section's wash, its own
     * mark, and the sentence setting the terms for everything under it. Two of
     * the six approved drawings carry one, and the block does not know which
     * screen it is on, so the hue is passed rather than guessed.
     */
    hue: TabHue? = null,
) {
    Block(modifier = modifier, tone = tone, hue = hue) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            mark?.let {
                // At the symbol's own 24dp, because this one is a mark on a
                // block rather than a glyph on a line of type.
                Symbol(symbol = it, contentDescription = null, tint = tone.label(hue))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                // bidi-ok: the label is fixed app copy naming what the block
                // holds, never the person's words. The sentence below it is
                // theirs and the caller isolates it.
                //
                // **Null where the sentence names itself**, which is every note
                // a form opens with: a label over one line saying the same
                // thing twice is furniture, not hierarchy.
                label?.let { Eyebrow(text = it, color = tone.label(hue)) }
                Body(
                    // bidi-ok: the caller isolates, because the sentence here is
                    // usually somebody's own words and only the caller knows.
                    text = text,
                    color = HealthTrail.colors.ink,
                    style = HealthTrail.type.bodyL,
                )
            }
        }
    }
}

/** The app's own type, so a v4 screen never reaches for Material's defaults. */
@Composable
fun Body(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HealthTrail.colors.ink2,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    /**
     * How much of somebody's own writing a preview shows.
     *
     * **Unbounded everywhere except a card that stands for a longer thing**,
     * because rule 11 forbids truncation: an ellipsis is only honest where the
     * whole text is one tap away and the card says so by being tappable.
     */
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    // bidi-ok: a caller passing a person's own words isolates them, because
    // only the caller knows whether this line is a sentence the app wrote or
    // something somebody typed. The three that must never be isolated, a field
    // being edited, a draft on its way to the database, and a filename, all
    // reach this the same way.
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier,
    )
}
