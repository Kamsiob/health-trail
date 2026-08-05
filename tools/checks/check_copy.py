#!/usr/bin/env python3
"""House style checks on everything a person reads.

Two rules from CLAUDE.md, enforced here rather than by remembering:

  4. No em dashes in anything a user or reader sees. App copy, documentation,
     README, commit messages, store text. Source code is exempt where a
     character is functionally required.
  5. American English everywhere.

Exit 0 when clean, 1 with a list of offenses otherwise.

Kamsiob, AGPL-3.0.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# Third party texts reproduced verbatim. Rewriting a standard license or code of
# conduct to match house style would defeat the point of using a standard text.
#
# The concept and reference files are the handover artifacts that fixed the
# visual design. They are historical records and are not edited. They do contain
# en dashes inside date ranges, and the app must not reproduce those: app copy
# writes a range as "September to October 2023" or formats it per locale. That
# is enforced on the app's own sources, which are not exempt.
#
# This checker is exempt from itself, since it necessarily contains every
# character and every spelling it looks for.
EXEMPT_FILES = {
    "LICENSE",
    "CODE_OF_CONDUCT.md",
    "templates/LICENSE-CONTENT.md",
    "kamsiob-project-template.md",
    "reference/screen-grid.html",
    "health-trail-concept-review.html",
    "health-trail.html",
    "tools/checks/check_copy.py",
}

EXEMPT_DIRS = {
    ".git", "build", ".gradle", "node_modules", "__pycache__", ".venv",
}

TEXT_SUFFIXES = {
    ".md", ".txt", ".json", ".yml", ".yaml", ".xml", ".kt", ".kts", ".java",
    ".py", ".sh", ".sql", ".html", ".css", ".js", ".ts", ".pro",
}

# Source files where the character may be functionally required. Checked with a
# narrower rule: the character is allowed, but not inside a user facing string.
SOURCE_SUFFIXES = {".kt", ".kts", ".java", ".py", ".sh", ".js", ".ts", ".css"}

EM_DASH = "—"
EN_DASH = "–"

# British spellings and their American forms. Word boundary anchored on the left
# so "colour" matches inside "colours" and "coloured" without matching "color".
#
# **Most entries are prefixes on purpose**, because the British form inflects:
# "organis" has to reach "organise", "organising" and "organisation", and a
# right-hand \b would catch none of them.
#
# **The cost of that is a substring matching inside a word that is already
# American**, which is #216: "programmer" is correctly spelled and contains
# "programme", and the check called it British. The fix is not a blanket right
# anchor, which would break every inflecting entry above. It is a negative
# lookahead on the specific entries where a real American word continues past
# the prefix, and each one names the word it is protecting.
#
# **A checker whose failures are mostly false teaches people to route around
# it**, which is worse than not having the checker. So when this fires on
# something correct, the entry gets a lookahead here rather than the sentence
# getting reworded.
BRITISH = {
    r"colou r?".replace(" ", ""): "color",
    r"honou r?".replace(" ", ""): "honor",
    r"behaviou r?".replace(" ", ""): "behavior",
    r"favou r?".replace(" ", ""): "favor",
    # Not "organism" or "organist", both American and both plausible here.
    r"organis(?![mt])": "organiz",
    r"recognis": "recogniz",
    r"initialis": "initializ",
    r"normalis": "normaliz",
    r"serialis": "serializ",
    r"analys(?:e|ing)": "analyz",
    r"artefact": "artifact",
    r"licence": "license",
    r"defence": "defense",
    r"offence": "offense",
    r"catalogue": "catalog",
    r"dialogue box": "dialog box",
    r"\bgrey\b": "gray",
    r"cancelled": "canceled",
    r"cancelling": "canceling",
    r"labelled": "labeled",
    r"travelled": "traveled",
    r"\btowards\b": "toward",
    r"\bamongst\b": "among",
    r"\bwhilst\b": "while",
    r"judgement": "judgment",
    r"acknowledgement": "acknowledgment",
    r"\bcentre\b": "center",
    r"\bmetre\b": "meter",
    r"\bfibre\b": "fiber",
    r"enrolment": "enrollment",
    r"fulfilment": "fulfillment",
    r"instalment": "installment",
    r"practise": "practice",
    # Not "programmer" or "programmed", both correct American spellings. #216.
    r"programme(?![rd])": "program",
    r"\bstorey\b": "story",
    r"sceptic": "skeptic",
    r"\bmoustache\b": "mustache",
    r"\bplough\b": "plow",
    r"\bdraught\b": "draft",
    r"\bkerb\b": "curb",
    r"\btyre\b": "tire",
    r"\bpyjama": "pajama",
    r"\baluminium\b": "aluminum",
    r"speciality": "specialty",
}

BRITISH_RE = [(re.compile(pattern, re.IGNORECASE), american)
              for pattern, american in BRITISH.items()]

# A quoted string in a source file. Rough but adequate: we only need to know
# whether a dash sits inside quotes, not to parse the language.
QUOTED = re.compile(r'"[^"\n]*"' + r"|'[^'\n]*'")

# **The battle voice, banned everywhere on 2026-08-04.** `DESIGN.md` section 22
# and D109. Nothing in this app frames a person's situation as a battle, a game,
# or a race, and the ban covers code identifiers as well as copy, because a
# variable named `daysToWin` becomes a string eventually.
#
# **Why the app cares.** The person opening it may be a parent whose child is in
# treatment. Casting their life as a fight they might lose is the app telling
# them what their situation means, which is the same rule that bans
# interpretation applied to the frame rather than to a number.
#
# Matched as whole words, and the replacement column says what to write instead
# rather than only what not to. A failure names the line, so a legitimate use in
# quoted source, such as the word "lost" in a person's own recorded words, is
# read by a human rather than guessed at here.
BATTLE = {
    r"\bfight\w*\b": "say what is happening: waiting on, asked for, appealed",
    r"\bbattl\w*\b": "say the process by name",
    r"\bopponents?\b": "name the person by role",
    r"\bthe ball is\b": "say whose hands it is in",
    r"\bhaving the last word\b": "say what was said, and by whom",
    # **Win and lose are matched in their framing sense only.** A bare "wins"
    # or "losing" is far more often innocent: "losing the reference number" is
    # a real failure point families meet, and a comment saying the first hit
    # wins is about a tap. #216 is this repository's own lesson that a checker
    # matching too broadly is a checker somebody learns to ignore, so these
    # match the phrase that carries the frame rather than the word.
    r"\b(?:win|won|lose|lost|losing|winning)\s+(?:the\s+)?"
    r"(?:appeal|case|claim|fight|battle|argument)\b":
        "say the outcome: approved, denied, answered, closed",
}

BATTLE_RE = [(re.compile(pattern, re.IGNORECASE), instead)
             for pattern, instead in BATTLE.items()]


def iter_files():
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file():
            continue
        if any(part in EXEMPT_DIRS for part in path.parts):
            continue
        relative = path.relative_to(ROOT).as_posix()
        if relative in EXEMPT_FILES:
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        yield path, relative


def check_dashes(path, relative, problems):
    is_source = path.suffix.lower() in SOURCE_SUFFIXES
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        return
    for number, line in enumerate(lines, start=1):
        for char, name in ((EM_DASH, "em dash"), (EN_DASH, "en dash")):
            if char not in line:
                continue
            if is_source:
                # In source, only flag it when it sits inside a string literal,
                # which is where a reader would eventually see it.
                inside = any(char in match.group(0) for match in QUOTED.finditer(line))
                if not inside:
                    continue
            problems.append(
                f"{relative}:{number}: {name} in text a person reads\n"
                f"    {line.strip()[:120]}"
            )


def check_spelling(path, relative, problems):
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        return
    is_markdown = path.suffix.lower() == ".md"
    for number, line in enumerate(lines, start=1):
        # URLs and file paths legitimately carry any spelling.
        stripped = re.sub(r"https?://\S+", "", line)
        # So does anything in backticks in a document, because backticks mark a
        # token being quoted rather than a word being written: an identifier, a
        # path, or a regular expression. A decision entry explaining why the
        # pattern is `organis(?![mt])` is not written in British English, and
        # rewording it to satisfy this check would lose the only part of the
        # entry a reader needs. Fenced blocks are left in scope on purpose,
        # since copy inside one is still copy.
        if is_markdown:
            stripped = re.sub(r"`[^`\n]*`", "", stripped)
        for pattern, american in BRITISH_RE:
            match = pattern.search(stripped)
            if match:
                problems.append(
                    f"{relative}:{number}: British spelling "
                    f"{match.group(0)!r}, use {american!r}\n"
                    f"    {line.strip()[:120]}"
                )


def is_user_copy(relative):
    """Whether this file holds words the person actually reads in the app.

    **Scoped deliberately, and the scope is the interesting decision.** The rule
    in `DESIGN.md` section 22 governs what the app says to somebody, so the
    check runs on the four locale catalogs and the template content, which are
    the two places user-facing sentences live, plus quoted strings in Kotlin.

    **The documents that define the rule are out of scope, because a rule
    cannot be written without naming what it bans.** `DESIGN.md` section 22,
    D109, and both reference grids all contain the word "fight" precisely in
    order to forbid it, and a check that fails on its own specification is a
    check somebody deletes.

    **Comments are out of scope too**, and that is not a loophole: "two things
    fighting for one finger" is about a touch target rather than about
    somebody's life, and #216 is this repository's own record of what happens
    when a checker matches more than it means.
    """
    return (
        relative.startswith("contract/i18n/")
        or relative.startswith("templates/data/")
        or relative.endswith(".kt")
    )


def check_voice(path, relative, problems):
    """Nothing frames a person's situation as a battle, a game, or a race."""
    if not is_user_copy(relative):
        return
    is_source = path.suffix.lower() in SOURCE_SUFFIXES
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        return
    for number, line in enumerate(lines, start=1):
        stripped = re.sub(r"https?://\S+", "", line)
        if is_source:
            # Only what sits inside a string literal, which is what a person
            # eventually reads. The same narrowing check_dashes uses.
            quoted = " ".join(m.group(0) for m in QUOTED.finditer(stripped))
            stripped = quoted
        for pattern, instead in BATTLE_RE:
            match = pattern.search(stripped)
            if match:
                problems.append(
                    f"{relative}:{number}: battle voice {match.group(0)!r}, "
                    f"{instead}. DESIGN.md section 22\n"
                    f"    {line.strip()[:120]}"
                )


def main():
    problems = []
    count = 0
    for path, relative in iter_files():
        count += 1
        check_dashes(path, relative, problems)
        check_spelling(path, relative, problems)
        check_voice(path, relative, problems)

    if problems:
        print(f"Copy check failed. {len(problems)} problems across {count} files.\n")
        for problem in problems:
            print(f"  {problem}")
        print(
            "\nCLAUDE.md rules 4 and 5, and DESIGN.md section 22. No em dashes "
            "in anything a person reads, American English everywhere, and "
            "nothing framed as a battle, a game, or a race."
        )
        return 1

    print(
        f"Copy check passed. {count} files, no em dashes, American English, "
        f"no battle voice."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
