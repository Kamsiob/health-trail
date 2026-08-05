package com.kamsiob.healthtrail.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The schema is split into statements before it is executed, because Android's
 * execSQL takes one statement at a time. Splitting it wrongly is not a loud
 * failure: a trigger body cut in half produces a confusing syntax error a long
 * way from its cause, and a trigger silently dropped produces a database that
 * looks fine and quietly stops recording changes.
 *
 * These run against the real `contract/schema.sql` rather than a fixture, so a
 * change to the schema that breaks the splitter is caught here rather than on a
 * device.
 */
class SchemaStatementSplitTest {

    private val schema: String by lazy {
        val file = File("../../contract/schema.sql")
        assertTrue(
            "contract/schema.sql was not found at ${file.absolutePath}. " +
                "Unit tests run with the module directory as the working directory.",
            file.isFile,
        )
        file.readText()
    }

    @Test
    fun `every trigger survives the split intact`() {
        val statements = ContractAssets.splitStatements(schema)
        val triggers = statements.filter { it.contains("CREATE TRIGGER", ignoreCase = true) }

        // Two triggers per user data table, counted from the schema rather than
        // written down here. The number was 68 and is 80, and a hardcoded count
        // means every table added to the contract fails this test for a reason
        // that has nothing to do with splitting, which is what this test is
        // about. That every table has both triggers is check_schema.py's job.
        val declared = Regex("""CREATE TRIGGER""", RegexOption.IGNORE_CASE)
            .findAll(schema).count()
        assertEquals("the splitter lost or invented a trigger", declared, triggers.size)

        for (trigger in triggers) {
            assertTrue(
                "a trigger was cut before its body: ${trigger.take(80)}",
                trigger.contains("BEGIN"),
            )
            assertTrue(
                "a trigger was cut before its END: ${trigger.take(80)}",
                trigger.trimEnd().endsWith("END;"),
            )
            assertTrue(
                "a trigger lost its change log insert: ${trigger.take(80)}",
                trigger.contains("INSERT INTO change_log"),
            )
        }
    }

    @Test
    fun `no statement is a bare fragment`() {
        val statements = ContractAssets.splitStatements(schema)
        assertTrue("the splitter produced nothing", statements.isNotEmpty())

        val recognized = listOf(
            "CREATE TABLE", "CREATE VIEW", "CREATE TRIGGER", "CREATE INDEX",
            // The lead slot's uniqueness is a partial unique index, which is the
            // first of its kind in the schema. DESIGN.md 21.1.
            "CREATE UNIQUE INDEX",
            "PRAGMA",
        )
        for (statement in statements) {
            val head = statement.lineSequence()
                .map { it.substringBefore("--").trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            assertTrue(
                "statement does not begin with anything recognizable: ${statement.take(90)}",
                recognized.any { head.startsWith(it, ignoreCase = true) },
            )
        }
    }

    @Test
    fun `the split covers every statement in the file`() {
        val statements = ContractAssets.splitStatements(schema)

        fun countIn(text: String, needle: String) =
            Regex(needle, RegexOption.IGNORE_CASE).findAll(text).count()

        // Comments in the schema mention these words, so the file is compared
        // against itself with comments stripped rather than raw.
        val withoutComments = schema.lineSequence()
            .joinToString("\n") { it.substringBefore("--") }

        for (kind in listOf("CREATE TABLE", "CREATE VIEW", "CREATE TRIGGER", "CREATE INDEX")) {
            assertEquals(
                "$kind count differs between the file and the split statements",
                countIn(withoutComments, kind),
                statements.sumOf { countIn(it, kind) },
            )
        }
    }

    @Test
    fun `pragmas are separated so they can be routed away from execSQL`() {
        val statements = ContractAssets.splitStatements(schema)
        val pragmas = statements.filter { it.trimStart().startsWith("PRAGMA", ignoreCase = true) }

        assertTrue("no pragma statements were found", pragmas.isNotEmpty())
        for (pragma in pragmas) {
            assertEquals(
                "a pragma was bundled with another statement, which would send it " +
                    "to execSQL and fail on any pragma that returns a row",
                1,
                Regex(";").findAll(pragma).count(),
            )
        }
    }
}
