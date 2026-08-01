# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

If you are a session with no memory, this file plus `git log` and the issue tracker is everything you need. Read this in full, then `CLAUDE.md`, then continue only from what the repository says is true.

**Last rewritten:** 2026-08-01, on the branch `feat/37-setup-screen`, in the session where the owner sent the standing quality bar.

If you find yourself re-reading files you already read this session, compaction has happened. Stop, read this file again, and re-orient before continuing.

---

## 1. The thing that changed everything, and it is retroactive

**The owner sent the standing quality bar mid-session.** It is recorded as **D34** and written into `CLAUDE.md` as rules 14 through 21, into `DESIGN.md` as sections 10.8, 10.9, and 10.10 plus additions to sections 1, 6, and 9, and into `MASTER_SPEC.md` section 4.2.

**Nothing in it is forward-only.** Every screen already built, every document already written, and every issue already open comes up to it. A codebase where the standard changed halfway through is a codebase with two standards. Issue **#43** is the retroactive audit and it is not optional.

The parts that change how you work, compressed:

- **No screen ships thin.** Functionally correct and visually plain is not done.
- **Hierarchy before decoration**, in the order `DESIGN.md` 10.8 sets out. Uniform weight is not neutral.
- **Everything the person touches responds.** One press treatment for the whole app, `DESIGN.md` 5.14.
- **Dates are a real model**, EDTF, never falsely precise, always editable, unknown is a first-class value.
- **Links go both ways**, and taps are the currency.
- **Accessibility is a gate**, verified with the settings actually on.
- **Look at it on the phone before closing anything**, then fix the worst thing you find and look again.

---

## 2. Where the work is, exactly

**Phase 0** is substantially built and not closed. Thirteen of its issues remain open, listed in section 5.

**Phase 1** is where the work is. The core loop runs end to end on real hardware: the gold capture button opens the sheet, the sheet opens a form, the form saves, the entry is written, the change log trigger fires in the same transaction, and the notebook's count refreshes through the live view.

**The notebook table of contents is rebuilt** and is the current pull request, **#49**, which closes **#36**. It was the last of the four screens the owner named as visually thin.

---

## 3. The precise next action

**The date model, issue #38, is built and the schema is converted.** What remains on it is the interface, which is issue #39, and it is the precise next action:

1. **The capture form's date control.** It currently offers four chips: today, yesterday, this week, not sure. The bar requires **an exact date and time always available as a peer of the chips, not behind them**, and natural expression for a month or a season. `DESIGN.md` section 10.9.
2. **Editing a date from the entry itself, forever, with the same control.** Nothing can edit a date yet, because nothing shows an entry yet.
3. **Rendering.** `EventDateText` exists and is proven by the vectors. Nothing calls it yet, because the trail is not built. **The first screen that shows a date must call it rather than formatting one itself.**

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
| Every screen built so far | Instrumented, plus built, installed, opened, and looked at on the Pixel |
| The notebook's fold behavior | Walked on the Pixel with a hospital stay template: appointments, the trail, documents, and standing instructions forward, money and progress collapsed, which is exactly what that template names |
| Every screen looked at with the keyboard up | Two defects found that way and nowhere else: the setup button colliding with the last field, and the field clipped mid-box at the scroll boundary |
| The press state, everywhere | Measured on the device on three different surfaces: a card row (26,36,43) to (43,50,56), the filled button (127,182,212) to (136,186,214), the capture button (227,177,85) to (228,182,100). `FilledButton` and `TextAction` previously had no press state at all |

**The whole instrumented suite: 70 tests, 0 failures**, run on the connected Pixel 10 Pro XL. All seven implemented compliance checks pass. JVM unit tests pass.

---

## 5. Remaining work inventory, in order

**The standing bar, opened 2026-08-01.** These came out of D34 and several of them outrank the Phase 0 leftovers.

