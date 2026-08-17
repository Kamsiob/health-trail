# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Written for a machine: fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-17, for the interface rebuild.

---

## 1. State

- Tree clean, all on `origin/main`. CI green at tip: `gh run list --branch main --limit 3`.
- 218 unit, 29 repo checks, lint, all green. **The instrumented suite has not been run clean end to end since the Today work**: it died at 430 on the Compose alpha, and the class that failed passes alone. Run it and read the counts before believing anything about it.
- **The APK is the last thing, not the next thing.** Owner, 2026-08-17: "the APK is after the full app is complete. all surfaces and widgets and buttons and text and styling and spacing and everything is updated to the material 3 expressive."
- Phone at baseline, stays plugged in: font scale 1.0, animator 1.0, no reader, night mode `no`.

## 2. The work

**The interface is being replaced on Material 3 Expressive.** `docs/V4.md` is the design authority and **its section 2.1 is the complete build manual**: a screen nobody drew is built from there, not guessed at. `docs/ACCEPTANCE.md` is the run, nine phases, and the run does not stop until every one is finished.

**The back end does not change.** Repository, schema, change log, export container, decryptor, fixtures, `contract/DATA-CONTRACT.md`. Anything needing a schema change is out of scope and goes to the owner.

**The method, owner ruling, not negotiable.** "No old design language at all. get rid of it so it doesn't influence." Nothing old is edited. A screen is **rewritten onto `ui/v4`** or left alone. An old component is deleted the moment its last caller goes. **The old package being empty is the test.**

### Where it stands

- **Step 1, the theme: done.** #385. `MaterialExpressiveTheme`, 48 color roles, the type ladder, five corners, reduced motion through `StillMotionScheme`.
- **Step 2, the shared surfaces: done.** #386. Face, icons, nav bar, blocks, rows, buttons, switch, sheet, forms. The accordion was closed rather than replaced, D185.
- **Phase 4, the screens: 21 of 85.** The notebook, the document, the care team, the person, medications, the questions, money, the milestones, the chapters, the care threads, the bill, the medication, the thread, the chapter, More, Appearance, the people, the paper picker, the violation, About, and **the fallback Today**.

**`ui/v4` holds:** `Page` + `labeledBlock`, `Block`/`BlockTone`, `FactBlock`, `Eyebrow`, `Lead`, `BigNumber`, `Body`, `ListRow`/`RowDivider`/`ChoiceRow`, `SearchDoor`, `PaperCard`, `Action`/`ActionEmphasis`/`IconAction`, `Sheet`/`SheetBody`/`rememberSheet`, `Avatar`/`PersonHero`/`PersonRow`, `Segments`, `Road`/`Stop`/`RouteMark`, `FieldBlock`.

**Also in `ui/v4` now:** `Trace` (+ `TraceHeight`), `Chip`, `StatBlock`, `NextBlock`/`InsetDoor`/`Face`.

**Still to write:** the switch row, and a grid cell for the documents pictures view.

### Next, in this order

**The owner's acceptance for this run: a complete app, every surface on Material 3 Expressive, and then an APK. No APK before that.** 2026-08-17.

1. **Finish the arranged Today.** `TodayFieldScreen` 2,600+ lines, `TodayCard` 458, `TodayLead` 190. The hero, the header, the card surfaces and the measure card are done; the per-type answer bodies, arrange mode's controls, and the card gallery are not. D191, D192, D193.
2. **The remaining screens**, roughly 64 of 85. All one shape: `Page` + `labeledBlock` + `ListRow`, with `Block`, `FactBlock`, `StatBlock`, `Trace`, `Chip`, `NextBlock` and `Road` where the content asks for them.
3. **Appointments and the documents list last of the lists**: each drags an old component in, `MonthGrid` and the thumbnail grid.
4. **More than one person in one notebook**, owner 2026-08-17, and it is a correctness job rather than a drawing one. All the testing so far has been on a single subject. What has to be proved: every query is scoped to the active subject and nothing leaks between them; export and restore keep them apart and do not merge or overwrite; **which subject is active is visible wherever it matters**; and **every write says plainly which profile it is going into**. Start with a sweep for repository reads and writes that take no `subject_id`.
5. Then phases 5 to 9 of `docs/ACCEPTANCE.md`, and the APK last.

### What the owner ruled on 2026-08-17, now rules

- **Three gaps and no more**: `withinGroup` 8, `betweenGroups` 24, `betweenZones` 32. D188.
- **Hints live under fields, never inside them**, at the `Support` 12sp role, 4dp down, aligned to the field's text. D189.
- **A person before their number**, and the name asked as a name.
- **Password, never passphrase**, everywhere a person reads it.
- **A label's weight and the air around it must agree.**
- **The spine is a path, never a filter.** D187.
- **Nothing behind a fold** that a label and a scroll can carry. D185.

### The card grammar, owner 2026-08-17, and it is not negotiable

