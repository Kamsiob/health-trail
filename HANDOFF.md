# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-12 at 18:20, on the Pixel 8.

**#361 is the work, and its first item is done.** The owner's words were that the app looks like a data entry app, cluttered and visually complicated, throughout rather than on one screen, and he named the document form, the notebook, the trail and the care team. **Item 1, the five forms that predate law 3, is built and walked on the phone.** Saving a document is three questions with the paper itself as the first one; adding a person, a medication, an appointment and a bill lead with what somebody has in their hand and put the rest behind "Add more". D147, and `DESIGN.md` 14 has a row for each.

**Item 2, the trail, is started.** Two changes landed: a row under a month band says "Monday 29" rather than repeating the band's own month and year, and the year rail's labels sit on a hairline so the index reads as a control rather than as two digits at the edge of the page. **The rail was built all along and invisible**, which is why "no way through time but a thumb" was true and the tool existed.

**The next decision on #361 belongs to the owner and is on the issue.** Screen 08 draws filter and search one tap away in the header; what is built is a search field and four chips inline, taking a third of the fold before any entry. **#220 chose the visible chips deliberately under law 2, and D142 made the grid authoritative afterward**, so the two need reconciling by a ruling rather than by a commit. **The other finding is that the endless scroll is inside the open month rather than between months**: between them there are folds and the rail, and inside June there are forty three entries with nothing but gap markers. **A chart of how much was written per month is ruled out** by rule 13, since that is a progress meter on the person's own diligence; an index of presence is not.

**Then item 3 the notebook, then item 4 the care team's faces and grouping**, which is #353 and is blocked on nothing writing `person.organization_id`. **The five forms are waiting on the owner's eye**, #362 through #366, because none of them is drawn in any grid and none ever was.

**Last rewritten before that:** 15:35. **Since the swap**: seven sheets that could not reach their own action on a short screen now scroll, the notebook uses the whole screen, all eight workflow defects on #360 are built and walked, the app's language changes without a restart, the fixture shows three links that were built and never seen, and `verify.sh` keeps its report and refuses to start with the notification shade open. **Last rewritten before that:** 13:25, paused mid task with the phone being swapped. **Section 10 opens with the device handover: read it before any device work.** **Nothing is uncommitted and nothing is half written**: the tree is clean, everything is on `origin/main`, and what is left is listed on #360 rather than sitting in the working copy. **Last rewritten before that:** 03:05, mid run. The owed full device run came back **611 green, exactly as #321 predicted**, #349 was ruled by the owner and built, and **the #345 fidelity pass moved from the four daily screens to the section screens under them**, which is where the drift turned out to be.

---

## 1. Where to start

**Read this file, then `gh issue view 321`. Those two, and nothing else, before you start.** #321 names the next action and the order of work; this file says what is true right now. They are different jobs and neither repeats the other.

**Then read only what the work in front of you needs.** The repository holds about twelve thousand words of documentation and a session that loads it all has spent its context before writing a line.

| When | Read |
|---|---|
| Always, and they are short | `CLAUDE.md` (loads itself), this file, issue #321 |
| Before doing the thing | `docs/TRAPS.md`, **one section only**, chosen from its own table |
| A visual question | `DESIGN.md`, the numbered section, not the file |
| A data question | `contract/DATA-CONTRACT.md` |
| "Why is it like this" | `DECISIONS.md`, search for the D number, do not read it through |
| Delegating | `AGENTS.md`. Subagents never write anything |
| **Never, to orient** | `docs/RUN-LOG.md`. It is history and a thousand lines of it |

### Verified rather than asserted, 2026-08-11

