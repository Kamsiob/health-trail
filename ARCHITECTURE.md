# Architecture

How Health Trail is put together, for someone who wants to understand or modify it.

**Status marker.** This document describes the app as it currently is, and every section says whether the thing it describes is built. Nothing here is described as built until it is, because a document that overstates completion is worse than one admitting something is half finished. **Brought current on 2026-08-27, for the 1.1 release**, having spent months saying the app did not exist.

**What exists right now:** all of it except the web scaffold. The monorepo layout, the canonical schema with its change log triggers and tombstone filtering views, the encrypted database, the repository layer, the deterministic engine, the export container and its published format, the fixture generator, the compliance checks, and 89 screens shipped on Google Play. **What does not:** the web scaffold, which is issue #16 and holds a README saying so.

---

## 1. The shape of the whole thing

```
contract/          platform neutral. The source of truth for anything shared.
  schema.sql       the canonical schema as DDL, with comments        [exists]
  EXPORT-FORMAT.md the export container specification                [exists]
  i18n/            message catalogs, ICU MessageFormat               [exists]
  test-vectors/    golden input and expected output per locale       [exists]
templates/         57 care templates as JSON                         [exists]
android/           the Kotlin application                            [exists]
web/               scaffold proving the contract, no features        [pending]
tools/             fixture generator, compliance checks              [exists]
```

Two rules hold this together, and both are enforced rather than trusted:

1. **Nothing in `/contract` imports from `/android` or `/web`.** It is pure data and specification.
2. **Neither platform defines its own copy** of the schema, the export format, the templates, or the message catalog. They read from `/contract` and `/templates`. The Android build copies both in at build time and fails loudly if it cannot read them, rather than falling back to a stale internal copy.

The reason is specific. A schema that exists only as Kotlin code makes the web version a reimplementation rather than a second reader, and two implementations of the same schema drift apart within weeks. Publishing the schema as the contract is what stops that.

## 2. Where the real constraints come from

Almost every structural decision in this app traces to one of four constraints. Knowing them explains most of the code.

**The app is used for years.** A notebook with twelve entries and one with two thousand entries across eight chapters and five ended care threads are different pieces of software. Queries are written to page rather than to load, filters live in SQL rather than in application code, and the test fixtures can generate any point in a five year history on demand.

**A future sync must be possible without discarding data.** Three things are impossible to retrofit: sync onto auto-increment primary keys has no correct merge when two devices both create row 47, sync onto a schema that deletes rows resurrects deleted entries on the next connection forever, and a second platform against an undocumented schema is a reimplementation. So every row carries a locally generated id, deletion is a tombstone, and every write appends to a change log. All three are in the first migration.

**Right to left is not a localization pass at the end.** Arabic does not ship in version one, D180, and this was designed in from the first screen anyway because it is the part that cannot be added later. Every screen is direction aware from the first screen, layout uses start and end rather than left and right, and the trail itself mirrors. The deterministic engine composes sentences from per-locale message templates rather than concatenating fragments, because concatenation breaks in every language except English.

**The app must never conclude.** No medical advice, no interpretation, no ranges, no thresholds, no color coding by value. This is a constraint on the rendering layer and on the engine, not only on the copy, and it is checked by tests that assert against rendered components rather than against strings.

## 3. Data storage and protection

**Status: built.**

`contract/schema.sql` is the schema, the Android app executes it on the device, and SQLCipher, the Keystore key and the Kotlin repository layer are all in place. Issues #14 and #8 are closed.

One SQLite database is the entire data store, encrypted at rest with SQLCipher using a key generated in and held by the Android Keystore. The key never leaves the device and is never written to preferences, a file, or a log.

The database is created by executing `contract/schema.sql`, copied into assets at build time. There is no Kotlin schema definition, no Room entity set, and no second hand-maintained copy anywhere.

### Every row

Every user data table carries the same six columns, without exception:

