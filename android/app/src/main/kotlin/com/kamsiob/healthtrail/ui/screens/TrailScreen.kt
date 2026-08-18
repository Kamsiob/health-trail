package com.kamsiob.healthtrail.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Distance
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.v4.DistanceMarker
import com.kamsiob.healthtrail.ui.v4.EdgeRail
import com.kamsiob.healthtrail.ui.components.PinMark
import com.kamsiob.healthtrail.ui.components.PinnedGroupText
import com.kamsiob.healthtrail.ui.v4.RouteDash
import com.kamsiob.healthtrail.ui.v4.RouteSwatch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.kamsiob.healthtrail.ui.v4.ScopedSearch
import com.kamsiob.healthtrail.ui.v4.SpineRow
import com.kamsiob.healthtrail.ui.v4.StickySectionHeader
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.LocalMotion
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import java.time.Instant
import java.time.ZoneId
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card

object TrailTags {
    const val NAME = "trail"
    const val SEE_ALL = "trail_see_all"
    const val SEARCH = "trail_search"
    const val SCRUBBER = "trail_scrubber"
    const val FILTER = "trail_filter"
    const val FILTER_STATE = "trail_filter_state"
    fun kind(kind: String) = "trail_filter_$kind"
    fun entry(id: String) = "trail_entry_$id"
    fun pin(id: String) = "trail_pin_$id"
    fun month(label: String) = "trail_month_$label"
}

