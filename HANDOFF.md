# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

If you are a session with no memory, this file plus `git log` and the issue tracker is everything you need. Read this in full, then `CLAUDE.md`, then continue only from what the repository says is true.

**Last rewritten:** 2026-08-01, in the session that proved the destructive command guard had never been wired and rebuilt this file's section 5 from the tracker.

If you find yourself re-reading files you already read this session, compaction has happened. Stop, read this file again, and re-orient before continuing.

---

## 1. The thing that changed everything, and it is retroactive

**The owner sent the standing quality bar mid-session.** It is recorded as **D34** and written into `CLAUDE.md` as rules 14 through 21, into `DESIGN.md` as sections 10.8, 10.9, and 10.10 plus additions to sections 1, 6, and 9, and into `MASTER_SPEC.md` section 4.2.

**Nothing in it is forward-only.** Every screen already built, every document already written, and every issue already open comes up to it. A codebase where the standard changed halfway through is a codebase with two standards. Issue **#43** is the retroactive audit and it is not optional.

The parts that change how you work, compressed:

- **No screen ships thin.** Functionally correct and visually plain is not done.
- **Hierarchy before decoration**, in the order `DESIGN.md` 10.8 sets out. Uniform weight is not neutral.
- **Everything the person touches responds.** One press treatment for the whole app, `DESIGN.md` 5.14.
- **Dates are a real model**, EDTF, never falsely precise, always editable, unknown is a first-class value.
- **Links go both ways**, and taps are the currency.
- **Accessibility is a gate**, verified with the settings actually on.
- **Look at it on the phone before closing anything**, then fix the worst thing you find and look again.

---

## 2. Where the work is, exactly

**Phase 0** is substantially built and not closed. Eleven of its issues remain open: #1 the parent, #7, #8, #9, #10, #12, #13, #15, #16, #17, and #18. All are in section 5 with what each is actually waiting on.

**Phase 1** is where the work is. The core loop runs end to end on real hardware: the gold capture button opens the sheet, the sheet opens a form, the form saves, the entry is written, the change log trigger fires in the same transaction, and the notebook's count refreshes through the live view.

**All four screens the owner named as visually thin have been rebuilt** and their issues are closed: the disclaimer gate, setup, the situation picker, and the capture form, plus the notebook table of contents last, #36 through pull request #49. Nothing is in flight.

---

## 3. The precise next action

**The round trip test, and it comes before every other feature.** It is the first thing in section 5 and the two sections agree; if they ever disagree again, section 5 is rebuilt from the tracker and section 3 follows it.

**Why it is first rather than merely next.** B4 dropped the emulator from this project, and the reasoning that made that safe was explicit: data survival is not proven by a long lived phone installation, it is proven by the export and import round trip against the golden vectors in continuous integration. **That test does not exist.** So the argument that retired the emulator currently rests on something unbuilt, and every claim about data surviving an update is unproven. Build it unencrypted if that is what it takes to have it running today.

Then the rest of **#9**, in order:

1. ~~Content addressed attachment storage.~~ **Done.** `Attachments`, with the row side already in the schema.
2. ~~The container.~~ **Done, unencrypted.** `ExportContainer` writes and reads it, and six of the eight section 7 failure cases are covered.
3. **The round trip test**, field by field, asserting the EDTF column survives byte for byte and that the derived range is recomputed on import rather than trusted.
4. **Encryption**, per `contract/export-format.md` section 4. **The dependency question is answered:** the owner decided on 2026-08-01 to keep the format exactly as written, take AES-256-GCM from the platform JCE, and add **Bouncy Castle** `Argon2BytesGenerator` for Argon2id, which is pure Java and needs no NDK. **Do not substitute PBKDF2.** Per D24 the export file is the only recovery path from key loss, which makes it the most security sensitive artifact in the project. Record the Argon2id parameters in the export manifest so older files stay readable and the cost can be raised later. Start from the OWASP baseline and tune only if it measures unusably slow on the phone.
5. **The last two failure cases** of the eight in section 7.
6. **Tombstones travel**, and a test says so. That is the last unmet criterion on #8.

`contract/export-format.md` specifies all of it and is current, including the line added this run about event dates travelling as their EDTF string and the derived range being recomputed on import rather than trusted.

