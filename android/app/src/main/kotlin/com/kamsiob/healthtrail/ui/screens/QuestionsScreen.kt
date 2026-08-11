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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.FoldRowText
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.DenseRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object QuestionTags {
    const val NAME = "questions"
    const val ASKED_FOLD = "questions_asked_fold"
    fun roleFold(role: String) = "questions_role_$role"
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
    /**
     * Opens the question.
     *
     * **Every question opens, not only an asked one.** A question still waiting
     * had no tap at all and carried removal on a long press, so the only way to
     * take one off the list was a gesture nobody could see. #218. What the
     * sheet offers depends on whether it has been asked yet.
     */
    onOpen: (Repository.Question) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val waiting = questions.filter { it.isOpen }
    val asked = questions.filterNot { it.isOpen }
    var askedOpen by rememberSaveable { mutableStateOf(false) }
    var openRoles by rememberSaveable { mutableStateOf(emptySet<String>()) }

    SectionScaffold(
        name = QuestionTags.NAME,
        title = strings["notebook.section.ask_next_time"],
        subtitle = strings["questions.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.ASK_NEXT_TIME,
        headingKey = "questions.heading",
    ) {
        if (questions.isEmpty()) {
            item { SectionEmpty(name = QuestionTags.NAME, text = strings["questions.empty"], section = Repository.Section.ASK_NEXT_TIME, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION)) }
        }

        // **The ones still waiting lead; the answered fold.** Law 1 and law 4:
        // a person opens this screen to ask something next time, and the ones
        // already asked are the record rather than the job.
        //
        // **Answered is never hidden and never a score.** The fold names them
        // and counts them, and the count is a fact about the list rather than
        // a tally of the person's diligence, per rule 13.
        // **Grouped by who answers it, per grid screen 21**, which is the
        // adaptive layout at its largest: five questions show flat and fifty
        // need grouping, and this notebook has seventy two.
        //
        // A single group of seventy two identical rows is a wall. Re-costuming
        // it from cards to rows made it a tidier wall, which is not the same as
        // converting it: **law 4 says a list that can grow gets its tools the
        // moment it can grow**, and grouping is the first of them.
        //
        // **The largest group leads open and the rest fold.** A person standing
        // in front of the charge nurse wants the charge nurse's questions, and
        // the biggest group is the likeliest one to be why they opened this.
        val byRole = waiting
            .groupBy { it.roleLabel?.takeIf { role -> role.isNotBlank() } }
            // Sorted by size then name, so the order is stable rather than
            // depending on which question happened to be created first.
            .toList()
            .sortedWith(compareByDescending<Pair<String?, List<Repository.Question>>> {
                it.second.size
            }.thenBy { it.first ?: "" })

        byRole.forEachIndexed { index, (role, inRole) ->
            val label = role ?: strings["questions.group.anyone"]
            val leads = index == 0
            if (!leads) {
                item(key = "fold_${role ?: "anyone"}") {
                    FoldRowText(
                        label = label,
                        expanded = openRoles.contains(label),
                        onToggle = {
                            openRoles = if (openRoles.contains(label)) {
                                openRoles - label
                            } else {
                                openRoles + label
                            }
                        },
                        count = inRole.size.toString(),
                        modifier = Modifier.testTag(QuestionTags.roleFold(label)),
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            if (leads || openRoles.contains(label)) {
                item(key = "group_${role ?: "anyone"}") {
                    if (leads) {
                        GroupHeaderText(label = label)
                        Spacer(Modifier.height(Space.s))
                    }
                    GroupedSurface {
                        inRole.forEachIndexed { row, question ->
                            QuestionRow(
                                question = question,
                                onMarkAsked = { onMarkAsked(question) },
                                onOpen = { onOpen(question) },
                                isLast = row == inRole.lastIndex,
                                // **Not repeated under its own heading.** The
                                // group already says whose question it is, and
                                // the same label in two slots of one row is on
                                // the ban list in `DESIGN.md` 17.
                                showRole = false,
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        if (asked.isNotEmpty()) {
            item {
                FoldRow(
                    labelKey = "questions.group.asked",
                    expanded = askedOpen,
                    onToggle = { askedOpen = !askedOpen },
                    count = asked.size.toString(),
                    modifier = Modifier.testTag(QuestionTags.ASKED_FOLD),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            if (askedOpen) {
                item {
                    GroupedSurface {
                        asked.forEachIndexed { index, question ->
                            QuestionRow(
                                question = question,
                                onMarkAsked = null,
                                onOpen = { onOpen(question) },
                                isLast = index == asked.lastIndex,
                            )
                        }
                    }
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
    onOpen: () -> Unit,
    isLast: Boolean,
    showRole: Boolean = true,
) {
    val strings = LocalStrings.current

    // **The question itself is the row's title**, because it is the thing being
    // read. Who it is for, when it was asked and what came back are the second
    // line: context for a question rather than things somebody scans for.
    //
    // **An asked question with no answer says so**, rather than leaving a gap
    // that reads as though nothing came back. Not knowing yet and being told
    // nothing are different, and the app only knows the first.
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

    DenseRow(
        title = Bidi.isolate(question.text),
        subtitle = detail,
        // **The one action worth taking from the list**, per law 2: an outlined
        // pill carrying a verb. A question already asked has nothing to mark.
        trailingContent = onMarkAsked?.let {
            {
                QuietButton(
                    label = strings["questions.mark_asked"],
                    onClick = it,
                    modifier = Modifier.testTag(QuestionTags.markAsked(question.id)),
                )
            }
        },
        divider = !isLast,
        // **Both states open**, which is what makes removal reachable without a
        // gesture. A question waiting to be asked opens onto what it is waiting
        // for; one already asked opens onto what came back.
        onClick = onOpen,
        clickLabel = strings["open.action"],
        modifier = Modifier.testTag(QuestionTags.row(question.id)),
    )
}
