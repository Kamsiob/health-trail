# TRAPS.md, Health Trail by Kamsiob

**Read the one section that matches what you are about to do. Do not read this file end to end.**

That instruction is the point of the file. Every trap below cost real time at least once, and most cost it twice, so they are all worth keeping. But a session that loads all of them at the start has spent a third of its context on device warnings before finding out whether it is doing device work. **`HANDOFF.md` is current state and is read every session. This is conditional knowledge and is read on demand.**

Each section says when it applies. If you are not doing that thing, skip it.

| If you are about to | Read |
|---|---|
| Touch the phone at all | [1. The phone](#1-before-you-touch-the-phone) |
| Run or believe the tests | [2. The tests](#2-before-you-run-or-believe-the-tests) |
| Change copy, a catalog, or anything a person reads | [3. Words and languages](#3-before-you-change-copy-or-a-catalog) |
| Write or change a check | [4. Checks](#4-before-you-write-or-change-a-check) |
| Design, build, or change a screen | [5. Screens](#5-before-you-change-a-screen) |
| Commit and push | [6. Committing](#6-before-you-commit) |
| Run any command at all | [7. This machine](#7-this-machine) |

---

## 1. Before you touch the phone

**Check the phone is unlocked before anything else.** `adb shell dumpsys window | grep isKeyguardShowing`. A secure keyguard blocks every device check, and **it fails every instrumented class with a message that blames your test**: `IllegalStateException: No compose hierarchies found in the app`. Proved on 2026-08-08 by running `TodayScreenTest`, untouched and green two days earlier, and watching all twelve fail the same way. A wake and a swipe do not clear a secure lock and there is no way past it from here. #316.

**`tools/seed.sh` drives the system file picker, and a phone that has never opened it lands on Recent rather than Downloads.** The seed then reports "seed.htx is not in the picker" on a device that plainly has the file, because the push worked and the search looked in the wrong folder. **Invisible until a fresh device arrives**: the old phone had opened that picker dozens of times and always came up where the file was. Fixed on 2026-08-12, the day a Pixel 8 replaced it: the script opens the drawer, taps Downloads, and looks again. **The picker does not remember Downloads between invocations from this app**, so navigating by hand once does not fix it and the script has to do it every time.

**`tools/device.sh` used to install only when the app was missing.** A phone that already had it kept whatever build was on it, so a walk after a source change walked the change that was not there and the fix looked broken. Fixed on 2026-08-11: it installs every time and **refuses when any source file is newer than the APK**. **`compileDebugKotlin` does not build an APK**, which is the other half of the same mistake: only `assembleDebug` does.

**`installDebug` clears this app's data on this phone.** A screenshot taken straight after an install is a screenshot of onboarding. **Every device check is install, then seed, then navigate.** `tools/device.sh` does all three and refuses rather than half succeeding.

**`connectedDebugAndroidTest` uninstalls the app when it finishes.** So a seed straight afterward prints `Failed` with one word and it is never the fixture: check `pm list packages | grep kamsiob` before checking anything else. It bit three times in one night, and each time the next thing was `walk.sh see` dumping the owner's home screen with his real calendar on it. **This is also the sanctioned way back to first-run state**, since `adb uninstall` is on the blocklist. D50.

**A whole class failing identically is the environment, not the product.** On 2026-08-06 all six `BackJourneyTest` tests failed with `RootViewWithoutFocusException` while the notification shade held focus over Reddit. **Read the exception before reading the code**: `has-window-focus=false` and a `Sys2040` in `mCurrentFocus` mean nothing was ever driven. Collapse the shade, press home, rerun the class alone. #316.

**Setting the app locale does not change the words until the app restarts.** `cmd locale set-app-locales ... ar` flips the layout to right to left immediately and leaves the copy in English, because `Strings.load` is `remember(context)` and Compose keeps the same context object across a configuration change. The screenshot is an RTL layout full of English, which reads as a translation defect and is not one. **Force stop and relaunch, every time:** `adb shell am force-stop com.kamsiob.healthtrail`. #326.

**`walk.sh tap` matches on the label, so it fails in Arabic** when handed an English string, and the bottom navigation mirrors, so a coordinate that hit Today now hits More. **A NOT FOUND usually means below the fold or a different locale**, not absent. Swiping at a y inside the keyboard does nothing, which has read as "the button is unreachable" more than once.

**A tap at stale coordinates can leave the app entirely, and the worst landing is the dialer.** On 2026-08-11 a tap aimed at a trail entry landed on a care team row's Call button, which opened the phone app on the owner's real contacts. **The bounds were read from one screen and the tap was sent after navigating to another**, which is the whole mistake: a dump goes stale the moment anything moves.

**Re-dump immediately before every tap you send by coordinate**, and check `mCurrentFocus` after a tap that was supposed to stay inside the app. If it left, force stop whatever opened, relaunch, and **do not screenshot or read what is on screen** first.

**Choosing a photograph opens the owner's real photo library**, because the app uses the system picker, which is the correct thing for it to do. **Anything that needs a file chosen cannot be walked from here**: reaching a file pushed to `Download` means going through the picker's own browser, past his photographs, on his daily driver. **Back out and say so.** That is why the document over the size limit is asserted against the error string in `ScreenReaderTest` rather than walked with a real 30 MB file. Attempted 2026-08-11, backed out at the picker, and the pushed file was removed from the phone.

**`walk.sh tap` cannot match a label that carries a name, because the isolate marks are inside it.** `Bidi.join` wraps every part it is handed, so the Remove control on a Today card describes itself as `Remove ⁨Milestones⁩` and no plain string matches. It reports NOT FOUND, which reads as "below the fold" and is not. **Dump the tree and tap the bounds**, and re-dump between taps because removing a card reflows everything under it. Cost twenty minutes on 2026-08-09, and one blind tap landed on a different screen entirely.

**A tap on a button while the keyboard is up types into the focused field instead.** `KEYCODE_ESCAPE` does not dismiss the soft keyboard and `walk.sh tap` does not check for it, so a tap aimed at a control below the fold lands on a key. On 2026-08-09 the export screen's confirm passphrase field gained exactly one character each time the save button was pressed, and the screen correctly said the two did not match. **It reads as a typing defect and it is not one:** the give-away is a field whose character count grew by one between two reads with no typing in between. **Press `KEYCODE_BACK` first**, then dump and tap the bounds.

**`tap` matches on a substring, so a short word hits the longest sentence containing it.** `walk.sh tap "Remove"` matched a care team card whose answer is "Closed. Still here until you remove it." and opened the card instead of the control. Same night.

**Distrust a negative result from a tool that cannot say what it did not examine.** Five times in one night and several since.

**Look at every screenshot before committing it, and `screenshot.sh` is not the last control.** It refuses unless the app is focused, suppresses heads-up notifications and crops the status bar, and things still get through. A heads-up notification once put the owner's phone number and a contact photo into a capture. On 2026-08-08 the **system clipboard overlay** put his shell prompt into two, because it never takes focus and was not a heads-up, a toast or a popup; the pattern catches `clipboard` now. **Both times the image was caught by a person looking at it.** D53, D72. **Never screenshot the share sheet or the calendar app**: they show real contacts.

**`screenshot.sh` appends the theme**, so a name ending in `-dark` yields `-dark-dark.png`. It reads the app's own Appearance choice first and the device only when that choice is to follow it. D31.

**Rule 19's exception and its condition.** Font scale, animation duration and TalkBack may be changed **provided the prior value is recorded first and restored exactly**. It does not extend to anything else on the device, including the phone's own dark mode: change the app's Appearance setting instead. Read the baseline before touching anything:

    adb shell settings get system font_scale                     # expect 1.0
    adb shell settings get global animator_duration_scale        # expect null, so delete rather than set to 1.0
    adb shell settings get global heads_up_notifications_enabled # expect 1
    adb shell settings get secure enabled_accessibility_services # expect the KDE Connect string, NOT TalkBack
    adb shell cmd locale get-app-locales com.kamsiob.healthtrail # expect []

**If a run ends with TalkBack still on:**

    adb shell settings put secure enabled_accessibility_services org.kde.kdeconnect_tp/org.kde.kdeconnect.plugins.mousereceiver.MouseReceiverService
    adb shell settings put secure accessibility_enabled 0

**Say when the phone can be unplugged.** The owner waits to be told, and most work needs no device.

### Driving it

- `tools/device.sh` puts the phone in a usable state in one step. Use it rather than `seed.sh` directly.
- `tools/seed.sh` defaults to month six. `tools/seed.sh year5 5 walk-year-five` for the long record. It goes in through the app's own restore screen deliberately, per D61: a fixture that arrives any other way has never been through the importer. **If it says the file is not in the picker, something else has the focus**; press back and home and run it again.
- `tools/walk.sh see` prints the semantics tree, which is what a screen reader walks: both the text and the content descriptions, marked `desc:`. `tools/walk.sh tap "Medications"` taps and prints what it tapped. `tools/walk.sh fields` lists editable fields with coordinates.
- By hand: `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds.
- **Which fixture shows which state:** `python3 tools/checks/report_today_rungs.py` builds all six horizons and prints, per Today card, which rung of `DESIGN.md` 21.4 it lands on. A report and not a gate, and its queries are a second copy of `Repository.todayAnswers`, so **if a rung it promises is not on the screen, believe the screen**. **The empty Today is reachable from no seed at all**: it comes from a cleared install and walking onboarding.

---

## 2. Before you run or believe the tests
**A green suite can be green on a code path it never runs.** `MergeApplyTest` had five cases around the importer's insert path and no coverage of it at all, because every archive those cases merge came from the notebook they merge into: everything was unchanged and nothing was ever inserted. The path crashed on the phone the first time a real second notebook arrived. **Ask what the test data makes reachable**, not what the test names say.


**`tools/verify.sh` is the honest runner and the only one that reaches everything.** It captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. CI once failed on a lint error in code that had been walked on the device and had passed every content check and 185 instrumented tests. Running `run_all.py` plus the suite by hand feels like verifying and skips whatever is not in that habit.

**A command that never ran looks exactly like a command that ran clean.** `./gradlew` does not exist at the repository root, it is in `android/`, and a root invocation piped through `grep -E "^e:|error:"` prints nothing at all: no wrapper, no output, no lines matched, and the step reads as a pass. It did that twice in a row on 2026-08-11, once for a compile and once for what was supposed to be the entire instrumented suite. **Grep for the success line rather than for failure**, `BUILD SUCCESSFUL` or a test count, so silence cannot be mistaken for a clean run. Better still, use `tools/verify.sh`, which asserts on exit codes rather than on output.

**`tools/verify.sh` skips the instrumented suite unless it is passed `--device`.** It says so in its own last lines, under a heading that reads "Skipped, which is not the same as passed". Reading those lines rather than the exit code is the whole difference: a run with 218 unit tests and no device tests exits zero and is not a verified build.

**"Checks pass" does not mean the suite compiles.** `compileDebugAndroidTestKotlin` is not in the main compile path. `ScreenReaderTest` was broken for a day while `run_all.py` and `compileDebugKotlin` both reported clean.

**Copy the suite's report before rerunning anything.** A single class rerun overwrites `androidTest-results/connected/debug/TEST-*.xml`, and two flakes lost their assertion and stack that way. Copy it into the scratchpad the moment the suite goes red.

**`AppLanguageTest` fails inside a full run and passes alone, and the locale leak is not the only cause.** Setting `applicationLocales` is asynchronous and the class already polls for it; **the context's own configuration catches up separately**, and `Strings.load` reads the configuration. On 2026-08-11 it asserted that Japanese falls back to English and got Arabic, which is what the test before it had chosen. It now waits for the requested language to reach the head of `context.resources.configuration.locales` as well. **A poll on the wrong object is indistinguishable from no poll at all.**

**Clear the app locale before running the suite.** `AppLanguageTest` failed twice because a right to left check left the per app locale set to Arabic: `connectedDebugAndroidTest` only clears it when it actually reinstalls. `adb shell cmd locale set-app-locales com.kamsiob.healthtrail --user 0 --locales ""` first. #306.

**A screen that clears its descendants' semantics cannot be tested through its text, and will not tell you so.** `RoadStrip` and every Today card speak as one node by design, so `onNodeWithText` finds nothing inside them. **A test written against the rendered text fails looking exactly like the defect it was meant to catch**, which is what fourteen of `TodayFieldScreenTest`'s did the first time they ran. Assert on the composed `contentDescription` instead, and strip the isolate marks before comparing.

**`performScrollToNode` gives up early and lies about it.** It stopped two rows short on the Arabic catalog and reported the rows as absent. **Scroll by the list's own item key** with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` itself: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Never start a second `verify.sh --device` while one is running, and check rather than assume.** One run uninstalls the app while the other installs it, and the symptom does not look like a collision: **the first test fails, the run dies after one test, and the log says `DELETE_FAILED_INTERNAL_ERROR`, `Error during Sync: Remote object doesn't exist!`, and `Process crashed`.** That reads like the product crashing on launch. On 2026-08-11 a run was started while the previous one was still in its instrumented phase, having read the process count in the same command that launched the new run rather than before it. **`ps -eo cmd | grep -c '[t]ools/verify.sh'` first, as its own step, and read the answer.**

**Do not touch the phone while the suite runs.** A capture attempted mid-run force-stopped the app under the suite and produced one bogus failure.

**Batch it.** Run the one class you changed while iterating; run the whole suite once before committing. It costs about seven and a half minutes.

---

## 3. Before you change copy or a catalog

**A `{count}` that is not inside a plural form reads "1 entries".** The catalog has 52 ICU plural strings and had five flat ones, and two of those put a number next to a noun: `trail.search.hint` said "Search 1 entries" and the progress chart described "1 readings" to a screen reader. **The one-item state is the one nobody has**, because every fixture has several of everything. Fixed 2026-08-11. When you add a string with a count in it, ask what it says at one, and at zero.

**A catalog key is a string literal and the compiler never sees it.** `ChangeSituationScreen` asked for `more.title`, which has never existed, and `Strings.resolve` throws rather than falling back. The screen crashed the app the first time it was opened, having passed seventeen checks, the Kotlin compiler and lint, because `check_i18n.py` holds the four catalogs to **each other** and nothing held the **code** to them. `check_string_keys.py` reads the other direction now.

**A key built from a variable is checked by nothing.** `check_string_keys.py` skips it by design, and its stated safety net is the instrumented suite, which is a net with a hole in it whenever the phone is unreachable. **Prefer a literal in a `when`.** Where a dynamic key is unavoidable, hold the whole set some other way, as `TodayCardKeyTest` holds the schema's card types to the catalog and `check_readable_labels.py` holds the archive's rendered tables and columns to all four.

**The archive's version of this had no crash to warn anybody.** Its labels are `archive.field.${column}`, so `check_string_keys.py` skipped them and `check_i18n.py` only compared the catalogs to each other: **four catalogs agreeing that a key is absent passed both checks**, and a missing label falls back to the column name with its underscores opened out. So an Arabic archive rendered, linked, counted and looked finished, in English. #327. **A dynamic key whose miss is silent is worse than one that throws**, and it is the one that needs the set held somewhere.

**A parser hides a duplicate from every check that reads the parsed thing.** All four catalogs carried `project.step.handled_by` twice and `check_i18n.py` compares dictionaries, so the second copy was gone before the first comparison ran. It happened to be harmless, which was luck. The check reads raw text for this now.

**Almost no bidi defect is visible in English at font scale 1.0.** Text the person typed gets rearranged in Arabic. `Bidi.isolate` and `Bidi.join` are the fix, `DESIGN.md` section 15 carries the rule, and `check_bidi_isolation.py` now fails the build on any place that is neither isolated nor annotated `// bidi-ok:` with a reason. **A green check means every place has been decided, not that every decision is right**: only Arabic on the device shows that. #226.

**Anything joined with a separator by hand is a candidate.** `RoadStrip` lays its waypoints out so they flip in Arabic, and its fallback line concatenated the stage names by hand so they did not: the road ran one way while its own names ran the other. `Bidi.join`'s default separator is already ` · `.

**Never hand `Bidi.join` a string that has already been through it.** The isolates nest and the reader hears `⁨⁨Next up⁩⁩ · ...`. Compose from raw parts and join once, at the end. This has now been written into the same file twice.

**A stored value is not display text.** An EDTF string, a project status, a currency amount: every one of them nearly reached a screen as itself. Turn stored values into words in one place and nowhere else, and let dates go through `EventDateText` so precision is never invented.

**All three of those reached the archive, and stayed there for months.** A real export read `المبلغ 679040` and `أين وصلت paid`, plus `1781701200000` where a page meant to say when somebody answered and `0` under a label reading "Someone answered". **The screens were fine, because a screen has somebody looking at it.** Nothing looks at a readable page until a family opens it in ten years, so **the only thing that finds this is grepping a produced archive** for long integers, bare digits and schema tokens. `check_readable_labels.py` does it from the schema now, and #328 is the account.

**A helper written for exactly this failure and called by nothing is the pattern, not the exception.** `ReadablePage.attachment` shipped uncalled and forty photographs went unreferenced; `ReadableDate.timestamp`, whose own comment says "never a bare epoch number, which is the easier half to get wrong", was uncalled while six columns printed epochs. **When a helper's comment describes a defect precisely, check who calls it** before assuming it is handled.

**A date pattern is a translated string that is also code.** A bad one throws in one language only. `DateFormatPatternTest` compiles every `date.format.*` in all four catalogs and checks that no rendered date contains a number the date does not have, which is what catches an unquoted word like the Spanish `de`.

---

## 4. Before you write or change a check

**Compose lint's `ModifierParameterDetector` crashes on some token references inside a modifier chain, and it fails the build without naming a rule.** On 2026-08-11 `.clip(Radius.referenceLine)` in `ReferenceLine.kt` crashed `lintAnalyzeDebug` with "this is a bug in lint", while the identical chain with a literal shape passed. **Hold the token in a local first** and the crash goes away. It is a tooling defect rather than a code defect, **and it looks like neither**: the message names no rule and points at the file rather than the line.

**Read `lint.log` from the run you are looking at.** `tools/verify.sh` writes each run to its own `/tmp/tmp.XXXX`, and an older directory sitting beside it will happily say BUILD SUCCESSFUL about a different run. Sort by time.

**Ten cross references in `DECISIONS.md` and `docs/RUN-LOG.md` point at `DESIGN.md` sections that no longer exist, and they are not a bug.** They are dated records of what was decided against the document as it stood then, and `check_cross_references.py` skips both files by name for that reason. **Do not fix them and do not widen the check to cover them.** D143. If a tool flags them, the tool is wrong for those two files.

**Prove it by breaking the data and watching it fail.** A check that has never failed is a check nobody knows the shape of.

**A probe that edits a real file is restored by copy, never by git.** On 2026-08-05 a probe was undone with `git checkout -- templates/data/projects.json`, which rule 6 bans by name, and it discarded an hour of uncommitted work on the same file. **Copy the file into the scratchpad first and copy it back**, or commit before probing.

**A reader that finds nothing makes every assertion pass.** Guard it: assert the reader found something before asserting anything about what it found. Two tests written on 2026-08-08 passed on zero strings, because `^` in a Kotlin `Regex` is not multiline unless told, and the guard is the only reason it was visible rather than green.

**A local check passing is not the same as the check passing.** A fixture check passed here and failed in CI on the next push, because this laptop is in New York and CI is in UTC. **Prefix anything that computes a timestamp with `TZ=UTC` before pushing.**

**Anything checked only through a parser has a blind spot.** See section 3.

**A tool that reports a count is claiming something.** `board.py sync` said "200 added" and then "63 added" on an immediately repeated run, because both its reads capped at 200 rows while the repository had passed 200 issues. It was silently resetting the Status of everything past the cap. **A count that changes when nothing did is the tool describing something other than what it did.** A read that comes back exactly full is a read that was cut off.

---

## 5. Before you change a screen

**Look at it on the phone.** Almost every defect found on 2026-08-06 and 2026-08-08 was invisible in the code and obvious in a screenshot. Five on Today in one afternoon: a tab running under the corner chevron, a tall card reserving height it had nothing to fill, a query reading the wrong column so a card said "No readings yet" above its own chart, a list repeating the answer already above it, and a "more" line subtracting clusters from steps.

**A row whose only behavior is edit is a screen nobody built.** One bill and one document both opened the form that edits them, which is the app answering "tell me about this" with "change this".

**Check what is at the top of a detail screen.** One project opened with five status chips and an empty text field taking a third of the fold, above four identical step cards, one of which was the answer. The controls that describe a thing are not the thing.

**Check what is carrying the accent.** One incident had its filled action on marking it answered, which somebody does once at the end, rather than on adding what happened next, which is why the screen gets opened.

**State the answer, then fold the volume.** One chapter had 293 entries on screen at one weight; one care thread had 174.

**Not everything is a card.** The prep sheet's questions were eight cards on a spine, each repeating its role in mono. Rule 22: a question is one sentence, which is a row. Where a wall of something already has a solved composition elsewhere, use that one.

**A surface with no test of its own loses whatever the screen it replaced was carrying.** `TodayFieldScreen` replaced the previous Today on every real notebook and arrived with **no way into search at all**, because the old screen had it in the header and nothing carried it across. It had no test file of any kind, so nothing said so. **Check what the superseded screen did that the new one does not**, and write the test at the moment the surface lands.

**A control that came off a superseded screen does not come back just because its call survived.** `saveProjectAsTemplate` kept its repository call and its shell state through a supersession while the control did not exist. **Nothing catches this shape.** It was found by reading `docs/REMOVAL-LEDGER.md` against the app, which is what the ledger is for. #314.

**A defect can live entirely inside somebody else's app.** The calendar hand-off put a November 27 appointment on the 26th and the screen said November 27 the whole time. Three attempts, and none of the causes was time zones.

**`walk.sh see` shows the unmerged semantics tree, so it cannot tell you how many stops a reader has.** A card that merges its parts into one sentence still prints every part on its own line there, which reads as "a reader stops six times to learn one thing" and is not true. Twenty minutes went into fixing a defect the tool had invented on 2026-08-09. **The merged tree is what a reader walks, and only the Compose test API can see it**: `onNodeWithTag(...).fetchSemanticsNode().children`. Use `walk.sh` for what is on the screen and a test for how it is heard. What the tool does catch is real: a `clearAndSetSemantics` card shows only its sentence, so parts appearing at all means something is outside the clear.

**A number with a space in it never reached the dialer.** `Uri.fromParts("tel", number, null)` escapes nothing, so `tel:555 0142` opened the keypad blank while `tel:555%200142` filled it in. Every number the fixture holds has a space and so does almost every number anybody writes down, so the one tap the care team promises landed on an empty screen for as long as dialing existed. **Encode the scheme specific part.** Proved by starting the same intent three ways from `adb` rather than by reading the code, which looked correct.

---

## 6. Before you commit

**Run `tools/verify.sh`**, not the checks you happen to remember.

**Prefix anything that computes a timestamp with `TZ=UTC`.**

**Never chain a commit on a grep of output.**

**Commit and push after every working increment**, per rule 7. **An increment ends when `origin/main` has it.**

**Check CI after every push**, `gh run list --branch main --limit 3`. A clean tree and passing local checks say nothing about it.

**Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools` or `contract` without touching that file. "README.md describes the screens that exist" fails one that adds or removes a file under `ui/screens/` without touching `README.md`. "Every screenshot the README points at exists" catches a rename. **Rewrite the documents in the same commit as the work.**

**An issue closes only on device verification**, per the `DESIGN.md` 16.4 checklist: both themes, font scale 2.0, right to left, every state in 13.3 including the empty one, and a screenshot looked at before it is committed.

**An edit that replaces text must assert it matched.** Nine decision entries were once written and none reached `DECISIONS.md`, because the anchor had been consumed by an earlier edit and every one matched nothing and reported success. **A silent no-op is worse than an error.**

**After editing `CLAUDE.md`, read the rules back from disk.** The copy in a session's context is the one from session start.

---

## 7. This machine

**The shell does not carry state between tool calls.** Every command starts fresh.

- **`ANDROID_HOME` is not set.** The SDK is at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and **does not exist in a fresh clone**. Recreate it: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH.** It is at `/home/Kamsiob/Android/Sdk/platform-tools/adb`.
- **The working directory contains a space and two leading dashes.** Quote every path.
- **Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**A release by somebody else used to turn CI red on an unchanged tree.** `warningsAsErrors` made lint's two version currency checks build breaking, so the moment Gradle or any dependency published, `lintDebug` failed naming a file nobody had edited. Twice inside an hour on 2026-08-08. `NewerVersionAvailable` and `AndroidGradlePluginVersion` are disabled with their reason and **Dependabot owns staying current**. D121.

**Versions:** Gradle 9.7.0, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. **Reuse it rather than writing a second splitter.**

**Continuous integration** triggers on `push` to main, on `pull_request` and on `workflow_dispatch`. If pull request events stop firing: `gh workflow run ci.yml --ref <branch>`, then poll. **Do not read an absence of checks on a pull request as a passing build.**

**`pkill -f <pattern>` matches its own command line and kills the shell running it.** A cleanup step that read `pkill -f "connectedDebugAndroidTest" ; bash tools/verify.sh ...` killed itself before the redirect ever created the log, so the failure was a missing file and an exit code of 144 rather than anything about the tests. **Check for the process and act on what you find**, or match on something the killing command cannot contain.

**`gh issue comment` takes no `-q`, and fails quietly when given one.** A comment written to a file and posted with `--body-file -q .html_url` printed nothing and posted nothing, which looks exactly like a comment that posted and printed nothing. **Check the issue, not the exit code.**

**Backticks in a `gh issue comment --body` are run by the shell.** A paragraph of a comment on #46 posted with four code spans replaced by nothing, and the shell said `command not found` four times in the middle of a successful post, which is easy to read past. **Write the body to a file and pass `--body-file`**, or `-F body=@file` for an edit through the API. The damage is silent in the rendered comment: the sentence still reads as a sentence, with the names missing.
