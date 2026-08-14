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
    ("check_readable_coverage.py", "every schema column is rendered in the archive or explicitly not"),
    ("check_readable_labels.py", "every table and column the archive renders has a word in all four languages"),
    ("check_decrypt_tool.py", "an archive still opens with only the standalone decryptor"),
    ("check_format_spec.py", "the published format specification still matches the code"),
    ("check_query_ordering.py", "every query feeding a render or an export orders itself"),
    (
        "check_digest_sections.py",
        "every table the digest maps is one the change log actually writes",
    ),
    (
        "check_dead_gestures.py",
        "no control announces an action it does not perform",
    ),
    (
        "check_reader_coverage.py",
        "every screen is walked by the reader check, so the claim cannot drift",
    ),
    ("check_contrast.py", "every color pair in both themes against the WCAG AA floors"),
    ("check_ink3_is_not_text.py", "ink3 is non-text only, so no label is drawn at 2.37:1"),
    ("check_self_contained.py", "nothing outside this project is named in the repository"),
    ("check_i18n.py", "the four locale catalogs against each other and their rules"),
    (
        "check_concatenation.py",
        "no sentence a person reads is glued together in Kotlin instead of the catalog",
    ),
    ("check_string_keys.py", "every catalog key the app asks for exists, so no screen crashes on opening"),
    ("check_live_views.py", "no base table read outside a live view, so tombstones cannot leak"),
    ("check_fixtures.py", "the fixture generator is deterministic and hits its stated scale"),
    ("check_hook_quoting.py", "hook commands are quoted, so a guard cannot be a silent no-op"),
    (
        "check_guard.py",
        "the destructive command guard refuses what it must and lets prose about it through",
    ),
    ("check_text_sources.py", "every source file is searchable text, so grep sees the whole repository"),
    (
        "check_bidi_isolation.py",
        "the person's own words reach a screen isolated, or say in a comment why not",
    ),
    (
        "check_silent_clip.py",
        "no text stops mid-word without an ellipsis, so nothing truncates invisibly",
    ),
    (
        "check_cross_references.py",
        "every section pointer between documents resolves to a section that exists",
    ),
    (
        "check_token_drift.py",
        "the count of measurements not coming from a token falls and never rises",
    ),
    (
        "check_type_ladder.py",
        "DESIGN.md 5.1 and Type.kt state the same size for every role",
    ),
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
