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
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.v4.FactBlock
import java.time.LocalDate

object AddMilestoneTags {
    const val REMOVE = "milestone_remove"
    const val ROOT = "add_milestone_root"
    const val SAVE = "add_milestone_save"
    const val CANCEL = "add_milestone_cancel"
    const val PICK_DATE = "add_milestone_pick_date"
    fun field(key: String) = "add_milestone_$key"
    fun chapter(id: String) = "add_milestone_chapter_$id"
}

/** What the person typed about a milestone. */
data class MilestoneDraft(
    val label: String = "",
    val note: String = "",
    val occurred: Edtf.Date? = null,
    /** Where they were when it happened, or null because nobody said. */
    val chapterId: String? = null,
)

/**
 * Marking something worth remembering.
 *
 * **Only the person marks one.** Nothing here is suggested, derived, or
 * proposed: not from a run of good days, not from a measurement, not from a
 * chapter ending. Rule 2. "First day without oxygen" is a sentence only the
 * person watching can write, and the app's whole job is to hold it.
 *
 * **Only the label is needed**, and the date can be as rough as the memory is.
 * "Sometime last spring she started walking to the window" is a milestone, and
 * refusing it for want of a day would be the app deciding that a vague memory
 * is not worth keeping.
 *
 * **The chips are the same two the appointment form uses**, for the same
 * reason: a rough set built around "when did this happen today or yesterday"
 * does not fit something the person is remembering from a year ago, and the
 * picker plus "not sure" says both in the words the rest of the app uses.
 */
@Composable
fun AddMilestoneScreen(
    onSave: (MilestoneDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Takes the milestone out. Only offered on one that already exists:
     * anything added can be removed, 2026-08-16.
     */
    onRemove: () -> Unit = {},
    /** The one being corrected, or null when this is new. */
    existing: Repository.Milestone? = null,
    /**
     * The places, so one can be named.
     *
     * **Offered, never chosen for them.** The app could work out which chapter
     * a date falls inside and fill this in, and it does not: it does not file
     * anything on its own anywhere else either, and a milestone attributed to
     * the wrong place is a small false statement about somebody's own record.
     */
    chapters: List<Repository.Chapter> = emptyList(),
    today: LocalDate = LocalDate.now(),
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    var draft by remember(existing?.id) {
        mutableStateOf(
            MilestoneDraft(
                // bidi-ok: the value inside a field being edited. Isolate marks here would become characters the person has to delete.
                label = existing?.label.orEmpty(),
                note = existing?.note.orEmpty(),
                occurred = existing?.occurredEdtf?.let { Edtf.parse(it) },
                chapterId = existing?.chapterId,
            ),
        )
    }
    var picking by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = colors.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .testTag(AddMilestoneTags.ROOT),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screenHorizontal),
            ) {
                FormHeader(
                    title = strings[ if (existing == null) "milestones.add" else "milestones.edit.title" ],
                    // The lead is an Aside now, on the section's wash with
                    // its own icon, rather than the smallest gray line under the
                    // title. D172, and the approved medication mockup.
                    lead = null,
                    section = Repository.Section.CHAPTERS,
                )
                    Spacer(Modifier.height(Space.m))
                    FactBlock(
                        label = null,
                        text = strings["milestones.add.lead"],
                        tone = BlockTone.Section,
                        mark = Symbols.of(Repository.Section.CHAPTERS),
                        hue = hueFor(Repository.Section.CHAPTERS),
                    )
                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings["milestones.label"],
                    value = draft.label,
                    onValueChange = { draft = draft.copy(label = it) },
                    hint = strings["milestones.label.hint"],
                    fieldTestTag = AddMilestoneTags.field("label"),
                )
                Spacer(Modifier.height(Space.m))

                ChoiceChipGroup(label = strings["milestones.when"]) {
                    ChoiceChip(
                        label = strings["capture.when.exact"],
                        selected = draft.occurred != null &&
                            draft.occurred?.precision != Edtf.Precision.UNKNOWN,
                        onClick = { picking = true },
                        modifier = Modifier.testTag(AddMilestoneTags.PICK_DATE),
                    )
                    ChoiceChip(
                        label = strings["date.pick.clear"],
                        selected = draft.occurred?.precision == Edtf.Precision.UNKNOWN,
                        onClick = { draft = draft.copy(occurred = Edtf.unknown()) },
                    )
                }

                draft.occurred?.let { chosen ->
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = EventDateText.render(strings, chosen),
                        style = HealthTrail.type.bodyL,
                        color = colors.ink,
                    )
                }

                if (chapters.isNotEmpty()) {
                    Spacer(Modifier.height(Space.m))
                    ChoiceChipGroup(label = strings["milestones.where"]) {
                        chapters.forEach { chapter ->
                            ChoiceChip(
                                label = Bidi.isolate(chapter.name),
                                selected = draft.chapterId == chapter.id,
                                // Tapping the chosen one again clears it,
                                // because "actually I do not know where they
                                // were" has to be sayable after saying they
                                // were somewhere.
                                onClick = {
                                    draft = draft.copy(
                                        chapterId = chapter.id
                                            .takeIf { it != draft.chapterId },
                                    )
                                },
                                modifier = Modifier.testTag(
                                    AddMilestoneTags.chapter(chapter.id),
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.m))

                DictatableField(
                    label = strings["milestones.note"],
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    hint = strings["milestones.note.hint"],
                    singleLine = false,
                    imeAction = ImeAction.Done,
                    fieldTestTag = AddMilestoneTags.field("note"),
                )

                Spacer(Modifier.height(Space.xl))
            }

            Spacer(Modifier.height(Space.m))

            // **Saves what is there.** A milestone with no date is a milestone,
            // and the save never waits for the rest of it. Rule 13.
            Action(
                label = strings["capture.save"],
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddMilestoneTags.SAVE), emphasis = ActionEmphasis.Main,
            )
            Spacer(Modifier.height(Space.s))
            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier
                    .padding(horizontal = Space.screenHorizontal)
                    .testTag(AddMilestoneTags.CANCEL),
            )
            // **Only on one that already exists.** Anything added can be
            // removed, 2026-08-16; a form for a new milestone has nothing to
            // take out yet.
            if (existing != null) {
                Spacer(Modifier.height(Space.cardGap))
                Action(
                    label = strings["remove.action"],
                    onClick = onRemove,
                    modifier = Modifier
                        .padding(horizontal = Space.screenHorizontal)
                        .testTag(AddMilestoneTags.REMOVE),
                )
            }
            Spacer(Modifier.height(Space.l))
        }
    }

    if (picking) {
        DatePickerSheet(
            initial = draft.occurred,
            today = today,
            onPick = {
                draft = draft.copy(occurred = it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
