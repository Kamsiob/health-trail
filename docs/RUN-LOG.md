# RUN-LOG.md, Health Trail by Kamsiob

**This is history, and nothing here is the current state of the work.** `HANDOFF.md` is.

**Do not read this file to orient yourself.** It exists so that the account of how something came to be is not lost, and so a session wondering "was this ever built, and how was it checked" has somewhere to look. It is appended to and it is never the thing that tells you what to do next.

**It was split out of `HANDOFF.md` on 2026-08-04**, because that file had reached sixteen thousand words and `CLAUDE.md` rule 1 requires it be read at the start of every session. A file that costs most of a context window to read is one that gets skimmed, and rule 1 is worth more than the narrative.

Three things are worth reading here rather than searching git for, and each has its own section below:

- **Section 4, what was verified and how.** The table answers "was this ever actually checked, or does it just look right", for every piece of the foundation.
- **Section 10, the persona runs.** What each walk found, including what failed.
- **Section 5, the superseded work inventory.** Useful only when wondering whether something was ever built before the v4 direction replaced it.

---

## 0a. Where the project actually stood before this run touched anything

Written honestly, per the instruction, including what was found half-done.

**The tree was clean and `main` was current.** `git status --porcelain` returned nothing, `HEAD` was `527ff06`, and nothing was unpushed. The previous session, which was updating the knowledge documents and the issue board, **had finished rather than stopped mid-way.** That was checked rather than assumed.

**What existed and worked.** The core loop ran end to end on real hardware. 50 screen files, 20 component files, a seeded year five fixture on the phone, 277 instrumented tests, 102 unit tests, lint, and eleven content checks, all green as of the previous session's last commit.

**What was half-done and is now superseded rather than finished.** The previous run had built six of the nine components in the old section 11 library and left three written down but unbuilt: the stat display, the avatar, and the segmented control. **That work is not resumed as it stands.** It was folded into the v4 conversion issues, per the instruction that work in flight on the old direction is not resumed. The avatar survives into the v4 inventory as #156; the segmented control became the view toggle #160; the stat display folded into the token and type pass.

**The one thing that was open and stays open.** `#128` / `B5`: this project's destructive command guard has never been observed to fire through Claude Code, on any day, in any session. **Only the owner can install the fix**, because Claude Code correctly refuses to let a session edit the hooks that constrain it. **It does not block the work.** What has actually protected this repository through four long unattended runs is rule 6 followed by hand, plus Claude Code's own classifier. Do not spend time re-probing it; the answer is known until B5 is done.

**Nothing destructive was run in this run.** The only deletion was the v3 grid, which was the owner's explicit instruction, and its blob survives in git at `fbeb6cf`.

---

## 1. What step zero did

One documentation-only commit, before any application code, exactly as instructed.

- **`reference/screen-grid.html` is now v4.** The v3 file is gone from the working tree, one copy only, no archived variants. `reference/concept-review.pdf` is kept and `DESIGN.md` now says at the top that it is a historical record and not the visual reference.
- **`DESIGN.md` was rewritten**, not patched. Identity, the five laws, the tokens including the tab pack and `stone`, the component inventory, the FAB correction, the interaction grammar, the method for an undrawn screen, the polish list, and the two audits. Paragraphs describing replaced patterns were **deleted rather than kept as history**, and section 18 records exactly what survived and what did not, so nobody mistakes a deliberate deletion for an omission.
- **`contract/DATA-CONTRACT.md` gained THE ARCHIVE in full**, marked as an owner-approved amendment. `contract/export-format.md` was renamed to `contract/EXPORT-FORMAT.md` and given its new job.
- **`MASTER_SPEC.md` was corrected** wherever it described the old design or an export weaker than THE ARCHIVE.
- **`DECISIONS.md` gained D76 through D85**, one per decision this direction forced. **It runs to D98 now**, the later ones from the nights of building rather than from step zero.
- **The board was reconciled.** 37 issues closed as superseded with the reason named, 3 rewritten in place, 67 opened, all on the board in `ORDER OF WORK` order.

---

## 2. Two things the owner decided during this run, both of which change what gets built

### 2a. D67 stands. Every export is encrypted, and the archive is two layers

**THE ARCHIVE as first supplied required that an unencrypted export be offered. That directly contradicted D67**, which had removed the unencrypted export because the payload became a plain SQLite database, meaning a plain container would be a fully readable copy of an entire care record sitting in a folder a file manager can browse and a cloud sync can copy.

**The contradiction was flagged rather than silently resolved in either direction, and the owner corrected it the same day.** D67 stands. There is no unencrypted export path, no chip offering one, and no settings toggle producing one.

**What replaces it is harder and better.** An encrypted archive must remain **openable by someone who has the passphrase but does not have this app**, because a format only this app can decrypt is the same failure as a format only this app can read, arriving one step later. So the container is two layers: a plain outer ZIP64 holding only a stranger-readable `README.txt`, a non-sensitive `MANIFEST.json`, and `payload.enc`. **Nothing in the outer layer reveals anything about the person.**

**All three of these are now built**, on 2026-08-04, and each is described in section 0 with what verified it. They are kept here as the requirements rather than as pending work. Three requirements make that real rather than aspirational, and each was a build gate: the format is published **byte for byte** in `contract/EXPORT-FORMAT.md` under AGPL so it survives this project, #214. A standalone decryptor ships at `tools/decrypt/` with no build step and a README somebody who does not write software can follow, tested in continuous integration against a real archive, #215. And the passphrase gets every chance to survive: confirmed twice, an optional hint stored in the outer manifest **in plaintext with the app saying so plainly**, and a backup that reuses a passphrase set once. **D84 carries the full account.**

**The lesson worth keeping.** A quiet reversal would have removed a real safety property and nobody would have noticed for months. That is the same shape as every silent negative this project has been caught by.

### 2b. The tab hues all failed the contrast floor, and the floor won

**Measured on adoption, before a single screen was built against the palette.** All six tab hues, used as small text, measured between 3.23:1 and 4.56:1 against every surface they land on. **Every one is under the 4.5:1 floor.**

This is not theoretical: the tab chip is drawn at roughly 11sp and is the **first element on every section screen**.

**The owner's section-to-hue mapping is untouched and was not re-derived.** Each hue keeps its hue angle and its saturation; only its lightness moves, so the binder tabs still read exactly as specified at arm's length. The base is for shapes, and a new **ink** variant carries text. That is not a new idea here, it is the split this codebase already used for gold, leaf, and alert, applied to six more hues. **D80** has the measured table.

Two base tokens moved for the same reason: `ink-2` to `#576873` and `blue` to `#2E6D8C`, both of which were fractionally under the floor on `sand`.

---

## 3. What is deliberately not claimed

**The dark theme is built and looked at, and this section said otherwise until 2026-08-04.** `DarkColors` carries the full ladder in `Color.kt`, starting at `#141C23`, and **101 dark screenshots** sit in `docs/screenshots`, one for every screen closed on a sweep. D87 and `DESIGN.md` 4.5 hold the derivation and both measured tables.

**What that leaves genuinely unclaimed** is the dark theme under a color vision check: #152 is open for the protanopia and deuteranopia screenshots against the respread hues, and the tables are the floor rather than the verification.

**The color vision verification is done, and the hues were respread because of what it found.** Simulated protanopia and deuteranopia over the real screenshots at both themes, committed in `docs/screenshots/`.

**As the grid drew them, the light hues collapsed**: rose against moss measured 2.4 CIEDE2000 under simulated deuteranopia, which is the same color. **They now hold at 11.1**, and the ink variants at 12.5. D89. Every hue keeps its angle, which is the owner's mapping; only lightness and saturation moved, and they moved to separate the six from each other rather than to sit at a floor. **Dark holds at 10.8.**

**The binding half was the inks, not the bases.** The notebook draws each section's icon in `ink` rather than `base`, so on the one screen showing all six, the separation being measured was not the separation being seen. Both are spread now.

**No pair collapses**, so the owner's fallback, the section icon at differing shape weight as a second distinguisher, is held in reserve rather than built. If a later change puts any pair back under about 10, that is the answer rather than more color.

**One finding from the derivation is worth carrying forward.** A first derivation that optimized each hue against its own wash alone produced six hues that **collapsed under red-green color vision deficiency**, rose against stone at 2.8 CIEDE2000 under simulated deuteranopia, which is the same color. **Lightness is what survives red-green CVD.** The hues keep their angles, which is the owner's mapping, and are spread across a 48 to 78 percent lightness band, giving a minimum separation of 10.8 across normal vision, protanopia, and deuteranopia. **The tables are the floor, not the verification**: the color vision screenshots still have to be looked at.

**That was written at the end of step zero, when no screen had been converted, and it is kept here only to date what follows it.** As of 2026-08-04 the four destinations, all twelve section screens and five detail screens are on v4, each closed on a device sweep. What remains on the old direction is visible as an open issue, which is what `ORDER OF WORK` step 5 asks for. **Section 0 is the current picture; this section is the record of step zero.**

**The undrawn-screen map moved.** It used to live in this file and in `DESIGN.md` section 8. **It is now `DESIGN.md` section 14**, rebuilt clean from the twenty-five screens that actually exist, correcting a list in the grid that was stale and repeated three of its own mappings. Chapters and Appointments are drawn, at 19 and 22, and are no longer listed as undrawn.

## 4. What is done, and how each piece was verified

Verified means checked through the mechanism, not inferred from the code being written.

| Piece | How it was verified |
|---|---|
| The schema, 34 user data tables | `tools/checks/check_schema.py` runs it into a real SQLite database on every push and asserts the six contract columns, a `live_*` view, both change log triggers, no AUTOINCREMENT, and that a failing change log write rolls the data write back |
| Locally generated ids | `IdsTest`, a JVM unit test. UUIDv7 with same millisecond sequence bits and backward clock protection |
| Event dates, the EDTF model | `EdtfTest`, 20 JVM tests. Round trips every supported precision, proves a month never collapses to its first day, proves uncertainty never widens a range, proves unknown survives |
| Event dates, as they read | `DateVectorTest` on the phone runs `contract/test-vectors/dates.json`, the shared file, and asserts every precision's string, range, and rendering in all four locales. It also asserts directly that nothing coarser than a day ever renders as its first day |
| The date columns | `check_schema.py` asserts every event date is a full four column group and that no bare `<name>_at` survives on a world event. Both failures were verified by breaking the schema on purpose and watching the check catch them |
| Encrypted database, SQLCipher, key in the Keystore | `DatabaseTest` on the connected phone |
| The repository layer | Every read goes through a `live_*` view. Proven by the instrumented suite writing and counting through it |
| Four locale catalogs, ICU MessageFormat | `check_i18n.py` on every push. `CopyIntegrityTest` on the phone proves no locale silently falls back to English for the disclaimer |
| Contrast in both themes | `check_contrast.py` measures 80 pairs against the actual token values on every push |
| Content compliance | `check_copy.py`, `check_templates.py`, `check_contract_isolation.py`, `check_self_contained.py` |
| Schema migrations | `MigrationTest` on the phone. An upgrade keeps every row, a failed step changes nothing and does not move the version, a database from the future is refused, and steps apply in order and only once. Proven with a synthetic step rather than a fake shipped migration |
| The change log, through Kotlin | `ChangeLogTransactionTest` on the phone, through SQLCipher rather than plain SQLite. Insert, update, and tombstone each append exactly one entry, the entry names the table, the row, and the operation, and a write inside an abandoned outer transaction leaves no orphan |
| The fixture generator | `check_fixtures.py` generates twice and compares bytes, checks a different seed differs, checks all six points grow, checks year five hits its stated scale, and checks the shapes a random generator can miss by chance: every bill state, every project state, both instruction tags, an incident that never resolves, and an attachment exactly at the size limit. Proven to catch drift and two of those gaps by breaking them on purpose |
| **The export and import round trip** | `RoundTripTest` on the phone, **15 tests, run both plain and encrypted**. **This is the test B4's whole argument rested on and it did not exist until 2026-08-01.** Every row of every user table compared column by column across an export and a restore. The EDTF string survives byte for byte, a month never collapses to its first day, unknown survives as unknown rather than as null or today, the uncertainty qualifier is not stripped, tombstones travel so a deletion is not resurrected, and the derived range is proven recomputed on import by writing a deliberately wrong one into the file and watching the import correct it |
| The export container | `ExportContainerTest` on the phone. What goes in comes out byte for byte, the manifest survives to the millisecond including tables with zero rows, the manifest is the first entry, and **all eight** files that must fail cleanly each name what was wrong. The last two, an unknown table or column and an attachment the database names that the archive lacks, needed to read the payload and only became possible once the payload was portable |
| **The export is openable somewhere other than the phone that wrote it** | `PortabilityTest` on the phone. **This was false until 2026-08-02 and every other export test passed throughout**, because they all restore onto the same device, where the Keystore key never changes. The archive carried the SQLCipher file as it sits on disk, so no other device could ever have read it, which made the only recovery path from key loss not exist. The check is on the payload's first sixteen bytes for the SQLite magic, plus an encrypted export decrypting to one, plus the live database *not* being one so the first check cannot pass by going vacuous. D61 |
| The restore, end to end through the interface | Walked on the Pixel twice, once before the portability fix and once after. Exported with a passphrase, added a care team row named SHOULD NOT SURVIVE afterward, restored, and the row was gone with the original two people back. A wrong passphrase was tried first and reported honestly without saying which of the two things was wrong |
| The export passphrase is concealed | `PassphraseMaskingTest` on the phone, checking what the field renders rather than which parameters it is passed. **It was rendering in the clear**, because a password keyboard is not a mask. D62 |
| **Today's digest** | `DigestTest`, 10 JVM vectors with no database and no composition, which is the shape #15 asks for. Covers the strict boundary, one row written four times counting as one correction, a row created and removed in the same span counting only as removed, notebook order rather than order by volume, and bookkeeping tables being left out. Walked on the phone with a seeded notebook |
| A visit is a run of the app | `LastVisitTest` on the phone. **It was once per composition**, so a theme change or rotation advanced the mark mid-visit and the digest went blank. Found on the phone with a freshly seeded notebook reporting nothing at all. D63 |
| Attachment storage | `AttachmentsTest` on the phone. The same bytes are one file, the streaming and whole-file paths agree, a changed file fails verification, and a half written file is never visible under its hash |
| The date picker | Walked on the Pixel: opened from the capture form, picked August 18 with a time, and the form read back "August 18, 2026 at 2:00 PM" through the same renderer every other date uses |
| A deleted row is actually gone | `TombstoneTest` on the phone deletes through the repository and asserts it leaves every read the app has: the count, the Unfiled tray, the date lookup, the thread chips, and a link table join. It also asserts the row physically survives, because a removed row leaves nothing to tell a peer it was deleted |
| Tombstones cannot leak | `check_live_views.py` fails any read of a base table outside a live view, in app and test sources alike. Proven by three deliberate breakages: a leak inside the repository, a leak on a screen, and an allowance with no reason |
| Every screen built so far | Instrumented, plus built, installed, opened, and looked at on the Pixel |
| Today, as the app's front page | Walked on the Pixel with a seeded notebook. It led with an apology for the unbuilt digest and three fixed suggestions that ignored the notebook behind them. Now it leads with what changed, each row opening its section, and suggests only what is still undone |
| A section opened from Today comes back to Today | Walked on the Pixel. It used to say "Back to the notebook" to somebody who had come from Today, because opening a section also switched the destination underneath the overlay for no other reason |
| **The eight sections the notebook opens onto** | Each walked on the Pixel with real data typed through the app's own forms, in both themes, at font scale 2.0, and in Arabic. `ScreenReaderTest` covers all of them for labeling, 16 cases. **The reader pass with TalkBack actually running is not done and is not claimed**, #44 |
| The trail's route | Drawn to `DESIGN.md` 5.2 and checked on the device: the dashed gold line runs continuously through the month headings, the node lands on its date at both font scales, node color carries the entry kind, and the whole thing mirrors to the start edge in Arabic |
| Links that go both ways | A medication flagged for the emergency card appears on it, and one that is not does not. Walked with two medications. Taking somebody off the emergency card leaves them on the care team, walked and confirmed |
| A date corrected from the entry | Changed an entry's date in the trail, force stopped the app, relaunched, and the new date was still there, so it is in the database rather than in composition state |
| The notebook's fold behavior | Walked on the Pixel with a hospital stay template: appointments, the trail, documents, and standing instructions forward, money and progress collapsed, which is exactly what that template names |
| Dynamic type at font scale 2.0 | Every built screen looked at on the phone with the system font at maximum. Two defects found and fixed, both invisible at 1.0. The setting was restored afterward |
| Reduced motion | Verified with `animator_duration_scale` actually set to 0 on the phone, not by reading the code. A press still acknowledges, reaching the same target through a 100ms fade rather than a spring. The setting was restored afterward |
| Arabic on the device | Ran through a per-app locale rather than a system setting. Real Noto glyphs, no fallback boxes, and the whole layout mirrored. It also found that the template catalog is English only, #62, which no check covered. **This pass missed a heading defect that had been shipping the whole time**, see the row below |
| **Display headings in Arabic** | **Every Display L title in the app broke in the middle of a word**, including the notebook's own, on a screen with most of its width empty. The negative letter spacing the display styles carry is a Latin device that crushes a connected script's joins and broke line layout outright. `displayS` carries none and was always correct, which is what identified it. Fixed by `healthTrailTypeFor`, held by `TypeTest`, 6 JVM tests. **Found by opening the app in Arabic and looking at a title.** No check covered it, the code read correctly, and the earlier Arabic pass confirmed glyphs and mirroring without ever looking at a heading |
| The typefaces | Bundled and looked at. Bricolage Grotesque, Atkinson Hyperlegible, JetBrains Mono, Noto Sans Arabic. Every license verified against `google/fonts` rather than assumed |
| The capture sheet, looked at with fresh eyes | Two defects nothing else would have found: "Save a document" closed the sheet and did nothing, and the inherited Material scrim barely dimmed the notebook behind it. D44 and D45 |
| Screen reader labels | `ScreenReaderTest` walks every screen's semantics tree, including a sheet's own window, and fails any touchable node with no text and no content description. Ten screens. It found one on its first run and that is fixed |
| Screen reader, with TalkBack actually running | Walked on the Pixel 2026-08-01 with TalkBack enabled, the notebook, Appearance, and the capture form. Traversal order matches visual order. Rows are one stop reading "Care team, nothing yet" rather than two fragments. Fields carry their label. Selection reports as `checked` on chips and options, so a reader user is told which is chosen rather than inferring it from a mark. No unlabeled control. The phone was restored exactly and TalkBack confirmed unbound. D54 |
| Every screen looked at with the keyboard up | Two defects found that way and nowhere else: the setup button colliding with the last field, and the field clipped mid-box at the scroll boundary |
| The Unfiled tray | Walked on the Pixel end to end: a call saved with no thread, the waiting card appears on the notebook, the tray suggests "Nursing" from the words in the entry, filing it links the thread and clears the tray in one transaction, and the card disappears |
| **The component library, section 11** | Built and used rather than described. The tile, the dense row, the hero, the thumbnail, the capped chip group and the disclosure each appear on at least one screen, and each was looked at on the phone with the year five fixture, at font scale 2.0, and in Arabic. **The stat display, the avatar and the segmented control are specified and not built**, and saying so is the difference between a library and a wish list |
| The icon set, 23 drawings | Rendered on one sheet by `tools/icons/sheet.py`, which reads the paths out of `SectionIcon.kt` rather than holding a copy, at 44dp, 32dp and 20dp. **Both silhouette collisions were invisible in the source and obvious on the sheet.** `docs/icon-set.png` is the current sheet |
| The capture form's chip cap | Walked on the phone: ten people on the year five notebook show five chips and "Show all 9", the sheet opens with search, and picking from it fills the field. `cappedChips` keeps the chosen answer in the five |
| The unfiled tray's suggestion | Walked: tapped the suggestion on one card and the notebook's count went 86 to 85 in one tap, where it took two before |
| Project steps are editable | Walked all four: added a step, edited it, moved it earlier and later, and removed it through the confirmation. The move controls are absent at either end rather than inert |
| A project's own template | Walked end to end: saved five steps as a template, found it in the library under "Yours", started a project from it, and watched the library go from "Nothing started from this yet" to "1 started from this" with the project linked |
| Documents render actual images | Walked with the year five fixture after teaching `pack.py` to write page images. **Before that the screen had never rendered one**, because the fixture's attachments were random bytes and every thumbnail fell back to its kind drawing |
| The press state, everywhere | Measured on the device on three different surfaces: a card row (26,36,43) to (43,50,56), the filled button (127,182,212) to (136,186,214), the capture button (227,177,85) to (228,182,100). `FilledButton` and `TextAction` previously had no press state at all |

