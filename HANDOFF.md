# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-18, second Material rebuild session.

---

## 0. Cold start

1. `gh issue view 321`, then this file. Nothing else.
2. `tools/sweep.sh audit`, **look at the captures**.
3. Take the next screen. **New file, built from Material's components outward, old file deleted in the same commit.** One screen per commit: `tools/verify.sh`, install, look on the phone, commit, push.
4. Do **not** read the old design docs first. Do **not** re-measure the mockups.

Worked examples, in order of quality: `TodayField.kt`, `Notebook.kt`, `OneThread.kt`, `MedicationList.kt`.

## 1. State

- Tree clean, all on `origin/main`. 218 unit, **30 checks**, lint green.
- **`ui/components` is 32 files.** Retired this session: `EdgeScrubber` to
  `ui/v4/Rail.kt`, `ScopedSearch` to `ui/v4/Search.kt`, `ConfirmRemoveSheet` to
  `ui/v4/Confirm.kt`. `Entrance` deleted outright: nothing called it.
- **The folder cannot empty, and D199 settles what the target is instead.**
  Three frozen files import from `ui/components`, and a frozen file is never
  called, never extended and never fixed. **The test is whether any live file
  imports from it, not whether the folder is empty.** `StepRow` and `Tile` have
  no live caller at all and now have ledger rows; do not rewrite either.
- **Before retiring a component, ask who calls it excluding the three frozen
  files**: `ProjectDetailScreen.kt`, `CaptureSheet.kt`, `PinnedGroup.kt`.
- **Instrumented: not run end to end.** Read counts from the XML, never a
  gradle exit code: the run below exited 0 with a failure in it.
  - 2026-08-18, second session: `MedicationsScreenTest` 4, `RemovalIsVisibleTest` 18, `ReaderStopsTest` 4 with **1 failed**.
  - Earlier: `TodayFieldScreenTest` 40, `NotebookScreenTest` 14, `ScreenReaderTest` 109, `AddCardOffersTest` 11, `MedicationQuestionJourneyTest` **2 failed**.
- **#391, `ReaderStopsTest` expects 12 notebook rows carrying their purpose in
  one stop and finds 10.** Pre-existing, not from the retirement work: the test
  renders `NotebookScreen` directly and nothing on its path changed. Rule 19
  makes it a gate.
- **Known failure**: `MedicationQuestionJourneyTest` dies at `capture_form_more_medications`. Not from this session's changes; it now reaches further than it used to.
- **#390, three live screens take a callback and never call it.** Held by
  `check_uncalled_callbacks.py`, which names them rather than counting them, so
  the list can only shrink. A fourth, `MedicationRow`'s `onOpen`, was found and
  fixed: no row on the medications list opened at all.
- **No APK yet.** Owner: the APK is after the app is complete.
- **Not started**: more than one person per notebook; restore tested across profiles.
- Phone at baseline, plugged in: font 1.0, animator 1.0, no reader, night mode `no`.

## 2. The direction, and it is not negotiable

**The old design is deleted. The new one is built out of Material 3 Expressive's own components and Google's assets. D196.**

**Material 3 Expressive is the baseline, not the finish.** Owner, 2026-08-18: the design is "a 7 out of 10, we need a 10 out of 10... it doesn't have that extra love and care and attention to detail and polish and visual components that represent a top tier app worthy of recognition for design and functionality and creativity." A screen that is correct, on Material's components, and plain **is not done**. `docs/V4.md` section 6 is the bar.

| This | Not this |
|---|---|
| `ListItem`, `Card`, `Scaffold`, `LargeFlexibleTopAppBar`, `FilterChip`, `AssistChip`, `SegmentedButton`, `OutlinedTextField`, `Switch`, `ShortNavigationBar`, `ModalBottomSheet`, `FloatingActionButton`, `IconButton`, `Icon` | a hand built `Row`/`Box` that looks like one |
| `MaterialTheme.colorScheme`, `typography`, `shapes` | a hex or a second ladder |
| Material Symbols through `Symbols` | an authored glyph |
| **Our polish on top**: color identity, copy, arrangement, motion, what leads | re-deriving the control |

**Do not patch an old screen. Delete the file and write a new one.** Swapping innards under a file written for the old language keeps the old bones.

**Do not trace the mockups.** `docs/screenshots/m3v4-*` are arrangement and hierarchy only. **If a component has to be measured against a picture, replace it with Material's.**

**The back end does not change.** Repository, schema, change log, export, decryptor, fixtures, `contract/DATA-CONTRACT.md`.

### Color, D198, and it is app-wide

1. A mark is `TabHue.base` with `TabHue.onBase` on top. Pass `markHue` / use `HueMark`, never a loose ink and wash.
2. Surfaces stay neutral. **No page is overwhelmingly one color** (owner, 2026-08-18).
3. One tonal block per page, never full height.
4. Entry lists carry the kind's mark in the kind's color: `entryHue` / `entryMark`.
5. Identity, never state. Rule 2. `hueFor` is the owner's mapping.
6. A color never carries meaning alone: mark plus words.

### Stays hand drawn, four exceptions

