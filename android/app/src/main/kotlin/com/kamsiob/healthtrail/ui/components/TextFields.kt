package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A text field, per DESIGN.md section 5.9.
 *
 * **There is no error state and that is deliberate.** Every capture field in
 * this app is optional and partial is a finished state, so a field cannot be
 * wrong. It never turns red, never shows a warning glyph, and never blocks
 * saving. Where a value genuinely cannot be interpreted, the app keeps what the
 * person typed and says what it could not read, in [note], without discarding
 * the input and without scolding.
 *
 * **The field is outlined rather than filled**, which is what the approved v4
 * mockup draws. `docs/screenshots/m3v4-4-light.png`, D172.
 *
 * **It was a sand slab**, and that shape is most of what "#381: forms are a
 * wall of label and box" was pointing at. Every field was a gray block; a
 * column of them is a column of blocks, and the paper the rest of the app is
 * drawn on stopped anywhere a form began. An outline lets the paper through, so
 * a form reads as writing on the page rather than as boxes stacked on it. **One
 * component, 64 sites, every form in the app.**
 *
 * **The label sits in a gap in the top border**, which is what the mockup
 * draws. D177, revised: the owner's answer to the label being a sentence was
 * "just the dose", so the copy shortened and the notch is the shape.
 *
 * **The nuance moved to the hint, where it already was.** "The dose, as you
 * were told it" became "The dose", and the hint under the caret still says
 * "However it was said. 50 mcg, half a tablet, whatever fits." Eleven labels
 * shortened that way and not one of them lost a thing, because every one had a
 * hint carrying the same permission.
 *
 * **The notch falls back to a label above the field when one line will not
 * hold it**, and that is what makes it safe rather than lucky. Copy is not a
 * guarantee: a longer language, a font scale of 2.0, or one careless catalog
 * edit puts the label onto a second line, and a two line label punches a two
 * line hole through the outline. So the label reports its own line count and
 * the layout answers. Nothing truncates, at any size, in any language, which
 * is rule 11 and the reason the first attempt at this was taken back out.
 *
 * **The notch is measured rather than guessed.** How far the label rises is
 * half its own height, and its height is a function of the font scale.
 * Measuring it is the only version that holds at 2.0.
 *
 * The old reason for a label above the field is not lost either: it said a
 * floating label "disappears exactly when someone interrupted mid sentence
 * needs it most", which is true of the Material 1 filled field whose label
 * dissolves into the placeholder. This one is in the border at rest, while
 * typing, and when the field is full.
 *
 * @param labelBackground what the notch punches through to. Defaults to the
 *   paper every form in this app is drawn on. **A caller putting a field on a
 *   card has to say so**, or the gap in the outline is the wrong color, which
 *   is the one way this component can look broken.
 */
