package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.BottomNav
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.screens.AboutScreen
import com.kamsiob.healthtrail.ui.screens.ExportScreen
import com.kamsiob.healthtrail.data.ExportContainer
import com.kamsiob.healthtrail.ui.screens.RestoreScreen
import com.kamsiob.healthtrail.ui.screens.RestoreState
import com.kamsiob.healthtrail.ui.screens.ExportState
import com.kamsiob.healthtrail.ui.screens.AcknowledgeSheet
import com.kamsiob.healthtrail.ui.screens.AnswerSheet
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.MoreScreen
import com.kamsiob.healthtrail.ui.screens.AddAppointmentScreen
import com.kamsiob.healthtrail.ui.screens.AddBillScreen
import com.kamsiob.healthtrail.ui.screens.AddDocumentScreen
import com.kamsiob.healthtrail.ui.screens.DocumentsScreen
import com.kamsiob.healthtrail.ui.screens.MoneyScreen
import com.kamsiob.healthtrail.ui.screens.AddInstructionScreen
import com.kamsiob.healthtrail.ui.screens.AddMedicationScreen
import com.kamsiob.healthtrail.ui.screens.AppointmentsScreen
import com.kamsiob.healthtrail.ui.screens.CareThreadsScreen
import com.kamsiob.healthtrail.ui.screens.ChaptersScreen
import com.kamsiob.healthtrail.ui.screens.ProgressScreen
import com.kamsiob.healthtrail.ui.screens.ProjectDetailScreen
import com.kamsiob.healthtrail.ui.screens.ProjectsScreen
import com.kamsiob.healthtrail.ui.screens.StartProjectScreen
import com.kamsiob.healthtrail.ui.screens.QuestionsScreen
import com.kamsiob.healthtrail.ui.screens.StandingInstructionsScreen
import com.kamsiob.healthtrail.ui.screens.AddPersonScreen
import com.kamsiob.healthtrail.ui.screens.CareTeamScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyCardEditScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyCardScreen
import com.kamsiob.healthtrail.ui.screens.MedicationsScreen
import com.kamsiob.healthtrail.ui.screens.TrailScreen
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CaptureSheet
import com.kamsiob.healthtrail.ui.screens.DisclaimerScreen
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.MeasurementScreen
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.SetupScreen
import com.kamsiob.healthtrail.ui.screens.SituationPickerScreen
import com.kamsiob.healthtrail.ui.screens.UnfiledTrayScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every control the person can touch says what it is.
 *
 * **A screen reader user meets this app one node at a time.** A tappable thing
 * that announces nothing is, to them, a button labeled "button", and this
 * audience includes people who are older, tired, and reading in bad light,
 * which is the same audience the whole design is for.
 *
 * `DESIGN.md` section 9 requires complete labels on every control. This turns
 * that from a thing somebody checked once into a thing the build checks, on
 * every screen, forever. It is the half of the accessibility gate that can be
 * automated, and it does not replace running the reader by hand: traversal
 * order and how the labels actually sound still need ears.
 *
 * The rule it enforces: **every node carrying a click, a long click, or a
 * selection action has text or a content description**. Nothing else about it
 * matters here.
 */
