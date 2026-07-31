# The Health Trail export container

Binding on every platform. A file written by the Android app imports into the web version and the reverse, with no conversion step. That is the entire reason this specification exists rather than each platform inventing its own.

The governing rule, from the universal data portability standard: **anything the app can store, the export contains and the import restores.** A feature that stores something the export does not carry silently loses records on device migration, which for this audience is the worst failure the app can have.

---

## 1. The file

```
healthtrail-export-YYYYMMDD-HHMM.htx
```

A zip container. The extension is `.htx` so the file is recognizable and so a person does not open it expecting a document, but it is an ordinary zip and that is deliberate: in ten years someone should be able to open it with whatever they have.

```
manifest.json      always first in the archive, always unencrypted
data.sqlite        the whole database, tombstones included
attachments/       one file per attachment, named by its content hash
```

## 2. manifest.json

Read before anything else. It is never encrypted, even when the payload is, because an importer has to be able to say what a file is before it can ask for a passphrase.

```json
{
  "format_version": 1,
  "app_version": "0.1.0",
  "platform": "android",
  "exported_at": 1753977600000,
  "origin_device": "01J8Z9K2QF7X3M5N",
  "encrypted": true,
  "encryption": {
    "algorithm": "AES-256-GCM",
    "kdf": "Argon2id",
    "kdf_iterations": 3,
    "kdf_memory_kib": 65536,
    "kdf_parallelism": 1,
    "salt": "base64",
    "nonce": "base64"
  },
  "database": {
    "sha256": "hex of data.sqlite as stored",
    "byte_size": 1048576,
    "schema_version": 1,
    "row_counts": { "entry": 1843, "person": 62, "chapter": 8 }
  },
  "attachments": { "count": 40, "total_bytes": 88160256 },
  "subject_count": 1
}
```

`row_counts` carries every table including the ones with zero rows. It exists so an import can state plainly what is about to be imported before doing it, and so a partial or truncated file is detectable before any work starts.

## 3. Rules

**The format version is in the manifest from release one.** An importer reads it first and refuses a version it does not understand, with a plain message naming the version it found and the versions it supports. It never guesses at an unknown format. This costs nothing now and is unfixable later.

**SQLite is the payload** because every target platform reads it natively or through a well maintained WebAssembly build, and because it preserves relationships, ordering, and state exactly. The schema is published in `schema.sql`, so the file is not a black box.

**Tombstones are included.** An export that drops them cannot restore a deletion, which means restoring a backup resurrects things the person deleted.

**The change log travels with it,** and the importer renumbers `seq` locally while preserving `table_name`, `row_id`, `op`, `rev`, `changed_at`, and `device_id`. `seq` is meaningful only on the device that wrote it, and two devices will both have a sequence 1 that are not the same event. See DECISIONS.md D12.

**Attachments are named by their content hash,** with no extension and no directory nesting. The database row carries the original filename, the mime type, and the size. An attachment whose bytes do not hash to its name is corrupt and the import says so and stops.

## 4. Encryption

**Offered, and defaulting on.** This file contains health information about a real person and it will land in a folder the person picked, which may be synced somewhere by something they already run.

The payload, meaning `data.sqlite` and everything under `attachments/`, is encrypted with a key derived from a passphrase the person chooses. `manifest.json` stays readable.

**This is not the same as the at-rest database encryption and must never be conflated with it.** The database on the device is encrypted with a key held in the Android Keystore that never leaves the device. A portable file cannot depend on one device's keystore, so it uses a passphrase instead.

**If the passphrase is lost the file cannot be recovered.** There is no server, no recovery code, and no backdoor. The interface says exactly that, in those words, before the person commits to it, rather than afterward.

**An unencrypted export is available** for someone who wants to inspect their own data, with a plain warning rather than a scolding. It is their data and wanting to read it is reasonable.

## 5. Import

**Atomic.** It fully succeeds or it changes nothing. A partially restored state that looks complete is worse than a clean failure, because the person stops worrying.

**Honest.** If the file contains a table or a column this version does not recognize, the import says so and names it rather than silently dropping it. If anything cannot be restored, it says exactly what and why rather than reporting success.

**Merge or replace is an explicit choice,** described in plain terms, defaulting to whichever is safer for the situation the app can detect. Importing into an empty notebook defaults to replace. Importing into one that already has records defaults to merge, because replace would discard what is already there.

Merge reconciles per row using the same rules as sync: last write wins on `updated_at`, `origin_device` as a deterministic tiebreaker, tombstones beating earlier edits, and the losing version written to `conflict_log` intact with the person told plainly that two versions existed.

## 6. What the round trip test asserts

Not "did the export succeed". Field by field equality after export, wipe, and import:

- every row of every table, including tombstones
- all archived, pinned, resolved, and completion state
- ordering
- all timestamps to the millisecond
- every relationship between rows
- every attachment present and hash verified
- the schema version

It runs on a fresh install, on an install with existing data, and on a device with less storage than the source. It runs on an emulator, never on the owner's device.

**No feature is finished until it survives that round trip.**

## 7. Files that must fail cleanly

Each of these changes nothing and names what was wrong:

- a truncated file
- a valid zip with no manifest
- a manifest claiming a format version from the future
- a database with an unknown table or column
- an attachment whose hash does not match its filename
- an attachment referenced by the database but absent from the archive
- a manifest whose row counts disagree with the database
- a correct file with the wrong passphrase
