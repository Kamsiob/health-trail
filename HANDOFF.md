# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

If you are a session with no memory, this file plus `git log` and the issue tracker is everything you need. Read this in full, then `CLAUDE.md`, then continue only from what the repository says is true.

**Last rewritten:** 2026-08-02, and updated continuously through the run that began that evening. Sections 0a, 3, 4, and 6 are current to the last commit.

**Work in progress is on a branch and pushed.** Every increment is committed and pushed as it lands, per rule 7, so nothing is only on this machine. The phone holds a build matching the work.

**The single most important thing this run found:** every export the app had ever written could only be opened by the phone that wrote it, which meant the only recovery path from key loss did not exist. It is fixed, and D61 explains why no test caught it.

**The phone holds a current build with a seeded notebook**, per D56. If an instrumented run has just wiped it, reinstall and reseed before doing anything else.

If you find yourself re-reading files you already read this session, compaction has happened. Stop, read this file again, and re-orient before continuing.

---

## 0a. The guard question is answered, and it is now waiting on the owner. Do not probe it again

**Read this instead of section 0 and section 7's opening, both of which describe the state before the answer.**

**Answered 2026-08-02 at 20:52, first thing in a fresh session.** The probe was run as instructed and the result is not what the last three sessions were expecting.

**The guard did not fire, and this time that is a measurement.** The log gains a line for every command the guard inspects, passed or blocked. Five ordinary commands were run in this session and the log gained nothing from any of them.

**The bigger finding.** The two lines the log has ever held were both written by running the script by hand during the fix, not by Claude Code invoking the hook. So the honest statement is not that the guard broke. **This project's destructive command guard has never been observed to fire through Claude Code, on any day, in any session.**

**The hypothesis the last three sessions carried is dead.** It was that a session cannot benefit from a hook configuration it edits itself, which predicted the guard would work from the next session's first command. This was that session. It did not.

**The script is fine and that was checked again.** Piped a real payload in, it printed the refusal, exited 2, and logged. The defect is entirely in the wiring.

**The fix is known and the agent is not allowed to make it.** Claude Code refused the edit to the hook script, because it does not let a session modify the hooks that constrain it. That refusal is correct and was not worked around. **It is B5 in `DECISIONS.md`, written for the owner in steps he can act on**, and it is the only blocked item in the project. D64 has the full account.

**So: do not spend fifteen minutes on this again.** The next session's probe is worth running only after B5 is done, and then it is one line, `cat ~/.claude/health-trail-guard.log`. Until then the answer is known.

**What actually protects the work** is rule 6 followed by hand, which has held through three long unattended runs, and Claude Code's auto mode classifier, which is what refused the destructive commands each time. **Neither is this project's guard.** Nothing destructive was run in this session.

---

## 0a-old. The guard was tested on 2026-08-01 at 22:31, and it failed. Kept for the record

**Superseded by 0a above.**

The probe was run first thing, as instructed. **The guard did not fire.** D49 carries the full account and D56 and D57 carry two standing rules the owner set during the same run.

**The two commands section 0 names were both refused by Claude Code's own auto mode classifier rather than by this guard**, so neither tested what it was meant to test. **A refusal arriving from somewhere else is not evidence about the guard.** The test that answered it was `git restore --version`: on the blocklist, harmless if it runs, dull enough that the classifier allowed it. It ran.

**`CLAUDE_PROJECT_DIR` is empty in a session's shell.** The quoting fix was real and insufficient: an unset variable yields an unusable path exactly as surely as an unquoted one. The hook path is now **absolute, with no variable in it**.

**The guard now logs every invocation** to `~/.claude/health-trail-guard.log`, blocked or passed. This is the change that matters. Three times this project has been unable to answer "did the guard run", because a guard that does not fire produces exactly as much output as a guard with nothing to do.

**So the test for the next session is now a one liner, and it comes before the two commands:**

    cat ~/.claude/health-trail-guard.log

**If it has no line stamped inside your session, you have no guard**, whatever the configuration says. Run the two commands in section 0 anyway to confirm, and record the outcome in D49 either way.

**What protected the phone through this run** was the auto mode classifier, not this project's guard.

### Checked again at the end of the long run, 2026-08-02 06:23

`cat ~/.claude/health-trail-guard.log` holds **two lines, both stamped 22:31 on 2026-08-01**, which are the two from the fix itself. **Nothing from the eight hours after it.** So the guard did not fire once during the whole run, and the absolute path plus logging did not change that within the session that made the change.

**The most likely reason is mundane and should be tested rather than assumed:** `.claude/settings.json` is read when a session starts, so a session that edits its own hook configuration is the one session that cannot benefit from it. That predicts the guard works from the *next* session's first command. **Predicting is not testing.** The next session must run `cat ~/.claude/health-trail-guard.log` first, then a blocklisted command, then read the log again, and record the outcome in D49 either way.

**Do not write "the guard is live" anywhere without a log line stamped inside your own session.** That error is D29 and it has now been made twice.

**Nothing destructive was run in this session.** Rule 6 was followed by hand throughout, and the only device actions were installs, instrumented test runs, and removing two test export files this session created in Downloads.

---

## 0. Read this before you trust anything about safety

**All three run safety guards were decorative until 2026-08-01, and the repository said otherwise for a week.** D29, D49, D50, D53.

The destructive command hook interpolated a path, this project's path contains spaces, the shell split it, the executable was never found, and the hook exited 127. **A blocking hook has to exit 2**, so 127 read as "nothing to say" and every destructive command ran. The pre-compaction save had the identical defect. The retry cap turned out not to be a hook at all but a tool nothing ever called.

None of that produced a single line of output, which is the whole problem: **a guard that does not fire looks exactly like a guard with nothing to do.**

**The quoting is fixed and a check in continuous integration now fails on it.** What is not proven is that the hook fires in your session, because a fix made mid session does not take effect in that session. **Prove it first, before anything else:**

    git reset --hard HEAD          # on a clean tree
    adb shell pm clear com.kamsiob.healthtrail

Both must be refused with "Blocked by the Health Trail destructive command guard". **If either runs, fixing that comes before any other work.** `RUN-SAFETY.md` section 1.1 has the full procedure. **Record the result in D49 either way, including a pass**, because that entry is the only place the outcome of this test is written down.

Guard 2 has still never fired and cannot be triggered on demand. Treat it as absent and keep this file current by hand.

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

## 2a. Two standing rules the owner set on 2026-08-01, both binding

**One current build of the app stays installed on the phone at all times.** D56. He went to use it and it was not there: the previous session ended on an instrumented run, and `connectedAndroidTest` uninstalls the app. **The phone is where he tests, and he cannot test an app that is not installed.** So an instrumented run is followed by a reinstall in the same increment, before anything else is picked up, and a session never ends with the app absent. The old rule about exporting first was about not losing data and missed the more basic thing.

**Another application of his on the same phone is out of bounds entirely**, and is deliberately not named in this repository. D57. Enumerating a shared device surfaces things that are not this project's, and the right response to seeing them is to stop looking.

**A third instruction governs the languages.** D58: the translated languages ship with a friendly disclaimer on the language selection screen rather than behind a native speaker gate, English never carries one because it is authored rather than translated, and the translations get a good faith check at the very end. #102 is closed and #109 carries the disclaimer. **#62 lost `release-blocking`**: the app ships in English, and the rest of language access sits behind the app itself.

## 3. The precise next action

### Start here. Rewritten 2026-08-03 at 13:52, at the start of the run that builds the component library

**Read this section and "Where to pick up" below, then start. Everything else in this file is context you can reach for when you need it.**

#### What this run is about, and it changes what every screen is built from