/**
 * The trail: everything the person has written down, most recent first.
 *
 * **This is the screen v4's law 4 was written for.** A notebook kept for five
 * years holds sixteen hundred entries, and every list in this app was built as
 * though it held twelve. The four tools all land here together because they only
 * work together: a sticky header without a scrubber tells you where you are in a
 * scroll you cannot escape, and a scrubber without folding drops you into the
 * middle of a wall.
 *
 * **The one thing, per law 1: what is at the top of the trail right now.** The
 * newest month is open and everything older is folded and counted. A person
 * opening this screen in a corridor sees this month; a person with ten minutes
 * opens as far back as they like. Nothing is hidden, and every fold says what it
 * holds and how much.
 *
 * **Pinned entries float above time itself.** In a five year record the thing
 * somebody needs at a desk is almost never the most recent thing. It is the
 * letter they have to quote, or the incident nobody answered. Pinning is the
 * person saying "I keep needing this one" and it changes nothing else: no state,
 * no category, no meaning the app added.
 *
 * **The pin is not on the row.** It was, for one build, and ten pin buttons down
 * one screen were the loudest thing on it. Pinning is a decision somebody makes
 * a handful of times over years, so it lives on the entry screen, one tap away,
 * and the row carries only the mark that says an entry is pinned.
 *
 * **The search is scoped to the trail and filters as you type.** It reads what
 * the person wrote, which is titles and notes, and it never reaches into other
 * sections. Dates are the scrubber's job and saying so is better than quietly
 * matching "June" against a rendered string somebody cannot see.
 *
 * **The route is still the app's signature**, per `DESIGN.md` 5.2 and the
 * inventory's "a spine for anything sequential". A dashed gold line runs down
 * the start edge, each entry sits on it as a node carrying its kind, and mono
 * markers say when a real gap passed. It mirrors for free, because the gutter is
 * the start edge rather than the left one.
 *
 * **The rows lost their cards in the v4 pass.** An entry in a list is a date and
 * a line about what happened, which rule 22 calls a dense row; the note it
 * carries is three or more lines somebody reads, which is the entry screen. The
 * clamped body preview went with the card, and with it the odd middle state
 * where a list row showed most of a note and asked you to open it for the rest.
 *
 * **The date is no longer a button inside a row that is also a button.** Law 2
 * wants one costume per thing, and a row you tap to open with a second target
 * inside it is two. Rule 17 is kept where it belongs: the entry screen edits its
 * own date, one tap away, and that screen already did.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrailScreen(
    entries: List<Repository.TrailEntry>,
    onOpen: (Repository.TrailEntry) -> Unit,
    /**
     * Opens one month, gathered, given any instant inside it.
     *
     * An instant rather than a label, because the label is already localized
     * and parsing it back into a month would be reading the app's own rendering
     * as data.
     */
    onReview: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Opens the capture sheet, for a trail that holds nothing yet.
     *
     * Null where there is no sheet to open, which is every caller that is not
     * the shell. The empty state drops its action rather than drawing one that
     * goes nowhere.
     */
    onAdd: (() -> Unit)? = null,
    zone: ZoneId = ZoneId.systemDefault(),
    /**
     * Show only what arrived since this instant, or null for the whole trail.
     *
     * **The digest promised a filtered view and opened an unfiltered one.**
     * D169, the owner: "when I tap the big banner at the top gives you an
     * update on the today page of new things that have happened and I tap it,
     * it's not reflective of it's actually there." The card said three new
     * things since you were last here and the trail then showed everything
     * ever written, so the count and the screen disagreed and the person was
     * left looking for three items among two hundred.
     *
     * **The heading changes with it**, so a filtered trail says it is
     * filtered rather than looking like a notebook that lost its history.
     */
    since: Long? = null,
    /** Drops the filter, for the person who wants the rest after all. */
    onSeeAll: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val motion = LocalMotion.current

    // **Filtered at the top, so everything below reads one list.** The search,
    // the kind chips, the month folds and the scrubber all work off `entries`,
    // and narrowing it here means none of them has to know about the digest.
    val entries = remember(entries, since) {
        if (since == null) entries else entries.filter { it.createdAt >= since }
    }

    var query by rememberSaveable { mutableStateOf("") }
    // Which month the scrubber asked for, held until the plan that contains it
    // has been built. A scrub can open several folds, and the row it wants does
    // not exist in the list until the next composition.
    var scrollWanted by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    /**
     * Which kinds the person is looking at, or empty for all of them. #220.
     *
     * **It resets when they leave this screen**, and that is the decision
     * rather than an oversight. The view toggle is remembered per section, per
     * `DESIGN.md` section 7, and this is not the same case: a remembered view
     * changes how the trail is drawn and a remembered filter changes **what is
     * in it**. Somebody opening the trail in a hallway to check whether a call
     * happened, and seeing a list with the calls filtered out from a week ago,
     * is looking at a record that is lying to them. D134.
     */
    var kinds by remember(entries) { mutableStateOf(emptySet<String>()) }

    /**
     * One chip per name, over the kinds this notebook actually has.
     *
     * **What is here rather than what the schema allows**, because a chip for a
     * kind nobody has ever written narrows to nothing, which is a control that
     * looks broken.
     *
     * **Grouped by the name rather than by the stored kind.** `kindNameKey`
     * already folds `transfer` and `milestone` into "A note", which is how the
     * trail names them everywhere else, so one chip per stored kind would put
     * two chips reading "A note" side by side filtering different things. The
     * chip filters everything the app calls by that word.
     */
    val kindGroups: List<Pair<String, Set<String>>> = remember(entries) {
        entries.map { it.kind }
            .distinct()
            .sortedBy { KIND_ORDER.indexOf(it).takeIf { at -> at >= 0 } ?: KIND_ORDER.size }
            .groupBy { kindNameKey(it) }
            .map { (key, kinds) -> key to kinds.toSet() }
    }

    val plan = remember(entries, strings, zone, query, kinds) {
        planTrail(
            entries = entries.filter { kinds.isEmpty() || it.kind in kinds },
            strings = strings,
            zone = zone,
            query = query,
        )
    }

    // **How many items sit above the plan**, which is what turns a plan index
    // into a list index. The scaffold's own header is one; the search field is
    // another when the notebook is large enough to have earned it.
    //
    // It was hard coded to one for a build, and the scrubber landed a row early
    // every time: the month header it aimed at arrived with the fold row above
    // it still on screen, so a jump to 2022 looked like a jump to the bottom of
    // 2023. Found by scrubbing on the phone, not by reading this.
    val headerItems = 1 + if (entries.size >= MIN_SEARCH_ENTRIES) 1 else 0

    // **Where the list actually is**, read from the topmost visible row rather
    // than assumed. Derived rather than remembered, so it recomputes while a
    // finger is moving and not one frame after it stops.
    val currentYear by remember(plan) {
        derivedStateOf {
            val row = listState.firstVisibleItemIndex - headerItems
            val label = plan.rowYears.getOrNull(row.coerceAtLeast(0))
            plan.years.indexOfFirst { it.label == label }.coerceAtLeast(0)
        }
    }

    LaunchedEffect(plan, scrollWanted) {
        val wanted = scrollWanted ?: return@LaunchedEffect
        val index = plan.rows.indexOfFirst { it is TrailRowSpec.Month && it.label == wanted }
        if (index >= 0) {
            listState.scrollToItem(index + headerItems)
            scrollWanted = null
        }
    }

    // The one ambient flourish, per section 10, and it runs once per screen
    // entry rather than on every recomposition.
    var drawn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { drawn = true }
    val draw by animateFloatAsState(
        targetValue = if (drawn) 1f else 0f,
        animationSpec = motion.trailDraw(),
        label = "trailDraw",
    )

    Page(
        // **A filtered trail says so in its own heading.** Otherwise it reads
        // as the whole trail with most of it missing.
        title = if (since != null) strings["trail.since.title"] else strings["trail.heading"],
        onBack = onBack,
        backLabel = strings[LocalSectionBackKey.current],
        modifier = modifier.testTag(SectionTags.root(TrailTags.NAME)),
        eyebrow = strings["notebook.section.trail"],
        subtitle = strings["trail.subtitle"],
        section = Repository.Section.TRAIL,
        listState = listState,
        rail = if (plan.years.size >= MIN_SCRUB_YEARS && query.isBlank()) {
            {
                EdgeRail(
                    labels = plan.years.map { it.label },
                    currentIndex = currentYear,
                    onScrub = { index ->
                        val year = plan.years.getOrNull(index) ?: return@EdgeRail
                        // **Scrubbing scrolls, and there is nothing to open.**
                        // Every month is on the list now, so the year somebody
                        // asked for is a position rather than a door. D185.
                        scrollWanted = year.firstMonth
                    },
                    description = strings["trail.scrubber"],
                    modifier = Modifier.testTag(TrailTags.SCRUBBER),
                )
            }
        } else {
            null
        },
    ) {
        if (since != null) {
            item {
                Action(
                    label = strings["trail.since.all"],
                    onClick = { onSeeAll?.invoke() },
                    modifier = Modifier.testTag(TrailTags.SEE_ALL),
                )
            }
        }

        if (entries.isEmpty()) {
            item {
                SectionEmpty(
                    name = TrailTags.NAME,
                    text = strings["trail.empty"],
                    section = Repository.Section.TRAIL,
                    // **The one screen in the app with nothing on it and no
                    // way to put anything on it**, which is what #388 finding 1
                    // names alongside the blank canvas. The trail is opened from
                    // the notebook, so it is pushed over the shell and has no
                    // capture button in the corner the way a destination does,
                    // and D200's floating add belongs to a section that owns a
                    // kind of row. The trail owns every kind, so its way in is
                    // the capture sheet itself.
                    //
                    // **The label is the capture button's own sentence** rather
                    // than a new one, because it opens the capture button's own
                    // sheet and the four catalogs already carry it.
                    actionLabel = onAdd?.let { strings["capture.button.description"] },
                    onAction = onAdd,
                )
            }
            return@Page
        }

        // The search sits above everything including the pins, because it is the
        // way into a list this size and burying it under a group nobody has
        // pinned to yet would be the first thing to go wrong at year three.
        if (entries.size >= MIN_SEARCH_ENTRIES) {
            item(key = "search") {
                ScopedSearch(
                    value = query,
                    onValueChange = { query = it },
                    // **The count the search will actually look through**, which
                    // is the filtered set rather than the whole trail. Both
                    // controls narrow this one list and they compose: the
                    // search looks inside the filter. Saying 182 while showing
                    // 21 would be the screen describing a list that is not on
                    // it. #220.
                    hint = strings(
                        "trail.search.hint",
                        "count" to entries.count { kinds.isEmpty() || it.kind in kinds },
                    ),
                    clearLabel = strings["trail.search.clear"],
                    testTag = TrailTags.SEARCH,
                )
            }
        }

        // **The filter, beside the search, because both narrow this one list.**
        // Screen 08 draws it in the header; it sits under the search here for
        // the same reason the search sits above the pins, which is that a
        // control for a list belongs with the list it narrows.
        //
        // **Chips rather than a sheet**, per law 2 and the issue: the choices
        // are few, they are all visible, and nothing is hidden behind a tap.
        //
        // **Only shown once there is more than one kind to choose between.** A
        // row of one chip is a control with no decision in it.
        if (kindGroups.size > 1) {
            item(key = "filter") {
                // **One row that scrolls, not two that wrap.** #388 finding 6:
                // four chips of four different widths wrapped two and two, and
                // both lines ended in a ragged edge a third of the way across
                // the screen. `docs/V4.md` 6.1 item 6 asks for optical
                // alignment and the wrap had none, and the pair of lines took
                // 150dp of the first screen before a single entry.
                //
                // **The whole set is still reachable and nothing is behind a
                // tap**, which is what law 2 asks of a filter: the row scrolls
                // sideways the way Material's own chip rows do, and the first
                // chips of the set are the ones on screen.
                //
                // **`LazyRow` rather than a `Row` in a scroll modifier**, so a
                // notebook that grows a seventh kind of entry does not compose
                // seven chips to draw two.
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier.fillMaxWidth().testTag(TrailTags.FILTER),
                ) {
                    items(kindGroups.toList(), key = { it.first }) { (key, group) ->
                        val on = group.all { it in kinds }
                        ChoiceChip(
                            label = strings[key],
                            selected = on,
                            onClick = { kinds = if (on) kinds - group else kinds + group },
                            modifier = Modifier.testTag(TrailTags.kind(group.first())),
                        )
                    }
                }
            }
        }

        // **A filtered trail says so, in one line, with the way out in it.**
        // Somebody who forgot they set a filter is looking at a record with
        // things missing from it, and the trail's whole promise is that it is
        // everything. This is the sentence that keeps that promise honest.
        //
        // **It counts what is hidden rather than what is shown**, because the
        // number that matters to somebody who has lost track is how much of
        // their record is not on the screen.
        if (kinds.isNotEmpty()) {
            item(key = "filtered") {
                val hidden = entries.size - entries.count { it.kind in kinds }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Space.s),
                ) {
                    Text(
                        text = strings("trail.filter.showing", "count" to hidden),
                        style = HealthTrail.type.bodyS,
                        color = HealthTrail.colors.ink2,
                        modifier = Modifier.weight(1f).testTag(TrailTags.FILTER_STATE),
                    )
                    Action(
                        label = strings["trail.filter.clear"],
                        onClick = { kinds = emptySet() },
                    )
                }
            }
        }

        if (query.isNotBlank()) {
            item(key = "found") {
                Text(
                    text = strings("trail.search.count", "count" to plan.rows.count {
                        it is TrailRowSpec.Entry
                    }),
                    style = HealthTrail.type.bodyS,
                    color = HealthTrail.colors.ink2,
                    modifier = Modifier.padding(bottom = Space.s),
                )
            }
        }

        for ((index, row) in plan.rows.withIndex()) {
            when (row) {
                is TrailRowSpec.Pinned -> item(key = "pinned") {
                    PinnedGroupText(label = strings["trail.pinned"]) {
                        Column {
                            for (entry in row.entries) {
                                TrailRow(
                                    entry = entry,
                                    onOpen = { onOpen(entry) },
                                    // Pinned entries float above time itself,
                                    // so each one carries its own whole date.
                                    withinMonth = false,
                                )
                            }
                        }
                    }
                }

                is TrailRowSpec.Month -> stickyHeader(key = "month_${row.label}") {
                    // **The month header is the door to that month's review**,
                    // per rule 18 and 13.5: a review nobody can find is a
                    // screen nobody has. The header is where the eye already is
                    // when somebody wonders what a month held, and it costs no
                    // furniture, where a row under fourteen entries would be
                    // discoverable only by scrolling past the thing it
                    // summarizes.
                    //
                    // **A closed month is a fold rather than a door**, and that
                    // is deliberate: a fold opens in place, which is what a
                    // sand row promises. Reviewing a month somebody has not
                    // opened is one tap further, which is the right price for
                    // keeping the two costumes honest.
                    //
                    // **The undated group is a heading and never a door.** The
                    // entries nobody could date sit under their own heading at
                    // the end, and there is no month to review: opening one
                    // would mean the app picking a month for them, which is the
                    // precision rule 17 forbids inventing.
                    StickySectionHeader(
                        // bidi-ok: the app formats this itself, so it is never somebody's own words.
                        title = row.label,
                        count = strings("trail.month.count", "count" to row.count),
                        openLabel = row.firstMillis?.let { strings["review.open"] },
                        onOpen = row.firstMillis?.let { millis -> { onReview(millis) } },
                        modifier = Modifier.testTag(TrailTags.month(row.label)),
                    )
                }

                is TrailRowSpec.Entry -> item(key = row.entry.id) {
                    RouteRow(
                        draw = draw,
                        continuesAbove = row.continuesAbove,
                        continuesBelow = row.continuesBelow,
                        node = nodeColor(row.entry.kind),
                        nodeDelayMillis = index * motion.trailNodeStaggerMillis,
                    ) {
                        Column {
                            row.gap?.let { gap ->
                                DistanceMarker(strings(gap.key, "count" to gap.count))
                            }
                            TrailRow(
                                entry = row.entry,
                                onOpen = { onOpen(row.entry) },
                                withinMonth = row.withinMonth,
                            )
                        }
                    }
                }
            }
        }

        if (query.isNotBlank() && plan.rows.none { it is TrailRowSpec.Entry }) {
            item(key = "nothing") {
                Text(
                    text = strings["trail.search.none"],
                    style = HealthTrail.type.bodyM,
                    color = HealthTrail.colors.ink2,
                    modifier = Modifier.padding(vertical = Space.l),
                )
            }
        }
    }
}

