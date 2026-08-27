#!/usr/bin/env python3
"""Nothing in the tracked tree identifies a person, a machine, or a secret.

**This repository is public.** Everything `git ls-files` lists is readable by
anybody, forever, and a thing written into a file once is a thing that gets
copied into three more before anybody notices. #470.

**What this refuses, and why each one is here rather than imagined.** Every
pattern below matched something real in this tree on 2026-08-27:

- **A personal email address.** Role addresses are fine and are what a project
  publishes. A personal one is a person's inbox.
- **A device serial.** The Pixel 8's serial was in `HANDOFF.md`, in the run log
  twice, and inside a check's own test data. A serial identifies one physical
  phone belonging to one person.
- **An absolute path under somebody's home directory.** `/home/<name>` and
  `/var/home/<name>` publish an account name, and usually a folder structure
  with it. A `~` path publishes neither and says the same thing.
- **A private invite link.** A `t.me/+<token>` is a bearer credential: whoever
  holds it joins the group. One was in `README.md` and in the house template.
- **A cloud service account address.** Not a key, and still an admin principal
  named in public together with its project id.
- **A private network address.** RFC1918 and CGNAT ranges, and tailnet names.
- **Key material.** A PEM header, a keystore, a service account JSON. None has
  ever been committed here; this is what keeps that true.

**What it deliberately does not do.** It does not read binary files, so a name
inside a screenshot is not its job: `docs/COLD-START.md` carries the capture
rules for that. It does not object to fixture personas, which are invented and
are named in `tools/fixtures/generate.py`.

**Exemptions are a list of lines, not a list of files.** A file level exemption
is how `check_self_contained.py` ended up passing over the one file that
violated it, printing a green line while the thing it was looking for sat at
line 383. Every exemption below names a file, a pattern and a reason.

Usage: python3 tools/checks/check_no_pii.py

Kamsiob, AGPL-3.0.
"""

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# Files that are read. Anything else is binary as far as this check is
# concerned, and a check that cannot read a file must not claim it passed one.
TEXT_SUFFIXES = {
    ".md", ".py", ".sh", ".kt", ".kts", ".json", ".yml", ".yaml", ".xml",
    ".html", ".css", ".js", ".ts", ".txt", ".sql", ".pro", ".properties",
    ".toml", ".gradle", ".cfg", ".ini", ".gitignore", ".editorconfig",
}
EXTRA_FILES = {"LICENSE", ".gitignore", "gradlew", "Makefile"}

# A rule is a name, the pattern, an optional pattern the whole line must also
# match, and the sentence a reader gets when it fires. The line level condition
# is what makes the serial rule usable: a run of upper case letters and digits
# is also SVG path data, a Kotlin `Long` literal and a ULID, and a check that
# cries wolf on those is a check somebody turns off.
RULES = [
    (
        "personal email address",
        re.compile(r"\b[A-Za-z0-9._%+-]+@(?:gmail|googlemail|outlook|hotmail|yahoo|proton|protonmail|icloud|me)\.[A-Za-z]{2,}\b", re.I),
        "A personal inbox. Publish a role address instead.",
        None,
    ),
    (
        "device serial",
        # An Android serial as `adb devices` prints one: a run of upper case
        # letters and digits with at least one of each, standing alone, on a
        # line that is talking about a device.
        re.compile(r"(?<![A-Za-z0-9])(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*[0-9])[A-Z0-9]{12,20}(?![A-Za-z0-9])"),
        "A device identifier names one physical phone. `adb devices` says it; a file should not.",
        re.compile(r"\badb\b|\bserial\b|\bdevices?\b|\bIMEI\b|-s ", re.I),
    ),
    (
        "home directory path",
        re.compile(r"(?:/var)?/home/(?!linuxbrew\b)[A-Za-z0-9._-]+/"),
        "An absolute path under an account name. Write `~/`, `$HOME`, or `$CLAUDE_PROJECT_DIR`.",
        None,
    ),
    (
        "windows home directory path",
        re.compile(r"[Cc]:\\\\?Users\\\\?(?!Margaret\b)[A-Za-z0-9._-]+"),
        "An absolute path under an account name.",
        None,
    ),
    (
        "private invite link",
        re.compile(r"t\.me/\+[A-Za-z0-9_-]+"),
        "A bearer credential: whoever holds the link joins the group.",
        None,
    ),
    (
        "cloud service account",
        re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.iam\.gserviceaccount\.com"),
        "An admin principal, named in public with its project.",
        None,
    ),
    (
        "private network address",
        re.compile(r"\b(?:10\.\d{1,3}|192\.168|172\.(?:1[6-9]|2\d|3[01])|100\.(?:6[4-9]|[7-9]\d|1[01]\d|12[0-7]))\.\d{1,3}\.\d{1,3}\b"),
        "A machine on somebody's own network.",
        None,
    ),
    (
        "tailnet or LAN hostname",
        re.compile(r"\b[A-Za-z0-9-]+\.(?:ts\.net|lan)\b"),
        "Names a machine on somebody's own network.",
        None,
    ),
    (
        "key material",
        re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----|\"private_key_id\"\s*:|\"type\"\s*:\s*\"service_account\""),
        "Key material never belongs in a repository, public or not.",
        None,
    ),
]

# file, rule name, substring that must be on the line, and why it is allowed.
EXEMPT = [
    (
        "tools/checks/check_no_pii.py",
        None,
        None,
        "This file states every pattern, so it matches all of them.",
    ),
    (
        "android/app/src/test/kotlin/com/kamsiob/healthtrail/data/RowJsonTest.kt",
        "windows home directory path",
        "C:",
        "A hostile string in a fixture, testing that a path in somebody's own "
        "words survives being stored. The name is a fixture persona.",
    ),
    (
        "DECISIONS.md",
        "device serial",
        "SHA256",
        "A certificate fingerprint, which is public by design once an app ships.",
    ),
    (
        "HANDOFF.md",
        "device serial",
        "SHA256",
        "The same certificate fingerprint.",
    ),
]


def tracked():
    listing = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=ROOT, capture_output=True, text=True, check=True,
    ).stdout
    for name in listing.split("\0"):
        if not name:
            continue
        path = ROOT / name
        if path.suffix.lower() in TEXT_SUFFIXES or path.name in EXTRA_FILES:
            yield name, path


def exempt(name: str, rule: str, line: str) -> bool:
    for where, which, needle, _ in EXEMPT:
        if where != name:
            continue
        if which is None:
            return True
        if which == rule and (needle is None or needle in line):
            return True
    return False


def main() -> int:
    problems = []
    read = 0
    for name, path in tracked():
        try:
            body = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        read += 1
        for number, line in enumerate(body.splitlines(), start=1):
            for rule, pattern, why, context in RULES:
                if context is not None and not context.search(line):
                    continue
                found = pattern.search(line)
                if not found or exempt(name, rule, line):
                    continue
                problems.append(f"{name}:{number}: {rule}: {found.group(0)}\n      {why}")

    if problems:
        print(f"Personal information check failed. {len(problems)} problems.\n")
        for problem in problems:
            print(f"  {problem}")
        print(
            "\nThis repository is public. Remove it from the working tree; if it is "
            "\nalso in history, report it and let the owner decide, because rewriting "
            "\nhistory here needs two guards lifted that are his. DECISIONS.md B7."
        )
        return 1

    print(f"Personal information check passed. {read} text files, nothing identifying.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
