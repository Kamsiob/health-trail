#!/usr/bin/env python3
"""Every column in the schema is either rendered in the readable copy or explicitly not.

`contract/DATA-CONTRACT.md` section 8.5, the coverage test:

> Enumerate every column in contract/schema.sql and assert each one appears in
> the readable renderer's field map. Adding a column without rendering it fails
> the build until it is either rendered or explicitly listed as not-for-rendering
> with a written reason. This is what keeps completeness true in year three
> rather than only on the day it was built.

**Why this exists rather than a reviewer remembering.** Section 8.2 requires the
readable copy to render every field stored in the database: it is not a summary,
not a highlight reel, and not a subset. That requirement is true on the day it is
written and quietly false six months later, the first time somebody adds a column
to `entry` and does not think about the archive. **The person who loses by that is
the one who exports after three years and finds a field missing**, and by then the
source device is wiped.

So the rule is enforced where it can actually be caught: at the moment the column
is added. D90.

**A not-rendered decision is legitimate and needs a reason, not permission.** Row
bookkeeping, sync metadata, and derived index columns genuinely should not appear
in a document a person hands to a doctor. What is not legitimate is silence: a
column with no decision recorded is a column nobody thought about.

The map is `contract/readable-fields.json`, which lives in `/contract` rather
than in `/android` because the web version renders the same archive from the same
decisions, and two copies of this would drift within weeks.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "contract/schema.sql"
FIELD_MAP = ROOT / "contract/readable-fields.json"

# A decision must say something. A one word reason is not a reason, and this is
# the length below which somebody has written "internal" and moved on.
MIN_REASON = 25


def schema_columns() -> dict[str, list[str]]:
    sql = re.sub(r"--[^\n]*", "", SCHEMA.read_text(encoding="utf-8"))
    tables: dict[str, list[str]] = {}
    for match in re.finditer(
        r"CREATE TABLE(?:\s+IF NOT EXISTS)?\s+(\w+)\s*\((.*?)\n\)\s*;", sql, re.S
    ):
        name, body = match.group(1), match.group(2)
        parts, depth, current = [], 0, ""
        for char in body:
            if char == "(":
                depth += 1
            if char == ")":
                depth -= 1
            if char == "," and depth == 0:
                parts.append(current)
                current = ""
            else:
                current += char
        parts.append(current)

        columns = []
        for part in parts:
            part = part.strip()
            if not part:
                continue
            first = part.split()[0].upper()
            # Table constraints are not columns.
            if first in ("PRIMARY", "FOREIGN", "UNIQUE", "CHECK", "CONSTRAINT"):
                continue
            columns.append(part.split()[0])
        tables[name] = columns
    return tables


def main() -> int:
    if not SCHEMA.is_file():
        print("Readable coverage check skipped: no schema yet.")
        return 0
    if not FIELD_MAP.is_file():
        print(f"Readable coverage check failed: {FIELD_MAP.relative_to(ROOT)} does not exist.")
        print("Every column needs a rendering decision. contract/DATA-CONTRACT.md 8.5.")
        return 1

    schema = schema_columns()
    field_map = json.loads(FIELD_MAP.read_text(encoding="utf-8"))

    missing_tables = sorted(set(schema) - set(field_map))
    extra_tables = sorted(set(field_map) - set(schema))
    missing_columns: list[str] = []
    extra_columns: list[str] = []
    undecided: list[str] = []
    thin_reasons: list[str] = []

    for table, columns in sorted(schema.items()):
        mapped = field_map.get(table, {}).get("columns", {})
        for column in columns:
            if column not in mapped:
                missing_columns.append(f"{table}.{column}")
                continue
            decision = mapped[column]
            has_render = "render" in decision
            reason = decision.get("notRendered")
            if not has_render and not reason:
                undecided.append(f"{table}.{column}")
            elif reason is not None and len(reason.strip()) < MIN_REASON:
                thin_reasons.append(f"{table}.{column}")
        for column in mapped:
            if column not in columns:
                extra_columns.append(f"{table}.{column}")

    problems = (
        missing_tables or extra_tables or missing_columns
        or extra_columns or undecided or thin_reasons
    )
    if problems:
        print("Readable coverage check failed.")
        print()

        def report(title: str, items: list[str], note: str) -> None:
            if not items:
                return
            print(f"  {title} ({len(items)}):")
            for item in items[:20]:
                print(f"    {item}")
            if len(items) > 20:
                print(f"    ... and {len(items) - 20} more")
            print(f"    {note}")
            print()

        report(
            "Tables in the schema with no rendering decision", missing_tables,
            "Add them to contract/readable-fields.json.",
        )
        report(
            "Tables in the field map that the schema no longer has", extra_tables,
            "Remove them, or the map is describing an app that does not exist.",
        )
        report(
            "Columns in the schema with no rendering decision", missing_columns,
            "Every column is rendered or explicitly not, with a reason. 8.5.",
        )
        report(
            "Columns in the field map the schema does not have", extra_columns,
            "The map has drifted ahead of or behind the schema.",
        )
        report(
            "Columns with neither a renderer nor a reason", undecided,
            "A column with no decision is a column nobody thought about.",
        )
        report(
            "Not-rendered decisions whose reason says nothing", thin_reasons,
            f"A reason is at least {MIN_REASON} characters of why, not a label.",
        )
        print(
            "contract/DATA-CONTRACT.md 8.2 and 8.5. The readable copy is not a\n"
            "summary and not a subset. A person who exports after three years and\n"
            "finds a field missing cannot get it back: the source device is wiped."
        )
        return 1

    total = sum(len(c) for c in schema.values())
    rendered = sum(
        1
        for table in schema
        for column, decision in field_map[table]["columns"].items()
        if "render" in decision
    )
    print(
        f"Readable coverage check passed. {len(schema)} tables, {total} columns, "
        f"{rendered} rendered and {total - rendered} explicitly not, every one with a reason."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
