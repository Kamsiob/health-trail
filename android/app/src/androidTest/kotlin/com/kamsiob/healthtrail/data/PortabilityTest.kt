package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Whether an export can be opened by an app that is not the one that wrote it.
 *
 * **This is the property the whole backup story rests on.** `DECISIONS.md` D24
 * makes the export file the only recovery path from key loss, and
 * `DatabaseKey.kt` says out loud that an export "has to open on a different
 * device, so it cannot depend on one device's keystore". A file that only opens
 * on the phone that wrote it is not a backup, it is a copy, and the difference
 * only shows up on the day somebody's phone is gone.
 *
 * The database at rest is SQLCipher, keyed by 32 random bytes wrapped by the
 * Android Keystore. If the archive carries that file as it sits on disk, the
 * bytes are useless anywhere else: the wrapping key cannot be exported and does
 * not travel, by design and correctly.
 *
 * So the check is on the payload rather than on a second device. A plain SQLite
 * file begins with the sixteen byte string "SQLite format 3\u0000". A SQLCipher
 * file begins with its random per database salt, which is why the header is the
 * one thing that tells the two apart without a key.
 */
@RunWith(AndroidJUnit4::class)
class PortabilityTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var work: File

    /** What every unencrypted SQLite file starts with, header format included. */
    private val sqliteMagic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    @Before
    fun setUp() {
        work = File(context.cacheDir, "portability-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        work.deleteRecursively()
        HealthTrailDatabase.closeForTest()
        Repository.closeForTest()
    }

    @Test
    fun theArchiveNeverCarriesAReadableDatabase() = runBlocking {
        // **The other half of D67, and it only became testable when the payload
        // became portable.** Since format version 2 there is no unencrypted
        // export, so the bytes sitting in the archive must never be a database
        // anybody can open. Before the portability fix this was true by
        // accident, because the payload was a device keyed SQLCipher file that
        // nothing could read. It is true on purpose now, and this is what would
        // catch it silently becoming untrue again.
        val target = File(work, "export.htx")
        Backup.export(
            context,
            target,
            exportedAt = 1_785_000_000_000L,
            passphrase = "a passphrase for the tests".toCharArray(),
        )

        val header = databaseHeaderIn(target)

        assertFalse(
            "The payload inside the export is a plain SQLite file, readable by " +
                "anyone who opens the archive. That is the whole record of " +
                "somebody's care in the clear, in a file that a backup agent or a " +
                "cloud sync may copy anywhere. Since format version 2 every export " +
                "is encrypted and there is no path that writes one without a " +
                "passphrase. D67.",
            header.contentEquals(sqliteMagic),
        )
    }

    /**
     * The same property stated the other way around, so a regression cannot pass
     * by making the header check vacuous.
     *
     * An export written with a passphrase is encrypted at the container layer,
     * so its payload is deliberately not a SQLite header. Opening it with the
     * passphrase has to yield one.
     */
    @Test
    fun anEncryptedExportDecryptsToAPortableDatabase() = runBlocking {
        val target = File(work, "encrypted.htx")
        Backup.export(
            context,
            target,
            exportedAt = 1_785_000_000_000L,
            passphrase = "a passphrase for the test".toCharArray(),
        )

        val opened = ExportContainer.open(
            target,
            File(work, "staging"),
            passphrase = "a passphrase for the test".toCharArray(),
        ).getOrThrow()

        val header = opened.database.readBytes().copyOfRange(0, sqliteMagic.size)
        assertTrue(
            "An encrypted export did not decrypt to a portable database. Header was " +
                header.joinToString(" ") { "%02x".format(it) },
            header.contentEquals(sqliteMagic),
        )
    }

    /**
     * The header check is only worth anything if it can fail, so this proves the
     * live database really is SQLCipher and really does not carry that header.
     */
    @Test
    fun theDatabaseAtRestIsEncryptedSoTheCheckAboveIsNotVacuous() = runBlocking {
        HealthTrailDatabase.open(context)
        val live = context.getDatabasePath(HealthTrailDatabase.FILE_NAME)
        val header = live.inputStream().use { it.readNBytes(sqliteMagic.size) }

        assertEquals(sqliteMagic.size, header.size)
        assertTrue(
            "The database at rest is a plain SQLite file, which means it is not " +
                "encrypted at all and the contract's first promise is broken.",
            !header.contentEquals(sqliteMagic),
        )
    }

    private fun databaseHeaderIn(archive: File): ByteArray =
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == ExportContainer.DATABASE) {
                    return@use zip.readNBytes(sqliteMagic.size)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            throw AssertionError("the export has no database entry at all")
        }
}
