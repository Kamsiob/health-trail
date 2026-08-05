package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object AddCardTags {
    const val ROOT = "add-card"
    fun entry(key: String) = "add-card-$key"
}

/**
 * One thing a person can put on Today.
 *
 * [sourceTable] and [sourceId] are set for a card that points at something,
 * which is what makes one measure a different entry from another.
 */
data class CardOffer(
    val type: String,
    val label: String,
    /** The answer this card would give right now, already worded. */
    val preview: String,
    val sourceTable: String? = null,
    val sourceId: String? = null,
) {
    val key: String get() = sourceId?.let { "$type-$it" } ?: type
}

/**
 * The gallery of cards a person can add. `DESIGN.md` 21.6 screen 6.
 *
 * **Each entry previews what it would say right now**, with real current data,
 * because a name alone asks somebody to imagine a screen they have never seen.
 * "Medications, 6" is a decision they can make; "Medications" is a guess.
 *
 * **A card already on Today is not offered again**, except the ones that point
 * at something: a second measure is a different card from the first, and a
 * person tracking blood pressure and weight wants both.
 *
 * **Nothing here is ranked by what the app thinks matters.** The order is the
 * binder's order, which is the order every other list in the app uses, so the
 * gallery teaches nothing new and hides nothing.
 */
@Composable
fun AddCardSheet(
    offers: List<CardOffer>,
    onAdd: (CardOffer) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    SectionScaffold(
        name = AddCardTags.ROOT,
        title = strings["today.tab"],
        headingKey = "today.add.title",
        subtitle = strings["today.add.lead"],
        onBack = onBack,
        backLabelKey = "today.add.back",
        modifier = modifier,
    ) {
        if (offers.isEmpty()) {
            // **A real state and a calm one.** Somebody who has added every
            // card has not made a mistake, and the screen says so rather than
            // showing an empty list.
            item {
                Text(
                    text = strings["today.add.none"],
                    style = type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.padding(vertical = Space.m),
                )
            }
            return@SectionScaffold
        }

        items(offers.size, key = { offers[it].key }) { index ->
            val offer = offers[index]
            DenseRow(
                title = Bidi.isolate(offer.label),
                subtitle = offer.preview,
                onClick = { onAdd(offer) },
                modifier = Modifier.testTag(AddCardTags.entry(offer.key)),
            )
        }

        item { Spacer(Modifier.height(Space.xxl)) }
    }
}
