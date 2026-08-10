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
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIELD_MAP = ROOT / "contract/readable-fields.json"
VOCABULARIES = ROOT / "contract/readable-vocabularies.json"
SCHEMA = ROOT / "contract/schema.sql"
I18N = ROOT / "contract/i18n"
TEMPLATE_DATA = ROOT / "templates" / "data"

LOCALES = ["en", "es", "zh", "ar"]

# Drawn inside another field rather than on its own, so neither ever shows a
# label. `ReadableWords` filters the same two decisions out.
RENDERED_INSIDE_ANOTHER = {"dateZone", "moneyCurrency"}

# Every decision `ReadableArchive.renderField` knows.
#
# **Held here because the renderer's last branch is `else`.** A decision spelled
# wrong does not fail: it falls through to the plain value and prints the column
# contents, which is the whole of #328 arriving again by typo.
DECISIONS = {
    "id", "date", "dateZone", "timestamp", "boolean", "attachment", "link",
    "money", "moneyCurrency", "enum", "tableName", "value",
}

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
            if decision not in RENDERED_INSIDE_ANOTHER and column not in columns:
                columns.append(column)
    return tables, sorted(columns)


def vocabulary_problems(field_map, vocabularies, schema, catalogs):
    """Every stored value that would reach a page as itself.

    **Three separate ways this goes wrong**, and the third is the one nothing
    else could see.

    A column declared `enum` naming a vocabulary that does not exist renders the
    raw token, because the renderer falls back to it rather than throwing.

    A declared value with no word in one of the four catalogs renders the raw
    token in that language only, which is the #327 shape again.

    And **a value the schema allows that the vocabulary does not list** is the
    quiet one: everything above passes, the build is green, and the first row
    somebody writes with that value prints `waiting_on_insurance` in a document
    a family reads. So where the schema constrains the column, its CHECK is the
    authority and this holds the declaration to it.
    """
    problems = []

    used = {}
    for table, spec in sorted(field_map.items()):
        for column in spec.get("order", []):
            decision = spec["columns"].get(column, {})
            if decision.get("render") != "enum":
                continue
            name = decision.get("vocabulary")
            if not name:
                problems.append(
                    f"readable-fields.json: {table}.{column} is an enum with no "
                    f"vocabulary, so its value renders as the column contents."
                )
                continue
            if name not in vocabularies:
                problems.append(
                    f"readable-fields.json: {table}.{column} names the vocabulary "
                    f"{name!r}, which readable-vocabularies.json does not declare."
                )
                continue
            used.setdefault(name, []).append((table, column))

    for name in sorted(vocabularies):
        if name not in used:
            problems.append(
                f"readable-vocabularies.json declares {name!r}, which no column in "
                f"readable-fields.json renders. Either it is spelled wrong or the "
                f"column it was for is gone."
            )

    for name, values in sorted(vocabularies.items()):
        if not values:
            problems.append(f"readable-vocabularies.json: {name!r} declares no values.")
        for code, catalog in catalogs.items():
            for value in values:
                key = f"archive.vocabulary.{name}.{value}"
                if key not in catalog:
                    problems.append(
                        f"{code}.json has no {key!r}. The archive falls back to the "
                        f"stored value, so the page prints {value!r} in a document "
                        f"somebody reads."
                    )
                elif not str(catalog[key]).strip():
                    problems.append(f"{code}.json has {key!r} empty.")

        # The schema is the authority wherever it constrains the column.
        for table, column in used.get(name, []):
            allowed = check_values(schema, table, column)
            if allowed is None:
                continue
            missing = [v for v in allowed if v not in values]
            extra = [v for v in values if v not in allowed]
            for value in missing:
                problems.append(
                    f"schema.sql allows {table}.{column} = {value!r} and the {name!r} "
                    f"vocabulary does not list it, so the first row written with it "
                    f"prints the stored value."
                )
            for value in extra:
                problems.append(
                    f"the {name!r} vocabulary lists {value!r}, which the CHECK on "
                    f"{table}.{column} does not allow. It is four translations "
                    f"nothing can ever render."
                )

    # Any archive.vocabulary.* key nothing declares.
    known = {
        f"archive.vocabulary.{name}.{value}"
        for name, values in vocabularies.items()
        for value in values
    }
    for code, catalog in catalogs.items():
        for key in sorted(catalog):
            if key.startswith("archive.vocabulary.") and key not in known:
                problems.append(
                    f"{code}.json has {key!r}, which no declared vocabulary contains."
                )
    return problems


