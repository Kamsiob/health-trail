#!/usr/bin/env python3
"""Guard 3 of RUN-SAFETY.md section 1: cap retries at three and escalate.

The failure this prevents is the documented loop where an agent fixes, checks,
sees the same error, fixes again, and repeats twenty times, reporting success
each round. It is not dishonest. It is fixing the same wrong thing repeatedly,
and without a cap it can consume an entire run.

Usage, called by the session before each attempt at something that has already
failed once:

    .claude/hooks/retry-guard.py attempt <label> "what I am about to try"
    .claude/hooks/retry-guard.py clear   <label>
    .claude/hooks/retry-guard.py status  [label]

`attempt` prints the attempt number and exits 0 for attempts 1, 2, and 3. On
the fourth it exits 1 and prints the escalation instruction, which is to write
what was tried and what happened to the BLOCKED section of DECISIONS.md and
start the next item.

State lives in .claude/retry-state.json, which is gitignored, because it is
about this run rather than about the project.

Kamsiob, AGPL-3.0.
"""

import json
import os
import sys
from datetime import datetime, timezone

LIMIT = 3
ROOT = os.environ.get(
    "CLAUDE_PROJECT_DIR",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")),
)
STATE_PATH = os.path.join(ROOT, ".claude", "retry-state.json")


def load() -> dict:
    try:
        with open(STATE_PATH, "r", encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, json.JSONDecodeError):
        return {}


def save(state: dict) -> None:
    os.makedirs(os.path.dirname(STATE_PATH), exist_ok=True)
    with open(STATE_PATH, "w", encoding="utf-8") as handle:
        json.dump(state, handle, indent=2, sort_keys=True)
        handle.write("\n")


def usage() -> int:
    sys.stderr.write(__doc__ or "")
    return 2


def main(argv: list) -> int:
    if len(argv) < 2:
        return usage()

    action = argv[1]
    state = load()

    if action == "status":
        if len(argv) >= 3:
            entry = state.get(argv[2])
            if not entry:
                print(f"{argv[2]}: no attempts recorded")
                return 0
            print(f"{argv[2]}: {entry['count']} of {LIMIT} attempts used")
            for index, note in enumerate(entry["attempts"], start=1):
                print(f"  {index}. [{note['at']}] {note['note']}")
            return 0
        if not state:
            print("No attempts recorded.")
            return 0
        for label, entry in sorted(state.items()):
            print(f"{label}: {entry['count']} of {LIMIT}")
        return 0

    if action == "clear":
        if len(argv) < 3:
            return usage()
        state.pop(argv[2], None)
        save(state)
        print(f"Cleared retry state for: {argv[2]}")
        return 0

    if action != "attempt" or len(argv) < 3:
        return usage()

    label = argv[2]
    note = argv[3] if len(argv) > 3 else ""
    entry = state.setdefault(label, {"count": 0, "attempts": []})
    entry["count"] += 1
    entry["attempts"].append(
        {
            "at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "note": note,
        }
    )
    save(state)

    if entry["count"] > LIMIT:
        sys.stderr.write(
            f"Retry cap reached for: {label}\n"
            f"{entry['count'] - 1} attempts have already failed:\n"
        )
        for index, past in enumerate(entry["attempts"][:-1], start=1):
            sys.stderr.write(f"  {index}. [{past['at']}] {past['note']}\n")
        sys.stderr.write(
            "\nStop working on this now. Write to the BLOCKED section of "
            "DECISIONS.md what was attempted, what happened each time, and what "
            "you would try next. Open or label the corresponding issue as "
            "blocked with the same detail. Then start the next item in the "
            "HANDOFF.md remaining work inventory.\n"
            "Ref: RUN-SAFETY.md section 1.3, CLAUDE.md hard rule 9.\n"
        )
        return 1

    remaining = LIMIT - entry["count"]
    print(
        f"Attempt {entry['count']} of {LIMIT} for: {label}"
        f" ({remaining} remaining before escalation to BLOCKED)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
