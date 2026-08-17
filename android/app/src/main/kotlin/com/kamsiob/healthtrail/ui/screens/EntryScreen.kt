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
import androidx.compose.foundation.layout.sizeIn
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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.RouteSwatch
import com.kamsiob.healthtrail.ui.components.WaypointDot
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.Trail
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.Eyebrow

object EntryTags {
    const val NAME = "entry"
    const val DATE = "entry_date"
    const val CORRECT = "entry_correct"
    const val PIN = "entry_pin"
    const val REMOVE = "entry_remove"
    fun thread(id: String) = "entry_thread_$id"
    fun person(id: String) = "entry_person_$id"
    const val CHAPTER = "entry_chapter"
    const val MEDICATION = "entry_medication"
    const val INCIDENT = "entry_incident"
    fun project(id: String) = "entry_project_$id"
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
    /** Opens the long process this entry is about. Rule 18, #283. */
    onOpenProject: (Repository.EntryProject) -> Unit,
    onOpenMedication: () -> Unit,
    onOpenIncident: () -> Unit,
    /**
     * Changes what this entry says. #368.
     *
     * **The words were the one thing on this screen that could not be fixed.**
     * The date has had its own control since rule 17, and everything else the
     * app records has a correction path. A note dictated in a corridor with a
     * drug name heard wrong could only be removed and typed again from memory.
     */
    onCorrect: () -> Unit,
    onRemove: () -> Unit,
    /**
     * Pin this to the top of the trail, or take the pin out.
     *
     * **The pin lives here rather than on every row of the trail.** It was built
     * on the row first, which put a second control on sixteen hundred rows to
     * serve a decision somebody makes a handful of times. Seen on the phone, ten
     * pin buttons down one screen were the loudest thing on it, competing with
     * the words they were sitting beside. Rule 15: uniform weight is not
     * neutral. The trail still shows the mark on a pinned entry, as state.
     */
    onSetPinned: (Boolean) -> Unit,
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
        // **The chip says where you are and the heading says what you came
        // for.** This passed the entry's own words as the title, so the whole
        // sentence appeared in the tab chip in 11sp mono and again underneath at
        // display weight. One entry belongs to the trail, so the chip says the
        // trail, and it wears the trail's own hue like every other screen there.
        title = strings["notebook.section.trail"],
        heading = Bidi.isolate(heading.text),
        section = Repository.Section.TRAIL,
        // **What kind of thing this is, and not when.** The date belongs to the
        // control below that changes it, and putting it here as well was the
        // same value in two slots: the screenshot showed "A call, June 17, 2026"
        // above a row reading "June 17, 2026, Change". The grid draws both; the
        // ban on saying one thing twice is older and wins.
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
                    Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
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
            detail.projects.isNotEmpty() ||
            detail.people.isNotEmpty()

