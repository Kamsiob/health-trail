# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Written for a machine: fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-16, for the v4 overhaul. Prior version is in git history.

---

## 1. State

- **735 instrumented tests green**, 218 unit, 29 repo checks, lint. 2026-08-16.
- Tree clean, all on `origin/main`. CI green at tip: `gh run list --branch main --limit 3`.
- **Do not build a release APK or AAB.** Owner holds delivery until he approves the design. The debug install is the rule 21 loop and is not what he is holding.
- **The v4 overhaul is the work.** `docs/V4.md` is the plan and has authority over `DESIGN.md`. Progress is a command: `python3 tools/checks/check_v4.py`.
- **`check_v4.py` reports zero: 55 of 55 components, 85 of 85 screens.** It covers screens as well as components since 2026-08-16; it read only the 55 component files before that, which is 40 percent of the surface.
- **Conformance being zero is not the overhaul being done.** It measures four tells. The layers it cannot see are hierarchy, rule 11 states and motion, and those are the work now. Section 3.
- Phone at baseline, stays plugged in: font scale 1.0, animator 1.0, no reader, **night mode `no`** (this file said auto and the device says no). Holds the current build and the month6 fixture.

## 2. Reading ladder

Read on demand, never in bulk.

| Need | Read |
|---|---|
| What to do next | `gh issue view 321`, then this file |
| The design, and the order of work | **`docs/V4.md`** |
| What will bite me | `docs/TRAPS.md`, **one section**, from its own table |
| Why is it like this | `DECISIONS.md`, search a D number |
| What may the data do | `contract/DATA-CONTRACT.md` |
| What the app is for | `MASTER_SPEC.md` |
| Delegation, unattended runs | `AGENTS.md`, `RUN-SAFETY.md`. Subagents never write |
| How it got here | `docs/RUN-LOG.md`. **History. Never to orient** |

`DESIGN.md` is superseded where it disagrees with `docs/V4.md`. It still holds the accessibility gate (12), the component library (7) and the interaction grammar (9).

Precedence: verified code > `docs/V4.md` (visual) > this file > `DECISIONS.md` > `contract/DATA-CONTRACT.md` (data) > `RUN-SAFETY.md`/`AGENTS.md` > `PROJECT-DELTAS.md` > `MASTER_SPEC.md` > template.

## 3. The work

**#384 is done and closed. The work is layer 3 onward in `docs/V4.md` section 4.**

**What the mockup comparison found, and it is the method that matters.** Install, seed, capture, open the approved mockup beside it. Three defects on the notebook alone that no check could see: the app's own eyebrow was set in the same style as the row subtitles under it, its heading sat equidistant between its own group and the one above, and the table of contents had no way into search while Today, Today's field and More all did. **The eyebrow one had a KDoc describing uppercasing that the code never did.**

**Open against the mockups, not started:**

- **The two square cards on Today** carry an eyebrow at the top and a number at the bottom with a hand's width of white between. `TodayCard` sets `SpaceBetween` deliberately so a row of squares lines its answers along one baseline. It reads as empty rather than as calm, and the fix is the card's height, not its arrangement.
- **The chart card** is a line with a dot at every reading. The mockup draws one smooth line, a gradient under it and a single end dot.
- **The hero on Today** is a sentence on a slate block. The mockup carries a chip, a display title, a subtitle and an inset white action row inside the same block.
- **`TodayCard`'s eyebrow is sentence case and the mockup's is tracked caps**, and **this is an owner question rather than a defect.** D171 tried uppercase there and rejected it as louder than the answer beneath it, on a card the size of a stamp. Half of that reasoning was about mono being a third typeface, which D176 removed. The mockup's uppercase eyebrow is on a full width chart card, not a stamp. **Do not flip it without asking**, rule 23.

**#381 is eleven forms, not seven.** `SetupScreen` and `MeasurementScreen` are the only two using `FieldGroup`. The medication form is converted as the worked example: `Aside` for the lead, `ToggleRow` for the card question.

**The medication mockup's frequency chips are not built and need the owner.** Five chips plus "Say it another way" is roughly 24 catalog strings across four languages, including Arabic and Chinese phrasing next to medical words. Rule 24 forbids shipping English only.

**Component before screen.** A component fixed once is fixed on every screen that uses it. Screen by screen is what produced an overhaul three components deep out of fifty five while it was being called finished; the owner caught it on the care team folds.

Order, and none of it starts early:

1. **#384**, the fourteen components, batched by tell rather than by screen.
2. **The screens that arrange them**: hierarchy, not restyling. What leads, what recedes, what is grouped. Projects home, the Document screen, Trail, Progress, Money, Chapters and Threads have not been read against `docs/V4.md` section 1.
3. **Rule 11 states** on all 85 screens: empty, one, many, long text, RTL.
4. **#383**, polish and motion. Blocked until the check reports zero.

