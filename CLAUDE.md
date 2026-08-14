# CLAUDE.md

Health Trail by Kamsiob. Local-first care notebook for the family member who is the point person for someone else's care. Android, Kotlin, offline, no account, no cloud.

Loaded every session and the last thing to survive compaction. **Only rules that must never be lost. Do not add to this file.** Written for a machine: fragments, no filler.

## Start of every session

**`HANDOFF.md`, then `gh issue view 321` ("START HERE", the order of work). Both, before anything.** Never read `docs/RUN-LOG.md` to orient; it is history.

Re-reading files you already read this session means compaction happened: stop, read both again, re-orient.

**Read on demand, never in bulk.** `HANDOFF.md` section 2 is the ladder. `docs/TRAPS.md` is one section, chosen from its own table. `DESIGN.md` is by numbered section. `DECISIONS.md` is searched for a D number.

## Rules

Numbers are load bearing: 400+ places in the sources, the documents and the issues cite "rule N". **Never renumber.** Rules 1 and 1a are the session-start block above.

2. **No medical or legal advice, no interpretation.** Record, organize, count. Never conclude. No target ranges, normal values, thresholds, color coding by value, judgments on any measurement, educational content.
3. **Schema is fixed by `contract/DATA-CONTRACT.md`.** Every row: local id, created, updated, revision, origin device, tombstone. Deletion is always a tombstone. Every write appends to the change log in the same transaction. Changing any of this needs the owner.
4. **No em dashes in anything a person reads.** App copy, docs, README, commit messages, store text. Use commas, periods, colons. Source code exempt where functionally required.
5. **American English.** Color, organize, behavior, artifact, license, catalog.
6. **Never run a destructive command.** No `rm -rf`, `git reset --hard`, `git checkout .`, `git clean -fd`, force push, branch deletion, history rewriting. If one seems needed, write it to `DECISIONS.md` BLOCKED instead.
7. **Commit and push after every working increment.** Never leave more than one unit uncommitted. Git is the recovery mechanism.
8. **Subagents never write.** They read, run, check, report. `AGENTS.md`.
9. **Three attempts, then move on.** Same failure three times: write what was tried and what happened to `DECISIONS.md` BLOCKED, start the next item. Never loop.
10. **Never stop to ask.** Decide, log in `DECISIONS.md`, continue. Only a genuine owner-only blocker goes to BLOCKED.
11. **Nothing unfinished reaches the person.** No blank area, placeholder, stub, debug label, truncation, or layout that only holds with tidy sample data. A screen ships with its empty, one-item, many-item, partially-filled, long-text, longest-language, loading and error states, and RTL, or it is not built. Whether or not it was ever mocked up.
12. **Undesigned screens are composed, never invented, and always logged.** `reference/screen-grid.html` has 25 screens and does not cover everything. Reaching an undrawn one: build from existing components, then immediately (a) open a `needs-design-review` issue with a real device screenshot, (b) add to `DESIGN.md` 14, (c) list in `HANDOFF.md`. All three, at the moment of building. Protocol: `DESIGN.md` 13, and 13.4 is the three places.
13. **Partial is a finished state.** Never require completion to save, never block on a missing field, never frame unfinished work as a deficiency. No progress meters on the person's own diligence, no completion percentages, no prompts to finish setting up. An unfilled slot reads as "not yet", never as an error.
14. **No screen ships thin, and the bar is retroactive.** Functionally correct and visually plain is not done. When the standard rises, everything already built comes up to it. `DESIGN.md` 15, and 16.5 is the retroactive clause.
15. **Hierarchy before decoration.** Decide what matters most; give it weight through size, position and space rather than color; group what belongs together under a quiet mono eyebrow; let the rest recede; give it room. Uniform weight pushes the sorting onto someone already exhausted. `DESIGN.md` law 1 in section 2, type ladder in 5.1.
16. **Everything the person touches responds.** Visible press state on every button, row, chip, tappable card, one treatment app-wide (`DESIGN.md` 15, interaction grammar in 9). Motion from the section 10 tokens, never inline. A control that does nothing on press reads as broken.
17. **Dates are flexible, always editable, never falsely precise.** Stored as EDTF, displayed at exactly the precision expressed, unknown is a first-class value that saves and appears in the trail, every date editable forever from the entry itself. `contract/DATA-CONTRACT.md`, `DESIGN.md` 9.2.
18. **Links go both ways.** If A shows B, B shows A. Carry context forward instead of asking again. Count the taps: four where two would do gets abandoned in a hallway.
19. **Accessibility is a gate, not a phase.** `DESIGN.md` 12 clears before any issue closes, including screens already built. Verified with the reader on, font at maximum, reduced motion actually enabled, never by reading code. **The phone's accessibility settings may be changed for that** (font scale, animation duration, TalkBack) **only if the prior value is recorded before changing and restored exactly after.** Owner-granted exception; extends to nothing else on the device.
20. **The complexity lives in the code, never on the screen.** Any time the interface asks the person to understand how the app stores or organizes something, the code has failed to absorb its own complexity.
21. **Look at it on the phone before closing anything.** Install, open, use it, not just a screenshot. Where does the eye go first, where does it stick, what competes that should not, how many taps, did everything respond. Fix the worst thing, look again.
22. **Pick the component from the shape of the content.** Library is `DESIGN.md` 7. Tile for a fixed set of destinations. Dense row for a long scanned list. Card only for three or more lines actually read. One hero per screen at most. Spine for anything sequential. Thumbnail where the app holds the person's own paper. Avatar for a person. **Not everything is a card, and making everything a card is why nothing stands out.** Reusing the layout pattern of the screen you arrived from is a deliberate choice, not the default. Retroactive per rule 14.
23. **When more than one answer is defensible, take the easiest for the person, provided it is safe, private and compatible.** Those three filter, they are not a tiebreaker applied afterward. Resolves open questions; does **not** reopen closed ones, and is never a reason to weaken the content rules, the data contract, D67, or anything already decided. D86.
24. **Version one ships English.** Acceptance criteria read "verified in English". **RTL is still verified on every screen**, against a forced layout direction rather than Arabic content. The four catalogs stay complete and checked, because letting the waiting three rot is how a deferral becomes a cancelation. Spanish, Chinese, Arabic deferred, not canceled. D141, superseding D58.

## Map

`gh issue view 321` is what to do next; the six milestones are the order. `README-START-HERE.md` maps the folder. `HANDOFF.md` is current state. `docs/TRAPS.md` is what will bite you. `MASTER_SPEC.md` is features and phases. `contract/DATA-CONTRACT.md` is schema and export. `DESIGN.md` + `reference/screen-grid.html` is appearance. `TESTING-PERSONAS.md` is testing. `AGENTS.md` is delegation. `RUN-SAFETY.md` is unattended runs. `PROJECT-DELTAS.md` overrides `kamsiob-project-template.md`.
