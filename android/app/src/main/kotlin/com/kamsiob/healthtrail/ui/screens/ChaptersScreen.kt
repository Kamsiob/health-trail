package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.FoldRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ChapterTags {
    const val NAME = "chapters"
    const val EARLIER_FOLD = "chapters_earlier_fold"
    fun row(id: String) = "chapter_$id"
}

/**
 * Chapters: the places they have been, in order.
 *
 * **A chapter is a place, and the record is organized around them** because
 * that is how the person actually remembers it. "When she was at Maplewood" is
 * how somebody recalls a period, not a date range, and `MASTER_SPEC.md` section
 * 4.5 builds the journey view on exactly that.
 *
 * **Where they are now is separated from where they have been.** It is the one
 * a person needs most often, it is the answer to a question somebody gets asked
 * on the phone, and it is identified by having no end date rather than by a
 * flag that could disagree with the dates.
 *
 * **A place with no dates at all is a complete row.** Setup asks where they are
 * and asks for nothing else, so the very first chapter in most notebooks is a
 * name and nothing more. That is not a partial record, it is what the person
 * knew at the time.
 *
 * The chapter journey of section 4.5, which pulls a chapter's incidents,
 * documents, and archived care team together, is not built. This lists the
 * places and says when. It is the layer that has to exist first.
 */
@Composable
fun ChaptersScreen(
    /** Opens the chapter itself. Every stop on the journey was a dead end. */
    onOpen: (Repository.Chapter) -> Unit,
    chapters: List<Repository.Chapter>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val current = chapters.filter { it.isCurrent }
    val earlier = chapters.filterNot { it.isCurrent }
    var earlierOpen by rememberSaveable { mutableStateOf(false) }

    SectionScaffold(
        name = ChapterTags.NAME,
        title = strings["notebook.section.chapters"],
        subtitle = strings["chapters.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.CHAPTERS,
        headingKey = "chapters.heading",
    ) {
        if (chapters.isEmpty()) {
            item { SectionEmpty(name = ChapterTags.NAME, text = strings["chapters.empty"], section = Repository.Section.CHAPTERS, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION)) }
        }

        if (current.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "chapters.current")
                Spacer(Modifier.height(Space.headerGap))
            }
            current.forEachIndexed { index, chapter ->
                item(key = chapter.id) {
                    // **A current chapter is a milestone waypoint**, because it
                    // is where the person actually is, and it is the only place
                    // on this screen that gets one. If every chapter were ringed
                    // none of them would be. `DESIGN.md` section 5.2.1.
                    ChapterSpineRow(
                        chapter = chapter,
                        onOpen = { onOpen(chapter) },
                        state = Waypoint.MILESTONE,
                        continuesAbove = index > 0,
                        continuesBelow = index < current.lastIndex,
                    )
                }
            }
        }

        if (earlier.isNotEmpty()) {
            // **The road here folds**, per grid screen 19 and law 4: closed
            // periods collapse into a fold rather than sitting open beside the
            // place the person is now. History serves the present.
            //
            // **"Other places" rather than "Before that."**
            //
            // A chapter is current when it has no end date, so a stay that
            // began in December and is still running sits above an overnight
            // trip to the emergency department in June, and calling that trip
            // "before that" is the app stating an order the record contradicts.
            // Seen with a month six fixture, where exactly that pair occurs.
            //
            // A person leaves a place and comes back, goes to hospital for a
            // night and returns, or moves between a facility and home
            // repeatedly. The heading has to be true in all of those, so it
            // names the group rather than sequencing it, and the dates on each
            // row carry the sequence.
            item {
                Spacer(Modifier.height(Space.s))
                FoldRow(
                    labelKey = "chapters.earlier",
                    expanded = earlierOpen,
                    onToggle = { earlierOpen = !earlierOpen },
                    count = earlier.size.toString(),
                    modifier = Modifier.testTag(ChapterTags.EARLIER_FOLD),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            if (earlierOpen) {
                earlier.forEachIndexed { index, chapter ->
                    item(key = chapter.id) {
                        ChapterSpineRow(
                            chapter = chapter,
                            onOpen = { onOpen(chapter) },
                            state = Waypoint.HAPPENED,
                            continuesAbove = index > 0,
                            continuesBelow = index < earlier.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A chapter on the spine.
 *
 * **Chapters are a journey through places, which is a trail by definition**, and
 * this screen was a column of cards that happened to be about places. It is the
 * same shape as the timeline, the same shape as a thread, and now it looks like
 * it. `DESIGN.md` section 5.2.3.
 *
 * **The line is continuous rather than dashed**, because this is the person's
 * actual path rather than a filter over entries. That is the distinction 5.2
 * always drew and only two views ever honored.
 */
@Composable
private fun ChapterSpineRow(
    chapter: Repository.Chapter,
    onOpen: () -> Unit,
    state: Waypoint,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
) {
    val colors = HealthTrail.colors
    SpineRow(
        continuesAbove = continuesAbove,
        continuesBelow = continuesBelow,
        node = if (chapter.isCurrent) colors.gold else colors.ink3,
        state = state,
        routeColor = colors.gold,
        dash = null,
    ) {
        Column {
            ChapterRow(chapter, onOpen)
            Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun ChapterRow(chapter: Repository.Chapter, onOpen: () -> Unit) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .clip(Radius.card)
            .background(surface)
            .removableByLongPress(
                label = strings["remove.hint"],
                onLongPress = {},
                onTap = onOpen,
                interactionSource = interaction,
            )
            .testTag(ChapterTags.row(chapter.id))
            .padding(Space.cardPadding),
    ) {
        // The dates are the eyebrow, and they are absent entirely when nobody
        // gave any. An empty date line would be worse than none.
        val started = chapter.startedEdtf?.takeIf { it.isNotBlank() }
        if (started != null) {
            val rendered = EventDateText.render(strings, started)
            Text(
                text = if (chapter.isCurrent) {
                    strings("chapters.since", "date" to rendered)
                } else {
                    rendered
                },
                style = HealthTrail.type.mono,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = chapter.name,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        chapter.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Spacer(Modifier.height(Space.xs))
            Text(text = reason, style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        chapter.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.xs))
            Text(text = notes, style = HealthTrail.type.bodyS, color = colors.ink2)
        }
    }
}