**The owner used the app and named the problem: it is uninspired, everything on every screen is the same shape, and the cause is that this app has exactly one component.** A full width rounded card containing text. Grouping twelve of them under headers is organization, not hierarchy. **D71** carries the diagnosis and **`DESIGN.md` section 11** is the answer: ten components and five layout patterns, each with its geometry, its states, and the clause section 5 always omitted, **when to use it and when not to.** The old section 11 became section 12. **`CLAUDE.md` rule 22** carries the short form, and it was added mid-run, so it is one a compaction can lose, per section 8 of this file.

**The bans in section 1 all stay.** The other failure mode is the one to watch now: avoiding generated-looking design does not mean avoiding design. Plain is not a virtue here and crude is not authenticity.

**Read section 11 before touching any screen.** Everything below is built from it, and **#144** is the retroactive sweep of everything already built.

`main` is current and green. Continuous integration passes on it. Nothing is uncommitted and no branch is ahead of `main`: the last commit is the navigation bar icons. **D69 is the rule that made that true and it is new: an increment ends when `main` contains it, and `git rev-list --count origin/main..HEAD` reading zero is the check.** The whole of the previous night sat on unmerged branches because D48 only guarded the start of an increment.

**What this run was about, in one sentence:** the fixture generator learned to write the tables the app had grown into, and almost everything below was found by looking at a screen with real data on it for the first time.

**The eight defects that came out of that**, all fixed, all on `main`:

1. **The app crashed** opening any medication whose history held a `noted` event. No catalog defined the key and it was built by interpolating a database column.
2. **The medication picker wrote a word the database rejects**, `restarted` where the schema says `resumed`. Three layers agreed with each other and disagreed with the CHECK constraint.
3. **A stopped medication claimed to be on the emergency card**, which is the one claim in this app whose cost is outside it.
4. **An untitled entry led with a stock phrase** at display size while the sentence the person wrote sat below it in body text. Most entries are untitled.
5. **Removing an untitled entry asked "remove what?" with a blank.**
6. **Records were named with the button that made them**: "Log a visit" appeared wherever an untitled visit was listed, an imperative sitting where a description belongs.
7. **Waypoints drifted out of line with their rows** on any card shorter than the trail's.
8. **The spine stopped mirroring in Arabic** the moment its node moved into a canvas, and it looked correct.

**The phone is clean.** `font_scale` 1.0, `animator_duration_scale` deleted, app locale back to `en-US`, the app installed and holding a year-five fixture, which is generated data and not the owner's. `/tmp/restore-a11y.sh` exists if a setting is ever left changed.

**One thing needs the owner and only the owner: B5**, the destructive command guard, in the BLOCKED section of `DECISIONS.md`. It has never fired. Rule 6 is being kept by hand.

---

**Rewritten 2026-08-02 after the long run that opened the notebook.** What is below the horizontal rule is the older export work and it is still accurate; what is here is what changed.

### What happened on the night of 2026-08-01 into 08-02

**The notebook opened.** `onOpen` was an empty lambda: twelve sections with live counts and not one of them opened. Capture wrote entries and nothing anywhere gave one back.

**All twelve sections now open onto real screens**, and **all six capture inputs are built**, which closed #57 and removed the last `NOT_BUILT` screen except More's note about what is still coming.

| What landed | Notes |
|---|---|
| The trail | Built to `DESIGN.md` 5.2, dashed route and kind-colored nodes, after first being built as a plain card list and rejected under rule 14 |
| Care team, medications, appointments, money, documents | Each with its own way in, since none could be created before |
| Ask next time, standing instructions | Both were counting zero while the thing they count was being captured |
| Care threads, progress, chapters | Read-only sections the notebook counted and would not open |
| **Projects** | The whole destination said "not built". Sixteen catalog processes with their ordered steps |
| **About** | Closed #25, the oldest open issue. Links the canonical privacy policy |
| Removing and correcting | Long press removes anything, a tap corrects it, everywhere |
| **The system back button** | It left the app from every screen above the notebook. That was the worst defect found all night |
| Arabic headings | Every Display L title broke mid-word, in every language using a connected script, since the type scale was written |

**The pull request is #112**, on branch `fix/guard-observable`, and every increment in it is a separate commit.

### What happened on the morning of 2026-08-02, continuing the same run

**Restore was built and the round trip closed on the phone.** Exported with a
passphrase, added a care team row named SHOULD NOT SURVIVE afterward, restored,
and the row was gone with the original two people back. Five honest states,
including a wrong passphrase that leaves the field in place to try again.

**Then the export turned out not to be portable, which was the serious find of
the run.** `Backup.export` copied the SQLCipher file exactly as it sits on disk,
and that file is keyed by 32 random bytes wrapped by this phone's Keystore,
which cannot be exported and does not travel. **Every export written before this
was unopenable on any other device.** D24 makes the export the only recovery
path from key loss, so the one scenario it exists for, the phone being gone, was
the exact scenario in which the file was unreadable. Every round trip test
passed throughout, because all of them restore onto the same device where the
key never changes. The bug lived in the gap between what the tests exercised and
what the file is for.

The archive now carries a plain SQLite database produced by `sqlcipher_export`
inside a transaction, so the whole schema travels rather than being redeclared
against D16, and restore keys the result with the receiving device's own
passphrase. `PortabilityTest` checks the payload's first sixteen bytes for the
SQLite magic, checks an encrypted export decrypts to one, and checks the live
database is *not* one so the first check cannot pass by becoming vacuous.

**The export passphrase was on screen in plain text**, found by walking the
screen rather than reading it. `KeyboardType.Password` selects a keyboard and
conceals nothing; masking is a visual transformation and has to be asked for
separately. Both fields are concealed now with one control to reveal them,
cleared the moment the file exists, and the result replaces the form instead of
sitting under two still live buttons.

**Today's digest is built, which closes the last thing the app admitted was
unbuilt.** `Digest` is pure, takes change log rows and a timestamp, and has ten
JVM vectors. The counting rules it settles are in its own documentation. Each
row opens its section and each coached step opens what it names and disappears
once taken. **`LastVisit` marks a visit once per process**, which it did not at
first: it was once per composition, so a theme change or rotation advanced the
mark mid-visit and the digest went blank. Found on the phone, with a freshly
seeded notebook reporting nothing at all.

### What happened on the night of 2026-08-02 into 08-03

**Rewritten to current truth at the end of the run rather than appended to**, per this file's own rule. The order below is what a fresh session needs to know, not the order it happened in.

#### The guard is answered and handed over

**It has never fired through Claude Code, on any day, in any session.** Section 0a has the whole account. The fix needs the owner because Claude Code refuses to let a session edit its own hooks, which is correct. **B5** in `DECISIONS.md` and **#128**. Rule 6 followed by hand throughout, as it has been for three runs.

#### The pattern the whole night turned on

**Four separate tools reported on something other than what they were asked about**, and each was silent about its own blind spot:

| The tool | What it said | What it was looking at |
|---|---|---|
| The destructive command guard | Nothing, which reads as a clean run | It was never invoked. D64 |
| The interface suite, 130 tests | Every screen passes | One screen at a time, no shell, no back button. D65 |
| `grep` and `git grep` | No match for a call that exists | Not the file, which two NUL bytes made binary. D66 |
| `uiautomator dump` | Twenty four reader stops | The view tree, not the semantics tree. D68 |

**Distrust a negative result from a tool that cannot tell you what it did not examine.** That sentence is the most valuable thing this project learned tonight.

#### What that produced

**`TESTING-PERSONAS.md` section 7 is the shortcut rule**, written from the audit D39 asked for. Screen tests and journey tests are different tests and this project needs both; every screen owes at least one journey that reaches it. **`BackJourneyTest` found a real defect on its first run.**

**`check_text_sources.py`** fails any source file that grep cannot read. It caught its own author twice within the hour.

**`ReaderStopsTest`** asks Compose for the merged semantics tree rather than asking the window manager, which closes the countable half of #44.

