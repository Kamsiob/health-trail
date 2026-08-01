#!/usr/bin/env python3
"""Run every compliance check and report all failures rather than the first.

These are the app's promises written as tests. A promise that is not tested is a
promise that will eventually be broken, so TESTING-PERSONAS.md section 5 requires
them to run in continuous integration rather than as a manual review.

Coverage grows as the app does. What runs today is what exists today, and the
list below says plainly which of the section 5 checks are not yet implemented,
so nobody reads a passing run as broader coverage than it is.

Usage: python3 tools/checks/run_all.py

Kamsiob, AGPL-3.0.
"""

import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent

CHECKS = [
    ("check_copy.py", "House style: no em dashes, American English"),
    ("check_templates.py", "The 57 templates against their schema and content rules"),
    ("check_contract_isolation.py", "/contract stays platform neutral, no second schema"),
    ("check_schema.py", "schema.sql against the data contract, shape and behavior"),
    ("check_contrast.py", "every color pair in both themes against the WCAG AA floors"),
    ("check_self_contained.py", "nothing outside this project is named in the repository"),
    ("check_i18n.py", "the four locale catalogs against each other and their rules"),
    ("check_live_views.py", "no base table read outside a live view, so tombstones cannot leak"),
]

# TESTING-PERSONAS.md section 5 checks that are not implemented yet, each named
# with what has to exist first. Printed on every run so a green result is never
# mistaken for full coverage.
NOT_YET = [
    ("No rendered range, threshold, color coded value, arrow, or judgment",
     "needs the chart and row components, Phase 1"),
    ("No chart interpolates across a gap",
     "needs the chart component, Phase 1"),
    ("Pattern language only above the minimum-data threshold",
     "needs the deterministic engine, Phase 1"),
    ("Engine output byte identical to the golden vectors in four locales",
     "needs the engine and contract/test-vectors, issue #15"),
    ("Every standing instruction rendering shows a tag",
     "needs the standing instructions screen, Phase 3"),
    ("Every template string present in all four locale catalogs",
     "needs contract/i18n, issue #13"),
    ("Export, wipe, import round trip equality",
     "needs the export container, issue #9"),
]


def main():
    print("Content compliance checks\n" + "=" * 60, flush=True)
    failed = []
    for script, description in CHECKS:
        print(f"\n> {description}", flush=True)
        result = subprocess.run([sys.executable, str(HERE / script)])
        if result.returncode != 0:
            failed.append(script)

    print("\n" + "=" * 60)
    print("Not yet covered, and what each one is waiting on:")
    for description, waiting in NOT_YET:
        print(f"  [ ] {description}\n        {waiting}")

    print("\n" + "=" * 60)
    if failed:
        print(f"FAILED: {', '.join(failed)}")
        return 1
    print(f"All {len(CHECKS)} implemented checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
