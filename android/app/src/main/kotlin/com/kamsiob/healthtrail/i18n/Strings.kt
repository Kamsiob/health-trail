package com.kamsiob.healthtrail.i18n

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import android.icu.text.MessageFormat
import com.kamsiob.healthtrail.BuildConfig
import org.json.JSONObject
import java.util.Locale

/**
 * User facing text, composed from the shared message catalogs.
 *
 * **Why this exists rather than `res/values/strings.xml`.** The catalogs live in
 * `contract/i18n` and are read by both platforms, so the Android app does not
 * keep a second copy of them any more than it keeps a second copy of the schema.
 * The build copies them into assets and this loads them from there.
 *
 * **Why ICU MessageFormat rather than Android plurals.** A sentence is never
 * assembled by concatenating fragments, because concatenation breaks in every
 * language except English. Composing from a template means plural and date
 * handling belongs to the catalog, where a translator can see it. Arabic needs
 * six plural forms and the catalog carries all six; Android's own `<plurals>`
 * could express that, but converting the catalogs into it would mean a second
 * representation, which is the thing being avoided.
 *
 * `android.icu.text.MessageFormat` is the same ICU implementation the eventual
 * TypeScript engine will use, which is what makes byte identical output across
 * platforms achievable rather than aspirational.
 */
@Immutable
class Strings internal constructor(
    private val entries: Map<String, String>,
    private val fallback: Map<String, String>,
    val locale: Locale,
    /** True when this locale reads right to left. Arabic ships in v1. */
    val isRtl: Boolean,
    /**
     * Whether a native speaker who has dealt with the American care system has
     * reviewed this catalog. Currently false for every locale, and the app says
     * so rather than implying a reviewed translation.
     */
    val reviewedByNativeSpeaker: Boolean,
) {

    /** A string with no arguments. */
    operator fun get(key: String): String = resolve(key)

    /**
     * A string with arguments, formatted through ICU.
     *
     * ```
     * strings("today.open.incidents", "count" to 3)
     * ```
     */
    operator fun invoke(key: String, vararg args: Pair<String, Any>): String {
        val template = resolve(key)
        if (args.isEmpty()) return template
        return runCatching {
            MessageFormat(template, locale).format(mapOf(*args))
        }.getOrElse { error ->
            if (BuildConfig.DEBUG) {
                throw IllegalStateException(
                    "Could not format $key for ${locale.toLanguageTag()}: ${error.message}. " +
                        "Template was: $template",
                    error,
                )
            }
            template
        }
    }

    private fun resolve(key: String): String {
        entries[key]?.let { return it }

        // Falling back to English here cannot mask a missing translation,
        // because check_i18n.py fails the build when the catalogs disagree on
        // even one key. It can only mask code asking for a key that exists in
        // no catalog, which is a bug in the code rather than in a translation.
        // So it is loud in debug and quiet in release: crashing a caregiver
        // over a missing label would be a worse outcome than a visible key.
        fallback[key]?.let {
            if (BuildConfig.DEBUG) {
                throw IllegalStateException(
                    "Missing key $key in ${locale.toLanguageTag()}, which should be " +
                        "impossible because check_i18n.py enforces key parity. " +
                        "Falling back to English."
                )
            }
            return it
        }

        if (BuildConfig.DEBUG) {
            throw IllegalStateException("No catalog defines the key $key")
        }
        return key
    }

    companion object {
        private const val DIRECTORY = "contract/i18n"
        private const val SOURCE_LOCALE = "en"
        private val SUPPORTED = listOf("en", "es", "zh", "ar")

        /**
         * Loads the catalog matching the device locale, or English when the
         * device is set to something this app does not ship.
         *
         * Reads two files rather than one: the requested locale, and English as
         * the fallback map. English is loaded even when it is the requested
         * locale, which costs one small parse and keeps the code paths
         * identical rather than special casing the source language.
         */
        fun load(context: Context, locale: Locale = Locale.getDefault()): Strings {
            val code = SUPPORTED.firstOrNull { it == locale.language } ?: SOURCE_LOCALE
            val requested = read(context, code)
            val english = if (code == SOURCE_LOCALE) requested else read(context, SOURCE_LOCALE)
            val meta = requested.meta
            return Strings(
                entries = requested.entries,
                fallback = english.entries,
                locale = Locale.forLanguageTag(code),
                isRtl = meta.optString("direction") == "rtl",
                reviewedByNativeSpeaker = meta.optBoolean("reviewed_by_native_speaker", false),
            )
        }

        /**
         * A catalog for `@Preview` only, which has no `Context` to read assets
         * from. It carries the English source values for the keys previews use,
         * so a preview shows real copy rather than key names.
         *
         * **These values are copies and copies drift.** `StringsTest` asserts
         * every entry here is byte identical to the English catalog, so a
         * preview cannot quietly show wording the app no longer ships. That
         * check is the only reason duplicating the copy is acceptable at all.
         */
        fun preview(): Strings =
            Strings(PREVIEW_ENTRIES, PREVIEW_ENTRIES, Locale.ENGLISH, isRtl = false,
                reviewedByNativeSpeaker = false)

        /** Exposed so the drift check above can be an actual test. */
        internal val previewEntries: Map<String, String> get() = PREVIEW_ENTRIES

        private val PREVIEW_ENTRIES = mapOf(
                "disclaimer.title" to "Before you start",
                "disclaimer.lead" to
                    "Health Trail is a notebook. It helps you keep track of someone's " +
                    "care: the calls, the visits, the questions, and the paperwork that " +
                    "piles up around all of it.",
                "disclaimer.block.1.title" to "It is not a medical app",
                "disclaimer.block.1.body" to
                    "It gives no medical advice, and it is not a medical device. " +
                    "Nothing here replaces a doctor, a nurse, emergency services, or " +
                    "advice from a lawyer. If something is urgent, call emergency " +
                    "services.",
                "disclaimer.block.2.title" to "What you write stays on this phone",
                "disclaimer.block.2.body" to
                    "There is no account and no cloud. Your notes live on this device " +
                    "and go nowhere else unless you send them somewhere yourself.",
                "disclaimer.block.3.title" to "The record is yours",
                "disclaimer.block.3.body" to
                    "The app writes down what you tell it and keeps it organized. It " +
                    "never decides what any of it means. You choose what goes in, and " +
                    "it stays yours.",
                "disclaimer.accept" to "I understand",
                "common.loading" to "Loading",
                "common.error.generic" to "That did not work. Nothing was changed.",
                "common.retry" to "Try again",
        )

        private class Catalog(val entries: Map<String, String>, val meta: JSONObject)

        private fun read(context: Context, code: String): Catalog {
            val text = context.assets.open("$DIRECTORY/$code.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val entries = HashMap<String, String>(root.length())
            var meta = JSONObject()
            for (key in root.keys()) {
                if (key == "_meta") {
                    meta = root.getJSONObject(key)
                    continue
                }
                if (key.startsWith("_")) continue
                entries[key] = root.getString(key)
            }
            return Catalog(entries, meta)
        }
    }
}

/**
 * The catalog for the current locale.
 *
 * Deliberately has no default. A composable reading text before the catalog is
 * provided is a bug, and failing at that point is better than rendering key
 * names on a screen a person is looking at.
 */
val LocalStrings = staticCompositionLocalOf<Strings> {
    error("No Strings provided. Wrap the content in HealthTrailApp's provider.")
}
