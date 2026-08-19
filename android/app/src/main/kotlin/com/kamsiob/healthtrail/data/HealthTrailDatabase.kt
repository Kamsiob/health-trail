package com.kamsiob.healthtrail.data

import android.content.Context
import android.database.sqlite.SQLiteException
import com.kamsiob.healthtrail.contract.ContractAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.DatabaseErrorHandler
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * Thrown when the database file is on disk and cannot be read as a database.
 *
 * **Distinct from [DatabaseKeyLost] because the remedy is the same and the
 * sentence is not.** A lost key means the bytes are intact and permanently
 * opaque. This means the bytes themselves are damaged. Telling somebody their
 * key is gone when their file is torn is a false explanation of a true dead
 * end, and the screen says so in its own words. #407.
 */
class DatabaseUnreadable(cause: Throwable) : Exception(
    "The file this notebook is kept in cannot be read as a database. " +
        "Nothing has been deleted.",
    cause,
)

/**
 * What the database says its journal and sync mode are. #408.
 *
 * **Read back out of SQLite, never the value that was asked for.** The defect
 * this exists to close is a pragma that was declared, believed and never
 * checked, and a record of the request would repeat it exactly.
 */
data class JournalMode(val journal: String, val synchronous: String) {
    /** True when write ahead logging is genuinely in force. */
    val isWal: Boolean get() = journal.equals("wal", ignoreCase = true)
}

/**
 * Reports corruption and never acts on it. #407.
 *
 * **The whole point of this object is the method body that is missing.** The
 * library's [net.zetetic.database.DefaultDatabaseErrorHandler] deletes the
 * database file and every database attached to it, so that the next open finds
 * nothing and creates a fresh one. On this app that is every year of somebody's
 * record erased, with no error and nothing on screen: the app simply opens at
 * "Before you start" looking newly installed.
 *
 * **Measured rather than assumed, 2026-08-19.** That default carries an early
 * return when `SQLiteDatabase.hasCodec()` is true, which it is on this build,
 * so the deletion was probably not reachable here. Probably is not a word this
 * project spends a person's record on. Whether the file survives corruption is
 * now a property of this file rather than of a native compile flag in a
 * dependency, and reading this object answers the question that previously
 * needed a decompiler. The `null` that used to be passed here is what made it
 * a question at all.
 *
 * A corrupt database is still a dead end. What changes is that it is a dead end
 * with the bytes still on the disk, which is what an export or a future repair
 * needs, and which is the difference between "this cannot be opened" and
 * "there was never anything here".
 */
internal object NeverDeleteOnCorruption : DatabaseErrorHandler {

    /**
     * The last corruption the library reported, or null.
     *
     * Read straight after an open fails, so the failure can be named as damage
     * rather than guessed at from an exception type. SQLCipher reports a torn
     * file and a wrong key through the same [SQLiteException].
     */
    @Volatile
    var reported: String? = null

