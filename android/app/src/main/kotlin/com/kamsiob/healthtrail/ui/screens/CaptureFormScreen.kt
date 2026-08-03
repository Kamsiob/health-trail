package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import java.time.LocalDate
import com.kamsiob.healthtrail.ui.components.ChoiceChip
import com.kamsiob.healthtrail.ui.components.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.DictatableField
import com.kamsiob.healthtrail.ui.components.HealthTrailTextField
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object CaptureFormTags {
    const val ROOT = "capture_form_root"
    const val WHO = "capture_form_who"
    fun person(id: String) = "capture_form_person_$id"
    const val NOTE = "capture_form_note"
    const val SAVE = "capture_form_save"
    const val CANCEL = "capture_form_cancel"
    const val UNFILED_NOTE = "capture_form_unfiled_note"
    const val THREAD_UNSURE = "capture_thread_unsure"
    const val EXACT = "capture_when_exact"
    const val PICKED = "capture_when_picked"
    fun whenChip(rough: RoughWhen) = "capture_when_${rough.name.lowercase()}"
    fun threadChip(id: String) = "capture_thread_$id"
}

/**
 * How roughly a person remembers when something happened.
 *
 * **Rough is the default, not the fallback.** Someone writing at 11pm about a
 * call three days ago does not know the time, and a date picker gets either a
 * guess recorded as fact or nothing recorded at all. Four answers, and one of
 * them is not knowing.
 */
enum class RoughWhen { TODAY, YESTERDAY, THIS_WEEK, NOT_SURE }

/** What a captured entry carries. Every part of it can be empty. */
/**
 * A capture form part way through being filled in.
 *
 * **This exists so that leaving the form does not throw the note away.**
 * `MASTER_SPEC.md` calls the capture form the thing somebody reaches for while
 * a nurse is still talking. Losing a half written note from a hospital corridor
 * is the worst thing this app could do short of losing the notebook, and until
 * this existed the form held everything in a local `remember`: a back press, a
 * rotation, or the system reclaiming memory took the lot.
 *
 * Held above the form by `NotebookShell` in a `rememberSaveable`, so it
 * survives back, rotation, a theme change, and process death.
 *
 * **Never shown as a warning and never blocking.** Nothing tells the person
 * they have an unfinished entry, nothing asks them to confirm leaving, and
 * nothing counts how complete it is. It is simply still there. Rule 13.
 */
data class CaptureFormState(
    val who: String = "",
    val note: String = "",
    val rough: RoughWhen? = RoughWhen.TODAY,
    /** The EDTF string of a date chosen from the calendar, or null. */
    val pickedEdtf: String? = null,
    val threadId: String? = null,
    /**
     * The person this involved, when they are somebody already on the care
     * team. Null when the person typed a name instead, which is ordinary.
     */
    val personId: String? = null,
) {
    /** True when there is something in here worth keeping. */
    val isEmpty: Boolean
        get() = who.isBlank() && note.isBlank() && pickedEdtf == null && threadId == null

    companion object {
        /**
         * Saved as a flat list of strings, which is what the bundle can carry.
         *
         * A hand written saver rather than serialization, because the bundle is
         * the one place this app's data leaves the encrypted database, and what
         * goes into it should be short, obvious, and read at a glance. It holds
         * an unsaved note, which is the person's words: nothing else about them
         * belongs here.
         */
        val Saver: androidx.compose.runtime.saveable.Saver<CaptureFormState, Any> =
            androidx.compose.runtime.saveable.listSaver(
                save = {
                    listOf(
                        it.who,
                        it.note,
                        it.rough?.name ?: "",
                        it.pickedEdtf ?: "",
                        it.threadId ?: "",
                        it.personId ?: "",
                    )
                },
                restore = {
                    CaptureFormState(
                        who = it[0] as String,
                        note = it[1] as String,
                        rough = (it[2] as String).takeIf { name -> name.isNotEmpty() }
                            ?.let { name -> RoughWhen.valueOf(name) },
                        pickedEdtf = (it[3] as String).takeIf { text -> text.isNotEmpty() },
                        threadId = (it[4] as String).takeIf { text -> text.isNotEmpty() },
                        personId = (it[5] as String).takeIf { text -> text.isNotEmpty() },
                    )
                },
            )
    }
}

data class CaptureDraft(
    val kind: CaptureKind,
    val who: String,
    val note: String,
    /**
     * When it happened, at exactly the precision the person gave. A chip
     * answer and a date picked from the calendar arrive here the same way,
     * because by this point the difference between them is not interesting.
     */
    val occurred: Edtf.Date,
    /** Null means the person did not say, which sends the entry to the Unfiled tray. */
    val threadId: String?,
    /** Set when who was chosen from the care team rather than typed. */
    val personId: String? = null,
)

