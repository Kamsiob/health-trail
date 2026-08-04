#!/usr/bin/env python3
"""Every catalog key the app asks for exists, so no screen can crash on opening.

**Written after a screen crashed on the phone the first time it was opened.**
`ChangeSituationScreen` asked for `more.title`, which has never existed, and
`Strings.resolve` throws on a key it does not have. That is the right behavior:
a silent fallback would ship a screen with a hole in it, and `check_i18n.py`
exists precisely to stop half translated screens from looking finished.

But nothing caught it before the device. The four catalogs agreed with each
other, every compliance check passed, the Kotlin compiled, and lint was clean,
because a catalog key is a string literal and nothing was comparing the literals
in the code against the catalog. The screen was reachable for exactly one tap
before it took the app down.

**So this reads the other direction**: every key the Kotlin asks for, against the
English catalog, which `check_i18n.py` already holds the other three to. Between
the two, a key exists in all four or the build fails.

**Only literals, deliberately.** A key built at runtime, such as
`"projects.status.${project.status}"` or `kindNameKey(entry.kind)`, cannot be
resolved by reading the source and is skipped rather than guessed at. Those are
covered by the instrumented suite, which renders the screens. What this catches
is the typo and the invented key, which is the whole of what went wrong.

Exit 0 when clean, 1 with every problem listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "contract" / "i18n" / "en.json"
SOURCES = ROOT / "android" / "app" / "src" / "main" / "kotlin"

# A key is lowercase words joined by dots. Anything with a `$` in it is built at
# runtime and is skipped by construction, because the pattern cannot match it.
KEY = r'"([a-z][a-z0-9]*(?:\.[a-z0-9_]+)+)"'

# The three ways the app asks for one. `strings[...]` and `strings(...)` are the
# catalog's own accessors; the `*Key =` parameters are the components that take
# a key rather than a rendered string, which exist so a component can uppercase
# or pluralize in the locale's own rules.
PATTERNS = [
    re.compile(r"strings\s*\[\s*" + KEY + r"\s*\]"),
    re.compile(r"strings\s*\(\s*" + KEY),
    re.compile(r"\b\w*[Kk]ey\s*=\s*" + KEY),
]


def main() -> int:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    problems: list[str] = []

    files = sorted(SOURCES.rglob("*.kt"))
    asked = 0
    for path in files:
        text = path.read_text(encoding="utf-8")
        # Line by line, so a failure names the line somebody has to open.
        for number, line in enumerate(text.splitlines(), start=1):
            for pattern in PATTERNS:
                for match in pattern.finditer(line):
                    key = match.group(1)
                    asked += 1
                    if key not in catalog:
                        problems.append(
                            f"{path.relative_to(ROOT)}:{number} asks for {key!r}, "
                            f"which is in no catalog. The screen throws the moment "
                            f"it is opened."
                        )

    if problems:
        print(f"String key check failed. {len(problems)} problems.\n")
        for problem in problems:
            print(f"  {problem}")
        print("\nAdd the key to all four catalogs in contract/i18n, or use the")
        print("one that already says this. See MASTER_SPEC.md section 7.")
        return 1

    print(
        f"String key check passed. {asked} catalog keys asked for across "
        f"{len(files)} Kotlin files, every one of them present."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
