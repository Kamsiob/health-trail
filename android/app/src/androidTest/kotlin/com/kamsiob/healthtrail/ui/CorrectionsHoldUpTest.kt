package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.kamsiob.healthtrail.ui.v4.PageTags
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AddThreadScreen
import com.kamsiob.healthtrail.ui.screens.AddThreadTags
import com.kamsiob.healthtrail.ui.screens.CorrectMeasureScreen
import com.kamsiob.healthtrail.ui.screens.MeasurementTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The six corrections at a forced right to left direction and at the largest
 * font, which is what closing them properly still owed.
 *
 * **D141: right to left is verified against a forced layout direction rather
 * than Arabic content.** Version one ships English, and a screen that mirrors
 * correctly with English in it is the thing that check is for. The four
 * catalogs stay complete separately.
 *
 * **Font scale 2.0 is the other half.** `DESIGN.md` 16.4 will not let an issue
 * close without it, and a correction screen is the one place somebody is
 * reading their own words back: if the field holding them is clipped or off
 * screen at the largest type, the screen has failed at the only job it has.
 *
 * **What this cannot do is say whether it looks right.** That is the device
 * pass, and tonight's was light theme at scale 1.0. Dark is still owed.
 */
@RunWith(AndroidJUnit4::class)
class CorrectionsHoldUpTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings get() = Strings.load(context)

    /** Composed mirrored, at the largest type, or both. */
    private fun show(
        mirrored: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            HealthTrailTheme {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalLayoutDirection provides
                        if (mirrored) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = fontScale,
                    ),
                ) {
                    content()
                }
            }
        }
    }

    @Composable
    private fun NameCorrection() {
        AddThreadScreen(
            onStart = {},
            onCancel = {},
            titleKey = "chapters.rename.title",
            labelKey = "chapters.rename.name",
            hintKey = null,
            saveKey = "chapters.rename.save",
            leadKey = "chapters.rename.lead",
            section = Repository.Section.CHAPTERS,
            initialName = "Maplewood Care Center",
        )
    }

    @Test
    fun aNameCorrectionMirrorsAndKeepsWhatIsInTheField() {
        // **The words being corrected are the person's own**, so they carry
        // their own direction inside a layout running the other way. If the
        // field came back empty or the save went missing, a mirrored notebook
        // could not correct anything.
        show(mirrored = true) { NameCorrection() }

        compose.onNodeWithText("Maplewood Care Center").assertIsDisplayed()
        compose.onNodeWithTag(AddThreadTags.START).assertIsDisplayed()
        // The way out is the arrow in the corner now: a cancel action beside
        // a back arrow was two controls that leave. #387.
        compose.onNodeWithTag(PageTags.BACK).assertIsDisplayed()
    }

    @Test
    fun aNameCorrectionSurvivesTheLargestType() {
        // **The one screen where somebody reads their own words back.** At
        // font scale 2.0 the title, the lead and the label all grow, and the
        // field holding the name has to stay on screen with the save under it.
        show(fontScale = 2f) { NameCorrection() }

        compose.onNodeWithTag(AddThreadTags.NAME).assertIsDisplayed()
        compose.onNodeWithTag(AddThreadTags.START).assertIsDisplayed()
    }

    @Test
    fun aWordsCorrectionSurvivesTheLargestTypeWithASentenceInIt() {
        // The multiline path, which is a different composable inside the field
        // and the one most likely to push the save off the bottom.
        show(fontScale = 2f) {
            AddThreadScreen(
                onStart = {},
                onCancel = {},
                titleKey = "ask.correct.title",
                labelKey = "ask.correct.field",
                hintKey = null,
                saveKey = "ask.correct.save",
                leadKey = "ask.correct.lead",
                section = Repository.Section.ASK_NEXT_TIME,
                initialName = "When was the last time she was weighed?",
                singleLine = false,
            )
        }

        compose.onNodeWithTag(AddThreadTags.NAME).assertIsDisplayed()
        compose.onNodeWithTag(AddThreadTags.START).assertIsDisplayed()
    }

    @Test
    fun aMeasureCorrectionMirrorsWithItsKindStillSaid() {
        // **The kind is a sentence rather than a chip on a correction**, #374's
        // reasoning, and a sentence has to survive mirroring like anything
        // else. This would have caught it being drawn as a chip again.
        show(mirrored = true) {
            CorrectMeasureScreen(
                measure = Repository.Measure(
                    id = "m1",
                    name = "Weight",
                    presetId = null,
                    unit = "lb",
                    isText = false,
                ),
                onSave = {},
                onCancel = {},
            )
        }

        // **The kind's words alone would not catch a regression here**, because
        // the chip carries the same string as the sentence: a test asserting
        // "Number" is displayed passes either way. The absence of the chip is
        // what says it is a sentence. Watched failing against `kindIsFixed`
        // set back to false.
        assertTrue(
            "the kind is a chip again on a measure that has readings",
            compose.onAllNodesWithTag(MeasurementTags.OWN_TEXT).fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithText(strings["measurement.own.kind.number"]).assertIsDisplayed()
        compose.onNodeWithTag(MeasurementTags.OWN_NAME).assertIsDisplayed()
        compose.onNodeWithTag(MeasurementTags.OWN_START).assertIsDisplayed()
    }
}
