# HANDOFF.md, Health Trail by Kamsiob

**This is the current state of the work, and nothing else.** Read it before doing anything, every session, per `CLAUDE.md` rule 1. It is rewritten to current truth rather than appended to, so nothing in it describes a state that has passed.

**The history moved to `docs/RUN-LOG.md` on 2026-08-04** and this file was cut from sixteen thousand words to something a session can actually read. Do not put narrative back in here. If an account is worth keeping, it goes in the run log, in `DECISIONS.md`, or in the commit message.

**Last rewritten:** 2026-08-05.

---

## 1. Where to start

Everything below is verified rather than asserted, as of 2026-08-05:

- The working tree is clean and everything is on `origin/main`. **Check it rather than trusting this line**: `git status --porcelain` and `git log --oneline -5`.
- **17 repository checks pass** (`python3 tools/checks/run_all.py`).
- **Continuous integration is green on `main`.** It had been **red for three commits**, from `050ac27` to `b40e6ac`, and nothing said so: the last green before that was `c99bff5`. **Check it after every push**, `gh run list --branch main --limit 3`, because the tree being clean and the checks passing tell you nothing about it.
- **356 instrumented tests pass**, last full run 2026-08-05 after Today's card field landed.
- **The phone was unplugged on 2026-08-05 at the owner's request, at a clean point.** It was left installed, with the month six fixture restored through the app's own importer, font scale 1.0, night mode on, animation scale unset, no per-app locale, and TalkBack off. Every one of those was checked against the value it had at the start of the run, not assumed. **Confirm it is attached before planning any device work**: `adb devices`, then `tools/seed.sh`.
- **`connectedAndroidTest` uninstalls the app every time it runs**, and that bit three times in this run: a seed against a phone with no app fails with one word, and the next thing you look at is the launcher. Reinstall and reseed after every suite.

**Take these in this order.**

1. **A new design direction arrived on 2026-08-04 and both surfaces are largely built to it.** Section 2 is the whole of it: the data work, the five components, all three project shapes, the projects list, Today's lead slot and card field, edit mode, the gallery, and the three project sheets. **What is left is named at the end of each subsection.**
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

### 2.1 #262, the data work: landed, not yet seen on a screen

**The database now holds what a person arranges.** `contract/schema.sql` is at **version 2** and `HealthTrailDatabase.SCHEMA_VERSION` moved with it.

**Six new tables**, each with its live view, both change log triggers, and its indexes: `today_card`, `project_stage`, `project_standing`, `project_date`, `project_date_kind`, `project_paper`. **Four new columns**: `project.lead`, `project.current_stage_id`, `project_step.cluster`, `project_step.handler_label`.

**Three things in it are worth knowing before touching either surface.**

- **The lead slot is singular in the database, not only on the screen.** A partial unique index, `ux_today_card_lead`, refuses a second lead per subject. Zero is the application's to prevent, because a database cannot require a row to exist. It is the schema's first `CREATE UNIQUE INDEX`.
- **`today_card.source_table` and `source_id` are deliberately not foreign keys.** 8.7 requires that a card whose source is gone is kept rather than dropped, and a foreign key would make import the thing that quietly edited somebody's desk.
- **No column marks a project's important date.** The screen leads with the soonest date that has not passed, and the most recent one when they all have. D113 says why, and what that gives up.

**The migration is real and is proved against a version 1 database**, not against today's. `Migrations.steps` has its first entry; `Step.apply` now receives the contract schema text alongside the database, so an additive step replays `contract/schema.sql` rather than carrying a second copy of six table definitions that would drift. `MigrationTest` builds a hand-written version 1 database and asserts the tables arrive, the columns arrive, and the project and its step survive with nothing invented in the new columns.

**The repository layer is built too**, and the trust model is tested rather than asserted. `ArrangementTest` is 16 tests covering the part a screenshot cannot show: the lead is singular, promoting demotes, the lead cannot be removed, removal is a tombstone, the layout does not reorder itself between reads, a broken layout is reported rather than repaired quietly, and a card pointing at a tombstoned project keeps its reference.

**Three test accessors were added and are named so nobody reaches for them by accident**: `columnForTest`, `clearEveryLeadForTest`, and `tombstoneForTest`. The middle one exists to produce a state the app cannot, which is the only way to prove the reader reports a broken layout instead of inventing a lead.

