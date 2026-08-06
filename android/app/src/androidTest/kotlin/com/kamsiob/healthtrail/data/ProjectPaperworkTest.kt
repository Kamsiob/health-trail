package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The papers of a project, and the document filed in one. Screen 13, #286.
 *
 * **The place and the paper are different things**, and every assertion here is
 * about keeping them different: an empty place is still a row, a filled one
 * keeps the name the person gave the slot, and the document knows which slot it
 * is in so the link goes both ways.
 */
@RunWith(AndroidJUnit4::class)
class ProjectPaperworkTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var repository: Repository
    private lateinit var subjectId: String
    private lateinit var projectId: String

    @Before
    fun setUp() = runBlocking {
        Repository.closeForTest()
        repository = Repository.open(context)
        subjectId = repository.createSubject("Margaret")
        projectId = repository.startProject(
            subjectId = subjectId,
            templateId = "medicaid_ltc",
            name = "Medicaid application",
            steps = emptyList(),
            lead = "standing",
            stages = listOf("Applied", "In review"),
            dateKinds = emptyList(),
            papers = listOf("Proof of income", "The award letter"),
        )
    }

    @After
    fun tearDown() = Repository.closeForTest()

    /**
     * An empty place is a row, not an absence.
     *
     * 20.4: a placeholder is a place and reads "not yet". A left join that
     * dropped it would leave the screen unable to say so, and the screen would
     * look entirely correct with the places simply missing.
     */
    @Test
    fun anEmptyPlaceIsStillARow() = runBlocking {
        val cards = repository.projectPaperCards(projectId)
        assertEquals(2, cards.size)
        assertTrue("an empty place claims to be filled", cards.none { it.isFilled })
        assertNull(cards.first().title)
        assertEquals("Proof of income", cards.first().paper.name)
    }

    /**
     * A filled place keeps the name the person gave the slot.
     *
     * The tile led with the document's title at first, which hid the place:
     * somebody looking for "Proof of income" found a tile called "Discharge
     * summary" with no way to tell it was the same place.
     */
    @Test
    fun aFilledPlaceKeepsItsOwnNameAndNamesWhatIsInIt() = runBlocking {
        val documentId = repository.createDocument(
            subjectId = subjectId,
            title = "Discharge summary",
            received = Edtf.day(LocalDate.parse("2026-05-12")),
        )
        val place = repository.projectPapers(projectId).first { it.name == "Proof of income" }
        repository.fillProjectPaper(place.id, documentId, direction = "sent")

        val card = repository.projectPaperCards(projectId).first { it.paper.id == place.id }
        assertTrue(card.isFilled)
        assertEquals("Proof of income", card.paper.name)
        assertEquals("Discharge summary", card.title)
    }

    /** The document knows which project's paper it is, so the link goes both ways. */
    @Test
    fun theDocumentNamesTheProjectAndThePlace() = runBlocking {
        val documentId = repository.createDocument(
            subjectId = subjectId,
            title = "Discharge summary",
            received = Edtf.day(LocalDate.parse("2026-05-12")),
        )
        val place = repository.projectPapers(projectId).first { it.name == "Proof of income" }
        repository.fillProjectPaper(place.id, documentId, direction = "sent")

        val filings = repository.filingsForDocument(documentId)
        assertEquals(1, filings.size)
        assertEquals(projectId, filings.first().projectId)
        assertEquals("Medicaid application", filings.first().projectName)
        assertEquals("Proof of income", filings.first().paperName)
    }

    /** A document filed nowhere says so, rather than claiming a project. */
    @Test
    fun anUnfiledDocumentNamesNoProject() = runBlocking {
        val documentId = repository.createDocument(
            subjectId = subjectId,
            title = "A letter",
            received = Edtf.day(LocalDate.parse("2026-05-12")),
        )
        assertEquals(emptyList<Repository.DocumentFiling>(), repository.filingsForDocument(documentId))
    }

    /**
     * Emptying the place leaves the document alone.
     *
     * The thing at risk is a photograph of a letter somebody may not be able to
     * get again, so this is worth holding rather than trusting.
     */
    @Test
    fun emptyingThePlaceKeepsTheDocument() = runBlocking {
        val documentId = repository.createDocument(
            subjectId = subjectId,
            title = "A letter",
            received = Edtf.day(LocalDate.parse("2026-05-12")),
        )
        val place = repository.projectPapers(projectId).first()
        repository.fillProjectPaper(place.id, documentId, direction = "sent")
        repository.emptyProjectPaper(place.id)

        assertFalse(repository.projectPaperCards(projectId).first { it.paper.id == place.id }.isFilled)
        assertTrue(
            "emptying the place took the document with it",
            repository.documents(subjectId).any { it.id == documentId },
        )
    }
}
