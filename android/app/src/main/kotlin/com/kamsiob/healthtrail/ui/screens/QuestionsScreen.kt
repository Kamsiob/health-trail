package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object QuestionTags {
    const val NAME = "questions"
    fun row(id: String) = "question_$id"
    fun markAsked(id: String) = "question_asked_$id"
}

/**
 * Ask next time: the things somebody meant to ask and did not get the chance to.
 *
 * **This section counted zero for as long as it existed.** Capturing "ask next
 * time" wrote a trail entry and nothing else, so the question appeared in the
 * trail and this section said "Nothing yet" forever. A section that counts
 * nothing while the thing it counts is actively being captured is the app being
 * wrong about itself, which is worse than the section not existing at all. Both
 * rows are now written in one transaction.
 *
 * **Asked questions stay.** They move into their own group and keep the date
 * they were asked on. "We asked in March and were told it would be reviewed" is
 * exactly the kind of thing somebody needs six months later, and deleting it
 * the moment it is asked would throw away the half that matters.
 *
 * **Marking one asked takes one tap and never demands an answer.** The answer
 * often does not arrive in the same conversation, and requiring one would mean
 * the person either lies or leaves the question open when it is not.
 */
@Composable
fun QuestionsScreen(
    questions: List<Repository.Question>,
    onMarkAsked: (Repository.Question) -> Unit,
    onRemove: (Repository.Question) -> Unit,
    onAnswer: (Repository.Question) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val waiting = questions.filter { it.isOpen }
    val asked = questions.filterNot { it.isOpen }

    SectionScaffold(
        name = QuestionTags.NAME,
        title = strings["notebook.section.ask_next_time"],
        subtitle = strings["questions.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (questions.isEmpty()) {
            item { SectionEmpty(name = QuestionTags.NAME, text = strings["questions.empty"]) }
        }

        // A group header only appears when its group has something under it, so
        // a notebook with nothing asked yet never shows an empty "Asked".
        if (waiting.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "questions.group.waiting")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (question in waiting) {
                item(key = question.id) {
                    QuestionRow(
                        question = question,
                        onMarkAsked = { onMarkAsked(question) },
                        onRemove = { onRemove(question) },
                        onAnswer = { onAnswer(question) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        if (asked.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "questions.group.asked")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (question in asked) {
                item(key = question.id) {
                    QuestionRow(
                        question = question,
                        onMarkAsked = null,
                        onRemove = { onRemove(question) },
                        onAnswer = { onAnswer(question) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }
    }
}

/**
 * One question.
 *
 * The question itself carries the weight, because it is the thing being read.
 * An asked question keeps its text at the same size and drops to the quieter
 * ink: it is still the record, it is simply no longer waiting.
 */
@Composable
private fun QuestionRow(
    question: Repository.Question,
    onMarkAsked: (() -> Unit)?,
    onRemove: () -> Unit,
    onAnswer: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.card)
            // A tap records what came back, once the question has been asked.
            .removableByLongPress(
                if (question.isOpen) strings["remove.hint"] else strings["edit.hint"],
                onRemove,
                if (question.isOpen) null else onAnswer,
            )
            .testTag(QuestionTags.row(question.id))
            .padding(Space.cardPadding),
    ) {
        question.roleLabel?.takeIf { it.isNotBlank() }?.let { role ->
            Text(text = role, style = HealthTrail.type.mono, color = colors.ink3Text)
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = question.text,
            style = HealthTrail.type.bodyL,
            color = if (question.isOpen) colors.ink else colors.ink2,
        )

        if (question.isOpen) {
            Spacer(Modifier.height(Space.xs))
            if (onMarkAsked != null) {
                TextAction(
                    label = strings["questions.mark_asked"],
                    onClick = onMarkAsked,
                    modifier = Modifier.testTag(QuestionTags.markAsked(question.id)),
                )
            }
        } else {
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings(
                    "questions.asked_on",
                    "date" to EventDateText.render(strings, question.askedEdtf),
                ),
                style = HealthTrail.type.mono,
                color = colors.ink3Text,
            )
            Spacer(Modifier.height(Space.xs))
            // **An asked question with no answer says so**, rather than
            // leaving a gap that reads as though nothing came back. Not
            // knowing yet and being told nothing are different, and the app
            // only knows the first.
            Text(
                text = question.answerText?.takeIf { it.isNotBlank() }
                    ?: strings["questions.answer.none"],
                style = HealthTrail.type.bodyM,
                color = if (question.answerText.isNullOrBlank()) {
                    colors.ink3Text
                } else {
                    colors.ink2
                },
            )
        }
    }
}