- The working tree is clean and everything is on `origin/main`. **Check rather than trust**: `git status --porcelain`.
- **25 repository checks pass**, `python3 tools/checks/run_all.py`. **`check_silent_clip.py` and `check_bidi_isolation.py` landed on 2026-08-11.** The first refuses a `maxLines` with no overflow treatment, because Compose clips at the line box and a cut with no ellipsis is truncation with the evidence removed. The second was converted from a worklist once its residue reached zero. **`check_reader_coverage.py` landed the same day** and holds every screen in `ui/screens` to a case in `ScreenReaderTest`, because `DESIGN.md` 12 claimed the test walked every screen and it walked 44 of 75. **`check_dead_gestures.py` grew a third rule on 2026-08-11**: no live screen puts an action behind a long press, and the frozen file that still carries one is exempt by reading `docs/REMOVAL-LEDGER.md` rather than by a second copy of the list. **Three landed on 2026-08-10** and each holds something that is invisible by looking: `check_digest_sections.py` holds the digest's table mapping to the change log's own trigger literals, `check_dead_gestures.py` refuses a handler assigned a lambda that does nothing and an `openableByTap` labeled with a remove string, and `check_readable_labels.py` gained a clause holding a `link` column's declared catalog to `templates/data`.
- **218 unit tests pass and they need no phone**, and **three of them are the archive's regeneration**, which until 2026-08-09 could only run on the phone. `contract/test-vectors/readable/`. On a day when the device is unreachable this is most of what is left, and it is worth writing logic where these can reach it rather than only into a composable.
- **A full run is owed and the count will have moved.** Since the last full run the suite gained `SectionsReachableJourneyTest`, `WelcomeSeenTest`, `IncidentCorrectionTest`, `MedicationsScreenTest`, `MedicationQuestionCountsTest`, `CareTeamScreenTest`, `ChaptersScreenTest`, `AppointmentsMonthTest` and three `ScreenReaderTest` cases. **Run `tools/verify.sh --device` and read the number out of the report rather than trusting arithmetic**, then write it here.
- **`verify.sh --device` keeps every report now**, timestamped, under `android/app/build/outputs/androidTest-results/history/`, and **refuses to start with the notification shade open**. Both were flake confounds: #302, #308 and #316.
- **642 instrumented tests, 1 failure, on the run before this afternoon's work**, and the failure was a test that had not waited for its own callback rather than anything in the app. **A run is in flight as this is written** and its count will be higher again: `SectionsReachableJourneyTest`, `WelcomeSeenTest` and the incident tests all landed after it.
- **The three failures on the first Pixel 8 run were real and are fixed.** They said "expected the words but was null", which reads like a save that did not fire, and what was true was that a sheet's own Save sat below the fold with no way to reach it. **The old phone was one of the tallest Androids made and hid that for the life of those screens.** `docs/TRAPS.md` section 1 carries the technique for testing a small screen without swapping devices.
- **The suite gained `TrailScreenTest`, six tests, on 2026-08-12**, and it is the first test file the trail has ever had. **A run is owed**: expect 657 rather than 651. `AddDocumentScreenTest` and `ScreenReaderTest` were rerun after the last screen change, 105 tests, no failures.
- **651 instrumented tests, 4 failures, 2026-08-12 at 18:20**, `tools/verify.sh --device`, with 218 unit tests and lint. **One failure was real and is fixed**: Today's first coaching step had picked up "need to", which `TodayScreenTest` bans as scolding, when the words changed earlier that day. **The other three did not reproduce**: two `SQLITE_READONLY_DBMOVED`, which is #346, and one Compose timeout in `MedicationQuestionJourneyTest`. All four classes were rerun together and came back 25 of 25. **The next full run should be 651 green.**
- **637 instrumented tests passed, 0 failures, 2026-08-12 at 16:20 UTC**, after #358, and **five workflow fixes have landed since on #360 and are walked but not yet in a full run**: expect 637 again unless a test is added. **#360 is the live list**: eight defects a focus group pass found in the shell's navigation, three fixed, five open, and **the blocking one is that an appointment can never be corrected after it is saved**, `updateAppointment` being dead code no caller can reach, which also breaks rule 17.
- **636 instrumented tests passed, 0 failures, 2026-08-12 at 15:30**, with 218 unit tests and lint, through `tools/verify.sh --device`. **Nothing is owed.** The count is read from the root element of `android/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`, because `verify.sh`'s own summary table lists the unit classes only.
- **The run before it came back 636 with two failures, and neither was in the file that caused them.** `AppointmentsMonthTest` switched the appointments view to the month, **the choice is remembered on this phone because that is the feature**, and every later test composing that screen got the month: `assertExists` on a row no longer drawn, and the reader sweep on a screen it did not expect. **A test that changes a remembered preference puts it back**, before and after, which is what that class does now. **This is the shape to watch on any screen with a `ViewToggle`**, and documents is the other one.
- **The 00:31 run that night was 611 green**, exactly what #321 predicted, so nothing had landed unchecked before the fidelity work started.
- **The phone is not on a dark schedule any more**, checked 2026-08-11 at 22:00 and again at 00:03: `ui_night_mode` reads 1 and `cmd uimode night` reads "no" at both. **The old note here said it flipped to dark at 17:00 and back at 06:30**, and a session trusting that would misread its own baseline in the other direction. **Leave night mode alone either way**: rule 19's exception covers font scale, animation and the reader, and nothing else on the phone. It also means **the disclaimer gate cannot be seen in dark from here**, because it shows before the app has a theme choice of its own. #204.
- **Continuous integration is green on `main` at the tip**, checked 2026-08-12 at 18:45. Check after every push: `gh run list --branch main --limit 3`.
- **Three CI runs went red on 2026-08-12 on commits that changed markdown and screenshots**, and none of it was about this code: Compose BOM 2026.08.00 published and `lintDebug` failed naming a version catalog line nobody had touched. **D121 had disabled two of the three version currency checks and left `GradleDependency`.** All three are disabled now, D148, and Dependabot still owns staying current. **The lesson is the entry**: when a rule like that is written, check whether the tool has a third of the same thing.
- **The phone was unplugged at 13:25 on 2026-08-12 and is being replaced.** Section 10 has the handover. **Nothing device dependent runs until the owner says the new one is connected.** What follows is how the old one was left. **Its accessibility settings are at their baseline** between walks and were read back rather than assumed after each one: font scale 1.0, animator null, no per app locale. **The app's own theme setting is back on "Follow the phone"** after being switched to Dark for a walk. **A tap landed on the owner's launcher once tonight**, because `connectedDebugAndroidTest` had uninstalled the app and a walk started without checking: nothing was read or kept, the stray capture was deleted, and `docs/TRAPS.md` section 1 already says to check `pm list packages` first. **That check is the one to actually run**, not remember.
- **The phone was returned to its starting values earlier on 2026-08-12 and unplugged**, every setting read back rather than assumed: font scale 1.0, animator null, touch exploration 0, no per app locale, and the accessibility services string the KDE Connect one alone. **It holds the current build and the month six fixture** with the disclaimer accepted, and none of it is real. **The 30 MB file pushed for a size limit test was removed.** **Two taps left the app onto the owner's own screens during the last session**, the dialer and a ride app, both backed out of without reading or capturing anything: bounds had been read from one screen and the tap sent after navigating to another. `docs/TRAPS.md` section 1 now says to re-dump immediately before every coordinate tap and to check `mCurrentFocus` afterward.
- **The owner is testing the build by hand, and a lot landed on 2026-08-11 that he has not seen.** In the order he is likeliest to meet them: **Money says "$13,771.73 not settled"** where it said "6 items"; **every other notebook section now counts in its own units**, 15 people and 182 entries and 6 bills rather than N items for all twelve; **a person's screen can write something down about them**, with their name already attached; **the documents List view now shows a list**, where it used to change the pill and nothing else; **searching for a project by name now opens the project** rather than a screen saying it was not built; **long subtitles wrap instead of stopping mid-word**, which is most visible on the care team at a large font; **removal is now an outlined "Remove this" on each thing's own screen** rather than a long press; **a question that has not been asked opens** onto its own face; **a document is asked when it is from** and no longer stamped with today; **a new notebook gets a first days list** on Today from its setting's checklist; **Progress can track something he names himself**; and the foot of most screens now has its actions sized to their labels with only the way back full width. Before that, from 2026-08-10: the date picker's month and year views, the trail's kind filter, cards having a shadow in light theme, and the document folder field.
- **The four named failure modes that were open on #212 are down to two**, 2026-08-10. Time, numbers and absence each have their own tests now, `RoundTripTimeTest` and `RoundTripValueTest`, eleven between them. **Unicode is #227 and is a change to every write path rather than a test**, and the four gigabyte half of scale needs a fixture nobody has written.
- **Four more test archives sit in `Download` from 2026-08-10**, beside the ones from 2026-08-09 and 2026-08-04, and every one holds nothing but the fixture. **Three use `missing332` and one uses `catalog329`**, and **all four open on the current build**. The `missing332` three were written before #332 closed, with two attachment files absent, and were refused by name at the time; the manifest declares a missing attachment now, so the same files restore. `catalog329` is Arabic and is the one #329 was proved on. **A fifth, `gone332`, is the one that proved the restore**: written with a file genuinely absent, opened, and restored into the app. The older ones are `arabic327`, `arabic328` and `arabic328b`, and those open too. A locked file whose passphrase nobody recorded is a file nobody can use.
- **#62 puts English paragraphs inside an Arabic screen**, seen on the phone 2026-08-11 rather than reasoned about. The standing instructions screen reads Arabic throughout and then carries two full English sentences explaining what a federal rule does or does not require, out of `templates/data`. **The issue title undersells it**: this is not a library screen full of untranslated names, it is the sentence a family most needs to understand sitting next to their own words in a language they did not choose.
- **A question can be attached to an appointment in the schema and nowhere else**, read out on 2026-08-11. `asked_at_appointment_id` exists, the archive renders it, all four catalogs name it, and **the only thing that has ever written it is the fixture generator**. The model does not carry it, nothing writes it, the prep sheet cannot create a question, and the reverse link does not exist. **That is the rest of #46 and it is not small**: half of it shipped is a question claiming an appointment it does not appear on. Same shape as #330.
- **Version one ships English**, owner decision 2026-08-11, `DECISIONS.md` D141, which supersedes D58 by name. About 1,500 strings are regulatory and rights content, and a confident wrong translation of that is somebody acting on false information about their rights. **It is scope, not architecture**: string externalization, the four catalogs, locale-aware formatting, every translated string already written, and **full right to left on every screen** all stay. **RTL is verified against a forced layout direction** rather than against Arabic content, so the structural check survives the content moving out. Spanish, Chinese and Arabic are on milestone 8 and labeled `deferred-by-d141`, never closed.
- **The built screens do not match the grid files**, owner finding 2026-08-11, D142 and `DESIGN.md` 16.6, tracked at **#345**. Spacing, elevation, type scale and composition drift, and **no audit was asking whether a screen matched what was approved**. The grids are now authoritative on measurement, the fidelity check is item 11 of the closing checklist, and `check_token_drift.py` ratchets the 161 hardcoded dp and sp literals downward. Retroactive per rule 14, which is the expensive half.
- **Every `DESIGN.md` section pointer in `CLAUDE.md` resolves again**, #344 closed, and `check_cross_references.py` holds all 40 of them. **The two history files are skipped on purpose**: ten of their pointers are stale the same way and correcting them would falsify a dated record, so both say at the top that their numbers are as of the entry.
- **The fidelity pass, #345, has now compared fourteen screens**, 2026-08-12 at 03:00, and **seven drifts are built and closed**: #347 and #348, both verified on the phone again and closed on 2026-08-12 having been built the day before and left open, #351 the care team's lead group, #352 the medication row, Money's "Needs attention" eyebrow, #354 the emergency card as rows, #355 a way to write a question down, #356 the current chapter's counted line, and #357 the appointments month view. **#353 and #358 are filed and not started**: #353 is blocked on nothing writing `person.organization_id`, and **#358 is that an incident cannot be corrected or removed from its own screen**, which is the most consequential thing a family types and the one thing they cannot fix afterward. **Its write half landed on 2026-08-12**, `updateIncident` and `removeIncident` with three tests that have not run: what is left is the two controls, the confirmation, and the walk. **The correction form is the piece to think about rather than type**, since no `AddIncidentScreen` exists and an incident is created by the capture form. Progress, Documents, the trail, capture, Today and the notebook all hold.
- **Three findings this pass are lessons rather than screens.** **A count pill is a quantity**: a phrase in one crushed a fold's label to a three character column at font scale 2.0 and, once both sides were given weight, was clipped by the pill's own ellipse. `DESIGN.md` 15.1. **A merged node is not in the tree a finder walks**: a `DenseRow`'s or a chapter card's test tag assertion passes when the line is absent and fails when it is there, which is a test proving the opposite of what it says. Assert on the words. **And `MonthGrid` had been finished and composed by nothing for over a week**, which is the same shape as `RootState`: a component nobody can reach is a component nobody reviews.
- **The fidelity pass, #345, had compared ten screens by 02:00**, 2026-08-12. **Four findings were documents that had stopped describing the app**, all corrected. **Six were screens that had drifted**: the notebook's counts #347, an entry's place in its thread #348, **the care team's flat list #351**, **a medication row silent about a waiting question #352**, **Money's missing "Needs attention" eyebrow**, and **the emergency card #354**. **Five of the six are built**; #354 is filed and not started. **The pattern is the finding**: the four screens somebody spends all day on had been looked at hardest and drifted least, and the section screens under them had never been compared to anything. **Where the pass goes next is the rest of the section screens**, then the detail screens.
- **#353 is the half of #351 that the data cannot support yet.** The grid folds the care team by where each person works, `person.organization_id` is in the schema with an index, and **nothing writes it**: the add person form has no field and the fixture creates no organizations. Building the folds today would produce one fold holding everybody. The order of work is on the issue: the writer, then the fixture, then the folds.
- **The fidelity pass, #345, compared five screens on 2026-08-11 and the score then was four stale documents to two drifted screens.** Today, the notebook, the trail, capture and one entry, each against its drawing. **Both drifts are now built**: #347 the per section counts including Money's total, and #348 an entry saying which step of its thread it is. **The four documents were the risk**: the type ladder said 19 to 20 where the grid and the app both said 22, 15.1 said the trail's filter was not built when it was, capture's "Skip this" was a departure recorded only in a code comment, and D142 as first written would have restored two colors that fail contrast. **A session reading the documents to build a new screen was at more risk than the screens already built.**
- **The token layer is faithful and that is settled**: nine of eleven colors match the grid exactly and the other two are recorded accessibility corrections, the spacing and radius vocabulary agrees, elevation matches to the alpha, and **Material defaults are not leaking**, checked across 114 `Text` calls, 26 `Surface` calls and 17 sheets. **What is left on #345 is composition, which only a person looking can find.**
- **63 remote branches survive and none of them is safe to delete without a ruling**, 2026-08-11. They have no pull request at all: they are direct pushes from long autonomous runs that then committed to `main`, so no merge event ever fired to clean them up. **Every one is an ancestor of `main`** and holds no commit `main` lacks. Two branches with merged pull requests were deleted, #70 and #65. **`delete_branch_on_merge` was already on**, verified by reading it. **Never use `git branch --merged` here**: squash-merging gives the branch's work a new sha and the command reports merged branches as unmerged. D144, and `CONTRIBUTING.md` carries it.
- **The destructive command guard is live and proven.** It refused three real commands, two on 2026-08-08 and one on 2026-08-09, and it also refused a run log paragraph that merely named a blocked verb, which is #323. Section 9.

