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
from zoneinfo import ZoneInfo
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
    ("Reported it to the charge nurse", "Charge nurse, day shift"),
    ("Called the unit to ask what had been done", "Charge nurse, day shift"),
    ("Asked the director of nursing for it in writing", "Director of nursing"),
    ("Called back, was told it had gone to the care plan meeting", "Director of nursing"),
    ("Told what they decided", "Director of nursing"),
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

    # Every row this file writes says it happened in New York, so every instant
    # it computes has to be in New York too.
    ZONE = ZoneInfo("America/New_York")

    def ms(self, day_offset, hour=9, minute=0):
        """The instant, in the zone the rows claim rather than the machine's.

        **This used the machine's local time.** `datetime.timestamp()` on a
        naive datetime resolves in whatever zone the generator happens to be
        running in, while every row it writes carries
        `"America/New_York"`. On a laptop in New York the two agreed and
        nothing showed; in continuous integration, which runs in UTC, a
        `2021-08-10T10:00` appointment got an instant that reads 06:00 in the
        zone it claims.

        **Found by the check added for #233**, on the first push after it, which
        is the check working: the fixture must produce rows the app could have
        written, and an EDTF that disagrees with its own instant is not one.
        """
        moment = datetime.combine(
            self.start + timedelta(days=day_offset), datetime.min.time()
        ).replace(hour=hour, minute=minute, tzinfo=self.ZONE)
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
        appointments = self.appointments(db, subject_id, chapters, people)
        self.questions(db, subject_id, people, appointments)
        self.medications(db, subject_id, chapters, people)
        self.emergency_card(db, subject_id, people)
        self.measures(db, subject_id)
        self.milestones(db, subject_id)
        self.incidents(db, subject_id, chapters, people)
        self.bills(db, subject_id, chapters)
        self.instructions(db, subject_id, chapters)
        projects = self.projects(db, subject_id)
        self.documents(db, subject_id, chapters)
        self.awkward(db, subject_id, chapters, threads)
        # **Last, because it works over every entry that exists.** It ran
        # immediately after `entries` at first, which meant the calls hanging
        # off an incident had not been written yet and none of them named
        # anybody. An incident could then never show who was involved, and the
        # screen that reads it looked broken when it was the fixture that was
        # ordered wrong.
        self.involve(db, people)
        # After documents, because a paper placeholder points at one, and after
        # projects, because it points at those. Same reason `involve` is last.
        self.fill_project_papers(db)
        self.connect_entries_to_projects(db, subject_id, projects)
        # Last of all, because a card points at a measure, a project or a
        # person, and every one of them has to exist before the card names it.
        self.today_layout(db, subject_id)
        db.commit()

    def fill_project_papers(self, db):
        """Puts a real document behind some placeholders and leaves the rest empty.

        **Both halves matter.** A filled placeholder is what the papers screen
        is for; an empty one is the state that has to read "not yet" rather than
        as something the person failed to do, 20.4. A fixture with only one of
        them can only ever show half the screen.
        """
        documents = [
            row[0]
            for row in db.execute(
                "SELECT id FROM document WHERE deleted_at IS NULL ORDER BY id"
            )
        ]
        if not documents:
            return
        papers = [
            row[0]
            for row in db.execute(
                "SELECT id FROM project_paper WHERE deleted_at IS NULL ORDER BY id"
            )
        ]
        # Every third one, so the mix is visible on one screen rather than
        # needing a scroll to find an empty one.
        for position, paper_id in enumerate(papers):
            if position % 3 != 0:
                continue
            db.execute(
                "UPDATE project_paper SET document_id = ? WHERE id = ?",
                (documents[position % len(documents)], paper_id),
            )

    def connect_entries_to_projects(self, db, subject_id, projects):
        """The latest word: what somebody actually said about this project.

        `DESIGN.md` 20.1, the third of the three answers. **An entry has no
        `project_id` column and does not need one**: the generic `link` table is
        what carries this, and adding a column would be a schema change nobody
        approved.

        **Without these rows the third answer can never render**, which is the
        same defect as every other one this file has had: a feature built, and a
        fixture that never produces what it reads.

        **These are written rather than borrowed from the entries already
        there.** Linking whichever calls happened to be most recent produced
        "She was sitting up and knew who I was" as the latest word on a waiver
        application, which is a true sentence about a good afternoon and says
        nothing about the application. The latest word is what an office said,
        in the office's own flat words, and a fixture that shows the wrong kind
        of sentence there makes a correct screen look broken.

        Calls, because the latest word is almost always something said on the
        phone.
        """
        contact_ids = [
            self.row(
                db,
                "person",
                {
                    "subject_id": subject_id,
                    "display_name": name,
                    "role_label": role,
                    "phone": phone,
                },
                day=0,
            )
            for name, role, phone in PROJECT_CONTACTS
        ]

        for index, project_id in enumerate(projects):
            said = OFFICE_WORDS[index % len(OFFICE_WORDS)]
            for turn, words in enumerate(said):
                # Spread back from the end of the history, so the last one is
                # genuinely the latest and the ones behind it are a history.
                #
                # **And each process is at a different point in its own back
                # and forth**, which is the realism this was missing and it
                # showed. Without the per-project offset every project's latest
                # word landed on the same day, so **the three newest entries in
                # the whole notebook shared one date at every horizon**: the
                # trail card said "June 29, 2026" three times down its spine and
                # no gap marker could ever appear on it, at any seed. Three
                # separate offices do not all call back on the same afternoon.
                # #259.
                day = max(
                    0,
                    self.days - 1 - index * PROCESSES_APART - (len(said) - 1 - turn) * 26,
                )
                entry_id = self.row(
                    db,
                    "entry",
                    {
                        "subject_id": subject_id,
                        "kind": "call",
                        "title": None,
                        "body": words,
                        **self.edtf_day(day),
                    },
                    day=day,
                )
                self.row(
                    db,
                    "link",
                    {
                        "source_table": "entry",
                        "source_id": entry_id,
                        "target_table": "project",
                        "target_id": project_id,
                        "relation": "about",
                    },
                    day=day,
                )
                # **Who said it**, which is what makes screen 14 possible at
                # all. Deliberate rather than sampled: the people pass runs
                # before these entries exist, so a project's calls named nobody
                # and the project people screen was always empty.
                #
                # **The third contact is shared by exactly two projects**, so
                # the cross-project door has something to point at and is not
                # noise. Putting her on every project's last call gave one
                # person four "also in" rows, which turns the one new idea on
                # that screen into wallpaper: seen on the phone, not reasoned.
                shared = index in SHARED_CONTACT_PROJECTS and turn == len(said) - 1
                contact = (
                    contact_ids[-1]
                    if shared
                    else contact_ids[index % (len(contact_ids) - 1)]
                )
                db.execute(
                    "INSERT INTO entry_person (id, created_at, updated_at,"
                    " origin_device, rev, entry_id, person_id)"
                    " VALUES (?, ?, ?, ?, 1, ?, ?)",
                    (
                        self.new_id(),
                        self.ms(day),
                        self.ms(day),
                        self.device,
                        entry_id,
                        contact,
                    ),
                )

    def today_layout(self, db, subject_id):
        """The arranged Today, as a situation template would have set it.

        `contract/DATA-CONTRACT.md` 8.7 and `DESIGN.md` 21.5: nobody ever sees a
        blank Today, so a fixture that writes no layout is a fixture that cannot
        show the surface at all.

        **The point of the spread is the states ladder**, 21.4 and 23.1. A card
        for every rung has to be producible, so this deliberately includes a
        card pointing at a project that is closed, which is the source-closed
        rung, and a measure card whose measure has few readings.
        """
        def source(table):
            row = db.execute(
                f"SELECT id FROM {table} WHERE deleted_at IS NULL ORDER BY id LIMIT 1"
            ).fetchone()
            return row[0] if row else None

        # **A measure with numbers in it, so the chart has a line to draw.**
        # `ORDER BY id` picks by hash, and it kept landing on "How she seemed",
        # which is a measure recorded in words. The card rendered correctly and
        # the chart at tall was never once seen with anything in it. Same family
        # as #325: a card pointing at a source it cannot answer from.
        numeric = db.execute(
            "SELECT m.id FROM measure m JOIN measurement v ON v.measure_id = m.id "
            "WHERE m.deleted_at IS NULL AND v.deleted_at IS NULL "
            "AND v.value_number IS NOT NULL GROUP BY m.id ORDER BY COUNT(*) DESC LIMIT 1"
        ).fetchone()
        # **And one recorded in words, because plenty are.** The app never parses
        # a dose or a value into a number, and a measure that is a sentence has
        # no shape to draw: that is a real state and it needs looking at too.
        wordy = db.execute(
            "SELECT m.id FROM measure m JOIN measurement v ON v.measure_id = m.id "
            "WHERE m.deleted_at IS NULL AND v.deleted_at IS NULL "
            "AND v.value_number IS NULL GROUP BY m.id ORDER BY m.id LIMIT 1"
        ).fetchone()
        measure_id = (numeric or wordy or (None,))[0] if (numeric or wordy) else None
        wordy_measure = wordy[0] if wordy and (not numeric or wordy[0] != measure_id) else None
        # A project that is finished, on purpose: a card pointing at one says so
        # and keeps working as a door, and is removed only by the person's hand.
        closed = db.execute(
            "SELECT id FROM project WHERE deleted_at IS NULL "
            "AND status IN ('done', 'abandoned') ORDER BY id LIMIT 1"
        ).fetchone()
        open_project = db.execute(
            "SELECT id FROM project WHERE deleted_at IS NULL "
            "AND status NOT IN ('done', 'abandoned') ORDER BY id LIMIT 1"
        ).fetchone()
        # **The care team card comes in two**, DESIGN.md 21.7: the row of
        # everyone, and one chosen person with their number as an outlined pill.
        # A fixture with only the first has never shown the second, which is the
        # half of the card the whole "how do I reach them, now" question turns
        # on. #258.
        reachable = db.execute(
            "SELECT id FROM person WHERE deleted_at IS NULL AND archived_at IS NULL "
            "AND phone IS NOT NULL ORDER BY display_name LIMIT 1"
        ).fetchone()
        # **And one nobody has a number for, because plenty of them are.** That
        # is an ordinary state and not a gap, and it is the state that says so
        # on the card rather than leaving an empty pill.
        unreachable = db.execute(
            "SELECT id FROM person WHERE deleted_at IS NULL AND archived_at IS NULL "
            "AND phone IS NULL ORDER BY display_name LIMIT 1"
        ).fetchone()
        # Somebody who has left, which is the source-closed rung for this card:
        # it says so, keeps working as a door, and is removed only by hand.
        departed = db.execute(
            "SELECT id FROM person WHERE deleted_at IS NULL AND archived_at IS NOT NULL "
            "ORDER BY display_name LIMIT 1"
        ).fetchone()

        cards = [
            # The lead. The digest is the default lead in every template, 21.7.
            ("digest", "wide", None, None),
            ("next_up", "wide", None, None),
            ("medications", "small", None, None),
            ("incidents", "small", None, None),
        ]
        if measure_id:
            cards.append(("measure", "tall", "measure", measure_id))
        if wordy_measure:
            cards.append(("measure", "small", "measure", wordy_measure))
        # **A card pointing at a source it cannot answer from is a card nobody
        # has seen working.** `project_standing` takes a project that is
        # actually waiting on somebody, and `project_steps` takes an open one so
        # its cluster counts are visible at all. #325.
        waiting = db.execute(
            "SELECT id FROM project WHERE deleted_at IS NULL "
            "AND status = 'waiting' ORDER BY id LIMIT 1"
        ).fetchone()
        standing_source = waiting or open_project
        if standing_source:
            cards.append(("project_standing", "wide", "project", standing_source[0]))
        if open_project:
            cards.append(("project_date", "small", "project", open_project[0]))
            cards.append(("project_steps", "wide", "project", open_project[0]))
        if closed:
            # **The source-closed rung still has to exist somewhere**, 21.4, and
            # a second steps card on a finished project is what a person's own
            # desk looks like anyway: the card stays until their hand removes it.
            cards.append(("project_steps", "wide", "project", closed[0]))
        cards += [
            ("ask_next_time", "small", None, None),
            ("unfiled", "small", None, None),
            ("money", "small", None, None),
            ("care_team", "wide", None, None),
        ]
        # **Wide, because that is where the number is allowed to be.** 21.3 puts
        # an inline outlined action at wide and tall and nowhere smaller, so a
        # small card pointed at a person would never once have drawn the pill.
        if reachable:
            cards.append(("care_team", "wide", "person", reachable[0]))
        # Wide as well, because "no number yet" is a second line and 21.3 gives
        # a second line to wide and tall. At small this card is a name and a
        # role, which is honest and says nothing about the number either way.
        if unreachable:
            cards.append(("care_team", "wide", "person", unreachable[0]))
        if departed:
            cards.append(("care_team", "small", "person", departed[0]))
        cards += [
            ("emergency_card", "small", None, None),
            ("trail_lately", "tall", None, None),
            ("recent_documents", "wide", None, None),
            ("standing_instructions", "small", None, None),
            ("milestones", "small", None, None),
        ]

        for position, (card_type, size, table, row_id) in enumerate(cards):
            self.row(
                db,
                "today_card",
                {
                    "subject_id": subject_id,
                    "card_type": card_type,
                    "size": size,
                    "sort_index": position,
                    "is_lead": 1 if position == 0 else 0,
                    "source_table": table,
                    "source_id": row_id,
                },
                day=max(0, self.days - 1),
            )

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

    def moment_edtf(self, day_offset, hour, minute):
        """An EDTF at minute precision, which is what a timed thing has.

        `Edtf.Precision.MOMENT` is `YYYY-MM-DDTHH:MM`. Written here so the
        fixture cannot drift from what the app would store: the whole point of
        a fixture is that it produces rows the app itself could have written.
        """
        on = self.start + timedelta(days=day_offset)
        return f"{on.isoformat()}T{hour:02d}:{minute:02d}"

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
            values = {
                "subject_id": subject_id,
                "name": PLACES[index % len(PLACES)],
                "started_edtf": (self.start + timedelta(days=began)).isoformat(),
                "started_start": self.ms(began, 0, 0),
                "started_end": self.ms(began, 23, 59),
            }
            # **A person is in one place at a time**, #219. Every chapter but
            # the last one ends, on the day the next one starts, because a
            # chapter is current exactly when it has no end date and eight
            # current places is the fixture asserting somebody is in eight
            # buildings at once.
            #
            # **It made the screen lie in the way 5.2.1 warns about**: the
            # chapters screen ringed all eight with a gold milestone waypoint,
            # and a milestone is rare by design. If everything is ringed
            # nothing is.
            if index < wanted - 1:
                ends = (index + 1) * span
                values["ended_edtf"] = (self.start + timedelta(days=ends)).isoformat()
                values["ended_start"] = self.ms(ends, 0, 0)
                values["ended_end"] = self.ms(ends, 23, 59)
            made.append(self.row(db, "chapter", values, day=began))
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

    def incidents(self, db, subject_id, chapters, people):
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
            self.incident_thread(db, subject_id, incident_id, day, closed, people)

    def incident_thread(self, db, subject_id, incident_id, reported_day, closed_day, people):
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
            title, role = INCIDENT_STEPS[step % len(INCIDENT_STEPS)]
            entry_id = self.row(
                db,
                "entry",
                {
                    "subject_id": subject_id,
                    "kind": "call" if step else "incident",
                    "title": title,
                    "body": None,
                    "incident_id": incident_id,
                    "occurred_edtf": (self.start + timedelta(days=day)).isoformat(),
                    "occurred_start": self.ms(day, 0, 0),
                    "occurred_end": self.ms(day, 23, 59),
                    "is_unfiled": 0,
                },
                day=day,
            )

            # **An incident thread always names somebody**, which is what makes
            # it an escalation rather than a list of calls. The random sampling
            # in `involve` is right for an ordinary entry, because plenty of
            # what a family writes down is "called the nursing station" with no
            # name attached. It is wrong here: the point of chasing something is
            # that you know who you chased it with and who you were passed to
            # next, and each step above says whose job it was.
            named = self.person_with_role(db, role)
            if named:
                at = self.ms(day)
                db.execute(
                    "INSERT INTO entry_person (id, created_at, updated_at, origin_device,"
                    " rev, entry_id, person_id) VALUES (?, ?, ?, ?, 1, ?, ?)",
                    (self.new_id(), at, at, self.device, entry_id, named),
                )

    def person_with_role(self, db, role):
        """Whoever holds a role, or nobody at a fixture too small to have them."""
        row = db.execute(
            "SELECT id FROM person WHERE role_label = ? AND archived_at IS NULL"
            " ORDER BY created_at LIMIT 1",
            (role,),
        ).fetchone()
        return row[0] if row else None

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
                # **A moment, because it has a time.** #233: this wrote a
                # day precision EDTF beside a 10am instant, which is a row the
                # app itself could never produce. `Repository.dateColumns`
                # derives the columns from `Edtf.resolve`, and a day gets
                # midnight to one millisecond before the next day, so anything
                # reading `scheduled_start` for one of these got a time nobody
                # typed: the appointments screen splits upcoming from past on
                # that value, and an appointment on today's date flipped from
                # "coming up" to "already happened" at 10am rather than at
                # midnight.
                #
                # **A moment resolves to start == end**, which is what
                # `Edtf.Precision.MOMENT` does, so the hour long end this used
                # to write was not a shape the app has either.
                "scheduled_edtf": self.moment_edtf(day, 10, 0),
                "scheduled_zone": "America/New_York",
                "scheduled_start": self.ms(day, 10, 0),
                "scheduled_end": self.ms(day, 10, 0),
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
        """Projects at various stages, including the ones nobody finished.

        Returns the ids in the order they were made, so what an office said
        can be matched to the project it was said about. Ordering by id
        later does not work: ids are hashes and sort arbitrarily, which put
        the billing office's words on the power of attorney.
        """
        made = []
        for index in range(max(len(PROJECT_STATES), self.scaled(FULL["projects"]))):
            state = PROJECT_STATES[index % len(PROJECT_STATES)]
            # **Which of the three answers this one opens with.** All three
            # shapes have to exist in the fixture or two of the three project
            # home screens can never be looked at on the phone. DESIGN.md 20.3.
            lead = PROJECT_LEADS[index % len(PROJECT_LEADS)]
            day = self.rng.randrange(0, max(1, self.days))
            project_id = self.row(
                db,
                "project",
                {
                    "subject_id": subject_id,
                    "name": PROJECTS[index % len(PROJECTS)],
                    "template_id": PROJECT_TEMPLATES[index % len(PROJECT_TEMPLATES)],
                    "status": state,
                    # **A project waiting on somebody says who**, which nothing
                    # in the fixture ever set. `report_today_rungs.py` found it:
                    # `project_standing` was on its none-yet rung at all six
                    # horizons, so the card whose question 21.7 states as "whose
                    # hands, since when" had never once rendered an answer, and
                    # every device pass read that emptiness as the card being
                    # fine. #325.
                    **(
                        {
                            "waiting_on": PROJECT_WAITING_ON[
                                index % len(PROJECT_WAITING_ON)
                            ],
                            "waiting_since": self.ms(
                                min(self.days - 1, day + self.rng.randrange(2, 40)),
                            ),
                        }
                        if state == "waiting"
                        else {}
                    ),
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
            # **The busy stretch is actually busy.** Two intense weeks of small
            # parallel arrangements is what the shape is for, and a fixture that
            # gave it three steps meant the clustering in 20.3 was never seen
            # doing anything: three areas holding one row each. The other two
            # shapes keep the short list, because their weight is the standing
            # and the date rather than the steps.
            total = self.rng.randrange(7, 11) if lead == "steps" else self.rng.randrange(2, 7)
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
                # **The busy stretch needs its clusters and its handler tags**,
                # DESIGN.md 20.3, and a fixture that never writes them means a
                # built feature is never seen. Only the steps shape carries
                # them, which is also what the screens expect: null is the
                # normal state on the other two.
                if lead == "steps":
                    values["cluster"] = STEP_AREAS[step % len(STEP_AREAS)]
                    if step % 3 == 0:
                        values["handler_label"] = HANDLERS[
                            (index + step) % len(HANDLERS)
                        ]
                self.row(db, "project_step", values, day=day)

            self.project_shape(db, project_id, index, day, lead, state)
            made.append(project_id)
        return made

    def project_shape(self, db, project_id, index, day, lead, state):
        """The road, where it stands, the dates, the papers, and the date kinds.

        `contract/DATA-CONTRACT.md` 8.7. **Without this the Projects grid cannot
        be looked at on the phone at all**: every project screen leads with one
        of the three answers in `DESIGN.md` 20.1, and two of the three are read
        from tables that had no fixture writer.
        """
        stages = PROJECT_STAGES[index % len(PROJECT_STAGES)]

        # How far along the road this one is. A finished project has reached the
        # end; an abandoned one stopped where it stopped, which is honest and is
        # what the screen has to be able to draw.
        reached_through = {
            "done": len(stages),
            "active": 2,
            "waiting": 2,
            "stalled": 1,
            "abandoned": 1,
        }.get(state, 1)

        current_stage_id = None
        for position, name in enumerate(stages):
            values = {
                "project_id": project_id,
                "name": name,
                "sort_index": position,
            }
            if position < reached_through:
                at = min(self.days - 1, day + position * 21)
                values["entered_edtf"] = (self.start + timedelta(days=at)).isoformat()
                values["entered_zone"] = "America/New_York"
                values["entered_start"] = self.ms(at, 0, 0)
                values["entered_end"] = self.ms(at, 23, 59) + 59_999
            stage_id = self.row(db, "project_stage", values, day=day)
            if position == reached_through - 1:
                current_stage_id = stage_id

        if current_stage_id:
            db.execute(
                "UPDATE project SET current_stage_id = ?, lead = ? WHERE id = ?",
                (current_stage_id, lead, project_id),
            )
        else:
            db.execute(
                "UPDATE project SET lead = ? WHERE id = ?", (lead, project_id)
            )

        # **Where it stands, with its history.** Two entries on most, so the
        # screen has something to show and the trail has a sequence. An
        # abandoned project keeps the last thing anybody said, because that is
        # the record.
        for turn, (holder, activity) in enumerate(
            STANDING[index % len(STANDING)]
        ):
            since = min(self.days - 1, day + turn * 34)
            self.row(
                db,
                "project_standing",
                {
                    "project_id": project_id,
                    "holder_label": holder,
                    "activity": activity,
                    "since_edtf": (self.start + timedelta(days=since)).isoformat(),
                    "since_zone": "America/New_York",
                    "since_start": self.ms(since, 0, 0),
                    "since_end": self.ms(since, 23, 59) + 59_999,
                },
                day=since,
            )

        # **The dates, and deliberately one of each side of today.** D113 says
        # the screen leads with the soonest that has not passed and falls back
        # to the most recent when they all have, so a fixture with only future
        # dates can never show the passed rung of the states ladder, 21.4.
        kinds = PROJECT_DATE_KINDS[index % len(PROJECT_DATE_KINDS)]
        for position, label in enumerate(kinds):
            self.row(
                db,
                "project_date_kind",
                {"project_id": project_id, "label": label, "sort_index": position},
                day=day,
            )

        for offset, source in PROJECT_DATE_OFFSETS[index % len(PROJECT_DATE_OFFSETS)]:
            # Offsets are counted from the end of the history, which is today
            # for a fixture, so a negative one has passed and a positive one has
            # not.
            at = self.days - 1 + offset
            on = self.start + timedelta(days=at)
            self.row(
                db,
                "project_date",
                {
                    "project_id": project_id,
                    "kind": kinds[abs(offset) % len(kinds)],
                    "due_edtf": on.isoformat(),
                    "due_zone": "America/New_York",
                    "due_start": int(
                        datetime.combine(on, datetime.min.time()).timestamp() * 1000
                    ),
                    "due_end": int(
                        datetime.combine(on, datetime.min.time())
                        .replace(hour=23, minute=59)
                        .timestamp()
                        * 1000
                    )
                    + 59_999,
                    "source_note": source,
                },
                day=max(0, min(self.days - 1, at)),
            )

        # **The papers.** They are written empty here and some are filled in
        # later, once documents exist: `fill_project_papers` runs after
        # `documents` for the same reason `involve` runs last. An empty
        # placeholder reads "not yet" and never as an error, 20.4, and a fixture
        # where every placeholder is empty cannot show the other half of that.
        for position, name in enumerate(PROJECT_PAPERS[index % len(PROJECT_PAPERS)]):
            self.row(
                db,
                "project_paper",
                {
                    "project_id": project_id,
                    "name": name,
                    "sort_index": position,
                    "direction": ["received", "sent"][position % 2]
                    if position < 2
                    else None,
                },
                day=day,
            )

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

# Who a waiting project is waiting on. **An office rather than a person's name**
# in most cases, because that is what the answer usually is and because a card
# on the front screen naming an individual is a different privacy question than
# one naming a department. #325.
PROJECT_WAITING_ON = [
    "The county benefits office",
    "The insurer's appeals desk",
    "Dr. Okafor's office, for the letter",
    "The housing authority",
]

# Which of the three answers each project opens with, DESIGN.md 20.3. All three
# are here because two of the three project home screens cannot be looked at on
# the phone otherwise.
# Everything below is indexed by a project's position in PROJECTS, and each list
# is written in that order.
#
# **They were written generically once and it showed on the phone**: the power
# of attorney had an appeal's stages, an appeal deadline as its date kind, and a
# denial letter among its papers. Every one of those is a true sentence about
# some project and a wrong one about that project, and a fixture that puts the
# wrong words on a screen makes a correct screen look broken. Same defect as
# OFFICE_WORDS had, found the same way.

# Which of the three answers each project opens with, DESIGN.md 20.3. All three
# are here because two of the three project home screens cannot be looked at on
# the phone otherwise.
# **The first three have to cover all three shapes**, because PROJECT_STATES
# puts the fourth and fifth in done and abandoned, which fold away on the
# projects screen. A shape that only exists on a finished project is a shape
# nobody looking at the fixture will ever open.
PROJECT_LEADS = ["standing", "steps", "date", "steps", "standing"]

# The road each one runs along, taken from the built-in bundles in 20.4 where
# the project matches one, so what the fixture draws is what a template would
# actually have produced.
PROJECT_STAGES = [
    ["Applied", "In review", "Decision"],
    ["Prepared", "Signed", "Sent to them", "Accepted"],
    ["Decision received", "Preparing", "Submitted", "Answered"],
    ["Date set", "Sorting", "Cleared"],
    ["Asked around", "On a list"],
]

# Whose hands it has been in, and what was happening there. **Stated as fact and
# never as a complaint**, DESIGN.md 20.7 and 22: a caseworker is a caseworker,
# nobody is an adversary, and waiting is not framed as somebody's failure.
STANDING = [
    [
        ("The county", "reviewing the application"),
        ("The county", "waiting on the bank statements"),
    ],
    [
        ("The bank", "checking the copy we sent"),
        ("The bank's legal team", "reviewing it"),
    ],
    [
        ("The insurer", "reviewing the appeal"),
        ("The review panel", "scheduled for the second Thursday"),
    ],
    [("The facility", "holding the boxes in the storage room")],
    [
        ("Us", "calling around"),
        ("The practice", "adding her to the spring round"),
    ],
]

# The kinds of date each situation tends to have, offered as chips when a date
# is recorded, 20.4. Never a closed set: recording a date of a kind not listed
# here is allowed.
PROJECT_DATE_KINDS = [
    ["Filing deadline", "Decision expected", "Renewal"],
    ["Sent", "Response expected"],
    ["Appeal deadline", "Hearing", "Answer expected"],
    ["Room must be clear", "Collection"],
    ["Asked", "Visit expected"],
]

# Days from the end of the history, so a negative one has already passed and a
# positive one has not. **Both sides of today on purpose**, because the passed
# rung of the states ladder cannot be seen otherwise, 21.4 and D113.
#
# The future ones are stated in terms of UPCOMING_DAYS rather than as small
# numbers of their own, and that is the whole reason they work: the history ends
# on a fixed date, so an offset of "+21 days" stopped being in the future three
# weeks after that date and the fixture quietly lost its upcoming rung. It has
# the same known staleness as the upcoming appointment above, it goes stale on
# the same day, and moving HISTORY_ENDS forward fixes both at once.
PROJECT_DATE_OFFSETS = [
    [(-34, "the letter of March 5"), (UPCOMING_DAYS - 40, "the letter of March 5")],
    [(-6, "the receipt"), (UPCOMING_DAYS, "the clerk, by phone")],
    [(UPCOMING_DAYS + 20, "the notice they sent")],
    [(-12, "the facility, by phone")],
    [(-58, "the first call"), (UPCOMING_DAYS - 10, "the practice, by phone")],
]

# The papers each one usually needs, as named placeholders, 20.4.
PROJECT_PAPERS = [
    ["The application", "Proof of income", "Bank statements", "The award letter"],
    ["The signed original", "The certified copy", "Their acknowledgment"],
    ["The assessment", "The appeal form", "The doctor's letter"],
    ["The inventory", "The storage receipt"],
    ["The referral", "The visit note"],
]

# What each office actually said, in the order it was said, oldest first.
#
# **The office's own flat words**, DESIGN.md 20.1 and 22. Nobody here is an
# adversary and nothing is performed: a caseworker says where the file is, a
# clerk says what is missing, and none of it is framed as a setback or a win.
# These are the sentences a person repeats at the start of the next call, which
# is the whole reason the latest word is one of the three answers.
OFFICE_WORDS = [
    # Medicaid application
    [
        "They have the application. It goes to a nurse reviewer next.",
        "It is with the nurse reviewer. You should hear something within two weeks.",
        "Still with the reviewer. They said to call back after the 15th.",
    ],
    # Get the power of attorney recognized
    [
        "They will not accept the copy. It has to be the one with the raised seal.",
        "The seal copy was received. It goes to their legal team.",
        "Legal has accepted it. The account should show it in a few days.",
    ],
    # Appeal the level of care assessment
    [
        "The appeal was received. They will write to us either way.",
        "It is scheduled for the panel on the second Thursday.",
    ],
    # Move her belongings out of the old room
    [
        "The room has to be cleared by the end of the month.",
        "They can hold the boxes in the storage room for a week if we need it.",
    ],
    # Find a dentist who will come to the facility
    [
        "That practice stopped visiting facilities last year.",
        "This one does visit. They are taking names for the spring round.",
    ],
]

# The area each step in PROJECT_STEPS belongs to, 20.3, indexed alongside it.
#
# **Parallel to the steps and never round robin.** This was four discharge areas
# handed out by position, which put "The house", "The ride" and "Equipment" over
# three steps of a power of attorney, each cluster holding exactly one row. It
# read as nonsense on the phone and it was: the areas belonged to a hospital
# discharge and the steps belonged to an office process. The same defect the
# handler list already had, fixed the same way.
#
# Indexed with the step, the clusters come out uneven and real, which is what a
# busy stretch actually looks like: several calls, a couple of pieces of paper,
# and one thing somebody else is waiting on.
STEP_AREAS = [
    "The phone calls",   # Call and ask what form it is
    "The paperwork",     # Get the form
    "What they need",    # Find the last three bank statements
    "The paperwork",     # Send it certified
    "The phone calls",   # Follow up after two weeks
    "The phone calls",   # Ask for it in writing
    "The paperwork",     # Check what the deadline actually is
    "What they need",    # Get a copy of the last letter they sent
    "The phone calls",   # Ask who is handling it now
    "The phone calls",   # Confirm they received it
]

# Who said they would handle a step. **A label and never an identity**, D108:
# no account, no address, and nothing that leaves the device.
#
# Kept to people a family would name on any project rather than to a role that
# belongs to one kind of process: "The discharge planner" was in this list and
# turned up handling a step on a power of attorney.
HANDLERS = ["My brother", "Me", "My sister", "The office"]

PROJECTS = [
    "Medicaid application",
    "Get the power of attorney recognized",
    "Appeal the level of care assessment",
    "Move her belongings out of the old room",
    "Find a dentist who will come to the facility",
]

# Which template each of those was started from, and None for the ones somebody
# wrote themselves.
#
# **Every project came out with no template behind it**, so the template library
# could never show what any template had produced, which is the whole reason it
# is a library rather than a catalog. Three of these five are plainly one of the
# sixteen and two plainly are not, which is also the honest mix: families start
# things the catalog never heard of.
#
# The ids are held to the real catalog by `check_fixtures.py`, so a template
# renamed or removed in `templates/data/projects.json` fails the build here
# rather than producing a project pointing at nothing.
PROJECT_TEMPLATES = [
    "medicaid_ltc",
    "legal_documents",
    "discharge_appeal",
    None,
    None,
]

PROJECT_STEPS = [
    "Call and ask what form it is",
    "Get the form",
    "Find the last three bank statements",
    "Send it certified",
    "Follow up after two weeks",
    "Ask for it in writing",
    "Check what the deadline actually is",
    "Get a copy of the last letter they sent",
    "Ask who is handling it now",
    "Confirm they received it",
]

# The two lists are read by the same index, so a step added without its area
# would silently take somebody else's.
assert len(STEP_AREAS) == len(PROJECT_STEPS), "every step needs its area"

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

# The people on the other end of a long process, who are not the care team.
#
# **A project's people and the care team are different lists**, DESIGN.md 20.5
# screen 14. Without these the project people screen could only ever show ward
# nurses, which is the wrong answer dressed as a right one, or nothing at all.
#
# **The last one is on two projects on purpose.** The cross-project door is the
# one new navigation idea on that surface and a fixture that never puts anybody
# in two processes cannot show it, which is the same defect as #229 and #237.
# How far apart two processes are in their own back and forth.
#
# **Nine days, which is over `Distance.THRESHOLD_DAYS` when two of them stack.**
# A marker appears at fourteen days and no sooner, so a spacing under seven
# would have produced the same silent spine this exists to fix. Nine puts the
# newest three entries of a notebook at roughly nought, nine and eighteen days,
# which is one office that came back last week and one that has not been heard
# from in over two, and that is what a caregiver's trail actually looks like.
PROCESSES_APART = 9

PROJECT_CONTACTS = [
    ("Denise Alvarado", "Intake caseworker", "555 0114"),
    ("R. Boyd", "Intake supervisor", "555 0117"),
    ("Colleen Marsh", "Records office", "555 0128"),
    ("Hector Salas", "Housing coordinator", "555 0133"),
    ("Priya Nandakumar", "Benefits adviser", "555 0139"),
    # The shared one, and the last on purpose: see below.
    ("Marisol Vega", "Appeals clerk", "555 0121"),
]

# Which projects the shared contact turns up in. Two, and only two: she is the
# whole reason the cross-project door exists, and putting her on all five gave
# one person four "also in" rows, which turns that idea into wallpaper.
SHARED_CONTACT_PROJECTS = (0, 1)

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