#### The export

**There is no unencrypted export, as of format version 2.** D67. Making the payload portable is what made a plain one dangerous, so the decision that was right at version 1 is wrong at version 2. The importer refuses by what a file is rather than by which version wrote it, and names what it refused.

#### The visual system

**`DESIGN.md` 5.2 is a system in five parts now and it is built**: waypoints with states, routes that are a color and a dash together, spines, distance markers, and the empty state drawings. Plus tabular figures everywhere. **The diagnosis is in 5.2 itself**: section 1's bans were right and nothing replaced them, so every screen converged on a card with text in it.

#### The app became usable end to end

| What landed | Why it mattered |
|---|---|
| **Search**, ten sections, grouped in notebook order | A years long notebook is unusable without it. #47's universal half |
| **Incidents as threads** | `MASTER_SPEC.md` 4.7. It was an entry with a scary kind: reported once, never followed, never resolved |
| **One entry, read on its own** | Nothing could open one. A trail row's only tappable part was its date |
| **A person, and every call involving them** | Section 3 promised it since Phase 0 and `entry_person` had no writer |
| **A care thread, a chapter, a medication** | Each is a place the route led nowhere |
| **The prep sheet** | 4.5, and the app's most useful two minutes |
| **A shareable document** | 4.9. Generated locally, handed to the share sheet, legible to somebody who has never seen the app |
| **Draft preservation and dictation** | The two friction fixes that matter most on the capture form |

**Six lists learned the same rule: a tap opens the thing itself.** Each previously opened either nothing or the form for correcting itself.

#### The fixtures unlock the personas

**`tools/fixtures/pack.py`** wraps a generated fixture in a real export container and it goes in through the app's own restore screen. **D61's portability fix is what made that possible**, used in the other direction. It found journey six broken at the last step within minutes, and three fixture defects that were invisible in a database file and obvious on a screen.


**A standing instruction can record the times it was not followed**, which is P4's stated gap and the part of the record a family actually needs in a room. `instruction_violation` sat in the schema from Phase 0 with no reader and no writer.

**"We asked in writing in March, and it happened again in May and again in June" is a different conversation from "we asked in March."**

**A count, and immediately under it the sentence saying what it is not.** `MASTER_SPEC.md` 4.11 requires that line every time a count like this is shown, and the two are one thought: a bare number would be the app implying a conclusion it is not entitled to. **Zero says nothing at all**, because a count of nothing is not a finding and printing it would turn every instruction into a scoreboard with most of the scores at zero.

**The link to the bill or incident it broke is optional and stays that way.** Somebody writing this down in a corridor knows it happened; working out what it belongs to is a later, calmer job, and asking now is how the thing never gets written down.

### Where to pick up

**Rewritten 2026-08-03 at 13:52, and this is the owner's order for the run rather than the previous night's.** Each one is a whole increment.

1. **`DESIGN.md` section 11, the component library. Done**, and it came before any screen was touched deliberately.
2. **The icon set. Done.** Twenty three drawings: thirteen sections, four destinations, and six capture kinds, of which two are deliberately not new drawings because a captured question lands in Ask next time and a captured document lands in Documents. **Four section drawings were changed and none of them looked wrong on its own**: medications, money, the trail, and projects each shared a silhouette with something else, which does not matter in a list and is fatal in a grid. `DESIGN.md` 5.12.1. **`tools/icons/sheet.py` renders the whole set from the app's own paths** at 44dp, 32dp, and 20dp, and both collisions were invisible in the source and obvious on the sheet. `docs/icon-set.png` is the current sheet.
3. **The capture sheet and the form behind it. Done.** Two new components came out of it and both are in `DESIGN.md`: **the capped chip group with its full set behind search**, 5.11.1, and **the disclosure**, 5.18. The dense row, 11.3, is built and is what the picker's list is made of.

   **What the form looks like now.** What happened comes first and used to come last, which is rule 15 backwards in the same way the entry screen was. Then when, then who with five person chips picked by **who the person has been dealing with lately** rather than who was added first, `Repository.peopleByRecentUse`. Everything else is behind "Add more". On the year five notebook that took the form from twenty three controls to two fields and ten chips, and the whole thing now fits on one screen with the save button still pinned in the lower half.

   **The line saying an entry with no thread goes to the Unfiled tray stays outside the disclosure**, because for somebody who never opens it that sentence is the only thing telling them where their entry is going.

3a. **The old item, for the record.** Two of its three defects were fixed first. **The clipping was the title sitting on the content's first pixel** with the sheet's own 28dp corners cutting into it, which is a missing top padding rather than a peek height, and it is fixed. **The six choices are a two by three tile grid**, one column above font scale 1.3, walked in English at 1.0 and 2.0 and in Arabic where it mirrors correctly. **What remains is the form behind it**, where the person still meets roughly twenty three chips on a year five notebook: at most five per group, most recently used and most contextually likely, plus one control opening the full set with search, and the note field moved to the top where it belongs.
4. **The unfiled tray, inverted to suggestion first.** One card per unfiled item showing the item's own words, the matcher's single best suggestion as the primary action, two alternates, one control for anything else.
5. **The notebook: a hero plus a tile grid.** The one or two things that need attention now at real size, then the sections as tiles, forward at standard size and folded compact.
6. **Today: a hero plus dense rows.** The single most important thing since the last visit at display size, supporting items as dense rows rather than cards, then a quiet footer.
7. **Projects, which are templates and neither look nor behave like it.** The largest single gap and it is functional as much as visual. A project must say which template it came from and that its steps are a starting point. Everything editable, per `MASTER_SPEC.md` 4.10: add, remove, reorder, rename, edit, attach notes, dates, documents, people, and calls to any step, create a project with no template, save a modified project as the person's own template. Editing a shipped template creates the person's own copy with lineage preserved. Visually it is a spine, not a checklist. No percentage, no progress bar, no streaks: position, not score. The library in More shows which templates are in use, when they were applied, and what each created, with links.
8. **#144, the retroactive sweep** of every other existing screen against section 11.
9. **Documents and bills as galleries with real thumbnails.** The app stores photographs of the person's own paper and shows none of them.
10. **Care team with avatars.**
11. **#62, the template catalog in four languages.**
12. **Remaining Phase 1:** the emergency card, More, and anything still saying it is unbuilt.
13. **#43 and #44**, the audit and the accessibility gate, alongside all of the above rather than after them.
14. **The eight journeys walked end to end, then personas P2 through P13.**

**Still true and still waiting, from the previous night.** These are not superseded, they sit behind the list above.

- **The design review queue waits on the owner rather than on work.** Thirty three `needs-design-review` issues. Nothing there is blocked on code.

- **The three connections the spec promises and the schema cannot hold: #141.** An incident knows its project, a bill knows the call where it was disputed, a bill knows the standing instruction it broke. All three are additive nullable columns; what needs deciding is whether each is a column or a link table, and getting that wrong costs a second migration. **This is a decision, not a task.**

- **P7's two unwalked targets**, which are the interesting half: the export of a full year-five notebook completing, showing progress, being cancellable and surviving being backgrounded; and memory across twenty minutes of navigation. The fixture and the passphrase are in section 8. **Everything needed to walk them is on the phone already.**

- **#142, a benchmark that can actually see a 400ms target.** P7 asks for search under 400ms and the answer tonight was "faster than adb can observe", because `uiautomator dump` costs 2,770ms on this notebook and `screencap` costs 985ms. An instrumented test timing `Repository.search` with `System.nanoTime` would give a number that holds in continuous integration. Until it exists, that target has no evidence behind it either way.

- **#131, the other half of search**: scoped search per section, the assembly view, date range search against imprecise dates, reconstructing a day. **The assembly view is also P7's untestable target**, so this closes two things.

