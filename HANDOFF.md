# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-08.

---

## 1. Where to start

**The board says what to do next. This file says what is true right now.** They are different jobs and neither repeats the other.

**Read issue #321, "START HERE. The order of work, kept current."** It names the next action and the order, as six milestones, and it is rewritten at the end of every session. `gh issue view 321`.

**Do not read `docs/RUN-LOG.md` to orient.** It is history, it is a thousand lines, and nothing in it is current state.

### Verified rather than asserted, 2026-08-08

- The working tree is clean and everything is on `origin/main`. **Check rather than trust**: `git status --porcelain`.
- **17 repository checks pass**, `python3 tools/checks/run_all.py`. **`tools/verify.sh` is the honest runner**: it is the only one that compiles the instrumented sources and runs lint, and both have broken CI on work already walked on the phone.
- **176 unit tests pass and they need no phone.** `tools/verify.sh` runs them. On a night when the device is unreachable this is most of what is left, and it is worth writing logic into a place these can reach rather than only into a composable.
- **479 instrumented tests pass**, last full run 2026-08-08 on the unlocked phone.
- **Continuous integration is green on `main` at the tip.** **Check after every push**, `gh run list --branch main --limit 3`. A clean tree and passing local checks say nothing about it.
- **The phone is attached, unlocked, installed, seeded and at its starting values**, each read back rather than assumed: font scale 1.0, animator null, no per-app locale, the accessibility services string the KDE Connect one, the app theme following the phone. **It was behind a secure keyguard from 01:30 to about 08:10 EDT on 2026-08-08**, which blocked every device check in that window; section 7 has what that looks like.
- **`tools/device.sh` puts the phone in a usable state in one step** and refuses if the app is not frontmost. Use it rather than `seed.sh` directly.
- **The destructive command guard is live and proven**, not blocked. It refused a real removal command on 2026-08-07. Section 9.

### The five rules that actually get broken

1. **Run `tools/verify.sh`**, not the checks you happen to remember.
2. **An issue closes only on device verification**: both themes, font scale 2.0, right to left, and the empty state.
3. **Commit and push after every working increment**, and check CI after each push.
4. **The fixture must produce rows the app itself could write.** Five defects of that family in one run.
5. **Look at the screen before closing anything.** Almost every defect found on 2026-08-06 was invisible in the code and obvious on the phone.

### Working faster, which the owner has asked for

- **Batch the instrumented suite.** Run the one class you changed while iterating; run the full suite once before committing. It costs seven and a half minutes each time.
- **An instrumented run removes the app when it finishes**, so a seed straight afterward fails with one word and the next walk dumps the owner's home screen. `tools/device.sh` handles it.
- **Prefix anything that computes a timestamp with `TZ=UTC`** before pushing. CI runs in UTC and this laptop does not, and that difference cost a red build.
- **Navigating by label from inside a script does not work reliably.** #322 has what was tried and what to do instead.

## 6. What is built

**Design direction v4 is adopted and most of the app is in it.** `reference/screen-grid.html` is the v4 grid. `DESIGN.md` was rewritten rather than patched.

- **Step 1, the foundation: complete.** Every token in both themes, the type scale with all three faces verified per locale, the geometry, and all sixteen components. #149 through #168 closed.
- **Step 2, the four destinations: complete.** #169 through #172 closed.
- **Step 3, the section screens: complete but for #182**, which is blocked. Fourteen closed on device verification.
- **Step 4, the detail screens: thirteen of twenty closed.** #189 through #198, #200, #201 and #202. #199 is blocked; #203 through #208 are untouched.

**#192, one medication, closed with its remainder split out rather than left vague.** Its questions are built and the fixture never exercises them, **#229**; its incidents cannot be expressed because the schema has no link from an incident to a medication, **#230**, which is the owner's call.

**Milestone 1, Today: the surface is real and nothing in it is closed.** The night of 2026-08-08 was worked with the phone behind a secure keyguard, so everything below is built, pushed and green in continuous integration, and **every one of it still owes its device verification**. Do not close any of it from the code.

