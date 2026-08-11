#!/usr/bin/env python3
"""Where the person's own words reach a screen without being isolated.

**It was a report and it is now a gate, and the rename is the point.** It began
as a worklist of 115 places because it could not tell a real defect from a
false positive, and a gate that cries wolf is one somebody learns to run with
`|| true`. Every one of those places has now been decided: isolated where the
words are the person's own, annotated `bidi-ok` with a reason where they are
not. **A worklist becomes a gate the moment its residue is zero**, and from here
what it catches is not old work, it is the next screen somebody writes.

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

**It reads the whole argument, not the first line of it.** The ordinary shape
here is `entry.title?.takeIf { it.isNotBlank() }` on one line and
`?.let { Bidi.isolate(it) }` on the next, and a line-at-a-time reading calls
every one of those a defect. It said 60 places were unisolated when 21 of them
had been isolated the line below, which is a report training its reader to
distrust it. The continuation rule is deliberately dull: keep taking lines while
the brackets are unbalanced, or while the next one opens with `?.`, `?:` or `.`.

**Saying a place is correct.** Put `// bidi-ok:` and the reason on the line.
The three that must never be isolated are the value inside a field being
edited, a draft on its way to the database, and anything building a filename,
because in all three the invisible marks become part of the data. The ordinary
one is a caller that isolates a level up.

**Where it is known to be blind.** An argument that mentions `strings`
anywhere is skipped, and that is one heuristic doing two jobs: it is right when
the whole value came from the catalog, and wrong when the catalog only supplies
a fallback. `ProjectSetupScreen` joined a family's own stage names with a dot
and ended the expression `.ifBlank { strings[...] }`, and this check walked past
it on 2026-08-11. Tightening the rule would fill the output with the case it was
written to suppress, so **the honest statement is that a green result covers
what the rule can see**, and a raw join with a catalog fallback is outside it.

**Why it still cannot decide on its own.** Once a value is inside a `Text`,
nothing in the source distinguishes a string that came from the database from
one that came from the catalog by way of a variable. That is what the
annotation is for: the check demands a decision, it does not make one. Deleting
a `bidi-ok` line is the way to reopen a question, and it will fail the build
until somebody answers it again.

**Arabic on the device is still the only place the real ones are visible.** A
green check means every place has been decided, not that every decision is
right. #226.

    python3 tools/checks/check_bidi_isolation.py
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


CONTINUES = re.compile(r"^\s*(\?\.|\?:|\.)")


def whole_argument(source: str, match: re.Match[str]) -> str:
    """The argument as written, however many lines it takes.

    **A one-line reading is wrong about the commonest shape in this codebase**,
    where a value is narrowed on one line and isolated on the next. Nothing here
    parses Kotlin: it keeps taking lines while the brackets are open or while
    the next line begins with a call or an elvis, which is enough to see a
    `Bidi` that lives one line down and stops well before the next argument.
    """
    lines = source[match.start(1):].split("\n")
    taken = [lines[0]]
    for nxt in lines[1:]:
        joined = "\n".join(taken)
        unbalanced = any(
            joined.count(o) > joined.count(c) for o, c in ("()", "{}", "[]")
        )
        if not unbalanced and not CONTINUES.match(nxt):
            break
        taken.append(nxt)
        if len(taken) > 12:
            break
    return "\n".join(taken)


def nearby_comment(source: str, match: re.Match[str]) -> str:
    """The `// bidi-ok` note, wherever on or above the line it was written.

    **A decision belongs next to the code it is about.** An allowlist in this
    file would be a list of line numbers that rots the first time somebody adds
    an import, and a reviewer reading the screen would never see it. A comment
    on the line shows up in the diff that moves the code, which is exactly when
    the decision needs making again.
    """
    before = source[: match.start()].split("\n")
    end = source.find("\n", match.end())
    line = source[match.start(): end if end != -1 else len(source)]
    comment = line[line.find("//"):] if "//" in line else ""
    # The comment block directly above, so a reason can be a sentence rather
    # than whatever fits after the code on one line.
    for earlier in reversed(before[:-1]):
        if not earlier.strip().startswith("//"):
            break
        comment += " " + earlier.strip()
    return comment


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
            expression = whole_argument(source, match)
            if "Bidi" in expression or "strings" in expression:
                continue
            if "bidi-ok" in expression or "bidi-ok" in nearby_comment(source, match):
                continue
            if not any(re.search(rf"\.{name}\b", expression) for name in properties):
                continue
            line = source[: match.start()].count("\n") + 1
            found.append((str(path.relative_to(ROOT)), line, expression.strip()))

    if not found:
        print(f"{len(properties)} string properties across the models.")
        print("Every place one reaches a screen is isolated or annotated bidi-ok.")
        return 0

    print(f"{len(properties)} string properties across the models.")
    print(f"{len(found)} render one without Bidi and without saying why.")
    print()
    print("Isolate it with Bidi.isolate if the words are the person's own, or")
    print("write // bidi-ok: and the reason on the line if they are not. The")
    print("three that must never be isolated are the value inside a field being")
    print("edited, a draft on its way to the database, and anything building a")
    print("filename: in all three the invisible marks become part of the data.")
    print()

    current = None
    for path, line, expression in found:
        if path != current:
            current = path
            print(path)
        print(f"  {line:>5}  {expression.splitlines()[0][:88]}")

    return 1


if __name__ == "__main__":
    sys.exit(main())