**349 instrumented tests pass**, up from 332. **All 17 checks pass, 167 unit tests pass, lint is clean.**

**Four tests were failing for reasons worth knowing.**

- **`SituationPickerTest` was broken by `11c5a4e` and nothing caught it**, because the instrumented suite was last run before that commit. The picker renders every catalog subtitle through `Bidi.isolate`, so the rendered string carries U+2068 and U+2069 and no longer equals the bare catalog string the test compared against. **Found by opening the app and reading the screen**, not by reading the code: `tools/walk.sh see` prints the isolate marks. The test now compares against `Bidi.isolate(subtitle)`, which is what the screen actually draws. **#226's worklist will keep producing this**, so expect it.
- **`BackJourneyTest` was broken by the new `ArrangementTest`**, four classes later, with nothing to connect them: the new test did not close the repository in an `@After`, and the repository is a singleton over the one real database. Every other data test here closes it. It does now.
- **Three counts were written down in tests and are now counted from the contract**: the table, view and trigger counts in `FoundationSmokeTest` and `DatabaseTest`, and the trigger count in `SchemaStatementSplitTest`. A number in a test file means every table added to the contract fails a test for a reason that has nothing to do with what the test is about. **They assert the shape now instead**: one live view per user data table, two triggers each.
- **`MigrationTest`'s synthetic steps sat at a fixed version 2**, which stopped running the moment `CURRENT` reached 2, so three tests passed while proving nothing. They are at `CURRENT + 1` now.

**The fixture writes all of it now, and the archive carries it.**

- **`tools/fixtures/generate.py`** writes the road, where it stands with its history, dates on both sides of today with their sources, date kinds, papers both filled and empty, step clusters and handler tags, and a seventeen-card Today layout. All three project leads exist, because two of the three project home screens cannot be looked at on the phone otherwise.
- **A Today card deliberately points at a finished project**, which is the source-closed rung of the states ladder.
- **The future dates are written as `UPCOMING_DAYS` offsets**, not as small numbers. The history ends on a fixed date, so "+21 days" stops being in the future three weeks later and the upcoming rung disappears silently. They go stale on the same day the upcoming appointment does, and moving `HISTORY_ENDS` fixes both.
- **`check_fixtures.py` now fails if any of it stops being written**, including if the dates stop falling on both sides of today. **Each new assertion was proved by breaking the generator on purpose**, not by watching the check pass.
- **`RegenerationTest`'s notebook now contains the arrangement**, so export, import, and re-rendering the readable copy byte-identically actually covers these tables. It did not before, and it would have kept passing.

**All sixteen templates carry the five defaults, and starting one applies all five.**

- **Only three fields were actually missing.** `steps` was already the starting steps and `documents` was already the usual papers, so they are used as those rather than duplicated. The new fields are `lead`, `stages` and `date_kinds`. `templates/SCHEMA.md` says which field is which default.
- **`check_templates.py` holds them**: `lead` to the same closed set the schema's CHECK enforces, `stages` to at least two, `date_kinds` to non-empty. Proved by breaking the data on purpose.
- **`startProject` applies all five in one transaction**, and it applied only the steps before, so a project started from a template had no road, no chips and no papers.
- **A person's own template carries the whole shape now too.** `saveProjectAsTemplate` wrote only the steps, so somebody who shaped a project over months and saved it got the checklist back and nothing else. Bodies written by older builds have none of the new keys and fall back to empty, which is what those templates actually held.

**#262 is done apart from device verification.** **That is the next work and it is #263 onward**, not a remainder of #262.

### 2.2 The five Projects components exist and have not been seen

**Built and compiling**, #263 through #268: `RoadStrip`, `StandingCard`, `DateRow`, `LatestWordCard`, `StepRow`, `ReferenceLine`. Each carries its when-to-use and when-not-to-use, per section 19.

**Five of the six are now on a screen and have been looked at**, on the project home. `ReferenceLine` has not: nothing has a reference number to render, which is the owner decision in #303.

### 2.3 The long road project home is built and seen

**`ProjectHomeScreen`, #278**, the grid's screen 05. The three answers in order, then steps and papers folded and counted. Verified on the phone at both themes, at font scale 2.0, and in Arabic. Screenshots are `project-home-dark`, `project-home-light`, `project-steps`, `project-max-font`, `project-arabic`.

