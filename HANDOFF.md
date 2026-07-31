# HANDOFF.md, Health Trail by Kamsiob

Rewritten to current truth on every commit. If you are a session with no memory, this file plus `git log` and the issue tracker is everything you need. Read this in full, then read `CLAUDE.md`, then continue only from what the repository says is true.

**Last updated:** 2026-07-31, branch `feat/14-encrypted-database` in progress.

---

## 1. State of play

**Phase:** 0, foundation, contract, and repository.

**Where exactly:** the foundation is built and running on real hardware. Seven of the nineteen Phase 0 issues are closed and device verified. There is no notebook to write in yet, which is Phase 1.

**Done and verified:** #2 documentation, #3 continuous integration, #4 monorepo layout, #5 the schema, #11 the Android project and design tokens, #19 the subagent definitions, #20 the smoke test. Plus #22, a content bug found by the template validator.

**Still open in Phase 0, in the order I would take them:**

| Issue | What | Why this order |
|---|---|---|
| #14 | Encrypted database, SQLCipher, key in the Keystore, schema from the copied asset | Everything else that stores anything sits on top of it |
| #6 | Locally generated collision safe time ordered ids | Needed by the first insert #14 makes |
| #8 | Repository layer that makes tombstone filtering structural | The only safe way to read, so it comes before anything reads |
| #7 | Prove the change log append is transactional through Kotlin | The schema already proves it. This proves the Kotlin path |
| #13 | Four locale catalogs, ICU MessageFormat, right to left verified on a screen | The engine composes from these, so they precede it |
| #15 | Golden test vectors both platforms run | Defines correct before the engine exists |
| #9 | Export container, manifest, encryption, round trip equality | Needs the database and the repository layer |
| #10 | `SyncTransport` with the file implementation behind it | Needs the export container |
| #16 | Web scaffold opening the same schema | Needs nothing else, can be done any time. `npm` is absent, see question 5 |
| #12 | Fonts for four scripts, verified on a device | Independent, but pointless before there are screens to look at |
| #17 | Deterministic fixture generator | Needs the schema and the repository layer to write through |

**The precise next action:** get an emulator running, which is blocked, see `DECISIONS.md` B4. The AVD for this project is **`health-trail-api36`** and it is correctly created; the emulator process exits with code 1 and no error the moment full startup begins, from every launch strategy tried. The most likely cause is that it is being started from inside a sandboxed shell session. Start it from an ordinary terminal outside this session, then run `./tools/verify.sh --device` here, which picks up any attached `emulator-` serial. **Never run the instrumented suite against the phone:** it creates and writes a database.

**One thing to know before writing that code.** Android's `execSQL` refuses any statement that returns rows, and `PRAGMA journal_mode` returns one. `ContractAssets.splitStatements` already handles the statement splitting including trigger bodies, and routes pragmas through `rawQuery`. Reuse it rather than writing a second splitter.

---

## 2. Remaining work inventory, in order

Phase 0 only. Later phases are in `MASTER_SPEC.md` section 8 and are not restated here. Status values: not started, in progress, partial, verified, blocked, skipped.

