# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-10, after the export learned to look for the files it was about to leave behind.

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

### Verified rather than asserted, 2026-08-10

- The working tree is clean and everything is on `origin/main`. **Check rather than trust**: `git status --porcelain`.
- **19 repository checks pass**, `python3 tools/checks/run_all.py`. The nineteenth is `check_digest_sections.py`, which holds every table the Today digest maps to the change log's own trigger literals in `contract/schema.sql`. It was written because the mapping named a table that does not exist and Progress was missing from the digest for the life of the project. #336. The eighteenth is `check_readable_labels.py`, which holds every table and column the archive renders to a word in all four catalogs, **and since 2026-08-10 also holds a `link` column's declared catalog to `templates/data`**, because a catalog lookup that guesses answers wrongly rather than failing. #329.
- **223 unit tests pass and they need no phone**, and **three of them are the archive's regeneration**, which until 2026-08-09 could only run on the phone. `contract/test-vectors/readable/`. On a day when the device is unreachable this is most of what is left, and it is worth writing logic where these can reach it rather than only into a composable.
- **534 instrumented tests pass**, last full run 2026-08-10 on the unlocked phone. **Nothing instrumented changed after that run**: the digest work of 2026-08-10 is unit tests and checks only.
- **The phone's dark theme is on a custom schedule**, `customStart=17:00 customEnd=06:30`, so `ui_night_mode` reads 2 overnight and 1 after half past six. **A session that records it as a baseline at night and reads it back in the morning will think it changed.** It did not; leave it alone.
- **Continuous integration is green on `main` at the tip.** Check after every push: `gh run list --branch main --limit 3`.
- **The phone was returned to its starting values on 2026-08-10 and unplugged**, each setting read back rather than assumed: font scale 1.0, animator null, `ui_night_mode` 2, no per-app locale so the app runs in English, and the accessibility services string the KDE Connect one. **It holds the month six fixture notebook** with the disclaimer accepted, which is what `tools/seed.sh` leaves, and none of it is real. **Its four attachment files are all present**: two were moved to `files/parked` to reach #335's screen and were moved back, verified by count.
- **The four named failure modes that were open on #212 are down to two**, 2026-08-10. Time, numbers and absence each have their own tests now, `RoundTripTimeTest` and `RoundTripValueTest`, eleven between them. **Unicode is #227 and is a change to every write path rather than a test**, and the four gigabyte half of scale needs a fixture nobody has written.
- **Four more test archives sit in `Download` from 2026-08-10**, beside the ones from 2026-08-09 and 2026-08-04, and every one holds nothing but the fixture. **Three of them use `missing332` and all three are archives the app refuses to open**, deliberately: they were written with two attachment files absent, which is exactly what #332 is about. **The fourth is `catalog329`, it is Arabic, it opens, and it is the one #329 was proved on.** The older ones are `arabic327`, `arabic328` and `arabic328b`, and those open too. A locked file whose passphrase nobody recorded is a file nobody can use.
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
- **THE ARCHIVE is largely built and proved on real hardware**: a two-layer container at format version 3, a readable copy, a standalone decryptor at `tools/decrypt/` tested in CI, and the format published byte for byte in `contract/EXPORT-FORMAT.md`. **The stranger test passes**, run on 2026-08-09 on a laptop that has never had the app.
- **The readable copy is written in the person's language and so are its values**, #327, #328 and #329. Verified by exporting in Arabic, decrypting with the passphrase alone, and reading the pages in a browser: 128 field labels, 39 headings, 81 stored values across 17 vocabularies, money as money, and no bare epoch anywhere. **#329 closed the last of it on 2026-08-10**: a link into a shipped catalog resolves to that entry's name, and the two indexes stopped being printed. **A sweep of a real Arabic archive now finds no raw identifier, no schema token in a `<dd>`, no five digit integer, and no bare `0` or `1`.** D130.
- **The contract now carries four files for the readable copy, not one.** `contract/readable-money.json` is the fourth, added 2026-08-10 for #331: the rules an amount renders by and the ISO 4217 codes whose minor unit is not two digits. **Money is no longer asked of the platform**, because `java.text.NumberFormat` answered differently on Android and on a JVM and 8.5's byte identity was therefore a claim about one phone. D131.

**The older three, unchanged.** `contract/readable-fields.json` is what each column renders as, `contract/readable-vocabularies.json` is the fixed vocabularies those decisions name, and `contract/test-vectors/readable/` is the golden vector. All three are generated into the app by the build, so nothing is hand kept on the Kotlin side.
- **The importer merges as well as replaces**, #211. `Merge` is pure so the rules that decide whose version of a note survives are unit tested without a phone: match by id, later `updated_at` wins, `origin_device` breaks a tie so two phones reach the same answer in either direction, and merge never deletes because removal travels as a tombstone. Every resolution goes to `conflict_log` with both sides whole, and **there is a screen that reads it**. **Two criteria of #211 are not met and say so on the issue**: a missing attachment is #332, whose first half landed on 2026-08-10 and whose second half is a format decision, and per-section view choices are not a thing the schema carries.
- **The export looks before it writes**, #332. `Attachments.all` lists files rather than rows, so a live attachment row whose bytes were gone shipped as a row with no file and the archive could not be opened again, while the screen said "Saved" and nothing else. `Backup.export` now returns the manifest plus what it could not find, **using the same query `ExportContainer.open` refuses on** so the warning and the refusal cannot name different files, and the screen says it under "Saved" rather than instead of it. D129.
- **B4's argument is finally true.** It dropped the emulator because "data survival is proven by the round trip against the golden vectors in continuous integration", and **nothing in continuous integration rendered a readable page at all** until 2026-08-09: `RegenerationTest` is instrumented and `DateVectorTest` reads assets. `ReadableVectorTest` is the half that needs no Android. **Regenerate it deliberately**, `-Dhealthtrail.vector.write=true`, and read the diff.

