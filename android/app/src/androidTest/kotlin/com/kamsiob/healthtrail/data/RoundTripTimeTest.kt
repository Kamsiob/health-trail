package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.io.File
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Time, the first of `contract/DATA-CONTRACT.md` 8.4's named failure modes.
 * #212.
 *
 * **The sentence the contract puts it in is the whole test:** "A note written on
 * July 6 must still read July 6 after the person moves to another country." A
 * timestamp is not a date, and an app that stores one and renders the other has
 * a record that quietly reinterprets itself the day somebody travels.
 *
 * **Why this is a real risk here rather than a theoretical one.**
 * `Repository.dateColumns` resolves an EDTF string to a start and end using
 * `ZoneId.systemDefault()` at the moment of writing, and `Backup.recomputeRanges`
 * recomputes those two columns on every import. **So the derived range is
 * computed twice, in two places, potentially on two continents.** If the second
 * computation reaches for the device's zone rather than the one stored beside
 * the string, an entry silently moves a day, and both values look plausible on
 * their own so nothing would ever flag it.
 *
 * **What this class does and does not prove**, said plainly.
 *
 * It sets the **process** default zone with `TimeZone.setDefault`, which is what
 * `ZoneId.systemDefault()` reads, so the code under test sees exactly what it
 * would see on a phone set to that zone. **It does not change the phone**, which
 * is deliberate: the device is the owner's daily driver and rule 19's settings
 * exception covers font scale, animation and the reader, and nothing else. What
 * it therefore cannot prove is anything living below `java.time`, in the
 * platform's own zone handling.
 *
 * **Every zone here is one with a real, awkward history.** `Pacific/Chatham` is
 * a forty five minute offset, `America/New_York` and `Australia/Sydney` change
 * on opposite sides of the year, and `Asia/Kolkata` never changes at all.
 */
