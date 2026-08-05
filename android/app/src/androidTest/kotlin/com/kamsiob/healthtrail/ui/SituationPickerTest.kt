package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Bidi
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.SituationPickerScreen
import com.kamsiob.healthtrail.ui.screens.SituationPickerTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The situation picker, and the promises it has to keep.
 *
 * Two of these are about content rather than behavior, because the content is
 * where this screen can go wrong in ways that matter: the posture strings have
 * to appear verbatim, and choosing must never be something that happens by
 * looking.
 */
@RunWith(AndroidJUnit4::class)
class SituationPickerTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private fun show(
        onChoose: (TemplateCatalog.Situation) -> Unit = {},
        onSkip: () -> Unit = {},
    ): TemplateCatalog.Situations {
        val catalog = runBlocking { TemplateCatalog.situations(context) }
        val strings = Strings.load(context)
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    SituationPickerScreen(catalog, onChoose = onChoose, onSkip = onSkip)
                }
            }
        }
        return catalog
    }

    @Test
    fun theCatalogReachesTheScreenIntact() {
        val catalog = show()
        assertEquals("expected the 14 shipped care settings", 14, catalog.all.size)
        catalog.all.forEach {
            assertTrue("a setting has no name", it.name.isNotBlank())
            assertTrue("${it.id} has no id", it.id.isNotBlank())
        }
        compose.onNodeWithTag(SituationPickerTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun choosingRequiresATapAndNothingIsAppliedByLooking() {
        var chosen: TemplateCatalog.Situation? = null
        val catalog = show(onChoose = { chosen = it })

        // Rendering the list must not select anything.
        assertEquals("something was chosen just by showing the screen", null, chosen)

        val first = catalog.all.first()
        compose.onNodeWithTag(SituationPickerTags.row(first.id)).performClick()

        assertNotNull("tapping a setting did not choose it", chosen)
        assertEquals(first.id, chosen!!.id)
    }

    @Test
    fun notSureYetIsARealAnswer() {
        var skipped = false
        show(onSkip = { skipped = true })

        compose.onNodeWithTag(SituationPickerTags.SKIP).performClick()

        assertTrue("not sure yet did nothing, which would make it a dead end", skipped)
    }

    @Test
    fun everySettingIsReachableByScrolling() {
        // Grouping is presentation. It may never cost anyone their own
        // situation, so every one of the fourteen is still reachable, including
        // any the grouping did not account for.
        val catalog = show()
        catalog.all.forEach { situation ->
            compose.onNodeWithTag(SituationPickerTags.LIST)
                .performScrollToKey(situation.id)
            compose.onNodeWithTag(SituationPickerTags.row(situation.id)).assertIsDisplayed()
        }
    }

    @Test
    fun everySettingHasAGroupThePickerKnowsAbout() {
        // The one that catches a catalog edit adding a fifteenth setting under
        // a heading this version has never heard of. It would still render,
        // under no heading, which is the safe outcome rather than the intended
        // one, so this fails loudly instead of letting it look deliberate.
        val catalog = runBlocking { TemplateCatalog.situations(context) }
        val known = setOf("facility", "home", "treatment", "comfort")
        val strays = catalog.all.filter { it.group !in known }
        assertTrue(
            "settings with an unknown group: ${strays.map { "${it.id}=${it.group}" }}",
            strays.isEmpty(),
        )
    }

    @Test
    fun everyGroupIsHeaded() {
        show()
        listOf("facility", "home", "treatment", "comfort").forEach { group ->
            compose.onNodeWithTag(SituationPickerTags.LIST)
                .performScrollToKey("group_$group")
            compose.onNodeWithTag(SituationPickerTags.group(group)).assertIsDisplayed()
        }
    }

    @Test
    fun everySettingShowsItsSubtitleSoTwoSimilarOnesCanBeToldApart() {
        // A nursing home and assisted living are one word apart on this screen
        // and are not the same thing. The subtitle exists to carry that, and a
        // name on its own forces a guess this audience should not have to make.
        val catalog = show()
        catalog.all.filter { it.subtitle.isNotBlank() }.forEach { situation ->
            compose.onNodeWithTag(SituationPickerTags.LIST)
                .performScrollToKey(situation.id)
            // **Isolated, because that is what the screen actually renders.**
            // The catalog is English inside a layout that may be right to left,
            // #62, so every template name and subtitle goes through
            // `Bidi.isolate` and carries U+2068 and U+2069. Comparing against
            // the bare catalog string asserts a screen this app does not draw.
            compose.onNodeWithText(Bidi.isolate(situation.subtitle)).assertIsDisplayed()
        }
    }

    @Test
    fun theScreenSaysNothingHereIsPermanentWithoutScrolling() {
        // The sentence that removes most of the pressure from this screen, and
        // it is worth nothing if a person has to scroll fourteen options to
        // find it. It sits above the skip action, pinned, with the list
        // scrolling behind it.
        show()
        compose.onNodeWithTag(SituationPickerTags.NOT_PERMANENT).assertIsDisplayed()
        compose.onNodeWithTag(SituationPickerTags.SKIP).assertIsDisplayed()
    }

    @Test
    fun applyingASettingRecordsItAndCreatesItsThreads() = runBlocking {
        val repository = Repository.open(context)
        val catalog = TemplateCatalog.situations(context)

        // A setting that actually ships with threads, so the assertion means
        // something rather than passing on an empty list.
        val situation = catalog.all.first { it.threads.isNotEmpty() }
        val subjectId = repository.createSubject(displayName = "Template subject")

        val threadsBefore = repository.count(Repository.Section.THREADS, subjectId)
        repository.applySituation(
            subjectId = subjectId,
            templateId = situation.id,
            threads = situation.threads.map { it.id to it.label },
        )
        val threadsAfter = repository.count(Repository.Section.THREADS, subjectId)

        assertEquals(
            "applying did not create one care thread per offered thread",
            threadsBefore + situation.threads.size,
            threadsAfter,
        )

        val subject = repository.subject(subjectId)
        assertNotNull(subject)
        assertEquals(
            "the notebook does not record which setting configured it",
            situation.id,
            subject!!.situationTemplateId,
        )
    }

    @Test
    fun thePostureStringsAreCarriedVerbatim() = runBlocking {
        // templates/SCHEMA.md: these are shown exactly as written, because they
        // are the sentences that keep this content reading as structure rather
        // than as advice. Paraphrasing them in the interface is forbidden.
        val catalog = TemplateCatalog.situations(context)
        assertTrue("the general guide posture string is missing", catalog.posture.generalGuide.isNotBlank())
        assertTrue("the record only posture string is missing", catalog.posture.recordOnly.isNotBlank())

        val raw = context.assets.open("templates/situations.json")
            .bufferedReader().use { it.readText() }
        assertTrue(
            "the general guide string was altered on its way to the screen",
            raw.contains(catalog.posture.generalGuide),
        )
    }
}
