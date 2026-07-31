# HANDOFF.md, Health Trail by Kamsiob

Rewritten to current truth on every commit. If you are a session with no memory, this file plus `git log` and the issue tracker is everything you need. Read this in full, then read `CLAUDE.md`, then continue only from what the repository says is true.

**Last updated:** 2026-07-31, branch `feat/4-5-monorepo-and-schema`, the schema.

---

## 1. State of play

**Phase:** 0, foundation, contract, and repository.

**Where exactly:** the safety guards, the repository, the tracker, the required documentation, the monorepo layout, and the canonical schema exist. No application code exists yet: no Android project, no web scaffold, no Kotlin at all.

**Just completed, and how it was verified:**

- The three safety guards. Guard 1 at `.claude/hooks/block-destructive.py`, wired as a `PreToolUse` hook on Bash. Verified by feeding it 25 hook payloads directly: 13 destructive commands refused with exit code 2 and a plain reason, 12 legitimate commands allowed through including `git push origin main`, `git checkout -b`, `git restore --staged`, `git merge`, and `./gradlew clean`. That test exercises the distinction between `git restore .` and `git restore --staged`, and between `git branch -D` and `git branch -d`. Guard 2 at `.claude/hooks/precompact-save-state.sh`, wired as a `PreCompact` hook, not yet observed firing because no compaction has happened. Guard 3 at `.claude/hooks/retry-guard.py`, verified across four attempts on one label, escalating on the fourth.
- Repository live at https://github.com/Kamsiob/health-trail, public, 14 topics set, first commit pushed and signature verified as good.
- Tracker: 21 issues. #1 is the Phase 0 parent with 19 children, #21 is the pinned roadmap. Every issue carries acceptance criteria in checkable terms. Milestone `v0.1.0 Foundation` created and applied to all Phase 0 issues.
- Labels: 19, being 5 `type:` labels, 10 `area:` labels, plus `release-blocking`, `blocked`, `good first issue`, and `help wanted`. GitHub's default noise labels deleted.
- Board at https://github.com/users/Kamsiob/projects/2, 20 items, every one carrying Status, Platform, Area, Priority, and Size. Fields and their options were configured before any item was added. Description and README written.
- Documentation: README, ARCHITECTURE, CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, PRIVACY, CHANGELOG, LICENSE, plus two issue templates, an issue template config, a pull request template, and FUNDING.yml.
- Compliance verified by grep across the whole repository: zero em dashes, zero en dashes, zero British spellings. Three issue bodies used the British spelling of color, were corrected, and all 21 issues re-verified clean.

**In progress right now:** issues #4 and #5 on branch `feat/4-5-monorepo-and-schema`. The monorepo layout and `contract/schema.sql`.

**What that branch contains so far:** `contract/schema.sql`, 1,669 lines, 34 user data tables each carrying the six required columns, 34 `live_*` views that filter tombstones, 68 change log triggers, 47 indexes, plus `app_meta`, `device`, `change_log`, `conflict_log`, and `schema_migration`. `contract/export-format.md`. `tools/checks/check_schema.py`, which loads the schema into a real database and asserts both its shape and its behavior, and which was negative tested against six deliberately broken schemas and caught all six.