**Apart from the hero, every widget is a half width square or a full width rectangle, and the height is the same either way.** The height is the square's side, derived in `TodayCard` and `TodayLead` from the grid: window width less two margins less one gap, halved. **Do not invent a size between them.** A card whose content does not fit shows less content and says what it is not showing; it does not grow. At `WIDE_TYPE_SCALE` and above the height becomes a floor, because the field has already reflowed to one column and clipping somebody's own words is rule 11's truncation.

**The hero is the exception and is a fixture**, not a card: not in the layout, not arrangeable, always drawn. Its foot is one row, one wide thing and up to two marks. D192.

### Traps this work has already paid for

- **A tonal surface is set in two places.** `TodayLead` and `TodayCard` both paint a `background` and then an `openableByTap(resting = ...)` over it. Changing only the first gives a block drawn in the old color with the new ink on it: dark on dark. D192.
- **The next appointment is "soonest of the future", not "soonest, if future".** Filtering after `minByOrNull` resolves a second year notebook to its first ever appointment and then to nothing.
- **`onNodeWithText` does not see a `contentDescription`.** Anything that speaks as one merged node, the inset door and every icon action, is found with `onNodeWithContentDescription`.
- **The instrumented suite dies around 430 tests on the Compose alpha.** Re-run the class alone before believing a failure; `NotebookScreenTest` failed in the suite and passed 14 of 14 by itself.

- **A road goes in one lazy item**, or the page's own air puts gaps in the line. And **it does not know which way time runs**: each half says whether it has been traveled.
- **`Page`'s bar is pinned**, because as a list item it scrolled away and left the gesture as the only way out.
- **A weighted trailing slot reserves its share**: a row has a `value` (weighted) and a `trailing` mark (not).
- **The caller's modifier lands on the list**, not the surface, or `performScrollToNode` finds no scrollable container.
- **`tools/seed.sh` taps the password field by `=Password`**, an exact match added to `walk.sh` on 2026-08-17. Layout changes to the restore screen break the seed; its last line says "Restored." or it did not finish.
- **A `SlotWriter` `ArrayIndexOutOfBoundsException` mid suite is the Compose alpha**, not the change under test. Re-run the class alone.
- **An eyebrow's words are capitals on screen and natural in the description.** Assert on the description.
- **A name inside a sentence is isolated, so the test expectation carries the marks too.** `today.masthead` renders `⁨Ruth⁩'s day`, and asserting on "Ruth's day" fails on a screen that is drawing it correctly.
- **A screen's own file name is not evidence of which screen ships.** Two Todays, one board entry, and the shell comment naming the live one was a year out of date. D191.

## 3. Reading ladder

Read on demand, never in bulk.

| Need | Read |
|---|---|
| What to do next | `gh issue view 321`, then this file |
| The design, and the order of work | **`docs/V4.md`** |
| What the approved design looks like | `docs/screenshots/m3v4-{0..5}-light.png`. **Open them** |
| What will bite me | `docs/TRAPS.md`, **one section**, from its own table |
| Why is it like this | `DECISIONS.md`, search a D number |
| What may the data do | `contract/DATA-CONTRACT.md` |
| What the app is for | `MASTER_SPEC.md` |
| Delegation, unattended runs | `AGENTS.md`, `RUN-SAFETY.md`. Subagents never write |
| How it got here | `docs/RUN-LOG.md`. **History. Never to orient** |

Precedence: verified code > `docs/V4.md` (visual) > this file > `DECISIONS.md` > `contract/DATA-CONTRACT.md` (data) > `RUN-SAFETY.md`/`AGENTS.md` > `PROJECT-DELTAS.md` > `MASTER_SPEC.md` > template.

`DESIGN.md` is superseded by `docs/V4.md` on anything visual. It still holds the accessibility gate (12).

## 4. Blocked, read before planning

**Nothing is blocked.**

## 5. Rules that get broken

1. `tools/verify.sh` is the only honest runner (compiles instrumented sources, runs lint).
2. **Read test counts, never the exit code.** A piped gradle run reports the pipe's status: a run that failed seven tests exited zero and was reported green. Counts are in `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`.
3. An issue closes only on device verification: both themes, font scale 2.0, RTL, every state including empty.
4. Commit and push per increment. An increment ends when `origin/main` has it.
5. The fixture must only produce rows the app itself can write.
6. Look at the screen before closing anything.
7. **A check passing is not the design being done.** A conformance command measures what it was told to measure and is silent on everything else.

## 6. Traps that cost real time (full set: `docs/TRAPS.md`)

