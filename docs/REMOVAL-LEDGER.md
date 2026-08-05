# REMOVAL-LEDGER.md, Health Trail by Kamsiob

**Nothing in this repository is deleted because a newer thing replaced it.** Superseded code is **frozen**: never called, never extended, never fixed, and never translated. This file is the record of what was superseded, by what, and when, so that a session reading a frozen file knows immediately that it is reading history rather than a live path.

**Created 2026-08-04** with the adoption of the Projects and Today grids, D112. The rule was being followed in practice and had no file to record it in.

## Why freezing rather than deleting

**A deletion loses the reasoning along with the code.** Six months later somebody re-derives the same approach, hits the same wall, and has no way to know it was already tried. The commit history holds the diff but not the judgment, and nobody greps a repository's history before writing a screen.

**And a frozen file is honest in a way a deleted one is not.** A screen that still exists but is never reached is visible to anybody reading the source, which means the question "why is this still here" gets asked and answered, rather than the answer being lost with the file.

## What the rule means in practice

- **Never called.** Nothing in the live path may reference a frozen file.
- **Never extended.** A frozen screen does not gain a feature because it was convenient.
- **Never fixed.** A defect in a frozen screen is not a defect: it is history. If it must be fixed, it is not frozen, and this file says so.
- **Never translated.** A frozen string does not consume a translator's time or a catalog key.
- **Its tests go with it.** A test asserting frozen behavior is frozen too, or removed with a row here saying which.

## The ledger

| What | Superseded by | When | State |
|---|---|---|---|
| Nothing yet | | | |

**The first rows arrive when the Today and Projects conversions begin.** The adoption on 2026-08-04 was documentation only: no application code was written, so nothing has been superseded in fact yet. What it makes obsolete, and what will appear here as each conversion lands:

- **The current Today dashboard**, superseded by `reference/today-grid.html` and `DESIGN.md` section 21. Today becomes a lead slot plus a field of card instances the person arranges.
- **The current checklist Projects**, superseded by `reference/projects-grid.html` and `DESIGN.md` section 20. A project becomes three answers with three shapes, and the checklist survives as one of the three rather than as the whole.

**A row is added at the moment the code is frozen, not at a phase gate**, which is the same discipline rule 12 applies to design reviews and for the same reason.