- **#135, a situation template applies its threads and nothing else.** The ten item first-days checklist and the six document slots are parsed and unread, and the checklist is the highest value content in the catalog.

- **The personas still unwalked: P3, P5, P6, P8 through P13.** All of them are reachable now that the generator writes people, appointments, questions, medications and an emergency card. **P9 is the cheapest and the most revealing**: it needs no device at all, only reading an export as somebody who has never seen the app.

- **#44's remaining half, which needs ears.** How a label sounds, where pauses land, whether a row is bearable at higher verbosity, and Arabic with the reader running.

- **#143, the fourteen tables that still have no fixture writer.** Each is a screen that has never met generated data, which is exactly the condition that produced six of tonight's eight defects. The issue orders them by how much screen each unlocks.

- **#125 asks the owner a question**: should the app open on Today rather than the Notebook? P7 measured cold launch to the notebook because that is where the app opens.

- **The final translation good faith check**, deliberately last, and **language access after everything above**, per section 5.

### `main` is current again, and there is now a rule for the end of an increment

Tonight ran as a chain of branches, each cut from the last, and none of them was ever merged. `main` sat at `d2ad004` thirty six commits back: the commit this session started from. Everything was pushed and nothing was at risk, which is why it went unnoticed for a whole night. A fresh session clones `main`, reads `HANDOFF.md`, and would have found a file describing a night that had not happened.

`origin/main` is fast forwarded and **D69 adds the missing half of D48**: an increment ends when `main` contains it, and `git rev-list --count origin/main..HEAD` reading zero is the check, the same way `git branch --show-current` is the check at the other end.

### What the light theme pass has found so far

The pass is running now and it is not only producing captures. Three things on screens that had already been walked in dark:

**An untitled entry led with a stock phrase.** Most entries have no title, because capture asks what happened rather than for a name, which is right. The entry screen rendered "Something you wrote down" at display size with the sentence the person actually wrote in body text underneath it: the largest words on the screen carrying the least information, rule 15 backwards. The body is now the heading, cut at a word boundary past ninety characters and then repeated in full below. `headingFor` in `EntryScreen.kt`, six cases in `EntryHeadingTest`.

**Removing an untitled entry asked "remove what?" with a blank.** The entry screen passed `entry.title.orEmpty()` into the confirmation, so an entry with no title named nothing. It now names the same words the heading shows. The trail's own remove already had an honest fallback; the two are deliberately different because they name what is on the screen the person is looking at.

**A record was named with the button that made it.** `kindLabelKey` mapped to the `capture.*` catalog keys, so a visit already written down appeared on the trail, on a person, in a chapter, on a thread, on a prep sheet and in search results as "Log a visit". An imperative sitting where the description belongs, reading as an instruction to somebody looking back over six months. The `entry.kind.*` nouns already existed in all four catalogs for the entry screen's subtitle. Renamed `kindNameKey` and repointed; all nine callers name something that has happened.

### The accessibility gate, walked on tonight's screens

**Font scale 2.0**: the medications list, one medication with its history, and the prep sheet with both its spines all hold. Nothing truncated, nothing overlapping, everything reachable by scrolling. The waypoints stay anchored to the first line of each card, which is what the clamp is for.

**Arabic**, set through the app's own per-app locale rather than by changing the phone's language, so nothing outside this app was touched. The layout mirrors, the gutter moves to the correct side, and **the spine itself did not mirror**, which is the finding. `Modifier.offset` is layout direction aware; the canvas that replaced it is not, so the line sat nineteen dp from the start edge instead of nine. It looked right in isolation and was caught by measuring the same screen in both directions: fifty nine pixels from the start edge in English, eighty one in Arabic, sixty one after the fix.

**The phone was restored exactly**, per rule 19. `font_scale` back to 1.0, `animator_duration_scale` deleted rather than set, the app's locale back to en-US, and the restore script was written to `/tmp` before anything was changed.

### What is owed on every screen built tonight

**Light theme captures, Arabic, and font scale 2.0** for the eight new screens. The gate was walked on a sample tonight, not on all of them: the notebook, the disclaimer, the incident thread, and Today were checked at 2.0 and in Arabic and all pass. **The nav label cap came out of that pass**, D68's sibling finding.

**The fixture had no appointments, no medications, no questions and one person**, so the prep sheet, the medication history, the questions section and the person screen could not be reached from a seed at all. Found trying to capture them, and **fixed**: `care_team`, `involve`, `appointments`, `questions`, `medications` and `medication_history` are written now, and a month six seed opens on ten people, eight medications with seventeen events behind them, nine questions and two care plan meetings.

**It immediately paid for itself three times, all on the medications screens, all within ten minutes of the first real seed.**

**The app crashed outright opening any medication with a `noted` event.** `Strings.resolve` throws on a key no catalog defines, which is right for a key the code writes and wrong for one built by interpolating a database column. `medication.event.noted` was a real value in the schema's CHECK constraint that no catalog had. `medicationEventKey` is a `when` over the six kinds now, with a fallback, so a notebook written by a later build cannot take the app down.

**The picker wrote a value the database rejects.** `medication_event.kind` allows `resumed`; `MedicationChange` offered `restarted`, and all four catalogs agreed with the picker rather than with the schema. Choosing "Started again" would have failed the CHECK constraint. That path had never been exercised because nothing had ever produced a medication with a history. `MedicationEventKindTest` reads the constraint out of `contract/schema.sql` and holds the picker and the four catalogs against it, rather than restating the list and becoming a fourth copy of the same mistake.

**A stopped medication claimed to be on the emergency card.** The card assembles itself from medications that say they belong on it and drops the stopped ones, correctly and deliberately. The list and the detail screen each rendered the stored flag instead, so a medication she stopped in February sat there in alert orange claiming to be on the card somebody hands a paramedic. One rule in one place now, `Medication.showsOnEmergencyCard`, held by `EmergencyCardClaimTest`.

### What the "everything connects" audit found

`MASTER_SPEC.md` section 3 lists what connects to what under **Everything connects. No dead ends.** Every clause was checked against the built app tonight. Most now hold. **A medication knows its pending questions** was built in both directions: the question form offers medications as chips, the medication carries a "Waiting to ask" section, and the question's entry links back. **An incident knows its people and its documents** needed no new column, only a join nobody had written.

**Three clauses cannot be built without schema columns that do not exist**, and the schema is fixed by contract under rule 3, so they are **#141** rather than done: an incident knows its project, a bill knows the call where it was disputed, and a bill knows the standing instruction it broke. The third is the sharpest, because "they charged us for the transport after we asked in writing to be called first" is exactly the sentence this app exists to produce.

**`MedicationQuestionJourneyTest` walks it from the front door**, per `TESTING-PERSONAS.md` section 7: adds the medication through the form, asks through the capture sheet, taps the chip, and follows the link both ways. It took five runs to get green and **not one of the five failures was in the thing being tested**: a button below the fold in a lazy list, the capture button being drawn under an open section, a save button deliberately outside the scrolling region, and twice a scaffold name used where a test tag was meant. That is the ordinary cost of walking in from the front door rather than composing a screen, and it is also the reason those tests find what screen tests cannot.

Seventeen tables still have no writer. The ones that matter next are `emergency_card` and `emergency_contact`, `custom_template`, `cost_sheet` and `cost_entry`, and `person_chapter`, which is one of the links-both-ways gaps.

**A modeling error came out of it.** `scaled` was being applied to two things that are not events: the care team and the medication list are a roster, not a stream. Somebody on seven medications is on seven medications on her first day, and a family a month in already knows the charge nurse. Scaling them by history length gave a month six fixture two medications and made every medication screen look thin for a reason that had nothing to do with the screen. `roster()` holds them flat; what grows with time is the churn, the people who left and the medications that were stopped.

