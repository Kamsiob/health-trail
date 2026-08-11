# Contributing to Health Trail

One person maintains this. Response times vary, sometimes by a lot. Everything gets read.

The most valuable thing anyone can contribute is not code. It is a description of what actually happened to you while keeping track of someone's care, and where this app got in the way. That is the kind of thing no amount of testing finds.

## Before you start

Read the [roadmap](https://github.com/Kamsiob/health-trail/issues/21). It lists what is planned and, more usefully, what this app will deliberately never do. Cloud sync, accounts, shared editing, reminders, medical advice, and anything that interprets a measurement are all on the second list on purpose, with the reasoning next to each one.

A change that adds one of those will not be accepted, however well it is written. That is not a judgment on the code. It is that the app makes specific promises to people who are already dealing with enough, and the promises are the product.

## How to report a problem

Open an issue using the bug report template. It asks for the version, the device, the language, and whether anything you had written disappeared or came back wrong. That last question decides how urgently it gets looked at.

**Do not paste anything from your own notebook into a public issue.** Screenshots and logs from a real notebook contain private health information about a real person, usually someone who did not choose to use this app. Describe what happened, or reproduce it with made up names first.

Anything that could expose someone's records goes to [SECURITY.md](SECURITY.md) instead, privately.

## How to propose a change

Open an issue using the feature or change template before writing code. It asks what situation you were in when the need came up, which is more useful than a feature description, because a real situation often has a better answer than the one being requested.

For anything beyond a small fix, wait for a reply on the issue before building it. This avoids the worst outcome, which is someone spending an evening on a change that was never going to be merged.

## How the project is specified

The specification documents in this repository are authoritative, not descriptive. They are written before the code and corrected in the same commit as any change that makes them wrong.

The implementation is written by a coding agent working from those specifications. That is why the documents are the source of truth rather than the code, and why a pull request that changes behavior is expected to update the relevant document in the same change. If the code and `DESIGN.md` disagree, `DESIGN.md` is right and the code is a bug.

In precedence order: verified code, then `HANDOFF.md`, then `DECISIONS.md`, then `contract/DATA-CONTRACT.md` for anything about the data model, then `DESIGN.md` for anything visual, then `MASTER_SPEC.md`.

`contract/DATA-CONTRACT.md` is a special case. It cannot be revised without an explicit decision from the owner, because changing the schema after real data exists means discarding someone's records.

## Setting up to build

You need:

- **JDK 21.** Newer versions are not yet supported by the Android Gradle plugin in use here.
- **The Android SDK,** with the platform named by `compileSdk` in `android/app/build.gradle.kts`.
- **Python 3** for the template catalog build and the fixture generator in `tools/`.

Then:

```
git clone https://github.com/Kamsiob/health-trail.git
cd health-trail/android
./gradlew assembleDebug
```

The build reads `contract/schema.sql` and `templates/data/*.json` and copies them into the app's assets. It fails loudly if it cannot find them rather than falling back to a stale internal copy. If you see that failure, you are running Gradle from the wrong directory.

## Testing expectations

- `./gradlew test` for the unit suite.
- `./gradlew assembleAndroidTest` compiles the instrumented suite. Continuous integration runs this on every push, because an instrumented suite that nobody builds rots silently and a suite that does not compile is worse than no suite.
- `./gradlew connectedAndroidTest` runs it on a connected device.

**`connectedAndroidTest` uninstalls the application and takes its data with it.** If the device holds anything worth keeping, export through the app first and reimport afterward. That is a checklist step, not a reason to avoid running the tests.

Data survival across updates is proven by the export and import round trip against the golden vectors in continuous integration, not by a long lived installation.

Every bug fixed gets a regression test, so it cannot come back.

No change is finished until it survives the export, wipe, import round trip with field by field equality. Anything the app can store, the export must contain and the import must restore. A feature that stores something the export does not carry silently loses records on device migration, which for this audience is the worst thing the app can do.

## Coding conventions

- **Kotlin**, Jetpack Compose, single activity.
- **The schema is never defined in Kotlin.** It lives in `contract/schema.sql`. Do not add a second copy, in any form, including Room entities or generated code from a separate schema file.
- **Never query a user data table directly.** Go through the repository layer, which filters tombstones by construction. One forgotten `deleted_at IS NULL` is a data leak of something a user believed they deleted.
- **Layout uses start and end, never left and right.** Arabic ships in v1 and every screen is direction aware.
- **Never assemble a user facing sentence by concatenating fragments.** Compose it from a message template in `contract/i18n/`. Concatenation breaks in every language except English.
- **Design tokens, never literal values.** Colors, spacing, radii, and type come from the theme.

## Copy conventions

These are enforced by tests, not by review.

- **No em dashes** in anything a person reads. Commas, periods, and colons instead. Source code is exempt where the character is functionally required, for example inside a regular expression or a test fixture.
- **American English.** Color, organize, behavior, artifact, license, catalog, gray.
- **Never interpret, advise, or judge.** The app says what it counted and stops. Counts are allowed. "This suggests a pattern" is not, in any form.
- **Never imply a lapse is a failure.** "Since you were last here" is correct. "You have not logged anything in 3 weeks" is not.
- Second person, warm, never familiar. No exclamation points, no hype, no fear language, and nothing that congratulates someone for using the app.

## Commit convention

```
type: imperative summary under about 70 characters

Body explaining the reasoning where it is not obvious from the diff.
Wrapped at 72 columns. Says what changed and why, not what the issue
title already said.

Refs #12
```

Types in use: `feat`, `fix`, `docs`, `design`, `data`, `i18n`, `test`, `build`, `perf`, `refactor`, `chore`.

Every commit references its issue with `Refs #N`, or `Closes #N` when it genuinely finishes the work. The specific convention matters far less than following it without exception.

## Branch hygiene

**One branch per issue, named for its issue.** `feat/207-empty-states`, `fix/58-subject-scoped-counts`. The number is what lets somebody arriving later connect a branch to the reasoning behind it.

**Branches are deleted automatically when their pull request merges.** The repository setting is on and was verified on 2026-08-11. Nothing needs deleting by hand after a merge.

**No long-lived branches besides `main`.** If a branch outlives the issue it was named for, the issue was too big.

**Do not use `git branch --merged` to decide what is safe to delete here.** This repository squash-merges, which rewrites a branch's commits into one new commit with a different sha, so git cannot see that the work is already in `main` and will report merged branches as unmerged. **GitHub is the source of truth**, because it records which pull request merged which branch: `gh pr list --state merged --json number,headRefName`. `DECISIONS.md` D144.

## Pull requests

Substantive work goes through a branch and a pull request. Branch names reference the issue, as in `feat/12-noto-fallback-chain`. One logical change per branch.

The pull request template asks what changed, which issue it closes, and how it was tested including on what device. Fill it in properly. "Tested locally" tells a reader nothing.

Continuous integration must pass. If it fails, the failing check is the bug. It never gets disabled, bypassed, or merged around.

## What will not be accepted

- Anything on the deliberately not planned list.
- Anything that adds a network call, an account, telemetry, or a crash reporting service.
- Anything that interprets a measurement, adds a range or a threshold, or colors a value by whether it is good or bad.
- Anything that adds an engagement mechanic: streaks, badges, notifications pulling someone back, or a completion percentage about their own diligence.
- A dependency that brings a permission with it, unless the permission is justifiable to a user in one sentence.
- A change to `contract/DATA-CONTRACT.md` or to the schema, without an explicit decision from the owner first.
- Code with no test, where the thing being changed is testable.

## License

By contributing you agree that your contribution is licensed under AGPL-3.0, the same as the rest of the code. Template content in `templates/` is CC BY-SA 4.0 and stays that way.
