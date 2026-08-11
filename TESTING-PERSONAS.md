# TESTING-PERSONAS.md, Health Trail

The universal testing standards in `kamsiob-project-template.md` section A8 apply in full: continuous testing, suites that actually compile, a gate per phase, regression sweeps, journeys over functions, the hostile path, and a regression test for every fixed bug. This document adds what is specific to Health Trail, and it exists because of one property of this app that most apps do not have.

**The app is used for years, and almost every defect that matters only appears with time.** A notebook with twelve entries and a notebook with two thousand entries across eight chapters and five ended care threads are different pieces of software. Nothing in a normal test suite finds what breaks between them.

So the protocol has two halves: a fixture generator that can produce any point in a five-year history on demand, and a set of personas walked end to end against those fixtures.

---

## 1. The fixture generator, built in Phase 0

Build a deterministic seeding tool in `tools/` before the personas exist, because none of them are testable without it and waiting five years is not a plan.

Requirements:

- **Deterministic.** A given seed always produces the same data, so a failure is reproducible and a screenshot is comparable across runs.
- **Parameterized by time.** It can generate a notebook as it would look on day one, day 30, month 6, year 1, year 2, and year 5.
- **Realistic in shape, not just volume.** Uneven entry frequency with real gaps, several chapters with transfers between them, parallel care threads that start and end, incidents that resolve and one that never does, bills in every state, standing instructions with recorded violations, projects at various stages, measurements with gaps, milestones, documents with attachments, and an Unfiled tray with items in it.
- **Includes the awkward cases on purpose**: unicode in every field, a note 8,000 characters long, a person with one name, an entry with only a rough date, a measurement with no unit, an attachment at the size limit, a chapter that lasted one day, a thread with a single session, and a bill for zero dollars.
- **Available in all four languages**, so the personas can be run with real strings rather than English text in a mirrored layout. **Version one is walked in English**, per D141, and right to left is walked against a forced layout direction. The other three catalogs stay complete and stay checked, because that is what keeps them from rotting while they wait.
- **Runs against an emulator, never the owner's device.**

Every persona below names the fixture it runs against. A persona run that cannot be reproduced from a seed is not a test, it is an anecdote.

---

## 2. Time-horizon personas

Each persona is a complete journey walked as a person, not a checklist of function calls. Each has a fixture, a set of things that must be true, and at least one thing that historically breaks at that horizon.

### P1. Day one, in a hallway

**Fixture:** empty. Fresh install.
**The situation:** a parent was just admitted after a fall. The person is standing in a corridor, holding the phone in one hand, and has about four minutes.

Must be true: the disclaimer appears and requires explicit acceptance. Setup asks for three things and lets everything else wait. The empty Today screen coaches rather than sitting blank, and its first suggestion is the Emergency Card. A first call can be logged in under thirty seconds from cold launch. Nothing anywhere asks for an account, an email, or a permission that is not needed yet.

Watch for: onboarding that cannot be completed one-handed. A keyboard covering the field being typed into. A required field that should be optional. Any screen that assumes the person already knows the facility's name.

### P2. Week one, building the notebook

**Fixture:** day 7, nursing home situation template applied.
**The situation:** the person is adding people as they meet them and working through the first-days checklist between visits.

Must be true: the situation template applied its roles, threads, checklist, and document slots, and the person can edit or delete any of it. Adding a contact offers the template's roles as suggestions without forcing them. The checklist can be partially completed and left. Capture chips are prefilled from the care team that now exists.

Watch for: template content that reads as advice rather than administration. A checklist that cannot be edited. Roles that cannot be renamed.

### P3. Month one, the routine

**Fixture:** day 30.
**The situation:** calls, a Sunday visit, one appointment, questions accumulating at 11pm.

Must be true: the digest reads correctly with a month of data. Ask next time holds questions and surfaces them on the right appointment's prep sheet. The prep sheet's change summary is composed from real entries and every line taps through to its source. The family update drafts from the week's entries, is fully editable, and shares through the system share sheet with nothing passing through any server.

