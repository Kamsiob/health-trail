package com.kamsiob.healthtrail.data

import android.content.Context
import com.kamsiob.healthtrail.contract.ContractAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * Opens the one database, encrypted at rest, created from the shared schema.
 *
 * The schema is not defined here, and it is not defined anywhere else in Kotlin.
 * It is `contract/schema.sql`, copied into assets at build time and executed as
 * written. That is why there is no Room in this project: Room means declaring
 * every table a second time as annotated classes, and a second declaration is
 * exactly what makes the web platform a reimplementation rather than a second
 * reader. See DECISIONS.md D16.
 *
 * The practical consequence is that this class knows how to run DDL and nothing
 * about what the DDL says. If a table is added to the contract, nothing here
 * changes.
 */
class HealthTrailDatabase private constructor(
    internal val database: SQLiteDatabase,
    val deviceId: String,
) {

    companion object {
        const val FILE_NAME = "health-trail.db"

        /**
         * The schema version this build expects. It is written to
         * `schema_migration` on creation, so an upgrade never has to guess what
         * state it is looking at.
         *
         * Uninstalling to work around a migration is never allowed on this
         * project, because it destroys the records the upgrade path exists to
         * protect. So a migration that cannot run is a bug to fix, not a reason
         * to start over.
         */
        /**
         * What `contract/schema.sql` currently describes.
         *
         * 2, since 2026-08-04: the Today layout and the project shapes became
         * record, per `contract/DATA-CONTRACT.md` 8.7 and D110.
         */
        const val SCHEMA_VERSION = 2

        @Volatile
        private var instance: HealthTrailDatabase? = null

        /**
         * Opens the database, creating it on first call.
         *
         * **This is a suspend function that does its work on [Dispatchers.IO],
         * and that is the whole point.** Opening does real blocking work:
         * Keystore operations that touch secure hardware, a synchronous
         * preference write, and on first run the execution of the entire
         * schema.
         *
         * An earlier version enforced the thread with a runtime `check` that
         * threw. That was the wrong instrument. It is correct in debug and
         * dangerous in release, because the failure it produces is a crash, and
         * the path it fires on is by definition one the tests did not reach.
         * The person it would crash is a caregiver in a hallway.
         *
         * Being a suspend function that switches dispatcher itself removes the
         * question rather than answering it. There is no way to call this and
         * end up doing blocking work on the main thread, whatever the caller
         * does, so no check is needed and none is present. Calling it from the
         * wrong place is now a compile error rather than a runtime one.
         *
         * Throws [DatabaseKeyLost] when the wrapping key is gone. See
         * `DECISIONS.md` D24 for what the app does about that.
         */
        suspend fun open(context: Context): HealthTrailDatabase =
            withContext(Dispatchers.IO) {
                instance ?: synchronized(this@Companion) {
                    instance ?: create(context.applicationContext).also { instance = it }
                }
            }

        /** Closes and forgets the open database. For tests and the full wipe. */
        fun closeForTest() = synchronized(this) {
            instance?.database?.close()
            instance = null
        }

        private fun create(context: Context): HealthTrailDatabase {
            System.loadLibrary("sqlcipher")

            val file = context.getDatabasePath(FILE_NAME)
            file.parentFile?.mkdirs()
            val fresh = !file.exists()

            val key = DatabaseKey(context)
            val passphrase = key.passphrase()

            val database = try {
                SQLiteDatabase.openOrCreateDatabase(file, passphrase, null, null)
            } finally {
                passphrase.fill(0)
            }

            database.execSQL("PRAGMA foreign_keys = ON")

            if (fresh) {
                // applySchema stamps the version itself, so there is nothing
                // to migrate on a database created right now.
                applySchema(context, database)
            } else {
                // **An existing database is migrated, never rebuilt.**
                // Uninstalling to work around a migration is asking a person to
                // delete years of their record of somebody's care, on a device
                // that is the only place it exists. The mechanism runs on every
                // open so a database left at an older version by any path,
                // including an import, is brought forward rather than read
                // through a schema that does not describe it.
                Migrations.run(database, ContractAssets.readSchema(context)).getOrElse { problem ->
                    database.close()
                    throw problem
                }
            }

            val deviceId = ensureDeviceId(database)
            return HealthTrailDatabase(database, deviceId)
        }

        /**
         * Runs the contract's DDL.
         *
         * Statement splitting is delegated rather than reimplemented, because
         * getting it wrong is quiet: a trigger cut in half produces a syntax
         * error a long way from its cause, and a trigger silently dropped
         * produces a database that looks fine and stops recording changes.
         *
         * Pragmas are routed away from `execSQL`, which refuses any statement
         * that returns rows, and `PRAGMA journal_mode` returns one.
         *
         * **Internal rather than private so the migration test can build a
         * database the way the app does.** A test that applies the schema
         * differently proves a schema the app never runs, and this one in
         * particular carries the pragma routing above, which a naive copy in a
         * test gets wrong immediately.
         */
        internal fun applySchema(context: Context, database: SQLiteDatabase) {
            val sql = ContractAssets.readSchema(context)
            database.beginTransaction()
            try {
                for (statement in ContractAssets.splitStatements(sql)) {
                    if (statement.trimStart().startsWith("PRAGMA", ignoreCase = true)) {
                        database.rawQuery(statement, null).use { it.moveToFirst() }
                    } else {
                        database.execSQL(statement)
                    }
                }
                database.execSQL(
                    "INSERT OR REPLACE INTO schema_migration (version, applied_at, note) " +
                        "VALUES (?, ?, ?)",
                    arrayOf<Any>(
                        SCHEMA_VERSION,
                        System.currentTimeMillis(),
                        "created from contract/schema.sql",
                    ),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }

        /**
         * The id of this installation, generated once and kept.
         *
         * It matters beyond bookkeeping: the change log triggers read it out of
         * `app_meta` on every write, so if it were missing every logged change
         * would be attributed to `unknown-device` and a future sync could not
         * tell this device's writes from a peer's.
         */
        /**
         * Gives this phone a new identity, keeping the arriving one as a peer.
         *
         * **Called after a restore, and nowhere else.** #320. `app_meta` travels
         * in the archive and `device_id` lives in it, so a restored notebook
         * arrives carrying the source phone's identity, and this phone then
         * stamps every row and every change log entry it writes with a device
         * that is not it. `contract/DATA-CONTRACT.md` defines `origin_device` as
         * "the id of the device that created the row", so that was false for
         * every row created after any restore, silently, and would have stayed
         * false until a sync that does not exist yet had to sort it out.
         *
         * **The old identity is demoted rather than deleted.** The notebook
         * genuinely did come from another device: the `device` table exists to
         * record exactly that, and the rows already written there were written
         * by it. What changes is only which row is `is_self`.
         *
         * **The change log is not rewritten.** Those entries describe writes
         * that happened on the other phone and they are still true. Only what
         * this phone writes from now on gets the new id.
         */
        suspend fun reidentify(context: Context) {
            val database = open(context).database
            val generated = Ids.newDeviceId()
            val now = System.currentTimeMillis()
            database.beginTransaction()
            try {
                database.execSQL("UPDATE device SET is_self = 0 WHERE is_self = 1")
                database.execSQL(
                    "INSERT OR REPLACE INTO app_meta (key, value, updated_at) VALUES (?, ?, ?)",
                    arrayOf<Any>("device_id", generated, now),
                )
                database.execSQL(
                    "INSERT OR REPLACE INTO device (id, label, is_self, created_at, last_seq_in) " +
                        "VALUES (?, ?, 1, ?, 0)",
                    arrayOf<Any>(generated, android.os.Build.MODEL, now),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            // **Closed so the next open reads the new id.** The identity is
            // read once when the database is opened and carried on the handle,
            // so a caller holding the old one would go on stamping the old
            // device until the process restarted.
            closeForTest()
        }

        private fun ensureDeviceId(database: SQLiteDatabase): String {
            database.rawQuery(
                "SELECT value FROM app_meta WHERE key = 'device_id'", null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val existing = cursor.getString(0)
                    if (!existing.isNullOrBlank()) return existing
                }
            }

            val generated = Ids.newDeviceId()
            val now = System.currentTimeMillis()
            database.beginTransaction()
            try {
                database.execSQL(
                    "INSERT OR REPLACE INTO app_meta (key, value, updated_at) VALUES (?, ?, ?)",
                    arrayOf<Any>("device_id", generated, now),
                )
                database.execSQL(
                    "INSERT OR REPLACE INTO device (id, label, is_self, created_at, last_seq_in) " +
                        "VALUES (?, ?, 1, ?, 0)",
                    arrayOf<Any>(generated, android.os.Build.MODEL, now),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            return generated
        }
    }

    /** The applied schema version, read from the database rather than assumed. */
    fun schemaVersion(): Int =
        database.rawQuery("SELECT MAX(version) FROM schema_migration", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /** How many rows the change log holds. The digest reads from here. */
    fun changeLogSize(): Int =
        database.rawQuery("SELECT COUNT(*) FROM change_log", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /**
     * True when the file on disk is not readable as an ordinary SQLite
     * database, which is the only honest way to check that encryption is on.
     * Asserting that a passphrase was passed proves nothing about the bytes.
     */
    fun fileIsEncrypted(context: Context): Boolean {
        val file = File(context.getDatabasePath(FILE_NAME).path)
        if (!file.isFile || file.length() < 16) return false
        val header = ByteArray(16)
        file.inputStream().use { it.read(header) }
        // A plaintext SQLite file starts with "SQLite format 3\u0000".
        return String(header, Charsets.ISO_8859_1) != "SQLite format 3\u0000"
    }
}
