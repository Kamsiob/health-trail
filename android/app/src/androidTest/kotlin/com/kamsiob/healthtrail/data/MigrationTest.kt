package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.contract.ContractAssets
import java.io.File
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrading a database keeps every row.
 *
 * **Uninstalling to work around a migration is never allowed here.** For most
 * apps that is an inconvenience. This data is years of somebody's record of
 * their mother's care, it exists on one device by design, and there is no
 * server holding a copy. A migration that fails and asks for a clean install
 * is asking a person to delete the thing the app exists to keep.
 *
 * The mechanism therefore exists before it is needed, and this proves it
 * carries rows across rather than proving it compiles.
 *
 * **Most of the steps here are synthetic**, because the mechanism has to be
 * provable independently of whatever the shipped list happens to contain: a
 * failing step, a step from the future, and ordering are all easier to state
 * with a step written for the purpose than with a real one.
 * **The shipped step 2 has its own test** at the bottom, because an additive
 * migration that silently adds nothing looks exactly like one that worked.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var file: File
    private lateinit var db: SQLiteDatabase
    private val schemaSql get() = ContractAssets.readSchema(context)

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        file = File(context.cacheDir, "migration-${System.nanoTime()}.db")
        db = SQLiteDatabase.openOrCreateDatabase(file, "test passphrase", null, null)
        // The app's own schema application, not a copy of it. A test that
        // applies the schema differently proves a schema the app never runs,
        // and this one in particular has to route pragmas away from execSQL,
        // which refuses any statement that returns rows.
        HealthTrailDatabase.applySchema(context, db)
    }

    @After
    fun tearDown() {
        db.close()
        file.delete()
    }

    private fun rows(): Int =
        // allow-base-table: this test is about what survives an upgrade,
        // including rows a live view would filter, so counting the view would
        // measure the wrong thing.
        db.rawQuery("SELECT COUNT(*) FROM subject", null)
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun addSubject(name: String) {
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) " +
                "VALUES (?, ?, ?, 'test', 1, ?)",
            arrayOf<Any>(name, now, now, name),
        )
    }

    @Test
    fun aDatabaseWithNoStampIsBroughtForwardRatherThanRefused() {
        // What a build from before this mechanism existed left behind. It is
        // not an error and it must not trigger anything destructive.
        //
        // An unstamped database is read as being at zero, so every step runs
        // against it. That is only safe because each step is written to be safe
        // against a database that already has what it adds, which is a property
        // of the steps rather than of this mechanism. **A step that is not
        // idempotent does not belong in this list**, because this is the path it
        // will meet.
        addSubject("Margaret")
        db.execSQL("DELETE FROM schema_migration")
        assertEquals(0, Migrations.versionOf(db))

        val at = Migrations.run(db, schemaSql).getOrThrow()

        assertEquals(Migrations.CURRENT, at)
        assertEquals(Migrations.CURRENT, Migrations.versionOf(db))
        assertEquals("bringing an unstamped database forward lost rows", 1, rows())
    }

    @Test
    fun anUpgradePreservesEveryRow() {
        addSubject("Margaret")
        addSubject("Ruth")
        Migrations.run(db, schemaSql).getOrThrow()

        val step = Migrations.Step(2, "adds a column nothing reads yet") { database, _ ->
            database.execSQL("ALTER TABLE subject ADD COLUMN nickname TEXT")
        }

        val at = Migrations.run(db, schemaSql, target = 2, available = listOf(step)).getOrThrow()

        assertEquals(2, at)
        assertEquals("the upgrade lost rows", 2, rows())
        // And the rows are the same rows, not two new empty ones.
        db.rawQuery("SELECT display_name FROM live_subject ORDER BY display_name", null).use {
            val names = buildList { while (it.moveToNext()) add(it.getString(0)) }
            assertEquals(listOf("Margaret", "Ruth"), names)
        }
    }

    @Test
    fun aFailedStepChangesNothingAndTheVersionDoesNotMove() {
        addSubject("Margaret")
        Migrations.run(db, schemaSql).getOrThrow()

        val doomed = Migrations.Step(2, "writes and then fails") { database, _ ->
            database.execSQL(
                "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, " +
                    "display_name) VALUES ('ghost', 1, 1, 'test', 1, 'Ghost')"
            )
            error("the migration could not finish")
        }

        val result = Migrations.run(db, schemaSql, target = 2, available = listOf(doomed))

        assertTrue("a failing step reported success", result.isFailure)
        assertTrue(result.exceptionOrNull() is Migrations.Failed)
        assertEquals("the failed step left rows behind", 1, rows())
        assertEquals("the version moved despite the failure", 1, Migrations.versionOf(db))
    }

    @Test
    fun aFailedStepSaysNothingWasLost() {
        // The message a person may read at the worst possible moment: the app
        // will not open and their notebook is the only copy. It has to say
        // plainly that the records are still there.
        Migrations.run(db, schemaSql).getOrThrow()
        val doomed = Migrations.Step(2, "fails") { _, _ -> error("nope") }

        val message = Migrations.run(db, schemaSql, target = 2, available = listOf(doomed))
            .exceptionOrNull()!!.message!!

        assertTrue("it does not say nothing was lost: $message", "nothing was lost" in message)
        assertTrue("it does not reassure: $message", "still here" in message)
    }

    @Test
    fun aDatabaseFromTheFutureIsRefusedRatherThanOpened() {
        // A downgrade, or an export restored onto an older build. Reading it
        // through a schema that does not describe it loses data in the
        // quietest way available.
        Migrations.stamp(db, 99, "written by a newer app")

        val result = Migrations.run(db, schemaSql, target = Migrations.CURRENT)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Migrations.FromTheFuture)
        val message = result.exceptionOrNull()!!.message!!
        assertTrue("it does not name the version found: $message", "99" in message)
    }

    @Test
    fun stepsApplyInOrderAndOnlyOnce() {
        Migrations.run(db, schemaSql).getOrThrow()
        val applied = mutableListOf<Int>()
        val steps = listOf(
            Migrations.Step(3, "third") { _, _ -> applied += 3 },
            Migrations.Step(2, "second") { _, _ -> applied += 2 },
        )

        Migrations.run(db, schemaSql, target = 3, available = steps).getOrThrow()
        assertEquals(listOf(2, 3), applied)

        // Running again applies nothing, which is what makes it safe to call on
        // every open rather than only on an upgrade.
        Migrations.run(db, schemaSql, target = 3, available = steps).getOrThrow()
        assertEquals(listOf(2, 3), applied)
    }

    @Test
    fun theShippedListMatchesTheVersionItClaims() {
        // A step added without moving CURRENT would never run. Moving CURRENT
        // without a step would stamp a version nothing produced.
        val highest = Migrations.steps.maxOfOrNull { it.version } ?: HealthTrailDatabase.SCHEMA_VERSION
        assertTrue(
            "Migrations.steps reaches $highest but CURRENT is ${Migrations.CURRENT}",
            highest <= Migrations.CURRENT,
        )
        assertEquals(
            "Migrations.CURRENT and HealthTrailDatabase.SCHEMA_VERSION disagree",
            HealthTrailDatabase.SCHEMA_VERSION,
            Migrations.CURRENT,
        )
        assertNotNull(Migrations.steps)
    }

    /**
     * A version 1 database, as that version actually shaped the two tables
     * step 2 alters.
     *
     * Hand-written on purpose. It is a record of a past state rather than a
     * second copy of the current schema, so it must not be regenerated from
     * `contract/schema.sql`: the whole point is that it lacks what today's
     * schema has.
     */
    private fun buildVersionOneDatabase(): Pair<File, SQLiteDatabase> {
        val old = File(context.cacheDir, "v1-${System.nanoTime()}.db")
        val database = SQLiteDatabase.openOrCreateDatabase(old, "test passphrase", null, null)
        database.execSQL(
            "CREATE TABLE schema_migration (version INTEGER NOT NULL PRIMARY KEY, " +
                "applied_at INTEGER NOT NULL, note TEXT)"
        )
        database.execSQL(
            "CREATE TABLE app_meta (key TEXT NOT NULL PRIMARY KEY, value TEXT, " +
                "updated_at INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE change_log (seq INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "table_name TEXT NOT NULL, row_id TEXT NOT NULL, op TEXT NOT NULL, " +
                "rev INTEGER NOT NULL, changed_at INTEGER NOT NULL, device_id TEXT NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE subject (id TEXT NOT NULL PRIMARY KEY, created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, deleted_at INTEGER, origin_device TEXT NOT NULL, " +
                "rev INTEGER NOT NULL DEFAULT 1, display_name TEXT NOT NULL)"
        )
        // project and project_step as version 1 had them: no lead, no
        // current_stage_id, no cluster, no handler_label. The columns that are
        // here are the ones version 1's own indexes name, because replaying the
        // schema recreates those indexes and an index on a column that is not
        // there fails the step.
        database.execSQL(
            "CREATE TABLE project (id TEXT NOT NULL PRIMARY KEY, created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, deleted_at INTEGER, origin_device TEXT NOT NULL, " +
                "rev INTEGER NOT NULL DEFAULT 1, subject_id TEXT NOT NULL, name TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'active')"
        )
        database.execSQL(
            "CREATE TABLE project_step (id TEXT NOT NULL PRIMARY KEY, created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, deleted_at INTEGER, origin_device TEXT NOT NULL, " +
                "rev INTEGER NOT NULL DEFAULT 1, project_id TEXT NOT NULL, text TEXT NOT NULL, " +
                "sort_index INTEGER NOT NULL DEFAULT 0)"
        )
        database.execSQL(
            "INSERT INTO app_meta (key, value, updated_at) VALUES ('device_id', 'old-phone', 1)"
        )
        database.execSQL(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) " +
                "VALUES ('s1', 1, 1, 'old-phone', 1, 'Margaret')"
        )
        database.execSQL(
            "INSERT INTO project (id, created_at, updated_at, origin_device, rev, subject_id, name) " +
                "VALUES ('p1', 1, 1, 'old-phone', 1, 's1', 'The waiver application')"
        )
        database.execSQL(
            "INSERT INTO project_step (id, created_at, updated_at, origin_device, rev, " +
                "project_id, text) VALUES ('st1', 1, 1, 'old-phone', 1, 'p1', 'Get the form')"
        )
        Migrations.stamp(database, 1, "baseline")
        return old to database
    }

    private fun tableExists(database: SQLiteDatabase, name: String): Boolean =
        // allow-base-table: reads sqlite_master, which holds no user rows.
        database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name),
        ).use { it.moveToFirst() }

    private fun columns(database: SQLiteDatabase, table: String): List<String> =
        // allow-base-table: reads the shape of a table, never its rows.
        database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val index = cursor.getColumnIndexOrThrow("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(index)) }
        }

    @Test
    fun theShippedStepTwoAddsTheArrangementTablesAndKeepsTheProjectIntact() {
        // The migration this build actually ships, run against a database that
        // genuinely lacks what it adds. An additive step that silently adds
        // nothing looks exactly like one that worked, so it is proved against a
        // version 1 database rather than against today's.
        val (old, database) = buildVersionOneDatabase()
        try {
            val at = Migrations.run(database, schemaSql, target = 2).getOrThrow()
            assertEquals(2, at)

            for (table in listOf(
                "today_card", "project_stage", "project_standing",
                "project_date", "project_date_kind", "project_paper",
            )) {
                assertTrue("$table was not created", tableExists(database, table))
                assertTrue("live_$table was not created", viewExists(database, "live_$table"))
            }

            assertTrue("project.lead was not added", "lead" in columns(database, "project"))
            assertTrue(
                "project.current_stage_id was not added",
                "current_stage_id" in columns(database, "project"),
            )
            assertTrue("project_step.cluster was not added", "cluster" in columns(database, "project_step"))
            assertTrue(
                "project_step.handler_label was not added",
                "handler_label" in columns(database, "project_step"),
            )

            // The project that was already there is untouched, and it leads with
            // where it stands, which is the shape the old screen had.
            database.rawQuery("SELECT name, lead FROM live_project", null).use {
                assertTrue("the project did not survive the upgrade", it.moveToFirst())
                assertEquals("The waiver application", it.getString(0))
                assertEquals("standing", it.getString(1))
            }
            database.rawQuery("SELECT text, cluster, handler_label FROM live_project_step", null).use {
                assertTrue("the step did not survive the upgrade", it.moveToFirst())
                assertEquals("Get the form", it.getString(0))
                assertTrue("cluster should be empty, not invented", it.isNull(1))
                assertTrue("handler_label should be empty, not invented", it.isNull(2))
            }

            // Running it a second time changes nothing, which is what makes it
            // safe on every open rather than only on an upgrade.
            assertEquals(2, Migrations.run(database, schemaSql, target = 2).getOrThrow())
        } finally {
            database.close()
            old.delete()
        }
    }

    private fun viewExists(database: SQLiteDatabase, name: String): Boolean =
        // allow-base-table: reads sqlite_master, which holds no user rows.
        database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'view' AND name = ?", arrayOf(name),
        ).use { it.moveToFirst() }

    @Test
    fun theRealDatabaseOpensAtTheCurrentVersion() = runBlocking {
        // The mechanism running for real, through the app's own open path.
        Repository.closeForTest()
        val repository = Repository.open(context)
        assertEquals(Migrations.CURRENT, repository.schemaVersionForTest())
    }
}
