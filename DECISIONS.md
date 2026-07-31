# DECISIONS.md, Health Trail by Kamsiob

Every judgment call made without asking, recorded at the moment it was made rather than reconstructed afterward. Each entry gives the date, the decision, the alternatives considered, the reasoning, and what would have to change for it to be worth revisiting.

The purpose of this file is to stop the same question being reopened and relitigated by a session that has forgotten the answer. Rejected approaches are recorded with the reason they were rejected, because that is the part a future session cannot reconstruct.

The BLOCKED section at the end lists anything only the owner can resolve, each with exactly what he needs to do.

---

## 2026-07-31

### D1. The first commit is a faithful snapshot of the handover folder

**Decision.** Initialize git in place and commit every file already in the folder unchanged, including the two concept PDFs, the two concept HTML files, the reference folder, the template catalog, and every specification document. Reorganize afterward in separate commits with their own messages.

**Alternatives considered.** Reorganizing into the monorepo layout first and committing the result as one initial commit. Discarding the concept PDFs and the standalone HTML files as scaffolding.

**Reasoning.** The kickoff instruction is explicit that the specifications are part of the public record of this project rather than scaffolding to be discarded. Committing the handover state first means every later move is visible as a diff, so a session reading the history can see what was given versus what was built. Squashing that into a reorganized initial commit would erase the distinction permanently.

**Revisit if.** Never. This is a historical fact once committed.

### D2. SSH commit signing configured locally, repository scoped

**Decision.** Configure `gpg.format=ssh` with the existing SSH key already on the machine, whose path this repository records in its own git config under `user.signingkey`, `commit.gpgsign=true`, and `tag.gpgsign=true`, scoped to this repository rather than globally.

**Alternatives considered.** Committing unsigned per the RUN-SAFETY.md section 5 fallback. Generating a new signing key for this project.

**Reasoning.** The key exists, has no passphrase, and is already listed in `~/.ssh/allowed_signers`, so signing costs nothing per commit and cannot stall the run on a passphrase prompt. The key is not yet registered with GitHub as a signing key, so commits will show as unverified there until it is, but GitHub verifies signatures at display time against currently registered keys, which means adding the key later makes the whole existing history show as verified. Signing now is therefore strictly better than signing later, and the fallback of committing unsigned would have thrown away that property. Repository scope rather than global because this run should not change the owner's global git configuration.

Generating a second key was rejected because the owner would then have two keys to manage for no benefit.

**Revisit if.** The key ever acquires a passphrase, which would make signing prompt and stall an unattended run. In that case switch to unsigned and record it.

**Owner action required.** See BLOCKED B1.

### D3. The safety guards are written in Python 3 rather than shell with jq

**Decision.** Guard 1, the destructive command hook, and guard 3, the retry cap, are Python 3 scripts. Guard 2, the pre-compaction state save, is bash because it is almost entirely git commands.

**Alternatives considered.** The shell plus `jq` pattern shown in the Claude Code hooks documentation.

**Reasoning.** Python 3.14.6 is confirmed present in this environment. `jq` is not confirmed present, and a guard that fails to run because a parser is missing is worse than no guard, because it fails open and silently. The destructive command guard in particular needs real regular expressions to distinguish `git restore .` from `git restore --staged`, and `git branch -D` from `git branch -d`, which is awkward in shell and clear in Python.

**Revisit if.** Never worth revisiting. The guards work and are tested.

### D4. Subagent definition format verified against current documentation before writing

**Decision.** Subagent definitions go in `.claude/agents/*.md` as Markdown with YAML frontmatter. The fields used are `name`, `description`, `tools`, `model`, and `maxTurns`. Verified against https://code.claude.com/docs/en/sub-agents on 2026-07-31.

**What the documentation confirmed.** Only `name` and `description` are required. The `tools` field, when omitted, inherits every tool available to subagents rather than granting none, which is exactly the trap `AGENTS.md` section 4 names as the single most important mechanical fact in that document. Every definition in this project therefore names its tools explicitly. `maxTurns` is a real supported field and is what `AGENTS.md` section 4 means by giving each agent a turn limit. `model` accepts `sonnet`, `opus`, `haiku`, `fable`, a full model id, or `inherit`, defaulting to `inherit`.

**Reasoning.** `AGENTS.md` section 4 instructs verifying the field names against the documentation rather than trusting its own table, and recording what was used here.

**Revisit if.** The documented field set changes.

### D5. The template's section C4 desktop APK export does not apply

**Decision.** Built artifacts stay in the Gradle output path inside the project and are installed to the device directly over ADB. No APK or AAB is ever copied to the desktop, a downloads folder, or anywhere else.

**Alternatives considered.** Following template section C4, which says every session ends by deleting any previously exported APK from the desktop and placing exactly one fresh current APK there.

**Reasoning.** The kickoff instruction for this project states the opposite of C4 explicitly and gives the reason: the only place a build needs to exist is on the phone. Direct instruction for this project outranks the universal template, which is the same relationship `PROJECT-DELTAS.md` establishes for section C5. The build output path is gitignored and nothing built is ever committed.

**Revisit if.** The owner asks for an APK somewhere he can reach it.

### D6. Android only. Part B of the template is out of scope, and the web scaffold stays

**Decision.** Part B of `kamsiob-project-template.md`, the Linux desktop build prompt, is out of scope for this project and nothing desktop-shaped is built. The `/web` scaffold required by `contract/DATA-CONTRACT.md` section 6 is still built.

**Alternatives considered.** Reading the owner's instruction as also canceling the `/web` scaffold.

**Reasoning.** The owner stated during the run that this is an Android build only unless he says otherwise, and that desktop and Linux specific sections should be ignored. Part B is exactly that and is now out of scope. The `/web` scaffold is a different thing: it is not a desktop application and it is not a second app being built. It is a Phase 0 acceptance criterion in the data contract, which sits above the template in precedence, and its stated job is to prove the schema contract is real by opening the same schema, which is what stops the two platforms drifting. It has no features and gains none. The data contract also mentions a possible future Linux desktop version, and that is not being designed for, anticipated, or scaffolded in any way.

