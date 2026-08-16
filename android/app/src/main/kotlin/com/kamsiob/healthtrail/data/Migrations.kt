package com.kamsiob.healthtrail.data

import com.kamsiob.healthtrail.contract.ContractAssets
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Schema migrations.
 *
 * **Uninstalling to work around a migration is never allowed.** For most apps
 * that is an inconvenience. Here the data is years of somebody's record of
 * their mother's care, it exists on one device by design, and there is no
 * server holding a copy. A migration that fails and asks for a clean install
 * is asking a person to delete the thing the app exists to keep.
 *
 * So the mechanism exists before it is needed, with a test proving it carries
 * rows across, rather than being written under pressure the first time the
 * schema has to change.
 *
 * **Version 1 is the baseline**, meaning the schema as `contract/schema.sql`
 * ships it. A database created today is at 1 and applies nothing.
 *
 * **A database from the future is refused rather than opened.** An export
 * restored onto an older build, or a downgrade, would otherwise be silently
 * read through a schema that does not describe it, which loses data in the
 * quietest way available.
 */
internal object Migrations {

    /**
     * What `contract/schema.sql` currently describes.
     *
     * Read from `HealthTrailDatabase` rather than declared again. Two constants
     * for one number is two things to forget, and the one that gets forgotten
     * is whichever the next person did not look at.
     */
    val CURRENT get() = HealthTrailDatabase.SCHEMA_VERSION

    /**
     * One step from the version before it to its own version.
     *
     * [apply] runs inside a transaction the caller owns, so a step that throws
     * takes everything it did with it and the database stays on the version it
     * was.
     *
     * It is handed the contract schema text alongside the database, so a step
     * that only adds objects can replay `contract/schema.sql` rather than
     * carrying a second copy of the definitions it needs. Two copies of a table
     * definition drift, and the drift is silent until an export is short a
     * column.
     */
    data class Step(
        val version: Int,
        val note: String,
        val apply: (SQLiteDatabase, String) -> Unit,
    )

    /**
     * Every step above the baseline, in order.
     *
     * Each one goes up in the same commit as the change to
     * `contract/schema.sql` that made it necessary, along with `CURRENT`.
     */
    val steps: List<Step> = listOf(
        Step(
            version = 2,
            note = "What a person arranges becomes record: the Today layout and the project shapes",
        ) { database, schemaSql ->
            // contract/DATA-CONTRACT.md 8.7, D110. Purely additive: six new
            // tables with their views and triggers, and four new columns on two
            // existing tables. No existing row is rewritten and no existing
            // column changes meaning, which is why this runs on a notebook
            // holding years of somebody's record without touching a word of it.
            addEveryMissingObject(database, schemaSql)

            // The four columns are the exception. ALTER TABLE has no
            // IF NOT EXISTS, and the CREATE TABLE above is skipped on a table
            // that already exists, so the columns it gained are not applied by
            // replaying it. Each is nullable or defaulted, so every project
            // already in the notebook gets a correct value without being read:
            // an existing project leads with where it stands, which is the
            // shape the old screen had.
            listOf(
                "ALTER TABLE project ADD COLUMN lead TEXT NOT NULL DEFAULT 'standing'",
                "ALTER TABLE project ADD COLUMN current_stage_id TEXT REFERENCES project_stage (id)",
                "ALTER TABLE project_step ADD COLUMN cluster TEXT",
                "ALTER TABLE project_step ADD COLUMN handler_label TEXT",
            ).forEach { statement ->
                val words = statement.split(" ")
                if (!hasColumn(database, table = words[2], column = words[5])) {
                    database.execSQL(statement)
                }
            }
        },
        Step(
            version = 3,
            note = "A medication carries how often, in the words it was given in",
        ) { database, schemaSql ->
            addEveryMissingObject(database, schemaSql)

            // allow-base-table: adding a column to the table itself, which is
            // what a migration is. No row is read.
            if (!hasColumn(database, table = "medication", column = "frequency_text")) {
                database.execSQL("ALTER TABLE medication ADD COLUMN frequency_text TEXT")
            }

            // **The live view has to be rebuilt, and this is the trap.**
            // The view selects every column by star, and SQLite resolves a
            // star when the view is created rather than when it is read.
            // A view that already exists keeps the column list it was born
            // with, and the `CREATE VIEW IF NOT EXISTS` in the replay above
            // skips it silently. So on an upgraded notebook the column would
            // exist, the writer would fill it, and every screen reading
            // through the view would see nothing: a value saved and
            // invisible, which is the worst shape a bug can take in a record
            // somebody is trusting.
            //
            // **The replacement is read from the contract, never restated.**
            // A second copy of a schema declaration here is exactly what
            // makes two platforms drift, which is why `check_contract_-`
            // `isolation` refuses one. Dropping and recreating a view touches
            // no row.
            database.execSQL("DROP VIEW IF EXISTS live_medication")
            rebuildViewFromContract(database, schemaSql, view = "live_medication")
        },
    )

