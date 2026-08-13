package com.kamsiob.healthtrail.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
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
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.formatMoney
import com.kamsiob.healthtrail.ui.components.BottomNav
import com.kamsiob.healthtrail.ui.components.CaptureFab
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.screens.AboutScreen
import com.kamsiob.healthtrail.ui.screens.PROJECT_CARD_TYPES
import com.kamsiob.healthtrail.ui.screens.cardOffers
import com.kamsiob.healthtrail.ui.screens.countLineKey
import com.kamsiob.healthtrail.ui.screens.emptyLineKey
import com.kamsiob.healthtrail.ui.screens.headingFor
import com.kamsiob.healthtrail.ui.screens.wordedAnswer
import com.kamsiob.healthtrail.ui.screens.IncidentScreen
import com.kamsiob.healthtrail.ui.screens.IncidentsScreen
import com.kamsiob.healthtrail.data.Readable
import com.kamsiob.healthtrail.ui.components.Share
import com.kamsiob.healthtrail.ui.screens.EntryScreen
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.AddMilestoneScreen
import com.kamsiob.healthtrail.ui.screens.MilestoneDraft
import com.kamsiob.healthtrail.ui.screens.MilestonesScreen
import com.kamsiob.healthtrail.ui.screens.ChangeSituationScreen
import com.kamsiob.healthtrail.ui.screens.MonthReviewScreen
import com.kamsiob.healthtrail.ui.screens.SituationChange
import com.kamsiob.healthtrail.ui.screens.SituationPickerScreen
import com.kamsiob.healthtrail.ui.screens.PrepScreen
import com.kamsiob.healthtrail.ui.screens.BillScreen
import com.kamsiob.healthtrail.ui.screens.ChapterScreen
import com.kamsiob.healthtrail.ui.screens.MedicationEventDraft
import com.kamsiob.healthtrail.ui.screens.MedicationEventScreen
import com.kamsiob.healthtrail.ui.screens.MedicationScreen
import com.kamsiob.healthtrail.ui.screens.ViolationDraft
import com.kamsiob.healthtrail.ui.screens.ViolationScreen
import com.kamsiob.healthtrail.ui.screens.SearchScreen
import com.kamsiob.healthtrail.ui.screens.ThreadScreen
import com.kamsiob.healthtrail.ui.screens.ExportScreen
import com.kamsiob.healthtrail.ui.screens.ConflictsScreen
import com.kamsiob.healthtrail.ui.screens.RestoreScreen
import com.kamsiob.healthtrail.ui.screens.RestoreHow
import com.kamsiob.healthtrail.ui.screens.RestoreState
import com.kamsiob.healthtrail.data.ExportContainer
import com.kamsiob.healthtrail.data.MergeApply
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
import com.kamsiob.healthtrail.ui.screens.CaptureBloom
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
import com.kamsiob.healthtrail.ui.screens.AddThreadScreen
import com.kamsiob.healthtrail.ui.screens.CorrectIncidentScreen
import com.kamsiob.healthtrail.ui.screens.IncidentCorrection
import com.kamsiob.healthtrail.ui.screens.MedicationDraft
import com.kamsiob.healthtrail.ui.screens.MedicationsScreen
import com.kamsiob.healthtrail.ui.screens.AcknowledgeSheet
import com.kamsiob.healthtrail.ui.screens.AnswerSheet
import com.kamsiob.healthtrail.ui.screens.QuestionsScreen
import com.kamsiob.healthtrail.ui.screens.CareThreadsScreen
import com.kamsiob.healthtrail.ui.screens.ProgressScreen
import com.kamsiob.healthtrail.ui.screens.ProjectHomeScreen
import com.kamsiob.healthtrail.ui.screens.ProjectSetupScreen
import com.kamsiob.healthtrail.ui.screens.StageSheet
import com.kamsiob.healthtrail.ui.screens.LogCallSheet
import com.kamsiob.healthtrail.ui.screens.ProjectDateSheet
import com.kamsiob.healthtrail.ui.screens.StandingSheet
import com.kamsiob.healthtrail.ui.screens.ProjectsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectStepsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectPaperworkScreen
import com.kamsiob.healthtrail.ui.screens.ProjectPeopleScreen
import com.kamsiob.healthtrail.ui.screens.ProjectTrailScreen
import com.kamsiob.healthtrail.ui.screens.StepEditSheet
import com.kamsiob.healthtrail.ui.screens.DateKindEditSheet
import com.kamsiob.healthtrail.ui.screens.ProjectDateKindsScreen
import com.kamsiob.healthtrail.ui.screens.PaperEditSheet
import com.kamsiob.healthtrail.ui.screens.ProjectPapersScreen
import com.kamsiob.healthtrail.ui.screens.ProjectRoadScreen
import com.kamsiob.healthtrail.ui.screens.StageEditSheet
import com.kamsiob.healthtrail.ui.screens.StartProjectPreviewSheet
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
import com.kamsiob.healthtrail.ui.screens.DocumentScreen
import com.kamsiob.healthtrail.ui.screens.DocumentsScreen
import com.kamsiob.healthtrail.data.Attachments
import com.kamsiob.healthtrail.ui.screens.parseAmountToMinor
import com.kamsiob.healthtrail.ui.screens.EmergencyCardEditScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyCardScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyDraft
import com.kamsiob.healthtrail.ui.screens.CorrectEntryScreen
import com.kamsiob.healthtrail.ui.screens.CorrectSubjectScreen
import com.kamsiob.healthtrail.ui.screens.SubjectCorrection
import com.kamsiob.healthtrail.ui.screens.EntryCorrection
import com.kamsiob.healthtrail.ui.screens.PersonDraft
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.TrailScreen
import com.kamsiob.healthtrail.ui.components.ConfirmRemoveSheet
import com.kamsiob.healthtrail.ui.screens.labelKey
import com.kamsiob.healthtrail.ui.screens.kindNameKey
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.screens.CoachStep
import com.kamsiob.healthtrail.ui.screens.LocalSectionBackKey
import com.kamsiob.healthtrail.ui.screens.TemplateLibraryScreen
import com.kamsiob.healthtrail.ui.screens.AddCardSheet
import com.kamsiob.healthtrail.ui.screens.CardOffer
import com.kamsiob.healthtrail.ui.screens.TodayFieldScreen
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.UnfiledTrayScreen
import com.kamsiob.healthtrail.ui.components.Waiting
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
    /**
     * The card types this person's stated situation starts them with.
     *
     * **The gallery puts them first**, 21.6 screen 6. It is the person's own
     * stated situation and never inference, per 21.5: this app does not watch
     * its user, and the only two things that shape Today are what they chose
     * and what they told it.
     */
    var suggestedCards by remember { mutableStateOf<Set<String>>(emptySet()) }
    var savingPerson by remember { mutableStateOf<PersonDraft?>(null) }

    /** Somebody being put at the top of the care team, or taken back out. #361. */
    var pinningPerson by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    /** Somebody being retired from the care team without being erased. #371. */
    var archivingPerson by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    /** The care thread being renamed, and what to write. #371. */
    var renamingThread by remember { mutableStateOf<Repository.CareThread?>(null) }
    var savingThreadRename by remember { mutableStateOf<Pair<String, String>?>(null) }

    /** Correcting who the notebook is about, and what to write. #371. */
    var correctingSubject by remember { mutableStateOf(false) }
    /** The subject row, read when the correction opens rather than held all the time. */
    var subjectRow by remember { mutableStateOf<Repository.Subject?>(null) }
    var savingSubject by remember { mutableStateOf<SubjectCorrection?>(null) }

    /** The entry whose words are being corrected, and what to write. #368. */
    var correctingEntry by remember { mutableStateOf<Repository.TrailEntry?>(null) }
    var savingEntryCorrection by remember {
        mutableStateOf<Pair<String, EntryCorrection>?>(null)
    }
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
    /**
     * The template being looked at before anything is created, screen 04.
     *
     * **Separate from `chosenTemplate` on purpose.** That one means "create
     * this now", and the whole point of the preview is that looking at a
     * template is not agreeing to it.
     */
    var previewTemplate by remember {
        mutableStateOf<TemplateCatalog.ProjectTemplate?>(null)
    }
    /** What the person called it on the preview, which may not be the template's own name. */
    var previewName by remember { mutableStateOf<String?>(null) }
    // The project being looked at, and its steps.
    var openProject by remember { mutableStateOf<Repository.Project?>(null) }
    var projectSteps by remember { mutableStateOf<List<Repository.ProjectStep>>(emptyList()) }
    var projectStages by remember { mutableStateOf<List<Repository.ProjectStage>>(emptyList()) }
    var projectStanding by remember { mutableStateOf<Repository.ProjectStanding?>(null) }
    var projectNextDate by remember { mutableStateOf<Repository.ProjectDate?>(null) }
    var projectLatestWord by remember { mutableStateOf<Repository.TrailEntry?>(null) }
    var projectPapers by remember { mutableStateOf<List<Repository.ProjectPaper>>(emptyList()) }
    var projectStandingHistory by remember {
        mutableStateOf<List<Repository.ProjectStanding>>(emptyList())
    }
    var updatingStanding by remember { mutableStateOf<Repository.Project?>(null) }
    var addingDateTo by remember { mutableStateOf<Repository.Project?>(null) }
    var loggingCallOn by remember { mutableStateOf<Repository.Project?>(null) }
    var setupOpen by rememberSaveable { mutableStateOf(false) }
    var movingStageOn by remember { mutableStateOf<Repository.Project?>(null) }
    var movingStage by remember {
        mutableStateOf<Triple<String, String, Edtf.Date>?>(null)
    }
    var settingLead by remember { mutableStateOf<Pair<String, String>?>(null) }
    var settingStatus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var savingCall by remember {
        mutableStateOf<Triple<String, String, String>?>(null)
    }
    var savingDate by remember {
        mutableStateOf<Quadruple<String, String, Edtf.Date, String>?>(null)
    }
    var projectDateKinds by remember { mutableStateOf<List<String>>(emptyList()) }
    var projectTrail by remember {
        mutableStateOf<List<Repository.ProjectTrailItem>>(emptyList())
    }
    var trailOpen by remember { mutableStateOf(false) }
    var paperworkOpen by remember { mutableStateOf(false) }
    var peopleOpen by remember { mutableStateOf(false) }
    var projectPeople by remember {
        mutableStateOf<List<Repository.ProjectPerson>>(emptyList())
    }
    var documentFilings by remember {
        mutableStateOf<List<Repository.DocumentFiling>>(emptyList())
    }
    var projectPaperCards by remember {
        mutableStateOf<List<Repository.ProjectPaperCard>>(emptyList())
    }
    var projectEntries by remember {
        mutableStateOf<List<Repository.TrailEntry>>(emptyList())
    }
    // A named type rather than a fourth slot on a Triple. Three anonymous
    // strings and a date is exactly the shape where the holder and the activity
    // get passed the wrong way round, and neither the compiler nor a screenshot
    // would say so.
    var savingStanding by remember { mutableStateOf<StandingWrite?>(null) }
    var projectCards by remember {
        mutableStateOf<Map<String, Repository.ProjectCard>>(emptyMap())
    }
    var todayLayout by remember { mutableStateOf<Repository.TodayLayout?>(null) }
    var addingCardTo by remember {
        mutableStateOf<List<Repository.TodayCard>?>(null)
    }
    var savingLayout by remember {
        mutableStateOf<List<Repository.TodayCard>?>(null)
    }
    var todayAnswers by remember {
        mutableStateOf<Map<String, Repository.TodayAnswer>>(emptyMap())
    }
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
    var offerAnswers by remember {
        mutableStateOf<Map<String, Repository.TodayAnswer>>(emptyMap())
    }
    var togglingStep by remember { mutableStateOf<Repository.ProjectStep?>(null) }
    var settingProjectStatus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var settingWaitingOn by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Every step edit is a pending write held here rather than done inside a
    // composable, which is the shape every other write on this screen uses.
    var addingStep by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingStep by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    var movingStep by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    // The incident being corrected, and the correction in flight. #358.
    var correctingIncident by remember { mutableStateOf<Repository.Incident?>(null) }
    var savingCorrection by remember {
        mutableStateOf<Pair<String, IncidentCorrection>?>(null)
    }
    var removingStep by remember { mutableStateOf<String?>(null) }
    /** The starting steps being changed, 20.5 screen 18. */
    var stepsOpen by remember { mutableStateOf(false) }
    /** The one step whose sheet is open. */
    var stepUnderEdit by remember { mutableStateOf<Repository.ProjectStep?>(null) }
    /** The road being changed, 20.5 screen 18. */
    var roadOpen by remember { mutableStateOf(false) }
    var stageUnderEdit by remember { mutableStateOf<Repository.ProjectStage?>(null) }
    var addingStage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var renamingStage by remember { mutableStateOf<Pair<String, String>?>(null) }
    /** Reordering a stage against its neighbor, which is not the same as moving the project onto one. */
    var reorderingStage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var removingStage by remember { mutableStateOf<String?>(null) }
    /** The date kinds being changed, 20.5 screen 18. */
    var kindsOpen by remember { mutableStateOf(false) }
    var projectDateKindRows by remember {
        mutableStateOf<List<Repository.ProjectDateKind>>(emptyList())
    }
    var kindUnderEdit by remember { mutableStateOf<Repository.ProjectDateKind?>(null) }
    var addingKind by remember { mutableStateOf<Pair<String, String>?>(null) }
    var renamingKind by remember { mutableStateOf<Pair<String, String>?>(null) }
    var removingKind by remember { mutableStateOf<String?>(null) }
    /** The paper placeholders being changed, 20.5 screen 18. */
    var papersOpen by remember { mutableStateOf(false) }
    var paperUnderEdit by remember { mutableStateOf<Repository.ProjectPaper?>(null) }
    var addingPaper by remember { mutableStateOf<Pair<String, String>?>(null) }
    var renamingPaper by remember { mutableStateOf<Pair<String, String>?>(null) }
    var emptyingPaper by remember { mutableStateOf<String?>(null) }
    var removingPaper by remember { mutableStateOf<String?>(null) }
    var creatingOwnProject by remember { mutableStateOf<String?>(null) }
    var ownTemplates by remember {
        mutableStateOf<List<Repository.OwnTemplate>>(emptyList())
    }
    var libraryOpen by remember { mutableStateOf(false) }
    /**
     * How the notebook is set up, and changing it.
     *
     * **The picker ran once during setup and was then unreachable forever**, so
     * a family whose care moved could not tell the app and could not even see
     * which setting they had picked. Law 5 promises all of it is changeable
     * afterward from one screen, and there was no screen.
     */
    var situationOpen by remember { mutableStateOf(false) }
    var situationPickerOpen by remember { mutableStateOf(false) }
    var situations by remember { mutableStateOf<TemplateCatalog.Situations?>(null) }
    var chosenSituation by remember { mutableStateOf<TemplateCatalog.Situation?>(null) }
    /** The change waiting to be written: the setting, and a chapter or null. */
    var applyingSituation by remember { mutableStateOf<SituationChange?>(null) }
    /**
     * Which setting the notebook is on, reloaded with everything else.
     *
     * The id rather than the subject, because it is the only field of the
     * subject anything above the notebook reads, and holding the whole row here
     * would be a second copy of it going stale beside the first.
     */
    var situationId by remember { mutableStateOf<String?>(null) }
    var savingTemplate by remember { mutableStateOf<Repository.Project?>(null) }
    // Which projects have had their steps saved, so the control says so once
    // rather than inviting the same save again.
    var savedTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var startingFromOwn by remember { mutableStateOf<Repository.OwnTemplate?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }
    var conflictsOpen by remember { mutableStateOf(false) }
    var conflicts by remember { mutableStateOf(emptyList<Repository.Resolution>()) }
    var unseenConflicts by remember { mutableStateOf(0) }
    var markConflictsSeen by remember { mutableStateOf(false) }

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
    /** True while the emergency card is on its way to the share sheet. */
    var sharingCard by remember { mutableStateOf(false) }
    /** The document whose own screen is open, or null. */
    var openDocument by remember { mutableStateOf<Repository.Document?>(null) }
    /** The bill whose own screen is open, or null. */
    var openBill by remember { mutableStateOf<Repository.Bill?>(null) }
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
    /**
     * The month being reviewed, and what it holds.
     *
     * **The month rather than a label**, because the label is already localized
     * and reading the app's own rendering back as data is how a screen ends up
     * agreeing with itself in English and disagreeing in Arabic.
     */
    var reviewMonth by remember { mutableStateOf<java.time.YearMonth?>(null) }
    var review by remember { mutableStateOf<Repository.MonthReview?>(null) }
    /** A month waiting to become a document for the share sheet. */
    var sharingReview by remember { mutableStateOf<Repository.MonthReview?>(null) }
    var milestonesOpen by remember { mutableStateOf(false) }
    var addingMilestone by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<Repository.Milestone?>(null) }
    var savingMilestone by remember { mutableStateOf<MilestoneDraft?>(null) }
    var milestones by remember { mutableStateOf<List<Repository.Milestone>>(emptyList()) }

    /** The care thread being read, and what is on it. */
    var openThread by remember { mutableStateOf<Repository.CareThread?>(null) }

    /** The chapter being read, and what happened while they were there. */
    var openChapter by remember { mutableStateOf<Repository.Chapter?>(null) }

    /** The medication being read, and how it changed. */
    var openMedication by remember { mutableStateOf<Repository.Medication?>(null) }
    /** The medication a change is being written down for. */
    var recordingChangeTo by remember { mutableStateOf<Repository.Medication?>(null) }
    /** Every time each standing instruction was not followed, and the one being written down. */
    var violationsByInstruction by remember {
        mutableStateOf<Map<String, List<Repository.Violation>>>(emptyMap())
    }
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
    /**
     * How many attached files the last export could not find. #332.
     *
     * **Beside the state rather than inside it**, because it is not a state:
     * the export finished either way and the file is written either way. What
     * it changes is what the finished screen has to say.
     */
    var exportMissing by remember { mutableStateOf(0) }
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
    // Which of the two things the person asked for. Held beside the trigger
    // rather than inside the screen, because the screen is redrawn from state
    // and an answer that lived there would be lost the moment it was given.
    var applyHow by remember { mutableStateOf(RestoreHow.REPLACE) }
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
    // How many questions wait on each medication, so the list row can say so
    // rather than making somebody open every medication in turn. #352.
    var medicationQuestionCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var addingMedication by remember { mutableStateOf(false) }
    var savingMedication by remember { mutableStateOf<MedicationDraft?>(null) }
    var questions by remember { mutableStateOf<List<Repository.Question>>(emptyList()) }
    var threadCounts by remember {
        mutableStateOf<List<Repository.ThreadWithCount>>(emptyList())
    }
    // Starting a thread from nothing, #349 and D145. The name in flight rather
    // than a draft type, because a name is the only thing the screen asks for.
    var addingThread by remember { mutableStateOf(false) }
    var savingThread by remember { mutableStateOf<String?>(null) }
    var readings by remember { mutableStateOf<List<Repository.Reading>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<Repository.Chapter>>(emptyList()) }
    var chapterContents by remember {
        mutableStateOf<Map<String, Repository.ChapterContents>>(emptyMap())
    }
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

    /**
     * The folders this notebook already has, offered by the document form. #221.
     *
     * **Reloaded with everything else**, so saving into a new folder makes that
     * folder offerable to the next document without a second trip.
     */
    var documentFolders by remember { mutableStateOf<List<String>>(emptyList()) }
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
            // **Counted on every load, not only after a merge.** It was set
            // once, when a merge finished, so the door to reading what the
            // merge decided vanished the next time the app started and the
            // resolutions became unreachable. A capability only reachable in
            // the session that created it is not reachable, `DESIGN.md` 13.5.
            unseenConflicts = repository.unseenConflicts()
            val subject = repository.activeSubject()
            situationId = subject?.situationTemplateId
            val emphasis = emphasisFor(context, subject?.situationTemplateId)
            // **The roles this situation actually has**, so adding a contact
            // offers them rather than asking the person to type a job title
            // from memory. The catalog has carried them since it was written.
            roleSuggestions = rolesFor(context, subject?.situationTemplateId)
            suggestedCards = startingHandFor(context, subject?.situationTemplateId)
            // Counts belong to a subject. With no subject there is no notebook
            // to count, and showing zeros would be inventing a notebook that
            // does not exist yet.
            counts = subject?.let { active ->
                // **Money says what is not settled rather than how many bills**,
                // which is what the grid draws and what somebody opens Money to
                // find. Null when nothing is unsettled, and the row counts. #347.
                val unsettled = repository.unsettledTotal(active.id)
                SECTION_ORDER.map {
                    SectionCount(
                        it,
                        repository.count(it, active.id),
                        emphasis[it] ?: Emphasis.STANDING,
                        amount = if (it == Repository.Section.MONEY) {
                            unsettled?.let { (minor, currency) ->
                                formatMoney(strings, minor, currency)
                            }
                        } else {
                            null
                        },
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
            medicationQuestionCounts =
                subject?.let { repository.openQuestionCountsByMedication(it.id) }.orEmpty()
            questions = subject?.let { repository.questions(it.id) }.orEmpty()
            threadCounts = subject?.let { repository.threadsWithCounts(it.id) }.orEmpty()
            readings = subject?.let { repository.readings(it.id) }.orEmpty()
            chapters = subject?.let { repository.chapters(it.id) }.orEmpty()
            chapterContents = subject?.let { repository.chapterContents(it.id) }.orEmpty()
            milestones = subject?.let { repository.milestones(it.id) }.orEmpty()
            appointments = subject?.let { repository.appointments(it.id) }.orEmpty()
            instructions = subject?.let { repository.standingInstructions(it.id) }.orEmpty()
            violationsByInstruction =
                subject?.let { repository.violationsBySubject(it.id) }.orEmpty()
            bills = subject?.let { repository.bills(it.id) }.orEmpty()
            documents = subject?.let { repository.documents(it.id) }.orEmpty()
            documentFolders = subject?.let { repository.documentFolders(it.id) }.orEmpty()
            projects = subject?.let { repository.projects(it.id) }.orEmpty()
            // The mini road and the two answers, for every project at once
            // rather than three queries each. DESIGN.md 20.5 screen 2.
            projectCards = subject?.let { repository.projectCards(it.id) }.orEmpty()
            // **Every card's answer in one pass, on focus and after a save.**
            // 21.2: the surface pulls, and nothing here watches the person.
            todayLayout = subject?.let { repository.todayLayout(it.id) }
            todayAnswers = subject?.let { current ->
                val byType = repository.todayAnswers(current.id)
                // **A card with no answer is absent from this map**, not
                // present holding an empty one. An empty answer means the
                // record has nothing to say; an absent one means the question
                // was never asked. Filling the gap with an empty answer made
                // every unanswered card claim "Nothing waiting", including the
                // digest in the lead slot on a notebook holding 182 entries.
                todayLayout?.all.orEmpty().mapNotNull { card ->
                    val answer = repository.todayAnswerForSource(
                        card.type, card.sourceTable, card.sourceId,
                    ) ?: byType[card.type]
                    answer?.let { card.id to it }
                }.toMap()
            }.orEmpty()
            projectTemplates = TemplateCatalog.projects(context)
            ownTemplates = repository.ownTemplates("project")
            // Kept in step with the list, so a step ticked on the detail screen
            // and the count on the card behind it can never disagree.
            openProject = openProject?.let { current ->
                projects.firstOrNull { it.id == current.id }
            }
            projectSteps = openProject?.let { repository.projectSteps(it.id) }.orEmpty()
            // The shape the person gave this project, loaded with it. Each
            // is one of the three answers in DESIGN.md 20.1, or the road
            // that says it is a project at all.
            projectStages = openProject?.let { repository.projectStages(it.id) }.orEmpty()
            projectStandingHistory =
                openProject?.let { repository.projectStandingHistory(it.id) }.orEmpty()
            projectStanding = projectStandingHistory.firstOrNull()
            projectNextDate = openProject?.let { repository.leadingProjectDate(it.id) }
            projectLatestWord = openProject?.let { repository.latestWordFor(it.id) }
            projectEntries = openProject?.let { repository.entriesAbout(it.id) }.orEmpty()
            // What was said, where the road turned, and the dates it runs
            // against, on one line. 20.5 screen 11.
            projectTrail = openProject?.let { repository.projectTrail(it.id) }.orEmpty()
            projectPapers = openProject?.let { repository.projectPapers(it.id) }.orEmpty()
            // The places with whatever is filed in them, for screen 13.
            projectPaperCards =
                openProject?.let { repository.projectPaperCards(it.id) }.orEmpty()
            // Whoever this process has actually involved, 20.5 screen 14.
            projectPeople = openProject?.let { repository.projectPeople(it.id) }.orEmpty()
            projectDateKinds =
                openProject?.let { repository.projectDateKinds(it.id) }.orEmpty()
            // The same list with the ids the editor needs. Read alongside
            // rather than instead, so the chips keep taking labels alone.
            projectDateKindRows =
                openProject?.let { repository.projectDateKindRows(it.id) }.orEmpty()
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
    // **Above the trail it was opened from and below everything it opens.** The
    // dispatcher hands a press to the most recently added enabled callback, so a
    // chapter reached through a review has to register after this line or back
    // would close the review out from under it and leave the chapter standing.
    // Every door on the review screen is registered below.
    BackHandler(enabled = reviewMonth != null) { reviewMonth = null; review = null }
    // Setup sits on top of the project, so back closes it first. Depth stops
    // at level three, per the grid's own navigation model.
    BackHandler(enabled = setupOpen) { setupOpen = false }
    // The trail sits on top of the project, so back closes the trail rather
    // than the project underneath it. Without this, one back from a project's
    // trail left the projects list and the person lost two screens to one tap.
    BackHandler(enabled = openProject != null && trailOpen) { trailOpen = false }
    BackHandler(enabled = openProject != null && paperworkOpen) { paperworkOpen = false }
    BackHandler(enabled = openProject != null && peopleOpen) { peopleOpen = false }
    BackHandler(
        enabled = openProject != null && !setupOpen && !trailOpen &&
            !paperworkOpen && !peopleOpen,
    ) {
        openProject = null
    }
    BackHandler(enabled = aboutOpen) { aboutOpen = false }
    BackHandler(enabled = searchOpen) { searchOpen = false }
    BackHandler(enabled = incidentsOpen && openIncident == null) { incidentsOpen = false }
    // **The library is above More and below a project**, so back from a project
    // opened out of the library returns to the library rather than leaving the
    // app. `BackJourneyTest` exists because that exact thing was wrong on every
    // screen above the notebook once.
    BackHandler(enabled = libraryOpen && openProject == null) { libraryOpen = false }
    BackHandler(enabled = situationOpen && !situationPickerOpen) {
        situationOpen = false
        chosenSituation = null
    }
    BackHandler(enabled = situationPickerOpen) { situationPickerOpen = false }
    BackHandler(enabled = openIncident != null) { openIncident = null }
    BackHandler(enabled = openPerson != null) { openPerson = null }
    BackHandler(enabled = openPrepFor != null) { openPrepFor = null }
    BackHandler(enabled = openThread != null) { openThread = null }
    BackHandler(enabled = openChapter != null) { openChapter = null }
    BackHandler(enabled = milestonesOpen && !addingMilestone) { milestonesOpen = false }
    BackHandler(enabled = addingMilestone) {
        addingMilestone = false
        editingMilestone = null
    }
    // **The entry sits on top of whatever opened it, so its way back is
    // registered last.** The dispatcher hands the press to the most recently
    // added enabled callback, so an entry opened from a prep sheet with this
    // declared earlier would have closed the sheet underneath and left the
    // entry on screen. Every screen that can open an entry belongs above this
    // line.
    BackHandler(enabled = openEntry != null) { openEntry = null }
    // **Three full screens had no handler at all**, so system back was swallowed
    // by the section handler below: the section closed behind a document that
    // stayed drawn, nothing changed on screen, and the next press left the app
    // from a detail screen. A document, a bill and the conflicts list. #371.
    BackHandler(enabled = correctingSubject) { correctingSubject = false }
    BackHandler(enabled = renamingThread != null) { renamingThread = null }
    BackHandler(enabled = openDocument != null) { openDocument = null }
    BackHandler(enabled = openBill != null) { openBill = null }
    BackHandler(enabled = conflictsOpen) { conflictsOpen = false; markConflictsSeen = true }
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
    BackHandler(enabled = addingThread) { addingThread = false }
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
        // **Back from a form reopens the chooser**, which is D65's contract and
        // is not this change's to alter: the bloom is a different presentation
        // of the same chooser, not a different flow.
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
                    // **The Today the person arranged, when there is one.**
                    // DESIGN.md 21. `todayLayout` is null only before a starting
                    // hand has been applied, and nothing applies one yet: the
                    // situation template's hand belongs to onboarding and is
                    // #305. Until that lands a real notebook has no layout, so
                    // the previous Today still answers rather than the surface
                    // being blank, which is the one thing 21.5 rules out.
                    //
                    // **The old screen is not extended while it is here**, per
                    // the freeze rule. It is called and nothing more.
                    Destination.TODAY -> todayLayout?.let { layout ->
                        TodayFieldScreen(
                            layout = layout,
                            answers = todayAnswers,
                            // **Every card is a door**, 21.2, and a door
                            // that does nothing on press reads as broken. Each
                            // one opens the section its answer lives in, which
                            // is where the person would go to see the whole of
                            // it. A project card opens the project itself.
                            digest = digest,
                            // **The same signal the previous Today used**, so
                            // the digest does not tell somebody on their first
                            // morning what has changed since a visit they never
                            // made. #245.
                            hasAnything = (counts?.sumOf { it.count } ?: 0) > 0,
                            // **Search keeps its place on Today**, 21.1, and
                            // this surface arrived without one at all: the
                            // previous Today had the way into search in its
                            // header, and a seeded notebook, which is every
                            // real one, had no way to search from the front
                            // door for as long as the new surface has existed.
                            onSearch = { searchOpen = true },
                            // **One write, from Done.** The screen holds every
                            // change while editing so a person can move three
                            // cards and change their mind about all of them,
                            // and nothing saves behind their back. 21.6.
                            onSave = { cards -> savingLayout = cards },
                            onAddCard = { draft -> addingCardTo = draft },
                            // **The roster, for the care team card's source
                            // picker and for what a chosen card shows.** 21.7:
                            // that card takes a person the way the measure and
                            // project cards take theirs.
                            people = people,
                            // **`ACTION_DIAL` and never `ACTION_CALL`**, which
                            // is the same argument the calendar hand-off makes
                            // for `ACTION_INSERT`: the dialer opens filled in
                            // and nothing goes out until the person presses the
                            // green button, so they see the call before it
                            // happens. It also asks for no permission.
                            onDial = { number -> dial(context, number) },
                            onOpen = { card ->
                                val project = card.sourceId
                                    ?.takeIf { card.sourceTable == "project" }
                                    ?.let { id -> projects.firstOrNull { it.id == id } }
                                // **A card pointing at one person opens that
                                // person**, rule 18: the card shows them, so
                                // they show the card's way back. Opening the
                                // whole care team instead would answer "tell me
                                // about this one" with a list.
                                val person = card.sourceId
                                    ?.takeIf { card.sourceTable == "person" }
                                    ?.let { id -> people.firstOrNull { it.id == id } }
                                when {
                                    project != null -> {
                                        destination = Destination.PROJECTS
                                        openProject = project
                                    }

                                    person != null -> openPerson = person

                                    // **The two cards that count something
                                    // with a list of its own open that list.**
                                    // Both used to open the trail, so the card
                                    // answered a question and then took the
                                    // person somewhere the answer was not.
                                    // #360.
                                    card.type == "incidents" -> {
                                        destination = Destination.NOTEBOOK
                                        incidentsOpen = true
                                    }

                                    card.type == "unfiled" -> {
                                        destination = Destination.NOTEBOOK
                                        trayOpen = true
                                    }

                                    // Somebody archived is not in the roster,
                                    // and their card is still a door: it opens
                                    // the section, where they can be found.
                                    else -> sectionForCard(card.type)?.let { openSection = it }
                                }
                                revision += 1
                            },
                        )
                    } ?: TodayScreen(
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
                        onOpen = { openProject = it; setupOpen = false; revision += 1 },
                        onStart = { startingProject = true },
                        cards = projectCards,
                        // **The same sentence the project's own screen uses**,
                        // composed in one place, so a card and the screen it
                        // opens can never word the same date differently.
                        countdown = { project ->
                            projectCards[project.id]?.nextDate?.dueStart?.let { due ->
                                val days = java.time.Duration
                                    .ofMillis(due - System.currentTimeMillis()).toDays()
                                when {
                                    days == 0L -> strings["project.countdown.today"]
                                    days > 0 -> strings("project.countdown.days", "count" to days)
                                    else -> strings("project.countdown.passed", "count" to -days)
                                }
                            }
                        },
                    )
                    // More is no longer entirely unbuilt. Appearance is
                    // real; everything else in it still says so plainly.
                    Destination.MORE -> MoreScreen(
                        choice = themeChoice,
                        onChoose = onThemeChoice,
                        onAbout = { aboutOpen = true },
                        onSearch = { searchOpen = true },
                        onLibrary = { libraryOpen = true },
                        onSituation = { situationOpen = true },
                        onSubject = { correctingSubject = true },
                        onExport = { exportState = ExportState.READY; exportOpen = true },
                        onRestore = {
                            restoreState = RestoreState.Empty
                            restoreFile = null
                            restoreOpen = true
                        },
                        onConflicts = { conflictsOpen = true },
                        conflicts = unseenConflicts,
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
                missingAttachments = exportMissing,
            )
        }

        // Named for what it is rather than "destination", which is already the
        // navigation state in this composable and shadowed it.
        val exportTarget = writeTo
        if (exportTarget != null) {
            LaunchedEffect(exportTarget) {
                val passphrase = pendingPassphrase
                val outcome = withContext(Dispatchers.IO) {
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
                            val written = Backup.export(
                                context = context,
                                target = staged,
                                exportedAt = System.currentTimeMillis(),
                                passphrase = chosen.toCharArray(),
                                passphraseHint = pendingHint,
                            )
                            context.contentResolver.openOutputStream(exportTarget)?.use { out ->
                                staged.inputStream().use { it.copyTo(out) }
                            } ?: error("no output stream")
                            // **Returned rather than reported here**, so what the
                            // export noticed reaches the screen the person is
                            // looking at. An export that finished short and said
                            // only "Saved" is #332, and the person finds out on
                            // the new phone with the old one gone.
                            written
                        } finally {
                            staged.delete()
                        }
                    }
                }
                exportMissing = outcome.getOrNull()?.missingAttachments?.size ?: 0
                exportState =
                    if (outcome.isSuccess) ExportState.DONE else ExportState.FAILED
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
                onRestore = { chosen -> applyHow = chosen; applyNow = true },
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
            LaunchedEffect(source, applyHow) {
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
                        // **Two promises, two functions.** Replace swaps the
                        // whole file; keeping both merges row by row and writes
                        // what it resolved. 8.3 requires the choice to be the
                        // person's rather than the app's, so it is carried here
                        // rather than decided here.
                        val applied = when (applyHow) {
                            RestoreHow.MERGE ->
                                MergeApply.merge(context, container, System.currentTimeMillis())
                                    .map { 0 }
                            else -> Backup.restore(context, container)
                        }
                        applied.fold(
                            onSuccess = {
                                if (applyHow == RestoreHow.MERGE) {
                                    RestoreState.Merged
                                } else {
                                    RestoreState.Done
                                }
                            },
                            onFailure = { failure ->
                                RestoreState.Problem(
                                    // A merge that refused says why in its own
                                    // words, and those words are the ones the
                                    // person needs rather than a generic
                                    // failure.
                                    if (failure is MergeApply.Refused) {
                                        strings["restore.refused"]
                                    } else {
                                        failure.message ?: strings["common.error.generic"]
                                    },
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
                // A merge may have decided something, and the door to reading
                // what it decided only exists when there is something behind
                // it. Counted here rather than polled, because this is the one
                // moment the number can change.
                unseenConflicts = Repository.open(context).unseenConflicts()
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
                        // bidi-ok: this builds a filename. An invisible mark ends up in the name of a file somebody shares.
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

        // The emergency card as something a sibling in another state, or a
        // neighbor with a key, can read without this app.
        if (sharingCard) {
            LaunchedEffect(Unit) {
                val text = Readable.emergencyCard(
                    strings = strings,
                    subjectName = repository.activeSubject()?.displayName,
                    card = emergencyCard,
                    // **Keyed by the card, not the subject.** The first version
                    // passed the subject id, which is a valid id of the wrong
                    // thing: the query matched nothing, returned empty, and the
                    // document handed to a stranger came out with no contacts on
                    // it. Nothing failed and nothing said so. Caught by reading
                    // the file the share sheet was about to send.
                    contacts = emergencyContacts,
                    medications = medications.filter { it.showsOnEmergencyCard },
                )
                val intent = Share.documentIntent(
                    context = context,
                    fileName = Readable.fileName(
                        title = strings["readable.card.title"],
                        isoDate = java.time.LocalDate.now().toString(),
                        fallback = strings["readable.fallback"],
                    ),
                    text = text,
                    chooserTitle = strings["readable.share.title"],
                )
                sharingCard = false
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
                        // bidi-ok: this builds a filename. An invisible mark ends up in the name of a file somebody shares.
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

        // One month as a document, for the sibling who is not here.
        sharingReview?.let { gathered ->
            LaunchedEffect(gathered.monthStart) {
                val month = com.kamsiob.healthtrail.time.EventDateText.monthHeading(
                    strings,
                    gathered.monthStart,
                    java.time.ZoneId.systemDefault(),
                )
                val text = Readable.monthReview(
                    strings = strings,
                    subjectName = repository.activeSubject()?.displayName,
                    month = month,
                    review = gathered,
                )
                val intent = Share.documentIntent(
                    context = context,
                    fileName = Readable.fileName(
                        title = month,
                        isoDate = java.time.LocalDate.now().toString(),
                        fallback = strings["readable.fallback"],
                    ),
                    text = text,
                    chooserTitle = strings["readable.share.title"],
                )
                sharingReview = null
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
                    // **A project is not a notebook section**, and search is
                    // the one place that could forget it. Everything else here
                    // maps a hit to a section screen, projects have no section
                    // screen because they are a destination of their own, and
                    // the section route ended at a placeholder that said the
                    // screen was not built. **Somebody searching for the name
                    // of their own project found the one screen in the app
                    // that has nothing on it.** Rule 11, and the reason the
                    // routing below now lists every case rather than falling
                    // through an else.
                    // **A result opens the thing itself wherever the thing has
                    // a screen.** Opening the roster it lives in and leaving
                    // somebody to find the row again is the shape rule 18
                    // names, and every one of these detail screens is opened by
                    // id elsewhere in this same file. #360.
                    when (hit.section) {
                        Repository.Section.TRAIL -> openEntry = hit.id

                        Repository.Section.PROJECTS -> {
                            destination = Destination.PROJECTS
                            openProject = projects.firstOrNull { it.id == hit.id }
                        }

                        Repository.Section.CARE_TEAM ->
                            people.firstOrNull { it.id == hit.id }
                                ?.let { openPerson = it }
                                ?: run { openSection = hit.section }

                        Repository.Section.MEDICATIONS ->
                            medications.firstOrNull { it.id == hit.id }
                                ?.let { openMedication = it }
                                ?: run { openSection = hit.section }

                        Repository.Section.DOCUMENTS ->
                            documents.firstOrNull { it.id == hit.id }
                                ?.let { openDocument = it }
                                ?: run { openSection = hit.section }

                        Repository.Section.MONEY ->
                            bills.firstOrNull { it.id == hit.id }
                                ?.let { openBill = it }
                                ?: run { openSection = hit.section }

                        // **The roster where the row is not in memory.** An
                        // archived person is not in `people`, and a section is
                        // an honest answer for them rather than nothing at all.
                        else -> openSection = hit.section
                    }
                },
                onBack = { searchOpen = false },
            )
        }

        if (aboutOpen) {
            AboutScreen(onBack = { aboutOpen = false })
        }

        // **Marked seen after the screen closes, never as it opens.** Marking
        // on open would clear the notice before the person had read a word of
        // it, and this is the one screen whose entire purpose is that they do.
        if (markConflictsSeen) {
            LaunchedEffect(Unit) {
                val repo = Repository.open(context)
                repo.markConflictsSeen()
                unseenConflicts = repo.unseenConflicts()
                markConflictsSeen = false
            }
        }

        if (conflictsOpen) {
            // **Read when the screen opens, and marked seen when it closes.**
            // Marking on open would clear the notice before the person had
            // scrolled it, and this is the one screen whose whole purpose is
            // that they read it.
            LaunchedEffect(Unit) { conflicts = Repository.open(context).conflicts() }
            ConflictsScreen(
                resolutions = conflicts,
                onBack = { conflictsOpen = false; markConflictsSeen = true },
            )
        }

        movingStageOn?.let { project ->
            StageSheet(
                stages = projectStages,
                // **The last stage reached**, derived rather than read from
                // project.current_stage_id, so the sheet and the road strip can
                // never disagree about where the project is: the strip derives
                // it the same way.
                currentStageId = projectStages.lastOrNull { it.isReached }?.id,
                onPick = { stage, on ->
                    movingStage = Triple(project.id, stage.id, on)
                    movingStageOn = null
                },
                onDismiss = { movingStageOn = null },
            )
        }

        movingStage?.let { (projectId, stageId, on) ->
            LaunchedEffect(projectId, stageId) {
                repository.moveProjectToStage(
                    projectId = projectId,
                    stageId = stageId,
                    // **What the sheet said, not what the clock says.** Today by
                    // default, and the sheet offers the date so somebody writing
                    // down on Thursday that the letter came on Monday can say so.
                    reached = on,
                )
                movingStage = null
                revision += 1
            }
        }

        loggingCallOn?.let { project ->
            LogCallSheet(
                projectName = project.name,
                onSave = { who, words ->
                    savingCall = Triple(project.id, who, words)
                    loggingCallOn = null
                },
                onDismiss = { loggingCallOn = null },
            )
        }

        savingCall?.let { (projectId, who, words) ->
            LaunchedEffect(projectId, who, words) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    // **The entry and the link, both.** An entry nobody
                    // connected to the project is not something anybody said
                    // about the project, and the whole reason this sheet lives
                    // on the project screen is that it already knows which one.
                    val entryId = repository.createEntry(
                        subjectId = subject.id,
                        kind = "call",
                        title = who.ifBlank { null },
                        body = words,
                        // **Where she was when this happened.** #371: the
                        // chapter axis had readers everywhere and no writer, so
                        // a chapter could never hold anything.
                        chapterId = repository.currentChapterId(subject.id),
                    )
                    repository.linkEntryToProject(entryId, projectId)
                }
                savingCall = null
                revision += 1
            }
        }

        addingDateTo?.let { project ->
            ProjectDateSheet(
                kinds = projectDateKinds,
                onSave = { kind, due, source ->
                    savingDate = Quadruple(project.id, kind, due, source)
                    addingDateTo = null
                },
                onDismiss = { addingDateTo = null },
            )
        }

        savingDate?.let { (projectId, kind, due, source) ->
            LaunchedEffect(projectId, kind, due, source) {
                repository.addProjectDate(
                    projectId = projectId,
                    kind = kind,
                    due = due,
                    sourceNote = source.ifBlank { null },
                )
                savingDate = null
                revision += 1
            }
        }

        updatingStanding?.let { project ->
            StandingSheet(
                // The project's own people, which is what the grid means by
                // chips drawn from the project's people.
                // **Whose hands it has been in before, most recent first.**
                // The grid says chips drawn from the project's people; these
                // are the people this project actually has, which is what
                // somebody already wrote down rather than a roster they would
                // have to have filled in first.
                people = projectStandingHistory.map { it.holderLabel }.distinct(),
                previous = projectStanding,
                onSave = { holder, activity, since ->
                    savingStanding = StandingWrite(project.id, holder, activity, since)
                    updatingStanding = null
                },
                onDismiss = { updatingStanding = null },
            )
        }

        savingStanding?.let { write ->
            LaunchedEffect(write) {
                repository.addProjectStanding(
                    projectId = write.projectId,
                    holderLabel = write.holder,
                    // **What the sheet says, not what the clock says.** The
                    // lead has always read "the date is today unless you change
                    // it" and this line used to stamp today regardless, so the
                    // one sentence on the sheet that made a promise was the one
                    // the code broke. Rule 17.
                    since = write.since,
                    activity = write.activity.ifBlank { null },
                )
                savingStanding = null
                revision += 1
            }
        }

        addingCardTo?.let { onScreen ->
            // **The previews, read once when the sheet opens.** 21.6 screen 6
            // asks every entry to preview its small size with real current
            // data, and a name alone asks somebody to imagine a screen they
            // have never seen: "Medications, 6" is a decision they can make and
            // "Medications" is a guess.
            //
            // **Wrapped, because a read can fail**, and a gallery that throws
            // takes the app down from inside edit mode where the person has
            // unsaved work. A failed read leaves the previews empty, which the
            // rows render as their names alone.
            LaunchedEffect(onScreen, revision) {
                offerAnswers = runCatching {
                    val subject = repository.activeSubject() ?: return@runCatching emptyMap()
                    buildMap {
                        putAll(repository.todayAnswers(subject.id))
                        for (measure in measures) {
                            repository.todayAnswerForSource("measure", "measure", measure.id)
                                ?.let { put("measure-${measure.id}", it) }
                        }
                        for (project in projects.filterNot { it.isFinished }) {
                            for (type in PROJECT_CARD_TYPES) {
                                repository.todayAnswerForSource(type, "project", project.id)
                                    ?.let { put("$type-${project.id}", it) }
                            }
                        }
                    }
                }.getOrDefault(emptyMap())
            }

            AddCardSheet(
                offers = cardOffers(
                    onScreen = onScreen,
                    measures = measures,
                    projects = projects,
                    answers = offerAnswers,
                    strings = strings,
                    today = LocalDate.now(),
                    suggested = suggestedCards,
                ),
                onAdd = { offer ->
                    // **Written straight away**, so the card is there when the
                    // person looks. Appended to what was already on the draft,
                    // so a move made before opening the gallery is not lost.
                    savingLayout = onScreen + Repository.TodayCard(
                        id = "",
                        type = offer.type,
                        size = "small",
                        sortIndex = onScreen.size,
                        isLead = false,
                        sourceTable = offer.sourceTable,
                        sourceId = offer.sourceId,
                    )
                    addingCardTo = null
                },
                onBack = { addingCardTo = null },
            )
        }

        settingLead?.let { (projectId, lead) ->
            LaunchedEffect(projectId, lead) {
                repository.setProjectLead(projectId, lead)
                settingLead = null
                revision += 1
            }
        }

        settingStatus?.let { (projectId, status) ->
            LaunchedEffect(projectId, status) {
                // **The waiting-on note is left exactly as it was.** Changing
                // the status is not a reason to forget who somebody was told to
                // wait for, and the old screen cleared it on every change.
                repository.setProjectStatus(
                    projectId = projectId,
                    status = status,
                    waitingOn = projects.firstOrNull { it.id == projectId }?.waitingOn,
                )
                settingStatus = null
                revision += 1
            }
        }

        val currentProject = openProject
        if (currentProject != null && setupOpen && papersOpen) {
            ProjectPapersScreen(
                projectName = currentProject.name,
                papers = projectPapers,
                onAdd = { addingPaper = currentProject.id to it },
                onOpen = { paperUnderEdit = it },
                onBack = { papersOpen = false },
            )

            paperUnderEdit?.let { paper ->
                PaperEditSheet(
                    paper = paper,
                    onSave = { renamingPaper = paper.id to it; paperUnderEdit = null },
                    onEmpty = { emptyingPaper = paper.id; paperUnderEdit = null },
                    onRemove = { removingPaper = paper.id; paperUnderEdit = null },
                    onDismiss = { paperUnderEdit = null },
                )
            }
        } else if (currentProject != null && setupOpen && kindsOpen) {
            ProjectDateKindsScreen(
                projectName = currentProject.name,
                kinds = projectDateKindRows,
                onAdd = { addingKind = currentProject.id to it },
                onOpen = { kindUnderEdit = it },
                onBack = { kindsOpen = false },
            )

            kindUnderEdit?.let { kind ->
                DateKindEditSheet(
                    kind = kind,
                    onSave = { renamingKind = kind.id to it; kindUnderEdit = null },
                    onRemove = { removingKind = kind.id; kindUnderEdit = null },
                    onDismiss = { kindUnderEdit = null },
                )
            }
        } else if (currentProject != null && setupOpen && roadOpen) {
            ProjectRoadScreen(
                projectName = currentProject.name,
                stages = projectStages,
                onAdd = { addingStage = currentProject.id to it },
                onOpen = { stageUnderEdit = it },
                onBack = { roadOpen = false },
            )

            stageUnderEdit?.let { stage ->
                val index = projectStages.indexOfFirst { it.id == stage.id }
                StageEditSheet(
                    stage = stage,
                    canMoveEarlier = index > 0,
                    canMoveLater = index >= 0 && index < projectStages.lastIndex,
                    onSave = { renamingStage = stage.id to it; stageUnderEdit = null },
                    onMove = { reorderingStage = stage.id to it; stageUnderEdit = null },
                    onRemove = { removingStage = stage.id; stageUnderEdit = null },
                    onDismiss = { stageUnderEdit = null },
                )
            }
        } else if (currentProject != null && peopleOpen) {
            ProjectPeopleScreen(
                projectName = currentProject.name,
                people = projectPeople,
                careTeamSize = people.size,
                onOpenPerson = { openProject = null; peopleOpen = false; openPerson = it },
                // **The cross-project door**, which is the one new navigation
                // idea on this surface: it swaps the project underneath rather
                // than stacking a second one on top.
                onOpenProject = { other ->
                    peopleOpen = false
                    openProject = projects.firstOrNull { it.id == other.id }
                },
                onCall = { number -> dial(context, number) },
                onOpenCareTeam = {
                    openProject = null
                    peopleOpen = false
                    openSection = Repository.Section.CARE_TEAM
                },
                onBack = { peopleOpen = false },
            )
        } else if (currentProject != null && paperworkOpen) {
            ProjectPaperworkScreen(
                projectName = currentProject.name,
                papers = projectPaperCards,
                // The document itself, which already knows where it came from,
                // so rule 18 holds both ways.
                onOpenDocument = { id ->
                    openDocument = documents.firstOrNull { it.id == id }
                    if (openDocument == null) openSection = Repository.Section.DOCUMENTS
                },
                onBack = { paperworkOpen = false },
            )
        } else if (currentProject != null && trailOpen) {
            ProjectTrailScreen(
                projectName = currentProject.name,
                items = projectTrail,
                // **Rule 18 both ways**, and precisely: the entry itself rather
                // than the section it lives in, and the entry names this
                // project back since #283.
                onOpenEntry = { openEntry = it.id },
                onBack = { trailOpen = false },
            )
        } else if (currentProject != null && setupOpen && stepsOpen) {
            ProjectStepsScreen(
                projectName = currentProject.name,
                steps = projectSteps,
                onAdd = { addingStep = currentProject.id to it },
                onOpen = { stepUnderEdit = it },
                onBack = { stepsOpen = false },
            )

            stepUnderEdit?.let { step ->
                val index = projectSteps.indexOfFirst { it.id == step.id }
                StepEditSheet(
                    step = step,
                    canMoveEarlier = index > 0,
                    canMoveLater = index >= 0 && index < projectSteps.lastIndex,
                    onSave = { text, note ->
                        editingStep = Triple(step.id, text, note)
                        stepUnderEdit = null
                    },
                    onMove = { earlier ->
                        movingStep = step.id to earlier
                        stepUnderEdit = null
                    },
                    onRemove = {
                        removingStep = step.id
                        stepUnderEdit = null
                    },
                    onDismiss = { stepUnderEdit = null },
                )
            }
        } else if (currentProject != null && setupOpen) {
            ProjectSetupScreen(
                project = currentProject,
                stages = projectStages,
                steps = projectSteps,
                papers = projectPapers,
                dateKinds = projectDateKinds,
                templateName = currentProject.templateId?.let { id ->
                    projectTemplates.firstOrNull { it.id == id }?.name
                },
                onSetLead = { settingLead = currentProject.id to it },
                onSetStatus = { settingStatus = currentProject.id to it },
                // **Both of these had their repository call and their state
                // here and nothing setting them**, from the day the detail
                // screen was superseded until #314 was found by reading the
                // removal ledger against what had actually come back.
                onSetWaitingOn = { settingWaitingOn = currentProject.id to it },
                onSaveAsTemplate = { savingTemplate = currentProject },
                savedAsTemplate = currentProject.id in savedTemplates,
                onOpenSteps = { stepsOpen = true },
                onOpenRoad = { roadOpen = true },
                onOpenKinds = { kindsOpen = true },
                onOpenPapers = { papersOpen = true },
                onBack = { setupOpen = false },
            )
        } else if (currentProject != null) {
            // **The screen the Projects grid draws**, DESIGN.md 20.5 screen 5.
            //
            // The old ProjectDetailScreen is superseded by it and is frozen
            // rather than deleted, per D112 and the row in
            // docs/REMOVAL-LEDGER.md: never called, extended, fixed, or
            // translated.
            val zone = java.time.ZoneId.systemDefault()
            val now = System.currentTimeMillis()

            // **Composed here rather than inside the screen**, so the screen
            // does no arithmetic on a date and every sentence comes from the
            // catalog in the person's own language.
            val daysToDate = projectNextDate?.dueStart?.let { due ->
                java.time.Duration.ofMillis(due - now).toDays()
            }
            val countdownText = when {
                daysToDate == null -> null
                daysToDate == 0L -> strings["project.countdown.today"]
                daysToDate > 0 -> strings(
                    "project.countdown.days",
                    // A Number, not its text. ICU refuses a String for a
                    // plural argument, and the refusal is a crash rather
                    // than a wrong word.
                    "count" to daysToDate,
                )
                else -> strings(
                    "project.countdown.passed",
                    "count" to -daysToDate,
                )
            }
            // **Raw parts into Bidi.join, never pre-isolated ones.** join
            // isolates each part itself, so an isolated part comes out wrapped
            // twice and a joined string passed into another join comes out
            // wrapped three deep. Seen in the semantics tree on the phone as
            // U+2068 U+2068 U+2068 Renewal, which no screenshot would show.
            val dateKindText = projectNextDate?.kind
            val dateWhenText = projectNextDate?.let {
                EventDateText.render(strings, it.dueEdtf)
            }
            val standingSinceText = projectStanding?.let { standing ->
                Bidi.join(
                    standing.activity,
                    standing.sinceEdtf?.let { EventDateText.render(strings, it) },
                )
            }
            // **Who said it and when, as two raw parts**, 20.1. The date
            // alone is half the attribution, and the half that is missing is
            // the one somebody needs when they call back and are asked who they
            // spoke to. Null where nobody was named, which is ordinary: plenty
            // of calls are answered by whoever picked up.
            //
            // **Two parts rather than one joined string**, because Bidi.join
            // isolates whatever it is handed, and handing it something already
            // joined nests the marks. That has now happened three times in
            // three days, each time in code written by somebody who had just
            // fixed it elsewhere, and it is invisible outside the semantics
            // tree.
            val attributionWho = projectLatestWord?.title
            val attributionWhen = projectLatestWord?.let {
                EventDateText.render(strings, it.occurredEdtf)
            }

            ProjectHomeScreen(
                // **Reopening puts it back to active and clears the close
                // date.** 20.5 screen 17: these processes come back, and a file
                // that could only be closed once would make somebody start a
                // second one and lose the history that made the first worth
                // keeping.
                onReopen = { settingStatus = currentProject.id to "active" },
                project = currentProject,
                stages = projectStages,
                standing = projectStanding,
                nextDate = projectNextDate,
                latestWord = projectLatestWord,
                countdown = countdownText,
                dateKind = dateKindText,
                dateWhen = dateWhenText,
                standingSince = standingSinceText,
                attributionWho = attributionWho,
                attributionWhen = attributionWhen,
                steps = projectSteps,
                papers = projectPapers,
                onToggleStep = { togglingStep = it },
                onUpdateStanding = { updatingStanding = currentProject },
                onAddDate = { addingDateTo = currentProject },
                onLogCall = { loggingCallOn = currentProject },
                onOpenSetup = { setupOpen = true },
                // **The project's own screen removes it**, per #218. The
                // projects list carried this on a long press, which is the one
                // path a sighted person cannot find. The screen closes as the
                // confirmation opens.
                onRemove = {
                    removing = Removal(
                        Repository.Section.PROJECTS, currentProject.id, currentProject.name,
                    )
                    openProject = null
                    setupOpen = false
                },
                onMoveStage = { movingStageOn = currentProject },
                entries = projectEntries,
                trailCount = projectTrail.size,
                onOpenTrail = { trailOpen = true },
                onOpenPaperwork = { paperworkOpen = true },
                onOpenPeople = { peopleOpen = true },
                peopleCount = projectPeople.size,
                // **Rule 18 both ways.** The entry already knows the project;
                // this is the project opening the entry.
                onOpenEntryById = { openEntry = it },
                onBack = {
                    openProject = null
                    setupOpen = false
                    trailOpen = false
                    paperworkOpen = false
                    peopleOpen = false
                    stepsOpen = false
                    roadOpen = false
                    kindsOpen = false
                    papersOpen = false
                },
            )
        }

        if (startingProject) {
            StartProjectScreen(
                templates = projectTemplates,
                onChoose = { template ->
                    // **Nothing is created here any more**, 20.5 screen 04.
                    // This used to start the project, its road, its steps, its
                    // papers and its date chips on one tap, and the first time
                    // anybody saw any of that was on a screen that already
                    // existed.
                    startingProject = false
                    previewTemplate = template
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

        previewTemplate?.let { template ->
            StartProjectPreviewSheet(
                template = template,
                onCreate = { name ->
                    previewTemplate = null
                    previewName = name
                    chosenTemplate = template
                },
                onDismiss = {
                    // Back to the picker rather than out of the flow, which is
                    // where the sheet's own label says it goes.
                    previewTemplate = null
                    startingProject = true
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

        if (situationOpen) {
            // Loaded once and held, because the picker and this screen both
            // read it and the catalog is an asset rather than a query.
            LaunchedEffect(Unit) {
                if (situations == null) situations = TemplateCatalog.situations(context)
            }
            val catalog = situations
            if (situationPickerOpen && catalog != null) {
                SituationPickerScreen(
                    situations = catalog,
                    onChoose = {
                        chosenSituation = it
                        situationPickerOpen = false
                    },
                    // **Backing out of the picker leaves the setting alone.**
                    // Skip on this screen means "not now", not "no situation":
                    // clearing what they already had because they opened a
                    // picker and closed it would be the app throwing away an
                    // answer nobody withdrew.
                    onSkip = { situationPickerOpen = false },
                )
            } else {
                ChangeSituationScreen(
                    current = catalog?.all?.firstOrNull { it.id == situationId },
                    chosen = chosenSituation,
                    onOpenPicker = { situationPickerOpen = true },
                    onApply = { chapterName ->
                        chosenSituation?.let {
                            applyingSituation = SituationChange(it, chapterName)
                        }
                    },
                    onBack = { situationOpen = false; chosenSituation = null },
                )
            }
        }

        applyingSituation?.let { change ->
            LaunchedEffect(change.situation.id, change.chapterName) {
                val subjectId = repository.activeSubject()?.id
                if (subjectId != null) {
                    repository.applySituation(
                        subjectId = subjectId,
                        templateId = change.situation.id,
                        // **The threads the new setting offers, added rather
                        // than swapped.** A thread from the old setting is a
                        // thread the person has been writing on, and ending it
                        // because the address changed would be the app deciding
                        // a concern is over.
                        threads = change.situation.threads.map { it.id to it.label },
                        // **The Today this setting ships**, DESIGN.md 21.5, and
                        // only when there is not one already: a person whose
                        // care setting changed keeps the desk they arranged.
                        startingHand = change.situation.startingHand,
                        // **A new setting is a new set of first days**, and
                        // this is where MASTER_SPEC 4.6's carry forward comes
                        // out right by construction: the old setting's list is
                        // a project and nothing here touches it. #135.
                        checklist = change.situation.checklist,
                        documents = change.situation.documents,
                        firstDaysName = strings["situation.firstdays"],
                    )
                    // **The move, not just a new name.** A chapter boundary
                    // that starts a place without ending the one before it
                    // leaves two places somebody is in at once, and the
                    // chapters screen shows both as current. Seen on the phone.
                    change.chapterName?.let { repository.moveToChapter(subjectId, it) }
                }
                applyingSituation = null
                chosenSituation = null
                situationOpen = false
                revision += 1
            }
        }

        savingLayout?.let { cards ->
            LaunchedEffect(cards) {
                val subject = repository.activeSubject()
                if (subject != null) {
                    repository.setTodayLayout(
                        subjectId = subject.id,
                        cards = cards.map { it.type to it.size },
                        // **The sources travel with the cards.** A card that
                        // points at one project must still point at it after a
                        // reorder, and 8.7 keeps a source that no longer
                        // resolves rather than dropping the card.
                        sources = cards.withIndex().mapNotNull { (index, card) ->
                            val table = card.sourceTable
                            val id = card.sourceId
                            if (table != null && id != null) index to (table to id) else null
                        }.toMap(),
                    )
                }
                savingLayout = null
                revision += 1
            }
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
                        // What the person called it, which the preview
                        // pre-filled from the template and they may have
                        // changed. Rule 20: what they typed is what it is.
                        name = previewName?.takeIf { it.isNotBlank() } ?: template.name,
                        steps = template.steps,
                        lead = template.lead,
                        stages = template.stages,
                        dateKinds = template.dateKinds,
                        papers = template.papers,
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
                    // **All five defaults, not only the steps.** DESIGN.md
                    // 20.4: a project template is a bundle of five, and a
                    // project started with just the checklist has no road to
                    // draw, no chips when a date is recorded, and no papers.
                    repository.startProject(
                        subjectId = subject.id,
                        templateId = template.id,
                        // What the person called it, which the preview
                        // pre-filled from the template and they may have
                        // changed. Rule 20: what they typed is what it is.
                        name = previewName?.takeIf { it.isNotBlank() } ?: template.name,
                        steps = template.steps,
                        lead = template.lead,
                        stages = template.stages,
                        dateKinds = template.dateKinds,
                        papers = template.papers,
                    )
                }
                chosenTemplate = null
                previewName = null
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

        addingPaper?.let { pending ->
            LaunchedEffect(pending) {
                repository.addProjectPaper(pending.first, pending.second)
                addingPaper = null
                revision += 1
            }
        }

        renamingPaper?.let { pending ->
            LaunchedEffect(pending) {
                repository.renameProjectPaper(pending.first, pending.second)
                renamingPaper = null
                revision += 1
            }
        }

        emptyingPaper?.let { id ->
            LaunchedEffect(id) {
                repository.emptyProjectPaper(id)
                emptyingPaper = null
                revision += 1
            }
        }

        removingPaper?.let { id ->
            LaunchedEffect(id) {
                repository.removeProjectPaper(id)
                removingPaper = null
                revision += 1
            }
        }

        addingKind?.let { pending ->
            LaunchedEffect(pending) {
                repository.addProjectDateKind(pending.first, pending.second)
                addingKind = null
                revision += 1
            }
        }

        renamingKind?.let { pending ->
            LaunchedEffect(pending) {
                repository.renameProjectDateKind(pending.first, pending.second)
                renamingKind = null
                revision += 1
            }
        }

        removingKind?.let { id ->
            LaunchedEffect(id) {
                repository.removeProjectDateKind(id)
                removingKind = null
                revision += 1
            }
        }

        addingStage?.let { pending ->
            LaunchedEffect(pending) {
                repository.addProjectStage(pending.first, pending.second)
                addingStage = null
                revision += 1
            }
        }

        renamingStage?.let { pending ->
            LaunchedEffect(pending) {
                repository.renameProjectStage(pending.first, pending.second)
                renamingStage = null
                revision += 1
            }
        }

        reorderingStage?.let { pending ->
            LaunchedEffect(pending) {
                repository.moveProjectStage(pending.first, pending.second)
                reorderingStage = null
                revision += 1
            }
        }

        removingStage?.let { id ->
            LaunchedEffect(id) {
                repository.removeProjectStage(id)
                removingStage = null
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
                    // The month header is the door, per rule 18 and 13.5. Any
                    // instant inside the month says which month it is, and the
                    // zone turns it into one in the same place the review does.
                    onReview = { millis ->
                        reviewMonth = java.time.YearMonth.from(
                            java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault()),
                        )
                    },
                    // **Pinning writes and then reloads, like every other write
                    // in the shell.** The pinned run is derived from the rows
                    // rather than held beside them, so a pin that only moved a
                    // flag in memory would be correct until the screen was left
                    // and wrong afterward.
                    onBack = { openSection = null },
                )

                Repository.Section.DOCUMENTS -> DocumentsScreen(
                    documents = documents,
                    onOpen = { document -> openDocument = document },
                    onAdd = {
                        editingDocument = null
                        documentError = null
                        addingDocument = true
                    },
                    onBack = { openSection = null },
                )

                Repository.Section.MONEY -> MoneyScreen(
                    bills = bills,
                    onOpen = { bill -> openBill = bill },
                    onAdd = { editingBill = null; addingBill = true },
                    onBack = { openSection = null },
                )

                Repository.Section.STANDING_INSTRUCTIONS -> StandingInstructionsScreen(
                    instructions = instructions,
                    tags = instructionCatalog?.tags.orEmpty(),
                    // **One handler where there were two**, per #218: the row
                    // opens the request and the sheet it opens is where the
                    // request is taken off the list.
                    onOpen = { acknowledging = it },
                    onAdd = { addingInstruction = true },
                    onBack = { openSection = null },
                    violations = violationsByInstruction,
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
                    onOpen = { appointment -> openPrepFor = appointment.id },
                    onAdd = { editingAppointment = null; addingAppointment = true },
                    onBack = { openSection = null },
                )

                Repository.Section.CHAPTERS -> ChaptersScreen(
                    // What each chapter holds, so the current place says it on
                    // its own card rather than only inside. #356.
                    contents = chapterContents,
                    onOpen = { openChapter = it },
                    chapters = chapters,
                    onOpenMilestones = { milestonesOpen = true },
                    onBack = { openSection = null },
                )

                Repository.Section.PROGRESS -> ProgressScreen(
                    measures = measures,
                    readings = readings,
                    // The measurement form, which is a real destination rather
                    // than a stub: it is the same screen capture opens.
                    onAddReading = { capturing = CaptureKind.MEASUREMENT },
                    onBack = { openSection = null },
                )

                Repository.Section.THREADS -> CareThreadsScreen(
                    onOpen = { openThread = it },
                    onAdd = { addingThread = true },
                    threads = threadCounts,
                    onBack = { openSection = null },
                )

                Repository.Section.ASK_NEXT_TIME -> QuestionsScreen(
                    questions = questions,
                    // Marked asked as of today, which is the honest default: the
                    // person is tapping it because it just happened. The date is
                    // editable later like every other date, per rule 17.
                    onMarkAsked = { markingAsked = it },
                    onOpen = { answering = it },
                    // The capture form's question face, which is where a
                    // question is written down everywhere else in the app.
                    // #355.
                    onAdd = { capturing = CaptureKind.QUESTION },
                    onBack = { openSection = null },
                )

                Repository.Section.MEDICATIONS -> MedicationsScreen(
                    medications = medications,
                    openQuestions = medicationQuestionCounts,
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
                    onShare = { sharingCard = true },
                    onBack = { openSection = null },
                )

                Repository.Section.CARE_TEAM -> CareTeamScreen(
                    people = people,
                    // The ones somebody actually calls lead the screen, #351.
                    // This list is already loaded for the capture form, which
                    // has been asking the same question all along.
                    byRecentUse = peopleForCapture,
                    // ACTION_DIAL rather than ACTION_CALL, which would need the
                    // call permission. The dialer opens with the number filled in
                    // and the person presses the green button themselves. That is
                    // one extra tap and it is the right one: this app asks for no
                    // permission it does not need, and placing a call on somebody's
                    // behalf is not a thing it should be able to do silently.
                    onCall = { person -> dial(context, person.phone) },
                    onOpen = { person -> openPerson = person },
                    onAdd = { editingPerson = null; addingPerson = true },
                    onBack = { openSection = null },
                )

                // **Named rather than left to an else, so the compiler is the
                // guard.** Projects are a destination of their own and have no
                // section screen, and while this branch said `else` a section
                // added later would have compiled cleanly and shipped the one
                // screen in the app with nothing on it. Rule 11 is not a thing
                // to remember at review time if the build can hold it.
                //
                // Nothing sets this any more: search used to, and now opens the
                // project itself. It stays reachable only if some later route
                // forgets again, and then it says so rather than looking blank.
                Repository.Section.PROJECTS -> {
                    NotBuiltYet(
                        // bidi-ok: a section label from the catalog, in the app's own words.
                        name = strings[labelKey(Repository.Section.PROJECTS)],
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
            // **The row as the database now has it**, so a correction shows on
            // the screen behind the form rather than the version that was
            // opened. `openIncident` is a snapshot taken when it was tapped,
            // and after #358 that snapshot can be out of date by a title.
            val current = openIncident?.let { opened ->
                incidents.firstOrNull { it.id == opened.id } ?: opened
            }
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
                    // **The incident stays open underneath this too.** It
                    // closed itself to show a person, so checking the charge
                    // nurse's number mid-read cost three taps to get back and
                    // the way back named the care team, which is not where the
                    // person had been. #360.
                    onOpenPerson = { person -> openPerson = person },
                    // **The incident stays open underneath**, so somebody who
                    // reads one entry and comes back is where they were rather
                    // than two taps away from it. Rule 18 and #46, and it is
                    // the same choice the prep sheet already makes.
                    onOpenEntry = { entry -> openEntry = entry.id },
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
                    // **The screen closes as the confirmation opens**, the same
                    // as a medication's, so the sheet is not asking about a
                    // thing the screen behind it still shows. #358.
                    onRemove = {
                        removing = Removal(
                            section = null,
                            rowId = current.id,
                            what = current.title,
                            remove = { repository.removeIncident(it) },
                        )
                        openIncident = null
                    },
                    // The incident stays open underneath, so coming back from
                    // the letter lands on the incident it came out of. #360.
                    onOpenDocument = { openDocument = it },
                    onCorrect = { correctingIncident = current },
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
                    // **The two arguments nothing had ever passed.** #371, and
                    // both readers on the instruction's own row were dead until
                    // this line: the form now asks, from behind the disclosure,
                    // and never requires it.
                    incidentId = draft.incidentId,
                    billId = draft.billId,
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
                // What it could have broken against, offered and never
                // required. Both lists are already loaded for their own
                // sections, so this costs no query.
                incidents = incidents,
                bills = bills,
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
                // **The medication's own screen removes it**, per #218. The
                // screen closes as the confirmation opens, so the sheet is not
                // asking about a thing the screen behind it still shows.
                onRemove = {
                    removing = Removal(
                        Repository.Section.MEDICATIONS, medication.id, medication.name,
                    )
                    openMedication = null
                },
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
                    // The chapter stays open underneath, per #360.
                    onOpenEntry = { openEntry = it.id },
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
                // **The thread stays open underneath**, the way the incident
                // and the prep sheet already do, so reading three entries off
                // one thread is three taps rather than nine. #360.
                onOpenEntry = { openEntry = it.id },
                onRename = { renamingThread = thread },
                onBack = { openThread = null },
            )
        }

        // **One month of the trail, gathered.** The second half of #200 and the
        // last row of `DESIGN.md` section 14 that pointed at nothing. It sits
        // above the trail rather than inside it because it is a screen the trail
        // opens, and every line on it opens something further.
        reviewMonth?.let { month ->
            LaunchedEffect(month, revision) {
                val subjectId = repository.activeSubject()?.id
                review = subjectId?.let { repository.monthReview(it, month) }
                if (review == null) reviewMonth = null
            }
            review?.let { gathered ->
                MonthReviewScreen(
                    review = gathered,
                    // **The review stays open underneath every door.** Somebody
                    // reading one entry out of June and coming back belongs in
                    // June, not on the trail they passed through. Rule 18, and
                    // the same shape the prep sheet uses.
                    onOpenEntry = { openEntry = it.id },
                    onOpenMilestones = { milestonesOpen = true },
                    onOpenChapter = { openChapter = it },
                    onOpenAppointment = { openPrepFor = it.id },
                    // **The review closes and the incident opens.** Setting
                    // the row alone did nothing at all: the incident screen
                    // renders inside `incidentsOpen` and earlier in this file,
                    // so the review painted over it, leaving a control with a
                    // press state and no result and a back press that went to
                    // the swallowed handler instead. The review is one tap from
                    // the trail's own month heading, which is where the person
                    // came from. #360.
                    onOpenIncident = {
                        reviewMonth = null
                        review = null
                        incidentsOpen = true
                        openIncident = it
                    },
                    onOpenDocument = { openDocument = it },
                    onShare = { sharingReview = gathered },
                    onBack = { reviewMonth = null; review = null },
                )
            }
        }

        if (milestonesOpen) {
            MilestonesScreen(
                milestones = milestones,
                // **Tapping one opens the form that changes it**, which is the
                // exception the bill and the document taught: a milestone is a
                // label, a date and a note, and a detail screen for three lines
                // would be a door onto the same three lines. Rule 17 keeps the
                // date editable forever and this is where that happens.
                onOpen = { editingMilestone = it; addingMilestone = true },
                onOpenChapter = { milestonesOpen = false; openChapter = chapters.firstOrNull { c -> c.id == it } },
                onAdd = { editingMilestone = null; addingMilestone = true },
                onBack = { milestonesOpen = false },
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
                    // **The sheet stays open underneath.** Somebody reading a
                    // change and coming back had been dropped on the
                    // appointments list, which is two taps to get back to the
                    // place they were already standing. Rule 18.
                    onOpenEntry = { openEntry = it.id },
                    onShare = { sharingPrep = sheet },
                    onWriteUp = {
                        // The ordinary capture form, so what comes out is an
                        // ordinary entry on the trail rather than a special
                        // kind only this screen knows about.
                        capturing = CaptureKind.VISIT
                    },
                    // **The appointment's own screen corrects it**, #360, the
                    // same door every other row type has had all along.
                    // The prep sheet stays open underneath, so ticking a
                    // question off leaves the person on the list they walked
                    // in with. #360.
                    onOpenQuestion = { answering = it },
                    onCorrect = {
                        editingAppointment = sheet.appointment
                        addingAppointment = true
                    },
                    // **The appointment's own screen removes it**, per #218.
                    onRemove = {
                        removing = Removal(
                            Repository.Section.APPOINTMENTS,
                            sheet.appointment.id,
                            sheet.appointment.title,
                        )
                        openPrepFor = null
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
                onSetPinned = { pinned -> pinningPerson = person.id to pinned },
                onSetArchived = { archived -> archivingPerson = person.id to archived },
                onEdit = { editingPerson = person; addingPerson = true; openPerson = null },
                // **The name travels, the kind does not.** What happened is
                // the person's to say, so this opens the same sheet the gold
                // button opens and carries only who it is about. #46, rule 18.
                //
                // **The person's screen closes first**, so back from the
                // capture form lands on the care team rather than on a screen
                // about somebody the entry is now already attached to.
                // **It carries the name forward without destroying a draft.**
                // This replaced the whole state with a fresh one, so a half
                // written note somebody had left in the capture form was gone
                // the moment they tapped "Write something down" on a person.
                // The capture form's own comment calls losing a half written
                // note "the worst thing this app could do short of losing the
                // notebook", and one button in the app did it. #371.
                //
                // **The person is attached to whatever is already there**, and
                // an empty draft behaves exactly as before.
                onCapture = {
                    captureDraft = captureDraft.togglePerson(person)
                    openPerson = null
                    sheetOpen = true
                },
                onRemove = {
                    removing = Removal(
                        section = Repository.Section.CARE_TEAM,
                        rowId = person.id,
                        what = person.displayName.ifBlank {
                            person.phone.orEmpty().ifBlank { person.roleLabel.orEmpty() }
                        },
                    )
                    openPerson = null
                },
                // The person stays open underneath, per #360.
                onOpenEntry = { openEntry = it.id },
                onBack = { openPerson = null },
                // **The way back names where the person actually came from.**
                // It said "back to the care team" whatever opened it, so
                // somebody who reached a nurse from an incident, a project or
                // the trail was told to go somewhere they had not been. The
                // entry screen has varied this by origin all along. #360.
                backLabelKey = when {
                    openIncident != null -> "section.back.incident"
                    openProject != null -> "section.back.project"
                    openEntry != null -> "section.back.trail"
                    openThread != null -> "section.back.threads"
                    else -> "section.back.careteam"
                },
            )
        }

        // **A document's own screen**, same story as the bill: the row opened
        // the editor, so nothing in the app ever showed a piece of the person's
        // own paper at a size somebody could read.
        openDocument?.let { current ->
            val fresh = documents.firstOrNull { it.id == current.id } ?: current
            LaunchedEffect(current.id, revision) {
                documentFilings = repository.filingsForDocument(current.id)
            }
            DocumentScreen(
                document = fresh,
                filings = documentFilings,
                // **The project itself**, and it keeps the papers screen
                // underneath so back returns where the person came from.
                onOpenProject = { id ->
                    openDocument = null
                    openProject = projects.firstOrNull { it.id == id }
                    if (openProject == null) destination = Destination.PROJECTS
                },
                // **Names where it goes.** Reached from a project's papers this
                // returns there, and "Back to documents" was the same small lie
                // the entry screen told before #283.
                backLabelKey = if (paperworkOpen) {
                    "section.back.project_papers"
                } else {
                    "section.back.documents"
                },
                onEdit = {
                    editingDocument = fresh
                    documentError = null
                    addingDocument = true
                },
                onRemove = {
                    removing = Removal(
                        Repository.Section.DOCUMENTS, fresh.id, fresh.title,
                    )
                    openDocument = null
                },
                onOpenChapter = { chapterId ->
                    openDocument = null
                    openChapter = chapters.firstOrNull { it.id == chapterId }
                    if (openChapter == null) openSection = Repository.Section.CHAPTERS
                },
                onBack = { openDocument = null },
            )
        }

        // **A bill's own screen, which nothing could reach until 2026-08-04.**
        // Tapping a bill opened the editor, so the place it came out of and who
        // sent it had nowhere to appear.
        openBill?.let { current ->
            val fresh = bills.firstOrNull { it.id == current.id } ?: current
            BillScreen(
                bill = fresh,
                onEdit = { editingBill = fresh; addingBill = true },
                onRemove = {
                    removing = Removal(
                        Repository.Section.MONEY, fresh.id, fresh.description,
                    )
                    openBill = null
                },
                onOpenChapter = { chapterId ->
                    openBill = null
                    openChapter = chapters.firstOrNull { it.id == chapterId }
                    if (openChapter == null) openSection = Repository.Section.CHAPTERS
                },
                onBack = { openBill = null },
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
                    // **The way back names where it actually goes.** An entry
                    // opened from a project's "What was said" returns to that
                    // project and said "Back to the notebook" while doing it,
                    // which is the same small lie the setup screen told about
                    // the projects list and that somebody only notices by being
                    // surprised. Found by tapping it, not by reading it.
                    backLabelKey = when {
                        openSection != null -> "section.back.trail"
                        openProject != null -> "section.back.project"
                        else -> "section.back"
                    },
                    onEditDate = { editingDate = detail.entry },
                    onCorrect = { correctingEntry = detail.entry },
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
                    // **The project itself, not the projects tab.** The
                    // difference between a link and a signpost, the same one
                    // the chapter door above makes. #283 closes the last one
                    // way link on this screen: a call logged from inside a
                    // project could be opened from the project and had no way
                    // back to it.
                    onOpenProject = { project ->
                        openEntry = null
                        openProject = projects.firstOrNull { it.id == project.id }
                        // A project the list has not loaded is not a reason to
                        // strand somebody on nothing: the tab always opens.
                        if (openProject == null) destination = Destination.PROJECTS
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
                val remover = confirmed.remove
                if (remover != null) {
                    remover(confirmed.rowId)
                } else {
                    repository.delete(confirmed.section!!, confirmed.rowId)
                }
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
                // The sheet closes first, so the confirmation is not a sheet
                // over a sheet, which Material stacks and a person reads as one
                // screen that has stopped responding.
                onRemove = {
                    removing = Removal(
                        Repository.Section.STANDING_INSTRUCTIONS,
                        toAcknowledge.id,
                        toAcknowledge.name,
                    )
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
                onMarkAsked = {
                    markingAsked = toAnswer
                    answering = null
                },
                onRemove = {
                    removing = Removal(
                        Repository.Section.ASK_NEXT_TIME, toAnswer.id, toAnswer.text,
                    )
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

        // **Correcting an entry, over whatever it was opened from.** #368, and
        // it is the last of the app's records to get a correction path.
        correctingEntry?.let { entry ->
            CorrectEntryScreen(
                entry = entry,
                onSave = { correction ->
                    savingEntryCorrection = entry.id to correction
                    correctingEntry = null
                },
                onCancel = { correctingEntry = null },
            )
        }

        // **Correcting who this is about, over the More tab it is reached from.**
        LaunchedEffect(correctingSubject) {
            subjectRow = if (correctingSubject) repository.activeSubject() else null
        }

        val subjectNow = subjectRow
        if (correctingSubject && subjectNow != null) {
            CorrectSubjectScreen(
                subject = subjectNow,
                onSave = { correction ->
                    savingSubject = correction
                    correctingSubject = false
                },
                onCancel = { correctingSubject = false },
            )
        }

        val subjectEdit = savingSubject
        val subjectTarget = subjectRow
        if (subjectEdit != null && subjectTarget != null) {
            LaunchedEffect(subjectEdit) {
                repository.updateSubject(
                    subjectId = subjectTarget.id,
                    // bidi-ok: on its way to the database. Isolate marks here
                    // would be stored as part of what the person typed.
                    displayName = subjectEdit.displayName.trim(),
                    relationship = subjectEdit.relationship.trim(),
                )
                savingSubject = null
                revision += 1
            }
        }

        renamingThread?.let { thread ->
            AddThreadScreen(
                existing = thread,
                onStart = { label ->
                    savingThreadRename = thread.id to label
                    renamingThread = null
                },
                onCancel = { renamingThread = null },
            )
        }

        val threadRename = savingThreadRename
        if (threadRename != null) {
            LaunchedEffect(threadRename) {
                // bidi-ok: on its way to the database.
                repository.renameThread(threadRename.first, threadRename.second)
                savingThreadRename = null
                revision += 1
            }
        }

        val personArchive = archivingPerson
        if (personArchive != null) {
            LaunchedEffect(personArchive) {
                repository.setPersonArchived(personArchive.first, personArchive.second)
                archivingPerson = null
                openPerson = null
                revision += 1
            }
        }

        val personPin = pinningPerson
        if (personPin != null) {
            LaunchedEffect(personPin) {
                repository.setPersonPinned(personPin.first, personPin.second)
                pinningPerson = null
                revision += 1
            }
        }

        val entryCorrection = savingEntryCorrection
        if (entryCorrection != null) {
            LaunchedEffect(entryCorrection) {
                val (entryId, correction) = entryCorrection
                // bidi-ok: on its way to the database. Isolate marks here would
                // be stored as part of what the person typed.
                repository.updateEntry(
                    entryId = entryId,
                    // bidi-ok: a correction on its way to the database. Isolate
                    // marks here would be stored as part of what the person typed.
                    title = correction.title.trim(),
                    // bidi-ok: the same, and it is the note itself.
                    body = correction.body.trim(),
                )
                savingEntryCorrection = null
                revision += 1
            }
        }

        if (addingDocument) {
            AddDocumentScreen(
                existing = editingDocument,
                error = documentError,
                folders = documentFolders,
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
                // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
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
                        category = documentDraft.category,
                        received = documentDraft.received,
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
                            // **What the person said, and unknown when they
                            // said nothing.** This was `Edtf.day(now())`, so
                            // every document was stamped with the day it was
                            // photographed and nothing could correct it: a
                            // letter from three weeks ago arrived today,
                            // forever, in the notebook and in the archive.
                            // #339, rule 17, and DESIGN.md 9.2.
                            received = documentDraft.received ?: Edtf.unknown(),
                            originalLocation = documentDraft.originalLocation,
                            notes = documentDraft.notes,
                            sha256 = file?.sha256,
                            byteSize = file?.byteSize ?: 0,
                            mimeType = picked?.let { context.contentResolver.getType(it) },
                            category = documentDraft.category,
                            chapterId = repository.currentChapterId(subject.id),
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
                        // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
                        title = appointmentDraft.title.trim(),
                        scheduled = appointmentDraft.scheduled ?: Edtf.unknown(),
                        locationNote = appointmentDraft.where,
                        notes = appointmentDraft.notes,
                    )
                } else if (subject != null && appointmentDraft.title.isNotBlank()) {
                    repository.createAppointment(
                        subjectId = subject.id,
                        // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
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

        if (addingMilestone) {
            AddMilestoneScreen(
                existing = editingMilestone,
                chapters = chapters,
                onSave = { draft ->
                    addingMilestone = false
                    savingMilestone = draft
                },
                onCancel = { addingMilestone = false; editingMilestone = null },
            )
        }

        val milestoneDraft = savingMilestone
        if (milestoneDraft != null) {
            LaunchedEffect(milestoneDraft) {
                val subject = repository.activeSubject()
                // **A milestone with no words is not one.** The date can be
                // anything, including unknown, but something has to have been
                // marked, and a blank save is somebody backing out rather than
                // recording an empty moment.
                val correcting = editingMilestone
                if (correcting != null && milestoneDraft.label.isNotBlank()) {
                    repository.updateMilestone(
                        milestoneId = correcting.id,
                        // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
                        label = milestoneDraft.label.trim(),
                        occurred = milestoneDraft.occurred ?: Edtf.unknown(),
                        chapterId = milestoneDraft.chapterId,
                        note = milestoneDraft.note,
                    )
                } else if (subject != null && milestoneDraft.label.isNotBlank()) {
                    repository.createMilestone(
                        subjectId = subject.id,
                        // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
                        label = milestoneDraft.label.trim(),
                        occurred = milestoneDraft.occurred ?: Edtf.unknown(),
                        chapterId = milestoneDraft.chapterId,
                        note = milestoneDraft.note,
                    )
                }
                editingMilestone = null
                savingMilestone = null
                revision += 1
            }
        }

        val correctingNow = correctingIncident
        if (correctingNow != null) {
            CorrectIncidentScreen(
                incident = correctingNow,
                onSave = { values ->
                    correctingIncident = null
                    savingCorrection = correctingNow.id to values
                },
                onCancel = { correctingIncident = null },
            )
        }

        val incidentCorrection = savingCorrection
        if (incidentCorrection != null) {
            LaunchedEffect(incidentCorrection) {
                val (incidentId, values) = incidentCorrection
                if (values.title.isNotBlank()) {
                    repository.updateIncident(
                        incidentId = incidentId,
                        // bidi-ok: a draft on its way to the database. Isolate
                        // marks here would be stored as part of the words.
                        title = values.title,
                        description = values.description,
                        reported = values.reported,
                    )
                }
                savingCorrection = null
                // The screen behind resolves the row from the reloaded list,
                // so bumping the revision is the whole of the refresh.
                revision += 1
            }
        }

        if (addingThread) {
            AddThreadScreen(
                onStart = { name ->
                    addingThread = false
                    savingThread = name
                },
                onCancel = { addingThread = false },
            )
        }

        val threadName = savingThread
        if (threadName != null) {
            LaunchedEffect(threadName) {
                val subject = repository.activeSubject()
                if (subject != null && threadName.isNotBlank()) {
                    val id = repository.createThread(
                        subjectId = subject.id,
                        label = threadName,
                        // The end of the list and the next route color, so a
                        // new thread does not land on the first one's color.
                        sortIndex = threadCounts.size,
                    )
                    // **Opened straight away**, the same as a project started
                    // from nothing: somebody who has just named a thread is
                    // standing in front of it, and rule 18 counts the taps.
                    // Read back rather than assembled here, so the screen shows
                    // what the database holds.
                    openThread = repository.threads(subject.id).firstOrNull { it.id == id }
                }
                savingThread = null
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
                val anything = listOf(person.name, person.role, person.phone, person.notes)
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
                        notes = person.notes.trim(),
                    )
                } else if (subject != null && anything) {
                    repository.createPerson(
                        subjectId = subject.id,
                        displayName = person.name.trim(),
                        phone = person.phone.trim(),
                        roleLabel = person.role.trim().ifBlank { null },
                        notes = person.notes.trim().ifBlank { null },
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
            CaptureBloom(
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
                folders = documentFolders,
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
                    // **Three ways in, and the third is the person's own.**
                    // Sixteen presets was the whole catalog and it is not the
                    // world, #203, which is the same argument createProject
                    // already makes about sixteen catalog processes.
                    val own = measurement.own
                    val measureId = measurement.measureId
                        ?: if (own != null) {
                            repository.createOwnMeasure(
                                subjectId = subject.id,
                                name = own.name,
                                unit = own.unit,
                                isText = own.isText,
                                sortIndex = measures.size,
                            )
                        } else {
                            repository.createMeasure(
                                subjectId = subject.id,
                                preset = measurement.preset!!,
                                unit = measurement.unit,
                                sortIndex = measures.size,
                            )
                        }
                    repository.recordMeasurement(
                        measureId = measureId,
                        number = measurement.number,
                        // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
                        text = measurement.text,
                        unit = measurement.unit,
                        occurred = measurement.occurred,
                        note = measurement.note,
                        // **So the reading lands on the trail.** #371: this
                        // writer took an entry id from the beginning and the
                        // only caller passed none, so a measurement was in no
                        // month review, no prep sheet and no digest.
                        subjectId = subject.id,
                        chapterId = repository.currentChapterId(subject.id),
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
                        // bidi-ok: a draft on its way to the database. Isolate marks here would be stored as part of what the person typed.
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
                        chapterId = repository.currentChapterId(subject.id),
                    )
                } else if (subject != null) {
                    val entryId = repository.createEntry(
                        subjectId = subject.id,
                        kind = draft.kind.entryKind(),
                        title = draft.who,
                        // bidi-ok: a draft on its way to the database.
                        body = draft.note,
                        occurred = draft.occurred,
                        // **Where she was when this happened**, stamped at
                        // write time from the chapter with no end date. #371:
                        // `createEntry` has accepted this since it was written
                        // and neither caller passed it, so `ChapterScreen`'s
                        // entries group could never be non-empty, the chapters
                        // list could never show a count, and the index on
                        // `entry.chapter_id` had nothing in it. The medication
                        // event path already did this and is the shape copied.
                        chapterId = repository.currentChapterId(subject.id),
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

/**
 * The card types a situation's starting hand puts on Today. `DESIGN.md` 21.5.
 *
 * **The types rather than the whole hand**, because the gallery is choosing what
 * to offer rather than rebuilding a layout: what it needs to know is whether
 * this kind of card is one the situation suggested.
 *
 * Empty for somebody who skipped the situation question, which is a real answer
 * and not a gap: the gallery simply has no group to put first.
 */
private suspend fun startingHandFor(
    context: android.content.Context,
    templateId: String?,
): Set<String> {
    if (templateId == null) return emptySet()
    return TemplateCatalog.situations(context).all
        .firstOrNull { it.id == templateId }
        ?.startingHand
        ?.map { it.first }
        ?.toSet()
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
 *
 * **Percent encoded, and this was a real defect for as long as this existed.**
 * `Uri.fromParts` builds an opaque URI without escaping anything, so a number
 * recorded the way people write them down, "555 0142", produced `tel:555 0142`,
 * and **the dialer opened with an empty keypad**. Every number in the fixture
 * has a space in it and so does almost every number anybody types, so the one
 * tap this app promises landed on a blank screen every time. Proved on the
 * phone by dialing the same number three ways: unescaped opened blank, and both
 * `tel:5550142` and `tel:555%200142` filled the keypad.
 *
 * **The number is never reformatted**, per rule 20 and the record's own rule:
 * escaping is about the URI, and the dialer decodes it back to exactly what the
 * person wrote. An international number keeps its plus, which was checked the
 * same way.
 */
private fun dial(context: android.content.Context, phone: String?) {
    phone?.takeIf { it.isNotBlank() }?.let { number ->
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                ("tel:" + android.net.Uri.encode(number)).toUri(),
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
    /**
     * The section the row lives in, for everything the notebook lists.
     *
     * **Null where the thing has no section of its own**, which today means an
     * incident: the notebook's table of contents draws twelve rows and adding a
     * thirteenth to the enum to reach a removal would put one on the screen.
     * Those carry [remove] instead. #358.
     */
    val section: Repository.Section?,
    val rowId: String,
    val what: String,
    /** How to remove it, where [section] cannot say. Null means use the section. */
    val remove: (suspend (String) -> Unit)? = null,
)

/**
 * The name the file picker opens with.
 *
 * The shape `contract/EXPORT-FORMAT.md` section 1 specifies, so a person with
 * several of these can tell them apart by name alone.
 *
 * **`.zip`, not `.htx`.** A private extension made the file recognizable and
 * made it a dead end: somebody who copies their archive to a laptop and double
 * clicks it gets nothing, on the first step of the one procedure the whole
 * two-layer container exists to make possible. The outer layer is a plain zip
 * precisely so that any machine can open it, and the extension is what tells
 * the machine that. D98.
 */
private fun exportFileName(): String {
    val now = java.time.LocalDateTime.now()
    val stamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
    return "healthtrail-export-$stamp.zip"
}

internal val SECTION_ORDER = listOf(
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

/** The app's one loading treatment, so the shell and the root cannot drift. */
@Composable
private fun Loading() = Waiting()

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
        // bidi-ok: a route name in the developer's words on a placeholder screen, never the person's.
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

/**
 * Where a card's answer lives, so tapping it opens the whole of it.
 *
 * `DESIGN.md` 21.2: the answer renders and the card is a door to where the
 * answer lives. **Null for a card whose home is not one of the sections**, and
 * those are handled by the caller rather than being sent somewhere close enough.
 */
internal fun sectionForCard(type: String): Repository.Section? = when (type) {
    "next_up" -> Repository.Section.APPOINTMENTS
    "medications" -> Repository.Section.MEDICATIONS
    "ask_next_time" -> Repository.Section.ASK_NEXT_TIME
    "measure", "milestones" -> Repository.Section.PROGRESS
    "money" -> Repository.Section.MONEY
    "recent_documents" -> Repository.Section.DOCUMENTS
    "care_team" -> Repository.Section.CARE_TEAM
    "standing_instructions" -> Repository.Section.STANDING_INSTRUCTIONS
    "emergency_card" -> Repository.Section.EMERGENCY_CARD
    "trail_lately", "digest", "unfiled" -> Repository.Section.TRAIL
    "incidents" -> Repository.Section.TRAIL
    else -> null
}

/** Four things travelling together, which Kotlin has no name for. */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

/**
 * One pending write of where a project stands, held between the sheet and the
 * effect that saves it.
 *
 * **Named rather than a Triple with a date bolted on.** Two of these fields are
 * free text the person typed and they are adjacent, which is the shape that
 * gets passed the wrong way round with nothing to catch it.
 */
private data class StandingWrite(
    val projectId: String,
    val holder: String,
    val activity: String,
    val since: com.kamsiob.healthtrail.time.Edtf.Date,
)