**The whole instrumented suite: 277 tests, 0 failures as of 2026-08-03 14:50.** Before that it was **three failures, and this file said zero for a day**: `5ca7368` gave the emergency card its own count string and left `NotebookScreenTest` and `ReaderStopsTest` asserting the old one. D73 has the account and the rule it sets. The older sentence read: 275 tests across 30 classes, 0 failures, run on the connected Pixel 10 Pro XL through `tools/verify.sh --device`, and the app was reinstalled immediately afterward per D56. **98 JVM unit tests, 0 failures**, the newest of which are `EntryHeadingTest`, `EmergencyCardClaimTest` and `MedicationEventKindTest`, all three written against defects the fixture found rather than against code being added. All 11 implemented compliance checks pass, and **lint passes**, which is the step that keeps catching what the habitual checks do not.

**A pattern worth carrying forward.** Almost every defect this run found came from putting the built thing in a hand and changing one condition: the font at maximum, the keyboard up, the language set to Arabic, or simply looking at a screen that had already passed its tests. None of them were visible in the code, and several had passed a review. The tests are what keep them fixed; they are not what found them.

---

## 5. Remaining work inventory, in order

> **Superseded on 2026-08-03 by the v4 adoption.** The inventory below describes work against the old direction. **The live inventory is the board**, project 3, where 67 new issues sit in `ORDER OF WORK` order and 37 old ones were closed as superseded. Section 0 of this file names what to pick up first. What follows is kept because the closed-issue history in it is still useful for a session wondering whether something was ever built.

**Rebuilt from the tracker on 2026-08-01.** The previous version of this section had four rows spliced in from section 4's verification table, which put `MigrationTest` text under issue #9 and left three rows with no issue number at all. It also listed #39 as unbuilt while sections 4 and 9 recorded it as built and walked. It was patched too many times and is now derived from `gh issue list` rather than edited in place. **If this section and section 3 ever disagree, rebuild this one from the tracker and make section 3 follow it.**

**Closed in the long run of 2026-08-01**, so a fresh session does not go looking for them: #14 the migration mechanism, #22 the end of life instruction tag, #36 the notebook, #37 setup, #38 the date model, **#39 the date interface**, #40 the press sweep, #41 the situation picker, #42 measurement, #48 the template, #53 the Unfiled tray, #58 the subject scoped counts, and #78 the empty Today. #21, the roadmap, is also closed, and **#12 the fonts**, closed once Chinese was verified rendering from the system face on the device. #25 About and #57 the document capture input closed in the same run. #102 and #109 closed on the language question and the translation disclaimer.

**Changed on 2026-08-02**, in the run that continued through the morning. The rows below are current as of 06:25.

**In order. The first is the one to take.**

| Issue | What | Why here, and what it is actually waiting on |
|---|---|---|
| **#9** | **The export container** | **Done.** Round trip, encryption, all eight failure cases, portability, and as of 2026-08-03 a passphrase is required and there is no unencrypted export, D67. Format version 2 |
| **#47** | **Search** | **The universal half is done and walked.** #131 carries the rest |
| #62 | The template catalog is English only | Release blocking. The app currently shows an Arabic interface wrapped around English content, which any Arabic reader sees immediately. Found by running the device in Arabic, not by any check |
| #43, #44 | The retroactive audit, and the accessibility gate | **Worked alongside new screens, never saved for a phase gate.** Both partly done with findings on the issues. **#44's reader criterion is met for three screens**, walked with TalkBack actually running on 2026-08-01, D54. What remains is the same pass over the screens not yet walked, which is now cheap and proven safe |
| #57 | The document capture input | The last of the six ways in. **No longer blocked**: the attachment storage it needed landed with the export's first piece |
| #8 | The repository layer | ~~Tombstones travel through the export.~~ **Proven by `RoundTripTest`.** Ready to close once somebody confirms the other criteria |
| #7 | The change log | Proven through Kotlin. **The digest now reads from it**, so the last criterion is met. Ready to close once somebody confirms |
| #17 | The fixture generator | Everything but the four language variants, which wait on #62 |
| #15 | Golden vectors | `dates.json` runs on the phone in all four locales, and **`DigestTest` adds 10 JVM vectors for the digest engine**. What remains is a second platform to run them against, which is #16 |
| #10 | `SyncTransport` | Needs the export container, so it follows #9 |
| #46 | No dead ends, links both ways | **Partly done.** Today's digest rows and coached steps now open what they name, and a section opened from Today returns to Today. What remains is a deliberate sweep of the rest rather than the two found by walking |
| #47 | Search | **Unblocked.** Today and the digest engine both exist now. This is the largest remaining feature and a reasonable next thing to take |
| #45 | Capture from outside the app | Independent of everything above. Widget, quick settings tile, share sheet target |
| #16 | The web scaffold | `npm` is absent on this machine. Nothing else blocks it |
| #13 | The four locale scaffold | Arabic and Chinese are both verified on the device now, and choosing a language actually changes the language, which it did not before D52. What remains overlaps #62 |
| #18 | Content checks in continuous integration | Ten run. Open for the ones not implementable yet, each named in `run_all.py` with what it waits on |
| #1 | Phase 0 parent | Closes when its children do |

**In the review queue, waiting on the owner rather than on work.** Thirty five as of 2026-08-03, and **six of them now describe a screen that no longer exists and carry a comment saying so**: #34, #50, #55, #81, #122 and #123. The newest are #146 a project, #147 the template library, and #148 documents as a gallery. The older list read: twenty two, and none of them is waiting on anything this project can do: #28 the disclaimer gate, #30 setup, #32 the situation picker, #34 the capture sheet and form, #50 the notebook, #55 the Unfiled tray, #68 the date picker, #81 Today's empty state, #89 Appearance, #111 the trail and care team, #113 the emergency card, #114 medications, #115 Ask next time, #116 care threads, #117 Progress, #118 chapters, #119 appointments, #120 standing instructions, #121 money, #122 documents, #123 projects, and #124 About. Each carries a real device screenshot. **Arabic screenshots are no longer blocked** for any of them.

**#125 is a question for the owner, not a task:** should the app open on Today rather than the Notebook? `MASTER_SPEC.md` calls Today the dashboard. Today is now worth opening on, which it was not when the question was first asked.

**Phase 1 feature work still ahead:** Today with the digest engine, the trail itself, care team, medications, the emergency card, projects, and More.

~~**An in-app theme setting.**~~ **Built, #88.** Follow the phone, light, or dark, in More. It applies immediately and persists, and it removed the standing dependency it was partly built to remove: **both theme sets are now captured from inside the app and the phone's own theme is never touched.** `tools/screenshot.sh` reads the app's stored choice first and falls back to the device only when the choice is to follow it, which corrects D31's assumption that the device is the answer.

**Language access comes after all of the above**, and it is a body of work rather than a task. **Twelve issues are open and none of them is started, deliberately.**

**#92 is the parent** and carries the ten languages, the ordering, and the cost. Seven new languages: **#93** Vietnamese, **#94** Korean, **#95** Tagalog, **#96** Russian, **#97** Haitian Creole, **#98** Portuguese, **#99** French. Then **#100** script and typeface coverage, **#101** plurals with golden vectors, **#102** translation quality and what shippable means, and **#103** right to left confirmation.

**It is language access for caregivers in the United States, not international expansion.** The federal, Medicare, and Medicaid content is specific to this country, so translating for a Spanish speaker in Texas is right and presenting the same app to someone in Spain would be wrong. `MASTER_SPEC.md` sections 7.1 and 7.2 carry the reasoning.

**Two things easy to get wrong, both written into the issues.** Haitian Creole is a distinct language and never a fallback for French, or the reverse. Chinese ships as Simplified and Traditional is a separate question rather than an alias.

**Roughly 1500 strings per language, so seven languages is on the order of ten thousand**, each of which is care instructions, money, or somebody's rights. **An unreviewed language is not shippable**, not shippable with a caveat. That applies to the four already shipping, all of which are currently unreviewed.

**Do not begin any of it until everything ahead of it is done.**

**Something that must not survive to release.** The More destination renders an honest interim screen below Appearance, and Today says plainly that its digest is still being built. **Projects is built**, so only those two remain. **The document capture input no longer does**, because it is built, #57. That is deliberate rather than a stub: `DESIGN.md` section 5.5 fixes the four destinations and their order, and D44 says an interface may offer something it has not built but may not go quiet when someone takes it up. Each disappears as its destination lands. **If one is still there at release, that is a bug**, and `ShellTags.NOT_BUILT` makes them greppable.

---

---

## 9. Screens built without a mockup

> **The map moved on 2026-08-03. It is now `DESIGN.md` section 14**, rebuilt clean from the twenty-five screens the v4 grid actually draws, and it names which drawn screen every undrawn screen follows. It corrects a list in the grid's own Part C that was stale and repeated three of its own mappings, and that wrongly listed Chapters and Appointments as undrawn when both are drawn, at 19 and 22.
>
> **The protocol also moved**, from `DESIGN.md` section 10 to **section 13**, and the three places a new screen is logged are now: a `needs-design-review` issue with a device screenshot, a row in `DESIGN.md` section 14, and a line here.
>
> **Every `needs-design-review` issue in the table below was closed as superseded**, because each reviewed a screen built on the old direction and each is replaced by a conversion issue carrying the two audits. The table is kept as the record of what was built and when.

Every screen built without one is composed from existing components, ships complete with every state, and is logged in three places at the moment it is built.

| Screen | Built | Issue | Reviewed |
|---|---|---|---|
| Documents, as a gallery | 2026-08-03 | #148 | not yet |
| The template library | 2026-08-03 | #147 | not yet |
| A project, rebuilt as a spine with editable steps | 2026-08-03 | #146 | not yet |
| One medication, and how it changed | 2026-08-03 | #140 | not yet |
| One chapter, and what happened there | 2026-08-03 | #139 | not yet |
| One care thread, and everything on it | 2026-08-03 | #138 | not yet |
| An appointment's prep sheet | 2026-08-03 | #137 | not yet |
| One person, and everything that involved them | 2026-08-03 | #136 | not yet |
| One entry, read on its own | 2026-08-02 | #134 | not yet |
| Incidents, and one incident's thread | 2026-08-02 | #133 | not yet |
| Search | 2026-08-02 | #130 | not yet |
| Exporting the notebook | 2026-08-02 | #126 | not yet |
| Restoring from a file | 2026-08-02 | #127 | not yet |
| Today, rebuilt around the digest | 2026-08-02 | #81 | not yet |
| The trail | 2026-08-01 | #111 | not yet |
| The emergency card, and filling it in | 2026-08-01 | #113 | not yet |
| Medications, and adding one | 2026-08-01 | #114 | not yet |
| Ask next time | 2026-08-02 | #115 | not yet |
| Care threads | 2026-08-02 | #116 | not yet |
| Progress, the readable record | 2026-08-02 | #117 | not yet |
| Chapters, the places | 2026-08-02 | #118 | not yet |
| Appointments, and adding one | 2026-08-02 | #119 | not yet |
| Standing instructions, and asking for one | 2026-08-02 | #120 | not yet |
| Money, and adding a bill | 2026-08-02 | #121 | not yet |
| Documents, and saving one | 2026-08-02 | #122 | not yet |
| Projects, starting one, and a project's steps | 2026-08-02 | #123 | not yet |
| About | 2026-08-02 | #124 | not yet |
| Care team, read only | 2026-08-01 | #111 | not yet |
| Disclaimer gate | 2026-07-31, rebuilt same day to the 10.6 bar | #28 | not yet |
| Essentials first setup | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #30 | not yet |
| Situation picker | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #32 | not yet |
| Capture form, four kinds | 2026-07-31, rebuilt same day to screen 26 | #34 | not yet |
| Notebook table of contents | 2026-08-01, rebuilt to the 10.6 bar | #36, review on #50 | not yet |
| Unfiled tray | 2026-08-01 | #53, review on #55 | not yet |
| Adding a measurement | 2026-08-01 | #42 | not yet |
| The date picker | 2026-08-01 | #39, review on #68 | not yet |
| Today, the empty state | 2026-08-01 | #78 | not yet |
| Appearance, and More around it | 2026-08-01 | #88, review on #89 | not yet |

The notebook is drawn in the reference file, so it is listed here as a correction rather than as an undrawn screen. `DESIGN.md` section 3 item 8 records the four ways the built screen departs from the mockup, with reasons.

**Known ahead:** the template library, the four template pickers, the template detail view, and the template editor. None is drawn. `MASTER_SPEC.md` section 4.10 carries their requirements.

---

## 10. Persona runs

### P1, day one in a hallway. Walked 2026-08-01 on the Pixel, fixture: fresh install, no seed

**Four of the five things P1 says must be true are true. One is not.**

| Must be true | Result |
|---|---|
| The disclaimer appears and requires explicit acceptance | Yes. Nothing else is reachable until it is accepted |
| Setup asks for three things and lets everything else wait | Yes. Every field skippable, and skipping produces a working notebook |
| The empty Today coaches rather than sitting blank, first suggestion the Emergency Card | **Now yes.** It failed on the walk, which is how issue #78 came to exist, and it was built the same day |
| A first call can be logged in under thirty seconds from cold launch | **Six taps and no typing**, skipping everything optional: accept, skip setup, "Not sure yet", the capture button, "Log a call", save. Comfortably inside thirty seconds for anyone |
| Nothing asks for an account, an email, or a permission not needed yet | Yes. Nothing anywhere |

**What P1 says to watch for, and what was seen:**

- *Onboarding that cannot be completed one-handed.* Every primary action is in the lower half. The capture button is centered above the navigation bar.
- *A keyboard covering the field being typed into.* Fixed this run, D38. It was real on setup and is not any more.
- *A required field that should be optional.* None. The whole path can be walked without typing a character.
- *A screen that assumes the person already knows the facility's name.* Setup's "Where are they right now" hints "The ward, the building, or just the town", and it is skippable.

**Worth recording beyond the checklist:** answering "Not sure yet" to the situation picker produces a notebook with no care threads, and the capture form then drops the thread question entirely rather than showing a question whose only answer is "not sure". The entry is not marked unfiled, because with nothing offered, not choosing is not the person declining to say. That behaved correctly without being specifically tested for.

### P2, week one, building the notebook. Walked 2026-08-02 on the Pixel, fixture: fresh install, nursing home situation

**Walked without the day-7 fixture**, and that limit is stated rather than hidden: the generator writes a plain SQLite file and the app's database is SQLCipher keyed by the phone's Keystore, so there is no way to seed it short of building an export container in Python. What was walked is everything P2's requirements turn on except volume.

