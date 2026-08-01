package com.kamsiob.healthtrail.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.data.Repository
import com.kamsiob.healthtrail.data.TemplateCatalog
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Unfiled tray, through the repository.
 *
 * The capture form already tells the person their entry is going here, so the
 * promises worth testing are the ones that would break that: an entry reaches
 * the tray, it leaves the tray when filed, and **filing links and clears in one
 * transaction** so neither half can happen without the other.
 *
 * Runs on the phone rather than on the JVM because it goes through SQLCipher
 * and the real change log triggers, which is where a half applied write would
 * actually show up.
 */
@RunWith(AndroidJUnit4::class)
class UnfiledTrayTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private suspend fun subjectWithThread(): Triple<Repository, String, Repository.CareThread> {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tray subject")
        // A real situation from the catalog rather than a hand made thread, so
        // the fixture exercises the same path setup does.
        val situation = TemplateCatalog.situations(context).all.first { it.threads.isNotEmpty() }
        repository.applySituation(
            subjectId = subjectId,
            templateId = situation.id,
            threads = situation.threads.map { it.id to it.label },
        )
        return Triple(repository, subjectId, repository.threads(subjectId).first())
    }

    @Test
    fun anUnfiledEntryReachesTheTray() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tray subject")

        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Called the nursing station",
            isUnfiled = true,
        )

        val waiting = repository.unfiled(subjectId)
        assertEquals(1, waiting.size)
        assertEquals(id, waiting.first().id)
        assertEquals(1, repository.unfiledCount(subjectId))
    }

    @Test
    fun anEntryWithAHomeDoesNotReachTheTray() = runBlocking {
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tray subject")

        repository.createEntry(subjectId = subjectId, kind = "call", isUnfiled = false)

        assertTrue(repository.unfiled(subjectId).isEmpty())
        assertEquals(0, repository.unfiledCount(subjectId))
    }

    @Test
    fun filingLinksTheThreadAndClearsTheTrayTogether() = runBlocking {
        val (repository, subjectId, thread) = subjectWithThread()
        val id = repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            title = "Called about her meals",
            isUnfiled = true,
        )

        repository.fileEntry(id, thread.id)

        // Left the tray.
        assertTrue("the entry stayed in the tray", repository.unfiled(subjectId).isEmpty())
        // And is linked. Either half without the other is worse than neither:
        // linking alone leaves it filed and still waiting, clearing alone loses
        // the answer the person just gave.
        assertTrue("the entry was cleared but not linked", repository.threadsForEntry(id).contains(thread.id))
    }

    @Test
    fun noneOfTheseClearsTheEntryWithoutInventingAThread() = runBlocking {
        val (repository, subjectId, thread) = subjectWithThread()
        val id = repository.createEntry(subjectId = subjectId, kind = "call", isUnfiled = true)

        repository.fileEntry(id, null)

        assertTrue(repository.unfiled(subjectId).isEmpty())
        assertTrue(
            "the app invented a thread the person did not choose",
            !repository.threadsForEntry(id).contains(thread.id),
        )
    }

    @Test
    fun theTrayCarriesTheDateAsTheStringRatherThanAsATimestamp() = runBlocking {
        // The tray is the likeliest place in the app to hold an unknown date,
        // since someone who could not say where something belonged often could
        // not say when either. It must arrive as the string so the screen can
        // render it honestly.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tray subject")
        repository.createEntry(
            subjectId = subjectId,
            kind = "call",
            occurred = Edtf.unknown(),
            isUnfiled = true,
        )

        val waiting = repository.unfiled(subjectId).single()
        assertEquals(Edtf.UNKNOWN, waiting.occurredEdtf)
        assertNull(
            "an unknown date resolved to a real instant",
            Edtf.resolve(Edtf.parse(waiting.occurredEdtf!!)!!, java.time.ZoneId.systemDefault())
                .start,
        )
    }

    @Test
    fun theTrayIsOrderedByWhenThingsWereWrittenDown() = runBlocking {
        // Not by when they happened, because a good share of these have no
        // date at all and therefore no position on a timeline.
        val repository = Repository.open(context)
        val subjectId = repository.createSubject(displayName = "Tray subject")

        val first = repository.createEntry(
            subjectId = subjectId, kind = "call", title = "First", isUnfiled = true,
        )
        Thread.sleep(2)
        val second = repository.createEntry(
            subjectId = subjectId, kind = "call", title = "Second", isUnfiled = true,
        )

        assertEquals(listOf(first, second), repository.unfiled(subjectId).map { it.id })
    }
}
