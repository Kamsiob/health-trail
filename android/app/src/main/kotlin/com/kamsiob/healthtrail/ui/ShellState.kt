package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.screens.AppointmentDraft
import com.kamsiob.healthtrail.ui.screens.BillDraft
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.DocumentDraft
import com.kamsiob.healthtrail.ui.screens.EmergencyDraft
import com.kamsiob.healthtrail.ui.screens.EntryCorrection
import com.kamsiob.healthtrail.ui.screens.ExportState
import com.kamsiob.healthtrail.ui.screens.IncidentCorrection
import com.kamsiob.healthtrail.ui.screens.MeasurementDraft
import com.kamsiob.healthtrail.ui.screens.MedicationDraft
import com.kamsiob.healthtrail.ui.screens.MedicationEventDraft
import com.kamsiob.healthtrail.ui.screens.MilestoneDraft
import com.kamsiob.healthtrail.ui.screens.PersonDraft
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.SituationChange
import com.kamsiob.healthtrail.ui.screens.SubjectCorrection
import com.kamsiob.healthtrail.ui.screens.ViolationDraft

/**
 * Every piece of screen state the notebook shell holds, in one object.
 *
 * **This exists because of a hard platform ceiling, not because of taste.**
 * `NotebookShell` reached the JVM's 64KB limit on a single method, and after
 * that no screen could gain a parameter and no overlay could be added. The 214
 * `var x by remember { mutableStateOf(...) }` declarations that used to open
 * the composable each emitted a lambda class, a `remember` call, a slot read
 * and a delegate setup **inside the method that was at the ceiling**. Almost
 * every one of them held a plain literal: `null`, `false`, `emptyList()`, an
 * enum constant. Constructing them once, here, costs the shell a single
 * `remember`. `DECISIONS.md` B6 carries the four attempts that failed first,
 * and the one shape that has ever paid: fewer arguments, not more files.
 *
 * **The shell wraps its body in `with(ui) { }`**, so not one of the 214
 * references had to be rewritten. Renaming 214 identifiers across 4,700 lines
 * by pattern is how a session breaks something that compiles.
 *
 * **Three pieces of state are deliberately not here.** `captureDraft`,
 * `setupOpen` and `searchQuery` stay in the composable as `rememberSaveable`,
 * because surviving process death is the whole job of those three and a plain
 * object field would not do it. A half written note lost to the system
 * reclaiming memory is the worst thing this app could do short of losing the
 * notebook.
 *
 * Held for the life of the composition, so nothing here is saved and nothing
 * here is restored: this is what is on screen right now, not what the person
 * wrote down.
 */
internal class ShellState {
    var destination by mutableStateOf(Destination.NOTEBOOK)
    var counts by mutableStateOf<List<SectionCount>?>(null)
    var sheetOpen by mutableStateOf(false)

