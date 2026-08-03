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

# How far past the end of the history the one appointment that has not happened
# yet is scheduled. Far enough to still be ahead for a good while, and stated
# here rather than buried, because it is the one thing in this file that goes
# stale on its own.
UPCOMING_DAYS = 150

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
    "appointments": 22,
    "questions": 90,
}

# **Two of these are not counts of events, they are the size of a roster**, and
# scaling them by how long the history is was wrong in a way that only showed
# up as thin screens. Somebody who is on seven medications is on seven
# medications on her first day, and a family that has been at this a month
# already knows the charge nurse, the social worker and the aide who calls back.
# What grows with time is not the roster, it is how much of it has churned:
# the people who left and the medications that were stopped.
#
# Everything above accumulates and is scaled. These two plateau.
def roster(days, full, early):
    return full if days >= POINTS["day30"] else early


# What actually happens on an incident, in the order it happens. Administration
# rather than advice, per rule 2 and `templates/SCHEMA.md`: these are records of
# who was told and when, and none of them says whether anything was reasonable.
INCIDENT_STEPS = [
    "Reported it to the charge nurse",
    "Called the unit to ask what had been done",
    "Asked the director of nursing for it in writing",
    "Called back, was told it had gone to the care plan meeting",
    "Told what they decided",
]


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
        # The care team comes before the entries that name them and before the
        # appointments, medications and questions that point at them.
        people = self.care_team(db, subject_id, chapters)
        self.entries(db, subject_id, chapters, threads)
        self.involve(db, people)
        appointments = self.appointments(db, subject_id, chapters, people)
        self.questions(db, subject_id, people, appointments)
        self.medications(db, subject_id, chapters, people)
        self.emergency_card(db, subject_id, people)
        self.measures(db, subject_id)
        self.milestones(db, subject_id)
        self.incidents(db, subject_id, chapters)
        self.bills(db, subject_id, chapters)
        self.instructions(db, subject_id, chapters)
        self.projects(db, subject_id)
        self.documents(db, subject_id, chapters)
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

    def incidents(self, db, subject_id, chapters):
        """Incidents that resolve, and one that never does.

        The one that never resolves is the point. An app that only ever holds
        closed incidents never shows what an open one looks like sitting there
        for months, which is the state a family actually lives with.
        """
        wanted = max(2, self.scaled(FULL["chapters"]))
        for index in range(wanted):
            day = self.rng.randrange(0, max(1, self.days))
            values = {
                "subject_id": subject_id,
                "title": self.rng.choice(INCIDENTS),
                "description": "Reported to the charge nurse. Asked for it in writing.",
                "chapter_id": chapters[min(len(chapters) - 1, day // max(1, self.days // len(chapters)))],
                "reported_edtf": (self.start + timedelta(days=day)).isoformat(),
                "reported_start": self.ms(day, 0, 0),
                "reported_end": self.ms(day, 23, 59),
            }
            # The last one stays open, always, whatever the seed.
            closed = None
            if index < wanted - 1:
                closed = min(self.days - 1, day + self.rng.randrange(2, 60))
                values["resolved_at"] = self.ms(closed)
                values["resolution_note"] = "They changed the schedule. It held for a while."
            incident_id = self.row(db, "incident", values, day=day)

            # **An incident is a thread, so it carries the calls that chased
            # it.** `MASTER_SPEC.md` 4.7 makes it a sequence from first report
            # to resolution, and until 2026-08-02 the generator wrote the
            # incident and never linked a single entry to it. A month six
            # fixture therefore showed every incident reading "nothing written
            # down", which is not what a family living through one has, and it
            # made P4 untestable against generated data: P4's first requirement
            # is that the thread records every call with names and dates and
            # reads start to finish.
            self.incident_thread(db, subject_id, incident_id, day, closed)

    def incident_thread(self, db, subject_id, incident_id, reported_day, closed_day):
        """The calls and escalations that hang off one incident.

        Between two and five of them, the first on the day it was reported,
        the rest spread through to the answer if there was one. Every entry is
        an ordinary entry, so it appears on the trail in its own right as well
        as on the thread.
        """
        last = closed_day if closed_day is not None else min(self.days - 1, reported_day + 45)
        span = max(1, last - reported_day)
        howmany = self.rng.randrange(2, 6)
        for step in range(howmany):
            day = min(self.days - 1, reported_day + (span * step) // howmany)
            self.row(
                db,
                "entry",
                {
                    "subject_id": subject_id,
                    "kind": "call" if step else "incident",
                    "title": INCIDENT_STEPS[step % len(INCIDENT_STEPS)],
                    "body": None,
                    "incident_id": incident_id,
                    "occurred_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "occurred_start": self.ms(day, 0, 0),
                    "occurred_end": self.ms(day, 23, 59),
                    "is_unfiled": 0,
                },
                day=day,
            )

    def care_team(self, db, subject_id, chapters):
        """The people, which nothing generated until 2026-08-03.

        **Every screen that depends on a care team has only ever been seen with
        data typed in by hand.** The generator wrote exactly one person, "Dee",
        and only as the one-name edge case in `awkward`. A month six fixture
        opened on a care team of one, so the person screen built tonight, the
        chips on the capture form that link an entry to somebody, and the whole
        argument for a phone number one tap away were unreachable from a seed.

        One of them has left, because a list that only ever grows is not a list
        that has been used for five years, and an archived person is a state the
        screen has to hold.
        """
        wanted = roster(self.days, len(PEOPLE), 3)
        people = []
        for name, role, phone, note in PEOPLE[:wanted]:
            day = self.rng.randrange(0, max(1, self.days))
            people.append(
                self.row(
                    db,
                    "person",
                    {
                        "subject_id": subject_id,
                        "display_name": name,
                        "role_label": role,
                        "phone": phone,
                        "notes": note,
                    },
                    day=day,
                )
            )

        # Only once there is enough history for somebody to have left.
        if self.days >= POINTS["month6"]:
            for name, role, phone, note in ARCHIVED_PEOPLE:
                left = self.rng.randrange(self.days // 3, max(2, self.days - 1))
                self.row(
                    db,
                    "person",
                    {
                        "subject_id": subject_id,
                        "display_name": name,
                        "role_label": role,
                        "phone": phone,
                        "notes": note,
                        "archived_at": self.ms(left),
                    },
                    day=max(0, left - self.days // 3),
                )
        return people

    def involve(self, db, people):
        """Who was on the other end of a call or a visit.

        `MASTER_SPEC.md` section 3 promises a person knows every call and visit
        involving them, and `entry_person` has been in the schema since Phase 0
        with nothing writing to it, in the app until tonight and here until now.

        **Not every entry gets one, deliberately.** Plenty of what a family
        writes down is "called the nursing station" with no name attached,
        because nobody gave one, and a fixture where every entry names somebody
        would hide how the screen reads when most do not.
        """
        if not people:
            return
        rows = db.execute(
            "SELECT id, created_at FROM entry WHERE kind IN ('call', 'visit')"
        ).fetchall()
        for entry_id, at in rows:
            if self.rng.random() > 0.45:
                continue
            db.execute(
                "INSERT INTO entry_person (id, created_at, updated_at, origin_device, rev,"
                " entry_id, person_id) VALUES (?, ?, ?, ?, 1, ?, ?)",
                (self.new_id(), at, at, self.device, entry_id, self.rng.choice(people)),
            )

    def appointments(self, db, subject_id, chapters, people):
        """Meetings, past and coming, which the prep sheet is built on.

        **The prep sheet counts from the previous appointment**, so a fixture
        with none at all cannot show it at any window, and one with a single
        appointment can only ever show the "everything so far" case. These are
        spread through the history with at least one still ahead, because the
        sheet somebody actually opens is the one for a meeting that has not
        happened yet.
        """
        wanted = max(2, self.scaled(FULL["appointments"]))
        made = []
        for index in range(wanted + 1):
            title, where = APPOINTMENTS[index % len(APPOINTMENTS)]
            # Spread rather than clustered: meetings are the one thing in a
            # care record that happen on a schedule.
            #
            # **The last one has not happened yet**, which is the whole reason
            # the screen exists. Every entry in a fixture lands inside the
            # history, so the first version of this put every appointment in the
            # past: the "coming up" half of the screen was empty at every
            # horizon, and the prep sheet somebody actually opens, the one for a
            # meeting they are about to walk into, could not be reached at all.
            #
            # UPCOMING_DAYS past the end of the history rather than relative to
            # today, because a fixture that reads the clock is not deterministic
            # and this file says so at the top. The cost is that it stops being
            # upcoming once real time passes it, which is a known and stated
            # property rather than a surprise: move HISTORY_ENDS forward.
            day = (
                self.days + UPCOMING_DAYS
                if index == wanted
                else int(self.days * (index + 0.5) / max(1, wanted))
            )
            values = {
                "subject_id": subject_id,
                "title": title,
                "location_note": where,
                "chapter_id": chapters[min(len(chapters) - 1, day // max(1, self.days // len(chapters)))],
                "scheduled_edtf": (self.start + timedelta(days=day)).isoformat(),
                "scheduled_zone": "America/New_York",
                "scheduled_start": self.ms(day, 10, 0),
                "scheduled_end": self.ms(day, 11, 0),
            }
            if people:
                values["person_id"] = self.rng.choice(people)
            # Everything that has already happened was attended. The one that
            # has not is the sheet worth opening.
            if index < wanted:
                values["attended_edtf"] = values["scheduled_edtf"]
                values["attended_start"] = values["scheduled_start"]
                values["outcome_note"] = "Went through the care plan. Asked for it in writing."
            made.append((self.row(db, "appointment", values, day=day), day))
        return made

    def questions(self, db, subject_id, people, appointments):
        """Things to ask, some asked and some still waiting.

        **The open ones are what a prep sheet carries.** A fixture where every
        question has been asked produces an empty sheet that looks like a bug,
        and one where none has been asked never shows that an answered question
        stops coming back, which is the behavior somebody notices only when it
        fails.
        """
        wanted = max(3, self.scaled(FULL["questions"]))
        asked_at = [a for a in appointments if a[1] < self.days - 1]
        for index in range(wanted):
            day = self.day_of_activity()
            text, role = QUESTIONS[index % len(QUESTIONS)]
            values = {"subject_id": subject_id, "text": text}
            if role:
                values["role_label"] = role
                if people:
                    values["person_id"] = self.rng.choice(people)
            # Two in three were asked. The rest are still waiting, which is what
            # the next prep sheet picks up.
            if self.rng.random() < 0.66 and asked_at:
                appointment_id, on = self.rng.choice(asked_at)
                if on >= day:
                    values["asked_edtf"] = (self.start + timedelta(days=on)).isoformat()
                    values["asked_start"] = self.ms(on, 10, 30)
                    values["asked_at_appointment_id"] = appointment_id
                    values["answer_text"] = self.rng.choice(
                        [
                            "They said they would look into it.",
                            "She said it was a staffing decision.",
                            "Nobody could tell me.",
                            None,
                        ]
                    )
            self.row(db, "question", values, day=day)

    def medications(self, db, subject_id, chapters, people):
        """What she is taking, what she was taking, and every change in between.

        **The history is the point, not the list.** `MASTER_SPEC.md` treats a
        medication as a thing with a course: it starts, the dose changes, it is
        held for a week, it resumes, and sometimes it stops. A fixture holding
        only current medications with no events behind them cannot show the one
        screen that matters, which is the one somebody opens to answer "when did
        that change, and who told me".

        Nothing here says whether any of it was right. Rule 2.
        """
        wanted = roster(self.days, len(MEDICATIONS), 5)
        for index in range(wanted):
            name, dose, purpose = MEDICATIONS[index % len(MEDICATIONS)]
            started = self.rng.randrange(0, max(2, self.days // 2))
            values = {
                "subject_id": subject_id,
                "name": name,
                "dose_text": dose,
                "purpose_text": purpose,
                "started_edtf": (self.start + timedelta(days=started)).isoformat(),
                "started_zone": "America/New_York",
                "started_start": self.ms(started, 0, 0),
                # A few are on the emergency card, which is the state that
                # screen reads and nothing has ever written.
                "on_emergency_card": 1 if index < 3 else 0,
            }
            if people and self.rng.random() < 0.5:
                values["prescriber_person_id"] = self.rng.choice(people)

            # One in four was stopped, and the last one always is, so a fixture
            # of any size holds at least one stopped medication.
            stopped = None
            if index == wanted - 1 or self.rng.random() < 0.25:
                stopped = min(self.days - 1, started + self.rng.randrange(20, max(21, self.days // 2)))
                values["stopped_edtf"] = (self.start + timedelta(days=stopped)).isoformat()
                values["stopped_start"] = self.ms(stopped, 0, 0)
                values["stop_reason"] = self.rng.choice(MED_STOP_REASONS)

            medication_id = self.row(db, "medication", values, day=started)
            self.medication_history(db, medication_id, chapters, started, stopped, dose)

    def medication_history(self, db, medication_id, chapters, started, stopped, dose):
        """The course of one medication, in the order it happened."""
        last = stopped if stopped is not None else self.days - 1

        def event(kind, day, note=None, dose_text=None):
            day = max(0, min(self.days - 1, day))
            self.row(
                db,
                "medication_event",
                {
                    "medication_id": medication_id,
                    "kind": kind,
                    "chapter_id": chapters[
                        min(len(chapters) - 1, day // max(1, self.days // len(chapters)))
                    ],
                    "occurred_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "occurred_zone": "America/New_York",
                    "occurred_start": self.ms(day, 0, 0),
                    "dose_text": dose_text,
                    "note": note,
                },
                day=day,
            )

        event("started", started, dose_text=dose)

        # **How many changes depends on how long she has been on it**, which
        # the first version of this missed: a fixed nought to three gave a
        # medication running five years the same history as one running three
        # weeks, so the year five screen was no fuller than the day thirty one.
        # Roughly one change every four months, which is what a dose adjustment
        # and a hold for a stomach bug actually come to.
        span = max(1, last - started)
        for _ in range(self.rng.randrange(0, 2 + span // 120)):
            when = started + self.rng.randrange(1, span + 1)
            kind = self.rng.choice(["dose_changed", "held", "resumed", "noted"])
            if kind == "dose_changed":
                event(kind, when, note="Told at the care plan meeting.", dose_text="Doubled")
            elif kind == "held":
                event(kind, when, note="Held while she had the stomach thing.")
            elif kind == "resumed":
                event(kind, when, note="Back on it.")
            else:
                event(kind, when, note="Pharmacy switched the manufacturer.")

        if stopped is not None:
            event("stopped", stopped, note="Nobody told me until I asked.")

    def emergency_card(self, db, subject_id, people):
        """The card somebody would be handed, and who to call first.

        **Written in the words a family would use, not a clinician's.** The
        schema calls one column `resuscitation_status` and the screen asks for
        "what the signed paperwork says", which is the honest question: the app
        records what the document says and never what it means. Rule 2.

        **Where the original is kept is half of every answer here.** A card
        saying there is a signed directive is no use to anybody who cannot
        produce the paper, which is why the schema carries a location beside
        each one and why the fixture fills them in.
        """
        card_id = self.row(
            db,
            "emergency_card",
            {
                "subject_id": subject_id,
                "allergies": "Penicillin, comes up in a rash. Latex.",
                "blood_type": "O positive",
                "conditions": "Dementia. She will say she is fine. Deaf in the left ear.",
                "resuscitation_status": "Do not resuscitate, signed 2024",
                "resuscitation_document_location": "In the blue folder at home, and the facility has a copy in her chart",
                "decision_maker_person_id": None,
                "decision_maker_document_location": "Power of attorney, filed with the county, copy in the blue folder",
                "insurance_note": "Medicare plus the supplement. Cards are in her purse and photographed in Documents.",
                "other_notes": (
                    "She gets frightened in the ambulance if nobody is holding her hand. "
                    "Her glasses and hearing aid are in the drawer by the bed."
                ),
            },
            day=max(0, self.days // 4),
        )

        # **Family first, then the facility.** The order is the order somebody
        # would work down it, and `sort_index` is what holds it.
        contacts = [
            ("Me", "555 0121", "Daughter, has the power of attorney"),
            ("Danny", "555 0134", "Son, lives out of state"),
        ]
        for index, (name, phone, relationship) in enumerate(contacts):
            self.row(
                db,
                "emergency_contact",
                {
                    "emergency_card_id": card_id,
                    "display_name": name,
                    "phone": phone,
                    "relationship": relationship,
                    "sort_index": index,
                },
                day=max(0, self.days // 4),
            )

        # Somebody from the care team, carried by their person row rather than
        # retyped, which is the link `person_id` exists for and nothing wrote.
        if people:
            self.row(
                db,
                "emergency_contact",
                {
                    "emergency_card_id": card_id,
                    "person_id": people[0],
                    "display_name": PEOPLE[0][0],
                    "phone": PEOPLE[0][2],
                    "relationship": PEOPLE[0][1],
                    "sort_index": len(contacts),
                },
                day=max(0, self.days // 4),
            )

    def bills(self, db, subject_id, chapters):
        """One bill in every state the schema allows.

        Written as a loop over the states rather than random ones, so a state
        can never be missing because a seed happened not to pick it. A screen
        that has never rendered a disputed bill is a screen nobody has tested.
        """
        for index, state in enumerate(BILL_STATES):
            day = self.rng.randrange(0, max(1, self.days))
            self.row(
                db,
                "bill",
                {
                    "subject_id": subject_id,
                    "description": BILL_TEXT[index % len(BILL_TEXT)],
                    "amount_minor": self.rng.randrange(1200, 940_000),
                    "currency": "USD",
                    "state": state,
                    "chapter_id": chapters[min(len(chapters) - 1, index)],
                    "received_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "received_start": self.ms(day, 0, 0),
                    "received_end": self.ms(day, 23, 59),
                },
                day=day,
            )

    def instructions(self, db, subject_id, chapters):
        """Standing instructions with both tags, and violations recorded against them.

        Both tags on purpose. DESIGN.md section 5.7 says the federal tag must
        never appear on a notebook whose chapter is not a nursing home without
        the explanation visible, and that rule cannot be exercised by a fixture
        that only ever carries requests.
        """
        for index, (name, wording, tag) in enumerate(INSTRUCTIONS):
            day = self.rng.randrange(0, max(1, self.days // 2))
            instruction_id = self.row(
                db,
                "standing_instruction",
                {
                    "subject_id": subject_id,
                    "name": name,
                    "wording": wording,
                    "tag": tag,
                    "chapter_id": chapters[min(len(chapters) - 1, index)],
                    "given_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "given_start": self.ms(day, 0, 0),
                    "given_end": self.ms(day, 23, 59),
                },
                day=day,
            )
            for _ in range(self.rng.randrange(0, 4)):
                broke = min(self.days - 1, day + self.rng.randrange(1, 200))
                self.row(
                    db,
                    "instruction_violation",
                    {
                        "instruction_id": instruction_id,
                        "note": "Happened again. Nobody had been told.",
                        **self.edtf_day(broke),
                    },
                    day=broke,
                )

    def projects(self, db, subject_id):
        """Projects at various stages, including the ones nobody finished."""
        for index in range(max(len(PROJECT_STATES), self.scaled(FULL["projects"]))):
            state = PROJECT_STATES[index % len(PROJECT_STATES)]
            day = self.rng.randrange(0, max(1, self.days))
            project_id = self.row(
                db,
                "project",
                {
                    "subject_id": subject_id,
                    "name": PROJECTS[index % len(PROJECTS)],
                    "status": state,
                    "started_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "started_start": self.ms(day, 0, 0),
                    "started_end": self.ms(day, 23, 59),
                },
                day=day,
            )
            # **How far through it actually is, which the state implies.**
            #
            # Every project came out reading "0 of N steps done", including the
            # ones marked done, because no step was ever completed. A finished
            # project with nothing ticked is a contradiction on screen, and a
            # column of zeroes reads as a scorecard rather than as the state of
            # four separate processes. Seen with a month six fixture on the
            # projects screen.
            #
            # A stalled project is deliberately part way: that is what stalled
            # means, and it is the state a family actually sits in.
            total = self.rng.randrange(2, 7)
            done_through = {
                "done": total,
                "active": self.rng.randrange(1, max(2, total)),
                "waiting": self.rng.randrange(1, max(2, total)),
                "stalled": self.rng.randrange(1, max(2, total)),
                "abandoned": self.rng.randrange(0, max(1, total)),
            }.get(state, 0)

            for step in range(total):
                values = {
                    "project_id": project_id,
                    "text": PROJECT_STEPS[step % len(PROJECT_STEPS)],
                    "sort_index": step,
                }
                if step < done_through:
                    # Steps are completed in order and spread through the days
                    # since the project began, which is how one actually moves.
                    at = min(self.days - 1, day + (step + 1) * 3)
                    values["completed_edtf"] = (self.start + timedelta(days=at)).isoformat()
                    values["completed_start"] = self.ms(at, 0, 0)
                    values["completed_end"] = self.ms(at, 23, 59)
                self.row(db, "project_step", values, day=day)

    def documents(self, db, subject_id, chapters):
        """Documents, each with an attachment and a note on where the paper is.

        The attachment rows carry real sha256 values over generated bytes, so a
        round trip that verifies hashes has something true to verify. One is
        deliberately at the 25 MB size limit from D13, recorded by size rather
        than by generating 25 MB into a fixture nobody wants to move around.
        """
        wanted = self.scaled(FULL["documents"])
        for index in range(wanted):
            day = self.rng.randrange(0, max(1, self.days))
            document_id = self.row(
                db,
                "document",
                {
                    "subject_id": subject_id,
                    "title": DOCUMENTS[index % len(DOCUMENTS)],
                    "category": self.rng.choice(["medical", "legal", "financial", "facility"]),
                    "chapter_id": chapters[min(len(chapters) - 1, index % len(chapters))],
                    "original_location": self.rng.choice(ORIGINALS),
                    "received_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "received_start": self.ms(day, 0, 0),
                    "received_end": self.ms(day, 23, 59),
                },
                day=day,
            )
            body = f"page bytes for document {index}".encode()
            at_limit = index == 0
            self.row(
                db,
                "attachment",
                {
                    "sha256": hashlib.sha256(body).hexdigest(),
                    "original_filename": f"scan-{index:03d}.jpg",
                    "mime_type": "image/jpeg",
                    # Exactly the ceiling D13 set, so a screen that formats or
                    # warns on size has the boundary case to render.
                    "byte_size": 25 * 1024 * 1024 if at_limit else len(body),
                    "document_id": document_id,
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
        # A care thread with a single session and nothing else, which is what
        # a discipline that assessed once and discharged looks like.
        single = max(0, self.days - 4)
        lonely = self.row(
            db,
            "care_thread",
            {
                "subject_id": subject_id,
                "label": "Speech therapy",
                "color_index": 7,
                "sort_index": 90,
                "started_edtf": (self.start + timedelta(days=single)).isoformat(),
                "started_start": self.ms(single, 0, 0),
                "started_end": self.ms(single, 23, 59),
                "ended_edtf": (self.start + timedelta(days=single)).isoformat(),
                "ended_start": self.ms(single, 0, 0),
                "ended_end": self.ms(single, 23, 59),
            },
            day=single,
        )
        only = self.row(
            db,
            "entry",
            {
                "subject_id": subject_id,
                "kind": "visit",
                "title": "Speech assessed her once and discharged her",
                **self.edtf_day(single),
            },
            day=single,
        )
        self.row(db, "entry_thread", {"entry_id": only, "thread_id": lonely}, day=single)

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

INCIDENTS = [
    "Found on the floor beside the bed",
    "Call light not answered for forty minutes",
    "Wrong medication brought to the room",
    "Nobody told us she had been moved",
    "Bruise on her arm nobody could explain",
]

BILL_STATES = ["needs_attention", "disputed", "waiting_on_insurance", "paid", "closed"]

BILL_TEXT = [
    "Monthly room and board",
    "Level of care reassessment",
    "Ambulance transfer",
    "Physical therapy, out of network",
    "Pharmacy charges",
]

INSTRUCTIONS = [
    (
        "Call me before any medication change",
        "Please call me before any change to her medications, including a dose change.",
        "request",
    ),
    (
        "Notify me of any fall",
        "Federal rules for nursing homes require the facility to notify the "
        "representative of an accident or injury.",
        "federal",
    ),
    (
        "She showers in the morning",
        "She has always showered in the morning and gets upset in the evening.",
        "request",
    ),
]

PROJECT_STATES = ["active", "waiting", "stalled", "done", "abandoned"]

PROJECTS = [
    "Medicaid application",
    "Get the power of attorney recognized",
    "Appeal the level of care assessment",
    "Move her belongings out of the old room",
    "Find a dentist who will come to the facility",
]

PROJECT_STEPS = [
    "Call and ask what form it is",
    "Get the form",
    "Find the last three bank statements",
    "Send it certified",
    "Follow up after two weeks",
    "Ask for it in writing",
]

DOCUMENTS = [
    "Discharge summary",
    "Care plan, signed",
    "Power of attorney",
    "Insurance card, both sides",
    "Level of care assessment",
    "Grievance, filed",
]

ORIGINALS = [
    "In the blue folder at home",
    "The facility has the original",
    "Filed with the county",
    "In the glove box",
    "I do not know where the original is",
]

PEOPLE = [
    ("Angela Reyes", "Charge nurse, day shift", "555 0142", "Days, 7 to 3. Ask for her by name."),
    ("Marcus Bell", "Social worker", "555 0187", None),
    ("Dr. Priya Raman", "Attending physician", "555 0110", "Rounds Tuesdays."),
    ("Tonya K.", "Aide, evenings", None, "The one who actually calls back."),
    ("Wesley Obi", "Director of nursing", "555 0100", None),
    ("Sharon Delacroix", "Billing office", "555 0166", None),
    ("Ruth Ann Pierce", "Physical therapy", "555 0173", None),
    ("Jerome Whitfield", "Ombudsman", "555 0199", "County office. Not facility staff."),
]

# Somebody who left. A care team that only ever grows is not a care team that
# has been used for five years.
ARCHIVED_PEOPLE = [
    ("Nadine Cross", "Charge nurse, day shift", "555 0142", "Left in the spring."),
]

APPOINTMENTS = [
    ("Care plan meeting", "Conference room, second floor"),
    ("Quarterly review", "Conference room, second floor"),
    ("Doctor, follow up", "Suite 210, the medical building"),
    ("Dentist", "They come to the facility"),
    ("Podiatry", "In her room"),
    ("Annual assessment", "Conference room, second floor"),
    ("Meeting about the level of care", None),
]

# **Administration, not clinical curiosity.** Rule 2. Every one of these is a
# question about who did what and when, or about a decision somebody made. None
# of them asks whether anything was medically right.
# **Each question carries who it is for.** The first version chose a role at
# random alongside the text, which put the billing office in charge of the
# window bed and asked a clerk why nobody called about a fall. Nothing was
# technically wrong and the screen looked broken, because a person reads the
# pair and not the columns. A fixture that is realistic everywhere except in how
# its pieces fit together teaches you to distrust the screen.
QUESTIONS = [
    ("Why was the shower schedule changed?", "Charge nurse"),
    ("Who authorized the room move?", "Social worker"),
    ("Can I have the care plan in writing?", "Social worker"),
    ("What is the aide to resident ratio on evenings?", "Director of nursing"),
    ("Who do I call at night when the office is closed?", "Charge nurse"),
    ("Why was I not told about the fall until the next day?", "Director of nursing"),
    ("Can she have the window bed?", "Social worker"),
    ("What is this line on the bill for?", "Billing"),
    ("When was the last time she was weighed?", "Charge nurse"),
    ("Who is covering when Angela is off?", "Charge nurse"),
    ("Has the dentist been asked to come?", None),
    ("Is she still going to physical therapy?", "Physical therapy"),
]

# **Names and doses only, and never a purpose that reads as a judgment.** The
# app records what somebody was told they are taking. Rule 2 forbids the rest.
MEDICATIONS = [
    ("Lisinopril", "10 mg, mornings", "Blood pressure"),
    ("Metformin", "500 mg, twice a day", "Diabetes"),
    ("Atorvastatin", "20 mg, evenings", "Cholesterol"),
    ("Donepezil", "5 mg, evenings", "Memory"),
    ("Levothyroxine", "50 mcg, mornings, empty stomach", "Thyroid"),
    ("Vitamin D", "1000 units", None),
    ("Trazodone", "25 mg, at night", "Sleep"),
    ("Tylenol", "As needed", "Pain"),
]

MED_STOP_REASONS = [
    "Stopped at the care plan meeting.",
    "The doctor took her off it.",
    "Pharmacy said it was a duplicate.",
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
        for table in (
            "entry",
            "chapter",
            "care_thread",
            "measurement",
            "milestone",
            "person",
            "entry_person",
            "appointment",
            "question",
            "medication",
            "medication_event",
            "emergency_card",
            "emergency_contact",
        )
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