/**
 * How few entries make the two big-list tools furniture rather than help.
 *
 * Both the scrubber and the search field are explicit in `DESIGN.md` section 7
 * about not appearing on a list that fits in a screenful or two. A search field
 * over eight entries costs more to read than the list under it.
 */
private const val MIN_SEARCH_ENTRIES = 12
private const val MIN_SCRUB_YEARS = 2

/**
 * How many months stay visible as their own folds before the rest become
 * "Earlier".
 *
 * Three, which is the reference file's shape: the current month open, a couple
 * of named months behind it, and one door to the whole record. Naming twelve
 * months is a wall of doors, and a person who wants March of last year reaches
 * it with the scrubber rather than by reading a list of months.
 */

/** What the trail draws, in order, once the folding has been worked out. */
private sealed interface TrailRowSpec {
    /** The pinned run, above time itself. */
    data class Pinned(val entries: List<Repository.TrailEntry>) : TrailRowSpec

    /**
     * An open month's sticky header, carrying its own count.
     *
     * [firstMillis] is any instant inside the month, which is what the review
     * needs to know which month it is, and null for the group holding entries
     * nobody could date. Null is what makes that group a heading rather than a
     * door.
     */
    data class Month(val label: String, val count: Int, val firstMillis: Long?) : TrailRowSpec