| What changed | Issues |
|---|---|
| **The lead is the hero costume**, not a wide card. D119 | #292, #270 |
| **The universal search door**, which the surface had been missing entirely. D120 | #292 |
| **Every counting card carries the noun under its number**, and wide shows the list with "and N more" | #247, #250, #255, #258, #260 |
| **Every card says its own "nothing yet"**, rather than fourteen cards sharing one sentence | 21.4's none-yet rung |
| **The measure card answers with the value** and draws its shape at tall | #248 |
| **Every card pointing at one thing names it on the tab** | #248, #251, #252, #253 |
| **The three project cards answer their own questions**, countdown and "passed N days ago" included | #251, #252, #253 |
| **The instructions card answers whether anything was not followed** | #261 |
| **Next up says Today and Tomorrow in words** | #246 |
| **`TodayFieldScreenTest` exists.** The surface had no test of any kind | all of them |

**Two things that reached the model and nearly reached the screen.** A raw EDTF string in a card's list, and a project's stored status value. Both are the same defect: a stored value is not display text. Stored values become words in `worded()` and nowhere else.

**What is left in milestone 1, by issue:** the care team source picker and its dialable number #258, which #258 now scopes in full; the tall mini spine on the trail #259; thumbnails on documents #260; and screens #293 through #301, which are mostly device verification of what is now built.

**THE ARCHIVE is largely built and proved on real hardware**, not asserted: a two-layer container at format version 3, a readable copy of 61 pages, a standalone decryptor at `tools/decrypt/` tested in CI, and the format published byte for byte in `contract/EXPORT-FORMAT.md`. `docs/RUN-LOG.md` has the account and what each piece was proved with.

---

## 7. What keeps going wrong, so it stops

**These are patterns, not history. Every one of them has now happened more than once.**

**A row whose only behavior is edit is a screen nobody built.** One bill and one document both opened the form that edits them, which is the app answering "tell me about this" with "change this". Both also carried schema links nothing read. **Check the remaining detail screens for the same shape.**

**Check what is at the top of a detail screen.** One project opened with five status chips and an empty text field taking a third of the fold, above four identical step cards, one of which was the answer. The controls that describe a thing are not the thing.

**Check what is carrying the accent.** One incident had its filled action on marking it answered, which somebody does once at the end, rather than on adding what happened next, which is why the screen gets opened.

**State the answer, then fold the volume.** One chapter had 293 entries on screen at one weight; one care thread had 174.

**Not everything is a card.** The prep sheet's questions were eight cards on a spine, each repeating its role in mono. Rule 22: a question is one sentence, which is a row. Where a wall of something already has a solved composition elsewhere in the app, use that one rather than inventing a second answer.

**The sweeps are where the defects are, and almost none is visible in English at font scale 1.0.** Text the person typed gets rearranged in Arabic; `Bidi.isolate` and `Bidi.join` are the fix and `DESIGN.md` section 15 carries the rule. `report_bidi_isolation.py` generates the remaining worklist, 76 candidates, tracked at **#226**.

**A catalog key is a string literal, and nothing was checking the literals.** `ChangeSituationScreen` asked for `more.title`, which has never existed, and `Strings.resolve` throws rather than falling back, which is correct. The screen crashed the app the first time it was opened, having passed seventeen checks, the Kotlin compiler and lint, because `check_i18n.py` holds the four catalogs to **each other** and nothing held the **code** to them. `check_string_keys.py` reads the other direction now and was proved against the real crash.

**A screen added to the shell is a screen the instrumented suite has to be told about, and "checks pass" does not mean the suite compiles.** `ScreenReaderTest` had been broken since `050ac27`: the arc added a parameter to `ChaptersScreen` and nothing recompiled the test source, so the whole suite could not build for a day while `run_all.py` and `compileDebugKotlin` both reported clean. **`compileDebugAndroidTestKotlin` is not in the main compile path.** Run `tools/verify.sh`, which is the only runner that reaches all of it.

**A defect can live entirely inside somebody else's app.** The calendar hand-off put a November 27 appointment on the 26th, and the screen said November 27 the whole time. It cost three attempts and none of the causes was time zones.

