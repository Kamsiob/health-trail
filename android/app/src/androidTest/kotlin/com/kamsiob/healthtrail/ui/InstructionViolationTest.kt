package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Edtf
import com.kamsiob.healthtrail.time.EventDateText
import com.kamsiob.healthtrail.ui.screens.InstructionTags
import com.kamsiob.healthtrail.ui.screens.StandingInstructionsScreen
import com.kamsiob.healthtrail.ui.screens.ViolationDraft
import com.kamsiob.healthtrail.ui.screens.ViolationScreen
import com.kamsiob.healthtrail.ui.screens.ViolationTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A time an instruction was not followed, written down and read back.
 *
 * **Both halves of #371's first item.** The form took a note and nothing else,
 * so `recordViolation`'s `incidentId` and `billId` were parameters no caller
 * passed and `Violation.incidentTitle` and `billDescription` were columns no
 * screen could ever receive. And the row that read them back showed a count
 * alone, so somebody who typed "the night nurse gave it at 9 instead of 6" was
 * shown a 3.
 *
 * **The counting rules are tested alongside the words**, because rule 2 lives
 * exactly here: the screen may say how many times something was written down
 * and may never say what that means.
 */
@RunWith(AndroidJUnit4::class)
class InstructionViolationTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val instruction = Repository.StandingInstruction(
        "s1",
        "Call me about any fall",
        "Please call me right away, at any hour.",
        "federal",
        "2026-08-02",
        null,
        null,
        null,
    )

    private val fall = Repository.Incident(
        id = "i1",
        title = "Bruise on her arm nobody could explain",
        description = null,
        reportedEdtf = "2026-08-03",
        reportedStart = null,
        resolvedAt = null,
        resolutionNote = null,
        chapterName = null,
        entryCount = 1,
    )

    private val ambulance = Repository.Bill(
        id = "b1",
        description = "Ambulance transfer",
        amountMinor = 316_877,
        currency = "USD",
        state = "disputed",
        stateNote = null,
        receivedEdtf = "2026-08-04",
        notes = null,
    )

    private fun violation(
        id: String,
        note: String?,
        incidentTitle: String? = null,
        billDescription: String? = null,
    ) = Repository.Violation(
        id = id,
        occurredEdtf = "2026-08-05",
        note = note,
        incidentId = incidentTitle?.let { "i1" },
        incidentTitle = incidentTitle,
        billId = billDescription?.let { "b1" },
        billDescription = billDescription,
    )

    private fun showList(
        violations: List<Repository.Violation>,
        onOpenViolation: (Repository.Violation) -> Unit = {},
    ) {
        val catalog = runBlocking { TemplateCatalog.instructions(context) }
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    StandingInstructionsScreen(
                        instructions = listOf(instruction),
                        tags = catalog.tags,
                        onOpen = {},
                        onAdd = {},
                        onBack = {},
                        violations = if (violations.isEmpty()) {
                            emptyMap()
                        } else {
                            mapOf(instruction.id to violations)
                        },
                        onOpenViolation = { _, violation -> onOpenViolation(violation) },
                    )
                }
            }
        }
    }

    private fun showForm(
        incidents: List<Repository.Incident> = listOf(fall),
        bills: List<Repository.Bill> = listOf(ambulance),
        direction: LayoutDirection = LayoutDirection.Ltr,
        existing: Repository.Violation? = null,
        onRemove: (() -> Unit)? = null,
        onSave: (ViolationDraft) -> Unit = {},
    ) {
        compose.setContent {
            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalLayoutDirection provides direction,
            ) {
                HealthTrailTheme {
                    ViolationScreen(
                        instruction = instruction,
                        onSave = onSave,
                        onCancel = {},
                        incidents = incidents,
                        bills = bills,
                        existing = existing,
                        onRemove = onRemove,
                        // Fixed, so the rendered date is the same string on
                        // every run and at every hour of the night.
                        today = LocalDate.of(2026, 8, 13),
                    )
                }
            }
        }
    }

    // ---- what the row reads back -----------------------------------------

    /**
     * **The finding itself.** The words are the record and the count is only
     * how many of them there are, and for the life of this screen the count was
     * all it showed.
     */
    @Test
    fun theWordsAreShownAndNotOnlyTheCount() {
        showList(
            listOf(
                violation("v1", "The night nurse gave it at 9 instead of 6"),
                violation("v2", "Nobody called until the next morning"),
            ),
        )
        compose.onNodeWithText("The night nurse gave it at 9 instead of 6", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Nobody called until the next morning", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        // **The count is the group's own header now**, which reads
        // "TIMES IT WAS NOT FOLLOWED 2" and announces the whole sentence,
        // because "Times it was not followed 2" is not what the count means.
        compose.onNodeWithContentDescription("Not followed 2 times", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * **The two columns the schema carried and no form ever wrote.** This line
     * could not appear at all until the form began to ask, which is why the two
     * halves are one item of work rather than two.
     */
    @Test
    fun aViolationSaysWhatItWasAbout() {
        showList(
            listOf(
                violation(
                    "v1",
                    "They took her without telling me",
                    incidentTitle = fall.title,
                    billDescription = ambulance.description,
                ),
            ),
        )
        compose.onNodeWithText(fall.title, substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ambulance.description, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * **A time written down with no words is still a time it happened.** The
     * note is optional at the writer, so the row renders the date alone rather
     * than an empty line, and it is still counted.
     *
     * **Asserted on the date rather than on a tag**, because the card merges
     * its descendants and a tag inside it is not in the tree a finder walks.
     * That trap cost two runs on #352 and it is written down in `HANDOFF.md`.
     */
    @Test
    fun aTimeWithNoWordsStillAppearsAndIsCounted() {
        showList(listOf(violation("v1", null)))
        compose.onNodeWithText(EventDateText.render(strings, "2026-08-05"), substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Not followed once", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * **Nothing written down says nothing at all**, rather than "0 times".
     * Zero is not a finding, and printing it would turn every instruction on
     * the screen into a scoreboard.
     */
    @Test
    fun nothingWrittenDownSaysNothingAtAll() {
        showList(emptyList())
        compose.onNodeWithText("Not followed", substring = true).assertDoesNotExist()
        compose.onNodeWithText("TIMES IT WAS NOT FOLLOWED", substring = true)
            .assertDoesNotExist()
        // The instruction itself is untouched by any of this.
        compose.onNodeWithText("Call me about any fall", substring = true).assertIsDisplayed()
    }

    // ---- what the form asks ----------------------------------------------

    /**
     * **The corridor path is one field and a Save.** The link sits behind the
     * disclosure, so somebody standing in a hallway meets what they always met.
     */
    @Test
    fun theShortFormIsStillOneFieldAndASave() {
        showForm()
        compose.onNodeWithTag(ViolationTags.NOTE).assertIsDisplayed()
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(ViolationTags.incident(fall.id)).assertDoesNotExist()
    }

    /**
     * The two arguments nothing had ever passed, passed. Chosen from behind the
     * disclosure, carried on the draft, and never required to save.
     */
    @Test
    fun whatItWasAboutIsCarriedOnTheDraft() {
        var saved: ViolationDraft? = null
        showForm(onSave = { saved = it })

        compose.onNodeWithTag(ViolationTags.ABOUT).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.incident(fall.id)).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.bill(ambulance.id)).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()

        assertEquals(fall.id, saved?.incidentId)
        assertEquals(ambulance.id, saved?.billId)
    }

    /**
     * **A link a person cannot take off again is one they hesitate to put on.**
     * Tapping the chosen chip clears it, the same toggle the capture form's
     * person and medication chips use.
     */
    @Test
    fun tappingTheChosenOneAgainClearsIt() {
        var saved: ViolationDraft? = null
        showForm(onSave = { saved = it })

        compose.onNodeWithTag(ViolationTags.ABOUT).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.incident(fall.id)).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.incident(fall.id)).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()

        assertNull(saved?.incidentId)
        // And saving still worked, because none of this was ever required.
        assertEquals(instruction.id, saved?.instructionId)
    }

    /**
     * **An offer to link to nothing is a control that does nothing**, rule 11.
     * A notebook with no incidents and no bills does not draw the disclosure.
     */
    @Test
    fun withNothingToPointAtTheFormDoesNotOffer() {
        showForm(incidents = emptyList(), bills = emptyList())
        compose.onNodeWithTag(ViolationTags.ABOUT).assertDoesNotExist()
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().assertIsDisplayed()
    }

    /**
     * Right to left against a forced layout direction, per D141 and rule 24.
     * The chips and the disclosure are the new structure here, so it is the
     * structure that is walked.
     */
    @Test
    fun theFormHoldsRightToLeft() {
        var saved: ViolationDraft? = null
        showForm(direction = LayoutDirection.Rtl, onSave = { saved = it })

        compose.onNodeWithTag(ViolationTags.ABOUT).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.bill(ambulance.id)).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()

        assertEquals(ambulance.id, saved?.billId)
    }

    /**
     * The date the draft carries is a real EDTF day rather than a stamp nobody
     * can correct later, which is rule 17's floor for anything this writes.
     */
    @Test
    fun theDraftCarriesADayPrecisionDate() {
        var saved: ViolationDraft? = null
        showForm(onSave = { saved = it })
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()
        assertEquals(Edtf.Precision.DAY, saved?.occurred?.precision)
    }

    // ---- correcting one, which nothing could do -----------------------------

    /**
     * **Each time is its own door now.** Inside the card it was part of one
     * merged announcement whose only action opened the acknowledgment sheet, so
     * a person tapping their own sentence got somebody else's screen.
     */
    @Test
    fun aRecordedTimeOpensForCorrection() {
        var opened: Repository.Violation? = null
        showList(
            listOf(violation("v1", "They took her without telling me")),
            onOpenViolation = { opened = it },
        )
        compose.onNodeWithTag(InstructionTags.violationRow("v1"))
            .performScrollTo()
            .performClick()
        assertEquals("v1", opened?.id)
    }

    /**
     * **A correction opens on what is already written down**, D147, including
     * the link, which the disclosure must not fold away behind a control that
     * says "Say what this was about".
     */
    @Test
    fun aCorrectionOpensOnWhatIsAlreadyThere() {
        showForm(
            existing = violation(
                "v1",
                "They took her without telling me",
                incidentTitle = fall.title,
            ),
        )
        compose.onNodeWithText("They took her without telling me", substring = true)
            .assertIsDisplayed()
        // The disclosure is open rather than closed, so the chosen incident is
        // visible without anybody hunting for it.
        compose.onNodeWithTag(ViolationTags.incident(fall.id))
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag(ViolationTags.ABOUT).assertDoesNotExist()
    }

    @Test
    fun aCorrectionCarriesTheTimeItIsCorrecting() {
        var saved: ViolationDraft? = null
        showForm(existing = violation("v1", "Half a sen"), onSave = { saved = it })
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()
        assertEquals("v1", saved?.violationId)
        assertEquals("Half a sen", saved?.note)
    }

    /**
     * Rule 17. The form stamped `LocalDate.now()` and drew no control at all,
     * so a time written down a month later was dated today at full day
     * precision and could never be changed.
     */
    @Test
    fun whenItHappenedCanBeSaidToBeUnknown() {
        var saved: ViolationDraft? = null
        showForm(onSave = { saved = it })
        compose.onNodeWithTag(ViolationTags.UNKNOWN_DATE).performScrollTo().performClick()
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()
        assertEquals(Edtf.Precision.UNKNOWN, saved?.occurred?.precision)
    }

    /** A time already written down keeps its own date rather than gaining today's. */
    @Test
    fun aCorrectionKeepsTheDateItWasGiven() {
        var saved: ViolationDraft? = null
        showForm(existing = violation("v1", "Nobody called"), onSave = { saved = it })
        compose.onNodeWithTag(ViolationTags.SAVE).performScrollTo().performClick()
        assertEquals("2026-08-05", saved?.occurred?.canonical)
    }

    /** **Removal belongs to a record that exists**, and nowhere else. D135. */
    @Test
    fun writingOneDownOffersNoWayToRemoveIt() {
        showForm()
        compose.onNodeWithTag(ViolationTags.REMOVE).assertDoesNotExist()
    }

    /**
     * **It opens the confirmation rather than removing anything itself**, which
     * is why the screen only reports the tap. The sheet is the shell's.
     */
    @Test
    fun aCorrectionCanTakeTheTimeOffTheRecord() {
        var asked = false
        showForm(existing = violation("v1", "Nobody called"), onRemove = { asked = true })
        compose.onNodeWithTag(ViolationTags.REMOVE).performScrollTo().performClick()
        assertEquals(true, asked)
    }
}
