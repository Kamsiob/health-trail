#!/usr/bin/env python3
"""No sentence a person reads is glued together out of pieces in Kotlin.

**#13's fourth criterion, which said "enforced by a check rather than by
review" and had no check.** The rule itself is older than this file and is the
architectural half of the four locale scaffold: a sentence assembled by
concatenation is an English sentence with the other languages' word order,
separators and spacing designed by accident, and in Arabic the pieces also run
in the wrong direction.

**What it looks for.** A catalog lookup joined to anything else with `+`, and a
string template holding two of them. Both are the same mistake in two costumes.
The catalog's own placeholders are the answer: one key, one sentence, and the
parts handed to it as named arguments, which is what `strings("key", "name" to
value)` already does everywhere else.

**What it deliberately does not touch.** `Bidi.join`, which is the opposite of
this defect: it takes parts that genuinely are separate runs, a date beside a
name, and isolates each so a reader can tell them apart. And ordinary Kotlin
string building that never reaches a screen, since the rule is about what a
person reads.

**One violation existed when this was written**, a content description on the
money screen reading label, comma, amount. Fixed in the same commit.

Kamsiob, AGPL-3.0.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCES = ROOT / "android/app/src/main/kotlin"

# `strings["x"] +` or `+ strings["x"]`, and the same for the call form.
GLUED = re.compile(
    r"(strings\s*[\[(][^\]\)]*[\]\)]\s*\+)|(\+\s*strings\s*[\[(])",
)

# Two catalog lookups inside one string template, which is the same glue with
# the plus signs hidden.
TEMPLATED = re.compile(r'"[^"\n]*\$\{?strings[^"\n]*\$\{?strings[^"\n]*"')

# A date or time pattern written into Kotlin instead of taken from the catalog.
#
# **The same defect as gluing a sentence, and it hides better.** `RestoreScreen`
# rendered "the day the file was made, in the reader's own locale" with a
# hardcoded `yyyy-MM-dd HH:mm`: not the locale's order, not its month name, and
# a 24 hour clock in an app whose every other English time is 12 hour. Its own
# doc comment claimed the opposite. The catalog holds `date.format.*` for
# exactly this, and a pattern in the code cannot follow the language.
#
# A filename stamp is not a sentence and is allowed to be sortable, so it says
# so on the line.
DATE_PATTERN = re.compile(r'ofPattern\(\s*"[^"]*(?:yyyy|MMMM|MMM|HH|hh|mm|EEEE)[^"]*"')

# **Said on the line, so the reason travels with the code.** The same shape the
# bidi check uses, per D139: an allowlist of line numbers rots on the next edit
# and nobody reading the screen ever sees it.
ALLOW = "// concat-ok:"


def main() -> int:
    if not SOURCES.is_dir():
        print("Concatenation check skipped: the sources are not here.")
        return 0

    problems = []
    scanned = 0
    for path in sorted(SOURCES.rglob("*.kt")):
        scanned += 1
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if ALLOW in line:
                continue
            if GLUED.search(line) or TEMPLATED.search(line):
                problems.append(
                    f"{path.relative_to(ROOT)}:{number}: a sentence a person "
                    f"reads is being assembled here rather than composed from "
                    f"one catalog key. Add the pieces as placeholders on a "
                    f"single key. If this genuinely is not a sentence, say so "
                    f"with '{ALLOW} <reason>' on the line."
                )
            if DATE_PATTERN.search(line):
                problems.append(
                    f"{path.relative_to(ROOT)}:{number}: a date or time pattern "
                    f"is written here rather than read from the catalog's "
                    f"`date.format.*` keys, so it cannot follow the language "
                    f"and it cannot follow the 12 hour clock. If this is a "
                    f"filename stamp rather than something a person reads, say "
                    f"so with '{ALLOW} <reason>' on the line."
                )

    if problems:
        print(f"Concatenation check failed. {len(problems)} place(s).")
        for problem in problems:
            print(f"  {problem}")
        print(
            "\nThe rule is #13 and contract/DATA-CONTRACT.md's message "
            "templates: word order, separators and spacing are the catalog's "
            "to decide, per language, not Kotlin's."
        )
        return 1

    print(
        f"Concatenation check passed. {scanned} source files, no sentence "
        f"assembled from pieces outside the catalog."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
