#!/usr/bin/env python3
"""Generate a realistic Health Trail notebook at any point in a five year history.

The app is used for years, and almost every defect that matters only appears
with time. A notebook with twelve entries and a notebook with two thousand
entries across eight chapters and five ended care threads are different pieces
of software. None of the thirteen personas in TESTING-PERSONAS.md are testable
without this, and waiting five years is not a plan.

**It writes a database file rather than driving a device.** The issue originally
said this runs against an emulator and never the owner's phone. The emulator
was dropped from this project in D21, D23, and B4, so that constraint no longer
describes anything. A file is better than the constraint it replaces: it is the
same artifact both platforms can load, it is diffable, it costs nothing to
produce, and it cannot touch anyone's records because it never opens theirs.

**Deterministic, and that is the whole point.** The same seed produces byte
identical data, so a failure is reproducible and a screenshot from one run is
comparable with a screenshot from another. Nothing here reads the clock or the
system random source: the seed drives everything, and every timestamp is
derived from the requested end date.

Usage:

    python3 tools/fixtures/generate.py --at year5 --seed 1 --out /tmp/five.sqlite
    python3 tools/fixtures/generate.py --at day1 --seed 1 --out /tmp/day1.sqlite

Kamsiob, AGPL-3.0.
"""

import argparse
import hashlib
import random
import sqlite3
import sys
from datetime import date, datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "contract" / "schema.sql"

# The six points TESTING-PERSONAS.md section 1 names, as days of history.
POINTS = {
    "day1": 1,
    "day30": 30,
    "month6": 183,
    "year1": 365,
    "year2": 730,
    "year5": 1826,
}

# The end of the history. Fixed rather than "today", because a fixture whose
# contents depend on the day it was generated is not deterministic, and the
# comparison this tool exists to enable would break every midnight.
HISTORY_ENDS = date(2026, 6, 30)

# Year 5 scale, from the issue. Everything smaller is scaled from these.
FULL = {
    "entries": 1600,
    "chapters": 8,
    "threads": 6,
    "threads_ended": 3,
    "projects": 4,
    "documents": 40,
    "measures": 3,
    "milestones": 15,
}


