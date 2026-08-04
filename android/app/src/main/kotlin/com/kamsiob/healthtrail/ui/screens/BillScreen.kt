package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object BillTags {
    const val NAME = "bill"
    const val AMOUNT = "bill_amount"
    const val EDIT = "bill_edit"
    const val CHAPTER = "bill_chapter"
}

/**
 * One bill: the amount, and what it is waiting on.
 *
 * **Tapping a bill used to open the form that edits it.** That is the app
 * answering "tell me about this" with "change this", and it is why the links a
 * bill already carries, the place it came out of and who sent it, had nowhere
 * to appear. Changing it is still one tap away and is now a door rather than
 * the only thing behind the row.
 *
 * **The amount is the one thing, and the state is what it is waiting on.** Those
 * are the two facts somebody opens a bill for, and they sit above everything
 * else at the scale law 1 asks for.
 *
 * **The app adds up and decides nothing.** No overdue, no red, no urgency
 * ordering. A due date is stated at the precision it was given and the person
 * decides what it means, which is the same rule the whole money section is
 * built on and the reason it says "the app adds them up and decides nothing
 * about them" at the top of the list.
 *
 * **Where this came from is a door, per rule 18.** A chapter shows its bills;
 * this is the other direction, and it was missing.
 */
@Composable
fun BillScreen(
    bill: Repository.Bill,
    onEdit: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.money",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = BillTags.NAME,
        title = strings["notebook.section.money"],
        heading = Bidi.isolate(bill.description),
        section = Repository.Section.MONEY,
        subtitle = strings["money.state.${bill.state}"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        item {
            // **The amount at hero weight, and the words for it underneath.**
            // A figure with no amount says so rather than showing nothing,
            // because a bill that has not said how much yet is an ordinary
            // state and not a missing value. Rule 13.
            Text(
                text = bill.amountMinor
                    ?.let { formatMoney(strings, it, bill.currency) }
                    ?: strings["bill.nothing"],
                style = HealthTrail.type.hero,
                color = colors.ink,
                modifier = Modifier.testTag(BillTags.AMOUNT),
            )

            bill.stateNote?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Space.s))
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink2)
            }

            Spacer(Modifier.height(Space.sectionGap))
        }

        // The facts, each stated once and only when it is there. An absent date
        // is not rendered as a blank row: not recorded and zero are different
        // things and stay different things.
        val facts = listOfNotNull(
            bill.organizationName?.takeIf { it.isNotBlank() }
                ?.let { strings["bill.from"] to it },
            bill.receivedEdtf?.takeIf { it.isNotBlank() }
                ?.let { strings["bill.received"] to EventDateText.render(strings, it) },
            bill.dueEdtf?.takeIf { it.isNotBlank() }
                ?.let { strings["bill.due"] to EventDateText.render(strings, it) },
        )
        if (facts.isNotEmpty()) {
            item {
                GroupedSurface {
                    facts.forEachIndexed { index, (label, value) ->
                        DenseRow(
                            title = label,
                            trailing = Bidi.isolate(value),
                            divider = index < facts.size - 1,
                        )
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        bill.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item {
                Text(
                    text = Bidi.isolate(notes),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // **Both ways, per rule 18.** A chapter lists its bills and a bill could
        // not name its chapter, which is a link with one end.
        bill.chapterId?.let { chapterId ->
            val name = bill.chapterName?.takeIf { it.isNotBlank() } ?: return@let
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "bill.where")
                Spacer(Modifier.height(Space.headerGap))
                GroupedSurface {
                    DenseRow(
                        title = Bidi.isolate(name),
                        leading = { WaypointDot(color = colors.gold, state = Waypoint.MILESTONE) },
                        chevron = true,
                        divider = false,
                        onClick = { onOpenChapter(chapterId) },
                        modifier = Modifier.testTag(BillTags.CHAPTER),
                    )
                }
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["bill.edit"],
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().testTag(BillTags.EDIT),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
