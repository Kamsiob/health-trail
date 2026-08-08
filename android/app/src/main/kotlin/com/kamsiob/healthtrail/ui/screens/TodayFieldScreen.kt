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
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.CardSize
import com.kamsiob.healthtrail.ui.components.TabChip
import com.kamsiob.healthtrail.ui.components.TodayCard
import com.kamsiob.healthtrail.ui.components.TodayLead
import com.kamsiob.healthtrail.ui.components.UniversalSearchDoor
import com.kamsiob.healthtrail.ui.components.wholeAppHue
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.TabHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import java.time.LocalDate

object TodayFieldTags {
    const val ROOT = "today-field"
    const val LEAD = "today-lead"
    const val EDIT = "today-edit"
    const val DONE = "today-done"
    const val ADD = "today-add"
    const val SEARCH = "today-field-search"
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
 * **The lead wears the lead costume and not the card costume**, per 21.1 and
 * the `TodayLead` component. It sat in a wide card for a while, which put the
 * most important thing on the screen at exactly the weight of the four things
 * under it, and rule 15 calls uniform weight what it is: the whole job of
 * sorting pushed onto somebody already exhausted.
 *
 * **Search and capture keep their places regardless of layout.** 21.1: finding
 * and recording are the two acts that must never move, so the search door sits
 * under the lead and the gold capture button sits in the shell, while
 * everything between them is the person's to arrange.
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
     * Opens search, which is a whole screen with its own field.
     *
     * **`MASTER_SPEC.md` 4.8 puts universal search at the top of Today**, and
     * 21.1 says it stays there whatever the person does to the cards. When this
     * surface replaced the previous Today it arrived without one at all, so on
     * a real notebook there was no way to search from the front door.
     */
    onSearch: () -> Unit = {},
    /**
     * Saves a rearranged layout, in the order given, the first card leading.
     *
     * **Called once, from Done.** 21.6 screen 5: nothing saves behind your
     * back, so every change in edit mode is held here and written in one go.
     */
    onSave: (List<Repository.TodayCard>) -> Unit = {},
    /**
     * Opens the gallery, handing it the cards already on the draft.
     *
     * **The draft rather than the saved layout**, so a card added and then a
     * card removed in the same edit do not fight over what is already there.
     */
    onAddCard: (List<Repository.TodayCard>) -> Unit = {},
    /**
     * What changed since the person was last here.
     *
     * **The same summary the app already had**, computed once in the shell and
     * handed here, rather than a second digest written for this surface. Two
     * sentences about the same week that disagree is worse than one.
     */
    digest: com.kamsiob.healthtrail.data.Digest.Summary =
        com.kamsiob.healthtrail.data.Digest.nothing,
    /**
     * What day it is, for the lead's eyebrow.
     *
     * **Passed in rather than read here**, so a test can say what today is and
     * the screen has no clock of its own.
     */
    today: LocalDate = LocalDate.now(),
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

