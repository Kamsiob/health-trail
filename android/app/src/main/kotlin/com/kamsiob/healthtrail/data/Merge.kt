package com.kamsiob.healthtrail.data

/**
 * What a merge would do, decided in full before anything is written.
 * `contract/DATA-CONTRACT.md` section 8.3.
 *
 * **Pure, and for the same reason `ReadableArchive` is.** Rows in, a plan out.
 * No Android, no database handle, no clock. Two things follow that nothing else
 * would give.
 *
 * The rules that decide whose version of a row survives can be checked
 * exhaustively without a phone, which matters because the failure this guards
 * against is somebody's note being replaced by an older copy of itself and
 * nobody noticing for a year.
 *
 * And **the plan exists before the write does**, which is what makes 8.3's
 * "fully succeeds or changes nothing" achievable rather than aspirational. The
 * caller can look at the whole plan, find it unsafe, and never open a
 * transaction at all.
 *
 * **Merge never deletes.** A row the incoming file has never heard of is a row
 * the other phone never saw, not a row somebody removed. Removal travels as a
 * tombstone, which is an ordinary row with `deleted_at` set, so it merges by the
 * same rule as everything else and needs no special case. That is the whole
 * reason the schema has no hard deletes.
 *
 * **Nothing is invented.** No new ids, no refreshed timestamps, no re-derived
 * ordering, no default filling a null. A row that wins is written exactly as it
 * arrived. If the file does not say it, this does not decide it.
 */
internal object Merge {

    /** One row, as column name to value. Null is a null column, not an empty one. */
    typealias Row = Map<String, String?>

    /**
     * Tables this deliberately does not merge, each with the reason.
     *
     * **Named rather than silently skipped**, because 8.3's rule about unknown
     * content is that nothing is ever quietly discarded, and a reader of this
     * file deserves the same courtesy about content that is knowingly left
     * alone.
     *
     * `change_log` is an append-only audit of what each device did, keyed by
     * that device's own sequence. Merging two of them would interleave two
     * devices' sequences into one counter and the result would describe a
     * history that never happened. `conflict_log` is this mechanism's own
     * output and merging it would fold one phone's resolutions into another's.
     * `app_meta` is unresolved: it holds text stored unnormalized and a restored
     * phone writing under the source phone's identity, which is #319 and #320
     * and is the owner's to settle. `device` and `migration` describe the phone
     * this database is on rather than the record it holds.
     */
    val NOT_MERGED = mapOf(
        "change_log" to "one device's own sequence of what it did",
        "conflict_log" to "this mechanism's own output",
        "app_meta" to "application state, and unresolved: #319 and #320",
        "device" to "the identity of the phone, not the record",
        "schema_migration" to "which migrations this phone has applied",
        "android_metadata" to "SQLite's own",
    )

    /** Why one version of a row was kept and the other was not. */
    object Reason {
        /** The kept version was written later. The ordinary case. */
        const val NEWER = "newer"

        /**
         * Both were written in the same millisecond, so the device that made
         * the row decides, by its id.
         *
         * **A rule rather than a coin toss.** Two phones merging the same pair
         * in either direction must reach the same answer, or the two notebooks
         * diverge permanently and each thinks it is right. `Ids` says the same
         * thing where the device id is generated.
         */
        const val SAME_TIME = "same_time"
    }

    /** A version of a row that did not survive, kept whole so nothing is lost. */
    data class Conflict(
        val table: String,
        val rowId: String,
        /** "local" or "incoming". */
        val winner: String,
        /** One of [Reason]. A token, localized when it is shown. */
        val reason: String,
        val local: Row,
        val incoming: Row,
    )

    /** A row that points at a parent no version of the notebook has. */
    data class Dangling(
        val table: String,
        val rowId: String,
        val column: String,
        val parentTable: String,
        val missingId: String,
    )

    /**
     * Everything the merge would do, and everything wrong with it.
     *
     * @param inserts rows the incoming file has and this phone does not.
     * @param updates rows both have where the incoming version won.
     * @param conflicts every resolution, in both directions. **A row where the
     *   local version won is still a conflict**, because the person needs to
     *   know that a second version existed and what it said. That is the whole
     *   argument for the conflict log: a record keeping app that quietly eats
     *   an entry has failed at its one job.
     * @param unchanged rows that were the same on both sides, counted rather
     *   than listed. They are the overwhelming majority and they are not news.
     * @param dangling references that do not resolve. **Any of these and the
     *   merge must not run**, per 8.3.
     * @param skipped tables not merged, with the reason, so a reader of the
     *   report is never left wondering.
     */
    data class Plan(
        val inserts: Map<String, List<Row>>,
        val updates: Map<String, List<Row>>,
        val conflicts: List<Conflict>,
        val unchanged: Int,
        val dangling: List<Dangling>,
        val skipped: Map<String, String>,
    ) {
        /** True when nothing in the plan would leave the notebook inconsistent. */
        val canApply: Boolean get() = dangling.isEmpty()

        val insertCount: Int get() = inserts.values.sumOf { it.size }
        val updateCount: Int get() = updates.values.sumOf { it.size }
    }

