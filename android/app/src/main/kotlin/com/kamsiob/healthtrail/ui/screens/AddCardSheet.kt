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
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.LocalDate

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
    /**
     * The answer this card would give right now, already worded.
     *
     * **Null while it is still being read**, which is a real state and not a
     * gap: the answers for cards that are not on Today are queried when this
     * sheet opens rather than on every Today focus, so for a moment the row has
     * its name and not its answer. **The name alone is honest and "Nothing
     * waiting" is not**, which is what this said for every entry until now.
     */
    val preview: String? = null,
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

/**
 * The three questions a project card can answer. `DESIGN.md` 21.7.
 *
 * **Named once**, because the gallery offers them and the sheet reads their
 * answers, and a list written twice is a list that gains a fourth entry in one
 * place only.
 */
internal val PROJECT_CARD_TYPES = listOf("project_standing", "project_date", "project_steps")

/**
 * What a person can still put on Today. `DESIGN.md` 21.6 screen 6.
 *
 * **Each offer previews what the card would say right now.** A name alone asks
 * somebody to imagine a screen they have never seen; "Medications, 6" is a
 * decision they can actually make.
 *
 * **A card already on Today is not offered again**, except the ones that point
 * at something: a second measure is a different card from the first, and
 * somebody tracking two things wants two.
 *
 * **The order is the binder's order.** Nothing here is ranked by what the app
 * thinks matters, because that would be the app having a view about somebody's
 * care.
 */
internal fun cardOffers(
    onScreen: List<Repository.TodayCard>,
    measures: List<Repository.Measure>,
    projects: List<Repository.Project>,
    answers: Map<String, Repository.TodayAnswer>,
    strings: Strings,
    /** What day it is, for the one card whose answer is a countdown. */
    today: LocalDate,
): List<CardOffer> {
    val taken = onScreen.map { it.type to it.sourceId }.toSet()

    fun label(type: String) = strings["today.card.$type"]

    /**
     * What this card would say right now, at the size the gallery previews.
     *
     * **Read for cards that are not on Today, which is the whole point and is
     * what was missing.** The previews came from the answers of the cards
     * already on the screen, and the gallery only ever offers the ones that are
     * not, so the lookup could never hit: **every entry in the gallery said
     * "Nothing waiting"** regardless of what the record held. Somebody choosing
     * between fourteen cards was reading fourteen identical lies.
     *
     * **Null while the read is still in flight**, which the row shows as its
     * name alone. That is honest and a sentence about somebody's record is not.
     */
    fun preview(type: String, key: String): String? {
        // **Through the card's own wording**, so the preview and the card can
        // never say different things about the same record.
        val answer = wordedAnswer(type, answers[key], today, strings) ?: return null
        val said: List<String?> = when {
            // A card pointing at something closed says so here too, so the
            // person is not offered a card and then shown a different one.
            answer.sourceClosed -> listOf(answer.title, strings["today.card.source_closed"])
            // **The card's own nothing, not a general one.** 21.4: the rung
            // names what is not there, and the gallery saying "Nothing
            // scheduled" under Next up is the same sentence the card will say.
            answer.isEmpty -> listOf(strings[emptyLineKey(type)])
            answer.title != null -> listOf(answer.title)
            // A number with the word that says what it counted, which is
            // exactly what the small size renders.
            (answer.count ?: 0) > 0 -> listOf(
                answer.count.toString(),
                countLineKey(type)?.let { strings[it] },
            )

            else -> emptyList()
        }
        // **The thing it points at, in front of its answer, joined once.**
        // "Weight · 138.8 lb" rather than either half alone: the name says
        // which card this is and the answer says what it would show.
        //
        // **Flat parts.** `Bidi.join` isolates everything it is handed, so
        // joining a joined string nests the marks and the row came out
        // `⁨Medicaid application⁩ · ⁨⁨4⁩ · ⁨steps in the plan⁩⁩`. The same defect
        // the cards had, found again by reading the semantics tree.
        return Bidi.join(listOf(answer.sourceName) + said)
            .takeIf { it.isNotBlank() }
    }

    // **The card in its plain form, which is the one with no source.** The care
    // team card comes in two, 21.7, and a card pointed at one person must not
    // stand in for the row of everyone: without this, choosing a person on the
    // only care team card took the card itself out of the gallery, so there was
    // no way back to everybody and no way to a second person.
    val plain = listOf(
        "digest", "next_up", "medications", "milestones", "ask_next_time",
        "incidents", "money", "unfiled", "emergency_card", "care_team",
        "trail_lately", "recent_documents", "standing_instructions",
    ).filterNot { type -> taken.contains(type to null) }
        .map { type -> CardOffer(type, label(type), preview(type, type)) }

    // **The answer names the row and the thing it points at is the second
    // line.** The other way round put three rows reading "Appeal the level of
    // care assessment / Project" next to each other, which are three different
    // cards and looked like the same one listed three times. Seen on the phone.
    val perMeasure = measures
        .filterNot { measure -> taken.contains("measure" to measure.id) }
        .map { measure ->
            CardOffer(
                type = "measure",
                label = strings["today.card.measure.long"],
                preview = preview("measure", "measure-${measure.id}")
                    ?: Bidi.isolate(measure.name),
                sourceTable = "measure",
                sourceId = measure.id,
            )
        }

    val perProject = projects
        .filterNot { it.isFinished }
        .flatMap { project ->
            PROJECT_CARD_TYPES
                .filterNot { type -> taken.contains(type to project.id) }
                .map { type ->
                    CardOffer(
                        type = type,
                        label = strings["today.card.$type.long"],
                        // The project's name until its answer arrives, because
                        // three rows reading only "The next date" cannot be
                        // told apart while the read is in flight.
                        preview = preview(type, "$type-${project.id}")
                            ?: Bidi.isolate(project.name),
                        sourceTable = "project",
                        sourceId = project.id,
                    )
                }
        }

    return plain + perMeasure + perProject
}
