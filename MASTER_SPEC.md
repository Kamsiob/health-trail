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
- **Free. No paywall, no subscription, no ads, no in-app purchase.** Support is a single donate link, labeled "Support this work," following the canonical support copy. It appears in three places: **the disclaimer gate**, the bottom of Settings, and the About screen. One destination and one label, in more than one place. D59.
- **The interface says the app is free, and says it where the person can see it.** The disclaimer gate's third point states plainly that there are no ads, no subscription, nothing to unlock, and no tracking. This was true from the beginning and was written only here, where nobody using the app would ever read it.
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

`reference/screen-grid.html` is the layout specification for all of this. It holds the twenty-five screens of **design direction v4, adopted 2026-08-03**, which supersedes the direction this specification was originally written against. `DESIGN.md` is binding on how every screen looks and behaves, and section 13 of it is the method for the screens the grid does not draw. This section defines behavior only.

### 4.1 First run

Disclaimer gate, explicit accept, wording fixed in `DESIGN.md` section 11.1. Then essentials-first setup: who you are looking after, where they are, one phone number you would need in an emergency. Everything else is offered but skippable. A situation template is chosen, which configures sections, roles, threads, a first-days checklist, and document slots, all of it editable and deletable afterward.

### 4.2 Capture, the only way in

One gold button, present on every screen in all four tabs, opening a sheet with six choices: log a call, log a visit, report an incident, add a measurement, ask next time, save a document. Each files itself into the right section. The person decides what happened, never where it goes.

**Capture forgives, and this is a functional requirement rather than a nicety.** Every field is optional. A half-remembered note is a valid note. Anything the person cannot categorize goes to an Unfiled tray, where the app suggests a home by plain word matching and the person confirms with one tap. The app never files anything on its own.

**Dates are a real capability, not a rough option.** An earlier version of this section described them in one sentence and understated what is required. A care record spans years and is written from memory: "the fall was sometime in November 2024," "she was moved in the fall," "I called them, I think it was a Tuesday." A schema that stores only a precise timestamp turns every one of those into a claim the person never made.

Dates are stored as EDTF, the Extended Date/Time Format standardized as ISO 8601-2:2019, where precision is expressed by truncation and uncertainty is a separate axis from precision. **Unknown is a first-class value**, and an entry with an unknown date saves, is valid, and appears in the trail. **Whatever the person expresses is recorded at exactly that precision and no finer, and displayed at exactly that precision everywhere**, including the trail, month reviews, exports, PDFs, and the engine's composed sentences. Every date is editable forever from the entry itself, and editing one never creates a new entry and never loses its links. Imprecise entries sort among precise ones and appear in any date-range search their range overlaps.

The interface hides all of it: chips for the common cases, an exact date and time always available as a peer of the chips rather than behind them, and natural expression where it is easier. The person never sees EDTF and never chooses a precision.

The full model, its columns, and its round-trip requirement are in `contract/DATA-CONTRACT.md`. The display rules are `DESIGN.md` section 9.2. The owner approved this on 2026-07-31 and it is made before real data exists, because retrofitting it later means discarding records. D34.

Capture is additionally exposed as a home screen widget, a quick settings tile, and a share sheet target, so a photographed bill or an emailed PDF lands directly in capture.

### 4.3 Today

**Rewritten 2026-08-04 with the adoption of `reference/today-grid.html`**, D106. The fixed dashboard described here before is superseded. `DESIGN.md` section 21 is the full specification.

**Today is a desk the person sets the way they keep it.** One **lead slot** at the top, singular by construction, and below it a **field of cards the person adds, moves, resizes, and removes**. What fills the lead is their choice; by default it is the digest.

**A card is one deterministic question asked of the record**, answered on open and after any save, and it is a door to where the answer lives. Roughly sixteen card types ship, one per real recurring question rather than one per table. **No card interprets, advises, scores, or colors by value.**

**The situation template ships a complete starting layout**, so nobody meets a blank canvas, and every default is editable from the first minute.

**Today never rearranges itself.** Only the person's hand and the situation template at onboarding touch the layout. That is the trust model of the surface and it is absolute.

**The layout is record**, not a preference: an ordered list of card instances with the lead assignment, archived and restored like everything else the person has made. `contract/DATA-CONTRACT.md` 8.7.

