# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-04.

---

## 1. Where to start

Everything below is verified rather than asserted, as of 2026-08-04:

- The working tree is clean and everything is on `origin/main`. **Check it rather than trusting this line**: `git status --porcelain` and `git log --oneline -5`.
- **17 repository checks pass** (`python3 tools/checks/run_all.py`).
- **Continuous integration is green on `main`.** It had been **red for three commits**, from `050ac27` to `b40e6ac`, and nothing said so: the last green before that was `c99bff5`. **Check it after every push**, `gh run list --branch main --limit 3`, because the tree being clean and the checks passing tell you nothing about it.
- **332 instrumented tests pass**, last full run 2026-08-04 after #202 landed.
- The phone was left with the month six fixture, font scale 1.0, night mode off, and the per-app locale at the system default. **It has been unplugged since**, so confirm it is attached before planning any device work: `adb devices`.

**Take these in this order.**

1. **A new design direction arrived on 2026-08-04 and step zero is done.** Section 2 is the whole of it. **#262, the data contract work, is underway and its schema half has landed.** Section 2.1 says exactly where it stands and what is left of it.
2. **#200, #201 and #202 are done and closed**, and sections 3 to 5 say what came out of them.
3. **The rest of step 4 of the v4 conversion is untouched**: #203 through #208, in number order, **except take #208 last**. These are not affected by the new grids.
   - **#208**, the family update draft, does not exist at all and is Phase 5 work sitting in a step 4 list. Everything it needs is built: `Readable.kt` composes from real rows and `Share.kt` hands a document to the system sheet. Read `PrepScreen.kt` first; it is the same shape. **Take it last.**
   - #203, #204, #205, #206 and #207 are conversions of screens that exist.
4. **The isolate audit, #226.** It has a generated worklist and needs Arabic on the device. **#202 found one of its cases by accident**, so the worklist is real.

**Two things in step 4 are blocked and are not yours to unblock.** #199 (one test round and one test's history) and #182 (the tests section) both need a schema decision from the owner: there is no test, no round and no result anywhere in `contract/schema.sql`, so there is nothing to convert and nothing to build against. Both are labeled `blocked` and say what has to be decided. **Skip them.**

**THE ARCHIVE runs on its own track and must not be scheduled behind the screens.** #209 through #215, with #9 as the parent. #209, #213, #214 and #215 are done. What is next there is **#211**, the importer's remaining 8.3 rules, which three other things wait on.

---

## 2. The Today and Projects grids are adopted, and step zero is done

**2026-08-04, owner's instruction.** Two new design references arrived and are now `reference/projects-grid.html` and `reference/today-grid.html`. **They extend v4; they do not replace it.** Everything in `DESIGN.md` sections 1 through 19 still governs both. Where the v4 grid drew Today or Projects, those drawings are superseded; every other screen in it is untouched. D106.

**Step zero is complete and was documentation only. No application code was written against either grid.**

- **`DESIGN.md` gained sections 20 through 23**, encoding both grids in the repository's own words: Projects, Today, the global voice rule, and the two new audits. **Read those rather than the HTML.** The three grid files are named side by side at the top of `DESIGN.md`, each saying what it governs.
- **`DECISIONS.md` D106 through D112** record the adoption, the eleven inventory additions, the handler-tag ruling, the voice rule, the data contract amendment, the seven provisional resolutions, and the freeze rule.
- **`contract/DATA-CONTRACT.md` gained 8.7**: the Today layout, project templates, stage assignments, standing entries, recorded dates with their sources, and steps with handler tags are **record**, not preference. They travel in the archive and restore on import.
- **`MASTER_SPEC.md` 4.3 and 4.5 were rewritten**, and phases 1 and 4 corrected.
- **`docs/REMOVAL-LEDGER.md` is new.** Superseded Today and Projects code freezes rather than being deleted: never called, extended, fixed, or translated. **Its ledger is empty because nothing has been superseded in fact yet.**
- **`check_copy.py` gained the battle-voice rule**, D109.