def catalog_problems(field_map):
    """A link into a shipped catalog names one that exists and answers uniquely.

    **The failure this exists for prints a confident wrong name**, which is worse
    than the identifier it replaced. `discharge_planning` is both a care thread
    and a project template and `dietary` is both a thread and a standing
    instruction, so a lookup that searched every catalog would answer, and answer
    wrongly, on a page nobody reads until it matters. #329.

    Three things are held: the catalog a column names exists, its ids are unique
    inside it, and no catalog is declared that nothing uses.
    """
    problems = []

    entries = {}
    if TEMPLATE_DATA.is_dir():
        situations = json.loads(
            (TEMPLATE_DATA / "situations.json").read_text(encoding="utf-8")
        )["templates"]
        projects = json.loads(
            (TEMPLATE_DATA / "projects.json").read_text(encoding="utf-8")
        )["templates"]
        progress = json.loads(
            (TEMPLATE_DATA / "progress-and-instructions.json").read_text(encoding="utf-8")
        )
        entries = {
            "situations": [x["id"] for x in situations],
            "projects": [x["id"] for x in projects],
            "presets": [x["id"] for x in progress["progress_presets"]],
            "instructions": [x["id"] for x in progress["standing_instructions"]],
        }

    if not entries:
        return ["templates/data is missing, so the readable copy's catalogs cannot be checked."]

    for name, ids in sorted(entries.items()):
        duplicates = sorted({i for i in ids if ids.count(i) > 1})
        if duplicates:
            problems.append(
                f"the {name} catalog has more than one entry with the same id: "
                f"{', '.join(duplicates)}. The readable copy resolves a link by id "
                f"inside one catalog, so a duplicate renders whichever came first."
            )

    used = set()
    for table, spec in sorted(field_map.items()):
        for column, decision in sorted(spec.get("columns", {}).items()):
            name = decision.get("catalog")
            if not name:
                continue
            if decision.get("render") != "link":
                problems.append(
                    f"readable-fields.json: {table}.{column} names a catalog but its "
                    f"render is {decision.get('render')!r}. Only `link` consults one."
                )
                continue
            if name not in entries:
                problems.append(
                    f"readable-fields.json: {table}.{column} names the catalog {name!r}, "
                    f"which templates/data does not have. The page would fall back to "
                    f"printing the identifier and nothing would fail."
                )
                continue
            used.add(name)

    return problems


def raw_number_problems(field_map, schema):
    """Columns whose type says the plain value cannot be what a reader should see.

    **Written because three of these were found by grepping a real archive for
    long integers, not by reading anything.** `entry.pinned_at` and five like it
    printed epoch milliseconds on a page, which `contract/DATA-CONTRACT.md` 8.2
    forbids in as many words and which `ReadableDate.timestamp` was written for
    and nothing called. `call_detail.reached` printed 0 and 1, which is the
    sentence with no meaning outside a database that the `boolean` decision
    already existed to stop.

    **The type is the tell and the schema already carries it.** An INTEGER
    column constrained to 0 and 1 is a flag whatever it is called, and an
    INTEGER column named `_at` or `_since` is a time. Both are provable from
    `contract/schema.sql`, so neither has to be noticed by a person again.
    """
    problems = []
    tables = dict(re.findall(
        r"CREATE TABLE IF NOT EXISTS (\w+) \((.*?)\n\);", schema, re.S,
    ))
    for table, spec in sorted(field_map.items()):
        body = tables.get(table, "")
        for column in spec.get("order", []):
            if spec["columns"].get(column, {}).get("render") != "value":
                continue
            if not re.search(rf"^\s*{column}\s+INTEGER", body, re.M):
                continue
            if re.search(
                rf"^\s*{column}\s+INTEGER[^,]*CHECK \({column} IN \(0, 1\)\)", body, re.M,
            ):
                problems.append(
                    f"{table}.{column} is an INTEGER flag rendered as a plain value, so "
                    f"the page prints 0 or 1. Render it as a boolean, which says yes or "
                    f"no and keeps a flag nobody set apart from one somebody cleared."
                )
            elif column.endswith("_at") or column.endswith("_since"):
                problems.append(
                    f"{table}.{column} is epoch milliseconds rendered as a plain value, "
                    f"so the page prints a number like 1781701200000. "
                    f"contract/DATA-CONTRACT.md 8.2: never a bare epoch number. Render "
                    f"it as a timestamp."
                )
    return problems


def check_values(schema, table, column):
    """The values a CHECK constraint allows, or None where there is no CHECK."""
    body = re.search(
        rf"CREATE TABLE IF NOT EXISTS {table} \((.*?)\n\);", schema, re.S,
    )
    if not body:
        return None
    found = re.search(
        rf"{column}\s+TEXT[^,]*?CHECK \({column} IN \(([^)]*)\)", body.group(1), re.S,
    )
    return re.findall(r"'([^']+)'", found.group(1)) if found else None


def main() -> int:
    if not FIELD_MAP.is_file():
        print("Readable label check skipped: contract/readable-fields.json does not exist yet.")
        return 0

    field_map = json.loads(FIELD_MAP.read_text(encoding="utf-8"))
    vocabularies = {
        name: spec["values"]
        for name, spec in json.loads(VOCABULARIES.read_text(encoding="utf-8")).items()
        if not name.startswith("_")
    }
    schema = SCHEMA.read_text(encoding="utf-8")
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

    for table, spec in sorted(field_map.items()):
        for column, decision in sorted(spec.get("columns", {}).items()):
            render = decision.get("render")
            if render and render not in DECISIONS:
                problems.append(
                    f"readable-fields.json: {table}.{column} says render {render!r}, "
                    f"which the renderer does not know. Its last branch is `else`, so "
                    f"the column would print its stored contents and nothing would fail."
                )

    problems.extend(vocabulary_problems(field_map, vocabularies, schema, catalogs))
    problems.extend(catalog_problems(field_map))
    problems.extend(raw_number_problems(field_map, schema))

    # A label nothing renders is a translation four people maintain for no
    # reader. Reported so the set stays exactly the set that reaches a page.
    # The vocabulary keys have their own accounting above.
    known = set(required)
    for code, catalog in catalogs.items():
        for key in sorted(catalog):
            if key.startswith("archive.vocabulary."):
                continue
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

    values = sum(len(v) for v in vocabularies.values())
    print(
        f"Readable label check passed. {len(tables)} tables, {len(columns)} columns, "
        f"{len(PAGE_KEYS)} page strings and {values} stored values across "
        f"{len(vocabularies)} vocabularies have a word in all {len(LOCALES)} languages, "
        f"and every vocabulary matches the CHECK constraint behind it."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
