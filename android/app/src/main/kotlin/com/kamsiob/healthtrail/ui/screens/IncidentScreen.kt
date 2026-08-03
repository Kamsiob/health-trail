package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.removableByLongPress
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object IncidentTags {
    const val LIST_NAME = "incidents"
    const val NAME = "incident"
    fun row(id: String) = "incident_row_$id"
    const val RESOLVE = "incident_resolve"
    const val REOPEN = "incident_reopen"
    const val ADD = "incident_add"
    const val SHARE = "incident_share"
    fun node(id: String) = "incident_node_$id"
    fun person(id: String) = "incident_person_$id"
    fun document(id: String) = "incident_document_$id"
}

/**
 * Every incident, open ones first.
 *
 * **`MASTER_SPEC.md` 4.7 makes an incident a thread rather than an event**, and
 * until now it was an entry with a scary kind: reported once, never followed,
 * never resolved. A fall, a missed medication, or a wound that was not dressed
 * is followed by calls, by somebody promising to look into it, and eventually
 * by an answer, and the thing a caregiver needs six months later is the
 * sequence rather than the first line of it.
 *
 * **Open first, and never a judgment about how long.** An incident nobody has
 * answered is the thing the person is carrying around, and a list that buries
 * it under resolved ones by date has forgotten what it is for. Nothing here
 * says a gap was too long, nothing is colored by age, and no count is scored.
 * Rule 2 and rule 13.
 */
@Composable
fun IncidentsScreen(
    incidents: List<Repository.Incident>,
    onOpen: (Repository.Incident) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val open = incidents.filter { it.isOpen }
    val settled = incidents.filterNot { it.isOpen }

    SectionScaffold(
        name = IncidentTags.LIST_NAME,
        title = strings["incidents.title"],
        subtitle = strings["incidents.subtitle"],
        onBack = onBack,
        modifier = modifier,
    ) {
        if (incidents.isEmpty()) {
            item {
                SectionEmpty(
                    name = IncidentTags.LIST_NAME,
                    text = strings["incidents.empty"],
                    modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION),
                )
            }
            return@SectionScaffold
        }

        if (open.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "incidents.open")
                Spacer(Modifier.height(Space.headerGap))
            }
            open.forEachIndexed { index, incident ->
                item(key = incident.id) {
                    IncidentSpineRow(
                        incident = incident,
                        continuesAbove = index > 0,
                        continuesBelow = index < open.lastIndex || settled.isNotEmpty(),
                        onOpen = { onOpen(incident) },
                    )
                }
            }
        }

        if (settled.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "incidents.settled")
                Spacer(Modifier.height(Space.headerGap))
            }
            settled.forEachIndexed { index, incident ->
                item(key = incident.id) {
                    IncidentSpineRow(
                        incident = incident,
                        continuesAbove = true,
                        continuesBelow = index < settled.lastIndex,
                        onOpen = { onOpen(incident) },
                    )
                }
            }
        }
    }
}

/**
 * One incident on the spine.
 *
 * **Hollow while open, ringed once resolved.** Section 5.2.1: shape carries the
 * state and color carries the kind, so an incident is always `alert` and the
 * drawing says whether anybody has answered it. Both survive grayscale, and the
 * words beside it say the same thing, per section 2.2.
 */
@Composable
private fun IncidentSpineRow(
    incident: Repository.Incident,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
    onOpen: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    SpineRow(
        continuesAbove = continuesAbove,
        continuesBelow = continuesBelow,
        node = colors.alert,
        state = if (incident.isOpen) Waypoint.UPCOMING else Waypoint.MILESTONE,
        routeColor = colors.alert,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { }
                    .clip(Radius.card)
                    .background(surface)
                    .border(2.dp, colors.blue.copy(alpha = ring), Radius.card)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onOpen,
                    )
                    .testTag(IncidentTags.row(incident.id))
                    .padding(Space.cardPadding),
            ) {
                val date = incident.reportedEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) }
                val eyebrow = listOfNotNull(date, incident.chapterName).joinToString("  ")
                if (eyebrow.isNotEmpty()) {
                    Text(eyebrow, style = HealthTrail.type.mono, color = colors.ink3Text)
                    Spacer(Modifier.height(Space.xs))
                }

                Text(
                    text = incident.title.ifBlank { strings["incidents.untitled"] },
                    style = HealthTrail.type.displayS,
                    color = colors.ink,
                )

                Spacer(Modifier.height(Space.xs))
                // **The word beside the color**, per 2.2, so the state never
                // depends on seeing red.
                Text(
                    text = if (incident.isOpen) {
                        strings("incidents.open.count", "count" to incident.entryCount)
                    } else {
                        strings("incidents.settled.count", "count" to incident.entryCount)
                    },
                    style = HealthTrail.type.mono,
                    color = colors.ink3Text,
                )
            }
            Spacer(Modifier.height(Space.cardGap))
        }
    }
}

/**
 * One incident, read as the thread it is.
 *
 * **Oldest first, which is the opposite of the trail.** The trail answers "what
 * has been happening lately" and reads newest first. This answers "how did this
 * go", and a story told backward is not the same story.
 *
 * **The line is continuous rather than dashed**, per 5.2.3, because this is the
 * actual sequence of what happened rather than a filter over entries.
 */
