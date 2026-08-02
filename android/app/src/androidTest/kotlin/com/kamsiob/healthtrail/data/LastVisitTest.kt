package com.kamsiob.healthtrail.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * When the person was last here.
 *
 * **The rule under test is that the mark moves once per run of the app**, not
 * once per object and not once per composition. It was per composition, and a
 * composition is rebuilt whenever the activity is, so a theme change or a
 * rotation advanced the mark into the middle of the visit and the digest went
 * blank. That was found on the phone: a freshly filled notebook reported
 * nothing new at all.
 */
@RunWith(AndroidJUnit4::class)
class LastVisitTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() = LastVisit(context).forget()

    @After
    fun tearDown() = LastVisit(context).forget()

    @Test
    fun aFirstRunHasNoPreviousVisit() {
        assertNull(LastVisit(context).openAndAdvance(now = 1_000L))
    }

    @Test
    fun theMarkDoesNotMoveWhenTheScreenIsRebuiltDuringTheSameRun() {
        val visit = LastVisit(context)
        visit.openAndAdvance(now = 1_000L)

        // Every one of these stands for the activity being recreated: a theme
        // change, a rotation, a font scale change. A fresh instance each time,
        // because that is what composition does.
        repeat(3) {
            assertNull(
                "recreating the screen moved the visit mark",
                LastVisit(context).openAndAdvance(now = 5_000L + it),
            )
        }
    }

    @Test
    fun thePreviousRunIsWhatTheNextRunReportsAgainst() {
        LastVisit(context).openAndAdvance(now = 1_000L)

        // A new process, which is the only thing that ends a visit.
        LastVisit.endVisitForTest()

        assertEquals(
            1_000L,
            LastVisit(context).openAndAdvance(now = 9_000L),
        )
    }

    @Test
    fun theMarkSurvivesTheProcessBeingKilledImmediatelyAfterAVisitOpens() {
        // Written with commit rather than apply, so a run that opens and is
        // killed a moment later does not report the same span twice.
        LastVisit(context).openAndAdvance(now = 4_242L)
        LastVisit.endVisitForTest()

        assertEquals(4_242L, LastVisit(context).openAndAdvance(now = 9_999L))
    }
}
