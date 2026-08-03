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
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.FilledButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object AddPersonTags {
    const val ROOT = "add_person_root"
    const val NAME = "add_person_name"
    const val ROLE = "add_person_role"
    fun suggestion(role: String) = "add_person_role_" + role.lowercase().replace(' ', '_')
    const val PHONE = "add_person_phone"
    const val SAVE = "add_person_save"
    const val CANCEL = "add_person_cancel"
}

/** What the person typed. Every field optional, including the name. */
data class PersonDraft(
    val name: String,
    val role: String,
    val phone: String,
)

/**
 * Adding somebody to the care team.
 *
 * **The care team could be read and never written to**, which made it a section
 * that could only ever hold whatever setup happened to capture. Setup asks for
 * one number, at the bottom of a screen that can be skipped, so in practice the
 * section was empty and stayed empty.
 *
 * **Every field is optional, including the name**, per rule 13 and for the same
 * reason the capture form requires nothing: somebody standing in a corridor
 * with a number on a scrap of paper should be able to keep it. A row with a
 * number and no name is more useful than a number that was never written down.
 * The save action says "Save what you have", the same words the capture form
 * uses, because it is the same promise.
 *
 * **The phone field takes a phone keyboard but never validates.** Numbers in a
 * care setting arrive as extensions, as "ask for the charge nurse", and as
 * partial strings somebody read out too fast. Rejecting any of those would lose
 * the thing the person was trying to keep.
 *
 * Composed from Display L, Body M, the text field of 5.9, one filled button,
 * and one text action. Nothing new was introduced.
 */
@Composable
fun AddPersonScreen(
    /**
     * Roles the active situation template names, offered as chips.
     *
     * Empty for a notebook with no situation, which is a real state: "Not sure
     * yet" is a valid answer to the picker and produces a working notebook.
     */
    roleSuggestions: List<String> = emptyList(),
    onSave: (PersonDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The person being corrected, or null when this is somebody new.
     *
     * **The same screen does both**, because they ask exactly the same
     * questions and a second near identical form is how two screens drift
     * apart. Only the heading changes, so somebody correcting a number is not
     * told they are adding a person.
     */
    existing: Repository.Person? = null,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var name by remember(existing?.id) { mutableStateOf(existing?.displayName.orEmpty()) }
    var role by remember(existing?.id) { mutableStateOf(existing?.roleLabel.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                // Without this the keyboard covers the field being typed into,
                // which was real on setup and is D38.
                .imePadding()
                .testTag(AddPersonTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                Spacer(Modifier.height(Space.l))
                Text(
                    text = if (existing == null) {
                        strings["careteam.add.title"]
                    } else {
                        strings["careteam.edit.title"]
                    },
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    text = strings["careteam.add.lead"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )
                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["careteam.add.name"],
                    value = name,
                    onValueChange = { name = it },
                    hint = strings["careteam.add.name.hint"],
                    imeAction = ImeAction.Next,
                    fieldTestTag = AddPersonTags.NAME,
                )
                Spacer(Modifier.height(Space.m))

                // **The roles this situation actually has, offered as chips.**
                //
                // The situation templates have carried a `roles` list since the
                // catalog was written, documented there as "suggestions, not a
                // fixed list", and nothing had ever shown them. A nursing home
                // notebook knows there is a director of nursing, a charge
                // nurse, a social worker, an assessment coordinator, an
                // administrator, and a billing office, and it was asking the
                // person to type all six from memory.
                //
                // Part Two: anywhere the set of possible answers is knowable,
                // offer chips rather than a text field. P2 asks for exactly
                // this, in exactly these words: offered as suggestions without
                // forcing them.
                //
                // **The field stays.** A role that is not on the list is the
                // common case in home care and in a hospital, tapping a chip
                // only fills the field, and what it filled can be edited or
                // cleared. Nothing here is a fixed vocabulary.
                if (roleSuggestions.isNotEmpty()) {
                    ChoiceChipGroup(
                        label = strings["careteam.add.role.suggestions"],
                        aside = strings["careteam.add.role.suggestions.aside"],
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                            verticalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            roleSuggestions.forEach { suggestion ->
                                ChoiceChip(
                                    label = suggestion,
                                    // Selected when the field already says it,
                                    // so a chip tapped by mistake can be seen
                                    // and a second tap clears it.
                                    selected = role.trim()
                                        .equals(suggestion, ignoreCase = true),
                                    onClick = {
                                        role = if (
                                            role.trim().equals(suggestion, ignoreCase = true)
                                        ) {
                                            ""
                                        } else {
                                            suggestion
                                        }
                                    },
                                    modifier = Modifier.testTag(
                                        AddPersonTags.suggestion(suggestion),
                                    ),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.m))
                }

                HealthTrailTextField(
                    label = strings["careteam.add.role"],
                    value = role,
                    onValueChange = { role = it },
                    hint = strings["careteam.add.role.hint"],
                    imeAction = ImeAction.Next,
                    fieldTestTag = AddPersonTags.ROLE,
                )
                Spacer(Modifier.height(Space.m))

                HealthTrailTextField(
                    label = strings["careteam.add.phone"],
                    value = phone,
                    onValueChange = { phone = it },
                    hint = strings["careteam.add.phone.hint"],
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Phone,
                    fieldTestTag = AddPersonTags.PHONE,
                )

                Spacer(Modifier.height(Space.xl))
            }

            // The pinned action footer, per 5.15, with its required gap. The
            // button never disables: there is no such thing as not enough to
            // save, and a button that greys out is the app telling somebody
            // they have not done enough.
            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = { onSave(PersonDraft(name = name, role = role, phone = phone)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddPersonTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddPersonTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}
