package com.kamsiob.healthtrail.contract

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

/**
 * Reads the shared contract that the build copied into assets.
 *
 * The app does not keep its own copy of the schema, the export format, the
 * template catalog, or the message catalogs. All of them live at the root of
 * the monorepo and are read from `/contract` and `/templates`, because a schema
 * that exists only as platform code makes the web version a reimplementation
 * rather than a second reader, and the two drift apart within weeks.
 *
 * If any of it is missing, this reports that plainly rather than falling back to
 * something internal. There is nothing to fall back to, and inventing something
 * would be worse than failing.
 */
object ContractAssets {

    const val SCHEMA_PATH = "contract/schema.sql"
    const val EXPORT_FORMAT_PATH = "contract/EXPORT-FORMAT.md"

    /**
     * Which of the schema's columns the archive's readable copy renders.
     *
     * `contract/DATA-CONTRACT.md` 8.5. In `/contract` rather than here because
     * the web version renders the same archive from the same decisions, and two
     * copies of this would drift within weeks.
     */
    const val READABLE_FIELDS_PATH = "contract/readable-fields.json"

    private val TEMPLATE_FILES = listOf(
        "templates/situations.json",
        "templates/projects.json",
        "templates/progress-and-instructions.json",
    )

    /** The canonical schema, as the DDL text the build copied in. */
    fun readSchema(context: Context): String =
        context.assets.open(SCHEMA_PATH).bufferedReader().use { it.readText() }

    /**
     * What the schema actually produced when executed, rather than what it was
     * expected to produce.
     *
     * This runs the real DDL in a throwaway in-memory database. It is the proof
     * that the contract reached this device intact and that SQLite on this
     * device accepts it, which is not the same thing as the file being present.
     */
    fun inspectSchema(context: Context): SchemaFacts {
        val sql = readSchema(context)
        val database = SQLiteDatabase.create(null)
        return try {
            // The DDL is split on statement boundaries rather than executed as
            // one string, because execSQL takes a single statement. Semicolons
            // inside trigger bodies mean a naive split is wrong, so BEGIN and
            // END are tracked.
            //
            // Pragmas go through rawQuery rather than execSQL. Android's
            // execSQL refuses any statement that returns rows, and several
            // pragmas do: PRAGMA journal_mode returns the mode it settled on.
            // Passing one to execSQL fails with "Queries can be performed using
            // SQLiteDatabase query or rawQuery methods only", which reads like a
            // schema problem and is not one.
            for (statement in splitStatements(sql)) {
                if (statement.trimStart().startsWith("PRAGMA", ignoreCase = true)) {
                    database.rawQuery(statement, null).use { it.moveToFirst() }
                } else {
                    database.execSQL(statement)
                }
            }
            SchemaFacts(
                tables = count(database, "table"),
                views = count(database, "view"),
                triggers = count(database, "trigger"),
                indexes = count(database, "index"),
                error = null,
            )
        } catch (error: Exception) {
            SchemaFacts(0, 0, 0, 0, error.message ?: error.javaClass.simpleName)
        } finally {
            database.close()
        }
    }

    /**
     * How many templates the catalog holds, counted from the same JSON files the
     * web platform reads.
     */
    fun countTemplates(context: Context): Int {
        var total = 0
        for (path in TEMPLATE_FILES) {
            val text = context.assets.open(path).bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            for (key in listOf("templates", "progress_presets", "standing_instructions")) {
                if (root.has(key)) {
                    total += root.getJSONArray(key).length()
                }
            }
        }
        return total
    }

    /** True when every file the contract requires is present in assets. */
    fun isComplete(context: Context): Boolean = try {
        context.assets.open(SCHEMA_PATH).close()
        context.assets.open(EXPORT_FORMAT_PATH).close()
        TEMPLATE_FILES.forEach { context.assets.open(it).close() }
        true
    } catch (_: Exception) {
        false
    }

    private fun count(database: SQLiteDatabase, type: String): Int =
        database.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name NOT LIKE 'sqlite_%'",
            arrayOf(type),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    /**
     * Splits DDL into executable statements.
     *
     * A plain split on semicolons is wrong here, because every change log
     * trigger contains semicolons inside its BEGIN and END block. Those have to
     * stay with their trigger or SQLite receives fragments.
     */
    internal fun splitStatements(sql: String): List<String> {
        val statements = mutableListOf<String>()
        val current = StringBuilder()
        var insideBlock = false

        for (rawLine in sql.lineSequence()) {
            val line = rawLine.substringBefore("--").trimEnd()
            if (line.isBlank() && current.isBlank()) continue

            current.append(rawLine).append('\n')
            val upper = line.uppercase()

            if (Regex("""\bBEGIN\b""").containsMatchIn(upper)) {
                insideBlock = true
            }

            val ends = line.trimEnd().endsWith(";")
            if (!ends) continue

            if (insideBlock) {
                // Only END; closes a trigger body.
                if (Regex("""^\s*END\s*;""").containsMatchIn(line)) {
                    insideBlock = false
                    statements.add(current.toString().trim())
                    current.clear()
                }
            } else {
                statements.add(current.toString().trim())
                current.clear()
            }
        }

        if (current.isNotBlank()) statements.add(current.toString().trim())
        return statements.filter { it.isNotBlank() && it != ";" }
    }
}

/** What executing the schema produced on this device. */
data class SchemaFacts(
    val tables: Int,
    val views: Int,
    val triggers: Int,
    val indexes: Int,
    val error: String?,
) {
    val isUsable: Boolean get() = error == null && tables > 0
}
