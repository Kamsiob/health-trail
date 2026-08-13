package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CaptureFormState
import com.kamsiob.healthtrail.ui.screens.PersonScreen
import com.kamsiob.healthtrail.ui.screens.PersonTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writing something down about the person whose screen you are on.
 *
 * **This screen could not record anything at all.** Somebody who had just come
 * off the phone with the charge nurse, looking at her card with her number on
 * it, had to leave, press the gold button, choose a kind, and find her name
 * again in a picker. **Four taps to say a thing the app was standing next to.**
 * Rule 18: carry the context forward instead of asking again, and count the
 * taps. #46.
 *
 * **The button is the easy half and it is not the half that goes wrong.** A
 * control that opens the capture sheet and forgets who it was about looks
 * identical on the screen and leaves the person doing the same picking they
 * were doing before. So this asserts the draft, not the navigation: the name
 * is in it, attached to the right id, before anything else happens.
 *
 * **The kind is deliberately not carried.** What happened is the person's to
 * say, and guessing it would be the app filing on their behalf.
 */
@RunWith(AndroidJUnit4::class)
class CarryTheNameForwardTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val person = Repository.Person(
        "p1", "Angela Reyes", "Charge nurse, day shift", "5550142", null, null,
    )

    @Test
    fun theNameTravelsWithTheDraftAndTheKindDoesNot() {
        // What the shell holds between the person's screen and the form.
        var draft = CaptureFormState()

        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    PersonScreen(
                        person = person,
                        entries = emptyList(),
                        onCall = {},
                onSetPinned = {},
                onSetArchived = {},
                        onEdit = {},
                        onCapture = { draft = CaptureFormState().togglePerson(person) },
                        onRemove = {},
                        onOpenEntry = {},
                        onBack = {},
                    )
                }
            }
        }

        compose.onNodeWithTag(PersonTags.CAPTURE).performClick()

        assertEquals(
            "the capture the person's screen opened did not know who it was about",
            "p1",
            draft.personId,
        )
        assertEquals(
            "the name shown back in the form was not theirs",
            "Angela Reyes",
            draft.who,
        )
        // **Nothing else travels.** A prefilled note or a chosen kind would be
        // the app deciding what happened.
        assertEquals("", draft.note)
    }
}
