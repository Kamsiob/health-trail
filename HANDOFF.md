# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-27, during the v1.1 run. **1.0 is live on Google Play**, versionCode 2, and the current run is milestone 13.

---

## 0. Cold start

1. `gh issue view 321`, then this file. Nothing else.
2. **The phone is shared.** Another session on this machine is building a
   different app and uses the same device. Section 9 is the rules of the road,
   and they are not optional.
3. Check the phone is unlocked before planning device work:
   `adb shell dumpsys window | grep isKeyguardShowing`. **Never switch Android
   user profiles**; it re-locks the phone and there is no way past a secure
   keyguard from here, #316.
4. `tools/sweep.sh audit`, and **look at the captures**.
5. One unit per commit: `tools/verify.sh`, install, look on the phone, commit, push.

Worked examples, best first: `ui/v4/Arrival.kt`, `ui/v4/Press.kt`,
`ui/v4/MonthGrid.kt`, `screens/MedicationList.kt`, `screens/Notebook.kt`.

## 1. What shipped, and what is shipping

**1.1.0 is on Google Play**, versionCode 3, production, status completed,
committed 2026-08-30 through the Android Publisher API. Bundle SHA-256
`1d4d29e764f37b98a587e33513ffcac4a246989faad098afbfc270d40cc9b354`, signed
`CN=Health Trail, O=Kamsiob, C=US`, all eight native libraries 16 KB aligned.

**The GitHub release is `v1.1.0`** and its asset is the **Google-signed
universal APK** pulled back from Play through `generatedapks`, SHA-256
`1e83cc401b9001c6bc1df4829c91d279346f48ca091a6236edd970d2014b78bb`, signed
`CN=Android, O=Google Inc.` rather than by the upload key. That is
`PROJECT-DELTAS.md` 17 to 19 followed exactly: one signature, two paths, and the
release asset never built locally.

**The whole pipeline is automated and needs no Play Console clicks.** Upload,
track, release notes, validate, commit, and the universal APK download all go
through the service account held outside this repository. 1.0's listing text,
eight framed screenshots and feature graphic are still live and unchanged.

**Milestone 13 is v1.1**, opened 2026-08-27 out of the owner's own testing.
Ten issues, #462 to #471.

| | | |
|---|---|---|
| #462 | The Projects card cannot be removed from Today | **Code landed.** Device verification pending |
| #463 | The capture bloom colors only the memo option | **Code landed.** Device verification pending |
| #464 | The Memos lightbulb, and every other tip | **Code landed.** Device verification pending |
| #465 | Deleted Items, and permanent delete | **Code landed.** Device verification and the instrumented suite pending, which is B9 |
| #466 | Visual consistency sweep | **Static half done, device walk not started** |
| #467 | Reorganize More | **Code landed.** Device verification pending |
| #468 | Profiles | **Code landed.** Device verification pending |
| #469 | Ship v1.1 | Not started. Gated on the rest |
| #470 | GitHub cleanup, face and safety | **Done, both halves** |
| #471 | Removing a reading wrote to a table that does not exist | **Code landed.** Test written, not run, which is B9 |

**Nothing above is closed.** An issue closes on device verification, rule 3 in
section 7, and the phone has not been available.

## 2. What the v1.1 run changed, and what it found

**Ten commits, every one on `origin/main`, every one with `tools/verify.sh`
passing every executed step and 34 checks green.**

**Three of the owner's four bugs were not what the report said, and the real
cause was worse in each case.**

| Reported | Actually |
|---|---|
| The Projects card cannot be removed | **There is no Projects card kind and nothing excluded one.** The exclusion is positional: whatever card stands in the visible lead slot loses its remove mark, its options sheet and its say in where it sits. **And that slot is not holding the lead**: all fourteen shipped starting hands begin with `digest`, which D193 never draws, so the stored lead is a row nobody has seen and the card on screen sits at stored index one or two |
| The Memos lightbulb shows filler | **The four keys have never existed in any catalog.** Release renders `tips.notes.title` literally; debug throws. `check_string_keys.py` cannot see a key built from an enum name and says so in its own docstring |
| The bloom colors only the memo | True, and the file already carried the sentence saying what it should do. Six of seven took a branch hardcoding `ink` on `sand`; the one option belonging to no section was the only one asking for a hue |