**Fifty-nine issues are open for the work**, #243 through #301. The two parents are **#243 Today** and **#244 Projects**. Under them: 28 screen issues, 17 card issues each carrying its full states ladder as acceptance criteria, 11 component issues, the data contract work at **#262**, and the two provisional template hands at **#273**.

**Take #262 first.** Everything on both surfaces stores something, and building screens against a contract that does not exist yet is the order that produces a second migration.

### 2.1 #262, the schema half: landed

**The database now holds what a person arranges.** `contract/schema.sql` is at **version 2** and `HealthTrailDatabase.SCHEMA_VERSION` moved with it.

**Six new tables**, each with its live view, both change log triggers, and its indexes: `today_card`, `project_stage`, `project_standing`, `project_date`, `project_date_kind`, `project_paper`. **Four new columns**: `project.lead`, `project.current_stage_id`, `project_step.cluster`, `project_step.handler_label`.

**Three things in it are worth knowing before touching either surface.**

- **The lead slot is singular in the database, not only on the screen.** A partial unique index, `ux_today_card_lead`, refuses a second lead per subject. Zero is the application's to prevent, because a database cannot require a row to exist. It is the schema's first `CREATE UNIQUE INDEX`.
- **`today_card.source_table` and `source_id` are deliberately not foreign keys.** 8.7 requires that a card whose source is gone is kept rather than dropped, and a foreign key would make import the thing that quietly edited somebody's desk.
- **No column marks a project's important date.** The screen leads with the soonest date that has not passed, and the most recent one when they all have. D113 says why, and what that gives up.

**The migration is real and is proved against a version 1 database**, not against today's. `Migrations.steps` has its first entry; `Step.apply` now receives the contract schema text alongside the database, so an additive step replays `contract/schema.sql` rather than carrying a second copy of six table definitions that would drift. `MigrationTest` builds a hand-written version 1 database and asserts the tables arrive, the columns arrive, and the project and its step survive with nothing invented in the new columns.

**All 17 checks pass, 167 unit tests pass, lint is clean.** The instrumented suite has **not** been run against this yet.

**What is left of #262, in order:**

1. **`Repository` reads and writes for the six tables.** Nothing in the app can create a card or a stage yet.
2. **The fixture writers**, so every rung of every card's states ladder can actually be produced on the device. `check_fixtures.py` holds ids to the real catalog, so this is where the states ladder audit becomes possible at all.
3. **The archive round trip proved on the device**, per 8.5. Export and readable rendering are schema-driven and pick the tables up automatically; that is the reason to trust them, not the reason to skip the test.
4. **The built-in project templates gain the five defaults.** `templates/data/projects.json` has `steps` and `roles` and needs `stages`, `lead`, `papers`, and `date_kinds` on all sixteen. This is content work in the app's voice.

**Two checkers were fixed rather than worked around while doing this**, D114, and **#216 is closed by it**.

**Seven things are resolved provisionally, to the drawn default, and are meant to be revisited in one sitting after the owner tests on the phone.** D111 lists them together. **Two template default hands, hospital and rehab, were drafted rather than deferred and are not final.**

---

## 3. What #202 landed

**Both halves are built, swept, tested and closed.** Both themes, font scale 2.0, and Arabic, on the phone. Reviews at **#241** and **#242**, and `DESIGN.md` section 14 carries both rows.

**The picker is converted**: the setting each group leads with keeps its card and its burden line, and the rest are dense rows in a grouped surface. Fourteen cards was three and a half screenfuls on the first screen after the disclaimer.

**Change of situation is new and it had no door at all.** The picker ran once during setup and was then unreachable forever, so a family whose care moved could not tell the app and could not even see which setting they had. It is now a destination in More. The screen states the boundary plainly and offers a chapter, and **the boundary is made rather than only stated**: `moveToChapter` ends the open chapter today and starts the new one today, because starting a second without ending the first left two places somebody was in at once.

**Two things it said that were not true, both found by looking at it:** "Right now" showed the setting the person had just picked, before anything was written, and the chapter field carried a mono header saying the same three words as its own label.

**The picker's rows lost their grouped surface, and that was the component's own rule rather than a compromise.** Section 7: not around a list long enough to scroll, where the rows should be full bleed so the scroll is not a slab moving under a window. It also restored **one lazy item per setting**, which is what the picker's test needs to reach all fourteen by key, and batching a group into one item had broken exactly that. The trap is in section 10 of this file and was walked into anyway.