@Composable
fun HealthTrailTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    note: String? = null,
    /**
     * Applied to the editable field itself rather than to the wrapping column.
     *
     * A tag on the wrapper looks correct and is useless: the node it selects is
     * a layout, not something that can take focus, so text input against it
     * fails with "RequestFocus is not defined". Passing it here puts it where
     * the caret is.
     */
    fieldTestTag: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /**
     * Whether the characters are hidden as they are typed.
     *
     * **[KeyboardType.Password] does not do this.** It asks for a keyboard
     * without autocorrect and nothing more, so a field set that way and nothing
     * else renders the secret in full on screen, which is how the export
     * passphrase came to sit in the clear on the one screen where it matters.
     * Masking is a visual transformation and has to be asked for separately.
     */
    masked: Boolean = false,
    /**
     * A control drawn inside the field, at the end edge.
     *
     * **This is where the microphone goes**, and until 2026-08-12 it was an
     * outlined pill reading "Speak it" under the field. The owner's words on
     * #361 were that a microphone belongs in the text entry field, which is
     * where every other application on the phone puts it and where somebody
     * looks for it without being told. A separate control under every text area
     * is a second row of furniture per field, and on a form with four fields it
     * is four of them.
     *
     * **The field reserves room for it rather than drawing it over the words.**
     */
    trailing: (@Composable () -> Unit)? = null,
    labelBackground: Color = HealthTrail.colors.paper,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    // **The label decides where it goes by measuring itself.** One line rides
    // the border; two or more sit above the field, because a notch is a gap in
    // a line and a gap cannot be two lines tall. Starts true so the first
    // frame, before any measurement exists, is the safe arrangement rather
    // than a label overlapping an outline.
    var labelWraps by remember { mutableStateOf(true) }
    var labelHeight by remember { mutableIntStateOf(0) }
    val labelRise = with(LocalDensity.current) { (labelHeight / 2).toDp() }

    // **One label, composed once, always at the same width.** Two copies at two
    // widths is an infinite recomposition: the narrow one wraps and asks to go
    // above the field, the wide one fits and asks to come back, forever. So the
    // insets that make room for the notch are applied in both arrangements,
    // whether or not the label is sitting on the line.
    val labelText: @Composable (Modifier) -> Unit = { mod ->
        Text(
            text = label,
            style = type.bodyS,
            color = if (focused) colors.blue else colors.ink2,
            onTextLayout = { result ->
                labelWraps = result.lineCount > 1
                labelHeight = result.size.height
            },
            // **Left in the semantics tree, and the silencing that was here is
            // reverted.** It was silenced on the reasoning that the field
            // already carries this string as its content description, so a
            // reader would say it twice. That may well be true, and it was
            // reasoning rather than something anybody heard: rule 19 says the
            // accessibility gate clears with the reader actually on, never by
            // reading code. It also took the words off the screen as far as
            // every test and every tool is concerned, which is what
            // `docs/TRAPS.md` means by asserting on words. Five tests found it
            // in one run. The double announcement is a real question and it
            // gets answered with TalkBack on, not here.
            modifier = mod
                .padding(start = Space.sm)
                .padding(horizontal = Space.xs),
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (labelWraps) {
            labelText(Modifier)
            Spacer(Modifier.height(Space.s))
        }

        Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Room for the label to sit on the line, and none when the
                // label is above the field instead.
                .padding(top = if (labelWraps) Space.none else labelRise)
                .clip(Radius.cardLarge)
                .border(
                    // The same focus treatment as every other focusable thing:
                    // a 2dp blue outline. Accessibility floor, section 9. At
                    // rest it is the non-text hairline rather than nothing,
                    // because an outline is what says where to write.
                    width = if (focused) Space.focusRing else Space.hairlineWidth,
                    color = if (focused) colors.blue else colors.ink3,
                    shape = Radius.cardLarge,
                )
                .padding(horizontal = Space.m, vertical = Space.fieldVertical),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        interactionSource = interaction,
                        textStyle = type.bodyL.copy(
                            color = if (enabled) colors.ink else colors.ink2,
                        ),
                        cursorBrush = SolidColor(colors.blue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction,
                        ),
                        visualTransformation = if (masked) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // 48dp minimum touch target including the padding
                            // above.
                            .defaultMinSize(minHeight = 20.dp)
                            .then(
                                if (fieldTestTag != null) {
                                    Modifier.testTag(fieldTestTag)
                                } else {
                                    Modifier
                                },
                            )
                            .semantics { contentDescription = label },
                    )


                }

                if (trailing != null) {
                    Spacer(Modifier.width(Space.s))
                    trailing()
                }
            }
        }

            // **Drawn after the box so it paints over the line**, carrying the
            // page's own color so the outline stops on either side of the
            // words rather than running through them. Composed only in the
            // arrangement it belongs to, so there is one measurement and no
            // argument between two copies about whether it fits.
            if (!labelWraps) {
                labelText(Modifier.background(labelBackground))
            }
        }

        // **The hint lives under the field, not inside it.** Nielsen Norman
        // Group's research on form design is unambiguous: placeholders inside
        // fields are harmful, and hints belong "persistent and placed outside
        // of the field". Two of the seven problems they list are exactly what
        // the owner saw, 2026-08-17: people read placeholder text as content
        // already filled in and skip the field, and an empty field draws the
        // eye better than one that looks full. The others are worse: the hint
        // vanishes the moment somebody types, so it is gone when it is needed
        // most, and it comes back only by deleting what they wrote.
        //
        // **Material says the same thing from the other side.** The label
        // carries the meaning and stays; supporting text under the field gives
        // context about the input and is persistent. So the field is empty when
        // it is empty, which is the strongest signal there is that it is a
        // place to type, and the example is still there while they type.
        //
        // D189, and it replaced an italic placeholder that was an improvement
        // to the wrong thing.
        if (hint != null) {
            Text(
                text = hint,
                // **Material's supporting text, exactly**: its own size, one
                // step below any prose beside it, four points under the field
                // and aligned with the text inside the field rather than with
                // the screen margin. The owner, 2026-08-17: "it just reads like
                // it's part of the sentence below it", and it did, because it
                // was the same size, the same ink and the same left edge as the
                // paragraph under it. Size, proximity and alignment are the
                // three things that say "this belongs to the control above".
                style = type.support,
                color = colors.ink2,
                modifier = Modifier.padding(top = Space.xs, start = Space.m),
            )
        }

        if (note != null) {
            Text(
                text = note,
                style = type.support,
                color = colors.ink2,
                modifier = Modifier.padding(top = Space.xs, start = Space.m),
            )
        }
    }
}

