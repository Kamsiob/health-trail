# The cold start prompt

Paste the block below into a fresh Claude Code session. It is kept here so it
survives a cleared session and so it can be corrected rather than rewritten from
memory each time.

**It is written to run unattended until the app is finished.** `RUN-SAFETY.md`
is the contract for that. Rules 9 and 10 are what keep it moving: three attempts
then log to `DECISIONS.md` BLOCKED and start the next item; decide rather than
ask. The board and `HANDOFF.md` are the state, which is what makes the run
survive its own compactions.

---

```
Read `gh issue view 321`, then HANDOFF.md. Both, fully, before anything else.

═══ THE JOB ═══

**Finish the app. Do not stop until it is finished.**

Work this order from the top. Close each issue before starting the next. Do not
stop between items, do not ask for review between items, do not report progress
between items. One report, at the very end, when every item below is closed.

  1. #395  The APK. Every precondition is met.
  2. #399  The projects section, reimagined.
  3. #398  A screen for every tracked thing.
  4. #397  Notes, general and attached, with light rich text.
  5. #396  The dictation rule.
  6. #395  Deliver the APK again from the finished app, and walk it.

#400 is a gate inside 2, 3 and 4 rather than an item of its own: none of those
three closes until its rows are in the archive, the standalone decryptor reads
them, and two people in one notebook with every area populated survive export,
wipe, restore and a merge with a conflict.

**The run is finished when all six are done and `gh issue list --milestone "8.
Notes, projects and progress, the owner's next phase" --state open` is empty.**
Not before. If you find yourself composing a summary while any of them is open,
you are stopping early: go back to the list and take the next item.

═══ THE RUN SURVIVES ITSELF ═══

This run is long and the context will compact, more than once. It keeps going
because the state lives on disk and on the board rather than in the
conversation.

**Re-reading a file you already read this session means compaction happened.**
Stop, re-read `gh issue view 321` and HANDOFF.md, then work out the next action
from the board rather than from memory:

    gh issue list --milestone "8. Notes, projects and progress, the owner's next phase" --state open
    git log --oneline -15

The lowest-numbered open item in the order above is what you are doing. The last
fifteen commits say how far into it you are.

**So keep the state current as you go, not at the end.** After every unit:
commit, push, and put what you learned on the issue you are on. After every
issue: close it, and rewrite HANDOFF.md sections 1, 2 and 5 to the new truth.
A decision goes in DECISIONS.md with a D number when you make it, never later.
If a compaction lands between a change and its record, the record is what is
missing and the next hour is spent rediscovering it.

═══ WHAT IS ALREADY DONE. DO NOT REDO ANY OF IT ═══

The interface is rebuilt on Material 3 Expressive and polished. #388 closed
2026-08-18. HANDOFF.md section 1 is the table. Specifically do not re-audit:
`ui/components` is retired; every screen is on Material's own state layer;
motion is app-wide and off under reduced motion; three people in one notebook
with no leakage; export, wipe, restore, merge and the standalone decryptor all
proved; the navigation bar, the selected destination, every empty state, and
the eight findings on #388.

**Enhance, do not replace, everywhere except projects.** The owner set that rule
and then exempted projects himself, which is why #399 says reimagined.

═══ THE BAR, WHICH UNATTENDED DOES NOT LOWER ═══

Material 3 Expressive is the floor, not the finish. A screen that is correct, on
Material's components, and plain is a 7 out of 10. `docs/V4.md` section 6 is the
bar: eleven checkable items, and 6.2 is a six step pass run against a real
capture rather than against the code. Rule 14 makes it retroactive.

**Every new screen is built twice on purpose, and the second pass is inside the
item, not after it.** Built on Material's own components and Google's own
assets, D196, then taken back through the eleven for visuals, fonts, styling and
spacing, so the whole app feels like one application. An issue is not closed
after the first pass.

Consistent where appropriate means these by name, and a new screen inventing its
own version of any of them is two answers to one question: D198 color, D200 the
add control, D201 the navigation bar, D202 the selected destination, D203
actions, D204 a tracked thing's own hue, and `SectionEmpty`.

Before drawing any measure screen, read `docs/TRACKED-THINGS.md`. It is the
research, with sources, and it is mostly about what rule 2 rules out.

**Rule 11 is what unattended runs break first.** Nothing unfinished reaches the
person: no blank area, placeholder, stub, debug label, truncation, or layout
that only holds with tidy sample data. A screen ships with its empty, one-item,
many-item, partially-filled, long-text, loading and error states, or it is not
built. If time pressure ever argues otherwise, it is wrong, because there is no
deadline in this run, only a finish line.

═══ WHAT EACH ITEM MEANS BY DONE ═══

**#395, both times.** `assembleRelease` exit 0 from current main, exported the
notebook first because a debug and a release build cannot upgrade each other,
installed the release, restored into it, walked the four destinations, one
section screen, one form and the restore. Size and SHA-256 in a comment.

**#399.** The section is navigable without being taught. Eleven screens is the
finding before any of them is looked at, rule 20, so the count comes down.
`ProjectDetailScreen.kt` is frozen, D112, so it is replaced through
`docs/REMOVAL-LEDGER.md` rather than extended. Every screen answers the eleven.
Rule 12 for anything composed rather than drawn: a `needs-design-review` issue
with a real device screenshot, a `DESIGN.md` 14 row, and a HANDOFF line, all
three at the moment of building.

