#!/usr/bin/env python3
"""Assert that contract/schema.sql satisfies the data contract.

Two halves. The first reads the schema and asserts its shape, table by table,
rather than trusting anyone to have looked. The second loads it into a real
SQLite database and exercises the behavior, because a trigger that exists is
not the same as a trigger that fires correctly.

The three things this is guarding against are the three the data contract names
as impossible to fix later without discarding user data: sync onto
auto-increment primary keys, sync onto a schema that deletes rows, and a second
platform against an undocumented schema.

Exit 0 when clean, 1 with a list of failures otherwise.

Kamsiob, AGPL-3.0.
"""

import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "contract" / "schema.sql"

REQUIRED_COLUMNS = {
    "id": ("TEXT", True),             # (declared type, must be NOT NULL)
    "created_at": ("INTEGER", True),
    "updated_at": ("INTEGER", True),
    "deleted_at": ("INTEGER", False),  # nullable is the whole point
    "origin_device": ("TEXT", True),
    "rev": ("INTEGER", True),
}

# Local tables. They describe this installation rather than the person's
# records, so they carry no tombstone and are the only place AUTOINCREMENT is
# allowed.
LOCAL_TABLES = {"app_meta", "device", "change_log", "conflict_log", "schema_migration"}

# Bookkeeping about the row rather than a claim about the world. These stay UTC
# milliseconds, per data contract section 3.
ROW_METADATA = {"created_at", "updated_at", "deleted_at", "changed_at"}

# Timestamps recording something the app did at the moment the person did it in
# the app, which is genuinely an instant and genuinely known. Each one is here
# on purpose, and adding to this set is the loophole in check_event_dates, so
# every entry carries its reason.
APP_ACTION_TIMES = {
    "closed_at",      # subject: the person closed the notebook, in the app, now
    "archived_at",    # organization, person: archived in the app, now
    "pinned_at",      # entry: pinned in the app, now
    "resolved_at",    # conflict_log: local bookkeeping, not a user record
    "seen_at",        # conflict_log: local bookkeeping
    "applied_at",     # schema_migration: local bookkeeping
}

CHANGE_LOG_COLUMNS = {
    "seq", "table_name", "row_id", "op", "rev", "changed_at", "device_id",
}


