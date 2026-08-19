package com.kamsiob.healthtrail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
// `getValue` and `setValue` look unused and are not: they are the operators
// `by` resolves to, so a sweep for unreferenced imports takes them and the
// file stops compiling. Kept named rather than left to be rediscovered.
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.components.DatePickerSheet
import com.kamsiob.healthtrail.ui.v4.Share
import com.kamsiob.healthtrail.data.Attachments
import java.time.LocalDate
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.ui.v4.Destination
import com.kamsiob.healthtrail.ui.screens.headingFor
import com.kamsiob.healthtrail.ui.screens.IncidentScreen
import com.kamsiob.healthtrail.ui.screens.IncidentsScreen
import com.kamsiob.healthtrail.ui.screens.EntryScreen
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.MilestonesScreen
import com.kamsiob.healthtrail.ui.screens.ChangeSituationScreen
import com.kamsiob.healthtrail.ui.screens.MonthReviewScreen
import com.kamsiob.healthtrail.ui.screens.SituationChange
import com.kamsiob.healthtrail.ui.screens.SituationPickerScreen
import com.kamsiob.healthtrail.ui.screens.PrepScreen
import com.kamsiob.healthtrail.ui.screens.BillScreen
import com.kamsiob.healthtrail.ui.screens.AddThreadScreen
import com.kamsiob.healthtrail.ui.screens.ChapterScreen
import com.kamsiob.healthtrail.ui.screens.CorrectReadingScreen
import com.kamsiob.healthtrail.ui.screens.MedicationEventScreen
import com.kamsiob.healthtrail.ui.screens.MedicationScreen
import com.kamsiob.healthtrail.ui.screens.ViolationScreen
import com.kamsiob.healthtrail.ui.screens.ThreadScreen
import com.kamsiob.healthtrail.ui.screens.CaptureFormState
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CareTeamScreen
import com.kamsiob.healthtrail.ui.screens.MedicationsScreen
import com.kamsiob.healthtrail.ui.screens.QuestionsScreen
import com.kamsiob.healthtrail.ui.screens.CareThreadsScreen
import com.kamsiob.healthtrail.ui.screens.MeasureScreen
import com.kamsiob.healthtrail.ui.screens.NoteScreen
import com.kamsiob.healthtrail.ui.screens.NotesScreen
import com.kamsiob.healthtrail.ui.screens.ProgressScreen
import com.kamsiob.healthtrail.ui.screens.ProjectHomeScreen
import com.kamsiob.healthtrail.ui.screens.ProjectSetupScreen
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
import com.kamsiob.healthtrail.ui.screens.ChoosePaperScreen
import com.kamsiob.healthtrail.ui.screens.AppointmentsScreen
import com.kamsiob.healthtrail.ui.screens.StandingInstructionsScreen
import com.kamsiob.healthtrail.ui.screens.MoneyScreen
import com.kamsiob.healthtrail.ui.screens.DocumentScreen
import com.kamsiob.healthtrail.ui.screens.DocumentsScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyCardScreen
import com.kamsiob.healthtrail.ui.screens.TrailScreen
import com.kamsiob.healthtrail.ui.screens.labelKey
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.screens.LocalRefresh
import com.kamsiob.healthtrail.ui.screens.LocalSectionBackKey
import com.kamsiob.healthtrail.ui.screens.TemplateLibraryScreen

/**
 * The milestone arc, and the month review that reads it.
 *
 * **Lifted out of `NotebookShell` for B6.** It takes `ui` and the
 * repository and almost nothing else, which is the one shape that has ever
 * bought room: the bytecode that counts is at the call site, so a group
 * that costs three arguments pays and one that costs eighteen does not.
 * `with(ui)` keeps every reference inside reading as it did in the shell.
 */
