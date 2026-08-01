# Health Trail roadmap

What is planned, what is being worked on now, and what this app will deliberately never do.

**Kept current as phases complete.** The [project board](https://github.com/users/Kamsiob/projects/2) has the detail and the issue tracker has the reasoning. Where this file and the board disagree, the board is newer.

**Last brought current:** 2026-08-01.

---

## What Health Trail is

A private care notebook for family caregivers. One trail of every call, visit, incident, medication change, document, and dollar, kept for years, searchable in seconds, and never leaving the phone.

It is for the person who becomes the information hub because providers rarely talk to each other. Not for the patient, not for clinicians.

---

## Now

**Phase 0, the foundation, is substantially built.** The schema, the encrypted database, locally generated ids, the repository layer, the four locale catalogs, the content compliance checks, and the event date model are all in and verified.

**Phase 1, the notebook core, is where the work is.** What runs on real hardware today: the disclaimer gate, essentials first setup, the situation picker, the notebook table of contents, capture with five of its six inputs, the Unfiled tray, and the date picker. An entry saved reaches the trail, the change log records it in the same transaction, and the counts refresh.

**Phase 0 is deliberately larger than usual**, because every item in it is something that cannot be added later without discarding user data or reimplementing a platform: stable ids, tombstones instead of row deletion, the change log, a published schema both platforms read, event dates that can say "sometime in November", and right to left support designed in from the first screen rather than retrofitted.

### The nearest things still open

- **The export container.** Attachments and the container itself are done. Encryption, import, and the field by field round trip are not. This is the only proof that data survives an update, so nothing ships without it.
- **The template catalog in four languages.** The interface is translated and the 1500 strings of template content are not, so an Arabic reader currently gets an Arabic app wrapped around English content.
- **Today, with the digest engine**, and the trail itself.

---

## Planned

Each of these is one coherent area, and each ends with its testing gate passed and a regression sweep of everything before it.

1. **The notebook core.** The disclaimer gate, essentials first setup, situation templates, Today with its digest, capture with all six inputs and the Unfiled tray, the trail, care team, medications, the emergency card.
2. **Time and structure.** Chapters with transfers and archiving, care threads with filtering and ending, appointments with prep sheets, ask next time, the year scrubber, month review, the milestone arc.
3. **Incidents, instructions, and money.** Incident threads with export, standing instructions with their tags and violation tracking, bills with states and links, running cost sheets.
4. **Projects and documents.** Projects with checklists, waiting on, contacts, and attachments. Documents with categories and a note on where the physical original lives. The full template library, editable and extendable.
5. **Search and assembly.** Universal search, scoped search per section, assembled collections, any day reconstruction, the family update draft, and the PDF export engine.
6. **System integration.** Home screen widget, quick settings tile, share sheet target.
7. **Portability.** Automated local backup to a folder you choose, restore, situation change, closing a trail, and the full data wipe.
8. **Hardening and release.** Every persona walked, accessibility verified, all four languages including right to left, performance at five year scale, and the release.

---

## Deliberately not planned

**These are decisions, not gaps.** Each one is a thing the app could do and will not.

- **No cloud, no server, no account.** There is nothing to sign in to and nothing to leak. Sharing happens by exporting a document and sending it yourself.
- **No shared live editing.** The app is built around a single point person holding one continuous account. Several people editing the same notebook is a different product with different failure modes.
- **No medical advice, no legal advice, no educational content.** The app records, organizes, and counts. It never concludes.
- **No target ranges, normal values, thresholds, or color coding by value.** No arrows, no judgments on any measurement. If a number matters, it matters because a clinician said so, and the app records what they said.
- **No reminders, alerts, or medication dose tracking.** It keeps the record. It does not nag, and the medications screen says so plainly rather than burying it.
- **No engagement mechanics.** No streaks, no badges, no notifications pulling you back, no completion percentages about your own diligence. Stopping for months is normal and is never treated as a failure.
- **No ads, no subscription, no paywall, no in-app purchase.** A single donate link, and the app is fully usable without it.
- **No analytics, no telemetry, no crash reporting.** No network calls at all.
- **No model, no inference, no AI anywhere in the app.** Every digest, summary, and count is deterministic, composed from real entries, and traceable line by line to the entry it came from. **A summary that might be subtly wrong is worse than no summary** for an app whose entire value is being an accurate record.
- **No iOS.** Out of scope, and no iOS shaped abstractions are being added in anticipation.

---

## Under consideration, with no dates and no promises

- **Direct device to device sync**, local network only, explicitly paired, encrypted, manual, no relay and no server ever. The schema is already built so this is possible. It is not in v1, and nothing in v1 talks to another device.
- **A web version**, as a progressive web app keeping every byte in the browser. A scaffold in `/web` whose only job is to prove the schema contract is real. It has no features.
- **On device voice dictation for notes**, transcription only.
- **Downloadable template packs**, signed and hash verified.
- **Simplified Chinese as a bundled typeface.** Latin and Arabic are bundled. Noto Sans SC is around ten megabytes per weight against 680 kilobytes for all six faces currently shipped, so Chinese falls back to the system face. This is a measured tradeoff rather than an oversight, and it may change.

---

## How to read the tracker

- **`release-blocking`** means the release does not happen until it is closed.
- **`needs-design-review`** is the owner's review queue: a screen built without a mockup, composed from existing components, with a real device screenshot on the issue.
- An issue with acceptance criteria in checkable terms is ready to work. One without them is not ready yet.

`DECISIONS.md` carries every judgment call with its reasoning, including the ones that turned out to be wrong and were reversed. It is the honest record rather than the tidy one.

---

## Feedback

Issues on [the tracker](https://github.com/Kamsiob/health-trail/issues), or hello@kamsiob.com.

One person builds this. Everything gets read, and not everything gets a reply.
