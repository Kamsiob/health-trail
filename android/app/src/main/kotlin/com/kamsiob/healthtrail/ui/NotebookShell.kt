package com.kamsiob.healthtrail.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.LastVisit
import com.kamsiob.healthtrail.data.Repository
import java.time.LocalDate
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.BottomNav
import com.kamsiob.healthtrail.ui.components.CaptureFab
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.screens.AboutScreen
import com.kamsiob.healthtrail.ui.screens.headingFor
import com.kamsiob.healthtrail.ui.screens.IncidentScreen
import com.kamsiob.healthtrail.ui.screens.IncidentsScreen
import com.kamsiob.healthtrail.data.Readable
import com.kamsiob.healthtrail.ui.components.Share
import com.kamsiob.healthtrail.ui.screens.EntryScreen
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.PrepScreen
import com.kamsiob.healthtrail.ui.screens.ChapterScreen
import com.kamsiob.healthtrail.ui.screens.MedicationEventDraft
import com.kamsiob.healthtrail.ui.screens.MedicationEventScreen
import com.kamsiob.healthtrail.ui.screens.MedicationScreen
import com.kamsiob.healthtrail.ui.screens.ViolationDraft
import com.kamsiob.healthtrail.ui.screens.ViolationScreen
import com.kamsiob.healthtrail.ui.screens.SearchScreen
import com.kamsiob.healthtrail.ui.screens.ThreadScreen
import com.kamsiob.healthtrail.ui.screens.ExportScreen
import com.kamsiob.healthtrail.ui.screens.RestoreScreen
import com.kamsiob.healthtrail.ui.screens.RestoreState
import com.kamsiob.healthtrail.data.ExportContainer
import com.kamsiob.healthtrail.ui.screens.ExportState
import com.kamsiob.healthtrail.data.Backup
import com.kamsiob.healthtrail.ui.screens.MoreScreen
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import androidx.compose.foundation.background
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureFormState
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CaptureSheet
import com.kamsiob.healthtrail.ui.screens.edtf
import com.kamsiob.healthtrail.ui.screens.entryKind
import com.kamsiob.healthtrail.ui.screens.usesTheSharedForm
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.emphasisFrom
import com.kamsiob.healthtrail.ui.screens.MeasurementDraft
import com.kamsiob.healthtrail.ui.screens.MeasurementScreen
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.AddPersonScreen
import com.kamsiob.healthtrail.ui.screens.CareTeamScreen
import com.kamsiob.healthtrail.ui.screens.AddMedicationScreen
import com.kamsiob.healthtrail.ui.screens.MedicationDraft
import com.kamsiob.healthtrail.ui.screens.MedicationsScreen
import com.kamsiob.healthtrail.ui.screens.AcknowledgeSheet
import com.kamsiob.healthtrail.ui.screens.AnswerSheet
import com.kamsiob.healthtrail.ui.screens.QuestionsScreen
import com.kamsiob.healthtrail.ui.screens.CareThreadsScreen
import com.kamsiob.healthtrail.ui.screens.ProgressScreen
import com.kamsiob.healthtrail.ui.screens.ProjectDetailScreen
import com.kamsiob.healthtrail.ui.screens.ProjectsScreen
import com.kamsiob.healthtrail.ui.screens.StartProjectScreen
import com.kamsiob.healthtrail.ui.screens.ChaptersScreen
import com.kamsiob.healthtrail.ui.screens.AddAppointmentScreen
import com.kamsiob.healthtrail.ui.screens.AppointmentDraft
import com.kamsiob.healthtrail.ui.screens.AppointmentsScreen
import com.kamsiob.healthtrail.ui.screens.AddInstructionScreen
import com.kamsiob.healthtrail.ui.screens.StandingInstructionsScreen
import com.kamsiob.healthtrail.ui.screens.AddBillScreen
import com.kamsiob.healthtrail.ui.screens.BillDraft
import com.kamsiob.healthtrail.ui.screens.MoneyScreen
import com.kamsiob.healthtrail.ui.screens.AddDocumentScreen
import com.kamsiob.healthtrail.ui.screens.DocumentDraft
import com.kamsiob.healthtrail.ui.screens.DocumentsScreen
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.ui.screens.parseAmountToMinor
import com.kamsiob.healthtrail.ui.screens.EmergencyCardEditScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyCardScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyDraft
import com.kamsiob.healthtrail.ui.screens.PersonDraft
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.TrailScreen
import com.kamsiob.healthtrail.ui.components.ConfirmRemoveSheet
import com.kamsiob.healthtrail.ui.screens.labelKey
import com.kamsiob.healthtrail.ui.screens.kindNameKey
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.screens.CoachStep
import com.kamsiob.healthtrail.ui.screens.LocalSectionBackKey
import com.kamsiob.healthtrail.ui.screens.TemplateLibraryScreen
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.UnfiledTrayScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrail
import com.kamsiob.healthtrail.ui.theme.Space

object ShellTags {
    const val ROOT = "shell_root"
    const val NOT_BUILT = "shell_not_built"
    const val ERROR = "shell_error"
}

/**
 * The notebook itself: four destinations and the capture button, on every screen.
 *
 * **The section order is fixed here, in one place.** It is the order in
 * `MASTER_SPEC.md` section 4.4, and it never varies by situation template,
 * because a table of contents whose order shifts is not a table of contents. The
 * template decides emphasis, not availability.
 *
 * Counts are read through the repository, which means through the `live_*`
 * views, so anything the person deleted is already excluded rather than filtered
 * out here.
 */
