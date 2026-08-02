#!/usr/bin/env python3
"""Guard 1 of RUN-SAFETY.md section 1: refuse commands that destroy work.

Runs as a PreToolUse hook on the Bash tool. Reads the hook payload on stdin,
inspects tool_input.command, and exits 2 with an explanation when the command
matches anything on the blocked list. Exit code 2 blocks the call and returns
stderr to the session as the reason.

It refuses. It never asks, because in an unattended run there is nobody to ask.
When a blocked command genuinely seems necessary, that goes to the BLOCKED
section of DECISIONS.md and the run continues on something else.

Kamsiob, AGPL-3.0.
"""

import datetime
import json
import os
import re
import sys

# Every invocation is logged, whether it blocks or passes. This exists because
# the guard was inert for a week and looked installed the whole time: a guard
# that does not fire looks exactly like a guard with nothing to do. The log is
# the difference. If this file has no line for a session, the hook did not run
# in that session, and that is a wiring fault rather than a quiet clean run.
LOG_PATH = os.path.expanduser("~/.claude/health-trail-guard.log")

# Each rule is a compiled pattern plus the plain reason returned to the session.
# Patterns are matched against the whole command string, which may contain
# several statements joined by ; && || or newlines.
RULES = [
    (
        r"\brm\s+(-[a-zA-Z]*\s+)*-[a-zA-Z]*[rR][a-zA-Z]*f|\brm\s+(-[a-zA-Z]*\s+)*-[a-zA-Z]*f[a-zA-Z]*[rR]",
        "rm -rf destroys work irreversibly. Delete a specific file without -r, "
        "or use the build tool's own clean task.",
    ),
    (
        r"\bgit\s+reset\s+(--\S+\s+)*--hard\b",
        "git reset --hard discards the working tree. Fix forward instead.",
    ),
    (
        r"\bgit\s+checkout\s+(--\s+)?(\.|\*)(\s|$)",
        "git checkout . discards uncommitted work. Fix forward instead.",
    ),
    (
        r"\bgit\s+checkout\s+--\s",
        "git checkout -- <path> discards uncommitted work in that path. "
        "Fix forward instead.",
    ),
    (
        r"\bgit\s+restore\s+(?!.*--staged\b)",
        "git restore discards uncommitted work. Fix forward instead. "
        "git restore --staged is permitted because it only unstages.",
    ),
    (
        r"\bgit\s+clean\b.*-[a-zA-Z]*[dfx]",
        "git clean removes untracked files, which are often the only copy. "
        "Delete a specific file by name if it is genuinely unwanted.",
    ),
    (
        r"\bgit\s+push\b.*(--force(?!-with-lease)|(\s|^)-f(\s|$))",
        "Force pushing rewrites published history and can destroy commits "
        "other clones depend on.",
    ),
    (
        r"\bgit\s+push\b.*\s\+[^\s]*:",
        "A refspec beginning with + is a force push. Blocked for the same reason.",
    ),
    (
        r"\bgit\s+branch\s+(-[a-zA-Z]*\s+)*-D\b|\bgit\s+branch\s+--delete\s+--force\b",
        "git branch -D deletes a branch that may hold unmerged commits.",
    ),
    (
        r"\bgit\s+rebase\b",
        "git rebase rewrites history. This project never rewrites history, "
        "published or not. Merge instead.",
    ),
    (
        r"\bgit\s+filter-branch\b|\bgit\s+filter-repo\b",
        "Rewriting history across the whole repository is never done here.",
    ),
    (
        r"\bgit\s+commit\b.*--amend\b",
        "Amending rewrites a commit. If it was already pushed this rewrites "
        "published history. Make a follow-up commit instead.",
    ),
    (
        r"\bgit\s+reflog\s+(expire|delete)\b|\bgit\s+gc\b.*--prune=now",
        "Expiring the reflog removes the last safety net for recovering "
        "lost commits.",
    ),
    (
        # Narrowed to spare the instrumentation package, which is not the app.
        # A package id ending in .test is the androidTest APK that Gradle
        # installs to run instrumented tests. Removing it is cleanup and is
        # required, so that exactly one real package remains on the device.
        # Uninstalling anything else still destroys the data the in-place
        # upgrade path exists to protect.
        r"\b(?:adb\s+(?:-s\s+\S+\s+)?uninstall|pm\s+uninstall)\b"
        r"(?!(?:\s+-\S+)*\s+\S*\.test\b)",
        "Uninstalling destroys the app data that the in-place upgrade path "
        "exists to protect. Every install after the first is an upgrade. "
        "If a migration appears to need a clean install, that is a migration "
        "bug and the migration gets fixed. Only a package id ending in .test, "
        "which is the instrumentation APK rather than the app, may be removed.",
    ),
    (
        r"\badb\s+shell\s+pm\s+clear\b",
        "pm clear wipes app data. Destructive data tests run on an emulator "
        "against a fixture, never against a real installation.",
    ),
    (
        r"\bgh\s+repo\s+delete\b|\bgh\s+release\s+delete\b",
        "Deleting a repository or a published release destroys the public "
        "record of the project.",
    ),
    (
        r">\s*/dev/(sd|nvme|mmcblk)|\bmkfs\b|\bdd\s+.*of=/dev/",
        "Writing to a block device can destroy the machine's storage.",
    ),
]

COMPILED = [(re.compile(p, re.IGNORECASE), reason) for p, reason in RULES]


def log(decision: str, command: str, cwd: str) -> None:
    """Append one line per invocation. Never raises, because a log that cannot
    be written must not stop the guard from guarding."""
    try:
        stamp = datetime.datetime.now().isoformat(timespec="seconds")
        os.makedirs(os.path.dirname(LOG_PATH), exist_ok=True)
        with open(LOG_PATH, "a", encoding="utf-8") as handle:
            handle.write(f"{stamp}\t{decision}\tcwd={cwd}\t{command.strip()[:200]}\n")
    except OSError:
        pass


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        # A payload we cannot read is not grounds for blocking real work.
        return 0

    if payload.get("tool_name") != "Bash":
        return 0

    command = (payload.get("tool_input") or {}).get("command") or ""
    cwd = payload.get("cwd") or ""
    if not command:
        return 0

    for pattern, reason in COMPILED:
        if pattern.search(command):
            log("BLOCKED", command, cwd)
            sys.stderr.write(
                "Blocked by the Health Trail destructive command guard "
                "(RUN-SAFETY.md section 1.1).\n\n"
                f"Command: {command.strip()[:400]}\n\n"
                f"Why: {reason}\n\n"
                "This guard refuses rather than asking, because an unattended "
                "run has nobody to ask. If this command is genuinely necessary, "
                "record it under BLOCKED in DECISIONS.md and continue with "
                "something else.\n"
            )
            return 2

    log("pass", command, cwd)
    return 0


if __name__ == "__main__":
    sys.exit(main())
