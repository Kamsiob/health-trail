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
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.Road
import com.kamsiob.healthtrail.ui.v4.Stop

object ChapterTags2 {
    const val NAME = "chapter"
    fun entry(id: String) = "chapter_entry_$id"
    fun incident(id: String) = "chapter_incident_$id"
    fun document(id: String) = "chapter_document_$id"
    const val RENAME = "chapter_rename"
    fun milestone(id: String) = "chapter_milestone_$id"
    const val REMOVE = "chapters_remove"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_chapter"
}

/**
 * One place, and what happened while they were there. Rewritten onto `ui/v4`,
 * #386.
 *
 * `MASTER_SPEC.md` 4.6: inside a chapter, its dates, why the stay began, its
 * incidents, its documents, and any project that began there.
 *
 * **A chapter is this app's unit of "where", and it could not be opened.** The
 * chapters screen drew the journey and every stop on it was a dead end.
 *
 * **What went wrong leads.** A stay with something unresolved in it is a stay
 * defined by that, and it is what somebody opens a chapter looking for. That is
 * a judgment about ordering and it is written down here rather than left
 * implicit.
 *
 * **Nothing is folded any more.** Four groups behind four doors meant a person
 * opening a place they lived in for a year had to guess which door held what
 * they came for. Each group carries its own label and its own rows, in the order
 * that answers the question first. D185.
 *
 * **The entries are a road**, because they are what happened while they were
 * here, in order. The incidents, the milestones and the papers are lists: they
 * are things the stay held rather than a sequence through it. D187.
 */