**`ProjectDetailScreen` is superseded and frozen**, with its row in `docs/REMOVAL-LEDGER.md`. It carried real work the new screen does not do yet: adding, editing, moving and removing a step, setting the status and what the project is waiting on, and saving as a template. **None of it is lost and none of it is bridged by extending the frozen file**; it belongs to the busy stretch and the setup screen, #280 and #291.

**Two defects were found by looking at it rather than by reading it.** The countdown at 22sp beat the holder at 18sp, so a project whose whole shape is that it leads with where it stands was led by its date; `DateRow` gained `prominent` and only the closing window draws the number large. And the step checkbox was a glyph in a fixed box, so at font scale 2.0 the tick was cut in half and read as a damaged control, on the setting this audience is most likely to be using. It is drawn now. **Neither is visible at font scale 1.0 in English.**

**A third was found in the semantics tree and is invisible in any screenshot.** `Bidi.join` isolates every part it is given, so passing it a part that was already isolated nests the marks, and passing it a joined string nests them three deep. The rule is: **raw parts into `Bidi.join`, never pre-isolated ones.**

**Where a project stands can be recorded now**, #281. The standing card carries an outlined "Update where it stands", and the none-yet rung carries the same one, so a project nobody has said anything about names its one action rather than only saying nothing is known. `docs/screenshots/project-standing-sheet-light.png`.

- **A one-stage sheet**, because this is what somebody does in a corridor right after a call.
- **The chips are the project's own history**, the holders it has actually been with, most recent first. There is no project people roster to draw from, and asking somebody to fill one in first would be the app charging admission for a sentence.
- **Since when is today and is not asked for.** A person recording this five minutes after the call knows when it happened. It stays editable from the entry, rule 17.
- **Partial saves.** Somebody who knows only that it is with the county can write that and nothing else.
- Proved on the phone: saved, and the card's date became today.

**A project date can be written down now**, with where it came from on the same sheet. `docs/screenshots/project-date-sheet-light.png`. The kind is chips from the project's own date kinds and free text besides; the date carries whatever precision the person gave it; the source may wait, because somebody who has a deadline but has not written down which letter it came off should not lose the deadline over it.

**The action is offered whether or not there is already a date.** It appeared only on the empty state at first, and a project with a filing deadline still gets a hearing date: an action that shows only on an empty screen is one nobody finds twice.

**D113 was seen working end to end.** Writing an August date onto a project whose only date was in October moved the card to the August one, because the screen leads with the soonest that has not passed and nothing had to be told which date mattered.

**A call can be logged from inside the project**, which is what makes the third answer writable, and it writes the entry and the link together. `docs/screenshots/project-log-call-light.png` and `project-after-call-light.png`. **This closes the app-side half of #303**: the read path existed and nothing wrote the link, so on a real notebook the latest word was permanently absent. **The reference number half of #303 is still the owner's**, and `ReferenceLine` still has never rendered with real data.

- **Pre-answered with the project**, and the project's name is shown back so nobody logs a call against the wrong process.
- **The attribution names who said it**, not only when. The date alone is the half a person does not need when they call back and are asked who they spoke to.
- Proved on the phone: logged a call, and the latest word card became it, dated today, with "Denise, intake caseworker" beside the date.

**The nested isolate defect appeared a third time** and was fixed at the call site again, not at the source. **Making `Bidi.isolate` idempotent was tried and reverted**: a joined run like `⁨a⁩ · ⁨b⁩` also starts with the isolate and ends with the pop, so the guard would have stopped wrapping compound runs as a unit, which changes how they lay out inside an RTL sentence. That is a real behavior change across every screen and it was not verifiable in one sitting. **The rule stays: raw parts into `Bidi.join`, never something already joined.**

**The project's setup screen is built and seen**, #291, and it is law 5 made concrete: the shape as three chips, what the template decided with what it has become, and the status. `docs/screenshots/project-setup-light.png` and `project-shape-changed-light.png`.

- **Changing the shape reorders the project home with no penalty**, proved by tapping a chip and watching the countdown take the lead. There is no confirmation, because asking whether somebody is sure would invent a consequence that does not exist.
- **Status is back**, which the frozen `ProjectDetailScreen` used to carry, and **the waiting-on note is left alone** when it changes: the old screen cleared it on every status change, which threw away who somebody had been told to wait for.
- **The way back names the project, not the projects list.** It said the list at first, which is the small lie somebody only notices by being surprised.
- **The four rows are read-only summaries.** Editing stages, steps, papers and date kinds one by one is not built, and the grid draws chevrons on them.

