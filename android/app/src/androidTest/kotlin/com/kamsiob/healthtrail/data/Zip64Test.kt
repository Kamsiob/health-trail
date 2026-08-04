package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The zip this format is built on survives past 65,535 entries.
 *
 * `contract/DATA-CONTRACT.md` 8.1 says the outer layer is ZIP64, and the issue
 * asking for it says **confirmed on both the writing and the reading side, not
 * assumed**. That wording is doing real work: the classic zip format keeps a
 * sixteen bit entry count and thirty two bit offsets in its central directory,
 * so an archive that crosses either limit is silently wrong rather than loudly
 * broken, and a caregiver's five year notebook with a photograph of every letter
 * is exactly the archive that crosses one.
 *
 * **This proves the entry count, and it does not prove the size.** Four
 * gigabytes of test data is not something to write on every build, so the size
 * half is stated as untested rather than implied. What is tested here is the
 * limit an ordinary notebook actually reaches: a person who photographs their
 * paperwork for five years passes sixty five thousand files long before they
 * pass four gigabytes.
 *
 * **It is an instrumented test rather than a unit test on purpose.** It was
 * written as a unit test first and passed in a second, which proved that the
 * desktop JVM handles ZIP64 and said nothing about the phone. Android's
 * `java.util.zip` is a different build of similar code, and "similar code
 * probably behaves the same" is exactly the reasoning the issue's word
 * "confirmed" rules out. It runs where the app runs.
 */
class Zip64Test {

    @get:Rule
    val folder = TemporaryFolder()

    /** Past the sixteen bit count, with enough margin that an off by one shows. */
    private val entries = 70_000

    @Test
    fun aZipOfSeventyThousandEntriesIsWrittenAndReadBack() {
        val archive = folder.newFile("many.zip")

        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            for (index in 0 until entries) {
                zip.putNextEntry(ZipEntry("attachments/$index"))
                zip.write(index.toString().toByteArray())
                zip.closeEntry()
            }
        }

        // **Read twice, two different ways, because they fail differently.** A
        // streaming read walks the local headers and never looks at the central
        // directory, so it can succeed on a file that no ordinary zip tool can
        // open. The random access read is the one that goes through the central
        // directory, which is where the sixteen bit count lives.
        var streamed = 0
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                streamed += 1
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertEquals("a streaming read lost entries", entries, streamed)

        ZipFile(archive).use { zip ->
            assertEquals("the central directory lost entries", entries, zip.size())
            // The last one specifically, since a truncated count loses the tail.
            val last = zip.getEntry("attachments/${entries - 1}")
            assertTrue("the last entry is not in the central directory", last != null)
            assertEquals(
                "the last entry does not read back",
                (entries - 1).toString(),
                zip.getInputStream(last).readBytes().decodeToString(),
            )
        }
    }
}