**The reader pass with TalkBack running**, #44, for the same eight.


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
| **The export and import round trip** | `RoundTripTest` on the phone, **15 tests, run both plain and encrypted**. **This is the test B4's whole argument rested on and it did not exist until 2026-08-01.** Every row of every user table compared column by column across an export and a restore. The EDTF string survives byte for byte, a month never collapses to its first day, unknown survives as unknown rather than as null or today, the uncertainty qualifier is not stripped, tombstones travel so a deletion is not resurrected, and the derived range is proven recomputed on import by writing a deliberately wrong one into the file and watching the import correct it |
| The export container | `ExportContainerTest` on the phone. What goes in comes out byte for byte, the manifest survives to the millisecond including tables with zero rows, the manifest is the first entry, and **all eight** files that must fail cleanly each name what was wrong. The last two, an unknown table or column and an attachment the database names that the archive lacks, needed to read the payload and only became possible once the payload was portable |
| **The export is openable somewhere other than the phone that wrote it** | `PortabilityTest` on the phone. **This was false until 2026-08-02 and every other export test passed throughout**, because they all restore onto the same device, where the Keystore key never changes. The archive carried the SQLCipher file as it sits on disk, so no other device could ever have read it, which made the only recovery path from key loss not exist. The check is on the payload's first sixteen bytes for the SQLite magic, plus an encrypted export decrypting to one, plus the live database *not* being one so the first check cannot pass by going vacuous. D61 |
| The restore, end to end through the interface | Walked on the Pixel twice, once before the portability fix and once after. Exported with a passphrase, added a care team row named SHOULD NOT SURVIVE afterward, restored, and the row was gone with the original two people back. A wrong passphrase was tried first and reported honestly without saying which of the two things was wrong |
| The export passphrase is concealed | `PassphraseMaskingTest` on the phone, checking what the field renders rather than which parameters it is passed. **It was rendering in the clear**, because a password keyboard is not a mask. D62 |
| **Today's digest** | `DigestTest`, 10 JVM vectors with no database and no composition, which is the shape #15 asks for. Covers the strict boundary, one row written four times counting as one correction, a row created and removed in the same span counting only as removed, notebook order rather than order by volume, and bookkeeping tables being left out. Walked on the phone with a seeded notebook |
| A visit is a run of the app | `LastVisitTest` on the phone. **It was once per composition**, so a theme change or rotation advanced the mark mid-visit and the digest went blank. Found on the phone with a freshly seeded notebook reporting nothing at all. D63 |
| Attachment storage | `AttachmentsTest` on the phone. The same bytes are one file, the streaming and whole-file paths agree, a changed file fails verification, and a half written file is never visible under its hash |
| The date picker | Walked on the Pixel: opened from the capture form, picked August 18 with a time, and the form read back "August 18, 2026 at 2:00 PM" through the same renderer every other date uses |
| A deleted row is actually gone | `TombstoneTest` on the phone deletes through the repository and asserts it leaves every read the app has: the count, the Unfiled tray, the date lookup, the thread chips, and a link table join. It also asserts the row physically survives, because a removed row leaves nothing to tell a peer it was deleted |
| Tombstones cannot leak | `check_live_views.py` fails any read of a base table outside a live view, in app and test sources alike. Proven by three deliberate breakages: a leak inside the repository, a leak on a screen, and an allowance with no reason |
| Every screen built so far | Instrumented, plus built, installed, opened, and looked at on the Pixel |
| Today, as the app's front page | Walked on the Pixel with a seeded notebook. It led with an apology for the unbuilt digest and three fixed suggestions that ignored the notebook behind them. Now it leads with what changed, each row opening its section, and suggests only what is still undone |
| A section opened from Today comes back to Today | Walked on the Pixel. It used to say "Back to the notebook" to somebody who had come from Today, because opening a section also switched the destination underneath the overlay for no other reason |
| **The eight sections the notebook opens onto** | Each walked on the Pixel with real data typed through the app's own forms, in both themes, at font scale 2.0, and in Arabic. `ScreenReaderTest` covers all of them for labeling, 16 cases. **The reader pass with TalkBack actually running is not done and is not claimed**, #44 |
| The trail's route | Drawn to `DESIGN.md` 5.2 and checked on the device: the dashed gold line runs continuously through the month headings, the node lands on its date at both font scales, node color carries the entry kind, and the whole thing mirrors to the start edge in Arabic |
| Links that go both ways | A medication flagged for the emergency card appears on it, and one that is not does not. Walked with two medications. Taking somebody off the emergency card leaves them on the care team, walked and confirmed |
| A date corrected from the entry | Changed an entry's date in the trail, force stopped the app, relaunched, and the new date was still there, so it is in the database rather than in composition state |
| The notebook's fold behavior | Walked on the Pixel with a hospital stay template: appointments, the trail, documents, and standing instructions forward, money and progress collapsed, which is exactly what that template names |
| Dynamic type at font scale 2.0 | Every built screen looked at on the phone with the system font at maximum. Two defects found and fixed, both invisible at 1.0. The setting was restored afterward |
| Reduced motion | Verified with `animator_duration_scale` actually set to 0 on the phone, not by reading the code. A press still acknowledges, reaching the same target through a 100ms fade rather than a spring. The setting was restored afterward |
| Arabic on the device | Ran through a per-app locale rather than a system setting. Real Noto glyphs, no fallback boxes, and the whole layout mirrored. It also found that the template catalog is English only, #62, which no check covered. **This pass missed a heading defect that had been shipping the whole time**, see the row below |
| **Display headings in Arabic** | **Every Display L title in the app broke in the middle of a word**, including the notebook's own, on a screen with most of its width empty. The negative letter spacing the display styles carry is a Latin device that crushes a connected script's joins and broke line layout outright. `displayS` carries none and was always correct, which is what identified it. Fixed by `healthTrailTypeFor`, held by `TypeTest`, 6 JVM tests. **Found by opening the app in Arabic and looking at a title.** No check covered it, the code read correctly, and the earlier Arabic pass confirmed glyphs and mirroring without ever looking at a heading |
| The typefaces | Bundled and looked at. Bricolage Grotesque, Atkinson Hyperlegible, JetBrains Mono, Noto Sans Arabic. Every license verified against `google/fonts` rather than assumed |
| The capture sheet, looked at with fresh eyes | Two defects nothing else would have found: "Save a document" closed the sheet and did nothing, and the inherited Material scrim barely dimmed the notebook behind it. D44 and D45 |
| Screen reader labels | `ScreenReaderTest` walks every screen's semantics tree, including a sheet's own window, and fails any touchable node with no text and no content description. Ten screens. It found one on its first run and that is fixed |
| Screen reader, with TalkBack actually running | Walked on the Pixel 2026-08-01 with TalkBack enabled, the notebook, Appearance, and the capture form. Traversal order matches visual order. Rows are one stop reading "Care team, nothing yet" rather than two fragments. Fields carry their label. Selection reports as `checked` on chips and options, so a reader user is told which is chosen rather than inferring it from a mark. No unlabeled control. The phone was restored exactly and TalkBack confirmed unbound. D54 |
| Every screen looked at with the keyboard up | Two defects found that way and nowhere else: the setup button colliding with the last field, and the field clipped mid-box at the scroll boundary |
| The Unfiled tray | Walked on the Pixel end to end: a call saved with no thread, the waiting card appears on the notebook, the tray suggests "Nursing" from the words in the entry, filing it links the thread and clears the tray in one transaction, and the card disappears |
| The press state, everywhere | Measured on the device on three different surfaces: a card row (26,36,43) to (43,50,56), the filled button (127,182,212) to (136,186,214), the capture button (227,177,85) to (228,182,100). `FilledButton` and `TextAction` previously had no press state at all |

