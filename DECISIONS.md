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

**Decision.** Configure `gpg.format=ssh` with the existing key at `~/.ssh/kamai_signing.pub`, `commit.gpgsign=true`, and `tag.gpgsign=true`, scoped to this repository rather than globally.

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

Two notes on what happened. The `gh project create` command produced project number 2, and an early command in this run listed the fields of project number 1 by mistake, which is the owner's existing Kam AI board. Nothing on project 1 was modified: the mistake surfaced as a KeyError before any mutation ran, and project 1's fields were checked afterward and are unchanged. Recorded because a later session reading the history should not have to wonder.

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

---

## BLOCKED

Anything only the owner can resolve. Each entry states exactly what he needs to do, in terms he can act on without reading any code.

### B1. Commits will show as unverified on GitHub until the signing key is added

**What is happening.** Every commit is being signed with the SSH key already on this machine. GitHub does not yet know that key belongs to the account, so it labels the commits unverified rather than verified.

**Why it is blocked here.** Adding a signing key needs a permission the logged-in GitHub CLI does not currently hold. Granting it opens a browser sign-in, which an unattended run cannot complete.

**What the owner needs to do,** either one of these, whichever is easier:

1. In a terminal, run `gh auth refresh -h github.com -s admin:ssh_signing_key`, complete the browser sign-in, then run:
   `gh ssh-key add ~/.ssh/kamai_signing.pub --type signing --title "kamsiob commit signing"`

2. Or in a browser, go to https://github.com/settings/ssh/new, set the key type to **Signing Key**, give it any title, and paste in this exact line:
   `ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPJDjMJuCwQhz7/FCxEPPdCYepd5hH6Bv01uitNbrdv2 kamsiob commit signing`

**What happens after.** Every commit already made will show as verified, not only new ones, because GitHub checks signatures against currently registered keys when it displays them. Nothing needs redoing.

**Impact while blocked.** Cosmetic only. The commits are genuinely signed and the history is sound. Nothing about the build depends on this.

### B2. Two board automations need one visit to the project settings

**What is happening.** The project board at https://github.com/users/Kamsiob/projects/2 is fully configured and populated, but its two built-in automations are off. That means a new issue does not appear on the board by itself, and closing an issue does not move its card to Done. Both are being done by hand in the meantime, which works but will go stale during a long run, which is exactly what the automation exists to prevent.

**Why it is blocked here.** GitHub has no API and no command line support for the built-in project workflows. They can only be switched on in the web interface.

**What the owner needs to do,** once, about a minute:

1. Open https://github.com/users/Kamsiob/projects/2
2. Click the three dots at the top right, then **Workflows**
3. Click **Item added to project**, set the status to **Todo**, and turn it on
4. Click **Item closed**, set the status to **Done**, and turn it on
5. Click **Auto-add to project**, set the filter to `repo:Kamsiob/health-trail is:issue`, and turn it on

**Impact while blocked.** The board stays correct because it is being maintained by hand, but by hand is the failure mode A4b names: status maintained by hand during a long unattended run goes stale, status derived from issue state cannot.

### B3. A hosted privacy policy URL is needed before release, not before now

**What is happening.** `PRIVACY.md` exists in the repository and is accurate. Template A6 requires the app's About screen to link a single canonical hosted version, so that no second copy can drift out of sync with it.

**What the owner needs to do,** at release rather than now: publish the contents of `PRIVACY.md` at a stable URL under kamsiob.com, and say what that URL is. The About screen will point at it and `PRIVACY.md` will be kept identical, with the same effective date.

**Impact while blocked.** None yet. There is no About screen and no release. Recorded now so it is not discovered during the release itself.