| Must be true | Result |
|---|---|
| The situation template applied its **threads** | Yes. Five, and the notebook counts them straight away |
| It applied its **fold behavior** | Yes. Standing instructions, the trail, threads, and money forward; appointments and progress collapsed |
| **Adding a contact offers the template's roles as suggestions without forcing them** | **It did not. Fixed this run.** All six now appear as chips above a free text field that is unchanged |
| It applied its **checklist** and **document slots** | **No. Neither is built.** The catalog carries ten checklist items and six document slots for a nursing home and nothing reads them. Filed as #135 |
| Any of it can be edited or deleted | Threads and people yes. The checklist and document slots do not exist to edit |

**The roles finding is the one worth dwelling on.** `TemplateCatalog.Situation.roles` has existed since the catalog was written, with a comment on the field reading "Contact roles to offer when adding a person. Suggestions, not a fixed list." **The data was parsed, documented, and never shown.** A nursing home notebook knew there was a director of nursing, a charge nurse, a social worker, an assessment coordinator, an administrator, and a billing office, and it asked the person to type all six from memory.

That is Part Two's rule almost word for word: anywhere the set of possible answers is knowable, offer chips rather than a text field.

**The field stays, and a chip only fills it.** A role not on the list is the common case in home care, tapping a chip twice clears it, and what it filled can be edited. Nothing became a fixed vocabulary.

**What P2 says to watch for, and what was seen:**

- *Template content that reads as advice rather than administration.* The five threads are Nursing, Daily personal care, Activities, Meals and dietary, and Social services. All administration.
- *A checklist that cannot be edited.* There is no checklist, which is #135 rather than a pass.
- *Roles that cannot be renamed.* They are suggestions filling an ordinary text field, so renaming is typing.

### P4, month six, the first fight. Partly walked 2026-08-03 on the Pixel, fixture: month6 seed 1, restored through the app

**The first persona walked against generated data rather than data typed by hand**, which is what `tools/fixtures/pack.py` unlocked.

| Must be true | Result |
|---|---|
| **The incident thread records every call with names and dates and reads start to finish** | **Yes.** Five weeks of chasing on one screen: reported to the charge nurse on April 11, called the unit on the 20th, asked the director of nursing in writing on the 29th, told on May 8 it had gone to the care plan meeting, told what they decided on May 17, and then a hollow waypoint and "Nothing since. This one is still open." |
| Open incidents are visible from Today | Yes, "1 open incident", opening the list |
| **Each of these exports as its own document, legible to somebody who has never seen the app** | **Yes for an incident.** Not yet for a standing instruction or a bill |
| The standing instruction shows its violation count, each violation linking to its bill or incident | **Not built.** `instruction_violation` is in the schema and nothing reads it |
| The disputed bill carries its state and its link to the instruction it broke | **Partly.** Bills carry state; the link to an instruction is not built |

**Two fixture defects found by looking at real data, both fixed.**

**The generator wrote incidents with nothing on them.** Every incident read "0 things written down", because no entry ever carried an `incident_id`. P4's first requirement is precisely that the thread records every call, so the persona was untestable against generated data. The generator now writes two to five steps per incident, spread from the report to the answer.

**"0 things written down" read as broken** even when it was true. An incident is itself a thing written down, so the zero case now says "Nothing written down since" and "Answered and nothing else written down".

**Not walked yet:** P3, and P5 through P13. P10 through P12 need #62, since the template catalog is English only and a language persona against English content tests nothing.

---

### P7, year five, the long record. Walked 2026-08-03 on the Pixel, fixture: `year5` seed 5, restored through the app's own restore screen

**The fixture is finally the one P7 describes**, which it was not before tonight: 1,630 entries, 9 chapters, 7 care threads, 23 appointments, 90 questions, 8 medications with 75 events, 40 documents with attachments, 15 milestones, 10 people and 562 entry-person links.

| Target | Result |
|---|---|
| Cold launch to interactive, under 1.5s | **Pass, about 0.9s.** Still loading at 0.8s, drawn at 1.0s, measured by capturing the screen at fixed delays. Note this is **the notebook, not Today**: the app opens on the notebook, which is what #125 asks the owner about |
| Universal search, first results under 400ms | **Pass, though not measured to the millisecond.** Results were already rendered by the time a capture could be issued, which is about 1.1s after typing began and mostly the tools' own latency. The honest statement is "faster than adb can observe", and a real number needs a benchmark rather than a stopwatch |
| Trail scroll from now back to year one: no dropped frames, no placeholders, no jumps | **Pass.** 3,026 frames over the full scroll to the end of the record, **0.23% janky, 99th percentile 14ms**. An earlier shorter run measured 0.48% at p99 16ms. No loading placeholder was ever visible and the position never jumped. The scroll ends on the undated group, which is where the ordering puts entries whose date nobody knows |
| Year scrubber jump to any year, under 300ms | **Not testable. There is no year scrubber**, which is the finding rather than a pass or a fail |
| Assembled collection for an item with 30 related records, under 1s | **Not testable. The assembly view is #131** and is not built |
| Export: completes, shows progress, cancellable, survives backgrounding | **Not walked.** Ran out of session |
| Memory: no growth across 20 minutes of navigation | **Not walked.** Ran out of session |

**What P7 says to watch for, and what was seen.** Nothing loads the whole trail to show ten rows: the scroll is flat across 1,630 entries and the frame numbers say so. Chapter and thread filters are in the query rather than in application code, which the live-view check already enforces. The PDF export and attachment scanning were not reached.

**The tooling lesson, which is the same one as four times earlier tonight.** `uiautomator dump` costs about 2,770ms on this notebook, so the first attempt to time the launch measured the dump and reported 2,770ms. `screencap` costs about 985ms, also too slow. And a pixel detector written to spot the drawn screen sampled a band that is empty even when the screen is drawn, so it reported "still loading" at five seconds against a screenshot that plainly showed the notebook. **Three instruments in a row reported on something other than what they were asked about**, which is D68's rule arriving for the fifth time in one night: distrust a negative result from a tool that cannot tell you what it did not examine.

## 11. Open questions

The six in `MASTER_SPEC.md` section 10. Three are decided and recorded, three are not yet forced:

1. Tombstone retention window. **Decided: 730 days**, D11.
2. Whether the change log is exported. **Decided: yes, and the importer renumbers it**, D12.
3. Attachment size and count limits. **Decided: 25 MB each, a warning at 4 GB total, no hard ceiling**, D13.
4. Whether the web scaffold uses the same UI toolkit or a minimal one. Not forced until #16.
5. PDF pagination for very large exports. Not forced until Phase 5.
6. How the app describes its own translation status honestly. The catalogs carry `reviewed_by_native_speaker: false` and `check_i18n.py` prints it, so the app can say so plainly. The wording is not written and it affects the store listing and the README.

---

## 12. Subagents

`AGENTS.md` defines four. **Subagents never write anything**, per `CLAUDE.md` rule 8. None was used this session.

---

## 12a. The front page, corrected 2026-08-03

**`README.md` showed five screens that no longer exist and listed three built things as still being built.** The screenshots are the current ones now and the two lists are true: the digest, universal search, the portable encrypted export, the documents gallery, editable projects and their own templates, and the standing instruction violation record all moved from "still being built" to "can do today".

**The continuous integration check for this only fires on a pull request**, and every increment tonight went straight to `main`, so it never ran. **The rule it protects still applies**: a public front page that undersells a working app is as much a lie as one that oversells it, and this is the class of staleness nobody notices because the person who changed the code is not the person reading the front page.

## 12b. The overnight run of 2026-08-05, and what it cost to find

**Nine increments, all pushed, all seen on the phone.** The account is here rather than in `HANDOFF.md` because it is how things came to be rather than what is true now.

**What closed:** #274 the projects empty state, #276 and #277 starting a project with the template's five defaults shown before anything is created, #280 the busy stretch clustering its steps, #291 every one of those five defaults made editable from setup. #216, #305 and #306 had closed earlier in the same run, and #306 was reopened at the end of it.

**Three defects were found by opening a screen rather than by reading code, which is the pattern this project keeps rediscovering.**

- **The fixture handed four hospital discharge areas round robin to any steps led project**, so "The house", "The ride" and "Equipment" sat over three steps of a power of attorney with one row under each. It looked like nonsense on the phone and it was. The same defect the handler list had already had, fixed the same way: areas indexed alongside the steps themselves.
- **A steps led project generated two to six steps**, so the busy stretch was never busy and the clustering had nothing to cluster. Seven to ten now.
- **`RoadStrip` broke a stage name in half at font scale 2.0** with four stages: "Gathering" rendered as "Gatherin" over "g". A threshold derived from the font scale was written first, reasoned correctly, and changed nothing on the phone. Measuring the text with a text measurer worked. **That is twice in one night that a number which reasoned well lost to a screenshot.**

**One promise was found to be false in the app's own words.** The road turning sheet said "the date is today unless you change it" and offered no way to change it. The sheet had been built, wired, and never opened.

**#306 was closed on three green runs and was not fixed.** It failed twice at the end of the run, once expecting `en` and getting `ar`, once getting `es`, which is the exact symptom its own fix comment describes. The trigger is the per app locale being non empty when the suite starts, which the run's own right to left checks kept leaving behind. **Three fixes were tried and none worked**, so the file went back to its committed state and the issue was reopened with all three written into it. Rule 9, applied rather than admired.

**Two hazards were learned the hard way and are now in `HANDOFF.md` section 7.** `installDebug` clears this app's data on this phone, so every device check is install, then seed, then navigate. And `connectedDebugAndroidTest` uninstalls the app when it finishes, after which `walk.sh see` dumps the owner's home screen with his real calendar and contacts on it; nothing was captured to disk, and `screenshot.sh` would have refused, but `walk.sh` does not.

**Five screens were composed rather than drawn** and are logged in all three places per rule 12, at #309 through #313. They are the screens behind the four setup rows the grid draws as doors without drawing what is behind them.

---

## 13. Uncommitted work

**None.** Verified with `git status --porcelain` returning nothing and the push confirmed against `origin/main`, rather than assumed. Per the memory this project keeps: **an increment ends when `origin/main` has it**, and a local commit is not done.

### The instrumented suite, which had not run in weeks

**291 instrumented tests.** They could not run at all until this session, because the `androidTest` sources stopped compiling when the capture button left the navigation bar and nothing rebuilds them on the unit test task. Compiling them again meant running them again, and running them found **seven failures, none from tonight's work**:

- **Five were the notebook becoming four sections and a fold** on 2026-08-03. Every test there was written when all twelve sections were on the front door. They open the fold now, which is what a person does.
- **Two were the same thing** reached through the medication journey.
- **One was a test tag that went missing in a redesign.** The notebook's counts carried a tag while the front door was tiles; the v4 conversion made it dense rows and the tag stayed with the tile, so three tests looked for a node that no longer existed with no way to know why. `DenseRow` can tag its subtitle now.

**All 291 pass as of the end of this run.** Confirmed by a full run after the fixes, not by rerunning the two classes that had been red.

**Do not touch the phone while it runs.** A capture attempted mid-run on 2026-08-04 force-stopped the app under the suite and produced a partial result: 79 tests with one failure in `MigrationTest`, which is not a defect, it is interference. A run driven from two places at once tells you nothing.

**Run `:app:connectedDebugAndroidTest` after any run that changes a screen.** It takes about six minutes and it is the only thing that would have caught these. **It uninstalls the app and takes the notebook with it**, so `tools/seed.sh year5 5 walk-year-five` afterward.

### The device, as it was left

**The year five fixture is loaded** through the app's own restore screen, which is how `tools/seed.sh` puts it there. If the notebook looks empty, an instrumented test run has cleared it: `tools/seed.sh year5 5 walk-year-five` puts it back in about a minute.

**Every accessibility setting is back where it started**: font scale 1.0, night mode off, and the per-app locale at the system default. Each was recorded before being changed and restored after every sweep, which is the exception `CLAUDE.md` rule 19 grants and its condition.

**Reinstalling the debug build resets the disclaimer gate and the setup flow** but keeps the notebook. That is not a defect anybody has looked into; it is worth knowing because a walk script that does not tap through the gate lands on the wrong screen and reports NOT REACHED.

**What the trail dragged with it, so a cold session is not surprised by the diff:**

- `SectionScaffold` takes a `listState` and an optional `rail`, and reserves `Space.railInset` for the rail rather than letting it sit over the words. Every other section passes neither and is unchanged.
- `ScopedSearch` is a new component, and it is in the inventory as "search bar" rather than being an invention. `DESIGN.md` section 7.
- `TrailEntry.pinnedAt` has **no default**, which is D95 and which is what caught the entry screen reading a column it never selected. Five queries name it now.
- The androidTest sources had been uncompilable since the capture button left the navigation bar. They compile again, and the capture button has its own reader test rather than being tested through a signature that no longer exists.

**None as of the step zero commit.** Verified with `git status --porcelain` returning nothing and the push confirmed against `origin/main`, rather than assumed. Per the memory this project keeps: **an increment ends when `origin/main` has it**, and a local commit is not done.

---

## The Today and Projects grids, and the overnight run of 2026-08-06

**Moved out of `HANDOFF.md` on 2026-08-07**, because that file is read in full at the start of every session and had grown to 836 lines. Nothing here is current state; it is the account of how these surfaces came to be. `HANDOFF.md` is the short one.

## 2. The Today and Projects grids are adopted, and step zero is done

**2026-08-04, owner's instruction.** Two new design references arrived and are now `reference/projects-grid.html` and `reference/today-grid.html`. **They extend v4; they do not replace it.** Everything in `DESIGN.md` sections 1 through 19 still governs both. Where the v4 grid drew Today or Projects, those drawings are superseded; every other screen in it is untouched. D106.

**Step zero is complete and was documentation only. No application code was written against either grid.**

- **`DESIGN.md` gained sections 20 through 23**, encoding both grids in the repository's own words: Projects, Today, the global voice rule, and the two new audits. **Read those rather than the HTML.** The three grid files are named side by side at the top of `DESIGN.md`, each saying what it governs.
- **`DECISIONS.md` D106 through D112** record the adoption, the eleven inventory additions, the handler-tag ruling, the voice rule, the data contract amendment, the seven provisional resolutions, and the freeze rule.
- **`contract/DATA-CONTRACT.md` gained 8.7**: the Today layout, project templates, stage assignments, standing entries, recorded dates with their sources, and steps with handler tags are **record**, not preference. They travel in the archive and restore on import.
- **`MASTER_SPEC.md` 4.3 and 4.5 were rewritten**, and phases 1 and 4 corrected.
- **`docs/REMOVAL-LEDGER.md` is new.** Superseded Today and Projects code freezes rather than being deleted: never called, extended, fixed, or translated. **Its ledger is empty because nothing has been superseded in fact yet.**
- **`check_copy.py` gained the battle-voice rule**, D109.

**Fifty-nine issues were opened for the work**, #243 through #301, and **the Projects half is now all closed but three**: #288, #289 and #290. Today's are untouched. The two parents are **#243 Today** and **#244 Projects**. Under them: 28 screen issues, 17 card issues each carrying its full states ladder as acceptance criteria, 11 component issues, the data contract work at **#262**, and the two provisional template hands at **#273**.

**Take #262 first.** Everything on both surfaces stores something, and building screens against a contract that does not exist yet is the order that produces a second migration.

### 2.1 #262, the data work: landed, not yet seen on a screen

**The database now holds what a person arranges.** `contract/schema.sql` is at **version 2** and `HealthTrailDatabase.SCHEMA_VERSION` moved with it.

**Six new tables**, each with its live view, both change log triggers, and its indexes: `today_card`, `project_stage`, `project_standing`, `project_date`, `project_date_kind`, `project_paper`. **Four new columns**: `project.lead`, `project.current_stage_id`, `project_step.cluster`, `project_step.handler_label`.

**Three things in it are worth knowing before touching either surface.**

- **The lead slot is singular in the database, not only on the screen.** A partial unique index, `ux_today_card_lead`, refuses a second lead per subject. Zero is the application's to prevent, because a database cannot require a row to exist. It is the schema's first `CREATE UNIQUE INDEX`.
- **`today_card.source_table` and `source_id` are deliberately not foreign keys.** 8.7 requires that a card whose source is gone is kept rather than dropped, and a foreign key would make import the thing that quietly edited somebody's desk.
- **No column marks a project's important date.** The screen leads with the soonest date that has not passed, and the most recent one when they all have. D113 says why, and what that gives up.

**The migration is real and is proved against a version 1 database**, not against today's. `Migrations.steps` has its first entry; `Step.apply` now receives the contract schema text alongside the database, so an additive step replays `contract/schema.sql` rather than carrying a second copy of six table definitions that would drift. `MigrationTest` builds a hand-written version 1 database and asserts the tables arrive, the columns arrive, and the project and its step survive with nothing invented in the new columns.

**The repository layer is built too**, and the trust model is tested rather than asserted. `ArrangementTest` is 16 tests covering the part a screenshot cannot show: the lead is singular, promoting demotes, the lead cannot be removed, removal is a tombstone, the layout does not reorder itself between reads, a broken layout is reported rather than repaired quietly, and a card pointing at a tombstoned project keeps its reference.

**Three test accessors were added and are named so nobody reaches for them by accident**: `columnForTest`, `clearEveryLeadForTest`, and `tombstoneForTest`. The middle one exists to produce a state the app cannot, which is the only way to prove the reader reports a broken layout instead of inventing a lead.

**349 instrumented tests pass**, up from 332. **All 17 checks pass, 167 unit tests pass, lint is clean.**

**Four tests were failing for reasons worth knowing.**