**The whole instrumented suite: 277 tests, 0 failures as of 2026-08-03 14:50.** Before that it was **three failures, and this file said zero for a day**: `5ca7368` gave the emergency card its own count string and left `NotebookScreenTest` and `ReaderStopsTest` asserting the old one. D73 has the account and the rule it sets. The older sentence read: 275 tests across 30 classes, 0 failures, run on the connected Pixel 10 Pro XL through `tools/verify.sh --device`, and the app was reinstalled immediately afterward per D56. **98 JVM unit tests, 0 failures**, the newest of which are `EntryHeadingTest`, `EmergencyCardClaimTest` and `MedicationEventKindTest`, all three written against defects the fixture found rather than against code being added. All 11 implemented compliance checks pass, and **lint passes**, which is the step that keeps catching what the habitual checks do not.

**A pattern worth carrying forward.** Almost every defect this run found came from putting the built thing in a hand and changing one condition: the font at maximum, the keyboard up, the language set to Arabic, or simply looking at a screen that had already passed its tests. None of them were visible in the code, and several had passed a review. The tests are what keep them fixed; they are not what found them.

---

## 5. Remaining work inventory, in order

**Rebuilt from the tracker on 2026-08-01.** The previous version of this section had four rows spliced in from section 4's verification table, which put `MigrationTest` text under issue #9 and left three rows with no issue number at all. It also listed #39 as unbuilt while sections 4 and 9 recorded it as built and walked. It was patched too many times and is now derived from `gh issue list` rather than edited in place. **If this section and section 3 ever disagree, rebuild this one from the tracker and make section 3 follow it.**

**Closed in the long run of 2026-08-01**, so a fresh session does not go looking for them: #14 the migration mechanism, #22 the end of life instruction tag, #36 the notebook, #37 setup, #38 the date model, **#39 the date interface**, #40 the press sweep, #41 the situation picker, #42 measurement, #48 the template, #53 the Unfiled tray, #58 the subject scoped counts, and #78 the empty Today. #21, the roadmap, is also closed, and **#12 the fonts**, closed once Chinese was verified rendering from the system face on the device. #25 About and #57 the document capture input closed in the same run. #102 and #109 closed on the language question and the translation disclaimer.

**Changed on 2026-08-02**, in the run that continued through the morning. The rows below are current as of 06:25.

**In order. The first is the one to take.**

| Issue | What | Why here, and what it is actually waiting on |
|---|---|---|
| **#9** | **The export container** | **Done.** Round trip, encryption, all eight failure cases, portability, and as of 2026-08-03 a passphrase is required and there is no unencrypted export, D67. Format version 2 |
| **#47** | **Search** | **The universal half is done and walked.** #131 carries the rest |
| #62 | The template catalog is English only | Release blocking. The app currently shows an Arabic interface wrapped around English content, which any Arabic reader sees immediately. Found by running the device in Arabic, not by any check |
| #43, #44 | The retroactive audit, and the accessibility gate | **Worked alongside new screens, never saved for a phase gate.** Both partly done with findings on the issues. **#44's reader criterion is met for three screens**, walked with TalkBack actually running on 2026-08-01, D54. What remains is the same pass over the screens not yet walked, which is now cheap and proven safe |
| #57 | The document capture input | The last of the six ways in. **No longer blocked**: the attachment storage it needed landed with the export's first piece |
| #8 | The repository layer | ~~Tombstones travel through the export.~~ **Proven by `RoundTripTest`.** Ready to close once somebody confirms the other criteria |
| #7 | The change log | Proven through Kotlin. **The digest now reads from it**, so the last criterion is met. Ready to close once somebody confirms |
| #17 | The fixture generator | Everything but the four language variants, which wait on #62 |
| #15 | Golden vectors | `dates.json` runs on the phone in all four locales, and **`DigestTest` adds 10 JVM vectors for the digest engine**. What remains is a second platform to run them against, which is #16 |
| #10 | `SyncTransport` | Needs the export container, so it follows #9 |
| #46 | No dead ends, links both ways | **Partly done.** Today's digest rows and coached steps now open what they name, and a section opened from Today returns to Today. What remains is a deliberate sweep of the rest rather than the two found by walking |
| #47 | Search | **Unblocked.** Today and the digest engine both exist now. This is the largest remaining feature and a reasonable next thing to take |
| #45 | Capture from outside the app | Independent of everything above. Widget, quick settings tile, share sheet target |
| #16 | The web scaffold | `npm` is absent on this machine. Nothing else blocks it |
| #13 | The four locale scaffold | Arabic and Chinese are both verified on the device now, and choosing a language actually changes the language, which it did not before D52. What remains overlaps #62 |
| #18 | Content checks in continuous integration | Ten run. Open for the ones not implementable yet, each named in `run_all.py` with what it waits on |
| #1 | Phase 0 parent | Closes when its children do |

**In the review queue, waiting on the owner rather than on work.** Twenty two now, and none of them is waiting on anything this project can do: #28 the disclaimer gate, #30 setup, #32 the situation picker, #34 the capture sheet and form, #50 the notebook, #55 the Unfiled tray, #68 the date picker, #81 Today's empty state, #89 Appearance, #111 the trail and care team, #113 the emergency card, #114 medications, #115 Ask next time, #116 care threads, #117 Progress, #118 chapters, #119 appointments, #120 standing instructions, #121 money, #122 documents, #123 projects, and #124 About. Each carries a real device screenshot. **Arabic screenshots are no longer blocked** for any of them.

**#125 is a question for the owner, not a task:** should the app open on Today rather than the Notebook? `MASTER_SPEC.md` calls Today the dashboard. Today is now worth opening on, which it was not when the question was first asked.

**Phase 1 feature work still ahead:** Today with the digest engine, the trail itself, care team, medications, the emergency card, projects, and More.

~~**An in-app theme setting.**~~ **Built, #88.** Follow the phone, light, or dark, in More. It applies immediately and persists, and it removed the standing dependency it was partly built to remove: **both theme sets are now captured from inside the app and the phone's own theme is never touched.** `tools/screenshot.sh` reads the app's stored choice first and falls back to the device only when the choice is to follow it, which corrects D31's assumption that the device is the answer.

**Language access comes after all of the above**, and it is a body of work rather than a task. **Twelve issues are open and none of them is started, deliberately.**

**#92 is the parent** and carries the ten languages, the ordering, and the cost. Seven new languages: **#93** Vietnamese, **#94** Korean, **#95** Tagalog, **#96** Russian, **#97** Haitian Creole, **#98** Portuguese, **#99** French. Then **#100** script and typeface coverage, **#101** plurals with golden vectors, **#102** translation quality and what shippable means, and **#103** right to left confirmation.

**It is language access for caregivers in the United States, not international expansion.** The federal, Medicare, and Medicaid content is specific to this country, so translating for a Spanish speaker in Texas is right and presenting the same app to someone in Spain would be wrong. `MASTER_SPEC.md` sections 7.1 and 7.2 carry the reasoning.

**Two things easy to get wrong, both written into the issues.** Haitian Creole is a distinct language and never a fallback for French, or the reverse. Chinese ships as Simplified and Traditional is a separate question rather than an alias.

**Roughly 1500 strings per language, so seven languages is on the order of ten thousand**, each of which is care instructions, money, or somebody's rights. **An unreviewed language is not shippable**, not shippable with a caveat. That applies to the four already shipping, all of which are currently unreviewed.

**Do not begin any of it until everything ahead of it is done.**

**Something that must not survive to release.** The More destination renders an honest interim screen below Appearance, and Today says plainly that its digest is still being built. **Projects is built**, so only those two remain. **The document capture input no longer does**, because it is built, #57. That is deliberate rather than a stub: `DESIGN.md` section 5.5 fixes the four destinations and their order, and D44 says an interface may offer something it has not built but may not go quiet when someone takes it up. Each disappears as its destination lands. **If one is still there at release, that is a bug**, and `ShellTags.NOT_BUILT` makes them greppable.