/**
 * One form for four of the six capture inputs: a call, a visit, an incident, and
 * a question.
 *
 * **One form rather than four, deliberately.** All four record who, when, what
 * it is part of, and what happened. Building them separately would put the same
 * pattern on screen in four slightly different shapes, which `DESIGN.md` section
 * 10.2 calls a defect. The shape is defined once and the words come from the
 * catalog per kind, so a person who has logged a call has learned how to log a
 * visit.
 *
 * Measurement and document are genuinely different shapes, a value with a unit
 * and a photograph, and they get their own screens rather than being forced
 * through this one.
 *
 * **This is screen 26 of the reference file, "capture that forgives".** An
 * earlier version was two single line text fields. It worked, and it was not
 * what was designed: rough date chips, thread chips, an open note area, and a
 * save action that accepts whatever is there. Recorded as D30.
 *
 * **Every field is optional and saving with all of them untouched is allowed.**
 * A person who hangs up and taps this has already done the useful thing, which
 * is recording that something happened and roughly when. Partial is a finished
 * state.
 *
 * **The thread question defaults to not knowing**, which sends the entry to the
 * Unfiled tray. That is the honest default for someone who just tapped save, and
 * the note underneath says where it is going while they can still change it.
 *
 * Composed from Display L, Body M, Body S, the text field from section 5.9, the
 * choice chip from section 5.11, one filled button, and one text action.
 */