**And the audits found the mirror defects.** Four notebook sections have a tip
written and translated four times with `section` omitted from the `Page` call
that draws the lamp. Medications had no lamp at all. The people switcher's lamp
opened the care team's tip. Deleted Items iterated `Section`, so every deleted
memo was listed twice, and six kinds of thing live in tables no `Section` names
as its own and could not be shown or put back at all.

**The one write in this app that is not a tombstone now exists**, and
`contract/DATA-CONTRACT.md` 3 was amended by the owner for it rather than worked
around. Two exceptions became three. The new one names all five things "removed
entirely" has to mean, because four of them are invisible: the row, its
dependents, a nullable reference cleared rather than followed, its change log
rows, and its attachment bytes.

| What is true now that was not | Where |
|---|---|
| **Every card on Today can be taken off, the lead included**, and Today has a designed empty state because removing the last one is now reachable | #462 |
| **Every bloom option wears the hue it writes into**, through `entryHue`, which is the map the trail already reads | #463 |
| **Every lamp opens a written tip and every written tip has a lamp**, held by `check_tips.py`, which enumerates the sections from the enum | #464 |
| **Deleted Items lists everything a person can delete**, once each, and can delete one for good | #465 |
| **More is five groups in frequency order**, recorded in `DESIGN.md` 24, and carries the support link the specification has placed at the bottom of Settings since it was written | #467 |
| **Removing a reading works.** It wrote to a table called `reading`, which does not exist | #471 |
| **Nothing in the tracked tree identifies a person, a machine or a secret**, held by `check_no_pii.py` | #470 |
| **The build is proved to meet what Play requires**, held by `check_play_requirements.py` and `tools/store/check-bundle.sh` | D15 |

**Four new checks. 34 now, and every one of them found something real:**

- `check_tips.py`, 18 lamps and 72 keys.
- `check_no_pii.py`, 529 text files. Proved against a planted example of every
  pattern it refuses.
- `check_play_requirements.py`. Proved by lowering `targetSdk` and watching it
  fail.
- `tools/store/check-bundle.sh`, which measures what cannot be read from source:
  16 KB page alignment, size, hash and signature on the artifact about to be
  uploaded.

**Do not re-derive these, they cost real time:**

- **`check_hook_quoting.py`'s `resolve()` looks for `$CLAUDE_PROJECT_DIR`.** The
  settings file named an absolute path instead, so the "script present and
  executable" half of that check had never once run. Both hooks use the
  placeholder now, and **the guard was then seen refusing a real `rm -rf`**,
  which is the only thing RUN-SAFETY 1.1 accepts as proof that a hook fires.
- **`Space.bloomDrawing` is gone**, because `HueMark` derives its glyph from the
  disc rather than taking a second measurement that could disagree with it.
- **Play requires API 36 of updates from 31 August 2026, and the API 35 figure
  in summaries is a different rule**: it is the visibility threshold for an app
  that is never updated. Conflating them is how somebody concludes they are
  compliant when they are not.
- **16 KB page alignment is the requirement that actually applies to this app**,
  from 1 February 2027, because it ships two native libraries across four ABIs.
  All eight `.so` files in the 1.0 bundle report `0x4000`. Measured, not assumed.

---

**Counts as of 2026-08-27:** 127 open. Milestone 13 has 10 open, milestone 9 has
3, milestone 10 has 11, milestone 11 has 9, milestone 12 has 9. **Count with
`--limit 200 --json number -q '.[].number' | wc -l`**; a bare `gh issue list`
truncates and reported the wrong number three times in one session.