- **`SituationPickerTest` was broken by `11c5a4e` and nothing caught it**, because the instrumented suite was last run before that commit. The picker renders every catalog subtitle through `Bidi.isolate`, so the rendered string carries U+2068 and U+2069 and no longer equals the bare catalog string the test compared against. **Found by opening the app and reading the screen**, not by reading the code: `tools/walk.sh see` prints the isolate marks. The test now compares against `Bidi.isolate(subtitle)`, which is what the screen actually draws. **#226's worklist will keep producing this**, so expect it.
- **`BackJourneyTest` was broken by the new `ArrangementTest`**, four classes later, with nothing to connect them: the new test did not close the repository in an `@After`, and the repository is a singleton over the one real database. Every other data test here closes it. It does now.
- **Three counts were written down in tests and are now counted from the contract**: the table, view and trigger counts in `FoundationSmokeTest` and `DatabaseTest`, and the trigger count in `SchemaStatementSplitTest`. A number in a test file means every table added to the contract fails a test for a reason that has nothing to do with what the test is about. **They assert the shape now instead**: one live view per user data table, two triggers each.
- **`MigrationTest`'s synthetic steps sat at a fixed version 2**, which stopped running the moment `CURRENT` reached 2, so three tests passed while proving nothing. They are at `CURRENT + 1` now.

**The fixture writes all of it now, and the archive carries it.**

- **`tools/fixtures/generate.py`** writes the road, where it stands with its history, dates on both sides of today with their sources, date kinds, papers both filled and empty, step clusters and handler tags, and a seventeen-card Today layout. All three project leads exist, because two of the three project home screens cannot be looked at on the phone otherwise.
- **A Today card deliberately points at a finished project**, which is the source-closed rung of the states ladder.
- **The future dates are written as `UPCOMING_DAYS` offsets**, not as small numbers. The history ends on a fixed date, so "+21 days" stops being in the future three weeks later and the upcoming rung disappears silently. They go stale on the same day the upcoming appointment does, and moving `HISTORY_ENDS` fixes both.
- **`check_fixtures.py` now fails if any of it stops being written**, including if the dates stop falling on both sides of today. **Each new assertion was proved by breaking the generator on purpose**, not by watching the check pass.
- **`RegenerationTest`'s notebook now contains the arrangement**, so export, import, and re-rendering the readable copy byte-identically actually covers these tables. It did not before, and it would have kept passing.

**All sixteen templates carry the five defaults, and starting one applies all five.**

- **Only three fields were actually missing.** `steps` was already the starting steps and `documents` was already the usual papers, so they are used as those rather than duplicated. The new fields are `lead`, `stages` and `date_kinds`. `templates/SCHEMA.md` says which field is which default.
- **`check_templates.py` holds them**: `lead` to the same closed set the schema's CHECK enforces, `stages` to at least two, `date_kinds` to non-empty. Proved by breaking the data on purpose.
- **`startProject` applies all five in one transaction**, and it applied only the steps before, so a project started from a template had no road, no chips and no papers.
- **A person's own template carries the whole shape now too.** `saveProjectAsTemplate` wrote only the steps, so somebody who shaped a project over months and saved it got the checklist back and nothing else. Bodies written by older builds have none of the new keys and fall back to empty, which is what those templates actually held.

**#262 is done apart from device verification.** **That is the next work and it is #263 onward**, not a remainder of #262.

### 2.2 The five Projects components exist and have not been seen

**Built and compiling**, #263 through #268: `RoadStrip`, `StandingCard`, `DateRow`, `LatestWordCard`, `StepRow`, `ReferenceLine`. Each carries its when-to-use and when-not-to-use, per section 19.

**Five of the six are now on a screen and have been looked at**, on the project home. `ReferenceLine` has not: nothing has a reference number to render, which is the owner decision in #303.

### 2.3 The long road project home is built and seen

**`ProjectHomeScreen`, #278**, the grid's screen 05. The three answers in order, then steps and papers folded and counted. Verified on the phone at both themes, at font scale 2.0, and in Arabic. Screenshots are `project-home-dark`, `project-home-light`, `project-steps`, `project-max-font`, `project-arabic`.

**`ProjectDetailScreen` is superseded and frozen**, with its row in `docs/REMOVAL-LEDGER.md`. It carried real work the new screen does not do yet: adding, editing, moving and removing a step, setting the status and what the project is waiting on, and saving as a template. **None of it is lost and none of it is bridged by extending the frozen file**; it belongs to the busy stretch and the setup screen, #280 and #291.

**Two defects were found by looking at it rather than by reading it.** The countdown at 22sp beat the holder at 18sp, so a project whose whole shape is that it leads with where it stands was led by its date; `DateRow` gained `prominent` and only the closing window draws the number large. And the step checkbox was a glyph in a fixed box, so at font scale 2.0 the tick was cut in half and read as a damaged control, on the setting this audience is most likely to be using. It is drawn now. **Neither is visible at font scale 1.0 in English.**

**A third was found in the semantics tree and is invisible in any screenshot.** `Bidi.join` isolates every part it is given, so passing it a part that was already isolated nests the marks, and passing it a joined string nests them three deep. The rule is: **raw parts into `Bidi.join`, never pre-isolated ones.**

**Where a project stands can be recorded now**, #281. The standing card carries an outlined "Update where it stands", and the none-yet rung carries the same one, so a project nobody has said anything about names its one action rather than only saying nothing is known. `docs/screenshots/project-standing-sheet-light.png`.

- **A one-stage sheet**, because this is what somebody does in a corridor right after a call.
- **The chips are the project's own history**, the holders it has actually been with, most recent first. There is no project people roster to draw from, and asking somebody to fill one in first would be the app charging admission for a sentence.
- **Since when is today and is not asked for.** A person recording this five minutes after the call knows when it happened. It stays editable from the entry, rule 17.
- **Partial saves.** Somebody who knows only that it is with the county can write that and nothing else.
- Proved on the phone: saved, and the card's date became today.

**A project date can be written down now**, with where it came from on the same sheet. `docs/screenshots/project-date-sheet-light.png`. The kind is chips from the project's own date kinds and free text besides; the date carries whatever precision the person gave it; the source may wait, because somebody who has a deadline but has not written down which letter it came off should not lose the deadline over it.

**The action is offered whether or not there is already a date.** It appeared only on the empty state at first, and a project with a filing deadline still gets a hearing date: an action that shows only on an empty screen is one nobody finds twice.

**D113 was seen working end to end.** Writing an August date onto a project whose only date was in October moved the card to the August one, because the screen leads with the soonest that has not passed and nothing had to be told which date mattered.

**A call can be logged from inside the project**, which is what makes the third answer writable, and it writes the entry and the link together. `docs/screenshots/project-log-call-light.png` and `project-after-call-light.png`. **This closes the app-side half of #303**: the read path existed and nothing wrote the link, so on a real notebook the latest word was permanently absent. **The reference number half of #303 is still the owner's**, and `ReferenceLine` still has never rendered with real data.

- **Pre-answered with the project**, and the project's name is shown back so nobody logs a call against the wrong process.
- **The attribution names who said it**, not only when. The date alone is the half a person does not need when they call back and are asked who they spoke to.
- Proved on the phone: logged a call, and the latest word card became it, dated today, with "Denise, intake caseworker" beside the date.

**The nested isolate defect appeared a third time** and was fixed at the call site again, not at the source. **Making `Bidi.isolate` idempotent was tried and reverted**: a joined run like `⁨a⁩ · ⁨b⁩` also starts with the isolate and ends with the pop, so the guard would have stopped wrapping compound runs as a unit, which changes how they lay out inside an RTL sentence. That is a real behavior change across every screen and it was not verifiable in one sitting. **The rule stays: raw parts into `Bidi.join`, never something already joined.**

**The project's setup screen is built and seen**, #291, and it is law 5 made concrete: the shape as three chips, what the template decided with what it has become, and the status. `docs/screenshots/project-setup-light.png` and `project-shape-changed-light.png`.

- **Changing the shape reorders the project home with no penalty**, proved by tapping a chip and watching the countdown take the lead. There is no confirmation, because asking whether somebody is sure would invent a consequence that does not exist.
- **Status is back**, which the frozen `ProjectDetailScreen` used to carry, and **the waiting-on note is left alone** when it changes: the old screen cleared it on every status change, which threw away who somebody had been told to wait for.
- **The way back names the project, not the projects list.** It said the list at first, which is the small lie somebody only notices by being surprised.
- **The four rows are read-only summaries.** Editing stages, steps, papers and date kinds one by one is not built, and the grid draws chevrons on them.

**The road can advance now**, #285's sheet. `moveProjectToStage` had existed since the schema landed and nothing called it, so a project sat on whatever stage it started on forever. `docs/screenshots/project-stage-sheet-light.png` and `project-road-turned-light.png`.

- **Every stage is offered, not only the next one.** These processes skip stages and go backward, and a control that only moves forward one step is wrong the first time something unusual happens, which on these processes is most of the time.
- **A stage already reached keeps its first arrival date**, so a road that turns back does not erase that it had ever been there.
- **The current stage is derived from what has been reached**, the same way the road strip derives it, so the sheet and the strip cannot disagree.
- **The road strip stays bare**, 20.6. The control sits beside it rather than making the waypoints tappable, which would turn an information graphic into a picker.
- Proved on the phone: moved a project to Decision and watched two waypoints fill and the reader say "Stage 3 of 3".

**Every piece of project data can now be written from the app**: where it stands, a date with its source, the latest word with its link, the shape, the status, and the road. **What is still read-only** is the stages, steps, papers and date kinds as lists, which is #291's remainder.

**The project shows everything said about it**, #284's fold: "What was said" with a count, opening onto every linked entry most recent first, each a door. **Rule 18 is satisfied both ways** for the project-to-entry link now; before this a person who logged six calls could see one of them.

**#306 was fixed rather than filed and left.** `AppLanguageTest` asserted Japanese falls back to English and twice got Spanish, which is what the test before it had chosen: setting `applicationLocales` is asynchronous and the next line read whatever was still in place. It now waits for the system to report the language it asked for, **and waits for the request rather than for the answer**, because polling until the expected result appears would make the assertion prove itself. Three runs of the class and a full suite, clean.

**#302 is still open**, and it is the other flaky one: `BackJourneyTest` failed once in a full suite and passed the next run.

**The review this owes under rule 12 is #304**, and it lists what was deliberately left out and the three things I am unsure about.

**All three shapes are built and all three have been seen**, #278, #279 and #280's home screens. **The shape is only the order of the same components**, which is 20.3's whole claim, so `Repository.Project` carries `lead` and the screen orders itself from it. The long road opens with where it stands, the closing window with the countdown at `monoL`, the busy stretch with the steps cluster already open above the answers. `ProjectHomeScreenTest` asserts the vertical order for each, because every shape shows the same four things and a wrong order still looks like a finished screen. Screenshots: `project-home-*`, `project-window-dark`, `project-busy-dark`.

**The fixture's project content is matched to its projects now.** The stages, date kinds, papers, standing entries and office words were each written once in a generic order and indexed by project position, which put an appeal's stages and an appeal deadline on a power of attorney. **A fixture that puts the wrong words on a screen makes a correct screen look broken**, and every one of those was found by opening it rather than by reading it.

**The first three projects cover all three shapes on purpose.** `PROJECT_STATES` puts the fourth and fifth in done and abandoned, which fold away, so a shape assigned to one of those is a shape nobody looking at the fixture will ever open.

**The busy stretch clusters its steps by area**, which closes #280. Each area is a mono eyebrow with a count of how many steps are in it and a hairline out to the end edge, the same `GroupHeaderText` the trail heads its months with, and a step nobody has filed keeps its place in a run after the named areas rather than being hidden until it is tidy.

**The count says how many steps are there, not how many are done.** The grid draws `1 OF 3`, which is a completion count, and rule 13 rules that out in its own words. The screen shows the plain count instead and **`DECISIONS.md` D116 puts the difference in front of the owner**, because the grid supersedes `DESIGN.md` for this surface and rule 13 is a hard rule that the adoption did not name. One line in `ProjectHomeScreen` changes it if he meant the drawing literally.

**Two fixture defects came out of building it, both the same shape as one already fixed.** Four hospital discharge areas were handed out round robin to any steps led project, so "The house", "The ride" and "Equipment" sat over three steps of a power of attorney with one row under each. Areas are indexed alongside the steps now. And a steps led project generated two to six steps, so **the busy stretch was never busy and the clustering had nothing to cluster**: it generates seven to ten. Both were invisible until the screen was opened.

**358 instrumented tests pass**, up from 356. Both new assertions were proved by breaking the screen on purpose and putting it back from a scratchpad copy, per section 7. Seen on the phone at both themes, at font scale 2.0, and in Arabic: `project-clusters-light`, `project-clusters-dark`, `project-clusters-2x-dark`, `project-clusters-rtl-dark`.

### 2.32 The road turns on the date it actually turned

**#285's sheet was built and had never been opened.** It said "the date is today unless you change it" and nothing on it let you change it: tapping a stage recorded today and there was no way to say otherwise. Somebody writing down on Thursday that the letter came on Monday could not.

The sheet carries the date now, defaulting to today so the common case stays one tap, and `moveProjectToStage` is given what the sheet says rather than what the clock says. Rule 17: a date is the person's and never falsely precise. Seen on the phone: the road advanced to Decision on the chosen date and the stages before it kept the dates they already had.

### 2.33 The starting steps can be changed again

**Part of #291.** `addProjectStep`, `updateProjectStep`, `moveProjectStep` and `deleteProjectStep` have been in the repository since Phase 0 with nothing reachable calling them since `ProjectDetailScreen` was superseded. The setup screen said everything the template decided was changeable and offered no way to change any of it, which is the promise without the thing.

- **`ProjectStepsScreen` is behind setup's Starting steps row**, which is a door with a chevron now rather than a line, the way 20.5 screen 18 draws it.
- **`StepEditSheet` is what a row opens**, carrying the text, the note, move earlier, move later and remove. **One sheet rather than three controls per row**: three targets repeated down a list fails section 9 at font scale 1.0 and falls apart at 2.0. A control that would do nothing is not drawn, so the first step is offered no way to move earlier.
- **The list clusters by area the same way the project does**, so the list somebody edits is the list they read.
- **`project.step.handled_by` said "Me is handling this".** It was already wrong in the spoken description on the project home and the editor put it on screen where it could be seen. It is "Handled by {who}" in all four catalogs now.
- **The way back says "Back to setup"**, because that is where it goes.

**Still owed on #291:** stages, usual papers and date kinds are still read-only lines.

**The stages are done, and the road is editable.** `ProjectRoadScreen` sits behind setup's Stages row, which is a door now. It draws the same `RoadStrip` the project draws, above the list it edits, so what somebody changes is the thing they see rather than an abstract list that turns into it. Each stage opens `StageEditSheet`: rename, move earlier, move later, remove, the same shape as a step's sheet because it is the same job.

- **Removal was the part that needed thinking about.** `RoadStrip` works out where a project is from the stages themselves, so a project left pointing at a removed stage draws as having reached nothing and the road says the application was never filed. Removal moves the project back to the last stage before it that was actually reached, in the same transaction, and to no stage where there is none. `RoadEditTest` covers it against a real database.
- **Renaming keeps the arrival**, and the sheet says so on a stage the project has reached rather than leaving somebody to guess what they are about to lose.
- **One stage is a list and not a road.** `RoadStrip` needs two, so a project down to one draws the list alone.
- **The field and the button no longer say the same words.** Both the steps and the road screens had "Add a step" and "Add a stage" on the field label and on the control that acts on it, which is two nodes saying one thing to a reader. The field names what you type, the button is the verb.

**The date kinds are done too.** `ProjectDateKindsScreen` sits behind setup's Date kinds row, with `DateKindEditSheet` for rename and remove. These are the chips somebody taps when they write a date down, and a template that offered "Renewal" to a process that never renews left a chip in the way forever.

- **The list is what is offered next time and never a key into the record.** `project_date.kind` is the words the person used when they wrote the date down, copied at that moment, so renaming a kind does not reach back and rewrite what they recorded and removing one does not take the date with it. **The sheet says so**, because otherwise the only way to find out is to try it.
- **No reordering here**, unlike the road and the steps. A handful of labels offered as chips has no order somebody reads down, and a control that exists because the neighboring screen has one is decoration.
- **`projectDateKindRows` reads alongside `projectDateKinds` rather than replacing it**, so the chips keep taking labels alone and a caller that only offers them does not have to know they have identities.
- Verified end to end on the phone: removing a kind on this screen removes the chip from the date sheet.

**The usual papers are done, which closes #291.** `ProjectPapersScreen` sits behind setup's Usual papers row, with `PaperEditSheet` for rename, empty and remove.

- **A placeholder is a place, not a paper.** An empty one says "Waiting", never "missing", and **nothing on the screen counts the empty ones or chases them**: six placeholders with a count of how many are still unfilled is rule 13 pointed at somebody who is waiting on other people's post.
- **Emptying and removing are separate, and neither touches the document.** Taking the wrong paper out of the right place is the common mistake and must not require destroying the place; removing the place is a decision about how the project is organized. The document stays in the notebook either way, and the sheet says so, because the thing at risk is a photograph of a letter somebody may not be able to get again.
- **Emptying is not offered on a place with nothing in it**, so the sheet never draws a control that would do nothing.

**388 instrumented tests pass**, up from 373. Seen at both themes, at font scale 2.0 and in Arabic: `project-road-light`, `project-road-dark`, `project-road-2x-dark`, `project-road-rtl-dark`, `stage-edit-light`.

**373 instrumented tests pass**, up from 365. Seen at both themes, at font scale 2.0 and in Arabic: `project-steps-light`, `project-steps-dark`, `project-steps-2x-dark`, `project-steps-rtl-dark`, `step-edit-light`.

### 2.51 The reader was turned on, and it settled half of #231