    /** One entry on the route. */
    data class Entry(
        val entry: Repository.TrailEntry,
        val gap: Distance.Gap?,
        val continuesAbove: Boolean,
        val continuesBelow: Boolean,
        /**
         * True when a month band directly above this row has already said the
         * month and the year, so the row says the weekday and the day instead.
         *
         * **False for search results and for the pinned run**, which is the
         * whole reason this is a flag rather than a rule: both of those float
         * free of the month structure, and "Monday 29" in a list that spans
         * five years is a date somebody cannot place.
         */
        val withinMonth: Boolean = true,
    ) : TrailRowSpec
}

/** A year the scrubber can jump to, and what opening it has to open. */
private data class TrailYear(
    val label: String,
    val firstMonth: String,
    val months: Set<String>,
)

private data class TrailPlan(
    val rows: List<TrailRowSpec>,
    val years: List<TrailYear>,
    /**
     * Which year each row belongs to, position for position with [rows].
     *
     * **It is what lets the scrubber say where you actually are.** The first
     * build lit the newest year permanently, which is a control reporting a
     * position it never checked: it looked right on a screen that had not been
     * scrolled and was wrong from the first swipe.
     */
    val rowYears: List<String?>,
)

/**
 * Works out the whole screen before any of it is drawn.
 *
 * **It is a pure function over the entries and what the person has opened**,
 * which is what lets the folding be checked without a phone and, more usefully,
 * what lets the scrubber know which list index a year lands on. A layout that
 * decides its own shape while it is being emitted cannot answer that question.
 */
