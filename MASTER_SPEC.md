# MASTER_SPEC.md, Health Trail by Kamsiob

## Precedence

This document, `DESIGN.md`, `DECISIONS.md`, and the open GitHub issues are the current source of truth. Anything in an older prompt or an earlier conversation that conflicts with them is superseded.

Within that, two documents sit above this one on their own subjects. `contract/DATA-CONTRACT.md` governs the data model, the export format, and anything touching sync or the web platform, and its requirements cannot be revised without an explicit decision from the owner, because changing them after real data exists means discarding someone's records. `DESIGN.md` governs every visual, motion, and copy decision.

`PROJECT-DELTAS.md` records where this project departs from `kamsiob-project-template.md`, which otherwise applies in full. `AGENTS.md` governs how work is divided between the main session and its subagents, `RUN-SAFETY.md` governs how a long unattended run stays safe and recoverable, and `CLAUDE.md` holds the short set of rules that must survive context compaction. None of those three subjects are covered by the template.

This is a living document. With every commit, correct anything here that the change made wrong, in the same commit. Superseded instructions are rewritten in place, never left beside their replacements. Anything pending is marked pending rather than described as built.

---

## 1. What this is

Health Trail is a local-first care notebook for the person who becomes the point person for someone else's care.

Not for the patient. Not for clinicians. For the son, daughter, spouse, or parent making the phone calls, sitting in the meetings, chasing the paperwork, and holding the only continuous account of what has happened.

The insight the whole app is built on: in practice the family caregiver is the information hub, because providers rarely talk to each other. Health Trail is that hub given structure. One trail of every call, visit, incident, medication change, document, and dollar, kept for years, searchable in seconds, and never leaving the phone.

**Positioning language, used consistently in the README, the store listing, and the website:** a private care notebook for family caregivers. Everything stays on your phone.

**Never positioned as:** a health app, a medical app, a patient portal, a family engagement platform, or a care coordination platform. Those are other categories with other obligations and other competitors.

### Honest limits, stated in the interface where they matter

- It keeps records. It does not remind, alert, or track medication doses.
- It gives no medical or legal advice, and no educational content.
- It counts things. It never interprets what the counts mean.
- It has no cloud and no account, so there is no shared live view for several family members. Sharing happens by exporting a document and sending it.
- On the web version, browsers can clear local data under storage pressure, which is why backup matters more there.

---

## 2. Non negotiables

All values in `kamsiob-project-template.md` section A2 apply. Specific to this app:

- **Zero data collection.** No analytics, no telemetry, no crash reporting to any service, no network calls at all in v1 except the user-initiated template pack mechanism if and when it ships.
- **No account, no login, no cloud, no server, ever.**
- **Free. No paywall, no subscription, no ads, no in-app purchase.** Support is a single donate link, labeled "Support this work," at the bottom of Settings and on the About screen, following the canonical support copy.
- **AGPLv3** for the code. Template content is CC BY-SA 4.0 and ships with its license and attribution.
- **No model, no inference, no AI.** Every digest, summary, and count is deterministic. Section 5 covers this.
- **Single point person.** One perspective, one owner of the notebook. The Emergency Card lists several emergency contacts, but the app is not built for co-decision-makers and no feature may creep toward shared editing.
- **No dark patterns.** No streaks, no badges, no engagement notifications, no nagging, and no friction in front of deleting your own data.
- **Not a medical app.** Never the Medical store category, no health claims in store copy, and the first-launch disclaimer is a gate. Complete any required store health declaration truthfully rather than working around it, verifying current policy at submission time.

---

## 3. Data model

`contract/DATA-CONTRACT.md` is binding and complete on the schema. Summarized here only so this document reads coherently:

Every row carries a locally generated unique id, created and updated timestamps, a revision, an origin device, and a tombstone column. Deletion is always a tombstone. Every write appends to a change log in the same transaction. Attachments are content addressed by hash. A sync engine transport interface exists with the export file as its only v1 implementation. The schema lives in `contract/schema.sql` and is not redefined in Kotlin.

