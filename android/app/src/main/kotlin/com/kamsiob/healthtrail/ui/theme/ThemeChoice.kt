package com.kamsiob.healthtrail.ui.theme

import android.content.Context
import androidx.core.content.edit

/**
 * Whether the app follows the system's light and dark setting, or overrides it.
 *
 * **Three choices and no more.** No true black, no per-screen override, no
 * scheduling by time of day. Each of those is a setting somebody has to
 * understand before they can use the app, and rule 20 says the complexity lives
 * in the code rather than on the screen.
 *
 * **[FOLLOW_SYSTEM] is the default and stays the default.** Overriding what a
 * person has already told their phone is presumptuous, and the setting exists
 * for the case where they want this one app to differ, not to make an opinion
 * the starting point.
 */
enum class ThemeChoice {
    /** Whatever the phone is set to, including when that changes while open. */
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
    ;

    /** The key stored on disk. Stable, and never the ordinal. */
    val stored: String get() = name

    companion object {
        val DEFAULT = FOLLOW_SYSTEM

        /**
         * Reads a stored value, falling back to the default for anything
         * unrecognized.
         *
         * **Stored by name rather than by ordinal**, so reordering this enum or
         * inserting a value cannot silently change what somebody had chosen.
         * An ordinal is a number whose meaning lives somewhere else.
         */
        fun of(stored: String?): ThemeChoice =
            entries.firstOrNull { it.name == stored } ?: DEFAULT
    }
}

/**
 * Where the choice is kept.
 *
 * **Plain `SharedPreferences`, not the database.** This is a preference about
 * how the app looks on this device. It is not part of the person's record, it
 * must not travel in an export, and it must not appear in the change log. Two
 * different devices reading the same notebook should be allowed to disagree
 * about the theme.
 *
 * That also means it survives the full data wipe, which is correct: wiping the
 * notebook should not silently change the colors.
 */
class ThemeSetting(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): ThemeChoice = ThemeChoice.of(prefs.getString(KEY, null))

    fun write(choice: ThemeChoice) {
        prefs.edit { putString(KEY, choice.stored) }
    }

    private companion object {
        const val PREFS = "health-trail-appearance"
        const val KEY = "theme_choice"
    }
}