Watch for: a digest that repeats the same line every day. Prep sheets that include questions already answered. A family update that reads as generated rather than written.

### P4. Month six, the first fight

**Fixture:** month 6, with one open incident, two resolved, a disputed bill, and a violated standing instruction.
**The situation:** a medication round was missed, the facility is slow to respond, and a bill arrived from a provider the family never chose.

Must be true: the incident thread records every call with names and dates and reads start to finish. The standing instruction shows its violation count and each violation links to its bill or incident. The disputed bill carries its state and its link to the instruction it broke. Each of these exports as its own document, and the exported document is legible to someone who has never seen the app.

Watch for: a pattern count that drifts into interpretation. The federal tag appearing where the backing does not apply. An export that assumes context the reader does not have.

### P5. Year one, the situation changes

**Fixture:** month 14, mid-transfer from rehab to home.
**The situation:** the person is coming home. The notebook has to change shape without losing anything.

Must be true: the chapter change carries every entry, incident, document, project, and measurement forward. The old facility's care team archives but stays searchable. A new situation template applies without overwriting anything the person customized. Nothing is deleted. Search across chapters returns results tagged with which chapter they belong to.

Watch for: archived contacts vanishing from old entries, which turns a two-year-old call note into a call with nobody. Template reapplication clobbering edits. Any orphaned row.

### P6. Year two, coming back after a gap

**Fixture:** year 2, with a four-month gap ending two weeks ago.
**The situation:** things were stable, the person stopped using the app, and now something happened.

Must be true: the digest says what changed since they were last here, with no guilt, no catch-up prompt, no streak, and no completion percentage about their own diligence. Older months are folded. Charts show the gap as a gap, unbridged and unannotated. The app is immediately usable without any reconciliation step. Nothing anywhere implies the lapse was a failure.

Watch for: any copy that scolds, even gently. An empty-state that reappears because recent activity is zero. A month review for a month with no entries that fabricates something to say. Interpolated chart lines across the gap.

### P7. Year five, the long record

**Fixture:** year 5. Roughly 1,200 to 2,000 entries, 8 chapters, 6 care threads of which 3 have ended, 4 projects, 40 documents with attachments, 3 measurement series with gaps, 15 milestones.
**The situation:** the person needs one specific phone call from three years ago.

Must be true, with performance targets that are pass or fail rather than impressions:

- Cold launch to interactive Today screen: under 1.5 seconds.
- Universal search first results: under 400ms.
- Trail scroll from now back to year one: no dropped frames, no loading placeholders left visible, no scroll position jumps.
- Year scrubber jump to any year: under 300ms.
- Assembled collection for an item with 30 related records: under 1 second.
- Export of the full notebook: completes, shows progress from the moment it starts, is cancellable, and survives being backgrounded halfway.
- Memory: no growth across 20 minutes of continuous navigation.

Watch for: a query that loads the whole trail into memory to show ten rows. Chapter or thread filters applied in application code rather than in the query. A PDF export that runs out of memory on a five-year notebook. Search that scans attachments it should not.

### P8. The end of a trail

**Fixture:** year 5, closing.
**The situation:** caregiving has ended, however it ended.

Must be true: closing a trail is possible without deleting it. The record stays readable and exportable afterward. The full export contains everything including tombstones. Deleting everything requires the two-tier confirmation, states plainly that it cannot be undone, and genuinely removes everything with nothing recoverable. There is no account anywhere to close. The copy on this screen is handled with more care than any other screen in the app.

Watch for: any cheerfulness. Any upsell. Any friction placed in front of deletion. An export that quietly omits archived chapters or ended threads.

### P9. The sibling who never installs anything

**Fixture:** month 6 and year 5, both.
**The situation:** someone receives a shared PDF and nothing else.

Must be true: every export type is legible standalone, with dates, names, and enough context to be understood by a reader with no knowledge of the app. Nothing in an export requires the app to interpret. Exports carry no branding beyond a quiet footer, no marketing, and no link asking the reader to install anything.

Watch for: an export that refers to internal concepts without explaining them. Text clipped at page boundaries. Attachments referenced but missing.

