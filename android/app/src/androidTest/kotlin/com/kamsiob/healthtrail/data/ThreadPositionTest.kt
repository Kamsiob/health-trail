package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Where an entry sits inside its thread, which the entry screen says out loud.
 *
 * **A thread is a sequence somebody is trying to follow**, so "step 3 of 4" is
 * the fact that matters on the entry's own screen, and the word "thread" on its
 * own only repeats the row above it. The grid draws it as "This is step 4 of
 * its thread". #348.
 *
 * **The step counts from the oldest and the thread screen lists the newest
 * first.** Those are deliberately opposite: somebody scanning a thread wants
 * the latest at the top, and a step number only means anything counting forward
 * from the beginning. **This test exists mostly to hold that inversion**, since
 * it is the thing a later change would quietly get backwards.
 */
@RunWith(AndroidJUnit4::class)
class ThreadPositionTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }


    /**
     * **A thread from a situation**, which is how most of them arrive, so this
     * makes one the same way the app does. **A person can also start one from
     * nothing** since #349, and `CreateThreadTest` covers that path: this one
     * is about where an entry sits in a thread and either origin would do.
     */
    private suspend fun threadFor(subject: String, label: String): String {
        val repository = Repository.open(context)
        repository.applySituation(
            subjectId = subject,
            templateId = "nursing_home",
            threads = listOf(label.lowercase().replace(" ", "_") to label),
            startingHand = emptyList(),
        )
        return repository.threads(subject).first { it.label == label }.id
    }

    @Test
    fun theStepCountsFromTheOldestWhileTheThreadListsTheNewestFirst() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Thread position")
        val thread = threadFor(subject, "Discharge planning")

        val days = listOf("2026-01-10", "2026-02-10", "2026-03-10", "2026-04-10")
        val ids = days.map { day ->
            repository.createEntry(
                subjectId = subject,
                kind = "call",
                occurred = Edtf.parse(day)!!,
                body = "Written on $day",
            ).also { repository.linkEntryToThread(it, thread) }
        }

        // The thread's own screen: newest first.
        assertEquals(
            "the thread stopped listing the newest first",
            ids.reversed(),
            repository.entriesForThread(thread).map { it.id },
        )

        // The entry's own screen: counted from the oldest.
        ids.forEachIndexed { index, id ->
            val position = repository.entry(id)!!.threadPositions[thread]
            assertEquals("step for entry $index", index + 1, position!!.step)
            assertEquals("total for entry $index", days.size, position.total)
        }
    }

    /**
     * **An entry in two threads is a different step of each**, which is why the
     * position is carried per link rather than per entry.
     */
    @Test
    fun anEntryInTwoThreadsIsADifferentStepOfEach() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Two threads")
        val first = threadFor(subject, "Billing")
        val second = threadFor(subject, "Nursing")

        // Two earlier entries on the first thread only.
        listOf("2026-01-01", "2026-01-02").forEach { day ->
            repository.createEntry(
                subjectId = subject,
                kind = "call",
                occurred = Edtf.parse(day)!!,
                body = day,
            ).also { repository.linkEntryToThread(it, first) }
        }

        val shared = repository.createEntry(
            subjectId = subject,
            kind = "call",
            occurred = Edtf.parse("2026-01-03")!!,
            body = "On both",
        )
        repository.linkEntryToThread(shared, first)
        repository.linkEntryToThread(shared, second)

        val positions = repository.entry(shared)!!.threadPositions
        assertEquals("third on the thread it came late to", 3, positions[first]!!.step)
        assertEquals("first on the thread it starts", 1, positions[second]!!.step)
        assertEquals(3, positions[first]!!.total)
        assertEquals(1, positions[second]!!.total)
    }
}
