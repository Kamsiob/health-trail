#!/usr/bin/env python3
"""No control announces an action it does not perform.

**Rule 11: a control that says it does something and does nothing is not
finished.** `DESIGN.md` section 9 is a gate.

`removableByLongPress` was for a long time the only modifier the app had for a
tappable card, so every screen that needed a card to *open* something reached
for it and passed `onLongPress = {}` to switch the removal off.

**The gesture went quiet and the words did not.** The modifier declares two
semantics actions from the label it is handed, so with the removal label passed
in, a screen reader announced the **tap** as "remove" on a card that opens an
entry, and listed a **long press** called "remove" that ran an empty function.
Six screens shipped that way. #231.

**Nothing could have caught it by looking.** `OpenNotRemoveTest`'s own comment
says it: a screenshot of the fixed screen and the broken one are the same image.
It survived six screens and a design review because there was nothing to see.

**So it is held here, across every source file, rather than by a test per
screen.** A test proves the screen it names; this proves the shape cannot come
back anywhere, including in a screen nobody has written yet. `Modifier.openableByTap`
in `Press.kt` is what a card that opens something should use.

**What this does not check.** That a label says what the action does. A caller
can still hand `openableByTap` the wrong words, and only a reader on a real
device hears that. Rule 19, and it is why #231 was closed on a device rather
than on a green run.

Exit 0 when clean, 1 with every problem listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCES = ROOT / "android/app/src/main/kotlin"

# `onLongPress = {}` and `onLongPress = { }`, and the same with any handler
# name, because the next one will not be called onLongPress. A handler assigned
# an empty lambda does nothing, and if something announces it, it lies.
EMPTY_ANY = re.compile(r"\bon[A-Z]\w*\s*=\s*\{\s*\}", re.MULTILINE)

# Assigning an empty lambda is legitimate where nothing announces it, and the
# whole legitimate category is `@Preview`: a preview has nowhere to accept to
# and no reader ever hears it. Those are skipped by shape rather than by asking
# somebody to mark each one, because a marker on every preview would be noise
# that teaches people to add markers.
ALLOW = "allow-empty-handler:"

DECLARATION = re.compile(r"^\s*(?:private |internal |public )?fun\b")

# `openableByTap` is the modifier for a card that *opens* something, so a label
# that names removal on one is the #231 inversion exactly: the words say remove
# and the tap opens. Matched across lines because the label is usually on its
# own line under the call.
OPEN_LABELED_REMOVE = re.compile(
    r'openableByTap\s*\((?:[^()]|\([^()]*\))*?label\s*=\s*strings\[\s*"(remove[^"]*)"',
    re.DOTALL,
)


def line_of(text: str, at: int) -> int:
    return text.count("\n", 0, at) + 1


def inside_preview(lines: list[str], number: int) -> bool:
    """Whether the function containing line [number] is a `@Preview`.

    Walks up to the nearest function declaration, then up through the
    annotations and comments attached to it. Compose puts `@Preview` and
    `@Composable` immediately above `fun`, so this reaches it without needing to
    match braces.
    """
    at = number - 1
    while at >= 0 and not DECLARATION.match(lines[at]):
        at -= 1
    if at < 0:
        return False
    at -= 1
    while at >= 0:
        line = lines[at].strip()
        if line.startswith("@"):
            if line.startswith("@Preview"):
                return True
        elif not (line.startswith("//") or line.startswith("*") or
                  line.startswith("/*") or line == ""):
            return False
        at -= 1
    return False


def main() -> int:
    if not SOURCES.is_dir():
        print("Dead gesture check skipped: the Kotlin sources are not here.")
        return 0

    problems = []
    scanned = 0

    for path in sorted(SOURCES.rglob("*.kt")):
        text = path.read_text(encoding="utf-8")
        scanned += 1
        lines = text.splitlines()

        for found in EMPTY_ANY.finditer(text):
            number = line_of(text, found.start())
            # The reason has to be on the line itself or the one above it, so
            # it is read by whoever is looking at the handler.
            here = lines[number - 1] if number <= len(lines) else ""
            above = lines[number - 2] if number >= 2 else ""
            if ALLOW in here or ALLOW in above:
                continue
            if inside_preview(lines, number):
                continue
            relative = path.relative_to(ROOT)
            problems.append(
                f"{relative}:{number}: {found.group().strip()} is a handler that does "
                f"nothing. If anything announces it, the app tells a reader it does "
                f"something it does not. Use openableByTap for a card that opens, or "
                f"write `{ALLOW} <reason>` on the line above."
            )

    for path in sorted(SOURCES.rglob("*.kt")):
        text = path.read_text(encoding="utf-8")
        for found in OPEN_LABELED_REMOVE.finditer(text):
            relative = path.relative_to(ROOT)
            problems.append(
                f"{relative}:{line_of(text, found.start())}: openableByTap is labeled "
                f"{found.group(1)!r}, so a reader is told the tap removes something and "
                f"it opens it instead. That inversion is #231 exactly."
            )

    if problems:
        print(f"Dead gesture check failed. {len(problems)} problem(s).")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(
        f"Dead gesture check passed. {scanned} source files, and no handler is "
        f"assigned a lambda that does nothing."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
