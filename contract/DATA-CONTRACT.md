# Health Trail Data Contract

This document is binding on Phase 0. Everything in it must be in place before any feature is built on top of the database, because each requirement here is cheap now and a rewrite later.

Health Trail ships on Android first. A web version and possibly a Linux desktop version follow, and the Android app must be able to sync directly with a computer over a local connection with no server in the middle. None of those are v1 features. All of them are v1 constraints on the data model.

The one sentence version: **the database and the export format are a public contract between three platforms and a future sync engine, so they get designed once, correctly, at the start.**

---

## 1. Why this exists

Three things are technically impossible to add later without discarding user data:

1. **Sync onto a schema with auto-increment primary keys.** Two devices both create row 47. There is no correct merge. Fixing it means reassigning every id and every foreign key on real user data.
2. **Sync onto a schema that deletes rows.** If a row is gone, there is nothing left to tell the other device it was deleted, so the peer resurrects it on the next sync. The deletion appears to undo itself, forever.
3. **A second platform against an undocumented schema.** If the schema exists only as Kotlin code, the web app is a reimplementation rather than a second reader, and the two drift apart within weeks.

So: stable ids, tombstones, and a written contract. All three go in the first migration.

---

## 2. Repository layout

The repository is a monorepo from the first commit, even though only one platform is built at first. Creating this layout later means rewriting every import path and every CI workflow.

```
/contract          platform neutral, the source of truth for anything shared
  schema.sql       the canonical schema as DDL, with comments
  export-format.md the export container specification
  i18n/            message catalogs, ICU MessageFormat
  test-vectors/    golden input and expected output files, see section 7
/templates         the template catalog, already written, consumed by every platform
/android           the Kotlin application
/web               the progressive web app
/tools             pipelines, generators, screenshot scripts
```

Rules:

- Nothing in `/contract` may import from `/android` or `/web`. It is pure data and specification.
- Neither platform may define its own copy of the schema, the export format, the templates, or the message catalog. They read from `/contract` and `/templates`.
- The Android build reads `/contract/schema.sql` and `/templates/data/*.json` at build time, copying them into assets. It does not maintain a second hand-written copy. If the build cannot read them, the build fails loudly rather than falling back to a stale internal copy.
- `/web` exists from Phase 0 with a working scaffold, not an empty folder. See section 6 for what it must actually do.

---

## 3. Every row, without exception

These columns exist on every user data table. Not most tables. Every one.

| Column | Type | Rule |
|---|---|---|
| `id` | TEXT | A UUID generated on the device that created the row. Never an integer. Never auto-increment. Never reused. This is the primary key and the only thing foreign keys ever point at. |
| `created_at` | INTEGER | Milliseconds since epoch, UTC. Set once, never changed. |
| `updated_at` | INTEGER | Milliseconds since epoch, UTC. Touched on every write. |
| `deleted_at` | INTEGER NULL | Null means live. A timestamp means deleted. |
| `origin_device` | TEXT | The id of the device that created the row. Random, local, never transmitted anywhere except to a paired device the user chose. |
| `rev` | INTEGER | Increments on every local write to the row. |

Sortable time-ordered UUIDs are preferred over fully random ones, because they index better and give a stable natural ordering, but any collision-safe unique id generated locally satisfies the contract.

**Deletion is always a tombstone.** Set `deleted_at` and bump `rev`. Never issue a DELETE against a user data table outside of the tombstone purge described below. Every query filters on `deleted_at IS NULL`. Enforce this with views or a repository layer, not with discipline, because one forgotten filter is a data leak of something the user thought they deleted.

Two exceptions to the tombstone rule, both explicit:

- **Full data wipe** genuinely deletes everything, including tombstones. That is the point of it.
- **Tombstone purge** may remove tombstones older than a long retention window, only after every known paired device has acknowledged them. Until direct sync exists there are no peers, so nothing is purged and this is dead code. Write the retention window into the schema comments now so the future implementation does not have to guess.

**Attachments are content addressed.** A photographed bill is stored as a file named by the hash of its bytes, plus a database row holding the hash, the original filename, the mime type, and the size. Consequences, all good: an attachment can never conflict because identical bytes are the same file, transferring one twice is free, and a corrupt transfer is detectable by rehashing.

---

## 4. The change log

One append-only table makes future sync a feature instead of an archaeology project.

```
change_log(
  seq            INTEGER PRIMARY KEY AUTOINCREMENT,  -- local only, never synced
  table_name     TEXT NOT NULL,
  row_id         TEXT NOT NULL,
  op             TEXT NOT NULL,   -- insert | update | delete
  rev            INTEGER NOT NULL,
  changed_at     INTEGER NOT NULL,
  device_id      TEXT NOT NULL
)
```

