# RUN-SAFETY.md, Health Trail

How a long unattended run stays safe and recoverable. Every rule here answers a documented failure mode from real unattended Claude Code runs, not a hypothetical one. `AGENTS.md` covers delegation; this covers everything else about surviving a long session.

The single sentence version: **nothing may destroy work, nothing may loop forever, nothing may quietly claim to have done something it did not do, and the repository must be complete enough that a session with total memory loss can pick up from it.**

---

## 1. Set these up first, in Phase 0, before any feature work

Three guards prevent the large majority of accidents in long autonomous runs. Install them before writing app code, because after the accident is too late.

### 1.1 Block destructive commands

A hook that runs before every shell command and refuses the ones that destroy work. At minimum, blocked outright:

`rm -rf`, `git reset --hard`, `git checkout .`, `git checkout --`, `git restore .`, `git clean -fd`, `git push --force`, `git push -f`, `git branch -D`, `git rebase`, `git filter-branch`, `git filter-repo`, and anything that rewrites published history.

The hook refuses the command and returns a message saying it was blocked and why. It does not ask for permission, because in an unattended run there is nobody to ask. If a blocked command genuinely seems necessary, that goes to the BLOCKED section of DECISIONS.md and the run continues on something else.

This exists because the most expensive documented accident in unattended runs is exactly this: an agent clearing a working tree or force pushing and destroying hours or weeks of work that no test would have caught.

**The guard is not installed, and as of 2026-08-02 that is settled rather than suspected.** Do not run the probe below and do not spend a session's opening on it. **It has never fired through Claude Code, on any day, in any session**, which was established by running five ordinary commands in a fresh session and watching a log that records every inspected command gain nothing. D64 and issue #128 carry the account, and **B5 is what the owner needs to do**, which is to install the same guard from `~/.claude/settings.json` at a path with no space in it.

**The agent cannot fix this**, because Claude Code refuses to let a session edit the hooks that constrain it. That refusal is correct and is not to be worked around. **Until B5 lands, rule 6 in `CLAUDE.md` is followed by hand**, which is what has actually protected three long unattended runs, alongside Claude Code's own auto mode classifier.

**Once B5 lands, the probe is one line and it comes first:**

    cat ~/.claude/health-trail-guard.log

**A line stamped inside the session reading it is the only evidence that counts.** The guard logs every command it inspects, passed or blocked, so an absence of lines is an absence of guard. Then confirm the refusal itself with a blocklisted command that is harmless if it runs, since Claude Code's own classifier will refuse the frightening ones before this guard is ever consulted and that refusal is not evidence about this guard:

    git restore --version

**Refusal looks like this**, and nothing else counts:

> Blocked by the Health Trail destructive command guard (RUN-SAFETY.md section 1.1).

Record the outcome in DECISIONS.md either way, including a pass.

**Two things that look like evidence and are not.** A command refused by Claude Code's auto mode classifier says nothing about this guard, because the classifier answers first. And feeding the script a payload by hand proves the script rather than the wiring: every line this log has ever held was written that way, which is exactly how the guard looked installed for three sessions while never running once.

**Every hook command that interpolates a path is quoted.** This project lives at a path containing spaces, `/var/home/Kamsiob/Kamiob Apps/-- Android/Health Trail`. An unquoted `${CLAUDE_PROJECT_DIR}/...` is split by the shell, the executable is never found, and the hook exits 127. A PreToolUse hook blocks on exit 2 and only on exit 2, so 127 passes the command straight through. That is D49, and it is the bug this section is warning about rather than a hypothetical.

**Feeding the script a payload proves nothing about the wiring.** It proves the script. The hook is a separate question and it is the one that failed.

**A fix made mid session does not take effect in that session.** Configuration is read at session start. This was confirmed with a sentinel hook that also never fired, so a guard repaired during a run stays inert for the rest of it, and that run has only rule 6 in `CLAUDE.md` protecting the device.

### 1.2 Save state before context compaction

A hook on the compaction event that writes and commits the current state to HANDOFF.md before the context is compacted.

**Unproven as of 2026-08-01, and it has never once fired.** It carried the same unquoted path defect as guard 1 and exited 127 every time. The quoting is fixed, and unlike guard 1 there is no way to trigger this one deliberately: compaction happens when it happens. The evidence it worked will be a commit appearing in `git log` at a compaction boundary that no session remembers making. Until such a commit exists, treat this guard as absent and keep HANDOFF.md current by hand.