@Composable
fun NotebookShell(
    repository: Repository,
    themeChoice: ThemeChoice,
    onThemeChoice: (ThemeChoice) -> Unit,
) {
    val strings = LocalStrings.current
    var destination by remember { mutableStateOf(Destination.NOTEBOOK) }
    var counts by remember { mutableStateOf<List<SectionCount>?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }
    // What changed since the previous launch.
    //
    // **The boundary is read once and held for the whole session**, so a digest
    // does not empty itself out from under somebody who is still reading it,
    // and so the things they write during this visit show up as what they are:
    // changes since they were last here.
    var digest by remember { mutableStateOf(Digest.nothing) }
    var capturing by remember { mutableStateOf<CaptureKind?>(null) }

    /**
     * What the person has typed into the capture form and not yet saved.
     *
     * **Held here rather than in the form, and saved rather than remembered.**
     * The form used to keep everything in a local `remember`, so a back press,
     * a rotation, a theme change, or the system reclaiming memory threw away a
     * half written note. Somebody standing in a corridor writing down what the
     * nurse just said is exactly who that loses, and losing it is the worst
     * thing this app could do short of losing the notebook.
     *
     * `rememberSaveable` puts it in the bundle, so it survives process death
     * too, which `remember` at this level would not.
     *
     * **Nothing about it is ever shown as a warning.** No "you have unsaved
     * changes", no confirmation before leaving, no completeness count. It is
     * simply still there when they come back. Rule 13.
     */
    var captureDraft by rememberSaveable(stateSaver = CaptureFormState.Saver) {
        mutableStateOf(CaptureFormState())
    }
    // Bumped after every write, which is what makes the counts refresh without
    // the screen having to know what changed.
    var revision by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf<CaptureDraft?>(null) }
    // The entry being filed out of the tray, and where it is going. Null means
    // nothing is in flight.
    var filing by remember { mutableStateOf<Pair<String, String?>?>(null) }
    // What this notebook already tracks, and the catalog of things it could.
    var measures by remember { mutableStateOf<List<Repository.Measure>>(emptyList()) }
    var presets by remember { mutableStateOf<List<TemplateCatalog.Preset>>(emptyList()) }
    var recording by remember { mutableStateOf<MeasurementDraft?>(null) }
    // The threads this notebook carries, which the capture form offers as chips.
    // Empty is a real state: a notebook with no situation template has none, and
    // the form drops that question rather than showing an answerless one.
    var threads by remember { mutableStateOf<List<Repository.CareThread>>(emptyList()) }
    // The counts could not be read. Distinct from not having read them yet,
    // which is what a null `counts` means.
    var failed by remember { mutableStateOf(false) }
    // Everything the person saved without saying where it belonged. The capture
    // form already promises them this tray exists.
    var unfiled by remember { mutableStateOf<List<Repository.UnfiledEntry>>(emptyList()) }
    var trayOpen by remember { mutableStateOf(false) }
    // Which section of the table of contents is open, if any. Null is the
    // notebook itself. **Every section opens**, because a row that counts
    // things and then does nothing when tapped is a dead end, per rules 16
    // and 18, and twelve of them was the largest one in the app.
    var openSection by remember { mutableStateOf<Repository.Section?>(null) }
    var trail by remember { mutableStateOf<List<Repository.TrailEntry>>(emptyList()) }
    var people by remember { mutableStateOf<List<Repository.Person>>(emptyList()) }
    // **The same people in a different order, and it needs to be a second
    // list.** The care team screen shows them in the order they were added,
    // which is what somebody scanning a roster expects and what that screen
    // documents. The capture form shows five of them as chips, and the five
    // worth offering are whoever the person has been dealing with lately.
    // Reordering the one list would have quietly reordered the roster too.
    var peopleForCapture by remember {
        mutableStateOf<List<Repository.Person>>(emptyList())
    }
    // The same threads in the order worth offering rather than the order worth
    // scanning. See `Repository.threadsByRecentUse`.
    var threadsForFiling by remember {
        mutableStateOf<List<Repository.CareThread>>(emptyList())
    }
    // The entry whose date is being corrected, per rule 17. Null means nothing
    // is being edited.
    var editingDate by remember { mutableStateOf<Repository.TrailEntry?>(null) }
    // The correction in flight: which entry, and the date the person chose.
    var correcting by remember { mutableStateOf<Pair<String, Edtf.Date>?>(null) }
    // Somebody being added to the care team, and the draft being written.
    var addingPerson by remember { mutableStateOf(false) }
    /** Roles the active situation names, offered as chips when adding a contact. */
    var roleSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var savingPerson by remember { mutableStateOf<PersonDraft?>(null) }
    // The person or medication being corrected. Null means the form, when
    // open, is adding rather than editing.
    var editingPerson by remember { mutableStateOf<Repository.Person?>(null) }
    var editingMedication by remember { mutableStateOf<Repository.Medication?>(null) }
    var editingAppointment by remember { mutableStateOf<Repository.Appointment?>(null) }
    var editingBill by remember { mutableStateOf<Repository.Bill?>(null) }
    var editingDocument by remember { mutableStateOf<Repository.Document?>(null) }
    var projects by remember { mutableStateOf<List<Repository.Project>>(emptyList()) }
    var projectTemplates by remember {
        mutableStateOf<List<TemplateCatalog.ProjectTemplate>>(emptyList())
    }
    var startingProject by remember { mutableStateOf(false) }
    var chosenTemplate by remember {
        mutableStateOf<TemplateCatalog.ProjectTemplate?>(null)
    }
    // The project being looked at, and its steps.
    var openProject by remember { mutableStateOf<Repository.Project?>(null) }
    var projectSteps by remember { mutableStateOf<List<Repository.ProjectStep>>(emptyList()) }
    var togglingStep by remember { mutableStateOf<Repository.ProjectStep?>(null) }
    var settingProjectStatus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var settingWaitingOn by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Every step edit is a pending write held here rather than done inside a
    // composable, which is the shape every other write on this screen uses.
    var addingStep by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingStep by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    var movingStep by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var removingStep by remember { mutableStateOf<String?>(null) }
    var creatingOwnProject by remember { mutableStateOf<String?>(null) }
    var ownTemplates by remember {
        mutableStateOf<List<Repository.OwnTemplate>>(emptyList())
    }
    var libraryOpen by remember { mutableStateOf(false) }
    var savingTemplate by remember { mutableStateOf<Repository.Project?>(null) }
    // Which projects have had their steps saved, so the control says so once
    // rather than inviting the same save again.
    var savedTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var startingFromOwn by remember { mutableStateOf<Repository.OwnTemplate?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }

    /** Incidents, which `MASTER_SPEC.md` 4.7 makes threads rather than events. */
    var incidents by remember { mutableStateOf<List<Repository.Incident>>(emptyList()) }
    var incidentsOpen by remember { mutableStateOf(false) }
    var openIncident by remember { mutableStateOf<Repository.Incident?>(null) }
    var incidentEntries by remember { mutableStateOf<List<Repository.TrailEntry>>(emptyList()) }
    /** Set while a capture is going to hang off an incident rather than stand alone. */
    var addingToIncident by remember { mutableStateOf<String?>(null) }
    /** The incident to settle or reopen, and which of the two. */
    var resolvingIncident by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    /** The entry waiting to be pinned to the top of the trail, or unpinned. */
    var pinningEntry by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    /** The incident waiting to be turned into a document and handed to the share sheet. */
    var sharingIncident by remember { mutableStateOf<Repository.Incident?>(null) }
    /** The prep sheet waiting to become a document. */
    var sharingPrep by remember { mutableStateOf<Repository.Prep?>(null) }

    /**
     * The entry being read on its own.
     *
     * **Nothing could open one until 2026-08-02.** A trail row's only tappable
     * part was its date, and a search result opened the section and left the
     * person to find the row again. Rule 18 and #46.
     */
    var openEntry by remember { mutableStateOf<String?>(null) }
    var entryDetail by remember { mutableStateOf<Repository.EntryDetail?>(null) }

    /**
     * The person being read on their own, and everything that involved them.
     *
     * The other half of the link `entry_person` finally has a writer for.
     * `MASTER_SPEC.md` section 3: a person knows every call and visit
     * involving them.
     */
    var openPerson by remember { mutableStateOf<Repository.Person?>(null) }

    /** The appointment whose prep sheet is open, and the sheet itself. */
    var openPrepFor by remember { mutableStateOf<String?>(null) }

    /** The care thread being read, and what is on it. */
    var openThread by remember { mutableStateOf<Repository.CareThread?>(null) }

    /** The chapter being read, and what happened while they were there. */
    var openChapter by remember { mutableStateOf<Repository.Chapter?>(null) }

    /** The medication being read, and how it changed. */
    var openMedication by remember { mutableStateOf<Repository.Medication?>(null) }
    /** The medication a change is being written down for. */
    var recordingChangeTo by remember { mutableStateOf<Repository.Medication?>(null) }
    /** How many times each standing instruction was not followed, and the one being written down. */
    var violationCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var recordingViolationFor by remember {
        mutableStateOf<Repository.StandingInstruction?>(null)
    }
    var savingViolation by remember { mutableStateOf<ViolationDraft?>(null) }
    var savingMedicationEvent by remember { mutableStateOf<MedicationEventDraft?>(null) }
    var medicationHistory by remember {
        mutableStateOf<List<Repository.MedicationEvent>>(emptyList())
    }
    var medicationQuestions by remember {
        mutableStateOf<List<Repository.Question>>(emptyList())
    }
    var incidentPeople by remember { mutableStateOf<List<Repository.Person>>(emptyList()) }
    var incidentDocuments by remember { mutableStateOf<List<Repository.Document>>(emptyList()) }
    var chapterDetail by remember { mutableStateOf<Repository.ChapterDetail?>(null) }
    var threadEntries by remember { mutableStateOf<List<Repository.TrailEntry>>(emptyList()) }
    var prep by remember { mutableStateOf<Repository.Prep?>(null) }
    var personEntries by remember { mutableStateOf<List<Repository.TrailEntry>>(emptyList()) }

    /**
     * Search, and what has been typed into it.
     *
     * **The query is saved rather than remembered**, so somebody who searched,
     * opened a result, and came back is not made to type it again. A search a
     * person had to repeat is one they stop using. Rule 18 counts taps.
     */
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Repository.SearchHit>>(emptyList()) }
    var searchFailed by remember { mutableStateOf(false) }
    var exportOpen by remember { mutableStateOf(false) }
    var exportState by remember { mutableStateOf(ExportState.READY) }
    // Held between choosing a passphrase and choosing where the file goes,
    // because the system picker is a round trip through another activity.
    var pendingPassphrase by remember { mutableStateOf<String?>(null) }
    /** The reminder the person wrote, which travels in the clear. 8.1. */
    var pendingHint by remember { mutableStateOf<String?>(null) }
    var writeTo by remember { mutableStateOf<android.net.Uri?>(null) }
    var restoreOpen by remember { mutableStateOf(false) }
    var restoreState by remember { mutableStateOf<RestoreState>(RestoreState.Empty) }
    // The chosen file, copied into the cache so it can be read more than once:
    // once to find out whether it is locked, and again with the passphrase.
    var restoreFile by remember { mutableStateOf<File?>(null) }
    var openWith by remember { mutableStateOf<String?>(null) }
    var openNow by remember { mutableStateOf(false) }
    var applyNow by remember { mutableStateOf(false) }
    // The emergency card, and whether it is being filled in.
    var emergencyCard by remember { mutableStateOf<Repository.EmergencyCard?>(null) }
    var editingEmergencyCard by remember { mutableStateOf(false) }
    var savingEmergencyCard by remember { mutableStateOf<EmergencyDraft?>(null) }
    var emergencyContacts by remember {
        mutableStateOf<List<Repository.EmergencyContact>>(emptyList())
    }
    // Somebody being put on or taken off the card. Null means nothing in flight.
    var togglingContact by remember { mutableStateOf<Repository.Person?>(null) }
    var medications by remember { mutableStateOf<List<Repository.Medication>>(emptyList()) }
    var addingMedication by remember { mutableStateOf(false) }
    var savingMedication by remember { mutableStateOf<MedicationDraft?>(null) }
    var questions by remember { mutableStateOf<List<Repository.Question>>(emptyList()) }
    var threadCounts by remember {
        mutableStateOf<List<Repository.ThreadWithCount>>(emptyList())
    }
    var readings by remember { mutableStateOf<List<Repository.Reading>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<Repository.Chapter>>(emptyList()) }
    var appointments by remember {
        mutableStateOf<List<Repository.Appointment>>(emptyList())
    }
    var addingAppointment by remember { mutableStateOf(false) }
    var instructions by remember {
        mutableStateOf<List<Repository.StandingInstruction>>(emptyList())
    }
    var instructionCatalog by remember {
        mutableStateOf<TemplateCatalog.Instructions?>(null)
    }
    var addingInstruction by remember { mutableStateOf(false) }
    var bills by remember { mutableStateOf<List<Repository.Bill>>(emptyList()) }
    var addingBill by remember { mutableStateOf(false) }
    var documents by remember { mutableStateOf<List<Repository.Document>>(emptyList()) }
    var addingDocument by remember { mutableStateOf(false) }
    var savingDocument by remember { mutableStateOf<DocumentDraft?>(null) }
    // Said only when a file was refused, and cleared the moment the person
    // picks another, so it never lingers over a choice it does not describe.
    var documentError by remember { mutableStateOf<String?>(null) }
    // What the person long pressed, and what removing it means. Held as the
    // section plus the row plus the words to show back, so one sheet serves
    // every list rather than each screen growing its own.
    var removing by remember { mutableStateOf<Removal?>(null) }
    var confirmedRemoval by remember { mutableStateOf<Removal?>(null) }
    var savingBill by remember { mutableStateOf<BillDraft?>(null) }
    var savingInstruction by remember {
        mutableStateOf<TemplateCatalog.Instruction?>(null)
    }
    var savingAppointment by remember { mutableStateOf<AppointmentDraft?>(null) }
    var markingAsked by remember { mutableStateOf<Repository.Question?>(null) }
    // The question whose answer is being recorded, and the answer in flight.
    var answering by remember { mutableStateOf<Repository.Question?>(null) }
    var savingAnswer by remember { mutableStateOf<Pair<String, String>?>(null) }
    var acknowledging by remember { mutableStateOf<Repository.StandingInstruction?>(null) }
    var savingAcknowledgment by remember { mutableStateOf<Pair<String, String>?>(null) }
    val context = LocalContext.current
    val lastVisit = remember(context) {
        LastVisit(context).openAndAdvance(System.currentTimeMillis())
    }

    // Recounted whenever the tab changes, so returning to the notebook after
    // writing something shows the new number rather than a stale one.
    //
    // Wrapped, because a read can fail and a table of contents that throws
    // takes the whole app down. The screen says so and offers the read again,
    // per DESIGN.md section 10.3, which requires an error state on every screen
    // rather than only on the ones where a failure was imagined.
    LaunchedEffect(destination, revision) {
        failed = false
        try {
            val subject = repository.activeSubject()
            val emphasis = emphasisFor(context, subject?.situationTemplateId)
            // **The roles this situation actually has**, so adding a contact
            // offers them rather than asking the person to type a job title
            // from memory. The catalog has carried them since it was written.
            roleSuggestions = rolesFor(context, subject?.situationTemplateId)
            // Counts belong to a subject. With no subject there is no notebook
            // to count, and showing zeros would be inventing a notebook that
            // does not exist yet.
            counts = subject?.let { active ->
                SECTION_ORDER.map {
                    SectionCount(
                        it,
                        repository.count(it, active.id),
                        emphasis[it] ?: Emphasis.STANDING,
                    )
                }
            } ?: SECTION_ORDER.map { SectionCount(it, 0, Emphasis.STANDING) }
            // **Nothing to summarize on a first launch**, where there is no
            // previous visit to be "since". Reporting a notebook's whole
            // history as though it happened this week would be false on the one
            // screen whose job is to be true about the last few days.
            digest = lastVisit
                ?.let { Digest.since(repository.changesSince(it), it) }
                ?: Digest.nothing
            threads = subject?.let { repository.threads(it.id) }.orEmpty()
            unfiled = subject?.let { repository.unfiled(it.id) }.orEmpty()
            // Read alongside the counts rather than when a section opens, so
            // opening one is instant and never shows a spinner over a number
            // the person is already looking at.
            trail = subject?.let { repository.trail(it.id) }.orEmpty()
            incidents = subject?.let { repository.incidents(it.id) }.orEmpty()
            people = subject?.let { repository.people(it.id) }.orEmpty()
            peopleForCapture =
                subject?.let { repository.peopleByRecentUse(it.id) }.orEmpty()
            threadsForFiling =
                subject?.let { repository.threadsByRecentUse(it.id) }.orEmpty()
            emergencyCard = subject?.let { repository.emergencyCard(it.id) }
            medications = subject?.let { repository.medications(it.id) }.orEmpty()
            questions = subject?.let { repository.questions(it.id) }.orEmpty()
            threadCounts = subject?.let { repository.threadsWithCounts(it.id) }.orEmpty()
            readings = subject?.let { repository.readings(it.id) }.orEmpty()
            chapters = subject?.let { repository.chapters(it.id) }.orEmpty()
            appointments = subject?.let { repository.appointments(it.id) }.orEmpty()
            instructions = subject?.let { repository.standingInstructions(it.id) }.orEmpty()
            violationCounts = subject?.let { repository.violationCounts(it.id) }.orEmpty()
            bills = subject?.let { repository.bills(it.id) }.orEmpty()
            documents = subject?.let { repository.documents(it.id) }.orEmpty()
            projects = subject?.let { repository.projects(it.id) }.orEmpty()
            projectTemplates = TemplateCatalog.projects(context)
            ownTemplates = repository.ownTemplates("project")
            // Kept in step with the list, so a step ticked on the detail screen
            // and the count on the card behind it can never disagree.
            openProject = openProject?.let { current ->
                projects.firstOrNull { it.id == current.id }
            }
            projectSteps = openProject?.let { repository.projectSteps(it.id) }.orEmpty()
            instructionCatalog = TemplateCatalog.instructions(context)
            emergencyContacts = emergencyCard
                ?.let { repository.emergencyContacts(it.id) }
                .orEmpty()
            measures = subject?.let { repository.measures(it.id) }.orEmpty()
            presets = TemplateCatalog.presets(context)
        } catch (t: Throwable) {
            failed = true
        }
    }

    // **The system back button goes back, rather than out of the app.**
    // Everything above the notebook is state in this composable rather than a
    // navigation stack, so without this a person on any section screen, form,
    // or sheet who pressed back left Health Trail entirely. On a phone that is
    // the most used control there is, and losing a half typed form to it is
    // exactly the failure this app exists not to have.
    //
    // **Declared outermost first, because the last enabled handler wins.**
    // Compose gives priority to the most recently registered handler, so the
    // innermost thing has to be declared last or back closes the screen
    // underneath the one the person is looking at. That is what happened first
    // time: back dismissed the section while the form stayed on screen.
    //
    // Each is enabled only when the thing it closes is actually showing, so
    // back still leaves the app from the notebook itself, which is what a
    // person expects there.
    // **Back from a secondary destination returns to the notebook**, and only
    // the notebook lets go of the app.
    //
    // Found by walking search on the phone: back from More left the app
    // outright, from four taps deep, with no way to glance at anything else
    // first. Android's own model is that back walks up to the start
    // destination before it exits, and every other app on this person's phone
    // behaves that way, so leaving from any tab reads as the app dropping them.
    //
    // **Registered first, which gives it the lowest priority.** Compose hands
    // a back press to the most recently registered enabled handler, so this has
    // to come before every overlay below it. Put after them it won a press that
    // belonged to an open section, switching tabs out from under somebody who
    // was reading one. That is the same ordering trap the overlay block already
    // carries a comment about.
    BackHandler(enabled = destination != Destination.NOTEBOOK) {
        destination = Destination.NOTEBOOK
    }

    BackHandler(enabled = openSection != null) { openSection = null }
    BackHandler(enabled = openProject != null) { openProject = null }
    BackHandler(enabled = aboutOpen) { aboutOpen = false }
    BackHandler(enabled = searchOpen) { searchOpen = false }
    BackHandler(enabled = incidentsOpen && openIncident == null) { incidentsOpen = false }
    // **The library is above More and below a project**, so back from a project
    // opened out of the library returns to the library rather than leaving the
    // app. `BackJourneyTest` exists because that exact thing was wrong on every
    // screen above the notebook once.
    BackHandler(enabled = libraryOpen && openProject == null) { libraryOpen = false }
    BackHandler(enabled = openIncident != null) { openIncident = null }
    BackHandler(enabled = openEntry != null) { openEntry = null }
    BackHandler(enabled = openPerson != null) { openPerson = null }
    BackHandler(enabled = openPrepFor != null) { openPrepFor = null }
    BackHandler(enabled = openThread != null) { openThread = null }
    BackHandler(enabled = openChapter != null) { openChapter = null }
    BackHandler(enabled = openMedication != null) { openMedication = null }
    BackHandler(enabled = recordingChangeTo != null) { recordingChangeTo = null }
    BackHandler(enabled = recordingViolationFor != null) { recordingViolationFor = null }
    BackHandler(enabled = exportOpen) { exportOpen = false; exportState = ExportState.READY }
    BackHandler(enabled = restoreOpen) {
        restoreOpen = false
        restoreState = RestoreState.Empty
        restoreFile = null
    }
    BackHandler(enabled = answering != null) { answering = null }
    BackHandler(enabled = acknowledging != null) { acknowledging = null }
    BackHandler(enabled = startingProject) { startingProject = false }
    BackHandler(enabled = trayOpen) { trayOpen = false }
    BackHandler(enabled = sheetOpen) { sheetOpen = false }
    BackHandler(enabled = editingEmergencyCard) { editingEmergencyCard = false }
    BackHandler(enabled = addingInstruction) { addingInstruction = false }
    BackHandler(enabled = addingPerson) { addingPerson = false; editingPerson = null }
    BackHandler(enabled = addingMedication) {
        addingMedication = false
        editingMedication = null
    }
    BackHandler(enabled = addingAppointment) {
        addingAppointment = false
        editingAppointment = null
    }
    BackHandler(enabled = addingBill) { addingBill = false; editingBill = null }
    BackHandler(enabled = addingDocument) {
        addingDocument = false
        editingDocument = null
        documentError = null
    }
    // Back from a capture form reopens the sheet it came from, rather than
    // dropping the person on the notebook.
    //
    // **Found by `BackJourneyTest` on its first run.** Choosing a kind closes
    // the sheet and opens the form, so before this the sheet was simply gone:
    // somebody who reached for "Log a call" and hit "Log a visit" pressed back,
    // landed on the notebook, and had to tap the capture button and then the
    // right kind again. Three taps to undo one mistap, on the screen most
    // likely to be used one-handed in a hallway. It is one now.
    //
    // Back is a step up, and the sheet is the step it came from. **The form's
    // own Cancel button is a different question and still closes everything**,
    // because cancel means abandoning the entry rather than going back one
    // level. Both are reachable and they mean different things. D65.
    BackHandler(enabled = capturing != null) {
        capturing = null
        documentError = null
        sheetOpen = true
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ShellTags.ROOT),
        color = HealthTrail.colors.paper,
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            // **The capture button is a corner FAB in v4, not a notch in the
            // navigation bar.** It overlays the content rather than sitting in
            // the bar, so the bar is four equal tabs, and it lands where a thumb
            // already rests on a large phone.
            //
            // `BottomEnd` rather than `BottomRight`, so in Arabic it moves to
            // the start corner with the rest of the layout and the clearance in
            // `FabClearance` moves with it.
            //
            // Nothing tappable may sit underneath it, per `DESIGN.md` section 8
            // and D81. Screens inside this box owe it `fabScrollClearance` at
            // the bottom of any scrolling list and `fabSafeActionBar` on any
            // bottom-anchored action.
            Box(modifier = Modifier.weight(1f)) {
                when (destination) {
                    Destination.NOTEBOOK -> {
                        val loaded = counts
                        when {
                            failed -> CouldNotRead(onRetry = { revision += 1 })
                            loaded == null -> Loading()
                            else -> NotebookScreen(
                                sections = loaded,
                                onOpen = { openSection = it },
                                waiting = unfiled.size,
                                openIncidents = incidents.count { it.isOpen },
                                onOpenUnfiled = { trayOpen = true },
                                onOpenIncidents = { incidentsOpen = true },
                            )
                        }
                    }
                    // These arrive in their own increments. Until then each
                    // says plainly what it is rather than showing an empty
                    // frame, because a blank area reads as broken.
                    // Today's empty state is a finished screen, per #78 and
                    // persona P1. What is not built is the digest, and the
                    // screen says so itself rather than standing in for it.
                    Destination.TODAY -> TodayScreen(
                        openQuestions = questions.count { it.isOpen },
                        // Projects sitting on somebody else. The status and the
                        // named person are separate answers, so either counts.
                        waitingOnSomebody = projects.count {
                            !it.isFinished &&
                                (it.status == "waiting" || !it.waitingOn.isNullOrBlank())
                        },
                        unfiled = unfiled.size,
                        // The soonest one still ahead. An appointment with no
                        // date is not "next", because nothing about it says
                        // when, and putting it here would be the app inventing
                        // an order the person never gave.
                        nextAppointment = appointments
                            .filter { it.scheduledStart != null }
                            .minByOrNull { it.scheduledStart!! }
                            ?.takeIf {
                                it.scheduledStart!! >= LocalDate.now()
                                    .atStartOfDay(java.time.ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                            },
                        // **Opened over Today, not by switching to the
                        // notebook.** A section is an overlay, so changing the
                        // destination underneath it only decided where "back"
                        // would land, and it landed somewhere the person had
                        // not been.
                        onOpenQuestions = { openSection = Repository.Section.ASK_NEXT_TIME },
                        onOpenProjects = { destination = Destination.PROJECTS },
                        onOpenUnfiled = { trayOpen = true },
                        onOpenAppointments = { openSection = Repository.Section.APPOINTMENTS },
                        onOpenEmergencyCard = { openSection = Repository.Section.EMERGENCY_CARD },
                        hasAnything = (counts?.sumOf { it.count } ?: 0) > 0,
                        digest = digest,
                        lastVisitMillis = lastVisit,
                        onOpenSection = { section -> openSection = section },
                        // **A step disappears once it has been taken**, so the
                        // list is what is left rather than a fixed lecture. The
                        // emergency card is first while it is empty, because it
                        // is the highest value two minutes a new person can
                        // spend and the one thing here useful to somebody else
                        // in a hurry.
                        coaching = coachingSteps(
                            counts = counts,
                            // **What the card screen itself calls filled in.**
                            // The row count only knows whether a card row
                            // exists, so Today went on advising somebody to
                            // fill in a card that already listed a medication.
                            // The two screens now answer the question the same
                            // way, which is the only way they can agree.
                            emergencyCardHasSomething =
                                emergencyCard?.isEmpty == false ||
                                emergencyContacts.isNotEmpty() ||
                                medications.any { it.onEmergencyCard && !it.isStopped },
                            onOpenSection = { section -> openSection = section },
                            onCapture = { sheetOpen = true },
                        ),
                        onSearch = { searchOpen = true },
                        openIncidents = incidents.count { it.isOpen },
                        onOpenIncidents = { incidentsOpen = true },
                    )
                    Destination.PROJECTS -> ProjectsScreen(
                        projects = projects,
                        onOpen = { openProject = it; revision += 1 },
                        onRemove = { project ->
                            removing = Removal(
                                Repository.Section.PROJECTS, project.id, project.name,
                            )
                        },
                        onStart = { startingProject = true },
                    )
                    // More is no longer entirely unbuilt. Appearance is
                    // real; everything else in it still says so plainly.
                    Destination.MORE -> MoreScreen(
                        choice = themeChoice,
                        onChoose = onThemeChoice,
                        onAbout = { aboutOpen = true },
                        onSearch = { searchOpen = true },
                        onLibrary = { libraryOpen = true },
                        onExport = { exportState = ExportState.READY; exportOpen = true },
                        onRestore = {
                            restoreState = RestoreState.Empty
                            restoreFile = null
                            restoreOpen = true
                        },
                    )
                }

                CaptureFab(
                    open = sheetOpen,
                    onClick = { sheetOpen = true },
                    description = strings["capture.button.description"],
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = Space.sm, bottom = Space.sm),
                )
            }

            BottomNav(
                current = destination,
                onSelect = { destination = it },
                labels = {
                    when (it) {
                        Destination.TODAY -> strings["nav.today"]
                        Destination.NOTEBOOK -> strings["nav.notebook"]
                        Destination.PROJECTS -> strings["nav.projects"]
                        Destination.MORE -> strings["nav.more"]
                    }
                },
            )
        }

        // **The system file picker, so the file lands where the person says.**
        // The app never writes to storage it chose itself: an export is theirs
        // and belongs somewhere they will find it again, which is the whole
        // point of D24 calling it the only way back.
        val chooseDestination = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            if (uri == null) {
                // Canceled. Nothing was written and nothing is said, because
                // the person just decided not to.
                exportState = ExportState.READY
                pendingPassphrase = null
            } else {
                writeTo = uri
            }
        }

        if (exportOpen) {
            ExportScreen(
                state = exportState,
                onExport = { passphrase, hint ->
                    pendingPassphrase = passphrase
                    pendingHint = hint
                    exportState = ExportState.WORKING
                    chooseDestination.launch(exportFileName())
                },
                onBack = {
                    exportOpen = false
                    exportState = ExportState.READY
                    pendingPassphrase = null
                    pendingHint = null
                },
                onAgain = { exportState = ExportState.READY },
            )
        }

        // Named for what it is rather than "destination", which is already the
        // navigation state in this composable and shadowed it.
        val exportTarget = writeTo
        if (exportTarget != null) {
            LaunchedEffect(exportTarget) {
                val passphrase = pendingPassphrase
                exportState = withContext(Dispatchers.IO) {
                    runCatching {
                        // **Written to a cache file first, then copied out.**
                        // Backup.export takes a File, and a partial write to
                        // the person's chosen location would leave something
                        // that looks like a backup and is not one.
                        val staged = File(
                            context.cacheDir,
                            "export-${System.currentTimeMillis()}.htx",
                        )
                        try {
                            // **The screen cannot enable Save without a
                            // matching pair**, so an empty passphrase here
                            // would mean the state that produced it was
                            // reached some other way. There is no unencrypted
                            // export to fall back to since format version 2,
                            // D67, so this refuses rather than quietly
                            // writing the whole record in the clear.
                            val chosen = passphrase?.takeIf { it.isNotEmpty() }
                                ?: error("export reached with no passphrase")
                            Backup.export(
                                context = context,
                                target = staged,
                                exportedAt = System.currentTimeMillis(),
                                passphrase = chosen.toCharArray(),
                                passphraseHint = pendingHint,
                            )
                            context.contentResolver.openOutputStream(exportTarget)?.use { out ->
                                staged.inputStream().use { it.copyTo(out) }
                            } ?: error("no output stream")
                        } finally {
                            staged.delete()
                        }
                    }.fold(
                        onSuccess = { ExportState.DONE },
                        onFailure = { ExportState.FAILED },
                    )
                }
                writeTo = null
                pendingPassphrase = null
                pendingHint = null
            }
        }

        val chooseFile = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                val staged = File(context.cacheDir, "restore-source.htx")
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        staged.outputStream().use { input.copyTo(it) }
                    } ?: error("no input stream")
                }.onSuccess {
                    restoreFile = staged
                    openWith = null
                    openNow = true
                }.onFailure {
                    restoreState = RestoreState.Problem(strings["export.failed"])
                }
            }
        }

        if (restoreOpen) {
            RestoreScreen(
                state = restoreState,
                onChoose = {
                    restoreState = RestoreState.Empty
                    chooseFile.launch(arrayOf("*/*"))
                },
                onUnlock = { entered -> openWith = entered; openNow = true },
                onRestore = { applyNow = true },
                onBack = {
                    restoreOpen = false
                    restoreState = RestoreState.Empty
                    restoreFile = null
                },
            )
        }

        // **Reading is separate from applying, and this is the reading half.**
        // Nothing about the notebook changes here, whatever the file turns out
        // to be.
        // **The guard is cleared last, never first.** Setting it false at the
        // top of the effect removes the effect from composition and cancels the
        // coroutine before it finishes, which looked exactly like the file
        // being unreadable: the staging directory filled up and the screen
        // never moved off its empty state.
        if (openNow) {
            val source = restoreFile
            LaunchedEffect(source, openWith) {
                if (source == null) {
                    openNow = false
                    return@LaunchedEffect
                }
                val staging = File(context.cacheDir, "restore-staging")
                val result = ExportContainer.open(
                    file = source,
                    staging = staging,
                    passphrase = openWith?.takeIf { it.isNotEmpty() }?.toCharArray(),
                    expected = Backup.schema(context),
                )
                restoreState = result.fold(
                    onSuccess = { RestoreState.Ready(it.manifest) },
                    onFailure = { failure ->
                        val problem = failure as? ExportContainer.ExportProblem
                        when (val p = problem?.problem) {
                            is ExportContainer.Problem.PassphraseNeeded ->
                                RestoreState.NeedsPassphrase()
                            // **A wrong passphrase leaves them here to try
                            // again.** Retyping is the expected case, and
                            // sending somebody back to pick the file a second
                            // time punishes a typo.
                            is ExportContainer.Problem.CouldNotDecrypt ->
                                RestoreState.NeedsPassphrase(p.message)
                            null -> RestoreState.Problem(
                                failure.message ?: strings["common.error.generic"],
                            )
                            else -> RestoreState.Problem(p.message)
                        }
                    },
                )
                openNow = false
            }
        }

        if (applyNow) {
            val source = restoreFile
            LaunchedEffect(source) {
                if (source == null) {
                    applyNow = false
                    return@LaunchedEffect
                }
                restoreState = RestoreState.Working
                val staging = File(context.cacheDir, "restore-staging")
                val opened = ExportContainer.open(
                    file = source,
                    staging = staging,
                    passphrase = openWith?.takeIf { it.isNotEmpty() }?.toCharArray(),
                    expected = Backup.schema(context),
                )
                restoreState = opened.fold(
                    onSuccess = { container ->
                        Backup.restore(context, container).fold(
                            onSuccess = { RestoreState.Done },
                            onFailure = {
                                RestoreState.Problem(
                                    it.message ?: strings["common.error.generic"],
                                )
                            },
                        )
                    },
                    onFailure = {
                        RestoreState.Problem(it.message ?: strings["common.error.generic"])
                    },
                )
                // Everything on screen came from the database that was just
                // replaced, so it is all reread rather than left showing the
                // notebook that no longer exists.
                revision += 1
                applyNow = false
            }
        }

        // Turning a thread into something a sibling in another state can read.
        // `MASTER_SPEC.md` 4.9: generated locally, handed to the share sheet,
        // no account and no link.
        sharingIncident?.let { incident ->
            LaunchedEffect(incident.id) {
                val entries = repository.incidentTrail(incident.id)
                val subjectName = repository.activeSubject()?.displayName
                val text = Readable.incident(
                    strings = strings,
                    subjectName = subjectName,
                    incident = incident,
                    entries = entries,
                )
                val intent = Share.documentIntent(
                    context = context,
                    fileName = Readable.fileName(
                        title = incident.title,
                        isoDate = java.time.LocalDate.now().toString(),
                        fallback = strings["readable.fallback"],
                    ),
                    text = text,
                    chooserTitle = strings["readable.share.title"],
                )
                sharingIncident = null
                // Nothing to hand over is said rather than shown as a sheet
                // with nothing behind it, per rule 11.
                if (intent != null) context.startActivity(intent) else failed = true
            }
        }

        // The prep sheet as a document, for a meeting or for a sibling.
        sharingPrep?.let { sheet ->
            LaunchedEffect(sheet.appointment.id) {
                val text = Readable.prep(
                    strings = strings,
                    subjectName = repository.activeSubject()?.displayName,
                    prep = sheet,
                )
                val intent = Share.documentIntent(
                    context = context,
                    fileName = Readable.fileName(
                        title = sheet.appointment.title,
                        isoDate = java.time.LocalDate.now().toString(),
                        fallback = strings["readable.fallback"],
                    ),
                    text = text,
                    chooserTitle = strings["readable.share.title"],
                )
                sharingPrep = null
                if (intent != null) context.startActivity(intent) else failed = true
            }
        }

        // Pinning an entry, or taking the pin out. Here rather than in the
        // screen's click handler for the same reason settling an incident is:
        // the screen stays a screen, and the write happens once and off the
        // main thread. The trail is re-read rather than patched, because the
        // pinned run is derived from the rows and a pin held only in memory
        // would be right until the screen was left.
        pinningEntry?.let { (id, pinned) ->
            LaunchedEffect(id, pinned) {
                repository.setEntryPinned(id, pinned)
                pinningEntry = null
                revision += 1
            }
        }

        // Settling an incident, or reopening one. Written here rather than in
        // the screen's click handler so the screen stays a screen and the write
        // happens once, off the main thread, like every other write in this file.
        resolvingIncident?.let { (id, settle) ->
            LaunchedEffect(id, settle) {
                repository.resolveIncident(
                    incidentId = id,
                    resolvedAt = if (settle) System.currentTimeMillis() else null,
                )
                resolvingIncident = null
                revision += 1
                // The open copy is stale the moment it is written, so it is
                // re-read rather than patched in place.
                openIncident = repository.activeSubject()?.id
                    ?.let { subjectId -> repository.incidents(subjectId) }
                    ?.firstOrNull { incident -> incident.id == id }
            }
        }


        if (searchOpen) {
            // Re-run whenever the words change, and whenever the notebook does,
            // so a result the person just corrected is not stale behind them.
            LaunchedEffect(searchQuery, revision) {
                // The subject is read here rather than held in state, because
                // it is read the same way everywhere else in this file and one
                // more piece of shell state is one more thing to keep true.
                val id = repository.activeSubject()?.id
                if (id == null) {
                    searchResults = emptyList()
                    searchFailed = false
                } else {
                    // **A failed search is not an empty search.** Swallowing the
                    // throwable here told the person their record does not
                    // contain what they are certain they wrote down, which is
                    // the single most alarming lie this screen could tell. It
                    // says the read failed and offers it again instead.
                    repository.runCatching { search(id, searchQuery) }
                        .onSuccess { searchResults = it; searchFailed = false }
                        .onFailure {
                            // **A cancellation is not a failure.** Every
                            // keystroke cancels the previous read, so treating
                            // the cancellation as an error flashed "the search
                            // could not run" between letters on a search that
                            // was working perfectly. Rethrown so the coroutine
                            // machinery sees it, which is also what keeps the
                            // effect cancellable at all.
                            if (it is kotlinx.coroutines.CancellationException) throw it
                            android.util.Log.w("HealthTrail", "search failed", it)
                            searchResults = emptyList()
                            searchFailed = true
                        }
                }
            }
            SearchScreen(
                // Named from where they actually are, per the same rule the
                // section screens follow one block below.
                backLabelKey = when (destination) {
                    Destination.TODAY -> "section.back.today"
                    Destination.MORE -> "section.back.more"
                    Destination.PROJECTS -> "section.back.projects"
                    Destination.NOTEBOOK -> "section.back"
                },
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults,
                failed = searchFailed,
                onOpen = { hit ->
                    // **A trail result opens the entry itself**, which is what
                    // journey five asks for: every line taps through to the
                    // thing it came from. Opening the section and leaving the
                    // person to find the row again was the closest this could
                    // get before an entry could be opened at all.
                    //
                    // Everything else still opens its section, because those
                    // sections are lists of the thing rather than lists of
                    // entries, and each of them owns its own detail. Those are
                    // the next ones to land.
                    searchOpen = false
                    if (hit.section == Repository.Section.TRAIL) {
                        openEntry = hit.id
                    } else {
                        openSection = hit.section
                    }
                },
                onBack = { searchOpen = false },
            )
        }

        if (aboutOpen) {
            AboutScreen(onBack = { aboutOpen = false })
        }

        val currentProject = openProject
        if (currentProject != null) {
            ProjectDetailScreen(
                project = currentProject,
                steps = projectSteps,
                onToggleStep = { togglingStep = it },
                onSetStatus = { status -> settingProjectStatus = currentProject.id to status },
                onSetWaitingOn = { who -> settingWaitingOn = currentProject.id to who },
                onBack = { openProject = null },
                // **The template's own name, looked up rather than stored.**
                // `project.template_id` is the catalog id, and the person needs
                // the words. A project whose template is not in this build's
                // catalog, which an export from a later version can produce,
                // shows no provenance line rather than an id.
                templateName = currentProject.templateId?.let { id ->
                    projectTemplates.firstOrNull { it.id == id }?.name
                },
                onAddStep = { text -> addingStep = currentProject.id to text },
                onEditStep = { id, text, note -> editingStep = Triple(id, text, note) },
                onMoveStep = { id, earlier -> movingStep = id to earlier },
                onRemoveStep = { step -> removingStep = step.id },
                onSaveAsTemplate = { savingTemplate = currentProject },
                savedAsTemplate = currentProject.id in savedTemplates,
            )
        }

        if (startingProject) {
            StartProjectScreen(
                templates = projectTemplates,
                onChoose = { template ->
                    startingProject = false
                    chosenTemplate = template
                },
                onCancel = { startingProject = false },
                onStartOwn = { name ->
                    startingProject = false
                    creatingOwnProject = name
                },
                own = ownTemplates,
                onChooseOwn = { template ->
                    startingProject = false
                    startingFromOwn = template
                },
            )
        }

        creatingOwnProject?.let { name ->
            LaunchedEffect(name) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    val id = repository.createProject(subject.id, name)
                    // **Opened straight away**, because a project with no steps
                    // is a screen the person has to fill in, and sending them
                    // back to a list to find it again is a tap paid for
                    // nothing. Read back rather than assembled here, so what
                    // opens is what the database holds.
                    openProject = repository.projects(subject.id).firstOrNull { it.id == id }
                }
                creatingOwnProject = null
                revision += 1
            }
        }

        savingTemplate?.let { project ->
            LaunchedEffect(project.id) {
                repository.saveProjectAsTemplate(project.id, project.name)
                savedTemplates = savedTemplates + project.id
                savingTemplate = null
                revision += 1
            }
        }

        if (libraryOpen) {
            TemplateLibraryScreen(
                shipped = projectTemplates,
                own = ownTemplates,
                projects = projects,
                onOpenProject = { project ->
                    libraryOpen = false
                    openProject = project
                },
                onBack = { libraryOpen = false },
            )
        }

        startingFromOwn?.let { template ->
            LaunchedEffect(template.id) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // **The template's own id**, so the library can say what it
                    // produced and the project can say where it came from.
                    repository.startProject(
                        subjectId = subject.id,
                        templateId = template.id,
                        name = template.name,
                        steps = template.steps,
                    )
                }
                startingFromOwn = null
                revision += 1
            }
        }

        val template = chosenTemplate
        if (template != null) {
            LaunchedEffect(template) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    repository.startProject(
                        subjectId = subject.id,
                        templateId = template.id,
                        name = template.name,
                        steps = template.steps,
                    )
                }
                chosenTemplate = null
                revision += 1
            }
        }

        val step = togglingStep
        if (step != null) {
            LaunchedEffect(step) {
                repository.setProjectStepDone(step.id, !step.isDone)
                togglingStep = null
                revision += 1
            }
        }

        addingStep?.let { pending ->
            LaunchedEffect(pending) {
                repository.addProjectStep(pending.first, pending.second)
                addingStep = null
                revision += 1
            }
        }

        editingStep?.let { pending ->
            LaunchedEffect(pending) {
                repository.updateProjectStep(pending.first, pending.second, pending.third)
                editingStep = null
                revision += 1
            }
        }

        movingStep?.let { pending ->
            LaunchedEffect(pending) {
                repository.moveProjectStep(pending.first, pending.second)
                movingStep = null
                revision += 1
            }
        }

        removingStep?.let { id ->
            LaunchedEffect(id) {
                repository.deleteProjectStep(id)
                removingStep = null
                revision += 1
            }
        }

        val waitingChange = settingWaitingOn
        if (waitingChange != null) {
            LaunchedEffect(waitingChange) {
                val current = projects.firstOrNull { it.id == waitingChange.first }
                repository.setProjectStatus(
                    projectId = waitingChange.first,
                    // Naming somebody does not by itself declare the project
                    // stalled, so the status is left exactly as it was.
                    status = current?.status ?: "active",
                    waitingOn = waitingChange.second,
                )
                settingWaitingOn = null
                revision += 1
            }
        }

        val statusChange = settingProjectStatus
        if (statusChange != null) {
            LaunchedEffect(statusChange) {
                repository.setProjectStatus(
                    projectId = statusChange.first,
                    status = statusChange.second,
                    // What it is waiting on is its own question and is not
                    // invented here. Choosing "waiting" records the status and
                    // the moment; naming who comes later.
                    waitingOn = projects.firstOrNull { it.id == statusChange.first }?.waitingOn,
                )
                settingProjectStatus = null
                revision += 1
            }
        }

        // **A section, opened from the table of contents.**
        //
        // The trail and the care team are built. The other ten open onto the
        // same honest interim screen the unbuilt destinations use, which names
        // the section and says plainly that it is not built. That is worse than
        // having them all built and far better than a row that swallows a tap:
        // rule 16 says a control that does nothing on press reads as broken,
        // and until tonight all twelve did nothing.
        //
        // Each of these disappears as its section lands, and ShellTags.NOT_BUILT
        // makes the remainder greppable so none can survive to release.
        // **One place decides what every section's way back says.** They are
        // opened from the notebook and from Today, and a screen reached from
        // Today whose only way back said "Back to the notebook" named a place
        // the person had not come from.
        CompositionLocalProvider(
            LocalSectionBackKey provides when (destination) {
                Destination.TODAY -> "section.back.today"
                else -> "section.back"
            },


        ) {
            when (openSection) {
                null -> Unit

                Repository.Section.TRAIL -> TrailScreen(
                    entries = trail,
                    onOpen = { openEntry = it.id },
                    // **Pinning writes and then reloads, like every other write
                    // in the shell.** The pinned run is derived from the rows
                    // rather than held beside them, so a pin that only moved a
                    // flag in memory would be correct until the screen was left
                    // and wrong afterward.
                    onBack = { openSection = null },
                )

                Repository.Section.DOCUMENTS -> DocumentsScreen(
                    documents = documents,
                    onRemove = { document ->
                        removing = Removal(
                            Repository.Section.DOCUMENTS, document.id, document.title,
                        )
                    },
                    onEdit = { document ->
                        editingDocument = document
                        documentError = null
                        addingDocument = true
                    },
                    onAdd = {
                        editingDocument = null
                        documentError = null
                        addingDocument = true
                    },
                    onBack = { openSection = null },
                )

                Repository.Section.MONEY -> MoneyScreen(
                    bills = bills,
                    onRemove = { bill ->
                        removing = Removal(Repository.Section.MONEY, bill.id, bill.description)
                    },
                    onEdit = { bill -> editingBill = bill; addingBill = true },
                    onAdd = { editingBill = null; addingBill = true },
                    onBack = { openSection = null },
                )

                Repository.Section.STANDING_INSTRUCTIONS -> StandingInstructionsScreen(
                    instructions = instructions,
                    tags = instructionCatalog?.tags.orEmpty(),
                    onRemove = { instruction ->
                        removing = Removal(
                            Repository.Section.STANDING_INSTRUCTIONS,
                            instruction.id,
                            instruction.name,
                        )
                    },
                    onAcknowledge = { acknowledging = it },
                    onAdd = { addingInstruction = true },
                    onBack = { openSection = null },
                    violations = violationCounts,
                    onRecordViolation = { recordingViolationFor = it },
                )

                Repository.Section.APPOINTMENTS -> AppointmentsScreen(
                    appointments = appointments,
                    // Midnight this morning, so something scheduled earlier today
                    // still reads as coming up rather than dropping into the past
                    // the moment its hour passes.
                    todayMillis = LocalDate.now()
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                    onRemove = { appointment ->
                        removing = Removal(
                            Repository.Section.APPOINTMENTS, appointment.id, appointment.title,
                        )
                    },
                    onOpen = { appointment -> openPrepFor = appointment.id },
                    onAdd = { editingAppointment = null; addingAppointment = true },
                    onBack = { openSection = null },
                )

                Repository.Section.CHAPTERS -> ChaptersScreen(
                    onOpen = { openChapter = it },
                    chapters = chapters,
                    onBack = { openSection = null },
                )

                Repository.Section.PROGRESS -> ProgressScreen(
                    measures = measures,
                    readings = readings,
                    onBack = { openSection = null },
                )

                Repository.Section.THREADS -> CareThreadsScreen(
                    onOpen = { openThread = it },
                    threads = threadCounts,
                    onBack = { openSection = null },
                )

                Repository.Section.ASK_NEXT_TIME -> QuestionsScreen(
                    questions = questions,
                    onRemove = { question ->
                        removing = Removal(Repository.Section.ASK_NEXT_TIME, question.id, question.text)
                    },
                    // Marked asked as of today, which is the honest default: the
                    // person is tapping it because it just happened. The date is
                    // editable later like every other date, per rule 17.
                    onMarkAsked = { markingAsked = it },
                    onAnswer = { answering = it },
                    onBack = { openSection = null },
                )

                Repository.Section.MEDICATIONS -> MedicationsScreen(
                    medications = medications,
                    onRemove = { medication ->
                        removing = Removal(
                            Repository.Section.MEDICATIONS, medication.id, medication.name,
                        )
                    },
                    onOpen = { medication -> openMedication = medication },
                    onAdd = { editingMedication = null; addingMedication = true },
                    onBack = { openSection = null },
                )

                Repository.Section.EMERGENCY_CARD -> EmergencyCardScreen(
                    card = emergencyCard,
                    contacts = emergencyContacts,
                    // **The other half of the link, per rule 18.** A medication
                    // knows it is on the card, and the card is assembled from the
                    // medications that say so, so neither has to be kept in step
                    // with the other. A stopped medication drops off the card by
                    // itself, which is the behavior somebody would expect and the
                    // one that is dangerous to get wrong.
                    medications = medications.filter { it.showsOnEmergencyCard },
                    onCall = { contact -> dial(context, contact.phone) },
                    onEdit = { editingEmergencyCard = true },
                    onBack = { openSection = null },
                )

                Repository.Section.CARE_TEAM -> CareTeamScreen(
                    people = people,
                    // ACTION_DIAL rather than ACTION_CALL, which would need the
                    // call permission. The dialer opens with the number filled in
                    // and the person presses the green button themselves. That is
                    // one extra tap and it is the right one: this app asks for no
                    // permission it does not need, and placing a call on somebody's
                    // behalf is not a thing it should be able to do silently.
                    onCall = { person -> dial(context, person.phone) },
                    onRemove = { person ->
                        removing = Removal(
                            section = Repository.Section.CARE_TEAM,
                            rowId = person.id,
                            what = person.displayName.ifBlank {
                                person.phone.orEmpty().ifBlank { person.roleLabel.orEmpty() }
                            },
                        )
                    },
                    onOpen = { person -> openPerson = person },
                    onAdd = { editingPerson = null; addingPerson = true },
                    onBack = { openSection = null },
                )

                else -> {
                    val section = openSection!!
                    NotBuiltYet(
                        name = strings[labelKey(section)],
                        onClose = { openSection = null },
                    )
                }
            }
        }

        // Declared here for the same z-order reason as the entry screen
        // below: an incident opened from an entry that was opened from the
        // trail has the trail painted over it otherwise, and the tap reads as
        // doing nothing.
        if (incidentsOpen) {
            val current = openIncident
            if (current == null) {
                IncidentsScreen(
                    incidents = incidents,
                    onOpen = { openIncident = it },
                    onBack = { incidentsOpen = false },
                )
            } else {
                LaunchedEffect(current.id, revision) {
                    incidentEntries = repository.incidentTrail(current.id)
                    incidentPeople = repository.peopleOnIncident(current.id)
                    incidentDocuments = repository.documentsOnIncident(current.id)
                }
                IncidentScreen(
                    incident = current,
                    entries = incidentEntries,
                    people = incidentPeople,
                    documents = incidentDocuments,
                    onOpenPerson = { person ->
                        openIncident = null
                        incidentsOpen = false
                        openPerson = person
                    },
                    onAdd = {
                        // **Carries the incident forward rather than asking
                        // again.** Part Two: a prefill is a default the person
                        // can change, and making somebody re-say which incident
                        // they are already looking at is the app not paying
                        // attention.
                        addingToIncident = current.id
                        capturing = CaptureKind.CALL
                    },
                    onShare = { sharingIncident = current },
                    onResolve = { resolvingIncident = current.id to true },
                    onReopen = { resolvingIncident = current.id to false },
                    onBack = { openIncident = null },
                )
            }
        }

        // **Rendered after the section screens, which is what puts it on
        // top.** Placed above them it drew underneath the trail, so tapping a
        // row appeared to do nothing at all: the entry screen was there and
        // the trail was painted over it. These are overlays in one Box, so
        // order is z-order, and the thing opened last has to be declared last.
        savingMedicationEvent?.let { draft ->
            LaunchedEffect(draft) {
                val subjectId = repository.activeSubject()?.id
                repository.recordMedicationEvent(
                    medicationId = draft.medicationId,
                    kind = draft.kind,
                    occurred = draft.occurred,
                    doseText = draft.doseText,
                    note = draft.note,
                    // **Stamped from where they are now**, which is what makes
                    // a medication's journey cross chapters at all.
                    chapterId = subjectId?.let { repository.currentChapterId(it) },
                )
                savingMedicationEvent = null
                revision += 1
            }
        }


        savingViolation?.let { draft ->
            LaunchedEffect(draft) {
                repository.recordViolation(
                    instructionId = draft.instructionId,
                    occurred = draft.occurred,
                    note = draft.note,
                )
                savingViolation = null
                revision += 1
            }
        }

        recordingViolationFor?.let { instruction ->
            ViolationScreen(
                instruction = instruction,
                onSave = { draft -> savingViolation = draft; recordingViolationFor = null },
                onCancel = { recordingViolationFor = null },
            )
        }

        openMedication?.let { medication ->
            LaunchedEffect(medication.id, revision) {
                medicationHistory = repository.medicationHistory(medication.id)
                medicationQuestions = repository.openQuestionsAbout(medication.id)
            }
            MedicationScreen(
                medication = medication,
                history = medicationHistory,
                questions = medicationQuestions,
                // **The other half of the link, per rule 18.** A question opens
                // its own entry, which is where it can be dated, read, and
                // followed back to whatever else it belongs to.
                onOpenQuestion = { question ->
                    openMedication = null
                    openEntry = question.entryId
                    if (question.entryId == null) {
                        openSection = Repository.Section.ASK_NEXT_TIME
                    }
                },
                onEdit = {
                    editingMedication = medication
                    addingMedication = true
                    openMedication = null
                },
                onRecordChange = { recordingChangeTo = medication },
                onBack = { openMedication = null },
            )
        }

        // **After the medication screen it opens from, because order is
        // z-order.** Declared before it, this drew underneath and the button
        // read as doing nothing, which is the third time that trap has been
        // hit tonight: the entry screen, an incident opened from an entry,
        // and now this. Anything opened from an overlay is declared after it.
        recordingChangeTo?.let { medication ->
            MedicationEventScreen(
                medicationId = medication.id,
                medicationName = medication.name,
                onSave = { draft ->
                    savingMedicationEvent = draft
                    recordingChangeTo = null
                },
                onCancel = { recordingChangeTo = null },
            )
        }

        openChapter?.let { chapter ->
            LaunchedEffect(chapter.id, revision) {
                val subjectId = repository.activeSubject()?.id
                chapterDetail = subjectId?.let { repository.chapterDetail(it, chapter.id) }
                if (chapterDetail == null) openChapter = null
            }
            chapterDetail?.takeIf { it.chapter.id == chapter.id }?.let { detail ->
                ChapterScreen(
                    detail = detail,
                    onOpenEntry = { openChapter = null; openEntry = it.id },
                    onOpenIncident = {
                        openChapter = null
                        openIncident = it
                        incidentsOpen = true
                    },
                    onBack = { openChapter = null },
                )
            }
        }

        openThread?.let { thread ->
            LaunchedEffect(thread.id, revision) {
                threadEntries = repository.entriesForThread(thread.id)
            }
            ThreadScreen(
                thread = thread,
                entries = threadEntries,
                onOpenEntry = { openThread = null; openEntry = it.id },
                onBack = { openThread = null },
            )
        }

        openPrepFor?.let { appointmentId ->
            LaunchedEffect(appointmentId, revision) {
                val subjectId = repository.activeSubject()?.id
                prep = subjectId?.let { repository.prep(it, appointmentId) }
                if (prep == null) openPrepFor = null
            }
            prep?.takeIf { it.appointment.id == appointmentId }?.let { sheet ->
                PrepScreen(
                    prep = sheet,
                    onOpenEntry = { openPrepFor = null; openEntry = it.id },
                    onShare = { sharingPrep = sheet },
                    onWriteUp = {
                        // The ordinary capture form, so what comes out is an
                        // ordinary entry on the trail rather than a special
                        // kind only this screen knows about.
                        capturing = CaptureKind.VISIT
                    },
                    onBack = { openPrepFor = null },
                )
            }
        }

        openPerson?.let { person ->
            LaunchedEffect(person.id, revision) {
                personEntries = repository.entriesForPerson(person.id)
            }
            PersonScreen(
                person = person,
                entries = personEntries,
                onCall = { number -> dial(context, number) },
                onEdit = { editingPerson = person; addingPerson = true; openPerson = null },
                onOpenEntry = { openPerson = null; openEntry = it.id },
                onBack = { openPerson = null },
                backLabelKey = "section.back.careteam",
            )
        }

        openEntry?.let { entryId ->
            LaunchedEffect(entryId, revision) {
                entryDetail = repository.entry(entryId)
                // An entry that is gone closes rather than showing a blank
                // screen, which is what a removal from underneath looks like.
                if (entryDetail == null) openEntry = null
            }
            entryDetail?.takeIf { it.entry.id == entryId }?.let { detail ->
                EntryScreen(
                    detail = detail,
                    backLabelKey = if (openSection != null) "section.back.trail" else "section.back",
                    onEditDate = { editingDate = detail.entry },
                    onSetPinned = { pinned -> pinningEntry = detail.entry.id to pinned },
                    onOpenPerson = { openEntry = null; openPerson = it },
                    onOpenThread = { thread ->
                        // Both ways, and precisely: the entry names its thread
                        // and the thread itself opens, rather than the list it
                        // is in. Rule 18.
                        openEntry = null
                        openThread = thread
                    },
                    onOpenChapter = {
                        // The chapter itself rather than the list of them,
                        // which is the difference between a link and a
                        // signpost. Rule 18.
                        openEntry = null
                        openChapter = chapters.firstOrNull {
                            it.id == detail.chapterId
                        }
                        if (openChapter == null) openSection = Repository.Section.CHAPTERS
                    },
                    onOpenIncident = {
                        openEntry = null
                        openIncident = incidents.firstOrNull { it.id == detail.incidentId }
                        incidentsOpen = openIncident != null
                    },
                    // Back to what the question is about, which closes the loop
                    // the medication screen opened.
                    onOpenMedication = {
                        openEntry = null
                        openMedication = medications.firstOrNull {
                            it.id == detail.medicationId
                        }
                        if (openMedication == null) {
                            openSection = Repository.Section.MEDICATIONS
                        }
                    },
                    onRemove = {
                        removing = Removal(
                            section = Repository.Section.TRAIL,
                            rowId = detail.entry.id,
                            // **The words the person is looking at.** Most
                            // entries have no title, and naming the row by its
                            // title alone asked "remove what?" with a blank
                            // where the answer goes.
                            what = headingFor(detail.entry, strings["entry.untitled"]).text,
                        )
                        openEntry = null
                    },
                    onBack = { openEntry = null },
                )
            }
        }


        // Correcting when something happened, from the entry itself, per rule
        // 17. The same picker every other date in the app opens, so a date is
        // edited the way it was entered rather than through a second control
        // that behaves almost the same.
        val toRemove = removing
        if (toRemove != null) {
            ConfirmRemoveSheet(
                what = toRemove.what,
                onConfirm = {
                    confirmedRemoval = toRemove
                    removing = null
                },
                onDismiss = { removing = null },
            )
        }

        val confirmed = confirmedRemoval
        if (confirmed != null) {
            LaunchedEffect(confirmed) {
                // A tombstone, per the data contract, which the repository
                // handles. Nothing here knows or needs to know that.
                repository.delete(confirmed.section, confirmed.rowId)
                confirmedRemoval = null
                revision += 1
            }
        }

        val editing = editingDate
        if (editing != null) {
            DatePickerSheet(
                initial = editing.occurredEdtf?.let { Edtf.parse(it) },
                onPick = { picked ->
                    editingDate = null
                    correcting = editing.id to picked
                },
                onDismiss = { editingDate = null },
            )
        }

        if (addingPerson) {
            AddPersonScreen(
                roleSuggestions = roleSuggestions,
                existing = editingPerson,
                onSave = { draft ->
                    addingPerson = false
                    savingPerson = draft
                },
                onCancel = { addingPerson = false; editingPerson = null },
            )
        }

        if (editingEmergencyCard) {
            EmergencyCardEditScreen(
                card = emergencyCard,
                people = people,
                onTheCard = emergencyContacts.mapNotNull { it.personId }.toSet(),
                onToggleContact = { togglingContact = it },
                onSave = { draft ->
                    editingEmergencyCard = false
                    savingEmergencyCard = draft
                },
                onCancel = { editingEmergencyCard = false },
            )
        }

        val toAcknowledge = acknowledging
        if (toAcknowledge != null) {
            AcknowledgeSheet(
                instruction = toAcknowledge,
                onSave = { how ->
                    savingAcknowledgment = toAcknowledge.id to how
                    acknowledging = null
                },
                onDismiss = { acknowledging = null },
            )
        }

        val ackToSave = savingAcknowledgment
        if (ackToSave != null) {
            LaunchedEffect(ackToSave) {
                repository.setInstructionAcknowledged(
                    instructionId = ackToSave.first,
                    how = ackToSave.second,
                    acknowledged = Edtf.day(LocalDate.now()),
                )
                savingAcknowledgment = null
                revision += 1
            }
        }

        val toAnswer = answering
        if (toAnswer != null) {
            AnswerSheet(
                question = toAnswer,
                onSave = { text ->
                    savingAnswer = toAnswer.id to text
                    answering = null
                },
                onDismiss = { answering = null },
            )
        }

        val answerToSave = savingAnswer
        if (answerToSave != null) {
            LaunchedEffect(answerToSave) {
                repository.setQuestionAnswer(answerToSave.first, answerToSave.second)
                savingAnswer = null
                revision += 1
            }
        }

        val asked = markingAsked
        if (asked != null) {
            LaunchedEffect(asked) {
                repository.markQuestionAsked(asked.id, Edtf.day(LocalDate.now()))
                markingAsked = null
                revision += 1
            }
        }

        if (addingDocument) {
            AddDocumentScreen(
                existing = editingDocument,
                error = documentError,
                onSave = { draft -> savingDocument = draft },
                onCancel = {
                    addingDocument = false
                    editingDocument = null
                    documentError = null
                },
            )
        }

        val documentDraft = savingDocument
        if (documentDraft != null) {
            LaunchedEffect(documentDraft) {
                val subject = repository.activeSubject()
                val title = documentDraft.title.trim()
                val correctingDocument = editingDocument
                if (correctingDocument != null && title.isNotBlank()) {
                    // **Correcting the words never touches the photograph.**
                    // Replacing an image is a different action with different
                    // consequences, since the file may be shared with another
                    // row by its hash, and conflating the two would make a text
                    // correction quietly capable of losing an image.
                    repository.updateDocument(
                        documentId = correctingDocument.id,
                        title = title,
                        originalLocation = documentDraft.originalLocation,
                        notes = documentDraft.notes,
                    )
                    addingDocument = false
                    capturing = null
                    editingDocument = null
                    documentError = null
                } else if (subject != null && title.isNotBlank()) {
                    // **The bytes go to disk before the rows exist**, so a
                    // document row can never point at a file that was never
                    // written. Attachments is content addressed, so the same
                    // photograph chosen twice is one file rather than two.
                    val picked = documentDraft.picked
                    val file = if (picked == null) null else storePicked(context, picked)

                    if (picked != null && file == null) {
                        // Refused or unreadable. Nothing is written and the
                        // screen says so, because the worst outcome here is
                        // somebody believing a document went in when it did not.
                        documentError = strings["docs.too_large"]
                    } else {
                        repository.createDocument(
                            subjectId = subject.id,
                            title = title,
                            received = Edtf.day(LocalDate.now()),
                            originalLocation = documentDraft.originalLocation,
                            notes = documentDraft.notes,
                            sha256 = file?.sha256,
                            byteSize = file?.byteSize ?: 0,
                            mimeType = picked?.let { context.contentResolver.getType(it) },
                        )
                        addingDocument = false
                        capturing = null
                        documentError = null
                    }
                } else {
                    addingDocument = false
                }
                savingDocument = null
                revision += 1
            }
        }

        if (addingBill) {
            AddBillScreen(
                existing = editingBill,
                onSave = { draft ->
                    addingBill = false
                    savingBill = draft
                },
                onCancel = { addingBill = false },
            )
        }

        val billDraft = savingBill
        if (billDraft != null) {
            LaunchedEffect(billDraft) {
                val subject = repository.activeSubject()
                // A bill with no description is not a record of anything. The
                // amount stays optional, because a bill that has not said one
                // yet is the common case rather than the exception.
                val correctingBill = editingBill
                if (correctingBill != null && billDraft.description.isNotBlank()) {
                    repository.updateBill(
                        billId = correctingBill.id,
                        description = billDraft.description.trim(),
                        amountMinor = parseAmountToMinor(billDraft.amount),
                        state = billDraft.state,
                        notes = billDraft.notes,
                    )
                } else if (subject != null && billDraft.description.isNotBlank()) {
                    repository.createBill(
                        subjectId = subject.id,
                        description = billDraft.description.trim(),
                        amountMinor = parseAmountToMinor(billDraft.amount),
                        state = billDraft.state,
                        received = Edtf.day(LocalDate.now()),
                        notes = billDraft.notes,
                    )
                }
                editingBill = null
                savingBill = null
                revision += 1
            }
        }

        val catalog = instructionCatalog
        if (addingInstruction && catalog != null) {
            AddInstructionScreen(
                catalog = catalog,
                onChoose = { starter ->
                    addingInstruction = false
                    savingInstruction = starter
                },
                onCancel = { addingInstruction = false },
            )
        }

        val instructionDraft = savingInstruction
        if (instructionDraft != null) {
            LaunchedEffect(instructionDraft) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // Recorded as asked today, because the person is tapping it
                    // at the moment they ask. The wording and the tag are
                    // copied from the catalog rather than referenced, so a
                    // later catalog edit never rewrites what somebody asked for.
                    repository.createStandingInstruction(
                        subjectId = subject.id,
                        templateId = instructionDraft.id,
                        name = instructionDraft.name,
                        wording = instructionDraft.wording,
                        tag = instructionDraft.tag,
                        given = Edtf.day(LocalDate.now()),
                    )
                }
                savingInstruction = null
                revision += 1
            }
        }

        if (addingAppointment) {
            AddAppointmentScreen(
                existing = editingAppointment,
                onSave = { draft ->
                    addingAppointment = false
                    savingAppointment = draft
                },
                onCancel = { addingAppointment = false },
            )
        }

        val appointmentDraft = savingAppointment
        if (appointmentDraft != null) {
            LaunchedEffect(appointmentDraft) {
                val subject = repository.activeSubject()
                // A date with no name is not an appointment. A name with no
                // date is: "the care plan meeting, not scheduled yet" is worth
                // writing down before it slips.
                val correctingAppointment = editingAppointment
                if (correctingAppointment != null && appointmentDraft.title.isNotBlank()) {
                    repository.updateAppointment(
                        appointmentId = correctingAppointment.id,
                        title = appointmentDraft.title.trim(),
                        scheduled = appointmentDraft.scheduled ?: Edtf.unknown(),
                        locationNote = appointmentDraft.where,
                        notes = appointmentDraft.notes,
                    )
                } else if (subject != null && appointmentDraft.title.isNotBlank()) {
                    repository.createAppointment(
                        subjectId = subject.id,
                        title = appointmentDraft.title.trim(),
                        scheduled = appointmentDraft.scheduled ?: Edtf.unknown(),
                        locationNote = appointmentDraft.where,
                        notes = appointmentDraft.notes,
                    )
                }
                editingAppointment = null
                savingAppointment = null
                revision += 1
            }
        }

        if (addingMedication) {
            AddMedicationScreen(
                existing = editingMedication,
                onSave = { draft ->
                    addingMedication = false
                    savingMedication = draft
                },
                onCancel = { addingMedication = false; editingMedication = null },
            )
        }

        val medicationDraft = savingMedication
        if (medicationDraft != null) {
            LaunchedEffect(medicationDraft) {
                val subject = repository.activeSubject()
                // The name is the one thing a medication row cannot do without,
                // since the schema requires it and a row with no name is not a
                // record of anything. Everything else is optional. Saving with
                // nothing typed writes nothing and says nothing about it, the
                // same as the care team.
                val correcting = editingMedication
                if (correcting != null && medicationDraft.name.isNotBlank()) {
                    repository.updateMedication(
                        medicationId = correcting.id,
                        name = medicationDraft.name.trim(),
                        doseText = medicationDraft.dose,
                        purposeText = medicationDraft.purpose,
                        notes = medicationDraft.notes,
                        onEmergencyCard = medicationDraft.onEmergencyCard,
                    )
                } else if (subject != null && medicationDraft.name.isNotBlank()) {
                    repository.createMedication(
                        subjectId = subject.id,
                        name = medicationDraft.name.trim(),
                        doseText = medicationDraft.dose,
                        purposeText = medicationDraft.purpose,
                        notes = medicationDraft.notes,
                        onEmergencyCard = medicationDraft.onEmergencyCard,
                    )
                }
                editingMedication = null
                savingMedication = null
                revision += 1
            }
        }

        val toggling = togglingContact
        if (toggling != null) {
            LaunchedEffect(toggling) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // The card row has to exist before anybody can be put on
                    // it, and the person may be doing this before they have
                    // typed a single field. Creating it here rather than
                    // refusing is the difference between the app absorbing its
                    // own storage model and asking the person to understand it,
                    // which is rule 20.
                    val cardId = emergencyCard?.id
                        ?: repository.saveEmergencyCard(subjectId = subject.id)
                    val existing = repository.emergencyContacts(cardId)
                        .firstOrNull { it.personId == toggling.id }
                    if (existing != null) {
                        repository.removeEmergencyContact(existing.id)
                    } else {
                        repository.addEmergencyContact(
                            cardId = cardId,
                            personId = toggling.id,
                            // Copied onto the card rather than read through the
                            // link, so archiving a care team row never blanks
                            // the card. A person with no name is carried by
                            // whatever they do have.
                            displayName = toggling.displayName.ifBlank {
                                toggling.phone.orEmpty().ifBlank {
                                    toggling.roleLabel.orEmpty()
                                }
                            },
                            phone = toggling.phone,
                            relationship = toggling.roleLabel,
                            sortIndex = repository.emergencyContacts(cardId).size,
                        )
                    }
                }
                togglingContact = null
                revision += 1
            }
        }

        val emergencyDraft = savingEmergencyCard
        if (emergencyDraft != null) {
            LaunchedEffect(emergencyDraft) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // Every field goes through, including the blank ones, so
                    // clearing something that turned out to be wrong actually
                    // clears it. The repository stores blank as null.
                    repository.saveEmergencyCard(
                        subjectId = subject.id,
                        allergies = emergencyDraft.allergies,
                        bloodType = emergencyDraft.bloodType,
                        conditions = emergencyDraft.conditions,
                        resuscitationStatus = emergencyDraft.resuscitationStatus,
                        resuscitationDocumentLocation = emergencyDraft.resuscitationWhere,
                        decisionMakerDocumentLocation = emergencyDraft.decisionMakerWhere,
                        insuranceNote = emergencyDraft.insurance,
                        otherNotes = emergencyDraft.other,
                    )
                }
                savingEmergencyCard = null
                revision += 1
            }
        }

        val person = savingPerson
        if (person != null) {
            LaunchedEffect(person) {
                val subject = repository.activeSubject()
                // **Nothing at all is not a partial answer, it is a stray tap.**
                // Rule 13 makes an unfilled field a finished state, which is why
                // any one of these alone writes a real row. All three empty is a
                // different thing: there is nothing to keep, so nothing is
                // written and nothing is said about it either.
                val anything = listOf(person.name, person.role, person.phone)
                    .any { it.isNotBlank() }
                val correcting = editingPerson
                if (correcting != null) {
                    // A correction may legitimately empty every field except
                    // the name, so the all-blank guard below does not apply:
                    // the row already exists and the person is changing it.
                    repository.updatePerson(
                        personId = correcting.id,
                        displayName = person.name.trim(),
                        phone = person.phone.trim(),
                        roleLabel = person.role.trim(),
                    )
                } else if (subject != null && anything) {
                    repository.createPerson(
                        subjectId = subject.id,
                        displayName = person.name.trim(),
                        phone = person.phone.trim(),
                        roleLabel = person.role.trim().ifBlank { null },
                    )
                }
                editingPerson = null
                savingPerson = null
                revision += 1
            }
        }

        val correction = correcting
        if (correction != null) {
            LaunchedEffect(correction) {
                repository.updateEntryOccurred(correction.first, correction.second)
                correcting = null
                // Reread, which reorders the trail if the new date moves it.
                revision += 1
            }
        }

        if (trayOpen) {
            UnfiledTrayScreen(
                entries = unfiled,
                threads = threadsForFiling,
                onFile = { entryId, threadId ->
                    filing = entryId to threadId
                },
                onClose = { trayOpen = false },
            )
        }

        val toFile = filing
        if (toFile != null) {
            LaunchedEffect(toFile) {
                repository.fileEntry(toFile.first, toFile.second)
                filing = null
                // Recount, which also refreshes the tray and closes it when the
                // last waiting entry has been filed.
                revision += 1
            }
        }

        if (sheetOpen) {
            CaptureSheet(
                onChoose = { kind ->
                    sheetOpen = false
                    capturing = kind
                },
                onDismiss = { sheetOpen = false },
            )
        }

        // Four kinds, one form. They differ in wording rather than in shape, so
        // they share a screen rather than appearing as four near copies. Which
        // four is declared once, next to the form, rather than repeated here.
        //
        // Measurement and document arrive in their own increments. Until then
        // choosing one closes the sheet and does nothing rather than opening an
        // empty screen, which is the lesser of two bad interim states.
        val kind = capturing
        if (kind != null && kind.usesTheSharedForm) {
            CaptureFormScreen(
                kind = kind,
                threads = threadsForFiling,
                people = peopleForCapture,
                medications = medications,
                state = captureDraft,
                onStateChange = { captureDraft = it },
                onSave = { draft ->
                    capturing = null
                    // **Cleared only once the entry is on its way to the
                    // database.** Cleared any earlier and a save that failed
                    // would take the note with it.
                    captureDraft = CaptureFormState()
                    saving = draft
                },
                onCancel = {
                    capturing = null
                    // **Cancel is the one thing that does discard it**, because
                    // cancel means abandoning the entry. Back does not, which is
                    // the whole point of holding it here. D65 draws the same
                    // distinction for where back goes.
                    captureDraft = CaptureFormState()
                },
            )
        }

        // **Document says so rather than doing nothing.** The sheet offers six
        // ways in and this one is not built, so choosing it used to close the
        // sheet and silently do nothing, which is the app appearing to lose
        // what someone tried to save. It now says plainly that it is not built
        // and why, which is the same honesty the not-yet-built destinations
        // carry, and it is greppable through ShellTags.NOT_BUILT so it cannot
        // survive to release. Issue #57.
        // **The sixth way in, which used to say it was not built.** D44 said an
        // interface may offer something it has not built but may not go quiet
        // when somebody takes it up. It is built now, so the honest interim
        // screen is gone rather than kept alongside it.
        if (kind == CaptureKind.DOCUMENT) {
            AddDocumentScreen(
                error = documentError,
                onSave = { draft ->
                    capturing = null
                    savingDocument = draft
                },
                onCancel = { capturing = null; documentError = null },
            )
        }

        // Measurement has its own screen because it does not fit the shared
        // form: a value needs to know what is being measured before anything
        // else on the screen means anything.
        if (kind == CaptureKind.MEASUREMENT) {
            MeasurementScreen(
                measures = measures,
                presets = presets,
                onSave = { draft ->
                    capturing = null
                    recording = draft
                },
                onCancel = { capturing = null },
            )
        }

        val measurement = recording
        if (measurement != null) {
            LaunchedEffect(measurement) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // A measure is created the first time the person records
                    // one, never at setup. A notebook that arrives with sixteen
                    // empty charts is a list of things somebody has not done.
                    val measureId = measurement.measureId ?: repository.createMeasure(
                        subjectId = subject.id,
                        preset = measurement.preset!!,
                        unit = measurement.unit,
                        sortIndex = measures.size,
                    )
                    repository.recordMeasurement(
                        measureId = measureId,
                        number = measurement.number,
                        text = measurement.text,
                        unit = measurement.unit,
                        occurred = measurement.occurred,
                        note = measurement.note,
                    )
                }
                recording = null
                revision += 1
                destination = Destination.NOTEBOOK
            }
        }

        val draft = saving
        if (draft != null) {
            LaunchedEffect(draft) {
                val subject = repository.activeSubject()
                if (subject != null && draft.kind == CaptureKind.QUESTION) {
                    // **A question is two rows and always has been.** Writing
                    // only the entry put it in the trail and left the Ask next
                    // time section counting zero forever, which is the app
                    // being wrong about itself. Both, in one transaction.
                    val (questionEntryId, _) = repository.createQuestionWithEntry(
                        subjectId = subject.id,
                        // The form's two fields map straight across: what you
                        // want to ask is the question, and who it is for is who
                        // it is for. Folding them into one string lost the
                        // column the schema keeps precisely so a question can be
                        // filtered to the person it is waiting on.
                        text = draft.note,
                        roleLabel = draft.who,
                        occurred = draft.occurred,
                        threadId = draft.threadId,
                        isUnfiled = threads.isNotEmpty() && draft.threadId == null,
                        medicationId = draft.medicationId,
                    )
                    // **The same link every other kind writes.** Choosing the
                    // charge nurse from the chips on a question recorded her
                    // name in `role_label` and stopped there, so the question
                    // named her and she did not know about it, and her page
                    // could not show what was waiting to be asked of her. Only
                    // the five other kinds wrote `entry_person`, which made this
                    // one an exception nobody had decided on.
                    draft.personId?.let { repository.linkEntryToPerson(questionEntryId, it) }
                } else if (subject != null && draft.kind == CaptureKind.INCIDENT) {
                    // **An incident is two rows for the same reason a question
                    // is.** `MASTER_SPEC.md` 4.7 makes it a thread from first
                    // report to resolution, and one written only as an entry
                    // can never be escalated, resolved, or exported as a
                    // thread. It was an entry with a scary kind until now.
                    repository.reportIncident(
                        subjectId = subject.id,
                        title = draft.who,
                        description = draft.note,
                        occurred = draft.occurred,
                        threadId = draft.threadId,
                        isUnfiled = threads.isNotEmpty() && draft.threadId == null,
                    )
                } else if (subject != null) {
                    val entryId = repository.createEntry(
                        subjectId = subject.id,
                        kind = draft.kind.entryKind(),
                        title = draft.who,
                        body = draft.note,
                        occurred = draft.occurred,
                        // An entry nobody could place goes to the Unfiled tray
                        // rather than being filed by the app. Only marked when
                        // there were threads to choose from: with none offered,
                        // not choosing one is not the person declining to say.
                        isUnfiled = threads.isNotEmpty() && draft.threadId == null,
                    )
                    draft.threadId?.let { repository.linkEntryToThread(entryId, it) }
                    // **The person, when they were chosen rather than typed.**
                    // This is what makes "a person knows every call and visit
                    // involving them" true rather than a promise, and
                    // `entry_person` had no writer at all until now.
                    draft.personId?.let { repository.linkEntryToPerson(entryId, it) }
                    // **The incident it was opened from, carried forward.** The
                    // person is looking at the thread; asking which one would
                    // be the app not paying attention. Part Two.
                    addingToIncident?.let { repository.attachEntryToIncident(entryId, it) }
                    // A call carries an extra row for whether anyone picked up.
                    // The other three are fully described by the entry itself.
                    if (draft.kind == CaptureKind.CALL) {
                        repository.addCallDetail(entryId = entryId)
                    }
                }
                saving = null
                addingToIncident = null
                revision += 1
                // **Stays where they were when the capture belonged to an
                // incident.** Being thrown back to the notebook after adding a
                // call to a thread is losing the person's place in the one
                // screen they were reading.
                if (!incidentsOpen) destination = Destination.NOTEBOOK
            }
        }
    }
}

