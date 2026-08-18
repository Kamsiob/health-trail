package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.Radius

/**
 * Search inside one list, sitting at the top of that list. #387, D196.
 *
 * **Written fresh on `OutlinedTextField` and the old file deleted.** What it
 * replaces was a `Row` with a clip, a background, a border, a hand animated
 * focus ring and a `BasicTextField` inside it, which is a description of
 * `OutlinedTextField` rather than a reason to build one. Material owns the
 * container, the focus treatment, the placeholder, the leading and trailing
 * slots and the label semantics now.
 *
 * **Its two glyphs were drawn on a `Canvas`.** A magnifier assembled from a
 * circle and a line, and a clear mark from two crossed lines, both with their
 * own stroke widths and their own mirroring arithmetic. They are
 * `Symbols.search` and `Symbols.close`, which are Google's own and already
 * mirror through the layout direction.
 *
 * **It is scoped, and the scope is the point.** This is not the app's search
 * screen and it never looks outside the list it sits on. Somebody standing in
 * the trail looking for the word "catheter" wants the trail, not eleven other
 * sections arriving to help. A field that quietly widens its own scope is the
 * app answering a question nobody asked.
 *
 * **The hint says how much it is searching**, which is the one number that
 * makes a search field feel like a tool rather than a formality. "Search 1,630
 * entries" tells the person both what the field does and how large the thing
 * under it has become, and it is the honest reason the field exists at all.
 *
 * **Nothing is submitted.** It filters as the person types and there is no
 * search button, because a field that needs a second tap to do the thing it
 * obviously does is a tap spent on the app's convenience.
 *
 * **When not to use it.** On a list that fits in a screenful or two, which is
 * the same rule the rail has. A search field over six rows is furniture, and on
 * a small list it costs a person more to read than to scan what is under it.
 *
 * @param hint what the field is for, already carrying the count of the scope,
 *   so it says what it is searching. It is also the field's label for a reader.
 */
@Composable
fun ScopedSearch(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        // **The hint is the placeholder and nothing else.** It was a `Text`
        // drawn behind the field and swapped out by hand on the first
        // keystroke, which is what a placeholder is, except that Material's
        // also stops being announced once there is a value in the field.
        placeholder = { Text(text = hint) },
        leadingIcon = {
            // Decorative, and it says so. The hint beside it names the field,
            // so a reader that announced the glyph too would say it twice.
            Icon(painter = painterResource(Symbols.search), contentDescription = null)
        },
        trailingIcon = {
            // **Only once there is something to clear.** An always-present
            // clear button on an empty field is a control that does nothing,
            // which rule 16 says reads as broken.
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    // The sentence sits on the node that takes the tap, and on
                    // one node, per `docs/TRAPS.md`.
                    modifier = Modifier.semantics { contentDescription = clearLabel },
                ) {
                    Icon(painter = painterResource(Symbols.close), contentDescription = null)
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            // **Search, not Next.** There is nothing after this field to move
            // to, and a Next key that goes nowhere is the keyboard promising
            // something the screen has not got. The list is already filtering
            // by then, so the key only closes the keyboard.
            imeAction = ImeAction.Search,
        ),
        // A pill, because the field sits on top of a list rather than inside a
        // form, and the shape is what says which of the two it is.
        shape = Radius.pill,
        colors = OutlinedTextFieldDefaults.colors(
            // **The canvas is `surfaceContainerLow` in this scheme**, so an
            // unfocused outline at Material's default `outline` is the only
            // thing separating the field from the page. It stays.
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .semantics { contentDescription = hint },
    )
}
