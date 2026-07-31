# Health Trail: deltas to the Kamsiob project template

`kamsiob-project-template.md` sits in this folder and applies in full. This document records where Health Trail departs from it or adds to it, so every departure reads as a decision rather than an omission. Per section A4b, all of this also gets recorded in DECISIONS.md during the build.

**Precedence.** Where this document and the project template disagree, this document wins for this project. Where this document and `contract/DATA-CONTRACT.md` disagree on anything about the data model, the data contract wins. Where anything concerns how work is divided between the main session and its subagents, `AGENTS.md` is authoritative, including the one place where it interacts with a template rule, which is the README wording covered in its section 10. Where anything concerns run safety, destructive commands, context compaction, or recovering from a lost session, `RUN-SAFETY.md` is authoritative.

---

## 1. Section C5 is superseded for this project

The template's C5 still contains the older two-key distribution model, including the instruction that release notes explain the Play build and the GitHub build are signed differently and that switching requires uninstalling one first. That is obsolete. The canonical template needs the same correction eventually, but this project uses the version below.

**Replacement C5, Release and distribution:**

Signing uses Play App Signing. Generate an upload key in the final phase, store it outside the repository, and note plainly for the owner where it lives and that it should be backed up. Because Google holds the actual app signing key, a lost upload key is recoverable, and there is only ever one signing identity for the app.

There is one signature and two distribution paths. Upload the app bundle to Google Play. After Play finishes processing, download the Google-signed universal APK from the Play Console and publish that exact file as the GitHub release asset. Both paths therefore carry the same signature, which means a user can install from either source, switch between them freely, and keep their data. Never build and sign a separate local release APK for GitHub.

The sequencing consequence is unavoidable and must be respected: the GitHub release asset cannot exist until the bundle has reached Play and been processed. Every release goes Play upload first, wait for processing, download the universal APK, then publish the GitHub release.

Any wording anywhere in the project stating or implying that the two builds are signed differently, or that switching requires an uninstall, is wrong and must be removed. That includes release notes, the README, store copy, LAUNCH.md, and any build prompt.

Everything else in C5 stands: the organization Play Console account under B7 Collective with the public developer name Kamsiob, the service account and Google Cloud project for the Publisher API, the JSON key that is never committed, the manual steps that cannot be automated, and LAUNCH.md listing the owner's exact remaining clicks.

---

## 2. Additions to C2, platform specifics

The template's stack applies: Kotlin, Jetpack Compose, Material 3 with a fully custom theme, single activity, one SQLite database encrypted at rest with SQLCipher using a key in the Android Keystore.

Additions specific to Health Trail:

- **The schema is not defined in Kotlin.** It comes from `contract/schema.sql`, per the data contract. The build copies it in. There is no second hand-maintained copy.
- **At-rest encryption and export encryption are separate concerns.** The database is encrypted with a keystore-held key that never leaves the device. The export container is encrypted with a user passphrase, because a portable file cannot depend on one device's keystore. Do not conflate them.
- **No native code, no NDK, no inference.** Health Trail runs no model. The C7 section of the template about on-device models does not apply to this project at all. If any future feature appears to need it, that is a specification conversation, not an implementation decision.
- **Right to left layout is a Phase 0 concern**, not a localization pass at the end. Arabic ships in v1. Every screen is built direction-aware from the first screen. Retrofitting RTL is the kind of task that quietly consumes a week.
- **Fonts must cover four scripts.** The design faces do not include Chinese or Arabic coverage, so a Noto fallback chain is required and must be verified by rendering real strings in all four languages, not assumed.

---

## 3. Play Store listing constraints

- **Never select the Medical category.** Productivity or Lifestyle. Health Trail is a record-keeping app.
- **No health claims in store copy.** The listing describes organizing information, keeping records, and staying on top of paperwork. It does not describe improving outcomes, managing conditions, or anything a regulator would read as a health function.
- **Complete any required declaration truthfully.** If Play's health apps declaration is genuinely triggered by the app storing information the user typed in, fill it out honestly rather than working around it. Verify the current policy at submission time rather than trusting this document. The honest answers are unusually easy here: no data collected, nothing transmitted, no account, no medical function.
- Data Safety: no data collected, no data shared, and say so.

---

## 4. The first-launch disclaimer is a gate

Before any part of the app is usable, on first launch, the user sees a plain-language screen and must explicitly accept it. Substance, in the app's own voice, not legalese:

