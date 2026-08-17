package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.formatMoney
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.BigNumber
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.IconAction
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object BillTags {
    const val NAME = "bill"
    const val AMOUNT = "bill_amount"
    const val EDIT = "bill_edit"
    const val REMOVE = "bill_remove"
    fun violation(id: String) = "bill_violation_$id"
    const val CHAPTER = "bill_chapter"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_bill"
}

/**
 * One bill: the amount, and what it is waiting on. Rewritten onto `ui/v4`, #386.
 *
 * **The amount is the one thing, and the state is what it is waiting on.** Those
 * are the two facts somebody opens a bill for, so the figure leads in the
 * section's own block at display size and everything else follows underneath.
 *
 * **The app adds up and decides nothing.** No overdue, no red, no urgency
 * ordering. A due date is stated at the precision it was given and the person
 * decides what it means, which is the rule the whole money section is built on.
 * Rule 2.
 *
 * **A bill with no amount says so**, because a bill that has not said how much
 * yet is an ordinary state and not a missing value. Rule 13.
 *
 * **Where this came from is a door, per rule 18.** A chapter shows its bills;
 * this is the other direction, and it was missing.
 */
@Composable
fun BillScreen(
    bill: Repository.Bill,
    onEdit: () -> Unit,
    /** Taking the bill out of the notebook, per #218. Opens the confirmation. */
    onRemove: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Every time a request was not followed that names this bill.
     *
     * **The other half of rule 18**, the same as the incident's: a violation
     * could name a bill from the moment the form began to ask, and the bill said
     * nothing back. #371.
     */
    violations: List<Repository.Violation> = emptyList(),
    onOpenViolations: () -> Unit = {},
    backLabelKey: String = "section.back.money",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.MONEY)

    Page(
        eyebrow = strings["notebook.section.money"],
        eyebrowColor = hue.ink,
        title = Bidi.isolate(bill.description),
        subtitle = strings["money.state.${bill.state}"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        actions = {
            IconAction(
                symbol = Symbols.edit,
                label = strings["bill.edit"],
                onClick = onEdit,
                modifier = Modifier.testTag(BillTags.EDIT),
            )
        },
        modifier = modifier.testTag(BillTags.ROOT),
    ) {
        item {
            Block(tone = BlockTone.Section, hue = hue) {
                BigNumber(
                    value = bill.amountMinor
                        ?.let { formatMoney(strings, it, bill.currency) }
                        ?: strings["bill.nothing"],
                    modifier = Modifier.testTag(BillTags.AMOUNT),
                )
                bill.stateNote?.takeIf { it.isNotBlank() }?.let {
                    Body(text = Bidi.isolate(it), color = hue.ink, style = HealthTrail.type.bodyL)
                }
            }
        }

        // The facts, each stated once and only when it is there. An absent date
        // is not rendered as a blank row: not recorded and zero are different
        // things and stay different things.
        labeledBlock(
            label = null,
            rows = listOfNotNull(
                bill.organizationName?.takeIf { it.isNotBlank() }
                    ?.let { strings["bill.from"] to it },
                bill.receivedEdtf?.takeIf { it.isNotBlank() }
                    ?.let { strings["bill.received"] to EventDateText.render(strings, it) },
                bill.dueEdtf?.takeIf { it.isNotBlank() }
                    ?.let { strings["bill.due"] to EventDateText.render(strings, it) },
            ).map { (label, value) ->
                { ListRow(title = label, value = Bidi.isolate(value)) }
            },
        )

        bill.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item {
                Block { Body(text = Bidi.isolate(notes), color = colors.ink) }
            }
        }

        // **Both ways, per rule 18.** A chapter lists its bills and a bill could
        // not name its chapter, which is a link with one end.
        bill.chapterId?.let { chapterId ->
            val name = bill.chapterName?.takeIf { it.isNotBlank() } ?: return@let
            labeledBlock(
                label = strings["bill.where"],
                rows = listOf {
                    ListRow(
                        title = Bidi.isolate(name),
                        mark = Symbols.chapters,
                        markTint = colors.goldInk,
                        markWash = colors.goldWash,
                        isDoor = true,
                        onClick = { onOpenChapter(chapterId) },
                        clickLabel = strings["open.action"],
                        modifier = Modifier.testTag(BillTags.CHAPTER),
                    )
                },
            )
        }

        // **What was asked for and not done, on the bill that carries it.** The
        // same group the incident grows, in the same shape, because it is the
        // same question from the other end. Rule 18.
        labeledBlock(
            label = strings["instruction.violations.linked"],
            rows = violations.map { violation ->
                {
                    ListRow(
                        title = Bidi.isolate(violation.instructionName.orEmpty()),
                        support = violation.note?.takeIf { it.isNotBlank() }
                            ?.let { Bidi.isolate(it) },
                        mark = Symbols.standingInstructions,
                        markTint = colors.alertInk,
                        markWash = colors.alertWash,
                        isDoor = true,
                        onClick = onOpenViolations,
                        clickLabel = strings["open.action"],
                        modifier = Modifier.testTag(BillTags.violation(violation.id)),
                    )
                }
            },
        )

        // **Sized to its label rather than to the screen**, D118. Removal is the
        // rarer errand, it sits last, and it opens the confirmation rather than
        // removing anything itself.
        item {
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(BillTags.REMOVE),
            )
        }
    }
}
