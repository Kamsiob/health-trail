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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.components.FoldRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ChapterTags {
    const val NAME = "chapters"
    const val EARLIER_FOLD = "chapters_earlier_fold"
    const val MILESTONES = "chapters_milestones"
    const val MOVED = "chapters_moved"
    fun row(id: String) = "chapter_$id"
    fun holds(id: String) = "chapter_holds_$id"
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
    /** Opens the milestone arc, which has no section of its own to live in. */
    onOpenMilestones: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What each chapter holds, counted, by chapter id. #356.
     *
     * **Grid screen 19 makes the current place a card for exactly this line.**
     * The card carried a name and a date and said nothing about what happened
     * there, which is what somebody opens it for. Absent for a chapter with
     * nothing in it, so the row never says four zeroes.
     */
    contents: Map<String, Repository.ChapterContents> = emptyMap(),
    /**
     * Records that they are somewhere new. #377.
     *
     * **This screen had no way to add anything to it.** A place could be
     * created from the optional setup field and from a care setting change,
     * and nowhere else, so somebody whose person transferred between two
     * facilities of the same kind could not record it at all. An empty
     * Chapters screen was a drawing, a sentence saying places can be added
     * whenever they move, and nothing to touch. Found by looking at it.
     *
     * **A move rather than an add**, which is what keeps the model honest:
     * naming a place ends the one they were in and starts this one, both
     * dated today, because somebody is in one place at a time. That is
     * `moveToChapter`, which already existed for the setting change and was
     * always the right writer for this.
     *
     * **Not gated on the care setting changing.** A transfer between two
     * nursing homes changes no setting and is still a move, which is what the
     * change flow could not express. D159.
     */
    onMoved: () -> Unit,
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
            item {
                SectionEmpty(
                    name = ChapterTags.NAME,
                    text = strings["chapters.empty"],
                    section = Repository.Section.CHAPTERS,
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                    // The one thing to do from an empty Chapters screen, and
                    // the whole of #377: without it, zero taps lead anywhere.
                    actionLabel = strings["chapters.moved"],
                    onAction = onMoved,
                )
            }
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
                        holds = contents[chapter.id],
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
                            holds = contents[chapter.id],
                            onOpen = { onOpen(chapter) },
                            state = Waypoint.HAPPENED,
                            continuesAbove = index > 0,
                            continuesBelow = index < earlier.lastIndex,
                        )
                    }
                }
            }
        }

        // **Under the list, where care threads puts its own.** The common
        // errand here is opening a place already written down; saying they
        // moved is the rarer one, and it is not hidden: 13.5 calls a
        // capability only its author can find unfinished.
        //
        // **Absent while the list is empty**, because the empty state above is
        // already carrying it and two of the same button on one screen is the
        // competition 10.8 is about.
        if (chapters.isNotEmpty()) {
            item(key = "moved") {
                Spacer(Modifier.height(Space.s))
                QuietButton(
                    label = strings["chapters.moved"],
                    onClick = onMoved,
                    modifier = Modifier.testTag(ChapterTags.MOVED),
                )
            }
        }

        // **The arc is a door from here, per section 14 and rule 18.** Chapters
        // are where somebody was; milestones are what happened along the way,
        // and each one names the chapter it fell in. A screen that could not be
        // reached would be the discoverability failure 13.5 names, and there is
        // no thirteenth section to put it in.
        item {
            Spacer(Modifier.height(Space.sectionGap))
            GroupedSurface {
                DenseRow(
                    title = strings["milestones.door"],
                    subtitle = strings["milestones.subtitle"],
                    // **A sentence wraps**, D105. Capped at one line this read
                    // "The moments you decided were worth marking. Nothing
                    // here is" and stopped, which is the truncation rule 11
                    // bans outright, and it is worse than a shortened label
                    // because the half that was cut is the half that says the
                    // app works nothing out.
                    subtitleMaxLines = Int.MAX_VALUE,
                    leading = {
                        WaypointDot(
                            color = HealthTrail.colors.gold,
                            state = Waypoint.MILESTONE,
                        )
                    },
                    chevron = true,
                    divider = false,
                    onClick = onOpenMilestones,
                    modifier = Modifier.testTag(ChapterTags.MILESTONES),
                )
            }
            Spacer(Modifier.height(Space.l))
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
    holds: Repository.ChapterContents?,
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
            ChapterRow(chapter, holds, onOpen)
            Spacer(Modifier.height(Space.cardGap))
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Repository.Chapter,
    holds: Repository.ChapterContents?,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .clip(Radius.cardLarge)
            // **#231, and this one was not on the issue's list of five.** The
            // acceptance criterion is that no caller passes an empty
            // `onLongPress` anywhere, which is what found it.
            .openableByTap(
                label = strings["chapters.open"],
                onTap = onOpen,
                resting = colors.card,
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
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
        }

        // **The place's own name, isolated.** #226: a Latin name inside an
        // Arabic layout lays out against the surrounding direction rather than
        // within its own box, and its final punctuation lands on the wrong side.
        Text(
            text = Bidi.isolate(chapter.name),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        chapter.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Spacer(Modifier.height(Space.xs))
            Text(text = Bidi.isolate(reason), style = HealthTrail.type.bodyM, color = colors.ink2)
        }

        chapter.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(Space.xs))
            Text(text = Bidi.isolate(notes), style = HealthTrail.type.bodyS, color = colors.ink2)
        }

        // **What the chapter holds, counted**, per grid screen 19. Each part
        // counts in its own units, the same words the notebook uses since
        // #347, and **a count of zero is left out entirely** rather than
        // printed as "no documents": a chapter is a place somebody was, not a
        // checklist of what they collected there.
        if (holds != null && !holds.isEmpty) {
            val parts = listOfNotNull(
                strings("notebook.count.trail", "count" to holds.entries)
                    .takeIf { holds.entries > 0 },
                strings("notebook.count.documents", "count" to holds.documents)
                    .takeIf { holds.documents > 0 },
                strings("notebook.count.care_team", "count" to holds.people)
                    .takeIf { holds.people > 0 },
                strings("today.open.incidents", "count" to holds.openIncidents)
                    .takeIf { holds.openIncidents > 0 },
            )
            Spacer(Modifier.height(Space.s))
            // bidi-ok: every part is a catalog phrase around a number, in the
            // app's own words rather than anything the person typed.
            Text(
                text = strings["chapters.holds"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Bidi.join(parts),
                style = HealthTrail.type.bodyM,
                color = colors.ink,
                modifier = Modifier.testTag(ChapterTags.holds(chapter.id)),
            )
        }
    }
}