**A probe that edits a real file has to be restored by copy, never by git.** Proving a checker catches what it claims means breaking something on purpose and putting it back. On 2026-08-05 that was put back with `git checkout -- templates/data/projects.json`, which is a destructive command rule 6 bans by name, and it discarded an hour of uncommitted work on the same file rather than the probe. **Copy the file into the scratchpad first and copy it back**, or commit before probing. Nothing was lost permanently because the change was scripted and was regenerated, which was luck rather than a safeguard. **This is what B5 exists to prevent and it is the first time the missing guard has cost anything.**

**`installDebug` clears this app's data on this phone.** Twice in a row an install was followed by the app opening at "Before you start" with an empty notebook. **Every device check is install, then `tools/seed.sh`, then navigate**, and a screenshot taken straight after an install is a screenshot of onboarding.

**Clear the app locale before running the instrumented suite.** `AppLanguageTest` failed twice tonight because a right to left check left the per app locale set to Arabic by hand: `connectedDebugAndroidTest` only clears it when it actually reinstalls, and on an up to date install nothing wipes it. `adb shell cmd locale set-app-locales com.kamsiob.healthtrail --user 0 --locales ""` first. **#306 is reopened**, with three attempted fixes that did not work written into it.

**Copy the suite's report before rerunning anything.** A single class rerun overwrites `androidTest-results/connected/debug/TEST-*.xml`, and both flakes found this week, #302 and #308, lost their assertion and stack that way. Copy it into the scratchpad the moment the suite goes red.

**`tools/seed.sh` fails with one word after an instrumented run, and it is never the fixture.** `connectedDebugAndroidTest` uninstalls the app when it finishes, so the seed has nothing to restore into and prints `Failed`. **It bit three times in one night** and each time the next thing was `walk.sh see` dumping the owner's home screen with his real calendar on it. **Reinstall before seeding, always**, and if a seed says `Failed`, check `pm list packages | grep kamsiob` before checking anything else.

**A local check passing is not the same as the check passing.** The fixture check written for #233 passed here and failed in continuous integration on the very next push, on 1658 rows, because `Fixture.ms` resolved its instants in the machine's own timezone while every row it wrote claimed New York. **This laptop is in New York and CI is in UTC**, so the defect was invisible locally by coincidence of geography. **Anything that computes a timestamp should be run under `TZ=UTC` before it is pushed**, which is one word in front of the command.

**Setting the app locale does not change the words until the app restarts.** `cmd locale set-app-locales ... ar` flips the layout to right to left immediately and **leaves the copy in English**, because `Strings.load` is `remember(context)` and Compose keeps the same context object across a configuration change. A screenshot taken then is an RTL layout full of English, which reads as a translation defect and is not one. **Force stop and relaunch after setting the locale**, every time: `adb shell am force-stop com.kamsiob.healthtrail`. #326.

**`screenshot.sh` could not see the clipboard overlay, and it got into two captures.** The system clipboard preview shows what was copied, KDE Connect syncs the laptop's clipboard onto this phone constantly, and on 2026-08-08 two images came back with the owner's shell prompt across the bottom. It never takes focus, so the focus check passed, and it is not a heads-up, a toast or a popup, so the overlay pattern missed it. The pattern catches `clipboard` now. **This is the second time an image was caught by looking at it rather than by a control**, which is why rule 21 says look.

**A locked phone fails every instrumented class, and the message blames the test.** It says `IllegalStateException: No compose hierarchies found in the app`, which reads as "the class you just wrote is set up wrong", and it is not: **a secure keyguard stops the test activity reaching the foreground**, so `setContent` never gets a window. Proved on 2026-08-08 by running `TodayScreenTest`, untouched and green two days earlier, and watching all twelve fail the same way. **Read the phone before the code**: `adb shell dumpsys window | grep isKeyguardShowing`. A wake and a swipe do not clear a secure lock and there is no way past it from here. #316.

**A whole class failing identically is the environment, not the product.** On 2026-08-06 all six `BackJourneyTest` tests failed with `RootViewWithoutFocusException` and the phone's notification shade was open, holding focus over Reddit. Six named back-journey tests going red at once looks like a real back-stack regression and cost eight minutes. **Read the exception before reading the code**: `has-window-focus=false` and a `Sys2040` in `mCurrentFocus` mean nothing was ever driven. Collapse the shade, press home, rerun the class alone. **#316.**