### The six that actually get broken

1. **Run `tools/verify.sh`**, not the checks you happen to remember. It is the only runner that compiles the instrumented sources and runs lint.
2. **An issue closes only on device verification**, per `DESIGN.md` 16.4: both themes, font scale 2.0, right to left, and every state including the empty one.
3. **Commit and push after every working increment**, and check CI after each push. An increment ends when `origin/main` has it.
4. **The fixture must produce rows the app itself could write.**
5. **Look at the screen before closing anything.** Five defects on Today on 2026-08-08 were invisible in the code and obvious in a screenshot, and five more on 2026-08-09 were invisible in a green test suite.
6. **`tools/seed.sh` drives the restore screen**, so a change to that screen breaks seeding. It did on 2026-08-09 and the seed reported failure while leaving an empty notebook.

## 6. What is built

**Design direction v4 is adopted and most of the app is in it.** **Cards are no longer flat**, 2026-08-10: `ui/theme/Raise.kt` is the one place a surface is lifted, `Elevation` is read by something at last, and dark theme stays flat because 4.7 says elevation there is `paper` to `card` rather than a shadow. #324, and it was retroactive per rule 14. `reference/screen-grid.html` is the v4 grid, with `today-grid.html` and `projects-grid.html` for those two surfaces.

- **The foundation, the four destinations and the section screens: complete**, but for #182, which is blocked.
- **The detail screens: thirteen of twenty.** #199 is blocked, #203 through #208 are untouched, and they are milestone 4.
- **Milestone 1, Today: finished.** All ten grid screens built and walked, parent #243 closed, and the only issue left on that milestone is the tracker #321.
- **Milestone 2, Projects: four issues left and all four are blocked.** Section 9 says on what.
- **Milestone 3, the archive: five issues left and every one is blocked on something a session cannot do.** #15 needs a second platform, which is #16 and does not exist. #211's last criterion is the `app_meta` question. #212's last two modes are #227 and a four gigabyte fixture nobody has written. #210 and #9 are parents. **Four issues closed there on 2026-08-10**: #329, #331, #332, and #210's locale question.
- **Milestone 4, the rest of the v4 conversion: twelve issues, and section 9 lists them.** **Ten closed on it in two days**, 2026-08-11 and 12. The account below is the older one and is kept for what it says about each. #324, #231, #132 and #221 closed on 2026-08-10, and **#218, #135, #339, #340, #341, #342 and #226 on 2026-08-11**. **#343 is the one filed and not started**: the screen shown when the notebook cannot be opened should offer restore rather than telling somebody to install the app again, and **its seam is already open**, `RootState` is internal and `RootStatesTest` walks it. #57 still needs an owner decision about the camera.
- **#204, #205 and #46 are open on things a session cannot finish**, and each carries what was done and what was not. Two of them wait on the same wall: **a reader pass cannot be captured over adb**, because `uiautomator` reports the view tree rather than the merged semantics tree, per D68.
- **THE ARCHIVE is largely built and proved on real hardware**: a two-layer container at format version 3, a readable copy, a standalone decryptor at `tools/decrypt/` tested in CI, and the format published byte for byte in `contract/EXPORT-FORMAT.md`. **The stranger test passes**, run on 2026-08-09 on a laptop that has never had the app.
- **The readable copy is written in the person's language and so are its values**, #327, #328 and #329. Verified by exporting in Arabic, decrypting with the passphrase alone, and reading the pages in a browser: 128 field labels, 39 headings, 81 stored values across 17 vocabularies, money as money, and no bare epoch anywhere. **#329 closed the last of it on 2026-08-10**: a link into a shipped catalog resolves to that entry's name, and the two indexes stopped being printed. **A sweep of a real Arabic archive now finds no raw identifier, no schema token in a `<dd>`, no five digit integer, and no bare `0` or `1`.** D130.
- **The contract now carries four files for the readable copy, not one.** `contract/readable-money.json` is the fourth, added 2026-08-10 for #331: the rules an amount renders by and the ISO 4217 codes whose minor unit is not two digits. **Money is no longer asked of the platform**, because `java.text.NumberFormat` answered differently on Android and on a JVM and 8.5's byte identity was therefore a claim about one phone. D131.

