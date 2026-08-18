package com.kamsiob.healthtrail.ui.v4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * A group of fields under the quiet label that names it. #386.
 *
 * **The label was a gray sentence and it got lost.** The owner, on the setup
 * screen, 2026-08-17: "the text just kind of gets lost and it seems like
 * clutter." It was body text in the secondary ink, indented, sitting directly
 * above a block of fields whose own floating labels are body text in the
 * secondary ink: three sizes of the same thing stacked, so nothing said which
 * was the heading.
 *
 * **So it is the eyebrow**, which is what `docs/V4.md` 2.1 gives a label that
 * names what follows: small, tracked, capitals where the words are the app's
 * own and short. Different in size, weight, spacing and case from everything
 * inside the block, which is what makes it read as a heading rather than as
 * another line of the form. Rule 15: hierarchy before decoration.
 *
 * **The fields sit on the canvas, not in a block**, which is what `m3v4-4`
 * draws: a field is already a container, with its own notched outline and its
 * own floating label, so putting it inside a second container is two edges
 * around one thing. That is the clutter the owner named. The eyebrow and the
 * air between groups are what say where one group ends. D183.
 */
@Composable
fun FieldBlock(
    label: String,
    modifier: Modifier = Modifier,
    /** The ink the label takes, where a form belongs to a section. */
    labelColor: Color = HealthTrail.colors.ink2,
    /**
     * One line under the label, saying what the group is for.
     *
     * **This is where "none of this is needed to save" goes**, which is what
     * the old disclosure said as it hid the fields. Rule 13: the sentence is
     * what makes a group an invitation rather than a queue of things somebody
     * has not done yet, and it stays whether or not the fields are filled in.
     */
    aside: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        Eyebrow(text = label, color = labelColor)
        // bidi-ok: the app's own sentence about what this group is for.
        aside?.let { Body(text = it, style = HealthTrail.type.support) }
        content()
    }
}

/**
 * A field, on Material's own outlined text field. #386.
 *
 * **The label is persistent and carries the meaning**, in the notched outline,
 * `docs/V4.md` 2.1. It is there before somebody types, while they type, and
 * after: a label that leaves when the field fills is the pattern that fails
 * exactly when an interrupted person looks back at what they were doing.
 *
 * **The hint is supporting text under the field, never inside it.** Nielsen
 * Norman Group's research on form design is unambiguous, and two of the seven
 * problems they list are what the owner saw on 2026-08-17: people read
 * placeholder text as content already filled in and skip the field, and an empty
 * field draws the eye better than one that looks full. The others are worse: a
 * placeholder vanishes the moment somebody types, so it is gone when it is
 * needed most, and it comes back only by deleting what they wrote. D189.
 *
 * **One supporting line, never two.** A field with an example and a promise and
 * a warning under it is three sentences competing to be read at the moment
 * somebody is trying to write one. Where a warning applies, it takes the line
 * from the example: the caller chooses, because only the caller knows.
 *
 * **It never turns red and never blocks.** Nothing in this app is required, so a
 * field cannot be wrong. Rule 13.
 *
 * **Material's own rather than hand drawn**, which is the rebuild's rule for
 * anything Material already does: the old one drew its own notch, measured its
 * own label, and painted the page's color over the outline to make a gap for the
 * words. That is a lot of geometry to maintain in order to arrive at the control
 * the platform ships.
 */
@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** The one line under the field: an example, or the warning that replaces it. */
    support: String? = null,
    /** A tag on the editable line itself, which is what a test types into. */
    fieldTestTag: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /** A passphrase, and the only thing in this app that is ever hidden. */
    masked: Boolean = false,
    /** What sits at the end of the line inside the field: dictation, a unit. */
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = HealthTrail.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(fieldTestTag?.let { Modifier.testTag(it) } ?: Modifier),
        enabled = enabled,
        // bidi-ok: the label is the app's own words for what the field asks.
        label = { Text(text = label, style = HealthTrail.type.bodyS) },
        textStyle = HealthTrail.type.bodyL,
        trailingIcon = trailing,
        supportingText = support?.let {
            {
                // bidi-ok: the app's own example or warning about this field.
                Text(text = it, style = HealthTrail.type.support)
            }
        },
        visualTransformation = if (masked) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = singleLine,
        shape = Radius.cardLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            disabledTextColor = colors.ink2,
            cursorColor = colors.blue,
            // **The canvas shows through.** A field sits on the page rather
            // than in a block, `m3v4-4`, so a container color here would be the
            // second edge around one thing that D183 took out of the forms.
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            // The outline at rest is the non-text ink, which is what says where
            // to write; focused it is the same 2dp blue every focusable thing
            // in this app takes. `DESIGN.md` 12.
            focusedBorderColor = colors.blue,
            unfocusedBorderColor = colors.ink3,
            disabledBorderColor = colors.hairline,
            focusedLabelColor = colors.blue,
            unfocusedLabelColor = colors.ink2,
            disabledLabelColor = colors.ink2,
            focusedSupportingTextColor = colors.ink2,
            unfocusedSupportingTextColor = colors.ink2,
            disabledSupportingTextColor = colors.ink2,
        ),
    )
}

