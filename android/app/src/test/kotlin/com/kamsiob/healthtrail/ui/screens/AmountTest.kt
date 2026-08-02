package com.kamsiob.healthtrail.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Turning what somebody typed into minor units.
 *
 * **This is money in a record that may be read out in a dispute**, which is why
 * the schema stores minor units as an integer and why this never goes near a
 * double. A rounding boundary here moves somebody's money.
 *
 * It is deliberately forgiving on the way in and exact once stored. Somebody
 * copying an amount off a statement types it however the statement prints it.
 */
class AmountTest {

    @Test
    fun `a plain amount becomes cents`() {
        assertEquals(128450L, parseAmountToMinor("1284.50"))
        assertEquals(31000L, parseAmountToMinor("310"))
        assertEquals(5L, parseAmountToMinor("0.05"))
    }

    @Test
    fun `the shapes a statement actually prints`() {
        assertEquals(128450L, parseAmountToMinor("$1,284.50"))
        assertEquals(128450L, parseAmountToMinor("1,284.50"))
        assertEquals(128400L, parseAmountToMinor("1,284"))
        assertEquals(128450L, parseAmountToMinor(" 1284.50 "))
    }

    /**
     * **Nothing usable is null, never zero.** Zero is a claim that the bill
     * says nothing is owed, and somebody who typed nothing has not made it.
     */
    @Test
    fun `nothing usable is null rather than zero`() {
        assertNull(parseAmountToMinor(""))
        assertNull(parseAmountToMinor("   "))
        assertNull(parseAmountToMinor("pending"))
        assertNull(parseAmountToMinor("this is not a bill"))
        assertNull(parseAmountToMinor("$"))
    }

    /**
     * **Truncated rather than rounded up.** The app never records more money
     * than was written down. Half a cent is not somebody's problem to pay.
     */
    @Test
    fun `more precision than the currency has is truncated`() {
        assertEquals(128459L, parseAmountToMinor("1284.599"))
        assertEquals(128450L, parseAmountToMinor("1284.5049"))
    }

    /**
     * A large bill is exactly as ordinary to this code as a small one, and a
     * facility bill runs to five figures routinely.
     */
    @Test
    fun `a large amount survives without losing precision`() {
        assertEquals(1234567890L, parseAmountToMinor("12,345,678.90"))
    }

    /**
     * Zero typed on purpose is a real answer and must not be confused with
     * nothing typed. A bill that says nothing is owed exists, and it is worth
     * keeping precisely because somebody may later say otherwise.
     */
    @Test
    fun `zero typed on purpose is kept`() {
        assertEquals(0L, parseAmountToMinor("0"))
        assertEquals(0L, parseAmountToMinor("0.00"))
    }

    /**
     * A currency with no minor unit at all. Not shipped today, since the app is
     * United States only, but the function takes the digits as a parameter and
     * a wrong answer here would be silent.
     */
    @Test
    fun `a currency with no fraction digits scales by one`() {
        assertEquals(1284L, parseAmountToMinor("1284", fractionDigits = 0))
    }
}
