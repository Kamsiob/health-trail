package com.kamsiob.healthtrail.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.cardOffers
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the add-a-card gallery offers, and what each entry says. `DESIGN.md`
 * 21.6 screen 6, and issue #272.
 *
 * **The promise this file exists to keep is that a preview is true.** 21.6 asks
 * every entry to preview its small size with real current data, because a name
 * alone asks somebody to imagine a screen they have never seen: "Medications,
 * 6" is a decision they can make and "Medications" is a guess.
 *
 * **It was previewing nothing at all and nobody could see it.** The previews
 * were looked up from the answers of the cards already on Today, and the
 * gallery only ever offers the cards that are *not* on Today, so the lookup
 * could never hit: **every entry said "Nothing waiting"**, whatever the record
 * held. That is fourteen identical sentences about somebody's own notebook,
 * every one of them false, on the screen where they choose what to look at.
 *
 * **A pure function, so this is exhaustive rather than a walk.** The rungs a
 * gallery entry can land on are the card's own rungs, and checking them on the
 * phone would mean building six notebooks and removing cards by hand.
 */
@RunWith(AndroidJUnit4::class)
class AddCardOffersTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings get() = Strings.load(context)
    private val today = LocalDate.of(2026, 4, 4)

    private fun card(type: String, sourceId: String? = null) = Repository.TodayCard(
        id = "c-$type-${sourceId.orEmpty()}",
        type = type,
        size = "small",
        sortIndex = 0,
        isLead = false,
        sourceTable = sourceId?.let { "person" },
        sourceId = sourceId,
    )

    private fun offers(
        onScreen: List<Repository.TodayCard> = emptyList(),
        measures: List<Repository.Measure> = emptyList(),
        projects: List<Repository.Project> = emptyList(),
        answers: Map<String, Repository.TodayAnswer> = emptyMap(),
    ) = cardOffers(onScreen, measures, projects, answers, strings, today)

    private fun previewOf(type: String, answers: Map<String, Repository.TodayAnswer>): String? =
        offers(answers = answers).first { it.type == type }.preview

    @Test
    fun aCardWithACountPreviewsTheNumberAndWhatItCounted() {
        // The small size renders a number and the word under it, and this is
        // that. "6" alone makes the person supply the noun themselves.
        val said = previewOf(
            "medications",
            mapOf("medications" to Repository.TodayAnswer(count = 6)),
        )
        assertTrue("the number is missing: $said", "6" in said.orEmpty())
        assertTrue(
            "the word that says what it counted is missing: $said",
            strings["today.card.medications.count"] in said.orEmpty(),
        )
    }

    @Test
    fun anEmptyCardPreviewsItsOwnNothingRatherThanAGeneralOne() {
        // 21.4: the rung names what is not there. "Nothing scheduled" under
        // Next up also tells the person which door this is.
        //
        // **The marks are stripped rather than written into the expectation.**
        // `Bidi.join` isolates every part it is handed, which is correct and is
        // not what this test is about.
        assertEquals(
            strings["today.card.next_up.none"],
            previewOf("next_up", mapOf("next_up" to Repository.TodayAnswer(count = 0)))
                ?.filterNot { it == '⁨' || it == '⁩' },
        )
    }

    @Test
    fun aPreviewIsNullUntilTheAnswerHasBeenRead() {
        // **The row shows its name alone, which is honest.** The previews are
        // read when the sheet opens rather than on every Today focus, so there
        // is a moment with no answer, and a sentence about somebody's record
        // is the one thing that must not fill it.
        assertNull(previewOf("medications", emptyMap()))
    }

    @Test
    fun aProjectCardPreviewsTheWordTheCardWouldShowAndNotTheStoredValue() {
        // The defect this pins: a project whose status is a database value
        // previewed as "Nothing waiting", because the raw answer counts as
        // empty when its title is null. The card would have said "Waiting on
        // somebody". A preview that disagrees with its own card is worse than
        // no preview.
        val project = Repository.Project(
            id = "p-1",
            name = "Medicaid application",
            templateId = null,
            status = "waiting",
            waitingOn = null,
            notes = null,
            stepCount = 4,
            doneCount = 1,
            nextStep = null,
        )
        val said = offers(
            projects = listOf(project),
            answers = mapOf(
                "project_standing-p-1" to Repository.TodayAnswer(
                    sourceName = "Medicaid application",
                    detail = "waiting",
                ),
            ),
        ).first { it.type == "project_standing" }.preview

        assertTrue(
            "the preview does not say what the card would say: $said",
            strings["projects.status.waiting"] in said.orEmpty(),
        )
        assertTrue(
            "the preview does not name the project: $said",
            "Medicaid application" in said.orEmpty(),
        )
    }

    @Test
    fun aPreviewIsJoinedOnceAndNeverNested() {
        // `Bidi.join` isolates every part it is handed, so joining a joined
        // string wraps it twice and the row came out
        // `⁨Medicaid application⁩ · ⁨⁨4⁩ · ⁨steps in the plan⁩⁩`. The same defect
        // the cards had, arrived at from the other side.
        val project = Repository.Project(
            id = "p-1",
            name = "Medicaid application",
            templateId = null,
            status = "active",
            waitingOn = null,
            notes = null,
            stepCount = 4,
            doneCount = 1,
            nextStep = null,
        )
        val said = offers(
            projects = listOf(project),
            answers = mapOf(
                "project_steps-p-1" to Repository.TodayAnswer(
                    sourceName = "Medicaid application",
                    count = 4,
                    itemsSampleTheCount = false,
                ),
            ),
        ).first { it.type == "project_steps" }.preview.orEmpty()

        assertTrue(
            "the isolate marks are nested: $said",
            "⁨⁨" !in said && "⁩⁩" !in said,
        )
    }

    @Test
    fun aCardAlreadyOnTodayIsNotOfferedAgain() {
        val onScreen = listOf(card("medications"), card("money"))
        val types = offers(onScreen = onScreen).map { it.type }
        assertTrue("medications is offered twice", "medications" !in types)
        assertTrue("money is offered twice", "money" !in types)
        assertTrue("nothing else was offered", "next_up" in types)
    }

    @Test
    fun aCareTeamCardPointedAtOnePersonDoesNotStandInForTheRowOfEveryone() {
        // 21.7's two variants. Choosing a person on the only care team card
        // took the card itself out of the gallery, so there was no way back to
        // everybody and no way to a second person.
        val types = offers(onScreen = listOf(card("care_team", sourceId = "person-1")))
            .map { it.type }
        assertTrue("the row of everyone cannot be added back", "care_team" in types)
    }
}