| # | Item | Status |
|---|---|---|
| 0.1 | Three safety guards installed and tested | **verified** |
| 0.2 | Git initialized, signing configured, first commit | **verified** |
| 0.3 | Public GitHub repository created, remote added, pushed | **verified** |
| 0.4 | Monorepo layout: `/contract`, `/templates`, `/android`, `/web`, `/tools` | **partial.** All five directories exist. `/contract` holds the schema and the export format. `/android` and `/web` are empty until #11 and #16. Issue #4 |
| 0.5 | Repository documents: README, ARCHITECTURE, CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, PRIVACY, LICENSE, CHANGELOG | **verified.** Issue #2. ARCHITECTURE marks each subsystem pending rather than describing it as built |
| 0.6 | `.github`: issue templates, pull request template, FUNDING.yml | **verified.** Issue #2 |
| 0.7 | Continuous integration workflow compiling every test source set | **verified.** Three jobs, all green on the runner. The Android job assembles, runs unit tests, compiles the instrumented suite, lints, asserts the build fails without the contract, and compares the schema inside the APK to the contract file. Issue #3 |
| 0.8 | Release workflow with artifact provenance | not started, deferred to Phase 8 where there is an artifact to attest |
| 0.9 | Labels, milestone, project board with single-select status and automation configured before population | **partial.** Labels, milestone, board, fields, and all 20 items done. The two built-in board automations are BLOCKED B2, owner action, one minute |
| 0.10 | Pinned roadmap issue including the deliberate exclusions | **verified.** Issue #21, pinned |
| 0.11 | Branch protection on status checks, no review requirement | **verified.** Both checks required, force pushes and deletion refused, no review requirement. Administrator enforcement deliberately off so documentation fixes can go direct while behavior changes go through a pull request |
| 0.12 | `contract/schema.sql`: every column from data contract section 3 on every user data table, plus `change_log` and `conflict_log` | **verified by test.** 34 user data tables, all six columns each, no AUTOINCREMENT, 34 live views, 68 triggers. `tools/checks/check_schema.py` asserts it and was negative tested. Issue #5 |
| 0.13 | `contract/export-format.md` | **verified.** Written. The container, the manifest, encryption as separate from at-rest encryption, atomic and honest import, and the eight hostile files that must fail cleanly |
| 0.14 | `contract/i18n/` four locale catalogs, ICU MessageFormat | not started |
| 0.15 | `contract/test-vectors/` covering empty, one entry, two entries, gap, plural boundaries | not started |
| 0.16 | Android Gradle project, single activity, Compose, minimum and target SDK verified against current Play requirement | **verified on device.** Builds, installs, launches on the Pixel 10 Pro XL. Issue #11 |
| 0.17 | Design tokens for both themes, contrast measured and ratios recorded in DECISIONS.md | **verified.** 80 pairs measured across both themes by `tools/checks/check_contrast.py`, which runs on every push. Five tokens corrected, and the capture button glyph is no longer white. Ratios in DECISIONS.md D19 and DESIGN.md section 2.3. Issue #11 |
| 0.18 | Fonts: display and body faces confirmed by current name and license, Noto fallback chain for four scripts | not started |
| 0.19 | Four-locale i18n scaffold with RTL working | not started |
| 0.20 | Database layer: SQLCipher, key in Keystore, schema applied from the copied `schema.sql` asset, no second copy in Kotlin | **written, compiles, not device verified.** SQLCipher opens with a Keystore-wrapped 32 byte passphrase, schema executed from the asset, no Kotlin schema definition. `DatabaseTest` is written and unrun, see DECISIONS.md D21. Issue #14 |
| 0.21 | Repository layer making it structurally difficult to query without filtering tombstones | **partial.** The `live_*` views exist and are asserted to filter. The Kotlin repository layer and the static check that forbids raw table access are still to do. Issue #8 |
| 0.22 | Locally generated collision-safe ids, no auto-increment on any user data table | **verified by test.** UUID version 7, ordered within a millisecond and safe against a backward clock. 7 unit tests including 200,000 ids for uniqueness and 50,000 for ordering. Issue #6 |
| 0.23 | Every write appends to `change_log` in the same transaction, proven by a test | **partial.** Enforced by triggers in the schema and proven by `check_schema.py`, including that a failing log write rolls the data write back. Still needs the same proof through the Kotlin layer. Issue #7 |
| 0.24 | `SyncTransport` interface with the file implementation behind it, reconciliation ignorant of transport | not started |
| 0.25 | Export container: manifest, version check, encryption, round trip equality test passing on an emulator | not started |
| 0.26 | `/web` scaffold opening the same schema through SQLite in WebAssembly and reading the same template JSON | not started |
| 0.27 | Fixture generator in `/tools` per `TESTING-PERSONAS.md` section 1 | not started |
| 0.28 | Four subagent definitions in `.claude/agents/`, tools explicitly scoped | **verified.** reviewer (Read, Grep, Glob, opus), test-runner (Bash, Read, Grep, sonnet, emulator only), sweeper (Read, Grep, Glob, sonnet), researcher (Read, Grep, Glob, WebFetch, WebSearch, sonnet). Every one names its tools and carries a `maxTurns` limit. Issue #19 |
| 0.29 | Smoke test proving the app launches | **verified on device.** Six instrumented tests pass on the Pixel 10 Pro XL, covering launch, the contract reaching the device, the schema executing there, and the template count. Issue #20 |
| 0.30 | Phase 0 gate: content compliance checks in continuous integration, living documents current, board status update | not started |