**Then #62**, the template catalog being English only, which is release blocking and which the app currently shows plainly to any Arabic reader.

**Then #43 and #44 worked alongside new screens** rather than saved for a phase gate.

**Then the rest of Phase 1:** Today with the digest engine, the trail, care team, medications, the emergency card, projects, and More.

**Language access is last**, after everything above. Section 5 says why.

**Persona runs happen as their supporting screens land**, not in a block at the end. `TESTING-PERSONAS.md` has thirteen and one has been walked. Section 10 records each with its seed and date.

## 4. What is done, and how each piece was verified

Verified means checked through the mechanism, not inferred from the code being written.

| Piece | How it was verified |
|---|---|
| The schema, 34 user data tables | `tools/checks/check_schema.py` runs it into a real SQLite database on every push and asserts the six contract columns, a `live_*` view, both change log triggers, no AUTOINCREMENT, and that a failing change log write rolls the data write back |
| Locally generated ids | `IdsTest`, a JVM unit test. UUIDv7 with same millisecond sequence bits and backward clock protection |
| Event dates, the EDTF model | `EdtfTest`, 20 JVM tests. Round trips every supported precision, proves a month never collapses to its first day, proves uncertainty never widens a range, proves unknown survives |
| Event dates, as they read | `DateVectorTest` on the phone runs `contract/test-vectors/dates.json`, the shared file, and asserts every precision's string, range, and rendering in all four locales. It also asserts directly that nothing coarser than a day ever renders as its first day |
| The date columns | `check_schema.py` asserts every event date is a full four column group and that no bare `<name>_at` survives on a world event. Both failures were verified by breaking the schema on purpose and watching the check catch them |
| Encrypted database, SQLCipher, key in the Keystore | `DatabaseTest` on the connected phone |
| The repository layer | Every read goes through a `live_*` view. Proven by the instrumented suite writing and counting through it |
| Four locale catalogs, ICU MessageFormat | `check_i18n.py` on every push. `CopyIntegrityTest` on the phone proves no locale silently falls back to English for the disclaimer |
| Contrast in both themes | `check_contrast.py` measures 80 pairs against the actual token values on every push |
| Content compliance | `check_copy.py`, `check_templates.py`, `check_contract_isolation.py`, `check_self_contained.py` |
| Schema migrations | `MigrationTest` on the phone. An upgrade keeps every row, a failed step changes nothing and does not move the version, a database from the future is refused, and steps apply in order and only once. Proven with a synthetic step rather than a fake shipped migration |
| The change log, through Kotlin | `ChangeLogTransactionTest` on the phone, through SQLCipher rather than plain SQLite. Insert, update, and tombstone each append exactly one entry, the entry names the table, the row, and the operation, and a write inside an abandoned outer transaction leaves no orphan |
| The fixture generator | `check_fixtures.py` generates twice and compares bytes, checks a different seed differs, checks all six points grow, checks year five hits its stated scale, and checks the shapes a random generator can miss by chance: every bill state, every project state, both instruction tags, an incident that never resolves, and an attachment exactly at the size limit. Proven to catch drift and two of those gaps by breaking them on purpose |
| The export container | `ExportContainerTest` on the phone. What goes in comes out byte for byte, the manifest survives to the millisecond including tables with zero rows, the manifest is the first entry, and six of the eight files that must fail cleanly each name what was wrong |
| Attachment storage | `AttachmentsTest` on the phone. The same bytes are one file, the streaming and whole-file paths agree, a changed file fails verification, and a half written file is never visible under its hash |
| The date picker | Walked on the Pixel: opened from the capture form, picked August 18 with a time, and the form read back "August 18, 2026 at 2:00 PM" through the same renderer every other date uses |
| A deleted row is actually gone | `TombstoneTest` on the phone deletes through the repository and asserts it leaves every read the app has: the count, the Unfiled tray, the date lookup, the thread chips, and a link table join. It also asserts the row physically survives, because a removed row leaves nothing to tell a peer it was deleted |
| Tombstones cannot leak | `check_live_views.py` fails any read of a base table outside a live view, in app and test sources alike. Proven by three deliberate breakages: a leak inside the repository, a leak on a screen, and an allowance with no reason |
| Every screen built so far | Instrumented, plus built, installed, opened, and looked at on the Pixel |
| The notebook's fold behavior | Walked on the Pixel with a hospital stay template: appointments, the trail, documents, and standing instructions forward, money and progress collapsed, which is exactly what that template names |
| Dynamic type at font scale 2.0 | Every built screen looked at on the phone with the system font at maximum. Two defects found and fixed, both invisible at 1.0. The setting was restored afterward |
| Reduced motion | Verified with `animator_duration_scale` actually set to 0 on the phone, not by reading the code. A press still acknowledges, reaching the same target through a 100ms fade rather than a spring. The setting was restored afterward |
| Arabic on the device | Ran through a per-app locale rather than a system setting. Real Noto glyphs, no fallback boxes, and the whole layout mirrored. It also found that the template catalog is English only, #62, which no check covered |
| The typefaces | Bundled and looked at. Bricolage Grotesque, Atkinson Hyperlegible, JetBrains Mono, Noto Sans Arabic. Every license verified against `google/fonts` rather than assumed |
| The capture sheet, looked at with fresh eyes | Two defects nothing else would have found: "Save a document" closed the sheet and did nothing, and the inherited Material scrim barely dimmed the notebook behind it. D44 and D45 |
| Screen reader labels | `ScreenReaderTest` walks every screen's semantics tree, including a sheet's own window, and fails any touchable node with no text and no content description. Nine screens. It found one on its first run and that is fixed |
| Every screen looked at with the keyboard up | Two defects found that way and nowhere else: the setup button colliding with the last field, and the field clipped mid-box at the scroll boundary |
| The Unfiled tray | Walked on the Pixel end to end: a call saved with no thread, the waiting card appears on the notebook, the tray suggests "Nursing" from the words in the entry, filing it links the thread and clears the tray in one transaction, and the card disappears |
| The press state, everywhere | Measured on the device on three different surfaces: a card row (26,36,43) to (43,50,56), the filled button (127,182,212) to (136,186,214), the capture button (227,177,85) to (228,182,100). `FilledButton` and `TextAction` previously had no press state at all |