**A component that mirrors is not the same as a component whose text mirrors.** `RoadStrip` lays its waypoints out, so they flip in Arabic; its fallback line concatenated the stage names by hand, so they did not, and the road ran one way while its own names ran the other. **Anything joined with a separator by hand is a candidate**: `Bidi.join` exists and its default separator is already ` · `. This is the same family as the nested isolates, from the opposite direction. #226's worklist is where the rest of these live.

**A screen that clears its descendants' semantics cannot be tested through its text, and will not tell you so.** `RoadStrip` speaks as one node by design, so `onNodeWithText` finds nothing inside it and `walk.sh see` prints nothing from it. A test written against the rendered text fails looking exactly like the defect it was meant to catch. **Put the logic in a named function and hold that instead.**

**A surface with no test of its own loses whatever the screen it replaced was carrying.** `TodayFieldScreen` replaced the previous Today on every seeded notebook, which is every real one, and arrived with no way into search at all: the old screen had it in the header, nothing carried it across, and the new surface had **no test file of any kind**, so nothing said so for as long as it has existed. **Check what the superseded screen did that the new one does not**, the same way `docs/REMOVAL-LEDGER.md` is read against the app for #314, and write the test at the moment the surface lands rather than after.

**A parser hides a duplicate from every check that reads the parsed thing.** All four locale catalogs carried `project.step.handled_by` twice, and `check_i18n.py` compares dictionaries, so the second copy was gone before the first comparison ran. The four agreed, the placeholders matched, the plurals were complete, and the files were wrong. It happened to be harmless because both copies held the same words, which is luck rather than a safeguard. `check_i18n.py` reads the raw text for this now and was proved by writing a real duplicate into `en.json` and watching it fail. **Anything checked only through a parser has this blind spot.**

**A control that came off a superseded screen does not come back just because its call survived.** `saveProjectAsTemplate` and `setProjectStatus`'s waiting-on argument both kept their repository call and their `NotebookShell` state through the supersession, and nothing set either for a week: the state is read, the effect is written, the compiler is happy, and the control does not exist. **Nothing catches this shape.** It was found by reading `docs/REMOVAL-LEDGER.md` against the app, which is what the ledger is for. #314. **Check the ledger's other rows the same way rather than trusting what they claim came back.**

**`connectedDebugAndroidTest` uninstalls the app when it finishes.** `walk.sh` then dumps whatever is on the phone, which is the owner's home screen with his real calendar and contacts on it. **Reinstall and check the app is focused before walking**, and never screenshot without it: `screenshot.sh` refuses, but `walk.sh see` does not.

**Distrust a negative result from a tool that cannot say what it did not examine.** This has now happened five times in one night and twice since. A "not found" from `walk.sh` usually means the thing is below the fold or the label differs in that locale.

---

## 8. Running the work

**Never route around a check to make progress, and never delete or weaken a test to make a build pass.**

    python3 tools/checks/run_all.py                    # 17 content and contract checks, seconds
    tools/verify.sh                                    # the honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest   # 297 tests, about six minutes

**Run `tools/verify.sh`, not the checks you happen to remember.** CI once failed on a lint error in code that had been walked on the device and passed every content check and 185 instrumented tests.

**Run the instrumented suite after any run that changes a screen**, and **do not touch the phone while it runs**. A capture attempted mid-run on 2026-08-04 force-stopped the app under the suite and produced 79 tests with one bogus failure. A run driven from two places at once tells you nothing.

**`connectedAndroidTest` uninstalls the app and takes the notebook with it.** Reinstall and reseed afterward:

    adb install -r android/app/build/outputs/apk/debug/app-debug.apk
    tools/seed.sh                       # month six, the notebook most walks use
    tools/seed.sh year5 5 walk-year-five

**Commit and push after every working increment**, per rule 7. An increment ends when `origin/main` has it. **An issue closes only on device verification**: both themes, maximum font scale, right to left, and every state in `DESIGN.md` 13.3 including the empty one.

**When a screen is undrawn, rule 12 wants three things at the moment it is built**: a `needs-design-review` issue with a real device screenshot, a row in `DESIGN.md` section 14, and a line in this file.

### 8.1 The running list of screens composed rather than drawn