    /**
     * Whether Today is being arranged, which is the one time the capture button
     * stands down.
     *
     * **Held here rather than only inside Today**, because the button is the
     * shell's and the mode is Today's, and the shell cannot see into a screen.
     *
     * **Why it stands down at all.** Somebody moving cards around is not also
     * writing something down, and the button sat on top of the cards being
     * moved: at font scale 2.0 it covered the words on the card beneath it. It
     * is also the screen's one filled action, and while the arrangement is open
     * the action that matters is the one that keeps it.
     */
    var todayArranging by mutableStateOf(false)
    // What changed since the previous launch.
    //
    // **The boundary is read once and held for the whole session**, so a digest
    // does not empty itself out from under somebody who is still reading it,
    // and so the things they write during this visit show up as what they are:
    // changes since they were last here.
    var digest by mutableStateOf(Digest.nothing)
    var capturing by mutableStateOf<CaptureKind?>(null)
    // Bumped after every write, which is what makes the counts refresh without
    // the screen having to know what changed.
    var revision by mutableStateOf(0)
    var saving by mutableStateOf<CaptureDraft?>(null)
    // The entry being filed out of the tray, and where it is going. Null means
    // nothing is in flight.
    var filing by mutableStateOf<Pair<String, String?>?>(null)
    // What this notebook already tracks, and the catalog of things it could.
    var measures by mutableStateOf<List<Repository.Measure>>(emptyList())
    var presets by mutableStateOf<List<TemplateCatalog.Preset>>(emptyList())
    var recording by mutableStateOf<MeasurementDraft?>(null)
    // The threads this notebook carries, which the capture form offers as chips.
    // Empty is a real state: a notebook with no situation template has none, and
    // the form drops that question rather than showing an answerless one.
    var threads by mutableStateOf<List<Repository.CareThread>>(emptyList())
    // The counts could not be read. Distinct from not having read them yet,
    // which is what a null `counts` means.
    var failed by mutableStateOf(false)
    // Everything the person saved without saying where it belonged. The capture
    // form already promises them this tray exists.
    var unfiled by mutableStateOf<List<Repository.UnfiledEntry>>(emptyList())
    var trayOpen by mutableStateOf(false)
    // Which section of the table of contents is open, if any. Null is the
    // notebook itself. **Every section opens**, because a row that counts
    // things and then does nothing when tapped is a dead end, per rules 16
    // and 18, and twelve of them was the largest one in the app.
    var openSection by mutableStateOf<Repository.Section?>(null)
    var trail by mutableStateOf<List<Repository.TrailEntry>>(emptyList())
    var people by mutableStateOf<List<Repository.Person>>(emptyList())
    // **The same people in a different order, and it needs to be a second
    // list.** The care team screen shows them in the order they were added,
    // which is what somebody scanning a roster expects and what that screen
    // documents. The capture form shows five of them as chips, and the five
    // worth offering are whoever the person has been dealing with lately.
    // Reordering the one list would have quietly reordered the roster too.
    var peopleForCapture by mutableStateOf<List<Repository.Person>>(emptyList())
    // The same threads in the order worth offering rather than the order worth
    // scanning. See `Repository.threadsByRecentUse`.
    var threadsForFiling by mutableStateOf<List<Repository.CareThread>>(emptyList())
    // The entry whose date is being corrected, per rule 17. Null means nothing
    // is being edited.
    var editingDate by mutableStateOf<Repository.TrailEntry?>(null)
    // The correction in flight: which entry, and the date the person chose.
    var correcting by mutableStateOf<Pair<String, Edtf.Date>?>(null)
    // Somebody being added to the care team, and the draft being written.
    var addingPerson by mutableStateOf(false)
    /** Roles the active situation names, offered as chips when adding a contact. */
    var roleSuggestions by mutableStateOf<List<String>>(emptyList())
    /**
     * The card types this person's stated situation starts them with.
     *
     * **The gallery puts them first**, 21.6 screen 6. It is the person's own
     * stated situation and never inference, per 21.5: this app does not watch
     * its user, and the only two things that shape Today are what they chose
     * and what they told it.
     */
    var suggestedCards by mutableStateOf<Set<String>>(emptySet())
    var savingPerson by mutableStateOf<PersonDraft?>(null)

    /** Somebody being put at the top of the care team, or taken back out. #361. */
    var pinningPerson by mutableStateOf<Pair<String, Boolean>?>(null)

    /** Somebody being retired from the care team without being erased. #371. */
    var archivingPerson by mutableStateOf<Pair<String, Boolean>?>(null)

    /**
     * The standing instruction whose own words are being corrected, and what to
     * write. #374, and the fourth of the six.
     */
    var correctingInstruction by mutableStateOf<Repository.StandingInstruction?>(null)
    var savingInstructionWords by mutableStateOf<Triple<String, String, String>?>(null)

    /**
     * The question whose own words are being corrected, and what to write.
     * #374, and the third of the six.
     */
    var correctingQuestion by mutableStateOf<Repository.Question?>(null)
    var savingQuestionText by mutableStateOf<Pair<String, String>?>(null)

