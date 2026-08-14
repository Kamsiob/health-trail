package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.ui.screens.CorrectMeasureScreen
import com.kamsiob.healthtrail.ui.screens.CorrectReadingScreen
import com.kamsiob.healthtrail.ui.screens.MeasurementTags
import com.kamsiob.healthtrail.ui.screens.OwnMeasure
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Correcting a reading and correcting the measure it belongs to. #374, the last
 * two of the six.
 *
 * **A reading is typed one handed while holding something else**, which is how
 * 138.8 becomes 1388. A measure's name is on every reading of it, on its card
 * on Today and on the chart's own heading, so a name typed wrong at setup is
 * typed wrong in four places forever.
 *
 * **Both reuse the form that created them** rather than a second one asking the
 * same questions in a slightly different order, which is what these tests are
 * really holding: that the reuse carries the existing values in and the
 * corrected ones back out.
 */
@RunWith(AndroidJUnit4::class)
class CorrectingAMeasurementTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings get() = Strings.load(context)

    private fun reading(number: Double? = 1388.0) = Repository.Reading(
        id = "r1",
        measureId = "m1",
        number = number,
        text = null,
        unit = "lb",
        occurredEdtf = "2026-03-04",
        occurredStart = 1772582400000L,
        note = "After breakfast",
        source = null,
    )

    @Test
    fun theReadingFormStartsFromWhatIsAlreadyWrittenDown() {
        // **D147: a correction starts with what is there.** Somebody who opened
        // this to move a decimal point should not retype the value, the unit,
        // the date and the note.
        //
        // **The number is rendered without a trailing `.0`**, because 1388.0 in
        // a field somebody is about to edit is the app showing its own storage
        // rather than their reading.
        var saved: Double? = null
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    CorrectReadingScreen(
                        name = "Weight",
                        units = listOf("lb"),
                        isText = false,
                        reading = reading(),
                        onSave = { _, number, _, _, _ -> saved = number },
                        onCancel = {},
                    )
                }
            }
        }

        compose.onNodeWithText("1388").assertExists()

        compose.onNodeWithTag(MeasurementTags.VALUE).performTextClearance()
        compose.onNodeWithTag(MeasurementTags.VALUE).performTextInput("138.8")
        compose.onNodeWithTag(MeasurementTags.SAVE).performClick()

        assertEquals("the corrected value did not come back", 138.8, saved)
    }

    @Test
    fun theReadingFormDoesNotOfferToMakeAnOldDateRough() {
        // **"Today" is right for a reading being taken and wrong for one from
        // March.** The date it already has is the answer, and defaulting a
        // correction to a rough when would invite somebody to overwrite a fact
        // with an approximation. Rule 17: the date is editable, not reset.
        var savedDate: Edtf.Date? = null
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    CorrectReadingScreen(
                        name = "Weight",
                        units = listOf("lb"),
                        isText = false,
                        reading = reading(),
                        onSave = { _, _, _, occurred, _ -> savedDate = occurred },
                        onCancel = {},
                    )
                }
            }
        }

        // Saved untouched, so what comes back is what was already recorded.
        compose.onNodeWithTag(MeasurementTags.SAVE).performClick()

        assertEquals(
            "the reading's own date was not what came back",
            "2026-03-04",
            savedDate?.canonical,
        )
    }

    @Test
    fun theMeasureFormSaysItsKindRatherThanOfferingToChangeIt() {
        // **A continuous measure with readings in it does not become an
        // observational one because somebody tapped a chip**: every number
        // already written down would have nowhere to live. A chip that looks
        // tappable and is not is worse than a sentence, per rule 16, so the
        // kind is said.
        var saved: OwnMeasure? = null
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    CorrectMeasureScreen(
                        measure = Repository.Measure(
                            id = "m1",
                            name = "Wieght",
                            presetId = null,
                            unit = "lb",
                            isText = false,
                        ),
                        onSave = { saved = it },
                        onCancel = {},
                    )
                }
            }
        }

        assertTrue(
            "the kind is still offered as a choice on a measure that has readings",
            compose.onAllNodesWithTag(MeasurementTags.OWN_TEXT).fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithText(strings["measurement.own.kind.number"]).assertExists()

        compose.onNodeWithTag(MeasurementTags.OWN_NAME).performTextClearance()
        compose.onNodeWithTag(MeasurementTags.OWN_NAME).performTextInput("Weight")
        compose.onNodeWithTag(MeasurementTags.OWN_START).performClick()

        assertEquals("the corrected name did not come back", "Weight", saved?.name)
        assertEquals("the unit was lost on the way", "lb", saved?.unit)
    }
}
