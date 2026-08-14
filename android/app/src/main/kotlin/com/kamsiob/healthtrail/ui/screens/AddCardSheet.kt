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
import com.kamsiob.healthtrail.ui.SECTION_ORDER
import com.kamsiob.healthtrail.ui.sectionForCard
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.CardSize
import com.kamsiob.healthtrail.ui.components.TodayCard
import com.kamsiob.healthtrail.ui.components.GroupHeader
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
    /**
     * The catalog key for the heading this offer sits under.
     *
     * **The binder's own section names**, 21.6 screen 6, so the gallery teaches
     * nothing new: a card lives where its answer lives, and somebody looking
     * for the medications card looks under Medications.
     */
    val groupKey: String = "",
    /**
     * Whether the person's stated situation puts this card on Today by default.
     *
     * **The situation's suggestions come first**, 21.6 screen 6, and 21.5 is
     * why that is not the app having a view: personalization here is the
     * person's explicit choice plus the situation they told it about, never
     * inference, because this app does not watch its user.
     *
     * **In practice this group is empty**, because a situation's starting hand
     * is already on Today. It fills up for somebody who took cards off and came
     * back looking for one, which is exactly who it is for.
     */
    val suggested: Boolean = false,
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

        // **Grouped under the binder's own section names**, 21.6 screen 6, with
        // the situation's suggestions first. A flat list of seventeen offers is
        // the wall the curated catalog exists to prevent, and the grouping is
        // the one the person already knows from the notebook rather than a
        // second organizing idea to learn.
        //
        // **The order inside a group is the order it arrived in**, which is the
        // catalog's, and nothing is ranked by what the app thinks matters.
        for ((groupKey, group) in offers.groupedForGallery()) {
            item(key = "add-card-group-$groupKey") {
                GroupHeader(
                    labelKey = groupKey,
                    modifier = Modifier.padding(top = Space.m, bottom = Space.xs),
                )
            }
            items(group.size, key = { group[it].key }) { index ->
                val offer = group[index]
                // **The card itself, not a line of type describing it.** This
                // was a list of names with a preview underneath, and choosing
                // from it meant reading seventeen rows of words to picture
                // seventeen cards. **The picker on the phone somebody already
                // owns shows the widget**, which is the whole of the owner's
                // note on #376: that is the only frame of reference anybody
                // brings here.
                //
                // **It wears the hue and the shape it will wear on Today**, so
                // what is chosen and what arrives are the same object seen
                // twice rather than a description and a thing. D157.
                TodayCard(
                    tab = Bidi.isolate(offer.label),
                    hue = hueForCard(offer.type),
                    // **What a reader hears, composed here**, because the card
                    // silences its own insides so it is one stop rather than
                    // three. Same rule as on Today.
                    description = Bidi.join(listOf(offer.label, offer.preview)),
                    onOpen = { onAdd(offer) },
                    // **A verb that says what the tap does**, and here it adds
                    // rather than opens. A preview labeled "open" would promise
                    // a door that is not there.
                    openLabel = strings("today.add.this", "name" to offer.label),
                    size = CardSize.WIDE,
                    modifier = Modifier
                        .padding(bottom = Space.cardGap)
                        .testTag(AddCardTags.entry(offer.key)),
                ) {
                    // **The preview wraps, because it is the thing being read
                    // to choose.** D105: a fixed cap is a cap at the smallest
                    // type size and a truncation at the largest, and at font
                    // scale 2.0 every row here ended mid-word with no ellipsis:
                    // "passed 75 days", "4 steps in the". The whole promise of
                    // this screen is that the answer is on it.
                    Text(
                        text = offer.preview.orEmpty(),
                        style = type.displayS,
                        color = colors.ink,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(Space.xxl)) }
    }
}

/**
 * The offers, in the order and the groups the gallery draws them.
 *
 * **The situation's suggestions first, then the binder's order**, 21.6 screen 6.
 * Those are two rules rather than one and they do not conflict: the suggestions
 * are a group of their own at the top, and everything else falls into the
 * section its answer lives in, in the order the notebook already uses.
 *
 * **A card is in one group only.** A suggested card is not repeated under its
 * section, because a person scanning for one card twice is a person who thinks
 * there are two.
 *
 * **The order inside a group is the order it arrived in**, which is the
 * catalog's. Nothing here is ranked by what the app thinks matters.
 */
internal fun List<CardOffer>.groupedForGallery(): List<Pair<String, List<CardOffer>>> {
    val suggested = filter { it.suggested }
    val rest = filterNot { it.suggested }
    val order = GALLERY_GROUP_ORDER.withIndex().associate { (at, key) -> key to at }
    val groups = rest.groupBy { it.groupKey }
        .toList()
        // A group the order does not name goes last rather than disappearing,
        // which is what a new card type would do before anybody updated a list.
        .sortedBy { (key, _) -> order[key] ?: order.size }
    return if (suggested.isEmpty()) groups else listOf(SUGGESTED_GROUP to suggested) + groups
}

/** The heading over the cards the person's stated situation starts them with. */
private const val SUGGESTED_GROUP = "today.add.suggested"

/**
 * The binder's order, as the catalog keys the gallery heads its groups with.
 *
 * **Derived from `SECTION_ORDER` rather than written out again**, so a section
 * that moves in the notebook moves here too. Projects are not a binder section,
 * so they sit after the binder, which is where the navigation puts them.
 */
internal val GALLERY_GROUP_ORDER: List<String> =
    SECTION_ORDER.map { labelKey(it) } + "notebook.section.projects"

/**
 * The heading an offer sits under. `DESIGN.md` 21.6 screen 6.
 *
 * **A card lives where its answer lives**, so this is the same map the card's
 * own door uses: somebody looking for the medications card looks under
 * Medications, and the gallery does not teach a second organizing idea.
 */
internal fun galleryGroupKey(type: String): String = when {
    type in PROJECT_CARD_TYPES -> "notebook.section.projects"
    else -> sectionForCard(type)?.let { labelKey(it) } ?: "notebook.section.trail"
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
    /**
     * The card types the person's stated situation starts them with.
     *
     * 21.6 screen 6 puts these first. **Empty is the ordinary case**, both for
     * somebody who skipped the situation question and for anybody who still has
     * their whole starting hand on Today, since a card already there is not
     * offered at all.
     */
    suggested: Set<String> = emptySet(),
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
        .map { type ->
            CardOffer(
                type = type,
                label = label(type),
                preview = preview(type, type),
                groupKey = galleryGroupKey(type),
                suggested = type in suggested,
            )
        }

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
                groupKey = galleryGroupKey("measure"),
                suggested = "measure" in suggested,
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
                        groupKey = galleryGroupKey(type),
                        suggested = type in suggested,
                    )
                }
        }

    return plain + perMeasure + perProject
}
