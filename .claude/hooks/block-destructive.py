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
#
# **Matched at a command position rather than anywhere in the text**, since
# 2026-08-13 and #323. The guard used to search the whole string, so it fired on
# prose that merely names a blocked command: a sentence going into `HANDOFF.md`
# about the uninstall on the blocklist was refused, and the workaround was to
# write that file another way. **A guard that fires on documentation teaches the
# next session to avoid writing the word**, which is exactly the knowledge these
# files exist to carry. It refused the edit that introduced this comment, too.
#
# **It is not weakened.** The command it correctly refused earlier that same
# day, a real uninstall of the app package, sits at the start of its own
# statement and is still refused, as is one behind sudo, xargs, -exec, a
# subshell, or an &&. What is no longer refused is the same words inside a
# sentence, which no shell would ever run.
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
        # **`pm clear` rather than `adb shell pm clear`**, because the matcher
        # strips `adb shell` to find the command it is actually running. Typed
        # either way, on the host or in a device shell, it is the same wipe.
        r"\bpm\s+clear\b",
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

# A statement ends at a newline or at one of the shell's own separators, and a
# find's -exec begins one in the middle of another command. Nothing here has to
# be a full parse: it only has to find where a command could begin.
#
# **Both of these were found by the check rather than by thinking**: the first
# version of this missed `find . -exec rm -rf {} +` and `adb shell pm uninstall`,
# which are a real removal and a real uninstall.
SEPARATORS = re.compile(
    r"\n|;|&&|\|\||(?<![>|])\|(?!\|)|&(?!&)|\s-execdir\s|\s-exec\s",
)

# What may sit in front of a command and leave it a command. sudo, xargs, a
# subshell, the then, do and else of a conditional, and anything that hands a
# command to another shell: `sh -c`, `su -c`, and `adb shell`, which is how
# every destructive thing on the phone is actually spelled.
PREFIXES = re.compile(
    r"^(?:\s*(?:sudo|doas|xargs|env|nohup|time|command|builtin|exec|then|do|else)\s+"
    r"|\s*adb(?:\s+-s\s+\S+)?\s+shell\s+"
    r"|\s*(?:sh|bash|zsh|su)\s+-c\s+"
    r"|\s*[(!{]\s*|\s*\$\(\s*|\s*`\s*|\s*[\"']\s*)*",
    re.IGNORECASE,
)

# The rules that are about a redirection or an argument rather than about the
# command word, so they are searched anywhere in the text rather than anchored
# to the start of a statement.
UNANCHORED = ("of=/dev/", "/dev/(sd")


# A heredoc, and the word that opens the statement carrying it.
HEREDOC = re.compile(r"<<-?\s*(['\"]?)(\w+)\1")
SHELLS = ("bash", "sh", "zsh", "dash", "ksh", "source", "eval")


def without_heredoc_bodies(command: str) -> str:
    """The command with the contents of its heredocs removed.

    **A heredoc body is data, not a command**, and this is the other half of
    #323: a commit message, a documentation file or an issue comment written
    this way names blocked commands on purpose, and the guard used to refuse the
    lot. Removing the body is what lets those sentences be written.

    **Unless the heredoc feeds a shell.** `bash <<EOF` runs every line of it, so
    that body is exactly as dangerous as the same lines typed out and it stays
    in the text the rules see. What is dropped is what a heredoc is nearly
    always used for here: prose on its way to a file, a commit, or an issue.

    A body fed to something that is not a shell but can still run one, `python3`
    and its `os.system`, is dropped with the rest. That is a deliberate limit
    rather than an oversight: this guard reads shell, and a language that can
    shell out is past what a regular expression should pretend to police.
    """
    lines = command.split("\n")
    kept: list[str] = []
    skipping_until: str | None = None
    for line in lines:
        if skipping_until is not None:
            if line.strip() == skipping_until:
                skipping_until = None
            continue
        kept.append(line)
        found = HEREDOC.search(line)
        if found:
            opener = PREFIXES.sub("", line).lstrip().split(" ")[0].lower()
            if opener not in SHELLS:
                skipping_until = found.group(2)
    return "\n".join(kept)


def statements(command: str):
    """Every place a command could begin, with its leading noise removed."""
    for piece in SEPARATORS.split(without_heredoc_bodies(command)):
        if piece is None:
            continue
        yield PREFIXES.sub("", piece).lstrip()


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
        anchored = not any(mark in pattern.pattern for mark in UNANCHORED)
        hit = (
            any(pattern.match(piece) for piece in statements(command))
            if anchored
            else pattern.search(without_heredoc_bodies(command))
        )
        if hit:
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
