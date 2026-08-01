#!/usr/bin/env python3
"""Every hook command that interpolates a path is quoted, and every script it
names exists and is executable.

**This project lives at a path containing spaces.** `/var/home/Kamsiob/Kamiob
Apps/-- Android/Health Trail`. An unquoted `${CLAUDE_PROJECT_DIR}/...` in a hook
command is split by the shell on those spaces, the executable is never found,
and the hook exits 127.

**A PreToolUse hook blocks on exit 2 and only on exit 2.** So 127 does not read
as an error. It reads as "this hook had nothing to say", and the command it was
supposed to refuse runs. The guard becomes a no-op wearing the shape of a guard,
and it produces no output at all while doing it.

That is not hypothetical. It is D49. The destructive command guard was inert
from the day it was written, through every session, and nothing surfaced it
until `adb shell pm clear` reached the owner's phone. The pre compaction state
save carried the identical defect and has never once run.

This check exists because the failure is silent by construction. There is no
output when a guard does not fire, so nothing but a check like this one will
notice it happening again.

**It cannot prove a hook fires**, which is the thing that actually matters, and
it does not claim to. Only running a blocked command and being refused proves
that, and `RUN-SAFETY.md` section 1.1 says to do exactly that at the start of
every session. What this catches is the specific mistake that made the guards
inert, so it cannot be reintroduced quietly.

Usage: python3 tools/checks/check_hook_quoting.py

Kamsiob, AGPL-3.0.
"""

import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SETTINGS = ROOT / ".claude" / "settings.json"

# ${VAR} or $VAR anywhere in a command string. Any of these can expand to
# something containing a space, so any of them has to be quoted.
INTERPOLATION = re.compile(r"\$\{?[A-Za-z_][A-Za-z0-9_]*\}?")

# The placeholder wrapped in double quotes, which is the correct form.
QUOTED = re.compile(r'"[^"]*\$\{?[A-Za-z_][A-Za-z0-9_]*\}?[^"]*"')


def commands(settings: dict):
    """Every command hook in the file, with a path saying where it came from."""
    for event, groups in (settings.get("hooks") or {}).items():
        for gi, group in enumerate(groups or []):
            for hi, hook in enumerate(group.get("hooks") or []):
                if hook.get("type") != "command":
                    continue
                where = f"{event}[{gi}].hooks[{hi}]"
                yield where, hook


def resolve(command: str) -> Path | None:
    """The script a command runs, if it names one inside this repository."""
    match = re.search(r"\$\{?CLAUDE_PROJECT_DIR\}?(/[^\"'\s]+)", command)
    if not match:
        return None
    return ROOT / match.group(1).lstrip("/")


def main() -> int:
    if not SETTINGS.is_file():
        print(f"  no {SETTINGS.relative_to(ROOT)}, nothing to check")
        return 0

    try:
        settings = json.loads(SETTINGS.read_text(encoding="utf-8"))
    except json.JSONDecodeError as problem:
        # Worth its own message: malformed settings silently disable every
        # setting in the file, hooks included, with no error anywhere.
        print(f"  FAIL {SETTINGS.relative_to(ROOT)} is not valid JSON: {problem}")
        print("       A malformed settings file disables every hook in it silently.")
        return 1

    problems = []
    checked = 0

    for where, hook in commands(settings):
        command = hook.get("command", "")
        checked += 1

        # The exec form passes each argument as its own argv element and never
        # reaches a shell, so quoting is neither needed nor meaningful there.
        exec_form = "args" in hook

        if not exec_form:
            for found in INTERPOLATION.finditer(command):
                span = found.group(0)
                if not any(
                    q.start() <= found.start() and found.end() <= q.end()
                    for q in QUOTED.finditer(command)
                ):
                    problems.append(
                        f"{where}: {span} is unquoted in\n"
                        f"           {command}\n"
                        f"         The shell splits it on spaces, the executable is not\n"
                        f"         found, and the hook exits 127 instead of blocking.\n"
                        f'         Write it as "{span}/..." with the quotes inside the\n'
                        f"         JSON string. See D49."
                    )

        script = resolve(command)
        if script is not None:
            if not script.is_file():
                problems.append(
                    f"{where}: names {script.relative_to(ROOT)}, which does not exist.\n"
                    f"         A hook pointing at a missing script exits non-zero and\n"
                    f"         blocks nothing."
                )
            elif not os.access(script, os.X_OK):
                problems.append(
                    f"{where}: {script.relative_to(ROOT)} is not executable.\n"
                    f"         chmod +x it, or the hook cannot run."
                )

    if problems:
        print(f"  FAIL {len(problems)} problem(s) in {SETTINGS.relative_to(ROOT)}:\n")
        for problem in problems:
            print(f"    - {problem}\n")
        print("  A hook with any of these looks installed and protects nothing.")
        return 1

    print(f"  {checked} hook command(s) quoted correctly, every script present and executable")
    print("  Note: this proves the wiring is well formed, not that a hook fires.")
    print("  Only being refused a blocked command proves that. RUN-SAFETY.md 1.1.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