**The road can advance now**, #285's sheet. `moveProjectToStage` had existed since the schema landed and nothing called it, so a project sat on whatever stage it started on forever. `docs/screenshots/project-stage-sheet-light.png` and `project-road-turned-light.png`.

- **Every stage is offered, not only the next one.** These processes skip stages and go backward, and a control that only moves forward one step is wrong the first time something unusual happens, which on these processes is most of the time.
- **A stage already reached keeps its first arrival date**, so a road that turns back does not erase that it had ever been there.
- **The current stage is derived from what has been reached**, the same way the road strip derives it, so the sheet and the strip cannot disagree.
- **The road strip stays bare**, 20.6. The control sits beside it rather than making the waypoints tappable, which would turn an information graphic into a picker.
- Proved on the phone: moved a project to Decision and watched two waypoints fill and the reader say "Stage 3 of 3".

**Every piece of project data can now be written from the app**: where it stands, a date with its source, the latest word with its link, the shape, the status, and the road. **What is still read-only** is the stages, steps, papers and date kinds as lists, which is #291's remainder.

**The project shows everything said about it**, #284's fold: "What was said" with a count, opening onto every linked entry most recent first, each a door. **Rule 18 is satisfied both ways** for the project-to-entry link now; before this a person who logged six calls could see one of them.

**#306 was fixed rather than filed and left.** `AppLanguageTest` asserted Japanese falls back to English and twice got Spanish, which is what the test before it had chosen: setting `applicationLocales` is asynchronous and the next line read whatever was still in place. It now waits for the system to report the language it asked for, **and waits for the request rather than for the answer**, because polling until the expected result appears would make the assertion prove itself. Three runs of the class and a full suite, clean.

**#302 is still open**, and it is the other flaky one: `BackJourneyTest` failed once in a full suite and passed the next run.

**The review this owes under rule 12 is #304**, and it lists what was deliberately left out and the three things I am unsure about.

**All three shapes are built and all three have been seen**, #278, #279 and #280's home screens. **The shape is only the order of the same components**, which is 20.3's whole claim, so `Repository.Project` carries `lead` and the screen orders itself from it. The long road opens with where it stands, the closing window with the countdown at `monoL`, the busy stretch with the steps cluster already open above the answers. `ProjectHomeScreenTest` asserts the vertical order for each, because every shape shows the same four things and a wrong order still looks like a finished screen. Screenshots: `project-home-*`, `project-window-dark`, `project-busy-dark`.

**The fixture's project content is matched to its projects now.** The stages, date kinds, papers, standing entries and office words were each written once in a generic order and indexed by project position, which put an appeal's stages and an appeal deadline on a power of attorney. **A fixture that puts the wrong words on a screen makes a correct screen look broken**, and every one of those was found by opening it rather than by reading it.

**The first three projects cover all three shapes on purpose.** `PROJECT_STATES` puts the fourth and fifth in done and abandoned, which fold away, so a shape assigned to one of those is a shape nobody looking at the fixture will ever open.

**The busy stretch clusters its steps by area**, which closes #280. Each area is a mono eyebrow with a count of how many steps are in it and a hairline out to the end edge, the same `GroupHeaderText` the trail heads its months with, and a step nobody has filed keeps its place in a run after the named areas rather than being hidden until it is tidy.

**The count says how many steps are there, not how many are done.** The grid draws `1 OF 3`, which is a completion count, and rule 13 rules that out in its own words. The screen shows the plain count instead and **`DECISIONS.md` D116 puts the difference in front of the owner**, because the grid supersedes `DESIGN.md` for this surface and rule 13 is a hard rule that the adoption did not name. One line in `ProjectHomeScreen` changes it if he meant the drawing literally.

**Two fixture defects came out of building it, both the same shape as one already fixed.** Four hospital discharge areas were handed out round robin to any steps led project, so "The house", "The ride" and "Equipment" sat over three steps of a power of attorney with one row under each. Areas are indexed alongside the steps now. And a steps led project generated two to six steps, so **the busy stretch was never busy and the clustering had nothing to cluster**: it generates seven to ten. Both were invisible until the screen was opened.

