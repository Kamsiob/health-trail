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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
 * The label sits above the field rather than floating into it, because a
 * floating label disappears exactly when someone interrupted mid sentence needs
 * it most.
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
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = type.bodyM,
            color = if (enabled) colors.ink2 else colors.ink2,
        )

        Spacer(Modifier.height(Space.s))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.tile)
                .background(colors.sand)
                .then(
                    // The same focus treatment as every other focusable thing:
                    // a 2dp blue outline, offset 2dp. Accessibility floor,
                    // section 9.
                    if (focused) Modifier.border(2.dp, colors.blue, Radius.tile) else Modifier
                )
                .padding(horizontal = Space.m, vertical = 14.dp),
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

                    if (value.isEmpty() && hint != null) {
                        Text(
                            text = hint,
                            style = type.bodyL,
                            color = colors.ink2,
                        )
                    }
                }

                if (trailing != null) {
                    Spacer(Modifier.width(Space.s))
                    trailing()
                }
            }
        }

        if (note != null) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = note,
                style = type.bodyS,
                color = colors.ink2,
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

                    if (value.isEmpty() && hint != null) {
                        // **`ink2`, never `ink3`.** D92: this app has two text
                        // levels and `ink3` is non-text only, at 2.37:1 on
                        // paper. A hint is text somebody has to read to know
                        // what the row wants. `check_ink3_is_not_text.py`
                        // caught this one the same minute it was written.
                        Text(
                            text = hint,
                            style = type.bodyL,
                            color = colors.ink2,
                        )
                    }
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
    }
}
