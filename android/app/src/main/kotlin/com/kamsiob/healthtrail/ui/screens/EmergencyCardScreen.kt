package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.QuietButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Radius
import com.kamsiob.healthtrail.ui.theme.Space

object EmergencyTags {
    const val NAME = "emergency_card"
    const val EDIT = "emergency_card_edit"
    const val SHARE = "emergency_card_share"
    const val CHANGE = "emergency_card_change"
    fun field(key: String) = "emergency_field_$key"
    fun contact(id: String) = "emergency_contact_$id"
    fun call(id: String) = "emergency_call_$id"
}

/**
 * The emergency card: what somebody would need to know in a hurry.
 *
 * **It is designed to be handed to a paramedic**, per `MASTER_SPEC.md` section
 * 4.6, and that decides almost everything about it. Somebody reading it is
 * standing up, holding another person's phone, under time pressure, and did not
 * write any of it. So the values carry the weight and the labels recede, which
 * is the opposite of a form and the reverse of what a settings screen does.
 *
 * **Only what has been filled in appears.** An empty field is not shown as an
 * empty field. A card of three lines that are all true is worth more than a
 * card of nine where six say nothing, and under time pressure the blanks are
 * noise somebody has to read past.
 *
 * **The card never interprets, per rule 2.** The resuscitation line is stored
 * and shown as **what the signed paperwork says**, in the words on the form,
 * next to where the original is kept. The app does not summarize it, does not
 * shorten it to an abbreviation, and does not say what it means. A card that is
 * wrong about this is worse than no card, and the only safe thing the app can
 * do is repeat the sentence and say where the paper is.
 *
 * **It is `alert` toned throughout**, which section 2.2 gives to this one
 * section and nothing else.
 */
@Composable
fun EmergencyCardScreen(
    card: Repository.EmergencyCard?,
    contacts: List<Repository.EmergencyContact>,
    medications: List<Repository.Medication>,
    onCall: (Repository.EmergencyContact) -> Unit,
    onEdit: () -> Unit,
    /**
     * Hands the card to the share sheet as plain text.
     *
     * **The door out, per the grid.** The card is the one thing in this app most
     * likely to be needed by somebody who does not have the app: a sibling in
     * another state, a neighbor with a key, the folder at the nurses' station.
     */
    onShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    SectionScaffold(
        name = EmergencyTags.NAME,
        title = strings["notebook.section.emergency_card"],
        subtitle = strings["emergency.subtitle"],
        onBack = onBack,
        modifier = modifier,
        section = Repository.Section.EMERGENCY_CARD,
        headingKey = "emergency.heading",
    ) {
        // Computed before anything draws, because the header at the top of the
        // screen depends on it as much as the body below does.
        val hasSomething = (card != null && !card.isEmpty) ||
            contacts.isNotEmpty() ||
            medications.isNotEmpty()

        // **Two doors, at the top, where a person looks for them.** The grid
        // draws a Change pill on every block. That is four identical controls
        // opening one editor, which is the same noise the trail's per-row pin
        // turned out to be, so there is one of each instead. `DESIGN.md` 15.1
        // records the departure.
        //
        // **Neither appears on an empty card**, and they are two different
        // reasons. **Sharing one would hand somebody a document with nothing
        // on it**, which is the same call `PrepScreen` makes about a phone
        // with no calendar: an action that cannot do anything shows no action
        // at all rather than one that fails when tapped. **And Change would be
        // the second way to say "Fill in the card"**, which the empty state
        // already offers, so it is one action under two names on one screen,
        // which is the very noise this row exists to have removed.
        if (hasSomething) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    QuietButton(
                        label = strings["emergency.share"],
                        onClick = onShare,
                        modifier = Modifier.weight(1f).testTag(EmergencyTags.SHARE),
                    )
                    QuietButton(
                        label = strings["emergency.change"],
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).testTag(EmergencyTags.CHANGE),
                    )
                }
                Spacer(Modifier.height(Space.sectionGap))
            }
        }

        // **Who to call comes first, above everything else on the card.**
        // Somebody holding this phone in an emergency needs a number before
        // they need a blood type: the number reaches a person who knows the
        // rest. The paperwork below it is what they read once they have made
        // the call.
        if (contacts.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "emergency.group.who")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (contact in contacts) {
                item(key = contact.id) {
                    ContactRow(contact = contact, onCall = { onCall(contact) })
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **Assembled from the medications that say they belong here**, rather
        // than copied onto the card. A medication knows it is on the card, so
        // one that gets stopped drops off by itself, which is the behavior
        // somebody would expect and the one that is dangerous to get wrong.
        if (medications.isNotEmpty()) {
            item {
                GroupHeader(labelKey = "emergency.group.meds")
                Spacer(Modifier.height(Space.headerGap))
            }
            for (medication in medications) {
                item(key = "med_${medication.id}") {
                    MedicationCardRow(medication)
                    Spacer(Modifier.height(Space.cardGap))
                }
            }
            item { Spacer(Modifier.height(Space.s)) }
        }

        // **Emptiness is about what the card shows, not about whether a row
        // exists.** Medications reach this card through their own flag and need
        // no card row at all, so keying the empty state off `card == null`
        // printed "Nothing on the card yet" directly underneath a medication
        // that was plainly on it. Found by looking at the screen; nothing in
        // the code looked wrong.
        if (!hasSomething) {
            item {
                SectionEmpty(name = EmergencyTags.NAME, text = strings["emergency.empty"], section = Repository.Section.EMERGENCY_CARD, modifier = Modifier.fillParentMaxHeight(EMPTY_HEIGHT_FRACTION))
                Spacer(Modifier.height(Space.l))
            }
        } else {
            // Null throughout when no card row exists yet, which is the state a
            // notebook is in when a medication put itself on the card before
            // anybody opened the editor. Both lists come out empty and their
            // headers do not appear.
            val inAHurry = listOfNotNull(
                field("allergies", strings["emergency.allergies"], card?.allergies),
                field("blood_type", strings["emergency.blood_type"], card?.bloodType),
                field("conditions", strings["emergency.conditions"], card?.conditions),
            )
            val paperwork = listOfNotNull(
                field(
                    "resuscitation",
                    strings["emergency.resuscitation"],
                    card?.resuscitationStatus,
                ),
                field(
                    "resuscitation_where",
                    strings["emergency.resuscitation.where"],
                    card?.resuscitationDocumentLocation,
                ),
                field(
                    "decision_maker_where",
                    strings["emergency.decision_maker.where"],
                    card?.decisionMakerDocumentLocation,
                ),
                field("insurance", strings["emergency.insurance"], card?.insuranceNote),
                field("other", strings["emergency.other"], card?.otherNotes),
            )

            // A group header only appears when its group has something in it, so
            // the card never shows a heading over nothing.
            if (inAHurry.isNotEmpty()) {
                item {
                    GroupHeader(labelKey = "emergency.group.medical")
                    Spacer(Modifier.height(Space.headerGap))
                }
                for (entry in inAHurry) {
                    item(key = entry.key) {
                        CardField(entry)
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }

            if (paperwork.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(Space.s))
                    GroupHeader(labelKey = "emergency.group.paperwork")
                    Spacer(Modifier.height(Space.headerGap))
                }
                for (entry in paperwork) {
                    item(key = entry.key) {
                        CardField(entry)
                        Spacer(Modifier.height(Space.cardGap))
                    }
                }
            }

            item { Spacer(Modifier.height(Space.s)) }
        }

        // **The one action on an empty card, and the only one.** On a filled
        // card the Change pill at the top is the same door, so this would be
        // the third control opening one editor.
        if (!hasSomething) {
            item {
                QuietButton(
                    label = strings["emergency.edit"],
                    onClick = onEdit,
                    modifier = Modifier.testTag(EmergencyTags.EDIT),
                )
            }
        }
    }
}

