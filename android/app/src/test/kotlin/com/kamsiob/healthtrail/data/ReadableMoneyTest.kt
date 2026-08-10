package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The archive's own money rule. `contract/readable-money.json`, #331.
 *
 * **These run without a phone, and that is the point of the whole change.** The
 * defect was that `java.text.NumberFormat` answered differently on Android and
 * on a JVM, so the only honest test of the old behavior was "whatever this
 * machine says". Formatting the amount here makes it a function with a table,
 * and a function with a table can be pinned.
 *
 * **Every case below is a rule in `contract/readable-money.json`**, so this file
 * and that file fail together rather than drifting.
 */
class ReadableMoneyTest {

    /** The exact example the issue was opened with. */
    @Test
    fun `the case the defect was found on`() {
        assertEquals("6,790.40 USD", ReadableMoney.format(679_040, "USD"))
    }

    /**
     * Latin digits, in every language, always.
     *
     * **This is the assertion the whole change exists for**, and it cannot be
     * written against the old code at all: there was no locale to pass, because
     * the locale was whatever the platform had. Here it is simply true.
     */
    @Test
    fun `the digits are Latin and there is no locale to change that`() {
        val rendered = ReadableMoney.format(679_040, "USD")
        assertTrue(
            "a non-Latin digit reached the archive: $rendered",
            rendered.none { it.isDigit() && it !in '0'..'9' },
        )
    }

    /**
     * Trailing zeros survive, because they are a statement about the amount.
     *
     * 5.00 and 5.0 and 5 are three different things to say about a bill, and a
     * formatter that trims is a formatter that changes what the record says.
     */
    @Test
    fun `the fraction is always exactly as many digits as the currency has`() {
        assertEquals("5.00 USD", ReadableMoney.format(500, "USD"))
        assertEquals("0.00 USD", ReadableMoney.format(0, "USD"))
        assertEquals("0.05 USD", ReadableMoney.format(5, "USD"))
        assertEquals("0.50 USD", ReadableMoney.format(50, "USD"))
    }

    /**
     * A currency with no minor unit renders no decimal point at all.
     *
     * **Not `500.00 JPY`**, which would be inventing two decimal places nobody
     * gave, on a currency that does not have them.
     */
    @Test
    fun `a currency with no minor unit has no fractional part`() {
        assertEquals("500 JPY", ReadableMoney.format(500, "JPY"))
        assertEquals("1,234 JPY", ReadableMoney.format(1_234, "JPY"))
        assertEquals("0 KRW", ReadableMoney.format(0, "KRW"))
    }

    /** Three minor digits, which is a real and easily forgotten shape. */
    @Test
    fun `a three digit currency keeps all three`() {
        assertEquals("1,234.567 BHD", ReadableMoney.format(1_234_567, "BHD"))
        assertEquals("0.001 KWD", ReadableMoney.format(1, "KWD"))
    }

    /** Four, which exists, and is the one nobody remembers. */
    @Test
    fun `a four digit currency keeps all four`() {
        assertEquals("1.2345 CLF", ReadableMoney.format(12_345, "CLF"))
    }

    @Test
    fun `groups are threes from the right`() {
        assertEquals("1.00 USD", ReadableMoney.format(100, "USD"))
        assertEquals("10.00 USD", ReadableMoney.format(1_000, "USD"))
        assertEquals("100.00 USD", ReadableMoney.format(10_000, "USD"))
        assertEquals("1,000.00 USD", ReadableMoney.format(100_000, "USD"))
        assertEquals("12,345,678.90 USD", ReadableMoney.format(1_234_567_890, "USD"))
    }

    /** The sign leads everything, including the grouping. */
    @Test
    fun `a negative amount carries one leading minus`() {
        assertEquals("-25.50 USD", ReadableMoney.format(-2_550, "USD"))
        assertEquals("-1,234,567.89 USD", ReadableMoney.format(-123_456_789, "USD"))
        assertEquals("-7 JPY", ReadableMoney.format(-7, "JPY"))
    }

    /**
     * The extreme a `Long` can hold, in both directions.
     *
     * **`Long.MIN_VALUE` has no positive counterpart**, so negating it gives
     * back itself and a formatter that negates first prints a positive number
     * with a minus in front of it. The magnitude is taken through `BigInteger`
     * for exactly this, and this is the case that proves it.
     */
    @Test
    fun `the ends of the range render honestly`() {
        assertEquals("92,233,720,368,547,758.07 USD", ReadableMoney.format(Long.MAX_VALUE, "USD"))
        assertEquals("-92,233,720,368,547,758.08 USD", ReadableMoney.format(Long.MIN_VALUE, "USD"))
    }

    /**
     * A code nobody here has heard of renders with two, and says which code.
     *
     * **Rather than refusing.** An amount the person recorded is part of the
     * record, and printing it with the wrong number of decimals is recoverable
     * while dropping it is not. The code travels beside it either way, so a
     * reader can tell.
     */
    @Test
    fun `an unknown code renders with two digits and keeps its code`() {
        assertEquals("12.34 ZZZ", ReadableMoney.format(1_234, "ZZZ"))
    }

    /**
     * The code, never a symbol.
     *
     * `$` is at least six different currencies and a care record read by a
     * sibling abroad cannot carry an amount that means six things. Same fix as
     * spelling out the month.
     */
    @Test
    fun `the ISO code travels rather than a symbol`() {
        for (code in listOf("USD", "CAD", "AUD", "EUR", "GBP")) {
            val rendered = ReadableMoney.format(1_000, code)
            assertTrue("$rendered does not carry its code", rendered.endsWith(" $code"))
            assertTrue("$rendered carries a currency symbol", rendered.none { it in "$€£¥" })
        }
    }

    /**
     * The table is the contract's, not a second copy of it.
     *
     * A thin assertion on purpose: what it proves is that the generated table
     * arrived at all, so a build that silently produced an empty one fails here
     * rather than by rendering every currency with two digits.
     */
    @Test
    fun `the exponent table came from the contract`() {
        assertTrue("the generated exponent table is empty", ReadableCurrency.exponents.isNotEmpty())
        assertEquals(0, ReadableCurrency.exponents["JPY"])
        assertEquals(3, ReadableCurrency.exponents["BHD"])
        assertEquals(4, ReadableCurrency.exponents["CLF"])
        assertEquals(
            "USD is listed, but it is the default and listing it invites a second answer",
            null,
            ReadableCurrency.exponents["USD"],
        )
    }
}
