package com.kamsiob.healthtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The digest, against the contract's golden vectors.
 * `contract/test-vectors/digest.json`, issue #15.
 *
 * **These cases used to live in this file as Kotlin**, which made them one
 * platform's tests rather than the contract's vectors. #15 asks for input
 * fixtures paired with the exact expected output, run by the Kotlin suite **and**
 * by the web scaffold, so that if the two engines disagree on one input
 * continuous integration says so. **A vector only one platform can read cannot
 * do that**, so they moved to `/contract` on 2026-08-10 and this file reads them.
 *
 * **They run on the JVM with no database and no composition**, because the thing
 * that has to be right is the counting rule rather than the pixels.
 *
 * **What this still does not prove**, said plainly: there is no second engine
 * yet. `web/` holds a README and nothing else, which is #16. Until that exists
 * these are golden vectors with one reader, and the acceptance criterion about
 * continuous integration failing when two platforms disagree cannot be met by
 * anything in this repository.
 *
 * **Regenerating is not a thing here.** Unlike the readable vector, these
 * expectations are written by hand in the contract file with a sentence saying
 * why each one is what it is. A case whose answer changed is a decision, and it
 * is made by editing that file and reading the diff.
 */
class DigestTest {

    /**
     * Every case in the contract, run as itself.
     *
     * **One test method rather than one per case**, because the cases are data
     * now: adding one to the contract file must not need a Kotlin edit, or the
     * file stops being the source and becomes a copy. The failure message names
     * the case and quotes the contract's own reason for it, so a red run says
     * which rule broke rather than which line did.
     */
    @Test
    fun `every case in the contract holds`() {
        assertTrue("the contract carries no cases", DigestVector.cases.isNotEmpty())

        for (case in DigestVector.cases) {
            val summary = Digest.since(case.changes, since = case.since)
            val where = "${case.name}\n  the contract says: ${case.why}\n "

            assertEquals(
                "$where added does not match, and its order is part of the answer",
                case.added,
                summary.added.map { it.section.name to it.count },
            )
            assertEquals("$where corrected does not match", case.corrected, summary.corrected)
            assertEquals("$where removed does not match", case.removed, summary.removed)
            assertEquals(
                "$where the total of new things does not match its own section counts",
                case.added.sumOf { it.second },
                summary.newThings,
            )
            assertEquals(
                "$where isEmpty disagrees with the counts beside it",
                case.added.isEmpty() && case.corrected == 0 && case.removed == 0,
                summary.isEmpty,
            )
        }
    }

    /**
     * The mapping is the contract's, and the engine agrees with it.
     *
     * **This is the assertion whose absence hid #336 for the life of the
     * project.** The test that used to be here walked a hard-coded list in this
     * file that said `reading`, and so did the code, so two copies of one
     * mistake agreed with each other and Progress reported nothing forever.
     *
     * **The list is in the contract now and `check_digest_sections.py` holds
     * that list to `contract/schema.sql`.** So the chain runs schema, to
     * contract, to engine, and no link in it is a list somebody typed twice.
     */
    @Test
    fun `the engine maps exactly what the contract says it maps`() {
        assertTrue("the contract carries no sections", DigestVector.sections.isNotEmpty())

        for ((table, section) in DigestVector.sections) {
            val mapped = Digest.sectionOf(table)
            assertTrue("$table maps to no section, and the contract says $section", mapped != null)
            assertEquals("$table maps to the wrong section", section, mapped!!.name)
        }
    }

    /**
     * And it maps nothing the contract does not.
     *
     * **The half that stops the mapping growing quietly.** A table added to the
     * engine and not to the contract would be counted into a section on the
     * person's screen with nothing anywhere saying it should be.
     */
    @Test
    fun `the engine maps nothing the contract leaves out`() {
        for (table in DigestVector.unmapped) {
            assertNull(
                "$table is left out by the contract and the engine counts it into a section",
                Digest.sectionOf(table),
            )
        }
    }

    /**
     * A table nobody has heard of is left out rather than counted.
     *
     * The `else` branch, asserted directly, because everything above it only
     * proves the branches that exist.
     */
    @Test
    fun `a table from a later schema is left out`() {
        assertNull(Digest.sectionOf("something_a_later_version_added"))
    }
}
