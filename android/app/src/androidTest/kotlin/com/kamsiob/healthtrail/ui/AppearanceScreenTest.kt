package com.kamsiob.healthtrail.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.i18n.LocalStrings
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.ui.screens.AppearanceScreen
import com.kamsiob.healthtrail.ui.screens.AppearanceTags
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import com.kamsiob.healthtrail.ui.theme.ThemeChoice
import com.kamsiob.healthtrail.ui.theme.ThemeSetting
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Appearance, and the promise that a setting which needs a restart is a setting
 * that did not take.
 */
@RunWith(AndroidJUnit4::class)
class AppearanceScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(
        initial: ThemeChoice = ThemeChoice.DEFAULT,
        locale: Locale = Locale.ENGLISH,
    ) {
        val strings = Strings.load(context, locale)
        compose.setContent {
            var choice by remember { mutableStateOf(initial) }
            HealthTrailTheme {
                CompositionLocalProvider(LocalStrings provides strings) {
                    AppearanceScreen(choice = choice, onChoose = { choice = it })
                }
            }
        }
    }

    @Test
    fun allThreeChoicesAreOfferedAndFollowingThePhoneIsTheDefault() {
        show()
        ThemeChoice.entries.forEach {
            compose.onNodeWithTag(AppearanceTags.option(it)).assertIsDisplayed()
        }
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.FOLLOW_SYSTEM))
            .assertIsSelected()
    }

    @Test
    fun choosingOneSelectsItAndDeselectsTheOthers() {
        show()
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.DARK)).performClick()

        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.DARK)).assertIsSelected()
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.LIGHT)).assertIsNotSelected()
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.FOLLOW_SYSTEM))
            .assertIsNotSelected()
    }

    @Test
    fun theSelectionIsStateAndNotOnlyAMark() {
        // A screen reader user cannot see the dot. Without `selected` in the
        // semantics the three rows are indistinguishable to them, which is the
        // difference between labeling a control and describing it.
        show(initial = ThemeChoice.LIGHT)
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.LIGHT)).assertIsSelected()
    }

    // Two tests rather than one, because `setContent` may only be called once
    // per rule and calling it twice fails in a way that reads like a layout
    // problem rather than a test-harness one.

    @Test
    fun itHoldsUpInTheLongestLanguage() {
        show(locale = Locale("es"))
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.DARK)).assertIsDisplayed()
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.FOLLOW_SYSTEM))
            .assertIsDisplayed()
    }

    @Test
    fun itHoldsUpInRightToLeft() {
        show(locale = Locale("ar"))
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.DARK)).assertIsDisplayed()
        compose.onNodeWithTag(AppearanceTags.option(ThemeChoice.FOLLOW_SYSTEM))
            .assertIsDisplayed()
    }

    @Test
    fun theChoiceSurvivesBeingWrittenAndReadBack() {
        // The persistence half. A theme that resets on next launch is worse
        // than no setting, because the person has to set it every time.
        val setting = ThemeSetting(context)
        val before = setting.read()
        try {
            ThemeChoice.entries.forEach { choice ->
                setting.write(choice)
                assertEquals(
                    "the stored theme did not read back as what was written",
                    choice,
                    ThemeSetting(context).read(),
                )
            }
        } finally {
            setting.write(before)
        }
    }

    @Test
    fun anUnrecognizedStoredValueFallsBackToFollowingThePhone() {
        // A value written by a future build, or a corrupted preference. It must
        // not crash and it must not pick something arbitrary.
        assertEquals(ThemeChoice.FOLLOW_SYSTEM, ThemeChoice.of(null))
        assertEquals(ThemeChoice.FOLLOW_SYSTEM, ThemeChoice.of("SEPIA"))
        assertEquals(ThemeChoice.FOLLOW_SYSTEM, ThemeChoice.of(""))
    }

    @Test
    fun theStoredFormIsTheNameSoReorderingCannotChangeSomebodysChoice() {
        // An ordinal is a number whose meaning lives somewhere else. Inserting
        // a value into the enum would silently move everybody's setting.
        ThemeChoice.entries.forEach { assertEquals(it.name, it.stored) }
        assertEquals(ThemeChoice.DARK, ThemeChoice.of("DARK"))
    }
}
