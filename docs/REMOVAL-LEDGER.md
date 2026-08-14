# REMOVAL-LEDGER.md, Health Trail by Kamsiob

**Nothing in this repository is deleted because a newer thing replaced it.** Superseded code is **frozen**: never called, never extended, never fixed, and never translated. This file is the record of what was superseded, by what, and when, so that a session reading a frozen file knows immediately that it is reading history rather than a live path.

**Created 2026-08-04** with the adoption of the Projects and Today grids, D112. The rule was being followed in practice and had no file to record it in.

## Why freezing rather than deleting

**A deletion loses the reasoning along with the code.** Six months later somebody re-derives the same approach, hits the same wall, and has no way to know it was already tried. The commit history holds the diff but not the judgment, and nobody greps a repository's history before writing a screen.

**And a frozen file is honest in a way a deleted one is not.** A screen that still exists but is never reached is visible to anybody reading the source, which means the question "why is this still here" gets asked and answered, rather than the answer being lost with the file.

## What the rule means in practice

- **Never called.** Nothing in the live path may reference a frozen file.
- **Never extended.** A frozen screen does not gain a feature because it was convenient.
- **Never fixed.** A defect in a frozen screen is not a defect: it is history. If it must be fixed, it is not frozen, and this file says so.
- **Never translated.** A frozen string does not consume a translator's time or a catalog key.
- **Its tests go with it.** A test asserting frozen behavior is frozen too, or removed with a row here saying which.

## The ledger

| What | Superseded by | When | State |
|---|---|---|---|
| `ui/screens/ProjectDetailScreen.kt` | `ui/screens/ProjectHomeScreen.kt` | 2026-08-05 | Frozen. Not called: `NotebookShell` opens the new screen. |
| `ProjectDetailTags`, and the tests that use them | The new screen's own tags | 2026-08-05 | Frozen with the screen. |
| `ui/screens/CaptureSheet.kt` | The capture bloom in `NotebookShell` | 2026-08-13 | Frozen. Not called: the gold button blooms its six choices in place, which is what grid screen 04 always drew. |
| Its case in `ScreenReaderTest` | The bloom's own walk in `CaptureTest` and `BackJourneyTest` | 2026-08-13 | Removed with the screen, per "its tests go with it". |
| `ui/components/PinnedGroup.kt` | `PinMark` and each screen's own lead group | 2026-08-13 | Frozen. Not called: pinning shipped drawn per screen rather than as one group. |

**The Projects conversion has begun and its first row is above.** What else it makes obsolete, and what will appear here as each conversion lands:

- **The current Today dashboard**, superseded by `reference/today-grid.html` and `DESIGN.md` section 21. Today becomes a lead slot plus a field of card instances the person arranges.
- **The rest of the checklist Projects**, superseded by `reference/projects-grid.html` and `DESIGN.md` section 20. A project becomes three answers with three shapes, and the checklist survives as one of the three rather than as the whole.

**On the frozen project screen specifically.** It carried real work the new screen did not do: adding, editing, moving and removing a step, setting the status, setting what the project is waiting on, and saving the project as a template. **None of that was deleted and none of it is lost.**

**Checked on 2026-08-06, after #280 and #291 closed, rather than assumed.** Four of the six came back: the steps are edited from `ProjectStepsScreen` and `StepEditSheet`, and the status is set from the setup screen's chips.

**Two did not, and this paragraph used to say they had.** Saving the project as a template and setting what it is waiting on both still had their repository call and their shell state with **nothing anywhere that set them**. That was **#314**, and it was written down here because a ledger that quietly overstates what came back is worse than no ledger: the whole value of freezing rather than deleting is that somebody can check.

## The ledger, second table: a frozen symbol inside a live file

**Added 2026-08-13, D152, working #371 item 8.** This file was file shaped, and said so, and that was fine while the only superseded things were whole screens. It stopped being fine when the audit counted repository functions with no caller: a superseded writer sitting in the middle of a 8,000 line file the app uses every day cannot have its own file, and the alternative was a paragraph of prose that no check can read.

**Same rule, smaller unit.** Never called, never extended, never fixed. A row here says the symbol is history rather than an oversight.

