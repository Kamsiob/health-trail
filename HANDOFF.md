# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

If you are a session with no memory, this file plus `git log` and the issue tracker is everything you need. Read this in full, then `CLAUDE.md`, then continue only from what the repository says is true.

**Last rewritten:** 2026-07-31, at the end of the session that raised the quality bar. Everything it describes is merged to `main` through pull request #35. There is no work sitting on a branch.

If you find yourself re-reading files you already read this session, compaction has happened. Stop, read this file again, and re-orient before continuing.

---

## 1. Where the work is, exactly

**Phase 0** is substantially built but not closed. Nine of its issues are closed and device verified. Thirteen remain open and are listed in section 4 in the order to take them.

**Phase 1** is in progress and is where the work actually is. The core loop runs end to end on real hardware: the gold capture button opens the sheet, the sheet opens a form, the form saves, the entry is written, the change log trigger fires in the same transaction, and the notebook's count refreshes through the live view.

**The session that just ended did one thing above all else, and it changes how every future screen is built.** The owner reviewed four finished screens and said they were built to be functionally correct and left visually thin, that this was one symptom rather than four defects, and that the bar changes now rather than in a cleanup pass. That is recorded as **D30** and written into `DESIGN.md` as section 10.5, with a checklist in section 10.6 that every screen passes before its issue closes.

**Two of the four screens are now rebuilt to that bar. Two are not.** That is the current unit of work and the precise next action, in section 3.

---

## 2. What is done, and how each piece was verified

Verified means checked through the mechanism, not inferred from the code being written.

| Piece | How it was verified |
|---|---|
| The schema, 34 user data tables | `tools/checks/check_schema.py` runs it into a real SQLite database on every push and asserts the six contract columns, a `live_*` view, both change log triggers, no AUTOINCREMENT, and that a failing change log write rolls the data write back |
| Locally generated ids | `IdsTest`, a JVM unit test. UUIDv7 with same millisecond sequence bits and backward clock protection |
| Encrypted database, SQLCipher, key in the Keystore | `DatabaseTest` on the connected phone |
| The repository layer | Every read goes through a `live_*` view. Proven by the instrumented suite writing and counting through it |
| Four locale catalogs, ICU MessageFormat | `check_i18n.py` on every push: same keys, same placeholders, consistent direction. `CopyIntegrityTest` on the phone proves no locale silently falls back to English for the disclaimer |
| Contrast in both themes | `check_contrast.py` measures 80 pairs against the actual token values on every push |
| Content compliance | `check_copy.py` (no em dashes, American English), `check_templates.py` (1510 strings checked for advice and judgments), `check_contract_isolation.py`, `check_self_contained.py` |
| Disclaimer gate, setup, situation picker, notebook, capture sheet, capture form | Instrumented, plus built, installed, opened, and looked at on the Pixel |
| The whole capture path | Walked by hand on the Pixel: a visit filed under discharge planning, dated yesterday, reached the trail and the count moved from "Nothing yet" to "1 item" |

**The whole instrumented suite: 47 tests, 0 failures**, run on the connected Pixel 10 Pro XL at the end of the session. All seven implemented compliance checks pass.

**The two screens rebuilt to the new bar, both looked at on the device:**

- **The disclaimer gate.** Was one heading and two long paragraphs. Now a lead plus three cards, each with its own real heading. The voice was rewritten from defensive to plain. `DESIGN.md` section 7 carries the new wording verbatim and states that the safety substance may not be cut on the grounds of warmth. Screenshot: `docs/screenshots/disclaimer-dark.png`.
- **The capture form.** Was two single line text fields. Now screen 26 of the reference file: rough date chips with "Not sure" among them, care thread chips carrying their route color with "Not sure yet" among them, an open note area, and "Save what you have". Screenshot: `docs/screenshots/capture-visit-dark.png`.

---

## 3. In progress, and the precise next action

**The precise next action: rebuild the notebook table of contents to the bar in `DESIGN.md` section 10.6. That is issue #36, and it carries the acceptance criteria.**