| Column | Purpose |
|---|---|
| `id` | A locally generated, collision safe, time ordered id. Never an integer, never auto-increment, never reused. The only thing foreign keys point at. |
| `created_at` | Milliseconds since epoch, UTC. Set once. |
| `updated_at` | Milliseconds since epoch, UTC. Touched on every write. Also the conflict comparison key. |
| `deleted_at` | Null means live. A timestamp means deleted. |
| `origin_device` | The random local id of the device that created the row. |
| `rev` | Increments on every local write to the row. |

**Deletion is always a tombstone.** Set `deleted_at`, bump `rev`. The only two exceptions are the full data wipe, which genuinely removes everything including tombstones, and a tombstone purge that has no peers to acknowledge it yet and is therefore dead code until direct sync exists.

### The repository layer

Tombstone filtering is enforced by construction rather than by remembering. Live rows are reachable only through a path that filters them, reading tombstones requires calling something whose name says so, and a static check fails the build on a raw table query outside that layer.

This is not fussiness. One forgotten `deleted_at IS NULL` is a data leak of something the user believed they had deleted, and the symptom is an entry reappearing in a search result or an export months later.

### The change log

**Status: built and enforced by the database itself.**

One append-only table records every insert, update, and delete, written **in the same transaction as the write itself**. A write that succeeds while its log entry fails would be a silent hole in the record.

This is enforced by two triggers per user data table, written into `schema.sql`, rather than by the repository layer. A repository layer satisfies the requirement right up until someone adds a write path that forgets, which happens once, silently. A trigger cannot be forgotten and cannot run in a different transaction than the statement that fired it. It also means the guarantee holds on the web platform without being reimplemented there. Reasoning in DECISIONS.md D14.

The `delete` operation is derived rather than declared: deletion here is an update that sets `deleted_at`, so the update trigger records `delete` when `deleted_at` moves from null to set, and `update` otherwise. Undeleting records an `update`, which is correct, because a peer needs to know the tombstone was lifted.

It exists for a future sync, where it answers the only useful question a peer can ask, which is give me everything after sequence N. Without it, sync means diffing whole tables and cannot tell an edit from a delete then recreate. It is also useful immediately: it is what tells the Today digest what changed since the person was last here.

`seq` is local only and meaningful only on the device that wrote it.

### Attachments

Content addressed. A photographed bill is stored as a file named by the hash of its bytes, with a row holding the hash, the original filename, the mime type, and the size. Three consequences follow, all good: an attachment can never conflict because identical bytes are the same file, transferring one twice is free, and a corrupt transfer is detectable by rehashing.

## 4. The three structural axes

**Status: built.**

Every entry can carry all three, and this is in the schema from the first migration rather than bolted on:

- **Chapters** answer *where*. A place and a period: home, a hospital stay, a rehab facility. Chapters hold their own entries, documents, and care team, and the care team archives with the chapter while staying searchable.
- **Care threads** answer *what is ongoing*. Parallel streams running at the same time: physical therapy, wound care, nursing. Each has its own color and history, and can end while the notebook continues.
- **The trail** answers *when*. One chronological record.

Every screen in the app is a lens on the same entries through some combination of these three, which is why a screen is rarely more than a query plus a renderer.

## 5. The deterministic engine

**Status: built. There is no model and no inference anywhere in the app.**

Every digest, month review, prep sheet, and pattern count is produced by querying real rows, doing all arithmetic in code, and composing sentences from per-locale message templates. There is no model, no inference, and no interpretation anywhere in the app.

The rules it obeys:

- All arithmetic in code, never in a template string.
- Sentences composed from message templates with proper plural and date handling per locale, never assembled by concatenating fragments.
- Counts are allowed. Interpretation is not. A line stating a count is followed by a plain line saying that what it means is the person's to judge.
- Trend and pattern language only above a minimum-data threshold. Below it, the engine says what it has and stops.
- Gaps are stated as gaps, never interpolated.