**The whole instrumented suite: 141 tests, 0 failures**, run on the connected Pixel 10 Pro XL. **30 JVM unit tests, 0 failures.** All nine implemented compliance checks pass.

**A pattern worth carrying forward.** Almost every defect this run found came from putting the built thing in a hand and changing one condition: the font at maximum, the keyboard up, the language set to Arabic, or simply looking at a screen that had already passed its tests. None of them were visible in the code, and several had passed a review. The tests are what keep them fixed; they are not what found them.

---

## 5. Remaining work inventory, in order

**Rebuilt from the tracker on 2026-08-01.** The previous version of this section had four rows spliced in from section 4's verification table, which put `MigrationTest` text under issue #9 and left three rows with no issue number at all. It also listed #39 as unbuilt while sections 4 and 9 recorded it as built and walked. It was patched too many times and is now derived from `gh issue list` rather than edited in place. **If this section and section 3 ever disagree, rebuild this one from the tracker and make section 3 follow it.**

**Closed in the long run of 2026-08-01**, so a fresh session does not go looking for them: #14 the migration mechanism, #22 the end of life instruction tag, #36 the notebook, #37 setup, #38 the date model, **#39 the date interface**, #40 the press sweep, #41 the situation picker, #42 measurement, #48 the template, #53 the Unfiled tray, #58 the subject scoped counts, and #78 the empty Today. #21, the roadmap, is also closed, and **#12 the fonts**, closed once Chinese was verified rendering from the system face on the device.

**In order. The first is the one to take.**

