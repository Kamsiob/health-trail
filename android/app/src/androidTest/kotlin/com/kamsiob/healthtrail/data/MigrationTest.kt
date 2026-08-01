package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
 * **The steps are synthetic.** `Migrations.steps` is empty because nothing has
 * changed since the baseline, and the alternative to a synthetic step was
 * shipping a fake migration to have something to test, which would put a lie in
 * the migration history of every install forever.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var file: File
    private lateinit var db: SQLiteDatabase

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
    fun aDatabaseWithNoStampIsTakenAsTheBaseline() {
        // What a build from before this mechanism existed left behind. It is
        // not an error and it must not trigger anything destructive.
        db.execSQL("DELETE FROM schema_migration")
        assertEquals(0, Migrations.versionOf(db))

        val at = Migrations.run(db).getOrThrow()

        assertEquals(1, at)
        assertEquals(1, Migrations.versionOf(db))
    }

    @Test
    fun anUpgradePreservesEveryRow() {
        addSubject("Margaret")
        addSubject("Ruth")
        Migrations.run(db).getOrThrow()

        val step = Migrations.Step(2, "adds a column nothing reads yet") { database ->
            database.execSQL("ALTER TABLE subject ADD COLUMN nickname TEXT")
        }

        val at = Migrations.run(db, target = 2, available = listOf(step)).getOrThrow()

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
        Migrations.run(db).getOrThrow()

        val doomed = Migrations.Step(2, "writes and then fails") { database ->
            database.execSQL(
                "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, " +
                    "display_name) VALUES ('ghost', 1, 1, 'test', 1, 'Ghost')"
            )
            error("the migration could not finish")
        }

        val result = Migrations.run(db, target = 2, available = listOf(doomed))

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
        Migrations.run(db).getOrThrow()
        val doomed = Migrations.Step(2, "fails") { error("nope") }

        val message = Migrations.run(db, target = 2, available = listOf(doomed))
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

        val result = Migrations.run(db, target = Migrations.CURRENT)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Migrations.FromTheFuture)
        val message = result.exceptionOrNull()!!.message!!
        assertTrue("it does not name the version found: $message", "99" in message)
    }

    @Test
    fun stepsApplyInOrderAndOnlyOnce() {
        Migrations.run(db).getOrThrow()
        val applied = mutableListOf<Int>()
        val steps = listOf(
            Migrations.Step(3, "third") { applied += 3 },
            Migrations.Step(2, "second") { applied += 2 },
        )

        Migrations.run(db, target = 3, available = steps).getOrThrow()
        assertEquals(listOf(2, 3), applied)

        // Running again applies nothing, which is what makes it safe to call on
        // every open rather than only on an upgrade.
        Migrations.run(db, target = 3, available = steps).getOrThrow()
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

    @Test
    fun theRealDatabaseOpensAtTheCurrentVersion() = runBlocking {
        // The mechanism running for real, through the app's own open path.
        Repository.closeForTest()
        val repository = Repository.open(context)
        assertEquals(Migrations.CURRENT, repository.schemaVersionForTest())
    }
}
