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
import com.kamsiob.healthtrail.ui.screens.AddMedicationScreen
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
}
