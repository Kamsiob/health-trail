# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-09.

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

### Verified rather than asserted, 2026-08-09

- The working tree is clean and everything is on `origin/main`. **Check rather than trust**: `git status --porcelain`.
- **17 repository checks pass**, `python3 tools/checks/run_all.py`.
- **176 unit tests pass and they need no phone.** On a day when the device is unreachable this is most of what is left, and it is worth writing logic where these can reach it rather than only into a composable.
- **505 instrumented tests pass**, last full run 2026-08-09 on the unlocked phone.
- **Continuous integration is green on `main` at the tip.** Check after every push: `gh run list --branch main --limit 3`.
- **The phone is attached, installed, seeded and at its starting values**, each read back rather than assumed: font scale 1.0, animator null, heads-up 1, no per-app locale, the accessibility services string the KDE Connect one, the app theme following the phone.
- **The destructive command guard is live and proven.** It refused two real commands on 2026-08-08, a recursive directory removal and an app data wipe, both correctly. Section 9.

### The five that actually get broken

1. **Run `tools/verify.sh`**, not the checks you happen to remember. It is the only runner that compiles the instrumented sources and runs lint.
2. **An issue closes only on device verification**, per `DESIGN.md` 16.4: both themes, font scale 2.0, right to left, and every state including the empty one.
3. **Commit and push after every working increment**, and check CI after each push. An increment ends when `origin/main` has it.
4. **The fixture must produce rows the app itself could write.**
5. **Look at the screen before closing anything.** Five defects on Today on 2026-08-08 were invisible in the code and obvious in a screenshot.

## 6. What is built

**Design direction v4 is adopted and most of the app is in it.** `reference/screen-grid.html` is the v4 grid. `DESIGN.md` was rewritten rather than patched.

- **Step 1, the foundation: complete.** Every token in both themes, the type scale with all three faces verified per locale, the geometry, and all sixteen components. #149 through #168 closed.
- **Step 2, the four destinations: complete.** #169 through #172 closed.
- **Step 3, the section screens: complete but for #182**, which is blocked. Fourteen closed on device verification.
- **Step 4, the detail screens: thirteen of twenty closed.** #189 through #198, #200, #201 and #202. #199 is blocked; #203 through #208 are untouched.

**#192, one medication, closed with its remainder split out rather than left vague.** Its questions are built and the fixture never exercises them, **#229**; its incidents cannot be expressed because the schema has no link from an incident to a medication, **#230**, which is the owner's call.

**Milestone 1, Today: the surface is built and verified, and fourteen issues closed on the device on 2026-08-08.** #292, #270, #269, #247, #248, #250 through #257 and #261. The 16.4 checklist was walked in full: both themes, font scale 2.0 with the baseline restored, Arabic right to left with the app restarted so the catalog switched, the empty state from a cleared install, 479 instrumented tests, and TalkBack bound against the app.

**Nothing on `main` is unverified now.** #260's document thumbnails were the last one and they have been on a screen: full and few rungs, both themes, font scale 2.0 and Arabic right to left. **They were drawn at 24dp, which is a spacing token borrowed as a dimension and is a speck**, so the card claimed to show the person's paper and showed a dot beside a title. They are at the component's own `ROW_SIZE` now, and the rows have room between them so three pages do not read as one strip.

**The one rung not seen today is this card's empty state**, which needs a cleared install: the destructive command guard refuses `pm clear`, correctly, and `tools/seed.sh` always restores a notebook. The empty Today was walked from a cleared install on 2026-08-08 and a card with no items has no thumbnail to get wrong. Said here rather than ticked.

**#258 is done and walked on the phone.** The care team card now has both variants 21.7 draws: one chosen person with their number as an outlined pill, and the row of everyone as avatars with an overflow mark. The source picker is in the card's options in edit mode. Verified at both themes, font scale 2.0, and Arabic right to left, with all four rungs on screen at once from the fixture, which now writes three sourced cards on purpose.

**Three real defects came out of that and all three are fixed.** Each was invisible in the code.