Lapse tolerance is a requirement: returning after months shows what changed with no guilt, no catch-up prompt, and no reference to how long it has been in any judging sense. **No card ever measures the person's absence back at them.**

The digest itself, the universal search bar, and the gold capture button keep their places regardless of layout.

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

**Rewritten 2026-08-04 with the adoption of `reference/projects-grid.html`**, D106. The checklist-led project described here before is superseded. `DESIGN.md` section 20 is the full specification.

Separate from the notebook, for long bureaucratic processes. Each has its own contacts, timeline, attachments, papers, and a status. Sixteen project templates ship. Each exports as its own document.

**Every project answers three things, and the shape of a project is which one it leads with**: where it stands (whose hands, since when), the next date (a recorded fact carrying its source), and the latest word (what was last said, by whom, with the reference number).

**The checklist is not the project.** In a long process most of what happens is not a task anybody can check off, and a wall of unchecked boxes makes waiting feel like the person's failure. **Steps remain, and lead only in the busy-stretch shape**, where the work genuinely is many small arrangements.

**A project template is five defaults and nothing more**: stages, lead, starting steps, usual papers, date kinds. **Applied by copy at creation with no live link**, so editing a project never touches the template and updating a template never touches existing projects. All five are visible and changeable from one screen forever after.

**Nothing on this surface frames the process as a fight**, per `DESIGN.md` section 22. People are named by role, and urgency is a number and its source rather than a warning.

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

**The full notebook export is THE ARCHIVE**, `contract/DATA-CONTRACT.md` section 8, and nothing weaker. It is a ZIP64 archive carrying the SQLite payload, its schema, a manifest, checksums, the original attachment bytes, and **a complete human-readable HTML copy that renders every field and needs no software to read.** The single-item exports above are the PDF and text paths and are separate from it. **A backup and a full export are the same artifact.**

### 4.10 Templates, and the template library

The catalog in `templates/` ships bundled and offline: 14 situation templates, 16 project templates, 16 progress presets, 11 standing instruction starters. `templates/SCHEMA.md` defines every field and, importantly, what must never be done with the ones flagged high risk.

The downloadable pack mechanism, using signed hash-verified packs from GitHub releases, is not in v1. Design the loader with it in mind, because it is also the mechanism behind the future Facility Edition.

**None of these screens are in `reference/screen-grid.html`.** The library, the pickers, and the editor have no mockup. They are built under the protocol in `DESIGN.md` section 13: composed from the inventory in section 7, shipped complete with every state, and logged in three places at the moment they are built. `DESIGN.md` section 14 names which drawn screen each of them follows.

#### Where it lives, and what it has to feel like

The library lives in More. **That placement is settled. Its quality is not.** It is the person's own library and it has to read as one polished, organized thing rather than a settings page with lists on it.

**One presentation, four kinds.** Situations, projects, progress presets, and standing instructions use the same screen structure, the same detail layout, and the same actions in the same positions. The content differs because the content is different. The experience does not. A person who has opened one template has learned how to open all of them, and must never feel they have walked into something built by a different person on a different day.

#### What every template detail view carries, in this order

1. The name and subtitle.
2. **A plain statement of what applying it will actually create or change,** stated before it is applied rather than discovered afterward.
3. The posture strings, displayed verbatim per `templates/SCHEMA.md`. These are not paraphrased in the interface.
4. **Provenance.** The person can always tell whether they are looking at something that shipped with the app, something they made, or a shipped one they have edited.
5. The same set of actions, in the same positions, every time.

#### Browsing, previewing, and applying are three different things

They must be visually distinguishable, and **nothing is ever applied as a side effect of looking at it.**

#### The library shows state, not just choices

It is a record of what the person has done, not only a menu of what they could do. Which templates are in use, when they were applied, and what each one created must be visible from the library itself. **An applied template can always answer the question "what did this put in my notebook,"** with links to each thing.

#### Editing a built-in creates the person's own copy

Editing never mutates the shipped template. The lineage is preserved, so the original stays available and a future catalog update cannot silently overwrite the person's version.

### 4.10b Two rules that hold everywhere in this app

Stated here because they come up first with templates, but they are not about templates.

**Nothing gets lost.** The person can never enter information and then be unable to find it. Everything a template creates is reachable **from at least two directions**: from the notebook section it belongs to, and from the library entry that created it. Anything created is written to the trail, which is what makes the trail the universal index of everything that has ever happened in this notebook. Search reaches both template-created content and the templates themselves. The no dead ends rule in section 3 applies here in full: from any item, every road out of it is available, and the person never has to remember where something was filed.