Every one of these was built from the existing components, logged in all three places at the moment it was built, and is waiting on the owner's eye. **None of them is a defect**; the list exists so that no composed screen is mistaken for a designed one.

**All sixteen of them, oldest first.** Checked against the board on 2026-08-06 at the end of the overnight run with `gh issue list --label needs-design-review` rather than remembered, because a list that is only partly a list is the defect this section exists to prevent.

| Screen | Issue |
|---|---|
| Care threads, the list | #223 |
| Standing instructions, the list | #225 |
| One appointment and its prep sheet | #232 |
| The milestone arc | #235 |
| Month review | #236 |
| Starting a project | #239 |
| The template library | #240 |
| Change of situation | #241 |
| The situation picker, converted | #242 |
| The long road project home | #304 |
| Starting a project, what the template sets up | #309 |
| The starting steps, changed | #310 |
| The road, changed | #311 |
| The date kinds, changed | #312 |
| The usual papers, changed | #313 |
| Keeping a project as a template, and who it is waiting on | #317 |

**Seven of these are the Projects surface**, #304, #309 through #313, and #317, and they are the ones that arrived in a single run. The other nine have been waiting longer.

---

## 9. Blocked, and it does not stop the work

**No machinery is blocked.** B5, the destructive command guard, is **resolved as of 2026-08-07**: it is installed in `.claude/settings.json`, it is live, and it refused a real removal command aimed at the app package. `DECISIONS.md` B5 has the account. **One defect came with it, #323**: it matches prose that merely mentions a blocked verb, so writing certain sentences into this file is refused. That is not a reason to weaken it.

**What is blocked is decisions, and each says on its own issue exactly what has to be chosen:**

- **#182 and #199** need a schema decision. There is no test, no round and no result in `contract/schema.sql`, so there is nothing to build against. Skip them.
- **#303** needs somewhere for a reference number to live. `ReferenceLine` has still never rendered with real data.
- **#238** needs a decision on whether a milestone may point at a measure at all, which comes close to interpreting a measurement.
- **#319 and #320** need a direction for the `app_meta` problem: text already stored unnormalized, and a restored phone writing under the source phone's identity.

**None of it stops the work.** Everything in milestones 1 through 6 is buildable without any of these.

## 10. The phone

- **Pixel 10 Pro XL, serial `57241FDCQ0000H`, over USB. The only test device.**
- **No emulator.** Dropped from this project. Do not launch one, do not create an AVD, do not treat its absence as a blocker. D21, D23, B4.
- **This is the owner's daily driver.** Everything below follows from that.

**`connectedAndroidTest` uninstalls the app and takes the notebook with it.** That is also the sanctioned way back to first-run state; `adb uninstall` is on the blocklist. D50. Reinstall and reseed afterward.

**Read the theme, never assume it.** `adb shell cmd uimode night`. `tools/screenshot.sh` reads it from the device and names the file accordingly, so do not pass a theme argument. D31.

**Run the app in one language without touching the phone's own settings:**

    adb shell cmd locale set-app-locales com.kamsiob.healthtrail --locales ar
    adb shell cmd locale set-app-locales com.kamsiob.healthtrail --locales ""

**Use `zh-Hans` for Chinese, never a bare `zh`.** A bare tag has no script and yields English rather than an error. D52.

**Rule 19's exception, and its condition.** Font scale, animation duration and TalkBack may be changed **provided the prior value is recorded first and restored exactly.** On this phone, before anything:

    adb shell settings get system font_scale                     # expect 1.0
    adb shell settings get global animator_duration_scale        # expect null, so delete rather than set to 1.0
    adb shell settings get global heads_up_notifications_enabled # expect 1
    adb shell settings get secure enabled_accessibility_services # expect the KDE Connect string, NOT TalkBack
    adb shell cmd locale get-app-locales com.kamsiob.healthtrail # expect []

**If a run ends with TalkBack still on:**

    adb shell settings put secure enabled_accessibility_services org.kde.kdeconnect_tp/org.kde.kdeconnect.plugins.mousereceiver.MouseReceiverService
    adb shell settings put secure accessibility_enabled 0

