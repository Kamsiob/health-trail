#!/usr/bin/env python3
"""Which components still carry the pre-v4 design, and why.

The overhaul was three components deep before anybody counted. This counts,
so "is it done" is a command rather than an opinion, and so a component that
regresses is caught the same day.

The four tells are the ones found on the phone, in the order they were found:

  no-press   a tappable surface that does not answer the finger. Rule 16, and
             the app's answer is a spring, never a ripple.
  ripple     the Material ripple, which is the other app's interaction grammar.
  mono       the typewriter face on a word. Mono is for figures that line up in
             a column: chart axes, day grids, reference values, the scrubber.
             A count, a label, an eyebrow, a person's name are words.
  old-shape  Radius.card, Radius.fold or Radius.small, against the cardLarge
             and hero the containers use. A fold under a card drawn at a
             different radius reads as a different hand.

ALLOWED holds the components that legitimately keep a tell, each with the
reason. Everything else must come clean. Shrink REMAINING; never grow it.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
COMPONENTS = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui/components"

# Components whose tell is correct, and why. A figure in a column is mono on
# purpose; a component with no surface of its own has no shape to get wrong.
ALLOWED = {
    "ChartCard": {"mono"},        # axis values and the reading count: figures
    "MonthGrid": {"mono"},        # day numbers in a grid, which must align
    "ReferenceLine": {"mono"},    # a value against an axis
    "DatePicker": {"mono"},       # numbers the person is picking between
    "EdgeScrubber": {"mono"},     # index marks down the margin
    "RoadStrip": {"mono"},        # stage names measured to one width, tabular
    "Spine": {"mono"},            # the step number on a sequence
    "DateRow": {"mono"},          # a date at prominence is a figure
}

# Still to convert. Each entry is a promise to somebody, not a permanent
# exemption: take entries out as they are done, and never add one.
REMAINING = {
    # no-press: tappable and silent under the finger. Rule 16.
    "ChipPicker", "Dictate", "Disclosure", "EdgeScrubber", "MonthGrid",
    "RoundCard",
    # old-shape: Radius.card or fold, against the containers' cardLarge.
    "ChartCard", "DateRow", "GroupedSurface", "LatestWordCard", "Press",
    "StandingCard", "Tile",
    # both, plus mono on a word.
    "WashBand",
}


def tells(source: str) -> set:
    found = set()
    tappable = bool(re.search(r"clickable\(|selectable\(|onClick", source))
    springs = bool(re.search(r"pressScale|springy\(\)|openableByTap|pressedSurface", source))
    if tappable and not springs:
        found.add("no-press")
    if "indication = ripple()" in source:
        found.add("ripple")
    if re.search(r"type\.mono\b|type\.monoL\b", source):
        found.add("mono")
    if re.search(r"Radius\.(card\b|fold\b|small\b)", source):
        found.add("old-shape")
    return found


def main() -> int:
    unexpected = []
    done = []
    for path in sorted(COMPONENTS.glob("*.kt")):
        name = path.stem
        found = tells(path.read_text(encoding="utf-8")) - ALLOWED.get(name, set())
        if found and name not in REMAINING:
            unexpected.append(f"{name}: {', '.join(sorted(found))}")
        if not found and name in REMAINING:
            done.append(name)

    if unexpected:
        print("Components carrying the old design that are not on the list:")
        for line in unexpected:
            print(f"  {line}")
        print("\nEither convert it, or add it to REMAINING with a reason.")
    if done:
        print("Converted, so take these out of REMAINING in this file:")
        for name in done:
            print(f"  {name}")

    if unexpected or done:
        return 1

    total = len(list(COMPONENTS.glob("*.kt")))
    print(f"v4: {total - len(REMAINING)} of {total} components converted, "
          f"{len(REMAINING)} to go.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