class SchemaCheck:
    def __init__(self):
        self.failures = []
        self.sql = SCHEMA.read_text(encoding="utf-8")
        self.db = sqlite3.connect(":memory:")
        self.db.executescript(self.sql)
        self.db.execute("PRAGMA foreign_keys = ON")

    def fail(self, message):
        self.failures.append(message)

    def objects(self, kind):
        return [
            row[0]
            for row in self.db.execute(
                "SELECT name FROM sqlite_master WHERE type = ? "
                "AND name NOT LIKE 'sqlite_%' ORDER BY name",
                (kind,),
            )
        ]

    def user_tables(self):
        found = []
        for table in self.objects("table"):
            if table in LOCAL_TABLES:
                continue
            columns = {
                row[1] for row in self.db.execute(f"PRAGMA table_info({table})")
            }
            if "origin_device" in columns:
                found.append(table)
            else:
                self.fail(
                    f"{table}: not in the local table list and has no origin_device. "
                    f"Either it is a user data table missing the required columns, "
                    f"or it is a local table that needs adding to LOCAL_TABLES here."
                )
        return found

    # -- shape -------------------------------------------------------------

    def check_columns(self, tables):
        for table in tables:
            info = {
                row[1]: {"type": row[2].upper(), "notnull": bool(row[3]), "pk": row[5]}
                for row in self.db.execute(f"PRAGMA table_info({table})")
            }
            for name, (expected_type, must_be_not_null) in REQUIRED_COLUMNS.items():
                if name not in info:
                    self.fail(f"{table}: missing required column {name!r}")
                    continue
                column = info[name]
                if column["type"] != expected_type:
                    self.fail(
                        f"{table}.{name}: declared {column['type']}, "
                        f"the contract requires {expected_type}"
                    )
                if must_be_not_null and not column["notnull"]:
                    self.fail(f"{table}.{name}: must be NOT NULL")
                if name == "deleted_at" and column["notnull"]:
                    self.fail(
                        f"{table}.deleted_at: must be nullable. "
                        f"Null means live, a timestamp means deleted."
                    )
            if "id" in info and info["id"]["pk"] != 1:
                self.fail(f"{table}.id: must be the primary key")

    def check_no_autoincrement(self, tables):
        for table in tables:
            row = self.db.execute(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name=?", (table,)
            ).fetchone()
            if row and re.search(r"AUTOINCREMENT", row[0], re.IGNORECASE):
                self.fail(
                    f"{table}: uses AUTOINCREMENT. Two devices both creating row 47 "
                    f"has no correct merge, and fixing it after real data exists "
                    f"means reassigning every id and every foreign key."
                )
            if row and re.search(r"\bid\s+INTEGER\b", row[0], re.IGNORECASE):
                self.fail(f"{table}.id: declared INTEGER. Ids are TEXT, generated locally.")

    def check_views(self, tables):
        views = set(self.objects("view"))
        for table in tables:
            expected = f"live_{table}"
            if expected not in views:
                self.fail(
                    f"{table}: no {expected} view. Every read goes through a view "
                    f"that filters tombstones, because one forgotten "
                    f"'deleted_at IS NULL' is a data leak of something the person "
                    f"believed they deleted."
                )
                continue
            # The view must actually filter, not merely exist.
            row = self.db.execute(
                "SELECT sql FROM sqlite_master WHERE type='view' AND name=?", (expected,)
            ).fetchone()
            if row and "deleted_at IS NULL" not in row[0]:
                self.fail(f"{expected}: exists but does not filter on deleted_at IS NULL")

    def check_triggers(self, tables):
        triggers = set(self.objects("trigger"))
        for table in tables:
            for suffix in ("insert", "update"):
                name = f"trg_{table}_{suffix}"
                if name not in triggers:
                    self.fail(
                        f"{table}: missing {name}. Every write appends to the change "
                        f"log in the same transaction, and that is enforced by "
                        f"triggers rather than by the application remembering."
                    )

    def check_local_tables(self):
        tables = set(self.objects("table"))
        for required in ("change_log", "conflict_log"):
            if required not in tables:
                self.fail(f"{required} is missing, and the data contract requires it")
        if "change_log" in tables:
            columns = {row[1] for row in self.db.execute("PRAGMA table_info(change_log)")}
            missing = CHANGE_LOG_COLUMNS - columns
            if missing:
                self.fail(f"change_log is missing columns: {sorted(missing)}")

    def check_foreign_keys(self, tables):
        for table in tables:
            for row in self.db.execute(f"PRAGMA foreign_key_list({table})"):
                target_table, to_column = row[2], row[4]
                # SQLite reports None for "to" when it implies the primary key.
                if to_column not in (None, "id"):
                    self.fail(
                        f"{table}: foreign key points at {target_table}.{to_column}. "
                        f"Ids are the only thing foreign keys ever point at."
                    )

    def check_event_dates(self, tables):
        """Every event date is a full section 3.1 group, and no bare one survives.

        Two failures are worth catching automatically and neither is visible by
        reading. A group missing its range columns means the database cannot
        sort or answer a date query, so someone will quietly add a second
        timestamp beside it. A surviving bare `<name>_at` on a world event
        means one date in the app can still assert a precision the person never
        gave, which is the whole defect the model exists to remove.
        """
        for table in tables:
            columns = {row[1] for row in self.db.execute(f"PRAGMA table_info({table})")}

            for column in sorted(columns):
                if not column.endswith("_edtf"):
                    continue
                base = column[: -len("_edtf")]
                for part, kind in (("zone", "TEXT"), ("start", "INTEGER"), ("end", "INTEGER")):
                    if f"{base}_{part}" not in columns:
                        self.fail(
                            f"{table}.{column} has no {base}_{part}. An event date is the "
                            f"whole group in data contract section 3.1: the EDTF string, "
                            f"the zone, and the derived range. A string on its own cannot "
                            f"be sorted or searched, which is how a second timestamp "
                            f"ends up beside it."
                        )

            for column in sorted(columns):
                if column in ROW_METADATA or column in APP_ACTION_TIMES:
                    continue
                if not column.endswith("_at"):
                    continue
                self.fail(
                    f"{table}.{column} is a bare timestamp on something that happened in "
                    f"the world. Event dates are the group in data contract section 3.1, "
                    f"because a single instant asserts a precision the person may never "
                    f"have given. If this genuinely records something the app did rather "
                    f"than something that happened, add it to APP_ACTION_TIMES with a "
                    f"reason."
                )

    def check_retention_documented(self):
        match = re.search(
            r"TOMBSTONE RETENTION WINDOW:\s*(\d+)\s*days", self.sql, re.IGNORECASE
        )
        if not match:
            self.fail(
                "The tombstone retention window is not stated in the schema comments. "
                "The data contract requires the number written in now so the future "
                "implementation does not have to guess."
            )
            return None
        return int(match.group(1))

    # -- behavior ----------------------------------------------------------

    def check_change_log_behavior(self):
        """Exercise the triggers rather than trusting that they exist."""
        db = sqlite3.connect(":memory:")
        db.executescript(self.sql)
        db.execute("PRAGMA foreign_keys = ON")
        db.execute(
            "INSERT INTO app_meta (key, value, updated_at) VALUES ('device_id', 'dev-a', 1)"
        )

        def log_rows():
            return list(
                db.execute(
                    "SELECT table_name, row_id, op, rev, device_id "
                    "FROM change_log ORDER BY seq"
                )
            )

        # Insert.
        db.execute(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) "
            "VALUES ('s1', 100, 100, 'dev-a', 1, 'Mum')"
        )
        rows = log_rows()
        if len(rows) != 1 or rows[0][:3] != ("subject", "s1", "insert"):
            self.fail(f"insert did not append one 'insert' change_log row, got {rows}")
        if rows and rows[0][4] != "dev-a":
            self.fail(f"change_log device_id is {rows[0][4]!r}, expected the app_meta value")

        # Update.
        db.execute("UPDATE subject SET display_name='Mom', updated_at=200, rev=2 WHERE id='s1'")
        rows = log_rows()
        if len(rows) != 2 or rows[1][2] != "update":
            self.fail(f"update did not append an 'update' row, got {rows}")

        # Tombstone. Deletion is an update that sets deleted_at, and it must be
        # recorded as a delete, because a peer that reads it as an edit will not
        # remove the row.
        db.execute("UPDATE subject SET deleted_at=300, updated_at=300, rev=3 WHERE id='s1'")
        rows = log_rows()
        if len(rows) != 3 or rows[2][2] != "delete":
            self.fail(f"setting deleted_at did not append a 'delete' row, got {rows}")

        # Undelete records an update, which is correct: the peer needs to know
        # the tombstone was lifted.
        db.execute("UPDATE subject SET deleted_at=NULL, updated_at=400, rev=4 WHERE id='s1'")
        rows = log_rows()
        if len(rows) != 4 or rows[3][2] != "update":
            self.fail(f"undelete did not append an 'update' row, got {rows}")

        # The live view hides a tombstone.
        db.execute("UPDATE subject SET deleted_at=500, updated_at=500, rev=5 WHERE id='s1'")
        live = db.execute("SELECT COUNT(*) FROM live_subject").fetchone()[0]
        base = db.execute("SELECT COUNT(*) FROM subject").fetchone()[0]
        if live != 0:
            self.fail(f"live_subject returned {live} rows for a tombstoned row, expected 0")
        if base != 1:
            self.fail(f"the tombstone was removed from the base table, expected it to remain")

        db.close()

    def check_same_transaction(self):
        """Prove the log entry and the write share a transaction.

        Forcing the log insert to fail must roll the data write back with it. If
        they were in different transactions the row would survive, which is a
        silent hole in the record: a write nothing can tell a peer about.
        """
        db = sqlite3.connect(":memory:")
        db.executescript(self.sql)
        db.execute(
            "INSERT INTO app_meta (key, value, updated_at) VALUES ('device_id', 'dev-a', 1)"
        )
        # A constraint the change log insert will violate, without touching the
        # user data table at all.
        db.execute(
            "CREATE TRIGGER trg_test_block BEFORE INSERT ON change_log "
            "BEGIN SELECT RAISE(ABORT, 'change log write refused for the test'); END"
        )
        try:
            db.execute(
                "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, display_name) "
                "VALUES ('s2', 100, 100, 'dev-a', 1, 'Dad')"
            )
            db.commit()
            self.fail(
                "a failing change_log write did not abort the statement, "
                "so writes and their log entries are not atomic"
            )
        except sqlite3.IntegrityError:
            pass
        except sqlite3.OperationalError:
            pass

        survived = db.execute("SELECT COUNT(*) FROM subject WHERE id='s2'").fetchone()[0]
        if survived:
            self.fail(
                "the row survived a failed change_log write. The write and its log "
                "entry must be in the same transaction, or a peer can never learn "
                "about a change that did happen."
            )
        db.close()

    def check_lead_slot_is_singular(self):
        """The Today lead slot is singular by construction, not by discipline.

        `DESIGN.md` 21.1: Today always has exactly one lead slot, and there is
        never zero and never two. **Two is refused here**, by the partial unique
        index `ux_today_card_lead`, so that it is a property of the file rather
        than a promise a screen keeps. Zero is the application's to prevent,
        because a database cannot require a row to exist.

        Exercised rather than read, for the same reason the change log behavior
        is: an index that exists is not the same as an index that refuses.
        """
        db = sqlite3.connect(":memory:")
        db.executescript(self.sql)
        db.execute("PRAGMA foreign_keys = ON")
        db.execute(
            "INSERT INTO app_meta (key, value, updated_at) VALUES ('device_id', 'dev-a', 1)"
        )
        db.execute(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, "
            "display_name) VALUES ('s1', 1, 1, 'dev-a', 1, 'Mum')"
        )

        def card(row_id, card_type, is_lead, index):
            db.execute(
                "INSERT INTO today_card (id, created_at, updated_at, origin_device, rev, "
                "subject_id, card_type, size, sort_index, is_lead) "
                "VALUES (?, 1, 1, 'dev-a', 1, 's1', ?, 'small', ?, ?)",
                (row_id, card_type, index, is_lead),
            )

        card("c1", "digest", 1, 0)
        card("c2", "next_up", 0, 1)

        try:
            card("c3", "incidents", 1, 2)
            self.fail(
                "today_card accepted a second lead for one subject. The lead slot is "
                "singular by construction, DESIGN.md 21.1, and a screen that has to "
                "remember that is a screen that forgets it once, quietly."
            )
        except sqlite3.IntegrityError:
            pass

        # A tombstoned lead must not block the next one, or a card could never
        # be promoted again after the layout was replaced.
        db.execute("UPDATE today_card SET deleted_at = 2, updated_at = 2, rev = 2 WHERE id = 'c1'")
        try:
            card("c4", "measure", 1, 3)
        except sqlite3.IntegrityError:
            self.fail(
                "a tombstoned lead still blocks a new one, so a layout could never be "
                "replaced. The index must be filtered on deleted_at IS NULL."
            )

        # Two subjects each have their own lead, which is what makes the index
        # per subject rather than global.
        db.execute(
            "INSERT INTO subject (id, created_at, updated_at, origin_device, rev, "
            "display_name) VALUES ('s2', 1, 1, 'dev-a', 1, 'Dad')"
        )
        try:
            db.execute(
                "INSERT INTO today_card (id, created_at, updated_at, origin_device, rev, "
                "subject_id, card_type, size, sort_index, is_lead) "
                "VALUES ('c5', 1, 1, 'dev-a', 1, 's2', 'digest', 'small', 0, 1)"
            )
        except sqlite3.IntegrityError:
            self.fail(
                "a second subject could not have its own lead. The uniqueness is per "
                "subject, not across the notebook."
            )

        # A card outside the catalog does not exist, DESIGN.md 21.7, so the
        # database refuses one rather than rendering a blank where a card should
        # be.
        try:
            card("c6", "horoscope", 0, 4)
            self.fail(
                "today_card accepted a card type outside the seventeen in DESIGN.md "
                "21.7. Anything not in the catalog does not exist as a card."
            )
        except sqlite3.IntegrityError:
            pass

        db.close()

    # -- run ---------------------------------------------------------------

    def run(self):
        tables = self.user_tables()
        if not tables:
            self.fail("no user data tables found in contract/schema.sql")

        self.check_columns(tables)
        self.check_no_autoincrement(tables)
        self.check_views(tables)
        self.check_triggers(tables)
        self.check_foreign_keys(tables)
        self.check_event_dates(tables)
        self.check_local_tables()
        retention = self.check_retention_documented()
        self.check_change_log_behavior()
        self.check_same_transaction()
        self.check_lead_slot_is_singular()

        if self.failures:
            print(f"Schema check failed. {len(self.failures)} problems.\n")
            for failure in self.failures:
                print(f"  {failure}")
            print("\nSee contract/DATA-CONTRACT.md sections 3 and 4.")
            return 1

        print(
            f"Schema check passed. {len(tables)} user data tables, each with the six "
            f"required columns, a live view that filters tombstones, and both change "
            f"log triggers. No AUTOINCREMENT on any of them. Every foreign key points "
            f"at an id. Every event date is a full EDTF group. "
            f"Tombstone retention window: {retention} days."
        )
        print(
            "  Behavior verified: insert logs 'insert', update logs 'update', setting "
            "deleted_at logs 'delete', undelete logs 'update', the live view hides a "
            "tombstone while the base table keeps it, a failing log write rolls "
            "the data write back with it, and the Today lead slot refuses a second "
            "lead while allowing one per subject."
        )
        return 0


if __name__ == "__main__":
    sys.exit(SchemaCheck().run())