@Composable
internal fun MilestoneOverlays(
    ui: ShellState,
    repository: Repository,
    captureDraftState: MutableState<CaptureFormState>,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var captureDraft by captureDraftState
    with(ui) {
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
                memosAbout = repository.notesAbout("appointment", appointmentId)
                if (prep == null) openPrepFor = null
            }
            prep?.takeIf { it.appointment.id == appointmentId }?.let { sheet ->
                PrepScreen(
                    memos = memosAbout,
                    onOpenMemo = { openEntry = it.id },
                    onWriteMemo = {
                        writingNote = NoteTarget(
                            "appointment", sheet.appointment.id, sheet.appointment.title,
                        )
                    },
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
                    // **The form opens knowing who this appointment is with**,
                    // #46, so a question written in the room lands on this
                    // sheet rather than on every sheet. A prefill the person
                    // can change, never a decision made for them.
                    // **And it carries the name in without destroying a half
                    // written note**, which is what replacing the whole state
                    // would have done. #371 item 7.
                    onAsk = {
                        captureDraft = captureDraft.withPerson(
                            people.firstOrNull { it.id == sheet.appointment.personId },
                        )
                        capturing = CaptureKind.QUESTION
                    },
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
                memosAbout = repository.notesAbout("person", person.id)
            }
            PersonScreen(
                person = person,
                entries = personEntries,
                memos = memosAbout,
                onOpenMemo = { openEntry = it.id },
                onWriteMemo = {
                    writingNote = NoteTarget("person", person.id, person.displayName)
                },
                // **The other end of "who it is with", rule 18**, and it costs
                // no query: every appointment is loaded for its own section.
                appointments = appointments.filter { it.personId == person.id },
                onOpenAppointment = { openPerson = null; openPrepFor = it.id },
                onCall = { number -> dial(context, number) },
                // Writing to them, the same way the care team offers it. #386.
                onEmail = { address -> email(context, address) },
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
                memosAbout = repository.notesAbout("document", current.id)
            }
            DocumentScreen(
                document = fresh,
                memos = memosAbout,
                onOpenMemo = { openEntry = it.id },
                onWriteMemo = {
                    writingNote = NoteTarget("document", fresh.id, fresh.title)
                },
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
                // **The paper opens at reading size**, #378, carrying the
                // document's own name for the reader.
                onOpenPaper = { sha -> viewingPaper = sha to fresh.title },
                // **Saving is the system sheet**, which is how a phone puts a
                // file in Downloads, in the gallery, or into a message. #379.
                onSavePaper = { sha ->
                    val store = Attachments.open(context)
                    val intent = Share.paperIntent(
                        context = context,
                        sourceFile = store.fileFor(sha),
                        fileName = Share.paperFileName(fresh.title),
                        // The document's own name, which is what somebody
                        // called their own paper.
                        subject = fresh.title,
                        chooserTitle = strings["document.save.chooser"],
                    )
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        documentError = strings["document.save.failed"]
                    }
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
            LaunchedEffect(fresh.id, revision) {
                billViolations = repository.violationsLinkedTo(fresh.id)
                memosAbout = repository.notesAbout("bill", fresh.id)
            }
            BillScreen(
                bill = fresh,
                memos = memosAbout,
                onOpenMemo = { openEntry = it.id },
                onWriteMemo = {
                    writingNote = NoteTarget("bill", fresh.id, fresh.description)
                },
                violations = billViolations,
                onOpenViolations = {
                    openBill = null
                    openSection = Repository.Section.STANDING_INSTRUCTIONS
                },
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
                // **The other end of the link**, rule 18, #397.
                entryAbout = repository.aboutFor(entryId)
                // An entry that is gone closes rather than showing a blank
                // screen, which is what a removal from underneath looks like.
                if (entryDetail == null) openEntry = null
            }
            entryDetail?.takeIf { it.entry.id == entryId }?.let { detail ->
                EntryScreen(
                    detail = detail,
                    about = entryAbout,
                    // **Opening what it is about**, which is what makes the row
                    // a door rather than a label. Each table lands on the
                    // screen that holds that thing.
                    onOpenAbout = { target ->
                        openEntry = null
                        when (target.table) {
                            "person" -> openPerson = people.firstOrNull { it.id == target.id }
                            "project" -> openProject = projects.firstOrNull { it.id == target.id }
                            "document" -> openDocument = documents
                                .firstOrNull { it.id == target.id }
                            "bill" -> openBill = bills.firstOrNull { it.id == target.id }
                            "appointment" -> openPrepFor = target.id
                            "incident" -> openIncident = incidents
                                .firstOrNull { it.id == target.id }
                            // **A table this screen cannot open lands nowhere
                            // rather than somewhere wrong.** The row is only
                            // drawn for a link that exists, and every table the
                            // app can attach from is above; anything else is a
                            // link written by a newer version, which 8.3 says
                            // to carry rather than to guess at.
                            else -> Unit
                        }
                    },
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
    }
}

/**
 * Incidents, an incident's own screen, and the month review.
 *
 * **Lifted out of `NotebookShell` for B6.** It takes `ui` and the
 * repository and almost nothing else, which is the one shape that has ever
 * bought room: the bytecode that counts is at the call site, so a group
 * that costs three arguments pays and one that costs eighteen does not.
 * `with(ui)` keeps every reference inside reading as it did in the shell.
 */
@Composable
internal fun IncidentAndReviewOverlays(
    ui: ShellState,
    repository: Repository,
) {
    val strings = LocalStrings.current
    with(ui) {
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
                    incidentDetail = repository.incidentDetail(current.id)
                    memosAbout = repository.notesAbout("incident", current.id)
                }
                IncidentScreen(
                    incident = current,
                    detail = incidentDetail,
                    memos = memosAbout,
                    onOpenMemo = { openEntry = it.id },
                    onWriteMemo = {
                        writingNote = NoteTarget("incident", current.id, current.title)
                    },
                    // **The way back from a request that was not followed**,
                    // which is the list of what was asked for, since a request
                    // has no screen of its own while B6 stands.
                    onOpenViolations = {
                        openIncident = null
                        incidentsOpen = false
                        openSection = Repository.Section.STANDING_INSTRUCTIONS
                    },
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
                if (draft.violationId == null) {
                    repository.recordViolation(
                        instructionId = draft.instructionId,
                        occurred = draft.occurred,
                        note = draft.note,
                        // **The two arguments nothing had ever passed.** #371,
                        // and both readers on the instruction's own row were
                        // dead until this line: the form now asks, from behind
                        // the disclosure, and never requires it.
                        incidentId = draft.incidentId,
                        billId = draft.billId,
                    )
                } else {
                    repository.updateViolation(
                        violationId = draft.violationId,
                        occurred = draft.occurred,
                        note = draft.note,
                        incidentId = draft.incidentId,
                        billId = draft.billId,
                    )
                }
                savingViolation = null
                revision += 1
            }
        }

        // **One form, whether the time is being written down or corrected**,
        // the same way the add screens take an `existing`. A second screen
        // would be a second answer to "what happened, when, and against what".
        (recordingViolationFor?.let { it to null } ?: correctingViolation)
            ?.let { (instruction, existing) ->
                ViolationScreen(
                    instruction = instruction,
                    existing = existing,
                    onSave = { draft ->
                        savingViolation = draft
                        recordingViolationFor = null
                        correctingViolation = null
                    },
                    onCancel = { recordingViolationFor = null; correctingViolation = null },
                    // What it could have broken against, offered and never
                    // required. Both lists are already loaded for their own
                    // sections, so this costs no query.
                    incidents = incidents,
                    bills = bills,
                    // **The screen closes as the confirmation opens**, the same
                    // as an incident's, so the sheet is not asking about a
                    // thing the screen behind it still shows.
                    onRemove = existing?.let { time ->
                        {
                            removing = Removal(
                                section = null,
                                rowId = time.id,
                                what = time.note?.takeIf { it.isNotBlank() }
                                    ?: EventDateText.render(strings, time.occurredEdtf),
                                remove = { repository.removeViolation(it) },
                            )
                            correctingViolation = null
                        }
                    },
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
                    onRename = { renamingChapter = detail.chapter },
                    // #432, rule 17: the start first, then the end, through the
                    // same picker every other date in the app opens.
                    onCorrectDates = { correctingChapterStart = detail.chapter },
                    // **The screen closes as the confirmation opens**, the
                    // same as a medication's. Anything added can be removed,
                    // 2026-08-16; what was filed here stays on the trail.
                    onRemove = {
                        openChapter = null
                        removing = Removal(
                            Repository.Section.CHAPTERS,
                            detail.chapter.id,
                            detail.chapter.name,
                        )
                    },
                )
            }
        }

        // **Correcting a project's name**, #374. Same screen, same one
        // question, wearing gold because Projects belongs to no section: 4.3,
        // and `Section.PROJECTS` is what `hueFor` maps to the whole app hue.
        renamingProject?.let { project ->
            AddThreadScreen(
                onStart = { name ->
                    savingProjectRename = project.id to name
                    renamingProject = null
                },
                onCancel = { renamingProject = null },
                titleKey = "projects.rename.title",
                labelKey = "projects.rename.name",
                hintKey = null,
                saveKey = "projects.rename.save",
                leadKey = "projects.rename.lead",
                section = Repository.Section.PROJECTS,
                initialName = project.name,
            )
        }

        savingProjectRename?.let { rename ->
            LaunchedEffect(rename) {
                // bidi-ok: on its way to the database.
                repository.renameProject(rename.first, rename.second)
                savingProjectRename = null
                // **Reread rather than patched**, so the heading on the
                // project's own screen is the row the database now has.
                openProject = null
                revision += 1
            }
        }

        // **Correcting a chapter's name**, #374, and the surface #373 made room
        // for. The same screen the care thread rename uses, asking the same one
        // question in the chapter's own words and wearing the chapters chip.
        // **A chapter's dates, corrected through the ordinary picker.** #432.
        // The start is asked first and picking it opens the end, so the two
        // reads as one correction rather than two errands.
        correctingChapterStart?.let { chapter ->
            DatePickerSheet(
                initial = chapter.startedEdtf?.takeIf { it.isNotBlank() }?.let { Edtf.parse(it) },
                titleKey = "chapters.dates.started",
                onPick = { picked ->
                    savingChapterDates = Triple(chapter.id, picked, chapter.endedEdtf)
                    correctingChapterStart = null
                    correctingChapterEnd = chapter
                },
                onDismiss = { correctingChapterStart = null },
            )
        }

        correctingChapterEnd?.let { chapter ->
            DatePickerSheet(
                initial = chapter.endedEdtf?.takeIf { it.isNotBlank() }?.let { Edtf.parse(it) },
                titleKey = "chapters.dates.ended",
                onPick = { picked ->
                    savingChapterEnd = Triple(chapter.id, chapter.startedEdtf, picked)
                    correctingChapterEnd = null
                },
                // **Dismissing leaves the end alone rather than clearing it.**
                // A person who corrected the start and backed out of the end has
                // not said the chapter is still running.
                onDismiss = { correctingChapterEnd = null },
            )
        }

        renamingChapter?.let { chapter ->
            AddThreadScreen(
                onStart = { name ->
                    savingChapterRename = chapter.id to name
                    renamingChapter = null
                },
                onCancel = { renamingChapter = null },
                titleKey = "chapters.rename.title",
                labelKey = "chapters.rename.name",
                hintKey = null,
                saveKey = "chapters.rename.save",
                leadKey = "chapters.rename.lead",
                section = Repository.Section.CHAPTERS,
                initialName = chapter.name,
            )
        }

        // **Saying they are somewhere new**, #377, the same one question form
        // every other name in the app is written in. It asks for the place and
        // nothing else: the date is today by construction, and asking somebody
        // to confirm the day their mother moved, on the day it happened, is the
        // app making work for itself. Rule 17 leaves it editable from the
        // chapter afterward.
        if (sayingMoved) {
            AddThreadScreen(
                onStart = { name ->
                    savingMove = name
                    sayingMoved = false
                },
                onCancel = { sayingMoved = false },
                titleKey = "chapters.moved.title",
                labelKey = "chapters.moved.name",
                hintKey = "chapters.moved.hint",
                saveKey = "chapters.moved.save",
                leadKey = "chapters.moved.lead",
                section = Repository.Section.CHAPTERS,
            )
        }

        savingMove?.let { name ->
            LaunchedEffect(name) {
                val subjectId = repository.activeSubject()?.id
                // **The move, not just a new name**, which is the whole reason
                // this writes through `moveToChapter`: it closes every open
                // place today and starts this one today, so two current places
                // never exist at once. The same seam the setting change flow
                // hit and the comment there records.
                // bidi-ok: on its way to the database.
                if (subjectId != null) repository.moveToChapter(subjectId, name)
                savingMove = null
                revision += 1
            }
        }

        savingChapterRename?.let { rename ->
            LaunchedEffect(rename) {
                // bidi-ok: on its way to the database.
                repository.renameChapter(rename.first, rename.second)
                savingChapterRename = null
                // **The open chapter is reread rather than patched**, so the
                // title above the list is the row the database now has.
                chapterDetail = null
                revision += 1
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
                // #433. Ending is dated today by default, the way every other
                // "this happened now" in the app is, and stays editable from
                // the thread afterward per rule 17.
                onEnd = { endingThread = thread },
                onReopen = { reopeningThread = thread },
                // Anything added can be removed, 2026-08-16. The entries the
                // thread gathered stay on the trail; the thread itself goes.
                onRemove = {
                    openThread = null
                    removing = Removal(
                        Repository.Section.THREADS,
                        thread.id,
                        thread.label,
                    )
                },
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
    }
}

/**
 * A project's steps, its standing, and what it is waiting on.
 *
 * **Lifted out of `NotebookShell` for B6.** It takes `ui` and the
 * repository and almost nothing else, which is the one shape that has ever
 * bought room: the bytecode that counts is at the call site, so a group
 * that costs three arguments pays and one that costs eighteen does not.
 * `with(ui)` keeps every reference inside reading as it did in the shell.
 */
@Composable
internal fun ProjectStepOverlays(
    ui: ShellState,
    repository: Repository,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    with(ui) {
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
            // **Pulling a section list down re-reads the record**, D167. The
            // revision counter is what every query in the shell already keys
            // on, so bumping it is exactly what a refresh means here.
            LocalRefresh provides { revision += 1 },
        ) {
            when (openSection) {
                null -> Unit

                // **Notes, which are entries of one kind.** #397, D207: this
                // is a lens on the trail rather than a second record, so the
                // rows come from the same table and nothing here is stored
                // twice.
                Repository.Section.NOTES -> NotesScreen(
                    notes = trail.filter { it.kind == "note" },
                    onOpen = { openEntry = it.id },
                    onAdd = { writingNote = NoteTarget(null, null, null) },
                    onPin = { note, keep -> pinningEntry = note.id to keep },
                    // **The confirmation removes it, never this screen**, which
                    // is the shape every other removal in the app has.
                    onRemove = {
                        removing = Removal(
                            Repository.Section.NOTES,
                            it.id,
                            it.title.orEmpty().ifBlank { it.body.orEmpty() },
                        )
                    },
                    onBack = { openSection = null },
                )

                Repository.Section.TRAIL -> TrailScreen(
                    entries = trail,
                    // Set by the digest card and cleared on the way out, so
                    // the trail is the whole trail unless somebody asked for
                    // the new things. D169.
                    since = trailSince,
                    onSeeAll = { trailSince = null },
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
                    // **An empty trail had no way to add the first entry.**
                    // #388 finding 1. The trail is pushed over the shell, so it
                    // has no capture button of its own; this opens the shell's.
                    onAdd = { sheetOpen = true },
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
                    onOpenViolation = { instruction, violation ->
                        correctingViolation = instruction to violation
                    },
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
                    onBack = { openSection = null; trailSince = null },
                )

                Repository.Section.CHAPTERS -> ChaptersScreen(
                    // What each chapter holds, so the current place says it on
                    // its own card rather than only inside. #356.
                    contents = chapterContents,
                    onOpen = { openChapter = it },
                    chapters = chapters,
                    onOpenMilestones = { milestonesOpen = true },
                    onBack = { openSection = null },
                    // #377. The section stays open underneath, so saving lands
                    // back on the list with the new place at the top of it.
                    onMoved = { sayingMoved = true },
                )

                Repository.Section.PROGRESS -> {
                    // **One tracked thing's own screen sits above the list**,
                    // #398, so back closes the measure and lands on Progress
                    // rather than leaving the section entirely.
                    val opened = openMeasure?.let { current ->
                        measures.firstOrNull { it.id == current.id }
                    }
                    if (opened != null) {
                        MeasureScreen(
                            measure = opened,
                            // **This measure's readings and no other's.** The
                            // flat mixed list is what #398 exists to remove.
                            readings = readings.filter { it.measureId == opened.id },
                            onAddReading = { capturing = CaptureKind.MEASUREMENT },
                            onCorrectReading = { correctingReading = it },
                            onCorrectMeasure = { correctingMeasure = opened },
                            onBack = { openMeasure = null },
                        )
                    } else {
                        ProgressScreen(
                            measures = measures,
                            readings = readings,
                            // The measurement form, which is a real destination
                            // rather than a stub: it is the same screen capture
                            // opens.
                            onAddReading = { capturing = CaptureKind.MEASUREMENT },
                            // **Correcting a reading lives where the readings
                            // do**, which is the measure's own screen since
                            // #398. This one holds no readings any more.
                            onCorrectMeasure = { correctingMeasure = it },
                            onOpenMeasure = { openMeasure = it },
                            onBack = { openSection = null },
                        )
                    }
                }


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
                    // Where they are now, so that facility's staff lead and
                    // everyone else reads as outside it. #379.
                    currentPlace = chapters.firstOrNull { it.isCurrent }?.name,
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
                    // **Writing to them, where they gave an address.** `m3v4-3`
                    // draws it beside calling on the person the screen leads
                    // with, and the column has been there since the form asked.
                    onEmail = { person -> email(context, person.email) },
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
    }
}

/**
 * The template library, and changing what the notebook is about.
 *
 * **Lifted out of `NotebookShell` for B6.** It takes `ui` and the
 * repository and almost nothing else, which is the one shape that has ever
 * bought room: the bytecode that counts is at the call site, so a group
 * that costs three arguments pays and one that costs eighteen does not.
 * `with(ui)` keeps every reference inside reading as it did in the shell.
 */
@Composable
internal fun SituationOverlays(
    ui: ShellState,
    repository: Repository,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    with(ui) {
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
    }
}

/**
 * Everything a project opens: its own screen, its setup, and the six surfaces under it.
 *
 * **Lifted out of `NotebookShell` for B6.** It takes `ui` and the
 * repository and almost nothing else, which is the one shape that has ever
 * bought room: the bytecode that counts is at the call site, so a group
 * that costs three arguments pays and one that costs eighteen does not.
 * `with(ui)` keeps every reference inside reading as it did in the shell.
 */
@Composable
internal fun ProjectOverlays(
    ui: ShellState,
    repository: Repository,
    setupOpenState: MutableState<Boolean>,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var setupOpen by setupOpenState
    with(ui) {
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
                onFillPaper = { fillingPaper = it },
            )

            // **Choosing which paper goes in an empty place.** #379: the
            // template suggests places and nothing could ever be put in one.
            // The list is the documents already kept, because filing an
            // existing paper is the common case; photographing a new one is
            // the other door and it lands here the same way.
            fillingPaper?.let { paper ->
                ChoosePaperScreen(
                    placeName = paper.name,
                    documents = documents,
                    attachments = Attachments.open(context),
                    onChoose = { documentId ->
                        savingPaperFill = paper.id to documentId
                        fillingPaper = null
                    },
                    onPhotograph = {
                        fillingPaper = null
                        capturing = CaptureKind.DOCUMENT
                    },
                    onBack = { fillingPaper = null },
                )
            }

            savingPaperFill?.let { (paperId, documentId) ->
                LaunchedEffect(paperId, documentId) {
                    repository.fillProjectPaper(
                        paperId = paperId,
                        documentId = documentId,
                        direction = null,
                    )
                    savingPaperFill = null
                    revision += 1
                }
            }
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
        } else if (currentProject != null && stepsOpen) {
            // **Reachable from the home's file row directly**, D164. The old
            // gate required Setup to be open underneath, which was the "sub
            // menus" half of the owner's complaint: a list of steps two doors
            // deep behind a settings screen.
            ProjectStepsScreen(
                projectName = currentProject.name,
                steps = projectSteps,
                onAdd = { addingStep = currentProject.id to it },
                onOpen = { stepUnderEdit = it },
                // **Whichever door was used.** Setup can still open this, so
                // the label follows the path rather than asserting one of them.
                backKey = if (setupOpen) "section.back.setup" else "section.back.project",
                onBack = { stepsOpen = false },
            )

            stepUnderEdit?.let { opened ->
                val index = projectSteps.indexOfFirst { it.id == opened.id }
                // **The sheet reads the list, not the row it was opened from.**
                // `stepUnderEdit` is the step as it was at the moment of the
                // tap, so a switch inside the sheet wrote the change, reloaded
                // the list behind it and left the control showing the old
                // state: the one thing rule 16 says a control must never do.
                // Seen on the phone.
                val step = projectSteps.getOrNull(index) ?: opened
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
                    // **The sheet stays open.** Marking a step taken is not
                    // leaving the step, and closing under somebody's finger
                    // would make the switch read as a way out rather than a
                    // state. The list under it is what shows the change.
                    onSetDone = { togglingStep = step },
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
                memos = projectMemos,
                onOpenMemo = { openEntry = it.id },
                onWriteMemo = {
                    writingNote = NoteTarget("project", currentProject.id, currentProject.name)
                },
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
                // D164: the steps open as their own screen from the file row,
                // no longer only from inside Setup.
                onOpenSteps = { stepsOpen = true },
                onOpenPaperwork = { paperworkOpen = true },
                onOpenPeople = { peopleOpen = true },
                peopleCount = projectPeople.size,
                // **Rule 18 both ways.** The entry already knows the project;
                // this is the project opening the entry.
                onOpenEntryById = { openEntry = it },
                onRename = { renamingProject = currentProject },
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
    }
}
