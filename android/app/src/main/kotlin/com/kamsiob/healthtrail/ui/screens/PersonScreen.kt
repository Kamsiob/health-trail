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
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.Avatar
import com.kamsiob.healthtrail.ui.components.AvatarSize
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.DenseRow
import com.kamsiob.healthtrail.ui.components.GroupedSurface
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.RouteDash
import com.kamsiob.healthtrail.ui.components.SpineRow
import com.kamsiob.healthtrail.ui.components.openableByTap
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object PersonTags {
    const val NAME = "person"
    const val CALL = "person_call"
    const val EDIT = "person_edit"
    const val PIN = "person_pin"
    const val ARCHIVE = "person_archive"
    const val CAPTURE = "person_capture"
    fun appointment(id: String) = "person_appointment_$id"
    const val REMOVE = "person_remove"
    fun entry(id: String) = "person_entry_$id"
}

/**
 * One person, and every call and visit that involved them.
 *
 * **`MASTER_SPEC.md` section 3 promises exactly this**, in one line: a person
 * knows every call and visit involving them. **It had no data behind it.**
 * `entry_person` has been in the schema since Phase 0 with nothing writing to
 * it, because capture kept who was spoken to as the entry's title, which is a
 * string. A person's page could not have listed their own calls if it had
 * existed, and it did not exist.
 *
 * Both halves landed together on 2026-08-03: the capture form offers the care
 * team as chips, choosing one writes the link, and this reads it back.
 *
 * **Drawn on the spine, dashed**, per `DESIGN.md` 5.2.3, because this is a
 * filter over the record rather than the person's own path. Same reasoning as
 * a search result and deliberately not the continuous line an incident thread
 * gets.
 *
 * **Their number is one tap away**, which is the whole argument for the care
 * team section: somebody standing in a corridor who needs the charge nurse
 * needs the number, not a form.
 */
