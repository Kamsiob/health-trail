# TRAPS.md

**Read the one section that matches what you are about to do. Never end to end.**

Every line cost real time at least once. Written for a machine: fragments, no filler. Each trap keeps its tell, which is the part worth having: what the failure *looks* like when it is not what it is.

| About to | Read |
|---|---|
| Touch the phone | [1](#1-the-phone) |
| Run or believe the tests | [2](#2-the-tests) |
| Change copy or a catalog | [3](#3-words-and-languages) |
| Write or change a check | [4](#4-checks) |
| Change a screen | [5](#5-screens) |
| Commit | [6](#6-committing) |
| Run any command | [7](#7-this-machine) |

---

## 1. The phone

**State**

- **Check unlocked first**: `adb shell dumpsys window | grep isKeyguardShowing`. A secure keyguard fails every instrumented class with `IllegalStateException: No compose hierarchies found in the app`, which blames your test. No way past it from here. #316.
- **`installDebug` clears app data.** A screenshot straight after an install is a screenshot of onboarding. Every device check is install, seed, navigate. `tools/device.sh` does all three and refuses rather than half succeeding.
- **`connectedDebugAndroidTest` uninstalls the app.** A seed straight after prints `Failed` with one word. Check `pm list packages | grep kamsiob` first. Bit three times in one night, and each time the next thing dumped the owner's home screen with his real calendar on it. Also the sanctioned way back to first-run state, since `adb uninstall` is blocked. D50.
- **A whole class failing identically is the environment.** All six `BackJourneyTest` failed with `RootViewWithoutFocusException` while the notification shade held focus. Tell: `has-window-focus=false`, `Sys2040` in `mCurrentFocus`. Collapse the shade, press home, rerun alone. #316.
- **A tall test phone hides every "does not fit" defect.** The Pixel 10 Pro XL was 1344x2992; the Pixel 8 is 1080x2400 and its first run failed three long-green tests. Tell: "expected the words but was null", which reads as a save that did not fire; truth was a Save below the fold, and **a click at a node's center outside the viewport does nothing**. Test small deliberately: `adb shell wm size 720x1280 && adb shell wm density 320` (360x640 in layout units), then `wm size reset && wm density reset`. **Record and restore both**, rule 19.

**Seeding and installing**

- `tools/seed.sh` drives the system file picker. A phone that has never opened it lands on Recent, not Downloads, and reports "seed.htx is not in the picker" on a device that has the file. The script opens the drawer and taps Downloads every time; the picker does not remember between invocations.
- `tools/device.sh` installs every time and **refuses when any source is newer than the APK**. **`compileDebugKotlin` does not build an APK. Only `assembleDebug` does.**
- Seeding goes through the app's own restore screen deliberately, D61: a fixture arriving any other way has never been through the importer. If it says the file is not in the picker, something else has focus: back, home, rerun.

**Walking**

- **A tap at stale coordinates can leave the app; the worst landing is the dialer** on the owner's real contacts. A dump goes stale the moment anything moves. **Re-dump immediately before every coordinate tap**, and check `mCurrentFocus` after. If it left: force stop what opened, relaunch, **do not screenshot or read what is on screen**.
- **`walk.sh tap` matches a substring**, so a short word hits the longest sentence containing it: `tap "Remove"` matched "Closed. Still here until you remove it."
- **It cannot match a label carrying a name**, because `Bidi.join` puts isolate marks inside: `Remove ⁨Milestones⁩`. Reports NOT FOUND, which reads as "below the fold". Dump the tree and tap bounds; re-dump between taps because removing a card reflows everything under it.
- **It fails in Arabic** given an English string, and the bottom navigation mirrors, so a coordinate that hit Today now hits More.
- **A tap while the keyboard is up types into the focused field.** `KEYCODE_ESCAPE` does not dismiss it. Tell: a field whose character count grew by one between two reads with no typing. Press `KEYCODE_BACK` first, then dump and tap bounds.
- Swiping at a y inside the keyboard does nothing, which has read as "the button is unreachable".
- **Distrust a negative from a tool that cannot say what it did not examine.**
- **Choosing a photograph opens the owner's real photo library** (system picker, correct behavior). Anything needing a file chosen **cannot be walked from here**. Back out and say so. That is why the over-size document is asserted against the error string rather than walked with a real 30 MB file.

**Locale**

- Setting the app locale changes the words at once since #326. If it stops doing so, the catalog has stopped following the language and the fix is in `AppRoot`, keyed on the language tag rather than the context. **The old failure**: `Strings.load` was `remember(context)` and Compose keeps the same context object across a configuration change, so the layout flipped to RTL and every word stayed English, which reads as a translation defect and is not one. Force stop and relaunch: `adb shell am force-stop com.kamsiob.healthtrail`.

**Screenshots**

- **Look at every one before committing.** `screenshot.sh` refuses unless the app is focused, suppresses heads-up notifications, crops the status bar, and things still get through: a heads-up notification put the owner's phone number and a contact photo into a capture; the **system clipboard overlay** put his shell prompt into two, because it takes no focus and is not a heads-up, toast or popup (the pattern catches `clipboard` now). Both were caught by a person looking. D53, D72.
- **Never screenshot the share sheet or the calendar app** (real contacts), or **any screen with a password field** (the password manager adds its own bar).
- `screenshot.sh` appends the theme, so a name ending `-dark` yields `-dark-dark.png`. It reads the app's Appearance first, the device only when that is "follow". D31.
- It refuses while Appearance is "Follow the phone", because device night mode is `auto`: "Cannot tell what theme the app is in." Set Light or Dark, capture, set back. Not a defect.

**Rule 19's exception.** Font scale, animation duration, TalkBack may change **only if the prior value is recorded first and restored exactly**. Nothing else on the device, including the phone's dark mode: change the app's Appearance instead.

    adb shell settings get system font_scale                     # expect 1.0
    adb shell settings get global animator_duration_scale        # expect null: delete, do not set 1.0
    adb shell settings get global heads_up_notifications_enabled # expect 1
    adb shell settings get secure enabled_accessibility_services # expect the KDE Connect string, NOT TalkBack
    adb shell cmd locale get-app-locales com.kamsiob.healthtrail # expect []

If a run ends with TalkBack on:

    adb shell settings put secure enabled_accessibility_services org.kde.kdeconnect_tp/org.kde.kdeconnect.plugins.mousereceiver.MouseReceiverService
    adb shell settings put secure accessibility_enabled 0

**Say when the phone can be unplugged.** The owner waits to be told.

**Tools.** `walk.sh see` prints the semantics tree (text plus `desc:` descriptions). `walk.sh tap "X"` taps and prints what it tapped. `walk.sh fields` lists editable fields with coordinates. By hand: `adb shell uiautomator dump /sdcard/w.xml`, tap the center of a node's bounds. `python3 tools/checks/report_today_rungs.py` says which rung of `DESIGN.md` 21.4 each Today card lands on per fixture: a report, not a gate, and its queries are a second copy of `Repository.todayAnswers`, so **if a rung it promises is not on screen, believe the screen**. The empty Today comes only from a cleared install plus walking onboarding.

---

## 2. The tests

- **A green suite can be green on a path it never runs.** `MergeApplyTest` had five cases around the importer's insert path and no coverage of it, because every archive it merged came from the notebook it merged into: nothing was ever inserted. The path crashed on the phone the first time a real second notebook arrived. **Ask what the test data makes reachable**, not what the names say.
- **`tools/verify.sh` is the only honest runner.** Captures every exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, exits nonzero naming what failed. CI once failed on a lint error in code walked on the device that had passed every check and 185 instrumented tests.
- **`verify.sh` skips the instrumented suite without `--device`**, under a heading reading "Skipped, which is not the same as passed". A run with 218 unit tests and no device tests exits zero and is not a verified build.
- **A command that never ran looks exactly like one that ran clean.** `./gradlew` is in `android/`, not the root; a root invocation piped through `grep -E "^e:|error:"` prints nothing and reads as a pass. Did it twice in a row. **Grep for the success line** (`BUILD SUCCESSFUL`, a test count), never for failure.
- **"Checks pass" does not mean the suite compiles.** `compileDebugAndroidTestKotlin` is not in the main compile path. `ScreenReaderTest` was broken a day while `run_all.py` and `compileDebugKotlin` were clean.
- **Copy the report before rerunning anything.** A single-class rerun overwrites `androidTest-results/connected/debug/TEST-*.xml`; two flakes lost their assertion and stack that way.
- **Never start a second `verify.sh --device` while one runs.** Tell: the first test fails, the run dies after one test, log says `DELETE_FAILED_INTERNAL_ERROR`, `Error during Sync: Remote object doesn't exist!`, `Process crashed`. Reads as the product crashing on launch. Guard as its own call, printing lines: `ps -eo pid,cmd | grep '[v]erify.sh'`.
- **Never edit a shell script while a long run of it is in flight.** Bash reads scripts incrementally: editing `verify.sh` mid-run gave `e_step: command not found` then a syntax error, on a script that passes `bash -n`. The run is corrupted, not the file.
- **Do not touch the phone while the suite runs.** A capture mid-run force-stopped the app under it.
- **Never put a short `timeout` on a device run.** It kills mid-suite and leaves the app uninstalled.
- **A screen that clears its descendants' semantics cannot be tested through its text and will not say so.** `RoadStrip` and every Today card speak as one node, so `onNodeWithText` finds nothing inside them; fourteen `TodayFieldScreenTest` cases failed looking exactly like the defect they were written to catch. Assert on the composed `contentDescription`, stripping isolate marks.
- **A merged node's testTag assertion passes when the line is absent** and fails when it is there. Assert on words.
- **`performScrollToNode` gives up early and lies about it.** Use `performScrollToKey`, which asks the list where the item is; needs the test tag on the `LazyColumn` itself, because the scroll action merges upward and looks like it works while `IndexForKey` does not.
- **`performScrollTo` fails on a pinned footer or a non-scrolling parent.** Drop the scroll.
- **`performScrollTo` cannot reach anything below a `SectionScaffold`'s fold.** The scaffold renders through a `LazyColumn`, so the node is not off screen, it does not exist, and the failure says "could not find any node that satisfies". Scroll the list: `onNodeWithTag(SectionTags.root(NAME)).performScrollToNode(hasTestTag(TARGET))`.
- **A composed control whose center is outside the window swallows a tap silently.** `performClick` reports success and nothing happens, so the test fails on the value it was checking rather than on the node, which sends you to the writer instead of the layout. Scroll to it first.
- **Both of these appear when text gets bigger, not when the test changes.** Seven tests broke the night the type ladder was lifted, five on one screen, and none was a defect in a screen. Confirmed by putting the old ladder back and watching them pass.
- **Two `setContent` calls in one test** give "already set content". Split the test.
- **`live_entry` has no `rowid`.** Order by `id`.
- **A test that changes remembered state puts it back**: view toggles, an open `Disclosure`, the app locale. `AppointmentsMonthTest` taught this; #308 is the class that still has not learned it.
- **Clear the app locale before a suite**: `adb shell cmd locale set-app-locales com.kamsiob.healthtrail --user 0 --locales ""`. `connectedDebugAndroidTest` only clears it when it actually reinstalls. #306.
- **`AppLanguageTest` fails in a full run and passes alone for two reasons.** Setting `applicationLocales` is asynchronous, and **the context's own configuration catches up separately**, which is what `Strings.load` reads. It once asserted Japanese falls back to English and got Arabic, chosen by the test before it. **A poll on the wrong object is indistinguishable from no poll.**
- **The keyboard now moves the screen** (`SectionScaffold`, 2026-08-13), so a control below a field is genuinely off screen while it is up. Close it before tapping.
- **Batch it.** One class while iterating, the whole suite before committing.

---

## 3. Words and languages

- **A `{count}` outside a plural form reads "1 entries".** `trail.search.hint` said "Search 1 entries"; the progress chart told a screen reader "1 readings". **The one-item state is the one nobody has**, because every fixture has several of everything. Ask what a new count string says at one, and at zero.
- **A catalog key is a string literal the compiler never sees.** `ChangeSituationScreen` asked for `more.title`, which never existed, and `Strings.resolve` throws rather than falling back: the screen crashed the app the first time it opened, having passed seventeen checks, the compiler and lint, because `check_i18n.py` holds the four catalogs to **each other** and nothing held the **code** to them. `check_string_keys.py` reads the other direction now.
- **A key built from a variable is checked by nothing.** Prefer a literal in a `when`. Where dynamic is unavoidable, hold the whole set another way, as `TodayCardKeyTest` and `check_readable_labels.py` do.
- **A dynamic key whose miss is silent is worse than one that throws.** The archive's `archive.field.${column}` labels were skipped by the key check while `check_i18n.py` only compared catalogs to each other: **four catalogs agreeing a key is absent passed both**, and a missing label falls back to the column name with underscores opened out. An Arabic archive rendered, linked, counted and looked finished, in English. #327.
- **A parser hides a duplicate from every check that reads the parsed thing.** All four catalogs carried `project.step.handled_by` twice; `check_i18n.py` compares dictionaries so the second copy was gone before comparison. Harmless by luck. It reads raw text now.
- **Almost no bidi defect is visible in English at font scale 1.0.** `Bidi.isolate`/`Bidi.join` are the fix; `check_bidi_isolation.py` fails the build on any place neither isolated nor annotated `// bidi-ok:` with a reason. **A green check means every place has been decided, not that every decision is right.** #226.
- **Anything joined with a separator by hand is a candidate.** `RoadStrip`'s fallback line concatenated stage names so they did not flip while the road did. `Bidi.join`'s default separator is already ` · `.
- **Never hand `Bidi.join` a string already through it.** Isolates nest: `⁨⁨Next up⁩⁩ · ...`. Compose from raw parts, join once, at the end. Written into this file twice now.
- **A stored value is not display text.** An EDTF string, a project status, a currency amount each nearly reached a screen as itself. Convert in one place; dates go through `EventDateText` so precision is never invented.
- **All three reached the archive and stayed for months**: `المبلغ 679040`, `أين وصلت paid`, `1781701200000` where a page meant to say when somebody answered, `0` under "Someone answered". **Screens are fine because somebody looks at them.** Nothing looks at a readable page until a family opens it in ten years. **Only grepping a produced archive finds this**: long integers, bare digits, schema tokens. #328.
- **A helper written for exactly this failure and called by nothing is the pattern, not the exception.** `ReadablePage.attachment` shipped uncalled and forty photographs went unreferenced; `ReadableDate.timestamp`, whose comment says "never a bare epoch number", was uncalled while six columns printed epochs. **When a helper's comment describes a defect precisely, check who calls it.**
- **A date pattern is a translated string that is also code**, and a bad one throws in one language only. `DateFormatPatternTest` compiles every `date.format.*` in all four catalogs and checks no rendered date contains a number the date does not have, which catches an unquoted word like the Spanish `de`.
- **No sentence a person reads is assembled from pieces in Kotlin.** One catalog key, parts as named arguments. `check_concatenation.py`.

---

## 4. Checks

- **Prove it by breaking the data and watching it fail.** A check that has never failed is a check nobody knows the shape of.
- **A reader that finds nothing makes every assertion pass.** Assert the reader found something first. Two tests passed on zero strings because `^` in a Kotlin `Regex` is not multiline unless told.
- **A probe that edits a real file is restored by copy, never by git.** `git checkout -- templates/data/projects.json` is banned by rule 6 and discarded an hour of uncommitted work on the same file. Copy into the scratchpad and copy back, or commit before probing.
- **A local check passing is not the check passing.** A fixture check passed here and failed in CI because this laptop is in New York and CI is UTC. **Prefix anything computing a timestamp with `TZ=UTC`.**
- **A tool reporting a count is claiming something.** `board.py sync` said "200 added" then "63 added" on an immediate rerun, because both reads capped at 200 rows: it was silently resetting Status past the cap. **A count that changes when nothing did describes something other than what it did.** A read that comes back exactly full was cut off.
- **Compose lint's `ModifierParameterDetector` crashes on some token references inside a modifier chain and fails the build naming no rule.** `.clip(Radius.referenceLine)` crashed `lintAnalyzeDebug` with "this is a bug in lint" while the same chain with a literal shape passed. **Hold the token in a local first.** Looks like neither a code defect nor a tooling one: no rule named, points at the file rather than the line.
- **Read `lint.log` from the run you are looking at.** `verify.sh` writes each run to its own `/tmp/tmp.XXXX`; an older directory beside it will say BUILD SUCCESSFUL about a different run. Sort by time.
- **D143: ten cross references in `DECISIONS.md` and `docs/RUN-LOG.md` point at `DESIGN.md` sections that no longer exist and are not a bug.** They are dated records. `check_cross_references.py` skips both files by name. **Do not fix them, do not widen the check.**
- **Anything checked only through a parser has a blind spot.** See section 3.

---

## 5. Screens

- **Look at it on the phone.** Nearly every defect found on 2026-08-06 and 08 was invisible in the code and obvious in a screenshot: a tab under the corner chevron; a tall card reserving height it could not fill; a query reading the wrong column so a card said "No readings yet" above its own chart; a list repeating the answer above it; a "more" line subtracting clusters from steps.
- **A row whose only behavior is edit is a screen nobody built.** A bill and a document both opened the form that edits them, which answers "tell me about this" with "change this".
- **Check what is at the top of a detail screen.** One project opened with five status chips and an empty field taking a third of the fold, above four identical step cards, one of which was the answer. **The controls that describe a thing are not the thing.**
- **Check what carries the accent.** One incident put its filled action on marking it answered, done once at the end, rather than on adding what happened next, which is why the screen gets opened.
- **State the answer, then fold the volume.** One chapter had 293 entries on screen at one weight; one care thread had 174.
- **Not everything is a card.** The prep sheet's questions were eight cards on a spine each repeating its role in mono. A question is one sentence, which is a row. Rule 22. Where a wall of something has a solved composition elsewhere, use that one.
- **A surface with no test of its own loses whatever the screen it replaced carried.** `TodayFieldScreen` replaced Today on every real notebook with **no way into search at all**, because the old screen had it in the header. It had no test file, so nothing said so. **Check what the superseded screen did that the new one does not**, and write the test when the surface lands.
- **A control that came off a superseded screen does not come back because its call survived.** `saveProjectAsTemplate` kept its repository call and shell state while the control did not exist. **Nothing catches this shape**; it was found by reading `docs/REMOVAL-LEDGER.md` against the app, which is what the ledger is for. #314.
- **A defect can live entirely inside somebody else's app.** The calendar hand-off put a November 27 appointment on the 26th while the screen said November 27 throughout. Three attempts; none of the causes was time zones.
- **`walk.sh see` shows the unmerged tree, so it cannot tell you how many stops a reader has.** A card merging its parts into one sentence still prints every part on its own line, which reads as "a reader stops six times" and is false. Twenty minutes went into fixing a defect the tool invented. **Only the Compose test API sees the merged tree**: `onNodeWithTag(...).fetchSemanticsNode().children`. What `walk.sh` does catch is real: a `clearAndSetSemantics` card shows only its sentence, so parts appearing means something is outside the clear.
- **A number with a space never reached the dialer.** `Uri.fromParts("tel", number, null)` escapes nothing, so `tel:555 0142` opened the keypad blank while `tel:555%200142` filled it in. Every number the fixture holds has a space, and so does almost every number anybody writes down: the one tap the care team promises landed on an empty screen for as long as dialing existed. **Encode the scheme specific part.** Proved by starting the intent three ways from `adb`; the code looked correct.
- **New optional parameters go after `modifier`** or lint's `ModifierParameter` fails the build. Three failures in one day.

---

## 6. Committing

- **Run `tools/verify.sh`**, not the checks you happen to remember.
- **Prefix anything computing a timestamp with `TZ=UTC`.**
- **Never chain a commit on a grep of output.** A grep that finds the error string succeeds, and the commit runs anyway. Happened 2026-08-13: a lint failure was pushed behind `&&`.
- **Commit and push per increment.** An increment ends when `origin/main` has it. Rule 7.
- **Check CI after every push**: `gh run list --branch main --limit 3`. A clean tree and green local checks say nothing about it.
- **Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any PR changing `android`, `web`, `tools` or `contract` without touching it. "README.md describes the screens that exist" fails one adding or removing a file under `ui/screens/` without touching `README.md`. "Every screenshot the README points at exists" catches a rename. **Rewrite the documents in the same commit as the work.**
- **An issue closes only on device verification**: both themes, font scale 2.0, RTL, every state in `DESIGN.md` 13.3 including empty, and a screenshot looked at before it is committed.
- **An edit that replaces text must assert it matched.** Nine decision entries were written and none reached `DECISIONS.md`: the anchor had been consumed by an earlier edit, and every one matched nothing and reported success. **A silent no-op is worse than an error.**
- **After editing `CLAUDE.md`, read the rules back from disk.** The copy in context is from session start.
- **The destructive-command hook matches prose**, so writing certain verbs into a file is refused. #323. Not a reason to weaken it. Use the Edit tool rather than a heredoc when it bites.
- **`gh issue comment` takes no `-q` and fails quietly when given one.** Prints nothing, posts nothing, looks like a comment that posted silently. **Check the issue, not the exit code.**
- **Backticks in a `gh issue comment --body` are run by the shell.** A comment on #46 posted with four code spans replaced by nothing while the shell said `command not found` four times mid-post. **Write the body to a file and pass `--body-file`**, or `-F body=@file` through the API. The damage is silent: the sentence still reads as a sentence, with the names missing.

---

## 7. This machine

- **The shell carries no state between tool calls.** Every command starts fresh.
- **`ANDROID_HOME` is not set.** SDK at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and **absent in a fresh clone**: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH**: `/home/Kamsiob/Android/Sdk/platform-tools/adb`.
- **The working directory contains a space and two leading dashes.** Quote every path.
- **Gradle is fast and it looks broken.** An incremental Kotlin recompile of several files takes about a second. That is real.
- **`pkill -f <pattern>` matches its own command line and kills the shell running it.** `pkill -f "connectedDebugAndroidTest" ; bash tools/verify.sh ...` killed itself before the redirect created the log: the failure was a missing file and exit 144, not anything about tests. Check for the process and act on what you find.
- **`[t]ools/verify.sh` counts its own command line and the bracket trick does not save it** when run in the same compound command that starts the run: the parent shell's command line contains the string. Reported 2 with nothing running. **Run the guard as its own call and print lines, not a count.**
- **A release by somebody else used to turn CI red on an unchanged tree.** `warningsAsErrors` made lint's version currency checks build breaking, so `lintDebug` failed naming a file nobody had edited whenever Gradle or a dependency published. Twice in an hour. `NewerVersionAvailable`, `AndroidGradlePluginVersion` and `GradleDependency` are disabled with their reason; **Dependabot owns staying current**. D121, D148.
- **CI** triggers on push to main, pull_request and workflow_dispatch. If PR events stop firing: `gh workflow run ci.yml --ref <branch>`, then poll. **An absence of checks on a PR is not a passing build.**
- **Versions**: Gradle 9.7.0, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` reaches `Edtf.kt` without desugaring.
- **Android's `execSQL` refuses any statement returning rows** and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles splitting including trigger bodies and routes pragmas through `rawQuery`. **Reuse it rather than writing a second splitter.**