**Revisit if.** The owner says the web scaffold is also out of scope, in which case the contract's section 10 criterion 8 needs an explicit owner decision to drop, since the data contract cannot be revised without one.

### D7. Type labels stand in for GitHub issue types

**Decision.** Carry the kind of work on a small set of `type:` labels rather than on GitHub's issue type field. One is applied to every issue. No second label holds the same meaning.

**Alternatives considered.** Using the issue type field as template A4b prescribes.

**Reasoning.** Issue types are an organization-level feature. This repository is under a personal account, confirmed by `gh api repos/Kamsiob/health-trail --jq .owner.type` returning `User`, and `gh api orgs/Kamsiob/issue-types` returning 404. The feature is genuinely unavailable rather than merely unconfigured. A4b says to adapt deliberately and record what was adapted and why, which is what this is. The discipline the field was there to enforce, meaning every issue carries exactly one kind and the kinds are consistent, is preserved by the labels.

**Revisit if.** The repository moves under an organization account, at which point the type field becomes available and the `type:` labels should be migrated to it and deleted, since holding the same meaning in two places guarantees they eventually disagree.

### D8. Board field set, and the built-in automation that could not be configured

**Decision.** The board carries Status, Platform, Area, Priority, Size, and Actual, all single select. Status is Todo, In progress, Blocked, Done. The board was configured with all of its fields and options before any item was added to it.

**Alternatives considered.** Accepting the default Status of Todo, In Progress, Done without a Blocked value.

**Reasoning.** `RUN-SAFETY.md` requires every blocked item to name what it is waiting on, and a board with no Blocked status cannot show that without abusing another field. Configuring fields before populating is A4b's explicit instruction and is also simply correct, because reshaping a single select's options after items reference them risks losing values.

One note on what happened. The `gh project create` command produced project number 2, and an early command in this run listed the fields of a different, unrelated project belonging to the owner by mistake. Nothing on that project was modified: the mistake surfaced as a KeyError before any mutation ran, and its fields were checked afterward and are unchanged. Recorded because a later session reading the history should not have to wonder.

Second, `Done` on this board means verified on a device or an emulator. Code being written is not grounds for moving anything there, and the board description says so.

**What could not be done.** GitHub's built-in project workflows, meaning auto-add new issues to the board and move an item to Done when its issue closes, have no public API and no `gh` command. They are configurable only through the web interface. See BLOCKED B2.

**Revisit if.** GitHub adds API support for the built-in workflows.

### D9. There is no canonical hosted privacy policy yet

**Decision.** `PRIVACY.md` in the repository is currently the only version of the policy, with an effective date of 31 July 2026. The About screen's privacy row will point at the hosted canonical version once it exists.

**Reasoning.** Template A6 requires the About screen to link the single canonical hosted policy so no second copy can drift, and requires `PRIVACY.md` to mirror it word for word with the same effective date. Nothing is hosted yet, and there is no About screen yet either, so nothing is currently inconsistent. This becomes a real requirement at release rather than now. Recorded so it is not discovered late. See BLOCKED B3.

**Revisit if.** Nothing. It gets done at release.

### D10. The repository README carries no continuous integration badge yet

**Decision.** Ship the README with license and status badges only. Add the continuous integration badge in the same commit that adds a workflow which genuinely passes.

**Reasoning.** A4b is explicit that badges must reflect real state, and that a badge showing a passing build while the build fails is worse than no badge. There is no Android project yet, so a workflow added now would be red for reasons that are not defects. The first workflow therefore covers only what exists and can genuinely pass, which is the content compliance checks, and it grows as the app does.

**Revisit if.** Nothing.

### D11. Tombstone retention window: 730 days

Answers open question 3 in `MASTER_SPEC.md` section 10. The data contract requires the number written into the schema comments now so a future implementation does not have to guess.

**Decision.** 730 days, two years. Written into the header comment of `contract/schema.sql`.

**Alternatives considered.** 90 days, which is the common default in systems that sync frequently. 365 days. No window at all, meaning tombstones are never purged.

**Reasoning.** The cost of the window being too long is a handful of bytes per deleted row. The cost of it being too short is that a peer which was dormant longer than the window resurrects records the person deleted, permanently and silently, which is the exact failure the tombstone mechanism exists to prevent.

Those costs are not remotely symmetrical, so the window should be generous. Two years is chosen against this app's actual usage rather than against a general default: lapse tolerance is a stated value, persona P6 is someone returning after a four month gap, persona P7 is a five year notebook, and a second device could easily sit in a drawer for a year. A 90 day window would be actively dangerous here.

Note that until direct sync exists there are no peers to acknowledge anything, so nothing is ever purged and the purge path is dead code. The number matters for when that changes.

**Revisit if.** Real devices are seen carrying enough tombstones to matter, which would require a notebook with an extraordinary amount of deletion. Even then, lengthening is safe and shortening is not.

### D12. The change log travels in the export, and the importer renumbers it

Answers open question 2 in `MASTER_SPEC.md` section 10.

**Decision.** `change_log` is included in `data.sqlite` inside the export container. On import, the rows are appended to the importing device's log with fresh local `seq` values, preserving `table_name`, `row_id`, `op`, `rev`, `changed_at`, and `device_id`.

**Alternatives considered.** Omitting the log and rebuilding it on import from the row timestamps.

**Reasoning.** Three things point the same way.

The export file is not only a backup. The data contract makes it the v1 implementation of the `SyncTransport` interface, which means it is the manual version of sync, and a peer needs the log. Rebuilding a log from row timestamps cannot distinguish an edit from a delete then recreate, which is precisely the distinction the log exists to carry.

The Today digest reads the change log to answer what changed since the person was last here. Restoring onto a fresh device without the log would produce either an empty first digest or a fabricated one, and fabricating is worse.

The cost is small. The log is narrow, and it compresses well inside a zip.

`seq` is renumbered rather than trusted because it is explicitly local only and meaningful solely on the device that wrote it. Two devices will both have a sequence 1 and they are not the same event.

**Revisit if.** A five year log turns out to dominate export size. Measure it against a year 5 fixture before changing anything.

### D13. Attachment limits: 25 MB each, a warning at 4 GB total, and no hard ceiling