| Issue | What | Why here, and what it is actually waiting on |
|---|---|---|
| **#9** | **The export container, round trip first** | **First among all feature work.** B4 retired the emulator on the grounds that data survival is proven by the export and import round trip against the golden vectors in continuous integration. That test does not exist, so the claim is unproven. Build it unencrypted if necessary, then encryption with Bouncy Castle Argon2id per section 3, then the last two of the eight failure cases |
| #62 | The template catalog is English only | Release blocking. The app currently shows an Arabic interface wrapped around English content, which any Arabic reader sees immediately. Found by running the device in Arabic, not by any check |
| #43, #44 | The retroactive audit, and the accessibility gate | **Worked alongside new screens, never saved for a phase gate.** Both partly done with findings on the issues. #44's remaining criterion is now reachable: the owner granted permission to enable TalkBack, provided the prior state is recorded and restored exactly |
| #57 | The document capture input | The last of the six ways in. **No longer blocked**: the attachment storage it needed landed with the export's first piece |
| #8 | The repository layer | One unmet criterion: tombstones travel through the export, and a test says so. Falls out of #9 |
| #7 | The change log | Proven through Kotlin. Open only for "the digest reads from the change log", which needs the digest engine |
| #17 | The fixture generator | Everything but the four language variants, which wait on #62 |
| #15 | Golden vectors | `dates.json` exists and runs on the phone in all four locales. The engine vectors need the engine |
| #10 | `SyncTransport` | Needs the export container, so it follows #9 |
| #46 | No dead ends, links both ways | Needs screens that can link to each other, so it follows the trail |
| #47 | Search | Needs Today, which needs the digest engine |
| #45 | Capture from outside the app | Independent of everything above. Widget, quick settings tile, share sheet target |
| #16 | The web scaffold | `npm` is absent on this machine. Nothing else blocks it |
| #13 | The four locale scaffold | Arabic and Chinese are both verified on the device now, and choosing a language actually changes the language, which it did not before D52. What remains overlaps #62 |
| #18 | Content checks in continuous integration | Ten run. Open for the ones not implementable yet, each named in `run_all.py` with what it waits on |
| #25 | The About screen links the privacy policy | Needs an About screen, which does not exist |
| #1 | Phase 0 parent | Closes when its children do |

**In the review queue, waiting on the owner rather than on work.** Eight, and the list had fallen two behind: #28 the disclaimer gate, #30 setup, #32 the situation picker, #34 the capture sheet and form, #50 the notebook, #55 the Unfiled tray, **#68 the date picker**, and **#81 Today's empty state**. **Arabic screenshots are no longer blocked** for any of them.

**Phase 1 feature work still ahead:** Today with the digest engine, the trail itself, care team, medications, the emergency card, projects, and More.

**An in-app theme setting**, system, light, or dark, belongs in More and is a real feature rather than a testing convenience. It also happens to remove a standing dependency: capturing both themes currently means changing the system theme on somebody's daily driver, which is not a session's to do unattended. With the setting, both sets are captured from inside the app and the phone is never touched.

**Language access comes after all of the above**, and it is a body of work rather than a task: ten languages chosen by United States limited English proficiency population, roughly 1500 strings each. **It is language access for caregivers in the United States, not international expansion.** The federal, Medicare, and Medicaid content is specific to this country, so translating for a Spanish speaker in Texas is right and presenting the same app to someone in Spain would be wrong. `MASTER_SPEC.md` carries the distinction and the issues carry the detail. **Do not begin it until everything ahead of it is done.**

**Something that must not survive to release.** The Projects and More destinations render an honest interim screen, Today says plainly that its digest is still being built, and so does the document capture input. That is deliberate rather than a stub: `DESIGN.md` section 5.5 fixes the four destinations and their order, and D44 says an interface may offer something it has not built but may not go quiet when someone takes it up. Each disappears as its destination lands. **If one is still there at release, that is a bug**, and `ShellTags.NOT_BUILT` makes them greppable.

---

## 6. Blocked

**Nothing is blocked.** All four entries that ever appeared in the BLOCKED section of `DECISIONS.md` are resolved and kept there with their outcomes. A fresh session needs nothing from the owner in order to continue.

**Arabic is no longer waiting.** The fonts landed on 2026-08-01 and Arabic renders correctly on the device, so the Arabic screenshots on the design review issues can be captured whenever someone works through them.

**The light theme screenshots are no longer waiting.** The phone was found in light theme on 2026-08-01, so the full set of 28 was captured then. **The dark set is now the one that is short**, and it is deliberately not being solved by flipping the owner's daily driver: the in-app theme setting in section 5 removes the dependency entirely, and the dark captures happen through it.

---

## 7. The phone

**Before anything else, prove the guard, because nothing below is protected without it.** Run `git reset --hard HEAD` on a clean tree and `adb shell pm clear com.kamsiob.healthtrail`. Both must be refused with "Blocked by the Health Trail destructive command guard". **If either runs, stop and fix the wiring before any other work.** Full procedure and what to check first in `RUN-SAFETY.md` section 1.1. Record the result in D49 whichever way it goes.

