package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.components.CHIP_CAP
import com.kamsiob.healthtrail.ui.screens.EmergencyCardEditScreen
import com.kamsiob.healthtrail.ui.screens.EmergencyEditTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Filling in the emergency card, which had never had a test of its own.
 *
 * **Both promises here were broken and both were found by opening the screen on
 * the phone**, working #376 item 6. Neither is visible in the source and neither
 * fails anything else: the screen was internally consistent and looked fine on
 * its own, which is what `DESIGN.md` 16.6 says about drift.
 */
@RunWith(AndroidJUnit4::class)
class EmergencyCardEditScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings get() = Strings.load(context)

    /**
     * A chip's label, as the chip actually carries it.
     *
     * **`Bidi.isolate` wraps every name**, so the node's text is `⁨Person 1⁩`
     * and a plain "Person 1" matches nothing. `docs/TRAPS.md` section 2 has
     * this and I walked into it anyway, an hour after writing it down, which is
     * why it is here at the point somebody would hit it rather than only there.
     */
    private fun chip(name: String) = Bidi.isolate(name)

    /** A care team big enough to be past the cap, which is what a real one is. */
    private fun team(size: Int) = (1..size).map {
        Repository.Person("p$it", "Person $it", "Role $it", "555014$it", null, null)
    }

    private fun show(people: List<Repository.Person>, onCard: Set<String> = emptySet()) {
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    EmergencyCardEditScreen(
                        card = null,
                        people = people,
                        onTheCard = onCard,
                        onToggleContact = {},
                        onSave = {},
                        onCancel = {},
                    )
                }
            }
        }
    }

    @Test
    fun theCareTeamIsCappedLikeEveryOtherChipSetInTheApp() {
        // **`DESIGN.md` 5.11.1 caps a chip set at five** and this one drew the
        // whole team: fifteen names filling the first screen, so allergies and
        // the rest of what the card exists for started below the fold. Seen on
        // the phone rather than reasoned about.
        show(team(15))

        // **Existence rather than display, and that is the claim.** "Capped at
        // five" is about which chips the screen draws, not about where they
        // land on one viewport, and the screen scrolls. Whether they look right
        // is the device pass, which a test cannot do.
        for (index in 1..CHIP_CAP) {
            compose.onNodeWithText(chip("Person $index")).assertExists()
        }
        assertTrue(
            "a sixth name is on the screen before anybody asked for the rest",
            compose.onAllNodesWithText(chip("Person ${CHIP_CAP + 1}")).fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithTag(EmergencyEditTags.MORE_PEOPLE).performClick()
        compose.onNodeWithText(chip("Person 15")).assertExists()
    }

    @Test
    fun nobodyAlreadyOnTheCardIsHiddenByTheCap() {
        // **The reason the multi select cap exists.** `cappedChips` takes one
        // selection and cannot serve a set, and a chip that disappeared when
        // the list was capped would hide a choice somebody had already made.
        // Person 14 is past the cap and on the card, so it has to be drawn.
        show(team(15), onCard = setOf("p14"))

        compose.onNodeWithText(chip("Person 14")).assertExists()
    }

    @Test
    fun theGroupIsNamedOnceRatherThanTwice() {
        // **It said "Who to call first" as a mono header and again as a body
        // label**, one under the other with the sentence explaining the tap
        // between them. That is the redundancy `SectionScaffold`'s own
        // documentation bans by name, and #371 item 5 took the same shape out
        // of two other screens.
        //
        // **The header is uppercased by `GroupHeader`**, so the two are
        // different strings on screen and a plain count of the label's own text
        // is what catches the duplicate.
        show(team(3))

        assertTrue(
            "the group's name is drawn twice",
            compose.onAllNodesWithText(strings["emergency.group.who"])
                .fetchSemanticsNodes().size <= 1,
        )
    }
}