Every write appends one entry, in the same transaction as the write itself. This is the only way a peer can ask a useful question, which is "give me everything that changed after sequence N". Without it, sync means diffing entire tables on every connection, which is slow, fragile, and cannot tell an edit from a delete-then-recreate.

The log is local. `seq` is meaningful only on the device that wrote it. A peer tracks the last sequence it received *from* each device it has paired with.

The log is also immediately useful before any sync exists: it is what tells the Today digest what changed since the person last opened the app, which is a real v1 feature. Build it in Phase 0 and use it in Phase 1.

---

## 5. Sync, designed now and built later

**What ships in v1:** nothing that talks to another device. The export and import file is the bridge between devices for v1, and it is enough.

**What must be true in v1 so sync is later possible:** everything in sections 3 and 4, plus the transport boundary below.

### The transport boundary

Write the sync engine's transport as an interface with a single implementation:

```
SyncTransport
  advertise()                  make this device findable on the local network
  discover()                   list peers advertising nearby
  pair(code)                   establish a shared secret out of band
  send(bytes) / receive()      an authenticated encrypted duplex channel
```

The v1 implementation of this interface is the file: `export()` writes the container, `import()` reads it. The engine that reconciles rows never knows whether the bytes arrived from a file the user emailed themselves or from a laptop on the same wifi. Later, a LAN or Tailscale transport slots in behind the same interface and the reconciliation logic is not touched.

### Non negotiables for the eventual direct connection

- **Device to device only.** No relay, no rendezvous server, no cloud, no account, no discovery service operated by anyone. If both devices cannot see each other on a network the user controls, sync does not happen and the app says so plainly.
- **Pairing is out of band and explicit.** One device shows a code or a QR, the other enters it, and that establishes a shared secret. Nothing pairs automatically. Nothing trusts a device the user did not confirm.
- **The channel is authenticated and encrypted** even on a local network, because a local network is not a trusted network.
- **Sync is manual and visible.** The user starts it. It shows what it is doing and what it changed. It never runs in the background, and never surprises anyone.

### Conflict policy, decided now

Health Trail has a single point person, so conflicts are the same human editing on two devices, not two people disagreeing. That makes a simple policy correct:

- **Per row, last write wins,** compared on `updated_at`, with `origin_device` as a deterministic tiebreaker when timestamps are identical.
- **Nothing is silently discarded.** The losing version is written to a `conflict_log` table with both versions intact. The user gets a plain notice that two versions existed and can see what was replaced. A record-keeping app that quietly eats an entry has failed at its one job.
- **Attachments never conflict,** because they are content addressed.
- **Tombstones win over edits.** A deletion the user performed on one device is not undone by an edit made earlier on another.

Document this in DECISIONS.md with the reasoning, because a future session will otherwise try to build field-level merging that this app does not need.

---

## 6. Web readiness

The web version is a progressive web app: installable to a home screen, works offline, and keeps every byte in the browser on the user's own device.

**What Phase 0 must produce in `/web`:** not a placeholder. A running scaffold that opens a database created from `/contract/schema.sql` using SQLite compiled to WebAssembly, stored in the browser's origin private file system, and reads the template JSON from `/templates`. It needs no features. It needs to prove that the contract is real by opening the same schema the Android app uses. That single proof is what keeps the two platforms honest for the rest of the project.

**Requirements to design for now, implement later:**

- **Persistent storage must be requested explicitly,** and the app must be honest that browsers can evict local data under storage pressure. This is a real limitation, it goes in the interface in plain words, and it makes backup discipline more important on web than on Android, not less.
- **The export container is byte-identical across platforms.** A file exported from the web version imports into Android and the reverse, with no conversion step. This is the entire reason the contract exists.
- **No service worker caching of user data.** The service worker caches the application shell so it works offline. User data lives in the database only, never in a cache the user cannot see or clear.

---

## 7. The deterministic engine and shared test vectors

The Today digest, the month review, the appointment prep sheet, and the pattern counts are all produced by a deterministic engine: real values pulled from the database, all arithmetic done in code, sentences composed from per-locale templates. No model, no inference, no interpretation. Every line traceable to a real entry.

Because that engine will exist twice, once in Kotlin and once in TypeScript, it needs a shared definition of correct:

`/contract/test-vectors/` holds golden files. Each is an input fixture, a small set of rows in a documented JSON shape, paired with the exact expected output string for each supported locale. Both platforms run the same vectors in their own test suites. If Kotlin and TypeScript disagree on the same input, one of them is wrong and CI says so.

The vectors must cover the cases that actually break:

