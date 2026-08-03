#!/usr/bin/env python3
"""Every source file is text, so that searching the repository actually
searches the whole repository.

**A single NUL byte makes a file binary to grep, and grep says nothing about
it.** It does not warn, it does not list the file, it simply returns no match.
`git grep` behaves the same way. The file keeps compiling, the tests keep
passing, and it quietly stops existing for every tool that searches by text.

That is not hypothetical, and the cost was immediate. Three files carried a
literal NUL inside the string `"SQLite format 3\\u0000"`, which is the SQLite
header magic, written as a raw byte rather than as an escape. One of them was
`HealthTrailDatabase.kt`. On 2026-08-02 a session auditing the test suite ran

    git grep -n "Migrations.run"

got hits only in `MigrationTest.kt`, and concluded that the migration mechanism
was fully built, thoroughly tested, and never called by the app. **That
conclusion was wrong.** The call is on line 114 of `HealthTrailDatabase.kt`,
and it was invisible because two bytes elsewhere in the file made the whole
file binary. The wrong conclusion was about to become a filed defect.

The same blindness applies to every audit anyone runs by searching: a compliance
sweep, a rename, a check for a forbidden call, a review of who touches the
Keystore. **A file that cannot be searched is a file exempt from every check
that searches, and nothing anywhere reports that exemption.**

The fix in the source is one character: write the escape, `\\u0000`, and the
compiled result is byte for byte identical while the file on disk stays text.

This check exists because the failure is silent by construction, which is the
same reason `check_hook_quoting.py` exists. D66.

Usage: python3 tools/checks/check_text_sources.py

Kamsiob, AGPL-3.0.
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# Everything a person or a check would expect to be able to search.
SUFFIXES = {
    ".kt", ".kts", ".java", ".py", ".sh", ".sql", ".json", ".md",
    ".xml", ".html", ".css", ".js", ".ts", ".yml", ".yaml", ".toml",
    ".pro", ".properties", ".txt",
}

# Directories that hold generated or vendored bytes rather than source.
SKIP = {".git", "build", "node_modules", "__pycache__", ".gradle", ".idea"}


def sources():
    for path in ROOT.rglob("*"):
        if not path.is_file() or path.suffix not in SUFFIXES:
            continue
        if any(part in SKIP for part in path.relative_to(ROOT).parts):
            continue
        yield path


def main() -> int:
    problems = []

    for path in sources():
        data = path.read_bytes()
        where = path.relative_to(ROOT)

        if b"\x00" in data:
            # Name the lines, because the whole point is that grep will not.
            lines = [
                i for i, line in enumerate(data.split(b"\n"), 1) if b"\x00" in line
            ]
            problems.append(
                f"{where}: {data.count(b'\x00')} NUL byte(s) on line(s) "
                f"{', '.join(str(n) for n in lines)}. "
                f"grep and git grep skip this file silently, so it is exempt "
                f"from every search based check. Write the escape instead: "
                f"\\u0000 in Kotlin and Java, \\0 or \\x00 in Python."
            )
            continue

        try:
            data.decode("utf-8")
        except UnicodeDecodeError as problem:
            problems.append(
                f"{where}: not valid UTF-8, {problem}. "
                f"Tools that read source as text will fail or skip it."
            )

    if problems:
        print("Source files that cannot be searched as text:\n", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        print(
            f"\n{len(problems)} file(s). A file grep cannot see is a file no "
            f"search based check covers.",
            file=sys.stderr,
        )
        return 1

    checked = sum(1 for _ in sources())
    print(f"All {checked} source files are searchable text.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
