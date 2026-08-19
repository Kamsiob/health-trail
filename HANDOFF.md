# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-19, during the overnight durability run. **The order of work changed on 2026-08-18: the interface is last.** Milestone 9 is substantially done and section 2a is what moved.

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
| Step 10 | #395 | **Delivered twice and still open on purpose.** The one the owner has is **13,113,726 bytes, SHA-256 `254f5fb145de8384aa0aa98afee62641163e4ba60ff321f9c7bfef5124d70cea`**, built after Memos joined the bar and walked on the minified build: five destinations and a memo written with a mark. The first delivery is on the issue. **It stays open until the build from the finished app**, because closing and reopening it would lose which APK he has. |
| Step 11 | #399 | **Done and closed.** The projects list leads with the nearest date and lists the rest as contained rows; inside a project the answer and the date are one block, the file is four tiles, and housekeeping is in the corner. D205, D206. |

**"Multiple users and profiles" in #389 means people inside one notebook**, which
is #393. It does not mean a second Android user on the device. Owner, 2026-08-18.
Reading it literally cost a locked phone and proved nothing.

## 2. What is left, and the order changed on 2026-08-18

**The interface is built and milestone 8 is empty.** Projects, tracked things,
memos, the bin and the dictation rule all landed and closed the same day.

**Then the record itself was reviewed**, and the app was found able to lose
everything a person put in it with no error on screen. **The order is now risk
order, not effort order.** `gh issue view 321` holds it. Four new milestones:

### 2a. What the overnight run of 2026-08-19 changed

**17 commits, every one on `origin/main`, every one with `tools/verify.sh`
passing every executed step.** The single most important fact in the previous
version of this file, that `HealthTrailDatabase.kt:101` passes `null` as the
`DatabaseErrorHandler` and the library therefore deletes the notebook, **was
half wrong and is closed either way.** See #407.

**Milestone 9, 15 of 18 closed.** #407, #408, #409, #410, #411, #412, #413,
#414, #415, #416, #417, #418, #419, #420, #454, #457.

| What is true now that was not | Where |
|---|---|
| **Write ahead logging is actually on.** Proved by `health-trail.db-wal` existing on the phone, where it never did. The contract declared it and it had never once applied | #408 |
| **The database open names its own error handler**, so surviving corruption is a property of our code rather than of a native flag in a dependency | #407 |
| **A file that will not open reaches a screen** instead of crashing at launch forever, and that screen asks the person not to uninstall | #410 |
| **The restore swap is a rename**, and startup adopts the safety copy a half finished restore leaves | #409 |
| **Attachments are written before the rows that point at them**, and a missing photograph has its own sentence | #411 |
| **"Saved" means an archive that was reopened and read back** | #412 |
| **Two silent losses are gone**: a prep answer overwritten by a racing write, and a photographed paper discarded for having no title | #420, #419 |
| **A merge says what it did**, and an archive from a newer build is no longer called tampering | #454 |
| **A second person gets the same beginning as the first**, situation picker included | #452 |
| **The person with two people in the notebook can switch**, which only the fallback Today allowed before | #453 |
| **A care thread can be finished and started again.** The columns existed, were read in two places, and had no writer | #433 |

**Three transactions that were only ever a comment are now transactions**:
`makeSubjectActive`, `moveToChapter`, `recordMedicationEvent`. #423 stays open
for `createPerson` and `applySituation`.

**New tests**, all on the phone: `CorruptionTest`, `JournalModeTest`,
`InterruptedRestoreTest`, `ActiveSubjectTest`, plus three on `RootStatesTest`
and one on `PrepTest`. **`ActiveSubjectTest` is the one the run instructions
asked for**, since nothing had ever exercised `addSubject` or
`makeSubjectActive`.

**The milestone 9 gate was walked**, not just coded: type a call, `am kill`,
relaunch. A saved entry survives and is findable. A half written note survives
too. What does **not** come back is which screen was open, which is now the
substance of #451 and is **B8** in `DECISIONS.md` after three failed attempts.

**Do not re-derive these three**, they cost real time:

- **`DefaultDatabaseErrorHandler` returns before deleting when `hasCodec()` is
  true**, and this build logs `hasCodec() = true`. #407's premise was wrong on
  the deletion and right that nothing routed the failure anywhere.
- **The SQLCipher pool is capped at exactly one connection until WAL is on**,
  and the driver's configuration holds the passphrase array **by reference**.
  Turning WAL on woke both. D209.
- **`readablePages` returning an empty map is deliberate** and changing it to
  throw fails 11 `ExportContainerTest` tests, because that class writes payloads
  that are not databases. The check belongs on the manifest's page count. #412.

**Still open in milestone 9:** #451 (B8), #455, #461.

---

| Milestone | What it is | Why it is where it is |
|---|---|---|
| **9. The record survives** | 18 issues, #407 to #420, #451, #454, #455, #457 | The app deletes its own database on corruption, the declared journal mode has never been applied, restore replaces the live file with a stream copy, and anything but a lost key crashes at launch forever. Two archive import holes ride along because the same file reaches them. |
| **10. The wiring under the screens** | 18 issues, #421 to #434, #452, #453, #458, #459 | Columns with a reader and no writer. Transactions claimed in a comment and absent from the code. Filing that happens only at capture, so nothing can be re-filed. |
| **11. What the notebook still needs** | 9 issues, #435 to #442, #456 | The nine additions agreed in **D208**. Two need schema first. |
| **12. One chrome, one motion** | 8 issues, #443 to #450 | Seven header implementations become one. Then the repeats. |

**That paragraph is gone because the work is done.** It said
`HealthTrailDatabase.kt:101` passing `null` as the `DatabaseErrorHandler` meant
the library deletes the notebook on corruption. Measured on 2026-08-19: the
library's default returns before deleting when `hasCodec()` is true, which it is
here. The open names its own handler now regardless, and the real defect, that
nothing routed an unopenable file anywhere, is #410.

**`docs/TRAPS.md` section 8 is new** and is the shapes all of this takes: a
comment claiming a transaction, a pragma silently ignored inside a transaction,
`runCatching` swallowing `Throwable`, a column with a reader and no writer,
`-1` passing a `>` comparison. **Read it before touching a write path.**

**Verified by the main session rather than taken on report**, because two claims
were wrong and both mattered:

- `requireSafeName` does **not** reject `..`, so it is not a path check and
  reusing it does not close #414.
- Subject scoping **holds** at the query layer. The two unscoped reads,
  `ownTemplates` and `organizationNamed`, hold shared reference data rather than
  one person's records, and the dedup in the second reads as deliberate. No
  medical record crosses between people.

**The owner, 2026-08-18: breaking the app is not an option.** Every new issue
carries a risk of fix. Anything marked SCHEMA stops and goes to `DECISIONS.md`
BLOCKED, rule 3.