    /**
     * The chapter being renamed, and what to write. #374.
     *
     * **`renameChapter` sat in the repository with no caller** from the day the
     * defect was noticed until #373 made room in the shell for another full
     * screen surface. It was deliberately kept out of the removal ledger, per
     * D152, because a ledger row would have told the next reader the app had
     * decided somebody may not fix a name they typed wrong.
     */
    var renamingChapter by mutableStateOf<Repository.Chapter?>(null)
    var savingChapterRename by mutableStateOf<Pair<String, String>?>(null)

    /** The project being renamed, and what to write. #374. Same story. */
    var renamingProject by mutableStateOf<Repository.Project?>(null)
    var savingProjectRename by mutableStateOf<Pair<String, String>?>(null)

    /** The care thread being renamed, and what to write. #371. */
    var renamingThread by mutableStateOf<Repository.CareThread?>(null)
    var savingThreadRename by mutableStateOf<Pair<String, String>?>(null)

    /** Correcting who the notebook is about, and what to write. #371. */
    var correctingSubject by mutableStateOf(false)
    /** The subject row, read when the correction opens rather than held all the time. */
    var subjectRow by mutableStateOf<Repository.Subject?>(null)
    var savingSubject by mutableStateOf<SubjectCorrection?>(null)

    /** The entry whose words are being corrected, and what to write. #368. */
    var correctingEntry by mutableStateOf<Repository.TrailEntry?>(null)
    var savingEntryCorrection by mutableStateOf<Pair<String, EntryCorrection>?>(null)
    // The person or medication being corrected. Null means the form, when
    // open, is adding rather than editing.
    var editingPerson by mutableStateOf<Repository.Person?>(null)
    var editingMedication by mutableStateOf<Repository.Medication?>(null)
    var editingAppointment by mutableStateOf<Repository.Appointment?>(null)
    var editingBill by mutableStateOf<Repository.Bill?>(null)
    var editingDocument by mutableStateOf<Repository.Document?>(null)
    var projects by mutableStateOf<List<Repository.Project>>(emptyList())
    var projectTemplates by mutableStateOf<List<TemplateCatalog.ProjectTemplate>>(emptyList())
    var startingProject by mutableStateOf(false)
    var chosenTemplate by mutableStateOf<TemplateCatalog.ProjectTemplate?>(null)
    /**
     * The template being looked at before anything is created, screen 04.
     *
     * **Separate from `chosenTemplate` on purpose.** That one means "create
     * this now", and the whole point of the preview is that looking at a
     * template is not agreeing to it.
     */
    var previewTemplate by mutableStateOf<TemplateCatalog.ProjectTemplate?>(null)
    /** What the person called it on the preview, which may not be the template's own name. */
    var previewName by mutableStateOf<String?>(null)
    // The project being looked at, and its steps.
    var openProject by mutableStateOf<Repository.Project?>(null)
    var projectSteps by mutableStateOf<List<Repository.ProjectStep>>(emptyList())
    var projectStages by mutableStateOf<List<Repository.ProjectStage>>(emptyList())
    var projectStanding by mutableStateOf<Repository.ProjectStanding?>(null)
    var projectNextDate by mutableStateOf<Repository.ProjectDate?>(null)
    var projectLatestWord by mutableStateOf<Repository.TrailEntry?>(null)
    var projectPapers by mutableStateOf<List<Repository.ProjectPaper>>(emptyList())
    var projectStandingHistory by mutableStateOf<List<Repository.ProjectStanding>>(emptyList())
    var updatingStanding by mutableStateOf<Repository.Project?>(null)
    var addingDateTo by mutableStateOf<Repository.Project?>(null)
    var loggingCallOn by mutableStateOf<Repository.Project?>(null)
    var movingStageOn by mutableStateOf<Repository.Project?>(null)
    var movingStage by mutableStateOf<Triple<String, String, Edtf.Date>?>(null)
    var settingLead by mutableStateOf<Pair<String, String>?>(null)
    var settingStatus by mutableStateOf<Pair<String, String>?>(null)
    var savingCall by mutableStateOf<Triple<String, String, String>?>(null)
    var savingDate by mutableStateOf<Quadruple<String, String, Edtf.Date, String>?>(null)
    var projectDateKinds by mutableStateOf<List<String>>(emptyList())
    var projectTrail by mutableStateOf<List<Repository.ProjectTrailItem>>(emptyList())
    var trailOpen by mutableStateOf(false)
    var paperworkOpen by mutableStateOf(false)
    var peopleOpen by mutableStateOf(false)
    var projectPeople by mutableStateOf<List<Repository.ProjectPerson>>(emptyList())
    var documentFilings by mutableStateOf<List<Repository.DocumentFiling>>(emptyList())
    var projectPaperCards by mutableStateOf<List<Repository.ProjectPaperCard>>(emptyList())
    var projectEntries by mutableStateOf<List<Repository.TrailEntry>>(emptyList())
    // A named type rather than a fourth slot on a Triple. Three anonymous
    // strings and a date is exactly the shape where the holder and the activity
    // get passed the wrong way round, and neither the compiler nor a screenshot
    // would say so.
    var savingStanding by mutableStateOf<StandingWrite?>(null)
    var projectCards by mutableStateOf<Map<String, Repository.ProjectCard>>(emptyMap())
    var todayLayout by mutableStateOf<Repository.TodayLayout?>(null)
    var addingCardTo by mutableStateOf<List<Repository.TodayCard>?>(null)
    var savingLayout by mutableStateOf<List<Repository.TodayCard>?>(null)
    var todayAnswers by mutableStateOf<Map<String, Repository.TodayAnswer>>(emptyMap())
    /**
     * What each card the gallery can offer would say right now, by offer key.
     *
     * **Read when the gallery opens rather than when Today gains focus.** These
     * are the answers for cards that are *not* on the screen, which on a year
     * five notebook is eight measures and three questions about each of six
     * projects: twenty six queries, on the front door, every time somebody
     * looks at it, for a sheet they open a handful of times a year. 21.2 says
     * the surface pulls on focus and this is not part of that pull.
     */
    var offerAnswers by mutableStateOf<Map<String, Repository.TodayAnswer>>(emptyMap())
    var togglingStep by mutableStateOf<Repository.ProjectStep?>(null)
    var settingProjectStatus by mutableStateOf<Pair<String, String>?>(null)
    var settingWaitingOn by mutableStateOf<Pair<String, String>?>(null)
    // Every step edit is a pending write held here rather than done inside a
    // composable, which is the shape every other write on this screen uses.
    var addingStep by mutableStateOf<Pair<String, String>?>(null)
    var editingStep by mutableStateOf<Triple<String, String, String?>?>(null)
    var movingStep by mutableStateOf<Pair<String, Boolean>?>(null)
    // The incident being corrected, and the correction in flight. #358.
    var correctingIncident by mutableStateOf<Repository.Incident?>(null)
    var savingCorrection by mutableStateOf<Pair<String, IncidentCorrection>?>(null)
    var removingStep by mutableStateOf<String?>(null)
    /** The starting steps being changed, 20.5 screen 18. */
    var stepsOpen by mutableStateOf(false)
    /** The one step whose sheet is open. */
    var stepUnderEdit by mutableStateOf<Repository.ProjectStep?>(null)
    /** The road being changed, 20.5 screen 18. */
    var roadOpen by mutableStateOf(false)
    var stageUnderEdit by mutableStateOf<Repository.ProjectStage?>(null)
    var addingStage by mutableStateOf<Pair<String, String>?>(null)
    var renamingStage by mutableStateOf<Pair<String, String>?>(null)
    /** Reordering a stage against its neighbor, which is not the same as moving the project onto one. */
    var reorderingStage by mutableStateOf<Pair<String, Boolean>?>(null)
    var removingStage by mutableStateOf<String?>(null)
    /** The date kinds being changed, 20.5 screen 18. */
    var kindsOpen by mutableStateOf(false)
    var projectDateKindRows by mutableStateOf<List<Repository.ProjectDateKind>>(emptyList())
    var kindUnderEdit by mutableStateOf<Repository.ProjectDateKind?>(null)
    var addingKind by mutableStateOf<Pair<String, String>?>(null)
    var renamingKind by mutableStateOf<Pair<String, String>?>(null)
    var removingKind by mutableStateOf<String?>(null)
    /** The paper placeholders being changed, 20.5 screen 18. */
    var papersOpen by mutableStateOf(false)
    var paperUnderEdit by mutableStateOf<Repository.ProjectPaper?>(null)
    var addingPaper by mutableStateOf<Pair<String, String>?>(null)
    var renamingPaper by mutableStateOf<Pair<String, String>?>(null)
    var emptyingPaper by mutableStateOf<String?>(null)
    var removingPaper by mutableStateOf<String?>(null)
    var creatingOwnProject by mutableStateOf<String?>(null)
    var ownTemplates by mutableStateOf<List<Repository.OwnTemplate>>(emptyList())
    var libraryOpen by mutableStateOf(false)
    /**
     * How the notebook is set up, and changing it.
     *
     * **The picker ran once during setup and was then unreachable forever**, so
     * a family whose care moved could not tell the app and could not even see
     * which setting they had picked. Law 5 promises all of it is changeable
     * afterward from one screen, and there was no screen.
     */
    var situationOpen by mutableStateOf(false)
    var situationPickerOpen by mutableStateOf(false)
    var situations by mutableStateOf<TemplateCatalog.Situations?>(null)
    var chosenSituation by mutableStateOf<TemplateCatalog.Situation?>(null)
    /** The change waiting to be written: the setting, and a chapter or null. */
    var applyingSituation by mutableStateOf<SituationChange?>(null)
    /**
     * Which setting the notebook is on, reloaded with everything else.
     *
     * The id rather than the subject, because it is the only field of the
     * subject anything above the notebook reads, and holding the whole row here
     * would be a second copy of it going stale beside the first.
     */
    var situationId by mutableStateOf<String?>(null)
    var savingTemplate by mutableStateOf<Repository.Project?>(null)
    // Which projects have had their steps saved, so the control says so once
    // rather than inviting the same save again.
    var savedTemplates by mutableStateOf<Set<String>>(emptySet())
    var startingFromOwn by mutableStateOf<Repository.OwnTemplate?>(null)
    var aboutOpen by mutableStateOf(false)
    var conflictsOpen by mutableStateOf(false)
    var conflicts by mutableStateOf(emptyList<Repository.Resolution>())
    var unseenConflicts by mutableStateOf(0)
    var markConflictsSeen by mutableStateOf(false)

