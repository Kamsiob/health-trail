#!/usr/bin/env python3
"""Assert that /contract stays platform neutral.

contract/DATA-CONTRACT.md section 2:

  Nothing in `/contract` may import from `/android` or `/web`. It is pure data
  and specification.

  Neither platform may define its own copy of the schema, the export format,
  the templates, or the message catalog. They read from `/contract` and
  `/templates`.

The first rule is checked directly here. The second is checked from the other
side: no source file outside `/contract` may declare the schema itself.

The reason both matter is one sentence from the contract: if the schema exists
only as platform code, the second platform is a reimplementation rather than a
second reader, and the two drift apart within weeks.

Exit 0 when clean, 1 with the offending files otherwise.

Kamsiob, AGPL-3.0.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTRACT = ROOT / "contract"

SKIP_DIRS = {".git", "build", ".gradle", "node_modules", "__pycache__", ".venv"}

# A reference from inside /contract out to a platform directory.
PLATFORM_REFERENCE = re.compile(
    r"""(?:^|[\s"'(/])(?:\.\./)*(?:android|web)/""",
    re.IGNORECASE | re.MULTILINE,
)

# A schema declaration. Any of these outside /contract means a second copy.
SCHEMA_DECLARATION = re.compile(
    r"\bCREATE\s+(?:TABLE|VIEW|TRIGGER|INDEX)\b", re.IGNORECASE
)

# Where a schema declaration is legitimate outside /contract. Kept deliberately
# short. Every entry is a file that talks about the schema rather than defining
# one, and adding to this list should feel like a decision.
SCHEMA_ALLOWED = {
    # The checks themselves name the pattern they look for.
    "tools/checks/check_contract_isolation.py",
    # Creates a throwaway trigger in an in-memory database to prove that a
    # failing change log write rolls the data write back with it. It declares
    # no table and no column.
    "tools/checks/check_schema.py",
}

# Test sources are exempt as a category rather than file by file.
#
# The thing this check exists to prevent is production code declaring tables,
# because that is what makes the second platform a reimplementation. A test that
# asserts the schema contains "CREATE TABLE IF NOT EXISTS change_log", or that
# counts "CREATE TRIGGER" occurrences, is doing the opposite: it is holding the
# real schema to its contract. Flagging those would train whoever reads this
# check to ignore it, which is worse than not having it.
#
# A test cannot substitute a schema for the app either. The app builds its
# database from the copied asset, and which file that is comes from the build,
# not from a test source set.
TEST_PATH_MARKERS = ("/src/test/", "/src/androidtest/", "/tests/", "/test/")

# Agent definitions and hooks are instructions, not source. The sweeper's
# definition necessarily quotes the rule it sweeps for, which is the same
# situation as the checks quoting the pattern they look for. Nothing under
# .claude is compiled, packaged, or read by the app, so a schema could not
# reach a device from here even if someone put one in.
INSTRUCTION_PATH_MARKERS = ("/.claude/",)

TEXT_SUFFIXES = {".md", ".sql", ".json", ".yml", ".yaml", ".kt", ".kts", ".py",
                 ".js", ".ts", ".html", ".xml", ".sh"}


def iter_files(base):
    if not base.exists():
        return
    for path in sorted(base.rglob("*")):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        yield path


def main():
    problems = []
    contract_files = 0

    # Rule one. Nothing in /contract points at a platform.
    for path in iter_files(CONTRACT):
        contract_files += 1
        relative = path.relative_to(ROOT).as_posix()
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for match in PLATFORM_REFERENCE.finditer(text):
            line = text[: match.start()].count("\n") + 1
            snippet = text.splitlines()[line - 1].strip()[:110]
            # The data contract document itself describes the layout, naming the
            # platform folders. Describing them is not importing from them.
            if path.name == "DATA-CONTRACT.md":
                continue
            problems.append(
                f"{relative}:{line}: /contract references a platform directory\n"
                f"      {snippet}"
            )

    if contract_files == 0:
        problems.append("contract/ has no files, so the contract does not exist yet")

    # Rule two. No second copy of the schema outside /contract.
    outside = 0
    for path in iter_files(ROOT):
        relative = path.relative_to(ROOT).as_posix()
        if relative.startswith("contract/"):
            continue
        if relative in SCHEMA_ALLOWED:
            continue
        lowered = f"/{relative.lower()}"
        if any(marker in lowered for marker in TEST_PATH_MARKERS):
            continue
        if any(marker in lowered for marker in INSTRUCTION_PATH_MARKERS):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        outside += 1
        match = SCHEMA_DECLARATION.search(text)
        if match:
            line = text[: match.start()].count("\n") + 1
            problems.append(
                f"{relative}:{line}: a schema declaration outside /contract. "
                f"The schema lives in contract/schema.sql and is copied in at "
                f"build time. A second copy is what makes the two platforms drift."
            )

    if problems:
        print(f"Contract isolation check failed. {len(problems)} problems.\n")
        for problem in problems:
            print(f"  {problem}")
        print("\nSee contract/DATA-CONTRACT.md section 2.")
        return 1

    print(
        f"Contract isolation check passed. {contract_files} files in /contract, "
        f"none referencing a platform. {outside} files outside, none declaring a schema."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