### The three structural axes

Three axes, and every entry can carry all three. This is what makes the app more than a form collection, and it must be in the schema from the first migration rather than bolted on.

- **Chapters** answer *where*. A place and a period: home, a hospital stay, a rehab facility, a nursing home. Chapters hold their own entries, documents, and care team, and the care team archives with the chapter while staying searchable. Transfers between chapters are recorded.
- **Care threads** answer *what is ongoing*. Parallel streams that run at the same time: physical therapy, occupational therapy, speech, nursing, wound care. Each has its own color, its own history, and can end while the notebook continues.
- **The trail** answers *when*. One chronological record of every call, visit, incident, measurement, and change.

Every screen in the app is a lens on the same entries through some combination of these three.

### Everything connects

No dead ends. A medication knows its own incidents, its pending questions, its dose history, and its place on the Emergency Card. A person knows every call and visit involving them. A bill knows its chapter, the call where it was disputed, and the standing instruction it broke. An incident knows its project, its documents, and its people.

The person must never have to remember where something was filed, because every path leads to it.

---

## 4. Feature specification

`reference/screen-grid.html` is the layout specification for all of this, and `DESIGN.md` section 8 states what each screen must do that the image cannot show. This section defines behavior.

### 4.1 First run

Disclaimer gate, explicit accept, wording fixed in `DESIGN.md` section 7. Then essentials-first setup: who you are looking after, where they are, one phone number you would need in an emergency. Everything else is offered but skippable. A situation template is chosen, which configures sections, roles, threads, a first-days checklist, and document slots, all of it editable and deletable afterward.

### 4.2 Capture, the only way in

One gold button, present on every screen in all four tabs, opening a sheet with six choices: log a call, log a visit, report an incident, add a measurement, ask next time, save a document. Each files itself into the right section. The person decides what happened, never where it goes.

**Capture forgives, and this is a functional requirement rather than a nicety.** Every field is optional. Dates can be rough, including "sometime this week" and "not sure." A half-remembered note is a valid note. Anything the person cannot categorize goes to an Unfiled tray, where the app suggests a home by plain word matching and the person confirms with one tap. The app never files anything on its own.

Capture is additionally exposed as a home screen widget, a quick settings tile, and a share sheet target, so a photographed bill or an emailed PDF lands directly in capture.

### 4.3 Today

The digest headed "since you were last here," built from the change log. Open item counts for incidents, questions, and waiting-ons. The next appointment with its prep status. The Emergency Card one tap away. Universal search at the top.

Lapse tolerance is a requirement: returning after months shows what changed with no guilt, no catch-up prompt, and no reference to how long it has been in any judging sense.

### 4.4 Notebook

A table of contents with live counts and fixed positions. Sections: care team, medications, appointments, chapters, care threads, the trail, progress, documents, money, standing instructions, ask next time, emergency card. Which sit expanded versus folded comes from the situation template.

- **Care team.** People with role labels, the facility's own details separate from individuals, and every person's page assembling their whole history.
- **Medications.** Record only. Start dates, dose history, concern flags that stay attached forever, and a journey view across chapters. The screen states plainly that the app does not remind or alert.
- **Appointments.** Upcoming with prep sheets, past with their notes. A prep sheet carries the questions waiting for that person plus a change summary composed from real entries, every line tapping through to its source.
- **The trail.** The chronological record, with a year scrubber, months that fold as they age, thread filtering, and month review summaries.
- **Progress.** User-defined measures from the presets, with medication start markers, milestones as dated events, and gaps rendered as gaps.
- **Documents.** Photographed, categorized, each with a note on where the physical original lives.
- **Money.** Bills with states, totals, and links to the chapter, call, and instruction they relate to. Running cost sheets for long expenses.
- **Standing instructions.** What was asked, of whom, when, how it was acknowledged, every documented violation, and a tag saying whether federal nursing home rules back it or it is a request.
- **Ask next time.** The question inbox, surfacing on the right appointment.
- **Emergency card.** Allergies, blood type, current medications, emergency contacts, decision-maker, resuscitation status, and where every original document is kept. Designed to be handed to a paramedic.

