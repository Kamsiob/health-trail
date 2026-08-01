package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.TodayScreen
import com.kamsiob.healthtrail.ui.screens.TodayTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
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

    private fun show(
        showCoaching: Boolean = true,
        hasAnything: Boolean = false,
        locale: Locale = Locale.ENGLISH,
    ) {
        val strings = Strings.load(context, locale)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    TodayScreen(showCoaching = showCoaching, hasAnything = hasAnything)
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
    fun aNotebookWithSomethingInItSaysTheDigestIsNotBuilt() {
        // D44: an interface may offer something it has not built, and it may
        // not go quiet about it. Faking a digest would be worse than either.
        show(showCoaching = false, hasAnything = true)
        compose.onNodeWithTag(TodayTags.INTERIM).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.EMPTY).assertIsNotDisplayed()
    }

    @Test
    fun theCoachingSurvivesTheFirstCall() {
        // The defect this replaced: tying the coaching to "has anything been
        // written" took the emergency card suggestion off the screen the
        // moment somebody logged their first call, before they had done it.
        // The most useful two minutes in the app disappeared for writing
        // something down, which is the opposite of the intended reward.
        show(showCoaching = true, hasAnything = true)

        compose.onNodeWithTag(TodayTags.INTERIM).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.step(1)).assertIsDisplayed()
    }

    @Test
    fun theInterimLineSaysNothingIsWaitingOnIt() {
        // The half that matters more. A person reading that a summary is being
        // built needs to know in the same breath that their records are kept
        // regardless, or the sentence reads as a reason to stop writing.
        val strings = Strings.load(context)
        val line = strings["today.digest.not_built"].lowercase()
        assertTrue("it does not say records are still kept: $line", "kept" in line)
        assertTrue("it does not say nothing waits on it: $line", "waiting on this" in line)
    }

    @Test
    fun itHoldsUpInTheLongestLanguage() {
        show(locale = Locale("es"))
        compose.onNodeWithTag(TodayTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(TodayTags.step(3)).assertIsDisplayed()
    }
}
