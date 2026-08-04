package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.WashBand
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.FoldRowText
import com.kamsiob.healthtrail.ui.components.DenseRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import java.text.NumberFormat
import java.util.Currency

object MoneyTags {
    const val NAME = "money"
    const val ADD = "money_add"
    const val TOTAL = "money_total"
    fun fold(state: String) = "money_fold_$state"
    fun row(id: String) = "bill_$id"
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
 * Money: the bills, and where each one stands.
 *
 * **The app adds them up and decides nothing about them.** A total is counting,
 * which this app does. What it never does is say whether an amount is
 * reasonable, whether a bill should be disputed, or what somebody ought to do
 * next, all of which would be advice.
 *
 * **The total is of what is not settled**, because that is the number somebody
 * actually carries around. Paid and closed are excluded not because they do not
 * matter but because "what is still hanging over me" is the question a
 * caregiver is asking when they open this.
 *
 * **A bill with no amount is a real bill.** They arrive constantly saying "this
 * is not a bill" or with the amount pending, and a record that will not hold
 * one until a number exists loses the thing at the moment it appears. Null is
 * not zero, and the total says so by leaving it out rather than adding nothing.
 *
 * **Money is stored in minor units as an integer**, per the schema, because
 * floating point money is a defect waiting for a rounding boundary and this
 * record may be read out in a dispute.
 *
 * The links `MASTER_SPEC.md` section 4.3 wants, a bill knowing its chapter, the
 * call where it was disputed, and the standing instruction it broke, are not
 * built. This is the list underneath them.
 */
@Composable
fun MoneyScreen(
    bills: List<Repository.Bill>,
    onRemove: (Repository.Bill) -> Unit,
    onEdit: (Repository.Bill) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val open = bills.filter { it.isOpen }
    val openTotal = open.mapNotNull { it.amountMinor }.sum()
    var openStates by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val currency = bills.firstOrNull()?.currency ?: "USD"

    SectionScaffold(
        name = MoneyTags.NAME,
        title = strings["notebook.section.money"],
        subtitle = strings["money.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.MONEY,
        headingKey = "money.heading",
    ) {
        if (bills.isEmpty()) {
            item {
                SectionEmpty(name = MoneyTags.NAME, text = strings["money.empty"], section = Repository.Section.MONEY, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        }

        // The total sits above the list, per section 4.3, and only appears when
        // there is something unsettled to total. A zero total on a settled
        // notebook would be a number with nothing behind it.
        // **The total is a wash band**, per grid screen 14: a section-colored
        // strip carrying one summary figure above the detail rather than inside
        // it. Only when there is something unsettled to total, because a zero
        // on a settled notebook is a number with nothing behind it.
        //
        // **It states the figure and stops.** No arrow, no comparison to last
        // month, no color that changes with the amount. Whether fifteen
        // thousand dollars is a lot is the person's to judge, per rule 2.
        if (open.isNotEmpty() && openTotal > 0) {
            item {
                WashBand(
                    label = strings["money.open_total"],
                    value = formatMoney(strings, openTotal, currency),
                    hue = hueFor(Repository.Section.MONEY),
                    description = strings["money.open_total"] + ", " +
                        formatMoney(strings, openTotal, currency),
                    modifier = Modifier.testTag(MoneyTags.TOTAL),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }

        // **The one that needs a decision leads; every other state folds**,
        // per grid screen 14 and law 1. A person opens this screen because
        // something wants an answer, and four groups at one weight makes them
        // read all of it to find which.
        //
        // **A fold carries its count and its own sum**, so the person can see
        // what is behind it without opening it.
        STATE_ORDER.forEachIndexed { index, state ->
            val inState = bills.filter { it.state == state }
            if (inState.isEmpty()) return@forEachIndexed

            val leads = index == 0
            if (leads) {
                item(key = "lead_$state") {
                    GroupedSurface {
                        inState.forEachIndexed { row, bill ->
                            BillRow(
                                bill = bill,
                                onRemove = { onRemove(bill) },
                                onEdit = { onEdit(bill) },
                                isLast = row == inState.lastIndex,
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            } else {
                val sum = inState.mapNotNull { it.amountMinor }.sum()
                item(key = "fold_$state") {
                    FoldRowText(
                        label = strings["money.state.$state"],
                        expanded = openStates.contains(state),
                        onToggle = {
                            openStates = if (openStates.contains(state)) {
                                openStates - state
                            } else {
                                openStates + state
                            }
                        },
                        count = if (sum > 0) {
                            Bidi.join(inState.size.toString(), formatMoney(strings, sum, currency))
                        } else {
                            inState.size.toString()
                        },
                        modifier = Modifier.testTag(MoneyTags.fold(state)),
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
                if (openStates.contains(state)) {
                    item(key = "open_$state") {
                        GroupedSurface {
                            inState.forEachIndexed { row, bill ->
                                BillRow(
                                    bill = bill,
                                    onRemove = { onRemove(bill) },
                                    onEdit = { onEdit(bill) },
                                    isLast = row == inState.lastIndex,
                                )
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }

        item {
            QuietButton(
                label = strings["money.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(MoneyTags.ADD),
            )
        }
    }
}

@Composable
private fun BillRow(
    bill: Repository.Bill,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    isLast: Boolean,
) {
    val strings = LocalStrings.current

    // **The amount is the trailing value, right aligned and tabular**, per
    // `DESIGN.md` 15: amounts right-align so a column of them can be read down.
    //
    // **One ink for every amount.** A large bill is not colored differently
    // from a small one, which would be the app saying something about it.
    DenseRow(
        title = Bidi.isolate(bill.description),
        subtitle = listOfNotNull(
            bill.receivedEdtf?.takeIf { it.isNotBlank() }
                ?.let { EventDateText.render(strings, it) },
            bill.stateNote?.takeIf { it.isNotBlank() },
            bill.notes?.takeIf { it.isNotBlank() },
        ).let { Bidi.join(it) }.takeIf { it.isNotBlank() },
        trailing = bill.amountMinor
            ?.let { formatMoney(strings, it, bill.currency) }
            ?: strings["money.no_amount"],
        chevron = true,
        divider = !isLast,
        onClick = onEdit,
        modifier = Modifier
            .removableByLongPress(strings["edit.hint"], onRemove, onEdit)
            .testTag(MoneyTags.row(bill.id)),
    )
}

/**
 * Minor units rendered as money, in the catalog's locale but the bill's own
 * currency.
 *
 * **The locale decides the shape and the bill decides the currency.** An Arabic
 * reader in the United States is still looking at dollars, and rendering them
 * with the locale's default currency would silently relabel the amount.
 */
internal fun formatMoney(strings: Strings, minor: Long, currencyCode: String): String {
    val format = NumberFormat.getCurrencyInstance(strings.locale)
    val currency = runCatching { Currency.getInstance(currencyCode) }
        .getOrElse { Currency.getInstance("USD") }
    format.currency = currency
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    format.minimumFractionDigits = digits
    format.maximumFractionDigits = digits
    // Divided as a decimal rather than a double, so nothing rounds on the way
    // to the screen.
    val major = java.math.BigDecimal(minor)
        .movePointLeft(digits)
    return format.format(major)
}
