package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.goldHue
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.Road
import com.kamsiob.healthtrail.ui.v4.Stop
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object ChapterTags {
    const val NAME = "chapters"
    const val MOVED = "chapters_moved"
    const val MILESTONES = "chapters_milestones"
    fun row(id: String) = "chapter_$id"
    fun holds(id: String) = "chapter_holds_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_chapters"
}

/**
 * Chapters: the places somebody has been, on the road between them. Rewritten
 * onto `ui/v4`, #386.
 *
 * **Where they are now is the stop the road is at**, and everywhere else is
 * behind it. That is what `m3v4-2`'s road says with a gold disc in a halo, and
 * it is exactly this screen's question: a person opens Chapters to see where
 * somebody is, and the places before that are how they got there.
 *
 * **"Other places" rather than "Before that."** A chapter is current when it
 * has no end date, so a stay that began in December and is still running sits
 * above an overnight trip to the emergency department in June, and calling that
 * trip "before that" is the app stating an order the record contradicts. The
 * group is named rather than sequenced, and the dates on each row carry the
 * sequence.
 *
 * **They used to fold, and now they are on the road.** D185: a closed period
 * behind a door was the app deciding history is clutter. On the road it is
 * plainly behind you, which is the true thing and takes no tap to say.
 *
 * **The arc is a door from here**, per rule 18: chapters are where somebody
 * was, milestones are what happened along the way, and each one names the
 * chapter it fell in.
 */
@Composable
fun ChaptersScreen(
    chapters: List<Repository.Chapter>,
    onOpen: (Repository.Chapter) -> Unit,
    onOpenMilestones: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** What each chapter holds, by id, counted in the notebook's own words. */
    contents: Map<String, Repository.ChapterContents> = emptyMap(),
    /**
     * Says they moved somewhere new, which ends the current chapter and starts
     * one dated today, because somebody is in one place at a time.
     *
     * **Not gated on the care setting changing.** A transfer between two nursing
     * homes changes no setting and is still a move, which is what the change
     * flow could not express. D159.
     */
    onMoved: () -> Unit,
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.CHAPTERS)
    val current = chapters.filter { it.isCurrent }
    val earlier = chapters.filterNot { it.isCurrent }

    // **Where they are now leads, and everywhere else follows behind it.** The
    // road is read downward, so the stop it is at goes first and the places
    // already left run under it.
    val road = current + earlier

    Page(
        section = Repository.Section.CHAPTERS,
        eyebrow = strings["notebook.section.chapters"],
        eyebrowColor = hue.ink,
        title = strings["chapters.heading"],
        subtitle = strings["chapters.subtitle"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(ChapterTags.ROOT),
    ) {
        if (chapters.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about a notebook with no
                    // places written down yet.
                    Body(
                        text = strings["chapters.empty"],
                        color = colors.ink,
                        style = HealthTrail.type.bodyL,
                    )
                }
            }
            // The one thing to do from an empty Chapters screen, and the whole
            // of #377: without it, zero taps lead anywhere.
            item {
                Action(
                    label = strings["chapters.moved"],
                    onClick = onMoved,
                    mark = Symbols.chapters,
                    modifier = Modifier.testTag(ChapterTags.MOVED),
                )
            }
        }

        if (road.isNotEmpty()) {
            item { Eyebrow(text = strings["chapters.current"]) }
        }

        // **One item for the whole road**, because a page puts air between its
        // items and air between two stops is a road with gaps in it.
        if (road.isNotEmpty()) {
            item {
                Column {
                    road.forEachIndexed { index, chapter ->
                        Road(
                            // **Only the place they are now is the stop the road
                            // is at.** If every chapter were marked, none of them
                            // would be: `DESIGN.md` 5.2.1 keeps the ring rare so
                            // it means something.
                            stop = if (chapter.isCurrent) Stop.Now else Stop.Done,
                            continuesAbove = index > 0,
                            continuesBelow = index < road.lastIndex,
                            // **This road reads backward**, where they are now
                            // first and the places behind them under it, so
                            // every half of it has been traveled.
                            traveledBelow = true,
                        ) {
                            ChapterStop(
                                chapter = chapter,
                                holds = contents[chapter.id],
                                hue = hue,
                                onOpen = { onOpen(chapter) },
                            )
                        }
                    }
                }
            }
        }

        // **Under the list, where every section screen puts its own way in.**
        // The common errand here is opening a place already written down;
        // saying they moved is the rarer one, and it is not hidden.
        //
        // **Absent while the list is empty**, because the empty state above is
        // already carrying it and two of the same button on one screen is
        // competition rather than emphasis.
        if (chapters.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.s))
                Action(
                    label = strings["chapters.moved"],
                    onClick = onMoved,
                    mark = Symbols.chapters,
                    modifier = Modifier.testTag(ChapterTags.MOVED),
                )
            }
        }

        labeledBlock(
            leading = true,
            label = null,
            rows = listOf {
                ListRow(
                    title = strings["milestones.door"],
                    // **A sentence wraps**, D105. Capped at one line this read
                    // "The moments you decided were worth marking. Nothing here
                    // is" and stopped, which is the truncation rule 11 bans, and
                    // the half that was cut is the half saying the app works
                    // nothing out.
                    support = strings["milestones.subtitle"],
                    mark = Symbols.trail,
                    markHue = goldHue(),
                    isDoor = true,
                    onClick = onOpenMilestones,
                    clickLabel = strings["open.action"],
                    modifier = Modifier.testTag(ChapterTags.MILESTONES),
                )
            },
        )
    }
}