**Guard 1 was inert from the day it was written until 2026-08-01**, and looked installed the whole time. Its hook command was unquoted and this project's path contains spaces, so the shell split it, the executable was never found, and the hook exited 127 instead of 2. Nothing blocked. D29 blamed session start timing, which was wrong; D49 has the real cause and the fix. **The fix is committed but was not live in the session that made it**, because configuration is read at session start, so that session ran to its end on rule 6 alone.

**Guard 2, the pre compaction state save, has never fired and remains unproven.** Same unquoted path defect, now fixed, but it cannot be triggered deliberately: compaction happens when it happens. The evidence will be a commit in `git log` at a compaction boundary that no session remembers making. **Until such a commit exists, treat it as absent and keep this file current by hand.** Do not record it as working on the strength of the fix looking right, which is exactly the mistake D29 made.

**Guard 3, the retry cap, is a command line tool nothing calls.** `.claude/hooks/retry-guard.py attempt <label> "<what>"` before a second try at the same thing. No session has ever run it. Not miswired, just unused, which reaches the same place.

- Device: Pixel 10 Pro XL, serial `57241FDCQ0000H`, connected over USB. **The only test device.**
- **No emulator.** Dropped from this project. Do not attempt to launch one, do not create an AVD, and do not treat its absence as a blocker. See D21, D23, and B4 in `DECISIONS.md`.
- **The phone's theme is not fixed and must be read, never assumed.** It was dark through 2026-07-31 and is **light** as of 2026-08-01. `tools/screenshot.sh` reads it from the device and names the file accordingly, per D31, so do not pass a theme argument and do not assume the suffix. Check with `adb shell cmd uimode night`.
- **To run the app in one language without touching the phone's own settings**, which matters because this is the owner's daily driver: `adb shell cmd locale set-app-locales com.kamsiob.healthtrail --locales ar`, and `--locales ""` to clear it. Doing this found #62 within a minute, and later found that Chinese did not work at all. **Use `zh-Hans` for Chinese, never a bare `zh`**: a bare tag has no script, and getting it wrong yields English rather than an error. D52.
- **Accessibility settings used during this run were restored to exactly what the phone had before.** `font_scale` back to 1.0 and `animator_duration_scale` deleted rather than set to 1.0, because it was unset to begin with. Check all of these if a run ends unexpectedly:

      adb shell settings get system font_scale                    # expect 1.0
      adb shell settings get global animator_duration_scale       # expect null
      adb shell settings get global heads_up_notifications_enabled # expect 1
      adb shell settings get secure enabled_accessibility_services # expect the KDE Connect string, not TalkBack
      adb shell cmd locale get-app-locales com.kamsiob.healthtrail # expect []
- **TalkBack may now be enabled, and the owner granted that explicitly on 2026-08-01.** It supersedes D43's blanket avoidance. The condition is the same one that governs font scale and animation duration: **record the prior value, restore it exactly.**

  Before: `adb shell settings get secure enabled_accessibility_services` and `adb shell settings get secure accessibility_enabled`. On this phone the prior value is `org.kde.kdeconnect_tp/org.kde.kdeconnect.plugins.mousereceiver.MouseReceiverService`, which is KDE Connect and **not** TalkBack, so restoring means putting that exact string back rather than clearing the setting.

  **If a run ends with TalkBack still on**, which is the risk D43 was right about, turn it off with:

      adb shell settings put secure enabled_accessibility_services org.kde.kdeconnect_tp/org.kde.kdeconnect.plugins.mousereceiver.MouseReceiverService
      adb shell settings put secure accessibility_enabled 0

  On the phone itself it is Settings, Accessibility, TalkBack, or holding both volume keys for three seconds if that shortcut is on.
- **The app on the phone is the app in the repository.** Installed from the head of `main` and launched. Version 0.1.0. All four destinations walked: Today coaches, the notebook lists its twelve sections, and Projects and More say honestly that they are not built.
- **It is a fresh install sitting at the disclaimer gate.** The last `connectedAndroidTest` run uninstalled it and took its data, which is normal and is why the gate is showing. Nothing on it is worth preserving. **This is also the sanctioned way back to first-run state**: run the instrumented suite rather than reaching for `adb uninstall`, which is on the blocklist. D50.

