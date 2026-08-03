package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
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
import com.kamsiob.healthtrail.ui.components.FilledButton
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
}

/**
 * Recording what came back after a question was asked.
 *
 * **The answer is the other half of the record.** "We asked in March" is worth
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
                .padding(horizontal = Space.screenHorizontal)
                .padding(bottom = Space.l),
        ) {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["questions.answer.title"],
                style = HealthTrail.type.displayM,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.s))
            // The question shown back, so somebody answering three of them in a
            // row is never answering the wrong one.
            Text(text = question.text, style = HealthTrail.type.bodyL, color = colors.ink2)

            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["questions.answer.lead"],
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
            )

            Spacer(Modifier.height(Space.l))

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

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(AnswerTags.CANCEL),
            )
        }
    }
}
