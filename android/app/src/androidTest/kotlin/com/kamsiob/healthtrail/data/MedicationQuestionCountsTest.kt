package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How many questions are still waiting on each medication.
 *
 * **The medications list was the one place this link did not go both ways**,
 * #352 and grid screen 12. The capture form writes `question.medication_id`,
 * the medication's own screen lists them, and the list row said nothing, so
 * finding out that anything was waiting meant opening every medication in turn.
 *
 * **A medication with nothing waiting is absent from the map** rather than
 * present with a zero, so no caller can render "0 questions" by accident.
 */
@RunWith(AndroidJUnit4::class)
class MedicationQuestionCountsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    @Test
    fun onlyTheMedicationsWithSomethingWaitingAppear() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Waiting questions")

        val asked = repository.createMedication(subjectId = subject, name = "Lisinopril")
        val quiet = repository.createMedication(subjectId = subject, name = "Vitamin D")

        repository.createQuestionWithEntry(
            subjectId = subject,
            text = "Is the morning dose still ten milligrams?",
            roleLabel = null,
            occurred = Edtf.parse("2026-08-12")!!,
            threadId = null,
            isUnfiled = false,
            medicationId = asked,
        )
        repository.createQuestionWithEntry(
            subjectId = subject,
            text = "Should this be taken with food?",
            roleLabel = null,
            occurred = Edtf.parse("2026-08-12")!!,
            threadId = null,
            isUnfiled = false,
            medicationId = asked,
        )

        val counts = repository.openQuestionCountsByMedication(subject)
        assertEquals(2, counts[asked])
        // Absent rather than zero.
        assertFalse(counts.containsKey(quiet))
    }
}
