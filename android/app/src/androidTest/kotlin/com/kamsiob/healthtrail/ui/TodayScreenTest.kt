package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CoachStep
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.TodayHeroTags
import com.kamsiob.healthtrail.ui.screens.TodayTags
import com.kamsiob.healthtrail.ui.screens.TrackedMeasure
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Today, and the promise persona P1 makes about it.
 *
 * P1 is the person standing in a corridor on the day of an admission. One of
 * the five things that must be true for them is that **the empty Today coaches
 * rather than sitting blank, and its first suggestion is the Emergency Card.**
 * It failed that on 2026-08-01, which is how this screen came to be built.
 */
@RunWith(AndroidJUnit4::class)
class TodayScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** The three steps a brand new notebook still owes. */
    private fun allSteps(onOpen: (Repository.Section?) -> Unit = {}) = listOf(
        CoachStep("today.empty.step.1", Repository.Section.EMERGENCY_CARD) {
            onOpen(Repository.Section.EMERGENCY_CARD)
        },
        CoachStep("today.empty.step.2", Repository.Section.CARE_TEAM) {
            onOpen(Repository.Section.CARE_TEAM)
        },
        CoachStep("today.empty.step.3", null) { onOpen(null) },
    )

    /** A day the tests can name, so "tomorrow" is a fact rather than the clock. */
    private val theDay: LocalDate = LocalDate.of(2026, 8, 16)

    /** The appointment `m3v4-0` draws, with the two things the drawing needs. */
    private fun anAppointment(
        title: String = "Care plan meeting",
        where: String? = "Conference room, 2nd floor",
        personName: String? = "Maria Alvarez",
    ) = Repository.Appointment(
        id = "a1",
        title = title,
        scheduledEdtf = "2026-08-17T10:15",
        scheduledStart = 0L,
        locationNote = where,
        notes = null,
        personId = "p1",
        personName = personName,
    )

    /** A measure with a handful of readings, as the arranged fixture seeds one. */
    private fun aMeasure(
        name: String = "Weight",
        unit: String? = "lb",
        howMany: Int = 6,
    ) = TrackedMeasure(
        name = name,
        unit = unit,
        readings = (0 until howMany).map { index ->
            Repository.Reading(
                id = "r$index",
                measureId = "m1",
                number = 138.8 - index,
                text = null,
                unit = unit,
                occurredEdtf = null,
                // Newest first, as the repository returns them. February 2026
                // onward, so the card has a month to name at its foot.
                occurredStart = 1_770_000_000_000L + (howMany - index) * 86_400_000L,
                note = null,
                source = null,
            )
        },
    )

    private fun show(
        hasAnything: Boolean = false,
        digest: Digest.Summary = Digest.nothing,
        coaching: List<CoachStep> = allSteps(),
        onOpenSection: (Repository.Section) -> Unit = {},
        locale: Locale = Locale.ENGLISH,
        subjectName: String? = "Ruth",
        nextAppointment: Repository.Appointment? = null,
        questionsReady: Int = 0,
        tracked: TrackedMeasure? = null,
        openIncidents: Int = 0,
        openQuestions: Int = 0,
        waitingOnSomebody: Int = 0,
        unfiled: Int = 0,
        onOpenQuestions: () -> Unit = {},
        onOpenAppointments: () -> Unit = {},
        onOpenProgress: () -> Unit = {},
        onAddAppointment: () -> Unit = {},
        onAddQuestion: () -> Unit = {},
    ) {
        val strings = Strings.load(context, locale)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    TodayScreen(
                        hasAnything = hasAnything,
                        subjectName = subjectName,
                        today = theDay,
                        digest = digest,
                        coaching = coaching,
                        onOpenSection = onOpenSection,
                        nextAppointment = nextAppointment,
                        questionsReady = questionsReady,
                        tracked = tracked,
                        openIncidents = openIncidents,
                        openQuestions = openQuestions,
                        waitingOnSomebody = waitingOnSomebody,
                        unfiled = unfiled,
                        onOpenQuestions = onOpenQuestions,
                        onOpenAppointments = onOpenAppointments,
                        onOpenProgress = onOpenProgress,
                        onAddAppointment = onAddAppointment,
                        onAddQuestion = onAddQuestion,
                    )
                }
            }
        }
    }

    @Test
    fun anEmptyNotebookIsCoachedRatherThanBlank() {
        show()
        compose.onNodeWithTag(TodayTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.step(1)).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.step(2)).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.step(3)).assertIsDisplayed()
    }

    @Test
    fun theFirstStepIsAlwaysTheEmergencyCard() {
        // The whole reason the list is ordered. It is the highest value two
        // minutes a new person can spend, and it is the one thing in this app
        // that is useful to somebody else in a hurry.
        val strings = Strings.load(context)
        show()
        compose.onNodeWithText(strings["today.empty.step.1"]).assertIsDisplayed()
        assertTrue(
            "step one is not about the emergency card: ${strings["today.empty.step.1"]}",
            "emergency card" in strings["today.empty.step.1"].lowercase(),
        )
    }

    @Test
    fun everyStepGoesWhereItPoints() {
        // Rule 18. Telling somebody to fill in the emergency card and leaving
        // them to go and find it is a dead end wearing a suggestion.
        var opened: Repository.Section? = null
        show(coaching = allSteps { opened = it })

        compose.onNodeWithTag(TodayTags.step(1)).performClick()
        assertEquals(Repository.Section.EMERGENCY_CARD, opened)

        compose.onNodeWithTag(TodayTags.step(2)).performClick()
        assertEquals(Repository.Section.CARE_TEAM, opened)
    }

    @Test
    fun aStepAlreadyTakenIsNotSuggested() {
        // The defect: a notebook with two people on the care team and four
        // logged calls was still being told to add people and log a call.
        show(hasAnything = true, coaching = allSteps().take(1))

        compose.onNodeWithTag(TodayTags.step(1)).assertIsDisplayed()
        assertTrue(
            "a step that has already been taken is still being suggested",
            compose.onAllNodesWithTag(TodayTags.step(2)).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aNotebookWithNothingLeftToSuggestShowsNoCoachingAtAll() {
        // An empty list is a finished state. Inventing a fourth suggestion to
        // fill the space would be the app keeping score, which rule 13 forbids.
        show(hasAnything = true, coaching = emptyList())
        compose.onNodeWithTag(TodayTags.EMPTY).assertIsNotDisplayed()
    }

    @Test
    fun theEmptyStateReadsAsAnInvitationRatherThanAnAbsence() {
        // Section 5.10, and rule 13. It must never read as a list of things the
        // person has failed to do.
        val strings = Strings.load(context)
        val copy = listOf(
            strings["today.empty.title"],
            strings["today.empty.step.1"],
            strings["today.empty.step.2"],
            strings["today.empty.step.3"],
        ).joinToString(" ").lowercase()

        listOf(
            "you have not", "you haven't", "missing", "incomplete", "empty",
            "should", "must", "need to", "failed", "%",
        ).forEach { banned ->
            assertTrue(
                "the empty state scolds or keeps score: $banned appears in $copy",
                banned !in copy,
            )
        }
    }

    @Test
    fun theEmergencyCardIsNotOfferedTwiceOnTheSameScreen() {
        // While the coaching is still asking for it, the persistent button
        // would be the same offer a second time, in fewer words.
        show(coaching = allSteps())
        compose.onNodeWithTag(TodayTags.EMERGENCY).assertIsNotDisplayed()
    }

    @Test
    fun theEmergencyCardStaysOneTapAwayOnceItIsFilledIn() {
        // MASTER_SPEC 4.1 keeps it one tap from this screen, because it is the
        // one thing here useful to somebody else in a hurry.
        show(hasAnything = true, coaching = emptyList())
        compose.onNodeWithTag(TodayTags.EMERGENCY).assertIsDisplayed()
    }

    @Test
    fun itHoldsUpInTheLongestLanguage() {
        show(locale = Locale("es"))
        compose.onNodeWithTag(TodayTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.step(3)).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------
    // What `m3v4-0` draws. #386.
    // ---------------------------------------------------------------------

    @Test
    fun theMastheadSaysWhoseDayItIs() {
        // D170. The screen opened with the word "Today" over a navigation tab
        // an inch below it saying the same word.
        show(hasAnything = true, coaching = emptyList())
        val strings = Strings.load(context)
        // **Isolated, because a name in a sentence is a name.** The marks are
        // invisible to a person and are part of the string, so the expectation
        // carries them rather than the screen dropping them.
        compose.onNodeWithText(
            strings("today.masthead", "name" to Bidi.isolate("Ruth")),
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun theDateIsCapitalsForTheEyeAndWordsForAReader() {
        // D183. Uppercasing changes the string the semantics tree carries too,
        // and a reader announcing "THURSDAY, AUGUST 16" is being handed a shape
        // rather than a date.
        show(hasAnything = true, coaching = emptyList())
        val strings = Strings.load(context)
        val spoken = EventDateText.masthead(strings, theDay)
        assertTrue(
            "the date is announced as capitals rather than as a date",
            spoken != spoken.uppercase(Locale.ENGLISH),
        )
        compose.onNodeWithContentDescription(spoken).assertExists()
    }

    @Test
    fun theNextAppointmentLeadsAndSaysWhereItIs() {
        // The lead of the drawing, and the location is the half of it that was
        // in the schema and on no screen.
        show(hasAnything = true, coaching = emptyList(), nextAppointment = anAppointment())
        compose.onNodeWithTag(TodayHeroTags.ROOT).assertIsDisplayed()
        // **Assert on the words, not the tag.** A merged node's testTag passes
        // whether or not the line inside it was drawn at all.
        compose.onNodeWithText("Care plan meeting", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Conference room, 2nd floor", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun anAppointmentTomorrowSaysTomorrowRatherThanADate() {
        show(hasAnything = true, coaching = emptyList(), nextAppointment = anAppointment())
        val strings = Strings.load(context)
        // The pill is capitals on screen and natural words for a reader, so the
        // assertion is on the description.
        compose.onNodeWithContentDescription(strings["date.tomorrow"], substring = true)
            .assertExists()
    }

    @Test
    fun anAppointmentWithNoPlaceWrittenDownIsStillAWholeAppointment() {
        // Rule 13: an unfilled slot reads as "not yet" and never as an error.
        // Nothing appears where the place would be, and nothing scolds.
        show(
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = anAppointment(where = null),
        )
        compose.onNodeWithText("Care plan meeting", substring = true).assertIsDisplayed()
    }

    @Test
    fun theQuestionsSavedForItAreCountedInsideTheBlockAndOpenTheList() {
        var opened = false
        show(
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = anAppointment(),
            questionsReady = 2,
            onOpenQuestions = { opened = true },
        )
        val strings = Strings.load(context)
        compose.onNodeWithText(strings("today.next.questions.short", "count" to 2))
            .assertIsDisplayed()
        compose.onNodeWithText("2").assertIsDisplayed()
        // **The card speaks as one sentence**, so it is found by what a reader
        // hears rather than by the words split across the disc and the label.
        compose.onNodeWithContentDescription(strings("today.next.questions", "count" to 2))
            .performClick()
        assertTrue("the card inside the block is a door and it did nothing", opened)
    }

    @Test
    fun nothingSavedToAskMeansNoCardRatherThanAnEmptyOne() {
        // Rule 11.5: announcing the absence of a problem is not information, and
        // a white frame around nothing is exactly the blank area rule 11 bans.
        show(
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = anAppointment(),
            questionsReady = 0,
        )
        compose.onNodeWithTag(TodayHeroTags.QUESTIONS).assertIsNotDisplayed()
    }

    @Test
    fun nothingAheadKeepsTheBlockAndOffersTheTwoWaysIn() {
        // D192: the hero is permanent. An empty calendar is a finished state,
        // rule 13, so it says so and offers both quick adds rather than
        // vanishing and leaving the screen to start with whatever is next.
        show(hasAnything = true, coaching = emptyList(), nextAppointment = null)
        val strings = Strings.load(context)
        compose.onNodeWithTag(TodayHeroTags.ROOT).assertIsDisplayed()
        compose.onNodeWithText(strings["today.next.none"]).assertIsDisplayed()
        compose.onNodeWithText(strings["appts.add"]).assertIsDisplayed()
        compose.onNodeWithText(strings["questions.add"]).assertIsDisplayed()
    }

    @Test
    fun theQuickWaysInGoWhereTheySay() {
        var appointment = false
        var question = false
        show(
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = null,
            onAddAppointment = { appointment = true },
            onAddQuestion = { question = true },
        )
        val strings = Strings.load(context)
        compose.onNodeWithText(strings["appts.add"]).performClick()
        compose.onNodeWithText(strings["questions.add"]).performClick()
        assertTrue("the calendar shortcut did nothing", appointment)
        assertTrue("the question shortcut did nothing", question)
    }

    @Test
    fun theBlockIsADoorIntoTheAppointment() {
        var opened = false
        show(
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = anAppointment(),
            onOpenAppointments = { opened = true },
        )
        compose.onNodeWithText("Care plan meeting", substring = true).performClick()
        assertTrue("the one saturated block on the screen did nothing on press", opened)
    }

    @Test
    fun theTrackedMeasureShowsItsLatestValueAndCountsTheRest() {
        show(hasAnything = true, coaching = emptyList(), tracked = aMeasure())
        val strings = Strings.load(context)
        compose.onNodeWithTag(TodayTags.TRACK).assertIsDisplayed()
        compose.onNodeWithText(strings["progress.heading"]).assertIsDisplayed()
        compose.onNodeWithText("138.8", substring = true).assertIsDisplayed()
        compose.onNodeWithText(strings("progress.readings", "count" to 6)).assertIsDisplayed()
    }

    @Test
    fun theMeasureKeepsTheCaseSomebodyTypedRatherThanShouting() {
        // D183. A measure is named by the person, so it drops the eyebrow's
        // capitals: capitals cost about fifteen percent of the width and the
        // result was their own words cut off.
        show(
            hasAnything = true,
            coaching = emptyList(),
            tracked = aMeasure(name = "Blood pressure, sitting"),
        )
        compose.onNodeWithText("Blood pressure, sitting", substring = true).assertExists()
    }

    @Test
    fun aMeasureWithNothingInItKeepsItsCardAndSaysSoPlainly() {
        // Rule 13: partial is a finished state, and an empty slot never reads as
        // a deficiency. The card keeps the measure's name and says what is true.
        show(
            hasAnything = true,
            coaching = emptyList(),
            tracked = aMeasure(howMany = 0),
        )
        val strings = Strings.load(context)
        compose.onNodeWithTag(TodayTags.TRACK).assertIsDisplayed()
        compose.onNodeWithText(strings["progress.empty"], substring = true).assertExists()
    }

    @Test
    fun theTrackedCardAndItsHeadingBothOpenProgress() {
        // Rule 18: a number that leads nowhere is a dead end wearing a value.
        var opened = 0
        show(
            hasAnything = true,
            coaching = emptyList(),
            tracked = aMeasure(),
            onOpenProgress = { opened += 1 },
        )
        compose.onNodeWithTag(TodayTags.TRACK_OPEN).performClick()
        compose.onNodeWithTag(TodayTags.TRACK).performClick()
        assertEquals(2, opened)
    }

    @Test
    fun nothingTrackedMeansNoCardAtAll() {
        show(hasAnything = true, coaching = emptyList(), tracked = null)
        compose.onNodeWithTag(TodayTags.TRACK).assertIsNotDisplayed()
    }

    @Test
    fun everythingStillOpenIsOnTheScreenAtOnceWithNothingBehindAFold() {
        // D185. This was one row and a fold called "Also waiting" carrying a
        // count, and a fold is a label over a thing the person cannot see.
        show(
            hasAnything = true,
            coaching = emptyList(),
            openIncidents = 1,
            openQuestions = 2,
            waitingOnSomebody = 3,
            unfiled = 4,
        )
        val strings = Strings.load(context)
        compose.onNodeWithText(strings("today.open.incidents", "count" to 1)).assertIsDisplayed()
        compose.onNodeWithText(strings("today.open.questions", "count" to 2)).assertIsDisplayed()
        compose.onNodeWithText(strings("today.open.waiting", "count" to 3)).assertIsDisplayed()
        compose.onNodeWithText(strings("unfiled.waiting", "count" to 4)).assertIsDisplayed()
    }

    @Test
    fun nothingOpenMeansNoGroupRatherThanAHeadingOverNothing() {
        show(hasAnything = true, coaching = emptyList())
        compose.onNodeWithTag(TodayTags.OPEN_GROUP).assertIsNotDisplayed()
    }

    @Test
    fun whatNeedsSomebodySitsAboveWhatIsMerelyTracked() {
        // D191. The drawing is of a quiet notebook, so it shows the appointment
        // and then the measure. When something is open it comes between them:
        // an incident nobody has answered is the thing the person is carrying
        // around, and a weight is not.
        show(
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = anAppointment(),
            tracked = aMeasure(),
            openIncidents = 1,
        )
        val appointment = compose.onNodeWithTag(TodayHeroTags.ROOT)
            .fetchSemanticsNode().boundsInRoot.top
        val open = compose.onNodeWithTag(TodayTags.OPEN_GROUP)
            .fetchSemanticsNode().boundsInRoot.top
        val track = compose.onNodeWithTag(TodayTags.TRACK)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue("the appointment does not lead", appointment < open)
        assertTrue("an open incident sits below a weight", open < track)
    }

    @Test
    fun theWholeDrawingHoldsUpInTheLongestLanguage() {
        // Rule 11's longest-language state, for the two blocks the drawing adds.
        // Spanish is the longest of the four catalogs.
        show(
            locale = Locale("es"),
            hasAnything = true,
            coaching = emptyList(),
            nextAppointment = anAppointment(
                title = "Reunión del plan de cuidados con el equipo completo",
            ),
            questionsReady = 2,
            tracked = aMeasure(name = "Presión arterial, sentada"),
        )
        compose.onNodeWithTag(TodayHeroTags.ROOT).assertIsDisplayed()
        compose.onNodeWithTag(TodayHeroTags.QUESTIONS).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.TRACK).assertExists()
    }
}