/**
 * The fixed order of the table of contents, from `MASTER_SPEC.md` section 4.4.
 * Projects sit outside the notebook and have their own destination, so they are
 * not in this list.
 */
/**
 * What weight each section carries, read from the active situation template.
 *
 * **The template decides emphasis and nothing else.** It cannot add a section,
 * cannot remove one, and cannot move one, so this returns a weight per section
 * and never an order. A subject with no template, which is what "Not sure yet"
 * on the situation picker leaves behind, gets an empty map and therefore a
 * notebook where every section stands at the same weight. That is a complete
 * notebook, not a degraded one.
 *
 * A template id that is not in the catalog, which is what an export from a
 * later version of the app would carry, falls through to the same empty map
 * rather than failing.
 */
/**
 * The contact roles the active situation names.
 *
 * Empty for a notebook with no situation, which is a real state rather than a
 * gap: "Not sure yet" is a valid answer to the picker, and a notebook with no
 * template gets a free text field and no chips rather than somebody else's
 * vocabulary.
 */
private suspend fun rolesFor(
    context: android.content.Context,
    templateId: String?,
): List<String> {
    if (templateId == null) return emptyList()
    return TemplateCatalog.situations(context).all
        .firstOrNull { it.id == templateId }
        ?.roles
        .orEmpty()
}

