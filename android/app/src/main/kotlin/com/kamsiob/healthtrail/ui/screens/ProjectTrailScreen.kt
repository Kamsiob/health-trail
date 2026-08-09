package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Distance
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.DistanceMarker
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import java.time.ZoneId

object ProjectTrailTags {
    const val NAME = "project-trail"
    const val FILTERS = "project-trail-filters"
    fun filter(key: String) = "project-trail-filter-$key"
    fun row(id: String) = "project-trail-row-$id"
}

/** What the filter chips offer, which is only ever what the project actually has. */
private const val FILTER_ALL = "all"

/**
 * One project's own trail. `DESIGN.md` 20.5, screen 11.
 *
 * **Three sources on one spine.** What was said, where the road turned, and the
 * dates the process is running against. A long process is not only its calls:
 * the road reaching Decision and a response window closing are as much a part
 * of what happened, and drawing them on separate screens would leave the person
 * to interleave three lists in their head, which is rule 20 pointed the wrong
 * way.
 *
 * **Oldest first, which is the opposite of the main trail.** The trail answers
 * "what happened lately" and opens on now. A process is read forward: how it
 * got here, and what is coming. The grid draws it forward for the same reason,
 * and the quiet stretches say time passed rather than that anybody let it.
 *
 * **The gaps say "pass", not "earlier".** The main trail's markers are written
 * for a list read backwards and would be plainly wrong here.
 *
 * **Nothing on this screen is a verdict**, section 22 and rule 2. A date that
 * has not arrived is drawn like every other row; nothing is late, nothing is
 * missed, and no row is colored by how it is going.
 *
 * **The entry rows are doors and the rest are not.** An entry opens itself, and
 * the entry names this project back, so rule 18 holds both ways. A stage and a
 * date are drawn bare, the way [com.kamsiob.healthtrail.ui.components.RoadStrip]
 * is: they are what happened, and the controls that change them live on the
 * screens that own them. A row that responded to a press and then did nothing
 * would read as broken, D42.
 */
@Composable
fun ProjectTrailScreen(
    projectName: String,
    items: List<Repository.ProjectTrailItem>,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    // **Only the kinds this project actually has.** A chip for something with
    // nothing behind it is a control that does nothing on press, D42, and on a
    // young project most of them would be exactly that.
    val counts = remember(items) {
        items.groupingBy { filterKeyFor(it) }.eachCount()
    }
    var filter by rememberSaveable(projectName) { mutableStateOf(FILTER_ALL) }
    // A filter can outlive the thing it filtered: removing the last call while
    // Calls is chosen would leave an empty screen with no way to tell why.
    val active = if (filter == FILTER_ALL || counts.containsKey(filter)) filter else FILTER_ALL
    val shown = remember(items, active) {
        if (active == FILTER_ALL) items else items.filter { filterKeyFor(it) == active }
    }

    SectionScaffold(
        name = ProjectTrailTags.NAME,
        title = strings["notebook.section.projects"],
        headingKey = "project.trail",
        subtitle = Bidi.isolate(projectName),
        onBack = onBack,
        backLabelKey = "section.back.project",
        modifier = modifier,
    ) {
        if (items.isEmpty()) {
            item {
                // **5.17's solution rather than a second one.** Written first
                // as a gray paragraph under the subtitle with the rest of the
                // screen blank, which is the exact shape #274 already had to
                // fix once: it reads as a screen that failed to load. The
                // ground, a line that says what this place is for, then the
                // paragraph.
                //
                // **The ground alone, with no section drawing.** A project is
                // not one of the twelve sections, which is what `section = null`
                // means here rather than an omission.
                //
                // **No action.** Everything that would put something on this
                // trail is one screen back on the project, and offering "Log a
                // call" from here would be a second door to a control the
                // person has just come past.
                SectionEmpty(
                    name = ProjectTrailTags.NAME,
                    lead = strings["project.trail.empty.lead"],
                    text = strings["project.trail.none"],
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_TALL),
                )
            }
            return@SectionScaffold
        }

        // **Only when there is more than one kind to choose between.** One chip
        // reading "All" beside nothing is a picker with one option, which says
        // the screen could be filtered and then offers no filtering.
        if (counts.size > 1) {
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag(ProjectTrailTags.FILTERS),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    ChoiceChip(
                        label = strings("project.trail.filter.all", "count" to items.size),
                        selected = active == FILTER_ALL,
                        onClick = { filter = FILTER_ALL },
                        modifier = Modifier.testTag(ProjectTrailTags.filter(FILTER_ALL)),
                    )
                    // **In the order the trail itself runs**, so the chips do
                    // not reshuffle as a project grows and a person's hand
                    // stops landing on the same one.
                    for (key in counts.keys.sortedBy { filterOrder(it) }) {
                        ChoiceChip(
                            label = strings[labelKeyFor(key)],
                            selected = active == key,
                            onClick = { filter = key },
                            modifier = Modifier.testTag(ProjectTrailTags.filter(key)),
                        )
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        itemsIndexed(shown) { index, item ->
            val previous = shown.getOrNull(index - 1)
            // **Only between two dates the person actually gave.** Rule 17
            // forbids inventing precision, and a gap measured from a month the
            // person wrote as a month is arithmetic on something that was never
            // that precise.
            val gap = if (Edtf.isDayPrecise(previous?.whenEdtf) && Edtf.isDayPrecise(item.whenEdtf)) {
                Distance.between(previous?.whenStart, item.whenStart, zone)
            } else {
                null
            }

            SpineRow(
                continuesAbove = index > 0,
                continuesBelow = index < shown.lastIndex,
                node = nodeFor(item),
                routeColor = colors.gold,
                dash = RouteDash.TRAIL,
                state = if (item.kind == Repository.ProjectTrailKind.STAGE) {
                    // **The road turning is the one thing on this line that
                    // changed what the project is.** The grid rings it, and a
                    // milestone is the ring this app already has.
                    Waypoint.MILESTONE
                } else {
                    Waypoint.HAPPENED
                },
            ) {
                Column {
                    gap?.let {
                        DistanceMarker(strings(passedKey(it.key), "count" to it.count))
                        Spacer(Modifier.height(Space.s))
                    }
                    TrailItemRow(
                        item = item,
                        onOpen = item.entry?.let { entry -> { onOpenEntry(entry) } },
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                Text(
                    text = strings["project.trail.filter.none"],
                    style = type.bodyM,
                    color = colors.ink2,
                    modifier = Modifier.padding(vertical = Space.l),
                )
            }
        }
    }
}

/**
 * One row, whichever of the three kinds it is.
 *
 * **The same shape as a trail row**, a mono eyebrow carrying the date and what
 * kind of thing it was, then the line underneath. Two shapes for one line would
 * make the stages and the dates read as a different screen's furniture that had
 * wandered in.
 */
@Composable
private fun TrailItemRow(
    item: Repository.ProjectTrailItem,
    onOpen: (() -> Unit)?,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val type = HealthTrail.type

    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, Color.Transparent)
    val date = EventDateText.render(strings, item.whenEdtf)
    val kind = strings[kindLabelFor(item)]
    val line = item.entry
        ?.let { entry -> entry.title?.takeIf { it.isNotBlank() } ?: entry.body }
        ?.takeIf { it.isNotBlank() }
        ?: item.title.takeIf { it.isNotBlank() }
        ?: kind

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(surface)
            .then(
                if (onOpen != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        // The surface is the answer to the touch, 5.14.
                        indication = null,
                        role = Role.Button,
                        onClick = onOpen,
                    )
                } else {
                    Modifier
                },
            )
            .testTag(ProjectTrailTags.row(item.id))
            // One stop for the reader: the eyebrow and the line are one thing.
            .semantics(mergeDescendants = true) { }
            .padding(vertical = Space.s, horizontal = Space.sm),
    ) {
        Text(
            text = strings("trail.row.eyebrow", "date" to date, "kind" to kind),
            style = type.mono,
            color = colors.ink2,
        )
        Text(
            // The person's own words, isolated so an Arabic layout does not
            // reorder an English sentence against it. Section 15.
            text = Bidi.isolate(line),
            style = type.bodyM,
            color = colors.ink,
        )
        item.note?.takeIf { it.isNotBlank() && it != line }?.let { note ->
            Text(
                text = Bidi.isolate(note),
                style = type.bodyS,
                color = colors.ink2,
            )
        }
    }
}