| Issue | What |
|---|---|
| ~~#38~~ | **Done.** The contract, the schema, the repository, the renderer, and the vectors |
| #39 | The date interface, which hides all of the model. Depends on #38 |
| ~~#40~~ | **Done.** Every tappable surface in the app uses the one treatment in 5.14 |
| ~~#41~~ | **Done.** Grouped by where the care is happening, ordered by how common, and visibly skippable |
| #42 | The remaining two capture inputs, measurement and document |
| #43 | Retroactive: audit every screen already built against the bar. Opens further issues rather than fixing everything itself |
| #44 | Accessibility gate, verified with the reader on, the font at maximum, and reduced motion enabled |
| #45 | Capture from outside the app: widget, quick settings tile, share sheet target |
| #46 | No dead ends. Every link goes both ways, context carries forward |
| #47 | Search: universal from Today, scoped in every section |
| #48 | Carry the universal parts of the bar into `kamsiob-project-template.md` |

**Phase 1, still open.**

| Issue | What |
|---|---|
| ~~#37~~ | **Done.** Grouped, hinted, and the reassurance said once in words |

**Phase 0, still open.**

| Issue | What | Why this order |
|---|---|---|
| #8 | A static check that makes querying a base table structurally hard | The repository layer is built and correct. What is missing is the check that stops the next person bypassing it |
| #7 | Prove the change log append is transactional through the Kotlin path | The schema already proves it. This proves it through SQLCipher and Kotlin |
| #13 | Four locale catalogs with right to left verified on a screen | Arabic has not been looked at on the device, which is the unmet half |
| #15 | Golden test vectors both platforms run against | **The first vector, `dates.json`, exists and runs.** The engine vectors do not |
| #14 | Encrypted database, remaining criteria | The migration mechanism and the key loss screen are the unmet parts |
| #12 | Fonts covering all four scripts | Independent. **Blocks the light theme and Arabic screenshot recaptures on every design review issue** |
| #9 | Export container, manifest, encryption, round trip equality | **Now also has to round trip the EDTF column byte for byte** |
| #10 | `SyncTransport` with the file implementation behind it | Needs the export container |
| #17 | Deterministic fixture generator | Needed before any persona run means anything |
| #16 | Web scaffold opening the same schema | `npm` is absent on this machine |
| #18 | Content compliance checks in CI | Seven checks exist and run. The issue stays open for the ones not implementable yet |
| #21 | Roadmap document | Documentation only |
| #25 | About screen links the canonical privacy policy | Needs an About screen, which does not exist |

**The immediate Phase 1 feature queue after the above.**

1. **Today, with the digest engine**, reading the change log for what changed since the person was last here.
2. **The Unfiled tray.** The capture form already writes `is_unfiled = 1` and tells the person their entry is going there. There is nowhere to see it yet. **This is a promise the app is currently making and not keeping**, so it ranks above the rest.
3. The trail itself, projects, and More.

**Something that must not survive to release.** The Today, Projects, and More destinations render an honest interim screen saying that part is not built yet. That is deliberate rather than a stub: `DESIGN.md` section 5.5 fixes the four destinations and their order, so hiding them would break the rule that a person finds things where they last were. Each disappears as its destination lands. **If one is still there at release, that is a bug**, and `ShellTags.NOT_BUILT` makes them greppable.

---

## 6. Blocked

**Nothing is blocked.** All four entries that ever appeared in the BLOCKED section of `DECISIONS.md` are resolved and kept there with their outcomes. A fresh session needs nothing from the owner in order to continue.

The one thing waiting rather than blocked: **the light theme and Arabic screenshots on the design review issues** are deliberately not captured, because the bundled fonts for Arabic and Chinese have not landed. That is issue #12. Capturing Arabic before the fonts are in would put a screenshot of fallback glyphs on a review issue.

---

## 7. The phone

