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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = type.bodyM,
            color = if (enabled) colors.ink2 else colors.ink3Text,
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
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                interactionSource = interaction,
                textStyle = type.bodyL.copy(color = if (enabled) colors.ink else colors.ink3Text),
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
                    // 48dp minimum touch target including the padding above.
                    .defaultMinSize(minHeight = 20.dp)
                    .then(if (fieldTestTag != null) Modifier.testTag(fieldTestTag) else Modifier)
                    .semantics { contentDescription = label },
            )

            if (value.isEmpty() && hint != null) {
                Text(
                    text = hint,
                    style = type.bodyL,
                    color = colors.ink3Text,
                )
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
