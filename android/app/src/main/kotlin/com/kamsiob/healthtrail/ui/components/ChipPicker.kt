package com.kamsiob.healthtrail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object ChipPickerTags {
    const val SHEET = "chip_picker_sheet"
    const val SEARCH = "chip_picker_search"
    const val EMPTY = "chip_picker_empty"
    fun option(id: String) = "chip_picker_option_$id"
}

/** One thing a person can choose from a long set. */
data class PickerOption(
    val id: String,
    val label: String,
    /** One line of context, where there is something true to say. */
    val detail: String? = null,
    /** A care thread's route color, shown the way 5.2.2 says a thread shows itself. */
    val routeColor: Color? = null,
    /** The thread's creation order, which is what picks its dash. */
    val routeIndex: Int = 0,
)

/**
 * The full set, behind one control, with search.
 *
 * **This is the other half of the capped chip group in `DESIGN.md` 5.11.1.** A
 * chip set exists so the person can see every answer at once, and that promise
 * breaks the moment there are twenty three of them: a year five notebook offers
 * ten people, eight medications and seven threads on one capture form, and what
 * the person met was a wall. Five chips plus this is the same set with the
 * common answers in front.
 *
 * **It is a dense list, per 11.3 and 11.12**, because the person opening it is
 * scanning for one name they already have in mind. Cards here would be the same
 * wall with more space between its bricks.
 *
 * **Selection is `blue_soft` plus the title at weight 700**, which is the chip's
 * own language from 5.11 rather than a second one, so a person who has learned
 * what a chosen chip looks like already knows what a chosen row looks like.
 *
 * **Search narrows, it never filters anything away permanently**, and clearing
 * the field brings everything back. Nothing matching says so in a sentence
 * rather than leaving a blank area, per rule 11.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipPickerSheet(
    title: String,
    options: List<PickerOption>,
    selectedId: String?,
    onPick: (PickerOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()
    var query by remember { mutableStateOf("") }

    // Matched on the same simple contains-ignoring-case the rest of the app
    // uses. **Never a fuzzy match**: a person typing three letters of a nurse's
    // name and being shown somebody else is worse than being shown nothing,
    // because this list writes a link into the record.
    val shown = remember(options, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            options
        } else {
            options.filter { it.label.contains(needle, ignoreCase = true) }
        }
    }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
        modifier = Modifier.testTag(ChipPickerTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(top = Space.l, bottom = Space.l),
        ) {
            Text(
                text = title,
                style = HealthTrail.type.displayM,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(Modifier.height(Space.m))

            HealthTrailTextField(
                label = strings["picker.search"],
                value = query,
                onValueChange = { query = it },
                hint = strings["picker.search.hint"],
                fieldTestTag = ChipPickerTags.SEARCH,
            )

            Spacer(Modifier.height(Space.m))

            if (shown.isEmpty()) {
                Text(
                    text = strings["picker.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(ChipPickerTags.EMPTY),
                )
            } else {
                LazyColumn(
                    // Capped so the sheet never grows past half the screen and
                    // pushes its own search field off the top, which is what a
                    // list of ninety questions did.
                    modifier = Modifier.fillMaxWidth().heightIn(max = LIST_MAX),
                ) {
                    items(shown, key = { it.id }) { option ->
                        DenseRow(
                            // bidi-ok: every caller isolates before handing it here.
                            title = option.label,
                            // bidi-ok: every caller isolates before handing it here.
                            subtitle = option.detail,
                            leading = option.routeColor?.let { color ->
                                { RouteSwatch(color = color, index = option.routeIndex) }
                            },
                            selected = option.id == selectedId,
                            divider = option != shown.last(),
                            onClick = { onPick(option) },
                            modifier = Modifier.testTag(ChipPickerTags.option(option.id)),
                        )
                    }
                }
            }
        }
    }
}


/**
 * How tall the list may grow.
 *
 * A bottom sheet that fills the screen is a screen, and this one has a search
 * field at the top that has to stay reachable with the keyboard up.
 */
private val LIST_MAX = 380.dp