@Composable
fun IncidentScreen(
    incident: Repository.Incident,
    entries: List<Repository.TrailEntry>,
    /**
     * Everybody named on the thread, and the paperwork that came out of it.
     *
     * `MASTER_SPEC.md` section 3: "an incident knows its project, its
     * documents, and its people." **Two of the three needed no new column and
     * no new writer**, only a join nobody had written: every call chasing an
     * incident is an ordinary entry, entries know who they involved, and a
     * document already points at the entry it was saved against.
     *
     * The project is the third and it has nowhere to live. `incident` has no
     * `project_id`, so that clause cannot be built without a schema change,
     * which is the owner's decision under rule 3 and is written up rather than
     * quietly skipped.
     */
    people: List<Repository.Person>,
    documents: List<Repository.Document>,
    onOpenPerson: (Repository.Person) -> Unit,
    onAdd: () -> Unit,
    /**
     * Hands this thread to the system share sheet as a readable document.
     *
     * `MASTER_SPEC.md` 4.7 asks for "its own export" and 4.9 says what that
     * means: generated locally, legible standalone to a reader who has never
     * seen the app.
     */
    onShare: () -> Unit,
    onResolve: () -> Unit,
    onReopen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = IncidentTags.NAME,
        title = incident.title.ifBlank { strings["incidents.untitled"] },
        subtitle = if (incident.isOpen) {
            strings["incident.open.lead"]
        } else {
            strings["incident.settled.lead"]
        },
        onBack = onBack,
        backLabelKey = "incident.back",
        modifier = modifier,
    ) {
        incident.description?.takeIf { it.isNotBlank() }?.let {
            item {
                Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **Who it involved, before the thread.** Somebody opening a six month
        // old incident is usually trying to remember who they dealt with, and
        // the answer was buried in four call titles they would have to read.
        if (people.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "incident.people")
                Spacer(Modifier.height(Space.headerGap))
            }
            people.forEach { person ->
                item(key = "p_${person.id}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) { }
                            .clip(Radius.card)
                            .background(colors.card)
                            .removableByLongPress(
                                label = strings["remove.hint"],
                                onLongPress = {},
                                onTap = { onOpenPerson(person) },
                            )
                            .testTag(IncidentTags.person(person.id))
                            .padding(Space.cardPadding),
                    ) {
                        person.roleLabel?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = HealthTrail.type.mono, color = colors.ink3Text)
                            Spacer(Modifier.height(Space.xs))
                        }
                        Text(
                            person.displayName,
                            style = HealthTrail.type.displayS,
                            color = colors.ink,
                        )
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **The paperwork it produced.** The grievance somebody filed and the
        // letter they were sent are what matters most six months later, and
        // they were reachable only by scrolling the documents section.
        if (documents.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "incident.documents")
                Spacer(Modifier.height(Space.headerGap))
            }
            documents.forEach { document ->
                item(key = "d_${document.id}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) { }
                            .clip(Radius.card)
                            .background(colors.card)
                            .testTag(IncidentTags.document(document.id))
                            .padding(Space.cardPadding),
                    ) {
                        Text(
                            document.title,
                            style = HealthTrail.type.displayS,
                            color = colors.ink,
                        )
                        document.originalLocation?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(Space.xs))
                            Text(it, style = HealthTrail.type.bodyM, color = colors.ink2)
                        }
                    }
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        item {
            GroupHeader(labelKey = "incident.thread")
            Spacer(Modifier.height(Space.headerGap))
        }

        entries.forEachIndexed { index, entry ->
            item(key = entry.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < entries.lastIndex || incident.isOpen,
                    node = colors.alert,
                    // The first report is where it started; everything after is
                    // something that happened on it.
                    state = if (index == 0) Waypoint.MILESTONE else Waypoint.HAPPENED,
                    routeColor = colors.alert,
                    dash = null,
                    modifier = Modifier.testTag(IncidentTags.node(entry.id)),
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .clip(Radius.card)
                                .background(colors.card)
                                .padding(Space.cardPadding),
                        ) {
                            val date = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                                ?.let { EventDateText.render(strings, it) }
                            if (date != null) {
                                Text(date, style = HealthTrail.type.mono, color = colors.ink3Text)
                                Spacer(Modifier.height(Space.xs))
                            }
                            entry.title?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = HealthTrail.type.displayS, color = colors.ink)
                            }
                            entry.body?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(it, style = HealthTrail.type.bodyM, color = colors.ink2)
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }

        // **The open end of the thread.** A hollow waypoint with nothing beside
        // it is what an unanswered incident actually looks like, and it is the
        // one place on this screen the eye should land.
        if (incident.isOpen) {
            item {
                SpineRow(
                    continuesAbove = entries.isNotEmpty(),
                    continuesBelow = false,
                    node = colors.alert,
                    state = Waypoint.UPCOMING,
                    routeColor = colors.alert,
                    dash = null,
                ) {
                    Column {
                        Text(
                            text = strings["incident.still.open"],
                            style = HealthTrail.type.bodyM,
                            color = colors.ink2,
                        )
                        Spacer(Modifier.height(Space.l))
                    }
                }
            }
        }

        incident.resolutionNote?.takeIf { it.isNotBlank() }?.let {
            item {
                Spacer(Modifier.height(Space.s))
                GroupHeader(labelKey = "incident.resolution")
                Spacer(Modifier.height(Space.headerGap))
                Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["incident.add"],
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().testTag(IncidentTags.ADD),
            )
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["readable.share"],
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().testTag(IncidentTags.SHARE),
            )
            Spacer(Modifier.height(Space.cardGap))
            if (incident.isOpen) {
                FilledButton(
                    label = strings["incident.resolve"],
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth().testTag(IncidentTags.RESOLVE),
                )
            } else {
                // **Reopening is offered plainly.** Somebody who resolved the
                // wrong one, or whose answer turned out not to hold, must be
                // able to say so without the app treating it as a correction to
                // be confessed.
                QuietButton(
                    label = strings["incident.reopen"],
                    onClick = onReopen,
                    modifier = Modifier.fillMaxWidth().testTag(IncidentTags.REOPEN),
                )
            }
            Spacer(Modifier.height(Space.l))
        }
    }
}
