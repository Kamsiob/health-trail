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
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton

object MoneyTags {
    const val NAME = "money"
    const val ADD = "money_add"
    const val TOTAL = "money_total"
    const val LEAD_HEADER = "money_lead_header"
    fun row(id: String) = "bill_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_money"
}

/** The order bills are grouped in, most pressing first. */
private val STATE_ORDER = listOf(
    "needs_attention",
    "disputed",
    "waiting_on_insurance",
    "paid",
    "closed",
)

/**
 * Money: the bills, and where each one stands. Rewritten onto `ui/v4`, #386.
 *
 * **The app adds them up and decides nothing about them.** A total is counting,
 * which this app does. What it never does is say whether an amount is
 * reasonable, whether a bill should be disputed, or what somebody ought to do
 * next, all of which would be advice. Rule 2.
 *
 * **The total is of what is not settled**, because that is the number somebody
 * actually carries around. Paid and closed are left out not because they do not
 * matter but because "what is still hanging over me" is the question a
 * caregiver opens this screen with.
 *
 * **A bill with no amount is a real bill.** They arrive constantly saying "this
 * is not a bill" or with the amount pending, and a record that will not hold one
 * until a number exists loses the thing at the moment it appears. Null is not
 * zero, and the total says so by leaving it out rather than adding nothing.
 *
 * **Every state is on the screen under its own label.** The one that wants a
 * decision leads and wears the alert ink; the rest follow in order. They used to
 * fold, each carrying its count and sum so the person could see what was behind
 * the door: a list that simply carries them says it without the door. D185.
 */
@Composable
fun MoneyScreen(
    bills: List<Repository.Bill>,
    /**
     * Opens the bill itself.
     *
     * **The row used to open the form that edits it**, which is the app
     * answering "tell me about this" with "change this". Changing it is one tap
     * further on, from the bill's own screen.
     */
    onOpen: (Repository.Bill) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.MONEY)
    val open = bills.filter { it.isOpen }
    val openTotal = open.mapNotNull { it.amountMinor }.sum()
    val currency = bills.firstOrNull()?.currency ?: "USD"

    // **The first state that has anything in it leads**, rather than the first
    // state in the order. Bound to the index, a notebook with three disputed
    // bills and nothing needing attention opened on a total and a stack of
    // labels with no bill visible at all.
    val leadState = STATE_ORDER.firstOrNull { state -> bills.any { it.state == state } }

    Page(
        eyebrow = strings["notebook.section.money"],
        eyebrowColor = hue.ink,
        title = strings["money.heading"],
        subtitle = strings["money.subtitle"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(MoneyTags.ROOT),
        // **The way in floats over the list rather than sitting under it.**
        // D200: it was the last `item` in the `LazyColumn`, and a section
        // screen has no capture button in that corner to compete with.
        fab = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = {
                    Icon(painter = painterResource(Symbols.add), contentDescription = null)
                },
                text = { Text(text = strings["money.add"]) },
                // The sentence sits on the button's own node, `docs/TRAPS.md`.
                modifier = Modifier
                    .testTag(MoneyTags.ADD)
                    .semantics { contentDescription = strings["money.add"] },
            )
        },
    ) {
        if (bills.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about an empty list.
                    Body(
                        text = strings["money.empty"],
                        color = colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
        }

        // **The figure the person carries around, raised into its own block**,
        // and only when there is something unsettled to total: a zero on a
        // settled notebook is a number with nothing behind it.
        //
        // **It states the figure and stops.** No arrow, no comparison to last
        // month, no color that changes with the amount. Whether fifteen
        // thousand dollars is a lot is the person's to judge, rule 2.
        if (open.isNotEmpty() && openTotal > 0) {
            item {
                Block(
                    tone = BlockTone.Section,
                    hue = hue,
                    modifier = Modifier
                        .semantics {
                            // **One catalog sentence rather than two pieces
                            // glued together.** A label, a comma and a number is
                            // an English sentence built in Kotlin: the order,
                            // the separator and the spacing all differ by
                            // language. #13, and `check_concatenation.py`
                            // refuses the shape now.
                            contentDescription = strings(
                                "money.open_total.said",
                                "amount" to formatMoney(strings, openTotal, currency),
                            )
                        }
                        .testTag(MoneyTags.TOTAL),
                ) {
                    Eyebrow(text = strings["money.open_total"], color = hue.ink)
                    BigNumber(value = formatMoney(strings, openTotal, currency))
                }
            }
        }

        STATE_ORDER.forEach { state ->
            val inState = bills.filter { it.state == state }
            if (inState.isEmpty()) return@forEach

            labeledBlock(
                label = strings["money.state.$state"],
                // **The label says why the first group is up here**, in the
                // alert ink, so the lead reads as the one wanting a decision
                // rather than as an arbitrary first row. #345.
                labelColor = if (state == leadState) colors.alertInk else colors.ink2,
                labelTag = MoneyTags.LEAD_HEADER.takeIf { state == leadState },
                rows = inState.map { bill -> { BillRow(bill, hue, onOpen) } },
            )
        }

    }
}

/**
 * One bill: what it is, when it came, and how much.
 *
 * **The amount is the row's value, tabular, so a column of them reads down.**
 * `DESIGN.md` 15. **One ink for every amount**: a large bill is not colored
 * differently from a small one, which would be the app saying something about
 * it. Rule 2.
 */
@Composable
private fun BillRow(
    bill: Repository.Bill,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onOpen: (Repository.Bill) -> Unit,
) {
    val strings = LocalStrings.current

    ListRow(
        title = Bidi.isolate(bill.description),
        support = listOfNotNull(
            bill.receivedEdtf?.takeIf { it.isNotBlank() }
                ?.let { EventDateText.render(strings, it) },
            bill.stateNote?.takeIf { it.isNotBlank() },
            bill.notes?.takeIf { it.isNotBlank() },
        ).let { Bidi.join(it) }.takeIf { it.isNotBlank() },
        mark = Symbols.money,
        markHue = hue,
        // **Isolated, like every other amount on the screen.** A number beside
        // a currency beside a right to left heading is three runs, and the app
        // says which is which rather than leaving it to the algorithm. Seen in
        // Arabic on the phone.
        value = bill.amountMinor
            ?.let { Bidi.isolate(formatMoney(strings, it, bill.currency)) }
            ?: strings["money.no_amount"],
        onClick = { onOpen(bill) },
        clickLabel = strings["open.action"],
        modifier = Modifier.testTag(MoneyTags.row(bill.id)),
    )
}
