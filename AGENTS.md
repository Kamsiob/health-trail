# AGENTS.md, Health Trail

The authoritative document on how work is divided between the main Claude Code session and its subagents. `kamsiob-project-template.md` does not cover this subject, so nothing here contradicts it. Where any other document mentions delegation it points here rather than restating rules. `RUN-SAFETY.md` covers everything else about surviving a long unattended run.

**Two rules govern everything below.**

**One: subagents never write anything.** They read, run, check, and report. The main session acts on their reports. This is not a stylistic preference, it is the guard against a specific documented failure described in section 3.

**Two: delegation is never allowed to block progress.** If any part of it fails, is unavailable, or returns nothing useful, the main session does the work itself immediately, logs one line in DECISIONS.md, and continues. Never stall on a subagent, never retry more than once, never ask the owner about one.

---

## 1. How this actually works

A subagent is a separate instance the main session spawns for one scoped task. Three properties of it drive every rule in this document.

**It starts cold.** It does not see the conversation, the files already read, or the decisions already made. The only channel into it is the prompt written when the task is delegated. Anything it needs, including file paths, error text, and the relevant project rule, has to be in that prompt.

**Only its final message comes back.** Every file it read and every line of output it processed stays in its own context and never enters the main session's. That is the entire point: the noisy work happens somewhere else and only the conclusion returns.

**It cannot ask for permission.** There is no mechanism for a subagent to stop, ask the owner a question, and wait. A subagent running in the background automatically denies anything that would have prompted, and then keeps going as if nothing happened.

That third property is why subagents never write.

## 2. The division of labor

**The main session owns this project.** The plan, the schema, the specifications, the living documents, the git history, the issue tracker, the board, and the device. It is the only thing that decides anything and the only thing that changes anything.

**Subagents gather and report.** Nothing else.

**Two things are deliberately not used here.** No second orchestrating session, because two sessions produce two partial handoffs that each claim to be complete and route architecture disagreements to an owner who does not read code. No git worktrees, because their purpose is isolating parallel file edits and this project has exactly one writer.

### The single writer rule

Only the main session writes to any of these. No subagent, under any circumstance:

HANDOFF.md, DECISIONS.md, MASTER_SPEC.md, DESIGN.md, README.md, CLAUDE.md, and every other living document. All commits, branches, pushes, and merges. Everything on GitHub, meaning issues, comments, labels, the board, milestones, and releases. Anything under `/contract`, since it is shared by two platforms. Any application source file. The connected device.

A subagent that thinks one of these should change says so in its report.

### The delegation test

**A lot of output and a small conclusion means delegate. Changes the plan, the schema, or the code means keep.**

Delegate: running the test suite and reporting only failures. Walking a persona script and reporting what broke. Auditing all 57 templates against `templates/SCHEMA.md`. Sweeping the four locale catalogs for missing keys. Verifying a library or font version. Listing every permission in the merged manifest with its source.

Keep: designing a table. Writing a screen. Resolving an open question. Deciding whether a phase gate was met. Anything touching the device. Anything that needs a decision partway through.

## 3. Why subagents never write

The most expensive documented failure in delegated work is this sequence. A subagent is asked to make a change. The change requires a permission that would normally prompt. A background subagent auto-denies it rather than asking. The subagent continues and returns a report describing the change as done. The change does not exist on disk. Nothing errored. Nothing looked wrong.

Scoping subagents to read-only tools removes the failure completely, because there is no write tool to be denied.

Two consequences that both matter:

- **Every subagent here gets read-only tools.** Not "should mostly not write." Cannot write.
- **Never trust a report that claims something changed.** Verify against the working tree with `git status` and `git diff HEAD` before believing it or marking anything complete.

## 4. Tool scoping, and the trap

**Leaving the tool list off a definition grants every tool, not none.** This is the single most important mechanical fact in this document. An unscoped agent inherits everything the main session can do, including editing files and running shell commands. Every definition in this project names its tools explicitly.

The scopings for this project:

| Agent | Tools | Model | Notes |
|---|---|---|---|
| reviewer | Read, Grep, Glob | main model | Judgment work, so it runs on the capable model |
| test-runner | Bash, Read, Grep | lighter model | Bash to run suites. Emulator only, never the device |
| sweeper | Read, Grep, Glob | lighter model | Pure pattern checking, no judgment |
| researcher | Read, Grep, Glob, WebFetch, WebSearch | lighter model | The only one with network access |

