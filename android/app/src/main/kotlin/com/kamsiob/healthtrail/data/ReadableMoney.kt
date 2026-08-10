package com.kamsiob.healthtrail.data

/**
 * How an amount reads in the archive. `contract/DATA-CONTRACT.md` section 8.2,
 * and the rules are in `contract/readable-money.json`. #331.
 *
 * **This exists because the platform gave two answers to one question.**
 * `java.text.NumberFormat.getCurrencyInstance` is backed by Android's bundled
 * ICU on the phone and by the JDK's CLDR of a different vintage anywhere else.
 * The same call, the same locale tag and the same currency produced
 * `‏6,790.40 US$` on the phone and `‏٦٬٧٩٠٫٤٠ US$` on this laptop: Latin digits
 * against Arabic-Indic. **8.5's byte identical regeneration is the strongest
 * guarantee the format has**, and an amount that depends on which machine opens
 * the archive makes it a guarantee about one phone.
 *
 * **And `/contract` exists because there are meant to be two readers.** A web
 * platform reading the same archive with `Intl.NumberFormat` would produce a
 * third answer, and the failure would look like data loss rather than like a
 * formatting difference.
 *
 * **So this is the same decision `ReadableDate` already made, applied to the
 * thing that did not get it.** That file spells the month name itself rather
 * than asking the locale, on the reasoning that the stranger a date must survive
 * is a records office or a lawyer who may not read the person's language. Money
 * has exactly the same argument: an amount in a document somebody may rely on
 * has to be unambiguous to whoever opens it, on whatever they open it with, in
 * ten years.
 *
 * **This is not what the app's screens do and must not become it.** `formatMoney`
 * still asks the platform, because a person reading their own notebook should
 * see money the way their phone writes money. Arabic-Indic digits are correct
 * Arabic. The archive is the document that leaves, and it is the one that has to
 * be the same everywhere.
 *
 * **Pure, like everything else the readable copy is built from**, so it is
 * checked exhaustively without a phone.
 *
 * **No isolate marks, and that is a decision rather than an oversight.** Opened
 * in a browser, an Arabic page lays `6,790.40 USD` out as `USD 6,790.40`,
 * because the paragraph runs right to left and the bidi algorithm orders the two
 * runs accordingly. Checked by rendering the page rather than by reasoning about
 * it. **The bytes are identical either way**, which is what 8.5 asks for, and
 * both orders are unambiguous to a reader: the digits are in the right order and
 * the code is beside them. Wrapping the amount in `U+2066`/`U+2069` the way
 * `Bidi.isolate` does on a screen would pin the visual order and would put
 * invisible control characters into a document whose whole standard is that it
 * opens in whatever exists in ten years. **A screen has somebody looking at it
 * and can afford marks a font might one day print; an archive cannot.**
 */
internal object ReadableMoney {

    /**
     * Minor units and an ISO 4217 code, as a figure a stranger can read.
     *
     * `679040` and `USD` give `6,790.40 USD`. `500` and `JPY` give `500 JPY`,
     * because the yen has no minor unit and inventing two decimal places for it
     * would be a claim about the amount that nobody made.
     *
     * **The code rather than a symbol.** `$` is at least six different
     * currencies, and a care record read by a sibling abroad cannot carry an
     * amount that means six things. This is the same fix as spelling the month.
     *
     * **Never through a floating point number**, at any point. The integer is
     * split by division and remainder on `Long`, so an amount that would round
     * cannot: this is money in a record that may be read out in a dispute.
     */
    fun format(minor: Long, currencyCode: String): String {
        val exponent = ReadableCurrency.exponents[currencyCode] ?: DEFAULT_EXPONENT
        val sign = if (minor < 0) "-" else ""

        // Taken on the absolute value so the split is the same either side of
        // zero, and read as a positive magnitude for the whole of the rest.
        // Long.MIN_VALUE has no positive counterpart, so it is widened first
        // rather than negated, which would silently give back itself.
        val magnitude = if (minor < 0) -minor.toBigInteger() else minor.toBigInteger()
        val divisor = java.math.BigInteger.TEN.pow(exponent)
        val whole = magnitude / divisor
        val fraction = magnitude % divisor

        val amount = if (exponent == 0) {
            group(whole.toString())
        } else {
            // Padded to exactly the exponent, trailing zeros and all, because
            // 5.00 and 5.0 and 5 are three different statements about a bill.
            group(whole.toString()) + "." + fraction.toString().padStart(exponent, '0')
        }
        return "$sign$amount $currencyCode"
    }

    /**
     * Digits in threes from the right, separated by a comma.
     *
     * **Written out rather than asked of a formatter**, which is the entire
     * point of this file: a grouping rule that comes from the platform is a
     * grouping rule that changes when the platform does.
     */
    private fun group(digits: String): String {
        if (digits.length <= 3) return digits
        return buildString {
            val lead = digits.length % 3
            if (lead > 0) append(digits, 0, lead)
            for (at in lead until digits.length step 3) {
                if (isNotEmpty()) append(',')
                append(digits, at, at + 3)
            }
        }
    }

    /**
     * What a code this build has never heard of renders with.
     *
     * Two, because it is the overwhelming majority of ISO 4217 and because the
     * alternative is refusing to print an amount the person recorded.
     * `contract/readable-money.json` states it rather than leaving it silent.
     */
    private const val DEFAULT_EXPONENT = 2
}
