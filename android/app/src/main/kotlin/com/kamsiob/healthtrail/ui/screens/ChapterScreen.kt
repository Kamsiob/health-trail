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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object ChapterTags2 {
    const val NAME = "chapter"
    fun entry(id: String) = "chapter_entry_$id"
    fun incident(id: String) = "chapter_incident_$id"
    fun document(id: String) = "chapter_document_$id"
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
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val chapter = detail.chapter

    SectionScaffold(
        name = ChapterTags2.NAME,
        title = chapter.name,
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
                Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.m))
            }
        }
        chapter.notes?.takeIf { it.isNotBlank() }?.let {
            item {
                Text(text = it, style = HealthTrail.type.bodyM, color = colors.ink2)
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **Incidents first**, because a stay with something unresolved in it
        // is a stay defined by that, and it is what somebody opens a chapter
        // looking for.
        if (detail.incidents.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "chapter.incidents")
                Spacer(Modifier.height(Space.headerGap))
            }
            detail.incidents.forEachIndexed { index, incident ->
                item(key = "i_${incident.id}") {
                    SpineRow(
                        continuesAbove = index > 0,
                        continuesBelow = index < detail.incidents.lastIndex,
                        node = colors.alert,
                        state = if (incident.isOpen) Waypoint.UPCOMING else Waypoint.MILESTONE,
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
                                title = incident.title,
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

        if (detail.documents.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "chapter.documents")
                Spacer(Modifier.height(Space.headerGap))
            }
            detail.documents.forEach { document ->
                item(key = "d_${document.id}") {
                    Card(
                        testTag = ChapterTags2.document(document.id),
                        onTap = null,
                        eyebrow = document.receivedEdtf?.takeIf { it.isNotBlank() }
                            ?.let { EventDateText.render(strings, it) },
                        title = document.title,
                        body = document.originalLocation,
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        item {
            GroupHeader(labelKey = "chapter.entries")
            Spacer(Modifier.height(Space.headerGap))
        }

        if (detail.entries.isEmpty()) {
            item {
                Text(
                    text = strings["chapter.entries.empty"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))
            }
            return@SectionScaffold
        }

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
                                ?: strings[kindNameKey(entry.kind)],
                            body = entry.body,
                        )
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
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
            .background(colors.card)
            .then(
                if (onTap == null) {
                    Modifier
                } else {
                    Modifier.removableByLongPress(
                        label = strings["remove.hint"],
                        onLongPress = {},
                        onTap = onTap,
                    )
                },
            )
            .testTag(testTag)
            .padding(Space.cardPadding),
    ) {
        eyebrow?.let {
            Text(text = it, style = HealthTrail.type.mono, color = colors.ink2)
            Spacer(Modifier.height(Space.xs))
        }
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