**Rule 19's exception was used properly**: the three accessibility values were written to disk before anything changed, TalkBack was enabled beside the existing KDE Connect service, and every value was read back afterward. `dumpsys accessibility` shows TalkBack unbound and KDE Connect the only bound service, which is where it started.

**Confirmed with it running:** each card is one focusable node rather than a cluster, and activating one opens the thing and removes nothing.

**Not confirmed, and it is the half the issue is about:** the announced words. **TalkBack does not log utterances to logcat at any tag that could be raised**, `dumpsys accessibility` exposes window and focus state but not the focused node's action labels, and `uiautomator dump` does not serialize `AccessibilityAction` labels at all. Three approaches, none of which produced the string, so the three-attempts rule ended it.

**The label is asserted by `OpenNotRemoveTest` against the semantics tree, which is where TalkBack reads it from.** That is good reason to believe it is right and it is not the same as hearing it, so **#231 stays open**. A person with the phone settles it in a minute.

**Worth knowing before anybody tries again**: this is a real limit of driving a screen reader over adb, not a missing trick. Anything whose acceptance is "what the reader says" needs an ear in the room.

### 2.50 Restore replaces `app_meta`, which is worse than #307 says

**#307 was confirmed on the phone twice** during this run, and tracing it found a second defect in the same line of code. **#320 is new and release-blocking.**

The chain: `ExportContainer.PLATFORM_TABLES` skips only `android_metadata`, so **`app_meta` travels in the archive**; `Backup.restore` copies the archive's database over the live path rather than merging; and `HealthTrailDatabase.ensureDeviceId` keeps whatever `device_id` it finds.

- **#307 is the cosmetic half**: `disclaimer_accepted` lives in `app_meta`, so a restore sends the person back through "Before you start" on a notebook they have used for months.
- **#320 is the other half**: `device_id` lives there too, so **a restored phone stamps every row and every change log entry with the source phone's identity**, and the `device` table's `is_self` row names the wrong device. Silent until sync exists, by which point every post-restore row is mislabeled.

**They are one fix**, in whatever preserves or re-stamps `app_meta` across the replacement. **Neither was changed**: #307 touches the first screen the app ever shows and #320 touches the recovery path, and both want an owner decision on direction. **#320's chain is read out of the source and was not observed on the device**, which is said on the issue.

### 2.49 The fixture computed its instants in the wrong timezone, and CI caught it

**The check added for #233 failed on the very next push**, which is the check working. `Fixture.ms` built a naive `datetime` and called `.timestamp()`, which resolves in **whatever zone the generator is running in**, while every row it writes carries `"America/New_York"`.

**On this laptop the two agreed and nothing showed. Continuous integration runs in UTC**, so a `2021-08-10T10:00` appointment got an instant reading 06:00 in the zone it claims, and 1658 rows were wrong.

- **`ms` resolves in `America/New_York` now**, which is what the rows say.
- **Checked under three zones**, `TZ=UTC` and `TZ=Asia/Tokyo` as well as this machine's, so it is zone independent rather than right by coincidence.
- **Every timestamp in every fixture moved**, which is why the suite was run again after it.

**This is the first time a check written in this repository has failed on real infrastructure for a reason the author could not see locally**, and it is the argument for CI in one line. It is also the fifth fixture defect this run.

### 2.48 Eight places at once, and #219 is closed

A chapter is current exactly when it has no end date, which is the right rule: the place somebody has not left is where they are. **The fixture left every chapter open**, so the chapters screen said "where they are now" and listed eight buildings, each ringed with a gold milestone waypoint. `DESIGN.md` 5.2.1: a milestone is rare by design, and **if everything is ringed nothing is**.

- **Every chapter but the last one ends now**, on the day the next one starts.
- **`check_fixtures.py` holds it**: exactly one chapter has no end date.
- Seen on the phone: "Where they are now" is one place, and the rest fold under "Other places".

### 2.47 An appointment's date and its instant disagreed, and #233 is closed

The fixture wrote a day precision EDTF beside a 10am instant, which is **a row the app itself could never produce**: `Repository.dateColumns` derives the columns from `Edtf.resolve`, and a day gets midnight to one millisecond before the next.

- **It is a moment now**, `2026-11-27T10:00`, with `scheduled_start` and `scheduled_end` both the instant, which is what `Precision.MOMENT` resolves to. The old hour long end was not a shape the app writes either.
- **Seen on the phone**: the appointment reads "November 27, 2026 at 10:00 AM". It used to say only the date while carrying a time nobody could see, and the appointments screen splits upcoming from past on that instant, so one on today's date flipped from "coming up" to "already happened" at 10am.
- **`check_fixtures.py` holds it now.** Every EDTF in `appointment` and `entry` must agree with the columns derived from it: a day starts at midnight, a moment's instant is the minute its text names, and a moment's end is its start. **Proved by putting #233 back on purpose**, watching the check name 45 rows, and restoring from a scratchpad copy.

**A fixture whose rows the app cannot produce cannot exercise the path the app takes**, which is the whole point of one, and this is the fourth defect of that family this run.

### 2.46 Text reaches the database in NFC now, and #227 is closed

**Release-blocking, and it was true of every string the app had ever stored.** `contract/DATA-CONTRACT.md` 8.4: "a name typed with a combining accent on one device and a precomposed character on another is the same person, not two." **Nothing normalized anything.**

- **`Repository` has one write path now.** Two private extensions, `SQLiteDatabase.write`, and **all fifty-five `execSQL` call sites go through them**, so every string argument of every insert and every one of the fifty-two updates is normalized. Doing it at `insertRow` alone would have covered the inserts and left the updates.
- **`data/Text.nfc` is the whole of it**, and an already-normal string is returned unchanged rather than copied, which is what almost every string is.
- **It changes the encoding of a character and nothing else.** No trimming, no casing, no collapsing of spaces; a test holds that.
- **`NormalizationTest` is 7 tests** and covers the three scripts the contract names. **One of them asserts the premise**, that the two spellings really are different strings of different lengths, so the rest cannot pass vacuously.
- **Proved by breaking it**: `Text.nfc` was made the identity function, four of the seven failed, and the file was restored from a scratchpad copy.

**What this does not do.** It normalizes on the way in. **Text already in a notebook written before today stays as it was typed**, so an old row with a decomposed name still misses a search for the composed one. Fixing that is a migration over every text column and it is not written: **that is worth deciding before v1 ships**, and it is the one part of 8.4 still outstanding.

### 2.45 Six cards told a reader that tapping them removed the thing

**#231, and it is not closed**: the code half is done and the gate it names is not.

`removableByLongPress` was the only modifier the app had for a tappable card, so every screen that needed a card to open something reached for it and passed `onLongPress = {}`. **The gesture went quiet and the words did not**: a reader announced the tap as "remove" on a card that opens an entry, and listed a long press called "remove" that ran an empty function.

**Six sites, not the five the issue lists.** `PersonScreen`, `ThreadScreen`, `ChapterScreen`, `CareThreadsScreen`, `MedicationScreen`, **and `ChaptersScreen`**, which the issue did not name and which the criterion "no caller passes an empty `onLongPress` anywhere" is what found. All six use `openableByTap` with a label that is a verb. `grep -rn "onLongPress = {}"` over live code returns nothing.

**The other `removableByLongPress` callers are left alone on purpose.** They pass a real `onRemove`, so the long press does something; that removal being long-press-only is **#218** and a different question.

**`OpenNotRemoveTest` asserts the click label and the absence of the long press**, because none of this is visible: a screenshot of the fixed screen and the broken one are the same image, which is how it survived six screens and a design review.

**What is left on #231 is its own third criterion**: "Verified with the reader on, per rule 19, not by reading the code." **That was not done**, and the issue stays open for it. Turning TalkBack on is rule 19's sanctioned exception and it was not worth starting at five in the morning at the end of a long run with a handoff owed. **It is the cheapest thing on the board for the next session**: turn the reader on, walk one of the six cards, restore per section 10.

### 2.44 Nine Arabic plurals were rendering the wrong form, and nothing could see it

**#318, filed and closed in the same run.** Building #287 put "1 مكالمات" on the phone, which is "1 calls". The string carried only `other`, which is well formed ICU and agrees with English, so **every one of the seventeen checks passed**.

**`check_i18n.py` compares plural categories now.** `en` and `es` need `one` and `other`, `zh` needs only `other`, `ar` needs all six. **It found nine more, all real**, in the countdown, the road, the papers count, the preview and Today's digest: `71 يوم` where Arabic wants `71 يومًا`. All nine are corrected.

**Two things it deliberately does not report**, and both were found by the first version reporting them:

- **A plural whose branches never interpolate `#`** is a state switch wearing plural syntax. "Nothing on it yet" against "Filled in" reads the same at one and at seven, and demanding a `one` branch would make a translator write the same words twice.
- **An explicit `=0` satisfies `zero`.** ICU matches explicit values before categories, and this catalog uses `=0` widely on purpose.

**Proved by breaking it**: the dual was stripped from one Arabic plural, the check failed naming that key, and the file was restored from a scratchpad copy rather than with git.

**This matters most for what has not been built yet.** #92 through #99 add ten more languages, each with its own category set, and adding them without this check means adding them wrong and not finding out.

### 2.43 The people a project has involved, and the cross-project door

**#287, screen 14.** `ProjectPeopleScreen` sits behind a new People row on the project home.

- **The project's own contacts, not the care team.** The two overlap and are not the same list; showing the whole team under a Medicaid application would bury the two caseworkers in a list of nurses. **The care team is a door at the bottom**, with a line saying which list is which.
- **Derived, and nothing writes anything.** Somebody is on a project because they are named on an entry linked to it. **There is no project-to-person table and this does not add one.**
- **The "also in" row is the cross-project door**, which screen 14 calls the one new navigation idea on this surface. It swaps the project underneath rather than stacking a second one.
- **The count is how many times somebody turned up, never a score**, and nothing is colored by how long it has been.
- **Two queries, not one per person.** A project with nine contacts would otherwise cost ten round trips to draw one screen.

**The fixture could never show this screen, which is the #229 and #237 pattern for the fourth time.** The people pass runs over calls before `connect_entries_to_projects` creates the office calls, so **a project's entries named nobody** and People was always 0. The fixture now writes `PROJECT_CONTACTS`, office people rather than ward nurses, one per project, **with one shared by exactly two projects** so the cross-project door has something to point at. Putting her on all five gave one person four "also in" rows, which turns the one new idea on that screen into wallpaper: seen on the phone, not reasoned.

**Two things were found by looking at it.**

1. **The "also in" card was the same white row as the person above it** and read as another person. It carries the gold waypoint the entry screen already uses to mean a project.
2. **The Arabic plurals I added were wrong**, "1 مكالمات". The catalog uses the full `zero`/`one`/`two`/`few`/`many`/`other` categories everywhere else and my additions used only `other`. **Seven strings were corrected**, across the trail gaps, the paper count and the people counts. **`check_i18n.py` holds the four catalogs to each other and does not compare plural categories**, so nothing caught it: worth a checker, and it is the reason to read an Arabic screen rather than trust the catalog.

**446 instrumented tests pass**, up from 440. `ProjectPeopleTest` is 6 tests. Seen at both themes, at font scale 2.0 and in Arabic: `project-people-light`, `project-people-dark`, `project-people-2x-dark`, `project-people-rtl-dark`.

### 2.42 The papers of a project, as paper

**#286, screen 13.** `ProjectPaperworkScreen` sits behind the project home's Papers row, which is a door now like the trail. The papers fold listed the places and their two states; this shows the paper.

- **What they sent and what you sent**, which is the one distinction that matters in any process and which `project_paper.direction` has carried the whole time. Chips for the two the grid draws, only when the project uses both.
- **A grid of thumbnails or a list**, toggled. **The toggle is remembered for rotation and the back stack and not across a restart**: view preferences have no table and that is **#222**, blocked on the owner, and storing it anywhere else would be inventing the table that issue is about.
- **A placeholder is a place**, so an empty one says "Waiting" at the same size as a full one, and **nothing counts the empty ones**. The header says how many places there are.
- **Both ways with Documents**, which is the acceptance criterion: a filled place opens the document, and the document now carries a "Filed as" row naming the project **and the place**, `Repository.filingsForDocument`.

**Four defects came out of looking at it, all fixed here.**

1. **The filled tile had a raised white panel the empty ones did not**, because `openableByTap` rests on the card surface. On the phone that read as two different components and made "Waiting" look like an afterthought, which is the opposite of what 20.4 says a placeholder is. Transparent at rest now.
2. **The tile led with the document's title and hid the place's own name.** Somebody looking for "Proof of income" found a tile called "Discharge summary" with no way to tell it was the same place. The place leads; what is in it says so underneath, and on a tile the photograph is that answer.
3. **The tile line truncated at font scale 2.0**, "May 12," with the year cut off. Neither tile line is capped now: an uneven tile is the honest shape, which is the same call `RoadStrip` makes.
4. **The document's way back said "Back to documents" while going to the project's papers**, the third instance of that small lie this run.

**`verify.sh` earned its keep again**: `lintDebug` failed on `ModifierParameter`, because the two new optional parameters landed before `modifier`. Everything else was green and it had already been walked on the phone.

**Not built, and named rather than skipped:** camera-first capture from this screen, and the "Older" fold the grid draws for a project with many papers. **#57 is stale and should be read before it is worked on**: attachment storage exists and is proved, `Attachments.put`/`read`, and the export carries it.

**440 instrumented tests pass**, up from 435. `ProjectPaperworkTest` is 5 tests. Seen at both themes, at font scale 2.0 and in Arabic: `project-paperwork-light`, `project-paperwork-dark`, `project-paperwork-2x-dark`, `project-paperwork-rtl-dark`, `project-paperwork-list-light`.

### 2.41 A project has its own trail now, and it reads forwards

**#284, screen 11.** The project home had a "What was said" fold listing the linked entries. A project's trail is those **and** the road turning **and** the dates it is running against, on one spine, which is what the grid draws and what a person actually wants when they ask what has happened.

- **`Repository.projectTrail` merges three sources**, `entriesAbout`, the reached stages and the project dates, into one date-ordered list of `ProjectTrailItem`. The screen does no lookups and no arithmetic on it.
- **Oldest first, the opposite of the main trail.** The trail answers "what happened lately"; a process is read forward. **The gap markers say "3 weeks pass", not "3 weeks earlier"**: `trail.gap.*` is written for a list read backwards and would have been plainly wrong here, so `project.trail.gap.*` is its forward twin.
- **A stage nobody has reached is not on it.** It has no date, so it has no place on something ordered by date, and putting it at the end would say it happened last rather than not at all.
- **Dates that have not arrived are on it and are not marked as anything.** Nothing is late and nothing is missed, rule 2 and 20.7.
- **The fold became a door**, keeping the fold row's own shape the way Setup does, and **it is drawn even at zero**: a door that appears only once there is something behind it is one nobody learns about, which is the same arithmetic that put "Write down a date" on a project that already has one.
- **The filter chips are only the kinds the project actually has**, so there is never a chip with nothing behind it, and they sit in a fixed order so they do not reshuffle as a project grows.

**Three things worth knowing, all found while building it.**

1. **`filterKeyFor` had to be a closed set.** The chip label is a computed key, `project.trail.filter.{kind}`, which is exactly the shape `check_string_keys.py` cannot see, and `entry.kind` allows `transfer` and `milestone` beyond the six the catalog names. An open set was a crash on opening the trail of a project holding a transfer. It mirrors `kindNameKey`'s own fallback, and **`ProjectTrailChipsTest` holds every kind the schema allows against all four catalogs**.
2. **The chips needed plural labels.** `entry.kind.call` is "A call", which is right over one row and reads as a mislabeled control on a chip that selects all of them.
3. **The empty state was written as a gray paragraph under the subtitle**, which is the exact shape #274 already had to fix once and which reads as a screen that failed to load. It uses `SectionEmpty` with the ground, a lead and the paragraph now, at `EMPTY_HEIGHT_TALL`.

**What is deliberately not built:** the scrubber and the scoped search, which the issue itself says arrive as it grows, and the reference number, which is **#303**.

**435 instrumented tests pass**, up from 427. `ProjectTrailTest` is 6 tests and `ProjectTrailChipsTest` 2. Seen at both themes, at font scale 2.0, in Arabic, and empty on a project started for the purpose: `project-trail-light`, `project-trail-dark`, `project-trail-2x-dark`, `project-trail-rtl-dark`, `project-trail-empty-light`.

### 2.40 An entry could be reached from its project and had no way back to it

**#283, and it is the last one way link on the entry screen.** `entriesAbout` let a project list every entry connected to it and logging a call from inside a project wrote that connection, but `EntryDetail` carried no project at all. A call logged against a Medicaid application opened onto a screen with no Medicaid application on it: rule 18's dead end wearing a disguise.

- **`EntryDetail.projects` is a list**, because 8.1's `link` table does not stop an entry being about two projects and somebody who rings one office about two applications has done exactly that.
- **Both link directions are read**, the same way `entriesAbout` and `latestWordFor` do. Nothing in the app writes the project-as-source direction; an import can. **`insertLinkForTest` exists to produce that state**, named like `tombstoneForTest` so nobody reaches for it by accident, and without it half of every both-ways reader in this file is untested and keeps passing.
- **A removed project stops being a door.** Deletion is a tombstone, so the row is still there to join against, and `live_project` is what keeps it off the screen.
- **The project sits with "what it is about", not at the top** where screen 10 draws it. Screen 10 draws an entry reached from its project; this screen is reached from the trail and from search far more often, and people-first still holds.

**Two things were found by tapping rather than reading, and both are fixed.**