**The English catalog reordered itself in Arabic**, on a screen nobody had isolated: every sentence's full stop jumped to the front of its last line, ".your own" rather than "your own.". Fixed on the picker. **#226's worklist is the rest of this**, and it is worth taking seriously.

**332 instrumented tests pass**, up from 325. **One flake seen once and not since**: `RoundTripTest.unknownSurvivesAsUnknownRatherThanAsNullOrToday` failed with "attempt to write a readonly database" inside `Backup.recomputeRanges`, then passed on the next run with no change. Not investigated, and recorded here rather than forgotten.

**A missing catalog key crashed the app on opening**, and nothing caught it: the four catalogs agreed with each other, seventeen checks passed, the Kotlin compiled and lint was clean, because nothing compared the literals in the code against the catalog. **`check_string_keys.py` now does**, and it was proved against the real crash rather than assumed.

---

## 4. What #201 landed

**Both screens are converted, swept and closed.** Both themes, font scale 2.0, Arabic, and the search's own empty state, on the phone. Reviews at **#239** and **#240**, `DESIGN.md` section 14 carries both rows, and D104 and D105 carry the two decisions.

**The sixteen project templates gained a `category`**, one of `paying`, `challenge`, `moving`, `papers`, held to that closed set by `check_templates.py` and labeled per locale under `projects.category.*`. **It is what the person is trying to do, not what kind of office it involves**, and it is not `phase`, which is build order and never reaches a screen. `templates/SCHEMA.md` carries the definition. Both screens group by it in the same fixed order, which lives in each file as `CATEGORY_ORDER`.

**Both screens were walls of sixteen cards and are now rows in folds.** The picker leads with the person's own templates, or with the first category when they have none, and searches. The library leads with what has actually produced something, as cards, and folds the rest.

**`DenseRow` gained `subtitleMaxLines`, defaulting to 1.** Every subtitle on the picker ended mid-sentence at one line. **Raise it only where the second line is a sentence somebody reads rather than a tag**; the fixed row height is what makes a long list scannable.

**The fixture's projects carried no `template_id`**, so the library could never show what any template produced, which is the whole reason it is a library rather than a catalog. Three of the five now do, and `check_fixtures.py` holds the ids to the real catalog so renaming a template fails the build rather than producing a project pointing at nothing. Third instance of this shape after #237 and #229.

**325 instrumented tests pass**, up from 313: `StartProjectScreenTest` covers the grouping and the search, and `ScreenReaderTest` walks both screens.

**The catalog is still English inside an Arabic layout**, which is #62 and not new. Every template name and subtitle now goes through `Bidi.isolate` so it cannot reorder against the layout, which is a patch over that rather than a fix for it.

---

## 5. What #200 landed, and the four issues that came out of it

**Both halves are built, swept, tested and logged.** This section is here because the next session inherits the decisions rather than the work.

**The arc.** The milestone reader and writer, `MilestonesScreen`, `AddMilestoneScreen`, the door from the chapters list, and the shell wiring. All verified on the device including the parts a previous session could only compile: **marking a milestone by hand and choosing a chapter makes the chapter door appear on its row, and the chapter's own "What was worth marking" fold shows it back.** Rule 18 holds in both directions, seen rather than asserted. `docs/screenshots/milestones-v4-light.png`.

**The fixture still cannot exercise the chapter link on its own**: generated milestones carry no `chapter_id`, so a walk that needs the door has to mark one by hand first. That is **#237**.

**Month review**, `MonthReviewScreen`, reached from the trail's own month heading, which now carries a chevron. Hero is the month's milestones and nothing else, then where they were, appointments, what went wrong, what was answered, paperwork, and a fold holding everything written down. One filled action, which shares the month as a document through `Readable.monthReview`. `docs/screenshots/review-light.png`.

**Two defects were found by looking at it and are fixed:** a place that began and ended in one month listed its name twice, and an incident reported and answered in one month listed twice under two headings. Both now read as one row. The gold total band was built and removed the same day, for the reason in `DESIGN.md` section 14.

