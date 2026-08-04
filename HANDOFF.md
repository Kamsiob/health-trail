# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-04.

---

## 1. Where to start

Everything below is verified rather than asserted, as of 2026-08-04:

- The working tree is clean and everything is on `origin/main`. **Check it rather than trusting this line**: `git status --porcelain` and `git log --oneline -5`.
- **16 repository checks pass** (`python3 tools/checks/run_all.py`).
- **297 instrumented tests pass**, last full run 2026-08-04 against `c99bff5`. Nothing since has been run on the device, which is what section 2 is about.
- The phone was left with the month six fixture, font scale 1.0, night mode off, and the per-app locale at the system default. **It has been unplugged since**, so confirm it is attached before planning any device work: `adb devices`.

**Take these in this order.**

1. **Finish #200, the milestone arc and month review.** It is part built, part unverified, and section 2 says exactly which is which. **Start there, not with a fresh issue.**
2. **The rest of step 4**, #201 through #208, in number order, **except take #208 last**. Each was scoped on 2026-08-04 and the two with gaps carry a comment saying so:
   - **#202** is half a conversion and half a build. "Change of situation" does not exist anywhere, and whoever takes it has to decide first whether changing the situation is a chapter boundary in the data or only in the words.
   - **#208**, the family update draft, does not exist at all and is Phase 5 work sitting in a step 4 list. Everything it needs is built: `Readable.kt` composes from real rows and `Share.kt` hands a document to the system sheet. Read `PrepScreen.kt` first; it is the same shape.
   - #201, #203, #204, #205, #206 and #207 are conversions of screens that exist.
3. **The isolate audit, #226.** It has a generated worklist and needs Arabic on the device.

**Two things in step 4 are blocked and are not yours to unblock.** #199 (one test round and one test's history) and #182 (the tests section) both need a schema decision from the owner: there is no test, no round and no result anywhere in `contract/schema.sql`, so there is nothing to convert and nothing to build against. Both are labeled `blocked` and say what has to be decided. **Skip them.**

**THE ARCHIVE runs on its own track and must not be scheduled behind the screens.** #209 through #215, with #9 as the parent. #209, #213, #214 and #215 are done. What is next there is **#211**, the importer's remaining 8.3 rules, which three other things wait on.

---

## 2. #200 is built end to end and needs its sweep

**Both halves exist and both have been used on the phone.** What is left is verification, not construction.

**The arc.** The milestone reader and writer, `MilestonesScreen`, `AddMilestoneScreen`, the door from the chapters list, and the shell wiring. All verified on the device including the parts a previous session could only compile: **marking a milestone by hand and choosing a chapter makes the chapter door appear on its row, and the chapter's own "What was worth marking" fold shows it back.** Rule 18 holds in both directions, seen rather than asserted. `docs/screenshots/milestones-v4-light.png`.

**The fixture still cannot exercise the chapter link on its own**: generated milestones carry no `chapter_id`, so a walk that needs the door has to mark one by hand first. That is #235.

**Month review**, `MonthReviewScreen`, reached from the trail's own month heading, which now carries a chevron. Hero is the month's milestones and nothing else, then where they were, appointments, what went wrong, what was answered, paperwork, and a fold holding everything written down. One filled action, which shares the month as a document through `Readable.monthReview`. `docs/screenshots/review-light.png`.

**Two defects were found by looking at it and are fixed:** a place that began and ended in one month listed its name twice, and an incident reported and answered in one month listed twice under two headings. Both now read as one row. The gold total band was built and removed the same day, for the reason in `DESIGN.md` section 14.

**#200 still needs:** the sweep at both themes, font scale 2.0 and Arabic; the empty state seen; the instrumented suite; and a `needs-design-review` issue per rule 12 for each of the two screens, since neither is drawn.

**The device holds one extra milestone**, "Sat up for the whole visit", written by hand to exercise the chapter link. `tools/seed.sh` clears it.

---

## 3. What is built

**Design direction v4 is adopted and most of the app is in it.** `reference/screen-grid.html` is the v4 grid. `DESIGN.md` was rewritten rather than patched.

- **Step 1, the foundation: complete.** Every token in both themes, the type scale with all three faces verified per locale, the geometry, and all sixteen components. #149 through #168 closed.
- **Step 2, the four destinations: complete.** #169 through #172 closed.
- **Step 3, the section screens: complete but for #182**, which is blocked. Fourteen closed on device verification.
- **Step 4, the detail screens: ten of twenty closed.** #189 through #198. #199 is blocked; #200 is in flight; #201 through #208 are untouched.