**Look at every screenshot before committing it.** `tools/screenshot.sh` refuses to capture unless the app is focused, suppresses heads-up notifications, and crops the status bar, and it is still not the last control. A heads-up notification once put the owner's phone number and a contact photo into a capture. D53, D72. **The share sheet and the calendar app show real contacts: do not screenshot either.**

**The three guards, so nobody re-derives them.** Guard 1 was inert from the day it was written until 2026-08-01 because its hook command was unquoted and this path contains spaces; fixed. Guard 2, the pre-compaction state save, **has never fired and is unproven** and cannot be triggered deliberately, so treat it as absent and keep this file current by hand. Guard 3, the retry cap, is a command line tool nothing calls. D29, D49.

---

## 11. This environment, so a fresh session does not rediscover it

**An edit that replaces text must assert it matched.** Nine decision entries were once written and none reached `DECISIONS.md`: the anchor they targeted had been consumed by an earlier edit, so every one matched nothing and reported success. A silent no-op is worse than an error, because the work continues on top of a record that is not there.

**After editing `CLAUDE.md`, read the rules back from disk.** The copy in a session's context is the one from session start, and edits made during the session never reach it. A rule added this session is one a compaction can lose. Same shape as D49: configuration read once at startup, edited later, believed to be live.

**The shell does not carry state between tool calls.** Every command starts fresh.

- **`ANDROID_HOME` is not set.** The SDK is at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and **does not exist in a fresh clone**. Recreate it: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH.** It is at `/home/Kamsiob/Android/Sdk/platform-tools/adb`. `tools/screenshot.sh` resolves it itself.
- **The working directory contains a space and two leading dashes.** Quote every path.

**Walking the app.** `tools/walk.sh see` prints every piece of text on screen, in order, by asking the semantics tree, which is what a screen reader walks. `tools/walk.sh tap "Medications"` taps the first node matching text or content description and prints what it tapped, so a walk that goes wrong says so. `tools/walk.sh fields` lists the editable fields with tap coordinates. A dump costs about 2.7 seconds on a five year notebook, which is fine for walking and useless for timing, per #142.

**`walk.sh tap` matches on the label, so it fails in Arabic** when handed an English string, and a bottom-navigation tab is easier to hit by coordinate than by label. **A NOT FOUND usually means below the fold or a different locale**, not absent. Swiping at a y inside the keyboard does nothing, which has read as "the button is unreachable" more than once.

**Seeding a notebook.** `tools/seed.sh` defaults to month six, the notebook most walks use. `tools/seed.sh year5 5 walk-year-five` for the long record. It generates, packs, pushes, clears the app and walks the app's own restore screen, deliberately, per D61: a fixture that arrives any other way has never been through the importer. It checks the screen says "Restored." before saying so itself. **If it reports the file is not in the picker, a notification shade or another app has the focus**; press back and home, then run it again.

**Which fixture shows which state, without generating six and walking them.** `python3 tools/checks/report_today_rungs.py` builds all six horizons and prints, per Today card, which rung of `DESIGN.md` 21.4 it lands on. It is a report and not a gate, and its queries are a second copy of `Repository.todayAnswers`, so **if a rung it promises is not on the screen, believe the screen**. Two things it found on the day it was written are at **#325**. **The empty Today is not reachable from any seed**: it comes from clearing the app and walking onboarding, which is how `today-fresh-dark.png` was taken.

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. **It appends the theme, so passing a name ending in `-dark` yields `-dark-dark.png`**; rename after. It refuses to capture unless the app is focused, suppresses heads-up notifications, and crops the status bar at a height read from `dumpsys window`. D31, D53, D72. **Look at every image before committing it.** The script is a control and it is not the last one.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**A trap in the Compose test API, found the hard way.** `performScrollToNode` walks a lazy list a viewport at a time and gives up when it thinks it can go no further. It got that wrong for the Arabic catalog, stopped two rows short, and reported the rows as absent when they were only further down. **Scroll by the list's own item key instead**, with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` rather than on a surface around it: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Continuous integration.** The workflow triggers on `push` to main, on `pull_request`, and on `workflow_dispatch`. Pull request events stopped firing part way through 2026-07-31 and **are firing again as of 2026-08-01**. If they stop again: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>`. **Do not read an absence of checks on a pull request as a passing build.**

**Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools`, or `contract` without touching this file. It caught pull request #49. "README.md describes the screens that exist" fails any pull request that adds or removes a file under `ui/screens/` without touching `README.md`, which exists because the front page claimed the app had one screen for a week after it had nine. "Every screenshot the README points at exists" catches a rename. Rewrite the documents in the same commit as the work, not afterward.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**A release by somebody else used to turn CI red on an unchanged tree, and no longer does.** `warningsAsErrors` made lint's two version currency checks build breaking, so the moment Gradle or any dependency published, `lintDebug` failed naming a file nobody had edited. It happened twice inside an hour on 2026-08-08: Gradle 9.7.0, then Bouncy Castle 1.85.2. **`NewerVersionAvailable` and `AndroidGradlePluginVersion` are disabled with their reason**, and **staying current is Dependabot's**, which now watches `/android` and opens a pull request instead. D121. **Nothing else in lint was weakened**, and `warningsAsErrors` still holds.

**Everything else:** Gradle 9.7.0, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

**Run `tools/verify.sh`, not the checks you happen to remember.** Continuous integration failed on 2026-08-02 for a lint error, `Uri.parse` where the KTX `String.toUri` was wanted, in code that had been walked on the device and had passed all ten content checks and 185 instrumented tests. **`verify.sh` runs `lintDebug` and would have caught it.** Running `run_all.py` plus the instrumented suite by hand feels like verifying and skips whatever is not in that habit.

**Verification.** `tools/verify.sh` is the honest runner: it captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. `python3 tools/checks/run_all.py` runs the 17 content and contract checks alone. **Never chain a commit on a grep of output.**

---

## 12. Where everything else is

| Question | File |
|---|---|
| What to do next | This file, section 1. Then the board, project 3, in `ORDER OF WORK` order |
| Why something is the way it is | `DECISIONS.md`, D1 through D112 |
| What it should look like | `DESIGN.md`. **Three grids**: `reference/screen-grid.html` generally, `projects-grid.html` and `today-grid.html` for those two surfaces. Section 14 is the undrawn-screen map; 20 and 21 are the two new surfaces |
| What the data may do | `contract/DATA-CONTRACT.md`, and `contract/EXPORT-FORMAT.md` for the archive |
| What the app is for | `MASTER_SPEC.md` |
| How it gets tested | `TESTING-PERSONAS.md` |
| How a long unattended run stays safe | `RUN-SAFETY.md` |
| Delegation | `AGENTS.md`. Subagents never write anything |
| How something came to be, and what proved it | `docs/RUN-LOG.md`. **History only. Never read it to orient.** |

**Precedence when two of them disagree:** verified code, then this file, then `DECISIONS.md`, then the data contract for data questions, then `DESIGN.md` for visual questions, then `RUN-SAFETY.md` and `AGENTS.md`, then `PROJECT-DELTAS.md`, then `MASTER_SPEC.md`, then the template.

---

## 13. Uncommitted work

**None.** Verified with `git status --porcelain` returning nothing and the push confirmed against `origin/main`, rather than assumed.

### The phone was locked for the 2026-08-08 overnight run

**A secure keyguard, so nothing on the device could be done**, and this is not a defect in anything. `adb` reported `isKeyguardShowing=true` with `mWakefulness=Awake`, and a wake plus a swipe does not get past a secure lock. **Instrumented tests cannot run through it either**: the activity never takes focus, which is #316's `RootViewWithoutFocusException` arriving from a different direction.

**What that means for anything built while it holds.** Code, checks, lint, catalogs and documents all proceed. **Nothing closes**, because an issue closes on device verification and only on that. #292 and #270 are built, tested and pushed, and both are waiting on the phone for their four gate captures and the 16.4 checklist.

**Check it before assuming the run is device-blocked:** `adb shell dumpsys window | grep isKeyguardShowing`.

**Fourteen commits landed on 2026-08-06 between 04:25 and 09:46 UTC**, `434db59` through `86339ae`. **Continuous integration is green on the tip.** Two commits in the middle, `17b4e43` and `a73f33d`, are red in the history and were fixed by `86339ae`: the fixture check added for #233 found a defect that only reproduces in UTC, so it passed here and failed there. That is the check working, and it is left in the history rather than rewritten.