**The sweep is done and both screens passed it.** Both themes, font scale 2.0, and Arabic, on the device. `docs/screenshots/` holds `milestones-v4-light`, `milestones-v4-dark`, `milestones-arabic-dark`, `milestones-max-font-dark`, `review-light`, `review-dark`, `review-arabic-dark`, `review-max-font-dark`. Nothing clipped at 2.0, the last item clears in both, the trail mirrors with the spine on the start edge, and the person's own words stay isolated in Arabic.

**313 instrumented tests pass**, up from 297: `MonthReviewTest` covers the boundary rules, and `ScreenReaderTest` now walks the arc and the review.

**Rule 12 is discharged for both.** The arc's review is **#235** and the month review's is **#236**, each with its device screenshots, what it was composed from, what was deliberately not invented, and what I was unsure about. `DESIGN.md` section 14 carries both rows.

**Two things were found and filed rather than built:** the fixture never gives a milestone a chapter, **#237**, and `milestone.measure_id` is a schema link nothing reads, **#238**, which needs the owner's decision because expressing it at all comes close to interpreting a measurement.

**The device holds one extra milestone**, "Sat up for the whole visit", written by hand to exercise the chapter link. `tools/seed.sh` clears it.

---

## 6. What is built

**Design direction v4 is adopted and most of the app is in it.** `reference/screen-grid.html` is the v4 grid. `DESIGN.md` was rewritten rather than patched.

- **Step 1, the foundation: complete.** Every token in both themes, the type scale with all three faces verified per locale, the geometry, and all sixteen components. #149 through #168 closed.
- **Step 2, the four destinations: complete.** #169 through #172 closed.
- **Step 3, the section screens: complete but for #182**, which is blocked. Fourteen closed on device verification.
- **Step 4, the detail screens: thirteen of twenty closed.** #189 through #198, #200, #201 and #202. #199 is blocked; #203 through #208 are untouched.

**#192, one medication, closed with its remainder split out rather than left vague.** Its questions are built and the fixture never exercises them, **#229**; its incidents cannot be expressed because the schema has no link from an incident to a medication, **#230**, which is the owner's call.

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

---

## 9. Blocked, and it does not stop the work

**One thing is blocked: B5.** The destructive command guard needs installing from user settings and **only the owner can do it**, because Claude Code correctly refuses to let a session edit the hooks that constrain it. D64 has the account and B5 in `DECISIONS.md` is written as steps he can act on.

**It does not stop anything.** What has protected this repository through five long unattended runs is rule 6 followed by hand, plus Claude Code's own classifier. **Do not spend time re-probing the guard**; the answer is known until B5 lands.

**Two schema decisions are also the owner's**, and they block #182 and #199 only: the tests section has no table, no query and no screen. Everything else on the board is buildable.

---

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

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. **It appends the theme, so passing a name ending in `-dark` yields `-dark-dark.png`**; rename after. It refuses to capture unless the app is focused, suppresses heads-up notifications, and crops the status bar at a height read from `dumpsys window`. D31, D53, D72. **Look at every image before committing it.** The script is a control and it is not the last one.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**A trap in the Compose test API, found the hard way.** `performScrollToNode` walks a lazy list a viewport at a time and gives up when it thinks it can go no further. It got that wrong for the Arabic catalog, stopped two rows short, and reported the rows as absent when they were only further down. **Scroll by the list's own item key instead**, with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` rather than on a surface around it: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Continuous integration.** The workflow triggers on `push` to main, on `pull_request`, and on `workflow_dispatch`. Pull request events stopped firing part way through 2026-07-31 and **are firing again as of 2026-08-01**. If they stop again: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>`. **Do not read an absence of checks on a pull request as a passing build.**

**Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools`, or `contract` without touching this file. It caught pull request #49. "README.md describes the screens that exist" fails any pull request that adds or removes a file under `ui/screens/` without touching `README.md`, which exists because the front page claimed the app had one screen for a week after it had nine. "Every screenshot the README points at exists" catches a rename. Rewrite the documents in the same commit as the work, not afterward.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**Everything else:** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

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
