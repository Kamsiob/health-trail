package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.IconAction
import com.kamsiob.healthtrail.ui.v4.ListRow
import com.kamsiob.healthtrail.ui.v4.Page
import com.kamsiob.healthtrail.ui.v4.labeledBlock

object PersonTags {
    const val NAME = "person"
    const val CALL = "person_call"
    const val EMAIL = "person_email"
    const val EDIT = "person_edit"
    const val PIN = "person_pin"
    const val ARCHIVE = "person_archive"
    const val CAPTURE = "person_capture"
    fun appointment(id: String) = "person_appointment_$id"
    const val REMOVE = "person_remove"
    fun entry(id: String) = "person_entry_$id"

    /** The tag the old scaffold produced, kept so a journey still finds this screen. */
    const val ROOT = "section_root_person"
}

/**
 * One person, and every call and visit that involved them. Rewritten onto
 * `ui/v4`, #386.
 *
 * **`MASTER_SPEC.md` section 3 promises exactly this**, in one line: a person
 * knows every call and visit involving them. Both halves landed on 2026-08-03,
 * when the capture form began writing `entry_person` and this screen began
 * reading it back.
 *
 * **This is the other side of the care team**, so it is built from the same
 * pieces: the section's wash behind the person, the mark in their own hue, and
 * calling as the one filled action. Somebody standing in a corridor who needs
 * the charge nurse needs the number, not a form.
 *
 * **The spine is gone from here and that is deliberate.** It was drawn dashed
 * to say "a filter over the record rather than this person's own path", and a
 * dashed line down the side of a list is a costume carrying one word of
 * meaning. The eyebrow says it in words instead, and the spine stays where it
 * means the path itself: the trail, and a project's road. `DESIGN.md` 5.2,
 * rule 22.
 *
 * **Nothing yet is the ordinary state and says so plainly.** A person added
 * before they were spoken to has no entries, which is not a gap in the record,
 * and rule 13 forbids reading an unfilled thing as a deficiency.
 */
