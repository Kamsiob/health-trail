# Findings, 2026-08-18. Eleven panels.

**What this is.** The owner asked for internal review before the app is exported: mock focus groups of end users, a fresh second set, an expert design and product audit, and a specialist group on code, security, database and durability. **Nothing here has been executed.** This folder is the input to the next session, not a record of work done.

**How it was produced.** Eleven independent panels. Round one found, round two refuted and grouped by root cause, round three resolved the disagreements. Every engineering claim carries a file and line. The main session re-verified the highest-stakes claims itself and the corrections are marked `MAIN-SESSION VERIFICATION` inline; **read those, some panels overreached.**

| file | what it holds |
|---|---|
| `../FINDINGS.md` | **Start here.** The synthesis, the ranked order, and the corrections. |
| `users-facility.md`, `users-home.md` | The real user panels. Fourteen people, plain language, no code. |
| `walks-first-week.md`, `walks-long-haul.md`, `walk-supplements.md` | Ten task walks, tap by tap, where each broke. |
| `engineering-r1.md` | The first four panels, before the owner corrected their framing. Kept because the findings are sound. |
| `engineering-r2-skeptic.md` | What was refuted, with the evidence that killed it. |
| `engineering-r2-rootcause.md` | Orphan columns, and ten root causes under 114 complaints. |
| `engineering-r3-moderator.md` | The contested questions resolved, and the closing ranked list. |
| `audit-chrome-motion.md` | The owner's three observations, answered as one design each. |
| `audit-destinations.md`, `audit-sections.md` | Every screen, three passes. |
| `spec-security.md`, `spec-database.md`, `spec-durability.md` | The specialist group. |
| `owner-defects.md` | The three the owner saw on the phone, traced to the code. |

**The rule that governs all of it:** nothing here overrides `CLAUDE.md`, the data contract, or a closed decision in `DECISIONS.md`. Where a finding wants one reopened, it is marked OWNER and waits.