The trail's route, a project's road, a measure's line (`Trace`), and `DatePicker` (D197: Material's cannot express EDTF precision or unknown, rule 17).

## 3. What is left, and the order

1. **The rest of `ui/components`**, `gh issue view 387`. 32 files, and read D199
   first. Live callers, counted excluding the three frozen files:

   | Next, biggest hand built first | Live callers |
   |---|---|
   | `Dictate` 244 (needs the Material mic symbol) | 3 |
   | `Tips` 216, the sheet half | 7 |
   | `MonthGrid` 201 | 3 |
   | `ChipPicker` 185 | 5 |
   | `DateRow` 168 | 1 |
   | `Thumbnail` 162 | 5 |
   | `HeaderAction` 161 | 5 |
   | `StickyHeader` 147 | 1 |
   | `TabChip` 137, `Share` 134, `Hero` 123, `StandingCard` 117, `LatestWordCard` 115, `SectionIcon` 115, `CalendarHandoff` 107, `Stages` 96, `ReferenceLine` 99, `ViewPreference` 73, `Chevron` 69, `FabClearance` 53, `DraftSavers` 31 | 1 to 4 each |
   | `RoadStrip` 372, `Spine` 401 | the road and the route stay drawn; what is a `Box` around them does not |

   **`Press` 254 is last**, and it shrinks as the screens do: 32 live files use
   `openableByTap` or `pressedSurface`, and every one of them should be on
   Material's own state layer instead. `BottomNav` moves without redrawing.
   `Symbols` stays, it is the catalog. `DatePicker` stays, D197.
   **`StepRow` and `Tile` are frozen-tail: leave both.**
2. **The remaining screens still on old bones.** Measured rather than guessed,
   2026-08-18: **78 of 86 screens carry at least one of the five tokens, about
   29,000 lines.** The grep is a proxy and the tail lies: forty of those carry
   one or two stray `HealthTrail.colors` references and are otherwise Material.
   **The genuinely old ones are the top twenty by hit count**, led by
   `IncidentScreen` 621L, `StandingInstructionsScreen` 477L,
   `MeasurementScreen` 861L, `UnfiledTrayScreen` 531L, `TrailScreen` 953L,
   `ProjectDetailScreen` (frozen, skip), `SearchScreen` 354L, `PrepScreen`
   510L, `EntryScreen` 476L, `TodayScreen` 551L. Re-run the count with the
   script in `docs/RUN-LOG.md` rather than reading this list as fixed.
3. **The polish pass**, `docs/V4.md` 6, every screen against the 10/10 bar. #388.
4. **More than one person per notebook.**
5. **The full instrumented suite clean**, read from the XML.
6. **Backup and restore across multiple people and profiles.** #389.
7. **The APK.** Last.

## 4. Blocked

**Nothing is blocked.**

## 5. Rules that get broken

1. `tools/verify.sh` is the only honest runner (compiles instrumented sources, runs lint).
2. **Read test counts from the XML, never a gradle exit code.** `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`.
3. An issue closes only on device verification: both themes, font scale 2.0, every state including empty.
4. An increment ends when `origin/main` has it.
5. The fixture must only produce rows the app itself can write.
6. **Look at the screen before closing anything.**
7. A check passing is not the design being done.

## 6. Traps that cost real time (full set: `docs/TRAPS.md`)

- **The caller's `testTag`, the tap and the reader's sentence must be on one node.** Put `combinedClickable` and `semantics { contentDescription }` on the `Card`'s own modifier, not on a column inside it. Twenty two tests read an empty description off the node they were handed.
- **This scheme's `surfaceContainerLow` is the canvas in light.** A card drawn on it is invisible. Use `surfaceContainer`.
- **A row that promises a door may not have one.** `MedicationRow` took an
  `onOpen` and never called it, and the whole medications list had two clickable
  nodes on it. `check_uncalled_callbacks.py` holds this now. **Dump the screen
  and count clickable nodes**; it is one line and it does not lie the way
  reading the file does.
- **Three files are frozen and must not be edited**, `docs/REMOVAL-LEDGER.md`:
  `ProjectDetailScreen.kt`, `CaptureSheet.kt`, `PinnedGroup.kt`. Retiring a
  component they import means repointing their import, which is extending a
  frozen screen. D199 says what to do instead.
- **`git rm` stages immediately.** Run it at the moment you commit, not when you
  start the rewrite, or the deletion lands in whatever commit goes out next and
  that commit does not build.