### 4.5 Projects

Separate from the notebook, for long bureaucratic processes. Each has its own contacts, timeline, checklist, attachments, a waiting-on field, and a status. Sixteen project templates ship. Each exports as its own document.

### 4.6 Chapters and threads

Chapters as stops on a trail, each with dates, reason for the stay, incidents, documents, archived care team, and any project that began there. Situation change carries everything forward and archives what belongs to the old place. Threads with per-thread colors, filtering, and preserved history when ended.

### 4.7 Incidents

A thread from first report to resolution, with each call and escalation as a node, its own export, and a resolution time. Incidents over time shows the history with a deterministic count above it and an explicit line saying what it means is the person's to judge.

### 4.8 Search and assembly

Universal search from Today, grouped by kind. Scoped search inside every section, with a visible chip saying what is being searched and one tap to widen. Every result carries its chapter. From any result, one tap assembles everything connected to it into one view that exports as a single document.

Any single day can be reconstructed, including what the medications and measurements were at that time.

### 4.9 Sharing

Everything shareable is generated locally as a PDF or text and handed to the system share sheet. No account, no server, no link. Exportable: the family update draft, an incident thread, a project, a chapter, an assembled collection, a month review, the emergency card, and the full notebook.

Every export must be legible standalone to a reader who has never seen the app.

### 4.10 Templates

The catalog in `templates/` ships bundled and offline: 14 situation templates, 16 project templates, 16 progress presets, 11 standing instruction starters. `templates/SCHEMA.md` defines every field and, importantly, what must never be done with the ones flagged high risk.

All templates are editable, duplicable, and deletable. The person can build one from scratch. Custom templates save alongside the built-ins.

The downloadable pack mechanism, using signed hash-verified packs from GitHub releases, is not in v1. Design the loader with it in mind, because it is also the mechanism behind the future Facility Edition.

### 4.11 Backup and restore

Critical for this audience, and specified in the data contract. Automated local backup to a folder the person chooses, using durable permission and scheduled work, with no cloud. A rolling set of recent files. A quiet permanent indicator of the last successful backup that never nags. The offer made once, at the moment there is something worth losing, with a decline honored permanently. Restore as easy as backup, and tested onto a fresh install, an install with data, and a weaker device.

### 4.12 Transparency screens

A "Being considered" list with no dates and no promises, each item naming its real constraint. A "Not planned" list framed as decisions: cloud sync, accounts, shared live editing, medical advice, educational content, reminders and alerts, and engagement mechanics. Feedback via GitHub issues and hello@kamsiob.com only, with the expectation-setting line that one person builds this, everything gets read, and not everything gets a reply.

---

## 5. The deterministic engine

Every digest, month review, prep sheet, and pattern count is produced by a deterministic composition engine. No model, no inference, no interpretation.

**How it works:** the app queries real rows, does all arithmetic in code, and composes sentences from per-locale message templates. Every line traces to a specific entry the person wrote and taps through to it.

**Why it is built this way rather than with a model:** correctness is verifiable, output is testable against golden files, it runs instantly with no download and no battery cost, and it cannot invent a number. For an app whose entire value is being an accurate record, a summary that might be subtly wrong is worse than no summary.

**Rules the engine obeys:**

- All arithmetic in code, never in a template string.
- Sentences composed from message templates with proper plural and date handling per locale, never assembled by concatenating fragments, because concatenation breaks in every language except English. This is an architectural requirement, not a translation-time concern.
- Counts are allowed. Interpretation is not. "3 of the 5 resolved incidents involved the evening shift" is correct, followed by a line saying what it means is the person's to judge.
- Trend and pattern language only above a minimum-data threshold. Below it, the engine says what it has and stops.
- Gaps are stated as gaps and never interpolated.
- Output must be byte-identical across Kotlin and TypeScript for the same input, verified against the golden vectors in `contract/test-vectors/`.