---

## 3. Language and layout personas

### P10. Spanish, formal register

**Fixture:** year 1, Spanish. Every string checked for formal usted, elders addressed as Señor or Señora, and no hospice cognate anywhere.

### P11. Chinese, no misleading terms

**Fixture:** year 1, Chinese. Verify the hospice, power of attorney, and social worker terms use the descriptive phrasing from the template data rather than direct cognates. Verify the bundled Noto face renders every screen with no fallback boxes and no mixed faces within a screen.

### P12. Arabic, right to left

**Fixture:** year 2, Arabic. Every screen mirrored. The trail runs from the end edge and the year scrubber moves to the start edge. Chevrons and back arrows mirrored. Mono metadata renders correctly rather than reversing. Numbers and dates follow locale conventions. Every screen screenshotted and compared against its English counterpart.

Watch for: a timeline still reading left to right, which is the single most likely RTL defect in this app.

### P13. The largest type, the lowest vision

**Fixture:** year 1, English, maximum system font size, with a screen reader on.

Must be true: nothing clips, nothing overlaps, no action becomes unreachable, and traversal order matches visual order on every screen. Every chart has a spoken label giving the measure, the date range, the latest value, and whether there is a gap. Every pill speaks its state as a word. The capture button is reachable and labeled.

---

## 4. The hostile pass

The universal hostile path applies in full. These are the additions specific to this app:

- **Process death during every long operation**: export, import, PDF generation, attachment capture, backup, and template application. Each must either complete or leave no partial state.
- **Import of a hostile file**: truncated, corrupt, a valid zip with a missing manifest, a manifest claiming a format version from the future, a database with an unknown table, an attachment whose hash does not match. Each must fail cleanly, name what was wrong, and change nothing.
- **Storage exhaustion mid-export and mid-attachment-capture.**
- **Permission denied** for the backup folder, for the camera, and for storage, each handled with a plain explanation and a path forward rather than a dead end.
- **The clock moved.** Device timezone changed, daylight saving boundary crossed, and the system clock set backward. Timestamps and the change log must remain coherent, and the digest must not claim things changed that did not.
- **Two devices, one person.** Export from device A, import to device B, edit on both, export and import again. Even before any direct sync exists, the ids, revisions, and tombstones must behave correctly through this path, because it is the manual version of sync and it exercises the same contract.
- **Deletion actually deletes.** After deleting an entry, verify it appears in no query, no search index, no digest, no export, and no chart, and that its attachment file is gone from storage.

All of the above runs on an emulator. None of it runs against the owner's installation.

---

## 5. Content compliance audit, automated

These are the app's promises, and a promise that is not tested is a promise that will eventually be broken. Write these as tests that run in continuous integration, not as a manual review:

1. No user-facing string anywhere contains an em dash.
2. No screen renders a target range, a normal range, a threshold, a color-coded value, an arrow, or any judgment on a measurement. Assert against the rendered chart and row components, not only the copy.
3. No chart interpolates across a gap.
4. Pattern and trend language appears only above the minimum-data threshold. Below it, verify the exact fallback string.
5. Deterministic engine output is byte-identical to the golden test vectors in `/contract/test-vectors/`, **in all four locales**, including the zero, one, and two entry cases and the plural boundaries. **This one stays at four deliberately**, per D141: the vectors are how the three waiting catalogs are held correct while they wait, and they cost nothing to keep passing.
6. Every standing instruction rendering shows a tag, and the federal tag's explanation is reachable.
7. Every template string in `templates/data/*.json` is present in the four locale catalogs, so a missing translation fails the build rather than silently falling back to English.
8. The round trip equality test from the data contract passes: export, wipe, import, assert field by field including tombstones, ordering, timestamps to the millisecond, relationships, and attachment hashes.

---

## 6. Who runs these

Per `AGENTS.md`, the test runner subagent executes the suites and persona scripts on the emulator and returns failures only, and the reviewer subagent runs the content compliance audit in section 5 and the cold read test at every phase gate. Both return reports. The main session decides what the reports mean, opens the issues, and does all the fixing. Anything touching the connected device stays with the main session.