**Also open:** **#381** forms (setup and measurement done, seven to go), **#382** the edit mark awaiting the owner's eye, **#379** four items (project interior as a checklist, standing instructions, linking any item to a document, the More page), **#378** a document reopening at dropped resolution, **#369** five deep project screens.

**What the phone found that no test would have**, kept because the pattern repeats: a saturated hero read as a paragraph when it was a pale wash; six tinted cards of equal weight read as a rainbow; a square tile left an honest empty state marooned in white; a phone number broke across two lines on the screen whose promise is that the number is one tap away; the readable export printed a raw column name into an Arabic archive; the edit mark read as an eyedropper. **None of that is visible in source and none of it fails a test.**

## 4. Blocked, read before planning

**Nothing is blocked. B6 is resolved**, 2026-08-13, and it was the only entry here.

`NotebookShell` had 1,860 bytes under the JVM 64KB method limit and has **38,856**. 2.8 percent of a method, now 59. Measured on the class files rather than inferred from whether the next thing fit. `DECISIONS.md` B6 carries the numbers and the four attempts that failed first.

**What it means:** a new full-screen surface is affordable again, and so is a parameter on an existing screen. **#374's six uncorrectable records are unblocked** and are the proof worth taking next. `renameChapter` and `renameProject` exist with no caller and are deliberately **not** in the removal ledger.

**Two documents were wrong about the same thing and both are corrected.** This file and #373 said `PersonScreen`'s appointments were reverted and waiting on B6. They landed the night `RestoreFlow` moved out for #343. **A blocker's write-up goes stale the moment something else unblocks part of it**, which is why B6's proof ended up being a bytecode number rather than "see whether this fits".

**The shape that pays, since it took four attempts to find.** The shell's cost is the number of arguments crossing it. An extracted group takes `ui`, the repository, and nothing else it can read from a composition local: `strings` and `context` are `LocalStrings.current` and `LocalContext.current`, and **a composition local costs the call site nothing**. Extraction with eighteen arguments made it worse three times.

## 5. Rules that get broken

1. `tools/verify.sh` is the only honest runner (compiles instrumented sources, runs lint).
2. An issue closes only on device verification: both themes, font scale 2.0, RTL, every state including empty. `DESIGN.md` 16.4.
3. Commit and push per increment. An increment ends when `origin/main` has it.
4. The fixture must only produce rows the app itself can write. A fixture filling a column no writer fills is how a screen looks joined up and is empty.
5. Look at the screen before closing anything.
6. `tools/seed.sh` drives the restore screen; changing that screen breaks seeding. **So does raising the font scale.** It taps by position, and at 2.0 the restore screen's controls are somewhere else, so the run ends on the wrong screen. **Seed at font scale 1.0, then raise it.** The tell is the last line: it said "Back to More" rather than "Restored." and the notebook was empty. Found 2026-08-16 doing the rule 19 pass.

## 6. Traps that cost real time (full set: `docs/TRAPS.md`)

- **Merged nodes**: a `DenseRow`/card testTag assertion passes when the line is absent. **Assert on words.**
- `performScrollTo` fails on a pinned footer or non-scrolling parent. Drop the scroll.
- Two `setContent` calls in one test → "already set content". Split the test.
- `live_entry` has no `rowid`. Order by `id`.
- **New optional parameters go after `modifier`** or lint `ModifierParameter` fails. Cost 3 build failures in one day.
- A test that changes a remembered preference puts it back (view toggles, `Disclosure` state).
- Since 2026-08-13 `SectionScaffold` makes room for the keyboard, so a control below a field is genuinely off screen while it is up. Three tests close it before tapping.
- **`SectionScaffold` is a `LazyColumn`, so `performScrollTo` cannot reach a control below the fold**: the node does not exist yet, and the failure reads "could not find any node". Scroll the list instead: `onNodeWithTag(SectionTags.root(NAME)).performScrollToNode(hasTestTag(TARGET))`. A composed control whose center is outside the window is worse, because `performClick` succeeds and the tap lands nowhere, so the test fails on the value. **Seven tests hit both on the night the type ladder was lifted.**
- `connectedDebugAndroidTest` uninstalls the app. `adb shell pm list packages | grep kamsiob` before any walk, else taps land on the owner's launcher.
- Never put a short `timeout` on a device run; it kills mid-suite and uninstalls.
- The destructive-command hook matches prose. Writing certain verbs into a file is refused. #323, not a reason to weaken it.

## 7. The phone

