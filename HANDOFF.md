# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-18, end of the visual polish session.

---

## 0. Cold start

1. `gh issue view 321`, then this file. Nothing else.
2. **The visual polish phase is done and the owner opened a new one.**
   `gh issue view 321` says the order. Do not re-audit anything before it.
3. Check the phone is unlocked before planning device work:
   `adb shell dumpsys window | grep isKeyguardShowing`. **Never switch Android
   user profiles**; it re-locks the phone and there is no way past a secure
   keyguard from here, #316.
4. `tools/sweep.sh audit`, and **look at the captures**.
5. One unit per commit: `tools/verify.sh`, install, look on the phone, commit, push.

Worked examples, best first: `ui/v4/Arrival.kt`, `ui/v4/Press.kt`,
`ui/v4/MonthGrid.kt`, `screens/MedicationList.kt`, `screens/Notebook.kt`.

## 1. What is done, and do not redo any of it

| | | |
|---|---|---|
| Step 3 | #387 | **Done.** No live file imports from `ui/components`. Eight files left, all frozen tail or `Symbols`/`DatePicker`, each with a `docs/REMOVAL-LEDGER.md` row. |
| Step 4 | #392 | **Done and closed.** Every screen is on Material's own state layer through `Surface`, `Card`, `FilterChip` or `opensOnTap`. No screen animates a resting color by hand, animates a focus border, or passes `indication = null`. The arrangement tail it handed to #388 is what that phase spent its day on. |
| Step 5 | #390 | **Done.** All three callbacks wired, `KNOWN` in `check_uncalled_callbacks.py` is empty. |
| Step 6 | #393 | **Done and verified end to end.** Three people in one notebook, no leakage. Every table has carried `subject_id` since Phase 0, so nothing in the schema changed. |
| Step 7 | #394 | **Measured twice.** 750 tests, 14 failed on 2026-08-18 after the polish phase, every one a class #394 already names: the 13 pre-existing plus #391. Nothing new. |
| Step 8 | #389 | **Done.** All six items proved on the phone with three people. |
| Step 9 | #388 | **Done.** The bar is separated, the selected destination is the app's own mark, every empty state is one designed block, and findings 2 through 8 are answered or resolved. D201, D202. |
| Step 10 | #395 | **Every precondition is met and it is the next thing to do.** The release build works and is signed; it needs rebuilding from current `main`, installing, and walking. |

**"Multiple users and profiles" in #389 means people inside one notebook**, which
is #393. It does not mean a second Android user on the device. Owner, 2026-08-18.
Reading it literally cost a locked phone and proved nothing.

## 2. What is left, and the owner opened it on 2026-08-18

**`FINISH THE APP` is finished except for delivering the APK, #395.** The
visual polish phase closed the same day it started, and the owner then named
three bodies of new work in his own words. They are milestone **8. Notes,
projects and progress**:

| | |
|---|---|
| **#399** | **The projects section is reimagined, not repainted.** The one place the "enhance, do not replace" rule does not apply, and the owner said so himself. Eleven project screens is the finding before any of them is looked at, rule 20. `ProjectDetailScreen` is frozen and gets replaced rather than extended. |
| **#398** | **Every tracked thing gets its own screen.** Progress is one page trying to be every measure at once: the hero gets a plot, every other measure gets a number, and one flat list mixes every measure's readings together. A measure card becomes a door. Rule 2 is the whole risk here. |
| **#397** | **Notes, general and attached.** A note with no target, a note attached to an incident, a question, a visit or a person, and light rich text. Rule 18 both ways is the hard half, and the storage is a `contract/DATA-CONTRACT.md` decision before it is a screen. |
| **#400** | **The archive carries everything, with two people in one notebook.** The gate on #397 and #398 rather than a task after them: notes, every tracked thing, a mother and a father each fully used, export, wipe, restore, and a merge with a conflict, on the signed minified build. D24 calls the export the only way back. |

**#396** is the dictation rule, split out of #388 finding 4: 34 `Field` call
sites, a rule that is checkable, three of them inside a frozen file.

**Two conditions the owner set on all of milestone 8**, and both are on each
issue:

1. **The archive is a gate.** Nothing closes until its rows are in the
   container, the standalone decryptor reads them, and two people in one
   notebook with every area populated survive the round trip. #400.
2. **Material 3 Expressive first, then the same polish pass.** Each screen is
   built on Material's own components, D196, then taken through `docs/V4.md`
   6.1's eleven items with 6.2's pass on a real capture, which is the loop #388
   just ran. **Consistent where appropriate**: D198 color, D201 the bar, D202
   the selected destination, D203 actions, D204 a tracked thing's hue, and
   `SectionEmpty` for every empty state. A new screen inventing its own version
   of any of those is what rule 16 calls two answers to one question.

**The owner's own words, 2026-08-18**, in order: "I don't want the taskbar
area ... to blend into any content above it"; "the little yellow oval
highlight for the active tab in the taskbar looks ugly and lazy. not polished
and premium"; "one style across all empty pages with the button inside the
card"; "'nothing here is worked out by the app'? remove that from anywhere it
appears"; "the whole project thing needs to be revisualized and reimagined".
The first four are done. The last is #399.

## 3. Blocked

**Nothing is blocked.** The phone is unlocked and at baseline: font 1.0,
animator 1.0, no reader, night mode `no`, appearance "Follow the phone".

## 4. The direction, and it is not negotiable

**Material 3 Expressive is the floor, not the finish.** Built out of Material's
own components and Google's own assets, D196, with our polish on top: color
identity, copy, arrangement, motion, what leads. **Nothing here is being
replaced.** The rebuild is finished; #388 enhances it.

| This | Not this |
|---|---|
| `ListItem`, `Card`, `Scaffold`, `LargeFlexibleTopAppBar`, `FilterChip`, `AssistChip`, `SegmentedButton`, `OutlinedTextField`, `Switch`, `ShortNavigationBar`, `ModalBottomSheet`, `FloatingActionButton`, `IconButton`, `Surface`, `HorizontalDivider` | a hand built `Row`/`Box` that looks like one |
| `MaterialTheme.colorScheme`, `typography`, `shapes` | a hex, or `HealthTrail.type` / `Radius.` where a Material role exists |
| Material Symbols through `Symbols` | an authored glyph or a `Canvas` |