/**
 * The order the kinds appear in the filter, which is the notebook's own.
 *
 * **Not alphabetical and not by how many there are.** Alphabetical is a
 * different order in every language, and ordering by count moves the chips
 * around as somebody writes, which is the same argument the digest makes for
 * never ranking sections by volume. A kind this list has never heard of sorts
 * to the end rather than being dropped.
 */
private val KIND_ORDER = listOf(
    "call", "visit", "incident", "measurement",
    "question", "document", "note", "transfer", "milestone",
)

private fun planTrail(
    entries: List<Repository.TrailEntry>,
    strings: Strings,
    zone: ZoneId,
    query: String,
): TrailPlan {
    val rows = mutableListOf<TrailRowSpec>()
    val rowYears = mutableListOf<String?>()
    fun add(row: TrailRowSpec, year: String?) {
        rows += row
        rowYears += year
    }

    val term = query.trim().lowercase()
    if (term.isNotEmpty()) {
        // **Flat, and no folds.** Somebody who has typed a word is looking at a
        // handful of results, and folding those by month would hide the answer
        // behind the structure the search exists to escape.
        val found = entries.filter { entry ->
            entry.title?.lowercase()?.contains(term) == true ||
                entry.body?.lowercase()?.contains(term) == true
        }
        found.forEachIndexed { index, entry ->
            add(
                TrailRowSpec.Entry(
                    entry = entry,
                    gap = null,
                    continuesAbove = index > 0,
                    continuesBelow = index < found.size - 1,
                    // A flat list with no month bands, so every row says its
                    // whole date.
                    withinMonth = false,
                ),
                null,
            )
        }
        return TrailPlan(rows, emptyList(), rowYears)
    }

    val pinned = entries.filter { it.pinnedAt != null }.sortedByDescending { it.pinnedAt }
    if (pinned.isNotEmpty()) add(TrailRowSpec.Pinned(pinned), null)

    // Grouped by the month a thing happened in, keeping the order the query
    // already put them in. Entries with no date fall into their own group at the
    // end, which is where the ordering already placed them.
    val byMonth = LinkedHashMap<String, MutableList<Repository.TrailEntry>>()
    for (entry in entries) {
        val label = entry.occurredStart
            ?.let { EventDateText.monthHeading(strings, it, zone) }
            ?: strings["date.unknown"]
        byMonth.getOrPut(label) { mutableListOf() } += entry
    }

    val months = byMonth.keys.toList()
    val positions = entries.withIndex().associate { (index, entry) -> entry.id to index }
    val years = yearsOf(byMonth, zone)
    val yearOfMonth = buildMap {
        for (year in years) for (label in year.months) put(label, year.label)
    }

    // **Every month is open, and the band is the label.** D185: nothing sits
    // behind a fold that a label and a scroll can carry, and this list is the
    // case that proves it. It used to name three months and put five years
    // behind a door called "Earlier", so the trail's own promise, that a year
    // of somebody's care is one scroll, was a door. The month bands stick, the
    // scrubber jumps by year, and a lazy list costs what is on the screen.
    months.forEach { label ->
        val inMonth = byMonth.getValue(label)
        add(
            TrailRowSpec.Month(
                label = label,
                count = inMonth.size,
                // Any dated entry in the group answers which month it is. The
                // undated group has none, which is what leaves it a heading.
                firstMillis = inMonth.firstNotNullOfOrNull { it.occurredStart },
            ),
            yearOfMonth[label],
        )
        for (entry in inMonth) {
            val position = positions.getValue(entry.id)
            // **The row above this one in the whole trail, not in this month.**
            // Scoping it to the month was wrong in the one case that matters: a
            // gap worth saying usually crosses a month boundary, and those
            // entries are the first in their group, so the marker never
            // appeared.
            val previous = entries.getOrNull(position - 1)
            add(
                TrailRowSpec.Entry(
                    entry = entry,
                    gap = gapAbove(entry, previous, zone),
                    continuesAbove = true,
                    continuesBelow = position < entries.size - 1,
                ),
                yearOfMonth[label],
            )
        }
    }

    return TrailPlan(rows, years, rowYears)
}

