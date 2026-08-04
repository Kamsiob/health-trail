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