Answers open question 1 in `MASTER_SPEC.md` section 10.

**Decision.** A single attachment may be up to 25 MB. Total attachment storage is not capped, but the app states the running total and mentions it plainly once the total passes 4 GB. Both numbers are stated before the person meets them rather than after.

**Alternatives considered.** No per-attachment limit. A hard total cap. A much smaller per-attachment limit with automatic downscaling of photographs.

**Reasoning.** 25 MB comfortably holds a photographed bill, a multi page scanned document, and a phone camera photograph at full resolution, while being small enough that a single attachment cannot make an export unusable on its own.

There is deliberately no hard total cap. It is their data, on their phone, and a record-keeping app that refuses to record something because of an arbitrary ceiling has failed. The interaction law that applies is "no invisible walls": state the limit before it is met and give a path forward at the moment it is met, rather than a dead end.

Automatic downscaling was rejected outright. A photograph of a bill is often evidence in a dispute, and silently reducing its resolution could destroy the readability of exactly the small print that mattered. If storage becomes a genuine problem the app shows what is using it and lets the person choose.

**Revisit if.** Real usage shows a common document type exceeding 25 MB, or the year 5 performance work finds attachment count rather than size to be the binding constraint.

### D14. The change log is enforced by database triggers rather than by the repository layer

**Decision.** Two triggers per user data table, written into `contract/schema.sql`, append the `change_log` row on insert and on update, deriving `delete` from `deleted_at` moving from null to set.

**Alternatives considered.** Appending the log row in the repository layer alongside each write, inside an explicit transaction.

**Reasoning.** The contract's requirement is that every write appends to the log *in the same transaction as the write*. A repository layer can satisfy that, right up until someone adds a write path that forgets, which happens once, silently, and produces a hole nothing can detect afterward. A trigger cannot be forgotten and it cannot run in a different transaction than the statement that fired it, because SQLite runs it inside that statement.

It also means the guarantee holds for the web platform without reimplementing it, which is the whole point of the schema being the contract.

The cost is that the log row is written even when the application would rather it were not, for example during an import. That is handled by the importer suppressing and then rewriting the log deliberately, which is explicit rather than accidental.

`device_id` is read from `app_meta` inside the trigger, falling back to `unknown-device` if it is missing, because losing the person's entry is worse than losing the provenance of it.

**Verified.** `tools/checks/check_schema.py` loads the schema into a real database and asserts the behavior: insert logs `insert`, update logs `update`, setting `deleted_at` logs `delete`, undeleting logs `update`, the live view hides a tombstone while the base table keeps it, and forcing the log write to fail rolls the data write back with it. It was then negative tested against six deliberately broken schemas and caught all six.

**Revisit if.** A trigger is measured to be a real cost in a bulk operation, in which case the fix is to batch inside one transaction, not to move the guarantee into application code.

### D15. SDK levels: compile against 37, target 36, minimum 26

**Decision.** `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`. Gradle 9.6.1, Android Gradle Plugin 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, JDK 21.

**How the numbers were arrived at,** rather than assumed:

**targetSdk 36.** Google Play requires new apps and updates to target Android 16, API 36, or higher from 31 August 2026, which is one month from now. Verified against the Play Console help and the Android developer documentation on 2026-07-31 rather than taken from any document in this folder.

Targeting 37 was considered and rejected for now. `targetSdk` opts the app into a platform version's runtime behavior changes, so raising it is a decision that should follow testing on a device, not one that follows an SDK release. It moves to 37 deliberately, after the behavior changes have been walked through. Android lint disagrees and raises `OldTargetApi`, which is disabled with that reasoning written at the point where it is disabled.

**compileSdk 37,** which is a separate concern: it decides which APIs the code may call. It is 37 because `androidx.core:core:1.19.0` and `androidx.lifecycle:lifecycle-runtime-compose:2.11.0` both refuse to be consumed by a project compiling against less. That was discovered by the build failing, not predicted.

**minSdk 26,** Android 8.0. Chosen for the audience rather than for convenience. These are frequently older, cheaper, hand-me-down phones. 26 also means `java.time` is available without desugaring, which removes a class of date handling bugs from an app whose entire subject is dates.

**Versions.** Every one was read from its actual Maven metadata on 2026-07-31. Gradle is 9.6.1, the current release. AGP 9.3.1 is the newest stable; 9.4.0-alpha07 exists and was not used.

**One thing that surprised the build.** AGP 9 carries its own Kotlin support and refuses the `org.jetbrains.kotlin.android` plugin outright, with an error saying so. The Kotlin version in the catalog is now only for the Compose compiler plugin, and the catalog says so where the version is declared.

**Revisit if.** Play raises the requirement again, which it does annually, or a dependency forces `compileSdk` higher.

### D16. No Room, and no ORM. Raw SQLite behind androidx.sqlite

**Decision.** The database layer is `androidx.sqlite` plus SQLCipher for the encrypted implementation, with hand written mappers. There is no Room, no SQLDelight, and no other library that generates or declares a schema.

**Alternatives considered.** Room, which is the default choice for an Android app this size and would save real work. SQLDelight, which generates Kotlin from `.sq` files.

**Reasoning.** Both of them require the schema to be declared a second time in the platform's own language: Room as annotated entity classes, SQLDelight as `.sq` files. `PROJECT-DELTAS.md` section 2 says plainly that the schema is not defined in Kotlin, and the data contract explains why: a schema that exists only as platform code makes the web version a reimplementation rather than a second reader, and the two drift apart within weeks.

Room's compile time verification would also fight the arrangement rather than help it. Room validates the database against its own generated schema and treats anything else as corruption, so a database created by executing `contract/schema.sql` would either be rejected or require the entity definitions to be kept manually identical to the SQL, which is precisely the second copy the contract forbids, with the added problem that it would look verified.

The cost is real and accepted: hand written mappers, no compile time query checking, and more test surface. That cost is paid once. Schema drift between two platforms is paid forever.

**Revisit if.** Never, without an owner decision, since it follows directly from the data contract.

### D17. Two lint checks disabled, each with its reason at the point of disabling

