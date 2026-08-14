# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Written for a machine: fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-14, at the owner's instruction to compress every document. Prior version archived whole at the foot of `docs/RUN-LOG.md`.

---

## 1. State

- Tree clean, everything on `origin/main`. Verify: `git status --porcelain`.
- **703 instrumented tests, 0 failures**, 2026-08-14. Plus 27 repo checks, 218 unit tests, lint. Nothing owed.
- CI green at tip. Check after each push: `gh run list --branch main --limit 3`.
- Phone at baseline, can be unplugged. Holds current build + month6 fixture.

## 2. Reading ladder

Read on demand, never in bulk.

| Need | Read |
|---|---|
| What to do next | `gh issue view 321`, then this file |
| What will bite me | `docs/TRAPS.md`, **one section**, from its own table |
| Why is it like this | `DECISIONS.md`, search a D number. Index at its top |
| What should it look like | `DESIGN.md`, numbered section. Index at its top |
| What may the data do | `contract/DATA-CONTRACT.md` |
| What the app is for | `MASTER_SPEC.md` |
| Delegation | `AGENTS.md`. Subagents never write |
| Long unattended runs | `RUN-SAFETY.md` |
| How it got here | `docs/RUN-LOG.md`. **History. Never to orient** |

Precedence: verified code > this file > `DECISIONS.md` > `contract/DATA-CONTRACT.md` (data) > `DESIGN.md` (visual) > `RUN-SAFETY.md`/`AGENTS.md` > `PROJECT-DELTAS.md` > `MASTER_SPEC.md` > template.

## 3. The work

**#375** is the live list: the owner's direction of 2026-08-14. The interface is better and still limited. Relevant information available from any point, nothing overly cluttered, smarter layout and organization, bugs found by using it. It carries the method and the do-not-undo list.

**#373** is B6, the unblocker. **#374** is the six records that cannot be corrected, blocked on #373.

#371, the five-panel audit, is closed: every item done but for what B6 blocks.

## 4. Blocked, read before planning

**B6. `NotebookShell` is at the JVM 64KB method limit.** No new full-screen surface. One added parameter on an existing screen also fails. Extraction has failed 3 times: the bytecode is at the call site, so moving a block out and passing it 18 arguments moves nothing. What worked: **fewer parameters** (3 lists → one `Repository.IncidentDetail`).

The worked-out pass is in `DECISIONS.md` B6. Order: (1) `ShellState` class for the 179 `var x by remember { mutableStateOf(...) }`; (2) `with(ui) { }` around the body, reindent, no reference rewritten; (3) **measure by restoring the 20 reverted lines giving `PersonScreen` its appointments**, stop and write it down if they don't fit; (4) only then extract overlays in groups.

B6 blocks: correcting a chapter, a project, a reading, a measure, a question's words, an instruction's words. `renameChapter` and `renameProject` exist with no caller and are deliberately **not** in the removal ledger.

## 5. Rules that get broken

1. `tools/verify.sh` is the only honest runner (compiles instrumented sources, runs lint).
2. An issue closes only on device verification: both themes, font scale 2.0, RTL, every state including empty. `DESIGN.md` 16.4.
3. Commit and push per increment. An increment ends when `origin/main` has it.
4. The fixture must only produce rows the app itself can write. A fixture filling a column no writer fills is how a screen looks joined up and is empty.
5. Look at the screen before closing anything.
6. `tools/seed.sh` drives the restore screen; changing that screen breaks seeding.

## 6. Traps that cost real time (full set: `docs/TRAPS.md`)

- **Merged nodes**: a `DenseRow`/card testTag assertion passes when the line is absent. **Assert on words.**
- `performScrollTo` fails on a pinned footer or non-scrolling parent. Drop the scroll.
- Two `setContent` calls in one test → "already set content". Split the test.
- `live_entry` has no `rowid`. Order by `id`.
- **New optional parameters go after `modifier`** or lint `ModifierParameter` fails. Cost 3 build failures in one day.
- A test that changes a remembered preference puts it back (view toggles, `Disclosure` state).
- Since 2026-08-13 `SectionScaffold` makes room for the keyboard, so a control below a field is genuinely off screen while it is up. Three tests close it before tapping.
- `connectedDebugAndroidTest` uninstalls the app. `adb shell pm list packages | grep kamsiob` before any walk, else taps land on the owner's launcher.
- Never put a short `timeout` on a device run; it kills mid-suite and uninstalls.
- The destructive-command hook matches prose. Writing certain verbs into a file is refused. #323, not a reason to weaken it.

## 7. The phone

- Pixel 8, `39151FDJH00506`, Android 17, USB. **The owner's daily driver.** No emulator (D21, D23, B4).
- Baseline: font scale 1.0, animator 1.0, touch exploration 0, no per-app locale, app theme "Follow the phone". Rule 19 lets these change **only** if the prior value is recorded first and restored exactly.
- Say when it can be unplugged. Most work needs no device.
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

    python3 tools/checks/run_all.py     # 27 checks, seconds
    tools/verify.sh                     # honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest    # ~15 min
    ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>   # one class, <1 min

Test count: read from the root element of `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`. `verify.sh`'s summary table lists unit classes only.

`ps -eo pid,etime,cmd | grep '[v]erify.sh'` **as its own call**, printing lines not a count: run in the same compound command, the parent shell matches itself.

Reading an archive needs no phone:

    echo <passphrase> | python3 tools/decrypt/decrypt.py <archive> <folder>

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
