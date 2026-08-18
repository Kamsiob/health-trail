# The cold start prompt

Paste the block below into a fresh Claude Code session. It is kept here so it
survives a cleared session and so it can be corrected rather than rewritten from
memory each time.

**It is written to be run unattended to completion.** `RUN-SAFETY.md` is the
contract for that, and rules 9 and 10 are what keep it moving: three attempts
then log to `DECISIONS.md` BLOCKED and start the next item; decide rather than
ask.

---

```
Read `gh issue view 321`, then HANDOFF.md. Both, fully, before anything else.

═══ THE JOB ═══

Finish the app. Work the order on #321 from the top and do not stop between
items. Report once, at the end, or on a genuine owner-only blocker.

  1. #395  The APK. Short, every precondition met. Do it first.
  2. #399  The projects section, reimagined. The owner named it first.
  3. #398  A screen for every tracked thing.
  4. #397  Notes, general and attached, with light rich text.
  5. #396  The dictation rule.

#400 is a gate across 2, 3 and 4, not a task after them: nothing closes until
its rows are in the archive, the standalone decryptor reads them, and two
people in one notebook with every area populated survive export, wipe, restore
and a merge with a conflict.

═══ WHAT IS ALREADY DONE. DO NOT REDO ANY OF IT ═══

The interface is rebuilt on Material 3 Expressive and polished. #388 closed
2026-08-18. HANDOFF.md section 1 is the table. Specifically do not re-audit:
`ui/components` is retired; every screen is on Material's own state layer;
motion is app-wide and off under reduced motion; three people in one notebook
with no leakage; export, wipe, restore, merge and the standalone decryptor all
proved; the navigation bar, the selected destination, every empty state, and
the eight findings on #388.

**Enhance, do not replace, everywhere except projects.** The owner set that
rule and then exempted projects himself, which is why #399 says reimagined.

═══ THE BAR ═══

Material 3 Expressive is the floor, not the finish. A screen that is correct,
on Material's components, and plain is a 7 out of 10. `docs/V4.md` section 6 is
the bar: eleven checkable items, and 6.2 is a six step pass run against a real
capture rather than against the code. Rule 14 makes it retroactive.

Every new screen is built twice on purpose: built on Material's own components
and Google's own assets, D196, then taken back through the eleven for visuals,
fonts, styling and spacing, so the whole app feels like one application.

Consistent where appropriate means these by name, and a new screen inventing
its own version of any of them is two answers to one question: D198 color,
D200 the add control, D201 the navigation bar, D202 the selected destination,
D203 actions, D204 a tracked thing's own hue, and `SectionEmpty`.

Before drawing any measure screen, read `docs/TRACKED-THINGS.md`. It is the
research, with sources, and it is mostly about what rule 2 rules out.

═══ HOW TO WORK ═══

OPEN THE SESSION ONCE:
  adb shell dumpsys window | grep isKeyguardShowing   # must say false
  tools/sweep.sh audit                                # seeds, walks, captures
  python3 tools/checks/run_all.py
Look at every capture. Notes go on the issue you are on.

ONE UNIT PER COMMIT: tools/verify.sh → install → LOOK AT IT ON THE PHONE →
commit → push. Never leave the tree dirty. An increment ends when origin/main
has it.

VERIFY AT THE RIGHT DEPTH:
- Package move, no pixels        → compile + checks
- Component rewrite              → compile + checks + look at one caller
- Screen rewrite                 → tools/verify.sh + install + walk it
- Phase gate                     → verify.sh + sweep + dark + font 2.0 + reader

READ THE REAL EXIT CODE. A command piped through grep and then `echo $?`
reports grep's status, or head's. Redirect to a file and read `$?` from the
command itself.

MEASURE, DO NOT JUDGE. Captures are 1080 wide at 3x, so dp is px/3.

TAKE A BASELINE BEFORE BLAMING YOUR OWN CHANGE. Build the pre-session commit in
a worktree and run the same class:
  git worktree add /tmp/baseline <commit>
  cd /tmp/baseline/android && ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>

AN EMPTY NOTEBOOK COSTS THREE TAPS: More, people in this notebook, add another
person. Every section is empty behind it and the first person's notebook is
untouched.

NEVER SWITCH ANDROID USER PROFILES. It re-locks the phone and there is no way
past a secure keyguard from here, #316.

═══ TRAPS ═══

- The Material role names are inverted in this theme. Theme.kt maps
  displayMedium to type.hero at 30sp and displaySmall to type.displayM at 36sp.
- surfaceContainerLow is the canvas on light. A card drawn on it is invisible,
  and so was a tonal button in sand on a sand block.
- Count callers by use, not by import. SearchDoor sat built and uncalled.
- 13 instrumented failures are PRE-EXISTING, on #394, plus #391. Do not
  attribute them to new work without a baseline.
- Test counts come from the log or the XML, never a gradle exit code.
- Never compile while the instrumented suite runs. It also uninstalls the app.
- FROZEN, never edit: ProjectDetailScreen.kt, CaptureSheet.kt, PinnedGroup.kt.
  Frozen tail: Confirm, StepRow, Tile, Press, Spine. #399 replaces the first
  through docs/REMOVAL-LEDGER.md rather than extending it.
- A weighted child of a column with no bounded height measures to zero.
- material3 1.5.0-alpha26: no ButtonGroup, SplitButton or ToggleButton.
  Expressive components need @OptIn(ExperimentalMaterial3ExpressiveApi::class).
- adb: /home/Kamsiob/Android/Sdk/platform-tools/adb. Nav bar y=2252,
  x=133/400/670/940. Screenshots crop 132px of status bar; add it back before
  tapping.
- tools/sweep.sh's closing list globs its prefix and will print a capture hours
  older than the run. Check the timestamp.
- Rule 19: a phone setting may change only if the prior value is recorded first
  and restored exactly after, including on failure.

═══ DONE ═══

Every screen answers all eleven of docs/V4.md 6.1. The accessibility gate in
DESIGN.md 12 clears with the reader on and the font at maximum. The archive
carries every new row across two people. The suite is no worse than the 13. The
APK is delivered and walked on the phone.

Work the list without stopping. Rules 9 and 10: three attempts then log to
DECISIONS.md BLOCKED and start the next item; decide rather than ask. Keep
HANDOFF.md, DECISIONS.md and the issues current as you go, because the session
is cleared between phases and they are what survives.
```
