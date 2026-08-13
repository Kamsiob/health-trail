package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AcknowledgeSheet
import com.kamsiob.healthtrail.ui.screens.AcknowledgeTags
import com.kamsiob.healthtrail.ui.screens.AnswerSheet
import com.kamsiob.healthtrail.ui.screens.AnswerTags
import com.kamsiob.healthtrail.ui.screens.AppointmentsScreen
import com.kamsiob.healthtrail.ui.screens.ApptTags
import com.kamsiob.healthtrail.ui.screens.BillScreen
import com.kamsiob.healthtrail.ui.screens.BillTags
import com.kamsiob.healthtrail.ui.screens.CareTeamScreen
import com.kamsiob.healthtrail.ui.screens.CareTeamTags
import com.kamsiob.healthtrail.ui.screens.DocTags
import com.kamsiob.healthtrail.ui.screens.DocumentScreen
import com.kamsiob.healthtrail.ui.screens.DocumentsScreen
import com.kamsiob.healthtrail.ui.screens.InstructionTags
import com.kamsiob.healthtrail.ui.screens.MedicationScreen
import com.kamsiob.healthtrail.ui.screens.MedicationTags
import com.kamsiob.healthtrail.ui.screens.MedicationsScreen
import com.kamsiob.healthtrail.ui.screens.MedsTags
import com.kamsiob.healthtrail.ui.screens.MoneyScreen
import com.kamsiob.healthtrail.ui.screens.MoneyTags
import com.kamsiob.healthtrail.ui.screens.OneDocTags
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.PersonTags
import com.kamsiob.healthtrail.ui.screens.PrepScreen
import com.kamsiob.healthtrail.ui.screens.PrepTags
import com.kamsiob.healthtrail.ui.screens.ProjectHomeScreen
import com.kamsiob.healthtrail.ui.screens.ProjectHomeTags
import com.kamsiob.healthtrail.ui.screens.ProjectsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectTags
import com.kamsiob.healthtrail.ui.screens.QuestionTags
import com.kamsiob.healthtrail.ui.screens.QuestionsScreen
import com.kamsiob.healthtrail.ui.screens.StandingInstructionsScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Removal is reachable by looking, and by nothing else. #218.
 *
 * **Law 2 bans a long-press-only action**, and removal was one in nine screens.
 * It was not a bare gesture, which is exactly why it lasted: it declared an
 * explicit long click semantics action, so a **reader** user was handed removal
 * in their action list while a **sighted** person who did not already know the
 * gesture could not remove anything at all. `DESIGN.md` 13.5 names that
 * inversion, and no screenshot of it differs from a screenshot of the fix.
 *
 * **Two halves, and both are needed.** That no row offers the gesture is held
 * for every source file by `tools/checks/check_dead_gestures.py`. That the
 * visible path actually exists, and actually calls back, can only be held here:
 * a check can see that a control is drawn and cannot see that it does anything.
 *
 * **The nine screens' removals landed in eight places.** Six things have a
 * screen of their own and removal went there; a question and a standing
 * instruction have a sheet instead, and it went there. Both kinds are asserted.
 * The ninth screen, `IncidentScreen`, only ever imported the modifier.
 */
