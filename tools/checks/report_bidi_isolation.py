#!/usr/bin/env python3
"""Where the person's own words reach a screen without being isolated.

**A report, not a gate, and the name says which.** Every other file in this
folder is a check that fails a build. This one prints a worklist, because it
cannot tell a real defect from a false positive and a gate that cries wolf is
one somebody learns to run with `|| true`.

**What the defect is.** Text a person typed, rendered inside a layout that has a
direction, is a bidirectional run of its own. Without an isolate the Unicode
algorithm lays it out against the surrounding direction: on the emergency card
in Arabic, a dose of "500 mg, twice a day" rendered as "mg, twice a day 500",
and an allergy ending in a period had the period moved to the front. Every one
of these reads correctly in English, which is why three separate passes found it
three times rather than once.

`DESIGN.md` section 15 carries the rule. `Bidi.isolate` is the fix. This finds
the places that have not had it applied.

**How it decides.** It reads the string-typed properties off the models in
`Repository.kt`, then looks for those property names inside a `text`, `title`,
`subtitle` or `label` argument that mentions neither `Bidi` nor `strings`.

**Why it cannot be a gate.** Once a value is inside a `Text`, nothing in the
source distinguishes a string that came from the database from one that came
from the catalog by way of a variable, and plenty of hits here are correct:
the value of a text field being edited, a label from the template catalog that
happens to share a property name, a caller that isolates one level up. Deciding
those needs a person, and #226 is where that work is tracked.

    python3 tools/checks/report_bidi_isolation.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPOSITORY = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/data/Repository.kt"
SCREENS = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui"

# Arguments that end up rendered as words a person reads.
RENDERED = re.compile(r"(?:text|title|subtitle|label)\s*=\s*([^\n]+)")


def model_properties() -> set[str]:
    """Every string-typed property on a `Repository` model."""
    source = REPOSITORY.read_text(encoding="utf-8")
    properties: set[str] = set()
    for block in re.finditer(r"data class \w+\((.*?)\n    \)", source, re.S):
        for prop in re.finditer(r"val (\w+):\s*String", block.group(1)):
            properties.add(prop.group(1))
    return properties


def main() -> int:
    properties = model_properties()
    if not properties:
        print("Could not read any model properties from Repository.kt.")
        return 1

    found: list[tuple[str, int, str]] = []
    for path in sorted(SCREENS.rglob("*.kt")):
        source = path.read_text(encoding="utf-8")
        for match in RENDERED.finditer(source):
            expression = match.group(1)
            if "Bidi" in expression or "strings" in expression:
                continue
            if not any(re.search(rf"\.{name}\b", expression) for name in properties):
                continue
            line = source[: match.start()].count("\n") + 1
            found.append((str(path.relative_to(ROOT)), line, expression.strip()))

    print(f"{len(properties)} string properties across the models.")
    print(f"{len(found)} places render one without going through Bidi.")
    print()
    print("Each is a candidate, not a verdict. Some are correct: the value of a")
    print("field being edited, a catalog label sharing a property name, or a")
    print("caller that isolates one level up. Deciding needs a person, and the")
    print("only way to see the real ones is Arabic on the device. #226.")
    print()

    current = None
    for path, line, expression in found:
        if path != current:
            current = path
            print(path)
        print(f"  {line:>5}  {expression[:88]}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
