package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object AnswerTags {
    const val SHEET = "answer_sheet"
    const val FIELD = "answer_field"
    const val SAVE = "answer_save"
    const val CANCEL = "answer_cancel"
    const val ASKED = "answer_asked"
    const val REMOVE = "answer_remove"
}

/**
 * A question, opened. What it offers depends on whether it has been asked.
 *
 * **A question that has not been asked yet had nowhere to open to**, and the
 * only thing that could be done with it from the list was a long press that
 * removed it. So a person who did not know the gesture could not take a
 * question off the list at all, and one who pressed by accident removed the
 * question they were reaching for. #218 and law 2.
 *
 * **The two faces are the same sheet because they are the same thing**, at two
 * points in its life. Splitting them into two sheets would mean a person
 * learning two places to find one question.
 *
 * **The asked face records what came back, which is the other half of the
 * record.** "We asked in March" is worth
 * something. "We asked in March and were told it would be reviewed at the next
 * care plan meeting" is the thing somebody actually needs six months later, and
 * the app could hold only the first half.
 *
 * **It never demands one.** The answer usually does not arrive in the same
 * conversation as the question, so this saves blank as readily as it saves a
 * paragraph, and it can be opened again later. That is the same reason marking
 * a question asked never asked for an answer in the first place.
 *
 * **A sheet rather than a screen**, because it is one field about a thing the
 * person is already looking at. Sending them to a separate screen for a
 * sentence would be two taps and a loss of context for no gain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerSheet(
    question: Repository.Question,
    onSave: (String) -> Unit,
    /** Marking it asked as of today, which is what the list's own pill does. */
    onMarkAsked: () -> Unit,
    /** Taking the question off the list, per #218. Opens the confirmation. */
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var answer by remember(question.id) { mutableStateOf(question.answerText.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = Radius.bottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        // Removed for the reason D42 gives: with the sheet fully expanded the
        // handle is a control that does nothing and announces nothing.
        dragHandle = null,
        modifier = Modifier.testTag(AnswerTags.SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                // **Every sheet that carries a form scrolls**, or its own
                // action is unreachable on a short screen. The standing sheet
                // proved it on 2026-08-12: its Save sat below the fold with no
                // way to reach it, and the old test device was tall enough to
                // hide that for the life of the screen. #129.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings[
                    if (question.isOpen) "questions.open.title" else "questions.answer.title"
                ],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))
            // The question shown back, so somebody answering three of them in a
            // row is never answering the wrong one.
            Text(
                text = Bidi.isolate(question.text),
                style = HealthTrail.type.bodyL,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings[
                    if (question.isOpen) "questions.open.lead" else "questions.answer.lead"
                ],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

            // **The field is only on the asked face.** Asking for what somebody
            // said about a question that has not been asked yet is the app
            // inventing a step, and an empty field with nothing to put in it
            // reads as work owed. Rule 13.
            if (question.isOpen) {
                FilledButton(
                    label = strings["questions.mark_asked"],
                    onClick = onMarkAsked,
                    modifier = Modifier.fillMaxWidth().testTag(AnswerTags.ASKED),
                )
            } else {
                DictatableField(
                    label = strings["questions.answer.title"],
                    value = answer,
                    onValueChange = { answer = it },
                    hint = strings["questions.answer.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = AnswerTags.FIELD,
                )

                Spacer(Modifier.height(Space.l))

                FilledButton(
                    label = strings["questions.answer.save"],
                    onClick = { onSave(answer) },
                    modifier = Modifier.fillMaxWidth().testTag(AnswerTags.SAVE),
                )
            }

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(AnswerTags.CANCEL),
            )

            // **Sized to its label and set apart**, which was wrong first and
            // was caught by looking at the sheet rather than at the code.
            // Drawn full width it was a third identical bar under Cancel, so
            // the one control that leaves and the one that removes wore the
            // same costume a thumb's width apart. Removal is a pill sized to
            // its label everywhere in the app now, on a screen and in a sheet,
            // and the gap above it says it does not belong to the pair.
            Spacer(Modifier.height(Space.l))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(AnswerTags.REMOVE),
            )
        }
    }
}
