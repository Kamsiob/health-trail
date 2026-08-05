package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The database, exercised on a real device rather than reasoned about.
 *
 * **These run on the connected phone.** There is no emulator in this project.
 *
 * One operational step comes before running them, and it is a checklist item
 * rather than a reason to avoid running: `connectedAndroidTest` uninstalls the
 * application, taking its data with it. If the phone holds anything worth
 * keeping, export through the app first and reimport afterward.
 *
 * Data survival across updates is proven by the export and import round trip
 * against golden vectors in continuous integration, not by a long lived
 * installation, so nothing on the phone needs preserving as evidence. See
 * `DECISIONS.md` D25.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        // Through the repository, so both caches clear together. Closing the
        // database alone used to leave the repository holding a closed handle,
        // which surfaced as a failure in a later test class rather than here.
        Repository.closeForTest()
    }

    @Test
    fun theDatabaseOpensAndCarriesTheContractSchema() {
        val db = runBlocking { HealthTrailDatabase.open(context) }

        assertEquals(
            "schema version was not recorded",
            HealthTrailDatabase.SCHEMA_VERSION,
            db.schemaVersion(),
        )

        val tables = db.database.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
            null,
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val views = db.database.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='view'", null,
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val triggers = db.database.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger'", null,
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        // The user data tables plus app_meta, device, change_log, conflict_log,
        // and schema_migration. android_metadata is not created here, because
        // SQLCipher does not add it the way the platform helper does.
        //
        // **The counts come from the contract, not from this file.** They were
        // 34 and 68 and are now 40 and 80, and a number written here means every
        // table added to the contract fails a test about whether the database
        // opened. The shape is what this asserts: one live view per user data
        // table and two triggers each, which is what makes a forgotten
        // tombstone filter or an unlogged write impossible rather than unlikely.
        val localTables = 5
        val userTables = tables - localTables
        assertTrue("too few tables: $tables", userTables > 0)
        assertEquals("a live view is missing", userTables, views)
        assertEquals("a change log trigger is missing", userTables * 2, triggers)
    }

    @Test
    fun theFileOnDiskIsNotReadableAsPlainSqlite() {
        val db = runBlocking { HealthTrailDatabase.open(context) }
        assertTrue(
            "the database file begins with the plaintext SQLite header, so it " +
                "is not encrypted at rest",
            db.fileIsEncrypted(context),
        )
    }

    @Test
    fun theDeviceIdExistsAndIsWhatTheTriggersWillRead() {
        val db = runBlocking { HealthTrailDatabase.open(context) }
        assertNotNull(db.deviceId)
        assertTrue(db.deviceId.isNotBlank())

        val stored = db.database.rawQuery(
            "SELECT value FROM app_meta WHERE key='device_id'", null,
        ).use { if (it.moveToFirst()) it.getString(0) else null }
        assertEquals(
            "app_meta disagrees with the opened database, which would make every " +
                "change log row say unknown-device",
            db.deviceId,
            stored,
        )
    }

    @Test
    fun everyWriteAppendsToTheChangeLogInTheSameTransaction() {
        val db = runBlocking { HealthTrailDatabase.open(context) }
        val id = Ids.new()
        val now = System.currentTimeMillis()

        val before = db.changeLogSize()

        db.database.execSQL(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) " +
                "VALUES (?, ?, ?, ?, 1, ?)",
            arrayOf<Any>(id, now, now, db.deviceId, "Test subject"),
        )
        assertEquals("insert did not append one change log row", before + 1, db.changeLogSize())

        db.database.execSQL(
            "UPDATE subject SET display_name = ?, updated_at = ?, rev = 2 WHERE id = ?",
            arrayOf<Any>("Renamed", now + 1, id),
        )
        assertEquals("update did not append one change log row", before + 2, db.changeLogSize())

        // Deletion is an update that sets deleted_at, and it must log as a
        // delete. A peer reading it as an edit would not remove the row.
        db.database.execSQL(
            "UPDATE subject SET deleted_at = ?, updated_at = ?, rev = 3 WHERE id = ?",
            arrayOf<Any>(now + 2, now + 2, id),
        )

        val ops = db.database.rawQuery(
            "SELECT op FROM change_log WHERE row_id = ? ORDER BY seq", arrayOf(id),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("insert", "update", "delete"), ops)

        val device = db.database.rawQuery(
            "SELECT device_id FROM change_log WHERE row_id = ? LIMIT 1", arrayOf(id),
        ).use { if (it.moveToFirst()) it.getString(0) else null }
        assertEquals("the change log did not record this device", db.deviceId, device)
    }

    @Test
    fun aTombstonedRowLeavesTheLiveViewButStaysInTheTable() {
        val db = runBlocking { HealthTrailDatabase.open(context) }
        val id = Ids.new()
        val now = System.currentTimeMillis()

        db.database.execSQL(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) " +
                "VALUES (?, ?, ?, ?, 1, ?)",
            arrayOf<Any>(id, now, now, db.deviceId, "Vanishing"),
        )

        fun count(source: String): Int = db.database.rawQuery(
            "SELECT COUNT(*) FROM $source WHERE id = ?", arrayOf(id),
        ).use { if (it.moveToFirst()) it.getInt(0) else -1 }

        assertEquals(1, count("live_subject"))

        db.database.execSQL(
            "UPDATE subject SET deleted_at = ?, updated_at = ?, rev = 2 WHERE id = ?",
            arrayOf<Any>(now + 1, now + 1, id),
        )

        assertEquals(
            "a deleted row is still visible through the live view, which is a " +
                "leak of something the person believed they deleted",
            0,
            count("live_subject"),
        )
        assertEquals(
            "the tombstone was removed from the base table, so a peer could " +
                "never learn about the deletion",
            1,
            count("subject"),
        )
    }

    @Test
    fun reopeningKeepsTheSameDeviceIdAndTheSameData() {
        // This is the upgrade path in miniature. Uninstalling to work around a
        // migration is never allowed here, so reopening must be lossless.
        val first = runBlocking { HealthTrailDatabase.open(context) }
        val deviceId = first.deviceId
        val id = Ids.new()
        val now = System.currentTimeMillis()
        first.database.execSQL(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) " +
                "VALUES (?, ?, ?, ?, 1, ?)",
            arrayOf<Any>(id, now, now, deviceId, "Survives a reopen"),
        )

        HealthTrailDatabase.closeForTest()
        val second = runBlocking { HealthTrailDatabase.open(context) }

        assertEquals("the device id changed across a reopen", deviceId, second.deviceId)
        assertEquals(
            "the schema was reapplied on reopen",
            HealthTrailDatabase.SCHEMA_VERSION,
            second.schemaVersion(),
        )

        val name = second.database.rawQuery(
            "SELECT display_name FROM live_subject WHERE id = ?", arrayOf(id),
        ).use { if (it.moveToFirst()) it.getString(0) else null }
        assertEquals("a row written before the reopen did not survive it", "Survives a reopen", name)
    }

    @Test
    fun theWrongPassphraseCannotOpenTheFile() {
        runBlocking { HealthTrailDatabase.open(context) }
        HealthTrailDatabase.closeForTest()

        val file = context.getDatabasePath(HealthTrailDatabase.FILE_NAME)
        var opened = true
        try {
            net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                file, "not the right passphrase", null, null,
            // allow-base-table: proving the file will not open at all, so the
            // query must reach the storage layer rather than a view. It is
            // expected to throw before a single row is read.
            ).use { it.rawQuery("SELECT COUNT(*) FROM subject", null).use { c -> c.moveToFirst() } }
        } catch (_: Exception) {
            opened = false
        }
        assertFalse("the database opened with the wrong passphrase", opened)
    }
}