    /** Incidents, which `MASTER_SPEC.md` 4.7 makes threads rather than events. */
    var incidents by mutableStateOf<List<Repository.Incident>>(emptyList())
    var incidentsOpen by mutableStateOf(false)
    var openIncident by mutableStateOf<Repository.Incident?>(null)
    /** Set while a capture is going to hang off an incident rather than stand alone. */
    var addingToIncident by mutableStateOf<String?>(null)
    /** The incident to settle or reopen, and which of the two. */
    var resolvingIncident by mutableStateOf<Pair<String, Boolean>?>(null)
    /** The entry waiting to be pinned to the top of the trail, or unpinned. */
    var pinningEntry by mutableStateOf<Pair<String, Boolean>?>(null)
    /** True while the emergency card is on its way to the share sheet. */
    var sharingCard by mutableStateOf(false)
    /** The document whose own screen is open, or null. */
    var openDocument by mutableStateOf<Repository.Document?>(null)
    /** The bill whose own screen is open, or null. */
    var openBill by mutableStateOf<Repository.Bill?>(null)
    /** The incident waiting to be turned into a document and handed to the share sheet. */
    var sharingIncident by mutableStateOf<Repository.Incident?>(null)
    /** The prep sheet waiting to become a document. */
    var sharingPrep by mutableStateOf<Repository.Prep?>(null)