---

## 3. Environment, so a fresh session does not have to rediscover it

| Thing | Value |
|---|---|
| Working folder | `/var/home/Kamsiob/Kamiob Apps/-- Android/Health Trail` |
| Platform | Linux, Fedora derivative, kernel 7.1.3 |
| GitHub CLI | Authenticated as `Kamsiob`. Scopes: `gist`, `project`, `read:org`, `repo`, `workflow`. **No `admin:ssh_signing_key`**, see BLOCKED B1 in DECISIONS.md |
| Default `java` | OpenJDK 26 from Homebrew. **Too new for Gradle and the Android plugin.** |
| JDK to actually use | `/home/linuxbrew/.linuxbrew/opt/openjdk@21` |
| Android SDK | `~/Android/Sdk`. `ANDROID_HOME` is **not** set in the environment, so set it explicitly |
| SDK platforms present | android-36, android-36.1, android-37.0, android-37.1 |
| Build tools present | 36.0.0, 37.0.0 |
| adb | `~/Android/Sdk/platform-tools/adb`, **not on PATH** |
| Connected device | Pixel 10 Pro XL, serial `57241FDCQ0000H`, authorized |
| Emulator AVD | **`health-trail-api36`**, created for this project. Never reuse an AVD belonging to anything else. System images under `~/Android/Sdk/system-images` |
| Node and npm | **Absent.** Affects how the `/web` scaffold gets built. See item 0.26 |
| Python | 3.14.6, on PATH as `python3` |
| Signing key | ed25519, no passphrase, already in `~/.ssh/allowed_signers`, and registered with GitHub. The path is in this repository's git config under `user.signingkey` |

---

## 4. Decisions a future session might otherwise reverse

Full reasoning is in `DECISIONS.md`. The short list of things not to undo:

- The first commit is deliberately a faithful snapshot of the handover folder, unreorganized. Do not squash or rewrite it.
- Commit signing is on and repository-scoped. Commits showing unverified on GitHub is expected until BLOCKED B1 is done by the owner. It is cosmetic. Do not turn signing off to make it go away.
- The guards are Python rather than shell plus `jq`, because `jq` is not confirmed present and a guard that fails open silently is worse than none.
- No APK or AAB is ever copied outside the Gradle output path. This contradicts template section C4 deliberately.
- Part B of the template, the Linux desktop prompt, is out of scope. The `/web` scaffold is not, because it is a data contract Phase 0 acceptance criterion.

---

## 5. Blocked

**One item is blocked: the emulator will not start here, so `DatabaseTest` cannot run and #14 cannot close.** Full detail in `DECISIONS.md` B4, including all five attempts, the precise symptom, and what would unblock it. Everything that does not need an emulator passes. The three original blockers are resolved. Kept here with outcomes, because a blocked list that only grows teaches a reader that nothing here gets fixed.

- **B1, commit signing. Done.** The owner registered the key. Verified: the account lists one signing key and `main` reports `verified=true, reason=valid`. It applied to the whole existing history at once. This is the first Kamsiob repository with signed commits.
- **B2, board automations.** Deliberately not switched on, and not a blocker. `tools/board.py sync` keeps the board current at every increment, and auto-add being off was verified empirically rather than assumed. The board is public.
- **B3, hosted privacy policy. Done, after a correction worth reading.** Canonical for this app is `https://kamsiob.com/health-trail.html#privacy`. **Not** `privacy.html#health-trail`, which is a longer all-products page the canonical one links to as "the full policy". That link is not a signal that the longer page governs. I got this wrong once by following the link instead of the instruction. `PRIVACY.md` now carries a warning at the top so nobody switches it back.

