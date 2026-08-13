package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.ui.components.FoldRowText
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.EmptyDrawing
import com.kamsiob.healthtrail.ui.components.GroupHeaderText
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object SearchTags {
    const val NAME = "search"
    const val ROOT = "search_root"
    const val FIELD = "search_field"
    const val EMPTY = "search_empty"
    const val NO_MATCH = "search_no_match"
    const val FAILED = "search_failed"
    fun result(id: String) = "search_result_$id"
    fun group(section: Repository.Section) = "search_group_${section.name.lowercase()}"
}

/**
 * Finding one thing in a notebook that has years in it.
 *
 * **This is the feature that makes a years long record usable at all.** By year
 * two there are hundreds of entries and "the call where they said the wound was
 * healing" is somewhere in them. Without this the app is a place things go in.
 * `MASTER_SPEC.md` 4.8 and issue #47.
 *
 * **Results are grouped by section, in notebook order**, which is the order the
 * person already learned from the table of contents. Not by how many matches
 * each section produced: a list whose order changes with the query is one
 * nobody can build a habit against.
 *
 * **Every result carries its chapter**, so the person always knows where in the
 * journey it happened. A result with no chapter says nothing rather than saying
 * "Unfiled", because plenty of things are recorded before anybody has said
 * where they belong and a placeholder there would be a claim the record does
 * not make.
 *
 * **Four states, all built**, per rule 11 and the acceptance criteria on #47:
 * nothing typed yet, one result, many results, and nothing found. The last is
 * written as a next step rather than as a dead end, per section 5.10.
 *
 * **Drawn on the spine**, dashed, because a search result is a filter over
 * entries rather than the person's own path. `DESIGN.md` 5.2.3 draws exactly
 * that distinction and this is the screen it was written for.
 */
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Repository.SearchHit>,
    onOpen: (Repository.SearchHit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when the read itself failed, which is not the same as finding nothing. */
    failed: Boolean = false,
    /**
     * What the way back says, because search is opened from two places.
     *
     * **Hardcoding "Back to More" named a place the person had not come from**
     * when they reached it from Today, which is the same defect fixed for the
     * section screens on 2026-08-01 and reintroduced the moment a second screen
     * grew two ways in. The caller knows where they came from; this screen does
     * not get to guess.
     */
    backLabelKey: String = "section.back.more",
) {
    val strings = LocalStrings.current
    var openSections by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val colors = HealthTrail.colors

    // Grouped here rather than in the query, because the query returns them in
    // section order already and grouping is a presentation question.
    val grouped = remember(results) { results.groupBy { it.section } }

    SectionScaffold(
        name = SearchTags.NAME,
        // **The chip stays "Search" rather than saying where you came from**,
        // because this screen has two doors: More and Today's search door.
        // A chip reading "More" would be a lie half the time, which is the
        // reason the other four could take that fix and this one could not.
        // #341.
        title = strings["search.title"],
        headingKey = "search.heading",
        subtitle = strings["search.subtitle"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier.testTag(SearchTags.ROOT),
    ) {
        item {
            HealthTrailTextField(
                label = strings["search.label"],
                value = query,
                onValueChange = onQueryChange,
                hint = strings["search.hint"],
                fieldTestTag = SearchTags.FIELD,
                singleLine = true,
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            )
            Spacer(Modifier.height(Space.l))
        }

        // **Nothing typed yet.** Not an error and not empty: it is the resting
        // state of a search box, and it says what can be found here.
        if (query.isBlank()) {
            item {
                SectionEmpty(
                    name = SearchTags.EMPTY,
                    text = strings["search.empty"],
                    modifier = Modifier.fillParentMaxHeight(0.5f),
                )
            }
            return@SectionScaffold
        }

        // **A failed read is not an empty result**, and saying "nothing
        // matches" when the search never ran tells somebody their record does
        // not contain what they are certain they wrote down. Section 10.3
        // requires an error state on every screen, and this is the screen where
        // confusing the two is most frightening.
        if (failed) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SearchTags.FAILED)
                        .fillParentMaxHeight(0.5f)
                        .padding(vertical = Space.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text(
                        text = strings["search.failed"],
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = strings["search.failed.note"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@SectionScaffold
        }

        // **Nothing found, written as a next step.** A dead end here is the
        // moment somebody decides the app lost their record.
        if (results.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SearchTags.NO_MATCH)
                        .fillParentMaxHeight(0.5f)
                        .padding(vertical = Space.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    EmptyDrawing(section = null)
                    Spacer(Modifier.height(Space.l))
                    Text(
                        text = strings("search.nomatch", "query" to query),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = strings["search.nomatch.next"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@SectionScaffold
        }

        // **The best answer first and large, per law 1.** A list of forty
        // results ranked by nothing a person can see is a list they read all of.
        // The first hit is the one the query matched best, so it says so and
        // takes the room to be read rather than scanned.
        val best = results.firstOrNull()
        if (best != null) {
            item(key = "best_${best.id}") {
                Text(
                    text = Bidi.join(
                        strings["search.best"],
                        strings[sectionKey(best.section)],
                    ),
                    style = HealthTrail.type.mono,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.xs))
                ResultRow(hit = best, onOpen = { onOpen(best) }, lead = true)
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **Everything else folded by kind, named and counted.** The section is
        // the kind a person thinks in, and the count is what tells them whether
        // opening it is worth the tap.
        val rest = results.drop(1).groupBy { it.section }
        for ((section, hits) in rest) {
            val open = section.name in openSections
            item(key = "group_${section.name}") {
                FoldRowText(
                    label = strings[sectionKey(section)],
                    expanded = open,
                    onToggle = {
                        openSections = if (open) {
                            openSections - section.name
                        } else {
                            openSections + section.name
                        }
                    },
                    count = hits.size.toString(),
                    modifier = Modifier.testTag(SearchTags.group(section)),
                )
                Spacer(Modifier.height(Space.cardGap))
            }

            if (!open) continue

            hits.forEachIndexed { index, hit ->
                item(key = hit.id) {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < hits.lastIndex,
                        node = colors.ink3,
                        routeColor = colors.ink3,
                        // Dashed, because a search result is a filter over the
                        // record rather than the person's own path. 5.2.3.
                        dash = RouteDash.TRAIL,
                    ) {
                        Column {
                            ResultRow(hit = hit, onOpen = { onOpen(hit) })
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One result.
 *
 * **The same row shape every list in this app uses:** what it is, then when,
 * then where. A person who has learned one row has learned every row, which is
 * the whole argument for typographic rhythm in `DESIGN.md` 10.8.
 */
@Composable
private fun ResultRow(
    hit: Repository.SearchHit,
    onOpen: () -> Unit,
    /** True for the best match, which leads the screen and is read rather than scanned. */
    lead: Boolean = false,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
            )
            .testTag(SearchTags.result(hit.id))
            .padding(Space.cardPadding),
    ) {
        // When and where, in mono, above the title. The same eyebrow the trail
        // puts a date in, so the two screens read as one app.
        val date = hit.occurredEdtf?.takeIf { it.isNotBlank() }
            ?.let { EventDateText.render(strings, it) }
        val where = hit.chapterName
        // Isolated and joined, because a date in one script beside a chapter
        // name in another is two runs and reorders without it.
        val eyebrow = Bidi.join(listOfNotNull(date, where))
        if (eyebrow.isNotEmpty()) {
            // bidi-ok: joined by Bidi.join two lines above, which isolates every part.
            Text(text = eyebrow, style = HealthTrail.type.bodyS, color = colors.ink2)
            Spacer(Modifier.height(Space.xs))
        }

        // **The kind, not "No title".** An entry with no title is ordinary,
        // because the capture form requires nothing, and the kind is what the
        // app knows for certain. The trail row has always fallen back this way
        // and a search result did not, so the same entry read differently in
        // two places.
        Text(
            text = Bidi.isolate(
                hit.title.ifBlank {
                    hit.kind?.let { strings[kindNameKey(it)] } ?: strings["search.untitled"]
                },
            ),
            style = if (lead) HealthTrail.type.hero else HealthTrail.type.displayS,
            color = colors.ink,
        )

        hit.detail?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Space.xs))
            Text(
                        text = Bidi.isolate(it),
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                        // **Three lines and then a mark**, never three lines
                        // and a clean cut. A search result is somebody's own
                        // sentence and the row has to say there is more of it.
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
        }
    }
}

/**
 * The catalog key naming a section.
 *
 * Derived from the enum rather than listed, because a thirteen line mapping is
 * thirteen chances to disagree with the notebook, and the notebook is where the
 * person learned these words.
 */
private fun sectionKey(section: Repository.Section): String =
    "notebook.section." + section.name.lowercase()
