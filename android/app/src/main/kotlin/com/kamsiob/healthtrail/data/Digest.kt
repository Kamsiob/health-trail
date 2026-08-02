package com.kamsiob.healthtrail.data

/**
 * What changed since the person was last here.
 *
 * **This is arithmetic on the change log and nothing else.** It counts rows
 * that were written, groups them by where they live, and stops. It never says
 * which change mattered, never orders by how much happened, and never remarks
 * on a quiet week or a busy one, because all three would be the app forming an
 * opinion about somebody's care. Rule 2.
 *
 * **Ordered by where things live, never by how many.** Ranking sections by
 * count would put whichever part of the week was busiest at the top and move
 * the sections around from visit to visit, which breaks the one promise the
 * notebook makes: the places never move. The order here is the notebook's own.
 *
 * **Pure, and separate from the screen and the database.** It takes a list and
 * a timestamp and returns a summary, so the rule that has to be right can be
 * tested against fixed vectors on both platforms rather than through a
 * database and a composition. That is what #15 asks for.
 */
object Digest {

    /** One row of the change log, reduced to what a summary needs. */
    data class Change(
        val table: String,
        val rowId: String,
        val op: String,
        val changedAt: Long,
    ) {
        companion object {
            const val INSERT = "insert"
            const val UPDATE = "update"
            const val DELETE = "delete"
        }
    }

    /** How many new things landed in one section. */
    data class Added(val section: Repository.Section, val count: Int)

    /**
     * What happened, in the only three categories this app is willing to name.
     *
     * Corrections and removals are totals rather than per section on purpose.
     * A correction is usually somebody fixing their own typing, and breaking
     * that out section by section would give the person's own tidying up the
     * same weight on the screen as the care itself.
     */
    data class Summary(
        val added: List<Added>,
        val corrected: Int,
        val removed: Int,
    ) {
        val isEmpty: Boolean get() = added.isEmpty() && corrected == 0 && removed == 0
        val newThings: Int get() = added.sumOf { it.count }
    }

    /** An empty summary, which is what a first visit and a quiet week both get. */
    val nothing = Summary(added = emptyList(), corrected = 0, removed = 0)

    /**
     * The summary of everything that changed strictly after [since].
     *
     * **Strictly after**, so the boundary row is never counted twice by two
     * consecutive visits. A change logged at exactly the moment of the last
     * visit was already on screen during it.
     *
     * **A row is counted once per category, however many times it was written.**
     * Somebody who corrects the same entry four times made one correction as far
     * as this screen is concerned; reporting four would turn a person fussing
     * with a phone keyboard into an event.
     *
     * **A row that was created and then removed in the same span reports only as
     * removed.** It never existed as far as the person's next visit is
     * concerned, and announcing it as both new and gone describes the app's
     * bookkeeping rather than their week. Rule 20.
     */
    fun since(changes: List<Change>, since: Long): Summary {
        val relevant = changes.filter { it.changedAt > since }
        if (relevant.isEmpty()) return nothing

        val byRow = relevant.groupBy { it.table to it.rowId }

        val removedRows = mutableSetOf<Pair<String, String>>()
        val insertedRows = mutableSetOf<Pair<String, String>>()
        val correctedRows = mutableSetOf<Pair<String, String>>()

        byRow.forEach { (key, ops) ->
            val kinds = ops.map { it.op }.toSet()
            when {
                Change.DELETE in kinds -> removedRows += key
                Change.INSERT in kinds -> insertedRows += key
                Change.UPDATE in kinds -> correctedRows += key
            }
        }

        val addedBySection = insertedRows
            .mapNotNull { (table, _) -> sectionOf(table) }
            .groupingBy { it }
            .eachCount()

        return Summary(
            // The notebook's own order, so a section never moves between visits.
            added = Repository.Section.entries
                .mapNotNull { section ->
                    addedBySection[section]?.let { Added(section, it) }
                },
            corrected = correctedRows.size,
            removed = removedRows.size,
        )
    }

    /**
     * Where a table's rows show up in the notebook.
     *
     * **A table with no section returns null and is left out rather than
     * counted into something.** Bookkeeping tables and anything a later version
     * of the schema adds are not things the person put anywhere, so announcing
     * them would be the app describing its own storage on the one screen that
     * exists to describe their week.
     */
    internal fun sectionOf(table: String): Repository.Section? = when (table) {
        "entry" -> Repository.Section.TRAIL
        "person" -> Repository.Section.CARE_TEAM
        "medication" -> Repository.Section.MEDICATIONS
        "appointment" -> Repository.Section.APPOINTMENTS
        "chapter" -> Repository.Section.CHAPTERS
        "care_thread" -> Repository.Section.THREADS
        "reading" -> Repository.Section.PROGRESS
        "document" -> Repository.Section.DOCUMENTS
        "bill" -> Repository.Section.MONEY
        "standing_instruction" -> Repository.Section.STANDING_INSTRUCTIONS
        "question" -> Repository.Section.ASK_NEXT_TIME
        "emergency_card", "emergency_contact" -> Repository.Section.EMERGENCY_CARD
        "project" -> Repository.Section.PROJECTS
        else -> null
    }
}