Health Trail is a record-keeping app for family caregivers. It is not a medical app. It gives no medical advice. Nothing in it replaces a doctor, a nurse, emergency services, or legal advice from a lawyer. If someone needs urgent help, call emergency services. What you record here is yours, it stays on this phone, and you are responsible for what you write down.

The same substance appears in the About screen and in the store listing. The accept action is recorded locally with a timestamp so it is not shown repeatedly. It is not shown again after acceptance, and there is no version of the app that skips it.

---

## 5. Global content rules, enforced in code not by discipline

These come from the template catalog's schema and apply to every screen that displays a value:

- **No ranges, no thresholds, no color coded values, no warnings, no judgments** on any measurement, anywhere, ever. No "normal", no "high", no red numbers, no sorting or highlighting by value.
- **Charts render gaps as gaps.** Never interpolate across a gap. Never imply a missing entry is a failure.
- **Fields that record clinical information record what a clinician said**, and the field label must say so. Wound staging and growth measurements are the clearest cases.
- **Pattern language requires a minimum-data threshold.** Below it, the app says what it has and stops. Counts are allowed. Interpretation never is.
- **No educational or advisory content anywhere**, which is a deliberate exclusion belonging on the Not planned screen, not a gap to be filled later.

---

## 6. Content, licensing, and originality

- The app's code is AGPLv3. The template content in `templates/` is CC BY-SA 4.0 and must ship with its license file and correct attribution, the same pattern as other Kamsiob projects that bundle content.
- **Every user-facing string is original.** Nothing is copied from government pamphlets, ombudsman publications, law firm pages, other applications, or any other source. Structures may be mirrored. Sentences may not. Stating that a federal rule exists is a fact and is fine.
- **No volatile facts in shipped content.** No contractor names, no dollar limits, no phone numbers, no agency names subject to renaming. Generic descriptions instead, so nothing goes stale and quietly becomes wrong.
- **Rights are stated precisely.** The federal tag on a standing instruction refers specifically to federal rules for nursing homes participating in Medicare or Medicaid, and the interface must not imply that backing carries over to assisted living, home care, or hospitals.
- Templates ship bundled and offline. The optional downloadable pack mechanism, using signed hash-verified packs from GitHub releases, is a later addition and also the mechanism behind the future Facility Edition. Design the loader with that in mind, ship only bundled content in v1.

---

## 7. Abandonment resistance, treated as requirements

Roughly half of health app users quit over data entry burden, and most who quit do so within the first weeks. These are therefore functional requirements, not polish:

- **Every capture field is optional.** Rough dates are allowed. Half a note is a valid note. Anything the person could not categorize goes to an Unfiled tray where the app suggests a home by plain word matching and the person confirms with one tap. The app never files anything on its own.
- **Lapse tolerance.** Stopping for months is normal and never treated as failure. On return, older months fold closed and the digest says what changed since they were last here, with no guilt, no streaks, no gamification, and no catch-up prompts.
- **Sparse data grace.** Every generated summary works honestly with two entries.
- **Multiple ways in**, in the system integration phase: a home screen widget, a quick settings tile, and a share sheet target so a photographed bill or an emailed PDF lands directly in capture.
- On-device voice dictation for notes is deferred, not rejected. Transcription only, which does not violate the deterministic rule.

---

## 8. Data model shape

- **Single point person.** One perspective, one owner of the notebook. The Emergency Card lists several emergency contacts, but the app is not built for co-decision-makers, and features must not creep toward shared editing.
- The structural axes are chapters for places, care threads for parallel ongoing streams, and the trail for chronology. Every entry can carry all three. Every screen is a lens on the same entries.
- **Everything links to everything it touches.** No dead ends. A medication knows its own incidents, questions, and chart. A person knows every call and visit involving them. This is the feature that makes the app more than a form collection, and it has to be in the schema from the start, not bolted on.

---

## 9. What Phase 0 must include beyond the template's version

The template's Phase 0 is repository, scaffolding, and a smoke test. For this project it additionally includes everything in section 10 of `contract/DATA-CONTRACT.md`: the monorepo layout, the contract folder, the sync-ready schema, the change log, the transport interface, the export container with a passing round trip test, the web scaffold that opens the same schema, and the first test vectors.

That is a larger Phase 0 than usual. It is deliberate. Every item in it is something that cannot be added later without discarding user data or reimplementing a platform.
