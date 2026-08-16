package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.components.DatePickerTags
import java.time.LocalDate
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.kamsiob.healthtrail.ui.screens.CaptureFormState
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureFormTags
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.RoughWhen
import com.kamsiob.healthtrail.ui.screens.entryKind
import com.kamsiob.healthtrail.ui.screens.edtf
import com.kamsiob.healthtrail.ui.screens.usesTheSharedForm
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capture, which is the only way data enters this app.
 *
 * The property that matters is not that the form saves. It is that **a blank
 * call still saves**. Someone who hangs up and taps the gold button has already
 * done the useful thing, which is recording that a call happened and when. An
 * app that refuses to write that down until a field is filled has failed the
 * person it was built for.
 */
@RunWith(AndroidJUnit4::class)
class CaptureTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private val threads = listOf(
        Repository.CareThread(id = "t-nursing", label = "Nursing", colorIndex = 0),
        Repository.CareThread(id = "t-discharge", label = "Discharge planning", colorIndex = 1),
    )

    private fun showForm(
        kind: CaptureKind = CaptureKind.CALL,
        threads: List<Repository.CareThread> = this.threads,
        onSave: (CaptureDraft) -> Unit,
        onCancel: () -> Unit = {},
    ) {
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    // **Hoisted here exactly as `NotebookShell` hoists it**,
                    // so this test drives the form the way the app does rather
                    // than through a shape only a test would produce. The draft
                    // outliving the form is the whole reason the state is up
                    // here, and a test that stubbed it would prove a form that
                    // does not ship.
                    var state by rememberSaveable(stateSaver = CaptureFormState.Saver) {
                        mutableStateOf(CaptureFormState())
                    }
                    CaptureFormScreen(
                        kind = kind,
                        threads = threads,
                        state = state,
                        onStateChange = { state = it },
                        onSave = onSave,
                        onCancel = onCancel,
                    )
                }
            }
        }
    }

    /**
     * Walks to the stage holding a control, the way a person does.
     *
     * **Capture became a staged conversation on 2026-08-04**, per law 3, and
     * every test here was written when its four questions were on one scroll.
     * The controls did not move; what changed is that one question is on screen
     * at a time and the rest are one tap away.
     *
     * **It taps the same skip a person taps.** A test that reached in and set
     * the stage would prove a screen nobody can use.
     */
    private fun toStage(stage: Int) {
        repeat(stage) {
            compose.onNodeWithTag(CaptureFormTags.NEXT).performClick()
            // **Each stage is waited for rather than assumed.** The forward
            // control is the same node on every stage, so clicking it twice in
            // a row can land both taps before the second stage has composed,
            // and the next `performTextInput` then fails with "Failed to
            // perform text input" on a field that is on its way in. Seen in a
            // full suite on 2026-08-12 and never when the class runs alone.
            compose.waitForIdle()
        }
    }

    /**
     * Where each question sits **for a call**, which is what this class shows.
     *
     * **A call asks who first now**, #379: "it should start off with who did
     * you speak to and then notes about the call ... and then the date." Every
     * other kind keeps note, when, who, and `CaptureKind.slots` is the one
     * place that order is decided.
     */
    private val WHO_STAGE = 0
    private val NOTE_STAGE = 1
    private val WHEN_STAGE = 2

    /**
     * The same three questions in the other order, for every kind that is not
     * a call. A visit or an incident still opens on the note, because that is
     * the sentence somebody came to write.
     */
    private fun whoStageFor(kind: CaptureKind) = if (kind == CaptureKind.CALL) 0 else 2

    private fun noteStageFor(kind: CaptureKind) = if (kind == CaptureKind.CALL) 1 else 0

    @Test
    fun aBlankCallStillSaves() {
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertNotNull("saving a blank call did nothing", draft)
        assertEquals("", draft!!.who)
        assertEquals("", draft.note)
    }

    @Test
    fun theRoughDateIsAskedWithChipsAndOneOfThemIsNotKnowing() {
        // Screen 26. The chips answer in one tap and one of them is not
        // knowing, so nobody is made to name a day they do not have.
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        toStage(WHEN_STAGE)

        compose.onNodeWithTag(CaptureFormTags.whenChip(RoughWhen.NOT_SURE)).performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(Edtf.Precision.UNKNOWN, draft!!.occurred.precision)
    }

    @Test
    fun anExactDateIsAPeerOfTheChipsRatherThanBehindThem() {
        // Section 10.9. Someone logging a call from three months ago, or who
        // knows the minute, is a normal case rather than an edge one, and
        // making them exhaust the chips first would say otherwise.
        showForm(onSave = {})

        toStage(WHEN_STAGE)
        compose.onNodeWithTag(CaptureFormTags.EXACT).assertIsDisplayed()
    }

    @Test
    fun pickingADayReplacesTheChipAnswerRatherThanJoiningIt() {
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        toStage(WHEN_STAGE)

        compose.onNodeWithTag(CaptureFormTags.EXACT).performClick()
        compose.onNodeWithTag(DatePickerTags.day(14)).performClick()
        compose.onNodeWithTag(DatePickerTags.CONFIRM).performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        // A day, not a moment: the time is a second act and was not taken.
        assertEquals(Edtf.Precision.DAY, draft!!.occurred.precision)
        assertEquals(14, java.time.LocalDate.parse(draft!!.occurred.canonical).dayOfMonth)
    }

    @Test
    fun theWholeMonthIsAnAnswerInItsOwnRight() {
        // The coarse answer must not feel like the failure case. For a record
        // written from memory it is usually the true one.
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        toStage(WHEN_STAGE)
        val month = java.time.YearMonth.now()

        compose.onNodeWithTag(CaptureFormTags.EXACT).performClick()
        compose.onNodeWithTag(DatePickerTags.month(month.monthValue)).performClick()
        compose.onNodeWithTag(DatePickerTags.CONFIRM).performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(Edtf.Precision.MONTH, draft!!.occurred.precision)
        assertEquals("%04d-%02d".format(month.year, month.monthValue), draft!!.occurred.canonical)
    }

    @Test
    fun theCalendarNeverPreselectsToday() {
        // A picker that preselects today turns every mistap into a claim, in a
        // record somebody may rely on years later. Confirming without touching
        // anything answers "not sure" rather than "today".
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        toStage(WHEN_STAGE)

        compose.onNodeWithTag(CaptureFormTags.whenChip(RoughWhen.NOT_SURE)).performClick()
        compose.onNodeWithTag(CaptureFormTags.EXACT).performClick()
        compose.onNodeWithTag(DatePickerTags.CONFIRM).performClick()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(Edtf.Precision.UNKNOWN, draft!!.occurred.precision)
    }

    @Test
    fun notSureStoresUnknownRatherThanTodayWithAShrug() {
        // The property that matters more than which chip was tapped. An entry
        // whose time is unknown must not carry a real looking timestamp, or
        // every screen downstream renders a precision the person never had.
        val today = LocalDate.of(2024, 11, 18)

        assertEquals(Edtf.UNKNOWN, RoughWhen.NOT_SURE.edtf(today).canonical)
        assertEquals(Edtf.Precision.UNKNOWN, RoughWhen.NOT_SURE.edtf(today).precision)

        // And each of the others says exactly as much as the chip claimed. A
        // chip reading "Today" must not store the minute the button was tapped,
        // because nobody said the minute.
        assertEquals("2024-11-18", RoughWhen.TODAY.edtf(today).canonical)
        assertEquals(Edtf.Precision.DAY, RoughWhen.TODAY.edtf(today).precision)
        assertEquals("2024-11-17", RoughWhen.YESTERDAY.edtf(today).canonical)
        assertEquals(Edtf.Precision.WEEK, RoughWhen.THIS_WEEK.edtf(today).precision)
    }

    @Test
    fun everyWayInEitherWorksOrSaysWhyNot() {
        // The sheet offers six ways in. Choosing one used to close the sheet
        // and silently do nothing for the one that is not built, which is the
        // app appearing to lose what somebody tried to save. Whatever is
        // unbuilt must say so, and it must say so in words rather than by
        // going quiet.
        val strings = Strings.load(context)
        CaptureKind.entries.forEach { kind ->
            val handled = kind.usesTheSharedForm ||
                kind == CaptureKind.MEASUREMENT ||
                kind == CaptureKind.DOCUMENT
            assertTrue(
                "$kind is offered on the sheet and nothing happens when it is chosen",
                handled,
            )
        }
        // And the one that is not built says why, rather than only that it is
        // not built, because "not built" on its own reads as neglect.
        val why = strings["capture.not_built"]
        assertTrue("the unbuilt path does not explain itself: $why", why.length > 40)
    }

    @Test
    fun theThreadDefaultsToNotKnowingAndSaysWhereThatGoes() {
        // Not knowing is the honest default for someone who just tapped save,
        // and the screen has to say where the entry is going while they can
        // still change it. The app never files anything on its own.
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        toStage(WHO_STAGE)

        compose.onNodeWithTag(CaptureFormTags.UNFILED_NOTE).assertIsDisplayed()
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertNull("a thread was chosen for the person", draft!!.threadId)
    }

    @Test
    fun choosingAThreadFilesItAndTakesTheUnfiledNoteAway() {
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        toStage(WHO_STAGE)

        // **The thread question is behind "Add more" as of 2026-08-03**, per
        // `DESIGN.md` 5.11.1 and the disclosure, so the test opens it the way a
        // person does rather than reaching past the control.
        compose.onNodeWithTag(CaptureFormTags.MORE).performScrollTo().performClick()
        compose.onNodeWithTag(CaptureFormTags.threadChip("t-discharge"))
            .performScrollTo()
            .performClick()
        assertTrue(
            "the unfiled note stayed after a thread was chosen",
            compose.onAllNodesWithTag(CaptureFormTags.UNFILED_NOTE)
                .fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()
        assertEquals("t-discharge", draft!!.threadId)
    }

    @Test
    fun aNotebookWithNoThreadsIsNotAskedAQuestionItCannotAnswer() {
        // The empty state of the thread question. A notebook where the person
        // answered "not sure yet" to the situation picker has no threads, and a
        // question whose only answer is "not sure yet" is not a question.
        var draft: CaptureDraft? = null
        showForm(threads = emptyList(), onSave = { draft = it })

        assertTrue(
            "the thread question was asked with nothing to answer it",
            compose.onAllNodesWithTag(CaptureFormTags.THREAD_UNSURE)
                .fetchSemanticsNodes().isEmpty(),
        )
        // And the control that would have opened it is not there either. A
        // disclosure offering nothing is an empty room to walk into, which is
        // the same defect as an empty section, per rule 11.
        assertTrue(
            "an empty disclosure was offered on a notebook with nothing in it",
            compose.onAllNodesWithTag(CaptureFormTags.MORE)
                .fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            "the unfiled note showed on a notebook with no threads",
            compose.onAllNodesWithTag(CaptureFormTags.UNFILED_NOTE)
                .fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()
        assertNotNull("saving without threads did nothing", draft)
    }

    @Test
    fun anEntryFiledUnderAThreadIsReadableAsSuch() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Thread subject")
        repository.applySituation(
            subjectId = subjectId,
            templateId = "hospital_stay",
            threads = listOf("nursing" to "Nursing"),
        )

        val thread = repository.threads(subjectId).single()
        val entryId = repository.createEntry(subjectId = subjectId, kind = "visit")
        repository.linkEntryToThread(entryId, thread.id)

        assertTrue("the thread was not created", thread.label == "Nursing")
        assertTrue("the entry was not written", entryId.isNotBlank())
    }

    @Test
    fun whatIsTypedIsWhatIsSaved() {
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        // **What the note stage holds, then what the who stage holds.** Both
        // survive the walk between them, which is the thing worth asserting
        // now that they are two screens rather than two fields.
        // **In the order a call now asks them**, who first: `toStage` walks
        // forward through the stages and cannot go back, which is honest about
        // how somebody moves through the form.
        toStage(WHO_STAGE)
        compose.onNodeWithTag(CaptureFormTags.WHO).performTextInput("Ward desk")
        toStage(NOTE_STAGE)
        compose.onNodeWithTag(CaptureFormTags.NOTE).performTextInput("Said they would call back")
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals("Ward desk", draft!!.who)
        assertEquals("Said they would call back", draft.note)
    }

    @Test
    fun cancelingSavesNothing() {
        var draft: CaptureDraft? = null
        var canceled = false
        showForm(onSave = { draft = it }, onCancel = { canceled = true })

        toStage(WHO_STAGE)
        compose.onNodeWithTag(CaptureFormTags.WHO).performTextInput("Typed then abandoned")
        compose.onNodeWithTag(CaptureFormTags.CANCEL).performClick()

        assertTrue(canceled)
        assertNull("canceling still saved something", draft)
    }

    @Test
    fun aSavedCallLandsOnTheTrailAndIsCounted() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Capture subject")

        val before = repository.count(Repository.Section.TRAIL, subjectId)
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Ward desk",
            body = "Said they would call back",
        )
        repository.addCallDetail(entryId = entryId, reached = true)
        val after = repository.count(Repository.Section.TRAIL, subjectId)

        assertEquals("the call did not reach the trail", before + 1, after)
    }

    @Test
    fun aBlankCallIsStillARealEntryOnTheTrail() = runBlocking {
        // The whole point. An entry with no words is still a record that
        // something happened at a time, which is often the only thing the
        // person had a free hand to capture.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Blank capture subject")

        val before = repository.count(Repository.Section.TRAIL, subjectId)
        repository.createEntry(subjectId = subjectId, kind = "call", title = "", body = "")
        val after = repository.count(Repository.Section.TRAIL, subjectId)

        assertEquals("a blank call was silently dropped", before + 1, after)
    }

    @Test
    fun everyFormKindSavesUnderItsOwnWords() {
        // One form serves four kinds, so the thing that can silently break is
        // the kind traveling with the draft. A visit filed as a call is a
        // wrong record rather than a missing one, which is worse.
        val kind = CaptureKind.VISIT
        var draft: CaptureDraft? = null
        showForm(kind = kind, onSave = { draft = it })

        toStage(whoStageFor(kind))
        compose.onNodeWithTag(CaptureFormTags.WHO).performTextInput("Dr Aurelio")
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(kind, draft!!.kind)
        assertEquals("Dr Aurelio", draft.who)
    }

    @Test
    fun everyKindTheFormServesNamesItselfInTheCatalog() {
        // The form builds its catalog keys from the enum rather than reading
        // them from a table, so a kind added to the form without its words
        // would render a raw key on screen. This is the check that makes
        // building keys safe, and it reads the served set from the same
        // declaration the shell does rather than repeating the list.
        val strings = Strings.load(context)
        val served = CaptureKind.entries.filter { it.usesTheSharedForm }
        assertTrue("no kind uses the shared form", served.isNotEmpty())

        served.forEach { kind ->
            val slug = kind.name.lowercase()
            listOf("title", "who", "who.hint", "note", "note.hint").forEach { slot ->
                // Strings throws on a key no catalog defines, so a missing one
                // fails here by name rather than reaching a person as a key on
                // screen.
                val value = strings["capture.$slug.$slot"]
                assertTrue("capture.$slug.$slot resolved to nothing", value.isNotBlank())
            }
        }
    }

    @Test
    fun everyKindHasARowKind() {
        // The schema column, not the label. Every kind must name what it is
        // stored as, and no two may collide, or the trail cannot tell them
        // apart after the fact.
        val kinds = CaptureKind.entries.map { it.entryKind() }
        assertEquals("two capture kinds store as the same row kind", kinds.size, kinds.toSet().size)
        assertTrue("a capture kind stores as a blank", kinds.none { it.isBlank() })
    }

    @Test
    fun anEntryWithAnUnknownTimeIsStillAccepted() = runBlocking {
        // Rough dates are a functional requirement. A person writing at 11pm
        // about a call three days ago does not know the time, and demanding one
        // gets either a guess recorded as fact or nothing recorded at all.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Rough date subject")

        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Sometime last week",
            occurred = Edtf.unknown(),
        )
        assertTrue("an entry with no date was rejected", id.isNotBlank())

        // And it comes back as unknown rather than as a date, which is the half
        // that a nullable timestamp column could never promise.
        val stored = repository.entryOccurred(id)
        assertEquals(Edtf.UNKNOWN, stored?.canonical)
        assertEquals(Edtf.Precision.UNKNOWN, stored?.precision)
    }
}
