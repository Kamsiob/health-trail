package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * The care team leads with the people somebody actually calls. #351.
 *
 * **The screen drew every name at one weight**, where grid screen 11 draws a
 * short group of the ones you call and folds the rest. Fifteen rows and fifteen
 * identical call pills is the uniform weight rule 15 names, on the screen a
 * person reaches for when they need a number in the next ten seconds.
 *
 * **`Repository.peopleByRecentUse` already answered this** and this list was
 * the only one not asking it, which is why the fix is an ordering rather than a
 * new column.
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
        phone = "555 0100",
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

    @Test
    fun theThreeMostRecentlyUsedLeadAndTheRestAreFolded() {
        show(everybody, byRecentUse = everybody.reversed())

        // The three most recently used, which here are the last three added.
        compose.onNodeWithTag(CareTeamTags.person("p8")).assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.person("p7")).assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.person("p6")).assertIsDisplayed()

        // Everyone else is behind the fold, counted rather than hidden.
        // The fold says how many it holds, so nothing is hidden without a
        // number on it. Asserted on the fold rather than on the digit, since
        // the phone numbers on the rows above contain digits too.
        compose.onNodeWithTag(CareTeamTags.REST_FOLD)
            .assertIsDisplayed()
            .assertTextContains("5", substring = true)
        compose.onNodeWithTag(CareTeamTags.person("p1")).assertDoesNotExist()
    }

    @Test
    fun openingTheFoldReachesEveryone() {
        show(everybody, byRecentUse = everybody.reversed())
        compose.onNodeWithTag(CareTeamTags.REST_FOLD).performClick()

        compose.onNodeWithTag(CareTeamTags.person("p1")).assertIsDisplayed()
    }

    /**
     * **A short roster is not split**, because folding one or two people behind
     * a tap to make a list look organized is furniture, and four names fit on
     * the screen with room to spare.
     */
    @Test
    fun aShortRosterStaysWhole() {
        val four = everybody.take(4)
        show(four, byRecentUse = four.reversed())

        four.forEach { compose.onNodeWithTag(CareTeamTags.person(it.id)).assertIsDisplayed() }
        compose.onNodeWithTag(CareTeamTags.REST_FOLD).assertDoesNotExist()
    }

    /**
     * **A notebook with no history still has an order**, the one people were
     * added in, and it degrades to that rather than to an empty lead group.
     */
    @Test
    fun withNoHistoryTheOrderTheyWereAddedInLeads() {
        show(everybody)

        compose.onNodeWithTag(CareTeamTags.person("p1")).assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.person("p8")).assertDoesNotExist()
    }
}
