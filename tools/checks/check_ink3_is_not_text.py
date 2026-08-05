#!/usr/bin/env python3
"""`ink-3` never renders a word.

DESIGN.md 4.1 and 4.6, and DECISIONS.md D92: this app has two text levels,
`ink` and `ink-2`. `ink-3` is non-text only. It measures 2.37:1 on paper, which
is less than half the 4.5:1 floor, so a label drawn in it is unreadable for
exactly the audience this app is for: people who are tired, often older, and
frequently reading in bad light.

The previous direction carried a separate text-safe tertiary token, so a session
reading old habits will reach for `ink3` on a timestamp or an eyebrow and it will
look approximately fine on a bright desk monitor. It is not fine on a phone.

This check exists because of D90: a rule enforced in the build is read at the
moment somebody tries to break it, and a rule in a document is read at the start
of a session and lost to compaction. `ink3` stays a real token because hairlines,
dividers, inactive strokes, and chevron glyphs genuinely need it, so the token
cannot simply be deleted. What can be done is to fail the build the moment it is
handed to something that draws type.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "android/app/src/main/kotlin"

# A composable that renders type. `color =` inside one of these, set to ink3, is
# the defect. Everything else that takes an ink3 is drawing a shape.
TEXT_COMPOSABLES = ("Text(", "BasicText(", "AnnotatedString(")

# How far past the opening call to look for the color argument.
#
# **Measured against code with the comments already blanked**, so it is a
# bound on a call rather than on a call plus whatever was written about it.
# The largest Text call in this codebase is well under this, and scanning
# further would start picking up the next sibling composable.
WINDOW = 1200


LINE_COMMENT = re.compile(r"//[^\n]*")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)


def code_only(source: str) -> str:
    """The file with every comment blanked, character for character.

    **Blanked rather than removed**, so every offset still points where it
    did and a failure names the real line.

    This runs over the whole file before any scanning, not over one call
    afterward. Doing it per call is not enough: the window below is a fixed
    number of characters, and a Text call carrying a long comment fills that
    window with prose before reaching its own color argument, so the check
    passes on a call that is genuinely wrong. **That is exactly what
    happened**, and it passed silently, which is the worst way for a checker
    to be wrong.
    """
    source = BLOCK_COMMENT.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), source)
    return LINE_COMMENT.sub(lambda m: " " * len(m.group(0)), source)


def offenders() -> list[str]:
    found: list[str] = []
    for path in sorted(SOURCE.rglob("*.kt")):
        source = code_only(path.read_text(encoding="utf-8"))
        for call in TEXT_COMPOSABLES:
            for match in re.finditer(re.escape(call), source):
                window = source[match.start(): match.start() + WINDOW]
                # Stop at the call's own closing paren where we can find it, so
                # a later sibling's ink3 is not blamed on this one.
                depth, end = 0, len(window)
                for index, char in enumerate(window):
                    if char == "(":
                        depth += 1
                    elif char == ")":
                        depth -= 1
                        if depth == 0:
                            end = index
                            break
                body = window[:end]
                # **Anywhere in the call, not only as a direct assignment.**
                # The first version matched `color = colors.ink3` and missed
                # `color = when { ... else -> colors.ink3 }`, which is how a
                # label that changes with state is written and therefore the
                # most likely place for this defect. RoadStrip shipped that
                # exact shape past this check on 2026-08-05.
                #
                # **Comments are stripped first**, and that is not optional:
                # the first widened version failed on the comment written to
                # explain why ink3 was not being used there. That is the #216
                # shape, in a checker widened in the same hour to fix a
                # different instance of it, and it is the reason this file
                # now matches code rather than text.
                if re.search(r"\bink3\b", body):
                    line = source[: match.start()].count("\n") + 1
                    rel = path.relative_to(ROOT)
                    found.append(f"  {rel}:{line}: {call.rstrip('(')} drawn in ink3")
    return found


def main() -> int:
    if not SOURCE.is_dir():
        print("ink3 check skipped: no Kotlin source yet.")
        return 0

    found = offenders()
    if found:
        print(f"ink3 check failed. {len(found)} place(s) render text in ink3.")
        print()
        for line in found:
            print(line)
        print()
        print(
            "DESIGN.md 4.1 and 4.6, DECISIONS.md D92. This app has two text\n"
            "levels: ink and ink2. ink3 is non-text only, at 2.37:1 on paper.\n"
            "Use ink2 for secondary text. If the thing genuinely is not text,\n"
            "it should not be inside a Text composable."
        )
        return 1

    print("ink3 check passed. No text is drawn in the non-text token.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
