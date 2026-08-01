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
import sqlite3
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "fixtures"))

import generate as fixtures  # noqa: E402

# What year five has to look like, from issue #17. Checked as a floor and a
# ceiling, because a generator that quietly produces a tenth of the data still
# passes every test that only asks whether it ran.
YEAR_FIVE = {
    "entry": (1200, 2000),
    "chapter": (8, 10),
    "care_thread": (5, 7),
    "milestone": (12, 18),
}


def counts(path):
    db = sqlite3.connect(path)
    try:
        return {
            table: db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            for table in ("entry", "chapter", "care_thread", "milestone", "measurement")
        }
    finally:
        db.close()


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
        "different seed is not, all six points generate and grow, and year five "
        "hits its stated scale."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