- **Merged nodes**: a `DenseRow`/card testTag assertion passes when the line is absent. **Assert on words.**
- **`tools/seed.sh` walks the restore screen by text**, and taps the password field by `=Password`, an exact match. Seed at font scale 1.0, and **any layout change to the restore screen can break it**: its last line says "Restored." or it did not finish, and every capture taken after a failed seed is of an empty notebook.
- **`tools/walk.sh tap` matches the first node containing the word.** The capture button is described "Add something to the notebook", so tapping "Notebook" opens the capture sheet. Navigate the four destinations by nav bar position.
- `performScrollTo` fails on a pinned footer or non-scrolling parent. Drop the scroll.
- **`SectionScaffold` is a `LazyColumn`**, so `performScrollTo` cannot reach a control below the fold: scroll the list instead with `performScrollToNode`.
- Two `setContent` calls in one test → "already set content". Split the test.
- `live_entry` has no `rowid`. Order by `id`.
- **New optional parameters go after `modifier`** or lint `ModifierParameter` fails.
- A test that changes a remembered preference puts it back.
- `connectedDebugAndroidTest` uninstalls the app. Reinstall and reseed after a suite.
- Never put a short `timeout` on a device run; it kills mid-suite and uninstalls.
- The destructive-command hook matches prose. Writing certain verbs into a file is refused. #323.
- **Do not compile while the instrumented suite runs.**
- **A `SlotWriter` `ArrayIndexOutOfBoundsException` mid-suite is the Compose alpha, not the change under test.** It killed the process during a three class run on 2026-08-17 and every class passed alone straight after. Re-run the class before believing it.

## 7. The phone

- Pixel 8, `39151FDJH00506`, Android 17, USB. **A dedicated development device, not the owner's daily driver.** It stays plugged in.
- Baseline: font scale 1.0, animator 1.0, touch exploration 0, night mode `no`, no per-app locale. Rule 19 lets these change **only** if the prior value is recorded first and restored exactly.
- **Never screenshot**: the share sheet, the calendar app, any screen with a password field.
- `tools/screenshot.sh` refuses while the app theme is "Follow the phone" and the device night mode is `auto`.
- Chinese is `zh-Hans`, never bare `zh` (D52).
- Reinstall + reseed after a suite: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk` then `tools/seed.sh`.

Fixture variants:

    tools/device.sh year2 6 walk-year-three  --arranged
    tools/device.sh year2 6 walk-appointment --arranged --appointment-on YYYY-MM-DD
    tools/device.sh month6 6 walk-home       --situation home_family
    tools/device.sh month6 6 walk-quiet      --quiet

**The arranged fixture is the one that puts a chart in Today's lead.** The quiet seed leaves the lead a sentence, so a defect in the lead's chart is invisible in every capture taken against it.

## 8. Commands

**Look at the whole app in one command**, and do this before deciding what to fix:

    tools/sweep.sh audit            # seed once, walk every screen, capture each
    tools/sweep.sh --no-seed audit  # reuse what is already on the phone

**Written because the loop was the bottleneck.** Change one thing, reinstall,
reseed through the restore screen, tap back to the screen, capture, look:
ninety seconds per screen, so nobody ever did a pass and defects were found one
at a time in whatever order they were stumbled on. **Seed once, walk once.**

**`adb install -r` does not keep the notebook** on this build. It lands on the
first run screen, so a sweep that needs data seeds.

    python3 tools/checks/run_all.py     # 29 checks, seconds
    tools/verify.sh                     # honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest    # ~16 min
    ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>   # one class, <1 min

Reading an archive needs no phone:

    echo <passphrase> | python3 tools/decrypt/decrypt.py <archive> <folder>

A signed release, D160. No phone, no device step:

    cd android && ./gradlew :app:assembleRelease

**The key is at `~/.kamsiob/health-trail-release.jks`, and `android/keystore.properties` points at it.** Neither is in git and neither can be. **No key present builds unsigned rather than failing**, which is what CI gets.

**A debug build and a release build cannot upgrade each other.** Different keys, so swapping one asks for an uninstall, and an uninstall takes the notebook with it. Export first.

## 9. What is built, below the interface

**All of this is out of scope for the rebuild and none of it changes.**

- Foundation, four destinations, all section screens, 13 of 20 detail screens.
- Archive: two-layer container v3, readable copy in the person's language, standalone decryptor in CI, format published byte for byte. Stranger test passed on a machine that never had the app.
- Merge as well as replace, with a conflict screen. `Merge` is pure and unit tested.
- Export, wipe, restore round trip proven on the signed minified build, D165.

## 10. Live lists that must not be lost

**Screens composed rather than drawn** (rule 12): tracked by label, `gh issue list --label needs-design-review`. Do not maintain a second copy here.

**Reachable only from a test, not from any seed** (each said on its own issue):

- Paperwork an incident produced.
- The care team card's sparse rung; the trail spine's gap markers.
- The digest's corrected and removed counts.
- #273's two template hands, which the owner has not seen.

## 11. Facts a session re-derives if they are not written down

- **A classfile listing is not an API.** Kotlin's `internal` lives in the metadata, not the bytecode, so `javap` and `unzip -l` show an internal function as public. The only check that answers "can this build call it" is compiling against it. D179, and it cost the plan its central premise.
- 63 remote branches survive, all ancestors of `main`, none safe to delete without a ruling. **Never `git branch --merged`** here: squash-merge gives new shas (D144).
- Guard 2, the pre-compaction state save, has never fired and is unproven. Keep this file current by hand.
- D143: ten cross-references in `DECISIONS.md` and `docs/RUN-LOG.md` point at `DESIGN.md` sections that no longer exist. **They stay.**
- #308 is reopened. Its class shares state through one installed app; a green run proves nothing.