    /**
     * The entry being read on its own.
     *
     * **Nothing could open one until 2026-08-02.** A trail row's only tappable
     * part was its date, and a search result opened the section and left the
     * person to find the row again. Rule 18 and #46.
     */
    var openEntry by mutableStateOf<String?>(null)
    var entryDetail by mutableStateOf<Repository.EntryDetail?>(null)

    /**
     * The person being read on their own, and everything that involved them.
     *
     * The other half of the link `entry_person` finally has a writer for.
     * `MASTER_SPEC.md` section 3: a person knows every call and visit
     * involving them.
     */
    var openPerson by mutableStateOf<Repository.Person?>(null)

    /** The appointment whose prep sheet is open, and the sheet itself. */
    var openPrepFor by mutableStateOf<String?>(null)
    /**
     * The month being reviewed, and what it holds.
     *
     * **The month rather than a label**, because the label is already localized
     * and reading the app's own rendering back as data is how a screen ends up
     * agreeing with itself in English and disagreeing in Arabic.
     */
    var reviewMonth by mutableStateOf<java.time.YearMonth?>(null)
    var review by mutableStateOf<Repository.MonthReview?>(null)
    /** A month waiting to become a document for the share sheet. */
    var sharingReview by mutableStateOf<Repository.MonthReview?>(null)
    var milestonesOpen by mutableStateOf(false)
    var addingMilestone by mutableStateOf(false)
    var editingMilestone by mutableStateOf<Repository.Milestone?>(null)
    var savingMilestone by mutableStateOf<MilestoneDraft?>(null)
    var milestones by mutableStateOf<List<Repository.Milestone>>(emptyList())

