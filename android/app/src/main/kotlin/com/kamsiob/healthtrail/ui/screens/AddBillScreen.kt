package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.FieldBlock
import com.kamsiob.healthtrail.ui.v4.Page

object AddBillTags {
    const val ROOT = "add_bill_root"
    const val SAVE = "add_bill_save"
    const val MORE = "add_bill_more"
    fun field(key: String) = "add_bill_$key"
    fun state(key: String) = "add_bill_state_$key"
}

/** What the person typed about a bill. */
data class BillDraft(
    val description: String = "",
    val amount: String = "",
    val state: String = "needs_attention",
    val notes: String = "",
)

/**
 * Recording a bill.
 *
 * **The amount is optional and that is not a courtesy.** Bills arrive saying
 * "this is not a bill", with the amount pending, or with a number nobody can
 * find. A record that refuses to hold one until a number exists loses the
 * thing at the moment it appears, which is the moment it is most likely to be
 * lost.
 *
 * **Nothing is calculated and nothing is advised.** No due date arithmetic, no
 * suggestion to dispute, no flag on a large amount. The person says where it
 * stands and the app writes that down.
 *
 * **What it is, how much, and where it stands are the bill; the note is behind
 * "Add more".** #361, 2026-08-12. It opens by itself when there is already a
 * note, so correcting a bill never hides what somebody wrote about it.
 */
@Composable
fun AddBillScreen(
    onSave: (BillDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** The record being corrected, or null when this one is new. */
    existing: Repository.Bill? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by remember(existing?.id) {
        mutableStateOf(
            BillDraft(
                description = existing?.description.orEmpty(),
                amount = existing?.amountMinor?.let { minorToPlain(it) }.orEmpty(),
                state = existing?.state ?: "needs_attention",
                notes = existing?.notes.orEmpty(),
            ),
        )
    }

    Page(
        title = if (existing == null) strings["money.add"] else strings["money.edit.title"],
        onBack = onCancel,
        backLabel = strings["common.cancel"],
        modifier = modifier.testTag(AddBillTags.ROOT),
        eyebrow = strings[labelKey(Repository.Section.MONEY)],
        section = Repository.Section.MONEY,
        // **The form's own gaps, not the page's.** A form is one
        // question after another rather than a column of groups, and
        // it spaces itself inside its single item.
        itemSpacing = Space.none,
        band = {
        Action(
            label = strings["capture.save"],
            onClick = { onSave(draft) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screenHorizontal)
                .testTag(AddBillTags.SAVE), emphasis = ActionEmphasis.Main,
        )
        Spacer(Modifier.height(Space.s))
        },
    ) {
        item {
            Column {
                Spacer(Modifier.height(Space.m))
                FactBlock(
                    label = null,
                    text = strings["money.add.lead"],
                    tone = BlockTone.Section,
                    mark = Symbols.of(Repository.Section.MONEY),
                    hue = hueFor(Repository.Section.MONEY),
                )
            Spacer(Modifier.height(Space.l))

            Field(
                label = strings["money.what"],
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                fieldTestTag = AddBillTags.field("what"),
                support = strings["money.what.hint"],
            )
            Spacer(Modifier.height(Space.m))

            Field(
                label = strings["money.amount"],
                value = draft.amount,
                onValueChange = { draft = draft.copy(amount = it) },
                keyboardType = KeyboardType.Decimal,
                fieldTestTag = AddBillTags.field("amount"),
                support = strings["money.amount.hint"],
            )
            Spacer(Modifier.height(Space.m))

            ChoiceChipGroup(label = strings["money.state"]) {
                listOf(
                    "needs_attention",
                    "disputed",
                    "waiting_on_insurance",
                    "paid",
                ).forEach { state ->
                    ChoiceChip(
                        label = strings["money.state.$state"],
                        selected = draft.state == state,
                        onClick = { draft = draft.copy(state = state) },
                        modifier = Modifier.testTag(AddBillTags.state(state)),
                    )
                }
            }

            Spacer(Modifier.height(Space.sectionGap))

            // **The note is behind "Add more"**, per law 3 and 10.8. #361.
            // Three questions and an open box was one box too many for a
            // screen somebody opens holding an envelope: what it is, how
            // much, and where it stands are the bill, and the rest is what
            // gets added later when somebody rings about it.
            // **The rest of the form is a group with a label, not a fold.**
            // D185: nothing sits behind a fold that a label and a scroll can
            // carry, and the sentence that used to explain the fold is the
            // group's own line now. Nothing here was ever required.
            FieldBlock(
                label = strings["capture.more"],
                aside = strings["capture.more.aside"],
                modifier = Modifier.testTag(AddBillTags.MORE),
            ) {
                DictatableField(
                    label = strings["appts.notes"],
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    support = strings["appts.notes.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = AddBillTags.field("notes"),
                )
            }

            Spacer(Modifier.height(Space.xl))
            }
        }
    }
}

/**
 * What the person typed, as minor units, or null when they typed nothing usable.
 *
 * **Forgiving on the way in and exact once stored.** Somebody copying an amount
 * off a statement types "$1,284.50", "1284.50", or "1,284". All three mean the
 * same thing and all three are accepted. Anything with no digits in it at all
 * is null rather than zero, because zero is a claim the person did not make.
 *
 * **Parsed through BigDecimal, never a double.** The schema stores minor units
 * as an integer precisely so no rounding boundary can move somebody's money,
 * and parsing through a double here would reintroduce exactly that on the way
 * in. A value with more decimal places than the currency has is truncated
 * rather than rounded up, so the app never records more than was written.
 */
internal fun parseAmountToMinor(raw: String, fractionDigits: Int = 2): Long? {
    val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' }
        // A comma is a thousands separator here, since the app is United States
        // only per MASTER_SPEC section 7.1 and every amount in it is dollars.
        .replace(",", "")
    if (cleaned.none { it.isDigit() }) return null
    return runCatching {
        java.math.BigDecimal(cleaned)
            .movePointRight(fractionDigits)
            .setScale(0, java.math.RoundingMode.DOWN)
            .toLong()
    }.getOrNull()
}

/**
 * Minor units back to something a person would type.
 *
 * **Only for seeding the form when correcting a bill**, so the field opens
 * showing what was stored rather than blank. It is the inverse of
 * [parseAmountToMinor] and it deliberately produces the plain form, with no
 * currency symbol and no grouping, because that is what an editable field
 * should hold.
 */
internal fun minorToPlain(minor: Long, fractionDigits: Int = 2): String =
    java.math.BigDecimal(minor)
        .movePointLeft(fractionDigits)
        .toPlainString()
