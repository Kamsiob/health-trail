package com.kamsiob.healthtrail.ui.v4

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.semantics.stateDescription
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.components.Symbol
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

/**
 * The row this interface scans, written from scratch. #386.
 *
 * **One shape, measured off `m3v4-1`**: a 44dp squircle carrying a Material
 * Symbol in the section's wash, a bold title, a quiet line under it, and a mark
 * at the end when the row is a door. The tiles sit on a 64dp pitch, which is
 * 13dp of air above and below the content, and that pitch is the difference the
 * owner named as the drawing breathing. D183.
 *
 * **The whole row is one stop for a reader.** A row is a name, a state and a
 * date, and it is one thing: fifty rows at three stops each is a hundred and
 * fifty swipes down one list.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    support: String? = null,
    /**
     * A tag on the support line itself, for a caller whose second line is a
     * fact something asserts on.
     *
     * The notebook's counts are the case: they are what its tests read, and a
     * tag on the whole row would make the assertion pass on the title.
     */
    supportTestTag: String? = null,
    @DrawableRes mark: Int? = null,
    markTint: Color = HealthTrail.colors.ink2,
    markWash: Color = HealthTrail.colors.card,
    /**
     * What stands where the mark would, when the mark is not a symbol.
     *
     * A person's row carries their initials rather than a glyph, and initials
     * are a mark for a person the way a symbol is a mark for a section. Passing
     * this instead of [mark] keeps one row rather than growing a second one.
     */
    leading: (@Composable () -> Unit)? = null,
    /**
     * A value the row is scanned by: a dose, a date, an amount.
     *
     * **Weighted with `fill = false`, so it cannot take the whole row and a
     * short one still sits at its natural width.** An unweighted value measures
     * at whatever it wants and leaves the title its minimum: a dose reading
     * "500 mg, twice a day" left "Levothyroxine" rendering one letter per line.
     *
     * **Not the mono face**, and that is measured off the drawings rather than
     * argued: `m3v4-0`, `m3v4-1`, `m3v4-3` and `m3v4-5` use mono for exactly
     * two things, the date eyebrow and the time pill, both of them tracked
     * capitals. **No row in any drawing sets its value in a typewriter.** A
     * dose and a date read as the words they are, at reading size, and the
     * primary ink is what separates a value from the quiet line above it.
     */
    value: String? = null,
    /**
     * Whether the value starts level with the title rather than centering
     * against the whole row.
     *
     * **Opt in, and centered stays the default on purpose.** For the ordinary
     * row, one line of title over one line of support, centering is right and
     * top aligning would lift the value off the row. **It stops being right the
     * moment the title wraps**: a bill called "Monthly room and board" runs to
     * two lines with its date under it, and the amount sat level with the
     * second line, beside the wrap rather than beside the name. Seen on the
     * phone, rule 21, and carried forward here because the row it was found on
     * is one of these now.
     */
    valueAtTop: Boolean = false,
    /**
     * Where the value sits, when the row is not left to decide for itself.
     *
     * **Null lets the row decide by length**, which is right for a list whose
     * values are all the same shape. **A list whose values vary passes the same
     * answer for every row**, because half a column at the end and half of it
     * under the title is a column that does not line up, and lining up is the
     * whole reason a value is set in the mono face. The medications are the
     * case: "5 mg, evenings" and "50 mcg, mornings, empty stomach" are the same
     * kind of fact at two lengths.
     */
    valueBelow: Boolean? = null,
    /**
     * The one thing to do about this row, as a mark at its end.
     *
     * **Unweighted, and that is the difference from [value].** A weighted slot
     * reserves its share whether or not the mark needs it, which put a 48dp
     * circle in the middle of the row with the title squeezed into the half
     * beside it. Seen on the phone, rule 21.
     */
    trailing: (@Composable () -> Unit)? = null,
    isDoor: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .sizeIn(minHeight = Space.touchTarget)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
                },
            )
            .padding(horizontal = Space.ml, vertical = Space.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (leading != null) {
            leading()
        } else if (mark != null) {
            Box(
                modifier = Modifier
                    .size(MARK_TILE)
                    .clip(Radius.iconTile)
                    .background(markWash),
                contentAlignment = Alignment.Center,
            ) {
                Symbol(symbol = mark, contentDescription = null, tint = markTint)
            }
        }
        // **A long value goes under the title rather than beside it.**
        // Measured on the phone: an appointment's "August 18, 2026 at 10:15 AM"
        // and a reading's "131.2 lb · May 8, 2026" are sentences rather than
        // values, and at the end of the row they took half its width and broke
        // mid-phrase, with the title wrapping around what was left. The slot at
        // the end is for something short enough to scan down a column, which is
        // what a dose and a bare date are; anything longer is a line of its own.
        // Rule 20: the row absorbs this so no screen has to know.
        val shown = value?.takeIf { it.isNotBlank() }
        // **Measured without the isolation marks.** `Bidi.isolate` wraps a
        // value in two invisible characters, so counting them would move the
        // bound by two for every value the app isolates and by nothing for the
        // ones it does not, which is a threshold that is different on different
        // rows for a reason nobody could see.
        val below = shown != null &&
            (valueBelow ?: (shown.count { it !in BIDI_MARKS } > VALUE_INLINE_MAX))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HealthTrail.type.rowTitle,
                color = HealthTrail.colors.ink,
            )
            if (!support.isNullOrBlank()) {
                Text(
                    text = support,
                    style = HealthTrail.type.bodyM,
                    color = HealthTrail.colors.ink2,
                    modifier = if (supportTestTag == null) {
                        Modifier
                    } else {
                        Modifier.testTag(supportTestTag)
                    },
                )
            }
            if (below) {
                Text(
                    text = shown,
                    style = HealthTrail.type.bodyM,
                    color = HealthTrail.colors.ink,
                )
            }
        }
        shown?.takeIf { !below }?.let {
            Text(
                text = it,
                style = HealthTrail.type.bodyM,
                color = HealthTrail.colors.ink,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(if (valueAtTop) Modifier.align(Alignment.Top) else Modifier),
            )
        }
        trailing?.invoke()
        if (isDoor && trailing == null) {
            Symbol(
                symbol = com.kamsiob.healthtrail.ui.components.Symbols.forward,
                contentDescription = null,
                tint = HealthTrail.colors.ink3,
            )
        }
    }
}