**The older three, unchanged.** `contract/readable-fields.json` is what each column renders as, `contract/readable-vocabularies.json` is the fixed vocabularies those decisions name, and `contract/test-vectors/readable/` is the golden vector. All three are generated into the app by the build, so nothing is hand kept on the Kotlin side.
- **The importer merges as well as replaces**, #211. `Merge` is pure so the rules that decide whose version of a note survives are unit tested without a phone: match by id, later `updated_at` wins, `origin_device` breaks a tie so two phones reach the same answer in either direction, and merge never deletes because removal travels as a tombstone. Every resolution goes to `conflict_log` with both sides whole, and **there is a screen that reads it**. **One criterion of #211 is left and says so on the issue**: per-section view choices, which wait on the `app_meta` question. The missing attachment criterion was met when #332 closed on 2026-08-10.
- **An attachment whose file is gone no longer makes an unopenable archive**, #332, closed 2026-08-10. The export compares live rows against the files on disk, `MANIFEST.json` carries the missing list with each row's name and date, and `open` accepts an archive that declares what it is missing while still refusing one whose attachment is absent and undeclared. **That difference is the point**: it separates a record with a gap from a copy damaged in transit. Proved end to end on the phone, export to decrypt to restore. D129 and D132.
- **B4's argument is finally true.** It dropped the emulator because "data survival is proven by the round trip against the golden vectors in continuous integration", and **nothing in continuous integration rendered a readable page at all** until 2026-08-09: `RegenerationTest` is instrumented and `DateVectorTest` reads assets. `ReadableVectorTest` is the half that needs no Android. **Regenerate it deliberately**, `-Dhealthtrail.vector.write=true`, and read the diff.