private suspend fun emphasisFor(
    context: android.content.Context,
    templateId: String?,
): Map<Repository.Section, Emphasis> {
    if (templateId == null) return emptyMap()
    val situation = TemplateCatalog.situations(context).all.firstOrNull { it.id == templateId }
        ?: return emptyMap()
    return emphasisFrom(forward = situation.forward, folded = situation.folded)
}

/**
 * Opens the dialer with a number filled in.
 *
 * **`ACTION_DIAL` rather than `ACTION_CALL`**, which would need the call
 * permission. The person presses the green button themselves. That is one extra
 * tap and it is the right one: this app asks for no permission it does not
 * need, and placing a call on somebody's behalf is not something it should be
 * able to do silently.
 *
 * A blank number does nothing rather than opening an empty dialer.
 */
private fun dial(context: android.content.Context, phone: String?) {
    phone?.takeIf { it.isNotBlank() }?.let { number ->
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                android.net.Uri.fromParts("tel", number, null),
            ),
        )
    }
}

/**
 * The most one attachment may hold, per the schema comment and D13.
 *
 * Stated to the person before they choose rather than after, and a photograph
 * taken on this phone is comfortably under it.
 */
private const val MAX_ATTACHMENT_BYTES = 25L * 1024 * 1024

/** A stored attachment: where it landed and how big it was. */
private data class StoredFile(val sha256: String, val byteSize: Long)