- **A phone number with a space in it never reached the dialer.** `Uri.fromParts` escapes nothing, so `tel:555 0142` opened an empty keypad. **This was every number in the app**, not just the new card: the care team screen and the emergency card share the helper. `docs/TRAPS.md` section 5.
- **A control drawn inside a Today card was unreachable by a screen reader**, because the card clears its whole subtree to speak as one sentence. The card now clears its answer only, and carries its one inline action in a slot beside it.
- **"+12" rendered as "12+" in Arabic**, because a plus is a neutral character and takes the paragraph direction. A remainder became a floor.

**#259 is done and walked.** The trail card draws its last three entries as a mini spine at tall, on the same route with the same node colors the trail itself uses, each row carrying the date and the kind because a color never carries meaning alone. **The gap markers are the reason it is drawn rather than listed**, and they cannot be reached from any seed: every fixture ends its history on the day it is generated, so the head of the trail is never quiet. They are held in `TodayFieldScreenTest` and the issue says so.

**And the fixture had a defect that showed up here.** Every project's latest word landed on the same day, so **the three newest entries in the whole notebook shared one date at every horizon**: the spine said "June 29, 2026" three times and every node was the same color. Three separate offices do not all call back on the same afternoon. They are nine days apart now.

**#272, the add-a-card gallery, is done and walked.** **Every entry said "Nothing waiting"**, whatever the record held, because the previews were looked up from the answers of the cards already on Today and the gallery only ever offers the ones that are not. The answers are read when the sheet opens now, through the card's own wording so a preview and its card cannot disagree.

It is grouped under the binder's own section names with the situation's suggestions first, and **the whole row adds the card rather than an outlined Add beside it, which is D123** rather than an oversight: rule 23 takes the easier target.

**And it was truncating its own previews at font scale 2.0**, mid-word and with no ellipsis: "passed 75 days", "4 steps in the". D105 says a second line somebody reads to choose wraps, and it does now.

**What is left in milestone 1, and each says why on its own issue:** screens #293 through #301, and the rest of the gallery #272.

**What the screen showed that the code could not.** Every one of these passed the compiler, the checks, lint and the instrumented suite, and every one was obvious in a screenshot.

- A card's tab ran underneath the corner chevron and ellipsized behind it. Nothing collides until the text is long enough.
- A tall card reserved 168dp whatever it had to say, so a measure with one reading was a hundred points of empty box.
- The measure query read `value_text` only, which is null for a measure recorded as a number, so a weight card with a hundred readings said "No readings yet" directly above its own chart.
- "and 5 more" subtracted clusters from steps, which is a sentence about nothing.
- The reader's sentence had nested isolate marks, and announced three readings the screen was not showing.

**Two of those were found only because `walk.sh see` was fixed to read content descriptions**, and it had been reading half the tree.

**And `walk.sh see` has a limit worth knowing before you trust it: it shows the unmerged semantics tree.** A card that merges its parts into one sentence still prints every part there, which reads as a reader stopping six times to learn one thing and is not true. Twenty minutes went into a defect the tool invented. **Only the Compose test API sees the merged tree**, which is the one a reader walks. `docs/TRAPS.md` section 5.

**THE ARCHIVE is largely built and proved on real hardware**, not asserted: a two-layer container at format version 3, a readable copy of 61 pages, a standalone decryptor at `tools/decrypt/` tested in CI, and the format published byte for byte in `contract/EXPORT-FORMAT.md`. `docs/RUN-LOG.md` has the account and what each piece was proved with.

---

## 7. What keeps going wrong

**Moved to `docs/TRAPS.md` on 2026-08-08, and the move is the point.** Every trap in it cost real time at least once and they are all worth keeping, and a session that loads all of them at the start has spent a third of its context on device warnings before finding out whether it is doing device work.

**It opens with a table: read the one section that matches what you are about to do.** Touching the phone, running the tests, changing copy, writing a check, changing a screen, committing, or anything about this machine. Section 1 above carries the five that apply to almost every session.

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

**All sixteen of them, oldest first.** Rechecked against the board on 2026-08-08 with `gh issue list --label needs-design-review` rather than remembered, because a list that is only partly a list is the defect this section exists to prevent.

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
| Why something is the way it is | `DECISIONS.md`, D1 through D122. Search it, do not read it |
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

**Nothing on `main` is unverified.** #260 was the last one and it has been walked. Section 6 has what the screen showed that the code did not.
