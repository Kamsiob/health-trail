package com.kamsiob.healthtrail.ui.screens

import com.kamsiob.healthtrail.ui.components.Symbols
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object CardOptionsTags {
    const val SHEET = "card-options"
    const val LEAD = "card-options-lead"
    const val UP = "card-options-up"
    const val DOWN = "card-options-down"
    const val REMOVE = "card-options-remove"
    const val WHO = "card-options-who"
    const val DONE = "card-options-done"
    fun size(option: String) = "card-options-size-$option"
}

/**
 * One card's whole life, in one sheet. `DESIGN.md` 21.6 screen 7.
 *
 * **Opened from the card in edit mode**, which is what the grid draws and what
 * the spec under it says: in edit mode a card carries a remove dot and a drag
 * handle, and everything else is here. **Inline, all of it was a wall**: three
 * size chips and four named actions on every card put roughly a hundred and
 * forty controls on a twenty card Today, at one weight, which is rule 15's
 * uniform weight doing exactly what rule 15 says it does.
 *
 * **Law 3, one question per sheet.** The question is "what should this card
 * be", and everything here answers it: how big, which one it points at, whether
 * it leads, where it sits, and whether it stays at all.
 *
 * **Move up and Move down are the reorder path, not a fallback.** 23.2: they
 * work one handed, with a reader on, and with switch access, and the drag on
 * the card is the shortcut for somebody who can make the gesture.
 *
 * **Nothing here saves.** Every change goes to the draft the field is holding
 * and lands when the person taps Done on Today, per 21.6 screen 5. This sheet's
 * own Done closes the sheet and nothing else, which is why it says so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardOptionsSheet(
    /**
     * The card's name, already joined with what it points at.
     *
     * **Already isolated, so this does not isolate it again.** `Bidi.join`
     * wraps every part it is handed, and wrapping the result a second time
     * nests the marks: the title rendered as `⁨⁨Medications⁩⁩`. The third time
     * this exact defect has appeared, and the rule is always the same: the
     * caller joins once and nobody downstream touches it.
     */
    name: String,
    /** The size the draft currently has, one of small, wide or tall. */
    size: String,
    onResize: (String) -> Unit,
    onPromote: () -> Unit,
    /** Null when this card is already at the top of the field. */
    onMoveUp: (() -> Unit)?,
    /** Null when it is already at the bottom. */
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit,
    /** Opens the source picker, or null for a card that points at nothing. */
    onPickSource: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val sheetState = rememberSheet()

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(CardOptionsTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                // **Scrolls, because this sheet grows.** A card that takes a
                // source has one more group than one that does not, and at font
                // scale 2.0 the chips alone wrap to three lines. A sheet that
                // cannot reach its own Done is a sheet somebody is stuck in.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(top = Space.l, bottom = Space.l),
        ) {
            // The eyebrow says what kind of screen this is and the name says
            // which card, which is the shape every sheet in this app uses.
            Text(
                text = strings["today.options.eyebrow"].uppercase(strings.locale),
                style = type.eyebrow,
                // **`ink2`, because this is text.** 4.1 and D92: the app has two
                // text levels and `ink3` is non-text only, at 2.37 to 1 on
                // paper. A check catches this and caught it here.
                color = colors.ink2,
            )
            Text(
                text = name,
                style = type.displayM,
                color = colors.ink,
                modifier = Modifier
                    .padding(top = Space.xs)
                    .semantics { heading() },
            )

            OptionGroup(labelKey = "today.options.size") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    // **Two, and the phone is why.** A widget on the home screen
                    // somebody already has is a square or it is the width of the
                    // screen, and that is the only vocabulary anybody brings
                    // here. A card stored as `tall` was a full width card all
                    // along, so it lights the same chip rather than none.
                    for (option in listOf("small", "wide")) {
                        SizeChip(
                            label = strings["today.edit.size.$option"],
                            selected = if (option == "wide") {
                                size == "wide" || size == "tall"
                            } else {
                                size == "small"
                            },
                            onClick = { onResize(option) },
                            modifier = Modifier.testTag(CardOptionsTags.size(option)),
                        )
                    }
                }
            }

            onPickSource?.let { pick ->
                OptionGroup(labelKey = "today.options.source") {
                    Action(
                        label = strings["today.options.source.choose"],
                        onClick = pick,
                        modifier = Modifier.testTag(CardOptionsTags.WHO),
                    )
                }
            }

            OptionGroup(labelKey = "today.options.place") {
                Text(
                    text = strings["today.options.lead.detail"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(bottom = Space.xs),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Action(
                        label = strings["today.options.lead"],
                        onClick = onPromote,
                        modifier = Modifier.testTag(CardOptionsTags.LEAD),
                    )
                    onMoveUp?.let {
                        Action(
                            label = strings["today.options.up"],
                            onClick = it,
                            modifier = Modifier.testTag(CardOptionsTags.UP),
                        )
                    }
                    onMoveDown?.let {
                        Action(
                            label = strings["today.options.down"],
                            onClick = it,
                            modifier = Modifier.testTag(CardOptionsTags.DOWN),
                        )
                    }
                }
            }

            OptionGroup(labelKey = "today.options.keep") {
                Action(
                    label = strings["today.options.remove"],
                    onClick = onRemove,
                    modifier = Modifier.testTag(CardOptionsTags.REMOVE),
                )
                // **It says what removing costs, which is nothing.** Rule 13:
                // an arrangement is the person's and taking a card off is not a
                // loss of anything they wrote down.
                Text(
                    text = strings["today.options.remove.detail"],
                    style = type.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }

            Spacer(Modifier.height(Space.l))
            Action(
                label = strings["today.options.done"],
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CardOptionsTags.DONE),
            )
        }
    }
}

/**
 * One headed group inside the sheet, per 5.13.
 *
 * **A quiet mono eyebrow over a run of controls**, which is rule 15's grouping:
 * what belongs together sits together under a word, and the rest recedes.
 */
@Composable
private fun OptionGroup(labelKey: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = Space.l)) {
        // **The app's own group header**, per rule 22, rather than a second one
        // that would drift the moment either was touched. 5.13.
        Eyebrow(
            text = LocalStrings.current[labelKey],
            modifier = Modifier.padding(bottom = Space.s),
        )
        content()
    }
}

/**
 * One of the three sizes, as a chip. `DESIGN.md` 21.6 screen 7.
 *
 * **Selected is a fill and not only a color**, per section 9, so it survives
 * grayscale and every color vision difference.
 */
@Composable
private fun SizeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // **Material's own chip**, #392 and D196. This was a `Text` wearing a
    // clip, a background animated by hand, a spring on its scale and an
    // `indication = null` clickable: a drawing of a chip rather than one, and
    // the only place in the app where choosing looked like this.
    //
    // **`FilterChip` because this is choosing among options**, which is what
    // the size control does. It carries the selected state as a fill and a
    // leading check rather than as a color alone, which is section 9's rule and
    // what the hand built version was already reaching for.
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            // bidi-ok: the app's own word for a card size, from the catalog,
            // never anything the person typed.
            Text(text = label, style = HealthTrail.type.label)
        },
        modifier = modifier,
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(Symbols.check),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

