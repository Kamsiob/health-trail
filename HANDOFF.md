# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Written for a machine: fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-14, at the owner's instruction to compress every document. Prior version archived whole at the foot of `docs/RUN-LOG.md`.

---

## 1. State

- Tree clean, everything on `origin/main`. Verify: `git status --porcelain`.
- **722 instrumented tests, 0 failures**, 2026-08-14. Plus 28 repo checks, 218 unit tests, lint. Run alone on a clean device: two runs at once produce `DELETE_FAILED_INTERNAL_ERROR` and failures in unrelated tests.
- CI green at tip. Check after each push: `gh run list --branch main --limit 3`.
- Phone at baseline and **can be unplugged**: font scale 1.0, animator 1.0, no reader, night mode auto, all four read back after being changed. Holds the current build and the month6 fixture.
- **Looked at on the phone**, not only tested: Today at scale 1.0 and 2.0, Projects, a project's own screen, the trail, the card gallery, two forms. Screenshots in `docs/screenshots/`: `today-*`, `projects-list-*`, `sweep-*`, `form-*`, `add-card-gallery-*`.

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

**#376** is the live list: the owner's direction of 2026-08-13, overnight, after using the build. **The acceptance criterion is a shippable app**, not a green run and not a correct screen. It names six failures and quotes him on all of them.

1. The text is hard to read. **Done**, D154: the whole ladder lifted, body 14sp to 16.
2. Today is hard to use. **Mostly done** through 3 and 4, plus the clutter: the word "Options" off every card, the capture button standing down while arranging, Done given weight.
3. **A card is square or full width, and he had said so more than once.** Done, D153, and the square fills itself.
4. Adding, removing and changing cards **must mimic the phone's own home screen**. Done: hold to arrange, carry the card itself, others move aside, back keeps it, and **the gallery offers the real card rather than a line describing it** (D157).
5. **The projects overhaul.** First pass done, D156: the road says its stage names and the subtitle counts your projects. **Not done: "+ Start" belongs at the top per the grid**, and it wants a decision because D137, D118 and the FAB clearance are behind where it is.
6. **No data entry screen is boring or lazily designed.** **Two of fourteen done.** `AddPersonScreen` asked five questions at one weight while D147 recorded it as converted. `EmergencyCardEditScreen` said "Who to call first" twice within three lines, once as a mono header and once as a body label.

   **Not a sweep for folding**, and the count on #376 is why: four forms were already staged and seven are too short to need it. The two done needed two different things: a fold, and a duplicated heading taken out.

   **The biggest thing left on item 6 is one decision, not a sweep.** Four of #374's correction surfaces, plus the care thread rename, are one question on an otherwise empty screen: a chip, a title, a lead, a field, then two thirds of a Pixel 8 doing nothing. **A one question correction may be sheet shaped rather than screen shaped**, and the app already answers a question in a sheet in four places. Against it: `AddThreadScreen` also starts a thread from nothing, and D151 gives it a chip and a `displayM` title a sheet does not wear. Written up on #376. The emergency card also **drew its whole care team as chips**, fifteen names filling the first screen while `CHIP_CAP` in `Chips.kt` caps a chip set at five; `cappedChips` could not serve it because it takes one selection and the card takes a set, so there is a multi select cap now and **nobody already on the card is ever folded away**. `EmergencyCardEditScreenTest` is its first test.

**Everything above was looked at on the phone**, not only tested. **5 and 6 are where the work is**: twelve project screens and thirteen forms have not been through this.

**What the phone found and no test would have.** The square was right and still wrong: the answer sat in the top third and two thirds was empty white, rule 11's blank area. **The chevron then landed in the middle of the card, and then in the wrong corner**, over two builds, because `align` says where a box goes and not how big it is. The capture button covered the words on the card beneath it at font scale 2.0. The word "Options" appeared on five cards at once. **A project's road was four dots on a dashed line**, which is a progress bar and is the one thing rule 13 rules out. **The gallery was seventeen rows of words** describing cards instead of showing them. None of that is visible in source and none of it fails a test.

**Still owed on Today, and it is the owner's to weigh.** At font scale 2.0 the capture button covers the words under it: "Levothyroxine · 50 mcg, morn" runs under the FAB. The list has clearance at its end, so this is the button floating over mid content rather than a missing inset, and it predates the type lift. Moving the FAB or insetting every card is a decision rather than a repair.

**#375** was the previous direction and #376 supersedes it as the live list. Its four asks are not withdrawn.

**#374 is done. All six records can be corrected**, 2026-08-14: a chapter's name, a project's name, a question's own words, a standing instruction's own words, a reading, and a measure's name and unit. The first four reuse the one question screen, which now takes `leadKey`, `section`, `initialName` and `singleLine`. The reading and the measure reuse the forms that created them, because two forms for one record is how two forms drift apart.

**They are also B6's real proof.** Both full screen surfaces together cost the shell **220 bytes**, against the 1,860 bytes of headroom it had before #373, when one added parameter on an existing screen was enough to fail the build. **#373** is B6 and is done.

**Each correction is reached from the thing itself**, per rule 17 and #374's own acceptance: the reading's from its row on the progress screen, the measure's from under its chart, the question's and the instruction's from their sheets, the two names from their own screens. **All six walked on the phone**, light, scale 1.0. **Three had a defect every test passed**: a save button repeating the words of the control that opened it, a correction wearing the recording lead, and a control sitting under the wrong measure. All wrong words or wrong placement, which is the class a test cannot see.

**Since covered**: the reader walk on four of them in `ScreenReaderTest`, and RTL against a forced layout direction plus font scale 2.0 in `CorrectionsHoldUpTest`. **The kind assertion there was watched failing** against `kindIsFixed` set back to false, because the chip and the sentence carry the same words and asserting the words alone would have passed either way.

**Still owed**: dark theme, and the same look at the other project screens. A test says a screen holds together; it cannot say whether it reads.

**The whole of #374 cost the shell 2,919 bytes.** Before #373 the shell had 1,860 bytes of headroom and one added parameter on an existing screen failed the build. Six full screen surfaces later there are **35,937 bytes** left. That is what B6 was worth, and it is a better number than any taken on the bytecode alone.

#371, the five-panel audit, is closed.

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
6. `tools/seed.sh` drives the restore screen; changing that screen breaks seeding.

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

    python3 tools/checks/run_all.py     # 28 checks, seconds
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
