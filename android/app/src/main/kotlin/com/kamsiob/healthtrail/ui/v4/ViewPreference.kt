package com.kamsiob.healthtrail.ui.v4

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/**
 * Which view a section was last left in, remembered across sessions.
 *
 * **`DESIGN.md` section 7 says the view toggle is remembered per section**, and
 * a toggle that forgets is worse than no toggle: somebody who prefers the
 * compact list sets it every single time they open the screen, which is a tax
 * for having a preference at all.
 *
 * **Per section, not one setting for the app.** Somebody can reasonably want
 * documents as pictures and appointments as a list, because the two answer
 * different questions.
 *
 * **`SharedPreferences`, following `ThemeSetting`, and there is a live question
 * about that.** This is a preference about how a screen looks on this device
 * rather than something about the person's care, which is the same reasoning
 * that keeps the theme out of the database. But `contract/DATA-CONTRACT.md`
 * 8.5 lists view preferences among what the round trip must carry, which means
 * the contract expects them in the record and there is no table for them.
 * **That is the owner's call and it is filed rather than decided here**, so
 * this stays local and the export is unaffected either way.
 */
class ViewPreference(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(section: String, fallback: String): String =
        prefs.getString(section, null) ?: fallback

    fun write(section: String, view: String) {
        prefs.edit { putString(section, view) }
    }

    private companion object {
        const val PREFS = "health-trail-views"
    }
}

/**
 * The remembered view for one section, as state a screen can drive a toggle
 * with.
 *
 * The write happens on change rather than on leaving the screen, because a
 * screen can be left by the system rather than by the person and a preference
 * that only survives a graceful exit is one that appears to forget at random.
 */
@Composable
fun rememberViewChoice(section: String, fallback: String): ViewChoice {
    val context = LocalContext.current
    val store = remember(context) { ViewPreference(context) }
    var current by remember(section) { mutableStateOf(store.read(section, fallback)) }
    return ViewChoice(
        value = current,
        onSelect = {
            current = it
            store.write(section, it)
        },
    )
}

/** What [rememberViewChoice] hands a screen: the current view and how to change it. */
data class ViewChoice(val value: String, val onSelect: (String) -> Unit)