- Device: Pixel 10 Pro XL, serial `57241FDCQ0000H`, connected over USB. **The only test device.**
- **No emulator.** Dropped from this project. Do not attempt to launch one, do not create an AVD, and do not treat its absence as a blocker. See D21, D23, and B4 in `DECISIONS.md`.
- The phone is in **dark** system theme, which is why every committed screenshot is `-dark`.
- **The notebook currently holds throwaway data**: a subject named Mom on a hospital stay template with four care threads. The last `connectedAndroidTest` run wiped it after the screenshots were taken, so what is on the phone right now is a fresh install sitting at the disclaimer gate. Nothing on it is worth preserving.

**The one operational rule about the phone.** `connectedAndroidTest` uninstalls the application and takes its data with it. Before running it, if the phone holds anything worth keeping, export through the app's own export feature first and reimport after.

---

## 8. This environment, so a fresh session does not rediscover it

**The shell does not carry state between tool calls.** Every command starts fresh.

- **`ANDROID_HOME` is not set.** The SDK is at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and **does not exist in a fresh clone**. Recreate it: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH.** It is at `/home/Kamsiob/Android/Sdk/platform-tools/adb`. `tools/screenshot.sh` resolves it itself.
- **The working directory contains a space and two leading dashes.** Quote every path.

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. It refuses to capture unless the app is the focused window, checked before and after, because this is the owner's daily driver phone. It reads the theme from the device and refuses an argument that disagrees, per D31. Do not pass a theme.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**A trap in the Compose test API, found the hard way.** `performScrollToNode` walks a lazy list a viewport at a time and gives up when it thinks it can go no further. It got that wrong for the Arabic catalog, stopped two rows short, and reported the rows as absent when they were only further down. **Scroll by the list's own item key instead**, with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` rather than on a surface around it: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Continuous integration.** The workflow triggers on `push` to main, on `pull_request`, and on `workflow_dispatch`. Pull request events stopped firing part way through 2026-07-31 and **are firing again as of 2026-08-01**. If they stop again: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>`. **Do not read an absence of checks on a pull request as a passing build.**

**One CI step catches a real habit.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools`, or `contract` without touching this file. It caught pull request #49. Rewrite this file in the same commit as the work, not afterward.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**Everything else:** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

**Verification.** `tools/verify.sh` is the honest runner: it captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. `python3 tools/checks/run_all.py` runs the seven content and contract checks alone. **Never chain a commit on a grep of output.**

---

## 9. Screens built without a mockup

Every screen built without one is composed from existing components under `DESIGN.md` section 10, ships complete with every state, and is logged in three places at the moment it is built: a `needs-design-review` issue with a device screenshot, an entry in `DESIGN.md` section 8, and a line here.

| Screen | Built | Issue | Reviewed |
|---|---|---|---|
| Disclaimer gate | 2026-07-31, rebuilt same day to the 10.6 bar | #28 | not yet |
| Essentials first setup | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #30 | not yet |
| Situation picker | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #32 | not yet |
| Capture form, four kinds | 2026-07-31, rebuilt same day to screen 26 | #34 | not yet |
| Notebook table of contents | 2026-08-01, rebuilt to the 10.6 bar | #36 | not yet |

The notebook is drawn in the reference file, so it is listed here as a correction rather than as an undrawn screen. `DESIGN.md` section 3 item 8 records the four ways the built screen departs from the mockup, with reasons.

**Known ahead:** the template library, the four template pickers, the template detail view, and the template editor. None is drawn. `MASTER_SPEC.md` section 4.10 carries their requirements.

---

## 10. Persona runs

None yet. `TESTING-PERSONAS.md` requires each run to be recorded here with its fixture seed and date. A persona walked against a schema that has since changed has not been walked, and the deterministic fixture generator that would make a run meaningful is issue #17. **The schema is about to change again for #38**, so a run before that lands would be wasted.

---

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

## 13. Uncommitted work

**None.** Everything described here is committed on `feat/38-edtf-dates`.
