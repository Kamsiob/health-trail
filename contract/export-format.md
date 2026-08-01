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

**Event dates travel as their EDTF string and are re-derived on the other side.** The `<name>_edtf` column round-trips byte for byte and is what the round trip test asserts equality on. The `<name>_start` and `<name>_end` columns are recomputed from it on import rather than trusted from the file, which is what keeps them an index rather than a second source of truth that can arrive already disagreeing. The zone travels as written, so a date recorded in one zone still reads as the reading the person saw. Data contract section 3.1.

**Attachments are named by their content hash,** with no extension and no directory nesting. The database row carries the original filename, the mime type, and the size. An attachment whose bytes do not hash to its name is corrupt and the import says so and stops.

## 4. Encryption

**Offered, and defaulting on.** This file contains health information about a real person and it will land in a folder the person picked, which may be synced somewhere by something they already run.

The payload, meaning `data.sqlite` and everything under `attachments/`, is encrypted with a key derived from a passphrase the person chooses. `manifest.json` stays readable.

**This is not the same as the at-rest database encryption and must never be conflated with it.** The database on the device is encrypted with a key held in the Android Keystore that never leaves the device. A portable file cannot depend on one device's keystore, so it uses a passphrase instead.

**If the passphrase is lost the file cannot be recovered.** There is no server, no recovery code, and no backdoor. The interface says exactly that, in those words, before the person commits to it, rather than afterward.

**An unencrypted export is available** for someone who wants to inspect their own data, with a plain warning rather than a scolding. It is their data and wanting to read it is reasonable.

### 4.1 What implements it, and what may not

**The algorithms above are the specification, not a preference.** Argon2id for the key derivation and AES-256-GCM for the payload.

**AES-256-GCM comes from the platform.** It is in the Java Cryptography Extension on every supported Android version and needs no dependency.

**Argon2id comes from Bouncy Castle**, `Argon2BytesGenerator`. It is pure Java, so it adds no native library and no NDK build step. Neither the platform nor SQLCipher exposes an Argon2 implementation, which is the whole reason a dependency is needed at all.

**PBKDF2 is not an acceptable substitute.** It is what is already in the platform, which makes it the easy path, and it is a materially weaker claim than this format makes. PBKDF2 is cheap to attack with parallel hardware because it needs almost no memory; Argon2id is memory-hard specifically to remove that advantage. Per D24 **the export file is the only recovery path from key loss**, which makes it the most security sensitive artifact this project produces. Weakening its key derivation to avoid one pure-Java dependency is the wrong trade, and it would be an invisible one: a file encrypted with PBKDF2 looks exactly as safe as one encrypted properly.

### 4.2 Why the parameters are in the manifest

`kdf_iterations`, `kdf_memory_kib`, and `kdf_parallelism` are recorded in the manifest of every encrypted file, and **an importer reads them from the file rather than assuming the values this build happens to use.**

That is what allows the cost to be raised later without stranding anything. Hardware gets faster and the recommended cost goes up with it; a file written in 2026 must still open in 2031 against whatever the parameters were when it was written. An importer that assumes today's constants silently fails to derive the right key from a correct passphrase, and reports it as a wrong passphrase, which is the worst available failure for a file that is somebody's only copy.

The shipped values, 3 iterations over 64 MiB with parallelism 1, sit above the OWASP baseline rather than at it. **Tune only if it measures unusably slow on the phone**, and if it is tuned, the manifest records what was used and older files keep opening.


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

It runs on a fresh install, on an install with existing data, and on a device with less storage than the source.

**It runs in continuous integration and on the connected phone.** An earlier version of this line said "on an emulator, never on the owner's device", which is stale: the emulator was dropped from this project in D21, D23, and B4, and B4 corrected the reasoning behind it. Data survival is proven by this round trip running on every push, which is repeatable, rather than by preserving one phone's installation, which is a sample of one nobody can reproduce.

On the phone the standing rule applies, and it is a checklist step rather than a reason to avoid anything: `connectedAndroidTest` uninstalls the application and takes its data with it, so anything worth keeping is exported through the app first and reimported after.

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
