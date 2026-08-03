package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RouteSwatch
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.Trail

object EntryTags {
    const val NAME = "entry"
    const val DATE = "entry_date"
    const val REMOVE = "entry_remove"
    fun thread(id: String) = "entry_thread_$id"
    fun person(id: String) = "entry_person_$id"
    const val CHAPTER = "entry_chapter"
    const val MEDICATION = "entry_medication"
    const val INCIDENT = "entry_incident"
}

/**
 * One thing that happened, read on its own.
 *
 * **This did not exist, and its absence was a dead end at the end of every
 * other screen.** A trail row's only tappable part was its date. A search
 * result opened the section it lived in and left the person to find the row
 * again. There was no way to get from a thing that happened to the thread it
 * belonged to, the chapter it happened in, or the incident it was part of.
 *
 * `MASTER_SPEC.md` 4.2, rule 18, and #46: **links go both ways, and a one way
 * link is a dead end wearing a disguise.** An entry knows its threads, its
 * chapter, and its incident, and each of them opens.
 *
 * **The route is how a thread is named here**, per `DESIGN.md` 5.2.2: the
 * thread's color and its dash pattern together, which is what makes it the same
 * thread the person saw on the care threads screen and on the trail.
 *
 * **The date stays editable**, per rule 17. A date captured in a hallway is the
 * one most likely to be wrong, and this is now the natural place to fix it.
 */
