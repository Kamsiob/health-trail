#!/usr/bin/env python3
"""Every lamp in the app opens a written tip, and every written tip has a lamp.

**The hole this closes was found in a release build by the owner.** #464: the
Memos lamp opened a panel whose title read `tips.notes.title`. Not filler, and
not a translation gap. The four keys existed in no catalog at all, so
`Strings.resolve` fell through to returning the key, which is what it is
supposed to do in release rather than crash somebody's notebook.

**`check_string_keys.py` cannot catch this and says so.** Its regex reads
literal keys out of the sources, and `tipFor` builds `"tips.$slug.title"` from
`Repository.Section.name`. A key assembled at runtime is invisible to a scan
that reads text. So this check goes the other way: it enumerates the sections
from the enum itself and demands the four keys for each.

Both directions are failures, and the second one is the quieter:

- A lamp with no tip shows raw keys in release and throws in debug.
- A tip with no lamp is copy that was written, reviewed and translated four
  times and that nobody can reach. Four of the twelve notebook rows were in
  that state when this check was written.

Usage: python3 tools/checks/check_tips.py

Kamsiob, AGPL-3.0.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "contract" / "i18n" / "en.json"
SOURCES = ROOT / "android" / "app" / "src" / "main" / "kotlin" / "com" / "kamsiob" / "healthtrail"
REPOSITORY = SOURCES / "data" / "Repository.kt"

PARTS = ("title", "body", "point1", "point2")

# `tipFor` is the only caller that derives a key from a section, and this is the
# shape it derives. Kept here as one string rather than three so that a change
# to the derivation is a change to one line in each place.
DERIVED = 'tips.{slug}.{part}'

# A section whose screen deliberately has no lamp still needs its tip written,
# because the tip is what the section is, not what one screen shows. Nothing is
# exempt today; the list exists so that an exemption has to be argued in writing
# rather than added by deleting an assertion.
NO_TIP_EXPECTED: set = set()


def sections():
    """The section slugs, read off the enum rather than listed a second time."""
    body = REPOSITORY.read_text(encoding="utf-8")
    start = body.index("enum class Section(")
    end = body.index("\n        ;", start)
    block = body[start:end]
    # `NAME("live_thing", hiddenWhen = ...)`, at the enum's own indentation.
    found = re.findall(r'^        ([A-Z][A-Z_]*)\(', block, re.MULTILINE)
    if not found:
        raise SystemExit("check_tips: could not read Repository.Section")
    return [name.lower() for name in found]


def destinations():
    """The keys passed to `tipForDestination`, which are literals and greppable."""
    keys = set()
    for path in SOURCES.rglob("*.kt"):
        for match in re.finditer(r'tipForDestination\(\s*"([a-z_]+)"', path.read_text(encoding="utf-8")):
            keys.add(match.group(1))
        # `MoreScreen` hands its key to `AppearanceScreen`, which passes it on.
        for match in re.finditer(r'tipsKey\s*=\s*"([a-z_]+)"', path.read_text(encoding="utf-8")):
            keys.add(match.group(1))
        for match in re.finditer(r'tipKey\s*=\s*"([a-z_]+)"', path.read_text(encoding="utf-8")):
            keys.add(match.group(1))
    return sorted(keys)


def main():
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    problems = []

    wanted = {}
    for slug in sections():
        if slug in NO_TIP_EXPECTED:
            continue
        wanted[slug] = "Repository.Section"
    for slug in destinations():
        wanted.setdefault(slug, "tipForDestination")

    for slug, why in sorted(wanted.items()):
        for part in PARTS:
            key = DERIVED.format(slug=slug, part=part)
            if key not in catalog:
                problems.append(
                    f"{key} is asked for by {why} and no catalog defines it. "
                    "In release that key is what the person reads."
                )

    # The other direction: a written tip nobody can open.
    written = {
        key.split(".")[1]
        for key in catalog
        if key.startswith("tips.") and key.count(".") == 2
    }
    for slug in sorted(written - set(wanted)):
        problems.append(
            f"tips.{slug}.* is written and translated four times and nothing asks for it. "
            "Either a screen lost its lamp or the group is dead copy."
        )

    if problems:
        print("Tips check failed.")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(
        f"Tips check passed. {len(wanted)} lamps, "
        f"{len(wanted) * len(PARTS)} keys, every one written."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