An optional bring-your-own-key local AI layer remains a possible far-future addition under strict rules, retrieval never recall and no interpretation. **It is not in scope and must not be built.**

---

## 6. Platforms

**Android first, in Kotlin.** The only platform being built now.

**The repository is a monorepo from the first commit:** `/android`, `/web`, `/contract`, `/templates`, `/tools`. Creating this layout later means rewriting every import path and every workflow.

**The web version** is a progressive web app, installable, offline, with all data in the browser on the person's own device. It is not being built now. Phase 0 produces a working scaffold in `/web` that opens the same schema through SQLite in WebAssembly and reads the same template JSON. That scaffold has no features. Its only job is to prove the contract is real, which is what keeps the two platforms from drifting.

**Direct sync with a computer** is a v1 constraint, not a v1 feature. Nothing in v1 talks to another device. The schema requirements that make it later possible are in the data contract and are mandatory in Phase 0. When sync arrives: local network only, explicit out-of-band pairing, encrypted, manual, visible, no relay and no server ever.

**iOS** is deliberately out of scope. Do not add iOS-shaped abstractions in anticipation.

---

## 7. Languages

English, Spanish, Chinese, and Arabic, all four in v1.

Consequences that must be handled from Phase 0 rather than retrofitted:

- **Right to left layout** for Arabic, designed into every screen from the first screen. `DESIGN.md` section 4.4 covers the specifics, including that the trail itself mirrors.
- **Font coverage** for four scripts, with a bundled Noto fallback chain verified by rendering real strings on a device.
- **The message template architecture** described in section 5.
- **Three terms must never use their direct cognate**, because the cognate misleads or stigmatizes: hospice, power of attorney, and social worker. The descriptive phrasing is carried in the template data's `localization_note` fields.
- Spanish uses formal usted throughout, and elders are addressed as Señor or Señora.
- Every template string must exist in all four locale catalogs or the build fails, rather than silently falling back to English.

Translations should be reviewed by a native speaker who has dealt with the American care system. Where that review has not happened, say so in `DECISIONS.md` and in the store listing rather than implying a reviewed translation.

---

## 8. Phase plan

Each phase ends with: its testing gate passed, a regression sweep of all previous phases, the reviewer subagent run per `AGENTS.md` and its findings turned into issues by the main session, living documents updated, issues closed only where device-verified, a board status update posted, and a commit and push.

**Phase 0. Foundation, contract, and repository.**
Monorepo layout. The full repository standard from `kamsiob-project-template.md` section A4b, meaning every required file, continuous integration compiling every test source set, dependency updates, code scanning, branch protection on status checks, signed commits, the documented commit convention, issue and pull request templates, labels including good first issue and help wanted, the project board with a single-select status field and automation configured before it is populated, the first milestone, and the pinned roadmap issue including the deliberate exclusions. The schema from `contract/schema.sql` with every column the data contract requires, the change log, the conflict log, and a repository layer that makes it structurally difficult to query without filtering tombstones. The export container with manifest, version check, encryption, and a passing round trip equality test. The `/web` scaffold opening the same schema. The first golden test vectors. Design tokens for both themes implemented and contrast-verified with measured ratios recorded. The four-locale i18n scaffold with RTL working. The fixture generator from `TESTING-PERSONAS.md` section 1. A smoke test proving the app launches.

The three safety guards from `RUN-SAFETY.md` section 1, installed before any feature work: destructive commands blocked, state saved to HANDOFF.md before context compaction, and retries capped at three with escalation to BLOCKED. `CLAUDE.md` committed as written. The four subagent definitions from `AGENTS.md` section 4, with tools explicitly scoped and the field names verified against current Claude Code documentation rather than assumed, noting that they become usable from the next session onward.

