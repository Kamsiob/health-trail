package com.kamsiob.healthtrail.data

import java.text.Normalizer

/**
 * Text as the data contract requires it to be stored.
 *
 * `contract/DATA-CONTRACT.md` 8.4 names Unicode normalization as one of the
 * failure modes that corrupt round trips in practice, and until #227 nothing in
 * this app normalized anything.
 */
object Text {

    /**
     * The same characters, in the one form the database stores.
     *
     * **NFC composes**: `e` followed by U+0301 becomes U+00E9. Two people who
     * typed the same name on two keyboards get one row, one search result and
     * one import, instead of two of each that look identical on screen.
     *
     * **Already-normal text is returned unchanged**, which is what
     * `Normalizer.isNormalized` is for: this runs on every string of every
     * write, and almost all of them are already NFC.
     *
     * **It never trims, cases or otherwise edits.** The person's own words are
     * theirs; this changes the encoding of a character and nothing else, and a
     * caller wanting `ifBlank { null }` still has to say so.
     */
    fun nfc(text: String): String =
        if (Normalizer.isNormalized(text, Normalizer.Form.NFC)) {
            text
        } else {
            Normalizer.normalize(text, Normalizer.Form.NFC)
        }
}