| Milestone | What it is | Why it is where it is |
|---|---|---|
| **13. v1.1** | 10 issues, #462 to #471 | The owner's four bugs from using 1.0, the polish pass, the settings reorganization, the repository cleanup, and the release |
| **9. The record survives** | 3 open of 19 | The app deleted its own database on corruption, the declared journal mode had never applied, restore replaced the live file with a stream copy. Mostly closed 2026-08-19 |
| **10. The wiring under the screens** | 11 open of 18 | Columns with a reader and no writer, which read on screen as a section that is merely unfilled. Transactions claimed in a comment and absent from the code |
| **11. What the notebook still needs** | 9 issues, #435 to #442, #456 | The nine additions agreed in **D208**. Two need schema first |
| **12. One chrome, one motion** | 9 issues, #443 to #450, #460 | Seven header implementations become one. Then the repeats. **#450 is the live fidelity structure**, and #466's overflow files against it |

**The owner, 2026-08-18: breaking the app is not an option.** Anything marked
SCHEMA stops and goes to `DECISIONS.md` BLOCKED, rule 3. The one exception so
far is the permanent delete, which he ordered along with the contract amendment
it needed.

## 3. Blocked

**B9, and read it before running any test.** The Pixel 8 holds the **signed
release build**, installed for #395's walk. No instrumented test can run until
it is uninstalled **by hand**: a debug build cannot replace a release one, and
every uninstall command is refused by the guard, which is D210 working as
intended. It is ten seconds of the owner's time. **It gates #465's acceptance**,
which names the coverage and regeneration tests, and #471's new test.

**A release build can still be installed over it**, same signature, so the visual
and behavioral walk is not blocked by B9. Only the instrumented suite is.

**B10, and it is one minute of the owner's time.** The private Telegram invite
link is out of the tree and is in four commits. It is a bearer credential:
whoever reads those commits joins the group. **Revoking it in Telegram makes the
history harmless**, which rewriting would not do as reliably. Nothing else in
B10's list is more than a serial number and a folder name.

**B8. #451**: the capture screen is not restored after process death, and the
mirror that should restore it arrives null while `captureDraft` beside it
arrives intact. Three attempts, all reverted, nothing of them in the repository.
`DECISIONS.md` B8 has the one question to answer before a fourth.

**B7 is unchanged** and is the history rewrite. The honest recommendation there
applies to B10 too: the current files are what anybody reads.

Otherwise nothing is blocked. The phone is unlocked and at baseline: font 1.0,
animator 1.0, no reader, night mode `no`, appearance "Follow the phone".

## 4. The direction, and it is not negotiable

**Material 3 Expressive is the floor, not the finish.** Built out of Material's
own components and Google's own assets, D196, with our polish on top: color
identity, copy, arrangement, motion, what leads. **Nothing here is being
replaced.**

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
4. Entry lists carry the kind's color. **Never a second mapping.** `entryHue` is
   that one mapping, and the capture bloom reads it now, #463.
5. Identity, never state. Rule 2. `hueFor` is the owner's mapping.
6. A color never carries meaning alone: the mark and the words together.

## 5. What this run added that the next one builds on

- **`Page` takes a `tipKey`**, so a page keeps its section's ink and states its
  own subject. Written because the people switcher wore the care team's tip.
- **`TodayFieldScreen` computes from `drawn`**, one list, and every move, every
  removal and every position reads it. A stored index and a screen position are
  two different numbers on that surface and always were.
- **`CardOptionsSheet` takes a nullable `onPromote` and `onRemove`**, so nothing
  is offered a promotion to where it already is.
- **`Repository.purge(table, rowId)`**, and `childrenOf` reads the dependency
  graph out of `PRAGMA foreign_key_list` rather than a list that goes stale.
- **`Attachments.remove(hash)`**, the only thing in that class that deletes, and
  it is called only after checking no row still names the hash.
- **`Discarded` carries its table**, because `Section` is a screen's idea of the
  notebook and there are fourteen of them over eleven tables.
- **`SUPPORT_URL` is one internal declaration**, having been written down twice
  while its own comment claimed to be the only place in the app that wrote it.
- **`DESIGN.md` 24** is More's grouping, the reasoning for the order, and the
  four rules a row added later follows.

## 6. Blocked, and section 3 is the live one

**Section 3.** B7, B8, B9, B10.

## 7. Rules that get broken