    // **Adding a card writes immediately and comes back through the layout**,
    // rather than being staged like a move. A person who taps Add expects the
    // card to be there; staging it would mean the gallery's Add did nothing
    // visible until Done, which is the opposite of what that button says.

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
            item(span = { GridItemSpan(2) }, key = "today-header") {
                Column {
                    Spacer(Modifier.height(Space.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Today belongs to no section, so gold and the base
                        // ladder, per 4.3. **A chip and not a display title**:
                        // the lead sentence directly under it is the display
                        // scale thing on this screen, and a heading at the same
                        // weight two lines above it would give the screen two
                        // first things. The word is already on the active
                        // navigation tab besides.
                        TabChip(hue = wholeAppHue(), labelKey = "today.tab")
                        Spacer(Modifier.weight(1f))
                        if (editing) {
                            TextAction(
                                label = strings["today.add"],
                                onClick = { onAddCard(draft) },
                                modifier = Modifier.testTag(TodayFieldTags.ADD),
                            )
                            Spacer(Modifier.width(Space.s))
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
                }
            }

            val shown = if (editing) draft else layout.all
            val lead = shown.first()
            val field = shown.drop(1)

            item(span = { GridItemSpan(2) }, key = "today-lead-slot") {
                LeadSlot(
                    card = lead,
                    answer = answers[lead.id] ?: digestAnswer(lead, digest, strings),
                    today = today,
                    onOpen = onOpen,
                    editing = editing,
                    // **Down is the lead's only control**, and it is how a lead
                    // is demoted: 21.1 says promoting a card demotes the one
                    // that was there, and this is the same move made from the
                    // other end. There is no Remove, because there is never
                    // zero, and no size chips, because a lead is not a size the
                    // person chose while it is leading.
                    canMoveDown = field.isNotEmpty(),
                    onMoveDown = {
                        draft = draft.toMutableList().apply { add(1, removeAt(0)) }
                    },
                )
            }

            // **Under the lead and above the field, always in that place.**
            // 21.1. Hidden only while editing, per grid screen 05, because
            // leaving for search mid-edit would throw away an unsaved draft and
            // a door that costs you your work is worse than no door.
            if (!editing) {
                item(span = { GridItemSpan(2) }, key = "today-search") {
                    UniversalSearchDoor(
                        onOpen = onSearch,
                        modifier = Modifier.testTag(TodayFieldTags.SEARCH),
                    )
                }
            }

            items(
                count = field.size,
                key = { field[it].id },
                span = { index -> GridItemSpan(if (field[index].size == "small") 1 else 2) },
            ) { index ->
                val card = field[index]
                // Its place in the whole surface, which is what a move has to
                // act on: the field is the layout with the lead taken off.
                val position = index + 1
                CardFor(
                    card = card,
                    answer = answers[card.id] ?: digestAnswer(card, digest, strings),
                    size = CardSize.of(card.size),
                    onOpen = onOpen,
                    modifier = Modifier.testTag(TodayFieldTags.card(card.id)),
                    editing = editing,
                    canMoveDown = index < field.lastIndex,
                    onMove = { earlier ->
                        val to = if (earlier) position - 1 else position + 1
                        draft = draft.toMutableList().apply { add(to, removeAt(position)) }
                    },
                    onResize = { size ->
                        draft = draft.toMutableList()
                            .also { it[position] = it[position].copy(size = size) }
                    },
                    onRemove = {
                        draft = draft.toMutableList().apply { removeAt(position) }
                    },
                    onPromote = {
                        draft = draft.toMutableList().apply { add(0, removeAt(position)) }
                    },
                )
            }

            item(span = { GridItemSpan(2) }, key = "today-fab-clearance") {
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}

/**
 * The one thing at the top. `DESIGN.md` 21.1.
 *
 * **The same answer the card would show, in the lead costume.** The body is
 * composed once for both, so the lead and the field can never word the same
 * answer differently, which is what would happen the first time either was
 * touched if there were two copies of it.
 */
@Composable
private fun LeadSlot(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    today: LocalDate,
    onOpen: (Repository.TodayCard) -> Unit,
    editing: Boolean,
    canMoveDown: Boolean,
    onMoveDown: () -> Unit,
) {
    val strings = LocalStrings.current
    val tab = strings["today.card.${card.type}"]

    // **The day for the digest, the card's own name for anything else.** The
    // digest's eyebrow is what day it is, which is a fact worth stating above a
    // sentence about today and is what the grid draws. Naming it "Today" there
    // would be the third time that word appears on the screen, after the chip
    // and the navigation tab. A card the person promoted says which card it is,
    // so they can see what they put at the top.
    val eyebrow = if (card.type == "digest") {
        EventDateText.dayHeading(strings, today)
    } else {
        tab
    }

    TodayLead(
        eyebrow = eyebrow,
        hue = hueForCard(card.type),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest.
        description = Bidi.join(
            listOf(eyebrow) + answerParts(
                answer,
                strings,
                showItems = true,
                countLine = countLineKey(card.type)?.let { strings[it] },
            ),
        ),
        openLabel = strings("today.card.open", "name" to tab),
        onOpen = { onOpen(card) },
        modifier = Modifier.testTag(TodayFieldTags.LEAD),
        speaksAsOneNode = !editing,
    ) {
        AnswerBody(answer = answer, cardType = card.type, lead = true, showDetail = true)

        if (editing && canMoveDown) {
            Row(modifier = Modifier.padding(top = Space.s)) {
                EditAction(
                    label = strings["today.edit.down.short"],
                    spoken = strings("today.edit.down", "name" to tab),
                    onClick = onMoveDown,
                )
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
    canMoveDown: Boolean = false,
    onMove: (earlier: Boolean) -> Unit = {},
    onResize: (String) -> Unit = {},
    onRemove: () -> Unit = {},
    onPromote: () -> Unit = {},
) {
    val strings = LocalStrings.current

    val tab = strings["today.card.${card.type}"]

    TodayCard(
        tab = tab,
        hue = hueForCard(card.type),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest. The same defect was fixed on the
        // project screen four commits ago and written straight back in here.
        description = Bidi.join(
            listOf(tab) + answerParts(
                answer,
                strings,
                showItems = size != CardSize.SMALL,
                countLine = countLineKey(card.type)?.let { strings[it] },
            ),
        ),
        onOpen = { onOpen(card) },
        openLabel = strings("today.card.open", "name" to tab),
        size = size,
        modifier = modifier,
        // In edit mode the card holds controls, and a node that speaks as one
        // thing would swallow them.
        speaksAsOneNode = !editing,
    ) {
        // The second line appears at wide and tall only, per 21.3: at small the
        // card carries one answer and one line of context, and that line is the
        // answer's own.
        AnswerBody(
            answer = answer,
            cardType = card.type,
            lead = false,
            showDetail = size != CardSize.SMALL,
        )

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
                    EditAction(
                        label = strings["today.edit.up.short"],
                        spoken = strings("today.edit.up", "name" to tab),
                        onClick = { onMove(true) },
                    )
                    if (canMoveDown) {
                        EditAction(
                            label = strings["today.edit.down.short"],
                            spoken = strings("today.edit.down", "name" to tab),
                            onClick = { onMove(false) },
                        )
                    }
                    // **Promoting to the lead is its own action**, 21.1,
                    // because reaching the top by tapping Move up eleven times
                    // is not the same offer. Promoting demotes the card that
                    // was there back into the field, which is what moving to
                    // position zero does.
                    EditAction(
                        label = strings["today.edit.lead.short"],
                        spoken = strings("today.edit.lead", "name" to tab),
                        onClick = onPromote,
                    )
                    EditAction(
                        label = strings["today.edit.remove.short"],
                        spoken = strings("today.edit.remove", "name" to tab),
                        onClick = onRemove,
                    )
                }
            }
        }
    }
}

/**
 * What the record says, in one place for both costumes.
 *
 * **The lead and a card show the same answer at different scales**, never
 * different answers. Two renderings of one query is two renderings that
 * disagree the first time either is touched.
 */
@Composable
private fun AnswerBody(
    answer: Repository.TodayAnswer?,
    /** The card's type, for the one line of context that names what the count counts. */
    cardType: String,
    lead: Boolean,
    /**
     * Whether this card is wide enough for a second line of context.
     *
     * **Small still gets one**, per 21.3: half width carries one answer and one
     * line of context, not an answer alone. A small Next up card showing a name
     * and nothing else is not answering the question it names.
     */
    showDetail: Boolean,
) {
    val colors = HealthTrail.colors
    val type = HealthTrail.type
    val strings = LocalStrings.current

    // **When it is, which nothing rendered until 2026-08-08.** `whenEdtf` is
    // read from the record for Next up, Milestones and a measure, carried
    // through `TodayAnswer`, and was dropped on the floor by every renderer, so
    // the card whose whole question is "what is the next dated thing" showed a
    // name and no date. Same family as a control that kept its repository call
    // through a supersession: read, carried, compiled, never on screen.
    //
    // **Through `EventDateText`, so precision is never invented**, per rule 17
    // and 9.2. A month stays a month here exactly as it does in the trail.
    val whenText = answer?.whenEdtf?.takeIf { it.isNotBlank() }
        ?.let { EventDateText.render(strings, it) }

    // **A zero is not an answer worth shouting.** Rendering the number whatever
    // it was put a large 0 directly above "Nothing waiting", which says the
    // same thing twice and, at that weight, reads as a score on a person who
    // has just started. The empty rung is the sentence alone.
    val count = answer?.count?.takeIf { it > 0 }
    if (count != null && answer.title == null) {
        Text(
            text = Bidi.isolate(count.toString()),
            style = if (lead) type.hero else type.monoL,
            color = colors.ink,
        )
        // **A number with no noun is not an answer.** 21.3 gives every size one
        // line of context and the smallest size is not exempt: a card reading
        // "6" alone makes the person supply the word themselves, on a screen
        // whose whole job is that they should not have to. The grid says "on
        // the current list" under the 6, and this is that line.
        countLineKey(cardType)?.let {
            Text(text = strings[it], style = type.bodyS, color = colors.ink2)
        }
    }
    answer?.title?.let {
        Text(
            text = Bidi.isolate(it),
            style = if (lead) type.hero else type.displayS,
            color = colors.ink,
            // **The lead wraps freely and a card does not.** D105: a fixed cap
            // is a cap at the smallest type size and a truncation at the
            // largest, and the lead's sentence is the one thing the screen
            // exists to say. A card in a half-width cell has four others beside
            // it and cannot grow to six lines without taking the fold.
            maxLines = if (lead) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    // **The source-closed rung**, 21.4. It says so plainly and keeps working as
    // a door, and the card stays until the person's own hand removes it.
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
        // saying "Nothing waiting" here would be the app asserting something
        // false about somebody's record. One broken query once made every card
        // on this surface say it at the same time.
        Text(
            text = strings["today.card.unread"],
            style = if (lead) type.hero else type.bodyS,
            color = if (lead) colors.ink else colors.ink2,
        )
    } else if (answer.isEmpty && !answer.sourceClosed) {
        // **A calm state, never a scold**, 21.4. Quiet is allowed to be good
        // news, and this says so rather than leaving a hole. At the lead it is
        // said at lead scale, because the answer to the question the person
        // opened the app to ask is still the answer when it is a quiet one.
        Text(
            text = strings["today.card.nothing"],
            style = if (lead) type.hero else type.bodyS,
            color = if (lead) colors.ink else colors.ink2,
        )
    }

    // **The list, at wide and tall only.** 21.3 uses this card as its example:
    // the medications card at small is a count and at wide it is the list, and
    // both are the one question "what is on the list right now" asked once.
    // Growing reveals more of the same answer, never a new kind of content, so
    // this is never a different query and never a feed.
    if (showDetail && answer != null && answer.items.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = Space.xs)) {
            for (item in answer.items) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        // The parts joined here rather than in the query,
                        // because joining is wording. `Bidi.join` isolates each
                        // part, so a medication somebody typed in Arabic keeps
                        // its own direction beside a dose typed in English, and
                        // a date never renders as the EDTF it is stored as.
                        text = Bidi.join(
                            item.label,
                            item.note,
                            item.noteEdtf?.let { EventDateText.render(strings, it) },
                        ),
                        style = type.bodyS,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // **Amounts at the end edge, in tabular mono.** 21.7 says
                    // amounts only at wide and right aligned, and section 7
                    // puts every amount in the mono face so a column of them
                    // lines up on the decimal point. `Alignment.End` rather
                    // than right, so it follows the reading direction.
                    item.amountMinor?.let { minor ->
                        Spacer(Modifier.width(Space.s))
                        Text(
                            text = Bidi.isolate(
                                formatMoney(strings, minor, item.currency ?: "USD"),
                            ),
                            style = type.mono,
                            color = colors.ink,
                            maxLines = 1,
                        )
                    }
                }
            }
            // **What is not here, said rather than cropped.** The count is the
            // true total, so a card showing three of eleven says so. Silently
            // showing the first three would be the app deciding which
            // medications matter, which is exactly what it must not do.
            val hidden = (answer.count ?: 0) - answer.items.size
            if (hidden > 0) {
                Text(
                    text = strings("today.card.more", "count" to hidden),
                    style = type.bodyS,
                    color = colors.ink2,
                )
            }
        }
    }

    // **The context lines, and growing reveals more of the same answer.** 21.3:
    // the first line is the one line of context every size carries, the second
    // appears at wide and tall. **When it is comes first** where the record has
    // it, because a date is the context on every card that carries one, and the
    // detail follows. Neither is a new kind of content at the larger size.
    val lines = listOfNotNull(whenText, answer?.detail?.takeIf { it.isNotBlank() })
        .let { if (showDetail) it else it.take(1) }
    for (line in lines) {
        Text(
            text = Bidi.isolate(line),
            style = type.bodyS,
            color = colors.ink2,
            maxLines = if (lead) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Space.xs),
        )
    }
}

