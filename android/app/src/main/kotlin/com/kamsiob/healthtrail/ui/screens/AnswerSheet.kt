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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Sheet
import com.kamsiob.healthtrail.ui.v4.rememberSheet

object AnswerTags {
    const val SHEET = "answer_sheet"
    const val FIELD = "answer_field"
    const val SAVE = "answer_save"
    const val CANCEL = "answer_cancel"
    const val ASKED = "answer_asked"
    const val ASKED_AT = "answer_asked_at"
    const val CORRECT = "answer_correct"
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
    /**
     * Opens the screen that corrects the question's own words. #374.
     *
     * **A question is typed in a corridor and read out in an appointment.** The
     * words matter, and the moment somebody writes them down is the worst
     * moment to get them right. Until now this was the one record on this sheet
     * that could be removed but not fixed, which is the shape rule 13 and every
     * audit of this app have both objected to.
     */
    onCorrect: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val sheetState = rememberSheet()
    var answer by remember(question.id) { mutableStateOf(question.answerText.orEmpty()) }

    Sheet(
        onDismiss = onDismiss,
        state = sheetState,
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

            // **Where it was asked, when it was ticked off a prep sheet.**
            // `asked_at_appointment_id` shipped in Phase 0 and only the fixture
            // had ever written it, so a question could claim an appointment
            // that said nothing back. Both ends say it now, rule 18. **Absent
            // rather than empty** when it was asked somewhere the app was not
            // told about, which is most of them.
            question.askedAtAppointmentTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings("questions.asked.at", "what" to Bidi.isolate(title)),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.testTag(AnswerTags.ASKED_AT),
                )
            }

            Spacer(Modifier.height(Space.l))

            // **The answer can always be typed.** It used to appear only after
            // the question was marked asked, so writing down what somebody
            // said took two trips through this sheet: mark it, reopen it,
            // type. The owner's words, #379: "I want to have the option to
            // type in the answer that I got not just Mark that I asked."
            //
            // **Writing an answer says it was asked**, which is why the
            // caller stamps the asked date when an open question is answered.
            // Nobody gets an answer to a question they never put.
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

            Action(
                label = strings["questions.answer.save"],
                onClick = { onSave(answer) },
                modifier = Modifier.fillMaxWidth().testTag(AnswerTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            // **Still offered on an open question**, because a question asked
            // and not answered is a real state: the meeting happened, nobody
            // knew, and that is worth recording without inventing an answer.
            if (question.isOpen) {
                Spacer(Modifier.height(Space.cardGap))
                Action(
                    label = strings["questions.mark_asked"],
                    onClick = onMarkAsked,
                    modifier = Modifier.testTag(AnswerTags.ASKED),
                )
            }

            Spacer(Modifier.height(Space.s))

            Action(
                label = strings["common.cancel"],
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(AnswerTags.CANCEL),
            )

            // **Correcting the words sits above removing them**, per rule 15
            // and the same order the person's screen uses: fixing a typo is the
            // errand somebody actually arrives with, and taking the question
            // away is the rare one.
            Spacer(Modifier.height(Space.l))
            Action(
                label = strings["ask.correct"],
                onClick = onCorrect,
                modifier = Modifier.testTag(AnswerTags.CORRECT),
            )

            // **Sized to its label and set apart**, which was wrong first and
            // was caught by looking at the sheet rather than at the code.
            // Drawn full width it was a third identical bar under Cancel, so
            // the one control that leaves and the one that removes wore the
            // same costume a thumb's width apart. Removal is a pill sized to
            // its label everywhere in the app now, on a screen and in a sheet,
            // and the gap above it says it does not belong to the pair.
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(AnswerTags.REMOVE),
            )
        }
    }
}
