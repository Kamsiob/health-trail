#!/usr/bin/env python3
"""Every table the digest maps is one the change log can actually write.

**Written after the mapping was wrong for the whole life of the project.**
`Digest.sectionOf` said:

    "reading" -> Repository.Section.PROGRESS

and there has never been a table called `reading`. The schema has `measure` and
`measurement`, and the change log triggers write exactly those two names. So
every reading anybody recorded fell through to the `else` branch and was left
out, and the Today digest reported a quiet week for a week spent taking
measurements. **It failed in the direction that looks like calm**, which is why
nobody saw it. #336.

**Nothing caught it, and the reason is the point of this file.** `DigestTest`
had a case that walked the mapping and asserted every table resolved to a
section. It walked a **hard-coded list in the test** that also said `reading`,
so it asserted the mapping the code has rather than the mapping the schema has.
Two copies of one mistake agree with each other forever.

This is `docs/RUN-LOG.md`'s "a list of table names in code is a list nothing
checks", which cost every bill and document a null date once already. The fix is
the same one: **hold the whole set to the file that generates it.**

**The authority is the change log's own literals.** A `change_log` row's `table`
column is written by a trigger, as `VALUES ('<table>', NEW.id, ...`, so the set
of names a change row can ever carry is exactly the set of those literals. A
name in the mapping that is not one of them can never match anything.

**The reverse is not an error and is not checked.** Plenty of tables are
deliberately unmapped: bookkeeping, join tables, and anything the person did not
put anywhere. `Digest.sectionOf` returns null for those on purpose and its own
comment says why.

Exit 0 when clean, 1 with every problem listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "contract/schema.sql"
DIGEST = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data/Digest.kt"

# `VALUES ('entry', NEW.id, 'insert', ...` in a change log trigger.
LOGGED = re.compile(r"VALUES\s*\(\s*'([a-z_]+)'\s*,\s*NEW\.id")

# `"entry" -> Repository.Section.TRAIL`, and the several-names-one-section form
# `"emergency_card", "emergency_contact" -> Repository.Section.EMERGENCY_CARD`.
BRANCH = re.compile(r'^\s*((?:"[a-z_]+"\s*,\s*)*"[a-z_]+")\s*->\s*Repository\.Section\.(\w+)')
NAME = re.compile(r'"([a-z_]+)"')


def logged_tables(schema: str) -> set[str]:
    return set(LOGGED.findall(schema))


def mapped_tables(source: str) -> dict[str, str]:
    """Table name to the section it maps to, read out of the `when`."""
    out = {}
    for line in source.splitlines():
        found = BRANCH.match(line)
        if not found:
            continue
        for table in NAME.findall(found.group(1)):
            out[table] = found.group(2)
    return out


def main() -> int:
    if not DIGEST.is_file():
        print("Digest section check skipped: Digest.kt does not exist yet.")
        return 0

    schema = SCHEMA.read_text(encoding="utf-8")
    source = DIGEST.read_text(encoding="utf-8")

    logged = logged_tables(schema)
    mapped = mapped_tables(source)

    problems = []

    if not logged:
        problems.append(
            "no change log triggers were found in contract/schema.sql, so this check "
            "would pass by having nothing to compare against."
        )
    if not mapped:
        problems.append(
            "no section mapping was found in Digest.kt, so this check would pass by "
            "having nothing to compare against."
        )

    for table, section in sorted(mapped.items()):
        if table not in logged:
            problems.append(
                f"Digest.sectionOf maps {table!r} to {section}, and no change log trigger "
                f"ever writes that name. Every row of it would fall through to the else "
                f"branch and be left out, and the digest would report a quiet week."
            )

    if problems:
        print(f"Digest section check failed. {len(problems)} problem(s).")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(
        f"Digest section check passed. All {len(mapped)} tables the digest maps are "
        f"names the change log actually writes, out of {len(logged)} logged tables."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
