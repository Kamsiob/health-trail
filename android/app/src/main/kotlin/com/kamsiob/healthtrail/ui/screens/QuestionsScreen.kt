package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockIconAction
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton

object QuestionTags {
    const val NAME = "questions"
    const val ADD = "questions_add"
    fun row(id: String) = "question_$id"
    fun markAsked(id: String) = "question_asked_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_questions"
}

/**
 * Ask next time: the things somebody meant to ask and did not get the chance
 * to. Rewritten onto `ui/v4`, #386.
 *
 * **Asked questions stay.** They keep the date they were asked on and move to
 * their own group. "We asked in March and were told it would be reviewed" is
 * exactly what somebody needs six months later, and deleting it the moment it
 * is asked would throw away the half that matters.
 *
 * **Marking one asked takes one tap and never demands an answer.** The answer
 * often does not arrive in the same conversation, and requiring one would mean
 * the person either lies or leaves the question open when it is not.
 *
 * **Grouped by who answers it, and no group is behind a door.** A person
 * standing in front of the charge nurse wants the charge nurse's questions, so
 * every group carries its own label and the largest leads. Seventy two
 * identical rows is a wall; seventy two rows under six labels is a list. D185
 * took the folds out: a label reached by scrolling beats a count reached by
 * tapping.
 *
 * **The count is never a score.** It is a fact about the list rather than a
 * tally of the person's diligence, per rule 13.
 */
@Composable
fun QuestionsScreen(
    questions: List<Repository.Question>,
    onMarkAsked: (Repository.Question) -> Unit,
    /**
     * Opens the question.
     *
     * **Every question opens, not only an asked one.** A question still waiting
     * had no tap at all and carried removal on a long press, so the only way to
     * take one off the list was a gesture nobody could see. #218.
     */
    onOpen: (Repository.Question) -> Unit,
    /**
     * Writes a new question down, from the screen that holds them. #355.
     *
     * **Every other section screen has its own way in and this one had none**,
     * so the only route to a question was the capture button and remembering
     * which kind it was.
     */
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val hue = hueFor(Repository.Section.ASK_NEXT_TIME)
    val waiting = questions.filter { it.isOpen }
    val asked = questions.filterNot { it.isOpen }

    // Sorted by size then name, so the order is stable rather than depending on
    // which question happened to be created first.
    val byRole = waiting
        .groupBy { it.roleLabel?.takeIf { role -> role.isNotBlank() } }
        .toList()
        .sortedWith(
            compareByDescending<Pair<String?, List<Repository.Question>>> { it.second.size }
                .thenBy { it.first ?: "" },
        )

    Page(
        section = Repository.Section.ASK_NEXT_TIME,
        eyebrow = strings["notebook.section.ask_next_time"],
        eyebrowColor = hue.ink,
        title = strings["questions.heading"],
        subtitle = strings["questions.subtitle"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(QuestionTags.ROOT),
        // **The way in floats over the list rather than sitting under it.**
        // D200: it was the last `item` in the `LazyColumn`, and a section
        // screen has no capture button in that corner to compete with.
        fab = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = {
                    Icon(painter = painterResource(Symbols.add), contentDescription = null)
                },
                text = { Text(text = strings["questions.add"]) },
                // The sentence sits on the button's own node, `docs/TRAPS.md`.
                modifier = Modifier
                    .testTag(QuestionTags.ADD)
                    .semantics { contentDescription = strings["questions.add"] },
            )
        },
    ) {
        if (questions.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about an empty list.
                    Body(
                        text = strings["questions.empty"],
                        color = HealthTrail.colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        byRole.forEach { (role, inRole) ->
            labeledBlock(
                leading = true,
                // A role is the person's own word for who answers this, so it
                // keeps its own case. "For anyone" is the app's own label.
                label = role ?: strings["questions.group.anyone"],
                fixedLabel = role == null,
                rows = inRole.map { question ->
                    {
                        QuestionRow(
                            question = question,
                            hue = hue,
                            // **Not repeated under its own heading.** The group
                            // already says whose question it is, and the same
                            // label twice in one row is on the ban list.
                            showRole = false,
                            onMarkAsked = { onMarkAsked(question) },
                            onOpen = { onOpen(question) },
                        )
                    }
                },
            )
        }

        labeledBlock(
            label = strings["questions.group.asked"],
            rows = asked.map { question ->
                {
                    QuestionRow(
                        question = question,
                        hue = hue,
                        onMarkAsked = null,
                        onOpen = { onOpen(question) },
                    )
                }
            },
        )

    }
}

/**
 * One question: the words, who answers it, and the one thing to do about it.
 *
 * **The question itself is the row's title**, because it is the thing being
 * read. Who it is for, when it was asked and what came back are the second
 * line: context for a question rather than things somebody scans for.
 *
 * **An asked question with no answer says so**, rather than leaving a gap that
 * reads as though nothing came back. Not knowing yet and being told nothing are
 * different, and the app only knows the first.
 */
@Composable
private fun QuestionRow(
    question: Repository.Question,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onMarkAsked: (() -> Unit)?,
    onOpen: () -> Unit,
    showRole: Boolean = true,
) {
    val strings = LocalStrings.current

    val detail = if (question.isOpen) {
        question.roleLabel?.takeIf { it.isNotBlank() && showRole }
    } else {
        listOfNotNull(
            question.roleLabel?.takeIf { it.isNotBlank() && showRole },
            strings(
                "questions.asked_on",
                "date" to EventDateText.render(strings, question.askedEdtf),
            ),
            question.answerText?.takeIf { it.isNotBlank() }
                ?: strings["questions.answer.none"],
        ).let { Bidi.join(it) }
    }

    ListRow(
        title = Bidi.isolate(question.text),
        support = detail,
        mark = Symbols.askNextTime,
        markHue = hue,
        // **The one action worth taking from the list, as a mark.** A question
        // already asked has nothing to mark. The words live in the description,
        // because twenty controls all called "I asked this" is the ambiguity
        // `DESIGN.md` 5.12 exists to prevent: a reader hears which question.
        trailing = onMarkAsked?.let {
            {
                BlockIconAction(
                    mark = Symbols.check,
                    // The words are the label rather than a description laid
                    // over the control, because twenty controls all called "I
                    // asked this" is the ambiguity `DESIGN.md` 5.12 exists to
                    // prevent: a reader hears which question.
                    label = strings(
                        "questions.mark_asked.what",
                        "question" to question.text,
                    ),
                    onClick = it,
                    modifier = Modifier.testTag(QuestionTags.markAsked(question.id)),
                )
            }
        },
        // **Both states open**, which is what makes removal reachable without a
        // gesture. A question waiting opens onto what it is waiting for; one
        // already asked opens onto what came back.
        onClick = onOpen,
        clickLabel = strings["open.action"],
        modifier = Modifier.testTag(QuestionTags.row(question.id)),
    )
}
