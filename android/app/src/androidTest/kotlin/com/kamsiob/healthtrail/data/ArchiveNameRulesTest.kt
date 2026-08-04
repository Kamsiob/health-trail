package com.kamsiob.healthtrail.data

import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The entry name rules from `contract/DATA-CONTRACT.md` section 8.4.
 *
 * **The failure mode this covers is "Filenames"**, and the contract names it
 * because it is the whole class of ways an archive extracts correctly on the
 * phone that wrote it and partially somewhere else. A name that is fine on
 * Android and impossible on Windows, or two names that differ only by case on a
 * volume that does not, turns a restore into a silent partial one.
 *
 * **The writer enforces this and nothing proved the enforcement fires.** It was
 * added after checking a real export by hand: 106 files, longest path 76
 * characters, no clashes. That is evidence about one archive on one night, not
 * about the rule, and the names that will break it have not been generated yet
 * because they come from what somebody types.
 *
 * **Every case here is refused by throwing rather than by writing a bad file.**
 * `contract/EXPORT-FORMAT.md` section 5: a writer that cannot satisfy these
 * fails the export. An archive that restores most of itself is the silent
 * partial correctness section 8 opens by calling worse than an honest failure.
 */
class ArchiveNameRulesTest {

    @Test
    fun ordinaryNamesAreAccepted() {
        // The shapes the writer actually produces, so a rule that refused
        // everything would fail here rather than passing every other test.
        listOf(
            "README.txt",
            "MANIFEST.json",
            "CHECKSUMS.txt",
            "data/trail.sqlite",
            "data/schema.sql",
            "readable/index.html",
            "readable/entries/2026-06.html",
            "attachments/" + "a".repeat(64),
        ).forEach { ExportContainer.requireSafeName(it) }
    }

    @Test
    fun aNameOutsideAsciiIsRefused() {
        // **The whole codepage class of failure in one character.** A zip entry
        // name is bytes plus a flag saying how to read them, and the flag is
        // not reliably honored. An accented name written here comes out mojibake
        // on a machine that guesses a different codepage.
        assertThrows(IllegalArgumentException::class.java) {
            ExportContainer.requireSafeName("readable/josé.html")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExportContainer.requireSafeName("readable/الفصول.html")
        }
    }

    @Test
    fun aPathLongerThanTheLimitIsRefused() {
        // Extracted a few folders deep, a long path stops being long and starts
        // being impossible.
        val long = "readable/" + "a".repeat(200) + ".html"
        assertThrows(IllegalArgumentException::class.java) {
            ExportContainer.requireSafeName(long)
        }
    }

    @Test
    fun charactersSomeSystemsRefuseAreRefused() {
        listOf(
            "readable/what: happened.html",
            "readable/she said \"no\".html",
            "readable/why?.html",
            "readable/a*b.html",
            "readable/a|b.html",
            "readable/a<b.html",
            "readable/a>b.html",
            "readable\\index.html",
        ).forEach { name ->
            assertThrows(IllegalArgumentException::class.java) {
                ExportContainer.requireSafeName(name)
            }
        }
    }

    @Test
    fun reservedDeviceNamesAreRefused() {
        // **Forty years on, and still true.** This is not a name the app would
        // generate today. It is exactly the name that arrives the day somebody's
        // chapter is called Con, or a document is named after a person called
        // Prn, and it would fail on one operating system and nowhere else.
        listOf(
            "readable/CON.html",
            "readable/con.html",
            "readable/PRN",
            "readable/aux.txt",
            "readable/NUL.html",
            "readable/COM1.html",
            "readable/lpt9.html",
        ).forEach { name ->
            assertThrows(IllegalArgumentException::class.java) {
                ExportContainer.requireSafeName(name)
            }
        }
    }

    @Test
    fun aNameThatIsNotReservedIsStillAccepted() {
        // The rule matches the stem, so it must not swallow ordinary words that
        // merely begin with one. "Consent form" is a document somebody will
        // certainly have.
        listOf(
            "readable/consent.html",
            "readable/console.html",
            "readable/company.html",
            "readable/nullify.html",
        ).forEach { ExportContainer.requireSafeName(it) }
    }
}