**358 instrumented tests pass**, up from 356. Both new assertions were proved by breaking the screen on purpose and putting it back from a scratchpad copy, per section 7. Seen on the phone at both themes, at font scale 2.0, and in Arabic: `project-clusters-light`, `project-clusters-dark`, `project-clusters-2x-dark`, `project-clusters-rtl-dark`.

### 2.33 The starting steps can be changed again

**Part of #291.** `addProjectStep`, `updateProjectStep`, `moveProjectStep` and `deleteProjectStep` have been in the repository since Phase 0 with nothing reachable calling them since `ProjectDetailScreen` was superseded. The setup screen said everything the template decided was changeable and offered no way to change any of it, which is the promise without the thing.

- **`ProjectStepsScreen` is behind setup's Starting steps row**, which is a door with a chevron now rather than a line, the way 20.5 screen 18 draws it.
- **`StepEditSheet` is what a row opens**, carrying the text, the note, move earlier, move later and remove. **One sheet rather than three controls per row**: three targets repeated down a list fails section 9 at font scale 1.0 and falls apart at 2.0. A control that would do nothing is not drawn, so the first step is offered no way to move earlier.
- **The list clusters by area the same way the project does**, so the list somebody edits is the list they read.
- **`project.step.handled_by` said "Me is handling this".** It was already wrong in the spoken description on the project home and the editor put it on screen where it could be seen. It is "Handled by {who}" in all four catalogs now.
- **The way back says "Back to setup"**, because that is where it goes.

**Still owed on #291:** stages, usual papers and date kinds are still read-only lines.

**The stages are done, and the road is editable.** `ProjectRoadScreen` sits behind setup's Stages row, which is a door now. It draws the same `RoadStrip` the project draws, above the list it edits, so what somebody changes is the thing they see rather than an abstract list that turns into it. Each stage opens `StageEditSheet`: rename, move earlier, move later, remove, the same shape as a step's sheet because it is the same job.

- **Removal was the part that needed thinking about.** `RoadStrip` works out where a project is from the stages themselves, so a project left pointing at a removed stage draws as having reached nothing and the road says the application was never filed. Removal moves the project back to the last stage before it that was actually reached, in the same transaction, and to no stage where there is none. `RoadEditTest` covers it against a real database.
- **Renaming keeps the arrival**, and the sheet says so on a stage the project has reached rather than leaving somebody to guess what they are about to lose.
- **One stage is a list and not a road.** `RoadStrip` needs two, so a project down to one draws the list alone.
- **The field and the button no longer say the same words.** Both the steps and the road screens had "Add a step" and "Add a stage" on the field label and on the control that acts on it, which is two nodes saying one thing to a reader. The field names what you type, the button is the verb.

**The date kinds are done too.** `ProjectDateKindsScreen` sits behind setup's Date kinds row, with `DateKindEditSheet` for rename and remove. These are the chips somebody taps when they write a date down, and a template that offered "Renewal" to a process that never renews left a chip in the way forever.

- **The list is what is offered next time and never a key into the record.** `project_date.kind` is the words the person used when they wrote the date down, copied at that moment, so renaming a kind does not reach back and rewrite what they recorded and removing one does not take the date with it. **The sheet says so**, because otherwise the only way to find out is to try it.
- **No reordering here**, unlike the road and the steps. A handful of labels offered as chips has no order somebody reads down, and a control that exists because the neighboring screen has one is decoration.
- **`projectDateKindRows` reads alongside `projectDateKinds` rather than replacing it**, so the chips keep taking labels alone and a caller that only offers them does not have to know they have identities.
- Verified end to end on the phone: removing a kind on this screen removes the chip from the date sheet.

**The usual papers are done, which closes #291.** `ProjectPapersScreen` sits behind setup's Usual papers row, with `PaperEditSheet` for rename, empty and remove.

- **A placeholder is a place, not a paper.** An empty one says "Waiting", never "missing", and **nothing on the screen counts the empty ones or chases them**: six placeholders with a count of how many are still unfilled is rule 13 pointed at somebody who is waiting on other people's post.
- **Emptying and removing are separate, and neither touches the document.** Taking the wrong paper out of the right place is the common mistake and must not require destroying the place; removing the place is a decision about how the project is organized. The document stays in the notebook either way, and the sheet says so, because the thing at risk is a photograph of a letter somebody may not be able to get again.
- **Emptying is not offered on a place with nothing in it**, so the sheet never draws a control that would do nothing.