@Composable
fun PersonScreen(
    person: Repository.Person,
    entries: List<Repository.TrailEntry>,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    /**
     * Writing something down about them, from the screen that already knows
     * who they are.
     *
     * **This screen had no way to record anything at all.** Somebody who had
     * just come off the phone with the charge nurse, looking at her card with
     * her number on it, had to leave, press the gold button, choose a kind,
     * and find her name again in a picker. **Four taps to say a thing the app
     * was already standing next to**, which is the shape rule 18 names: carry
     * the context forward instead of asking again. #46.
     *
     * **It opens the capture sheet rather than choosing a kind**, because what
     * happened is the person's to say and guessing it would be the app filing
     * for them. The name is carried through; nothing else is.
     */
    onCapture: () -> Unit,
    /**
     * Taking them off the care team, from the one screen that shows who they
     * are. **The list row used to do this on a long press**, which meant a
     * sighted person who did not know the gesture could not do it at all. #218.
     */
    /**
     * Puts them at the top of the care team, or takes them back out. #361.
     *
     * **Here rather than on every row**, which is the same reasoning the trail's
     * pin already carries: a decision somebody makes a handful of times does not
     * earn a second control on fifteen rows, and the list shows the result
     * rather than offering the choice.
     */
    onSetPinned: (Boolean) -> Unit,
    /**
     * Says they are no longer involved, without erasing them. #371.
     *
     * **The only thing this screen offered was Remove**, which tombstones and
     * takes them off every entry they were named on. Somebody who moved
     * facilities had eleven people to retire and the one available action
     * destroyed six months of their own record.
     */
    onSetArchived: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onOpenEntry: (Repository.TrailEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Every appointment this person is on, soonest first.
     *
     * **The other end of "who it is with", per rule 18**, and it could not be
     * asked until `appointment.person_id` gained a writer on 2026-08-13: an
     * appointment knew nobody, so this screen had nothing to say back. "When am
     * I next seeing the charge nurse" is a question this notebook holds the
     * answer to. #371.
     */
    appointments: List<Repository.Appointment> = emptyList(),
    onOpenAppointment: (Repository.Appointment) -> Unit = {},

    backLabelKey: String = "section.back",
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    SectionScaffold(
        onEdit = onEdit,
        editTag = PersonTags.EDIT,
        editLabel = strings["person.edit"],
        name = PersonTags.NAME,
        // **The chip says where you are, the heading says what you came for.**
        // This passed the record's own words as the title, which put them in an
        // 11sp mono chip and again underneath at display weight: the same label
        // in two slots, which section 1 bans. #189 gave the scaffold a heading
        // for exactly this, and every detail screen inherits it.
        title = strings["notebook.section.care_team"],
        heading = Bidi.isolate(person.displayName.ifBlank { strings["person.unnamed"] }),
        section = Repository.Section.CARE_TEAM,
        subtitle = person.roleLabel?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
            ?: strings["person.norole"],
        onBack = onBack,
        backLabelKey = backLabelKey,
        modifier = modifier,
    ) {
        // **Identity first, at 56dp.** A person's screen opens with the person,
        // and initials in their section's own wash are what makes a list of
        // nine names a list of nine people. It sits under the name rather than
        // beside it, because the scaffold's heading is where the name belongs
        // and putting a second copy of it next to the mark would be the same
        // words twice again.
        item {
            Avatar(
                name = person.displayName,
                hue = hueFor(Repository.Section.CARE_TEAM),
                size = AvatarSize.header,
            )
            Spacer(Modifier.height(Space.m))
        }

        // **What you need to know before you dial, above the thing that
        // dials.** "Days, 7 to 3, ask for her by name" and "the one who
        // actually calls back" are the reason somebody opened this screen at
        // all, and they were sitting underneath both buttons where the eye
        // reaches them after the decision has been made.
        person.notes?.takeIf { it.isNotBlank() }?.let {
            item {
                Text(text = Bidi.isolate(it), style = HealthTrail.type.bodyL, color = colors.ink)
                Spacer(Modifier.height(Space.l))
            }
        }

        item {
            // **The number carries the weight, because it is the whole point.**
            // Somebody standing in a corridor who needs the charge nurse needs
            // the number, not a form, and this screen gave the two the same
            // treatment: one quiet button above another quiet button, which
            // makes the reader sort them. Rule 15. Correcting a spelling is the
            // rare errand and stays quiet.
            person.phone?.takeIf { it.isNotBlank() }?.let { number ->
                FilledButton(
                    label = strings("person.call", "number" to number),
                    onClick = { onCall(number) },
                    modifier = Modifier.fillMaxWidth().testTag(PersonTags.CALL),
                )
                Spacer(Modifier.height(Space.cardGap))
            }
            // **Sized to its label rather than to the screen**, D118. Full
            // width outlined is what `SectionScaffold` uses at the foot of
            // every screen to mean the way back, and an in-content action
            // wearing it competes with the one control that leaves.
            // **Recording comes before correcting**, because it is the thing
            // somebody standing in a corridor actually came to do, and rule 15
            // gives the more common errand the better position. Both are quiet
            // pills sized to their labels: the number above them is the one
            // thing on this screen that carries weight.
            QuietButton(
                label = strings["person.capture"],
                onClick = onCapture,
                modifier = Modifier.testTag(PersonTags.CAPTURE),
            )
            Spacer(Modifier.height(Space.cardGap))
            Spacer(Modifier.height(Space.sectionGap))
        }

        // **When you are seeing them, above what was said**, because it is
        // ahead rather than behind: somebody opening a person's screen the day
        // before a review is looking for the review.
        if (appointments.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "person.appointments")
                Spacer(Modifier.height(Space.headerGap))
            }
            item(key = "appointments") {
                GroupedSurface {
                    appointments.forEachIndexed { index, appointment ->
                        DenseRow(
                            title = Bidi.isolate(appointment.title),
                            subtitle = appointment.scheduledEdtf
                                ?.takeIf { it.isNotBlank() }
                                ?.let { EventDateText.render(strings, it) },
                            chevron = true,
                            divider = index < appointments.lastIndex,
                            onClick = { onOpenAppointment(appointment) },
                            clickLabel = strings["open.action"],
                            modifier = Modifier.testTag(PersonTags.appointment(appointment.id)),
                        )
                    }
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        item {
            GroupHeader(labelKey = "person.entries")
            Spacer(Modifier.height(Space.headerGap))
        }

        // **Nothing yet is the ordinary state and says so plainly.** A person
        // added before they were spoken to has no entries, which is not a gap
        // in the record, and rule 13 forbids reading an unfilled thing as a
        // deficiency.
        if (entries.isEmpty()) {
            item {
                Text(
                    text = strings("person.entries.empty", "name" to person.displayName),
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))
            }
        }

        entries.forEachIndexed { index, entry ->
            item(key = entry.id) {
                SpineRow(
                    continuesAbove = index > 0,
                    continuesBelow = index < entries.lastIndex,
                    node = colors.blue,
                    routeColor = colors.blue,
                    dash = RouteDash.TRAIL,
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .clip(Radius.card)
                                // **`openableByTap`, not the removal
                                // modifier with its gesture switched off.**
                                // That declared a tap action called "remove" on
                                // a card that opens an entry, and a long press
                                // called "remove" that ran an empty function.
                                // The gesture went quiet and the words did not.
                                // #231.
                                .openableByTap(
                                    label = strings["prep.change.open"],
                                    onTap = { onOpenEntry(entry) },
                                    resting = colors.card,
                                )
                                .testTag(PersonTags.entry(entry.id))
                                .padding(Space.cardPadding),
                        ) {
                            entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = EventDateText.render(strings, it),
                                    style = HealthTrail.type.bodyS,
                                    color = colors.ink2,
                                )
                                Spacer(Modifier.height(Space.xs))
                            }
                            Text(
                                text = entry.title?.takeIf { it.isNotBlank() }
                                    ?.let { Bidi.isolate(it) }
                                    ?: strings[kindNameKey(entry.kind)],
                                style = HealthTrail.type.displayS,
                                color = colors.ink,
                            )
                            entry.body?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = Bidi.isolate(it),
                                    style = HealthTrail.type.bodyM,
                                    color = colors.ink2,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }
        }

        // **Taking them off the care team, from the screen that shows who they
        // are**, per #218. It sits last because it is the rarest thing anybody
        // comes here to do, and it opens the confirmation rather than doing
        // anything itself, so nothing destructive rests on the screen.
        item {
            Spacer(Modifier.height(Space.sectionGap))
            QuietButton(
                label = strings[
                    if (person.pinnedAt != null) "careteam.unpin" else "careteam.pin",
                ],
                onClick = { onSetPinned(person.pinnedAt == null) },
                modifier = Modifier.testTag(PersonTags.PIN),
            )
            Spacer(Modifier.height(Space.cardGap))
            // **Above Remove, and the note says the difference.** Somebody
            // looking for a way to say "she left" will otherwise take the only
            // control there is, which is the one that erases the record.
            QuietButton(
                label = strings[
                    if (person.archivedAt != null) "careteam.unarchive" else "careteam.archive",
                ],
                onClick = { onSetArchived(person.archivedAt == null) },
                modifier = Modifier.testTag(PersonTags.ARCHIVE),
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = strings["careteam.archive.note"],
                style = HealthTrail.type.bodyS,
                color = colors.ink2,
            )
            Spacer(Modifier.height(Space.cardGap))
            QuietButton(
                label = strings["remove.action"],
                onClick = onRemove,
                modifier = Modifier.testTag(PersonTags.REMOVE),
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}