**Partial is a finished state, not an incomplete one.** A template can be applied with nothing filled in, with some of it filled in, or completed months later. Never require completion to apply, never require completion to save, never block on a missing field. An unfilled slot reads as "not yet," never as an error, a warning, or a gap that needs fixing.

No progress meters that frame an unfinished checklist as a deficiency. No completion percentages. No prompts to finish setting up. No badge counting what has not been done. The person is doing this in a hallway during the worst month of their life, and the app's posture toward partial work is that **partial work is normal work**.

The same holds for changing things later. Anything applied can be renamed, reordered, edited, or removed at any time. Changing the situation template later, including at a change of situation, never destroys entries made under the previous one: it carries them forward and archives what belongs to the old place, per the chapter behavior in section 4.6.

### 4.11 Backup and restore

Critical for this audience, and specified in the data contract, which was amended on 2026-08-03 to carry THE ARCHIVE. **The scheduled backup writes exactly the archive format in `contract/DATA-CONTRACT.md` section 8**, so there is no second, weaker format for the file the person will actually reach for when the phone is gone. Automated local backup to a folder the person chooses, using durable permission and scheduled work, with no cloud. A rolling set of recent files. A quiet permanent indicator of the last successful backup that never nags. The offer made once, at the moment there is something worth losing, with a decline honored permanently. Restore as easy as backup, and tested onto a fresh install, an install with data, and a weaker device.

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

- **Right to left layout** for Arabic, designed into every screen from the first screen. `DESIGN.md` section 5.4 covers the specifics, including that the trail itself mirrors and that the FAB moves to the start corner.
- **Font coverage** for four scripts, with a bundled Noto fallback chain verified by rendering real strings on a device.
- **The message template architecture** described in section 5.
- **Three terms must never use their direct cognate**, because the cognate misleads or stigmatizes: hospice, power of attorney, and social worker. The descriptive phrasing is carried in the template data's `localization_note` fields.
- Spanish uses formal usted throughout, and elders are addressed as Señor or Señora.
- Every template string must exist in all four locale catalogs or the build fails, rather than silently falling back to English.

### 7.0 What makes a translated language shippable

**Decided by the owner on 2026-08-01, D58. This governs, and it replaces the rule that an unreviewed language is not shippable.**

**The rule applies to translation, not to language.** English is authored rather than translated, so it is reviewed by definition and never carries a disclaimer. The rest of this section is about the translated catalogs.

**A translated language ships**, provided both of these hold:

1. **The language selection screen carries a friendly disclaimer** that translations may not be one hundred percent accurate. It reads from each catalog's `reviewed_by_native_speaker` flag rather than from a hard coded list, so a language that is reviewed stops disclaiming without anyone editing a screen.
2. **The translation has actually been checked** as a good faith effort before release.

**Friendly is a requirement rather than a tone note.** Not a warning, not a legal notice, not an apology. The app being straight with someone, in the same register as the rest of it. It never reads as a judgment on the person's language, and it never frames English as the real version with every other language a lesser copy.

**Native speaker review remains the better outcome and the path stays open.** A native speaker who has dealt with the American care system reviewing a language turns its flag true and removes its disclaimer. Nothing here closes that door; it stops being a gate in front of shipping.

**Why the bar was high to begin with, and why the disclaimer carries that weight now.** A machine translated explanation of what federal nursing home rules do and do not guarantee is the app claiming more than it knows, which is the one thing this app is built not to do. The obligation did not disappear when the gate did. It moved into the interface, where the person sees it before they rely on a word of it.

### 7.1 This is language access in the United States, not international expansion

**The distinction is not pedantic and it decides what is correct.** This app is about Medicare, Medicaid, federal nursing home regulation, state ombudsman programs, and American billing. That content is specific to one country.

**Translating it for a Spanish speaker in Texas is exactly right.** They are navigating the American care system, in the United States, and the only barrier is the language it is written in. **Presenting the same app to someone in Spain would be wrong**, because every substantive thing it says about rights, coverage, and who to escalate to would be false there.

So the languages are chosen by **limited English proficiency population within the United States**, not by global speaker counts, not by app store market size, and not by which translations are cheapest to obtain. A language earns its place here because caregivers in this country speak it and are underserved reading English.

