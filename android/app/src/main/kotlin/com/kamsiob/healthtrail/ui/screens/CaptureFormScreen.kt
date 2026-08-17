package com.kamsiob.healthtrail.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kamsiob.healthtrail.ui.components.FormHeader
import com.kamsiob.healthtrail.ui.components.Symbols
import com.kamsiob.healthtrail.ui.theme.hueFor
import com.kamsiob.healthtrail.ui.v4.Action
import com.kamsiob.healthtrail.ui.v4.ActionEmphasis
import com.kamsiob.healthtrail.ui.v4.BlockTone
import com.kamsiob.healthtrail.ui.v4.ChoiceChip
import com.kamsiob.healthtrail.ui.v4.ChoiceChipGroup
import com.kamsiob.healthtrail.ui.v4.DictatableField
import com.kamsiob.healthtrail.ui.v4.FactBlock
import com.kamsiob.healthtrail.ui.v4.Field
import com.kamsiob.healthtrail.ui.v4.FieldBlock
import com.kamsiob.healthtrail.ui.v4.MoreChip
import com.kamsiob.healthtrail.ui.v4.cappedChips
import java.time.LocalDate
import com.kamsiob.healthtrail.ui.components.ChipPickerSheet
import com.kamsiob.healthtrail.ui.v4.Block
import com.kamsiob.healthtrail.ui.v4.Body
import com.kamsiob.healthtrail.ui.components.StageDots
import com.kamsiob.healthtrail.ui.components.PickerOption
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object CaptureFormTags {
    const val ROOT = "capture_form_root"
    const val WHO = "capture_form_who"
    fun person(id: String) = "capture_form_person_$id"
    fun medication(id: String) = "capture_form_medication_$id"
    fun project(id: String) = "capture_form_project_$id"
    const val MORE_PROJECTS = "capture_form_more_projects"
    const val NOTE = "capture_form_note"
    const val SAVE = "capture_form_save"
    const val CANCEL = "capture_form_cancel"
    const val UNFILED_NOTE = "capture_form_unfiled_note"
    const val STAGE_DOTS = "capture_form_stages"
    const val NEXT = "capture_form_next"
    const val BACK = "capture_form_back"
    const val THREAD_UNSURE = "capture_thread_unsure"
    const val EXACT = "capture_when_exact"
    const val MORE = "capture_form_more"
    const val MORE_PEOPLE = "capture_form_more_people"
    const val MORE_THREADS = "capture_form_more_threads"
    const val MORE_MEDICATIONS = "capture_form_more_medications"
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
    /**
     * The medication a question is about, when it is about one.
     *
     * Only ever set on a question, because the other five kinds have their own
     * ways of pointing at a medication and none of them is a chip on this form.
     */
    val medicationId: String? = null,
    /**
     * The piece of work this belongs to, when it belongs to one.
     *
     * **Any kind but a measurement**, which is a reading about the person
     * rather than a step in a piece of work. #303.
     */
    val projectId: String? = null,
) {
    /**
     * Choosing a person, or clearing the one already chosen.
     *
     * Lives here rather than in the screen because the chip and the picker sheet
     * both do it, and two copies of "tapping the chosen one again clears it"
     * is how they come to disagree.
     */
    fun togglePerson(person: Repository.Person): CaptureFormState =
        if (personId == person.id) {
            copy(personId = null, who = "")
        } else {
            copy(personId = person.id, who = person.displayName)
        }

    /**
     * Who it is about, carried in from a screen that already knew, without
     * touching a word of what is already here. #371 item 7.
     *
     * **Distinct from [togglePerson] on purpose.** Toggling is the person
     * choosing a chip, so choosing the same one twice means taking it back.
     * This is context arriving from where they came from, and arriving twice
     * is not a retraction. Null leaves the draft exactly as it was, because a
     * screen that does not know who is not saying "nobody".
     */
    fun withPerson(person: Repository.Person?): CaptureFormState =
        if (person == null) this else copy(personId = person.id, who = person.displayName)

    /** The same, for the medication a question is about. */
    fun toggleMedication(medication: Repository.Medication): CaptureFormState =
        copy(medicationId = if (medicationId == medication.id) null else medication.id)

    /** And for the piece of work it belongs to. #303. */
    fun toggleProject(project: Repository.Project): CaptureFormState =
        copy(projectId = if (projectId == project.id) null else project.id)

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
                        it.medicationId ?: "",
                        it.projectId ?: "",
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
                        medicationId = (it[6] as String).takeIf { text -> text.isNotEmpty() },
                        projectId = (it[7] as String).takeIf { text -> text.isNotEmpty() },
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
    /** Set when a question was marked as being about a medication. */
    val medicationId: String? = null,
    /** Set when the entry was marked as belonging to a project. #303. */
    val projectId: String? = null,
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
    /**
     * What they are taking, offered as chips on a question only.
     *
     * `MASTER_SPEC.md` section 3 says a medication knows its pending questions.
     * Nothing wrote `question.medication_id`, so the only way to ask something
     * about a medication was to type its name into the text and hope to find it
     * again by searching. Stopped medications are offered too: plenty of
     * questions are about something she came off.
     */
    medications: List<Repository.Medication> = emptyList(),
    /**
     * The projects underway, so an entry captured anywhere can say which one it
     * is about.
     *
     * **The read path has been built since Phase 1 and nothing outside a
     * project wrote it.** `latestWordFor` reads the link and `LatestWordCard`
     * renders it, and the only writer was the log-call sheet inside a project,
     * so a call taken in a corridor about the appeal could never reach the
     * appeal. #303, and rule 18 wants it from both ends.
     */
    projects: List<Repository.Project> = emptyList(),
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
    /**
     * Which of the three questions is on screen.
     *
     * **Not stored and not remembered between openings.** Capture is a
     * conversation somebody has once, and reopening it on the stage they left
     * would be the app deciding where they are in a sentence they have not
     * started.
     */
    var stage by rememberSaveable { mutableIntStateOf(0) }
    // Which full set is open, if any. Not saveable: a sheet the person left is
    // one they closed, and reopening it under them after a rotation would be
    // the form deciding what they were doing.
    var openPicker by remember { mutableStateOf<OpenPicker?>(null) }

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
            // **The questions fade into the actions rather than being cut by
            // them.** Content scrolling behind a surface is correct and a line
            // sliced in half at the fold is not: it reads as broken rather than
            // as "there is more". The fade is the canvas color over itself, so
            // it adds no surface and no color, and it is what says the screen
            // continues. The owner, 2026-08-17, on this screen: things pushing
            // up against each other with no structure.
            Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                // Says once, in a full sentence, what the old screen said with
                // the word Optional in a mono eyebrow. Section 5.9 asks for it
                // once per screen rather than beside every field, and a sentence
                // does more of the work than a label does.
                FormHeader(
                    title = strings[key(kind, "title")],
                    // The lead is an Aside now, on the section's wash with
                    // its own icon, rather than the smallest gray line under the
                    // title. D172, and the approved medication mockup.
                    lead = null,
                    section = Repository.Section.TRAIL,
                )
                    // The title and the terms are one thing: what this screen
                    // is. They sit at the within value.
                    Spacer(Modifier.height(Space.withinGroup))
                    FactBlock(
                        label = null,
                        text = strings["capture.sub"],
                        tone = BlockTone.Section,
                        mark = Symbols.of(Repository.Section.TRAIL),
                        hue = hueFor(Repository.Section.TRAIL),
                    )

                // **The terms, then the question**, one group to the next.
                Spacer(Modifier.height(Space.betweenGroups))

                // **What happened comes first, and until 2026-08-03 it came
                // last.** The person taps this having just put a phone down,
                // and the thing in their head is what was said. It sat below
                // the who field, the care team chips, the date chips and the
                // thread chips: on a year five notebook that is twenty three
                // controls between somebody and the one sentence they came to
                // write. Rule 15 says the thing that matters most gets the
                // most weight and the best position, and this form had it
                // backwards in the same way the entry screen did.
                //
                // **The fastest path through this screen is now type and
                // save**, which is what somebody in a corridor actually does.
                // **One question at a time, per law 3.** This screen used to ask
                // four at once down one scroll: the note, the date, who, and the
                // thread, which on a year five notebook is twenty three controls
                // between somebody standing in a corridor and the thing they
                // came to write down.
                //
                // **The note is stage one and the grid puts it third.** The grid
                // draws who or what, then when, then the note. This screen kept
                // the order it arrived at on the phone, because the person taps
                // capture having just put a phone down and the thing in their
                // head is what was said. Rule 15: the thing that matters most
                // gets the best position. `DESIGN.md` 15.1 records the
                // departure.
                //
                // **Saving is live from here.** Somebody who types one sentence
                // and taps save never sees stage two or three, which is the
                // fifteen second path law 3 is written around.
                if (kind.slots[stage] == Slot.NOTE) {
                DictatableField(
                    label = strings[key(kind, "note")],
                    value = note,
                    onValueChange = { onStateChange(state.copy(note = it)) },
                    support = strings[key(kind, "note.hint")],
                    fieldTestTag = CaptureFormTags.NOTE,
                    // Grows with what is typed rather than sitting at a fixed
                    // height, because a fixed height silently teaches people to
                    // write less.
                    singleLine = false,
                    // **Law 3: voice is the biggest control on the note
                    // stage.** Somebody standing in a corridor with a phone in
                    // one hand types badly and speaks fine, and a text link
                    // beside a keyboard is not an offer they will take.
                    prominentVoice = true,
                    imeAction = ImeAction.Default,
                )

                }

                // **No spacer between the stages.** These used to separate four
                // sections down one scroll. With one question on screen they
                // were dead space at the top of stages two and three, which
                // read as a screen that had failed to load its first line.
                if (kind.slots[stage] == Slot.WHEN) {
                ChoiceChipGroup(
                    // **A question has not happened yet.** Asking "when" about
                    // one, in the same words used for a call that already
                    // took place, is the app asking the person to date
                    // something they have not done. It asks when they mean to
                    // ask it instead, and the answer's own date is stamped
                    // when the answer arrives. #379, the owner's words: "it
                    // asked me to put in a date which is weird because I
                    // haven't asked the question yet."
                    label = if (kind == CaptureKind.QUESTION) {
                        strings["capture.when.question"]
                    } else {
                        strings["capture.when"]
                    },
                    aside = if (kind == CaptureKind.QUESTION) {
                        strings["capture.when.question.hint"]
                    } else {
                        strings["capture.when.hint"]
                    },
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

                }

                if (kind.slots[stage] == Slot.WHO) {
                Field(
                    label = strings[key(kind, "who")],
                    value = who,
                    onValueChange = { onStateChange(state.copy(who = it)) },
                    fieldTestTag = CaptureFormTags.WHO,
                    support = strings[key(kind, "who.hint")],
                )

                // **The care team, offered rather than retyped, and capped at
                // five.**
                //
                // Part Two's first rule: anywhere the set of possible answers
                // is knowable, offer chips rather than a text field, and people
                // are named first in that list. Somebody in a hallway who has
                // already added the charge nurse should not type her name
                // again, and typing it again is also how a person ends up with
                // four spellings of one nurse across six months.
                //
                // **The cap is 5.11.1 and the ordering is the query's.**
                // `peopleByRecentUse` puts whoever the person has been dealing
                // with lately in front, so the five are the likely five rather
                // than the first five ever added. The rest are one tap away
                // with search, and the chosen one is always among the five even
                // when it would fall outside, because a set that hides the
                // answer already given is lying about the state of the form.
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
                    val chosenPerson = people.firstOrNull { it.id == state.personId }
                    val shownPeople = cappedChips(people, chosenPerson)
                    // **Closer to the field than to whatever follows**, because
                    // these are a way of answering that field rather than a
                    // second question. Air is what says which things belong
                    // together, and at the same gap as everything else it read
                    // as a new section. Rule 15, D188.
                    Spacer(Modifier.height(Space.withinGroup))
                    // **The shortcuts are a hint under the field, not a
                    // section of their own.** The owner, 2026-08-17: the gap
                    // above them and the gap below them said two different
                    // things, and "it's not just a spacing issue, it's a visual
                    // structure that's still kind of absent." The label was set
                    // as a heading and sat eight points under a field, so the
                    // type said "new section" while the air said "same thing".
                    // These fill that field in: they get the field's own
                    // supporting line and nothing louder.
                    //
                    // **A reader still hears the set named**, which is what
                    // `showLabel` is for: the words stay at the call site and
                    // in the semantics.
                    ChoiceChipGroup(
                        label = strings["capture.who.known"],
                        // **One supporting line, not two.** The field's own
                        // says what to type; a second gray line under it
                        // saying the chips can be tapped is the blending the
                        // owner named, and a row of names beside a field
                        // already says what tapping one does. D189.
                        showLabel = false,
                    ) {
                        shownPeople.forEach { person ->
                            ChoiceChip(
                                // A name is the person's own words. #226.
                                label = Bidi.isolate(person.displayName),
                                selected = state.personId == person.id,
                                onClick = { onStateChange(state.togglePerson(person)) },
                                modifier = Modifier.testTag(
                                    CaptureFormTags.person(person.id),
                                ),
                            )
                        }
                        if (people.size > shownPeople.size) {
                            MoreChip(
                                label = strings("chips.all", "count" to people.size),
                                onClick = { openPicker = OpenPicker.PEOPLE },
                                modifier = Modifier.testTag(CaptureFormTags.MORE_PEOPLE),
                            )
                        }
                    }
                }

                // **One group to the next**, which is the between value. The
                // zone value is spent once, on the band that separates asking
                // from acting, and spending it twice is what made the rhythm
                // read as arbitrary. D188.
                Spacer(Modifier.height(Space.betweenGroups))

                // **Everything below here is behind one control nobody has to
                // touch**, per 10.8 and the disclosure in `Disclosure.kt`.
                // What is left out here is what the app can work out or live
                // without: an entry with no thread lands in the Unfiled tray,
                // which is a built screen that suggests a home and asks for one
                // tap, and a question about nothing in particular is an
                // ordinary question.
                val hasThreads = threads.isNotEmpty()
                val hasMedications =
                    kind == CaptureKind.QUESTION && medications.isNotEmpty()
                // **Every kind this form handles, unlike the medication
                // chips.** A call, a visit, something that went wrong and a
                // question can all belong to a piece of work. This form is the
                // four of them, per its own heading: a measurement and a
                // document are captured elsewhere, so there is no kind to
                // exclude here. A guard against `MEASUREMENT` was written first
                // and is a condition that can never be false, which a test
                // trying to prove it found immediately.
                val hasProjects = projects.isNotEmpty()

                if (hasThreads || hasMedications || hasProjects) {
                    // **The rest of the form is a group with a label, not a fold.**
                    // D185: nothing sits behind a fold that a label and a scroll can
                    // carry, and the sentence that used to explain the fold is the
                    // group's own line now. Nothing here was ever required.
                    FieldBlock(
                        label = strings["capture.more"],
                        aside = strings["capture.more.aside"],
                        modifier = Modifier.testTag(CaptureFormTags.MORE),
                    ) {
                        Column {
                            if (hasThreads) {
                                val chosenThread = threads.firstOrNull { it.id == threadId }
                                val shownThreads = cappedChips(threads, chosenThread)
                                ChoiceChipGroup(
                                    label = strings["capture.thread"],
                                    aside = strings["capture.thread.hint"],
                                ) {
                                    shownThreads.forEach { thread ->
                                        ChoiceChip(
                                            label = Bidi.isolate(thread.label),
                                            selected = threadId == thread.id,
                                            onClick = {
                                                onStateChange(state.copy(threadId = thread.id))
                                            },
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
                                    if (threads.size > shownThreads.size) {
                                        MoreChip(
                                            label = strings("chips.all", "count" to threads.size),
                                            onClick = { openPicker = OpenPicker.THREADS },
                                            modifier = Modifier
                                                .testTag(CaptureFormTags.MORE_THREADS),
                                        )
                                    }
                                }
                            }

                            // **What it is about**, on a question only.
                            //
                            // `MASTER_SPEC.md` section 3 promises a medication
                            // knows its pending questions, and
                            // `question.medication_id` had no writer, so the
                            // only way to ask something about a medication was
                            // to type its name into the text. That question
                            // then lived nowhere: not on the medication, not on
                            // a prep sheet for the person who prescribed it,
                            // findable only by searching for a drug name
                            // somebody may have spelled differently.
                            //
                            // **Optional, like everything else on this form.**
                            // Plenty of questions are about nothing in
                            // particular, and rule 13 says an unfilled slot
                            // reads as "not yet".
                            if (hasMedications) {
                                if (hasThreads) Spacer(Modifier.height(Space.sectionGap))
                                val chosenMedication =
                                    medications.firstOrNull { it.id == state.medicationId }
                                val shownMedications =
                                    cappedChips(medications, chosenMedication)
                                ChoiceChipGroup(
                                    label = strings["capture.about.medication"],
                                    aside = strings["capture.about.medication.aside"],
                                ) {
                                    shownMedications.forEach { medication ->
                                        ChoiceChip(
                                            label = Bidi.isolate(medication.name),
                                            selected = state.medicationId == medication.id,
                                            onClick = {
                                                onStateChange(state.toggleMedication(medication))
                                            },
                                            modifier = Modifier.testTag(
                                                CaptureFormTags.medication(medication.id),
                                            ),
                                        )
                                    }
                                    if (medications.size > shownMedications.size) {
                                        MoreChip(
                                            label = strings(
                                                "chips.all",
                                                "count" to medications.size,
                                            ),
                                            onClick = { openPicker = OpenPicker.MEDICATIONS },
                                            modifier = Modifier
                                                .testTag(CaptureFormTags.MORE_MEDICATIONS),
                                        )
                                    }
                                }
                            }

                            // **Which piece of work this belongs to.** The
                            // link table has held it since Phase 1, the
                            // project's own card reads it as the latest word,
                            // and the only thing that ever wrote one was the
                            // sheet inside a project: so a call taken in a
                            // corridor about the appeal reached the trail and
                            // never reached the appeal. #303.
                            //
                            // **Optional like everything else here**, and off
                            // by default: most of what a person writes down
                            // belongs to no project at all.
                            if (hasProjects) {
                                if (hasThreads || hasMedications) {
                                    Spacer(Modifier.height(Space.sectionGap))
                                }
                                val chosenProject =
                                    projects.firstOrNull { it.id == state.projectId }
                                val shownProjects = cappedChips(projects, chosenProject)
                                ChoiceChipGroup(
                                    label = strings["capture.about.project"],
                                    aside = strings["capture.about.project.aside"],
                                ) {
                                    shownProjects.forEach { project ->
                                        ChoiceChip(
                                            label = Bidi.isolate(project.name),
                                            selected = state.projectId == project.id,
                                            onClick = {
                                                onStateChange(state.toggleProject(project))
                                            },
                                            modifier = Modifier.testTag(
                                                CaptureFormTags.project(project.id),
                                            ),
                                        )
                                    }
                                    if (projects.size > shownProjects.size) {
                                        MoreChip(
                                            label = strings(
                                                "chips.all",
                                                "count" to projects.size,
                                            ),
                                            onClick = { openPicker = OpenPicker.PROJECTS },
                                            modifier = Modifier
                                                .testTag(CaptureFormTags.MORE_PROJECTS),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Said on the screen rather than discovered afterward. An entry
                // that quietly went somewhere the person did not choose is the
                // thing this app promises never to do, so the screen says where
                // it is going while they can still change it.
                //
                // **It stays outside the disclosure deliberately.** The thread
                // question is now folded away, so this sentence is the only
                // thing telling somebody who never opens it where their entry
                // is going, and it sits directly under the control that would
                // let them change it.
                if (threads.isNotEmpty() && threadId == null) {
                    Spacer(Modifier.height(Space.withinGroup))
                    // **A footnote to the control above it, not a block of its
                    // own.** It was tried as a block and that made three sand
                    // surfaces stacked down one screen, which is the blending
                    // the owner named rather than a fix for it: two of them
                    // were content and one was the actions, and they all
                    // weighed the same. At the smallest size directly under the
                    // disclosure it reads as a note about that control, which
                    // is what it is. Rule 15, and seen on the phone twice.
                    // bidi-ok: the app's own sentence about its own filing.
                    Body(
                        text = strings["capture.unfiled.note"],
                        style = HealthTrail.type.bodyS,
                        modifier = Modifier.testTag(CaptureFormTags.UNFILED_NOTE),
                    )
                }
                }

                // **Room under the last thing, so the band cuts empty canvas
                // rather than a sentence.** Content scrolling behind a surface
                // is correct; content sliced mid word by it reads as broken.
                Spacer(Modifier.height(Space.xl))
            }

            Spacer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(Space.l)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, HealthTrail.colors.paper),
                        ),
                    ),
            )
            }

            // The gap the pinned action footer requires, per DESIGN.md 5.15.
            // Without it the content at the scroll edge ends against the button
            // and reads as an overlap, which is what it did here at the largest
            // system font.
            Spacer(Modifier.height(Space.m))

            // **The actions stand on their own tonal band**, which is what
            // `m3v4-4` draws under Save and what the owner asked for on this
            // screen, 2026-08-17: "it's hard to know where to look or what to
            // focus on at any given time." The band splits the screen into the
            // part that asks and the part that acts, so the questions scroll
            // behind a surface rather than running into the buttons. It is the
            // language's own container, not a new costume.
            Block {

            // **Where you are, and the way on, on one line.** Law 3 asks for
            // progress dots and a skip that is always visible. The dots say
            // where somebody is and never how much is left, per rule 13, and
            // the way on is worded as skipping rather than advancing because
            // every one of these questions is optional and the button should
            // say so.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StageDots(
                    count = STAGES,
                    current = stage,
                    description = strings(
                        "capture.stage",
                        "current" to stage + 1,
                        "total" to STAGES,
                    ),
                    modifier = Modifier.testTag(CaptureFormTags.STAGE_DOTS),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stage > 0) {
                        Action(
                            label = strings["capture.back"],
                            onClick = { stage -= 1 },
                            modifier = Modifier.testTag(CaptureFormTags.BACK),
                        )
                        Spacer(Modifier.width(Space.m))
                    }
                    if (stage < STAGES - 1) {
                        // **"Skip this" while the stage is empty, "Next" once
                        // it has something.** Nothing in capture is required
                        // and a control that says Next implies the question
                        // behind it must be answered first, which is why the
                        // default is Skip. **But once somebody has typed a
                        // line, "Skip this" describes something they are not
                        // doing**, which is the oddness `DESIGN.md` 15.1
                        // raised on #345 and which the catalogs were already
                        // carrying `capture.next` for, unused, in all four
                        // languages. Rule 10: decided here rather than
                        // escalated.
                        val filled = when (kind.slots[stage]) {
                            Slot.NOTE -> state.note.isNotBlank()
                            Slot.WHEN -> state.pickedEdtf != null || state.rough != null
                            Slot.WHO -> state.who.isNotBlank() ||
                                state.threadId != null ||
                                state.personId != null ||
                                state.medicationId != null
                        }
                        Action(
                            label = strings[if (filled) "capture.next" else "capture.skip"],
                            onClick = { stage += 1 },
                            modifier = Modifier.testTag(CaptureFormTags.NEXT),
                        )
                    }
                }
            }

            // **Live from stage one.** Somebody who types one sentence and taps
            // save never sees the other two questions, which is the fifteen
            // second path law 3 is written around. It saves whatever is filled
            // in, from wherever they are.
            Action(
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
                            // No such caveat here: the chip is the only way to
                            // set this and nothing else on the form can
                            // contradict it.
                            medicationId = state.medicationId,
                            projectId = state.projectId,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(CaptureFormTags.SAVE), emphasis = ActionEmphasis.Main,
            )

            // **Sized to its label, not the width of the screen.** D137: a
            // full width outlined bar is the way back and nothing else, and
            // under a full width filled action it is a second bar of which
            // only one leaves. #371 item 5, and it is retroactive per rule 14.
            Action(
                label = strings["common.cancel"],
                onClick = onCancel,
                modifier = Modifier.testTag(CaptureFormTags.CANCEL),
            )
            }
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

    // **The full set, when the person asks for it**, per 5.11.1. One sheet
    // reused three times rather than three sheets, because it is the same
    // question every time: which one of these, out of more than fit in a row.
    when (openPicker) {
        OpenPicker.PEOPLE -> ChipPickerSheet(
            title = strings["capture.who.known"],
            options = people.map {
                PickerOption(
                    id = it.id,
                    label = Bidi.isolate(it.displayName),
                    detail = it.roleLabel?.let { role -> Bidi.isolate(role) },
                )
            },
            selectedId = state.personId,
            onPick = { option ->
                openPicker = null
                people.firstOrNull { it.id == option.id }
                    ?.let { onStateChange(state.togglePerson(it)) }
            },
            onDismiss = { openPicker = null },
        )
        OpenPicker.THREADS -> ChipPickerSheet(
            title = strings["capture.thread"],
            options = threads.map { thread ->
                PickerOption(
                    id = thread.id,
                    label = Bidi.isolate(thread.label),
                    routeColor = colors.threadRoutes[
                        thread.colorIndex.mod(colors.threadRoutes.size),
                    ],
                    routeIndex = thread.colorIndex,
                )
            },
            selectedId = threadId,
            onPick = { option ->
                openPicker = null
                onStateChange(state.copy(threadId = option.id))
            },
            onDismiss = { openPicker = null },
        )
        OpenPicker.MEDICATIONS -> ChipPickerSheet(
            title = strings["capture.about.medication"],
            options = medications.map { PickerOption(id = it.id, label = Bidi.isolate(it.name)) },
            selectedId = state.medicationId,
            onPick = { option ->
                openPicker = null
                medications.firstOrNull { it.id == option.id }
                    ?.let { onStateChange(state.toggleMedication(it)) }
            },
            onDismiss = { openPicker = null },
        )
        OpenPicker.PROJECTS -> ChipPickerSheet(
            title = strings["capture.about.project"],
            options = projects.map { PickerOption(id = it.id, label = Bidi.isolate(it.name)) },
            selectedId = state.projectId,
            onPick = { option ->
                openPicker = null
                projects.firstOrNull { it.id == option.id }
                    ?.let { onStateChange(state.toggleProject(it)) }
            },
            onDismiss = { openPicker = null },
        )
        null -> Unit
    }
}