The owner's words: extremely cluttered, twelve sections presented flat at uniform weight, so nothing has priority and it reads as a list of everything rather than a table of contents.

**The designed answer already exists in the data and is not wired up.** `templates/data/situations.json` carries a `forward` array and a `folded` array on every situation template. For `hospital_stay` they are `forward: [trail, appointments, documents, standing_instructions]` and `folded: [money, progress]`. Nothing in the app reads either one. `NotebookShell.kt` holds a flat `SECTION_ORDER` constant and `NotebookScreen.kt` renders every section as an identical card.

What has to be true when it is done:

- Forward sections sit expanded, folded sections sit collapsed, and **folded means collapsed and reachable, never hidden.** The section order itself never changes, because a table of contents whose order shifts is not a table of contents. `DESIGN.md` section 8 and `MASTER_SPEC.md` section 4.4 both say this.
- Related sections are grouped, the live counts get one consistent treatment, and the hierarchy is visible at a glance.
- A count of zero still reads as words rather than as a digit. `NotebookScreenTest` asserts this and it must keep passing.
- Reading the situation template needs `TemplateCatalog.kt`, which already parses `situations.json`, and the active subject's `situation_template_id`, which `Repository.activeSubject()` already returns.

**Then the setup screen, "Who you are looking after", which is issue #37.** The owner's words: it is the first real screen after the disclaimer and it decides whether someone in a hallway keeps going. Essentials only, everything skippable, warm supporting text, generous spacing, and clear indication that nothing here is permanent. It is functionally right already; it is visually thin.

**Then sweep every screen built so far against section 10.6 and fix what falls short.** Then carry on with Phase 1 feature work from section 4.

Nothing is half edited. The working tree is clean and every commit is pushed.

---

## 4. Remaining work inventory, in order

**Phase 0, still open.**

| Issue | What | Why this order |
|---|---|---|
| #8 | A static check that makes querying a base table structurally hard | The repository layer is built and correct. What is missing is the check that stops the next person bypassing it |
| #7 | Prove the change log append is transactional through the Kotlin path | The schema already proves it. This proves it through SQLCipher and Kotlin |
| #13 | Four locale catalogs with right to left verified on a screen | Catalogs and the runtime are built. Arabic has not been looked at on the device, which is the unmet half |
| #15 | Golden test vectors both platforms run against | Defines correct before the engine exists. The digest engine needs it |
| #14 | Encrypted database, remaining criteria | Built and running. The migration mechanism and the key loss screen are the unmet parts |
| #12 | Fonts covering all four scripts | Independent. **Blocks the light theme and Arabic screenshot recaptures on every design review issue** |
| #9 | Export container, manifest, encryption, round trip equality | Needs the database and the repository layer. Also the only proof that data survives an update, per the owner's instruction |
| #10 | `SyncTransport` with the file implementation behind it | Needs the export container |
| #17 | Deterministic fixture generator | Needs the schema and the repository. Needed before any persona run means anything |
| #16 | Web scaffold opening the same schema | Needs nothing else. `npm` is absent on this machine |
| #18 | Content compliance checks in CI | Seven checks exist and run. The issue stays open for the ones not implementable yet |
| #21 | Roadmap document | Documentation only |
| #25 | About screen links the canonical privacy policy | Needs an About screen, which does not exist |

**Phase 1, the two screen rebuilds that come first.**

| Issue | What |
|---|---|
| #36 | Rebuild the notebook table of contents. Hierarchy, grouping, and the `forward` and `folded` arrays that already exist in the data and are not read |
| #37 | Rebuild the essentials first setup screen to the section 10.6 bar |

**Phase 1, the immediate queue after the two screens in section 3.**

