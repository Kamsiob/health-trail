#!/usr/bin/env python3
"""A composable that takes a callback and never calls it is a dead control.

**Found the hard way on 2026-08-18.** `MedicationRow` took an `onOpen` and never
called it, so every row on the medications screen drew a chevron promising a
door and did nothing when it was tapped. The screen compiled, its checks passed,
its tests passed, and it is the screen `HANDOFF.md` names as a worked example.
`uiautomator` said the truth in one line: the whole list had two clickable nodes
on it, the back arrow and the floating button.

**Rule 16**: everything the person touches responds, and a control that does
nothing on press reads as broken. **Rule 11**: nothing unfinished reaches the
person. A parameter the caller passes and the screen ignores is the app
promising something it has not built, and it is invisible to every other check
here because nothing about it is malformed.

**Why a check rather than a test per screen.** This is the shape #231 paid for:
a defect that can appear on any screen is held by one check across every source
file, not by remembering to write an assertion each time. A test would have to
exist for all eighty six screens to catch the eighty seventh.

Frozen files are exempt, per `docs/REMOVAL-LEDGER.md`: a defect in a frozen
screen is not a defect, it is history.

Kamsiob, AGPL-3.0.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
UI = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui"

# `docs/REMOVAL-LEDGER.md`. A frozen file is never called and never fixed.
FROZEN = {"ProjectDetailScreen.kt", "CaptureSheet.kt", "PinnedGroup.kt"}

# **Known, each on #390, and this list only ever shrinks.** A number here is
# a control somebody can reach for and not get, so it is written down by name
# rather than counted: a baseline of "three" would let the next one in silently.
KNOWN = {
    ("screens/ProjectHomeScreen.kt", "ProjectHomeScreen", "onOpenEntryById"),
    ("screens/ProjectHomeScreen.kt", "ProjectHomeScreen", "onToggleStep"),
    ("screens/TodayScreen.kt", "TodayScreen", "onOpenSection"),
}

FUN = re.compile(r"^(?:private )?fun (\w+)\(", re.M)
CALLBACK = re.compile(r"^\s{4}(on[A-Z]\w*):\s*\(([^)]*)\)\s*->\s*Unit", re.M)


def uncalled():
    found = set()
    for folder in ("screens", "v4", "components"):
        for path in sorted((UI / folder).rglob("*.kt")):
            if path.name in FROZEN:
                continue
            src = path.read_text(encoding="utf-8")
            rel = f"{folder}/{path.relative_to(UI / folder).as_posix()}"
            starts = [m for m in FUN.finditer(src)]
            for i, m in enumerate(starts):
                end = starts[i + 1].start() if i + 1 < len(starts) else len(src)
                body = src[m.start():end]
                for cb in CALLBACK.finditer(body):
                    name = cb.group(1)
                    # Anywhere after its own declaration in the same function.
                    if not re.search(r"\b" + name + r"\b", body[cb.end():]):
                        found.add((rel, m.group(1), name))
    return found


def main():
    found = uncalled()
    new = sorted(found - KNOWN)
    fixed = sorted(KNOWN - found)

    for rel, fn, cb in new:
        print(f"  {rel}: {fn}() takes {cb} and never calls it")
    if new:
        print()
        print(f"{len(new)} control(s) a person can reach for and not get.")
        print("Rule 16: a control that does nothing on press reads as broken.")
        print("Wire it, or take the parameter off so the caller stops passing one.")
        return 1

    if fixed:
        print("These are wired now. Remove them from KNOWN in this file:")
        for rel, fn, cb in fixed:
            print(f"  {rel}: {fn}() {cb}")
        return 1

    print(f"No composable ignores a callback it was handed. {len(KNOWN)} known, "
          f"each on an issue.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