**What follows from that:**

- **No locale variants for other countries.** `es` rather than `es-MX` and `es-ES`, because the audience is Spanish speakers in the United States and splitting them serves nobody.
- **Nothing in the interface implies the app works elsewhere.** No country picker, no currency selection, no claim of international support.
- **The care system vocabulary stays American** even in translation. A Spanish rendering of "skilled nursing facility" describes the American thing, not the nearest equivalent in another country's system.
- **Ranking is by need, not by ease.** Vietnamese, Chinese, and Korean have the highest share of speakers with limited English proficiency, around 57, 52, and 51 percent, which matters more than raw population when deciding who is actually shut out.

### 7.2 The ten, and why

By limited English proficiency population in the United States. Approximate, and the ordering rather than the exact figures is what matters.

| Language | LEP speakers | Status |
|---|---|---|
| Spanish | ~16M | Ships |
| Chinese, Mandarin and Cantonese | ~1.8M | Ships, as Simplified |
| Vietnamese | ~960k | Planned |
| Korean | ~530k | Planned |
| Tagalog | ~530k | Planned |
| Russian | ~430k | Planned |
| Arabic | ~410k | Ships |
| Haitian Creole | ~280k | Planned |
| Portuguese | ~277k | Planned |
| French, including Cajun | ~252k | Planned |

**Haitian Creole is a distinct language and not a French dialect.** It has its own grammar and its own orthography, and a Haitian Creole speaker is not served by French. **Never treat either as a fallback for the other.** Getting this wrong would fail the people it most claims to serve, and it is an easy mistake for a system that groups by language family.

**Chinese ships as Simplified.** Traditional is a separate question, not an alias, and no build should silently serve one for the other.

**Cost, so the scale is not a surprise.** Roughly 1500 strings per language across the interface catalog and the 57 templates, plus plural rules, date and number formatting, and device verification. **Seven new languages is on the order of ten thousand strings**, each of which is care instructions, money, or somebody's rights.

**None of it starts until everything ahead of it is done.** What makes a translated language shippable is section 7.0, and it is the disclaimer plus a checked translation rather than native speaker review. D58.


---

## 8. Phase plan

Each phase ends with: its testing gate passed, a regression sweep of all previous phases, the reviewer subagent run per `AGENTS.md` and its findings turned into issues by the main session, living documents updated, issues closed only where device-verified, a board status update posted, and a commit and push.

**Phase 0. Foundation, contract, and repository.**
Monorepo layout. The full repository standard from `kamsiob-project-template.md` section A4b, meaning every required file, continuous integration compiling every test source set, dependency updates, code scanning, branch protection on status checks, signed commits, the documented commit convention, issue and pull request templates, labels including good first issue and help wanted, the project board with a single-select status field and automation configured before it is populated, the first milestone, and the pinned roadmap issue including the deliberate exclusions. The schema from `contract/schema.sql` with every column the data contract requires, the change log, the conflict log, and a repository layer that makes it structurally difficult to query without filtering tombstones. The export container with manifest, version check, encryption, and a passing round trip equality test. The `/web` scaffold opening the same schema. The first golden test vectors. Design tokens for both themes implemented and contrast-verified with measured ratios recorded. The four-locale i18n scaffold with RTL working. The fixture generator from `TESTING-PERSONAS.md` section 1. A smoke test proving the app launches.

The three safety guards from `RUN-SAFETY.md` section 1, installed before any feature work: destructive commands blocked, state saved to HANDOFF.md before context compaction, and retries capped at three with escalation to BLOCKED. `CLAUDE.md` committed as written. The four subagent definitions from `AGENTS.md` section 4, with tools explicitly scoped and the field names verified against current Claude Code documentation rather than assumed, noting that they become usable from the next session onward.

Phase 0 is deliberately larger than usual. Every item in it is something that cannot be added later without discarding user data or reimplementing a platform.

**Phase 1. The notebook core.** Disclaimer gate, essentials-first setup, situation templates applied, Today built to `reference/today-grid.html` with the digest engine behind its lead card, capture with all six inputs and the Unfiled tray, the trail, care team, medications, the emergency card, and the notebook table of contents. At the end of this phase a person could use the app daily. Every screen built to `DESIGN.md` including empty states, error states, and motion. **Phase 1 is reopened by the v4 adoption:** every screen it delivered is converted to the new direction before the phase is called done again.