/**
 * Copies a chosen image into content addressed storage.
 *
 * Returns null when the file is over the cap or cannot be read, and writes
 * nothing in that case. The size is checked before a single byte is copied,
 * so an oversized file never briefly occupies the phone.
 */
private suspend fun storePicked(
    context: android.content.Context,
    uri: android.net.Uri,
): StoredFile? = runCatching {
    val size = context.contentResolver
        .openAssetFileDescriptor(uri, "r")
        ?.use { it.length }
        ?: -1L
    if (size > MAX_ATTACHMENT_BYTES) return null

    val attachments = Attachments.open(context)
    val hash = context.contentResolver.openInputStream(uri)
        ?.use { attachments.put(it) }
        ?: return null
    StoredFile(hash, size.coerceAtLeast(0))
}.getOrNull()

/**
 * Something the person asked to remove.
 *
 * **One shape for every list**, so a single confirmation sheet serves all of
 * them rather than each screen growing its own. `what` is the person's own
 * words for the thing, shown back to them before the tap that matters.
 */
private data class Removal(
    val section: Repository.Section,
    val rowId: String,
    val what: String,
)

/**
 * The name the file picker opens with.
 *
 * The shape `contract/EXPORT-FORMAT.md` section 1 specifies, so a person with
 * several of these can tell them apart by name alone.
 */