    /** The care thread being read, and what is on it. */
    var openThread by mutableStateOf<Repository.CareThread?>(null)

    /** The chapter being read, and what happened while they were there. */
    var openChapter by mutableStateOf<Repository.Chapter?>(null)

    /** The medication being read, and how it changed. */
    var openMedication by mutableStateOf<Repository.Medication?>(null)
    /** The medication a change is being written down for. */
    var recordingChangeTo by mutableStateOf<Repository.Medication?>(null)
    /** Every time each standing instruction was not followed, and the one being written down. */
    var violationsByInstruction by
        mutableStateOf<Map<String, List<Repository.Violation>>>(emptyMap())
    var recordingViolationFor by mutableStateOf<Repository.StandingInstruction?>(null)
    var savingViolation by mutableStateOf<ViolationDraft?>(null)
    /** The recorded time being corrected, with the request it belongs to. */
    var correctingViolation by
        mutableStateOf<Pair<Repository.StandingInstruction, Repository.Violation>?>(null)
    var savingMedicationEvent by mutableStateOf<MedicationEventDraft?>(null)
    var medicationHistory by mutableStateOf<List<Repository.MedicationEvent>>(emptyList())
    var medicationQuestions by mutableStateOf<List<Repository.Question>>(emptyList())
    var incidentDetail by mutableStateOf(Repository.IncidentDetail())
    var billViolations by mutableStateOf<List<Repository.Violation>>(emptyList())
    var chapterDetail by mutableStateOf<Repository.ChapterDetail?>(null)
    var threadEntries by mutableStateOf<List<Repository.TrailEntry>>(emptyList())
    var prep by mutableStateOf<Repository.Prep?>(null)
    var personEntries by mutableStateOf<List<Repository.TrailEntry>>(emptyList())