**Nothing on `main` is unverified.** Every screen that has shipped has been on the phone at both themes, font scale 2.0 and Arabic right to left, **including the two the merge added on 2026-08-09**, which are waiting on the owner's eye rather than on a walk.

**The account of how it got here is `docs/RUN-LOG.md`, and it is history rather than orientation.** Section 6 of that file is 2026-08-09, the day milestone 1 finished: twenty-two issues, ten defects that were invisible in the code and obvious on a screen, four fixture modes that did not exist, and two decisions taken rather than escalated.

### Four things that are true and are not ticked anywhere

Each is said out loud on its own issue rather than counted as done.

- **The care team card's sparse rung** and **the trail spine's gap markers** are not reachable from any seed. Held in `TodayFieldScreenTest` instead.
- **The digest's corrected and removed counts** render, and no seed produces them: the generator's updates land on rows it inserted in the same window.
- **The document card's empty rung** needs a cleared install, which the destructive command guard refuses, correctly.
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

    python3 tools/checks/run_all.py                    # 18 content and contract checks, seconds
    tools/verify.sh                                    # the honest runner, includes lintDebug
    cd android && ./gradlew :app:connectedDebugAndroidTest   # 515 tests, about ten minutes

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

**Nineteen of them, oldest first.** Rechecked against the board on 2026-08-10 with `gh issue list --label needs-design-review` rather than remembered, because a list that is only partly a list is the defect this section exists to prevent.

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

**Seven of these are the Projects surface**, #304, #309 through #313, and #317, and they are the ones that arrived in a single run. **Two are the merge**, #333 and #334. **One is the export's second outcome**, #335, added 2026-08-10. The other nine have been waiting longer.

---

## 9. Blocked, and it does not stop the work

**No machinery is blocked.** B5, the destructive command guard, is **resolved as of 2026-08-07**: it is installed in `.claude/settings.json`, it is live, and it refused a real removal command aimed at the app package. `DECISIONS.md` B5 has the account. **One defect came with it, #323**: it matches prose that merely mentions a blocked verb, so writing certain sentences into this file is refused. That is not a reason to weaken it.

**What is blocked is decisions, and each says on its own issue exactly what has to be chosen:**

- **#182 and #199** need a schema decision. There is no test, no round and no result in `contract/schema.sql`, so there is nothing to build against. Skip them.
- **#303** needs somewhere for a reference number to live, and **#268 is blocked behind it**: `ReferenceLine` has still never rendered with real data.
- **#288** needs a PDF engine, and **nothing in the app can make a PDF**. The engine is #228 on milestone 5, two milestones later. Rule 11 rules out a screen whose only action does nothing. **The owner picks**: move #288 to milestone 5, or move #228 forward.
- **#238** needs a decision on whether a milestone may point at a measure at all, which comes close to interpreting a measurement.
- **#319 and #320** need a direction for the `app_meta` problem: text already stored unnormalized, and a restored phone writing under the source phone's identity. **The merge now depends on this**: `app_meta` is deliberately not merged because of it, which is why #211's "per-section view choices restore" criterion cannot be met.
- **#210's locale question is settled**, 2026-08-10, and it was settled by precedence rather than by choosing: the data contract outranks the format document, so the format document was corrected. D132. The rest of #210's checklist stands. The paragraph below is kept because it records what the question was.
- **The old note on #210, for the record**: `DATA-CONTRACT.md` 8.2 says the inner manifest carries the readable copy's locale and `EXPORT-FORMAT.md` line 181 says it carries `pages` and nothing else. The code follows the format document, **which is the published one and is what `tools/decrypt` was written from**, so which is right is not a session's call. **It stopped being cosmetic on 2026-08-09**: since #327 the readable pages are written in the person's language, so 8.5's byte identical regeneration now depends on a language the archive does not record. Said on #210 and in `RegenerationTest`'s class comment.

**Milestone 2 is entirely blocked. Milestone 3 has two questions in it and is not stopped by them**, since #332's first half and #212's tests can both proceed. Milestones 4 through 6 are buildable without any of these.

## 10. The phone

- **Pixel 10 Pro XL, serial `57241FDCQ0000H`, over USB. The only test device.**
- **It is the owner's daily driver.** Everything about how it is handled follows from that.
- **No emulator.** Dropped from this project. Do not launch one, do not create an AVD, do not treat its absence as a blocker. D21, D23, B4.
- **Say when it can be unplugged.** The owner waits to be told, and most work needs no device.

**How to drive it, what it does that surprises people, and rule 19's exception with the exact baseline commands: `docs/TRAPS.md` section 1.** That is the section to read before touching it, and it is the only one.

**Two device facts that are policy rather than technique**, so they stay here:

- **Use `zh-Hans` for Chinese, never a bare `zh`.** A bare tag has no script and yields English rather than an error. D52.
- **The share sheet and the calendar app show real contacts. Do not screenshot either.**

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