**#192, one medication, closed with its remainder split out rather than left vague.** Its questions are built and the fixture never exercises them, **#229**; its incidents cannot be expressed because the schema has no link from an incident to a medication, **#230**, which is the owner's call.

**THE ARCHIVE is largely built and proved on real hardware**, not asserted: a two-layer container at format version 3, a readable copy of 61 pages, a standalone decryptor at `tools/decrypt/` tested in CI, and the format published byte for byte in `contract/EXPORT-FORMAT.md`. `docs/RUN-LOG.md` has the account and what each piece was proved with.

---

## 4. What keeps going wrong, so it stops

**These are patterns, not history. Every one of them has now happened more than once.**

**A row whose only behavior is edit is a screen nobody built.** One bill and one document both opened the form that edits them, which is the app answering "tell me about this" with "change this". Both also carried schema links nothing read. **Check the remaining detail screens for the same shape.**

**Check what is at the top of a detail screen.** One project opened with five status chips and an empty text field taking a third of the fold, above four identical step cards, one of which was the answer. The controls that describe a thing are not the thing.

**Check what is carrying the accent.** One incident had its filled action on marking it answered, which somebody does once at the end, rather than on adding what happened next, which is why the screen gets opened.

**State the answer, then fold the volume.** One chapter had 293 entries on screen at one weight; one care thread had 174.

**Not everything is a card.** The prep sheet's questions were eight cards on a spine, each repeating its role in mono. Rule 22: a question is one sentence, which is a row. Where a wall of something already has a solved composition elsewhere in the app, use that one rather than inventing a second answer.

**The sweeps are where the defects are, and almost none is visible in English at font scale 1.0.** Text the person typed gets rearranged in Arabic; `Bidi.isolate` and `Bidi.join` are the fix and `DESIGN.md` section 15 carries the rule. `report_bidi_isolation.py` generates the remaining worklist, 76 candidates, tracked at **#226**.

**A defect can live entirely inside somebody else's app.** The calendar hand-off put a November 27 appointment on the 26th, and the screen said November 27 the whole time. It cost three attempts and none of the causes was time zones.

**Distrust a negative result from a tool that cannot say what it did not examine.** This has now happened five times in one night and twice since. A "not found" from `walk.sh` usually means the thing is below the fold or the label differs in that locale.

---

## 5. Running the work

**Never route around a check to make progress, and never delete or weaken a test to make a build pass.**

    python3 tools/checks/run_all.py                    # 16 content and contract checks, seconds
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

---

## 6. Blocked, and it does not stop the work

**One thing is blocked: B5.** The destructive command guard needs installing from user settings and **only the owner can do it**, because Claude Code correctly refuses to let a session edit the hooks that constrain it. D64 has the account and B5 in `DECISIONS.md` is written as steps he can act on.

**It does not stop anything.** What has protected this repository through five long unattended runs is rule 6 followed by hand, plus Claude Code's own classifier. **Do not spend time re-probing the guard**; the answer is known until B5 lands.

**Two schema decisions are also the owner's**, and they block #182 and #199 only: the tests section has no table, no query and no screen. Everything else on the board is buildable.

---

## 7. The phone

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

## 8. This environment, so a fresh session does not rediscover it

**An edit that replaces text must assert it matched.** Nine decision entries were once written and none reached `DECISIONS.md`: the anchor they targeted had been consumed by an earlier edit, so every one matched nothing and reported success. A silent no-op is worse than an error, because the work continues on top of a record that is not there.

**After editing `CLAUDE.md`, read the rules back from disk.** The copy in a session's context is the one from session start, and edits made during the session never reach it. A rule added this session is one a compaction can lose. Same shape as D49: configuration read once at startup, edited later, believed to be live.

**The shell does not carry state between tool calls.** Every command starts fresh.

- **`ANDROID_HOME` is not set.** The SDK is at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and **does not exist in a fresh clone**. Recreate it: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH.** It is at `/home/Kamsiob/Android/Sdk/platform-tools/adb`. `tools/screenshot.sh` resolves it itself.
- **The working directory contains a space and two leading dashes.** Quote every path.