**The one operational rule about the phone.** `connectedAndroidTest` uninstalls the application and takes its data with it. Before running it, if the phone holds anything worth keeping, export through the app's own export feature first and reimport after.

---

## 8. This environment, so a fresh session does not rediscover it

**Two mistakes this run made, both worth not repeating.**

**An edit that replaces text must assert it matched.** Nine decision entries, D39 through D47, were written and none reached `DECISIONS.md`: the anchor they all targeted had been consumed by an earlier edit, so every one of them matched nothing and reported success. They were restored from the commit messages that quoted them, which is the only reason the content survived. A silent no-op is worse than an error, because the work continues on top of a record that is not there.

**Create the branch as the first action of an increment, before a single file is touched.** Not at the point of committing, and not by checking afterward. This went wrong twice in one run, the second time after the rule had already been written down, which is why the fix is mechanical rather than a reminder: a branch made before the work cannot be forgotten after it. One commit reached `main` directly because the branch was assumed from a `checkout` several steps and one merge earlier. Every way of undoing it is a command rule 6 forbids, so it stayed. D48.

**`CLAUDE.md` in a session's context is the copy from session start, and edits to it during the session do not reach that copy.** Found on 2026-08-01 by reading the file from disk: it carries 21 rules, and the copy loaded into the running session carried 13. Rules 14 through 21, the standing quality bar, were added mid-run and are binding, on disk, and invisible to the context that is supposed to enforce them.

**This matters because `CLAUDE.md` says of itself that it is "the last thing to survive context compaction."** That is true of the copy loaded at session start. It is not true of anything added afterward, which survives only as ordinary conversation and is exactly what compaction discards. **After editing `CLAUDE.md`, read the rules back from disk rather than trusting the copy in context**, and treat a rule added this session as one a compaction can lose.

The same shape as the hook defect in D49: configuration read once at startup, edited later, and believed to be live.

**The shell does not carry state between tool calls.** Every command starts fresh.

- **`ANDROID_HOME` is not set.** The SDK is at `/home/Kamsiob/Android/Sdk`. Gradle finds it through `android/local.properties`, which is gitignored and **does not exist in a fresh clone**. Recreate it: `sdk.dir=/home/Kamsiob/Android/Sdk`.
- **`adb` is not on the PATH.** It is at `/home/Kamsiob/Android/Sdk/platform-tools/adb`. `tools/screenshot.sh` resolves it itself.
- **The working directory contains a space and two leading dashes.** Quote every path.

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. It refuses to capture unless the app is the focused window, checked before and after, because this is the owner's daily driver phone. It reads the theme from the device and refuses an argument that disagrees, per D31. Do not pass a theme. **It also switches heads-up notifications off for the duration and restores them through a trap**, because focus is not enough: a heads-up notification never takes focus, and one put the owner's phone number and a contact photo into a capture on 2026-08-01. D53. **Look at every image before committing it.** The script is a control and it is not the last one.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**A trap in the Compose test API, found the hard way.** `performScrollToNode` walks a lazy list a viewport at a time and gives up when it thinks it can go no further. It got that wrong for the Arabic catalog, stopped two rows short, and reported the rows as absent when they were only further down. **Scroll by the list's own item key instead**, with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` rather than on a surface around it: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Continuous integration.** The workflow triggers on `push` to main, on `pull_request`, and on `workflow_dispatch`. Pull request events stopped firing part way through 2026-07-31 and **are firing again as of 2026-08-01**. If they stop again: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>`. **Do not read an absence of checks on a pull request as a passing build.**

**Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools`, or `contract` without touching this file. It caught pull request #49. "README.md describes the screens that exist" fails any pull request that adds or removes a file under `ui/screens/` without touching `README.md`, which exists because the front page claimed the app had one screen for a week after it had nine. "Every screenshot the README points at exists" catches a rename. Rewrite the documents in the same commit as the work, not afterward.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**Everything else:** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

**Verification.** `tools/verify.sh` is the honest runner: it captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. `python3 tools/checks/run_all.py` runs the nine content and contract checks alone. **Never chain a commit on a grep of output.**

---

## 9. Screens built without a mockup

Every screen built without one is composed from existing components under `DESIGN.md` section 10, ships complete with every state, and is logged in three places at the moment it is built: a `needs-design-review` issue with a device screenshot, an entry in `DESIGN.md` section 8, and a line here.

| Screen | Built | Issue | Reviewed |
|---|---|---|---|
| Disclaimer gate | 2026-07-31, rebuilt same day to the 10.6 bar | #28 | not yet |
| Essentials first setup | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #30 | not yet |
| Situation picker | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #32 | not yet |
| Capture form, four kinds | 2026-07-31, rebuilt same day to screen 26 | #34 | not yet |
| Notebook table of contents | 2026-08-01, rebuilt to the 10.6 bar | #36, review on #50 | not yet |
| Unfiled tray | 2026-08-01 | #53, review on #55 | not yet |
| Adding a measurement | 2026-08-01 | #42 | not yet |
| The date picker | 2026-08-01 | #39, review on #68 | not yet |
| Today, the empty state | 2026-08-01 | #78 | not yet |

The notebook is drawn in the reference file, so it is listed here as a correction rather than as an undrawn screen. `DESIGN.md` section 3 item 8 records the four ways the built screen departs from the mockup, with reasons.

**Known ahead:** the template library, the four template pickers, the template detail view, and the template editor. None is drawn. `MASTER_SPEC.md` section 4.10 carries their requirements.

---

## 10. Persona runs

### P1, day one in a hallway. Walked 2026-08-01 on the Pixel, fixture: fresh install, no seed

**Four of the five things P1 says must be true are true. One is not.**

| Must be true | Result |
|---|---|
| The disclaimer appears and requires explicit acceptance | Yes. Nothing else is reachable until it is accepted |
| Setup asks for three things and lets everything else wait | Yes. Every field skippable, and skipping produces a working notebook |
| The empty Today coaches rather than sitting blank, first suggestion the Emergency Card | **Now yes.** It failed on the walk, which is how issue #78 came to exist, and it was built the same day |
| A first call can be logged in under thirty seconds from cold launch | **Six taps and no typing**, skipping everything optional: accept, skip setup, "Not sure yet", the capture button, "Log a call", save. Comfortably inside thirty seconds for anyone |
| Nothing asks for an account, an email, or a permission not needed yet | Yes. Nothing anywhere |

**What P1 says to watch for, and what was seen:**

- *Onboarding that cannot be completed one-handed.* Every primary action is in the lower half. The capture button is centered above the navigation bar.
- *A keyboard covering the field being typed into.* Fixed this run, D38. It was real on setup and is not any more.
- *A required field that should be optional.* None. The whole path can be walked without typing a character.
- *A screen that assumes the person already knows the facility's name.* Setup's "Where are they right now" hints "The ward, the building, or just the town", and it is skippable.

**Worth recording beyond the checklist:** answering "Not sure yet" to the situation picker produces a notebook with no care threads, and the capture form then drops the thread question entirely rather than showing a question whose only answer is "not sure". The entry is not marked unfiled, because with nothing offered, not choosing is not the person declining to say. That behaved correctly without being specifically tested for.

**Not walked yet:** P2 through P13. P10 through P12 need #62, since the template catalog is English only and a language persona against English content tests nothing.

---

## 11. Open questions

The six in `MASTER_SPEC.md` section 10. Three are decided and recorded, three are not yet forced:

1. Tombstone retention window. **Decided: 730 days**, D11.
2. Whether the change log is exported. **Decided: yes, and the importer renumbers it**, D12.
3. Attachment size and count limits. **Decided: 25 MB each, a warning at 4 GB total, no hard ceiling**, D13.
4. Whether the web scaffold uses the same UI toolkit or a minimal one. Not forced until #16.
5. PDF pagination for very large exports. Not forced until Phase 5.
6. How the app describes its own translation status honestly. The catalogs carry `reviewed_by_native_speaker: false` and `check_i18n.py` prints it, so the app can say so plainly. The wording is not written and it affects the store listing and the README.

---

## 12. Subagents

`AGENTS.md` defines four. **Subagents never write anything**, per `CLAUDE.md` rule 8. None was used this session.

---

## 13. Uncommitted work

**None.** Everything described here is committed and merged on `main`. Verified with `git status --porcelain` returning nothing and `git branch --show-current` reading `main`, rather than assumed from the last branch this file happened to mention.
