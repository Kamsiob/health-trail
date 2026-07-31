package com.kamsiob.healthtrail.i18n

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Two things about the app's own words that nothing else checks.
 *
 * `check_i18n.py` already proves every catalog carries the same keys with the
 * same placeholders. It cannot prove that a copy of a string held somewhere in
 * Kotlin still matches the catalog, and it cannot prove that the disclaimer
 * discloses the same amount in every language. Both of those are things a person
 * would only notice by reading four languages side by side, which nobody does.
 */
@RunWith(AndroidJUnit4::class)
class CopyIntegrityTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun thePreviewCatalogHasNotDriftedFromEnglish() {
        // Compose previews cannot read assets, so Strings.preview() keeps a hand
        // written copy of the strings previews use. A copy drifts. This is the
        // check that makes keeping one acceptable: it fails the moment the
        // catalog is edited and the copy is not.
        val english = Strings.load(context, Locale.ENGLISH)
        Strings.previewEntries.forEach { (key, previewValue) ->
            assertEquals(
                "the preview copy of $key no longer matches the English catalog",
                english[key],
                previewValue,
            )
        }
    }

    @Test
    fun theDisclaimerDisclosesTheSameAmountInEveryLanguage() {
        // The one failure this screen cannot have. DESIGN.md section 7 fixes the
        // wording and states that nothing may be cut from it, and a translation
        // that quietly drops a block would disclose less to the people least
        // able to notice.
        val keys = listOf("disclaimer.title", "disclaimer.lead", "disclaimer.accept") +
            (1..3).flatMap { listOf("disclaimer.block.$it.title", "disclaimer.block.$it.body") }

        listOf("en", "es", "zh", "ar").forEach { code ->
            val strings = Strings.load(context, Locale.forLanguageTag(code))
            keys.forEach { key ->
                val value = strings[key]
                assertTrue("$code is missing $key", value.isNotBlank())
            }
        }
    }

    @Test
    fun noLanguageFallsBackToEnglishForTheDisclaimer() {
        // Strings falls back to English for any key a catalog is missing, which
        // is the right behavior everywhere else and is wrong here: a person
        // reading Arabic would get one block in English and no signal that
        // anything was missing. Compared against English rather than merely
        // checked for presence, so a fallback is caught.
        val english = Strings.load(context, Locale.ENGLISH)
        val keys = (1..3).map { "disclaimer.block.$it.body" }

        listOf("es", "zh", "ar").forEach { code ->
            val strings = Strings.load(context, Locale.forLanguageTag(code))
            keys.forEach { key ->
                assertTrue(
                    "$code fell back to the English text for $key",
                    strings[key] != english[key],
                )
            }
        }
    }
}