1. The remaining two capture inputs: **measurement** and **document**. Neither fits the shared form. A measurement carries a value and a unit; a document carries a photograph. They get their own screens. Choosing one from the sheet currently closes it and does nothing.
2. **Today, with the digest engine**, reading the change log for what changed since the person was last here. This is the first thing that needs `contract/test-vectors`, issue #15.
3. **The Unfiled tray.** The capture form already writes `is_unfiled = 1` and tells the person on screen that their entry is going there. There is nowhere to see it yet. This is a promise the app is currently making and not keeping, so it ranks above the rest.
4. The trail itself, projects, and More.

**Something that must not survive to release.** The Today, Projects, and More destinations render an honest interim screen saying that part is not built yet. That is deliberate rather than a stub left lying around: `DESIGN.md` section 5.5 fixes the four destinations and their order, so hiding them would break the rule that a person finds things where they last were. Each disappears as its destination lands. **If one is still there at release, that is a bug**, and `ShellTags.NOT_BUILT` makes them greppable.

---

## 5. Blocked

**Nothing is blocked.** All four entries that ever appeared in the BLOCKED section of `DECISIONS.md` are resolved and kept there with their outcomes. A fresh session needs nothing from the owner in order to continue.

The one thing that is waiting rather than blocked: **the light theme and Arabic screenshots on the four design review issues** are deliberately not captured yet, because the bundled fonts for Arabic and Chinese have not landed. That is issue #12. Capturing Arabic before the fonts are in would produce a screenshot of fallback glyphs and put a misleading image on a review issue.

---

## 6. The phone

**Verified with adb at the end of the session, not assumed.**

- Device: Pixel 10 Pro XL, serial `57241FDCQ0000H`, connected over USB.
- `com.kamsiob.healthtrail` packages installed: **exactly 1**.
- `com.kamsiob.healthtrail.test` packages installed: **0**.
- Version installed: `0.1.0`, last updated 2026-07-31 16:30, built from commit `f3f0261`. **The installed build matches the current code.** `main` has since moved to the squash merge of pull request #35, and `git diff f3f0261 main -- android contract templates` is empty: everything after it was documentation and the merge itself. The app on the phone is the app in the repository.
- It launches. `topResumedActivity` was `com.kamsiob.healthtrail/.MainActivity`.
- The notebook on the phone holds throwaway data written while walking the capture flow: a subject named Mom, a hospital stay template, four care threads, and one visit entry. Nothing on it is worth preserving.
- The phone is in **dark** system theme, which is why every committed screenshot is `-dark`.

**The one operational rule about the phone.** `connectedAndroidTest` uninstalls the application and takes its data with it. Before running it, if the phone holds anything worth keeping, export through the app's own export feature first and reimport after. During this session it was run three times and wiped the throwaway data each time, which is fine and is why the disclaimer gate reappeared twice.

**No emulator.** The emulator is dropped from this project. Do not attempt to launch one, do not create an AVD, and do not treat its absence as a blocker. The phone is the only test device. See D21, D23, and B4 in `DECISIONS.md`, all three of which carry the correction.

---

## 7. This environment, so a fresh session does not rediscover it

**The shell does not carry state between tool calls.** Every command starts from a fresh environment. Two things bite immediately:

- **`ANDROID_HOME` is not set.** The SDK is at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and therefore **does not exist in a fresh clone**. Recreate it before the first build: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH.** It is at `/home/Kamsiob/Android/Sdk/platform-tools/adb`. Either export the path in each command or call it absolutely. `tools/screenshot.sh` already resolves it itself.

**The working directory contains a space and two leading dashes** in a parent folder name. Quote every path.

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. It refuses to capture unless the app is the focused window, checked immediately before and again after, because this is the owner's daily driver phone. It reads the theme from the device and refuses an argument that disagrees, per D31. Do not pass a theme; let it read one.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**Continuous integration has a problem worth knowing about.** The workflow triggers on `push` to main and on `pull_request`. **Pull request events stopped firing part way through 2026-07-31.** Six commits pushed to `feat/phase1-setup` after pull request #35 was opened produced no CI run at all, and the pull request showed no checks. Pushes to `main` still trigger, and so does `workflow_dispatch`.