---

## 6. Blocked

**One thing is blocked, B5, and it does not stop the work.** The destructive command guard needs installing from user settings and only the owner can do it, because Claude Code refuses to let a session edit its own hooks. Section 0a and D64 have the account, and B5 in `DECISIONS.md` is written as steps he can act on. **A fresh session continues on everything else exactly as the last three did**, on rule 6 followed by hand. The four older entries in BLOCKED are all resolved and kept there with their outcomes.

**Arabic is no longer waiting.** The fonts landed on 2026-08-01 and Arabic renders correctly on the device, so the Arabic screenshots on the design review issues can be captured whenever someone works through them.

**The light theme screenshots are no longer waiting.** The phone was found in light theme on 2026-08-01, so the full set of 28 was captured then. **Both sets now exist**, 30 light and 20 dark, captured through the in-app theme setting without touching the phone.

**The gap in the dark set is the first-run screens**, the disclaimer, setup, the situation picker, and the notebook straight after setup. Those are reachable only on a fresh install, and a fresh install has no stored theme choice, so it comes up following the phone, which is light. Getting them in dark means choosing dark and then getting back to first run, which wipes the choice. **Not worth solving with an uninstall**, which is blocklisted anyway, per D50. They exist in light and that is enough until either the phone is in dark or a debug-only "start over" action exists.

---

## 7. The phone

**Do not run the guard probe. It is answered.** Section 0a and D64 carry the result: the guard has never fired through Claude Code, the fix is B5, and it needs the owner. Rule 6 is followed by hand until B5 lands.

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

**Walking the app.** `tools/walk.sh see` prints every piece of text on screen, in order, by asking the semantics tree, which is what a screen reader walks. `tools/walk.sh tap "Medications"` taps the first node matching text or content description and prints what it tapped, so a walk that goes wrong says so. `tools/walk.sh fields` lists the editable fields with tap coordinates. **These lived in a session scratchpad for two nights and were rebuilt from scratch each time**; they are in the repository now. A dump costs about 2.7 seconds on a five year notebook, which is fine for walking and useless for timing, per #142.

**Seeding a notebook.** `tools/seed.sh month6 6 walk-month-six` generates, packs, pushes, clears the app and walks the restore screen. `tools/seed.sh year5 5 walk-year-five` for the long record. It goes in through the app's own restore path deliberately, per D61: a fixture that arrives any other way has never been through the importer.

**Screenshots.** `tools/screenshot.sh <name>` writes `docs/screenshots/<name>-<theme>.png`. It refuses to capture unless the app is the focused window, checked before and after, because this is the owner's daily driver phone. It reads the theme from the device and refuses an argument that disagrees, per D31. Do not pass a theme. **It also switches heads-up notifications off for the duration and restores them through a trap**, because focus is not enough: a heads-up notification never takes focus, and one put the owner's phone number and a contact photo into a capture on 2026-08-01. D53. **It also crops the status bar off**, at a height read out of `dumpsys window` rather than assumed, and it **deletes the file rather than keeping it** if the height cannot be read or ImageMagick is absent. D72. Suppressing heads-up notifications was only the loud half: the status bar is always there and on this phone it carries a contact photo about eleven pixels across. Demo mode was tried first and leaves every notification icon in place, which is worse than nothing because the result looks sanitized. **Look at every image before committing it.** The script is a control and it is not the last one.

**Driving the app by hand over adb.** `adb shell uiautomator dump /sdcard/w.xml`, then tap the center of a node's bounds. Matching on visible text is the simplest selector and it works.

**A trap in the Compose test API, found the hard way.** `performScrollToNode` walks a lazy list a viewport at a time and gives up when it thinks it can go no further. It got that wrong for the Arabic catalog, stopped two rows short, and reported the rows as absent when they were only further down. **Scroll by the list's own item key instead**, with `performScrollToKey`, which asks the list where the item is. That needs the test tag on the `LazyColumn` rather than on a surface around it: the scroll action merges upward and looks like it works, while `IndexForKey` does not.

**Continuous integration.** The workflow triggers on `push` to main, on `pull_request`, and on `workflow_dispatch`. Pull request events stopped firing part way through 2026-07-31 and **are firing again as of 2026-08-01**. If they stop again: `gh workflow run ci.yml --ref <branch>`, then poll `gh run list --branch <branch>`. **Do not read an absence of checks on a pull request as a passing build.**

**Three CI steps catch real habits.** "HANDOFF.md is current to within one increment" fails any pull request that changes `android`, `web`, `tools`, or `contract` without touching this file. It caught pull request #49. "README.md describes the screens that exist" fails any pull request that adds or removes a file under `ui/screens/` without touching `README.md`, which exists because the front page claimed the app had one screen for a week after it had nine. "Every screenshot the README points at exists" catches a rename. Rewrite the documents in the same commit as the work, not afterward.

**Gradle is fast and it looks broken.** An incremental Kotlin recompile of several changed files finishes in about a second. That is real.

**Everything else:** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21, compileSdk 37, targetSdk 36, minSdk 26. minSdk 26 is why `java.time` is available to `Edtf.kt` without desugaring. Android's `execSQL` refuses any statement that returns rows and `PRAGMA journal_mode` returns one, so `ContractAssets.splitStatements` handles the splitting including trigger bodies and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

**Run `tools/verify.sh`, not the checks you happen to remember.** Continuous integration failed on 2026-08-02 for a lint error, `Uri.parse` where the KTX `String.toUri` was wanted, in code that had been walked on the device and had passed all ten content checks and 185 instrumented tests. **`verify.sh` runs `lintDebug` and would have caught it.** Running `run_all.py` plus the instrumented suite by hand feels like verifying and skips whatever is not in that habit.

**Verification.** `tools/verify.sh` is the honest runner: it captures every step's exit code, never stops at the first failure, reports SKIPPED distinctly from PASS, and exits nonzero naming what failed. `python3 tools/checks/run_all.py` runs the 11 content and contract checks alone. **Never chain a commit on a grep of output.**

---

## 9. Screens built without a mockup

Every screen built without one is composed from existing components under `DESIGN.md` section 10, ships complete with every state, and is logged in three places at the moment it is built: a `needs-design-review` issue with a device screenshot, an entry in `DESIGN.md` section 8, and a line here.

| Screen | Built | Issue | Reviewed |
|---|---|---|---|
| One medication, and how it changed | 2026-08-03 | #140 | not yet |
| One chapter, and what happened there | 2026-08-03 | #139 | not yet |
| One care thread, and everything on it | 2026-08-03 | #138 | not yet |
| An appointment's prep sheet | 2026-08-03 | #137 | not yet |
| One person, and everything that involved them | 2026-08-03 | #136 | not yet |
| One entry, read on its own | 2026-08-02 | #134 | not yet |
| Incidents, and one incident's thread | 2026-08-02 | #133 | not yet |
| Search | 2026-08-02 | #130 | not yet |
| Exporting the notebook | 2026-08-02 | #126 | not yet |
| Restoring from a file | 2026-08-02 | #127 | not yet |
| Today, rebuilt around the digest | 2026-08-02 | #81 | not yet |
| The trail | 2026-08-01 | #111 | not yet |
| The emergency card, and filling it in | 2026-08-01 | #113 | not yet |
| Medications, and adding one | 2026-08-01 | #114 | not yet |
| Ask next time | 2026-08-02 | #115 | not yet |
| Care threads | 2026-08-02 | #116 | not yet |
| Progress, the readable record | 2026-08-02 | #117 | not yet |
| Chapters, the places | 2026-08-02 | #118 | not yet |
| Appointments, and adding one | 2026-08-02 | #119 | not yet |
| Standing instructions, and asking for one | 2026-08-02 | #120 | not yet |
| Money, and adding a bill | 2026-08-02 | #121 | not yet |
| Documents, and saving one | 2026-08-02 | #122 | not yet |
| Projects, starting one, and a project's steps | 2026-08-02 | #123 | not yet |
| About | 2026-08-02 | #124 | not yet |
| Care team, read only | 2026-08-01 | #111 | not yet |
| Disclaimer gate | 2026-07-31, rebuilt same day to the 10.6 bar | #28 | not yet |
| Essentials first setup | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #30 | not yet |
| Situation picker | 2026-07-31, rebuilt 2026-08-01 to the 10.6 bar | #32 | not yet |
| Capture form, four kinds | 2026-07-31, rebuilt same day to screen 26 | #34 | not yet |
| Notebook table of contents | 2026-08-01, rebuilt to the 10.6 bar | #36, review on #50 | not yet |
| Unfiled tray | 2026-08-01 | #53, review on #55 | not yet |
| Adding a measurement | 2026-08-01 | #42 | not yet |
| The date picker | 2026-08-01 | #39, review on #68 | not yet |
| Today, the empty state | 2026-08-01 | #78 | not yet |
| Appearance, and More around it | 2026-08-01 | #88, review on #89 | not yet |

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