/**
 * A field and its dictation control, which is the pairing every text area uses.
 *
 * Exists so that "every text area offers dictation" is one call rather than a
 * habit twelve screens have to remember, and so the spacing between the two is
 * decided once.
 *
 * **Spoken text is appended with a space**, so half typed and half spoken is a
 * sentence rather than two words jammed together.
 */
@Composable
fun DictatableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    support: String? = null,
    fieldTestTag: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    /**
     * Whether speaking is the point of this screen rather than an alternative
     * to typing on it. Law 3, on the note stage of capture.
     */
    prominentVoice: Boolean = false,
) {
    val append: (String) -> Unit = { spoken ->
        onValueChange(if (value.isBlank()) spoken else "${value.trimEnd()} $spoken")
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
    ) {
        Field(
            label = label,
            value = value,
            onValueChange = onValueChange,
            support = support,
            fieldTestTag = fieldTestTag,
            enabled = enabled,
            singleLine = singleLine,
            imeAction = imeAction,
            // **Inside the field**, unless this is the one screen where
            // speaking is the point rather than an alternative.
            trailing = if (prominentVoice) {
                null
            } else {
                { DictateAction(inField = true, enabled = enabled, onText = append) }
            },
        )

        if (prominentVoice) {
            DictateAction(prominent = true, enabled = enabled, onText = append)
        }
    }
}

/**
 * The rest of a form, behind one control nobody is required to touch. `m3v4-4`.
 *
 * **This comes back, and D185 said what would bring it.** That entry deleted
 * the accordion because "the drawing does not draw one", which was checked
 * against `m3v4-3`, the care team, where it is true. **`m3v4-4` draws one**: a
 * sand pill at the foot of the medication form carrying a plus, "What it is
 * for, and any note", and a chevron. D185's own revisit clause reads "a mockup
 * that draws a fold. Then it is measured and written from scratch", so that is
 * what this is. The list folds stay deleted: no drawing shows those, and
 * nothing behind them was ever a question the form was asking.
 *
 * **Measured off the PNG**: 60dp tall, the full content width, a 14dp corner,
 * the block's own sand, the plus and the chevron in the quiet ink.
 *
 * **It opens and stays open.** There is no close control, because somebody who
 * opened it wanted what is inside and taking it away again would be the form
 * arguing with them. Leaving the screen resets it, which is right: the next
 * entry starts from the short form.
 *
 * **It never carries a count and never says how much is left.** "Add more" is
 * an offer; "3 more fields" would be a measure of how incomplete the entry is,
 * which rule 13 rules out.
 *
 * **It never hides something already written down**, which is what [startOpen]
 * is for: a form correcting a saved record passes true, because folding away a
 * note somebody typed last week behind a control that says "Add more" is the
 * app hiding their own words.
 */
@Composable
fun Fold(
    label: String,
    modifier: Modifier = Modifier,
    startOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HealthTrail.colors
    // Saveable, so a rotation or a theme change does not fold the form back up
    // under somebody part way through filling it in.
    var open by rememberSaveable(startOpen) { mutableStateOf(startOpen) }

    if (open) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.withinGroup),
            content = content,
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = FOLD_HEIGHT)
            .clip(Radius.fold)
            .background(colors.sand)
            .clickable(role = Role.Button, onClickLabel = label) { open = true }
            .padding(horizontal = Space.ml, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Icon(
            painter = painterResource(Symbols.add),
            contentDescription = null,
            tint = colors.ink2,
        )
        // bidi-ok: the app's own words for what is behind the control.
        Body(text = label, style = HealthTrail.type.bodyL, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(Symbols.expand),
            contentDescription = null,
            tint = colors.ink2,
        )
    }
}

/** 59.8dp on `m3v4-4`, which is the touch target plus the air around the words. */
private val FOLD_HEIGHT = 60.dp
