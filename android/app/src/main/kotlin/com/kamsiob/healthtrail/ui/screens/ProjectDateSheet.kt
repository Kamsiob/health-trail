package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.cappedChips
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ProjectDateTags {
    const val SHEET = "project-date-sheet"
    const val KIND = "project-date-kind"
    const val SOURCE = "project-date-source"
    const val WHEN = "project-date-when"
    const val SAVE = "project-date-save"
}

/**
 * Writing down a date a project has. `DESIGN.md` 20.1 and 20.5.
 *
 * **The source is asked for on the same screen as the date**, because it is
 * half of what a date is here. "Apr 12, from the letter of Mar 5" is usable a
 * year later and a bare Apr 12 is not: nobody can tell afterward whether it was
 * read off a letter, said on the phone, or guessed.
 *
 * **The kind is chips from the project's own date kinds, and free text
 * besides.** The chips are what the template offered and what the person kept;
 * the field is for the kind nobody predicted, which the template system was
 * never meant to close off.
 *
 * **The date carries whatever precision the person gave it**, per rule 17 and
 * the EDTF model. A letter that says "sometime in May" is a month, and this
 * never turns that into the first of May.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDateSheet(
    /** The kinds this project offers, from its template and its own edits. */
    kinds: List<String>,
    onSave: (kind: String, due: Edtf.Date, source: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var kind by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<Edtf.Date?>(null) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = null,
        modifier = Modifier.testTag(ProjectDateTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.date.add"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.date.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            if (kinds.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    for (option in cappedChips(kinds, kind.takeIf { it in kinds })) {
                        ChoiceChip(
                            label = option,
                            selected = kind == option,
                            onClick = { kind = if (kind == option) "" else option },
                        )
                    }
                }
                Spacer(Modifier.height(Space.m))
            }

            DictatableField(
                label = strings["project.date.kind"],
                value = kind,
                onValueChange = { kind = it },
                hint = strings["project.date.kind.hint"],
                singleLine = true,
                imeAction = ImeAction.Next,
                fieldTestTag = ProjectDateTags.KIND,
            )

            Spacer(Modifier.height(Space.m))

            QuietButton(
                // **The future tense, like the picker it opens.** A project's
                // date is almost always ahead, and "When was this?" over a
                // filing deadline reads as the app not knowing what it is
                // asking about.
                label = due?.let { EventDateText.render(strings, it) }
                    ?: strings["date.pick.title.future"],
                onClick = { picking = true },
                modifier = Modifier.fillMaxWidth().testTag(ProjectDateTags.WHEN),
            )

            Spacer(Modifier.height(Space.m))

            DictatableField(
                label = strings["project.date.source"],
                value = source,
                onValueChange = { source = it },
                hint = strings["project.date.source.hint"],
                singleLine = true,
                imeAction = ImeAction.Done,
                fieldTestTag = ProjectDateTags.SOURCE,
            )

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["project.date.save"],
                // **A date and what it is, and the source may wait.** Rule 13:
                // partial is a finished state, and somebody who has a deadline
                // but has not yet written down which letter it came off should
                // not lose the deadline over it.
                enabled = kind.isNotBlank() && due != null,
                onClick = { due?.let { onSave(kind.trim(), it, source.trim()) } },
                modifier = Modifier.fillMaxWidth().testTag(ProjectDateTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (picking) {
        DatePickerSheet(
            initial = due,
            onPick = { due = it; picking = false },
            onDismiss = { picking = false },
            // These are almost always ahead, which is what the closing window
            // is about, so the picker asks in the right tense.
            titleKey = "date.pick.title.future",
        )
    }
}
