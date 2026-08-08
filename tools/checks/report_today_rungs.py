#!/usr/bin/env python3
"""Which states ladder rung each Today card lands on, at each fixture horizon.

**A report, not a gate, and the name says which.** It answers one question that
every card issue in milestone 1 asks and nothing could answer: *which fixture do
I load to see this rung on the phone?*

`DESIGN.md` 21.4 gives every card six rungs, and 23.1 says every one of them is
seen on the device at both themes, maximum font scale and right to left. The
acceptance criteria say "fixture data exists that produces each rung" and that
line was unchecked on all seventeen cards, because nobody knew. Generating six
notebooks by hand and walking seventeen cards on each is an hour on the phone
before any verifying starts. This is that hour, done in about twenty seconds,
with no phone at all.

**It reads the generated fixture, not the app.** The queries here mirror
`Repository.todayAnswers` and `Repository.todayAnswerForSource`. **That is a
second copy and copies drift**, which is exactly why this is a report: it is
guidance for somebody about to pick a fixture, never a claim about what the app
renders. If a rung it promises is not on the screen, believe the screen, and the
disagreement is itself worth knowing about.

Usage:
    python3 tools/checks/report_today_rungs.py
    python3 tools/checks/report_today_rungs.py --at day1 --at year5

Kamsiob, AGPL-3.0.
"""

from __future__ import annotations

import argparse
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GENERATE = ROOT / "tools" / "fixtures" / "generate.py"

# The same six the generator has. Named here rather than imported, so this file
# says out loud what it is about to build.
HORIZONS = ["day1", "day30", "month6", "year1", "year2", "year5"]

# The rungs, in `DESIGN.md` 21.4's order.
FULL = "full"
FEW = "few"
NONE_YET = "none yet"
PASSED = "passed"
CLOSED = "source closed"

# **Few means sparse enough that the card looks different.** 21.4's example is a
# measure with two entries drawing two dots, so the boundary is drawn where the
# card stops being a list and starts being a hint that the record is new.
FEW_AT_MOST = 2

# **Cards whose count is a yes or a no, so few and full do not mean anything.**
# The emergency card's count is whether there is one to hand over at all, which
# is zero or one forever. Reporting "no fixture produces full" for it would send
# somebody looking for a state that cannot exist.
YES_OR_NO = {"emergency_card"}

# **Rungs that cannot happen to a card that answers with a number.** A card
# saying how many bills are unsettled has no date to have gone by and no single
# source to close. 21.4 lists six rungs and the issue template asks for all six
# on all seventeen cards, which overstates it.
DATED_OR_SOURCED = {
    "next_up", "measure", "milestones", "trail_lately",
    "project_standing", "project_date", "project_steps",
}


def count(db, sql, *args):
    row = db.execute(sql, args).fetchone()
    return row[0] if row else 0


def rung_for_count(n):
    if n == 0:
        return NONE_YET
    if n <= FEW_AT_MOST:
        return FEW
    return FULL


def rungs(db, subject_id, now_ms):
    """Every card type and the rungs the fixture puts it on.

    **A set per type, because a layout may hold the same card twice.** The
    fixture carries two `project_steps` cards on purpose, one on an open project
    so its counts are visible and one on a finished project so the source-closed
    rung exists at all. The first draft of this kept one rung per type, so the
    second card silently overwrote the first and the report said the steps card
    was only ever closed. The fixture was fine; the reading of it was not.
    """
    out = {}

    def add(card_type, rung):
        out.setdefault(card_type, set()).add(rung)

    def counted(card_type, sql, *args):
        add(card_type, rung_for_count(count(db, sql, *args)))

    counted(
        "medications",
        "SELECT COUNT(*) FROM medication WHERE deleted_at IS NULL "
        "AND subject_id = ? AND stopped_edtf IS NULL",
        subject_id,
    )
    counted(
        "incidents",
        "SELECT COUNT(*) FROM incident WHERE deleted_at IS NULL "
        "AND subject_id = ? AND resolved_at IS NULL",
        subject_id,
    )
    counted(
        "unfiled",
        "SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL "
        "AND subject_id = ? AND is_unfiled = 1",
        subject_id,
    )
    counted(
        "money",
        "SELECT COUNT(*) FROM bill WHERE deleted_at IS NULL "
        "AND subject_id = ? AND state NOT IN ('paid', 'closed')",
        subject_id,
    )
    counted(
        "care_team",
        "SELECT COUNT(*) FROM person WHERE deleted_at IS NULL "
        "AND subject_id = ? AND archived_at IS NULL",
        subject_id,
    )
    counted(
        "recent_documents",
        "SELECT COUNT(*) FROM document WHERE deleted_at IS NULL AND subject_id = ?",
        subject_id,
    )
    counted(
        "standing_instructions",
        "SELECT COUNT(*) FROM standing_instruction WHERE deleted_at IS NULL "
        "AND subject_id = ? AND ended_edtf IS NULL",
        subject_id,
    )
    counted(
        "ask_next_time",
        "SELECT COUNT(*) FROM question WHERE deleted_at IS NULL "
        "AND subject_id = ? AND answer_text IS NULL",
        subject_id,
    )
    counted(
        "emergency_card",
        "SELECT COUNT(*) FROM emergency_card WHERE deleted_at IS NULL AND subject_id = ?",
        subject_id,
    )
    counted(
        "trail_lately",
        "SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL AND subject_id = ?",
        subject_id,
    )
    counted(
        "milestones",
        "SELECT COUNT(*) FROM milestone WHERE deleted_at IS NULL AND subject_id = ?",
        subject_id,
    )

    # **Next up is only what has not happened**, so a notebook whose every
    # appointment is in the past lands on the none-yet rung rather than the
    # passed one. That is the card's own rule and not a gap in the fixture.
    ahead = count(
        db,
        "SELECT COUNT(*) FROM appointment WHERE deleted_at IS NULL "
        "AND subject_id = ? AND scheduled_start >= ?",
        subject_id,
        now_ms,
    )
    add("next_up", rung_for_count(ahead))

    # The digest always has a sentence, quiet or otherwise.
    add("digest", FULL)

    # A card pointing at one thing depends on which thing the layout picked, so
    # these are read off the layout the fixture actually wrote rather than
    # assumed.
    for card_type, source_id in db.execute(
        "SELECT card_type, source_id FROM today_card WHERE deleted_at IS NULL "
        "AND subject_id = ? AND source_id IS NOT NULL ORDER BY sort_index",
        (subject_id,),
    ).fetchall():
        if card_type == "measure":
            readings = count(
                db,
                "SELECT COUNT(*) FROM measurement WHERE deleted_at IS NULL AND measure_id = ?",
                source_id,
            )
            add("measure", rung_for_count(readings))
            continue

        project = db.execute(
            "SELECT status, deleted_at, waiting_on FROM project WHERE id = ?",
            (source_id,),
        ).fetchone()
        if project is None:
            add(card_type, CLOSED)
            continue
        status, deleted_at, waiting_on = project
        if deleted_at is not None or status in ("done", "abandoned"):
            add(card_type, CLOSED)
        elif card_type == "project_date":
            soonest = db.execute(
                "SELECT due_start FROM project_date WHERE deleted_at IS NULL "
                "AND project_id = ? AND due_start IS NOT NULL ORDER BY due_start LIMIT 1",
                (source_id,),
            ).fetchone()
            if soonest is None:
                add(card_type, NONE_YET)
            elif soonest[0] < now_ms:
                add(card_type, PASSED)
            else:
                add(card_type, FULL)
        elif card_type == "project_standing":
            add(card_type, FULL if waiting_on else NONE_YET)
        else:
            steps = count(
                db,
                "SELECT COUNT(*) FROM project_step WHERE deleted_at IS NULL AND project_id = ?",
                source_id,
            )
            add(card_type, rung_for_count(steps))

    return out


