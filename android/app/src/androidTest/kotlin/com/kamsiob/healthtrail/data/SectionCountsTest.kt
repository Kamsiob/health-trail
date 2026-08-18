package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every count on the notebook equals the list the section opens onto. #145.
 *
 * **The defect this exists for was real and shipped**: the care team's count
 * included archived people and the screen did not, so the notebook said nine
 * and the screen listed eight. It was fixed with `Section.hiddenWhen`, and what
 * was never fixed is that nothing would have caught it, on any of the thirteen
 * sections.
 *
 * **The invariant is one line and it is the whole issue**: for every section,
 * `count(section, subjectId)` is the number of rows that section's own reader
 * returns. A number on the front door that disagrees with the screen behind it
 * is the app being wrong about itself, which is worse than not counting.
 *
 * **Written through the repository rather than against a fixture**, which the
 * issue considered and could not have: nothing in the app archived a person
 * when it was filed, so the state that caused the defect was unreachable except
 * through the generator. `setPersonArchived` landed on 2026-08-13, so the case
 * that broke it can now be built the way a person builds it.
 *
 * **Three sections have no list reader of their own** and are counted here
 * against the same view the count reads. That is deliberately weaker and it is
 * said out loud rather than hidden: what those three prove is only that the
 * count matches the table, not that any screen agrees with it.
 */
@RunWith(AndroidJUnit4::class)
class SectionCountsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository
    private lateinit var subjectId: String

    @Before
    fun setUp() = runBlocking {
        repository = Repository.open(context)
        subjectId = repository.createSubject(displayName = "Counts", relationship = "mother")
    }

    @After
    fun tearDown() = Repository.closeForTest()

    private fun day(text: String) = Edtf.day(LocalDate.parse(text))

    /** A notebook with something in every section, built the way a person does. */
    private fun fill() = runBlocking {
        repository.createPerson(subjectId = subjectId, displayName = "Angela Reyes")
        repository.createPerson(subjectId = subjectId, displayName = "Marcus Bell")
        repository.createMedication(subjectId = subjectId, name = "Lisinopril")
        repository.createOwnMeasure(
            subjectId = subjectId,
            name = "How she seems",
            unit = null,
            isText = true,
        )
        repository.createAppointment(
            subjectId = subjectId,
            title = "Care plan meeting",
            scheduled = day("2026-03-01"),
        )
        repository.createChapter(subjectId = subjectId, name = "Maplewood")
        repository.createThread(subjectId = subjectId, label = "The lease")
        repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Rang the ward",
            occurred = day("2026-02-01"),
        )
        repository.createDocument(
            subjectId = subjectId,
            title = "The grievance letter",
            received = day("2026-02-03"),
        )
        repository.createBill(
            subjectId = subjectId,
            description = "Ambulance transfer",
            amountMinor = 316_877,
            state = "needs_attention",
            received = day("2026-02-04"),
        )
        repository.createStandingInstruction(
            subjectId = subjectId,
            templateId = null,
            name = "Call me about any fall",
            wording = "Please call me right away.",
            tag = "federal",
            given = day("2026-01-05"),
        )
        repository.createQuestionWithEntry(
            subjectId = subjectId,
            text = "Why was the dressing not changed",
            roleLabel = null,
            occurred = day("2026-02-02"),
            threadId = null,
            isUnfiled = false,
        )
        repository.createProject(subjectId = subjectId, name = "The coverage appeal")
    }

    /** What the section's own screen is handed, for the sections that have a reader. */
    private fun listed(section: Repository.Section): Int? = runBlocking {
        when (section) {
            Repository.Section.CARE_TEAM -> repository.people(subjectId).size
            Repository.Section.MEDICATIONS -> repository.medications(subjectId).size
            Repository.Section.APPOINTMENTS -> repository.appointments(subjectId).size
            Repository.Section.CHAPTERS -> repository.chapters(subjectId).size
            Repository.Section.THREADS -> repository.threadsWithCounts(subjectId).size
            Repository.Section.TRAIL -> repository.trail(subjectId).size
            // **Notes are entries of one kind**, D207, so what the screen is
            // handed is the trail filtered the way the section's own predicate
            // filters it.
            Repository.Section.NOTES ->
                repository.trail(subjectId).count { it.kind == "note" }
            Repository.Section.PROGRESS -> repository.measures(subjectId).size
            Repository.Section.DOCUMENTS -> repository.documents(subjectId).size
            Repository.Section.MONEY -> repository.bills(subjectId).size
            Repository.Section.STANDING_INSTRUCTIONS ->
                repository.standingInstructions(subjectId).size
            Repository.Section.ASK_NEXT_TIME -> repository.questions(subjectId).size
            Repository.Section.PROJECTS -> repository.projects(subjectId).size
            // The emergency card is one row read as a card rather than a list.
            Repository.Section.EMERGENCY_CARD -> null
        }
    }

    @Test
    fun everySectionCountsWhatItsOwnScreenLists() = runBlocking {
        fill()
        for (section in Repository.Section.entries) {
            val listed = listed(section) ?: continue
            assertEquals(
                "the notebook's count for $section disagrees with the list it opens onto, " +
                    "which is the app being wrong about itself",
                listed,
                repository.count(section, subjectId),
            )
        }
    }

    /**
     * **The defect this issue was filed for, as a test.** An archived person is
     * on neither the care team screen nor its count, and before
     * `Section.hiddenWhen` the count included them: the notebook said nine and
     * the screen listed eight, and nothing anywhere would have noticed.
     */
    @Test
    fun anArchivedPersonLeavesTheCountAndTheListTogether() = runBlocking {
        fill()
        val before = repository.count(Repository.Section.CARE_TEAM, subjectId)
        assertEquals(repository.people(subjectId).size, before)

        val retiring = repository.people(subjectId).first()
        repository.setPersonArchived(retiring.id, true)

        assertEquals(
            "the care team screen stopped showing them",
            before - 1,
            repository.people(subjectId).size,
        )
        assertEquals(
            "and so did the number on the notebook",
            before - 1,
            repository.count(Repository.Section.CARE_TEAM, subjectId),
        )
    }

    /** A removed row leaves both, because every read goes through a live view. */
    @Test
    fun aremovedRowLeavesTheCountAndTheListTogether() = runBlocking {
        fill()
        val before = repository.count(Repository.Section.MEDICATIONS, subjectId)
        val medication = repository.medications(subjectId).first()
        repository.delete(Repository.Section.MEDICATIONS, medication.id)

        assertEquals(before - 1, repository.medications(subjectId).size)
        assertEquals(before - 1, repository.count(Repository.Section.MEDICATIONS, subjectId))
    }

    /** An empty notebook counts zero everywhere rather than counting nothing. */
    @Test
    fun anEmptyNotebookCountsZeroInEverySection() = runBlocking {
        for (section in Repository.Section.entries) {
            assertEquals(
                "$section on an empty notebook",
                0,
                repository.count(section, subjectId),
            )
        }
    }
}
