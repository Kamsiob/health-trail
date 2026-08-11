#!/usr/bin/env python3
"""No text stops mid-word without saying it stopped.

**Rule 11 bans truncation, and `maxLines` on its own is truncation with the
evidence removed.** Compose clips at the line box: the last word ends wherever
the box ends, there is no ellipsis, no fade, nothing on the screen to say a
sentence continued. A person reading a role, a dose, or what a document is has
no way to know they are looking at part of it.

**Where this came from.** `DenseRow` capped its subtitle at one line by default,
so every row in the app that had never thought about the question clipped
somebody's own words silently. It is the shape defaults always have: the screens
that never got looked at are exactly the screens the default decides for.

**What is allowed.** No cap at all, which is the right answer for a sentence.
A cap with `overflow = TextOverflow.Ellipsis`, so the row says there is more.
Or a cap with `softWrap = false`, which is the edge scrubber's case, where a
two digit month split across two lines at font scale 2.0 read as nothing.

**What this cannot see.** A cap that fits in English and not in German, and a
cap that fits at font scale 1.0 and not at 2.0. Both are visible only on the
phone, which is what `DESIGN.md` 16.2 is for. This check holds the floor under
that pass: it makes sure the app never clips *silently*, so what 16.2 is
looking for is a visible mark in the wrong place rather than an invisible cut.

    python3 tools/checks/check_silent_clip.py
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui"

# How far above and below a `maxLines` line the rest of its `Text` call can sit.
# Arguments are one per line here, and the longest of these calls carries a
# style, a color, a weight, a text align, a modifier and a comment block.
ABOVE = 16
BELOW = 8


def offenders() -> list[tuple[str, int, str]]:
    found: list[tuple[str, int, str]] = []
    for path in sorted(UI.rglob("*.kt")):
        lines = path.read_text(encoding="utf-8").split("\n")
        for index, line in enumerate(lines):
            if "maxLines" not in line:
                continue
            # A cap of every line is not a cap, and a line of prose about
            # `maxLines` is not a call to it.
            if "Int.MAX_VALUE" in line or line.lstrip().startswith(("//", "*")):
                continue
            window = "\n".join(lines[max(0, index - ABOVE): index + BELOW])
            if "overflow" in window or "softWrap" in window:
                continue
            found.append((str(path.relative_to(ROOT)), index + 1, line.strip()))
    return found


def main() -> int:
    found = offenders()
    if not found:
        print("No capped text clips without an ellipsis or an explicit no-wrap.")
        return 0

    print(f"{len(found)} places cap their lines and say nothing about the rest.")
    print()
    print("Drop the cap, which is right whenever the text is a sentence, or add")
    print("overflow = TextOverflow.Ellipsis so the reader can see there is more.")
    print("softWrap = false is the third answer and it needs its own reason.")
    print()
    for path, line, text in found:
        print(f"  {path}:{line}  {text}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
