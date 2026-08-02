package com.kamsiob.healthtrail.data

import android.content.Context
import androidx.core.content.edit

/**
 * When the person was last here, which is what "since you were last here" means.
 *
 * **A device preference rather than a row.** It is a fact about this phone and
 * this person's reading, not about the care, so it does not belong in the
 * record, must not travel in an export, and must not appear in the change log.
 * Two devices reading the same notebook are each entitled to their own idea of
 * what their holder has already seen.
 *
 * **Read once per launch and immediately advanced.** The value the screen uses
 * is the one from the previous launch, held for as long as the app is open, so
 * a digest does not empty itself out from under somebody who is still reading
 * it. Advancing it at launch rather than on the way out means a person who
 * force closes the app still gets a correct next digest instead of the same one
 * forever.
 *
 * **A first launch reports nothing rather than everything.** With no stored
 * visit there is no "since", and summarizing a notebook's entire history as
 * though it all happened this week would be false on the one screen whose whole
 * job is to be true about the last few days.
 */
class LastVisit(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * The previous visit, or null on a first launch, with [now] stored as the
     * current one.
     *
     * **Advances once per process, however many times it is called.** It is
     * held by composition, and a composition is rebuilt whenever the activity
     * is: a theme change, a rotation, a font scale change, the system
     * reclaiming memory. Advancing on each of those moved the mark to the
     * middle of the visit and threw away the digest the person was in the
     * middle of reading, which is how this was found: a notebook full of new
     * records reported nothing at all.
     *
     * A visit ends when the process does, which is the only definition of "last
     * here" that matches what somebody means by it.
     */
    fun openAndAdvance(now: Long): Long? = synchronized(lock) {
        opened?.let { return it.previous }

        val previous = if (prefs.contains(KEY)) prefs.getLong(KEY, 0L) else null
        // Committed rather than applied. The write has to survive the process
        // being killed a moment later, and a lost write means the next visit
        // reports the same span over again.
        prefs.edit(commit = true) { putLong(KEY, now) }
        opened = Visit(previous)
        previous
    }

    /** For tests and for the full wipe, which should not leave a stale visit behind. */
    fun forget() = synchronized(lock) {
        prefs.edit(commit = true) { remove(KEY) }
        opened = null
    }

    private data class Visit(val previous: Long?)

    companion object {
        /**
         * Ends this process's visit, which normally only the process ending
         * does. Exists so a test can act out the one thing it cannot do to
         * itself.
         */
        fun endVisitForTest() = synchronized(lock) { opened = null }

        const val PREFS = "health-trail-visits"
        const val KEY = "last_visit_at"

        private val lock = Any()

        /**
         * This process's visit, decided once.
         *
         * Deliberately outlives any one instance of this class, because the
         * thing being remembered is "this run of the app", not "this object".
         */
        @Volatile
        private var opened: Visit? = null
    }
}