**388 instrumented tests pass**, up from 373. Seen at both themes, at font scale 2.0 and in Arabic: `project-road-light`, `project-road-dark`, `project-road-2x-dark`, `project-road-rtl-dark`, `stage-edit-light`.

**373 instrumented tests pass**, up from 365. Seen at both themes, at font scale 2.0 and in Arabic: `project-steps-light`, `project-steps-dark`, `project-steps-2x-dark`, `project-steps-rtl-dark`, `step-edit-light`.

### 2.34 Starting a project shows what a template is before it creates anything

**#276 and #277 are built and seen.** Choosing a template used to create the project, its road, its steps, its papers and its date chips on one tap of a row, so the first time anybody saw what a template meant was on a project that already existed. **Nothing is created until Create now.**

- **`StartProjectPreviewSheet` is screen 04.** The road drawn rather than counted, then the starting steps, the usual papers and the date kinds each with a line saying what they are and that they can be changed, then which of the three answers the project will open with and where to change it, then the name.
- **Two stages, not three, and both say which they are.** The name is on the preview, pre-filled from the template, because a stage whose only question arrives pre-answered is a tap charged to everybody to serve the few who rename. The picker gained its own `1 OF 2` eyebrow, since the preview was announcing a stage nobody had been shown. `DECISIONS.md` D117.
- **The sixteen templates stay.** The grid ships four built-in bundles; this app has sixteen grouped into the same four kinds. Dropping twelve is an owner decision and this is not it.
- **`RoadStrip` no longer breaks a stage name in half.** At font scale 2.0 with four stages, each label gets about a quarter of the width, which is narrower than the word, and "Gathering" came out as "Gatherin" over "g". Below the width a name needs, the names run as one line under the road instead: nothing dropped, nothing abbreviated. **Measured with a text measurer rather than derived from the font scale**, because the derived threshold looked right and did nothing on the phone. This was one of #304's three uncertainties and it is fixed for the project home too, rule 14.

**Seen at both themes, at font scale 2.0, and in Arabic**: `project-preview-light`, `project-preview-dark`, `project-preview-2x-light`, `project-preview-rtl-light`, plus `project-start-light` for the picker's eyebrow. **365 instrumented tests pass**, up from 362.

### 2.35 The projects tab before the first project

**#274 is built and seen.** It was one gray paragraph under the subtitle with the rest of the screen blank below it, which is exactly the shape 5.17 already solved for every other empty screen in the app. It uses that solution now: the trail map ground, the line that says what this place is for at `displayS`, the paragraph under it, and the one thing to do.

- **`SectionEmpty` grew a lead and an action rather than a second empty state being written.** Seventeen screens call it and none of them changed: both parameters default to null. **Where a section's empty state is one line and a drawing, that is still what it is.**
- **The subtitle goes while the list is empty.** It describes what each row answers, which nothing on an empty screen does, and it opened with the same four words as the empty state's own lead. The screen said "the long processes" twice, one line above the other.
- **The bottom Start button goes too.** The empty state carries the action at the place the eye lands, and the two together were one control drawn twice on a screen with nothing else on it.
- **`EMPTY_HEIGHT_TALL` is 0.82 against the sections' 0.62.** A taller block needs more room to center in: at the section fraction this sat in the upper half with the bottom third blank, which reads as a screen that failed to load. Found by looking at it, twice.
- **No section drawing**, because projects are not one of the twelve. The ground alone is what 5.17 prescribes for a place outside the sections.

**Seen on the phone at both themes, at font scale 2.0, and in Arabic**, from a genuinely empty notebook reached through onboarding rather than by clearing a table: `projects-empty-light`, `projects-empty-dark`, `projects-empty-2x-dark`, `projects-empty-rtl-dark`. **362 instrumented tests pass**, up from 358, and both new guards were proved by breaking them.

### 2.4 The projects list carries the mini road

**`ProjectsScreen` is converted to the grid's screen 2**, #275. Each card is its status, its name, its mini road, and one line answering where it stands and the next date. Screenshot: `docs/screenshots/projects-list-dark.png`.