**Decision.** `warningsAsErrors = true`, `abortOnError = true`, no lint baseline, and exactly two checks disabled.

**Why no baseline.** A baseline file is how a project accumulates warnings nobody ever looks at again. Every lint finding is either fixed or disabled deliberately with a reason.

**`OldTargetApi`,** disabled for the reasoning in D15.

**`ObsoleteSdkInt`,** disabled because it is right in principle and wrong in practice here. It argues that the `v26` qualifier on `res/mipmap-anydpi-v26` is redundant now that `minSdk` is 26. Removing the qualifier was tried, and AAPT2 then skips the folder entirely: the launcher icon does not link and the build fails with `resource mipmap/ic_launcher not found`. Verified by doing it and reading the merged resource output, not by reasoning about it.

**The other three findings were fixed rather than disabled:** Robolectric moved to 4.16.1, and two genuinely unused resources were deleted rather than suppressed. They come back when something uses them.

**Revisit if.** A future AGP makes the `anydpi` folder work without the qualifier, in which case `ObsoleteSdkInt` should be re-enabled and the folder renamed.

### D18. Android 17 platforms carry minor API levels, and the package id is android-37.0

**What was found.** `compileSdk = 37` resolves to the SDK package `platforms;android-37.0`, not `platforms;android-37`. From Android 16 onward the platforms carry minor API levels, so the public repository publishes `android-36`, `android-36.1`, `android-37.0`, and `android-37.1`, with no plain `android-37` at all.

**Why it is worth recording.** Asking `sdkmanager` for `platforms;android-37` fails with `Failed to find package`, which reads exactly like the platform being unreleased or the tooling being out of date. Two continuous integration runs were spent on that reading: first updating the command line tools, then switching to `android-actions/setup-android` to update them properly. Neither was the problem. The package id was.

Confirmed by reading `repository2-3.xml` from the Google SDK repository directly, and by the local SDK, where `platforms/android-37.0/source.properties` reports `AndroidVersion.ApiLevel=37.0` and `Pkg.Desc=Android SDK Platform 17`.

**What this means going forward.** Any workflow, script, or document naming an SDK platform package for API 37 or later uses the minor form. The device the app is verified on runs Android 17.

**Revisit if.** Nothing. This is a fact about the platform, recorded so the next session does not spend the same two runs on it.

### D19. Contrast measured, five tokens corrected, and the capture button glyph is no longer white

`DESIGN.md` section 2.3 gave three text contrast corrections and then said something that turned out to matter: its numbers were calculated rather than measured, and the measurement is what counts. This is that measurement, and the document was right to hedge.

**Decision.** Measure every color pair the app actually renders, in both themes, as an automated check rather than a one time exercise. `tools/checks/check_contrast.py` reads the tokens out of the theme itself and runs on every push, because a ratio recorded in a document stops being true the first time somebody adjusts a token and nothing notices.

**What the measurement found.** 80 pairs measured. Five tokens needed correcting, including two of the corrections `DESIGN.md` had already proposed:

| Token | Was | Now | Why |
|---|---|---|---|
| `ink2` light | `#5C6F7E` | `#5A6D7C` | 4.38:1 on `sand` |
| `ink3Text` light | `#5E6E79` | `#5C6C77` | 4.43:1 on `sand`. This was already a correction and was still short |
| `blazeText` light | `#9A6E14` | `#8F6309` | 4.22:1 on paper, 3.87:1 in a gold tonal card, against the roughly 4.9:1 calculated |
| `alertText` light | `#B84A2E` | `#B34529` | 4.22:1 inside a red tonal pill |
| `ink3Text` dark | `#7F9099` | `#8798A1` | 4.10:1 on `sand`, the tightest pairing in the dark theme |

The pattern behind four of these: the original ratios were calculated against white, and this app has no white background. `paper` is `#FAF6EE` and `sand` is `#F1EBDC`, both warmer and darker than white, so every calculation against white was optimistic.

**The capture button glyph, which nothing had flagged.** `DESIGN.md` section 5.5 specified a white plus glyph on the gold circle. White on `blaze` measures 2.38:1 in light and 1.97:1 in dark, well under the 3:1 a control requires.

This is the most important control in the app. It is the single way data enters, it is on every screen, and the person using it is tired, often in bad light, and frequently older. So the fill stays gold in both themes, which is what carries the meaning and what section 2.4 protects, and the glyph darkens to `#22384A` in light, 5.08:1, and `#0B171E` in dark, 9.25:1. A new `onBlaze` token carries it. `DESIGN.md` sections 2.4 and 5.5 are corrected.

**Decorative is a category, not a loophole.** A hairline rule, the dashed trail line, a timeline node, and a care thread route are measured and reported but not held to 3:1. WCAG 1.4.11 covers interface components and graphical objects required to understand content, and none of these qualify: remove a hairline and nothing becomes unreadable, and a node's color is never the only carrier of its meaning because section 2.2 requires a word, a shape, or an icon alongside it.

The honest alternative was forcing the trail to stop being gold, and gold is the entire metaphor. So they are measured on every run, printed lowest first, and reviewed by eye on a device rather than quietly exempted. In light theme the trail line sits at 2.21:1 on paper and a hairline at 2.37:1.

**One bug in the check itself, worth recording** because it looked like a theme fault. The first version parsed `threadRoutes = listOf(...)` with a non-greedy match, which stops at the parenthesis closing `Color(...)`, so it found route zero and reported the other three as undefined. Fixed by scanning to the matching parenthesis. A check that reports a false failure trains whoever reads it to ignore it.

**Revisit if.** A token changes, which the check will catch, or the floors change.

### D20. The board is run for transparency, not as a simulated scrum team

**Decision.** Keep Status, Platform, Area, Priority, Size, and Actual. No sprint or iteration field, no story points, no velocity, no burndown. The board is public, and its README explains how to read it, what Done means, and how to check any claim on it by following the chain from issue to commit to pull request to a closing comment naming the device.

**How this was arrived at,** because the path matters more than the outcome here. The owner asked for the board to be managed like a professionally run Agile or Scrum project. `kamsiob-project-template.md` section A4b argues directly against that: no iteration or sprint fields, because time boxing has no meaning for one person working continuously and an iteration field that does not reflect a real cadence is the clearest possible example of process being performed rather than run. No story points. No delivery metrics designed to assess engineering organizations.

