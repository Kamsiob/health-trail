#!/usr/bin/env python3
"""`DESIGN.md` 5.1 and `Type.kt` say the same sizes, or this fails.

**This table has now been wrong twice, and both times it was the prose.**

On 2026-08-11 it said Display M was 19 to 20sp while the app had been 22sp all
along, so **a session building a new screen from the table would have set its
title two steps under every screen already shipped**. Display L and Display S
were missing from it entirely while being used across the app. That correction
is the point of D142.

On 2026-08-13 it still said the body was 13sp and the row title 13sp, months
after `rowTitle` had been written at 16 with a paragraph in the source
explaining why. **The source carried the reason and the document carried the
number, and they disagreed.**

**Why a check rather than care.** This table is not documentation, it is an
instruction: it is read by whoever is building a screen, and a stale row here
becomes a built screen tomorrow. Nothing else in the project notices, because a
screen built from the wrong row is internally consistent and looks fine on its
own. It is only wrong next to the twenty screens built from the right one.

**What this does not check.** Weight, tracking, and face, which `TypeTest`
covers where they matter, and whether the sizes are any good, which is a device
question and the owner's. It checks that two files agree, which is the failure
that has actually happened.

    python3 tools/checks/check_type_ladder.py

Exit 0 when they agree, 1 with every disagreement listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESIGN = ROOT / "DESIGN.md"
TYPE = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/theme/Type.kt"

# The ladder's own words on the left, the property they are implemented as on
# the right.
#
# **"Body" is `bodyM` and that is worth stating rather than inferring.** The
# ladder calls the floor "Body" and the code calls it `bodyM`, with `bodyL`
# above it for a screen's lead sentence. A mapping by name alone would pair
# "Body" with a `body` that does not exist and quietly check nothing.
ROLES = {
    "Hero": "hero",
    "Display L": "displayL",
    "Display M": "displayM",
    "Display S": "displayS",
    "Row title": "rowTitle",
    "Body L": "bodyL",
    "Body": "bodyM",
    "Body S": "bodyS",
    "Label": "label",
    "Nav label": "navLabel",
    "Mono": "mono",
    "Mono L": "monoL",
}

# `| Hero | Bricolage | **26sp** / 31 | 800, ... |`. The size may be bold and
# the line height is the number after the slash.
ROW = re.compile(
    r"^\|\s*([^|]+?)\s*\|[^|]*\|\s*\**(\d+)\s*sp\**\s*/\s*(\d+)\s*\|",
    re.MULTILINE,
)

# `hero = TextStyle(` ... `fontSize = 26.sp,` ... `lineHeight = 31.sp,`
STYLE = re.compile(
    r"(\w+)\s*=\s*TextStyle\((?:[^()]|\([^()]*\))*?"
    r"fontSize\s*=\s*(\d+)\.sp,(?:[^()]|\([^()]*\))*?"
    r"lineHeight\s*=\s*(\d+)\.sp,",
    re.DOTALL,
)


def ladder() -> dict[str, tuple[int, int]]:
    """The sizes `DESIGN.md` 5.1 states, by role."""
    text = DESIGN.read_text(encoding="utf-8")
    start = text.find("### 5.1")
    if start < 0:
        return {}
    end = text.find("\n### ", start + 1)
    section = text[start:end if end > 0 else len(text)]
    found = {}
    for role, size, line in ROW.findall(section):
        clean = role.replace("*", "").strip()
        if clean in ROLES:
            found[clean] = (int(size), int(line))
    return found


def implemented() -> dict[str, tuple[int, int]]:
    """The sizes `Type.kt` actually sets, by property."""
    text = TYPE.read_text(encoding="utf-8")
    return {
        name: (int(size), int(line))
        for name, size, line in STYLE.findall(text)
    }


def main() -> int:
    if not DESIGN.is_file() or not TYPE.is_file():
        print("Type ladder check skipped: DESIGN.md or Type.kt is not here.")
        return 0

    stated = ladder()
    built = implemented()

    problems = []
    for role, prop in ROLES.items():
        if role not in stated:
            problems.append(
                f"DESIGN.md 5.1 has no row for {role!r}, which is implemented as "
                f"{prop}. A role in the code and not in the table is how Display L "
                f"and Display S went unwritten while every screen used them."
            )
            continue
        if prop not in built:
            problems.append(
                f"Type.kt has no {prop} with a fontSize and a lineHeight, and "
                f"DESIGN.md 5.1 promises {role!r} at {stated[role][0]}sp."
            )
            continue
        if stated[role] != built[prop]:
            says, is_ = stated[role], built[prop]
            problems.append(
                f"{role!r}: DESIGN.md 5.1 says {says[0]}sp / {says[1]}, "
                f"Type.kt sets {prop} to {is_[0]}sp / {is_[1]}. "
                f"**The document is read by whoever builds the next screen**, so "
                f"whichever is right, they cannot both stay."
            )

    if problems:
        print(f"Type ladder check failed. {len(problems)} disagreement(s).")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(
        f"Type ladder check passed. {len(ROLES)} roles: DESIGN.md 5.1 and "
        f"Type.kt state the same size and line height for every one."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
