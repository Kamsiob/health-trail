package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
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
import com.kamsiob.healthtrail.ui.screens.IncidentScreen
import com.kamsiob.healthtrail.ui.screens.IncidentTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An incident's entries open, which is the other end of a link that had one.
 *
 * **`EntryScreen` opens the incident an entry belongs to**, and this screen
 * drew the same entries as cards that answered nothing. Somebody reading the
 * thread who wanted the whole of one entry had to leave, go to the trail, and
 * find it. Rule 18: a one-way link is a dead end wearing a disguise. #46.
 *
 * **Every other screen that draws entry cards already opens them**: a person's,
 * a thread's, a chapter's and a prep sheet's. This was the one that did not,
 * and it had **no test of any kind**, which is how it lasted. #342 is the wider
 * gap that let it: `ScreenReaderTest` walks 44 of the 75 screens and this was
 * not one of them.
 */
@RunWith(AndroidJUnit4::class)
class IncidentScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val incident = Repository.Incident(
        id = "i1",
        title = "Bruise on her arm nobody could explain",
        description = "Reported to the charge nurse. Asked for it in writing.",
        reportedEdtf = "2026-01-24",
        reportedStart = 1L,
        resolvedAt = null,
        resolutionNote = null,
        chapterName = "Maplewood Care Center",
        entryCount = 2,
    )

    private fun entry(id: String, title: String) = Repository.TrailEntry(
        id = id,
        kind = "call",
        title = title,
        body = "They said they would look into it.",
        occurredEdtf = "2026-02-15",
        occurredStart = 2L,
        createdAt = 2L,
        isUnfiled = false,
        threads = emptyList(),
        pinnedAt = null,
    )

    private val entries = listOf(
        entry("e1", "Reported it to the charge nurse"),
        entry("e2", "Called the unit to ask what had been done"),
    )

    private var opened: String? = null
    private var openedDocument: String? = null

    /**
     * **The paperwork an incident produced, which no seed can reach.** The
     * fixture links no document to an incident, so this is the only place the
     * row is exercised at all, and #360 was that the row did not open while the
     * people above it and the entries below it did.
     */
    private val paperwork = Repository.Document(
        id = "d1",
        title = "The grievance letter, as filed",
        category = null,
        originalLocation = "The blue folder at home",
        notes = null,
        receivedEdtf = "2026-06-14",
        sha256 = null,
        byteSize = null,
    )

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    IncidentScreen(
                        incident = incident,
                        detail = Repository.IncidentDetail(
                            entries = entries,
                            documents = listOf(paperwork),
                            // **What the incident says back**, which is the
                            // other half of rule 18: a violation could name it
                            // and it said nothing at all in return. #371.
                            violations = listOf(
                                Repository.Violation(
                                    id = "v1",
                                    occurredEdtf = "2026-06-15",
                                    note = "The dressing was not changed for two days",
                                    incidentId = "i1",
                                    incidentTitle = null,
                                    billId = null,
                                    billDescription = null,
                                    instructionId = "s1",
                                    instructionName = "Change the dressing daily",
                                ),
                            ),
                        ),
                        onOpenPerson = {},
                        onOpenDocument = { openedDocument = it.id },
                        onOpenEntry = { opened = it.id },
                        onAdd = {},
                        onShare = {},
                        onRemove = {},
                        onCorrect = {},
                        onResolve = {},
                        onReopen = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun anEntryOnTheThreadOpens() {
        show()
        compose.onNodeWithTag(IncidentTags.entry("e2")).performScrollTo().performClick()
        assertEquals("e2", opened)
    }

    /**
     * **And it says what the tap does**, in the same words every other entry
     * card in the app uses. A card that opens something and announces nothing
     * is what #231 was about, and the label is the half no screenshot shows.
     */
    @Test
    fun theCardSaysTheTapOpensTheEntry() {
        show()
        val node = compose.onNodeWithTag(IncidentTags.entry("e1")).fetchSemanticsNode()
        val label = node.config.getOrNull(SemanticsActions.OnClick)?.label
        assertNotNull("the entry card declares no click action at all", label)
        assertEquals(strings["prep.change.open"], label)
    }

    @Test
    fun thePaperworkAnIncidentProducedOpens() {
        show()
        compose.onNodeWithTag(IncidentTags.document("d1")).performClick()
        assertEquals("d1", openedDocument)
    }

    /**
     * **The other half of rule 18**, which four of the five panels named: a
     * violation could point at an incident from the moment the form began to
     * ask, and the incident said nothing back at all.
     */
    @Test
    fun anIncidentSaysWhatWasNotFollowedHere() {
        show()
        compose.onNodeWithText("Change the dressing daily", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("The dressing was not changed for two days", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
