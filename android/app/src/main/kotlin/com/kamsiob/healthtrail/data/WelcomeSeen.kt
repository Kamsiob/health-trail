package com.kamsiob.healthtrail.data

import android.content.Context
import androidx.core.content.edit

/**
 * Whether the person using this phone has been through "Before you start".
 *
 * **The acceptance is recorded twice on purpose, and the two answer different
 * questions.** `disclaimer_accepted_at` lives in the notebook because it is a
 * record with a timestamp, and it travels with the notebook into the archive.
 * This one lives on the phone because the question the first screen actually
 * asks is whether **this person, holding this device**, has read it.
 *
 * **Without it, restoring your own backup sent you back through onboarding.**
 * Restore replaces the notebook, the acceptance goes with it, and nothing looks
 * wrong until the process next starts, which on this app is as ordinary as
 * changing the font scale. Somebody reaching for restore has usually just lost
 * a phone, and being handed the first run screen at that moment reads as "the
 * restore did not work" while every note is sitting there behind it. #307, D146.
 *
 * **A fresh install still shows the gate**, because a preferences file is
 * created with the app and cleared with its data, so nothing here can make the
 * disclaimer skippable on a phone that has never seen it.
 */
class WelcomeSeen(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun seen(): Boolean = prefs.getBoolean(KEY, false)

    fun remember() {
        prefs.edit { putBoolean(KEY, true) }
    }

    /** For tests, which need a phone that has never seen the gate. */
    fun forgetForTest() {
        prefs.edit { remove(KEY) }
    }

    private companion object {
        const val PREFS = "health-trail-device"
        const val KEY = "welcome_seen"
    }
}
