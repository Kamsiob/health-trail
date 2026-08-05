package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.CardSize
import com.kamsiob.healthtrail.ui.components.TabChip
import com.kamsiob.healthtrail.ui.components.TodayCard
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.hueFor

object TodayFieldTags {
    const val ROOT = "today-field"
    const val LEAD = "today-lead"
    fun card(id: String) = "today-card-$id"
}

/**
 * Today, as the person arranged it. `DESIGN.md` 21.
 *
 * **Exactly one lead slot at the top, then the card field.** 21.1: a free-form
 * dashboard breaks law 1, because if six equal cards can be stacked then no
 * screen has one thing first. The resolution is a fixed structure with free
 * contents, and the singularity is not a convention here: the lead comes from
 * `Repository.TodayLayout`, which has nowhere to put zero or two.
 *
 * **This screen never rearranges anything.** 21.8. It renders the order it is
 * given and does nothing else with it: there is no promotion, no injection, no
 * sorting by recency or urgency, and no card appears because the data changed.
 * The difference between a quiet day and the morning of an appointment is data
 * inside a layout the person owns, and that distinction is the whole trust
 * model of the surface.
 *
 * **Nothing here interprets.** No trend is called good, nothing is colored by
 * value, and quiet is allowed to be good news rather than being dressed up as
 * a problem to fix.
 */
@Composable
fun TodayFieldScreen(
    layout: Repository.TodayLayout,
    /** One answer per card instance, keyed by the card's own id. */
    answers: Map<String, Repository.TodayAnswer>,
    onOpen: (Repository.TodayCard) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What changed since the person was last here.
     *
     * **The same summary the app already had**, computed once in the shell and
     * handed here, rather than a second digest written for this surface. Two
     * sentences about the same week that disagree is worse than one.
     */
    digest: com.kamsiob.healthtrail.data.Digest.Summary =
        com.kamsiob.healthtrail.data.Digest.nothing,
) {
    val colors = HealthTrail.colors
    val strings = LocalStrings.current

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .testTag(TodayFieldTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(Space.cardGap),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Spacer(Modifier.height(Space.sm))
                    // Today belongs to no section, so gold and the base ladder,
                    // per 4.3.
                    TabChip(hue = wholeAppHue(), labelKey = "today.tab")
                    Spacer(Modifier.height(Space.s))
                }
            }

            // **The lead, at its own weight and always exactly one.**
            item(span = { GridItemSpan(2) }) {
                CardFor(
                    card = layout.lead,
                    answer = answers[layout.lead.id]
                        ?: digestAnswer(layout.lead, digest, strings),
                    size = CardSize.WIDE,
                    onOpen = onOpen,
                    modifier = Modifier.testTag(TodayFieldTags.LEAD),
                )
            }

            items(
                count = layout.field.size,
                key = { layout.field[it].id },
                span = { index ->
                    GridItemSpan(if (layout.field[index].size == "small") 1 else 2)
                },
            ) { index ->
                val card = layout.field[index]
                CardFor(
                    card = card,
                    answer = answers[card.id] ?: digestAnswer(card, digest, strings),
                    size = CardSize.of(card.size),
                    onOpen = onOpen,
                    modifier = Modifier.testTag(TodayFieldTags.card(card.id)),
                )
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}