### P2, week one, building the notebook. Walked 2026-08-02 on the Pixel, fixture: fresh install, nursing home situation

**Walked without the day-7 fixture**, and that limit is stated rather than hidden: the generator writes a plain SQLite file and the app's database is SQLCipher keyed by the phone's Keystore, so there is no way to seed it short of building an export container in Python. What was walked is everything P2's requirements turn on except volume.

| Must be true | Result |
|---|---|
| The situation template applied its **threads** | Yes. Five, and the notebook counts them straight away |
| It applied its **fold behavior** | Yes. Standing instructions, the trail, threads, and money forward; appointments and progress collapsed |
| **Adding a contact offers the template's roles as suggestions without forcing them** | **It did not. Fixed this run.** All six now appear as chips above a free text field that is unchanged |
| It applied its **checklist** and **document slots** | **No. Neither is built.** The catalog carries ten checklist items and six document slots for a nursing home and nothing reads them. Filed as #135 |
| Any of it can be edited or deleted | Threads and people yes. The checklist and document slots do not exist to edit |

**The roles finding is the one worth dwelling on.** `TemplateCatalog.Situation.roles` has existed since the catalog was written, with a comment on the field reading "Contact roles to offer when adding a person. Suggestions, not a fixed list." **The data was parsed, documented, and never shown.** A nursing home notebook knew there was a director of nursing, a charge nurse, a social worker, an assessment coordinator, an administrator, and a billing office, and it asked the person to type all six from memory.

That is Part Two's rule almost word for word: anywhere the set of possible answers is knowable, offer chips rather than a text field.

**The field stays, and a chip only fills it.** A role not on the list is the common case in home care, tapping a chip twice clears it, and what it filled can be edited. Nothing became a fixed vocabulary.

**What P2 says to watch for, and what was seen:**

- *Template content that reads as advice rather than administration.* The five threads are Nursing, Daily personal care, Activities, Meals and dietary, and Social services. All administration.
- *A checklist that cannot be edited.* There is no checklist, which is #135 rather than a pass.
- *Roles that cannot be renamed.* They are suggestions filling an ordinary text field, so renaming is typing.

### P4, month six, the first fight. Partly walked 2026-08-03 on the Pixel, fixture: month6 seed 1, restored through the app

**The first persona walked against generated data rather than data typed by hand**, which is what `tools/fixtures/pack.py` unlocked.

| Must be true | Result |
|---|---|
| **The incident thread records every call with names and dates and reads start to finish** | **Yes.** Five weeks of chasing on one screen: reported to the charge nurse on April 11, called the unit on the 20th, asked the director of nursing in writing on the 29th, told on May 8 it had gone to the care plan meeting, told what they decided on May 17, and then a hollow waypoint and "Nothing since. This one is still open." |
| Open incidents are visible from Today | Yes, "1 open incident", opening the list |
| **Each of these exports as its own document, legible to somebody who has never seen the app** | **Yes for an incident.** Not yet for a standing instruction or a bill |
| The standing instruction shows its violation count, each violation linking to its bill or incident | **Not built.** `instruction_violation` is in the schema and nothing reads it |
| The disputed bill carries its state and its link to the instruction it broke | **Partly.** Bills carry state; the link to an instruction is not built |

**Two fixture defects found by looking at real data, both fixed.**

**The generator wrote incidents with nothing on them.** Every incident read "0 things written down", because no entry ever carried an `incident_id`. P4's first requirement is precisely that the thread records every call, so the persona was untestable against generated data. The generator now writes two to five steps per incident, spread from the report to the answer.

**"0 things written down" read as broken** even when it was true. An incident is itself a thing written down, so the zero case now says "Nothing written down since" and "Answered and nothing else written down".

**Not walked yet:** P3, and P5 through P13. P10 through P12 need #62, since the template catalog is English only and a language persona against English content tests nothing.

---

### P7, year five, the long record. Walked 2026-08-03 on the Pixel, fixture: `year5` seed 5, restored through the app's own restore screen

**The fixture is finally the one P7 describes**, which it was not before tonight: 1,630 entries, 9 chapters, 7 care threads, 23 appointments, 90 questions, 8 medications with 75 events, 40 documents with attachments, 15 milestones, 10 people and 562 entry-person links.

| Target | Result |
|---|---|
| Cold launch to interactive, under 1.5s | **Pass, about 0.9s.** Still loading at 0.8s, drawn at 1.0s, measured by capturing the screen at fixed delays. Note this is **the notebook, not Today**: the app opens on the notebook, which is what #125 asks the owner about |
| Universal search, first results under 400ms | **Pass, though not measured to the millisecond.** Results were already rendered by the time a capture could be issued, which is about 1.1s after typing began and mostly the tools' own latency. The honest statement is "faster than adb can observe", and a real number needs a benchmark rather than a stopwatch |
| Trail scroll from now back to year one: no dropped frames, no placeholders, no jumps | **Pass.** 3,026 frames over the full scroll to the end of the record, **0.23% janky, 99th percentile 14ms**. An earlier shorter run measured 0.48% at p99 16ms. No loading placeholder was ever visible and the position never jumped. The scroll ends on the undated group, which is where the ordering puts entries whose date nobody knows |
| Year scrubber jump to any year, under 300ms | **Not testable. There is no year scrubber**, which is the finding rather than a pass or a fail |
| Assembled collection for an item with 30 related records, under 1s | **Not testable. The assembly view is #131** and is not built |
| Export: completes, shows progress, cancellable, survives backgrounding | **Not walked.** Ran out of session |
| Memory: no growth across 20 minutes of navigation | **Not walked.** Ran out of session |

**What P7 says to watch for, and what was seen.** Nothing loads the whole trail to show ten rows: the scroll is flat across 1,630 entries and the frame numbers say so. Chapter and thread filters are in the query rather than in application code, which the live-view check already enforces. The PDF export and attachment scanning were not reached.

**The tooling lesson, which is the same one as four times earlier tonight.** `uiautomator dump` costs about 2,770ms on this notebook, so the first attempt to time the launch measured the dump and reported 2,770ms. `screencap` costs about 985ms, also too slow. And a pixel detector written to spot the drawn screen sampled a band that is empty even when the screen is drawn, so it reported "still loading" at five seconds against a screenshot that plainly showed the notebook. **Three instruments in a row reported on something other than what they were asked about**, which is D68's rule arriving for the fifth time in one night: distrust a negative result from a tool that cannot tell you what it did not examine.

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