- Pixel 8, `39151FDJH00506`, Android 17, USB. **The owner's test phone, not his daily driver.** He said so on 2026-08-15, correcting what this line and several D entries had assumed since the start. **His daily driver is a separate device this session never sees**, and it is where he installs a build to actually use it.
- **The care about this device stands anyway.** He uses it, personal content reaches its screen, and the screenshot guard writes into a public repository. D31, D43, D53 and rule 19 are unchanged: record before changing, restore exactly, never capture the share sheet or a password field. **The reason written in `tools/screenshot.sh` is now wrong and the guard it protects is not.** Do not relax it on the strength of this line.
- Baseline: font scale 1.0, animator 1.0, touch exploration 0, no per-app locale, app theme "Follow the phone". Rule 19 lets these change **only** if the prior value is recorded first and restored exactly.
- **It stays plugged in.** It is a dedicated development device, not his daily driver, so there is nothing to say about unplugging it. Corrected 2026-08-16.
- **Never screenshot**: the share sheet, the calendar app, any screen with a password field (the password manager puts its own bar in the shot).
- `tools/screenshot.sh` refuses while app theme is "Follow the phone" (device night mode is `auto`). Set Light or Dark, capture, set back.
- Chinese is `zh-Hans`, never bare `zh` (D52).
- Reinstall + reseed after a suite: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk` then `tools/seed.sh`.

Fixture variants:

    tools/device.sh year2 6 walk-year-three  --arranged
    tools/device.sh year2 6 walk-appointment --arranged --appointment-on YYYY-MM-DD
    tools/device.sh month6 6 walk-home       --situation home_family
    tools/device.sh month6 6 walk-quiet      --quiet

## 8. Commands

    python3 tools/checks/run_all.py     # 28 checks, seconds
    tools/verify.sh                     # honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest    # ~15 min
    ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>   # one class, <1 min

Test count: read from the root element of `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`. `verify.sh`'s summary table lists unit classes only.

`ps -eo pid,etime,cmd | grep '[v]erify.sh'` **as its own call**, printing lines not a count: run in the same compound command, the parent shell matches itself.

Reading an archive needs no phone:

    echo <passphrase> | python3 tools/decrypt/decrypt.py <archive> <folder>

A signed release, D160. No phone, no device step:

    cd android && ./gradlew :app:assembleRelease
    # -> app/build/outputs/apk/release/app-release.apk

**The key is at `~/.kamsiob/health-trail-release.jks`, and `android/keystore.properties` points at it.** Neither is in git and neither can be: `*.jks` and `keystore.properties` are both ignored, and the keystore is outside the tree. **No key present builds unsigned rather than failing**, which is what CI gets.

**A debug build and a release build cannot upgrade each other.** Different keys, so swapping one for the other asks for an uninstall, and an uninstall takes the notebook with it. Export first.

## 9. What is built

- Foundation, four destinations, all section screens: complete but for #182 (blocked).
- Detail screens: 13 of 20. Milestone 1 (Today) finished. Milestone 2 fully blocked. Milestone 3 blocked on #16/owner. **Milestone 4 is where the work is.**
- Archive: two-layer container v3, readable copy in the person's language, standalone decryptor in CI, format published byte for byte. Stranger test passed on a machine that never had the app.
- Merge as well as replace, with a conflict screen. `Merge` is pure and unit tested.
- v4 direction adopted; cards raised (`ui/theme/Raise.kt`), dark stays flat.

## 10. Live lists that must not be lost

**Screens composed rather than drawn** (rule 12: issue + `DESIGN.md` 14 + this file, at the moment of building). Twenty-nine, tracked by label: `gh issue list --label needs-design-review`. Do not maintain a second copy here.

**Reachable only from a test, not from any seed** (each said on its own issue):

- Paperwork an incident produced (fixture links no document to an incident).
- The care team card's sparse rung; the trail spine's gap markers.
- The digest's corrected and removed counts.
- #273's two template hands, which the owner has not seen.

**Owed on the device:** a screenshot for #359; a document saved with a real photograph (#362, picker opens the owner's library); the unfiled Today card branch.

## 11. Facts a session re-derives if they are not written down

- 63 remote branches survive, all ancestors of `main`, none safe to delete without a ruling. **Never `git branch --merged`** here: squash-merge gives new shas (D144).
- Guard 2, the pre-compaction state save, has never fired and is unproven. Keep this file current by hand.
- `RoundCard` has no caller and is **not** history: it waits on the #182 schema decision.
- D143: ten cross-references in `DECISIONS.md` and `docs/RUN-LOG.md` point at `DESIGN.md` sections that no longer exist. **They stay.**
- #308 is reopened. Its class shares state through one installed app; a green run proves nothing.