@Composable
private fun CardFor(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    size: CardSize,
    onOpen: (Repository.TodayCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    val tab = strings["today.card.${card.type}"]
    val sentence = sentenceFor(card, answer, strings)

    TodayCard(
        tab = tab,
        hue = hueForCard(card.type),
        description = Bidi.join(tab, sentence),
        onOpen = { onOpen(card) },
        openLabel = strings("today.card.open", "name" to tab),
        size = size,
        modifier = modifier,
    ) {
        // **The answer, and the same answer at every size.** 21.3: growing a
        // card reveals more of the same answer and never a new kind of content.
        // **A zero is not an answer worth shouting.** Rendering the number
        // whatever it was put a large 0 directly above "Nothing waiting", which
        // says the same thing twice and, at that weight, reads as a score on a
        // person who has just started. The empty rung is the sentence alone.
        val count = answer?.count?.takeIf { it > 0 }
        if (count != null && answer.title == null) {
            Text(
                text = Bidi.isolate(count.toString()),
                style = type.monoL,
                color = colors.ink,
            )
        }
        answer?.title?.let {
            Text(
                text = Bidi.isolate(it),
                style = type.displayS,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (answer == null) {
            // **Not the same thing as nothing waiting.** A card whose question
            // could not be asked has not learned that the record is empty, and
            // saying "Nothing waiting" here would be the app asserting
            // something false about somebody's record. One broken query once
            // made every card on this surface say it at the same time.
            Text(
                text = strings["today.card.unread"],
                style = type.bodyS,
                color = colors.ink2,
            )
        } else if (answer.isEmpty) {
            // **A calm state, never a scold**, 21.4. Quiet is allowed to be
            // good news, and this says so rather than leaving a hole.
            Text(
                text = strings["today.card.nothing"],
                style = type.bodyS,
                color = colors.ink2,
            )
        }
        // The second line appears at wide and tall only, per 21.3: at small the
        // card carries one answer and one line of context, and that line is the
        // answer's own.
        if (size != CardSize.SMALL) {
            answer?.detail?.let {
                Text(
                    text = Bidi.isolate(it),
                    style = type.bodyS,
                    color = colors.ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * What a reader hears instead of the card.
 *
 * One sentence: the tab, then what the record says. A reader stopping on the
 * number, the line and the chevron separately would make somebody listen to
 * three things to learn one.
 */
private fun sentenceFor(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    strings: Strings,
): String = when {
    answer == null -> strings["today.card.unread"]
    answer.isEmpty -> strings["today.card.nothing"]
    else -> Bidi.join(
        answer.count?.toString(),
        answer.title,
        answer.detail,
    )
}

/**
 * The card's hue, from the tab pack. `DESIGN.md` 21.2 and 21.7.
 *
 * **Identity, never state.** An appointments card is slate because appointments
 * are slate everywhere in the binder, and the hue does not change with what the
 * answer says. Cards for whole-app surfaces wear gold.
 */
@Composable
private fun hueForCard(type: String): TabHue = when (type) {
    "next_up" -> hueFor(Repository.Section.APPOINTMENTS)
    "medications" -> hueFor(Repository.Section.MEDICATIONS)
    "ask_next_time" -> hueFor(Repository.Section.ASK_NEXT_TIME)
    "measure", "milestones" -> hueFor(Repository.Section.PROGRESS)
    "money" -> hueFor(Repository.Section.MONEY)
    "recent_documents" -> hueFor(Repository.Section.DOCUMENTS)
    "care_team" -> hueFor(Repository.Section.CARE_TEAM)
    "standing_instructions" -> hueFor(Repository.Section.STANDING_INSTRUCTIONS)
    // **The one card where alert is an identity**, 21.7, and the emergency card
    // screen already wears it for the same reason: alert is what it is for.
    "emergency_card", "incidents" -> hueFor(Repository.Section.EMERGENCY_CARD)
    // The digest, the trail, the projects and the unfiled tray belong to no
    // section, so gold and the base ladder. 4.3.
    else -> wholeAppHue()
}

/**
 * The digest card's answer, from the summary the shell already computed.
 *
 * **Null for any other card**, so this cannot quietly become a fallback that
 * makes an unanswered card look answered.
 */
private fun digestAnswer(
    card: Repository.TodayCard,
    digest: com.kamsiob.healthtrail.data.Digest.Summary,
    strings: Strings,
): Repository.TodayAnswer? {
    if (card.type != "digest") return null
    // **Counted, never judged.** It says how many things are new, which is a
    // fact about the record. It never says whether that is a lot, whether the
    // person has been away too long, or what any of it means, and a quiet week
    // is stated as a quiet week rather than as a gap to explain. 21.4's
    // returning rung: no card ever measures the person's absence back at them.
    val fresh = digest.newThings
    return Repository.TodayAnswer(
        title = if (fresh > 0) {
            strings("today.card.digest.new", "count" to fresh)
        } else {
            strings["today.card.digest.quiet"]
        },
    )
}