**The precise next action:** open the pull request for `feat/4-5-monorepo-and-schema`, confirm continuous integration passes on it, merge it, then start issue #11, the Android Gradle project. Note that the Android job has to be added to `.github/workflows/ci.yml` as part of #11, which is what unblocks #3.

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
| 0.7 | Continuous integration workflow compiling every test source set | not started, issue #3. First cut covers compliance checks only, since no Android project exists to compile |
| 0.8 | Release workflow with artifact provenance | not started, deferred to Phase 8 where there is an artifact to attest |
| 0.9 | Labels, milestone, project board with single-select status and automation configured before population | **partial.** Labels, milestone, board, fields, and all 20 items done. The two built-in board automations are BLOCKED B2, owner action, one minute |
| 0.10 | Pinned roadmap issue including the deliberate exclusions | **verified.** Issue #21, pinned |
| 0.11 | Branch protection on status checks, no review requirement | not started. Needs a status check to exist first, so it follows issue #3 |
| 0.12 | `contract/schema.sql`: every column from data contract section 3 on every user data table, plus `change_log` and `conflict_log` | **verified by test.** 34 user data tables, all six columns each, no AUTOINCREMENT, 34 live views, 68 triggers. `tools/checks/check_schema.py` asserts it and was negative tested. Issue #5 |
| 0.13 | `contract/export-format.md` | **verified.** Written. The container, the manifest, encryption as separate from at-rest encryption, atomic and honest import, and the eight hostile files that must fail cleanly |
| 0.14 | `contract/i18n/` four locale catalogs, ICU MessageFormat | not started |
| 0.15 | `contract/test-vectors/` covering empty, one entry, two entries, gap, plural boundaries | not started |
| 0.16 | Android Gradle project, single activity, Compose, minimum and target SDK verified against current Play requirement | not started |
| 0.17 | Design tokens for both themes, contrast measured and ratios recorded in DECISIONS.md | not started |
| 0.18 | Fonts: display and body faces confirmed by current name and license, Noto fallback chain for four scripts | not started |
| 0.19 | Four-locale i18n scaffold with RTL working | not started |
| 0.20 | Database layer: SQLCipher, key in Keystore, schema applied from the copied `schema.sql` asset, no second copy in Kotlin | not started |
| 0.21 | Repository layer making it structurally difficult to query without filtering tombstones | **partial.** The `live_*` views exist and are asserted to filter. The Kotlin repository layer and the static check that forbids raw table access are still to do. Issue #8 |
| 0.22 | Locally generated collision-safe ids, no auto-increment on any user data table | not started |
| 0.23 | Every write appends to `change_log` in the same transaction, proven by a test | **partial.** Enforced by triggers in the schema and proven by `check_schema.py`, including that a failing log write rolls the data write back. Still needs the same proof through the Kotlin layer. Issue #7 |
| 0.24 | `SyncTransport` interface with the file implementation behind it, reconciliation ignorant of transport | not started |
| 0.25 | Export container: manifest, version check, encryption, round trip equality test passing on an emulator | not started |
| 0.26 | `/web` scaffold opening the same schema through SQLite in WebAssembly and reading the same template JSON | not started |
| 0.27 | Fixture generator in `/tools` per `TESTING-PERSONAS.md` section 1 | not started |
| 0.28 | Four subagent definitions in `.claude/agents/`, tools explicitly scoped | not started |
| 0.29 | Smoke test proving the app launches | not started |
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
| Emulator AVDs | `android-36`, `kamai-mig`. System images present under `~/Android/Sdk/system-images` |
| Node and npm | **Absent.** Affects how the `/web` scaffold gets built. See item 0.26 |
| Python | 3.14.6, on PATH as `python3` |
| Signing key | `~/.ssh/kamai_signing`, ed25519, no passphrase, already in `~/.ssh/allowed_signers` |

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

Three items, none of which stops any work. Full detail in `DECISIONS.md` under BLOCKED, each with the exact steps the owner needs.

- **B1.** Commits show as unverified on GitHub until the owner registers the SSH signing key. Cosmetic. Nothing depends on it, and registering the key later verifies the whole existing history at once.
- **B2.** The board's two built-in automations need one visit to the project settings, about a minute. Until then the board is maintained by hand, which works but is the exact thing that goes stale during a long run.
- **B3.** A hosted privacy policy URL is needed at release, not now. Recorded early so it is not discovered during the release.

---

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

## 9. Persona runs

None yet. `TESTING-PERSONAS.md` requires each run to be recorded here with its fixture seed and date. A persona walked against a schema that has since changed has not been walked.

---

## 10. Subagents

The four definitions from `AGENTS.md` section 5 are not written yet, item 0.28. Definitions load at session start, so ones written during this session will not be usable until the next session begins. That is expected and is not a blocker. This session does all of its own work without delegating.
