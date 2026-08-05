package com.kamsiob.healthtrail.i18n

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Setting the app's language actually changes the app's language.
 *
 * **This is the test that did not exist, and its absence cost Chinese.**
 *
 * Every other locale test passes a `Locale` to `Strings.load` directly. That
 * proves the catalogs parse and that the lookup works, and all of them passed
 * for months while **a person who set this app to Chinese got English**.
 *
 * The catalog was never the problem. The path to it was. `Strings.load` read
 * `Locale.getDefault()`, which is one locale, the first entry of the
 * configuration's list. Asking Android for Chinese produced a configuration of
 * `en-US,zh-Hans`: the requested language was appended rather than promoted,
 * because the app ships no `values-zh` resources for Android to serve, so
 * English ranked higher for resource resolution. Spanish and Arabic happened to
 * land first in their own lists and worked, which made a general resolution bug
 * look like a Chinese one.
 *
 * So this test goes through `LocaleManager`, the same API Android's own
 * per-app language picker uses, rather than through the back door every other
 * test uses. **A test that takes a shortcut the person cannot take proves
 * something the person does not get.**
 *
 * It restores whatever was set before, because it runs on a real phone.
 */
@RunWith(AndroidJUnit4::class)
class AppLanguageTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val manager: LocaleManager?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
        } else {
            null
        }

    private var before: LocaleList? = null

    @After
    fun restore() {
        before?.let { manager?.applicationLocales = it }
    }

    /**
     * Sets the app's language and waits for it to actually be set.
     *
     * **Setting `applicationLocales` is asynchronous**, and reading on the next
     * line reads whatever the previous test left behind. That is not a
     * hypothetical: this class asserted that Japanese falls back to English and
     * twice got Spanish, which is what the test before it had chosen. It passed
     * in isolation both times, which is the worst way for a test to be wrong.
     *
     * **Waiting for the value to come back, rather than for the answer.** The
     * loop confirms the system took the tag; what the app then resolves it to
     * is what the test is actually about, and polling for that would make the
     * assertion prove itself. #306.
     */
    private fun choose(tag: String): Strings {
        val locales = manager ?: error("no LocaleManager")
        if (before == null) before = locales.applicationLocales
        val wanted = LocaleList.forLanguageTags(tag)
        locales.applicationLocales = wanted

        val giveUpAt = SystemClock.uptimeMillis() + SETTLE_MILLIS
        while (locales.applicationLocales != wanted && SystemClock.uptimeMillis() < giveUpAt) {
            SystemClock.sleep(20)
        }
        assertEquals(
            "the system never took the language this test asked for, so whatever " +
                "it asserts next is about the language the test before it chose",
            wanted,
            locales.applicationLocales,
        )
        return Strings.load(context)
    }

    private companion object {
        /**
         * Long enough for a real phone under a full suite, short enough that a
         * genuine failure is a failure rather than a hang.
         */
        const val SETTLE_MILLIS = 3_000L
    }

    @Test
    fun choosingChineseGivesChinese() {
        // The exact failure. `zh-Hans` because a bare `zh` has no script, and
        // Hans and Hant are different writing systems rather than dialects.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        assertEquals("zh", choose("zh-Hans").locale.language)
    }

    @Test
    fun choosingEachShippedLanguageGivesThatLanguage() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        listOf("en" to "en", "es" to "es", "zh-Hans" to "zh", "ar" to "ar")
            .forEach { (tag, expected) ->
                assertEquals(
                    "setting the app language to $tag did not select the $expected catalog",
                    expected,
                    choose(tag).locale.language,
                )
            }
    }

    @Test
    fun theChosenLanguageWinsOverTheDeviceLanguage() {
        // The heart of it. The device is in English and the person has asked
        // this one app for something else. The app language is a deliberate
        // choice and outranks the device default, which is a fallback.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val strings = choose("zh-Hans")
        assertEquals("zh", strings.locale.language)
        assertEquals(
            "the Chinese catalog returned English text",
            "笔记本",
            strings["notebook.title"],
        )
    }

    @Test
    fun anUnshippedLanguageFallsBackToEnglishRatherThanFailing() {
        // Japanese is not shipped. A person whose phone is in Japanese gets a
        // working app in English, never a crash and never empty strings.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        assertEquals("en", choose("ja").locale.language)
    }
}
