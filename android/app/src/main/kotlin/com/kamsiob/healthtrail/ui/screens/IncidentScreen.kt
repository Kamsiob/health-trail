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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.Waypoint
import com.kamsiob.healthtrail.ui.components.focusRingAlpha
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.components.pressedSurface
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.RowDivider

object IncidentTags {
    const val LIST_NAME = "incidents"
    const val NAME = "incident"
    fun row(id: String) = "incident_row_$id"
    const val RESOLVE = "incident_resolve"
    const val REOPEN = "incident_reopen"
    const val ADD = "incident_add"
    const val SHARE = "incident_share"
    const val REMOVE = "incident_remove"
    fun violation(id: String) = "incident_violation_$id"
    const val CORRECT = "incident_correct"
    fun node(id: String) = "incident_node_$id"
    /**
     * The card, which is the control. [node] tags the spine row around it, and
     * a merged card inside a tagged wrapper means the wrapper's node carries no
     * click at all: the test that asserted the label failed for that reason and
     * the one that asserted the behavior passed, which is a good demonstration
     * of why both are worth writing.
     */
    fun entry(id: String) = "incident_entry_$id"
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
        // **The chip says where you are and the heading says what you came
        // for**, and this screen said "Incidents" in both slots. The heading
        // names the person's own act rather than the app's judgment about a
        // facility, which is the same choice "What you have asked for" makes
        // on the standing instructions. #341.
        title = strings["incidents.title"],
        headingKey = "incidents.heading",
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
                Eyebrow(text = strings["incidents.open"])
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
                Eyebrow(text = strings["incidents.settled"])
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
        state = if (incident.isOpen) Waypoint.OPEN else Waypoint.MILESTONE,
        routeColor = colors.alert,
    ) {
        Column {
            Column(
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
                        onClick = onOpen,
                    )
                    .testTag(IncidentTags.row(incident.id))
                    .padding(Space.cardPadding),
            ) {
                val date = incident.reportedEdtf?.takeIf { it.isNotBlank() }
                    ?.let { EventDateText.render(strings, it) }
                // Isolated and joined, because a date in one script beside a
                // chapter name in another is two runs and reorders without it.
                val eyebrow = Bidi.join(listOfNotNull(date, incident.chapterName))
                if (eyebrow.isNotEmpty()) {
                    Text(eyebrow, style = HealthTrail.type.bodyS, color = colors.ink2)
                    Spacer(Modifier.height(Space.xs))
                }

                Text(
                    text = Bidi.isolate(incident.title.ifBlank { strings["incidents.untitled"] }),
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
                    style = HealthTrail.type.bodyS,
                    color = colors.ink2,
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
    /**
     * The thread, everybody named on it, the paperwork it produced, and every
     * request that was not followed here.
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
     *
     * **Four lists behind one object rather than four parameters**, and that is
     * B6 rather than taste: every parameter here is a line inside
     * `NotebookShell`, which is one composable at the JVM's 64KB method limit,
     * and adding the violations as a fifth list failed the build.
     */
    detail: Repository.IncidentDetail,
    onOpenPerson: (Repository.Person) -> Unit,
    /**
     * Opens the paperwork this incident produced. #360, rule 18.
     *
     * **The people above these rows opened and the entries below them opened,
     * and the documents between them did not**, so the grievance letter had to
     * be found again by scrolling the documents section: five taps for the
     * thing the incident is holding out.
     */
    /**
     * Every time a request was not followed that names this incident.
     *
     * **The other half of the link, per rule 18.** A violation could name an
     * incident from the moment the form began to ask, and the incident said
     * nothing back, which four of the five panels called a dead end wearing a
     * disguise.
     */
    onOpenDocument: (Repository.Document) -> Unit,
    /**
     * Opens one of the entries on the thread.
     *
     * **The other end of a link that only went one way**, #46 and rule 18. An
     * entry opens the incident it belongs to, and the incident could show its
     * entries and not open them, so somebody reading the thread and wanting the
     * whole of one had to leave and find it in the trail. Every other screen
     * that draws entry cards, a person's, a thread's, a chapter's and a prep
     * sheet's, already opens them.
     */
    onOpenEntry: (Repository.TrailEntry) -> Unit,
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
    /**
     * Takes the incident off the record, as a tombstone. #358, D135.
     *
     * **Removal is reached by looking, from the thing's own screen**, which is
     * where the other six put it. An incident is typed in a hurry and somebody
     * who wrote the same one twice had no way to say so: the list offered
     * nothing and this screen offered nothing.
     *
     * **What happened next survives it.** Those are entries and they are the
     * record of what happened, which is the same call a thread's removal makes
     * about its links.
     */
    onRemove: () -> Unit,
    /**
     * Corrects the words, the date and the note. #358.
     *
     * **Above removal and below the rest**, per rule 15's ordering on the
     * person's screen: correcting is rarer than adding what happened next and
     * commoner than taking the whole thing off the record.
     */
    onCorrect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Opens the list of what this family has asked for.
     *
     * **A request has no screen of its own**: the one `DESIGN.md` 14 draws
     * cannot be built while `NotebookShell` is at the JVM method limit, B6, so
     * the door leads to the list that holds it rather than nowhere.
     */
    onOpenViolations: () -> Unit = {},
) {
    val entries = detail.entries
    val people = detail.people
    val documents = detail.documents
    val violations = detail.violations
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        name = IncidentTags.NAME,
        // **The chip says where you are, the heading says what you came for.**
        // This passed the record's own words as the title, which put them in an
        // 11sp mono chip and again underneath at display weight: the same label
        // in two slots, which section 1 bans. #189 gave the scaffold a heading
        // for exactly this, and every detail screen inherits it.
        title = strings["incidents.title"],
        heading = Bidi.isolate(incident.title.ifBlank { strings["incidents.untitled"] }),
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
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **Who it involved, before the thread.** Somebody opening a six month
        // old incident is usually trying to remember who they dealt with, and
        // the answer was buried in four call titles they would have to read.
        if (people.isNotEmpty()) {
            item {
                Eyebrow(text = strings["incident.people"])
                Spacer(Modifier.height(Space.headerGap))
            }
            // **Dense rows in one surface, and they were three cards.** Rule
            // 22: a card is for something with three or more lines somebody
            // reads, and a name with a role is two lines they scan. Three cards
            // for three people took half the screen above the thing the screen
            // is actually about, which is what happened and what happened next.
            //
            // The same shape the care team uses, with the same initials, so a
            // person looks the same wherever they appear.
            item(key = "people") {
                Block(padding = Space.none) {
                    people.forEachIndexed { index, person ->
                        ListRow(
                            title = Bidi.isolate(person.displayName),
                            support = person.roleLabel?.takeIf { it.isNotBlank() }
                                ?.let { Bidi.isolate(it) },
                            leading = {
                                Avatar(
                                    name = person.displayName,
                                    hue = hueFor(Repository.Section.CARE_TEAM),
                                )
                            },
                            isDoor = true,
                            onClick = { onOpenPerson(person) },
                            modifier = Modifier.testTag(IncidentTags.person(person.id)),
                        )
                        if (index < people.size - 1) RowDivider()
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **The paperwork it produced.** The grievance somebody filed and the
        // letter they were sent are what matters most six months later, and
        // they were reachable only by scrolling the documents section.
        if (documents.isNotEmpty()) {
            item {
                Eyebrow(text = strings["incident.documents"])
                Spacer(Modifier.height(Space.headerGap))
            }
            documents.forEach { document ->
                item(key = "d_${document.id}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) { }
                            .clip(Radius.cardLarge)
                            // A tap opens the paper itself, the same as the
                            // people above and the entries below. #360.
                            .openableByTap(
                                label = strings["open.action"],
                                onTap = { onOpenDocument(document) },
                                resting = colors.card,
                            )
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

        if (violations.isNotEmpty()) {
            item {
                Eyebrow(text = strings["instruction.violations.linked"])
                Spacer(Modifier.height(Space.headerGap))
            }
            // One surface of dense rows, the same shape the people above use,
            // because a request and a note is two lines somebody scans rather
            // than three they read. Rule 22.
            item(key = "violations") {
                Block(padding = Space.none) {
                    violations.forEachIndexed { index, violation ->
                        ListRow(
                            title = Bidi.isolate(violation.instructionName.orEmpty()),
                            support = violation.note?.takeIf { it.isNotBlank() }
                                ?.let { Bidi.isolate(it) },
                            // **The way back is the list of what was asked
                            // for**, because a request has no screen of its
                            // own: the one `DESIGN.md` 14 draws cannot be built
                            // while `NotebookShell` sits at the JVM method
                            // limit, B6.
                            onClick = onOpenViolations,
                            clickLabel = strings["open.action"],
                            isDoor = true,
                            modifier = Modifier.testTag(IncidentTags.violation(violation.id)),
                        )
                        if (index < violations.size - 1) RowDivider(inset = false)
                    }
                }
                Spacer(Modifier.height(Space.cardGap))
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        item {
            Eyebrow(text = strings["incident.thread"])
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
                                .clip(Radius.cardLarge)
                                // **`openableByTap`, which paints the surface
                                // as well as taking the tap**, so the card
                                // answers a finger the way every other entry
                                // card in the app does. 5.14 and #46.
                                .openableByTap(
                                    label = strings["prep.change.open"],
                                    onTap = { onOpenEntry(entry) },
                                    resting = colors.card,
                                )
                                .testTag(IncidentTags.entry(entry.id))
                                .padding(Space.cardPadding),
                        ) {
                            val date = entry.occurredEdtf?.takeIf { it.isNotBlank() }
                                ?.let { EventDateText.render(strings, it) }
                            if (date != null) {
                                Text(date, style = HealthTrail.type.bodyS, color = colors.ink2)
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
                    state = Waypoint.OPEN,
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
                Eyebrow(text = strings["incident.resolution"])
                Spacer(Modifier.height(Space.headerGap))
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
            }
        }

        item {
            Spacer(Modifier.height(Space.sectionGap))

            // **Adding what happened next is the filled action, and marking it
            // answered was.** Somebody opens an open incident because something
            // else has happened and they want it written down; marking it
            // answered is what they do once, at the end, and it was carrying
            // the screen's only accent for every visit before that one.
            //
            // Law 2 allows one filled action and it belongs on the reason the
            // screen gets opened. Answering and sharing are doors, not
            // competition, which is what the issue says in those words.
            if (incident.isOpen) {
                FilledButton(
                    label = strings["incident.add"],
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().testTag(IncidentTags.ADD),
                )
            } else {
                // **Once it is answered, adding is no longer the point.** The
                // record stays open to additions, because an answer can turn
                // out not to hold, but it stops being what the screen is for.
                QuietButton(
                    label = strings["incident.add"],
                    onClick = onAdd,
                    modifier = Modifier.testTag(IncidentTags.ADD),
                )
            }
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["readable.share"],
                onClick = onShare,
                modifier = Modifier.testTag(IncidentTags.SHARE),
            )
            Spacer(Modifier.height(Space.cardGap))
            if (incident.isOpen) {
                QuietButton(
                    label = strings["incident.resolve"],
                    onClick = onResolve,
                    modifier = Modifier.testTag(IncidentTags.RESOLVE),
                )
            } else {
                // **Reopening is offered plainly.** Somebody who resolved the
                // wrong one, or whose answer turned out not to hold, must be
                // able to say so without the app treating it as a correction to
                // be confessed.
                QuietButton(
                    label = strings["incident.reopen"],
                    onClick = onReopen,
                    modifier = Modifier.testTag(IncidentTags.REOPEN),
                )
            }
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["incident.correct"],
                onClick = onCorrect,
                modifier = Modifier.testTag(IncidentTags.CORRECT),
            )
            // **Last, and set apart**, per D135 and the person's screen: it is
            // the rarest thing anybody comes here to do, and it opens the
            // confirmation rather than doing anything itself.
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(IncidentTags.REMOVE),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
