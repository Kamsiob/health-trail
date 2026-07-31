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

**Alternatives considered.** Reading the owner's instruction as also cancelling the `/web` scaffold.

**Reasoning.** The owner stated during the run that this is an Android build only unless he says otherwise, and that desktop and Linux specific sections should be ignored. Part B is exactly that and is now out of scope. The `/web` scaffold is a different thing: it is not a desktop application and it is not a second app being built. It is a Phase 0 acceptance criterion in the data contract, which sits above the template in precedence, and its stated job is to prove the schema contract is real by opening the same schema, which is what stops the two platforms drifting. It has no features and gains none. The data contract also mentions a possible future Linux desktop version, and that is not being designed for, anticipated, or scaffolded in any way.

**Revisit if.** The owner says the web scaffold is also out of scope, in which case the contract's section 10 criterion 8 needs an explicit owner decision to drop, since the data contract cannot be revised without one.

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