class Generator:
    """Everything that writes, in one place, so the seed reaches all of it."""

    def __init__(self, seed, days):
        self.rng = random.Random(seed)
        self.days = days
        self.start = HISTORY_ENDS - timedelta(days=days)
        self.device = "fixture-%016x" % seed
        # A counter rather than a clock, so ids are stable across runs and
        # still sort in creation order the way UUIDv7 does in the app.
        self.counter = 0

    # -- ids and time -----------------------------------------------------

    def new_id(self):
        self.counter += 1
        return hashlib.sha256(
            f"{self.device}:{self.counter}".encode()
        ).hexdigest()[:32]

    def ms(self, day_offset, hour=9, minute=0):
        moment = datetime.combine(
            self.start + timedelta(days=day_offset), datetime.min.time()
        ).replace(hour=hour, minute=minute)
        return int(moment.timestamp() * 1000)

    def scaled(self, full):
        """A count for this history length, never below one where the full is above zero."""
        share = self.days / POINTS["year5"]
        return max(1, round(full * share)) if full else 0

    # -- the shape of a real notebook --------------------------------------

    def day_of_activity(self):
        """An uneven day, with real gaps.

        Care is not evenly distributed. There are weeks with a call every day
        and months with nothing, and a fixture that spreads entries uniformly
        hides every defect that only appears around a gap.
        """
        # Three quarters of activity lands in a quarter of the history, which
        # is roughly how a hospital stay sits inside a year.
        if self.rng.random() < 0.75:
            burst = self.rng.choice(self.bursts)
            return min(self.days - 1, max(0, int(self.rng.gauss(burst, 12))))
        return self.rng.randrange(0, max(1, self.days))

    def build(self, db):
        self.bursts = sorted(
            self.rng.randrange(0, max(1, self.days))
            for _ in range(max(1, self.days // 180))
        )

        subject_id = self.subject(db)
        chapters = self.chapters(db, subject_id)
        threads = self.threads(db, subject_id)
        self.entries(db, subject_id, chapters, threads)
        self.measures(db, subject_id)
        self.milestones(db, subject_id)
        self.awkward(db, subject_id, chapters, threads)
        db.commit()

    def row(self, db, table, values, day=0):
        at = self.ms(day)
        full = {
            "id": self.new_id(),
            "created_at": at,
            "updated_at": at,
            "origin_device": self.device,
            "rev": 1,
        }
        full.update(values)
        columns = ", ".join(full)
        marks = ", ".join("?" for _ in full)
        db.execute(f"INSERT INTO {table} ({columns}) VALUES ({marks})", list(full.values()))
        return full["id"]

    def edtf_day(self, day_offset):
        on = self.start + timedelta(days=day_offset)
        return {
            "occurred_edtf": on.isoformat(),
            "occurred_zone": "America/New_York",
            "occurred_start": self.ms(day_offset, 0, 0),
            "occurred_end": self.ms(day_offset, 23, 59) + 59_999,
        }

    # -- the pieces --------------------------------------------------------

    def subject(self, db):
        return self.row(
            db,
            "subject",
            {
                "display_name": "Margaret Ellison",
                "relationship": "My mother",
                "situation_template_id": "nursing_home",
                "is_active": 1,
                "born_edtf": "1941-03",
            },
        )

    def chapters(self, db, subject_id):
        wanted = self.scaled(FULL["chapters"])
        span = max(1, self.days // wanted)
        made = []
        for index in range(wanted):
            began = index * span
            made.append(
                self.row(
                    db,
                    "chapter",
                    {
                        "subject_id": subject_id,
                        "name": PLACES[index % len(PLACES)],
                        **{
                            "started_edtf": (self.start + timedelta(days=began)).isoformat(),
                            "started_start": self.ms(began, 0, 0),
                            "started_end": self.ms(began, 23, 59),
                        },
                    },
                    day=began,
                )
            )
        return made

    def threads(self, db, subject_id):
        wanted = self.scaled(FULL["threads"])
        ended = min(self.scaled(FULL["threads_ended"]), max(0, wanted - 1))
        made = []
        for index in range(wanted):
            began = self.rng.randrange(0, max(1, self.days // 2))
            values = {
                "subject_id": subject_id,
                "label": THREADS[index % len(THREADS)],
                "color_index": index,
                "sort_index": index,
                "started_edtf": (self.start + timedelta(days=began)).isoformat(),
                "started_start": self.ms(began, 0, 0),
                "started_end": self.ms(began, 23, 59),
            }
            # An ended thread keeps its whole story rather than disappearing,
            # which is the case the trail's 35% opacity treatment exists for.
            if index < ended:
                over = min(self.days - 1, began + self.rng.randrange(30, 200))
                values["ended_edtf"] = (self.start + timedelta(days=over)).isoformat()
                values["ended_start"] = self.ms(over, 0, 0)
                values["ended_end"] = self.ms(over, 23, 59)
            made.append(self.row(db, "care_thread", values, day=began))
        return made

    def entries(self, db, subject_id, chapters, threads):
        wanted = self.scaled(FULL["entries"])
        kinds = ["call"] * 5 + ["visit"] * 3 + ["note"] * 2 + ["incident"]
        for index in range(wanted):
            day = self.day_of_activity()
            kind = self.rng.choice(kinds)
            # One entry in twelve has only a rough date, because a person
            # writing at 11pm about a call three days ago genuinely does not
            # know, and one in forty has none at all.
            roll = self.rng.random()
            if roll < 0.025:
                when = {"occurred_edtf": "XXXX-XX-XX"}
            elif roll < 0.11:
                on = self.start + timedelta(days=day)
                when = {
                    "occurred_edtf": f"{on.year:04d}-{on.month:02d}",
                    "occurred_start": self.ms(day, 0, 0),
                    "occurred_end": self.ms(day, 23, 59),
                }
            else:
                when = self.edtf_day(day)

            entry_id = self.row(
                db,
                "entry",
                {
                    "subject_id": subject_id,
                    "kind": kind,
                    "title": self.rng.choice(TITLES),
                    "body": self.rng.choice(BODIES),
                    "chapter_id": chapters[min(len(chapters) - 1, day // max(1, self.days // len(chapters)))],
                    # One in twenty reaches the Unfiled tray, which is roughly
                    # how often a real person cannot say where something goes.
                    "is_unfiled": 1 if self.rng.random() < 0.05 else 0,
                    **when,
                },
                day=day,
            )
            if threads and self.rng.random() < 0.6:
                self.row(
                    db,
                    "entry_thread",
                    {"entry_id": entry_id, "thread_id": self.rng.choice(threads)},
                    day=day,
                )

    def measures(self, db, subject_id):
        for index in range(self.scaled(FULL["measures"])):
            preset = MEASURES[index % len(MEASURES)]
            measure_id = self.row(
                db,
                "measure",
                {
                    "subject_id": subject_id,
                    "name": preset["name"],
                    "preset_id": preset["id"],
                    "unit": preset["unit"],
                    "style": "continuous",
                    "advice_risk": preset["risk"],
                    "sort_index": index,
                },
            )
            # Deliberately gappy. A gap renders as a gap and is never
            # interpolated, and a series with no gaps never exercises that.
            day = 0
            while day < self.days:
                self.row(
                    db,
                    "measurement",
                    {
                        "measure_id": measure_id,
                        "value_number": round(self.rng.uniform(*preset["range"]), 1),
                        "unit": preset["unit"],
                        "source": "family",
                        **self.edtf_day(day),
                    },
                    day=day,
                )
                step = self.rng.randrange(3, 14)
                if self.rng.random() < 0.1:
                    step += self.rng.randrange(30, 90)
                day += step

    def milestones(self, db, subject_id):
        for _ in range(self.scaled(FULL["milestones"])):
            day = self.rng.randrange(0, max(1, self.days))
            self.row(
                db,
                "milestone",
                {
                    "subject_id": subject_id,
                    "label": self.rng.choice(MILESTONES),
                    **self.edtf_day(day),
                },
                day=day,
            )

    def awkward(self, db, subject_id, chapters, threads):
        """The cases that break things, put in on purpose.

        Every one of these is something a real notebook eventually contains and
        a hand made fixture never does. They are here so a layout that only
        holds together with tidy sample data fails in a test rather than in
        somebody's hands.
        """
        # Unicode in every field, including a script the bundled fonts do not
        # cover, so a fallback failure is visible.
        self.row(
            db,
            "entry",
            {
                "subject_id": subject_id,
                "kind": "note",
                "title": "الممرضة قالت ذلك · 護士這樣說 · Señora Ruiz",
                "body": "Emoji, punctuation, and a right to left run: مرحبا ‏«اقتباس»‏ done.",
                **self.edtf_day(max(0, self.days - 2)),
            },
            day=max(0, self.days - 2),
        )
        # An 8,000 character note.
        self.row(
            db,
            "entry",
            {
                "subject_id": subject_id,
                "kind": "note",
                "title": "The long one",
                "body": ("She had a hard night and the aide wrote nothing down. " * 148)[:8000],
                **self.edtf_day(max(0, self.days - 3)),
            },
            day=max(0, self.days - 3),
        )
        # A person with one name.
        self.row(db, "person", {"subject_id": subject_id, "display_name": "Dee"})
        # An entry with only a rough date and nothing else at all, which is the
        # emptiest thing capture can produce and still a real record.
        self.row(
            db,
            "entry",
            {"subject_id": subject_id, "kind": "call", "occurred_edtf": "XXXX-XX-XX"},
        )
        # A measurement with no unit.
        unitless = self.row(
            db,
            "measure",
            {
                "subject_id": subject_id,
                "name": "How she seemed",
                "preset_id": "mood_behavior",
                "style": "observational",
                "advice_risk": "high",
            },
        )
        self.row(
            db,
            "measurement",
            {
                "measure_id": unitless,
                "value_text": "Brighter than yesterday. Ate most of her lunch.",
                "source": "family",
                **self.edtf_day(max(0, self.days - 1)),
            },
            day=max(0, self.days - 1),
        )
        # A chapter that lasted one day.
        brief = max(0, self.days - 5)
        self.row(
            db,
            "chapter",
            {
                "subject_id": subject_id,
                "name": "Emergency department, overnight",
                "started_edtf": (self.start + timedelta(days=brief)).isoformat(),
                "started_start": self.ms(brief, 0, 0),
                "started_end": self.ms(brief, 23, 59),
                "ended_edtf": (self.start + timedelta(days=brief)).isoformat(),
                "ended_start": self.ms(brief, 0, 0),
                "ended_end": self.ms(brief, 23, 59),
            },
            day=brief,
        )
        # A bill for zero dollars, which is a real thing a facility sends and
        # which a naive "amount is truthy" check drops.
        self.row(
            db,
            "bill",
            {
                "subject_id": subject_id,
                "description": "Statement, no balance due",
                "amount_minor": 0,
                "state": "paid",
            },
        )


PLACES = [
    "Maplewood Care Center",
    "St. Anne's Hospital",
    "Brookdale Assisted Living",
    "Home, with the family",
    "Riverside Rehabilitation",
    "St. Anne's Hospital, second admission",
    "Maplewood Care Center, memory unit",
    "Home, with an agency",
]

THREADS = [
    "Nursing",
    "Daily personal care",
    "Activities",
    "Meals and dietary",
    "Social services",
    "Physical therapy",
]

TITLES = [
    "Called the nursing station",
    "Spoke to the charge nurse",
    "Care plan meeting",
    "Visited in the afternoon",
    "Left a message for the social worker",
    "Called about the bill",
    "Asked about her medication change",
    "",
]

BODIES = [
    "She was sitting up and knew who I was.",
    "Nobody could tell me who had seen her today.",
    "They said someone would call back and nobody did.",
    "",
    "Asked again about the shower schedule. Third time.",
]

MEASURES = [
    {"id": "weight", "name": "Weight", "unit": "lb", "range": (128, 141), "risk": "low"},
    {"id": "sleep", "name": "Sleep", "unit": "hours", "range": (3, 9), "risk": "low"},
    {"id": "pain", "name": "Pain", "unit": "0 to 10", "range": (0, 8), "risk": "medium"},
]

MILESTONES = [
    "Walked the length of the hall",
    "Moved to the memory unit",
    "First day without oxygen",
    "Family meeting with the whole team",
    "Came home for an afternoon",
]


def generate(seed, point, out):
    if point not in POINTS:
        raise SystemExit(f"unknown point {point!r}. One of: {', '.join(POINTS)}")

    out = Path(out)
    if out.exists():
        out.unlink()
    out.parent.mkdir(parents=True, exist_ok=True)

    db = sqlite3.connect(out)
    db.executescript(SCHEMA.read_text(encoding="utf-8"))
    Generator(seed, POINTS[point]).build(db)
    counts = {
        table: db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        for table in ("entry", "chapter", "care_thread", "measurement", "milestone")
    }
    db.close()
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--at", default="year5", help=f"one of {', '.join(POINTS)}")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    counts = generate(args.seed, args.at, args.out)
    print(f"Wrote {args.out} at {args.at}, seed {args.seed}.")
    for table, count in counts.items():
        print(f"  {table}: {count}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