/**
 * The years the scrubber offers, newest first, each knowing which months it has
 * to open to show itself.
 *
 * **Two digits, because the strip is a margin.** "26" beside "25" is read as a
 * year by anybody who has used a filing cabinet, and four digits at that size is
 * either unreadable or a column the content has to give up.
 *
 * Entries whose date is unknown belong to no year and are not offered. They sit
 * under their own heading at the end of the trail, which the scrubber's last
 * label already lands beside, and inventing a year for them would be the app
 * claiming something the person never said.
 */
private fun yearsOf(
    byMonth: Map<String, List<Repository.TrailEntry>>,
    zone: ZoneId,
): List<TrailYear> {
    val grouped = LinkedHashMap<Int, MutableSet<String>>()
    val firstMonth = LinkedHashMap<Int, String>()
    for ((label, inMonth) in byMonth) {
        val millis = inMonth.firstNotNullOfOrNull { it.occurredStart } ?: continue
        val year = Instant.ofEpochMilli(millis).atZone(zone).year
        grouped.getOrPut(year) { linkedSetOf() } += label
        firstMonth.putIfAbsent(year, label)
    }
    return grouped.entries.map { (year, labels) ->
        TrailYear(
            label = (year % 100).toString().padStart(2, '0'),
            firstMonth = firstMonth.getValue(year),
            months = labels,
        )
    }
}