**53 issues were opened, #407 to #459. 120 are open as of 2026-08-19**, after
the overnight run closed 19 and opened three: #460 the tips panel, #461 export
from the stuck screen, and the owner's two additions to #395. 85 were open before this work and most are
open on purpose: 28 owner review, 13 deferred by D141 and D180 which **must not
be closed**, 9 beyond v1, 8 release blocking (#1, #9, #15, #44, #210, #211,
#212, #319), and the maintenance tail. **Count with
`--limit 100 --json number -q '.[].number' | wc -l`**; a bare `gh issue list`
truncates and reported the wrong number three times in one session.

## 3. Blocked

**B8, and it is the only one.** #451: the capture screen is not restored after
process death, and the mirror that should restore it arrives null while
`captureDraft` beside it arrives intact. Three attempts, all reverted, nothing
of them in the repository. `DECISIONS.md` B8 has what was tried and the one
question to answer before a fourth.

Otherwise nothing is blocked. The phone is unlocked and at baseline: font 1.0,
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

- **D205, the projects list.** One project leads, whichever live one has the
  nearest date somebody else set, drawn as a tonal block with the road at full
  width and its labels on. Everything else is a dense row in a `Block` with the
  scheme's hairline. **The uppercase status tag above every name is gone**:
  GOV.UK spent two years and three services on that exact pattern and came out
  at plain sentence case. Sources on #399.
- **`ListRow` takes an `overline`, and it is a layout decision.** Material
  top-aligns a row's leading mark only once the row is three lines; at two, a
  title that wraps leaves the disc floating between the words. The owner named
  it: "the text doesn't align with the icon".
- **The four destinations arrive rather than cut.** `ProjectsScreen` was the
  first; `Page` had given every interior screen `arrives` and no destination
  had it, so the screens opened most were the ones that cut into place.
- **D206, inside a project.** Where it stands and the next date are one tonal
  block, because they were two a gap apart with the second drawn louder. The
  file is four tiles, rule 22's component for a fixed set of destinations.
  Setup, the name and the removal are in a Material overflow in the top bar.
- **"Who has it now" replaced "Update where it stands"**, and the lapse block
  lost its button. Three controls looked like the same verb and two of them
  were the same action.
- **`DateRow` has a `flat` mode**, for a caller that is already a container.
  Its rounded clip ate the first letter of its top and bottom lines, so flat is
  square and the block around it owns the corners.
- **"Stalled" is "Nothing moving" and "Left alone" is "Set aside"**, in all four
  catalogs. **The five stored values did not move**, rule 3.
- **#402 is open and release-blocking**: six merge resolutions are recorded and
  the door on More that opens them never appears. Everything ruled out is
  written on the issue so nobody repeats the diagnosis.
- **New issues this session**: #401 and #404 are the rule 12 design reviews,
  #402 the conflict door, #403 the three tracked-thing shapes the schema cannot
  carry, #405 the universal trash can.
- **`ListRow` takes `overline`** and `Field` takes `minLines`. Both exist
  because Material's own behavior needed them: a row top-aligns its mark only at
  three lines, and a field somebody writes paragraphs into should not stand at
  the height of one asking for a name.
- **An open question the owner has to settle**: "get rid of the mic completely"
  was read as this screen rather than app-wide. **If it was app-wide, #396 is
  the issue that changes**, because it says every text area should offer
  dictation.

## 6. Blocked, and section 3 is the live one

**Section 3.** B8 only, and it is #451.

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
- **`walk.sh see` reports what is laid out, not what is on the screen, and More
  scrolls.** A row below the fold is absent from the dump, and reading that as
  "the row is missing" cost most of a session: **#402 was filed as a bug and was
  not one.** The conflict door and the bin door both sit at the foot of More's
  "keeping a copy" group, and both looked absent until the screen was scrolled.
  **Scroll before concluding a row does not exist**, or dump after
  `input swipe`.
- **`tools/sweep.sh`'s closing list globs the prefix, so it lists files an
  older run with the same name left behind.** `after-setup-light.png` was
  printed by a sweep that never visited setup and was four hours old. Check
  the timestamp before reading a capture as today's.
- **`tools/walk.sh tap` matches the first node containing the word.** Navigate the five destinations by nav bar position: **x = 107 / 323 / 540 / 755 / 971, y = 2302**, which is Today, Notebook, Projects, Notes, More. **These changed on 2026-08-18** when notes joined the bar, #397: five items share the width four had, so every one of them moved. The old 133 / 400 / 670 / 940 at y=2252 taps the wrong tab now.
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

**Newest, 2026-08-18, #401**: the projects list's lead block, and a project's
file as four tiles with the housekeeping moved to a top-bar overflow. Both are
composed from existing components; the grid draws screens 02 and 05 and carries
neither. D205, D206.

**And #404**: one tracked thing's own screen, and Progress as a lead plus rows.
The grid draws screen 24 and carries neither how it is reached nor what happens
to the rest of the page. #398.

**And #406**: the memo screen, the memos page, `MemosAbout` on six kinds of
thing, Memos as the fourth destination, and what you took out. **The grid draws
none of them**, because neither notes nor a bin existed before 2026-08-18.
#397, #405, D207.

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