    /**
     * Decide the whole merge.
     *
     * @param local every table's rows on this phone, tombstones included. A
     *   tombstone is a row like any other here: leaving them out would make a
     *   deletion look like a row the other phone had and this one never did,
     *   and the merge would put it back.
     * @param incoming the same, from the archive.
     * @param references for each table, which of its columns point at which
     *   other table. Read from the database with `PRAGMA foreign_key_list`
     *   rather than declared a second time in Kotlin, per D16.
     */
    fun plan(
        local: Map<String, List<Row>>,
        incoming: Map<String, List<Row>>,
        references: Map<String, Map<String, String>>,
    ): Plan {
        val inserts = LinkedHashMap<String, MutableList<Row>>()
        val updates = LinkedHashMap<String, MutableList<Row>>()
        val conflicts = mutableListOf<Conflict>()
        var unchanged = 0

        // Sorted so a plan does not depend on map iteration order. The same two
        // notebooks must produce the same plan every time, or a merge that
        // failed cannot be reasoned about after the fact.
        for (table in incoming.keys.sorted()) {
            if (table in NOT_MERGED) continue
            val mine = local[table].orEmpty().associateBy { it["id"] }
            for (row in incoming.getValue(table).sortedBy { it["id"].orEmpty() }) {
                val id = row["id"] ?: continue
                val existing = mine[id]
                if (existing == null) {
                    inserts.getOrPut(table) { mutableListOf() } += row
                    continue
                }
                if (existing == row) {
                    unchanged++
                    continue
                }
                val incomingWins = incomingWins(existing, row)
                conflicts += Conflict(
                    table = table,
                    rowId = id,
                    winner = if (incomingWins) "incoming" else "local",
                    reason = if (updatedAt(existing) == updatedAt(row)) Reason.SAME_TIME
                    else Reason.NEWER,
                    local = existing,
                    incoming = row,
                )
                if (incomingWins) {
                    updates.getOrPut(table) { mutableListOf() } += row
                }
            }
        }

        return Plan(
            inserts = inserts,
            updates = updates,
            conflicts = conflicts,
            unchanged = unchanged,
            dangling = dangling(local, incoming, inserts, updates, references),
            skipped = NOT_MERGED.filterKeys { it in incoming.keys },
        )
    }

    /**
     * Whether the incoming version replaces the local one.
     *
     * **Later `updated_at` wins**, and a tie is broken by `origin_device`
     * because the answer has to be the same on both phones. A missing or
     * unreadable `updated_at` counts as the beginning of time, which makes the
     * row with a real timestamp win: a row this phone cannot date is a row it
     * cannot claim is newer.
     */
    private fun incomingWins(local: Row, incoming: Row): Boolean {
        val mine = updatedAt(local)
        val theirs = updatedAt(incoming)
        if (theirs != mine) return theirs > mine
        return incoming["origin_device"].orEmpty() > local["origin_device"].orEmpty()
    }

    private fun updatedAt(row: Row): Long = row["updated_at"]?.toLongOrNull() ?: Long.MIN_VALUE

    /**
     * Every reference that would not resolve after the merge.
     *
     * **Checked against what the notebook would hold afterward**, which is what
     * this phone already has plus what the merge would add. A child whose parent
     * is only on this phone is fine, and so is one whose parent arrives in the
     * same file. What is not fine is a parent that exists in neither, which 8.3
     * says must halt the import by name rather than drop the child or invent a
     * placeholder.
     *
     * **A null reference is not dangling.** Most of these columns are optional,
     * and "not filed under a chapter" is a state the app offers on purpose.
     */
    private fun dangling(
        local: Map<String, List<Row>>,
        incoming: Map<String, List<Row>>,
        inserts: Map<String, List<Row>>,
        updates: Map<String, List<Row>>,
        references: Map<String, Map<String, String>>,
    ): List<Dangling> {
        val known = HashMap<String, MutableSet<String>>()
        fun note(source: Map<String, List<Row>>) {
            for ((table, rows) in source) {
                val into = known.getOrPut(table) { mutableSetOf() }
                for (row in rows) row["id"]?.let { into += it }
            }
        }
        note(local)
        note(inserts)

        val found = mutableListOf<Dangling>()
        val arriving = (inserts.keys + updates.keys).sorted()
        for (table in arriving) {
            val columns = references[table] ?: continue
            val rows = inserts[table].orEmpty() + updates[table].orEmpty()
            for (row in rows.sortedBy { it["id"].orEmpty() }) {
                for ((column, parentTable) in columns.entries.sortedBy { it.key }) {
                    val target = row[column] ?: continue
                    if (target.isBlank()) continue
                    if (target in known[parentTable].orEmpty()) continue
                    found += Dangling(
                        table = table,
                        rowId = row["id"].orEmpty(),
                        column = column,
                        parentTable = parentTable,
                        missingId = target,
                    )
                }
            }
        }
        // Deliberately not deduplicated by parent: each child that cannot be
        // placed is its own problem for the person to understand.
        return found
    }
}
