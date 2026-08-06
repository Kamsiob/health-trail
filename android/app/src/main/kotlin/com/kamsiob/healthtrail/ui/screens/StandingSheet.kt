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
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.cappedChips
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

object StandingTags {
    const val SHEET = "standing-sheet"
    const val WHO = "standing-who"
    const val WHAT = "standing-what"
    const val WHEN = "standing-when"
    const val SAVE = "standing-save"
}

/**
 * Recording where a project stands now. `DESIGN.md` 20.5, screen 8.
 *
 * **A one-stage sheet**, because this is the thing a person does standing in a
 * corridor right after a phone call, and a screen with three steps is a screen
 * they will do later and then not do.
 *
 * **Whose hands is chips drawn from the project's own people, and free text
 * besides.** The chips are what somebody already wrote down; the field is for
 * the office nobody has named yet, which is most of them at the start.
 *
 * **Since when defaults to today and can be changed.** A person recording this
 * five minutes after the call usually wants today, so today is one tap. The
 * sheet's own copy said "the date is today unless you change it" and for a week
 * nothing on it changed the date, which is the same defect the stage sheet
 * carried until `ac526b6`. Somebody writing down on Thursday that the county
 * picked it up on Monday needs this, and rule 17 says a date is never falsely
 * precise and always the person's.
 *
 * **Nothing here is framed as a fight**, section 22. The holder is named by
 * role as the person wrote it down, and what is happening is stated as fact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingSheet(
    /** Names already on this project, offered as chips. */
    people: List<String>,
    onSave: (holder: String, activity: String, since: Edtf.Date) -> Unit,
    onDismiss: () -> Unit,
    /** Where it stood before, so the sheet opens on what is already true. */
    previous: Repository.ProjectStanding? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var holder by remember(previous?.id) { mutableStateOf(previous?.holderLabel.orEmpty()) }
    var activity by remember(previous?.id) { mutableStateOf(previous?.activity.orEmpty()) }
    // **Today, not the previous standing's date.** This records where the
    // project stands now; opening on the date it last changed hands would
    // pre-fill the one answer that is almost certainly wrong.
    var since by remember(previous?.id) { mutableStateOf(Edtf.day(LocalDate.now())) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        // D42: with the sheet fully expanded the handle is a control that does
        // nothing and announces nothing.
        dragHandle = null,
        modifier = Modifier.testTag(StandingTags.SHEET),
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
                text = strings["project.standing.update"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["project.standing.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            // **The people already on this project, as chips.** They are what
            // somebody has written down, and the field below is for the office
            // nobody has named yet, which is most of them at the start.
            if (people.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    for (person in cappedChips(people, holder.takeIf { it in people })) {
                        ChoiceChip(
                            label = person,
                            selected = holder == person,
                            // Tapping the chosen one again clears it, because a
                            // chip that cannot be undone is a decision the
                            // person cannot take back.
                            onClick = { holder = if (holder == person) "" else person },
                        )
                    }
                }
                Spacer(Modifier.height(Space.m))
            }

            DictatableField(
                label = strings["project.standing.who"],
                value = holder,
                onValueChange = { holder = it },
                hint = strings["project.standing.who.hint"],
                singleLine = true,
                imeAction = ImeAction.Next,
                fieldTestTag = StandingTags.WHO,
            )

            Spacer(Modifier.height(Space.m))

            DictatableField(
                label = strings["project.standing.what"],
                value = activity,
                onValueChange = { activity = it },
                hint = strings["project.standing.what.hint"],
                singleLine = false,
                imeAction = ImeAction.Done,
                fieldTestTag = StandingTags.WHAT,
            )

            Spacer(Modifier.height(Space.m))

            // **What the lead has promised all along.** Today by default, so
            // the common case is still no taps at all, and the whole control
            // is one line rather than a stage of its own.
            //
            // **Labeled like the two fields above it**, because unlabeled it
            // was a bare button showing a date in a column of labeled fields,
            // directly above two more full width buttons. A value is not an
            // action and must not be dressed as one.
            Text(
                text = strings["project.standing.when"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
            QuietButton(
                label = EventDateText.render(strings, since),
                onClick = { picking = true },
                modifier = Modifier.fillMaxWidth().testTag(StandingTags.WHEN),
            )

            Spacer(Modifier.height(Space.l))

            FilledButton(
                label = strings["project.standing.save"],
                // **Partial is a finished state**, rule 13: a person who knows
                // only that it is with the county can say that and nothing
                // else. The one thing this cannot save is nothing at all.
                enabled = holder.isNotBlank(),
                onClick = { onSave(holder.trim(), activity.trim(), since) },
                modifier = Modifier.fillMaxWidth().testTag(StandingTags.SAVE),
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
            initial = since,
            onPick = { since = it; picking = false },
            onDismiss = { picking = false },
            titleKey = "date.pick.title",
        )
    }
}