private fun exportFileName(): String {
    val now = java.time.LocalDateTime.now()
    val stamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
    return "healthtrail-export-$stamp.htx"
}

private val SECTION_ORDER = listOf(
    Repository.Section.CARE_TEAM,
    Repository.Section.MEDICATIONS,
    Repository.Section.APPOINTMENTS,
    Repository.Section.CHAPTERS,
    Repository.Section.THREADS,
    Repository.Section.TRAIL,
    Repository.Section.PROGRESS,
    Repository.Section.DOCUMENTS,
    Repository.Section.MONEY,
    Repository.Section.STANDING_INSTRUCTIONS,
    Repository.Section.ASK_NEXT_TIME,
    Repository.Section.EMERGENCY_CARD,
)

@Composable
private fun Loading() {
    val strings = LocalStrings.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = strings["common.loading"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
    }
}

/**
 * The counts could not be read.
 *
 * It says what happened, says plainly that nothing was lost, and offers the one
 * action that might fix it. It does not apologize for the software and it does
 * not explain the exception, because neither helps the person holding the phone.
 */
@Composable
private fun CouldNotRead(onRetry: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.l)
            .testTag(ShellTags.ERROR),
    ) {
        Text(
            text = strings["notebook.title"],
            style = HealthTrail.type.displayL,
            color = HealthTrail.colors.ink,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            text = strings["common.error.generic"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
        Spacer(Modifier.height(Space.m))
        FilledButton(label = strings["common.retry"], onClick = onRetry)
    }
}

/**
 * A destination that exists in the navigation but is not built yet.
 *
 * This is a real screen rather than a stub: it names itself and says plainly
 * that it is not built, which is honest. What it must never be is a blank area,
 * because a blank area reads as broken and the person cannot tell the difference
 * between "nothing here yet" and "this app is failing".
 *
 * It disappears entirely as each destination lands. If one of these is still
 * here at release, that is a bug.
 */
@Composable
private fun NotBuiltYet(
    name: String,
    detail: String? = null,
    onClose: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthTrail.colors.paper)
            .systemBarsPadding()
            .padding(horizontal = Space.screenHorizontal, vertical = Space.l)
            .testTag(ShellTags.NOT_BUILT),
    ) {
        Text(text = name, style = HealthTrail.type.displayL, color = HealthTrail.colors.ink)
        Spacer(Modifier.height(Space.s))
        Text(
            text = detail ?: strings["shell.not_built"],
            style = HealthTrail.type.bodyM,
            color = HealthTrail.colors.ink2,
        )
        if (onClose != null) {
            Spacer(Modifier.height(Space.l))
            TextAction(label = strings["common.close"], onClick = onClose)
        }
    }
}

