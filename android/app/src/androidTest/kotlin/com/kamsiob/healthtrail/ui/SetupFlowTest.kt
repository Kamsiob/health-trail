package com.kamsiob.healthtrail.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.SetupAnswers
import com.kamsiob.healthtrail.ui.screens.SetupScreen
import com.kamsiob.healthtrail.ui.screens.SetupTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Setup asks three things and lets everything else wait.
 *
 * The property under test is not that the form works. It is that **nothing is
 * required**. Persona P1 is someone standing in a corridor with four minutes,
 * and a required field there is a wall. Partial is a finished state, so
 * continuing with every field blank has to be a real path that produces a
 * working notebook rather than an error.
 */
@RunWith(AndroidJUnit4::class)
class SetupFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private fun showSetup(onContinue: (SetupAnswers) -> Unit, onSkip: () -> Unit = {}) {
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    SetupScreen(onContinue = onContinue, onSkip = onSkip)
                }
            }
        }
    }

    @Test
    fun theScreenSaysNoneOfItIsRequiredWithoutRepeatingItPerField() {
        // Said once, in words, at the top. The screen used to carry a mono
        // "Optional" here, which is accurate and is the vocabulary of a form
        // being administered, on the first real screen after the disclaimer.
        showSetup(onContinue = {})
        compose.onNodeWithTag(SetupTags.REASSURE).assertIsDisplayed()

        val strings = Strings.load(context)
        val reassurance = strings["setup.reassure"]
        // One reassurance on the screen, not five.
        assertEquals(
            "the reassurance is repeated",
            1,
            compose.onAllNodesWithText(reassurance).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun everyFieldCarriesGuidanceRatherThanAnEmptyBox() {
        // Four of the five were bare gray boxes with a label above them, which
        // is what made this read as paperwork more than anything else on it.
        // Section 5.9: a hint is genuine guidance, never a repeat of the label.
        showSetup(onContinue = {})
        val strings = Strings.load(context)
        listOf(
            "setup.name.hint",
            "setup.relationship.hint",
            "setup.where.hint",
            "setup.phone.person.hint",
            "setup.phone.number.hint",
        ).forEach { key ->
            val hint = strings[key]
            assertTrue(
                "the hint for $key is missing from the screen",
                compose.onAllNodesWithText(hint).fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @Test
    fun noGroupHeadingRepeatsAFieldLabel() {
        // Built with them shared, the screen showed the same sentence twice in
        // a row and a screen reader announced it twice. Asserted rather than
        // remembered, because the next person editing the copy will not know.
        val strings = Strings.load(context)
        val headings = listOf("setup.group.who", "setup.group.where", "setup.group.reach")
            .map { strings[it].lowercase() }
        val labels = listOf(
            "setup.name.label",
            "setup.relationship.label",
            "setup.where.label",
            "setup.phone.person.label",
            "setup.phone.number.label",
        ).map { strings[it].lowercase() }

        val clashes = headings.filter { it in labels }
        assertTrue("a group heading repeats a field label: $clashes", clashes.isEmpty())
    }

    @Test
    fun continuingWithEverythingBlankIsAllowed() {
        var answers: SetupAnswers? = null
        showSetup(onContinue = { answers = it })

        compose.onNodeWithTag(SetupTags.CONTINUE).performClick()

        assertNotNull("continuing with a blank form did nothing", answers)
        assertEquals("", answers!!.name)
        assertEquals("", answers.where)
        assertEquals("", answers.phoneNumber)
    }

    @Test
    fun skippingIsAReadPathAndReachesTheSameOutcome() {
        var skipped = false
        showSetup(onContinue = {}, onSkip = { skipped = true })

        compose.onNodeWithTag(SetupTags.SKIP).performClick()

        assertTrue("skip did nothing, which would make it a dead end", skipped)
    }

    @Test
    fun whatIsTypedIsWhatIsCarriedForward() {
        var answers: SetupAnswers? = null
        showSetup(onContinue = { answers = it })

        compose.onNodeWithTag(SetupTags.NAME).performTextInput("Mum")
        compose.onNodeWithTag(SetupTags.WHERE).performTextInput("Maplewood Care Center")
        compose.onNodeWithTag(SetupTags.PHONE).performTextInput("5551234567")
        compose.onNodeWithTag(SetupTags.CONTINUE).performClick()

        assertNotNull(answers)
        assertEquals("Mum", answers!!.name)
        assertEquals("Maplewood Care Center", answers.where)
        assertEquals("5551234567", answers.phoneNumber)
    }

    @Test
    fun setupWritesASubjectAndOnlyTheThingsThatWereFilledIn() = runBlocking {
        val repository = Repository.open(context)

        // A fresh notebook for this test. The rows written here are the ones
        // asserted on, and nothing else in the suite depends on them.
        val subjectId = repository.createSubject(displayName = "Mum", relationship = "My mother")
        assertTrue(subjectId.isNotBlank())

        // Looked up by id rather than through activeSubject(), because the
        // suite shares one database and an earlier test may have written a
        // subject first. A test that depends on being the only writer is a test
        // that fails whenever the suite is reordered.
        val subject = repository.subject(subjectId)
        assertNotNull("the subject was not written", subject)
        assertEquals("Mum", subject!!.displayName)
        assertEquals("My mother", subject.relationship)

        // Blank fields write nothing rather than writing an empty row, which
        // would put a nameless person on the care team.
        val peopleBefore = repository.count(Repository.Section.CARE_TEAM)
        repository.createPerson(subjectId = subjectId, displayName = "Ward desk", phone = "5551234567")
        val peopleAfter = repository.count(Repository.Section.CARE_TEAM)
        assertEquals("the phone number did not become a care team entry", peopleBefore + 1, peopleAfter)
    }

    @Test
    fun aBlankRelationshipIsStoredAsAbsentRatherThanAsAnEmptyString() = runBlocking {
        val repository = Repository.open(context)
        val id = repository.createSubject(displayName = "Someone", relationship = "")
        assertTrue(id.isNotBlank())

        val subject = repository.subject(id)
        assertNotNull(subject)
        // Empty strings and nulls read differently everywhere downstream, and a
        // blank answer means "not said" rather than "said nothing".
        assertNull("a blank relationship was stored as an empty string", subject!!.relationship)
    }
}
