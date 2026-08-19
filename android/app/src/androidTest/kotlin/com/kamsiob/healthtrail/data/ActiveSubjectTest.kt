package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exactly one person is the active one, always. #423.
 *
 * **Nothing exercised `addSubject` or `makeSubjectActive` before this**, and
 * their failure mode is the worst one the app has: every subject scoped screen
 * reads the active subject, so zero active rows means the notebook opens empty
 * and the person believes their record is gone.
 *
 * `makeSubjectActive` carried a doc comment saying "The clear and the set are
 * one transaction for that reason" above two bare writes that were not one. The
 * comment described the invariant and the code did not hold it, which is the
 * shape `docs/TRAPS.md` section 8 opens with.
 *
 * **`schema.sql` has no unique index on `is_active`**, so nothing underneath
 * catches this either. These tests are the only thing standing on it.
 */
@RunWith(AndroidJUnit4::class)
class ActiveSubjectTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: Repository

    @Before
    fun setUp() {
        Repository.closeForTest()
        repository = runBlocking {
            val open = Repository.open(context)
            // **The class brings its own person.** `connectedAndroidTest`
            // uninstalls the app, so a fresh database has no subject at all,
            // and an earlier class in the same run can replace the notebook
            // wholesale through a restore. Depending on fixture data made these
            // pass alone and fail in a suite, which is the least useful kind of
            // test there is.
            if (open.activeSubject() == null) {
                open.addSubject(displayName = "Somebody to start from")
            }
            open
        }
    }

    /** How many rows claim to be the active one. Never read through a helper. */
    private fun activeCount(): Int = runBlocking {
        HealthTrailDatabase.open(context).database.rawQuery(
            // allow-base-table: counting the invariant itself, which a view that
            // filters would hide.
            "SELECT COUNT(*) FROM subject WHERE is_active = 1 AND deleted_at IS NULL",
            null,
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    @Test
    fun addingASecondPersonLeavesExactlyOneActive() = runBlocking {
        assertNotNull("setUp must leave somebody active", repository.activeSubject())

        val added = repository.addSubject(displayName = "Second person for #423")

        assertEquals("adding a person must leave exactly one active row", 1, activeCount())
        assertEquals(
            "and the person just added is the one being shown",
            added,
            repository.activeSubject()?.id,
        )
    }

    /**
     * Switching back and forth never leaves zero or two.
     *
     * **`createSubject` sets `is_active = 1` explicitly**, so `addSubject`
     * creates a second active row and then clears the others. Between those two
     * statements there were two active rows, and `activeSubject()` resolves that
     * silently with `ORDER BY created_at LIMIT 1`, which picks the older person.
     * So the visible symptom of a half applied switch is somebody else's
     * notebook, not an error.
     */
    @Test
    fun switchingBetweenPeopleAlwaysLeavesExactlyOneActive() = runBlocking {
        val first = repository.activeSubject()!!.id
        val second = repository.addSubject(displayName = "Third person for #423")

        repeat(3) {
            repository.makeSubjectActive(first)
            assertEquals(1, activeCount())
            assertEquals(first, repository.activeSubject()?.id)

            repository.makeSubjectActive(second)
            assertEquals(1, activeCount())
            assertEquals(second, repository.activeSubject()?.id)
        }
    }

    /**
     * Making the already active person active again is not a way to end up with
     * none.
     *
     * The clear runs first and matches the same row the set is about, so an
     * order that looked harmless is exactly the one that empties the notebook if
     * it is interrupted.
     */
    @Test
    fun reselectingTheSamePersonKeepsThemActive() = runBlocking {
        val current = repository.activeSubject()!!.id
        repository.makeSubjectActive(current)
        assertEquals(1, activeCount())
        assertEquals(current, repository.activeSubject()?.id)
    }
}
