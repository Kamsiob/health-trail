package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Somebody can start a care thread the situation templates never heard of.
 *
 * **Applying a situation was the only way a thread had ever been created**, so
 * a person dealing with a landlord, a school, or an employer's leave department
 * had a real recurring situation with no spine of its own, and the fourteen
 * situations read as the only fourteen kinds of care there are. #349, D145.
 *
 * **The same argument `createProject` and `createOwnMeasure` already make**, in
 * the two sections of the repository nearest this one: a starting set is not
 * the world.
 *
 * **A thread started this way is a thread**, which is the whole point of these
 * tests: it takes entries, it counts them, it ends, and it tombstones exactly
 * like one a situation created.
 */
@RunWith(AndroidJUnit4::class)
class CreateThreadTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    /**
     * **A name is the only thing asked for**, per rule 13, and everything else
     * takes the schema's own default. `template_id` stays null, which is what
     * the schema already means by something the person set up themselves.
     */
    @Test
    fun aThreadCanBeStartedWithNothingButAName() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "From nothing")

        val id = repository.createThread(subjectId = subject, label = "  The lease  ")

        val threads = repository.threads(subject)
        assertEquals(1, threads.size)
        // Trimmed, because a name with a stray space is the same name and the
        // person is typing on a phone.
        assertEquals("The lease", threads.single().label)
        assertEquals(id, threads.single().id)

        // It started today and a day is exactly as much as anyone knows about
        // it, the same line `applySituation` writes for the threads it creates.
        assertEquals(
            Edtf.day(java.time.LocalDate.now()).canonical,
            repository.columnForTest("care_thread", id, "started_edtf"),
        )
        // **No template behind it**, which is what the schema's own comment
        // already means by a thread that did not come from one.
        assertNull(repository.columnForTest("care_thread", id, "template_id"))
    }

    /**
     * **It is a thread, not a second kind of thing.** An entry files under it,
     * the count screen counts it, and it appears everywhere a template thread
     * appears, because it is the same row in the same table.
     */
    @Test
    fun aThreadStartedFromNothingTakesEntriesLikeAnyOther() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Takes entries")

        val threadId = repository.createThread(subjectId = subject, label = "Her school")
        val entry = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2026-08-12")!!,
            body = "Called the district office about the missed days.",
        )
        repository.linkEntryToThread(entry, threadId)

        val counted = repository.threadsWithCounts(subject).single { it.thread.id == threadId }
        assertEquals(1, counted.entryCount)
        assertTrue(repository.entriesForThread(threadId).any { it.id == entry })
        assertTrue(repository.threadsForEntry(entry).contains(threadId))
    }

    /**
     * **A new thread joins the end and takes the next route color.** The color
     * is an index into the theme's routes rather than a stored color, so two
     * threads landing on the same index would be two identical spines on the
     * trail with no way to tell them apart.
     */
    @Test
    fun aNewThreadTakesTheNextPositionAndTheNextRoute() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Next in line")
        repository.applySituation(
            subjectId = subject,
            templateId = "nursing_home",
            threads = listOf("billing" to "Billing", "care_plan" to "The care plan"),
            startingHand = emptyList(),
        )

        val existing = repository.threads(subject)
        val id = repository.createThread(
            subjectId = subject,
            label = "The lease",
            sortIndex = existing.size,
        )

        val threads = repository.threads(subject)
        assertEquals(3, threads.size)
        // Last in the list the person reads, which is `sort_index` order.
        assertEquals(id, threads.last().id)
        assertEquals(2, threads.last().colorIndex)
        assertEquals(3, threads.map { it.colorIndex }.toSet().size)
    }

    /**
     * **Removal is a tombstone here too**, per rule 3 and the data contract. A
     * thread the person started and then thought better of leaves the screens
     * and stays in the record.
     */
    @Test
    fun aThreadStartedFromNothingIsRemovedByTombstone() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Thought better of it")

        val id = repository.createThread(subjectId = subject, label = "The wrong idea")
        repository.delete(Repository.Section.THREADS, id)

        assertTrue(repository.threads(subject).none { it.id == id })
        assertNotNull(repository.columnForTest("care_thread", id, "deleted_at"))
    }
}
