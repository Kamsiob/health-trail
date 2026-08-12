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
import com.kamsiob.healthtrail.ui.screens.MedicationsScreen
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The medications list, and the one line grid screen 12 draws that it did not.
 *
 * **A medication with a question waiting shows it on the row**, #352. The link
 * was built in both directions everywhere else: the capture form writes
 * `question.medication_id` and the medication's own screen lists them, so the
 * only place a person scanning could not see it was the list. Rule 18.
 *
 * **A screen test rather than a journey**, because the journey from the capture
 * chip to the column is already covered by `MedicationQuestionJourneyTest` and
 * what is in doubt here is only what the row renders for a given count.
 */
@RunWith(AndroidJUnit4::class)
class MedicationsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val lisinopril = Repository.Medication(
        id = "m1",
        name = "Lisinopril",
        doseText = "10 mg, mornings",
        purposeText = "Blood pressure",
        notes = null,
        onEmergencyCard = false,
        stoppedEdtf = null,
    )

    private fun show(waiting: Map<String, Int>) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    MedicationsScreen(
                        medications = listOf(lisinopril),
                        onOpen = {},
                        onAdd = {},
                        onBack = {},
                        openQuestions = waiting,
                    )
                }
            }
        }
    }

    @Test
    fun aMedicationWithOneQuestionWaitingSaysSoOnItsRow() {
        show(mapOf("m1" to 1))
        compose.onNodeWithText("1 question waiting", substring = true).assertIsDisplayed()
    }

    @Test
    fun severalQuestionsCountThemselves() {
        show(mapOf("m1" to 3))
        compose.onNodeWithText("3 questions waiting", substring = true).assertIsDisplayed()
    }

    /**
     * **Nothing waiting says nothing at all**, rather than "0 questions". An
     * absent key and a zero have to read the same way, because one comes from
     * the query and the other from a caller building the map by hand.
     */
    @Test
    fun aMedicationWithNothingWaitingSaysNothing() {
        show(emptyMap())
        compose.onNodeWithText("question", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        // The row is still there, with what it always carried.
        compose.onNodeWithText("Blood pressure", substring = true).assertIsDisplayed()
    }

    @Test
    fun aCountOfZeroReadsTheSameAsAnAbsentOne() {
        show(mapOf("m1" to 0))
        compose.onNodeWithText("question", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }
}