If delegation fails or is unavailable, the main session runs them directly and logs it. A persona that was not walked is never recorded as walked.

## 7. The shortcut rule, which exists because the same defect shipped three times

**A test that reaches the code by a path the person cannot take proves something the person does not get.**

That is not a general caution. It is the shape of the three most expensive defects this project has shipped, all three of which had a full green suite standing over them at the time.

| What shipped broken | What the suite handed the code that a person never could | Why every test passed |
|---|---|---|
| **Chinese did not work at all.** A person who set the app to Chinese got English, silently | The `Locale`, passed straight into `Strings.load` | The catalogs were fine. The resolution path to them was not, and no test used it. Spanish and Arabic happened to work, so a general bug looked like a Chinese one |
| **No export could be opened anywhere but the phone that wrote it**, which meant the only recovery path from key loss did not exist | The device. Every round trip restored onto the machine that exported, where the Keystore key never changes | The bytes did survive the round trip, exactly as asserted. The file was still unreadable everywhere else |
| **The system back button left the app** from every screen above the notebook | The screen, composed alone in a bare test activity with no shell around it and no activity to leave | 130 of 137 interface tests use `createComposeRule`. Every `BackHandler` lives in `NotebookShell`, above them. They could not have caught it |

D39 says a defect found twice is a defect and three times is a missing specification. This is the specification.

### The question to ask of every test

**What does this test hand the code that the person never could?**

Look for a locale, a theme, a device id, a passphrase, a key, a timestamp, a file path, a database handle, a composed screen, or a row inserted by SQL. Then look for the second form, which is quieter: **setup that performs a step the app performs differently at runtime.** Seeding a notebook with `INSERT` is not the same act as filling in a form, and the difference is where the bugs are.

Then ask the sharper version: **is this test asserting on a value it also supplied?** A test that passes in Spanish and asserts it got Spanish has proven a lookup, not a language.

### What to do about it, in order

1. **Rewrite it to go through the person's path** wherever that is possible. It usually is, and it is usually cheap. `AppLanguageTest` sets the app's language through `LocaleManager`, the same API Android's own picker uses, and it is thirty lines. `BackJourneyTest` launches the real activity and presses the real back button, and it found a defect on its first run.
2. **Where it genuinely cannot**, say so in the test, in the test, not in a document. Name the shortcut and name what covers the difference. A pure function tested with injected timestamps is correct and honest; what is not honest is leaving a reader to discover that nothing checks where those timestamps come from.
3. **Where nothing covers the difference, that is a gap and it gets an issue**, whether or not anything is currently known to be broken in it. The three above were all invisible until someone held the phone.

### The standing shape of the interface suite

**Screen tests and journey tests are different tests and the project needs both.**

A screen test composes one screen and proves it renders every state, at every font scale, in every language. That is what `createComposeRule` is for and it stays.

**A journey test launches `MainActivity` and walks in from the front door**, through the gate, through setup, into the thing being tested, using only what a person can touch. It is the only kind that can see navigation, back, the shell, state that survives rotation, and anything that depends on how the person arrived. **Every screen owes at least one journey that reaches it**, and a screen reachable only by composing it directly is a screen no test has actually visited.

**The reader pass belongs on journeys too.** `ScreenReaderTest` composes each screen alone, so it proves labeling and cannot prove traversal order as a person meets it. That is why #44 is a hand pass and not a suite.

## 8. The gate

No phase is complete until its own tests pass, the regression sweep of all previous phases passes, and every persona touching that phase's features has been walked on a device or emulator with the result recorded.

No release happens until every persona in this document has been walked at its stated fixture, in both themes, in all four languages, at the largest font size, with a screen reader, on a fresh install and on an upgrade carrying existing data, and the cold read test from the universal standards has been run on the repository with its findings recorded.

Record persona runs in HANDOFF.md with the fixture seed used and the date, so a later session can tell which have genuinely been walked against the current build rather than assuming inherited coverage. A persona walked three phases ago against a schema that has since changed has not been walked.
