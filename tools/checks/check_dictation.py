#!/usr/bin/env python3
"""Every field somebody writes their own words into offers dictation.

**#388 finding 4, split out as #396.** The medication form's dose and frequency
had a microphone; its name did not. Neither did any appointment or person field.
`DictatableField` exists so that "every text area offers dictation" is one call,
and thirty-three sites used it while thirty-four more called `Field` directly.

**Nothing could have caught it by looking**, which is why it is held here. The
two controls are the same control with one difference, so a screenshot of a
field with dictation and one without differ by a twenty dp glyph that a review
does not miss on the screen it is looking at and misses on the thirty-three it
is not.

**The rule, and it is the whole of it.** A `Field` offers dictation unless one
of these is true:

- it is `masked`, which is a passphrase and must never be spoken into a
  recognizer;
- its `keyboardType` is anything but `Text`, because a number, a phone number
  and a date are not somebody's own words;
- it already carries a `trailing`, which is a unit or a picker and has the slot
  the microphone would take;
- it is named in EXEMPT below, with the reason.

**The exemptions are named rather than assumed**, which is what #396 asks for.
A file that is frozen, a search box, a picker, and the memo screen are all real
and none of them is discoverable from the call itself.

Exit 0 when clean, 1 with every problem listed otherwise.

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCES = ROOT / "android/app/src/main/kotlin"

# **Named, with the reason, because none of these is visible in the call.**
EXEMPT = {
    # Frozen, D112 and D199. A frozen file is never edited, so the rule carries
    # a permanent exception here rather than the file carrying a fix.
    "ProjectDetailScreen.kt": "frozen, docs/REMOVAL-LEDGER.md",
    # A search box is not a record somebody writes; it is a way to find one, and
    # it is emptied the moment it has been used.
    "SearchScreen.kt": "a search box, not a field somebody writes a record into",
    # The same: a picker's box filters a list of choices.
    "ChipPicker.kt": "a picker's filter, not a field somebody writes a record into",
    # The memos page's own box filters the memos in front of somebody. Same
    # reason as the search screen: it is a way to find a record, not one.
    "NotesScreen.kt": "a search box, not a field somebody writes a record into",
    # **The owner, 2026-08-18: "get rid of the mic completely", on the memo
    # screen and there alone.** It had two treatments of dictation at once, the
    # inline mark and the prominent button, which is rule 16's two answers to
    # one question. He confirmed the next day that this is the memo section
    # only and that dictation stays everywhere else. #397.
    "NoteScreen.kt": "the owner asked for no microphone in memos, #397",
}

# `Field(` but not `DictatableField(`, and not the declaration itself.
CALL = re.compile(r"(?<![A-Za-z])Field\s*\(")


def excluded(call: str) -> str | None:
    """Why this call needs no microphone, or None."""
    if "masked = true" in call:
        return "masked"
    if "trailing =" in call:
        return "trailing"
    match = re.search(r"keyboardType\s*=\s*KeyboardType\.(\w+)", call)
    if match and match.group(1) != "Text":
        return f"keyboardType {match.group(1)}"
    return None


def body(text: str, start: int) -> str:
    """The call's arguments, by counting parentheses."""
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    return text[start:]


def main() -> int:
    problems: list[str] = []
    for path in sorted(SOURCES.rglob("*.kt")):
        if path.name in EXEMPT:
            continue
        # The component itself declares both, and declaring is not calling.
        if path.name == "Fields.kt":
            continue
        text = path.read_text(encoding="utf-8")
        for match in CALL.finditer(text):
            before = text[max(0, match.start() - 12) : match.start()]
            if before.endswith("Dictatable"):
                continue
            call = body(text, match.start() + text[match.start():].index("("))
            if excluded(call):
                continue
            # **A declaration is not a call.** `ReadableArchive` has its own
            # `data class Field`, and naming a type is not asking somebody for
            # words.
            line_start = text.rfind("\n", 0, match.start()) + 1
            if re.search(r"\b(class|fun)\b", text[line_start : match.start()]):
                continue
            line = text.count("\n", 0, match.start()) + 1
            problems.append(
                f"  {path.relative_to(ROOT)}:{line}: a Field somebody writes their "
                f"own words into and no way to speak them",
            )

    if problems:
        print(f"Dictation check failed. {len(problems)} field(s).\n")
        print("\n".join(problems))
        print(
            "\nUse DictatableField, or, if this is not somebody's own words, say "
            "which: masked, a keyboardType that is not Text, or a trailing it "
            "already carries. A whole file that is genuinely none of those goes "
            "in EXEMPT with its reason. #396.",
        )
        return 1

    print("Dictation: every field somebody writes their own words into offers it.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