**Walking the app.** `tools/walk.sh see` prints every piece of text on screen, in order, by asking the semantics tree, which is what a screen reader walks. `tools/walk.sh tap "Medications"` taps the first node matching text or content description and prints what it tapped, so a walk that goes wrong says so. `tools/walk.sh fields` lists the editable fields with tap coordinates. A dump costs about 2.7 seconds on a five year notebook, which is fine for walking and useless for timing, per #142.

**`walk.sh tap` matches on the label, so it fails in Arabic** when handed an English string, and a bottom-navigation tab is easier to hit by coordinate than by label. **A NOT FOUND usually means below the fold or a different locale**, not absent. Swiping at a y inside the keyboard does nothing, which has read as "the button is unreachable" more than once.

**Seeding a notebook.** `tools/seed.sh` defaults to month six, the notebook most walks use. `tools/seed.sh year5 5 walk-year-five` for the long record. It generates, packs, pushes, clears the app and walks the app's own restore screen, deliberately, per D61: a fixture that arrives any other way has never been through the importer. It checks the screen says "Restored." before saying so itself. **If it reports the file is not in the picker, a notification shade or another app has the focus**; press back and home, then run it again.

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. **It appends the theme, so passing a name ending in `-dark` yields `-dark-dark.png`**; rename after. It refuses to capture unless the app is focused, suppresses heads-up notifications, and crops the status bar at a height read from `dumpsys window`. D31, D53, D72. **Look at every image before committing it.** The script is a control and it is not the last one.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**A trap in the Compose test API, found the hard way.** `performScrollToNode` walks a lazy list a viewport at a time and gives up when it thinks it can go no further. It got that wrong for the Arabic catalog, stopped two rows short, and reported the rows as absent when they were only further down. **Scroll by the list's own item key instead**, with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` rather than on a surface around it: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Continuous integration.** The workflow triggers on `push` to main, on `pull_request`, and on `workflow_dispatch`. Pull request events stopped firing part way through 2026-07-31 and **are firing again as of 2026-08-01**. If they stop again: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>`. **Do not read an absence of checks on a pull request as a passing build.**

**Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools`, or `contract` without touching this file. It caught pull request #49. "README.md describes the screens that exist" fails any pull request that adds or removes a file under `ui/screens/` without touching `README.md`, which exists because the front page claimed the app had one screen for a week after it had nine. "Every screenshot the README points at exists" catches a rename. Rewrite the documents in the same commit as the work, not afterward.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**Everything else:** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

**Run `tools/verify.sh`, not the checks you happen to remember.** Continuous integration failed on 2026-08-02 for a lint error, `Uri.parse` where the KTX `String.toUri` was wanted, in code that had been walked on the device and had passed all ten content checks and 185 instrumented tests. **`verify.sh` runs `lintDebug` and would have caught it.** Running `run_all.py` plus the instrumented suite by hand feels like verifying and skips whatever is not in that habit.

**Verification.** `tools/verify.sh` is the honest runner: it captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. `python3 tools/checks/run_all.py` runs the 16 content and contract checks alone. **Never chain a commit on a grep of output.**

---

## 9. Where everything else is

| Question | File |
|---|---|
| What to do next | This file, section 1. Then the board, project 3, in `ORDER OF WORK` order |
| Why something is the way it is | `DECISIONS.md`, D1 through D102 |
| What it should look like | `DESIGN.md`, plus `reference/screen-grid.html`. Section 14 is the undrawn-screen map |
| What the data may do | `contract/DATA-CONTRACT.md`, and `contract/EXPORT-FORMAT.md` for the archive |
| What the app is for | `MASTER_SPEC.md` |
| How it gets tested | `TESTING-PERSONAS.md` |
| How a long unattended run stays safe | `RUN-SAFETY.md` |
| Delegation | `AGENTS.md`. Subagents never write anything |
| How something came to be, and what proved it | `docs/RUN-LOG.md`. **History only. Never read it to orient.** |

**Precedence when two of them disagree:** verified code, then this file, then `DECISIONS.md`, then the data contract for data questions, then `DESIGN.md` for visual questions, then `RUN-SAFETY.md` and `AGENTS.md`, then `PROJECT-DELTAS.md`, then `MASTER_SPEC.md`, then the template.

---

## 10. Uncommitted work

**None.** Verified with `git status --porcelain` returning nothing and the push confirmed against `origin/main`, rather than assumed.