The engine will exist twice, in Kotlin and in TypeScript, so `contract/test-vectors/` holds golden input and expected output per locale. Both platforms run the same vectors and continuous integration fails if they produce different bytes for the same input.

**Why not a model.** Correctness is verifiable, output is testable against golden files, it runs instantly with no download and no battery cost, and it cannot invent a number. For an app whose entire value is being an accurate record, a summary that might be subtly wrong is worse than no summary.

## 6. Sync, designed now and built later

**Status: interface pending. No network code exists or will in v1.**

Nothing in v1 talks to another device. The export file is the bridge between devices, and it is enough.

The transport is an interface with a single implementation. `SyncTransport` declares advertise, discover, pair, send, and receive. The v1 implementation is the file: export writes the container, import reads it. The reconciliation code takes rows and returns decisions and has no knowledge of files, zips, or transports, so a local network transport later slots in behind the same interface without touching it.

**Conflict policy, decided now.** Health Trail has a single point person, so conflicts are the same human editing on two devices rather than two people disagreeing. Per row, last write wins, compared on `updated_at`, with `origin_device` as a deterministic tiebreaker. Nothing is silently discarded: the losing version is written to `conflict_log` with both versions intact and the user gets a plain notice. Attachments never conflict, because they are content addressed. Tombstones win over edits.

Field level merging is deliberately not built. A future session should not add it.

## 7. The export container

**Status: built, published byte for byte in `contract/EXPORT-FORMAT.md`, and proven by a field by field round trip on the signed build.**

One versioned zip, self describing enough that a stranger could read it in ten years:

```
healthtrail-export-YYYYMMDD-HHMM.htx
  manifest.json    format version, app version and platform, timestamp, origin
                   device, row counts per table, sha256 of the database,
                   attachment count and bytes, whether the payload is encrypted
  data.sqlite      the whole database, tombstones included
  attachments/     one file per attachment, named by content hash
```

The format version is in the manifest from release one, and an importer reads it first and refuses a version it does not understand, naming the version found and the versions supported.

**Two encryptions that are not the same thing.** The database is encrypted at rest with a keystore key that never leaves the device. The export is encrypted with a passphrase the user chooses, because a portable file cannot depend on one device's keystore. If that passphrase is lost the file cannot be recovered, there is no server and no backdoor, and the interface says so before the user commits.

Import is atomic and honest: it fully succeeds or changes nothing, and it names any table or column it does not recognize rather than dropping it silently.

The test that matters is not whether import completes. It is field by field equality after export, wipe, and import, including tombstones, ordering, timestamps to the millisecond, every relationship, and every attachment hash.

## 8. Threading and lifecycle

**Status: built.**

Single activity, Jetpack Compose, Material 3 with a fully custom theme, all built. Dynamic color is deliberately not used: the palette carries meaning, gold means the trail and red means the emergency card, and letting the wallpaper reassign those would break the one rule keeping the app from looking clinical.

The build copies `contract/schema.sql` and the template JSON into assets and fails loudly if it cannot read them. That failure is asserted in continuous integration rather than trusted, along with a byte comparison between the schema inside the built APK and the contract file.

Database work happens off the main thread. Anything not instantaneous shows that something is happening from the moment it is triggered rather than after a delay, and anything slow is cancellable in a way that genuinely stops the underlying work.

Long operations survive process death or leave no partial state. Export, import, PDF generation, attachment capture, backup, and template application are each tested for it, because on a phone used in a hospital corridor the process will be killed halfway through at some point.

## 9. What is deliberately absent

Recorded here so nobody adds them back as an oversight.

- **No network layer.** There is no HTTP client, no retrofit, no socket, and no permission for one.
- **No analytics, telemetry, or crash reporting.**
- **No model, no inference, no NDK, no native code.**
- **No account, session, or identity code.**
- **No background work that talks to anything.** The only scheduled job is the local backup writer, which writes a file to a folder the user chose.
- **No iOS-shaped abstractions.** iOS is out of scope and is not being anticipated.
