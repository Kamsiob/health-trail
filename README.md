# Health Trail by Kamsiob

[![License](https://img.shields.io/badge/license-AGPL--3.0-2F6F8F)](LICENSE)
[![Content license](https://img.shields.io/badge/templates-CC%20BY--SA%204.0-4E8A5C)](templates/LICENSE-CONTENT.md)
[![Status](https://img.shields.io/badge/status-in%20development%2C%20not%20yet%20installable-D99D2B)](https://github.com/Kamsiob/health-trail/issues/1)

**A private care notebook for family caregivers. Everything stays on your phone.**

> **Not installable yet.** This is being built in the open and Phase 0 is in progress. There is no release, no APK, and no store listing. [Issue #1](https://github.com/Kamsiob/health-trail/issues/1) is the current state, and the [board](https://github.com/users/Kamsiob/projects/2) has the detail.

---

## What this is, and who it is for

When someone in a family needs ongoing care, one person ends up holding the whole picture. They make the phone calls, sit in the meetings, chase the paperwork, and remember what the night nurse said three weeks ago. Providers rarely talk to each other, so in practice that person becomes the information hub whether they wanted to be or not.

Health Trail is that hub, given structure. One trail of every call, visit, incident, medication change, document, and dollar, kept for years, searchable in seconds, and never leaving the phone.

It is for the son, daughter, spouse, or parent doing that work. It is not for the patient, and it is not for clinicians.

It is a record-keeping app. It is not a medical app, and it gives no medical advice.

## What it looks like

Real screenshots go here once there are screens to capture, and are recaptured whenever a screen changes. Nothing in this section will ever be a mockup or a rendering of a design file.

Until then, `reference/screen-grid.html` holds the 27 approved screens as the binding visual reference, and `DESIGN.md` holds the tokens, type scale, motion, and copy rules the built app is held to.

## What it can do

Nothing yet. The list below is what is being built, and this section becomes a description of the shipped app as each part lands rather than a promise.

- **Capture that forgives.** One button on every screen, six things to log: a call, a visit, an incident, a measurement, a question for next time, a document. Every field is optional. Rough dates are allowed, including "sometime this week". Anything you cannot categorize goes to an Unfiled tray where the app suggests a home and you confirm with one tap. The app never files anything on its own.
- **Three ways to see the same entries.** Chapters answer where, as a place and a period. Care threads answer what is ongoing, as parallel streams like physical therapy or wound care. The trail answers when. Every entry can carry all three.
- **Everything connects.** A medication knows its own incidents, its pending questions, its dose history, and its place on the emergency card. A person knows every call and visit involving them. A bill knows its chapter, the call where it was disputed, and the standing instruction it broke. You never have to remember where something was filed.
- **An emergency card** designed to be handed to a paramedic.
- **Standing instructions** recording what you asked, of whom, when, how it was acknowledged, and every documented violation, tagged by whether federal nursing home rules back it up or it is your request. The difference is stated precisely, including where the backing stops.
- **Exports that stand on their own.** An incident, a project, a chapter, a month, or the whole notebook, generated on your phone as a document a relative can read without ever having seen the app.
- **Four languages,** English, Spanish, Chinese, and Arabic, with right to left layout built in from the first screen rather than added at the end.
- **Automatic local backup** to a folder you choose, with no cloud involved.

## What it cannot do, and will not

These are decisions rather than gaps. The [roadmap](https://github.com/Kamsiob/health-trail/issues/21) carries the reasoning for each.

- **No cloud, no server, no account.** There is nothing to sign in to. Sharing means exporting a document and sending it yourself, which also means there is no live shared view for several family members.
- **No medical or legal advice, and no educational content.** The app records, organizes, and counts. It never concludes.
- **No target ranges, normal values, thresholds, or color coding by value.** No arrows, no judgments on any measurement. Where a field records a clinical assessment, it records what a clinician said and the label says so.
- **No reminders, alerts, or medication dose tracking.** It keeps the record. It does not nag, and the medications screen says that plainly rather than burying it.
- **No streaks, badges, or engagement notifications.** Stopping for months is normal. When you come back, it tells you what changed since you were last here, with nothing that implies the gap was a failure.
- **No ads, no subscription, no paywall, no in-app purchase.**
- **No analytics, no telemetry, no crash reporting.** No network calls at all.
- **No model, no inference, no AI in the app.** Every digest and count is deterministic, composed from your own entries, and every line taps through to the entry it came from.

## Install

There is no release yet. When there is, it will be on Google Play and as a GitHub release asset. Both carry the same signature, so you can install from either and switch between them without losing anything.

## Build from source

You need JDK 21, the Android SDK, and Python 3.

```
git clone https://github.com/Kamsiob/health-trail.git
cd health-trail/android
./gradlew assembleDebug
```

The build reads `contract/schema.sql` and `templates/data/*.json` and copies them into the app's assets. It fails loudly rather than falling back to a stale internal copy, so if it cannot find them you are running Gradle from the wrong directory.

`CONTRIBUTING.md` has the full setup, the conventions, and how to run the tests.

## How this repository is laid out

```
contract/     the schema, the export format, the message catalogs, and the golden
              test vectors. Platform neutral. Both the app and the web scaffold
              read from here, and neither keeps its own copy.
templates/    57 care templates as JSON, published separately under CC BY-SA 4.0
              so they are useful to people who never install anything.
android/      the Kotlin application.
web/          a scaffold whose only job is to open the same schema, which is what
              stops the two platforms drifting. It has no features.
tools/        the fixture generator, the compliance checks, and the build scripts.
reference/    the 27 approved screens.
```

## Approach and methodology

The app is specified before it is built, and the specification is kept current with the code rather than written once. `MASTER_SPEC.md` is what the app is, `DESIGN.md` is binding on every visual and copy decision, and `contract/DATA-CONTRACT.md` governs the data model and cannot be changed without an explicit decision, because changing a schema after real data exists means discarding someone's records.

Decisions are recorded in `DECISIONS.md` as they are made, with the alternatives considered and the reasoning, so the same question does not get reopened later. The issue tracker and the board are the authoritative record of what is done, what is in progress, and what is blocked. Work is verified on real hardware before it is marked complete, and an issue is closed only after the behavior was checked on a device or an emulator, never because code was written. `HANDOFF.md` is kept current to within one increment so the project can be picked up cold.

### How this is built

The implementation is written by Claude Code, a coding agent, working from the specification documents in this repository, directed by one person who does not write code.

That person's half is the part the agent cannot do: deciding what the app is and who it is for, writing and owning the specifications, resolving what happens when two of them conflict, judging whether the built thing is actually usable by an exhausted person in a hospital corridor, and testing it against a real situation rather than a test case.

Directing long autonomous runs turned out to require a specific set of guards, each answering a failure that happens rather than one that might. A run can destroy hours of work with a single command, so destructive commands are refused by a hook rather than avoided by intention. Context gets compacted on a long run, after which the session can revert to an earlier understanding and redo work it already finished, so state is committed and pushed before compaction and the repository is treated as the truth afterward rather than memory. An agent that hits the same error repeatedly will fix the same wrong thing twenty times and report success each round, so attempts are capped at three and then escalated in writing. A delegated task that needs a permission cannot ask for one, and silently reports success for a change that never reached disk, so the agents that assist are scoped to read only and cannot write anything. Work is claimed complete only against the working tree, never against recollection.

What came out of it is a repository where the specification, the reasoning, and the state are all readable by someone arriving with no context, and where the app's promises about medical advice, interpretation, and data leaving the device are checked by tests on every push rather than upheld by good intentions.

Specialized agents handle review, testing, and verification, and their definitions are in `.claude/agents/`.

## License

Code is [AGPL-3.0](LICENSE).

Template content in `templates/` is [CC BY-SA 4.0](templates/LICENSE-CONTENT.md), published separately so it is useful with a paper binder, a spreadsheet, or a notes app, by people who never install anything.

## Support this work

Built and carried by one person. If software made this way matters to you, there is a place to stand behind it. Either way, it is yours.

[Support this work](https://buymeacoffee.com/kamsiob)

## Elsewhere

- Website: https://kamsiob.com
- GitHub: https://github.com/kamsiob
- YouTube: https://youtube.com/@kamsiob
- Telegram, the Kamsiob Lab group: https://t.me/+g5LKm9rUnNcxMjk5
- Feedback: hello@kamsiob.com. One person builds this, everything gets read, and not everything gets a reply.
