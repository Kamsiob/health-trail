package com.kamsiob.healthtrail.i18n

import java.text.NumberFormat
import java.util.Currency

/**
 * Minor units rendered as money, in the catalog's locale but the record's own
 * currency.
 *
 * **The locale decides the shape and the record decides the currency.** An
 * Arabic reader in the United States is still looking at dollars, and rendering
 * them with the locale's default currency would silently relabel the amount.
 *
 * **It lived beside the money screen until 2026-08-09**, which was fine while a
 * screen was the only thing that showed an amount. The archive shows them too,
 * and `contract/DATA-CONTRACT.md` 8.2 is the reason it had to move rather than
 * be copied: the readable copy printed `679040` where the bill said six
 * thousand seven hundred and ninety dollars and forty cents, and a second
 * implementation of this would have been a second rounding rule on somebody's
 * money. #328.
 *
 * **It stays out of `data` and out of `ui`** because it belongs to neither. It
 * is a locale question, which is what this package is.
 *
 * @param currencyCode an ISO 4217 code. Anything unparseable falls back to the
 *   dollar rather than throwing, because an archive that fails to open over a
 *   currency code is worse than one that names the wrong symbol, and the amount
 *   itself is never wrong either way.
 */
internal fun formatMoney(strings: Strings, minor: Long, currencyCode: String): String {
    val format = NumberFormat.getCurrencyInstance(strings.locale)
    val currency = runCatching { Currency.getInstance(currencyCode) }
        .getOrElse { Currency.getInstance("USD") }
    format.currency = currency
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    format.minimumFractionDigits = digits
    format.maximumFractionDigits = digits
    // **Divided as a decimal rather than a double**, so nothing rounds on the
    // way out. This is money in a record that may be read out in a dispute.
    val major = java.math.BigDecimal(minor).movePointLeft(digits)
    return format.format(major)
}