**Nothing on `main` is unverified.** Every screen that has shipped has been on the phone at both themes, font scale 2.0 and Arabic right to left, **including the two the merge added on 2026-08-09**, which are waiting on the owner's eye rather than on a walk.

- **A setting applies its first days list and its papers, not only its threads**, #135, 2026-08-11. The ten checklist items and six document slots each setting has always carried are **a steps-led project** now, named "The first days", with the slots as its papers, and Today's first card points at it. **No schema change**: `project_step`, `project_paper` and the `project_steps` card type all already existed. D136.
- **#226 is closed**, 2026-08-11. Every place a string a person typed reaches a screen is isolated or carries `// bidi-ok:` and a reason, and `check_bidi_isolation.py` fails the build on a new one. **A green check means every place has been decided, not that every decision is right**: only Arabic on the device shows that, and **it proved the point on 2026-08-11**. The medications list rendered a dose raw while the medication's own screen isolated the same string, because the check read four argument names and `trailing` was not one of them. It reads nine now. **The capture form, a person's screen and a medication's have been walked in Arabic**; the rest moved to #205.
- **`tools/device.sh` now installs every time and refuses a stale APK**, 2026-08-11. It used to install only when the package was missing, so a phone that already had the app kept the old build and a walk after a change walked the change that was not there. **`compileDebugKotlin` does not build an APK**, only `assembleDebug` does, and the refusal says so by name.
- **#207 has a first pass on it**, 2026-08-11, from an empty notebook and at font scale 2.0. **Two defects fixed**: a row truncating its own sentence mid-word on the chapters screen, and an empty emergency card offering to share a document with nothing on it. **Two things that look wrong are already decided**: the empty notebook is twelve rows saying "Nothing yet" by design, and a notebook with no situation shows all twelve unfolded on purpose. The error states, Arabic on the empty ones, and the loading states are what is left, and they are on the issue.
- **The five add forms ask rather than stack**, #361 item 1, 2026-08-12. **Saving a document is three questions**, with progress dots, a way on that says "Skip this" until the question has something in it, and save live from the first one. **Its first question is an empty sheet at the size of the screen** and the sheet is the control: staging alone left a title, one outlined button and two thirds of a Pixel 8 doing nothing, which is rule 11 and was found by looking. **The other four use the disclosure rather than stages**: the care team leads with the name and the number, a medication with the name, the dose and the emergency card question, an appointment with what it is and roughly when, and a bill keeps all three of what it is, how much and where it stands. **Correcting a saved record is never staged and opens the disclosure**, D147, because somebody who came to fix one line should not be walked through a conversation. `AddDocumentScreenTest` is eight tests including right to left against a forced layout direction. **The `check_token_drift.py` baseline is 154**, down one: the sheet is sized from the thumbnail vocabulary rather than from numbers typed into the screen.
- **The picked photograph's half of that form cannot be walked from here**, and that is a device limit rather than a gap: choosing one opens the owner's real photo library, `docs/TRAPS.md` section 1. The empty sheet, the three questions, the disclosure and the correction path are all walked and tested; what a chosen image looks like inside the sheet and carried into the later questions has been seen only in code.
- **The capture button blooms**, 2026-08-12, which is what grid screen 04 always drew: six labeled choices rising from the button itself over a dimmed screen, one width so the left edge is a line and the marks are a column. **Movement only, never opacity**, because a pill fading in is a target somebody can tap and not see. **The owner asked for exactly this and approved the shape.**
- **The keyboard goes down when the bloom opens**, and that is the whole diagnosis of a journey test that failed four times: the modal sheet it replaced was its own window and took the IME with it, a menu in the same tree does not, and a back press with the keyboard up dismisses the keyboard rather than the form.
- **The care team leads with the people you actually call**, #351, 2026-08-12. Three lead, the rest fold behind a counted row, and **a roster of four or fewer is not split at all**. `peopleByRecentUse` decides which three and **this was the only list in the app not asking it**, so the fix was an ordering the data layer already had. With no history it degrades to the order people were added in.
- **A medication with a question waiting says so on its row**, #352, 2026-08-12. `question.medication_id` had a writer and a reader on the medication's own screen and **the list was silent**, so finding out anything was waiting meant opening every medication in turn. **A medication with none is absent from the count map** rather than present with a zero. **The trap it cost two runs to find**: the node carrying a `DenseRow`'s test tag has no text of its own, the merged node beneath it does, and asserting on the tag reads exactly like the line being absent.
- **Somebody can start a care thread the templates never heard of**, #349, 2026-08-12, on the owner's ruling. **Applying a situation was the only thing that had ever written `care_thread`**, so a person dealing with a landlord, a school, or an employer's leave department had a real recurring situation with no spine of its own. `createThread` is one insert with `template_id` null, the screen asks a name and nothing else, and it is reached from the foot of the threads list and from the empty state's own action. **The new thread opens straight away**, as a project started from nothing does. D145, review at #350, and **template-created threads are untouched**.
- **Somebody can track a thing the catalog never heard of**, #203 in part, 2026-08-11. Sixteen presets was the only way in. Naming your own is a third answer to "what are you tracking" rather than a second screen, a number and words stay two things, and nothing named carries a claim. **What is left of #203 needs a content decision**: the presets carry no group, so grouping them and searching them are catalog work. D138.
- **Removal is reached by looking, everywhere**, #218, 2026-08-11. `removableByLongPress` is deleted. Six things have a screen of their own and removal is an outlined "Remove this" at the foot of it; a question and a standing instruction have a sheet instead and it is there. **A question that has not been asked yet has a face of its own now**, because it had no tap at all before and the gesture was the only thing it answered to. D135, and the shape cannot come back: `check_dead_gestures.py` refuses `combinedClickable` and `onLongClick` in any live screen.