/**
 * The line between two rows inside a block.
 *
 * **Inset past the mark**, so it separates the words rather than cutting across
 * the tile, which is how `m3v4-1` draws it. Where a block holds one row there is
 * no line at all: the block's own edge ends it.
 */
@Composable
fun RowDivider(modifier: Modifier = Modifier, inset: Boolean = true) {
    HorizontalDivider(
        modifier = modifier.padding(start = if (inset) MARK_TILE + Space.sm else Space.none),
        color = HealthTrail.colors.hairline,
    )
}

/**
 * How long a value may be and still sit at the end of a row.
 *
 * **Measured rather than chosen**: "500 mg" and "6 readings" scan down a column
 * at the end of a row; "August 18, 2026 at 10:15 AM" does not, and on the phone
 * it took half the row and broke mid-phrase. Sixteen characters is the widest
 * thing in the app that still reads as a value rather than as a sentence.
 */
private const val VALUE_INLINE_MAX = 16

/** The isolation marks, which are invisible and must not count as width. */
private val BIDI_MARKS = setOf('\u2066', '\u2067', '\u2068', '\u2069')

/** The squircle a row's mark sits in, measured off `m3v4-1`. */
private val MARK_TILE = Space.markTile

/**
 * The door into search, as `m3v4-1` draws it directly under the title.
 *
 * **A tonal pill with the mark and the invitation**, not a field. Tapping it
 * opens the search screen, which is where typing happens: a field that looks
 * live but is not is the thing law 2 calls a costume lying about what it does.
 */
@Composable
fun SearchDoor(
    label: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .clip(Radius.pill)
            .background(HealthTrail.colors.sand)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onOpen)
            .padding(horizontal = Space.ml, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Symbol(
            symbol = com.kamsiob.healthtrail.ui.components.Symbols.search,
            contentDescription = null,
            tint = HealthTrail.colors.ink2,
        )
        Text(
            text = label,
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
    }
}