1. **The way back said "Back to the notebook" and went to the project.** The same small lie the setup screen told about the projects list. It names the project now.
2. **`Pin this to the top` and `Remove this` were full width**, which `SectionScaffold` uses at the foot of every screen to mean the way back, so the screen ended in three identical full width outlined buttons of which the last was the way out and the middle one removed the thing being read. Both are pills now, D118. **The edit sheets keep their full width remove**: they have no way back to collide with, only Cancel.

**What is deliberately not built**, and neither is invented here: the **reference line** has no column and is **#303**, the owner's call, so `ReferenceLine` still has never rendered with real data; the **paper it produced** needs attachment storage, **#57**; and the call's **duration** exists in `call_detail.duration_minutes` and no screen reads it.

**427 instrumented tests pass**, up from 422. `EntryProjectLinkTest` is 5 tests. Seen at both themes, at font scale 2.0 and in Arabic: `entry-project-door-light`, `entry-project-door-dark`, `entry-project-door-2x-dark`, `entry-project-door-rtl-dark`.

### 2.39 A brand new project's third answer did not say what it was

**Found by starting a real project on the phone**, which is the empty rung of 13.3 for screens 05, 06 and 07 and the one gate on them nobody had walked. The screen was honest everywhere else: "Nobody has said yet", "No date written down yet", each with its one action and none of it framed as a deficiency, rule 13.

**The latest word rung was the bare sentence "Nothing written down from them yet"**, and the filled `LatestWordCard` carries a "The latest word" eyebrow that the empty branch dropped. So on the first screen anybody opens after starting a project, "them" referred to nothing on the screen. 20.1 says this screen answers three questions, and a question is not answered by a sentence that does not name it.

- **The eyebrow stays when the card goes**, in the same `mono` and `goldInk` the card uses.
- **The date rung was deliberately left alone.** "No date written down yet" says what it is about, and the filled `DateRow` has no eyebrow either, so adding one would invent a label the drawn state does not have.
- `ProjectHomeScreenTest` covers it; `latestWord` is already null in every case there, so the empty rung was being rendered by all five existing tests and asserted by none.

**422 instrumented tests pass**, up from 421. `docs/screenshots/project-new-empty-light.png`.

### 2.38 The road ran one way in Arabic and its own stage names ran the other

**`RoadStrip`'s one line fallback concatenated the names by hand**, `stages.joinToString(" · ") { it.name }`, with no `Bidi` handling at all. The waypoints above it mirror because Compose lays them out; a run of Latin names joined by hand does not. **So in Arabic the road ran right to left and its names ran left to right, and the first stage sat at opposite ends of the two.** On the appeal project the current waypoint was at the right edge and its name, "Decision received", was at the left.

- **Every template name the app ships is Latin**, #62, so this was the normal case in Arabic rather than an edge one.
- **The fallback is the four stage case**, and the three stage projects did not show it, which is why it had not been seen: the Medicaid road mirrors correctly and was the one that had been looked at.
- **Fixed with `Bidi.join`**, whose default separator is already this exact string. Raw names in, per section 15.
- **Invisible in English**, where the order is unchanged.
- **`stageNamesLine` is a named internal function rather than an inline expression**, because `RoadStrip` clears its descendants' semantics to speak as one node, so **nothing in the strip's own text is reachable from a test**. Worth knowing before writing another one: the first version of `RoadStripTest` asserted on the rendered text and failed for that reason rather than for the defect.
- **A reader is not short-changed by that clearing**: `roadDescription` names the current stage and its position, so a reader user hears where the project is.

**421 instrumented tests pass**, up from 417. `RoadStripTest` is 4 tests. Seen in Arabic before and after: `project-window-rtl-dark`.

### 2.37 The standing sheet said the date was today unless you changed it, and nothing changed it

**The same defect the stage sheet carried until `ac526b6`, on its sibling sheet on the same screen**, found by opening it during the #281 gate sweep rather than by reading it. `project.standing.lead` has always read "Since when is optional. The date is today unless you change it", and the sheet had no date control at all: `addProjectStanding` already took an `Edtf.Date` and the shell stamped `LocalDate.now()` over whatever the person meant.

- **The sheet carries the date now**, defaulting to today so the common case is still no taps, and it is **labeled "Since when?"** like the two fields above it. Unlabeled it was a bare button showing a date directly above two more full width buttons, which dresses a value as an action.
- **The date does not inherit the previous standing's.** That is when the project last changed hands, which is the one answer almost certainly wrong for the change being recorded now.
- **`savingStanding` is a named `StandingWrite` rather than a Triple with a date bolted on.** Two of its four fields are adjacent free text the person typed, which is the shape that gets passed the wrong way round with nothing to catch it.
- Proved on the phone: picked August 3 while today was August 6, saved, and the standing card read August 3.
- **`StandingSheetTest` is 4 tests and the date assertion was proved by breaking it**, removing the control, watching that one test fail, and restoring from a scratchpad copy rather than with git, per section 7.

**417 instrumented tests pass**, up from 413. Seen at both themes, at font scale 2.0 and in Arabic: `project-standing-sheet-light`, `project-standing-sheet-dark`, `project-standing-sheet-2x-dark`, `project-standing-sheet-rtl-dark`.

**The log a call sheet was checked for the same shape and does not have it**: it claims no date and its save is enabled by the note alone, so rule 13's partial save holds. **It still has no date of its own**, which matters for a call being written up the next morning, and that is left as is rather than widened into here.

### 2.36 The last two things the superseded project screen could do have come back

**#314 is closed.** Saving a project as a template and naming who it is waiting on both went with `ProjectDetailScreen` when it was frozen. Their repository calls and their `NotebookShell` state survived the supersession and **nothing anywhere set them**, which is a shape no screenshot shows and no compiler complains about: the state is read, the effect is written, and the control that would fire it does not exist. Found by reading `docs/REMOVAL-LEDGER.md` against what had actually come back. **The ledger now says all six are back, and it was corrected in the same commit.**

- **Both live on the setup screen** and both are **pills sized to their label**, not full width buttons. The old screen drew them full width; this screen ends in "Back to the project", which is the treatment this app uses to mean the way back, and a column of three identical full width buttons was what the first build produced the moment somebody typed a name. `Update where it stands` and `Log a call` are the pattern. D118.
- **The template action is its own headed section**, "Keep this for the next one". Without a heading it sat directly under an empty text field and read as that field's save button, and its saved state was one gray sentence alone at the foot of the screen. **Both defects were found by looking at it on the phone**, not by reading it.
- **`projects.save_as_template` was corrected in place in all four catalogs.** It said "Save these steps as your own template" and a template has carried the lead, the stages, the steps, the papers and the date kinds since #262.
- **The save is explicit and the control is only drawn when it would do something.** Clearing the field is a save, so somebody who is no longer waiting on the county can say so.
- Proved on the phone end to end: a name saved, survived a force stop and a full restart, and a saved template turned up under YOURS in the library as "Your copy of a template that comes with the app".

**413 instrumented tests pass**, up from 404. Seen at both themes, at font scale 2.0 and in Arabic: `project-setup-template-light`, `project-setup-template-dark`, `project-setup-template-2x-dark`, `project-setup-template-rtl-dark`, `project-setup-waiting-typed-light`.

**Two things were filed rather than built.** Saving twice inserts a second `custom_template` row, so a person who saves in March and again in June gets two identically named templates with no way to tell them apart, which was the frozen screen's behavior too and is now reachable again: **#315**. And the instrumented suite fails a whole class with `RootViewWithoutFocusException` when the notification shade is open on this phone, which reads exactly like a product defect: **#316**.

### 2.34 Starting a project shows what a template is before it creates anything

**#276 and #277 are built and seen.** Choosing a template used to create the project, its road, its steps, its papers and its date chips on one tap of a row, so the first time anybody saw what a template meant was on a project that already existed. **Nothing is created until Create now.**

- **`StartProjectPreviewSheet` is screen 04.** The road drawn rather than counted, then the starting steps, the usual papers and the date kinds each with a line saying what they are and that they can be changed, then which of the three answers the project will open with and where to change it, then the name.
- **Two stages, not three, and both say which they are.** The name is on the preview, pre-filled from the template, because a stage whose only question arrives pre-answered is a tap charged to everybody to serve the few who rename. The picker gained its own `1 OF 2` eyebrow, since the preview was announcing a stage nobody had been shown. `DECISIONS.md` D117.
- **The sixteen templates stay.** The grid ships four built-in bundles; this app has sixteen grouped into the same four kinds. Dropping twelve is an owner decision and this is not it.
- **`RoadStrip` no longer breaks a stage name in half.** At font scale 2.0 with four stages, each label gets about a quarter of the width, which is narrower than the word, and "Gathering" came out as "Gatherin" over "g". Below the width a name needs, the names run as one line under the road instead: nothing dropped, nothing abbreviated. **Measured with a text measurer rather than derived from the font scale**, because the derived threshold looked right and did nothing on the phone. This was one of #304's three uncertainties and it is fixed for the project home too, rule 14.

**Seen at both themes, at font scale 2.0, and in Arabic**: `project-preview-light`, `project-preview-dark`, `project-preview-2x-light`, `project-preview-rtl-light`, plus `project-start-light` for the picker's eyebrow. **365 instrumented tests pass**, up from 362.

### 2.35 The projects tab before the first project

**#274 is built and seen.** It was one gray paragraph under the subtitle with the rest of the screen blank below it, which is exactly the shape 5.17 already solved for every other empty screen in the app. It uses that solution now: the trail map ground, the line that says what this place is for at `displayS`, the paragraph under it, and the one thing to do.

- **`SectionEmpty` grew a lead and an action rather than a second empty state being written.** Seventeen screens call it and none of them changed: both parameters default to null. **Where a section's empty state is one line and a drawing, that is still what it is.**
- **The subtitle goes while the list is empty.** It describes what each row answers, which nothing on an empty screen does, and it opened with the same four words as the empty state's own lead. The screen said "the long processes" twice, one line above the other.
- **The bottom Start button goes too.** The empty state carries the action at the place the eye lands, and the two together were one control drawn twice on a screen with nothing else on it.
- **`EMPTY_HEIGHT_TALL` is 0.82 against the sections' 0.62.** A taller block needs more room to center in: at the section fraction this sat in the upper half with the bottom third blank, which reads as a screen that failed to load. Found by looking at it, twice.
- **No section drawing**, because projects are not one of the twelve. The ground alone is what 5.17 prescribes for a place outside the sections.

**Seen on the phone at both themes, at font scale 2.0, and in Arabic**, from a genuinely empty notebook reached through onboarding rather than by clearing a table: `projects-empty-light`, `projects-empty-dark`, `projects-empty-2x-dark`, `projects-empty-rtl-dark`. **362 instrumented tests pass**, up from 358, and both new guards were proved by breaking them.

### 2.4 The projects list carries the mini road

**`ProjectsScreen` is converted to the grid's screen 2**, #275. Each card is its status, its name, its mini road, and one line answering where it stands and the next date. Screenshot: `docs/screenshots/projects-list-dark.png`.

- **The mini road draws no labels.** Three mono words under a card in a list is noise, and the road's shape already says where the thing is. The reader gets the whole sentence, the same one the project's own screen gives.
- **`Repository.projectCards` is three queries for the whole list**, not three per project. Fifteen projects would otherwise cost forty-five round trips to draw the screen the tab opens on.
- **The next step line only prints when the card would otherwise say nothing.** It was right while a project was a checklist; under the grid a card answers two things, and printing a third under them is three lines competing where the grid draws one.
- **`projects.subtitle` was corrected in place**, in all four catalogs. It described a project as "a list of steps and a note of who you are waiting on", which is the checklist the grid supersedes.

## 2.5 Today has a lead slot and a card field, and it is on the phone

**`TodayFieldScreen`, with `TodayCard` and `CardSize`.** The lead spans the width at the top and the field is a two-column grid under it, with small taking one column and wide and tall taking two. Every card wears its section's hue from the tab pack and carries a corner chevron, and **each one opens the section its answer lives in**, because a door that does nothing on press reads as broken.

**Seen at both themes, at font scale 2.0, and in Arabic.** `docs/screenshots/today-field-dark.png`, `today-field-light.png`, `today-field-max-font.png`, `today-field-arabic.png`. The grid mirrors: the small cards swap sides, the chevrons flip, the capture button moves to the start edge, and the English catalog stays isolated inside the Arabic layout.

**The lead is singular by construction, not by convention.** It comes from `Repository.TodayLayout`, which has nowhere to put zero or two.

**`Repository.todayAnswers` answers every card in one pass**, and each answer is computed under its own guard. That is not tidiness: two of these queries named columns that do not exist, `question.resolved_at` and `milestone.title`, and under the shell's single catch **every card on the surface said "Nothing waiting" at once**, which is the app asserting something false about somebody's record.

**A card with no answer is absent from the map rather than holding an empty one**, and the screen says so differently. Empty means the record has nothing to say; absent means the question was never asked. Filling the gap with an empty answer is what made the digest in the lead slot claim nothing was waiting on a notebook holding 182 entries.

**The digest uses the summary the app already had**, computed once in the shell, rather than a second digest written for this surface.

**All fourteen situations ship a starting hand, #305, and a brand new notebook lands on the new Today.** Verified by clearing the app and walking onboarding: choosing Nursing home produced its eight cards, every one saying "Nothing waiting" without a single zero or scold. `docs/screenshots/today-fresh-dark.png`.

- **A card that names a source is never in a hand**, D115. The grid's home care hand lists a measure; at onboarding there is no measure to point at, so it would render with nothing to answer on the first screen a person ever sees. `check_templates.py` refuses one.
- **Skipping the picker gets `Repository.defaultStartingHand`**, because skipping is a real answer and not a request for an empty screen.
- **A hand is applied only when there is no layout**, so a person whose care setting changes later keeps the desk they arranged.
- **A zero is not rendered.** A large 0 above "Nothing waiting" says the same thing twice, and at that weight it reads as a score on somebody who has just started.

**Edit mode is built and works on the phone**, #271, apart from adding a card. `docs/screenshots/today-editing-light.png`.

- **Entered by a visible Edit button**, per 21.6 screen 5. Touch and hold is not the only path, because it is not a path at all yet.
- **Staged.** Every change is held in the screen and written once, from Done, so a person can move three cards and change their mind about all of them. Cancel discards.
- **Move up, Move down, Remove, and three size chips per card.** Proved on the device: moving a card and tapping Done survives a full app restart.
- **The lead cannot be removed and cannot move up**, so there is never zero.

**Three defects came out of building it, and two of them are accessibility defects a screenshot cannot show.**

1. **The edit controls were unreachable by a screen reader.** `TodayCard` cleared all descendant semantics so it would speak as one node, which is right for a card that is only a door and wrong the moment it holds controls: Move up and Move down are the accessible reorder path 21.6 asks for, and they did not exist for the people who need them most. The card stops speaking as one node while editing. **Found by trying to drive the controls from a semantics dump and finding nothing there.**
2. **"Move Medications down" wrapped to one letter per line** on a half-width card and stretched it to four times its height. The visible word is Up, Down, Remove; the reader still hears which card it moves.
3. **The isolate marks nested again**, in code written four commits after the same defect was fixed on the project screen. `Bidi.join` isolates every part it is given.

**The source-closed rung is built and seen.** A card pointing at a finished or removed project says "Closed. Still here until you remove it." and keeps working as a door. `docs/screenshots/today-source-closed-light.png`. **Closed means finished or removed**, both of which are states the person put the project in, and neither is a reason for the app to take a card off their screen. A project that is gone entirely, which an import can produce, keeps its card too.

**Adding a card is built and seen**, #272. `docs/screenshots/today-gallery-light.png`. Reached from Edit, and each entry says what the card would answer with the thing it points at underneath: **"Where it stands / Appeal the level of care assessment"**, not the other way round. The first version had it inverted and put three rows reading "Appeal the level of care assessment / Project" next to each other, which are three different cards and looked like one listed three times.

- **A card already on Today is not offered again**, except the ones that point at something: a second measure is a different card from the first.
- **Adding writes immediately** rather than being staged like a move, because a person who taps Add expects the card to be there.
- Proved end to end: added the weight card and found it at the end of Today.

**Promoting a card to the lead is built and seen.** It is its own action, per 21.1, because reaching the top by tapping Move up eleven times is not the same offer, and **promoting demotes the card that was there back into the field**. Proved on the phone: promoted the medications card, saved, force stopped the app, and it came back leading with the digest directly under it. `docs/screenshots/today-promoted-light.png`.

**What is left on Today:** Reordering to the top is the same thing today, and `promoteTodayCardToLead` exists and is tested.

**The fallback to the previous Today is still in the shell** and now only fires for a notebook made before this landed. It comes out with #271, and `ProjectDetailScreen`'s ledger row is the model for what goes in `docs/REMOVAL-LEDGER.md` at that moment.

**The latest word can be read but the app cannot write it, and that is #303.** `Repository.latestWordFor` reads it through the `link` table, which is what 8.1's generic connection table is for. **Nothing outside the fixture writes that link**, so on a real notebook the third answer is permanently absent. The fixture writes it so the card can be built and seen, which is deliberately not a fix.

**A reference number has nowhere to live, and that is the owner's call.** The grid draws one on the latest word and calls it first-class; `call_detail` has no column for it. **Nothing was changed**: rule 3, and the WHAT BECOMES DATA amendment does not mention reference numbers. `LatestWordCard` takes it as an optional parameter and draws nothing without it. #303 states the decision.

**One thing found while building them.** The grid draws several small labels in the faintest tone it has, and `ink3` is 2.37:1, which D92 makes non-text. **Those are `ink2` in the app.** A stage the project has not reached still has to be legible: it is where the thing is going.

**353 instrumented tests pass**, up from 349.

**Two checkers were fixed rather than worked around while doing this**, D114, and **#216 is closed by it**.

