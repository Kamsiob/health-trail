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
import com.kamsiob.healthtrail.ui.screens.CallDraft
import com.kamsiob.healthtrail.ui.screens.LogCallScreen
import com.kamsiob.healthtrail.ui.screens.LogCallTags
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

    private fun showCallForm(onSave: (CallDraft) -> Unit, onCancel: () -> Unit = {}) {
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    LogCallScreen(onSave = onSave, onCancel = onCancel)
                }
            }
        }
    }

    @Test
    fun aBlankCallStillSaves() {
        var draft: CallDraft? = null
        showCallForm(onSave = { draft = it })

        compose.onNodeWithTag(LogCallTags.SAVE).performClick()

        assertNotNull("saving a blank call did nothing", draft)
        assertEquals("", draft!!.who)
        assertEquals("", draft.note)
    }

    @Test
    fun whatIsTypedIsWhatIsSaved() {
        var draft: CallDraft? = null
        showCallForm(onSave = { draft = it })

        compose.onNodeWithTag(LogCallTags.WHO).performTextInput("Ward desk")
        compose.onNodeWithTag(LogCallTags.NOTE).performTextInput("Said they would call back")
        compose.onNodeWithTag(LogCallTags.SAVE).performClick()

        assertEquals("Ward desk", draft!!.who)
        assertEquals("Said they would call back", draft.note)
    }

    @Test
    fun cancellingSavesNothing() {
        var draft: CallDraft? = null
        var cancelled = false
        showCallForm(onSave = { draft = it }, onCancel = { cancelled = true })

        compose.onNodeWithTag(LogCallTags.WHO).performTextInput("Typed then abandoned")
        compose.onNodeWithTag(LogCallTags.CANCEL).performClick()

        assertTrue(cancelled)
        assertNull("cancelling still saved something", draft)
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