This matters more than it sounds. Compaction summarizes older parts of a session to make room, and two things follow from it that are documented and repeatable. Instructions dilute, meaning rules that were clear early in a session lose force after several rounds of compaction. And the session can revert to an earlier understanding of the project, then redo or overwrite work it already completed, because from its own point of view that work never happened. A state file written just before compaction is what makes the difference between a session that re-orients and a session that repeats itself.

### 1.3 Cap retries and escalate

After three failed attempts at the same thing, stop. Write to the BLOCKED section of DECISIONS.md what was attempted, what happened each time, and what you would try next. Then start the next item.

The failure this prevents is the documented loop where an agent fixes, checks, sees the same error, fixes again, and repeats twenty times, reporting success each round. It is not dishonest, it is fixing the same wrong thing repeatedly, and without a cap it can consume an entire run.

Also cap any subagent with a turn limit so a delegated task cannot spin indefinitely, and never point a subagent at a paid external service in a loop.

**This one is not a hook and nothing calls it.** `.claude/hooks/retry-guard.py` is a command line tool, `attempt`, `clear`, and `status`, that a session has to choose to run. No session ever has. It is not miswired the way guards 1 and 2 were, but the effect is the same, so the count of three working guards was wrong on all three. Call it when something fails a second time, or the cap is only a rule in a document.

---

## 2. Recognizing compaction and recovering from it

Compaction is normal and expected in a long run. It is not a failure. Handling it badly is.

**The tell to watch for:** you find yourself reading a file you already read this session, or you cannot remember a decision you know was made. That is compaction, not confusion.

**What to do, in order:** stop the current action before writing anything. Re-read HANDOFF.md in full. Re-read CLAUDE.md. Run `git log --oneline -20` and `git status` to see what actually exists versus what you remember. Check the open issues for the current phase. Only then continue, and only from what the repository says is true, never from memory.

**The rule underneath it:** after compaction, the repository is the truth and your recollection is not. Never re-create something without first checking whether it already exists. Never revert something you do not remember writing.

**Precision increases as a session lengthens.** Early on, a general instruction is workable. Deep into a compacted session it is an invitation to drift. Late in a run, work from explicit file paths, explicit function names, and explicit acceptance criteria taken from the issue, rather than from a remembered intention.

---

## 3. Never claim work that was not done

Two documented ways an agent reports success for a change that does not exist on disk.

**A delegated write that was silently denied.** A subagent cannot ask for permission mid-task, and a subagent running in the background automatically denies anything that would have prompted. The result is an agent that continues and reports success-shaped output about an edit that never happened. The guard is structural and it is in `AGENTS.md`: subagents never write. Nothing to deny, nothing to silently fail.

**A memory-loss overwrite.** After compaction, redoing completed work can overwrite the finished version with a worse one.

**So, before marking anything complete:** verify it against the working tree rather than against your recollection. `git diff HEAD` and `git status` show what genuinely changed. An issue is closed only when the behavior was verified on the device or emulator, never because code was written. If a subagent reports a change, confirm the change exists before believing the report.

---

## 4. Git as the recovery mechanism

The whole safety model rests on the repository being current, so:

- **Commit and push after every working increment.** Never leave more than one unit of work uncommitted. An uncommitted hour is an hour that a crash or a compaction can erase.
- **Commit before starting anything risky**, meaning a migration, a dependency change, a refactor across files, or anything touching the schema.
- **Feature branches for substantive work,** merged through a pull request that references its issue. Trivial fixes and documentation may go straight to the default branch.
- **Never rewrite published history.** No force push, no rebase of anything already pushed, no amending a pushed commit.
- **If something is broken, fix forward.** Do not revert to a previous state to escape a problem unless the owner has decided that, since a revert during an unattended run is how completed work disappears.

**If branch protection blocks a merge because a check is failing,** the failing check is a bug to fix. Never bypass protection, never disable the check, and never merge around it. If it genuinely cannot be fixed, note it in BLOCKED and continue on work that does not depend on that merge.

---

## 5. Things that can stall Phase 0, and the fallback for each