Phase 0 is deliberately larger than usual. Every item in it is something that cannot be added later without discarding user data or reimplementing a platform.

**Phase 1. The notebook core.** Disclaimer gate, essentials-first setup, situation templates applied, Today with the digest engine, capture with all six inputs and the Unfiled tray, the trail, care team, medications, the emergency card, and the notebook table of contents. At the end of this phase a person could use the app daily. Every screen built to `DESIGN.md` including empty states, error states, and motion.

**Phase 2. Time and structure.** Chapters with transfers and archiving, care threads with filtering and ending, appointments with prep sheets, ask next time, the year scrubber and folding months, month review, the milestone arc.

**Phase 3. Incidents, instructions, and money.** Incident threads with export, incidents over time with deterministic counts, standing instructions with tags and violation tracking, bills with states and links, running cost sheets.

**Phase 4. Projects and documents.** Projects with checklists, waiting-on, contacts, and attachments. The documents section with categories and original-location notes. The full template library across all four types, with editing, duplication, and creation from scratch.

**Phase 5. Search and assembly.** Universal search, scoped search per section, assembled collections, any-day reconstruction, the family update draft, and the PDF export engine covering every export type.

**Phase 6. System integration.** Home screen widget, quick settings tile, share sheet target, each with the minimum permissions and each audited against the merged manifest.

**Phase 7. Portability.** Automated local backup with durable folder permission and scheduling, restore flows, situation change, closing a trail, and the full data wipe with two-tier confirmation.

**Phase 8. Hardening and release.** Every persona in `TESTING-PERSONAS.md` walked at its fixture. Accessibility verification. All four languages including RTL screenshot comparison. Performance targets at five-year scale met as pass or fail. The content compliance audit running in continuous integration. The cold read test on the repository with findings recorded. Real screenshots captured from the running app in both themes. Store assets generated from the design system. Version chosen with reasoning stated. Upload key generated and stored outside the repository. The bundle uploaded, the Google-signed universal APK downloaded and published as the GitHub release asset per the corrected release process in `PROJECT-DELTAS.md`. `LAUNCH.md` listing the owner's exact remaining clicks and nothing else.

---

## 9. Definition of done, for every change

1. It works on the device or the emulator, verified rather than assumed.
2. Its tests exist, compile, and pass, and every fixed bug has a regression test.
3. It survives the export, wipe, import round trip with field-by-field equality. Anything the app can store, the export contains and the import restores.
4. It matches `DESIGN.md` in both themes, at the largest font size, with a screen reader, and in all four languages including RTL.
5. It contains no medical or legal advice, no interpretation, no ranges, no thresholds, no color-coded values, no em dashes.
6. Its issue is closed with acceptance criteria met and verified, and its commit references the issue number.
7. The living documents describe the app as it now is, corrected in the same commit.
8. The README's capability and limitation lists are still true.
9. Its claim to be complete was verified against the working tree with `git status` and `git diff HEAD`, never against recollection.

---

## 10. Open questions

Marked open rather than left implicit. Decide during the build, log the decision in `DECISIONS.md`, and correct this section.

1. **Attachment size and count limits.** A five-year notebook with photographs of every bill could grow large. Decide a per-attachment cap and a plain-language warning, and state the limit before the person meets it rather than after.
2. **Whether the change log is exported.** The round trip needs the data; a peer sync needs the log. Decide whether the log travels in the container or is rebuilt on import, and record which.
3. **Tombstone retention window.** The contract requires the window to be written into the schema comments now. Choose the number.
4. **PDF pagination for very large exports.** A five-year full-notebook export needs a sane structure with a table of contents rather than a hundred unbroken pages.
5. **Whether the web scaffold uses the same UI toolkit** or a deliberately minimal one, given it only needs to prove the contract in Phase 0.
6. **Native-speaker translation review** has not happened. Decide how the app describes its own translation status honestly until it does.
