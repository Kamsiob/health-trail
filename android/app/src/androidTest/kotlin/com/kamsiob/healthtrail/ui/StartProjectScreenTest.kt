package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.StartProjectScreen
import com.kamsiob.healthtrail.ui.screens.StartProjectTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The picker's grouping and its search, which are the two things on it that can
 * be quietly wrong.
 *
 * **A fold that never opens and a search that quietly drops a match both look
 * like a shorter catalog**, and a shorter catalog looks deliberate. Neither
 * announces itself, which is what makes them worth a test rather than a look.
 *
 * The templates here are hand built rather than loaded from the catalog, so the
 * test says what it depends on. `check_templates.py` is what holds the real
 * sixteen to the same four categories.
 */
@RunWith(AndroidJUnit4::class)
class StartProjectScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun template(id: String, name: String, category: String, subtitle: String = "") =
        TemplateCatalog.ProjectTemplate(
            id = id,
            name = name,
            subtitle = subtitle,
            category = category,
            stateVariance = false,
            roles = emptyList(),
            steps = listOf("One", "Two"),
        )

    private val templates = listOf(
        template("medicaid_ltc", "Medicaid application", "paying", "Applying for coverage"),
        template("bill_reconciliation", "Sorting out bills", "paying"),
        template("discharge_appeal", "Appealing a discharge", "challenge"),
        template("facility_transfer", "Moving to a different facility", "moving"),
        template("records_request", "Requesting medical records", "papers"),
    )

    private fun show(own: List<Repository.OwnTemplate> = emptyList()) {
        val strings = Strings.load(context, Locale.ENGLISH)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    StartProjectScreen(
                        templates = templates,
                        own = own,
                        onChoose = {},
                        onChooseOwn = {},
                        onCancel = {},
                    )
                }
            }
        }
    }

    private fun ownTemplate(id: String, name: String) = Repository.OwnTemplate(
        id = id,
        name = name,
        derivedFromId = null,
        steps = listOf("One"),
        createdAt = 0L,
    )

    @Test
    fun theFirstCategoryLeadsOpenWhenThereAreNoneOfTheirOwn() {
        show()
        // Paying for care is first in the fixed order, so its rows are on
        // screen without anybody opening anything: the screen is never four
        // sand rows with nothing between them.
        compose.onNodeWithTag(StartProjectTags.template("medicaid_ltc")).assertIsDisplayed()
        compose.onNodeWithTag(StartProjectTags.category("challenge")).assertIsDisplayed()
    }

    @Test
    fun theLeadingCategoryHasNoFoldBecauseItIsAlreadyOpen() {
        show()
        compose.onNodeWithTag(StartProjectTags.category("paying")).assertDoesNotExist()
    }

    @Test
    fun aFoldedCategoryOpensAndShowsItsTemplates() {
        show()
        compose.onNodeWithTag(StartProjectTags.template("records_request")).assertDoesNotExist()
        compose.onNodeWithTag(StartProjectTags.category("papers")).performClick()
        compose.onNodeWithTag(StartProjectTags.template("records_request")).assertIsDisplayed()
    }

    @Test
    fun theirOwnLeadAndEveryCategoryFoldsBehindThem() {
        show(own = listOf(ownTemplate("mine", "The one I wrote")))
        compose.onNodeWithTag(StartProjectTags.ownTemplate("mine")).assertIsDisplayed()
        // With something of their own at the top, nothing else is open: the
        // first category becomes a fold like the rest.
        compose.onNodeWithTag(StartProjectTags.category("paying")).assertIsDisplayed()
        compose.onNodeWithTag(StartProjectTags.template("medicaid_ltc")).assertDoesNotExist()
    }

    @Test
    fun searchFindsSomethingInAClosedCategory() {
        show()
        // The whole point of the search: what somebody is looking for is
        // usually not in the group that happens to be open.
        compose.onNodeWithTag(StartProjectTags.template("records_request")).assertDoesNotExist()
        compose.onNodeWithTag(StartProjectTags.SEARCH).performTextInput("records")
        compose.onNodeWithTag(StartProjectTags.template("records_request")).assertIsDisplayed()
    }

    @Test
    fun searchFlattensSoTheAnswerIsNotBehindAFold() {
        show()
        compose.onNodeWithTag(StartProjectTags.SEARCH).performTextInput("records")
        compose.onNodeWithTag(StartProjectTags.category("papers")).assertDoesNotExist()
        // And what did not match is gone rather than merely further down.
        compose.onNodeWithTag(StartProjectTags.template("medicaid_ltc")).assertDoesNotExist()
    }

    @Test
    fun searchReadsTheLineUnderTheNameAsWellAsTheName() {
        show()
        // "Applying for coverage" is the subtitle of the Medicaid row and
        // appears in no name. Searching names alone is the easy version and it
        // misses the way people actually describe what they are trying to do.
        compose.onNodeWithTag(StartProjectTags.SEARCH).performTextInput("coverage")
        compose.onNodeWithTag(StartProjectTags.template("medicaid_ltc")).assertIsDisplayed()
    }

    @Test
    fun searchingTheirOwnWorksToo() {
        show(own = listOf(ownTemplate("mine", "The one I wrote")))
        compose.onNodeWithTag(StartProjectTags.SEARCH).performTextInput("wrote")
        compose.onNodeWithTag(StartProjectTags.ownTemplate("mine")).assertIsDisplayed()
        compose.onNodeWithTag(StartProjectTags.template("medicaid_ltc")).assertDoesNotExist()
    }

    @Test
    fun nothingMatchingStillOffersTheWayOut() {
        show()
        compose.onNodeWithTag(StartProjectTags.SEARCH).performTextInput("zzzz")
        // Rule 13 and 13.3: an empty result is not an error, and the one thing
        // the person can still do stays on screen.
        compose.onNodeWithTag(StartProjectTags.OWN).assertIsDisplayed()
    }

    @Test
    fun theBlankProjectOffersNoActionUntilItHasAName() {
        show()
        compose.onNodeWithTag(StartProjectTags.OWN_START).assertDoesNotExist()
        // **The block opens before it can be typed into**, since #379 made it
        // a compact row at the top of the picker rather than a form standing
        // open above the sixteen templates. The rule under test is unchanged:
        // naming it is what makes the action appear.
        compose.onNodeWithTag(StartProjectTags.OWN).performScrollTo().performClick()
        compose.onNodeWithTag(StartProjectTags.OWN_NAME).performTextInput("Sort out the wheelchair")
        // **Exists rather than displayed**, because the block sits at the foot
        // of a scrolling list and whether it is on screen depends on the
        // viewport rather than on the rule under test. What is being asserted
        // is that naming it is what makes the action appear at all.
        compose.onNodeWithTag(StartProjectTags.OWN_START).assertExists()
    }
}
