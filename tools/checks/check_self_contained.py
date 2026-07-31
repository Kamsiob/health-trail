#!/usr/bin/env python3
"""Fail if this repository names anything that is not this project.

This repository is public. It should read as a self contained project, with no
evidence of what else happens to live on the machine it was built on: no other
product names, no other project's emulator, keystore, build output, or paths.

**Why the forbidden words are stored as hashes rather than as a list.** A check
that held the list in plain text would itself put those names into the
repository, which is the exact thing it exists to prevent. So each token is
stored as a SHA-256 of its lowercase form. The check lowercases every word it
finds and compares hashes. It can therefore recognize a name it cannot spell.

The consequence, stated plainly because it is a real limitation: a failure here
tells you the offending word and where it is, since it found it in your file,
but this file alone will never tell you what the watched words are. That is the
intended trade.

**Adding a token,** if another name ever needs watching:

    python3 -c "import hashlib;print(hashlib.sha256(b'thename').hexdigest())"

and paste the result into WATCHED below with a neutral comment.

Exit 0 when clean, 1 with the offending file, line, and word otherwise.

Kamsiob, AGPL-3.0.
"""

import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# SHA-256 of the lowercase form of each watched token. Deliberately opaque.
WATCHED = {
    "1c6c78e1ecef1dcc75f1599cfad87109f04d76476a3f4012136ea030db7e53ea",
    "2436f87c94d01124dbbd8a677da1b905c154a7343fdee49257f30ca3442e18c2",
    "2ded30f6572404577875d4e221541d67c39e8a30b9c55dde385d1014fe67072e",
    "40eec00a2941d6324ff45623bf9b628c0b9de3d7c01f1d8f04bf9b81603f0811",
    "786e98a63c6688977a22747e5de97a29e31e4447ab946a532e974a51bb465acb",
    "81839a222e1649bb54d67d86a449a5590c6f3a7436f5af7ad312a4f8d842dbd2",
    "951431a6fe67a1b053263b32e95a36cd1d9ba12a35ba1d3c46f99114bc177439",
    "9905a36a81d4ad63900ffa69c8e51a8726fd96bcdf600aff2be5c0463320017c",
    "a5b892799b0e8ca6c9a23c1ab9efea8d7dcd3bb9f076c85bd8668f065b7d02a8",
}

SKIP_DIRS = {".git", "build", ".gradle", "node_modules", "__pycache__", ".venv"}

# Files inherited with the project brief rather than written for it. They are
# committed as the public record of what was handed over and are not edited to
# suit a rule introduced afterward.
EXEMPT = {
    "kamsiob-project-template.md",
    "tools/checks/check_self_contained.py",
}

TEXT_SUFFIXES = {".md", ".txt", ".json", ".yml", ".yaml", ".xml", ".kt", ".kts",
                 ".py", ".sh", ".sql", ".html", ".css", ".js", ".ts", ".toml",
                 ".properties", ".gitignore", ".pro"}

WORD = re.compile(r"[A-Za-z][A-Za-z0-9_-]{2,}")


def tracked_files():
    """Exactly what the repository contains.

    Scanning the working tree would also read gitignored session state and
    build output, which never reach anyone and would produce failures nobody
    can act on. The rule is about what is published, so the file list comes
    from git.
    """
    try:
        out = subprocess.run(
            ["git", "-C", str(ROOT), "ls-files"],
            capture_output=True, text=True, check=True,
        )
        return [ROOT / line for line in out.stdout.splitlines() if line]
    except (subprocess.CalledProcessError, FileNotFoundError):
        return [p for p in sorted(ROOT.rglob("*")) if p.is_file()]


def main():
    problems = []
    scanned = 0

    for path in tracked_files():
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        relative = path.relative_to(ROOT).as_posix()
        if relative in EXEMPT:
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES and path.name != ".gitignore":
            continue

        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except (UnicodeDecodeError, OSError):
            continue
        scanned += 1

        for number, line in enumerate(lines, start=1):
            for match in WORD.finditer(line):
                word = match.group(0).lower()
                if hashlib.sha256(word.encode()).hexdigest() in WATCHED:
                    problems.append(
                        f"{relative}:{number}: names something that is not this "
                        f"project: {match.group(0)!r}"
                    )

    if problems:
        print(f"Self contained check failed. {len(problems)} references.\n")
        for problem in problems:
            print(f"  {problem}")
        print(
            "\nThis repository is public and reads as a self contained project. "
            "Nothing else that lives on this machine is named in it, in any file, "
            "commit message, issue, or comment. Where a reference already sits in "
            "published history it stays there and the correction is noted in "
            "DECISIONS.md, because history is not rewritten here."
        )
        return 1

    print(f"Self contained check passed. {scanned} files, nothing outside this project named.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