## 6. Device state

Nothing installed. No application ID exists yet. The connected Pixel 10 Pro XL has not been touched.

The standing rules about it: exactly one copy of this app on that phone at all times, every install after the first is an in-place upgrade, never uninstall to work around a problem, every destructive or data-affecting test runs on an emulator, and never capture a screenshot unless this application is in the foreground.

---

## 7. Uncommitted work

None. This file is being committed in the same commit as the work it describes.

---

## 8. Open questions

The six in `MASTER_SPEC.md` section 10 are still open and are mine to decide and log as I reach them. None are decided yet. In order of when they will be forced:

1. Tombstone retention window. Forced by item 0.12, the schema, since the contract requires the window written into the schema comments.
2. Whether the change log is exported. Also forced by item 0.12 and 0.13.
3. Attachment size and count limits. Forced by item 0.12.
4. Whether the web scaffold uses the same UI toolkit or a minimal one. Forced by item 0.26, and the absence of npm bears on it.
5. PDF pagination for very large exports. Not forced until Phase 5.
6. How the app describes its own translation status honestly, given no native-speaker review has happened. Not forced until Phase 1 copy, but affects the store listing and README.

Additionally, one contradiction inside `DESIGN.md` needs deciding at item 0.17: section 3 item 4 sets a 13sp minimum text size, and section 4.3 defines the Mono style at 11sp while explicitly exempting only the nav label. Both cannot be true.

---

## 9. Screens built without a mockup

`reference/screen-grid.html` covers 27 screens and the app needs more than that. Every screen built without one is composed from existing components under `DESIGN.md` section 10, ships complete with every state, and is logged in three places at the moment it is built: a `needs-design-review` issue with a device screenshot, an entry in `DESIGN.md` section 8, and a line here.

This list exists so the owner can review them all in one sitting instead of archaeologically. **Never save these up for a phase gate.**

| Screen | Built | Issue | Composed from | Reviewed |
|---|---|---|---|---|
| *none yet* | | | | |

**Known ahead:** the template library, the four template pickers, the template detail view, and the template editor. All of them land in Phase 1 or Phase 4 and none is drawn. `MASTER_SPEC.md` section 4.10 carries their requirements in detail, including that all four template kinds share one presentation and that browsing, previewing, and applying must be visually distinct.

## 9. Persona runs

None yet. `TESTING-PERSONAS.md` requires each run to be recorded here with its fixture seed and date. A persona walked against a schema that has since changed has not been walked.

---

## 10. Subagents

**All four definitions are written and committed, and none of them has been used.** Definitions load at session start, so ones written during a session are not usable until the next session begins. That is expected, documented in `AGENTS.md` section 7 and `RUN-SAFETY.md` section 6, and is not a blocker. This session did all of its own work without delegating, which is what those documents say to do.

**From the next session onward, delegation works.** The four, with what each is for:

| Agent | Tools | Model | Use it for |
|---|---|---|---|
| `reviewer` | Read, Grep, Glob | opus | Every phase gate. The cold read test and the content compliance audit. The highest value of the four, because it is the only second reading of work the owner cannot review himself |
| `test-runner` | Bash, Read, Grep | sonnet | Suites and persona scripts, **emulator only**, never the connected phone. Returns failures and nothing else |
| `sweeper` | Read, Grep, Glob | sonnet | Mechanical checks with a right answer: locale key gaps, manifest permissions, banned patterns, raw table access |
| `researcher` | Read, Grep, Glob, WebFetch, WebSearch | sonnet | Version, license, and policy verification before integrating anything. The only one with network access |

**The rule that does not bend:** none of them can write, and that is structural rather than stylistic. A subagent cannot stop and ask for permission, so one running in the background silently denies anything that would have prompted and then reports success for a change that never reached disk. No write tool means nothing to deny. When any of them reports that something changed, verify against the working tree before believing it.

**Record their runs here,** at every phase gate: which ran, on what, and what they found. That record is how a later session knows the reviewer genuinely read phase four rather than inheriting an assumption that somebody did.