/**
 * One answer to a question that has two or three of them. #386.
 *
 * **A row rather than a chip**, because each option carries a line explaining
 * what it does, and a chip that needs a sentence under it is a row. Three
 * separate blocks for one question would be three answers pretending to be
 * three subjects: it is one question, so it is one block.
 *
 * **The chosen one announces as chosen**, through `selected` in the semantics
 * rather than only through the mark. Somebody who cannot see the mark gets the
 * same information, which is the difference between a label and a state.
 * `DESIGN.md` 12.
 */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    /**
     * A mark for the choice itself, where the choices differ by more than their
     * words: a thread's color, a route swatch.
     *
     * The same slot [ListRow] carries, and it is here so that a picker whose
     * options are colored does not need a second row to say so.
     */
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = HealthTrail.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = Space.touchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.ml, vertical = Space.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = HealthTrail.type.rowTitle,
                color = colors.ink,
            )
            if (!detail.isNullOrBlank()) {
                // bidi-ok: a choice's own explanation is the app's sentence
                // about what the choice does, never anything somebody typed.
                Text(text = detail, style = HealthTrail.type.bodyM, color = colors.ink2)
            }
        }
        // **The mark only where it is chosen**, and never a control of its own:
        // the whole row is the target, so a radio button beside it would be a
        // second thing to press that does the same job.
        if (selected) {
            Symbol(
                symbol = com.kamsiob.healthtrail.ui.components.Symbols.check,
                contentDescription = null,
                tint = colors.blue,
            )
        }
    }
}

/**
 * A row that is a switch, which is the one control this app asks a yes or no
 * with. #386, and the last shape `ui/v4` was missing.
 *
 * **The whole row is the target**, not the switch beside it: a 52dp track at
 * the far end of a row is a small thing to hit with a thumb while holding a
 * phone in the other hand, and rule 16 says everything the person touches
 * responds. The switch draws the state and the row takes the press.
 *
 * **It says on or off in words for a reader**, because a switch that only
 * announces "switch" has told somebody who cannot see it half of what it knows.
 *
 * **Material's own switch, themed.** `docs/V4.md` 2.1 and the replace table:
 * this app drew its own track and thumb once, and the platform's is the one
 * people already know.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    support: String? = null,
    @DrawableRes mark: Int? = null,
    /** Which section this question belongs to, drawn as the row's own wash. */
    hue: TabHue? = null,
) {
    val colors = HealthTrail.colors
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.cardLarge)
            .background(hue?.wash ?: colors.sand)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics {
                stateDescription = if (checked) strings["state.on"] else strings["state.off"]
            }
            .sizeIn(minHeight = Space.touchTarget)
            .padding(horizontal = Space.ml, vertical = Space.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        // **The mark sits in a circle of its own**, which is how `m3v4-4` draws
        // it: a filled disc in the section's base with the glyph knocked out of
        // it, not a bare glyph beside the words. Measured off the PNG at 44dp.
        mark?.let {
            Box(
                modifier = Modifier
                    .size(Space.markTile)
                    .clip(CircleShape)
                    .background(hue?.base?.copy(alpha = MARK_DISC_ALPHA) ?: colors.card),
                contentAlignment = Alignment.Center,
            ) {
                Symbol(
                    symbol = it,
                    contentDescription = null,
                    tint = hue?.ink ?: colors.ink2,
                    modifier = Modifier.size(Space.markInline),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            // bidi-ok: the app's own question.
            Text(text = title, style = HealthTrail.type.rowTitle, color = colors.ink)
            if (!support.isNullOrBlank()) {
                // bidi-ok: the app's own sentence about what answering does.
                Text(text = support, style = HealthTrail.type.bodyM, color = colors.ink2)
            }
        }
        // **Null `onCheckedChange`, because the row is the control.** A switch
        // with its own handler is a second target inside the first, and a
        // reader would meet two things that do one job.
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.blue,
                checkedThumbColor = colors.paper,
                checkedBorderColor = colors.blue,
                uncheckedTrackColor = colors.card,
                uncheckedThumbColor = colors.paper,
                uncheckedBorderColor = colors.ink3,
            ),
        )
    }
}

/**
 * How much of the section's base a switch row's disc takes.
 *
 * `m3v4-4` sets the mark's circle between the row's wash and the section's full
 * color, so the glyph reads without the disc becoming a second filled thing on
 * a row that already has a switch.
 */
private const val MARK_DISC_ALPHA = 0.22f