An owner instruction outranks the template, so a two week `Sprint` iteration field was created and configured. Before any item was assigned to it, the owner clarified: the goal is professionalism, visibility, and transparency, and he is not pretending there is an actual human team. The field was deleted, with nothing ever assigned to it.

**Reasoning, now that both statements are on the table.** They are not in conflict once the word professional is unpacked. What reads as professional on a public repository is that every claim can be checked: acceptance criteria ticked as each is verified rather than all at once, working notes written while the work happens rather than reconstructed, commits referencing issues, and Done meaning verified on hardware. What does not read as professional is a board carrying sprint numbers, velocity, and a burndown chart for a team of nobody. The second is decoration, and decoration on a tracker is worse than absence because it invites a reader to trust a signal that means nothing.

So the discipline is kept in full and the ceremony is dropped, and the board README says which is missing and why rather than leaving a visitor to wonder.

**What was added rather than removed.** The board README now carries the definition of done in short form, a section on how to check any claim by following the chain backward, and an explicit list of what is deliberately absent. The board was also made public, which it needed to be regardless: the README and the pinned roadmap both linked to it and those links reached a private page for everyone except the owner.

**Revisit if.** More than one person ever works on this. Then the cadence would be real and the fields would carry information.

### D21. The emulator did not come up, so DatabaseTest is written and unrun

**Situation.** `DatabaseTest` creates and writes a database, so it belongs on an emulator rather than the owner's phone. Two starts were attempted. The first named a system image directory rather than an AVD, and the emulator said so plainly. The second reused an AVD that already existed on the machine, which began a cold boot and had not attached to `adb` within several minutes. `/dev/kvm` is present and readable and the log shows no fatal error, so this looks like slowness rather than breakage.

**Decision.** Stop, record, and move on rather than keep retrying. The tests are committed, they compile, and both the commit message and `HANDOFF.md` say plainly that they have not run. Nothing claims otherwise.

**What was explicitly not done.** Running them against the connected Pixel. That would create a real database inside the owner's installation, and the rule that data-affecting tests stay on an emulator exists precisely so a convenient shortcut does not put test rows in a real notebook. An unrun test is a known gap. A test run in the wrong place is a quiet mess.

**How to finish it.** Superseded by D23: this project now has its own AVD, `health-trail-api36`, and reusing an existing one was itself the mistake.

**What is already proven without it.** The same schema, the same triggers, and the same tombstone behavior are asserted by `tools/checks/check_schema.py` against a real SQLite database on every push, including that a failing change log write rolls the data write back. What `DatabaseTest` adds is that this holds through SQLCipher and through the Kotlin path, which is a real gap and is why the issue stays open.

### D22. The protocol for screens that were never drawn, and the template library

**Context.** The owner established, mid-run, that `reference/screen-grid.html` does not cover everything the app needs, and gave the protocol for handling it plus a detailed specification for the template library. Recorded here because these are standing rules that will outlive the conversation they were given in.

**Where each part landed, deliberately spread rather than kept in one place:**

- **`CLAUDE.md` rules 11, 12, and 13.** That file is loaded every session and is the last thing to survive compaction, so the three rules that must never be lost live there in one line each: nothing unfinished reaches the person, undesigned screens are composed and logged, and partial is a finished state.
- **`DESIGN.md` section 10**, the full protocol. Compose rather than design, the eight states a screen ships with, the three places to log it, and discoverability as part of the screen rather than a consequence of layout.
- **`DESIGN.md` section 8** gains a running list of screens built without a mockup, so the document keeps describing the app as it is.
- **`MASTER_SPEC.md` section 4.10**, the template library requirements, and **4.10b**, the two rules that hold everywhere: nothing gets lost, and partial is a finished state.
- **`HANDOFF.md` section 9**, the running list in review order.
- A **`needs-design-review`** label on the tracker, in the blaze color, since it is the owner's review queue.

**The judgment worth preserving,** because it is the part most likely to be eroded by a later session in a hurry. The instruction is not "design the missing screens well." It is that an undesigned screen is **assembled** from a design language that is already finished, and that finding yourself designing means you have already gone wrong. A new component is a last resort, defined once with its states, used everywhere it applies, and a pattern appearing twice in two different forms is a defect to be fixed backward rather than left standing.

**And the reason the logging is not optional.** Building is allowed to proceed without asking, which means the review happens after the fact. That only works if the record is written at the moment of the decision. A screen built on Tuesday and logged on Friday is three days of work stacked on an unreviewed choice, and the owner cannot review what he cannot find.

**Revisit if.** Never, without the owner. These are his design authority, delegated with conditions attached.

### D23. A dedicated emulator, and nothing from any other project

**Decision.** This project has its own AVD, `health-trail-api36`, created with `avdmanager` against `system-images;android-36;google_apis;x86_64`. Nothing belonging to any other work is reused: no emulator, no device profile, no keystore, no build artifact.

**What went wrong before this.** The first emulator attempt named a system image directory rather than an AVD. The second reused an AVD that already existed on the machine and belonged to something else, which then failed to boot and cost two rounds of diagnosis. Reusing it was the actual mistake; the boot failure was only how it surfaced. A dedicated AVD is also reproducible, which a borrowed one never is.

A shell left waiting on that emulator was still running eighteen minutes later and was ended.

**The repository is self contained, and that is now enforced.** Every reference to other work was removed from `DECISIONS.md`, `HANDOFF.md`, and the working tree, and `tools/checks/check_self_contained.py` fails the build if one returns.

That check stores its watched words as SHA-256 hashes rather than as a list, because a check holding the list in plain text would itself put those names into the repository, which is the thing it exists to prevent. It scans the files git tracks rather than the working tree, since gitignored session state and build output never reach anyone. Negative tested: a watched word added to a tracked file makes it exit 1 and name the file, the line, and the word.

**One file was removed rather than edited.** A copy of the public website page sat at the repository root. It carried site navigation naming other products, and a stale copy of a live page drifts from it besides, which is the same argument already applied to the privacy policy. It is gitignored now. Nothing about the build depended on it, and the design reference is `reference/screen-grid.html`.