**Seven things are resolved provisionally, to the drawn default, and are meant to be revisited in one sitting after the owner tests on the phone.** D111 lists them together. **Two template default hands, hospital and rehab, were drafted rather than deferred and are not final.**

---

## 3. What #202 landed

**Both halves are built, swept, tested and closed.** Both themes, font scale 2.0, and Arabic, on the phone. Reviews at **#241** and **#242**, and `DESIGN.md` section 14 carries both rows.

**The picker is converted**: the setting each group leads with keeps its card and its burden line, and the rest are dense rows in a grouped surface. Fourteen cards was three and a half screenfuls on the first screen after the disclaimer.

**Change of situation is new and it had no door at all.** The picker ran once during setup and was then unreachable forever, so a family whose care moved could not tell the app and could not even see which setting they had. It is now a destination in More. The screen states the boundary plainly and offers a chapter, and **the boundary is made rather than only stated**: `moveToChapter` ends the open chapter today and starts the new one today, because starting a second without ending the first left two places somebody was in at once.

**Two things it said that were not true, both found by looking at it:** "Right now" showed the setting the person had just picked, before anything was written, and the chapter field carried a mono header saying the same three words as its own label.

**The picker's rows lost their grouped surface, and that was the component's own rule rather than a compromise.** Section 7: not around a list long enough to scroll, where the rows should be full bleed so the scroll is not a slab moving under a window. It also restored **one lazy item per setting**, which is what the picker's test needs to reach all fourteen by key, and batching a group into one item had broken exactly that. The trap is in section 10 of this file and was walked into anyway.

**The English catalog reordered itself in Arabic**, on a screen nobody had isolated: every sentence's full stop jumped to the front of its last line, ".your own" rather than "your own.". Fixed on the picker. **#226's worklist is the rest of this**, and it is worth taking seriously.

**332 instrumented tests pass**, up from 325. **One flake seen once and not since**: `RoundTripTest.unknownSurvivesAsUnknownRatherThanAsNullOrToday` failed with "attempt to write a readonly database" inside `Backup.recomputeRanges`, then passed on the next run with no change. Not investigated, and recorded here rather than forgotten.

**A missing catalog key crashed the app on opening**, and nothing caught it: the four catalogs agreed with each other, seventeen checks passed, the Kotlin compiled and lint was clean, because nothing compared the literals in the code against the catalog. **`check_string_keys.py` now does**, and it was proved against the real crash rather than assumed.

---

## 4. What #201 landed

**Both screens are converted, swept and closed.** Both themes, font scale 2.0, Arabic, and the search's own empty state, on the phone. Reviews at **#239** and **#240**, `DESIGN.md` section 14 carries both rows, and D104 and D105 carry the two decisions.

**The sixteen project templates gained a `category`**, one of `paying`, `challenge`, `moving`, `papers`, held to that closed set by `check_templates.py` and labeled per locale under `projects.category.*`. **It is what the person is trying to do, not what kind of office it involves**, and it is not `phase`, which is build order and never reaches a screen. `templates/SCHEMA.md` carries the definition. Both screens group by it in the same fixed order, which lives in each file as `CATEGORY_ORDER`.

**Both screens were walls of sixteen cards and are now rows in folds.** The picker leads with the person's own templates, or with the first category when they have none, and searches. The library leads with what has actually produced something, as cards, and folds the rest.

**`DenseRow` gained `subtitleMaxLines`, defaulting to 1.** Every subtitle on the picker ended mid-sentence at one line. **Raise it only where the second line is a sentence somebody reads rather than a tag**; the fixed row height is what makes a long list scannable.

**The fixture's projects carried no `template_id`**, so the library could never show what any template produced, which is the whole reason it is a library rather than a catalog. Three of the five now do, and `check_fixtures.py` holds the ids to the real catalog so renaming a template fails the build rather than producing a project pointing at nothing. Third instance of this shape after #237 and #229.

**325 instrumented tests pass**, up from 313: `StartProjectScreenTest` covers the grouping and the search, and `ScreenReaderTest` walks both screens.

**The catalog is still English inside an Arabic layout**, which is #62 and not new. Every template name and subtitle now goes through `Bidi.isolate` so it cannot reorder against the layout, which is a patch over that rather than a fix for it.

---

## 5. What #200 landed, and the four issues that came out of it

**Both halves are built, swept, tested and logged.** This section is here because the next session inherits the decisions rather than the work.

**The arc.** The milestone reader and writer, `MilestonesScreen`, `AddMilestoneScreen`, the door from the chapters list, and the shell wiring. All verified on the device including the parts a previous session could only compile: **marking a milestone by hand and choosing a chapter makes the chapter door appear on its row, and the chapter's own "What was worth marking" fold shows it back.** Rule 18 holds in both directions, seen rather than asserted. `docs/screenshots/milestones-v4-light.png`.

**The fixture still cannot exercise the chapter link on its own**: generated milestones carry no `chapter_id`, so a walk that needs the door has to mark one by hand first. That is **#237**.

**Month review**, `MonthReviewScreen`, reached from the trail's own month heading, which now carries a chevron. Hero is the month's milestones and nothing else, then where they were, appointments, what went wrong, what was answered, paperwork, and a fold holding everything written down. One filled action, which shares the month as a document through `Readable.monthReview`. `docs/screenshots/review-light.png`.

**Two defects were found by looking at it and are fixed:** a place that began and ended in one month listed its name twice, and an incident reported and answered in one month listed twice under two headings. Both now read as one row. The gold total band was built and removed the same day, for the reason in `DESIGN.md` section 14.

**The sweep is done and both screens passed it.** Both themes, font scale 2.0, and Arabic, on the device. `docs/screenshots/` holds `milestones-v4-light`, `milestones-v4-dark`, `milestones-arabic-dark`, `milestones-max-font-dark`, `review-light`, `review-dark`, `review-arabic-dark`, `review-max-font-dark`. Nothing clipped at 2.0, the last item clears in both, the trail mirrors with the spine on the start edge, and the person's own words stay isolated in Arabic.

**313 instrumented tests pass**, up from 297: `MonthReviewTest` covers the boundary rules, and `ScreenReaderTest` now walks the arc and the review.

**Rule 12 is discharged for both.** The arc's review is **#235** and the month review's is **#236**, each with its device screenshots, what it was composed from, what was deliberately not invented, and what I was unsure about. `DESIGN.md` section 14 carries both rows.

**Two things were found and filed rather than built:** the fixture never gives a milestone a chapter, **#237**, and `milestone.measure_id` is a schema link nothing reads, **#238**, which needs the owner's decision because expressing it at all comes close to interpreting a measurement.

**The device holds one extra milestone**, "Sat up for the whole visit", written by hand to exercise the chapter link. `tools/seed.sh` clears it.

---


## 6. The day milestone 1 finished, 2026-08-09

**Twenty-two issues closed.** Milestone 1, Today, went from fourteen open to none: #258, #259, #260, #271, #272, #273, #293, #294, #295, #296, #297, #298, #300, #301 and the parent #243. Milestone 2 went from thirteen to four, all of those blocked: #263 through #267, #275, #289, #290 and #315.

**Every one was verified on the phone** at both themes, font scale 2.0 and Arabic right to left, and every screenshot was looked at before it was committed.

### What the screens showed that the code could not

- **A phone number with a space in it never reached the dialer.** `Uri.fromParts` escapes nothing, so `tel:555 0142` opened an empty keypad while `tel:555%200142` filled it in. **That was every number in the app**, not one card: the care team screen and the emergency card share the helper, so the one tap this app promises had been landing on a blank screen for as long as dialing existed. Proved by starting the same intent three ways from `adb` rather than by reading the code, which looked correct.
- **A control drawn inside a Today card was reachable by finger and by no reader at all**, because the card cleared its whole subtree in order to speak as one sentence. The card clears its answer only now and carries its one inline action in a slot beside it.
- **The add-a-card gallery previewed "Nothing waiting" for every entry**, whatever the record held, because the previews were looked up from the answers of the cards already on Today and the gallery only ever offers the ones that are not. The lookup could never hit.
- **A chart promoted to the lead could not draw a chart.** The lead left `tall` at its default, so grid screen 02, which is a chart at the lead, could not exist.
- **The gold FAB sat on top of the Start one button** at font scale 2.0. `fabSafeActionBar` is the modifier D81 exists for and the projects list was not using it.
- **Edit mode carried about a hundred and forty controls** on a twenty card Today: three size chips and four named actions on every card. The grid says a card carries a remove dot and a drag handle and everything else lives in its options sheet.
- **A document thumbnail was drawn at 24dp**, a spacing token borrowed as a dimension, so the card that exists to show somebody their own paper showed a dot beside a title.
- **The gallery truncated its own previews mid-word** at font scale 2.0, with no ellipsis: "passed 75 days", "4 steps in the".
- **"+12" rendered as "12+" in Arabic**, because a plus is a neutral character and takes the paragraph direction. A remainder read as a floor.
- **Two edit controls on a half width card rendered the last as "Remov".**

**Four separate defects were isolate marks nested by a second call**, so `Bidi.isolate` does nothing to a string that is already isolated and that family is closed.

**`isDayPrecise` existed twice, privately, on two screens**, and one of the two was a regular expression matching four digits and two dashes, which is the parser rewritten badly and would have called an interval starting on a day day-precise. One answer now, on `Edtf`.

### What the fixture learned

None of grid screens 02, 03, 04, 09 or 10 was reachable from a seed, which is why none of them had ever been looked at.

    tools/device.sh year2 6 walk-year-three  --arranged
    tools/device.sh year2 6 walk-appointment --arranged --appointment-on 2026-08-09
    tools/device.sh month6 6 walk-home       --situation home_family
    tools/device.sh month6 6 walk-quiet      --quiet

**The date is an argument rather than a clock**, because `check_fixtures.py` holds one seed to byte identical output and a fixture nobody can reproduce is not a fixture. The first attempt put the appointment on the fixture's own last day, which is six weeks in the past by the time anybody looks at it.

**The last visit is a preference on the phone**, not a record, so screen 04 is set up with `run-as` against the debug build rather than from the notebook.

**Two fixture defects were found on the way.** Every project's latest word landed on the same day, so the three newest entries in the whole notebook shared one date at every horizon and the trail spine drew three identical nodes. And closing a project never wrote down when it closed, in the app or in the fixture, so the span screen 17 asks for could not be computed.

### Two decisions, taken rather than escalated

**D123.** The whole row adds a card in the gallery rather than an outlined Add beside it. Rule 23 takes the easier target, and `today.add.this` stays in the catalog for the day a row does two things.

**D124.** Saving a project as a template twice keeps both and the library says when each was saved. The two rows were never the defect; the library saying nothing about which was which was.

### The archive

**The stranger test passes.** An archive written from the phone opened on a laptop that has never had the app, with nothing but `tools/decrypt/decrypt.py`, the passphrase and Python. 44 files, no internet at any point.

**The Arabic check fails, which is what it was for.** Every readable page carries `lang="ar" dir="rtl"` and not one carries a single Arabic word of its own, because `ReadableArchive` holds its forty table names and sixty column labels as hard-coded English maps. **#327.**

### Two traps in the tooling

- **`walk.sh see` shows the unmerged semantics tree**, so it cannot say how many stops a reader has. Twenty minutes went into a defect it invented. Only the Compose test API sees the merged tree.
- **`walk.sh tap` cannot match a label carrying a name**, because `Bidi.join` puts isolate marks inside it, and it matches on substrings so a short word hits the longest sentence containing it.

---

## 7. The archive learns to speak, 2026-08-09

**#327, and it was the defect the previous run's Arabic check was built to find.** Every readable page carried `lang="ar" dir="rtl"` and not one carried an Arabic word of its own.

### What the shape of the fix had to be

`ReadableArchive` is pure by contract and 8.5's byte identical regeneration test rests on that, so it could not grow a catalog lookup. **It takes a vocabulary in.** `ReadableArchive.Words` carries the language, the direction, the table and column labels, the page's own prose, and three functions for the strings that carry a count or a year, because Arabic needs six plural forms and ICU belongs with the caller that has a locale.

**The label set is derived rather than listed.** `ReadableWords.from` reads `contract/readable-fields.json`, which is the same file that decides what is rendered at all, so a column given a rendering decision tomorrow needs a word tomorrow and there is no second declaration to drift. 199 keys: 40 tables, 144 columns, 15 page strings, in all four catalogs.

### The check is the part that matters, and it exists because two checks had the same blind spot

The keys are built from a variable. **`check_string_keys.py` skips dynamic keys by design and `check_i18n.py` only holds the four catalogs to each other, so all four agreeing that a key is absent passes both**, and a missing label is not a crash: it is an English fallback on a page that still looks finished. `check_readable_labels.py` derives the required set from the field map and fails when a catalog is missing one, or carries one nothing renders. Eighteen checks now. This is the same family as `check_string_keys.py` itself: a thing held only to something that shares its blind spot.

### Two defects the work walked into

- **`DATED` named two columns that do not exist.** `issued_edtf` on `bill`, `dated_edtf` on `document`. **Every bill and every document ever exported was grouped under a null date and landed on one `undated` page**, and nothing failed anywhere: a missing column reads as null, null is a real bucket, and the pages were produced, linked and counted with the year simply gone. Found by holding that list to the field map rather than by reading it. Both tables carry `received_edtf`.
- **The export asked `Locale.getDefault()` for the language it stamped.** `Strings.load` already documents that as unreliable for the per-app language, and the reason is written there: asking for Chinese yields a configuration of `en-US,zh-Hans` because the app carries no `values-zh` resources, so the default is English. An archive could carry `lang="en"` on a Chinese notebook. It asks the catalog it actually loaded now, and stamps `zh-Hans` rather than a bare `zh`, per D52.

### Verified rather than asserted

App set to Arabic, force stopped and relaunched per the trap about `Strings.load`, a real export driven through the export screen, pulled to this laptop and **opened with `tools/decrypt/decrypt.py` and the passphrase alone**. 45 files, 36 readable pages, 4 attachments.

**128 distinct field labels and 39 distinct headings across every page, and not one of them is ASCII.** The only Latin in a heading is the subject's name, which is data. Then **rendered in a browser rather than assumed**, which is what 8.2 asks for: the front page and a bill page both lay out right to left with the labels on the right.

**The bill and document fix showed on the same export**: `الفواتير، 2026` with five, `المستندات، 2026` with four, and one bill genuinely undated.

### What the phone showed that the code did not

**`walk.sh tap` on a button typed a character into a text field.** The soft keyboard was still up, `KEYCODE_ESCAPE` had not dismissed it, and the tap landed inside it, so the confirm passphrase field gained exactly one character and the screen said the two did not match. It read as a typing defect twice before the cause was clear. **`KEYCODE_BACK` dismisses it; escape does not.**

### What was deliberately left

- **#328, stored values reach the page as themselves.** `أين وصلت: paid`, and `المبلغ: 679040` where the amount is six thousand seven hundred and ninety dollars and forty cents. Wrong in English too, so localization made it visible rather than causing it. `docs/TRAPS.md` section 3 names all three of these exact cases and they all reached a page anyway.
- **Dates stay English.** `ReadableDate` documents that as a decided exception with its reason, and warns in its own comment that a later session would try to "fix" it.
- **#210 stopped being cosmetic.** The pages are a function of the language now, and the archive does not record which language it was written in, so 8.5's byte identical regeneration holds on one phone and would not hold on a restore into a different language. Written on the issue and into `RegenerationTest`'s class comment, not acted on, because which of the two documents is right is not a session's call.

---

## 8. The archive stops printing what the database holds, 2026-08-09

**#328, and it was found by reading the artifact #327 had just fixed.** The labels were in the person's language and the values beside them were not:

    المبلغ            679040
    أين وصلت          paid

`679040` is six thousand seven hundred and ninety dollars and forty cents. `paid` is a column's contents. Both read the same in every language because there is nothing in them to translate, so this was never only a translation defect: an English archive said them too.

### Three separate things, and the third was not in the issue

**Money.** `amount_minor` is minor units so that nothing rounds on the way into somebody's record, which is right, and it is not how an amount is read. `formatMoney` moved out of the money screen into `i18n`, so the archive uses the one implementation rather than growing a second rounding rule on somebody's money. The currency renders inside its own amount the way a date's zone does, so the page no longer says USD twice. **A cost entry carries no currency of its own** and follows `cost_sheet_id` to the sheet that does, which is why the `money` decision can name where its currency comes from.

**The vocabularies.** Seventeen of them, 81 values, in all four catalogs. **The archive keeps its own words rather than borrowing the screens'**, and the second reason is the one that matters: a screen speaks to the person standing there, so the paperwork filter says "You sent", and a document read by a sibling years later cannot. Coupling a page that must be byte identical across a round trip to copy somebody may soften next month is a regeneration failure nobody would trace back to its cause.

**Nine columns nothing had noticed**, found by grepping a real export for long integers rather than by reading the field map. Six printed epoch milliseconds: `incident.resolved_at` said `1781701200000` where the page meant to say when somebody answered. `contract/DATA-CONTRACT.md` 8.2 forbids that in as many words, and **`ReadableDate.timestamp` had been written for exactly it and called by nothing**, which is the second helper in this area found unused in two days. Three printed 0 and 1, including `call_detail.reached`, whose label is "Someone answered".

### What holds it, and every tooth was proved by breaking it

`check_readable_labels.py` grew four ways to fail and each was demonstrated rather than assumed:

- a declared value with no word in one catalog,
- **a value the schema's CHECK allows that the vocabulary does not list**, which is the quiet one: everything else passes and the first row written with it prints the token,
- a render decision spelled wrong, which matters because the renderer's last branch is `else` and a typo prints the column contents with nothing failing,
- **an INTEGER flag or an epoch column rendered as a plain value**, which the schema can prove without anybody noticing again.