def build(horizon, seed):
    path = Path(tempfile.gettempdir()) / f"health-trail-rungs-{horizon}-{seed}.db"
    if path.exists():
        path.unlink()
    subprocess.run(
        [sys.executable, str(GENERATE), "--at", horizon, "--seed", str(seed),
         "--out", str(path)],
        check=True,
        capture_output=True,
    )
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--at", action="append", help="a horizon, repeatable")
    parser.add_argument("--seed", type=int, default=6)
    args = parser.parse_args()

    horizons = args.at or HORIZONS
    table = {}
    for horizon in horizons:
        path = build(horizon, args.seed)
        db = sqlite3.connect(path)
        try:
            subject = db.execute(
                "SELECT id FROM subject WHERE deleted_at IS NULL ORDER BY created_at LIMIT 1"
            ).fetchone()
            if subject is None:
                print(f"{horizon}: no subject in the generated notebook", file=sys.stderr)
                continue
            now = db.execute("SELECT MAX(updated_at) FROM today_card").fetchone()[0] or 0
            table[horizon] = rungs(db, subject[0], now)
        finally:
            db.close()

    types = sorted({t for row in table.values() for t in row})
    width = max(len(t) for t in types) + 2

    def cell(horizon, card_type):
        found = table.get(horizon, {}).get(card_type)
        return " + ".join(sorted(found)) if found else "-"

    cell_width = max(
        [len(cell(h, t)) for h in horizons for t in types] + [len(h) for h in horizons]
    ) + 2

    print("Which rung each Today card lands on, per fixture. DESIGN.md 21.4.\n")
    print(" " * width + "".join(h.ljust(cell_width) for h in horizons))
    for card_type in types:
        print(
            card_type.ljust(width)
            + "".join(cell(h, card_type).ljust(cell_width) for h in horizons)
        )

    # **What no fixture produces is the useful half of this report.** A rung
    # nobody can reach is a rung nobody verified, and it will read as verified
    # once the checkbox is ticked.
    print()
    reachable = {}
    for card_type in types:
        seen = set()
        for horizon in horizons:
            seen |= table.get(horizon, {}).get(card_type) or set()
        reachable[card_type] = seen

    def applies(card_type, rung):
        if rung in (PASSED, CLOSED):
            return card_type in DATED_OR_SOURCED
        if rung in (FEW, FULL):
            return card_type not in YES_OR_NO
        return True

    print("Rungs that apply to a card and no fixture produces:\n")
    for rung in (FULL, FEW, NONE_YET, PASSED, CLOSED):
        without = [
            t for t in types if applies(t, rung) and rung not in reachable[t]
        ]
        if not without:
            print(f"  '{rung}': reachable for every card it applies to.")
        else:
            print(f"  '{rung}': missing for {', '.join(without)}")

    print(
        "\nRungs left out of that list on purpose. 'passed' and 'source closed' "
        "cannot happen\nto a card that answers with a number, and 'few' means "
        "nothing on a card whose count\nis a yes or a no."
    )

    print(
        "\nThis is guidance for picking a fixture, never a claim about what the "
        "app renders.\nThe queries here mirror Repository.todayAnswers and are a "
        "second copy of them.\nIf a rung promised here is not on the screen, "
        "believe the screen."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