/**
 * Which full set the person opened, if any.
 *
 * Named cases rather than a boolean per group, so another capped group cannot
 * be added without deciding what happens when two are open at once. The answer
 * is that they never are.
 */
private enum class OpenPicker { PEOPLE, THREADS, MEDICATIONS, PROJECTS }

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
/**
 * How many questions capture asks. Three, which is law 3's maximum.
 *
 * **A fourth would be a form.** The disclosure inside the third stage holds the
 * thread and the medication, which are what the app can work out or live
 * without, and they stay behind one control nobody has to touch.
 */
/**
 * What each stage of the form asks, and in what order for each kind.
 *
 * **A call starts with who you spoke to.** The owner's direction, #379: "for
 * logging a call it should start off with who did you speak to and then notes
 * about the call and an ability to connect it to anything else that I want ...
 * and then the date." That is how the conversation actually went: a name, then
 * what they said, then what it was about, then when.
 *
 * **Everything else keeps the order it had**, because for an incident or a
 * visit the note is the thing somebody opened the app to write, and the
 * fifteen second path law 3 is built around is typing one sentence and saving.
 */
private enum class Slot { NOTE, WHEN, WHO }

private val CaptureKind.slots: List<Slot>
    get() = when (this) {
        CaptureKind.CALL -> listOf(Slot.WHO, Slot.NOTE, Slot.WHEN)
        else -> listOf(Slot.NOTE, Slot.WHEN, Slot.WHO)
    }

private const val STAGES = 3

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
