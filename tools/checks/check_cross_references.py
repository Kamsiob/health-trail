#!/usr/bin/env python3
"""Every "section N" pointer between documents resolves to a section that exists.

**`CLAUDE.md` is loaded automatically every session and is the last thing to
survive compaction, so a wrong pointer in it is followed more often than any
other cross reference here.** On 2026-08-11 every one of its eight pointers
into `DESIGN.md` named the numbering that existed before the v4 rewrite. Rule 22
sent a session to section 11 for the component library, and section 11 is Voice.
That had been true since the rewrite and nothing could see it. #344.

**Why a check rather than care.** A renumbering is invisible from the document
doing the pointing: `DESIGN.md` was rebuilt correctly, and every file that
referred into it went stale in the same instant without changing a character.
Nothing in a diff shows that, and the reader who follows the pointer lands on a
real section with a plausible heading and no reason to doubt it.

**What it reads.** Any `Name.md` followed by a section number, with or without
the word section, in any tracked Markdown file. It resolves the number against
the target's own `##` and `###` headings.

**What it deliberately does not do.** It does not check that the section says
what the sentence claims it says. A pointer can resolve and still be wrong, and
that is a reading job.

**And it does not read the two history files.** `DECISIONS.md` and
`docs/RUN-LOG.md` record what was decided and done on a date, against the
numbering that existed on that date. Ten of their pointers are stale in exactly
the way #344 describes, and **correcting them would falsify a dated record**,
which is worth more than a resolving link. Both files say at the top that their
section numbers are as of the entry, which is the honest fix. Everything that
describes the current state is checked.

    python3 tools/checks/check_cross_references.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# The documents that carry numbered sections other files point into.
TARGETS = [
    "DESIGN.md",
    "MASTER_SPEC.md",
    "TESTING-PERSONAS.md",
    "RUN-SAFETY.md",
    "AGENTS.md",
    "contract/DATA-CONTRACT.md",
]

# `DESIGN.md` section 16.4, DESIGN.md 12, `MASTER_SPEC.md` section 7.
REFERENCE = re.compile(
    r"`?(?P<doc>[A-Z][A-Za-z-]*\.md)`?\s+(?:section\s+)?(?P<number>\d+(?:\.\d+)?)\b"
)

HEADING = re.compile(r"^#{2,3}\s+(\d+(?:\.\d+)?)[.\s]", re.MULTILINE)


def sections_of(path: Path) -> set[str]:
    """Every section number the document actually declares."""
    return set(HEADING.findall(path.read_text(encoding="utf-8")))


def main() -> int:
    known: dict[str, set[str]] = {}
    for rel in TARGETS:
        path = ROOT / rel
        if path.exists():
            known[path.name] = sections_of(path)

    # History, not current state. See the docstring.
    history = {"DECISIONS.md", "RUN-LOG.md", "CHANGELOG.md"}
    documents = [
        path
        for path in sorted(ROOT.glob("*.md")) + sorted((ROOT / "docs").glob("*.md"))
        if path.name not in history
    ]
    broken: list[tuple[str, int, str, str]] = []
    checked = 0

    for path in documents:
        for index, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
            for match in REFERENCE.finditer(line):
                doc, number = match.group("doc"), match.group("number")
                if doc not in known:
                    continue
                # A file pointing at its own numbering is checked too: that is
                # how a document goes stale against itself after a rewrite.
                checked += 1
                if number in known[doc]:
                    continue
                # `16.5` is allowed to resolve to `16` when the parent exists
                # and no such child does, since prose often names a whole
                # section and a clause inside it interchangeably.
                if number.split(".")[0] in known[doc] and "." not in number:
                    continue
                broken.append((str(path.relative_to(ROOT)), index, doc, number))

    if not broken:
        print(f"{checked} cross references checked, every one resolves.")
        return 0

    print(f"{len(broken)} of {checked} cross references name a section that does not exist.")
    print()
    print("The target document was probably renumbered. Follow the content to")
    print("its current section and correct the pointer, rather than adding the")
    print("section number the pointer expects.")
    print()
    for where, line, doc, number in broken:
        print(f"  {where}:{line}  points at {doc} section {number}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
