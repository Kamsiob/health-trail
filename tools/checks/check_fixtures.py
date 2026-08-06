#!/usr/bin/env python3
"""Assert the fixture generator is deterministic and hits its stated scale.

Determinism is the entire value of the generator. If the same seed stops
producing the same bytes, a failing persona run stops being reproducible and a
screenshot from one run stops being comparable with a screenshot from another,
which is the reason the tool exists rather than a property of it.

**A generator that drifts is worse than no generator**, because the runs still
happen and their results quietly stop meaning anything.

Exit 0 when clean, 1 with a list of failures otherwise.

Kamsiob, AGPL-3.0.
"""

import hashlib
import json
import sqlite3
from datetime import datetime
from zoneinfo import ZoneInfo
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "fixtures"))

import generate as fixtures  # noqa: E402

# What year five has to look like, from issue #17. Checked as a floor and a
# ceiling, because a generator that quietly produces a tenth of the data still
# passes every test that only asks whether it ran.
BILL_STATES = {"needs_attention", "disputed", "waiting_on_insurance", "paid", "closed"}
PROJECT_STATES = {"active", "waiting", "stalled", "done", "abandoned"}

YEAR_FIVE = {
    "entry": (1200, 2000),
    "chapter": (8, 10),
    "care_thread": (5, 7),
    "milestone": (12, 18),
    "document": (35, 45),
    "attachment": (35, 45),
}


def counts(path):
    db = sqlite3.connect(path)
    try:
        return {
            table: db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            for table in (
                "entry", "chapter", "care_thread", "milestone", "measurement",
                "incident", "bill", "standing_instruction", "project", "document",
                "attachment",
            )
        }
    finally:
        db.close()


# Which tables carry an EDTF beside the columns derived from it, and what the
# three columns are called there.
#
# **A fixture that writes these by hand can produce a row the app could never
# write**, which is #233: an appointment carried a day precision EDTF beside a
# 10am instant, so anything reading the instant got a time nobody typed and the
# appointments screen flipped one from "coming up" to "already happened" at 10am
# instead of at midnight. A fixture whose rows the app cannot produce cannot
# exercise the path the app actually takes, which is the whole point of one.
EDTF_COLUMNS = [
    ("appointment", "scheduled_edtf", "scheduled_start", "scheduled_end"),
    ("appointment", "attended_edtf", "attended_start", None),
    ("entry", "occurred_edtf", "occurred_start", "occurred_end"),
]