**#398.** Every measure reachable in one tap from Progress, its own screen, the
flat mixed list gone, all eight states, and nothing on any of it that
interprets a number. `docs/TRACKED-THINGS.md` says what each one's screen owes
it and what it must never do.

**#397.** The schema decided in `contract/DATA-CONTRACT.md` before a screen is
drawn, rule 3. Rule 18 both ways on every attachment: if A shows B, B shows A.
Light rich text and nothing more, surviving the archive byte for byte.

**#396.** The rule written where `DictatableField` is defined, a check in
`tools/checks` that fails when a `Field` breaks it, and the frozen-file
exceptions named rather than assumed.

**And the whole run.** The accessibility gate in `DESIGN.md` 12 cleared with the
reader on and the font at maximum, on the new screens and on anything rule 14
brought up with them. The instrumented suite no worse than the 13 on #394 plus
#391. `tools/verify.sh` exit 0 and all 30 checks.

═══ HOW TO WORK ═══

OPEN THE SESSION ONCE:
  adb shell dumpsys window | grep isKeyguardShowing   # must say false
  tools/sweep.sh audit                                # seeds, walks, captures
  python3 tools/checks/run_all.py
Look at every capture. Notes go on the issue you are on.

ONE UNIT PER COMMIT: tools/verify.sh → install → LOOK AT IT ON THE PHONE →
commit → push. Never leave the tree dirty. An increment ends when origin/main
has it. This is also what makes the run resumable after a compaction.

VERIFY AT THE RIGHT DEPTH:
- Package move, no pixels        → compile + checks
- Component rewrite              → compile + checks + look at one caller
- Screen rewrite                 → tools/verify.sh + install + walk it
- Issue gate                     → verify.sh + sweep + dark + font 2.0 + reader

READ THE REAL EXIT CODE. A command piped through grep and then `echo $?` reports
grep's status, or head's. Redirect to a file and read `$?` from the command
itself.

MEASURE, DO NOT JUDGE. Captures are 1080 wide at 3x, so dp is px/3.

TAKE A BASELINE BEFORE BLAMING YOUR OWN CHANGE. Build the pre-session commit in
a worktree and run the same class:
  git worktree add /tmp/baseline <commit>
  cd /tmp/baseline/android && ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>

AN EMPTY NOTEBOOK COSTS THREE TAPS: More, Profiles, add another
person. Every section is empty behind it and the first person's notebook is
untouched.

NEVER SWITCH ANDROID USER PROFILES. It re-locks the phone and there is no way
past a secure keyguard from here, #316.

═══ WHEN SOMETHING RESISTS ═══

**Three attempts, then move on.** Same failure three times: write what was tried
and what happened to DECISIONS.md BLOCKED, and start the next item. Never loop.

**Decide rather than ask.** Where more than one answer is defensible, take the
easiest for the person, provided it is safe, private and compatible, rule 23,
log it with a D number, and continue.

**Only a genuine owner-only blocker stops an item**, and it stops that item
rather than the run: a locked phone, a missing signing key, a decision that
changes the data contract in a way the owner has not asked for. Write it to
BLOCKED, say so in the final report, and take the next item.

═══ TRAPS ═══

- The Material role names are inverted in this theme. Theme.kt maps
  displayMedium to type.hero at 30sp and displaySmall to type.displayM at 36sp.
- surfaceContainerLow is the canvas on light. A card drawn on it is invisible,
  and so was a tonal button in sand on a sand block.
- Count callers by use, not by import. SearchDoor sat built and uncalled.
- 13 instrumented failures are PRE-EXISTING, on #394, plus #391. Do not
  attribute them to new work without a baseline.
- Test counts come from the log or the XML, never a gradle exit code.
- Never compile while the instrumented suite runs. It also uninstalls the app,
  so reinstall and reseed after it.
- FROZEN, never edit: ProjectDetailScreen.kt, CaptureSheet.kt, PinnedGroup.kt.
  Frozen tail: Confirm, StepRow, Tile, Press, Spine. #399 replaces the first
  through docs/REMOVAL-LEDGER.md rather than extending it.
- A weighted child of a column with no bounded height measures to zero.
- material3 1.5.0-alpha26: no ButtonGroup, SplitButton or ToggleButton.
  Expressive components need @OptIn(ExperimentalMaterial3ExpressiveApi::class).
- adb: /home/Kamsiob/Android/Sdk/platform-tools/adb. Nav bar y=2302,
  x=107/323/540/755/971, which is Today, Notebook, Projects, Notes, More. Five
  destinations since 2026-08-18 and all five moved.
  Screenshots crop 132px of status bar; add it back before
  tapping.
- tools/sweep.sh's closing list globs its prefix and will print a capture hours
  older than the run. Check the timestamp.
- Rule 19: a phone setting may change only if the prior value is recorded first
  and restored exactly after, including on failure.
- Never screenshot the share sheet, the calendar app, or any screen with a
  password field.

═══ THE ONE REPORT, AT THE END ═══

When the milestone is empty: what shipped, what each issue's device
verification showed, the suite's numbers against the 13, every D number added,
anything in BLOCKED and why, and where the APK is with its size and SHA-256.

Until then, keep working.
```
