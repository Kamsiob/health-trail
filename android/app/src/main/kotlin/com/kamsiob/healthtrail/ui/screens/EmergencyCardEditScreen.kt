package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.GroupHeader
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object EmergencyEditTags {
    const val ROOT = "emergency_edit_root"
    const val SAVE = "emergency_edit_save"
    const val CANCEL = "emergency_edit_cancel"
    fun field(key: String) = "emergency_edit_$key"
}

/** Everything the card holds, as typed. */
data class EmergencyDraft(
    val allergies: String = "",
    val bloodType: String = "",
    val conditions: String = "",
    val resuscitationStatus: String = "",
    val resuscitationWhere: String = "",
    val decisionMakerWhere: String = "",
    val insurance: String = "",
    val other: String = "",
)

/**
 * Filling in the emergency card.
 *
 * **Nine fields, all optional, in the order somebody actually learns them.**
 * Allergies first because it is the one thing most people already know and the
 * one a paramedic asks for first. The paperwork sits in its own group below,
 * because it is the part that gets filled in over weeks rather than in the
 * first two minutes.
 *
 * **Nothing here validates and nothing is required**, per rule 13. Blood type
 * is asked for with "only if you know it for certain" precisely because a
 * guessed blood type on a card handed to a paramedic is worse than a blank one,
 * and the honest way to get that is to say so rather than to reject input.
 *
 * **The resuscitation field asks for what the paperwork says, in its own
 * words.** It is not a choice between options, and it must never become one:
 * the app would then be interpreting a legal document, which rule 2 forbids and
 * which it cannot do correctly across fifty states. It asks for the sentence
 * and, next to it, where the original is kept.
 *
 * The whole form is one screen and scrolls, with the action pinned, so the
 * keyboard never covers the field being typed into. D38.
 */
@Composable
fun EmergencyCardEditScreen(
    card: Repository.EmergencyCard?,
    people: List<Repository.Person>,
    onTheCard: Set<String>,
    onToggleContact: (Repository.Person) -> Unit,
    onSave: (EmergencyDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // Seeded from what is already there, so opening the editor on a card that
    // has three lines does not read as starting over.
    var draft by remember(card?.id) {
        mutableStateOf(
            EmergencyDraft(
                allergies = card?.allergies.orEmpty(),
                bloodType = card?.bloodType.orEmpty(),
                conditions = card?.conditions.orEmpty(),
                resuscitationStatus = card?.resuscitationStatus.orEmpty(),
                resuscitationWhere = card?.resuscitationDocumentLocation.orEmpty(),
                decisionMakerWhere = card?.decisionMakerDocumentLocation.orEmpty(),
                insurance = card?.insuranceNote.orEmpty(),
                other = card?.otherNotes.orEmpty(),
            ),
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(EmergencyEditTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = strings["emergency.edit.title"],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["emergency.edit.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                // **Who to call is chosen, never typed again.** Everybody on
                // the care team already has a name and a number in this
                // notebook, and asking for them a second time would be the
                // interface making the person do the app's filing, which rule
                // 20 forbids. One tap puts somebody on the card, one tap takes
                // them off.
                Spacer(Modifier.height(Space.l))
                GroupHeader(labelKey = "emergency.group.who")
                Spacer(Modifier.height(Space.headerGap))

                if (people.isEmpty()) {
                    Text(
                        text = strings["emergency.who.empty_team"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                } else {
                    Text(
                        text = strings["emergency.who.lead"],
                        style = HealthTrail.type.bodyM,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(Space.sm))
                    ChoiceChipGroup(label = strings["emergency.group.who"]) {
                        people.forEach { person ->
                            ChoiceChip(
                                label = Bidi.isolate(
                                    person.displayName.ifBlank {
                                        person.phone.orEmpty().ifBlank {
                                            person.roleLabel.orEmpty()
                                        }
                                    },
                                ),
                                selected = person.id in onTheCard,
                                onClick = { onToggleContact(person) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.m))
                GroupHeader(labelKey = "emergency.group.medical")
                Spacer(Modifier.height(Space.headerGap))

                Field(
                    key = "allergies",
                    label = strings["emergency.allergies"],
                    hint = strings["emergency.allergies.hint"],
                    value = draft.allergies,
                    onChange = { draft = draft.copy(allergies = it) },
                )
                Field(
                    key = "blood_type",
                    label = strings["emergency.blood_type"],
                    hint = strings["emergency.blood_type.hint"],
                    value = draft.bloodType,
                    onChange = { draft = draft.copy(bloodType = it) },
                )
                Field(
                    key = "conditions",
                    label = strings["emergency.conditions"],
                    hint = strings["emergency.conditions.hint"],
                    value = draft.conditions,
                    onChange = { draft = draft.copy(conditions = it) },
                )

                Spacer(Modifier.height(Space.m))
                GroupHeader(labelKey = "emergency.group.paperwork")
                Spacer(Modifier.height(Space.headerGap))

                Field(
                    key = "resuscitation",
                    label = strings["emergency.resuscitation"],
                    hint = strings["emergency.resuscitation.hint"],
                    value = draft.resuscitationStatus,
                    onChange = { draft = draft.copy(resuscitationStatus = it) },
                )
                Field(
                    key = "resuscitation_where",
                    label = strings["emergency.resuscitation.where"],
                    hint = strings["emergency.resuscitation.where.hint"],
                    value = draft.resuscitationWhere,
                    onChange = { draft = draft.copy(resuscitationWhere = it) },
                )
                Field(
                    key = "decision_maker_where",
                    label = strings["emergency.decision_maker.where"],
                    hint = strings["emergency.decision_maker.where.hint"],
                    value = draft.decisionMakerWhere,
                    onChange = { draft = draft.copy(decisionMakerWhere = it) },
                )
                Field(
                    key = "insurance",
                    label = strings["emergency.insurance"],
                    hint = strings["emergency.insurance.hint"],
                    value = draft.insurance,
                    onChange = { draft = draft.copy(insurance = it) },
                )
                Field(
                    key = "other",
                    label = strings["emergency.other"],
                    hint = strings["emergency.other.hint"],
                    value = draft.other,
                    onChange = { draft = draft.copy(other = it) },
                    imeAction = ImeAction.Done,
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(EmergencyEditTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(EmergencyEditTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}

@Composable
private fun Field(
    key: String,
    label: String,
    hint: String,
    value: String,
    onChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    DictatableField(
        label = label,
        value = value,
        onValueChange = onChange,
        hint = hint,
        imeAction = imeAction,
        // Every one of these can run long. An allergy is often a sentence, and
        // the resuscitation line is a quotation from a form, so none of them is
        // held to a single line.
        singleLine = false,
        fieldTestTag = EmergencyEditTags.field(key),
    )
    Spacer(Modifier.height(Space.m))
}