def edtf_problems(path):
    """Every row whose derived columns disagree with the precision it declares.

    Only the two shapes this fixture writes are checked, day and moment, since
    a rule about a precision nothing generates is a rule nobody can break.
    """
    found = []
    db = sqlite3.connect(path)
    try:
        for table, edtf_col, start_col, end_col in EDTF_COLUMNS:
            cols = f"{edtf_col}, {start_col}" + (f", {end_col}" if end_col else "")
            rows = db.execute(
                f"SELECT id, {cols} FROM {table} WHERE {edtf_col} IS NOT NULL "
                f"AND {start_col} IS NOT NULL"
            ).fetchall()
            for row in rows:
                rid, edtf, start = row[0], row[1], row[2]
                end = row[3] if end_col else None
                at = datetime.fromtimestamp(start / 1000, ZoneInfo("America/New_York"))
                if "T" in edtf:
                    # A moment resolves to start == end, and the instant has to
                    # be the minute the text names.
                    want = edtf.split("T", 1)[1][:5]
                    got = f"{at.hour:02d}:{at.minute:02d}"
                    if want != got:
                        found.append(
                            f"{table} {rid} says {edtf!r} and its {start_col} is {got}"
                        )
                    if end is not None and end != start:
                        found.append(
                            f"{table} {rid} is a moment and its {end_col} is not its start"
                        )
                else:
                    # A day runs midnight to one millisecond before the next.
                    if (at.hour, at.minute, at.second) != (0, 0, 0):
                        found.append(
                            f"{table} {rid} says {edtf!r}, which is day precision, and "
                            f"its {start_col} is {at.hour:02d}:{at.minute:02d}, a time "
                            f"nobody typed"
                        )
    finally:
        db.close()
    return found


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    failures = []

    with tempfile.TemporaryDirectory() as work:
        work = Path(work)

        # The same seed, twice, byte for byte.
        first = work / "first.sqlite"
        second = work / "second.sqlite"
        fixtures.generate(1, "year5", first)
        fixtures.generate(1, "year5", second)
        if digest(first) != digest(second):
            failures.append(
                "the same seed produced different bytes. A failing persona run is no "
                "longer reproducible and screenshots are no longer comparable."
            )

        # A different seed has to produce something different, or the seed is
        # not reaching the generator and every run is the same notebook.
        other = work / "other.sqlite"
        fixtures.generate(2, "year5", other)
        if digest(first) == digest(other):
            failures.append(
                "two different seeds produced identical bytes, so the seed is not "
                "reaching the generator."
            )

        # Every point produces something, and they grow.
        sizes = {}
        for point in fixtures.POINTS:
            path = work / f"{point}.sqlite"
            fixtures.generate(1, point, path)
            sizes[point] = counts(path)["entry"]
            if sizes[point] < 1:
                failures.append(f"{point} produced no entries at all.")

        ordered = list(fixtures.POINTS)
        for earlier, later in zip(ordered, ordered[1:]):
            if sizes[later] < sizes[earlier]:
                failures.append(
                    f"{later} has fewer entries than {earlier}, so history is not growing."
                )

        # The shapes the personas need, each of which a random generator can
        # miss by chance and which the issue names explicitly.
        db = sqlite3.connect(first)
        try:
            def distinct(table, column):
                return {row[0] for row in db.execute(f"SELECT DISTINCT {column} FROM {table}")}

            missing_bills = BILL_STATES - distinct("bill", "state")
            if missing_bills:
                failures.append(
                    f"no bill in state {sorted(missing_bills)}. A screen that has never "
                    f"rendered a disputed bill is a screen nobody has tested."
                )
            missing_projects = PROJECT_STATES - distinct("project", "status")
            if missing_projects:
                failures.append(f"no project in state {sorted(missing_projects)}.")

            # **A project has to have come from a template, and the template has
            # to be real.** The template library's whole job is saying what each
            # template produced, and every generated project carried no
            # template at all, so that half of the screen had never rendered.
            # The same shape as the milestones that carried no chapter, #237.
            #
            # The ids are checked against the catalog rather than a list here,
            # so renaming a template in templates/data/projects.json fails this
            # instead of silently producing a project pointing at nothing.
            catalog = {
                item["id"]
                for item in json.loads(
                    (ROOT / "templates/data/projects.json").read_text(encoding="utf-8")
                )["templates"]
            }
            used = {t for t in distinct("project", "template_id") if t is not None}
            if not used:
                failures.append(
                    "no project was started from a template, so the template library "
                    "can never show what any template produced, which is the whole "
                    "reason it is a library rather than a catalog."
                )
            unknown = used - catalog
            if unknown:
                failures.append(
                    f"projects point at templates that are not in the catalog: "
                    f"{sorted(unknown)}."
                )
            # **The shape a person gave a project, and the Today they arranged.**
            # contract/DATA-CONTRACT.md 8.7. This block exists because the same
            # defect has now landed four times in this repository: a feature is
            # built, the fixture never writes the rows it reads, and the feature
            # is never once seen working. #237, #229, #233, and the projects
            # that carried no template above are all this shape.
            #
            # Every project screen leads with one of the three answers in
            # DESIGN.md 20.1, so all three leads have to exist or two of the
            # three home screens can never be looked at on the phone.
            leads = {t for t in distinct("project", "lead") if t}
            missing_leads = {"standing", "date", "steps"} - leads
            if missing_leads:
                failures.append(
                    f"no project leads with {sorted(missing_leads)}, so those project "
                    f"shapes can never be seen on the device. DESIGN.md 20.3."
                )

            for table, why in (
                ("project_stage", "the road strip has no stages to draw"),
                ("project_standing", "where it stands is the answer two of the three "
                                     "shapes lead with, and it would always be blank"),
                ("project_date", "the next date is the answer the closing window "
                                 "leads with"),
                ("project_date_kind", "recording a date offers no chips"),
                ("project_paper", "the papers screen has no placeholders"),
                ("today_card", "nobody ever sees a blank Today, DESIGN.md 21.5, and "
                               "an empty layout is exactly that"),
            ):
                if db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] < 1:
                    failures.append(f"{table} is empty, so {why}.")

            # **Both sides of today.** D113: a screen leads with the soonest date
            # that has not passed, and the most recent when they all have. A
            # fixture with dates on one side of today can only ever show one of
            # those, and the passed rung of the states ladder is the one that
            # would go unseen. This is a real date comparison against the clock
            # on purpose: the point is whether it is still true today.
            now = int(time.time() * 1000)
            passed = db.execute(
                "SELECT COUNT(*) FROM project_date WHERE due_start < ?", (now,)
            ).fetchone()[0]
            upcoming = db.execute(
                "SELECT COUNT(*) FROM project_date WHERE due_start >= ?", (now,)
            ).fetchone()[0]
            if passed < 1 or upcoming < 1:
                failures.append(
                    f"project dates do not fall on both sides of today: {passed} have "
                    f"passed and {upcoming} have not. The fixture's history ends on a "
                    f"fixed date, so a future date written as a small offset stops "
                    f"being in the future once real time passes it. Move HISTORY_ENDS "
                    f"forward in tools/fixtures/generate.py."
                )

            # A date without its source is half a date, 20.1: "Apr 12, from the
            # letter of Mar 5" is usable a year later and a bare Apr 12 is not.
            if db.execute(
                "SELECT COUNT(*) FROM project_date WHERE source_note IS NOT NULL"
            ).fetchone()[0] < 1:
                failures.append(
                    "no project date carries where it was taken from, so the half of "
                    "the date that makes it usable later is never rendered."
                )

            # The lead slot is singular by construction, DESIGN.md 21.1. The
            # database refuses two; this catches zero, which it cannot.
            for subject_id, leads_here in db.execute(
                "SELECT subject_id, SUM(is_lead) FROM today_card "
                "WHERE deleted_at IS NULL GROUP BY subject_id"
            ):
                if leads_here != 1:
                    failures.append(
                        f"subject {subject_id} has {leads_here} lead cards on Today. "
                        f"There is never zero and never two. DESIGN.md 21.1."
                    )

            # A card pointing at a project that is finished is the source-closed
            # rung of the states ladder, 21.4, and it cannot be seen without one.
            if db.execute(
                "SELECT COUNT(*) FROM today_card c JOIN project p "
                "ON p.id = c.source_id WHERE c.source_table = 'project' "
                "AND p.status IN ('done', 'abandoned')"
            ).fetchone()[0] < 1:
                failures.append(
                    "no Today card points at a finished project, so the source-closed "
                    "rung of the states ladder in DESIGN.md 21.4 can never be seen."
                )

            if distinct("standing_instruction", "tag") != {"federal", "request"}:
                failures.append(
                    "the fixture does not carry both standing instruction tags, so the "
                    "rule in DESIGN.md 5.7 about the federal tag cannot be exercised."
                )

            open_incidents = db.execute(
                "SELECT COUNT(*) FROM incident WHERE resolved_at IS NULL"
            ).fetchone()[0]
            if open_incidents < 1:
                failures.append(
                    "every incident resolves. An app that only ever holds closed "
                    "incidents never shows what an open one looks like sitting there "
                    "for months, which is the state a family actually lives with."
                )

            at_limit = db.execute(
                "SELECT COUNT(*) FROM attachment WHERE byte_size = ?",
                (25 * 1024 * 1024,),
            ).fetchone()[0]
            if at_limit < 1:
                failures.append(
                    "no attachment sits exactly at the 25 MB limit from D13, so no "
                    "screen renders the boundary case."
                )

            violations = db.execute("SELECT COUNT(*) FROM instruction_violation").fetchone()[0]
            if violations < 1:
                failures.append("no standing instruction has a recorded violation.")
        finally:
            db.close()

        # Every EDTF must agree with the columns derived from it. #233.
        failures.extend(edtf_problems(first))

        # Year five hits the scale the issue states.
        actual = counts(first)
        for table, (low, high) in YEAR_FIVE.items():
            found = actual[table]
            if not low <= found <= high:
                failures.append(
                    f"year five has {found} rows in {table}, and the stated scale is "
                    f"{low} to {high}. A notebook a tenth of this size is a different "
                    f"piece of software and would not exercise what the personas test."
                )

    if failures:
        print(f"Fixture check failed. {len(failures)} problems.\n")
        for failure in failures:
            print(f"  {failure}")
        print("\nSee TESTING-PERSONAS.md section 1 and issue #17.")
        return 1

    print(
        "Fixture check passed. The same seed is byte identical across runs, a "
        "different seed is not, all six points generate and grow, year five "
        "hits its stated scale, and every EDTF agrees with the columns derived "
        "from it."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
