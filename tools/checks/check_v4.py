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

**It counts screens too, and it did not always.** For its first day this check
read 55 component files and was blind to the 85 screens that arrange them,
which is 60 percent of the surface the person actually sees. An overhaul three
components deep was found by the owner's eye rather than by the command whose
whole job is to say whether the overhaul is done, and a check that cannot see
most of the app would have let that happen again.

**A screen's press tell is not a component's.** A screen passing `onClick` into
a converted component is correct and must not be flagged: the spring lives in
the component and pressing that surface already answers. Only a raw
`Modifier.clickable`/`selectable`/`toggleable` written on the screen itself is
a silent surface. Scanning screens the component way reported 51 offenders when
there was 1, and a check that cries wolf 50 times gets ignored on the 51st.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
UI = ROOT / "android/app/src/main/kotlin/com/kamsiob/healthtrail/ui"
COMPONENTS = UI / "components"
SCREENS = UI / "screens"

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
    # no-press is empty: Dictate, EdgeScrubber and MonthGrid were the three
    # that wrote their own gesture and did not answer it, and they now spring
    # through the shared pressScale in Press.kt. ChipPicker and Disclosure were
    # on this list and were never broken.
    # old-shape: Radius.card or fold, against the containers' cardLarge.
    "ChartCard", "DateRow", "GroupedSurface", "LatestWordCard", "Press",
    "RoundCard", "StandingCard", "Tile",
    # both, plus mono on a word.
    "WashBand",
}

# Screens whose tell is correct, and why. Filled in as each mono site is judged:
# a figure in a column keeps mono and earns a line here, a word loses it.
SCREENS_ALLOWED = {
    # The double tap zooms the paper, and the zoom is the answer. Rule 16 asks
    # that a touch be answered, not that it be answered with a spring, and
    # scaling a full-screen photograph under the finger that is trying to
    # magnify it would fight the gesture it is acknowledging.
    "PaperViewerScreen": {"no-press"},
}

# The screens still carrying the old design. Thirty-five of eighty-five, found
# once the press tell stopped counting a passed-through onClick as silence.
SCREENS_REMAINING = {
    # old-shape only. Every screen site is Radius.card, forty-four of them, and
    # none is fold or small: containers drawn 22dp under components drawn 26dp.
    "CareThreadsScreen", "DisclaimerScreen", "MeasurementScreen",
    "MilestonesScreen", "MonthReviewScreen", "PersonScreen", "PrepScreen",
    "ProjectDetailScreen", "ProjectTrailScreen", "RestoreScreen",
    "SituationPickerScreen", "UnfiledTrayScreen",
    # old-shape and mono. Each mono site is judged word against figure.
    "AddInstructionScreen", "ChaptersScreen", "EntryScreen", "IncidentScreen",
    "MedicationScreen", "ProgressScreen", "ProjectPaperworkScreen",
    "ProjectsScreen", "SearchScreen", "StandingInstructionsScreen",
    "StartProjectScreen", "TemplateLibraryScreen", "ThreadScreen",
    "TodayScreen", "TrailScreen",
    # mono only.
    "AboutScreen", "EmergencyCardScreen", "PeopleScreen", "ProjectHomeScreen",
    "StartProjectPreviewSheet", "TodayFieldScreen",
    # the one genuinely silent surface in the app, and the one raw M3 card.
    "CardOptionsSheet", "ChapterScreen",
}


def tells(source: str) -> set:
    found = set()
    # **A file answers for the gestures it writes, and only those.** Passing
    # onClick down to a component that springs is correct, and the component it
    # is passed to is scanned on its own line, so nothing goes unchecked by
    # decomposing it this way. Counting a passed-through onClick as silence
    # named 51 screens and 3 components that were already right.
    tappable = bool(
        re.search(r"\.clickable\(|\.selectable\(|\.toggleable\(|detectTapGestures", source)
    )
    springs = bool(re.search(r"pressScale|springy\(\)|openableByTap|pressedSurface", source))
    if tappable and not springs:
        found.add("no-press")
    if "indication = ripple()" in source:
        found.add("ripple")
    if re.search(r"type\.mono\b|type\.monoL\b", source):
        found.add("mono")
    if re.search(r"Radius\.(card\b|fold\b|small\b)", source):
        found.add("old-shape")
    if re.search(r"(?<!\w)(Elevated|Outlined)?Card\(", source):
        found.add("m3-card")
    return found


def sweep(folder, remaining, allowed):
    """Returns (unexpected, done, total) for one folder."""
    unexpected, done = [], []
    paths = sorted(folder.glob("*.kt"))
    for path in paths:
        name = path.stem
        found = tells(path.read_text(encoding="utf-8"))
        found -= allowed.get(name, set())
        if found and name not in remaining:
            unexpected.append(f"{name}: {', '.join(sorted(found))}")
        if not found and name in remaining:
            done.append(name)
    return unexpected, done, len(paths)


def main() -> int:
    failed = False
    line = []
    for label, folder, remaining, allowed in (
        ("component", COMPONENTS, REMAINING, ALLOWED),
        ("screen", SCREENS, SCREENS_REMAINING, SCREENS_ALLOWED),
    ):
        unexpected, done, total = sweep(folder, remaining, allowed)
        if unexpected:
            failed = True
            print(f"{label.capitalize()}s carrying the old design "
                  f"that are not on the list:")
            for entry in unexpected:
                print(f"  {entry}")
            print(f"\nEither convert it, or list it with a reason.")
        if done:
            failed = True
            print(f"Converted, so take these {label}s off the list in this file:")
            for name in done:
                print(f"  {name}")
        line.append(f"{total - len(remaining)} of {total} {label}s")

    if failed:
        return 1

    remaining_total = len(REMAINING) + len(SCREENS_REMAINING)
    print(f"v4: {', '.join(line)} converted. {remaining_total} to go.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