@RunWith(AndroidJUnit4::class)
class RoundTripTimeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var archive: File
    private lateinit var staging: File

    /**
     * The zone this process started in, restored in teardown.
     *
     * **Restored rather than assumed harmless.** The default zone is process
     * wide, so a test that left it set would move every later class in the run
     * to another country, and the failures would appear in tests that have
     * nothing to do with time.
     */
    private lateinit var originalZone: TimeZone

    private val secret get() = "a passphrase for the time tests".toCharArray()

    @Before
    fun setUp() {
        Repository.closeForTest()
        originalZone = TimeZone.getDefault()
        archive = File(context.cacheDir, "time-${System.nanoTime()}.htz")
        staging = File(context.cacheDir, "time-staging-${System.nanoTime()}")
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
        archive.delete()
        staging.deleteRecursively()
        Repository.closeForTest()
    }

    private fun inZone(id: String) = TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(id)))

    private suspend fun roundTrip() {
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val opened = ExportContainer.open(archive, staging, passphrase = secret).getOrThrow()
        Backup.restore(context, opened).getOrThrow()
    }

    /** One entry, written as if the phone were in [zone], on [day]. */
    private suspend fun entryOn(day: String, zone: String, body: String): String {
        inZone(zone)
        return Repository.open(context).createEntry(
            subjectId = subject(),
            kind = "call",
            occurred = Edtf.parse(day)!!,
            body = body,
        )
    }

    private var subject: String? = null

    private suspend fun subject(): String = subject ?: Repository.open(context)
        .createSubject(displayName = "Margaret", relationship = "Mom")
        .also { subject = it }

    /** The four date columns of one entry, read past the live view. */
    private suspend fun dateOf(id: String): Map<String, String?> {
        val repository = Repository.open(context)
        return listOf("occurred_edtf", "occurred_zone", "occurred_start", "occurred_end")
            .associateWith { repository.columnForTest("entry", id, it) }
    }

    /**
     * The contract's own sentence, as an assertion.
     *
     * Written on a phone in New York, restored on a phone in Tokyo, and it has
     * to still be July 6. **Tokyo is fourteen hours ahead in July**, which is
     * more than enough to move a start-of-day instant into the previous or the
     * next date, so a resolver reaching for the wrong zone fails here rather
     * than by a margin too small to notice.
     */
    @Test
    fun aNoteWrittenOnJulySixthStillReadsJulySixthAfterMovingCountry() = runBlocking {
        val id = entryOn("2026-07-06", "America/New_York", "Spoke to the ward in New York")
        val before = dateOf(id)
        assertEquals("the fixture did not store the writing zone", "America/New_York", before["occurred_zone"])

        // The person emigrates between the export and the restore, which is
        // exactly the case the contract describes and the only one that matters.
        inZone("Asia/Tokyo")
        roundTrip()

        val after = dateOf(id)
        assertEquals("the EDTF string changed", "2026-07-06", after["occurred_edtf"])
        assertEquals("the writing zone changed", "America/New_York", after["occurred_zone"])
        assertEquals(
            "the derived range was recomputed in the reading device's zone rather than the " +
                "writing one, so the entry has moved",
            before,
            after,
        )
    }

    /**
     * The same, in the direction that crosses the date line the other way.
     *
     * **Both directions, because an off-by-one-zone defect is directional.**
     * Resolving in the reader's zone when the reader is behind the writer moves
     * an entry one way; ahead moves it the other, and a test that only went one
     * way would pass on half of a broken implementation.
     */
    @Test
    fun theSameHoldsMovingWest() = runBlocking {
        val id = entryOn("2026-07-06", "Australia/Sydney", "Spoke to the ward in Sydney")
        val before = dateOf(id)

        inZone("America/Los_Angeles")
        roundTrip()

        assertEquals("the entry moved when the person went west", before, dateOf(id))
    }

    /**
     * A zone whose offset is not a whole hour.
     *
     * **Forty five minutes is the case an implementation that stores an offset
     * instead of a zone gets wrong**, and it is a real place with real people in
     * it. It is here because "store the zone id, not the offset" is a rule the
     * schema states and only an awkward zone can actually check.
     */
    @Test
    fun aFortyFiveMinuteZoneSurvives() = runBlocking {
        val id = entryOn("2026-07-06", "Pacific/Chatham", "The Chatham Islands are a real place")
        val before = dateOf(id)

        inZone("Asia/Kolkata")
        roundTrip()

        val after = dateOf(id)
        assertEquals("Chatham's zone did not survive", "Pacific/Chatham", after["occurred_zone"])
        assertEquals("a forty five minute offset did not survive", before, after)
    }

    /**
     * Across a daylight saving boundary, in both directions, which 8.4 names.
     *
     * **The two days that do not have twenty four hours in them.** In
     * `America/New_York`, 2026-03-08 is twenty three hours long and 2026-11-01
     * is twenty five. A day resolved as "start plus 86,400,000" is wrong on
     * both, and wrong in opposite directions, which is why the contract asks for
     * both rather than for one.
     */
    @Test
    fun bothDaylightSavingBoundariesSurvive() = runBlocking {
        val forward = entryOn("2026-03-08", "America/New_York", "The day the clocks went forward")
        val back = entryOn("2026-11-01", "America/New_York", "The day the clocks went back")
        val before = mapOf(forward to dateOf(forward), back to dateOf(back))

        // The lengths are asserted before the round trip, because a resolver
        // that was already wrong would otherwise round trip its own mistake
        // faithfully and this test would pass on a broken app.
        val short = before.getValue(forward).let {
            it["occurred_end"]!!.toLong() - it["occurred_start"]!!.toLong()
        }
        val long = before.getValue(back).let {
            it["occurred_end"]!!.toLong() - it["occurred_start"]!!.toLong()
        }
        val day = 24 * 60 * 60 * 1000L
        assertTrue("the spring forward day was resolved as a full day: $short", short < day)
        assertTrue("the fall back day was resolved as a full day: $long", long > day)
        assertNotEquals("both boundaries resolved to the same length", short, long)

        inZone("Europe/London")
        roundTrip()

        assertEquals("the spring forward day changed", before.getValue(forward), dateOf(forward))
        assertEquals("the fall back day changed", before.getValue(back), dateOf(back))
    }

    /**
     * A device whose clock is wrong does not rewrite what is already recorded.
     *
     * **8.3 says nothing is invented at import: "no refreshed timestamps on
     * existing rows".** The clock is the most available thing an importer could
     * reach for and the most damaging, because `created_at` and `updated_at` are
     * what a merge resolves by. A restore onto a phone whose clock is a year out
     * that restamped every row would make that notebook win every future
     * conflict, forever, silently.
     *
     * The wrong clock is expressed as an absurd `exportedAt` rather than by
     * changing the device's time, which is not this session's to change.
     */
    @Test
    fun aWrongClockDoesNotRestampWhatIsAlreadyThere() = runBlocking {
        val id = entryOn("2026-07-06", "America/New_York", "Written while the clock was right")
        val repository = Repository.open(context)
        val createdBefore = repository.columnForTest("entry", id, "created_at")
        val updatedBefore = repository.columnForTest("entry", id, "updated_at")
        val revBefore = repository.columnForTest("entry", id, "rev")

        // 1975, which no phone in this app's life can honestly report, so a
        // value derived from it would be unmistakable rather than plausible.
        Backup.export(context, archive, exportedAt = 173_000_000_000L, passphrase = secret)
        val opened = ExportContainer.open(archive, staging, passphrase = secret).getOrThrow()
        Backup.restore(context, opened).getOrThrow()

        val after = Repository.open(context)
        assertEquals("created_at was rewritten", createdBefore, after.columnForTest("entry", id, "created_at"))
        assertEquals("updated_at was rewritten", updatedBefore, after.columnForTest("entry", id, "updated_at"))
        assertEquals("rev was bumped by a restore", revBefore, after.columnForTest("entry", id, "rev"))
    }
}