/**
 * One person to call, with the number as the action.
 *
 * The same treatment the care team gives a person, in the card's own tone. The
 * relationship is the eyebrow, because on this card "her daughter" or "the
 * facility" is what tells a stranger which call to make first.
 */
@Composable
private fun ContactRow(
    contact: Repository.EmergencyContact,
    onCall: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors
    val phone = contact.phone?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.alertWash)
            .testTag(EmergencyTags.contact(contact.id))
            .padding(Space.cardPadding),
    ) {
        contact.relationship?.takeIf { it.isNotBlank() }?.let { relationship ->
            Text(
                text = Bidi.isolate(relationship),
                style = HealthTrail.type.mono,
                color = colors.alertInk,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = Bidi.isolate(contact.displayName),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )

        Spacer(Modifier.height(Space.xs))

        if (phone != null) {
            TextAction(
                label = strings("careteam.call.number", "number" to phone),
                onClick = onCall,
                modifier = Modifier.testTag(EmergencyTags.call(contact.id)),
            )
        } else {
            // Somebody can be on the card without a number, and saying so is
            // better than leaving a gap where an action should be. It is also
            // the honest prompt to go and find one.
            Text(
                text = strings["careteam.no_phone"],
                style = HealthTrail.type.bodyS,
                color = colors.alertInk,
            )
        }
    }
}

/**
 * One medication, as the card shows it.
 *
 * **The name is the value here, not the label.** On this card a paramedic is
 * reading names, and the dose is the detail under it. That is the reverse of
 * the medications screen, where the two are read together, and it is the same
 * principle both times: the thing being looked for carries the weight.
 *
 * A medication with no dose recorded is a complete row. Most people know what
 * somebody takes long before they can quote the dose.
 */
@Composable
private fun MedicationCardRow(medication: Repository.Medication) {
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.alertWash)
            .testTag(EmergencyTags.field("med_${medication.id}"))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = Bidi.isolate(medication.name),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
        medication.doseText?.takeIf { it.isNotBlank() }?.let { dose ->
            Spacer(Modifier.height(Space.xs))
            Text(text = Bidi.isolate(dose), style = HealthTrail.type.bodyM, color = colors.ink2)
        }
    }
}

/** One line of the card, present only because it has something in it. */
private data class CardEntry(val key: String, val label: String, val value: String)

private fun field(key: String, label: String, value: String?): CardEntry? =
    value?.takeIf { it.isNotBlank() }?.let { CardEntry(key, label, it) }

/**
 * One fact, with the fact carrying the weight and the label receding.
 *
 * **This inverts the usual order deliberately.** On a form the label is what
 * you are looking for. Here you already know what you are looking for and you
 * need the answer, so the answer is Display S and the label is the quiet mono
 * eyebrow above it. Somebody scanning this card is reading values, not
 * headings.
 */
@Composable
private fun CardField(entry: CardEntry) {
    val colors = HealthTrail.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.alertWash)
            .testTag(EmergencyTags.field(entry.key))
            .padding(Space.cardPadding),
    ) {
        Text(
            // bidi-ok: a catalog label, in the app's own words rather than the person's.
            text = entry.label,
            style = HealthTrail.type.mono,
            color = colors.alertInk,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = Bidi.isolate(entry.value),
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
    }
}
