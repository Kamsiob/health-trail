package com.kamsiob.healthtrail.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kamsiob.healthtrail.MainActivity
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.ui.components.Destination
import com.kamsiob.healthtrail.ui.components.NavTags
import com.kamsiob.healthtrail.ui.screens.AddMedTags
import com.kamsiob.healthtrail.ui.components.ChipPickerTags
import com.kamsiob.healthtrail.ui.screens.CaptureFormTags
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.CaptureTags
import com.kamsiob.healthtrail.ui.screens.DisclaimerTags
import com.kamsiob.healthtrail.ui.screens.EntryTags
import com.kamsiob.healthtrail.ui.screens.MedicationTags
import com.kamsiob.healthtrail.ui.screens.MedsTags
import com.kamsiob.healthtrail.ui.screens.NotebookTags
import com.kamsiob.healthtrail.ui.screens.SectionTags
import com.kamsiob.healthtrail.ui.screens.SetupTags
import com.kamsiob.healthtrail.ui.screens.SituationPickerTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Asking something about a medication, and finding it again from the medication.
 *
 * **`MASTER_SPEC.md` section 3 says a medication knows its pending questions**,
 * and until 2026-08-03 `question.medication_id` had no writer and no reader, so
 * the only way to ask about a drug was to type its name into the sentence and
 * hope to search for the same spelling later.
 *
 * **A journey rather than a screen test, deliberately**, per
 * `TESTING-PERSONAS.md` section 7. Composing the medication screen with a list
 * of questions handed to it proves the screen renders a list, which was never
 * in doubt. What was in doubt is everything between the two screens: that the
 * chip writes the column, that the shell reads it back, that opening the
 * question reaches its entry, and that the entry can get back to the
 * medication. Every one of those is shell behavior, and a screen test cannot
 * see any of it.
 *
 * **It hands the code nothing the person could not.** The medication is added
 * through the add form, the question through the capture sheet, and every step
 * is a tap or a keystroke.
 */