1. `tools/verify.sh` is the only honest runner (compiles instrumented sources, runs lint).
2. **Read test counts from the XML, never a gradle exit code.** `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`.
3. An issue closes only on device verification: both themes, font scale 2.0, every state including empty.
4. An increment ends when `origin/main` has it.
5. The fixture must only produce rows the app itself can write.
6. **Look at the screen before closing anything.**
7. A check passing is not the design being done.
8. **`HANDOFF.md` lands in the same commit as the work it describes.** Ten
   commits went by on 2026-08-27 without it, which is how this file goes stale.

## 8. Traps that cost real time (full set: `docs/TRAPS.md`)

- **A stored index is not a screen position on Today.** `digest` and `next_up` are in the layout and are never drawn, so `draft[0]` is a row nobody has seen. Anything meaning "the first one" or "the one above" reads the drawn list. #462.
- **A catalog key built from an enum name is invisible to `check_string_keys.py`**, which says so in its own docstring. `check_tips.py` goes the other way and enumerates the enum. #464.
- **`Section` is not a table.** Fourteen sections over eleven tables, and `TRAIL` and `NOTES` are both `entry`. Anything iterating `Section.entries` over rows lists memos twice. #465.
- **The caller's `testTag`, the tap and the reader's sentence must be on one node.** Put `combinedClickable` and `semantics { contentDescription }` on the `Card`'s own modifier, not on a column inside it.
- **This scheme's `surfaceContainerLow` is the canvas in light.** A card drawn on it is invisible. Use `surfaceContainer`.
- **A row that promises a door may not have one.** `check_uncalled_callbacks.py` holds this now. **Dump the screen and count clickable nodes.**
- **Three files are frozen and must not be edited**, `docs/REMOVAL-LEDGER.md`: `ProjectDetailScreen.kt`, `CaptureSheet.kt`, `PinnedGroup.kt`.
- **`git rm` stages immediately.** Run it at the moment you commit.
- **A `LazyColumn` does not compose off-screen rows.** Scroll by the list's own item key, `performScrollToKey`.
- **A floating action button is on the scaffold, not in the list.**
- **`tools/seed.sh` walks the restore screen by text.** Its last line says "Restored." or every capture after it is of an empty notebook.
- **`walk.sh see` reports what is laid out, not what is on the screen, and More scrolls.** **Scroll before concluding a row does not exist.** #402 was filed as a bug and was not one.
- **`tools/sweep.sh`'s closing list globs the prefix**, so it lists files an older run left behind. Check the timestamp.
- **`tools/walk.sh tap` matches the first node containing the word.** Navigate the five destinations by nav bar position: **x = 107 / 323 / 540 / 755 / 971, y = 2302**, which is Today, Notebook, Projects, Memos, More.
- **Screenshot coordinates are not device coordinates.** `tools/screenshot.sh` crops the status bar; add it back before `adb shell input tap`.
- `onNodeWithText` does not see a `contentDescription`. Use `onNodeWithContentDescription`.
- **A `SlotWriter` `ArrayIndexOutOfBoundsException` mid-suite is the Compose alpha.** Re-run the class alone.
- **New optional parameters go after `modifier`** or lint `ModifierParameter` fails.
- `connectedDebugAndroidTest` uninstalls the app. Reinstall and reseed after a suite.
- **Do not compile while the instrumented suite runs.**
- The destructive-command hook matches prose. Some verbs cannot be written into a file. #323.

## 9. The phone, and it is shared

**Another session on this machine is building a different app and drives the
same device.** Rules of the road, owner, 2026-08-27:

- **Before any install, launch, test run or screenshot, check the foreground and
  check for an install in flight.** If the other app is in the foreground or
  mid-operation, wait and check again. That session takes long thinking breaks
  between bursts of device work, so waiting several minutes is normal. **Never
  race it.**
- **A `PackageUpdateActivity` in the foreground is that session installing**, not
  leaving. A watch that fires on it fires on a false positive. Require several
  consecutive clear samples.
- **Never uninstall, force stop or clear the other package**, and never reboot
  the device or change a global setting to resolve a conflict.
- **Nothing from it enters this project.** No reference, no string, no capture
  containing its interface. A capture that catches it is retaken.
- **Device contention is never a reason to skip verification.** Do non-device
  work from the board and come back.