- **A `LazyColumn` does not compose off-screen rows.** Tests that walked a scrolling `Column` break: scroll by the list's own item key, `performScrollToKey`.
- **A floating action button is on the scaffold, not in the list.** Do not scroll a list to reach it.
- **`tools/seed.sh` walks the restore screen by text** and taps the password field by `=Password`. Its last line says "Restored." or every capture after it is of an empty notebook.
- **`tools/walk.sh tap` matches the first node containing the word.** Navigate the four destinations by nav bar position: x = 133 / 400 / 670 / 940, y = 2252.
- **Screenshot coordinates are not device coordinates.** `tools/screenshot.sh` crops 132px of status bar; add it back before `adb shell input tap`.
- `onNodeWithText` does not see a `contentDescription`. Use `onNodeWithContentDescription`.
- **A `SlotWriter` `ArrayIndexOutOfBoundsException` mid-suite is the Compose alpha.** Re-run the class alone.
- **A scan that reads declaration names calls an extension function dead.** `fun Modifier.arrivesInOrder` scans as `Modifier`. `git show HEAD:<path> > <path>` brings a file back; rule 6 needs no destructive command.
- **A road goes in one lazy item**, or the page's air puts gaps in the line.
- **New optional parameters go after `modifier`** or lint `ModifierParameter` fails.
- `connectedDebugAndroidTest` uninstalls the app. Reinstall and reseed after a suite.
- **Do not compile while the instrumented suite runs.**
- The destructive-command hook matches prose. Some verbs cannot be written into a file. #323.

## 7. The phone

- Pixel 8, `39151FDJH00506`, Android 17, USB. **A development device, not the owner's daily driver.**
- `adb` is not on `PATH`: `/home/Kamsiob/Android/Sdk/platform-tools/adb`.
- Baseline: font 1.0, animator 1.0, touch exploration 0, night mode `no`. Rule 19 lets these change **only** if the prior value is recorded first and restored exactly.
- **Never screenshot**: the share sheet, the calendar app, any screen with a password field.
- Reinstall + reseed after a suite.

Fixture variants:

    tools/device.sh year2 6 walk-appointment --appointment-on YYYY-MM-DD   # the usual one
    tools/device.sh year2 6 walk-year-three  --arranged
    tools/device.sh month6 6 walk-home       --situation home_family
    tools/device.sh month6 6 walk-quiet      --quiet

**The arranged fixture is the one that puts a chart in Today's lead.**

## 8. Commands

    tools/sweep.sh audit                # seed once, walk every screen, capture each
    tools/sweep.sh --no-seed audit      # reuse what is on the phone
    python3 tools/checks/run_all.py     # 29 checks, seconds
    tools/verify.sh                     # the honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest                  # ~16 min
    cd android && ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>             # one class, <1 min
    cd android && ./gradlew :app:assembleRelease                            # signed, D160

**`adb install -r` does not keep the notebook on this build.** A sweep that needs data seeds.

Reading an archive needs no phone:

    echo <passphrase> | python3 tools/decrypt/decrypt.py <archive> <folder>

**The key is at `~/.kamsiob/health-trail-release.jks`**, `android/keystore.properties` points at it, neither is in git. No key present builds unsigned rather than failing. **A debug build and a release build cannot upgrade each other**: export first.

## 9. Below the interface, and none of it changes

Foundation, four destinations, all section screens, 13 of 20 detail screens. Archive: two-layer container v3, readable copy, standalone decryptor in CI, format published byte for byte, stranger test passed. Merge as well as replace, with a conflict screen. Export, wipe, restore round trip proven on the signed minified build, D165.

## 10. Reading ladder

Read on demand, never in bulk.

| Need | Read |
|---|---|
| What to do next | `gh issue view 321`, then this file |
| The design and the polish bar | **`docs/V4.md`** |
| What the approved design looks like | `docs/screenshots/m3v4-{0..5}-light.png`. **Open them** |
| What will bite me | `docs/TRAPS.md`, **one section**, from its own table |
| Why is it like this | `DECISIONS.md`, **search a D number** |
| What may the data do | `contract/DATA-CONTRACT.md` |
| What the app is for | `MASTER_SPEC.md` |
| Delegation, unattended runs | `AGENTS.md`, `RUN-SAFETY.md`. Subagents never write |
| How it got here | `docs/RUN-LOG.md`. **History. Never to orient** |

Precedence: verified code > `docs/V4.md` (visual) > this file > `DECISIONS.md` > `contract/DATA-CONTRACT.md` (data) > `RUN-SAFETY.md`/`AGENTS.md` > `PROJECT-DELTAS.md` > `MASTER_SPEC.md` > template.

`DESIGN.md` is superseded by `docs/V4.md` on anything visual. It still holds the accessibility gate, section 12.

## 11. Facts a session re-derives if they are not written down

- **A classfile listing is not an API.** Kotlin's `internal` lives in the metadata, so `javap` shows it public. Only compiling against it answers "can this build call it". D179.
- 63 remote branches survive, all ancestors of `main`. **Never `git branch --merged`** here: squash-merge gives new shas. D144.
- Guard 2, the pre-compaction state save, has never fired. Keep this file current by hand.
- D143: ten cross-references point at `DESIGN.md` sections that no longer exist. **They stay.**
- #308 is reopened. Its class shares state through one installed app; a green run proves nothing.

## 12. Live lists that must not be lost

**Screens composed rather than drawn** (rule 12): `gh issue list --label needs-design-review`. No second copy here.

**Reachable only from a test, not from any seed** (each on its own issue): paperwork an incident produced; the care team card's sparse rung and the trail spine's gap markers; the digest's corrected and removed counts; #273's two template hands.

**Not yet seen on a phone**: the empty-screen mark (`EmptyDrawing`), because neither fixture has an empty section. Look at it on the next fresh-notebook walk.
