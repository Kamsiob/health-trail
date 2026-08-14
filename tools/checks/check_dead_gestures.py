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
LEDGER = ROOT / "docs/REMOVAL-LEDGER.md"

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


# **Law 2: no long-press-only actions anywhere.** `DESIGN.md` section 2 and
# section 12: anything gesture-only also has a visible, non-gesture path.
#
# Removal lived on a long press in nine screens until #218. It was not a bare
# gesture, and that is exactly what made it survive: it declared an explicit
# long click semantics action, so a **reader** user was handed removal in their
# action list while a **sighted** person who did not already know the gesture
# could not remove anything at all. That inversion is `DESIGN.md` 13.5's, and no
# screenshot shows it.
#
# So the gesture is banned by shape rather than by name. `removableByLongPress`
# is deleted; this is what stops the next one being written.
LONG_PRESS = re.compile(
    r"\bcombinedClickable\b|\bonLongClick\b|\bonLongPress\b|"
    r"\bdetectDragGesturesAfterLongPress\b"
)

# **The one legitimate long press, and it has to name its twin.** D155.
#
# The rule law 2 states is that no action is reachable *only* by a gesture, and
# this check enforced something stricter: no gesture at all. That was right for
# every case it had seen, because every one of them was removal hidden behind a
# hold with nothing visible doing the same job.
#
# **Touch and hold to start arranging Today is not that case.** The owner asked
# for it by name: the home screen of the phone somebody already owns is the only
# frame of reference anybody brings to a grid of cards, and holding a widget is
# how arranging starts there. The visible Arrange control is still in the
# header, so the gesture is a shortcut to something already reachable, which is
# what 21.6 screen 5 said it must be from the day the screen was drawn.
#
# **The marker has to name the visible control**, not just claim one exists,
# because "there is another way" with nothing after it is how a guard becomes a
# formality. A bare marker with no words after the colon fails like an unmarked
# gesture. What it cannot check is whether the named control is real, which is
# rule 19's job on the device with a reader on.
TWIN = "long-press-twin:"


def frozen_files() -> set[str]:
    """The repository-relative paths `docs/REMOVAL-LEDGER.md` calls frozen.

    **Read from the ledger rather than listed here**, per D133: a second copy of
    a set agrees with the first forever, including about a mistake. A frozen
    screen is never fixed, so a gesture inside one is history rather than a
    defect, and the file that declares it frozen is the only honest source for
    which files those are.
    """
    if not LEDGER.is_file():
        return set()
    found = set()
    for line in LEDGER.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if len(cells) < 4 or "frozen" not in cells[-1].lower():
            continue
        for name in re.findall(r"`([^`]+\.kt)`", cells[0]):
            found.add(name)
    return found


def line_of(text: str, at: int) -> int:
    return text.count("\n", 0, at) + 1


def named_twin(text: str, number: int) -> bool:
    """Whether a [TWIN] marker naming a visible control covers this line.

    **Searched upward through the comment block rather than only one line
    above**, because a long press is written across several lines: the parameter
    is in one place, the `combinedClickable` in another, and the call that
    passes the handler in a third. Requiring the marker to sit immediately above
    each of them would put the same sentence in a file three times, and three
    copies is three chances to differ.

    **The marker must have words after the colon.** A bare one is a mute, and a
    mute is how a guard stops being a guard.
    """
    lines = text.splitlines()
    at = number - 1
    # Up through the declaration this sits in, and the comment block above it.
    limit = max(0, at - TWIN_REACH)
    while at >= limit:
        line = lines[at] if at < len(lines) else ""
        if TWIN in line:
            return bool(line.split(TWIN, 1)[1].strip())
        at -= 1
    return False


# How far above a gesture the marker may sit. Enough to cover a parameter's own
# KDoc, which is where the reason belongs, and short enough that a marker cannot
# quietly cover a second gesture further down the file.
TWIN_REACH = 30


def lines_at(text: str, number: int) -> str:
    lines = text.splitlines()
    return lines[number - 1] if 0 < number <= len(lines) else ""


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

    frozen = frozen_files()
    exempt = 0
    twins = 0

    for path in sorted(SOURCES.rglob("*.kt")):
        text = path.read_text(encoding="utf-8")
        for found in OPEN_LABELED_REMOVE.finditer(text):
            relative = path.relative_to(ROOT)
            problems.append(
                f"{relative}:{line_of(text, found.start())}: openableByTap is labeled "
                f"{found.group(1)!r}, so a reader is told the tap removes something and "
                f"it opens it instead. That inversion is #231 exactly."
            )

        relative = path.relative_to(ROOT)
        # The ledger names a frozen file by its path under the source root.
        under_source = str(path.relative_to(SOURCES / "com/kamsiob/healthtrail"))
        if under_source in frozen:
            if LONG_PRESS.search(text):
                exempt += 1
            continue

        for found in LONG_PRESS.finditer(text):
            number = line_of(text, found.start())
            here = lines_at(text, number)
            # A comment saying the gesture is gone is not the gesture. An import
            # is not the gesture either: it is the file saying which symbol it
            # will use, and the use itself is caught where it happens.
            if here.lstrip().startswith(("//", "*", "/*", "import ")):
                continue
            if named_twin(text, number):
                twins += 1
                continue
            problems.append(
                f"{relative}:{number}: {found.group()} is a long press, and law 2 bans "
                f"an action reachable only by one. A sighted person who does not already "
                f"know the gesture cannot do it at all, while a reader user is handed it "
                f"in their action list. #218. Put the action on the thing's own screen or "
                f"in the sheet its row opens, or write `{TWIN} <the visible control that "
                f"does the same thing>` on the line above."
            )

    if problems:
        print(f"Dead gesture check failed. {len(problems)} problem(s).")
        for problem in problems:
            print(f"  {problem}")
        return 1

    frozen_note = (
        f" {exempt} frozen file(s) still carry one and are exempt, because "
        f"docs/REMOVAL-LEDGER.md says they are never fixed."
        if exempt
        else ""
    )
    twin_note = (
        f" {twins} long press(es) name a visible control that does the same "
        f"thing, per D155, which is what law 2 actually asks for."
        if twins
        else ""
    )
    print(
        f"Dead gesture check passed. {scanned} source files: no handler is "
        f"assigned a lambda that does nothing, and no live screen puts an action "
        f"behind a long press alone.{frozen_note}{twin_note}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
