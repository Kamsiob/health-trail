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
 * Where somebody is, against everywhere else. #353, #379, rewritten for the
 * `ui/v4` care team, #386.
 *
 * **The folds are gone and the question they answered is not.** `m3v4-3` draws
 * a toggle rather than accordions, so the split is now two views of one list:
 * the staff where the person actually is, and everybody outside. D185.
 *
 * **The notebook with no places matters as much as the one with them.** That is
 * every notebook until somebody says where somebody works, and it must show one
 * plain list with no control on it: a toggle that filters a list into itself and
 * an empty one is furniture.
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

    private fun show(people: List<Repository.Person>, place: String? = "Maplewood Care Center") {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    CareTeamScreen(
                        people = people,
                        currentPlace = place,
                        onCall = {},
                        onOpen = {},
                        onAdd = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    /** **Where they are leads**, and the other side is named rather than counted. */
    @Test
    fun thetoggleNamesWhereTheyAreAndEverywhereElse() {
        show(roster)
        compose.onNodeWithTag(CareTeamTags.segment(0)).assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.segment(1)).assertIsDisplayed()
        compose.onNodeWithText("Marcus Bell", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** **Nothing is hidden behind a door**: the other side is one tap, not two. */
    @Test
    fun theothersideIsOneTapAway() {
        show(roster)
        compose.onNodeWithTag(CareTeamTags.segment(1)).performClick()
        compose.onNodeWithText("Ruth Ann Pierce", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Jerome Whitfield", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** People with no place at all are outside the facility, which is where they are. */
    @Test
    fun peopleWithNoPlaceAreOutsideTheFacility() {
        show(roster)
        compose.onNodeWithTag(CareTeamTags.segment(1)).performClick()
        compose.onNodeWithText("Tonya K.", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * **A notebook where nobody has a place is one list and no control.** That is
     * the ordinary case and it must not grow a toggle because a feature exists.
     */
    @Test
    fun anotebookWithNoPlacesIsOneList() {
        show(roster.map { it.copy(organizationId = null, organizationName = null) }, place = null)
        compose.onNodeWithTag(CareTeamTags.segment(0)).assertDoesNotExist()
        compose.onNodeWithText("Wesley Obi", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** Everybody is reachable from one side or the other, and nobody is on both. */
    @Test
    fun everybodyIsOnExactlyOneSide() {
        show(roster)
        compose.onNodeWithText("Angela Reyes", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag(CareTeamTags.segment(1)).performClick()
        compose.onNodeWithText("Angela Reyes", substring = true).assertDoesNotExist()
    }
}