**One reference stays where it is, and the claim I first made about it was wrong.** A commit message on `feat/14-encrypted-database` names the borrowed AVD. History is not rewritten here, so it stays on that branch, and this entry is the correction.

I originally recorded that it would not reach the default branch because a squash merge writes a fresh message. **That was false, and checking rather than asserting is what caught it.** The repository was configured with `squash_merge_commit_message = COMMIT_MESSAGES`, which concatenates every branch commit message into the squash commit body. The reference would have landed on `main` verbatim.

Corrected by configuring the repository so that the only available merge method is squash, and so that the squash commit takes its title and body from the pull request rather than from the branch commits:

- `allow_squash_merge: true`, `allow_merge_commit: false`, `allow_rebase_merge: false`
- `squash_merge_commit_title: PR_TITLE`, `squash_merge_commit_message: PR_BODY`
- `delete_branch_on_merge: true`

The pull request body is written here and is checked, so what reaches `main` is controlled rather than inherited. Disabling merge commits and rebase merges matters for the same reason: either would have carried the individual commit messages through.

**The general lesson, worth more than the specific fix.** I asserted a property of the tooling instead of reading it. The assertion was plausible, wrong, and would have quietly defeated the thing it was cited to guarantee.

**Revisit if.** Nothing. From here, no other project is named anywhere in this repository, in any file, commit message, issue, or comment.

### D24. Where the key write happens, and what happens when the key is gone

Two follow-ups on the key storage in D14 and the lint exchange that preceded it.

#### The synchronous write stays, and it runs off the main thread

Lint raised `ApplySharedPref`, asking for `apply()` instead of `commit()`. Moving to `prefs.edit(commit = true)` satisfied the rule while keeping the blocking write. That silenced the question without answering it, and the question was real: **lint was asking about blocking work on the main thread.**

**The answer, decided rather than assumed.** `HealthTrailDatabase.open()` is a **suspend function whose body runs on `Dispatchers.IO`**. It does Keystore operations that touch secure hardware, a synchronous preference write, and on first run executes the entire schema. All of it belongs off the main thread.

**A runtime check was the first answer and it was the wrong instrument.** The original version enforced the thread with a `check` that threw. That is correct in debug and dangerous in release, because the failure it produces is a crash, and the path it fires on is by definition one the tests did not reach. The person it would crash is a caregiver in a hallway.

Making the function suspend and switch dispatcher itself removes the question instead of answering it. There is no longer a way to call it and end up doing blocking work on the main thread, whatever the caller does, so no check is needed and none is present. Calling it from the wrong place is now a compile error rather than a runtime one, which is where this class of mistake belongs.

The general form of the lesson: when a constraint can be made structural, a runtime assertion is not a cheaper version of it, it is a worse one that fails in front of the user.

The write itself stays synchronous. `apply()` writes in the background, so a process death in the following milliseconds would lose the wrapped passphrase while the database file it unlocks already exists, which is unrecoverable in the worst way: the notebook still on disk and nothing able to open it. It costs microseconds and happens once per install.

#### Backup exclusion, confirmed rather than assumed

Read out of the merged manifest and the rules file, not from memory:

- `android:allowBackup="false"`
- `android:fullBackupContent="false"`
- `data_extraction_rules.xml` excludes `root`, `database`, `sharedpref`, `external`, and `file` from **both** `cloud-backup` and `device-transfer`.

So the database file and the preferences holding the wrapped passphrase are both excluded from Auto Backup and from device to device transfer. They never travel together, and neither travels at all.

#### What the app does when the wrapping key is gone

**It cannot be recovered in place, and the app says so.**

The Keystore key can disappear: a factory reset, certain device migrations, and some Keystore corruption cases all take it. The database file may still be sitting on disk, and without that key it is bytes.

`DatabaseKey` now throws a typed `DatabaseKeyLost` rather than a generic failure. It deliberately does **not** catch the error and generate a fresh passphrase, which would leave the notebook on disk, undecryptable, while the app behaved as though the person had never written anything. That is the worst available outcome and it looks like the best one.

The honest answer is **restore from your export**, and stating it has a consequence worth naming plainly: **backup and restore are load bearing, not optional.** They are the only recovery path this app has for its own encryption, so they are built accordingly. Concretely:

- The backup offer is made once, at the moment the person has something worth losing, and a decline is honored permanently. Its copy says what it is protecting against.
- The quiet permanent indicator of the last successful backup is not decoration. It is the person's only view of whether recovery is possible.
- Restore is tested onto a fresh install as its primary case rather than an afterthought.
- The screen shown when the key is gone explains what happened in plain words, does not blame the person, and offers import as the way forward. It never suggests the data is retrievable when it is not.

That screen has no mockup, so it is built under the `DESIGN.md` section 10 protocol and logged like any other undesigned screen. Tracked on issue #14.

**Revisit if.** Android ever offers a durable way to recover a Keystore key across a reset, which would change the recovery story rather than this reasoning.

### D27. User facing text is loaded from the contract at runtime, not compiled into resources

**Decision.** The app reads `contract/i18n/*.json` from assets at runtime and formats with `android.icu.text.MessageFormat`. `res/values/strings.xml` is reduced to `app_name` and the three translated resource files are deleted.

**Alternatives considered.** Generating Android string resources from the catalogs at build time, which would have used the platform's own locale resolution and `<plurals>`.

**Reasoning.** Generating resources means a second representation of every string, and the generated copy is the one that goes stale when someone edits it in the wrong place. The same argument that keeps the schema out of Kotlin applies to the copy.

It also matters for correctness rather than only tidiness. The catalogs use full ICU `select` and `plural` syntax, Arabic carries six plural forms, and the deterministic engine has to produce byte identical output in Kotlin and in TypeScript against shared golden vectors. `android.icu` is the same ICU implementation the TypeScript side will use, so identical output is achievable rather than aspirational. A conversion step between the catalog and the platform format would be one more place for the two to diverge.

