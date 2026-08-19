package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.RandomAccessFile

/**
 * A damaged database file is surfaced, never deleted. #407.
 *
 * **The defect this covers produced no error.** The library's default error
 * handler deletes the database on corruption, the next open finds no file and
 * creates a fresh one, and the app comes up at "Before you start" looking newly
 * installed. Every year of somebody's record, gone, with nothing on screen. So
 * the assertion that matters here is not that the open fails, which it always
 * did. It is that the bytes are still on the disk afterward.
 *
 * **The file is put back byte for byte before this class returns.** Damage is
 * done to the real database on purpose, because the code path under test is the
 * app's own open and a copy somewhere else would exercise a different one. That
 * makes leaving the phone in a working state part of the test rather than
 * politeness: every class after this one opens the same file.
 */
@RunWith(AndroidJUnit4::class)
class CorruptionTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    @Test
    fun aDamagedDatabaseIsNotDeleted() {
        runBlocking { HealthTrailDatabase.open(context) }
        Repository.closeForTest()

        val file = context.getDatabasePath(HealthTrailDatabase.FILE_NAME)
        val original = file.readBytes()
        assertTrue("the fixture needs a database bigger than one page", original.size > 8192)

        try {
            // **Past the salt and into the first page.** SQLCipher keeps the key
            // derivation salt in the first 16 bytes; damaging those makes the
            // key wrong, which is a different failure with a different sentence.
            // Damaging page content after it is the torn file this is about.
            RandomAccessFile(file, "rw").use { handle ->
                handle.seek(2048)
                handle.write(ByteArray(4096) { 0x7A })
            }

            val failure = runCatching { runBlocking { HealthTrailDatabase.open(context) } }
                .exceptionOrNull()

            assertTrue(
                "a torn file must not open, and it must say which dead end this is, " +
                    "got ${failure?.let { it::class.java.simpleName }}",
                failure is DatabaseUnreadable,
            )
            assertTrue("the database file must still be on disk", file.exists())
            assertEquals(
                "and it must not have been erased or rebuilt underneath the person",
                original.size.toLong(),
                file.length(),
            )
        } finally {
            Repository.closeForTest()
            file.writeBytes(original)
            Repository.closeForTest()
        }

        // The put back file opens, which is what proves the restore above rather
        // than assuming it.
        runBlocking { HealthTrailDatabase.open(context) }
        Repository.closeForTest()
    }

    /**
     * What the library's default handler would actually have done here.
     *
     * **Recorded on the device rather than argued about.** #407 says the default
     * deletes the file. It does, except that
     * `DefaultDatabaseErrorHandler.onCorruption` returns before the deletion
     * when `hasCodec()` is true, and this build links SQLCipher, so it probably
     * never was reachable. This test states which it is on this phone so that
     * nobody decompiles the dependency a second time to find out.
     *
     * It asserts nothing about the value, because the app no longer depends on
     * it either way. That is the point of naming the handler at the open.
     */
    @Test
    fun theDefaultHandlersDeletionIsRecordedRatherThanAssumed() {
        System.loadLibrary("sqlcipher")
        val hasCodec = net.zetetic.database.sqlcipher.SQLiteDatabase.hasCodec()
        android.util.Log.i(
            "HealthTrail",
            "#407: SQLiteDatabase.hasCodec() = $hasCodec on this build. " +
                "True means the library default would have returned before deleting.",
        )
    }
}
