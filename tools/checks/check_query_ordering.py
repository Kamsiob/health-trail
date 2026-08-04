#!/usr/bin/env python3
"""Every query that feeds a render or an export orders itself explicitly.

`contract/DATA-CONTRACT.md` section 8.4, the failure mode named "Ordering":

> Every query that feeds an export or a render has an explicit `ORDER BY` on
> stable columns. **Never depend on rowid, insertion order, or default iteration
> order.**

**Why this is a check and not a review note.** SQLite will happily return rows in
a consistent order for years and then change it: after a vacuum, after an index
is added, after a version upgrade, on a different device. Nothing about the app
looks different on the day it changes. What changes is that two exports of one
unchanged database stop being identical, which is exactly what 8.5's
regeneration test asserts, so the test starts failing **intermittently** rather
than never. An intermittent failure on the archive path is worse than a broken
one: it teaches whoever sees it to run the suite again.

**What counts as exempt, and why each one is safe.**

- A query using an aggregate over the whole result. One row comes back.
- A query selecting by primary key or by a unique key. One row comes back.
- A query whose caller sorts the result itself, which has to say so with the
  marker below, because it is the one case this cannot see.

    // unordered-query: <reason>

**What it cannot check.** That the columns being ordered on are stable ones. A
query ordering by a column the person can edit is still deterministic on any one
day and reorders itself when they fix a typo. That needs a person, and the
existing queries say in their own comments why they order the way they do.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPOSITORY = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data/Repository.kt"

MARKER = "unordered-query:"

# One row comes back, so there is no order to fix.
AGGREGATE = re.compile(r"\b(?:COUNT|SUM|MAX|MIN|AVG|TOTAL)\s*\(", re.I)
BY_KEY = re.compile(r"WHERE\s+[\w.]*\bid\s*=\s*\?|WHERE\s+key\s*=\s*\?", re.I)


def queries(source: str) -> list[tuple[int, str, str]]:
    """Each `rawQuery` call as (line, the SQL, the source text of the call)."""
    found = []
    for match in re.finditer(r"rawQuery\(", source):
        # Walk the argument list to its closing parenthesis so a comment or a
        # line break inside the concatenation does not end the match early. The
        # first version of this used a regex over string literals and stopped at
        # the first comment, which silently exempted every query whose ORDER BY
        # was explained in one.
        depth = 0
        end = match.end()
        while end < len(source):
            if source[end] == "(":
                depth += 1
            elif source[end] == ")":
                if depth == 0:
                    break
                depth -= 1
            end += 1
        call = source[match.end():end]
        sql = " ".join(" ".join(re.findall(r'"([^"]*)"', call)).split())
        found.append((source[: match.start()].count("\n") + 1, sql, call))
    return found


def main() -> int:
    source = REPOSITORY.read_text(encoding="utf-8")
    lines = source.splitlines()

    problems = []
    checked = 0
    for line, sql, call in queries(source):
        if not sql.upper().lstrip().startswith("SELECT"):
            continue
        checked += 1
        if "ORDER BY" in sql.upper():
            continue
        if AGGREGATE.search(sql) or BY_KEY.search(sql):
            continue
        # The caller sorts it, and said so.
        window = "\n".join(lines[max(0, line - 6): line + len(call.splitlines()) + 2])
        if MARKER in window:
            continue
        problems.append((line, sql))

    if problems:
        print("Query ordering check failed.")
        print()
        for line, sql in problems:
            print(f"  Repository.kt:{line}")
            print(f"    {sql[:100]}")
        print()
        print(
            "contract/DATA-CONTRACT.md 8.4. A query without an explicit ORDER BY is\n"
            "consistent until it is not: after a vacuum, an index, a version upgrade,\n"
            "or on another device. Two exports of one unchanged database then stop\n"
            "being identical, and 8.5's regeneration test starts failing\n"
            f"intermittently rather than never. If the caller sorts it, say so with\n"
            f"a `{MARKER} <reason>` comment beside the query."
        )
        return 1

    print(
        f"Query ordering check passed. {checked} queries feed a render or an "
        "export and every one of them orders itself."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
