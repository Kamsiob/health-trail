package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CareTeamScreen
import com.kamsiob.healthtrail.ui.screens.CareTeamTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The care team leads with the person somebody actually calls. #351, rewritten
 * for the `ui/v4` screen, #386.
 *
 * **The screen drew every name at one weight**, and `m3v4-3` draws one person
 * raised into a block of their own with the two things you do about them, then
 * everyone else as a row with the one. Fifteen rows and fifteen identical call
 * pills is the uniform weight rule 15 names, on the screen a person reaches for
 * when they need a number in the next ten seconds.
 *
 * **`Repository.peopleByRecentUse` already answered who that is**, and a pin
 * beats it outright, because a pin is the person overriding the guess. #361.
 */
@RunWith(AndroidJUnit4::class)
class CareTeamScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun person(id: String, name: String) = Repository.Person(
        id = id,
        displayName = name,
        roleLabel = "Nurse",
        phone = "(555) 555-0100",
        email = null,
        notes = null,
    )

    private val everybody = (1..8).map { person("p$it", "Person $it") }

    private fun show(
        people: List<Repository.Person>,
        byRecentUse: List<Repository.Person> = emptyList(),
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    CareTeamScreen(
                        people = people,
                        byRecentUse = byRecentUse,
                        onCall = {},
                        onOpen = {},
                        onAdd = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    /** The most recently used leads, in a block rather than in the list. */
    @Test
    fun themostRecentlyUsedIsTheOneRaised() {
        show(everybody, byRecentUse = everybody.reversed())

        compose.onNodeWithTag(CareTeamTags.LEAD).assertIsDisplayed()
        compose.onNodeWithText("Person 8", substring = true).assertIsDisplayed()
        // Raised means raised out of the list, not shown twice.
        compose.onNodeWithTag(CareTeamTags.person("p8")).assertDoesNotExist()
    }

    /**
     * **Nothing is folded any more, so everybody is on the screen.** D185: the
     * accordion's count said how many were waiting behind a tap, and a list that
     * simply carries them says it better.
     */
    @Test
    fun everybodyElseIsARowAndNobodyIsBehindADoor() {
        show(everybody, byRecentUse = everybody.reversed())

        listOf("p1", "p2", "p7").forEach {
            compose.onNodeWithTag(CareTeamTags.person(it)).performScrollTo().assertIsDisplayed()
        }
    }

    /** A short roster is the same screen with fewer rows on it. */
    @Test
    fun ashortRosterIsTheSameScreen() {
        val four = everybody.take(4)
        show(four, byRecentUse = four.reversed())

        compose.onNodeWithTag(CareTeamTags.LEAD).assertIsDisplayed()
        // The most recently used leads, so p4 is the block and p1 to p3 are rows.
        four.dropLast(1).forEach {
            compose.onNodeWithTag(CareTeamTags.person(it.id)).performScrollTo().assertIsDisplayed()
        }
    }

    /**
     * **A notebook with no history still has an order**, the one people were
     * added in, and it degrades to that rather than to an empty block.
     */
    @Test
    fun withNoHistoryTheOrderTheyWereAddedInLeads() {
        show(everybody)

        compose.onNodeWithText("Person 1", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.person("p1")).assertDoesNotExist()
    }

    /** A pin is the person overriding the guess, so it wins outright. #361. */
    @Test
    fun apinBeatsRecentUse() {
        val pinned = everybody.map { if (it.id == "p3") it.copy(pinnedAt = 1_000L) else it }
        show(pinned, byRecentUse = pinned.reversed())

        compose.onNodeWithText("Person 3", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.person("p3")).assertDoesNotExist()
    }

    /**
     * **A real United States number is fourteen characters and it must not cost
     * the person their name.** The owner's words on #361: it needs to fit.
     *
     * **`m3v4-3` keeps the number off the row entirely**: the name, what they
     * do, and a gold mark carrying the phone. So what has to stay true is that a
     * reader still hears whose number it is, because "Call" fifteen times is the
     * ambiguity `DESIGN.md` 5.12 exists to prevent.
     */
    @Test
    fun afullUnitedStatesNumberIsCarriedByTheMarkAndNamedForAReader() {
        // **Two different numbers**, because both the block and the row name
        // theirs, and one number on both is a description that matches twice.
        val row = person("p2", "Ada Lovelace").copy(phone = "(555) 555-0177")
        show(listOf(person("p1", "Marguerite Boateng"), row))

        compose.onNodeWithText("Ada Lovelace", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(
            strings("careteam.call.number", "number" to "(555) 555-0177"),
        ).assertExists()
    }
}
