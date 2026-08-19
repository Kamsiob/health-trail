package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AddThreadScreen
import com.kamsiob.healthtrail.ui.screens.AddThreadTags
import com.kamsiob.healthtrail.ui.screens.ChapterScreen
import com.kamsiob.healthtrail.ui.screens.ChapterTags2
import com.kamsiob.healthtrail.ui.screens.SectionTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Correcting a name, on the two records that could not be corrected.
 *
 * **This is the defect four separate audits and six walkthroughs each found on
 * their own**: a record somebody typed and cannot fix. It was fixed everywhere
 * it could be fixed without a new full screen surface, and `renameChapter` and
 * `renameProject` sat in the repository with no caller because `NotebookShell`
 * was at the JVM's 64KB method limit and had no room for one.
 *
 * **They were deliberately kept out of `docs/REMOVAL-LEDGER.md`**, per D152: a
 * ledger row would have told the next reader the app had decided somebody may
 * not fix a name they typed wrong.
 *
 * #373 made the room. Both surfaces together cost the shell 220 bytes, against
 * the 1,860 of headroom it had before that pass, when one added parameter on an
 * existing screen was enough to fail the build. #374.
 */
@RunWith(AndroidJUnit4::class)
class RenameSurfacesTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings get() = Strings.load(context)

    private fun chapter() = Repository.Chapter(
        id = "c1",
        // Typed wrong at two in the morning, which is the whole point.
        name = "Maplewwod",
        reason = null,
        notes = null,
        startedEdtf = "2026-01-04",
        endedEdtf = null,
    )

    @Test
    fun aChapterOffersTheWayToCorrectItsName() {
        // **A chapter is the app's unit of "where".** It titles its own screen,
        // heads a run in the month review, and is printed on the documents this
        // app hands to other people. Setup asks for it in one line at two in
        // the morning, which is exactly when it gets typed wrong.
        var asked = false
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    ChapterScreen(
                        detail = Repository.ChapterDetail(
                            chapter = chapter(),
                            entries = emptyList(),
                            incidents = emptyList(),
                            documents = emptyList(),
                            milestones = emptyList(),
                        ),
                        onOpenEntry = {},
                        onOpenIncident = {},
                        onBack = {},
                        onRename = { asked = true },
                    )
                }
            }
        }

        // **Scrolled to by the list**, because `SectionScaffold` is a
        // `LazyColumn` and a control below the fold does not exist yet.
        compose.onNodeWithTag(SectionTags.root(ChapterTags2.NAME))
            .performScrollToNode(hasTestTag(ChapterTags2.RENAME))
        compose.onNodeWithTag(ChapterTags2.RENAME).assertIsDisplayed().performClick()

        assertEquals("the chapter's rename did not open anything", true, asked)
    }

    @Test
    fun theNamingScreenStartsFromTheNameItIsCorrecting() {
        // **A correction starts with what is there**, per D147: somebody who
        // opened this to fix one letter should not retype the whole name. That
        // is what `initialName` is for, and it is the reason the screen could
        // not simply be reused as it stood, since it read its starting value
        // from a care thread and a chapter is not one.
        var saved: String? = null
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    AddThreadScreen(
                        onStart = { saved = it },
                        onCancel = {},
                        titleKey = "chapters.rename.title",
                        labelKey = "chapters.rename.name",
                        hintKey = null,
                        saveKey = "chapters.rename.save",
                        leadKey = "chapters.rename.lead",
                        section = Repository.Section.CHAPTERS,
                        initialName = "Maplewwod",
                    )
                }
            }
        }

        // The typo, corrected the way somebody would: fix it, save it.
        compose.onNodeWithTag(AddThreadTags.NAME).performTextClearance()
        compose.onNodeWithTag(AddThreadTags.NAME).performTextInput("Maplewood")
        compose.onNodeWithTag(AddThreadTags.START).performClick()

        assertEquals("the corrected name did not come back", "Maplewood", saved)
    }

    @Test
    fun theNamingScreenWearsTheSectionItWasOpenedFrom() {
        // **D151, one header system**: a writing screen wears its section's tab
        // chip over a `displayM` title. A chapter's rename that wore the care
        // threads chip would tell somebody they had ended up somewhere else.
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    AddThreadScreen(
                        onStart = {},
                        onCancel = {},
                        titleKey = "chapters.rename.title",
                        leadKey = "chapters.rename.lead",
                        section = Repository.Section.CHAPTERS,
                        initialName = "Maplewood",
                    )
                }
            }
        }

        // **Unmerged, because the chip merges into the header's node.** The
        // failure says so itself, which is the one Compose test message that
        // tells you exactly what to do.
        compose.onNodeWithText(strings["notebook.section.chapters"], useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText(strings["chapters.rename.title"], substring = true)
            .assertExists()
    }
}
