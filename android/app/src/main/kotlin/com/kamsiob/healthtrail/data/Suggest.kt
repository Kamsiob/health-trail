package com.kamsiob.healthtrail.data

/**
 * Suggesting a home for an entry the person could not place.
 *
 * **Plain word matching, and deliberately nothing cleverer.** `MASTER_SPEC.md`
 * section 4.2 says the app suggests by matching words and the person confirms
 * with one tap, and that it never files anything on its own. Both halves are
 * the requirement. A smarter matcher would be a system quietly deciding what a
 * family's care record means, which is the thing this app does not do.
 *
 * **A suggestion is a default the person can change, never a decision made for
 * them.** It arrives already highlighted and one tap changes it, and the tray
 * files nothing until the person confirms.
 *
 * **It is allowed to find nothing**, and often will. No suggestion is a better
 * outcome than a weak one: an entry reached this tray because it was hard to
 * place, and a wrong guess presented confidently is worse than an honest blank.
 */
object Suggest {

    /**
     * The thread whose label shares the most words with this entry, or null.
     *
     * Matching is on whole words, lowercased, so "nursing" matches "Nursing"
     * and not "nurse". Word stems are exactly the kind of cleverness that turns
     * matching into interpreting, and the person is one tap from the right
     * answer either way.
     *
     * Ties go to nothing rather than to whichever thread happened to sort
     * first. Two equally good guesses mean the app does not know.
     */
    fun threadFor(
        entryText: String,
        threads: List<Repository.CareThread>,
    ): Repository.CareThread? {
        val words = words(entryText)
        if (words.isEmpty()) return null

        val scored = threads
            .map { it to words(it.label).count { word -> word in words } }
            .filter { (_, score) -> score > 0 }
        if (scored.isEmpty()) return null

        val best = scored.maxOf { it.second }
        val winners = scored.filter { it.second == best }
        return if (winners.size == 1) winners.first().first else null
    }

    /**
     * Words worth matching on.
     *
     * **Four characters is the floor, and it was three until a test caught it.**
     * At three, "Meals and dietary" matched the sentence "I rang and asked",
     * because "and" is a word by any measure and carries no information at all.
     * Nearly every connective in English is three characters or fewer, so the
     * floor does the work a stop word list would, without a list that would
     * have to be written and maintained per locale and would silently be wrong
     * in the locales nobody checked.
     *
     * **A known limitation, stated rather than hidden:** Chinese does not
     * separate words with spaces, so this finds almost nothing there. That is
     * acceptable because a suggestion is never required. The person sees the
     * same chips either way and is one tap from the right answer, so the
     * failure mode is no help rather than wrong help.
     */
    private fun words(text: String): Set<String> =
        text.lowercase()
            .split(*SEPARATORS)
            .filter { it.length >= MIN_WORD }
            .toSet()

    private const val MIN_WORD = 4

    private val SEPARATORS = charArrayOf(
        ' ', '\n', '\t', '.', ',', ';', ':', '!', '?', '(', ')', '/', '\\', '-', '\'', '"',
    )
}