@Composable
fun ChapterScreen(
    detail: Repository.ChapterDetail,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onOpenIncident: (Repository.Incident) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back.chapters",
    /** Opens the screen that corrects this chapter's name. #374. */
    onRename: () -> Unit = {},
    /**
     * Takes it out of the notebook, with the confirmation the caller owns.
     * **Anything that can be added can be removed**, the owner's rule,
     * 2026-08-16: a record started by mistake should not be forever.
     */
    onRemove: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.CHAPTERS)
    val chapter = detail.chapter

    Page(
        eyebrow = strings["notebook.section.chapters"],
        eyebrowColor = hue.ink,
        title = Bidi.isolate(chapter.name),
        subtitle = chapter.startedEdtf?.takeIf { it.isNotBlank() }
            ?.let { started ->
                val from = EventDateText.render(strings, started)
                chapter.endedEdtf?.takeIf { it.isNotBlank() }
                    ?.let { ended ->
                        strings(
                            "chapter.between",
                            "from" to from,
                            "to" to EventDateText.render(strings, ended),
                        )
                    }
                    ?: strings("chapters.since", "date" to from)
            }
            ?: strings["chapter.nodates"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        modifier = modifier.testTag(ChapterTags2.ROOT),
    ) {
        if (!chapter.reason.isNullOrBlank() || !chapter.notes.isNullOrBlank()) {
            item {
                Block(tone = BlockTone.Section, hue = hue) {
                    // The person's own words about why they moved. #226.
                    chapter.reason?.takeIf { it.isNotBlank() }?.let {
                        Body(
                            text = Bidi.isolate(it),
                            color = colors.ink,
                            style = HealthTrail.type.bodyL,
                        )
                    }
                    chapter.notes?.takeIf { it.isNotBlank() }?.let {
                        Body(text = Bidi.isolate(it))
                    }
                }
            }
        }

        // **What went wrong here, first.**
        if (detail.incidents.isNotEmpty()) {
            item { Eyebrow(text = strings["chapter.incidents"], color = colors.alertInk) }
        }
        detail.incidents.forEach { incident ->
            item(key = "i_${incident.id}") {
                ChapterCard(
                    testTag = ChapterTags2.incident(incident.id),
                    onTap = { onOpenIncident(incident) },
                    tone = if (incident.isOpen) BlockTone.Alert else BlockTone.Quiet,
                    eyebrow = incident.reportedEdtf?.takeIf { it.isNotBlank() }
                        ?.let { EventDateText.render(strings, it) },
                    title = Bidi.isolate(incident.title),
                    body = if (incident.isOpen) {
                        strings["readable.state.open"]
                    } else {
                        strings["readable.state.answered"]
                    },
                )
            }
        }

        // **What was worth marking while they were here, per rule 18.** A
        // milestone names its chapter on the arc, and a chapter could not name
        // its milestones, which is a link with one end.
        if (detail.milestones.isNotEmpty()) {
            item { Eyebrow(text = strings["chapter.milestones"]) }
        }
        detail.milestones.forEach { milestone ->
            item(key = "m_${milestone.id}") {
                ChapterCard(
                    testTag = ChapterTags2.milestone(milestone.id),
                    onTap = null,
                    eyebrow = EventDateText.render(strings, milestone.occurredEdtf),
                    title = Bidi.isolate(milestone.label),
                    body = milestone.note?.let { Bidi.isolate(it) },
                )
            }
        }

        if (detail.documents.isNotEmpty()) {
            item { Eyebrow(text = strings["chapter.documents"]) }
        }
        detail.documents.forEach { document ->
            item(key = "d_${document.id}") {
                ChapterCard(
                    testTag = ChapterTags2.document(document.id),
                    onTap = null,
                    eyebrow = document.receivedEdtf?.takeIf { it.isNotBlank() }
                        ?.let { EventDateText.render(strings, it) },
                    title = Bidi.isolate(document.title),
                    body = document.originalLocation?.let { Bidi.isolate(it) },
                )
            }
        }

        item { Eyebrow(text = strings["chapter.entries"]) }

        if (detail.entries.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence about a place with
                    // nothing filed against it yet.
                    Body(text = strings["chapter.entries.empty"])
                }
            }
        }

        // **One item for the whole road**, because a page puts air between its
        // items and air between two stops is a road with gaps in it.
        if (detail.entries.isNotEmpty()) {
            item {
                Column {
                    detail.entries.forEachIndexed { index, entry ->
                        Road(
                            stop = Stop.Done,
                            continuesAbove = index > 0,
                            continuesBelow = index < detail.entries.lastIndex,
                            // The trail reads backward, newest first, so every
                            // half of this road has been traveled.
                            traveledBelow = true,
                        ) {
                            Column(modifier = Modifier.padding(bottom = Space.sm)) {
                                ChapterCard(
                                    testTag = ChapterTags2.entry(entry.id),
                                    onTap = { onOpenEntry(entry) },
                                    eyebrow = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                                        ?.let { EventDateText.render(strings, it) },
                                    title = entry.title?.takeIf { it.isNotBlank() }
                                        ?.let { Bidi.isolate(it) }
                                        ?: strings[kindNameKey(entry.kind)],
                                    body = entry.body?.let { Bidi.isolate(it) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // **Outside any group**, which is a real trap: a chapter with nothing
        // written in it yet is exactly the one somebody has just typed the name
        // of, and the control to fix that name used to exist only once a fold
        // was open. Caught by a test whose chapter had no entries.
        //
        // **This is the defect four audits and six walkthroughs each found on
        // their own**: a chapter is the app's unit of "where", it titles this
        // screen, heads a run in the month review and is printed on documents
        // the app hands to other people, and setup asks for it in one line at
        // two in the morning. #374.
        item {
            Spacer(Modifier.height(Space.s))
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Action(
                    label = strings["chapters.rename"],
                    onClick = onRename,
                    mark = Symbols.edit,
                    modifier = Modifier.testTag(ChapterTags2.RENAME),
                )
                Action(
                    label = strings["remove.action"],
                    onClick = onRemove,
                    modifier = Modifier.testTag(ChapterTags2.REMOVE),
                )
            }
        }
    }
}

/**
 * One thing a chapter holds: when, what, and a few lines of it.
 *
 * Written once rather than four times, because four near identical blocks on one
 * screen is how a screen starts drifting from itself.
 */
@Composable
private fun ChapterCard(
    testTag: String,
    onTap: (() -> Unit)?,
    eyebrow: String?,
    title: String,
    body: String?,
    tone: BlockTone = BlockTone.Quiet,
) {
    val strings = LocalStrings.current
    Block(
        tone = tone,
        modifier = Modifier
            .semantics(mergeDescendants = true) { }
            .then(
                if (onTap == null) {
                    // Not tappable, so it declares no action at all.
                    Modifier
                } else {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = strings["prep.change.open"],
                        onClick = onTap,
                    )
                },
            )
            .testTag(testTag),
    ) {
        // bidi-ok: every caller isolates before handing it here.
        eyebrow?.let { Eyebrow(text = it, fixed = false) }
        Body(
            text = title,
            color = HealthTrail.colors.ink,
            style = HealthTrail.type.rowTitle,
        )
        body?.takeIf { it.isNotBlank() }?.let {
            Body(text = it, maxLines = CHAPTER_PREVIEW_LINES, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** How much of a thing a block shows before its own screen takes over. */
private const val CHAPTER_PREVIEW_LINES = 3