- **The mini road draws no labels.** Three mono words under a card in a list is noise, and the road's shape already says where the thing is. The reader gets the whole sentence, the same one the project's own screen gives.
- **`Repository.projectCards` is three queries for the whole list**, not three per project. Fifteen projects would otherwise cost forty-five round trips to draw the screen the tab opens on.
- **The next step line only prints when the card would otherwise say nothing.** It was right while a project was a checklist; under the grid a card answers two things, and printing a third under them is three lines competing where the grid draws one.
- **`projects.subtitle` was corrected in place**, in all four catalogs. It described a project as "a list of steps and a note of who you are waiting on", which is the checklist the grid supersedes.

## 2.5 Today has a lead slot and a card field, and it is on the phone

**`TodayFieldScreen`, with `TodayCard` and `CardSize`.** The lead spans the width at the top and the field is a two-column grid under it, with small taking one column and wide and tall taking two. Every card wears its section's hue from the tab pack and carries a corner chevron, and **each one opens the section its answer lives in**, because a door that does nothing on press reads as broken.

**Seen at both themes, at font scale 2.0, and in Arabic.** `docs/screenshots/today-field-dark.png`, `today-field-light.png`, `today-field-max-font.png`, `today-field-arabic.png`. The grid mirrors: the small cards swap sides, the chevrons flip, the capture button moves to the start edge, and the English catalog stays isolated inside the Arabic layout.

**The lead is singular by construction, not by convention.** It comes from `Repository.TodayLayout`, which has nowhere to put zero or two.

**`Repository.todayAnswers` answers every card in one pass**, and each answer is computed under its own guard. That is not tidiness: two of these queries named columns that do not exist, `question.resolved_at` and `milestone.title`, and under the shell's single catch **every card on the surface said "Nothing waiting" at once**, which is the app asserting something false about somebody's record.

**A card with no answer is absent from the map rather than holding an empty one**, and the screen says so differently. Empty means the record has nothing to say; absent means the question was never asked. Filling the gap with an empty answer is what made the digest in the lead slot claim nothing was waiting on a notebook holding 182 entries.

**The digest uses the summary the app already had**, computed once in the shell, rather than a second digest written for this surface.

**All fourteen situations ship a starting hand, #305, and a brand new notebook lands on the new Today.** Verified by clearing the app and walking onboarding: choosing Nursing home produced its eight cards, every one saying "Nothing waiting" without a single zero or scold. `docs/screenshots/today-fresh-dark.png`.

- **A card that names a source is never in a hand**, D115. The grid's home care hand lists a measure; at onboarding there is no measure to point at, so it would render with nothing to answer on the first screen a person ever sees. `check_templates.py` refuses one.
- **Skipping the picker gets `Repository.defaultStartingHand`**, because skipping is a real answer and not a request for an empty screen.
- **A hand is applied only when there is no layout**, so a person whose care setting changes later keeps the desk they arranged.
- **A zero is not rendered.** A large 0 above "Nothing waiting" says the same thing twice, and at that weight it reads as a score on somebody who has just started.

**Edit mode is built and works on the phone**, #271, apart from adding a card. `docs/screenshots/today-editing-light.png`.

- **Entered by a visible Edit button**, per 21.6 screen 5. Touch and hold is not the only path, because it is not a path at all yet.
- **Staged.** Every change is held in the screen and written once, from Done, so a person can move three cards and change their mind about all of them. Cancel discards.
- **Move up, Move down, Remove, and three size chips per card.** Proved on the device: moving a card and tapping Done survives a full app restart.
- **The lead cannot be removed and cannot move up**, so there is never zero.

**Three defects came out of building it, and two of them are accessibility defects a screenshot cannot show.**

1. **The edit controls were unreachable by a screen reader.** `TodayCard` cleared all descendant semantics so it would speak as one node, which is right for a card that is only a door and wrong the moment it holds controls: Move up and Move down are the accessible reorder path 21.6 asks for, and they did not exist for the people who need them most. The card stops speaking as one node while editing. **Found by trying to drive the controls from a semantics dump and finding nothing there.**
2. **"Move Medications down" wrapped to one letter per line** on a half-width card and stretched it to four times its height. The visible word is Up, Down, Remove; the reader still hears which card it moves.
3. **The isolate marks nested again**, in code written four commits after the same defect was fixed on the project screen. `Bidi.join` isolates every part it is given.