    /**
     * Search, and what has been typed into it.
     *
     * **The query is saved rather than remembered**, so somebody who searched,
     * opened a result, and came back is not made to type it again. A search a
     * person had to repeat is one they stop using. Rule 18 counts taps.
     */
    var searchOpen by mutableStateOf(false)
    var searchResults by mutableStateOf<List<Repository.SearchHit>>(emptyList())
    var searchFailed by mutableStateOf(false)
    var exportOpen by mutableStateOf(false)
    var exportState by mutableStateOf(ExportState.READY)
    /**
     * How many attached files the last export could not find. #332.
     *
     * **Beside the state rather than inside it**, because it is not a state:
     * the export finished either way and the file is written either way. What
     * it changes is what the finished screen has to say.
     */
    var exportMissing by mutableStateOf(0)
    // Held between choosing a passphrase and choosing where the file goes,
    // because the system picker is a round trip through another activity.
    var pendingPassphrase by mutableStateOf<String?>(null)
    /** The reminder the person wrote, which travels in the clear. 8.1. */
    var pendingHint by mutableStateOf<String?>(null)
    var writeTo by mutableStateOf<android.net.Uri?>(null)
    var restoreOpen by mutableStateOf(false)
    var recountConflicts by mutableStateOf(false)
    // The chosen file, copied into the cache so it can be read more than once:
    // once to find out whether it is locked, and again with the passphrase.
    // Which of the two things the person asked for. Held beside the trigger
    // rather than inside the screen, because the screen is redrawn from state
    // and an answer that lived there would be lost the moment it was given.
    // The emergency card, and whether it is being filled in.
    var emergencyCard by mutableStateOf<Repository.EmergencyCard?>(null)
    var editingEmergencyCard by mutableStateOf(false)
    var savingEmergencyCard by mutableStateOf<EmergencyDraft?>(null)
    var emergencyContacts by mutableStateOf<List<Repository.EmergencyContact>>(emptyList())
    // Somebody being put on or taken off the card. Null means nothing in flight.
    var togglingContact by mutableStateOf<Repository.Person?>(null)
    var medications by mutableStateOf<List<Repository.Medication>>(emptyList())
    // How many questions wait on each medication, so the list row can say so
    // rather than making somebody open every medication in turn. #352.
    var medicationQuestionCounts by mutableStateOf<Map<String, Int>>(emptyMap())
    var addingMedication by mutableStateOf(false)
    var savingMedication by mutableStateOf<MedicationDraft?>(null)
    var questions by mutableStateOf<List<Repository.Question>>(emptyList())
    var threadCounts by mutableStateOf<List<Repository.ThreadWithCount>>(emptyList())
    // Starting a thread from nothing, #349 and D145. The name in flight rather
    // than a draft type, because a name is the only thing the screen asks for.
    var addingThread by mutableStateOf(false)
    var savingThread by mutableStateOf<String?>(null)
    var readings by mutableStateOf<List<Repository.Reading>>(emptyList())
    var chapters by mutableStateOf<List<Repository.Chapter>>(emptyList())
    var chapterContents by mutableStateOf<Map<String, Repository.ChapterContents>>(emptyMap())
    var appointments by mutableStateOf<List<Repository.Appointment>>(emptyList())
    var addingAppointment by mutableStateOf(false)
    var instructions by mutableStateOf<List<Repository.StandingInstruction>>(emptyList())
    var instructionCatalog by mutableStateOf<TemplateCatalog.Instructions?>(null)
    var addingInstruction by mutableStateOf(false)
    var bills by mutableStateOf<List<Repository.Bill>>(emptyList())
    var addingBill by mutableStateOf(false)
    var documents by mutableStateOf<List<Repository.Document>>(emptyList())

    /**
     * The folders this notebook already has, offered by the document form. #221.
     *
     * **Reloaded with everything else**, so saving into a new folder makes that
     * folder offerable to the next document without a second trip.
     */
    var documentFolders by mutableStateOf<List<String>>(emptyList())
    var addingDocument by mutableStateOf(false)
    var savingDocument by mutableStateOf<DocumentDraft?>(null)
    // Said only when a file was refused, and cleared the moment the person
    // picks another, so it never lingers over a choice it does not describe.
    var documentError by mutableStateOf<String?>(null)
    // What the person long pressed, and what removing it means. Held as the
    // section plus the row plus the words to show back, so one sheet serves
    // every list rather than each screen growing its own.
    var removing by mutableStateOf<Removal?>(null)
    var confirmedRemoval by mutableStateOf<Removal?>(null)
    var savingBill by mutableStateOf<BillDraft?>(null)
    var savingInstruction by mutableStateOf<TemplateCatalog.Instruction?>(null)
    var savingAppointment by mutableStateOf<AppointmentDraft?>(null)
    var markingAsked by mutableStateOf<Repository.Question?>(null)
    // The question whose answer is being recorded, and the answer in flight.
    var answering by mutableStateOf<Repository.Question?>(null)
    var savingAnswer by mutableStateOf<Pair<String, String>?>(null)
    var acknowledging by mutableStateOf<Repository.StandingInstruction?>(null)
    var savingAcknowledgment by mutableStateOf<Pair<String, String>?>(null)
}
