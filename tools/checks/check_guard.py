#!/usr/bin/env python3
"""The destructive command guard still refuses what it must, and nothing else.

**Written because the guard was changed**, 2026-08-13 and #323. It used to
search the whole command text, so it fired on prose naming a blocked command:
a sentence for `HANDOFF.md` about the uninstall on the blocklist was refused,
and the guard even refused the edit that fixed it. It matches at a command
position now.

**A change to a safety guard needs a test that fails when the guard stops
guarding**, and this repository had none: the guard was inert for a week once,
per D29, and looked installed the whole time. Every row below is run through
the real hook, the same way the session runs it, with a payload on stdin.

**Both halves matter equally.** The refusals prove it still guards; the passes
prove it does not fire on documentation, which is the failure this file was
written for. A guard nobody can write about is a guard that quietly edits the
record.

Kamsiob, AGPL-3.0.
"""

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
HOOK = ROOT / ".claude/hooks/block-destructive.py"

# Every one of these must be refused. Each is either a command that has actually
# been attempted here or the same thing wearing a prefix a shell still runs.
MUST_REFUSE = [
    "rm -rf build",
    "rm -fr /tmp/x",
    "cd android && rm -rf .gradle",
    "sudo rm -rf /var/tmp/thing",
    "find . -name '*.tmp' -exec rm -rf {} +",
    "git reset --hard origin/main",
    "git checkout .",
    "git checkout -- HANDOFF.md",
    "git clean -fd",
    "git push --force",
    "git push origin +main:main",
    "adb uninstall com.kamsiob.healthtrail",
    "adb -s SERIALNUMBER uninstall com.kamsiob.healthtrail",
    "adb shell pm uninstall com.kamsiob.healthtrail",
    "adb shell pm clear com.kamsiob.healthtrail",
    "gh repo delete Kamsiob/health-trail",
    "dd if=/dev/zero of=/dev/sda",
    "echo hello > /dev/sda",
    # Newline separated, because a heredoc is not the only way to send two.
    "cd android\nrm -rf app/build",
    # **A heredoc fed to a shell is a shell script**, so its body is read the
    # same way as anything typed out. Only a body going to something else, a
    # commit message or a documentation file, is treated as data.
    "bash <<'EOF'\nrm -rf build\nEOF",
    "sh <<EOF\ngit reset --hard\nEOF",
]

# Every one of these must pass. The first several are the prose this guard used
# to refuse, and they are sentences this repository actively wants written.
MUST_PASS = [
    "python3 - <<'PY'\ns = 'the uninstall is on the blocklist, per D50'\nPY",
    "python3 - <<'PY'\ns = 'connectedAndroidTest removes the app when it finishes'\nPY",
    "git commit -m 'Never run git reset --hard here, fix forward instead'",
    "gh issue comment 12 --body 'the fix is not to git checkout . but to fix forward'",
    "echo 'rm -rf is on the blocked list' >> notes.md",
    # Ordinary work that happens to contain a blocked word inside a longer name.
    "./gradlew :app:connectedDebugAndroidTest",
    "git status --porcelain",
    "git restore --staged HANDOFF.md",
    "adb install -r android/app/build/outputs/apk/debug/app-debug.apk",
    "tools/verify.sh --device",
    # The commit message that could not be written while #323 was open, and the
    # sentence that closed it.
    (
        "git commit -F - <<'MSG'\n"
        "It refused the edit that fixed it: find . -exec rm -rf {} + and\n"
        "adb shell pm uninstall are named here on purpose.\n"
        "MSG"
    ),
    "gh issue comment 323 --body-file - <<'MD'\nrm -rf and git clean -fd\nMD",
]


def decide(command: str) -> int:
    payload = json.dumps(
        {"tool_name": "Bash", "tool_input": {"command": command}, "cwd": str(ROOT)}
    )
    result = subprocess.run(
        [sys.executable, str(HOOK)],
        input=payload,
        capture_output=True,
        text=True,
        check=False,
    )
    return result.returncode


def main() -> int:
    if not HOOK.is_file():
        print("Guard check failed: the hook is not where the settings point.")
        return 1

    wrong = []
    for command in MUST_REFUSE:
        if decide(command) != 2:
            wrong.append(f"let through, and must not be: {command!r}")
    for command in MUST_PASS:
        if decide(command) == 2:
            wrong.append(f"refused, and must not be: {command!r}")

    if wrong:
        print(f"Guard check failed. {len(wrong)} case(s) decided wrongly.")
        for problem in wrong:
            print(f"  {problem}")
        print(
            "\nThe guard is RUN-SAFETY.md section 1.1. It may become stricter "
            "freely. It may only become looser deliberately, and this file is "
            "where that decision is written down."
        )
        return 1

    print(
        f"Guard check passed. {len(MUST_REFUSE)} destructive commands refused, "
        f"{len(MUST_PASS)} ordinary ones and sentences about them let through."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