**On falling back to English.** `MASTER_SPEC.md` section 7 forbids silently falling back rather than failing the build. The runtime does fall back, and that does not contradict it: `check_i18n.py` fails the build when the catalogs disagree on even one key, so a fallback can never mask a missing translation. It can only mask code asking for a key no catalog defines, which is a code bug. That throws in debug and falls back quietly in release, because crashing a caregiver over a missing label is worse than a visible key.

**Revisit if.** Startup cost ever shows up in the cold launch budget, which is under 1.5 seconds at five year scale. Two small JSON parses is not currently near it.

### D28. Two bugs found by building the screen and looking at it

Recorded because both were invisible in the code and obvious on the device, which is the argument for the rule that every screen is installed and looked at before its issue closes.

**The accept button sat in the upper third.** The whole disclaimer column scrolled, so with short content everything bunched at the top and the one action on the screen ended up out of easy reach. `DESIGN.md` section 9 requires primary actions in the lower half so the screen works one handed on a large phone.

Fixed by scrolling the text and not the action: the text column takes the available height and the button sits below it. That also handles the case the original structure was trying to serve, since the wording can now grow to any font size or translation length without pushing the action off the bottom. Logged as item 8 in the screens list.

**The repository held a closed database.** `Repository` cached the `HealthTrailDatabase` in its constructor while `HealthTrailDatabase` kept its own singleton. Closing the database, which the full data wipe does and which tests do between cases, left the repository holding a closed handle, and the next call failed with "attempt to re-open an already-closed object".

It surfaced as two failures in a test class that had not touched the database, several tests after the one that closed it, which is the shape of bug that is expensive to find later. Fixed structurally rather than by patching: the repository holds no handle and resolves the database per call. Two singletons with independent lifecycles will always drift, so the fix is to have one lifecycle.

Worth noting this would have reached a person. The full data wipe closes the database, and the screen shown afterward would have failed on its first read.

### D29. The destructive command hook has not been active this whole run

**Found on 2026-07-31 by testing the hook rather than the script.** `git rebase --help`, which is on the blocklist, ran without being refused. So did `adb shell pm clear`, which is how it surfaced: I cleared app data on the owner's phone to get back to a fresh install, and nothing stopped me.

**The script is fine. The wiring was not.** `.claude/hooks/block-destructive.py` returns exit code 2 for every blocked pattern, verified again just now including the `$ADB` variable form. The hook simply never ran.

**The cause is the timing rule this project already knew about and I applied to the wrong thing.** `RUN-SAFETY.md` section 6 and `AGENTS.md` section 7 both say agent definitions load at session start and are not usable until the next session. Hooks in `.claude/settings.json` load the same way. That file was created during this session, in Phase 0, so it takes effect from the next session onward and has protected nothing so far.

**What my verification actually proved, and what it did not.** I fed 25 payloads to the script directly and confirmed 13 refusals and 12 passes. That tested the script. It did not test that the session would call it, and I recorded it as though the guard were live. `RUN-SAFETY.md` section 3 warns about exactly this shape of error: reporting a protection as in place when the thing on disk is real but is not doing anything.

**What it cost.** Nothing irreversible. The phone held only rows I had typed into it minutes earlier while testing, no real notebook existed, and no destructive git command was attempted during the run. The guard being inert was luck rather than design, which is the point.

**What changes.**

- The guard is live from the next session, with no action needed.
- `HANDOFF.md` states plainly that guard 1 was inert for this run, so nobody reads the Phase 0 entry as meaning it was protecting the work.
- Every future claim that a guard is in place has to be verified through the mechanism rather than against the artifact. For a hook, that means running a blocked command and being refused.
- The same question applies to guard 2, the pre compaction state save, which has also never fired. It is written and committed and unproven in practice.

**Revisit if.** Nothing. This is a fact about the run, recorded so the next session does not inherit a false belief about when protection started.

---

## BLOCKED

Anything only the owner can resolve. Each entry states exactly what he needs to do, in terms he can act on without reading any code.

**Two of the three original entries are resolved.** Kept below with their outcomes rather than deleted, because a BLOCKED section that only ever grows teaches a reader that nothing here gets fixed.

### D25. Instrumented tests run on the phone. Corrected, the permission does not expire

**Superseded on the same day it was written, and the correction is the interesting part.**

**What this entry originally said.** That running `DatabaseTest` on the owner's phone was a one time exception, permitted only while the phone held no notebook worth preserving, expiring the moment real data existed, after which an emulator became a prerequisite.

**Why that was wrong.** It rested on treating a long lived phone installation as the evidence that data survives updates. It is not that evidence. **The export and import round trip against the golden vectors in continuous integration is**, and it is repeatable, runs on every push, and depends on no device's history. An installation is a sample of one that nobody else can reproduce. Building a rule around preserving it manufactured a dependency on an emulator that this project does not need.

**What holds now.** Instrumented tests run on the phone. There is no expiry, because there is nothing on the phone that needs preserving as proof of anything.

**What was right, and remains the single operational rule.** `connectedAndroidTest` uninstalls the application itself, not only the instrumentation package. That was found by running it: the phone was left with no Health Trail at all and had to be reinstalled. So before running it, if the phone holds data worth keeping, export through the app's own export feature and reimport afterward. A checklist step, not a reason to avoid running tests.

**What the run proved,** which stands unchanged: 13 instrumented tests, 0 failures, 0 errors, on Android 17. The schema loads through SQLCipher with 34 live views and 68 triggers. The file on disk does not begin with the plaintext SQLite header, which is the only honest test that encryption is on. The wrong passphrase cannot open it. Insert, update, and tombstone log correctly through the Kotlin path carrying this device's id. A tombstoned row leaves the live view while staying in the base table. Reopening preserves both the device id and the rows.

**Also unchanged.** The destructive command guard permits uninstalling a package id ending in `.test`, the instrumentation APK, while still refusing to uninstall the app. Verified in both directions.

### B4. The emulator. Resolved by dropping it, 2026-07-31

**Outcome.** There is no emulator in this project and its absence is not a blocker. The connected phone is the only test device. Unit tests need no device, instrumented tests run on the phone over ADB, development builds install to the phone, and manual verification happens there.