Each of these is a real setup dependency that could halt an unattended run before any app code exists. In every case: try, and if it fails, log it to BLOCKED with the exact command and error, then continue with everything that does not depend on it. Never stall the run, never wait for the owner.

| If this fails | Do this instead |
|---|---|
| The GitHub CLI is not authenticated, so the repository cannot be created | Initialize git locally, commit everything, note in BLOCKED that the remote needs to be created and pushed, and keep building locally. Nothing about the app depends on the remote existing. |
| Commit signing cannot be configured | Commit unsigned, note it in BLOCKED with what is needed, and continue. Do not spend a run debugging signing. Signing is applied going forward once available, never retroactively. |
| Continuous integration cannot be made to pass because of a toolchain issue in the runner | Document the exact command that separates genuine failures from known environment noise, record it in DECISIONS.md and HANDOFF.md, use it every time, and continue. Never configure the pipeline to pass while tests fail, and never leave a permanently red badge unexplained. |
| The phone is not connected | Say so, defer only the work that genuinely needs a device, and continue with everything else. Never treat a disconnected device as a reason to stop. |
| An agent definition does not seem to be active | See section 6. Do the work in the main session and continue. |
| A dependency, font, or library version named in a document turns out to be wrong or renamed | Verify the current release yourself, use it, and record what you chose and why in DECISIONS.md. The documents in this folder are not authoritative about the outside world. |

---

## 6. The agent definition timing gotcha

Agent definitions are read when a session starts. If you create them during this session, they will most likely not be usable until the next session begins.

**So:** create the definitions early in Phase 0, commit them, note in HANDOFF.md that delegation becomes available from the next session onward, and then **do the current session's work in the main session without delegating.** Do not restart the session to pick them up, do not spend time troubleshooting why a freshly written definition is not available, and do not treat their absence as a blocker. From the second session onward they load automatically and delegation works normally.

If a definition changes later, the same rule applies: it takes effect next session.

---

## 7. What the repository must always contain

He asked for the repository to be the source of truth after everything is pushed. That means a specific, testable property:

**A session with no memory of anything, reading only the repository, must be able to state what is done, what is in progress, what is next, what was tried and rejected, and why every unusual decision was made.**

Held to that standard, on every commit:

- **HANDOFF.md** is current to within one increment. Where the work stands, the next concrete step, everything tried that failed and whether it is worth retrying, every measurement with real numbers, every environment quirk, an item by item inventory of remaining work marked verified, unverified, partial, not started, skipped, or blocked, the recommended order, anything deferred with what would un-defer it, anything written but not device-verified, the real state of the issue tracker as distinct from what its labels claim, anything waiting on the owner, and every open question. Updated before any pause, when context starts running low rather than after, and whenever anything fails while the details are fresh.
- **DECISIONS.md** records every judgment call as it is made, with reasoning, plus a BLOCKED section listing anything only the owner can resolve, each with exactly what he needs to do.
- **MASTER_SPEC.md and DESIGN.md** describe the app as it currently is. Superseded instructions corrected in place, never left beside their replacements. Anything pending marked pending, never described as built.
- **The issue tracker** has an issue for every bug, feature, and enhancement, including ones you discovered rather than were told about, each with acceptance criteria in checkable terms, real working notes added as progress happens rather than only at closing, an issue number referenced in every commit message, and closure only after device verification.
- **The board** reflects real status, with work in progress genuinely limited to one or two items, and every blocked item naming what it is waiting on.
- **README.md** still answers what this is, who it is for, what it looks like, what it can and cannot do, how to install it, how to build it, and what license it carries. The capability and limitation lists are the parts most likely to quietly become false, so re-read them against reality on any commit that changes capability.

**Two failure modes to avoid specifically.** A tracker or document that is wrong during active work is worse than none, because decisions get made against it. And a document that overstates completion is worse than one admitting something is half finished, because the next session builds on top of the claim and the error compounds.

---

## 8. The end of every session

Commit and push everything. Update HANDOFF.md, DECISIONS.md, MASTER_SPEC.md, DESIGN.md, and README.md so they match reality. Update and close the issues that are genuinely done. Post a board status update if the phase moved. Export the build per the platform rules. Then a short plain summary of what was done, what remains, and everything in BLOCKED.

If the session ends unexpectedly, the last committed HANDOFF.md is what the next one inherits, which is why it is never more than one increment out of date.
