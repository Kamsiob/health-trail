package com.kamsiob.healthtrail.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether a medication is on the card somebody hands to a paramedic.
 *
 * **This is the one claim in the app where being wrong has a cost outside the
 * app.** A card that lists something she stopped taking in March is worse than
 * a card that omits it, and the shell has always been right about that: it
 * assembles the card from medications that say they belong on it and drops the
 * stopped ones.
 *
 * The medications list and the medication screen were each deciding the same
 * thing separately, and both were deciding it from the stored flag alone, so a
 * stopped medication sat in the list in alert orange claiming to be on a card it
 * had already fallen off. **Three copies of a safety rule is a safety rule that
 * will disagree with itself**, and this one already had.
 *
 * `Repository.Medication.showsOnEmergencyCard` is now the only place it is
 * decided, and this is what holds it there.
 */
class EmergencyCardClaimTest {

    private fun medication(onCard: Boolean, stopped: String?) = Repository.Medication(
        id = "m1",
        name = "Metformin",
        doseText = "500 mg, twice a day",
        purposeText = "Diabetes",
        notes = null,
        onEmergencyCard = onCard,
        stoppedEdtf = stopped,
    )

    @Test
    fun acurrentMedicationThatWasPutOnTheCardIsOnIt() {
        assertTrue(medication(onCard = true, stopped = null).showsOnEmergencyCard)
    }

    @Test
    fun astoppedMedicationIsNotOnTheCardEvenThoughItsFlagIsStillSet() {
        val stopped = medication(onCard = true, stopped = "2026-03-14")
        assertTrue("the flag itself is kept, so the card can be rejoined", stopped.onEmergencyCard)
        assertTrue(stopped.isStopped)
        assertFalse("a stopped medication claimed to be on the card", stopped.showsOnEmergencyCard)
    }

    @Test
    fun amedicationNobodyPutOnTheCardIsNotOnIt() {
        assertFalse(medication(onCard = false, stopped = null).showsOnEmergencyCard)
        assertFalse(medication(onCard = false, stopped = "2026-03-14").showsOnEmergencyCard)
    }

    @Test
    fun ablankStoppedDateIsNotAstopped() {
        // The column is nullable and the app writes null, but an imported
        // notebook could carry an empty string, and an empty string must not
        // read as "stopped" and quietly pull a current medication off the card.
        assertTrue(medication(onCard = true, stopped = "").showsOnEmergencyCard)
        assertTrue(medication(onCard = true, stopped = "   ").showsOnEmergencyCard)
    }

    @Test
    fun anImpreciseStopDateStillStopsIt() {
        // Rule 17: dates are EDTF and a person may only know the month. Somebody
        // who knows she came off it sometime in March still knows she came off
        // it, and the card must not keep listing it because the date is rough.
        assertFalse(medication(onCard = true, stopped = "2026-03").showsOnEmergencyCard)
        assertFalse(medication(onCard = true, stopped = "XXXX-XX-XX").showsOnEmergencyCard)
    }
}
