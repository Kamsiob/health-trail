package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.FoldRow
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ChapterTags2 {
    const val NAME = "chapter"
    fun entry(id: String) = "chapter_entry_$id"
    fun incident(id: String) = "chapter_incident_$id"
    fun document(id: String) = "chapter_document_$id"
    const val RENAME = "chapter_rename"
    const val MILESTONES_FOLD = "chapter_milestones_fold"
    fun milestone(id: String) = "chapter_milestone_$id"
    const val REMOVE = "chapters_remove"
}

/**
 * One place, and what happened while they were there.
 *
 * `MASTER_SPEC.md` 4.6: inside a chapter, its dates, why the stay began, its
 * incidents, its documents, and any project that began there.
 *
 * **A chapter is this app's unit of "where", and it could not be opened.** The
 * chapters screen drew the journey and every stop on it was a dead end.
 *
 * **The line is continuous**, per 5.2.3, because a chapter is the person's
 * actual path rather than a filter over the record. That is the same reason the
 * chapters list itself draws one, and it is deliberately not the dashed route a
 * care thread or a search result gets.
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
    // What went wrong leads open; the rest are folded until asked for.
    var incidentsOpen by rememberSaveable { mutableStateOf(true) }
    var documentsOpen by rememberSaveable { mutableStateOf(false) }
    var milestonesOpen by rememberSaveable { mutableStateOf(false) }
    var entriesOpen by rememberSaveable { mutableStateOf(false) }
    val colors = HealthTrail.colors
    val chapter = detail.chapter

    SectionScaffold(
        name = ChapterTags2.NAME,
        // **The chip says where you are, the heading says what you came for.**
        // This passed the record's own words as the title, which put them in an
        // 11sp mono chip and again underneath at display weight: the same label
        // in two slots, which section 1 bans. #189 gave the scaffold a heading
        // for exactly this, and every detail screen inherits it.
        title = strings["notebook.section.chapters"],
        heading = Bidi.isolate(chapter.name),
        section = Repository.Section.CHAPTERS,
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
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        chapter.reason?.takeIf { it.isNotBlank() }?.let {
            item {
                // The person's own words about why they moved. #226.
                Text(
                    text = Bidi.isolate(it),
                    style = HealthTrail.type.bodyL,
                    color = colors.ink,
                )
                Spacer(Modifier.height(Space.m))
            }
        }
        chapter.notes?.takeIf { it.isNotBlank() }?.let {
            item {
                Text(
                    text = Bidi.isolate(it),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **Incidents first**, because a stay with something unresolved in it
        // is a stay defined by that, and it is what somebody opens a chapter
        // looking for.
        // **Each group folds, named and counted.** Three headings at one weight
        // over three lists that run for pages is the wall law 1 is about: a
        // person opening a place they lived for a year has to read all of it to
        // find out what is in it. The counts are what let them choose.
        //
        // **What went wrong is open and the rest are folded**, because an
        // incident is the thing somebody comes to a chapter looking for. That is
        // a judgment and it is written down here rather than left implicit.
        if (detail.incidents.isNotEmpty()) {
            item(key = "incidents_fold") {
                FoldRow(
                    labelKey = "chapter.incidents",
                    expanded = incidentsOpen,
                    onToggle = { incidentsOpen = !incidentsOpen },
                    count = detail.incidents.size.toString(),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }
        if (detail.incidents.isNotEmpty() && incidentsOpen) {
            detail.incidents.forEachIndexed { index, incident ->
                item(key = "i_${incident.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < detail.incidents.lastIndex,
                        node = colors.alert,
                        state = if (incident.isOpen) Waypoint.OPEN else Waypoint.MILESTONE,
                        routeColor = colors.alert,
                        dash = null,
                    ) {
                        Column {
                            Card(
                                testTag = ChapterTags2.incident(incident.id),
                                onTap = { onOpenIncident(incident) },
                                eyebrow = incident.reportedEdtf
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { EventDateText.render(strings, it) },
                                title = Bidi.isolate(incident.title),
                                body = if (incident.isOpen) {
                                    strings["readable.state.open"]
                                } else {
                                    strings["readable.state.answered"]
                                },
                            )
                            Spacer(Modifier.height(Space.cardGap))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **What was worth marking while they were here, per rule 18.** A
        // milestone names its chapter on the arc, and a chapter could not name
        // its milestones, which is a link with one end. It leads the folds
        // rather than following them: of everything a chapter holds, this is
        // the part somebody would tell another person about.
        if (detail.milestones.isNotEmpty()) {
            item(key = "milestones_fold") {
                FoldRow(
                    labelKey = "chapter.milestones",
                    expanded = milestonesOpen,
                    onToggle = { milestonesOpen = !milestonesOpen },
                    count = detail.milestones.size.toString(),
                    modifier = Modifier.testTag(ChapterTags2.MILESTONES_FOLD),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }
        if (detail.milestones.isNotEmpty() && milestonesOpen) {
            detail.milestones.forEach { milestone ->
                item(key = "m_${milestone.id}") {
                    Card(
                        testTag = ChapterTags2.milestone(milestone.id),
                        onTap = null,
                        eyebrow = EventDateText.render(strings, milestone.occurredEdtf),
                        title = Bidi.isolate(milestone.label),
                        body = milestone.note?.let { Bidi.isolate(it) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        if (detail.documents.isNotEmpty()) {
            item(key = "documents_fold") {
                FoldRow(
                    labelKey = "chapter.documents",
                    expanded = documentsOpen,
                    onToggle = { documentsOpen = !documentsOpen },
                    count = detail.documents.size.toString(),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
        }
        if (detail.documents.isNotEmpty() && documentsOpen) {
            detail.documents.forEach { document ->
                item(key = "d_${document.id}") {
                    Card(
                        testTag = ChapterTags2.document(document.id),
                        onTap = null,
                        eyebrow = document.receivedEdtf?.takeIf { it.isNotBlank() }
                            ?.let { EventDateText.render(strings, it) },
                        title = Bidi.isolate(document.title),
                        body = document.originalLocation?.let { Bidi.isolate(it) },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        item(key = "entries_fold") {
            FoldRow(
                labelKey = "chapter.entries",
                expanded = entriesOpen,
                onToggle = { entriesOpen = !entriesOpen },
                count = detail.entries.size.toString(),
            )
            Spacer(Modifier.height(Space.cardGap))
        }
        if (entriesOpen) {
            if (detail.entries.isEmpty()) {
                item {
                    Text(
                        text = strings["chapter.entries.empty"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(Space.l))
                }
            } else {
                detail.entries.forEachIndexed { index, entry ->
                    item(key = entry.id) {
                        SpineRow(
                            continuesAbove = index > 0,
                            continuesBelow = index < detail.entries.lastIndex,
                            node = colors.gold,
                            routeColor = colors.gold,
                            dash = RouteDash.TRAIL,
                        ) {
                            Column {
                                Card(
                                    testTag = ChapterTags2.entry(entry.id),
                                    onTap = { onOpenEntry(entry) },
                                    eyebrow = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                                        ?.let { EventDateText.render(strings, it) },
                                    title = entry.title?.takeIf { it.isNotBlank() }
                                        ?.let { Bidi.isolate(it) }
                                        ?: strings[kindNameKey(entry.kind)],
                                    body = entry.body?.let { Bidi.isolate(it) },
                                )
                                Spacer(Modifier.height(Space.cardGap))
                            }
                        }
                    }
                }
            }

        }
        // **Outside the entries fold**, which is where it landed first and is
        // a real trap: a chapter with nothing written in it yet is exactly the
        // one somebody has just typed the name of, and the control to fix that
        // name only existed once the fold was open. Caught by a test whose
        // chapter had no entries.
        //
        // **A pill sized to its label**, D118, above the scaffold's own way
        // back, and the same shape the care thread's rename already uses.
        //
        // **This is the defect four audits and six mock users each found on
        // their own**: a chapter is the app's unit of "where", it titles this
        // screen, heads a run in the month review and is printed on documents
        // the app hands to other people, and setup asks for it in one line at
        // two in the morning. `renameChapter` has been in the repository since
        // the day that was noticed, with no caller, because the shell had no
        // room for another full screen surface. #373 made room. #374.
        item(key = "rename") {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["chapters.rename"],
                onClick = onRename,
                modifier = Modifier.testTag(ChapterTags2.RENAME),
            )
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(ChapterTags2.REMOVE),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * The one card shape this screen uses for all three kinds of thing on it.
 *
 * Written once rather than three times, because three near identical cards on
 * one screen is how a screen starts drifting from itself, which is the same
 * argument `SectionScaffold` exists for.
 */
@Composable
private fun Card(
    testTag: String,
    onTap: (() -> Unit)?,
    eyebrow: String?,
    title: String,
    body: String?,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .clip(Radius.card)
            .then(
                if (onTap == null) {
                    // Not tappable, so it keeps the plain card surface and
                    // declares no action at all.
                    Modifier.background(colors.card)
                } else {
                    // #231: it opens something, so it says "Open this entry"
                    // rather than announcing a removal it does not do.
                    Modifier.openableByTap(
                        label = strings["prep.change.open"],
                        onTap = onTap,
                        resting = colors.card,
                    )
                },
            )
            .testTag(testTag)
            .padding(Space.cardPadding),
    ) {
        eyebrow?.let {
            // bidi-ok: every caller isolates before handing it here.
            Text(text = it, style = HealthTrail.type.mono, color = colors.ink2)
            Spacer(Modifier.height(Space.xs))
        }
        // bidi-ok: every caller isolates before handing it here.
        Text(text = title, style = HealthTrail.type.displayS, color = colors.ink)
        body?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = it,
                style = HealthTrail.type.bodyM,
                color = colors.ink2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
