package com.kamsiob.healthtrail.ui.screens

/**
 * What a person can write down, and the tags every way in shares.
 *
 * **Lifted out of `CaptureSheet.kt` on 2026-08-13, when that screen was frozen.**
 * The sheet was superseded by the capture bloom in the shell, and the file it
 * lived in still held the two things the live path uses: the six kinds, which
 * nine files name, and the tags, which the bloom draws and `BackJourneyTest`
 * walks. **A frozen file is never called**, `docs/REMOVAL-LEDGER.md`, and that
 * rule cannot be true of a file the live path imports from, so the live half
 * moved out rather than the ledger being bent around it.
 */
object CaptureTags {
    const val SHEET = "capture_sheet"
    fun option(kind: CaptureKind) = "capture_option_${kind.name.lowercase()}"
}

/**
 * The six things a person can write down.
 *
 * The order is the order in `MASTER_SPEC.md` section 4.2 and it does not change,
 * because someone reaching for this while a nurse is still on the phone is
 * reaching by position rather than by reading.
 */
enum class CaptureKind { CALL, VISIT, INCIDENT, MEASUREMENT, QUESTION, DOCUMENT }