**The account of how it got here is `docs/RUN-LOG.md`, and it is history rather than orientation.** Section 6 of that file is 2026-08-09, the day milestone 1 finished: twenty-two issues, ten defects that were invisible in the code and obvious on a screen, four fixture modes that did not exist, and two decisions taken rather than escalated.

### Three things that are true and are not ticked anywhere

Each is said out loud on its own issue rather than counted as done.

- **The paperwork an incident produced** is not reachable from any seed: the fixture links no document to an incident, so the rows, and the tap that opens them since #360, live only in `IncidentScreenTest`.
- **The care team card's sparse rung** and **the trail spine's gap markers** are not reachable from any seed. Held in `TodayFieldScreenTest` instead.
- **The digest's corrected and removed counts** render, and no seed produces them: the generator's updates land on rows it inserted in the same window.
- **The document card's empty rung** needs an install with no data behind it. **That is reachable, and this line used to say it was not**: `connectedDebugAndroidTest` uninstalls the app when it finishes, so installing afterward and not seeding is a genuine first run. It is how the empty notebook was walked on 2026-08-11. The route the guard refuses is a different one, and it is right to refuse it.
- **#273's two template hands are provisional** and the owner has not looked at them.

### The tools the fixture grew, because five screens were unreachable without them

    tools/device.sh year2 6 walk-year-three  --arranged
    tools/device.sh year2 6 walk-appointment --arranged --appointment-on YYYY-MM-DD
    tools/device.sh month6 6 walk-home       --situation home_family
    tools/device.sh month6 6 walk-quiet      --quiet

**The appointment date is an argument rather than a clock**, because `check_fixtures.py` holds one seed to byte identical output. **The last visit is a preference on the phone rather than a record**, so grid screen 04 is set up with `run-as` against the debug build:

    adb shell run-as com.kamsiob.healthtrail sh -c 'echo <base64 xml> | base64 -d > /data/data/com.kamsiob.healthtrail/shared_prefs/health-trail-visits.xml'

---

## 7. What keeps going wrong

**Moved to `docs/TRAPS.md` on 2026-08-08, and the move is the point.** Every trap in it cost real time at least once and they are all worth keeping, and a session that loads all of them at the start has spent a third of its context on device warnings before finding out whether it is doing device work.

**It opens with a table: read the one section that matches what you are about to do.** Touching the phone, running the tests, changing copy, writing a check, changing a screen, committing, or anything about this machine. Section 1 above carries the five that apply to almost every session.

---

## 8. Running the work

**Never route around a check to make progress, and never delete or weaken a test to make a build pass.**

    python3 tools/checks/run_all.py                    # 21 content and contract checks, seconds
    tools/verify.sh                                    # the honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest   # 601 tests, about eleven minutes

**Run `tools/verify.sh`, not the checks you happen to remember.** CI once failed on a lint error in code that had been walked on the device and passed every content check and 185 instrumented tests.

**One class while iterating, the whole suite before committing.** A single class runs in under half a minute:

    ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=com.kamsiob.healthtrail.data.MergeApplyTest

**Run the whole suite after any run that changes a screen**, and **do not touch the phone while it runs**. A capture attempted mid-run on 2026-08-04 force-stopped the app under the suite and produced 79 tests with one bogus failure. A run driven from two places at once tells you nothing.

**`connectedAndroidTest` uninstalls the app and takes the notebook with it.** Reinstall and reseed afterward:

    adb install -r android/app/build/outputs/apk/debug/app-debug.apk
    tools/seed.sh                       # month six, the notebook most walks use
    tools/seed.sh year5 5 walk-year-five

**Commit and push after every working increment**, per rule 7. An increment ends when `origin/main` has it. **An issue closes only on device verification**: both themes, maximum font scale, right to left, and every state in `DESIGN.md` 13.3 including the empty one.

**When a screen is undrawn, rule 12 wants three things at the moment it is built**: a `needs-design-review` issue with a real device screenshot, a row in `DESIGN.md` section 14, and a line in this file.

**Reading an archive needs no app and no phone**, and it is how the readable copy is actually checked:

    echo <passphrase> | python3 tools/decrypt/decrypt.py <archive> <folder>
    flatpak run --filesystem="$PWD" com.brave.Browser --headless \
      --screenshot=out.png --window-size=900,1400 "file://$PWD/<folder>/readable/index.html"

**Sweep a produced archive rather than trusting the field map.** Raw schema tokens, bare integers of five digits or more, and a bare `0` or `1` in a `<dd>` are the three shapes that have hidden real defects.