/**
 * The steps still worth suggesting on Today.
 *
 * **Built from what the notebook actually holds.** The list was three fixed
 * sentences, which meant a notebook with two people on the care team and four
 * logged calls was still being told to add people and log a call. Advice that
 * ignores the screen behind it reads as an app that is not paying attention.
 *
 * **Order is fixed, and the emergency card is first.** It is the highest value
 * two minutes a new person can spend and the one thing in this app that is
 * useful to somebody else in a hurry. Ordering by anything else, or shuffling
 * as things get done, would make the advice move around between visits.
 *
 * **An empty list is a finished state, not a gap.** Somebody who has done all
 * three gets a Today with no coaching on it at all, which is correct: there is
 * nothing left to suggest and inventing a fourth suggestion to fill the space
 * would be the app keeping score of their diligence, which rule 13 forbids.
 */
private fun coachingSteps(
    counts: List<SectionCount>?,
    emergencyCardHasSomething: Boolean,
    onOpenSection: (Repository.Section) -> Unit,
    onCapture: () -> Unit,
): List<CoachStep> {
    // Null means the counts have not been read yet. Suggesting nothing is the
    // honest answer for that moment, and it lasts one frame.
    if (counts == null) return emptyList()
    fun empty(section: Repository.Section) =
        counts.firstOrNull { it.section == section }?.count == 0

    return listOfNotNull(
        CoachStep(
            labelKey = "today.empty.step.1",
            section = Repository.Section.EMERGENCY_CARD,
            onOpen = { onOpenSection(Repository.Section.EMERGENCY_CARD) },
        ).takeIf { !emergencyCardHasSomething },
        CoachStep(
            labelKey = "today.empty.step.2",
            section = Repository.Section.CARE_TEAM,
            onOpen = { onOpenSection(Repository.Section.CARE_TEAM) },
        ).takeIf { empty(Repository.Section.CARE_TEAM) },
        // Capture rather than a section, because logging a call is the gold
        // button's job and sending somebody to the trail to read an empty list
        // would be advice that leads away from the thing it is advising.
        CoachStep(
            labelKey = "today.empty.step.3",
            section = null,
            onOpen = onCapture,
        ).takeIf { empty(Repository.Section.TRAIL) },
    )
}
