package com.kamsiob.healthtrail.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.kamsiob.healthtrail.data.Repository
import java.time.LocalDate
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.components.BottomNav
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.screens.MoreScreen
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import androidx.compose.foundation.background
import com.kamsiob.healthtrail.ui.components.FilledButton
import com.kamsiob.healthtrail.ui.components.TextAction
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
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
import com.kamsiob.healthtrail.ui.screens.kindLabelKey
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.time.Edtf
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
    var capturing by remember { mutableStateOf<CaptureKind?>(null) }
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
    // The entry whose date is being corrected, per rule 17. Null means nothing
    // is being edited.
    var editingDate by remember { mutableStateOf<Repository.TrailEntry?>(null) }
    // The correction in flight: which entry, and the date the person chose.
    var correcting by remember { mutableStateOf<Pair<String, Edtf.Date>?>(null) }
    // Somebody being added to the care team, and the draft being written.
    var addingPerson by remember { mutableStateOf(false) }
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
    val context = LocalContext.current

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
            threads = subject?.let { repository.threads(it.id) }.orEmpty()
            unfiled = subject?.let { repository.unfiled(it.id) }.orEmpty()
            // Read alongside the counts rather than when a section opens, so
            // opening one is instant and never shows a spinner over a number
            // the person is already looking at.
            trail = subject?.let { repository.trail(it.id) }.orEmpty()
            people = subject?.let { repository.people(it.id) }.orEmpty()
            emergencyCard = subject?.let { repository.emergencyCard(it.id) }
            medications = subject?.let { repository.medications(it.id) }.orEmpty()
            questions = subject?.let { repository.questions(it.id) }.orEmpty()
            threadCounts = subject?.let { repository.threadsWithCounts(it.id) }.orEmpty()
            readings = subject?.let { repository.readings(it.id) }.orEmpty()
            chapters = subject?.let { repository.chapters(it.id) }.orEmpty()
            appointments = subject?.let { repository.appointments(it.id) }.orEmpty()
            instructions = subject?.let { repository.standingInstructions(it.id) }.orEmpty()
            bills = subject?.let { repository.bills(it.id) }.orEmpty()
            documents = subject?.let { repository.documents(it.id) }.orEmpty()
            projects = subject?.let { repository.projects(it.id) }.orEmpty()
            projectTemplates = TemplateCatalog.projects(context)
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
    BackHandler(enabled = openSection != null) { openSection = null }
    BackHandler(enabled = openProject != null) { openProject = null }
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
    BackHandler(enabled = capturing != null) { capturing = null; documentError = null }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ShellTags.ROOT),
        color = HealthTrail.colors.paper,
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
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
                                onOpenUnfiled = { trayOpen = true },
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
                        // The coaching stays until the emergency card exists,
                        // because that is what it is coaching toward. Tying it
                        // to "has anything been written" hid it the moment
                        // somebody logged their first call.
                        showCoaching = counts
                            ?.firstOrNull { it.section == Repository.Section.EMERGENCY_CARD }
                            ?.count == 0,
                        hasAnything = (counts?.sumOf { it.count } ?: 0) > 0,
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
                    )
                }
            }

            BottomNav(
                current = destination,
                onSelect = { destination = it },
                onCapture = { sheetOpen = true },
                labels = {
                    when (it) {
                        Destination.TODAY -> strings["nav.today"]
                        Destination.NOTEBOOK -> strings["nav.notebook"]
                        Destination.PROJECTS -> strings["nav.projects"]
                        Destination.MORE -> strings["nav.more"]
                    }
                },
                captureDescription = strings["capture.button.description"],
            )
        }

        val currentProject = openProject
        if (currentProject != null) {
            ProjectDetailScreen(
                project = currentProject,
                steps = projectSteps,
                onToggleStep = { togglingStep = it },
                onSetStatus = { status -> settingProjectStatus = currentProject.id to status },
                onBack = { openProject = null },
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
            )
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
        when (openSection) {
            null -> Unit

            Repository.Section.TRAIL -> TrailScreen(
                entries = trail,
                onEditDate = { editingDate = it },
                onRemove = { entry ->
                    removing = Removal(
                        section = Repository.Section.TRAIL,
                        rowId = entry.id,
                        what = entry.title?.takeIf { it.isNotBlank() }
                            ?: strings[kindLabelKey(entry.kind)],
                    )
                },
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
                onAdd = { addingInstruction = true },
                onBack = { openSection = null },
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
                onEdit = { appointment ->
                    editingAppointment = appointment
                    addingAppointment = true
                },
                onAdd = { editingAppointment = null; addingAppointment = true },
                onBack = { openSection = null },
            )

            Repository.Section.CHAPTERS -> ChaptersScreen(
                chapters = chapters,
                onBack = { openSection = null },
            )

            Repository.Section.PROGRESS -> ProgressScreen(
                measures = measures,
                readings = readings,
                onBack = { openSection = null },
            )

            Repository.Section.THREADS -> CareThreadsScreen(
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
                onBack = { openSection = null },
            )

            Repository.Section.MEDICATIONS -> MedicationsScreen(
                medications = medications,
                onRemove = { medication ->
                    removing = Removal(
                        Repository.Section.MEDICATIONS, medication.id, medication.name,
                    )
                },
                onEdit = { medication ->
                    editingMedication = medication
                    addingMedication = true
                },
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
                medications = medications.filter { it.onEmergencyCard && !it.isStopped },
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
                onEdit = { person -> editingPerson = person; addingPerson = true },
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
                threads = threads,
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
                threads = threads,
                onSave = { draft ->
                    capturing = null
                    saving = draft
                },
                onCancel = { capturing = null },
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
                    repository.createQuestionWithEntry(
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
                    // A call carries an extra row for whether anyone picked up.
                    // The other three are fully described by the entry itself.
                    if (draft.kind == CaptureKind.CALL) {
                        repository.addCallDetail(entryId = entryId)
                    }
                }
                saving = null
                revision += 1
                destination = Destination.NOTEBOOK
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
