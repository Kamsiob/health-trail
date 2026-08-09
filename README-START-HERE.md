# Health Trail: project folder

Health Trail by Kamsiob, a local-first care notebook for the family member who is the point person for someone else's care.

---

## Picking up the work? Read three things and stop

1. **`CLAUDE.md`**, which is loaded automatically anyway. Twenty-four rules, one page.
2. **`HANDOFF.md`**, which is what is true right now. About two and a half thousand words.
3. **Issue #321**, pinned on GitHub, which is what to do next: `gh issue view 321`.

**That is enough to start**, and it is about five thousand words in total.

**Everything else is read on demand and never in bulk.** This folder holds more documentation than a session can hold in its head, and reading it front to back spends the context before any work starts. `HANDOFF.md` section 1 has the ladder that says what to open when.

**Two files have their own rule about how to read them.** **`docs/TRAPS.md` is read one section at a time**, chosen from the table at the top of it by what you are about to do, and never end to end. **`DECISIONS.md` is searched for a D number**, never read through.

**Do not read `docs/RUN-LOG.md` to orient.** It is history, it is long, and nothing in it is the current state.

The reading order further down is for **somebody meeting this project for the first time**, not for a session continuing the work.

---

## What is in here

```
MASTER_SPEC.md               what the app is, every feature, the phase plan, and the
                             definition of done. A living document from here on.

DESIGN.md                    the binding visual and voice specification. Both themes,
                             real dp and sp values, motion, RTL, accessibility, and the
                             final disclaimer wording.

contract/DATA-CONTRACT.md    binding on Phase 0. The schema requirements that make a
                             future direct device sync and a future web version
                             possible. Read before designing any table.

TESTING-PERSONAS.md          the testing protocol. A fixture generator plus thirteen
                             personas across day one to year five, four languages, and
                             the hostile pass.

PROJECT-DELTAS.md            where this project departs from or adds to the template,
                             including the corrected release and signing section.

AGENTS.md                    how work is divided between the main session and its
                             subagents. One writer, four read-only specialists, and the
                             rule that delegation never blocks progress.

RUN-SAFETY.md                how a long unattended run stays safe and recoverable.
                             Destructive commands blocked, context compaction handled,
                             retries capped, and what the repository must always contain.

CLAUDE.md                    the short list of rules that must survive compaction.
                             Loaded automatically every session.

kamsiob-project-template.md  the universal standards and the Android build prompt.
                             Applies in full except where PROJECT-DELTAS.md overrides.

templates/                   the finished template catalog. 57 templates as JSON the app
                             embeds, plus a readable catalog published separately under
                             CC BY-SA 4.0. Has its own README and SCHEMA.

HANDOFF.md                   the current state of the work and what to do next. Read
                             this every session. Rewritten to current truth, never
                             appended to.

DECISIONS.md                 every decision and why, D1 through D102, plus the BLOCKED
                             section for anything only the owner can resolve.

docs/TRAPS.md                what will bite you, grouped by what you are about to do.
                             Read one section, chosen from its own table. Never all of it.
docs/RUN-LOG.md              history. How things came to be and what proved them.
                             Never read this to orient yourself.

reference/screen-grid.html   the visual reference. 27 approved screens plus the sitemap.
                             Open it in a browser. This is what the app looks like.

reference/concept-review.pdf the same screens as a reading document, for sequence
                             and voice.

SESSION-HANDOFF-PROMPTS.txt  the before and after pair for handing a dying session to a
                             fresh one.
```

## Reading order, for a first encounter only

**Not for a session continuing the work.** See the top of this file.

1. `MASTER_SPEC.md`
2. `contract/DATA-CONTRACT.md`
3. `DESIGN.md` alongside `reference/screen-grid.html` open in a browser
4. `TESTING-PERSONAS.md`
5. `PROJECT-DELTAS.md`
6. `RUN-SAFETY.md`
7. `AGENTS.md`
8. `kamsiob-project-template.md`, Part A and Part C
9. `templates/README.md` and `templates/SCHEMA.md` before touching template content

`CLAUDE.md` is read automatically, so it needs no place in this order.

## Precedence when documents disagree

Built code verified on the device, then `HANDOFF.md`, then `DECISIONS.md`, then `contract/DATA-CONTRACT.md` for anything about the data model, then `DESIGN.md` for anything visual, then `RUN-SAFETY.md` and `AGENTS.md` for how-to-work questions, then `PROJECT-DELTAS.md`, then `MASTER_SPEC.md`, then `kamsiob-project-template.md`, then anything else.

The data contract sits high deliberately. Its requirements cannot be revised by a later session without an explicit decision from the owner, because changing the schema after real data exists means discarding someone's records.

## The shape of the build

Android first, in Kotlin. The repository is a monorepo from the first commit with `/android`, `/web`, `/contract`, `/templates`, and `/tools`. The web version is not being built now, but Phase 0 produces a scaffold in `/web` that opens the same schema, which is what stops the two platforms drifting apart later. Direct sync with a computer is a v1 constraint rather than a v1 feature: nothing in v1 talks to another device, but the schema is built so that it can.

## What is deliberately not in this project

No model, no inference, no AI. No medical or legal advice. No ranges, thresholds, or judgments on any measurement. No educational content. No accounts, no cloud, no telemetry, no ads, no subscriptions. No engagement mechanics of any kind.
