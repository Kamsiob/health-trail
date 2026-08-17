package com.kamsiob.healthtrail.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.LocalMotion
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.Disclosure
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.Avatar
import com.kamsiob.healthtrail.ui.components.DictateAction
import com.kamsiob.healthtrail.ui.components.FieldRow
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.Eyebrow
import com.kamsiob.healthtrail.ui.v4.FactBlock

object AddPersonTags {
    const val EMAIL = "add_person_email"
    const val ROOT = "add_person_root"
    const val NAME = "add_person_name"
    const val ROLE = "add_person_role"
    fun suggestion(role: String) = "add_person_role_" + role.lowercase().replace(' ', '_')
    const val WHERE = "add_person_where"
    fun place(name: String) = "add_person_place_" + name.lowercase().replace(' ', '_')
    const val PHONE = "add_person_phone"
    const val NOTES = "add_person_notes"
    const val SAVE = "add_person_save"
    const val MORE = "add_person_more"
    const val CANCEL = "add_person_cancel"
}

/** What the person typed. Every field optional, including the name. */
data class PersonDraft(
    val name: String,
    val role: String,
    val phone: String,
    /**
     * Their email, which the column has held since Phase 0 with nothing
     * writing it. #379: some of these people answer email and never the
     * phone, and the family needs both.
     */
    val email: String = "",
    /**
     * Anything else about them.
     *
     * **`person.notes` has been in the schema since Phase 0 and no form ever
     * wrote it.** The owner asked for it by name on #361: name, title, number,
     * notes. Where somebody parks, which extension actually reaches them, and
     * that they are only in on Tuesdays is the knowledge that makes a care team
     * useful, and it was being kept in people's heads.
     */
    val notes: String = "",
    /**
     * Where they work, in the person's own words.
     *
     * **A name rather than a chosen row**, because there is no catalog of
     * facilities and there must not be: which places a family deals with is
     * theirs, the same way a care thread and a measure are. The repository
     * matches what they typed against what they have typed before, so
     * "Maplewood" twice is one place. #353.
     */
    val where: String = "",
)

/**
 * Adding somebody to the care team, and correcting them afterward.
 *
 * **The card is the screen, not a stack of boxes.** #361, rebuilt 2026-08-12
 * after the owner said the first attempt still looked like a data entry app and
 * that even a phone's Contacts app looks better. It is one raised group of
 * rows, each with its label above its value and a hairline between, which is
 * the shape the rest of the app already uses to *show* a person. A form that
 * matches the screen it feeds is one thing seen twice rather than two things.
 *
 * **The name, the role and the number lead; where they work and anything to
 * remember are behind "Add more".** #361 says the care team "leads with the
 * name and the number, which is the reason anybody adds a person", and an
 * earlier build put the **role** behind the disclosure and had it reverted,
 * because the role is the field that tells you who somebody is and a control
 * saying "Add more" is the wrong place for it. Both hold at once, and this is
 * the shape that satisfies them.
 *
 * **It asked all five at one weight until 2026-08-13**, while D147 recorded it
 * as converted along with the medication, appointment, bill and document forms.
 * Those four had disclosures; this one did not. Found by opening it on the
 * phone, and the correction is on D147 as well as here. #376.
 *
 * **The face appears once there is a name.** It used to be drawn empty, on the
 * argument that an empty one is the honest state before a name. On the phone it
 * is a featureless colored circle floating above the first field and it reads
 * as a picture that failed to load, which is rule 11's placeholder. Looking at
 * it is what changed the answer.
 *
 * **Every field is optional, including the name**, per rule 13 and for the same
 * reason the capture form requires nothing: somebody standing in a corridor
 * with a number on a scrap of paper should be able to keep it. A row with a
 * number and no name is more useful than a number that was never written down.
 *
 * **The phone field takes a phone keyboard but never validates.** Numbers in a
 * care setting arrive as extensions, as "ask for the charge nurse", and as
 * partial strings somebody read out too fast. Rejecting any of those would lose
 * the thing the person was trying to keep.
 *
 * **The face is the person taking shape as you type.** The initials fill in
 * from the name, which is the same avatar their row will wear, so the form
 * shows what it is making rather than only asking for it.
 */