Then:

- Pixel 8, Android 17, USB. **A development device, not the owner's daily
  driver.** Its serial is not written down here, #470: `adb devices` says it, and
  `check_no_pii.py` refuses one in the tree.
- **Switching Android user profiles re-locks it** and there is no way past a
  secure keyguard from here, #316. **Never switch users.**
- `adb` may not be on `PATH`. The four scripts under `tools/` find it through
  `$ADB`, then `command -v adb`, then `$ANDROID_HOME`, then
  `$HOME/Android/Sdk/platform-tools/adb`.
- Baseline: font 1.0, animator 1.0, touch exploration 0, night mode `no`. Rule
  19 lets these change **only** if the prior value is recorded first and restored
  exactly.
- **Never screenshot**: the share sheet, the calendar app, any screen with a
  password field, or anything belonging to the other session's app.
- Reinstall and reseed after a suite.

Fixture variants:

    tools/device.sh year2 6 walk-appointment --appointment-on YYYY-MM-DD   # the usual one
    tools/device.sh year2 6 walk-year-three  --arranged
    tools/device.sh month6 6 walk-home       --situation home_family
    tools/device.sh month6 6 walk-quiet      --quiet

**The arranged fixture is the one that puts a chart in Today's lead.**

## 10. Commands

    tools/sweep.sh audit                # seed once, walk every screen, capture each
    tools/sweep.sh --no-seed audit      # reuse what is on the phone
    python3 tools/checks/run_all.py     # 34 checks, seconds
    tools/verify.sh                     # the honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest                  # ~16 min
    cd android && ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>             # one class, <1 min
    cd android && ./gradlew :app:assembleRelease                            # signed, D160
    cd android && ./gradlew :app:bundleRelease                              # the AAB Play takes
    tools/store/check-bundle.sh <path.aab>  # 16 KB alignment, size, hash, signature

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
| What each tracked thing is, before drawing its screen | **`docs/TRACKED-THINGS.md`** |
| The prompt that starts a cleared session | **`docs/COLD-START.md`** |
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

`DESIGN.md` is superseded by `docs/V4.md` on anything visual. It still holds the accessibility gate, section 12, and section 24, which is More's grouping.

## 13. Facts a session re-derives if they are not written down

- **A classfile listing is not an API.** Kotlin's `internal` lives in the metadata, so `javap` shows it public. D179.
- 63 remote branches survive, all ancestors of `main`. **Never `git branch --merged`** here: squash-merge gives new shas. D144.
- D143: ten cross-references point at `DESIGN.md` sections that no longer exist. **They stay.**
- #308 is reopened. Its class shares state through one installed app; a green run proves nothing.
- **`LocalPageSection` has a writer and no reader.** `Page.kt` provides it and its comment says a page's own groups can wear its section. Nothing consumes it. The shape `docs/TRAPS.md` section 8 opens with, found 2026-08-27 and not yet filed.

## 14. Live lists that must not be lost

**Screens composed rather than drawn** (rule 12): `gh issue list --label needs-design-review`. No second copy here.

**Newest, 2026-08-27**: Today's empty field state, and Deleted Items with a
permanent delete on it. Both are composed from existing components and neither
is drawn in any grid.

**#401**: the projects list's lead block, and a project's file as four tiles.
**#404**: one tracked thing's own screen, and Progress as a lead plus rows.
**#406**: the memo screen, the memos page, `MemosAbout` on six kinds of thing,
Memos as the fourth destination, and Deleted Items. **The grid draws none of
them.**

**Reachable only from a test, not from any seed** (each on its own issue): paperwork an incident produced; the care team card's sparse rung and the trail spine's gap markers; the digest's corrected and removed counts; #273's two template hands; **the month review's `Hero` block**, which draws only when a month holds a milestone.

**`EmptyDrawing` has no live drawing caller left.** The one file that still draws it is `ProjectDetailScreen`, which is frozen.

**A second person is the cheapest empty notebook there is.** More, Profiles, add
another person: every section is empty behind it and nothing in the first
person's notebook is touched. That is how every `empty3-*` and `empty4-*`
capture in `docs/screenshots` was taken.