        if (hasLinks) {
            item {
                Eyebrow(text = strings["entry.belongs"])
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
                        label = Bidi.isolate(title),
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
                        label = Bidi.isolate(person.displayName),
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
                        label = Bidi.isolate(thread.label),
                        // **Where this sits in the thread, not the word
                        // "thread" again.** A care thread is a sequence
                        // somebody is following, and the step is what says
                        // three things came before this one. The row above
                        // already showed the thread's name, so repeating the
                        // kind here told the person nothing. The grid's screen
                        // 09 draws it as "This is step 4 of its thread". #348.
                        //
                        // **Falls back to the plain word** when the position is
                        // unknown, which is what an entry linked to a thread it
                        // is somehow not counted in would be, rather than
                        // rendering a step of zero.
                        note = detail.threadPositions[thread.id]?.let {
                            strings(
                                "entry.thread.step",
                                "step" to it.step,
                                "total" to it.total,
                            )
                        } ?: strings["entry.thread"],
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }

            // **The long process this was about**, #283 and rule 18. A project
            // shows every entry connected to it and logging a call from inside
            // one writes that connection; the entry said nothing back, so a
            // call logged from a Medicaid application opened onto a screen with
            // no way to reach the Medicaid application.
            //
            // **Here rather than at the top, which is where the grid draws
            // it.** Screen 10 draws an entry reached from its project, where
            // the project is the thing you came from. This screen is reached
            // from the trail and from search far more often, and the reason
            // people sit above everything else still holds: a call is a call
            // with somebody. So the project sits with "what it is about",
            // beside the medication, and above the chapter, which is where it
            // happened rather than what it was.
            detail.projects.forEach { project ->
                item(key = "project_${project.id}") {
                    LinkRow(
                        testTag = EntryTags.project(project.id),
                        onClick = { onOpenProject(project) },
                        leading = { WaypointDot(color = colors.gold) },
                        label = Bidi.isolate(project.name),
                        // The kind first, then where it stands, so the row says
                        // what it is before it says how it is going.
                        note = Bidi.join(
                            strings["entry.project"],
                            strings["projects.status.${project.status}"],
                        ),
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
                        label = Bidi.isolate(name),
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
                            WaypointDot(color = colors.gold, state = Waypoint.MILESTONE)
                        },
                        label = Bidi.isolate(name),
                        note = strings["entry.chapter"],
                    )
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))
            // **A bordered pill, because it does something now and is not why
            // the screen was opened**, which is law 2's definition of the
            // smaller action. The label says what the tap will do rather than
            // what the state is, so it is an offer rather than a report.
            //
            // **Sized to the label and not to the screen**, D118. Both of these
            // were full width, which is the treatment `SectionScaffold` uses at
            // the foot of every screen to mean the way back, so this screen
            // ended in three identical full width outlined buttons of which the
            // last was the way out and the middle one removed the thing being
            // read. The sheets keep their full width remove: they have no way
            // back to collide with, only Cancel.
            // **First of the three, because it is the one somebody came for.**
            // Pinning and removing are decisions made a handful of times; a
            // typo is found the moment the entry is read.
            Action(
                label = strings["entry.correct"],
                onClick = onCorrect,
                modifier = Modifier.testTag(EntryTags.CORRECT),
            )
            Spacer(Modifier.height(Space.cardGap))
            Action(
                label = strings[if (entry.pinnedAt != null) "trail.unpin" else "trail.pin"],
                onClick = { onSetPinned(entry.pinnedAt == null) },
                modifier = Modifier.testTag(EntryTags.PIN),
            )
            Spacer(Modifier.height(Space.cardGap))
            Action(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(EntryTags.REMOVE),
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
            .clip(Radius.cardLarge)
            .background(surface)
            .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.cardLarge)
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
            // bidi-ok: every caller isolates before handing it here.
            Text(text = label, style = HealthTrail.type.displayS, color = colors.ink)
            Spacer(Modifier.height(Space.xs))
            // bidi-ok: every caller isolates before handing it here.
            Text(text = note, style = HealthTrail.type.bodyS, color = colors.ink2)
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
            .border(Space.focusRing, colors.blue.copy(alpha = ring), Radius.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(testTag)
            // **The 48dp floor, per `DESIGN.md` 12.** Mono is 11sp, so the
            // padding alone left this at about 39dp: the first control on the
            // screen, and the one rule 17's "editable forever" depends on, was
            // the smallest target in the app. #361.
            .sizeIn(minHeight = Space.touchTarget)
            .padding(horizontal = Space.m, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // bidi-ok: every caller isolates before handing it here.
        Text(text = label, style = HealthTrail.type.eyebrow, color = colors.ink)
        Spacer(Modifier.width(Space.s))
        Text(text = strings["entry.date.change"], style = HealthTrail.type.label, color = colors.blue)
    }
}

/** What kind of thing this was, in the person's words rather than the column's. */
private fun kindKey(kind: String): String = "entry.kind.$kind"
