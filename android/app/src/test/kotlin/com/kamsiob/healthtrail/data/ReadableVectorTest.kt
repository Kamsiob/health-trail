package com.kamsiob.healthtrail.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The archive's readable copy, rendered against the golden vector, **with no
 * device**. `contract/test-vectors/readable/vector.json`, issue #9, `B4`.
 *
 * **This is the test B4's argument depends on.** The emulator was dropped on the
 * reasoning that data survival is proven "by the export and import round trip
 * against the golden vectors in continuous integration, which is repeatable,
 * runs on every push, and does not depend on any one device's history". That
 * sentence was true about the intent and false about the repository: **nothing
 * in continuous integration rendered a readable page at all.** `RegenerationTest`
 * is instrumented, `DateVectorTest` reads assets, and both need the phone that
 * B4 says should not be the proof. So on any day the phone was unreachable, the
 * strongest guarantee in the format was unchecked.
 *
 * **What this proves.** Given these rows and these words, the renderer produces
 * exactly these bytes, in a left to right language and a right to left one, on
 * every push, forever. That is the half of the round trip that needs no Android:
 * the container half is `check_decrypt_tool.py`, which opens a real archive with
 * the standalone decryptor in continuous integration, and the two halves meet in
 * `RegenerationTest` on the phone.
 *
 * **What it does not prove**, said plainly so a green run is not read as more
 * than it is. It does not exercise SQLCipher, the zip layout, the encryption, or
 * `ReadableRows`, which is the one Android piece of the readable pipeline. It
 * cannot see a defect that lives in reading rows out of a database rather than
 * in turning rows into pages.
 *
 * **The rows are real.** They were taken out of an archive the app itself wrote,
 * so they are rows the app could write, which is the fixture rule applied to a
 * vector. Between them they exercise all twelve rendering decisions, including
 * the four that had defects in them: a stored enum, minor units, a row
 * timestamp, and an integer flag.
 *
 * **Regenerating is deliberate and never automatic.** A diff here means the
 * archive's permanent text or its layout changed, which is a decision somebody
 * made rather than an accident, so it is confirmed by hand:
 *
 *     ./gradlew :app:testDebugUnitTest -Dhealthtrail.vector.write=true
 *
 * then read the diff before committing it. A test that quietly rewrites its own
 * expectation is a test that always passes.
 */
class ReadableVectorTest {

    private val expectedDir = File(
        requireNotNull(System.getProperty("healthtrail.vector.expected")) {
            "The build did not pass healthtrail.vector.expected. See app/build.gradle.kts."
        },
    )

    private val rewriting = System.getProperty("healthtrail.vector.write") == "true"

    private fun render(locale: String) = ReadableArchive.render(
        ReadableArchive.Source(
            tables = ReadableVector.rows,
            words = ReadableVector.words.getValue(locale),
            subjectName = ReadableVector.SUBJECT_NAME,
        ),
        ReadableFieldMap.tables,
    )

    private fun check(locale: String) {
        val pages = render(locale)
        val dir = File(expectedDir, locale)

        if (rewriting) {
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }
            pages.forEach { (path, html) -> File(dir, path).writeText(html) }
            println("Rewrote ${pages.size} expected pages into $dir. Read the diff.")
            return
        }

        assertTrue(
            "no expected pages for $locale at $dir. Regenerate with " +
                "-Dhealthtrail.vector.write=true and read the diff.",
            dir.isDirectory,
        )
        assertEquals(
            "the set of pages changed for $locale",
            dir.listFiles().orEmpty().map { it.name }.sorted(),
            pages.keys.sorted(),
        )
        for ((path, actual) in pages) {
            val expected = File(dir, path).readText()
            if (expected != actual) {
                // Say where, rather than dumping two pages of HTML at somebody.
                val at = expected.zip(actual).indexOfFirst { (a, b) -> a != b }
                    .let { if (it == -1) minOf(expected.length, actual.length) else it }
                val from = (at - 60).coerceAtLeast(0)
                throw AssertionError(
                    "$locale/$path does not match the golden vector, first difference at " +
                        "character $at\n" +
                        "  expected: ...${expected.drop(from).take(160)}\n" +
                        "  rendered: ...${actual.drop(from).take(160)}\n" +
                        "If this change is intended, regenerate with " +
                        "-Dhealthtrail.vector.write=true and commit the diff.",
                )
            }
        }
    }

    @Test
    fun `the vector renders byte for byte in English`() = check("en")

    /**
     * The same rows in Arabic, which is where a rendering defect actually shows.
     *
     * Almost no bidi or escaping defect is visible in English, and the archive
     * carried `lang="ar" dir="rtl"` on pages of English for months. Holding the
     * Arabic bytes is what makes that a build failure rather than something
     * somebody has to notice.
     */
    @Test
    fun `the vector renders byte for byte in Arabic`() = check("ar")

    @Test
    fun `the vector reaches every rendering decision the contract has`() {
        // A vector that misses a decision is a vector that locks nothing about
        // it, and this is how that stays true as decisions are added. The four
        // that had defects in them are named because they are the reason this
        // assertion is here at all: enum, money, timestamp and boolean.
        val reached = buildSet {
            for ((table, rows) in ReadableVector.rows) {
                val fields = ReadableFieldMap.tables[table] ?: continue
                for (field in fields.rendered) {
                    if (rows.any { it[field.column] != null }) add(field.render)
                }
            }
        }
        val all = ReadableFieldMap.tables.values
            .flatMap { it.rendered }
            .map { it.render }
            .toSet()
        assertEquals(
            "the vector no longer covers every rendering decision",
            emptySet<String>(),
            all - reached,
        )
    }
}