/**
 * A field as a row in a group, rather than a labeled slab of its own.
 *
 * **This is the answer to "it still looks like a data entry app".** The owner's
 * words on #361, twice, and the second time with the rest of the app named
 * alongside the forms. A screen built from [HealthTrailTextField] is a stack of
 * gray labels over filled boxes with gaps between them, which is what every
 * form on every platform looked like in 2010 and what a person recognizes
 * instantly as data entry. Four of them down one screen is four slabs.
 *
 * **One card, rows, hairlines, no boxes.** The same [GroupedSurface] the rest of
 * the app is made of, so a form reads as a card of facts rather than as a queue
 * of inputs, and it matches the screens that display those same facts. Rule 22:
 * the component comes from the shape of the content, and a name, a role and a
 * number are rows.
 *
 * **The label stays visible while somebody types**, above the value in the same
 * row. A floating label that leaves when the field fills is the pattern that
 * fails exactly when an interrupted person looks back at what they were doing,
 * which 5.9 already ruled out.
 *
 * **Focus is the row's own rule turning blue**, rather than a 2dp box drawn
 * around a slab. It is the same idea as the field's focus ring, at the weight a
 * row can carry, and it survives grayscale because the line also thickens.
 *
 * **The row never grows a border, never turns red, and never blocks.** Nothing
 * in this app is required, so a field cannot be wrong.
 */
@Composable
fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    fieldTestTag: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /** Drawn at the end edge of the row. The microphone, where there is one. */
    trailing: (@Composable () -> Unit)? = null,
    /** False on the last row of a group, where the group's own edge ends it. */
    divider: Boolean = true,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.cardPadding, vertical = Space.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = type.bodyS,
                    color = if (focused) colors.blueDeep else colors.ink2,
                )

                Spacer(Modifier.height(Space.xs))

                Box {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        interactionSource = interaction,
                        textStyle = type.bodyL.copy(
                            color = if (enabled) colors.ink else colors.ink2,
                        ),
                        cursorBrush = SolidColor(colors.blue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Space.l)
                            .then(
                                if (fieldTestTag != null) {
                                    Modifier.testTag(fieldTestTag)
                                } else {
                                    Modifier
                                },
                            )
                            .semantics { contentDescription = label },
                    )


                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(Space.s))
                trailing()
            }
        }

        // **The rule under the row is the focus state as well as the divider**,
        // so a focused row is marked without anything moving and without the
        // group gaining a second treatment. The last row has no rule of its own
        // and takes the group's edge, so it grows one only while it is focused.
        if (focused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.cardPadding)
                    .height(Space.focusRule)
                    .background(colors.blue),
            )
        } else if (divider) {
            Hairline(inset = Space.cardPadding, end = Space.cardPadding)
        }

        // The same as the field above: the hint is supporting text under the
        // row rather than a placeholder inside it. D189.
        if (hint != null) {
            Text(
                text = hint,
                style = type.support,
                color = colors.ink2,
                modifier = Modifier.padding(
                    start = Space.cardPadding,
                    end = Space.cardPadding,
                    top = Space.xs,
                ),
            )
        }
    }
}