@RunWith(AndroidJUnit4::class)
class MedicationQuestionJourneyTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val drug = "Lisinopril"
    private val question = "Who changed the morning dose"

    private fun showing(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun settle() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag(AppRootTags.LOADING).fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
    }

    /** In from the front door, taking each gate only if it is actually there. */
    private fun reachTheNotebook() {
        settle()
        if (showing(DisclaimerTags.ROOT)) {
            compose.onNodeWithTag(DisclaimerTags.ACCEPT).performClick()
            settle()
        }
        if (showing(SetupTags.ROOT)) {
            compose.onNodeWithTag(SetupTags.SKIP).performClick()
            settle()
        }
        if (showing(SituationPickerTags.ROOT)) {
            compose.onNodeWithTag(SituationPickerTags.SKIP).performClick()
            settle()
        }
        compose.waitUntil(timeoutMillis = 20_000) { showing(ShellTags.ROOT) }
        compose.onNodeWithTag(NavTags.tab(Destination.NOTEBOOK)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(NotebookTags.ROOT) }
    }

    private fun openSection(section: Repository.Section) {
        compose.onNodeWithTag(NavTags.tab(Destination.NOTEBOOK)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(NotebookTags.ROOT) }
        // **Scrolls to the tile rather than asking a list for a key.** The
        // notebook became a plain scrolling column of tiles on 2026-08-03, so
        // there is no lazy list to ask, and every tile is in the tree.
        compose.onNodeWithTag(NotebookTags.section(section)).performScrollTo()
        compose.onNodeWithTag(NotebookTags.section(section)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(SectionTags.BACK) }
    }

    /** Adds one medication the way a person does, through the form. */
    private fun addTheMedication() {
        openSection(Repository.Section.MEDICATIONS)
        // **Scrolled to, because a lazy list has not composed what is below the
        // fold.** The first run of this test failed here rather than on
        // anything it was written to check, which is the ordinary cost of
        // walking in from the front door instead of composing a screen.
        compose.onNodeWithTag(SectionTags.root(MedsTags.NAME))
            .performScrollToNode(hasTestTag(MedsTags.ADD))
        compose.onNodeWithTag(MedsTags.ADD).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(AddMedTags.field("name")) }
        compose.onNodeWithTag(AddMedTags.field("name")).performTextInput(drug)
        compose.onNodeWithTag(AddMedTags.SAVE).performClick()
        settle()
        // **Out of the section before capturing.** The capture button lives on
        // the shell and an open section is drawn over it, so tapping it from
        // inside a section lands on the section instead. A person leaves the
        // section the same way.
        compose.onNodeWithTag(SectionTags.BACK).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(NotebookTags.ROOT) }
    }

    /** Asks something, tapping the medication chip on the way past. */
    private fun askAboutIt() {
        compose.onNodeWithTag(NavTags.CAPTURE).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureTags.SHEET) }
        compose.onNodeWithTag(CaptureTags.option(CaptureKind.QUESTION)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(CaptureFormTags.ROOT) }

        compose.onNodeWithTag(CaptureFormTags.NOTE).performTextInput(question)

        // The chip, which is the whole point. Scrolled to by tag rather than by
        // viewport, per the trap in HANDOFF section 8.
        // `performScrollTo` on the node itself rather than `performScrollToNode`
        // on a container: the form scrolls through a plain `verticalScroll`
        // rather than a lazy list, which has no scroll action of its own to ask.
        // **"Add more" first**, because the medication question moved behind the
        // disclosure on 2026-08-03. Walking in from the front door is the whole
        // point of this test, so it opens the control rather than reaching past
        // it.
        compose.onNodeWithTag(CaptureFormTags.MORE).performScrollTo().performClick()

        // **And the full set when the chip is not among the five**, which is
        // the other half of 5.11.1 and is the ordinary case for somebody on
        // eight medications. This test passed alone and failed in the suite
        // for exactly this reason: the suite shares one active subject, so by
        // the time it runs there are more medications than fit in a capped row
        // and the one it just added is the newest. Reaching past the cap with a
        // test tag would have hidden that the person cannot.
        val chip = CaptureFormTags.medication(medicationId())
        if (compose.onAllNodesWithTag(chip).fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag(CaptureFormTags.MORE_MEDICATIONS)
                .performScrollTo()
                .performClick()
            compose.onNodeWithTag(ChipPickerTags.SEARCH).performTextInput(drug)
            compose.onNodeWithTag(ChipPickerTags.option(medicationId())).performClick()
        } else {
            compose.onNodeWithTag(chip).performScrollTo().performClick()
        }
        // Save is outside the scrolling region and always on screen, which is
        // deliberate on that form and is why it must not be scrolled to.
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()
        settle()
    }

    /**
     * The medication's id, which the test needs only to name a test tag.
     *
     * **A shortcut, and named as one**, per `TESTING-PERSONAS.md` section 7.
     * Every tag in this app is keyed by a locally generated id, and Compose has
     * no prefix matcher, so a test that only ever touched the screen could not
     * say which chip to tap. It reads the id through the same public call the
     * screen itself uses, and it reads nothing else: every assertion below is
     * about what is on the screen, and no state is written this way.
     */
    private fun medicationId(): String = kotlinx.coroutines.runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        val repository = Repository.open(context)
        val subject = repository.activeSubject()!!.id
        repository.medications(subject).first { it.name == drug }.id
    }

    @Test
    fun aquestionAskedAboutAmedicationIsWaitingOnThatMedication() {
        reachTheNotebook()
        addTheMedication()
        askAboutIt()

        openSection(Repository.Section.MEDICATIONS)
        val id = medicationId()
        compose.onNodeWithTag(SectionTags.root(MedsTags.NAME))
            .performScrollToNode(hasTestTag(MedsTags.row(id)))
        compose.onNodeWithTag(MedsTags.row(id)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(SectionTags.root(MedicationTags.NAME)) }

        val questionId = kotlinx.coroutines.runBlocking {
            val context = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext
            Repository.open(context).openQuestionsAbout(id).also {
                assertTrue(
                    "the chip did not write question.medication_id, so the " +
                        "medication cannot know what is waiting to be asked about it",
                    it.isNotEmpty(),
                )
            }.first().id
        }

        compose.onNodeWithTag(SectionTags.root(MedicationTags.NAME))
            .performScrollToNode(hasTestTag(MedicationTags.question(questionId)))
        compose.onNodeWithTag(MedicationTags.question(questionId)).assertIsDisplayed()
    }

    @Test
    fun thequestionOpensItsEntryAndTheEntryLeadsBackToTheMedication() {
        reachTheNotebook()
        addTheMedication()
        askAboutIt()

        openSection(Repository.Section.MEDICATIONS)
        val id = medicationId()
        compose.onNodeWithTag(SectionTags.root(MedsTags.NAME))
            .performScrollToNode(hasTestTag(MedsTags.row(id)))
        compose.onNodeWithTag(MedsTags.row(id)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(SectionTags.root(MedicationTags.NAME)) }

        val questionId = kotlinx.coroutines.runBlocking {
            val context = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext
            Repository.open(context).openQuestionsAbout(id).first().id
        }

        compose.onNodeWithTag(SectionTags.root(MedicationTags.NAME))
            .performScrollToNode(hasTestTag(MedicationTags.question(questionId)))
        compose.onNodeWithTag(MedicationTags.question(questionId)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(SectionTags.root(EntryTags.NAME)) }

        // **The half that was missing for ten minutes.** Rule 18: if the
        // medication shows the question, the question shows the medication.
        compose.onNodeWithTag(SectionTags.root(EntryTags.NAME))
            .performScrollToNode(hasTestTag(EntryTags.MEDICATION))
        compose.onNodeWithTag(EntryTags.MEDICATION).assertIsDisplayed()
        compose.onNodeWithTag(EntryTags.MEDICATION).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { showing(SectionTags.root(MedicationTags.NAME)) }
    }
}
