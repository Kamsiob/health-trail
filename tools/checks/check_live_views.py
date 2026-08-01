#!/usr/bin/env python3
"""Fail the build on a raw user data table query outside the repository layer.

The data contract's first named failure is a forgotten `deleted_at IS NULL`.
It will be forgotten exactly once, quietly, and the symptom is a deleted entry
turning up in a search result, a chart, or an export months later, in a record
somebody may be relying on.

Views make the right thing easy. This makes the wrong thing hard: reading a
base table is only allowed inside the repository, and only where the code says
out loud that it is doing something a live view cannot.

Two escape hatches exist and both are explicit, because the contract names two
operations that must see tombstones:

  - a full data wipe, which deletes everything including tombstones
  - the tombstone purge, which removes tombstones past the retention window

Anything else that needs a base table writes `// allow-base-table: <reason>` on
the line or in the comment directly above it, and the reason is read by a person in review rather than by this
script. **The point is that it cannot happen silently**, not that it lives in
one directory: a test proving the wrong passphrase cannot open the database
legitimately reads a base table and is not repository code.

Exit 0 when clean, 1 with a list of failures otherwise.

Kamsiob, AGPL-3.0.
"""

import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "contract" / "schema.sql"

# Where a base table read is allowed at all. Everywhere else in the app, a
# query naming one is a defect regardless of what it says.
REPOSITORY = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data"

# Kotlin the check reads. Test sources are deliberately included: a test that
# reaches around the repository proves the repository rather than the app, and
# it is how the next person learns the wrong pattern.
SOURCES = [
    ROOT / "android/app/src/main/kotlin",
    ROOT / "android/app/src/androidTest/kotlin",
    ROOT / "android/app/src/test/kotlin",
]

ALLOW = "allow-base-table:"

# The two operations the contract says may see tombstones. Named here so the
# allowance is a short list somebody can read rather than a habit.
CONTRACT_EXCEPTIONS = ("full data wipe", "tombstone purge")


def user_tables():
    """Every table carrying a tombstone, read from the schema rather than listed."""
    db = sqlite3.connect(":memory:")
    db.executescript(SCHEMA.read_text(encoding="utf-8"))
    found = set()
    for (name,) in db.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
    ):
        columns = {row[1] for row in db.execute(f"PRAGMA table_info({name})")}
        if "deleted_at" in columns:
            found.add(name)
    return found


def main():
    tables = user_tables()
    if not tables:
        print("No user data tables found in the schema. That is itself wrong.")
        return 1

    # **Reads only.** FROM and JOIN, not INSERT or UPDATE.
    #
    # A write has to name the base table: there is nothing to insert into a
    # view, and an update through one would be a different feature. The leak
    # this check exists to stop is a read that forgets `deleted_at IS NULL` and
    # hands back something the person deleted.
    #
    # An update that touches a tombstoned row is a real and separate worry, and
    # it is not this. The change log triggers make such a write visible, and the
    # repository is the only place that writes at all.
    #
    # `live_entry` does not match, because the boundary is on the whole word and
    # `live_` is part of it.
    pattern = re.compile(
        r"\b(?:FROM|JOIN)\s+(%s)\b" % "|".join(sorted(tables)),
        re.IGNORECASE,
    )

    failures = []
    checked = 0

    for root in SOURCES:
        if not root.is_dir():
            continue
        for path in sorted(root.rglob("*.kt")):
            checked += 1
            inside_repository = REPOSITORY in path.parents
            lines = path.read_text(encoding="utf-8").splitlines()
            for number, line in enumerate(lines, 1):
                match = pattern.search(line)
                if not match:
                    continue
                table = match.group(1)
                where = path.relative_to(ROOT)

                # On the line, or in the comment block directly above it. A
                # query long enough to need this is usually long enough to have
                # no room left on the line, and a reason worth writing rarely
                # fits after eighty columns of SQL anyway.
                above = []
                back = number - 2
                while back >= 0 and lines[back].strip().startswith("//"):
                    above.append(lines[back])
                    back -= 1
                context = "\n".join([line] + above)

                if ALLOW in context:
                    # **The allowance is honored anywhere, and it must carry a
                    # reason.** Restricting it to the repository was tried and
                    # was the check inventing a rule the contract does not have:
                    # a test proving the wrong passphrase cannot open the
                    # database legitimately reads a base table, and it is not
                    # repository code. What the contract asks is that a base
                    # table read is never silent, not that it lives in one
                    # directory.
                    reason = context.split(ALLOW, 1)[1].strip()
                    if not reason:
                        failures.append(
                            f"{where}:{number}: allows a base table read with no reason. "
                            f"Write why, so the next reader does not have to guess."
                        )
                    continue

                if inside_repository:
                    failures.append(
                        f"{where}:{number}: queries the base table {table!r}. "
                        f"Read live_{table} instead, which filters tombstones by "
                        f"construction. If this genuinely must see deleted rows, say "
                        f"so with '// {ALLOW} <reason>' on the line."
                    )
                else:
                    failures.append(
                        f"{where}:{number}: queries the base table {table!r} outside "
                        f"the repository. Every read goes through live_{table}, and "
                        f"one forgotten 'deleted_at IS NULL' is a data leak of "
                        f"something the person believed they deleted. If this "
                        f"genuinely must see the base table, say so with "
                        f"'// {ALLOW} <reason>' on the line."
                    )

    if failures:
        print(f"Live view check failed. {len(failures)} problems.\n")
        for failure in failures:
            print(f"  {failure}")
        print(
            "\nSee contract/DATA-CONTRACT.md section 3. The two operations allowed to "
            f"see tombstones are the {CONTRACT_EXCEPTIONS[0]} and the "
            f"{CONTRACT_EXCEPTIONS[1]}."
        )
        return 1

    print(
        f"Live view check passed. {checked} Kotlin files, {len(tables)} tombstoned "
        f"tables, no base table read outside a live view."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