The workaround, which was used before merging and which worked: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>` until the head sha matches what you pushed. **Do not read an absence of checks on a pull request as a passing build.** Trigger it and look. If this recurs, it is worth a few minutes on whether the repository has hit an Actions limit.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second and prints `BUILD SUCCESSFUL in 1s`. That is real. Confirm with `find app/build -name '<YourNewClass>*.class'` if it matters.

**Everything else worth knowing:** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

**Verification.** `tools/verify.sh` is the honest runner: it captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. `python3 tools/checks/run_all.py` runs the seven content and contract checks on their own. **Never chain a commit on a grep of output.** That mistake was made once and is why this sentence is here.

---

## 8. Screens built without a mockup

Every screen built without one is composed from existing components under `DESIGN.md` section 10, ships complete with every state, and is logged in three places at the moment it is built: a `needs-design-review` issue with a device screenshot, an entry in `DESIGN.md` section 8, and a line here.

This list exists so the owner can review them all in one sitting instead of archaeologically. **Never save these up for a phase gate.**

| Screen | Built | Issue | Composed from | Reviewed |
|---|---|---|---|---|
| Disclaimer gate | 2026-07-31, rebuilt same day to the section 10.6 bar | #28 | Mark, Display L, Body L, card 5.3, Display S, Body M, filled button | not yet |
| Essentials first setup | 2026-07-31 | #30 | Display L, Display S, Body M, text field 5.9, filled button, text action | not yet, **and it does not meet the section 10.6 bar** |
| Situation picker | 2026-07-31 | #32 | Display L, Body M, Body S, card 5.3, text action | not yet |
| Capture form, four kinds | 2026-07-31, rebuilt same day to screen 26 | #34 | Display L, Body M, Body S, text field 5.9, choice chip 5.11, filled button, text action | not yet |

The notebook table of contents is not in this list because it is drawn in the reference file. It is nonetheless the next thing to rebuild, for the reason in section 3.

**Known ahead:** the template library, the four template pickers, the template detail view, and the template editor. All of them land in Phase 1 or Phase 4 and none is drawn. `MASTER_SPEC.md` section 4.10 carries their requirements, including that all four template kinds share one presentation and that browsing, previewing, and applying must be visually distinct.

---

## 9. Persona runs

None yet. `TESTING-PERSONAS.md` requires each run to be recorded here with its fixture seed and date. A persona walked against a schema that has since changed has not been walked, and the deterministic fixture generator that would make a run meaningful is issue #17.

---

## 10. Open questions

The six in `MASTER_SPEC.md` section 10. Three are decided and recorded, three are not yet forced:

1. Tombstone retention window. **Decided: 730 days**, D11.
2. Whether the change log is exported. **Decided: yes, and the importer renumbers it**, D12.
3. Attachment size and count limits. **Decided: 25 MB each, a warning at 4 GB total, no hard ceiling**, D13.
4. Whether the web scaffold uses the same UI toolkit or a minimal one. Not forced until #16. `npm` is absent on this machine, which bears on it.
5. PDF pagination for very large exports. Not forced until Phase 5.
6. How the app describes its own translation status honestly, given no native speaker review has happened. The catalogs already carry `reviewed_by_native_speaker: false` and `check_i18n.py` prints it, so the app can say so plainly. The wording is not written yet and it affects the store listing and the README.

A contradiction inside `DESIGN.md` raised earlier is **resolved**: section 3 item 4 sets a 13sp minimum, and section 4.3 lists exactly two exemptions, the nav label and the Mono metadata style, each with its reasoning. Nothing else may be added to that list.

---

## 11. Subagents

`AGENTS.md` defines four. **Subagents never write anything**, per `CLAUDE.md` rule 8: they read, run, check, and report, and the main session acts on their reports. None was used this session.

---

## 12. Uncommitted work

**None.** Everything described in this file is committed on `feat/phase1-setup` and pushed to GitHub. This file is committed in the same commit as the work it describes.
