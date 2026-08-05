package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.BottomNav
import com.kamsiob.healthtrail.ui.components.CaptureFab
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.screens.AboutScreen
import com.kamsiob.healthtrail.ui.screens.ChangeSituationScreen
import com.kamsiob.healthtrail.ui.screens.MilestonesScreen
import com.kamsiob.healthtrail.ui.screens.TemplateLibraryScreen
import com.kamsiob.healthtrail.ui.screens.MonthReviewScreen
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
import com.kamsiob.healthtrail.ui.screens.ProjectDetailTags
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kamsiob.healthtrail.ui.screens.CaptureFormState
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CaptureSheet
import com.kamsiob.healthtrail.ui.screens.DisclaimerScreen
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.MeasurementScreen
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.EntryScreen
import com.kamsiob.healthtrail.ui.screens.IncidentsScreen
import com.kamsiob.healthtrail.ui.screens.PrepScreen
import com.kamsiob.healthtrail.ui.screens.PrepTags
import com.kamsiob.healthtrail.ui.screens.SearchScreen
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
 *
 * ## The shortcut this test takes, named per `TESTING-PERSONAS.md` section 7
 *
 * **Every screen here is composed directly, with arguments handed to it, rather
 * than reached the way a person reaches it.** That is deliberate and it is the
 * only way to cover thirty-odd screens in seconds, including states that are
 * awkward to reach on demand, like an error or an empty section in a notebook
 * that has data.
 *
 * **What it therefore cannot see**, so that nobody reads a passing run as more
 * than it is:
 *
 * - **Traversal order.** Nodes are checked as a set, not as a sequence, so a
 *   reader meeting them in the wrong order passes here. That is the hand pass,
 *   issue #44, and it is why #44 is not closed by this file.
 * - **How a label actually sounds.** "Care team, nothing yet" and "Care team"
 *   followed by "nothing yet" are one stop and two stops to a reader, and both
 *   satisfy the rule this test enforces.
 * - **Anything above an individual screen.** The shell, navigation, and back
 *   are not composed here at all. `BackJourneyTest` covers that, and D65 is why
 *   it exists.
 * - **Whether the screen is reachable.** A screen only ever composed by this
 *   test is a screen no test has actually visited.
 *
 * **What covers the difference:** the hand pass with TalkBack running, #44, and
 * the journey tests. Neither is optional and neither is replaced by this file.
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
            var state by remember { mutableStateOf(CaptureFormState()) }
            CaptureFormScreen(
                kind = CaptureKind.CALL,
                threads = listOf(Repository.CareThread("t1", "Nursing", 0)),
                state = state,
                onStateChange = { state = it },
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
        val strings = Strings.load(context)
        compose.show {
            BottomNav(
                current = Destination.NOTEBOOK,
                onSelect = {},
                labels = { strings["nav.notebook"] },
            )
        }
        assertEverythingIsLabeled("bottom navigation")
    }

    @Test
    fun theCaptureButtonLabelsItself() {
        // **The one control in the app with no words on it at all**, so it is
        // the one most likely to announce nothing. It left the navigation bar
        // in the v4 pass and became the floating button, which is also how this
        // test came to be calling a signature that no longer existed: the
        // androidTest sources are not built by the unit test task, so it went
        // unnoticed until the trail brought a row here.
        val strings = Strings.load(context)
        compose.show {
            CaptureFab(
                open = false,
                onClick = {},
                description = strings["capture.button.description"],
            )
        }
        assertEverythingIsLabeled("the capture button")
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
                onOpen = {},
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
                        pinnedAt = null,
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
                        pinnedAt = null,
                    ),
                    // A pinned entry, which is the row that carries a second
                    // control and a mark that must not be announced twice.
                    Repository.TrailEntry(
                        id = "e3",
                        kind = "incident",
                        title = "Call light not answered",
                        body = null,
                        occurredEdtf = "2026-07-06",
                        occurredStart = 1_751_000_000_000,
                        createdAt = 1_751_000_000_000,
                        isUnfiled = false,
                        threads = emptyList(),
                        pinnedAt = 1_751_500_000_000,
                    ),
                ),
                onReview = {},
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
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("care team")
    }

    @Test
    fun addingSomebodyLabelsEverything() {
        compose.show {
            AddPersonScreen(
                // With the situation's roles offered, which is the state a
                // person in a nursing home notebook actually meets. Composed
                // without them, this screen would pass while the chips a real
                // notebook shows went unchecked.
                roleSuggestions = listOf(
                    "Director of nursing",
                    "Charge nurse on the unit",
                    "Social worker or family liaison",
                ),
                onSave = {},
                onCancel = {},
            )
        }
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
                onShare = {},
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
                onOpen = {},
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
                onOpen = {},
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
                onAddReading = {},
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
                onOpen = {},
                onOpenMilestones = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("chapters")
    }

    /**
     * The arc, including the chapter door that only appears once somebody has
     * named a place. The fixture cannot produce one, #235, so the case is built
     * here by hand rather than left unwalked.
     */
    @Test
    fun theMilestoneArcLabelsEverything() {
        compose.show {
            MilestonesScreen(
                milestones = listOf(
                    Repository.Milestone(
                        id = "m1",
                        label = "First day without oxygen",
                        occurredEdtf = "2026-06-19",
                        occurredStart = 1_750_000_000_000,
                        chapterId = null,
                        chapterName = null,
                        note = null,
                    ),
                    Repository.Milestone(
                        id = "m2",
                        label = "Sat up for the whole visit",
                        occurredEdtf = "2026-08-12",
                        occurredStart = 1_755_000_000_000,
                        chapterId = "c1",
                        chapterName = "Maplewood Care Center",
                        note = "She knew who I was.",
                    ),
                ),
                onOpen = {},
                onOpenChapter = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("the milestone arc")
    }

    /**
     * How the notebook is set up, in both states: before a different setting is
     * picked, and after, when the chapter field and the filled action appear.
     */
    @Test
    fun changingHowTheNotebookIsSetUpLabelsEverything() {
        val situations = runBlocking { TemplateCatalog.situations(context) }
        compose.show {
            ChangeSituationScreen(
                current = situations.all.first(),
                chosen = situations.all.last(),
                onOpenPicker = {},
                onApply = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("changing how the notebook is set up")
    }

    /**
     * The same screen before anybody has opened the picker, which is the state
     * most people meet: they came to check which setting they picked.
     */
    @Test
    fun howTheNotebookIsSetUpLabelsEverythingBeforeAnyChange() {
        val situations = runBlocking { TemplateCatalog.situations(context) }
        compose.show {
            ChangeSituationScreen(
                current = situations.all.first(),
                chosen = null,
                onOpenPicker = {},
                onApply = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("how the notebook is set up")
    }

    /**
     * The picker with the person's own template at the top.
     *
     * The other case, further down, loads the real catalog and covers the
     * sixteen and their four folds. This one covers the shape that only appears
     * once somebody has saved a template of their own, which is the state the
     * fixture cannot reach.
     */
    @Test
    fun startingAProjectWithTheirOwnTemplateLabelsEverything() {
        compose.show {
            StartProjectScreen(
                templates = listOf(
                    TemplateCatalog.ProjectTemplate(
                        id = "medicaid_ltc",
                        name = "Medicaid application for long term care",
                        subtitle = "Applying for coverage of nursing home or facility care",
                        category = "paying",
                        stateVariance = true,
                        roles = emptyList(),
                        steps = listOf("One", "Two"),
                    ),
                    TemplateCatalog.ProjectTemplate(
                        id = "discharge_appeal",
                        name = "Fighting a facility discharge",
                        subtitle = "When a facility says someone has to leave",
                        category = "challenge",
                        stateVariance = false,
                        roles = emptyList(),
                        steps = listOf("One"),
                    ),
                ),
                own = listOf(
                    Repository.OwnTemplate(
                        id = "mine",
                        name = "The one I wrote",
                        derivedFromId = null,
                        steps = listOf("One"),
                        createdAt = 0L,
                    ),
                ),
                onChoose = {},
                onChooseOwn = {},
                onStartOwn = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("starting a project")
    }

    /**
     * The library, with one template in use and the rest folded, so the card
     * shape, the fold and the project row inside a card are all walked.
     */
    @Test
    fun theTemplateLibraryLabelsEverything() {
        val shipped = listOf(
            TemplateCatalog.ProjectTemplate(
                id = "medicaid_ltc",
                name = "Medicaid application for long term care",
                subtitle = "Applying for coverage of nursing home or facility care",
                category = "paying",
                stateVariance = true,
                roles = emptyList(),
                steps = listOf("One", "Two"),
            ),
            TemplateCatalog.ProjectTemplate(
                id = "records_request",
                name = "Requesting medical records",
                subtitle = "Getting copies of records you have a right to see",
                category = "papers",
                stateVariance = false,
                roles = emptyList(),
                steps = listOf("One"),
            ),
        )
        compose.show {
            TemplateLibraryScreen(
                shipped = shipped,
                own = emptyList(),
                projects = listOf(
                    Repository.Project(
                        "p1", "Medicaid application", "medicaid_ltc", "active",
                        null, null, 14, 3, "Find the last three bank statements",
                    ),
                ),
                onOpenProject = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("the template library")
    }

    /**
     * One month gathered, with every group present at once.
     *
     * **Every row on it is a door**, which is what makes it worth walking: a
     * screen made entirely of clickable nodes is the screen where an unlabeled
     * one costs the most, because a reader user meets nothing else.
     */
    @Test
    fun theMonthReviewLabelsEverything() {
        compose.show {
            MonthReviewScreen(
                review = Repository.MonthReview(
                    monthStart = 1_748_736_000_000,
                    entries = listOf(
                        Repository.TrailEntry(
                            id = "e1",
                            kind = "call",
                            title = "Care plan meeting",
                            body = "She was sitting up and knew who I was.",
                            occurredEdtf = "2026-06-29",
                            occurredStart = 1_751_000_000_000,
                            createdAt = 1_751_000_000_000,
                            isUnfiled = false,
                            threads = emptyList(),
                            pinnedAt = null,
                        ),
                    ),
                    kinds = listOf(Repository.KindCount("call", 1)),
                    milestones = listOf(
                        Repository.Milestone(
                            id = "m1",
                            label = "First day without oxygen",
                            occurredEdtf = "2026-06-19",
                            occurredStart = 1_750_000_000_000,
                            chapterId = null,
                            chapterName = null,
                            note = null,
                        ),
                    ),
                    appointments = listOf(
                        Repository.Appointment(
                            "a1", "Care plan meeting", "2026-06-11", 2L, "4West day room", null,
                        ),
                    ),
                    reported = listOf(
                        Repository.Incident(
                            id = "i1",
                            title = "Wrong medication brought to the room",
                            description = null,
                            reportedEdtf = "2026-06-12",
                            reportedStart = 1_749_000_000_000,
                            resolvedAt = 1_750_500_000_000,
                            resolutionNote = null,
                            chapterName = null,
                            entryCount = 2,
                        ),
                    ),
                    answered = listOf(
                        Repository.Incident(
                            id = "i2",
                            title = "Nobody called back",
                            description = null,
                            reportedEdtf = "2026-03-04",
                            reportedStart = 1_741_000_000_000,
                            resolvedAt = 1_750_500_000_000,
                            resolutionNote = null,
                            chapterName = null,
                            entryCount = 1,
                        ),
                    ),
                    documents = listOf(
                        Repository.Document(
                            id = "d1",
                            title = "Care plan, signed",
                            category = "medical",
                            originalLocation = null,
                            notes = null,
                            receivedEdtf = "2026-06-12",
                            sha256 = null,
                            byteSize = null,
                        ),
                    ),
                    began = listOf(
                        Repository.Chapter(
                            "c1", "Emergency department, overnight", null, null,
                            "2026-06-02", "2026-06-03",
                        ),
                    ),
                    ended = listOf(
                        Repository.Chapter(
                            "c1", "Emergency department, overnight", null, null,
                            "2026-06-02", "2026-06-03",
                        ),
                    ),
                ),
                onOpenEntry = {},
                onOpenMilestones = {},
                onOpenChapter = {},
                onOpenAppointment = {},
                onOpenIncident = {},
                onOpenDocument = {},
                onShare = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("the month review")
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
                onOpen = {},
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
                onOpen = {},
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
                onOpen = {},
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
            ExportScreen(state = ExportState.READY, onExport = { _, _ -> }, onBack = {})
        }
        assertEverythingIsLabeled("export")
    }

    @Test
    fun exportLabelsEverythingWhileWorking() {
        // Every control is disabled in this state, which is exactly when a
        // reader is most likely to meet something that announces nothing.
        compose.show {
            ExportScreen(state = ExportState.WORKING, onExport = { _, _ -> }, onBack = {})
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
    fun searchLabelsEverything() {
        compose.show {
            SearchScreen(
                query = "ward",
                onQueryChange = {},
                results = listOf(
                    Repository.SearchHit(
                        id = "h1",
                        section = Repository.Section.TRAIL,
                        title = "Nurse Okonkwo, ward 4",
                        kind = "call",
                        detail = "Said the dressing looks better",
                        chapterName = "Riverbend Rehab",
                        occurredEdtf = "2026-08-02",
                        occurredStart = 1_785_000_000_000L,
                    ),
                ),
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("search, with results")
    }

    @Test
    fun searchWithNothingTypedLabelsEverything() {
        compose.show {
            SearchScreen(
                query = "",
                onQueryChange = {},
                results = emptyList(),
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("search, resting")
    }

    @Test
    fun searchWithNoMatchLabelsEverything() {
        compose.show {
            SearchScreen(
                query = "zzzz",
                onQueryChange = {},
                results = emptyList(),
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("search, no match")
    }

    @Test
    fun searchThatFailedLabelsEverything() {
        compose.show {
            SearchScreen(
                query = "ward",
                onQueryChange = {},
                results = emptyList(),
                failed = true,
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("search, failed")
    }

    @Test
    fun oneEntryReadOnItsOwnLabelsEverything() {
        compose.show {
            EntryScreen(
                detail = Repository.EntryDetail(
                    entry = Repository.TrailEntry(
                        id = "e1",
                        kind = "call",
                        title = "Ward 4 nurse station",
                        body = "They said the dressing was changed this morning",
                        occurredEdtf = "2026-08-02",
                        occurredStart = 1_785_000_000_000L,
                        createdAt = 1_785_000_000_000L,
                        isUnfiled = false,
                        threads = listOf(Repository.CareThread("t1", "Wound care", 0)),
                        pinnedAt = null,
                    ),
                    people = listOf(Repository.Person("p1", "Marguerite Boateng", "Charge nurse", "555 0134", null, null)),
                    chapterId = "c1",
                    chapterName = "Riverbend Rehab",
                    incidentId = "i1",
                    incidentTitle = "Dressing not changed for two days",
                    incidentIsOpen = true,
                    medicationId = "m1",
                    medicationName = "Lisinopril",
                ),
                onEditDate = {},
                onOpenPerson = {},
                onOpenThread = {},
                onOpenChapter = {},
                onOpenMedication = {},
                onOpenIncident = {},
                onRemove = {},
                onSetPinned = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one entry, with everything hanging off it")
    }

    @Test
    fun anEntryWithNothingHangingOffItLabelsEverything() {
        compose.show {
            EntryScreen(
                detail = Repository.EntryDetail(
                    entry = Repository.TrailEntry(
                        id = "e2",
                        kind = "call",
                        title = null,
                        body = null,
                        occurredEdtf = null,
                        occurredStart = null,
                        createdAt = 1_785_000_000_000L,
                        isUnfiled = false,
                        threads = emptyList(),
                        pinnedAt = null,
                    ),
                    people = emptyList(),
                    chapterId = null,
                    chapterName = null,
                    incidentId = null,
                    incidentTitle = null,
                    incidentIsOpen = false,
                    medicationId = null,
                    medicationName = null,
                ),
                onEditDate = {},
                onOpenPerson = {},
                onOpenThread = {},
                onOpenChapter = {},
                onOpenMedication = {},
                onOpenIncident = {},
                onRemove = {},
                onSetPinned = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one entry, bare")
    }

    @Test
    fun incidentsLabelEverything() {
        compose.show {
            IncidentsScreen(
                incidents = listOf(
                    Repository.Incident(
                        id = "i1",
                        title = "Dressing not changed for two days",
                        description = null,
                        reportedEdtf = "2026-08-02",
                        reportedStart = 1_785_000_000_000L,
                        resolvedAt = null,
                        resolutionNote = null,
                        chapterName = "Riverbend Rehab",
                        entryCount = 2,
                    ),
                ),
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("incidents")
    }

    @Test
    fun aPrepSheetLabelsEverything() {
        compose.show {
            PrepScreen(
                prep = Repository.Prep(
                    appointment = Repository.Appointment(
                        id = "a1",
                        title = "Care plan meeting",
                        scheduledEdtf = "2026-08-20",
                        scheduledStart = 1_787_000_000_000L,
                        locationNote = "The day room",
                        notes = null,
                    ),
                    // **Two roles, so both halves are on screen.** The largest
                    // group leads open and the rest arrive as folds, and a
                    // fixture with one role would only ever exercise the first.
                    questions = listOf(
                        Repository.Question(
                            id = "q1",
                            text = "Why was the dressing schedule changed?",
                            roleLabel = "Charge nurse",
                            entryId = null,
                            askedEdtf = null,
                            answerText = null,
                        ),
                        Repository.Question(
                            id = "q2",
                            text = "Who authorized the room move?",
                            roleLabel = "Charge nurse",
                            entryId = null,
                            askedEdtf = null,
                            answerText = null,
                        ),
                        Repository.Question(
                            id = "q3",
                            text = "Can she have the window bed?",
                            roleLabel = "Social worker",
                            entryId = null,
                            askedEdtf = null,
                            answerText = null,
                        ),
                    ),
                    changes = listOf(
                        Repository.TrailEntry(
                            id = "e1",
                            kind = "call",
                            title = "Called the ward",
                            body = "They said the schedule had changed",
                            occurredEdtf = "2026-08-02",
                            occurredStart = 1_785_000_000_000L,
                            createdAt = 1_785_000_000_000L,
                            isUnfiled = false,
                            threads = emptyList(),
                            pinnedAt = null,
                        ),
                    ),
                    sinceEdtf = "2026-06-01",
                ),
                onOpenEntry = {},
                onShare = {},
                onWriteUp = {},
                onBack = {},
            )
        }
        // **The changes arrive folded, so opening it is part of the test.**
        // Asserting on the sheet as it lands would have checked everything
        // except the card that opens an entry, which is the one control here
        // that was announcing the wrong verb until 2026-08-04.
        compose.onNodeWithTag(PrepTags.CHANGES_FOLD).performClick()
        assertEverythingIsLabeled("a prep sheet")
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
                onSearch = {},
            )
        }
        assertEverythingIsLabeled("more")
    }

    @Test
    fun projectsLabelEverything() {
        compose.show {
            ProjectsScreen(
                projects = listOf(
                    Repository.Project("pr1", "Medicaid application", "medicaid_ltc", "waiting", "The caseworker", null, 14, 3, "Gather proof of income"),
                    // No steps and no waiting on, which strips two lines off the
                    // card and leaves the least to announce.
                    Repository.Project("pr2", "Records request", null, "done", null, null, 0, 0, null),
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
                project = Repository.Project("pr1", "Medicaid application", "medicaid_ltc", "active", null, null, 3, 1, "Gather proof of income"),
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
        // **What is done arrives folded, so opening it is part of the test.**
        // Asserting on the screen as it lands would check the step that is
        // still to do and never the one behind the fold, and putting a step
        // back is as ordinary as marking it done.
        compose.onNodeWithTag(ProjectDetailTags.DONE_FOLD).performClick()
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
