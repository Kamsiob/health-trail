package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.Radius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    const val EDIT = "today-edit"
    const val DONE = "today-done"
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
     * Saves a rearranged layout, in the order given, the first card leading.
     *
     * **Called once, from Done.** 21.6 screen 5: nothing saves behind your
     * back, so every change in edit mode is held here and written in one go.
     */
    onSave: (List<Repository.TodayCard>) -> Unit = {},
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

    // **Edit mode is entered by a visible button**, 21.6 screen 5. Touch and
    // hold is a shortcut and never the only path, because a gesture nobody is
    // told about is a feature that does not exist for most people.
    var editing by rememberSaveable { mutableStateOf(false) }

    // **The staged layout.** Held here while editing and written once, so a
    // person can move three cards and change their mind about all of them.
    var draft by remember(layout, editing) { mutableStateOf(layout.all) }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Today belongs to no section, so gold and the base
                        // ladder, per 4.3.
                        TabChip(hue = wholeAppHue(), labelKey = "today.tab")
                        Spacer(Modifier.weight(1f))
                        if (editing) {
                            TextAction(
                                label = strings["today.edit.cancel"],
                                onClick = { editing = false },
                            )
                            Spacer(Modifier.width(Space.s))
                            TextAction(
                                label = strings["today.edit.done"],
                                onClick = {
                                    onSave(draft)
                                    editing = false
                                },
                                modifier = Modifier.testTag(TodayFieldTags.DONE),
                            )
                        } else {
                            TextAction(
                                label = strings["today.edit"],
                                onClick = { editing = true },
                                modifier = Modifier.testTag(TodayFieldTags.EDIT),
                            )
                        }
                    }
                    if (editing) {
                        Text(
                            text = strings["today.edit.hint"],
                            style = HealthTrail.type.bodyS,
                            color = colors.ink2,
                            modifier = Modifier.padding(top = Space.xs),
                        )
                    }
                    Spacer(Modifier.height(Space.s))
                }
            }

            val shown = if (editing) draft else layout.all

            items(
                count = shown.size,
                key = { shown[it].id },
                span = { index ->
                    // **The first card always spans**, editing or not: it is
                    // the lead, and the lead is never half a screen wide.
                    GridItemSpan(if (index > 0 && shown[index].size == "small") 1 else 2)
                },
            ) { index ->
                val card = shown[index]
                val isLead = index == 0
                CardFor(
                    card = card,
                    answer = answers[card.id] ?: digestAnswer(card, digest, strings),
                    size = if (isLead) CardSize.WIDE else CardSize.of(card.size),
                    onOpen = onOpen,
                    modifier = Modifier.testTag(
                        if (isLead) TodayFieldTags.LEAD else TodayFieldTags.card(card.id),
                    ),
                    editing = editing,
                    canMoveUp = index > 0,
                    canMoveDown = index < shown.lastIndex,
                    onMove = { earlier ->
                        val to = if (earlier) index - 1 else index + 1
                        draft = draft.toMutableList().apply { add(to, removeAt(index)) }
                    },
                    onResize = { size ->
                        draft = draft.toMutableList()
                            .also { it[index] = it[index].copy(size = size) }
                    },
                    onRemove = {
                        // **The lead cannot be removed**, because there is
                        // never zero. Edit mode does not offer it.
                        if (!isLead) {
                            draft = draft.toMutableList().apply { removeAt(index) }
                        }
                    },
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
    editing: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMove: (earlier: Boolean) -> Unit = {},
    onResize: (String) -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    val tab = strings["today.card.${card.type}"]

    TodayCard(
        tab = tab,
        hue = hueForCard(card.type),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest. The same defect was fixed on the
        // project screen four commits ago and written straight back in here.
        description = Bidi.join(listOf(tab) + answerParts(answer, strings)),
        onOpen = { onOpen(card) },
        openLabel = strings("today.card.open", "name" to tab),
        size = size,
        modifier = modifier,
        // In edit mode the card holds controls, and a node that speaks as one
        // thing would swallow them.
        speaksAsOneNode = !editing,
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
        // **The source-closed rung**, 21.4. It says so plainly and keeps
        // working as a door, and the card stays until the person's own hand
        // removes it.
        if (answer?.sourceClosed == true) {
            Text(
                text = strings["today.card.source_closed"],
                style = type.bodyS,
                color = colors.ink2,
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
        } else if (answer.isEmpty && !answer.sourceClosed) {
            // **A calm state, never a scold**, 21.4. Quiet is allowed to be
            // good news, and this says so rather than leaving a hole.
            Text(
                text = strings["today.card.nothing"],
                style = type.bodyS,
                color = colors.ink2,
            )
        }
        // **The controls, and only while editing.** 21.6 screen 7 gives Move
        // up and Move down as the accessible reorder path, so reordering works
        // one-handed, with the reader on, and with switch access. Drag is a
        // shortcut on top of this, never instead of it.
        if (editing) {
            Column(modifier = Modifier.padding(top = Space.s)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    for (option in listOf("small", "wide", "tall")) {
                        SizeChip(
                            label = strings["today.edit.size.$option"],
                            selected = card.size == option,
                            onClick = { onResize(option) },
                        )
                    }
                }
                // **The word is short and the sentence is the reader's.**
                // "Move Medications down" is a correct sentence and it is far
                // too long for a half width card: it wrapped to one letter per
                // line and stretched the card to four times its height. Seen on
                // the phone. The visible word is Up, Down, Remove; the reader
                // still hears which card it moves, which is the part that
                // matters when you cannot see the card it is sitting on.
                Row(
                    modifier = Modifier.padding(top = Space.xs),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    if (canMoveUp) {
                        EditAction(
                            label = strings["today.edit.up.short"],
                            spoken = strings("today.edit.up", "name" to tab),
                            onClick = { onMove(true) },
                        )
                    }
                    if (canMoveDown) {
                        EditAction(
                            label = strings["today.edit.down.short"],
                            spoken = strings("today.edit.down", "name" to tab),
                            onClick = { onMove(false) },
                        )
                    }
                    if (canMoveUp) {
                        // **Only a card that is not the lead can be removed**,
                        // because there is never zero, and the lead is the one
                        // card that cannot move up.
                        EditAction(
                            label = strings["today.edit.remove.short"],
                            spoken = strings("today.edit.remove", "name" to tab),
                            onClick = onRemove,
                        )
                    }
                }
            }
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
private fun answerParts(
    answer: Repository.TodayAnswer?,
    strings: Strings,
): List<String?> = when {
    answer == null -> listOf(strings["today.card.unread"])
    answer.sourceClosed -> listOf(answer.title, strings["today.card.source_closed"])
    answer.isEmpty -> listOf(strings["today.card.nothing"])
    else -> listOf(
        answer.count?.takeIf { it > 0 }?.toString(),
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

/**
 * One of the three sizes, as a chip. `DESIGN.md` 21.6 screen 5.
 *
 * **Selected is a fill and not only a color**, per section 9, so it survives
 * grayscale and every color vision difference.
 */
@Composable
private fun SizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HealthTrail.colors
    Text(
        text = label,
        style = HealthTrail.type.mono,
        color = if (selected) colors.paper else colors.ink2,
        modifier = Modifier
            .clip(Radius.pill)
            .background(if (selected) colors.ink else colors.sand)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.xs),
    )
}

/**
 * One edit control: a short word to look at, a whole sentence to hear.
 *
 * **Both are needed and they are not the same string.** A card in the field is
 * half a screen wide, so a label naming the card does not fit; a reader moving
 * through eight of these hears "Up" eight times and learns nothing. The word is
 * for the eye and [spoken] is for the ear.
 */
@Composable
private fun EditAction(label: String, spoken: String, onClick: () -> Unit) {
    val colors = HealthTrail.colors
    Text(
        text = label,
        style = HealthTrail.type.mono,
        color = colors.blueDeep,
        maxLines = 1,
        modifier = Modifier
            .clip(Radius.pill)
            .clickable(onClickLabel = spoken, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken }
            .padding(horizontal = Space.s, vertical = Space.s),
    )
}
