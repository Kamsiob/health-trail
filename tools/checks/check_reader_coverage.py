#!/usr/bin/env python3
"""Every screen is walked by the reader check, and the claim cannot drift.

**`DESIGN.md` 12 said `ScreenReaderTest` walks every screen's semantics tree.**
It walked 44 of 75. #342.

**A document that overstates a check is worse than a missing check**, because
the next person reads the claim and stops looking. That is not hypothetical
here: `IncidentScreen` had no test of any kind, and a one-way link inside it,
where an entry could open its incident and the incident could not open its
entries, went unnoticed until somebody read the source for a different reason.

**So the claim is held to the directory rather than to anybody's memory**, which
is D133: hold the set to the file that generates it, never to a second copy of
the set. A list of screens inside this file would be the second copy, and it
would agree with a mistake forever.

**Two things this checks and one it deliberately does not.**

It checks that every file in `ui/screens` declaring a public composable has at
least one of those composables constructed by `ScreenReaderTest`. It does not
check that the fixture is any good: a screen composed with everything empty has
few nodes to label and passes for free, which is a judgment no check can make.
That one is on whoever writes the case, and the existing cases say so.

**A file may be exempt with a written reason on the line**, which is the same
escape `check_dead_gestures.py` uses and for the same reason: an exception
somebody has to justify in one sentence is cheap, and one nobody can make is a
check people route around.

Exit 0 when clean, 1 with every uncovered screen listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREENS = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/screens"
TEST = (
    ROOT
    / "android/app/src/androidTest/kotlin/com/kamsiob/healthtrail/ui/ScreenReaderTest.kt"
)

# **A file's composables, not its name.** `MedicationEventSheet.kt` declares
# `MedicationEventScreen`, so counting by file stem reported a screen as
# uncovered while a test walked it. Found while writing this check.
DECLARED = re.compile(r"^fun ([A-Z]\w+)\(", re.MULTILINE)

# **Named here rather than in a list of exempt files**, so the reason travels
# with the thing rather than with the check.
EXEMPT = {
    # Furniture. Every section screen composes it, so it is walked by all of
    # them and has no states of its own to check.
    "SectionScaffold",
}


def main() -> int:
    if not SCREENS.is_dir() or not TEST.is_file():
        print("Reader coverage check skipped: the sources are not here.")
        return 0

    test = TEST.read_text(encoding="utf-8")
    uncovered = []
    scanned = 0

    for path in sorted(SCREENS.glob("*.kt")):
        names = set(DECLARED.findall(path.read_text(encoding="utf-8")))
        if not names:
            continue
        scanned += 1
        if path.stem in EXEMPT:
            continue
        if any(re.search(r"\b" + re.escape(name) + r"\(", test) for name in names):
            continue
        uncovered.append(
            f"{path.relative_to(ROOT)}: declares "
            f"{', '.join(sorted(names))} and ScreenReaderTest constructs none of "
            f"them. An unlabeled touchable node is invisible in a screenshot, "
            f"which is the whole reason that test exists."
        )

    if uncovered:
        print(f"Reader coverage check failed. {len(uncovered)} screen(s) unwalked.")
        for problem in uncovered:
            print(f"  {problem}")
        print(
            "\nAdd a case to ScreenReaderTest with a fixture carrying real "
            "states, or name the file in EXEMPT with the reason. DESIGN.md 12."
        )
        return 1

    print(
        f"Reader coverage check passed. {scanned} screen files, "
        f"{len(EXEMPT)} exempt by name, and every other one is walked."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
