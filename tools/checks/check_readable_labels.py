#!/usr/bin/env python3
"""Every table and column the archive renders has a word in all four languages.

`contract/DATA-CONTRACT.md` section 8.2:

> Faithful to the person's language. Written in the locale the person used the
> app in, with the correct `dir` attribute and correct RTL rendering for Arabic,
> verified in a browser and not assumed.

**Written after only the direction half was true.** An archive exported in
Arabic on 2026-08-09 carried `<html lang="ar" dir="rtl">` on all thirty five
pages and not one Arabic word of its own: `ReadableArchive` held forty table
names and sixty column labels as hard-coded English maps. The page mirrored
correctly and every heading on it was English, which is the failure that looks
most like success. #327.

**So the labels moved into the catalogs, and that is what makes this check
necessary rather than optional.** The keys are built from a variable,
`archive.field.${column}`, and `docs/TRAPS.md` section 3 is explicit that a key
built that way is checked by nothing: `check_string_keys.py` skips dynamic keys
by design, and `check_i18n.py` only holds the four catalogs to each other, so
all four agreeing that a key is absent passes both. The stated safety net for a
dynamic key is the instrumented suite, and the instrumented suite cannot run on
a day the phone is unreachable.

**What this reads is the same file that decides what gets rendered at all.**
`contract/readable-fields.json` names every column's fate, `ReadableWords`
derives the label set from it, and this holds that derived set to the catalogs.
There is no third list to keep in step, which is D16.

**A missing label is not a crash and that is the point.** `ReadableArchive`
falls back to the column name with its underscores opened out, so the page still
renders and still looks finished, in English, in an Arabic archive. Silence is
the whole failure mode here.

Exit 0 when clean, 1 with every problem listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIELD_MAP = ROOT / "contract/readable-fields.json"
I18N = ROOT / "contract/i18n"

LOCALES = ["en", "es", "zh", "ar"]

# A zone is rendered as part of its own date and never on its own, so it never
# shows a label. `ReadableWords` filters the same decision out.
ZONE = "dateZone"

# What the archive says on its own behalf, beyond the table and column names.
# Listed rather than derived because prose has no schema to derive it from, and
# a page that says "not recorded" in English in an Arabic archive is the same
# defect as a heading that does.
PAGE_KEYS = [
    "archive.page.subject.fallback",
    "archive.page.about",
    "archive.page.covers.one",
    "archive.page.covers.range",
    "archive.page.dated.heading",
    "archive.page.whole.heading",
    "archive.page.howto.heading",
    "archive.page.howto.body",
    "archive.page.back",
    "archive.page.year.title",
    "archive.page.year.undated",
    "archive.page.records",
    "archive.value.not_recorded",
    "archive.value.yes",
    "archive.value.no",
]


def rendered(field_map):
    """The tables and columns that actually reach a page, from the field map."""
    tables, columns = [], []
    for table, spec in sorted(field_map.items()):
        decisions = [
            (column, spec["columns"].get(column, {}).get("render"))
            for column in spec.get("order", [])
        ]
        shown = [(column, decision) for column, decision in decisions if decision]
        if not shown:
            continue
        tables.append(table)
        for column, decision in shown:
            if decision != ZONE and column not in columns:
                columns.append(column)
    return tables, sorted(columns)


def main() -> int:
    if not FIELD_MAP.is_file():
        print("Readable label check skipped: contract/readable-fields.json does not exist yet.")
        return 0

    field_map = json.loads(FIELD_MAP.read_text(encoding="utf-8"))
    tables, columns = rendered(field_map)

    required = (
        [f"archive.table.{table}" for table in tables]
        + [f"archive.field.{column}" for column in columns]
        + PAGE_KEYS
    )

    problems: list[str] = []
    catalogs = {}
    for code in LOCALES:
        path = I18N / f"{code}.json"
        if not path.is_file():
            problems.append(f"{code}.json is missing. All four locales ship in v1.")
            continue
        catalogs[code] = json.loads(path.read_text(encoding="utf-8"))

    for code, catalog in catalogs.items():
        for key in required:
            if key not in catalog:
                problems.append(
                    f"{code}.json has no {key!r}. The archive falls back to the "
                    f"column name opened out, so the page renders in English and "
                    f"looks finished."
                )
            elif not str(catalog[key]).strip():
                problems.append(f"{code}.json has {key!r} empty, which renders as a blank label.")

    # A label nothing renders is a translation four people maintain for no
    # reader. Reported so the set stays exactly the set that reaches a page.
    known = set(required)
    for code, catalog in catalogs.items():
        for key in sorted(catalog):
            if key.startswith("archive.") and key not in known:
                problems.append(
                    f"{code}.json has {key!r}, which nothing in the readable copy "
                    f"renders. Either it is spelled wrong or the column it names "
                    f"is gone."
                )

    if problems:
        print(f"Readable label check failed. {len(problems)} problems.\n")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(
        f"Readable label check passed. {len(tables)} tables, {len(columns)} columns "
        f"and {len(PAGE_KEYS)} page strings have a word in all {len(LOCALES)} languages."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
