package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Digest
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CoachStep
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.TodayTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
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

    private fun show(
        hasAnything: Boolean = false,
        digest: Digest.Summary = Digest.nothing,
        coaching: List<CoachStep> = allSteps(),
        onOpenSection: (Repository.Section) -> Unit = {},
        locale: Locale = Locale.ENGLISH,
    ) {
        val strings = Strings.load(context, locale)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    TodayScreen(
                        hasAnything = hasAnything,
                        digest = digest,
                        coaching = coaching,
                        onOpenSection = onOpenSection,
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
    fun whatChangedIsShownAndEachSectionCanBeOpened() {
        var opened: Repository.Section? = null
        show(
            hasAnything = true,
            digest = Digest.Summary(
                added = listOf(Digest.Added(Repository.Section.TRAIL, 2)),
                corrected = 1,
                removed = 0,
            ),
            coaching = emptyList(),
            onOpenSection = { opened = it },
        )

        compose.onNodeWithTag(TodayTags.DIGEST).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.digestRow(Repository.Section.TRAIL)).performClick()
        assertEquals(Repository.Section.TRAIL, opened)
    }

    @Test
    fun aQuietWeekSaysSoRatherThanLeavingThePersonToInferIt() {
        // **This is the opposite of what this test asserted until 2026-08-03**,
        // and the change is deliberate.
        //
        // The screen used to show nothing at all when nothing had changed, on
        // the reasoning that a heading over "nothing changed" is a heading over
        // nothing. What that actually produced was a screen where the absence
        // of a line carried the meaning, which is ambiguous between "nothing
        // changed" and "the digest is not working" in exactly the way D49 and
        // D64 are about. **A person opening the app after two days away asked a
        // question, and the calm answer is still an answer.**
        //
        // `today.digest.empty` has been in all four catalogs since the digest
        // was built, written for this and never shown.
        show(hasAnything = true, digest = Digest.nothing, coaching = emptyList())
        val strings = Strings.load(context)
        compose.onNodeWithTag(TodayTags.DIGEST)
            .assertTextEquals(strings["today.digest.empty"])
    }

    @Test
    fun afirstRunHasNoDigestAtAll() {
        // Nothing has ever been written down, so "nothing new since you were
        // last here" would be true and useless. The coaching leads instead.
        show(hasAnything = false, digest = Digest.nothing, coaching = allSteps())
        compose.onNodeWithTag(TodayTags.DIGEST).assertIsNotDisplayed()
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
}
