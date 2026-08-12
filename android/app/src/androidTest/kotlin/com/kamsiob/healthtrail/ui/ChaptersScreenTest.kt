package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.ChaptersScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The current chapter says what it holds. #356.
 *
 * **Grid screen 19 makes the current place a card for exactly this line**, "23
 * entries · 7 documents · 6 people · 1 open incident", and the caption is the
 * argument: history serves the present. The card carried a name and a date and
 * said nothing about what happened there.
 *
 * **A count of zero is left out entirely.** A chapter is a place somebody was,
 * not a checklist of what they collected there, and "no documents" on the card
 * for the place they are living now is the app scoring their filing.
 */
@RunWith(AndroidJUnit4::class)
class ChaptersScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val current = Repository.Chapter(
        id = "c1",
        name = "Maplewood Care Center",
        reason = null,
        notes = null,
        startedEdtf = "2025-12-29",
        endedEdtf = null,
    )

    private fun show(contents: Map<String, Repository.ChapterContents>) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ChaptersScreen(
                        chapters = listOf(current),
                        contents = contents,
                        onOpen = {},
                        onOpenMilestones = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun theCurrentChapterCountsWhatIsInIt() {
        show(
            mapOf(
                "c1" to Repository.ChapterContents(
                    entries = 23,
                    documents = 7,
                    people = 6,
                    openIncidents = 1,
                ),
            ),
        )

        // **Asserted on the words rather than on the test tag.** The chapter
        // card merges its descendants so it announces as one sentence to a
        // reader, and a merged child is not in the tree a finder walks: a tag
        // assertion on it passes when it is absent and fails when it is there,
        // which is a test that proves the opposite of what it says. Two runs
        // went into finding that out.
        compose.onNodeWithText("What is in this chapter", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("23 entries", substring = true).assertIsDisplayed()
        compose.onNodeWithText("7 documents", substring = true).assertIsDisplayed()
        compose.onNodeWithText("6 people", substring = true).assertIsDisplayed()
        compose.onNodeWithText("1 open incident", substring = true).assertIsDisplayed()
    }

    @Test
    fun aCountOfZeroIsLeftOutRatherThanSaidOutLoud() {
        show(
            mapOf(
                "c1" to Repository.ChapterContents(
                    entries = 4,
                    documents = 0,
                    people = 0,
                    openIncidents = 0,
                ),
            ),
        )

        compose.onNodeWithText("4 entries", substring = true).assertIsDisplayed()
        compose.onNodeWithText("document", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        compose.onNodeWithText("incident", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    /**
     * **A chapter with nothing in it shows no line at all**, rather than a
     * heading over four zeroes. Setup creates exactly this chapter.
     */
    @Test
    fun anEmptyChapterSaysNothingAboutWhatItHolds() {
        show(mapOf("c1" to Repository.ChapterContents(0, 0, 0, 0)))

        compose.onNodeWithText("What is in this chapter", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun aChapterWithNoCountsLoadedYetSaysNothing() {
        show(emptyMap())

        compose.onNodeWithText("What is in this chapter", substring = true)
            .assertDoesNotExist()
    }
}
