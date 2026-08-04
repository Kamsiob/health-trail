package com.kamsiob.healthtrail.data

import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Reads a plain SQLite file into the rows the readable copy renders from.
 *
 * **The only Android in the readable pipeline.** `ReadableArchive` is pure so it
 * can be checked exhaustively without a phone; this is the thin layer that
 * hands it real rows, and it is deliberately dull for that reason.
 *
 * **It reads the export's own copy of the database, not the live one.** The file
 * being exported has already been decrypted and staged, so this cannot race a
 * write, and an archive whose readable copy disagreed with its payload would be
 * the silent partial correctness `contract/DATA-CONTRACT.md` section 8 opens by
 * calling worse than an honest failure.
 *
 * **Tombstones never come out.** Every read filters `deleted_at IS NULL` where
 * the column exists. A deleted row is absent from the readable copy by
 * definition: printing that something was deleted would put back what the person
 * removed, and this is the document they may hand to somebody.
 *
 * **Every query has an explicit ORDER BY**, which is section 8.4's named failure
 * mode. Ordering that depends on rowid or on insertion order is how one database
 * produces two different archives, and 8.5's regeneration test would fail
 * intermittently rather than never, which is worse.
 */
internal object ReadableRows {

    /**
     * @param database the staged plain SQLite file.
     * @param tables which tables to read, which is every table the field map
     *   renders anything for.
     */
    fun read(database: File, tables: Collection<String>): Map<String, List<Map<String, String?>>> {
        val out = LinkedHashMap<String, List<Map<String, String?>>>()
        SQLiteDatabase.openDatabase(
            database.path, null, SQLiteDatabase.OPEN_READONLY,
        ).use { db ->
            val present = presentTables(db)
            for (table in tables.sorted()) {
                if (table !in present) continue
                val columns = columnsOf(db, table)
                if (columns.isEmpty()) continue
                val live = if ("deleted_at" in columns) " WHERE deleted_at IS NULL" else ""
                // By id, which is stable, locally generated, and never reused.
                // Sorting by anything the person can edit would reorder the
                // archive when they corrected a typo.
                val order = if ("id" in columns) " ORDER BY id" else ""
                val rows = mutableListOf<Map<String, String?>>()
                db.rawQuery("SELECT * FROM \"$table\"$live$order", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val row = LinkedHashMap<String, String?>(columns.size)
                        for (index in columns.indices) {
                            row[columns[index]] =
                                if (cursor.isNull(index)) null else cursor.getString(index)
                        }
                        rows += row
                    }
                }
                out[table] = rows
            }
        }
        return out
    }

    private fun presentTables(db: SQLiteDatabase): Set<String> {
        val names = mutableSetOf<String>()
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' ORDER BY name",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) names += cursor.getString(0)
        }
        return names
    }

    /**
     * Column names in the order the table declares them, which is the order
     * `SELECT *` returns and therefore the order the cursor indexes match.
     */
    private fun columnsOf(db: SQLiteDatabase, table: String): List<String> {
        val columns = mutableListOf<String>()
        db.rawQuery("PRAGMA table_info(\"$table\")", null).use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameColumn)
        }
        return columns
    }

    /** The subject's name, for the front door, or null if there is not one yet. */
    fun subjectName(rows: Map<String, List<Map<String, String?>>>): String? =
        rows["subject"]?.firstOrNull()?.get("display_name")
            ?: rows["subject"]?.firstOrNull()?.get("name")
}