### The shape change, and the shortcut not taken

A rendering decision was a bare string and could not stay one: an enum has to name its vocabulary and money has to say where its currency comes from. **Encoding those into the decision string would have been quicker**, `"enum:bill_state"`, and the next person would have had to work out the grammar from the parser. It is a `Field` now, generated from the contract by the same build task as the field map.

### Verified rather than asserted

Two real Arabic exports, each decrypted on this laptop with `tools/decrypt/decrypt.py` and the passphrase alone, each swept across all 36 pages. **Zero raw vocabulary tokens, zero bare epochs**, flags reading نعم and لا, `المبلغ ‏6,790.40 US$` with the currency inside it, and `أين وصلت مدفوعة`. Read in a browser, right to left.

### One trap, and the guard doing its job

**`KEYCODE_ESCAPE` does not dismiss the soft keyboard**, so a tap aimed at the save button landed on a key and typed one character into the confirm passphrase field. The screen correctly said the two did not match, which reads as a typing defect and is not one. `docs/TRAPS.md` section 1.

**The destructive command guard refused a recursive directory removal aimed at a scratch folder**, correctly and without being asked. Third real refusal. **It also refused this section**, because the sentence above named the verb, which is #323 and is not a reason to weaken it: the text was written to a file and appended instead.

---

## 9. B4's argument becomes true, 2026-08-09

**#9's last item, and it turned out to be a claim the repository had never made good on.** B4 dropped the emulator from this project on an explicit argument:

> Data survival is proven by the export and import round trip against the golden vectors in continuous integration, which is repeatable, runs on every push, and does not depend on any one device's history.

**Nothing in continuous integration rendered a readable page at all.** `RegenerationTest` is instrumented, `DateVectorTest` reads assets, and both need the phone that B4 says should not be the proof. So the reasoning that justified dropping the emulator pointed at a test that did not exist, and on any day the phone was unreachable the strongest guarantee in the format was unchecked. **Nothing anywhere said so**, which is the same shape as every other defect this week: a promise that reads as kept.

### What it is

`contract/test-vectors/readable/`, rendered by an ordinary JVM unit test.

- **Fifteen rows across ten tables**, lifted out of an archive the app itself wrote, so they are rows the app could write. The fixture rule, applied to a vector.
- **All twelve rendering decisions reached**, and a third test fails if a decision is added that the vector does not reach, so it cannot quietly stop covering the thing it exists for.
- **English and Arabic**, because almost no rendering defect is visible in English.
- **The failure names the file and the character**, proved by changing one word in an expected page and reading the message rather than assuming it.

### Two things worth keeping

**A golden vector needs a regeneration path, and it must be deliberate.** `-Dhealthtrail.vector.write=true`, then read the diff. A test that quietly rewrites its own expectation is a test that always passes, so the switch is explicit, documented, and never on by default.

**Reading a path out of a system property is invisible to Gradle.** Editing an expected page left `testDebugUnitTest` UP-TO-DATE, so the vector could change and the test would not run. It would still have run in continuous integration on a fresh checkout, which is the worst version of that bug: **green locally and red only on push**, with the failure arriving after the commit rather than before it. The expected pages are a declared task input now.

### What the vector caught in its first hour

**#331.** Its money strings had to come from somewhere, and computing them here rather than reading them off a real export produced a different answer: the JDK renders `‏٦٬٧٩٠٫٤٠ US$` where Android produced `‏6,790.40 US$`. Same code, same locale, same currency, different digits.

That is not a curiosity. **8.5 asserts an archive regenerates byte identical, and `/contract` exists so that a second reader renders the same archive rather than reimplementing it.** Two readers that disagree about digits cannot both satisfy 8.5. `ReadableDate` already argues this exact point for dates and spells month names itself rather than asking the locale; money did not get the same treatment.

---

## 10. The importer learns to merge, 2026-08-09

**#211, and the half of it that was left.** Restore could replace a notebook and could not add to one, and merge is the half where the rules have to be written down: whose version of somebody's note survives is a question a record keeping app cannot get wrong quietly.

### The shape

`Merge` is pure, so the rules are unit tested without a device. Thirteen of them. Match by id and nothing else. Later `updated_at` wins, and a tie goes to `origin_device` **because two phones merging the same pair in either direction have to reach the same answer**, or the notebooks diverge permanently and each is certain it is right. Nothing is invented. And **merge never deletes**: a row the file has never heard of is a row the other phone never saw, not one somebody removed, and removal travels as a tombstone that merges by the same rule as everything else. That is what the schema having no hard deletes buys.

`MergeApply` is the thin Android half. It plans first, so a file it cannot place never opens a transaction, which is what makes "fully succeeds or changes nothing" true rather than hoped for.

### What the phone found and no test could

**The merge crashed with `FOREIGN KEY constraint failed`.** Rows go in table by table and a child can arrive before its parent: `chapter` sorts before `subject`, `attachment` before `entry`.

**No test in the class could have caught it, and the reason is worth keeping.** Every archive those tests merge came from the notebook it is merged into, so everything was `unchanged` and **nothing was ever inserted**. The suite was green on a code path it never once executed. A real second phone is all inserts. The new test empties the notebook first so the file is genuinely new, and the fix was proved by removing it and watching the test fail.

**Then three more, all from looking at the screen.** The conflict screen printed `Kept: 1786315875877` for a pinned entry, which is #328's defect turning up in a screen on the day it was closed in the archive. The door to that screen vanished when the app restarted, because the count was set after a merge and never loaded. And the Arabic screen used a Latin comma where the language writes it reversed.

**And two on the restore screen.** A disabled confirm button reading "Replace everything with this" while nothing was chosen, which reads as replace being the default on the one control whose entire point is that there is no default. And "Nothing here is removed" set in alarm red, which teaches people to ignore the color when it means something.

### Two lessons that generalize

**A skip list of table names is a list nothing checks.** The first version named `migration` and the table is `schema_migration`, so the merge read a table with no `id` and threw. Mergeability is asked of the schema now: a table without `id` and `updated_at` cannot be merged whatever it is called. That is the third time in one day the same shape has appeared, after the archive's `DATED` and the fixture's document categories.

**A green suite can be green on a path it never runs.** The insert path had five tests around it and no coverage of it at all, because of what the fixtures happened to contain. **Ask what the test data makes reachable**, not just what the test names say.

### What is not done, and it is on the issue rather than implied

Three of #211's ten criteria are not met. **#332** is the serious one and it was found by reading 8.3 against the code: an attachment row whose file is gone produces an archive **this app then refuses to open**, and export never notices. Per-section view choices do not exist in the schema. And nothing yet lets a person read the unknown tables that `open` names.

---

## 11. The export learns to look, 2026-08-10

**#332, the only open defect where the app could produce something it cannot read back.** Found the night before by reading `contract/DATA-CONTRACT.md` 8.3 against the code rather than by anything failing.

### The chain, in one paragraph

`Attachments.all()` lists files on disk, not rows. So a live `attachment` row whose bytes had gone shipped as a row with no file, `ExportContainer.open` refused the whole archive by name, and nothing at export time noticed. The person was told it succeeded. **The failure lands at restore**, on the new phone, with the old one gone.

### What was built, and what deliberately was not

**The export looks.** `Backup.export` returns `Backup.Written`, the manifest plus a list of what it could not find, each entry carrying the hash, the original filename, and `created_at`. 8.3 asks for the name and the date to survive, and a bare content hash says nothing to anybody.

**It reads the staged copy, not the live database.** The staged copy is what ships. A row written between staging and now is not in the archive, so warning about it would name a file the archive never claimed; a row deleted in that window is still in the archive and still has to be checked.

**The query is the import's own, copied deliberately**, tombstone clause and all. A deleted attachment's bytes are legitimately gone while its row still travels, so checking those would fire on nearly every real notebook. And a warning that named a different file from the one blocking the restore would be worse than no warning at all.

**What was not built is the manifest field**, and that is the point rather than an omission. `contract/EXPORT-FORMAT.md` is published byte for byte and `tools/decrypt/` was written from it, so a new field is a decision. Three shapes are on the issue. **The archive is still one this app cannot open, and the screen now says so.**

### Three things worth carrying forward

1. **A test that ends on its own cleanup returns what the cleanup returned.** `fun x() = runBlocking { ... staging.deleteRecursively() }` is a method returning `Boolean`, and JUnit refuses **the whole class** with `should be void`, naming the method rather than the expression. Two tests did it at once and the class would not load. Cleanup belongs in `@After`.
2. **A test that leaves the app's database in a bad state breaks classes it has never heard of.** These tests exist to create the one condition every other export test must not meet, and the database persists across classes in a connected run. Left uncleaned, `RoundTripTest`, `RegenerationTest` and `MergeApplyTest` would all fail on an archive that will not open, naming a hash from a fixture they do not know about. **Tombstoned in teardown**, which is what the app itself would leave.
3. **Assert the property, not the instance.** The first version asserted that import refused on *this exact hash* and it failed, because `open` stops at the first bad row it finds and which row that is depends on the whole database. The property worth asserting is that **export cannot be silent about anything import will refuse on**, which is both stronger and robust to what the run left behind.

### The walk

Two of the month six fixture's four attachment files were moved to `files/parked` so the state was genuine rather than mocked, and moved back afterward with the count read again. Both themes, font scale 2.0, and Arabic right to left. **The Arabic dual form renders**, `ملفين مرفقين` rather than a plural, which is the six form catalog entry working and is the kind of thing only rendering proves. Four captures on #335, a row in `DESIGN.md` section 14, a line in `HANDOFF.md` section 8.1, and D129 for the three decisions.

---

## 12. The named failure modes, and the archive stops printing identifiers, 2026-08-10

Two pieces of work in one run, and the second one caught the first kind of defect the repository is best at hiding.

### #212, three of the eight named modes

**Time, numbers and absence.** The other five were already covered or blocked: unicode is #227 and is a change to every write path rather than a test, and the four gigabyte half of scale needs a fixture nobody has written.

**Numbers and absence had been blocked on the importer rather than on attention**, and the reason is the useful part: 8.5's regeneration test renders both sides with the same code from the same bytes, **so a value mangled identically on the way out and the way back matches itself and passes**. Only reading the restored database catches them. #211 landing is what unblocked eleven tests.

**Time is the contract's own sentence as an assertion.** July 6 in New York, restored in Tokyo, still July 6, and the same going west, because an off by one zone defect is directional and one direction passes on half of a broken implementation. Plus a forty five minute zone and both daylight saving boundaries, whose lengths are asserted **before** the round trip so a resolver that was already wrong cannot round trip its own mistake faithfully and pass.

**The zone is changed in the process, never on the phone.** `TimeZone.setDefault` is what `ZoneId.systemDefault()` reads, restored in teardown. Rule 19's settings exception covers font scale, animation and the reader and nothing else, and the default zone is process wide, so a test that left it set would move every later class in the run to another country.

### #329, and the sweep that found more than the issue did

**The issue named four identifier columns and one color.** Fixing them was ordinary. Two things about it are worth keeping.

**The obvious implementation was wrong in the worst available way.** One map from id to name across all the shipped catalogs. `discharge_planning` is both a care thread and a project template; `dietary` is both a thread and a standing instruction. A merged map does not fail, it **answers, confidently, with the wrong name**, on a page nobody reads until it matters. Which catalog a column resolves into is declared beside its render decision now, exactly as an enum declares its vocabulary.

**Sweeping the produced archive found a third integer the issue had not named.** Forty nine fields across nine tables reading "Its place in the order: 0". `ReadableRows` orders every page by id on purpose, so the number described a sequence the pages do not follow. **A number that disagrees with its own document is worse than no number.** That is now three times the grep-a-real-archive sweep has found something reading the code did not.

### The one that got away, and what caught it

**`RegenerationTest` failed on a defect introduced by the fix itself.** The export wrote `Nursing home`, the regeneration wrote `nursing_home`, and 8.5's byte identity broke on exactly the guarantee it exists to hold.

**The cause was an empty default on a parameter.** `ReadableWords.from` took `catalogNames` with a default, so one side of a round trip could be asked a different question from the other without anybody writing anything wrong. `ExportContainer.Source.readableWords` already has no default for the same reason, written down at the time as "a default would be an English archive that nothing complains about". **That is the second time the rule has earned itself in two days**, so the default is gone and the next omission is a compile error rather than a diff.

**The general form, since it has now happened twice:** where two paths must produce identical bytes, a parameter with a default on either of them is a way for them to disagree silently. Make it required and let the compiler ask.

---

## 13. Two documents disagreed, and reading the higher one closed two issues, 2026-08-10

**#210's locale question and #332's second half had both been deferred to the owner as published format changes.** Both were wrong, in the same way, and the cost was two sessions writing careful escalations instead of ten minutes of work.

### What was actually true

`contract/DATA-CONTRACT.md` 8.2 lists what the inner manifest carries. It has always included the export's timezone, **the locale the readable copy was written in**, and **the list of any attachment whose bytes could not be read at export time.**

`contract/EXPORT-FORMAT.md` listed `readable` as carrying `pages` and nothing else, and said nothing about a missing list. The code followed the format document, because that is the published one and is what `tools/decrypt` was written from, and so wrote none of the three.

**`CLAUDE.md` already says which wins.** Verified code, then `HANDOFF.md`, then `DECISIONS.md`, **then the data contract for data questions**, then `DESIGN.md`. The format document is below the contract. So neither of these was a decision about what the format should carry. Both were unimplemented requirements plus a document that had fallen behind, and the fix is to correct the document.

### The distinction, because the next one will look the same

**A format change is the owner's when the contract does not say what to do.** That is what #332 looked like from `EXPORT-FORMAT.md` alone, and the reasoning written on the issue at the time was internally sound. **It is not the owner's when the contract already says what to do and something else disagrees.** Reading the higher document before escalating is the cheap step that was skipped twice.

### What it bought

**`readable.locale`** is what lets a regeneration reproduce the archive it came from. 8.5's byte identical guarantee had quietly depended on it since #327 made the pages speak the person's language, and nothing recorded which language that was.

**`attachments.missing`** turns #332's silent failure into a stated one. The archive opens, the row arrives with its name and its date, and the person learns a photograph existed and is gone rather than never learning it was there.

**An attachment that is absent and undeclared is still refused.** That difference is the whole reason it is a list rather than a flag: it separates a record with a gap from a copy damaged in transit. The test for it has to be built by hand now, because the app cannot produce an undeclared gap any more.

**`tools/decrypt` needed no change**, and that is what made the correction safe rather than merely correct: it reads only the outer manifest's version and encryption parameters, so every field is additive to a reader that has already shipped.

### The proof, which was not a test

The month six fixture with one of its four attachment files moved aside. Exported: the screen said "Saved" and then named the file it could not find. Decrypted on the laptop: the manifest carried the hash, `scan-000.jpg`, and the timestamp, plus `readable.locale` and `exported_zone`. **Then restored, in the app, on the phone: "Restored. Your notebook is what was in the file."** 182 trail items back, every section, three attachment files present and the fourth recorded as gone.

**On the build #332 was opened against, that same file was refused by name**, at restore, on the new phone, with the old one gone.

---

## 14. Moving the vectors into the contract found a defect nobody could see, 2026-08-10

**#15 asked for the digest's cases to live in `contract/test-vectors/` where both platforms can run them.** They lived inside `DigestTest` as Kotlin. Moving them is bookkeeping; what it found is not.

### The defect

`Digest.sectionOf` mapped `"reading"` to Progress. **There has never been a table called `reading`.** The schema has `measure` and `measurement`, and the change log triggers write exactly those two names, so every reading anybody recorded fell through to the `else` branch and was left out of the Today digest.

**On the six month fixture that is 244 new things where it should say 261**, computed from a real decrypted archive by applying the engine's own counting rule to its change log. Seventeen of the person's own readings, invisible, on the front screen.

**It fails in the direction that looks like calm.** A quiet digest is exactly what a quiet week looks like, so there was nothing to notice.

### Why nothing caught it, which is the part worth keeping

`DigestTest` had a case that walked the mapping and asserted every table resolved to a section. **It walked a hard-coded list in the test file that also said `reading`.** So it asserted the mapping the code had rather than the mapping the schema had, and two copies of one mistake agree with each other forever.

**This is the third time this exact shape has cost something.** `DATED` named two columns that do not exist and every bill and document ever exported was filed under a null date. The archive's dynamic label keys were absent from all four catalogs at once and passed both checks that looked at them. Now this.

**The rule, stated once more because it keeps earning itself: hold the set to the file that generates it, never to a second copy of the set.** The authority here is the change log's own `VALUES ('<table>'` trigger literals, because that is the complete set of names a change row can ever carry.

### What is in place now

Three things that cannot agree with each other by accident. `check_digest_sections.py`, the nineteenth check, holds the engine's mapping to the schema's literals. `contract/test-vectors/digest.json` carries the mapping and fourteen cases, each with the sentence saying why its answer is what it is. `DigestTest` asserts the engine matches the contract **in both directions**, so a table added to the engine and not to the contract fails too.

**The unmapped tables are listed rather than absent**, because "deliberately not counted" and "nobody thought about it" look identical in code.

**Proved not vacuous** by putting the old mapping back and watching two tests and the check fail, then restoring it.

### What did not move

The pattern engine's vectors, because there is no pattern engine, and #15's criteria about gaps and minimum-data thresholds assume one. The digest counts and stops: it has no threshold and says nothing about a gap, deliberately, because anything it could say about one would be an opinion about somebody's care.

**And the second reader.** `web/` holds a README. Until #16 exists these are golden vectors with one reader, which is better than tests with one reader and is not what the issue asks for.

---