@Composable
fun AddPersonScreen(
    onSave: (PersonDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Roles the active situation template names, offered as chips.
     *
     * Empty for a notebook with no situation, which is a real state: "Not sure
     * yet" is a valid answer to the picker and produces a working notebook.
     */
    roleSuggestions: List<String> = emptyList(),
    /**
     * The person being corrected, or null when this is somebody new.
     *
     * **The same screen does both**, because they ask exactly the same
     * questions and a second near identical form is how two screens drift
     * apart. Only the heading changes, so somebody correcting a number is not
     * told they are adding a person.
     */
    existing: Repository.Person? = null,
    /**
     * Places somebody on this care team already works, offered as chips.
     *
     * **A family deals with the same three or four for months**, and typing
     * "Maplewood Care Center" for the eleventh person is the app asking
     * somebody to do its remembering. Empty on a notebook where nobody has one,
     * and then the field is simply a field. #353.
     */
    organizations: List<String> = emptyList(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var name by remember(existing?.id) { mutableStateOf(existing?.displayName.orEmpty()) }
    var role by remember(existing?.id) { mutableStateOf(existing?.roleLabel.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var where by remember(existing?.id) { mutableStateOf(existing?.organizationName.orEmpty()) }

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
                FormHeader(
                    title = if (existing == null) {
                        strings["careteam.add.title"]
                    } else {
                        strings["careteam.edit.title"]
                    },
                    // The lead is an Aside now, on the section's wash with
                    // its own icon, rather than the smallest gray line under the
                    // title. D172, and the approved medication mockup.
                    lead = null,
                    section = Repository.Section.CARE_TEAM,
                )
                    Spacer(Modifier.height(Space.m))
                    FactBlock(
                        label = null,
                        text = strings["careteam.add.lead"],
                        tone = BlockTone.Section,
                        mark = Symbols.of(Repository.Section.CARE_TEAM),
                        hue = hueFor(Repository.Section.CARE_TEAM),
                    )

                // **The face the row will wear, filling in as they type.** It
                // is the same avatar the care team draws, so somebody sees what
                // they are making rather than only being asked for it.
                //
                // **Only once there is a name**, which is the correction. An
                // avatar with nothing in it is a featureless colored circle
                // floating above the first field, and on the phone it reads as
                // a picture that failed to load rather than as a preview
                // waiting for a name. Rule 11 bans a placeholder, and a shape
                // that means nothing until a condition is met is one.
                //
                // **It appears rather than being uncovered**, so the moment it
                // starts saying something is the moment it arrives. `AnimatedVisibility`
                // takes its spec from the motion tokens, per section 10.
                AnimatedVisibility(
                    visible = name.isNotBlank(),
                    enter = fadeIn(LocalMotion.current.standard()),
                    exit = fadeOut(LocalMotion.current.standard()),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Space.l))
                        Avatar(
                            name = name,
                            hue = hueFor(Repository.Section.CARE_TEAM),
                            size = Space.avatarLead,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }

                Spacer(Modifier.height(Space.l))

                // **Who they are and how to reach them, which is the section.**
                // "Who you call. Everyone involved in the care, and how to
                // reach them." This screen asked five questions at one weight,
                // which is law 3 and rule 15 both, and #361 says in the owner's
                // own terms that the care team "leads with the name and the
                // number, which is the reason anybody adds a person".
                //
                // **The role leads too, and that is not an oversight.** An
                // earlier build put it behind "Add more" and it was reverted:
                // the role is the field that tells you who somebody *is*, and a
                // control labeled "Add more" is the wrong place for it. So the
                // two records that looked like they disagreed do not. Lead with
                // the name, the role and the number; fold where they work and
                // anything to remember.
                //
                // **Found by opening the form on the phone**, where it was five
                // rows of identical weight and D147 said it had been converted.
                // #376.
                Block(padding = Space.none) {
                    FieldRow(
                        label = strings["careteam.add.name"],
                        value = name,
                        onValueChange = { name = it },
                        hint = strings["careteam.add.name.hint"],
                        imeAction = ImeAction.Next,
                        fieldTestTag = AddPersonTags.NAME,
                    )

                    FieldRow(
                        label = strings["careteam.add.role"],
                        value = role,
                        onValueChange = { role = it },
                        hint = strings["careteam.add.role.hint"],
                        imeAction = ImeAction.Next,
                        fieldTestTag = AddPersonTags.ROLE,
                    )

                    FieldRow(
                        label = strings["careteam.add.phone"],
                        value = phone,
                        onValueChange = { phone = it },
                        hint = strings["careteam.add.phone.hint"],
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Phone,
                        fieldTestTag = AddPersonTags.PHONE,
                        divider = false,
                    )
                }

                // **Directly under the field they fill, and above the fold.**
                // The role leads on this form, so the chips that offer one
                // lead with it. They spent a build inside "Add more", which
                // placed an offer about a visible field behind a control that
                // says it holds what is hidden.
                if (roleSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(Space.sectionGap))
                    // **The header and the chips, and no sentence between
                    // them.** It read "Tap one to fill it in. You can change
                    // it, or ignore these and write your own", which is two
                    // lines of the app explaining that a chip fills a field and
                    // that nothing here is required. Rule 20: the first is what
                    // a chip visibly does, and the second is what the lead
                    // sentence at the top of the form already promises. On the
                    // phone those two lines plus six chips pushed "Add more"
                    // off the bottom of the screen.
                    Eyebrow(text = strings["careteam.add.role.suggestions"], fixed = false)
                    Spacer(Modifier.height(Space.headerGap))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.s),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        roleSuggestions.forEach { suggestion ->
                            ChoiceChip(
                                label = suggestion,
                                // Selected when the field already says it, so a
                                // chip tapped by mistake can be seen and a
                                // second tap clears it.
                                selected = role.trim().equals(suggestion, ignoreCase = true),
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

                // **Open when any of it already says something**, which is the
                // whole point of `startOpen`: a form correcting a saved record
                // must not fold away words somebody typed last week behind a
                // control that reads "Add more".
                Disclosure(
                    testTag = AddPersonTags.MORE,
                    startOpen = where.isNotBlank() || notes.isNotBlank(),
                ) {
                Block(padding = Space.none) {
                    // **Where they work, which the care team folds by.** Grid
                    // screen 11 groups the people who are not in the lead by
                    // exactly this, and the column and its index shipped in
                    // Phase 0 with nothing writing either, so the fold could
                    // not be built: grouping by a column nothing writes gives
                    // one fold holding everybody. #353.
                    FieldRow(
                        label = strings["careteam.add.where"],
                        value = where,
                        onValueChange = { where = it },
                        hint = strings["careteam.add.where.hint"],
                        imeAction = ImeAction.Next,
                        fieldTestTag = AddPersonTags.WHERE,
                    )

                    FieldRow(
                        label = strings["careteam.add.email"],
                        value = email,
                        onValueChange = { email = it },
                        hint = strings["careteam.add.email.hint"],
                        imeAction = ImeAction.Next,
                        fieldTestTag = AddPersonTags.EMAIL,
                    )

                    FieldRow(
                        label = strings["careteam.add.notes"],
                        value = notes,
                        onValueChange = { notes = it },
                        hint = strings["careteam.add.notes.hint"],
                        singleLine = false,
                        imeAction = ImeAction.Done,
                        fieldTestTag = AddPersonTags.NOTES,
                        // The microphone, in the field, per #361. The note is
                        // the one thing here somebody would rather say than
                        // type: "parks round the back, only in on Tuesdays".
                        trailing = {
                            DictateAction(
                                inField = true,
                                onText = { spoken ->
                                    notes = if (notes.isBlank()) {
                                        spoken
                                    } else {
                                        "${notes.trimEnd()} $spoken"
                                    }
                                },
                            )
                        },
                        divider = false,
                    )
                }

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
                // **Under the card rather than inside it**, because they are an
                // offer about one row rather than a row of their own, and a
                // wrapping strip of chips inside a group of aligned rows is the
                // thing that breaks the alignment the group exists for.
                //
                // **The field stays.** A role that is not on the list is the
                // common case in home care and in a hospital, tapping a chip
                // only fills the field, and what it filled can be edited or
                // cleared.
                // The places already on this care team, offered the same way
                // the roles are: tapping one fills the field, and what it
                // filled can be edited or cleared like anything else typed.
                if (organizations.isNotEmpty()) {
                    Spacer(Modifier.height(Space.sectionGap))
                    Eyebrow(text = strings["careteam.add.where.known"], fixed = false)
                    Spacer(Modifier.height(Space.headerGap))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.s),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        organizations.forEach { place ->
                            ChoiceChip(
                                label = Bidi.isolate(place),
                                selected = where.equals(place, ignoreCase = true),
                                onClick = {
                                    where = if (where.equals(place, ignoreCase = true)) "" else place
                                },
                                modifier = Modifier.testTag(AddPersonTags.place(place)),
                            )
                        }
                    }
                }

                }

                Spacer(Modifier.height(Space.xl))
            }

            // The pinned action footer, per 5.15, with its required gap. The
            // button never disables: there is no such thing as not enough to
            // save, and a button that greys out is the app telling somebody
            // they have not done enough.
            Spacer(Modifier.height(Space.m))

            Action(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        PersonDraft(
                            name = name,
                            role = role,
                            phone = phone,
                            email = email,
                            notes = notes,
                            where = where,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddPersonTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            Spacer(Modifier.height(Space.s))

            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddPersonTags.CANCEL),
            )

            Spacer(Modifier.height(Space.l))
        }
    }
}