Give each a turn limit so a delegated task cannot spin indefinitely. Nothing here needs write access, shell access beyond running tests, the GitHub CLI, or git, which keeps the permission surface small enough that nothing can get blocked mid-run.

Verify the current field names and file location for agent definitions against the Claude Code documentation before writing them, rather than trusting this table's format, and record what was used in DECISIONS.md. The official reference is https://code.claude.com/docs/en/sub-agents.

## 5. The four subagents

Each definition's description field is what the dispatcher reads to decide whether to delegate, so write it as a trigger condition rather than as a summary for a human.

**reviewer.** Runs at every phase gate. Two jobs: the cold read test from the template's section A4b, and the content compliance audit from `TESTING-PERSONAS.md` section 5. Returns a findings list: what failed, where, and how it was checked. The main session opens the issues from it, which is why the reviewer needs no write access at all.

This is the highest value of the four. It is the only mechanism in the project that gives a second reading of work the owner cannot review himself.

**test-runner.** Runs suites and persona scripts on the emulator. Returns failing tests with their error output and nothing else. Never the connected device.

**sweeper.** Mechanical checks with no judgment: template data against its schema rules, locale catalogs for missing keys, the manifest permission list, source sweeps for banned patterns. Returns a list. Fixes nothing.

**researcher.** Version and documentation verification, which the standards already require before integrating anything. Returns the current release, the recommended integration path, and the license, as short facts.

## 6. Briefing a subagent

A thin briefing is the second most common way delegation wastes time. The subagent starts cold and the prompt is the only way in, so every delegation includes:

The exact question or task, stated as one thing. The specific paths or files in scope. Any project rule that applies, quoted rather than referenced, since a subagent may not have read the same documents. What the answer should look like. And an explicit instruction to return a summary rather than a transcript.

That last one matters because the isolation only keeps a subagent's internal work out of the main context. Its final message does come back, so five verbose reports can fill the context the delegation was meant to protect. Ask for failures only, or the three files that matter, rather than everything it saw.

Scope a task so it fits in one agent's context. Ask for the specific thing rather than an open-ended sweep of a whole area.

## 7. Timing, and not tripping over it

**Agent definitions load at session start.** Definitions written during a session are usually not usable until the next one begins.

So in Phase 0: write the definitions, commit them, note in HANDOFF.md that delegation becomes available next session, and do that session's work in the main session without delegating. Do not restart the session to pick them up, do not troubleshoot why a new definition is not there, and do not treat it as a blocker. From the second session onward they load automatically. `RUN-SAFETY.md` section 6 says the same thing, and neither is a reason to pause.

## 8. Limits worth knowing

Subagents cannot spawn subagents, so any chaining is orchestrated by the main session. Do not fan out more than three at once, because more does not go faster and it makes both context and quota unpredictable. Prefer custom definitions over the built-in exploration agents where a project rule matters, since the built-ins skip the project instruction file to stay fast. Every delegated task consumes quota proportionally, which is why three of the four run on a lighter model.

## 9. What gets recorded

At every phase gate, HANDOFF.md records which subagents ran, on what, and what they found, next to the persona runs and their fixture seeds.

That record is the point. It is how a later session knows the reviewer genuinely read phase four, rather than inheriting an assumption that somebody did.

## 10. How this appears in the repository

The definition files are committed like any other file, so the practice is visible to anyone reading the repository.

**How it is described is constrained, and the constraint comes from the template.** Section A4b of `kamsiob-project-template.md` prohibits the README from claiming multi agent orchestration, agent tooling development, or evaluation infrastructure, and prohibits congratulating the project on its own process. That applies in full and is not softened here.

So the README's How this is built section may state factually, in one clause without adjectives, that specialized agents handle review, testing, and verification, and that their definitions are in the repository. It does not present that as an achievement, does not use the phrase multi agent orchestration, does not explain why the arrangement is good, and does not appear in marketing copy, the store listing, or anywhere in the app. Anyone curious can open the definition files and see exactly what is there, which is a stronger signal than a sentence about it.