This was an owner decision, and it dissolves the problem rather than solving it. Five attempts to start an emulator in this environment all failed the same way, and the session cannot grant itself the device access QEMU needs, so it was never solvable from here.

**The reasoning that made the emulator look necessary was itself wrong.** It rested on preserving a long lived phone installation as evidence that data survives updates. That is not what proves it. **Data survival is proven by the export and import round trip against the golden vectors in continuous integration**, which is repeatable, runs on every push, and does not depend on any one device's history. A phone installation is a sample of one that nobody can reproduce.

**The one operational rule that remains,** and it is a checklist step rather than a reason to avoid anything:

> `connectedAndroidTest` uninstalls the application and takes its data with it. Before running it, if the phone holds anything worth keeping, export through the app's own export feature first and reimport afterward.

**What this changed in the repository:** `tools/verify.sh` no longer refuses to run the instrumented suite on a physical device, `CONTRIBUTING.md` and the `test-runner` agent definition carry the export-first step instead of an emulator requirement, and the test classes say where they run and why.

### B1. Commit signing. Resolved 2026-07-31

**Outcome.** The owner registered the SSH signing key. Verified rather than assumed: the account now lists one signing key titled "kamsiob commit signing", and `repos/Kamsiob/health-trail/commits/main` reports `verified=true, reason=valid`.

As expected, this applied to the whole existing history at once rather than only to new commits, because GitHub checks signatures against currently registered keys when it displays them.

**Worth keeping in mind.** This is the first Kamsiob repository with signed commits. The other four projects still show zero verified commits, since the key is registered now but their history was written unsigned. Nothing needs doing about that, and nothing should be: history is never rewritten here.

### B2. Board automations. Resolved by doing it a different way

**Original problem.** GitHub's built-in project workflows, auto-add and move to Done on close, have no API and no command line support, so an unattended run cannot switch them on.

**Outcome.** Not switched on, and no longer treated as a blocker. Verified empirically on 2026-07-31: issue #25 was created and did not appear on the board by itself.

The board is instead maintained by `tools/board.py`, which is committed, deterministic, and run at every increment. `sync` adds anything missing and moves anything whose issue is closed to Done. It deliberately never moves an open issue, because whether something is in progress is a judgment rather than something derivable from issue state.

The template's concern is that hand-maintained status goes stale during a long run. A script run every increment is not hand-maintained in the sense that warning means. The owner said to make the board whatever works so long as it is professional, and this works.

**If the automations are ever wanted anyway,** they are three switches at https://github.com/users/Kamsiob/projects/2 under the three dots, then Workflows: **Item added to project** set to Todo, **Item closed** set to Done, and **Auto-add to project** filtered to `repo:Kamsiob/health-trail is:issue`. Nothing depends on it.

**Also done:** the board is public, which it needed to be, since the README and the pinned roadmap both link to it and those links were reaching a private page for everyone except the owner.

### B3. Hosted privacy policy. Resolved, then corrected

**Outcome.** The canonical policy for this app is **https://kamsiob.com/health-trail.html#privacy**. That is what the About screen links, what the Play Console listing uses, and what governs.

**The correction, recorded because the mistake is an easy one to repeat.** The owner gave that URL. Following it, the page ends with a link reading "The full policy, same plain words" pointing at `privacy.html#health-trail`, a longer all-products policy. I inferred from that link that the longer page was canonical and wrote `PRIVACY.md` to mirror it. That was wrong, and the owner corrected it: the link between the two is not a signal that the longer page governs.

The lesson is narrow and worth stating plainly. An instruction naming a specific URL is not an invitation to go looking for a more authoritative one. `PRIVACY.md` now carries a warning at the top naming this trap, so the next reader does not helpfully switch it back.

`PRIVACY.md` mirrors the canonical wording. Issue #25 carries the remaining work.

**One thing that is not blocking.** The canonical page carries no effective date, while promising that any change is posted there with a new date. Template section A6 asks the repository copy to match including its date. There is no date to match, so `PRIVACY.md` carries none rather than inventing one that would guarantee the two disagree. If a date appears, copy it across.

### D30. Functionally correct is not done

**Date:** 2026-07-31. **Decided by:** the owner, and it applies from here on rather than to a cleanup pass.

Four screens were built, verified on the device, and shipped: the disclaimer gate, the setup screen, the notebook table of contents, and the capture form. Each one worked. Each one was visually thin. The owner named them as one symptom rather than four defects, which is the correct reading: they were built to be functionally correct and left there, and every screen after them would have inherited the same bar.

**The standard, which is now in `DESIGN.md` section 10.5 and checked by the list in 10.6.** A screen is not done when it works. It is done when it looks and reads like the rest of the app, has been looked at on the device, and nothing on it stands in for a design decision that was never made.

**What the failure actually was, stated plainly so it is not repeated.** Not a shortage of taste and not a missing mockup. Every piece needed already existed: cards, section headers, list rows with subtitle and chevron, eyebrow labels, pills, chips, empty states, the spacing scale, the type scale, the motion vocabulary. The capture screen in particular had a mockup, screen 26 of the reference file, showing rough date chips, thread chips, an open note area, and a save action that accepts whatever is there. What shipped was two single line text fields. The specification was not read closely enough before building, and the result passed its tests.

**The lesson is narrow and worth stating in its narrowest form.** Passing tests measures whether a screen does what it was built to do. It says nothing about whether it should have been built that way. A screen that works but looks unfinished is unfinished, and the tests will not tell you.

**One thing this does not license.** The fix for a thin screen is the components that already exist, not new ones. Composing badly and not composing look the same from a distance, and the temptation after a note like this one is to invent something to prove effort. Section 10.2 still holds.

### D31. The screenshot theme label is read from the device

**Date:** 2026-07-31.

`tools/screenshot.sh` took the theme as an optional argument defaulting to `light` and never asked the device. Two captures in one session were written as `-light` while the phone was in dark mode.

The theme is now read from `cmd uimode night` and an argument that disagrees with the device is refused rather than honored. A mislabeled screenshot in a public repository is the kind of error nobody catches, because the label is believed and the image is only glanced at, and these images are the evidence attached to design review issues.