### 8.1 The running list of screens composed rather than drawn

Every one of these was built from the existing components, logged in all three places at the moment it was built, and is waiting on the owner's eye. **None of them is a defect**; the list exists so that no composed screen is mistaken for a designed one.

**Twenty-eight of them, oldest first.** Rechecked against the board on 2026-08-10 with `gh issue list --label needs-design-review` rather than remembered, because a list that is only partly a list is the defect this section exists to prevent. **#350 and #359 were added on 2026-08-12**, when starting a care thread and correcting an incident were built, and **#362 through #366 the same day**, when the five add forms were rebuilt on #361. **Those five had been undrawn since they were written and were never listed**, which is the gap this section exists to prevent: they were logged when #361 reached them.

**The Today work of 2026-08-09 added nothing to this list**, and that was checked rather than assumed: every screen and state built that morning is drawn in one of the three grids, so rule 12 never applied. The card's options sheet is grid screen 07, the closed project is 17, the greeting is 16, and the Today work is screens 01 through 10. **The merge work that evening added two**, because the grid draws restore with one outcome and draws nothing at all for reading what a merge decided.

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
| Merge or replace, on the restore screen | #333 |
| What the merge decided | #334 |
| The export finished and could not find a file | #335 |
| The date picker, zoomed to months and to years | #337 |
| A question that has not been asked yet, opened | #338 |
| Starting a care thread | #350 |
| Correcting an incident | #359 |
| Saving a document, in three questions | #362 |
| Adding somebody to the care team | #363 |
| Adding a medication | #364 |
| Adding an appointment | #365 |
| Adding a bill | #366 |

**Seven of these are the Projects surface**, #304, #309 through #313, and #317, and they are the ones that arrived in a single run. **Two are the merge**, #333 and #334. **Two are from 2026-08-10**, #335 the export's second outcome and #337 the date picker's two new views, and **one from 2026-08-11**, #338, the face a question wears before it has been asked. The other nine have been waiting longer.

---

## 9. Blocked, and it does not stop the work

**No machinery is blocked.** B5, the destructive command guard, is **resolved as of 2026-08-07**: it is installed in `.claude/settings.json`, it is live, and it refused a real removal command aimed at the app package. `DECISIONS.md` B5 has the account. **One defect came with it, #323**: it matches prose that merely mentions a blocked verb, so writing certain sentences into this file is refused. That is not a reason to weaken it.

**What is blocked is decisions, and each says on its own issue exactly what has to be chosen:**

- **#182 and #199** need a schema decision. There is no test, no round and no result in `contract/schema.sql`, so there is nothing to build against. Skip them.
- **#303** needs somewhere for a reference number to live, and **#268 is blocked behind it**: `ReferenceLine` has still never rendered with real data.
- **#288** needs a PDF engine, and **nothing in the app can make a PDF**. The engine is #228 on milestone 5, two milestones later. Rule 11 rules out a screen whose only action does nothing. **The owner picks**: move #288 to milestone 5, or move #228 forward.
- **#238** needs a decision on whether a milestone may point at a measure at all, which comes close to interpreting a measurement.
- **#319 and #320** need a direction for the `app_meta` problem: text already stored unnormalized, and a restored phone writing under the source phone's identity. **The merge now depends on this**: `app_meta` is deliberately not merged because of it, which is why #211's "per-section view choices restore" criterion cannot be met.
- **#210's locale question is settled**, 2026-08-10, by precedence rather than by choosing: the data contract outranks the format document, so the format document was corrected to carry the locale, the export timezone and the missing attachment list, all three of which `DATA-CONTRACT.md` 8.2 had always required. **D132, and it is the entry worth reading before escalating anything else of this shape.**
- **#227 blocks the last of #212.** Unicode normalization is a change to every write path and a half applied one is worse than none.
- **#16 blocks #15.** There is no second platform to run the golden vectors against, so they are vectors with one reader.

**Milestone 2 is entirely blocked, and as of 2026-08-10 so is milestone 3**: its five remaining issues each wait on the owner or on #16. **Milestone 4 is where the work is**, twelve issues as of 2026-08-12 at 03:30, counted with `gh issue list --milestone "4. The rest of the v4 conversion" --state open` rather than remembered. **Two of them wait on a missing write path rather than on judgment**, #353 and #358, and **#57 and #203 wait on the owner**. The rest is work: #345 the fidelity pass, #343 restore from the unrecoverable screen, #46 the links that go one way, and the five `v4 screen` issues #204 through #208.

## 10. The phone

### The device changed on 2026-08-12. Read this before any device work.

**The Pixel 10 Pro XL is gone and the Pixel 8 arrived at 13:30**, with nothing installed on it. **It came up authorized, unlocked and empty**, and `adb` sees it as `39151FDJH00506`.

**What was on the old phone when it went away**, so nothing is mistaken for a fresh state later:

- **The current debug build**, installed from `android/app/build/outputs/apk/debug/app-debug.apk` at commit `8027d2c`, with the disclaimer accepted.
- **The month six fixture**, seeded by `tools/device.sh`, plus **the things tonight's walks wrote into it, none of which is real**: a question attached to Lisinopril, an appointment renamed to "Doctor, follow up X", an incident renamed to "Bruise on her arm nobody could e Xxplain", and earlier, on a since replaced empty notebook, a care thread called "The lease with her landlord". **Every one of those is a walk artifact and none of it needs recreating.**
- **A view preference file**, `health-trail-views`, holding the appointments and documents view choices. `AppointmentsMonthTest` writes the list view back around itself, so it should read agenda.
- **Its accessibility settings at the values they started at**, read back rather than assumed: font scale 1.0, animator null, touch exploration 0, no per app locale. **The app's own theme is back on "Follow the phone".**

**What the new phone needs before verification can resume**, in order:

1. **USB debugging authorized** for this machine, and `adb devices` showing it as `device` rather than `unauthorized`.
2. **The serial in this file corrected**, since the line below still names the old one and `docs/TRAPS.md` section 1 refers to it. **No tool hardcodes a serial**, checked: `device.sh`, `seed.sh`, `walk.sh` and `verify.sh` all take whatever `adb` finds, so a single connected device is all they need.
3. **`tools/device.sh`**, which installs the APK, seeds the month six fixture and focuses the app, refusing rather than half succeeding. **It will not accept an APK older than the sources**, so build first.
4. **A full `tools/verify.sh --device`** before anything closes: the last one was **637 green on 2026-08-12 at 16:20 UTC**, and **six commits have landed since with device walks but no full run**.

**The phone is at its baseline as of 2026-08-12 at 18:45**, read back rather than assumed: font scale 1.0, animator 1.0, touch exploration 0, no per app locale, and the app's own theme back on "Follow the phone". **It holds the current build and the month six fixture**, reseeded after the suite uninstalled the app. **It can be unplugged.**

**`tools/screenshot.sh` refuses on this phone until the app's own theme is set.** Its night mode is `auto`, so the script cannot derive a theme from the device and says "Cannot tell what theme the app is in." **Set Appearance to Light or Dark in the app, capture, and set it back to "Follow the phone"**, which is what the old phone never needed. It is not a defect in the script.

**What is owed on the new device**, and none of it is started:

- **A screenshot for #359**, the correcting an incident screen, which rule 12 requires and which the issue says is coming.
- **A document saved with a real photograph in it**, which is the one part of #362 nobody has seen: the picker opens the owner's own library, so the sheet holding an image and the band carried into the later questions exist only in code. `docs/TRAPS.md` section 1.
- **The unfiled Today card**, whose branch is the same code as the incidents card and was never seen, because the month six layout does not carry that card.
- **#360 items 3, 5 and 8**, which are the three workflow defects left. **The next action is item 5**, search opening the result rather than the roster it lives in.

- **Pixel 8, serial `39151FDJH00506`, over USB. The only test device**, connected 2026-08-12 at 13:30 and replacing the Pixel 10 Pro XL, `57241FDCQ0000H`, which is gone. **Android 17, the same platform level the old one ran**, so nothing about `compileSdk 37` or `targetSdk 36` changes.
- **Its baseline is not the old phone's baseline**, and rule 19 says restore what was actually there: **font scale 1.0, animator duration 1.0, touch exploration 0**, all read on arrival. **The animator is 1.0 here where the old phone reported null**, so restoring this one to null would be leaving it changed rather than putting it back.
- **It is the owner's daily driver.** Everything about how it is handled follows from that.
- **No emulator.** Dropped from this project. Do not launch one, do not create an AVD, do not treat its absence as a blocker. D21, D23, B4.
- **Say when it can be unplugged.** The owner waits to be told, and most work needs no device.

**How to drive it, what it does that surprises people, and rule 19's exception with the exact baseline commands: `docs/TRAPS.md` section 1.** That is the section to read before touching it, and it is the only one.

**Two device facts that are policy rather than technique**, so they stay here:

- **Use `zh-Hans` for Chinese, never a bare `zh`.** A bare tag has no script and yields English rather than an error. D52.
- **The share sheet and the calendar app show real contacts. Do not screenshot either.**
- **A password field brings the phone's password manager with it.** Typing a passphrase into the restore screen made Bitwarden offer to save it and put its own bar in the screenshot. **Decline it, and do not commit a screenshot of any screen with a password field.** 2026-08-11.

**The three guards, so nobody re-derives them.** Guard 1 was inert from the day it was written until 2026-08-01 because its hook command was unquoted and this path contains spaces; fixed. Guard 2, the pre-compaction state save, **has never fired and is unproven** and cannot be triggered deliberately, so treat it as absent and keep this file current by hand. Guard 3, the retry cap, is a command line tool nothing calls. D29, D49.

---

## 11. This environment

**Moved to `docs/TRAPS.md` section 7.** Paths, the unset `ANDROID_HOME`, the space in the working directory, the toolchain versions, and why a Gradle release used to turn CI red.

---


## 12. Where everything else is

**Section 1 has the reading ladder. This is the index.**

| Question | File |
|---|---|
| What to do next | Issue #321, then this file |
| What will bite me doing this | `docs/TRAPS.md`, one section, chosen from its table |
| Why something is the way it is | `DECISIONS.md`, D1 through D128. Search it, do not read it |
| What it should look like | `DESIGN.md`. **Three grids**: `reference/screen-grid.html` generally, `projects-grid.html` and `today-grid.html` for those two surfaces. Section 14 is the undrawn-screen map; 20 and 21 are the two new surfaces |
| What the data may do | `contract/DATA-CONTRACT.md`, and `contract/EXPORT-FORMAT.md` for the archive |
| What the archive renders, and how | `contract/readable-fields.json` per column, `contract/readable-vocabularies.json` for the fixed vocabularies, `contract/test-vectors/readable/` for the bytes it must produce |
| What the app is for | `MASTER_SPEC.md` |
| How it gets tested | `TESTING-PERSONAS.md` |
| How a long unattended run stays safe | `RUN-SAFETY.md` |
| Delegation | `AGENTS.md`. Subagents never write anything |
| How something came to be, and what proved it | `docs/RUN-LOG.md`. **History only. Never read it to orient.** |

**Precedence when two of them disagree:** verified code, then this file, then `DECISIONS.md`, then the data contract for data questions, then `DESIGN.md` for visual questions, then `RUN-SAFETY.md` and `AGENTS.md`, then `PROJECT-DELTAS.md`, then `MASTER_SPEC.md`, then the template.

---

## 13. Uncommitted work

**None.** Verified with `git status --porcelain` returning nothing and the push confirmed against `origin/main`, rather than assumed.

**Nothing on `main` is unverified.** #260 was the last one and it has been walked. Section 6 has what the screen showed that the code did not.
