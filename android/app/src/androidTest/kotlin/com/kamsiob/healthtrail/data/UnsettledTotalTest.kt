package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.healthtrail.time.Edtf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the notebook's Money row says instead of counting.
 *
 * **The grid draws "$15,072.98 not settled"**, and the app said "6 items" for
 * every section including this one. "6 bills" is true and is still not the
 * number somebody opens Money to find. #347.
 *
 * **The rule this holds is that two screens agree.** The Money screen totals
 * what is open, meaning everything except paid and closed, and skips bills with
 * no amount. If the notebook decided separately what counts as settled, the two
 * totals would drift and the person would have no way to tell which was right.
 */
@RunWith(AndroidJUnit4::class)
class UnsettledTotalTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Repository.closeForTest()
    }

    private suspend fun bill(
        repository: Repository,
        subject: String,
        state: String,
        minor: Long?,
    ) = repository.createBill(
        subjectId = subject,
        description = "$state bill",
        amountMinor = minor,
        state = state,
        received = Edtf.parse("2026-03-01")!!,
    )

    @Test
    fun theTotalIsWhatIsOpenAndSkipsTheOnesWithNoAmount() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "Money totals")

        bill(repository, subject, "needs_attention", 10_000)
        bill(repository, subject, "disputed", 2_550)
        bill(repository, subject, "waiting_on_insurance", 1_000)
        // Settled, so out of the total.
        bill(repository, subject, "paid", 99_999)
        bill(repository, subject, "closed", 88_888)
        // **A bill with no amount is a real bill and is not a zero.** It stays
        // in the count and out of the sum.
        bill(repository, subject, "needs_attention", null)

        val total = repository.unsettledTotal(subject)
        assertEquals("the open bills with amounts", 13_550L, total!!.first)
        assertEquals("USD", total.second)

        // The count still counts every bill, including the settled ones, because
        // the Money screen lists them all.
        assertEquals(6, repository.count(Repository.Section.MONEY, subject))
    }

    /**
     * **A settled notebook says nothing rather than zero**, which is the rule
     * the Money screen already applies to its own band: a zero total is a
     * number with nothing behind it, and the row counts bills instead.
     */
    @Test
    fun nothingUnsettledMeansNoAmountAtAll() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "All settled")

        bill(repository, subject, "paid", 4_200)
        bill(repository, subject, "closed", 1_100)

        assertNull("a settled notebook produced a total", repository.unsettledTotal(subject))
        assertEquals(2, repository.count(Repository.Section.MONEY, subject))
    }

    /**
     * **An open bill with no amount is not a total.** It is the case that looks
     * like zero and is not: there is something unsettled and nothing to add up.
     */
    @Test
    fun anOpenBillWithNoAmountProducesNoTotal() = runBlocking {
        val repository = Repository.open(context)
        val subject = repository.createSubject(displayName = "No amount yet")

        bill(repository, subject, "needs_attention", null)

        assertNull(repository.unsettledTotal(subject))
        assertEquals(1, repository.count(Repository.Section.MONEY, subject))
    }
}
