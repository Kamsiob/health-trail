package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * The care team folds by where people work. #353.
 *
 * **Grid screen 11 has always drawn this** as "At Maplewood, 4 more" and
 * "Outside, billing, ombudsman, 2", and it could not be built: the column and
 * its index shipped in Phase 0 and nothing ever wrote either, so grouping by it
 * gave one fold holding everybody, labeled with nothing.
 *
 * **The notebook with no places matters as much as the one with them.** That is
 * every notebook until somebody says where somebody works, and it must look
 * exactly as it did.
 */
@RunWith(AndroidJUnit4::class)
class CareTeamPlacesTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private fun person(name: String, place: String?) = Repository.Person(
        id = name.lowercase().replace(' ', '_'),
        displayName = name,
        roleLabel = "Nurse",
        phone = "(555) 555-0142",
        email = null,
        notes = null,
        organizationId = place?.lowercase(),
        organizationName = place,
    )

    /** Eight, so the lead group takes three and five are left to fold. */
    private val roster = listOf(
        person("Angela Reyes", "Maplewood Care Center"),
        person("Marcus Bell", "Maplewood Care Center"),
        person("Dr. Priya Raman", "Northside Medical Group"),
        person("Tonya K.", null),
        person("Wesley Obi", "Maplewood Care Center"),
        person("Sharon Delacroix", "Maplewood Care Center"),
        person("Ruth Ann Pierce", "Northside Medical Group"),
        person("Jerome Whitfield", null),
    )

    private fun show(people: List<Repository.Person>) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    CareTeamScreen(
                        people = people,
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
    fun therestIsFoldedByWhereTheyWork() {
        show(roster)
        compose.onNodeWithTag(CareTeamTags.placeFold("Maplewood Care Center"))
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.placeFold("Northside Medical Group"))
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** **People with no place keep the fold they always had**, and it goes last. */
    @Test
    fun peopleWithNoPlaceAreStillEveryoneElse() {
        show(roster)
        compose.onNodeWithTag(CareTeamTags.REST_FOLD).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun afoldOpensOntoThePeopleInIt() {
        show(roster)
        compose.onNodeWithTag(CareTeamTags.placeFold("Northside Medical Group"))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Ruth Ann Pierce", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * **A notebook where nobody has a place looks exactly as it did**, which is
     * this issue's own acceptance and is the ordinary case: one fold, the same
     * words, no empty groups.
     */
    @Test
    fun anotebookWithNoPlacesLooksAsItAlwaysDid() {
        show(roster.map { it.copy(organizationId = null, organizationName = null) })
        compose.onNodeWithTag(CareTeamTags.REST_FOLD).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.placeFold("Maplewood Care Center"))
            .assertDoesNotExist()
    }
}
