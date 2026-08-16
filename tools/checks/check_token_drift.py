#!/usr/bin/env python3
"""Measurements come from tokens, and the count of the ones that do not only falls.

**A dp or sp written into a screen is a measurement that no longer comes from
`DESIGN.md` section 6.** It looks right on the phone that was in somebody's hand
and it is invisible to every other check here: the costume audit, the overflow
audit, and the judgment check all pass on a screen whose spacing was typed.
D142, and it is one of the three mechanical causes of built screens drifting
from the grid files.

**This is a ratchet rather than a gate, because a gate would have to fail today.**
There were 161 of these across 51 files when this was written. Failing the build on all of them would
mean either a day of work before anything else can land or somebody adding
`|| true`, and the second is what actually happens. So the check fails only when
the number **rises**, which costs nothing to keep passing and makes every new one
a deliberate act.

**When it reaches zero it becomes an ordinary check**, the same way
`check_bidi_isolation.py` did once its residue was gone.

**What is legitimately a literal.** A hairline at `1.dp`, a border at `2.dp`, and
a few geometry constants that are not spacing at all. Those are exactly why this
counts rather than forbids: **the number going down is the goal, not the number
being zero tomorrow.** Lowering `BASELINE` as they go is part of doing the work.

    python3 tools/checks/check_token_drift.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui"

# Recorded 2026-08-11 against the tree at D142. It goes down and never up.
# 161 at D142. 158 after the first fidelity pass, 155 after the second.
# 154 once the document form's empty sheet was sized from the thumbnail
# vocabulary rather than from numbers typed into the screen. #361.
# 153 once the width of a hairline became a token, which is what the trail's
# index rail needed and what the sticky header had been typing. #361.
# Every radius outside the theme is a token now, each naming the grid selector
# it came from, so a later reader can check it against the drawing.
BASELINE = 127

LITERAL = re.compile(r"(?<![\w.])(\d+(?:\.\d+)?)\.(dp|sp)\b")


def drift() -> dict[str, int]:
    """Every file outside the theme package that writes its own measurements."""
    found: dict[str, int] = {}
    for path in sorted(UI.rglob("*.kt")):
        if "/theme/" in str(path):
            continue
        hits = LITERAL.findall(path.read_text(encoding="utf-8"))
        if hits:
            found[str(path.relative_to(ROOT))] = len(hits)
    return found


def main() -> int:
    found = drift()
    total = sum(found.values())

    if total > BASELINE:
        print(f"{total} hardcoded dp and sp literals, up from {BASELINE}.")
        print()
        print("A measurement written into a screen no longer comes from the")
        print("tokens in DESIGN.md section 6, and it is invisible to every other")
        print("check here. Use a Space, Radius, or type token, or lower BASELINE")
        print("in this file if you have genuinely removed others in the same change.")
        print()
        for path, count in sorted(found.items(), key=lambda kv: -kv[1])[:15]:
            print(f"  {count:4}  {path}")
        return 1

    if total < BASELINE:
        print(f"{total} hardcoded dp and sp literals, down from {BASELINE}.")
        print("Lower BASELINE in this file to lock the gain in. D142.")
        return 1

    print(f"{total} hardcoded dp and sp literals, holding at the recorded baseline.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