/**
 * The distance from the row above to this one, or null when there is nothing
 * worth saying.
 *
 * **An entry whose date is not a day never produces a marker.** A month, a year,
 * or an unknown date carries a real range rather than a point, and the distance
 * between two ranges is not a number the person gave. Rule 17 and `DESIGN.md`
 * 10.9 both say the app never invents precision, and a confident "Three weeks
 * earlier" derived from two vague months would be exactly that.
 */
private fun gapAbove(
    entry: Repository.TrailEntry,
    previous: Repository.TrailEntry?,
    zone: ZoneId,
): Distance.Gap? {
    if (previous == null) return null
    if (!Edtf.isDayPrecise(entry.occurredEdtf) || !Edtf.isDayPrecise(previous.occurredEdtf)) {
        return null
    }
    return Distance.between(
        olderMillis = entry.occurredStart,
        newerMillis = previous.occurredStart,
        zone = zone,
    )
}

/**
 * One position on the route: the dashed line, an optional node, and whatever
 * sits beside them.
 *
 * Drawn by the shared spine rather than here, so the chapter list and the care
 * threads read as the same shape rather than as unrelated lists.
 * `DESIGN.md` section 5.2.3.
 */
@Composable
private fun RouteRow(
    draw: Float,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
    node: Color? = null,
    nodeDelayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val colors = HealthTrail.colors
    SpineRow(
        continuesAbove = continuesAbove,
        continuesBelow = continuesBelow,
        node = node,
        routeColor = colors.gold,
        dash = RouteDash.TRAIL,
        progress = draw,
        content = content,
    )
}

/**
 * What the node's color says the entry is, per section 5.2.
 *
 * The three the design names each get their own. Everything else takes the quiet
 * non-text ink rather than borrowing one of the three, because a measurement
 * wearing the incident color would be the app saying something about it that is
 * not true.
 */
@Composable
internal fun nodeColor(kind: String): Color = entryHue(kind).base

/**
 * One entry, as the trail shows it: a mono eyebrow carrying the date and what
 * kind of thing it was, then the line the person wrote.
 *
 * The whole row is one target and it opens the entry. The pin is the one other
 * thing on it, and it is a real control with its own label rather than a mark
 * that only responds to a gesture nobody was told about.
 */
