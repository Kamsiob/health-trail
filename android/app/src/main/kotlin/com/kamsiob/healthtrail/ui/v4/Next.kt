package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    /** When it is, already said the way somebody standing in a kitchen needs it. */
    whenLabel: String,
    /** The thing itself, in the person's own words. */
    title: String,
    /** What a reader hears for the appointment itself, as one sentence. */
    description: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where it is, when the person wrote it down. Absent is ordinary, rule 13. */
    where: String? = null,
    /** Whoever it is with, as their marks. Empty draws none. */
    faces: List<Face> = emptyList(),
    /**
     * The one thing worth doing before it, as a white card inside the block.
     *
     * Null when there is nothing, and the block is then simply the appointment.
     * A card offering nothing to do would be an empty frame, rule 11.
     */
    door: (@Composable () -> Unit)? = null,
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
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // **A pill of the block's own color lightened, not a second
                // hue.** Measured off the drawing at white over blue at
                // twenty-two percent, which reads as a quieter piece of the
                // same surface rather than as another thing with its own
                // meaning.
                Text(
                    text = whenLabel.uppercase(
                        androidx.compose.ui.platform.LocalConfiguration.current.locales[0],
                    ),
                    style = HealthTrail.type.eyebrow,
                    color = colors.onBlue,
                    modifier = Modifier
                        .clip(Radius.pill)
                        .background(colors.onBlue.copy(alpha = PILL_ALPHA))
                        .padding(horizontal = Space.sm, vertical = Space.s)
                        // Capitals are for the eye. A reader gets the words as
                        // they were written, D183.
                        .semantics { contentDescription = whenLabel },
                )
                Box(modifier = Modifier.weight(1f))
                if (faces.isNotEmpty()) {
                    // **Overlapped, because they are one group.** Measured off
                    // the drawing: two marks across 52dp, so 32dp circles with
                    // twelve of overlap. They are decorative here, and the names
                    // are in the sentence a reader hears.
                    Row(horizontalArrangement = Arrangement.spacedBy(-FACE_OVERLAP)) {
                        faces.forEach { face ->
                            Avatar(name = face.name, hue = face.hue, size = FACE)
                        }
                    }
                }
            }

            Text(
                // bidi-ok: the caller isolates. An appointment's name is always
                // somebody's own words.
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

        door?.invoke()
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
            .sizeIn(minHeight = Space.touchTarget)
            .padding(Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
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
                style = HealthTrail.type.rowTitle,
                color = hue.ink,
            )
        }
        Text(
            // bidi-ok: the app's own words for what it counted.
            text = label,
            style = HealthTrail.type.rowTitle,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(Space.markTile)
                .clip(CircleShape)
                .background(colors.blueWash),
            contentAlignment = Alignment.Center,
        ) {
            Symbol(symbol = Symbols.forward, contentDescription = null, tint = colors.blue)
        }
    }
}

/** White over the block, measured off `m3v4-0` at twenty-two percent. */
private const val PILL_ALPHA = 0.22f

/** A face on the block, measured off the drawing: two circles across 52dp. */
private val FACE = Space.xl

/**
 * How much of the circle behind is covered, so the pair reads as one group.
 *
 * The drawing's two marks span 52dp, which at 32dp each is twelve of overlap.
 */
private val FACE_OVERLAP = Space.sm

/** The count's disc, measured off `m3v4-0` at 29.7dp. */
private val DISC = Space.xl
