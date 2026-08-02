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
import com.kamsiob.healthtrail.data.Repository
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
    onCall: (Repository.EmergencyContact) -> Unit,
    onEdit: () -> Unit,
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
    ) {
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

        if (card == null || (card.isEmpty && contacts.isEmpty())) {
            item {
                SectionEmpty(name = EmergencyTags.NAME, text = strings["emergency.empty"])
                Spacer(Modifier.height(Space.l))
            }
        } else {
            val inAHurry = listOfNotNull(
                field("allergies", strings["emergency.allergies"], card.allergies),
                field("blood_type", strings["emergency.blood_type"], card.bloodType),
                field("conditions", strings["emergency.conditions"], card.conditions),
            )
            val paperwork = listOfNotNull(
                field(
                    "resuscitation",
                    strings["emergency.resuscitation"],
                    card.resuscitationStatus,
                ),
                field(
                    "resuscitation_where",
                    strings["emergency.resuscitation.where"],
                    card.resuscitationDocumentLocation,
                ),
                field(
                    "decision_maker_where",
                    strings["emergency.decision_maker.where"],
                    card.decisionMakerDocumentLocation,
                ),
                field("insurance", strings["emergency.insurance"], card.insuranceNote),
                field("other", strings["emergency.other"], card.otherNotes),
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

        item {
            QuietButton(
                label = strings["emergency.edit"],
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().testTag(EmergencyTags.EDIT),
            )
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
            .background(colors.alertSoft)
            .testTag(EmergencyTags.contact(contact.id))
            .padding(Space.cardPadding),
    ) {
        contact.relationship?.takeIf { it.isNotBlank() }?.let { relationship ->
            Text(
                text = relationship,
                style = HealthTrail.type.mono,
                color = colors.alertText,
            )
            Spacer(Modifier.height(Space.xs))
        }

        Text(
            text = contact.displayName,
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
                color = colors.alertText,
            )
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
            .background(colors.alertSoft)
            .testTag(EmergencyTags.field(entry.key))
            .padding(Space.cardPadding),
    ) {
        Text(
            text = entry.label,
            style = HealthTrail.type.mono,
            color = colors.alertText,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = entry.value,
            style = HealthTrail.type.displayS,
            color = colors.ink,
        )
    }
}
