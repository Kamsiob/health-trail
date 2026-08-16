package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
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
import com.kamsiob.healthtrail.ui.screens.ExportTags
import com.kamsiob.healthtrail.ui.screens.AcknowledgeSheet
import com.kamsiob.healthtrail.ui.screens.AnswerSheet
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.MoreScreen
import com.kamsiob.healthtrail.ui.screens.AddAppointmentScreen
import com.kamsiob.healthtrail.ui.screens.AddBillScreen
import com.kamsiob.healthtrail.ui.screens.AddDocumentScreen
import com.kamsiob.healthtrail.ui.screens.DocumentsScreen
import com.kamsiob.healthtrail.ui.screens.MoneyScreen
import com.kamsiob.healthtrail.ui.screens.MoneyTags
import com.kamsiob.healthtrail.ui.screens.AddInstructionScreen
import com.kamsiob.healthtrail.ui.screens.AddMedicationScreen
import com.kamsiob.healthtrail.ui.screens.AppointmentsScreen
import com.kamsiob.healthtrail.ui.screens.AddThreadScreen
import com.kamsiob.healthtrail.ui.screens.CaptureBloom
import com.kamsiob.healthtrail.ui.screens.CareThreadsScreen
import com.kamsiob.healthtrail.ui.screens.CorrectEntryScreen
import com.kamsiob.healthtrail.ui.screens.CorrectIncidentScreen
import com.kamsiob.healthtrail.ui.screens.CorrectSubjectScreen
import com.kamsiob.healthtrail.ui.screens.ChaptersScreen
import com.kamsiob.healthtrail.ui.screens.ChoosePaperScreen
import com.kamsiob.healthtrail.ui.screens.PeopleScreen
import androidx.compose.ui.test.onAllNodesWithTag
import com.kamsiob.healthtrail.ui.screens.PaperViewerTags
import com.kamsiob.healthtrail.ui.screens.PaperViewerScreen
import com.kamsiob.healthtrail.ui.screens.ProgressScreen
import com.kamsiob.healthtrail.ui.screens.ProjectDetailScreen
import com.kamsiob.healthtrail.ui.screens.ProjectDetailTags
import com.kamsiob.healthtrail.ui.screens.ProjectsScreen
import com.kamsiob.healthtrail.ui.screens.StartProjectScreen
import com.kamsiob.healthtrail.ui.screens.QuestionsScreen
import com.kamsiob.healthtrail.ui.screens.StandingInstructionsScreen
import com.kamsiob.healthtrail.ui.screens.CorrectMeasureScreen
import com.kamsiob.healthtrail.ui.screens.CorrectReadingScreen
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
import com.kamsiob.healthtrail.ui.screens.DisclaimerScreen
import com.kamsiob.healthtrail.ui.screens.Emphasis
import com.kamsiob.healthtrail.ui.screens.MeasurementScreen
import com.kamsiob.healthtrail.ui.screens.MeasurementTags
import com.kamsiob.healthtrail.ui.screens.NotebookScreen
import com.kamsiob.healthtrail.ui.screens.SectionCount
import com.kamsiob.healthtrail.ui.screens.EntryScreen
import com.kamsiob.healthtrail.ui.screens.IncidentsScreen
import com.kamsiob.healthtrail.ui.screens.BillScreen
import com.kamsiob.healthtrail.ui.screens.DocumentScreen
import com.kamsiob.healthtrail.ui.screens.IncidentScreen
import com.kamsiob.healthtrail.ui.screens.MedicationScreen
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.ProjectHomeScreen
import com.kamsiob.healthtrail.ui.screens.AppearanceScreen
import com.kamsiob.healthtrail.ui.screens.ChapterScreen
import com.kamsiob.healthtrail.ui.screens.AddMilestoneScreen
import com.kamsiob.healthtrail.ui.screens.ConflictsScreen
import com.kamsiob.healthtrail.ui.screens.LogCallSheet
import com.kamsiob.healthtrail.ui.screens.MedicationEventScreen
import com.kamsiob.healthtrail.ui.screens.AddCardSheet
import com.kamsiob.healthtrail.ui.screens.CardOffer
import com.kamsiob.healthtrail.ui.screens.CardOptionsSheet
import com.kamsiob.healthtrail.ui.screens.PrepScreen
import com.kamsiob.healthtrail.ui.screens.TodayFieldScreen
import com.kamsiob.healthtrail.ui.screens.StartProjectPreviewSheet
import com.kamsiob.healthtrail.ui.screens.ProjectDateKindsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectPapersScreen
import com.kamsiob.healthtrail.ui.screens.ProjectRoadScreen
import com.kamsiob.healthtrail.ui.screens.ProjectDateSheet
import com.kamsiob.healthtrail.ui.screens.StageSheet
import com.kamsiob.healthtrail.ui.screens.StandingSheet
import com.kamsiob.healthtrail.ui.screens.StepEditSheet
import com.kamsiob.healthtrail.ui.screens.ProjectSetupScreen
import com.kamsiob.healthtrail.ui.screens.ProjectPaperworkScreen
import com.kamsiob.healthtrail.ui.screens.ProjectPeopleScreen
import com.kamsiob.healthtrail.ui.screens.ProjectStepsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectTrailScreen
import com.kamsiob.healthtrail.ui.screens.ThreadScreen
import com.kamsiob.healthtrail.ui.screens.ViolationScreen
import com.kamsiob.healthtrail.ui.screens.ViolationTags
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
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


    // ---- the detail screens, added for #342 ------------------------------
    //
    // **These are the screens somebody spends the most time on** and none of
    // them was walked here: a person, a medication, a bill, a document, an
    // incident, a project. `DESIGN.md` 12 said this test walks every screen
    // and it walked 44 of 75, which is a claim overstating a check, and a
    // claim like that is worse than a missing check because the next person
    // reads it and stops looking.
    //
    // **Each fixture carries real states rather than an empty screen**, since
    // an empty one has few nodes to label and would pass for free.

    @Test
    fun onePersonLabelsEverything() {
        compose.show {
            PersonScreen(
                person = Repository.Person(
                    "p1", "Angela Reyes", "Charge nurse, day shift", "5550142", null,
                    "Days, 7 to 3. Ask for her by name.",
                ),
                entries = listOf(
                    Repository.TrailEntry(
                        id = "e1", kind = "call", title = "Called about the shower schedule",
                        body = "She said it was a staffing decision.",
                        occurredEdtf = "2026-05-29", occurredStart = 1L, createdAt = 1L,
                        isUnfiled = false, threads = emptyList(), pinnedAt = null,
                    ),
                ),
                onCall = {},
                onSetPinned = {},
                onSetArchived = {},
                onEdit = {},
                onCapture = {},
                onRemove = {},
                onOpenEntry = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one person, with an entry")
    }

    /**
     * **The same person with nothing filled in but a name.**
     *
     * Rule 13: partial is a finished state, and every fixture in this file is
     * a full record, so the sparse one is the state nothing here was walking.
     * It is also where a reader is likeliest to meet an unlabeled node, because
     * an absent value renders as a fallback in one place and as nothing at all
     * in another, and only one of those announces itself.
     */
    @Test
    fun aPersonWithOnlyANameLabelsEverything() {
        compose.show {
            PersonScreen(
                person = Repository.Person("p1", "Dee", null, null, null, null),
                entries = emptyList(),
                onCall = {},
                onSetPinned = {},
                onSetArchived = {},
                onEdit = {},
                onCapture = {},
                onRemove = {},
                onOpenEntry = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one person, nothing but a name")
    }

    @Test
    fun oneMedicationLabelsEverything() {
        compose.show {
            MedicationScreen(
                medication = Repository.Medication(
                    "m1", "Donepezil", "5 mg, evenings", "Memory", null, true, null,
                ),
                history = listOf(
                    Repository.MedicationEvent(
                        "ev1", "started", "2026-01-10", "5 mg, evenings", null,
                        "Maplewood Care Center",
                    ),
                ),
                questions = listOf(
                    Repository.Question(
                        "q1", "Can the dose move earlier?", "The attending", null, null, null,
                    ),
                ),
                onOpenQuestion = {},
                onEdit = {},
                onRemove = {},
                onRecordChange = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one medication, with history and a question")
    }

    /**
     * **A medication with a name and nothing else**, which is what somebody
     * writes down standing in a hallway holding a bottle. Rule 13 again: no
     * field is required, so this is not a degraded record, it is the ordinary
     * one on the day it is created.
     */
    @Test
    fun aMedicationWithOnlyANameLabelsEverything() {
        compose.show {
            MedicationScreen(
                medication = Repository.Medication(
                    "m1", "Donepezil", null, null, null, false, null,
                ),
                history = emptyList(),
                questions = emptyList(),
                onOpenQuestion = {},
                onEdit = {},
                onRemove = {},
                onRecordChange = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one medication, nothing but a name")
    }

    @Test
    fun oneBillLabelsEverything() {
        compose.show {
            BillScreen(
                bill = Repository.Bill(
                    "b1", "Monthly room and board", 323701, "USD", "needs_attention",
                    "They billed the wrong plan", "2026-01-21", null,
                    chapterId = "c1", chapterName = "Maplewood Care Center",
                ),
                onEdit = {},
                onRemove = {},
                onOpenChapter = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one bill, with the place it came from")
    }

    @Test
    fun oneDocumentLabelsEverything() {
        compose.show {
            DocumentScreen(
                document = Repository.Document(
                    "d1", "Insurance card, both sides", "Insurance", "Filed with the county",
                    null, "2026-06-22", null, null,
                    chapterId = "c1", chapterName = "Maplewood Care Center",
                ),
                onEdit = {},
                onRemove = {},
                onOpenChapter = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one document")
    }

    @Test
    fun oneIncidentLabelsEverything() {
        compose.show {
            IncidentScreen(
                incident = Repository.Incident(
                    "i1", "Bruise on her arm nobody could explain",
                    "Reported to the charge nurse.", "2026-01-24", 1L, null, null,
                    "Maplewood Care Center", 1,
                ),
                detail = Repository.IncidentDetail(
                    entries = listOf(
                        Repository.TrailEntry(
                            id = "e1", kind = "call", title = "Reported it to the charge nurse",
                            body = "Asked for it in writing.",
                            occurredEdtf = "2026-01-24", occurredStart = 1L, createdAt = 1L,
                            isUnfiled = false, threads = emptyList(), pinnedAt = null,
                        ),
                    ),
                    people = listOf(
                        Repository.Person(
                            "p1", "Angela Reyes", "Charge nurse", "5550142", null, null,
                        ),
                    ),
                    // **Swept with the group that says what was not followed
                    // here**, because a screen swept without it is a screen
                    // swept without the half that was added. #371.
                    violations = listOf(
                        Repository.Violation(
                            id = "v1",
                            occurredEdtf = "2026-01-24",
                            note = "The dressing was not changed for two days",
                            incidentId = "i1",
                            incidentTitle = null,
                            billId = null,
                            billDescription = null,
                            instructionId = "s1",
                            instructionName = "Change the dressing daily",
                        ),
                    ),
                ),
                onOpenPerson = {},
                onOpenDocument = {},
                onOpenEntry = {},
                onAdd = {},
                onShare = {},
                onRemove = {},
                onCorrect = {},
                onResolve = {},
                onReopen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one incident, still open")
    }

    @Test
    fun oneProjectLabelsEverything() {
        compose.show {
            ProjectHomeScreen(
                project = Repository.Project(
                    id = "pr1", name = "The waiver application", templateId = "medicaid_ltc",
                    status = "active", waitingOn = null, notes = null,
                    stepCount = 2, doneCount = 1, nextStep = "Gather the statements",
                    lead = "standing",
                ),
                stages = listOf(
                    Repository.ProjectStage("s1", "Applied", 0, "2026-03-05", 1L),
                    Repository.ProjectStage("s2", "In review", 1, null, null),
                ),
                standing = Repository.ProjectStanding(
                    id = "st1", holderLabel = "The county", personId = null,
                    organizationId = null, activity = "reviewing it", sinceEdtf = "2026-03",
                    sinceStart = 1L, entryId = null, note = null,
                ),
                nextDate = Repository.ProjectDate(
                    id = "d1", kind = "Decision expected", dueEdtf = "2026-09-12",
                    dueStart = 2L, sourceNote = "the letter of March 5",
                    sourceDocumentId = null, sourceEntryId = null,
                ),
                latestWord = null,
                countdown = "12 days",
                dateKind = "Decision expected",
                dateWhen = "September 12, 2026",
                standingSince = "reviewing it",
                steps = listOf(
                    Repository.ProjectStep("x1", "Get the form", "2026-03-06", null, null, "Me"),
                    Repository.ProjectStep("x2", "Gather the statements", null, null, null, null),
                ),
                papers = emptyList(),
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one project, standing led")
    }


    @Test
    fun oneCareThreadLabelsEverything() {
        compose.show {
            ThreadScreen(
                thread = Repository.CareThread("t1", "Nursing", 0),
                entries = listOf(
                    Repository.TrailEntry(
                        id = "e1", kind = "call", title = "Asked about the dressing",
                        body = "They will change it daily.",
                        occurredEdtf = "2026-04-02", occurredStart = 1L, createdAt = 1L,
                        isUnfiled = false, threads = emptyList(), pinnedAt = null,
                    ),
                ),
                onOpenEntry = {},
                onRename = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one care thread")
    }

    @Test
    fun oneChapterLabelsEverything() {
        compose.show {
            ChapterScreen(
                detail = Repository.ChapterDetail(
                    chapter = Repository.Chapter(
                        "c1", "Maplewood Care Center", "Moved in after the hospital",
                        "Room 214, west wing.", "2026-01-08", null,
                    ),
                    entries = listOf(
                        Repository.TrailEntry(
                            id = "e1", kind = "visit", title = "First care plan meeting",
                            body = "Everyone was there.",
                            occurredEdtf = "2026-02-01", occurredStart = 1L, createdAt = 1L,
                            isUnfiled = false, threads = emptyList(), pinnedAt = null,
                        ),
                    ),
                    incidents = listOf(
                        Repository.Incident(
                            "i1", "Bruise nobody could explain", null, "2026-01-24", 1L,
                            null, null, "Maplewood Care Center", 1,
                        ),
                    ),
                    documents = listOf(
                        Repository.Document(
                            "d1", "Admission agreement", null, "The blue folder", null,
                            "2026-01-08", null, null,
                        ),
                    ),
                    milestones = listOf(
                        Repository.Milestone(
                            "ms1", "She settled in", "2026-02", 1L, "c1",
                            "Maplewood Care Center", null,
                        ),
                    ),
                ),
                onOpenEntry = {},
                onOpenIncident = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("one chapter, with everything hanging off it")
    }

    /**
     * **The disclosure is opened here rather than left folded**, because what is
     * inside it is the part that was added: the chips saying which incident or
     * which bill this broke against. A reader sweep of a form with its optional
     * half closed is a sweep of half the form. #371.
     */
    @Test
    fun writingDownAViolationLabelsEverything() {
        compose.show {
            ViolationScreen(
                instruction = Repository.StandingInstruction(
                    "s1", "Call me about any fall", "Please call me right away.",
                    "federal", "2026-08-02", null, null, null,
                ),
                onSave = {},
                onCancel = {},
                incidents = listOf(
                    Repository.Incident(
                        "i1", "Bruise on her arm nobody could explain", null,
                        "2026-08-03", null, null, null, null, 1,
                    ),
                ),
                bills = listOf(
                    Repository.Bill(
                        "b1", "Ambulance transfer", 316877, "USD", "disputed",
                        null, "2026-08-04", null,
                    ),
                ),
            )
        }
        compose.onNodeWithTag(ViolationTags.ABOUT).performScrollTo().performClick()
        assertEverythingIsLabeled("writing down a time it was not followed")
    }

    @Test
    fun appearanceLabelsEverything() {
        compose.show {
            AppearanceScreen(choice = ThemeChoice.FOLLOW_SYSTEM, onChoose = {})
        }
        assertEverythingIsLabeled("appearance on its own")
    }


    @Test
    fun aProjectsTrailLabelsEverything() {
        compose.show {
            ProjectTrailScreen(
                projectName = "The waiver application",
                items = listOf(
                    Repository.ProjectTrailItem(
                        kind = Repository.ProjectTrailKind.ENTRY,
                        id = "e1",
                        whenEdtf = "2026-03-05",
                        whenStart = 1L,
                        title = "Called the caseworker",
                        note = "She said to call back after the 15th.",
                        entry = Repository.TrailEntry(
                            id = "e1", kind = "call", title = "Called the caseworker",
                            body = "She said to call back after the 15th.",
                            occurredEdtf = "2026-03-05", occurredStart = 1L, createdAt = 1L,
                            isUnfiled = false, threads = emptyList(), pinnedAt = null,
                        ),
                    ),
                    Repository.ProjectTrailItem(
                        kind = Repository.ProjectTrailKind.STAGE,
                        id = "s1",
                        whenEdtf = "2026-03-05",
                        whenStart = 1L,
                        title = "Applied",
                        note = null,
                    ),
                ),
                onOpenEntry = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's trail")
    }

    @Test
    fun aProjectsPeopleLabelEverything() {
        compose.show {
            ProjectPeopleScreen(
                projectName = "The waiver application",
                people = listOf(
                    Repository.ProjectPerson(
                        person = Repository.Person(
                            "p1", "Denise Alvarado", "Intake caseworker", "5550142", null, null,
                        ),
                        mentions = 4,
                        lastEdtf = "2026-06-29",
                        lastStart = 1L,
                        // The cross project door, which is the one new
                        // navigation idea on this surface and has to announce.
                        alsoIn = listOf(
                            Repository.EntryProject("pr2", "The appeal", "active"),
                        ),
                    ),
                ),
                careTeamSize = 15,
                onOpenPerson = {},
                onOpenProject = {},
                onCall = {},
                onOpenCareTeam = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's people")
    }

    @Test
    fun aProjectsPapersLabelEverything() {
        compose.show {
            ProjectPaperworkScreen(
                projectName = "The waiver application",
                papers = listOf(
                    // **One filled and one waiting**, because an empty slot and
                    // a filled one draw differently and both have to announce.
                    Repository.ProjectPaperCard(
                        paper = Repository.ProjectPaper("pp1", "The award letter", 0, "received", "d1"),
                        documentId = "d1",
                        title = "Award letter",
                        sha256 = null,
                        receivedEdtf = "2026-06-22",
                        originalLocation = "Filed with the county",
                    ),
                    Repository.ProjectPaperCard(
                        paper = Repository.ProjectPaper("pp2", "The certified copy", 1, null, null),
                        documentId = null,
                        title = null,
                        sha256 = null,
                        receivedEdtf = null,
                        originalLocation = null,
                    ),
                ),
                onOpenDocument = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's papers, one filled and one waiting")
    }

    @Test
    fun aProjectsStepsLabelEverything() {
        compose.show {
            ProjectStepsScreen(
                projectName = "The waiver application",
                steps = listOf(
                    Repository.ProjectStep("x1", "Get the form", "2026-03-06", null, null, "Me"),
                    Repository.ProjectStep("x2", "Gather the statements", null, null, null, null),
                ),
                onAdd = {},
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's starting steps")
    }


    @Test
    fun whatTheMergeDecidedLabelsEverything() {
        compose.show {
            ConflictsScreen(
                resolutions = listOf(
                    Repository.Resolution(
                        seq = 1L,
                        table = "entry",
                        rowId = "e1",
                        resolvedAt = 1L,
                        kept = "mine",
                        reason = "later_updated_at",
                        differences = listOf(
                            Repository.Difference(
                                column = "title",
                                render = "text",
                                vocabulary = null,
                                keptValue = "Called the caseworker",
                                otherValue = "Called the case worker",
                            ),
                        ),
                        seen = false,
                    ),
                ),
                onBack = {},
            )
        }
        assertEverythingIsLabeled("what the merge decided")
    }

    @Test
    fun addingAMilestoneLabelsEverything() {
        compose.show {
            AddMilestoneScreen(
                onSave = {},
                onCancel = {},
                chapters = listOf(
                    Repository.Chapter(
                        "c1", "Maplewood Care Center", null, null, "2026-01-08", null,
                    ),
                ),
            )
        }
        assertEverythingIsLabeled("adding a milestone")
    }

    @Test
    fun aProjectsSetupLabelsEverything() {
        compose.show {
            ProjectSetupScreen(
                project = Repository.Project(
                    id = "pr1", name = "The waiver application", templateId = "medicaid_ltc",
                    status = "active", waitingOn = "The county", notes = null,
                    stepCount = 2, doneCount = 1, nextStep = null, lead = "standing",
                ),
                stages = listOf(Repository.ProjectStage("s1", "Applied", 0, "2026-03-05", 1L)),
                steps = listOf(
                    Repository.ProjectStep("x1", "Get the form", null, null, null, null),
                ),
                papers = listOf(Repository.ProjectPaper("pp1", "The award letter", 0, null, null)),
                dateKinds = listOf("Decision expected"),
                onSetLead = {},
                onSetStatus = {},
                onSetWaitingOn = {},
                onSaveAsTemplate = {},
                onOpenSteps = {},
                onOpenRoad = {},
                onOpenKinds = {},
                onOpenPapers = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's setup")
    }


    // **The project sheets, which are their own windows**, and the reason this
    // test walks every root rather than the root: a sheet asked for as "the"
    // root is a screen the test cannot see, and a screen it cannot see is one
    // it passes for free.

    @Test
    fun oneStepOpenedLabelsEverything() {
        compose.show {
            StepEditSheet(
                step = Repository.ProjectStep(
                    "x1", "Gather the statements", null, "Three months of them", null, "Me",
                ),
                canMoveEarlier = true,
                canMoveLater = true,
                onSave = { _, _ -> },
                onMove = {},
                onRemove = {},
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("one step, opened")
    }

    @Test
    fun movingAlongTheRoadLabelsEverything() {
        compose.show {
            StageSheet(
                stages = listOf(
                    Repository.ProjectStage("s1", "Applied", 0, "2026-03-05", 1L),
                    Repository.ProjectStage("s2", "In review", 1, null, null),
                ),
                currentStageId = "s1",
                onPick = { _, _ -> },
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("moving a project along the road")
    }

    @Test
    fun recordingWhereItStandsLabelsEverything() {
        compose.show {
            StandingSheet(
                people = listOf("The county", "Denise Alvarado"),
                onSave = { _, _, _ -> },
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("recording where a project stands")
    }

    @Test
    fun writingDownADateLabelsEverything() {
        compose.show {
            ProjectDateSheet(
                kinds = listOf("Decision expected", "Hearing"),
                onSave = { _, _, _ -> },
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("writing down a project date")
    }

    @Test
    fun loggingACallLabelsEverything() {
        compose.show {
            LogCallSheet(
                projectName = "The waiver application",
                onSave = { _, _ -> },
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("logging a call on a project")
    }


    @Test
    fun aProjectsRoadLabelsEverything() {
        compose.show {
            ProjectRoadScreen(
                projectName = "The waiver application",
                stages = listOf(
                    Repository.ProjectStage("s1", "Applied", 0, "2026-03-05", 1L),
                    Repository.ProjectStage("s2", "In review", 1, null, null),
                ),
                onAdd = {},
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's road")
    }

    @Test
    fun aProjectsPaperPlacesLabelEverything() {
        compose.show {
            ProjectPapersScreen(
                projectName = "The waiver application",
                papers = listOf(
                    Repository.ProjectPaper("pp1", "The award letter", 0, "received", "d1"),
                    Repository.ProjectPaper("pp2", "The certified copy", 1, null, null),
                ),
                onAdd = {},
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's places for paper")
    }

    @Test
    fun aProjectsDateKindsLabelEverything() {
        compose.show {
            ProjectDateKindsScreen(
                projectName = "The waiver application",
                kinds = listOf(
                    Repository.ProjectDateKind("k1", "Decision expected"),
                    Repository.ProjectDateKind("k2", "Hearing"),
                ),
                onAdd = {},
                onOpen = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("a project's date kinds")
    }

    @Test
    fun writingDownAMedicationChangeLabelsEverything() {
        compose.show {
            MedicationEventScreen(
                medicationId = "m1",
                medicationName = "Donepezil",
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("writing down a medication change")
    }


    // **Today's two sheets and the preview**, which are the last of the ones a
    // person reaches by hand. #342.

    @Test
    fun addingACardLabelsEverything() {
        compose.show {
            AddCardSheet(
                offers = listOf(
                    CardOffer(
                        type = "medications",
                        label = "Medications",
                        preview = "6 on the list now",
                        suggested = true,
                    ),
                    CardOffer(
                        type = "project_steps",
                        label = "The waiver application",
                        preview = "10 steps in the plan",
                        sourceTable = "project",
                        sourceId = "pr1",
                        // **A catalog key, not a group name.** The screen hands
                        // this straight to GroupHeader, and `Strings.resolve`
                        // throws on a key no catalog defines rather than falling
                        // back, deliberately. A fixture with "projects" in it
                        // crashed the sheet, which is TRAPS section 3's first
                        // trap seen from the test's side.
                        groupKey = "notebook.section.projects",
                    ),
                ),
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("adding a card to Today")
    }

    @Test
    fun oneCardsOptionsLabelEverything() {
        compose.show {
            CardOptionsSheet(
                name = "Medications",
                size = "small",
                onResize = {},
                onPromote = {},
                onMoveUp = {},
                onMoveDown = {},
                onRemove = {},
                onPickSource = {},
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("one card's options")
    }

    @Test
    fun whatATemplateSetsUpLabelsEverything() {
        val catalog = runBlocking { TemplateCatalog.projects(context) }
        compose.show {
            StartProjectPreviewSheet(
                template = catalog.first(),
                onCreate = {},
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("what a project template sets up")
    }


    /**
     * **Today itself, which is the screen the app opens on**, and the last one
     * this check did not reach. #342.
     *
     * A lead and four cards, two of which answer with something and two of
     * which are empty, because 21.4's states ladder makes those draw
     * differently and a field of empty cards would pass for free.
     */
    @Test
    fun theTodayFieldLabelsEverything() {
        fun card(id: String, type: String, size: String = "small", lead: Boolean = false) =
            Repository.TodayCard(
                id = id, type = type, size = size, sortIndex = 0, isLead = lead,
                sourceTable = null, sourceId = null,
            )
        compose.show {
            TodayFieldScreen(
                layout = Repository.TodayLayout(
                    lead = card("c-digest", "digest", size = "wide", lead = true),
                    field = listOf(
                        card("c-next", "next_up"),
                        card("c-meds", "medications"),
                        card("c-ask", "ask_next_time"),
                        card("c-emerg", "emergency_card"),
                    ),
                ),
                answers = mapOf(
                    "c-next" to Repository.TodayAnswer(
                        title = "Care plan meeting", whenEdtf = "2026-08-20",
                    ),
                    "c-meds" to Repository.TodayAnswer(count = 6),
                    "c-ask" to Repository.TodayAnswer(),
                    "c-emerg" to Repository.TodayAnswer(),
                ),
                onOpen = {},
            )
        }
        assertEverythingIsLabeled("Today, the screen the app opens on")
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

    /**
     * **The third answer to "what are you tracking"**, reached by a tap because
     * it is a face of the same screen rather than a screen of its own. #203.
     */
    @Test
    fun namingSomethingToTrackLabelsEverything() {
        val presets = runBlocking { TemplateCatalog.presets(context) }
        compose.show {
            MeasurementScreen(
                measures = emptyList(),
                presets = presets,
                onSave = {},
                onCancel = {},
            )
        }
        // **`performScrollToNode` rather than `performScrollTo`**, because a
        // LazyColumn does not compose what is below the fold at all, so the
        // node cannot be found before the list has been asked to reach it.
        compose.onNodeWithTag(MeasurementTags.PICK)
            .performScrollToNode(hasTestTag(MeasurementTags.OWN))
        compose.onNodeWithTag(MeasurementTags.OWN).performClick()
        assertEverythingIsLabeled("naming something to track")
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
                onOpen = {},
                onAdd = {},
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
                onMarkAsked = {},
                onRemove = {},
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
                onRemove = {},
                onDismiss = {},
            )
        }
        assertEverythingIsLabeled("acknowledging an instruction")
    }

    /**
     * Correcting an incident, #358: one screen, three fields and a date that
     * can be "not sure", so the reader has to announce the chips as well as
     * the words.
     */
    @Test
    fun correctingAnIncidentLabelsEverything() {
        compose.show {
            CorrectIncidentScreen(
                incident = Repository.Incident(
                    id = "i1",
                    title = "Bruise on her arm nobody could explain",
                    description = "Reported to the charge nurse.",
                    reportedEdtf = "2026-03",
                    reportedStart = null,
                    resolvedAt = null,
                    resolutionNote = null,
                    chapterName = "Maplewood Care Center",
                    entryCount = 3,
                ),
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("correcting an incident")
    }

    /**
     * The capture bloom, which replaced the sheet of tiles: six choices, each
     * announcing its own words, over a scrim that carries no label of its own.
     */
    @Test
    fun theCaptureBloomLabelsEveryChoice() {
        compose.show {
            CaptureBloom(onChoose = {}, onDismiss = {})
        }
        assertEverythingIsLabeled("the capture bloom")
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
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("care threads")
    }

    /**
     * Starting a thread from nothing, #349 and D145. The screen asks one
     * question, so the reader has one field, one action and the way out to
     * announce, and the action is disabled until the field has something in it.
     */
    @Test
    fun startingAThreadLabelsEverything() {
        compose.show {
            AddThreadScreen(onStart = {}, onCancel = {})
        }
        assertEverythingIsLabeled("starting a care thread")
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
                onMoved = {},
                // What the current chapter holds, #356: counted on the card
                // rather than only inside it.
                contents = mapOf(
                    "c1" to Repository.ChapterContents(
                        entries = 23,
                        documents = 7,
                        people = 6,
                        openIncidents = 1,
                    ),
                ),
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
                        name = "Appealing a facility discharge",
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
                onOpen = {},
                onAdd = {},
                onBack = {},
                // **The times it was not followed, in the person's own words**,
                // which the row showed only as a count until #371. One of them
                // says what it broke against, so the swept screen carries both
                // shapes the row can draw.
                violations = mapOf(
                    "s1" to listOf(
                        Repository.Violation(
                            "v1", "2026-08-05", "The night nurse gave it at 9 instead of 6",
                            "i1", "Bruise on her arm nobody could explain", null, null,
                        ),
                        Repository.Violation("v2", "2026-08-06", null, null, null, null, null),
                    ),
                ),
            )
        }
        assertEverythingIsLabeled("standing instructions")
    }

    /**
     * **The eyebrow that says why one bill sits above the folds**, per grid
     * screen 14 and the #345 fidelity pass. Alert ink, and the words carry the
     * meaning so section 9 holds with the color only agreeing with them.
     */
    @Test
    fun theBillNeedingADecisionIsSaidToBeOne() {
        compose.show {
            MoneyScreen(
                bills = listOf(
                    Repository.Bill("b1", "Room and board", 334762, "USD", "needs_attention", null, "2026-08-06", null),
                    Repository.Bill("b2", "Ambulance transfer", 316877, "USD", "disputed", null, null, null),
                ),
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        compose.onNodeWithTag(MoneyTags.LEAD_HEADER).assertExists()
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
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("documents")
    }

    /** The last of the app's records to get a correction path. #368. */
    @Test
    fun correctingAnEntryLabelsEverything() {
        compose.show {
            CorrectEntryScreen(
                entry = Repository.TrailEntry(
                    id = "e1",
                    kind = "call",
                    title = "Spoke to the ward about the warfain dose",
                    body = "She said she would check with the pharmacy and ring back.",
                    occurredEdtf = "2026-06-29",
                    occurredStart = 1_782_691_200_000,
                    createdAt = 1_782_691_200_000,
                    isUnfiled = false,
                    threads = emptyList(),
                    pinnedAt = null,
                ),
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("correcting an entry")
    }

    /** Correcting who the notebook is about. #371. */
    @Test
    fun correctingTheSubjectLabelsEverything() {
        compose.show {
            CorrectSubjectScreen(
                subject = Repository.Subject(
                    id = "s1",
                    displayName = "Margaret Ellison",
                    relationship = "Mom",
                    situationTemplateId = null,
                ),
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("correcting the subject")
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

    /**
     * **The failed export, which is the state that matters most here.**
     *
     * Export is the only recovery path this app has for its own encryption,
     * per D24, so the screen that says it did not work is load bearing. It was
     * the one `ExportState` no test composed: READY, WORKING and the two
     * finished faces all had cases and this did not.
     */
    @Test
    fun exportLabelsEverythingAfterItFailed() {
        compose.show {
            ExportScreen(state = ExportState.FAILED, onExport = { _, _ -> }, onBack = {})
        }
        assertEverythingIsLabeled("export, failed")
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
     * The finished screen, and the version of it that has something to add.
     *
     * **Two cases rather than one, because they are different screens.** The
     * plain one replaced the form with a title and a sentence; this one adds a
     * headed block naming what the export could not find, and the block is the
     * only thing on the screen a reader has never met. #332.
     *
     * **It also renders the three catalog keys**, which is what proves them.
     * `check_string_keys.py` holds the code to the catalogs and the catalogs to
     * each other, and neither of those runs `MessageFormat` over a plural
     * pattern with six Arabic forms in it. Only rendering does.
     */
    @Test
    fun exportLabelsEverythingWhenSaved() {
        compose.show {
            ExportScreen(state = ExportState.DONE, onExport = { _, _ -> }, onBack = {})
        }
        assertEverythingIsLabeled("export, saved")
    }

    @Test
    fun exportLabelsEverythingWhenSavedWithoutAFile() {
        compose.show {
            ExportScreen(
                state = ExportState.DONE,
                onExport = { _, _ -> },
                onBack = {},
                // Two rather than one, so the plural form is the one exercised.
                // A count of one passes on a template that has no plural rule
                // at all.
                missingAttachments = 2,
            )
        }
        compose.onNodeWithTag(ExportTags.MISSING).assertIsDisplayed()
        assertEverythingIsLabeled("export, saved without a file")
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
                onCorrect = {},
                onOpenPerson = {},
                onOpenThread = {},
                onOpenChapter = {},
                onOpenProject = {},
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
                onCorrect = {},
                onOpenPerson = {},
                onOpenThread = {},
                onOpenChapter = {},
                onOpenProject = {},
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
                onOpenQuestion = {},
                onCorrect = {},
                onRemove = {},
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
                onStart = {},
            )
        }
        assertEverythingIsLabeled("projects")
    }

    // ---- the six corrections, added for #374 -----------------------------
    //
    // **They were composed, tested and walked, and none of that is this
    // check.** Rule 19 is a gate: every touchable node says what it does, or
    // somebody using a reader meets a button that announces nothing. Six new
    // full screen surfaces went in overnight and the reader walk is what
    // `DESIGN.md` 12 asks of each of them before its issue closes.

    @Test
    fun correctingANameLabelsEverything() {
        compose.show {
            AddThreadScreen(
                onStart = {},
                onCancel = {},
                titleKey = "chapters.rename.title",
                labelKey = "chapters.rename.name",
                hintKey = null,
                saveKey = "chapters.rename.save",
                leadKey = "chapters.rename.lead",
                section = Repository.Section.CHAPTERS,
                initialName = "Maplewood Care Center",
            )
        }
        assertEverythingIsLabeled("correcting a chapter's name")
    }

    @Test
    fun correctingWordsLabelsEverything() {
        // **The multiline case**, which is a different composable path inside
        // the field: a question is a sentence rather than a name.
        compose.show {
            AddThreadScreen(
                onStart = {},
                onCancel = {},
                titleKey = "ask.correct.title",
                labelKey = "ask.correct.field",
                hintKey = null,
                saveKey = "ask.correct.save",
                leadKey = "ask.correct.lead",
                section = Repository.Section.ASK_NEXT_TIME,
                initialName = "When was the last time she was weighed?",
                singleLine = false,
            )
        }
        assertEverythingIsLabeled("correcting a question's words")
    }

    @Test
    fun correctingAReadingLabelsEverything() {
        compose.show {
            CorrectReadingScreen(
                name = "Weight",
                units = listOf("lb"),
                isText = false,
                reading = Repository.Reading(
                    id = "r1",
                    measureId = "m1",
                    number = 138.8,
                    text = null,
                    unit = "lb",
                    occurredEdtf = "2026-03-04",
                    occurredStart = 1772582400000L,
                    note = "After breakfast",
                    source = null,
                ),
                onSave = { _, _, _, _, _ -> },
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("correcting a reading")
    }

    @Test
    fun correctingAMeasureLabelsEverything() {
        compose.show {
            CorrectMeasureScreen(
                measure = Repository.Measure(
                    id = "m1",
                    name = "Weight",
                    presetId = null,
                    unit = "lb",
                    isText = false,
                ),
                onSave = {},
                onCancel = {},
            )
        }
        assertEverythingIsLabeled("correcting a measure")
    }

    @Test
    fun projectsLabelEverythingWhenEmpty() {
        compose.show {
            ProjectsScreen(projects = emptyList(), onOpen = {}, onStart = {})
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

    /**
     * The paper at reading size, #378. Two touchables: the paper itself,
     * which announces the document's own name rather than "image", and the
     * way back. The fixture stores a real file through a real Attachments
     * root, because a viewer walked with a missing file would only ever walk
     * the failure text.
     */
    @Test
    fun thePaperViewerLabelsEverything() {
        val root = java.io.File(context.cacheDir, "reader-walk-attachments").apply { mkdirs() }
        val attachments = com.kamsiob.healthtrail.data.Attachments.openAt(root)
        val png = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        val out = java.io.ByteArrayOutputStream()
        png.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        val sha = kotlinx.coroutines.runBlocking { attachments.put(out.toByteArray()) }

        compose.show {
            PaperViewerScreen(
                sha256 = sha,
                title = "Discharge summary, March",
                attachments = attachments,
                onBack = {},
            )
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(PaperViewerTags.IMAGE, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertEverythingIsLabeled("the paper viewer")
    }

    /**
     * Who the notebook is about, #379. Two people, one showing, so the walk
     * covers both the row that switches and the row that only marks.
     */
    @Test
    fun theSubjectsLabelEverything() {
        compose.show {
            PeopleScreen(
                subjects = listOf(
                    Repository.Subject("s1", "Margaret", "Mom", null),
                    Repository.Subject("s2", "Walter", "Dad", null),
                ),
                activeId = "s1",
                onSwitch = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("who this notebook is about")
    }

    /**
     * Choosing which paper fills an empty place on a project, #379. Both
     * doors are walked: the papers already kept, and photographing a new one.
     */
    @Test
    fun choosingAPaperLabelsEverything() {
        compose.show {
            ChoosePaperScreen(
                placeName = "The decision letter",
                documents = listOf(
                    Repository.Document(
                        id = "d1",
                        title = "Discharge summary",
                        category = null,
                        originalLocation = "Blue folder",
                        notes = null,
                        receivedEdtf = "2026-08-01",
                        sha256 = null,
                        byteSize = null,
                    ),
                ),
                attachments = null,
                onChoose = {},
                onPhotograph = {},
                onBack = {},
            )
        }
        assertEverythingIsLabeled("choosing a paper")
    }
}
