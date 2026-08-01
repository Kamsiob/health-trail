package com.kamsiob.healthtrail.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.i18n.Strings
import com.kamsiob.healthtrail.time.Edtf
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recording a measurement, which is the capture input with the sharpest content
 * rule attached to it.
 *
 * `CLAUDE.md` rule 2: the app records, organizes, and counts, and never
 * concludes. On this screen that means no range, no normal value, no threshold,
 * and no judgment of any number, ever. Two of the tests here exist to make that
 * a thing the build checks rather than a thing somebody remembered.
 */
@RunWith(AndroidJUnit4::class)
class MeasurementTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private suspend fun preset(id: String): TemplateCatalog.Preset =
        TemplateCatalog.presets(context).first { it.id == id }

    @Test
    fun aMeasureIsCreatedTheFirstTimeSomethingIsRecorded() = runBlocking {
        // Never at setup. A notebook that arrives with sixteen empty charts is
        // a list of things somebody has not done, which is the scorecard this
        // app does not keep.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Measure subject")

        assertTrue("a new notebook already tracked something", repository.measures(subjectId).isEmpty())

        val measureId = repository.createMeasure(subjectId, preset("weight"), unit = "lb")
        repository.recordMeasurement(measureId = measureId, number = 163.4, unit = "lb")

        val measures = repository.measures(subjectId)
        assertEquals(1, measures.size)
        assertEquals("Weight", measures.first().name)
        assertEquals("lb", measures.first().unit)

        // Counted through the section too, now that counts take a subject.
        // This assertion is what failed when they did not, returning two after
        // one measure was created, which is how #58 was found.
        assertEquals(1, repository.count(Repository.Section.PROGRESS, subjectId))
    }

    @Test
    fun aNumberIsStoredAsANumberAndWordsAsWords() = runBlocking {
        // "Ate about half her lunch" is not a number, and forcing it into one
        // would either lose it or invent a figure nobody gave.
        assertFalse("weight should take a number", preset("weight").isText)
        assertTrue("eating should take words", preset("appetite").isText)
        assertTrue("mood should take words", preset("mood_behavior").isText)
    }

    @Test
    fun aMeasurementCarriesTheDateAsEdtf() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Measure subject")
        val measureId = repository.createMeasure(subjectId, preset("weight"), unit = "lb")

        // "Not sure" is as available here as anywhere else. A reading remembered
        // from last week is still a reading.
        val id = repository.recordMeasurement(
            measureId = measureId,
            number = 163.4,
            unit = "lb",
            occurred = Edtf.unknown(),
        )
        assertTrue(id.isNotBlank())
    }

    @Test
    fun everyPresetCarriesItsRiskAndNoneOfItReachesThePerson() = runBlocking {
        // `advice_risk` exists so the rendering layer can be careful about how
        // it *displays* a value. It must never become a warning, a caution, or
        // a label the person sees, because that would be the app judging.
        val presets = TemplateCatalog.presets(context)
        assertTrue(presets.size >= 16)
        assertTrue(
            "a preset has no advice_risk",
            presets.all { it.adviceRisk in setOf("low", "medium", "high") },
        )

        val strings = Strings.load(context, Locale.ENGLISH)
        val onScreen = listOf(
            "measurement.pick.title", "measurement.pick.lead", "measurement.value.number",
            "measurement.value.number.hint", "measurement.value.text",
            "measurement.value.text.hint", "measurement.unit", "measurement.note",
            "measurement.note.hint", "measurement.record_only",
        ).map { strings[it].lowercase() }

        val banned = listOf(
            "normal", "range", "target", "high", "low", "healthy", "should",
            "too much", "too little", "warning", "abnormal", "concerning",
        )
        val found = onScreen.flatMap { copy -> banned.filter { it in copy }.map { "$it in: $copy" } }
        assertTrue("judgment language on the measurement screen: $found", found.isEmpty())
    }

    @Test
    fun theScreenSaysPlainlyThatItDoesNotInterpret() = runBlocking {
        // The honest limit, stated where the person is rather than buried in a
        // settings page. DESIGN.md section 7.
        val strings = Strings.load(context, Locale.ENGLISH)
        val line = strings["measurement.record_only"].lowercase()
        assertTrue(
            "the record only line does not say the app will not interpret: $line",
            "does not tell you what it means" in line,
        )
    }

    @Test
    fun oneNotebookNeverCountsAnother() = runBlocking {
        // The defect #58 was, asserted directly. Two subjects, one measure
        // each, and neither may see the other's. Invisible with one notebook
        // and silently wrong with two, which is the worse kind of wrong in a
        // care record because nobody thinks to check it.
        val repository = Repository.open(context)
        val mine = repository.createSubject(displayName = "Mine")
        val theirs = repository.createSubject(displayName = "Theirs")

        repository.createMeasure(mine, preset("weight"), unit = "lb")

        assertEquals(1, repository.count(Repository.Section.PROGRESS, mine))
        assertEquals(0, repository.count(Repository.Section.PROGRESS, theirs))

        repository.createMeasure(theirs, preset("sleep"), unit = "hours")
        assertEquals(1, repository.count(Repository.Section.PROGRESS, mine))
        assertEquals(1, repository.count(Repository.Section.PROGRESS, theirs))
    }

    @Test
    fun aPresetAlreadyTrackedIsNotOfferedTwice() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Measure subject")
        repository.createMeasure(subjectId, preset("weight"), unit = "lb")

        val tracked = repository.measures(subjectId).mapNotNull { it.presetId }.toSet()
        val offered = TemplateCatalog.presets(context).filter { it.id !in tracked }

        assertFalse("weight was offered again", offered.any { it.id == "weight" })
        assertTrue("nothing else was offered", offered.size >= 15)
    }
}