@RunWith(AndroidJUnit4::class)
class RemovalIsVisibleTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private var removals = 0

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme { content() }
            }
        }
    }

    /** No row anywhere offers the gesture that used to be the only way. */
    private fun assertNoLongPress(tag: String) {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        assertNull(
            "$tag still declares a long press, which is the action law 2 bans",
            node.config.getOrNull(SemanticsActions.OnLongClick),
        )
    }

    /**
     * The visible control is there, it can be reached, and it calls back once.
     *
     * **`performScrollTo` first**, because on every one of these the control is
     * at the foot of a list, and a test that only asserted it existed would pass
     * on a screen where nobody could reach it at font scale 2.0.
     */
    private fun assertRemovesOnce(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        assertEquals("the visible removal did not call back exactly once", 1, removals)
    }

    /**
     * The same, for a sheet.
     *
     * **A sheet is not scrolled to, it is already there**, and asking for a
     * scrollable ancestor inside one throws rather than failing usefully. The
     * reachability question a sheet has instead is whether its own content
     * scrolls at font scale 2.0, which is section 7's rule for sheets and is
     * checked on the phone rather than here.
     */
    private fun assertSheetRemovesOnce(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        assertEquals("the sheet's removal did not call back exactly once", 1, removals)
    }

    // ---- the fixtures, one of each thing --------------------------------

    private val person = Repository.Person(
        "p1", "Denise Alvarado", "Intake caseworker", "5550142", null, null,
    )

    private val medication = Repository.Medication(
        "m1", "Warfarin", "Small blue one", "Blood thinner", null, true, null,
    )

    /**
     * **`needs_attention`, which is deliberate.** `MoneyScreen` leads with the
     * first state in its own order and folds every other one, so a bill in any
     * other state is not composed at all until somebody opens its fold. A
     * fixture that landed in a fold would fail this test for a reason that has
     * nothing to do with removal.
     */
    private val bill = Repository.Bill(
        "b1", "Maplewood General, room and board", 128450, "USD",
        "needs_attention", null, "2026-08-02", null,
    )

    private val document = Repository.Document(
        "d1", "Signed consent form", null, "Blue folder", null, "2026-08-02", null, null,
    )

    private val appointment = Repository.Appointment(
        "a1", "Care plan meeting", "2026-08-20", 1_787_000_000_000L, "The day room", null,
    )

    private val project = Repository.Project(
        id = "pr1",
        name = "The waiver application",
        templateId = "medicaid_ltc",
        status = "active",
        waitingOn = null,
        notes = null,
        stepCount = 0,
        doneCount = 0,
        nextStep = null,
        lead = "standing",
    )

    private val asked = Repository.Question(
        "q1", "Can the water pill move earlier?", "The attending", null,
        "2026-08-01", "They will review it",
    )

    private val waiting = Repository.Question(
        "q2", "Is the dressing changed daily?", "The wound nurse", null, null, null,
    )

    private val instruction = Repository.StandingInstruction(
        "s1", "Call me about any fall", "Please call me right away.",
        "federal", "2026-08-02", null, null, null,
    )

    // ---- half one: no list row hides an action behind a gesture ---------

    @Test
    fun noAppointmentRowOffersALongPress() {
        show {
            AppointmentsScreen(
                appointments = listOf(appointment),
                todayMillis = 1L,
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertNoLongPress(ApptTags.row(appointment.id))
    }

    @Test
    fun noCareTeamRowOffersALongPress() {
        show {
            CareTeamScreen(
                people = listOf(person),
                onCall = {},
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertNoLongPress(CareTeamTags.person(person.id))
    }

    @Test
    fun noMedicationRowOffersALongPress() {
        show {
            MedicationsScreen(
                medications = listOf(medication),
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertNoLongPress(MedsTags.row(medication.id))
    }

    @Test
    fun noBillRowOffersALongPress() {
        show {
            MoneyScreen(bills = listOf(bill), onOpen = {}, onAdd = {}, onBack = {})
        }
        assertNoLongPress(MoneyTags.row(bill.id))
    }

    @Test
    fun noDocumentRowOffersALongPress() {
        show {
            DocumentsScreen(
                documents = listOf(document),
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertNoLongPress(DocTags.row(document.id))
    }

    @Test
    fun noProjectCardOffersALongPress() {
        show { ProjectsScreen(projects = listOf(project), onOpen = {}, onStart = {}) }
        assertNoLongPress(ProjectTags.row(project.id))
    }

    /**
     * **Both question states**, because they used to differ and the difference
     * was the defect: an asked question opened its sheet on a tap, and one
     * still waiting had no tap at all, so a long press was the only thing it
     * answered to.
     */
    @Test
    fun noQuestionRowOffersALongPress() {
        show {
            QuestionsScreen(
                questions = listOf(waiting, asked),
                onMarkAsked = {},
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertNoLongPress(QuestionTags.row(waiting.id))
        // Asked questions are folded away, and never hidden: the fold names
        // them and counts them. Opening it is part of walking this screen.
        compose.onNodeWithTag(QuestionTags.ASKED_FOLD).performScrollTo().performClick()
        assertNoLongPress(QuestionTags.row(asked.id))
    }

    @Test
    fun noStandingInstructionOffersALongPress() {
        val catalog = runBlocking { TemplateCatalog.instructions(context) }
        show {
            StandingInstructionsScreen(
                instructions = listOf(instruction),
                tags = catalog.tags,
                onOpen = {},
                onAdd = {},
                onBack = {},
            )
        }
        assertNoLongPress(InstructionTags.row(instruction.id))
    }

    // ---- half two: the visible path exists, and it works ----------------

    @Test
    fun aPersonIsRemovedFromTheirOwnScreen() {
        show {
            PersonScreen(
                person = person,
                entries = emptyList(),
                onCall = {},
                onSetPinned = {},
                onSetArchived = {},
                onEdit = {},
                onCapture = {},
                onRemove = { removals += 1 },
                onOpenEntry = {},
                onBack = {},
            )
        }
        assertRemovesOnce(PersonTags.REMOVE)
    }

    @Test
    fun aMedicationIsRemovedFromItsOwnScreen() {
        show {
            MedicationScreen(
                medication = medication,
                history = emptyList(),
                questions = emptyList(),
                onOpenQuestion = {},
                onEdit = {},
                onRemove = { removals += 1 },
                onRecordChange = {},
                onBack = {},
            )
        }
        assertRemovesOnce(MedicationTags.REMOVE)
    }

    @Test
    fun aBillIsRemovedFromItsOwnScreen() {
        show {
            BillScreen(
                bill = bill,
                onEdit = {},
                onRemove = { removals += 1 },
                onOpenChapter = {},
                onBack = {},
            )
        }
        assertRemovesOnce(BillTags.REMOVE)
    }

    @Test
    fun aDocumentIsRemovedFromItsOwnScreen() {
        show {
            DocumentScreen(
                document = document,
                onEdit = {},
                onRemove = { removals += 1 },
                onOpenChapter = {},
                onBack = {},
            )
        }
        assertRemovesOnce(OneDocTags.REMOVE)
    }

    /**
     * **A question still waiting has a face of its own**, and it is the state
     * that had nowhere to go at all before this. Marking it asked is the filled
     * action there; removing it is the quiet one.
     */
    @Test
    fun aWaitingQuestionIsRemovedFromItsSheet() {
        show {
            AnswerSheet(
                question = waiting,
                onSave = {},
                onMarkAsked = {},
                onRemove = { removals += 1 },
                onDismiss = {},
            )
        }
        assertSheetRemovesOnce(AnswerTags.REMOVE)
    }

    @Test
    fun anAskedQuestionIsRemovedFromItsSheet() {
        show {
            AnswerSheet(
                question = asked,
                onSave = {},
                onMarkAsked = {},
                onRemove = { removals += 1 },
                onDismiss = {},
            )
        }
        assertSheetRemovesOnce(AnswerTags.REMOVE)
    }

    /**
     * **The appointment is removed from its prep sheet**, which is the screen
     * the row opens. There is no other screen for an appointment, and the sheet
     * is the thing somebody is looking at when they decide it is not happening.
     */
    @Test
    fun anAppointmentIsRemovedFromItsPrepSheet() {
        show {
            PrepScreen(
                prep = Repository.Prep(
                    appointment = appointment,
                    questions = emptyList(),
                    changes = emptyList(),
                    sinceEdtf = null,
                ),
                onOpenEntry = {},
                onShare = {},
                onWriteUp = {},
                onOpenQuestion = {},
                onCorrect = {},
                onRemove = { removals += 1 },
                onBack = {},
            )
        }
        assertRemovesOnce(PrepTags.REMOVE)
    }

    /**
     * **A project is removed from the project**, not from the card in the list,
     * so the road, the papers and the people going with it are on screen when
     * the decision is made.
     */
    @Test
    fun aProjectIsRemovedFromItsOwnScreen() {
        show {
            ProjectHomeScreen(
                project = project,
                stages = emptyList(),
                standing = null,
                nextDate = null,
                latestWord = null,
                steps = emptyList(),
                papers = emptyList(),
                onRemove = { removals += 1 },
                onBack = {},
            )
        }
        assertRemovesOnce(ProjectHomeTags.REMOVE)
    }

    @Test
    fun aStandingInstructionIsRemovedFromItsSheet() {
        show {
            AcknowledgeSheet(
                instruction = instruction,
                onSave = {},
                onRemove = { removals += 1 },
                onDismiss = {},
            )
        }
        assertSheetRemovesOnce(AcknowledgeTags.REMOVE)
    }
}
