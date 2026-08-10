package com.kamsiob.healthtrail.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import com.kamsiob.healthtrail.time.Edtf

/**
 * The regeneration test. `contract/DATA-CONTRACT.md` 8.5 calls it the important
 * one and this is why:
 *
 * > Export an archive. Import it onto a clean install. Regenerate the readable
 * > copy from the imported database. **Assert it is byte-identical to the
 * > readable copy inside the original archive.**
 *
 * **One assertion, near-total coverage.** The readable copy renders every field
 * the field map says to render, which is 313 columns across 39 tables. Any value
 * lost, shifted, reordered, re-derived, or silently defaulted anywhere in the
 * round trip changes some page's bytes and fails this. A field-by-field
 * assertion would need three hundred lines and would still miss the fields
 * nobody thought to assert on.
 *
 * **It is also the test that makes determinism load-bearing.** Two renders of
 * one database have to be identical, which is why every query feeding it has an
 * explicit `ORDER BY` (8.4, and `check_query_ordering.py`) and why the field map
 * carries an explicit render order per table rather than relying on JSON key
 * order.
 *
 * **What it does not do yet, stated so a green run is not read as more than it
 * is.** It regenerates from the archive's own payload rather than from a
 * notebook restored onto a cleared install. The difference is the importer: this
 * proves the archive round trips through the container, and the fuller version
 * proves it round trips through `Backup.restore` as well. That needs the import
 * path finished, #211, and it is the natural next step for this file.
 *
 * **It holds one thing constant, and the archive records it now.** Since #327 the
 * pages are written in the person's language, so their bytes are a function of
 * the vocabulary as well as of the rows. This test loads the same catalogs on
 * both sides, which is true of an export and a regeneration on one phone and is
 * exactly the case 8.5 describes.
 *
 * **Until 2026-08-10 nothing recorded which language that was**, so a restore
 * onto a phone set to another language would regenerate correct pages that were
 * not the archive's bytes, and no reader could have known. `MANIFEST.json`
 * carries `readable.locale` now, which `contract/DATA-CONTRACT.md` 8.2 always
 * asked for and `EXPORT-FORMAT.md` did not list. #210.
 *
 * **What is still not proven here**, said plainly rather than assumed closed:
 * this test regenerates in the device's own language, which happens to be the
 * one the archive was written in, so it exercises the field's presence and not
 * its use. **Regenerating in the recorded locale rather than the current one is
 * what would close it**, and it needs a `Strings` loaded for a named tag rather
 * than for the device.
 */
class RegenerationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var work: File
    private val secret get() = "a passphrase for the regeneration test".toCharArray()

    @Before
    fun setUp() {
        work = File(context.cacheDir, "regeneration").apply { deleteRecursively(); mkdirs() }
    }

    @After
    fun tearDown() {
        work.deleteRecursively()
    }

    @Test
    fun theReadableCopyRegeneratesByteForByte() = runBlocking {
        seed()

        val archive = File(work, "export.zip")
        Backup.export(
            context = context,
            target = archive,
            exportedAt = 1_785_000_000_000L,
            passphrase = secret,
        )

        // What the archive carries.
        val written = readablePagesInside(archive)
        assertTrue("the archive carries no readable copy at all", written.isNotEmpty())

        // What the same database renders now, from the payload the archive
        // carries rather than from the live notebook, so that anything the
        // container did to the bytes on the way in or out shows up here.
        val staging = File(work, "staging")
        val opened = ExportContainer.open(archive, staging, passphrase = secret)
        assertTrue("the archive did not open: ${opened.exceptionOrNull()}", opened.isSuccess)
        // **The same vocabulary and the same catalogs, because the pages are
        // now a function of both as well as of the rows.** `Backup.export`
        // reads them the same way, so this is the same value the archive was
        // written with, and the caveat in the class comment is about the day it
        // would not be.
        //
        // **The catalogs were missed here for an afternoon and this test is
        // what found it**, on 2026-08-10: the archive said `Nursing home` and
        // the regeneration said `nursing_home`, so byte identity broke on
        // exactly the thing 8.5 exists to hold. `ReadableWords.from` takes no
        // default for them now, so the next omission is a compile error rather
        // than a diff. #329.
        val regenerated = ExportContainer.readablePages(
            opened.getOrThrow().database,
            ReadableWords.from(
                com.kamsiob.healthtrail.i18n.Strings.load(context),
                catalogNames = ReadableWords.catalogNames(context),
            ),
        )

        assertEquals(
            "the set of readable pages changed across the round trip",
            written.keys.sorted(),
            regenerated.keys.sorted(),
        )

        written.forEach { (path, original) ->
            val again = regenerated.getValue(path)
            if (original != again) {
                // Say where, rather than dumping two pages of HTML at somebody.
                val at = original.zip(again).indexOfFirst { (a, b) -> a != b }
                val from = (at - 60).coerceAtLeast(0)
                throw AssertionError(
                    "readable/$path is not byte identical after the round trip, " +
                        "first difference at character $at\n" +
                        "  archive:     ...${original.drop(from).take(140)}\n" +
                        "  regenerated: ...${again.drop(from).take(140)}",
                )
            }
        }
    }

    /**
     * The archive's own readable pages, by unsealing the payload the way the
     * published format says to.
     *
     * Deliberately not through `open`, which reads the inner container and
     * deletes it. This walks `contract/EXPORT-FORMAT.md` section 4 exactly as
     * `tools/decrypt/` does, so if this stops working the standalone tool has
     * stopped working too.
     */
    private fun readablePagesInside(archive: File): Map<String, String> {
        val outer = java.util.zip.ZipInputStream(archive.inputStream()).use { zip ->
            buildMap<String, ByteArray> {
                var entry = zip.nextEntry
                while (entry != null) {
                    put(entry.name, zip.readBytes())
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val encryption = org.json.JSONObject(
            outer.getValue(ExportContainer.OUTER_MANIFEST).decodeToString(),
        ).getJSONObject("encryption")
        val key = ExportCrypto.derive(
            passphrase = secret,
            salt = android.util.Base64.decode(
                encryption.getString("salt"), android.util.Base64.NO_WRAP,
            ),
            iterations = encryption.getInt("kdf_iterations"),
            memoryKib = encryption.getInt("kdf_memory_kib"),
            parallelism = encryption.getInt("kdf_parallelism"),
        )
        val prefix = android.util.Base64.decode(
            encryption.getString("nonce_prefix"), android.util.Base64.NO_WRAP,
        )

        val sealed = outer.getValue(ExportContainer.PAYLOAD)
        val plain = java.io.ByteArrayOutputStream()
        var at = 0
        var index = 0L
        while (at < sealed.size) {
            val size = ((sealed[at].toInt() and 0xFF) shl 24) or
                ((sealed[at + 1].toInt() and 0xFF) shl 16) or
                ((sealed[at + 2].toInt() and 0xFF) shl 8) or
                (sealed[at + 3].toInt() and 0xFF)
            at += 4
            val frame = sealed.copyOfRange(at, at + size)
            at += size
            val last = at >= sealed.size
            plain.write(
                ExportCrypto.decrypt(
                    key, ExportCrypto.chunkNonce(prefix, index), frame,
                    ExportCrypto.frameAad(index, last),
                ),
            )
            index += 1
        }
        ExportCrypto.wipe(key)

        return java.util.zip.ZipInputStream(plain.toByteArray().inputStream()).use { zip ->
            buildMap {
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith(ExportContainer.READABLE)) {
                        put(
                            entry.name.removePrefix(ExportContainer.READABLE),
                            zip.readBytes().decodeToString(),
                        )
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    /**
     * A notebook with the shapes that break round trips.
     *
     * Small on purpose. The point is coverage of **kinds** of value, not volume:
     * a date at day precision and one at month precision, a note with an accent
     * and one with Arabic, an entry with nothing but a kind, and a person.
     */
    private suspend fun seed() {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Margaret", relationship = "Mom")

        repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2026-08-01T14:30")!!,
            body = "Spoke to the night nurse about the dressing",
        )
        // A month, which must never collapse to its first day.
        repository.createEntry(
            subjectId = subject,
            kind = "note",
            occurred = Edtf.parse("2026-07")!!,
            body = "Sometime in July, she mentioned the physiotherapist",
        )
        // Text that is not ASCII, in two scripts, because the readable copy is
        // where an encoding mistake would show up as mojibake rather than as a
        // failure.
        repository.createEntry(
            subjectId = subject,
            kind = "visit",
            occurred = Edtf.parse("2026-06-15")!!,
            body = "José said the same thing as الممرضة",
        )
        // Nothing but a kind, which is what the capture form promises is enough.
        repository.createEntry(
            subjectId = subject,
            kind = "note",
            occurred = Edtf.parse("2026-06-01")!!,
        )

        // **What the person arranged**, contract/DATA-CONTRACT.md 8.7. Without
        // these rows this test proves nothing about them, and the promise that
        // an arranged Today and a shaped project survive the new phone would be
        // asserted in a document and tested nowhere.
        //
        // It is the same defect the fixture kept having: a feature is built,
        // the notebook under test never contains it, and it is never once
        // proved. Here it would have been quiet, because the test would still
        // have passed.
        val project = repository.startProject(
            subjectId = subject,
            templateId = "medicaid_ltc",
            name = "The waiver application",
            steps = listOf("Get the form", "Gather the statements"),
        )
        repository.setProjectLead(project, "date")

        val applied = repository.addProjectStage(project, "Applied")
        repository.addProjectStage(project, "In review")
        repository.moveProjectToStage(project, applied, Edtf.parse("2026-03-05")!!)

        repository.addProjectStanding(
            projectId = project,
            holderLabel = "The county",
            since = Edtf.parse("2026-03")!!,
            activity = "reviewing it",
        )
        // The source is the half that makes a date usable a year later, and a
        // month-precision date here for the same reason the entry above has one.
        repository.addProjectDate(
            projectId = project,
            kind = "Decision expected",
            due = Edtf.parse("2026-09-12")!!,
            sourceNote = "the letter of Mar 5",
        )
        repository.addProjectDateKind(project, "Decision expected")
        repository.addProjectPaper(project, "The award letter", direction = "received")

        val step = repository.projectSteps(project).first()
        repository.setProjectStepHandling(
            step.id,
            cluster = "The paperwork",
            handlerLabel = "My brother",
        )

        // A Today with a lead, a field, and a card that points at something, so
        // the source pair travels rather than only the card types.
        repository.setTodayLayout(
            subjectId = subject,
            cards = listOf(
                "digest" to "wide",
                "next_up" to "small",
                "project_date" to "small",
            ),
            sources = mapOf(2 to ("project" to project)),
        )
    }
}