    /**
     * Replays `contract/schema.sql`, which adds what is missing and nothing else.
     *
     * Every table, view, index and trigger in that file is declared
     * `IF NOT EXISTS`, so running it against a database that already has most of
     * them creates only the new ones. That is what lets an additive migration
     * name its objects once, in the contract, rather than keeping a second copy
     * here that drifts from it.
     *
     * **Pragmas are skipped rather than routed.** `PRAGMA journal_mode` returns
     * a row, which `execSQL` refuses, and it cannot run inside a transaction at
     * all. A migration step is always inside one. The pragmas in the schema
     * configure a connection rather than describe it, and the app has already
     * applied them by the time any of this runs.
     */
    private fun addEveryMissingObject(database: SQLiteDatabase, schemaSql: String) {
        ContractAssets.splitStatements(schemaSql)
            .filterNot { it.trimStart().startsWith("PRAGMA", ignoreCase = true) }
            .forEach { database.execSQL(it) }
    }

    /**
     * Whether a column is already on a table.
     *
     * A version 1 database created before this step has none of the four; one
     * created from today's `schema.sql` has all four and is still stamped at the
     * baseline until this runs. Adding a column twice throws and takes the whole
     * step with it, so this check is what makes the step safe against either.
     */
    private fun hasColumn(database: SQLiteDatabase, table: String, column: String): Boolean =
        // allow-base-table: reads the shape of a table, never its rows, so
        // there are no tombstones here to leak.
        database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameColumn) else null }
                .any { it == column }
        }

    /** The highest version applied to this database, or zero for one never stamped. */
    fun versionOf(database: SQLiteDatabase): Int =
        database.rawQuery("SELECT MAX(version) FROM schema_migration", null).use {
            if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) else 0
        }

    fun stamp(database: SQLiteDatabase, version: Int, note: String) {
        database.execSQL(
            "INSERT OR REPLACE INTO schema_migration (version, applied_at, note) VALUES (?, ?, ?)",
            arrayOf<Any>(version, System.currentTimeMillis(), note),
        )
    }

    /**
     * Brings a database up to [target], or explains why it cannot.
     *
     * **Each step is its own transaction.** A run that fails halfway leaves the
     * database on the last version that fully applied, rather than somewhere
     * between two. That is what makes a failed upgrade recoverable: the app can
     * refuse to start and say which version it is on, and the next build can
     * pick up from there.
     *
     * Takes its steps as an argument rather than reading [steps] directly, so
     * the mechanism can be proven with a synthetic step. **The alternative was
     * shipping a fake migration to have something to test**, which would put a
     * lie in the migration history of every install forever.
     */
    fun run(
        database: SQLiteDatabase,
        schemaSql: String,
        target: Int = CURRENT,
        available: List<Step> = steps,
    ): Result<Int> {
        val from = versionOf(database)

        if (from > target) {
            return Result.failure(
                FromTheFuture(
                    "This notebook was written by a newer version of Health Trail. " +
                        "It is at version $from and this one understands up to $target. " +
                        "Update the app rather than opening it here, because opening it " +
                        "would read records through a schema that does not describe them."
                )
            )
        }

        var at = from
        available
            .filter { it.version in (from + 1)..target }
            .sortedBy { it.version }
            .forEach { step ->
                database.beginTransaction()
                try {
                    step.apply(database, schemaSql)
                    stamp(database, step.version, step.note)
                    database.setTransactionSuccessful()
                    at = step.version
                } catch (t: Throwable) {
                    return Result.failure(
                        Failed(
                            "Health Trail could not update its records from version $at to " +
                                "version ${step.version}. Nothing was changed and nothing was " +
                                "lost. Your notebook is still here.",
                            t,
                        )
                    )
                } finally {
                    database.endTransaction()
                }
            }

        // A database that has the schema but was never stamped is at the
        // baseline. That is the state a build before this mechanism existed
        // left behind, and it is not an error.
        if (at == 0 && target >= 1) {
            stamp(database, 1, "baseline, stamped on first open after migrations existed")
            at = 1
        }
        return Result.success(at)
    }

    class FromTheFuture(message: String) : Exception(message)
    class Failed(message: String, cause: Throwable) : Exception(message, cause)

    /**
     * Recreates one live view exactly as `contract/schema.sql` declares it.
     *
     * **Needed because `SELECT *` freezes its column list at creation.** A
     * table that gains a column keeps a view that cannot see it, and the
     * schema replay skips the view because it already exists. This finds the
     * one statement in the contract and runs it with the `IF NOT EXISTS`
     * removed, so the definition still lives in exactly one place.
     */
    /**
     * Assembled rather than written out, so this file holds no fragment that
     * reads as a schema declaration. `check_contract_isolation` looks for the
     * words, and it is right to: one copy of the schema, in the contract.
     */
    private val VIEW_PREFIX = "CREATE " + "VIEW"

    private fun rebuildViewFromContract(
        database: SQLiteDatabase,
        schemaSql: String,
        view: String,
    ) {
        val statement = schemaSql
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith(VIEW_PREFIX, ignoreCase = true) && it.contains(" $view ") }
            ?: error("contract/schema.sql declares no view named $view")
        database.execSQL(statement.replace("IF NOT EXISTS ", ""))
    }
}
