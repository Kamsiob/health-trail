package com.kamsiob.healthtrail.data

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
     */
    data class Step(
        val version: Int,
        val note: String,
        val apply: (SQLiteDatabase) -> Unit,
    )

    /**
     * Every step above the baseline, in order.
     *
     * Empty, and that is correct: nothing has changed since version 1. The
     * first real entry goes here, and `CURRENT` goes up in the same commit as
     * the change to `contract/schema.sql` that made it necessary.
     */
    val steps: List<Step> = emptyList()

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
                    step.apply(database)
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
}
