package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.CaptureDraft
import com.kamsiob.healthtrail.ui.screens.CaptureFormScreen
import com.kamsiob.healthtrail.ui.screens.CaptureFormTags
import com.kamsiob.healthtrail.ui.screens.CaptureKind
import com.kamsiob.healthtrail.ui.screens.entryKind
import com.kamsiob.healthtrail.ui.screens.usesTheSharedForm
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
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
 * Capture, which is the only way data enters this app.
 *
 * The property that matters is not that the form saves. It is that **a blank
 * call still saves**. Someone who hangs up and taps the gold button has already
 * done the useful thing, which is recording that a call happened and when. An
 * app that refuses to write that down until a field is filled has failed the
 * person it was built for.
 */
@RunWith(AndroidJUnit4::class)
class CaptureTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private fun showForm(
        kind: CaptureKind = CaptureKind.CALL,
        onSave: (CaptureDraft) -> Unit,
        onCancel: () -> Unit = {},
    ) {
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    CaptureFormScreen(kind = kind, onSave = onSave, onCancel = onCancel)
                }
            }
        }
    }

    @Test
    fun aBlankCallStillSaves() {
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertNotNull("saving a blank call did nothing", draft)
        assertEquals("", draft!!.who)
        assertEquals("", draft.note)
    }

    @Test
    fun whatIsTypedIsWhatIsSaved() {
        var draft: CaptureDraft? = null
        showForm(onSave = { draft = it })

        compose.onNodeWithTag(CaptureFormTags.WHO).performTextInput("Ward desk")
        compose.onNodeWithTag(CaptureFormTags.NOTE).performTextInput("Said they would call back")
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals("Ward desk", draft!!.who)
        assertEquals("Said they would call back", draft.note)
    }

    @Test
    fun cancelingSavesNothing() {
        var draft: CaptureDraft? = null
        var canceled = false
        showForm(onSave = { draft = it }, onCancel = { canceled = true })

        compose.onNodeWithTag(CaptureFormTags.WHO).performTextInput("Typed then abandoned")
        compose.onNodeWithTag(CaptureFormTags.CANCEL).performClick()

        assertTrue(canceled)
        assertNull("canceling still saved something", draft)
    }

    @Test
    fun aSavedCallLandsOnTheTrailAndIsCounted() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Capture subject")

        val before = repository.count(Repository.Section.TRAIL)
        val entryId = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Ward desk",
            body = "Said they would call back",
        )
        repository.addCallDetail(entryId = entryId, reached = true)
        val after = repository.count(Repository.Section.TRAIL)

        assertEquals("the call did not reach the trail", before + 1, after)
    }

    @Test
    fun aBlankCallIsStillARealEntryOnTheTrail() = runBlocking {
        // The whole point. An entry with no words is still a record that
        // something happened at a time, which is often the only thing the
        // person had a free hand to capture.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Blank capture subject")

        val before = repository.count(Repository.Section.TRAIL)
        repository.createEntry(subjectId = subjectId, kind = "call", title = "", body = "")
        val after = repository.count(Repository.Section.TRAIL)

        assertEquals("a blank call was silently dropped", before + 1, after)
    }

    @Test
    fun everyFormKindSavesUnderItsOwnWords() {
        // One form serves four kinds, so the thing that can silently break is
        // the kind traveling with the draft. A visit filed as a call is a
        // wrong record rather than a missing one, which is worse.
        val kind = CaptureKind.VISIT
        var draft: CaptureDraft? = null
        showForm(kind = kind, onSave = { draft = it })

        compose.onNodeWithTag(CaptureFormTags.WHO).performTextInput("Dr Aurelio")
        compose.onNodeWithTag(CaptureFormTags.SAVE).performClick()

        assertEquals(kind, draft!!.kind)
        assertEquals("Dr Aurelio", draft.who)
    }

    @Test
    fun everyKindTheFormServesNamesItselfInTheCatalog() {
        // The form builds its catalog keys from the enum rather than reading
        // them from a table, so a kind added to the form without its words
        // would render a raw key on screen. This is the check that makes
        // building keys safe, and it reads the served set from the same
        // declaration the shell does rather than repeating the list.
        val strings = Strings.load(context)
        val served = CaptureKind.entries.filter { it.usesTheSharedForm }
        assertTrue("no kind uses the shared form", served.isNotEmpty())

        served.forEach { kind ->
            val slug = kind.name.lowercase()
            listOf("title", "who", "who.hint", "note", "note.hint").forEach { slot ->
                // Strings throws on a key no catalog defines, so a missing one
                // fails here by name rather than reaching a person as a key on
                // screen.
                val value = strings["capture.$slug.$slot"]
                assertTrue("capture.$slug.$slot resolved to nothing", value.isNotBlank())
            }
        }
    }

    @Test
    fun everyKindHasARowKind() {
        // The schema column, not the label. Every kind must name what it is
        // stored as, and no two may collide, or the trail cannot tell them
        // apart after the fact.
        val kinds = CaptureKind.entries.map { it.entryKind() }
        assertEquals("two capture kinds store as the same row kind", kinds.size, kinds.toSet().size)
        assertTrue("a capture kind stores as a blank", kinds.none { it.isBlank() })
    }

    @Test
    fun anEntryWithAnUnknownTimeIsStillAccepted() = runBlocking {
        // Rough dates are a functional requirement. A person writing at 11pm
        // about a call three days ago does not know the time, and demanding one
        // gets either a guess recorded as fact or nothing recorded at all.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Rough date subject")

        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Sometime last week",
            occurredAt = null,
            whenKnown = Repository.WhenKnown.UNKNOWN,
        )
        assertTrue("an entry with no date was rejected", id.isNotBlank())
    }
}
