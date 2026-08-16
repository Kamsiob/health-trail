package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Numbers and absence, two of `contract/DATA-CONTRACT.md` 8.4's named failure
 * modes. #212.
 *
 * **Both were held back until the importer existed**, and the reason is worth
 * keeping. 8.5's regeneration test renders both sides with the same code from
 * the same bytes, so a value that was mangled identically on the way out and
 * the way back would match itself and pass. **Only reading the restored
 * database can catch these**, which is why they waited on #211 rather than on
 * anybody's attention.
 *
 * **Numbers.** Money is integer minor units and never a floating point number,
 * and a measurement keeps **both** the text the person typed and the parsed
 * value. The contract names the exact shape of the defect: "so 5.0 does not
 * come back as 5 and a value the app could not parse is not lost."
 *
 * **Absence.** Null, empty string, and zero are three different things and stay
 * three different things. **Not recorded is never rendered as a number.** These
 * collapse into each other quietly, at any layer that treats a column as
 * missing rather than as holding nothing, and once collapsed they cannot be
 * told apart afterward.
 *
 * **What is asserted is the database, not a screen.** A screen has somebody
 * looking at it. These three states look identical in most renderings and the
 * only place the difference survives to be checked is the column itself.
 */
@RunWith(AndroidJUnit4::class)
class RoundTripValueTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var archive: File
    private lateinit var staging: File

    private val secret get() = "a passphrase for the value tests".toCharArray()

    @Before
    fun setUp() {
        Repository.closeForTest()
        archive = File(context.cacheDir, "value-${System.nanoTime()}.htz")
        staging = File(context.cacheDir, "value-staging-${System.nanoTime()}")
    }

    @After
    fun tearDown() {
        archive.delete()
        staging.deleteRecursively()
        Repository.closeForTest()
    }

    private suspend fun roundTrip() {
        Backup.export(context, archive, exportedAt = 1_785_000_000_000L, passphrase = secret)
        val opened = ExportContainer.open(archive, staging, passphrase = secret).getOrThrow()
        Backup.restore(context, opened).getOrThrow()
    }

    private var subject: String? = null

    private suspend fun subject(): String = subject ?: Repository.open(context)
        .createSubject(displayName = "Margaret", relationship = "Mom")
        .also { subject = it }

    private suspend fun column(table: String, id: String, name: String): String? =
        Repository.open(context).columnForTest(table, id, name)

    /**
     * A measure to hang measurements off, from a preset whose value is a number.
     *
     * **Taken from the bundled catalog rather than invented**, so the rows below
     * are rows the app itself writes, which is the fixture rule applied to a
     * test. `isText` is the preset's own answer to whether it takes a number.
     */
    private suspend fun measure(): String {
        val preset = TemplateCatalog.presets(context).first { !it.isText }
        return Repository.open(context).createMeasure(
            subjectId = subject(),
            preset = preset,
            unit = preset.unitOptions.firstOrNull(),
        )
    }

    /**
     * Money is integer minor units, and the same integer afterward.
     *
     * **The defect this rules out is a round trip through a floating point
     * type**, which is what any "amount" that passes through a `Double`
     * eventually does. 1,234,567 minor units is chosen because it is large
     * enough that a float would have to round it and small enough that nothing
     * else would complain.
     */
    @Test
    fun moneyComesBackAsTheSameInteger() = runBlocking {
        val bill = Repository.open(context).createBill(
            subjectId = subject(),
            description = "The ambulance transfer",
            amountMinor = 1_234_567L,
            // One of the five the schema's CHECK allows. Inventing a sixth is
            // how this test first failed, and the constraint caught it, which
            // is the schema doing exactly what it is for.
            state = "needs_attention",
            received = Edtf.parse("2026-08-01")!!,
        )
        assertEquals("1234567", column("bill", bill, "amount_minor"))

        roundTrip()

        assertEquals(
            "the amount changed across the round trip, which is what a float does to money",
            "1234567",
            column("bill", bill, "amount_minor"),
        )
    }

    /**
     * A measurement keeps both what was typed and what was parsed, and 5.0 does
     * not come back as 5.
     *
     * **This is the contract's own example and it is not pedantry.** A reading
     * written as 5.0 was written to one decimal place by somebody who meant one
     * decimal place, and rendering it as 5 loses a fact about how it was
     * measured. The text column is what protects that, so the test asserts both
     * columns rather than either.
     */
    @Test
    fun bothTheTypedTextAndTheParsedNumberSurvive() = runBlocking {
        val id = Repository.open(context).recordMeasurement(
            measureId = measure(),
            number = 5.0,
            text = "5.0",
            occurred = Edtf.parse("2026-08-01")!!,
        )
        assertEquals("5.0", column("measurement", id, "value_text"))
        val numberBefore = column("measurement", id, "value_number")

        roundTrip()

        assertEquals(
            "the text the person typed did not survive, so 5.0 now reads as 5",
            "5.0",
            column("measurement", id, "value_text"),
        )
        assertEquals(
            "the parsed value changed across the round trip",
            numberBefore,
            column("measurement", id, "value_number"),
        )
    }

    /**
     * A value the app could not parse is kept rather than dropped.
     *
     * **"Ate about half her lunch" is not a number and must not become one**,
     * and it must not become nothing either. The row carries words and no
     * number, which is a legitimate and common shape, and an importer that
     * required a number would silently lose the observation.
     */
    @Test
    fun aValueThatIsNotANumberIsNotLost() = runBlocking {
        val words = "Ate about half her lunch, more than yesterday"
        val id = Repository.open(context).recordMeasurement(
            measureId = measure(),
            number = null,
            text = words,
            occurred = Edtf.parse("2026-08-01")!!,
        )

        roundTrip()

        assertEquals("the words were lost", words, column("measurement", id, "value_text"))
        assertNull(
            "a number was invented for a value nobody gave one for",
            column("measurement", id, "value_number"),
        )
    }

    /**
     * Null and zero are two different things, on the same column, at once.
     *
     * **Both are real and they mean opposite things.** A bill with no amount
     * recorded is one nobody has been told the cost of yet; a bill for zero is
     * one that was waived or fully covered, which is a fact somebody may need to
     * prove. Rendering the first as 0 tells a person their record says something
     * it does not.
     *
     * **Asserted against each other rather than each alone**, because the way
     * this fails is that they converge.
     */
    @Test
    fun nullAndZeroStayTwoDifferentThings() = runBlocking {
        val repository = Repository.open(context)
        val unknown = repository.createBill(
            subjectId = subject(),
            description = "The consultant's bill, no figure yet",
            amountMinor = null,
            state = "needs_attention",
            received = Edtf.parse("2026-08-01")!!,
        )
        val waived = repository.createBill(
            subjectId = subject(),
            description = "The transport, waived in full",
            amountMinor = 0L,
            state = "paid",
            received = Edtf.parse("2026-08-01")!!,
        )
        assertNull("the fixture did not produce an unrecorded amount", column("bill", unknown, "amount_minor"))
        assertEquals("the fixture did not produce a zero amount", "0", column("bill", waived, "amount_minor"))

        roundTrip()

        assertNull(
            "an unrecorded amount came back as a number, which says the record holds a " +
                "figure nobody gave",
            column("bill", unknown, "amount_minor"),
        )
        assertEquals("a waived bill lost its zero", "0", column("bill", waived, "amount_minor"))
        assertNotEquals(
            "not recorded and zero are now the same value",
            column("bill", unknown, "amount_minor"),
            column("bill", waived, "amount_minor"),
        )
    }

    /**
     * The same for a measurement, where the third state also exists.
     *
     * A measurement of zero is a reading somebody took. A measurement with no
     * number is an observation in words. **Both are rows the app writes on
     * ordinary days**, and the contract's rule is that they never converge.
     */
    @Test
    fun aReadingOfZeroIsNotAnUnrecordedReading() = runBlocking {
        val measure = measure()
        val repository = Repository.open(context)
        val zero = repository.recordMeasurement(
            measureId = measure,
            number = 0.0,
            text = "0",
            occurred = Edtf.parse("2026-08-01")!!,
        )
        val none = repository.recordMeasurement(
            measureId = measure,
            number = null,
            text = "She would not let me take it",
            occurred = Edtf.parse("2026-08-01")!!,
        )

        roundTrip()

        val stored = column("measurement", zero, "value_number")
        assertTrue("a reading of zero came back as no reading at all", stored != null)
        assertEquals("a reading of zero changed", 0.0, stored!!.toDouble(), 0.0)
        assertNull(
            "an observation with no number came back holding one",
            column("measurement", none, "value_number"),
        )
    }

    /**
     * An empty string stays an empty string rather than becoming null.
     *
     * **Seeded past the repository, and that is the point of the test rather
     * than a shortcut.** Every write path in `Repository` runs `ifBlank { null }`
     * over free text, deliberately, so the app itself does not produce this
     * state today. What is being checked here is the round trip machinery
     * underneath: `sqlcipher_export`, the container, and the restore have to
     * carry a column holding nothing rather than quietly folding it into a
     * column holding no value.
     *
     * **It matters even though the app cannot make one**, because an archive is
     * read by more than this build: a future version that stops trimming, or
     * another platform's importer, would meet exactly this and has a published
     * format telling it the three states are distinct.
     */
    @Test
    fun anEmptyStringDoesNotBecomeNull() = runBlocking {
        val repository = Repository.open(context)
        val id = repository.createEntry(
            subjectId = subject(),
            kind = "call",
            body = "The body is here and the title is deliberately empty",
            occurred = Edtf.parse("2026-08-01")!!,
        )
        HealthTrailDatabase.open(context).database.execSQL(
            "UPDATE entry SET title = '' WHERE id = ?",
            arrayOf<Any?>(id),
        )
        assertEquals("the fixture did not store an empty string", "", column("entry", id, "title"))

        roundTrip()

        val after = column("entry", id, "title")
        assertTrue(
            "an empty title came back as not recorded, so nothing left can tell the two apart",
            after != null,
        )
        assertEquals("an empty string changed across the round trip", "", after)
    }

    /**
     * **The fields added on 2026-08-16 have to survive the archive too.** A new
     * column reaches the database long before it reaches the export spec, and a
     * value that saves, shows on screen, and then vanishes on restore is the
     * worst shape this app can fail in: the person is told their notebook came
     * back and part of it did not. The owner asked for exactly this check.
     *
     * `frequency_text` on a medication, and `email` on a person.
     */
    @Test
    fun theNewFieldsSurviveTheArchive() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = subject()

        val medication = repository.createMedication(
            subjectId = subjectId,
            name = "Levothyroxine",
            doseText = "50 mcg",
            frequencyText = "Every morning, before food",
        )
        val person = repository.createPerson(
            subjectId = subjectId,
            displayName = "Maria Alvarez",
            phone = "5551234567",
            email = "m.alvarez@example.org",
        )

        assertEquals("Every morning, before food", column("medication", medication, "frequency_text"))
        assertEquals("m.alvarez@example.org", column("person", person, "email"))

        roundTrip()

        assertEquals(
            "how often a medication is taken did not survive the archive",
            "Every morning, before food",
            column("medication", medication, "frequency_text"),
        )
        assertEquals(
            "a person's email did not survive the archive",
            "m.alvarez@example.org",
            column("person", person, "email"),
        )
    }

    /**
     * **A blank is not an empty string, on the new fields as on every other.**
     * The writers coerce blank to null so that "unknown" and "deliberately
     * nothing" cannot drift apart across an export, which is the distinction
     * this whole class exists to protect.
     */
    @Test
    fun theNewFieldsKeepNullApartFromEmpty() = runBlocking {
        val repository = Repository.open(context)
        val medication = repository.createMedication(
            subjectId = subject(),
            name = "Aspirin",
            doseText = "81 mg",
            frequencyText = "   ",
        )

        assertNull("a blank frequency should be stored as nothing at all",
            column("medication", medication, "frequency_text"))

        roundTrip()

        assertNull(
            "a blank frequency came back as an empty string rather than nothing",
            column("medication", medication, "frequency_text"),
        )
    }
}
