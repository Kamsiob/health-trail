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

        // 34 user data tables, two triggers each.
        assertEquals("wrong number of trigger statements", 68, triggers.size)

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
            "CREATE TABLE", "CREATE VIEW", "CREATE TRIGGER", "CREATE INDEX", "PRAGMA",
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