- Zero entries, one entry, two entries. Sparse data must produce honest output, never inflated insight, and must never use trend or pattern language below the minimum-data threshold.
- A long gap, so gap handling is exercised. A gap renders as a gap. Never interpolate across it, never imply a missing entry was a failure.
- Plural boundaries: one call, two calls, zero calls, in every language. This is why the engine composes from message templates with proper plural rules rather than gluing strings together, and it is why the architecture decision has to be made now rather than during translation.
- Right to left rendering, since Arabic is a v1 language.
- Every measurement type, including the ones flagged high risk in the template schema, verifying that no output ever contains a range, a threshold, a judgment, or a color coded value.

---

## 8. The export container

One file, versioned from the first release, self describing enough that a stranger could read it in ten years.

```
healthtrail-export-YYYYMMDD-HHMM.htx        (a zip container)
  manifest.json      format version, app version and platform, export timestamp,
                     origin device id, row counts per table, sha256 of data.sqlite,
                     attachment count and total bytes, whether the payload is encrypted
  data.sqlite        the whole database, tombstones included
  attachments/       one file per attachment, named by content hash
```

Rules:

- **The format version is in the manifest from release one.** An importer reads the version first and refuses a version it does not understand, with a plain message naming the version it found and the versions it supports. Never guess at an unknown format.
- **SQLite is the payload** because all three target platforms read it natively or through a well maintained WebAssembly build, and because it preserves relationships, ordering, and state exactly. The schema is published, so the file is not a black box.
- **Tombstones are included.** An export that drops them cannot restore a deletion, which means restoring a backup resurrects things the user deleted.
- **Encryption is offered and defaults on,** using a passphrase the user chooses, because this file contains health information and will land in a folder the user picked. The encryption of the export is independent of the at-rest database encryption on the device, since a portable file cannot depend on a key held in one device's keystore. If the passphrase is lost the file cannot be recovered, there is no server and no backdoor, and the interface must say exactly that before the user commits to it.
- **An unencrypted export is available** for someone who wants to inspect their own data, with a plain warning rather than a scolding.
- **Import is atomic.** It fully succeeds or changes nothing. Never a partial import.
- **Import is honest.** If the file contains a table or column this version does not recognize, say so and name it rather than silently dropping it.
- **Import offers merge or replace** as an explicit choice, described in plain terms, defaulting to whichever is safer for the situation the app can detect.

### The round trip test

Per the universal data portability rules, this is not "did the export succeed" but field by field equality after export, wipe, and import. It asserts on: every row of every table including tombstones, all archived, pinned, resolved, and completion state, ordering, all timestamps to the millisecond, every relationship between rows, every attachment present and hash-verified, and the schema version. It runs on: a fresh install, an install with existing data, and a device with less storage than the source. It runs on an emulator, never on the owner's device.

No feature is finished until it survives that round trip. A feature that stores something the export does not carry is a feature that silently loses user data on device migration, which for this audience is the worst failure the app can have.

---

## 9. Automated local backup

Backup matters more here than in most apps, because the people using it are exhausted and the record is often the only continuous account of years of care.

- Android supports genuine automated local backup: the app asks once for durable permission to write to a folder the user chooses, then a scheduled job writes a fresh export there on a cadence. No cloud, no account, no server. If the folder the user picked happens to be synced by something they already run, that is their choice and their business.
- The folder choice is the user's, always. Never a hidden app-private location they cannot reach.
- Keep a rolling set of recent backups rather than overwriting one file, since a corrupted single backup is not a backup.
- A quiet, permanent indicator shows when the last backup succeeded. It never nags.
- Offer backup once, at the moment the person has something worth losing rather than at first launch, and honor a decline permanently.
- Restore is as easy as backup, equally tested, and works onto a fresh install, an install with existing data, and a weaker device.
- On web, automated backup is not reliably available, so the app says so plainly and prompts for manual export instead. Being honest about which one the user is getting is part of the requirement.

---

## 10. Phase 0 acceptance criteria

Phase 0 is not complete until all of these are true:

1. The monorepo layout in section 2 exists, with `/contract` holding the schema, the export format spec, the message catalog, and the test vectors directory.
2. `schema.sql` implements every column in section 3 on every user data table, plus `change_log` and `conflict_log`.
3. A repository or view layer makes it structurally difficult to query without filtering tombstones.
4. Ids are generated locally and are collision safe. There is no auto-increment primary key on any user data table.
5. Every write appends to `change_log` in the same transaction, proven by a test.
6. The `SyncTransport` interface exists with the file-based implementation behind it, and the reconciliation code has no knowledge of transport.
7. The export container writes and reads, with the manifest, the version check, encryption, and the full round trip test passing on an emulator.
8. `/web` opens the same schema through SQLite in WebAssembly and reads the same template JSON, proving the contract.
9. Test vectors exist for at least the empty, single entry, and gap cases, and both platforms run them.
10. Everything in this document that was adapted or deviated from is recorded in DECISIONS.md with the reasoning.
