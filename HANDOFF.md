# HANDOFF.md

Current state. Nothing else. Read with `gh issue view 321`; neither repeats the other.

Written for a machine: fragments, no filler. Rewritten to current truth, never appended to. History goes to `docs/RUN-LOG.md` (never read to orient) or a commit message.

**Last rewritten:** 2026-08-17, for the interface rebuild.

---

## 1. State

- Tree clean, all on `origin/main`. CI green at tip: `gh run list --branch main --limit 3`.
- **735 instrumented tests green**, 218 unit, 29 repo checks, lint. Full suite run on the phone 2026-08-17 after the sheet went app wide: 735 run, 0 failed, read from the counts.
- **Do not build a release APK or AAB.** Owner holds delivery until he approves the design. The debug install is the rule 21 loop and is not what he is holding.
- Phone at baseline, stays plugged in: font scale 1.0, animator 1.0, no reader, night mode `no`.

## 2. The work

**The interface is being replaced, from the ground up, on Google's Material 3 Expressive. `docs/V4.md` is the plan and the authority.**

**The back end does not change.** Repository, schema, change log, export container, decryptor, fixtures, `contract/DATA-CONTRACT.md`: all stay. This is a user interface replacement. Anything needing a schema change is out of scope and goes to the owner.

**Where it stands: steps 1 and 2 are done, and phase 4, the screens, is under way.**

Step 1, #385. `Theme.kt` is `MaterialExpressiveTheme`, with all 48 Material color roles named in both themes, all 15 type roles, five different corners on the shape scale, and reduced motion reaching Material's own components through `StillMotionScheme`.

Step 2, #386, done so far: **the face, the icons, the navigation bar, the group container, the icon tile, the row's air, the button, the switch, the section furniture, and the type ladder.** Roboto replaces Atkinson (D181). 34 Material Symbols replace the hand-drawn marks (D182). The nav bar is Material's `ShortNavigationBar`, its gold indicator free from the theme. `GroupedSurface` is a flat tonal block. Buttons and the switch are Material's (D184). The full width back footer is deleted app wide and `SectionTags.BACK` moved to a back arrow in the top corner. The measurements came off the drawing (D183): row air 13dp, screen margin 16dp, Today card padding 20dp, and the display end of the ladder raised so the title to row jump is the drawing's 2.3 rather than 1.5.

**The field was already right.** `m3v4-4` draws a notched outline with a floating label and the app already did.

**The method changed on 2026-08-17 and `docs/ACCEPTANCE.md` is the authority on it.** The owner: "no old design language at all. get rid of it so it doesn't influence." **Nothing old is edited any more.** The new set is written from scratch in `ui/v4`, a screen is rewritten onto it rather than adapted, and an old component is deleted the moment its last caller goes. **The old package being empty is the test.**

**Step 2 is finished.** The accordion was the last row of the replace table and it is not being swapped for another accordion: it already carries the spring, Material's chevron and the one container corner, and `m3v4-3` draws the care team with no fold on it at all. It dies per screen, in phase 4. **D185.**

**Phase 4 is under way: 6 of 85 screens are rewritten**, the notebook, the document, the care team, the person, medications and About. **Medications is the recipe for every section list**: `Page` + `labeledBlock` + `ListRow`, the empty state a sentence on a quiet block, a finished group under its own label rather than behind a fold, and the add action at the foot sized to its label.

**Next, and why in this order.** The section lists, which are all one shape: `Page` + `labeledBlock` + `ListRow`. Then the project screen and Today, the two the owner has named, and **the project screen needs the v4 spine**: `m3v4-2` draws "The road" with a 24dp filled node carrying a check, a 20dp gold node in a `goldWash` halo for where it is now, a hollow 17dp ring ahead, a 3.7dp line, and a 56dp gutter. Measured, D183. The spine stays out of the person screen deliberately, D187. Each is rewritten onto `ui/v4` with the components it needs written as it needs them.

**The care team, 2026-08-17.** `m3v4-3` measured: one person raised into a block in the section's wash with calling and writing to them inside it, everyone else a separated tonal row with a gold call mark, and **a toggle where the accordions were**. Per-person avatar hues, the number off the row, email through `ACTION_SENDTO`. **D186**, which also records what it gave up.

**The sheet is done and it is app wide.** `ui/v4/Sheet.kt` carries the container, the corner, the scrim and the missing handle, `SheetBody` carries the insets and the screen margin, and `rememberSheet` replaces the deprecated `rememberModalBottomSheetState` at **all 18 call sites**: the build has no deprecation warnings left. The call sites keep their own contents until their screens are rewritten, so this changed no layout; the tips sheet is the one visible difference, since it alone had Material's light veil and a drag handle.

**Phase 1 of `docs/ACCEPTANCE.md` is finished: all six drawn screens carry the drawing.** The document screen was the last, rewritten onto `ui/v4` on 2026-08-17. `ui/v4` gained `PaperCard`, `FactBlock`, `Action` and `IconAction`, and `Page` gained a hero slot, a subtitle mark, and **its own window insets**: a page opens over the shell rather than inside it, so without them the back arrow sits under the status bar. Any screen built on `Page` inherits that fix.

**The old set still stands:** `QuietButton` 153 callers, `GroupedSurface` 101, `DenseRow` 99, `GroupHeader` 90, `SectionScaffold` 70, `Thumbnail` 12. Each goes as its last caller goes, and the old package being empty is the test.

**The project screen is the one the owner has ruled on, and it is not rewritten yet:** "it's absolutely horrid and so far away from the mock-ups." Against `m3v4-2` it needs a gold "decision expected" tonal block where a white date row is now, a status pill in the top corner, one filled action beside two tonal ones with their icons above the labels rather than three white tiles, and a "The road" heading over the spine. Today's card grid is the other one he has named: a lone small card leaves half a row empty beside it.

**Six of today's fixes were found by measuring the approved PNG in pixels** and comparing it against a capture off the phone: the typeface, the tile's near-circle corner, the 54dp row pitch against the drawing's 64, the 13dp screen margin against 16, the card's 12dp padding against 21, and a type ladder whose jump was 1.5 where the drawing's is 2.3. **Do that before forming an impression**, `docs/V4.md` 6. D183.

**The foundation was not on the classpath, whatever three documents said. D179.** In stable material3 1.4.0 the expressive theme and the motion scheme are `internal` and the expressive components do not exist. The build now pins **material3 1.5.0-alpha26**, past the bom, and the bom is **2026.08.00** so Compose UI stays 1.12.0 stable rather than being dragged to a beta. **One artifact off the stable channel, deliberately.** Check the pin against what is stable before any release build.

**`MaterialShapes` is inside material3 1.5.0.** No `androidx.graphics:graphics-shapes` is needed. The old note saying otherwise was written from the same unverified paragraph.

**`rememberBottomSheetState` is what the alpha wants** and `ui/v4/rememberSheet()` is the app's one call to it: a hidden initial value with the half open stop left out of `enabledValues`, which is what `skipPartiallyExpanded` used to say. The parameter is `enabledValues`, not `sheetValues`, and the compiler is the only place that says so.

**The order is theme, then shared surfaces, then screens**, and a screen is rewritten or left alone. Nothing half converted. `docs/V4.md` 3.

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
- **`tools/seed.sh` taps by position.** Seed at font scale 1.0; a raised scale moves the restore screen's controls and the run ends on the wrong screen with an empty notebook. Its last line says "Restored." or it did not finish.
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