**material3 is 1.5.0-alpha26**: no `ButtonGroup`, no `SplitButton`, no
`ToggleButton`. Expressive components need
`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

**Four things stay hand drawn**: the trail's route (`ui/v4/Route.kt`), a
project's road (`ui/v4/RoadStrip.kt`), a measure's line (`ui/v4/Trace.kt`), and
`DatePicker` (D197). `StageDots` is a fifth and is not a progress bar.

### Color, D198, app-wide

1. A mark is `TabHue.base` with `TabHue.onBase` on top. Never a faded base.
2. Surfaces stay neutral. **No page is overwhelmingly one color.**
3. One tonal block per page, never full height.
4. Entry lists carry the kind's color. **Never a second mapping.**
5. Identity, never state. Rule 2. `hueFor` is the owner's mapping.
6. A color never carries meaning alone: the mark and the words together.

## 5. What this session added that the next one builds on

- **`navSurface` and `Radius.navBar`, D201.** The navigation bar is its own
  surface, deeper than the page, with a 24dp top corner and the hairline. In
  light this palette has no step above `sand`, which is why it is a token
  rather than a Material role. **White was built first and rejected by
  looking**, `nav-after-notebook-light-light.png`.
- **`NavMark` in `BottomNav.kt`, D202.** Material's stadium indicator is off
  and the current destination wears the app's own mark tile in `ink`. Gold
  was ruled out: gold means the way things enter the notebook, and the
  capture button floats a thumb's width above the bar.
- **`SectionEmpty` is one designed block**, in the flow rather than centered
  in a void, carrying the section's saturated mark at 64dp, an edge, the
  sentence in `ink`, and the way in. `EMPTY_HEIGHT_FRACTION` and
  `EMPTY_HEIGHT_TALL` are gone with the centering. **While a section is empty
  its floating add is null**, because the block carries the verb.
- **`SearchDoor` has callers.** It was built to `m3v4-1`'s drawing and never
  placed. It is the lead on the notebook and on More, and the notebook's
  top-bar search icon is gone rather than duplicated.
- **`ListRow` takes `markSize`.** A single-kind list carries the kind's color
  at the card size; the row size is for a screen showing several kinds.
- **Four strings in the app's disclaimer voice are gone** from all four
  catalogs, and `medications.record_only` is deleted.

## 6. Blocked, and section 3 is the live one

**Section 3.** Nothing is blocked.

## 7. Rules that get broken

1. `tools/verify.sh` is the only honest runner (compiles instrumented sources, runs lint).
2. **Read test counts from the XML, never a gradle exit code.** `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`.
3. An issue closes only on device verification: both themes, font scale 2.0, every state including empty.
4. An increment ends when `origin/main` has it.
5. The fixture must only produce rows the app itself can write.
6. **Look at the screen before closing anything.**
7. A check passing is not the design being done.

## 8. Traps that cost real time (full set: `docs/TRAPS.md`)

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
- **`tools/sweep.sh`'s closing list globs the prefix, so it lists files an
  older run with the same name left behind.** `after-setup-light.png` was
  printed by a sweep that never visited setup and was four hours old. Check
  the timestamp before reading a capture as today's.
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

## 9. The phone

- Pixel 8, `39151FDJH00506`, Android 17, USB. **A development device, not the owner's daily driver.**
- **Switching Android user profiles re-locks it** and there is no way past a
  secure keyguard from here, #316. **Never switch users**, and check before
  planning device work: `adb shell dumpsys window | grep isKeyguardShowing`.
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

## 10. Commands

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

## 11. Below the interface, and none of it changes

Foundation, four destinations, all section screens, 13 of 20 detail screens. Archive: two-layer container v3, readable copy, standalone decryptor in CI, format published byte for byte, stranger test passed. Merge as well as replace, with a conflict screen. Export, wipe, restore round trip proven on the signed minified build, D165.

## 12. Reading ladder

Read on demand, never in bulk.

| Need | Read |
|---|---|
| What each tracked thing is, before drawing its screen | **`docs/TRACKED-THINGS.md`**. Researched, with sources, and it is mostly about what rule 2 rules out |
| The prompt that starts a cleared session | **`docs/COLD-START.md`**. Written to run unattended to a finish line, and kept so it can be corrected rather than rewritten from memory |
| What to do next | `gh issue view 321`, then this file. Then `gh issue list --milestone "FINISH THE APP"` |
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

## 13. Facts a session re-derives if they are not written down

- **A classfile listing is not an API.** Kotlin's `internal` lives in the metadata, so `javap` shows it public. Only compiling against it answers "can this build call it". D179.
- 63 remote branches survive, all ancestors of `main`. **Never `git branch --merged`** here: squash-merge gives new shas. D144.
- Guard 2, the pre-compaction state save, has never fired. Keep this file current by hand.
- D143: ten cross-references point at `DESIGN.md` sections that no longer exist. **They stay.**
- #308 is reopened. Its class shares state through one installed app; a green run proves nothing.

## 14. Live lists that must not be lost

**Screens composed rather than drawn** (rule 12): `gh issue list --label needs-design-review`. No second copy here.

**Reachable only from a test, not from any seed** (each on its own issue): paperwork an incident produced; the care team card's sparse rung and the trail spine's gap markers; the digest's corrected and removed counts; #273's two template hands; **the month review's `Hero` block**, which draws only when a month holds a milestone, and no month in the `year2` fixture does. Checked June, April and March on 2026-08-18 rather than assumed.

**`EmptyDrawing` has no live drawing caller left.** `SectionEmpty` draws the
section's own saturated mark now, and the three remaining callers all pass
`section = null`, which draws nothing. The one file that still draws it is
`ProjectDetailScreen`, which is frozen.

**A second person is the cheapest empty notebook there is.** More, people in
this notebook, add another person, start their notebook: every section is
empty behind it and nothing in the first person's notebook is touched. That
is how every `empty3-*` and `empty4-*` capture in `docs/screenshots` was
taken.
