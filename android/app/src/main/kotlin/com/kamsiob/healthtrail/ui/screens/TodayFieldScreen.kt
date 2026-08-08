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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.ui.components.Thumbnail
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChartHeight
import com.kamsiob.healthtrail.ui.components.Plot
import com.kamsiob.healthtrail.ui.components.chartPoints
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
    /**
     * Whether anything has ever been written down.
     *
     * **Only the digest asks**, and only to avoid telling somebody on their
     * first morning what has changed since a visit they never made. Counted in
     * the shell, which already has the section totals, rather than inferred
     * here from every card being empty: a broken query would look identical.
     */
    hasAnything: Boolean = true,
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

    // **One column once the type is large enough that half a screen is not a
    // card.** 21.6 screen 8: the field reflows and every card renders at full
    // width with nothing clipped, and the layout order is preserved exactly,
    // which a one column grid gives for free. **Designed, not endured.**
    //
    // At scale 2.0 a half width cell on a 360dp screen is about 170dp, and a
    // card there is a tab, a number and a wrapped three word line stacked into
    // a column barely wider than the words. The threshold is 1.5 because that
    // is where the second column stops being able to hold a card rather than
    // where the person has reached the end of the slider: somebody at 1.7 has
    // the same problem as somebody at 2.0 and should not have to go further to
    // get the fix.
    val oneColumn = LocalDensity.current.fontScale >= WIDE_TYPE_SCALE

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (oneColumn) 1 else 2),
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .testTag(TodayFieldTags.ROOT)
                .padding(horizontal = Space.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(Space.cardGap),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            val fullWidth = GridItemSpan(if (oneColumn) 1 else 2)

            item(span = { fullWidth }, key = "today-header") {
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

            item(span = { fullWidth }, key = "today-lead-slot") {
                LeadSlot(
                    card = lead,
                    answer = answers[lead.id] ?: digestAnswer(lead, digest, strings, hasAnything),
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
                item(span = { fullWidth }, key = "today-search") {
                    UniversalSearchDoor(
                        onOpen = onSearch,
                        modifier = Modifier.testTag(TodayFieldTags.SEARCH),
                    )
                }
            }

            items(
                count = field.size,
                key = { field[it].id },
                span = { index ->
                    if (oneColumn) fullWidth
                    else GridItemSpan(if (field[index].size == "small") 1 else 2)
                },
            ) { index ->
                val card = field[index]
                // Its place in the whole surface, which is what a move has to
                // act on: the field is the layout with the lead taken off.
                val position = index + 1
                CardFor(
                    card = card,
                    answer = answers[card.id] ?: digestAnswer(card, digest, strings, hasAnything),
                    // **Full width is wide.** A card the person made small is
                    // rendering at full width in this layout, and 21.3 ties the
                    // second line of context to width rather than to the label
                    // on the size chip. Leaving it at small would give a full
                    // width card one line and a lot of nothing.
                    size = if (oneColumn && card.size == "small") {
                        CardSize.WIDE
                    } else {
                        CardSize.of(card.size)
                    },
                    onOpen = onOpen,
                    modifier = Modifier.testTag(TodayFieldTags.card(card.id)),
                    today = today,
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

            item(span = { fullWidth }, key = "today-fab-clearance") {
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
    val tab = tabFor(card, answer, strings)

    // **The day for the digest, the card's own name for anything else.** The
    // digest's eyebrow is what day it is, which is a fact worth stating above a
    // sentence about today and is what the grid draws. Naming it "Today" there
    // would be the third time that word appears on the screen, after the chip
    // and the navigation tab. A card the person promoted says which card it is,
    // so they can see what they put at the top.
    // **The day only for the digest**, and null otherwise, because for any
    // other card the eyebrow is the tab itself and the reader's sentence is
    // built from that tab's own raw parts below. Carrying the joined eyebrow
    // into those parts would say the card's name twice and nest its isolates.
    val day = if (card.type == "digest") EventDateText.dayHeading(strings, today) else null
    val eyebrow = day ?: tab

    val shown = worded(card.type, answer, today)

    TodayLead(
        eyebrow = eyebrow,
        hue = hueForCard(card.type),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest.
        // **Raw parts, joined once.** The tab is already a joined string, so
        // passing it here wrapped it a second time and the isolate marks
        // nested: the reader's sentence came out `⁨⁨Next up⁩⁩ · ...`. Seen by
        // walking the semantics tree on the phone, and invisible in English.
        description = Bidi.join(
            listOf(strings[cardTabKey(card.type)], answer?.sourceName, day) + answerParts(
                shown,
                strings,
                cardType = card.type,
                showItems = true,
                drewChart = false,
                countLine = countLineKey(card.type)?.let { strings[it] },
            ),
        ),
        openLabel = strings("today.card.open", "name" to tab),
        onOpen = { onOpen(card) },
        modifier = Modifier.testTag(TodayFieldTags.LEAD),
        speaksAsOneNode = !editing,
    ) {
        AnswerBody(
            answer = shown,
            cardType = card.type,
            lead = true,
            showDetail = true,
            hue = hueForCard(card.type),
        )

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
    today: LocalDate = LocalDate.now(),
) {
    val strings = LocalStrings.current

    val tab = tabFor(card, answer, strings)
    val shown = worded(card.type, answer, today)

    TodayCard(
        tab = tab,
        hue = hueForCard(card.type),
        // **Raw parts, joined once.** Bidi.join isolates every part it is
        // given, so handing it a string that was already joined wraps the whole
        // thing again and the marks nest. The same defect was fixed on the
        // project screen four commits ago and written straight back in here.
        // **Raw parts, joined once**, for the same reason as the lead: `tab` has
        // already been through `Bidi.join`, and joining a joined string nests
        // the isolates.
        description = Bidi.join(
            listOf(strings[cardTabKey(card.type)], answer?.sourceName) + answerParts(
                shown,
                strings,
                cardType = card.type,
                showItems = size != CardSize.SMALL,
                drewChart = size == CardSize.TALL && (shown?.series?.size ?: 0) > 1,
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
            answer = shown,
            cardType = card.type,
            lead = false,
            showDetail = size != CardSize.SMALL,
            tall = size == CardSize.TALL,
            hue = hueForCard(card.type),
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
    /** Whether the card has the height for a chart. Tall only, per 21.3. */
    tall: Boolean = false,
    /** The card's hue, so a chart drawn here is the section's line and not a new color. */
    hue: TabHue = wholeAppHue(),
    /**
     * Where the person's own pictures are read from, for the documents card.
     *
     * Opened against the context the same way `DocumentsScreen` does, rather
     * than threaded down from the shell, because it is a reader over a folder
     * and not state anybody owns.
     */
    attachments: Attachments = Attachments.open(LocalContext.current),
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
        //
        // **Each card says its own nothing.** Every card on the surface saying
        // "Nothing waiting" is the app answering fourteen different questions
        // with one sentence, and on a new notebook that is the whole screen
        // saying the same four words in a column. 21.4 wants the rung to name
        // what is not there, and "Nothing scheduled" also tells the person
        // which door this is without them having to work it out.
        //
        // **No count of what has not been done**, per rule 13: an unfilled slot
        // reads as not yet, never as an error and never as a tally.
        Text(
            text = strings[emptyLineKey(cardType)],
            style = if (lead) type.hero else type.bodyS,
            color = if (lead) colors.ink else colors.ink2,
        )
    }

    // **The recent shape, at tall only.** 21.7 asks the measure card "what is
    // the latest value, and its recent shape", and 21.3 puts a chart at tall
    // and nowhere smaller: a line drawn across half a screen width is a
    // decoration rather than a shape anybody can read.
    //
    // **The same plot the Progress screen draws**, through the same
    // `chartPoints`, so two charts of one measure cannot disagree about the
    // same silence. Every chart rule comes with it: one color from the section
    // hue and never from the value, no target, no zone, no axis, and **gaps
    // drawn as gaps**, because joining the dots across three missing months
    // invents a line nobody recorded.
    val drewChart = tall && answer != null && answer.series.size > 1
    if (drewChart) {
        Spacer(Modifier.height(Space.xs))
        Plot(
            readings = chartPoints(answer.series),
            line = hue.base,
            height = ChartHeight.standard,
        )
    }

    // **The list, at wide and tall only.** 21.3 uses this card as its example:
    // the medications card at small is a count and at wide it is the list, and
    // both are the one question "what is on the list right now" asked once.
    // Growing reveals more of the same answer, never a new kind of content, so
    // this is never a different query and never a feed.
    // **A chart or a list, never both.** 21.3 gives tall a chart, a mini spine,
    // or a short list, and a card carrying a line and then the same readings
    // written out underneath it says one thing twice and stops being a shape.
    if (showDetail && !drewChart && answer != null && answer.items.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = Space.xs)) {
            for (item in answer.items) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // **The person's own paper, at thumbnail size and no
                    // larger.** 21.7 says this card never renders private
                    // content bigger than this, which is the whole reason the
                    // hash travels and the screen decides the size. A document
                    // nobody photographed gets the section's fallback drawing
                    // rather than a hole.
                    if (cardType == "recent_documents") {
                        Thumbnail(
                            sha256 = item.imageSha,
                            attachments = attachments,
                            section = Repository.Section.DOCUMENTS,
                            size = Space.l,
                        )
                        Spacer(Modifier.width(Space.s))
                    }
                    Text(
                        // The parts joined here rather than in the query,
                        // because joining is wording. `Bidi.join` isolates each
                        // part, so a medication somebody typed in Arabic keeps
                        // its own direction beside a dose typed in English, and
                        // a date never renders as the EDTF it is stored as.
                        text = Bidi.join(
                            item.label.ifBlank { strings["project.steps.ungrouped"] },
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
            val hidden = if (answer.itemsSampleTheCount) {
                (answer.count ?: 0) - answer.items.size
            } else {
                0
            }
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
    // A counted second line, worded here because its plural belongs to the
    // reader's language. The key is a literal on the repository side.
    val countedLine = answer?.detailKey?.let { key ->
        answer.detailCount?.let { strings(key, "count" to it) }
    }
    val lines = listOfNotNull(
        whenText,
        answer?.detail?.takeIf { it.isNotBlank() },
        countedLine,
    )
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
 * The card's index tab, naming which one it is.
 *
 * **A card pointing at one thing says which thing.** 21.2 puts identity on the
 * tab, and the grid heads a measure card "Progress · Weight" and a project's
 * date card "Waiver · the next date". Four project cards on one Today all
 * reading "Project" is four cards nobody can tell apart, and the name belongs
 * here rather than in the answer: the answer is the reading or the countdown,
 * and this is what it is a reading of.
 */
private fun tabFor(
    card: Repository.TodayCard,
    answer: Repository.TodayAnswer?,
    strings: Strings,
): String = Bidi.join(strings[cardTabKey(card.type)], answer?.sourceName)

/**
 * The catalog key for a card type's tab. **Literal, per [countLineKey].**
 *
 * `check_string_keys.py` cannot see a key built from a variable, and this file
 * asked for `"today.card.${card.type}"` for as long as the surface has existed.
 * `TodayCardKeyTest` now holds the schema's seventeen types to the catalog, so
 * the dynamic form is covered; this stays a `when` anyway, because the check
 * that reads the code should be able to see the code's keys.
 */
private fun cardTabKey(cardType: String): String = when (cardType) {
    "digest" -> "today.card.digest"
    "next_up" -> "today.card.next_up"
    "medications" -> "today.card.medications"
    "measure" -> "today.card.measure"
    "milestones" -> "today.card.milestones"
    "ask_next_time" -> "today.card.ask_next_time"
    "project_standing" -> "today.card.project_standing"
    "project_date" -> "today.card.project_date"
    "project_steps" -> "today.card.project_steps"
    "incidents" -> "today.card.incidents"
    "money" -> "today.card.money"
    "unfiled" -> "today.card.unfiled"
    "emergency_card" -> "today.card.emergency_card"
    "care_team" -> "today.card.care_team"
    "trail_lately" -> "today.card.trail_lately"
    "recent_documents" -> "today.card.recent_documents"
    "standing_instructions" -> "today.card.standing_instructions"
    // A type the schema refuses cannot reach here, and `TodayCardKeyTest`
    // fails the build if the schema ever gains one this does not know.
    else -> "today.card.digest"
}

/**
 * The stored values in an answer, turned into words. `DESIGN.md` section 9.2.
 *
 * **A stored value is not display text and this is where that stops being
 * true.** A project's status arrives as `waiting`, which is a database value
 * and not a word anybody wrote, and a project date arrives as the date rather
 * than as the countdown 21.7 asks for. Rendering either one raw is the same
 * defect as putting EDTF on the screen, which this surface nearly did.
 *
 * **The countdown is computed here rather than in the query**, because how many
 * days away something is is a fact about now, and a query result is a fact
 * about when the query ran.
 */
@Composable
private fun worded(
    cardType: String,
    answer: Repository.TodayAnswer?,
    today: LocalDate,
): Repository.TodayAnswer? {
    val strings = LocalStrings.current
    if (answer == null || answer.sourceClosed) return answer
    return when (cardType) {
        "project_standing" -> answer.copy(
            // **Whose hands is the answer and the status is the context.**
            // 21.7 asks "whose hands, since when", and a person waiting on the
            // county wants the county, not the word Waiting.
            title = answer.title ?: strings[projectStatusKey(answer.detail)],
            detail = answer.title?.let { strings[projectStatusKey(answer.detail)] },
        )

        "project_date" -> {
            val due = answer.whenEdtf
                ?.let { com.kamsiob.healthtrail.time.Edtf.parse(it) }
                ?.let {
                    com.kamsiob.healthtrail.time.Edtf.resolve(it, java.time.ZoneId.systemDefault())
                }
                ?.start
                ?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }
            // **A date the person gave coarsely gets no countdown.** Rule 17:
            // "sometime in April" is not a number of days away, and turning it
            // into one would invent the precision the whole date model exists
            // to protect. The card shows the date it was given instead.
            val exact = answer.whenEdtf?.let {
                com.kamsiob.healthtrail.time.Edtf.parse(it)?.precision
            } == com.kamsiob.healthtrail.time.Edtf.Precision.DAY
            val days = if (due != null && exact) {
                java.time.temporal.ChronoUnit.DAYS.between(today, due)
            } else {
                null
            }
            answer.copy(
                title = when {
                    days == null -> answer.title
                    days == 0L -> strings["project.countdown.today"]
                    days > 0 -> strings("project.countdown.days", "count" to days)
                    // **Passed is plain words and no urgency color**, 21.4.
                    else -> strings("project.countdown.passed", "count" to -days)
                },
                // The kind, which is what the date is, in the person's own
                // words. It is free text they typed, so it renders as it is.
                detail = if (days == null) answer.detail else answer.title,
            )
        }

        // **What happens next, said the way somebody in a kitchen says it.**
        // The grid draws "Tomorrow 10:15" and the card was rendering "April 9,
        // 2026 at 10:15 AM", which is correct and makes the person count.
        // Anything further out than tomorrow keeps its full date, and a coarse
        // date is never given a day word at all: rule 17 again.
        // **Only what shares the day with the answer.** 21.7 wants two on one
        // day to become two lines, and nothing more: a card listing next week
        // as well has stopped answering "what is the next dated thing" and
        // started being an agenda, which is a screen the app already has.
        "next_up" -> answer.copy(
            items = answer.items.filter { sameDay(it.noteEdtf, answer.whenEdtf) },
            detail = EventDateText.nearby(strings, answer.whenEdtf, today)
                ?: answer.detail,
            whenEdtf = if (EventDateText.nearby(strings, answer.whenEdtf, today) != null) {
                null
            } else {
                answer.whenEdtf
            },
        )

        else -> answer
    }
}

/**
 * What a card says when the record has nothing for it yet. `DESIGN.md` 21.4.
 *
 * **Literal keys, per [countLineKey] and [cardTabKey]**, so the checker that
 * reads the code's keys can see them.
 *
 * **Falls back to the general sentence rather than to a key that may not
 * exist**, because `Strings.resolve` throws in debug and the empty rung is the
 * state a brand new notebook is entirely made of. A crash there would be a
 * crash on the first screen anybody ever sees.
 */
private fun emptyLineKey(cardType: String): String = when (cardType) {
    "next_up" -> "today.card.next_up.none"
    "medications" -> "today.card.medications.none"
    "ask_next_time" -> "today.card.ask_next_time.none"
    "money" -> "today.card.money.none"
    "incidents" -> "today.card.incidents.none"
    "unfiled" -> "today.card.unfiled.none"
    "care_team" -> "today.card.care_team.none"
    "recent_documents" -> "today.card.recent_documents.none"
    "standing_instructions" -> "today.card.standing_instructions.none"
    "emergency_card" -> "today.card.emergency_card.none"
    "trail_lately" -> "today.card.trail_lately.none"
    "measure" -> "today.card.measure.none"
    "milestones" -> "today.card.milestones.none"
    "project_date" -> "today.card.project_date.none"
    // The digest always has a sentence, and the two other project cards say
    // their own thing when there is nothing: a project with no steps and no
    // waiting-on is a project somebody just started.
    else -> "today.card.nothing"
}

/**
 * Whether two stored dates fall on the same calendar day.
 *
 * **Both have to be day precise for the question to mean anything.** "Sometime
 * in April" is not on any particular day, so it shares one with nothing, and
 * saying otherwise would be the invented precision rule 17 exists to prevent.
 */
private fun sameDay(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    fun day(text: String): String? {
        val date = com.kamsiob.healthtrail.time.Edtf.parse(text) ?: return null
        return when (date.precision) {
            com.kamsiob.healthtrail.time.Edtf.Precision.DAY -> date.canonical
            com.kamsiob.healthtrail.time.Edtf.Precision.MOMENT -> date.canonical.substringBefore('T')
            else -> null
        }
    }
    val left = day(a) ?: return false
    return left == day(b)
}

/** A project status value as the word the rest of the app uses for it. */
private fun projectStatusKey(status: String?): String = when (status) {
    "waiting" -> "projects.status.waiting"
    "stalled" -> "projects.status.stalled"
    "done" -> "projects.status.done"
    "abandoned" -> "projects.status.abandoned"
    else -> "projects.status.active"
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
    "project_steps" -> "today.card.project_steps.count"
    "measure" -> "today.card.measure.count"
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
    /** The card's type, so the empty rung reads aloud as it reads on screen. */
    cardType: String,
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
    /**
     * Whether the card drew its chart, so the ear gets what the eye gets.
     *
     * 21.3 gives tall a chart or a list and the screen picks the chart when
     * there is a series. Section 9: what is read aloud says the same thing the
     * screen says.
     */
    drewChart: Boolean = false,
): List<String?> = when {
    answer == null -> listOf(strings["today.card.unread"])
    answer.sourceClosed -> listOf(answer.title, strings["today.card.source_closed"])
    answer.isEmpty -> listOf(strings[emptyLineKey(cardType)])
    else -> buildList {
        // **The number only where the screen shows one**, which is where the
        // card has no title of its own. It was announced either way, so the
        // trail card told a reader there were 182 entries while the eye saw a
        // sentence and no number at all. Section 9.
        if (answer.title == null) {
            add(answer.count?.takeIf { it > 0 }?.toString())
            if ((answer.count ?: 0) > 0) add(countLine)
        }
        add(answer.title)
        // **The date the screen shows**, per section 9. It was absent here for
        // as long as it was absent there, so the omission was consistent and
        // wrong twice.
        add(answer.whenEdtf?.takeIf { it.isNotBlank() }?.let { EventDateText.render(strings, it) })
        // **The ear gets what the eye gets: a chart or a list, never both.**
        // 21.3 gives tall one or the other and the screen picks the chart when
        // there is a series, so announcing the list too made a reader listen to
        // three readings that were not on the screen and then be told there
        // were twelve. Section 9.
        if (showItems && !drewChart) {
            // **Raw parts, never a joined line.** `Bidi.join` isolates every
            // part it is handed, so adding an already joined item wrapped it a
            // second time and the sentence came out `⁨⁨137.3⁩ · ⁨May 5⁩⁩`. The
            // caller joins once, at the end, over flat parts.
            for (item in answer.items) {
                add(item.label.ifBlank { strings["project.steps.ungrouped"] })
                add(item.note)
                add(item.noteEdtf?.let { EventDateText.render(strings, it) })
                add(
                    item.amountMinor?.let {
                        formatMoney(strings, it, item.currency ?: "USD")
                    },
                )
            }
            val hidden = if (answer.itemsSampleTheCount) {
                (answer.count ?: 0) - answer.items.size
            } else {
                0
            }
            if (answer.items.isNotEmpty() && hidden > 0) {
                add(strings("today.card.more", "count" to hidden))
            }
        }
        // **A chart is one sentence to a reader, never a list of points.**
        // `ChartCard` says the same thing about itself: a chart announced as
        // coordinates is useless. How many readings there are is the part
        // somebody listening can act on.
        if (drewChart) {
            add(strings("progress.readings", "count" to answer.series.size))
        }
        add(answer.detail)
        add(
            answer.detailKey?.let { key ->
                answer.detailCount?.let { strings(key, "count" to it) }
            },
        )
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
    /** Whether anything has ever been written down. */
    hasAnything: Boolean,
): Repository.TodayAnswer? {
    if (card.type != "digest") return null
    // **A notebook made thirty seconds ago has no last time.** The lead read
    // "Nothing new since you were last here" on the first screen anybody sees
    // after onboarding, which refers to a visit that did not happen. The
    // previous Today had this right and showed no digest at all on a first
    // run; this surface cannot, because 21.1 says the lead is never empty.
    //
    // **So it says what is true on day one and nothing more.** Not a task, not
    // a setup prompt, not a count of what is missing: rule 13 rules all three
    // out, and this is the one screen where a new person decides whether the
    // app is going to nag them. It states what the card is for and stops.
    if (!hasAnything) {
        return Repository.TodayAnswer(
            title = strings["today.card.digest.first"],
            detail = strings["today.card.digest.first.detail"],
        )
    }
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

/**
 * The text scale at which Today's field becomes one column.
 *
 * `DESIGN.md` 21.6 screen 8. Not the top of the slider: somebody at 1.7 has the
 * same half width card as somebody at 2.0, and making them go further to get a
 * readable screen is the opposite of designing for it.
 */
private const val WIDE_TYPE_SCALE = 1.5f