**Phase 2. Time and structure.** Chapters with transfers and archiving, care threads with filtering and ending, appointments with prep sheets, ask next time, the year scrubber and folding months, month review, the milestone arc.

**Phase 3. Incidents, instructions, and money.** Incident threads with export, incidents over time with deterministic counts, standing instructions with tags and violation tracking, bills with states and links, running cost sheets.

**Phase 4. Projects and documents.** Projects built to `reference/projects-grid.html`: the three answers, the three shapes, the road strip, and the five template defaults. Waiting-on, contacts, papers, and attachments. The documents section with categories and original-location notes. The full template library across all four types, with editing, duplication, and creation from scratch.

**Phase 5. Search and assembly.** Universal search, scoped search per section, assembled collections, any-day reconstruction, the family update draft, and the PDF export engine covering every export type.

**Phase 6. System integration.** Home screen widget, quick settings tile, share sheet target, each with the minimum permissions and each audited against the merged manifest.

**Phase 7. Portability.** Automated local backup with durable folder permission and scheduling, restore flows, situation change, closing a trail, and the full data wipe with two-tier confirmation.

**Phase 8. Hardening and release.** Every persona in `TESTING-PERSONAS.md` walked at its fixture. Accessibility verification. All four languages including RTL screenshot comparison. Performance targets at five-year scale met as pass or fail. The content compliance audit running in continuous integration. The cold read test on the repository with findings recorded. Real screenshots captured from the running app in both themes. Store assets generated from the design system. Version chosen with reasoning stated. Upload key generated and stored outside the repository. The bundle uploaded, the Google-signed universal APK downloaded and published as the GitHub release asset per the corrected release process in `PROJECT-DELTAS.md`. `LAUNCH.md` listing the owner's exact remaining clicks and nothing else.

---

## 9. Definition of done, for every change

1. It works on the device or the emulator, verified rather than assumed.
2. Its tests exist, compile, and pass, and every fixed bug has a regression test.
3. It survives the export, wipe, import round trip with field-by-field equality, **and the regeneration test in `contract/DATA-CONTRACT.md` section 8.5**: the readable copy regenerated from the reimported database is byte-identical to the one in the original archive. Anything the app can store, the archive contains, the readable copy renders, and the import restores.
4. It matches `DESIGN.md` in both themes, at the largest font size, with a screen reader, and in all four languages including RTL, **and it passes the costume audit and the overflow audit in `DESIGN.md` section 16, on the phone.**
5. It contains no medical or legal advice, no interpretation, no ranges, no thresholds, no color-coded values, no em dashes.
6. Its issue is closed with acceptance criteria met and verified, and its commit references the issue number.
7. The living documents describe the app as it now is, corrected in the same commit.
8. The README's capability and limitation lists are still true.
9. Its claim to be complete was verified against the working tree with `git status` and `git diff HEAD`, never against recollection.

---

## 10. Open questions

Marked open rather than left implicit. Decide during the build, log the decision in `DECISIONS.md`, and correct this section.

**Decided:**

1. **Attachment size and count limits.** Decided in `DECISIONS.md` D13. 25 MB per attachment. No hard total cap, with the running total shown and mentioned plainly once it passes 4 GB. Automatic downscaling of photographs was rejected, because a photographed bill is often evidence in a dispute and silently reducing its resolution could destroy exactly the small print that mattered.
2. **Whether the change log is exported.** Decided in `DECISIONS.md` D12. It travels in the container, and the importer renumbers `seq` locally while preserving everything else, because `seq` is meaningful only on the device that wrote it.
3. **Tombstone retention window.** Decided in `DECISIONS.md` D11. 730 days, written into the header comment of `contract/schema.sql`. Chosen generously because the cost of the window being too long is a few bytes and the cost of it being too short is permanently resurrecting records the person deleted.

**Still open:**

4. **PDF pagination for very large exports.** A five-year full-notebook export needs a sane structure with a table of contents rather than a hundred unbroken pages. Not forced until Phase 5.
5. **Whether the web scaffold uses the same UI toolkit** or a deliberately minimal one, given it only needs to prove the contract in Phase 0. Forced by issue #16. Note that `npm` is absent from the build environment, which bears on the answer.
6. **Native-speaker translation review** has not happened. Decide how the app describes its own translation status honestly until it does. Forced by issue #13, and it affects the store listing and the README as well as the app.
