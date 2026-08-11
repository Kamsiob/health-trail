package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Somebody can track a thing the catalog never heard of.
 *
 * **Sixteen presets was the only way in**, so a family weighing a wound,
 * counting good days, or writing down how far she walked to the door had no way
 * to say so, and the sixteen read as the only sixteen things that count. #203.
 *
 * **The same argument `createProject` already makes**, in the section of the
 * repository right above this: "Sixteen catalog processes is a good starting
 * set and it is not the world."
 *
 * **A number and words stay different things**, which is the schema's own
 * distinction and not a flag on one column: "Ate about half her lunch" is not a
 * number, and storing it as one would either lose it or invent a figure nobody
 * gave.
 */
@RunWith(AndroidJUnit4::class)
class OwnMeasureTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    @Test
    fun somethingNamedByThePersonIsTrackedLikeAnythingElse() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Own measure")

        val id = repository.createOwnMeasure(
            subjectId = subjectId,
            name = "Glasses of water",
            unit = "glasses",
            isText = false,
        )
        repository.recordMeasurement(
            measureId = id,
            number = 4.0,
            text = "",
            unit = "glasses",
            occurred = Edtf.parse("2026-08-11")!!,
            note = "",
        )

        val measure = repository.measures(subjectId).single()
        assertEquals("Glasses of water", measure.name)
        // **No preset behind it**, which is what the schema already means by
        // something the person set up themselves.
        assertNull(measure.presetId)
        assertEquals("glasses", measure.unit)
        assertFalse(measure.isText)

        val readings = repository.readings(subjectId)
        assertEquals(1, readings.size)
        assertEquals(4.0, readings.single().number!!, 0.0001)
    }

    /**
     * **Words are a different column, and stay one.** The style a text measure
     * is written with is what `isText` reads back from, so this proves the two
     * ends agree rather than proving one of them in isolation.
     */
    @Test
    fun somethingKeptInWordsComesBackAsWords() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "In words")

        val id = repository.createOwnMeasure(
            subjectId = subjectId,
            name = "How the wound looks",
            unit = null,
            isText = true,
        )
        repository.recordMeasurement(
            measureId = id,
            number = null,
            text = "Less red than yesterday, still weeping a little.",
            unit = null,
            occurred = Edtf.parse("2026-08-11")!!,
            note = "",
        )

        val measure = repository.measures(subjectId).single()
        assertTrue(measure.isText)
        assertNull(measure.unit)

        val reading = repository.readings(subjectId).single()
        assertNull(reading.number)
        assertEquals("Less red than yesterday, still weeping a little.", reading.text)
    }

    /**
     * **A blank unit is null rather than an empty string**, so "no unit" is one
     * value rather than two that look the same on a screen and different in an
     * archive. 8.4's absence rule.
     */
    @Test
    fun aBlankUnitIsAbsentRatherThanEmpty() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "No unit")

        repository.createOwnMeasure(
            subjectId = subjectId,
            name = "Good days",
            unit = "   ",
            isText = false,
        )

        assertNull(repository.measures(subjectId).single().unit)
    }
}