@Composable
fun EntryScreen(
    detail: Repository.EntryDetail,
    onEditDate: () -> Unit,
    onOpenThread: (Repository.CareThread) -> Unit,
    onOpenPerson: (Repository.Person) -> Unit,
    onOpenChapter: () -> Unit,
    onOpenMedication: () -> Unit,
    onOpenIncident: () -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabelKey: String = "section.back",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val entry = detail.entry
    val heading = headingFor(entry, strings["entry.untitled"])

    SectionScaffold(
        name = EntryTags.NAME,
        title = heading.text,
        subtitle = strings[kindKey(entry.kind)],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        item {
            // **The date first, and it is a control.** Rule 17 makes every date
            // editable forever from the entry itself, and this is the entry.
            EditableRow(
                label = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) }
                    ?: strings["date.unknown"],
                testTag = EntryTags.DATE,
                onClick = onEditDate,
            )
            Spacer(Modifier.height(Space.l))
        }

        // **Not repeated when the heading is already the whole of it.** Saying
        // the same sentence twice, once large and once small, reads as a
        // rendering fault rather than emphasis.
        if (heading.repeatBody) {
            entry.body?.takeIf { it.isNotBlank() }?.let {
                item {
                    Text(text = it, style = HealthTrail.type.bodyL, color = colors.ink)
                    Spacer(Modifier.height(Space.sectionGap))
                }
            }
        } else {
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **Where this sits in the record**, which is the half that was missing.
        val hasLinks = detail.chapterName != null ||
            detail.incidentTitle != null ||
            detail.medicationName != null ||
            entry.threads.isNotEmpty() ||
            detail.people.isNotEmpty()

        if (hasLinks) {
            item {
                GroupHeader(labelKey = "entry.belongs")
                Spacer(Modifier.height(Space.headerGap))
            }

            detail.incidentTitle?.let { title ->
                item {
                    LinkRow(
                        testTag = EntryTags.INCIDENT,
                        onClick = onOpenIncident,
                        leading = {
                            WaypointDot(
                                color = colors.alert,
                                state = if (detail.incidentIsOpen) {
                                    Waypoint.UPCOMING
                                } else {
                                    Waypoint.MILESTONE
                                },
                            )
                        },
                        label = title,
                        note = if (detail.incidentIsOpen) {
                            strings["readable.state.open"]
                        } else {
                            strings["readable.state.answered"]
                        },
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            // **Who it involved, first**, because a call is a call with
            // somebody and that is the first thing a person looks for when
            // they open one months later.
            detail.people.forEach { person ->
                item(key = "person_${person.id}") {
                    LinkRow(
                        testTag = EntryTags.person(person.id),
                        onClick = { onOpenPerson(person) },
                        leading = { WaypointDot(color = colors.blue) },
                        label = person.displayName,
                        note = person.roleLabel?.takeIf { it.isNotBlank() }
                            ?: strings["entry.person"],
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            entry.threads.forEach { thread ->
                item(key = "thread_${thread.id}") {
                    val route = colors.threadRoutes[thread.colorIndex % colors.threadRoutes.size]
                    LinkRow(
                        testTag = EntryTags.thread(thread.id),
                        onClick = { onOpenThread(thread) },
                        leading = { RouteSwatch(color = route, index = thread.colorIndex) },
                        label = thread.label,
                        note = strings["entry.thread"],
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            // **What it is about**, above the chapter, because a question about
            // a dose is about the dose first and the building second.
            detail.medicationName?.let { name ->
                item {
                    LinkRow(
                        testTag = EntryTags.MEDICATION,
                        onClick = onOpenMedication,
                        leading = { WaypointDot(color = colors.blue) },
                        label = name,
                        note = strings["entry.medication"],
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            detail.chapterName?.let { name ->
                item {
                    LinkRow(
                        testTag = EntryTags.CHAPTER,
                        onClick = onOpenChapter,
                        leading = {
                            WaypointDot(color = colors.blaze, state = Waypoint.MILESTONE)
                        },
                        label = name,
                        note = strings["entry.chapter"],
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["entry.remove"],
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag(EntryTags.REMOVE),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * What the heading says, and whether the body still needs saying underneath.
 *
 * @property text what goes at the top, at display weight.
 * @property repeatBody whether the body is still shown below in full.
 */
data class EntryHeading(val text: String, val repeatBody: Boolean)

/** Longest heading promoted from a body before it is cut at a word boundary. */
private const val HEADING_CAP = 90

/**
 * The heading for an entry that may never have been given a title.
 *
 * **Most entries have no title.** Capture asks for what happened, not for a
 * name, which is right: somebody standing in a corridor types the sentence and
 * leaves. So the untitled case is the ordinary case, and it was being rendered
 * as "Something you wrote down" at the largest size on the screen while the
 * sentence the person actually wrote sat below it in body text.
 *
 * That is rule 15 backwards. The biggest thing on the screen carried the least
 * information, and every untitled entry looked like every other untitled entry
 * from across the room. Found on the phone in light, not in the code.
 *
 * So the sentence becomes the heading. A long one is cut at a word boundary for
 * the heading only and then shown again in full below, which is not the
 * truncation rule 11 bans: nothing is lost, and the full text is the next thing
 * the eye reaches. The stock phrase survives for an entry with no words at all,
 * a photograph or a recording, which is the case it was actually written for.
 */
fun headingFor(entry: Repository.TrailEntry, untitled: String): EntryHeading =
    headingFor(entry.title, entry.body, untitled)

/**
 * The same rule, given the two fields rather than a trail entry.
 *
 * **The Unfiled tray needs it and holds a different type**, and copying the
 * rule there would be a pattern appearing twice in two forms, which section
 * 10.2 calls a defect outright. The tray is also the likeliest place in the app
 * to hold an untitled entry, since somebody who could not say where something
 * belonged often did not stop to title it either.
 */
fun headingFor(title: String?, body: String?, untitled: String): EntryHeading {
    title?.takeIf { it.isNotBlank() }?.let { return EntryHeading(it.trim(), true) }

    val body = body?.trim()?.takeIf { it.isNotBlank() }
        ?: return EntryHeading(untitled, false)

    val firstLine = body.lineSequence().first().trim()
    val isWholeBody = firstLine.length == body.length
    if (firstLine.length <= HEADING_CAP) {
        return EntryHeading(firstLine, repeatBody = !isWholeBody)
    }

    val cut = firstLine.take(HEADING_CAP).substringBeforeLast(' ', "").trim()
    val shortened = cut.ifBlank { firstLine.take(HEADING_CAP).trim() }
    return EntryHeading(shortened + "…", repeatBody = true)
}

/** A row that opens something else. One stop for the reader, per D68. */
@Composable
private fun LinkRow(
    testTag: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    label: String,
    note: String,
) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.card)
    val ring by focusRingAlpha(interaction)

    Row(
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
                onClick = onClick,
            )
            .testTag(testTag)
            .padding(Space.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) { leading() }
        Spacer(Modifier.width(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = HealthTrail.type.displayS, color = colors.ink)
            Spacer(Modifier.height(Space.xs))
            Text(text = note, style = HealthTrail.type.mono, color = colors.ink3Text)
        }
    }
}

/** The date, which is a control rather than a label. Rule 17. */
@Composable
private fun EditableRow(label: String, testTag: String, onClick: () -> Unit) {
    val colors = HealthTrail.colors
    val interaction = remember { MutableInteractionSource() }
    val surface by pressedSurface(interaction, colors.sand)
    val ring by focusRingAlpha(interaction)
    val strings = LocalStrings.current

    Row(
        modifier = Modifier
            .semantics(mergeDescendants = true) { }
            .clip(Radius.pill)
            .background(surface)
            .border(2.dp, colors.blue.copy(alpha = ring), Radius.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(testTag)
            .padding(horizontal = Space.m, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = HealthTrail.type.mono, color = colors.ink)
        Spacer(Modifier.width(Space.s))
        Text(text = strings["entry.date.change"], style = HealthTrail.type.mono, color = colors.blue)
    }
}

/** What kind of thing this was, in the person's words rather than the column's. */
private fun kindKey(kind: String): String = "entry.kind.$kind"