@Composable
fun PersonScreen(
    person: Repository.Person,
    entries: List<Repository.TrailEntry>,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    /**
     * Writing something down about them, from the screen that already knows who
     * they are.
     *
     * **This screen had no way to record anything at all.** Somebody who had
     * just come off the phone with the charge nurse, looking at her card with
     * her number on it, had to leave, press the gold button, choose a kind, and
     * find her name again in a picker. **Four taps to say a thing the app was
     * already standing next to**, which is the shape rule 18 names. #46.
     */
    onCapture: () -> Unit,
    /**
     * Puts them at the top of the care team, or takes them back out. #361.
     *
     * **Here rather than on every row**: a decision somebody makes a handful of
     * times does not earn a second control on fifteen rows, and the list shows
     * the result rather than offering the choice.
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
     * asked until `appointment.person_id` gained a writer: an appointment knew
     * nobody, so this screen had nothing to say back. #371.
     */
    appointments: List<Repository.Appointment> = emptyList(),
    onOpenAppointment: (Repository.Appointment) -> Unit = {},
    /** Writing to them, where they gave an address. Null leaves the action off. */
    onEmail: ((String) -> Unit)? = null,
    backLabelKey: String = LocalSectionBackKey.current,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val hue = hueFor(Repository.Section.CARE_TEAM)
    val name = person.displayName.ifBlank { strings["person.unnamed"] }
    val phone = person.phone?.takeIf { it.isNotBlank() }
    val address = person.email?.takeIf { it.isNotBlank() }

    Page(
        eyebrow = strings["notebook.section.care_team"],
        eyebrowColor = hue.ink,
        title = Bidi.isolate(name),
        subtitle = person.roleLabel?.takeIf { it.isNotBlank() }?.let { Bidi.isolate(it) }
            ?: strings["person.norole"],
        onBack = onBack,
        backLabel = strings[backLabelKey],
        actions = {
            IconAction(
                symbol = Symbols.edit,
                label = strings["person.edit"],
                onClick = onEdit,
                modifier = Modifier.testTag(PersonTags.EDIT),
            )
        },
        modifier = modifier.testTag(PersonTags.ROOT),
    ) {
        item {
            Block(tone = BlockTone.Section, hue = hue) {
                // **The mark and the actions share one row**, because most
                // people on a care team are a name and a number: with nothing
                // written about them, an avatar alone left half the block empty,
                // which is the blank area rule 11 forbids. Seen on the phone.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Avatar(name = name, hue = hue, size = Space.avatarLead, solid = true)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        // **Where they work**, which the care team row carries
                        // and this screen did not say at all.
                        person.organizationName?.takeIf { it.isNotBlank() }?.let {
                            Body(text = Bidi.isolate(it), color = hue.ink)
                        }
                        // **What you need to know before you dial, above the
                        // thing that dials.** "Days, 7 to 3, ask for her by
                        // name" is the reason somebody opened this screen, and
                        // it used to sit under both buttons, where the eye
                        // reaches it after the decision has been made.
                        person.notes?.takeIf { it.isNotBlank() }?.let {
                            Body(
                                text = Bidi.isolate(it),
                                color = colors.ink,
                                style = HealthTrail.type.bodyL,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            // **The number carries the weight, because it is
                            // the whole point**, and correcting a spelling is
                            // the rare errand that stays a mark in the corner.
                            // Rule 15.
                            if (phone != null) {
                                Action(
                                    label = strings["careteam.call"],
                                    onClick = { onCall(phone) },
                                    emphasis = ActionEmphasis.Main,
                                    mark = Symbols.call,
                                    // The dialer opens with the number in it and
                                    // the person presses the green button, so
                                    // nothing is committed here and nothing ticks.
                                    confirms = false,
                                    modifier = Modifier
                                        .semantics {
                                            contentDescription =
                                                strings("person.call", "number" to phone)
                                        }
                                        .testTag(PersonTags.CALL),
                                )
                            }
                            if (address != null && onEmail != null) {
                                Action(
                                    label = strings["careteam.email"],
                                    onClick = { onEmail(address) },
                                    mark = Symbols.mail,
                                    modifier = Modifier
                                        .semantics {
                                            contentDescription = strings(
                                                "careteam.email.address",
                                                "address" to address,
                                            )
                                        }
                                        .testTag(PersonTags.EMAIL),
                                )
                            }
                        }

                        if (phone == null) {
                            // bidi-ok: the app's own sentence about a person
                            // with no number yet, who is a complete person.
                            // Rule 13.
                            Body(text = strings["careteam.no_phone"])
                        }

                    }
                }

                // **Under the row rather than in it**, because its label is a
                // sentence: beside a 56dp mark the column is narrower and
                // "Write something down about them" wrapped around its own
                // plus sign. Seen on the phone. Reaching them is the row;
                // writing about them is the whole block's width.
                Action(
                    label = strings["person.capture"],
                    onClick = onCapture,
                    mark = Symbols.add,
                    modifier = Modifier.testTag(PersonTags.CAPTURE),
                )
            }
        }

        // **When you are seeing them, above what was said**, because it is ahead
        // rather than behind: somebody opening a person's screen the day before
        // a review is looking for the review.
        labeledBlock(
            label = strings["person.appointments"],
            rows = appointments.map { appointment ->
                {
                    ListRow(
                        title = Bidi.isolate(appointment.title),
                        support = appointment.scheduledEdtf
                            ?.takeIf { it.isNotBlank() }
                            ?.let { EventDateText.render(strings, it) },
                        mark = Symbols.appointments,
                        markTint = colors.goldInk,
                        markWash = colors.goldWash,
                        isDoor = true,
                        onClick = { onOpenAppointment(appointment) },
                        clickLabel = strings["open.action"],
                        modifier = Modifier.testTag(PersonTags.appointment(appointment.id)),
                    )
                }
            },
        )

        item { Eyebrow(text = strings["person.entries"]) }

        if (entries.isEmpty()) {
            item {
                Block {
                    // bidi-ok: the app's own sentence, with the person's name
                    // isolated into it by the caller below.
                    Body(
                        text = strings(
                            "person.entries.empty",
                            "name" to Bidi.isolate(person.displayName),
                        ),
                    )
                }
            }
        }

        items(entries, key = { it.id }) { entry ->
            Block(
                modifier = Modifier
                    .semantics(mergeDescendants = true) { }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = strings["prep.change.open"],
                        onClick = { onOpenEntry(entry) },
                    )
                    .testTag(PersonTags.entry(entry.id)),
            ) {
                Column {
                    entry.occurredEdtf?.takeIf { it.isNotBlank() }?.let {
                        Eyebrow(text = EventDateText.render(strings, it), fixed = false)
                    }
                    Body(
                        text = entry.title?.takeIf { it.isNotBlank() }
                            ?.let { Bidi.isolate(it) }
                            ?: strings[kindNameKey(entry.kind)],
                        color = colors.ink,
                        style = HealthTrail.type.rowTitle,
                    )
                    entry.body?.takeIf { it.isNotBlank() }?.let {
                        Body(
                            text = Bidi.isolate(it),
                            // **Three lines and then the card ends.** The whole
                            // entry is one tap away, and a list where one item
                            // is a page long stops being a list.
                            maxLines = ENTRY_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // **The rarest things last, and each says what it does.** Somebody
        // looking for a way to say "she left" will otherwise take the only
        // control there is, which is the one that erases the record.
        item {
            Spacer(Modifier.height(Space.s))
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Action(
                    label = strings[
                        if (person.pinnedAt != null) "careteam.unpin" else "careteam.pin",
                    ],
                    onClick = { onSetPinned(person.pinnedAt == null) },
                    modifier = Modifier.testTag(PersonTags.PIN),
                )
                Action(
                    label = strings[
                        if (person.archivedAt != null) "careteam.unarchive" else "careteam.archive",
                    ],
                    onClick = { onSetArchived(person.archivedAt == null) },
                    modifier = Modifier.testTag(PersonTags.ARCHIVE),
                )
                // bidi-ok: the app's own note about what archiving does.
                Body(text = strings["careteam.archive.note"], style = HealthTrail.type.bodyS)
                Action(
                    label = strings["remove.action"],
                    onClick = onRemove,
                    modifier = Modifier.testTag(PersonTags.REMOVE),
                )
            }
        }
    }
}

/** How much of an entry a card shows before the entry's own screen takes over. */
private const val ENTRY_LINES = 3