@Composable
private fun TrailRow(
    entry: Repository.TrailEntry,
    onOpen: () -> Unit,
    /**
     * True when the band above this row has already named the month and year.
     *
     * **The row then says the weekday and the day.** #361: forty three rows
     * under a header reading "June 2026" each began by saying June 2026 again,
     * which is most of the ink on the most read screen in the app and is why a
     * long list read as a wall. `EventDateText.withinMonth` keeps the hedge and
     * refuses to shorten anything coarser than a day.
     */
    withinMonth: Boolean,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    val date = if (withinMonth) {
        EventDateText.withinMonth(strings, entry.occurredEdtf)
    } else {
        EventDateText.render(strings, entry.occurredEdtf)
    }
    val kind = strings[kindNameKey(entry.kind)]
    val title = entry.title?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) } ?: kind

    // **A container the eye can find**, `docs/V4.md` 6.1 item 4. The row drew
    // itself on a transparent background, so sixteen hundred entries were bare
    // text on the canvas with a dotted line beside them and nothing to say
    // where one stopped and the next began. Material's card is the surface, and
    // its own state layer is the answer to the touch.
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Space.none),
        // The tap, the tag and the reader's one stop on one node,
        // `docs/TRAPS.md`. A reader that stopped twice per entry would stop
        // three thousand times in this notebook.
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TrailTags.entry(entry.id))
            .semantics(mergeDescendants = true) { },
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.s, horizontal = Space.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // **State, not a control.** The pin itself is on the entry screen.
            // A mark here says this is one of the ones you keep coming back to;
            // it does not offer to change that, because sixteen hundred rows
            // carrying a second target was the loudest thing on the screen.
            if (entry.pinnedAt != null) {
                PinMark(tint = colors.goldInk)
                Spacer(Modifier.width(Space.xs))
            }
            Text(
                text = strings("trail.row.eyebrow", "date" to date, "kind" to kind),
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
        }

        Spacer(Modifier.height(Space.xs))

        // A blank title is normal and always has been: the capture form requires
        // nothing. The kind is what the app knows for certain, so the row never
        // shows an empty line where the subject should be.
        // **Isolated, because it is what somebody typed.** `DESIGN.md` 15: the
        // person's own words are isolated wherever they are rendered, not only
        // where they are joined. A Latin title in an Arabic layout is a run
        // going the other way, and without an isolate its final period lands on
        // the wrong side. This is the most read row in the app. #226.
        Text(
            text = Bidi.isolate(title),
            style = HealthTrail.type.bodyL,
            color = colors.ink,
        )

        if (entry.threads.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            ThreadTrace(threads = entry.threads)
        }

        // The tray already offers to file this. Saying so here is the other half
        // of that link, per rule 18: the tray shows the entry, so the entry says
        // it is in the tray. Stated rather than styled as a warning, because
        // being unfiled is a state and not a mistake.
        if (entry.isUnfiled) {
            Spacer(Modifier.height(Space.xs))
            Eyebrow(text = strings["trail.unfiled"])
        }
    }
    }
}

/**
 * Which threads this entry runs through, as the route dots the rest of the app
 * already uses for them.
 *
 * The color is an index into the theme's routes rather than a stored color, so
 * the dark theme substitution happens in the theme and a stored color can never
 * fail contrast. The label always sits next to the dot: color alone carries no
 * meaning, per section 9.
 */
@Composable
private fun ThreadTrace(threads: List<Repository.CareThread>) {
    val colors = HealthTrail.colors

    Column {
        for (thread in threads) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteSwatch(
                    color = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size],
                    index = thread.colorIndex,
                )
                Spacer(Modifier.width(Space.s))
                Text(
                    text = Bidi.isolate(thread.label),
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
                )
            }
        }
    }
}

/**
 * The catalog key naming a kind of entry, for rows the person left untitled.
 *
 * **A record is named with a noun, not with the button that made it.** This
 * pointed at the `capture.*` keys, so a visit already written down appeared on
 * the trail as "Log a visit": the imperative on the button the person had
 * already pressed, sitting where the description of the thing belongs.
 */
internal fun kindNameKey(kind: String): String = when (kind) {
    "call" -> "entry.kind.call"
    "visit" -> "entry.kind.visit"
    "incident" -> "entry.kind.incident"
    "measurement" -> "entry.kind.measurement"
    "question" -> "entry.kind.question"
    "document" -> "entry.kind.document"
    else -> "entry.kind.note"
}