/**
 * One place, as a stop on the road.
 *
 * **The dates are the eyebrow and they are absent entirely when nobody gave
 * any.** An empty date line would be worse than none, rule 13.
 *
 * **What the chapter holds is counted in the notebook's own units**, and a count
 * of zero is left out rather than printed as "no documents": a chapter is a
 * place somebody was, not a checklist of what they collected there.
 */
@Composable
private fun ChapterStop(
    chapter: Repository.Chapter,
    holds: Repository.ChapterContents?,
    hue: com.kamsiob.healthtrail.ui.theme.TabHue,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val started = chapter.startedEdtf?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.padding(bottom = Space.sm)) {
        Block(
            tone = if (chapter.isCurrent) BlockTone.Section else BlockTone.Quiet,
            hue = hue,
            modifier = Modifier
                .semantics(mergeDescendants = true) { }
                .clickable(
                    role = Role.Button,
                    onClickLabel = strings["chapters.open"],
                    onClick = onOpen,
                )
                .testTag(ChapterTags.row(chapter.id)),
        ) {
            started?.let {
                val rendered = EventDateText.render(strings, it)
                Eyebrow(
                    text = if (chapter.isCurrent) {
                        strings("chapters.since", "date" to rendered)
                    } else {
                        rendered
                    },
                    fixed = false,
                    color = if (chapter.isCurrent) hue.ink else colors.ink2,
                )
            }

            // **The place's own name, isolated.** #226: a Latin name inside an
            // Arabic layout lays out against the surrounding direction rather
            // than within its own box, and its final punctuation lands on the
            // wrong side.
            Body(
                text = Bidi.isolate(chapter.name),
                color = colors.ink,
                style = HealthTrail.type.displayS,
            )

            chapter.reason?.takeIf { it.isNotBlank() }?.let {
                Body(text = Bidi.isolate(it))
            }
            chapter.notes?.takeIf { it.isNotBlank() }?.let {
                Body(text = Bidi.isolate(it), style = HealthTrail.type.bodyS)
            }

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
                // **No label above it, and that is a correction.** "WHAT IS IN
                // THIS CHAPTER" sat inside every card on the screen, four
                // times down one scroll, naming what the line under it already
                // says: "281 entries, 5 documents" is not ambiguous. An
                // eyebrow names a group of things; repeating one inside each
                // member of a list is furniture, and no approved drawing does
                // it. Seen on the phone, rule 21.
                //
                // bidi-ok: every part is a catalog phrase around a number, in
                // the app's own words rather than anything typed.
                Body(
                    text = Bidi.join(parts),
                    color = colors.ink,
                    modifier = Modifier.testTag(ChapterTags.holds(chapter.id)),
                )
            }
        }
    }
}