/**
 * Which chip a row answers to, from a closed set.
 *
 * **Closed on purpose.** `entry.kind` allows `transfer` and `milestone` as well
 * as the six the catalog names, and the chip label is looked up by a computed
 * key that `check_string_keys.py` cannot see, so an open set here is a missing
 * catalog key and `Strings.resolve` throws rather than falling back. That is a
 * crash on opening the trail of a project that happens to hold a transfer.
 * This mirrors `kindNameKey`'s own fallback so the two cannot disagree.
 */
private fun filterKeyFor(item: Repository.ProjectTrailItem): String = when (item.kind) {
    Repository.ProjectTrailKind.STAGE -> "stages"
    Repository.ProjectTrailKind.DATE -> "dates"
    Repository.ProjectTrailKind.ENTRY -> when (item.entry?.kind) {
        "call", "visit", "incident", "measurement", "question", "document" ->
            item.entry.kind
        else -> "note"
    }
}

/** The order the chips sit in, which never changes as a project grows. */
private fun filterOrder(key: String): Int = when (key) {
    "call" -> 0
    "visit" -> 1
    "document" -> 2
    "incident" -> 3
    "measurement" -> 4
    "question" -> 5
    "stages" -> 6
    "dates" -> 7
    else -> 8
}

/**
 * The chip's own label, which is a plural and not the row's eyebrow.
 *
 * **`entry.kind.call` is "A call"**, which is right over one row and reads as a
 * mislabeled control on a chip that selects all of them. The grid draws
 * "Calls".
 */
private fun labelKeyFor(key: String): String = "project.trail.filter.$key"

private fun kindLabelFor(item: Repository.ProjectTrailItem): String = when (item.kind) {
    Repository.ProjectTrailKind.STAGE -> "project.trail.kind.stage"
    Repository.ProjectTrailKind.DATE -> "project.trail.kind.date"
    Repository.ProjectTrailKind.ENTRY -> kindNameKey(item.entry?.kind.orEmpty())
}

/**
 * The forward-reading twin of the main trail's gap phrase.
 *
 * `trail.gap.*` reads "23 days earlier", which is right for a list read
 * backwards and plainly wrong on one read forwards.
 */
private fun passedKey(gapKey: String): String =
    gapKey.replace("trail.gap.", "project.trail.gap.")

/**
 * What the node's color says a row is, per section 5.2.
 *
 * **A date takes the quiet non-text ink rather than one of the named colors.**
 * Borrowing the incident red for a response window would be the app saying
 * something about that window, which is rule 2's line.
 */
@Composable
private fun nodeFor(item: Repository.ProjectTrailItem): Color {
    val colors = HealthTrail.colors
    return when (item.kind) {
        Repository.ProjectTrailKind.STAGE -> colors.gold
        Repository.ProjectTrailKind.DATE -> colors.ink3
        Repository.ProjectTrailKind.ENTRY -> when (item.entry?.kind) {
            "call" -> colors.gold
            "visit" -> colors.blue
            "incident" -> colors.alert
            else -> colors.ink3
        }
    }
}