**The source-closed rung is built and seen.** A card pointing at a finished or removed project says "Closed. Still here until you remove it." and keeps working as a door. `docs/screenshots/today-source-closed-light.png`. **Closed means finished or removed**, both of which are states the person put the project in, and neither is a reason for the app to take a card off their screen. A project that is gone entirely, which an import can produce, keeps its card too.

**Adding a card is built and seen**, #272. `docs/screenshots/today-gallery-light.png`. Reached from Edit, and each entry says what the card would answer with the thing it points at underneath: **"Where it stands / Appeal the level of care assessment"**, not the other way round. The first version had it inverted and put three rows reading "Appeal the level of care assessment / Project" next to each other, which are three different cards and looked like one listed three times.

- **A card already on Today is not offered again**, except the ones that point at something: a second measure is a different card from the first.
- **Adding writes immediately** rather than being staged like a move, because a person who taps Add expects the card to be there.
- Proved end to end: added the weight card and found it at the end of Today.

**Promoting a card to the lead is built and seen.** It is its own action, per 21.1, because reaching the top by tapping Move up eleven times is not the same offer, and **promoting demotes the card that was there back into the field**. Proved on the phone: promoted the medications card, saved, force stopped the app, and it came back leading with the digest directly under it. `docs/screenshots/today-promoted-light.png`.

**What is left on Today:** Reordering to the top is the same thing today, and `promoteTodayCardToLead` exists and is tested.

**The fallback to the previous Today is still in the shell** and now only fires for a notebook made before this landed. It comes out with #271, and `ProjectDetailScreen`'s ledger row is the model for what goes in `docs/REMOVAL-LEDGER.md` at that moment.

**The latest word can be read but the app cannot write it, and that is #303.** `Repository.latestWordFor` reads it through the `link` table, which is what 8.1's generic connection table is for. **Nothing outside the fixture writes that link**, so on a real notebook the third answer is permanently absent. The fixture writes it so the card can be built and seen, which is deliberately not a fix.

**A reference number has nowhere to live, and that is the owner's call.** The grid draws one on the latest word and calls it first-class; `call_detail` has no column for it. **Nothing was changed**: rule 3, and the WHAT BECOMES DATA amendment does not mention reference numbers. `LatestWordCard` takes it as an optional parameter and draws nothing without it. #303 states the decision.

**One thing found while building them.** The grid draws several small labels in the faintest tone it has, and `ink3` is 2.37:1, which D92 makes non-text. **Those are `ink2` in the app.** A stage the project has not reached still has to be legible: it is where the thing is going.

**353 instrumented tests pass**, up from 349.

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

**A probe that edits a real file has to be restored by copy, never by git.** Proving a checker catches what it claims means breaking something on purpose and putting it back. On 2026-08-05 that was put back with `git checkout -- templates/data/projects.json`, which is a destructive command rule 6 bans by name, and it discarded an hour of uncommitted work on the same file rather than the probe. **Copy the file into the scratchpad first and copy it back**, or commit before probing. Nothing was lost permanently because the change was scripted and was regenerated, which was luck rather than a safeguard. **This is what B5 exists to prevent and it is the first time the missing guard has cost anything.**

**`installDebug` clears this app's data on this phone.** Twice in a row an install was followed by the app opening at "Before you start" with an empty notebook. **Every device check is install, then `tools/seed.sh`, then navigate**, and a screenshot taken straight after an install is a screenshot of onboarding.

**Clear the app locale before running the instrumented suite.** `AppLanguageTest` failed twice tonight because a right to left check left the per app locale set to Arabic by hand: `connectedDebugAndroidTest` only clears it when it actually reinstalls, and on an up to date install nothing wipes it. `adb shell cmd locale set-app-locales com.kamsiob.healthtrail --user 0 --locales ""` first. **#306 is reopened**, with three attempted fixes that did not work written into it.

**Copy the suite's report before rerunning anything.** A single class rerun overwrites `androidTest-results/connected/debug/TEST-*.xml`, and both flakes found this week, #302 and #308, lost their assertion and stack that way. Copy it into the scratchpad the moment the suite goes red.

**`connectedDebugAndroidTest` uninstalls the app when it finishes.** `walk.sh` then dumps whatever is on the phone, which is the owner's home screen with his real calendar and contacts on it. **Reinstall and check the app is focused before walking**, and never screenshot without it: `screenshot.sh` refuses, but `walk.sh see` does not.

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