@Composable
fun CaptureFormScreen(
    kind: CaptureKind,
    threads: List<Repository.CareThread>,
    /**
     * The care team, offered as chips so a name already recorded is not typed
     * again. Empty in the first days, which is the normal beginning.
     */
    people: List<Repository.Person> = emptyList(),
    onSave: (CaptureDraft) -> Unit,
    onCancel: () -> Unit,
    /**
     * What was typed last time, which is usually nothing.
     *
     * **Held above this screen so that leaving does not discard it.** See
     * [CaptureFormState].
     */
    state: CaptureFormState,
    /**
     * **Required, with no default.** A no-op default made the form silently
     * dead: every field is controlled by [state], so a caller that did not hoist
     * got a form nothing could be typed into, and it looked completely normal.
     * `CaptureTest` found it that way within a minute. A component that does
     * nothing when used with its defaults is worse than one that will not
     * compile.
     */
    onStateChange: (CaptureFormState) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = HealthTrail.colors

    // Read from the hoisted state and written straight back, so the caller is
    // always holding current truth rather than a copy taken when the screen
    // opened.
    val who = state.who
    val note = state.note
    val rough = state.rough
    val threadId = state.threadId
    // A date chosen from the calendar. Set means the chips are no longer the
    // answer, and the two can never both be the answer at once.
    val picked = state.pickedEdtf?.let { Edtf.parse(it) }
    var pickerOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag(CaptureFormTags.ROOT),
        color = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screenHorizontal, vertical = Space.l),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = strings[key(kind, "title")],
                    style = HealthTrail.type.displayL,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(Modifier.height(Space.s))

                // Says once, in a full sentence, what the old screen said with
                // the word Optional in a mono eyebrow. Section 5.9 asks for it
                // once per screen rather than beside every field, and a sentence
                // does more of the work than a label does.
                Text(
                    text = strings["capture.sub"],
                    style = HealthTrail.type.bodyM,
                    color = colors.ink2,
                )

                Spacer(Modifier.height(Space.l))

                HealthTrailTextField(
                    label = strings[key(kind, "who")],
                    value = who,
                    onValueChange = { onStateChange(state.copy(who = it)) },
                    hint = strings[key(kind, "who.hint")],
                    fieldTestTag = CaptureFormTags.WHO,
                )

                // **The care team, offered rather than retyped.**
                //
                // Part Two's first rule: anywhere the set of possible answers
                // is knowable, offer chips rather than a text field, and people
                // are named first in that list. Somebody in a hallway who has
                // already added the charge nurse should not type her name
                // again, and typing it again is also how a person ends up with
                // four spellings of one nurse across six months.
                //
                // **It also gives the app the link it never had.** Choosing a
                // chip writes `entry_person`, which has been in the schema
                // since Phase 0 with nothing writing to it, and which is what
                // `MASTER_SPEC.md` section 3 means by a person knowing every
                // call and visit involving them.
                //
                // **The field stays and is still the answer.** Somebody who
                // spoke to a person nobody has added yet types their name, and
                // that is the common case in the first weeks. A chip fills the
                // field and can be cleared by tapping again.
                if (people.isNotEmpty()) {
                    Spacer(Modifier.height(Space.m))
                    ChoiceChipGroup(
                        label = strings["capture.who.known"],
                        aside = strings["capture.who.known.aside"],
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                            verticalArrangement = Arrangement.spacedBy(Space.s),
                        ) {
                            people.forEach { person ->
                                ChoiceChip(
                                    label = person.displayName,
                                    selected = state.personId == person.id,
                                    onClick = {
                                        onStateChange(
                                            if (state.personId == person.id) {
                                                state.copy(personId = null, who = "")
                                            } else {
                                                state.copy(
                                                    personId = person.id,
                                                    who = person.displayName,
                                                )
                                            },
                                        )
                                    },
                                    modifier = Modifier.testTag(
                                        CaptureFormTags.person(person.id),
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Space.sectionGap))

                ChoiceChipGroup(
                    label = strings["capture.when"],
                    aside = strings["capture.when.hint"],
                ) {
                    RoughWhen.entries.forEach { option ->
                        ChoiceChip(
                            label = strings[option.labelKey],
                            selected = picked == null && rough == option,
                            onClick = { onStateChange(state.copy(rough = option, pickedEdtf = null)) },
                            modifier = Modifier.testTag(CaptureFormTags.whenChip(option)),
                        )
                    }
                    // **A peer of the chips, not something behind them.**
                    // Section 10.9. Someone logging a call from three months
                    // ago, or who knows the minute, is a normal case rather
                    // than an edge one, and making them exhaust the chips
                    // first would say otherwise.
                    ChoiceChip(
                        label = strings["capture.when.exact"],
                        selected = picked != null,
                        onClick = { pickerOpen = true },
                        modifier = Modifier.testTag(CaptureFormTags.EXACT),
                    )
                }

                // What was chosen, read back in words. The person sees the
                // claim they are about to make rather than a control state.
                picked?.let { chosen ->
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = EventDateText.render(strings, chosen),
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                        modifier = Modifier.testTag(CaptureFormTags.PICKED),
                    )
                }

                // Only asked where there is something to answer with. A notebook
                // with no situation template has no threads, and a question with
                // one possible answer is not a question.
                if (threads.isNotEmpty()) {
                    Spacer(Modifier.height(Space.sectionGap))

                    ChoiceChipGroup(
                        label = strings["capture.thread"],
                        aside = strings["capture.thread.hint"],
                    ) {
                        threads.forEach { thread ->
                            ChoiceChip(
                                label = thread.label,
                                selected = threadId == thread.id,
                                onClick = { onStateChange(state.copy(threadId = thread.id)) },
                                dotColor = colors.threadRoutes[
                                    thread.colorIndex.mod(colors.threadRoutes.size),
                                ],
                                modifier = Modifier
                                    .testTag(CaptureFormTags.threadChip(thread.id)),
                            )
                        }
                        ChoiceChip(
                            label = strings["capture.thread.not_sure"],
                            selected = threadId == null,
                            onClick = { onStateChange(state.copy(threadId = null)) },
                            modifier = Modifier.testTag(CaptureFormTags.THREAD_UNSURE),
                        )
                    }
                }

                Spacer(Modifier.height(Space.sectionGap))

                DictatableField(
                    label = strings[key(kind, "note")],
                    value = note,
                    onValueChange = { onStateChange(state.copy(note = it)) },
                    hint = strings[key(kind, "note.hint")],
                    fieldTestTag = CaptureFormTags.NOTE,
                    // Grows with what is typed rather than sitting at a fixed
                    // height, because a fixed height silently teaches people to
                    // write less.
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )

                // Said on the screen rather than discovered afterward. An entry
                // that quietly went somewhere the person did not choose is the
                // thing this app promises never to do, so the screen says where
                // it is going while they can still change it.
                if (threads.isNotEmpty() && threadId == null) {
                    Spacer(Modifier.height(Space.m))
                    Text(
                        text = strings["capture.unfiled.note"],
                        style = HealthTrail.type.bodyS,
                        color = colors.ink2,
                        modifier = Modifier.testTag(CaptureFormTags.UNFILED_NOTE),
                    )
                }

                Spacer(Modifier.height(Space.l))
            }

            // The gap the pinned action footer requires, per DESIGN.md 5.15.
            // Without it the content at the scroll edge ends against the button
            // and reads as an overlap, which is what it did here at the largest
            // system font.
            Spacer(Modifier.height(Space.m))

            FilledButton(
                label = strings["capture.save"],
                onClick = {
                    onSave(
                        CaptureDraft(
                            kind = kind,
                            who = who.trim(),
                            note = note.trim(),
                            // The calendar wins when it was used, and the chip
                            // otherwise. Nothing chosen at all is unknown,
                            // which is a real answer rather than a blank.
                            occurred = picked
                                ?: rough?.edtf(LocalDate.now())
                                ?: Edtf.unknown(),
                            threadId = threadId,
                            // **Only when the name still matches the chip.** A
                            // person who tapped a chip and then edited the name
                            // meant the edited name, so the link goes rather
                            // than quietly attaching the entry to somebody the
                            // words no longer say.
                            personId = state.personId?.takeIf { chosen ->
                                people.firstOrNull { it.id == chosen }
                                    ?.displayName == who.trim()
                            },
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.SAVE),
            )

            Spacer(Modifier.height(Space.s))

            TextAction(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.CANCEL),
            )
        }
    }

    if (pickerOpen) {
        DatePickerSheet(
            // Opens on whatever is already chosen, so confirming without
            // touching anything changes nothing.
            initial = picked ?: rough?.edtf(LocalDate.now()),
            onPick = { chosen ->
                pickerOpen = false
                if (chosen.precision == Edtf.Precision.UNKNOWN) {
                    // "I am not sure" from inside the picker is the same answer
                    // as the chip, so it lands on the chip rather than leaving
                    // two controls saying different things.
                    onStateChange(state.copy(pickedEdtf = null, rough = RoughWhen.NOT_SURE))
                } else {
                    onStateChange(state.copy(pickedEdtf = chosen.canonical, rough = null))
                }
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

/** The catalog key for a rough date answer. */
/**
 * The catalog key for a rough date chip.
 *
 * Public because the measurement screen asks the same question with the same
 * chips, and two screens mapping the same enum to two sets of words would be
 * the defect `DESIGN.md` section 10.2 names.
 */
val RoughWhen.labelKey: String
    get() = when (this) {
        RoughWhen.TODAY -> "capture.when.today"
        RoughWhen.YESTERDAY -> "capture.when.yesterday"
        RoughWhen.THIS_WEEK -> "capture.when.this_week"
        RoughWhen.NOT_SURE -> "capture.when.not_sure"
    }

/**
 * A rough answer turned into the date the schema actually stores.
 *
 * **The chip says exactly as much as the person did, and no more.** "Today" is
 * a day, so it stores a day and not the minute they happened to tap the button.
 * "Sometime this week" is a week, so it stores the week as the interval a week
 * is. **"Not sure" stores unknown**, rather than today's date with a shrug
 * attached, and every screen downstream renders it as not known because that is
 * what it says.
 *
 * Contract section 3.1 and `Edtf`.
 */
fun RoughWhen.edtf(today: LocalDate): Edtf.Date = when (this) {
    RoughWhen.TODAY -> Edtf.day(today)
    RoughWhen.YESTERDAY -> Edtf.day(today.minusDays(1))
    RoughWhen.THIS_WEEK -> Edtf.week(today)
    RoughWhen.NOT_SURE -> Edtf.unknown()
}

/**
 * Whether this kind is one of the four the shared form serves.
 *
 * **Declared in one place, and exhaustively.** Adding a seventh capture kind
 * will not compile until it answers this question, which is the whole point: a
 * kind that quietly defaulted to using the form would render raw catalog keys
 * on screen, and a kind that quietly defaulted to not using it would open
 * nothing at all when tapped. Both are silent, so neither is left to a default.
 *
 * Measurement and document say no. A measurement carries a value and a unit and
 * a document carries a photograph, and neither fits a form made of free text and
 * chips.
 */
val CaptureKind.usesTheSharedForm: Boolean
    get() = when (this) {
        CaptureKind.CALL, CaptureKind.VISIT, CaptureKind.INCIDENT, CaptureKind.QUESTION -> true
        CaptureKind.MEASUREMENT, CaptureKind.DOCUMENT -> false
    }

/**
 * The catalog key for one kind and one slot.
 *
 * Built rather than looked up in a table, so adding a kind means adding its
 * strings to the catalog and nothing else. `check_i18n.py` fails the build if
 * any locale is missing one, and `CaptureTest` fails if a kind the form serves
 * has no words, which is what makes building a key safe.
 */
private fun key(kind: CaptureKind, slot: String): String =
    "capture.${kind.name.lowercase()}.$slot"

/**
 * The row kind stored on the entry, which is what the trail and every filter
 * read. Kept next to the form so a new capture kind cannot be added without
 * deciding what it is called in the schema.
 */
fun CaptureKind.entryKind(): String = when (this) {
    CaptureKind.CALL -> "call"
    CaptureKind.VISIT -> "visit"
    CaptureKind.INCIDENT -> "incident"
    CaptureKind.MEASUREMENT -> "measurement"
    CaptureKind.QUESTION -> "question"
    CaptureKind.DOCUMENT -> "document"
}
