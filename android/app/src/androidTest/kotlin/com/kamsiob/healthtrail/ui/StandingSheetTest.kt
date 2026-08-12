package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.screens.StandingSheet
import com.kamsiob.healthtrail.ui.screens.StandingTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recording where a project stands. `DESIGN.md` 20.5 screen 8, issue #281.
 *
 * **The sheet said "the date is today unless you change it" and nothing on it
 * changed the date.** That is the same defect the stage sheet carried until
 * `ac526b6`, on the sibling sheet of the same screen, and it was found by
 * opening the sheet rather than by reading it. What is held here is that the
 * date the sheet shows is the date that reaches the caller.
 */
@RunWith(AndroidJUnit4::class)
class StandingSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private var savedHolder: String? = null
    private var savedActivity: String? = null
    private var savedSince: Edtf.Date? = null

    private val previous = Repository.ProjectStanding(
        id = "st1",
        holderLabel = "The county",
        personId = null,
        organizationId = null,
        activity = "reviewing it",
        sinceEdtf = "2026-03",
        sinceStart = 1L,
        entryId = null,
        note = null,
    )

    private fun show(
        people: List<String> = listOf("The county", "The insurer"),
        previous: Repository.ProjectStanding? = null,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    StandingSheet(
                        people = people,
                        previous = previous,
                        onSave = { holder, activity, since ->
                            savedHolder = holder
                            savedActivity = activity
                            savedSince = since
                        },
                        onDismiss = {},
                    )
                }
            }
        }
    }

    /**
     * The control the lead has always promised exists, and it opens on today.
     *
     * Today by default is the whole point: somebody recording this five minutes
     * after the call should spend no taps on the date at all.
     */
    @Test
    fun theSheetCarriesADateAndItOpensOnToday() {
        show()
        val today = EventDateText.render(strings, Edtf.day(LocalDate.now()))
        compose.onNodeWithTag(StandingTags.WHEN).assertIsDisplayed()
        compose.onNodeWithText(today).assertIsDisplayed()
    }

    /** Whatever the sheet shows is what the caller is given, not the clock. */
    @Test
    fun theDateOnTheSheetIsTheDateThatIsSaved() {
        show()
        compose.onNodeWithTag(StandingTags.WHO).performTextInput("The review panel")
        compose.onNodeWithTag(StandingTags.SAVE).performScrollTo()
        compose.onNodeWithTag(StandingTags.SAVE).performClick()
        assertEquals("The review panel", savedHolder)
        assertNotNull("the date never reached the caller", savedSince)
        assertEquals(Edtf.day(LocalDate.now()), savedSince)
    }

    /**
     * Partial is a finished state, rule 13.
     *
     * Somebody who knows only that it is with the county can write that and
     * nothing else. The one thing the sheet cannot save is nothing at all.
     */
    @Test
    fun aHolderAloneIsEnoughToSave() {
        show()
        compose.onNodeWithTag(StandingTags.SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(StandingTags.WHO).performTextInput("The county")
        compose.waitForIdle()
        compose.onNodeWithTag(StandingTags.WHO).assertTextContains("The county", substring = true)
        // **Scrolled to rather than assumed visible.** On a shorter phone the
        // sheet's own Save sits below the fold, and a click at a node's centre
        // outside the viewport does nothing at all: the test read as "save did
        // not fire" when what was true was "a person could not reach it".
        // 2026-08-12, the day the test device changed.
        compose.onNodeWithTag(StandingTags.SAVE).performScrollTo()
        compose.onNodeWithTag(StandingTags.SAVE).assertIsEnabled()
        compose.onNodeWithTag(StandingTags.SAVE).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { savedHolder != null }
        assertEquals("The county", savedHolder)
        assertEquals("", savedActivity)
    }

    /**
     * The sheet opens on where it already stood, and the date still opens on
     * today.
     *
     * The previous standing's own date is when the project last changed hands,
     * which is the one answer that is almost certainly wrong for the change
     * being recorded now.
     */
    @Test
    fun theDateDoesNotInheritTheLastOneEvenThoughTheWordsDo() {
        show(previous = previous)
        compose.onNodeWithTag(StandingTags.SAVE).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { savedHolder != null }
        assertEquals("The county", savedHolder)
        assertEquals("reviewing it", savedActivity)
        assertEquals(Edtf.day(LocalDate.now()), savedSince)
    }
}
