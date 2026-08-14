package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import com.kamsiob.healthtrail.ui.screens.SectionTags
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.DateKindEditSheet
import com.kamsiob.healthtrail.ui.screens.KindEditTags
import com.kamsiob.healthtrail.ui.screens.ProjectDateKindsScreen
import com.kamsiob.healthtrail.ui.screens.ProjectKindsTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Changing the kinds of date a project offers. `DESIGN.md` 20.5 screen 18.
 *
 * **These are the chips somebody taps when they write a date down**, and until
 * now they could only be added to: a template that offered "Renewal" to a
 * process that never renews left a chip in the way forever.
 */
@RunWith(AndroidJUnit4::class)
class ProjectDateKindsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings by lazy { Strings.load(context, Locale.ENGLISH) }

    private val kinds = listOf(
        Repository.ProjectDateKind("k1", "Filing deadline"),
        Repository.ProjectDateKind("k2", "Renewal"),
    )

    private var added: String? = null
    private var opened: Repository.ProjectDateKind? = null

    private fun showList(list: List<Repository.ProjectDateKind> = kinds) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    ProjectDateKindsScreen(
                        projectName = "Medicaid application",
                        kinds = list,
                        onAdd = { added = it },
                        onOpen = { opened = it },
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun aKindCanBeAddedAndTheFieldIsTheOnlyThingThatStopsIt() {
        showList()
        compose.onNodeWithTag(ProjectKindsTags.ADD).assertIsNotEnabled()
        compose.onNodeWithTag(ProjectKindsTags.ADD_FIELD).performTextInput("Response window")
        // **Scrolled to before it is touched.** `SectionScaffold` renders
        // through a `LazyColumn`, so a control below the fold is composed but
        // its center can be outside the window, and a tap there lands nowhere:
        // the click reports success and nothing happens, which is why this
        // failed on the value rather than on the node. It began after the type
        // ladder was lifted on 2026-08-13 and the content above grew. D154.
        compose.onNodeWithTag(SectionTags.root(ProjectKindsTags.NAME))
            .performScrollToNode(hasTestTag(ProjectKindsTags.ADD))
        compose.onNodeWithTag(ProjectKindsTags.ADD).performClick()
        assertEquals("Response window", added)
    }

    @Test
    fun tappingAKindOpensThatKind() {
        showList()
        compose.onNodeWithTag(ProjectKindsTags.kind("k2")).performClick()
        assertEquals("k2", opened?.id)
    }

    /**
     * A project with no kinds says so as "not yet", per rule 13, and says the
     * other way to record a date rather than making the list feel required.
     */
    @Test
    fun noKindsReadsAsNotYetAndNamesTheOtherWay() {
        showList(emptyList())
        compose.onNodeWithText(strings["project.kinds.none"]).assertIsDisplayed()
    }

    private var saved: String? = null
    private var removed = 0

    private fun showSheet(kind: Repository.ProjectDateKind) {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides strings) {
                HealthTrailTheme {
                    DateKindEditSheet(
                        kind = kind,
                        onSave = { saved = it },
                        onRemove = { removed++ },
                        onDismiss = {},
                    )
                }
            }
        }
    }

    /**
     * The sheet says what this list is and is not.
     *
     * Renaming a kind does not reach back into dates already written down, and
     * removing one does not take the date with it. Leaving somebody to find
     * that out by trying it is the app asking them to understand how it stores
     * things, which is rule 20.
     */
    @Test
    fun theSheetSaysDatesAlreadyWrittenDownKeepTheirWords() {
        showSheet(kinds[1])
        compose.onNodeWithText(strings["project.kinds.keeps_dates"]).assertIsDisplayed()
    }

    @Test
    fun theSheetSavesTheNewName() {
        showSheet(kinds[0])
        compose.onNodeWithTag(KindEditTags.NAME).performTextClearance()
        compose.onNodeWithTag(KindEditTags.NAME).performTextInput("Response window")
        compose.onNodeWithTag(KindEditTags.SAVE).performClick()
        assertEquals("Response window", saved)
    }

    @Test
    fun anEmptyNameCannotBeSaved() {
        showSheet(kinds[0])
        compose.onNodeWithTag(KindEditTags.NAME).performTextClearance()
        compose.onNodeWithTag(KindEditTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun removingIsOfferedPlainlyAndReportsOnce() {
        showSheet(kinds[1])
        compose.onNodeWithTag(KindEditTags.REMOVE).performClick()
        assertEquals(1, removed)
    }
}