    override fun onCorruption(database: SQLiteDatabase, error: SQLiteException) {
        reported = error.message ?: "corruption reported with no message"
        android.util.Log.e("HealthTrail", "corruption reported, nothing deleted", error)
    }
}

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
        const val SCHEMA_VERSION = 3

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

            // **The driver keeps this array rather than reading it once.** #408.
            //
            // `SQLiteDatabaseConfiguration.password` holds the reference it is
            // handed, with no copy, and the connection pool opens every later
            // connection from that same configuration. Zeroing the array we
            // passed in therefore does not tidy up after an open: it arms a
            // trap for the next connection, which opens against a passphrase of
            // all zeroes and fails with "file is not a database".
            //
            // **It has never fired, for one reason that is about to stop being
            // true.** `SQLiteConnectionPool.setMaxConnectionPoolSizeLocked`
            // caps the pool at exactly one connection unless the write ahead
            // logging flag is set, so until this same commit there was never a
            // second connection to fail. Turning WAL on below raises the cap to
            // `SQLiteGlobal.getWALConnectionPoolSize()`, and the failure it
            // produces is reported as corruption, which is the one thing this
            // milestone is about not doing.
            //
            // So the driver gets its own copy and keeps it for the life of the
            // process, and ours is wiped here. The key material stays in heap
            // where SQLCipher needs it to open connections. See D209.
            val forDriver = passphrase.copyOf()

            // **The fourth argument is the error handler and it used to be
            // `null`**, which the library replaces with its own default. #407.
            // Nothing about the record's survival should depend on what a
            // dependency's default happens to do, so the handler is named here.
            NeverDeleteOnCorruption.reported = null
            val database = try {
                SQLiteDatabase.openOrCreateDatabase(
                    file, forDriver, null, NeverDeleteOnCorruption,
                )
            } catch (error: SQLiteException) {
                forDriver.fill(0)
                // **A file that exists and will not open is damage, not a
                // missing key.** Surfaced under its own name so the screen can
                // say which of the two happened. A file that never existed
                // cannot be damaged, so a fresh create that fails is left to
                // travel as itself.
                if (!fresh) throw DatabaseUnreadable(error) else throw error
            } finally {
                passphrase.fill(0)
            }

            // **Foreign keys are set on the pool, not on one connection.** #457.
            //
            // This was `execSQL("PRAGMA foreign_keys = ON")`, and a pragma is
            // per connection. It happened to hold because the pool was capped
            // at one connection, and it would have stopped holding the moment
            // the line below raised that cap: writes landing on a connection
            // that never received the pragma would insert orphan rows, and no
            // test would catch it because the schema still declares every
            // constraint. `setForeignKeyConstraintsEnabled` puts it on the
            // shared configuration instead, which every connection applies as
            // it opens.
            database.setForeignKeyConstraintsEnabled(true)

            // **The journal mode is set through the driver, not through SQL.**
            // #408. `contract/schema.sql` declares `PRAGMA journal_mode = WAL`
            // and it had never once been applied, for two independent reasons,
            // neither of which reported anything:
            //
            // 1. `applySchema` runs its pragmas inside `beginTransaction()`.
            //    SQLite refuses a journal mode change inside a transaction and
            //    answers with the mode it is already in, and that answer was
            //    read and dropped on the floor.
            // 2. The AOSP derived driver here keeps a connection pool and
            //    applies its own journal and sync mode as each connection
            //    opens, so a statement run on one connection would not have
            //    survived anyway.
            //
            // `enableWriteAheadLogging` is the pool's own switch: it sets the
            // flag on the shared configuration and reconfigures every
            // connection, so it is the only form of this that is true for more
            // than one statement. The declaration in the contract stays where
            // it is, because it is the statement of intent; this is what makes
            // it so.
            //
            // **Rollback journal at `synchronous = NORMAL` is what was actually
            // live**, which does not fsync the journal before overwriting
            // pages, so a power loss mid commit could tear the file. That is
            // the failure #407 exists to not delete.
            journalMode = applyJournalMode(database)

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
         * What the journal and sync mode actually became, read back. #408.
         *
         * **Null until a database has been opened in this process.** Written
         * once by [create] and read by the test and by the About screen, so the
         * answer to "which mode is live on the phone" is a thing the app can be
         * asked rather than a thing somebody infers from a declaration.
         */
        @Volatile
        var journalMode: JournalMode? = null
            private set

        /**
         * Turns on write ahead logging and reports what took. #408.
         *
         * **Both values are read back out of the database.** The whole class of
         * defect this fixes is a pragma that is set, believed, and never
         * checked, so setting it and returning the value that was requested
         * would be the same bug wearing a fix. What is returned here is what
         * SQLite says it is in, which is occasionally not what was asked for:
         * WAL cannot be enabled on an in memory database or on one with
         * attached databases, and it fails by declining rather than by throwing.
         */
        private fun applyJournalMode(database: SQLiteDatabase): JournalMode {
            // Outside every transaction. This is the whole point.
            check(!database.inTransaction()) {
                "the journal mode cannot be changed inside a transaction, which is " +
                    "how it came to have never been applied at all"
            }
            val accepted = database.enableWriteAheadLogging()
            val mode = readPragma(database, "journal_mode")
            val sync = readPragma(database, "synchronous")
            val keys = readPragma(database, "foreign_keys")
            if (!accepted || !mode.equals("wal", ignoreCase = true)) {
                android.util.Log.w(
                    "HealthTrail",
                    "#408: write ahead logging was declined, journal_mode is $mode",
                )
            }
            // **Read back rather than trusted**, #457, and on a connection
            // taken from the pool after it was resized rather than on the one
            // the setting was made against.
            check(keys == "1") {
                "foreign keys are not enforced on this connection, PRAGMA foreign_keys = $keys"
            }
            return JournalMode(mode, sync)
        }

        private fun readPragma(database: SQLiteDatabase, name: String): String =
            database.rawQuery("PRAGMA $name", null).use {
                if (it.moveToFirst()) it.getString(0).orEmpty() else ""
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
