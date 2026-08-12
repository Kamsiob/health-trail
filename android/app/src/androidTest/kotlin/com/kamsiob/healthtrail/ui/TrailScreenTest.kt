package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.TrailScreen
import com.kamsiob.healthtrail.ui.screens.TrailTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.time.ZoneId
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The trail, and what each row says about when it happened.
 *
 * **The most read screen in the app had no test file of its own**, which
 * `docs/TRAPS.md` section 5 names as its own defect: whatever a screen carries
 * is unprotected until something asserts it. This is the first of them and it
 * covers the thing #361 changed.
 *
 * **A row under a month band says the weekday and the day.** It used to repeat
 * the month and the year the band above it had just given, on every row, which
 * on a real June is forty three rows each beginning "June … 2026". That is most
 * of the ink on the screen and none of it distinguishes anything.
 *
 * **What must not shorten is the interesting half.** A search result and a
 * pinned entry both float free of the month structure, and a date coarser than
 * a day would be given a precision the person never offered, which rule 17
 * forbids outright.
 */
@RunWith(AndroidJUnit4::class)
class TrailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val zone: ZoneId = ZoneId.of("UTC")

    /** 2026-06-29 is a Monday, and 2026-06-24 a Wednesday. */
    private fun entry(
        id: String,
        title: String,
        edtf: String?,
        start: Long?,
        pinnedAt: Long? = null,
        kind: String = "call",
    ) = Repository.TrailEntry(
        id = id,
        kind = kind,
        title = title,
        body = null,
        occurredEdtf = edtf,
        occurredStart = start,
        createdAt = 1_782_000_000_000,
        isUnfiled = false,
        threads = emptyList(),
        pinnedAt = pinnedAt,
    )

    private val monday = entry("e1", "Care plan meeting", "2026-06-29", 1_782_691_200_000)
    private val wednesday = entry("e2", "Asked about the dressing", "2026-06-24", 1_782_259_200_000)

    private fun show(entries: List<Repository.TrailEntry>) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    TrailScreen(
                        entries = entries,
                        onOpen = {},
                        onReview = {},
                        onBack = {},
                        zone = zone,
                    )
                }
            }
        }
    }

    @Test
    fun theBandNamesTheMonthOnceAndTheRowsDoNotRepeatIt() {
        show(listOf(monday, wednesday))

        compose.onNodeWithText("June 2026").assertIsDisplayed()
        compose.onNodeWithText("Monday 29", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Wednesday 24", substring = true).assertIsDisplayed()
        // The full form is what it said before, on every row, under a band that
        // had already said it.
        compose.onNodeWithText("June 29, 2026", substring = true).assertDoesNotExist()
        compose.onNodeWithText("June 24, 2026", substring = true).assertDoesNotExist()
    }

    /** The row still says what kind of thing it was, which is the other half. */
    @Test
    fun theRowStillSaysWhatItWas() {
        show(listOf(monday))
        compose.onNodeWithText("Monday 29 · A call").assertIsDisplayed()
    }

    /**
     * **A date coarser than a day keeps its whole form and its hedge.** The
     * person said "sometime in June" and no band above them turns that into a
     * weekday. Rule 17.
     */
    @Test
    fun aCoarseDateIsNeverGivenADayItDoesNotHave() {
        show(listOf(entry("e3", "The letter arrived", "2026-06", 1_782_000_000_000)))
        compose.onNodeWithText("Sometime in June 2026", substring = true).assertIsDisplayed()
    }

    /**
     * **A search result floats free of the months**, so it says its whole date.
     * "Monday 29" in a list that spans five years is a date nobody can place.
     */
    @Test
    fun aSearchResultSaysItsWholeDate() {
        show(List(14) { index -> entry("s$index", "Called the ward $index", "2026-06-29", 1_782_691_200_000) })

        compose.onNodeWithTag(TrailTags.SEARCH).performTextInput("ward 3")
        compose.onNodeWithText("June 29, 2026", substring = true).assertIsDisplayed()
    }

    /** Pinned entries sit above time itself, so each carries its own whole date. */
    @Test
    fun aPinnedEntrySaysItsWholeDate() {
        show(listOf(monday, entry("p1", "The letter to quote", "2026-06-24", 1_782_259_200_000, pinnedAt = 1L)))
        compose.onNodeWithText("June 24, 2026", substring = true).assertIsDisplayed()
    }

    /**
     * **An undated entry is a real answer and says so**, in a group of its own
     * that no band shortens.
     */
    @Test
    fun anUndatedEntrySaysTheDateIsNotKnown() {
        show(listOf(monday, entry("u1", "Nobody could say when", null, null)))
        compose.onNodeWithText("Date not known", substring = true).assertIsDisplayed()
    }
}
