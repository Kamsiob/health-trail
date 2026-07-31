# CLAUDE.md

Health Trail by Kamsiob. A local-first care notebook for the family member who is the point person for someone else's care. Android, Kotlin, offline, no account, no cloud.

This file is deliberately short. It is loaded automatically every session and it is the last thing to survive context compaction, so it holds only the rules that must never be lost. Everything else lives in the documents named at the bottom. **Do not add to this file unless a rule genuinely must survive compaction.**

## Hard rules

1. **Read HANDOFF.md before doing anything, every session.** It is the current state of the work. If you find yourself re-reading files you already read this session, compaction has happened: stop, read HANDOFF.md again, and re-orient before continuing.
2. **No medical advice, no legal advice, no interpretation.** The app records, organizes, and counts. It never concludes. No target ranges, no normal values, no thresholds, no color coding by value, no judgments on any measurement, no educational content.
3. **The schema is fixed by contract/DATA-CONTRACT.md.** Every row has a locally generated id, created and updated timestamps, a revision, an origin device, and a tombstone column. Deletion is always a tombstone, never a row removal. Every write appends to the change log in the same transaction. Do not change any of this without the owner's explicit decision.
4. **No em dashes in anything a user or reader sees.** App copy, documentation, README, commit messages, store text. Commas, periods, and colons instead. Source code is exempt where a character is functionally required.
5. **American English everywhere.** Color, organize, behavior, artifact, license, catalog.
6. **Never run a destructive command.** No `rm -rf`, no `git reset --hard`, no `git checkout .`, no `git clean -fd`, no force push, no branch deletion, no history rewriting. If you believe one is needed, stop and write it to the BLOCKED section of DECISIONS.md instead.
7. **Commit and push after every working increment.** Never let more than one unit of work sit uncommitted. Git is the recovery mechanism if a session loses its memory.
8. **Subagents never write anything.** They read, run, check, and report. You act on their reports. See AGENTS.md.
9. **Three attempts, then move on.** If the same thing fails three times, stop, write what you tried and what happened to the BLOCKED section of DECISIONS.md, and start the next item. Never loop.
10. **Never stop to ask a question.** Decide, log the decision in DECISIONS.md, and continue. Only a genuine blocker that only the owner can resolve goes to BLOCKED.
11. **Nothing unfinished reaches the person.** No blank area, no placeholder string, no stub, no debug label, no truncation, no layout that only holds together with tidy sample data. A screen ships with its empty, one-item, many-item, partially-filled, long-text, longest-language, loading, and error states, and right to left, or it is not built. This holds whether or not the screen was ever mocked up.
12. **Undesigned screens are composed, never invented, and always logged.** The 27 screens in `reference/screen-grid.html` do not cover everything. When you reach one that is not drawn: build it from the existing components, then immediately open a `needs-design-review` issue with a real device screenshot, add it to `DESIGN.md` section 8, and list it in `HANDOFF.md`. All three, at the moment you build it, never saved up for a phase gate. Full protocol in `DESIGN.md` section 10.
13. **Partial is a finished state.** Never require completion to apply or to save, never block on a missing field, and never frame unfinished work as a deficiency. No progress meters on the person's own diligence, no completion percentages, no prompts to finish setting up. An unfilled slot reads as "not yet," never as an error.

## Where everything is

`README-START-HERE.md` maps the folder. `MASTER_SPEC.md` is the features and the phase plan. `contract/DATA-CONTRACT.md` is the schema and export format. `DESIGN.md` plus `reference/screen-grid.html` is what it looks like. `TESTING-PERSONAS.md` is how it gets tested. `AGENTS.md` is delegation. `RUN-SAFETY.md` is how a long unattended run stays safe. `PROJECT-DELTAS.md` overrides `kamsiob-project-template.md`.

Precedence: verified code, then HANDOFF.md, then DECISIONS.md, then the data contract for data questions, then DESIGN.md for visual questions, then RUN-SAFETY.md and AGENTS.md for how-to-work questions, then PROJECT-DELTAS.md, then MASTER_SPEC.md, then the template.