@RunWith(AndroidJUnit4::class)
class ScreenReaderTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun ComposeContentTestRule.show(content: @androidx.compose.runtime.Composable () -> Unit) {
        val strings = Strings.load(context)
        setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) { content() }
            }
        }
    }

    /**
     * Every unlabeled touchable node, as a readable list.
     *
     * Walks the unmerged tree and then asks each touchable node for the text of
     * everything under it, which is how a reader actually announces a row: the
     * label may live on a child rather than on the node carrying the action.
     */
    private fun unlabeled(): List<String> {
        // **Every root, not the root.** A real bottom sheet is its own window,
        // so the capture sheet has two, and asking for "the" root threw rather
        // than checking the sheet at all. A test that cannot see a screen
        // passes it for free.
        val roots = compose.onAllNodes(isRoot(), useUnmergedTree = false)
            .fetchSemanticsNodes()
        val found = mutableListOf<String>()

        fun walk(node: SemanticsNode) {
            val config = node.config
            val touchable = config.contains(SemanticsActions.OnClick) ||
                config.contains(SemanticsActions.OnLongClick)

            if (touchable) {
                val described = config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.isNotBlank() } == true
                val labeled = config.getOrNull(SemanticsProperties.Text)
                    ?.any { it.text.isNotBlank() } == true
                val childText = node.children.any { child ->
                    child.config.getOrNull(SemanticsProperties.Text)
                        ?.any { it.text.isNotBlank() } == true
                }
                if (!described && !labeled && !childText) {
                    val tag = config.getOrNull(SemanticsProperties.TestTag) ?: "no test tag"
                    found += "touchable with no label: $tag at ${node.positionInRoot}"
                }
            }
            node.children.forEach(::walk)
        }

        roots.forEach(::walk)
        return found
    }

    private fun assertEverythingIsLabeled(screen: String) {
        val problems = unlabeled()
        assertTrue("$screen\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theDisclaimerGateLabelsEverything() {
        compose.show { DisclaimerScreen(onAccept = {}) }
        assertEverythingIsLabeled("disclaimer gate")
    }

    @Test
    fun setupLabelsEverything() {
        compose.show { SetupScreen(onContinue = {}, onSkip = {}) }
        assertEverythingIsLabeled("essentials first setup")
    }

    @Test
    fun theSituationPickerLabelsEverything() {
        val catalog = runBlocking { TemplateCatalog.situations(context) }
        compose.show { SituationPickerScreen(situations = catalog, onChoose = {}, onSkip = {}) }
        assertEverythingIsLabeled("situation picker")
    }

    @Test
    fun theNotebookLabelsEverything() {
        val rows = Repository.Section.entries
            .filter { it != Repository.Section.PROJECTS }
            .map { SectionCount(it, 0, Emphasis.STANDING) }
        compose.show {
            NotebookScreen(sections = rows, onOpen = {}, waiting = 3, onOpenUnfiled = {})
        }
        assertEverythingIsLabeled("notebook table of contents")
    }

    @Test
    fun theCaptureSheetLabelsEverything() {
        compose.show { CaptureSheet(onChoose = {}, onDismiss = {}) }
        assertEverythingIsLabeled("capture sheet")
    }

    @Test
    fun theCaptureFormLabelsEverything() {
        compose.show {
            CaptureFormScreen(
                kind = CaptureKind.CALL,
                threads = listOf(Repository.CareThread("t1", "Nursing", 0)),
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("capture form")
    }

    @Test
    fun theUnfiledTrayLabelsEverything() {
        compose.show {
            UnfiledTrayScreen(
                entries = listOf(
                    Repository.UnfiledEntry(
                        id = "e1",
                        kind = "call",
                        title = "Called the nursing station",
                        body = null,
                        occurredEdtf = "XXXX-XX-XX",
                        createdAt = 0L,
                    )
                ),
                threads = listOf(Repository.CareThread("t1", "Nursing", 0)),
                onFile = { _, _ -> },
                onClose = {},
            )
        }
        assertEverythingIsLabeled("unfiled tray")
    }

    @Test
    fun theMeasurementPickerLabelsEverything() {
        val presets = runBlocking { TemplateCatalog.presets(context) }
        compose.show {
            MeasurementScreen(
                measures = emptyList(),
                presets = presets,
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("measurement, what are you tracking")
    }

    @Test
    fun theBottomNavigationLabelsEverything() {
        // The capture button is the one control in the app with no words on it
        // at all, so it is the one most likely to announce nothing. It carries
        // a content description, and this is what keeps it carrying one.
        val strings = Strings.load(context)
        compose.show {
            BottomNav(
                current = Destination.NOTEBOOK,
                onSelect = {},
                onCapture = {},
                labels = { strings["nav.notebook"] },
                captureDescription = strings["capture.button.description"],
            )
        }
        assertEverythingIsLabeled("bottom navigation")
    }

    // -- the sections the notebook opens onto ------------------------------
    //
    // These arrived together with the table of contents finally opening. Each
    // is here the moment it is built rather than at a later accessibility pass,
    // because a screen added without a row here is a screen this test silently
    // passes by never visiting.

    @Test
    fun theTrailLabelsEverything() {
        compose.show {
            TrailScreen(
                entries = listOf(
                    Repository.TrailEntry(
                        id = "e1",
                        kind = "call",
                        title = "Nurse Patel on 4West",
                        body = "Asked about the wound check.",
                        occurredEdtf = "2026-07-22",
                        occurredStart = 1_753_000_000_000,
                        createdAt = 1_753_000_000_000,
                        isUnfiled = true,
                        threads = listOf(Repository.CareThread("t1", "Nursing", 0)),
                    ),
                    // An entry with no date, which is the row most likely to
                    // announce nothing at all.
                    Repository.TrailEntry(
                        id = "e2",
                        kind = "visit",
                        title = null,
                        body = null,
                        occurredEdtf = null,
                        occurredStart = null,
                        createdAt = 1_753_000_000_000,
                        isUnfiled = false,
                        threads = emptyList(),
                    ),
                ),
                onEditDate = {},
                onRemove = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("the trail")
    }

    @Test
    fun theCareTeamLabelsEverything() {
        compose.show {
            CareTeamScreen(
                people = listOf(
                    Repository.Person("p1", "Denise Okafor", "Case manager", "5550142", null, null),
                    // Somebody with nothing but a number, which is the row whose
                    // heading is not a name.
                    Repository.Person("p2", "", null, "5550198", null, null),
                    // Somebody with no number at all, so the row carries no
                    // action and must still read as complete.
                    Repository.Person("p3", "Dr. Prasad", "Attending", null, null, null),
                ),
                onCall = {},
                onRemove = {},
                onEdit = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("care team")
    }

    @Test
    fun addingSomebodyLabelsEverything() {
        compose.show { AddPersonScreen(onSave = {}, onCancel = {}) }
        assertEverythingIsLabeled("add someone to the care team")
    }

    @Test
    fun theEmergencyCardLabelsEverything() {
        compose.show {
            EmergencyCardScreen(
                card = Repository.EmergencyCard(
                    id = "c1",
                    allergies = "Penicillin",
                    bloodType = null,
                    conditions = null,
                    resuscitationStatus = "Do Not Resuscitate, signed 14 March 2025",
                    resuscitationDocumentLocation = "Blue folder",
                    decisionMakerDocumentLocation = null,
                    insuranceNote = null,
                    otherNotes = null,
                ),
                contacts = listOf(
                    Repository.EmergencyContact("x1", "p1", "Marisol Rivera", "5550142", "Her daughter"),
                    // On the card with no number, which leaves the row with no
                    // action on it.
                    Repository.EmergencyContact("x2", null, "The ward desk", null, null),
                ),
                medications = listOf(
                    Repository.Medication("m1", "Warfarin", "Small blue one", null, null, true, null),
                ),
                onCall = {},
                onEdit = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("emergency card")
    }

    @Test
    fun fillingInTheEmergencyCardLabelsEverything() {
        compose.show {
            EmergencyCardEditScreen(
                card = null,
                people = listOf(
                    Repository.Person("p1", "Denise Okafor", "Case manager", "5550142", null, null),
                ),
                onTheCard = setOf("p1"),
                onToggleContact = {},
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("filling in the emergency card")
    }

    @Test
    fun medicationsLabelEverything() {
        compose.show {
            MedicationsScreen(
                medications = listOf(
                    Repository.Medication("m1", "Warfarin", "Small blue one", "Blood thinner", null, true, null),
                    // A stopped medication, which reads differently and must
                    // still announce what it is.
                    Repository.Medication("m2", "Metformin", null, null, null, false, "2026-03"),
                ),
                onRemove = {},
                onEdit = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("medications")
    }

    @Test
    fun addingAMedicationLabelsEverything() {
        compose.show { AddMedicationScreen(onSave = {}, onCancel = {}) }
        assertEverythingIsLabeled("add a medication")
    }

    @Test
    fun askNextTimeLabelsEverything() {
        compose.show {
            QuestionsScreen(
                questions = listOf(
                    Repository.Question("q1", "Is the dressing changed daily?", "The wound nurse", "e1", null, null),
                    // Already asked, which renders differently and carries no
                    // action at all.
                    Repository.Question("q2", "Can the water pill move earlier?", null, null, "2026-08-01", "They will review it"),
                ),
                onMarkAsked = {},
                onRemove = {},
                onAnswer = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("ask next time")
    }

    @Test
    fun answeringAQuestionLabelsEverything() {
        compose.show {
            AnswerSheet(
                question = Repository.Question(
                    "q1", "Is the dressing changed daily?", "The wound nurse",
                    null, "2026-08-01", null,
                ),
                onSave = {},
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("answering a question")
    }

    @Test
    fun acknowledgingLabelsEverything() {
        compose.show {
            AcknowledgeSheet(
                instruction = Repository.StandingInstruction(
                    "s1", "Call me about any fall", "Please call me right away.",
                    "federal", "2026-08-02", null, null, null,
                ),
                onSave = {},
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("acknowledging an instruction")
    }

    @Test
    fun careThreadsLabelEverything() {
        compose.show {
            CareThreadsScreen(
                threads = listOf(
                    Repository.ThreadWithCount(Repository.CareThread("t1", "Nursing", 0), 4),
                    // A thread with nothing on it, which is the common case on
                    // day one and the row most likely to announce a bare zero.
                    Repository.ThreadWithCount(Repository.CareThread("t2", "Discharge planning", 1), 0),
                ),
                onBack = {},
            )
        }
        assertEverythingIsLabeled("care threads")
    }

    @Test
    fun progressLabelsEverything() {
        compose.show {
            ProgressScreen(
                measures = listOf(Repository.Measure("me1", "Weight", "weight", "lb", false)),
                readings = listOf(
                    Repository.Reading("r1", "me1", 148.0, null, "lb", "2026-08-02", 1L, null, "family"),
                    // No date and a clinician source, the two variations that
                    // add extra lines to a row.
                    Repository.Reading("r2", "me1", 151.5, null, "lb", null, null, "After dialysis", "clinician"),
                ),
                onBack = {},
            )
        }
        assertEverythingIsLabeled("progress")
    }

    @Test
    fun chaptersLabelEverything() {
        compose.show {
            ChaptersScreen(
                chapters = listOf(
                    Repository.Chapter("c1", "Maplewood General, 4West", "Admitted after a fall", null, "2026-08-01", null),
                    // No dates at all, which is what setup creates.
                    Repository.Chapter("c2", "Home", null, null, null, "2026-07-01"),
                ),
                onBack = {},
            )
        }
        assertEverythingIsLabeled("chapters")
    }

    @Test
    fun appointmentsLabelEverything() {
        compose.show {
            AppointmentsScreen(
                appointments = listOf(
                    Repository.Appointment("a1", "Care plan meeting", "2026-08-12", 2L, "4West day room", "Bring the folder"),
                    // No date, which lands in Coming up and renders its date as
                    // not known.
                    Repository.Appointment("a2", "Podiatry", null, null, null, null),
                ),
                todayMillis = 1L,
                onRemove = {},
                onEdit = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("appointments")
    }

    @Test
    fun addingAnAppointmentLabelsEverything() {
        compose.show { AddAppointmentScreen(onSave = {}, onCancel = {}) }
        assertEverythingIsLabeled("add an appointment")
    }

    @Test
    fun standingInstructionsLabelEverything() {
        val catalog = runBlocking { TemplateCatalog.instructions(context) }
        compose.show {
            StandingInstructionsScreen(
                instructions = listOf(
                    Repository.StandingInstruction("s1", "Call me about any fall", "Please call me right away.", "federal", "2026-08-02", null, null, null),
                    Repository.StandingInstruction("s2", "Tell me before a room change", "Please tell me first.", "request", null, null, null, null),
                ),
                tags = catalog.tags,
                onRemove = {},
                onAcknowledge = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("standing instructions")
    }

    @Test
    fun moneyLabelsEverything() {
        compose.show {
            MoneyScreen(
                bills = listOf(
                    Repository.Bill("b1", "Maplewood General, room and board", 128450, "USD", "waiting_on_insurance", null, "2026-08-02", null),
                    // No amount, which is the row that would otherwise render a
                    // bare gap where a number goes.
                    Repository.Bill("b2", "Ambulance transfer", null, "USD", "disputed", "They billed the wrong plan", null, null),
                    Repository.Bill("b3", "Pharmacy", 31000, "USD", "paid", null, "2026-07-30", null),
                ),
                onRemove = {},
                onEdit = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("money")
    }

    @Test
    fun addingABillLabelsEverything() {
        compose.show { AddBillScreen(onSave = {}, onCancel = {}) }
        assertEverythingIsLabeled("add a bill")
    }

    @Test
    fun documentsLabelEverything() {
        compose.show {
            DocumentsScreen(
                documents = listOf(
                    // A hash that is not on disk, so the thumbnail cannot
                    // decode. The row must still announce itself.
                    Repository.Document("d1", "Signed consent form", null, "Blue folder", null, "2026-08-02", "deadbeef", 2361),
                    // No photograph at all, which is a real document.
                    Repository.Document("d2", "Discharge summary", null, "With the ward clerk", null, null, null, null),
                ),
                onRemove = {},
                onEdit = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("documents")
    }

    @Test
    fun savingADocumentLabelsEverything() {
        compose.show { AddDocumentScreen(onSave = {}, onCancel = {}) }
        assertEverythingIsLabeled("save a document")
    }

    @Test
    fun savingADocumentLabelsEverythingWhenAFileWasRefused() {
        val strings = Strings.load(context)
        compose.show {
            AddDocumentScreen(onSave = {}, onCancel = {}, error = strings["docs.too_large"])
        }
        assertEverythingIsLabeled("save a document, file refused")
    }

    @Test
    fun todayLabelsEverything() {
        compose.show {
            TodayScreen(
                hasAnything = true,
                digest = com.kamsiob.healthtrail.data.Digest.Summary(
                    added = listOf(
                        com.kamsiob.healthtrail.data.Digest.Added(
                            Repository.Section.TRAIL, 2,
                        ),
                    ),
                    corrected = 1,
                    removed = 1,
                ),
                coaching = listOf(
                    com.kamsiob.healthtrail.ui.screens.CoachStep(
                        "today.empty.step.1", Repository.Section.EMERGENCY_CARD,
                    ) {},
                ),
                openQuestions = 2,
                waitingOnSomebody = 1,
                unfiled = 3,
                nextAppointment = Repository.Appointment(
                    "a1", "Care plan meeting", "2026-08-12", 2L, null, null,
                ),
            )
        }
        assertEverythingIsLabeled("today")
    }

    @Test
    fun exportLabelsEverything() {
        compose.show {
            ExportScreen(state = ExportState.READY, onExport = {}, onBack = {})
        }
        assertEverythingIsLabeled("export")
    }

    @Test
    fun exportLabelsEverythingWhileWorking() {
        // Every control is disabled in this state, which is exactly when a
        // reader is most likely to meet something that announces nothing.
        compose.show {
            ExportScreen(state = ExportState.WORKING, onExport = {}, onBack = {})
        }
        assertEverythingIsLabeled("export, working")
    }

    /**
     * Each restore state renders different controls, so each gets its own case.
     * The compose rule takes content once per test, which is why this is four
     * tests rather than a loop over the states.
     */
    @Test
    fun restoreLabelsEverythingBeforeAFileIsChosen() {
        compose.show { RestoreAt(RestoreState.Empty) }
        assertEverythingIsLabeled("restore, nothing chosen")
    }

    @Test
    fun restoreLabelsEverythingWhenLocked() {
        compose.show { RestoreAt(RestoreState.NeedsPassphrase("That did not open it.")) }
        assertEverythingIsLabeled("restore, locked after a failed attempt")
    }

    @Test
    fun restoreLabelsEverythingWhenReady() {
        compose.show {
            RestoreAt(
                RestoreState.Ready(
                    ExportContainer.Manifest(
                        formatVersion = 1,
                        appVersion = "0.1.0",
                        platform = "android",
                        exportedAt = 1_785_657_781_157,
                        originDevice = "device",
                        encrypted = true,
                        databaseSha256 = "abc",
                        databaseBytes = 1,
                        rowCounts = mapOf("entry" to 4),
                        attachmentCount = 0,
                        attachmentBytes = 0,
                        subjectCount = 1,
                    ),
                ),
            )
        }
        assertEverythingIsLabeled("restore, ready to replace")
    }

    @Test
    fun restoreLabelsEverythingOnAProblem() {
        compose.show { RestoreAt(RestoreState.Problem("This file could not be opened.")) }
        assertEverythingIsLabeled("restore, problem")
    }

    @androidx.compose.runtime.Composable
    private fun RestoreAt(state: RestoreState) {
        RestoreScreen(
            state = state,
            onChoose = {},
            onUnlock = {},
            onRestore = {},
            onBack = {},
        )
    }

    @Test
    fun aboutLabelsEverything() {
        compose.show { AboutScreen(onBack = {}) }
        assertEverythingIsLabeled("about")
    }

    @Test
    fun moreLabelsEverything() {
        compose.show {
            MoreScreen(
                choice = com.kamsiob.healthtrail.ui.theme.ThemeChoice.DEFAULT,
                onChoose = {},
                onAbout = {},
                onExport = {},
                onRestore = {},
            )
        }
        assertEverythingIsLabeled("more")
    }

    @Test
    fun projectsLabelEverything() {
        compose.show {
            ProjectsScreen(
                projects = listOf(
                    Repository.Project("pr1", "Medicaid application", "medicaid_ltc", "waiting", "The caseworker", null, 14, 3),
                    // No steps and no waiting on, which strips two lines off the
                    // card and leaves the least to announce.
                    Repository.Project("pr2", "Records request", null, "done", null, null, 0, 0),
                ),
                onOpen = {},
                onRemove = {},
                onStart = {},
            )
        }
        assertEverythingIsLabeled("projects")
    }

    @Test
    fun projectsLabelEverythingWhenEmpty() {
        compose.show {
            ProjectsScreen(projects = emptyList(), onOpen = {}, onRemove = {}, onStart = {})
        }
        assertEverythingIsLabeled("projects, nothing started")
    }

    @Test
    fun startingAProjectLabelsEverything() {
        val templates = runBlocking { TemplateCatalog.projects(context) }
        compose.show {
            StartProjectScreen(templates = templates, onChoose = {}, onCancel = {})
        }
        assertEverythingIsLabeled("start a project")
    }

    @Test
    fun aProjectLabelsEverything() {
        compose.show {
            ProjectDetailScreen(
                project = Repository.Project("pr1", "Medicaid application", "medicaid_ltc", "active", null, null, 3, 1),
                steps = listOf(
                    Repository.ProjectStep("s1", "Get the right form", "2026-08-01", null),
                    Repository.ProjectStep("s2", "Gather proof of income", null, null),
                ),
                onToggleStep = {},
                onSetStatus = {},
                onSetWaitingOn = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project")
    }

    @Test
    fun askingForSomethingLabelsEverything() {
        val catalog = runBlocking { TemplateCatalog.instructions(context) }
        compose.show {
            AddInstructionScreen(catalog = catalog, onChoose = {}, onCancel = {})
        }
        assertEverythingIsLabeled("ask for something")
    }
}