| What | Where | Superseded by | When |
|---|---|---|---|
| `setBillState` | `data/Repository.kt` | `updateBill`, which writes the state with everything else the correction form asks | 2026-08-13 |
| `setTodayCardSize` | `data/Repository.kt` | `setTodayLayout`, which writes the whole arrangement in one transaction | 2026-08-13 |
| `setMedicationOnEmergencyCard` | `data/Repository.kt` | `updateMedication`, which takes `onEmergencyCard` alongside the rest | 2026-08-13 |
| `peopleOnEntry` | `data/Repository.kt` | `EntryDetail`, which carries the people it already read | 2026-08-13 |
| `QuietAction` | `ui/components/FoldRow.kt` | `QuietButton`, and `TextAction` is its second name | 2026-08-13 |
| `neutralHue` | `ui/components/TabChip.kt` | `wholeAppHue`, for a surface that belongs to no section | 2026-08-13 |
| `DragHandle`, `GRIP_TARGET`, `GRIP_WIDTH`, `GRIP_GAP`, `GRIP_STROKE` | `ui/screens/TodayFieldScreen.kt` | The card itself, which is what a finger carries once Today is being arranged. D153 | 2026-08-13 |

**The audit counted twenty two and the real number is six**, which is the finding rather than a correction to a tally. **Most of what looked dead is reached by a test and by nothing else**, and that is a different thing entirely: `columnForTest`, `revisionForTest` and their fifteen siblings exist for the tests by name, and `addToIncident`, `threadsForEntry`, `openIncidentCount` and `fillProjectPaper` are covered by tests while the screen that should call them is not built. **A function a test reaches is a promise the app has not kept, not code nobody needs.**

**On the grip specifically.** It was a 48dp target in the corner of a card that is itself a target, and the owner's note on #376 is why it went: nobody grabs a handle to move an icon on the phone they already own. **Nothing was lost by it going.** The reorder path that has to exist is the worded move up and move down in the card's options sheet, which works one handed, with a reader on, and with switch access, and 23.2 is why that path rather than the drag is the one that must exist. The drag was always the shortcut.

**Two were neither superseded nor covered and were deliberately not frozen: `renameChapter` and `renameProject`. Both have callers now**, 2026-08-14, #374. They were the app's missing half rather than history, waiting on B6 rather than on a decision, and freezing them would have said the app decided somebody may not fix a name they typed wrong. **The judgment not to freeze them held**, which is the useful part: a ledger row would have made the next reader stop looking.

**What they cost the shell, since that was the whole reason they had no caller**: 220 bytes for both full screen surfaces together, against the 1,860 bytes of headroom the shell had before #373, when one added parameter on an existing screen was enough to fail the build.

**As of 2026-08-13 they were on #371 and waiting**, described here as the same defect the care thread rename already fixed. That sentence is kept because it is what was true then and it is why they were not frozen.

### What is unreached and is not history, 2026-08-13

**`ui/components/RoundCard.kt` has no caller and does not belong in this ledger.** It is grid screen 20, one round of lab work, and it is waiting on a schema decision rather than on a screen: `contract/schema.sql` has no test, no round and no result, which is #182 and #199 and is the owner's under rule 3. **It is built ahead of its data, not superseded by anything**, and freezing it would tell the next reader the opposite of what is true.

**`updateReading` and `updateMeasure` each had a row here for about an hour and both are gone**, because their screens landed the same night. **Worth keeping the sentence**: this section is for things waiting on a screen, and the correct response to an entry here is to build the screen rather than to leave the entry tidy. Every writer of #374 has a caller now.

**`QuietAction` in `FoldRow.kt` and `neutralHue` in `TabChip.kt` have no callers either**, and both live inside files the app uses every day. **This paragraph used to end by saying the ledger could not express that**, and it was right until the audit found four more of the same shape in the repository. Both have rows in the second table now, D152, and the shape has a place rather than a paragraph.

### The capture sheet, 2026-08-13

**It was superseded on 2026-08-12 and had no row here until #371 asked for one**, which is the gap this file exists to close: a reader met a 227 line screen with no way to know it was history.

**Its live half moved out rather than the rule being bent.** `CaptureSheet.kt` also held `CaptureKind`, which nine files name, and `CaptureTags`, which the bloom draws and `BackJourneyTest` walks. A frozen file is never called, and that cannot be true of a file the live path imports from, so the enum and the tags are `ui/screens/Capture.kt` now and the frozen file holds only the superseded screen.

**`check_reader_coverage.py` reads this ledger now**, the way `check_dead_gestures.py` already did. Otherwise freezing a screen made that check fail, which would have meant either keeping a walk of a screen nobody can reach or writing the same list of frozen files a second time. D133.

**All six have come back as of 2026-08-06.** #314 built the last two onto the setup screen, where they were placed by looking at that screen rather than by copying the old one: both are pills rather than full width buttons, and the template action is a headed section of its own. D118. Verified on the phone at both themes, at font scale 2.0 and in Arabic, end to end in both directions: a name saved and survived a full app restart, and a saved template turned up under YOURS in the template library with its lineage kept.

**The frozen file is not extended to bridge the gap**, which is the whole point of freezing it.

**A row is added at the moment the code is frozen, not at a phase gate**, which is the same discipline rule 12 applies to design reviews and for the same reason.