/**
 * The word under a card's number, naming what it counted.
 *
 * **Every key here is a literal and that is the whole reason this function
 * exists.** `check_string_keys.py` reads the code's keys against the catalog and
 * **skips anything built at runtime**, by design, because a key assembled from a
 * variable cannot be resolved by reading the source. Its stated safety net for
 * those is the instrumented suite rendering the screen, which is a net with a
 * hole in it on any night the phone cannot be driven. `"today.card.$type.count"`
 * would have been exactly the shape that took the app down once already:
 * `ChangeSituationScreen` asked for `more.title`, which has never existed, and
 * `Strings.resolve` throws rather than falling back.
 *
 * **Null for a card that does not answer with a number**, so a type that gains a
 * count later fails the `when` here rather than inventing a key.
 */
private fun countLineKey(cardType: String): String? = when (cardType) {
    "medications" -> "today.card.medications.count"
    "incidents" -> "today.card.incidents.count"
    "unfiled" -> "today.card.unfiled.count"
    "money" -> "today.card.money.count"
    "care_team" -> "today.card.care_team.count"
    "recent_documents" -> "today.card.recent_documents.count"
    "standing_instructions" -> "today.card.standing_instructions.count"
    "ask_next_time" -> "today.card.ask_next_time.count"
    "emergency_card" -> "today.card.emergency_card.count"
    "trail_lately" -> "today.card.trail_lately.count"
    // The digest, the next dated thing, a measure, a milestone and the project
    // cards all answer with a thing rather than a quantity, so their context
    // line is the answer's own date or detail.
    else -> null
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
    /**
     * Whether the card is showing its list.
     *
     * **The reader hears what is on the screen and not what is in the model.**
     * Section 9: what is read aloud says the same thing the screen says. A small
     * card announcing three medications it is not displaying is as wrong as a
     * wide one that shows them and stays silent.
     */
    showItems: Boolean = false,
    /** The word under the number, which the eye gets and the ear was not getting. */
    countLine: String? = null,
): List<String?> = when {
    answer == null -> listOf(strings["today.card.unread"])
    answer.sourceClosed -> listOf(answer.title, strings["today.card.source_closed"])
    answer.isEmpty -> listOf(strings["today.card.nothing"])
    else -> buildList {
        add(answer.count?.takeIf { it > 0 }?.toString())
        if (answer.count != null && answer.count > 0 && answer.title == null) add(countLine)
        add(answer.title)
        // **The date the screen shows**, per section 9. It was absent here for
        // as long as it was absent there, so the omission was consistent and
        // wrong twice.
        add(answer.whenEdtf?.takeIf { it.isNotBlank() }?.let { EventDateText.render(strings, it) })
        if (showItems) {
            for (item in answer.items) {
                add(
                    Bidi.join(
                        item.label,
                        item.note,
                        item.noteEdtf?.let { EventDateText.render(strings, it) },
                        item.amountMinor?.let {
                            formatMoney(strings, it, item.currency ?: "USD")
                        },
                    ),
                )
            }
            val hidden = (answer.count ?: 0) - answer.items.size
            if (answer.items.isNotEmpty() && hidden > 0) {
                add(strings("today.card.more", "count" to hidden))
            }
        }
        add(answer.detail)
    }
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
